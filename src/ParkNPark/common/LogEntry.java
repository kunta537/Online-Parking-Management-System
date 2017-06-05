package ParkNPark.common;

/**
 * Contains a single log entry
 */
public class LogEntry
{
	/** Time that method call began */
	protected long probeIn;
	
	/** Time that method call completed */
	protected long probeOut;
	
	/** Time that database access began */
	protected long probeDbIn;
	
	/** Time that database access completed */
	protected long probeDbOut;
	
	/** Time that naming service access began */
	protected long probeNameServerIn;
	
	/** Time that naming service access completed */
	protected long probeNameServerOut;
	
	/** Time that naming service access began */
	protected long probeClientManagerIn;
	
	/** Time that naming service access completed */
	protected long probeClientManagerOut;
	
	/** Time that the first fault began */
	protected long probeFaultDetection;
	
	/** Time that the last fault completed */
	protected long probeFaultRecovery;
	
	/** Time that the waiting began */
	protected long probeWaitIn;
	
	/** Time that the waiting completed */
	protected long probeWaitOut;

	/** Method name */
	protected String probeMethod;
	
	/** Client name */
	protected String probeSource;
	
	/** Whether or not this log entry is finished */
	protected boolean logEntryFinished = false;

	/**
	 * Sets the starting time of the method call to the current time
	 */
	public void setProbeIn() {
		this.probeIn = System.nanoTime() / 1000;
	}

	/**
	 * Sets the name of the method being probed
	 * @param probeMethod The name of the method being probed
	 */
	public void setProbeMethod(String probeMethod) {
		this.probeMethod = probeMethod;
	}

	/**
	 * Sets the time that the first fault began to the current time
	 */
	public void setProbeFaultDetection() {
		this.probeFaultDetection = System.nanoTime() / 1000;
	}

	/**
	 * Sets the time that the last fault completed to the current time
	 */
	public void setProbeFaultRecovery() {
		this.probeFaultRecovery = System.nanoTime() / 1000;
	}

	/**
	 * Sets the completed time of the method call to the current time
	 */
	public void setProbeOut() {
		this.probeOut = System.nanoTime() / 1000;
	}

	/**
	 * Sets the client name of the method call
	 * @param probeSource The client name of the method call
	 */
	public void setProbeSource(String probeSource) {
		this.probeSource = probeSource;
	}
	
	/** 
	 * Sets the time that the database access began
	 */
	public void setProbeDbIn()
	{	this.probeDbIn = System.nanoTime() / 1000;
	}
	
	/** 
	 * Sets the time that the database access ended to the current time
	 */
	public void setProbeDbOut()
	{	this.probeDbOut = System.nanoTime() / 1000;
	}

	/**
	 * Returns the starting time of this method call
	 * @return The starting time of this method call
	 */
	public long getProbeIn() {
		return probeIn;
	}

	/**
	 * Returns the name of the method
	 * @return The name of the method
	 */
	public String getProbeMethod() {
		return probeMethod;
	}

	/**
	 * Gets the time that the first fault began
	 * @return The time that the first fault began
	 */
	public long getProbeFaultDetection() {
		return probeFaultDetection;
	}

	/**
	 * Gets the time that the last fault completed
	 * @return The time that the last fault completed
	 */
	public long getProbeFaultRecovery() {
		return probeFaultRecovery;
	}

	/**
	 * Returns the ending time of this method call
	 * @return The ending time of this method call
	 */
	public long getProbeOut() {
		return probeOut;
	}

	/**
	 * Returns the client name of the method call
	 * @return The client name of the method call
	 */
	public String getProbeSource() {
		return probeSource;
	}
	
	/**
	 * Gets the time that the database access began
	 * @return the time that the database access began
	 */
	public long getProbeDbIn()
	{
		return probeDbIn;
	}
	
	/**
	 * Gets the time that the database access ended
	 * @return the time that the database access ended
	 */
	public long getProbeDbOut()
	{
		return probeDbOut;
	}
	
	/**
	 * Returns true if this log entry is finished or false if it
	 * is not
	 * @return True if this log entry is finished or false if it
	 * is not
	 */
	public synchronized boolean isLogEntryFinished() {
		return logEntryFinished;
	}
	
	/**
	 * Marks this LogEntry as finished, which means that it is ready
	 * to be written to the log files
	 */
	public synchronized void markLogEntryAsFinished() {
		logEntryFinished = true;
	}

	/**
	 * Gets the time of the probe before the name service is called
	 * @return The time of the probe before the name service is called
	 */
	public long getProbeNameServerIn() {
		return probeNameServerIn;
	}

	/**
	 * Sets the time of the probe before the name service is called
	 * to the current time
	 */
	public void setProbeNameServerIn()
	{
		// If an existing in and out time exists, then subtract their
		// distance from the current start time
		long newTime = System.nanoTime() / 1000;
		if (probeNameServerIn != 0 && probeNameServerOut != 0)
			probeNameServerIn = newTime - (probeNameServerOut - probeNameServerIn);
		else
		    probeNameServerIn = newTime;
	}

	/**
	 * Gets the time of the probe after the name service is called
	 * @return The time of the probe after the name service is called
	 */
	public long getProbeNameServerOut() {
		return probeNameServerOut;
	}

	/**
	 * Sets the time of the probe after the name service is called
	 * to the current time
	 */
	public void setProbeNameServerOut() {
		this.probeNameServerOut = System.nanoTime() / 1000;
	}

	/**
	 * Gets the time of the probe after the client manager factory is called
	 * @return The time of the probe after the client manager factory is called
	 */
	public long getProbeClientManagerOut() {
		return probeClientManagerOut;
	}

	/**
	 * Sets the time of the probe after the client manager factory is called
	 * to the current time
	 */
	public void setProbeClientManagerOut() {
		this.probeClientManagerOut = System.nanoTime() / 1000;
	}

	/**
	 * Gets the time of the probe before the client manager factory is called
	 * @return The time of the probe before the client manager factory is called
	 */
	public long getProbeClientManagerIn() {
		return probeClientManagerIn;
	}

	/**
	 * Sets the time of the probe before the client manager factory is called
	 * to the current time
	 */
	public void setProbeClientManagerIn()
	{
		// If an existing in and out time exists, then subtract their
		// distance from the current start time
		long newTime = System.nanoTime() / 1000;
		if (probeClientManagerIn != 0 && probeClientManagerOut != 0)
			probeClientManagerIn = newTime - (probeClientManagerOut - probeClientManagerIn);
		else
			probeClientManagerIn = newTime;
	}
	
	/**
	 * Sets the time that a wait began to the current time
	 */
	public void setProbeWaitIn()
	{
		// If an existing in and out time exists, then subtract their
		// distance from the current start time
		long newTime = System.nanoTime() / 1000;
		if (probeWaitIn != 0 && probeWaitOut != 0)
			probeWaitIn = newTime - (probeWaitOut - probeWaitIn);
		else
		    probeWaitIn = newTime;
	}
	
	/**
	 * Gets the time that a wait began
	 * @return The time that a wait began
	 */
	public long getProbeWaitIn() {
		return probeWaitIn;
	}
	
	/**
	 * Sets the time that a wait ended to the current time
	 */
	public void setProbeWaitOut() {
		probeWaitOut = System.nanoTime() / 1000;
	}
	
	/**
	 * Gets the time that a wait completed
	 * @return The time that a wait completed
	 */
	public long getProbeWaitOut() {
		return probeWaitOut;
	}
}
