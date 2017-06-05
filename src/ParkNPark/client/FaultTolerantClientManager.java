package ParkNPark.client;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;

import ParkNPark.common.InputEater;
import ParkNPark.common.LogEntry;
import ParkNPark.common.Logger;
import ParkNPark.common.NameService;
import ParkNPark.common.Server;
import ParkNPark.interfaces.AlreadyInLotException;
import ParkNPark.interfaces.AtBottomLevelException;
import ParkNPark.interfaces.AtTopLevelException;
import ParkNPark.interfaces.ClientManager;
import ParkNPark.interfaces.ClientManagerFactory;
import ParkNPark.interfaces.ClientManagerOperations;
import ParkNPark.interfaces.InvalidClientException;
import ParkNPark.interfaces.LotFullException;
import ParkNPark.interfaces.LotNotFoundException;
import ParkNPark.interfaces.NotInLotException;
import ParkNPark.interfaces.NotOnExitLevelException;
import ParkNPark.interfaces.PaddedInteger;
import ParkNPark.interfaces.PaddedIntegerSeq;
import ParkNPark.interfaces.PaddedVoid;
import ParkNPark.interfaces.ServiceUnavailableException;
import ParkNPark.interfaces._ClientManagerStub;

/**
 * Fault tolerant client manager that wraps a remote ClientManager and rolls
 * over to a new primary server in the presence of failures.
 * Note that this class is not multi-thread safe. Multiple threads should
 * synchronize access or wrap this class with a thread-safe delegate.
 */
public class FaultTolerantClientManager implements ClientManagerOperations
{
	/** The primary client manager */
	protected ClientManager primaryClientManager = null;
	
	/** The last-known client manager factory instance */
	protected ClientManagerFactory factory = null;
	
	/** When true, we are presently processing a fault */
	protected boolean fault = false;
	
	/** When true, the system is officially down due to no registration of a primary server */
	protected boolean systemDown = false;
	
	/** A logEntry instance to log to when fault is true */
	protected LogEntry faultyCallLogEntry = null;
	
	/** Lock for primaryClientManager, factory, faultyCallLogEntry, systemDown, and fault */
	protected Object primaryClientManagerLock = new Object();

	/** Our background thread that handles name service interactions, fault
	 *  detections, and fault recoveries */
	protected BackgroundThread backgroundThread;
	
	/** The latest sequence number from the client */
	protected int seq;
	
	/** The server-generated client ID */
	protected int clientID;

	/**
	 * Variables for real-time analysis
	 */
	
	/** Time between requests */
	private int interRequestTime;
	
	/** Our Logger instance */
	protected Logger logger;
	
	/** When true, never give up when trying to contact the server */
	protected boolean neverGiveUp;
	
	/** The wait timeout to use during fault recoveries */
	protected int recoveryTimeout;
	
	/** Original System.err object instance */
	protected PrintStream err;
	
	/**
	 * Creates a new fault-tolerant client manager that automatically fails over
	 * to new primary servers on method call failures
	 * @param orb The object request broker instance to use
	 * @param logger The Logger instance to write performance data to
	 * @param interRequestTime The time to wait between method invocations
	 * @param neverGiveUp When true, the client will never give up its attempt to
	 * contact the server, even if no primary is registered
	 * @param detectionTimeout The wait timeout to use during fault detections
	 * @param recoveryTimeout The wait timeout to use during fault recoveries
	 * @param The System.err instance to use
	 * @throws ServiceUnavailableException Thrown when a primary server could not be
	 * located or contacted
	 */
	public FaultTolerantClientManager(ORB orb, Logger logger, int interRequestTime,
			boolean neverGiveUp, int detectionTimeout, int recoveryTimeout,
			PrintStream err) throws ServiceUnavailableException {
		this.logger = logger;
		this.interRequestTime = interRequestTime;
		this.neverGiveUp = neverGiveUp;
		this.recoveryTimeout = recoveryTimeout;
		this.err = err;
		
		// Create the background thread, start it, and wait for it to finish its startup
		backgroundThread = new BackgroundThread(orb, detectionTimeout);
		synchronized(primaryClientManagerLock) {
			backgroundThread.start();
			try {
				primaryClientManagerLock.wait();
			} catch (InterruptedException e) { }
		}
		
		// If we don't have a primary server, then throw an exception now
		if (primaryClientManager == null)
			throw new ServiceUnavailableException("Could not connect to any server");
	}
	
	/**
	 * Return value or an exception along with the return value or the exception
	 * object. The exceptionThrown flag indicates whether this is an exception
	 * or a normal return
	 */
	protected class ReturnValueOrException
	{
		/** The return value or exception object */
		public Object retVal = null;
		
		/** When true, this instance is an exception; otherwise, this is a normal return value */
		public boolean exceptionThrown = false;
		
		/**
		 * Creates a new ReturnValueOrException using the given return value or exception object
		 * and whether or not that object is a return value or an exception
		 * @param retVal The return value or exception object
		 * @param exceptionThrown When true, retVal is an exception; when false, it's a return value
		 */
		public ReturnValueOrException(Object retVal, boolean exceptionThrown) {
			this.retVal = retVal;
			this.exceptionThrown = exceptionThrown;
		}
	}
	
