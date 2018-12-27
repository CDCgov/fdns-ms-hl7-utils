package gov.cdc.foundation.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import gov.cdc.security.SSLCertificateValidation;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class OAuth2Configuration extends ResourceServerConfigurerAdapter {

	@Value("${security.oauth2.protected}")
	private String protectedURIs;
	@Value("${security.oauth2.resource.user-info-uri}")
	private String userInfoUri;
	@Value("${security.oauth2.client.client-id}")
	private String clientId;
	@Value("${security.oauth2.client.client-secret}")
	private String clientSecret;
	@Value("${ssl.verifying.disable}")
	private boolean disableSSL;
	
	@Override
	public void configure(HttpSecurity http) throws Exception {
		if (protectedURIs.length() > 0)
			http
				.authorizeRequests()
				.antMatchers(HttpMethod.OPTIONS).permitAll()
				.antMatchers("/api/1.0/").permitAll()
				.antMatchers(protectedURIs).access("#oauth2.hasScope('fdns.hl7-utils')")
				.anyRequest().permitAll();
		else
			http
				.authorizeRequests()
				.anyRequest().permitAll();

		if (disableSSL)
			SSLCertificateValidation.disable();
	}

	@Bean
	public ResourceServerTokenServices userInfoTokenServices() {
		RemoteTokenServices tokenServices = new RemoteTokenServices();
		tokenServices.setCheckTokenEndpointUrl(userInfoUri);
		tokenServices.setClientId(clientId);
		tokenServices.setClientSecret(clientSecret);
		return tokenServices;
	}

	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		resources.resourceId(clientId);
	}

}
