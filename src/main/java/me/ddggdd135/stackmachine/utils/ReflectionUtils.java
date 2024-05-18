package me.ddggdd135.stackmachine.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReflectionUtils {
    public static <T> T getField(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field[] fields = getAllFields(object);
        Field field = null;
        for (Field f : fields) {
            if (f.getName().equals(fieldName)) {
                field = f;
                break;
            }
        }
        if (field == null) throw new NoSuchFieldException(fieldName);
        field.setAccessible(true);
        return (T) field.get(object);
    }

    public static <T> void setField(Object object, String fieldName, T value)
            throws NoSuchFieldException, IllegalAccessException {
        Field[] fields = getAllFields(object);
        Field field = null;
        for (Field f : fields) {
            if (f.getName().equals(fieldName)) {
                field = f;
                break;
            }
        }
        if (field == null) throw new NoSuchFieldException(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }

    public static Field[] getAllFields(Object object) {
        Class<?> clazz = object.getClass();
        List<Field> fieldList = new ArrayList<>();
        while (clazz != null) {
            fieldList.addAll(new ArrayList<>(Arrays.asList(clazz.getDeclaredFields())));
            clazz = clazz.getSuperclass();
        }
        Field[] fields = new Field[fieldList.size()];
        fieldList.toArray(fields);
        return fields;
    }
}
