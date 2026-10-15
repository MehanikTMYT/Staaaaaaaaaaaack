package me.frankv.staaaaaaaaaaaack.verify;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
@Mod(value="stxck_verification",dist=Dist.CLIENT)
public final class MixedProfileClient {
 int ticks;boolean synchronizedPile;
 public MixedProfileClient(){if(Boolean.getBoolean("stxck.mixedProfileClient"))NeoForge.EVENT_BUS.addListener(this::tick);}
 void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;ticks++;mc.player.setYRot(0);mc.player.setXRot(12);
 if(ticks%20==0){long total=0;int piles=0;for(var entity:mc.level.entitiesForRendering())if(entity instanceof me.frankv.staaaaaaaaaaaack.mixed.MixedPileEntity pile){total+=pile.total();piles++;}if(total==5000&&piles>0)synchronizedPile=true;}
 try{if(Files.readString(Path.of(System.getProperty("stxck.profilePhase"))).trim().equals("DONE")){Files.writeString(Path.of("profile-client.txt"),(synchronizedPile?"PASS":"FAIL")+" full 5000-item mixed count synchronized\n");mc.stop();}}catch(java.io.IOException ignored){}
 }
}
