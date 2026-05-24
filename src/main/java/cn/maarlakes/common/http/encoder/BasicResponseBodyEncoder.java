package cn.maarlakes.common.http.encoder;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * {@link ResponseBodyEncoder} 的抽象基类，提供统一的异常处理和空值防御逻辑。
 *
 * <p>子类只需实现 {@link #decode} 和 {@link #supported} 两个方法即可完成一种编码格式的支持。
 * 本类将 {@link #decoding} 方法标记为 final，确保所有子类的异常处理行为一致：
 * {@code IOException} 转为 {@code UncheckedIOException}，其他异常转为 {@code IllegalStateException}。</p>
 *
 * @author linjpxc
 */
abstract class BasicResponseBodyEncoder implements ResponseBodyEncoder {

    /**
     * 执行解码流程：调用子类的 {@link #decode} 方法，若返回 null 则原样返回输入流。
     * 解码过程中的异常会被统一转换为运行时异常。
     */
    @Nonnull
    @Override
    public final InputStream decoding(@Nonnull InputStream content) {
        try {
            final InputStream decode = this.decode(content);
            return decode == null ? content : decode;
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw new UncheckedIOException(e.getMessage(), (IOException) e);
            }
            throw new IllegalStateException("Failed to decompress the response content.", e);
        }
    }

    /**
     * 子类实现此方法以提供具体的解码（解压）逻辑。
     *
     * @param content 待解码的输入流
     * @return 解码后的输入流，若无需解码可返回 {@code null}
     * @throws Exception 解码过程中可能抛出的异常
     */
    protected abstract InputStream decode(@Nonnull InputStream content) throws Exception;
}
