package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.http.*;
import cn.maarlakes.common.utils.CollectionUtils;
import jakarta.annotation.Nonnull;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EndpointDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.nio.entity.BasicAsyncEntityConsumer;
import org.apache.hc.core5.http.nio.support.AbstractAsyncResponseConsumer;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.reactor.IOReactorStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public class ApacheAsyncHttpClient implements HttpClient {

    private final CloseableHttpAsyncClient client;

    public ApacheAsyncHttpClient() {
        this(HttpAsyncClientBuilder.create().build());
    }

    public ApacheAsyncHttpClient(@Nonnull CloseableHttpAsyncClient httpClient) {
        this.client = httpClient;
        if (httpClient.getStatus() != IOReactorStatus.ACTIVE) {
            httpClient.start();
        }
    }

    @Nonnull
    @Override
    public CompletionStage<? extends Response> execute(@Nonnull Request request) {
        try {
            final URIBuilder uriBuilder = new URIBuilder(request.getUri());
            if (CollectionUtils.isNotEmpty(request.getQueryParams())) {
                for (NameValuePair param : request.getQueryParams()) {
                    uriBuilder.addParameter(param.getName(), param.getValue());
                }
            }

            final AsyncRequestBuilder builder = AsyncRequestBuilder.create(request.getMethod().name())
                    .setUri(uriBuilder.build());
            if (!request.getHeaders().isEmpty()) {
                for (Header header : request.getHeaders()) {
                    for (String value : header.getValues()) {
                        builder.addHeader(header.getName(), value);
                    }
                }
            }
            if (request.getCharset() != null) {
                builder.setCharset(request.getCharset());
            }
            if (CollectionUtils.isNotEmpty(request.getFormParams())) {
                for (NameValuePair param : request.getFormParams()) {
                    builder.addParameter(param.getName(), param.getValue());
                }
            }
            if (request.getBody() != null) {
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    request.getBody().writeTo(out);
                    builder.setEntity(out.toByteArray(), toApacheContentType(request.getBody().getContentType()));
                }
            }
            final HttpContext context = HttpClientContext.create();
            context.setAttribute(HttpClientContext.COOKIE_STORE, new BasicCookieStore());
            if (CollectionUtils.isNotEmpty(request.getCookies())) {
                builder.addHeader("Cookie", request.getCookies().stream().map(item -> item.name() + "=" + item.value()).collect(Collectors.joining(";")));
            }
            final CompletableFuture<Response> future = new CompletableFuture<>();

            this.client.execute(builder.build(), new ResponseAsyncResponseConsumer(request.getUri(), context), context, new FutureCallback<Response>() {
                @Override
                public void completed(Response response) {
                    future.complete(response);
                }

                @Override
                public void failed(Exception ex) {
                    future.completeExceptionally(new HttpClientException(ex.getMessage(), ex));
                }

                @Override
                public void cancelled() {
                    future.cancel(true);
                }
            });
            return future;
        } catch (Exception e) {
            return CompletableFuture.supplyAsync(() -> {
                throw new HttpClientException(e.getMessage(), e);
            });
        }
    }

    @Override
    public void close() throws IOException {
        this.client.close();
    }

    private static Cookie toApacheCookie(@Nonnull cn.maarlakes.common.http.Cookie cookie) {
        final BasicClientCookie apacheCookie = new BasicClientCookie(cookie.name(), cookie.value());
        apacheCookie.setDomain(cookie.domain());
        apacheCookie.setPath(cookie.path());
        if (cookie.maxAge() > 0L) {
            apacheCookie.setExpiryDate(new Date(System.currentTimeMillis() + cookie.maxAge()));
        }
        apacheCookie.setSecure(cookie.isSecure());
        apacheCookie.setCreationDate(new Date());
        apacheCookie.setAttribute("HttpOnly", cookie.isHttpOnly() + "");
        if (cookie.sameSite() != null) {
            apacheCookie.setAttribute("SameSite", cookie.sameSite().name());
        }
        return apacheCookie;
    }

    private static org.apache.hc.core5.http.ContentType toApacheContentType(@Nonnull ContentType contentType) {
        final org.apache.hc.core5.http.ContentType result = org.apache.hc.core5.http.ContentType.create(contentType.getMediaType(), contentType.getCharset());
        if (CollectionUtils.isNotEmpty(contentType.getParameters())) {
            return result.withParameters(
                    contentType.getParameters().stream().map(item -> new BasicNameValuePair(item.getName(), item.getValue()))
                            .toArray(org.apache.hc.core5.http.NameValuePair[]::new)
            );
        }
        return result;
    }

    private static class ResponseAsyncResponseConsumer extends AbstractAsyncResponseConsumer<Response, byte[]> {
        private final URI uri;
        private final HttpContext context;

        public ResponseAsyncResponseConsumer(@Nonnull URI uri, HttpContext context) {
            super(new BasicAsyncEntityConsumer());
            this.uri = uri;
            this.context = context;
        }

        @Override
        protected Response buildResult(HttpResponse response, byte[] entity, org.apache.hc.core5.http.ContentType contentType) {
            return new DefaultResponse(this.uri, this.context, response, entity, contentType);
        }

        @Override
        public void informationResponse(HttpResponse response, HttpContext context) throws HttpException, IOException {

        }
    }

    private static class DefaultResponse implements Response {

        private final URI uri;
        private final HttpContext context;
        private final HttpResponse response;
        private final byte[] bodyBuffer;
        private final org.apache.hc.core5.http.ContentType contentType;

        private DefaultResponse(@Nonnull URI uri, HttpContext context, @Nonnull HttpResponse response, byte[] bodyBuffer, org.apache.hc.core5.http.ContentType contentType) {
            this.uri = uri;
            this.context = context;
            this.response = response;
            this.bodyBuffer = bodyBuffer;
            this.contentType = contentType;
        }

        @Override
        public int getStatusCode() {
            return this.response.getCode();
        }

        @Override
        public String getStatusText() {
            return this.response.getReasonPhrase();
        }

        @Override
        public String getBody(@Nonnull Charset charset) {
            if (this.bodyBuffer == null) {
                return null;
            }
            return new String(this.bodyBuffer, charset);
        }

        @Override
        public InputStream getBodyAsStream() {
            if (this.bodyBuffer == null) {
                return null;
            }
            return new ByteArrayInputStream(this.bodyBuffer);
        }

        @Override
        public byte[] getBodyAsBytes() {
            return Arrays.copyOf(this.bodyBuffer, this.bodyBuffer.length);
        }

        @Override
        public URI getUri() {
            return this.uri;
        }

        @Override
        public String getContentType() {
            if (this.contentType == null) {
                return null;
            }
            return this.contentType.toString();
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            final Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (org.apache.hc.core5.http.Header header : this.response.getHeaders()) {
                map.computeIfAbsent(header.getName(), k -> new ArrayList<>()).add(header.getValue());
            }
            final Map<String, Header> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            map.forEach((k, v) -> headers.put(k, new DefaultHeader(k, v)));
            return new DefaultHttpHeaders(headers);
        }

        @Nonnull
        @Override
        public List<? extends cn.maarlakes.common.http.Cookie> getCookies() {
            final CookieStore cookieStore = (CookieStore) context.getAttribute(HttpClientContext.COOKIE_STORE);
            final List<Cookie> cookies = cookieStore.getCookies();
            if (CollectionUtils.isEmpty(cookies)) {
                return new ArrayList<>();
            }
            final List<cn.maarlakes.common.http.Cookie> list = new ArrayList<>();
            for (Cookie cookie : cookies) {
                final cn.maarlakes.common.http.Cookie.Builder builder = cn.maarlakes.common.http.Cookie.builder(cookie.getName())
                        .value(cookie.getValue())
                        .domain(cookie.getDomain())
                        .path(cookie.getPath())
                        .isSecure(cookie.isSecure());
                if (cookie.getCreationDate() != null && cookie.getExpiryDate() != null) {
                    builder.maxAge((cookie.getExpiryDate().getTime() - cookie.getCreationDate().getTime()) / 1000L);
                }
                builder.isHttpOnly(Boolean.parseBoolean(cookie.getAttribute("httponly")));
                String sameSite = cookie.getAttribute("samesite");
                if (sameSite == null) {
                    sameSite = cookie.getAttribute("same-site");
                }
                if (sameSite != null) {
                    builder.sameSite(cn.maarlakes.common.http.Cookie.SameSite.of(sameSite));
                }
                list.add(builder.build());
            }
            return list;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            final EndpointDetails endpoint = (EndpointDetails) this.context.getAttribute(HttpClientContext.CONNECTION_ENDPOINT);
            if (endpoint == null) {
                return null;
            }
            return endpoint.getRemoteAddress();
        }
    }
}
