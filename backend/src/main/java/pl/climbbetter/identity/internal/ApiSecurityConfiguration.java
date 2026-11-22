package pl.climbbetter.identity.internal;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import jakarta.servlet.DispatcherType;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class ApiSecurityConfiguration {
    @Bean
    AuthenticationManager authenticationManager() {
        return authentication -> { throw new BadCredentialsException("Use Firebase authentication."); };
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, Environment environment,
            ObjectProvider<FirebaseTokenVerifier> verifier, ObjectProvider<IdentityAccountService> accounts) throws Exception {
        AuthenticationMode mode = AuthenticationMode.from(environment);
        http.cors(Customizer.withDefaults()).csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> IdentityErrorWriter.write(response,
                                new IdentityAccessException(401, "AUTH_REQUIRED", "Please sign in.")))
                        .accessDeniedHandler((request, response, exception) -> IdentityErrorWriter.write(response,
                                new IdentityAccessException(403, "ACCESS_DENIED", "Access denied."))));
        if (mode != AuthenticationMode.STUB) {
            http.addFilterBefore(new FirebaseAuthenticationFilter(verifier.getObject(), accounts.getObject()),
                    UsernamePasswordAuthenticationFilter.class);
        }
        http.authorizeHttpRequests(rules -> {
            rules.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
            rules.requestMatchers("/actuator/health").permitAll();
            if (environment.acceptsProfiles(Profiles.of("local", "test"))) {
                rules.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
            }
            if (mode == AuthenticationMode.STUB) rules.requestMatchers("/api/v1/**").permitAll();
            else rules.requestMatchers("/api/v1/**").authenticated();
            rules.anyRequest().denyAll();
        });
        return http.build();
    }
}
