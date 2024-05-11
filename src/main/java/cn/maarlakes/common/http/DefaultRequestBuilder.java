package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author linjpxc
 */
class DefaultRequestBuilder implements Request.Builder {

    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<Cookie> cookies = new ArrayList<>();
    private final List<NameValuePair> queryParams = new ArrayList<>();
    private final List<NameValuePair> formParams = new ArrayList<>();
    private HttpMethod method;
    private URI uri;
    private Charset charset;
    private RequestBody<?> body;

    @Nonnull
    @Override
    public Request.Builder method(@Nonnull HttpMethod method) {
        this.method = method;
        return this;
    }

    @Nonnull
    @Override
    public Request.Builder uri(@Nonnull URI uri) {
        this.uri = uri;
        return this;
    }

    @Nonnull
    @Override
    public Request.Builder setHeader(@Nonnull Header header) {
        this.headers.put(header.getName(), new ArrayList<>(header.getValues()));
        return this;
    }

    @Nonnull
    @Override
    public Request.Builder appendHeader(@Nonnull Header header) {
        this.headers.computeIfAbsent(header.getName(), k -> new ArrayList<>()).addAll(header.getValues());
        return this;
    }

    @Nonnull
    @Override
    public Request.Builder addCookie(@Nonnull Cookie cookie) {
        this.cookies.add(cookie);
        return this;
    }

    @Nonnull
    @Override
    public Request.Builder charset(@Nonnull Charset charset) {
        this.charset = charset;
        return this;
    }

    @Nonnull
    @Override
    public Request.Builder addQueryParam(@Nonnull NameValuePair param) {
        this.queryParams.add(param);
        return this;
    }

    @Nonnull
    @Override
    public Request.Builder addFormParam(@Nonnull NameValuePair param) {
        this.formParams.add(param);
        return this;
    }

    @Nonnull
    @Override
    public Request.Builder body(@Nonnull RequestBody<?> body) {
        this.body = body;
        return this;
    }

    @Nonnull
    @Override
    public Request build() {
        return new DefaultRequest();
    }

    private class DefaultRequest implements Request {
        @Nonnull
        @Override
        public HttpMethod getMethod() {
            return method;
        }

        @Nonnull
        @Override
        public URI getUri() {
            return uri;
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            final Map<String, Header> map = new TreeMap<>();
            headers.forEach((k, v) -> map.put(k, new DefaultHeader(k, v)));
            return new DefaultHttpHeaders(map);
        }

        @Nonnull
        @Override
        public List<? extends Cookie> getCookies() {
            return cookies;
        }

        @Override
        public Charset getCharset() {
            return charset;
        }

        @Override
        public List<? extends NameValuePair> getQueryParams() {
            return queryParams;
        }

        @Override
        public List<? extends NameValuePair> getFormParams() {
            return formParams;
        }

        @Override
        public RequestBody<?> getBody() {
            return body;
        }
    }
}
