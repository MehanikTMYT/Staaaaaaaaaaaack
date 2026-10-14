package me.frankv.staaaaaaaaaaaack;

/** Overflow-safe boundary between a normal ItemStack and its serialized int reserve. */
public final class PileCounts {
    private PileCounts() {}
    public record Split(int visible, int reserve) {
        public long total() { return (long) visible + reserve; }
    }
    public static Split split(long total, int stackLimit) {
        if (total < 0 || stackLimit < 1) throw new IllegalArgumentException("Invalid pile count/limit");
        int front = (int) Math.min(total, stackLimit);
        return new Split(front, Math.toIntExact(total - front));
    }
}
