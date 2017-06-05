package ParkNPark.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ParkNPark.interfaces.ServiceUnavailableException;

/**
 * Logger class that, as quickly as possible, logs information into a
 * pre-allocated buffer and has facilities for manual and automatic flushing.
 */
public class Logger
{
	/** Our currently-active log */
	protected List<LogEntry> log;
	
	/** Consistent mutex for the currently-active log (because log can get replaced by a new
	 *  instance) */
	protected Object logMutex = new Object();
	
	/** The number of array elements to allocate when creating new Lists */
	protected int allocationSize;
	
	/** Sink for the times that method calls begin in */
	protected BufferedWriter probeIn;
	
	/** Sink for the times that method calls complete in */
	protected BufferedWriter probeOut;
	
	/** Method name sink */
	protected BufferedWriter probeMethod;
	
	/** Client name sink */
	protected BufferedWriter probeSource;
	
	/** Fault detection sink */
	protected BufferedWriter probeFaultDetection;
	
	/** Fault recovery sink */
	protected BufferedWriter probeFaultRecovery;
	
	/** Sink for the times that database access begins */
	protected BufferedWriter probeDbIn;
	
	/** Sink for the times that database access ends */
	protected BufferedWriter probeDbOut;
	
	/** Sink for the times that naming service access begins */
	protected BufferedWriter probeNameServiceIn;
	
	/** Sink for the times that naming service access ends */
	protected BufferedWriter probeNameServiceOut;
	
	/** Sink for the times that client manager access begins */
	protected BufferedWriter probeClientManagerIn;
	
	/** Sink for the times that client manager access ends */
	protected BufferedWriter probeClientManagerOut;
	
	/** Sink for the times that the waiting begins */
	protected BufferedWriter probeWaitIn;
	
	/** Sink for the times that the waiting stops */
	protected BufferedWriter probeWaitOut;

	/** Our temporary file path for logging */
	protected static File tempFilePath;
	
