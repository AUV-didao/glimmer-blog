<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglib.jspf" %>
<%-- 新建时 post 为 null，校验失败重新渲染时用 form（用户已填的内容） --%>
<c:set var="p" value="${not empty form ? form : post}" />
<c:set var="isEdit" value="${not empty p && p.id > 0}" />
<c:set var="pageTitle" value="${isEdit ? '编辑文章' : '写文章'}" />
<%@ include file="/WEB-INF/views/common/head.jspf" %>

<c:set var="tagText" value="" />
<c:forEach var="t" items="${p.tags}"><c:set var="tagText" value="${tagText}${t.name} " /></c:forEach>

<div class="page-head editor-head">
    <span class="eyebrow">创作手记 / WRITING DESK</span>
    <h1>${isEdit ? '编辑文章' : '写一篇新文章'}</h1>
    <p>支持分类、标签与封面图；正文用纯文本保存，换行会原样保留。</p>
</div>

<c:if test="${not empty error}">
    <div class="flash flash-error" role="alert"><span>⚠</span><span><c:out value="${error}" /></span></div>
</c:if>

<form class="card card-pad editor-form" action="${ctx}/post/save" method="post" enctype="multipart/form-data">
                            <input type="hidden" name="_csrf" value="<c:out value='${sessionScope.csrfToken}' />">
    <c:if test="${isEdit}"><input type="hidden" name="id" value="${p.id}"></c:if>

    <div class="field">
        <label for="title">标题</label>
        <input class="input" id="title" name="title" maxlength="120" required
               placeholder="给文章起个标题" value="<c:out value='${p.title}' />">
    </div>

    <div class="field-row">
        <div class="field">
            <label for="categoryId">分类</label>
            <select class="select" id="categoryId" name="categoryId">
                <option value="0">不分类</option>
                <c:forEach var="cat" items="${categories}">
                    <option value="${cat.id}" ${p.categoryId == cat.id ? 'selected' : ''}><c:out value="${cat.name}" /></option>
                </c:forEach>
            </select>
        </div>
        <div class="field">
            <label for="tags">标签</label>
            <input class="input" id="tags" name="tags" maxlength="120"
                   placeholder="用空格或逗号分隔，最多 5 个" value="<c:out value='${tagText}' />">
            <div class="hint">例如：Servlet JSP JDBC</div>
        </div>
    </div>

    <div class="field">
        <label for="summary">摘要</label>
        <input class="input" id="summary" name="summary" maxlength="255"
               placeholder="一句话介绍这篇文章（不填会从正文截取）" value="<c:out value='${p.summary}' />">
    </div>

    <div class="field">
        <label for="content">正文</label>
        <textarea class="textarea textarea-lg" id="content" name="content" required
                  placeholder="开始写吧…"><c:out value="${p.content}" /></textarea>
        <div class="hint">至少 10 个字。纯文本保存，换行会保留。</div>
    </div>

    <div class="field-row">
        <div class="field">
            <label for="coverFile">封面图（可选）</label>
            <input class="input" type="file" id="coverFile" name="coverFile" accept="image/*">
            <div class="hint">jpg / png / gif / webp，5MB 以内</div>
        </div>
        <div class="field">
            <label for="coverUrl">或填写图片地址</label>
            <input class="input" id="coverUrl" name="coverUrl" placeholder="/upload/cover/xxx.png 或 https://…"
                   value="<c:out value='${p.cover}' />">
            <div class="hint">上传新图片时以上传文件为准；不改地址则保留原封面。</div>
            <c:if test="${not empty p.cover}"><label><input type="checkbox" name="removeCover" value="1"> 移除当前封面（上传新图片时忽略此项）</label></c:if>
        </div>
    </div>
    <c:if test="${not empty p.cover}">
        <div class="field">
            <label>当前封面</label>
            <c:choose><c:when test="${fn:startsWith(p.cover, 'http://') || fn:startsWith(p.cover, 'https://')}"><img src="<c:out value='${p.cover}' />" alt="封面" style="max-width:260px;border-radius:10px;border:1px solid var(--line)"></c:when><c:otherwise><img src="${ctx}<c:out value='${p.cover}' />" alt="封面" style="max-width:260px;border-radius:10px;border:1px solid var(--line)"></c:otherwise></c:choose>
        </div>
    </c:if>

    <div class="field">
        <label>发布状态</label>
        <label style="display:inline-flex;align-items:center;gap:6px;font-weight:500;margin-right:18px">
            <input type="radio" name="status" value="1" ${p.status == 0 ? '' : 'checked'}> 立即发布
        </label>
        <label style="display:inline-flex;align-items:center;gap:6px;font-weight:500">
            <input type="radio" name="status" value="0" ${p.status == 0 ? 'checked' : ''}> 存为草稿
        </label>
    </div>

    <div class="form-actions">
        <button class="btn btn-primary" type="submit">${isEdit ? '保存修改' : '发布文章'}</button>
        <c:choose>
            <c:when test="${isEdit}"><a class="btn btn-ghost" href="${ctx}/post?id=${p.id}">取消</a></c:when>
            <c:otherwise><a class="btn btn-ghost" href="${ctx}/">取消</a></c:otherwise>
        </c:choose>
    </div>
</form>

<%@ include file="/WEB-INF/views/common/foot.jspf" %>