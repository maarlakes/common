package cn.maarlakes.common.factory.datetime;

import jakarta.annotation.Nonnull;

import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @author linjpxc
 */
public class TimestampDateTimeParser implements DateTimeParser {

    private static final Pattern DATE_TO_MINUTES_PATTERN = Pattern.compile("^\\d{4}((0[1-9])|(1[0-2]))((0[1-9])|([1-2][0-9])|(3[0-1]))(([0-1][0-9])|(2[0-3]))(([0-5][0-9])|60)$");
    private static final Pattern DATE_TO_SECOND_PATTERN = Pattern.compile("^\\d{4}((0[1-9])|(1[0-2]))((0[1-9])|([1-2][0-9])|(3[0-1]))(([0-1][0-9])|(2[0-3]))(([0-5][0-9])|60)(([0-5][0-9])|60)$");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^\\d+$");

    @Override
    public boolean supported(@Nonnull String datetime, @Nonnull Class<? extends TemporalAccessor> timeType, @Nonnull Locale locale) {
        if (datetime.length() == 14 && DATE_TO_SECOND_PATTERN.matcher(datetime).matches()) {
            return false;
        }
        if (datetime.length() == 12 && DATE_TO_MINUTES_PATTERN.matcher(datetime).matches()) {
            return false;
        }
        return TIMESTAMP_PATTERN.matcher(datetime).matches();
    }

    @Nonnull
    @Override
    public TemporalAccessor parse(@Nonnull String datetime, @Nonnull Locale locale) {
        return DateTimeFactories.fromEpochMilli(Long.parseUnsignedLong(datetime));
    }
}
