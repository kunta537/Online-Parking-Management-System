package ParkNPark.interfaces;

/**
* ParkNPark/interfaces/ReplicationManagerHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from server.idl
* Friday, May 5, 2006 10:19:36 PM EDT
*/

public final class ReplicationManagerHolder implements org.omg.CORBA.portable.Streamable
{
  public ParkNPark.interfaces.ReplicationManager value = null;

  public ReplicationManagerHolder ()
  {
  }

  public ReplicationManagerHolder (ParkNPark.interfaces.ReplicationManager initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = ParkNPark.interfaces.ReplicationManagerHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    ParkNPark.interfaces.ReplicationManagerHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return ParkNPark.interfaces.ReplicationManagerHelper.type ();
  }

}
