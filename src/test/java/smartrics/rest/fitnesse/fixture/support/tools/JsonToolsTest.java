package smartrics.rest.fitnesse.fixture.support.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class JsonToolsTest {
    @Rule
      public ExpectedException exception = ExpectedException.none();

    @Test
    public void prettyPrintMustHandleNull() throws IOException {
        assertEquals("", JsonTools.prettyPrint(null));
    }

    @Test
    public void prettyPrintMustEmptyInput() throws IOException {
        assertEquals("", JsonTools.prettyPrint(""));
        assertEquals("", JsonTools.prettyPrint("    "));
    }

    @Test
    public void prettyPrintMustHandleFaultyJson1() {
        try {
            JsonTools.prettyPrint(" }   ");
            fail("Should have thrown an IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().startsWith("JsonParseException"));
        }
    }

    @Test
    public void prettyPrintMustHandleFaultyJson2() {
        try {
            JsonTools.prettyPrint(" { not a Json string   ");
            fail("Should have thrown an IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().startsWith("JsonParseException"));
        }
    }

    @Test
    public void prettyPrintHappyDayTest() throws IOException {
        assertNotNull(JsonTools.prettyPrint(" { \"somefield\" :\"somevalue\", \"aFlag\":false } "));
    }

    @Test
    public void validateJsonParametersMustHandleExpectedParameterNull() throws IOException {
        String errMessage = "Expected Json must be a java.util.String or a org.json.JSONObject.";
        try {
            JsonTools.validateJsonParameters(null, null);
            fail("IllegalArgumentException should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(errMessage, e.getMessage());
        }
    }

    @Test
    public void validateJsonParametersMustHandleActualParameterNull() throws IOException {
        String errMessage = "Actual Json must be a java.util.String or a org.json.JSONObject.";
        try {
            JsonTools.validateJsonParameters("   ", null);
            fail("IllegalArgumentException should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(errMessage, e.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void validateJsonParametersHandleIllegalExpectedParameter()  {
        String excpectedErrMessage = "Expected Json must be a java.util.String or a org.json.JSONObject.";
        try {
            JsonTools.validateJsonParameters(new HashMap(), " ");
            fail("IllegalArgumentException should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(excpectedErrMessage, e.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void validateJsonParametersHandleIllegalActualParameter()  {
        String actualErrMessage = "Actual Json must be a java.util.String or a org.json.JSONObject.";
        try {
            JsonTools.validateJsonParameters(" ", new HashMap());
            fail("IllegalArgumentException should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(actualErrMessage, e.getMessage());
        }
    }

    @Test
    public void validateJsonParametersHandleDifferentParameterTypes1()  {
        String actualErrMessage = "ExpectedJson[java.lang.String] and actualJson[org.json.JSONObject] must be of same type.";
        try {
            JsonTools.validateJsonParameters(" ", new JSONObject());
            fail("IllegalArgumentException should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(actualErrMessage, e.getMessage());
        }
    }

    @Test
    public void validateJsonParametersHandleDifferentParameterTypes2()  {
        String actualErrMessage = "ExpectedJson[org.json.JSONObject] and actualJson[java.lang.String] must be of same type.";
        try {
            JsonTools.validateJsonParameters(new JSONObject(), " ");
            fail("IllegalArgumentException should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(actualErrMessage, e.getMessage());
        }
    }

    @Test
    public void parseJsonCompareModeMustValidateLegalEnumNames() {
        assertEquals(JSONCompareMode.LENIENT, JsonTools.parseJsonCompareMode(JSONCompareMode.LENIENT.name()));
        assertEquals(JSONCompareMode.NON_EXTENSIBLE, JsonTools.parseJsonCompareMode(JSONCompareMode.NON_EXTENSIBLE.name()));
        assertEquals(JSONCompareMode.STRICT, JsonTools.parseJsonCompareMode(JSONCompareMode.STRICT.name()));
        assertEquals(JSONCompareMode.STRICT_ORDER, JsonTools.parseJsonCompareMode(JSONCompareMode.STRICT_ORDER.name()));
    }

    @Test
    public void parseJsonCompareModeMustHandleGarbageInput() {
        testParseJsonCompareMode(null);
        testParseJsonCompareMode("foo bar");
        testParseJsonCompareMode("   ");
        testParseJsonCompareMode("123");
    }

    private void testParseJsonCompareMode(String param) {
        try {
            JsonTools.parseJsonCompareMode(param);
            fail("Should have throw IllegalArgumentException for '" + param + "'.");
        } catch (IllegalArgumentException iae) {
            assertEquals("jsonCompareMode parameter must be one of: STRICT LENIENT NON_EXTENSIBLE STRICT_ORDER.", iae.getMessage());
        }
    }

    @Test
    public void compareNonExtensibleModes() throws IOException {
        String expected =  "{id:1,name:\"Carter\"}";
        String actual = "{id:1,name:\"Carter\",favoriteColor:\"blue\"}";
        assertEquals("", JsonTools.compare(expected, actual, JSONCompareMode.LENIENT.name()));
        assertEquals("", JsonTools.compare(expected, actual, JSONCompareMode.STRICT_ORDER.name()));
        assertEquals("\nUnexpected: favoriteColor\n", JsonTools.compare(expected, actual, JSONCompareMode.NON_EXTENSIBLE.name()));
        assertEquals("\nUnexpected: favoriteColor\n", JsonTools.compare(expected, actual, JSONCompareMode.STRICT.name()));
    }

    @Test
    public void compareStrictOrderModes() throws IOException {
        String expected =  "{id:1,friends:[{id:2},{id:3}]}";
        String actual = "{id:1,friends:[{id:3},{id:2}]}";
        assertEquals("", JsonTools.compare(expected, actual, JSONCompareMode.LENIENT.name()));
        assertEquals("", JsonTools.compare(expected, actual, JSONCompareMode.NON_EXTENSIBLE.name()));
        assertEquals("friends[0].id\nExpected: 2\n     got: 3\n ; friends[1].id\nExpected: 3\n     got: 2\n", JsonTools.compare(expected, actual, JSONCompareMode.STRICT_ORDER.name()));
        assertEquals("friends[0].id\nExpected: 2\n     got: 3\n ; friends[1].id\nExpected: 3\n     got: 2\n", JsonTools.compare(expected, actual, JSONCompareMode.STRICT.name()));
    }

    @Test
    public void compareTooManyExpectedValues() throws IOException {
        String actual =  "{id:1,name:\"Carter\"}";
        String expected = "{id:1,name:\"Carter\",favoriteColor:\"blue\"}";
        String errorMessage = "\nExpected: favoriteColor\n     but none found\n";
        assertEquals(errorMessage, JsonTools.compare(expected, actual, JSONCompareMode.LENIENT.name()));
        assertEquals(errorMessage, JsonTools.compare(expected, actual, JSONCompareMode.STRICT_ORDER.name()));
        assertEquals(errorMessage, JsonTools.compare(expected, actual, JSONCompareMode.NON_EXTENSIBLE.name()));
        assertEquals(errorMessage, JsonTools.compare(expected, actual, JSONCompareMode.STRICT.name()));
    }

    @Test
    public void compareStringsStrictHappyDay() throws IOException {
        assertEquals("", JsonTools.compare(" { \"somefield\" :\"somevalue\", \"aFlag\":false } ",
                                           "{\"aFlag\":false, \"somefield\" :\"somevalue\" } ",
                                           true));
    }

    @Test
    public void compareStringsNonStrictHappyDay() throws IOException {
        assertEquals("", JsonTools.compare(" { \"somefield\" :\"somevalue\", \"aFlag\":false } ",
                                           "{\"aFlag\":false, \"somefield\" :\"somevalue\" } ",
                                           false));
    }

    @Test
    public void isJsonMustHandleFaultyJson() {
        assertFalse(JsonTools.isJson(null));
        assertFalse(JsonTools.isJson("  "));
        assertFalse(JsonTools.isJson(" foo bar "));
        assertFalse(JsonTools.isJson(" \" "));
        assertFalse(JsonTools.isJson(" { \"foo\" : } "));
    }

    @Test
    public void isJsonHappyDayTests() {
        assertTrue(JsonTools.isJson(" { } "));
        assertTrue(JsonTools.isJson(" { \"foo\" : \"bar\" } "));
        assertTrue(JsonTools.isJson(" { \"number\" : 123 } "));
        assertTrue(JsonTools.isJson(" { \"flag\" : false } "));
        assertTrue(JsonTools.isJson(" { \"flag\" : true } "));
        assertTrue(JsonTools.isJson(" { \"aList\" : [ { \"item1\" : 1, \"item2\" : 2, \"item3\" : 3   } ] } "));
    }

}
