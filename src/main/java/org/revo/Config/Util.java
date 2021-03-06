package org.revo.Config;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.buf.MessageBytes;
import org.bson.types.ObjectId;
import org.revo.Service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.servlet.ServletException;
import javax.validation.Validator;
import java.io.IOException;

/**
 * Created by ashraf on 17/04/17.
 */
@Configuration
public class Util {
    // copied from spring-cloud/spring-cloud-netflix#1108


    public ValveBase valveBase() {
        return new ValveBase() {
            @Override
            public void invoke(Request request, Response response) throws IOException, ServletException {
                final MessageBytes serverNameMB = request.getCoyoteRequest().serverName();
                String originalServerName = null;
                final String forwardedHost = request.getHeader("X-Forwarded-Host");
                if (forwardedHost != null) {
                    originalServerName = serverNameMB.getString();
                    serverNameMB.setString(forwardedHost);
                }
                try {
                    getNext().invoke(request, response);
                } finally {
                    if (forwardedHost != null) {
                        serverNameMB.setString(originalServerName);
                    }
                }
            }
        };
    }


    @Bean
    @Profile("prod")
    public /*EmbeddedServletContainerCustomizer*/WebServerFactoryCustomizer customizer() {
        return factory -> ((/*TomcatEmbeddedServletContainerFactory*/TomcatServletWebServerFactory) factory).addContextValves(valveBase());
    }

    @Bean
    public PasswordEncoder encoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserService userService) {
        return s -> userService.findByEmail(s).orElseThrow(() -> new UsernameNotFoundException(s));
    }

    @Bean
    CommandLineRunner runner(Env env, UserService userService) {
        return strings -> {
            if (userService.count() == 0) {
                env.getUsers().forEach(user -> {
                    user.setId(new ObjectId(user.getId()).toString());
                    userService.save(user);
                });
            }
        };
    }

    @Bean
    Validator validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    ValidatingMongoEventListener validatingMongoEventListener(Validator validator) {
        return new ValidatingMongoEventListener(validator);
    }

}
