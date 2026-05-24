package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Objects;

/**
 * HTTP 请求方法的抽象表示。
 *
 * <p>提供标准 HTTP 方法的常量：{@link #GET}、{@link #POST}、{@link #PUT}、
 * {@link #DELETE}、{@link #PATCH}、{@link #HEAD}、{@link #OPTIONS}、{@link #TRACE}。
 * 通过 {@link #valueOf(String)} 可根据名称获取或创建方法实例。
 *
 * <p>实现 {@link Comparable}，按方法名排序。实现 {@link java.io.Serializable} 以支持序列化。
 *
 * @author linjpxc
 */
public interface HttpMethod extends Comparable<HttpMethod>, Serializable {

    /** HTTP 方法名称（如 "GET"、"POST"），始终大写。 */
    @Nonnull
    String name();

    HttpMethod GET = valueOf("GET");
    HttpMethod POST = valueOf("POST");
    HttpMethod PUT = valueOf("PUT");
    HttpMethod DELETE = valueOf("DELETE");
    HttpMethod PATCH = valueOf("PATCH");
    HttpMethod HEAD = valueOf("HEAD");
    HttpMethod OPTIONS = valueOf("OPTIONS");
    HttpMethod TRACE = valueOf("TRACE");

    /**
     * 根据名称获取 HTTP 方法。名称不区分大小写。
     *
     * @param name 方法名称，不允许为 null
     * @return 对应的 HttpMethod 实例
     */
    @Nonnull
    static HttpMethod valueOf(@Nonnull String name) {
        return HttpMethods.valueOf(Objects.requireNonNull(name));
    }
}
