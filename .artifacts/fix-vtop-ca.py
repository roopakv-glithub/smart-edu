from pathlib import Path
p=Path('supabase/functions/studysync-session/index.ts');s=p.read_text();s="import { vtopCa } from './vtop-ca.ts';\n"+s
s=s.replace("const url = Deno.env", "// VTOP omits an intermediate CA. This verified CA chain preserves certificate and hostname validation.\nconst vtopClient = Deno.createHttpClient({caCerts:[vtopCa]});\nconst url = Deno.env")
s=s.replace("{headers:{Cookie:cookies},redirect:'manual',signal:AbortSignal.timeout(25000)}", "{client:vtopClient,headers:{Cookie:cookies},redirect:'manual',signal:AbortSignal.timeout(25000)}")
p.write_text(s)
ca=Path('supabase/functions/studysync-session/vtop-ca.pem').read_text()
Path('supabase/functions/studysync-session/vtop-ca.ts').write_text('// Public CA certificates, validated against the Windows trusted root store on 2026-10-03.\nexport const vtopCa = `'+ca+'`;\n')
