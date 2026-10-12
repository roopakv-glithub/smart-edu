begin;
insert into auth.users(id,email) values
('00000000-0000-4000-8000-000000000001','test1@example.invalid'),
('00000000-0000-4000-8000-000000000002','test2@example.invalid'),
('00000000-0000-4000-8000-000000000003','test3@example.invalid');
insert into public.profiles(id,registration_number,display_name) values
('00000000-0000-4000-8000-000000000001','RLS_TEST_1','Test 1'),
('00000000-0000-4000-8000-000000000002','RLS_TEST_2','Test 2'),
('00000000-0000-4000-8000-000000000003','RLS_TEST_3','Test 3');
select public.studysync_sync_academics('00000000-0000-4000-8000-000000000001','TEST','[{"courseCode":"TEST101","title":"Testing","faculty":"Faculty A","classId":"classA"}]','[]');
select public.studysync_sync_academics('00000000-0000-4000-8000-000000000002','TEST','[{"courseCode":"TEST101","title":"Testing","faculty":"Faculty B","classId":"classB"}]','[]');
select public.studysync_sync_academics('00000000-0000-4000-8000-000000000003','TEST','[{"courseCode":"TEST101","title":"Testing","faculty":"Faculty A","classId":"classA"}]','[]');
insert into public.tasks(id,creator_id,pod_id,title,priority,course_code,due_at,is_personal)
select '00000000-0000-4000-8000-000000000011','00000000-0000-4000-8000-000000000001',id,'Pod task','HIGH','TEST101',now(),false from public.pods where semester_id='TEST' and class_id='classA';
insert into public.tasks(id,creator_id,title,priority,due_at,is_personal) values ('00000000-0000-4000-8000-000000000012','00000000-0000-4000-8000-000000000001','Personal','STANDARD',now(),true);
insert into public.notes(uploader_id,pod_id,semester_id,course_code,faculty,title,object_path,mime_type,size_bytes)
select '00000000-0000-4000-8000-000000000001',id,'TEST','TEST101',faculty,'Same subject note','00000000-0000-4000-8000-000000000001/test','text/plain',1 from public.pods where semester_id='TEST' and class_id='classA';
-- Repeated imports reuse the same student, snapshot and membership.
select public.studysync_sync_academics('00000000-0000-4000-8000-000000000001','TEST','[{"courseCode":"TEST101","title":"Testing","faculty":"Faculty A","classId":"classA"}]','[]');
do $$ begin
 if (select count(*) from public.profiles where registration_number='RLS_TEST_1')<>1 then raise exception 'Duplicate student'; end if;
 if (select count(*) from public.academic_snapshots where user_id='00000000-0000-4000-8000-000000000001')<>1 then raise exception 'Duplicate snapshot'; end if;
 if (select count(*) from public.pod_members where user_id='00000000-0000-4000-8000-000000000001')<>1 then raise exception 'Duplicate membership'; end if;
end $$;
set local role authenticated;
select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000002',true);
do $$ begin
 if (select count(*) from public.tasks)<>0 then raise exception 'Cross-faculty task leakage'; end if;
 if (select count(*) from public.notes)<>1 then raise exception 'Same-course notes unavailable'; end if;
 if (select count(*) from public.pods)<>1 then raise exception 'Pod isolation failed'; end if;
 if has_table_privilege('authenticated','public.pod_members','INSERT') then raise exception 'Self enrolment permitted'; end if;
 if has_function_privilege('authenticated','public.studysync_sync_academics(uuid,text,jsonb,jsonb)','EXECUTE') then raise exception 'Client can forge academics'; end if;
 if has_table_privilege('anon','public.academic_snapshots','SELECT') then raise exception 'Anonymous academic read permitted'; end if;
end $$;
select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000003',true);
do $$ begin
 if (select count(*) from public.tasks)<>1 then raise exception 'Classmate task access or personal isolation failed'; end if;
 if (select count(*) from public.academic_snapshots)<>1 then raise exception 'Private snapshot isolation failed'; end if;
end $$;
insert into public.task_user_state(task_id,user_id,is_completed) values ('00000000-0000-4000-8000-000000000011','00000000-0000-4000-8000-000000000003',true)
on conflict(task_id,user_id) do update set task_id=excluded.task_id,user_id=excluded.user_id,is_completed=excluded.is_completed;
select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000001',true);
do $$ begin
 if (select count(*) from public.task_user_state)<>0 then raise exception 'Completion shared across users'; end if;
 if (select count(*) from public.tasks)<>2 then raise exception 'Owner tasks missing'; end if;
end $$;
-- Common materials share the subject but do not impersonate a faculty section.
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
rollback;
