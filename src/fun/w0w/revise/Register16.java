package fun.w0w.revise;

public class Register16 implements Register {
  private short data;
  
  public int getVal() {
    return data & 0xFFFF;
  }
  
  public void setVal(int val) {
    data = (short) val;
  }
}
