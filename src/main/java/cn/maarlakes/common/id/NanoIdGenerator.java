package cn.maarlakes.common.id;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import jakarta.annotation.Nonnull;

import java.security.SecureRandom;

/**
 * @author linjpxc
 */
public final class NanoIdGenerator implements IdGenerator {
    private static final char[] DEFAULT_ALPHABET = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static final int LENGTH = 21;
    private final SecureRandom secureRandom = new SecureRandom();

    private final int length;

    public NanoIdGenerator() {
        this(LENGTH);
    }

    public NanoIdGenerator(int length) {
        this.length = length - 1;
    }

    public static NanoIdGenerator getInstance() {
        return Helper.GENERATOR;
    }

    @Nonnull
    @Override
    public String generateId() {
        return (this.secureRandom.nextInt(9) + 1) + NanoIdUtils.randomNanoId(this.secureRandom, DEFAULT_ALPHABET, this.length);
    }

    private static final class Helper {
        static final NanoIdGenerator GENERATOR = new NanoIdGenerator();
    }
}
