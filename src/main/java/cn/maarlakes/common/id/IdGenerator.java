package cn.maarlakes.common.id;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface IdGenerator {

    @Nonnull
    String generateId();
}
