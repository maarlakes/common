package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletionStage;

/**
 * Token 工厂接口，负责根据应用标识（appId）异步创建对应的 Token。
 *
 * <p>泛型参数说明：
 * <ul>
 *   <li>{@code T} — Token 类型，必须继承 {@link AppToken}</li>
 *   <li>{@code A} — 应用标识类型</li>
 *   <li>{@code V} — Token 值类型</li>
 * </ul>
 *
 * <p>实现类通常封装了与外部服务（如微信 API）的交互逻辑，通过 HTTP 请求获取 Token。
 *
 * @author linjpxc
 */
public interface TokenFactory<T extends AppToken<A, V>, A, V> {

    /**
     * 为指定应用异步创建 Token。
     *
     * @param appId 应用标识，用于确定向哪个服务请求 Token
     * @return 异步返回创建好的 Token
     */
    @Nonnull
    CompletionStage<T> createToken(@Nonnull A appId);
}
