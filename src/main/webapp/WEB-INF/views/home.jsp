<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglib.jspf" %>
<c:set var="pageTitle" value="${listTitle}" />
<%@ include file="/WEB-INF/views/common/head.jspf" %>

<section class="hero" aria-labelledby="journal-title">
    <div class="hero-copy">
        <span class="eyebrow">微光手记 / THE EVERYDAY JOURNAL</span>
        <h1 id="journal-title">${empty keyword ? '在平凡里，记下微光。' : '让好文字，与你相遇。'}</h1>
        <p>技术的探索，生活的切片，还有那些值得留住的瞬间。<br>在这里，慢慢想，认真写。</p>
        <c:if test="${not empty keyword}">
            <p>关键词「<strong><c:out value="${keyword}" /></strong>」，共 ${page.total} 篇相关文章。</p>
        </c:if>
    </div>
    <img class="hero-art" src="${ctx}/assets/editorial-still-life.svg" alt="" width="400" height="290">
    <div class="hero-bottom">
        <span class="hero-note">把日子写成故事，把热爱留在纸上。</span>
        <div class="hero-stats" aria-label="博客统计">
            <div class="hero-stat"><b>${applicationScope.statPosts}</b><span>篇文章</span></div>
            <div class="hero-stat"><b>${applicationScope.statUsers}</b><span>位作者</span></div>
            <div class="hero-stat"><b>${applicationScope.statCategories}</b><span>个分类</span></div>
        </div>
    </div>
</section>

