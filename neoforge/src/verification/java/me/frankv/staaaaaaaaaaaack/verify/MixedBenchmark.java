package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.*;
import me.frankv.staaaaaaaaaaaack.config.StxckCommonConfig;
import me.frankv.staaaaaaaaaaaack.mixed.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.nio.file.*;
import java.util.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Proxy;
@Mod("stxck_verification")
public final class MixedBenchmark {
 ServerLevel level;int phase=-1,ticks,merged=-1;long start,cpu,allocated,cpuTotal,allocTotal,spawn,heap;List<Double> samples=new ArrayList<>();List<String> rows=new ArrayList<>();StxckCommonConfig original;
 final com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
 final AABB box=new AABB(512,70,512,528,90,528);
 public MixedBenchmark(){if(Boolean.getBoolean("stxck.mixedBenchmark")){NeoForge.EVENT_BUS.addListener(this::started);NeoForge.EVENT_BUS.addListener(this::pre);NeoForge.EVENT_BUS.addListener(this::post);}}
 void started(ServerStartedEvent e){level=e.getServer().overworld();level.setChunkForced(32,32,true);level.getChunk(32,32);original=Stxck.commonConfig;rows.add("mode,types,count,spawn_ms,mean_mspt,p95_mspt,p99_mspt,max_mspt,cpu_ms,allocated_MiB,peak_heap_MiB,mixed_entities,item_entities,merge_tick,extract_ms");}
 List<MixedPileEntity> piles(){return level.getEntitiesOfClass(MixedPileEntity.class,box);}
 List<ItemEntity> items(){return level.getEntitiesOfClass(ItemEntity.class,box);}
 long total(){return piles().stream().mapToLong(MixedPileEntity::total).sum()+items().stream().mapToLong(Stxck::getTotalCount).sum();}
 void clear(){piles().forEach(e->e.remove(Entity.RemovalReason.KILLED));items().forEach(e->e.remove(Entity.RemovalReason.KILLED));}
 void begin(){clear();ticks=0;merged=-1;samples.clear();cpuTotal=allocTotal=heap=0;int mode=Math.min(phase/4,2);int types=phase>=12?100:new int[]{1,10,100,5000}[phase%4];MixedConfig.enabled.set(mode==2);MixedConfig.excludeAutomation.set(true);
 Stxck.commonConfig=(StxckCommonConfig)Proxy.newProxyInstance(StxckCommonConfig.class.getClassLoader(),new Class[]{StxckCommonConfig.class},(proxy,method,args)->{if(method.getName().equals("getMergeDelayTicks"))return 0;if(mode==0&&method.getName().equals("isItemListAsWhitelist"))return true;if(mode==0&&method.getName().equals("getItemList"))return List.of();return method.invoke(original,args);});
 var list=BuiltInRegistries.ITEM.stream().filter(i->i!=Items.AIR&&i.getDefaultMaxStackSize()==64).limit(Math.min(types,100)).toList();long t=System.nanoTime();for(int i=0;i<(phase>=12?0:5000);i++){var stack=new ItemStack(list.get(i%list.size()));if(types==5000)stack.set(DataComponents.CUSTOM_NAME,Component.literal("entry-"+i));var e=new ItemEntity(level,520,80,520,stack,0,0,0);e.setNoGravity(true);e.setNoPickUpDelay();level.addFreshEntity(e);}spawn=System.nanoTime()-t;}
 void flow(){long t=System.nanoTime();var list=BuiltInRegistries.ITEM.stream().filter(i->i!=Items.AIR&&i.getDefaultMaxStackSize()==64).limit(100).toList();int count=phase==12?5:25;for(int i=0;i<count;i++){var item=new ItemEntity(level,520,80,520,new ItemStack(list.get((ticks*count+i)%100)),0,0,0);item.setNoGravity(true);item.setNoPickUpDelay();level.addFreshEntity(item);}spawn+=System.nanoTime()-t;}
 void pre(ServerTickEvent.Pre event){start=System.nanoTime();cpu=bean.getCurrentThreadCpuTime();allocated=bean.getThreadAllocatedBytes(Thread.currentThread().threadId());if(level!=null&&phase>=12)flow();}
 void post(ServerTickEvent.Post event){if(level==null)return;try{if(phase==-1){if(++ticks==40){phase=Integer.getInteger("stxck.matrixStart",0);begin();}return;}
 samples.add((System.nanoTime()-start)/1e6);cpuTotal+=bean.getCurrentThreadCpuTime()-cpu;allocTotal+=bean.getThreadAllocatedBytes(Thread.currentThread().threadId())-allocated;heap=Math.max(heap,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());ticks++;
 if(merged<0&&phase/4==2&&items().size()+piles().size()<=Math.max(2,(new int[]{1,10,100,5000}[phase%4]+63)/64))merged=ticks;
 if(ticks==(phase==12?1000:phase==13?200:100)){if(total()!=5000)throw new AssertionError("benchmark quantity="+total());var sorted=new ArrayList<>(samples);Collections.sort(sorted);int mixed=piles().size(),ordinary=items().size();long t=System.nanoTime(),extracted=0;
 for(var p:List.copyOf(piles())){var h=p.inventory;while(!p.isRemoved()){var key=h.getResource(0);try(var tx=Transaction.openRoot()){int n=h.extract(0,key,64,tx);if(n<=0)throw new AssertionError("no extraction progress");extracted+=n;tx.commit();}}}long extract=System.nanoTime()-t;if(total()+extracted!=5000)throw new AssertionError("extraction conservation");
 String row=String.format(Locale.ROOT,"%s,%d,5000,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d,%d,%.3f",phase>=12?(phase==12?"flow100":"flow500"):new String[]{"vanilla-policy","same","mixed"}[phase/4],phase>=12?100:new int[]{1,10,100,5000}[phase%4],spawn/1e6,samples.stream().mapToDouble(x->x).average().orElse(0),sorted.get((int)Math.ceil(sorted.size()*0.95)-1),sorted.get((int)Math.ceil(sorted.size()*0.99)-1),sorted.getLast(),cpuTotal/1e6,allocTotal/1048576d,heap/1048576d,mixed,ordinary,merged,extract/1e6);rows.add(row);Files.write(Path.of("mixed-matrix.csv"),rows);System.out.println("MIXED MATRIX "+row);
 if(++phase==Integer.getInteger("stxck.matrixEnd",14)){clear();Stxck.commonConfig=original;Files.writeString(Path.of("mixed-matrix.txt"),"PASS "+(rows.size()-1)+" scenarios, creation/aggregation/extraction conservation\n");level=null;event.getServer().halt(false);}else begin();}
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("mixed-matrix.txt"),"FAIL "+t);}catch(Exception ignored){}level=null;event.getServer().halt(false);}}
}
