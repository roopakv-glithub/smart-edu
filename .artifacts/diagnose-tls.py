from pathlib import Path
p=Path('supabase/functions/studysync-session/index.ts');s=p.read_text().replace('error:`Could not complete ${stage}. Retry or sign in again.`','error:`Could not complete ${stage}. Retry or sign in again.`,diagnostic:stage===\'VTOP connection\' && error instanceof Error ? error.message.slice(0,400) : undefined');p.write_text(s)
