package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author linjpxc
 */
public interface HttpMethod extends Comparable<HttpMethod>, Serializable {

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

    @Nonnull
    static HttpMethod valueOf(@Nonnull String name) {
        return HttpMethods.valueOf(Objects.requireNonNull(name));
    }
}