	/**
	 * Calls the given method on the server with the given arguments and returns
	 * a ReturnValueOrException object to indicate a normal or exceptional result
	 * and contain the return value or the exception object. If server communication
	 * or service unavailability exceptions arise, this method will automatically
	 * fail over to the new primary after that becomes available. Server communication
	 * and service unavailability exceptions are only returned if no primary
	 * server is registered
	 * @param method The method to call on our client manager on the primary server
	 * @param logAndWait When true, this method call will be logged and, after returning
	 * from that call, this method will wait for the inter-request time before returning
	 * @param arguments The arguments to pass to the given method's parameters
	 * @return A ReturnValueOrException indicating a normal or exceptional result
	 */
	protected ReturnValueOrException callServer(Method method, boolean logAndWait, Object... arguments)	    
	{
		// Log the call if we are supposed to
		LogEntry logEntry = null;
		if (logAndWait) {
			try {
			    logEntry = logger.beginLogEntry(method.getName());
			} catch (ServiceUnavailableException e) {
				err.println("Log is closed during server method call; most likely a program bug; exiting");
				err.flush();
				System.exit(1);
				return null;
			}
		}
		
		// Try until the method works or is hopeless
		int attempts = 0;
		Object retVal;
		boolean faultLogged = false;
		while (true)
		{
			// If we logged a fault, then consider this the point of fault recovery,
			// just before we call the method. If this fails, then this value will
			// keep getting replaced until the method call is able to reach a functional server
			if (logAndWait && faultLogged)
				logEntry.setProbeFaultRecovery();
			
			// Get the primary client manager
			ClientManager clientManager;
			if (logAndWait)
			    logEntry.setProbeWaitIn();
			synchronized(primaryClientManagerLock) {
				if (logAndWait)
				    logEntry.setProbeWaitOut();
				clientManager = primaryClientManager;
			}

			// Attempt to call the method
			try {
				retVal = method.invoke(clientManager, arguments);
			    if (logAndWait) {
			    	try {
				        logger.endLogEntry(logEntry);
			    	} catch (ServiceUnavailableException e) { } // Should be unreachable
				    interRequestWait();
			    }
			    return new ReturnValueOrException(retVal, false);
			} catch (InvocationTargetException e)
			{
			    // This is an exception
				retVal = e.getCause();

				// Check for COMM_FAILURE, OBJECT_NOT_EXIST, and ServiceUnavailable exceptions
				if (retVal instanceof COMM_FAILURE || retVal instanceof OBJECT_NOT_EXIST ||
					retVal instanceof ServiceUnavailableException)
				{
					// Log the start of this fault if this is our first fault
					if (logAndWait && !faultLogged) {
						logEntry.setProbeFaultDetection();
						faultLogged = true;
					}
					
					// Notify the user
					if (retVal instanceof ServiceUnavailableException)
						System.out.println("Server is not operating properly; getting another server...");
					else if (retVal instanceof COMM_FAILURE)
						System.out.println("Problem while communicating with the server; getting another server...");
					else if (retVal instanceof OBJECT_NOT_EXIST)
						System.out.println("Problem while communicating with the server; getting another server...");
					
					// If this is our second or subsequent attempt, then wait for the timeout period first
					// (the replication manager needs time to see that the primary server has
					// database communication problems, too)
					if (attempts >= 2 && recoveryTimeout > 0) {
						if (logAndWait)
							logEntry.setProbeWaitIn();
						try {
							Thread.sleep(recoveryTimeout);
						} catch (InterruptedException f) { }
						if (logAndWait)
							logEntry.setProbeWaitOut();
					}

					// We need a new server! Bail out if we cannot get one and we're supposed
					// to give up
					if (!connectToAnyServer(clientManager, logEntry) && !neverGiveUp)
					{
						// If the system is down due to no primary server, then proceed
						boolean systemDown;
						if (logAndWait)
						    logEntry.setProbeWaitIn();
						synchronized(primaryClientManagerLock) {
							if (logAndWait)
							    logEntry.setProbeWaitOut();
							systemDown = this.systemDown;
						}
						if (systemDown)
						{
							// We could not get a primary server; inform the client
							System.out.println("No servers are up");
						    if (logAndWait) {
						    	try {
							        logger.endLogEntry(logEntry);
						    	} catch (ServiceUnavailableException f) { } // Should be unreachable
							    interRequestWait();
						    }
							return new ReturnValueOrException(retVal, true);
						} else
							attempts++;
					} else {
						attempts++;
					}
				}

				// Otherwise, normal exception was thrown by this method invocation; give that to our caller
				else {
				    if (logAndWait) {
				    	try {
					        logger.endLogEntry(logEntry);
				    	} catch (ServiceUnavailableException f) { } // Should be unreachable
					    interRequestWait();
				    }
					return new ReturnValueOrException(retVal, true);
				}
			} catch (IllegalAccessException e)
			{
			    // A reflection API exception occurred; throw this as a RuntimeException to the caller
				// (this thread does not have sufficient access rights to use this part of the
				// reflection API)
				System.out.println("Client is not permitted to use the Java Reflection API, which is required for normal operation; exiting");
			    if (logAndWait) {
			    	try {
				        logger.endLogEntry(logEntry);
			    	} catch (ServiceUnavailableException f) { } // Should be unreachable
				    interRequestWait();
			    }
				throw new RuntimeException(e);
			}
		}
	}

