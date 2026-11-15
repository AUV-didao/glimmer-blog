package com.example.blog.util;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 文件上传工具。
 *
 * <p>三个必须做对的地方（也是本项目踩过的坑）：</p>
 * <ol>
 *   <li><b>不能信任客户端文件名</b>：用户可以把文件命名成 ../../web.xml。
 *       这里只用「UUID + 校验过的后缀」重新命名，彻底丢掉原始文件名；</li>
 *   <li><b>后缀白名单</b>：只允许图片后缀，防止上传 jsp 文件后被当成脚本执行；</li>
 *   <li><b>大小限制</b>：Servlet 层用 @MultipartConfig 限制，这里再检查一次，
 *       因为超过限制时容器抛的是 IllegalStateException，提示不友好。</li>
 * </ol>
 */
public final class UploadUtil {

    private UploadUtil() {
    }

    /**
     * 保存上传的图片。
     *
     * @param ctx        ServletContext，用于 getRealPath 定位部署目录
     * @param part       上传的文件部件
     * @param subDir     webapp 下的子目录，例如 "upload/cover"
     * @param maxBytes   允许的最大字节数
     * @return 浏览器可访问的相对路径（如 /upload/cover/9f3a.png）；没有文件或校验失败返回 null
     */
    public static String saveImage(ServletContext ctx, Part part, String subDir, long maxBytes)
            throws java.io.IOException {
        if (part == null || part.getSize() <= 0) {
            return null;
        }
        if (part.getSize() > maxBytes) {
            throw new IllegalArgumentException("图片太大了，请压缩到 5MB 以内");
        }
        String ext = WebUtil.extOf(part.getSubmittedFileName());
        if (!isAllowedImageExt(ext)) {
            throw new IllegalArgumentException("只支持 jpg / png / gif / webp 格式的图片");
        }
        String realDir = ctx.getRealPath("/" + subDir);
        if (realDir == null) {
            throw new IllegalStateException("应用未解压部署，无法保存上传文件");
        }
        File dir = new File(realDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("创建上传目录失败：" + realDir);
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        File target = new File(dir, fileName);
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return "/" + subDir + "/" + fileName;
    }

    /**
     * 安全地取文件部件：只有真正的 multipart/form-data 请求才去 getPart，
     * 否则 Tomcat 会抛 InvalidContentTypeException（表单没加 enctype 时就会这样）。
     *
     * @return 文件部件；请求不是 multipart 或没有该字段时返回 null
     */
    public static Part imagePart(HttpServletRequest req, String name)
            throws IOException, ServletException {
        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        return req.getPart(name);
    }

    /** 后缀白名单校验。 */
    public static boolean isAllowedImageExt(String ext) {
        for (String allowed : Const.IMAGE_EXT) {
            if (allowed.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }
}