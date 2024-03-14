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

    public <T2> Tuple2<T1, T2> concat(T2 value) {
        return new Tuple2<>(this.item1, value);
    }

    public <T2> Tuple2<T1, T2> concat(@Nonnull Tuple1<T2> tuple) {
        return concat(tuple.item1());
    }

    public <T2, T3> Tuple3<T1, T2, T3> concat(@Nonnull T2 item2, T3 item3) {
        return new Tuple3<>(this.item1, item2, item3);
    }

    public <T2, T3> Tuple3<T1, T2, T3> concat(@Nonnull Tuple2<T2, T3> tuple) {
        return concat(tuple.item1(), tuple.item2());
    }

    public <T2, T3, T4> Tuple4<T1, T2, T3, T4> concat(@Nonnull T2 item2, T3 item3, T4 item4) {
        return new Tuple4<>(this.item1, item2, item3, item4);
    }

    public <T2, T3, T4> Tuple4<T1, T2, T3, T4> concat(@Nonnull Tuple3<T2, T3, T4> tuple) {
        return concat(tuple.item1(), tuple.item2(), tuple.item3());
    }

    public <T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> concat(@Nonnull T2 item2, T3 item3, T4 item4, T5 item5) {
        return new Tuple5<>(this.item1, item2, item3, item4, item5);
    }

    public <T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> concat(@Nonnull Tuple4<T2, T3, T4, T5> tuple) {
        return concat(tuple.item1(), tuple.item2(), tuple.item3(), tuple.item4());
    }

    public <T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> concat(@Nonnull T2 item2, T3 item3, T4 item4, T5 item5, T6 item6) {
        return new Tuple6<>(this.item1, item2, item3, item4, item5, item6);
    }

    public <T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> concat(@Nonnull Tuple5<T2, T3, T4, T5, T6> tuple) {
        return concat(tuple.item1(), tuple.item2(), tuple.item3(), tuple.item4(), tuple.item5());
    }

    public <T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(@Nonnull T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7) {
        return new Tuple7<>(this.item1, item2, item3, item4, item5, item6, item7);
    }

    public <T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(@Nonnull Tuple6<T2, T3, T4, T5, T6, T7> tuple) {
        return concat(tuple.item1(), tuple.item2(), tuple.item3(), tuple.item4(), tuple.item5(), tuple.item6());
    }

    public <T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(@Nonnull T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7, T8 item8) {
        return new Tuple8<>(this.item1, item2, item3, item4, item5, item6, item7, item8);
    }

    public <T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(@Nonnull Tuple7<T2, T3, T4, T5, T6, T7, T8> tuple) {
        return concat(tuple.item1(), tuple.item2(), tuple.item3(), tuple.item4(), tuple.item5(), tuple.item6(), tuple.item7());
    }

    public <T2, T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> concat(@Nonnull T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7, T8 item8, T9 item9) {
        return new Tuple9<>(this.item1, item2, item3, item4, item5, item6, item7, item8, item9);
    }

    public <T2, T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> concat(@Nonnull Tuple8<T2, T3, T4, T5, T6, T7, T8, T9> tuple) {
        return concat(tuple.item1(), tuple.item2(), tuple.item3(), tuple.item4(), tuple.item5(), tuple.item6(), tuple.item7(), tuple.item8());
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
