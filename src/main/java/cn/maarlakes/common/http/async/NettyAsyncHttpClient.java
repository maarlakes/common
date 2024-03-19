package cn.maarlakes.common.http.async;

import cn.maarlakes.common.http.*;
import cn.maarlakes.common.utils.CollectionUtils;
import io.netty.handler.codec.http.cookie.CookieHeaderNames;
import jakarta.annotation.Nonnull;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.RequestBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public class NettyAsyncHttpClient implements HttpClient {

    private final AsyncHttpClient client;

    public NettyAsyncHttpClient() {
        this(Dsl.asyncHttpClient());
    }

    public NettyAsyncHttpClient(@Nonnull AsyncHttpClient client) {
        this.client = client;
    }

    @Nonnull
    @Override
    public CompletionStage<? extends Response> execute(@Nonnull Request request) {
        return this.client.executeRequest(toBuilder(request))
                .toCompletableFuture()
                .exceptionally(error -> {
                    throw new HttpClientException(error.getMessage(), error);
                })
                .thenApply(DefaultResponse::new);
    }

    private static RequestBuilder toBuilder(@Nonnull Request request) {
        final RequestBuilder builder = toBuilder(request.getMethod(), request.getUri().toString());
        if (!request.getHeaders().isEmpty()) {
            for (Header header : request.getHeaders()) {
                builder.addHeader(header.getName(), header.getValues());
            }
        }
        if (CollectionUtils.isNotEmpty(request.getCookies())) {
            for (Cookie cookie : request.getCookies()) {
                builder.addCookie(toCookie(cookie));
            }
        }
        if (request.getCharset() != null) {
            builder.setCharset(request.getCharset());
        }
        if (CollectionUtils.isNotEmpty(request.getQueryParams())) {
            for (NameValuePair param : request.getQueryParams()) {
                builder.addQueryParam(param.getName(), param.getValue());
            }
        }
        if (CollectionUtils.isNotEmpty(request.getFormParams())) {
            for (NameValuePair param : request.getFormParams()) {
                builder.addFormParam(param.getName(), param.getValue());
            }
        }
        if (request.getBody() != null) {
            builder.setBody(request.getBody().getContent());
            builder.setHeader(request.getBody().getContentTypeHeader().getName(), request.getBody().getContentTypeHeader().getValues());
        }
        return builder;
    }

    @Nonnull
    private static io.netty.handler.codec.http.cookie.Cookie toCookie(@Nonnull Cookie cookie) {
        final io.netty.handler.codec.http.cookie.DefaultCookie defaultCookie = new io.netty.handler.codec.http.cookie.DefaultCookie(cookie.name(), cookie.value());
        defaultCookie.setDomain(cookie.domain());
        defaultCookie.setPath(cookie.path());
        defaultCookie.setMaxAge(cookie.maxAge());
        defaultCookie.setSecure(cookie.isSecure());
        defaultCookie.setHttpOnly(cookie.isHttpOnly());
        if (cookie.sameSite() != null) {
            defaultCookie.setSameSite(CookieHeaderNames.SameSite.valueOf(cookie.sameSite().name()));
        }
        return defaultCookie;
    }

    private static RequestBuilder toBuilder(@Nonnull HttpMethod method, @Nonnull String url) {
        if ("get".equalsIgnoreCase(method.name())) {
            return Dsl.get(url);
        }
        if ("post".equalsIgnoreCase(method.name())) {
            return Dsl.post(url);
        }
        if ("put".equalsIgnoreCase(method.name())) {
            return Dsl.put(url);
        }
        if ("delete".equalsIgnoreCase(method.name())) {
            return Dsl.delete(url);
        }
        if ("patch".equalsIgnoreCase(method.name())) {
            return Dsl.patch(url);
        }
        throw new IllegalArgumentException("method must be one of get, post, put, delete, patch");
    }

    @Override
    public void close() throws IOException {
        this.client.close();
    }

    protected static class DefaultResponse implements Response {

        private final org.asynchttpclient.Response response;

        protected DefaultResponse(org.asynchttpclient.Response response) {
            this.response = response;
        }

        @Override
        public int getStatusCode() {
            return this.response.getStatusCode();
        }

        @Override
        public String getStatusText() {
            return this.response.getStatusText();
        }

        @Override
        public String getBody(@Nonnull Charset charset) {
            return this.response.getResponseBody(charset);
        }

        @Override
        public InputStream getBodyAsStream() {
            return this.response.getResponseBodyAsStream();
        }

        @Override
        public byte[] getBodyAsBytes() {
            return this.response.getResponseBodyAsBytes();
        }

        @Override
        public URI getUri() {
            try {
                return new URI(this.response.getUri().toString());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public String getContentType() {
            return this.response.getContentType();
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            return new AsyncHttpHeaders(this.response.getHeaders());
        }

        @Nonnull
        @Override
        public List<? extends Cookie> getCookies() {
            final List<io.netty.handler.codec.http.cookie.Cookie> cookies = this.response.getCookies();
            if (CollectionUtils.isEmpty(cookies)) {
                return Collections.emptyList();
            }
            final List<Cookie> list = new ArrayList<>();
            for (io.netty.handler.codec.http.cookie.Cookie cookie : cookies) {
                final Cookie.Builder builder = Cookie.builder(cookie.name())
                        .value(cookie.value())
                        .domain(cookie.domain())
                        .path(cookie.path())
                        .maxAge(cookie.maxAge())
                        .isSecure(cookie.isSecure())
                        .isHttpOnly(cookie.isHttpOnly());
                if (cookie instanceof io.netty.handler.codec.http.cookie.DefaultCookie) {
                    final CookieHeaderNames.SameSite sameSite = ((io.netty.handler.codec.http.cookie.DefaultCookie) cookie).sameSite();
                    if (sameSite != null) {
                        builder.sameSite(Cookie.SameSite.of(sameSite.name()));
                    }
                }
                list.add(builder.build());
            }
            return list;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return this.response.getRemoteAddress();
        }

        @Override
        public String toString() {
            return this.response.toString();
        }
    }

    private static final class AsyncHttpHeaders implements HttpHeaders {

        private final io.netty.handler.codec.http.HttpHeaders headers;

        private AsyncHttpHeaders(@Nonnull io.netty.handler.codec.http.HttpHeaders headers) {
            this.headers = headers;
        }

        @Override
        public boolean isEmpty() {
            return this.headers.isEmpty();
        }

        @Override
        public Header getHeader(@Nonnull String name) {
            final List<String> list = this.headers.getAll(name);
            return new DefaultHeader(name, list == null ? new ArrayList<>() : list);
        }

        @Nonnull
        @Override
        public Iterator<Header> iterator() {
            final Iterator<String> iterator = this.headers.names().iterator();
            return new Iterator<Header>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Header next() {
                    return getHeader(iterator.next());
                }
            };
        }

        @Override
        public String toString() {
            return this.headers.toString();
        }
    }
}
