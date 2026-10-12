-- StudySync: identity and memberships are written only by the verified-session Edge Function.
create schema if not exists private;
revoke all on schema private from public;
grant usage on schema private to authenticated, service_role;

create table public.profiles (
 id uuid primary key references auth.users(id) on delete cascade,
 registration_number text not null unique,
 display_name text not null,
 created_at timestamptz not null default now()
);
create table public.academic_snapshots (
 user_id uuid not null references public.profiles(id) on delete cascade,
 semester_id text not null,
 courses jsonb not null default '[]'::jsonb check(jsonb_typeof(courses)='array'),
 exams jsonb not null default '[]'::jsonb check(jsonb_typeof(exams)='array'),
 synced_at timestamptz not null default now(),
 primary key(user_id,semester_id)
);
create table public.pods (
 id uuid primary key default gen_random_uuid(),
 campus text not null default 'VIT Chennai',
 semester_id text not null,
 course_code text not null,
 course_title text not null,
 faculty text not null,
 class_id text not null,
 created_at timestamptz not null default now(),
 unique(campus,semester_id,class_id,course_code,faculty),
 check(length(trim(class_id))>0 and length(trim(faculty))>0)
);
comment on table public.pods is 'Conservative isolation by verified VTOP class instance, semester, course and faculty. Program/cohort is not inferred from registration numbers.';
create table public.pod_members (
 pod_id uuid not null references public.pods(id) on delete cascade,
 user_id uuid not null references public.profiles(id) on delete cascade,
 active boolean not null default true,
 joined_at timestamptz not null default now(),
 left_at timestamptz,
 primary key(pod_id,user_id)
);
create index pod_members_user_active on public.pod_members(user_id,pod_id) where active;
create table public.tasks (
 id uuid primary key default gen_random_uuid(),
 creator_id uuid not null references public.profiles(id),
 pod_id uuid references public.pods(id),
 title text not null check(length(trim(title)) between 1 and 200),
 description text not null default '' check(length(description)<=10000),
 priority text not null check(priority in ('HIGH','MEDIUM','STANDARD')),
 course_code text not null default '',
 due_at timestamptz not null,
 is_personal boolean not null default false,
 created_at timestamptz not null default now(),
 check((is_personal and pod_id is null) or (not is_personal and pod_id is not null))
);
create index tasks_creator on public.tasks(creator_id);
create index tasks_pod_due on public.tasks(pod_id,due_at);
create table public.task_user_state (
 task_id uuid not null references public.tasks(id) on delete cascade,
 user_id uuid not null references public.profiles(id) on delete cascade,
 is_completed boolean not null default false,
 primary key(task_id,user_id)
);
create index task_state_user on public.task_user_state(user_id,task_id);
create table public.pod_messages (
 id uuid primary key default gen_random_uuid(),
 pod_id uuid not null references public.pods(id),
 sender_id uuid not null references public.profiles(id),
 body text not null check(length(trim(body)) between 1 and 10000),
 message_type text not null default 'text' check(message_type in ('text','ai_response')),
 created_at timestamptz not null default now(),
 deleted_at timestamptz
);
create index messages_pod_time on public.pod_messages(pod_id,created_at);
create index messages_sender on public.pod_messages(sender_id);
create table public.notes (
 id uuid primary key default gen_random_uuid(),
 uploader_id uuid not null references public.profiles(id),
 pod_id uuid not null references public.pods(id),
 semester_id text not null,
 course_code text not null,
 faculty text not null,
 title text not null check(length(trim(title)) between 1 and 200),
 object_path text not null unique,
 mime_type text not null,
 size_bytes bigint not null check(size_bytes between 1 and 20971520),
 created_at timestamptz not null default now()
);
create index notes_course on public.notes(semester_id,course_code);
create index notes_uploader on public.notes(uploader_id);
create index notes_pod on public.notes(pod_id);
create table public.personal_events (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references public.profiles(id),
 title text not null check(length(trim(title)) between 1 and 200),
 category text not null default 'Personal',
 event_date date not null,
 time_label text not null check(length(time_label) between 1 and 80),
 created_at timestamptz not null default now()
);
create index personal_events_user_date on public.personal_events(user_id,event_date);
create table public.user_settings (
 user_id uuid primary key references public.profiles(id) on delete cascade,
 vibrations boolean not null default true,
 notifications boolean not null default true,
 ai_copilot boolean not null default false
);
create table private.session_rate_limits (
 bucket timestamptz not null,
 key_hash text not null,
 attempts integer not null default 1,
 primary key(bucket,key_hash)
);

