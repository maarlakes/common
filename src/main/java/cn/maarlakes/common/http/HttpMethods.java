package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.TreeMap;

/**
 * HTTP 方法常量注册表，缓存标准 HTTP 方法的单例实例。
 *
 * <p>预注册了 GET、POST、PUT、DELETE、HEAD、OPTIONS、PATCH、TRACE、CONNECT 九种标准方法。
 * 对未预注册的方法名会动态创建 {@link DefaultHttpMethod} 实例。</p>
 *
 * @author linjpxc
 */
final class HttpMethods {
    private HttpMethods() {
    }

    private static final Map<String, HttpMethod> HTTP_METHODS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        HTTP_METHODS.put("GET", new DefaultHttpMethod("GET"));
        HTTP_METHODS.put("POST", new DefaultHttpMethod("POST"));
        HTTP_METHODS.put("PUT", new DefaultHttpMethod("PUT"));
        HTTP_METHODS.put("DELETE", new DefaultHttpMethod("DELETE"));
        HTTP_METHODS.put("HEAD", new DefaultHttpMethod("HEAD"));
        HTTP_METHODS.put("OPTIONS", new DefaultHttpMethod("OPTIONS"));
        HTTP_METHODS.put("PATCH", new DefaultHttpMethod("PATCH"));
        HTTP_METHODS.put("TRACE", new DefaultHttpMethod("TRACE"));
        HTTP_METHODS.put("CONNECT", new DefaultHttpMethod("CONNECT"));
    }

    @Nonnull
    static HttpMethod valueOf(@Nonnull String name) {
        final HttpMethod method = HTTP_METHODS.get(name);
        return method == null ? new DefaultHttpMethod(name) : method;
    }
}
