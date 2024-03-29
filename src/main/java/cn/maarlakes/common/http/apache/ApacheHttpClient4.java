package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.factory.datetime.DateTimeFactories;
import cn.maarlakes.common.http.*;
import cn.maarlakes.common.utils.CollectionUtils;
import jakarta.annotation.Nonnull;
import org.apache.http.HttpEntity;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public class ApacheHttpClient4 implements HttpClient {

    private final CloseableHttpClient client;
    private final Executor executor;

    public ApacheHttpClient4() {
        this(HttpClientBuilder.create().build(), new ForkJoinPool());
    }

    public ApacheHttpClient4(@Nonnull Executor executor) {
        this(HttpClientBuilder.create().build(), executor);
    }

    public ApacheHttpClient4(@Nonnull CloseableHttpClient client) {
        this(client, new ForkJoinPool());
    }

    public ApacheHttpClient4(@Nonnull CloseableHttpClient client, @Nonnull Executor executor) {
        this.client = client;
        this.executor = executor;
    }

    @Nonnull
    @Override
    @SuppressWarnings("DuplicatedCode")
    public CompletionStage<? extends Response> execute(@Nonnull Request request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final RequestBuilder builder = RequestBuilder.create(request.getMethod().name())
                        .setUri(toUri(request));
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
                    final InputStream content = request.getBody().getContent();
                    if (content != null) {
                        builder.setEntity(new InputStreamEntity(content, toApacheContentType(request.getBody().getContentType())));
                    }

                }
                final HttpContext context = HttpClientContext.create();
                context.setAttribute(HttpClientContext.COOKIE_STORE, new BasicCookieStore());
                if (CollectionUtils.isNotEmpty(request.getCookies())) {
                    builder.addHeader("Cookie", request.getCookies().stream().map(item -> item.name() + "=" + item.value()).collect(Collectors.joining(";")));
                }

                return this.client.execute(builder.build(), (ResponseHandler<Response>) response -> new DefaultResponse(request.getUri(), response, context), context);
            } catch (Exception e) {
                throw new HttpClientException(e.getMessage(), e);
            }
        }, this.executor);
    }

    @Override
    public void close() throws IOException {
        this.client.close();
        if (this.executor instanceof ExecutorService) {
            ((ExecutorService) this.executor).shutdown();
        }
    }

    @Nonnull
    public static URI toUri(@Nonnull Request request) throws URISyntaxException {
        final URIBuilder builder = new URIBuilder(request.getUri());
        if (CollectionUtils.isNotEmpty(request.getQueryParams())) {
            for (NameValuePair param : request.getQueryParams()) {
                builder.addParameter(param.getName(), param.getValue());
            }
        }
        return builder.build();
    }

    public static org.apache.http.entity.ContentType toApacheContentType(@Nonnull ContentType contentType) {
        final org.apache.http.entity.ContentType result = org.apache.http.entity.ContentType.create(contentType.getMediaType(), contentType.getCharset());
        if (CollectionUtils.isNotEmpty(contentType.getParameters())) {
            return result.withParameters(
                    contentType.getParameters().stream().map(item -> new BasicNameValuePair(item.getName(), item.getValue()))
                            .toArray(org.apache.http.NameValuePair[]::new)
            );
        }
        return result;
    }

    private static class DefaultResponse implements Response {
        private final URI uri;
        private final HttpResponse response;
        private final HttpContext context;
        private final byte[] body;
        private final String contentType;
        private final SocketAddress remoteAddress;

        private DefaultResponse(URI uri, HttpResponse response, HttpContext context) {
            this.uri = uri;
            this.response = response;
            this.contentType = Optional.ofNullable(response.getEntity()).map(HttpEntity::getContentType).map(Object::toString).orElse(null);
            final Object attribute = context.getAttribute("http.connection");
            if (attribute instanceof HttpInetConnection) {
                final HttpInetConnection connection = (HttpInetConnection) attribute;
                remoteAddress = new InetSocketAddress(connection.getRemoteAddress(), connection.getRemotePort());
            } else {
                remoteAddress = null;
            }

            try {
                this.body = EntityUtils.toByteArray(response.getEntity());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.context = context;
        }

        @Override
        public int getStatusCode() {
            return this.response.getStatusLine().getStatusCode();
        }

        @Override
        public String getStatusText() {
            return this.response.getStatusLine().getReasonPhrase();
        }

        @Override
        public String getBody(@Nonnull Charset charset) {
            final byte[] buffer = this.body;
            if (buffer == null) {
                return null;
            }
            return new String(buffer, charset);
        }

        @Override
        public InputStream getBodyAsStream() {
            final byte[] buffer = this.body;
            if (buffer == null) {
                return null;
            }
            return new ByteArrayInputStream(buffer);
        }

        @Override
        public byte[] getBodyAsBytes() {
            final byte[] buffer = this.body;
            if (buffer == null) {
                return null;
            }
            return Arrays.copyOf(buffer, buffer.length);
        }

        @Override
        public URI getUri() {
            return this.uri;
        }

        @Override
        public String getContentType() {
            return this.contentType;
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            final Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (org.apache.http.Header header : this.response.getAllHeaders()) {
                map.computeIfAbsent(header.getName(), k -> new ArrayList<>()).add(header.getValue());
            }
            final Map<String, Header> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            map.forEach((k, v) -> headers.put(k, new DefaultHeader(k, v)));
            return new DefaultHttpHeaders(headers);
        }

        @Nonnull
        @Override
        public List<? extends Cookie> getCookies() {
            final CookieStore cookieStore = (CookieStore) context.getAttribute(HttpClientContext.COOKIE_STORE);
            final List<org.apache.http.cookie.Cookie> cookies = cookieStore.getCookies();
            if (CollectionUtils.isEmpty(cookies)) {
                return new ArrayList<>();
            }
            final List<Cookie> list = new ArrayList<>();
            for (org.apache.http.cookie.Cookie cookie : cookies) {
                final cn.maarlakes.common.http.Cookie.Builder builder = cn.maarlakes.common.http.Cookie.builder(cookie.getName())
                        .value(cookie.getValue())
                        .domain(cookie.getDomain())
                        .path(cookie.getPath())
                        .isSecure(cookie.isSecure());
                if (cookie.getExpiryDate() != null) {
                    builder.expires(DateTimeFactories.fromEpochMilli(cookie.getExpiryDate().getTime()));
                }
                list.add(builder.build());
            }
            return list;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return this.remoteAddress;
        }
    }
}
