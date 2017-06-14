package com.staniul.xmlconfig;

import com.staniul.util.ReflectionUtil;
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
        if (oClass.isAnnotationPresent(ConfigFile.class)) {
            try {
                CustomXMLConfiguration configuration = ConfigurationLoader.load(oClass);
                Set<Field> fields = ReflectionUtil.getFieldsAnnotatedWith(oClass, WireConfig.class);
                fields.forEach(field -> ReflectionUtil.setField(field, o, configuration));
            } catch (ConfigurationException e) {
                throw new BeansXMLConfigurationException(e);
            }
        }

        return o;
    }

    @Override
    public Object postProcessAfterInitialization(Object o, String s) throws BeansException {
        return o;
    }
}
