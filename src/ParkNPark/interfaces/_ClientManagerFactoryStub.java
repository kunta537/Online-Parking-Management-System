package ParkNPark.interfaces;


/**
* ParkNPark/interfaces/_ClientManagerFactoryStub.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from server.idl
* Friday, May 5, 2006 10:19:36 PM EDT
*/


/**
         * The main interface that the client uses to communicate with the server
         */
public class _ClientManagerFactoryStub extends org.omg.CORBA.portable.ObjectImpl implements ParkNPark.interfaces.ClientManagerFactory
{


  /**
               * Creates a new client manager instance for a new client
               * @param hostname The host name of the client
               * @throws ServiceUnavailableException Thrown if the database cannot be
               * contacted or if some other reason prevents the client manager from
               * being created successfully
               * @return A new client manager instance for a new client
               * [Category: Dependability; Requirement: 2
               */
  public ParkNPark.interfaces.ClientManager getClientManager (String hostname) throws ParkNPark.interfaces.ServiceUnavailableException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("getClientManager", true);
                $out.write_string (hostname);
                $in = _invoke ($out);
                ParkNPark.interfaces.ClientManager $result = ParkNPark.interfaces.ClientManagerHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:ParkNPark/interfaces/ServiceUnavailableException:1.0"))
                    throw ParkNPark.interfaces.ServiceUnavailableExceptionHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return getClientManager (hostname        );
            } finally {
                _releaseReply ($in);
            }
  } // getClientManager


  /**
               * Returns the existing client manager instance for an existing client. A client can call
               * this method with its client ID and last sequence number on any server and should get its
               * client manager instance
               * @param clientID The ID of the client to get the ClientManager of
               * @param hostname The host name of the client
               * @throws InvalidClientException Thrown when the given client ID is not known to the system
               * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if
               * some other reason prevents the client manager from being retrieved successfully
               * @return The existing client manager instance for an existing client
               * [Category: Dependability; Requirement: 2]
               */
  public ParkNPark.interfaces.ClientManager getExistingClientManager (int clientID, String hostname) throws ParkNPark.interfaces.ServiceUnavailableException, ParkNPark.interfaces.InvalidClientException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("getExistingClientManager", true);
                $out.write_long (clientID);
                $out.write_string (hostname);
                $in = _invoke ($out);
                ParkNPark.interfaces.ClientManager $result = ParkNPark.interfaces.ClientManagerHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:ParkNPark/interfaces/ServiceUnavailableException:1.0"))
                    throw ParkNPark.interfaces.ServiceUnavailableExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/InvalidClientException:1.0"))
                    throw ParkNPark.interfaces.InvalidClientExceptionHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return getExistingClientManager (clientID, hostname        );
            } finally {
                _releaseReply ($in);
            }
  } // getExistingClientManager


  /**
               * Pokes the server to see if it is still alive and that it can still
               * communicate with the database
               * @throws ServiceUnavailableException Thrown when the server's database
               * connection is not working
               * [Category: Dependability; Requirement: 1]
               */
  public void poke () throws ParkNPark.interfaces.ServiceUnavailableException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("poke", true);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:ParkNPark/interfaces/ServiceUnavailableException:1.0"))
                    throw ParkNPark.interfaces.ServiceUnavailableExceptionHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                poke (        );
            } finally {
                _releaseReply ($in);
            }
  } // poke


  /**
               * Causes the server's database connection to become "hosed," meaning
               * that it will fail to work after this method is called. This is used
               * primarily for fault injection
               * [TO TEST ROBUSTNESS]
               */
  public void hoseDatabaseConnection ()
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("hoseDatabaseConnection", true);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                hoseDatabaseConnection (        );
            } finally {
                _releaseReply ($in);
            }
  } // hoseDatabaseConnection


  /**
               * Kills the server
               * [TO TEST FAILURES]
               */
  public void killServer ()
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("killServer", true);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                killServer (        );
            } finally {
                _releaseReply ($in);
            }
  } // killServer


  /**
               * Exits the server gracefully
               * [TO TERMINATE THE SERVICE]
               */
  public void exitServer ()
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("exitServer", true);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                exitServer (        );
            } finally {
                _releaseReply ($in);
            }
  } // exitServer


  /**
               * Flushes the server's log files
               * [FOR EXPERIMENTS]
               */
  public void flushLogs ()
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("flushLogs", true);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                flushLogs (        );
            } finally {
                _releaseReply ($in);
            }
  } // flushLogs

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:ParkNPark/interfaces/ClientManagerFactory:1.0"};

  public String[] _ids ()
  {
    return (String[])__ids.clone ();
  }

  private void readObject (java.io.ObjectInputStream s) throws java.io.IOException
  {
     String str = s.readUTF ();
     String[] args = null;
     java.util.Properties props = null;
     org.omg.CORBA.Object obj = org.omg.CORBA.ORB.init (args, props).string_to_object (str);
     org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl) obj)._get_delegate ();
     _set_delegate (delegate);
  }

  private void writeObject (java.io.ObjectOutputStream s) throws java.io.IOException
  {
     String[] args = null;
     java.util.Properties props = null;
     String str = org.omg.CORBA.ORB.init (args, props).object_to_string (this);
     s.writeUTF (str);
  }
} // class _ClientManagerFactoryStub
