package ParkNPark.middletier;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextPackage.AlreadyBound;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import ParkNPark.common.CommandLineParser;
import ParkNPark.common.InputEater;
import ParkNPark.common.NameService;
import ParkNPark.common.Server;
import ParkNPark.interfaces.ClientManagerFactory;
import ParkNPark.interfaces.ClientManagerFactoryHelper;
import ParkNPark.interfaces.ReplicationManagerHelper;
import ParkNPark.interfaces.ReplicationManagerPOA;
import ParkNPark.interfaces.ServiceUnavailableException;

public class ReplicationManager extends ReplicationManagerPOA {
	/** Object request broker */
	private ORB orb;

	/** Root POA */
	private POA rootPOA;

	/** The project root, which is used when restarting a failed server */
	protected String projectRoot;

	/** Instance of our common name service management object */
	protected NameService nameService;

	/**
	 * Variables for real-time analysis
	 */

	/** Number of clients */
	protected int numClients;

	/** Number of servers */
	protected int numServers;

	/** Time between requests */
	protected int interRequestTime;

	/** Size of reply */
	protected int replySize;

	/** The wait timeout to use during fault detections */
	protected int timeout;

	/** The original System.err instance */
	protected PrintStream err;

	/** The JDBC URL */
	private String jdbcURL;

	/** The JDBC user name */
	private String jdbcUsername;

	/** The JDBC password */
	private String jdbcPassword;

	/**
	 * Entry point of replication manager
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Parse the command line
		CommandLineParser clp = new CommandLineParser();
		Set<CommandLineParser.AcceptedParameters> acceptedParameters = new HashSet<CommandLineParser.AcceptedParameters>();
		acceptedParameters
				.add(CommandLineParser.AcceptedParameters.ORBInitialHost);
		acceptedParameters
				.add(CommandLineParser.AcceptedParameters.ORBInitialPort);
		acceptedParameters
				.add(CommandLineParser.AcceptedParameters.ORBServerHost);
		acceptedParameters
				.add(CommandLineParser.AcceptedParameters.ORBServerPort);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.numClients);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.numServers);
		acceptedParameters
				.add(CommandLineParser.AcceptedParameters.interRequestTime);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.replySize);
		acceptedParameters
				.add(CommandLineParser.AcceptedParameters.detectionTimeout);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.jdbcURL);
		acceptedParameters
				.add(CommandLineParser.AcceptedParameters.jdbcUsername);
		acceptedParameters
				.add(CommandLineParser.AcceptedParameters.jdbcPassword);

		if (!clp.parseCommandLine(ReplicationManager.class.getName(), args,
				acceptedParameters)) {
			System.err.println("Exiting!!");
			System.exit(1);
			return;
		}
		// Create our replication manager
		ReplicationManager rm = new ReplicationManager();

		// Set System.err to the input eater
		rm.err = System.err;
		System.setErr(new PrintStream(new InputEater()));

		// Set the real-time analysis parameters
		rm.numClients = clp.getNumClients();
		rm.numServers = clp.getNumServers();
		rm.interRequestTime = clp.getInterRequestTime();
		rm.replySize = clp.getReplySize();
		rm.timeout = clp.getFaultDetectionTimeout();
		rm.jdbcURL = clp.getJDBCURL();
		rm.jdbcUsername = clp.getJDBCUsername();
		rm.jdbcPassword = clp.getJDBCPassword();

		// Get our project root from the current working folder
		File projectRoot = new File(System.getProperty("user.dir", ""));

		// if (projectRoot.getName().equals("sourcecode"))//sacredServer
		rm.projectRoot = projectRoot.getAbsolutePath();// projectRoot.getParentFile().getAbsolutePath();
		// else
		// rm.err.println("Current working folder is not \"sourcecode\"; automatic server recovery is disabled");

		// Set up replication manager CORBA objects
		rm.setupReplicationManagerCORBAObjects(args);
		rm.nameService = new NameService(rm.orb, rm.err);

		// Refresh the ParkNPark naming context. If not successful, return now
		if (!rm.nameService.refreshParkNParkContext(true, true, null)) {
			rm.err.println("Could not get nor create the ParkNPark naming context; exiting");
			System.exit(1);
			return;
		}

		// Register the replication manager's name in the naming service
		rm.registerReplicationManager();

		// Initialize the server list from the ParkNPark context
		rm.nameService.addServerListFromParkNParkContext(true, true, null);

		// Get the primary server
		boolean foundPrimaryServer = false;
		List<Server> servers = rm.nameService.getServers();
		for (int i = 0; !foundPrimaryServer && i < servers.size(); i++) {
			if (servers.get(i).isPrimary) {
				// Set the primary server using the next index. If we're
				// at the end of the list, then use zero
				System.out
						.println("Activating the previously-registered active server");
				if (i == servers.size() - 1)
					rm.setPrimary(0);
				else
					rm.setPrimary(i + 1);
				foundPrimaryServer = true;
			}
		}

		// If we still don't have the primary server, then use the last server
		if (!foundPrimaryServer) {
			// Set the active server to the last server
			System.out.println("Activating the last registered server");
			rm.setPrimary(0);
		}

		// Notify the user that the server is now running
		System.out.println("Server running.");

		// Continuously check the servers
		while (true)
			rm.checkServers();
	}

	/**
	 * Called by servers when they start up.
	 * 
	 * @param ipAddress
	 * @param serviceName
	 * @param cmf
	 */
	public synchronized void serverRegistered(String ipAddress,
			String serviceName, ClientManagerFactory cmf) {
		// Remove any existing entry for this server
		boolean setPrimary = removeExistingEntries(serviceName);

		// Register this server
		List<Server> servers = nameService.getServers();
		servers.add(new Server(ipAddress, serviceName, cmf));
		System.out.println("Server at " + ipAddress + " [" + serviceName
				+ "] is now active.");

		// If this is the only active server or if it was removed from a match
		// in removeExistingEntries(), then set it as primary
		if (setPrimary || servers.size() == 1)
			setPrimary(0);
	}

