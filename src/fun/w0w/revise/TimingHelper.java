package fun.w0w.revise;

public final class TimingHelper {
  // gameboy clock cycle
  private static final double CYCLE_NANO = 238.418579;
  private static long startSleep;
  
  public static void startSleepTime() {
    startSleep = System.nanoTime();
  }
  
  public static void cycle(int cycles) {
    long nano = (long) (cycles * CYCLE_NANO);
    while (System.nanoTime() - startSleep < nano);
  }
}