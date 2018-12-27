package gov.cdc.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { 
		"logging.fluentd.host=fluentd", 
		"logging.fluentd.port=24224", 
		"proxy.hostname=",
		"security.oauth2.resource.user-info-uri=",
		"security.oauth2.protected=",
		"security.oauth2.client.client-id=",
		"security.oauth2.client.client-secret=",
		"ssl.verifying.disable=false" })
public class RulesValidationTests {

	@Autowired
	private TestRestTemplate restTemplate;
	private JacksonTester<JsonNode> json;
	private String baseUrlPath = "/api/1.0/";
	private String profile;

	@Before
	public void setup() {
		ObjectMapper objectMapper = new ObjectMapper();
		JacksonTester.initFields(this, objectMapper);
		
		// Define the object URL
		System.setProperty("OBJECT_URL", "http://fdns-ms-stubbing:3002/object");
	}

	@Test
	public void transformHL7toJSONWithProfile() throws Exception {
		
		// First create and update rules
		createAndUpdateRules();
		
		// Check a message without PII
		ResponseEntity<JsonNode> response = restTemplate.exchange(
				baseUrlPath + "/json/", 
				HttpMethod.POST, 
				getHL7Entity("01.txt"), 
				JsonNode.class,
				profile);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).hasJsonPathValue("@.extractor");
		assertThat(body).hasJsonPathValue("@.message.HL7.source");
		assertThat(body).extractingJsonPathStringValue("@.message.HL7.version").isEqualTo("2.3");
		assertThat(body).hasJsonPathValue("@.message.HL7.source.ORU_R01.MSH");
		assertThat(body).extractingJsonPathStringValue("@.message.HL7.source.ORU_R01.MSH.MSH-10").isEqualTo("Q335939501T337311002");
		assertThat(body).extractingJsonPathStringValue("@.message.HL7.source.ORU_R01.MSH.MSH-12").isEqualTo("2.3");
		assertThat(body).extractingJsonPathStringValue("@.message.HL7.source.ORU_R01.MSH.MSH-9.CM_MSG-1").isEqualTo("ORU");
		assertThat(body).extractingJsonPathStringValue("@.message.HL7.source.ORU_R01.MSH.MSH-9.CM_MSG-2").isEqualTo("R01");
		
		// Check a message with PII
		response = restTemplate.exchange(
				baseUrlPath + "/json/{profile}", 
				HttpMethod.POST, 
				getHL7Entity("02.txt"), 
				JsonNode.class,
				profile);
		body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(body).hasJsonPathValue("@.success");
		assertThat(body).extractingJsonPathBooleanValue("@.success").isEqualTo(false);
		assertThat(body).hasJsonPathValue("@.profile");
		assertThat(body).extractingJsonPathStringValue("@.profile").isEqualTo(profile);
		
