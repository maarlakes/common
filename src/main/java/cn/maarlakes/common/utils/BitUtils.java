package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.nio.ByteOrder;
import java.util.Arrays;

public final class BitUtils {

    private BitUtils() {
    }

    public static final int BYTE_MAX_VALUE = 0xFF;

    private static final int BYTE_BITS = 8;

    private static final int ONE_BYTE_BITS = BYTE_BITS;

    private static final int TWO_BYTE_BITS = BYTE_BITS * 2;

    private static final int THREE_BYTE_BITS = BYTE_BITS * 3;

    private static final int FOUR_BYTE_BITS = BYTE_BITS * 4;

    private static final int FIVE_BYTE_BITS = BYTE_BITS * 5;

    private static final int SIX_BYTE_BITS = BYTE_BITS * 6;

    private static final int SEVEN_BYTE_BITS = BYTE_BITS * 7;

    public static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    public static byte[] getBytes(boolean value) {
        return value ? new byte[]{1} : new byte[]{0};
    }

    public static byte[] getBytes(short value) {
        if (LITTLE_ENDIAN) {
            return getLittleEndianBytes(value);
        }
        return getBigEndianBytes(value);
    }

    public static byte[] getBytes(char value) {
        return getBytes((short) value);
    }

    public static byte[] getBytes(int value) {
        if (LITTLE_ENDIAN) {
            return getLittleEndianBytes(value);
        }
        return getBigEndianBytes(value);
    }

    public static byte[] getBytes(long value) {
        if (LITTLE_ENDIAN) {
            return getLittleEndianBytes(value);
        }
        return getBigEndianBytes(value);
    }

    public static byte[] getBytes(float value) {
        return getBytes(Float.floatToRawIntBits(value));
    }

    public static byte[] getBytes(double value) {
        return getBytes(Double.doubleToRawLongBits(value));
    }

    public static byte[] getLittleEndianBytes(short value) {
        return new byte[]{
                (byte) value,
                (byte) (value >>> ONE_BYTE_BITS)
        };
    }

    public static byte[] getLittleEndianBytes(char value) {
        return getLittleEndianBytes((short) value);
    }

    public static byte[] getLittleEndianBytes(int value) {
        return new byte[]{
                (byte) value,
                (byte) (value >>> ONE_BYTE_BITS),
                (byte) (value >>> TWO_BYTE_BITS),
                (byte) (value >>> THREE_BYTE_BITS)
        };
    }

    public static byte[] getLittleEndianBytes(long value) {
        return new byte[]{
                (byte) value,
                (byte) (value >>> ONE_BYTE_BITS),
                (byte) (value >>> TWO_BYTE_BITS),
                (byte) (value >>> THREE_BYTE_BITS),
                (byte) (value >>> FOUR_BYTE_BITS),
                (byte) (value >>> FIVE_BYTE_BITS),
                (byte) (value >>> SIX_BYTE_BITS),
                (byte) (value >>> SEVEN_BYTE_BITS)
        };
    }

    public static byte[] getLittleEndianBytes(float value) {
        return getLittleEndianBytes(Float.floatToRawIntBits(value));
    }

    public static byte[] getLittleEndianBytes(double value) {
        return getLittleEndianBytes(Double.doubleToRawLongBits(value));
    }

    public static byte[] getBigEndianBytes(short value) {
        return new byte[]{
                (byte) (value >>> ONE_BYTE_BITS),
                (byte) value
        };
    }

    public static byte[] getBigEndianBytes(char value) {
        return getBigEndianBytes((short) value);
    }

    public static byte[] getBigEndianBytes(int value) {
        return new byte[]{
                (byte) (value >>> THREE_BYTE_BITS),
                (byte) (value >>> TWO_BYTE_BITS),
                (byte) (value >>> ONE_BYTE_BITS),
                (byte) value
        };
    }

    public static byte[] getBigEndianBytes(long value) {
        return new byte[]{
                (byte) (value >>> SEVEN_BYTE_BITS),
                (byte) (value >>> SIX_BYTE_BITS),
                (byte) (value >>> FIVE_BYTE_BITS),
                (byte) (value >>> FOUR_BYTE_BITS),
                (byte) (value >>> THREE_BYTE_BITS),
                (byte) (value >>> TWO_BYTE_BITS),
                (byte) (value >>> ONE_BYTE_BITS),
                (byte) value
        };
    }

