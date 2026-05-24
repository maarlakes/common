package cn.maarlakes.common.http.encoder;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.Header;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * gzip 编码的响应体解码器，通过 {@link GZIPInputStream} 解压 gzip 格式的响应体。
 *
 * <p>作为 SPI 单例服务注册，在 {@code HttpClient} 处理响应时自动识别
 * Content-Encoding 为 "gzip" 的响应并执行解压。</p>
 *
 * @author linjpxc
 */
@Order(Integer.MAX_VALUE)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class GzipResponseBodyEncoder extends BasicResponseBodyEncoder {

    @Override
    public boolean supported(@Nonnull Header contentEncoding) {
        return contentEncoding.getValues().stream().anyMatch("gzip"::equalsIgnoreCase);
    }

    @Override
    protected InputStream decode(@Nonnull InputStream content) throws Exception {
        return new GZIPInputStream(content);
    }
}
