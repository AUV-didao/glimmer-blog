<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglib.jspf" %>
<c:set var="pageTitle" value="通知中心" />
<%@ include file="/WEB-INF/views/common/head.jspf" %>

<section class="notice-page">
    <div class="notice-head card card-pad">
        <div>
            <span class="eyebrow">读者来信 / CONVERSATIONS</span>
            <h1>通知中心</h1>
            <p class="sub">
                别人评论了你的文章、或者回复了你的评论，都会出现在这里。
                <c:if test="${unreadCount > 0}">当前有 <b>${unreadCount}</b> 条未读。</c:if>
            </p>
        </div>
        <c:if test="${unreadCount > 0}">
            <form action="${ctx}/notice/read" method="post">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}' />">
                <input type="hidden" name="all" value="1">
                <button class="btn btn-ghost btn-sm" type="submit">全部标为已读</button>
            </form>
        </c:if>
    </div>

    <c:choose>
        <c:when test="${empty notices}">
            <div class="card card-pad empty">
                <span class="emoji">🔔</span>
                <h3>还没有通知</h3>
                <p>等你发了文章、有了评论，这里就会热闹起来。</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="notice-list">
                <c:forEach var="n" items="${notices}">
                    <div class="notice-item card ${n.read ? '' : 'is-unread'}">
                        <c:choose>
                            <c:when test="${not empty n.actorAvatar}">
                                <img class="avatar" src="${ctx}<c:out value='${n.actorAvatar}' />" alt="">
                            </c:when>
                            <c:otherwise>
                                <span class="avatar"><c:out value="${n.actorInitial}" /></span>
                            </c:otherwise>
                        </c:choose>
                        <div class="notice-main">
                            <div class="notice-line">
                                <b><c:out value="${n.actorDisplayName}" /></b>
                                <span class="notice-type chip ${n.type == 'REPLY' ? 'chip-accent' : 'chip-tag'}"><c:out value="${n.typeText}" /></span>
                                <a href="${ctx}/post?id=${n.postId}#c${n.commentId}"><c:out value="${n.postTitle}" /></a>
                                <c:if test="${not n.read}"><span class="dot-unread" title="未读">NEW</span></c:if>
                            </div>
                            <p class="notice-excerpt"><c:out value="${n.excerpt}" /></p>
                            <div class="notice-foot">
                                <span class="when"><fmt:formatDate value="${n.createdAt}" pattern="yyyy-MM-dd HH:mm" /></span>
                                <c:if test="${not n.read}">
                                    <form action="${ctx}/notice/read" method="post">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}' />">
                                        <input type="hidden" name="id" value="${n.id}">
                                        <button class="btn btn-sm btn-ghost" type="submit">标为已读</button>
                                    </form>
                                </c:if>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/common/foot.jspf" %>