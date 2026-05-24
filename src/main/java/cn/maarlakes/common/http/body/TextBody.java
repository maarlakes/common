package cn.maarlakes.common.http.body;

import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.ContentTypes;
import cn.maarlakes.common.http.RequestBody;
import jakarta.annotation.Nonnull;

/**
 * 文本类型的 HTTP 请求体，内容为 {@link CharSequence}。
 *
 * <p>扩展了 {@code RequestBody} 接口，专门用于纯文本和 JSON 等字符型消息体。
 * 默认从 Content-Type 中提取字符集，若未指定则回退到 UTF-8。
 * 主要实现类为 {@link DefaultTextBody} 和 {@link JsonBody}。</p>
 *
 * @author linjpxc
 */
public interface TextBody extends RequestBody<CharSequence> {

    /**
     * 返回消息体使用的字符集名称。
     * 优先从 Content-Type 中解析，无法解析时默认返回 {@code "utf-8"}。
     */
    @Nonnull
    default String getCharset() {
        final ContentType contentType = this.getContentType();
        if (contentType != null) {
            return ContentTypes.getCharset(contentType);
        }
        return "utf-8";
    }
}
