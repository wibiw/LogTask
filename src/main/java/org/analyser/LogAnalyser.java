package org.analyser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 
 * @author wb
 *
 */
public class LogAnalyser {
	private static final long ALERT_TIME_MS = 4;
	
	private static final String FIELD_ID 		= "id";
	private static final String FIELD_TYPE 		= "type";
	private static final String FIELD_HOST 		= "host";
	private static final String FIELD_TIMESTAMP = "timestamp";
	
	private static final String DEFAULT_PROTOCOL	= "jdbc:hsqldb:file:";
	private static final String DEFAULT_DB_NAME		= "Analysis";
	private static final String DEFAULT_TABLE_NAME	= "Events";

	private static Logger logger=LogManager.getLogger(LogAnalyser.class.getName());	
	private List<JsonObject> waitingList=Collections.synchronizedList(new ArrayList<JsonObject>());
	private Connection connection=null;	

	private boolean deleteData=false;
	private boolean showResult=false;	
	private String fileName=null;
	
	private String protocol  = DEFAULT_PROTOCOL;
	private String dbName    = DEFAULT_DB_NAME;
	private String tableName = DEFAULT_TABLE_NAME;	
	/**
	 * Constructor
	 * @param fileName - log file name
	 */
	public LogAnalyser(String fileName) {
		this(fileName, false, false);
	}
	
	/**
	 * Constructor
	 * @param fileName - log file name
	 * @param deleteData - removes from database
	 * @param showResult - prints content of event table from database
	 */
	public LogAnalyser(String fileName, boolean deleteData, boolean showResult) {
		this.fileName=fileName;
		this.deleteData=deleteData;
		this.showResult=showResult;
	}

	/**
	 * Sets up connection to database
	 * @param protocol
	 * @param dBName
	 * @param tableName
	 */
	public void setUpDataBase(String protocol, String dBName, String tableName) {
		this.protocol=protocol;
		this.dbName=dBName;
		this.tableName=tableName;
	}
	
	/**
	 * process log file
	 */
	public void run() {
		try {			
			processFile();
			if (showResult) {
				report();
			}
			close();
		} catch (Exception e) {
			logger.error(e);
		}
	}
	
	
	private void processFile() throws IOException, SQLException, ClassNotFoundException {
		logger.info("processing file: "+fileName);
		try (PreparedStatement insertStatement = getConnection()
				.prepareStatement("insert into " + tableName + "(id ,duration, type, host, alert) values (?,?,?,?,?)");
				Stream<String> stream = Files.lines(Paths.get(fileName)).parallel()) {
			stream.forEach(e -> processEntryLine(e, insertStatement));
		}
	}
	
	private void processEntryLine(String entryLine, PreparedStatement ps){	
		
		JsonObject currentEntry = new Gson().fromJson(entryLine, JsonObject.class);
		
		if(!checkMandatoryFields(entryLine, currentEntry)) {
			return;
		}
		
		JsonObject matchingEntry = 
				waitingList.stream()
				.filter(e -> compareId(e, currentEntry))
				.findFirst()
				.orElse(null);

		if (matchingEntry == null) {
			waitingList.add(currentEntry);
		} else {
			if(checkOtherFields(currentEntry, matchingEntry)){
				insertRow(matchingEntry, currentEntry, ps);
			}
			waitingList.remove(matchingEntry);
		}
	}

	
	private String getId(JsonObject entry) {
		return entry.get(FIELD_ID).getAsString();
	}
	
	private long getTimeStamp(JsonObject entry) {
		return entry.get(FIELD_TIMESTAMP).getAsLong();
	}
	
	private boolean compareId(JsonObject a, JsonObject b) {
		String aId=getId(a);
		String bId=getId(b);
		return aId.equals(bId);
	}

	
	private boolean checkMandatoryFields(String line, JsonObject entry) {
		if(entry.get(FIELD_ID)==null) {
			logger.error("id for ident is not set, Line: "+line);
			return false;
		}
		if(entry.get(FIELD_TIMESTAMP)==null) {
			logger.error("timestamo for ident is not set, Line: "+line);
			return false;
		}		
		return true;
	}
	
	
	private boolean checkOtherFields(JsonObject a, JsonObject b) {
		return fieldsEqual(a, b, FIELD_TYPE) && fieldsEqual(a, b, FIELD_HOST);
	}
	
	
	private boolean fieldsEqual(JsonObject a, JsonObject b, String key) {
		String id=getId(a);
		if(!getStringVal(a, key).equals(getStringVal(b, key))){
			logger.warn("Field "+key+" not equal for id"+id);
			return false;
		}
		return true;
	}

	
	private void createDBTable() throws SQLException, ClassNotFoundException {

		String createTableQuery = "create table if not exists " + tableName
				+ "(id VARCHAR(20) not null, duration BIGINT, type VARCHAR (20), host VARCHAR (20), alert BOOLEAN)";
		
		executeSql(createTableQuery);

		if (deleteData) {
			String deleteTableQuery = "delete from " + tableName;
			executeSql(deleteTableQuery);	
		}
	}

	
	private void insertRow(JsonObject startEvent, JsonObject stopEvent, PreparedStatement insertStatement){
		
		long duration=Math.abs(getTimeStamp(stopEvent)-getTimeStamp(startEvent));
		boolean isAlert= duration>ALERT_TIME_MS;
		
		try{

			insertStatement.setString(1,getId(startEvent));
			insertStatement.setLong(2, duration);
			insertStatement.setString(3, getStringVal(startEvent, FIELD_TYPE));
			insertStatement.setString(4, getStringVal(startEvent, FIELD_HOST));
			insertStatement.setBoolean(5, isAlert);
		
			insertStatement.executeUpdate();
			
		} catch (Exception e) {
			logger.error(e);
		}
	}

	
	private void executeSql(String sqlQuery) throws SQLException, ClassNotFoundException {
		try(
		Statement statement = getConnection().createStatement();
		ResultSet rs = statement.executeQuery(sqlQuery)
		){}
	}
	
	
	private String getStringVal(JsonObject entry, String key) {
		JsonElement val=entry.get(key);
		if(val==null) {
			return "null";
		}
		return val.getAsString();
	}
	
	
	private void report() throws SQLException, ClassNotFoundException{
		String sql="select * from " + tableName;		
		try(
		Statement statement = getConnection().createStatement();
		ResultSet rs = statement.executeQuery(sql);
		){
			while(rs.next()) {
				System.out.println(rs.getString(1)+','+rs.getLong(2)+','+rs.getString(3)+','+rs.getString(4)+','+rs.getBoolean(5));
			}
		}
	}
	
	
	private Connection getConnection() throws SQLException, ClassNotFoundException {
		if (connection == null) {
			Class.forName("org.hsqldb.jdbcDriver");  //install driver
			connection = DriverManager.getConnection(protocol+dbName, "SA", "");
			createDBTable();
		}
		return connection;
	}
	
	
	private void close() throws SQLException{
		if (connection != null) {
			connection.close();
		}
	}
}
