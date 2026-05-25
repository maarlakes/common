package cn.maarlakes.common.utils;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author linjpxc
 */
public final class VersionRange {
    private final List<Range> ranges;

    private VersionRange(List<Range> ranges) {
        this.ranges = Collections.unmodifiableList(new ArrayList<>(ranges));
    }

    public boolean contains(Version version) {
        for (Range range : this.ranges) {
            if (range.contains(version)) {
                return true;
            }
        }
        return false;
    }

    public static VersionRange parse(String expression) {
        if (expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Expression must not be empty.");
        }

        String[] orParts = expression.split("\\|\\|");
        List<Range> ranges = new ArrayList<>();
        for (String orPart : orParts) {
            ranges.add(parseRange(orPart.trim()));
        }
        return new VersionRange(ranges);
    }

    private static Range parseRange(String rangeStr) {
        String[] constraintParts = rangeStr.split("\\s+");
        List<Constraint> constraints = new ArrayList<>();
        for (String part : constraintParts) {
            if (part.isEmpty()) {
                continue;
            }
            constraints.add(parseConstraint(part));
        }
        return new Range(constraints);
    }

    private static Constraint parseConstraint(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Empty constraint.");
        }

        if (isWildcard(trimmed)) {
            return parseWildcardConstraint(trimmed);
        }

        String op = "";
        String versionStr = trimmed;

        if (trimmed.startsWith(">=")) {
            op = ">=";
            versionStr = trimmed.substring(2).trim();
        } else if (trimmed.startsWith("<=")) {
            op = "<=";
            versionStr = trimmed.substring(2).trim();
        } else if (trimmed.startsWith(">")) {
            op = ">";
            versionStr = trimmed.substring(1).trim();
        } else if (trimmed.startsWith("<")) {
            op = "<";
            versionStr = trimmed.substring(1).trim();
        } else if (trimmed.startsWith("=")) {
            op = "=";
            versionStr = trimmed.substring(1).trim();
        } else if (trimmed.startsWith("^")) {
            op = "^";
            versionStr = trimmed.substring(1).trim();
        } else if (trimmed.startsWith("~")) {
            op = "~";
            versionStr = trimmed.substring(1).trim();
        }

        Version version = Version.parse(versionStr);

        if (">=".equals(op)) {
            return new GreaterThanConstraint(version, true);
        } else if ("<=".equals(op)) {
            return new LessThanConstraint(version, true);
        } else if (">".equals(op)) {
            return new GreaterThanConstraint(version, false);
        } else if ("<".equals(op)) {
            return new LessThanConstraint(version, false);
        } else if ("=".equals(op) || op.isEmpty()) {
            return new EqualsConstraint(version);
        } else if ("^".equals(op)) {
            return new CaretConstraint(version);
        } else if ("~".equals(op)) {
            return new TildeConstraint(version);
        }

        throw new IllegalArgumentException("Unknown constraint: " + text);
    }

    private static boolean isWildcard(String text) {
        return text.contains("*") || text.contains("x") || text.contains("X");
    }

    private static Constraint parseWildcardConstraint(String text) {
        String[] parts = text.split("\\.");
        if (parts.length == 1 && isWildcardPart(parts[0])) {
            return version -> true;
        } else if (parts.length == 2) {
            int major = Integer.parseInt(parts[0]);
            if (isWildcardPart(parts[1])) {
                return new AndConstraint(
                        new GreaterThanConstraint(Version.of(major, 0, 0), true),
                        new LessThanConstraint(Version.of(major + 1, 0, 0), false)
                );
            }
        } else if (parts.length == 3) {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            if (isWildcardPart(parts[2])) {
                return new AndConstraint(
                        new GreaterThanConstraint(Version.of(major, minor, 0), true),
                        new LessThanConstraint(Version.of(major, minor + 1, 0), false)
                );
            }
        }
        throw new IllegalArgumentException("Invalid wildcard version: " + text);
    }

    private static boolean isWildcardPart(String part) {
        return "*".equals(part) || "x".equals(part) || "X".equals(part);
    }

    private static final class Range {
        private final List<Constraint> constraints;

        Range(List<Constraint> constraints) {
            this.constraints = constraints;
        }

        boolean contains(Version version) {
            for (Constraint constraint : this.constraints) {
                if (!constraint.test(version)) {
                    return false;
                }
            }
            return true;
        }
    }

    private interface Constraint {
        boolean test(Version version);
    }

    private static final class EqualsConstraint implements Constraint {
        private final Version target;

        EqualsConstraint(Version target) {
            this.target = target;
        }

        @Override
        public boolean test(Version version) {
            return this.target.equals(version);
        }
    }

    private static final class GreaterThanConstraint implements Constraint {
        private final Version target;
        private final boolean orEqual;

        GreaterThanConstraint(Version target, boolean orEqual) {
            this.target = target;
            this.orEqual = orEqual;
        }

        @Override
        public boolean test(Version version) {
            int cmp = version.compareTo(this.target);
            return this.orEqual ? cmp >= 0 : cmp > 0;
        }
    }

    private static final class LessThanConstraint implements Constraint {
        private final Version target;
        private final boolean orEqual;

        LessThanConstraint(Version target, boolean orEqual) {
            this.target = target;
            this.orEqual = orEqual;
        }

        @Override
        public boolean test(Version version) {
            int cmp = version.compareTo(this.target);
            return this.orEqual ? cmp <= 0 : cmp < 0;
        }
    }

    private static final class AndConstraint implements Constraint {
        private final Constraint[] constraints;

        AndConstraint(Constraint... constraints) {
            this.constraints = constraints;
        }

        @Override
        public boolean test(Version version) {
            for (Constraint constraint : this.constraints) {
                if (!constraint.test(version)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class CaretConstraint implements Constraint {
        private final Version target;

        CaretConstraint(Version target) {
            this.target = target;
        }

        @Override
        public boolean test(Version version) {
            int cmp = version.compareTo(this.target);
            if (cmp < 0) {
                return false;
            }

            Version upperBound;
            if (this.target.major() != 0) {
                upperBound = Version.of(this.target.major() + 1, 0, 0);
            } else if (this.target.minor() != 0) {
                upperBound = Version.of(0, this.target.minor() + 1, 0);
            } else {
                upperBound = Version.of(0, 0, this.target.patch() + 1);
            }
            return version.compareTo(upperBound) < 0;
        }
    }

    private static final class TildeConstraint implements Constraint {
        private final Version target;

        TildeConstraint(Version target) {
            this.target = target;
        }

        @Override
        public boolean test(Version version) {
            int cmp = version.compareTo(this.target);
            if (cmp < 0) {
                return false;
            }

            Version upperBound = Version.of(this.target.major(), this.target.minor() + 1, 0);
            return version.compareTo(upperBound) < 0;
        }
    }
}
