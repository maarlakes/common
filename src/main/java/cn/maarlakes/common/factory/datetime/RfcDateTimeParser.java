package cn.maarlakes.common.factory.datetime;

import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoField.*;

/**
 * @author linjpxc
 */
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class RfcDateTimeParser implements DateTimeParser {

    private static final Pattern[] PATTERNS = new Pattern[]{
            Pattern.compile("(Mon|Tue|Wed|Thu|Fri|Sat|Sun)(,?\\s)(([1-9])|(0[1-9])|([1-2][0-9])|(3[0-1]))(-|\\s)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)(-|\\s)(\\d{2,4}|)\\s(([0-1]\\d)|(2[0-3])|(\\d)):(([0-5]\\d)|60|(\\d)):(([0-5]\\d)|60|(\\d))(\\s(GMT\\+\\d{4})|\\sGMT)?"),
            Pattern.compile("(Mon|Tue|Wed|Thu|Fri|Sat|Sun)(,?\\s)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s((0[1-9])|([1-2][0-9])|(3[0-1])|([1-9]))\\s(\\d{2,4}|)\\s(([0-1]\\d)|(2[0-3])|(\\d)):(([0-5]\\d)|60|(\\d)):(([0-5]\\d)|60|(\\d))(\\s(GMT\\+\\d{4})|\\sGMT)?")
    };

    private static final DateTimeFormatter[] FORMATTERS;

    static {
        Map<Long, String> dow = new HashMap<>();
        dow.put(1L, "Mon");
        dow.put(2L, "Tue");
        dow.put(3L, "Wed");
        dow.put(4L, "Thu");
        dow.put(5L, "Fri");
        dow.put(6L, "Sat");
        dow.put(7L, "Sun");
        Map<Long, String> moy = new HashMap<>();
        moy.put(1L, "Jan");
        moy.put(2L, "Feb");
        moy.put(3L, "Mar");
        moy.put(4L, "Apr");
        moy.put(5L, "May");
        moy.put(6L, "Jun");
        moy.put(7L, "Jul");
        moy.put(8L, "Aug");
        moy.put(9L, "Sep");
        moy.put(10L, "Oct");
        moy.put(11L, "Nov");
        moy.put(12L, "Dec");
        FORMATTERS = new DateTimeFormatter[]{
                new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .parseLenient()
                        .optionalStart()
                        .appendText(DAY_OF_WEEK, dow)
                        .appendLiteral(", ")
                        .optionalEnd()
                        .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                        .appendLiteral(' ')
                        .appendText(MONTH_OF_YEAR, moy)
                        .appendLiteral(' ')
                        .appendValueReduced(YEAR_OF_ERA, 2, 4, LocalDate.of(2000, 1, 1))
                        .appendLiteral(' ')
                        .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
                        .appendLiteral(':')
                        .appendValue(MINUTE_OF_HOUR, 2)
                        .optionalStart()
                        .appendLiteral(':')
                        .appendValue(SECOND_OF_MINUTE, 2)
                        .optionalEnd()
                        .appendLiteral(' ')
                        .optionalStart()
                        .appendLiteral("GMT")
                        .optionalEnd()
                        .appendOffset("+HHMM", "GMT")
                        .toFormatter(Locale.ENGLISH),
                new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .parseLenient()
                        .optionalStart()
                        .appendText(DAY_OF_WEEK, dow)
                        .appendLiteral(", ")
                        .optionalEnd()
                        .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                        .appendLiteral(' ')
                        .appendText(MONTH_OF_YEAR, moy)
                        .appendLiteral(' ')
                        .appendValueReduced(YEAR_OF_ERA, 2, 4, LocalDate.of(2000, 1, 1))
                        .appendLiteral(' ')
                        .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
                        .appendLiteral(':')
                        .appendValue(MINUTE_OF_HOUR, 2)
                        .optionalStart()
                        .appendLiteral(':')
                        .appendValue(SECOND_OF_MINUTE, 2)
                        .optionalEnd()
                        .appendLiteral(' ')
                        .appendOffset("+HHMM", "GMT")
                        .toFormatter(Locale.ENGLISH),
                new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .parseLenient()
                        .optionalStart()
                        .appendText(DAY_OF_WEEK, dow)
                        .appendLiteral(", ")
                        .optionalEnd()
                        .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                        .appendLiteral('-')
                        .appendText(MONTH_OF_YEAR, moy)
                        .appendLiteral('-')
                        .appendValueReduced(YEAR_OF_ERA, 2, 4, LocalDate.of(2000, 1, 1))
                        .appendLiteral(' ')
                        .appendValue(HOUR_OF_DAY, 2)
                        .appendLiteral(':')
                        .appendValue(MINUTE_OF_HOUR, 2)
                        .optionalStart()
                        .appendLiteral(':')
                        .appendValue(SECOND_OF_MINUTE, 2)
                        .optionalEnd()
                        .appendLiteral(' ')
                        .optionalStart()
                        .appendLiteral("GMT")
                        .optionalEnd()
                        .appendOffset("+HHMM", "GMT")
                        .toFormatter(Locale.ENGLISH),
                new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .parseLenient()
                        .optionalStart()
                        .appendText(DAY_OF_WEEK, dow)
                        .appendLiteral(", ")
                        .optionalEnd()
                        .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                        .appendLiteral('-')
                        .appendText(MONTH_OF_YEAR, moy)
                        .appendLiteral('-')
                        .appendValueReduced(YEAR_OF_ERA, 2, 4, LocalDate.of(2000, 1, 1))
                        .appendLiteral(' ')
                        .appendValue(HOUR_OF_DAY, 2)
                        .appendLiteral(':')
                        .appendValue(MINUTE_OF_HOUR, 2)
                        .optionalStart()
                        .appendLiteral(':')
                        .appendValue(SECOND_OF_MINUTE, 2)
                        .optionalEnd()
                        .appendLiteral(' ')
                        .appendOffset("+HHMM", "GMT")
                        .toFormatter(Locale.ENGLISH),
                new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .parseLenient()
                        .optionalStart()
                        .appendText(DAY_OF_WEEK, dow)
                        .appendLiteral(' ')
                        .optionalEnd()
                        .appendText(MONTH_OF_YEAR, moy)
                        .appendLiteral(' ')
                        .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                        .appendLiteral(' ')
                        .appendValueReduced(YEAR_OF_ERA, 2, 4, LocalDate.of(2000, 1, 1))
                        .appendLiteral(' ')
                        .appendValue(HOUR_OF_DAY, 2)
                        .appendLiteral(':')
                        .appendValue(MINUTE_OF_HOUR, 2)
                        .optionalStart()
                        .appendLiteral(':')
                        .appendValue(SECOND_OF_MINUTE, 2)
                        .optionalEnd()
                        .appendLiteral(' ')
                        .optionalStart()
                        .appendLiteral("GMT")
                        .optionalEnd()
                        .appendOffset("+HHMM", "GMT")
                        .toFormatter(Locale.ENGLISH)
        };
    }

    @Override
    public boolean supported(@Nonnull String datetime, @Nonnull Class<? extends TemporalAccessor> timeType, @Nonnull Locale locale) {
        for (Pattern pattern : PATTERNS) {
            if (pattern.matcher(datetime).matches()) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public TemporalAccessor parse(@Nonnull String datetime, @Nonnull Locale locale) {
        DateTimeException exception = null;
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return formatter.parse(datetime);
            } catch (DateTimeException e) {
                exception = e;
            }
        }
        if (exception == null) {
            throw new DateTimeException(datetime);
        }
        throw exception;
    }
}
