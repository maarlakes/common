package cn.maarlakes.common.http.body;

import cn.maarlakes.common.function.Consumer3;
import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.Header;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author linjpxc
 */
public interface ContentBody<T> {

    @Nullable
    ContentType getContentType();

    InputStream getContentStream();

    T getContent();

    int getContentLength();

    @Nullable
    default Header getContentTypeHeader() {
        final ContentType ct = this.getContentType();
        return ct == null ? null : ct.toHeader();
    }

    default void writeTo(@Nonnull OutputStream stream) {
        this.writeTo(stream::write);
    }

    void writeTo(@Nonnull Consumer3<byte[], Integer, Integer> consumer);
}
