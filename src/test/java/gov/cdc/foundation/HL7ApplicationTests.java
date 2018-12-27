package gov.cdc.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
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
public class HL7ApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;
	private JacksonTester<JsonNode> json;
	private String baseUrlPath = "/api/1.0/";

	@Before
	public void setup() {
		ObjectMapper objectMapper = new ObjectMapper();
		JacksonTester.initFields(this, objectMapper);
		
		// Define the object URL
		System.setProperty("OBJECT_URL", "http://fdns-ms-stubbing:3002/object");
	}

	@Test
	public void indexPage() {
		ResponseEntity<String> response = this.restTemplate.getForEntity("/", String.class);
		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertThat(response.getBody(), CoreMatchers.containsString("FDNS HL7 Utilities Microservice"));
	}

	@Test
	public void indexAPI() {
		ResponseEntity<String> response = this.restTemplate.getForEntity(baseUrlPath, String.class);
		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertThat(response.getBody(), CoreMatchers.containsString("version"));
	}

	@Test
	public void transformHL7toXML() throws Exception {
		ResponseEntity<String> response = restTemplate.exchange(
				baseUrlPath + "/xml", 
				HttpMethod.POST, 
				getHL7Entity("01.txt"), 
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
	
	@Test
	public void transformHL7toJSON() throws Exception {
		ResponseEntity<JsonNode> response = restTemplate.exchange(
				baseUrlPath + "/json", 
				HttpMethod.POST, 
				getHL7Entity("01.txt"), 
				JsonNode.class);
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
	}
	
	@Test
	public void generateHash() throws Exception {
		ResponseEntity<JsonNode> response = restTemplate.exchange(
				baseUrlPath + "/hash", 
				HttpMethod.POST, 
				getEntity("hello", MediaType.TEXT_PLAIN),
				JsonNode.class);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).hasJsonPathValue("@.hash");
		assertThat(body).extractingJsonPathStringValue("@.hash").isEqualTo("5d41402abc4b2a76b9719d911017c592");
	}
	
	@Test
	public void generateCaseIdentifier() throws Exception {
		ResponseEntity<JsonNode> response = restTemplate.exchange(
				baseUrlPath + "/caseId/{spec}", 
				HttpMethod.POST, 
				getHL7Entity("03.txt"),
				JsonNode.class,
				"hl7");
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).hasJsonPathValue("@.id");
		assertThat(body).extractingJsonPathStringValue("@.id").isEqualTo("5276074519442620141102");
		assertThat(body).extractingJsonPathStringValue("@.hash").isEqualTo("ca43e807469423456b3a148a03193069");
		
		response = restTemplate.exchange(
				baseUrlPath + "/caseId/{spec}", 
				HttpMethod.POST, 
				getHL7Entity("03.txt"),
				JsonNode.class,
				"phinms");
		body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).hasJsonPathValue("@.id");
		assertThat(body).extractingJsonPathStringValue("@.id").isEqualTo("5276074519442620141102");
		assertThat(body).extractingJsonPathStringValue("@.hash").isEqualTo("ca43e807469423456b3a148a03193069");
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
