from pathlib import Path
p=Path('supabase/functions/studysync-session/index.ts');s=p.read_text().replace(",diagnostic:stage==='VTOP connection' && error instanceof Error ? error.message.slice(0,400) : undefined",'');p.write_text(s)
# Remove obsolete secret-bearing source backup copies, replacing only the key assignment.
for p in Path('.artifacts').rglob('SupabaseConfig.kt'):
    p.write_text('package com.studysync.app.data.config\nobject SupabaseConfig { const val SUPABASE_URL = "https://pkccuhjqxgqpyrtiykse.supabase.co/"; const val SUPABASE_KEY = "sb_publishable_D8Fqh6YuXcV8ZZwiQ1DXXA_-gRWkOlB" }\n')
