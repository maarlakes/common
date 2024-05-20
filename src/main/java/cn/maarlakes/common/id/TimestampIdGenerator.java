package cn.maarlakes.common.id;

import jakarta.annotation.Nonnull;

import java.security.SecureRandom;
import java.util.Random;

/**
 * @author linjpxc
 */
public final class TimestampIdGenerator implements IdGenerator {

    private final Random random = new SecureRandom();
    private volatile long lastTimestamp;
    private long lastValue;
    private final long maxValue;
    private final int valueLength;
    private final int byteSize;

    public TimestampIdGenerator(int randomSize) {
        this.valueLength = randomSize;
        if (randomSize <= 0) {
            throw new IllegalArgumentException("randomSize must be greater than 0");
        }
        long value = 0L;
        for (int i = 0; i < randomSize; i++) {
            value = value * 10 + 9;
        }
        this.maxValue = value;

        final int bitCount = Long.bitCount(this.maxValue);
        int byteSize = bitCount / Byte.SIZE;
        if (bitCount % Byte.SIZE != 0) {
            byteSize++;
        }
        this.byteSize = byteSize + 1;
    }

    @Nonnull
    @Override
    public synchronized String generateId() {
        long now = System.currentTimeMillis();
        if (now == this.lastTimestamp) {
            this.lastValue++;
            if (this.lastValue > this.maxValue) {
                while (now == this.lastTimestamp) {
                    now = System.currentTimeMillis();
                }
                this.lastTimestamp = now;
                this.generateLastValue();
            }
        } else {
            this.lastTimestamp = now;
            this.generateLastValue();
        }
        String value = Long.toString(this.lastValue);
        if (value.length() >= this.valueLength) {
            return this.lastTimestamp + value;
        }
        StringBuilder str = new StringBuilder(value);
        while (str.length() < valueLength) {
            str.insert(0, "0");
        }
        return this.lastTimestamp + str.toString();
    }

    private void generateLastValue() {
        final byte[] buffer = new byte[this.byteSize];
        this.random.nextBytes(buffer);
        long value = 0L;
        for (byte b : buffer) {
            value = (value << 8) | (b & 0xFF);
        }
        this.lastValue = value & maxValue;
    }
}
