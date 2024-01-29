package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author linjpxc
 */
public final class StreamUtils {
    private StreamUtils() {
    }

    private static final int BUFFER_SIZE = 8192;

    @Nonnull
    public static String readAllText(@Nonnull InputStream stream) throws IOException {
        return readAllText(stream, StandardCharsets.UTF_8);
    }

    @Nonnull
    public static String readAllText(@Nonnull InputStream stream, @Nonnull Charset charset) throws IOException {
        final StringBuilder builder = new StringBuilder();
        final InputStreamReader reader = new InputStreamReader(stream, charset);
        char[] buffer = new char[BUFFER_SIZE];
        int charsRead;
        while ((charsRead = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, charsRead);
        }
        return builder.toString();
    }
}
