package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.Stxck;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
@Mod("stxck_verification")
public class CreateProbe {
    private ServerLevel level; private int ticks, phase;
    private final BlockPos left=new BlockPos(8,80,8), right=new BlockPos(9,80,8);
    private final AABB area=new AABB(0,60,0,16,100,16);
    private final StringBuilder results=new StringBuilder();
    public CreateProbe(){if(Boolean.getBoolean("stxck.create")){NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::tick);}}
    private void start(ServerStartedEvent e){level=e.getServer().overworld();level.setChunkForced(0,0,true);level.getChunk(0,0);for(var old:level.getEntitiesOfClass(ItemEntity.class,area))old.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);setup("andesite_funnel");}
    private void setup(String id){
        level.setBlockAndUpdate(left.west(),Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(right.east(),Blocks.BARREL.defaultBlockState());
        ((Container)level.getBlockEntity(left.west())).clearContent();
        ((Container)level.getBlockEntity(right.east())).clearContent();
        var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("create:"+id));
        if(block==Blocks.AIR)throw new AssertionError("Missing Create block "+id);
        level.setBlockAndUpdate(left,block.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.EAST));
        level.setBlockAndUpdate(right,block.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.WEST));
    }
    private long stored(){long n=0;for(var pos:new BlockPos[]{left.west(),right.east()}){
        var c=(Container)level.getBlockEntity(pos);for(int i=0;i<c.getContainerSize();i++){
            var s=c.getItem(i);if(s.getCount()>s.getMaxStackSize())throw new AssertionError("oversized inventory");
            if(!s.isEmpty()&&!s.getHoverName().getString().equals("Stxck unique reward"))throw new AssertionError("component lost");n+=s.getCount();}}
        return n;
    }
    private void exercise() throws Exception {
        // Entity chunk data loads asynchronously after ServerStarted. Reset the isolated
        // arena at the scenario boundary, after its 40-tick load wait, not just at startup.
        for(var old:level.getEntitiesOfClass(ItemEntity.class,area))old.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        for(var pos:new BlockPos[]{left.west(),right.east()})((Container)level.getBlockEntity(pos)).clearContent();

        var stack=new ItemStack(Items.COBBLESTONE,64);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Stxck unique reward"));
        var pile=new ItemEntity(level,9,80.5,8.5,stack,0,0,0);pile.setNoGravity(true);pile.setNoPickUpDelay();
        Stxck.setExtraItemCount(pile,936);level.addFreshEntity(pile);
        for(int i=0;i<40;i++){
            var list=level.getEntitiesOfClass(ItemEntity.class,area);if(list.isEmpty())break;
            var item=list.getFirst();item.setPos(9,80.5,8.5);
            for(var pos:new BlockPos[]{left,right}){
                var state=level.getBlockState(pos);
                state.getBlock().getClass().getMethod("entityInside", net.minecraft.world.level.block.state.BlockState.class, net.minecraft.world.level.Level.class, BlockPos.class, net.minecraft.world.entity.Entity.class, net.minecraft.world.entity.InsideBlockEffectApplier.class, boolean.class).invoke(state.getBlock(),state,level,pos,item,null,false);
                long ground=level.getEntitiesOfClass(ItemEntity.class,area).stream().mapToLong(Stxck::getTotalCount).sum();
                if(stored()+ground!=1000)throw new AssertionError("conservation: stored="+stored()+" ground="+ground);
            }
        }
        if(stored()!=1000)throw new AssertionError("funnels did not consume all: "+stored());
        results.append("PASS ").append(phase==0?"andesite":"brass").append(" dual real FunnelBlock.entityInside, stale same entity, custom name: stored=1000 ground=0\n");
        for(var pos:new BlockPos[]{left.west(),right.east()})((Container)level.getBlockEntity(pos)).clearContent();
    }
    private void chute() throws Exception {
        var pos=new BlockPos(8,80,8);
        var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("create:chute"));
        level.setBlockAndUpdate(pos,block.defaultBlockState());
        var be=level.getBlockEntity(pos);be.getClass().getMethod("initialize").invoke(be);be.getClass().getMethod("tick").invoke(be);
        var stack=new ItemStack(Items.COBBLESTONE,64);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Stxck unique reward"));
        var pile=new ItemEntity(level,8.5,81,8.5,stack.copy(),0,0,0);Stxck.setExtraItemCount(pile,936);level.addFreshEntity(pile);
        long extracted=0;
        for(int i=0;i<32;i++) {
            var list=level.getEntitiesOfClass(ItemEntity.class,area);if(list.isEmpty())break;
            var item=list.getFirst();item.setPos(8.5,81,8.5);
            for(int attempt=0;attempt<2;attempt++) {
                block.updateEntityMovementAfterFallOn(level,item);
                var held=(ItemStack)be.getClass().getMethod("getItem").invoke(be);
                if(!held.isEmpty()&&!ItemStack.isSameItemSameComponents(held,stack))throw new AssertionError("chute components");
                if(held.getCount()>64)throw new AssertionError("chute overstack");
                extracted+=held.getCount();be.getClass().getMethod("setItem",ItemStack.class).invoke(be,ItemStack.EMPTY);
                long ground=level.getEntitiesOfClass(ItemEntity.class,area).stream().mapToLong(Stxck::getTotalCount).sum();
                if(extracted+ground!=1000)throw new AssertionError("chute conservation "+extracted+" + "+ground);
            }
        }
        if(extracted!=1000)throw new AssertionError("chute incomplete "+extracted);
        results.append("PASS real Chute fall callback and internal ItemStack, repeated stale handle: extracted=1000 ground=0; downstream extraction fixture\n");
    }
    private void fan() throws Exception {
        var pile=new ItemEntity(level,8,85,8,new ItemStack(Items.RAW_IRON,64),0,0,0);Stxck.setExtraItemCount(pile,936);level.addFreshEntity(pile);
        var typeClass=Class.forName("com.simibubi.create.content.kinetics.fan.processing.FanProcessingType");
        var type=Class.forName("com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes").getField("BLASTING").get(null);
        var process=Class.forName("com.simibubi.create.content.kinetics.fan.processing.FanProcessing").getMethod("applyProcessing",ItemEntity.class,typeClass);
        boolean changed=false;for(int i=0;i<2000&&!changed;i++)changed=(boolean)process.invoke(null,pile,type);
        long raw=0,ingots=0;
        for(var item:level.getEntitiesOfClass(ItemEntity.class,area)){
            if(item.getItem().is(Items.RAW_IRON))raw+=Stxck.getTotalCount(item);
            else if(item.getItem().is(Items.IRON_INGOT))ingots+=Stxck.getTotalCount(item);
            else throw new AssertionError("unexpected fan product");
        }
        if(!changed||raw!=936||ingots!=64)throw new AssertionError("fan raw="+raw+" ingots="+ingots);
        results.append("PASS real FanProcessing BLASTING: 64 raw iron processed, 936 raw reserve unchanged, 64 ingots; processing callback, no airflow scene\n");
        for(var item:level.getEntitiesOfClass(ItemEntity.class,area))item.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
    }
    private void belt() throws Exception {
        var pos=new BlockPos(8,80,8);var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("create:belt"));
        level.setBlockAndUpdate(pos,block.defaultBlockState());var be=level.getBlockEntity(pos);
        be.getClass().getField("beltLength").setInt(be,1);be.getClass().getField("index").setInt(be,0);be.getClass().getMethod("setController",BlockPos.class).invoke(be,pos);
        var init=be.getClass().getDeclaredMethod("initializeItemHandler");init.setAccessible(true);init.invoke(be);
        var inv=be.getClass().getMethod("getInventory").invoke(be);
        var handler=Class.forName("com.simibubi.create.content.kinetics.belt.transport.ItemHandlerBeltSegment").getConstructor(inv.getClass(),int.class).newInstance(inv,0);
        var pile=new ItemEntity(level,8.5,80.8125,8.5,new ItemStack(Items.COBBLESTONE,64),0,0,0);Stxck.setExtraItemCount(pile,936);level.addFreshEntity(pile);
        long extracted=0;
        var collision=block.getClass().getMethod("entityInside",net.minecraft.world.level.block.state.BlockState.class,net.minecraft.world.level.Level.class,BlockPos.class,net.minecraft.world.entity.Entity.class,net.minecraft.world.entity.InsideBlockEffectApplier.class,boolean.class);
        for(int i=0;i<32;i++) {
            var list=level.getEntitiesOfClass(ItemEntity.class,area);if(list.isEmpty())break;var item=list.getFirst();item.setPos(8.5,80.8125,8.5);item.setDeltaMovement(0,0,0);
            for(int attempt=0;attempt<2;attempt++) {
                collision.invoke(block,level.getBlockState(pos),level,pos,item,null,false);inv.getClass().getMethod("tick").invoke(inv);
                var stack=(ItemStack)handler.getClass().getMethod("extractItem",int.class,int.class,boolean.class).invoke(handler,0,64,false);extracted+=stack.getCount();inv.getClass().getMethod("tick").invoke(inv);
                long ground=level.getEntitiesOfClass(ItemEntity.class,area).stream().mapToLong(Stxck::getTotalCount).sum();
                if(extracted+ground!=1000)throw new AssertionError("belt conservation "+extracted+" + "+ground);
            }
        }
        if(extracted!=1000)throw new AssertionError("belt incomplete "+extracted);
        results.append("PASS real BeltBlock collision and BeltInventory/ItemHandlerBeltSegment: extracted=1000 ground=0; stationary segment, downstream extraction fixture\n");
    }
    private void tick(ServerTickEvent.Post e){if(level==null||++ticks%40!=0)return;
        try{exercise();if(phase++==0){setup("brass_funnel");return;}
            chute();belt();fan();
            Files.writeString(Path.of("create-results.txt"),results);System.out.print(results);
        }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("create-results.txt"),results+"FAIL "+t);}catch(Exception ignored){}}
        level=null;e.getServer().halt(false);
    }
}
