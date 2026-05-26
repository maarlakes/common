package cn.maarlakes.common.id;


import java.security.SecureRandom;
import java.util.Random;

/**
 * @author linjpxc
 */
public class OulidGenerator implements IdGenerator, ObjectIdGenerator {

    private static final int C1_32 = 0xcc9e2d51;
    private static final int C2_32 = 0x1b873593;
    private static final int R1_32 = 15;
    private static final int R2_32 = 13;
    private static final int M_32 = 5;
    private static final int N_32 = 0xe6546b64;

    public static final int ENTROPY_LENGTH = 6;

    /**
     * Minimum allowed timestamp value.
     */
    public static final long MIN_TIME = 0x0L;
    public static final long MAX_TIME = 0x0000ffffffffffffL;

    private static final char[] C = new char[]{
            0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
            0x38, 0x39, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66,
            0x67, 0x68, 0x6a, 0x6b, 0x6d, 0x6e, 0x70, 0x71,
            0x72, 0x73, 0x74, 0x76, 0x77, 0x78, 0x79, 0x7a
    };

    private final Random random;

    private long lastTimestamp;
    private final byte[] lastEntropy = new byte[ENTROPY_LENGTH];

    private final Object LOCK = new Object();

    public OulidGenerator() {
        this(new SecureRandom());
    }

    public OulidGenerator(Random random) {
        this.random = random;
    }

    @Override
    public String generateId() {
        final byte[] data = new byte[32];
        random.nextBytes(data);
        return this.generateId(data, 0, data.length);
    }

    @Override
    public String generateId(byte[] data, int offset, int length) {
        final int hash = hash(data, offset, length);
        final byte[] entropy = new byte[ENTROPY_LENGTH + 4];
        entropy[0] = (byte) (hash >>> 24);
        entropy[1] = (byte) (hash >>> 16);
        entropy[2] = (byte) (hash >>> 8);
        entropy[3] = (byte) hash;
        long time = 0;
        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            if (now == this.lastTimestamp) {
                boolean carry = true;
                for (int i = ENTROPY_LENGTH - 1; i >= 0; i--) {
                    byte work = this.lastEntropy[i];
                    work = (byte) (work + 0x01);
                    carry = this.lastEntropy[i] == (byte) 0xff;
                    this.lastEntropy[i] = work;
                    if (!carry) {
                        break;
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
            System.arraycopy(this.lastEntropy, 0, entropy, 4, ENTROPY_LENGTH);
            time = this.lastTimestamp;
        }

        return generate(time, entropy);
    }

    private static String generate(long time, byte[] entropy) {
        if (time < MIN_TIME || time > MAX_TIME || entropy == null || entropy.length < ENTROPY_LENGTH) {
            throw new IllegalArgumentException(
                    "Time is too long, or entropy is less than " + ENTROPY_LENGTH + " bytes or null");
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

    private static int hash(byte[] data, int offset, int length) {
        int hash = 104729;
        final int blocks = length >> 2;

        for (int i = 0; i < blocks; i++) {
            final int i4 = i << 2;
            final int k = (data[offset + i4] & 0xff) | ((data[offset + i4 + 1] & 0xff) << 8)
                    | ((data[offset + i4 + 2] & 0xff) << 16) | ((data[offset + i4 + 3] & 0xff) << 24);

            hash = mix32(k, hash);
        }

        final int idx = blocks << 2;
        int k1 = 0;
        switch (length - idx) {
            case 3:
                k1 ^= data[offset + idx + 2] << 16;
            case 2:
                k1 ^= data[offset + idx + 1] << 8;
            case 1:
                k1 ^= data[offset + idx];

                // mix functions
                k1 *= C1_32;
                k1 = Integer.rotateLeft(k1, R1_32);
                k1 *= C2_32;
                hash ^= k1;
        }

        return fmix32(length, hash);
    }

    private static int mix32(int k, int hash) {
        k *= C1_32;
        k = Integer.rotateLeft(k, R1_32);
        k *= C2_32;
        hash ^= k;
        return Integer.rotateLeft(hash, R2_32) * M_32 + N_32;
    }

    private static int fmix32(final int length, int hash) {
        hash ^= length;
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);

        return hash;
    }


}
