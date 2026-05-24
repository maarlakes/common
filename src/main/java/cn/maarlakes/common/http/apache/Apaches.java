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
 * Apache HttpClient 5 的内部工具类，提供 URI 构建和 ContentType 转换方法。
 *
 * <p>将统一的 {@link Request} 和 {@link ContentType} 对象转换为
 * Apache HttpClient 5 原生的 {@link org.apache.hc.core5.net.URIBuilder} 和
 * {@link org.apache.hc.core5.http.ContentType}，供 Apache 后端适配器使用。</p>
 *
 * @author linjpxc
 */
final class Apaches {

    /**
     * 将统一的 Request 对象（含查询参数）转换为 Apache 的 URI。
     */
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

    /**
     * 将统一的 ContentType 转换为 Apache HttpClient 5 原生的 ContentType，
     * 保留媒体类型、字符集和附加参数。
     */
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
