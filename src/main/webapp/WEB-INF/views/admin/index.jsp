<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglib.jspf" %>
<c:set var="pageTitle" value="后台管理" />
<%@ include file="/WEB-INF/views/common/head.jspf" %>

<div class="page-head">
    <span class="eyebrow">站务工作台 / EDITOR'S DESK</span>
    <h1>后台管理</h1>
    <p>仅管理员可见。可以管理全部文章、用户与评论。</p>
</div>

<div class="stat-grid">
    <div class="stat-box"><b>${applicationScope.statUsers}</b><span>注册用户</span></div>
    <div class="stat-box"><b>${applicationScope.statPosts}</b><span>已发布文章</span></div>
    <div class="stat-box"><b>${applicationScope.statCategories}</b><span>分类数量</span></div>
    <div class="stat-box"><b>${applicationScope.onlineCount}</b><span>当前在线</span></div>
    <div class="stat-box"><b>${applicationScope.pageViews}</b><span>累计访问</span></div>
</div>

<div class="section-title">
    <h2>全部文章（含草稿）</h2>
    <span class="count">共 ${page.total} 篇</span>
</div>
<div class="card table-wrap" tabindex="0" role="region" aria-label="全部文章表格，可横向滚动">
    <table class="table">
        <thead><tr><th>#</th><th>标题</th><th>作者</th><th>分类</th><th>状态</th><th>数据</th><th>操作</th></tr></thead>
        <tbody>
        <c:forEach var="item" items="${page.list}">
            <tr>
                <td class="num">${item.id}</td>
                <td><a href="${ctx}/post?id=${item.id}"><c:out value="${item.title}" /></a></td>
                <td><c:out value="${item.authorDisplayName}" /></td>
                <td><c:out value="${empty item.categoryName ? '-' : item.categoryName}" /></td>
                <td>${item.status == 1 ? '已发布' : '草稿'}</td>
                <td class="num">👁 ${item.viewCount} · ♡ ${item.likeCount} · 💬 ${item.commentCount}</td>
                <td style="white-space:nowrap">
                    <a class="btn btn-sm btn-ghost" href="${ctx}/post/edit?id=${item.id}">编辑</a>
                    <form action="${ctx}/post/delete" method="post" style="display:inline"
                          onsubmit="return confirm('管理员删除文章，确定吗？');">
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

<c:if test="${page.totalPages > 1}">
    <nav class="pagination">
        <c:if test="${page.hasPrev}"><a href="${ctx}/admin?page=${page.prevPage}">‹ 上一页</a></c:if>
        <span class="current">${page.page} / ${page.totalPages}</span>
        <c:if test="${page.hasNext}"><a href="${ctx}/admin?page=${page.nextPage}">下一页 ›</a></c:if>
    </nav>
</c:if>

<div class="layout" style="margin-top:8px">
    <div>
        <div class="section-title"><h2>最新评论</h2></div>
        <div class="card table-wrap" tabindex="0" role="region" aria-label="最新评论表格，可横向滚动">
            <table class="table">
                <thead><tr><th>内容</th><th>评论人</th><th>所属文章</th><th>时间</th></tr></thead>
                <tbody>
                <c:forEach var="cm" items="${latestComments}">
                    <tr>
                        <td style="max-width:320px"><c:out value="${cm.content}" /></td>
                        <td><c:out value="${cm.displayName}" /></td>
                        <td><a href="${ctx}/post?id=${cm.postId}">#${cm.postId}</a></td>
                        <td class="num"><fmt:formatDate value="${cm.createdAt}" pattern="MM-dd HH:mm" /></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty latestComments}"><tr><td colspan="4" class="num">暂无评论</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </div>

    <aside>
        <div class="card side-card">
            <h3>注册用户</h3>
            <table class="table">
                <tbody>
                <c:forEach var="u" items="${users}">
                    <tr>
                        <td>
                            <div style="display:flex;align-items:center;gap:8px">
                                <span class="avatar avatar-sm"><c:out value="${u.initial}" /></span>
                                <span><c:out value="${u.displayName}" /></span>
                            </div>
                        </td>
                        <td class="num">${u.postCount} 篇</td>
                        <td>${u.admin ? '管理员' : '用户'}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </aside>
</div>

<%@ include file="/WEB-INF/views/common/foot.jspf" %>