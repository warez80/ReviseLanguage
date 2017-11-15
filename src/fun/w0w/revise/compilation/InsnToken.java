package fun.w0w.revise.compilation;

public class InsnToken extends Token {
  private String insnName;
  
  public InsnToken(String tokendata) {
    insnName = tokendata;
  }

  public String getInsnName() {
    return insnName;
  }

  @Override
  public String toString() {
    return "InsnToken [insnName=" + insnName + "]";
  }
}
