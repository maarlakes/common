package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.RequestBody;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * @author linjpxc
 */
public interface MultipartBody extends RequestBody<Collection<? extends MultipartPart<?>>> {

    @Nonnull
    String getBoundary();

    @Nonnull
    static Builder builder() {
        return new MultipartBodyBuilder();
    }

    interface Builder {

        @Nonnull
        Builder boundary(@Nonnull String boundary);

        @Nonnull
        Builder contentType(@Nonnull ContentType contentType);

        @Nonnull
        default Builder addBinaryPart(@Nonnull String name, @Nonnull byte[] buffer) {
            return this.addPart(new ByteArrayPart(name, buffer));
        }

        @Nonnull
        default Builder addBinaryPart(@Nonnull String name, @Nonnull byte[] buffer, Charset charset) {
            return this.addPart(new ByteArrayPart(name, buffer, charset));
        }

        @Nonnull
        default Builder addBinaryPart(@Nonnull String name, @Nonnull byte[] buffer, ContentType contentType) {
            return this.addPart(new ByteArrayPart(name, buffer, contentType));
        }

        @Nonnull
        default Builder addBinaryPart(@Nonnull String name, @Nonnull byte[] buffer, ContentType contentType, Charset charset) {
            return this.addPart(new ByteArrayPart(name, buffer, contentType, charset));
        }

        @Nonnull
        default Builder addTextPart(@Nonnull String name, @Nonnull String value) {
            return this.addPart(new DefaultTextPart(name, value));
        }

        @Nonnull
        default Builder addTextPart(@Nonnull String name, @Nonnull String value, @Nonnull Charset charset) {
            return this.addPart(new DefaultTextPart(name, value, charset));
        }

        @Nonnull
        default Builder addTextPart(@Nonnull String name, @Nonnull String value, ContentType contentType) {
            return this.addPart(new DefaultTextPart(name, value, contentType));
        }

        @Nonnull
        default Builder addTextPart(@Nonnull String name, @Nonnull String value, ContentType contentType, @Nonnull Charset charset) {
            return this.addPart(new DefaultTextPart(name, value, contentType, charset));
        }

        @Nonnull
        default Builder addJsonPart(@Nonnull String name, @Nonnull String json) {
            return this.addPart(new JsonPart(name, json));
        }

        @Nonnull
        default Builder addJsonPart(@Nonnull String name, @Nonnull String json, Charset charset) {
            return this.addPart(new JsonPart(name, json, charset));
        }

        @Nonnull
        default Builder addJsonPart(@Nonnull String name, @Nonnull Object obj) {
            return this.addPart(new JsonPart(name, obj));
        }

        @Nonnull
        default Builder addJsonPart(@Nonnull String name, @Nonnull Object obj, Charset charset) {
            return this.addPart(new JsonPart(name, obj, charset));
        }

        @Nonnull
        default Builder addFilePart(@Nonnull String name, @Nonnull File file) {
            return this.addPart(new DefaultFilePart(name, file));
        }

        @Nonnull
        default Builder addFilePart(@Nonnull String name, @Nonnull File file, Charset charset) {
            return this.addPart(new DefaultFilePart(name, file, charset));
        }

        @Nonnull
        default Builder addFilePart(@Nonnull String name, @Nonnull File file, ContentType contentType) {
            return this.addPart(new DefaultFilePart(name, file, contentType));
        }

        @Nonnull
        default Builder addFilePart(@Nonnull String name, @Nonnull File file, ContentType contentType, Charset charset) {
            return this.addPart(new DefaultFilePart(name, file, contentType, charset));
        }

        @Nonnull
        Builder addPart(@Nonnull MultipartPart<?> part);

        @Nonnull
        MultipartBody build();
    }
}