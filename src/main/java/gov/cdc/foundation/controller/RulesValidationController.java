package gov.cdc.foundation.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
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
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.cdc.foundation.helper.HL7Helper;
import gov.cdc.foundation.helper.LoggerHelper;
import gov.cdc.foundation.helper.MessageHelper;
import gov.cdc.helper.ErrorHandler;
import gov.cdc.helper.ObjectHelper;
import gov.cdc.helper.common.ServiceException;
import gov.cdc.engine.SimpleValidator;
import gov.cdc.engine.ValidatorException;
import gov.cdc.engine.result.CompoundValidationResult;
import gov.cdc.engine.result.ValidationResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

@Controller
@EnableAutoConfiguration
@RequestMapping("/api/1.0/rules/")
public class RulesValidationController {

	private static final Logger logger = Logger.getLogger(RulesValidationController.class);

	private String profileRegex;
	private String profileSchemaPath;

	public RulesValidationController(
		@Value("${profile.regex}") String profileRegex,
		@Value("${profile.schema.path}"
	) String profileSchemaPath) {
		this.profileRegex = profileRegex;
		this.profileSchemaPath = profileSchemaPath;
	}

	@RequestMapping(
		value = "schema",
		method = RequestMethod.GET,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(
		value = "Get rules schema",
		notes = "Get rules schema"
	)
	@ResponseBody
	public ResponseEntity<?> getRulesSchema() {
		ObjectMapper mapper = new ObjectMapper();

		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_GETRULESSCHEMA);

		try {

			String schema = IOUtils.toString(RulesValidationController.class.getResourceAsStream(profileSchemaPath));
			return new ResponseEntity<>(mapper.readTree(schema), HttpStatus.OK);

		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_GETRULESSCHEMA, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@RequestMapping(
		value = "check",
		method = RequestMethod.POST,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(
		value = "Check rules against the schema",
		notes = "Check rules against the schema"
	)
	@ResponseBody
	public ResponseEntity<?> checkRulesSyntax(@RequestBody(required = true) String payload) {

		ObjectMapper mapper = new ObjectMapper();

		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_CHECKRULESSYNTAX);

		try {

			// Check the payload structure
			SimpleValidator v = new SimpleValidator();
			v.initialize(RulesValidationController.class.getResourceAsStream(profileSchemaPath));
			ValidationResult vr = v.validate(IOUtils.toInputStream(payload));

			JSONObject json = new JSONObject();
			json.put(MessageHelper.CONST_VALID, vr.isValid());

			if (vr instanceof CompoundValidationResult) {
				CompoundValidationResult cvr = (CompoundValidationResult) vr;
				List<ValidationResult> vrs = cvr.flatten();
				JSONArray details = new JSONArray();
				for (ValidationResult validationResult : vrs) {
					JSONObject vrObject = new JSONObject();
					vrObject.put("comment", validationResult.getComment());
					vrObject.put("description", validationResult.getDescription());
					vrObject.put(MessageHelper.CONST_VALID, validationResult.isValid());
					vrObject.put("object", validationResult.getObject());
					details.put(vrObject);
				}
				json.put("details", details);
			}

			return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.OK);

		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_CHECKRULESSYNTAX, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}

	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or #oauth2.hasScope('fdns.hl7-utils.'.concat(#profile).concat('.create'))"
		+ " or #oauth2.hasScope('fdns.hl7-utils.'.concat(#profile).concat('.update'))"
		+ " or #oauth2.hasScope('fdns.hl7-utils.'.concat(#profile).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.hl7-utils.*.create')"
		+ " or #oauth2.hasScope('fdns.hl7-utils.*.update')"
		+ " or #oauth2.hasScope('fdns.hl7-utils.*.*')"
	)
	@RequestMapping(
		value = "{profile}",
		method = RequestMethod.PUT,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(
		value = "Create or update rules for the specified profile",
		notes = "Create or update rules"
	)
	@ResponseBody
	public ResponseEntity<?> upsertRulesWithPut(
		@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
		@RequestBody(required = true) String payload, 
		@ApiParam(value = "Profile identifier") @PathVariable(value = "profile") String profile
	) {
		return upsertRules(authorizationHeader, payload, profile);
	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or #oauth2.hasScope('fdns.hl7-utils.'.concat(#profile).concat('.create'))"
		+ " or #oauth2.hasScope('fdns.hl7-utils.'.concat(#profile).concat('.update'))"
		+ " or #oauth2.hasScope('fdns.hl7-utils.'.concat(#profile).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.hl7-utils.*.create')"
		+ " or #oauth2.hasScope('fdns.hl7-utils.*.update')"
		+ " or #oauth2.hasScope('fdns.hl7-utils.*.*')"
	)
	@RequestMapping(
		value = "{profile}",
		method = RequestMethod.POST,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(
		value = "Create or update rules for the specified profile",
		notes = "Create or update rules"
	)
	@ResponseBody
	public ResponseEntity<?> upsertRules(
		@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
		@RequestBody(required = true) String payload, 
		@ApiParam(value = "Profile identifier") @PathVariable(value = "profile") String profile
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_UPSERTRULES);

		try {

			// First, check the profile ID
			Pattern p = Pattern.compile(profileRegex);
			if (!p.matcher(profile).matches())
				throw new ServiceException(String.format(MessageHelper.ERROR_PROFILE_IDENTIFIER_INVALID, profileRegex));

			// Then, check the payload structure
			SimpleValidator v = new SimpleValidator();
			v.initialize(RulesValidationController.class.getResourceAsStream(profileSchemaPath));
			ValidationResult vr = v.validate(IOUtils.toInputStream(payload));

			if (!vr.isValid()) {
				throw new ServiceException(MessageHelper.ERROR_PROFILE_CONFIGURATION_INVALID);
			}

			JSONObject data = new JSONObject(payload);
			ObjectHelper helper = ObjectHelper.getInstance(authorizationHeader);
			for (Object key : data.keySet()) {
				if (helper.exists(profile + "_" + key))
					helper.updateObject(profile + "_" + key, data.getJSONObject((String) key));
				else
					helper.createObject(data.getJSONObject((String) key), profile + "_" + key);
			}

			JSONObject json = new JSONObject();
			json.put(MessageHelper.CONST_SUCCESS, true);
			json.put(MessageHelper.CONST_PROFILE, profile);
			return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.OK);

		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_UPSERTRULES, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}

	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or #oauth2.hasScope('fdns.hl7-utils.'.concat(#profile).concat('.read'))"
		+ " or #oauth2.hasScope('fdns.hl7-utils.'.concat(#profile).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.hl7-utils.*.read')"
		+ " or #oauth2.hasScope('fdns.hl7-utils.*.*')"
	)
	@RequestMapping(
		value = "{profile}/{ruleset}",
		method = RequestMethod.GET,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(
		value = "Get saved rules for the specified profile",
		notes = "Get saved rules"
	)
	@ResponseBody
	public ResponseEntity<?> getRules(
		@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
		@ApiParam(value = "Profile identifier") @PathVariable(value = "profile") String profile, 
		@ApiParam(value = "Rule Set (such as `pii`, `warning` or `error`)") @PathVariable(value = "ruleset") String ruleset
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_GETRULES);

		try {

			String objectId = profile + "_" + ruleset;
			
			ObjectHelper helper = ObjectHelper.getInstance(authorizationHeader);
			if (!helper.exists(objectId))
				throw new ServiceException(MessageHelper.ERROR_RULE_SET_DOESNT_EXIST);

			return new ResponseEntity<>(mapper.readTree(helper.getObject(objectId).toString()), HttpStatus.OK);

		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log("ruleset", log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}

	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or #oauth2.hasScope('fdns.hl7-utils.'.concat(#profile).concat('.read'))"
		+ " or #oauth2.hasScope('fdns.hl7-utils.'.concat(#profile).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.hl7-utils.*.read')"
		+ " or #oauth2.hasScope('fdns.hl7-utils.*.*')"
	)
	@RequestMapping(
		value = "validate/{profile}",
		method = RequestMethod.POST,
		produces = MediaType.APPLICATION_JSON_VALUE,
		consumes = MediaType.TEXT_PLAIN_VALUE
	)
	@ApiOperation(value = "Validate HL7 message", notes = "Valides an HL7 message using JSON rules.")
	@ResponseBody
	public ResponseEntity<?> validate(
		@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
		@RequestBody(required = true) String payload, 
		@ApiParam(value = "Profile identifier") @PathVariable(value = "profile") String profile, 
		@RequestParam(defaultValue = "false") boolean explain, @RequestParam(defaultValue = "false") boolean checkPII
	) {
		ObjectMapper mapper = new ObjectMapper();

		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_VALIDATEHL7_WITH_JSONRULES);


		try {
			// Get the json version of the HL7 message
			JSONObject json = HL7Helper.getInstance().parseToJSON(payload);
			HL7Helper.getInstance().addPIIAnalysis(authorizationHeader, json, checkPII ? profile : null);
			
			JSONArray explainationDetails = new JSONArray();

			JSONObject responseObj = new JSONObject();
			// The status object will contain the status of each phase: PII,
			// warning, error
			JSONObject statusObj = new JSONObject();
			responseObj.put("status", statusObj);
			// Check the PII status from the transformation phase
			statusObj.put("PII", json.get("PII"));

			// Let's check the warnings
			int nbOfWarnings = checkValidationRules(
				authorizationHeader,
				json,
				statusObj,
				profile,
				MessageHelper.CONST_WARNING,
				explain,
				explainationDetails
			);

			// Let's check the errors
			int nbOfErrors = checkValidationRules(
				authorizationHeader,
				json,
				statusObj,
				profile,
				MessageHelper.CONST_ERROR,
				explain,
				explainationDetails
			);

			responseObj.put(MessageHelper.CONST_VALID, nbOfErrors == 0);
			responseObj.put("errors", nbOfErrors);
			responseObj.put("warnings", nbOfWarnings);

			if (explain) {
				responseObj.put("details", explainationDetails);
			}

			log.put(MessageHelper.CONST_SUCCESS, true);
			log.put(MessageHelper.CONST_VALID, nbOfErrors == 0);
			log.put("errors", nbOfErrors);
			log.put("warnings", nbOfWarnings);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_VALIDATEHL7_WITH_JSONRULES, log);

			return new ResponseEntity<>(mapper.readTree(responseObj.toString()), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_VALIDATEHL7_WITH_JSONRULES, log);

			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	private int checkValidationRules(
		String authorizationHeader,
		JSONObject payload,
		JSONObject statusObj,
		String profile,
		String ruleSet,
		boolean explain,
		JSONArray explainationDetails
	) throws ServiceException, ValidatorException {
		int nbOfInvalidItems = 0;
		JSONObject status = new JSONObject();
		status.put(MessageHelper.CONST_CHECKED, true);
		JSONObject rules = null;

		try {
			rules = ObjectHelper.getInstance(authorizationHeader).getObject(profile + "_" + ruleSet);
			rules.remove("_id");
		} catch (ResourceAccessException e) {
			throw new ServiceException(e);
		} catch (Exception e) {
			logger.error(e);
		}

		List<ValidationResult> vrList = executeRules(payload, rules, status);
		if (vrList != null)
			for (ValidationResult vrItem : vrList) {
				// If the rules return true, it means that an error or a warning exists
				if (vrItem.isValid())
					nbOfInvalidItems++;
				if (explain)
					explainationDetails.put(createExplanationDetail(vrItem, ruleSet));
			}
		statusObj.put(ruleSet, status);

		return nbOfInvalidItems;
	}

	private JSONObject createExplanationDetail(ValidationResult vr, String type) {
		JSONObject detail = new JSONObject();
		detail.put("ID", vr.getDescription());
		detail.put("title", vr.getComment());
		detail.put("command", vr.getCommand());
		if (vr.getRule() != null)
			detail.put("rule", new JSONObject(vr.getRule()));
		detail.put("type", type);
		// We need to invert the passed
		detail.put("passed", !vr.isValid());
		return detail;
	}

	private List<ValidationResult> executeRules(
		JSONObject payload,
		JSONObject rules,
		JSONObject status
	) throws ServiceException, ValidatorException {
		List<ValidationResult> checkList = null;
		if (rules != null) {
			status.put(MessageHelper.CONST_FOUNDPROFILE, true);

			// Then, check the JSON Object
			ValidationResult vr = HL7Helper.getInstance().applyRules(payload, rules);
			status.put(MessageHelper.CONST_VALID, vr.isValid());

			if (vr instanceof CompoundValidationResult) {
				CompoundValidationResult cvr = (CompoundValidationResult) vr;
				checkList = cvr.flatten();
			}

		} else
			status.put(MessageHelper.CONST_FOUNDPROFILE, false);
		return checkList;
	}

}