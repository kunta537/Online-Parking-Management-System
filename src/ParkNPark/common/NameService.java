package ParkNPark.common;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CosNaming.Binding;
import org.omg.CosNaming.BindingHolder;
import org.omg.CosNaming.BindingIterator;
import org.omg.CosNaming.BindingIteratorHolder;
import org.omg.CosNaming.BindingListHolder;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.CosNaming.NamingContextPackage.AlreadyBound;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import ParkNPark.interfaces.ClientManagerFactory;
import ParkNPark.interfaces.ClientManagerFactoryHelper;

/**
 * Contains utility functions for working with the CORBA name service
 * and the ParkNPark name context
 */
public class NameService
{
	/** Object request broker */
	protected ORB orb;

	/** List of active servers */
	private ArrayList<Server> servers = new ArrayList<Server>();
	
	/** Naming context */
	private NamingContextExt nameServer;
	
	/** Our naming service's ParkNPark context, which contains server registrations */
	private NamingContext parkNParkContext = null;
	
	/** Whether or not a primary server registration exists as of the last call
	 *  to addServerListFromParkNParkContext() */
	protected boolean primaryServerRegistered;
	
	/** The original System.err instance */
	protected PrintStream err;
	
	/**
	 * Creates a new NameService instance using the given ORB
	 * @param orb The ORB to work with
	 * @param err The System.err instance to use
	 */
	public NameService(ORB orb, PrintStream err) {
		this.orb = orb;
		this.err = err;
	}
	
	/**
	 * Returns the set of ServerState objects
	 * @return The set of ServerState objects
	 */
	public List<Server> getServers() {
		return servers;
	}
	
	/**
	 * Returns the reference to the name service
	 * @return The reference to the name service
	 */
	public NamingContextExt getNameServer() {
		return nameServer;
	}
	
	/**
	 * Returns the ParkNPark name context, which can be null if the
	 * ParkNPark context was not initialized with a call to refreshParkNParkContext()
	 * @return The ParkNPark name context
	 */
	public NamingContext getParkNParkContext() {
		return parkNParkContext;
	}
	
	/**
	 * Returns whether or not a primary server registration exists as of the last call
	 *  to addServerListFromParkNParkContext()
	 * @return True when a primary server registration exists as of the last call
	 *  to addServerListFromParkNParkContext() or false otherwise
	 */
	public boolean isPrimaryServerRegistered() {
		return primaryServerRegistered;
	}
	
	/**
	 * Refreshes the parkNParkContext name service object that we use to get server
	 * bindings on. The name service is contacted from scratch in this method
	 * @param create When true and if the ParkNPark context does not exist in the
	 * name server, then this method will attempt to create it
	 * @param verbose If true, inform the user of possibly-active servers as each
	 * one is found
	 * @param logEntry A LogEntry object to log name service access times to when
	 * not null
	 * @return True if the parkNParkContext was refreshed or false if it was not
	 */
	public boolean refreshParkNParkContext(boolean create, boolean verbose, LogEntry logEntry)
	{	
		// Fetch the root naming context
		try {
			if (logEntry != null)
			    logEntry.setProbeNameServerIn();
			nameServer = NamingContextExtHelper.narrow(orb.resolve_initial_references("NameService"));
			if (logEntry != null)
			    logEntry.setProbeNameServerOut();
		}
		catch (Exception e) {
			if (logEntry != null)
			    logEntry.setProbeNameServerOut();
			err.println("Could not contact the naming service; please ensure that it is running.");
			err.flush();
			parkNParkContext = null;
			return false;
		}

        // Get the proper context object for ParkNPark
		NameComponent parkNParkContextName[];
		try 
		{
			if (verbose)
	            System.out.println("Fetching the ParkNPark name service context");
			if (logEntry != null)
			    logEntry.setProbeNameServerIn();
	        parkNParkContextName = nameServer.to_name("ParkNPark");
			if (logEntry != null)
			    logEntry.setProbeNameServerOut();
		} 
		catch (Exception e) 
		{
			if (logEntry != null)
			    logEntry.setProbeNameServerOut();
			System.out.println("Communication problem while communicating with the name server");
			err.flush();
			parkNParkContext = null;
			return false;
		}
        do {
            try 
            {
            	// Attempt to get the ParkNPark context from the name server
    			if (logEntry != null)
    			    logEntry.setProbeNameServerIn();
                parkNParkContext = NamingContextHelper.narrow(nameServer.resolve(parkNParkContextName));
    			if (logEntry != null)
    			    logEntry.setProbeNameServerOut();
            } catch (NotFound e) {
    			if (logEntry != null)
    			    logEntry.setProbeNameServerOut();

    			// Register the context if we are supposed to
    			if (create) {
                  	try {
                  		if (verbose)
                   		    System.out.println("ParkNPark name service context not found; creating");
            			if (logEntry != null)
            			    logEntry.setProbeNameServerIn();
                       	nameServer.bind_new_context(parkNParkContextName);
            			if (logEntry != null)
            			    logEntry.setProbeNameServerOut();
                   	} catch (AlreadyBound f) {
                   		if (verbose)
                   		    System.out.println("Naming context already created! Trying the fetch again");
            		} catch (SystemException f) {
            		    if (logEntry != null)
            			    logEntry.setProbeNameServerOut();
            			System.out.println("Communication problem while communicating with the name server");
            			parkNParkContext = null;
            			return false;
            		} catch (Exception f) {
            		    if (logEntry != null)
            			    logEntry.setProbeNameServerOut();
            			System.out.println("Miscellaneous exception (possibly a program bug?): " + f.getClass().getName());
            			parkNParkContext = null;
            			return false;
            		}
    			}
    			
    			// Otherwise, notify the user and return false
    			else {
    				System.out.println("ParkNPark name service context not found; is the ParkNPark server up?");
    				parkNParkContext = null;
    				return false;
    			}
    		} catch (SystemException e) {
    		    if (logEntry != null)
    			    logEntry.setProbeNameServerOut();
    			System.out.println("Communication problem while communicating with the name server");
    			parkNParkContext = null;
    			return false;
    		} catch (Exception e) {
    		    if (logEntry != null)
    			    logEntry.setProbeNameServerOut();
    			System.out.println("Miscellaneous exception (possibly a program bug?): " + e.getClass().getName());
    			parkNParkContext = null;
    			return false;
    		}
        } while (parkNParkContext == null);
		
		// We were able to get it or create it:-)
		return true;
	}
	
