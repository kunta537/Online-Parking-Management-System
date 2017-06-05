package ParkNPark.interfaces;


/**
* ParkNPark/interfaces/ByteSeqHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from server.idl
* Friday, May 5, 2006 10:19:35 PM EDT
*/


/**
         * CORBA type for a byte array
         */
public final class ByteSeqHolder implements org.omg.CORBA.portable.Streamable
{
  public byte value[] = null;

  public ByteSeqHolder ()
  {
  }

  public ByteSeqHolder (byte[] initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = ParkNPark.interfaces.ByteSeqHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    ParkNPark.interfaces.ByteSeqHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return ParkNPark.interfaces.ByteSeqHelper.type ();
  }

}