<div class="layout">
    <div>
        <div class="page-head">
            <h2>
                <c:choose>
                    <c:when test="${not empty keyword}">搜索：<c:out value="${keyword}" /></c:when>
                    <c:otherwise><c:out value="${listTitle}" /></c:otherwise>
                </c:choose>
            </h2>
            <p>
                <c:choose>
                    <c:when test="${not empty keyword}">
                        找到 ${page.total} 篇包含该关键词的文章 ·
                        <a href="${ctx}/">清空搜索</a>
                    </c:when>
                    <c:otherwise>共 ${page.total} 篇，第 ${page.page} / ${page.totalPages} 页</c:otherwise>
                </c:choose>
            </p>
        </div>

        <c:choose>
            <c:when test="${empty page.list}">
                <div class="card empty">
                    <span class="emoji">🍃</span>
                    <h3>这里还没有文章</h3>
                    <p>换个关键词试试，或者成为第一个写下的人。</p>
                    <a class="btn btn-primary" href="${ctx}/post/edit">去写第一篇</a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="post-list">
                    <c:forEach var="post" items="${page.list}">
                        <article class="post-card">
                            <div class="post-card-body">
                                <h2><a href="${ctx}/post?id=${post.id}"><c:out value="${post.title}" /></a></h2>
                                <p class="post-summary"><c:out value="${post.summaryOrTrimmed}" /></p>
                                <div class="post-meta">
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
                                    <span><fmt:formatDate value="${post.createdAt}" pattern="yyyy-MM-dd" /></span>
                                    <c:if test="${not empty post.categoryName}">
                                        <a class="chip chip-accent" href="${ctx}/?categoryId=${post.categoryId}"><c:out value="${post.categoryName}" /></a>
                                    </c:if>
                                    <span>👁 ${post.viewCount}</span>
                                    <span>♡ ${post.likeCount}</span>
                                    <span>💬 ${post.commentCount}</span>
                                    <c:if test="${post.status == 0}"><span class="chip chip-draft">草稿</span></c:if>
                                </div>
                                <c:if test="${not empty post.tags}">
                                    <div class="tag-cloud" style="margin-top:10px">
                                        <c:forEach var="tag" items="${post.tags}">
                                            <a class="chip chip-tag" href="${ctx}/?tagId=${tag.id}">#<c:out value="${tag.name}" /></a>
                                        </c:forEach>
                                    </div>
                                </c:if>
                            </div>
                            <c:if test="${not empty post.cover}">
                                <c:choose><c:when test="${fn:startsWith(post.cover, 'http://') || fn:startsWith(post.cover, 'https://')}"><img class="post-cover" src="<c:out value='${post.cover}' />" alt="封面"></c:when><c:otherwise><img class="post-cover" src="${ctx}<c:out value='${post.cover}' />" alt="封面"></c:otherwise></c:choose>
                            </c:if>
                        </article>
                    </c:forEach>
                </div>

                <%-- 翻页：保留当前的搜索/筛选条件，只改 page 参数 --%>
                <c:if test="${page.totalPages > 1}">
                    <nav class="pagination" aria-label="文章分页">
                        <c:choose>
                            <c:when test="${page.hasPrev}">
                                <c:set var="targetPage" value="${page.prevPage}" />
                                <%@ include file="/WEB-INF/views/common/list-page-url.jspf" %>
                                <a href="<c:out value='${listPageUrl}' />">‹ 上一页</a>
                            </c:when>
                            <c:otherwise><span class="disabled">‹ 上一页</span></c:otherwise>
                        </c:choose>

                        <c:forEach var="i" begin="1" end="${page.totalPages}">
                            <c:choose>
                                <c:when test="${i == page.page}"><span class="current" aria-current="page">${i}</span></c:when>
                                <c:otherwise>
                                    <c:set var="targetPage" value="${i}" />
                                    <%@ include file="/WEB-INF/views/common/list-page-url.jspf" %>
                                    <a href="<c:out value='${listPageUrl}' />">${i}</a>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>

                        <c:choose>
                            <c:when test="${page.hasNext}">
                                <c:set var="targetPage" value="${page.nextPage}" />
                                <%@ include file="/WEB-INF/views/common/list-page-url.jspf" %>
                                <a href="<c:out value='${listPageUrl}' />">下一页 ›</a>
                            </c:when>
                            <c:otherwise><span class="disabled">下一页 ›</span></c:otherwise>
                        </c:choose>
                    </nav>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>

    <aside>
        <div class="card side-card side-intro">
            <span class="eyebrow">一处小小的写作角落</span>
            <h3>你好，欢迎来到微光。</h3>
            <p>分享所学，记录所见。让每一个细小的发现，都有被认真收藏的地方。</p>
            <span class="side-signature">Small moments, lasting words.</span>
        </div>
        <div class="card side-card">
            <h3>文章目录 / CATEGORIES</h3>
            <div class="cat-list">
                <a href="${ctx}/" class="${empty activeCategoryId ? 'active' : ''}">全部文章 <span class="count">${applicationScope.statPosts}</span></a>
                <c:forEach var="cat" items="${categories}">
                    <a href="${ctx}/?categoryId=${cat.id}" class="${activeCategoryId == cat.id ? 'active' : ''}">
                        <c:out value="${cat.name}" /> <span class="count">${cat.postCount}</span>
                    </a>
                </c:forEach>
            </div>
        </div>

        <div class="card side-card">
            <h3>灵感标签 / TOPICS</h3>
            <div class="tag-cloud">
                <c:forEach var="tag" items="${tags}">
                    <a class="chip chip-tag" href="${ctx}/?tagId=${tag.id}">#<c:out value="${tag.name}" /> ${tag.postCount}</a>
                </c:forEach>
                <c:if test="${empty tags}"><span class="count">还没有标签</span></c:if>
            </div>
        </div>

        <div class="card side-card">
            <h3>值得一读 / POPULAR</h3>
            <div class="hot-list">
                <c:forEach var="hot" items="${hotPosts}" varStatus="st">
                    <div class="hot-item">
                        <span class="hot-index">${st.index + 1}</span>
                        <a href="${ctx}/post?id=${hot.id}"><c:out value="${hot.title}" /></a>
                    </div>
                </c:forEach>
                <c:if test="${empty hotPosts}"><span class="count">暂无数据</span></c:if>
            </div>
        </div>
    </aside>
</div>

<%@ include file="/WEB-INF/views/common/foot.jspf" %>
