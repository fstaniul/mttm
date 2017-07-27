package com.staniul;

import com.staniul.api.config.SpringRESTApiConfiguration;
import com.staniul.xmlconfig.di.ConfigInjectionPostProcessor;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Configuration class for Spring
 */
@EnableAspectJAutoProxy
@Configuration
@ComponentScan("com.staniul")
@Import({ConfigInjectionPostProcessor.class, SpringRESTApiConfiguration.class})
public class MTTMSpringConfiguration {
    @Bean
    public Reflections reflections() {
        return new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("com.staniul"))
                .setScanners(new SubTypesScanner(),
                        new MethodAnnotationsScanner(),
                        new TypeAnnotationsScanner()));
    }

    @Bean
    @Primary
    public DataSource teamspeakDatabase (@Value("${db.username}") String username, @Value("${db.password}") String password) {
        DriverManagerDataSource source = new DriverManagerDataSource("jdbc:mysql://localhost:3306/teamspeak", username, password);
        source.setDriverClassName("com.mysql.jdbc.Driver");
        return source;
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate (DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate (DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
