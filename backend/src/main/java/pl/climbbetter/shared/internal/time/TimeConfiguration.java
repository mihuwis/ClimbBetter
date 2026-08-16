package pl.climbbetter.shared.internal.time;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TimeConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
    
}
