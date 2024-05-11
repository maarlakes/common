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
 * @author linjpxc
 */
public abstract class AbstractPart<T> implements MultipartPart<T> {

    protected final String name;
    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    protected final ContentType contentType;
    protected final Charset charset;

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

    protected static Charset toCharset(ContentType contentType) {
        try {
            return contentType == null ? StandardCharsets.UTF_8 : Charset.forName(contentType.getCharset());
        } catch (Exception ignored) {
            return StandardCharsets.UTF_8;
        }
    }
}
