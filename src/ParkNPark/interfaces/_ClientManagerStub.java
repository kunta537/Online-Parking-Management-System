package ParkNPark.interfaces;


/**
* ParkNPark/interfaces/_ClientManagerStub.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from server.idl
* Friday, May 5, 2006 10:19:36 PM EDT
*/


/**
         * The interface that a single client uses to communicate with its server-side
         * client manager instance
         */
public class _ClientManagerStub extends org.omg.CORBA.portable.ObjectImpl implements ParkNPark.interfaces.ClientManager
{


  /**
               * Returns the client's ID, which can be used in the client manager factory's
               * getExistingClientManager() method
               * @return The client's ID
               * @throws ServiceUnavailableException Thrown when the log is closed and the
               * server is shutting down
               */
  public ParkNPark.interfaces.PaddedInteger getClientID () throws ParkNPark.interfaces.ServiceUnavailableException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("getClientID", true);
                $in = _invoke ($out);
                ParkNPark.interfaces.PaddedInteger $result = ParkNPark.interfaces.PaddedIntegerHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:ParkNPark/interfaces/ServiceUnavailableException:1.0"))
                    throw ParkNPark.interfaces.ServiceUnavailableExceptionHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return getClientID (        );
            } finally {
                _releaseReply ($in);
            }
  } // getClientID


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
  public ParkNPark.interfaces.PaddedIntegerSeq enterLot (int seq, int lot) throws ParkNPark.interfaces.AlreadyInLotException, ParkNPark.interfaces.LotNotFoundException, ParkNPark.interfaces.LotFullException, ParkNPark.interfaces.ServiceUnavailableException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("enterLot", true);
                $out.write_long (seq);
                $out.write_long (lot);
                $in = _invoke ($out);
                ParkNPark.interfaces.PaddedIntegerSeq $result = ParkNPark.interfaces.PaddedIntegerSeqHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:ParkNPark/interfaces/AlreadyInLotException:1.0"))
                    throw ParkNPark.interfaces.AlreadyInLotExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/LotNotFoundException:1.0"))
                    throw ParkNPark.interfaces.LotNotFoundExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/LotFullException:1.0"))
                    throw ParkNPark.interfaces.LotFullExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/ServiceUnavailableException:1.0"))
                    throw ParkNPark.interfaces.ServiceUnavailableExceptionHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return enterLot (seq, lot        );
            } finally {
                _releaseReply ($in);
            }
  } // enterLot


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
  public ParkNPark.interfaces.PaddedVoid exitLot (int seq) throws ParkNPark.interfaces.NotInLotException, ParkNPark.interfaces.NotOnExitLevelException, ParkNPark.interfaces.ServiceUnavailableException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("exitLot", true);
                $out.write_long (seq);
                $in = _invoke ($out);
                ParkNPark.interfaces.PaddedVoid $result = ParkNPark.interfaces.PaddedVoidHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:ParkNPark/interfaces/NotInLotException:1.0"))
                    throw ParkNPark.interfaces.NotInLotExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/NotOnExitLevelException:1.0"))
                    throw ParkNPark.interfaces.NotOnExitLevelExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/ServiceUnavailableException:1.0"))
                    throw ParkNPark.interfaces.ServiceUnavailableExceptionHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return exitLot (seq        );
            } finally {
                _releaseReply ($in);
            }
  } // exitLot


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
  public ParkNPark.interfaces.PaddedIntegerSeq getOtherLotAvailability (int lot) throws ParkNPark.interfaces.LotNotFoundException, ParkNPark.interfaces.ServiceUnavailableException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("getOtherLotAvailability", true);
                $out.write_long (lot);
                $in = _invoke ($out);
                ParkNPark.interfaces.PaddedIntegerSeq $result = ParkNPark.interfaces.PaddedIntegerSeqHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:ParkNPark/interfaces/LotNotFoundException:1.0"))
                    throw ParkNPark.interfaces.LotNotFoundExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/ServiceUnavailableException:1.0"))
                    throw ParkNPark.interfaces.ServiceUnavailableExceptionHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return getOtherLotAvailability (lot        );
            } finally {
                _releaseReply ($in);
            }
  } // getOtherLotAvailability


  /**
               * Returns an array of valid lot numbers in the system, sorted by the lot number in ascending order
               * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
               * other reason prevents the system from discovering the its defined lots
               * @return An array of valid lot numbers in the system
               * [Category: Baseline; Requirement: 12]
               */
  public ParkNPark.interfaces.PaddedIntegerSeq getLots () throws ParkNPark.interfaces.ServiceUnavailableException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("getLots", true);
                $in = _invoke ($out);
                ParkNPark.interfaces.PaddedIntegerSeq $result = ParkNPark.interfaces.PaddedIntegerSeqHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:ParkNPark/interfaces/ServiceUnavailableException:1.0"))
                    throw ParkNPark.interfaces.ServiceUnavailableExceptionHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return getLots (        );
            } finally {
                _releaseReply ($in);
            }
  } // getLots


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
  public ParkNPark.interfaces.PaddedInteger moveUpLevel (int seq) throws ParkNPark.interfaces.NotInLotException, ParkNPark.interfaces.AtTopLevelException, ParkNPark.interfaces.ServiceUnavailableException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("moveUpLevel", true);
                $out.write_long (seq);
                $in = _invoke ($out);
                ParkNPark.interfaces.PaddedInteger $result = ParkNPark.interfaces.PaddedIntegerHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:ParkNPark/interfaces/NotInLotException:1.0"))
                    throw ParkNPark.interfaces.NotInLotExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/AtTopLevelException:1.0"))
                    throw ParkNPark.interfaces.AtTopLevelExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/ServiceUnavailableException:1.0"))
                    throw ParkNPark.interfaces.ServiceUnavailableExceptionHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return moveUpLevel (seq        );
            } finally {
                _releaseReply ($in);
            }
  } // moveUpLevel


  /**
               * Moves the car from its present level to the level beneath it
               * @param seq The latest sequence number of the client
               * @throws NotInLotException Thrown if the car is not in a lot
               * @throws AtTopLevelException Thrown if the car is already on the lowest level
               * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some other reason prevents the car from moving to the lower level
               * @return The level number that the client's car is now on
               * [Category: Baseline; Requirement: 6]
               */
  public ParkNPark.interfaces.PaddedInteger moveDownLevel (int seq) throws ParkNPark.interfaces.NotInLotException, ParkNPark.interfaces.AtBottomLevelException, ParkNPark.interfaces.ServiceUnavailableException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("moveDownLevel", true);
                $out.write_long (seq);
                $in = _invoke ($out);
                ParkNPark.interfaces.PaddedInteger $result = ParkNPark.interfaces.PaddedIntegerHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:ParkNPark/interfaces/NotInLotException:1.0"))
                    throw ParkNPark.interfaces.NotInLotExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/AtBottomLevelException:1.0"))
                    throw ParkNPark.interfaces.AtBottomLevelExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/ServiceUnavailableException:1.0"))
                    throw ParkNPark.interfaces.ServiceUnavailableExceptionHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return moveDownLevel (seq        );
            } finally {
                _releaseReply ($in);
            }
  } // moveDownLevel


  /**
               * Returns the car's current level number
               * @throws NotInLotException Thrown if the car is not in a lot
               * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
               * other reason prevents the system from returning the car's current level
               * @return The car's current level number
               * [Category: Baseline; Requirement: 6]
               */
  public ParkNPark.interfaces.PaddedInteger getCurrentLevel () throws ParkNPark.interfaces.NotInLotException, ParkNPark.interfaces.ServiceUnavailableException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("getCurrentLevel", true);
                $in = _invoke ($out);
                ParkNPark.interfaces.PaddedInteger $result = ParkNPark.interfaces.PaddedIntegerHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:ParkNPark/interfaces/NotInLotException:1.0"))
                    throw ParkNPark.interfaces.NotInLotExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/ServiceUnavailableException:1.0"))
                    throw ParkNPark.interfaces.ServiceUnavailableExceptionHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return getCurrentLevel (        );
            } finally {
                _releaseReply ($in);
            }
  } // getCurrentLevel


  /**
               * Returns the top level number of the car's current parking lot
               * @throws NotInLotException Thrown if the car is not in a lot
               * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
               * other reason prevents the system from returning the current lot's highest level
               * @return The top level number of the car's current parking lot
               * [Category: Baseline; Requirement: 6]
               */
  public ParkNPark.interfaces.PaddedInteger getMaxLevel () throws ParkNPark.interfaces.NotInLotException, ParkNPark.interfaces.ServiceUnavailableException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("getMaxLevel", true);
                $in = _invoke ($out);
                ParkNPark.interfaces.PaddedInteger $result = ParkNPark.interfaces.PaddedIntegerHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:ParkNPark/interfaces/NotInLotException:1.0"))
                    throw ParkNPark.interfaces.NotInLotExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/ServiceUnavailableException:1.0"))
                    throw ParkNPark.interfaces.ServiceUnavailableExceptionHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return getMaxLevel (        );
            } finally {
                _releaseReply ($in);
            }
  } // getMaxLevel


  /**
               * Returns the bottom level number of the car's current parking lot
               * @throws NotInLotException Thrown if the car is not in a lot
               * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
               * other reason prevents the system from returning the current lot's lowest level
               * @return The bottom level number of the car's current parking lot
               * [Category: Baseline; Requirement: 6]
               */
  public ParkNPark.interfaces.PaddedInteger getMinLevel () throws ParkNPark.interfaces.NotInLotException, ParkNPark.interfaces.ServiceUnavailableException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("getMinLevel", true);
                $in = _invoke ($out);
                ParkNPark.interfaces.PaddedInteger $result = ParkNPark.interfaces.PaddedIntegerHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                if (_id.equals ("IDL:ParkNPark/interfaces/NotInLotException:1.0"))
                    throw ParkNPark.interfaces.NotInLotExceptionHelper.read ($in);
                else if (_id.equals ("IDL:ParkNPark/interfaces/ServiceUnavailableException:1.0"))
                    throw ParkNPark.interfaces.ServiceUnavailableExceptionHelper.read ($in);
                else
                    throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return getMinLevel (        );
            } finally {
                _releaseReply ($in);
            }
  } // getMinLevel


  /**
               * Closes the client manager and frees server resources associated with it, including the client
               * manager's activation in the server's CORBA portable object adapter
               * @throws ServiceUnavailableException Thrown if the database cannot be contacted or if some
               * other reason prevents the system from closing the client manager. The client manager remains
               * open if this exception gets thrown
               */
  public void closeClientManager () throws ParkNPark.interfaces.ServiceUnavailableException
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("closeClientManager", true);
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
                closeClientManager (        );
            } finally {
                _releaseReply ($in);
            }
  } // closeClientManager

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:ParkNPark/interfaces/ClientManager:1.0"};

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
} // class _ClientManagerStub
