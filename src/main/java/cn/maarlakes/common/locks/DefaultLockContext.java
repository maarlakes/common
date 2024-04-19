package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * @author linjpxc
 */
class DefaultLockContext implements LockContext {

    private final String key;
    private final boolean fair;

    DefaultLockContext(String key, boolean fair) {
        this.key = key;
        this.fair = fair;
    }

    @Nonnull
    @Override
    public String key() {
        return this.key;
    }

    @Override
    public boolean isFair() {
        return this.fair;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof LockContext) {
            final LockContext that = (LockContext) o;
            return Objects.equals(this.key, that.key()) && this.fair == that.isFair();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(key);
        result = 31 * result + Boolean.hashCode(fair);
        return result;
    }

    @Override
    public String toString() {
        return "key='" + key + '\'' +
                ", fair=" + fair;
    }
}
