package smartrics.rest.fitnesse.fixture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import smartrics.rest.client.RestClient;
import smartrics.rest.client.RestRequest;
import smartrics.rest.client.RestResponse;
import smartrics.rest.fitnesse.fixture.RestFixture.Runner;
import smartrics.rest.fitnesse.fixture.support.BodyTypeAdapter;
import smartrics.rest.fitnesse.fixture.support.CellFormatter;
import smartrics.rest.fitnesse.fixture.support.CellWrapper;
import smartrics.rest.fitnesse.fixture.support.Config;
import smartrics.rest.fitnesse.fixture.support.ContentType;
import smartrics.rest.fitnesse.fixture.support.RowWrapper;
import smartrics.rest.fitnesse.fixture.support.TextBodyTypeAdapter;
import smartrics.rest.fitnesse.fixture.support.Variables;

/**
 * Tests for the JsonRestFixture class.
 *
 * @author Dag Nygaard, Systek AS.
 */
public class JsonRestFixtureTest {
    private static final String LEGAL_JS_STRING = "{\"foo\":\"nay\"}";
    private static final String BASE_URL = "http://localhost:9090";
    private JsonRestFixture fixture;
    private final Variables variables = new Variables();
    private RestFixtureTestHelper helper;
    private PartsFactory mockPartsFactory;
    private RestClient mockRestClient;
    private RestRequest mockLastRequest;
    @SuppressWarnings("rawtypes")
    private CellFormatter mockCellFormatter;
    private Config config;
    private RestResponse lastResponse;
    private BodyTypeAdapter mockBodyTypeAdapter;

    @Before
    public void setUp() {
        helper = new RestFixtureTestHelper();

        mockBodyTypeAdapter = mock(BodyTypeAdapter.class);
        mockCellFormatter = mock(CellFormatter.class);
        mockRestClient = mock(RestClient.class);
        mockLastRequest = mock(RestRequest.class);
        mockPartsFactory = mock(PartsFactory.class);

        variables.clearAll();

        lastResponse = new RestResponse();
        lastResponse.setStatusCode(200);
        lastResponse.setRawBody("".getBytes());
        lastResponse.setResource("/uri");
        lastResponse.setStatusText("OK");
        lastResponse.setTransactionId(0L);

        config = Config.getConfig();

        ContentType.resetDefaultMapping();

        helper.wireMocks(config, mockPartsFactory, mockRestClient, mockLastRequest, lastResponse, mockCellFormatter, mockBodyTypeAdapter);
        fixture = new JsonRestFixture(mockPartsFactory, BASE_URL, Config.DEFAULT_CONFIG_NAME);
        fixture.initialize(Runner.OTHER);
    }

    @After
    public void tearDown() {
        config.clear();
    }

    /* ===================  copyJsonbodyToFile  ======================== */

