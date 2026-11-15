<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglib.jspf" %>
<c:set var="pageTitle" value="登录" />
<%@ include file="/WEB-INF/views/common/head.jspf" %>

<form class="card form-card" action="${ctx}/login" method="post">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}' />">
    <h1>欢迎回来</h1>
    <p class="sub">登录后可以发表文章、评论和点赞。</p>

    <c:if test="${not empty error}">
        <div class="flash flash-error" role="alert"><span>⚠</span><span><c:out value="${error}" /></span></div>
    </c:if>

    <input type="hidden" name="next" value="<c:out value='${next}' />">

    <div class="field">
        <label for="username">用户名</label>
        <input class="input" id="username" name="username" autocomplete="username" required autofocus
               value="<c:out value='${username}' />" placeholder="请输入用户名">
    </div>
    <div class="field">
        <label for="password">密码</label>
        <input class="input" id="password" type="password" name="password" autocomplete="current-password" required placeholder="请输入密码">
    </div>

    <label class="remember-row">
        <input type="checkbox" name="remember" value="1">
        <span>记住我（7 天内免登录）</span>
    </label>

    <button class="btn btn-primary btn-block" type="submit">登 录</button>

    <p class="form-foot">还没有账号？<a href="${ctx}/register">立即注册</a></p>
    <p class="form-foot" style="margin-top:8px">
        演示账号：<code>admin</code> / <code>123456</code>（管理员）　
        <code>lin</code> / <code>123456</code>（普通用户）
    </p>
</form>

<%@ include file="/WEB-INF/views/common/foot.jspf" %>