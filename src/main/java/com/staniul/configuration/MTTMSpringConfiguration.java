package com.staniul.configuration;

import com.staniul.query.Query;
import org.apache.commons.configuration2.XMLConfiguration;
import org.reflections.Reflections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration class for Spring
 */
@EnableAspectJAutoProxy
@Configuration
@ComponentScan("com.staniul")
public class MTTMSpringConfiguration {
    @Bean
    public Reflections reflections () {
        return Reflections.collect();
    }

    @Bean
    public Query query () throws Exception {
        XMLConfiguration queryConfiguration = ConfigurationLoader.load(Query.class);
        Query query = new Query(queryConfiguration);
 //       query.connect();
        return query;
    }
}
