package ParkNPark.interfaces;


/**
* ParkNPark/interfaces/NotInLotException.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from server.idl
* Friday, May 5, 2006 10:19:36 PM EDT
*/

public final class NotInLotException extends org.omg.CORBA.UserException
{

  public NotInLotException ()
  {
    super(NotInLotExceptionHelper.id());
  } // ctor


  public NotInLotException (String $reason)
  {
    super(NotInLotExceptionHelper.id() + "  " + $reason);
  } // ctor

} // class NotInLotException
