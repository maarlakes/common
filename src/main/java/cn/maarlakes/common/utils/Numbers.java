package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * @author linjpxc
 */
public final class Numbers {
    private Numbers() {
    }

    public static byte convertToByte(Object value) {
        return convertToByte(value, 10);
    }

    public static byte convertToByte(Object value, int radix) {
        return convertToByteOptional(value, radix).orElse((byte) 0);
    }

    @Nonnull
    public static Optional<Byte> convertToByteOptional(Object value) {
        return convertToByteOptional(value, 10);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static Optional<Byte> convertToByteOptional(Object value, int radix) {
        if (value instanceof Optional) {
            return convertToByteOptional(((Optional<Object>) value).orElse(null), radix);
        }
        if (value instanceof Number) {
            return Optional.of(((Number) value).byteValue());
        }
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Byte.parseByte(value.toString(), radix));
    }

    public static short convertToShort(Object value) {
        return convertToShort(value, 10);
    }

    public static short convertToShort(Object value, int radix) {
        return convertToShortOptional(value, radix).orElse((short) 0);
    }

    @Nonnull
    public static Optional<Short> convertToShortOptional(Object value) {
        return convertToShortOptional(value, 10);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static Optional<Short> convertToShortOptional(Object value, int radix) {
        if (value instanceof Optional) {
            return convertToShortOptional(((Optional<Object>) value).orElse(null), radix);
        }
        if (value instanceof Number) {
            return Optional.of(((Number) value).shortValue());
        }
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Short.parseShort(value.toString(), radix));
    }

    public static int convertToInteger(Object value) {
        return convertToInteger(value, 10);
    }

    public static int convertToInteger(Object value, int radix) {
        return convertToIntegerOptional(value, radix).orElse(0);
    }

    @Nonnull
    public static Optional<Integer> convertToIntegerOptional(Object value) {
        return convertToIntegerOptional(value, 10);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static Optional<Integer> convertToIntegerOptional(Object value, int radix) {
        if (value instanceof Optional) {
            return convertToIntegerOptional(((Optional<Object>) value).orElse(null), radix);
        }
        if (value instanceof Number) {
            return Optional.of(((Number) value).intValue());
        }
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(value.toString(), radix));
    }

    public static long convertToLong(Object value) {
        return convertToLong(value, 10);
    }

    public static long convertToLong(Object value, int radix) {
        return convertToLongOptional(value, radix).orElse(0L);
    }

    @Nonnull
    public static Optional<Long> convertToLongOptional(Object value) {
        return convertToLongOptional(value, 10);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static Optional<Long> convertToLongOptional(Object value, int radix) {
        if (value instanceof Optional) {
            return convertToLongOptional(((Optional<Object>) value).orElse(null), radix);
        }
        if (value instanceof Number) {
            return Optional.of(((Number) value).longValue());
        }
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(value.toString(), radix));
    }

    public static float convertToFloat(Object value) {
        return convertToFloatOptional(value).orElse(0F);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static Optional<Float> convertToFloatOptional(Object value) {
        if (value instanceof Optional) {
            return convertToFloatOptional(((Optional<Object>) value).orElse(null));
        }
        if (value instanceof Number) {
            return Optional.of(((Number) value).floatValue());
        }
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Float.parseFloat(value.toString()));
    }

    public static double convertToDouble(Object value) {
        return convertToDoubleOptional(value).orElse(0D);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static Optional<Double> convertToDoubleOptional(Object value) {
        if (value instanceof Optional) {
            return convertToDoubleOptional(((Optional<Object>) value).orElse(null));
        }
        if (value instanceof Number) {
            return Optional.of(((Number) value).doubleValue());
        }
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Double.parseDouble(value.toString()));
    }

    public static BigDecimal convertToBigDecimal(Object value) {
        return convertToBigDecimalOptional(value).orElse(BigDecimal.ZERO);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static Optional<BigDecimal> convertToBigDecimalOptional(Object value) {
        if (value instanceof Optional) {
            return convertToBigDecimalOptional(((Optional<Object>) value).orElse(null));
        }
        if (value instanceof Double || value instanceof Float) {
            return Optional.of(BigDecimal.valueOf(((Number) value).doubleValue()));
        }

        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(value.toString()));
    }
}
