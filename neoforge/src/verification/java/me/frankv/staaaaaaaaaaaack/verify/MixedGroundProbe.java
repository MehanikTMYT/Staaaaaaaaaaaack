package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.*;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
@Mod("stxck_verification")
public final class MixedGroundProbe {
 ServerLevel level;int ticks;AABB box=new AABB(512,70,512,528,90,528);BlockPos support=new BlockPos(520,79,520);
 public MixedGroundProbe(){if(Boolean.getBoolean("stxck.mixedGround")){NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::tick);}}
 void start(ServerStartedEvent e){level=e.getServer().overworld();level.setChunkForced(32,32,true);level.getChunk(32,32);}
 void tick(ServerTickEvent.Post e){if(level==null)return;try{ticks++;if(ticks==40){level.setBlockAndUpdate(support,Blocks.STONE.defaultBlockState());for(var type:new Item[]{Items.COBBLESTONE,Items.IRON_INGOT}){var item=new ItemEntity(level,520.5,82,520.5,new ItemStack(type,64),0,0,0);item.setNoPickUpDelay();level.addFreshEntity(item);}}
 if(ticks==100){var piles=level.getEntitiesOfClass(MixedPileEntity.class,box);if(piles.size()!=1||piles.getFirst().total()!=128){for(var item:level.getEntitiesOfClass(ItemEntity.class,box))System.out.println("GROUND_DIAGNOSTIC "+item.position()+" velocity="+item.getDeltaMovement()+" ground="+item.onGround()+" stable="+((MixedItemState)item).mixed$stableNow()+" eligible="+MixedAggregation.eligible(item)+" count="+Stxck.getTotalCount(item));throw new AssertionError("falling items did not settle and aggregate; piles="+piles.size());}level.setBlockAndUpdate(support,Blocks.AIR.defaultBlockState());piles.getFirst().tick();if(!piles.getFirst().isRemoved()||level.getEntitiesOfClass(ItemEntity.class,box).stream().mapToLong(Stxck::getTotalCount).sum()!=128)throw new AssertionError("lost ground fallback");Files.writeString(Path.of("mixed-ground.txt"),"PASS ordinary gravity drops settle and aggregate; removing supporting block materializes all 128 items\n");level=null;e.getServer().halt(false);}
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("mixed-ground.txt"),"FAIL "+t);}catch(Exception ignored){}level=null;e.getServer().halt(false);}}
}
