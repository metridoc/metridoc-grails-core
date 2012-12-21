package metridoc.utils

import java.text.SimpleDateFormat
import java.util.regex.Matcher

/**
 * Provides parsing utilities for apache logs
 */
class ApacheLogParser extends LogParser {

    static final TEXT = /([^\s]+)\s/
    static final COMMON_REGEX_MAP = [
            ipAddress: TEXT,
            clientId: TEXT,
            patronId: TEXT,
            logDate: /\[([^\]]+)\]\s/,
            unformattedUrl: /"([^"]+)"\s/,
            httpStatus: TEXT,
            fileSize: /([^\s]+)/
    ]

    static final COMBINED_REGEX_MAP = COMMON_REGEX_MAP
    static {
        COMBINED_REGEX_MAP.putAll(
            [
                    refUrl: /\s"(.+)"\s/,
                    agent: /"([^"]+)"/
            ]
        )
    }
    private static final String APACHE_DATE_FORMAT = "dd/MMM/yyyy:hh:mm:ss Z"
    private static final APACHE_COMMON_LOG_PARSER = new ApacheLogParser(regexMap: COMMON_REGEX_MAP)
    private static final APACHE_COMBINED_LOG_PARSER = new ApacheLogParser(regexMap: COMBINED_REGEX_MAP)

    static Map<String, Object> parseCommon(String line) {
        def unFormattedResult = APACHE_COMMON_LOG_PARSER.parse(line)
        return getFormattedValues(unFormattedResult)
    }

    static Map<String, Object> parseCombined(String line) {
        def unFormattedResult = APACHE_COMBINED_LOG_PARSER.parse(line)
        return getFormattedValues(unFormattedResult)
    }

    static Map<String, Object> getFormattedValues(Map<String, String> record) {
        def result = [:]
        result.putAll(record)
        result.logDate = parseLogDate(record.logDate)
        def urlParams = record.unformattedUrl.split(" ")
        result.httpMethod = urlParams[0]
        result.url = urlParams[1]
        try {
            result.httpStatus = Integer.valueOf(record.httpStatus)
        } catch (NumberFormatException ex) {
            result.httpStatus = null
        }

        try {
            result.fileSize = Integer.valueOf(record.fileSize)
        } catch (NumberFormatException ex) {
            result.fileSize = null
        }

        return result
    }

    static Date parseLogDate(String date) {
        try {
            return new SimpleDateFormat(APACHE_DATE_FORMAT).parse(date)
        } catch (Exception ex) {
            //do nothing
        }

        return null
    }
}