package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.*;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
@Mod("stxck_verification")
public final class MixedHoppers {
 ServerLevel level;int ticks,phase;AABB box=new AABB(512,70,512,528,90,528);
 public MixedHoppers(){if(Boolean.getBoolean("stxck.mixedHoppers")){NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::tick);}}
 void start(ServerStartedEvent e){level=e.getServer().overworld();level.setChunkForced(32,32,true);level.getChunk(32,32);}
 void fixture(){MixedConfig.excludeAutomation.set(phase==1);for(var type:new Item[]{Items.IRON_INGOT,Items.GOLD_INGOT}){var e=new ItemEntity(level,520,80,520.5,new ItemStack(type,64),0,0,0);e.setNoGravity(true);e.setNoPickUpDelay();Stxck.setExtraItemCount(e,436);level.addFreshEntity(e);}}
 void tick(ServerTickEvent.Post e){if(level==null)return;try{ticks++;if(ticks==40)fixture();if(ticks==70){if(level.getEntitiesOfClass(MixedPileEntity.class,box).size()!=1)throw new AssertionError("fixture did not aggregate");for(int x=519;x<=520;x++)level.setBlockAndUpdate(new BlockPos(x,79,520),Blocks.HOPPER.defaultBlockState());}
 if(ticks==170){long stored=0;for(int x=519;x<=520;x++){var pos=new BlockPos(x,79,520);var h=(HopperBlockEntity)level.getBlockEntity(pos);int n=0;for(int i=0;i<h.getContainerSize();i++)n+=h.getItem(i).getCount();if(n==0)throw new AssertionError("hopper did not extract "+x);stored+=n;h.clearContent();level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());}
 long ground=level.getEntitiesOfClass(MixedPileEntity.class,box).stream().mapToLong(MixedPileEntity::total).sum()+level.getEntitiesOfClass(ItemEntity.class,box).stream().mapToLong(Stxck::getTotalCount).sum();if(stored+ground!=1000)throw new AssertionError("hopper conservation "+stored+" + "+ground);System.out.println("MIXED HOPPERS PASS mode="+phase+" stored="+stored+" ground="+ground);
 level.getEntitiesOfClass(MixedPileEntity.class,box).forEach(p->p.remove(Entity.RemovalReason.KILLED));level.getEntitiesOfClass(ItemEntity.class,box).forEach(p->p.remove(Entity.RemovalReason.KILLED));if(phase++==0){ticks=0;}else{Files.writeString(Path.of("mixed-hoppers.txt"),"PASS two real ticking hoppers, direct transactional capability and vanilla fallback, 1000 items each\n");level=null;e.getServer().halt(false);}}
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("mixed-hoppers.txt"),"FAIL "+t);}catch(Exception ignored){}level=null;e.getServer().halt(false);}}
}
