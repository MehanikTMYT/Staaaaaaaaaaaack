import jdk.jfr.consumer.*;
import java.nio.file.*;
import java.util.*;
/** Streaming JFR summary; avoids huge JSON class-loader metadata expansions. */
class AnalyzeMixedJfr {
 static void add(Map<String,Long> map,String key,long n){map.merge(key,n,Long::sum);}
 static String method(RecordedFrame f){return f.getMethod().getType().getName()+"."+f.getMethod().getName();}
 static void print(String name,Map<String,Long> values,int limit){System.out.println("\n"+name);values.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(limit).forEach(e->System.out.println(e.getValue()+" "+e.getKey()));}
 public static void main(String[] args)throws Exception{
  Map<String,Long> leaves=new HashMap<>(),categories=new HashMap<>(),callers=new HashMap<>(),queries=new HashMap<>(),allocation=new HashMap<>(),packets=new HashMap<>();long samples=0,packetBytes=0;
  try(var file=new RecordingFile(Path.of(args[0]))){while(file.hasMoreEvents()){var event=file.readEvent();String type=event.getEventType().getName();
   if(type.equals("stxck.ProfilePhase"))System.out.println("PHASE "+event.getString("phase")+" start="+event.getStartTime()+" duration_ms="+event.getDuration().toNanos()/1e6);
   if(type.equals("minecraft.PacketSent")){add(packets,event.getString("packetId"),1);packetBytes+=event.getInt("bytes");}
   if(type.equals("jdk.ObjectAllocationSample")){add(allocation,event.getClass("objectClass").getName(),event.getLong("weight"));}
   if(!type.equals("jdk.ExecutionSample")||!"Server thread".equals(event.getThread("sampledThread").getJavaName())||event.getStackTrace()==null)continue;
   samples++;var frames=event.getStackTrace().getFrames();var names=frames.stream().map(AnalyzeMixedJfr::method).toList();if(names.isEmpty())continue;add(leaves,names.getFirst(),1);
   if(names.getFirst().equals("net.minecraft.world.level.Level.lambda$getEntities$1"))add(queries,String.join(" <- ",names.subList(0,Math.min(names.size(),20))),1);
   if(names.getFirst().equals("net.minecraft.world.level.entity.EntitySection.getEntities"))add(callers,String.join(" <- ",names.subList(0,Math.min(names.size(),18))),1);
   String joined=String.join(" ",names),category;
   if(joined.contains("MixedProfile.begin"))category="creation";
   else if(joined.contains("MixedProfile.post"))category="verification/save/extract";
   else if(joined.contains("MixedEntry.capture"))category="Mixed source NBT capture";
   else if(joined.contains("MixedPileEntity.changed"))category="Mixed sync-summary preparation";
   else if((joined.contains("MixedAggregation.onItemTick")||joined.contains("MixedAggregation.process")))category="Mixed search/merge (other)";
   else if(joined.contains("mergeWithNeighbours"))category="Same-Type neighbor merge";
   else if(joined.contains("getEntityCollisions")||joined.contains("noCollision"))category="entity physics collision search";
   else if(joined.contains("ServerEntity.sendChanges")||joined.contains("PacketEncoder"))category="network tracking/encode";
   else category="other";
   add(categories,category,1);
  }}
  System.out.println("SERVER_SAMPLES "+samples+" PACKET_BYTES "+packetBytes);print("CATEGORIES",categories,30);print("LEAVES",leaves,25);print("ENTITY_QUERY_CALLERS",callers,8);print("LEVEL_QUERY_CALLERS",queries,6);print("ALLOCATION_SAMPLED_BYTES_ALL_THREADS",allocation,15);print("PACKET_COUNTS",packets,25);
 }
}
