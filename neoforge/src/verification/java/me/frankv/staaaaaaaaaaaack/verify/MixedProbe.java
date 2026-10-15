package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.*;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.nio.file.*;
import java.util.*;
@Mod("stxck_verification")
public final class MixedProbe {
 boolean rejectSpawns;
 ServerLevel level;int ticks,phase;long started,spawn;List<Double> times=new ArrayList<>();List<String> rows=new ArrayList<>();
 AABB area=new AABB(512,70,512,528,90,528);
 public MixedProbe(){if(Boolean.getBoolean("stxck.mixed")){NeoForge.EVENT_BUS.addListener(this::reject);NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::pre);NeoForge.EVENT_BUS.addListener(this::post);}}
 void reject(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event){if(rejectSpawns&&event.getEntity() instanceof ItemEntity)event.setCanceled(true);}
 void start(ServerStartedEvent event){level=event.getServer().overworld();level.setChunkForced(32,32,true);level.getChunk(32,32);}
 void pre(ServerTickEvent.Pre event){started=System.nanoTime();}
 List<MixedPileEntity> piles(){return level.getEntitiesOfClass(MixedPileEntity.class,area);}
 List<ItemEntity> items(){return level.getEntitiesOfClass(ItemEntity.class,area);}
 long total(){return piles().stream().mapToLong(MixedPileEntity::total).sum()+items().stream().mapToLong(Stxck::getTotalCount).sum();}
 void check(boolean ok,String message){if(!ok)throw new AssertionError(message);System.out.println("MIXED PASS "+message);}
 void clear(){piles().forEach(e->e.remove(Entity.RemovalReason.KILLED));items().forEach(e->e.remove(Entity.RemovalReason.KILLED));}
 void spawn(int count,int types){long start=System.nanoTime();var list=BuiltInRegistries.ITEM.stream().filter(i->i!=Items.AIR&&i.getDefaultMaxStackSize()==64).limit(types).toList();for(int i=0;i<count;i++){var stack=new ItemStack(list.get(i%types));var entity=new ItemEntity(level,520,80,520,stack,0,0,0);entity.setNoGravity(true);entity.setNoPickUpDelay();level.addFreshEntity(entity);}spawn=System.nanoTime()-start;}
 MixedPileEntity decode(net.minecraft.nbt.CompoundTag tag){var p=new MixedPileEntity(MixedModule.TYPE.get(),level);p.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag));return p;}
 net.minecraft.nbt.CompoundTag save(MixedPileEntity p){var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());p.saveWithoutId(out);return out.buildResult();}
 void operations(){
  check(total()==1000,"automatic aggregation conserves 1000");check(!piles().isEmpty(),"automatic mixed entity exists");  var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);player.getInventory().clearContent();
  var other=piles().isEmpty()?null:piles().getFirst();
  if(other!=null){var template=other.snapshot().getFirst().item;for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.BEDROCK,64));player.getInventory().setItem(0,template.copyWithCount(63));long groundBefore=total();other.playerTouch(player);check(player.getInventory().getItem(0).getCount()==64,"near-full player receives one");check(total()==groundBefore-1,"partial player pickup conserves remaining");player.getInventory().clearContent();}
var pile=piles().getFirst();long before=pile.total();var h=pile.inventory;var key=h.getResource(0);
  try(var tx=Transaction.openRoot()){check(h.extract(0,key,7,tx)==7,"simulate partial extraction");check(decode(save(pile)).total()==before,"save during simulation sees committed quantity");}
  check(pile.total()==before,"simulation rollback");
  try(var outer=Transaction.openRoot()){h.extract(0,key,3,outer);try(var inner=Transaction.open(outer)){h.extract(0,key,4,inner);inner.commit();}}
  check(pile.total()==before,"nested commit rolls back with outer simulation");
  try(var tx=Transaction.openRoot()){check(h.extract(0,key,7,tx)==7,"execute partial extraction");tx.commit();}
  check(pile.total()==before-7,"execute debits once");
  long current=pile.total();var restored=decode(save(pile));check(restored.total()==current,"NBT roundtrip count");check(restored.snapshot().getFirst().compatible(pile.snapshot().getFirst()),"NBT roundtrip original metadata");
  int extracted=0;while(!pile.isRemoved()){var resource=h.getResource(0);try(var tx=Transaction.openRoot()){int n=h.extract(0,resource,11,tx);check(n>0,"extract makes progress");extracted+=n;tx.commit();}}
  check(extracted==current,"repeated mechanism extraction conserves all");try(var tx=Transaction.openRoot()){check(h.extract(0,key,64,tx)==0,"stale handler cannot extract twice");tx.commit();}
  restored.setUUID(UUID.randomUUID());check(level.addFreshEntity(restored),"restore fixture into world");
  long remaining=total();check(remaining>0,"disable fixture nonempty");rejectSpawns=true;restored.unpack();check(total()==remaining,"cancelled materialization retains ledger");rejectSpawns=false;MixedConfig.enabled.set(false);for(var p:List.copyOf(piles()))p.tick();check(piles().isEmpty(),"disable unpacks all piles");check(total()==remaining,"disable preserves all remaining items");MixedConfig.enabled.set(true);
 }
 void post(ServerTickEvent.Post event){if(level==null)return;try{
  ticks++;
  if(phase==0&&ticks==40){clear();MixedConfig.excludeAutomation.set(false);spawn(1000,10);}
  if(phase==0&&ticks==80){operations();clear();phase=1;ticks=0;spawn(5000,10);times.clear();return;}
  if(phase>=1){times.add((System.nanoTime()-started)/1e6);if(ticks==100){check(total()==5000,"benchmark conservation phase "+phase);var sorted=new ArrayList<>(times);Collections.sort(sorted);String row=String.format(Locale.ROOT,"%d,5000,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d",phase==1?10:100,spawn/1e6,times.stream().mapToDouble(x->x).average().orElse(0),sorted.get(94),sorted.get(98),sorted.getLast(),piles().size(),items().size());rows.add(row);System.out.println("MIXED BENCH "+row);clear();if(phase==1){phase=2;ticks=0;spawn(5000,100);times.clear();}else{Files.write(Path.of("mixed-benchmark.csv"),rows);Files.writeString(Path.of("mixed-results.txt"),"PASS automatic merge, transactions, serialization, disable, 10/100 types\n");level=null;event.getServer().halt(false);}}}
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("mixed-results.txt"),"FAIL "+t);}catch(Exception ignored){}level=null;event.getServer().halt(false);}}
}
