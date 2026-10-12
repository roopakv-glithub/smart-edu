from pathlib import Path
p=Path('app/src/main/java/com/studysync/app/data/repository/SupabaseRepository.kt');s=p.read_text().replace('expiresAt=saved.get("expires_at")?.asLong ?: 0','expiresAt=saved.get("expires_at")?.takeIf { !it.isJsonNull }?.asLong ?: 0').replace('expiresAt=body.get("expires_at")?.asLong ?: (System.currentTimeMillis()/1000+3500)','expiresAt=body.get("expires_at")?.takeIf { !it.isJsonNull }?.asLong ?: (System.currentTimeMillis()/1000+(body.get("expires_in")?.asLong ?: 3500))').replace('listOf("access_token","refresh_token","expires_at").forEach { saved.add(it,body.get(it)) }','listOf("access_token","refresh_token").forEach { saved.add(it,body.get(it)) }\n        saved.addProperty("expires_at",expiresAt)');p.write_text(s)
p=Path('supabase/tests/access_control.sql');s=p.read_text();needle="set local role authenticated;";s=s.replace(needle,"""-- Repeated imports reuse the same student, snapshot and membership.
select public.studysync_sync_academics('00000000-0000-4000-8000-000000000001','TEST','[{"courseCode":"TEST101","title":"Testing","faculty":"Faculty A","classId":"classA"}]','[]');
do $$ begin
 if (select count(*) from public.profiles where registration_number='RLS_TEST_1')<>1 then raise exception 'Duplicate student'; end if;
 if (select count(*) from public.academic_snapshots where user_id='00000000-0000-4000-8000-000000000001')<>1 then raise exception 'Duplicate snapshot'; end if;
 if (select count(*) from public.pod_members where user_id='00000000-0000-4000-8000-000000000001')<>1 then raise exception 'Duplicate membership'; end if;
end $$;
"""+needle);p.write_text(s)
