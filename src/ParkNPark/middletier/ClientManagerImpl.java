package ParkNPark.middletier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import ParkNPark.common.LogEntry;
import ParkNPark.common.Logger;
import ParkNPark.interfaces.AlreadyInLotException;
import ParkNPark.interfaces.AtBottomLevelException;
import ParkNPark.interfaces.AtTopLevelException;
import ParkNPark.interfaces.ClientManagerPOA;
import ParkNPark.interfaces.InvalidClientException;
import ParkNPark.interfaces.LotFullException;
import ParkNPark.interfaces.LotNotFoundException;
import ParkNPark.interfaces.NotInLotException;
import ParkNPark.interfaces.NotOnExitLevelException;
import ParkNPark.interfaces.PaddedInteger;
import ParkNPark.interfaces.PaddedIntegerSeq;
import ParkNPark.interfaces.PaddedVoid;
import ParkNPark.interfaces.ServiceUnavailableException;

/**
 * Server object that will manage communication between the client
 * and the database.
 */
public class ClientManagerImpl extends ClientManagerPOA
{
	/** Maximum number of attempts when trying to execute commends on database */
	protected static final int MAX_ATTEMPTS = 3;
	
	/** Size of an intger variable */
	protected static int INTEGER_SIZE = 4;
	
	/** Byte array used to pad integer variables */
	protected byte[] integerPadding;
	
	/** PaddedVoid instance for padded void returns */
	protected PaddedVoid paddedVoid;
	
	/** Flags used to print out the status of lots and levels */
	protected static final boolean PRINT_LOTS_STATUS = false;
	protected static final boolean PRINT_LEVELS_STATUS = false;
	
	/** Our database manager instance */
	protected DatabaseManager databaseManager;
	
	/** Client's ID used for fault-tolerance */
	protected int clientID;
	
	/** Client's current operation sequence number used for fault-tolerance */
	protected int seq;
	
	/** Lot that client is in */
	protected int lot;
	
	/** Level that client is in */
	protected int level;
	
	/** Connection to the database */
	protected Connection conn;
	
	/** Statements used for duplicate message detection */
	protected PreparedStatement checkDuplicate;
	protected PreparedStatement updateSequence;
	
	/**
	 * Variables for real-time analysis
	 */
	
	/** Client's hostname */
	protected String clientHostname;
	
	/** Size of replies */
	protected int replySize;
	
	/** Our Logger instance */
	protected Logger logger;

