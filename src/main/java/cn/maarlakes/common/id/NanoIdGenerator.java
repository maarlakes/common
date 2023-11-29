package cn.maarlakes.common.id;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import jakarta.annotation.Nonnull;

import static com.aventrix.jnanoid.jnanoid.NanoIdUtils.DEFAULT_ALPHABET;
import static com.aventrix.jnanoid.jnanoid.NanoIdUtils.DEFAULT_NUMBER_GENERATOR;

/**
 * @author linjpxc
 */
public final class NanoIdGenerator implements IdGenerator {
    private static final int LENGTH = 21;

    private final int length;

    public NanoIdGenerator() {
        this(LENGTH);
    }

    public NanoIdGenerator(int length) {
        this.length = length;
    }

    @Nonnull
    @Override
    public String generateId() {
        return NanoIdUtils.randomNanoId(DEFAULT_NUMBER_GENERATOR, DEFAULT_ALPHABET, this.length);
    }
}
