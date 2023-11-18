package cn.maarlakes.common.factory.datetime;

import jakarta.annotation.Nonnull;

import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * @author linjpxc
 */
public interface DateTimeParser {

    boolean supported(@Nonnull String datetime, @Nonnull Class<? extends TemporalAccessor> timeType, @Nonnull Locale locale);

    @Nonnull
    TemporalAccessor parse(@Nonnull String datetime, @Nonnull Locale locale);
}
