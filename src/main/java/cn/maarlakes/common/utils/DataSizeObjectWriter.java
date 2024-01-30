package cn.maarlakes.common.utils;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;

import java.lang.reflect.Type;

/**
 * @author linjpxc
 */
class DataSizeObjectWriter implements ObjectWriter<DataSize> {
    @Override
    public void write(JSONWriter writer, Object object, Object fieldName, Type fieldType, long features) {
        if (object == null) {
            writer.writeNull();
        } else {
            writer.writeString(object.toString());
        }
    }
}
