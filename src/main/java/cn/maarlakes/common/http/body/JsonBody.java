package cn.maarlakes.common.http.body;

import cn.maarlakes.common.http.ContentType;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Nonnull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * JSON 格式的 HTTP 请求体，Content-Type 为 {@code application/json}。
 *
 * <p>继承 {@link DefaultTextBody}，自动设置 JSON Content-Type。
 * 支持两种构造方式：直接传入 JSON 字符串，或传入任意对象通过 fastjson2 序列化为 JSON。
 * 是 HTTP 客户端发送 JSON 请求的推荐封装。</p>
 *
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
