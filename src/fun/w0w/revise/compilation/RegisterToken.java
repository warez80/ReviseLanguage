package fun.w0w.revise.compilation;

public class RegisterToken extends Token {
  private String register;
  
  public RegisterToken(String register) {
    this.register = register;
  }

  public String getRegister() {
    return register;
  }

  @Override
  public String toString() {
    return "RegisterToken [register=" + register + "]";
  }
}
