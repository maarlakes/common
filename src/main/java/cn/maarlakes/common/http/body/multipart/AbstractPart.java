package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.http.*;
import jakarta.annotation.Nonnull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@link MultipartPart} 的抽象基类，提供名称、Content-Type、字符集和自定义头部的通用管理。
 *
 * <p>所有具体 Part 实现（{@link ByteArrayPart}、{@link DefaultTextPart}、{@link DefaultFilePart}）
 * 均继承此类。头部存储使用大小写不敏感的 {@link TreeMap}，确保 HTTP 头部名称的语义正确性。
 * 子类可通过 setter 方法设置 filename、Content-Transfer-Encoding、Content-ID 等头部。</p>
 *
 * @param <T> Part 内容的原始类型
 * @author linjpxc
 */
public abstract class AbstractPart<T> implements MultipartPart<T> {

    /** Part 的字段名称 */
    protected final String name;
    /** 大小写不敏感的头部存储 */
    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    /** Part 的 Content-Type，可能为 null */
    protected final ContentType contentType;
    /** Part 内容使用的字符集 */
    protected final Charset charset;

    /** Part 的文件名，对应 Content-Disposition 中的 filename 属性 */
    protected String filename;

    public AbstractPart(@Nonnull String name, ContentType contentType, Charset charset) {
        this.name = name;
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
        this.contentType = contentType == null ? null : contentType.withCharset(this.charset);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public HttpHeaders getHeaders() {
        final Map<String, Header> map = new TreeMap<>();
        headers.forEach((k, v) -> map.put(k, new DefaultHeader(k, v)));
        return new DefaultHttpHeaders(map);
    }

    public void setHeader(@Nonnull String name, @Nonnull String value) {
        this.setHeader(new DefaultHeader(name, value));
    }

    public void setHeader(@Nonnull Header header) {
        this.headers.put(header.getName(), new ArrayList<>(header.getValues()));
    }

    @Override
    public Charset getCharset() {
        return this.charset;
    }

    @Override
    public ContentType getContentType() {
        return this.contentType;
    }

    public void setTransferEncoding(@Nonnull String transferEncoding) {
        this.setHeader("Content-Transfer-Encoding", transferEncoding);
    }

    public void setContentId(@Nonnull String contentId) {
        this.setHeader("Content-ID", contentId);
    }

    public void setDispositionType(@Nonnull String dispositionType) {
        this.setHeader("Content-Disposition", dispositionType);
    }

    /**
     * 从 Content-Type 中安全提取字符集，解析失败时回退到 UTF-8。
     * 供子类构造函数中确定字符集使用。
     */
    protected static Charset toCharset(ContentType contentType) {
        try {
            return contentType == null ? StandardCharsets.UTF_8 : Charset.forName(contentType.getCharset());
        } catch (Exception ignored) {
            // 字符集名称无效时回退到 UTF-8
            return StandardCharsets.UTF_8;
        }
    }
}