    public static byte[] getBigEndianBytes(float value) {
        return getBigEndianBytes(Float.floatToRawIntBits(value));
    }

    public static byte[] getBigEndianBytes(double value) {
        return getBigEndianBytes(Double.doubleToRawLongBits(value));
    }

    public static short toShort(byte[] value, int startIndex) {
        if (LITTLE_ENDIAN) {
            return toLittleEndianShort(value, startIndex);
        }
        return toBigEndianShort(value, startIndex);
    }

    public static char toCharacter(byte[] value, int startIndex) {
        return (char) toShort(value, startIndex);
    }

    public static int toInteger(byte[] value, int startIndex) {
        if (LITTLE_ENDIAN) {
            return toLittleEndianInteger(value, startIndex);
        }
        return toBigEndianInteger(value, startIndex);
    }

    public static long toLong(byte[] value, int startIndex) {
        if (LITTLE_ENDIAN) {
            return toLittleEndianLong(value, startIndex);
        }
        return toBigEndianLong(value, startIndex);
    }

    public static float toFloat(byte[] value, int startIndex) {
        return Float.intBitsToFloat(toInteger(value, startIndex));
    }

    public static double toDouble(byte[] value, int startIndex) {
        return Double.longBitsToDouble(toLong(value, startIndex));
    }

