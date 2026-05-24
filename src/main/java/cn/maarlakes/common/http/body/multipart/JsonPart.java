package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.http.ContentType;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Nonnull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * JSON 类型的 multipart Part，Content-Type 为 {@code application/json}。
 *
 * <p>继承 {@link DefaultTextPart}，自动设置 JSON Content-Type。
 * 支持两种构造方式：直接传入 JSON 字符串，或传入任意对象通过 fastjson2 序列化。</p>
 *
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
