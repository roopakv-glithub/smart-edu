begin;
alter table public.notes add column is_common boolean not null default false;
alter table public.tasks add constraint task_title_words check(cardinality(regexp_split_to_array(btrim(title), '\s+')) <= 25) not valid;
update public.profiles set display_name='' where display_name=registration_number;
drop policy notes_read on public.notes;
create policy notes_read on public.notes for select to authenticated using(uploader_id=(select auth.uid()) or exists(select 1 from public.pods p where regexp_replace(p.course_code,'\s*\([^)]*\)\s*$','')=regexp_replace(notes.course_code,'\s*\([^)]*\)\s*$','') and p.semester_id=notes.semester_id and private.is_pod_member(p.id)));
drop policy notes_create on public.notes;
create policy notes_create on public.notes for insert to authenticated with check(uploader_id=(select auth.uid()) and split_part(object_path,'/',1)=(select auth.uid())::text and private.is_pod_member(pod_id) and exists(select 1 from public.pods p where p.id=notes.pod_id and p.course_code=notes.course_code and p.semester_id=notes.semester_id and ((notes.is_common and notes.faculty='') or (not notes.is_common and p.faculty=notes.faculty))));
commit;
