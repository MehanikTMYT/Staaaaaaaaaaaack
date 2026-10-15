package me.frankv.staaaaaaaaaaaack.mixed;
import java.util.*;
import me.frankv.staaaaaaaaaaaack.Stxck;
import net.minecraft.network.syncher.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.*;

/** Server-authoritative mixed ground pile; never pretends to be an ItemEntity. */
public final class MixedPileEntity extends Entity {
 private static final EntityDataAccessor<Integer> TOTAL=SynchedEntityData.defineId(MixedPileEntity.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> TYPES=SynchedEntityData.defineId(MixedPileEntity.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<ItemStack> SAMPLE=SynchedEntityData.defineId(MixedPileEntity.class,EntityDataSerializers.ITEM_STACK);
 private static final EntityDataAccessor<String> SUMMARY=SynchedEntityData.defineId(MixedPileEntity.class,EntityDataSerializers.STRING);
 final List<MixedEntry> entries=new ArrayList<>();
 public final MixedInventory inventory=new MixedInventory(this);
 boolean busy;
 public MixedPileEntity(EntityType<? extends MixedPileEntity> type,Level level){super(type,level);}
 protected void defineSynchedData(SynchedEntityData.Builder b){b.define(TOTAL,0);b.define(TYPES,0);b.define(SAMPLE,ItemStack.EMPTY);b.define(SUMMARY,"");}
 public long total(){return level().isClientSide()?entityData.get(TOTAL):entries.stream().mapToLong(e->e.count).sum();}
 public int entryCount(){return level().isClientSide()?entityData.get(TYPES):(int)entries.stream().filter(e->e.count>0).count();}
 public ItemStack sample(){return entityData.get(SAMPLE);}
 public String summary(){return entityData.get(SUMMARY);}
 public List<MixedEntry> snapshot(){return entries.stream().map(MixedEntry::copy).toList();}
 boolean canAccept(MixedEntry entry){
  if(isRemoved()||inventory.isInTransaction()||entry.count<=0||entry.item.isEmpty()||total()+entry.count>MixedConfig.maxTotalItems.get())return false;
  return entries.stream().anyMatch(e->e.count>0&&e.compatible(entry))||entryCount()<MixedConfig.maxEntries.get();
 }
 boolean append(MixedEntry entry){
  if(!canAccept(entry))return false;
  for(var old:entries)if(old.count>0&&old.compatible(entry)){old.count=Math.addExact(old.count,entry.count);return true;}
  entries.add(entry);return true;
 }
 public boolean absorb(ItemEntity source){
  if(busy||inventory.isInTransaction()||!MixedAggregation.eligible(source))return false;
  if(total()+Stxck.getTotalCount(source)>MixedConfig.maxTotalItems.get())return false;
  if(entryCount()>=MixedConfig.maxEntries.get()&&entries.stream().noneMatch(e->ItemStack.isSameItemSameComponents(e.item,source.getItem())))return false;
  var entry=MixedEntry.capture(source);if(!append(entry))return false;
  MixedAggregation.consume(source);changed();return true;
 }
 void changed(){
  if(inventory.isInTransaction())return;
  entries.removeIf(e->e.count==0);
  if(entries.isEmpty()){discard();return;}
  long total=total();entityData.set(TOTAL,(int)Math.min(Integer.MAX_VALUE,total));entityData.set(TYPES,entries.size());
  var first=entries.stream().filter(e->!e.item.isEmpty()).findFirst();entityData.set(SAMPLE,first.map(e->e.item.copyWithCount(1)).orElse(new ItemStack(Items.BARRIER)));
  var text=new StringBuilder();int shown=0;
  for(var e:entries){if(shown++==3)break;if(!text.isEmpty())text.append(" / ");String name=e.item.isEmpty()?"unresolved":e.item.getHoverName().getString();text.append(e.count).append(" ").append(name.substring(0,Math.min(name.length(),28)));}
  if(entries.size()>3)text.append(" / +").append(entries.size()-3);entityData.set(SUMMARY,text.toString());
 }
 private ItemEntity release(MixedEntry entry,int count){
  if(!(level() instanceof ServerLevel server)||count<=0)return null;
  var ordinary=entry.materialize(server,count,getX(),getY(),getZ());if(ordinary==null)return null;
  entry.count-=count;
  if(!server.addFreshEntity(ordinary)&&!ordinary.isRemoved()){entry.count+=count;return null;}
  return ordinary;
 }
 /** Safe fallback for machines expecting ItemEntity; vetoed spawns remain owned by this pile. */
 public List<ItemEntity> unpack(){
  if(level().isClientSide()||busy||inventory.isInTransaction())return List.of();
  var released=new ArrayList<ItemEntity>();busy=true;
  try{for(var entry:List.copyOf(entries))if(entry.count>0){var entity=release(entry,entry.count);if(entity!=null)released.add(entity);}}
  finally{busy=false;changed();}
  return released;
 }
 @Override public void playerTouch(Player player){
  if(level().isClientSide()||busy||isRemoved()||inventory.isInTransaction())return;
  busy=true;int attempts=0;long picked=0;
  try{
   for(var entry:List.copyOf(entries))while(entry.count>0&&!entry.item.isEmpty()&&attempts++<128){
    int quantity=Math.min(entry.count,entry.item.getMaxStackSize());var ordinary=release(entry,quantity);if(ordinary==null)break;
    // A REAL registered ItemEntity executes vanilla/NeoForge pre+post pickup hooks, stats and owner checks.
    ordinary.playerTouch(player);
    long left=ordinary.isRemoved()?0:Stxck.getTotalCount(ordinary);picked+=Math.max(0,quantity-left);
    if(left>0&&append(MixedEntry.capture(ordinary)))MixedAggregation.consume(ordinary);
    // Unsupported event-side modifications remain real drops with a re-aggregation cooldown.
    if(left>=quantity)break;
   }
  }finally{busy=false;changed();}
  if(picked>0)level().playSound(null,getX(),getY(),getZ(),SoundEvents.ITEM_PICKUP,SoundSource.PLAYERS,0.2f,1.0f);
 }
 @Override public void tick(){
  super.tick();if(level().isClientSide()||isRemoved()||busy||inventory.isInTransaction())return;
  if(!MixedConfig.enabled.get()||(MixedConfig.fallbackToVanilla.get()&&AutomationPolicy.mustUnpack(this))){unpack();return;}
  boolean changed=false;busy=true;
  try{for(var e:List.copyOf(entries)){
   if(e.delay>0&&e.delay!=32767)e.delay--;if(e.age!=-32768)e.age++;
   // Let vanilla and NeoForge decide expiration/cancellation on an actual restored entity.
   if(e.age>=e.lifespan-1){release(e,e.count);changed=true;}
  }}finally{busy=false;}
  if(changed)changed();
 }
 @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
  var released=unpack();for(var item:released)if(item.isAlive())item.hurtServer(level,source,amount);return !released.isEmpty();
 }
 @Override public void remove(RemovalReason reason){
  if(!level().isClientSide()&&reason==RemovalReason.DISCARDED&&!entries.isEmpty()&&!busy){unpack();return;}
  super.remove(reason);
  if(reason.shouldDestroy())entries.clear();
 }
 protected void addAdditionalSaveData(ValueOutput output){output.putInt("MixedVersion",1);output.store("MixedEntries",CompoundTag.CODEC.listOf(),inventory.saveView().stream().map(MixedEntry::save).toList());}
 protected void readAdditionalSaveData(ValueInput input){
  if(!(level() instanceof ServerLevel server))return;entries.clear();
  for(var tag:input.read("MixedEntries",CompoundTag.CODEC.listOf()).orElse(List.of())){var e=MixedEntry.load(server,tag);if(e.count>0)entries.add(e);}
  // Existing piles are never truncated by lowered config limits.
  if(!entries.isEmpty())changed();
 }
}
