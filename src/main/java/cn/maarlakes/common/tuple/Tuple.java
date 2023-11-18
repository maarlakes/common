package cn.maarlakes.common.tuple;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * @author linjpxc
 */
public interface Tuple extends Iterable<TupleItem>, Cloneable, Serializable {

    int size();

    <T> T get(int index);

    @Nonnull
    Tuple clone();

    @Nonnull
    @Override
    default Iterator<TupleItem> iterator() {
        return new Iterator<TupleItem>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return this.index < size();
            }

            @Override
            public TupleItem next() {
                if (this.hasNext()) {
                    final int i = this.index++;
                    return new DefaultTupleItem(i, get(i));
                }
                throw new NoSuchElementException();
            }
        };
    }

    @Nonnull
    default <T> Optional<T> getOptional(int index) {
        return Optional.ofNullable(this.get(index));
    }

    @Nonnull
    static <T1> Tuple1<T1> of(T1 item1) {
        return new Tuple1<>(item1);
    }

    @Nonnull
    static <T1, T2> Tuple2<T1, T2> of(T1 item1, T2 item2) {
        return new Tuple2<>(item1, item2);
    }

    @Nonnull
    static <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 item1, T2 item2, T3 item3) {
        return new Tuple3<>(item1, item2, item3);
    }

    @Nonnull
    static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(T1 item1, T2 item2, T3 item3, T4 item4) {
        return new Tuple4<>(item1, item2, item3, item4);
    }

    @Nonnull
    static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> of(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5) {
        return new Tuple5<>(item1, item2, item3, item4, item5);
    }

    @Nonnull
    static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> of(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6) {
        return new Tuple6<>(item1, item2, item3, item4, item5, item6);
    }

    @Nonnull
    static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> of(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7) {
        return new Tuple7<>(item1, item2, item3, item4, item5, item6, item7);
    }

    @Nonnull
    static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> of(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7, T8 item8) {
        return new Tuple8<>(item1, item2, item3, item4, item5, item6, item7, item8);
    }

    @Nonnull
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> of(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7, T8 item8, T9 item9) {
        return new Tuple9<>(item1, item2, item3, item4, item5, item6, item7, item8, item9);
    }
}
