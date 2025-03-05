package cn.maarlakes.common.utils;

import com.alibaba.fastjson2.annotation.JSONType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;

/**
 * @author linjpxc
 */
@JSONType(serializer = DataSizeObjectWriter.class, deserializer = DataSizeObjectReader.class)
public final class DataSize implements Comparable<DataSize>, Serializable {
    private static final long serialVersionUID = -9021457487794346924L;
    public static final long UNIT_RATIO = 1024;
    private static final BigDecimal TWO = BigDecimal.valueOf(2L);
    private final BigDecimal bytes;
    private final NumberFormat baseFormat;

    public static final DataSize ZERO = new DataSize(BigDecimal.ZERO);
//    public static final DataSize BYTE = DataSize.ofBytes(1L);
//    public static final DataSize KILOBYTE = DataSize.ofKilobytes(1L);
//    public static final DataSize MEGABYTE = DataSize.ofMegabyte(1L);
//    public static final DataSize GIGABYTE = DataSize.ofGigabyte(1L);
//    private static final DataSize TERABYTE = DataSize.ofTerabyte(1L);
//    private static final DataSize PETABYTE = DataSize.ofPetabyte(1L);
//    private static final DataSize EXABYTE = DataSize.ofExabyte(1L);
//    private static final DataSize ZETTABYTE = DataSize.ofZettabyte(1L);
//    private static final DataSize YOTTABYTE = DataSize.ofYottabyte(1L);
//    private static final DataSize BRONTOBYTE = DataSize.ofBrontobyte(1L);

    private DataSize(@Nonnull BigDecimal bytes) {
        this.bytes = bytes;
        this.baseFormat = new DecimalFormat();
        this.baseFormat.setGroupingUsed(false);
    }

    @Override
    public int compareTo(@Nullable DataSize o) {
        if (o == null) {
            return 1;
        }
        return this.bytes.compareTo(o.bytes);
    }

    @Nonnull
    public DataSize plus(@Nonnull DataSize size) {
        return new DataSize(this.bytes.add(size.bytes));
    }

    @Nonnull
    public DataSize plus(@Nonnull BigDecimal size, @Nonnull Unit unit) {
        return this.plus(DataSize.of(size, unit));
    }

    @Nonnull
    public DataSize plusByte(@Nonnull BigDecimal size) {
        return this.plus(size, Unit.B);
    }

