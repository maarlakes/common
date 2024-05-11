package cn.maarlakes.common.http;

import cn.maarlakes.common.http.body.DefaultTextBody;
import cn.maarlakes.common.http.body.JsonBody;
import cn.maarlakes.common.http.body.multipart.MultipartBody;
import jakarta.annotation.Nonnull;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author linjpxc
 */
public interface Request {

    @Nonnull
    HttpMethod getMethod();

    @Nonnull
    URI getUri();

    @Nonnull
    HttpHeaders getHeaders();

    @Nonnull
    List<? extends Cookie> getCookies();

    Charset getCharset();

    List<? extends NameValuePair> getQueryParams();

    List<? extends NameValuePair> getFormParams();

    RequestBody<?> getBody();

    @Nonnull
    static Builder builder() {
        return new DefaultRequestBuilder();
    }

    interface Builder {

        @Nonnull
        Builder method(@Nonnull HttpMethod method);

        @Nonnull
        default Builder get(@Nonnull String url) {
            return this.method(HttpMethod.GET).uri(url);
        }

        @Nonnull
        default Builder post(@Nonnull String url) {
            return this.method(HttpMethod.POST).uri(url);
        }

        @Nonnull
        default Builder put(@Nonnull String url) {
            return this.method(HttpMethod.PUT).uri(url);
        }

        @Nonnull
        default Builder delete(@Nonnull String url) {
            return this.method(HttpMethod.DELETE).uri(url);
        }

        @Nonnull
        default Builder patch(@Nonnull String url) {
            return this.method(HttpMethod.PATCH).uri(url);
        }

        @Nonnull
        default Builder uri(@Nonnull String url) {
            return this.uri(URI.create(url));
        }

        @Nonnull
        Builder uri(@Nonnull URI uri);

        @Nonnull
        default Builder setHeader(@Nonnull String name, @Nonnull String value) {
            return this.setHeader(new DefaultHeader(name, value));
        }

        @Nonnull
        Builder setHeader(@Nonnull Header header);

        @Nonnull
        default Builder appendHeader(@Nonnull String name, @Nonnull String value) {
            return this.appendHeader(new DefaultHeader(name, value));
        }

        @Nonnull
        Builder appendHeader(@Nonnull Header header);

        @Nonnull
        default Builder addCookie(@Nonnull String name, @Nonnull String value) {
            return this.addCookie(Cookie.builder(name).value(value).build());
        }

        @Nonnull
        Builder addCookie(@Nonnull Cookie cookie);

        @Nonnull
        Builder charset(@Nonnull Charset charset);

        @Nonnull
        default Builder addQueryParam(@Nonnull String name, String value) {
            return this.addQueryParam(new DefaultNameValuePair(name, value));
        }

        @Nonnull
        Builder addQueryParam(@Nonnull NameValuePair param);

        @Nonnull
        default Builder addFormParam(@Nonnull String name, String value) {
            return this.addFormParam(new DefaultNameValuePair(name, value));
        }

        @Nonnull
        Builder addFormParam(@Nonnull NameValuePair param);

        @Nonnull
        Builder body(@Nonnull RequestBody<?> body);

        @Nonnull
        default Builder multipartBody(@Nonnull MultipartBody.Builder builder) {
            return this.multipartBody(builder.build());
        }

        @Nonnull
        default Builder multipartBody(@Nonnull MultipartBody body) {
            return this.body(body);
        }

        @Nonnull
        default Builder text(@Nonnull String text) {
            return this.body(new DefaultTextBody(text, ContentType.TEXT_PLAIN));
        }

        @Nonnull
        default Builder text(@Nonnull String text, String charset) {
            return this.body(new DefaultTextBody(text, ContentType.TEXT_PLAIN.withCharset(charset)));
        }

        @Nonnull
        default Builder json(@Nonnull String json) {
            return this.body(new JsonBody(json));
        }

        @Nonnull
        default Builder json(@Nonnull String json, String charset) {
            return this.body(new JsonBody(json, charset));
        }

        @Nonnull
        default Builder json(@Nonnull String json, Charset charset) {
            return this.body(new JsonBody(json, charset));
        }

        @Nonnull
        default Builder json(@Nonnull Object obj) {
            return this.body(new JsonBody(obj));
        }

        @Nonnull
        default Builder json(@Nonnull Object obj, Charset charset) {
            return this.body(new JsonBody(obj, charset));
        }

        @Nonnull
        default Builder json(@Nonnull Object obj, String charset) {
            return this.body(new JsonBody(obj, charset));
        }

        @Nonnull
        Request build();
    }
}
