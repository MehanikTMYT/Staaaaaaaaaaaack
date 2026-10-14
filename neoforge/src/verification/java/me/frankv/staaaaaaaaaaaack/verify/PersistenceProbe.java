package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.Stxck;
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
import java.nio.file.*;
import java.util.*;
@Mod("stxck_verification")
public class PersistenceProbe {
    private ServerLevel level;
    private int ticks;
    private boolean restoring;
    private UUID id;
    private boolean unloaded;
    public PersistenceProbe() {
        if(Boolean.getBoolean("stxck.persistence")) {
            NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::tick);
        }
    }
    private void start(ServerStartedEvent e) {
        level=e.getServer().overworld();restoring=Files.exists(Path.of("saved.marker"));
        level.setChunkForced(32,32,true);level.getChunk(32,32);
    }
    private java.util.List<ItemEntity> items() { return level.getEntitiesOfClass(ItemEntity.class,new AABB(512,70,512,528,90,528)); }
    private void check(boolean ok,String message) { if(!ok)throw new AssertionError(message);System.out.println("STXCK_PERSIST PASS "+message); }
    private void tick(ServerTickEvent.Post e) {
        if(level==null)return;
        try {
            ticks++;
            if(ticks==100) {
                if(restoring) {
                    check(items().size()==1,"world reload single pile");
                    var item=items().getFirst();
                    check(Stxck.getTotalCount(item)==1000,"world reload total 1000");
                    check(Component.literal("persistent").equals(item.getItem().get(DataComponents.CUSTOM_NAME)),"world reload components");
                    id=item.getUUID();level.setChunkForced(32,32,false);
                } else {
                    check(items().isEmpty(),"fresh arena");
                    var stack=new ItemStack(Items.COBBLESTONE,64);stack.set(DataComponents.CUSTOM_NAME,Component.literal("persistent"));
                    var item=new ItemEntity(level,520,80,520,stack,0,0,0);item.setNoGravity(true);
                    Stxck.setExtraItemCount(item,936);level.addFreshEntity(item);
                    Files.writeString(Path.of("saved.marker"),"1000 cobblestone; custom_name=persistent\n");
                    System.out.println("STXCK_PERSIST SAVE 1000");level=null;e.getServer().halt(false);
                }
            }
            if(restoring&&ticks>100&&ticks<600&&level.getEntity(id)==null)unloaded=true;
            if(restoring&&ticks==600) {
                check(unloaded,"chunk entity actually unloaded");level.setChunkForced(32,32,true);level.getChunk(32,32);
            }
            if(restoring&&ticks==700) {
                check(items().size()==1,"chunk reload single pile");check(Stxck.getTotalCount(items().getFirst())==1000,"chunk reload count");
                Files.writeString(Path.of("persistence-results.txt"),"PASS world restart, components, actual chunk unload and reload: 1000 items\n");
                level=null;e.getServer().halt(false);
            }
        }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("persistence-results.txt"),"FAIL "+t);}catch(Exception ignored){}level=null;e.getServer().halt(false);}
    }
}