	/**
	 * Creates a new Logger instance that can be used to log activities and,
	 * in the background, flush existing log entries to disk
	 * @param numClients The number of clients in the current test
	 * @param numServers The number of servers in the current test
	 * @param requestsPerClient The number of requests expected from each client
	 * @param interRequestTime The delay between each invocation, which is used when
	 * constructing the log file names
	 * @param replySize The size of the server's replies, which is used when
	 * constructing the log file names
	 * @param tier The tier being logged: <code>srv</code> or <code>cli</code>
	 * @param hostname The host name of the machine being logged
	 * @param logServerInfo Log information relating to the server 
	 * @param logFaults Log the fault detection and fault recovery times, which primarily
	 * makes sense on a client
	 * @param appendLogs True when the log files should be appended or false if the
	 * log files should be overwritten
	 */
	public Logger(int numClients, int numServers, int requestsPerClient, int interRequestTime,
			      int replySize, String tier, String hostname, boolean logServerInfo,
			      boolean logClientInfo, boolean appendLogs)
	{
		// Store our array allocation size
		allocationSize = numClients * requestsPerClient;
		
		// Create our initial log
		log = new ArrayList<LogEntry>(allocationSize);
		
		// Create our log files
		try {
			probeIn = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "in", tier, hostname), appendLogs));
			probeOut = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "out", tier, hostname), appendLogs));
			probeMethod = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "msg", tier, hostname), appendLogs));
			if (logServerInfo)
			{	probeSource = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "source", tier, hostname), appendLogs));
				probeDbIn = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "dbIn", tier, hostname), appendLogs));
				probeDbOut = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "dbOut", tier, hostname), appendLogs));
			}
			else
			{	probeSource = null;
				probeDbIn = null;
				probeDbOut = null;
			}
			if (logClientInfo) {
				probeFaultDetection = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "fault_detection", tier, hostname), appendLogs));
				probeFaultRecovery = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "fault_recovery", tier, hostname), appendLogs));
				probeNameServiceIn = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "NameServiceIn", tier, hostname), appendLogs));
				probeNameServiceOut = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "NameServiceOut", tier, hostname), appendLogs));
				probeClientManagerIn = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "ClientManagerIn", tier, hostname), appendLogs));
				probeClientManagerOut = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "ClientManagerOut", tier, hostname), appendLogs));
				probeWaitIn = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "WaitIn", tier, hostname), appendLogs));
				probeWaitOut = new BufferedWriter(new FileWriter(getLogFile(numClients, numServers, interRequestTime, replySize, "WaitOut", tier, hostname), appendLogs)); 
			} else {
				probeFaultDetection = null;
				probeFaultRecovery = null;
				probeNameServiceIn = null;
				probeNameServiceOut = null;
				probeClientManagerIn = null;
				probeClientManagerOut = null;
				probeWaitIn = null;
				probeWaitOut = null;
			}
		}
		catch (IOException e) {
			System.err.println("Could not create probe log files; exiting");
			System.err.flush();
			throw new RuntimeException("Could not create probe log files");
		}
	}
	
	/**
	 * Creates a new LogEntry object instance, adds it to the log,
	 * and, just before returning it, sets its probeIn property to the
	 * current time in microseconds. Use this method only if you
	 * constructed this Logger instance with logSource set to false
	 * @param methodName The name of the method that was called
	 * @return A new LogEntry with the
	 * current time in microseconds into the probeIn property
	 * @throws ServiceUnavailableException Thrown if a log entry is attempted
	 * when the log has already been closed
	 */
	public LogEntry beginLogEntry(String methodName) throws ServiceUnavailableException {
		return beginLogEntry(methodName, null);
	}
	
	/**
	 * Creates a new LogEntry object instance, adds it to the log,
	 * and, just before returning it, sets its probeIn property to the
	 * current time in microseconds
	 * @param methodName The name of the method that was called
	 * @param clientName The hostname of the client that called the method
	 * @return A new LogEntry with the
	 * current time in microseconds into the probeIn property
	 * @throws ServiceUnavailableException Thrown if a log entry is attempted
	 * when the log has already been closed
	 */
	public LogEntry beginLogEntry(String methodName, String clientName) throws ServiceUnavailableException
	{
		// Throw an exception if the logger is not ready
		synchronized(this) {
			if (log == null)
				throw new ServiceUnavailableException("Logging is shut down; server is probably in the process of exiting");
		}
		
		// Create the new LogEntry instance
		LogEntry logEntry = new LogEntry();
		
		// Serialize access to the log
		boolean flush = false;
		do {
			synchronized(logMutex)
			{
				// If the log is full, warn the user and flush the logs first
				if (allocationSize <= log.size())
					flush = true;
				
				// Otherwise, add it to the log
				else {
				    log.add(logEntry);
				    flush = false;
				}

				// If we need to flush, then do that now
				if (flush) {
					System.err.println("Logger Warning: Log is full; flushing contents before beginning the new log entry");
				    System.err.flush();
					try {
					    flush();
					    System.err.println("Flush complete; resuming");
					    System.err.flush();
					} catch (IOException e) {
						System.err.println("IOException in Logger.flush(): " + e.getMessage());
						System.err.println("Recent log data might be permanently lost");
						System.err.flush();
					}
				}
			}
		} while (flush);

		// Set the method name and the client name
		logEntry.setProbeMethod(methodName);
		logEntry.setProbeSource(clientName);
		
		// Set the probeIn property and immediately return
		logEntry.setProbeIn();
		return logEntry;
	}
	
	/**
	 * Ends a LogEntry after setting the probeOut property to the current
	 * time in microseconds
	 * @param logEntry The LogEntry instance to finish
	 * @throws ServiceUnavailableException Thrown when the logger has been
	 * shut down and thus this log entry cannot be persisted
	 */
	public void endLogEntry(LogEntry logEntry) throws ServiceUnavailableException
	{
		// If the logger was shut down, then throw an exception now
		synchronized(this) {
			if (log == null)
				throw new ServiceUnavailableException("Logging is shut down; server is probably in the process of exiting");
		}

		// Set the probeOut property immediately
		logEntry.setProbeOut();
		
		// We are finished with this log entry
		logEntry.markLogEntryAsFinished();
	}
	
	/**
	 * Flush out all the log entries into the output files. Do not call this
	 * method if you have <code>logMutex</code> locked or else a deadlock might occur!
	 * @throws IOException Thrown if an IOException occurs during the flushing,
	 * such as out-of-disk-space exceptions
	 */
	public synchronized void flush() throws IOException
	{
		// Perform a synchronized replace of the log
		List<LogEntry> oldLog;
		synchronized(logMutex) {
			List<LogEntry> newLog = new ArrayList<LogEntry>(allocationSize);
			oldLog = log;
			log = newLog;
		}
		
		// Now that we have exclusive use of the old log as no one else has
		// a reference to it, iterate through its entire contents
		for (LogEntry entry : oldLog)
		{
			// If this log entry is not ready, then punt it back to the live log
			if (!entry.isLogEntryFinished()) {
				synchronized(logMutex) {
					log.add(entry);
				}
			}
			
			// Otherwise, write the contents of this log entry to the proper files
			else {
				probeIn.write(Long.toString(entry.getProbeIn(), 10));
				probeIn.newLine();
				probeOut.write(Long.toString(entry.getProbeOut(), 10));
				probeOut.newLine();
				probeMethod.write(entry.getProbeMethod());
				probeMethod.newLine();
				
				// If we are tracking client hostnames, then write that
				if (probeSource != null) {
				    probeSource.write(entry.getProbeSource());
					probeSource.newLine();
				}
				
				// If we are tracking database access, then write those
				if ((probeDbIn != null) && (probeDbOut != null))
				{	probeDbIn.write(Long.toString(entry.getProbeDbIn(), 10));
					probeDbIn.newLine();
					probeDbOut.write(Long.toString(entry.getProbeDbOut(), 10));
					probeDbOut.newLine();
				}
				
				// If we are tracking fault detections and recoveries, then write those
				if (probeFaultDetection != null) {
					probeFaultDetection.write(Long.toString(entry.getProbeFaultDetection(), 10));
					probeFaultDetection.newLine();
					probeFaultRecovery.write(Long.toString(entry.getProbeFaultRecovery(), 10));
					probeFaultRecovery.newLine();
				}
				
				// If we are tracking naming service access, then write those
				if ((probeNameServiceIn != null) && (probeNameServiceOut != null)) {
					probeNameServiceIn.write(Long.toString(entry.getProbeNameServerIn(), 10));
					probeNameServiceIn.newLine();
					probeNameServiceOut.write(Long.toString(entry.getProbeNameServerOut(), 10));
					probeNameServiceOut.newLine();
				}
					
				// If we are tracking client manager access, then write those
				if ((probeClientManagerIn != null) && (probeClientManagerOut != null)) {
					probeClientManagerIn.write(Long.toString(entry.getProbeClientManagerIn(), 10));
					probeClientManagerIn.newLine();
					probeClientManagerOut.write(Long.toString(entry.getProbeClientManagerOut(), 10));
					probeClientManagerOut.newLine();
				}
				
				// If we are tracking the wait times, then write those
				if ((probeWaitIn != null) && (probeWaitOut != null)) {
					probeWaitIn.write(Long.toString(entry.getProbeWaitIn(), 10));
					probeWaitIn.newLine();
					probeWaitOut.write(Long.toString(entry.getProbeWaitOut(), 10));
					probeWaitOut.newLine();
				}
			}
		}
		
		// Flush the log file buffers
		probeIn.flush();
		probeOut.flush();
		probeMethod.flush();
		if (probeSource != null)
			probeSource.flush();
		if ((probeDbIn != null) && (probeDbOut != null))
		{	probeDbIn.flush();
			probeDbOut.flush();
		}
		if (probeFaultDetection != null) {
			probeFaultDetection.flush();
			probeFaultRecovery.flush();
		}
		if (probeNameServiceIn != null) {
			probeNameServiceIn.flush();
			probeNameServiceOut.flush();
		}
		if (probeClientManagerIn != null) {
			probeClientManagerIn.flush();
			probeClientManagerOut.flush();
		}
		if (probeWaitIn != null) {
			probeWaitIn.flush();
			probeWaitOut.flush();
		}
	}
	
	/**
	 * Closes the log files. Note that you must not call flush() nor log any more
	 * data after calling this method. Attempts to do so will throw NullPointerExceptions
	 */
	public synchronized void close() throws IOException
	{
		// Perform a flush first
 	    flush();
		
		// Close our outputs
		probeIn.close();
		probeOut.close();
		probeMethod.close();
		if (probeSource != null)
			probeSource.close();
		if ((probeDbIn != null) && (probeDbOut != null))
		{	probeDbIn.close();
			probeDbOut.close();
		}
		if (probeFaultDetection != null) {
			probeFaultDetection.close();
			probeFaultRecovery.close();
		}
		if (probeNameServiceIn != null) {
			probeNameServiceIn.close();
			probeNameServiceOut.close();
		}
		if (probeClientManagerIn != null) {
			probeClientManagerIn.close();
			probeClientManagerOut.close();
		}
		if (probeWaitIn != null) {
			probeWaitIn.close();
			probeWaitOut.close();
		}
		
		// If we still have unfinished entries, then write out their method names to the console
		// so that the console user can see that bugs might exist in the code (or
		// the server is being closed in the middle of a client request)
		synchronized (logMutex) {
			if (log.size() > 0) {
				Set<String> methodNames = new HashSet<String>();
				for (LogEntry entry : log)
					methodNames.add(entry.getProbeMethod());
				
				// Notify the console user
				System.out.println("The following methods have one or more open log entries:");
				for (String methodName : methodNames) {
					System.out.print("  ");
					System.out.println(methodName);
				}
			}
		}

		// Lose the reference to our log
		log = null;
	}
	
	/**
	 * Generate the full name of the log file name according to the given parameters
	 * @param numClients The number of clients in the current test
	 * @param numServers The number of servers in the current test
	 * @param interRequestTime The delay between each invocation
	 * @param replySize The size of the server's replies
	 * @param probeType The type of probe: <code>in</code>, <code>out</code>,
	 * <code>msg</code>, or <code>source</code>
	 * @param machine The machine being logged: <code>srv</code> or <code>cli</code>
	 * @param hostname The host name of the machine being logged
	 * @return A File object containing the full path to the log file
	 */
	protected File getLogFile(int numClients, int numServers, int interRequestTime, int replySize, String probeType, String machine, String hostname)
	{
		// Construct the relative name
		String name = "DATA749_app_" + probeType + "_" + machine + "_WARM_PASSIVE_" + numServers + "srv_" + numClients
			+ "cli_" + interRequestTime + "us_" + replySize + "req_" + hostname + "_team6.txt";
		
		// Return our File object based on our temporary file path
		return new File(getTempFilePath(), name);
	}
	
	/**
	 * Returns the temporary file path to store logging files into
	 * @return The temporary file path to store logging files into
	 */
	protected static File getTempFilePath()
	{
		// If we already have a temporary file path, then return that
		if (tempFilePath != null)
			return tempFilePath;
		
		// Get the temporary file path
		try {
		    tempFilePath = File.createTempFile("meow", "cats");

			// We don't care about the temp file; we just want its path
            tempFilePath.delete();
		} catch (IOException e) {
			System.err.println("Could not get the temporary file path for logging; exiting");
			System.err.flush();
			System.exit(1);
			return null;
		}
		
		// Set our temporary file path's subfolder and ensure that it exists
		tempFilePath = new File(tempFilePath.getParentFile(), "ParkNPark");
		if (!tempFilePath.exists() && !tempFilePath.mkdirs()) {
			System.err.println("Could not create the \"" + tempFilePath.getAbsolutePath() + "\" folder for logging; exiting");
			System.err.flush();
			System.exit(1);
		}
		
		// Return this temp file path
		return tempFilePath;
	}
}
