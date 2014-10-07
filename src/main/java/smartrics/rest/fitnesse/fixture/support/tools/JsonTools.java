package smartrics.rest.fitnesse.fixture.support.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Collection of mostly JSON data related functions. The code is based upon public domain
 * example code with minor adjustments.
 *
 * @author Dag Nygaard, Systek AS.
 */
public class JsonTools {

    private static final Logger LOG = LoggerFactory.getLogger(JsonTools.class);

    /**
     * Format JSON data string to a human readable format, i.e. include indentations
     * and linefeeds in the returned string.
     *
     * @param jsonString string with JSON content.
     * @return formatted inputstring.
     * @throws IOException formatting failed.
     */
    public static String prettyPrint(final String jsonString) throws IOException {
        if (null == jsonString || "".equals(jsonString.trim())) {
            return "";
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        try {
            Object jsonObj = mapper.readValue(jsonString, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);
        } catch (JsonParseException e) {
            throw new IOException("JsonParseException:" + e.getMessage());
        } catch (JsonMappingException e) {
            throw new IOException("JsonMappingException:" + e.getMessage());
        } catch (JsonProcessingException e) {
            throw new IOException("JsonProcessingException:" + e.getMessage());
        }
    }

    /**
     * Compare two JSON objects in either a strict or non-strict mode.
     * </p>
     * The parameter <code>strict</code> is translated like this:
     * <table border="1">
     * <tr>
     * <td>true</td><td>non-extendible - all fields and children must be mentioned.</td>
     * </tr> <tr>
     * <td>false</td><td>extendible and lenient.</td>
     * </tr>
     * </table>
     * Neither require strict order of the JSON fields.
     * </p>
     *
     * In both cases any found differences are described in the returned string.
     *
     * @param expectedJSON expected JSON data object.
     * @param actualJSON actual JSON data object.
     * @param strict indicate if compare mode is NON_EXTENSIBLE (true) or LENIENT (false).
     * @return string with description of JSON field differences.
     * @throws IOException some JSON data conversion function failed.
     */
    public static String compare(Object expectedJSON, Object actualJSON, Boolean strict) throws IOException {
        validateJsonParameters(expectedJSON, actualJSON);
        JSONCompareMode strictMode = null;
        if (strict) {
            strictMode = JSONCompareMode.NON_EXTENSIBLE;
        } else {
            strictMode = JSONCompareMode.LENIENT;
        }
        return compareJSON(expectedJSON, actualJSON, strictMode);
    }

    /**
     * Validate and compare two JSON objects with JSONCompareMode as a stringvalue.
     *
     * @param expectedJSON expected JSONObject or JSON as String.
     * @param actualJSON actual JSONObject or JSON as String.
     * @param jsonCompareModeStr JSONCompareMode as String.
     * @return Empty string if the two objects are equal in JSON value otherwise return deviation as a string.
     * @throws IOException Json parsing error.
     */
    public static String compare(Object expectedJSON, Object actualJSON, String jsonCompareModeStr) throws IOException {
        validateJsonParameters(expectedJSON, actualJSON);
        JSONCompareMode jsonCompareMode = parseJsonCompareMode(jsonCompareModeStr);
        return compareJSON(expectedJSON, actualJSON, jsonCompareMode);
    }

    /**
     * Compare two JSON objects with JSONCompareMode as parameter using JsonAssert compare functionality.
     *
     * @param expectedJSON expected JSONObject or JSON as String
     * @param actualJSON actual JSONObject or JSON as String.
     * @param jsonCompareMode JSONCompareMode enum.
     * @return Empty string if the two objects are equal in JSON value otherwise return deviation as a string.
     * @throws IOException
     */
    private static String compareJSON(Object expectedJSON, Object actualJSON, JSONCompareMode jsonCompareMode) throws IOException {
        JSONCompareResult jsonCompareResult = null;
        try {
            if (expectedJSON instanceof JSONObject) {
                jsonCompareResult = JSONCompare.compareJSON((JSONObject) expectedJSON, (JSONObject) actualJSON, jsonCompareMode);
            } else {
                jsonCompareResult = JSONCompare.compareJSON((String) expectedJSON, (String) actualJSON, jsonCompareMode);
            }
            if (null == jsonCompareResult) {
                throw new IOException("Unexpected comparasin parameter, neither String nor JSONObject.");
            } else {
                return jsonCompareResult.getMessage();
            }
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Simple JSON string indicator. Expect all JSON strings to contain '{' and "}".
     *
     * @param jsonStr string that is possibly a JSON data string.
     * @return true if the string contain both an opening "{" and a closing "}".
     */
    public static boolean isJson(final String jsonStr) {
        if (null == jsonStr || "".equals(jsonStr.trim())) {
            return false;
        }
        boolean valid = false;
        try {
            final JsonParser parser = new JsonFactory().createParser(jsonStr);
            while (parser.nextToken() != null) {
            }
            valid = true;
        } catch (JsonParseException jpe) {
            LOG.debug(jpe.getClass().getName() + ":" + jpe.getMessage());
        } catch (IOException ioe) {
            LOG.debug(ioe.getClass().getName() + ":" + ioe.getMessage());
        }
        return valid;
    }

    /**
     * Read the contents from the file and return in a single String.
     *
     * @param path file to read from.
     * @return string with filecontents.
     * @throws IOException Some error occurred.
     */
    public static String readFileContent(Path path) throws IOException {
        byte[] readBytes = Files.readAllBytes(path);
        return new String(readBytes);
    }

    /**
     * Convert a JSON data object (org.mozzila.javascript.*, not a {@link java.util.String}) to a java.lang.String.
     *
     * @param object JSON data object.
     * @return string with human readable JSON data string.
     */
    public static String toJSONString(Object object) {
        return new JsonParseTools().convertObjectToJson(object);
    }

    /**
     * Validate if compare objects are one of String or JSONObject and that both objects are of the same type.
     *
     * @param expectedJSON inputparameter 1.
     * @param actualJSON inputparameter 2.
     */
    protected static void validateJsonParameters(Object expectedJSON, Object actualJSON) {
        String errMsgGeneralpart = "Json must be a java.util.String or a org.json.JSONObject.";
        if (null == expectedJSON || !((expectedJSON instanceof String) || (expectedJSON instanceof JSONObject))) {
            throw new IllegalArgumentException("Expected " + errMsgGeneralpart);
        }
        if (null == actualJSON || !((actualJSON instanceof String) || (actualJSON instanceof JSONObject))) {
            throw new IllegalArgumentException("Actual " +errMsgGeneralpart);
        }

        if (!expectedJSON.getClass().getName().equals(actualJSON.getClass().getName())) {
            throw new IllegalArgumentException(String.format("ExpectedJson[%s] and actualJson[%s] must be of same type.",
                                                expectedJSON.getClass().getName(),
                                                actualJSON.getClass().getName()));
        }
    }

    /**
     * Parse a string, extract JSONCompareMode enum or throw a readable exception if the value
     * does not parse.
     *
     * @param jsonCompareModeStr string to parse.
     * @return JSONCompareMode enum value or throws IllegalArguementException.
     */
    protected static JSONCompareMode parseJsonCompareMode(String jsonCompareModeStr) {
        if (null == jsonCompareModeStr) {
            StringBuffer errMsg = new StringBuffer("jsonCompareMode parameter must be one of:");
            for (JSONCompareMode mode : JSONCompareMode.values()) {
                errMsg.append(" " + mode.name());
            }
            errMsg.append(".");
            throw new IllegalArgumentException(errMsg.toString());
        }
        try {
            return JSONCompareMode.valueOf(jsonCompareModeStr);
        } catch (IllegalArgumentException iae) {
            //not a JSONCompareMode, throw same exception as for null.
            return parseJsonCompareMode(null);
        }
    }
}
