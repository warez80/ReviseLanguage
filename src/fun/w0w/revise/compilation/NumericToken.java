package fun.w0w.revise.compilation;

import java.math.BigInteger;

public class NumericToken extends Token {
  private BigInteger value;
  private String token;
  private NumSize size;
  private static enum NumSize {
    BYTE, SHORT, INT, LONG, LONGER // raise your longers
  }
  
  public NumericToken(String tokendata, int radix) {
    token = tokendata;
    tokendata = tokendata.replace("_", "");
    // parse into number
    if (radix == 16) {
      tokendata = tokendata.replace("$","").replace("h","").replace("H","");
    }
    while (tokendata.charAt(0) == 0 && tokendata.length() > 1) { // TODO: this can be made faster
      tokendata = tokendata.substring(1);
    }
    value = new BigInteger(tokendata, radix); //FIXME: does this make them signed...?
    try {
      value.longValueExact();
      try {
        value.intValueExact();
        try {
          value.shortValueExact();
          try {
            value.byteValueExact();
            size = NumSize.BYTE;
          } catch (ArithmeticException e) {
            size = NumSize.SHORT;
          }
        } catch (ArithmeticException e) {
          size = NumSize.INT;
        }
      } catch (ArithmeticException e) {
        size = NumSize.LONG;
      }
    } catch (ArithmeticException e) {
      size = NumSize.LONGER;
    }
  }
  
  public boolean isByteSeries() {
    return size == NumSize.LONGER;
  }
  
  public boolean isLong() {
    return size == NumSize.LONG;
  }
  
  public boolean isInt() {
    return size == NumSize.INT;
  }
  
  public boolean isShort() {
    return size == NumSize.SHORT;
  }
  
  public boolean isByte() {
    return size == NumSize.BYTE;
  }
  
  public int intValue() {
    return value.intValue();
  }
  
  public byte byteValue() {
    return value.byteValue();
  }
  
  public short shortValue() {
    return value.shortValue();
  }
  
  public long longValue() {
    return value.longValue();
  }
  
  public byte[] asByteSeries() {
    return value.toByteArray();
  }
  
  public byte[] asByteSeries(int length) {
    byte[] x = asByteSeries();
    if (x.length < length) {
      int padding = length - x.length;
      byte[] a = new byte[length];
      System.arraycopy(x, 0, a, padding, x.length);
      return a;
    } else if (x.length > length) {
      byte[] a = new byte[length];
      System.arraycopy(x, 0, a, 0, length);
      return a;
    }
    return x;
  }

  @Override
  public String toString() {
    return "NumericToken [token=" + token + "]";
  }
}