    @Test
    @SuppressWarnings("unchecked")
    public void mustReportToTheUserIfCopyJsonbodyToFileCellsAreMissing() {
        RowWrapper<?> row = helper.createTestRow("copyJsonbodyToFile"); //filename and result cells are missing.
        fixture.processRow(row);
        verify(mockCellFormatter).exception(isA(CellWrapper.class), eq("Correct number of cells not found: | copyJsonbodyToFile  | fileName | |"));
        verifyNoMoreInteractions(mockCellFormatter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mustReportToTheUserIfCopyJsonbodyToFileCellIsMissing() {
        RowWrapper<?> row = helper.createTestRow("copyJsonbodyToFile", "foo"); //result cell is missing.
        fixture.processRow(row);
        verify(mockCellFormatter).exception(isA(CellWrapper.class), eq("Correct number of cells not found: | copyJsonbodyToFile  | fileName | |"));
        verifyNoMoreInteractions(mockCellFormatter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mustReportToTheUserIfCopyJsonbodyToFileTooManyCells() {
        RowWrapper<?> row = helper.createTestRow("copyJsonbodyToFile", "filename", "result", "a bridge too far"); //too many cells
        fixture.processRow(row);
        verify(mockCellFormatter).exception(isA(CellWrapper.class), eq("Correct number of cells not found: | copyJsonbodyToFile  | fileName | |"));
        verifyNoMoreInteractions(mockCellFormatter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mustReportToTheUserIfCopyJsonbodyToFileCellsFilenameIsEmpty() {
        RowWrapper<?> row = helper.createTestRow("copyJsonbodyToFile", "   ", "result"); //filename cell is just blank
        fixture.processRow(row);
        verify(mockCellFormatter).exception(isA(CellWrapper.class), eq("fileName cannot be empty: | copyJsonbodyToFile  | fileName | |"));
        verifyNoMoreInteractions(mockCellFormatter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mustReportToTheUserIfCopyJsonbodyToFileThrowsIOExceptionForgotGET() throws Exception {
        String json = "{\"test\":\"me\"}";
        String xpath = "/test[text()='me']";
        when(mockBodyTypeAdapter.parse(xpath)).thenReturn(xpath);
        when(mockBodyTypeAdapter.equals(xpath, json)).thenReturn(true);
        when(mockLastRequest.getQuery()).thenReturn("");
        when(mockRestClient.getBaseUrl()).thenReturn(BASE_URL);
        RowWrapper<?> row = helper.createTestRow("copyJsonbodyToFile", "./js.txt", "result"); //filename include nonexisting directory.
        fixture.processRow(row);
        assertNull(fixture.getLastResponse());
        verify(mockCellFormatter).exception(isA(CellWrapper.class), eq("No HTTPResponse found, is preceeding HTTP GET or POST call with expected application/json response missing?"));
        verifyNoMoreInteractions(mockCellFormatter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mustReportToTheUserIfCopyJsonbodyToFileThrowsIOExceptionMissingDirectory() throws Exception {
        String json = "{\"test\":\"me\"}";
        String xpath = "/test[text()='me']";
        when(mockBodyTypeAdapter.parse(xpath)).thenReturn(xpath);
        when(mockBodyTypeAdapter.equals(xpath, json)).thenReturn(true);
        when(mockLastRequest.getQuery()).thenReturn("");
        when(mockRestClient.getBaseUrl()).thenReturn(BASE_URL);
        lastResponse.setResource("/uri");
        lastResponse.addHeader("Content-Type", "application/json");
        lastResponse.setBody(json);
        RowWrapper<?> row = helper.createTestRow("GET", "/uri", "", "", "");
        fixture.processRow(row);
        assertNotNull(fixture.getLastResponse());
        row = helper.createTestRow("copyJsonbodyToFile", "./fz/js.txt", "result"); //filename include nonexisting directory.
        fixture.processRow(row);
        verify(mockCellFormatter).exception(isA(CellWrapper.class), eq("IO Error:java.nio.file.NoSuchFileException:.\\fz\\js.txt"));
    }

    @Test
    public void mustReportToTheUserIfCopyJsonbodyToFileThrowsIOException() throws Exception {
        String json = "{\"test\":\"me\"}";
        String xpath = "/test[text()='me']";
        when(mockBodyTypeAdapter.parse(xpath)).thenReturn(xpath);
        when(mockBodyTypeAdapter.equals(xpath, json)).thenReturn(true);
        when(mockLastRequest.getQuery()).thenReturn("");
        when(mockRestClient.getBaseUrl()).thenReturn(BASE_URL);
        lastResponse.setResource("/uri");
        lastResponse.addHeader("Content-Type", "application/json");
        lastResponse.setBody(json);
        RowWrapper<?> row = helper.createTestRow("GET", "/uri", "", "", "");
        fixture.processRow(row);
        String foobar = fixture.getJsonString(lastResponse);
        assertNotNull(foobar);
        assertEquals("{\"test\":\"me\"}", foobar);
    }

    /* ===================  showJsonFileContent  ======================== */

    @Test
    @SuppressWarnings("unchecked")
    public void mustReportToTheUserIfShowJsonFileContentCellsAreMissing() {
        RowWrapper<?> row = helper.createTestRow("showJsonFileContent"); //filename and result cells are missing.
        fixture.processRow(row);
        verify(mockCellFormatter).exception(isA(CellWrapper.class), eq("Not all cells found: | showJsonFileContent  | fileName | |"));
        verifyNoMoreInteractions(mockCellFormatter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mustReportToTheUserIfShowJsonFileContentCellIsMissing() {
        RowWrapper<?> row = helper.createTestRow("showJsonFileContent", "foo"); //result cell is missing.
        fixture.processRow(row);
        verify(mockCellFormatter).exception(isA(CellWrapper.class), eq("Not all cells found: | showJsonFileContent  | fileName | |"));
        verifyNoMoreInteractions(mockCellFormatter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mustReportToTheUserIfShowJsonFileContentCellsFilenameIsEmpty() {
        RowWrapper<?> row = helper.createTestRow("showJsonFileContent", "   ", "result"); // filename cell is just blank
        fixture.processRow(row);
        verify(mockCellFormatter).exception(isA(CellWrapper.class), eq("fileName cannot be empty: | showJsonFileContent  | fileName | |"));
        verifyNoMoreInteractions(mockCellFormatter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mustReportToTheUserIfShowJsonFileContentFileDoesNotExist() {
        String nonExistingFilename = "./fz/989/foo.json";
        RowWrapper<?> row = helper.createTestRow("showJsonFileContent", nonExistingFilename, "result"); // filename cell is just blank
        fixture.processRow(row);
        verify(mockCellFormatter).exception(isA(CellWrapper.class), eq("File '" + nonExistingFilename + "' does not exist: | showJsonFileContent  | fileName | |"));
        verifyNoMoreInteractions(mockCellFormatter);
    }

    /* ===================  jsCompare  ======================== */

    @Test
    @SuppressWarnings("unchecked")
    public void mustReportToTheUserIfJsCompareCellsAreMissing() {
        RowWrapper<?> row = helper.createTestRow("jsCompare"); //missing parameters: strict, expected, actual, result
        fixture.processRow(row);
        row = helper.createTestRow("jsCompare", "false"); //missing parameters: expected, actual, result
        fixture.processRow(row);
        row = helper.createTestRow("jsCompare", "false", "{\"foo\": \"bar\"}"); //missing parameters: actual, result
        fixture.processRow(row);
        row = helper.createTestRow("jsCompare", "false", "{\"foo\": \"bar\"}", LEGAL_JS_STRING); //missing parameters: result
        fixture.processRow(row);
        verify(mockCellFormatter, times(4)).exception(isA(CellWrapper.class), eq("Not all cells found: | jsCompare | strict | actual | expected | result |"));
        verifyNoMoreInteractions(mockCellFormatter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mustReportToTheUserIfJsCompareFaultyParamStrict() {
        String faultyStrictParam = "FAULT";
        RowWrapper<?> row = helper.createTestRow("jsCompare", faultyStrictParam, "{\"foo\": \"bar\"}", LEGAL_JS_STRING, " ");
        fixture.processRow(row);
        verify(mockCellFormatter).exception(isA(CellWrapper.class), eq("Parameter strict has not been set correctly:Unknown Boolean value '" + faultyStrictParam + "'. Please use value 'True' or 'False'"));
        verifyNoMoreInteractions(mockCellFormatter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mustNotReportToTheUserIfJsCompareLegalParamStrict() {
        RowWrapper<?> row = helper.createTestRow("jsCompare", "True", "{\"foo\": \"bar\"}", LEGAL_JS_STRING, " ");
        fixture.processRow(row);
        row = helper.createTestRow("jsCompare", "true", "{\"foo\": \"bar\"}", LEGAL_JS_STRING, " ");
        fixture.processRow(row);
        row = helper.createTestRow("jsCompare", "False", "{\"foo\": \"bar\"}", LEGAL_JS_STRING, " ");
        fixture.processRow(row);
        row = helper.createTestRow("jsCompare", "false", "{\"foo\": \"bar\"}", LEGAL_JS_STRING, " ");
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void mustReportToTheUserIfJsCompareFaultyParamExpected() {
        RowWrapper<?> row = helper.createTestRow("jsCompare", "false", "noFile", LEGAL_JS_STRING, " ");
        ArgumentCaptor<CellWrapper> argument = ArgumentCaptor.forClass(CellWrapper.class);
        fixture.processRow(row);
        verify(mockCellFormatter).wrong(argument.capture(), any(TextBodyTypeAdapter.class));
        verify(argument.getValue()).body(eq("ActualContent neither JSON String nor name of file with JSON content."));
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void mustReportToTheUserIfJsCompareFaultyParamActual() {
        RowWrapper<?> row = helper.createTestRow("jsCompare", "false", LEGAL_JS_STRING, "noFile", " ");
        fixture.processRow(row);
        ArgumentCaptor<CellWrapper> argument = ArgumentCaptor.forClass(CellWrapper.class);
        verify(mockCellFormatter).wrong(argument.capture(), any(TextBodyTypeAdapter.class));
        verify(argument.getValue()).body(eq("ExpectedContent neither JSON String nor name of file with JSON content."));
    }

    @Test
    public void mustReportToTheUserIfJsCompareStrictJsJsEquals() {
        String strictStr = "true";
        ArgumentCaptor<String> argument = createAndProcessRow(strictStr, LEGAL_JS_STRING, LEGAL_JS_STRING);
         assertEquals("[No deviations found.]", argument.getValue().toString());
    }

    @Test
    public void mustReportToTheUserIfJsCompareStrictJsFileEquals() throws IOException {
        String strictStr = "true";
        String tmpFileName = createTmpJsFile(LEGAL_JS_STRING);
        ArgumentCaptor<String> argument = createAndProcessRow(strictStr, LEGAL_JS_STRING, tmpFileName);
         assertEquals("[No deviations found.]", argument.getValue().toString());
         Files.deleteIfExists(FileSystems.getDefault().getPath(tmpFileName));
    }

    @Test
    public void mustReportToTheUserIfJsCompareStrictFileJsEquals() throws IOException {
        String strictStr = "true";
        String tmpFileName = createTmpJsFile(LEGAL_JS_STRING);
        ArgumentCaptor<String> argument = createAndProcessRow(strictStr, tmpFileName, LEGAL_JS_STRING);
         assertEquals("[No deviations found.]", argument.getValue().toString());
         Files.deleteIfExists(FileSystems.getDefault().getPath(tmpFileName));
    }

    @Test
    public void mustReportToTheUserIfJsCompareStrictFileFileEquals() throws IOException {
        String strictStr = "true";
        String tmpFileName = createTmpJsFile(LEGAL_JS_STRING);
        ArgumentCaptor<String> argument = createAndProcessRow(strictStr, tmpFileName, tmpFileName);
         assertEquals("[No deviations found.]", argument.getValue().toString());
         Files.deleteIfExists(FileSystems.getDefault().getPath(tmpFileName));
    }

    @Test
    public void mustReportToTheUserIfJsCompareNonStrictJsJsEquals() {
        String strictStr = "false";
        ArgumentCaptor<String> argument = createAndProcessRow(strictStr, LEGAL_JS_STRING, LEGAL_JS_STRING);
         assertEquals("[No deviations found.]", argument.getValue().toString());
    }

    @Test
    public void mustReportToTheUserIfJsCompareNonStrictJsFileEquals() throws IOException {
        String strictStr = "false";
        String tmpFileName = createTmpJsFile(LEGAL_JS_STRING);
        ArgumentCaptor<String> argument = createAndProcessRow(strictStr, LEGAL_JS_STRING, tmpFileName);
         assertEquals("[No deviations found.]", argument.getValue().toString());
         Files.deleteIfExists(FileSystems.getDefault().getPath(tmpFileName));
    }

    @Test
    public void mustReportToTheUserIfJsCompareNonStrictFileJsEquals() throws IOException {

        String strictStr = "false";
        String tmpFileName = createTmpJsFile(LEGAL_JS_STRING);
        ArgumentCaptor<String> argument = createAndProcessRow(strictStr, tmpFileName, LEGAL_JS_STRING);
         assertEquals("[No deviations found.]", argument.getValue().toString());
         Files.deleteIfExists(FileSystems.getDefault().getPath(tmpFileName));
    }

    @Test
    public void mustReportToTheUserIfJsCompareNonStrictFileFileEquals() throws IOException {
        String strictStr = "false";
        String tmpFileName = createTmpJsFile(LEGAL_JS_STRING);
        ArgumentCaptor<String> argument = createAndProcessRow(strictStr, tmpFileName, tmpFileName);
         assertEquals("[No deviations found.]", argument.getValue().toString());
         Files.deleteIfExists(FileSystems.getDefault().getPath(tmpFileName));
    }

    /**
     * Test the following string:
     * { "a":"b", "c:d", "e":{ "f":"g", "h":"i"}}
     */
    private static String LEGAL_COMPLEX_JS_STRING = "{ \"a\":\"b\", \"c\":\"d\", \"e\":{ \"f\":\"g\", \"h\":\"i\"}}";

    /**
     * Differ LEGAL_COMPLEX_JS_STRING in order only.
     * Strict is True.
     *
     * Expect Green - no deviations.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareStrictOrderDifference() {
        String strictStr = "true";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr, LEGAL_COMPLEX_JS_STRING,
                "{ \"c\":\"d\", \"a\":\"b\",  \"e\":{ \"h\":\"i\", \"f\":\"g\"}}", " ");
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(mockCellFormatter, times(1)).label(any(String.class));
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockCellFormatter).label(argument.capture());
        assertEquals("[No deviations found.]", argument.getValue().toString());
    }

    /**
     * Differ LEGAL_COMPLEX_JS_STRING in order only.
     * Strict is FALSE.
     *
     * Expect Green - no deviations.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareNONStrictOrderDifference() {
        String strictStr = "false";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr, LEGAL_COMPLEX_JS_STRING,
                "{ \"c\":\"d\", \"a\":\"b\",  \"e\":{ \"h\":\"i\", \"f\":\"g\"}}", " ");
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(mockCellFormatter, times(1)).label(any(String.class));
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockCellFormatter).label(argument.capture());
        assertEquals("[No deviations found.]", argument.getValue().toString());
    }

    /**
     * Differ LEGAL_COMPLEX_JS_STRING in expected value.
     * Strict is True.
     *
     * Expects "fail" in resultcell.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareStrictValueDifferenceInExpected1() {
        String strictStr = "true";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr, LEGAL_COMPLEX_JS_STRING,
                "{ \"c\":\"d\", \"a\":\"x\",  \"e\":{ \"h\":\"i\", \"f\":\"g\"}}", " "); //item 'h' has new expected value x.
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(row.getCell(4), times(1)).body(startsWith("fail:<div>a\nExpected: x\n     got: b"));
    }

    /**
     * Differ LEGAL_COMPLEX_JS_STRING in expected value.
     * Strict is True.
     *
     * Expects "fail" in resultcell.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareStrictValueDifferenceInExpected2() {
        String strictStr = "true";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr, LEGAL_COMPLEX_JS_STRING,
                "{ \"c\":\"d\", \"a\":\"b\",  \"e\":{ \"h\":\"x\", \"f\":\"g\"}}", " "); //item 'h' has new expected value x.
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(row.getCell(4), times(1)).body(startsWith("fail:<div>e.h\nExpected: x\n     got: i"));
    }

    /**
     * Differ LEGAL_COMPLEX_JS_STRING in expected value.
     * Strict is False.
     *
     * Expects "fail" in resultcell.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareNONStrictValueDifferenceInExpected1() {
        String strictStr = "false";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr, LEGAL_COMPLEX_JS_STRING,
                "{ \"c\":\"d\", \"a\":\"x\",  \"e\":{ \"h\":\"i\", \"f\":\"g\"}}", " "); //item 'a' has new expected value x.
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(row.getCell(4), times(1)).body(startsWith("fail:<div>a\nExpected: x\n     got: b"));
    }

    /**
     * Differ LEGAL_COMPLEX_JS_STRING in expected value.
     * Strict is False.
     *
     * Expects "fail" in resultcell.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareNONStrictValueDifferenceInExpected2() {
        String strictStr = "true";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr, LEGAL_COMPLEX_JS_STRING,
                "{ \"c\":\"d\", \"a\":\"b\",  \"e\":{ \"h\":\"x\", \"f\":\"g\"}}", " "); //item 'h' has new expected value x.
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(row.getCell(4), times(1)).body(startsWith("fail:<div>e.h\nExpected: x\n     got: i"));
    }

    /**
     * Differ LEGAL_COMPLEX_JS_STRING field in actual is not listed in expected.
     * Strict is True.
     *
     * Expect Red -failure
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareStrictMissingFieldinExpected1() {
        String strictStr = "true";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr, LEGAL_COMPLEX_JS_STRING,
                "{ \"c\":\"d\", \"a\":\"b\", \"e\":{ \"f\":\"g\"}}", " "); // 'h' is missing from expected
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(row.getCell(4), times(1)).body(startsWith("fail:<div>e\nUnexpected: h"));
        verify(mockCellFormatter, times(0)).label(any(String.class));
    }

    /**
     * Differ LEGAL_COMPLEX_JS_STRING field in actual is not listed in expected.
     * Strict is True.
     *
     * Expect Red
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareNONStrictMissingFieldinExpected2() {
        String strictStr = "false";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr, LEGAL_COMPLEX_JS_STRING,
                "{ \"c\":\"d\",  \"e\":{ \"h\":\"i\", \"f\":\"g\"}}", " "); //'a' is missing in expected.
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(row.getCell(4), times(1)).body(startsWith("error:<div>\nExpected: a\n     but none found"));
        verify(mockCellFormatter, times(0)).label(any(String.class));
    }


    /**
     * Differ LEGAL_COMPLEX_JS_STRING field in actual is not listed in expected.
     * Strict is True.
     *
     * Expect yellow - warning (exception)
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareNONStrictMissingFieldinExpected1() {
        String strictStr = "false";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr, LEGAL_COMPLEX_JS_STRING,
                "{ \"c\":\"d\", \"a\":\"b\", \"e\":{ \"f\":\"g\"}}", " "); // 'h' is missing from expected
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(row.getCell(4), times(1)).body(startsWith("error:<div>e\nExpected: h\n     but none found"));
        verify(mockCellFormatter, times(0)).label(any(String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareNONStrictMissingFieldInExpected3() {
        String strictStr = "false";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr,
                "{ \"name\":\"Norway\",\"id\":2,\"cars\":[{\"make\":\"GM\",\"color\":\"blue\"},{\"make\":\"BMW\",\"color\":\"red\"}]}",
                "{\"id\":2,\"name\":\"Norway\",\"cars\":[{\"make\":\"BMW\",\"color\":\"red\"}]}", " ");
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(row.getCell(4), times(1)).body(eq("error:<div>cars[]: Expected 1 values but got 2</div>"));
        verify(mockCellFormatter, times(0)).label(any(String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareNONStrictMissingFieldInActual3() {
        String strictStr = "false";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr,
                "{\"id\":2,\"name\":\"Norway\",\"cars\":[{\"make\":\"BMW\",\"color\":\"red\"}]}",
                "{ \"name\":\"Norway\",\"id\":2,\"cars\":[{\"make\":\"GM\",\"color\":\"blue\"},{\"make\":\"BMW\",\"color\":\"red\"}]}",
                " ");
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(row.getCell(4), times(1)).body(startsWith("fail:"));
        verify(mockCellFormatter, times(0)).label(any(String.class));
    }
    /**
     * Differ LEGAL_COMPLEX_JS_STRING field in expected is not listed in actual.
     * Strict is True.
     *
     * Expect Red -failure
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareStrictMissingFieldinActual1() {
        String strictStr = "true";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr,
                "{ \"c\":\"d\", \"a\":\"b\", \"e\":{ \"f\":\"g\"}}",
                LEGAL_COMPLEX_JS_STRING," "); // 'h' is missing from actual
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(row.getCell(4), times(1)).body(startsWith("fail:<div>e\nExpected: h\n     but none found"));
        verify(mockCellFormatter, times(0)).label(any(String.class));
    }

    /**
     * Differ LEGAL_COMPLEX_JS_STRING field in expected is not listed in actual.
     * Strict is True.
     *
     * Expect Red -failure
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareStrictMissingFieldinActual2() {
        String strictStr = "true";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr,
                "{ \"c\":\"d\", \"e\":{ \"h\":\"i\", \"f\":\"g\"}}",
                LEGAL_COMPLEX_JS_STRING," "); // 'a' is missing from actual
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(row.getCell(4), times(1)).body(startsWith("fail:<div>\nExpected: a\n     but none found"));
        verify(mockCellFormatter, times(0)).label(any(String.class));
    }

    /**
     * Differ LEGAL_COMPLEX_JS_STRING field in expected is not listed in actual.
     * Strict is false.
     *
     * Expect Red -failure
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareNONStrictMissingFieldinActual1() {
        String strictStr = "false";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr,
                "{ \"c\":\"d\", \"a\":\"b\", \"e\":{ \"f\":\"g\"}}",
                LEGAL_COMPLEX_JS_STRING," "); // 'h' is missing from actual
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(row.getCell(4), times(1)).body(startsWith("fail:<div>"));
        verify(mockCellFormatter, times(0)).label(any(String.class));
    }

    /**
     * Differ LEGAL_COMPLEX_JS_STRING field in expected is not listed in actual.
     * Strict is false.
     *
     * Expect Red -failure
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testJsCompareNONStrictMissingFieldinActual2() {
        String strictStr = "false";
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr,
                "{ \"c\":\"d\", \"e\":{ \"h\":\"i\", \"f\":\"g\"}}",
                LEGAL_COMPLEX_JS_STRING," "); // 'h' is missing from actual
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(row.getCell(4), times(1)).body(startsWith("fail:<div>"));
        verify(mockCellFormatter, times(0)).label(any(String.class));
    }

    private String createTmpJsFile(String legalJsString) throws IOException {
        Path tmpFilePath = FileSystems.getDefault().getPath("tmpFitneseTestJsfile.tmp");
        if (Files.exists(tmpFilePath)) {
            Files.delete(tmpFilePath);
        }
        Path tmpFile = Files.createFile(tmpFilePath);
        Files.write(tmpFile, legalJsString.getBytes());
        return tmpFile.toAbsolutePath().toString();
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<String> createAndProcessRow(String strictStr, String expected, String actual) {
        RowWrapper<?> row = helper.createTestRow("jsCompare", strictStr, expected, actual, " ");
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), any(String.class));
        verify(mockCellFormatter, times(0)).wrong(any(CellWrapper.class),  any(TextBodyTypeAdapter.class));
        verify(mockCellFormatter, times(1)).label(any(String.class));
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mockCellFormatter).label(argument.capture());
        return argument;
    }

    /* ===================  jsonAssertCompare  ======================== */

    @Test
    @SuppressWarnings("unchecked")
    public void mustReportToTheUserIfJsonAssertCompareCellsAreMissing() {
        RowWrapper<?> row = helper.createTestRow("jsonAssertCompare"); //missing parameters: strict, expected, actual, result
        fixture.processRow(row);
        row = helper.createTestRow("jsonAssertCompare", "LENIENT"); //missing parameters: expected, actual, result
        fixture.processRow(row);
        row = helper.createTestRow("jsonAssertCompare", "LENIENT", "{\"foo\": \"bar\"}"); //missing parameters: actual, result
        fixture.processRow(row);
        row = helper.createTestRow("jsonAssertCompare", "LENIENT", "{\"foo\": \"bar\"}", LEGAL_JS_STRING); //missing parameters: result
        fixture.processRow(row);
        verify(mockCellFormatter, times(4)).exception(isA(CellWrapper.class), eq("Not all cells found: | jsonAssertCompare | jsonCompareMode | actual | expected | result |"));
        verifyNoMoreInteractions(mockCellFormatter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void jsonAssertCompareHappyDay() {
        RowWrapper<?> row = helper.createTestRow("jsonAssertCompare", "LENIENT", LEGAL_JS_STRING, LEGAL_JS_STRING, "");
        fixture.processRow(row);
        verify(mockCellFormatter, times(0)).exception(isA(CellWrapper.class), eq("Not all cells found: | jsonAssertCompare | jsonCompareMode | actual | expected | result |"));
        verify(row.getCell(4), times(1)).body(startsWith("fail:<div>null</div>"));
    }

    /* ===================  getJsonString  ======================== */

    @Test
    public void mustThrowExceptionForGetJsonSTringIfLastHttpCallIsMissing() {
        try {
            fixture.getJsonString(null);
            fail("Should have thrown an exception.");
        } catch (IOException e) {
            assertEquals("No HTTPResponse found, is preceeding HTTP GET or POST call with expected application/json response missing?", e.getMessage());
        }
    }

    @Test
    public void verifyGetJsonStringNullIsOk() throws IOException {
        RestResponse someRestResponse = new RestResponse();
        assertNull(fixture.getJsonString(someRestResponse));
    }

    @Test
    public void verifyGetJsonStringNullIsOk2() throws IOException {
        assertNull(fixture.getJsonString(this.lastResponse));
    }

    @Test
    public void verifyGetJsonStringLegalJsIsOk() throws IOException {
        lastResponse.setBody(LEGAL_JS_STRING);
        assertEquals(LEGAL_JS_STRING, fixture.getJsonString(this.lastResponse));
    }

    @Test
    public void verifyGetJsonStringNonJsonResponseReturnNull() throws IOException {
        lastResponse.setBody("{\"foo\"");
        assertNull(fixture.getJsonString(this.lastResponse));
    }

    /* ===================  getBoolean  ======================== */

    @Test
    public void verifyGetBooleanWorksOk() {
        try {
            assertEquals(Boolean.TRUE, fixture.getBoolean("true"));
            assertEquals(Boolean.TRUE, fixture.getBoolean("True"));
            assertEquals(Boolean.TRUE, fixture.getBoolean("true "));
            assertEquals(Boolean.TRUE, fixture.getBoolean("True    "));
            assertEquals(Boolean.TRUE, fixture.getBoolean("TRUE"));
            assertEquals(Boolean.FALSE, fixture.getBoolean("false"));
            assertEquals(Boolean.FALSE, fixture.getBoolean("False"));
            assertEquals(Boolean.FALSE, fixture.getBoolean("false "));
            assertEquals(Boolean.FALSE, fixture.getBoolean("False    "));
            assertEquals(Boolean.FALSE, fixture.getBoolean("FALSE"));
        } catch (IOException e) {
            fail("Should not have thrown exception e:" + e.getMessage());
        }
        assertTrue(failGetBoolean(null));
        assertTrue(failGetBoolean("   "));
        assertTrue(failGetBoolean("random"));
        assertTrue(failGetBoolean("123"));
    }

    private boolean failGetBoolean(String str) {
        try {
            fixture.getBoolean(str);
            return false;
        } catch (IOException e) {
            return true;
        }
    }

}
