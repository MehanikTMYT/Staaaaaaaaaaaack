package me.frankv.staaaaaaaaaaaack;

import net.minecraft.world.item.ItemStack;

/** Private reserve template, independent of mutable stacks lent to automation. */
public interface PileState {
    boolean stxck$mergingAllowed();
    boolean stxck$syncAllowed();
    long stxck$elapsedTicks();
    void stxck$remember(ItemStack stack);
}
