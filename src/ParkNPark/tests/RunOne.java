package ParkNPark.tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.omg.CORBA.ORB;

import ParkNPark.common.CommandLineParser;
import ParkNPark.common.InputEater;
import ParkNPark.common.NameService;
import ParkNPark.common.Server;

/**
 * Class that runs one test configuration
 */
public class RunOne implements Runnable
{
	/**
	 * Variables for real-time analysis
	 */
	
	/** Time between requests */
	protected int interRequestTime;
	
	/** Size of reply */
	protected int replySize;

	/** Singleton instance of the ProcessInfoComparator */
	protected static final ProcessInfoComparator<ProcessInfo> processInfoComparator = new ProcessInfoComparator<ProcessInfo>(); 

	/** Our servers */
	protected Set<ProcessInfo> servers = new TreeSet<ProcessInfo>(processInfoComparator);
	
	/** All servers that are defined, because we need to kill them all on startup */
	protected Set<String> allServers = new TreeSet<String>();
	
	/** Our clients */
	protected Set<ProcessInfo> clients = new TreeSet<ProcessInfo>(processInfoComparator);
	
	/** All clients that are defined, because we need to kill them all on startup */
	protected Set<String> allClients = new TreeSet<String>();
	
	/** Our name and replication server */
	protected ProcessInfo sacredServer = new ProcessInfo();
	
	/** The project root folder */
	protected String projectRoot;
	
	/** Whether or not fault injection is enabled */
	protected boolean faultInjection;
	
	/** The minimum delay of the fault injection in milliseconds */
	protected int faultInjectionMinDelay;
	
	/** The maximum delay of the fault injection in milliseconds */
	protected int faultInjectionMaxDelay;

	/** When true, the testing manager will only kill everything */
	protected boolean killOnly;
	
	/** When not 0, how many invocations of getLotsMode should be performed */
	protected int getLotsModeCount;
	
	/** The wait timeout to use during fault recoveries */
	protected int recoveryTimeout;
	
	/** The wait timeout to use during fault detections */
	protected int detectionTimeout;
	
	/** Object request broker */
	protected ORB orb;
	
	/** The JDBC URL */
	private String jdbcURL;
	
	/** The JDBC user name */
	private String jdbcUsername;
	
	/** The JDBC password */
	private	String jdbcPassword;
	
	/** A writer for stderr */
	protected BufferedWriter stdErrWriter = new BufferedWriter(new OutputStreamWriter(System.err));
	
