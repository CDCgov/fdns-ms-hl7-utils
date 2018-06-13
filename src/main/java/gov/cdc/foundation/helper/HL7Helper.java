package gov.cdc.foundation.helper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.camel.dataformat.xmljson.XmlJsonDataFormat;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.jayway.jsonpath.JsonPath;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.DefaultXMLParser;
import ca.uhn.hl7v2.parser.XMLParser;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator;
import edu.emory.mathcs.backport.java.util.Arrays;
import gov.cdc.foundation.controller.HL7Controller;
import gov.cdc.helper.ObjectHelper;
import gov.cdc.helper.common.ServiceException;
import gov.cdc.engine.SimpleValidator;
import gov.cdc.engine.ValidatorException;
import gov.cdc.engine.result.ValidationResult;

@Component
@Configuration
public class HL7Helper {

	private static final Logger logger = Logger.getLogger(HL7Controller.class);

	private static HL7Helper instance;

	@Value("${version}")
	private String version;

	private String caseIdHL7Identifiers;
	private String caseIdPHINIdentifiers;

	public HL7Helper(@Value("${caseId.observationIdentifiers.hl7}") String caseIdHL7Identifiers, @Value("${caseId.observationIdentifiers.phin}") String caseIdPHINIdentifiers) {
		this.caseIdHL7Identifiers = caseIdHL7Identifiers;
		this.caseIdPHINIdentifiers = caseIdPHINIdentifiers;
		instance = this;
	}

	public static HL7Helper getInstance() {
		return instance;
	}

	public Document parseToXML(Message msg) throws ServiceException {
		try {
			// instantiate an XML parser
			XMLParser xmlParser = new DefaultXMLParser();
			String xml = xmlParser.encode(msg);

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document message = builder.parse(new InputSource(new StringReader(xml)));

			Document doc = builder.newDocument();

			Element processor = doc.createElement("PROCESSOR");
			doc.appendChild(processor);

			Element messageElement = doc.createElement("MESSAGE");
			processor.appendChild(messageElement);

			Element hl7 = doc.createElement("HL7");
			messageElement.appendChild(hl7);

			Element source = doc.createElement("SOURCE");
			hl7.appendChild(source);
			Node importedNode = doc.importNode(message.getDocumentElement(), true);
			source.appendChild(importedNode);

			// Add extractor information
			Element extractor = doc.createElement("EXTRACTOR");
			Element version = doc.createElement("VERSION");
			version.setTextContent(this.version);
			extractor.appendChild(version);
			Element hash = doc.createElement("HASH");
			hash.setTextContent(getMD5Hash(msg.toString()));
			extractor.appendChild(hash);
			Element ucidElement = doc.createElement("UCID");
			ucidElement.setTextContent("EMPTY");
			extractor.appendChild(ucidElement);
			Element timestamp = doc.createElement("timestamp");
			timestamp.setTextContent(Long.toString(Calendar.getInstance().getTimeInMillis()));
			extractor.appendChild(timestamp);

			processor.appendChild(extractor);

			return doc;
		} catch (Exception e) {
			throw new ServiceException(e);
		}
	}

	public JSONObject parseToJSON(String message) throws ServiceException {
		return parseToJSON(message, null);
	}

	public JSONObject parseToJSON(String message, String spec) throws ServiceException {
		ByteArrayInputStream is = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
		Hl7InputStreamMessageIterator iter = new Hl7InputStreamMessageIterator(is);

		if (iter.hasNext()) {
			Message msg = iter.next();
			JSONObject json = HL7Helper.getInstance().parseToJSON(msg);
			if (spec != null) {
				JSONObject extractor = json.getJSONObject("extractor");
				extractor.put("UCID", getCaseIdentifier(json, spec));
			}
			return json;
		} else
			return null;
	}

