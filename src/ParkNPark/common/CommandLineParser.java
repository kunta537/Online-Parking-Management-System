package ParkNPark.common;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.util.HashSet;
import java.util.Set;

/**
 * Parses a command line using GNU GetOpt
 */
public class CommandLineParser
{
	/** The reply size of the server messages in this test run in bytes */
	protected int replySize;
	
	/** The number of clients in this test run */
	protected int numClients;
	
	/** The number of servers in this test run */
	protected int numServers;
	
	/** The inter-request time in this test run in milliseconds */
	protected int interRequestTime;
	
	/** Whether or not fault injection is enabled */
	protected boolean faultInjection;
	
	/** When true, logs should be appended; when false, logs should be overwritten */
	protected boolean appendLogs;
	
	/** When true, never give up when trying to contact the server */
	protected boolean neverGiveUp;
	
	/** The minimum delay of the fault injection in milliseconds */
	protected int faultInjectionMinDelay = 0;
	
	/** The maximum delay of the fault injection in milliseconds */
	protected int faultInjectionMaxDelay = 30000;
	
	/** Our project root folder */
	protected String projectRoot;
	
	/** When not 0, how many invocations of getLotsMode should be performed */
	protected int getLotsModeCount;
	
	/** When true, the testing manager will only kill everything */
	protected boolean killOnly;
	
	/** The wait timeout to use during fault detections */
	protected int detectionTimeout = 5000;
	
	/** The wait timeout to use during fault recoveries */
	protected int recoveryTimeout = 5000;
	
	/** The JDBC URL */
	protected String jdbcURL = "jdbc:mysql://localhost/parknpark";

	/** The JDBC user name */
	private String jdbcUsername = "root";
	
	/** The JDBC password */
	private	String jdbcPassword = "root";
	
	private int serverPort;
	
	/** Enumeration of the accepted commands that the caller wants */
	public static enum AcceptedParameters { ORBInitialHost, ORBInitialPort, ORBServerPort,
		                                    ORBServerHost, replySize, numClients, numServers,
		                                    interRequestTime, faultInjection,
		                                    faultInjectionMinDelay, faultInjectionMaxDelay,
		                                    projectRoot, appendLogs, neverGiveUp, getLotsMode,
		                                    killOnly, recoveryTimeout, detectionTimeout,
		                                    jdbcURL, jdbcUsername, jdbcPassword };

