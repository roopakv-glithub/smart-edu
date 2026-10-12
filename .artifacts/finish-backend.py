from pathlib import Path
p=Path('supabase/functions/studysync-session/index.ts')
s=p.read_text().replace("Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!", "JSON.parse(Deno.env.get('SUPABASE_SECRET_KEYS')!)['default']").replace("Deno.env.get('SUPABASE_ANON_KEY')!", "JSON.parse(Deno.env.get('SUPABASE_PUBLISHABLE_KEYS')!)['default']")
p.write_text(s)
p=Path('supabase/config.toml');s=p.read_text();s+='\n[functions.studysync-session]\nverify_jwt = false\n';p.write_text(s)
# Maintain CLI migration history after applying SQL through the Management API.
files=sorted(Path('supabase/migrations').glob('*.sql'))
sql=['begin;','create schema if not exists supabase_migrations;','create table if not exists supabase_migrations.schema_migrations(version text primary key, statements text[], name text);']
for p in files:
    version,name=p.stem.split('_',1)
    statement=p.read_text().replace("'","''")
    sql.append(f"insert into supabase_migrations.schema_migrations(version,statements,name) values ('{version}',array['{statement}'],'{name}') on conflict(version) do nothing;")
sql.append('commit;')
Path('.artifacts/record-migrations.sql').write_text('\n'.join(sql))
