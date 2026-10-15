package me.frankv.staaaaaaaaaaaack.mixed;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
/** Cooperative server-thread budget. Unprocessed items continue their ordinary entity lifecycle. */
final class MixedWorkBudget {
 private static final WeakHashMap<ServerLevel,MixedWorkBudget> LEVELS=new WeakHashMap<>();
 private long tick=Long.MIN_VALUE,spent,start,limit;private int entries;private boolean running;
 static MixedWorkBudget begin(ServerLevel level){
  var b=LEVELS.computeIfAbsent(level,ignored->new MixedWorkBudget());if(b.running)return null;
  if(b.tick!=level.getGameTime()){b.tick=level.getGameTime();b.spent=0;b.entries=0;}
  b.limit=(long)(MixedConfig.maxMergeMillisPerTick.get()*1_000_000);
  if(b.spent>=b.limit||b.entries>=MixedConfig.maxMergedEntriesPerTick.get())return null;
  b.running=true;b.start=System.nanoTime();return b;
 }
 boolean available(){return entries<MixedConfig.maxMergedEntriesPerTick.get()&&spent+System.nanoTime()-start<limit;}
 boolean canStartPair(){return entries+2<=MixedConfig.maxMergedEntriesPerTick.get();}
 void moved(){entries++;}
 void finish(){spent+=System.nanoTime()-start;running=false;}
}
