package ParkNPark.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ParkNPark.common.CommandLineParser;

/**
 * Runs all 48 test configurations
 */
public class RunAll implements Runnable
{
	/** The project root folder */
	protected String projectRoot;
	
	/** Whether or not fault injection is enabled */
	protected boolean faultInjection;

	/** The minimum delay of the fault injection in milliseconds */
	protected int faultInjectionMinDelay;
	
	/** The maximum delay of the fault injection in milliseconds */
	protected int faultInjectionMaxDelay;
	
	/** The number of servers in this test run */
	protected int numServers;
	
	/** When not 0, how many invocations of getLotsMode should be performed */
	protected int getLotsModeCount;
	
	/** The wait timeout to use during fault recoveries */
	protected int recoveryTimeout;
	
	/** The wait timeout to use during fault detections */
	protected int detectionTimeout;
		
	/** The JDBC URL */
	private String jdbcURL;
	
	/** The JDBC user name */
	private String jdbcUsername;
	
	/** The JDBC password */
	private	String jdbcPassword;
	
	/**
	 * Runs the entire test suite
	 * @param args Command-line arguments
	 */
	public static void main(String[] args)
	{
		// Get our project root from the current working folder
		File projectRoot = new File(System.getProperty("user.dir", ""));
		if (!projectRoot.getName().equals("tests")) {
			System.err.println("Current working folder is not \"tests\"; cannot guarantee directory structure; exiting");
			System.exit(1);
			return;
		}

		// Parse the command line arguments
		CommandLineParser clp = new CommandLineParser();
		Set<CommandLineParser.AcceptedParameters> acceptedParameters = new HashSet<CommandLineParser.AcceptedParameters>();
		acceptedParameters.add(CommandLineParser.AcceptedParameters.faultInjection);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.faultInjectionMinDelay);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.faultInjectionMaxDelay);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.numServers);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.detectionTimeout);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.recoveryTimeout);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.getLotsMode);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.jdbcURL);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.jdbcUsername);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.jdbcPassword);
		if (!clp.parseCommandLine(RunAll.class.getName(), args, acceptedParameters)) {
			System.exit(1);
			return;
		}

        // Create our new test and run it
		(new RunAll(projectRoot.getParentFile().getAbsolutePath(), clp.getNumServers(),
				    clp.isFaultInjectionEnabled(), clp.getFaultInjectionMinDelay(),
				    clp.getFaultInjectionMaxDelay(), clp.getGetLotsModeCount(),
				    clp.getFaultDetectionTimeout(), clp.getFaultRecoveryTimeout(),
				    clp.getJDBCURL(), clp.getJDBCUsername(), clp.getJDBCPassword())).run();
	}
	
	/**
	 * Creates a new test suite runner for the given project root path
	 * @param projectRoot The root of the project to use in this test
	 * suite run
	 * @param numServers The number of servers to run in this test suite instance
	 * @param faultInjection True if fault injection is enabled or
	 * false if it is disabled
	 * @param faultInjectionMinDelay The minimum delay of the fault injector in milliseconds
	 * @param faultInjectionMaxDelay The maximum delay of the fault injector in milliseconds
	 * @param getLotsMode When not zero, this is the number of iterations to run in getLotsMode
	 * on the client. When zero or less, the client is run with the predefined test suite
	 * @param detectionTimeout The fault detection timeout value to pass to the replication
	 * manager
	 * @param recoveryTimeout The fault recovery timeout value to pass to clients
	 * @param jdbcURL The JDBC URL to use in the database connection
	 * @param jdbcUsername The username to use in the database connection
	 * @param jdbcPassword The password to use in the database connection
	 */
	public RunAll(String projectRoot, int numServers, boolean faultInjection,
			      int faultInjectionMinDelay, int faultInjectionMaxDelay,
			      int getLotsMode, int detectionTimeout, int recoveryTimeout,
			      String jdbcURL, String jdbcUsername, String jdbcPassword) {
		this.projectRoot = projectRoot;
		this.numServers = numServers;
		this.faultInjection = faultInjection;
		this.faultInjectionMinDelay = faultInjectionMinDelay;
		this.faultInjectionMaxDelay = faultInjectionMaxDelay;
		this.getLotsModeCount = getLotsMode;
		this.detectionTimeout = detectionTimeout;
		this.recoveryTimeout = recoveryTimeout;
		this.jdbcURL = jdbcURL;
		this.jdbcUsername = jdbcUsername;
		this.jdbcPassword = jdbcPassword;
	}
	
	/**
	 * Runs the entire test suite
	 */
	public void run() {
		List<Integer> numClientsList = new ArrayList<Integer>(4);
		List<Integer> interRequestTimeList = new ArrayList<Integer>(3);
		List<Integer> replySizeList = new ArrayList<Integer>(4);
		
		// Build the number of clients list
		numClientsList.add(new Integer(1));
		numClientsList.add(new Integer(4));
		numClientsList.add(new Integer(7));
		numClientsList.add(new Integer(10));

		// Build the inter-request time list
		interRequestTimeList.add(new Integer(0));
		interRequestTimeList.add(new Integer(20));
		interRequestTimeList.add(new Integer(40));
		
		// Build the reply size list
		replySizeList.add(new Integer(0));
		replySizeList.add(new Integer(256));
		replySizeList.add(new Integer(512));
		replySizeList.add(new Integer(1024));
		
		// Run our test suite
		try {
			for (Integer numClients : numClientsList) {
				for (Integer interRequestTime : interRequestTimeList) {
					for (Integer replySize : replySizeList) {
						(new RunOne(projectRoot, numClients.intValue(), numServers, interRequestTime.intValue(),
								    replySize.intValue(), faultInjection, faultInjectionMinDelay,
								    faultInjectionMaxDelay, false, getLotsModeCount,
								    detectionTimeout, recoveryTimeout, jdbcURL,
								    jdbcUsername, jdbcPassword)).run();
						System.out.println();
					}
				}
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
			return;
		}
		
		// We are finished :-)!
		System.out.println("Test suite complete");
    }
}
