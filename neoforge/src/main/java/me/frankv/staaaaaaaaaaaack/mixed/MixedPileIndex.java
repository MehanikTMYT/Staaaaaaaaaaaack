package me.frankv.staaaaaaaaaaaack.mixed;
import java.util.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.event.level.LevelEvent;
/** Only loaded mixed piles, indexed in four-block cells. No ordinary-item/world scan or chunk access. */
final class MixedPileIndex {
 private static final Map<ServerLevel,Index> LEVELS=new IdentityHashMap<>();
 private record Membership(long cell,Set<ItemResource> kinds){}
 private static final class Index {
  final Long2ObjectOpenHashMap<Bucket> cells=new Long2ObjectOpenHashMap<>();
  final Map<MixedPileEntity,Membership> members=new IdentityHashMap<>();
 }
 private static final class Bucket {
  final Set<MixedPileEntity> all=new LinkedHashSet<>(),available=new LinkedHashSet<>();
  final Map<ItemResource,Set<MixedPileEntity>> kinds=new HashMap<>();
  int entriesLimit=-1,totalLimit=-1;
  void limits(){if(entriesLimit==MixedConfig.maxEntries.get()&&totalLimit==MixedConfig.maxTotalItems.get())return;entriesLimit=MixedConfig.maxEntries.get();totalLimit=MixedConfig.maxTotalItems.get();available.clear();for(var p:all)if(p.entryCount()<entriesLimit&&p.total()<totalLimit)available.add(p);}
 }
 private static int cell(double coordinate){return Math.floorDiv((int)Math.floor(coordinate),4);}
 private static long cell(MixedPileEntity p){return BlockPos.asLong(cell(p.getX()),cell(p.getY()),cell(p.getZ()));}
 static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)LEVELS.remove(level);}
 static void remove(MixedPileEntity pile){
  if(!(pile.level() instanceof ServerLevel level))return;var index=LEVELS.get(level);if(index==null)return;var previous=index.members.remove(pile);if(previous==null)return;var bucket=index.cells.get(previous.cell());if(bucket==null)return;
  bucket.all.remove(pile);bucket.available.remove(pile);for(var key:previous.kinds()){var members=bucket.kinds.get(key);if(members!=null){members.remove(pile);if(members.isEmpty())bucket.kinds.remove(key);}}
  if(bucket.all.isEmpty())index.cells.remove(previous.cell());
 }
 static void update(MixedPileEntity pile){
  remove(pile);if(!(pile.level() instanceof ServerLevel level)||!pile.isAddedToLevel()||pile.isRemoved()||pile.total()==0)return;
  var index=LEVELS.computeIfAbsent(level,ignored->new Index());long cell=cell(pile);var kinds=Set.copyOf(pile.indexKinds());index.members.put(pile,new Membership(cell,kinds));
  var bucket=index.cells.computeIfAbsent(cell,ignored->new Bucket());bucket.limits();bucket.all.add(pile);if(pile.entryCount()<bucket.entriesLimit&&pile.total()<bucket.totalLimit)bucket.available.add(pile);
  for(var key:kinds)bucket.kinds.computeIfAbsent(key,ignored->new LinkedHashSet<>()).add(pile);
 }
 static void moved(MixedPileEntity pile){
  if(!(pile.level() instanceof ServerLevel level)||!pile.isAddedToLevel())return;var index=LEVELS.get(level);var member=index==null?null:index.members.get(pile);if(member==null||member.cell()!=cell(pile))update(pile);
 }
 static List<MixedPileEntity> nearby(ServerLevel level,AABB area,ItemResource kind){
  var index=LEVELS.get(level);if(index==null)return List.of();var found=new LinkedHashSet<MixedPileEntity>();var cells=area.inflate(.5);
  for(int x=cell(cells.minX);x<=cell(cells.maxX);x++)for(int y=cell(cells.minY);y<=cell(cells.maxY);y++)for(int z=cell(cells.minZ);z<=cell(cells.maxZ);z++){
   var bucket=index.cells.get(BlockPos.asLong(x,y,z));if(bucket==null)continue;bucket.limits();found.addAll(bucket.available);var matching=bucket.kinds.get(kind);if(matching!=null)found.addAll(matching);
  }
  found.removeIf(p->p.isRemoved()||!p.getBoundingBox().intersects(area));return List.copyOf(found);
 }
 private MixedPileIndex(){}
}
