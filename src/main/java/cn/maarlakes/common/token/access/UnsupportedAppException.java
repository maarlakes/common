package cn.maarlakes.common.token.access;

import cn.maarlakes.common.token.TokenException;

/**
 * 当没有 {@link AccessTokenProvider} 能处理给定的 {@link AppId} 时抛出此异常。
 *
 * <p>通常由 {@link DefaultAccessTokenFactory} 在遍历所有 Provider 后无法匹配时抛出，
 * 表明注册的 Provider 列表中缺少对当前 AppId 类型的支持。
 *
 * @author linjpxc
 */
public class UnsupportedAppException extends TokenException {
    private static final long serialVersionUID = -7753187741470224815L;

    public UnsupportedAppException() {
    }

    public UnsupportedAppException(String message) {
        super(message);
    }

    public UnsupportedAppException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedAppException(Throwable cause) {
        super(cause);
    }

    public UnsupportedAppException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
