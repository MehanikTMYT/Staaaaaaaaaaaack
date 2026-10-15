package me.frankv.staaaaaaaaaaaack.mixed;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
/** A marker query using vanilla section boundaries, retaining every non-vanilla-item candidate. */
public enum MixedCollisionQuery implements EntityTypeTest<Entity,Entity> {
 INSTANCE;
 public Entity tryCast(Entity entity){return entity.getClass()==ItemEntity.class||entity instanceof MixedPileEntity?null:entity;}
 public Class<? extends Entity> getBaseClass(){return Entity.class;}
}
