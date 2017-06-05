package ParkNPark.interfaces;


/**
* ParkNPark/interfaces/ClientManagerHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from server.idl
* Friday, May 5, 2006 10:19:36 PM EDT
*/


/**
         * The interface that a single client uses to communicate with its server-side
         * client manager instance
         */
abstract public class ClientManagerHelper
{
  private static String  _id = "IDL:ParkNPark/interfaces/ClientManager:1.0";

  public static void insert (org.omg.CORBA.Any a, ParkNPark.interfaces.ClientManager that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static ParkNPark.interfaces.ClientManager extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CORBA.ORB.init ().create_interface_tc (ParkNPark.interfaces.ClientManagerHelper.id (), "ClientManager");
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static ParkNPark.interfaces.ClientManager read (org.omg.CORBA.portable.InputStream istream)
  {
    return narrow (istream.read_Object (_ClientManagerStub.class));
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, ParkNPark.interfaces.ClientManager value)
  {
    ostream.write_Object ((org.omg.CORBA.Object) value);
  }

  public static ParkNPark.interfaces.ClientManager narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof ParkNPark.interfaces.ClientManager)
      return (ParkNPark.interfaces.ClientManager)obj;
    else if (!obj._is_a (id ()))
      throw new org.omg.CORBA.BAD_PARAM ();
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      ParkNPark.interfaces._ClientManagerStub stub = new ParkNPark.interfaces._ClientManagerStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

  public static ParkNPark.interfaces.ClientManager unchecked_narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof ParkNPark.interfaces.ClientManager)
      return (ParkNPark.interfaces.ClientManager)obj;
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      ParkNPark.interfaces._ClientManagerStub stub = new ParkNPark.interfaces._ClientManagerStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

}
