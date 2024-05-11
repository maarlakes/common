package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.function.Consumer1;
import cn.maarlakes.common.function.Consumer3;
import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.ContentTypes;
import cn.maarlakes.common.http.Header;
import cn.maarlakes.common.http.RequestHeaders;
import cn.maarlakes.common.id.UlidGenerator;
import jakarta.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * @author linjpxc
 */
class MultipartBodyBuilder implements MultipartBody.Builder {
    private static final byte[] CRLF_BYTES = "\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] CONTENT_DISPOSITION_BYTES = "Content-Disposition: ".getBytes(US_ASCII);
    private static final byte[] FORM_DATA_DISPOSITION_TYPE_BYTES = "form-data".getBytes(US_ASCII);
    private static final byte[] NAME_BYTES = "; name=".getBytes(US_ASCII);
    private static final byte[] FILENAME_BYTES = "; filename=".getBytes(US_ASCII);
    static final byte[] QUOTE_BYTES = new byte[]{'\"'};
    private static final byte[] CONTENT_TYPE_BYTES = "Content-Type: ".getBytes(US_ASCII);
    private static final byte[] HEADER_NAME_VALUE_SEPARATOR_BYTES = ": ".getBytes(US_ASCII);

    private String boundary = UlidGenerator.getInstance().generateId();
    private ContentType contentType;
    private final List<MultipartPart<?>> parts = new ArrayList<>();

    @Nonnull
    @Override
    public MultipartBody.Builder boundary(@Nonnull String boundary) {
        this.boundary = Objects.requireNonNull(boundary);
        return this;
    }

    @Nonnull
    @Override
    public MultipartBody.Builder contentType(@Nonnull ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    @Nonnull
    @Override
    public MultipartBody.Builder addPart(@Nonnull MultipartPart<?> part) {
        this.parts.add(part);
        return this;
    }

    @Nonnull
    @Override
    public MultipartBody build() {
        return new DefaultMultipartBody();
    }

    private static byte[] getContentDispositionBytes(@Nonnull MultipartPart<?> part) {
        final Header header = part.getHeaders().getHeader("Content-Disposition");
        if (header != null) {
            final String s = header.get();
            if (s != null && !s.isEmpty()) {
                return s.getBytes(US_ASCII);
            }
        }
        return FORM_DATA_DISPOSITION_TYPE_BYTES;
    }

    private class DefaultMultipartBody implements MultipartBody {
        @Nonnull
        @Override
        public String getBoundary() {
            return boundary;
        }

        @Override
        public ContentType getContentType() {
            final ContentType type = contentType == null ? ContentType.MULTIPART_FORM_DATA : contentType;
            return type.withParameter("boundary", boundary);
        }

        @Override
        public InputStream getContentStream() {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                this.writeTo(out);
                out.flush();
                return new ByteArrayInputStream(out.toByteArray());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Collection<? extends MultipartPart<?>> getContent() {
            return parts;
        }

        @Override
        public int getContentLength() {
            return -1;
        }

        @Override
        public void writeTo(@Nonnull Consumer3<byte[], Integer, Integer> consumer) {
            final byte[] boundaryBuffer = ("--" + boundary).getBytes(US_ASCII);
            final Consumer1<byte[]> bufferConsumer = buffer -> consumer.acceptUnchecked(buffer, 0, buffer.length);
            for (MultipartPart<?> part : parts) {
                bufferConsumer.acceptUnchecked(boundaryBuffer);
                bufferConsumer.acceptUnchecked(CRLF_BYTES);
                bufferConsumer.acceptUnchecked(CONTENT_DISPOSITION_BYTES);
                bufferConsumer.acceptUnchecked(getContentDispositionBytes(part));
                if (part.getName() != null) {
                    bufferConsumer.acceptUnchecked(NAME_BYTES);
                    bufferConsumer.acceptUnchecked(QUOTE_BYTES);
                    bufferConsumer.acceptUnchecked(part.getName().getBytes(US_ASCII));
                    bufferConsumer.acceptUnchecked(QUOTE_BYTES);
                }
                if (part.getFilename() != null) {
                    bufferConsumer.acceptUnchecked(FILENAME_BYTES);
                    bufferConsumer.acceptUnchecked(QUOTE_BYTES);
                    bufferConsumer.acceptUnchecked(part.getFilename().getBytes(US_ASCII));
                    bufferConsumer.acceptUnchecked(QUOTE_BYTES);
                }
                if (part.getContentType() != null) {
                    bufferConsumer.acceptUnchecked(CRLF_BYTES);
                    bufferConsumer.acceptUnchecked(CONTENT_TYPE_BYTES);
                    bufferConsumer.acceptUnchecked(ContentTypes.toString(part.getContentType()).getBytes(US_ASCII));
                }
                if (!part.getHeaders().isEmpty()) {
                    for (Header header : part.getHeaders()) {
                        if ("Content-Disposition".equalsIgnoreCase(header.getName())) {
                            continue;
                        }
                        bufferConsumer.acceptUnchecked(CRLF_BYTES);
                        bufferConsumer.acceptUnchecked(header.getName().getBytes(US_ASCII));
                        bufferConsumer.acceptUnchecked(HEADER_NAME_VALUE_SEPARATOR_BYTES);
                        bufferConsumer.acceptUnchecked(RequestHeaders.toString(header).getBytes(US_ASCII));
                    }
                }
                bufferConsumer.acceptUnchecked(CRLF_BYTES);
                bufferConsumer.acceptUnchecked(CRLF_BYTES);
                part.writeTo(consumer);
                bufferConsumer.acceptUnchecked(CRLF_BYTES);
            }
            bufferConsumer.acceptUnchecked(("--" + boundary + "--").getBytes(US_ASCII));
        }
    }
}
