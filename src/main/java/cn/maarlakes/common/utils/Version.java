package cn.maarlakes.common.utils;

import com.alibaba.fastjson2.annotation.JSONType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * 语义化版本（Semantic Versioning）值对象，遵循 SemVer 2.0.0 规范。
 *
 * <p>支持 1~3 段式版本号解析，缺失段自动补 0，例如 {@code "1.0"} 会被解析为 {@code 1.0.0}。
 * 版本格式为 {@code MAJOR[.MINOR[.PATCH]][-prerelease][+buildMetadata]}，其中：</p>
 * <ul>
 *   <li>{@code MAJOR}、{@code MINOR}、{@code PATCH} 为非负整数</li>
 *   <li>{@code prerelease} 为可选的预发布版本标识，由点分隔的标识符组成</li>
 *   <li>{@code buildMetadata} 为可选的构建元数据，不参与版本比较和 {@code equals}</li>
 * </ul>
 *
 * <p>本类为不可变对象，实现了 {@link Comparable} 接口，比较规则严格遵循 SemVer 规范：</p>
 * <ol>
 *   <li>先比较 {@code major}、{@code minor}、{@code patch} 数字大小</li>
 *   <li>有 {@code preRelease} 的版本小于无 {@code preRelease} 的同名版本</li>
 *   <li>{@code preRelease} 标识符按数值或字典序逐个比较</li>
 *   <li>{@code buildMetadata} 不参与比较</li>
 * </ol>
 *
 * @author linjpxc
 * @see VersionRange
 */
@JSONType(serializer = VersionObjectWriter.class, deserializer = VersionObjectReader.class)
public final class Version implements Comparable<Version>, Serializable {
    private static final long serialVersionUID = 8793409821736409821L;

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;
    private final String buildMetadata;

