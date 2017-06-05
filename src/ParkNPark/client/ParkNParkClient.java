package ParkNPark.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;

import ParkNPark.common.CommandLineParser;
import ParkNPark.common.InputEater;
import ParkNPark.common.Logger;
import ParkNPark.interfaces.AlreadyInLotException;
import ParkNPark.interfaces.AtBottomLevelException;
import ParkNPark.interfaces.AtTopLevelException;
import ParkNPark.interfaces.ClientManagerOperations;
import ParkNPark.interfaces.LotFullException;
import ParkNPark.interfaces.LotNotFoundException;
import ParkNPark.interfaces.NotInLotException;
import ParkNPark.interfaces.NotOnExitLevelException;
import ParkNPark.interfaces.ServiceUnavailableException;

/**
 * ParkNPark client
 */
public class ParkNParkClient implements Runnable
{
	/** Our object request broker */
	protected ORB orb;
	
	/**
	 * Variables for real-time analysis
	 */
	
	/** Number of clients */
	protected int numClients;
	
	/** Number of servers */
	protected int numServers;
	
	/** When true, never give up when trying to contact the server */
	protected boolean neverGiveUp;
	
	/** Time between requests */
	protected int interRequestTime;
	
	/** Size of reply */
	protected int replySize;

	/** Logger instance */
	protected int getLotsModeCount;
	
	/** The wait timeout to use during fault detections */
	protected int detectionTimeout;
	
	/** The wait timeout to use during fault recoveries */
	protected int recoveryTimeout;
	
	/** Logger instance */
	protected Logger logger;
	
	/** The original System.err instance */
	protected PrintStream err;
	
