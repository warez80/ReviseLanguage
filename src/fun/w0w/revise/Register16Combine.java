package fun.w0w.revise;

public final class Register16Combine extends Register16 {
  private final Register8 reg1, reg2;
  
  public Register16Combine(Register8 r1, Register8 r2) {
    reg1 = r1;
    reg2 = r2;
  }
  
  @Override
  public int getVal() {
    return ((reg1.getVal() << 8) | reg2.getVal())&0xFFFF;
  }

  @Override
  public void setVal(int i) {
    i&=0xFFFF;
    reg1.setVal(i >>> 8);
    reg2.setVal(i & 0xFF);
  }
}
