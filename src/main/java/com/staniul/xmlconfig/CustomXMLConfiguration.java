package com.staniul.xmlconfig;

import com.staniul.util.ReflectionUtil;
import com.staniul.util.StringToTypeConverterFactory;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Extended basic functionality of XMLConfiguration class.
 */
public class CustomXMLConfiguration extends XMLConfiguration {
    private static Logger log = Logger.getLogger(CustomXMLConfiguration.class);

    CustomXMLConfiguration(HierarchicalConfiguration<ImmutableNode> c) {
        super(c);
    }

    public <T> T getClass (Class<T> tClass) {
        return getClass(tClass, "");
    }

    public <T> T getClass (Class<T> tClass, String prefix) {
        return getClass(tClass, prefix, null);
    }

    public <T> T getClass (Class<T> tClass, String prefix, StringToTypeConverterFactory typeConverter) {
        return getClasses(tClass, prefix, typeConverter).get(0);
    }

    public <T> List<T> getClasses (Class<T> tClass) {
        return getClasses(tClass, "");
    }

    public <T> List<T> getClasses (Class<T> tClass, String prefix) {
        return getClasses(tClass, prefix, null);
    }

    public <T> List<T> getClasses (Class<T> tClass, String prefix, StringToTypeConverterFactory typeConverter) {
        if (tClass.isAnnotationPresent(CustomConfigLoad.class)) {
            return internalCustomGetClass(tClass, prefix, typeConverter, tClass.getAnnotation(CustomConfigLoad.class));
        }

        return internalGetClass(tClass, prefix, typeConverter);
    }

    private <T> List<T> internalCustomGetClass(Class<T> tClass, String prefix, StringToTypeConverterFactory typeConverter, CustomConfigLoad customConfigLoad) {
        String template = ("".equals(prefix) ? "" : (prefix + ".")) + ( customConfigLoad.value().equals("") ? tClass.getSimpleName().toLowerCase() : customConfigLoad.value() ) + "[@%s]";
        Set<Field> fields = ReflectionUtil.getFieldsAnnotatedWith(tClass, ConfigEntry.class);
        List<FieldDataContainer> data = fields.stream().map(f -> new FieldDataContainer(f, f.getAnnotation(ConfigEntry.class).value())).collect(Collectors.toList());
        if(readData(data, template, typeConverter))
            return createObjects(tClass, data);

        return null;
    }

    private <T> List<T> internalGetClass(Class<T> tClass, String prefix, StringToTypeConverterFactory typeConverter) {
        String template = ("".equals(prefix) ? "" : (prefix + ".")) + tClass.getSimpleName().toLowerCase() + "[@%s]";
        Set<Field> fields = ReflectionUtil.getFields(tClass);
        List<FieldDataContainer> data = fields.stream().map(FieldDataContainer::new).collect(Collectors.toList());
        if (readData(data, template, typeConverter))
            return createObjects (tClass, data);

        return null;
    }

    private <T> List<T> createObjects(Class<T> tClass, List<FieldDataContainer> data) {
        Integer[] cc = {Integer.MAX_VALUE}; //Java cheat
        data.forEach(d -> cc[0] = Math.min(cc[0], d.getValues().size()));
        List<T> tList = new ArrayList<>(cc[0]);

        for (int[] i = {0}; i[0] < cc[0]; i[0]++) {
            T t = ReflectionUtil.createDefaultOrNull(tClass);
            assert t != null;
            data.forEach(d -> ReflectionUtil.setField(d.getField(), t, d.getValues().get(i[0])));
            tList.add(t);
        }

        return tList;
    }

    private boolean readData (List<FieldDataContainer> dataContainers, String template, StringToTypeConverterFactory typeConverter) {
        for (FieldDataContainer fdc : dataContainers) {
            List<String> strData = Arrays.stream(getStringArray(String.format(template, fdc.getEntry()))).collect(Collectors.toList());
            if (strData == null) return false;

            Class<?> fieldTypeClass = fdc.getField().getType();

            if (typeConverter != null && (fieldTypeClass.isPrimitive() || fieldTypeClass.equals(String.class))) {
                fdc.setValues(strData.stream().map(s -> typeConverter.convert(fieldTypeClass, s)).collect(Collectors.toList()));
            } else {
                List<?> l;
                if (fieldTypeClass.equals(Integer.TYPE) || fieldTypeClass.equals(Integer.class))
                    l = strData.stream().map(Integer::parseInt).collect(Collectors.toList());
                else if (fieldTypeClass.equals(Long.TYPE) || fieldTypeClass.equals(Long.class))
                    l = strData.stream().map(Boolean::parseBoolean).collect(Collectors.toList());
                else if (fieldTypeClass.equals(Double.TYPE) || fieldTypeClass.equals(Double.class))
                    l = strData.stream().map(Double::parseDouble).collect(Collectors.toList());
                else if (fieldTypeClass.equals(Float.TYPE) || fieldTypeClass.equals(Float.class))
                    l = strData.stream().map(Float::parseFloat).collect(Collectors.toList());
                else if (fieldTypeClass.equals(Long.TYPE) || fieldTypeClass.equals(Long.class))
                    l = strData.stream().map(Long::parseLong).collect(Collectors.toList());
                else l = strData;
                fdc.setValues(l);
            }
        }

        return true;
    }

    public Set<Integer> getIntSet (String key) {
        try {
            return Arrays.stream(getString(key).split(",")).map(Integer::parseInt).collect(Collectors.toSet());
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    public Set<Integer> getIntSet (String key, Set<Integer> defaultValues) {
        try {
            return Arrays.stream(getString(key).split(",")).map(Integer::parseInt).collect(Collectors.toSet());
        } catch (Exception e) {
            return defaultValues;
        }
    }
}
