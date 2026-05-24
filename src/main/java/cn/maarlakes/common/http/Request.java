package cn.maarlakes.common.http;

import cn.maarlakes.common.http.body.DefaultTextBody;
import cn.maarlakes.common.http.body.JsonBody;
import cn.maarlakes.common.http.body.multipart.MultipartBody;
import jakarta.annotation.Nonnull;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

/**
 * HTTP 请求模型，封装方法、URI、头部、Cookie、查询参数、表单参数和请求体。
 *
 * <p>通过 {@link #builder()} 创建构建器，支持链式调用：
 * <pre>
 *   Request request = Request.builder()
 *       .post("https://example.com/api")
 *       .json("{\"key\":\"value\"}")
 *       .setHeader("Authorization", "Bearer token")
 *       .build();
 * </pre>
 *
 * <p>请求体支持多种类型：纯文本（{@link Builder#text}）、JSON（{@link Builder#json}）、
 * 表单参数（{@link Builder#addFormParam}）、Multipart（{@link Builder#multipartBody}），
 * 或通过 {@link Builder#body} 传入自定义 {@link RequestBody}。
 *
 * @author linjpxc
 */
public interface Request {

    /** HTTP 方法（GET、POST、PUT、DELETE 等）。 */
    @Nonnull
    HttpMethod getMethod();

    /** 请求目标 URI（不含查询参数片段，查询参数通过 {@link #getQueryParams()} 获取）。 */
    @Nonnull
    URI getUri();

    /** 请求头部集合。 */
    @Nonnull
    HttpHeaders getHeaders();

    /** 请求附带的 Cookie 列表。 */
    @Nonnull
    List<? extends Cookie> getCookies();

    /** 请求的字符编码，用于编码请求体和查询参数。可为 null（使用默认 UTF-8）。 */
    Charset getCharset();

    /** URL 查询参数列表。 */
    List<? extends NameValuePair> getQueryParams();

    /** 表单参数列表（仅用于 POST/PUT 请求，Content-Type 为 application/x-www-form-urlencoded）。 */
    List<? extends NameValuePair> getFormParams();

    /** 请求体。为 null 时，如果有表单参数则自动构建 URL 编码的请求体。 */
    RequestBody<?> getBody();

    /**
     * 创建请求构建器。
     *
     * @return 新的 Builder 实例
     */
    @Nonnull
    static Builder builder() {
        return new DefaultRequestBuilder();
    }

    /**
     * 请求构建器，提供流式 API 构建完整的 HTTP 请求。
     *
     * <p>便捷方法（{@code get}、{@code post}、{@code json} 等）组合了
     * 方法和 URI 或请求体的设置，减少样板代码。
     *
     * <p>Header 方法分为两种：
     * <ul>
     *   <li>{@link #setHeader}：替换同名头的所有值</li>
     *   <li>{@link #appendHeader}：追加到同名头的值列表</li>
     * </ul>
     */
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
        default Builder head(@Nonnull String url) {
            return this.method(HttpMethod.HEAD).uri(url);
        }

        @Nonnull
        default Builder options(@Nonnull String url) {
            return this.method(HttpMethod.OPTIONS).uri(url);
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
