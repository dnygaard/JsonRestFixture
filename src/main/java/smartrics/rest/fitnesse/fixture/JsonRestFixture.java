package smartrics.rest.fitnesse.fixture;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;

import smartrics.rest.client.RestResponse;
import smartrics.rest.fitnesse.fixture.support.CellWrapper;
import smartrics.rest.fitnesse.fixture.support.JavascriptWrapper;
import smartrics.rest.fitnesse.fixture.support.StringTypeAdapter;
import smartrics.rest.fitnesse.fixture.support.TextBodyTypeAdapter;
import smartrics.rest.fitnesse.fixture.support.Tools;
import smartrics.rest.fitnesse.fixture.support.tools.JsonTools;

/**
 * JsonRestFixture is an extension of {@code RestFixture} that focuses on JSON REST response objects
 * and includes JSON utilities especially made for non-technical testers.
 * <p/>
 *
 * The superclass still handle the actual HTTP Requests, HTTP Responses and HTTP headers.
 * <p/>
 *
 * This fixture has the following new functionality:
 * <ul>
 * <li><b>copyJsonbodyToFile</b> - copy the HttpResponse body to a file, given that the Content-
 * type is "application/json".
 * <li><b>showJsFileContent</b> - present the contents of a textfile.
 * <li><b>jsCompare</b> - compare the contents  of two input fields. The fields must either be either
 * a string with JSON data or the path of a file with JSON data.
 * </ul>
 * <p/>
 *
 * As for {@code RestFixture}, URL is a required parameter even though it might not be used. This
 * is to enforce test documentation - which URL was used to extract JSON data.
 * <p/>
 *
 * Example FitNesse table setup:</br>
 * <code>
 * |!-Table:smartrics.rest.fitnesse.fixture.JsonRestFixture-! | http://url| </br>
 * |GET|/function|HTTP-responsecode|!-Content-Type : application/json-!| |
 * </code>
 * <p/>
 *
 * @author Dag Nygaard, Systek AS.
 */
