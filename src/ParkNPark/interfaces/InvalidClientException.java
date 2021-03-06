package ParkNPark.interfaces;


/**
* ParkNPark/interfaces/InvalidClientException.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from server.idl
* Friday, May 5, 2006 10:19:35 PM EDT
*/

public final class InvalidClientException extends org.omg.CORBA.UserException
{

  /** The client ID that is not recognized by the system */
  public int clientID = (int)0;

  public InvalidClientException ()
  {
    super(InvalidClientExceptionHelper.id());
  } // ctor

  public InvalidClientException (int _clientID)
  {
    super(InvalidClientExceptionHelper.id());
    clientID = _clientID;
  } // ctor


  public InvalidClientException (String $reason, int _clientID)
  {
    super(InvalidClientExceptionHelper.id() + "  " + $reason);
    clientID = _clientID;
  } // ctor

} // class InvalidClientException
