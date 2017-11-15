package fun.w0w.revise;

public class Register8 implements Register {
  private byte data;
  
  public int getVal() {
    return data & 0xFF;
  }
  
  public void setVal(int val) {
    data = (byte) val;
  }
  
  public byte getRaw() {
    return data;
  }
}
