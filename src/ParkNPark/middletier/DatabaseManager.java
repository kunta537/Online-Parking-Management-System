package ParkNPark.middletier;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.WeakHashMap;

/**
 * Manages the server's database parameters
 */
public class DatabaseManager
{
	/** The JDBC driver class */
	private String jdbcDriver = "com.mysql.jdbc.Driver";
	
	/** The JDBC URL */
	private String jdbcURL;
	
	/** The user name */
	private String jdbcUsername;
	
	/** The password */
	private	String jdbcPassword;
	
	/** Weak set of references to our created connections so that we can hose
	 *  them upon request */
	protected WeakHashMap<Connection,Object> connections = new WeakHashMap<Connection,Object>();

	/**
	 * Create the database manager and initialize the JDBC driver
	 * @param jdbcURL The JDBC URL to use in the database connection
	 * @param jdbcUsername The username to use in the database connection
	 * @param jdbcPassword The password to use in the database connection
	 */
	public DatabaseManager(String jdbcURL, String jdbcUsername, String jdbcPassword) {
		this.jdbcURL = jdbcURL;
		this.jdbcUsername = jdbcUsername;
		this.jdbcPassword = jdbcPassword;
		
		// Load the JDBC driver
		try {
			Class.forName(jdbcDriver);
		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.err.println("Could not load JDBC driver; ensure that it is within the classpath");
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a new database connection using the database manager's
	 * current configuration
	 * @return A new database connection using the database manager's
	 * current configuration
	 * @throws SQLException Thrown when a problem arises while creating
	 * a new connection to the database
	 */
	public Connection getConnection() throws SQLException
	{
		// Create the connection
		Connection connection = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
		
		// Store this connection's reference weakly
		connections.put(connection, null);
		
		// Return the connection
		return connection;
	}
	
	/**
	 * Hose all current and future connections
	 */
	public void hoseConnection()
	{
		// Hose future connections
		jdbcURL = "jdbc:meow://fluffy_kittens_invade_mars!";
		
		// Hose existing connections
		Iterator<Connection> iterator = connections.keySet().iterator();
		Connection connection;
		while (iterator.hasNext()) {
			connection = iterator.next();
			
			// Close this database connection
			try {
				connection.close();
			} catch (SQLException e) {
				// If this gave a SQLException, then it's probably already closed
				// or hosed. Thus, we don't care
			}
		}
	}
}
