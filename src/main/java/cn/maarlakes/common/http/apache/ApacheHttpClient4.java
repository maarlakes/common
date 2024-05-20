package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.factory.datetime.DateTimeFactories;
import cn.maarlakes.common.http.*;
import cn.maarlakes.common.http.body.multipart.FilePart;
import cn.maarlakes.common.http.body.multipart.MultipartBody;
import cn.maarlakes.common.http.body.multipart.MultipartPart;
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
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

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
                settingHeader(builder, request);
                if (request.getCharset() != null) {
                    builder.setCharset(request.getCharset());
                }
                settingFormParams(builder, request);
                if (request.getBody() != null) {
                    if (request.getBody() instanceof MultipartBody) {
                        settingMultipart(builder, (MultipartBody) request.getBody(), request.getCharset());
                    } else {
                        final InputStream content = request.getBody().getContentStream();
                        if (content != null) {
                            builder.setEntity(new InputStreamEntity(content, toApacheContentType(request.getBody().getContentType())));
                        }
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

    @Nonnull
    private static URI toUri(@Nonnull Request request) throws URISyntaxException {
        final URIBuilder builder = new URIBuilder(request.getUri());
        if (CollectionUtils.isNotEmpty(request.getQueryParams())) {
            for (NameValuePair param : request.getQueryParams()) {
                builder.addParameter(param.getName(), param.getValue());
            }
        }
        return builder.build();
    }

    @SuppressWarnings("DuplicatedCode")
    private static void settingMultipart(@Nonnull RequestBuilder builder, @Nonnull MultipartBody body, Charset charset) {
        if (CollectionUtils.isNotEmpty(body.getContent())) {
            final MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            multipartEntityBuilder.setContentType(org.apache.http.entity.ContentType.parse(ContentTypes.toString(body.getContentType())));
            for (MultipartPart<?> part : body.getContent()) {
                ContentBody contentBody;
                if (part instanceof FilePart) {
                    if (part.getContentType() == null) {
                        contentBody = new FileBody(((FilePart) part).getContent());
                    } else {
                        contentBody = new FileBody(((FilePart) part).getContent(), toApacheContentType(part.getContentType()), part.getFilename());
                    }
                } else {
                    if (part.getContentType() == null) {
                        contentBody = new InputStreamBody(part.getContentStream(), part.getFilename());
                    } else {
                        contentBody = new InputStreamBody(part.getContentStream(), toApacheContentType(part.getContentType()), part.getFilename());
                    }
                }
                final FormBodyPartBuilder partBuilder = FormBodyPartBuilder.create(part.getName(), contentBody);
                if (!part.getHeaders().isEmpty()) {
                    for (Header header : part.getHeaders()) {
                        for (String s : header.getValues()) {
                            if (s != null) {
                                partBuilder.addField(header.getName(), s);
                            }
                        }
                    }
                }
                multipartEntityBuilder.addPart(partBuilder.build());
            }
            if (charset != null) {
                multipartEntityBuilder.setCharset(charset);
            }
            builder.setEntity(multipartEntityBuilder.build());
        }
    }

    private static void settingFormParams(@Nonnull RequestBuilder builder, @Nonnull Request request) {
        if (CollectionUtils.isNotEmpty(request.getFormParams())) {
            for (NameValuePair param : request.getFormParams()) {
                builder.addParameter(param.getName(), param.getValue());
            }
        }
    }

    private static void settingHeader(@Nonnull RequestBuilder builder, @Nonnull Request request) {
        if (!request.getHeaders().isEmpty()) {
            for (Header header : request.getHeaders()) {
                for (String value : header.getValues()) {
                    builder.addHeader(header.getName(), value);
                }
            }
        }
    }

    private static class DefaultResponse implements Response {
        private final URI uri;
        private final HttpResponse response;
        private final HttpContext context;
        private final SocketAddress remoteAddress;
        private final ResponseBody body;

        private DefaultResponse(URI uri, HttpResponse response, HttpContext context) {
            this.uri = uri;
            this.response = response;
            final Object attribute = context.getAttribute("http.connection");
            if (attribute instanceof HttpInetConnection) {
                final HttpInetConnection connection = (HttpInetConnection) attribute;
                remoteAddress = new InetSocketAddress(connection.getRemoteAddress(), connection.getRemotePort());
            } else {
                remoteAddress = null;
            }
            final HttpEntity entity = response.getEntity();
            byte[] buffer;
            if (entity == null) {
                buffer = new byte[0];
            } else {
                try {
                    buffer = EntityUtils.toByteArray(entity);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            this.body = new ByteArrayResponseBody(buffer, Optional.ofNullable(response.getEntity()).map(HttpEntity::getContentType).map(Object::toString).map(ContentType::parse).orElse(null));
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

        @Nonnull
        @Override
        public ResponseBody getBody() {
            return this.body;
        }

        @Override
        public URI getUri() {
            return this.uri;
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
