package fun.w0w.revise;

@FunctionalInterface
public interface InsnHandler {
  void exec(MemoryHandler m);
}