	/**
	 * Run the tests as specified on the command line
	 * @param args The command line arguments
	 */
	public static void main(String[] args)
	{
		// Parse the command line
		CommandLineParser clp = new CommandLineParser();
		Set<CommandLineParser.AcceptedParameters> acceptedParameters = new HashSet<CommandLineParser.AcceptedParameters>();
		acceptedParameters.add(CommandLineParser.AcceptedParameters.faultInjection);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.faultInjectionMinDelay);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.faultInjectionMaxDelay);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.numServers);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.numClients);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.interRequestTime);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.projectRoot);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.replySize);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.killOnly);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.getLotsMode);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.detectionTimeout);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.recoveryTimeout);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.jdbcURL);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.jdbcUsername);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.jdbcPassword);
		if (!clp.parseCommandLine(RunOne.class.getName(), args, acceptedParameters)) {
			System.exit(1);
			return;
		}
		
		// If the user provided a project root, then use that. Otherwise, get our
		// project root from the current working folder's parent folder
		String projectRootName = clp.getProjectRoot();
		if (projectRootName == null) {
			File projectRoot = new File(System.getProperty("user.dir", ""));
			if (!projectRoot.getName().equals("tests")) {
				System.err.println("Current working folder is not \"tests\"; cannot guarantee directory structure; exiting");
				System.exit(1);
				return;
			}
			projectRootName = projectRoot.getParentFile().getAbsolutePath();
		}

        // Create our new test and run it
		RunOne test;
		try {
		    test = new RunOne(projectRootName, clp.getNumClients(), clp.getNumServers(),
		    		          clp.getInterRequestTime(), clp.getReplySize(),
		    		          clp.isFaultInjectionEnabled(), clp.getFaultInjectionMinDelay(),
		    		          clp.getFaultInjectionMaxDelay(), clp.shouldKillOnly(),
		    		          clp.getGetLotsModeCount(), clp.getFaultDetectionTimeout(),
		    		          clp.getFaultRecoveryTimeout(), clp.getJDBCURL(),
		    		          clp.getJDBCUsername(), clp.getJDBCPassword());
		} catch (IOException e) {
			System.out.println(e.getMessage());
			return;
		}
		test.run();
	}
	
	/**
	 * Creates a new test case runner
	 * @param projectRoot The project's root folder to use for this test case run
	 * @param numClients The number of clients to use in this test case run
	 * @param numServers The number of servers to use in this test case run
	 * @param interRequestTime The amount of time in milliseconds to wait between client invocations
	 * @param replySize The size of the replies from the server in bytes
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
	 * @throws IOException Thrown if an IOException is thrown during the processing
	 */
	public RunOne(String projectRoot, int numClients, int numServers, int interRequestTime, int replySize,
			      boolean faultInjection, int faultInjectionMinDelay,
			      int faultInjectionMaxDelay, boolean killOnly,
			      int getLotsMode, int detectionTimeout, int recoveryTimeout,
			      String jdbcURL, String jdbcUsername, String jdbcPassword) throws IOException {
		this.projectRoot = projectRoot;
		this.interRequestTime = interRequestTime;
		this.replySize = replySize;
		this.faultInjection = faultInjection;
		this.faultInjectionMinDelay = faultInjectionMinDelay;
		this.faultInjectionMaxDelay = faultInjectionMaxDelay;
		this.killOnly = killOnly;
		this.getLotsModeCount = getLotsMode;
		this.detectionTimeout = detectionTimeout;
		this.recoveryTimeout = recoveryTimeout;
		this.jdbcURL = jdbcURL;
		this.jdbcUsername = jdbcUsername;
		this.jdbcPassword = jdbcPassword;
		
		// Read our server list, being careful to not exceed the number specified in the command line
	    Set<String> names = new TreeSet<String>();
	    populateSet(names, new FileReader("servers"));
	    Iterator<String> nameIterator = names.iterator();
	    int i = 0;
	    while (i < numServers && nameIterator.hasNext()) {
	    	ProcessInfo info = new ProcessInfo();
	    	info.name = nameIterator.next();
	    	servers.add(info);
	    	i++;
	    }
	    for (String client: names)
	    	allServers.add(client);

		// Read our client list, being careful to not exceed the number specified in the command line
	    names.clear();
	    populateSet(names, new FileReader("clients"));
	    nameIterator = names.iterator();
	    i = 0;
	    while (i < numClients && nameIterator.hasNext()) {
	    	ProcessInfo info = new ProcessInfo();
	    	info.name = nameIterator.next();
	    	clients.add(info);
	    	i++;
	    }
	    for (String client: names)
	    	allClients.add(client);

	    // Get the name and port of our sacred server
	    names.clear();
	    populateSet(names, new FileReader("sacredserver"));
	    sacredServer.name = names.iterator().next();
	    names.clear();
	    
	    // Split the sacred server's name into name and port. If it's
	    // only the name, then fall back to the default port of 7777
	    int delimiter;
	    String port;
	    if ((delimiter = sacredServer.name.indexOf(':')) > 0 && delimiter < sacredServer.name.length() - 1) {
	    	port = sacredServer.name.substring(delimiter + 1);
	    	sacredServer.name = sacredServer.name.substring(0, delimiter);
	    } else
	    	port = "7777";
	    
	    // Initialize our ORB using the sacred server's name and port
        System.out.println("Starting the CORBA ORB, pointing the name service to " + sacredServer.name + ":" + port);
        try {
        	String args[] = new String[4];
        	args[0] = "-ORBInitialHost";
        	args[1] = sacredServer.name;
        	args[2] = "-ORBInitialPort";
        	args[3] = port;
        	orb = ORB.init(args, null);
        } 
        catch (Exception e) {
        	System.out.println("Exception while creating ORB; exiting");
        	System.exit(1);
        	return;
        }
    }
	
	/**
	 * Populates a Set from individual lines of a reader
	 * @param set The Set to populate
	 * @param input The Reader to read lines from
	 * @throws IOException Thrown if an IOException arises during processing
	 */
	protected static void populateSet(Set<String> set, Reader input) throws IOException
	{
		// Populate the set from the input, one line at a time
		BufferedReader reader = new BufferedReader(input);
		String line;
		while ((line = reader.readLine()) != null) {
			line.trim();
			if (line.length() > 0)
				set.add(line);
		}
	}
	
	/**
	 * Ensures that the output folders exist, which the programs
	 * write stdout or stderr to, depending on the program
	 */
	protected void ensureOutputFoldersExist()
	{
		// Ensure that our output folders exist
		File sacredServerOutput = new File("output/sacredServer");
		File clientsOutput = new File("output/clients");
		File serversOutput = new File("output/servers");
		if (!sacredServerOutput.exists())
			sacredServerOutput.mkdirs();
		if (!clientsOutput.exists())
			clientsOutput.mkdirs();
		if (!serversOutput.exists())
			serversOutput.mkdirs();
	}

	/**
	 * Runs this test's configuration by cleaning out any old
	 * server instances, starting the name service from scratch,
	 * starting the replication manager, starting the servers,
	 * starting the clients, waiting for the clients to exit,
	 * telling the servers to exit, and waiting for the servers
	 * to exit
	 */
	public void run()
	{
		// Redirect all stderr to the InputEater (we have multiple threads
		// that can call CORBA methods that may barf to stderr)
		PrintStream err = System.err;
		System.setErr(new PrintStream(new InputEater()));
		try
		{
			// Ensure that our output folders exist
			ensureOutputFoldersExist();
			
			// Describe this test
			if (!killOnly) {
				System.out.println("Running test scenario with " + clients.size() + " clients, " + servers.size() + " servers, an inter-request time of " + interRequestTime + " milliseconds, and a reply size of " + replySize + " bytes");
				if (faultInjection) {
					if (faultInjectionMinDelay == faultInjectionMaxDelay)
						System.out.println("Fault injection enabled with a fixed delay of " + faultInjectionMinDelay + " milliseconds");
					else
						System.out.println("Fault injection enabled with a random delay between " + faultInjectionMinDelay + " and " + faultInjectionMaxDelay + " milliseconds");
				} else
					System.out.println("Fault injection disabled");
				System.out.println("Replication manager fault detection timeout set to " + detectionTimeout + "ms");
				System.out.println("Client fault recovery timeout set to " + recoveryTimeout + "ms");
				if (getLotsModeCount > 0)
					System.out.println("Running client in getLots()-only mode with " + getLotsModeCount + " invocations");
				else
					System.out.println("Running client with test suite specified in client-test");
				System.out.println("JDBC URL: " + jdbcURL);
				System.out.println("JDBC Username: " + jdbcUsername);
			}
			
			// Shut down the replication manager
			System.out.println("Stopping the existing replication manager");
			sacredServer.process = Runtime.getRuntime().exec("ssh -x boggle \"" + projectRoot + "/sacredServer/killreplicationmanager\"");
			sacredServer.stdout = new InputCopier(new InputStreamReader(sacredServer.process.getInputStream()),
			                                      new FileWriter("output/sacredServer/" + sacredServer.name));
			sacredServer.stdout.start();
			if (sacredServer.process.waitFor() > 0)
				System.out.println("Could not kill the replication manager (possibly already dead)");
			
			// Shut down the name service
			System.out.println("Stopping the name service and resetting its database");
			sacredServer.process = Runtime.getRuntime().exec("ssh -x boggle killall -9 orbd; rm -rf \"" + projectRoot + "/sacredServer/orb.db\"");
			sacredServer.stdout = new InputCopier(new InputStreamReader(sacredServer.process.getInputStream()),
                                                  new FileWriter("output/sacredServer/" + sacredServer.name));
			sacredServer.stdout.start();
			if (sacredServer.process.waitFor() > 0)
				System.out.println("Could not kill the name service (possily already dead)");
			
			// Start the name service and the replication manager
			if (!killOnly) {
				System.out.println("Starting the name service and replication manager");
				sacredServer.process = Runtime.getRuntime().exec("ssh -x boggle cd \"" + projectRoot + "/sacredServer\"; ./nameservice; sleep 2; ./replicationmanager" +
						 " --num-clients " + clients.size() + " --detection-timeout " + detectionTimeout +
						 " --interrequest-time " + interRequestTime +
						 " --reply-size " + replySize + " --num-servers " + servers.size() +
						 " --jdbc-url \"" + jdbcURL.replace("\"", "\\\"") +
						 "\" --jdbc-username \"" + jdbcUsername.replace("\"", "\\\"") +
						 "\" --jdbc-password \"" + jdbcPassword.replace("\"", "\\\"") + "\"");
				sacredServer.stdout = new ServerInputCopier(sacredServer, new InputStreamReader(sacredServer.process.getInputStream()),
	                                                        new FileWriter("output/sacredServer/" + sacredServer.name));
				sacredServer.stdout.start();
				sacredServer.stderr = new ErrorCopier(sacredServer, new InputStreamReader(sacredServer.process.getErrorStream()),
	                                                  stdErrWriter);
				sacredServer.stderr.start();
				
				// Wait for the replication manager to finish starting
				System.out.println("Waiting for the replication manager to finish starting");
				synchronized(sacredServer.interruptThread) {
					while (!sacredServer.ready && !sacredServer.abnormalExit) {
						try {
						    sacredServer.interruptThread.wait();
						} catch (InterruptedException e) { }
					}

					// If the replication manager is not running, then exit now
					if (sacredServer.abnormalExit) {
						System.out.println("Replication manager on " + sacredServer.name + " did not start up successfully; exiting");
						synchronized(sacredServer.stdout) {
							if (sacredServer.stdout.isAlive())
								sacredServer.stdout.join();
						}
						synchronized(sacredServer.stderr) {
							if (sacredServer.stderr.isAlive())
								sacredServer.stderr.join();
						}
						System.exit(1);
						return;
					}
				}
			}
			
			// Shut down the existing servers
			System.out.println("Shutting down any existing servers");
			for (String server : allServers)
			{
				// Shut down this server
				ProcessInfo process = new ProcessInfo();
				process.name = server;
				System.out.println("  " + server);
				process.process = Runtime.getRuntime().exec("ssh -x " + server + " \"" + projectRoot + "/server/killserver\"");
				process.stdout = new ErrorCopier(process, new InputStreamReader(process.process.getInputStream()),
	                                            stdErrWriter);
				process.stdout.start();
				synchronized(process.stdout) {
					if (process.stdout.isAlive())
						process.stdout.join();
				}
			}
			
			// Shut down the existing clients
			System.out.println("Shutting down any existing clients");
			for (String client : allClients)
			{
				// Shut down this client
				ProcessInfo process = new ProcessInfo();
				process.name = client;
				System.out.println("  " + client);
				process.process = Runtime.getRuntime().exec("ssh -x " + client + " \"" + projectRoot + "/client/killclient\"");
				process.stdout = new ErrorCopier(process, new InputStreamReader(process.process.getInputStream()),
	                                             stdErrWriter);
				process.stdout.start();
				synchronized(process.stdout) {
					if (process.stdout.isAlive())
						process.stdout.join();
				}
			}
			
			// If we were only killing, then return now
			if (killOnly){
				System.out.println("Killing complete; test is no longer running");
				return;
			}

			// Start each server
			System.out.println("Starting " + servers.size() + " servers");
			for (ProcessInfo server : servers)
			{
				// Start this server instance and remember it
				System.out.println("  " + server.name);
				Process newServer = Runtime.getRuntime().exec("ssh -x " + server.name +
						" cd \"" + projectRoot + "/server\"; ./server --num-clients " + clients.size() +
						" --interrequest-time " + interRequestTime +
						" --reply-size " + replySize + " --num-servers " + servers.size() +
						" --jdbc-url \"" + jdbcURL.replace("\"", "\\\"") +
						"\" --jdbc-username \"" + jdbcUsername.replace("\"", "\\\"") +
						"\" --jdbc-password \"" + jdbcPassword.replace("\"", "\\\"") + "\"");
				server.process = newServer;
				
				// Start the output copying
				server.stdout = new ServerInputCopier(server, new InputStreamReader(newServer.getInputStream()),
						                              new FileWriter("output/servers/" + server.name));
				server.stdout.start();
				server.stderr = new ErrorCopier(server, new InputStreamReader(newServer.getErrorStream()),
						                        stdErrWriter);
                server.stderr.start();
			}
			
			// Wait for the servers to finish starting
			System.out.println("Waiting for the servers to finish starting");
			for (ProcessInfo server : servers) {
				System.out.println("  " + server.name);
				
				// Wait for this server to finish starting
				synchronized(server.interruptThread) {
					while (!server.ready && !server.abnormalExit) {
						try {
							server.interruptThread.wait();
						} catch (InterruptedException e) { }
					}

					// If the server is not running, then exit now
					if (server.abnormalExit) {
						System.out.println("Server " + server.name + " did not start up successfully; exiting");
						synchronized(server.stdout) {
							if (server.stdout.isAlive())
								server.stdout.join();
						}
						synchronized(server.stderr) {
							if (server.stderr.isAlive())
								server.stderr.join();
						}
						System.exit(1);
						return;
					}
				}
			}
			
			// Create the name service helper object instance now that the servers are running
			// and get the ParkNPark context
			NameService nameService = new NameService(orb, err);
			if (!nameService.refreshParkNParkContext(false, true, null)) {
				stdErrWriter.write("Could not get the ParkNPark context from the naming service; exiting\n");
				stdErrWriter.flush();
				System.exit(1);
				return;
			}
			
			// Begin the fault injector if we are supposed to
			FaultInjector faultInjector = null;
			if (faultInjection) {
				System.out.println("Starting the fault injector");
				faultInjector = new FaultInjector(nameService, faultInjectionMinDelay, faultInjectionMaxDelay);
				faultInjector.start();
			}

			// Start each client
			System.out.println("Starting " + clients.size() + " clients");
			for (ProcessInfo client : clients)
			{
				// Build the command line
				System.out.println("  " + client.name);
				String commandLine = "ssh -x " + client.name +
				    " cd \"" + projectRoot + "/client\"; ./client --num-clients " + clients.size() +
				    " --interrequest-time " + interRequestTime  + " --num-servers " + servers.size() +
				    " --never-give-up --reply-size " + replySize + " --recovery-timeout " + recoveryTimeout +
				    " --detection-timeout " + detectionTimeout;
				
				// If in get-lots-mode, then pass in the requested number
				if (getLotsModeCount > 0)
					commandLine += " --get-lots-mode " + getLotsModeCount;
				else
					commandLine += " < ../tests/client-test > /dev/null";
				
				// Start this client instance and remember it
				Process newClient = Runtime.getRuntime().exec(commandLine);
				client.process = newClient;
				
				// Start the output copying
				client.stdout = new ClientInputCopier(client, new InputStreamReader(newClient.getInputStream()),
						                              new FileWriter("output/clients/" + client.name));
				client.stderr = new ErrorCopier(client, new InputStreamReader(newClient.getErrorStream()), stdErrWriter);
				client.stdout.start();
				client.stderr.start();
			}

			// Wait for each client to complete
			System.out.println("Waiting for the clients to complete");
			Thread currentThread = Thread.currentThread();
			boolean clientErrors = false;
			while (clients.size() > 0)
			{
				// Determine if any clients are active and, if so, sleep
				Iterator<ProcessInfo> clientIterator;
				boolean found = false;
				ProcessInfo current;
				synchronized(currentThread) {
					clientIterator = clients.iterator();
					while (!found && clientIterator.hasNext()) {
						current = clientIterator.next();
						if (!current.exited)
							found = true;
						else {
							clientIterator.remove();

							// If this was an abnormal exit, then remember that
							// we have at least one client error
							if (current.abnormalExit && !clientErrors)
								clientErrors = true;
						}
					}
					if (found) {
					    try {
							currentThread.wait();
						} catch (InterruptedException e) { }
					}
				}
			}
			
			// Stop injecting faults if we are doing fault injection
			if (faultInjection) {
				System.out.println("Waiting for the fault injector to finish");
				faultInjector.beginExit();
				faultInjector.interrupt();
				try {
				    faultInjector.join();
				} catch (InterruptedException e) {
					System.out.println("Interrupted while waiting for the fault injector to finish; wacky things might happen!");
				}
			}
			
			// We can now expect our servers to exit
			for (ProcessInfo server : servers)
			    server.exitExpected = true;
			
			// Tell each running server to exit
			System.out.println("Telling the servers to exit");
			nameService.getServers().clear();
			nameService.addServerListFromParkNParkContext(false, false, null);
			for (Server server : nameService.getServers()) {
				System.out.println("  " + server.serviceName);
				
				// Tell this server to exit programmatically
				try {
				    server.clientManagerFactory.exitServer();
				} catch (Exception e) { }
			}
			
			// Wait for the servers to finish exiting
			System.out.println("Waiting for the servers to complete");
			for (ProcessInfo server : servers)
			{
				// Close stdin, not caring if an exception is thrown (with
				// fault injection, it might be closed already)
				try {
				    server.process.getOutputStream().close();
				} catch (IOException e) { }
				
				// Wait for the server to exit
				server.process.waitFor();
				synchronized(server.stdout) {
					if (server.stdout.isAlive())
				        server.stdout.join();
				}
				synchronized(server.stderr) {
					if (server.stderr.isAlive())
					    server.stderr.join();
				}
			}
			
			// Close the sacred server and wait for it to exit
			System.out.println("Closing the connection to the replication manager and name service");
			sacredServer.exitExpected = true;
			sacredServer.process.destroy();
			synchronized(sacredServer.stdout) {
				if (sacredServer.stdout.isAlive())
					sacredServer.stdout.join();
			}
			synchronized(sacredServer.stderr) {
				if (sacredServer.stderr.isAlive())
					sacredServer.stderr.join();
			}
			
			// Shut down the replication manager
			System.out.println("Stopping the existing replication manager");
			sacredServer.process = Runtime.getRuntime().exec("ssh -x boggle \"" + projectRoot + "/sacredServer/killreplicationmanager\"");
			sacredServer.stdout = new InputCopier(new InputStreamReader(sacredServer.process.getInputStream()),
			                                      new FileWriter("output/sacredServer/" + sacredServer.name));
			sacredServer.stdout.start();
			if (sacredServer.process.waitFor() > 0)
				System.out.println("Could not kill the replication manager (possibly already dead)");
			
			// Shut down the name service
			System.out.println("Stopping the name service and resetting its database");
			sacredServer.process = Runtime.getRuntime().exec("ssh -x boggle killall orbd; rm -rf \"" + projectRoot + "/sacredServer/orb.db\"");
			sacredServer.stdout = new InputCopier(new InputStreamReader(sacredServer.process.getInputStream()),
                                                  new FileWriter("output/sacredServer/" + sacredServer.name));
			sacredServer.stdout.start();
			if (sacredServer.process.waitFor() > 0)
				System.out.println("Could not kill the name service (possily already dead)");

			// If one or more clients had errors, then notify the user and exit
			if (clientErrors) {
				System.out.println("One or more clients abnormally exited; exiting");
				System.exit(1);
				return;
			}
			
			// We are done!
			stdErrWriter.flush();
			System.out.println("Test scenario complete");
		} catch (InterruptedException e) {
			err.println(e.getMessage());
			err.flush();
		} catch (IOException e)  {
			err.println(e.getMessage());
			err.flush();
		} finally
		{
			// Restore System.err
			System.setErr(err);

			// Flush out anything remaining in our stderr writer
			try {
				stdErrWriter.flush();
			} catch (IOException e) {
				System.out.println("IOException while flushing stderr: " + e.toString());
			}
		}
	}
}