public class JsonRestFixture extends RestFixture {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRestFixture.class);

    private static final String RESPONSE_JSONBODY = JavascriptWrapper.RESPONSE_OBJ_NAME + "." + JavascriptWrapper.JSON_OBJ_NAME;

    /**
     * Flag to debug methodcalls. Default value should be false. Set to true to show call start- and endpoints in log.
     */
    private boolean debugMethodCall = true;

    /**
     * Constructor for Fit runner.
     */
    public JsonRestFixture() {
        super();
    }

    /**
     * Constructor for Slim runner.
     *
     * @param hostName
     *            the cells following up the first cell in the first row.
     */
    public JsonRestFixture(String hostName) {
        super(hostName);
    }

    /**
     * Constructor for Slim runner.
     *
     * @param hostName
     *            the cells following up the first cell in the first row.
     * @param configName
     *            the value of cell number 3 in first row of the fixture table.
     */
    public JsonRestFixture(String hostName, String configName) {
        super(hostName, configName);
    }

    /**
     * @param partsFactory
     *            the factory of parts necessary to create the rest fixture
     * @param hostName
     * @param configName
     */
    public JsonRestFixture(PartsFactory partsFactory, String hostName, String configName) {
        super(partsFactory, hostName, configName);
    }

    /* ------------- Debug functions copied from  @link RestFixture -------------  */

    /**
     * Log start of called method ("=>").
     */
    private void debugMethodCallStart() {
        debugMethodCall("=> ");
    }

    /**
     * Log end of called method ("<=").
     */
    private void debugMethodCallEnd() {
        debugMethodCall("<= ");
    }

    /**
     * Tag start and end of call in log, or tag of some other reason,  add methodname to log.
     * @param logPrefix stringsymbols prefixing log entries of methodcalls to indicate start, end or developerchosen log-alerts.
     */
    private void debugMethodCall(String logPrefix) {
        if (debugMethodCall) {
            StackTraceElement el = Thread.currentThread().getStackTrace()[4];
            LOG.debug(logPrefix + el.getMethodName());
        }
    }

    /**
     * <code> | copyJsonbodyToFile  | fileName | result |</code>
     * <p/>
     * Copy the last HTTP Response result of JSON type to a file, i.e. copy HttpResponseBody from the last
     * GET or POST call to a file.
     * <p/>
     * Note: The response must be of type "application/json".
     * <p/>
     *
     * <ul>
     * <li/><code>fileName</code> Name of file to write to. If no path is included
     * in the filename, ./ is FitNesseRoot directory.
     * <p/>
     * Legal fileName values given that indicated directories exist: </br>
     * <code>c:/foo/bar/jsresult.txt</code> </br>
     * <code>./files/foobared.txt</code> </br>
     * <code>.\files\foobared2.txt</code>  </br>
     * <p/>
     *
     * <li/><code>result</code> is a mandatory empty cell used to display method
     * call result if no errors has occurred. Any input will be ignored, but
     * do not forget to include the cell in the input.
     *
     * The test row must have an empty cell at the end that will display the
     * value extracted and assigned to the label.
     * <p/>
     * Example call: <br/>
     * <code>| copyJsonbodyToFile | ./files/someJsonContent.txt |  |</code><br/>
     * <p/>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void copyJsonbodyToFile() {
        debugMethodCallStart();
        if(row.size() != 3) {
            getFormatter().exception(row.getCell(row.size() - 1), "Correct number of cells not found: | copyJsonbodyToFile  | fileName | |");
            debugMethodCallEnd();
            return;
        }
        String fileName = row.getCell(1).text().trim();
        if(null == fileName || "".equals(fileName)) {
            getFormatter().exception(row.getCell(1), "fileName cannot be empty: | copyJsonbodyToFile  | fileName | |");
            debugMethodCallEnd();
            return;
        }
        if (null == getLastResponse()) {
            getFormatter().exception(row.getCell(1), "No HTTPResponse found, is preceeding HTTP GET or POST call with expected application/json response missing?");
            debugMethodCallEnd();
            return;
        }
        try {
            Path jsonFile = FileSystems.getDefault().getPath(fileName);
            Files.write(jsonFile, JsonTools.prettyPrint(getJsonString(getLastResponse())).getBytes());
            CellWrapper statusCell = row.getCell(2);
            statusCell.body("pass:" + Tools.wrapInDiv(getFormatter().label("[Requestresponse successfully copied to file.]")));
        } catch (JsonParseException e) {
            getFormatter().exception(row.getCell(1), "IO Error:" + e.getClass().getName() + ":"  + e.getMessage());
            debugMethodCallEnd();
            return;
        } catch (IOException e) {
            getFormatter().exception(row.getCell(1), "IO Error:" + e.getClass().getName() + ":"  + e.getMessage());
            debugMethodCallEnd();
            return;
        }
        debugMethodCallEnd();
    }


    /**
     * <code> | showJsonFileContent  | fileName | |</code>
     * <p/>
     * Copy the last HTTP Response result of JSON type to a file, i.e. copy HttpResponseBody from the last
     * GET or POST call to a file.
     * <p/>
     * Note: The response must be of type "application/json".
     * <p/>
     *
     * <ul>
     * <li/><code>fileName</code> name of file to write to. If no path is included
     * in the filename, the file will use current working directory, i.e. FitNesseRoot as path.
     * <p/>
     * Legal fileName values given that indicated directories exist: </br>
     * <code>c:/foo/bar/jsresult.txt</code> </br>
     * <code>./files/foobared.txt</code> </br>
     * <code>.\files\foobared2.txt</code>  </br>
     * <p/>
     *
     * <li/><code>result</code> is a mandatory empty cell used to display file contents if no errors has occurred.
     * <p/>
     * </ul>
     *
     * <b>Limitations:</b> The file content size must not exceed Max String size as the the code will have buffer problems.
     * <p/>
     *
     * Example call: <br/>
     * <code>| copyJsonbodyToFile | ./files/someJsonContent.txt |  |</code><br/>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void showJsonFileContent() {
        debugMethodCallStart();
        if(row.size() != 3) {
            getFormatter().exception(row.getCell(row.size() - 1), "Not all cells found: | showJsonFileContent  | fileName | |");
            debugMethodCallEnd();
            return;
        }
        String fileName = row.getCell(1).text().trim();
        if(null == fileName || "".equals(fileName)) {
            getFormatter().exception(row.getCell(1), "fileName cannot be empty: | showJsonFileContent  | fileName | |");
            debugMethodCallEnd();
            return;
        }
        Path jsonFile = FileSystems.getDefault().getPath(fileName);
        if (!Files.exists(jsonFile)) {
            getFormatter().exception(row.getCell(1), "File '" + fileName + "' does not exist: | showJsonFileContent  | fileName | |");
            debugMethodCallEnd();
            return;
        }
        try {
            String stringFromFile = new String(Files.readAllBytes(jsonFile));
            String prettyfiedJsonString = JsonTools.prettyPrint(stringFromFile);
            CellWrapper contentCell = row.getCell(2);
            contentCell.body("pass:" +  Tools.wrapInDiv(Tools.toHtml(prettyfiedJsonString)));
        } catch (IOException e) {
            getFormatter().exception(row.getCell(1), "IOError:" + e.getMessage());
        }
        debugMethodCallEnd();
    }

    /**
     * <code> | jsCompare | strict | actual | expected | result |</code>
     * <p/>
     * Compare two JSON data parameters, strictly or non-strictly. The two JSON data parameters must be
     * either a string with JSON data or the name of a file containing a JSON data string. Strict comparison
     * means that all fields and elements must be listed and in order in the expected parameter to avoid negative result.
     * Non-strict comparison will warn and list unmentioned JSON elements, but will still report errors when values are
     * not equal. The comparison does not completely solve the "equal to" vs "identical to" issue, but lets testers
     * avoid problematic fields as timestamps and sequencenumbers that might not be known prior to running a
     * test.
     * <p/>
     *
     * <ul>
     * <li/><code>strict</code> "True" or "False" - Do a "identical" comparison vs do a near-"similar" comparison respectively.
     * <p/>
     *
     * <li/><code>actual</code> String with JSON data or the name of a file containing JSON data that the webservice being tested has produced.
     * <p/>
     *
     * <li/><code>expected</code> String with JSON data or the name of a file containing JSON data that the tester requires the actual data to be compared to.
     * <p/>
     *
     * <li/><code>result</code> Empty cell where the comparison result is presented. Any user input will be ignored.
     * <p/>
     * </ul>
     *
     * Filenames without any path will use current directory, i.e. FitNesseRoot directory.
     * <p/>
     *
     * Example call: <br/>
     * <code>| jsCompare | false | ./files/someFile.tmp | {"foo":"bar"} | | </code><br/>
     *
     * <p/>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void jsCompare() {
        debugMethodCallStart();
        if (row.size() != 5) {
            getFormatter().exception(row.getCell(row.size() - 1), "Not all cells found: | jsCompare | strict | actual | expected | result |");
            debugMethodCallEnd();
            return;
        }
        Boolean strict = null;
        try {
            strict = getBoolean(row.getCell(1).text());
        } catch (IOException e) {
            getFormatter().exception(row.getCell(1), "Parameter strict has not been set correctly:" + e.getMessage());
            debugMethodCallEnd();
            return;
        }
        CellWrapper actualCell = row.getCell(2);
        CellWrapper expectedCell = row.getCell(3);
        CellWrapper resultCell = row.getCell(4);

        String actualStr = getJsonContentFromCellOrFileOrWriteErrorMessageToCell(actualCell, "ActualContent");
        if (null == actualStr) {
            debugMethodCallEnd();
            return;
        }
        String expectedStr = getJsonContentFromCellOrFileOrWriteErrorMessageToCell(expectedCell, "ExpectedContent");
        if (null == expectedStr) {
            debugMethodCallEnd();
            return;
        }
        try {
            String diffMessage = JsonTools.compare(expectedStr, actualStr, strict);
            LOG.debug("DiffMessage: " + diffMessage);
            if ("".equals(diffMessage)) {
                if (strict) {
                    resultCell.body("pass:" + Tools.wrapInDiv(getFormatter().label("[No deviations found.]")));
                } else {
                    //non-strict testing will ignore missing fields, we want to warn about fields that are in actual json, but exist in actual json.
                    String reverseDiffMessage = JsonTools.compare(actualStr, expectedStr, strict);
                    LOG.debug("ReverseDiffMessage:" + reverseDiffMessage);
                    if ("".equals(reverseDiffMessage)) {
                        resultCell.body("pass:" + Tools.wrapInDiv(getFormatter().label("[No deviations found.]")));
                    } else {
                        resultCell.body("error:" + Tools.wrapInDiv(reverseDiffMessage));
                    }
                }
            } else {
                if (strict) {
                    resultCell.body("fail:" + Tools.wrapInDiv(diffMessage));
                } else {
                    String reverseDiffMessage = JsonTools.compare(actualStr, expectedStr, strict);
                    //|| Pattern.compile("Expected|values|got").matcher(reverseDiffMessage).find()
                    if (diffMessage.contains("got:") ) {
                        resultCell.body("fail:" + Tools.wrapInDiv(diffMessage));
                    } else if (Pattern.compile(".*Expected.*values.*got.*").matcher(diffMessage).find()) {
                        //example: Expected 1 values but got 2
                        int found = 0;
                        int expected = 0;
                        @SuppressWarnings("resource")
                        Scanner scanner = new Scanner(diffMessage).useDelimiter("[^\\d]+");
                        if (scanner.hasNextInt()) {
                            expected = scanner.nextInt();
                        }
                        if (scanner.hasNextInt()) {
                            found = scanner.nextInt();
                        }
                        if (expected > found) {
                            resultCell.body("fail:" + Tools.wrapInDiv(diffMessage));
                        } else {
                            resultCell.body("error:" + Tools.wrapInDiv(diffMessage));
                        }
                    } else if (!"".equals(reverseDiffMessage) && Pattern.compile("Expected|values|got").matcher(reverseDiffMessage).find()) {
                        resultCell.body("error:" + Tools.wrapInDiv(reverseDiffMessage));
                    } else {
                        StringBuffer sb = new StringBuffer();
                        sb.append(Tools.makeToggleCollapseable("Missing field(s)", Tools.toHtml(diffMessage)));
                            resultCell.body("fail:" + Tools.wrapInDiv(sb.toString()));
                        }
                    }
                }
            debugMethodCallEnd();
            return;
        } catch (Exception e) {
            resultCell.body(e.getMessage());
            getFormatter().wrong(resultCell, new StringTypeAdapter());
            debugMethodCallEnd();
            return;
        }
    }

    /**
     * Verify that cell contains a Jsonformatted string or the name of a file containing a
     * Jsonformatted string returning the string in both cases. If both verifications fail set
     * up cell with error message and return null.
     *
     * @param cell expected or actual json content
     * @param contentName name of content for possible errormessage.
     * @return jsonformatted string or null.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String getJsonContentFromCellOrFileOrWriteErrorMessageToCell(CellWrapper cell, String contentName) {
        String cellBodyStr = cell.body();
        if (!JsonTools.isJson(cellBodyStr)) {
            //cellBodyStr is not a JSON string - if string is a filename return filecontent.
            if (Files.isRegularFile(Paths.get(cellBodyStr))) {
                try {
                    return JsonTools.readFileContent(Paths.get(cellBodyStr));
                } catch (IOException e) {
                    cell.addToBody("Failed reading file with JSON " + contentName + ", error:"+ e.getMessage());
                    getFormatter().wrong(cell, new TextBodyTypeAdapter());
                    return null;
                }
            } else {
                cell.body(contentName + " neither JSON String nor name of file with JSON content.");
                getFormatter().wrong(cell, new TextBodyTypeAdapter());
                return null;
            }
        } else {
            return cellBodyStr;
        }
    }

    /**
     * Get the JSon content from the last GET response body as String.
     * @param lastResponse the last HttpResponse object, should also have header including "Content-type: application/json"
     *
     * @return String with JSON contents.
     * @throws IOException failed reading http response object.
     */
    protected String getJsonString(RestResponse lastResponse) throws IOException {
        debugMethodCallStart();
        if (null == lastResponse) {
            debugMethodCallEnd();
            throw new IOException("No HTTPResponse found, is preceeding HTTP GET or POST call with expected application/json response missing?");
        }
        JavascriptWrapper js = new JavascriptWrapper();
        try {
            Object result = js.evaluateExpression(lastResponse, RESPONSE_JSONBODY);
            if (null == result) {
                LOG.debug("No jsonbody found in response. Cannot extract stringvalue.");
                debugMethodCallEnd();
                return null;
            } else {
                String jsonString = JsonTools.toJSONString(result);
                debugMethodCallEnd();
                return jsonString;
            }
        } catch (Exception e) {
            LOG.warn("Exception: " + e.getClass().getName() + " - " + e.getMessage());
            debugMethodCallEnd();
            throw new IOException("A " + e.getClass().getName() + " has been thrown, errormessage:" + e.getMessage());
        }
    }

    /**
     * Convert string values of True or False to Boolean, explain reason through IOException upon conversion failure.
     *
     * @param txt string with the value "True" or "False".
     * @return converted Boolean value
     * @throws IOException describing why the string was not translated.
     */
    protected Boolean getBoolean(String txt) throws IOException {
        if (null == txt || "".equals(txt.trim())) {
            throw new IOException("Parameter cannot be null or empty, please use True or False");
        }
        if ("True".equalsIgnoreCase(txt.trim())) {
            return Boolean.TRUE;
        } if ("False".equalsIgnoreCase(txt.trim())) {
            return Boolean.FALSE;
        } else {
            throw new IOException("Unknown Boolean value '" + txt + "'. Please use value 'True' or 'False'");
        }
    }
}
