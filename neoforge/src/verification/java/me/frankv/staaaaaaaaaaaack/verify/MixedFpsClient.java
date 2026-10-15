package me.frankv.staaaaaaaaaaaack.verify;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;
@Mod(value="stxck_verification",dist=Dist.CLIENT)
public class MixedFpsClient {
    private String phase="";
    private final Map<String,List<Integer>> samples=new LinkedHashMap<>();
    public MixedFpsClient(){if(Boolean.getBoolean("stxck.mixedFpsClient"))NeoForge.EVENT_BUS.addListener(this::tick);}
    private void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        mc.player.setYRot(0);mc.player.setXRot(12);
        try{
            String current=Files.readString(Path.of(System.getProperty("stxck.mixedFpsPhase"))).trim();
            if(current.equals("WAIT"))return;
            if(current.equals("DONE")){
                List<String> rows=new ArrayList<>();rows.add("mode,items,mean_fps,min_fps,max_fps,samples");
                samples.forEach((key,values)->rows.add(key+","+values.stream().mapToInt(Integer::intValue).average().orElse(0)+","+Collections.min(values)+","+Collections.max(values)+","+values.size()));
                Files.write(Path.of("mixed-fps.csv"),rows);System.out.println("MIXED_FPS "+rows);mc.stop();return;
            }
            samples.computeIfAbsent(current,k->new ArrayList<>()).add(mc.getFps());
        }catch(java.io.IOException ignored){}
    }
}