	public JSONObject parseToJSON(Message msg) throws ServiceException {
		try {
			// instantiate an XML parser
			XMLParser xmlParser = new DefaultXMLParser();
			String xml = xmlParser.encode(msg);

			XmlJsonDataFormat xmlJsonDataFormat = new XmlJsonDataFormat();
			xmlJsonDataFormat.setEncoding("UTF-8");
			xmlJsonDataFormat.setForceTopLevelObject(true);
			xmlJsonDataFormat.setTrimSpaces(false);
			xmlJsonDataFormat.setSkipNamespaces(true);
			xmlJsonDataFormat.setRemoveNamespacePrefixes(true);

			xmlJsonDataFormat.start();
			InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
			String json = xmlJsonDataFormat.getSerializer().readFromStream(stream).toString();

			JSONObject obj = new JSONObject();
			JSONObject message = new JSONObject();
			JSONObject hl7 = new JSONObject();
			obj.put("message", message);
			message.put("HL7", hl7);
			message.put("type", "HL7");

			// Get json message
			JSONObject jsonObject = new JSONObject(json);
			cleanJSON(jsonObject);
			Object messageVersion = parseJsonPath(jsonObject, "$.ORU_R01.MSH.MSH-12");
			if (messageVersion == null)
				messageVersion = "unknown";
			hl7.put("version", messageVersion);
			hl7.put("source", jsonObject);

			// Add extractor information
			JSONObject extractor = new JSONObject();
			extractor.put("version", version);
			extractor.put("hash", getMD5Hash(msg.toString()));
			extractor.put("timestamp", Calendar.getInstance().getTimeInMillis());

			obj.put("extractor", extractor);

			return obj;
		} catch (Exception e) {
			throw new ServiceException(e);
		}
	}

	public void addPIIAnalysis(String authorizationKey, JSONObject json, String profile) throws ServiceException {
		json.put("PII", checkPII(authorizationKey, profile, json));
	}

	private JSONObject checkPII(String authorizationKey, String profile, JSONObject payload) throws ServiceException {
		// If profile is provided, check if we can find PII
		JSONObject piiObject = new JSONObject();
		if (profile != null && profile.length() > 0) {

			piiObject.put(MessageHelper.CONST_CHECKED, true);

			JSONObject piiRules = null;
			try {
				piiRules = ObjectHelper.getInstance(authorizationKey).getObject(profile + "_pii");
				piiRules.remove("_id");
			} catch (ResourceAccessException e) {
				throw new ServiceException(e);
			} catch (Exception e) {
				logger.error(e);
			}

			if (piiRules != null) {
				piiObject.put(MessageHelper.CONST_FOUNDPROFILE, true);

				ValidationResult vr = applyRules(payload, piiRules);

				// If the rule set matches
				if (vr != null && vr.isValid()) {
					// It means that we have PII
					throw new ServiceException(String.format(MessageHelper.ERROR_MESSAGE_WITH_PII, profile));
				}
				piiObject.put(MessageHelper.CONST_VALID, true);
			} else
				piiObject.put(MessageHelper.CONST_FOUNDPROFILE, false);
		} else
			piiObject.put(MessageHelper.CONST_CHECKED, false);
		return piiObject;
	}

	public ValidationResult applyRules(JSONObject object, JSONObject rules) throws ServiceException {
		try {
			SimpleValidator v = new SimpleValidator();
			v.initialize(rules);
			return v.validate(object);
		} catch (ValidatorException e) {
			throw new ServiceException(e);
		}
	}

	public JSONObject getCaseIdentifier(String message, String spec) throws ServiceException {
		JSONObject jsonObject = parseToJSON(message);
		return getCaseIdentifier(jsonObject, spec);
	}

	public JSONObject getCaseIdentifier(JSONObject message, String spec) throws ServiceException {
		JSONObject caseIdentifier = new JSONObject();

		// Get all observation identifiers
		Object observations = parseJsonPath(message, "$..OBX");

		// Get the list of identifiers
		String preferredCaseIdIdentifiers = "hl7".equalsIgnoreCase(spec) ? caseIdHL7Identifiers : caseIdPHINIdentifiers;
		String secondaryCaseIdIdentifiers = "hl7".equalsIgnoreCase(spec) ? caseIdPHINIdentifiers : caseIdHL7Identifiers;

		if (observations instanceof net.minidev.json.JSONArray) {
			net.minidev.json.JSONArray observationsArray = (net.minidev.json.JSONArray) observations;

			String[] caseIdIdentifiersArray = preferredCaseIdIdentifiers.split(",");
			String[] caseIdValuesArray = new String[caseIdIdentifiersArray.length];

			for (Object observation : observationsArray)
				processObservation(observation, caseIdIdentifiersArray, secondaryCaseIdIdentifiers.split(","), caseIdValuesArray);

			// Replace null values by empty string
			for (int i = 0; i < caseIdValuesArray.length; i++) {
				if (caseIdValuesArray[i] == null)
					caseIdValuesArray[i] = "";
			}

			// Just join all values
			String caseId = String.join("", caseIdValuesArray);
			caseIdentifier.put("id", caseId);
			caseIdentifier.put("hash", caseId.length() > 0 ? getMD5Hash(caseId) : "");

			// List all components
			JSONObject segments = new JSONObject();
			for (int i = 0; i < caseIdIdentifiersArray.length; i++) {
				segments.put(caseIdIdentifiersArray[i], caseIdValuesArray[i]);
			}
			caseIdentifier.put("segments", segments);

			return caseIdentifier;

		} else
			throw new ServiceException("Impossible to extract observation identifiers.");
	}

