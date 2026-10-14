package me.frankv.staaaaaaaaaaaack.mixin;

import me.frankv.staaaaaaaaaaaack.PileState;
import me.frankv.staaaaaaaaaaaack.Stxck;
import me.frankv.staaaaaaaaaaaack.StxckAttachments;
import net.minecraft.network.syncher.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** A removed extraction handle stays removed: its reserve gets a fresh entity identity. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements PileState {
    @Shadow private int health;
    @Unique private ItemStack stxck$template = ItemStack.EMPTY;
    @Unique private boolean stxck$spilling;
    @Unique private boolean stxck$removing;
    @Unique private final long stxck$createdAt = level().getGameTime();
    protected ItemEntityMixin(EntityType<?> type, Level level) { super(type, level); }
    @Unique private ItemEntity stxck$self() { return (ItemEntity) (Object) this; }
    @Override public boolean stxck$syncAllowed() { return !isRemoved() && !stxck$removing; }
    @Override public long stxck$elapsedTicks() { return level().getGameTime() - stxck$createdAt; }
    @Override public boolean stxck$mergingAllowed() {
        return !stxck$spilling && (Stxck.getExtraItemCount(stxck$self()) == 0
                || ItemStack.isSameItemSameComponents(stxck$self().getItem(), stxck$template));
    }
    @Override public void stxck$remember(ItemStack stack) { stxck$template = stack.copyWithCount(1); }

    @Inject(method = "tick", at = @At("HEAD"))
    private void stxck$tick(CallbackInfo ci) {
        if (level().isClientSide() || isRemoved()) return;
        ItemStack visible = stxck$self().getItem();
        // In-place component changes must never transform the hidden reserve.
        if (!visible.isEmpty() && Stxck.getExtraItemCount(stxck$self()) > 0
                && !ItemStack.isSameItemSameComponents(visible, stxck$template) && !stxck$spill()) return;
        Stxck.refillItemStack(stxck$self());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void stxck$deferredReady(CallbackInfo ci) {
        int delay = Stxck.commonConfig.getMergeDelayTicks();
        if (!level().isClientSide() && tickCount == Math.max(1, delay)
                && Stxck.isMergable(stxck$self()) && !Stxck.isBlackListItem(stxck$self()))
            ((ItemEntityAccessor) this).stxck$mergeNeighbours();
    }

    @Inject(method = "isMergable", at = @At("HEAD"), cancellable = true)
    private void stxck$mergable(CallbackInfoReturnable<Boolean> cir) {
        if (!Stxck.isMergable(stxck$self())) { cir.setReturnValue(false); return; }
        if (!Stxck.isBlackListItem(stxck$self()))
            cir.setReturnValue(Stxck.getTotalCount(stxck$self()) < Stxck.commonConfig.getMaxSize());
    }

    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void stxck$merge(ItemEntity other, CallbackInfo ci) {
        if (!Stxck.isBlackListItem(stxck$self()) || Stxck.getExtraItemCount(stxck$self()) > 0
                || Stxck.getExtraItemCount(other) > 0) {
            Stxck.tryToMerge(stxck$self(), other);
            ci.cancel();
        }
    }

    @ModifyArg(method = "mergeWithNeighbours", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"), index = 1)
    private AABB stxck$radius(AABB box) {
        double h = Stxck.commonConfig.getMaxMergeDistanceHorizontal();
        return getBoundingBox().inflate(h, Stxck.commonConfig.getMaxMergeDistanceVertical(), h);
    }

    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void stxck$setItem(ItemStack stack, CallbackInfo ci) {
        if (isRemoved()) { ci.cancel(); return; }
        if (!level().isClientSide() && !stxck$spilling && !stack.isEmpty()
                && Stxck.getExtraItemCount(stxck$self()) > 0
                && !ItemStack.isSameItemSameComponents(stack, stxck$template) && !stxck$spill()) return;
        if (!stack.isEmpty()) stxck$remember(stack);
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void stxck$stalePlayer(net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        if (isRemoved()) ci.cancel();
    }

    @Inject(method = "playerTouch", at = @At("TAIL"))
    private void stxck$syncPickup(net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        if (!level().isClientSide() && !isRemoved() && !stxck$self().getItem().isEmpty())
            stxck$self().setItem(stxck$self().getItem().copy());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void stxck$save(ValueOutput output, CallbackInfo ci) {
        int extra = Stxck.getExtraItemCount(stxck$self());
        // Saving between extraction and the next tick must also preserve an empty front stack.
        if (extra > 0 && stxck$self().getItem().isEmpty()) {
            int front = Math.min(extra, stxck$template.getMaxStackSize());
            output.store("Item", ItemStack.CODEC, stxck$template.copyWithCount(front));
            extra -= front;
        }
        if (extra > 0) {
            output.putInt(Stxck.EXTRA_ITEM_COUNT_TAG, extra);
            // A mod may mutate the exposed stack's components immediately before a save.
            // Preserve the independent reserve until the next tick can separate it.
            output.store("StxckExtraItemTemplate", ItemStack.CODEC, stxck$template);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void stxck$load(ValueInput input, CallbackInfo ci) {
        Stxck.setExtraItemCount(stxck$self(), Math.max(0, input.getIntOr(Stxck.EXTRA_ITEM_COUNT_TAG, 0)));
        input.read("StxckExtraItemTemplate", ItemStack.CODEC).filter(stack -> !stack.isEmpty()).ifPresent(this::stxck$remember);
    }

    @Override public void remove(RemovalReason reason) {
        if (isRemoved()) return;
        // playerTouch sends take before discard; the client may already have removed this UUID.
        // Do not send reserve updates during removal, including the pre-super spill operation.
        stxck$removing = true;
        try {
            boolean spill = !level().isClientSide() && reason == RemovalReason.DISCARDED
                    && health > 0 && stxck$self().getAge() < stxck$self().lifespan;
            // The spill flag excludes this handle from join-time merging while the replacement is added.
            // If another mod vetoes the replacement, retain the reserve on an empty handle and retry
            // next tick. Never refill a handle inside an extraction/removal callback.
            if (spill && !stxck$spill()) {
                getEntityData().set(ItemEntityAccessor.stxck$itemData(), ItemStack.EMPTY);
                return;
            }
            super.remove(reason);
            if (reason.shouldDestroy()) {
                Stxck.setExtraItemCount(stxck$self(), 0);
                // Bypass setItem's stale-handle guard, detach stacks still referenced by playerTouch.
                getEntityData().set(ItemEntityAccessor.stxck$itemData(), ItemStack.EMPTY);
            }
        } finally { stxck$removing = false; }
    }

    @Unique private boolean stxck$spill() {
        int reserve = Stxck.getExtraItemCount(stxck$self());
        if (reserve == 0) return true;
        if (stxck$spilling) return false;
        stxck$spilling = true;
        try {
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registryAccess());
            saveWithoutId(output);
            int front = me.frankv.staaaaaaaaaaaack.PileCounts.split(reserve, stxck$template.getMaxStackSize()).visible();
            output.store("Item", ItemStack.CODEC, stxck$template.copyWithCount(front));
            output.putInt(Stxck.EXTRA_ITEM_COUNT_TAG, reserve - front);
            var tag = output.buildResult();
            tag.remove("UUID");
            var remainder = new ItemEntity(EntityType.ITEM, level());
            remainder.load(TagValueInput.create(ProblemReporter.DISCARDING, registryAccess(), tag));
            remainder.tickCount = tickCount;
            Stxck.setExtraItemCount(stxck$self(), 0);
            if (!level().addFreshEntity(remainder) && !remainder.isRemoved()) {
                stxck$self().setData(StxckAttachments.EXTRA, reserve);
                return false;
            }
            return true;
        } finally { stxck$spilling = false; }
    }
}
