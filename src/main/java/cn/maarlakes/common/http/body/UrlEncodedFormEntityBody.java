package cn.maarlakes.common.http.body;

import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.ContentTypes;
import cn.maarlakes.common.http.NameValuePair;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
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
