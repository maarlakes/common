package cn.maarlakes.common.http.body;

import cn.maarlakes.common.http.NameValuePair;
import cn.maarlakes.common.http.RequestBody;

import java.util.Collection;

/**
 * @author linjpxc
 */
public interface FormBody extends RequestBody<Collection<? extends NameValuePair>> {
}
