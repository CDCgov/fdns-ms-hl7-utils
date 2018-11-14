package gov.cdc.foundation;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger.web.SecurityConfigurationBuilder;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

	@Value("${proxy.hostname}")
	private String proxy;

	@Value("${security.oauth2.protected}")
	private String protectedURIs;

	@Value("${version}")
	private String version;

	@Value("${build.date}")
	private String buildDate;

	@Bean
	public Docket api() {
		Docket d = new Docket(DocumentationType.SWAGGER_2).select()
				.apis(RequestHandlerSelectors.basePackage("gov.cdc.foundation.controller")).paths(PathSelectors.any())
				.build()
				.apiInfo(apiInfo())
				.securitySchemes(Arrays.asList(apiKey()))
				.securityContexts(Arrays.asList(securityContext()));
		if (proxy != null)
			d.host(proxy);
		return d;
	}

	@Bean
	public SecurityConfiguration security() {
		return SecurityConfigurationBuilder.builder().scopeSeparator(",")
				.additionalQueryStringParams(null)
				.useBasicAuthenticationWithAccessCodeGrant(false).build();
	}
 	private SecurityContext securityContext() {
		return SecurityContext.builder().securityReferences(defaultAuth()).forPaths(PathSelectors.ant(protectedURIs)).build();
	}
 	private List<SecurityReference> defaultAuth() {
		AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
		AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
		authorizationScopes[0] = authorizationScope;
		return Arrays.asList(new SecurityReference("apiKey", authorizationScopes));
	}
 	private ApiKey apiKey() {
		return new ApiKey("apiKey", "Authorization", "header");
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder().title("FDNS HL7 Utilities Microservice")
				.description("This is the repository with the HL7 utilities service to parse, validate and generate sample HL7 data.")
				.version(version + " / " + buildDate).build();
	}
}