	/**
	 * Parses the given command line for the given program name and
	 * returns true if the command line was successfully parsed or
	 * false if the program should exit
	 * @param name The name of the program
	 * @param args The arguments passed in from the command line
	 * @return True if the program can continue or false if the program
	 * should exit
	 * @param acceptedParameters Set of accepted parameters that the caller is interested in
	 */
	public boolean parseCommandLine(String name, String[] args, Set<AcceptedParameters> acceptedParameters)
	{
		// Generate our accepted list of command line options
		Set<LongOpt> optionSet = new HashSet<LongOpt>();
		for (AcceptedParameters parameter : acceptedParameters) {
			switch (parameter) {
			    case ORBInitialHost :
			    	optionSet.add(new LongOpt("ORBInitialHost", LongOpt.REQUIRED_ARGUMENT, null, 'w'));
			    	break;
			    case ORBInitialPort :
			    	optionSet.add(new LongOpt("ORBInitialPort", LongOpt.REQUIRED_ARGUMENT, null, 'x'));
			    	break;
			    case ORBServerPort:
			    	optionSet.add(new LongOpt("ORBServerPort", LongOpt.REQUIRED_ARGUMENT, null, 'y'));
			    	break;
			    case ORBServerHost:
			    	optionSet.add(new LongOpt("ORBServerHost", LongOpt.REQUIRED_ARGUMENT, null, 'z'));
			    	break;
			    case replySize:
			    	optionSet.add(new LongOpt("reply-size", LongOpt.REQUIRED_ARGUMENT, null, 'r'));
			    	break;
			    case numClients:
			    	optionSet.add(new LongOpt("num-clients", LongOpt.REQUIRED_ARGUMENT, null, 'n'));
			    	break;
			    case numServers:
			    	optionSet.add(new LongOpt("num-servers", LongOpt.REQUIRED_ARGUMENT, null, 's'));
			    	break;
			    case interRequestTime:
			    	optionSet.add(new LongOpt("interrequest-time", LongOpt.REQUIRED_ARGUMENT, null, 'i'));
			    	break;
			    case faultInjection:
			    	optionSet.add(new LongOpt("fault-injection", LongOpt.NO_ARGUMENT, null, 'f'));
			    	break;
			    case faultInjectionMinDelay:
			    	optionSet.add(new LongOpt("fault-injection-min-delay", LongOpt.REQUIRED_ARGUMENT, null, 'm'));
			    	break;
			    case faultInjectionMaxDelay:
			    	optionSet.add(new LongOpt("fault-injection-max-delay", LongOpt.REQUIRED_ARGUMENT, null, 'a'));
			    	break;
			    case projectRoot:
			    	optionSet.add(new LongOpt("project-root", LongOpt.REQUIRED_ARGUMENT, null, 'p'));
			    	break;
			    case appendLogs:
			    	optionSet.add(new LongOpt("append-logs", LongOpt.NO_ARGUMENT, null, 'l'));
			    	break;
			    case neverGiveUp:
			    	optionSet.add(new LongOpt("never-give-up", LongOpt.NO_ARGUMENT, null, 'v'));
			    	break;
			    case getLotsMode :
			    	optionSet.add(new LongOpt("get-lots-mode", LongOpt.REQUIRED_ARGUMENT, null, 'g'));
			    	break;
			    case killOnly :
			    	optionSet.add(new LongOpt("kill-only", LongOpt.NO_ARGUMENT, null, 'k'));
			    	break;
			    case detectionTimeout :
			    	optionSet.add(new LongOpt("detection-timeout", LongOpt.REQUIRED_ARGUMENT, null, 'q'));
			    	break;
			    case recoveryTimeout :
			    	optionSet.add(new LongOpt("recovery-timeout", LongOpt.REQUIRED_ARGUMENT, null, 'o'));
			    	break;
			    case jdbcURL :
			    	optionSet.add(new LongOpt("jdbc-url", LongOpt.REQUIRED_ARGUMENT, null, 'j'));
			    	break;
			    case jdbcUsername :
			    	optionSet.add(new LongOpt("jdbc-username", LongOpt.REQUIRED_ARGUMENT, null, 'd'));
			    	break;
			    case jdbcPassword :
			    	optionSet.add(new LongOpt("jdbc-password", LongOpt.REQUIRED_ARGUMENT, null, 'b'));
			    	break;
			}
		}
		
		// Always add help and test
    	optionSet.add(new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'));
    	optionSet.add(new LongOpt("test", LongOpt.NO_ARGUMENT, null, 't'));
		
		// Convert the accepted parameters into an array for GNU GetOpt
        LongOpt options[] = optionSet.toArray(new LongOpt[optionSet.size()]);
        
        // Have GNU GetOpt parse the command line parameters
        Getopt opt = new Getopt(name, args, "", options, true);
        int c;
        boolean testingOnly = false;
        while ((c = opt.getopt()) != -1) {
            switch (c)
            {
                case '?' :  // Warning already written out by GetOpt
                	System.out.println();
                case 'h' :
                {
                	// Display the help screen
                	help(name, acceptedParameters);
                	return false;
                }
                
                case 'r' :
                {
                	// Set the reply size for the test
                	String arg = opt.getOptarg();
                	try {
                	    replySize = Integer.parseInt(arg, 10);
                	    
                	    // Validate the reply size
                	    if (replySize != 0 && replySize != 256 && replySize != 512 &&
                	    	replySize != 1024) {
                	    	System.err.println("Reply size can only be 0 (original), 256, 512, or 1024");
                    		System.err.flush();
                        	System.out.println();
                        	help(name, acceptedParameters);
                	    	return false;
                	    }
                	} catch (NumberFormatException e) {
                		System.err.println("Parameter reply-size is not a number");
                		System.err.flush();
                    	System.out.println();
                    	help(name, acceptedParameters);
                		return false;
                	}
                	break;
                }
                
                case 'n' :
                {
                	// Set the number of clients
                	String arg = opt.getOptarg();
                	try {
                    	numClients = Integer.parseInt(arg, 10);
                    	
                    	// Validate the number of clients
                    	if (numClients != 1 && numClients != 4 && numClients != 7 &&
                    		numClients != 10) {
                    		System.err.println("Number of clients can only be 1, 4, 7, or 10");
                    		System.err.flush();
                        	System.out.println();
                        	help(name, acceptedParameters);
                    		return false;
                    	}
                	} catch (NumberFormatException e) {
                		System.err.println("Parameter num-clients is not a number");
                		System.err.flush();
                    	System.out.println();
                    	help(name, acceptedParameters);
                		return false;
                	}
                	break;
                }
            
                case 's' :
                {
                	// Set the number of servers
                	String arg = opt.getOptarg();
                	try {
                    	numServers = Integer.parseInt(arg, 10);
                    	
                    	// Validate the number of clients
                    	if (numServers <= 0) {
                    		System.err.println("Number of servers must be greater than 1");
                    		System.err.flush();
                        	System.out.println();
                        	help(name, acceptedParameters);
                    		return false;
                    	}
                	} catch (NumberFormatException e) {
                		System.err.println("Parameter num-servers is not a number");
                		System.err.flush();
                    	System.out.println();
                    	help(name, acceptedParameters);
                		return false;
                	}
                	break;
                }
            
                case 'i' :
                {
                	// Set the inter-request time
                	String arg = opt.getOptarg();
                	try {
                    	interRequestTime = Integer.parseInt(arg, 10);
                    	
                    	// Validate the inter-request time
                    	if (interRequestTime != 0 && interRequestTime != 20 &&
                    		interRequestTime != 40) {
                    		System.err.println("Inter-request time must be 0 (no delay), 20, or 40 milliseconds");
                    		System.err.flush();
                        	System.out.println();
                        	help(name, acceptedParameters);
                    		return false;
                    	}
                	} catch (NumberFormatException e) {
                		System.err.println("Parameter interrequest-time is not a number");
                		System.err.flush();
                    	System.out.println();
                    	help(name, acceptedParameters);
                		return false;
                	}
                	break;
                }
                
                case 't' :
                {
                	// The caller merely wanted to test whether or not the command
                	// line is valid or not. We will return success or failure before
                	// this method returns
                	System.err.println("testingOnly = true");
                	testingOnly = true;
                	break;
                }
                
                case 'p' :
                {
                	// Project root folder
                	projectRoot = opt.getOptarg();
                    break;
                }
                
                case 'k' :
                {
                	// Kill-only mode
                	killOnly = true;
                	break;
                }
                
                case 'f' :
                {
                	// Enable fault injection
                	faultInjection = true;
                    break;
                }
                
                case 'l' :
                {
                	// Append to log files instead of overwrite
                	appendLogs = true;
                	break;
                }
                
                case 'v' :
                {
                	// Never give up when trying to contact the server
                	neverGiveUp = true;
                	break;
                }
                
                case 'g' :
                {
                	// Get-lots mode
                	String arg = opt.getOptarg();
                	try {
                	    getLotsModeCount = Integer.parseInt(arg, 10);
                	} catch (NumberFormatException e) {
                		System.err.println("Parameter get-lots-mode is not a number");
                		System.err.flush();
                    	System.out.println();
                    	help(name, acceptedParameters);
                		return false;
                	}
                	break;
                }
                
                case 'o' :
                {
                	// Fault recovery timeout
                	String arg = opt.getOptarg();
                	try {
                	    recoveryTimeout = Integer.parseInt(arg, 10);
                	} catch (NumberFormatException e) {
                		System.err.println("Parameter recovery-timeout is not a number");
                		System.err.flush();
                    	System.out.println();
                    	help(name, acceptedParameters);
                		return false;
                	}
                	break;
                }
                
                case 'q' :
                {
                	// Fault detection timeout
                	String arg = opt.getOptarg();
                	try {
                	    detectionTimeout = Integer.parseInt(arg, 10);
                	} catch (NumberFormatException e) {
                		System.err.println("Parameter detection-timeout is not a number");
                		System.err.flush();
                    	System.out.println();
                    	help(name, acceptedParameters);
                		return false;
                	}
                	break;
                }
                
                case 'a' :
                {
                    // Maximum delay for fault injection
                	String arg = opt.getOptarg();
                	try {
                	    faultInjectionMaxDelay = Integer.parseInt(arg, 10);
                	} catch (NumberFormatException e) {
                		System.err.println("Parameter fault-injection-max-delay is not a number");
                		System.err.flush();
                    	System.out.println();
                    	help(name, acceptedParameters);
                		return false;
                	}
                	break;
                }

                case 'm' :
                {
                    // Minimum delay for fault injection
                	String arg = opt.getOptarg();
                	try {
                	    faultInjectionMinDelay = Integer.parseInt(arg, 10);
                	} catch (NumberFormatException e) {
                		System.err.println("Parameter fault-injection-min-delay is not a number");
                		System.err.flush();
                    	System.out.println();
                    	help(name, acceptedParameters);
                		return false;
                	}
                	break;
                }
                
                case 'j' :
                {
                	// JDBC URL
                	jdbcURL = opt.getOptarg();
                	break;
                }
                
                case 'd' :
                {
                	// JDBC username
                	jdbcUsername = opt.getOptarg();
                	break;
                }
                
                case 'b' :
                {
                	// JDBC password
                	jdbcPassword = opt.getOptarg();
                	break;
                }
                
                case 'y' :
                {
                	// ORBServerPort
                	System.out.println("ORBServerPort: " + opt.getOptarg());
                	serverPort = Integer.parseInt(opt.getOptarg());
                	break;
                }
                
            }
        }
        // If kill-only is set, then don't validate the required parameters
        if (!killOnly)
        {
            // Ensure that numClients is set
            if (acceptedParameters.contains(AcceptedParameters.numClients) && numClients == 0) {
            	System.err.println("--num-clients is a required parameter");
        		System.err.flush();
            	System.out.println();
            	help(name, acceptedParameters);
            	return false;
            }
            
            // Ensure that numServers is set
            if (acceptedParameters.contains(AcceptedParameters.numServers) && numServers == 0) {
            	System.err.println("--num-servers is a required parameter");
        		System.err.flush();
            	System.out.println();
            	help(name, acceptedParameters);
            	return false;
            }
             
        }
   
        // If the caller was only testing us, then exit successfully now
        if (testingOnly) {
        	System.exit(0);
        	return false;
        }
        
        // If fault injection is enabled, ensure that the maximum delay is
        // greater than the minimum delay
        if (faultInjection && faultInjectionMinDelay > faultInjectionMaxDelay) {
        	System.err.println("--fault-injection-max-delay must be greater than or equal to --fault-injection-min-delay");
        	System.err.flush();
        	System.out.println();
        	help(name, acceptedParameters);
        	return false;
        }
        // We can continue
		return true;
	}
	
	/**
	 * Writes out our command line help text to stdout using the given program name
	 * @param name The program name to use when displaying the command line help
	 * @param acceptedParameters Set of accepted parameters that the caller is interested in
	 */
	protected void help(String name, Set<AcceptedParameters> acceptedParameters) {
    	System.out.println(name + " command line parameters:");
    	System.out.println();
    	if (acceptedParameters.contains(AcceptedParameters.ORBInitialHost))
    	    System.out.println("-ORBInitialHost      Host name or IP address of the name service ");
    	if (acceptedParameters.contains(AcceptedParameters.ORBInitialPort))
    	    System.out.println("-ORBInitialPort      Port of the name service");
    	if (acceptedParameters.contains(AcceptedParameters.ORBServerHost))
    	    System.out.println("-ORBServerHost       Host name or IP address to serve on");
    	if (acceptedParameters.contains(AcceptedParameters.ORBServerPort))
    	    System.out.println("-ORBServerPort       Port to serve on");
    	if (acceptedParameters.contains(AcceptedParameters.replySize)) {
    	    System.out.println("--reply-size         Size of the reply messages: 0 (original), 256, 512, or");
	        System.out.println("                     1024 bytes");
    	}
    	if (acceptedParameters.contains(AcceptedParameters.numClients))
    	    System.out.println("--num-clients        Number of clients running in this test: 1, 4, 7, or 10");
    	if (acceptedParameters.contains(AcceptedParameters.numServers))
    	    System.out.println("--num-servers        Number of servers running in this test; must be >= 1");
    	if (acceptedParameters.contains(AcceptedParameters.interRequestTime)) {
    	    System.out.println("--interrequest-time  Time to delay in milliseconds between requests:");
    	    System.out.println("                     0 (no delay), 20, or 40");
    	}
    	if (acceptedParameters.contains(AcceptedParameters.faultInjection))
    	    System.out.println("--fault-injection    Enable randomized fault injection");
    	if (acceptedParameters.contains(AcceptedParameters.faultInjectionMinDelay)) {
    	    System.out.println("--fault-injection-min-delay  Minimum delay of the fault injector between");
    	    System.out.println("                     fault injections in milliseconds. Default is 0");
    	}
    	if (acceptedParameters.contains(AcceptedParameters.faultInjectionMaxDelay)) {
    	    System.out.println("--fault-injection-max-delay  Maximum delay of the fault injector between");
    	    System.out.println("                     fault injections in milliseconds. Default is 30000.");
    	    System.out.println("                     If equal to minimum, fault injection time is periodic");
    	    System.out.println("                     and not random");
    	}
    	if (acceptedParameters.contains(AcceptedParameters.neverGiveUp)) {
    		System.out.println("--never-give-up      Never give up connection attempts to the server, even");
    		System.out.println("                     if no primary servers are registered");
    	}
    	if (acceptedParameters.contains(AcceptedParameters.appendLogs))
    	    System.out.println("--append-logs        Append to log files instead of overwriting them");
    	if (acceptedParameters.contains(AcceptedParameters.projectRoot))
    	    System.out.println("--project-root       Project root folder to start servers in");
    	if (acceptedParameters.contains(AcceptedParameters.getLotsMode)) {
    		System.out.println("--get-lots-mode      The number of invocations of getLots() to invoke or,");
    	    System.out.println("                     when 0 or not specified, run the client normally");
	    }
    	if (acceptedParameters.contains(AcceptedParameters.killOnly)) {
    		System.out.println("--kill-only          When set, the existing clients and servers will be");
    		System.out.println("                     killed and the testing manager will then exit");
    	}
    	if (acceptedParameters.contains(AcceptedParameters.detectionTimeout)) {
    		System.out.println("--detection-timeout  The number of milliseconds to wait between fault");
    		System.out.println("                     detection cycles");
    		System.out.println("                     (default is " + detectionTimeout + "ms)");
    	}
    	if (acceptedParameters.contains(AcceptedParameters.recoveryTimeout)) {
    		System.out.println("--recovery-timeout   The number of milliseconds to wait during fault");
    		System.out.println("                     recovery when a server cannot be obtained");
    		System.out.println("                     (default is " + recoveryTimeout + "ms)");
    	}
    	if (acceptedParameters.contains(AcceptedParameters.jdbcURL)) {
    		System.out.println("--jdbc-url           The JDBC URL to use for the database connection");
    		System.out.println("                     (default is " + jdbcURL + ")");
    	}
    	if (acceptedParameters.contains(AcceptedParameters.jdbcUsername)) {
    		System.out.println("--jdbc-username      The username to use in the database connection");
    		System.out.println("                     (default is " + jdbcUsername + ")");
    	}
    	if (acceptedParameters.contains(AcceptedParameters.jdbcPassword)) {
    		System.out.println("--jdbc-password      The password to use in the database connection");
    		System.out.println("                     (default is " + jdbcPassword + ")");
    	}
	    System.out.println("--test               Tests the command line parameters for correctness and");
	    System.out.println("                     exits with 0 if successful or 1 if unsuccessful");
    	System.out.println("--help               Display this help page");
    	if (acceptedParameters.contains(AcceptedParameters.ORBServerHost)) {
    	    System.out.println();
    	    System.out.println("Note that the -ORBServerHost parameter is only necessary if this system's host");
    	    System.out.println("name maps to a localhost address such as 127.0.0.1.");
    	}
	}

	/**
	 * Returns the inter-request time in this test run
	 * @return The inter-request time in this test run
	 */
	public int getInterRequestTime() {
		return interRequestTime;
	}

	/**
	 * Returns the number of clients in this test run
	 * @return The number of clients in this test run
	 */
	public int getNumClients() {
		return numClients;
	}

	/**
	 * Returns the number of servers in this test run
	 * @return The number of servers in this test run
	 */
	public int getNumServers() {
		return numServers;
	}

	/**
	 * Returns the reply size of the server messages in this test run
	 * @return The reply size of the server messages in this test run
	 */
	public int getReplySize() {
		return replySize;
	}
	
	/**
	 * Returns true when fault injection is enabled or false when it is disabled
	 * @return True when fault injection is enabled or false when it is disabled
	 */
	public boolean isFaultInjectionEnabled() {
		return faultInjection;
	}
	
	/**
	 * Returns the minimum delay of the fault injector in milliseconds
	 * @return The minimum delay of the fault injector in milliseconds
	 */
	public int getFaultInjectionMinDelay() {
		return faultInjectionMinDelay;
	}
	
	/**
	 * Returns the maximum delay of the fault injector in milliseconds
	 * @return The maximum delay of the fault injector in milliseconds
	 */
	public int getFaultInjectionMaxDelay() {
		return faultInjectionMaxDelay;
	}
	
	/**
	 * Returns true when log files should be appended or false when logs
	 * files should be overwritten
	 * @return True when log files should be appended or false when logs
	 * files should be overwritten
	 */
	public boolean shouldAppendLogs() {
		return appendLogs;
	}
	
	/**
	 * Returns true when the program should never give up when trying
	 * to reach a server. Otherwise, a client can give up if no primary
	 * server is registered, for example
	 * @return True when the program should never give up when trying
	 * to reach a server
	 */
	public boolean shouldNeverGiveUp() {
		return neverGiveUp;
	}
	
	/**
	 * Returns whether or not the testing manager should only kill
	 * everything and exit
	 * @return Whether or not the testing manager should only kill
	 * everything and exit
	 */
	public boolean shouldKillOnly() {
		return killOnly;
	}
	
	/**
	 * Returns the project root folder or null if none was given in the command
	 * line arguments
	 * @return The project root folder or null if none was given in the command
	 * line arguments
	 */
	public String getProjectRoot() {
		return projectRoot;
	}
	
	/**
	 * When not 0, how many invocations of getLotsMode should be performed
	 * @return When not 0, how many invocations of getLotsMode should be performed
	 */
	public int getGetLotsModeCount() {
		return getLotsModeCount;
	}
	
	/**
	 * Returns the fault recovery timeout
	 * @return The fault recovery timeout
	 */
	public int getFaultRecoveryTimeout() {
		return recoveryTimeout;
	}
	
	/**
	 * Returns the fault detection timeout
	 * @return The fault detection timeout
	 */
	public int getFaultDetectionTimeout() {
		return detectionTimeout;
	}
	
	/**
	 * Returns the JDBC URL
	 * @return The JDBC URL
	 */
	public String getJDBCURL() {
		return jdbcURL;
	}
	
	/**
	 * Returns the JDBC username
	 * @return The JDBC username
	 */
	public String getJDBCUsername() {
		return jdbcUsername;
	}
	
	/**
	 * Returns the JDBC password
	 * @return The JDBC password
	 */
	public String getJDBCPassword() {
		return jdbcPassword;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
}
