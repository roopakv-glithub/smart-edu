$ErrorActionPreference='Stop'
$keys=supabase projects api-keys --project-ref pkccuhjqxgqpyrtiykse --output json | ConvertFrom-Json
$secret=($keys | Where-Object {$_.type -eq 'secret'} | Select-Object -First 1).api_key
if(!$secret){throw 'Modern server key not available'}
$public=($keys | Where-Object {$_.type -eq 'publishable'} | Select-Object -First 1).api_key
$base='https://pkccuhjqxgqpyrtiykse.supabase.co'
$adminHeaders=@{apikey=$secret;Authorization="Bearer $secret"}
$created=$null
try {
 $email=([guid]::NewGuid().ToString())+'@studysync.invalid'
 $created=Invoke-RestMethod "$base/auth/v1/admin/users" -Method Post -Headers $adminHeaders -ContentType 'application/json' -Body (@{email=$email;password=([guid]::NewGuid().ToString()+[guid]::NewGuid().ToString());email_confirm=$true}|ConvertTo-Json)
 $link=Invoke-RestMethod "$base/auth/v1/admin/generate_link" -Method Post -Headers $adminHeaders -ContentType 'application/json' -Body (@{type='magiclink';email=$email}|ConvertTo-Json)
 $auth=Invoke-RestMethod "$base/auth/v1/verify" -Method Post -Headers @{apikey=$public} -ContentType 'application/json' -Body (@{type='magiclink';token_hash=$link.hashed_token}|ConvertTo-Json)
 if(!$auth.access_token){throw 'No app token returned'}
 $snapshot=Invoke-RestMethod "$base/rest/v1/academic_snapshots?select=semester_id" -Headers @{apikey=$public;Authorization="Bearer $($auth.access_token)"}
 Write-Output 'PASS: server identity creation, session exchange and authenticated RLS read with modern keys'
} finally {
 if($created.id){ $null=Invoke-RestMethod "$base/auth/v1/admin/users/$($created.id)" -Method Delete -Headers $adminHeaders;Write-Output 'Temporary auth test account removed' }
 $secret=$null;$keys=$null;$auth=$null;$link=$null
}
