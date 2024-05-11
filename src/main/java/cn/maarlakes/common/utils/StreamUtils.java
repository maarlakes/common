package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.io.*;
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

    public static int copy(@Nonnull InputStream in, @Nonnull OutputStream out) throws IOException {
        int byteCount = 0;
        int bytesRead;
        final byte[] buffer = new byte[BUFFER_SIZE];
        while ((bytesRead = in.read(buffer)) >= 0) {
            out.write(buffer, 0, bytesRead);
            byteCount += bytesRead;
        }

        out.flush();
        return byteCount;
    }

    @Nonnull
    public static byte[] readAllBytes(@Nonnull InputStream stream) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            copy(stream, out);
            return out.toByteArray();
        }
    }
}
