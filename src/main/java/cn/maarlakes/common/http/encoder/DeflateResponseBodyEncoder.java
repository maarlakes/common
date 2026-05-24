package cn.maarlakes.common.http.encoder;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.Header;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.util.zip.InflaterInputStream;

/**
 * deflate 编码的响应体解码器，通过 {@link InflaterInputStream} 解压 deflate 格式的响应体。
 *
 * <p>作为 SPI 单例服务注册，在 {@code HttpClient} 处理响应时自动识别
 * Content-Encoding 为 "deflate" 的响应并执行解压。</p>
 *
 * @author linjpxc
 */
@Order(Integer.MAX_VALUE)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class DeflateResponseBodyEncoder extends BasicResponseBodyEncoder {
    @Override
    protected InputStream decode(@Nonnull InputStream content) throws Exception {
        return new InflaterInputStream(content);
    }

    @Override
    public boolean supported(@Nonnull Header contentEncoding) {
        return contentEncoding.getValues().stream().anyMatch("deflate"::equalsIgnoreCase);
    }
}