		// Check a message with PII but with an unknown profile
		response = restTemplate.exchange(
				baseUrlPath + "/json/{profile}", 
				HttpMethod.POST, 
				getHL7Entity("02.txt"), 
				JsonNode.class,
				"__unknown__");
		body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).hasJsonPathValue("@.PII");
		assertThat(body).extractingJsonPathBooleanValue("@.PII.foundProfile").isEqualTo(false);
		assertThat(body).extractingJsonPathBooleanValue("@.PII.checked").isEqualTo(true);
	}
	
	@Test
	public void validate() throws Exception {
		
		// First create and update rules
		createAndUpdateRules();
		
		// Check a message without PII
		ResponseEntity<JsonNode> response = restTemplate.exchange(
				baseUrlPath + "/rules/validate/{profile}", 
				HttpMethod.POST, 
				getHL7Entity("01.txt"), 
				JsonNode.class,
				profile);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).extractingJsonPathBooleanValue("@.valid").isEqualTo(true);
		assertThat(body).extractingJsonPathNumberValue("@.warnings").isEqualTo(0);
		assertThat(body).extractingJsonPathNumberValue("@.errors").isEqualTo(0);
		assertThat(body).extractingJsonPathBooleanValue("@.status.PII.checked").isEqualTo(false);
		assertThat(body).extractingJsonPathBooleanValue("@.status.warning.checked").isEqualTo(true);
		assertThat(body).extractingJsonPathBooleanValue("@.status.error.checked").isEqualTo(true);
		
		// Check a message without PII but ask to check them
		response = restTemplate.exchange(
				baseUrlPath + "/rules/validate/{profile}?checkPII=true", 
				HttpMethod.POST, 
				getHL7Entity("01.txt"), 
				JsonNode.class,
				profile);
		body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).extractingJsonPathBooleanValue("@.valid").isEqualTo(true);
		assertThat(body).extractingJsonPathNumberValue("@.warnings").isEqualTo(0);
		assertThat(body).extractingJsonPathNumberValue("@.errors").isEqualTo(0);
		assertThat(body).extractingJsonPathBooleanValue("@.status.PII.checked").isEqualTo(true);
		assertThat(body).extractingJsonPathBooleanValue("@.status.warning.checked").isEqualTo(true);
		assertThat(body).extractingJsonPathBooleanValue("@.status.error.checked").isEqualTo(true);
		
		// Check a message with PII
		response = restTemplate.exchange(
				baseUrlPath + "/rules/validate/{profile}", 
				HttpMethod.POST, 
				getHL7Entity("02.txt"), 
				JsonNode.class,
				profile);
		body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).extractingJsonPathBooleanValue("@.valid").isEqualTo(true);
		assertThat(body).extractingJsonPathNumberValue("@.warnings").isEqualTo(1);
		assertThat(body).extractingJsonPathNumberValue("@.errors").isEqualTo(0);
		assertThat(body).extractingJsonPathBooleanValue("@.status.PII.checked").isEqualTo(false);
		assertThat(body).extractingJsonPathBooleanValue("@.status.warning.checked").isEqualTo(true);
		assertThat(body).extractingJsonPathBooleanValue("@.status.error.checked").isEqualTo(true);
		
		// Check a message with PII but ask to check them
		response = restTemplate.exchange(
				baseUrlPath + "/rules/validate/{profile}?checkPII=true", 
				HttpMethod.POST, 
				getHL7Entity("02.txt"), 
				JsonNode.class,
				profile);
		body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(body).extractingJsonPathBooleanValue("@.success").isEqualTo(false);
		assertThat(body).extractingJsonPathStringValue("@.message").contains(profile);
		
		// Check a message with explain mode
		response = restTemplate.exchange(
				baseUrlPath + "/rules/validate/{profile}?explain=true", 
				HttpMethod.POST, 
				getHL7Entity("02.txt"), 
				JsonNode.class,
				profile);
		body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).extractingJsonPathBooleanValue("@.valid").isEqualTo(true);
		assertThat(body).extractingJsonPathNumberValue("@.warnings").isEqualTo(1);
		assertThat(body).extractingJsonPathNumberValue("@.errors").isEqualTo(0);
		assertThat(body).extractingJsonPathBooleanValue("@.status.PII.checked").isEqualTo(false);
		assertThat(body).extractingJsonPathBooleanValue("@.status.warning.checked").isEqualTo(true);
		assertThat(body).extractingJsonPathBooleanValue("@.status.error.checked").isEqualTo(true);
		assertThat(body).hasJsonPathValue("@.details");
	}


	@Test
	public void getRulesSchema() throws Exception {
		ResponseEntity<JsonNode> response = restTemplate.exchange(
				baseUrlPath + "/rules/schema", 
				HttpMethod.GET, 
				null, 
				JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void checkRules() throws Exception {
		ResponseEntity<JsonNode> response = restTemplate.exchange(
				baseUrlPath + "/rules/check", 
				HttpMethod.POST, 
				getEntity(getResourceAsString("junit/profiles/test.json"), MediaType.APPLICATION_JSON),
				JsonNode.class);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).hasJsonPathValue("@.valid");
		assertThat(body).extractingJsonPathBooleanValue("@.valid").isEqualTo(true);
		assertThat(body).hasJsonPathValue("@.details");
	}
	
	@Test
	public void createAndUpdateRules() throws Exception {
		
		int nbOfCalls = 2;
		profile = UUID.randomUUID().toString();
		
		for (int i = 0; i < nbOfCalls; i++) {
			ResponseEntity<JsonNode> response = restTemplate.exchange(
					baseUrlPath + "/rules/{profile}", 
					HttpMethod.POST, 
					getEntity(getResourceAsString("junit/profiles/test.json"), MediaType.APPLICATION_JSON),
					JsonNode.class,
					profile);
			JsonContent<JsonNode> body = this.json.write(response.getBody());
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(body).hasJsonPathValue("@.success");
			assertThat(body).extractingJsonPathBooleanValue("@.success").isEqualTo(true);
			assertThat(body).hasJsonPathValue("@.profile");
			assertThat(body).extractingJsonPathStringValue("@.profile").isEqualTo(profile);
		}
		
	}
	
	@Test
	public void getRuleSet() throws Exception {
		
		// Be sure that we create the profile
		createAndUpdateRules();
		
		String[] sets = {"pii", "error", "warning"};
		
		for (String ruleSet : sets) {
			ResponseEntity<JsonNode> response = restTemplate.exchange(
					baseUrlPath + "/rules/{profile}/{ruleSet}", 
					HttpMethod.GET, 
					null,
					JsonNode.class,
					profile,
					ruleSet);
			JsonContent<JsonNode> body = this.json.write(response.getBody());
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(body).hasJsonPathValue("@._id");
		}
	}

	private InputStream getResource(String path) throws IOException {
		return getClass().getClassLoader().getResourceAsStream(path);
	}

	private String getResourceAsString(String path) throws IOException {
		return IOUtils.toString(getResource(path));
	}

	private HttpEntity<String> getEntity(String content, MediaType mediaType) throws IOException {
		HttpHeaders headers = new HttpHeaders();
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
