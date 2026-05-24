package cn.maarlakes.common.http.async;

import cn.maarlakes.common.http.*;
import cn.maarlakes.common.http.Request;
import cn.maarlakes.common.http.Response;
import cn.maarlakes.common.http.body.multipart.FilePart;
import cn.maarlakes.common.http.body.multipart.MultipartBody;
import cn.maarlakes.common.http.body.multipart.MultipartPart;
import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.CollectionUtils;
import io.netty.handler.codec.http.cookie.CookieHeaderNames;
import jakarta.annotation.Nonnull;
import org.asynchttpclient.*;
import org.asynchttpclient.netty.ssl.JsseSslEngineFactory;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.request.body.multipart.InputStreamPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author linjpxc
 */
public class NettyAsyncHttpClient implements HttpClient {

    private static final Logger log = LoggerFactory.getLogger(NettyAsyncHttpClient.class);

    private final AsyncHttpClient client;
    private final RequestConfig defaultConfig;

    public NettyAsyncHttpClient() {
        this(Dsl.asyncHttpClient(), null);
    }

    public NettyAsyncHttpClient(RequestConfig defaultConfig) {
        this(Dsl.asyncHttpClient(), defaultConfig);
    }

    public NettyAsyncHttpClient(@Nonnull SSLContext sslContext) {
        this(new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setSslEngineFactory(new JsseSslEngineFactory(sslContext))
                        .build()
        ), null);
    }

    public NettyAsyncHttpClient(@Nonnull SSLContext sslContext, RequestConfig defaultConfig) {
        this(new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setSslEngineFactory(new JsseSslEngineFactory(sslContext))
                        .build()
        ), defaultConfig);
    }

    public NettyAsyncHttpClient(@Nonnull AsyncHttpClient client) {
        this(client, null);
    }

    public NettyAsyncHttpClient(@Nonnull AsyncHttpClient client, RequestConfig defaultConfig) {
        this.client = client;
        this.defaultConfig = defaultConfig;
    }

    @Nonnull
    @Override
    public CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config) {
        final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
        return this.client.executeRequest(toBuilder(request, effectiveConfig))
                .toCompletableFuture()
                .exceptionally(error -> {
                    throw new HttpClientException(error.getMessage(), error);
                })
                .thenApply(DefaultResponse::new);
    }

    @Nonnull
    @Override
    public <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler) {
        final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
        final AtomicReference<ListenableFuture<Void>> listenableFutureRef = new AtomicReference<>();
        final CompletableFuture<T> result = new CompletableFuture<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (mayInterruptIfRunning) {
                    final ListenableFuture<Void> lf = listenableFutureRef.get();
                    if (lf != null) {
                        lf.cancel(true);
                    }
                }
                return super.cancel(mayInterruptIfRunning);
            }
        };
        final cn.maarlakes.common.http.PushBodySink sink = new cn.maarlakes.common.http.PushBodySink();
        final ListenableFuture<Void> listenableFuture = this.client.executeRequest(
                toBuilder(request, effectiveConfig),
                new AsyncCompletionHandler<Void>() {
                    private HttpResponseStatus status;
                    private io.netty.handler.codec.http.HttpHeaders headers;
                    private final AtomicBoolean handlerInvoked = new AtomicBoolean();

                    @Override
                    public State onStatusReceived(HttpResponseStatus status) throws Exception {
                        this.status = status;
                        return State.CONTINUE;
                    }

                    @Override
                    public State onHeadersReceived(io.netty.handler.codec.http.HttpHeaders headers) throws Exception {
                        this.headers = headers;
                        super.onHeadersReceived(headers);
                        invokeHandler();
                        return State.CONTINUE;
                    }

                    @Override
                    public State onBodyPartReceived(HttpResponseBodyPart part) throws Exception {
                        sink.pushChunk(part.getBodyPartBytes(), 0, part.length());
                        return State.CONTINUE;
                    }

                    @Override
                    public Void onCompleted(org.asynchttpclient.Response response) throws Exception {
                        if (!handlerInvoked.get()) {
                            invokeHandler();
                        }
                        sink.complete();
                        return null;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        if (handlerInvoked.get()) {
                            sink.fail(t);
                        } else {
                            result.completeExceptionally(new HttpClientException(t.getMessage(), t));
                        }
                    }

                    private void invokeHandler() {
                        if (handlerInvoked.compareAndSet(false, true)) {
                            final HttpResponse httpResponse = createResponseInfo(status, headers, request.getUri());
                            final CompletableFuture<T> handlerFuture = handler.handle(httpResponse, sink);
                            handlerFuture.whenComplete((val, err) -> {
                                if (err != null) {
                                    result.completeExceptionally(err);
                                } else {
                                    result.complete(val);
                                }
                            });
                        }
                    }
                }
        );
        listenableFutureRef.set(listenableFuture);
        listenableFuture.toCompletableFuture().exceptionally(ex -> {
            if (!result.isDone()) {
                result.completeExceptionally(ex);
            }
            return null;
        });
        return result;
    }

    private static RequestBuilder toBuilder(@Nonnull Request request, RequestConfig config) {
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
            if (request.getBody() instanceof MultipartBody) {
                final MultipartBody multipartBody = (MultipartBody) request.getBody();
                if (CollectionUtils.isNotEmpty(multipartBody.getContent())) {
                    for (MultipartPart<?> part : multipartBody.getContent()) {
                        final Header contentId = part.getHeaders().getHeader("Content-ID");
                        final Header contentTransferEncoding = part.getHeaders().getHeader("Content-Transfer-Encoding");
                        if (part instanceof FilePart) {
                            builder.addBodyPart(new org.asynchttpclient.request.body.multipart.FilePart(
                                    part.getName(), ((FilePart) part).getContent(), part.getContentType() == null ? null : ContentTypes.toString(part.getContentType()),
                                    part.getCharset(), part.getFilename(), contentId == null ? null : contentId.get(),
                                    contentTransferEncoding == null ? null : contentTransferEncoding.get()));
                        } else {
                            builder.addBodyPart(new InputStreamPart(
                                    part.getName(), part.getContentStream(), part.getFilename(), -1,
                                    part.getContentType() == null ? null : ContentTypes.toString(part.getContentType()),
                                    part.getCharset(), contentId == null ? null : contentId.get(),
                                    contentTransferEncoding == null ? null : contentTransferEncoding.get()));
                        }
                    }
                }
            } else {
                builder.setBody(request.getBody().getContentStream());
                final Header contentTypeHeader = request.getBody().getContentTypeHeader();
                if (contentTypeHeader != null) {
                    builder.setHeader(contentTypeHeader.getName(), contentTypeHeader.getValues());
                }
            }
        }
        if (config != null) {
            if (config.isRedirectsEnabled() != null) {
                builder.setFollowRedirect(config.isRedirectsEnabled());
            }
            if (config.getResponseTimeout() != null) {
                builder.setReadTimeout((int) config.getResponseTimeout().toMillis());
            }
            if (config.getRequestTimeout() != null) {
                builder.setRequestTimeout((int) config.getRequestTimeout().toMillis());
            }
            if (config.getProxy() != null) {
                if (config.getProxyAuthentication() == null) {
                    final InetSocketAddress address = (InetSocketAddress) config.getProxy().address();
                    builder.setProxyServer(new ProxyServer.Builder(address.getHostName(), address.getPort()));
                } else {
                    for (ProxyAuthenticator authenticator : SpiServiceLoader.loadShared(ProxyAuthenticator.class, NettyAsyncHttpClient.class.getClassLoader())) {
                        final ProxyServer.Builder proxyServerBuilder = authenticator.authenticate(config.getProxy(), config.getProxyAuthentication());
                        if (proxyServerBuilder != null) {
                            builder.setProxyServer(proxyServerBuilder);
                            break;
                        }
                    }
                }
            }
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

    private static HttpResponse createResponseInfo(HttpResponseStatus status, io.netty.handler.codec.http.HttpHeaders headers, URI uri) {
        final HttpHeaders httpHeaders = new AsyncHttpHeaders(headers);
        return new DefaultHttpResponse(
                status.getStatusCode(),
                status.getStatusText(),
                httpHeaders,
                uri,
                Cookies.parseFromHeaders(httpHeaders),
                status.getRemoteAddress()
        );
    }


    private static RequestBuilder toBuilder(@Nonnull HttpMethod method, @Nonnull String url) {
        return Dsl.request(method.name(), url);
    }

    @Override
    public void close() {
        try {
            this.client.close();
        } catch (Exception ignored) {
        }
    }

    protected static class DefaultResponse implements Response {

        private final org.asynchttpclient.Response response;
        private volatile ResponseBody body;

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

        @Nonnull
        @Override
        public ResponseBody getBody() {
            if (this.body == null) {
                synchronized (this) {
                    if (this.body == null) {
                        this.body = new ByteArrayResponseBody(
                                this.response.getResponseBodyAsBytes(),
                                Optional.ofNullable(this.response.getContentType()).map(ContentType::parse).orElse(null),
                                this.getHeaders().getHeader(HttpHeaderNames.CONTENT_ENCODING)
                        );
                    }
                }
            }
            return this.body;
        }

        @Override
        public URI getUri() {
            try {
                return new URI(this.response.getUri().toString());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
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
