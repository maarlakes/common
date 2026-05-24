package cn.maarlakes.common.http.proxy;

import java.io.Serializable;

/**
 * 代理认证信息的标记接口，所有代理认证凭证类型均实现此接口。
 *
 * <p>作为各 HTTP 客户端后端（Apache、OkHttp、JDK、AsyncHttpClient）代理认证的统一抽象。
 * 具体实现包括 {@link BasicAuthentication}（Basic 认证）和 {@link DigestAuthentication}（Digest 认证）。
 * 各后端的 {@code ProxyAuthenticator} 通过 {@code instanceof} 判断具体的认证类型并执行相应的认证逻辑。</p>
 *
 * @author linjpxc
 */
public interface ProxyAuthentication extends Serializable {
}
