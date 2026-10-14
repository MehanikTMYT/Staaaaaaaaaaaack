package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.Stxck;
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
public class GachaProbe {
 private ServerLevel level; private int ticks;
 public GachaProbe(){if(Boolean.getBoolean("stxck.gacha")){NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::tick);}}
 private void start(ServerStartedEvent e){level=e.getServer().overworld();level.setChunkForced(0,0,true);level.getChunk(0,0);}
 private void tick(ServerTickEvent.Post e){if(level==null||++ticks!=40)return;
  try{
   var area=new AABB(0,60,0,16,100,16);
   for(var old:level.getEntitiesOfClass(ItemEntity.class,area))old.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
   var player=FakePlayerFactory.getMinecraft(level);player.setPos(8,80,8);player.getInventory().clearContent();
   var stack=new ItemStack(Items.COBBLESTONE,64);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Stxck unique reward"));
   var pile=new ItemEntity(level,8,81.3,8,stack.copy(),0,0,0);Stxck.setExtraItemCount(pile,936);level.addFreshEntity(pile);
   var method=Class.forName("xiaojin.gachaaddiction.command.GachaCommand").getDeclaredMethod("executeGacha",net.minecraft.commands.CommandSourceStack.class,java.util.Collection.class,Identifier.class,Identifier.class,int.class);method.setAccessible(true);
   var source=e.getServer().createCommandSourceStack();
   for(int phase=0;phase<2;phase++){
    if(phase==1){player.getInventory().clearContent();for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.STONE,64));}
    method.invoke(null,source,java.util.List.of(player),Identifier.parse("gachaaddiction:empty"),Identifier.parse("stxck_verification:gacha_reward"),1);
    for(var item:level.getEntitiesOfClass(ItemEntity.class,area))if(item.isAlive())item.tick();
    long ground=level.getEntitiesOfClass(ItemEntity.class,area).stream().filter(Stxck::isMergable).mapToLong(Stxck::getTotalCount).sum();
    if(ground!=(phase==0?1000:1064))throw new AssertionError("Gacha real ground phase="+phase+" total="+ground);
    if(Stxck.getTotalCount(pile)!=1000)throw new AssertionError("reward merged before owner/fake initialization");
    long inventory=0;for(int i=0;i<36;i++){var s=player.getInventory().getItem(i);if(s.is(Items.COBBLESTONE)){if(!ItemStack.isSameItemSameComponents(stack,s))throw new AssertionError("reward component lost");inventory+=s.getCount();}}
    if(inventory!=(phase==0?64:0))throw new AssertionError("reward inventory "+inventory);
   }
   Files.writeString(Path.of("gacha-results.txt"),"PASS real GachaCommand.executeGacha with named loot: free inventory 64 + ground1000; full inventory ground1064, owner preserved, fake reward excluded. FakePlayer, no UI test.\n");
  }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("gacha-results.txt"),"FAIL "+t);}catch(Exception ignored){}}
  level=null;e.getServer().halt(false);
 }
}
