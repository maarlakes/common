package cn.maarlakes.common.http.body;

import cn.maarlakes.common.function.Consumer3;
import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.Header;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * @author linjpxc
 */
public interface ContentBody<T> extends Serializable {

    ContentType getContentType();

    InputStream getContentStream();

    T getContent();

    int getContentLength();

    default Header getContentTypeHeader() {
        return this.getContentType().toHeader();
    }

    default void writeTo(@Nonnull OutputStream stream) {
        this.writeTo(stream::write);
    }

    void writeTo(@Nonnull Consumer3<byte[], Integer, Integer> consumer);
}
