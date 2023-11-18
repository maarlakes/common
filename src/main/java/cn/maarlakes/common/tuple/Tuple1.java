package cn.maarlakes.common.tuple;

import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author linjpxc
 */
public class Tuple1<T1> implements Tuple {
    private static final long serialVersionUID = 6543116642260873299L;
    protected final T1 item1;

    public Tuple1(T1 item1) {
        this.item1 = item1;
    }

    @Override
    public final int size() {
        return 1;
    }

    public final T1 item1() {
        return this.item1;
    }

    public final Optional<T1> optionalItem1() {
        return Optional.ofNullable(this.item1);
    }

    public Tuple1<T1> with(T1 item1) {
        return new Tuple1<>(item1);
    }

    public Tuple1<T1> withItem1(T1 item1) {
        return new Tuple1<>(item1);
    }

    public <TR> Tuple1<TR> map(Function<T1, TR> map) {
        return mapItem1(map);
    }

    public <TR> Tuple1<TR> mapItem1(Function<T1, TR> map) {
        return new Tuple1<>(map.apply(this.item1));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(int index) {
        if (index == 0) {
            return (T) this.item1;
        }
        throw new IndexOutOfBoundsException();
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Tuple1<T1> clone() {
        try {
            return (Tuple1<T1>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Tuple1<?> tuple1 = (Tuple1<?>) o;
        return Objects.equals(item1, tuple1.item1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item1);
    }

    @Override
    public String toString() {
        return "{" + this.item1 + "}";
    }
}
