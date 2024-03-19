package cn.maarlakes.common.http;

import cn.maarlakes.common.utils.CollectionUtils;
import cn.maarlakes.common.utils.StreamUtils;
import jakarta.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public class JdkHttpClient implements HttpClient {

    private final Executor executor;

    public JdkHttpClient() {
        this(new ForkJoinPool());
    }

    public JdkHttpClient(@Nonnull Executor executor) {
        this.executor = executor;
    }

    @Nonnull
    @Override
    public CompletionStage<? extends Response> execute(@Nonnull Request request) {
        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection connection = null;
            try {
                final URL url = toUrl(request);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestMethod(request.getMethod().name());
                if (!request.getHeaders().isEmpty()) {
                    for (Header header : request.getHeaders()) {
                        connection.setRequestProperty(header.getName(), header.get());
                    }
                }
                if (CollectionUtils.isNotEmpty(request.getCookies())) {
                    connection.setRequestProperty("Cookie", request.getCookies().stream().map(item -> item.name() + "=" + item.value()).collect(Collectors.joining(";")));
                }
                Request.Body body = request.getBody();
                if (body == null && CollectionUtils.isNotEmpty(request.getFormParams())) {
                    body = new UrlEncodedFormEntityBody(request.getFormParams(), Optional.ofNullable(request.getCharset()).map(Charset::name).orElse("utf-8"));
                }
                if (body != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", body.getContentTypeHeader().get());
                    try (OutputStream out = connection.getOutputStream()) {
                        body.writeTo(out);
                    }
                }
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    try (InputStream in = connection.getInputStream()) {
                        return new DefaultResponse(connection.getResponseCode(), connection.getResponseMessage(), StreamUtils.readAllBytes(in), request.getUri(), toHeaders(connection.getHeaderFields()));
                    }
                }
                try (InputStream in = connection.getErrorStream()) {
                    return new DefaultResponse(connection.getResponseCode(), connection.getResponseMessage(), in == null ? null : StreamUtils.readAllBytes(in), request.getUri(), toHeaders(connection.getHeaderFields()));
                }
            } catch (Exception e) {
                throw new HttpClientException(e.getMessage(), e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }, this.executor);
    }

    @Override
    public void close() throws IOException {
        if (this.executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
    }

    private static URL toUrl(@Nonnull Request request) throws Exception {
        if (CollectionUtils.isEmpty(request.getQueryParams())) {
            return request.getUri().toURL();
        }
        final String url = request.getUri().toString();
        if (url.contains("?")) {
            if (url.endsWith("&")) {
                return new URL(url + RequestParams.format(request.getQueryParams(), Optional.ofNullable(request.getCharset()).map(Charset::name).orElse("utf-8")));
            }
            return new URL(url + "&" + RequestParams.format(request.getQueryParams(), Optional.ofNullable(request.getCharset()).map(Charset::name).orElse("utf-8")));
        }
        return new URL(url + "?" + RequestParams.format(request.getQueryParams(), Optional.ofNullable(request.getCharset()).map(Charset::name).orElse("utf-8")));
    }

    private HttpHeaders toHeaders(final Map<String, List<String>> map) {
        return new DefaultHttpHeaders(
                map.entrySet().stream().filter(item -> item.getKey() != null).map(entry -> new DefaultHeader(entry.getKey(), entry.getValue())).collect(Collectors.toList())
                        .stream().collect(Collectors.toMap(DefaultHeader::getName, Function.identity()))
        );
    }

    private static class DefaultResponse implements Response {
        private final int statusCode;
        private final String statusText;
        private final byte[] body;
        private final URI uri;
        private final HttpHeaders headers;

        private DefaultResponse(int statusCode, String statusText, byte[] body, URI uri, HttpHeaders headers) {
            this.statusCode = statusCode;
            this.statusText = statusText;
            this.body = body;
            this.uri = uri;
            this.headers = headers;
        }

        @Override
        public int getStatusCode() {
            return this.statusCode;
        }

        @Override
        public String getStatusText() {
            return this.statusText;
        }

        @Override
        public String getBody(@Nonnull Charset charset) {
            if (this.body == null) {
                return null;
            }
            return new String(this.body, charset);
        }

        @Override
        public InputStream getBodyAsStream() {
            if (this.body == null) {
                return null;
            }
            return new ByteArrayInputStream(this.body);
        }

        @Override
        public byte[] getBodyAsBytes() {
            if (this.body == null) {
                return null;
            }
            return Arrays.copyOf(this.body, this.body.length);
        }

        @Override
        public URI getUri() {
            return this.uri;
        }

        @Override
        public String getContentType() {
            return this.headers.getHeader("content-type").get();
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            return this.headers;
        }

        @Nonnull
        @Override
        public List<? extends Cookie> getCookies() {
            Header header = this.getHeaders().getHeader("Set-Cookie");
            final List<Cookie> cookies = new ArrayList<>();
            if (header != null && CollectionUtils.isNotEmpty(header.getValues())) {
                for (String value : header.getValues()) {
                    final Cookie cookie = Cookies.parse(value);
                    if (cookie != null) {
                        cookies.add(cookie);
                    }
                }
            }
            header = this.getHeaders().getHeader("set-cookie2");
            if (header != null && CollectionUtils.isNotEmpty(header.getValues())) {
                for (String value : header.getValues()) {
                    final Cookie cookie = Cookies.parse(value);
                    if (cookie != null) {
                        cookies.add(cookie);
                    }
                }
            }

            return cookies;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return null;
        }
    }
}
