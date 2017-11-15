package fun.w0w.revise;

public final class MemoryHandler {
  private final byte[] MEMORY;
  
  public MemoryHandler(int memory) {
    MEMORY = new byte[memory];
  }
  
  public int u1(int addr) {
    return MEMORY[wrapAddress(addr)] & 0xFF;
  }
  
  public void u1(int addr, int val) {
    MEMORY[wrapAddress(addr)] = (byte) (val & 0xFF);
  }
  
  public int u2(int addr) {
    return (u1(addr) << 8) | u1(addr + 1);
  }
  
  public void u2(int addr, int val) {
    u1(addr, val >>> 8);
    u1(addr + 1, val & 0xFF);
  }
  
  public void loadProgram(byte[] program) {
    System.arraycopy(program, 0, MEMORY, 0, program.length);
  }
  
  public void loadProgram(int[] program) {
    for (int i = 0; i < program.length; i++) {
      MEMORY[i] = (byte) program[i];
    }
  }
  
  public int wrapAddress(int addr) {
    if (0 < addr && addr < MEMORY.length) {
      return addr;
    }
    while (addr < 0) addr += MEMORY.length;
    addr %= MEMORY.length;
    return addr;
  }
}
