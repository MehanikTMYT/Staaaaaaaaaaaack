package me.frankv.staaaaaaaaaaaack.mixed;
import java.util.WeakHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
/** Bounds repeated neighbour searches in dense mixed storms; never delays pickup. */
public final class MixedMergeBudget {
 private static final WeakHashMap<ServerLevel,Budget> LEVELS=new WeakHashMap<>();
 private static final class Budget {long tick=Long.MIN_VALUE;final Long2IntOpenHashMap searches=new Long2IntOpenHashMap();}
 public static boolean exhausted(ItemEntity item){
  if(!(item.level() instanceof ServerLevel level)||!MixedConfig.enabled.get()||item.getType()!=EntityType.ITEM||item.getClass()!=ItemEntity.class||((MixedItemState)item).mixed$cooldown()!=0)return false;
  var budget=LEVELS.computeIfAbsent(level,ignored->new Budget());long tick=level.getGameTime();
  if(budget.tick!=tick){budget.tick=tick;budget.searches.clear();}
  return budget.searches.addTo(item.chunkPosition().pack(),1)>=256;
 }
 private MixedMergeBudget(){}
}
