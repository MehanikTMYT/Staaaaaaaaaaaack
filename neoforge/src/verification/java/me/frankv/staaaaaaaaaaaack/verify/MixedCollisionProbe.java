package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
import java.util.*;
@Mod("stxck_verification")
public final class MixedCollisionProbe {
 ServerLevel level;int ticks,checks;
 static final class CollidableItem extends ItemEntity {
  CollidableItem(ServerLevel level){super(level,520,80,520,new ItemStack(Items.DIAMOND),0,0,0);}
  @Override public boolean canBeCollidedWith(Entity other){return true;}
 }
 public MixedCollisionProbe(){if(Boolean.getBoolean("stxck.mixedCollision")){NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::tick);}}
 void start(ServerStartedEvent e){level=e.getServer().overworld();level.setChunkForced(32,32,true);level.setChunkForced(33,32,true);level.getChunk(32,32);level.getChunk(33,32);}
 List<String> shapes(Entity source,AABB box,boolean mixed){MixedConfig.enabled.set(mixed);return level.getEntityCollisions(source,box).stream().flatMap(s->s.toAabbs().stream()).map(Object::toString).sorted().toList();}
 void check(Entity source,AABB box){var vanilla=shapes(source,box,false);var optimized=shapes(source,box,true);if(!vanilla.equals(optimized))throw new AssertionError("collision mismatch "+vanilla+" vs "+optimized);checks++;}
 void tick(ServerTickEvent.Post event){if(level==null||++ticks!=40)return;try{
  var source=new ItemEntity(level,520,80,520,new ItemStack(Items.COBBLESTONE),0,0,0);source.setNoGravity(true);level.addFreshEntity(source);
  for(int i=0;i<200;i++){var item=new ItemEntity(level,520+(i%3)*.1,80,520,new ItemStack(Items.IRON_INGOT),0,0,0);item.setNoGravity(true);level.addFreshEntity(item);}
  var custom=new CollidableItem(level);custom.setNoGravity(true);level.addFreshEntity(custom);
  var boat=EntityType.OAK_BOAT.create(level,EntitySpawnReason.COMMAND);if(boat==null)throw new AssertionError("boat");boat.setPos(520.5,80,520);boat.setNoGravity(true);level.addFreshEntity(boat);
  var near=new AABB(518,78,518,523,83,523);check(source,near);check(boat,near);check(null,near);
  if(shapes(source,near,true).size()!=2)throw new AssertionError("boat/custom collidable item must remain candidates");
  boat.setPos(536,80,520);custom.setPos(536,80,520);check(source,near);check(source,new AABB(534,78,518,539,83,523));
  boat.discard();custom.discard();check(source,near);check(source,new AABB(534,78,518,539,83,523));
  int loaded=level.getChunkSource().getLoadedChunksCount();check(source,new AABB(100000,70,100000,100010,90,100010));if(loaded!=level.getChunkSource().getLoadedChunksCount())throw new AssertionError("collision query loaded a chunk");
  Files.writeString(Path.of("mixed-collision.txt"),"PASS "+checks+" vanilla-equivalent collision queries: ordinary drops excluded, boat/custom collidable ItemEntity retained, section moves/removal, non-item/null source fallback, no chunk loading\n");
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("mixed-collision.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{MixedConfig.enabled.set(true);level=null;event.getServer().halt(false);}}
}
