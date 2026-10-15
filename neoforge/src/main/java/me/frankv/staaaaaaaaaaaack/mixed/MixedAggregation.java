package me.frankv.staaaaaaaaaaaack.mixed;
import me.frankv.staaaaaaaaaaaack.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
/** A bounded nearby query only after stability delay; removed sources cannot be extracted twice. */
public final class MixedAggregation {
 public static boolean eligible(ItemEntity item){
  if(!MixedConfig.enabled.get()||item.getType()!=EntityType.ITEM||item.getClass()!=ItemEntity.class||!item.isAlive()||item.getItem().isEmpty()||!((PileState)item).stxck$mergingAllowed()||Stxck.isFiltered(item))return false;
  var access=(me.frankv.staaaaaaaaaaaack.mixin.ItemEntityAccessor)item;
  if(access.getPickupDelay()==32767||access.getAge()==-32768||access.getAge()>=item.lifespan)return false;
  if(Stxck.getTotalCount(item)>MixedConfig.maxTotalItems.get())return false;
  var state=(MixedItemState)item;
  return state.mixed$cooldown()==0&&state.mixed$stableNow()&&item.level().getGameTime()-state.mixed$stableSince()>=MixedConfig.delayTicks.get()
   &&(!MixedConfig.excludeAutomation.get()||!AutomationPolicy.nearAutomation(item));
 }
 public static void onItemTick(ItemEntity source){
  if(source.level().isClientSide()||!eligible(source)||!(source.level() instanceof ServerLevel level))return;
  double radius=MixedConfig.radius.get();var area=source.getBoundingBox().inflate(radius);
  for(var pile:level.getEntitiesOfClass(MixedPileEntity.class,area,e->e.isAlive()))if(pile.absorb(source))return;
  var candidates=level.getEntitiesOfClass(ItemEntity.class,area,MixedAggregation::eligible);
  // Keep the efficient original implementation for a single item identity.
  boolean different=candidates.stream().anyMatch(e->!ItemStack.isSameItemSameComponents(source.getItem(),e.getItem()));
  if(!different)return;
  var pile=new MixedPileEntity(MixedModule.TYPE.get(),level);pile.setPos(source.getX(),source.getY(),source.getZ());
  if(!level.addFreshEntity(pile))return;
  pile.absorb(source);
  for(var item:candidates){
   if(item!=source&&!item.isRemoved())pile.absorb(item);
   if(pile.entryCount()>=MixedConfig.maxEntries.get()||pile.total()>=MixedConfig.maxTotalItems.get())break;
  }
  if(pile.total()==0)pile.discard();
 }
 public static void consume(ItemEntity source){Stxck.setExtraItemCount(source,0);source.setItem(ItemStack.EMPTY);source.discard();}
 private MixedAggregation(){}
}
