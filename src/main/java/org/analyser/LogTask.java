package org.analyser;

public class LogTask {

	private static final String PROGRAM_NAME="LogTask";
	private static final String USAGE = 
			"usage: "+PROGRAM_NAME+" file [-d] [-r]\r\n"+
			"  file - log file name\r\n"+
			"  -r - report database content\r\n"+
			"  -d - delate data from database\r\n"+
			"creates database Analysis in working directory and rewrites data from log file to Event table";

	private static boolean showResult=false;
	private static boolean deleteData=false;	
	private static String fileName=null;
	
	public static void main(String[] args) {
		if(!checkParams(args)) {
			System.out.println(USAGE);
			return;
		}
		LogAnalyser logAnalyser=new LogAnalyser(fileName, deleteData, showResult);
		logAnalyser.run();
	}
	
	private static boolean checkParams(String[] args) {
		int argsNo=args!=null ? args.length : 0;
		
		if(argsNo<1 || argsNo >3) {
			return false;
		}
		
		for (String arg : args) {
			if(arg==null) {
				return false;
			}
			if (arg.equals("-r")) {
				showResult = true;
			} else if (arg.equals("-d")) {
				deleteData = true;
			} else {
				if(fileName==null) {
					fileName = arg;
				}
				else {
					return false;
				}
			}
		}
		
		return true;
	}
}
