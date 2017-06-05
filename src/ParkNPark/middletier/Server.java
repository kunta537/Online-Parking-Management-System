package ParkNPark.middletier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.CosNaming.NamingContextPackage.AlreadyBound;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import ParkNPark.common.CommandLineParser;
import ParkNPark.common.InputEater;
import ParkNPark.common.Logger;
import ParkNPark.interfaces.ClientManagerFactoryHelper;
import ParkNPark.interfaces.ReplicationManager;
import ParkNPark.interfaces.ReplicationManagerHelper;

/**
 * Starts the server from scratch by activating CORBA, registering our client
 * manager factory with the naming service, and handing control over to CORBA
 */
public class Server
{
	/**
	 * Starts the ParkNPark server
	 * @param args
	 */
	public static void main(String[] args)
	{
        NamingContextExt nameServer;
        ORB orb = null;
	  
        // Parse the command line
        CommandLineParser clp = new CommandLineParser();
		Set<CommandLineParser.AcceptedParameters> acceptedParameters = new HashSet<CommandLineParser.AcceptedParameters>();
		acceptedParameters.add(CommandLineParser.AcceptedParameters.ORBInitialHost);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.ORBInitialPort);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.ORBServerHost);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.ORBServerPort);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.numClients);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.numServers);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.interRequestTime);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.replySize);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.appendLogs);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.jdbcURL);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.jdbcUsername);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.jdbcPassword);
        if (!clp.parseCommandLine(Server.class.getName(), args, acceptedParameters)) {
        	System.exit(1);
        	return;
        }
        
        // Initialize the ORB
        System.out.println("Starting the CORBA ORB");
        try {
            orb = ORB.init(args, null);
        } catch (Exception e) {
        	System.out.println("Exception while activating the root POA; perhaps your JVM is malfunctioning");
        	System.exit(1);
        	return;
        }

        // Get the root POA and activate it
        POA rootPOA;
        PrintStream err = System.err, inputEater = new PrintStream(new InputEater());
        try {
        	System.setErr(inputEater);
            System.out.println("Activating the CORBA root POA");
            rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPOA.the_POAManager().activate();
            System.setErr(err);
        } catch (Exception e) {
            System.setErr(err);
        	System.out.println("Exception while creating ORB; perhaps the port is in use due to another server instance on this server?");
        	System.exit(1);
        	return;
        }
       
        System.out.println("Fetching the name service reference");
        try {
            // Get the naming service object
        	System.setErr(inputEater);
            nameServer = NamingContextExtHelper.narrow(orb.resolve_initial_references("NameService"));
            System.setErr(err);
        } catch (Exception e) {
            System.setErr(err);
        	System.out.println("Cannot connect to the name service; please ensure that it is running and try again");
        	System.exit(1);
        	return;
        }
        
        // Get the server's primary IP address and host name
        String ipAddr, hostName, registrationName;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            ipAddr = addr.getHostAddress();
            hostName = addr.getHostName();
            
            registrationName = hostName + ":" + clp.getServerPort() + "-" + ipAddr;
        } catch (UnknownHostException e) {
           	System.out.println("Could not get IP address! Exiting");
           	System.exit(1);
           	return;
        }
        
		// Create our Logger instance
		Logger logger = new Logger(clp.getNumClients(), clp.getNumServers(), 11000 /* requests per client */,
				                   clp.getInterRequestTime(), clp.getReplySize(), "srv",
				                   hostName, true, false, clp.shouldAppendLogs());

        // Create our client manager factory servant object
        System.out.println("Creating the client manager factory");
        ClientManagerFactoryImpl clientManagerFactory;
        try {
            clientManagerFactory = new ClientManagerFactoryImpl(logger, clp.getReplySize(),
            		clp.getJDBCURL(), clp.getJDBCUsername(), clp.getJDBCPassword());
        } catch (SQLException e) {
        	System.err.println("Problem connecting to the database while creating client manager factory: " + e.toString() + "; exiting");
        	System.err.flush();
        	System.exit(1);
        	return;
        }

        err = System.err;
        System.setErr(inputEater);
        try {
            // Get the proper context object for ParkNPark
            NameComponent serverName[], parkNParkContextName[];
            System.out.println("Fetching the ParkNPark name service context");
            parkNParkContextName = nameServer.to_name("ParkNPark");
            NamingContext parkNParkContext = null;
            do {
                try {
                    parkNParkContext = NamingContextHelper.narrow(nameServer.resolve(parkNParkContextName));
                } catch (NotFound e)
                {
                 	// Register the context
                  	try {
                   		System.out.println("ParkNPark name service context not found; creating");
                       	nameServer.bind_new_context(parkNParkContextName);
                   	} catch (AlreadyBound f) {
                   		System.out.println("Naming context already created! Trying the fetch again");
                   	}
                }
            } while (parkNParkContext == null);
            
            // Get the replication manager instance
            ReplicationManager replicationManager;
            try {
            	NameComponent replicationManagerName[] = nameServer.to_name("ParkNParkReplicationManager");
            	replicationManager = ReplicationManagerHelper.narrow(parkNParkContext.resolve(replicationManagerName));
            } catch (NotFound e) {
            	System.out.println("Warning: Could not find the replication manager");
            	replicationManager = null;
            }
            
            // Register client manager factory with the naming service
            System.out.println("Registering server as " + registrationName + " with the CORBA naming service");
            serverName = nameServer.to_name(registrationName);
            try {
                parkNParkContext.bind(serverName, ClientManagerFactoryHelper.narrow(rootPOA.servant_to_reference(clientManagerFactory)));
            } catch (AlreadyBound e) {
            	System.out.println("Server already registered! Overwriting the old name registration with myself");
                parkNParkContext.rebind(serverName, ClientManagerFactoryHelper.narrow(rootPOA.servant_to_reference(clientManagerFactory)));
            }

            // Attempt to register with the replication manager, not caring if it does not work
            // because the replication manager can pick us up from the name service later
            if (replicationManager != null) {
                System.out.println("Notifying the replication manager that we are now active");
                try {
                    replicationManager.serverRegistered(ipAddr, registrationName, ClientManagerFactoryHelper.narrow(rootPOA.servant_to_reference(clientManagerFactory)));
                } catch (SystemException e) {
                	System.out.println("Warning: CORBA communication problem while trying to notify the replication manager that we are up: " + e.getClass().getName());
                } catch (Exception e) {
                    System.setErr(err);
                    e.printStackTrace();
                    System.exit(1);
                    return;
                }
            }
        } catch (Exception e) {
        	System.out.println("Could not register with the name service due to an exception " + e.getClass().getName() + "; exiting");
            System.setErr(err);
        	e.printStackTrace();
           	System.exit(1);
           	return;
        }
        System.setErr(err);
        
        // Notify the user that the server is now running
        System.out.println("Server running.");
        
        // Wait for the user to press Enter to flush the logs to disk
        BufferedReader inputReader  = new BufferedReader(new InputStreamReader(System.in));
        String command;
        while (true)
        {
        	// Display the menu
        	System.out.println("Server console:");
        	System.out.println("f Flush the logs to disk");
        	System.out.println("h Hose the database server connection");
        	System.out.println("k Kill server (but flush the logs first)");
        	System.out.println("x Exit");
        	
        	// Get the input from the user or, if the input is shut down,
        	// then hand control over to orb.run()
        	try {
        	    command = inputReader.readLine();
        	    
        	    // If the command is null, the input was just closed, so now
        	    // we wait around until we're killed
        	    if (command == null) {
            	    System.out.println("Backgrounding the server");
            	    orb.run();
            	    
            	    // After CORBA shuts down, assume that we are exiting normally
            	    command = "x";
        	    }
        	} catch (IOException e) {
        		System.out.println("Cannot read input from the menu; assuming it's an exit");
        		command = "x";
        	}
        	
        	// Hose the database connection
        	if (command.equalsIgnoreCase("h"))
        		clientManagerFactory.hoseDatabaseConnection();
        	
        	// Flush the buffers
        	if (command.equalsIgnoreCase("f"))
        		clientManagerFactory.flushLogs();
        	
        	// Kill the server
        	if (command.equalsIgnoreCase("k"))
        		clientManagerFactory.killServer();
        	
        	// Exit the server
        	if (command.equalsIgnoreCase("x"))
        		clientManagerFactory.exitServer();
        }
    }
}
