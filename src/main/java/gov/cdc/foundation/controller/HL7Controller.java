package gov.cdc.foundation.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import freemarker.template.Configuration;
import freemarker.template.Template;
import gov.cdc.foundation.helper.HL7Helper;
import gov.cdc.foundation.helper.LoggerHelper;
import gov.cdc.foundation.helper.MessageHelper;
import gov.cdc.foundation.helper.TemplateHelper;
import gov.cdc.helper.ErrorHandler;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

@Controller
@EnableAutoConfiguration
@RequestMapping("/api/1.0/")
public class HL7Controller {

	private static final Logger logger = Logger.getLogger(HL7Controller.class);
	
	@Value("${version}")
	private String version;
	
	@Autowired
	private TemplateHelper templateHelper;

	@RequestMapping(method = RequestMethod.GET)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 400, message = "Route parameters or json payload contain invalid data"),
			@ApiResponse(code = 401, message = "HTTP header lacks valid OAuth2 token"),
			@ApiResponse(code = 403, message = "HTTP header has valid OAuth2 token but lacks the appropriate scope to use this route"),
			@ApiResponse(code = 404, message = "Not Found"),
	})
	@ResponseBody
	public ResponseEntity<?> index() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		
		Map<String, Object> log = new HashMap<>();
		
		try {
			JSONObject json = new JSONObject();
			json.put("version", version);
			return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_INDEX, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}
	
	@RequestMapping(
		value = "json",
		method = RequestMethod.POST,
		produces = MediaType.APPLICATION_JSON_VALUE,
		consumes = MediaType.TEXT_PLAIN_VALUE
	)
	@ApiOperation(
		value = "Transform HL7 to JSON",
		notes = "Transforms an HL7 message to JSON object."
	)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Returns json transformation of provided HL7 data"),
			@ApiResponse(code = 400, message = "Route parameters or json payload contain invalid data"),
			@ApiResponse(code = 401, message = "HTTP header lacks valid OAuth2 token"),
			@ApiResponse(code = 403, message = "HTTP header has valid OAuth2 token but lacks the appropriate scope to use this route"),
			@ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 413, message = "Request payload too large")
	})
	@ResponseBody
	public ResponseEntity<?> transformHL7toJSON(
		@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
		@RequestBody(required = true) String message, 
		@ApiParam(value = "Spec (HL7 or PHIN)", allowableValues = "hl7,phinms") @RequestParam(value = "spec", required = false) String spec
	) {
		return transformHL7toJSON(authorizationHeader, message, spec, null);
	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or #oauth2.hasScope('fdns.hl7-utils.'.concat(#profile).concat('.read'))"
		+ " or #oauth2.hasScope('fdns.hl7-utils.'.concat(#profile).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.hl7-utils.*.read')"
		+ " or #oauth2.hasScope('fdns.hl7-utils.*.*')"
	)
	@RequestMapping(
		value = "json/{profile}",
		method = RequestMethod.POST,
		produces = MediaType.APPLICATION_JSON_VALUE,
		consumes = MediaType.TEXT_PLAIN_VALUE
	)
	@ApiOperation(
		value = "Transform HL7 to JSON",
		notes = "Transforms an HL7 message to JSON object using a profile."
	)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Returns json transformation of provided HL7 data"),
			@ApiResponse(code = 400, message = "Route parameters or json payload contain invalid data"),
			@ApiResponse(code = 401, message = "HTTP header lacks valid OAuth2 token"),
			@ApiResponse(code = 403, message = "HTTP header has valid OAuth2 token but lacks the appropriate scope to use this route"),
			@ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 413, message = "Request payload too large")
	})
	@ResponseBody
	public ResponseEntity<?> transformHL7toJSON(
		@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
		@RequestBody(required = true) String message, 
		@ApiParam(value = "Spec (HL7 or PHIN)", allowableValues = "hl7,phinms") @RequestParam(value = "spec", required = false) String spec, 
		@ApiParam(value = "Profile identifier") @PathVariable(value = "profile") String profile
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_TRANSFORMHL7TOJSON);
		log.put(MessageHelper.CONST_PROFILE, profile);

		try {

			JSONObject json = HL7Helper.getInstance().parseToJSON(message, spec);

			if (json != null) {
				HL7Helper.getInstance().addPIIAnalysis(authorizationHeader, json, profile);

				log.put(MessageHelper.CONST_SUCCESS, true);
				LoggerHelper.getInstance().log(MessageHelper.METHOD_TRANSFORMHL7TOJSON, log);

				return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.OK);
			} else {
				log.put(MessageHelper.CONST_SUCCESS, false);
				log.put(MessageHelper.CONST_ERROR, MessageHelper.ERROR_EMPTY_MESSAGE);
				LoggerHelper.getInstance().log(MessageHelper.METHOD_TRANSFORMHL7TOJSON, log);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
			}

		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_TRANSFORMHL7TOJSON, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@RequestMapping(
		value = "caseId/{spec}",
		method = RequestMethod.POST,
		produces = MediaType.APPLICATION_JSON_VALUE,
		consumes = MediaType.TEXT_PLAIN_VALUE
	)
	@ApiOperation(
		value = "Get case identifier",
		notes = "Get case identifier"
	)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Returns json containing case identifier and its segments"),
			@ApiResponse(code = 400, message = "Route parameters or json payload contain invalid data"),
			@ApiResponse(code = 401, message = "HTTP header lacks valid OAuth2 token"),
			@ApiResponse(code = 403, message = "HTTP header has valid OAuth2 token but lacks the appropriate scope to use this route"),
			@ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 413, message = "Request payload too large")
	})
	@ResponseBody
	public ResponseEntity<?> getCaseIdentifier(
		@RequestBody(required = true) String message,
		@ApiParam(value = "Spec (HL7 or PHIN)",
		allowableValues = "hl7,phinms",
		required = true) @PathVariable(value = "spec") String spec
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_GETCASEIDENTIFIER);

		try {
			return new ResponseEntity<>(mapper.readTree(HL7Helper.getInstance().getCaseIdentifier(message, spec).toString()), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_GETCASEIDENTIFIER, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@RequestMapping(
		value = "hash",
		method = RequestMethod.POST,
		produces = MediaType.APPLICATION_JSON_VALUE,
		consumes = MediaType.TEXT_PLAIN_VALUE
	)
	@ApiOperation(
		value = "Get message hash",
		notes = "Get message hash"
	)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Returns hash for provided message"),
			@ApiResponse(code = 400, message = "Route parameters or json payload contain invalid data"),
			@ApiResponse(code = 401, message = "HTTP header lacks valid OAuth2 token"),
			@ApiResponse(code = 403, message = "HTTP header has valid OAuth2 token but lacks the appropriate scope to use this route"),
			@ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 413, message = "Request payload too large")
	})
	@ResponseBody
	public ResponseEntity<?> getMessageHash(@RequestBody(required = true) String message) {
		ObjectMapper mapper = new ObjectMapper();

		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_GETMESSAGEHASH);

		try {

			JSONObject json = new JSONObject();
			json.put("hash", HL7Helper.getInstance().getMD5Hash(message));

			return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.OK);

		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_GETMESSAGEHASH, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@RequestMapping(
		value = "xml",
		method = RequestMethod.POST,
		produces = MediaType.APPLICATION_XML_VALUE
	)
	@ApiOperation(
		value = "Transform HL7 to XML",
		notes = "Transforms an HL7 message to XML document."
	)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Returns XML transformation of provided HL7 data"),
			@ApiResponse(code = 400, message = "Route parameters or json payload contain invalid data"),
			@ApiResponse(code = 401, message = "HTTP header lacks valid OAuth2 token"),
			@ApiResponse(code = 403, message = "HTTP header has valid OAuth2 token but lacks the appropriate scope to use this route"),
			@ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 413, message = "Request payload too large")
	})
	@ResponseBody
	public ResponseEntity<?> transformHL7toXML(
		@RequestBody(required = true) String message
	) throws HL7Exception {
		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_TRANSFORMHL7TOXML);

		try {
			ByteArrayInputStream is = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
			Hl7InputStreamMessageIterator iter = new Hl7InputStreamMessageIterator(is);

			if (iter.hasNext()) {
				Message msg = iter.next();
				Document doc = HL7Helper.getInstance().parseToXML(msg);

				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				StreamResult result = new StreamResult(new StringWriter());
				DOMSource source = new DOMSource(doc);
				transformer.transform(source, result);

				log.put(MessageHelper.CONST_SUCCESS, true);
				LoggerHelper.getInstance().log(MessageHelper.METHOD_TRANSFORMHL7TOXML, log);

				String xmlStr = result.getWriter().toString();
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document xml = builder.parse(new InputSource(new StringReader(xmlStr)));

				return new ResponseEntity<>(HL7Helper.getInstance().transform(xml), HttpStatus.OK);
			} else {

				log.put(MessageHelper.CONST_SUCCESS, false);
				log.put(MessageHelper.CONST_ERROR, MessageHelper.ERROR_EMPTY_MESSAGE);
				LoggerHelper.getInstance().log(MessageHelper.METHOD_TRANSFORMHL7TOXML, log);

				// convert to XML
				XStream xStream = new XStream(new DomDriver());
				xStream.alias("map", java.util.Map.class);
				String xml = xStream.toXML(log);

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(xml);
			}

		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_TRANSFORMHL7TOXML, log);

			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@RequestMapping(
		value = "generate",
		method = RequestMethod.GET,
		produces = MediaType.TEXT_PLAIN_VALUE
	)
	@ApiOperation(
		value = "Generate random HL7 message",
		notes = "Generate random HL7 message."
	)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Returns generated HL7 message"),
			@ApiResponse(code = 400, message = "Route parameters or json payload contain invalid data"),
			@ApiResponse(code = 401, message = "HTTP header lacks valid OAuth2 token"),
			@ApiResponse(code = 403, message = "HTTP header has valid OAuth2 token but lacks the appropriate scope to use this route"),
			@ApiResponse(code = 404, message = "Not Found"),
	})
	@ResponseBody
	public ResponseEntity<?> generate() {
		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_GENERATE);

		try {

			Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
			cfg.setClassForTemplateLoading(this.getClass(), "/");
			Template template = cfg.getTemplate("templates/hl7.ftl");
			StringWriter sw = new StringWriter();
			template.process(templateHelper.getModel(), sw);
			return new ResponseEntity<>(sw.toString(), HttpStatus.OK);

		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_GENERATE, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

}