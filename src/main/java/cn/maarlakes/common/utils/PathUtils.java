package cn.maarlakes.common.utils;


/**
 * @author linjpxc
 */
public final class PathUtils {
    private PathUtils() {
    }

    public static String combine(String... paths) {
        return combineWith("/", paths);
    }

    public static String combineWith(String delimiter, String... paths) {
        if (paths.length < 1) {
            return "";
        }
        int startIndex = 0;
        while (startIndex < paths.length && paths[startIndex] == null) {
            startIndex++;
        }
        if (startIndex >= paths.length) {
            return "";
        }
        final StringBuilder builder = new StringBuilder(paths[startIndex]);
        final int delimiterLength = delimiter.length();
        for (int i = startIndex + 1; i < paths.length; i++) {
            final String path = paths[i];
            if (path == null) {
                continue;
            }
            if (builder.lastIndexOf(delimiter) == builder.length() - delimiterLength) {
                if (path.startsWith(delimiter)) {
                    builder.append(path.substring(delimiterLength));
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
