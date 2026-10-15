package me.frankv.staaaaaaaaaaaack.mixed;
import me.frankv.staaaaaaaaaaaack.Stxck;
import me.frankv.staaaaaaaaaaaack.mixin.ItemEntityAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;
/** One independently aging item identity. Full original entity metadata survives materialization. */
public final class MixedEntry {
 public final ItemStack item;
 public final net.neoforged.neoforge.transfer.item.ItemResource resource;
 public final CompoundTag source, identity;
 public int count, age, delay;
 public final int lifespan;
 private MixedEntry(ItemStack item,CompoundTag source,int count,int age,int delay,int lifespan){
  this.item=item.copyWithCount(1);this.resource=net.neoforged.neoforge.transfer.item.ItemResource.of(this.item);this.source=source;this.count=count;this.age=age;this.delay=delay;this.lifespan=lifespan;
  identity=source.copy();
  for(String key:new String[]{"UUID","Pos","Motion","Rotation","Item","Age","PickupDelay","StxckExtraItemCount","StxckExtraItemTemplate","StxckMixedCooldown","OnGround","FallDistance"})identity.remove(key);
 }
 public static MixedEntry capture(ItemEntity entity){
  var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,entity.registryAccess());entity.saveWithoutId(output);
  var accessor=(ItemEntityAccessor)entity;
  return new MixedEntry(entity.getItem(),output.buildResult(),Math.toIntExact(Stxck.getTotalCount(entity)),accessor.getAge(),accessor.getPickupDelay(),entity.lifespan);
 }
 private MixedEntry(MixedEntry original){item=original.item.copyWithCount(1);resource=original.resource;source=original.source;identity=original.identity;count=original.count;age=original.age;delay=original.delay;lifespan=original.lifespan;}
 /** Internal transaction snapshots share immutable metadata; only count/timers are journaled. */
 public MixedEntry copy(){return new MixedEntry(this);}
 /** External snapshots must not expose the authoritative compound tags. */
 public MixedEntry snapshotCopy(){return new MixedEntry(item,source.copy(),count,age,delay,lifespan);}
 public boolean compatible(MixedEntry other){return age==other.age&&delay==other.delay&&lifespan==other.lifespan&&identity.equals(other.identity)&&ItemStack.isSameItemSameComponents(item,other.item);}
 public CompoundTag save(){
  var tag=new CompoundTag();tag.put("Source",source.copy());tag.putInt("Count",count);tag.putInt("Age",age);tag.putInt("Delay",delay);tag.putInt("Lifespan",lifespan);return tag;
 }
 public static MixedEntry load(ServerLevel level,CompoundTag tag){
  var source=tag.getCompoundOrEmpty("Source").copy();source.remove("UUID");
  var probe=new ItemEntity(EntityType.ITEM,level);probe.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),source));
  // Unknown item data remains serialized in Source even when no ItemStack can be resolved.
  return new MixedEntry(probe.getItem(),source,Math.max(0,tag.getIntOr("Count",0)),tag.getIntOr("Age",probe.getAge()),tag.getIntOr("Delay",0),tag.getIntOr("Lifespan",probe.lifespan));
 }
 public ItemEntity materialize(ServerLevel level,int quantity,double x,double y,double z){
  if(item.isEmpty()||quantity<=0)return null;
  var tag=source.copy();tag.remove("UUID");tag.remove(Stxck.EXTRA_ITEM_COUNT_TAG);tag.remove("StxckExtraItemTemplate");
  var entity=new ItemEntity(EntityType.ITEM,level);entity.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag));
  if(entity.isRemoved())return null;
  int visible=Math.min(quantity,item.getMaxStackSize());entity.setItem(item.copyWithCount(visible));Stxck.setExtraItemCount(entity,quantity-visible);
  var accessor=(ItemEntityAccessor)entity;accessor.setAge(age);accessor.setPickupDelay(delay);entity.lifespan=lifespan;
  entity.setPos(x,y,z);((MixedItemState)entity).mixed$cooldown(100);return entity;
 }
}
