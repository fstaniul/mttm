package com.staniul.util.json;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Otjs = Object to JSON String
 */
public class Otjs {
    public static String format (Object o) {
        Class<?> oClass = o.getClass();
        Field[] fields = oClass.getDeclaredFields();
        StringBuilder sb = new StringBuilder("{");
        for (Field field : fields) {
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            sb.append("\n\t\"").append(field.getName()).append("\": ");

            try {
                Object cof = field.get(o);

                if (cof instanceof Collection) {
                    sb.append("[\n");
                    for (Object oic : (Collection) cof) {
                        sb.append("\t\t\"").append(oic).append("\"").append(",\n");
                    }

                    if (((Collection) cof).size() > 0) sb.delete(sb.length() - ",\n".length(), sb.length());
                    sb.append("\n\t],");
                }

                else {
                    sb.append("\"").append(cof).append("\",");
                }

            } catch (IllegalAccessException ignore) {
            }

            field.setAccessible(accessible);
        }

        sb.append("\n}");

        return sb.toString();
    }
}
