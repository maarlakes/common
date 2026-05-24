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
 * {@link MultipartBody} 的默认构建器实现，负责组装 Part 并生成符合 multipart 协议的字节流。
 *
 * <p>使用 ULID 作为默认 boundary 以保证唯一性。构建时通过内部类 {@code DefaultMultipartBody}
 * 按照标准 multipart/form-data 格式序列化所有 Part：boundary 分隔符、Content-Disposition 头部、
 * Content-Type 头部、自定义头部，以及 Part 内容体。</p>
 *
 * @author linjpxc
 */
class MultipartBodyBuilder implements MultipartBody.Builder {
    /** multipart 协议行终止符 CRLF */
    private static final byte[] CRLF_BYTES = "\r\n".getBytes(StandardCharsets.US_ASCII);
    /** Content-Disposition 头部名称前缀 */
    private static final byte[] CONTENT_DISPOSITION_BYTES = "Content-Disposition: ".getBytes(US_ASCII);
    /** 默认的 disposition 类型：form-data */
    private static final byte[] FORM_DATA_DISPOSITION_TYPE_BYTES = "form-data".getBytes(US_ASCII);
    /** name 属性前缀 */
    private static final byte[] NAME_BYTES = "; name=".getBytes(US_ASCII);
    /** filename 属性前缀 */
    private static final byte[] FILENAME_BYTES = "; filename=".getBytes(US_ASCII);
    /** 双引号字节，用于包围属性值 */
    static final byte[] QUOTE_BYTES = new byte[]{'\"'};
    /** Content-Type 头部名称前缀 */
    private static final byte[] CONTENT_TYPE_BYTES = "Content-Type: ".getBytes(US_ASCII);
    /** HTTP 头部名称与值的分隔符 */
    private static final byte[] HEADER_NAME_VALUE_SEPARATOR_BYTES = ": ".getBytes(US_ASCII);

    /** boundary 分隔符，默认使用 ULID 生成以保证唯一性 */
    private String boundary = UlidGenerator.getInstance().generateId();
    /** 整个 multipart 消息体的 Content-Type，默认为 multipart/form-data */
    private ContentType contentType;
    /** 待组装的 Part 列表 */
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

    /**
     * 获取 Part 的 Content-Disposition 字节表示。
     * 优先使用 Part 自定义的 Content-Disposition 头部值，否则使用默认的 {@code form-data}。
     */
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

    /**
     * {@link MultipartBody} 的内部实现，按 multipart/form-data 协议格式序列化所有 Part。
     */
    private class DefaultMultipartBody implements MultipartBody {
        @Nonnull
        @Override
        public String getBoundary() {
            return boundary;
        }

        /**
         * 返回 Content-Type，自动附加 boundary 参数。
         * 若未指定则默认为 {@code multipart/form-data}。
         */
        @Override
        public ContentType getContentType() {
            final ContentType type = contentType == null ? ContentType.MULTIPART_FORM_DATA : contentType;
            return type.withParameter("boundary", boundary);
        }

        /**
         * 将所有 Part 序列化为字节流后返回。先将内容写入 {@link ByteArrayOutputStream}，
         * 再转为 {@link ByteArrayInputStream} 供调用方读取。
         */
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

        /**
         * 返回 -1，因为 multipart 消息体的总长度需要完整序列化后才能确定，
         * 此处不预先计算以避免不必要的开销。
         */
        @Override
        public int getContentLength() {
            return -1;
        }

        /**
         * 按照 multipart/form-data 协议格式将所有 Part 写入消费者。
         *
         * <p>每个 Part 按顺序写入：boundary 行、Content-Disposition（含 name/filename 属性）、
         * Content-Type（如有）、其他自定义头部（跳过已处理的 Content-Disposition）、
         * 空行分隔、Part 内容体。最后追加终止 boundary。</p>
         */
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
                        // Content-Disposition 已在上方单独处理，此处跳过以避免重复写入
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
