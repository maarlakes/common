package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.function.Consumer3;
import cn.maarlakes.common.http.ContentType;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * @author linjpxc
 */
public class DefaultFilePart extends AbstractPart<File> implements FilePart {

    private final File file;

    public DefaultFilePart(@Nonnull String name, @Nonnull File file) {
        this(name, file, ContentType.APPLICATION_OCTET_STREAM, null);
    }

    public DefaultFilePart(@Nonnull String name, @Nonnull File file, Charset charset) {
        this(name, file, ContentType.APPLICATION_OCTET_STREAM, charset);
    }

    public DefaultFilePart(@Nonnull String name, @Nonnull File file, ContentType contentType) {
        this(name, file, contentType, StandardCharsets.UTF_8);
    }

    public DefaultFilePart(@Nonnull String name, @Nonnull File file, ContentType contentType, Charset charset) {
        super(name, contentType, charset);
        this.file = file;
    }

    @Override
    public InputStream getContentStream() {
        try {
            return new FileInputStream(this.file);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public File getContent() {
        return this.file;
    }

    @Override
    public int getContentLength() {
        return (int) this.file.length();
    }

    @Override
    public String getFilename() {
        if (this.filename == null || this.filename.isEmpty()) {
            return this.file.getName();
        }
        return this.filename;
    }

    @Override
    public void writeTo(@Nonnull Consumer3<byte[], Integer, Integer> consumer) {
        try (InputStream in = Files.newInputStream(this.file.toPath())) {
            int bytesRead;
            byte[] buffer = new byte[8192];
            while ((bytesRead = in.read(buffer)) >= 0) {
                consumer.accept(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}