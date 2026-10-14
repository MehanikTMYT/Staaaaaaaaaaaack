package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.Stxck;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;
@Mod(value="stxck_verification",dist=Dist.CLIENT)
public class ClientProbe {
    private int ticks;
    private boolean countSeen;
    private final List<Integer> fps=new ArrayList<>();
    public ClientProbe() { if(Boolean.getBoolean("stxck.clientVerify") && !Boolean.getBoolean("stxck.fpsClient"))NeoForge.EVENT_BUS.addListener(this::tick); }
    private void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        ticks++;
        mc.player.setYRot(0);mc.player.setXRot(12);
        for(var e:mc.level.entitiesForRendering())if(e instanceof ItemEntity item&&Stxck.getTotalCount(item)==1000) {
            if(!countSeen)System.out.println("STXCK_CLIENT count=1000 overlay="+Stxck.getOverlayText(item));
            countSeen=true;
        }
        if(ticks>60&&ticks<160)fps.add(mc.getFps());
        if(ticks==120) net.minecraft.client.Screenshot.grab(mc.gameDirectory, "stxck-count.png", mc.getMainRenderTarget(), 1, message -> {});
        if(ticks==280) {
            long n=0;for(int i=0;i<mc.player.getInventory().getContainerSize();i++)n+=mc.player.getInventory().getItem(i).getCount();
            String result="count_sync="+countSeen+" inventory="+n+" fps_mean="+fps.stream().mapToInt(Integer::intValue).average().orElse(0);
            System.out.println("STXCK_CLIENT "+result);
            try { Files.writeString(Path.of("client-verification.txt"),result+"\n"); }catch(Exception ex){throw new RuntimeException(ex);}
            mc.stop();
        }
    }
}
