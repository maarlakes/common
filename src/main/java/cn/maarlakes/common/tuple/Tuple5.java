package cn.maarlakes.common.tuple;

import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author linjpxc
 */
public class Tuple5<T1, T2, T3, T4, T5> implements Tuple {
    private static final long serialVersionUID = 1469383794070211097L;

    protected final T1 item1;
    protected final T2 item2;
    protected final T3 item3;
    protected final T4 item4;
    protected final T5 item5;

    public Tuple5(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5) {
        this.item1 = item1;
        this.item2 = item2;
        this.item3 = item3;
        this.item4 = item4;
        this.item5 = item5;
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

    @Override
    public final int size() {
        return 5;
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
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public Tuple5<T1, T2, T3, T4, T5> with(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5) {
        return new Tuple5<>(item1, item2, item3, item4, item5);
    }

    public Tuple5<T1, T2, T3, T4, T5> withItem1(T1 item1) {
        return new Tuple5<>(item1, this.item2, this.item3, this.item4, this.item5);
    }

    public Tuple5<T1, T2, T3, T4, T5> withItem2(T2 item2) {
        return new Tuple5<>(this.item1, item2, this.item3, this.item4, this.item5);
    }

    public Tuple5<T1, T2, T3, T4, T5> withItem3(T3 item3) {
        return new Tuple5<>(this.item1, this.item2, item3, this.item4, this.item5);
    }

    public Tuple5<T1, T2, T3, T4, T5> withItem4(T4 item4) {
        return new Tuple5<>(this.item1, this.item2, this.item3, item4, this.item5);
    }

    public Tuple5<T1, T2, T3, T4, T5> withItem5(T5 item5) {
        return new Tuple5<>(this.item1, this.item2, this.item3, this.item4, item5);
    }

    public <TM1, TM2, TM3, TM4, TM5> Tuple5<TM1, TM2, TM3, TM4, TM5> map(
            Function<T1, TM1> map1,
            Function<T2, TM2> map2,
            Function<T3, TM3> map3,
            Function<T4, TM4> map4,
            Function<T5, TM5> map5) {
        return new Tuple5<>(
                map1.apply(this.item1),
                map2.apply(this.item2),
                map3.apply(this.item3),
                map4.apply(this.item4),
                map5.apply(this.item5)
        );
    }

    public <TM1> Tuple5<TM1, T2, T3, T4, T5> mapItem1(Function<T1, TM1> map1) {
        return new Tuple5<>(map1.apply(this.item1), this.item2, this.item3, this.item4, this.item5);
    }

    public <TM2> Tuple5<T1, TM2, T3, T4, T5> mapItem2(Function<T2, TM2> map2) {
        return new Tuple5<>(this.item1, map2.apply(this.item2), this.item3, this.item4, this.item5);
    }

    public <TM3> Tuple5<T1, T2, TM3, T4, T5> mapItem3(Function<T3, TM3> map3) {
        return new Tuple5<>(this.item1, this.item2, map3.apply(this.item3), this.item4, this.item5);
    }

    public <TM4> Tuple5<T1, T2, T3, TM4, T5> mapItem4(Function<T4, TM4> map4) {
        return new Tuple5<>(this.item1, this.item2, this.item3, map4.apply(this.item4), this.item5);
    }

    public <TM5> Tuple5<T1, T2, T3, T4, TM5> mapItem5(Function<T5, TM5> map5) {
        return new Tuple5<>(this.item1, this.item2, this.item3, this.item4, map5.apply(this.item5));
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Tuple5<T1, T2, T3, T4, T5> clone() {
        try {
            return (Tuple5<T1, T2, T3, T4, T5>) super.clone();
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
        final Tuple5<?, ?, ?, ?, ?> tuple5 = (Tuple5<?, ?, ?, ?, ?>) o;
        return Objects.equals(item1, tuple5.item1)
                && Objects.equals(item2, tuple5.item2)
                && Objects.equals(item3, tuple5.item3)
                && Objects.equals(item4, tuple5.item4)
                && Objects.equals(item5, tuple5.item5);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item1, item2, item3, item4, item5);
    }

    @Override
    public String toString() {
        return "{" + this.item1 + ", " + this.item2 + ", " + this.item3 + ", " + this.item4 + ", " + this.item5 + "}";
    }
}
