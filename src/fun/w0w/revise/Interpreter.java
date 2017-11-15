package fun.w0w.revise;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fun.w0w.revise.compilation.ArgSeparatorToken;
import fun.w0w.revise.compilation.CodeScanner;
import fun.w0w.revise.compilation.ContainerToken;
import fun.w0w.revise.compilation.DeclareBytesToken;
import fun.w0w.revise.compilation.InsnToken;
import fun.w0w.revise.compilation.LineTerminatorToken;
import fun.w0w.revise.compilation.NumericToken;
import fun.w0w.revise.compilation.RegisterToken;
import fun.w0w.revise.compilation.Token;

public class Interpreter {
  private static final boolean DEBUG = false;
  
  /*
   * TODO: 
   * - make it optionally take 2 operands
   * - add a stack
   * - add an instruction to toggle between using the stack vs. operand storage
   */
  public static void main(String[]args) {
    byte[] compiled = asm(""
        + "#define TEST 1  \n"
        + "     NOP        \n" // placeholder insn
        + "     ADD $1F    \n" // set A to $1F
        + "     LD ($01),A \n" // change the NOP insn to a KIL insn
        + "     JR 4       \n" // skip over the bytes
        + "     db 7,4     \n" // is JR 7, but backwards :)
        + "     TBF        "); // make control flow go backwards
    
    System.out.println("Source code: ");
    for (int i = 0; i < compiled.length; i++) {
      int b = compiled[i] & 0xFF;
      System.out.print("0x" + Integer.toHexString(b).toUpperCase() + ", ");
    }
    
    Interpreter x = new Interpreter(0);
    
    x.MEMORY.loadProgram(compiled);
    
//    x.MEMORY.loadProgram(new int[] {
//        0x00, // NOP
//        0x01, 0x1F, // ADD 0x1F; PUT 1F IN A
//        0x03, 0x00, 0x01, // LD ($01),A; PUT 1F IN 0x01
//        0x04, 0x04, // JR :X
//        0x07, 0x04,
//        0xFE, // :X
//    });
    
    while (true) {
      x.continueEnvironment();
    }
  }
  
  private final MemoryHandler MEMORY;
  
  public Interpreter(int begin) {
    this(begin, 64 * 1024); // 64K memory; generous, I know
  }
  
  public Interpreter(int begin, int memorysize) {
    CPU.PC.setVal(begin);
    MEMORY = new MemoryHandler(memorysize);
  }
  
