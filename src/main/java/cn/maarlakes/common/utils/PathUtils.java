package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public final class PathUtils {
    private PathUtils() {
    }

    @Nonnull
    public static String combine(@Nonnull String... paths) {
        return combineWith("/", paths);
    }

    @Nonnull
    public static String combineWith(@Nonnull String delimiter, @Nonnull String... paths) {
        final StringBuilder builder = new StringBuilder();
        final int length = delimiter.length();
        for (String path : paths) {
            if (builder.lastIndexOf(delimiter) == builder.length() - length) {
                if (path.startsWith(delimiter)) {
                    builder.append(path.substring(length));
                } else {
                    builder.append(path);
                }
            } else {
                if (path.startsWith(delimiter)) {
                    builder.append(path);
                } else {
                    builder.append(delimiter).append(path);
                }
            }
        }
        return builder.toString();
    }
}
