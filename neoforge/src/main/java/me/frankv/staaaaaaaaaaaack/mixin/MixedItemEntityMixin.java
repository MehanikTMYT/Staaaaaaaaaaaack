package me.frankv.staaaaaaaaaaaack.mixin;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
public abstract class MixedItemEntityMixin implements MixedItemState {
 @Unique private double mixed$x,mixed$y,mixed$z;
 @Unique private long mixed$since;
 @Unique private int mixed$cooldown;
 @Unique private boolean mixed$queued;
 @Unique private long mixed$nextAttempt;
 public boolean mixed$queued(){return mixed$queued;}
 public void mixed$queued(boolean value){mixed$queued=value;}
 public long mixed$nextAttempt(){return mixed$nextAttempt;}
 public void mixed$nextAttempt(long tick){mixed$nextAttempt=tick;}
 public void mixed$joined(){var e=(ItemEntity)(Object)this;mixed$x=e.getX();mixed$y=e.getY();mixed$z=e.getZ();mixed$since=e.level().getGameTime();}
 public long mixed$stableSince(){return mixed$since;}
 public int mixed$cooldown(){return mixed$cooldown;}
 public void mixed$cooldown(int ticks){mixed$cooldown=ticks;}
 // Grounded vanilla items accumulate vertical gravity between movement updates.
 // Position and horizontal motion determine rest; floating items still check all axes.
 public boolean mixed$stableNow(){var e=(ItemEntity)(Object)this;return !MixedConfig.requireStablePosition.get()||((e.onGround()?e.getDeltaMovement().horizontalDistanceSqr()<0.001:e.isNoGravity()&&e.getDeltaMovement().lengthSqr()<0.001)&&e.distanceToSqr(mixed$x,mixed$y,mixed$z)<0.001);}
 @WrapOperation(method="mergeWithNeighbours",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
 private List<ItemEntity> mixed$matchingCandidates(Level level,Class<ItemEntity> type,AABB area,Predicate<ItemEntity> predicate,Operation<List<ItemEntity>> original){
  var self=(ItemEntity)(Object)this;
  if(level.isClientSide()||!MixedConfig.enabled.get()||self.getClass()!=ItemEntity.class||mixed$cooldown!=0)return original.call(level,type,area,predicate);
  var stack=self.getItem();
  Predicate<ItemEntity> matching=item->ItemStack.isSameItemSameComponents(stack,item.getItem())&&predicate.test(item);
  return level.getEntities(new MixedSameQuery(net.neoforged.neoforge.transfer.item.ItemResource.of(stack),level.getGameTime()),area,matching);
 }
 @Inject(method="mergeWithNeighbours",at=@At("HEAD"),cancellable=true) private void mixed$budget(CallbackInfo ci){if(MixedMergeBudget.exhausted((ItemEntity)(Object)this))ci.cancel();}
 @Inject(method="tick",at=@At("TAIL")) private void mixed$tick(CallbackInfo ci){var e=(ItemEntity)(Object)this;if(e.level().isClientSide())return;if(mixed$cooldown>0)mixed$cooldown--;if(!mixed$stableNow())mixed$joined();MixedAggregation.onItemTick(e);}
 @Inject(method="addAdditionalSaveData",at=@At("TAIL")) private void mixed$save(ValueOutput out,CallbackInfo ci){out.putInt("StxckMixedCooldown",mixed$cooldown);}
 @Inject(method="readAdditionalSaveData",at=@At("TAIL")) private void mixed$load(ValueInput in,CallbackInfo ci){mixed$cooldown=Math.max(0,in.getIntOr("StxckMixedCooldown",0));}
}
