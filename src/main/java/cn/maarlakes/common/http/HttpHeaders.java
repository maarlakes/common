package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * @author linjpxc
 */
public interface HttpHeaders extends Iterable<Header>, Serializable {

    boolean isEmpty();

    Header getHeader(@Nonnull String name);
}
