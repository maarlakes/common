package cn.maarlakes.common.http.body;

import cn.maarlakes.common.function.Consumer3;
import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.Header;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * HTTP 消息体的统一抽象，定义内容类型、流式读写和长度查询等核心契约。
 *
 * <p>作为 body 包的基础接口，{@code ContentBody} 被 {@link TextBody}、{@link FormBody} 等
 * 具体请求体类型所继承/实现，同时也作为 {@code MultipartPart} 的父接口服务于 multipart 场景。
 * 通过 {@link Consumer3} 重载的 {@code writeTo} 方法支持零拷贝式的分段写入。</p>
 *
 * @param <T> 消息体内容的原始类型（如 {@code CharSequence}、{@code Collection} 等）
 * @author linjpxc
 */
public interface ContentBody<T> {

    /**
     * 返回消息体的 Content-Type，无类型信息时返回 {@code null}。
     */
    @Nullable
    ContentType getContentType();

    /**
     * 以输入流形式返回消息体内容，调用方负责关闭流。
     */
    InputStream getContentStream();

    /**
     * 返回消息体的原始内容对象。
     */
    T getContent();

    /**
     * 返回消息体的字节长度，未知时返回 {@code -1}。
     */
    int getContentLength();

    /**
     * 将 Content-Type 转换为 HTTP Header 形式，便于直接添加到请求头中。
     * 当 {@link #getContentType()} 为 {@code null} 时返回 {@code null}。
     */
    @Nullable
    default Header getContentTypeHeader() {
        final ContentType ct = this.getContentType();
        return ct == null ? null : ct.toHeader();
    }

    /**
     * 将消息体内容写入输出流。
     */
    default void writeTo(@Nonnull OutputStream stream) {
        this.writeTo(stream::write);
    }

    /**
     * 以分段回调方式写入消息体内容，接收 {@code byte[], offset, length} 三元组。
     * 适用于底层 HTTP 客户端需要逐块写入的场景，避免一次性分配完整字节数组。
     */
    void writeTo(@Nonnull Consumer3<byte[], Integer, Integer> consumer);
}
