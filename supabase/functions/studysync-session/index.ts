import { vtopCa } from './vtop-ca.ts';
import { createClient } from 'npm:@supabase/supabase-js@2.117.2';
// VTOP omits an intermediate CA. This verified CA chain preserves certificate and hostname validation.
const vtopClient = Deno.createHttpClient({caCerts:[vtopCa]});
const url = Deno.env.get('SUPABASE_URL')!;
const admin = createClient(url, JSON.parse(Deno.env.get('SUPABASE_SECRET_KEYS')!)['default'], {auth:{persistSession:false,autoRefreshToken:false}});
const reply = (status:number, body:unknown) => new Response(JSON.stringify(body), {status,headers:{'Content-Type':'application/json','Cache-Control':'no-store'}});
const text = (x:unknown) => typeof x === 'string' || typeof x === 'number' ? String(x).trim() : '';
function input(html:string, name:string) {
  for (const tag of html.match(/<input\b[^>]*>/gi) ?? []) {
    const attrs:Record<string,string> = {};
    for (const m of tag.matchAll(/([\w-]+)\s*=\s*(["'])(.*?)\2/g)) attrs[m[1].toLowerCase()] = m[3];
    if ([attrs.name?.toLowerCase(),attrs.id?.toLowerCase()].includes(name.toLowerCase())) return attrs.value ?? '';
  }
  return '';
}
async function university(path:string, payload:unknown) {
  const res = await fetch('https://api-unicc.arya22.dev/api/'+path,{method:'POST',redirect:'error',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload),signal:AbortSignal.timeout(45000)});
  if (!res.ok) throw Error('Academic service unavailable');
  return await res.json();
}
function date(value:string):string {
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
  const m=value.match(/^(\d{1,2})[-/]([A-Za-z]{3}|\d{1,2})[-/](\d{4})$/);
  if (!m) return '';
  const month = /^\d+$/.test(m[2]) ? +m[2] : ['jan','feb','mar','apr','may','jun','jul','aug','sep','oct','nov','dec'].indexOf(m[2].toLowerCase())+1;
  const iso=`${m[3]}-${String(month).padStart(2,'0')}-${m[1].padStart(2,'0')}`;
  return month>0 && !isNaN(Date.parse(iso)) && new Date(iso).toISOString().slice(0,10)===iso ? iso : '';
}
Deno.serve(async req => {
  if(req.method !== 'POST') return reply(405,{error:'POST required'});
  let stage='request';
  try {
    if (Number(req.headers.get('content-length'))>16384) return reply(413,{error:'Request too large'});
    const raw=await req.text();
    if(raw.length>16384) return reply(413,{error:'Request too large'});
    const body=JSON.parse(raw);
    const rawCookies=text(body.cookies), semesterId=text(body.semesterId);
    const cookieMap=new Map<string,string>();
    for(const part of rawCookies.split(';')) { const match=part.trim().match(/^([^=\s]+)=(.*)$/); if(match && !/^(path|domain|expires|max-age|samesite)$/i.test(match[1])) cookieMap.set(match[1],match[2]); }
    const cookies=Array.from(cookieMap,([k,v])=>`${k}=${v}`).join('; ');
    if (!cookies || cookies.length>12000 || /[\r\n]/.test(cookies) || !/^CH\d{8}$/.test(semesterId)) return reply(400,{error:'Valid session and semester required'});
    const hash=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(cookies)))).map(b=>b.toString(16).padStart(2,'0')).join('');
    stage='database connection';
    const limit=await admin.rpc('studysync_rate_limit',{p_key_hash:hash});
    if(limit.error) return reply(503,{error:'Session service unavailable'});
    if(!limit.data) return reply(429,{error:'Wait a minute before refreshing'});
    // Identity comes from VTOP over verified TLS, never from a client-supplied registration number.
    stage='VTOP connection';
    let html='';
    let target='https://vtopcc.vit.ac.in/vtop/content';
    for(let redirects=0;redirects<4;redirects++) {
      const page=await fetch(target,{client:vtopClient,headers:{Cookie:cookies,'User-Agent':'Mozilla/5.0','Accept':'text/html,application/xhtml+xml'},redirect:'manual',signal:AbortSignal.timeout(25000)});
      if(page.status>=300 && page.status<400) {
        const next=new URL(page.headers.get('location')??'',target);
        if(next.origin!=='https://vtopcc.vit.ac.in' || !next.pathname.startsWith('/vtop/')) return reply(502,{error:'Unexpected university redirect'});
        target=next.href; continue;
      }
      if(!page.ok) return reply(502,{error:'University is currently unavailable'});
      html=await page.text(); break;
    }
    const authorizedID=input(html,'authorizedID').toUpperCase(), csrf=input(html,'_csrf');
    if(!/^[A-Z0-9]{5,30}$/.test(authorizedID) || !csrf) return reply(401,{error:'VTOP connection could not be verified. Please retry the connection.'});
    const payload={cookies,authorizedID,csrf,semesterId,type:'ALL'};
    stage='subject fetch';
    const attendance=await university('attendance',payload);
    if(!Array.isArray(attendance?.attRes?.attendance)) return reply(502,{error:'University returned an invalid subjects response'});
    const courses=attendance.attRes.attendance.map((r:Record<string,unknown>)=>({courseCode:text(r.courseCode),title:text(r.courseTitle),faculty:text(r.faculty),classId:text(r.classId),slots:text(r.slotName),venue:text(r.slotVenue),attendance:text(r.attendancePercentage)})).filter((r:{courseCode:string,title:string})=>r.courseCode&&r.title);
    let exams:unknown[]=[]; const warnings:string[]=[];
    let examsFetched=false;
    try {
      const schedule=await university('schedule',payload);
      if(!schedule.Schedule || typeof schedule.Schedule!=='object') throw Error();
      exams=Object.entries(schedule.Schedule).flatMap(([group,rows])=>Array.isArray(rows)?rows.flatMap(r=>{
        const d=date(text(r.examDate)); if(!d) return [];
        return [{id:`exam:${text(r.courseCode)}:${group}:${d}:${text(r.examTime)}`,title:text(r.courseTitle)||text(r.courseCode),category:group,time:text(r.examTime),duration:text(r.examSession),location:text(r.venue),dateDay:+d.slice(-2),date:d,readOnly:true}];
      }):[]);
      examsFetched=true;
    } catch { warnings.push('Exam refresh failed. Previously saved exams are retained.'); }
    stage='profile sync';
    let {data:profile,error:profileError}=await admin.from('profiles').select('id,registration_number,display_name').eq('registration_number',authorizedID).maybeSingle();
    if(profileError) throw Error();
    if(!profile) {
      const email=crypto.randomUUID()+'@studysync.invalid';
      const created=await admin.auth.admin.createUser({email,password:crypto.randomUUID()+crypto.randomUUID(),email_confirm:true});
      if(created.error || !created.data.user) throw Error();
      const inserted=await admin.from('profiles').insert({id:created.data.user.id,registration_number:authorizedID,display_name:''}).select('id,registration_number,display_name').single();
      if(inserted.error) {
        await admin.auth.admin.deleteUser(created.data.user.id);
        const existing=await admin.from('profiles').select('id,registration_number,display_name').eq('registration_number',authorizedID).single();
        if(existing.error) throw Error(); profile=existing.data;
      } else profile=inserted.data;
    }
    if(!examsFetched) {
      const previous=await admin.from('academic_snapshots').select('exams').eq('user_id',profile!.id).eq('semester_id',semesterId).maybeSingle();
      if(previous.error) throw Error(); exams=previous.data?.exams??[];
    }
    stage='academic storage';
    const synced=await admin.rpc('studysync_sync_academics',{p_user_id:profile!.id,p_semester:semesterId,p_courses:courses,p_exams:exams});
    if(synced.error) throw Error();
    stage='app session';
    const user=await admin.auth.admin.getUserById(profile!.id);
    if(user.error || !user.data.user.email) throw Error();
    const link=await admin.auth.admin.generateLink({type:'magiclink',email:user.data.user.email});
    if(link.error) throw Error();
    const authClient=createClient(url,JSON.parse(Deno.env.get('SUPABASE_PUBLISHABLE_KEYS')!)['default'],{auth:{persistSession:false,autoRefreshToken:false}});
    const session=await authClient.auth.verifyOtp({token_hash:link.data.properties.hashed_token,type:'magiclink'});
    if(session.error || !session.data.session) throw Error();
    return reply(200,{access_token:session.data.session.access_token,refresh_token:session.data.session.refresh_token,expires_at:session.data.session.expires_at,user_id:profile!.id,registration_number:authorizedID,snapshot:{semesterId,courses,exams,warnings}});
  } catch (error) { console.error('StudySync sync failed',stage,error instanceof Error ? error.name : 'Unknown'); return reply(502,{error:`Could not complete ${stage}. Retry or sign in again.`}); }
});
