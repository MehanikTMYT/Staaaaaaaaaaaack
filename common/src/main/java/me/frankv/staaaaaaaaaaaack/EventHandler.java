package me.frankv.staaaaaaaaaaaack;

import me.frankv.staaaaaaaaaaaack.config.StxckCommonConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

public class EventHandler {



    public static void onEntityCreate(Entity entity, Runnable eventCanceller) {
        if (entity.level().isClientSide()) return;
        // Callers finish configuring new drops AFTER addFreshEntity (e.g. makeFakeItem,
        // pickup ownership, components). Never merge those before that call returns.
        // The first entity tick performs the local search once initialization is complete.
        if (entity.tickCount == 0) return;
        if (!(entity instanceof ItemEntity itemEntity && Stxck.isMergable(itemEntity))
                || Stxck.isBlackListItem(itemEntity)) return;

        var config = Stxck.commonConfig;
        var h = config.getMaxMergeDistanceHorizontal();
        var v = config.getMaxMergeDistanceVertical();

        var nearByEntities = itemEntity.level().getEntitiesOfClass(
                ItemEntity.class,
                itemEntity.getBoundingBox().inflate(h, v, h),
                (she) -> itemEntity != she && Stxck.isMergable(she)
        );

        for(var nearByEntity : nearByEntities) {
            Stxck.tryToMerge(nearByEntity, itemEntity, true);
            if (itemEntity.isRemoved()) {
                eventCanceller.run();
                break;
            }
        }
    }
    
}
