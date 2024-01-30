package cn.maarlakes.common.utils;

import com.alibaba.fastjson2.annotation.JSONType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Nonnull;

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

    private DataSize(@Nonnull BigDecimal bytes) {
        this.bytes = bytes;
        this.baseFormat = new DecimalFormat();
        this.baseFormat.setGroupingUsed(false);
    }

    @Override
    public int compareTo(DataSize o) {
        return 0;
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
    public BigDecimal toMegabytes() {
        return this.toSize(Unit.MB);
    }

    @Nonnull
    public BigDecimal toGigabytes() {
        return this.toSize(Unit.GB);
    }

    @Nonnull
    public BigDecimal toTerabytes() {
        return this.toSize(Unit.TB);
    }

    @Nonnull
    public BigDecimal toPetabytes() {
        return this.toSize(Unit.PB);
    }

    @Nonnull
    public BigDecimal toExabytes() {
        return this.toSize(Unit.EB);
    }

    @Nonnull
    public BigDecimal toZettabytes() {
        return this.toSize(Unit.ZB);
    }

    @Nonnull
    public BigDecimal toYottabytes() {
        return this.toSize(Unit.YB);
    }

    public boolean isNegative() {
        return this.bytes.compareTo(BigDecimal.ZERO) < 0;
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
        for (int i = units.length - 1; i >= 0; i--) {
            final Unit unit = units[i];
            if (this.bytes.compareTo(unit.bytes) >= 0) {
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
    public static DataSize ofMegabytes(@Nonnull BigDecimal amount) {
        return of(amount, Unit.MB);
    }

    @Nonnull
    public static DataSize ofMegabytes(long amount) {
        return ofMegabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofMegabytes(double amount) {
        return ofMegabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofGigabytes(@Nonnull BigDecimal amount) {
        return of(amount, Unit.GB);
    }

    @Nonnull
    public static DataSize ofGigabytes(long amount) {
        return ofGigabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofGigabytes(double amount) {
        return ofGigabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofTerabytes(@Nonnull BigDecimal amount) {
        return of(amount, Unit.TB);
    }

    @Nonnull
    public static DataSize ofTerabytes(long amount) {
        return ofTerabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofTerabytes(double amount) {
        return ofTerabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofPetabytes(@Nonnull BigDecimal amount) {
        return of(amount, Unit.PB);
    }

    @Nonnull
    public static DataSize ofPetabytes(long amount) {
        return ofPetabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofPetabytes(double amount) {
        return ofPetabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofExabytes(@Nonnull BigDecimal amount) {
        return of(amount, Unit.EB);
    }

    @Nonnull
    public static DataSize ofExabytes(long amount) {
        return ofExabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofExabytes(double amount) {
        return ofExabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofZettabytes(@Nonnull BigDecimal amount) {
        return of(amount, Unit.ZB);
    }

    @Nonnull
    public static DataSize ofZettabytes(long amount) {
        return ofZettabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofZettabytes(double amount) {
        return ofZettabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofYottabytes(@Nonnull BigDecimal amount) {
        return of(amount, Unit.YB);
    }

    @Nonnull
    public static DataSize ofYottabytes(long amount) {
        return ofYottabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    public static DataSize ofYottabytes(double amount) {
        return ofYottabytes(BigDecimal.valueOf(amount));
    }

    @Nonnull
    @JsonCreator
    public static DataSize parse(@Nonnull CharSequence text) {
        final String value = text.toString();
        try {
            final String unitName = getUnitName(value);
            return of(new BigDecimal(value.substring(0, value.length() - unitName.length())), valueOfUnit(unitName));
        } catch (Exception e) {
            throw new DataSizeFormatException(value, e);
        }
    }

    @Nonnull
    private static Unit valueOfUnit(String unitName) {
        if (unitName == null || unitName.isEmpty()) {
            return Unit.B;
        }
        if (unitName.length() == 1 && !"b".equalsIgnoreCase(unitName)) {
            unitName += "b";
        }
        return Unit.valueOf(unitName.toUpperCase());
    }

    private static String getUnitName(@Nonnull String text) {
        final char[] array = text.toCharArray();
        final StringBuilder builder = new StringBuilder();
        for (int i = array.length - 1; i >= 0; i--) {
            final char c = array[i];
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
        YB(80);

        final int power;
        final BigDecimal bytes;

        Unit(int power) {
            this.power = power;
            this.bytes = TWO.pow(power);
        }
    }
}
