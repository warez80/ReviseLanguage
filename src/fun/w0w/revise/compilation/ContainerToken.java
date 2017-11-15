package fun.w0w.revise.compilation;

import java.util.ArrayList;
import java.util.List;

public class ContainerToken extends Token {
  private List<Token> innards;
  
  public ContainerToken(Token inner) {
    innards = new ArrayList<>();
    innards.add(inner);
  }
  
  public ContainerToken(List<Token> innards) {
    this.innards = innards;
  }

  public List<Token> getInnards() {
    return innards;
  }
  
  @Override
  public String toString() {
    return "ContainerToken [innards=" + innards + "]";
  }
}
