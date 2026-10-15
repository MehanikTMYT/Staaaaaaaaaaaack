package me.frankv.staaaaaaaaaaaack.verify;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
import java.lang.management.ManagementFactory;
import java.util.*;
@Mod("stxck_verification")
public class MixedVanillaBenchmark {
    private ServerLevel level; private int scenario=-1,ticks,firstPacked;
    private final List<Double> samples=new ArrayList<>();
    private long start,cpu,spawn,total,usedCpu,heap;
    private final List<String> rows=new ArrayList<>();
    public MixedVanillaBenchmark() {
        if(Boolean.getBoolean("stxck.mixedVanilla")) {
            NeoForge.EVENT_BUS.addListener(this::started);NeoForge.EVENT_BUS.addListener(this::pre);NeoForge.EVENT_BUS.addListener(this::post);
        }
    }
    private void started(ServerStartedEvent e) {level=e.getServer().overworld();level.setChunkForced(0,0,true);level.getChunk(0,0);scenario=-2;}
    private List<ItemEntity> entities(){return level.getEntitiesOfClass(ItemEntity.class,new AABB(0,60,0,16,100,16));}
    private void begin() {
        entities().forEach(ItemEntity::discard);int n=5000;long t=System.nanoTime();samples.clear();var types=net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().filter(i->i!=Items.AIR&&i.getDefaultMaxStackSize()==64).limit(scenario==0?10:100).toList();
        for(int i=0;i<n;i++){var e=new ItemEntity(level,8,80,8,new ItemStack(types.get(i%types.size())),0,0,0);e.setNoGravity(true);e.setNoPickUpDelay();level.addFreshEntity(e);}
        spawn=System.nanoTime()-t;ticks=0;firstPacked=-1;total=0;usedCpu=0;heap=0;
    }
    private void pre(ServerTickEvent.Pre e){start=System.nanoTime();cpu=ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();}
    private void post(ServerTickEvent.Post e){
        if(scenario==-1)return;
        if(scenario==-2){if(++ticks==40){scenario=0;begin();}return;}
        samples.add((System.nanoTime()-start)/1e6);total+=System.nanoTime()-start;usedCpu+=ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()-cpu;
        heap=Math.max(heap,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
        if(firstPacked<0&&entities().size()==(5000+63)/64)firstPacked=ticks+1;
        if(++ticks==100){
            long count=entities().stream().mapToLong(x->x.getItem().getCount()).sum();int n=5000;
            var sorted=new ArrayList<>(samples);Collections.sort(sorted);
            String row=String.format(Locale.ROOT,"vanilla,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d",scenario==0?10:100,n,spawn/1e6,total/1e6/ticks,sorted.get(94),sorted.get(98),sorted.getLast(),entities().size(),count);
            rows.add(row);System.out.println("STXCK_BASELINE "+row);
            if(count!=n)throw new AssertionError("vanilla count");
            if(++scenario==2){scenario=-1;try{Files.write(Path.of("mixed-vanilla.csv"),rows);}catch(Exception x){throw new RuntimeException(x);}e.getServer().halt(false);}else begin();
        }
    }
}
