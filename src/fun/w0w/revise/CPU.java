package fun.w0w.revise;

import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("unused")
public final class CPU {
  public static final Register8 
  A = new Register8(),
  B = new Register8(),
  C = new Register8(),
  D = new Register8(),
  E = new Register8(),
  F = new Register8(),
  H = new Register8(),
  L = new Register8();
  public static final Register16
  AF = new Register16Combine(A, F),
  BC = new Register16Combine(B, C),
  DE = new Register16Combine(D, E),
  HL = new Register16Combine(H, L),
  PC = new Register16(),
  SP = new Register16();
  
  public static boolean Z() {
    return flagState(0b1000_0000);
  }
  public static void Z(boolean f) {
    flagState(0b1000_0000, f);
  }
  public static boolean N() {
    return flagState(0b0100_0000);
  }
  public static void N(boolean f) {
    flagState(0b0100_0000, f);
  }
  public static boolean H() {
    return flagState(0b0010_0000);
  }
  public static void H(boolean f) {
    flagState(0b0010_0000, f);
  }
  public static boolean C() {
    return flagState(0b0001_0000);
  }
  public static void C(boolean f) {
    flagState(0b0001_0000, f);
  }
  private static boolean flagState(int mask) {
    return (F.getVal() & mask)!=0;
  }
  private static void flagState(int mask, boolean f) {
    int regstate = F.getVal();
    if (f) {
      F.setVal(regstate | mask);
    } else {
      F.setVal(regstate &~mask);
    }
  }
  
  private static boolean backflow;
  
  public static void incPC(int n) {
    PC.setVal(PC.getVal() + n);
  }
  
  private static void ADD(Register8 r1, int d8) {
    int a = r1.getVal();
    int raw = a + d8;
    int res = raw & 0xFF;
    r1.setVal(res);
    Z(res == 0);
    N(false);
    H((((a&0xF)+(d8&0xF))&0x10) != 0);
    C(raw != res);
  }
  
  private static void SUB(int d8) {
    int a = A.getVal();
    int raw = a - d8;
    int res = raw & 0xFF;
    A.setVal(res);
    Z(res == 0);
    N(true);
    H((((a&0xF)-(d8&0xF))&0x10) != 0);
    C(raw != res);
  }
  
  private static void PUSH_8(MemoryHandler m, int d8) {
    int sp = SP.getVal();
    m.u1(sp, d8);
    SP.setVal(sp - 1);
  }
  private static void PUSH_16(MemoryHandler m, int d16) {
    int sp = SP.getVal();
    m.u2(sp, d16);
    SP.setVal(sp - 2);
  }
  private static void PUSH(MemoryHandler m, Register8 r) {
    int sp = SP.getVal();
    m.u1(sp, r.getVal());
    SP.setVal(sp - 1);
  }
  private static void PUSH(MemoryHandler m, Register16 r) {
    int sp = SP.getVal();
    m.u2(sp, r.getVal());
    SP.setVal(sp - 2);
  }
  private static void POP(MemoryHandler m, Register8 r) {
    int sp = SP.getVal();
    r.setVal(m.u1(sp + 2));
  }
  
  
  public static final Insn[] INSN_TABLE = new Insn[0xFF];
  private static void table(Insn n) {
    INSN_TABLE[n.getOpcode()] = n;
  }
  private static ThreadLocalRandom rng = ThreadLocalRandom.current();
  static {//TODO: implement an entire REDCODE mode.
    table(new Insn(0x00, "NOP", 1, 4, (m) -> {}));
    table(new Insn(0x01, "ADD d8", 2, 8, (m) -> ADD(A, op8(m))));
    table(new Insn(0x02, "SUB d8", 2, 8, (m) -> SUB(op8(m))));
    table(new Insn(0x03, "LD (a16),A", 3, 4, (m) -> m.u1(op16(m), A.getVal())));
    table(new Insn(0x04, "JFR d8", 2, 8, true, (m) -> PC.setVal(PC.getVal() + op8(m))));
    table(new Insn(0x05, "PUSH A", 1, 4, (m) -> PUSH(m, A)));
    table(new Insn(0x06, "POP A", 1, 4, (m) -> POP(m, A)));
    table(new Insn(0x07, "LD SP,HL", 1, 4, (m) -> SP.setVal(HL.getVal())));
    table(new Insn(0x08, "JBR d8", 2, 8, true, (m) -> PC.setVal(PC.getVal() - op8(m))));
    table(new Insn(0x10, "JR d8", 2, 8, true, (m) -> PC.setVal(PC.getVal() + (byte) op8(m))));
    table(new Insn(0x1F, "KIL", 2, 4, (m) -> System.exit(op8(m))));
    table(new Insn(0xFD, "RNG (a16)", 3, 8, (m) -> m.u1(op16(m), rng.nextInt(0x100))));
    table(new Insn(0xFE, "TBF", 1, 1, (m) -> backflow = !backflow));
  }
  
  public static int op8(MemoryHandler m) {
    if (backflow) {
      return m.u1(PC.getVal() - 1);
    } else {
      return m.u1(PC.getVal() + 1);
    }
  }
  public static int op16(MemoryHandler m) {
    if (backflow) {
      int pc = PC.getVal();
      return (m.u1(pc - 1) << 8) | m.u1(pc);
    } else {
      return m.u2(PC.getVal() + 1);
    }
  }
  
  public static void resumeExecution(MemoryHandler m) {
    int pc = PC.getVal();
    int opcode = m.u1(pc);
    
    if (backflow) {
      CPU.incPC(-handleOpcode(m, opcode));
    } else {
      CPU.incPC(handleOpcode(m, opcode));
    }
    CPU.PC.setVal(m.wrapAddress(CPU.PC.getVal()));
  }
  
  public static int handleOpcode(MemoryHandler m, int opcode) {
    TimingHelper.startSleepTime();
    Insn x = INSN_TABLE[opcode];
    int c = x.getCycles();
    x.executeInsn(m);
    TimingHelper.cycle(c);
    return x.isJumpInsn()? 0 : x.getWidth();
  }
}
