package se.larsson.springcloud.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeExchange()
                .pathMatchers("/headerrouting/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/oauth2/**").permitAll()
                .pathMatchers("/login/**").permitAll()
                .pathMatchers("/error/**").permitAll()
                .pathMatchers("/openapi/**").permitAll()
                .pathMatchers("/webjars/**").permitAll()
                .pathMatchers("/config/**").permitAll()
                .anyExchange().authenticated()
                .and()
                .oauth2ResourceServer()
                .jwt();
        return http.build();
    }
}
