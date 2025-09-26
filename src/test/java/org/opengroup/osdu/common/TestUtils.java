package org.opengroup.osdu.common;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

public class TestUtils {

    public static void setFieldValue(Object targetObject, String fieldName, Object fieldValue) {
        Field field = ReflectionUtils.findField(targetObject.getClass(), fieldName);
        field.setAccessible(true);
        ReflectionUtils.setField(field, targetObject, fieldValue);
    }

}