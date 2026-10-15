package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.*;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.nio.file.*;
import java.util.*;
@Mod("stxck_verification")
public final class MixedLifecycleProbe {
 ServerLevel level;int ticks;AABB box=new AABB(512,70,512,528,90,528);
 public MixedLifecycleProbe(){if(Boolean.getBoolean("stxck.mixedLifecycle")){NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::tick);}}
 void start(ServerStartedEvent e){level=e.getServer().overworld();level.setChunkForced(32,32,true);level.getChunk(32,32);}
 MixedPileEntity pile(){
  var pile=new MixedPileEntity(MixedModule.TYPE.get(),level);var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());pile.saveWithoutId(output);var records=new ArrayList<CompoundTag>();
  for(var type:new Item[]{Items.COBBLESTONE,Items.IRON_INGOT}){var item=new ItemEntity(level,520,80,520,new ItemStack(type,64),0,0,0);item.setNoGravity(true);item.setNoPickUpDelay();Stxck.setExtraItemCount(item,436);records.add(MixedEntry.capture(item).save());}
  output.store("MixedEntries",CompoundTag.CODEC.listOf(),records);pile.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),output.buildResult()));pile.setPos(520,80,520);if(!level.addFreshEntity(pile))throw new AssertionError("fixture spawn");return pile;
 }
 long ground(){return level.getEntitiesOfClass(ItemEntity.class,box).stream().mapToLong(Stxck::getTotalCount).sum();}
 void check(boolean ok,String why){if(!ok)throw new AssertionError(why);System.out.println("MIXED LIFECYCLE PASS "+why);}
 void tick(ServerTickEvent.Post event){if(level==null||++ticks!=40)return;try{
  var discarded=pile();var handler=discarded.inventory;var resource=handler.getResource(0);discarded.discard();check(discarded.isRemoved()&&ground()==1000,"discard materializes all contents");discarded.discard();check(ground()==1000,"repeated discard does not duplicate");try(var tx=Transaction.openRoot()){check(handler.extract(0,resource,64,tx)==0,"stale handler after discard is empty");tx.commit();}
  var damaged=pile();check(damaged.hurtServer(level,level.damageSources().cactus(),1),"damage delegates to real items");check(damaged.isRemoved()&&ground()==2000,"nonfatal damage conserves quantity");damaged.hurtServer(level,level.damageSources().cactus(),1);check(ground()==2000,"repeated damage on removed pile does not duplicate");
  var killed=pile();killed.remove(Entity.RemovalReason.KILLED);check(killed.isRemoved()&&ground()==2000,"explicit kill destroys only its own 1000 items");check(level.getEntitiesOfClass(MixedPileEntity.class,box).isEmpty(),"no ghost piles");Files.writeString(Path.of("mixed-lifecycle.txt"),"PASS discard/repeated discard, stale extraction, nonfatal damage/repeated damage, explicit kill semantics; 3000 input = 2000 ordinary + 1000 legitimate destruction\n");
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("mixed-lifecycle.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{level=null;event.getServer().halt(false);}}
}
