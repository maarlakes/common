package cn.maarlakes.common.factory.datetime;

import cn.maarlakes.common.factory.ProviderFactories;
import cn.maarlakes.common.utils.Lazy;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoField.*;

/**
 * @author linjpxc
 */
public final class DateTimeFactories {
    private DateTimeFactories() {
    }

    private static final Supplier<List<ParserWrapper>> PARSER = Lazy.of(() -> Arrays.stream(ProviderFactories.getProviders(DateTimeParser.class).get()).map(ParserWrapper::new).collect(Collectors.toList()));

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
        return PARSER.get().stream().sorted()
                .filter(item -> item.parser.supported(newDatetime, LocalDateTime.class, locale))
                .findFirst()
                .map(item -> {
                    final LocalDateTime time = toLocalDateTime(item.parser.parse(newDatetime, locale));
                    item.counter.incrementAndGet();
                    return time;
                }).orElseGet(() -> LocalDateTime.parse(newDatetime, DATE_TIME_FORMATTER.withLocale(locale)));
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

    private static final class ParserWrapper implements Comparable<ParserWrapper> {
        private final DateTimeParser parser;
        private final AtomicInteger counter = new AtomicInteger(0);

        public ParserWrapper(DateTimeParser parser) {
            this.parser = parser;
        }

        @Override
        public int compareTo(@Nullable ParserWrapper other) {
            if (other == null) {
                return 1;
            }
            return Integer.compareUnsigned(other.counter.get(), this.counter.get());
        }
    }
}
