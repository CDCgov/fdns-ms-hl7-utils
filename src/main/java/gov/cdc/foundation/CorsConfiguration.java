package gov.cdc.foundation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class CorsConfiguration {

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurerAdapter() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**").allowedOrigins("*");
			}
		};
	}
	
	@Bean
	public CorsFilter corsFilter() {
		// Required after the update to the newest Springfox version that don't include it by default
	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

	    // Allow anyone and anything access. Probably ok for Swagger spec
	    org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
	    config.setAllowCredentials(true);
	    config.addAllowedOrigin("*");
	    config.addAllowedHeader("*");
	    config.addAllowedMethod("*");

	    source.registerCorsConfiguration("/v2/api-docs", config);
	    return new CorsFilter(source);
	}
}