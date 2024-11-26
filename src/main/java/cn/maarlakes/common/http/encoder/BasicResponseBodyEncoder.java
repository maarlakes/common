package cn.maarlakes.common.http.encoder;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * @author linjpxc
 */
abstract class BasicResponseBodyEncoder implements ResponseBodyEncoder {

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

    protected abstract InputStream decode(@Nonnull InputStream content) throws Exception;
}
