package cn.maarlakes.common.http.encoder;

import cn.maarlakes.common.http.Header;
import jakarta.annotation.Nonnull;

import java.io.InputStream;

/**
 * 响应体编码解码器的 SPI 接口，用于根据 Content-Encoding 头自动解压 HTTP 响应体。
 *
 * <p>每个实现负责识别自己支持的编码格式（如 gzip、deflate、br），
 * 并将压缩的输入流转换为解压后的输入流。通过 SPI 机制加载，
 * 与 {@code HttpClient} 的响应解码流程集成。</p>
 *
 * @author linjpxc
 */
public interface ResponseBodyEncoder {

    /**
     * 判断是否支持给定的 Content-Encoding 头所指示的编码格式。
     *
     * @param contentEncoding 响应的 Content-Encoding 头
     * @return 如果本编码器能处理该编码则返回 {@code true}
     */
    boolean supported(@Nonnull Header contentEncoding);

    /**
     * 对响应体输入流执行解码（解压）操作，返回解压后的输入流。
     *
     * @param content 原始响应体输入流
     * @return 解码后的输入流
     */
    @Nonnull
    InputStream decoding(@Nonnull InputStream content);
}