	/**
	 * Adds to the server List the ParkNParkContext's registered servers. If the name
	 * service cannot be contacted, it is contacted as a result of this call
	 * @param createParkNParkContext When true and if the ParkNPark context does not exist in the
	 * name server, then this method will attempt to create it
	 * @param verbose If true, inform the user of possibly-active servers as each
	 * one is found
	 * @param logEntry A LogEntry object to log name service access times to when
	 * not null
	 * @return True when the server list was successfully obtained (even if it's empty)
	 * or false if the server list was unavailable
	 */
	public boolean addServerListFromParkNParkContext(boolean createParkNParkContext, boolean verbose, LogEntry logEntry)
	{
		// If we don't have a ParkNPark context, then get it now
		if (parkNParkContext == null && !refreshParkNParkContext(createParkNParkContext, verbose, logEntry))
			return false;
		
		// Get the servers that are currently registered into a new set
		boolean debugMessages = false;
		while (true) {
			try {
				BindingListHolder listHolder = new BindingListHolder();
				BindingIteratorHolder listIteratorHolder = new BindingIteratorHolder();
				
				// See if the parkNParkContext is alive. If it's not, and we cannot refresh it,
				// then return false now
				try {
					if (verbose)
						System.out.println("Fetching the server list from the naming service");
					if (logEntry != null)
						logEntry.setProbeNameServerIn();
				    parkNParkContext.list(0, listHolder, listIteratorHolder);
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
				} catch (COMM_FAILURE e)
				{
					// Refresh the parkNPark context and, if that fails, notify the user
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
					if (debugMessages) System.out.println("Problem communicating with the name service; retrying...");
					if (!refreshParkNParkContext(createParkNParkContext, verbose, logEntry)) {
						System.out.println("Name service is not accessible");
						return false;
					}
					
					// Start from the beginning
					continue;
				} catch (OBJECT_NOT_EXIST e)
				{
					// Refresh the parkNPark context and, if that fails, notify the user
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
					if (debugMessages) System.out.println("Name service object disappeared; retrying...");
					if (!refreshParkNParkContext(createParkNParkContext, verbose, logEntry)) {
						System.out.println("Name service is not accessible");
						return false;
					}
					
					// Start from the beginning
					continue;
				} catch (SystemException e) {
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
					System.out.println("Communication problem while communicating with the name server");
					return false;
				} catch (Exception e) {
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
					System.out.println("Miscellaneous exception (possibly a program bug?): " + e.getClass().getName());
					return false;
				}
				
				// Iterate through the server list
				BindingIterator iterator = listIteratorHolder.value;
				BindingHolder currentBindingHolder = new BindingHolder();
				Binding currentBinding;
				ClientManagerFactory currentServer;
				boolean innerException = false, nextOne;
				if (logEntry != null)
					logEntry.setProbeNameServerIn();
				nextOne = iterator.next_one(currentBindingHolder);
				if (logEntry != null)
					logEntry.setProbeNameServerOut();
				while (!innerException && nextOne) {
					currentBinding = currentBindingHolder.value;
					
					// Narrow this server down to an object and add that server object reference
					if (currentBinding.binding_name != null) {
						try {
							// Only care about ClientManagerFactory registrations
							if (logEntry != null)
								logEntry.setProbeNameServerIn();
							org.omg.CORBA.Object registeredObject = parkNParkContext.resolve(currentBinding.binding_name);
							if (logEntry != null)
								logEntry.setProbeNameServerOut();
							if (registeredObject instanceof ClientManagerFactory) {
							    currentServer = ClientManagerFactoryHelper.narrow(registeredObject);
							    
							    // Get the name of the binding, which contains the IP address
								if (logEntry != null)
									logEntry.setProbeNameServerIn();
								
							    String serviceName = nameServer.to_string(currentBinding.binding_name);
								if (logEntry != null)
									logEntry.setProbeNameServerOut();
							    
								//System.out.println("Raw serviceName: " + serviceName);
						    	// Remove backslashes from the name (inserted by the name service)
							    serviceName = serviceName.replace("\\", "");
						    	
							    // Extract the IP address if this is a server registration with an IP address
							    int ipStart = serviceName.lastIndexOf('-');
							    if (ipStart > 0 && ipStart < serviceName.length() - 1) {
							    	String ipAddress = serviceName.substring(ipStart + 1);
							    	
							    	// Add this server registration
							    	servers.add(new Server(ipAddress, serviceName, currentServer));
							    	if (verbose)
									    System.out.println("Server at " + ipAddress + " [" + serviceName + "] is possibly active.");
							    }
							}
						} catch (NotFound e) {
							if (logEntry != null)
								logEntry.setProbeNameServerOut();
							System.out.println("A server's name registration went away during the server registration fetch; ignoring that server");
							innerException = true;
						} catch (InvalidName e) {
							if (logEntry != null)
								logEntry.setProbeNameServerOut();
							System.out.println("A server's name registration somehow became syntactically invalid; ignoring that server");
							innerException = true;
						} catch (CannotProceed e) {
							if (logEntry != null)
								logEntry.setProbeNameServerOut();
							System.out.println("A server's name registration could not be retrieved due to an unspecified reason (CannotProceed exception); ignoring that server");
							innerException = true;
						}
					}
					
					// Get the next entry
					if (logEntry != null)
						logEntry.setProbeNameServerIn();
					nextOne = iterator.next_one(currentBindingHolder);
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
				}
				
				// If we encountered an exception while iterating, then try again
				if (innerException)
					continue;
				
				// Discover which server is primary by looking up the primary name
				primaryServerRegistered = false;
				try {
					if (verbose) System.out.println("Looking up the primary server from the name service");
					if (logEntry != null)
						logEntry.setProbeNameServerIn();
					NameComponent[] serverName = nameServer.to_name("ParkNParkPrimary");
					ClientManagerFactory factory = ClientManagerFactoryHelper.narrow(parkNParkContext.resolve(serverName));
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
					primaryServerRegistered = true;
					
					// Find which server equals the primary
					boolean found = false;
					Iterator<Server> serverIterator = servers.iterator();
					Server current;
					while (!found && serverIterator.hasNext()) {
						current = serverIterator.next();
						
						// If this is the same as the primary, then make this primary
						if (factory._is_equivalent(current.clientManagerFactory)) {
							current.isPrimary = true;
							found = true;
						}
					}
					
					// If we found a primary, then return now. Otherwise, we'll need
					// set the last server as primary
					if (found)
						return true;
				} catch (InvalidName e) {
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
					System.out.println("Unexpected exception: " + e.toString());
				} 
				catch (NotFound e)
				{
					// No servers registered
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
					if (verbose) System.out.println("No servers are active at this time");
				} 
				catch (CannotProceed e) {
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
					System.out.println("Unexpected exception: " + e.toString());
				} 
				catch (COMM_FAILURE e)
				{
					// Refresh the parkNPark context and, if that fails, notify the user
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
					System.out.println("Problem communicating with the name service; retrying...");
				} catch (OBJECT_NOT_EXIST e)
				{
					// Refresh the parkNPark context and, if that fails, notify the user
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
					System.out.println("Name service object disappeared; assuming last server is primary");
				} catch (SystemException e) {
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
					System.out.println("Communication problem while communicating with the name server");
				} catch (Exception e) {
					if (logEntry != null)
						logEntry.setProbeNameServerOut();
					System.out.println("Miscellaneous exception (possibly a program bug?): " + e.getClass().getName());
				}
				
				// We had an exception, so assume that the last server is primary
				if (servers.size() > 0) {
					System.out.println("Could not find the primary server in the list of servers; assuming that the last server is primary");
				    servers.get(servers.size() - 1).isPrimary = true;
				}
				return true;
			} catch (SystemException e) {
				if (logEntry != null)
					logEntry.setProbeNameServerOut();
				System.out.println("CORBA problem while fetching the server set from the name server: " + e.getClass().getName());
				return false;
			} catch (Exception e) {
				if (logEntry != null)
					logEntry.setProbeNameServerOut();
				System.out.println("Miscellaneous exception (possibly a program bug?): " + e.getClass().getName());
				return false;
			}
		}
	}	
}
