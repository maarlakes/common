package cn.maarlakes.common.http.body;

import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.ContentTypes;
import cn.maarlakes.common.http.NameValuePair;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * {@code application/x-www-form-urlencoded} 格式的表单请求体实现。
 *
 * <p>同时继承 {@link AbstractByteArrayBody}（复用字节缓存）和实现 {@link FormBody} 接口。
 * 通过 {@link BodyUtils#formatParamsEncode} 对键值对进行 URL 编码后转为字节数组。
 * 默认字符集 UTF-8，可通过构造函数指定。</p>
 *
 * @author linjpxc
 */
public class UrlEncodedFormEntityBody extends AbstractByteArrayBody<Collection<? extends NameValuePair>> implements FormBody {

    private final Collection<? extends NameValuePair> params;
    private final ContentType contentType;

    public UrlEncodedFormEntityBody(Collection<? extends NameValuePair> params) {
        this(params, StandardCharsets.UTF_8);
    }

    public UrlEncodedFormEntityBody(Collection<? extends NameValuePair> params, Charset charset) {
        this.params = params;
        this.contentType = charset == null ? ContentType.APPLICATION_FORM_URLENCODED : ContentType.APPLICATION_FORM_URLENCODED.withCharset(charset);
    }

    public UrlEncodedFormEntityBody(Collection<? extends NameValuePair> params, String charset) {
        this.params = params;
        this.contentType = charset == null ? ContentType.APPLICATION_FORM_URLENCODED : ContentType.APPLICATION_FORM_URLENCODED.withCharset(charset);
    }

    @Override
    public ContentType getContentType() {
        return this.contentType;
    }

    @Override
    public Collection<? extends NameValuePair> getContent() {
        return this.params;
    }

    @Override
    protected byte[] contentAsBytes() {
        return BodyUtils.contentAsBytes(BodyUtils.formatParamsEncode(this.params, ContentTypes.getCharset(this.contentType)));
    }
}
