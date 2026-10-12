-- Upserts check both ownership and task visibility through RLS.
grant update on public.task_user_state to authenticated;
drop policy messages_create on public.pod_messages;
create policy messages_create on public.pod_messages for insert to authenticated
with check(sender_id=(select auth.uid()) and private.is_pod_member(pod_id) and deleted_at is null and message_type='text');
