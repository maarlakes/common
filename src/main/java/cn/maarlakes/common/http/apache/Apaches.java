package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.NameValuePair;
import cn.maarlakes.common.http.Request;
import cn.maarlakes.common.utils.CollectionUtils;
import jakarta.annotation.Nonnull;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author linjpxc
 */
final class Apaches {

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

    public static org.apache.hc.core5.http.ContentType toApacheContentType(@Nonnull ContentType contentType) {
        final org.apache.hc.core5.http.ContentType result = org.apache.hc.core5.http.ContentType.create(contentType.getMediaType(), contentType.getCharset());
        if (CollectionUtils.isNotEmpty(contentType.getParameters())) {
            return result.withParameters(
                    contentType.getParameters().stream().map(item -> new BasicNameValuePair(item.getName(), item.getValue()))
                            .toArray(org.apache.hc.core5.http.NameValuePair[]::new)
            );
        }
        return result;
    }
}
