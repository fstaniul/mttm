package com.staniul.xmlconfig;

import com.staniul.util.reflection.ReflectionUtil;
import com.staniul.xmlconfig.annotations.ConfigField;
import com.staniul.xmlconfig.convert.NullTypeConverter;
import com.staniul.xmlconfig.convert.StringToTypeConverter;
import com.staniul.xmlconfig.convert.StringToTypeConverterFactory;
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

    public Set<Integer> getIntSet(String key) {
        try {
            return Arrays.stream(getString(key).split(",")).map(Integer::parseInt).collect(Collectors.toSet());
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    public Set<Integer> getIntSet(String key, Set<Integer> defaultValues) {
        try {
            return Arrays.stream(getString(key).split(",")).map(Integer::parseInt).collect(Collectors.toSet());
        } catch (Exception e) {
            return defaultValues;
        }
    }

    public <T> Set<T> getSet(Class<T> tClass, String entry) {
        return getSet(tClass, entry, new StringToTypeConverterFactory());
    }

    public <T> Set<T> getSet(Class<T> tClass, String entry, StringToTypeConverter<T> typeConverter) {
        return getSet(tClass, entry, new StringToTypeConverterFactory().add(tClass, typeConverter));
    }

    private <T> Set<T> getSet(Class<T> tClass, String entry, StringToTypeConverterFactory converterFactory) {
        List<String> strings = Arrays.stream(getString(entry).split(",")).collect(Collectors.toList());
        Set<T> tSet = new HashSet<>();
        strings.forEach(s -> tSet.add(converterFactory.convert(tClass, s)));
        return tSet;
    }

    public <T> T getClass(Class<T> tClass, String entry) {
        return getClass(tClass, entry, new StringToTypeConverterFactory());
    }

    public <T> T getClass(Class<T> tClass, String entry, StringToTypeConverterFactory converterFactory) {
        Set<Field> fields = ReflectionUtil.getFields(tClass);
        Map<Field, String> valueMap = new HashMap<>();
        String template = entry + "[@%s]";

        for (Field field : fields) {
            String fieldName;
            if (field.isAnnotationPresent(ConfigField.class))
                fieldName = field.getAnnotation(ConfigField.class).value();
            else fieldName = field.getName();

            String value = getString(String.format(template, fieldName));
            valueMap.put(field, value);
        }

        try {
            T t = tClass.newInstance();
            for (Field field : fields) {
                if (field.isAnnotationPresent(ConfigField.class))
                    addConverterToFactory(converterFactory, field.getType(), field.getAnnotation(ConfigField.class));

                Object value = converterFactory.convert(field.getType(), valueMap.get(field));
                ReflectionUtil.setField(field, t, value);
            }
            return t;
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    public <T> List<T> getClasses(Class<T> tClass, String entry) {
        return getClasses(tClass, entry, new StringToTypeConverterFactory());
    }

    public <T> List<T> getClasses(Class<T> tClass, String entry, StringToTypeConverterFactory converterFactory) {
        Set<Field> fields = ReflectionUtil.getFields(tClass);
        Map<Field, List<String>> valueMap = new HashMap<>();

        for (Field field : fields) {
            String template = entry + "[@%s]";
            String fieldName;

            if (field.isAnnotationPresent(ConfigField.class)) {
                fieldName = field.getAnnotation(ConfigField.class).value();
            }
            else {
                fieldName = field.getName();
            }

            List<String> list = getList(String.class, String.format(template, fieldName));
            valueMap.put(field, list);
        }

        int count = valueMap.entrySet().stream().filter(e -> e.getValue() != null).mapToInt(e -> e.getValue().size()).min().orElse(-1);

        if (count == -1) return null;

        try {
            List<T> result = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                T t = tClass.newInstance();

                for (Field field : fields) {
                    if (field.isAnnotationPresent(ConfigField.class))
                        addConverterToFactory(converterFactory, field.getType(), field.getAnnotation(ConfigField.class));

                    Object value;
                    if (valueMap.get(field) != null)
                        value = converterFactory.convert(field.getType(), valueMap.get(field).get(i));
                    else value = null;
                    ReflectionUtil.setField(field, t, value);
                }

                result.add(t);
            }

            return result;
        } catch (IllegalAccessException | InstantiationException e) {
            log.error("Failed to instantiate class " + tClass, e);
            return null;
        }
    }

    private void addConverterToFactory(StringToTypeConverterFactory converterFactory, Class<?> type, ConfigField annotation) {
        if (!NullTypeConverter.class.equals(annotation.converter())) {
            try {
                converterFactory.add(type, annotation.converter().newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                // do nothing
            }
        }
    }
}
