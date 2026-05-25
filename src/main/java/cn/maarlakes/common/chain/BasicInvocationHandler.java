package cn.maarlakes.common.chain;

import cn.maarlakes.common.reflect.AbstractInvocationHandler;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * 链式调用处理器的公共基类。
 *
 * <p>封装了处理器接口类型和处理器数组，并为 {@code equals}、{@code hashCode}、{@code toString}
 * 提供基于字段的标准实现，使得子类只需关注 {@link #handleInvocation} 中的调用分发逻辑。
 *
 * @param <H> 处理器类型
 * @author linjpxc
 */
public abstract class BasicInvocationHandler<H> extends AbstractInvocationHandler {

    /** 处理器接口类型，即动态代理所实现的接口 */
    protected final Class<H> type;

    /** 按 SPI 或 Bean 发现顺序排列的处理器实例数组 */
    protected final H[] handlers;

    protected BasicInvocationHandler(@Nonnull Class<H> type, @Nonnull H[] handlers) {
        this.type = type;
        this.handlers = handlers;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BasicInvocationHandler) {
            final BasicInvocationHandler<?> that = (BasicInvocationHandler<?>) obj;
            return this.type == that.type && Arrays.deepEquals(this.handlers, that.handlers);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type);
        result = 31 * result + Arrays.hashCode(handlers);
        return result;
    }

    @Override
    protected String toString(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) {
        return this.type.getName() + "$ChainProxy@" + Integer.toHexString(System.identityHashCode(this));
    }
}
