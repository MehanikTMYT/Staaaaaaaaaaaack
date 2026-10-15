package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.Stxck;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
import java.util.*;
@Mod("stxck_verification")
public final class MixedPersistence {
 ServerLevel level;int ticks;boolean restore,unloaded;UUID id;
 AABB box=new AABB(512,70,512,528,90,528);
 public MixedPersistence(){if(Boolean.getBoolean("stxck.mixedPersistence")){NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::tick);}}
 void start(ServerStartedEvent e){level=e.getServer().overworld();restore=Files.exists(Path.of("mixed.saved"));level.setChunkForced(32,32,true);level.getChunk(32,32);}
 List<MixedPileEntity> piles(){return level.getEntitiesOfClass(MixedPileEntity.class,box);}
 void check(boolean ok,String text){if(!ok)throw new AssertionError(text);System.out.println("MIXED PERSIST PASS "+text);}
 void tick(ServerTickEvent.Post event){if(level==null)return;try{ticks++;
 if(!restore&&ticks==40){for(var item:List.of(Items.DIAMOND,Items.IRON_INGOT)){var stack=new ItemStack(item,64);stack.set(DataComponents.CUSTOM_NAME,Component.literal("mixed-persistent"));var e=new ItemEntity(level,520,80,520,stack,0,0,0);e.setNoGravity(true);e.setNoPickUpDelay();Stxck.setExtraItemCount(e,436);level.addFreshEntity(e);}}
 if(ticks==100){check(piles().size()==1,"single saved pile");var pile=piles().getFirst();check(pile.total()==1000,"1000 items survive");check(pile.snapshot().stream().allMatch(e->Component.literal("mixed-persistent").equals(e.item.get(DataComponents.CUSTOM_NAME))),"components survive");if(!restore){Files.writeString(Path.of("mixed.saved"),pile.getUUID().toString());level=null;event.getServer().halt(false);return;}id=pile.getUUID();check(id.toString().equals(Files.readString(Path.of("mixed.saved"))),"entity UUID survives restart");level.setChunkForced(32,32,false);}
 if(restore&&ticks>100&&ticks<600&&level.getEntity(id)==null)unloaded=true;
 if(restore&&ticks==600){check(unloaded,"actual chunk unload");level.setChunkForced(32,32,true);level.getChunk(32,32);}
 if(restore&&ticks==700){check(piles().size()==1&&piles().getFirst().total()==1000,"actual chunk reload quantity");MixedConfig.enabled.set(false);piles().getFirst().tick();check(piles().isEmpty(),"disable loaded saved pile");check(level.getEntitiesOfClass(ItemEntity.class,box).stream().mapToLong(Stxck::getTotalCount).sum()==1000,"disable materializes 1000");Files.writeString(Path.of("mixed-persistence.txt"),"PASS restart, components, UUID, actual chunk unload/reload, disable\n");level=null;event.getServer().halt(false);}
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("mixed-persistence.txt"),"FAIL "+t);}catch(Exception ignored){}level=null;event.getServer().halt(false);}}
}
