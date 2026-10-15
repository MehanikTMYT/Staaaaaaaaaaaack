package me.frankv.staaaaaaaaaaaack.mixin;
import java.util.List;
import java.util.function.Predicate;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(EntityGetter.class)
public interface MixedEntityCollisionMixin {
 @WrapOperation(method="getEntityCollisions",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/EntityGetter;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
 default List<Entity> stxck$collisionCandidates(EntityGetter getter,Entity source,AABB box,Predicate<Entity> filter,Operation<List<Entity>> original){
  if(getter instanceof ServerLevel level&&source!=null&&source.getClass()==ItemEntity.class&&MixedConfig.enabled.get())return level.getEntities(MixedCollisionQuery.INSTANCE,box,e->e!=source&&(!(e instanceof net.neoforged.neoforge.entity.PartEntity<?> part)||part.getParent()!=source)&&filter.test(e));
  return original.call(getter,source,box,filter);
 }
}
