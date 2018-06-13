package gov.cdc.foundation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
	
	@Value("${proxy.hostname}")
	private String proxy;
	
	@Value("${version}")
	private String version;
	
	@Value("${build.date}")
	private String buildDate;
	
	@Bean
	public Docket api() {
		Docket d = new Docket(DocumentationType.SWAGGER_2).select()
				.apis(RequestHandlerSelectors.basePackage("gov.cdc.foundation.controller")).paths(PathSelectors.any())
				.build().apiInfo(apiInfo());
		if (proxy != null)
			d.host(proxy);
		return d;
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder().title("FDNS HL7 Utilities Microservice")
				.description("This is the repository with the HL7 utilities service to parse, validate and generate sample HL7 data.")
				.version(version + " / " + buildDate).build();
	}
}