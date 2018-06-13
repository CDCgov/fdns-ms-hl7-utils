package gov.cdc.foundation.helper;

import gov.cdc.helper.AbstractMessageHelper;

public class MessageHelper extends AbstractMessageHelper {

	public static final String CONST_PROFILE = "profile";
	public static final String CONST_CHECKED = "checked";
	public static final String CONST_FOUNDPROFILE = "foundProfile";
	public static final String CONST_VALID = "valid";
	public static final String CONST_WARNING = "warning";

	public static final String METHOD_INDEX = "index";
	public static final String METHOD_TRANSFORMHL7TOJSON = "transformHL7toJSON";
	public static final String METHOD_TRANSFORMHL7TOXML = "transformHL7toXML";
	public static final String METHOD_GETRULESSCHEMA = "getRulesSchema";
	public static final String METHOD_CHECKRULESSYNTAX = "checkRulesSyntax";
	public static final String METHOD_UPSERTRULES = "upsertRules";
	public static final String METHOD_GETRULES = "getRules";
	public static final String METHOD_VALIDATEHL7_WITH_JSONRULES = "validateWithJSONRules";
	public static final String METHOD_GETCASEIDENTIFIER = "getCaseIdentifier";
	public static final String METHOD_GETMESSAGEHASH = "getMessageHash";
	public static final String METHOD_GENERATE = "generate";

	public static final String ERROR_EMPTY_MESSAGE = "Empty message";
	public static final String ERROR_PROFILE_CONFIGURATION_INVALID = "The profile configuration is not valid, please check the syntax and the schema.";
	public static final String ERROR_PROFILE_IDENTIFIER_INVALID = "The profile identifier is not valid, it must match the following expression: %s";
	public static final String ERROR_RULE_SET_DOESNT_EXIST = "This rule set doesn't exist.";
	public static final String ERROR_MESSAGE_WITH_PII = "This HL7 message contains PII information (conforming to the profile `%s`)";

	private MessageHelper() {
		throw new IllegalAccessError("Helper class");
	}

}
