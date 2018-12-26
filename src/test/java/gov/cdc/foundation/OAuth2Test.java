package gov.cdc.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import gov.cdc.helper.RequestHelper;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { 
		"logging.fluentd.host=fluentd", 
		"logging.fluentd.port=24224", 
		"proxy.hostname=",
		"security.oauth2.resource.user-info-uri=http://fdns-ms-stubbing:3002/oauth2/token/introspect",
		"security.oauth2.protected=/api/1.0/**",
		"security.oauth2.client.client-id=test",
		"security.oauth2.client.client-secret=testsecret",
		"ssl.verifying.disable=false" })
public class OAuth2Test {

	@Autowired
	private TestRestTemplate restTemplate;
	private String baseUrlPath = "/api/1.0/";
	private String testToken = "Bearer testtoken";

	@Before
	public void setup() throws Exception {
		// Define the object URL
		System.setProperty("OBJECT_URL", "http://fdns-ms-stubbing:3002/object");
	}
	
	@Test
	public void testUnauthenticated() {
		ResponseEntity<String> response = this.restTemplate.getForEntity(baseUrlPath + "generate", String.class);
		assertThat(response.getStatusCodeValue()).isEqualTo(401);
	}

	@Test
	public void testAthenticatedSpecific() throws Exception {
		setScope("fdns.hl7-utils fdns.hl7-utils.myprofile.read");

		ResponseEntity<String> response = this.restTemplate.exchange(
			baseUrlPath + "json/myprofile",
			HttpMethod.POST,
			getHL7Entity("01.txt"),
			String.class
		);

		assertThat(response.getStatusCodeValue()).isEqualTo(200);
	}

	@Test
	public void testAthenticatedWildcard() throws Exception {
		setScope("fdns.hl7-utils fdns.hl7-utils.*.*");

		ResponseEntity<String> response = this.restTemplate.exchange(
			baseUrlPath + "json/myprofile",
			HttpMethod.POST,
			getHL7Entity("01.txt"),
			String.class
		);

		assertThat(response.getStatusCodeValue()).isEqualTo(200);
	}

	private void setScope(String scope) {
		MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
		data.add("scope", scope);
		RequestHelper.getInstance().executePost("http://fdns-ms-stubbing:3002/oauth2/mock", data);
	}

	private InputStream getResource(String path) throws IOException {
		return getClass().getClassLoader().getResourceAsStream(path);
	}

	private String getResourceAsString(String path) throws IOException {
		return IOUtils.toString(getResource(path));
	}

	private HttpEntity<String> getEntity(String content, MediaType mediaType) throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", testToken);
		headers.setContentType(mediaType);
		HttpEntity<String> entity = new HttpEntity<String>(content, headers);
		return entity;
	}

	private String getHL7(String filename) throws IOException {
		return getResourceAsString("junit/hl7/" + filename);
	}

	private HttpEntity<String> getHL7Entity(String filename) throws IOException {
		return getEntity(getHL7(filename), MediaType.TEXT_PLAIN);
	}
}
