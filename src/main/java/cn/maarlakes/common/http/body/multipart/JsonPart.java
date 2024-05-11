package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.http.ContentType;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Nonnull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author linjpxc
 */
public class JsonPart extends DefaultTextPart {
    public JsonPart(@Nonnull String name, @Nonnull Object value) {
        this(name, value, StandardCharsets.UTF_8);
    }

    public JsonPart(@Nonnull String name, @Nonnull Object value, Charset charset) {
        this(name, JSONObject.toJSONString(value), charset);
    }

    public JsonPart(@Nonnull String name, @Nonnull String value) {
        this(name, value, StandardCharsets.UTF_8);
    }

    public JsonPart(@Nonnull String name, @Nonnull String value, Charset charset) {
        super(name, value, ContentType.APPLICATION_JSON, charset);
    }
}