	protected static final Method getClientID;
	protected static final Method enterLot;
	protected static final Method exitLot;
	protected static final Method getOtherLotAvailability;
	protected static final Method getLots;
	protected static final Method moveUpLevel;
	protected static final Method moveDownLevel;
	protected static final Method getCurrentLevel;
	protected static final Method getMaxLevel;
	protected static final Method getMinLevel;
	protected static final Method closeClientManager;

	/** Initialize our cached Method instances */
	static {
		try {
		   Class noParameters[] = new Class[0];
		   Class oneIntParameter[] = new Class[] { int.class };
		   Class twoIntParameters[] = new Class[] { int.class, int.class };
		   getClientID = _ClientManagerStub.class.getMethod("getClientID", noParameters);
		   enterLot = _ClientManagerStub.class.getMethod("enterLot", twoIntParameters);
		   exitLot = _ClientManagerStub.class.getMethod("exitLot", oneIntParameter);
		   getOtherLotAvailability = _ClientManagerStub.class.getMethod("getOtherLotAvailability", oneIntParameter);
		   getLots = _ClientManagerStub.class.getMethod("getLots", noParameters);
		   moveUpLevel = _ClientManagerStub.class.getMethod("moveUpLevel", oneIntParameter);
		   moveDownLevel = _ClientManagerStub.class.getMethod("moveDownLevel", oneIntParameter);
		   getCurrentLevel = _ClientManagerStub.class.getMethod("getCurrentLevel", noParameters);
		   getMaxLevel = _ClientManagerStub.class.getMethod("getMaxLevel", noParameters);
		   getMinLevel = _ClientManagerStub.class.getMethod("getMinLevel", noParameters);
		   closeClientManager = _ClientManagerStub.class.getMethod("closeClientManager", noParameters);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
	
	/** Message for unexpected exceptions that arise during remote method calls (non-IDL and non-CORBA exceptions) */
	protected final static String unexpectedException = "Unexpected exception thrown";
	
	/**
     * Returns the client's ID, which can be used in the client manager factory's
     * getExistingClientManager() method
     * @return The client's ID
     * @throws ServiceUnavailableException Thrown when the log is closed and the
     * server is shutting down
     */
	public PaddedInteger getClientID() throws ServiceUnavailableException {
		ReturnValueOrException retVal = callServer(getClientID, true);
		if (retVal.exceptionThrown) {
			if (retVal.retVal instanceof RuntimeException)
				throw (RuntimeException) retVal.retVal;
			else
			    throw new RuntimeException(unexpectedException, (Throwable) retVal.retVal);
		} else
			return (PaddedInteger) retVal.retVal;
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
     * [Category: Baseline; Requirements: 1, 2, and 3]
     */
	public PaddedIntegerSeq enterLot(int seq, int lot) throws AlreadyInLotException, LotNotFoundException, LotFullException, ServiceUnavailableException {
		this.seq = seq;
		ReturnValueOrException retVal = callServer(enterLot, true, seq, lot);
		if (retVal.exceptionThrown) {
			if (retVal.retVal instanceof AlreadyInLotException)
				throw (AlreadyInLotException) retVal.retVal;
			else if (retVal.retVal instanceof LotNotFoundException)
				throw (LotNotFoundException) retVal.retVal;
			else if (retVal.retVal instanceof LotFullException)
				throw (LotFullException) retVal.retVal;
			else if (retVal.retVal instanceof ServiceUnavailableException)
				throw (ServiceUnavailableException) retVal.retVal;
			else if (retVal.retVal instanceof RuntimeException)
				throw (RuntimeException) retVal.retVal;
			else
			    throw new RuntimeException(unexpectedException, (Throwable) retVal.retVal);
		} else
		    return (PaddedIntegerSeq) retVal.retVal;
	}

	/**
     * Removes the client's car from the lot that it is currently in
     * @param seq The latest sequence number of the client
     * @throws NotInLotException Thrown if the car is not in a lot
     * @throws NotOnExitLevelException Thrown if the car is in a lot but is not on a permitted
     * exit level
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the car from exiting the lot
     * [Category: Baseline; Requirement: 7]
     */
	public PaddedVoid exitLot(int seq) throws NotInLotException, NotOnExitLevelException, ServiceUnavailableException {
		this.seq = seq;
		ReturnValueOrException retVal = callServer(exitLot, true, seq);
		if (retVal.exceptionThrown) {
			if (retVal.retVal instanceof NotInLotException)
				throw (NotInLotException) retVal.retVal;
			else if (retVal.retVal instanceof NotOnExitLevelException)
				throw (NotOnExitLevelException) retVal.retVal;
			else if (retVal.retVal instanceof ServiceUnavailableException)
				throw (ServiceUnavailableException) retVal.retVal;
			else if (retVal.retVal instanceof RuntimeException)
				throw (RuntimeException) retVal.retVal;
			else
			    throw new RuntimeException(unexpectedException, (Throwable) retVal.retVal);
		} else
			return (PaddedVoid) retVal.retVal;
	}

	/**
     * Returns an array of other lots that have availability, sorted by lot distance such that
     * closer lots are listed first
     * @param lot The lot to get lot distances from
     * @throws LotNotFoundException Thrown if the given lot number is not known to the system
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if
     * some other reason prevents the system from discovering the availability of other lots
     * @return An array of other lots that have availability
     * [Category: Baseline; Requirement: 4]
     */
	public PaddedIntegerSeq getOtherLotAvailability(int lot) throws LotNotFoundException, ServiceUnavailableException {
		ReturnValueOrException retVal = callServer(getOtherLotAvailability, true, lot);
		if (retVal.exceptionThrown) {
			if (retVal.retVal instanceof LotNotFoundException)
				throw (LotNotFoundException) retVal.retVal;
			else if (retVal.retVal instanceof ServiceUnavailableException)
				throw (ServiceUnavailableException) retVal.retVal;
			else if (retVal.retVal instanceof RuntimeException)
				throw (RuntimeException) retVal.retVal;
			else
			    throw new RuntimeException(unexpectedException, (Throwable) retVal.retVal);
		} else
		    return (PaddedIntegerSeq) retVal.retVal;
	}

	/**
     * Returns an array of valid lot numbers in the system, sorted by the lot number in ascending order
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the system from discovering the its defined lots
     * @return An array of valid lot numbers in the system
     * [Category: Baseline; Requirement: 12]
     */
	public PaddedIntegerSeq getLots() throws ServiceUnavailableException {
		ReturnValueOrException retVal = callServer(getLots, true);
		if (retVal.exceptionThrown) {
			if (retVal.retVal instanceof ServiceUnavailableException)
				throw (ServiceUnavailableException) retVal.retVal;
			else if (retVal.retVal instanceof RuntimeException)
				throw (RuntimeException) retVal.retVal;
			else
			    throw new RuntimeException(unexpectedException, (Throwable) retVal.retVal);
		} else
		    return (PaddedIntegerSeq) retVal.retVal; 
	}

	/**
     * Moves the car from its present level to the level above it
     * @param seq The latest sequence number of the client
     * @throws NotInLotException Thrown if the car is not in a lot
     * @throws AtTopLevelException Thrown if the car is already on the highest level
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the car from moving to the next highest level
     * @return The level number that the client's car is now on
     * [Category: Baseline; Requirement: 6]
     */
	public PaddedInteger moveUpLevel(int seq) throws NotInLotException, AtTopLevelException, ServiceUnavailableException {
		this.seq = seq;
		ReturnValueOrException retVal = callServer(moveUpLevel, true, seq);
		if (retVal.exceptionThrown) {
			if (retVal.retVal instanceof NotInLotException)
				throw (NotInLotException) retVal.retVal;
			else if (retVal.retVal instanceof AtTopLevelException)
				throw (AtTopLevelException) retVal.retVal;
			else if (retVal.retVal instanceof ServiceUnavailableException)
				throw (ServiceUnavailableException) retVal.retVal;
			else if (retVal.retVal instanceof RuntimeException)
				throw (RuntimeException) retVal.retVal;
			else
			    throw new RuntimeException(unexpectedException, (Throwable) retVal.retVal);
		} else
		    return (PaddedInteger) retVal.retVal;
	}

	/**
     * Moves the car from its present level to the level beneath it
     * @param seq The latest sequence number of the client
     * @throws NotInLotException Thrown if the car is not in a lot
     * @throws AtTopLevelException Thrown if the car is already on the lowest level
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some other reason prevents the car from moving to the lower level
     * @return The level number that the client's car is now on
     * [Category: Baseline; Requirement: 6]
     */
	public PaddedInteger moveDownLevel(int seq) throws NotInLotException, AtBottomLevelException, ServiceUnavailableException {
		this.seq = seq;
		ReturnValueOrException retVal = callServer(moveDownLevel, true, seq);
		if (retVal.exceptionThrown) {
			if (retVal.retVal instanceof NotInLotException)
				throw (NotInLotException) retVal.retVal;
			else if (retVal.retVal instanceof AtBottomLevelException)
				throw (AtBottomLevelException) retVal.retVal;
			else if (retVal.retVal instanceof ServiceUnavailableException)
				throw (ServiceUnavailableException) retVal.retVal;
			else if (retVal.retVal instanceof RuntimeException)
				throw (RuntimeException) retVal.retVal;
			else
			    throw new RuntimeException(unexpectedException, (Throwable) retVal.retVal);
		} else
		    return (PaddedInteger) retVal.retVal;
	}

	/**
     * Returns the car's current level number
     * @throws NotInLotException Thrown if the car is not in a lot
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the system from returning the car's current level
     * @return The car's current level number
     * [Category: Baseline; Requirement: 6]
     */
	public PaddedInteger getCurrentLevel() throws NotInLotException, ServiceUnavailableException {
		ReturnValueOrException retVal = callServer(getCurrentLevel, true);
		if (retVal.exceptionThrown) {
			if (retVal.retVal instanceof NotInLotException)
				throw (NotInLotException) retVal.retVal;
			else if (retVal.retVal instanceof ServiceUnavailableException)
				throw (ServiceUnavailableException) retVal.retVal;
			else if (retVal.retVal instanceof RuntimeException)
				throw (RuntimeException) retVal.retVal;
			else
			    throw new RuntimeException(unexpectedException, (Throwable) retVal.retVal);
		} else
		    return (PaddedInteger) retVal.retVal;
	}

	/**
     * Returns the top level number of the car's current parking lot
     * @throws NotInLotException Thrown if the car is not in a lot
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the system from returning the current lot's highest level
     * @return The top level number of the car's current parking lot
     * [Category: Baseline; Requirement: 6]
     */
	public PaddedInteger getMaxLevel() throws NotInLotException, ServiceUnavailableException {
		ReturnValueOrException retVal = callServer(getMaxLevel, true);
		if (retVal.exceptionThrown) {
			if (retVal.retVal instanceof NotInLotException)
				throw (NotInLotException) retVal.retVal;
			else if (retVal.retVal instanceof ServiceUnavailableException)
				throw (ServiceUnavailableException) retVal.retVal;
			else if (retVal.retVal instanceof RuntimeException)
				throw (RuntimeException) retVal.retVal;
			else
			    throw new RuntimeException(unexpectedException, (Throwable) retVal.retVal);
		} else
		    return (PaddedInteger) retVal.retVal;
	}

	/**
     * Returns the bottom level number of the car's current parking lot
     * @throws NotInLotException Thrown if the car is not in a lot
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the system from returning the current lot's lowest level
     * @return The bottom level number of the car's current parking lot
     * [Category: Baseline; Requirement: 6]
     */
	public PaddedInteger getMinLevel() throws NotInLotException, ServiceUnavailableException {
		ReturnValueOrException retVal = callServer(getMinLevel, true);
		if (retVal.exceptionThrown) {
			if (retVal.retVal instanceof NotInLotException)
				throw (NotInLotException) retVal.retVal;
			else if (retVal.retVal instanceof ServiceUnavailableException)
				throw (ServiceUnavailableException) retVal.retVal;
			else if (retVal.retVal instanceof RuntimeException)
				throw (RuntimeException) retVal.retVal;
			else
			    throw new RuntimeException(unexpectedException, (Throwable) retVal.retVal);
		} else
		    return (PaddedInteger) retVal.retVal;
	}

	/**
     * Closes the client manager and frees server resources associated with it, including the client
     * manager's activation in the server's CORBA portable object adapter. AFter calling
     * this method, you should not use this FaultTolerantClientManager instance again
     * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
     * other reason prevents the system from closing the client manager. The client manager remains
     * open if this exception gets thrown
     */
	public void closeClientManager() throws ServiceUnavailableException
	{
		// Close down the remote client manager
		ReturnValueOrException retVal = callServer(closeClientManager, false);
		if (retVal.exceptionThrown) {
			if (retVal.retVal instanceof ServiceUnavailableException)
				throw (ServiceUnavailableException) retVal.retVal;
			else if (retVal.retVal instanceof RuntimeException)
				throw (RuntimeException) retVal.retVal;
			else
			    throw new RuntimeException(unexpectedException, (Throwable) retVal.retVal);
		}
		
		// Close down the background thread
		backgroundThread.beginExit();
	}
	
	/** Shared static instance of the input eater output stream wrapped in a PrintStream */
	protected static PrintStream inputEater = new PrintStream(new InputEater());
	
	/**
	 * Wait for the configured inter-request wait period or, if none is configured,
	 * return immediately. Note that interrupting the calling Thread will cause
	 * this method to return immediately after being interrupted
	 */
	protected void interRequestWait()
	{
		// Attempt to wait for the inter-request wait time
		try {
			Thread.sleep(interRequestTime);
		} catch (InterruptedException e) {
		}
	}
	
	/**
	 * Hoses the server's database connection. This is primarily
	 * used for fault injection
	 */
	public void hoseServerDatabaseConnection() {
		synchronized(primaryClientManagerLock) {
			try {
			    factory.hoseDatabaseConnection();
			} catch (Exception e) {
				err.println("Cannot hose the server's database connection: " + e.getClass().getName());
				err.flush();
			}
		}
	}

	/**
	 * Connects to any registered server, which is chosen randomly
	 * @param faultyClientManager The ClientManager that was deemed to be faulty, or null
	 * if no ClientManager is faulty
	 * @param logEntry The LogEntry object to write performance data
	 * to when not null
	 * @return True when a connection with a server was successfully
	 * established or false if a server connection could not be
	 * established
	 */
	protected boolean connectToAnyServer(ClientManager faultyClientManager, LogEntry logEntry)
	{
		// Tell the background thread to recover our fault and wait until
		// it is complete
		if (logEntry != null)
		    logEntry.setProbeWaitIn();
		synchronized(primaryClientManagerLock) {
			if (logEntry != null)
			    logEntry.setProbeWaitOut();

			// If the new primary server is different from the faulty server,
			// then we can return immediately because the background
			// thread took care of the recovery for us
			if (faultyClientManager != null && !faultyClientManager._is_equivalent(primaryClientManager))
				return true;
			
			// Set the faulty call log entry and notify the background thread
			// that it needs to get a new primary server
			faultyCallLogEntry = logEntry;
			fault = true;
			primaryClientManagerLock.notifyAll();
			
			// Wait until the background thread completed its primary server retrieval
			try {
			    primaryClientManagerLock.wait();
			} catch (InterruptedException e) { }
			
			// Remove the reference to the LogEntry
			faultyCallLogEntry = null;
			
			// Get the return value of the background thread's call to connectToAnyServer()
			boolean retVal = !fault;
			
			// We are no longer in falut mode
			fault = false;
			
			// Return the return value
			return retVal;
		}
	}

	/**
	 * Background thread that performs the following actions in a loop continuously:
	 * <ol><li>Recover from a fault on the primary server if one was detected by
	 * either this thread or by the main thread</li>
	 * <li>Refresh the list of servers from the naming service</li>
	 * <li>Poke each server serially and, upon fault detection, remove that server
	 * from the list of servers. If that server was the primary server, then enter
	 * the fault recovery mode</li>
	 * <li>If the current primary server is no longer registered in the naming service,
	 * then enter the fault recovery mode</li>
	 * <li>Wait for the fault detection timeout time if no faults were detected in the
	 * current loop iteration</li>
	 * This permits the server list to remain up-to-date, relevant, and accurate for
	 * quick failover.
	 *
	 * When a fault is detected, the affected server's entry is not used for at least
	 * five times the fault detection timeout to give the replication manager ample
	 * time to unregister its name if it really is down. This match is performed via
	 * IOR so that a faulty server that gets restarted can be used quickly after it
	 * becomes available
	 */
	protected class BackgroundThread extends Thread
	{
		/** Our name server instance */
		protected NameService nameService;
		
		/** The index of the current server in the list of servers */
		protected int serverIndex = 0;

		/** The fault detection timeout */
		protected int detectionTimeout;
		
		/** True when we should exit or false when we should not */
		protected boolean exit = false;
		
		/** Map of fault detection times to their faulty servers */
		protected SortedMap<Long, Server> faultyServers = new TreeMap<Long, Server>();
		
		/**
		 * Creates a new background thread instance
		 * @param orb The ORB to operate with
		 * @param detectionTimeout The fault detection timeout to use
		 */
		public BackgroundThread(ORB orb, int detectionTimeout) {
			this.detectionTimeout = detectionTimeout;

			// Create the naming service object
			nameService = new NameService(orb, err);
		}
		
		/**
		 * Tells this thread to gracefully shut down
		 */
		public void beginExit() {
			exit = true;
			synchronized(primaryClientManagerLock) {
				primaryClientManagerLock.notifyAll();
			}
		}
		
		/**
		 * Background thread's execution
		 */
		@Override
		public void run()
		{
			// Get our initial connection and, if we could not get a server, then give up now
			try {
			    if (!initialConnection())
			    	return;
			} finally
			{
				// Notify the main thread so that it can continue
				synchronized(primaryClientManagerLock) {
					primaryClientManagerLock.notifyAll();
				}
			}
			
			// Keep running until we are no longer needed
			while (!exit)
			{
				// Lock the primary client manager
				synchronized(primaryClientManagerLock)
				{
					// If we have a fault, then try to recover now
					if (fault)
					{
						// Call connectToAnyServer() in this thread and then
						// notify the caller when complete
						if (connectToAnyServer(faultyCallLogEntry))
							fault = false;
						primaryClientManagerLock.notifyAll();
					}

					// Refresh our server list if we did not handle a fault above
					else if (!fault) {
						nameService.getServers().clear();
						if (nameService.addServerListFromParkNParkContext(false, false, null)) {
							systemDown = !nameService.isPrimaryServerRegistered();
						    processFaultyServers();
						}
					}
				}
				
				// Check the known servers by poking them
				Iterator<Server> serverIterator = nameService.getServers().iterator();
				boolean backupServerFault = false, primaryServerFound = false;
				while (serverIterator.hasNext())
				{
					// Stop processing if the main thread needs us to handle fault recovery
					// immediately
					synchronized(primaryClientManagerLock) {
						if (fault)
							break;
					}
					
					// Poke this server
					Server server = serverIterator.next();
					try {
						server.clientManagerFactory.poke();
						
						// If this server is the primary server, then note that
						synchronized(primaryClientManagerLock) {
							if (server.clientManagerFactory._is_equivalent(factory))
								primaryServerFound = true;
						}
					} catch (Exception e)
					{
						// Server is not working, so remove it from the list after adding it
						// to our set of faulty servers
						faultyServers.put(new Long(System.currentTimeMillis()), server);
						serverIterator.remove();
						
						// If this was the primary server, then we need a new
						// primary server
						synchronized(primaryClientManagerLock) {
						    if (server.clientManagerFactory._is_equivalent(factory))
						    	fault = true;
						    else
						    	backupServerFault = true;
						}
					}
				}
				
				// Lock the primary client manager
				synchronized(primaryClientManagerLock)
				{
					// If we could not find our primary server and the list had at
					// least one server in it, then enter the fault mode to force
					// a new primary server selection
					if (!fault && !primaryServerFound && nameService.getServers().size() > 0)
						fault = true;
					
				    // Wait for either our detection timeout or until we're needed
					try {
						if (!fault && !backupServerFault)
					        primaryClientManagerLock.wait(detectionTimeout);
					} catch (InterruptedException e) { }
				}
			}
		}
		
		/**
		 * Maintains the faulty server list and removes them from the current
		 * set of servers. Faulty servers that have been faulty for more than
		 * detectionTimeout * 5 milliseconds are removed from the faulty list
		 */
		protected void processFaultyServers()
		{
			// Remove all faulty server entries that are older than
			// detectionTimeout * 5 milliseconds
			faultyServers.headMap(new Long(System.currentTimeMillis() - detectionTimeout * 5)).clear();
			
			// Loop through each faulty server and, for each one, see if it's
			// equivalent to any registered server
			Iterator<Server> faultyServerIterator = faultyServers.values().iterator(), serverIterator;
			Server faultyServer, server;
			while (faultyServerIterator.hasNext()) {
				faultyServer = faultyServerIterator.next();
				
				// Loop through each registered server to see if this server matches
				// any of them
				serverIterator = nameService.getServers().iterator();
				while (serverIterator.hasNext()) {
					server = serverIterator.next();
					
					// If this is the same as the faulty server, then remove this server
					// from the active server set
					if (faultyServer.clientManagerFactory._is_equivalent(server.clientManagerFactory))
						serverIterator.remove();
				}
			}
		}
		
		/**
		 * Create the initial connection to a server and return
		 * whether or not it worked
		 * @return True if the initial connection exists or false
		 * if it does not exist
		 */
		protected boolean initialConnection()
		{
			// Connect to any random active server
			int attempts = 0;
			boolean serverListObtained = false;
			do
			{
				// Attempt to to connect to any server
				serverListObtained = connectToAnyServer(null);		    
				attempts++;
			    
			    // If this is our second or subsequent attempt, wait for the timeout period
			    if (attempts >= 2 && recoveryTimeout > 0) {
			    	try {
			    		Thread.sleep(recoveryTimeout);
			    	} catch (InterruptedException e) { }
			    }
			} while (neverGiveUp && !serverListObtained);
			return serverListObtained;
		}
		
		/**
		 * Finds the primary server in the List of servers and returns whether or not one
		 * was found
		 * @return True if the primary server was found or false if it was not
		 */
		protected boolean findPrimary()
		{
			// If we have no primary, set the primary to the last server plus one
			// to indicate that
			if (primaryClientManager == null) {
				serverIndex = nameService.getServers().size();
				return false;
			}
			
	        // Find out which server is primary, if any
	        boolean primaryFound = false;
			List<Server> servers = nameService.getServers();
			serverIndex = 0;
			while (!primaryFound && serverIndex < servers.size()) {
				if (servers.get(serverIndex).clientManagerFactory._is_equivalent(primaryClientManager))
					primaryFound = true;
				else
					serverIndex++;
			}
			return primaryFound;
		}

		/**
		 * Connects to any registered server, which is chosen randomly
		 * @param logEntry The LogEntry object to write performance data
		 * to when not null
		 * @return True when a connection with a server was successfully
		 * established or false if a server connection could not be
		 * established
		 */
		protected boolean connectToAnyServer(LogEntry logEntry) {
			int attempts = 0;
			boolean debugMessages = false;
			
			// Get our current host name
			String hostName;
	        try {
	        	hostName = InetAddress.getLocalHost().getHostName();
	        } catch (UnknownHostException e) {
	           	System.out.println("Could not get the system's host name, which is required for proper operation; exiting");
	           	return false;
	        }
	        
	        // Find out which server is primary, if any
	        findPrimary();
			
			// Try until we succeed or give up
			List<Server> servers = nameService.getServers();
			while (true)
			{
				// Remove the index of our old server, since it doesn't work
				// (but if we have no servers, then this is likely our initial call)
				if (servers.size() > serverIndex) {
					faultyServers.put(new Long(System.currentTimeMillis()), servers.get(serverIndex));
				    servers.remove(serverIndex);
				}
				
				// If our server list is empty, then refresh it now
				if (servers.size() == 0) {
					attempts++;
					if (!nameService.addServerListFromParkNParkContext(false, false, logEntry))
						return false;
					systemDown = !nameService.isPrimaryServerRegistered();

					// Remove faulty servers from the retrieved list
					processFaultyServers();
				}

				// If we have at least one server, then proceed
				if (nameService.getServers().size() > 0)
				{
					// Choose a random server from our selection unless if we have only one
					if (servers.size() == 1)
						factory = servers.get(serverIndex = 0).clientManagerFactory;
					else
					    factory = servers.get(serverIndex = (int) Math.floor(Math.random() * servers.size())).clientManagerFactory;

					// Get our client manager object
					ClientManager newClientManager = null;
					try {
						// If we already had a primary client manager, then ask for our existing client manager; otherwise,
						// create a new instance
						if (debugMessages) System.out.println("Obtaining our client manager");
						if (primaryClientManager == null) {
						    newClientManager = factory.getClientManager(hostName);
						} else
						{
							// Log this entry point into the client manager
							if (logEntry != null)
								logEntry.setProbeClientManagerIn();
							
							// Get our existing client manager
							newClientManager = factory.getExistingClientManager(clientID, hostName);
							
							// We are finished with the client manager call
							if (logEntry != null)
								logEntry.setProbeClientManagerOut();
						}
						if (debugMessages) System.out.println("Client manager obtained");
					} catch (ServiceUnavailableException e) {
						if (logEntry != null)
							logEntry.setProbeClientManagerOut();
						System.out.println("Server is not functioning properly; trying another server");
					} catch (InvalidClientException e) {
						if (logEntry != null)
							logEntry.setProbeClientManagerOut();
						
						// Our client's state no longer is in the system!
						System.out.println("Server does not remember us; we cannot continue");
						return false;
					} catch (COMM_FAILURE e) {
						if (logEntry != null)
							logEntry.setProbeClientManagerOut();
						
						// Communication exception while communicating with the server; try to get the primary again
						System.out.println("Communication problem while communicating with the server; trying another server...");
					} catch (OBJECT_NOT_EXIST e) {
						if (logEntry != null)
							logEntry.setProbeClientManagerOut();
						System.out.println("Communication problem while communicating with the server; trying another server...");
					} catch (SystemException e) {
						if(logEntry != null)
							logEntry.setProbeClientManagerOut();
						System.out.println("Unhandled CORBA exception while communicating with the client manager factory: " + e.getClass().getName());
						return false;
					} catch (Exception e) {
						if (logEntry != null)
							logEntry.setProbeClientManagerOut();
						System.out.println("Miscellaneous exception (possibly a program bug?): " + e.getClass().getName());
						return false;
					}
					
					// Use this client manager if we are supposed to
					if (newClientManager != null)
					{
						try {
							// Log this entry point into the client manager
							if(logEntry != null)
								logEntry.setProbeClientManagerIn();

							// Get the client ID from the server
							int serverClientID = newClientManager.getClientID().value;
							
							// We are finished with the client manager call
							if(logEntry != null)
								logEntry.setProbeClientManagerOut();

							// Set the new client manager synchronously
							System.out.println("Server successfully obtained");
							synchronized (primaryClientManagerLock) {
								primaryClientManager = newClientManager;
								clientID = serverClientID;
							}
							
							// We're done :-)
							return true;
						} catch (ServiceUnavailableException e) {
							if(logEntry != null)
								logEntry.setProbeClientManagerOut();
							System.out.println("Server is being shut down; trying another server");
						} catch (COMM_FAILURE e) {
							// Communication exception while communicating with the server; try to get the primary again
							if(logEntry != null)
								logEntry.setProbeClientManagerOut();
							System.out.println("Communication problem while communicating with the server; trying another server...");
						} catch (OBJECT_NOT_EXIST e) {
							if(logEntry != null)
								logEntry.setProbeClientManagerOut();
							System.out.println("Object does not exist on the server; trying to get the primary again: " + e.getClass().getName());
						} catch (SystemException e) {
							if(logEntry != null)
								logEntry.setProbeClientManagerOut();
							System.out.println("Unhandled CORBA exception while communicating with the client manager factory: " + e.getClass().getName());
							return false;
						} catch (Exception e) {
							if(logEntry != null)
								logEntry.setProbeClientManagerOut();
							System.out.println("Miscellaneous exception (possibly a program bug?): " + e.getClass().getName());
							return false;
						}
					}
				}
				
				// Otherwise, because we have no registered servers, give up
				else
					return false;

				// If this is our second or subsequent set of servers, then wait for the timeout period first
				if (attempts >= 2 && recoveryTimeout > 0) {
					if (logEntry != null)
						logEntry.setProbeWaitIn();
					try {
						System.out.println("Waiting for a server to become available...");
						Thread.sleep(recoveryTimeout);
					} catch (InterruptedException f) { }
					if (logEntry != null)
						logEntry.setProbeWaitOut();
				}
			}
		}
	}
}
