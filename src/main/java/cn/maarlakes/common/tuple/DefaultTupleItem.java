package cn.maarlakes.common.tuple;

import java.util.Objects;

/**
 * @author linjpxc
 */
public class DefaultTupleItem implements TupleItem {
    private static final long serialVersionUID = 5097313718538035070L;

    private final int index;
    private final Object value;

    public DefaultTupleItem(int index, Object value) {
        this.index = index;
        this.value = value;
    }

    @Override
    public int index() {
        return this.index;
    }

    @Override
    public Object value() {
        return this.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefaultTupleItem that = (DefaultTupleItem) o;
        return index == that.index && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, value);
    }
}
