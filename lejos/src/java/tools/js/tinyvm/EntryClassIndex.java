package js.tinyvm;

import java.io.IOException;

import js.tinyvm.io.ByteWriter;

public class EntryClassIndex implements WritableData, Constants
{
  String iClassName;
  Binary iBinary;

  public EntryClassIndex(Binary aBinary, String aClassName)
  {
    iBinary = aBinary;
    iClassName = aClassName;
  }

  public int getLength ()
  {
    return 1;
  }

  public void dump (ByteWriter aOut) throws TinyVMException
  {
    try
    {
      int pIndex = iBinary.getClassIndex(iClassName);
      assert pIndex >= 0 && pIndex < 256 : "Check: class index in range";
      aOut.writeU1(pIndex);
    }
    catch (IOException e)
    {
      throw new TinyVMException(e.getMessage(), e);
    }
  }
}