	private void processObservation(Object observation, String[] preferredIds, String[] secondaryIds, String[] caseIdValuesArray) throws ServiceException {
		if (observation instanceof Map) {
			JSONObject observationObj = new JSONObject((Map<?, ?>) observation);
			Object observationIdentifier = parseJsonPath(observationObj, "$.OBX-3.CE-1");
			if (observationIdentifier != null) {
				int index = Arrays.asList(preferredIds).indexOf(observationIdentifier);
				if (index != -1) {
					caseIdValuesArray[index] = getObservationValue(observationObj);
				} else {
					index = Arrays.asList(secondaryIds).indexOf(observationIdentifier);
					if (index != -1)
						caseIdValuesArray[index] = getObservationValue(observationObj);
				}
			}
		}
	}

	private String getObservationValue(JSONObject observation) throws ServiceException {
		String subField = "CWE-1";
		Object observationValue = parseJsonPath(observation, "$.OBX-5");
		if (observationValue instanceof String) {
			return (String) observationValue;
		} else if (observationValue instanceof JSONObject) {
			return ((JSONObject) observationValue).getString(subField);
		} else if (observationValue instanceof Map) {
			if (((Map<?, ?>) observationValue).containsKey(subField))
				return ((Map<?, ?>) observationValue).get(subField).toString();
			else
				return null;
		} else
			throw new ServiceException("Impossible to extract the observation value from: " + observation);
	}

	private Object parseJsonPath(JSONObject jsonObject, String jsonPath) {
		Object obj = null;
		try {
			obj = JsonPath.parse(jsonObject.toString()).read(jsonPath);
		} catch (Exception e) {
			logger.error(e);
		}
		return obj;
	}

	// Method used to remove "." from keys
	private void cleanJSON(Object object) {
		if (object instanceof JSONObject)
			cleanJSON((JSONObject) object);
	}

	// Method used to remove "." from keys
	private void cleanJSON(JSONObject jsonObject) {

		Set<String> keysToProcess = new HashSet<>();

		// Check if keys contains a dot
		Iterator<?> keys = jsonObject.keys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			// If it's a child is a JSON object
			if (jsonObject.get(key) instanceof JSONObject)
				cleanJSON((JSONObject) jsonObject.get(key));

			// If it's a child is a JSON array
			if (jsonObject.get(key) instanceof JSONArray) {
				JSONArray array = (JSONArray) jsonObject.get(key);
				for (int i = 0; i < array.length(); i++)
					cleanJSON(array.get(i));
			}

			if (key.contains("."))
				keysToProcess.add(key);
		}

		for (String key : keysToProcess) {
			String newKey = key.replaceAll("\\.", "-");
			jsonObject.put(newKey, jsonObject.get(key));
			jsonObject.remove((String) key);
		}
	}

	public String getMD5Hash(String msg) throws ServiceException {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new ServiceException(e);
		}
		md.update(msg.getBytes());

		byte[] byteData = md.digest();

		// convert the byte to hex format method 1
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < byteData.length; i++) {
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		}

		return sb.toString();

	}

	public String transform(Element element) throws ServiceException {
		try {
			return transform(new DOMSource(element));
		} catch (Exception e) {
			throw new ServiceException(e);
		}
	}

	public String transform(Document xml) throws ServiceException {
		try {
			return transform(new DOMSource(xml));
		} catch (Exception e) {
			throw new ServiceException(e);
		}
	}

	private String transform(DOMSource source) throws TransformerFactoryConfigurationError, TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		StringWriter writer = new StringWriter();
		transformer.transform(source, new StreamResult(writer));
		return writer.getBuffer().toString();
	}

}
