package cn.maarlakes.common.tuple;

import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author linjpxc
 */
public class Tuple4<T1, T2, T3, T4> implements Tuple {
    private static final long serialVersionUID = -7658846032238811696L;

    protected final T1 item1;
    protected final T2 item2;
    protected final T3 item3;
    protected final T4 item4;

    public Tuple4(T1 item1, T2 item2, T3 item3, T4 item4) {
        this.item1 = item1;
        this.item2 = item2;
        this.item3 = item3;
        this.item4 = item4;
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

    public final T4 item4() {
        return this.item4;
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

    public final Optional<T4> optionalItem4() {
        return Optional.ofNullable(this.item4);
    }

    @Override
    public final int size() {
        return 4;
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
            case 3:
                return (T) this.item4;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public Tuple4<T1, T2, T3, T4> with(T1 item1, T2 item2, T3 item3, T4 item4) {
        return new Tuple4<>(item1, item2, item3, item4);
    }

    public Tuple4<T1, T2, T3, T4> withItem1(T1 item1) {
        return new Tuple4<>(item1, this.item2, this.item3, this.item4);
    }

    public Tuple4<T1, T2, T3, T4> withItem2(T2 item2) {
        return new Tuple4<>(this.item1, item2, this.item3, this.item4);
    }

    public Tuple4<T1, T2, T3, T4> withItem3(T3 item3) {
        return new Tuple4<>(this.item1, this.item2, item3, this.item4);
    }

    public Tuple4<T1, T2, T3, T4> withItem4(T4 item4) {
        return new Tuple4<>(this.item1, this.item2, this.item3, item4);
    }

    public <TM1, TM2, TM3, TM4> Tuple4<TM1, TM2, TM3, TM4> map(Function<T1, TM1> map1, Function<T2, TM2> map2, Function<T3, TM3> map3, Function<T4, TM4> map4) {
        return new Tuple4<>(map1.apply(this.item1), map2.apply(this.item2), map3.apply(this.item3), map4.apply(this.item4));
    }

    public <TM1> Tuple4<TM1, T2, T3, T4> mapItem1(Function<T1, TM1> map1) {
        return new Tuple4<>(map1.apply(this.item1), this.item2, this.item3, this.item4);
    }

    public <TM2> Tuple4<T1, TM2, T3, T4> mapItem2(Function<T2, TM2> map2) {
        return new Tuple4<>(this.item1, map2.apply(this.item2), this.item3, this.item4);
    }

    public <TM3> Tuple4<T1, T2, TM3, T4> mapItem3(Function<T3, TM3> map3) {
        return new Tuple4<>(this.item1, this.item2, map3.apply(this.item3), this.item4);
    }

    public <TM4> Tuple4<T1, T2, T3, TM4> mapItem4(Function<T4, TM4> map4) {
        return new Tuple4<>(this.item1, this.item2, this.item3, map4.apply(this.item4));
    }

    public <T5> Tuple5<T1, T2, T3, T4, T5> concat(T5 item5) {
        return new Tuple5<>(this.item1, this.item2, this.item3, this.item4, item5);
    }

    public <T5> Tuple5<T1, T2, T3, T4, T5> concat(@Nonnull Tuple1<T5> tuple) {
        return new Tuple5<>(this.item1, this.item2, this.item3, this.item4, tuple.item1);
    }

    public <T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> concat(T5 item5, T6 item6) {
        return new Tuple6<>(this.item1, this.item2, this.item3, this.item4, item5, item6);
    }

    public <T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> concat(@Nonnull Tuple2<T5, T6> tuple) {
        return new Tuple6<>(this.item1, this.item2, this.item3, this.item4, tuple.item1, tuple.item2);
    }

    public <T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(T5 item5, T6 item6, T7 item7) {
        return new Tuple7<>(this.item1, this.item2, this.item3, this.item4, item5, item6, item7);
    }

    public <T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(@Nonnull Tuple3<T5, T6, T7> tuple) {
        return new Tuple7<>(this.item1, this.item2, this.item3, this.item4, tuple.item1, tuple.item2, tuple.item3);
    }

    public <T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(T5 item5, T6 item6, T7 item7, T8 item8) {
        return new Tuple8<>(this.item1, this.item2, this.item3, this.item4, item5, item6, item7, item8);
    }

    public <T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(@Nonnull Tuple4<T5, T6, T7, T8> tuple) {
        return new Tuple8<>(this.item1, this.item2, this.item3, this.item4, tuple.item1, tuple.item2, tuple.item3, tuple.item4);
    }

    public <T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> concat(T5 item5, T6 item6, T7 item7, T8 item8, T9 item9) {
        return new Tuple9<>(this.item1, this.item2, this.item3, this.item4, item5, item6, item7, item8, item9);
    }

    public <T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> concat(@Nonnull Tuple5<T5, T6, T7, T8, T9> tuple) {
        return new Tuple9<>(this.item1, this.item2, this.item3, this.item4, tuple.item1, tuple.item2, tuple.item3, tuple.item4, tuple.item5);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Tuple4<T1, T2, T3, T4> clone() {
        try {
            return (Tuple4<T1, T2, T3, T4>) super.clone();
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
        final Tuple4<?, ?, ?, ?> tuple4 = (Tuple4<?, ?, ?, ?>) o;
        return Objects.equals(item1, tuple4.item1) && Objects.equals(item2, tuple4.item2) && Objects.equals(item3, tuple4.item3) && Objects.equals(item4, tuple4.item4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item1, item2, item3, item4);
    }

    @Override
    public String toString() {
        return "{" + this.item1 + ", " + this.item2 + ", " + this.item3 + ", " + this.item4 + "}";
    }
}
