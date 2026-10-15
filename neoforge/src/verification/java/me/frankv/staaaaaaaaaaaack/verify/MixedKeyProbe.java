package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import java.nio.file.*;
import java.util.*;
@Mod("stxck_verification")
public final class MixedKeyProbe {
 ServerLevel level;int ticks;ItemEntity a,b;ItemStack alpha,beta;AABB box=new AABB(512,70,512,544,90,528);
 public MixedKeyProbe(){if(Boolean.getBoolean("stxck.mixedKey")){NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::tick);}}
 void start(ServerStartedEvent e){level=e.getServer().overworld();level.setChunkForced(32,32,true);level.setChunkForced(33,32,true);level.getChunk(32,32);level.getChunk(33,32);}
 ItemEntity add(ItemStack stack){var item=new ItemEntity(level,520,80,520,stack,0,0,0);item.setNoGravity(true);item.setNeverPickUp();level.addFreshEntity(item);return item;}
 List<ItemEntity> query(ItemStack stack){return level.getEntities(new MixedSameQuery(ItemResource.of(stack),level.getGameTime()),box,e->e.isAlive()&&ItemStack.isSameItemSameComponents(stack,e.getItem()));}
 void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 void tick(ServerTickEvent.Post event){if(level==null)return;try{ticks++;
  if(ticks==40){alpha=new ItemStack(Items.COBBLESTONE);alpha.set(DataComponents.CUSTOM_NAME,Component.literal("alpha"));beta=alpha.copy();beta.set(DataComponents.CUSTOM_NAME,Component.literal("beta"));a=add(alpha.copy());b=add(beta.copy());check(query(alpha).equals(List.of(a)),"full component key");a.setPos(536,80,520);check(query(alpha).equals(List.of(a)),"section move must not duplicate candidate");a.setPos(520,80,520);check(query(alpha).equals(List.of(a)),"return to cached section must not duplicate candidate");b.getItem().set(DataComponents.CUSTOM_NAME,Component.literal("alpha"));check(query(beta).isEmpty(),"live revalidation rejects stale component key");}
  if(ticks==41){check(new HashSet<>(query(alpha)).equals(Set.of(a,b))&&query(alpha).size()==2,"next tick discovers in-place component mutation exactly once");a.discard();check(query(alpha).equals(List.of(b)),"removed cached entity excluded");Files.writeString(Path.of("mixed-key.txt"),"PASS component identity, live mutation revalidation, next-tick discovery, cross-section movement/return without duplicate references, removal\n");level=null;event.getServer().halt(false);}
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("mixed-key.txt"),"FAIL "+t);}catch(Exception ignored){}level=null;event.getServer().halt(false);}}
}