    @Nonnull
    public DataSize plusByte(double size) {
        return this.plusByte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusByte(long size) {
        return this.plusByte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusKilobyte(@Nonnull BigDecimal size) {
        return this.plus(size, Unit.KB);
    }

    @Nonnull
    public DataSize plusKilobyte(double size) {
        return this.plusKilobyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusKilobyte(long size) {
        return this.plusKilobyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusMegabyte(@Nonnull BigDecimal size) {
        return this.plus(size, Unit.MB);
    }

    @Nonnull
    public DataSize plusMegabyte(double size) {
        return this.plusMegabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusMegabyte(long size) {
        return this.plusMegabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusGigabyte(@Nonnull BigDecimal size) {
        return this.plus(size, Unit.GB);
    }

    @Nonnull
    public DataSize plusGigabyte(double size) {
        return this.plusGigabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusGigabyte(long size) {
        return this.plusGigabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusTerabyte(@Nonnull BigDecimal size) {
        return this.plus(size, Unit.TB);
    }

    @Nonnull
    public DataSize plusTerabyte(double size) {
        return this.plusTerabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusTerabyte(long size) {
        return this.plusTerabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusPetabyte(@Nonnull BigDecimal size) {
        return this.plus(size, Unit.PB);
    }

    @Nonnull
    public DataSize plusPetabyte(double size) {
        return this.plusPetabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusPetabyte(long size) {
        return this.plusPetabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusExabyte(@Nonnull BigDecimal size) {
        return this.plus(size, Unit.EB);
    }

    @Nonnull
    public DataSize plusExabyte(double size) {
        return this.plusExabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusExabyte(long size) {
        return this.plusExabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusZettabyte(@Nonnull BigDecimal size) {
        return this.plus(size, Unit.ZB);
    }

    @Nonnull
    public DataSize plusZettabyte(double size) {
        return this.plusZettabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusZettabyte(long size) {
        return this.plusZettabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusYottabyte(@Nonnull BigDecimal size) {
        return this.plus(size, Unit.YB);
    }

    @Nonnull
    public DataSize plusYottabyte(double size) {
        return this.plusYottabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize plusYottabyte(long size) {
        return this.plusYottabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minus(@Nonnull DataSize size) {
        return new DataSize(this.bytes.subtract(size.bytes));
    }

    @Nonnull
    public DataSize minus(@Nonnull BigDecimal size, @Nonnull Unit unit) {
        return this.minus(DataSize.of(size, unit));
    }

    @Nonnull
    public DataSize minusByte(@Nonnull BigDecimal size) {
        return this.minus(size, Unit.B);
    }

    @Nonnull
    public DataSize minusByte(double size) {
        return this.minusByte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusByte(long size) {
        return this.minusByte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusKilobyte(@Nonnull BigDecimal size) {
        return this.minus(size, Unit.KB);
    }

    @Nonnull
    public DataSize minusKilobyte(double size) {
        return this.minusKilobyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusKilobyte(long size) {
        return this.minusKilobyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusMegabyte(@Nonnull BigDecimal size) {
        return this.minus(size, Unit.MB);
    }

    @Nonnull
    public DataSize minusMegabyte(double size) {
        return this.minusMegabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusMegabyte(long size) {
        return this.minusMegabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusGigabyte(@Nonnull BigDecimal size) {
        return this.minus(size, Unit.GB);
    }

    @Nonnull
    public DataSize minusGigabyte(double size) {
        return this.minusGigabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusGigabyte(long size) {
        return this.minusGigabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusTerabyte(@Nonnull BigDecimal size) {
        return this.minus(size, Unit.TB);
    }

    @Nonnull
    public DataSize minusTerabyte(double size) {
        return this.minusTerabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusTerabyte(long size) {
        return this.minusTerabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusPetabyte(@Nonnull BigDecimal size) {
        return this.minus(size, Unit.PB);
    }

    @Nonnull
    public DataSize minusPetabyte(double size) {
        return this.minusPetabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusPetabyte(long size) {
        return this.minusPetabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusExabyte(@Nonnull BigDecimal size) {
        return this.minus(size, Unit.EB);
    }

    @Nonnull
    public DataSize minusExabyte(double size) {
        return this.minusExabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusExabyte(long size) {
        return this.minusExabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusZettabyte(@Nonnull BigDecimal size) {
        return this.minus(size, Unit.ZB);
    }

    @Nonnull
    public DataSize minusZettabyte(double size) {
        return this.minusZettabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusZettabyte(long size) {
        return this.minusZettabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusYottabyte(@Nonnull BigDecimal size) {
        return this.minus(size, Unit.YB);
    }

    @Nonnull
    public DataSize minusYottabyte(double size) {
        return this.minusYottabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public DataSize minusYottabyte(long size) {
        return this.minusYottabyte(BigDecimal.valueOf(size));
    }

    @Nonnull
    public BigDecimal toSize(@Nonnull Unit unit) {
        return this.bytes.divide(unit.bytes, unit.power, RoundingMode.HALF_DOWN);
    }

    @Nonnull
    public BigDecimal toByte() {
        return this.bytes;
    }

    @Nonnull
    public BigDecimal toKilobyte() {
        return this.toSize(Unit.KB);
    }

    @Nonnull
    public BigDecimal toMegabyte() {
        return this.toSize(Unit.MB);
    }

    @Nonnull
    public BigDecimal toGigabyte() {
        return this.toSize(Unit.GB);
    }

    @Nonnull
    public BigDecimal toTerabyte() {
        return this.toSize(Unit.TB);
    }

    @Nonnull
    public BigDecimal toPetabyte() {
        return this.toSize(Unit.PB);
    }

    @Nonnull
    public BigDecimal toExabyte() {
        return this.toSize(Unit.EB);
    }

    @Nonnull
    public BigDecimal toZettabyte() {
        return this.toSize(Unit.ZB);
    }

    @Nonnull
    public BigDecimal toYottabyte() {
        return this.toSize(Unit.YB);
    }

    public boolean isNegative() {
        return this.bytes.compareTo(BigDecimal.ZERO) < 0;
    }

    @Nonnull
    public DataSize abs() {
        if (this.bytes.compareTo(BigDecimal.ZERO) >= 0) {
            return this;
        }
        return new DataSize(this.bytes.abs());
    }

    @Nonnull
    public DataSize negate() {
        return new DataSize(this.bytes.negate());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DataSize) {
            return this.bytes.compareTo(((DataSize) object).bytes) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bytes);
    }

    @Override
    @JsonValue
    public String toString() {
        return this.toString((NumberFormat) this.baseFormat.clone());
    }

    @Nonnull
    public String toString(@Nonnull Unit unit) {
        return this.toString((NumberFormat) this.baseFormat.clone(), unit);
    }

    @Nonnull
    public String toString(@Nonnull NumberFormat format) {
        final Unit[] units = Unit.values();
        final BigDecimal count = this.bytes.abs();
        for (int i = units.length - 1; i >= 0; i--) {
            final Unit unit = units[i];
            if (count.compareTo(unit.bytes) >= 0) {
                return this.toString(format, unit);
            }
        }
        return this.toString(Unit.B);
    }

    @Nonnull
    public String toString(@Nonnull NumberFormat format, @Nonnull Unit unit) {
        return format.format(this.toSize(unit)) + unit;
    }

    @Nonnull
    public static DataSize of(@Nonnull BigDecimal amount, @Nonnull Unit unit) {
        return new DataSize(amount.multiply(unit.bytes));
    }

    @Nonnull
    public static DataSize of(long amount, @Nonnull Unit unit) {
        return of(BigDecimal.valueOf(amount), unit);
    }

    @Nonnull
    public static DataSize of(double amount, @Nonnull Unit unit) {
        return of(BigDecimal.valueOf(amount), unit);
    }

    @Nonnull
    public static DataSize ofBytes(@Nonnull BigDecimal amount) {
        return of(amount, Unit.B);
    }

    @Nonnull
    public static DataSize ofBytes(long amount) {
        return ofBytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofBytes(double amount) {
        return ofBytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofKilobytes(@Nonnull BigDecimal amount) {
        return of(amount, Unit.KB);
    }

    @Nonnull
    public static DataSize ofKilobytes(long amount) {
        return ofKilobytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofKilobytes(double amount) {
        return ofKilobytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofMegabyte(@Nonnull BigDecimal amount) {
        return of(amount, Unit.MB);
    }

    @Nonnull
    public static DataSize ofMegabyte(long amount) {
        return ofMegabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofMegabyte(double amount) {
        return ofMegabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofGigabyte(@Nonnull BigDecimal amount) {
        return of(amount, Unit.GB);
    }

    @Nonnull
    public static DataSize ofGigabyte(long amount) {
        return ofGigabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofGigabyte(double amount) {
        return ofGigabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofTerabyte(@Nonnull BigDecimal amount) {
        return of(amount, Unit.TB);
    }

    @Nonnull
    public static DataSize ofTerabyte(long amount) {
        return ofTerabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofTerabyte(double amount) {
        return ofTerabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofPetabyte(@Nonnull BigDecimal amount) {
        return of(amount, Unit.PB);
    }

    @Nonnull
    public static DataSize ofPetabyte(long amount) {
        return ofPetabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofPetabyte(double amount) {
        return ofPetabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofExabyte(@Nonnull BigDecimal amount) {
        return of(amount, Unit.EB);
    }

    @Nonnull
    public static DataSize ofExabyte(long amount) {
        return ofExabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofExabyte(double amount) {
        return ofExabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofZettabyte(@Nonnull BigDecimal amount) {
        return of(amount, Unit.ZB);
    }

    @Nonnull
    public static DataSize ofZettabyte(long amount) {
        return ofZettabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofZettabyte(double amount) {
        return ofZettabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofYottabyte(@Nonnull BigDecimal amount) {
        return of(amount, Unit.YB);
    }

    @Nonnull
    public static DataSize ofYottabyte(long amount) {
        return ofYottabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofYottabyte(double amount) {
        return ofYottabyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofBrontobyte(BigDecimal amount) {
        return of(amount, Unit.BB);
    }

    @Nonnull
    public static DataSize ofBrontobyte(long amount) {
        return ofBrontobyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofBrontobyte(double amount) {
        return ofBrontobyte(BigDecimal.valueOf(amount));
    }

    @Nonnull
    @JsonCreator
    public static DataSize parse(@Nonnull CharSequence text) {
        return parse(text, null);
    }

    @Nonnull
    public static DataSize parse(@Nonnull CharSequence text, NumberFormat format) {
        final String value = text.toString().trim();
        try {
            final String unitName = getUnitName(value);
            final String number = value.substring(0, value.length() - unitName.length()).trim();
            if (format != null) {
                return of(new BigDecimal(format.parse(number).toString()), valueOfUnit(unitName));
            }
            return of(new BigDecimal(number), valueOfUnit(unitName));
        } catch (Exception e) {
            throw new DataSizeFormatException(value, e);
        }
    }

    @Nonnull
    private static Unit valueOfUnit(String unitName) {
        if (unitName == null || unitName.isEmpty()) {
            return Unit.B;
        }
        unitName = unitName.toUpperCase();
        if (unitName.length() == 1 && !"B".equals(unitName)) {
            unitName += "B";
        }
        return Unit.valueOf(unitName);
    }

    private static String getUnitName(@Nonnull String text) {
        final char[] array = text.toCharArray();
        final StringBuilder builder = new StringBuilder();
        for (int i = array.length - 1; i >= 0; i--) {
            final char c = array[i];
            if (c == ' ' && builder.length() > 0) {
                break;
            }
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                builder.insert(0, c);
            }
        }
        return builder.toString();
    }

    public enum Unit {
        B(0),
        KB(10),
        MB(20),
        GB(30),
        TB(40),
        PB(50),
        EB(60),
        ZB(70),
        YB(80),
        BB(90);

        final int power;
        final BigDecimal bytes;

        Unit(int power) {
            this.power = power;
            this.bytes = TWO.pow(power);
        }
    }
}
