package me.frankv.staaaaaaaaaaaack.mixed;
import me.frankv.staaaaaaaaaaaack.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
/** A bounded nearby query only after stability delay; removed sources cannot be extracted twice. */
public final class MixedAggregation {
 private static final java.util.WeakHashMap<ServerLevel,java.util.ArrayDeque<java.lang.ref.WeakReference<ItemEntity>>> QUEUES=new java.util.WeakHashMap<>();
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
  if(!(source.level() instanceof ServerLevel level)||!MixedConfig.enabled.get()||source.getClass()!=ItemEntity.class||!source.isAlive())return;
  var state=(MixedItemState)source;long tick=level.getGameTime();
  if(state.mixed$queued()||state.mixed$cooldown()!=0||tick<state.mixed$nextAttempt()||!state.mixed$stableNow()||tick-state.mixed$stableSince()<MixedConfig.delayTicks.get())return;
  state.mixed$queued(true);QUEUES.computeIfAbsent(level,ignored->new java.util.ArrayDeque<>()).addLast(new java.lang.ref.WeakReference<>(source));
 }
 public static void drain(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
  if(!MixedConfig.enabled.get()){
   for(var queue:QUEUES.values())for(var reference:queue){var item=reference.get();if(item!=null)((MixedItemState)item).mixed$queued(false);}
   QUEUES.clear();return;
  }
  for(var level:event.getServer().getAllLevels()){
   var queue=QUEUES.get(level);if(queue==null)continue;
   while(!queue.isEmpty()){
    var budget=MixedWorkBudget.begin(level);if(budget==null)break;
    try{var item=queue.removeFirst().get();if(item!=null){var state=(MixedItemState)item;state.mixed$queued(false);if(!item.isRemoved()&&item.level()==level)process(item,level,budget);if(item.isAlive())state.mixed$nextAttempt(level.getGameTime()+5);}}
    finally{budget.finish();}
   }
  }
 }
 private static void process(ItemEntity source,ServerLevel level,MixedWorkBudget budget){
  MixedPileEntity target=null;
  try{
   if(!eligible(source))return;
   double radius=MixedConfig.radius.get();var area=source.getBoundingBox().inflate(radius);
   var identity=net.neoforged.neoforge.transfer.item.ItemResource.of(source.getItem());
   for(var pile:MixedPileIndex.nearby(level,area,identity)){
    if(pile.absorb(source,false,identity)){target=pile;budget.moved();break;}
   }
   if(target==null){
    if(!budget.canStartPair())return;
    // A bounded initial pair is allowed to finish even if this cooperative time slice expires.
    // Otherwise a cold query can use the entire slice without ever making progress.
    var partner=new java.util.ArrayList<ItemEntity>(1);
    level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),area,
     item->item!=source&&!ItemStack.isSameItemSameComponents(source.getItem(),item.getItem())&&eligible(item),partner,1);
    if(partner.isEmpty())return;
    var pile=new MixedPileEntity(MixedModule.TYPE.get(),level);pile.setPos(source.getX(),source.getY(),source.getZ());
    if(!level.addFreshEntity(pile))return;target=pile;
    if(pile.absorb(source,false,identity))budget.moved();
    if(pile.absorb(partner.getFirst(),false))budget.moved();
   }
   if(!budget.available())return;
   var candidates=new java.util.ArrayList<ItemEntity>(16);
   level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),area,MixedAggregation::eligible,candidates,16);
   for(var item:candidates){
    if(!budget.available()||target.entryCount()>=MixedConfig.maxEntries.get()||target.total()>=MixedConfig.maxTotalItems.get())break;
    if(item!=source&&!item.isRemoved()&&target.absorb(item,false))budget.moved();
   }
  }finally{
   if(target!=null){if(target.total()==0)target.discard();else target.changed();}
  }
 }
 public static void consume(ItemEntity source){Stxck.setExtraItemCount(source,0);source.setItem(ItemStack.EMPTY);source.discard();}
 private MixedAggregation(){}
}
