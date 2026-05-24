package cn.maarlakes.common.http.body.multipart;

import java.io.File;

/**
 * 文件类型的 multipart Part 标记接口。
 *
 * <p>泛型参数为 {@link File}，主要实现类为 {@link DefaultFilePart}。
 * 用于在类型层面区分文件 Part 与其他类型的 Part。</p>
 *
 * @author linjpxc
 */
public interface FilePart extends MultipartPart<File> {
}