package cn.maarlakes.common.http;

import cn.maarlakes.common.tuple.KeyValuePair;
import jakarta.annotation.Nonnull;

/**
 * {@link NameValuePair} 的默认实现，继承自 {@link KeyValuePair}。
 *
 * <p>将 {@link KeyValuePair} 的 {@code key()} 和 {@code value()} 映射为
 * {@link NameValuePair} 接口的 {@code getName()} 和 {@code getValue()}。</p>
 *
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
