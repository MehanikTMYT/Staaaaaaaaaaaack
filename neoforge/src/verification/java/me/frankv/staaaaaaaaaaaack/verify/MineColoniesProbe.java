package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.Stxck;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.lang.reflect.*;
import java.nio.file.*;
@Mod("stxck_verification")
public class MineColoniesProbe {
    private ServerLevel level;private int ticks;
    public MineColoniesProbe(){if(Boolean.getBoolean("stxck.minecolonies")){NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::tick);}}
    private void start(ServerStartedEvent e){level=e.getServer().overworld();level.setChunkForced(0,0,true);level.getChunk(0,0);}
    private void tick(ServerTickEvent.Post event){
        if(level==null||++ticks!=40)return;
        try {
            Class<?> citizenClass=Class.forName("com.minecolonies.core.entity.citizen.EntityCitizen");
            Class<?> abstractCitizen=Class.forName("com.minecolonies.api.entity.citizen.AbstractEntityCitizen");
            Class<?> dataClass=Class.forName("com.minecolonies.api.colony.ICitizenData");
            Class<?> inventoryClass=Class.forName("com.minecolonies.api.inventory.InventoryCitizen");
            Object inventory=inventoryClass.getConstructor(String.class,boolean.class).newInstance("Stxck test",false);
            Object data=Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{dataClass},(p,m,a)->{
                if(m.getName().equals("getInventory"))return inventory;
                if(m.getReturnType()==boolean.class)return false;
                if(m.getReturnType()==int.class)return 0;
                return null;
            });
            var type=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecolonies:citizen"));
            Object citizen=citizenClass.getConstructor(EntityType.class,Level.class).newInstance(type,level);
            Field field=citizenClass.getDeclaredField("citizenData");field.setAccessible(true);field.set(citizen,data);
            ((Entity)citizen).setPos(8,80,8);
            Method pickup=Class.forName("com.minecolonies.core.util.citizenutils.CitizenItemUtils")
                    .getMethod("tryPickupItemEntity",abstractCitizen,ItemEntity.class);
            var item=new ItemEntity(level,8,80,8,new ItemStack(Items.COBBLESTONE,64),0,0,0);
            item.setNoPickUpDelay();Stxck.setExtraItemCount(item,936);level.addFreshEntity(item);
            var area=new AABB(0,60,0,16,100,16);
            for(int i=0;i<32;i++) {
                var entities=level.getEntitiesOfClass(ItemEntity.class,area);
                if(entities.isEmpty())break;
                var target=entities.getFirst();pickup.invoke(null,citizen,target);pickup.invoke(null,citizen,target);
            }
            int slots=(int)inventoryClass.getMethod("getSlots").invoke(inventory);long total=0;
            for(int i=0;i<slots;i++){
                ItemStack stack=(ItemStack)inventoryClass.getMethod("getStackInSlot",int.class).invoke(inventory,i);
                if(stack.getCount()>stack.getMaxStackSize())throw new AssertionError("NPC oversized inventory stack");total+=stack.getCount();
            }
            long ground=level.getEntitiesOfClass(ItemEntity.class,area).stream().mapToLong(Stxck::getTotalCount).sum();
            if(total!=1000||ground!=0)throw new AssertionError("inventory="+total+" ground="+ground);
            String result="PASS real CitizenItemUtils.tryPickupItemEntity and InventoryCitizen, repeated stale handle: inventory=1000 ground=0. ICitizenData fixture; full colony AI not exercised.";
            Files.writeString(Path.of("minecolonies-results.txt"),result+"\n");System.out.println("STXCK_MINECOLONIES "+result);
        }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("minecolonies-results.txt"),"FAIL "+t);}catch(Exception ignored){}}
        level=null;event.getServer().halt(false);
    }
}
