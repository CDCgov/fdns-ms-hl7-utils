package gov.cdc.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { 
		"logging.fluentd.host=fluentd", 
		"logging.fluentd.port=24224", 
		"proxy.hostname=localhost",
		"security.oauth2.resource.user-info-uri=",
		"security.oauth2.protected=/**",
		"security.oauth2.client.client-id=",
		"security.oauth2.client.client-secret=",
		"ssl.verifying.disable=false" })
public class OAuth2Test {

	@Autowired
	private TestRestTemplate restTemplate;
	private String baseUrlPath = "/api/1.0/";

	@Test
	public void indexPage() {
		ResponseEntity<String> response = this.restTemplate.getForEntity(baseUrlPath, String.class);
		assertThat(response.getStatusCodeValue()).isEqualTo(200);
	}
	
	@Test
	public void generatePage() {
		ResponseEntity<String> response = this.restTemplate.getForEntity(baseUrlPath + "generate", String.class);
		assertThat(response.getStatusCodeValue()).isEqualTo(401);
	}
}
