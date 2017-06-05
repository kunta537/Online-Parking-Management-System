package ParkNPark.middletier;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ParkNPark.common.Logger;
import ParkNPark.interfaces.ClientManager;
import ParkNPark.interfaces.ClientManagerFactoryPOA;
import ParkNPark.interfaces.ClientManagerHelper;
import ParkNPark.interfaces.InvalidClientException;
import ParkNPark.interfaces.ServiceUnavailableException;

/**
 * Creates and manages ClientManager instances for clients
 */
public class ClientManagerFactoryImpl extends ClientManagerFactoryPOA
{
	/** Maximum number of attempts when trying to execute commends on database */
	protected static final int MAX_ATTEMPTS = 3;
	
	/** Our database manager instance */
	protected DatabaseManager databaseManager;
	
	/**
	 * Variables for real-time analysis
	 */
	
	/** Size of reply */
	protected int replySize;
	
	/** Logger instance */
	protected Logger logger;
	
	/** Our database poke prepared statement that is used during database pokes */
	protected PreparedStatement pokeStatement;

	/**
	 * Creates a new ClientManagerFactoryImpl instance
	 * @param logger The Logger to write performance data to
	 * @param replySize The size of the message replies to use
	 * @param jdbcURL The JDBC URL to use in the database connection
	 * @param jdbcUsername The username to use in the database connection
	 * @param jdbcPassword The password to use in the database connection
	 * @throws SQLException Thrown when a problem prevents a connection
	 * to the database
	 */
	public ClientManagerFactoryImpl(Logger logger, int replySize, String jdbcURL,
			String jdbcUsername, String jdbcPassword) throws SQLException {
		this.logger = logger;
		this.replySize = replySize;
		this.databaseManager = new DatabaseManager(jdbcURL, jdbcUsername, jdbcPassword);
		
		// Create a database connection for the poke and create
		// the prepared statement that will be used during pokes
		Connection pokeConnection = databaseManager.getConnection();
		pokeStatement = pokeConnection.prepareStatement("SELECT 1", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	}

	/**
     * Creates a new client manager instance for a new client
     * @throws ServiceUnavailableException Thrown if the database cannot be
     * contacted or if some other reason prevents the client manager from
     * being created successfully
     * @return A new client manager instance for a new client
     */
	public ClientManager getClientManager(String clientHostname) throws ServiceUnavailableException
	{
		ClientManagerImpl impl = new ClientManagerImpl(databaseManager, clientHostname, logger, replySize);
		try {
  		    return ClientManagerHelper.narrow(_poa().servant_to_reference(impl));
		} catch (Exception e) {
			System.err.println("Exception while trying to narrow a new ClientManagerImpl into a CORBA object: " + e.getClass().getName());
		    throw new ServiceUnavailableException("Could not attach new ClientManagerHelper to CORBA POA");
		}
	}

	/**
     * Returns the existing client manager instance for an existing client. A client can call
     * this method with its client ID and last sequence number on any server and will get its
     * client manager instance
     * @param clientID The ID of the client to get the ClientManager of
     * @throws InvalidClientException Thrown when the given client ID is not known to the system
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if
     * some other reason prevents the client manager from being retrieved successfully
     * @return The existing client manager instance for an existing client
     */
	public ClientManager getExistingClientManager(int clientID, String clientHostname)
	    throws ServiceUnavailableException, InvalidClientException
	{
		ClientManagerImpl impl = new ClientManagerImpl(databaseManager, clientID, clientHostname, logger, replySize);
		try {
  		    return ClientManagerHelper.narrow(_poa().servant_to_reference(impl));
		} catch (Exception e) {
			System.err.println("Exception while trying to narrow a new ClientManagerImpl into a CORBA object: " + e.getClass().getName());
		    throw new ServiceUnavailableException("Could not attach new ClientManagerHelper to CORBA POA");
		}
	}
	
	/**
     * Pokes the server to see if it is still alive and that it can still
     * communicate with the database
     * @throws ServiceUnavailableException Thrown when the server's database
     * connection is not working
     */
	public void poke() throws ServiceUnavailableException {
		
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{
			try
			{
				// Attempt to run the poke query
				pokeStatement.execute();
				
				// Get the result set and immediately close it
				pokeStatement.getResultSet().close();
				return;
			}
			catch (SQLException e)
			{	numTries++;
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("ClientManagerFactoryImpl-poke(): Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		throw new ServiceUnavailableException("Could not create connection.");
	}

	/**
     * Causes the server's database connection to become "hosed," meaning
     * that it will fail to work after this method is called. This is used
     * primarily for fault injection
     */
	public void hoseDatabaseConnection() {
		databaseManager.hoseConnection();
		System.out.println("Database connection hosed");
	}
	
	/**
     * Flushes the server's log files
     */
	public void flushLogs()
	{
		// Flush the logs to disk
		try {
			logger.flush();
		} catch (IOException e) {
			System.err.println("IOException in Logger.flush(): " + e.getMessage());
			System.err.println("Recent log data might be permanently lost");
		}
		System.out.println("Logs flushed to disk.");
	}
	
	/**
     * Kills the server
     */
	public void killServer()
	{
		// Flush the logs to disk first
		flushLogs();
		
		// Close the logger
		try {
			logger.close();
		} catch (IOException e) {
			System.err.println("IOException in Logger.close(): " + e.getMessage());
			System.err.println("Recent log data might be permanently lost");
		}
		
		// Kill the server
		System.out.println("Server exited.");
		System.exit(0);
	}
	
	/**
     * Kills the server (we don't have graceful shutdowns in this implementation)
     */
    public void exitServer() {
    	killServer();
    }
}
