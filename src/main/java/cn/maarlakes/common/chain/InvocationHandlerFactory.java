package cn.maarlakes.common.chain;

import jakarta.annotation.Nonnull;

import java.lang.reflect.InvocationHandler;

public interface InvocationHandlerFactory {

    @Nonnull
    <H> InvocationHandler create(@Nonnull Class<H> type, @Nonnull H[] handlers);
}