	/**
	 * Convenience method that executes the ParkNParkClient from the command line
	 * @param args The command-line arguments to use when starting the client
	 */
	public static void main(String args[])
	{
		// Parse the command line
        CommandLineParser clp = new CommandLineParser();
		Set<CommandLineParser.AcceptedParameters> acceptedParameters = new HashSet<CommandLineParser.AcceptedParameters>();
		acceptedParameters.add(CommandLineParser.AcceptedParameters.ORBInitialHost);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.ORBInitialPort);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.numClients);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.numServers);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.interRequestTime);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.replySize);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.neverGiveUp);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.getLotsMode);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.detectionTimeout);
		acceptedParameters.add(CommandLineParser.AcceptedParameters.recoveryTimeout);
        if (!clp.parseCommandLine(ParkNParkClient.class.getName(), args, acceptedParameters)) {
        	System.exit(1);
        	return;
        }

        // Create and initialize the ORB
		try {
			ORB orb = ORB.init(args, null);

			// Create a new client instance and run it
			(new ParkNParkClient(orb, clp.getNumClients(), clp.getNumServers(),
					             clp.getInterRequestTime(), clp.getReplySize(),
					             clp.shouldNeverGiveUp(), clp.getGetLotsModeCount(),
					             clp.getFaultDetectionTimeout(),
					             clp.getFaultRecoveryTimeout())).run();
		} catch (Exception e) {
			System.err.println("Could not initialize the CORBA object request broker due to " + e.getClass().getName() + "; exiting");
		}
	}
	
	/**
	 * Creates a new ParkNParkClient instance that will use the given
	 * ORB to communicate with the server
	 * @param orb The ORB to communicate with the server on
	 * @param numClients The number of clients in the current test
	 * @param numServers The number of servers in the current test
	 * @param interRequestTime The delay between each invocation, which is used when
	 * constructing the log file names
	 * @param replySize The size of the server's replies, which is used when
	 * constructing the log file names
	 * @param neverGiveUp When true, the client will never give up its attempt to
	 * contact the server, even if no primary is registered
	 * @param detectionTimeout The wait timeout to use during fault detections
	 * @param recoveryTimeout The wait timeout to use during fault recoveries
	 */
	public ParkNParkClient(ORB orb, int numClients, int numServers, int interRequestTime,
			               int replySize, boolean neverGiveUp, int getLotsModeCount,
			               int detectionTimeout, int recoveryTimeout) {
		this.orb = orb;
		this.numClients = numClients;
		this.numServers = numServers;
		this.interRequestTime = interRequestTime;
		this.replySize = replySize;
		this.neverGiveUp = neverGiveUp;
		this.getLotsModeCount = getLotsModeCount;
		this.detectionTimeout = detectionTimeout;
		this.recoveryTimeout = recoveryTimeout;
		
		// Get our host name
		String hostName;
        try {
        	hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
           	System.err.println("Could not get the system's host name, which is required for proper operation; exiting");
           	System.exit(1);
           	return;
        }

        // Create our logger instance
		logger = new Logger(numClients, numServers, 11000 / numClients + 1 /* we only need a buffer for one client (our instance) */,
				            interRequestTime, replySize, "cli", hostName, false, true, false);
	}
	
	/**
	 * Runs the ParkNParkClient
	 */
	public void run()
	{
		int seq = 1;

		// Store the System.out value
		PrintStream out = System.out;
		
		// Set System.err to the input eater
		err = System.err;
		System.setErr(new PrintStream(new InputEater()));

		try
		{
			// Create our fault tolerant client manager instance, which handles the server
			// communication details for us
			ClientManagerOperations cm;
			try {
				cm = new FaultTolerantClientManager(orb, logger, interRequestTime, neverGiveUp,
						                            detectionTimeout, recoveryTimeout, err);
			} catch (ServiceUnavailableException e) {
				err.println("Could not contact a primary server; none were registered; exiting");
				System.exit(1);
				return;
			}

			// Create a buffered reader on the command line input
			BufferedReader objReader = new BufferedReader(new InputStreamReader(System.in));

			// Enter the main user interface loop
			int lots[] = null;
			int levels[] = null;
			int lotChoice;
			out.println("\nParkNPark");
			boolean displayLots = true;
			
			if(getLotsModeCount > 0) {
				while(getLotsModeCount > 0) {
					lots = cm.getLots().value;
					getLotsModeCount--;
				}
			}
				
			else { 
				while (true)
				{
					// If we are supposed to display the list of parking lots, then do so now
					if (displayLots)
					{
						// Get the list of valid parking lots from the server
						lots = cm.getLots().value;

						// If we have lots, then display them; otherwise, inform the user
						if (lots.length > 0) {
							out.println("Following is the List of Lots.");
							for (int i = 0; i < lots.length; i++)
								out.println((i + 1) + ". Lot " + lots[i]);
						} else
							out.println("No lots are defined in the system.");
					} else
						displayLots = true;

					// Prompt the user for the lot number and read it in
					out.println("Enter your choice and press return (press 0 to exit): ");
					try {
						lotChoice = Integer.parseInt(objReader.readLine().trim());
					} catch (NumberFormatException nfe) {
						out.println("Enter a valid choice, formatted as a number");
						continue;
					}
					
					// If the user entered 0, then exit the main user interface loop
					if (lotChoice == 0)
						break;

					// If the user entered an out-of-range lot, then ask the user for the
					// lot number again
					if ((lotChoice != 0) && (lotChoice > lots.length)) {
						out.println("Enter a valid choice");
						continue;
					}

					// Parking lot number is valid, so enter the lot
					if ((lotChoice > 0) && (lotChoice <= lots.length)) {
						try
						{
							// Try to enter the lot and get the list of available levels
							levels = cm.enterLot(seq, lots[lotChoice - 1]).value;
							seq++;
						} catch (LotFullException e)
						{
							// The parking lot is full!
							out.println("This Lot is full!");
							try
							{
								// Get the availability of other parking lots
								int[] otherLots = cm.getOtherLotAvailability(lotChoice).value;

								// Display the other lots if we have them or, if none, inform the user
								if (otherLots.length > 0) {
									out.println("Other Avaialble Lots are:");
									for (int i : otherLots)
										out.println(i + ".Lot" + i);

									// Don't display the full list of lots when we loop, since we
									// just gave the user a list of available lots
									displayLots = false;
								} else
									out.println("No other lots have available spaces.");
							} catch (LotNotFoundException lnfe) {
								out.println("Lot " + lnfe.lot + " disappeared when we tried to get the availability of other lots");
							}
							
							// Ask for the parking lot to enter
							continue;
						} catch (LotNotFoundException lnfe) {
							out.println("Lot " + lnfe.lot + " does not exist in the system");
							continue;
						} catch (AlreadyInLotException e) {
							out.println("You are already in parking lot " + e.lot + ", which is unexpected.");
							out.println("The server and the client disagree on the state; exiting");
							closeLogFiles();
							System.exit(1);
							return;
						}

						// Write out the levels that have available spaces
						if (levels != null) {
							out.println("Available level" + ((levels.length == 1)? "": "s") + " for lot " + lotChoice + " are:");
							for (int i : levels)
								out.println("Level " + i);
						}

						// Enter the in-parking-lot loop
						while (true)
						{
							// List of Options
							try
							{
								// If we are in a lot, display the menu; otherwise, indicate that
								// we are not in a lot (that condition can happen when we
								// "teleport" back into the lot after leaving for testing)
								try
								{
									// Display the current level in the parking lot
								    out.println("Current Level: " + cm.getCurrentLevel().value);

								    // Display the menu
									if (cm.getCurrentLevel().value < cm.getMaxLevel().value)
										out.println("U. Move Up");
									if (cm.getCurrentLevel().value <= cm.getMinLevel().value)
										out.println("X. Exit Lot");
									else
										out.println("D. Move Down");
								} catch (NotInLotException nile) {
									out.println("Server says that the car is not in lot in a parking lot");
								}


								// Get the user's selection
								out.println("Enter your choice:");
								String inLotChoice = objReader.readLine().trim();

								// User wants to move up one level
								if (inLotChoice.equalsIgnoreCase("U")) {
									cm.moveUpLevel(seq);
									seq++;
									continue;
								}

								// User wants to move down one level
								else if (inLotChoice.equalsIgnoreCase("D")) {
									cm.moveDownLevel(seq);
									seq++;
									continue;
								}

								// User wants to exit the lot
								else if (inLotChoice.equalsIgnoreCase("X")) {
								    cm.exitLot(seq);
								    seq++;
								    // Break out of the in-parking-lot loop
									break;
								}
								
								// User wants to force their way out of the parking lot
								// (via teleportation or something; this is meant for
								// testing purposes to test AlreadyInLotException and such)
								else if (inLotChoice.equalsIgnoreCase("X->TeleportOut")) {
									System.out.println("Magically teleporting out of the parking lot");
									break;
								}
								
								// User exits the parking lot but teleports back into
								// the parking lot (this is meant to test NotInLotException)
								else if (inLotChoice.equalsIgnoreCase("X->TeleportIn")) {
									System.out.println("Leaving the lot and then magically teleporting back in");
								    cm.exitLot(seq);
								    seq++;
								}
								
								// User wants to hose the server's database connection
								else if (inLotChoice.equalsIgnoreCase("H"))
									((FaultTolerantClientManager) cm).hoseServerDatabaseConnection();
								
								else {
									System.out.println("The command entered is not valid");
								}
							} catch (AtTopLevelException atle) {
								out.println("Car is already on the top level");
							} catch (AtBottomLevelException atle) {
								out.println("Car is already on the bottom level");
							} catch (NotOnExitLevelException noel) {
								out.println("Exiting the lot is not permitted on level " + noel.level);
							} catch (NotInLotException nile) {
								out.println("Car is not in lot in a parking lot");
							}
						}
					}
				}
			}
			// Close the client manager
			System.out.println("Closing the client manager at the server");
		    cm.closeClientManager();
		} catch (ServiceUnavailableException e) {
			err.println("No servers are running; exiting");
			closeLogFiles();
			System.exit(1);
			return;
		} catch (SystemException e) {
			err.println("The system is down; exiting");
			err.flush();
			closeLogFiles();
			System.exit(1);
			return;
		} catch (Exception e) {
			err.println("Miscellaneous exception (possibly a program bug?): " + e.getClass().getName() + "; exiting");
			err.flush();
			closeLogFiles();
			e.printStackTrace(err);
			System.exit(1);
			return;
		}
		
		// Close the log files
		closeLogFiles();
		
		// Restore the original System.err
		System.setErr(err);
	}
	
	/**
	 * Close the log files and, if a problem arises, notify the user
	 */
	private void closeLogFiles()
	{
		// Close the log files, which flushes out the logs to the disk
		try {
			logger.close();
		} catch (IOException e) {
			err.println("IOException in Logger.close(): " + e.getMessage());
			err.println("Recent log data might be permanently lost");
		}
	}
}
