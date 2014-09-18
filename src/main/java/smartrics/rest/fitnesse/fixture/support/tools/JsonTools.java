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
        if (null == expectedJSON || !((expectedJSON instanceof String) || (expectedJSON instanceof JSONObject))) {
            throw new IllegalArgumentException("Expected Json must be a java.util.String or a org.json.JSONObject.");
        }
        if (null == actualJSON || !((actualJSON instanceof String) || (actualJSON instanceof JSONObject))) {
            throw new IllegalArgumentException("Actual Json must be a java.util.String or a org.json.JSONObject.");
        }

        if (!expectedJSON.getClass().getName().equals(actualJSON.getClass().getName())) {
            throw new IllegalArgumentException(String.format("ExpectedJson[%s] and actualJson[%s] must be of same type.",
                                                expectedJSON.getClass().getName(),
                                                actualJSON.getClass().getName()));
        }
        try {
            JSONCompareMode strictMode = null;
            if (strict) {
                strictMode = JSONCompareMode.NON_EXTENSIBLE;
            } else {
                strictMode = JSONCompareMode.LENIENT;
            }
            JSONCompareResult jsonCompareResult = null;
            if (expectedJSON instanceof JSONObject) {
                jsonCompareResult = JSONCompare.compareJSON((JSONObject) expectedJSON, (JSONObject) actualJSON, strictMode);
            } else {
                jsonCompareResult = JSONCompare.compareJSON((String) expectedJSON, (String) actualJSON, strictMode);
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
}
