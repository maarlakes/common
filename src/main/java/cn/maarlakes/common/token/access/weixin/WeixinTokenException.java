package cn.maarlakes.common.token.access.weixin;

import cn.maarlakes.common.token.TokenException;
import jakarta.annotation.Nonnull;

/**
 * 微信 API 错误响应异常，封装微信返回的错误码（errcode）和错误消息（errmsg）。
 *
 * <p>微信 Token API 在出错时会返回类似以下的 JSON：
 * <pre>{@code {"errcode":40013,"errmsg":"invalid appid"}}</pre>
 * 此异常将错误码和错误消息提取为结构化字段，便于上层业务根据错误码进行差异化处理。
 *
 * @author linjpxc
 */
public class WeixinTokenException extends TokenException {
    private static final long serialVersionUID = 770687661039204148L;

    /** 微信错误码 */
    private final Integer errorCode;

    /** 微信错误消息 */
    private final String errorMessage;

    /**
     * 使用错误码和错误消息构造。
     *
     * @param errorCode    微信错误码（如 40013）
     * @param errorMessage 微信错误消息（如 "invalid appid"）
     */
    public WeixinTokenException(Integer errorCode, String errorMessage) {
        super(String.format("%s: %s", errorCode, errorMessage));
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * 获取微信错误码。
     *
     * @return 错误码
     */
    @Nonnull
    public Integer getErrorCode() {
        return errorCode;
    }

    /**
     * 获取微信错误消息。
     *
     * @return 错误消息
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
