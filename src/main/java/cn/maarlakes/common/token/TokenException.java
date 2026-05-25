package cn.maarlakes.common.token;

/**
 * Token 模块的统一异常类，所有 Token 相关操作的异常都应使用此异常或其子类包装。
 *
 * <p>常见的子类包括：
 * <ul>
 *   <li>{@code UnsupportedAppException} — 没有可用的 Provider 能处理给定的 AppId</li>
 *   <li>{@code WeixinTokenException} — 微信 API 返回的错误响应</li>
 * </ul>
 *
 * @author linjpxc
 */
public class TokenException extends RuntimeException {
    private static final long serialVersionUID = 6186180909649477937L;

    public TokenException() {
    }

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenException(Throwable cause) {
        super(cause);
    }

    public TokenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
