package cn.maarlakes.common.http.encoder;

import cn.maarlakes.common.http.Header;
import jakarta.annotation.Nonnull;

import java.io.InputStream;

/**
 * @author linjpxc
 */
public interface ResponseBodyEncoder {

    boolean supported(@Nonnull Header contentEncoding);

    @Nonnull
    InputStream decoding(@Nonnull InputStream content);
}
