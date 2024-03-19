package cn.maarlakes.common.http;

import cn.maarlakes.common.tuple.KeyValuePair;
import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public class DefaultNameValuePair extends KeyValuePair<String, String> implements NameValuePair {

    public DefaultNameValuePair(@Nonnull String key, String value) {
        super(key, value);
    }

    @Nonnull
    @Override
    public String getName() {
        return this.key();
    }

    @Override
    public String getValue() {
        return this.value();
    }
}
