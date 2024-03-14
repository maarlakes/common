package cn.maarlakes.common.tuple;

import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author linjpxc
 */
public class Tuple3<T1, T2, T3> implements Tuple {
    private static final long serialVersionUID = 2167394502406424804L;

    protected final T1 item1;
    protected final T2 item2;
    protected final T3 item3;

    public Tuple3(T1 item1, T2 item2, T3 item3) {
        this.item1 = item1;
        this.item2 = item2;
        this.item3 = item3;
    }

    public final T1 item1() {
        return this.item1;
    }

    public final T2 item2() {
        return this.item2;
    }

    public final T3 item3() {
        return this.item3;
    }

    public final Optional<T1> optionalItem1() {
        return Optional.ofNullable(this.item1);
    }

    public final Optional<T2> optionalItem2() {
        return Optional.ofNullable(this.item2);
    }

    public final Optional<T3> optionalItem3() {
        return Optional.ofNullable(this.item3);
    }

    public Tuple3<T1, T2, T3> with(T1 item1, T2 item2, T3 item3) {
        return new Tuple3<>(item1, item2, item3);
    }

    public Tuple3<T1, T2, T3> withItem1(T1 item1) {
        return new Tuple3<>(item1, this.item2, this.item3);
    }

    public Tuple3<T1, T2, T3> withItem2(T2 item2) {
        return new Tuple3<>(this.item1, item2, this.item3);
    }

    public Tuple3<T1, T2, T3> withItem3(T3 item3) {
        return new Tuple3<>(this.item1, this.item2, item3);
    }

    public <TM1, TM2, TM3> Tuple3<TM1, TM2, TM3> map(Function<T1, TM1> map1, Function<T2, TM2> map2, Function<T3, TM3> map3) {
        return new Tuple3<>(map1.apply(this.item1), map2.apply(this.item2), map3.apply(this.item3));
    }

    public <TM1> Tuple3<TM1, T2, T3> mapItem1(Function<T1, TM1> map1) {
        return new Tuple3<>(map1.apply(this.item1), this.item2, this.item3);
    }

    public <TM2> Tuple3<T1, TM2, T3> mapItem2(Function<T2, TM2> map2) {
        return new Tuple3<>(this.item1, map2.apply(this.item2), this.item3);
    }

    public <TM3> Tuple3<T1, T2, TM3> mapItem3(Function<T3, TM3> map3) {
        return new Tuple3<>(this.item1, this.item2, map3.apply(this.item3));
    }

    @Override
    public final int size() {
        return 3;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(int index) {
        switch (index) {
            case 0:
                return (T) this.item1;
            case 1:
                return (T) this.item2;
            case 2:
                return (T) this.item3;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public <T4> Tuple4<T1, T2, T3, T4> concat(T4 item4) {
        return new Tuple4<>(this.item1, this.item2, this.item3, item4);
    }

    public <T4> Tuple4<T1, T2, T3, T4> concat(@Nonnull Tuple1<T4> tuple) {
        return new Tuple4<>(this.item1, this.item2, this.item3, tuple.item1);
    }

    public <T4, T5> Tuple5<T1, T2, T3, T4, T5> concat(T4 item4, T5 item5) {
        return new Tuple5<>(this.item1, this.item2, this.item3, item4, item5);
    }

    public <T4, T5> Tuple5<T1, T2, T3, T4, T5> concat(@Nonnull Tuple2<T4, T5> tuple) {
        return new Tuple5<>(this.item1, this.item2, this.item3, tuple.item1, tuple.item2);
    }

    public <T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> concat(T4 item4, T5 item5, T6 item6) {
        return new Tuple6<>(this.item1, this.item2, this.item3, item4, item5, item6);
    }

    public <T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> concat(@Nonnull Tuple3<T4, T5, T6> tuple) {
        return new Tuple6<>(this.item1, this.item2, this.item3, tuple.item1, tuple.item2, tuple.item3);
    }

    public <T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(T4 item4, T5 item5, T6 item6, T7 item7) {
        return new Tuple7<>(this.item1, this.item2, this.item3, item4, item5, item6, item7);
    }

    public <T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(@Nonnull Tuple4<T4, T5, T6, T7> tuple) {
        return new Tuple7<>(this.item1, this.item2, this.item3, tuple.item1, tuple.item2, tuple.item3, tuple.item4);
    }

    public <T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(T4 item4, T5 item5, T6 item6, T7 item7, T8 item8) {
        return new Tuple8<>(this.item1, this.item2, this.item3, item4, item5, item6, item7, item8);
    }

    public <T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(@Nonnull Tuple5<T4, T5, T6, T7, T8> tuple) {
        return new Tuple8<>(this.item1, this.item2, this.item3, tuple.item1, tuple.item2, tuple.item3, tuple.item4, tuple.item5);
    }

    public <T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> concat(T4 item4, T5 item5, T6 item6, T7 item7, T8 item8, T9 item9) {
        return new Tuple9<>(this.item1, this.item2, this.item3, item4, item5, item6, item7, item8, item9);
    }

    public <T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> concat(@Nonnull Tuple6<T4, T5, T6, T7, T8, T9> tuple) {
        return new Tuple9<>(this.item1, this.item2, this.item3, tuple.item1, tuple.item2, tuple.item3, tuple.item4, tuple.item5, tuple.item6);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Tuple3<T1, T2, T3> clone() {
        try {
            return (Tuple3<T1, T2, T3>) super.clone();
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
        final Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;
        return Objects.equals(item1, tuple3.item1) && Objects.equals(item2, tuple3.item2) && Objects.equals(item3, tuple3.item3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item1, item2, item3);
    }

    @Override
    public String toString() {
        return "{" + this.item1 + ", " + this.item2 + ", " + this.item3 + "}";
    }
}
