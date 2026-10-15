package me.frankv.staaaaaaaaaaaack.mixin;
import java.util.LinkedHashSet;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.util.ClassInstanceMultiMap;
import net.neoforged.neoforge.transfer.item.ItemResource;
import java.util.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.AbortableIterationConsumer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Maintained by the existing section lifecycle; no world scan, stale positions or chunk tickets. */
@Mixin(EntitySection.class)
public abstract class MixedEntitySectionMixin {
 @Shadow @Final private ClassInstanceMultiMap<EntityAccess> storage;
 @Unique private final LinkedHashSet<EntityAccess> stxck$nonItems=new LinkedHashSet<>();
 @Unique private final Map<ItemResource,LinkedHashSet<ItemEntity>> stxck$itemKinds=new HashMap<>();
 @Unique private long stxck$keyTick=Long.MIN_VALUE;
 @Unique private final Map<ItemEntity,ItemResource> stxck$keys=new IdentityHashMap<>();
 @Unique private void stxck$indexItem(ItemEntity item){if(!item.getItem().isEmpty()&&!stxck$keys.containsKey(item)){var key=ItemResource.of(item.getItem());stxck$keys.put(item,key);stxck$itemKinds.computeIfAbsent(key,ignored->new LinkedHashSet<>()).add(item);}}
 @Inject(method="add",at=@At("TAIL")) private void stxck$add(EntityAccess entity,CallbackInfo ci){if(entity.getClass()!=ItemEntity.class&&!(entity instanceof MixedPileEntity))stxck$nonItems.add(entity);if(entity instanceof ItemEntity item){if(stxck$keyTick==item.level().getGameTime())stxck$indexItem(item);}}
 @Inject(method="remove",at=@At("RETURN")) private void stxck$remove(EntityAccess entity,CallbackInfoReturnable<Boolean> cir){if(cir.getReturnValue()){stxck$nonItems.remove(entity);var key=stxck$keys.remove(entity);if(key!=null){var group=stxck$itemKinds.get(key);if(group!=null){group.remove(entity);if(group.isEmpty())stxck$itemKinds.remove(key);}}}}
 @Inject(method="getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)Lnet/minecraft/util/AbortableIterationConsumer$Continuation;",at=@At("HEAD"),cancellable=true)
 private <U extends EntityAccess> void stxck$query(EntityTypeTest<EntityAccess,U> type,AABB box,AbortableIterationConsumer<? super U> consumer,CallbackInfoReturnable<AbortableIterationConsumer.Continuation> cir){
  if((Object)type instanceof MixedSameQuery query){
   if(stxck$keyTick!=query.tick()){stxck$itemKinds.clear();stxck$keys.clear();stxck$keyTick=query.tick();for(var item:storage.find(ItemEntity.class))stxck$indexItem(item);}
   var candidates=stxck$itemKinds.get(query.kind());if(candidates!=null)for(var item:candidates){U candidate=type.tryCast(item);if(candidate!=null&&item.getBoundingBox().intersects(box)&&consumer.accept(candidate).shouldAbort()){cir.setReturnValue(AbortableIterationConsumer.Continuation.ABORT);return;}}
   cir.setReturnValue(AbortableIterationConsumer.Continuation.CONTINUE);return;
  }
  if((Object)type!=MixedCollisionQuery.INSTANCE)return;
  for(var entity:stxck$nonItems){U candidate=type.tryCast(entity);if(candidate!=null&&entity.getBoundingBox().intersects(box)&&consumer.accept(candidate).shouldAbort()){cir.setReturnValue(AbortableIterationConsumer.Continuation.ABORT);return;}}
  cir.setReturnValue(AbortableIterationConsumer.Continuation.CONTINUE);
 }
}
