package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * @author linjpxc
 */
final class DefaultLockContext implements LockContext {

    private final String key;
    private final boolean fair;
    private final long waitTime;
    private final long leaseTime;

    DefaultLockContext(String key, boolean fair, long waitTime, long leaseTime) {
        this.key = key;
        this.fair = fair;
        this.waitTime = waitTime;
        this.leaseTime = leaseTime;
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
    public long waitTime() {
        return this.waitTime;
    }

    @Override
    public long leaseTime() {
        return this.leaseTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LockContext)) {
            return false;
        }
        final LockContext that = (LockContext) o;
        return this.fair == that.isFair()
                && this.waitTime == that.waitTime()
                && this.leaseTime == that.leaseTime()
                && Objects.equals(this.key, that.key());
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(key);
        result = 31 * result + Boolean.hashCode(fair);
        result = 31 * result + Long.hashCode(waitTime);
        result = 31 * result + Long.hashCode(leaseTime);
        return result;
    }

    @Override
    public String toString() {
        return "key='" + key + "', fair=" + fair + ", waitTime=" + waitTime + ", leaseTime=" + leaseTime;
    }
}
