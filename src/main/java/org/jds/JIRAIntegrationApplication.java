package org.jds;

import javax.management.relation.RoleNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
/**
 * @author J. Daniel Sobrado
 * @version 1.0
 * @since 2022-05-20
 */
@SpringBootApplication(scanBasePackages = {"org.jds", "springboot.rest"})
public class JIRAIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(JIRAIntegrationApplication.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.run(args);
    }

}
