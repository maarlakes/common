package cn.maarlakes.common.http.ok;

import cn.maarlakes.common.function.Function0;
import cn.maarlakes.common.http.Request;
import cn.maarlakes.common.http.Response;
import cn.maarlakes.common.http.ResponseBody;
import cn.maarlakes.common.http.*;
import cn.maarlakes.common.http.body.multipart.MultipartPart;
import cn.maarlakes.common.utils.CollectionUtils;
import cn.maarlakes.common.utils.Lazy;
import jakarta.annotation.Nonnull;
import okhttp3.Cookie;
import okhttp3.RequestBody;
import okhttp3.*;
import okhttp3.internal.connection.Exchange;
import okhttp3.internal.connection.RealConnection;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public class OkAsyncHttpClient implements HttpClient {

    private final OkHttpClient client;

    public OkAsyncHttpClient() {
        this(new OkHttpClient.Builder().build());
    }

    public OkAsyncHttpClient(@Nonnull OkHttpClient client) {
        this.client = client;
    }

    @Nonnull
    @Override
    public CompletionStage<? extends Response> execute(@Nonnull Request request, RequestConfig config) {
        try {
            final HttpUrl.Builder builder = Objects.requireNonNull(HttpUrl.get(request.getUri())).newBuilder();
            if (CollectionUtils.isNotEmpty(request.getQueryParams())) {
                for (NameValuePair param : request.getQueryParams()) {
                    builder.addQueryParameter(param.getName(), param.getValue());
                }
            }
            final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder().method(request.getMethod().name(), toRequestBody(request)).url(builder.build());
            if (!request.getHeaders().isEmpty()) {
                for (Header header : request.getHeaders()) {
                    for (String value : header.getValues()) {
                        requestBuilder.addHeader(header.getName(), value);
                    }
                }
            }
            final List<cn.maarlakes.common.http.Cookie> responseCookies = new ArrayList<>();
            final OkHttpClient client = this.getClient(request, responseCookies, config);
            final CompletableFuture<Response> future = new CompletableFuture<>();
            client.newCall(requestBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(@Nonnull Call call, @Nonnull IOException e) {
                    future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                }

                @Override
                public void onResponse(@Nonnull Call call, @Nonnull okhttp3.Response response) throws IOException {
                    future.complete(new DefaultResponse(response, responseCookies));
                }
            });
            return future;
        } catch (Exception e) {
            return CompletableFuture.supplyAsync(() -> {
                throw new HttpClientException(e.getMessage(), e);
            });
        }
    }

    private OkHttpClient getClient(@Nonnull Request request, final List<cn.maarlakes.common.http.Cookie> responseCookies, RequestConfig config) {
        final List<? extends cn.maarlakes.common.http.Cookie> cookies = request.getCookies();
        final OkHttpClient.Builder builder = client.newBuilder().cookieJar(new CookieJar() {
            @Override
            public void saveFromResponse(@Nonnull HttpUrl httpUrl, @Nonnull List<Cookie> list) {
                if (CollectionUtils.isNotEmpty(list)) {
                    for (Cookie cookie : list) {
                        final cn.maarlakes.common.http.Cookie.Builder builder = cn.maarlakes.common.http.Cookie.builder(cookie.name())
                                .value(cookie.value())
                                .domain(cookie.domain())
                                .path(cookie.path());
                        if (cookie.expiresAt() > 0) {
                            builder.maxAge((System.currentTimeMillis() - cookie.expiresAt()) / 1000L);
                        }
                        builder.isSecure(cookie.secure());
                        builder.isHttpOnly(cookie.hostOnly());
                        responseCookies.add(builder.build());
                    }
                }
            }

            @Nonnull
            @Override
            public List<Cookie> loadForRequest(@Nonnull HttpUrl httpUrl) {
                final List<Cookie> list = new ArrayList<>();
                for (cn.maarlakes.common.http.Cookie cookie : cookies) {
                    final Cookie.Builder cookieBuilder = new Cookie.Builder()
                            .name(cookie.name())
                            .value(cookie.value());
                    if (cookie.domain() != null) {
                        cookieBuilder.domain(cookie.domain());
                    } else {
                        cookieBuilder.domain(httpUrl.host());
                    }
                    if (cookie.path() != null) {
                        cookieBuilder.path(cookie.path());
                    }
                    if (cookie.maxAge() > 0L) {
                        cookieBuilder.expiresAt(System.currentTimeMillis() + cookie.maxAge() * 1000);
                    }
                    if (cookie.isSecure()) {
                        cookieBuilder.secure();
                    }
                    if (cookie.isHttpOnly()) {
                        cookieBuilder.httpOnly();
                    }
                    list.add(cookieBuilder.build());
                }
                return list;
            }
        });
        if (config != null) {
            if (config.getConnectTimeout() != null) {
                builder.connectTimeout(config.getConnectTimeout());
            }
            if (config.getRequestTimeout() != null) {
                builder.writeTimeout(config.getRequestTimeout());
            }
            if (config.getResponseTimeout() != null) {
                builder.readTimeout(config.getResponseTimeout());
            }
        }
        return builder.build();
    }

    private static RequestBody toRequestBody(@Nonnull Request request) throws Exception {
        final cn.maarlakes.common.http.RequestBody<?> body = request.getBody();
        if (body != null) {
            if (body instanceof cn.maarlakes.common.http.body.multipart.MultipartBody) {
                final cn.maarlakes.common.http.body.multipart.MultipartBody multipartBody = (cn.maarlakes.common.http.body.multipart.MultipartBody) request.getBody();
                if (CollectionUtils.isNotEmpty(multipartBody.getContent())) {
                    final MultipartBody.Builder builder = new MultipartBody.Builder();
                    builder.setType(Objects.requireNonNull(MediaType.parse(ContentTypes.toString(multipartBody.getContentType()))));
                    for (MultipartPart<?> part : multipartBody.getContent()) {
                        final Headers.Builder headers = new Headers.Builder();
                        if (!part.getHeaders().isEmpty()) {
                            for (Header header : part.getHeaders()) {
                                for (String value : header.getValues()) {
                                    headers.add(header.getName(), value);
                                }
                            }
                        }
                        builder.addFormDataPart(part.getName(), part.getFilename(), new ContentRequestBody(part));
                    }
                    return builder.build();
                }
            } else {
                return new ContentRequestBody(body);
            }
        }

        if (CollectionUtils.isNotEmpty(request.getFormParams())) {
            final FormBody.Builder builder = new FormBody.Builder();
            for (NameValuePair param : request.getFormParams()) {
                builder.add(param.getName(), param.getValue());
            }
            return builder.build();
        }
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    private static class DefaultResponse implements Response {

        private final okhttp3.Response response;
        private final List<cn.maarlakes.common.http.Cookie> cookies;
        private final Function0<ResponseBody> bodyFactory;

        private DefaultResponse(@Nonnull okhttp3.Response response, List<cn.maarlakes.common.http.Cookie> cookies) {
            this.response = response;
            this.cookies = cookies;

            this.bodyFactory = Lazy.of(() -> {
                final okhttp3.ResponseBody body = response.body();
                if (body == null) {
                    return new ByteArrayResponseBody(new byte[0], null, null);
                }
                return new ByteArrayResponseBody(
                        body.bytes(),
                        Optional.ofNullable(body.contentType()).map(MediaType::toString).map(ContentType::parse).orElse(null),
                        this.getHeaders().getHeader(HttpHeaderNames.CONTENT_ENCODING)
                );
            });
        }

        @Override
        public int getStatusCode() {
            return this.response.code();
        }

        @Override
        public String getStatusText() {
            return this.response.message();
        }

        @Nonnull
        @Override
        public ResponseBody getBody() {
            return this.bodyFactory.get();
        }

        @Override
        public URI getUri() {
            return this.response.request().url().uri();
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            final Set<String> names = this.response.headers().names();
            return new HttpHeaders() {
                @Override
                public boolean isEmpty() {
                    return names.isEmpty();
                }

                @Override
                public Header getHeader(@Nonnull String name) {
                    return new DefaultHeader(name, response.headers(name));
                }

                @Nonnull
                @Override
                public Iterator<Header> iterator() {
                    final Iterator<String> iterator = names.iterator();
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
                    return response.headers().toString();
                }
            };
        }

        @Nonnull
        @Override
        public List<? extends cn.maarlakes.common.http.Cookie> getCookies() {
            return new ArrayList<>(this.cookies);
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return Optional.ofNullable(this.response.exchange())
                    .map(Exchange::getConnection$okhttp)
                    .map(RealConnection::route)
                    .map(Route::socketAddress)
                    .orElse(null);
        }
    }
}
