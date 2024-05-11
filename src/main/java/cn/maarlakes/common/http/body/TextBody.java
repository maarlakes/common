package cn.maarlakes.common.http.body;

import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.ContentTypes;
import cn.maarlakes.common.http.RequestBody;
import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface TextBody extends RequestBody<CharSequence> {

    @Nonnull
    default String getCharset() {
        final ContentType contentType = this.getContentType();
        if (contentType != null) {
            return ContentTypes.getCharset(contentType);
        }
        return "utf-8";
    }
}
