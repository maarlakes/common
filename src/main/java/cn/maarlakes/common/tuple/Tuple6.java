package cn.maarlakes.common.tuple;

import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author linjpxc
 */
public class Tuple6<T1, T2, T3, T4, T5, T6> implements Tuple {
    private static final long serialVersionUID = 4244292816786905467L;

    protected final T1 item1;
    protected final T2 item2;
    protected final T3 item3;
    protected final T4 item4;
    protected final T5 item5;
    protected final T6 item6;

    public Tuple6(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6) {
        this.item1 = item1;
        this.item2 = item2;
        this.item3 = item3;
        this.item4 = item4;
        this.item5 = item5;
        this.item6 = item6;
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

    @Override
    public int size() {
        return 6;
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
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public Tuple6<T1, T2, T3, T4, T5, T6> with(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6) {
        return new Tuple6<>(item1, item2, item3, item4, item5, item6);
    }

    public Tuple6<T1, T2, T3, T4, T5, T6> withItem1(T1 item1) {
        return new Tuple6<>(item1, this.item2, this.item3, this.item4, this.item5, this.item6);
    }

    public Tuple6<T1, T2, T3, T4, T5, T6> withItem2(T2 item2) {
        return new Tuple6<>(this.item1, item2, this.item3, this.item4, this.item5, this.item6);
    }

    public Tuple6<T1, T2, T3, T4, T5, T6> withItem3(T3 item3) {
        return new Tuple6<>(this.item1, this.item2, item3, this.item4, this.item5, this.item6);
    }

    public Tuple6<T1, T2, T3, T4, T5, T6> withItem4(T4 item4) {
        return new Tuple6<>(this.item1, this.item2, this.item3, item4, this.item5, this.item6);
    }

    public Tuple6<T1, T2, T3, T4, T5, T6> withItem5(T5 item5) {
        return new Tuple6<>(this.item1, this.item2, this.item3, this.item4, item5, this.item6);
    }

    public Tuple6<T1, T2, T3, T4, T5, T6> withItem6(T6 item6) {
        return new Tuple6<>(this.item1, this.item2, this.item3, this.item4, this.item5, item6);
    }

    public <TM1, TM2, TM3, TM4, TM5, TM6> Tuple6<TM1, TM2, TM3, TM4, TM5, TM6> map(
            Function<T1, TM1> map1,
            Function<T2, TM2> map2,
            Function<T3, TM3> map3,
            Function<T4, TM4> map4,
            Function<T5, TM5> map5,
            Function<T6, TM6> map6) {
        return new Tuple6<>(
                map1.apply(this.item1),
                map2.apply(this.item2),
                map3.apply(this.item3),
                map4.apply(this.item4),
                map5.apply(this.item5),
                map6.apply(this.item6)
        );
    }

    public <TM1> Tuple6<TM1, T2, T3, T4, T5, T6> mapItem1(Function<T1, TM1> map1) {
        return new Tuple6<>(map1.apply(this.item1), this.item2, this.item3, this.item4, this.item5, this.item6);
    }

    public <TM2> Tuple6<T1, TM2, T3, T4, T5, T6> mapItem2(Function<T2, TM2> map2) {
        return new Tuple6<>(this.item1, map2.apply(this.item2), this.item3, this.item4, this.item5, this.item6);
    }

    public <TM3> Tuple6<T1, T2, TM3, T4, T5, T6> mapItem3(Function<T3, TM3> map3) {
        return new Tuple6<>(this.item1, this.item2, map3.apply(this.item3), this.item4, this.item5, this.item6);
    }

    public <TM4> Tuple6<T1, T2, T3, TM4, T5, T6> mapItem4(Function<T4, TM4> map4) {
        return new Tuple6<>(this.item1, this.item2, this.item3, map4.apply(this.item4), this.item5, this.item6);
    }

    public <TM5> Tuple6<T1, T2, T3, T4, TM5, T6> mapItem5(Function<T5, TM5> map5) {
        return new Tuple6<>(this.item1, this.item2, this.item3, this.item4, map5.apply(this.item5), this.item6);
    }

    public <TM6> Tuple6<T1, T2, T3, T4, T5, TM6> mapItem6(Function<T6, TM6> map6) {
        return new Tuple6<>(this.item1, this.item2, this.item3, this.item4, this.item5, map6.apply(this.item6));
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Tuple6<T1, T2, T3, T4, T5, T6> clone() {
        try {
            return (Tuple6<T1, T2, T3, T4, T5, T6>) super.clone();
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
        Tuple6<?, ?, ?, ?, ?, ?> tuple6 = (Tuple6<?, ?, ?, ?, ?, ?>) o;
        return Objects.equals(item1, tuple6.item1)
                && Objects.equals(item2, tuple6.item2)
                && Objects.equals(item3, tuple6.item3)
                && Objects.equals(item4, tuple6.item4)
                && Objects.equals(item5, tuple6.item5)
                && Objects.equals(item6, tuple6.item6);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item1, item2, item3, item4, item5, item6);
    }

    @Override
    public String toString() {
        return "{" + this.item1 + ", " + this.item2 + ", " + this.item3 + ", " + this.item4 + ", " + this.item5 + ", " + this.item6 + "}";
    }
}
