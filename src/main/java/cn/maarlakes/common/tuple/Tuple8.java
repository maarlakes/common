package cn.maarlakes.common.tuple;

import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author linjpxc
 */
public class Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> implements Tuple {
    private static final long serialVersionUID = -7055619067412974137L;

    protected final T1 item1;
    protected final T2 item2;
    protected final T3 item3;
    protected final T4 item4;
    protected final T5 item5;
    protected final T6 item6;
    protected final T7 item7;
    protected final T8 item8;

    public Tuple8(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7, T8 item8) {
        this.item1 = item1;
        this.item2 = item2;
        this.item3 = item3;
        this.item4 = item4;
        this.item5 = item5;
        this.item6 = item6;
        this.item7 = item7;
        this.item8 = item8;
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

    public final T5 item5() {
        return this.item5;
    }

    public final T6 item6() {
        return this.item6;
    }

    public final T7 item7() {
        return this.item7;
    }

    public final T8 item8() {
        return this.item8;
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

    public final Optional<T5> optionalItem5() {
        return Optional.ofNullable(this.item5);
    }

    public final Optional<T6> optionalItem6() {
        return Optional.ofNullable(this.item6);
    }

    public final Optional<T7> optionalItem7() {
        return Optional.ofNullable(this.item7);
    }

    public final Optional<T8> optionalItem8() {
        return Optional.ofNullable(this.item8);
    }

    @Override
    public final int size() {
        return 8;
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
            case 4:
                return (T) this.item5;
            case 5:
                return (T) this.item6;
            case 6:
                return (T) this.item7;
            case 7:
                return (T) this.item8;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> with(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7, T8 item8) {
        return new Tuple8<>(item1, item2, item3, item4, item5, item6, item7, item8);
    }

    public Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> withItem1(T1 item1) {
        return new Tuple8<>(item1, this.item2, this.item3, this.item4, this.item5, this.item6, this.item7, this.item8);
    }

    public Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> withItem2(T2 item2) {
        return new Tuple8<>(this.item1, item2, this.item3, this.item4, this.item5, this.item6, this.item7, this.item8);
    }

    public Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> withItem3(T3 item3) {
        return new Tuple8<>(this.item1, this.item2, item3, this.item4, this.item5, this.item6, this.item7, this.item8);
    }

    public Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> withItem4(T4 item4) {
        return new Tuple8<>(this.item1, this.item2, this.item3, item4, this.item5, this.item6, this.item7, this.item8);
    }

    public Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> withItem5(T5 item5) {
        return new Tuple8<>(this.item1, this.item2, this.item3, this.item4, item5, this.item6, this.item7, this.item8);
    }

    public Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> withItem6(T6 item6) {
        return new Tuple8<>(this.item1, this.item2, this.item3, this.item4, this.item5, item6, this.item7, this.item8);
    }

    public Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> withItem7(T7 item7) {
        return new Tuple8<>(this.item1, this.item2, this.item3, this.item4, this.item5, this.item6, item7, this.item8);
    }

    public Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> withItem8(T8 item8) {
        return new Tuple8<>(this.item1, this.item2, this.item3, this.item4, this.item5, this.item6, this.item7, item8);
    }

    public <TM1, TM2, TM3, TM4, TM5, TM6, TM7, TM8> Tuple8<TM1, TM2, TM3, TM4, TM5, TM6, TM7, TM8> map(
            Function<T1, TM1> map1,
            Function<T2, TM2> map2,
            Function<T3, TM3> map3,
            Function<T4, TM4> map4,
            Function<T5, TM5> map5,
            Function<T6, TM6> map6,
            Function<T7, TM7> map7,
            Function<T8, TM8> map8) {
        return new Tuple8<>(
                map1.apply(this.item1),
                map2.apply(this.item2),
                map3.apply(this.item3),
                map4.apply(this.item4),
                map5.apply(this.item5),
                map6.apply(this.item6),
                map7.apply(this.item7),
                map8.apply(this.item8)
        );
    }

    public <TM1> Tuple8<TM1, T2, T3, T4, T5, T6, T7, T8> mapItem1(Function<T1, TM1> map1) {
        return new Tuple8<>(map1.apply(this.item1), this.item2, this.item3, this.item4, this.item5, this.item6, this.item7, this.item8);
    }

    public <TM2> Tuple8<T1, TM2, T3, T4, T5, T6, T7, T8> mapItem2(Function<T2, TM2> map2) {
        return new Tuple8<>(this.item1, map2.apply(this.item2), this.item3, this.item4, this.item5, this.item6, this.item7, this.item8);
    }

    public <TM3> Tuple8<T1, T2, TM3, T4, T5, T6, T7, T8> mapItem3(Function<T3, TM3> map3) {
        return new Tuple8<>(this.item1, this.item2, map3.apply(this.item3), this.item4, this.item5, this.item6, this.item7, this.item8);
    }

    public <TM4> Tuple8<T1, T2, T3, TM4, T5, T6, T7, T8> mapItem4(Function<T4, TM4> map4) {
        return new Tuple8<>(this.item1, this.item2, this.item3, map4.apply(this.item4), this.item5, this.item6, this.item7, this.item8);
    }

    public <TM5> Tuple8<T1, T2, T3, T4, TM5, T6, T7, T8> mapItem5(Function<T5, TM5> map5) {
        return new Tuple8<>(this.item1, this.item2, this.item3, this.item4, map5.apply(this.item5), this.item6, this.item7, this.item8);
    }

    public <TM6> Tuple8<T1, T2, T3, T4, T5, TM6, T7, T8> mapItem6(Function<T6, TM6> map6) {
        return new Tuple8<>(this.item1, this.item2, this.item3, this.item4, this.item5, map6.apply(this.item6), this.item7, this.item8);
    }

    public <TM7> Tuple8<T1, T2, T3, T4, T5, T6, TM7, T8> mapItem7(Function<T7, TM7> map7) {
        return new Tuple8<>(this.item1, this.item2, this.item3, this.item4, this.item5, this.item6, map7.apply(this.item7), this.item8);
    }

    public <TM8> Tuple8<T1, T2, T3, T4, T5, T6, T7, TM8> mapItem8(Function<T8, TM8> map8) {
        return new Tuple8<>(this.item1, this.item2, this.item3, this.item4, this.item5, this.item6, this.item7, map8.apply(this.item8));
    }

    public <T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> concat(T9 item9) {
        return new Tuple9<>(this.item1, this.item2, this.item3, this.item4, this.item5, this.item6, this.item7, this.item8, item9);
    }

    public <T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> concat(@Nonnull Tuple1<T9> tuple) {
        return new Tuple9<>(this.item1, this.item2, this.item3, this.item4, this.item5, this.item6, this.item7, this.item8, tuple.item1());
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> clone() {
        try {
            return (Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>) super.clone();
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
        final Tuple8<?, ?, ?, ?, ?, ?, ?, ?> tuple8 = (Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) o;
        return Objects.equals(item1, tuple8.item1)
                && Objects.equals(item2, tuple8.item2)
                && Objects.equals(item3, tuple8.item3)
                && Objects.equals(item4, tuple8.item4)
                && Objects.equals(item5, tuple8.item5)
                && Objects.equals(item6, tuple8.item6)
                && Objects.equals(item7, tuple8.item7)
                && Objects.equals(item8, tuple8.item8);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item1, item2, item3, item4, item5, item6, item7, item8);
    }

    @Override
    public String toString() {
        return "{" + this.item1 + ", " + this.item2 + ", " + this.item3 + ", "
                + this.item4 + ", " + this.item5 + ", " + this.item6 + ", "
                + this.item7 + ", " + this.item8 + "}";
    }
}
