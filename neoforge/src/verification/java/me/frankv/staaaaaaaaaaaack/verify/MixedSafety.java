package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.*;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import me.frankv.staaaaaaaaaaaack.mixin.ItemEntityAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
import java.util.*;
@Mod("stxck_verification")
public final class MixedSafety {
 ServerLevel level;int ticks;UUID owner=UUID.randomUUID();AABB box=new AABB(512,70,512,528,90,528);
 public MixedSafety(){if(Boolean.getBoolean("stxck.mixedSafety")){NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::tick);}}
 void start(ServerStartedEvent e){level=e.getServer().overworld();level.setChunkForced(32,32,true);level.getChunk(32,32);}
 void check(boolean ok,String text){if(!ok)throw new AssertionError(text);System.out.println("MIXED SAFETY PASS "+text);}
 void tick(ServerTickEvent.Post event){if(level==null)return;try{ticks++;if(ticks==40){for(int i=0;i<3;i++){var stack=new ItemStack(i==2?Items.IRON_INGOT:Items.DIAMOND_SWORD,i==2?64:1);if(i<2)stack.setDamageValue(i+1);var e=new ItemEntity(level,520,80,520,stack,0,0,0);e.setNoGravity(true);e.setNoPickUpDelay();if(i==0)e.setTarget(owner);if(i==1)((ItemEntityAccessor)e).setPickupDelay(200);((ItemEntityAccessor)e).setAge(i*50);e.lifespan=6000+i*100;level.addFreshEntity(e);}}
 if(ticks==80){var piles=level.getEntitiesOfClass(MixedPileEntity.class,box);check(piles.size()==1,"different components and unstackables aggregate");var pile=piles.getFirst();check(pile.total()==66&&pile.entryCount()==3,"independent item identities");var player=FakePlayerFactory.getMinecraft(level);player.getInventory().clearContent();pile.playerTouch(player);long n=0;for(int i=0;i<player.getInventory().getContainerSize();i++)n+=player.getInventory().getItem(i).getCount();check(n==64&&pile.total()==2,"owner and pickup delay respected via real pickup hooks");var records=pile.snapshot();check(records.get(0).lifespan!=records.get(1).lifespan,"individual lifespans");var released=pile.unpack();check(released.size()==2,"unstackables materialize independently");check(released.stream().allMatch(e->e.getItem().getCount()==1),"ordinary stack maximum preserved");check(released.stream().anyMatch(e->owner.equals(e.getTarget())),"owner survives materialization");for(var e:released){var r=records.stream().filter(x->ItemStack.isSameItemSameComponents(x.item,e.getItem())).findFirst().orElseThrow();check(((ItemEntityAccessor)e).getAge()==r.age&&((ItemEntityAccessor)e).getPickupDelay()==r.delay&&e.lifespan==r.lifespan,"individual timers survive materialization");}
 Files.writeString(Path.of("mixed-safety.txt"),"PASS unstackables, different damage components, owner, pickup delay, individual ages/lifespans\n");level=null;event.getServer().halt(false);}
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("mixed-safety.txt"),"FAIL "+t);}catch(Exception ignored){}level=null;event.getServer().halt(false);}}
}