create function private.is_pod_member(target uuid) returns boolean
language sql stable security definer set search_path='' as $$
 select (select auth.uid()) is not null and exists(
  select 1 from public.pod_members m where m.pod_id=target and m.user_id=(select auth.uid()) and m.active
 );
$$;
revoke all on function private.is_pod_member(uuid) from public, anon;
grant execute on function private.is_pod_member(uuid) to authenticated;
create function private.is_peer(target uuid) returns boolean
language sql stable security definer set search_path='' as $$
 select (select auth.uid()) is not null and exists(
  select 1 from public.pod_members a join public.pod_members b on a.pod_id=b.pod_id
  where a.user_id=(select auth.uid()) and b.user_id=target and a.active and b.active
 );
$$;
revoke all on function private.is_peer(uuid) from public, anon;
grant execute on function private.is_peer(uuid) to authenticated;

alter table public.profiles enable row level security;
alter table public.academic_snapshots enable row level security;
alter table public.pods enable row level security;
alter table public.pod_members enable row level security;
alter table public.tasks enable row level security;
alter table public.task_user_state enable row level security;
alter table public.pod_messages enable row level security;
alter table public.notes enable row level security;
alter table public.personal_events enable row level security;
alter table public.user_settings enable row level security;
alter table private.session_rate_limits enable row level security;

create policy profiles_read on public.profiles for select to authenticated using(id=(select auth.uid()) or private.is_peer(id));
create policy snapshots_own on public.academic_snapshots for select to authenticated using(user_id=(select auth.uid()));
create policy pods_member on public.pods for select to authenticated using(private.is_pod_member(id));
create policy members_read on public.pod_members for select to authenticated using(private.is_pod_member(pod_id));
create policy tasks_read on public.tasks for select to authenticated using((is_personal and creator_id=(select auth.uid())) or (not is_personal and private.is_pod_member(pod_id)));
create policy tasks_create on public.tasks for insert to authenticated with check(creator_id=(select auth.uid()) and ((is_personal and pod_id is null) or (not is_personal and private.is_pod_member(pod_id) and exists(select 1 from public.pods p where p.id=pod_id and p.course_code=tasks.course_code))));
create policy tasks_edit on public.tasks for update to authenticated using(creator_id=(select auth.uid()) and (is_personal or private.is_pod_member(pod_id))) with check(creator_id=(select auth.uid()) and (is_personal or private.is_pod_member(pod_id)));
create policy tasks_delete on public.tasks for delete to authenticated using(creator_id=(select auth.uid()) and (is_personal or private.is_pod_member(pod_id)));
create policy completion_read on public.task_user_state for select to authenticated using(user_id=(select auth.uid()) and exists(select 1 from public.tasks t where t.id=task_id));
create policy completion_create on public.task_user_state for insert to authenticated with check(user_id=(select auth.uid()) and exists(select 1 from public.tasks t where t.id=task_id));
create policy completion_edit on public.task_user_state for update to authenticated using(user_id=(select auth.uid()) and exists(select 1 from public.tasks t where t.id=task_id)) with check(user_id=(select auth.uid()) and exists(select 1 from public.tasks t where t.id=task_id));
create policy messages_read on public.pod_messages for select to authenticated using(private.is_pod_member(pod_id));
create policy messages_create on public.pod_messages for insert to authenticated with check(sender_id=(select auth.uid()) and private.is_pod_member(pod_id) and deleted_at is null);
create policy messages_edit on public.pod_messages for update to authenticated using(sender_id=(select auth.uid()) and private.is_pod_member(pod_id)) with check(sender_id=(select auth.uid()) and private.is_pod_member(pod_id));
create policy notes_read on public.notes for select to authenticated using(uploader_id=(select auth.uid()) or exists(select 1 from public.pods p where p.course_code=notes.course_code and p.semester_id=notes.semester_id and private.is_pod_member(p.id)));
create policy notes_create on public.notes for insert to authenticated with check(uploader_id=(select auth.uid()) and split_part(object_path,'/',1)=(select auth.uid())::text and private.is_pod_member(pod_id) and exists(select 1 from public.pods p where p.id=notes.pod_id and p.course_code=notes.course_code and p.faculty=notes.faculty and p.semester_id=notes.semester_id));
create policy notes_delete on public.notes for delete to authenticated using(uploader_id=(select auth.uid()));
create policy events_own_read on public.personal_events for select to authenticated using(user_id=(select auth.uid()));
create policy events_own_create on public.personal_events for insert to authenticated with check(user_id=(select auth.uid()));
create policy events_own_edit on public.personal_events for update to authenticated using(user_id=(select auth.uid())) with check(user_id=(select auth.uid()));
create policy events_own_delete on public.personal_events for delete to authenticated using(user_id=(select auth.uid()));
create policy settings_own_read on public.user_settings for select to authenticated using(user_id=(select auth.uid()));
create policy settings_own_create on public.user_settings for insert to authenticated with check(user_id=(select auth.uid()));
create policy settings_own_edit on public.user_settings for update to authenticated using(user_id=(select auth.uid())) with check(user_id=(select auth.uid()));

