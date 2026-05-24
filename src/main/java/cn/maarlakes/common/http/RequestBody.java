package cn.maarlakes.common.http;

import cn.maarlakes.common.http.body.ContentBody;

/**
 * HTTP 请求体，继承 {@link cn.maarlakes.common.http.body.ContentBody} 的内容读写能力。
 *
 * <p>泛型参数 {@code T} 表示原始内容类型（如 {@code String}、{@code byte[]}、{@code File}），
 * 供底层实现在构建原生请求时使用。调用方通常通过
 * {@link Request.Builder#body}、{@link Request.Builder#json}、
 * {@link Request.Builder#text} 等便捷方法设置请求体，无需直接操作此接口。
 *
 * @author linjpxc
 */
public interface RequestBody<T> extends ContentBody<T> {
}
