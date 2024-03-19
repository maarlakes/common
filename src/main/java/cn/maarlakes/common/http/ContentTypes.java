package cn.maarlakes.common.http;

import cn.maarlakes.common.utils.CollectionUtils;
import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * @author linjpxc
 */
public final class ContentTypes {
    private ContentTypes() {
    }

   public static String contentTypeHeadValue(@Nonnull ContentType contentType) {
        final StringBuilder builder = new StringBuilder();
        boolean hasCharset = false;
        if (contentType.getCharset() != null) {
            hasCharset = true;
            builder.append("charset=").append(contentType.getCharset());
        }
        final Collection<NameValuePair> parameters = contentType.getParameters();
        if (CollectionUtils.isNotEmpty(parameters)) {
            for (NameValuePair parameter : parameters) {
                if (hasCharset && "charset".equalsIgnoreCase(parameter.getName())) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(";");
                }
                builder.append(parameter.getName()).append("=").append(parameter.getValue());
            }
        }
        return builder.toString();
    }
}
