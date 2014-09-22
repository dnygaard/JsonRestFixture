JsonRestFixture
===============
Let testers test Json REST calls writing tests in FitNesse framework (http://fitnesse.org).


Background
----------
RestFixture is a good tool to do technical REST call testing. What it lacks is a non-developer-friendly interface.

JsonRestFixture has been forked from RestFixture primarily to:
1. Fix a Fitnesse html-formatting bug (fixed by commit/pull request #69 cloneURL: https://github.com/nuggit32/RestFixture.git and possibly fixed in Fitnesse version 20140901 - not confirmed.)
2. Add Json library tools to enable users to do String-based Json comparing instead of XPath comparing.

The fixture allows test-writers to express tests as actions (any of the
allowed HTTP methods) to operate on resource URIs and express expectations on
the content of the return code, headers and body. All without writing one
single line of Java code.

The fixture can be used with both Fit (FitRestFixture) and Slim (Table:Rest Fixture) runners.

Changes
-------
Changes 20140918
- Add JsonRestFixture to accomodate Json string specific REST call testing.
(See JsonRestFixture API for details.)
- Add Jackson Json tools.

- Requirement update: now uses Java 1.7. 

- Developers must add new Json libraries to Fitnesse lib directory.


Changes 20140917
- Update pom with newer versions including Fitnesse updated to 20140901.

Forked 20140916 from  RestFixture (https://github.com/smartrics/RestFixture)


RestFixture Documentation
--------------------------
- https://github.com/smartrics/RestFixture
- http://github.com/smartrics/RestFixtureLiveDoc

Install
--------
Using Java 1.7 or newer, build code:
>mvn clean package

Install FitNesse - see FitNesse documentations for details.

Deploy built JsonRestFixture.jar and *all other libraries* from ./target/ and ./target/dependencies/ directories respectvily to the FitNesse ./lib/ directory.

Start up FitNesse:
>java -jar fitnesse-standalone.jar 

Open the FitNesse test in your browser:
>http://localhost/


Json REST call specific FitNesse test
-------------------------------------

<pre>
!define TEST_SYSTEM {slim}

!path lib/*.jar

|Table:smartrics.rest.fitnesse.fixture.RestFixtureConfig|
|restfixture.display.actual.on.right|true|
|restfixture.content.default.charset|UTF-8|

!define expectedJsonReturnHeaders {Content-Type:application/json}


!3 Get the Json contents from a live web service that usually return a valid application/json response and place contents into a local file

|!-Table:smartrics.rest.fitnesse.fixture.JsonRestFixture-! | http://service.someurl.com|
|GET|/country/search?text=Nor|200|!-Content-Type : application/json;charset=UTF-8
Vary : Accept-Encoding
Accept-Ranges : none
Transfer-Encoding : chunked-!| |
| let | body_value |js | response.jsonbody | [object Object] |
| copyJsonbodyToFile | ./files/fileAAA.txt | |


!3 Show the Json formatted contents from a local file

The file is the same one as used above.

!Show json file content
|!-Table:smartrics.rest.fitnesse.fixture.JsonRestFixture-! |http://localhost|
| showJsonFileContent |./files/fileAAA.txt| |

!3 Do some Json comparisons on strings and files.

The file is the same as the one used above.


|!-Table:smartrics.rest.fitnesse.fixture.JsonRestFixture-! |http://localhost|
| jsCompare |false|{ "name":"Norway","id":2,"cars":[{"make":"GM","color":"blue"},{"make":"BMW","color":"red"}]}  |{"id":2,"name":"Norway","cars":[{"make":"GM","color":"blue"},{"make":"BMW","color":"red"}]}| Green -  Should not find any deviations - this comment is ignored.|
| jsCompare |false| ./files/fileAAA.txt |{"RestResponse" : { "result" : [ {"name" : "Northern Mariana Islands", "alpha2_code" : "MP","alpha3_code" : "MNP"},{"name" : "Norway", "alpha2_code" : "NO", "alpha3_code" : "NOR"}]}} ||
| jsCompare |true| ./files/fileAAA.txt |./files/fileAAA.txt ||
</pre>


