package cn.maarlakes.common.http.encoder;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.Header;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
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
