package com.staniul;

import com.staniul.xmlconfig.di.ConfigInjectionPostProcessor;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.context.annotation.*;

/**
 * Configuration class for Spring
 */
@EnableAspectJAutoProxy
@Configuration
@ComponentScan("com.staniul")
@Import(ConfigInjectionPostProcessor.class)
public class MTTMSpringConfiguration {
    @Bean
    public Reflections reflections() {
        return new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("com.staniul"))
                .setScanners(new SubTypesScanner(),
                        new MethodAnnotationsScanner(),
                        new TypeAnnotationsScanner()));
    }
}
