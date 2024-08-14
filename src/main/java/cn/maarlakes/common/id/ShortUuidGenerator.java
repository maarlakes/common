package cn.maarlakes.common.id;

import jakarta.annotation.Nonnull;

import java.util.UUID;

/**
 * @author linjpxc
 */
public final class ShortUuidGenerator implements IdGenerator {

    public static final char[] DEFAULT_ALPHABET = "0123456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();

    private final char[] alphabet;

    public ShortUuidGenerator() {
        this(DEFAULT_ALPHABET);
    }

    public ShortUuidGenerator(@Nonnull char[] alphabet) {
        this.alphabet = alphabet;
    }

    @Nonnull
    @Override
    public String generateId() {
        final UUID uuid = UUID.randomUUID();
        final StringBuilder builder = new StringBuilder();
        toString(builder, alphabet, uuid.getMostSignificantBits());
        toString(builder, alphabet, uuid.getLeastSignificantBits());
        return builder.toString();
    }

    private static void toString(StringBuilder builder, char[] alphabet, long value) {
        value = Math.abs(value);
        while (value > 0) {
            builder.append(alphabet[(int) (value % alphabet.length)]);
            value /= alphabet.length;
        }
    }
}
