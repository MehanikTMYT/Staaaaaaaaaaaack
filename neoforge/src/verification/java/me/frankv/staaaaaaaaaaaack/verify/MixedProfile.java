package me.frankv.staaaaaaaaaaaack.verify;

import me.frankv.staaaaaaaaaaaack.*;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import jdk.jfr.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.lang.management.ManagementFactory;

/** Profiles the built release JAR without adding timing hooks to production code. */
@Mod("stxck_verification")
public final class MixedProfile {
 @Name("stxck.ProfilePhase") @Label("Stxck profile phase") @StackTrace(false)
 public static class Phase extends Event {public String phase;public int items=5000;}
 @Name("stxck.ProfileTick") @Label("Stxck measured tick") @StackTrace(false)
 public static class Tick extends Event {public int index;@Timespan(Timespan.NANOSECONDS) public long nanos;public int ordinary;public int mixed;}
 final AABB box=new AABB(512,70,512,528,90,528);
 final com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
 final List<Double> times=new ArrayList<>();final List<String> timeline=new ArrayList<>();
 ServerLevel level;ServerPlayer player;Recording recording;Phase mergePhase;
 int warmup,ticks,complete=-1,finish;long start,spawnNs,mergeStart,mergeNs,allocStart,cpuStart,allocated,cpuTotal;
 boolean active;
 final boolean connected=Boolean.getBoolean("stxck.profileConnected");
 public MixedProfile(){if(Boolean.getBoolean("stxck.mixedProfile")){NeoForge.EVENT_BUS.addListener(this::start);NeoForge.EVENT_BUS.addListener(this::login);NeoForge.EVENT_BUS.addListener(this::pre);NeoForge.EVENT_BUS.addListener(this::post);}}
 void start(ServerStartedEvent e){level=e.getServer().overworld();level.setChunkForced(32,32,true);level.getChunk(32,32);}
 void login(PlayerEvent.PlayerLoggedInEvent e){player=(ServerPlayer)e.getEntity();player.teleportTo(520,80,516);player.getInventory().clearContent();for(int x=516;x<525;x++)for(int z=513;z<525;z++)level.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState());warmup=0;}
 List<ItemEntity> items(){return level.getEntitiesOfClass(ItemEntity.class,box);}
 List<MixedPileEntity> piles(){return level.getEntitiesOfClass(MixedPileEntity.class,box);}
 long total(){return items().stream().mapToLong(Stxck::getTotalCount).sum()+piles().stream().mapToLong(MixedPileEntity::total).sum();}
 Phase phase(String label){var e=new Phase();e.phase=label;e.begin();return e;}
 void begin() throws Exception {
  MixedConfig.enabled.set(true);MixedConfig.excludeAutomation.set(true);
  recording=new Recording(Configuration.getConfiguration("profile"));recording.setName("Stxck unique-component burst");recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(5));recording.enable(Phase.class);recording.enable(Tick.class);recording.enable("minecraft.PacketSent");recording.enable("minecraft.PacketReceived");recording.start();
  var spawn=phase("create-and-add-5000-item-entities");long t=System.nanoTime();
  var types=net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().filter(i->i!=Items.AIR&&i.getDefaultMaxStackSize()==64).limit(100).toList();
  for(int i=0;i<5000;i++){var stack=new ItemStack(types.get(i%types.size()));stack.set(DataComponents.CUSTOM_NAME,Component.literal("entry-"+i));var item=new ItemEntity(level,520,80,520,stack,0,0,0);item.setNoGravity(true);item.setNoPickUpDelay();level.addFreshEntity(item);}
  spawnNs=System.nanoTime()-t;spawn.commit();mergeStart=System.nanoTime();mergePhase=phase("initial-aggregation-window");active=true;timeline.add("tick,mspt,ordinary,mixed,total");
 }
 void pre(ServerTickEvent.Pre e){start=System.nanoTime();cpuStart=bean.getCurrentThreadCpuTime();allocStart=bean.getThreadAllocatedBytes(Thread.currentThread().threadId());}
 void post(ServerTickEvent.Post event){if(level==null)return;try{
  if(finish>0){if(++finish==60){level=null;event.getServer().halt(false);}return;}
  if(!active){if((!connected||player!=null)&&++warmup==80)begin();return;}
  double ms=(System.nanoTime()-start)/1e6;times.add(ms);allocated+=bean.getThreadAllocatedBytes(Thread.currentThread().threadId())-allocStart;cpuTotal+=bean.getCurrentThreadCpuTime()-cpuStart;ticks++;
  int ordinary=items().size(),mixed=piles().size();long count=total();if(count!=5000)throw new AssertionError("profile conservation tick "+ticks+": "+count);
  timeline.add(String.format(Locale.ROOT,"%d,%.6f,%d,%d,%d",ticks,ms,ordinary,mixed,count));var te=new Tick();te.index=ticks;te.nanos=(long)(ms*1e6);te.ordinary=ordinary;te.mixed=mixed;te.commit();
  if(complete<0&&ordinary==0){complete=ticks;mergeNs=System.nanoTime()-mergeStart;mergePhase.commit();System.out.println("PROFILE aggregation complete tick="+complete+" elapsed_ms="+mergeNs/1e6);}
  if(ticks==Integer.getInteger("stxck.profileTicks",1000)){
   if(complete<0){Files.write(Path.of("profile-ticks.csv"),timeline);throw new AssertionError("aggregation incomplete ordinary="+ordinary+" piles="+mixed);}
   var save=phase("serialize-mixed-ledger");long t=System.nanoTime();long records=0;for(var pile:piles()){var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());pile.saveWithoutId(output);records+=output.buildResult().toString().length();}long saveNs=System.nanoTime()-t;save.commit();
   var extract=phase("transactional-extraction");t=System.nanoTime();long removed=0;for(var pile:List.copyOf(piles())){while(!pile.isRemoved()){var h=pile.inventory;var key=h.getResource(0);try(var tx=Transaction.openRoot()){int n=h.extract(0,key,64,tx);if(n<=0)throw new AssertionError("extract stalled");removed+=n;tx.commit();}}}long extractNs=System.nanoTime()-t;extract.commit();if(removed!=5000||total()!=0)throw new AssertionError("profile extraction");
   Files.write(Path.of("profile-ticks.csv"),timeline);var sorted=new ArrayList<>(times);Collections.sort(sorted);var burst=new ArrayList<>(times.subList(0,Math.min(100,times.size())));Collections.sort(burst);
   String summary=String.format(Locale.ROOT,"PASS connected=%s items=5000 ticks=%d spawn_ms=%.3f aggregation_ticks=%d aggregation_wall_ms=%.3f mean_mspt=%.3f p99_ms=%.3f p999_ms=%.3f max_ms=%.3f first100_mean=%.3f first100_p99=%.3f final_piles=%d serialize_ms=%.3f serialized_chars=%d extract_ms=%.3f cpu_ms=%.3f allocated_MiB=%.3f\n",connected,ticks,spawnNs/1e6,complete,mergeNs/1e6,times.stream().mapToDouble(x->x).average().orElse(0),sorted.get((int)Math.ceil(ticks*.99)-1),sorted.get((int)Math.ceil(ticks*.999)-1),sorted.getLast(),burst.stream().mapToDouble(x->x).average().orElse(0),burst.get((int)Math.ceil(burst.size()*.99)-1),mixed,saveNs/1e6,records,extractNs/1e6,cpuTotal/1e6,allocated/1048576d);
   Files.writeString(Path.of("profile-result.txt"),summary);System.out.print(summary);Files.writeString(Path.of("profile-phase.txt"),"DONE");recording.stop();recording.dump(Path.of("profile.jfr"));recording.close();finish=1;if(!connected){level=null;event.getServer().halt(false);}
  }
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("profile-result.txt"),"FAIL "+t);if(recording!=null){recording.stop();recording.dump(Path.of("profile-failed.jfr"));recording.close();}}catch(Exception ignored){}level=null;event.getServer().halt(false);}}
}
