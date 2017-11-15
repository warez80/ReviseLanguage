package fun.w0w.revise.compilation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeScanner {
  private String source;
  private String currentChomp;
  
  private Pattern lastPattern;
  
  public CodeScanner(String source) {
    this.source = currentChomp = source;
  }
  
  public String getSource() {
    return source;
  }
  
  public boolean isNext(Pattern pattern) {
    lastPattern = pattern;
    Matcher matcher = pattern.matcher(currentChomp);
    if (matcher.find()) {
      return matcher.start() == 0;
    }
    return false;
  }
  
  private String chompNext(Matcher matcher) {
    if (matcher.find()) {
      currentChomp = currentChomp.substring(matcher.end());
      return matcher.group();
    }
    return null;
  }
  
  public String chompNext(Pattern pattern) {
    return chompNext(pattern.matcher(currentChomp));
  }
  
  public String chomp() {
    return chompNext(lastPattern);
  }
  
  public boolean reachedEnd() {
    return currentChomp.length() == 0;
  }
}