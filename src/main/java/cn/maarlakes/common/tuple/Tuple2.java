package cn.maarlakes.common.tuple;

import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author linjpxc
 */
public class Tuple2<T1, T2> implements Tuple {
    private static final long serialVersionUID = 6000042060179342005L;
    protected final T1 item1;
    protected final T2 item2;

    public Tuple2(T1 item1, T2 item2) {
        this.item1 = item1;
        this.item2 = item2;
    }

    @Override
    public final int size() {
        return 2;
    }

    public final T1 item1() {
        return this.item1;
    }

    public final T2 item2() {
        return this.item2;
    }

    public final Optional<T1> optionalItem1() {
        return Optional.ofNullable(this.item1);
    }

    public final Optional<T2> optionalItem2() {
        return Optional.ofNullable(this.item2);
    }

    public Tuple2<T1, T2> with(T1 item1, T2 item2) {
        return new Tuple2<>(item1, item2);
    }

    public Tuple2<T1, T2> withItem1(T1 item1) {
        return new Tuple2<>(item1, this.item2);
    }

    public Tuple2<T1, T2> withItem2(T2 item2) {
        return new Tuple2<>(this.item1, item2);
    }

    public <TM1, TM2> Tuple2<TM1, TM2> map(Function<T1, TM1> map1, Function<T2, TM2> map2) {
        return new Tuple2<>(map1.apply(this.item1), map2.apply(this.item2));
    }

    public <TM1> Tuple2<TM1, T2> mapItem1(Function<T1, TM1> map1) {
        return new Tuple2<>(map1.apply(this.item1), this.item2);
    }

    public <TM2> Tuple2<T1, TM2> mapItem2(Function<T2, TM2> map2) {
        return new Tuple2<>(this.item1, map2.apply(this.item2));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(int index) {
        switch (index) {
            case 0:
                return (T) this.item1;
            case 1:
                return (T) this.item2;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Tuple2<T1, T2> clone() {
        try {
            return (Tuple2<T1, T2>) super.clone();
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
        final Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;
        return Objects.equals(item1, tuple2.item1) && Objects.equals(item2, tuple2.item2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item1, item2);
    }

    @Override
    public String toString() {
        return "{" + this.item1 + ", " + this.item2 + "}";
    }
}
