package cn.maarlakes.common.token.access;

import cn.maarlakes.common.token.TokenFactory;

/**
 * Access Token 工厂接口，泛型特化为 {@link AccessToken} 类型。
 *
 * <p>用于创建 {@link AccessToken} 实例，通常由 {@link DefaultAccessTokenFactory}
 * 实现多 Provider 路由逻辑。
 *
 * @author linjpxc
 */
public interface AccessTokenFactory extends TokenFactory<AccessToken, AppId, String> {
}
