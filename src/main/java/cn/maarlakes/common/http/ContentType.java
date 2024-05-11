package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

/**
 * @author linjpxc
 */
public interface ContentType extends Serializable {

    ContentType APPLICATION_ATOM_XML = create("application/atom+xml", StandardCharsets.UTF_8);

    ContentType APPLICATION_FORM_URLENCODED = create("application/x-www-form-urlencoded", StandardCharsets.UTF_8);

    ContentType APPLICATION_JSON = create("application/json", StandardCharsets.UTF_8);

    ContentType APPLICATION_OCTET_STREAM = create("application/octet-stream");

    ContentType APPLICATION_SVG_XML = create("application/svg+xml", StandardCharsets.UTF_8);

    ContentType APPLICATION_XHTML_XML = create("application/xhtml+xml", StandardCharsets.UTF_8);

    ContentType APPLICATION_XML = create("application/xml", StandardCharsets.UTF_8);

    ContentType IMAGE_GIF = create("image/gif");

    ContentType IMAGE_JPEG = create("image/jpeg");
    ContentType IMAGE_PNG = create("image/png");
    ContentType IMAGE_TIFF = create("image/tiff");
    ContentType IMAGE_WEBP = create("image/webp");

    ContentType MULTIPART_FORM_DATA = create("multipart/form-data", StandardCharsets.UTF_8);

    ContentType TEXT_EVENT_STREAM = create("text/event-stream", StandardCharsets.UTF_8);

    ContentType TEXT_HTML = create("text/html", StandardCharsets.UTF_8);

    ContentType TEXT_PLAIN = create("text/plain", StandardCharsets.UTF_8);

    ContentType TEXT_XML = create("text/xml", StandardCharsets.UTF_8);

    ContentType ALL = create("*/*");

    @Nonnull
    String getMediaType();

    String getCharset();

    Collection<NameValuePair> getParameters();

    String getParameter(@Nonnull String name);

    @Nonnull
    default ContentType withCharset(Charset charset) {
        if (charset == null) {
            return this.withCharset((String) null);
        }
        return this.withCharset(charset.name());
    }

    @Nonnull
    ContentType withCharset(String charset);

    @Nonnull
    default ContentType withParameter(@Nonnull String name, @Nonnull String value) {
        return this.withParameter(new DefaultNameValuePair(name, value));
    }

    @Nonnull
    ContentType withParameter(@Nonnull NameValuePair... params);

    @Nonnull
    default Header toHeader() {
        return new DefaultHeader(HttpHeaderNames.CONTENT_TYPE, ContentTypes.toString(this));
    }

    @Nonnull
    static ContentType create(@Nonnull String mediaType) {
        return new DefaultContentType(mediaType, null, Collections.emptyList());
    }

    @Nonnull
    static ContentType create(@Nonnull String mediaType, Charset charset) {
        return create(mediaType, charset, Collections.emptyList());
    }

    @Nonnull
    static ContentType create(@Nonnull String mediaType, Charset charset, Collection<? extends NameValuePair> parameters) {
        return new DefaultContentType(mediaType, charset == null ? null : charset.name().toLowerCase(), parameters == null ? Collections.emptyList() : parameters);
    }

    @Nonnull
    static ContentType create(@Nonnull String mediaType, Collection<? extends NameValuePair> parameters) {
        return new DefaultContentType(mediaType, null, parameters == null ? Collections.emptyList() : parameters);
    }

    @Nonnull
    static ContentType create(@Nonnull String mediaType, String charset) {
        return create(mediaType, charset, Collections.emptyList());
    }

    @Nonnull
    static ContentType create(@Nonnull String mediaType, String charset, Collection<? extends NameValuePair> parameters) {
        return new DefaultContentType(mediaType, charset, parameters == null ? Collections.emptyList() : parameters);
    }
}