  public void continueEnvironment() {
    CPU.resumeExecution(MEMORY);
  }
  
  
  /*
   * TODO: add an "OriginToken"
   */
  public static byte[] asm(String assembler) {
    // first, we'll run it through the C preprocessor to let us do cool stuff
    try {
      Process process = Runtime.getRuntime().exec("cpp -P -x assembler-with-cpp");
      OutputStream stdin = process.getOutputStream();
      stdin.write(assembler.getBytes());
      stdin.flush();
      stdin.close(); // signal EOF
      process.waitFor(); // let the preprocessor do its thing
      
      // read from the stdout
      // lol "Stupid Scanner tricks" http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
      try (@SuppressWarnings("resource") // stupid eclipse, it does close. I checked the bytecode output
      Scanner s = new Scanner(process.getInputStream()).useDelimiter("\\A")) {
        if (s.hasNext()) {
          assembler = s.next();
        }
      }
      
      // ta-da, preprocessed
    } catch (IOException e) {
      System.err.println("C preprocessor not found!");
    } catch (InterruptedException e) {
      e.printStackTrace(); // cpp process was terminated... wtf?
    }
    
    // now that it's pre-processed (hopefully), we can start lexing
    final Pattern whitespace = Pattern.compile("\\s+");
    final Pattern hexNumber = Pattern.compile("\\$(\\d|[A-F]|[a-f])+|(\\d|[A-F]|[a-f])+[Hh]");
    final Pattern decNumber = Pattern.compile("\\d+");
    final Pattern insnPattern = Pattern.compile("[a-zA-Z]+");
    final Pattern declareBytesPattern = Pattern.compile("\\.*[Dd][BbWw]");
    final Pattern argSeparator = Pattern.compile(",");
    final Pattern registerPattern = Pattern.compile("[Aa][Ff]|[Bb][Cc]|[Dd][Ee]|[Hh][Ll]|[Pp][Cc]|[Ss][Pp]|[AaBbCcDdEeFfHhLl]");
    boolean foundCurrentInsnToken = false;
    final Pattern lineDelimiter = Pattern.compile("[\\n|\\r]+");
    final Pattern containerStartToken = Pattern.compile("\\(");
    final Pattern containerEndToken = Pattern.compile("\\)");
    CodeScanner scanner = new CodeScanner(assembler);
    List<Token> lexTokens = new ArrayList<>();
    Stack<List<Token>> tokenStack = new Stack<>();
    while (!scanner.reachedEnd()) {
      if (scanner.isNext(lineDelimiter)) {
        scanner.chomp();
        foundCurrentInsnToken = false;
        lexTokens.add(new LineTerminatorToken());
        continue;
      }
      if (scanner.isNext(whitespace)) {
        scanner.chomp();
        continue;
      }
      if (scanner.isNext(hexNumber)) {
        lexTokens.add(new NumericToken(scanner.chomp(), 16));
        continue;
      }
      if (scanner.isNext(decNumber)) {
        String token = scanner.chomp();
        if (token.charAt(0) == '0') { // octal
          lexTokens.add(new NumericToken(token, 8));
        } else {
          lexTokens.add(new NumericToken(token, 10));
        }
        continue;
      }
      if (scanner.isNext(containerStartToken)) {
        scanner.chomp();
        tokenStack.push(lexTokens);
        lexTokens = new ArrayList<>();
        continue;
      }
      if (scanner.isNext(containerEndToken)) {
        scanner.chomp();
        List<Token> lowerTokens = tokenStack.pop();
        lowerTokens.add(new ContainerToken(lexTokens));
        lexTokens = lowerTokens;
        continue;
      }
      if (scanner.isNext(argSeparator)) {
        scanner.chomp();
        lexTokens.add(new ArgSeparatorToken());
        continue;
      }
      if (!foundCurrentInsnToken) {
        if (scanner.isNext(declareBytesPattern)) {
          scanner.chomp();
          lexTokens.add(new DeclareBytesToken());
          foundCurrentInsnToken = true; // it's a psuedo-instruction
          continue;
        }
        if (scanner.isNext(insnPattern)) {
          lexTokens.add(new InsnToken(scanner.chomp()));
          foundCurrentInsnToken = true;
          continue;
        }
      }
      
      if (scanner.isNext(registerPattern)) {
        lexTokens.add(new RegisterToken(scanner.chomp()));
        continue;
      }
    }
    if (!tokenStack.isEmpty()) {
      throw new IllegalStateException("Unterminated container! (parentheses might be mismatched)");
    }
    
    if (DEBUG) {
      String[] x = lexTokens.toString().split(", ");
      for (String s : x) {
        System.out.println(s);
      }
    }
    
    // lexical analysis is now finished, now onto the compilation stage
    
    // compile a code stream
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    Insn[] itable = CPU.INSN_TABLE;
    StringBuilder searchBuilder = new StringBuilder();
    List<Token> builtTokens = new ArrayList<>();
    for (int i = 0, s = lexTokens.size(); i < s; i++) {
      Token token = lexTokens.get(i);
      
      if (token instanceof LineTerminatorToken) {
        if (builtTokens.isEmpty()) { // in case we just get a couple useless lines.
          continue;
        }
        
        if (builtTokens.get(0) instanceof DeclareBytesToken) { // implement this pseudo-instruction
          int built_size = builtTokens.size();
          if (built_size > 1) {
            for (int j = 1; j < built_size; j++) {
              Token t = builtTokens.get(j);
              if (t instanceof NumericToken) {
                try { // this try block shouldn't be necessary, 
                      // so writing a handler shouldn't be either
                  baos.write(((NumericToken) t).asByteSeries());
                } catch (IOException e) { }
              }
            }
          }
          searchBuilder = new StringBuilder();
          builtTokens.clear();
          continue;
        }
        
        String searchRegex = searchBuilder.toString();
        
        Insn targetInsn = null;
        for (Insn insn : itable) {
          if (insn != null && insn.getName().matches(searchRegex)) {
            targetInsn = insn;
            break;
          }
        }
        
        if (targetInsn == null ||
            targetInsn.getOpcode() > 0xFF) { // unsupported and won't ever happen 
                                             // unless I expand the INSN_TABLE capacity.
          throw new IllegalArgumentException("Invalid instruction!");
        }
        
        baos.write(targetInsn.getOpcode());
        int written = 1;
        String expectancy = targetInsn.getName();
        Matcher m = Pattern.compile("r8|[da](8|16)").matcher(expectancy); // matches places where numbers are needed.
        int st = 0;
        while (m.find()) {
          String a = m.group();
          int wrsize;
          if (a.charAt(a.length() - 1) == '8') {
            wrsize = 1;
          } else { // is 16
            wrsize = 2;
          }
          for (int j = st, built_size = builtTokens.size(); j < built_size; j++) {
            Token t = builtTokens.get(j);
            if (_writeToken(baos, t, wrsize)) {
              st = j + 1;
              written += wrsize;
              break;
            }
          }
        }
        if (written != targetInsn.getWidth()) {
          throw new IllegalStateException(
              "Looks like it's finally time to update the helper function... ugh");
        }
        
        searchBuilder = new StringBuilder();
        builtTokens.clear();
        continue;
      } else {
        builtTokens.add(token);
      }
      
      if (token instanceof InsnToken) {
        searchBuilder.append(Pattern.compile(((InsnToken) token).getInsnName(), Pattern.LITERAL).pattern());
        searchBuilder.append("\\s*");
      } else if (token instanceof NumericToken) {
        searchBuilder.append("([rda](8|16)|[\\da-fA-FHh$]+)\\s*");
      } else if (token instanceof ArgSeparatorToken) {
        searchBuilder.append(",\\s*");
      } else if (token instanceof ContainerToken) {
        searchBuilder.append("\\([^)]+\\)\\s*");
      } else if (token instanceof RegisterToken) {
        searchBuilder.append(Pattern.compile(((RegisterToken) token).getRegister(), Pattern.LITERAL).pattern());
        searchBuilder.append("\\s*");
      } else if (token instanceof DeclareBytesToken) {//ignore
      } else {
        throw new IllegalStateException("Unhandled token type '" + token + "'!");
      }
    }
    
    return baos.toByteArray();
  }
  
  /**
   * helper function for asm()
   * @return whether the outputstream was written to
   */
  private static boolean _writeToken(ByteArrayOutputStream baos, Token token, int writesize) {
    // FIXME: this isn't perfect due to writesize and how it works with ContainerTokens.
    //        it might need to be fixed later, depends on how the language evolves.
    if (token instanceof NumericToken) {
      try {
        baos.write(((NumericToken) token).asByteSeries(writesize));
        return true;
      } catch (IOException e) {}
    } else if (token instanceof ContainerToken) {
      List<Token> innards = ((ContainerToken) token).getInnards();
      boolean wrote = false;
      for (Token inner : innards) {
        if (_writeToken(baos, inner, writesize)) {
          wrote = true;
        }
      }
      return wrote;
    }
    return false;
  }
}
