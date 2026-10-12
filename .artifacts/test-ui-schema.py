from pathlib import Path
p=Path('supabase/tests/access_control.sql');s=p.read_text(encoding='utf-8');s=s.replace('rollback;','''-- Common materials share the subject but do not impersonate a faculty section.
insert into public.notes(uploader_id,pod_id,semester_id,course_code,faculty,title,object_path,mime_type,size_bytes,is_common)
select '00000000-0000-4000-8000-000000000001',id,'TEST','TEST101','','Common material','00000000-0000-4000-8000-000000000001/common','text/plain',1,true from public.pods where semester_id='TEST' and class_id='classA';
do $$ begin
 begin
  insert into public.tasks(creator_id,title,priority,due_at,is_personal) values ('00000000-0000-4000-8000-000000000001',repeat('word ',26),'STANDARD',now(),true);
  raise exception '26 word title accepted';
 exception when check_violation then null; end;
 begin
  insert into public.notes(uploader_id,pod_id,semester_id,course_code,faculty,title,object_path,mime_type,size_bytes,is_common)
  select '00000000-0000-4000-8000-000000000001',id,'TEST','TEST101','Faculty B','Forged faculty','00000000-0000-4000-8000-000000000001/forged','text/plain',1,false from public.pods where semester_id='TEST' and class_id='classA';
  raise exception 'Forged faculty accepted';
 exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000002',true);
do $$ begin
 if (select count(*) from public.notes where is_common)<>1 then raise exception 'Common material missing across faculty'; end if;
end $$;
rollback;''');p.write_text(s,encoding='utf-8')
sql=['begin;']
for p in sorted(Path('supabase/migrations').glob('2026100313*.sql')):
    version,name=p.stem.split('_',1)
    body=p.read_text(encoding='utf-8').replace("'","''")
    sql.append(f"insert into supabase_migrations.schema_migrations(version,statements,name) values ('{version}',array['{body}'],'{name}') on conflict(version) do nothing;")
sql.append('commit;');Path('.artifacts/record-ui-migrations.sql').write_text('\n'.join(sql),encoding='utf-8')
