package cn.maarlakes.common.id;

import cn.maarlakes.common.utils.BitUtils;

import java.nio.charset.StandardCharsets;

/**
 * @author linjpxc
 */
public interface ObjectIdGenerator {

    default String generateId(Object data) {
        return this.generateId(BitUtils.getBigEndianBytes(data.hashCode()));
    }

    default String generateId(String data) {
        return this.generateId(data.getBytes(StandardCharsets.UTF_8));
    }

    default String generateId(byte[] data) {
        return this.generateId(data, 0, data.length);
    }

    String generateId(byte[] data, int offset, int length);
}
