package org.analyser;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;


public class LogAnalyserTestMethod {

	LogAnalyser logAnalyser=new LogAnalyser(null);
	StdOutTestHelper stdOutTestHelper=new StdOutTestHelper();

	@Before
	public void prepare() {
		stdOutTestHelper.setUpStreams();
	}

	@After
	public void wrapUp() {
		stdOutTestHelper.restoreStreams();
	}
	
	
	@Test
	public void testCheckMandatoryFieldsOK() {
		String logFileLineOK = "{\"id\":\"scsmbstgrb\", \"state\":\"STARTED\", \"timestamp\":1491377495213}";
		JsonObject entryOk = new Gson().fromJson(logFileLineOK, JsonObject.class);
		assertTrue("checkMandatoryFields OK - expected true", invokeCheckMandatoryFields(logFileLineOK, entryOk));
	}

	@Test
	public void testCheckMandatoryFieldsNoId() {
		String logFileLineNoId = "{\"state\":\"STARTED\", \"timestamp\":1491377495213}";
		JsonObject entryNoId = new Gson().fromJson(logFileLineNoId, JsonObject.class);
		assertTrue("checkMandatoryFields no Id - expexted false", !invokeCheckMandatoryFields(logFileLineNoId, entryNoId));
	}

	@Test
	public void testCheckMandatoryFieldsNoTS() {
		String logFileLineNoTS = "{\"id\":\"scsmbstgrb\", \"state\":\"STARTED\"}";
		JsonObject entryNoTS = new Gson().fromJson(logFileLineNoTS, JsonObject.class);
		assertTrue("checkMandatoryFields no TimeStamp - expexted false", !invokeCheckMandatoryFields(logFileLineNoTS, entryNoTS));
	}	
	
	@Test
	public void testFieldsEqualSame() {
		JsonObject jsonObj = new Gson().fromJson("{\"id\":\"val\"}", JsonObject.class);
		assertTrue("checkFieldsEqual TRUE", invokeFieldsEqual(jsonObj, jsonObj, "id"));
	}

	
	@Test
	public void testFieldsEqualDiff() {
		JsonObject jsonABC = new Gson().fromJson("{\"id\":\"abc\"}", JsonObject.class);
		JsonObject jsonXYZ = new Gson().fromJson("{\"id\":\"xyz\"}", JsonObject.class);
		assertTrue("checkFieldsEqual FALSE", !invokeFieldsEqual(jsonABC, jsonXYZ, "id"));
	}
	
	
	private boolean invokeCheckMandatoryFields(String line, JsonObject entry) {

		try {
			Method method = LogAnalyser.class.getDeclaredMethod("checkMandatoryFields", String.class, JsonObject.class);
			method.setAccessible(true);
			return (Boolean) method.invoke(logAnalyser, line, entry);
		} catch (Exception e) {
			fail(e.toString());
			return false;
		}
	}	
	
	private boolean invokeFieldsEqual(JsonObject a, JsonObject b, String key) {

		try {
			Method method = LogAnalyser.class.getDeclaredMethod("fieldsEqual", JsonObject.class, JsonObject.class, String.class);
			method.setAccessible(true);
			return (Boolean) method.invoke(logAnalyser, a, b, key);
		} catch (Exception e) {
			fail(e.toString());
			return false;
		}
	}	

}
