package me.frankv.staaaaaaaaaaaack.mixed;
import java.util.*;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.*;
/** Extraction-only transactional capability. No entity removal or packets before root commit. */
public final class MixedInventory extends SnapshotJournal<List<MixedEntry>> implements ResourceHandler<ItemResource> {
 private final MixedPileEntity pile;
 private List<MixedEntry> committed;
 List<MixedEntry> saveView(){return isInTransaction()&&committed!=null?committed:pile.entries;}
 MixedInventory(MixedPileEntity pile){this.pile=pile;}
 private MixedEntry entry(int index){return pile.isRemoved()||index<0||index>=pile.entries.size()?null:pile.entries.get(index);}
 public int size(){return pile.entries.size();}
 public ItemResource getResource(int index){var e=entry(index);return e==null||e.count==0?ItemResource.EMPTY:e.resource;}
 public long getAmountAsLong(int index){var e=entry(index);return e==null||e.item.isEmpty()?0:e.count;}
 public long getCapacityAsLong(int index,ItemResource resource){return isValid(index,resource)?MixedConfig.maxTotalItems.get():0;}
 public boolean isValid(int index,ItemResource resource){var e=entry(index);return e!=null&&!e.item.isEmpty()&&(resource.isEmpty()||resource.matches(e.item));}
 public int insert(int index,ItemResource resource,int amount,TransactionContext tx){TransferPreconditions.checkNonEmptyNonNegative(resource,amount);return 0;}
 public int extract(int index,ItemResource resource,int amount,TransactionContext tx){
  TransferPreconditions.checkNonEmptyNonNegative(resource,amount);var e=entry(index);
  if(pile.level().isClientSide()||pile.busy||e==null||e.item.isEmpty()||!resource.matches(e.item)||amount==0)return 0;
  int taken=Math.min(amount,e.count);if(taken==0)return 0;updateSnapshots(tx);e.count-=taken;return taken;
 }
 protected List<MixedEntry> createSnapshot(){var snapshot=pile.entries.stream().map(MixedEntry::copy).toList();if(committed==null)committed=snapshot;return snapshot;}
 protected void releaseSnapshot(List<MixedEntry> snapshot){if(snapshot==committed)committed=null;}
 protected void revertToSnapshot(List<MixedEntry> snapshot){pile.entries.clear();for(var e:snapshot)pile.entries.add(e.copy());}
 protected void onRootCommit(List<MixedEntry> original){pile.changed();}
}
