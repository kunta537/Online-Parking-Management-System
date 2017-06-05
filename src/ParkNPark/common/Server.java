package ParkNPark.common;

import ParkNPark.interfaces.ClientManagerFactory;

/**
 * Contains information on a single server's registration
 */
public class Server
{
	/** IP address for server */
	public String ipAddress;
	
	/** Service name of server in the naming service */
	public String serviceName;
	
	/** ClientManagerFactory for this server */
	public ClientManagerFactory clientManagerFactory;
	
	/** Flag indicating whether or not this server is the primary server */
	public boolean isPrimary;
	
	public Server(String ipAddress, String serviceName, ClientManagerFactory cmf)
	{
		this.ipAddress = ipAddress;
		this.serviceName = serviceName;
		this.clientManagerFactory = cmf;
		this.isPrimary = false;
	}
}
