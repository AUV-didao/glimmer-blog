<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglib.jspf" %>
<c:set var="pageTitle" value="注册" />
<%@ include file="/WEB-INF/views/common/head.jspf" %>

<form class="card form-card" action="${ctx}/register" method="post">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}' />">
    <h1>创建账号</h1>
    <p class="sub">只需几秒，就能拥有自己的写作空间。</p>

    <c:if test="${not empty error}">
        <div class="flash flash-error" role="alert"><span>⚠</span><span><c:out value="${error}" /></span></div>
    </c:if>

    <div class="field">
        <label for="username">用户名</label>
        <input class="input" id="username" name="username" autocomplete="username" required autofocus maxlength="20"
               value="<c:out value='${username}' />" placeholder="4-20 位字母、数字或下划线">
        <div class="hint">登录用，注册后不可修改。</div>
    </div>
    <div class="field">
        <label for="nickname">昵称</label>
        <input class="input" id="nickname" name="nickname" maxlength="32"
               value="<c:out value='${nickname}' />" placeholder="页面上显示的名字，不填就用用户名">
    </div>
    <div class="field">
        <label for="email">邮箱（选填）</label>
        <input class="input" id="email" name="email" type="email" maxlength="128"
               value="<c:out value='${email}' />" placeholder="you@example.com">
    </div>
    <div class="field-row">
        <div class="field">
            <label for="password">密码</label>
            <input class="input" id="password" type="password" name="password" autocomplete="new-password" required minlength="6" placeholder="至少 6 位">
        </div>
        <div class="field">
            <label for="password2">确认密码</label>
            <input class="input" id="password2" type="password" name="password2" autocomplete="new-password" required minlength="6" placeholder="再输一次">
        </div>
    </div>

    <button class="btn btn-primary btn-block" type="submit">注 册</button>
    <p class="form-foot">已有账号？<a href="${ctx}/login">去登录</a></p>
</form>

<%@ include file="/WEB-INF/views/common/foot.jspf" %>