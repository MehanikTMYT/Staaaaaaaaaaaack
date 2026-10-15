package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.*;
import me.frankv.staaaaaaaaaaaack.config.StxckCommonConfig;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.lang.reflect.Proxy;
import java.nio.file.*;
@Mod("stxck_verification")
public final class MixedFpsServer {
 ServerPlayer player;int phase,ticks,finish;StxckCommonConfig original;AABB box=new AABB(0,60,0,16,100,16);
 public MixedFpsServer(){if(Boolean.getBoolean("stxck.mixedFpsServer")){NeoForge.EVENT_BUS.addListener(this::login);NeoForge.EVENT_BUS.addListener(this::tick);}}
 void login(PlayerEvent.PlayerLoggedInEvent e){player=(ServerPlayer)e.getEntity();original=Stxck.commonConfig;for(int x=4;x<13;x++)for(int z=1;z<13;z++)player.level().setBlockAndUpdate(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState());player.teleportTo(8,80,4);player.getInventory().clearContent();begin();}
 void write(String s){try{Files.writeString(Path.of("fps-phase.txt"),s);}catch(Exception e){throw new RuntimeException(e);}}
 void begin(){ticks=0;write("WAIT");var level=player.level();level.getEntitiesOfClass(MixedPileEntity.class,box).forEach(e->e.remove(Entity.RemovalReason.KILLED));level.getEntitiesOfClass(ItemEntity.class,box).forEach(e->e.remove(Entity.RemovalReason.KILLED));int mode=phase/2,types=phase%2==0?10:100;MixedConfig.enabled.set(mode==2);
 Stxck.commonConfig=(StxckCommonConfig)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{StxckCommonConfig.class},(p,m,a)->{if(m.getName().equals("getMergeDelayTicks"))return 0;if(mode==0&&m.getName().equals("isItemListAsWhitelist"))return true;if(mode==0&&m.getName().equals("getItemList"))return java.util.List.of();return m.invoke(original,a);});
 for(var type:BuiltInRegistries.ITEM.stream().filter(i->i!=Items.AIR&&i.getDefaultMaxStackSize()==64).limit(types).toList()){int remaining=5000/types;while(remaining>0){int quantity=mode==0?Math.min(64,remaining):remaining;remaining-=quantity;var item=new ItemEntity(level,8,80,8,new ItemStack(type,Math.min(64,quantity)),0,0,0);item.setNoGravity(true);item.setNoPickUpDelay();Stxck.setExtraItemCount(item,Math.max(0,quantity-64));level.addFreshEntity(item);}}}
 void tick(ServerTickEvent.Post e){if(player==null)return;if(finish>0){if(++finish==60)e.getServer().halt(false);return;}if(++ticks==80){long total=player.level().getEntitiesOfClass(MixedPileEntity.class,box).stream().mapToLong(MixedPileEntity::total).sum()+player.level().getEntitiesOfClass(ItemEntity.class,box).stream().mapToLong(Stxck::getTotalCount).sum();if(total!=5000)throw new AssertionError("FPS conservation "+total);write(new String[]{"vanilla-policy","same","mixed"}[phase/2]+"-"+(phase%2==0?10:100)+",5000");}if(ticks==200){if(++phase==6){write("DONE");finish=1;}else begin();}}
}
