package ParkNPark.interfaces;

/**
* ParkNPark/interfaces/LotNotFoundExceptionHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from server.idl
* Friday, May 5, 2006 10:19:35 PM EDT
*/

public final class LotNotFoundExceptionHolder implements org.omg.CORBA.portable.Streamable
{
  public ParkNPark.interfaces.LotNotFoundException value = null;

  public LotNotFoundExceptionHolder ()
  {
  }

  public LotNotFoundExceptionHolder (ParkNPark.interfaces.LotNotFoundException initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = ParkNPark.interfaces.LotNotFoundExceptionHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    ParkNPark.interfaces.LotNotFoundExceptionHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return ParkNPark.interfaces.LotNotFoundExceptionHelper.type ();
  }

}
