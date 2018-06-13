package gov.cdc.foundation.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component("authz")
@Configuration
public class Authz {

	@Value("${security.oauth2.protected}")
	private String protectedURIs;

	public boolean isSecured() {
		return protectedURIs != null && protectedURIs.length() > 0;
	}

}