    public static short toLittleEndianShort(byte[] value, int startIndex) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        return (short) (getRawByte(value[startIndex]) | (getRawByte(value[startIndex + 1]) << ONE_BYTE_BITS));
    }

    public static char toLittleEndianCharacter(byte[] value, int startIndex) {
        return (char) toLittleEndianShort(value, startIndex);
    }

    public static int toLittleEndianInteger(byte[] value, int startIndex) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        return getRawByte(value[startIndex])
                | (getRawByte(value[startIndex + 1]) << ONE_BYTE_BITS)
                | (getRawByte(value[startIndex + 2]) << TWO_BYTE_BITS)
                | (getRawByte(value[startIndex + 3]) << THREE_BYTE_BITS);

    }

    public static long toLittleEndianLong(byte[] value, int startIndex) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        int num = getRawByte(value[startIndex])
                | (getRawByte(value[startIndex + 1]) << ONE_BYTE_BITS)
                | (getRawByte(value[startIndex + 2]) << TWO_BYTE_BITS)
                | (getRawByte(value[startIndex + 3]) << THREE_BYTE_BITS);
        int num2 = getRawByte(value[startIndex + 4])
                | (getRawByte(value[startIndex + 5]) << ONE_BYTE_BITS)
                | (getRawByte(value[startIndex + 6]) << TWO_BYTE_BITS)
                | (getRawByte(value[startIndex + 7]) << THREE_BYTE_BITS);
        return ((long) num & 0xFFFFFFFFL) | ((long) num2 << FOUR_BYTE_BITS);

    }

    public static float toLittleEndianFloat(byte[] value, int startIndex) {
        return Float.intBitsToFloat(toLittleEndianInteger(value, startIndex));
    }

    public static double toLittleEndianDouble(byte[] value, int startIndex) {
        return Double.longBitsToDouble(toLittleEndianLong(value, startIndex));
    }

    public static short toBigEndianShort(byte[] value, int startIndex) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        return (short) ((getRawByte(value[startIndex]) << ONE_BYTE_BITS) | getRawByte(value[startIndex + 1]));
    }

    public static char toBigEndianCharacter(byte[] value, int startIndex) {
        return (char) toBigEndianShort(value, startIndex);
    }

    public static int toBigEndianInteger(byte[] value, int startIndex) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        return (getRawByte(value[startIndex]) << THREE_BYTE_BITS)
                | (getRawByte(value[startIndex + 1]) << TWO_BYTE_BITS)
                | (getRawByte(value[startIndex + 2]) << ONE_BYTE_BITS)
                | getRawByte(value[startIndex + 3]);
    }

    public static long toBigEndianLong(byte[] value, int startIndex) {
        if (value == null) {
            throw new NullPointerException("value");
        }

        int num3 = (getRawByte(value[startIndex]) << THREE_BYTE_BITS)
                | (getRawByte(value[startIndex + 1]) << TWO_BYTE_BITS)
                | (getRawByte(value[startIndex + 2]) << ONE_BYTE_BITS)
                | getRawByte(value[startIndex + 3]);
        int num4 = (getRawByte(value[startIndex + 4]) << THREE_BYTE_BITS)
                | (getRawByte(value[startIndex + 5]) << TWO_BYTE_BITS)
                | (getRawByte(value[startIndex + 6]) << ONE_BYTE_BITS)
                | getRawByte(value[startIndex + 7]);

        return ((long) num4 & 0xFFFFFFFFL) | ((long) num3 << FOUR_BYTE_BITS);
    }

    public static float toBigEndianFloat(byte[] value, int startIndex) {
        return Float.intBitsToFloat(toBigEndianInteger(value, startIndex));
    }

    public static double toBigEndianDouble(byte[] value, int startIndex) {
        return Double.longBitsToDouble(toBigEndianLong(value, startIndex));
    }

    public static String toString(byte[] value) {
        return toString(value, 0, value.length, null);
    }

    public static String toString(byte[] value, char separator) {
        return toString(value, 0, value.length, separator);
    }

    public static String toString(byte[] value, String separator) {
        return toString(value, 0, value.length, separator);
    }

    public static String toString(byte[] value, CharSequence separator) {
        return toString(value, 0, value.length, separator);
    }

    public static String toString(byte[] value, int startIndex) {
        return toString(value, startIndex, value.length - startIndex, null);
    }

    public static String toString(byte[] value, int startIndex, char separator) {
        return toString(value, startIndex, value.length - startIndex, separator);
    }

    public static String toString(byte[] value, int startIndex, String separator) {
        return toString(value, startIndex, value.length - startIndex, separator);
    }

    public static String toString(byte[] value, int startIndex, CharSequence separator) {
        return toString(value, startIndex, value.length - startIndex, separator);
    }

    public static String toString(byte[] value, int startIndex, int length) {
        return toString(value, startIndex, length, (CharSequence) null);
    }

    public static String toString(byte[] value, int startIndex, int length, char separator) {
        return toString(value, startIndex, length, separator + "");
    }

    public static String toString(byte[] value, int startIndex, int length, String separator) {
        return toString(value, startIndex, length, (CharSequence) separator);
    }

    public static String toString(byte[] value, int startIndex, int length, CharSequence separator) {
        final StringBuilder builder = new StringBuilder();
        final int count = startIndex + length;
        final int last = count - 1;
        for (int i = startIndex; i < count; i++) {
            final String hex = Integer.toHexString(value[i] & 0xFF);
            if (hex.length() == 2) {
                builder.append(hex);
            } else {
                builder.append(0).append(hex);
            }
            if (separator != null && i < last) {
                builder.append(separator);
            }
        }
        return builder.toString();
    }

    public static byte[] toBytes(@Nonnull CharSequence value) {
        final int length = value.length();
        final boolean even = isEven(length);
        final byte[] buffer = createBytes(length, even);
        final int size = even ? length : length - 1;
        for (int i = 0; i < size; i += 2) {
            buffer[i / 2] = (byte) Integer.parseInt(value.subSequence(i, i + 2).toString(), 16);
        }
        if (!even) {
            buffer[buffer.length - 1] = (byte) Integer.parseInt(value.subSequence(size, size + 1).toString(), 16);
        }
        return buffer;
    }

    public static byte[] toBytes(@Nonnull CharSequence value, char separator) {
        final int length = (int) value.chars().filter(item -> item == separator).count() + 1;
        final byte[] buffer = new byte[length];
        int index = 0;
        StringBuilder hex = new StringBuilder();
        final int valueCount = value.length();
        for (int i = 0; i < valueCount; i++) {
            final char c = value.charAt(i);
            if (c == separator) {
                if (hex.length() > 0) {
                    buffer[index++] = (byte) Integer.parseInt(hex.toString(), 16);
                }
                hex = new StringBuilder();
            } else {
                hex.append(c);
            }
        }
        if (hex.length() > 0) {
            buffer[index++] = (byte) Integer.parseInt(hex.toString(), 16);
        }
        if (index != buffer.length) {
            return Arrays.copyOf(buffer, index);
        }
        return buffer;
    }

    private static byte[] createBytes(int length, boolean event) {
        if (event) {
            return new byte[length / 2];
        }
        return new byte[length / 2 + 1];
    }

    private static boolean isEven(int value) {
        return (value & 1) == 0;
    }

    private static int getRawByte(byte value) {
        return value & BYTE_MAX_VALUE;
    }
}

