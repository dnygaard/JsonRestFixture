package smartrics.rest.fitnesse.fixture.support.tools;

import org.json.JSONException;
import org.json.JSONStringer;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;


/**
 * A collection of Json parsing functions. The code is based upon public domain
 * example code with minor adjustments.
 */
public class JsonParseTools {

    /**
     * Convert a Json object of type org.mozzila.javascript object to a java.util.String.
     *
     * @param object org.mozzila.javascript.* object.
     * @return a java.util.String version of the input parameter.
     */
    public String convertObjectToJson(Object object) {
        if (null == object) {
            return null;
        }
        JSONStringer json = new JSONStringer();
        try {
            if (object instanceof NativeArray) {
                nativeArrayToJSONString((NativeArray) object, json);
            } else if (object instanceof NativeObject) {
                nativeObjectToJSONString((NativeObject) object, json);
            } else {
                return object.toString();
            }
        } catch (JSONException e) {
            return "JSONException: " + e.getMessage() + ": " + object.toString();
        }
        return json.toString();
    }

    /* ------------  Helper functions for converting org.mozzila.javascript.* to java.util.String --------- */

    private void nativeArrayToJSONString(NativeArray nativeArray, JSONStringer json) throws JSONException {
        Object[] propIds = nativeArray.getIds();
        if (isArray(propIds) == true) {
            json.array();
            for (int i = 0; i < propIds.length; i++) {
                Object propId = propIds[i];
                if (propId instanceof Integer) {
                    Object value = nativeArray.get((Integer) propId,
                            nativeArray);
                    valueToJSONString(value, json);
                }
            }
            json.endArray();
        } else {
            json.object();
            for (Object propId : propIds) {
                Object value = nativeArray.get(propId.toString(), nativeArray);
                json.key(propId.toString());
                valueToJSONString(value, json);
            }
            json.endObject();
        }
    }

    private void nativeObjectToJSONString(NativeObject nativeObject, JSONStringer json) throws JSONException {
        json.object();
        Object[] ids = nativeObject.getIds();
        for (Object id : ids) {
            String key = id.toString();
            json.key(key);
            Object value = nativeObject.get(key, nativeObject);
            valueToJSONString(value, json);
        }
        json.endObject();
    }

    private void valueToJSONString(Object value, JSONStringer json) throws JSONException {
        if (value instanceof IdScriptableObject && ((IdScriptableObject) value).getClassName().equals("Date") == true) {
            // Get the UTC values of the date
            Object year = NativeObject.callMethod((IdScriptableObject) value, "getUTCFullYear", null);
            Object month = NativeObject.callMethod((IdScriptableObject) value, "getUTCMonth", null);
            Object date = NativeObject.callMethod((IdScriptableObject) value, "getUTCDate", null);
            Object hours = NativeObject.callMethod((IdScriptableObject) value, "getUTCHours", null);
            Object minutes = NativeObject.callMethod((IdScriptableObject) value, "getUTCMinutes", null);
            Object seconds = NativeObject.callMethod((IdScriptableObject) value, "getUTCSeconds", null);
            Object milliSeconds = NativeObject.callMethod((IdScriptableObject) value, "getUTCMilliseconds", null);

            // Build the JSON object to represent the UTC date
            json.object().key("zone").value("UTC").key("year").value(year)
                    .key("month").value(month).key("date").value(date)
                    .key("hours").value(hours).key("minutes").value(minutes)
                    .key("seconds").value(seconds).key("milliseconds")
                    .value(milliSeconds).endObject();

        } else if (value instanceof NativeJavaObject) {
            Object javaValue = Context.jsToJava(value, Object.class);
            json.value(javaValue);
        } else if (value instanceof NativeArray) {
            // Output the native object
            nativeArrayToJSONString((NativeArray) value, json);
        } else if (value instanceof NativeObject) {
            // Output the native array
            nativeObjectToJSONString((NativeObject) value, json);
        } else {
            json.value(value);
        }
    }

    private boolean isArray(Object[] ids) {
        boolean result = true;
        for (Object id : ids) {
            if (id instanceof Integer == false) {
                result = false;
                break;
            }
        }
        return result;
    }

}
