package com.staniul.xmlconfig.di;

import com.staniul.util.reflection.ReflectionUtil;
import com.staniul.xmlconfig.ConfigurationLoader;
import com.staniul.xmlconfig.CustomXMLConfiguration;
import com.staniul.xmlconfig.annotations.ConfigValue;
import com.staniul.xmlconfig.annotations.UseConfig;
import com.staniul.xmlconfig.annotations.WireConfig;
import com.staniul.xmlconfig.convert.NullTypeConverter;
import com.staniul.xmlconfig.convert.StringToTypeConverterFactory;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.Set;

@Configuration
public class ConfigInjectionPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
        Class<?> oClass = o.getClass();
        if (oClass.isAnnotationPresent(UseConfig.class)) {
            try {
                CustomXMLConfiguration configuration = ConfigurationLoader.load(oClass);
                Set<Field> fields = ReflectionUtil.getFieldsAnnotatedWith(oClass, WireConfig.class);
                fields.forEach(field -> ReflectionUtil.setField(field, o, configuration));
                injectValueFields (oClass, o, configuration);
            } catch (ConfigurationException e) {
                throw new BeansXMLConfigurationException(e);
            }
        }

        return o;
    }

    private void injectValueFields(Class<?> oClass, Object o, CustomXMLConfiguration config) {
        StringToTypeConverterFactory converterFactory = new StringToTypeConverterFactory();
        Set<Field> fields = ReflectionUtil.getFieldsAnnotatedWith(oClass, ConfigValue.class);
        for (Field field : fields) {
            ConfigValue ann = field.getAnnotation(ConfigValue.class);
            String entry = config.getString(ann.value());
            Class<?> type = field.getType();

            if (!NullTypeConverter.class.equals(ann.converter())) {
                try {
                    converterFactory.add(type, ann.converter().newInstance());
                } catch (IllegalAccessException | InstantiationException e) {
                    // Do nothing.
                }
            }

            Object fieldValue = converterFactory.convert(type, entry);

            ReflectionUtil.setField(field, o, fieldValue);
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object o, String s) throws BeansException {
        return o;
    }
}
