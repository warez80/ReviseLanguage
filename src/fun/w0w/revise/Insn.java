package fun.w0w.revise;

public class Insn implements Comparable<Insn> {
  private final int opcode;
  private final String name;
  private final int width;
  private final int cycles;
  private final InsnHandler handler;
  private final boolean isjumpinsn;
  
  public Insn(int opcode, String name, int width, int cycles, InsnHandler handler) {
    this(opcode, name, width, cycles, false, handler);
  }
  
  public Insn(int opcode, String name, int width, int cycles, boolean jmp, InsnHandler handler) {
    this.opcode = opcode;
    this.name = name;
    this.width = width;
    this.cycles = cycles;
    this.handler = handler;
    this.isjumpinsn = jmp;
  }

  public final int getOpcode() {
    return opcode;
  }

  public final int getWidth() {
    return width;
  }

  public final int getCycles() {
    return cycles;
  }

  public final void executeInsn(MemoryHandler m) {
    handler.exec(m);
  }
  
  public final String getName() {
    return name;
  }
  
  public final boolean isJumpInsn() {
    return isjumpinsn;
  }

  @Override
  public final int compareTo(Insn o) {
    return opcode - o.opcode;
  }
}
