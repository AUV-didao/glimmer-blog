<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglib.jspf" %>
<c:set var="pageTitle" value="${post.title}" />
<%@ include file="/WEB-INF/views/common/head.jspf" %>

<div class="layout">
    <div>
        <article class="card article">
            <div class="article-head">
                <span class="eyebrow">微光 · 阅读时光 / READING ROOM</span>
                <h1><c:out value="${post.title}" />
                    <c:if test="${post.status == 0}"><span class="chip chip-draft">草稿</span></c:if>
                </h1>
                <div class="article-meta">
                    <span class="author">
                        <c:choose>
                            <c:when test="${not empty post.authorAvatar}">
                                <img class="avatar avatar-sm" src="${ctx}<c:out value='${post.authorAvatar}' />" alt="">
                            </c:when>
                            <c:otherwise>
                                <span class="avatar avatar-sm"><c:out value="${post.authorInitial}" /></span>
                            </c:otherwise>
                        </c:choose>
                        <c:out value="${post.authorDisplayName}" />
                    </span>
                    <span><fmt:formatDate value="${post.createdAt}" pattern="yyyy-MM-dd HH:mm" /></span>
                    <c:if test="${not empty post.categoryName}">
                        <span class="chip chip-accent"><c:out value="${post.categoryName}" /></span>
                    </c:if>
                    <span>👁 ${post.viewCount} 次浏览</span>
                    <span>💬 ${commentCount} 条评论</span>
                </div>
            </div>

            <c:if test="${not empty post.cover}">
                <div class="article-cover"><c:choose><c:when test="${fn:startsWith(post.cover, 'http://') || fn:startsWith(post.cover, 'https://')}"><img src="<c:out value='${post.cover}' />" alt="封面"></c:when><c:otherwise><img src="${ctx}<c:out value='${post.cover}' />" alt="封面"></c:otherwise></c:choose></div>
            </c:if>

            <%-- 正文是纯文本，用 pre-wrap 保留换行；c:out 自动转义，防止 XSS 注入标签 --%>
            <div class="article-body"><c:out value="${post.content}" /></div>

            <c:if test="${not empty post.tags}">
                <div class="tag-cloud" style="margin-top:24px">
                    <c:forEach var="tag" items="${post.tags}">
                        <a class="chip chip-tag" href="${ctx}/?tagId=${tag.id}">#<c:out value="${tag.name}" /></a>
                    </c:forEach>
                </div>
            </c:if>

            <div class="article-actions">
                <button type="button" aria-pressed="${post.likedByMe}" aria-describedby="like-feedback" class="btn btn-ghost like-btn ${post.likedByMe ? 'is-liked' : ''}"
                        data-post-id="${post.id}"
                        data-action="${ctx}/post/like"
                        data-login="${ctx}/login">
                    <span class="heart">♥</span>
                    <span class="like-text">${post.likedByMe ? '已赞' : '点赞'}</span>
                    <span class="like-count">${post.likeCount}</span>
                </button>
                <a class="btn btn-ghost" href="${ctx}/download?scope=post&id=${post.id}">⇩ 导出全文</a>
                <c:if test="${canEdit}">
                    <a class="btn btn-ghost" href="${ctx}/post/edit?id=${post.id}">✎ 编辑</a>
                    <form action="${ctx}/post/delete" method="post" onsubmit="return confirm('确定删除这篇文章吗？删除后不可恢复。');" style="display:inline">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}' />">
                        <input type="hidden" name="id" value="${post.id}">
                        <button class="btn btn-danger" type="submit">删除</button>
                    </form>
                </c:if>
                <span id="like-feedback" class="like-feedback" role="status" aria-live="polite"></span>
            </div>
        </article>

        <section class="card card-pad comments" id="comments">
            <div class="section-title" style="margin-top:0">
                <h2>评论 <span class="count">${commentCount}</span></h2>
            </div>

            <c:choose>
                <c:when test="${not empty sessionScope.loginUser}">
                    <%-- 一个表单身兼两职：parentId=0 是评论文章，parentId>0 是回复某条评论 --%>
                    <form class="comment-form" id="comment-form" action="${ctx}/comment/add" method="post">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}' />">
                        <input type="hidden" name="postId" value="${post.id}">
                        <input type="hidden" name="parentId" value="${empty replyTarget ? 0 : replyTarget.id}">
                        <c:if test="${not empty replyTarget}">
                            <div class="reply-hint">
                                正在回复 <b>@<c:out value="${replyTarget.displayName}" /></b>
                                <span class="reply-hint-quote">「<c:out value="${replyTarget.excerpt}" />」</span>
                                <a href="${ctx}/post?id=${post.id}#comment-form">取消回复</a>
                            </div>
                        </c:if>
                        <label class="sr-only" for="comment-content">评论或回复内容（最多 500 字）</label>
                        <textarea id="comment-content" name="content" maxlength="500" required placeholder="说点什么…（最多 500 字）"></textarea>
                        <button class="btn btn-primary" type="submit">${empty replyTarget ? '发表评论' : '发表回复'}</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <div class="flash flash-success" style="margin-bottom:20px">
                        <span>💡</span>
                        <span><a href="${ctx}/login">登录</a> 后就可以发表评论啦。</span>
                    </div>
                </c:otherwise>
            </c:choose>

            <c:choose>
                <c:when test="${empty comments}">
                    <div class="empty" style="padding:30px 0">
                        <span class="emoji">💬</span>
                        <h3>还没有评论</h3>
                        <p>来做第一个留言的人吧。</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="comment-list">
                        <c:forEach var="cm" items="${comments}">
                            <div class="comment-item" id="c${cm.id}">
                                <c:choose>
                                    <c:when test="${not empty cm.userAvatar}">
                                        <img class="avatar avatar-sm" src="${ctx}<c:out value='${cm.userAvatar}' />" alt="">
                                    </c:when>
                                    <c:otherwise>
                                        <span class="avatar avatar-sm"><c:out value="${cm.initial}" /></span>
                                    </c:otherwise>
                                </c:choose>
                                <div class="comment-main">
                                    <div class="comment-top">
                                        <span class="who"><c:out value="${cm.displayName}" /></span>
                                        <span class="when"><fmt:formatDate value="${cm.createdAt}" pattern="yyyy-MM-dd HH:mm" /></span>
                                        <c:if test="${not empty sessionScope.loginUser}">
                                            <a class="reply-link" href="${ctx}/post?id=${post.id}&replyTo=${cm.id}#comment-form">回复</a>
                                        </c:if>
                                        <%-- 只有本人或管理员能看到删除按钮 --%>
                                        <c:if test="${not empty sessionScope.loginUser && (sessionScope.loginUser.id == cm.userId || sessionScope.loginUser.admin)}">
                                            <form action="${ctx}/comment/delete" method="post" style="margin-left:auto"
                                                  onsubmit="return confirm('删除这条评论？');">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}' />">
                                                <input type="hidden" name="id" value="${cm.id}">
                                                <input type="hidden" name="postId" value="${post.id}">
                                                <button class="btn btn-sm btn-ghost" type="submit">删除</button>
                                            </form>
                                        </c:if>
                                    </div>
                                    <p class="comment-body"><c:out value="${cm.content}" /></p>

                                    <%-- 二级评论：回复挂在一级评论下面，缩进显示 --%>
                                    <c:if test="${not empty cm.replies}">
                                        <div class="reply-list">
                                            <c:forEach var="rp" items="${cm.replies}">
                                                <div class="reply-item" id="c${rp.id}">
                                                    <c:choose>
                                                        <c:when test="${not empty rp.userAvatar}">
                                                            <img class="avatar avatar-xs" src="${ctx}<c:out value='${rp.userAvatar}' />" alt="">
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="avatar avatar-xs"><c:out value="${rp.initial}" /></span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                    <div class="comment-main">
                                                        <div class="comment-top">
                                                            <span class="who"><c:out value="${rp.displayName}" /></span>
                                                            <span class="reply-to">回复 <b>@<c:out value="${rp.replyToName}" /></b></span>
                                                            <span class="when"><fmt:formatDate value="${rp.createdAt}" pattern="yyyy-MM-dd HH:mm" /></span>
                                                            <c:if test="${not empty sessionScope.loginUser}">
                                                                <a class="reply-link" href="${ctx}/post?id=${post.id}&replyTo=${rp.id}#comment-form">回复</a>
                                                            </c:if>
                                                            <c:if test="${not empty sessionScope.loginUser && (sessionScope.loginUser.id == rp.userId || sessionScope.loginUser.admin)}">
                                                                <form action="${ctx}/comment/delete" method="post" style="margin-left:auto"
                                                                      onsubmit="return confirm('删除这条回复？');">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}' />">
                                                                    <input type="hidden" name="id" value="${rp.id}">
                                                                    <input type="hidden" name="postId" value="${post.id}">
                                                                    <button class="btn btn-sm btn-ghost" type="submit">删除</button>
                                                                </form>
                                                            </c:if>
                                                        </div>
                                                        <p class="comment-body"><c:out value="${rp.content}" /></p>
                                                    </div>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </c:if>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <aside>
        <div class="card side-card">
            <h3>关于作者</h3>
            <div style="display:flex;gap:12px;align-items:center">
                <c:choose>
                    <c:when test="${not empty post.authorAvatar}">
                        <img class="avatar avatar-lg" src="${ctx}<c:out value='${post.authorAvatar}' />" alt="">
                    </c:when>
                    <c:otherwise>
                        <span class="avatar avatar-lg"><c:out value="${post.authorInitial}" /></span>
                    </c:otherwise>
                </c:choose>
                <div>
                    <div style="font-weight:700"><c:out value="${post.authorDisplayName}" /></div>
                    <div style="font-size:12.5px;color:var(--muted)">@<c:out value="${post.authorUsername}" /></div>
                </div>
            </div>
        </div>

        <div class="card side-card">
            <h3>快捷操作</h3>
            <div style="display:flex;flex-direction:column;gap:8px">
                <a class="btn btn-ghost btn-block" href="${ctx}/">← 返回文章列表</a>
                <a class="btn btn-ghost btn-block" href="${ctx}/post/edit">✎ 写一篇新文章</a>
            </div>
        </div>
    </aside>
</div>

<%@ include file="/WEB-INF/views/common/foot.jspf" %>