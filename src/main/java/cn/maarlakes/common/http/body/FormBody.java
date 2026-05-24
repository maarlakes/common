package cn.maarlakes.common.http.body;

import cn.maarlakes.common.http.NameValuePair;
import cn.maarlakes.common.http.RequestBody;

import java.util.Collection;

/**
 * 表单类型的 HTTP 请求体，内容为键值对集合。
 *
 * <p>定义了 {@code application/x-www-form-urlencoded} 风格的请求体契约，
 * 泛型参数为 {@link NameValuePair} 的集合。主要实现类为
 * {@link UrlEncodedFormEntityBody}，负责将键值对编码为 URL 编码格式。</p>
 *
 * @author linjpxc
 */
public interface FormBody extends RequestBody<Collection<? extends NameValuePair>> {
}
