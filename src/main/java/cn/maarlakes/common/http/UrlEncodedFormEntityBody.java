package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.Collection;

import static cn.maarlakes.common.http.RequestParams.format;

/**
 * @author linjpxc
 */
public class UrlEncodedFormEntityBody extends StringBody {

    public UrlEncodedFormEntityBody(@Nonnull Collection<? extends NameValuePair> params) {
        this(params, "utf-8", null);
    }

    public UrlEncodedFormEntityBody(@Nonnull Collection<? extends NameValuePair> params, Header contentEncoding) {
        this(params, "utf-8", contentEncoding);
    }

    public UrlEncodedFormEntityBody(@Nonnull Collection<? extends NameValuePair> params, String charset) {
        super(format(params, charset == null ? "utf-8" : charset), contentType(charset), null);
    }

    public UrlEncodedFormEntityBody(@Nonnull Collection<? extends NameValuePair> params, String charset, Header contentEncoding) {
        super(format(params, charset == null ? "utf-8" : charset), contentType(charset), contentEncoding);
    }

    private static ContentType contentType(String charset) {
        if (charset == null || charset.isEmpty()) {
            return ContentType.APPLICATION_FORM_URLENCODED;
        }
        return ContentType.APPLICATION_FORM_URLENCODED.withCharset(charset);
    }
}
