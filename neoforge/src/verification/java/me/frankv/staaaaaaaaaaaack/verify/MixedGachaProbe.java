package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.*;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
@Mod("stxck_verification")
public final class MixedGachaProbe {
 ServerLevel level;int ticks;AABB box=new AABB(0,60,0,16,100,16);
 public MixedGachaProbe(){if(Boolean.getBoolean("stxck.mixedGacha")){NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::tick);}}
 void start(ServerStartedEvent e){level=e.getServer().overworld();level.setChunkForced(0,0,true);level.getChunk(0,0);}
 void tick(ServerTickEvent.Post event){if(level==null)return;try{ticks++;var player=FakePlayerFactory.getMinecraft(level);
 if(ticks==40){player.setPos(8,80,8);player.getInventory().clearContent();for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
 for(var type:new Item[]{Items.COBBLESTONE,Items.IRON_INGOT}){var stack=new ItemStack(type,64);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Stxck unique reward"));var item=new ItemEntity(level,8,80,8,stack,0,0,0);item.setNoGravity(true);item.setNoPickUpDelay();Stxck.setExtraItemCount(item,436);level.addFreshEntity(item);}
 var method=Class.forName("xiaojin.gachaaddiction.command.GachaCommand").getDeclaredMethod("executeGacha",net.minecraft.commands.CommandSourceStack.class,java.util.Collection.class,Identifier.class,Identifier.class,int.class);method.setAccessible(true);method.invoke(null,event.getServer().createCommandSourceStack(),java.util.List.of(player),Identifier.parse("gachaaddiction:empty"),Identifier.parse("stxck_verification:gacha_reward"),1);
 for(var item:level.getEntitiesOfClass(ItemEntity.class,box)){item.setNoGravity(true);item.setDeltaMovement(0,0,0);item.setPos(8,80,8);}}
 if(ticks==80){var piles=level.getEntitiesOfClass(MixedPileEntity.class,box);long total=piles.stream().mapToLong(MixedPileEntity::total).sum()+level.getEntitiesOfClass(ItemEntity.class,box).stream().filter(Stxck::isMergable).mapToLong(Stxck::getTotalCount).sum();if(total!=1064||piles.size()!=1)throw new AssertionError("Gacha mixed quantity="+total+" piles="+piles.size());
 piles.getFirst().unpack();long owned=0,all=0;for(var item:level.getEntitiesOfClass(ItemEntity.class,box)){if(!Stxck.isMergable(item))continue;if(!Component.literal("Stxck unique reward").equals(item.getItem().get(DataComponents.CUSTOM_NAME)))throw new AssertionError("reward component lost");long n=Stxck.getTotalCount(item);all+=n;if(player.getUUID().equals(item.getTarget()))owned+=n;}if(all!=1064||owned!=64)throw new AssertionError("Gacha owner or quantity "+all+" owned="+owned);
 Files.writeString(Path.of("mixed-gacha-results.txt"),"PASS real GachaCommand full-inventory reward + two item types automatically aggregate after 40 world ticks: 1064 total, 64 owner-restricted, custom components preserved, fake display excluded; unpack exact. FakePlayer/no UI.\n");level=null;event.getServer().halt(false);}
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("mixed-gacha-results.txt"),"FAIL "+t);}catch(Exception ignored){}level=null;event.getServer().halt(false);}}
}
