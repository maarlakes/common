package cn.maarlakes.common.factory.datetime;

import cn.maarlakes.common.Ordered;
import cn.maarlakes.common.OrderedComparator;
import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.Comparators;
import cn.maarlakes.common.utils.Lazy;
import jakarta.annotation.Nonnull;

import java.time.*;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.time.temporal.ChronoField.*;

/**
 * @author linjpxc
 */
public final class DateTimeFactories {
    private DateTimeFactories() {
    }

    private static final Supplier<List<ParserWrapper>> PARSER = Lazy.of(() ->
            StreamSupport.stream(SpiServiceLoader.loadShared(DateTimeParser.class, DateTimeParser.class.getClassLoader()).spliterator(), false)
                    .map(ParserWrapper::new)
                    .collect(Collectors.toList())
    );

    private static final Comparator<Object> COMPARATOR = OrderedComparator.getInstance().reversed();

    public static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
            .optionalStart()
            .appendLiteral(':')
            .optionalEnd()
            .appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
            .optionalStart()
            .appendLiteral(':')
            .optionalEnd()
            .optionalStart()
            .appendValue(SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE)
            .optionalStart()
            .appendFraction(NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .optionalStart()
            .appendFraction(NANO_OF_SECOND, 0, 9, false)
            .optionalEnd()
            .optionalStart()
            .appendOffsetId()
            .toFormatter();

    public static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .optionalStart()
            .appendValueReduced(YEAR_OF_ERA, 2, 4, LocalDate.of(2000, 1, 1))
            .optionalEnd()
            .optionalStart()
            .appendLiteral('-')
            .optionalEnd()
            .optionalStart()
            .appendLiteral('/')
            .optionalEnd()
            .optionalStart()
            .appendLiteral('年')
            .optionalEnd()
            .appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NORMAL)
            .optionalStart()
            .appendLiteral('-')
            .optionalEnd()
            .optionalStart()
            .appendLiteral('/')
            .optionalEnd()
            .optionalStart()
            .appendLiteral('月')
            .optionalEnd()
            .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NORMAL)
            .optionalStart()
            .appendLiteral('日')
            .optionalEnd()
            .toFormatter();

    public static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DATE_FORMATTER)
            .optionalStart()
            .appendLiteral(' ')
            .optionalEnd()
            .optionalStart()
            .appendLiteral('T')
            .optionalEnd()
            .optionalStart()
            .append(TIME_FORMATTER)
            .optionalEnd()
            .toFormatter();

    @Nonnull
    public static LocalDateTime parse(@Nonnull String datetime) {
        return parse(datetime, Locale.getDefault(Locale.Category.FORMAT));
    }

    @Nonnull
    public static LocalDateTime parse(@Nonnull String datetime, @Nonnull Locale locale) {
        final String newDatetime = datetime.trim();
        return PARSER.get().stream().sorted(COMPARATOR)
                .filter(item -> item.parser.supported(newDatetime, LocalDateTime.class, locale))
                .findFirst()
                .map(item -> {
                    final LocalDateTime time = toLocalDateTime(item.parser.parse(newDatetime, locale));
                    item.counter.incrementAndGet();
                    return time;
                }).orElseGet(() -> LocalDateTime.parse(newDatetime, DATE_TIME_FORMATTER.withLocale(locale)));
    }

    @Nonnull
    @SafeVarargs
    public static <T extends ChronoLocalDateTime<?>> T min(@Nonnull T first, @Nonnull T... others) {
        final T min = Comparators.min(others);
        if (min != null && first.compareTo(min) > 0) {
            return min;
        }
        return first;
    }

    @Nonnull
    @SafeVarargs
    public static <T extends ChronoLocalDateTime<?>> T max(@Nonnull T first, @Nonnull T... others) {
        final T max = Comparators.max(others);
        if (max != null && first.compareTo(max) < 0) {
            return max;
        }
        return first;
    }

    @Nonnull
    @SafeVarargs
    public static <T extends ChronoLocalDate> T min(@Nonnull T first, @Nonnull T... others) {
        final T min = Comparators.min(others);
        if (min != null && first.compareTo(min) > 0) {
            return min;
        }
        return first;
    }

    @Nonnull
    @SafeVarargs
    public static <T extends ChronoLocalDate> T max(@Nonnull T first, @Nonnull T... others) {
        final T max = Comparators.max(others);
        if (max != null && first.compareTo(max) < 0) {
            return max;
        }
        return first;
    }

    @Nonnull
    @SafeVarargs
    public static <T extends ChronoZonedDateTime<?>> T min(@Nonnull T first, @Nonnull T... others) {
        final T min = Comparators.min(others);
        if (min != null && first.compareTo(min) > 0) {
            return min;
        }
        return first;
    }

    @Nonnull
    @SafeVarargs
    public static <T extends ChronoZonedDateTime<?>> T max(@Nonnull T first, @Nonnull T... others) {
        final T max = Comparators.max(others);
        if (max != null && first.compareTo(max) < 0) {
            return max;
        }
        return first;
    }

    @Nonnull
    public static LocalTime min(@Nonnull LocalTime first, @Nonnull LocalTime... others) {
        final LocalTime min = Comparators.min(others);
        if (min != null && first.isAfter(min)) {
            return min;
        }
        return first;
    }

    @Nonnull
    public static LocalTime max(@Nonnull LocalTime first, @Nonnull LocalTime... others) {
        final LocalTime max = Comparators.max(others);
        if (max != null && first.isBefore(max)) {
            return max;
        }
        return first;
    }

    @Nonnull
    public static Instant min(@Nonnull Instant first, @Nonnull Instant... others) {
        final Instant min = Comparators.min(others);
        if (min != null && first.isAfter(min)) {
            return min;
        }
        return first;
    }

    @Nonnull
    public static Instant max(@Nonnull Instant first, @Nonnull Instant... others) {
        final Instant max = Comparators.max(others);
        if (max != null && first.isBefore(max)) {
            return max;
        }
        return first;
    }

    @Nonnull
    public static OffsetDateTime min(@Nonnull OffsetDateTime first, @Nonnull OffsetDateTime... others) {
        final OffsetDateTime min = Comparators.min(others);
        if (min != null && first.isAfter(min)) {
            return min;
        }
        return first;
    }

    @Nonnull
    public static OffsetDateTime max(@Nonnull OffsetDateTime first, @Nonnull OffsetDateTime... others) {
        final OffsetDateTime max = Comparators.max(others);
        if (max != null && first.isBefore(max)) {
            return max;
        }
        return first;
    }

    @Nonnull
    public static OffsetTime min(@Nonnull OffsetTime first, @Nonnull OffsetTime... others) {
        final OffsetTime min = Comparators.min(others);
        if (min != null && first.isAfter(min)) {
            return min;
        }
        return first;
    }

    @Nonnull
    public static OffsetTime max(@Nonnull OffsetTime first, @Nonnull OffsetTime... others) {
        final OffsetTime max = Comparators.max(others);
        if (max != null && first.isBefore(max)) {
            return max;
        }
        return first;
    }

    @Nonnull
    public static LocalDateTime parse(@Nonnull String datetime, @Nonnull String pattern) {
        return LocalDateTime.parse(datetime, DateTimeFormatter.ofPattern(pattern));
    }

    @Nonnull
    public static LocalDateTime fromEpochSecond(long epochSecond) {
        return fromEpochSecond(epochSecond, ZoneId.systemDefault());
    }

    @Nonnull
    public static LocalDateTime fromEpochSecond(long epochSecond, @Nonnull ZoneId zone) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), zone);
    }

    @Nonnull
    public static LocalDateTime fromEpochSecond(long epochSecond, long nanoAdjustment) {
        return fromEpochSecond(epochSecond, nanoAdjustment, ZoneId.systemDefault());
    }

    @Nonnull
    public static LocalDateTime fromEpochSecond(long epochSecond, long nanoAdjustment, @Nonnull ZoneId zone) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond, nanoAdjustment), zone);
    }

    @Nonnull
    public static LocalDateTime fromEpochMilli(long epocMilli) {
        return fromEpochMilli(epocMilli, ZoneId.systemDefault());
    }

    @Nonnull
    public static LocalDateTime fromEpochMilli(long epocMilli, @Nonnull ZoneId zone) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epocMilli), zone);
    }

    public static long toEpochSecond(@Nonnull LocalDateTime dateTime) {
        return toEpochSecond(dateTime, ZoneOffset.systemDefault().getRules().getOffset(dateTime));
    }

    public static long toEpochSecond(@Nonnull LocalDateTime dateTime, @Nonnull ZoneOffset offset) {
        return toEpochMilli(dateTime, offset) / 1000;
    }

    public static long toEpochMilli(@Nonnull LocalDateTime dateTime) {
        return toEpochMilli(dateTime, ZoneOffset.systemDefault().getRules().getOffset(dateTime));
    }

    public static long toEpochMilli(@Nonnull LocalDateTime dateTime, @Nonnull ZoneOffset offset) {
        return dateTime.toInstant(offset).toEpochMilli();
    }

    private static LocalDateTime toLocalDateTime(@Nonnull TemporalAccessor accessor) {
        if (accessor instanceof LocalDateTime) {
            return (LocalDateTime) accessor;
        }
        if (accessor instanceof ZonedDateTime) {
            return ((ZonedDateTime) accessor).toLocalDateTime();
        }
        if (accessor instanceof OffsetDateTime) {
            return ((OffsetDateTime) accessor).toLocalDateTime();
        }
        if (accessor instanceof LocalDate) {
            return LocalDateTime.of((LocalDate) accessor, LocalTime.MIN);
        }
        if (accessor instanceof LocalTime) {
            return LocalDateTime.of(LocalDate.now(), (LocalTime) accessor);
        }
        if (accessor instanceof OffsetTime) {
            return LocalDateTime.of(LocalDate.now(), ((OffsetTime) accessor).toLocalTime());
        }
        if (accessor instanceof Instant) {
            final Instant instant = (Instant) accessor;
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }

        final LocalDate date = tryToLocalDate(accessor);
        final LocalTime time = tryToLocalTime(accessor);
        if (date != null) {
            if (time == null) {
                return LocalDateTime.of(date, LocalTime.MIDNIGHT);
            }
            return LocalDateTime.of(date, time);
        }
        if (time != null) {
            return LocalDateTime.of(LocalDate.now(), time);
        }
        return LocalDateTime.from(accessor);
    }

    private static LocalDate tryToLocalDate(@Nonnull TemporalAccessor accessor) {
        if (accessor instanceof LocalDate) {
            return (LocalDate) accessor;
        }
        if (accessor.isSupported(EPOCH_DAY)) {
            return LocalDate.ofEpochDay(accessor.getLong(EPOCH_DAY));
        }

        if (!accessor.isSupported(YEAR)) {
            if (!accessor.isSupported(MONTH_OF_YEAR)) {
                return null;
            }
            final int month = accessor.get(MONTH_OF_YEAR);
            final LocalDate now = LocalDate.now();
            if (!accessor.isSupported(DAY_OF_MONTH)) {
                return LocalDate.of(now.getYear(), month, 1);
            }
            return LocalDate.of(now.getYear(), month, accessor.get(DAY_OF_MONTH));
        }
        if (!accessor.isSupported(MONTH_OF_YEAR)) {
            return LocalDate.of(accessor.get(YEAR), 1, 1);
        }
        if (!accessor.isSupported(DAY_OF_MONTH)) {
            return LocalDate.of(accessor.get(YEAR), accessor.get(MONTH_OF_YEAR), 1);
        }
        return LocalDate.of(accessor.get(YEAR), accessor.get(MONTH_OF_YEAR), accessor.get(DAY_OF_MONTH));
    }

    private static LocalTime tryToLocalTime(@Nonnull TemporalAccessor accessor) {
        if (accessor instanceof LocalTime) {
            return (LocalTime) accessor;
        }
        if (accessor instanceof OffsetTime) {
            return ((OffsetTime) accessor).toLocalTime();
        }
        final LocalTime time = accessor.query(TemporalQueries.localTime());
        if (time != null) {
            return time;
        }
        if (accessor.isSupported(SECOND_OF_DAY)) {
            return LocalTime.ofSecondOfDay(accessor.getLong(SECOND_OF_DAY));
        }
        if (!accessor.isSupported(HOUR_OF_DAY)) {
            return null;
        }

        final int hour = accessor.get(HOUR_OF_DAY);
        if (!accessor.isSupported(MINUTE_OF_HOUR)) {
            return LocalTime.of(hour, 0);
        }
        final int minute = accessor.get(MINUTE_OF_HOUR);
        if (!accessor.isSupported(SECOND_OF_MINUTE)) {
            return LocalTime.of(hour, minute);
        }
        final int second = accessor.get(SECOND_OF_MINUTE);
        if (!accessor.isSupported(NANO_OF_SECOND)) {
            return LocalTime.of(hour, minute, second);
        }
        return LocalTime.of(hour, minute, second, accessor.get(NANO_OF_SECOND));
    }

    private static final class ParserWrapper implements Ordered {
        private final DateTimeParser parser;
        private final AtomicInteger counter = new AtomicInteger(0);

        public ParserWrapper(DateTimeParser parser) {
            this.parser = parser;
        }

        @Override
        public int order() {
            return this.counter.get();
        }

//        @Override
//        public int compareTo(@Nullable ParserWrapper other) {
//            if (other == null) {
//                return 1;
//            }
//            return Integer.compareUnsigned(other.counter.get(), this.counter.get());
//        }
    }
}
