package cn.maarlakes.common.id;

import jakarta.annotation.Nonnull;

import java.util.Random;

/**
 * @author linjpxc
 */
public final class SnowflakeIdGenerator implements IdGenerator {

    private static final long TWEPOCH = 1585644268888L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATA_CENTER_ID_BITS = 5L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_SHIFT;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_SHIFT + DATA_CENTER_ID_SHIFT;

    private final long workerId;
    private final long dataCenterId;
    private long sequence = 0L;
    private long lastTimestamp = 0L;

    public SnowflakeIdGenerator() {
        this.workerId = new Random().nextInt((int) MAX_WORKER_ID);
        this.dataCenterId = new Random().nextInt((int) MAX_DATA_CENTER_ID);
    }

    public SnowflakeIdGenerator(long workerId, long dataCenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATA_CENTER_ID));
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }

    @Nonnull
    @Override
    public synchronized String generateId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }
        if (this.lastTimestamp == timestamp) {
            this.sequence = (this.sequence + 1) & MAX_SEQUENCE;
            if (this.sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        this.lastTimestamp = timestamp;
        return Long.toUnsignedString(
                ((timestamp - TWEPOCH) << TIMESTAMP_LEFT_SHIFT) |
                        (dataCenterId << DATA_CENTER_ID_SHIFT) |
                        (workerId << WORKER_ID_SHIFT) |
                        sequence
        );
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
