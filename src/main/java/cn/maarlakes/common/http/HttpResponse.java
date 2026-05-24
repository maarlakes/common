package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

/**
 * HTTP 响应的元数据接口，提供状态码、头部、Cookie 等信息，不包含响应体。
 *
 * <p>此接口用于 {@link ResponseHandler} 的流式处理场景——handler 接收响应元数据
 * 和一个 {@link cn.maarlakes.common.http.BodySink} 来消费流式响应体。
 * 而完整的 {@link Response} 接口继承此接口并添加了已缓冲的 {@link ResponseBody}。
 *
 * @author linjpxc
 */
public interface HttpResponse {

    /** HTTP 状态码（如 200、404、500）。 */
    int getStatusCode();

    /** HTTP 状态文本（如 "OK"、"Not Found"）。 */
    String getStatusText();

    /** 请求的目标 URI。 */
    URI getUri();

    /** 响应头部。 */
    @Nonnull
    HttpHeaders getHeaders();

    /** 服务器返回的 Cookie 列表。 */
    @Nonnull
    List<? extends Cookie> getCookies();

    /** 远端服务器地址（IP + 端口），连接失败时可能为 null。 */
    SocketAddress getRemoteAddress();
}
