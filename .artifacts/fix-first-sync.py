from pathlib import Path
p=Path('app/src/main/java/com/studysync/app/ui/viewmodel/StudySyncViewModel.kt');s=p.read_text().replace('import androidx.lifecycle.ViewModel','import android.app.Application\nimport androidx.lifecycle.AndroidViewModel\nimport com.studysync.app.data.session.EncryptedSessionStore')
s=s.replace('class StudySyncViewModel : ViewModel()','class StudySyncViewModel(application: Application) : AndroidViewModel(application)').replace('private val cloud=SupabaseRepository()','private val cloud=SupabaseRepository(EncryptedSessionStore(application))')
s=s.replace('MutableStateFlow(AcademicConfig.DEFAULT_SEMESTER)','MutableStateFlow(cloud.selectedSemester)').replace('private val _isLoggedIn=MutableStateFlow(false)','private val _isLoggedIn=MutableStateFlow(cloud.hasSession)').replace('private val _userName=MutableStateFlow("")','private val _userName=MutableStateFlow(cloud.registrationNumber)')
s=s.replace('    private fun format(pattern:', '''    init {
        cloud.cachedSnapshot(_semesterId.value)?.let { applySnapshot(it) }
        if(cloud.hasSession) refreshAcademics()
    }
    private fun applySnapshot(snapshot: AcademicSnapshot) {
        _courses.value=snapshot.courses; examEvents=snapshot.exams; _scheduleEvents.value=examEvents
    }
    private fun format(pattern:''')
s=s.replace('is AuthResult.Success -> { _userName.value=result.session.authorizedID; _isLoggedIn.value=true; _loginState.value=LoginUiState.Authenticated; refreshAcademics() }','''is AuthResult.Success -> {
                    try {
                        val snapshot=cloud.sync(result.session,_semesterId.value)
                        applySnapshot(snapshot)
                        _userName.value=result.session.authorizedID; _isLoggedIn.value=true; _loginState.value=LoginUiState.Authenticated
                        _syncError.value=snapshot.warnings.takeIf { it.isNotEmpty() }?.joinToString(" ")
                        refreshCloud()
                    } catch(e:CancellationException) { throw e }
                    catch(e:Exception) { _loginState.value=LoginUiState.Error(failure(e)) }
                }''')
s=s.replace('_semesterId.value=value; _courses.value=', '_semesterId.value=value; cloud.selectedSemester=value; _courses.value=')
s=s.replace('    fun refreshAcademics() {','    fun refreshAcademics(fromVtop: Boolean = false) {')
a=s.index('                val session=academicRepository.currentSession ?:');b=s.index('            } catch(e:CancellationException)',a)
s=s[:a]+'''                val snapshot=if(fromVtop) {
                    val session=academicRepository.currentSession ?: throw CloudException("Reconnect VTOP to import new university data. Saved app data is still available.")
                    cloud.sync(session,_semesterId.value)
                } else cloud.fetchSnapshot(_semesterId.value)
                if(snapshot!=null) applySnapshot(snapshot)
                loadCloud()
                _syncError.value=snapshot?.warnings?.takeIf{it.isNotEmpty()}?.joinToString(" ") ?: if(snapshot==null || snapshot.courses.isEmpty()) "No subjects saved for this semester. Choose your enrolled semester or import from VTOP." else null
''' +s[b:]
p.write_text(s)
# Use the protected dashboard, normalize Set-Cookie strings, and follow only same-origin redirects.
p=Path('supabase/functions/studysync-session/index.ts');s=p.read_text()
s=s.replace("const cookies=text(body.cookies), semesterId=text(body.semesterId);", "const rawCookies=text(body.cookies), semesterId=text(body.semesterId);\n    const cookieMap=new Map<string,string>();\n    for(const part of rawCookies.split(';')) { const match=part.trim().match(/^([^=\\s]+)=(.*)$/); if(match && !/^(path|domain|expires|max-age|samesite)$/i.test(match[1])) cookieMap.set(match[1],match[2]); }\n    const cookies=Array.from(cookieMap,([k,v])=>`${k}=${v}`).join('; ');")
a=s.index("    const page=await fetch('https://vtopcc.vit.ac.in/vtop/open/page'");b=s.index('    const payload=',a)
s=s[:a]+'''    let html='';
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
''' +s[b:]
p.write_text(s)