	/**
	 * Shared static instance of the input eater output stream wrapped in a
	 * PrintStream
	 */
	protected static PrintStream inputEater = new PrintStream(new InputEater());

	/**
	 * Check whether or not servers are active
	 */
	public void checkServers() {
		System.err.println("checkServers...");

		boolean serverFailure = false;
		List<Server> servers = nameService.getServers();
		for (int i = 0; i < servers.size(); i++) {
			Server s = servers.get(i);

			// Check to see if server is active
			try {

				// Tries to connect to the database, if fails 3 times throws an
				// exception and the server is restarted

				s.clientManagerFactory.poke();

			} catch (Exception e) { // Remove from active servers list
				e.printStackTrace();
				servers.remove(i);
				System.out.println("Server at " + s.ipAddress + " ["
						+ s.serviceName + "] has failed.");

				// If this server is the primary, then set a new primary
				if (s.isPrimary)
					setPrimary(i);

				// Remove service name from naming service
				// TODO: If the server is manually started and calls
				// serverRegistered()
				// between the line above and the line below, we might
				// unregister a
				// possibly healthy server. How can we fix this (I haven't
				// thought through it yet)?
				// (access to the servers List should probably be synchronized
				// as ArrayList per-se
				// is not thread-safe..)

				// FIXME: The TODO above can be triggered by starting a server
				// and, after it's ready,
				// immediately ctrl+c-ing it and restarting it. Several tries
				// might be necessary,
				// but the effect is the message:
				// "Could not unbind service go-172.19.132.119 due to exception org.omg.CosNaming.NamingContextPackage.NotFound"
				// (it tried to unregister it twice)
				unbindServer(s);

				// Restart the failed server
				if (e instanceof ServiceUnavailableException)
					startServer(s, true);
				else
					startServer(s, false);

				// We had a server failure, so we should not go to sleep before
				// the next call to
				// checkServers()
				serverFailure = true;
			}
		}

		// Sleep before our returning if we did not encounter any server
		// failures in this call
		if (!serverFailure) {
			try {
				Thread.sleep(timeout);
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Set one of the active servers as the primary
	 * 
	 * @param index
	 */
	public synchronized void setPrimary(int index) {
		Server s;

		// If only one server, make it primary
		List<Server> servers = nameService.getServers();
		if (servers.size() == 1) {
			s = servers.get(0);
		} else if (servers.size() == 0) {
			System.out
					.println("No servers are active; notifying name service that no server is primary");

			// Register selected server as the primary in the naming service
			try {
				NameComponent[] serverName = nameService.getNameServer()
						.to_name("ParkNParkPrimary");
				nameService.getParkNParkContext().unbind(serverName);
			} catch (NotFound e) {
				System.out.println("No primary server was registered");
			} catch (Exception e) {
				err.println("Could not remove the primary server reference from the name service due to exception: "
						+ e.getClass().getName());
			}
			return;
		} else { // Set primary server using most-recently-active mechanism
			if (index > 0)
				s = servers.get(index - 1);
			else
				s = servers.get(servers.size() - 1);
		}

		// If we don't have our ParkNPark naming context, then get it from the
		// server now.
		if (nameService.getParkNParkContext() == null
				&& !nameService.refreshParkNParkContext(true, true, null)) {
			err.println("Could not get ParkNPark context");
			return;
		}

		// Set selected server as the primary
		System.out.println("Setting server at " + s.ipAddress + " ["
				+ s.serviceName + "] as the primary.");

		// Register selected server as the primary in the naming service
		try {
			NameComponent[] serverName = nameService.getNameServer().to_name(
					"ParkNParkPrimary");
			nameService.getParkNParkContext().rebind(serverName,
					ClientManagerFactoryHelper.narrow(s.clientManagerFactory));
			s.isPrimary = true;
		} catch (Exception e) {
			err.println("Could not set server at " + s.ipAddress
					+ " as the primary due to exception: "
					+ e.getClass().getName());
			err.flush();
		}
	}

	/**
	 * Unbind the failed server's service name from the naming service
	 * 
	 * @param s
	 */
	public void unbindServer(Server s) {
		// If we don't have our ParkNPark naming context, then get it from the
		// server now.
		if (nameService.getParkNParkContext() == null
				&& !nameService.refreshParkNParkContext(true, true, null)) {
			err.println("Could not get ParkNPark context");
			return;
		}

		// Unbind the server from the naming service
		try {
			NameComponent[] serverName = nameService.getNameServer().to_name(
					s.serviceName);
			nameService.getParkNParkContext().unbind(serverName);
		} catch (Exception e) {
			err.println("Could not unbind service " + s.serviceName
					+ " due to exception " + e.getClass().getName());
		}
	}

	/**
	 * Start a new server or restart a corrupted server
	 * 
	 * @param s
	 * @param corrupted
	 */
	public void startServer(Server s, boolean corrupted) {
		System.out.println("startServer- " + s.serviceName);
		// If server starting is disabled, then return now
		if (projectRoot == null)
			return;

		// If corrupted, then we first need to kill the server
		if (corrupted)
			System.out.println("Server at " + s.ipAddress + " ["
					+ s.serviceName
					+ "] is having database connectivity problems; killing");

		// Extract the host name from the service name
		String hostName = s.serviceName
				.substring(0, s.serviceName.indexOf('-'));

		// Start the new server
		System.out.println("Restarting server at " + s.ipAddress + " ["
				+ s.serviceName + "].");
		try {
			// Attempt to flush the log files on corrupted servers before
			// killing them
			if (corrupted) {
				try {
					s.clientManagerFactory.killServer(); // This flushes the
															// logs and prevents
															// concurrent client
															// requests from
															// succeeding
				} catch (Exception e) {
					System.out.println("Could not flush log files on server "
							+ s.ipAddress + " [" + s.serviceName
							+ "]; server's log data may be lost");
				}
			}

			// Start the script that will restart the server with log appending
			/*
			 * String restartCmd = "./restartserver 83439 " + hostName + " " +
			 * ((corrupted)? "1": "0") + " " + projectRoot + " --num-clients " +
			 * numClients + " --interrequest-time " + interRequestTime +
			 * " --reply-size " + replySize + " --num-servers " + numServers +
			 * " --append-logs " + " --jdbc-url \"" + jdbcURL.replace("\"",
			 * "\\\"") + "\" --jdbc-username \"" + jdbcUsername.replace("\"",
			 * "\\\"") + "\" --jdbc-password \"" + jdbcPassword.replace("\"",
			 * "\\\"") + "\"";
			 */

			String serverPort = hostName.substring(s.serviceName.indexOf(":") + 1);
			String restartCmd = projectRoot + "\\restartserver.bat " + serverPort;

			//System.out.println("restartCmd: " + restartCmd);
			Process process = Runtime.getRuntime().exec(restartCmd);

			// In a separate thread, listen for the results
			(new StartupListener(s, process)).start();
		} catch (IOException e) {
			err.println("Could not restart the server at " + s.ipAddress + " ["
					+ s.serviceName + "]: " + e.getMessage());
		}
	}

	/**
	 * Listens on a server process to determine if it started up successfully or
	 * not
	 */
	protected class StartupListener extends Thread {
		/** The server process to listen for success messages on */
		protected Process process;

		/** The server that is being restarted */
		protected Server server;

		/**
		 * Creates a new startup listener that will listen for the successful or
		 * unsuccessful startup of the given server process. After success or
		 * failure is ascertained, this thread exits
		 * 
		 * @param server
		 *            The server that is being listened to
		 * @param process
		 *            The server process to listen for successful messages on
		 */
		public StartupListener(Server server, Process process) {
			this.server = server;
			this.process = process;
		}

		/**
		 * Runs the thread that determines if the server started up successfully
		 * or not
		 */
		@Override
		public void run() {
			// Copy all lines from the reader to the writer and consult
			// with processLine() to determine if we should stop early
			String line = null;
			boolean stop = false;
			int pid = -1;
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			try {
				while ((!stop || pid < 0) && (line = reader.readLine()) != null) {
					// If this line is the PID, then remember it
					if (line.startsWith("PID=")) {
						try {
							pid = Integer.parseInt(line.substring(4), 10);
						} catch (NumberFormatException e) {
							err.println("Could not parse the PID from the server restart script!");
							err.flush();
							e.printStackTrace();
							stop = true;
							line = null;
						}
					}

					// If this line is the server success message, then we're
					// done
					else if (line.equals("Server running.")) {
						System.out.println("Server at " + server.ipAddress
								+ " [" + server.serviceName
								+ "] restarted successfully.");
						stop = true;
					}
				}

				if (pid != -1) {
					// Runtime.getRuntime().exec("kill " + pid);//linux
					Runtime.getRuntime().exec("taskkill -f -pid " + pid);//windows
				}
			} catch (IOException e) {
				err.println("Problem restarting the server at "
						+ server.ipAddress + " [" + server.serviceName + "]: "
						+ e.getMessage());
				err.flush();
			}

			// Close the reader
			try {
				reader.close();
			} catch (IOException e) {
				err.println("Problem while closing output reader while monitoring the restart of server at "
						+ server.ipAddress
						+ " ["
						+ server.serviceName
						+ "]: "
						+ e.getMessage());
			}

			// Close the stderr
			try {
				process.getErrorStream().close();
			} catch (IOException e) {
				err.println("Problem while closing error reader while monitoring the restart of server at "
						+ server.ipAddress
						+ " ["
						+ server.serviceName
						+ "]: "
						+ e.getMessage());
			}

			// Destroy the process (the server, if running, was backgrounded and
			// will remain running)
			process.destroy();

			// If the server could not be successfully restarted, wait for 15
			// seconds
			// and insert it back into the active server list so that it can be
			// rechecked and, if it's still down, a restart can be attempted
			// again
			if (line == null) {
				System.out
						.println("Waiting for 15 seconds before attempting a restart of server at "
								+ server.ipAddress
								+ " ["
								+ server.serviceName
								+ "]");
				try {
					sleep(15000);
				} catch (InterruptedException e) {
				}

				// Insert this registration back into the active registration
				// list
				// if it's not already in there
				synchronized (ReplicationManager.this) {
					List<Server> servers = nameService.getServers();
					Iterator<Server> serverIterator = servers.iterator();
					boolean found = false;
					while (!found && serverIterator.hasNext()) {
						if (serverIterator.next().serviceName
								.equals(server.serviceName))
							found = true;
					}
					if (!found)
						servers.add(server);
				}
			}
		}
	}

	/**
	 * Removes existing server entries that match the given service name. If a
	 * match is the current primary, this method returns true
	 * 
	 * @param serviceName
	 *            The service name to remove matching entries of
	 */
	protected synchronized boolean removeExistingEntries(String serviceName) {
		// Remove servers that match the given service name
		boolean removedPrimary = false;
		Iterator<Server> serverIterator = nameService.getServers().iterator();
		Server current;
		while (serverIterator.hasNext()) {
			current = serverIterator.next();
			if (current.serviceName.equals(serviceName)) {
				if (current.isPrimary)
					removedPrimary = true;
				serverIterator.remove();
			}
		}

		// Return whether or not we removed the primary
		return removedPrimary;
	}

	/**
	 * Setup CORBA objects
	 */
	protected void setupReplicationManagerCORBAObjects(String args[]) {
		// Initialize the ORB
		System.out.println("Starting the CORBA ORB");
		try {
			orb = ORB.init(args, null);
		} catch (Exception e) {
			System.out.println("Exception while creating ORB...terminating");
			System.exit(1);
		}

		// Get the root POA and activate it
		try {
			System.out.println("Activating the CORBA root POA");
			rootPOA = POAHelper.narrow(orb
					.resolve_initial_references("RootPOA"));
			rootPOA.the_POAManager().activate();
		} catch (Exception e) {
			System.out
					.println("Exception while creating ORB; perhaps the port is in use due to another server instance on this server? Terminating");
			System.exit(1);
			return;
		}
	}

	/**
	 * Registers the replication manager's name with the name service. You must
	 * have called refreshParkNParkContext() before calling this method
	 * 
	 * @return
	 */
	protected boolean registerReplicationManager() {
		try {
			NameComponent parkNParkReplicationManager[] = nameService
					.getNameServer().to_name("ParkNParkReplicationManager");

			// Register the replication manager servant
			System.out
					.println("Registering the replication manager name with the CORBA naming service");
			try {
				nameService.getParkNParkContext().bind(
						parkNParkReplicationManager,
						ReplicationManagerHelper.narrow(rootPOA
								.servant_to_reference(this)));
			} catch (AlreadyBound e) {
				System.out
						.println("Replication manager already registered! Overwriting the old name registration with myself");
				nameService.getParkNParkContext().rebind(
						parkNParkReplicationManager,
						ReplicationManagerHelper.narrow(rootPOA
								.servant_to_reference(this)));
			}
		} catch (Exception e) {
			err.println("CORBA problem while communicating with the name server");
			return false;
		}
		return true;
	}
}
