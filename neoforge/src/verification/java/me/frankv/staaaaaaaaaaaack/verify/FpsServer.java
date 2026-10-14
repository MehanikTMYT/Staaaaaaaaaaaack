package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.Stxck;
import me.frankv.staaaaaaaaaaaack.config.StxckCommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.lang.reflect.Proxy;
import java.nio.file.*;
@Mod("stxck_verification")
public class FpsServer {
    private ServerPlayer player; private int phase,ticks,finish; private boolean mod;
    public FpsServer(){if(Boolean.getBoolean("stxck.fpsServer")){NeoForge.EVENT_BUS.addListener(this::login);NeoForge.EVENT_BUS.addListener(this::tick);}}
    private void login(PlayerEvent.PlayerLoggedInEvent e){
        player=(ServerPlayer)e.getEntity();mod=ModList.get().isLoaded("staaaaaaaaaaaack");
        for(int x=4;x<13;x++)for(int z=1;z<13;z++)player.level().setBlockAndUpdate(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState());
        player.teleportTo(8,80,4);player.getInventory().clearContent();begin();
    }
    private void write(String value){try{Files.writeString(Path.of("fps-phase.txt"),value);}catch(Exception x){throw new RuntimeException(x);}}
    private String label(){return (mod?(phase<3?"immediate":"deferred10"):"vanilla")+","+new int[]{100,1000,5000}[phase%3];}
    private void begin(){
        ticks=0;write("WAIT");var level=player.level();
        level.getEntitiesOfClass(ItemEntity.class,new AABB(0,60,0,16,100,16)).forEach(x->x.remove(Entity.RemovalReason.KILLED));
        int n=new int[]{100,1000,5000}[phase%3];
        // This measures steady-state rendering after merging, not transient drop-storm FPS.
        if(mod){
            StxckCommonConfig config=Stxck.commonConfig;int delay=phase<3?0:10;
            Stxck.commonConfig=(StxckCommonConfig)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{StxckCommonConfig.class},
                    (p,m,a)->m.getName().equals("getMergeDelayTicks")?delay:m.invoke(config,a));
            var item=new ItemEntity(level,8,80,8,new ItemStack(Items.COBBLESTONE,64),0,0,0);item.setNoGravity(true);
            Stxck.setExtraItemCount(item,n-64);level.addFreshEntity(item);
        }else while(n>0){int size=Math.min(64,n);n-=size;var item=new ItemEntity(level,8,80,8,new ItemStack(Items.COBBLESTONE,size),0,0,0);item.setNoGravity(true);level.addFreshEntity(item);}
    }
    private void tick(ServerTickEvent.Post e){
        if(player==null)return;
        if(finish>0){if(++finish==60)e.getServer().halt(false);return;}
        if(++ticks==80)write(label());
        if(ticks==200){if(++phase==(mod?6:3)){write("DONE");finish=1;}else begin();}
    }
}
