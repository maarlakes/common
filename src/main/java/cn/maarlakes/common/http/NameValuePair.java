package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * 键值对接口，表示 HTTP 参数（如查询参数、表单字段、Content-Type 参数等）的名称-值对。
 *
 * <p>继承自 {@link Serializable}，可安全地序列化传输。
 * 通过 {@link #of} 工厂方法可快速创建实例。</p>
 *
 * @author linjpxc
 */
public interface NameValuePair extends Serializable {

    /**
     * 返回参数名称。
     */
    @Nonnull
    String getName();

    /**
     * 返回参数值，可能为 {@code null}。
     */
    String getValue();

    /**
     * 创建一个键值对实例。
     *
     * @param name  参数名称
     * @param value 参数值
     * @return 新的 NameValuePair 实例
     */
    @Nonnull
    static NameValuePair of(String name, String value) {
        return new DefaultNameValuePair(name, value);
    }
}
