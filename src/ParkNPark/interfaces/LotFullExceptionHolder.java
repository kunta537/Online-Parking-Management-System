package ParkNPark.interfaces;

/**
* ParkNPark/interfaces/LotFullExceptionHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from server.idl
* Friday, May 5, 2006 10:19:35 PM EDT
*/

public final class LotFullExceptionHolder implements org.omg.CORBA.portable.Streamable
{
  public ParkNPark.interfaces.LotFullException value = null;

  public LotFullExceptionHolder ()
  {
  }

  public LotFullExceptionHolder (ParkNPark.interfaces.LotFullException initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = ParkNPark.interfaces.LotFullExceptionHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    ParkNPark.interfaces.LotFullExceptionHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return ParkNPark.interfaces.LotFullExceptionHelper.type ();
  }

}