package me.frankv.staaaaaaaaaaaack;


import me.frankv.staaaaaaaaaaaack.config.StxckClientConfig;
import me.frankv.staaaaaaaaaaaack.config.StxckCommonConfig;
import me.frankv.staaaaaaaaaaaack.mixin.ItemEntityAccessor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

public class Stxck {
    public static final String MODID = "staaaaaaaaaaaack";

    public static final TagKey<Item> BLACK_LIST_TAG = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "blacklist"));
    public static final String EXTRA_ITEM_COUNT_TAG = "StxckExtraItemCount";

    public static StxckCommonConfig commonConfig;
    public static StxckClientConfig clientConfig;



    private static boolean itemMatches(ItemStack stack) {
        for (String entry : commonConfig.getItemList()) {
            boolean tag = entry.startsWith("#");
            Identifier id = Identifier.tryParse(tag ? entry.substring(1) : entry);
            if (id == null) continue;
            if (tag ? stack.is(TagKey.create(Registries.ITEM, id))
                    : BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id)) return true;
        }
        return false;
    }

    public static void refillItemStack(ItemEntity entity) {
        if (entity.level().isClientSide() || entity.isRemoved()) return;
        int extra = getExtraItemCount(entity);
        ItemStack stack = entity.getItem();
        if (extra <= 0 || stack.isEmpty()) return;
        int refill = Math.min(Math.max(0, stack.getMaxStackSize() - stack.getCount()), extra);
        if (refill > 0) {
            setExtraItemCount(entity, extra - refill);
            entity.setItem(stack.copyWithCount(stack.getCount() + refill));
        }
    }

    public static boolean areMergable(ItemEntity itemEntity, ItemEntity itemEntity1) {
        if (itemEntity == itemEntity1 || !isMergable(itemEntity) || !isMergable(itemEntity1)
                || isBlackListItem(itemEntity) || isBlackListItem(itemEntity1)) return false;
        if (getTotalCount(itemEntity) + getTotalCount(itemEntity1) > commonConfig.getMaxSize()) return false;

        var itemStack = itemEntity.getItem();
        var itemStack1 = itemEntity1.getItem();

        return ItemStack.isSameItemSameComponents(itemStack, itemStack1);
    }

    public static void grow(ItemEntity entity, int count) {
        setExtraItemCount(entity, Math.addExact(getExtraItemCount(entity), count));
        refillItemStack(entity);
    }

    public static boolean isMergable(ItemEntity entity) {
        var accessor = (ItemEntityAccessor) entity;
        var pickupDelay = accessor.getPickupDelay();
        var age = accessor.getAge();
        return entity.isAlive() && ((PileState) entity).stxck$mergingAllowed() && !entity.getItem().isEmpty() && pickupDelay != 32767
                && age != -32768 && age < entity.lifespan
                && Math.max(entity.tickCount, ((PileState) entity).stxck$elapsedTicks()) >= commonConfig.getMergeDelayTicks();
    }

    public static Optional<String> getOverlayText(ItemEntity entity) {
        boolean alwaysShowItemCount = clientConfig.isAlwaysShowItemCount();

        return switch (clientConfig.getOverlayDisplayMode()) {
            case ITEM_COUNT -> getTotalCountOverlayText(entity, alwaysShowItemCount);
            case STACK_COUNT -> {
                var maxStackSize = entity.getItem().getMaxStackSize();
                long stackCount = (getTotalCount(entity) + maxStackSize - 1L) / maxStackSize;
                var show = stackCount > 1 || alwaysShowItemCount;
                yield show ? Optional.of(String.format("%dx", stackCount)) : Optional.empty();
            }
        };
    }

    private static Optional<String> getTotalCountOverlayText(ItemEntity entity, boolean alwaysShowItemCount) {
        var total = getTotalCount(entity);

        if (total >= 1_000_000_000) {
            return Optional.of(String.format("%.3fB", total / 1_000_000_000f));
        }
        if (total >= 1_000_000) {
            return Optional.of(String.format("%.2fM", total / 1_000_000f));
        }
        if (total >= 10_000) {
            return Optional.of(String.format("%.1fK", total / 1_000f));
        }
        if (alwaysShowItemCount || total > entity.getItem().getMaxStackSize()) {
            return Optional.of(String.valueOf(total));
        }
        return Optional.empty();
    }

    public static long getTotalCount(ItemEntity entity) {
        return (long) entity.getItem().getCount() + getExtraItemCount(entity);
    }

    public static int getExtraItemCount(ItemEntity entity) {
        return entity.hasData(StxckAttachments.EXTRA) ? entity.getData(StxckAttachments.EXTRA) : 0;
    }

    public static void setExtraItemCount(ItemEntity entity, int count) {
        if (count < 0) throw new IllegalArgumentException("Negative reserve");
        if (getExtraItemCount(entity) != count) entity.setData(StxckAttachments.EXTRA, count);
        if (count > 0 && !entity.getItem().isEmpty()) {
            ((PileState) entity).stxck$remember(entity.getItem());
        }
    }

    public static void tryToMerge(ItemEntity itemEntity, ItemEntity itemEntity1) {
        tryToMerge(itemEntity, itemEntity1, false);
    }

    public static void tryToMerge(ItemEntity itemEntity, ItemEntity itemEntity1, boolean mergeToExisted) {
        if (Objects.equals(itemEntity.getTarget(), itemEntity1.getTarget())
                && areMergable(itemEntity, itemEntity1)
        ) {
            if (mergeToExisted || getTotalCount(itemEntity1) < getTotalCount(itemEntity)) {
                merge(itemEntity, itemEntity1);
            } else {
                merge(itemEntity1, itemEntity);
            }
        }
    }

    public static void merge(ItemEntity consumer, ItemEntity supplier) {
        if (consumer.level().isClientSide() || !areMergable(consumer, supplier)) return;
        var consumerAccessor = (ItemEntityAccessor) consumer;
        var supplierAccessor = (ItemEntityAccessor) supplier;

        consumerAccessor.setPickupDelay(Math.max(consumerAccessor.getPickupDelay(), supplierAccessor.getPickupDelay()));
        consumerAccessor.setAge(Math.min(consumerAccessor.getAge(), supplierAccessor.getAge()));

        long total = getTotalCount(consumer) + getTotalCount(supplier);
        int visible = consumer.getItem().getCount();
        setExtraItemCount(consumer, Math.toIntExact(total - visible));
        refillItemStack(consumer);

        setExtraItemCount(supplier, 0);
        supplier.setItem(ItemStack.EMPTY);
        supplier.discard();
    }

    public static boolean isBlackListItem(ItemEntity itemEntity) {
        // Custom entity types may carry state/behavior a vanilla remainder cannot reproduce.
        if (itemEntity.getType() != EntityType.ITEM) return true;
        var dim = itemEntity.level().dimension().identifier();
        if (commonConfig.getDimensionList().contains(dim.toString()) ^ commonConfig.isDimensionListAsWhitelist()) {
            return true;
        }

        var itemStack = itemEntity.getItem();
        if (!commonConfig.isEnableForUnstackableItem() && itemStack.getMaxStackSize() == 1) {
            return true;
        }

        return itemStack.is(BLACK_LIST_TAG)
                || (itemMatches(itemStack) ^ commonConfig.isItemListAsWhitelist());
    }
}
