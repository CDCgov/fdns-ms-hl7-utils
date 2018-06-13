package gov.cdc.foundation.helper;

import java.util.Map;

import org.apache.log4j.Logger;
import org.fluentd.logger.FluentLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoggerHelper {

	private static final Logger logger = Logger.getLogger(LoggerHelper.class);

	private static LoggerHelper instance;
	
	private String prefix;
	private String host;
	private int port;
	private FluentLogger fluent;
	

	public LoggerHelper(@Value("${logging.fluentd.host}") String host, @Value("${logging.fluentd.port}") int port, @Value("${logging.fluentd.prefix}") String prefix) {
		logger.debug("Creating logger helper...");
		this.host = host;
		this.prefix = prefix;
		this.port = port;
		instance = this;
	}
	
	public static LoggerHelper getInstance() {
		return instance;
	}

	public void log(String action, Map<String, Object> data) {
		FluentLogger myLogger = getLogger();
		if (myLogger != null)
			myLogger.log(action, data);
	}

	private FluentLogger getLogger() {
		if (fluent == null)
			try {
				fluent = FluentLogger.getLogger(prefix, host, port);
			} catch (NoClassDefFoundError e) {
				logger.error(e);
			}
		return fluent;
	}

}
