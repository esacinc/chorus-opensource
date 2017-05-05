package com.infoclinika.mssharing.skyline.web.test.help;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * @author timofey.kasyanov
 *         date: 15.05.2014
 */
@Configuration
@ComponentScan(basePackages = "com.infoclinika.mssharing.skyline.web.test")
public class SpringConfig {

    @Bean
    public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer(){
        final PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocation(
                new ClassPathResource("test-app.properties")
        );
        return configurer;
    }

}
