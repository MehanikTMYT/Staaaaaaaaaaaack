package me.frankv.staaaaaaaaaaaack.mixed;
public interface MixedItemState {
 void mixed$joined();
 boolean mixed$queued();
 void mixed$queued(boolean value);
 long mixed$nextAttempt();
 void mixed$nextAttempt(long tick);
 long mixed$stableSince();
 boolean mixed$stableNow();
 int mixed$cooldown();
 void mixed$cooldown(int ticks);
}
