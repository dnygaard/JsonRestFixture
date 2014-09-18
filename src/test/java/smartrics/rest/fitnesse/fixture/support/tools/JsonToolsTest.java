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
    public void compareMustHandleExpectedParameterNull() throws IOException {
        String errMessage = "Expected Json must be a java.util.String or a org.json.JSONObject.";
        try {
            JsonTools.compare(null, null, false);
            fail("IllegalArgumentException should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(errMessage, e.getMessage());
        } catch (IOException e) {
            fail("Wrong Exception:" + e.getClass().getName());
        }
    }

    @Test
    public void compareMustHandleActualParameterNull() throws IOException {
        String errMessage = "Actual Json must be a java.util.String or a org.json.JSONObject.";
        try {
            JsonTools.compare("   ", null, false);
            fail("IllegalArgumentException should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(errMessage, e.getMessage());
        } catch (IOException e) {
            fail("Wrong Exception:" + e.getClass().getName());
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void compareHandleIllegalExpectedParameter()  {
        String excpectedErrMessage = "Expected Json must be a java.util.String or a org.json.JSONObject.";
        try {
            JsonTools.compare(new HashMap(), " ", false);
            fail("IllegalArgumentException should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(excpectedErrMessage, e.getMessage());
        } catch (IOException e) {
            fail("Wrong Exception:" + e.getClass().getName());
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void compareHandleIllegalActualParameter()  {
        String actualErrMessage = "Actual Json must be a java.util.String or a org.json.JSONObject.";
        try {
            JsonTools.compare(" ", new HashMap(), false);
            fail("IllegalArgumentException should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(actualErrMessage, e.getMessage());
        } catch (IOException e) {
            fail("Wrong Exception:" + e.getClass().getName());
        }
    }

    @Test
    public void compareHandleDifferentParameterTypes1()  {
        String actualErrMessage = "ExpectedJson[java.lang.String] and actualJson[org.json.JSONObject] must be of same type.";
        try {
            JsonTools.compare(" ", new JSONObject(), false);
            fail("IllegalArgumentException should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(actualErrMessage, e.getMessage());
        } catch (IOException e) {
            fail("Wrong Exception:" + e.getClass().getName());
        }
    }

    @Test
    public void compareHandleDifferentParameterTypes2()  {
        String actualErrMessage = "ExpectedJson[org.json.JSONObject] and actualJson[java.lang.String] must be of same type.";
        try {
            JsonTools.compare(new JSONObject(), " ", false);
            fail("IllegalArgumentException should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(actualErrMessage, e.getMessage());
        } catch (IOException e) {
            fail("Wrong Exception:" + e.getClass().getName());
        }
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
