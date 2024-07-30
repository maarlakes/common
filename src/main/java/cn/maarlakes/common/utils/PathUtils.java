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
        if (paths.length < 1) {
            return "";
        }
        final StringBuilder builder = new StringBuilder(paths[0]);
        final int length = delimiter.length();
        for (int i = 1; i < paths.length; i++) {
            final String path = paths[i];
            if (builder.lastIndexOf(delimiter) == builder.length() - length) {
                if (path.startsWith(delimiter)) {
                    builder.append(path.substring(length));
                } else {
                    builder.append(path);
                }
            } else {
                if (!path.startsWith(delimiter)) {
                    builder.append(delimiter);
                }
                builder.append(path);
            }
        }
        return builder.toString();
    }
}