	/**
	 * Create a client manager for an existing client that will hold
	 * information specific to the client.
	 * @param databaseManager The database manager to use in this client manager
	 * @param clientID The ID of the client to restore this ClientManager instance from
	 * @param clientHostname The hostname of the client
	 * @param logger The Logger object to log performance data to
	 * @param replySize The size of the method return values to use for the current test
	 * @throws ServiceUnavailableException Thrown when a database exception prevented the client
	 * manager from being created successfully
	 * @throws InvalidClientException Thrown when the given client ID does not exist in
	 * the database
	 */
	public ClientManagerImpl(DatabaseManager databaseManager, int clientID, String clientHostname,
			                 Logger logger, int replySize) 
			                throws ServiceUnavailableException, InvalidClientException
	{
		//LogEntry logEntry = logger.beginLogEntry("ClientManagerImpl", clientHostname);
		
		// Setup database connection
		this.databaseManager = databaseManager;
		setupDatabaseConn();
		
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{	try
			{	// Check if valid client id
				PreparedStatement ps = conn.prepareStatement("SELECT Seq, LotID, Level FROM Client WHERE ClientID = ?");
				ps.setInt(1, clientID);
				//logEntry.setProbeDbIn();
				ResultSet rs = null;
				try
				{	rs = ps.executeQuery();
					//logEntry.setProbeDbOut();
				}
				catch (SQLException e)
				{	//logEntry.setProbeDbOut();
					e.printStackTrace();
				}
				
				if (rs.next())
				{	// Store client's id and sequence number
					this.clientID = clientID;
					this.seq = rs.getInt("Seq");
					this.lot = rs.getInt("LotID");
					if (rs.wasNull())
						this.lot = -1;
					this.level = rs.getInt("Level");
					if (rs.wasNull())
						this.level = -1;
					rs.close();
					
					// Set up the real-time probes
					setupProbes(clientHostname, logger, replySize);
					//logger.endLogEntry(logEntry);
					return;
				}
				else
				{	//logger.endLogEntry(logEntry);
					throw new InvalidClientException();
				}
			}
			catch (SQLException e)
			{	try
				{	conn.rollback();
					numTries++;
				}
				catch (SQLException se)
				{	//logger.endLogEntry(logEntry);
					throw new ServiceUnavailableException("Could not rollback when creating existing client.");
				}
			}
		}
	
		// If max attempts exceeded, throw exception
		System.err.println("ClientManagerImpl-158: Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		//logger.endLogEntry(logEntry);
		throw new ServiceUnavailableException("Could not create existing client.");
	}
	
	/**
	 * Create a client manager for a new client that will hold 
	 * information specific to the client.
	 * @param databaseManager The database manager to use in this client manager
	 * @param clientHostname The hostname of the client
	 * @param logger The Logger object to log performance data to
	 * @param replySize The size of the method return values to use for the current test
	 * @throws ServiceUnavailableException Thrown when a database exception prevented the client
	 * manager from being created successfully
	 */
	public ClientManagerImpl(DatabaseManager databaseManager, String clientHostname, Logger logger, int replySize) 
			                throws ServiceUnavailableException
	{
		// Setup database connection
		this.databaseManager = databaseManager;
		setupDatabaseConn();
		
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{	try
			{	// Create a new client id
				Statement stmt = conn.createStatement();
				stmt.executeUpdate("INSERT INTO Client (Seq, LotID, Level) VALUES(0, null, null)");
				ResultSet rs = stmt.executeQuery("SELECT last_insert_id() ClientID");
				if (rs.next())
				{	this.clientID = rs.getInt("ClientID");
					this.seq = 0;
					this.lot = -1;
					this.level = -1;
					conn.commit();
					
					// Set up the real-time probes
					setupProbes(clientHostname, logger, replySize);
					return;
				}
				else
				{	numTries++;
				}
			}
			catch (SQLException e)
			{	try
				{	conn.rollback();
					numTries++;
				}
				catch (SQLException se)
				{	throw new ServiceUnavailableException("Could not rollback when creating "
						+ "new client id");
				}
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("ClientManagerImpl-215: Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		throw new ServiceUnavailableException("Could not create new client id.");
	}
	
	/**
	 * Set up the database connection
	 * @throws ServiceUnavailableException Thrown if a problem came up while connecting to the database server
	 * or setting it up after connecting
	 */
	protected void setupDatabaseConn() throws ServiceUnavailableException
	{
		// Retry on failed attempts
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{	try
			{
				// Connect to the database
				conn = databaseManager.getConnection();
				
				// Turn off auto-commit, enabling transaction control
				conn.setAutoCommit(false);
				
				// Set up prepared statements
				this.checkDuplicate = conn.prepareStatement("SELECT Seq FROM Client WHERE ClientID = ? AND Seq >= ?");
				this.updateSequence = conn.prepareStatement(
						"UPDATE Client SET Seq = ?, LotID = ?, Level = ? WHERE ClientID = ?");
				
				return;
			} 
			catch (SQLException e) 
			{	numTries++;
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("ClientManagerImpl-250: Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		throw new ServiceUnavailableException("Could not create connection.");
	}
	
	/**
	 * Setup probes for real-time analysis
	 * @param clientHostname
	 * @param numClients
	 * @param interRequestTime
	 * @param replySize
	 * @param serverHostname
	 */
	protected void setupProbes(String clientHostname, Logger logger, int replySize)
	{
		// Keep track of real-time analysis variables
		this.clientHostname = clientHostname;
		this.replySize = replySize;
		this.logger = logger;
		
		// Create byte array for integer padding
		if (this.replySize > INTEGER_SIZE)
		    this.integerPadding = new byte[this.replySize - INTEGER_SIZE];
		else
			this.integerPadding = new byte[0];
		
		// Create the PaddedVoid instance for void padding
		this.paddedVoid = new PaddedVoid(new byte[this.replySize]);
	}

	/**
     * Closes the client manager and frees server resources associated with it, including the client
     * manager's activation in the server's CORBA portable object adapter
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the system from closing the client manager. The client manager remains
     * open if this exception gets thrown
     */
	public void closeClientManager() throws ServiceUnavailableException 
	{
		// No need to close connection if it's already closed
		if (conn == null)
			return;
		
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{	try
			{	conn.close();
				conn = null;
				
				// Stop serving this object now that it is closed
				try 
				{	_poa().deactivate_object(_poa().servant_to_id(this));
				} 
				catch (Exception e) 
				{	System.err.println("CORBA exception while closing the client manager: " + e.getClass().getName());
				}
				return;
			}
			catch(SQLException e)
			{	numTries++;
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		throw new ServiceUnavailableException("Could not close database connection.");
	}

    /**
     * Moves the client's car into the lot with the given lot number and returns an array of level
     * numbers that have available spaces
     * @param seq The latest sequence number of the client
     * @param lot The lot number to enter
     * @throws AlreadyInLotException Thrown when the client's car is already in a lot
     * @throws LotNotFoundException Thrown if the given lot number is not known to the system
     * @throws LotFullException Thrown if the given lot is full
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the car from entering the lot
     * @return An array of level numbers that have available spaces
     */
	public PaddedIntegerSeq enterLot(int seq, int lot) throws AlreadyInLotException, LotNotFoundException, LotFullException, 
		ServiceUnavailableException 
	{
		LogEntry logEntry = logger.beginLogEntry("enterLot", clientHostname);
		printLotsStatus("Before entering lot", seq);
		
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{
			try
			{	// Check if lot is valid
				PreparedStatement lotInfo = conn.prepareStatement("SELECT CarCount, Capacity FROM Lot WHERE ID = ?");
				lotInfo.setInt(1, lot);
				ResultSet rs = lotInfo.executeQuery();
				
				if (rs.next())
				{	// Check if request already completed
					if (alreadyCompleted(seq))
					{	System.out.println("Request already completed (in enter lot)");
					}
					else
					{	// If the client is already in a lot, then throw an AlreadyInLotException now
						if (this.lot != -1) {
							logger.endLogEntry(logEntry);
							throw new AlreadyInLotException(this.lot);
						}
					
						// Check if lot is full
						int carCount = rs.getInt("CarCount");
						int capacity = rs.getInt("Capacity");
						rs.close();
					
						if (carCount < capacity)
						{
							// Increment the lot's car count
							PreparedStatement enterLot = conn.prepareStatement(
								"UPDATE Lot SET CarCount = CarCount + 1 WHERE ID = ?");
							enterLot.setInt(1, lot);
							if (enterLot.executeUpdate() != 1) {
								conn.rollback();
								logger.endLogEntry(logEntry);
								throw new ServiceUnavailableException("Update on Lot table failed unexpectedly");
							}
	
							// Increment the entry level's car count
							PreparedStatement enterLevel = conn.prepareStatement(
								"UPDATE Level SET CarCount = CarCount + 1 WHERE LotID = ? AND Level = ?");
							enterLevel.setInt(1, lot);
							enterLevel.setInt(2, 1);
							if (enterLevel.executeUpdate() != 1) {
								conn.rollback();
								logger.endLogEntry(logEntry);
								throw new ServiceUnavailableException("Could not add car to entry level; perhaps the entry level does not exist?");
							}
						}
						else
						{
							logger.endLogEntry(logEntry);
							throw new LotFullException();
						}
					}
						
					// Get list of available levels
					PreparedStatement availableLevels = conn.prepareStatement(
						"SELECT Level FROM Level WHERE LotID = ? AND CarCount < Capacity ORDER BY Level");
					availableLevels.setInt(1, lot);
					rs = availableLevels.executeQuery();
					
					rs.last();
					int[] availLevels = new int[rs.getRow()];
					int i = 0;
					rs.beforeFirst();
					while (rs.next())
						availLevels[i++] = rs.getInt("Level");
					rs.close();
					
					// Update client data and commit the changes
					updateClientData(seq, lot, 1);
					conn.commit();
					
					// With the changes committed, update our cached values
					this.seq = seq;
					this.lot = lot;
					this.level = 1;
					
					printLotsStatus("After entering lot", seq);
					printLevelsStatus("After entering lot", seq);
					
					logger.endLogEntry(logEntry);
					return padIntegerArray(availLevels);
				}
				else
				{
					logger.endLogEntry(logEntry);
					throw new LotNotFoundException();
				}
			}
			catch (SQLException e)
			{	try
				{	conn.rollback();
					numTries++;
				}
				catch (SQLException se)
				{
					logger.endLogEntry(logEntry);
					throw new ServiceUnavailableException("Could not rollback when entering lot.");
				}
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		logger.endLogEntry(logEntry);
		throw new ServiceUnavailableException("Could not enter lot.");
	}

    /**
     * Removes the client's car from the lot that it is currently in
     * @param seq The latest sequence number of the client
     * @throws NotInLotException Thrown if the car is not in a lot
     * @throws NotOnExitLevelException Thrown if the car is in a lot but is not on a permitted
     * exit level
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the car from exiting the lot
     */
	public PaddedVoid exitLot(int seq) throws NotInLotException, NotOnExitLevelException, ServiceUnavailableException
	{
		LogEntry logEntry = logger.beginLogEntry("exitLot", clientHostname);
		printLotsStatus("Before exiting lot", seq);
		printLevelsStatus("Before exiting lot", seq);
		
		// Check if request already completed
		if (alreadyCompleted(seq))
		{	System.out.println("Request already completed (in exit lot)");
			printLotsStatus("After exiting lot", seq);
			printLevelsStatus("After exiting lot", seq);
			logger.endLogEntry(logEntry);
			return padVoid();
		}
		
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{
			try
			{   // If we are not in a lot, then throw an exception
				if (lot == -1) {
					logger.endLogEntry(logEntry);
					throw new NotInLotException();
				}
				
				// If we are not on an exit level, then throw an exception
				if (level != 1) {
					logger.endLogEntry(logEntry);
					throw new NotOnExitLevelException(lot, level);
				}
				
				// Get car count of lot
				PreparedStatement lotCarCount = conn.prepareStatement("SELECT CarCount FROM Lot WHERE ID = ?");
				lotCarCount.setInt(1, this.lot);
				ResultSet rs = lotCarCount.executeQuery();
				rs.next();
				int count = rs.getInt("CarCount");
				rs.close();
				
				// If lot car count is not zero, decrement count
				if (count > 0)
				{	PreparedStatement exitLot = conn.prepareStatement(
						"UPDATE Lot SET CarCount = CarCount - 1 WHERE ID = ?");
					exitLot.setInt(1, lot);
					exitLot.execute();
				}
				
				// Get car count of level
				PreparedStatement levelCarCount = conn.prepareStatement("SELECT CarCount FROM Level WHERE LotID = ? AND Level = ?");
				levelCarCount.setInt(1, lot);
				levelCarCount.setInt(2, 1);
				rs = levelCarCount.executeQuery();
				rs.next();
				count = rs.getInt("CarCount");
				rs.close();
				
				// If level car count is not zero, decrement count
				if (count > 0)
				{	PreparedStatement exitLevel = conn.prepareStatement(
						"UPDATE Level SET CarCount = CarCount - 1 WHERE LotID = ? AND Level = ?");
					exitLevel.setInt(1, lot);
					exitLevel.setInt(2, 1);
					exitLevel.execute();
				}
				
				printLotsStatus("After exiting lot", seq);
				printLevelsStatus("After exiting lot", seq);
				
				// Update client data and commit the changes
				updateClientData(seq, -1, -1);
				conn.commit();
				
				// With the changes committed, update our cached values
				this.seq = seq;
				this.lot = -1;
				this.level = -1;
				
				logger.endLogEntry(logEntry);
				return padVoid();
			}
			catch (SQLException e)
			{	try
				{	conn.rollback();
					numTries++;
				}
				catch (SQLException se)
				{
					logger.endLogEntry(logEntry);
					throw new ServiceUnavailableException("Could not rollback when exiting lock.");
				}
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		logger.endLogEntry(logEntry);
		throw new ServiceUnavailableException("Could not exit lot.");	
	}

	/**
     * Returns the client's ID, which can be used in the client manager factory's
     * getExistingClientManager() method
     * @return The client's ID
     * @throws ServiceUnavailableException Thrown when the log is closed and the
     * server is shutting down
     */
	public PaddedInteger getClientID() throws ServiceUnavailableException
	{
		// Return the client ID
	    LogEntry logEntry = logger.beginLogEntry("getClientID", clientHostname);
	    logger.endLogEntry(logEntry);
	    return padInteger(this.clientID);
	}

	/**
     * Returns the car's current level number
     * @throws NotInLotException Thrown if the car is not in a lot
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the system from returning the car's current level
     * @return The car's current level number
     */
	public PaddedInteger getCurrentLevel() throws NotInLotException, ServiceUnavailableException
	{
		LogEntry logEntry = logger.beginLogEntry("getCurrentLevel", clientHostname);
		
		// If we are not in a lot, then throw an exception
		if (lot == -1) {
			logger.endLogEntry(logEntry);
			throw new NotInLotException();
		}
		
		// Return the current level
		logger.endLogEntry(logEntry);
		return padInteger(this.level);
	}

	/**
     * Returns an array of valid lot numbers in the system, sorted by the lot number in ascending order
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the system from discovering the its defined lots
     * @return An array of valid lot numbers in the system
     */
	public PaddedIntegerSeq getLots() throws ServiceUnavailableException
	{
		LogEntry logEntry = logger.beginLogEntry("getLots", clientHostname);
		printLotsStatus("When getting lots", seq);
		
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{
			try
			{	// Get list of available lots
				PreparedStatement availableLots = conn.prepareStatement(
					"SELECT ID FROM Lot ORDER BY ID");
				logEntry.setProbeDbIn();
				ResultSet rs = availableLots.executeQuery();
				
				rs.last();				
				int[] lots = new int[rs.getRow()];
				rs.beforeFirst();
				
				int i = 0;
				while (rs.next())
					lots[i++] = rs.getInt("ID");
				rs.close();
				
				conn.commit();
				
				logEntry.setProbeDbOut();
				
				logger.endLogEntry(logEntry);
				return padIntegerArray(lots);
			}
			catch (SQLException e)
			{	try
				{	conn.rollback();
					numTries++;
				}
				catch (SQLException se)
				{
					logger.endLogEntry(logEntry);
					throw new ServiceUnavailableException("Could not rollback when getting lots.");
				}
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		logger.endLogEntry(logEntry);
		throw new ServiceUnavailableException("Could not get lots.");	
	}

	/**
     * Returns the top level number of the car's current parking lot
     * @throws NotInLotException Thrown if the car is not in a lot
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the system from returning the current lot's highest level
     * @return The top level number of the car's current parking lot
     */
	public PaddedInteger getMaxLevel() throws NotInLotException, ServiceUnavailableException 
	{
		LogEntry logEntry = logger.beginLogEntry("getMaxLevel", clientHostname);
		
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{
			try
			{	// If we are not in a lot, then throw an exception
				if (lot == -1) {
					logger.endLogEntry(logEntry);
					throw new NotInLotException();
				}

				PreparedStatement maxLevelStmt = conn.prepareStatement("SELECT MAX(Level) FROM Level WHERE LotID = ?");
				maxLevelStmt.setInt(1, this.lot);
				ResultSet rs = maxLevelStmt.executeQuery();
				rs.next();
				int maxLevel = rs.getInt(1);
				rs.close();
				
				logger.endLogEntry(logEntry);
				return padInteger(maxLevel);
			}
			catch (SQLException e)
			{	numTries++;
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		logger.endLogEntry(logEntry);
		throw new ServiceUnavailableException("Could not get max level.");
	}

	/**
     * Returns the bottom level number of the car's current parking lot
     * @throws NotInLotException Thrown if the car is not in a lot
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the system from returning the current lot's lowest level
     * @return The bottom level number of the car's current parking lot
     */
	public PaddedInteger getMinLevel() throws NotInLotException, ServiceUnavailableException 
	{
		LogEntry logEntry = logger.beginLogEntry("getMinLevel", clientHostname);
		
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{
			try
			{	// If we are not in a lot, then throw an exception
				if (lot == -1) {
					logger.endLogEntry(logEntry);
					throw new NotInLotException();
				}
				
				PreparedStatement minLevelStmt = conn.prepareStatement("SELECT MIN(Level) FROM Level WHERE LotID = ?");
				minLevelStmt.setInt(1, this.lot);
				ResultSet rs = minLevelStmt.executeQuery();
				rs.next();
				int minLevel = rs.getInt(1);
				rs.close();				
				logger.endLogEntry(logEntry);
				return padInteger(minLevel);
			}
			catch (SQLException e)
			{	numTries++;
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		logger.endLogEntry(logEntry);
		throw new ServiceUnavailableException("Could not get min level.");
	}

	/**
     * Returns an array of other lots that have availability, sorted by lot distance such that
     * closer lots are listed first
     * @param lot The lot to get lot distances from
     * @throws LotNotFoundException Thrown if the given lot number is not known to the system
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if
     * some other reason prevents the system from discovering the availability of other lots
     * @return An array of other lots that have availability
     */
	public PaddedIntegerSeq getOtherLotAvailability(int lot) throws LotNotFoundException, ServiceUnavailableException 
	{
		LogEntry logEntry = logger.beginLogEntry("getOtherLotAvailability", clientHostname);
		printLotsStatus("When getting other lot availability", seq);
		
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{
			try
			{	PreparedStatement validLot = conn.prepareStatement("SELECT 'X' FROM Lot WHERE ID = ?");
				validLot.setInt(1, lot);
				ResultSet rs = validLot.executeQuery();
				
				if (!rs.next()) {
					rs.close();
					conn.rollback();
					logger.endLogEntry(logEntry);
					throw new LotNotFoundException(lot);
				}
				rs.close();
				
				PreparedStatement otherLots = conn.prepareStatement("SELECT ToLotID FROM LotDistance D "
						+ "INNER JOIN Lot L ON L.ID=D.ToLotID WHERE FromLotID = ? AND CarCount < Capacity "
						+ "ORDER BY Distance");
				otherLots.setInt(1, lot);
				rs = otherLots.executeQuery();
				
				rs.last();
				int[] otherAvailableLots = new int[rs.getRow()];
				rs.beforeFirst();
				
				int i = 0;
				while (rs.next())
					otherAvailableLots[i++] = rs.getInt("ToLotID");
				rs.close();
				
				logger.endLogEntry(logEntry);
				return padIntegerArray(otherAvailableLots);
			}
			catch (SQLException e)
			{	numTries++;
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		logger.endLogEntry(logEntry);
		throw new ServiceUnavailableException("Could not get other lot availability.");
	}

	/**
     * Moves the car from its present level to the level beneath it
     * @param seq The latest sequence number of the client
     * @throws NotInLotException Thrown if the car is not in a lot
     * @throws AtTopLevelException Thrown if the car is already on the lowest level
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some other reason prevents the car from moving to the lower level
     * @return The level number that the client's car is now on
     */
	public PaddedInteger moveDownLevel(int seq) throws NotInLotException, AtBottomLevelException, ServiceUnavailableException
	{
		LogEntry logEntry = logger.beginLogEntry("moveDownLevel", clientHostname);
		printLevelsStatus("Before moving down one level", seq);
		
		// Check if request already completed
		if (alreadyCompleted(seq))
		{	System.out.println("Request already completed (in move down level)");
			printLevelsStatus("After moving down one level", seq);
			logger.endLogEntry(logEntry);
			return padInteger(level);
		}
		
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{
			try
			{	// If we are not in a lot, then throw an exception
				if (lot == -1) {
					logger.endLogEntry(logEntry);
					throw new NotInLotException();
				}

				// If we are on the bottom level, then throw an exception
				if (level == 1) {
					logger.endLogEntry(logEntry);
					throw new AtBottomLevelException(this.lot, this.level);
				}
				
				// Get car count of level
				PreparedStatement levelCarCount = conn.prepareStatement("SELECT CarCount FROM Level WHERE LotID=? AND Level=?");
				levelCarCount.setInt(1, lot);
				levelCarCount.setInt(2, level);
				ResultSet rs = levelCarCount.executeQuery();
				rs.next();
				int count = rs.getInt("CarCount");
				rs.close();
				
				// If lot car count is not zero, decrement count
				if (count > 0)
				{	PreparedStatement exitLevel = conn.prepareStatement(
						"UPDATE Level SET CarCount = CarCount - 1 WHERE LotID=? AND Level=?");
				    exitLevel.setInt(1, lot);
				    exitLevel.setInt(2, level);
				    exitLevel.execute();
				}
				
				// Decrement the level, but don't update the cached value yet
				int currLevel = level;
				currLevel--;
				
				// Get car count of level
				levelCarCount.setInt(2, currLevel);
				rs = levelCarCount.executeQuery();
				rs.next();
				count = rs.getInt("CarCount");
				rs.close();
				
				// Increment the level count
				PreparedStatement enterLevel = conn.prepareStatement(
						"UPDATE Level SET CarCount = CarCount + 1 WHERE LotID=? AND Level=?");
				enterLevel.setInt(1, lot);
				enterLevel.setInt(2, currLevel);
				enterLevel.execute();
				
				printLevelsStatus("After moving down one level", seq);
				
				// Update client data and commit the transaction
				updateClientData(seq, this.lot, currLevel);
				conn.commit();
				
				// With changes committed, update our cached values
				this.seq = seq;
				level = currLevel;
				
				logger.endLogEntry(logEntry);
				return padInteger(level);
			}
			catch (SQLException e)
			{	try
				{	conn.rollback();
					numTries++;
				}
				catch (SQLException se)
				{
					logger.endLogEntry(logEntry);
					throw new ServiceUnavailableException("Could not rollback when moving down level.");
				}
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		logger.endLogEntry(logEntry);
		throw new ServiceUnavailableException("Could not move down level.");	
	}

	/**
     * Moves the car from its present level to the level above it
     * @param seq The latest sequence number of the client
     * @throws NotInLotException Thrown if the car is not in a lot
     * @throws AtTopLevelException Thrown if the car is already on the highest level
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the car from moving to the next highest level
     * @return The level number that the client's car is now on
     */
	public PaddedInteger moveUpLevel(int seq) throws NotInLotException, AtTopLevelException, ServiceUnavailableException
	{
		LogEntry logEntry = logger.beginLogEntry("moveUpLevel", clientHostname);
		printLevelsStatus("Before moving up one level", seq);
		
		// Check if request already completed
		if (alreadyCompleted(seq))
		{	System.out.println("Request already completed (in move up level)");
			printLevelsStatus("After moving up one level", seq);			
			logger.endLogEntry(logEntry);
			return padInteger(level);
		}
		
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{
			try
			{   
				// If we are not in a lot, then throw an exception
				if (lot == -1) {
					logger.endLogEntry(logEntry);
					throw new NotInLotException();
				}

				// If we are on the top level, then throw an exception
				PreparedStatement levelQuery = conn.prepareStatement("SELECT MAX(Level) HighestLevel FROM Level WHERE LotID=?");
				levelQuery.setInt(1, lot);
				ResultSet rs = levelQuery.executeQuery();
				rs.next();
				int maxLevel = rs.getInt("HighestLevel");
				if (level == maxLevel) {
					rs.close();
					conn.rollback();
					logger.endLogEntry(logEntry);
					throw new AtTopLevelException(this.lot, this.level);
				}
				rs.close();
				
				// Increment the level, but don't update the cached value yet
				int currLevel = level;
				currLevel++;
				
				// Increment the level count of the upper level
				PreparedStatement enterLevel = conn.prepareStatement(
						"UPDATE Level SET CarCount = CarCount + 1 WHERE LotID=? AND Level=?");
				enterLevel.setInt(1, lot);
				enterLevel.setInt(2, currLevel);
				enterLevel.execute();

				// Get car count of the lower level
				PreparedStatement levelCarCount = conn.prepareStatement("SELECT CarCount FROM Level WHERE LotID=? AND Level=?");
				levelCarCount.setInt(1, lot);
				levelCarCount.setInt(2, level);
				rs = levelCarCount.executeQuery();
				rs.next();
				int countLowerLevel = rs.getInt("CarCount");
				rs.close();
				
				// If lot car count of the lower level is not zero, decrement count
				if (countLowerLevel > 0)
				{	PreparedStatement exitLevel = conn.prepareStatement(
						"UPDATE Level SET CarCount = CarCount - 1 WHERE LotID=? AND Level=?");
				    exitLevel.setInt(1, lot);
				    exitLevel.setInt(2, level);
				    exitLevel.execute();
				}
				printLevelsStatus("After moving up one level", seq);
				
				// Update client data and commit the transaction
				updateClientData(seq, this.lot, currLevel);
				conn.commit();
				
				// With changes committed, update our cached values
				this.seq = seq;
				level = currLevel;
				
				logger.endLogEntry(logEntry);
				return padInteger(level);
			}
			catch (SQLException e)
			{	try
				{	conn.rollback();
					numTries++;
				}
				catch (SQLException se)
				{
					logger.endLogEntry(logEntry);
					throw new ServiceUnavailableException("Could not rollback when moving up level.");
				}
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		logger.endLogEntry(logEntry);
		throw new ServiceUnavailableException("Could not move up level.");	
	}
	
	/**
	 * Checks if the current client request has already been completed.
	 * @param seq
	 * @return
	 * @throws ServiceUnavailableException
	 */
	protected boolean alreadyCompleted(int seq) throws ServiceUnavailableException
	{
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{	try
			{	this.checkDuplicate.setInt(1, this.clientID);
				this.checkDuplicate.setInt(2, seq);
				ResultSet rs = this.checkDuplicate.executeQuery();
				this.checkDuplicate.clearParameters();
				
				// Check if new request
				boolean completed = rs.next();
				rs.close();
				return completed;
			}
			catch (SQLException e)
			{	numTries++;
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		throw new ServiceUnavailableException("Could not check if request was already completed.");
	}
	
	/**
	 * Updates the client table, which is used for duplicate detection.
	 * @param seq
	 * @param lot
	 * @param level
	 * @return
	 * @throws ServiceUnavailableException
	 */
	protected void updateClientData(int seq, int lot, int level) throws ServiceUnavailableException
	{
		int numTries = 0;
		while (numTries < MAX_ATTEMPTS)
		{	try
			{	this.updateSequence.setInt(1, seq);
				
				if (lot == -1)
					this.updateSequence.setNull(2, Types.INTEGER);
				else
					this.updateSequence.setInt(2, lot);
				
				if (level == -1)
					this.updateSequence.setNull(3, Types.INTEGER);
				else
					this.updateSequence.setInt(3, level);
				
				this.updateSequence.setInt(4, this.clientID);
				
				if (this.updateSequence.executeUpdate() != 1) 
				{	conn.rollback();
					throw new ServiceUnavailableException("Update on Client table failed unexpectedly");
				}
				
				this.updateSequence.clearParameters();
				return;
			}
			catch (SQLException e)
			{	numTries++;
			}
		}
		
		// If max attempts exceeded, throw exception
		System.err.println("Failed to execute commands on database, even after " + MAX_ATTEMPTS + " attempts.");
		throw new ServiceUnavailableException("Could not check if request was already completed.");
	}
	
	/**
	 * Print the status of all the lots
	 * @param message The message to display with the status
	 * @param seq The client request sequence number
	 */
	protected void printLotsStatus(String message, int seq)
	{
		if (PRINT_LOTS_STATUS)
		{	try
			{	PreparedStatement lotsStatus = conn.prepareStatement("SELECT ID, CarCount FROM Lot");
				ResultSet rs = lotsStatus.executeQuery();
				
				System.out.println("=== Lots Status === (" + message + ") for Client " + this.clientID + " Seq " + seq);
				while (rs.next())
					System.out.println("Lot: " + rs.getInt("ID") + "\tCarCount: " + rs.getInt("CarCount"));
				rs.close();
				System.out.println();
			}
			catch (SQLException e)
			{	System.err.println("Could not print lot status.");
			}
		}
	}
	
	/**
	 * Print the status of all the levels in the current lot
	 * @param message The message to display with the status
	 * @param seq The client request sequence number
	 */
	protected void printLevelsStatus(String message, int seq)
	{
		if (PRINT_LEVELS_STATUS)
		{	try
			{	PreparedStatement levelsStatus = conn.prepareStatement("SELECT Level, CarCount FROM Level WHERE LotID = ?");
				levelsStatus.setInt(1, this.lot);
				ResultSet rs = levelsStatus.executeQuery();
				
				System.out.println("=== Levels Status === (" + message + ") for Client " + this.clientID + " Seq " + seq);
				while (rs.next())
					System.out.println("Level: " + rs.getInt("Level") + "\tCarCount: " + rs.getInt("CarCount"));
				rs.close();
				System.out.println();
			}
			catch (SQLException e)
			{	System.err.println("Could not print levels status.");
			}
		}
	}
	
	/**
	 * Pad the integer array in order to make it comply with the reply size of the analysis
	 * @param value
	 * @return padded value
	 */
	protected PaddedIntegerSeq padIntegerArray(int[] value)
	{
		if (this.replySize > value.length * INTEGER_SIZE)
		    return new PaddedIntegerSeq(value, new byte[this.replySize - (value.length * INTEGER_SIZE)]);
		else
			return new PaddedIntegerSeq(value, new byte[0]);
	}
	
	/**
	 * Pad the integer in order to make it comply with the reply size of the analysis
	 * @param value
	 * @return padded value
	 */
	protected PaddedInteger padInteger(int value)
	{
		return new PaddedInteger(value, this.integerPadding);
	}
	
	/**
	 * Pad the void in order to make it comply with the reply size of the analysis
	 * @return padded void
	 */
	protected PaddedVoid padVoid() {
		return this.paddedVoid;
	}
}
