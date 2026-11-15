<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglib.jspf" %>
<c:set var="pageTitle" value="出错了" />
<c:set var="statusCode" value="${requestScope['javax.servlet.error.status_code']}" />
<%@ include file="/WEB-INF/views/common/head.jspf" %>

<div class="card empty error-page">
    <span class="eyebrow">微光 · 暂停一页 / A LITTLE PAUSE</span>
    <span class="emoji">${statusCode == 404 ? '🧭' : (statusCode == 403 ? '🔒' : '🛠')}</span>
    <h1>
        <c:choose>
            <c:when test="${not empty errorTitle}"><c:out value="${errorTitle}" /></c:when>
            <c:when test="${statusCode == 404}">页面不存在</c:when>
            <c:when test="${statusCode == 403}">无权访问</c:when>
            <c:otherwise>服务器开了个小差</c:otherwise>
        </c:choose>
    </h1>
    <p>
        <c:choose>
            <c:when test="${not empty errorDetail}"><c:out value="${errorDetail}" /></c:when>
            <c:when test="${statusCode == 404}">你访问的地址可能已被移动或删除。</c:when>
            <c:when test="${statusCode == 403}">这个页面需要相应的权限才能打开。</c:when>
            <c:otherwise>请稍后再试，或者回到首页重新开始。</c:otherwise>
        </c:choose>
    </p>
    <div style="margin-top:24px;display:flex;flex-wrap:wrap;gap:12px;justify-content:center">
        <a class="btn btn-primary" href="${ctx}/">回到首页</a>
        <a class="btn btn-ghost" href="javascript:history.back()">返回上一页</a>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/foot.jspf" %>