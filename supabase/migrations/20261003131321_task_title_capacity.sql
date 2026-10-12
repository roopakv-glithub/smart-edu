begin;
alter table public.tasks drop constraint tasks_title_check;
alter table public.tasks add constraint tasks_title_check check(length(trim(title)) between 1 and 2000);
commit;