revoke all on public.profiles,public.academic_snapshots,public.pods,public.pod_members,public.tasks,public.task_user_state,public.pod_messages,public.notes,public.personal_events,public.user_settings from anon, authenticated;
grant select on public.profiles,public.academic_snapshots,public.pods,public.pod_members to authenticated;
grant select,insert,delete on public.tasks to authenticated;
grant update(title,description,priority,due_at) on public.tasks to authenticated;
grant select,insert on public.task_user_state to authenticated;
grant update(is_completed) on public.task_user_state to authenticated;
grant select,insert on public.pod_messages to authenticated;
grant update(body,deleted_at) on public.pod_messages to authenticated;
grant select,insert,delete on public.notes to authenticated;
grant select,insert,update,delete on public.personal_events to authenticated;
grant select,insert,update on public.user_settings to authenticated;
grant all on public.profiles,public.academic_snapshots,public.pods,public.pod_members,public.tasks,public.task_user_state,public.pod_messages,public.notes,public.personal_events,public.user_settings,private.session_rate_limits to service_role;

create function public.studysync_rate_limit(p_key_hash text) returns boolean
language plpgsql security invoker set search_path='' as $$
declare amount integer;
begin
 delete from private.session_rate_limits where bucket < now()-interval '2 hours';
 insert into private.session_rate_limits(bucket,key_hash) values(date_trunc('minute',now()),p_key_hash)
 on conflict(bucket,key_hash) do update set attempts=private.session_rate_limits.attempts+1 returning attempts into amount;
 return amount<=12;
end;
$$;
revoke all on function public.studysync_rate_limit(text) from public,anon,authenticated;
grant execute on function public.studysync_rate_limit(text) to service_role;

create function public.studysync_sync_academics(p_user_id uuid,p_semester text,p_courses jsonb,p_exams jsonb)
returns void language plpgsql security invoker set search_path='' as $$
declare course jsonb; selected_pod uuid;
begin
 if jsonb_typeof(p_courses)<>'array' or jsonb_typeof(p_exams)<>'array' then raise exception 'Invalid academic payload'; end if;
 perform 1 from public.profiles where id=p_user_id for update;
 if not found then raise exception 'Profile required'; end if;
 insert into public.academic_snapshots(user_id,semester_id,courses,exams) values(p_user_id,p_semester,p_courses,p_exams)
 on conflict(user_id,semester_id) do update set courses=excluded.courses,exams=excluded.exams,synced_at=now();
 update public.pod_members set active=false,left_at=now() where user_id=p_user_id and active;
 for course in select value from jsonb_array_elements(p_courses) loop
  if coalesce(trim(course->>'classId'),'')='' or coalesce(trim(course->>'faculty'),'')='' then continue; end if;
  insert into public.pods(semester_id,course_code,course_title,faculty,class_id)
  values(p_semester,course->>'courseCode',course->>'title',course->>'faculty',course->>'classId')
  on conflict(campus,semester_id,class_id,course_code,faculty) do update set course_title=excluded.course_title
  returning id into selected_pod;
  insert into public.pod_members(pod_id,user_id,active) values(selected_pod,p_user_id,true)
  on conflict(pod_id,user_id) do update set active=true,left_at=null;
 end loop;
 insert into public.user_settings(user_id) values(p_user_id) on conflict do nothing;
end;
$$;
revoke all on function public.studysync_sync_academics(uuid,text,jsonb,jsonb) from public,anon,authenticated;
grant execute on function public.studysync_sync_academics(uuid,text,jsonb,jsonb) to service_role;

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values('study-notes','study-notes',false,20971520,array['application/pdf','text/plain','image/jpeg','image/png','application/vnd.openxmlformats-officedocument.wordprocessingml.document'])
on conflict(id) do nothing;
create policy study_notes_upload on storage.objects for insert to authenticated
with check(bucket_id='study-notes' and (storage.foldername(name))[1]=(select auth.uid())::text and exists(select 1 from public.pod_members where user_id=(select auth.uid()) and active));
create policy study_notes_read on storage.objects for select to authenticated
using(bucket_id='study-notes' and ((storage.foldername(name))[1]=(select auth.uid())::text or exists(select 1 from public.notes where object_path=storage.objects.name)));
create policy study_notes_delete on storage.objects for delete to authenticated
using(bucket_id='study-notes' and (storage.foldername(name))[1]=(select auth.uid())::text);
