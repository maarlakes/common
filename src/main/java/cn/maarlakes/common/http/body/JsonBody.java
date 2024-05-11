package cn.maarlakes.common.http.body;

import cn.maarlakes.common.http.ContentType;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Nonnull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author linjpxc
 */
public class JsonBody extends DefaultTextBody {

    public JsonBody(@Nonnull CharSequence content) {
        super(content, ContentType.APPLICATION_JSON, StandardCharsets.UTF_8);
    }

    public JsonBody(@Nonnull Object content) {
        this(content, StandardCharsets.UTF_8);
    }

    public JsonBody(@Nonnull Object content, Charset charset) {
        this(JSON.toJSONString(content), charset);
    }

    public JsonBody(@Nonnull CharSequence content, Charset charset) {
        super(content, ContentType.APPLICATION_JSON, charset);
    }

    public JsonBody(@Nonnull Object content, String charset) {
        this(JSON.toJSONString(content), charset);
    }

    public JsonBody(@Nonnull CharSequence content, String charset) {
        super(content, ContentType.APPLICATION_JSON, charset);
    }
}
