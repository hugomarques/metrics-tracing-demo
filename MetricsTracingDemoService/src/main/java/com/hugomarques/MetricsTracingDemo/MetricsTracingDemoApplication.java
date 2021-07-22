package com.hugomarques.MetricsTracingDemo;

import static org.springframework.web.reactive.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

import java.util.Map;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsContributor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.SpanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.Tags;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class MetricsTracingDemoApplication {

    @Bean
    WebFluxTagsContributor consoleTagContributor() {
        return (exchange, ex) -> {
            var console = "UNKNOWN";
            var consolePathVariable = ((Map<String,String>) exchange.getAttribute(
                    URI_TEMPLATE_VARIABLES_ATTRIBUTE)).get("console");
            if (AvailabilityController.validate(consolePathVariable)) {
                console = consolePathVariable;
            }
            log.error("Logging tag: " + console);
            return Tags.of("console", console);
        };
    }

    public static void main(String[] args) {
        log.info("Starting server");
        SpringApplication.run(MetricsTracingDemoApplication.class, args);
    }

}

@AllArgsConstructor
@RestController
class AvailabilityController {

    private final SpanCustomizer spanCustomizer;

    @GetMapping("/availability/{console}")
    Map<String, Object> getAvailability(@PathVariable final String console) {
        return Map.of("console", console,
                      "available", checkAvailability(console));
    }

    private boolean checkAvailability(String console) {
        Assert.state(validate(console), () -> "the console specified, " + console + ", is not valid.");
        this.spanCustomizer.tag("console", console);
        switch (console) {
            case "ps5": throw new RuntimeException("Service exception");
            case "xbox":  return true;
            default: return false;
        }
    }

    public static boolean validate(String console) {
        return StringUtils.hasText(console) &&
                Set.of("ps5", "ps4", "switch", "xbox").contains(console);
    }
}
