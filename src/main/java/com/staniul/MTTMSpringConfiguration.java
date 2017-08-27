package com.staniul;

import com.staniul.xmlconfig.di.ConfigInjectionPostProcessor;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Configuration class for Spring
 */
@EnableAspectJAutoProxy
@Configuration
@ComponentScan("com.staniul")
@Import({ConfigInjectionPostProcessor.class})
public class MTTMSpringConfiguration {
    @Bean
    public Reflections reflections() {
        return new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("com.staniul"))
                .setScanners(new SubTypesScanner(),
                        new MethodAnnotationsScanner(),
                        new TypeAnnotationsScanner()));
    }

    @Primary
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:mysql://localhost:3306/teamspeak", "teamspeak", "ts20122012");
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
