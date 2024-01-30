package cn.maarlakes.common.id;

import jakarta.annotation.Nonnull;

import java.security.SecureRandom;
import java.util.Random;

/**
 * @author linjpxc
 */
public final class UlidGenerator implements IdGenerator {
    /**
     * ULID string length.
     */
    public static final int ULID_LENGTH = 26;

    /**
     * ULID binary length.
     */
    public static final int ULID_BINARY_LENGTH = 16;

    /**
     * ULID entropy byte length.
     */
    public static final int ENTROPY_LENGTH = 10;

    /**
     * Minimum allowed timestamp value.
     */
    public static final long MIN_TIME = 0x0L;

    /**
     * Maximum allowed timestamp value. Encoded value can encode up to 0x0003ffffffffffffL but ULID
     * binary/byte representation states that timestamp will only be 48-bits.
     */
    public static final long MAX_TIME = 0x0000ffffffffffffL;

    private static final char[] C = new char[]{
            0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
            0x38, 0x39, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66,
            0x67, 0x68, 0x6a, 0x6b, 0x6d, 0x6e, 0x70, 0x71,
            0x72, 0x73, 0x74, 0x76, 0x77, 0x78, 0x79, 0x7a
    };

    private UlidGenerator() {
    }

    public static UlidGenerator getInstance() {
        return SingleHelper.GENERATOR;
    }

    private final Random random = new SecureRandom();
    private long lastTimestamp;
    private final byte[] lastEntropy = new byte[ENTROPY_LENGTH];

    @Nonnull
    @Override
    public synchronized String generateId() {
        long now = System.currentTimeMillis();
        if (now == this.lastTimestamp) {
            boolean carry = true;
            for (int i = ENTROPY_LENGTH - 1; i >= 0; i--) {
                if (carry) {
                    byte work = this.lastEntropy[i];
                    work = (byte) (work + 0x01);
                    carry = this.lastEntropy[i] == (byte) 0xff;
                    this.lastEntropy[i] = work;
                }
            }
            if (carry) {
                while (now == this.lastTimestamp) {
                    now = System.currentTimeMillis();
                }
                this.lastTimestamp = now;
                this.random.nextBytes(this.lastEntropy);
            }
        } else {
            // Generate new entropy
            this.lastTimestamp = now;
            this.random.nextBytes(this.lastEntropy);
        }

        return generate(this.lastTimestamp, this.lastEntropy);
    }

    private static String generate(long time, byte[] entropy) {
        if (time < MIN_TIME || time > MAX_TIME || entropy == null || entropy.length < ENTROPY_LENGTH) {
            throw new IllegalArgumentException(
                    "Time is too long, or entropy is less than 10 bytes or null");
        }

        char[] chars = new char[26];

        // time
        chars[0] = C[((byte) (time >>> 45)) & 0x1f];
        chars[1] = C[((byte) (time >>> 40)) & 0x1f];
        chars[2] = C[((byte) (time >>> 35)) & 0x1f];
        chars[3] = C[((byte) (time >>> 30)) & 0x1f];
        chars[4] = C[((byte) (time >>> 25)) & 0x1f];
        chars[5] = C[((byte) (time >>> 20)) & 0x1f];
        chars[6] = C[((byte) (time >>> 15)) & 0x1f];
        chars[7] = C[((byte) (time >>> 10)) & 0x1f];
        chars[8] = C[((byte) (time >>> 5)) & 0x1f];
        chars[9] = C[((byte) (time)) & 0x1f];

        // entropy
        chars[10] = C[(byte) ((entropy[0] & 0xff) >>> 3)];
        chars[11] = C[(byte) (((entropy[0] << 2) | ((entropy[1] & 0xff) >>> 6)) & 0x1f)];
        chars[12] = C[(byte) (((entropy[1] & 0xff) >>> 1) & 0x1f)];
        chars[13] = C[(byte) (((entropy[1] << 4) | ((entropy[2] & 0xff) >>> 4)) & 0x1f)];
        chars[14] = C[(byte) (((entropy[2] << 1) | ((entropy[3] & 0xff) >>> 7)) & 0x1f)];
        chars[15] = C[(byte) (((entropy[3] & 0xff) >>> 2) & 0x1f)];
        chars[16] = C[(byte) (((entropy[3] << 3) | ((entropy[4] & 0xff) >>> 5)) & 0x1f)];
        chars[17] = C[(byte) (entropy[4] & 0x1f)];
        chars[18] = C[(byte) ((entropy[5] & 0xff) >>> 3)];
        chars[19] = C[(byte) (((entropy[5] << 2) | ((entropy[6] & 0xff) >>> 6)) & 0x1f)];
        chars[20] = C[(byte) (((entropy[6] & 0xff) >>> 1) & 0x1f)];
        chars[21] = C[(byte) (((entropy[6] << 4) | ((entropy[7] & 0xff) >>> 4)) & 0x1f)];
        chars[22] = C[(byte) (((entropy[7] << 1) | ((entropy[8] & 0xff) >>> 7)) & 0x1f)];
        chars[23] = C[(byte) (((entropy[8] & 0xff) >>> 2) & 0x1f)];
        chars[24] = C[(byte) (((entropy[8] << 3) | ((entropy[9] & 0xff) >>> 5)) & 0x1f)];
        chars[25] = C[(byte) (entropy[9] & 0x1f)];

        return new String(chars);
    }

    private static final class SingleHelper {
        public static final UlidGenerator GENERATOR = new UlidGenerator();
    }
}
