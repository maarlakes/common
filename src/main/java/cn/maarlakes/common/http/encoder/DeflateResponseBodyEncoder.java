package cn.maarlakes.common.http.encoder;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.Header;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.util.zip.InflaterInputStream;

/**
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
