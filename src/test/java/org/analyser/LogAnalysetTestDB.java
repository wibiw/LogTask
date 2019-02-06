package org.analyser;

import static org.junit.Assert.fail;

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

public class LogAnalysetTestDB {
	
	private static String lines[] = { 
		"{\"id\":\"scsmbstgrb\", \"state\":\"STARTED\", \"timestamp\":1491377495213}",
		"{\"id\":\"scsmbstgrb\", \"state\":\"FINISHED\", \"timestamp\":1491377495216}" };
	
	LogAnalyser logAnalyser=new LogAnalyser(null);
	StdOutTestHelper stdOutTestHelper=new StdOutTestHelper();

	@Before
	public void prepare() {
		stdOutTestHelper.setUpStreams();
		setUpTestDataBase();
	}

	@After
	public void wrapUp() {
		stdOutTestHelper.restoreStreams();
		invokeClose();
	}

	@Test
	public void testInsertRow(){
		insertRow();
	}

	@Test
	public void testReport() {
		insertRow();
		try {
			invokeReport();
		}
		catch(Exception e) {
			fail(e.toString());
		}
	}
	
	private void insertRow() {
		try (PreparedStatement insertStatement = invokeGetConnection()
				.prepareStatement("insert into Events(id ,duration, type, host, alert) values (?,?,?,?,?)")) {
			
			JsonObject entry = new Gson().fromJson(lines[0], JsonObject.class);		
			invokeInserRow(entry, entry, insertStatement);		
		}
		catch(SQLException e) {
			fail(e.toString());
		}
	}
	

	@Test
	public void testProcessFile(){

		Stream<String> stream = Arrays.stream(lines);

		try (PreparedStatement insertStatement = invokeGetConnection()
				.prepareStatement("insert into Events(id ,duration, type, host, alert) values (?,?,?,?,?)")) {

			stream.forEach(e -> invokeProcessEntryLine(e, insertStatement));
		}
		catch(SQLException e) {
			fail(e.toString());
		}
	}
	
	private void setUpTestDataBase() {
		logAnalyser.setUpDataBase("jdbc:hsqldb:mem:", "Analysis", "Events");
	}
	
	private void invokeClose() {
		try {
			Method method = LogAnalyser.class.getDeclaredMethod("close");
			method.setAccessible(true);
			 method.invoke(logAnalyser);
		} catch (Exception e) {
			fail(e.toString());
		}
	}
	
	private void invokeProcessEntryLine(String line, PreparedStatement statement) {
		try {
			Method method = LogAnalyser.class.getDeclaredMethod("processEntryLine", String.class, PreparedStatement.class);
			method.setAccessible(true);
			method.invoke(logAnalyser, line, statement);
		} catch (Exception e) {
			fail(e.toString());
		}
	}
	
	private void invokeInserRow(JsonObject entryStart, JsonObject entryEnd, PreparedStatement insertStatement) {
		try {
			Method method = LogAnalyser.class.getDeclaredMethod("insertRow", JsonObject.class, JsonObject.class, PreparedStatement.class);
			method.setAccessible(true);
			method.invoke(logAnalyser, entryStart, entryEnd, insertStatement);
		} catch (Exception e) {
			fail(e.toString());
		}
	}

	private void invokeReport() {
		try {
			Method method = LogAnalyser.class.getDeclaredMethod("report");
			method.setAccessible(true);
			method.invoke(logAnalyser);
		} catch (Exception e) {
			fail(e.toString());
		}
	}
	
	private Connection invokeGetConnection() {
		try {
			Method method = LogAnalyser.class.getDeclaredMethod("getConnection");
			method.setAccessible(true);
			return (Connection) method.invoke(logAnalyser);
		} catch (Exception e) {
			fail(e.toString());
			return null;
		}
	}
	

}
