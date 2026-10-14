package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.Stxck;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.lang.reflect.*;
import java.nio.file.*;
@Mod("stxck_verification")
public class Ae2Probe {
 private ServerLevel level; private int ticks;
 public Ae2Probe(){if(Boolean.getBoolean("stxck.ae2")){NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::tick);}}
 private void start(ServerStartedEvent e){level=e.getServer().overworld();level.setChunkForced(0,0,true);level.getChunk(0,0);}
 private void tick(ServerTickEvent.Post e){if(level==null||++ticks!=40)return;
  try{
   var clazz=Class.forName("appeng.parts.automation.ItemPickupStrategy");
   var sinkClass=Class.forName("appeng.api.behaviors.PickupSink");
   var strategy=clazz.getConstructor(ServerLevel.class,BlockPos.class,Direction.class,BlockEntity.class,ItemEnchantments.class,java.util.UUID.class).newInstance(level,new BlockPos(8,80,8),Direction.UP,null,ItemEnchantments.EMPTY,null);
   var pickup=clazz.getMethod("pickUpEntity",Class.forName("appeng.api.networking.energy.IEnergySource"),sinkClass,Entity.class);
   var area=new AABB(0,60,0,16,100,16);
   for(int limit:new int[]{64,7}){
    long[] stored={0};
    var sink=Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{sinkClass},(p,m,a)->{long n=Math.min((long)a[1],limit);stored[0]+=n;return n;});
    var item=new ItemEntity(level,8,80,8,new ItemStack(Items.COBBLESTONE,64),0,0,0);Stxck.setExtraItemCount(item,936);level.addFreshEntity(item);
    for(int i=0;i<300;i++){
     var list=level.getEntitiesOfClass(ItemEntity.class,area);if(list.isEmpty())break;
     var target=list.getFirst();clazz.getMethod("reset").invoke(strategy);pickup.invoke(strategy,null,sink,target);
     pickup.invoke(strategy,null,sink,target);
     long ground=level.getEntitiesOfClass(ItemEntity.class,area).stream().mapToLong(Stxck::getTotalCount).sum();
     if(ground+stored[0]!=1000)throw new AssertionError("AE2 conservation "+ground+" + "+stored[0]);
    }
    if(stored[0]!=1000)throw new AssertionError("AE2 incomplete "+stored[0]);
   }
   Files.writeString(Path.of("ae2-results.txt"),"PASS real ItemPickupStrategy full/partial acceptance and stale entity: 1000 stored each; PickupSink counter fixture, no live ME network or Import Bus.\n");
  }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("ae2-results.txt"),"FAIL "+t);}catch(Exception ignored){}}
  level=null;e.getServer().halt(false);
 }
}
