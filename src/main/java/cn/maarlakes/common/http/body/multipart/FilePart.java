package cn.maarlakes.common.http.body.multipart;

import java.io.File;

/**
 * @author linjpxc
 */
public interface FilePart extends MultipartPart<File> {
}
//public class FilePart extends AbstractPart<File> {
//
//    private final File file;
//
//    public FilePart(@Nonnull String name, @Nonnull File file) {
//        this(name, file, ContentType.APPLICATION_OCTET_STREAM, null);
//    }
//
//    public FilePart(@Nonnull String name, @Nonnull File file, Charset charset) {
//        this(name, file, ContentType.APPLICATION_OCTET_STREAM, charset);
//    }
//
//    public FilePart(@Nonnull String name, @Nonnull File file, ContentType contentType) {
//        this(name, file, contentType, StandardCharsets.UTF_8);
//    }
//
//    public FilePart(@Nonnull String name, @Nonnull File file, ContentType contentType, Charset charset) {
//        super(name, contentType, charset);
//        this.file = file;
//    }
//
//    @Override
//    public InputStream getContentStream() {
//        try {
//            return new FileInputStream(this.file);
//        } catch (FileNotFoundException e) {
//            throw new IllegalStateException(e);
//        }
//    }
//
//    @Override
//    public File getContent() {
//        return this.file;
//    }
//
//    @Override
//    public int getContentLength() {
//        return -1;
//    }
//
//    @Override
//    public String getFilename() {
//        if (this.filename == null || this.filename.isEmpty()) {
//            return this.file.getName();
//        }
//        return this.filename;
//    }
//
//    @Override
//    public void writeTo(@Nonnull Consumer3<byte[], Integer, Integer> consumer) {
//        try (InputStream in = Files.newInputStream(this.file.toPath())) {
//            int bytesRead;
//            byte[] buffer = new byte[8192];
//            while ((bytesRead = in.read(buffer)) >= 0) {
//                consumer.accept(buffer, 0, bytesRead);
//            }
//        } catch (Exception e) {
//            throw new IllegalStateException(e);
//        }
//    }
//}
