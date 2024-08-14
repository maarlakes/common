package cn.maarlakes.common.http.body;

import jakarta.annotation.Nonnull;

import java.nio.channels.WritableByteChannel;

/**
 * @author linjpxc
 */
public interface ContentChannel {

    void transferTo(@Nonnull WritableByteChannel channel);
}
