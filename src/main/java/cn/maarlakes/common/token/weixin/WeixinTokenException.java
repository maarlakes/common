package cn.maarlakes.common.token.weixin;

import cn.maarlakes.common.token.TokenException;
import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public class WeixinTokenException extends TokenException {
    private static final long serialVersionUID = 770687661039204148L;

    private final Integer errorCode;
    private final String errorMessage;

    public WeixinTokenException(Integer errorCode, String errorMessage) {
        super(String.format("%s: %s", errorCode, errorMessage));
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @Nonnull
    public Integer getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