    private Version(int major, int minor, int patch,  String preRelease,  String buildMetadata) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = (preRelease == null || preRelease.isEmpty()) ? null : preRelease;
        this.buildMetadata = (buildMetadata == null || buildMetadata.isEmpty()) ? null : buildMetadata;
    }

    /**
     * 返回主版本号（{@code MAJOR}）。
     *
     * @return 主版本号，非负整数
     */
    public int major() {
        return this.major;
    }

    /**
     * 返回次版本号（{@code MINOR}）。
     *
     * @return 次版本号，非负整数
     */
    public int minor() {
        return this.minor;
    }

    /**
     * 返回修订版本号（{@code PATCH}）。
     *
     * @return 修订版本号，非负整数
     */
    public int patch() {
        return this.patch;
    }

    /**
     * 返回预发布版本标识（{@code prerelease}）。
     *
     * @return 预发布版本标识；如果没有则返回 {@code null}
     */
    @Nullable
    public String preRelease() {
        return this.preRelease;
    }

    /**
     * 返回构建元数据（{@code buildMetadata}）。
     *
     * <p>构建元数据仅作为信息展示，不参与版本比较、{@code equals} 和 {@code hashCode}。</p>
     *
     * @return 构建元数据；如果没有则返回 {@code null}
     */
    @Nullable
    public String buildMetadata() {
        return this.buildMetadata;
    }

    /**
     * 判断当前版本是否为预发布版本。
     *
     * @return 如果包含 {@code preRelease} 则返回 {@code true}
     */
    public boolean isPreRelease() {
        return this.preRelease != null;
    }

    /**
     * 判断当前版本是否包含构建元数据。
     *
     * @return 如果包含 {@code buildMetadata} 则返回 {@code true}
     */
    public boolean hasBuildMetadata() {
        return this.buildMetadata != null;
    }

    /**
     * 创建由主版本号、次版本号和修订版本号组成的 {@link Version} 实例。
     *
     * @param major 主版本号，必须为非负整数
     * @param minor 次版本号，必须为非负整数
     * @param patch 修订版本号，必须为非负整数
     * @return 版本实例
     * @throws IllegalArgumentException 如果任意版本号为负数
     */
    public static Version of(int major, int minor, int patch) {
        return of(major, minor, patch, null, null);
    }

    /**
     * 创建包含预发布版本标识的 {@link Version} 实例。
     *
     * @param major      主版本号，必须为非负整数
     * @param minor      次版本号，必须为非负整数
     * @param patch      修订版本号，必须为非负整数
     * @param preRelease 预发布版本标识；若为 {@code null} 或空字符串则视为无预发布版本
     * @return 版本实例
     * @throws IllegalArgumentException 如果任意版本号为负数
     */
    public static Version of(int major, int minor, int patch, @Nullable String preRelease) {
        return of(major, minor, patch, preRelease, null);
    }

    /**
     * 创建完整的 {@link Version} 实例。
     *
     * @param major         主版本号，必须为非负整数
     * @param minor         次版本号，必须为非负整数
     * @param patch         修订版本号，必须为非负整数
     * @param preRelease    预发布版本标识；若为 {@code null} 或空字符串则视为无预发布版本
     * @param buildMetadata 构建元数据；若为 {@code null} 或空字符串则视为无构建元数据
     * @return 版本实例
     * @throws IllegalArgumentException 如果任意版本号为负数
     */
    public static Version of(int major, int minor, int patch, @Nullable String preRelease, @Nullable String buildMetadata) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version components must be non-negative.");
        }
        return new Version(major, minor, patch, preRelease, buildMetadata);
    }

    /**
     * 从字符串解析 {@link Version} 实例。
     *
     * <p>支持的格式：</p>
     * <ul>
     *   <li>{@code MAJOR[.MINOR[.PATCH]]}：支持 1~3 段式版本号，缺失段自动补 0</li>
     *   <li>{@code ...-prerelease}：可选的预发布版本标识</li>
     *   <li>{@code ...+buildMetadata}：可选的构建元数据</li>
     * </ul>
     *
     * <p>示例：</p>
     * <pre>
     * Version.parse("1");              // 1.0.0
     * Version.parse("1.0");            // 1.0.0
     * Version.parse("1.2.3");          // 1.2.3
     * Version.parse("1.0.0-beta");     // 1.0.0-beta
     * Version.parse("1.0.0+20130313"); // 1.0.0+20130313
     * </pre>
     *
     * @param text 待解析的版本字符串
     * @return 解析后的版本实例
     * @throws IllegalArgumentException 如果字符串为空或格式非法
     */
    @JsonCreator
    public static Version parse(CharSequence text) {
        if (text == null) {
            throw new IllegalArgumentException("Version string must not be null.");
        }
        String value = text.toString().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Version string must not be empty.");
        }

        String buildMetadata = null;
        int plusIndex = value.indexOf('+');
        if (plusIndex >= 0) {
            buildMetadata = value.substring(plusIndex + 1);
            if (buildMetadata.isEmpty()) {
                throw new IllegalArgumentException("Build metadata must not be empty: " + text);
            }
            value = value.substring(0, plusIndex);
        }

        String preRelease = null;
        int dashIndex = value.indexOf('-');
        if (dashIndex >= 0) {
            preRelease = value.substring(dashIndex + 1);
            if (preRelease.isEmpty()) {
                throw new IllegalArgumentException("Pre-release version must not be empty: " + text);
            }
            value = value.substring(0, dashIndex);
        }

        String[] parts = value.split("\\.");

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return of(major, minor, patch, preRelease, buildMetadata);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version format: " + text, e);
        }
    }

    /**
     * 按照 SemVer 2.0.0 规范比较两个版本的大小。
     *
     * <p>比较规则：</p>
     * <ol>
     *   <li>先依次比较 {@code major}、{@code minor}、{@code patch}</li>
     *   <li>有 {@code preRelease} 的版本小于无 {@code preRelease} 的版本</li>
     *   <li>{@code preRelease} 按标识符逐个比较：纯数字按数值比，非数字按 ASCII 字典序比</li>
     *   <li>{@code buildMetadata} 不参与比较</li>
     * </ol>
     *
     * @param o 待比较的版本；若为 {@code null} 则视为小于当前版本
     * @return 负数、零或正数，分别表示当前版本小于、等于或大于指定版本
     */
    @Override
    public int compareTo(@Nullable Version o) {
        if (o == null) {
            return 1;
        }
        int result = Integer.compare(this.major, o.major);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(this.minor, o.minor);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(this.patch, o.patch);
        if (result != 0) {
            return result;
        }

        if (this.preRelease == null && o.preRelease == null) {
            return 0;
        }
        if (this.preRelease == null) {
            return 1;
        }
        if (o.preRelease == null) {
            return -1;
        }

        String[] thisParts = this.preRelease.split("\\.");
        String[] otherParts = o.preRelease.split("\\.");
        int len = Math.min(thisParts.length, otherParts.length);
        for (int i = 0; i < len; i++) {
            String thisPart = thisParts[i];
            String otherPart = otherParts[i];
            boolean thisNumeric = isNumeric(thisPart);
            boolean otherNumeric = isNumeric(otherPart);

            if (thisNumeric && otherNumeric) {
                result = compareNumericIdentifiers(thisPart, otherPart);
            } else if (thisNumeric) {
                return -1;
            } else if (otherNumeric) {
                return 1;
            } else {
                result = thisPart.compareTo(otherPart);
            }
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(thisParts.length, otherParts.length);
    }

    private static boolean isNumeric(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return !s.isEmpty();
    }

    private static int compareNumericIdentifiers(String a, String b) {
        return Long.compare(Long.parseLong(a), Long.parseLong(b));
    }

    /**
     * 判断当前版本是否与指定对象相等。
     *
     * <p>相等判断基于 {@code major}、{@code minor}、{@code patch} 和 {@code preRelease}，
     * {@code buildMetadata} 不参与相等判断。</p>
     *
     * @param o 待比较的对象
     * @return 如果版本语义相等则返回 {@code true}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Version)) {
            return false;
        }
        Version other = (Version) o;
        return this.major == other.major
                && this.minor == other.minor
                && this.patch == other.patch
                && Objects.equals(this.preRelease, other.preRelease);
    }

    /**
     * 返回当前版本的哈希码。
     *
     * @return 哈希码
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.major, this.minor, this.patch, this.preRelease);
    }

    /**
     * 返回规范的版本字符串表示。
     *
     * <p>格式为 {@code MAJOR.MINOR.PATCH}，如果存在 {@code preRelease} 或 {@code buildMetadata} 则会追加。</p>
     *
     * @return 版本字符串，例如 {@code "1.2.3-beta+build"}
     */
    @Override
    @JsonValue
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.major).append('.').append(this.minor).append('.').append(this.patch);
        if (this.preRelease != null) {
            sb.append('-').append(this.preRelease);
        }
        if (this.buildMetadata != null) {
            sb.append('+').append(this.buildMetadata);
        }
        return sb.toString();
    }
}
