<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglib.jspf" %>
<c:set var="pageTitle" value="个人中心" />
<%@ include file="/WEB-INF/views/common/head.jspf" %>

<div class="page-head"><span class="eyebrow">我的写作空间 / PERSONAL DESK</span><h1>个人中心</h1><p>收藏日常的片段，整理自己的文字。</p></div>
<div class="card profile-head">
    <c:choose>
        <c:when test="${not empty profileUser.avatar}">
            <img class="avatar avatar-lg" src="${ctx}<c:out value='${profileUser.avatar}' />" alt="头像">
        </c:when>
        <c:otherwise>
            <span class="avatar avatar-lg"><c:out value="${profileUser.initial}" /></span>
        </c:otherwise>
    </c:choose>
    <div style="flex:1 1 auto">
        <div class="name"><c:out value="${profileUser.displayName}" /></div>
        <p class="bio"><c:out value="${profileUser.bio}" /></p>
        <div class="profile-stats">
            <span><b>${fn:length(myPosts)}</b>篇文章</span>
            <span><b><fmt:formatDate value="${profileUser.createdAt}" pattern="yyyy-MM-dd" /></b>加入时间</span>
            <c:if test="${profileUser.admin}"><span class="chip chip-accent">管理员</span></c:if>
        </div>
    </div>
    <div>
        <%-- 头像上传：必须 enctype=multipart/form-data，否则容器收不到文件 --%>
        <form action="${ctx}/profile/avatar" method="post" enctype="multipart/form-data">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}' />">
            <label class="sr-only" for="avatar-file">选择头像图片</label>
            <input id="avatar-file" type="file" name="avatarFile" accept="image/*" required class="input" style="max-width:220px">
            <button class="btn btn-ghost btn-sm" type="submit" style="margin-top:8px">上传头像</button>
        </form>
    </div>
</div>

<div class="layout" style="margin-top:22px">
    <div>
        <div class="section-title" style="margin-top:0">
            <h2>我的文章</h2>
            <a class="btn btn-primary btn-sm" href="${ctx}/post/edit">+ 写文章</a>
        </div>

        <c:choose>
            <c:when test="${empty myPosts}">
                <div class="card empty">
                    <span class="emoji">✍️</span>
                    <h3>还没有写过文章</h3>
                    <p>第一篇往往最难，但写完就顺了。</p>
                    <a class="btn btn-primary" href="${ctx}/post/edit">去写第一篇</a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="card table-wrap" tabindex="0" role="region" aria-label="我的文章表格，可横向滚动">
                    <table class="table">
                        <thead>
                        <tr><th>标题</th><th>状态</th><th>分类</th><th>数据</th><th>时间</th><th>操作</th></tr>
                        </thead>
                        <tbody>
                        <c:forEach var="item" items="${myPosts}">
                            <tr>
                                <td><a href="${ctx}/post?id=${item.id}"><c:out value="${item.title}" /></a></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${item.status == 1}"><span class="chip chip-accent">已发布</span></c:when>
                                        <c:otherwise><span class="chip chip-draft">草稿</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${empty item.categoryName ? '未分类' : item.categoryName}" /></td>
                                <td class="num">👁 ${item.viewCount} · ♡ ${item.likeCount} · 💬 ${item.commentCount}</td>
                                <td class="num"><fmt:formatDate value="${item.createdAt}" pattern="MM-dd HH:mm" /></td>
                                <td style="white-space:nowrap">
                                    <a class="btn btn-sm btn-ghost" href="${ctx}/post/edit?id=${item.id}">编辑</a>
                                    <form action="${ctx}/post/delete" method="post" style="display:inline"
                                          onsubmit="return confirm('确定删除这篇文章吗？删除后不可恢复。');">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}' />">
                                        <input type="hidden" name="id" value="${item.id}">
                                        <button class="btn btn-sm btn-danger" type="submit">删除</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <aside>
        <div class="card side-card">
            <h3>编辑资料</h3>
            <form action="${ctx}/profile" method="post">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}' />">
                <div class="field">
                    <label for="nickname">昵称</label>
                    <input class="input" id="nickname" name="nickname" maxlength="32" required
                           value="<c:out value='${profileUser.nickname}' />">
                </div>
                <div class="field">
                    <label for="email">邮箱</label>
                    <input class="input" id="email" name="email" type="email" maxlength="128"
                           value="<c:out value='${profileUser.email}' />">
                </div>
                <div class="field">
                    <label for="bio">简介</label>
                    <textarea class="textarea" id="bio" name="bio" rows="4" maxlength="255"><c:out value="${profileUser.bio}" /></textarea>
                </div>
                <button class="btn btn-primary btn-block" type="submit">保存资料</button>
            </form>
        </div>

        <div class="card side-card">
            <h3>账号信息</h3>
            <p style="font-size:13px;color:var(--muted);margin:0">
                登录名：<c:out value="${profileUser.username}" /><br>
                角色：${profileUser.admin ? '管理员' : '普通用户'}<br>
                注册时间：<fmt:formatDate value="${profileUser.createdAt}" pattern="yyyy-MM-dd HH:mm" />
            </p>
        </div>
    </aside>
</div>

<%@ include file="/WEB-INF/views/common/foot.jspf" %>