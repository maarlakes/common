package cn.maarlakes.common.utils;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;

import java.lang.reflect.Type;

/**
 * @author linjpxc
 */
class DataSizeObjectReader implements ObjectReader<DataSize> {
    @Override
    public DataSize readObject(JSONReader reader, Type fieldType, Object fieldName, long features) {
        if (reader.nextIfNull()) {
            return null;
        }
        final String text = reader.readString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return DataSize.parse(text);
    }
}
