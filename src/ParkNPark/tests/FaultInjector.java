package ParkNPark.tests;

import java.util.Iterator;
import java.util.List;

import ParkNPark.common.NameService;
import ParkNPark.common.Server;

/**
 * Fault injector background thread that injects random faults into
 * the servers.
 * 
 * This fault injector presently implements the following faults:
 * <ul><li>Server killing</li>
 * <li>Server database connection hosing</li>
 * </ul>
 */
public class FaultInjector extends Thread
{
	/** Instance of our common name service management object */
	protected NameService nameService;
	
	/** Our thread exit flag */
	protected boolean exit = false;

	/** The minimum delay of the fault injection in milliseconds */
	protected int faultInjectionMinDelay;
	
	/** The maximum delay of the fault injection in milliseconds */
	protected int faultInjectionMaxDelay;

	/**
	 * Creates a new fault injector using the given NameService
	 * helper object
	 * @param nameService The NameService helper object to query
	 * the running servers from
	 * @param faultInjectionMinDelay The minimum delay of the fault
	 * injector in milliseconds
	 * @param faultInjectionMinDelay The maximum delay of the fault
	 * injector in milliseconds
	 */
	public FaultInjector(NameService nameService, int faultInjectionMinDelay, int faultInjectionMaxDelay) {
		this.nameService = nameService;
		this.faultInjectionMinDelay = faultInjectionMinDelay;
		if (faultInjectionMinDelay > faultInjectionMaxDelay)
			throw new IllegalArgumentException("faultInjectionMinDelay must be <= faultInjectionMaxDelay");
		this.faultInjectionMaxDelay = faultInjectionMaxDelay - faultInjectionMinDelay;
	}
	
	/**
	 * Tells this thread to begin its graceful shutdown
	 */
	public void beginExit() {
		exit = true;
	}

	/**
	 * Runs the fault injector
	 */
	@Override
	public void run()
	{
		// Keep on going until we're told to exit
		List<Server> servers = nameService.getServers();
		long waitTime;
		try {
			while (!exit)
			{
				// Wait for a random amount of time between the minimum and the maximum delay
				// or, if min == max, wait for exactly that time
				try {
					if (faultInjectionMinDelay == faultInjectionMaxDelay)
						waitTime = faultInjectionMinDelay;
					else
						waitTime = faultInjectionMinDelay + Math.round(Math.random() * faultInjectionMaxDelay);
					sleep(waitTime);
				} catch (InterruptedException e)
				{
					// Loop back. If we're exiting, then this will cause us to exit immediately
					continue;
				}
				
				// Refresh the server list
				servers.clear();
				nameService.addServerListFromParkNParkContext(false, false, null);
				
				// If we don't have any servers, then loop back
				if (servers.size() == 0) {
					System.out.println("--Skipping this fault injection because no servers are registered");
					continue;
				}
				
				// Choose the primary server
				Server server = null, current;
				Iterator<Server> serverIterator = servers.iterator();
				while (server == null && serverIterator.hasNext()) {
					current = serverIterator.next();
					if (current.isPrimary)
						server = current;
				}
				
				// If we still don't have the primary server, then choose a random
				// number after informing the user
				if (server == null)
				{
					// Pick a random victim
					System.out.println("Could not find primary server; using a random server");
					int victim = (int) Math.floor(Math.random() * servers.size());
					server = servers.get(victim);
				}

				// Randomly choose a fault to inject from our lovely selection of 2
				int faultToInject = 0; // we're doing kill-only for now  (int) Math.floor(Math.random() * 2);
				switch (faultToInject) {
				    case 0 :
				    {
				    	// Kill the server
						System.out.println("**Injecting kill-server fault into " + server.serviceName);
				    	try {
				    		server.clientManagerFactory.killServer();
				    	} catch (Exception e) {
				    		// Technically, we will always get an exception because the server dies
				    		// in the process; ignore
				    	}
				    	
				    	// It was Professor Plum in the conservatory with the wrench
				    	break;
				    }
				    
				    case 1 :
				    {
				    	// Hose the database connection
						System.out.println("**Injecting database-connection-hose fault into " + server.serviceName);
						try {
							server.clientManagerFactory.hoseDatabaseConnection();
				    	} catch (Exception e) {
				    		// Replication manager might not have removed this server's reference yet
				    	}
				    	break;
				    }
				}
			}
		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}
	}
}
