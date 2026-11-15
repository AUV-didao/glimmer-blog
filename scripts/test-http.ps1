<#
  微光博客 · HTTP 自动化回归脚本
  用法：
    pwsh -File scripts/test-http.ps1
    pwsh -File scripts/test-http.ps1 -BaseUrl http://127.0.0.1:8082/blog -Database .\data\blog.db
  说明：
    * 需要应用已启动（Tomcat，context=/blog）；
    * 数据库断言通过 Python 标准库只读查询 SQLite，无需安装数据库客户端；
    * 每次运行都会往库里写测试数据，跑完建议执行 scripts/reset-db.ps1 -Force。
#>
param(
    [string]$BaseUrl = 'http://127.0.0.1:8082/blog',
    [string]$Database = (Join-Path $PSScriptRoot '..\data\blog.db')
)

$ErrorActionPreference = 'Stop'
$base = $BaseUrl
$Database = [System.IO.Path]::GetFullPath($Database)
$SqliteQuery = Join-Path $PSScriptRoot 'sqlite-query.py'
if (-not (Test-Path $Database -PathType Leaf)) { throw "SQLite 数据库不存在：$Database" }
if (-not (Test-Path $SqliteQuery -PathType Leaf)) { throw "SQLite 查询脚本不存在：$SqliteQuery" }
$results = New-Object System.Collections.ArrayList
function Check($name, $ok, $detail) {
    [void]$results.Add([pscustomobject]@{ Test = $name; Pass = [bool]$ok; Detail = $detail })
}
# ---------- CSRF 辅助 ----------
function Get-Csrf($session) {
    # 页头 <meta name="csrf-token"> 由 head.jspf 输出，与浏览器里 JS 取到的是同一个值
    try {
        $r = Invoke-WebRequest -Uri "$base/" -WebSession $session -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25
        $m = [regex]::Match($r.Content, 'name="csrf-token" content="([0-9a-f]{16,})"')
        if ($m.Success) { return $m.Groups[1].Value }
    } catch { }
    return $null
}
# 先 GET 一次页面，让服务端下发会话与 CSRF 令牌（否则直接 POST 会被 403 拦下）
function Prime-Session($session) {
    if (-not (Get-Csrf $session)) {
        [void](Invoke-WebRequest -Uri "$base/" -WebSession $session -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25)
    }
}
function Post-NoCsrf($url, $body, $session) {
    $r = Invoke-WebRequest -Uri $url -Method Post -Body $body -WebSession $session -UseBasicParsing `
        -SkipHttpErrorCheck -TimeoutSec 25
    return [pscustomobject]@{
        StatusCode = [int]$r.StatusCode
        FinalUrl   = [string]$r.BaseResponse.RequestMessage.RequestUri
        Content    = [string]$r.Content
        Headers    = $r.Headers
    }
}
function Post-Form($url, $body, $session) {
    Prime-Session $session
    $token = Get-Csrf $session
    if ($token) { $body['_csrf'] = $token }
    $r = Invoke-WebRequest -Uri $url -Method Post -Body $body -WebSession $session -UseBasicParsing `
        -SkipHttpErrorCheck -TimeoutSec 25
    return [pscustomobject]@{
        StatusCode = [int]$r.StatusCode
        FinalUrl   = [string]$r.BaseResponse.RequestMessage.RequestUri
        Content    = [string]$r.Content
        Headers    = $r.Headers
    }
}
function Get-Page($url, $session) {
    $r = Invoke-WebRequest -Uri $url -Method Get -WebSession $session -UseBasicParsing `
        -SkipHttpErrorCheck -TimeoutSec 25
    return [pscustomobject]@{
        StatusCode = [int]$r.StatusCode
        FinalUrl   = [string]$r.BaseResponse.RequestMessage.RequestUri
        Content    = [string]$r.Content
        Headers    = $r.Headers
    }
}

function Sql($q) {
    $out = & python $SqliteQuery $Database $q 2>$null
    if ($LASTEXITCODE -ne 0) { throw "SQLite 查询失败：$q" }
    return $out
}

# ---------- 1. 游客访问 / 拦截 ----------
$guest = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$r = Get-Page "$base/" $guest
Check '游客访问首页 200' ($r.StatusCode -eq 200) "status=$($r.StatusCode)"

$r = Get-Page "$base/post/edit" $guest
Check '未登录访问发文页被送去登录' ($r.FinalUrl -like '*login*') "final=$($r.FinalUrl)"

# ---------- 2. 密码错误 ----------
$bad = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$r = Post-Form "$base/login" @{ username = 'admin'; password = 'wrongpass' } $bad
Check '错误密码不通过' ($r.Content -like '*用户名或密码不正确*') "status=$($r.StatusCode)"
$r = Get-Page "$base/profile" $bad
Check '错误密码后仍无法进入个人中心' ($r.FinalUrl -like '*login*') "final=$($r.FinalUrl)"

# ---------- 3. 管理员登录 ----------
$admin = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$r = Post-Form "$base/login" @{ username = 'admin'; password = '123456'; next = '/' } $admin
Check '管理员登录成功后回到首页' ($r.FinalUrl -match '/blog/?$') "final=$($r.FinalUrl)"
$r = Get-Page "$base/profile" $admin
Check '登录后个人中心显示昵称' ($r.StatusCode -eq 200 -and $r.Content -like '*站长微光*') "status=$($r.StatusCode)"

# ---------- 4. 发文（含标签、分类） ----------
$title = '自动化测试文章 ' + (Get-Date -Format 'HHmmss')
$r = Post-Form "$base/post/save" @{
    title = $title; summary = '测试脚本创建'; content = '正文：验证 Servlet、JSP、JDBC 是否正常工作。'
    categoryId = '1'; tags = 'Servlet 测试'; status = '1'
} $admin
$newId = if ($r.FinalUrl -match 'id=(\d+)') { [int]$Matches[1] } else { 0 }
Check '发文成功并跳到详情页' ($newId -gt 0) "final=$($r.FinalUrl)"
$r = Get-Page "$base/post?id=$newId" $admin
Check '新文章详情页可打开' ($r.StatusCode -eq 200 -and $r.Content -like "*$title*") "status=$($r.StatusCode) id=$newId"

$badCoverTitle = '危险封面地址测试 ' + (Get-Date -Format 'HHmmss')
$r = Post-Form "$base/post/save" @{ title = $badCoverTitle; content = '用于验证危险协议不会被写入文章封面字段。'; coverUrl = 'javascript:alert(1)'; status = '1' } $admin
$badCoverRows = [int]((Sql "SELECT COUNT(*) FROM posts WHERE title='$badCoverTitle';") -join '')
Check '危险 coverUrl 被拒绝且未写库' ($r.Content -like '*封面地址只支持*' -and $badCoverRows -eq 0) "status=$($r.StatusCode) rows=$badCoverRows"
# ---------- 5. 服务端校验 ----------
$r = Post-Form "$base/post/save" @{ title = 'a'; content = '内容太短也不行'; status = '1' } $admin
Check '标题过短被服务端拦下' ($r.Content -like '*标题至少*') "status=$($r.StatusCode)"
$r = Post-Form "$base/register" @{ username = 'ab'; password = '123456'; password2 = '123456' } $guest
Check '用户名过短注册被拒' ($r.Content -like '*用户名需为*') "status=$($r.StatusCode)"

# ---------- 6. 评论 ----------
$r = Post-Form "$base/comment/add" @{ postId = "$newId"; content = '来自测试脚本的评论' } $admin
Check '评论提交后跳回详情页' ($r.FinalUrl -like "*post?id=$newId*") "final=$($r.FinalUrl)"
$r = Get-Page "$base/post?id=$newId" $admin
Check '评论出现在详情页' ($r.Content -like '*来自测试脚本的评论*') ''

# ---------- 7. 点赞（AJAX） ----------
$r = Invoke-WebRequest -Uri "$base/post/like" -Method Post -Body @{ id = "$newId"; _csrf = (Get-Csrf $admin) } -WebSession $admin `
    -Headers @{ 'X-Requested-With' = 'XMLHttpRequest' } -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25
Check 'AJAX 点赞返回 liked=true' ($r.Content -like '*"liked":true*') $r.Content
$r = Invoke-WebRequest -Uri "$base/post/like" -Method Post -Body @{ id = "$newId"; _csrf = (Get-Csrf $admin) } -WebSession $admin `
    -Headers @{ 'X-Requested-With' = 'XMLHttpRequest' } -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25
Check '重复点赞变成取消点赞' ($r.Content -like '*"liked":false*') $r.Content
$r = Invoke-WebRequest -Uri "$base/post/like" -Method Post -Body @{ id = "$newId" } -WebSession $guest `
    -Headers @{ 'X-Requested-With' = 'XMLHttpRequest' } -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25
Check '未登录点赞返回 401' ([int]$r.StatusCode -eq 401) "status=$($r.StatusCode)"

# ---------- 8. 导出下载 / 上传 ----------
$r = Invoke-WebRequest -Uri "$base/download?scope=post&id=$newId" -WebSession $admin -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25
$disp = @($r.Headers['Content-Disposition']) -join ''
Check '导出文章为附件下载' ($r.StatusCode -eq 200 -and $disp -like '*attachment*') "disp=$disp"
Check '导出内容含正文' ([string]$r.Content -like '*验证 Servlet*') ''

$img = Join-Path $env:PI_SCRATCH_DIR 'cover.png'
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap 320, 200
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::FromArgb(47, 110, 95))
$g.DrawString('MIGUANG BLOG', (New-Object System.Drawing.Font('Arial', 18)), [System.Drawing.Brushes]::White, 40, 80)
$bmp.Save($img, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
$img2 = Join-Path $env:PI_SCRATCH_DIR 'cover2.png'
Copy-Item $img $img2 -Force

$up = Invoke-WebRequest -Uri "$base/profile/avatar" -Method Post -WebSession $admin -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25 `
    -Form @{ avatarFile = Get-Item $img; _csrf = (Get-Csrf $admin) }
Check '头像上传被接受' ([int]$up.StatusCode -eq 302 -or [int]$up.StatusCode -eq 200) "status=$($up.StatusCode)"
$r = Get-Page "$base/profile" $admin
$avatarSrc = if ($r.Content -match 'src="([^"]*upload/avatar/[^"]+)"') { $Matches[1] } else { '' }
Check '头像上传后页面出现 /upload/avatar/' ($avatarSrc -ne '') "src=$avatarSrc"
$origin = ([uri]$base).GetLeftPart([System.UriPartial]::Authority)
if ($avatarSrc) {
    $avatarUrl = if ($avatarSrc -match '^https?://') { $avatarSrc } else { "$origin$avatarSrc" }
    $f = Invoke-WebRequest -Uri $avatarUrl -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25
    Check '头像文件可直接下载' ([int]$f.StatusCode -eq 200 -and [int]$f.RawContentLength -gt 0) "status=$([int]$f.StatusCode) bytes=$($f.RawContentLength)"
}
$r = Invoke-WebRequest -Uri "$base/post/save" -Method Post -WebSession $admin -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25 -Form @{
    title = '带封面的测试文章'; summary = '封面测试'; content = '这篇用于验证封面图上传链路是否正常。'
    categoryId = '2'; tags = '测试'; status = '1'; coverFile = Get-Item $img2; _csrf = (Get-Csrf $admin)
}
$coverId = if ($r.BaseResponse.RequestMessage.RequestUri -match 'id=(\d+)') { $Matches[1] } else { 0 }
$r = Get-Page "$base/post?id=$coverId" $admin
Check '封面图上传并显示' ($coverId -gt 0 -and $r.Content -like '*/upload/cover/*') "id=$coverId"
if ($coverId -gt 0) {
    $coverBefore = [regex]::Match($r.Content, '/blog/upload/cover/[^"''\s>]+').Value
    $r = Invoke-WebRequest -Uri "$base/post/edit?id=$coverId" -WebSession $admin -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25
    $coverFormValue = $coverBefore -replace '^/blog', ''
$img3 = Join-Path $env:PI_SCRATCH_DIR 'cover3.png'
Copy-Item $img $img3 -Force
    $r = Invoke-WebRequest -Uri "$base/post/save" -Method Post -WebSession $admin -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25 -Form @{
        id = "$coverId"; title = '带封面的测试文章'; summary = '封面测试'; content = '这篇用于验证封面图上传链路是否正常。'
        categoryId = '2'; tags = '测试'; status = '1'; coverFile = Get-Item $img3; coverUrl = $coverFormValue; _csrf = (Get-Csrf $admin)
    }
    $r = Get-Page "$base/post?id=$coverId" $admin
    $coverAfter = [regex]::Match($r.Content, '/blog/upload/cover/[^"''\s>]+').Value
    Check '已有封面文章重新上传后切换到新文件' ($coverAfter -and $coverAfter -ne $coverBefore) "before=$coverBefore after=$coverAfter"
}

# ---------- 9. 权限 ----------
$lin = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$r = Post-Form "$base/login" @{ username = 'lin'; password = '123456' } $lin
Check '普通用户登录成功' ($r.FinalUrl -match '/blog/?$') "final=$($r.FinalUrl)"
$r = Get-Page "$base/admin" $lin
Check '普通用户访问后台 403' ($r.StatusCode -eq 403) "status=$($r.StatusCode)"
$r = Get-Page "$base/admin" $admin
Check '管理员访问后台 200' ($r.StatusCode -eq 200 -and $r.Content -like '*后台管理*') "status=$($r.StatusCode)"
# 普通用户不得编辑管理员文章；管理员可编辑普通用户文章。
$r = Get-Page "$base/post/edit?id=$newId" $lin
Check '普通用户不能编辑他人文章' ($r.StatusCode -eq 403) "status=$($r.StatusCode)"
$linPostTitle = '普通用户文章 ' + (Get-Date -Format 'HHmmss')
$r = Post-Form "$base/post/save" @{ title = $linPostTitle; summary = '权限回归'; content = '这是普通用户创建的文章，用于管理员权限测试。'; status = '1' } $lin
$linPostId = if ($r.FinalUrl -match 'id=(\d+)') { [int]$Matches[1] } else { 0 }
$r = Get-Page "$base/post/edit?id=$linPostId" $admin
Check '管理员可打开他人文章编辑页' ($linPostId -gt 0 -and $r.StatusCode -eq 200) "id=$linPostId status=$($r.StatusCode)"
if ($linPostId -gt 0) {
    $r = Post-Form "$base/post/save" @{ id = "$linPostId"; title = "$linPostTitle 管理员修改"; content = '管理员修改普通用户文章，用于验证权限分支。'; status = '1' } $admin
    $r = Get-Page "$base/post?id=$linPostId" $admin
    Check '管理员成功保存他人文章修改' ($r.Content -like '*管理员修改*') "id=$linPostId"
}
# 点赞撤销后关联表与计数必须一致。
$likeRows = [int]((Sql "SELECT COUNT(*) FROM post_likes WHERE post_id=$newId AND user_id=1;") -join '')
$likeCount = [int]((Sql "SELECT like_count FROM posts WHERE id=$newId;") -join '')
Check '点赞/取消后关联与计数保持一致' ($likeRows -eq 0 -and $likeCount -eq 0) "rows=$likeRows count=$likeCount"
$badId = Invoke-WebRequest -Uri "$base/post/like" -Method Post -Body @{ id = 'not-a-number'; _csrf = (Get-Csrf $admin) } -WebSession $admin -Headers @{ 'X-Requested-With' = 'XMLHttpRequest' } -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25
Check '点赞非法数字 ID 返回 400 而非 500' ([int]$badId.StatusCode -eq 400) "status=$([int]$badId.StatusCode)"
$draftResp = Post-Form "$base/post/save" @{ title = '导出权限草稿'; content = '此草稿用于验证导出权限拒绝逻辑。'; status = '0' } $lin
$draftId = if ($draftResp.FinalUrl -match 'id=(\d+)') { [int]$Matches[1] } else { 0 }
$draftExport = Invoke-WebRequest -Uri "$base/download?scope=post&id=$draftId" -WebSession $guest -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25
Check '游客导出草稿被拒绝' ($draftId -gt 0 -and [int]$draftExport.StatusCode -eq 403) "id=$draftId status=$([int]$draftExport.StatusCode)"
if ($linPostId -gt 0) {
    [void](Post-Form "$base/post/delete" @{ id = "$linPostId" } $admin)
    $gone = Get-Page "$base/post?id=$linPostId" $guest
    Check '管理员可删除他人文章' ($gone.StatusCode -eq 404) "id=$linPostId status=$($gone.StatusCode)"
}
# 普通用户删除管理员文章应失败。
[void](Post-Form "$base/post/delete" @{ id = "$newId" } $lin)
$r = Get-Page "$base/post?id=$newId" $guest
Check '普通用户删不掉管理员文章' ($r.StatusCode -eq 200 -and $r.Content -like "*$title*") "status=$($r.StatusCode)"

# ---------- 10. 搜索 / 分类 / 分页 ----------
$r = Get-Page "$base/?keyword=Servlet" $guest
Check '搜索命中' ($r.StatusCode -eq 200 -and $r.Content -like '*搜索：*') "status=$($r.StatusCode)"
$r = Get-Page "$base/?tagId=1" $guest
Check '按标签筛选可用' ($r.StatusCode -eq 200) "status=$($r.StatusCode)"
$r = Get-Page "$base/?page=2" $guest
Check '第二页可访问' ($r.StatusCode -eq 200) "status=$($r.StatusCode)"

# ---------- 11. 错误页与退出 ----------
$r = Get-Page "$base/post?id=99999" $guest
Check '不存在的文章返回 404' ($r.StatusCode -eq 404) "status=$($r.StatusCode)"
$oldSid = ($admin.Cookies.GetCookies($base) | Where-Object Name -eq 'JSESSIONID').Value
$r = Post-Form "$base/logout" @{} $admin
$replay = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$replay.Cookies.Add((New-Object System.Net.Cookie('JSESSIONID', $oldSid, '/blog', '127.0.0.1')))
$r = Get-Page "$base/profile" $replay
Check '退出后旧 sessionId 立即失效' ($r.FinalUrl -like '*login*') "final=$($r.FinalUrl) oldSid=$oldSid"

# ============================================================
#  以下为「Cookie 记住我」与「二级回复 + 站内通知」的补充用例
# ============================================================
function New-Session($user, $pwd, [bool]$remember) {
    $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $body = @{ username = $user; password = $pwd; next = '/' }
    if ($remember) { $body['remember'] = '1' }
    [void](Post-Form "$base/login" $body $s)
    return $s
}

# ---------- 12. Cookie：记住我 / 自动登录 ----------
$rem = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$r = Post-Form "$base/login" @{ username = 'lin'; password = '123456'; remember = '1'; next = '/' } $rem
$names = ($rem.Cookies.GetCookies($base) | ForEach-Object Name) -join ','
Check '勾选记住我 → 响应里带回 blog_remember Cookie' ($names -like '*blog_remember*') "cookies=$names"

$token = ($rem.Cookies.GetCookies($base) | Where-Object Name -eq 'blog_remember').Value
Check '令牌格式为 userId.expireAt.HMAC-SHA256' ($token -match '^\d+\.\d+\.[0-9a-f]{64}$') "token=${token}"

# 模拟「关浏览器再打开」：新会话只有这张持久 Cookie，没有 JSESSIONID
$fresh = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$fresh.Cookies.Add((New-Object System.Net.Cookie('blog_remember', $token, '/blog', '127.0.0.1')))
$r = Get-Page "$base/profile" $fresh
Check '仅凭记住我 Cookie 就能自动登录（Filter 顺序正确）' ($r.StatusCode -eq 200 -and $r.Content -like '*小林同学*') "status=$($r.StatusCode) final=$($r.FinalUrl)"

$tampered = $token -replace '\.[0-9a-f]{8}', '.deadbeef'
$fake = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$fake.Cookies.Add((New-Object System.Net.Cookie('blog_remember', $tampered, '/blog', '127.0.0.1')))
$r = Get-Page "$base/profile" $fake
Check '篡改签名的 Cookie 无法自动登录' ($r.FinalUrl -like '*login*') "final=$($r.FinalUrl)"

$forgedId = $token -replace '^\d+', '1'
$fake2 = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$fake2.Cookies.Add((New-Object System.Net.Cookie('blog_remember', $forgedId, '/blog', '127.0.0.1')))
$r = Get-Page "$base/profile" $fake2
Check '伪造 userId 冒充他人登录失败' ($r.FinalUrl -like '*login*') "final=$($r.FinalUrl)"

[void](Post-Form "$base/logout" @{} $fresh)
$r = Get-Page "$base/profile" $fresh
Check '退出登录后记住我 Cookie 已失效' ($r.FinalUrl -like '*login*') "final=$($r.FinalUrl)"

$noRem = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$r = Post-Form "$base/login" @{ username = 'lin'; password = '123456'; next = '/' } $noRem
$names = ($noRem.Cookies.GetCookies($base) | ForEach-Object Name) -join ','
Check '不勾记住我 → 不下发持久 Cookie' ($names -notlike '*blog_remember*') "cookies=$names"

# ---------- 13. 二级评论回复 + 站内通知 ----------
$lin = New-Session 'lin' '123456' $false
$adm = New-Session 'admin' '123456' $false

$c1 = '测试一级评论：中文编码校验 ' + (Get-Date -Format 'HHmmss')
$r = Post-Form "$base/comment/add" @{ postId = '1'; parentId = '0'; content = $c1 } $lin
Check '发表一级评论成功' ($r.StatusCode -eq 200 -and $r.Content -like "*$c1*") "status=$($r.StatusCode)"

$cid1 = (Sql "SELECT id FROM comments WHERE post_id=1 ORDER BY id DESC LIMIT 1;") -join ''
$c2 = '测试二级回复：收到通知了吗 ' + (Get-Date -Format 'HHmmss')
$r = Post-Form "$base/comment/add" @{ postId = '1'; parentId = $cid1; content = $c2 } $adm
Check '回复指定评论成功（渲染出二级回复）' ($r.Content -like '*reply-item*' -and $r.Content -like "*$c2*") "status=$($r.StatusCode)"
Check '二级回复显示「回复 @被回复人」' ($r.Content -like '*reply-to*') "contains reply-to"

$r = Get-Page "$base/notice" $lin
Check '被回复者收到 REPLY 通知' ($r.Content -like '*回复了你的评论*' -and $r.Content -like "*$c2*") "status=$($r.StatusCode)"
Check '通知中心显示未读标记' ($r.Content -like '*dot-unread*') "has dot-unread"
$r = Get-Page "$base/" $lin
Check '页头铃铛出现未读数徽标' ($r.Content -like '*notice-dot*') "has badge"

$unreadBefore = [int]((Sql "SELECT COUNT(*) FROM notifications WHERE user_id=2 AND is_read=0;") -join '')
$nid = (Sql "SELECT id FROM notifications WHERE user_id=2 AND is_read=0 ORDER BY id DESC LIMIT 1;") -join ''
[void](Post-Form "$base/notice/read" @{ id = $nid } $lin)
$unreadAfter = [int]((Sql "SELECT COUNT(*) FROM notifications WHERE user_id=2 AND is_read=0;") -join '')
Check '标记单条已读后未读数 -1' ($unreadAfter -eq $unreadBefore - 1) "before=$unreadBefore after=$unreadAfter"
[void](Post-Form "$base/notice/read" @{ all = '1' } $lin)
$r = Get-Page "$base/notice" $lin
Check '全部标为已读后不再有未读标记' ($r.Content -notlike '*dot-unread*' -and $r.Content -notlike '*notice-dot*') "status=$($r.StatusCode)"

# 回复他人文章但 parentId 指向别的文章的评论 → 必须拒绝
$cidOtherPost = (Sql "SELECT id FROM comments WHERE post_id=4 ORDER BY id LIMIT 1;") -join ''
$before = [int]((Sql "SELECT COUNT(*) FROM comments WHERE post_id=1;") -join '')
$r = Post-Form "$base/comment/add" @{ postId = '1'; parentId = $cidOtherPost; content = '越界回复测试' } $lin
$after = [int]((Sql "SELECT COUNT(*) FROM comments WHERE post_id=1;") -join '')
Check '跨文章伪造 parentId 被拒绝' ($r.Content -like '*要回复的评论不存在*' -and $before -eq $after) "before=$before after=$after"

# 越权标记别人的通知
$admNid = (Sql "SELECT id FROM notifications WHERE user_id=1 ORDER BY id DESC LIMIT 1;") -join ''
$beforeRead = (Sql "SELECT is_read FROM notifications WHERE id=$admNid;") -join ''
$r = Post-Form "$base/notice/read" @{ id = $admNid } $lin
$afterRead = (Sql "SELECT is_read FROM notifications WHERE id=$admNid;") -join ''
Check '普通用户改不动别人的通知状态' ($r.StatusCode -eq 200 -and [string]$afterRead -eq [string]$beforeRead) "noticeId=$admNid before=$beforeRead after=$afterRead"

$anon = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$r = Get-Page "$base/notice" $anon
Check '游客访问通知中心被送去登录' ($r.StatusCode -eq 200 -and $r.FinalUrl -like '*login*') "final=$($r.FinalUrl)"
# ---------- 14. CSRF 防护 ----------
# 不带令牌的 POST：应被 CsrfFilter 在入口拦下（业务层根本没执行）
$bare = New-Object Microsoft.PowerShell.Commands.WebRequestSession
[void](Invoke-WebRequest -Uri "$base/login" -WebSession $bare -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25)

$r = Post-NoCsrf "$base/login" @{ username = 'admin'; password = '123456' } $bare
Check '缺少 CSRF 令牌的登录提交被拒绝' ($r.StatusCode -eq 403) "status=$($r.StatusCode)"

$csrfTest = New-Object Microsoft.PowerShell.Commands.WebRequestSession
[void](Post-Form "$base/login" @{ username = 'admin'; password = '123456' } $csrfTest)
$r = Post-NoCsrf "$base/comment/add" @{ postId = '1'; content = '没有令牌的评论' } $csrfTest
Check '已登录但缺少令牌无法发表评论' ($r.StatusCode -eq 403) "status=$($r.StatusCode)"

$r = Post-NoCsrf "$base/login" @{ username = 'admin'; password = '123456'; _csrf = 'deadbeef' } $bare
Check '错误令牌同样被拒绝' ($r.StatusCode -eq 403) "status=$($r.StatusCode)"

# 跨会话盗用令牌：A 会话的令牌放到 B 会话里必须失败
$other = New-Object Microsoft.PowerShell.Commands.WebRequestSession
[void](Invoke-WebRequest -Uri "$base/login" -WebSession $other -UseBasicParsing -SkipHttpErrorCheck -TimeoutSec 25)
$stolen = Get-Csrf $other
$r = Post-NoCsrf "$base/login" @{ username = 'admin'; password = '123456'; _csrf = $stolen } $bare
Check '别的会话的令牌不能用' ($r.StatusCode -eq 403) "status=$($r.StatusCode)"

# 正例：带正确令牌能正常登录
$okSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$r = Post-Form "$base/login" @{ username = 'lin'; password = '123456'; next = '/' } $okSession
Check '带正确令牌可以正常登录' ($r.StatusCode -eq 200 -and $r.FinalUrl -match '/blog/?$') "final=$($r.FinalUrl)"

# GET /logout 不再改状态（防止被 <img src> 静默踢下线）
$r = Get-Page "$base/logout" $okSession
$r = Get-Page "$base/profile" $okSession
Check 'GET /logout 不再退出登录' ($r.StatusCode -eq 200 -and $r.Content -like '*小林同学*') "status=$($r.StatusCode) final=$($r.FinalUrl)"
$results | ForEach-Object { "{0} {1}  {2}" -f ($(if ($_.Pass) { '[PASS]' } else { '[FAIL]' })), $_.Test, $_.Detail }
"通过 {0} / {1}" -f ($results | Where-Object Pass).Count, $results.Count
if (($results | Where-Object { -not $_.Pass }).Count -gt 0) { exit 1 }
