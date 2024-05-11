package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.http.HttpHeaders;
import cn.maarlakes.common.http.body.ContentBody;

import java.nio.charset.Charset;

/**
 * @author linjpxc
 */
public interface MultipartPart<T> extends ContentBody<T> {

    String getName();

    String getFilename();

    HttpHeaders getHeaders();

    Charset getCharset();
}
