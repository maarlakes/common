package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.http.HttpHeaders;
import cn.maarlakes.common.http.body.ContentBody;

import java.nio.charset.Charset;

/**
 * multipart 请求体中单个 Part 的抽象，定义了 Part 的名称、文件名、头部和字符集。
 *
 * <p>继承 {@link ContentBody}，在此基础上增加了 multipart 协议所需的元数据。
 * 主要实现类包括 {@link AbstractPart}（及其子类 {@code DefaultTextPart}、{@link ByteArrayPart}、
 * {@link DefaultFilePart}）和标记接口 {@link TextPart}、{@link FilePart}。</p>
 *
 * @param <T> Part 内容的原始类型
 * @author linjpxc
 */
public interface MultipartPart<T> extends ContentBody<T> {

    /** 返回 Part 的字段名称，对应 {@code Content-Disposition} 中的 {@code name} 属性。 */
    String getName();

    /** 返回 Part 的文件名，对应 {@code Content-Disposition} 中的 {@code filename} 属性，无文件名时可为 {@code null}。 */
    String getFilename();

    /** 返回 Part 的自定义 HTTP 头部集合。 */
    HttpHeaders getHeaders();

    /** 返回 Part 内容使用的字符集。 */
    Charset getCharset();
}
