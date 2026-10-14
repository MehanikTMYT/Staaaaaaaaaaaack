package me.frankv.staaaaaaaaaaaack.verify;

import me.frankv.staaaaaaaaaaaack.Stxck;
import me.frankv.staaaaaaaaaaaack.config.StxckCommonConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.lang.management.ManagementFactory;
import java.util.*;

@Mod("stxck_verification")
public class Verification {
    private ServerLevel level;
    private StxckCommonConfig original;
    private final Map<String,Object> overrides = new HashMap<>();
    private final List<String> results = new ArrayList<>();
    private static final AABB AREA = new AABB(0,60,0,16,100,16);
    private int scenario=-1, elapsed, firstMerged=-1;
    private long tickStart, cpuStart, spawnNanos, totalNanos, totalCpu, peakHeap;
    private int tests;
    private boolean rejectSpawns;
    public Verification() {
        if (Boolean.getBoolean("stxck.verify")) {
            NeoForge.EVENT_BUS.addListener(this::rejectJoin);
            NeoForge.EVENT_BUS.addListener(this::started);
            NeoForge.EVENT_BUS.addListener(this::pre);
            NeoForge.EVENT_BUS.addListener(this::post);
        }
    }
    private void rejectJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (rejectSpawns && event.getEntity() instanceof ItemEntity) event.setCanceled(true);
    }
    private void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        tests++;
    }
    private ItemEntity item(ItemStack stack) {
        var entity = new ItemEntity(level, 8,80,8,stack,0,0,0);
        entity.setNoGravity(true);
        entity.setNoPickUpDelay();
        entity.tickCount=40;
        return entity;
    }
    private ItemEntity pile(int count) {
        var entity=item(new ItemStack(Items.COBBLESTONE,Math.min(count,64)));
        Stxck.setExtraItemCount(entity,count-entity.getItem().getCount());
        return entity;
    }
    private List<ItemEntity> entities() { return level.getEntitiesOfClass(ItemEntity.class,AREA,e->e.isAlive()); }
    private long ground() { return entities().stream().mapToLong(Stxck::getTotalCount).sum(); }
    private long inventory(net.minecraft.world.Container c) {
        long n=0; for(int i=0;i<c.getContainerSize();i++) n+=c.getItem(i).getCount(); return n;
    }
    private void clear() {
        for(var e:entities()) e.remove(Entity.RemovalReason.KILLED);
    }
    private ItemEntity reload(ItemEntity e) {
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
        e.saveWithoutId(out);
        var clone=new ItemEntity(EntityType.ITEM,level);
        clone.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),out.buildResult()));
        clone.tickCount=40;
        return clone;
    }
    private void started(ServerStartedEvent event) {
        try {
            level=event.getServer().overworld();
            level.getChunk(0,0);
            level.setChunkForced(0,0,true); // Verification arena only; production mod never adds tickets.
            original=Stxck.commonConfig;
            Stxck.commonConfig=(StxckCommonConfig)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{StxckCommonConfig.class},
                    (proxy,method,args)->overrides.containsKey(method.getName())?overrides.get(method.getName()):method.invoke(original,args));
            overrides.put("getMergeDelayTicks",0);
            Files.writeString(Path.of("verification-results.csv"),"RUNNING\n");
            scenario=-2;elapsed=0;
        } catch(Throwable t) { fail(t,event.getServer()); }
    }
    private void functional() {
        // Gacha/vanilla reward visuals mark a drop fake AFTER adding it to the level.
        clear();
        var real=pile(1000);level.addFreshEntity(real);
        var visual=new ItemEntity(level,8,80,8,new ItemStack(Items.COBBLESTONE,64),0,0,0);
        level.addFreshEntity(visual);visual.makeFakeItem();
        check(Stxck.getTotalCount(real)==1000,"post-spawn fake reward must not merge on join");
        visual.tick();
        check(Stxck.getTotalCount(real)==1000,"fake reward excluded on first tick");
        clear();

        for(int n:new int[]{100,1000,5000}) {
            var a=pile(1);
            for(int i=1;i<n;i++) { var b=pile(1); Stxck.tryToMerge(a,b,true); check(b.isRemoved(),"supplier consumed"); }
            check(Stxck.getTotalCount(a)==n,"merge total "+n);
            check(Stxck.getTotalCount(reload(a))==n,"save roundtrip "+n);
            check(a.getItem().getCount()<=64,"front bounded");
        }
        var a=pile(1000); var b=pile(10);
        b.getItem().set(DataComponents.CUSTOM_NAME,Component.literal("unique"));
        Stxck.tryToMerge(a,b); check(Stxck.getTotalCount(a)==1000&&!b.isRemoved(),"components differ");
        b=pile(1); b.setTarget(UUID.randomUUID()); Stxck.tryToMerge(a,b); check(!b.isRemoved(),"pickup owner protected");
        overrides.put("getMaxSize",100); a=pile(99); b=pile(2); Stxck.tryToMerge(a,b); check(!b.isRemoved(),"max cap"); overrides.remove("getMaxSize");
        a=pile(64); Stxck.setExtraItemCount(a,Integer.MAX_VALUE); check(Stxck.getTotalCount(a)==2147483711L,"legacy overflow safe");
        check(Stxck.getTotalCount(reload(a))==2147483711L,"legacy overflow save");
        overrides.put("getItemList",List.of("minecraft:cobblestone")); check(Stxck.isBlackListItem(pile(1)),"blacklist");
        overrides.put("isItemListAsWhitelist",true); check(!Stxck.isBlackListItem(pile(1)),"whitelist");
        overrides.put("getItemList",List.of("#minecraft:logs")); check(!Stxck.isBlackListItem(item(new ItemStack(Items.OAK_LOG))),"tag whitelist");
        overrides.remove("getItemList"); overrides.remove("isItemListAsWhitelist");
        overrides.put("getDimensionList",List.of("minecraft:overworld")); check(Stxck.isBlackListItem(pile(1)),"dimension blacklist"); overrides.remove("getDimensionList");
        overrides.put("getMergeDelayTicks",10); a=pile(1); b=pile(1); a.tickCount=0; b.tickCount=0;
        Stxck.tryToMerge(a,b); check(!b.isRemoved(),"deferred does not merge early"); a.tickCount=10;b.tickCount=10;Stxck.tryToMerge(a,b,true);check(b.isRemoved(),"deferred eligible");overrides.put("getMergeDelayTicks",0);
        overrides.put("isEnableForUnstackableItem",true);
        a=item(new ItemStack(Items.IRON_SWORD)); b=item(new ItemStack(Items.IRON_SWORD));Stxck.tryToMerge(a,b,true);
        check(Stxck.getTotalCount(a)==2&&a.getItem().getCount()==1,"unstackable bounded");
        b=item(new ItemStack(Items.IRON_SWORD));b.getItem().setDamageValue(1);Stxck.tryToMerge(a,b);check(!b.isRemoved(),"damage differs");overrides.remove("isEnableForUnstackableItem");
        clear();a=pile(1000);level.addFreshEntity(a);
        var first=new SimpleContainer(1);check(HopperBlockEntity.addItem(first,a),"hopper extracts");
        check(a.isRemoved()&&a.getItem().isEmpty(),"old handle invalidated");
        var second=new SimpleContainer(1);HopperBlockEntity.addItem(second,a);
        System.out.println("STXCK_DEBUG first="+inventory(first)+" second="+inventory(second)+" ground="+ground()+" entities="+entities());
        check(inventory(first)+inventory(second)+ground()==1000,"two consumers stale handle conservation");
        long extracted=inventory(first)+inventory(second);
        while(ground()>0) { var c=new SimpleContainer(1);HopperBlockEntity.addItem(c,entities().getFirst());extracted+=inventory(c); }
        check(extracted==1000,"hopper drain exact");
        clear();a=pile(1000);level.addFreshEntity(a);first=new SimpleContainer(1);first.setItem(0,new ItemStack(Items.COBBLESTONE,63));
        HopperBlockEntity.addItem(first,a);check(inventory(first)+ground()==1063,"partial hopper conservation");
        var player=FakePlayerFactory.getMinecraft(level);player.getInventory().clearContent();
        for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        player.getInventory().setItem(0,new ItemStack(Items.COBBLESTONE,63));
        long before=inventory(player.getInventory())+ground();a.playerTouch(player);
        check(inventory(player.getInventory())+ground()==before,"near full player conservation");
        check(player.getInventory().getItem(0).getCount()==64,"player partial pickup");
        player.getInventory().clearContent(); before=ground();
        while(ground()>0)entities().getFirst().playerTouch(player);
        check(inventory(player.getInventory())==before,"player drain exact");
        clear();a=pile(1000);a.getItem().setCount(0);check(Stxck.getTotalCount(reload(a))==936,"save empty front");
        a=pile(1000);level.addFreshEntity(a);a.setItem(new ItemStack(Items.DIAMOND,64));
        check(ground()==1000,"transformation preserves reserve");
        check(entities().stream().filter(e->e.getItem().is(Items.COBBLESTONE)).mapToLong(Stxck::getTotalCount).sum()==936,"reserve retains original components");
        clear();
        a=pile(1000); a.getItem().set(DataComponents.CUSTOM_NAME,Component.literal("modified in place"));
        b=reload(a); level.addFreshEntity(b); b.tick();
        check(ground()==1000,"in-place component change save conservation");
        check(entities().stream().filter(e->!e.getItem().has(DataComponents.CUSTOM_NAME)).mapToLong(Stxck::getTotalCount).sum()==936,
                "saved reserve retains original components");
        clear();
        a=pile(1000);level.addFreshEntity(a);first=new SimpleContainer(1);rejectSpawns=true;
        HopperBlockEntity.addItem(first,a);rejectSpawns=false;
        check(inventory(first)+ground()==1000,"canceled replacement retains count");
        check(!a.isRemoved()&&a.getItem().isEmpty(),"canceled replacement leaves empty extraction handle");
        a.tick();check(a.isRemoved()&&ground()==936,"replacement retries after veto removed");
        clear();
        a=pile(1000);level.addFreshEntity(a);a.hurtServer(level,level.damageSources().generic(),100);
        check(a.isRemoved()&&ground()==0,"legitimate destruction does not resurrect reserve");
        a=pile(1000);level.addFreshEntity(a);((me.frankv.staaaaaaaaaaaack.mixin.ItemEntityAccessor)a).setAge(a.lifespan);a.tick();
        check(a.isRemoved()&&ground()==0,"expiry removes whole pile");
        var small=new ItemStack(Items.COBBLESTONE,16);small.set(DataComponents.MAX_STACK_SIZE,16);
        a=item(small);b=item(small.copy());Stxck.tryToMerge(a,b,true);
        check(a.getItem().getCount()==16&&Stxck.getTotalCount(a)==32,"component stack limit honored");
    }
    private void startScenario() {
        clear(); int mode=scenario/3;int n=new int[]{100,1000,5000}[scenario%3];
        overrides.put("isItemListAsWhitelist",mode==0);overrides.put("getItemList",List.of());
        overrides.put("getMergeDelayTicks",mode==2?10:0);
        long start=System.nanoTime();
        for(int i=0;i<n;i++) {var e=pile(1);e.tickCount=0;level.addFreshEntity(e);}
        spawnNanos=System.nanoTime()-start;elapsed=0;totalNanos=0;totalCpu=0;peakHeap=0;firstMerged=-1;
    }
    private void pre(ServerTickEvent.Pre event) {
        tickStart=System.nanoTime();cpuStart=ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
    }
    private void post(ServerTickEvent.Post event) {
        if(scenario==-2) {
            if(++elapsed==40)try {
                clear(); functional();
                results.add("FUNCTIONAL PASS assertions="+tests);
                Files.write(Path.of("verification-results.csv"),results);
                clear();
                for(int x=8;x<=9;x++) level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x,79,8),net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState());
                var source=pile(1000);source.setPos(9,80,8.5);level.addFreshEntity(source);
                scenario=-3;elapsed=0;
            }catch(Throwable t){fail(t,event.getServer());}
            return;
        }
        if(scenario==-3) {
            if(++elapsed==100)try {
                long stored=0;
                for(int x=8;x<=9;x++) {
                    var pos=new net.minecraft.core.BlockPos(x,79,8);
                    var hopper=(HopperBlockEntity)level.getBlockEntity(pos);
                    check(inventory(hopper)==320,"physical hopper fills to ordinary capacity");
                    stored+=inventory(hopper);hopper.clearContent();
                    level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                check(stored+ground()==1000,"two ticking hopper blocks conserve 1000");
                results.add("PHYSICAL_HOPPERS PASS stored="+stored+" ground="+ground());
                results.add("mode,count,spawn_ms,mean_mspt,server_thread_cpu_ms,peak_heap_MiB,entities,merge_tick,total");
                Files.write(Path.of("verification-results.csv"),results);
                scenario=3;startScenario();
            }catch(Throwable t){fail(t,event.getServer());}
            return;
        }
        if(scenario<0)return;
        try {
            totalNanos+=System.nanoTime()-tickStart;totalCpu+=ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()-cpuStart;
            peakHeap=Math.max(peakHeap,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
            elapsed++;
            if(firstMerged<0&&entities().size()==1)firstMerged=elapsed;
            if(elapsed==100) {
                int n=new int[]{100,1000,5000}[scenario%3];
                check(ground()==n,"benchmark conservation "+scenario);
                results.add(String.format(Locale.ROOT,"%s,%d,%.3f,%.3f,%.3f,%.1f,%d,%d,%d",
                        new String[]{"vanilla-policy","immediate","deferred10"}[scenario/3],n,spawnNanos/1e6,totalNanos/1e6/elapsed,totalCpu/1e6,peakHeap/1048576d,entities().size(),firstMerged,ground()));
                Files.write(Path.of("verification-results.csv"),results);
                System.out.println("STXCK_VERIFY "+results.getLast());
                if(++scenario==9) {
                    scenario=-1;clear();Stxck.commonConfig=original;
                    Files.write(Path.of("verification-results.csv"),results);
                    System.out.println("STXCK_VERIFY PASS assertions="+tests);
                    event.getServer().halt(false);
                } else startScenario();
            }
        }catch(Throwable t){fail(t,event.getServer());}
    }
    private void fail(Throwable t,net.minecraft.server.MinecraftServer server) {
        scenario=-1;t.printStackTrace();results.add("FAIL "+t);
        try{Files.write(Path.of("verification-results.csv"),results);}catch(Exception ignored){}
        server.halt(false);
    }
}
