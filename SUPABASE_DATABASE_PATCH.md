# StudySync 4.0 — Supabase Database Patch (SQL Script)

Copy and paste the SQL script below directly into your **Supabase Dashboard > SQL Editor** and click **Run**.

---

```sql
-- ==============================================================================
-- StudySync 4.0: Comprehensive Database Schema & Security Policies
-- ==============================================================================

-- 1. USER ROLES & PERMISSIONS
create table if not exists public.user_roles (
    user_id uuid primary key references auth.users(id) on delete cascade,
    role text not null default 'student' check (role in ('admin', 'moderator', 'student')),
    granted_by uuid references auth.users(id),
    updated_at timestamptz not null default now()
);

alter table public.user_roles enable row level security;

-- Read policy: Any authenticated user can read roles (to check permissions and display badges)
drop policy if exists user_roles_read on public.user_roles;
create policy user_roles_read on public.user_roles
    for select to authenticated
    using (true);

-- Helper function to check if current user is admin
create or replace function public.is_admin(p_user_id uuid default auth.uid())
returns boolean language sql stable security definer set search_path = '' as $$
    select exists (
        select 1 from public.user_roles
        where user_id = p_user_id and role = 'admin'
    );
$$;

-- Helper function to check if current user is moderator or admin
create or replace function public.is_moderator_or_admin(p_user_id uuid default auth.uid())
returns boolean language sql stable security definer set search_path = '' as $$
    select exists (
        select 1 from public.user_roles
        where user_id = p_user_id and role in ('admin', 'moderator')
    );
$$;

-- Only admins can manage user roles
drop policy if exists user_roles_admin_manage on public.user_roles;
create policy user_roles_admin_manage on public.user_roles
    for all to authenticated
    using (public.is_admin())
    with check (public.is_admin());

grant select on public.user_roles to authenticated;
grant all on public.user_roles to service_role;

-- 2. APPLICATION RESTRICTIONS (RESTRICT TASKS & RESTRICT NOTES)
create table if not exists public.app_restrictions (
    id int primary key default 1 check (id = 1),
    restrict_tasks boolean not null default false,
    restrict_notes boolean not null default false,
    updated_by uuid references auth.users(id),
    updated_at timestamptz not null default now()
);

insert into public.app_restrictions (id, restrict_tasks, restrict_notes)
values (1, false, false)
on conflict (id) do nothing;

alter table public.app_restrictions enable row level security;

drop policy if exists app_restrictions_select on public.app_restrictions;
create policy app_restrictions_select on public.app_restrictions
    for select to authenticated
    using (true);

drop policy if exists app_restrictions_admin_update on public.app_restrictions;
create policy app_restrictions_admin_update on public.app_restrictions
    for update to authenticated
    using (public.is_admin())
    with check (public.is_admin());

grant select on public.app_restrictions to authenticated;
grant update on public.app_restrictions to authenticated;
grant all on public.app_restrictions to service_role;

-- 3. PERSONAL HIDING FOR SHARED TASKS AND NOTES
create table if not exists public.user_hidden_tasks (
    user_id uuid not null references auth.users(id) on delete cascade,
    task_id uuid not null references public.tasks(id) on delete cascade,
    hidden_at timestamptz not null default now(),
    primary key (user_id, task_id)
);

alter table public.user_hidden_tasks enable row level security;

drop policy if exists hidden_tasks_user_policy on public.user_hidden_tasks;
create policy hidden_tasks_user_policy on public.user_hidden_tasks
    for all to authenticated
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

grant select, insert, delete on public.user_hidden_tasks to authenticated;
grant all on public.user_hidden_tasks to service_role;

create table if not exists public.user_hidden_notes (
    user_id uuid not null references auth.users(id) on delete cascade,
    note_id uuid not null references public.notes(id) on delete cascade,
    hidden_at timestamptz not null default now(),
    primary key (user_id, note_id)
);

alter table public.user_hidden_notes enable row level security;

drop policy if exists hidden_notes_user_policy on public.user_hidden_notes;
create policy hidden_notes_user_policy on public.user_hidden_notes
    for all to authenticated
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

grant select, insert, delete on public.user_hidden_notes to authenticated;
grant all on public.user_hidden_notes to service_role;

-- 4. CANONICAL SUBJECT SYLLABUSES & REUSABLE ROADMAPS
create table if not exists public.subject_syllabuses (
    id uuid primary key default gen_random_uuid(),
    course_code text not null unique,
    course_title text not null,
    syllabus_text text not null,
    file_path text,
    version int not null default 1,
    content_hash text not null default '',
    uploaded_by uuid references auth.users(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_subject_syllabuses_code on public.subject_syllabuses(course_code);
alter table public.subject_syllabuses enable row level security;

drop policy if exists subject_syllabuses_read on public.subject_syllabuses;
create policy subject_syllabuses_read on public.subject_syllabuses
    for select to authenticated
    using (true);

drop policy if exists subject_syllabuses_write on public.subject_syllabuses;
create policy subject_syllabuses_write on public.subject_syllabuses
    for all to authenticated
    using (
        not (select restrict_notes from public.app_restrictions where id = 1)
        or public.is_moderator_or_admin()
    )
    with check (
        not (select restrict_notes from public.app_restrictions where id = 1)
        or public.is_moderator_or_admin()
    );

grant select, insert, update on public.subject_syllabuses to authenticated;
grant all on public.subject_syllabuses to service_role;

create table if not exists public.subject_roadmaps (
    id uuid primary key default gen_random_uuid(),
    course_code text not null unique,
    syllabus_version int not null default 1,
    modules jsonb not null default '[]'::jsonb check (jsonb_typeof(modules) = 'array'),
    generated_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_subject_roadmaps_code on public.subject_roadmaps(course_code);
alter table public.subject_roadmaps enable row level security;

drop policy if exists subject_roadmaps_read on public.subject_roadmaps;
create policy subject_roadmaps_read on public.subject_roadmaps
    for select to authenticated
    using (true);

drop policy if exists subject_roadmaps_write on public.subject_roadmaps;
create policy subject_roadmaps_write on public.subject_roadmaps
    for all to authenticated
    using (true)
    with check (true);

grant select, insert, update on public.subject_roadmaps to authenticated;
grant all on public.subject_roadmaps to service_role;

-- 5. PERSISTENT QUIZ ATTEMPTS & ANALYTICS
create table if not exists public.quiz_attempts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    course_code text not null,
    module_title text not null default '',
    topic_title text not null default '',
    difficulty text not null check (difficulty in ('Easy', 'Medium', 'Hard')),
    total_questions int not null,
    correct_answers int not null,
    score int not null default 0,
    accuracy_percent int not null default 0,
    questions_data jsonb not null default '[]'::jsonb,
    created_at timestamptz not null default now()
);

create index if not exists idx_quiz_attempts_user on public.quiz_attempts(user_id, created_at desc);
create index if not exists idx_quiz_attempts_course on public.quiz_attempts(course_code);
alter table public.quiz_attempts enable row level security;

drop policy if exists quiz_attempts_read on public.quiz_attempts;
create policy quiz_attempts_read on public.quiz_attempts
    for select to authenticated
    using (true);

drop policy if exists quiz_attempts_insert on public.quiz_attempts;
create policy quiz_attempts_insert on public.quiz_attempts
    for insert to authenticated
    with check (user_id = auth.uid());

grant select, insert on public.quiz_attempts to authenticated;
grant all on public.quiz_attempts to service_role;

-- 6. DOCUMENT SUMMARIES & CORE IDEAS
create table if not exists public.note_summaries (
    id uuid primary key default gen_random_uuid(),
    note_id uuid references public.notes(id) on delete cascade,
    course_code text not null default '',
    title text not null,
    summary_text text not null,
    key_concepts jsonb not null default '[]'::jsonb,
    created_by uuid references auth.users(id),
    created_at timestamptz not null default now()
);

alter table public.note_summaries enable row level security;

drop policy if exists note_summaries_read on public.note_summaries;
create policy note_summaries_read on public.note_summaries
    for select to authenticated
    using (true);

drop policy if exists note_summaries_insert on public.note_summaries;
create policy note_summaries_insert on public.note_summaries
    for insert to authenticated
    with check (true);

grant select, insert on public.note_summaries to authenticated;
grant all on public.note_summaries to service_role;

-- 7. INITIAL ADMIN SEED (OPTIONAL)
-- To make your own account an admin immediately, replace <YOUR_USER_UUID> below:
-- insert into public.user_roles (user_id, role)
-- values ('<YOUR_USER_UUID>', 'admin')
-- on conflict (user_id) do update set role = 'admin';
```
