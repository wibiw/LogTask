package org.analyser;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LogTaskTest {
	
	StdOutTestHelper stdOutTestHelper=new StdOutTestHelper();

	@Before
	public void prepare() {
		stdOutTestHelper.setUpStreams();
		clearFileNameField();
	}

	@After
	public void wrapUp() {
		stdOutTestHelper.restoreStreams();
	}
	
	@Test
	public void testUsage(){
		LogTask.main(null);
		assertTrue("should be usage message", !getStdOut().isEmpty());
	}
	
	@Test
	public void testTooMuchParams(){
		String[] args = {"","","",""};
		LogTask.main(args);
		assertTrue("should be message", !getStdOut().isEmpty());
	}

	@Test
	public void testDoubleFileNameParams(){
		String[] args = {"",""};
		LogTask.main(args);
		assertTrue("should be message", !getStdOut().isEmpty());
	}

	
	@Test
	public void testFileNull(){
		String[] args = {null};
		LogTask.main(args);
		assertTrue("should NOT message", !getStdOut().isEmpty());
	}

	@Test
	public void testOneParamOK(){
		String[] args = {""};
		LogTask.main(args);
		assertTrue("should NOT message", !getStdOut().contains("usage"));
	}
	
	@Test
	public void testThreeParamsOK(){
		String[] args = {"","-d","-r"};
		LogTask.main(args);
		assertTrue("should NOT message", getStdOut().isEmpty());
	}
	
	@Test
	public void testCreateTarget(){
		@SuppressWarnings("unused")
		LogTask taskTest=new LogTask();
	}
	
	private String getStdOut() {
		return stdOutTestHelper.getOut().toString();
	}
	
	private void clearFileNameField() {
		try {
			Field field = LogTask.class.getDeclaredField("fileName");
			field.setAccessible(true);
			field.set(LogTask.class, null);
		} catch (Exception e) {
			fail(e.toString());
		}
	}
}
