package me.frankv.staaaaaaaaaaaack.mixed;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.neoforge.transfer.item.ItemResource;
/** Tick-local candidate key; the caller still validates live components and vanilla merge rules. */
public record MixedSameQuery(ItemResource kind,long tick) implements EntityTypeTest<Entity,ItemEntity> {
 public ItemEntity tryCast(Entity entity){return entity instanceof ItemEntity item?item:null;}
 public Class<? extends Entity> getBaseClass(){return ItemEntity.class;}
}
