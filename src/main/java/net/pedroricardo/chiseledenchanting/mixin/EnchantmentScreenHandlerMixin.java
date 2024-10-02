package net.pedroricardo.chiseledenchanting.mixin;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.util.Util;
import net.minecraft.util.collection.Weighting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.pedroricardo.chiseledenchanting.ChiseledEnchanting;
import net.pedroricardo.chiseledenchanting.ChiseledEnchantingTags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(EnchantmentScreenHandler.class)
public class EnchantmentScreenHandlerMixin {
    @Shadow @Final private Random random;
    @Unique
    private final List<EnchantmentLevelEntry> possibleEnchantments = new ArrayList<>();
    @Unique
    private int bookAmount = 0;

    @Inject(method = "method_17411", at = @At(value = "HEAD"))
    private void chiseledenchanting$getEnchantments(ItemStack itemStack, World world, BlockPos tablePos, CallbackInfo ci) {
        this.possibleEnchantments.clear();
        this.bookAmount = 0;
        for (BlockPos blockPos : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (!(world.getBlockEntity(tablePos.add(blockPos)) instanceof ChiseledBookshelfBlockEntity bookshelf)) {
                continue;
            }
            for (int i = 0; i < bookshelf.size(); i++) {
                if (bookshelf.getStack(i).isOf(Items.ENCHANTED_BOOK)) {
                    ++this.bookAmount;
                    Set<EnchantmentLevelEntry> possibleEnchantments = EnchantmentHelper.fromNbt(EnchantedBookItem.getEnchantmentNbt(bookshelf.getStack(i)))
                            .entrySet()
                            .stream()
                            .map(entry -> new EnchantmentLevelEntry(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toSet());
                    possibleEnchantments.removeIf(entry -> Registries.ENCHANTMENT.getEntry(entry.enchantment).isIn(ChiseledEnchantingTags.NOT_OBTAINABLE_FROM_CHISELED_BOOKSHELF));
                    this.possibleEnchantments.addAll(possibleEnchantments);
                }
            }
        }
    }

    @WrapOperation(method = "method_17411", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/EnchantingTableBlock;canAccessBookshelf(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean chiseledenchanting$chiseledBookshelfProvidesPower(World world, BlockPos tablePos, BlockPos providerOffset, Operation<Boolean> original) {
        if (!(world.getBlockEntity(tablePos.add(providerOffset)) instanceof ChiseledBookshelfBlockEntity bookshelf)) return original.call(world, tablePos, providerOffset);
        if (!ChiseledEnchanting.CONFIG.chiseledBookshelvesProvidePower()) return false;
        if (!world.isAir(tablePos.add(providerOffset.getX() / 2, providerOffset.getY(), providerOffset.getZ() / 2))) return false;

        int bookCount = 0;
        for (int i = 0; i < bookshelf.size(); ++i) {
            ItemStack itemStack = bookshelf.getStack(i);
            if (!itemStack.isEmpty()) ++bookCount;
        }
        return bookCount >= ChiseledEnchanting.CONFIG.booksNecessaryForPower();
    }

    @ModifyReturnValue(method = "generateEnchantments", at = @At("RETURN"))
    private List<EnchantmentLevelEntry> chiseledenchanting$addEnchantments(List<EnchantmentLevelEntry> list, @Local(ordinal = 0, argsOnly = true) ItemStack stack, @Local(ordinal = 1, argsOnly = true) int level) {
        List<EnchantmentLevelEntry> possibleEnchantments = this.possibleEnchantments.stream().filter(e -> !list.contains(e) && (e.enchantment.isAcceptableItem(stack) || (stack.isOf(Items.BOOK) && ChiseledEnchanting.CONFIG.allowBookEnchanting())) && EnchantmentHelper.isCompatible(EnchantmentHelper.fromNbt(stack.getEnchantments()).keySet(), e.enchantment)).toList();
        if (possibleEnchantments.isEmpty()) {
            return list;
        }

        for (int i = 0; i < this.bookAmount; i++) {
            float probability = getProbability(i);
            if (this.random.nextFloat() >= probability) {
                continue;
            }
            Map<Enchantment, EnchantmentLevelEntry> maxLevelEnchantments = possibleEnchantments.stream()
                        .collect(Collectors.toMap(
                                e -> e.enchantment,
                                Function.identity(),
                                (entry1, entry2) -> entry1.level > entry2.level ? entry1 : entry2
                        ));

            List<EnchantmentLevelEntry> entries = generateEnchantments(this.random, stack, level / (int) Math.pow(2, list.size() - 1), possibleEnchantments.stream().map(e -> e.enchantment));
            if (entries.isEmpty()) return list;
            entries = entries.stream().map(e -> {
                for (int j = maxLevelEnchantments.get(e.enchantment).level; j >= Math.min(e.enchantment.getMinLevel(), maxLevelEnchantments.get(e.enchantment).level); --j) {
                    if (level < 1 + 11 * (j - 1) || level > 21 + 11 * (j - 1)) continue;
                    return new EnchantmentLevelEntry(e.enchantment, j);
                }
                return new EnchantmentLevelEntry(e.enchantment, e.enchantment.getMinLevel());
            }).collect(Collectors.toCollection(ArrayList::new));
            if (stack.isOf(Items.BOOK)) {
                if (!list.isEmpty()) list.remove(this.random.nextInt(list.size())); // this is separate, so you can get a book with only the desired enchantment even with substituteEnchantmentChance being equal to 0
                if (entries.size() > 1) entries.remove(this.random.nextInt(entries.size()));
            }
            if (this.random.nextFloat() < ChiseledEnchanting.CONFIG.substituteEnchantmentChance()) {
                for (int j = 0; j < entries.size(); j++) {
                    if (list.isEmpty()) break;
                    list.remove(this.random.nextInt(list.size()));
                }
            }
            list.addAll(entries);
            break;
        }
        return list;
    }

    @Unique
    private float getProbability(int index) {
        return ChiseledEnchanting.CONFIG.probabilityType().getProbability(ChiseledEnchanting.CONFIG.firstBookProbability(), ChiseledEnchanting.CONFIG.tenthBookProbability(), index);
    }

    @Unique
    private static List<EnchantmentLevelEntry> generateEnchantments(Random random, ItemStack stack, int level, Stream<Enchantment> possibleEnchantments) {
        List<EnchantmentLevelEntry> list = Lists.newArrayList();
        Item item = stack.getItem();
        int i = item.getEnchantability();
        if (i > 0) {
            level += 1 + random.nextInt(i / 4 + 1) + random.nextInt(i / 4 + 1);
            float f = (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;
            level = MathHelper.clamp(Math.round((float) level + (float) level * f), 1, Integer.MAX_VALUE);
            List<EnchantmentLevelEntry> list2 = getPossibleEntries(level, possibleEnchantments);
            if (!list2.isEmpty()) {
                Optional<EnchantmentLevelEntry> var10000 = Weighting.getRandom(random, list2);
                Objects.requireNonNull(list);
                var10000.ifPresent(list::add);

                while (random.nextInt(50) <= level) {
                    if (!list.isEmpty()) {
                        removeConflicts(list2, Util.getLast(list));
                    }

                    if (list2.isEmpty()) {
                        break;
                    }

                    var10000 = Weighting.getRandom(random, list2);
                    Objects.requireNonNull(list);
                    var10000.ifPresent(list::add);
                    level /= 2;
                }
            }

        }
        return list;
    }

    @Unique
    private static void removeConflicts(List<EnchantmentLevelEntry> possibleEntries, EnchantmentLevelEntry pickedEntry) {
        possibleEntries.removeIf(enchantmentLevelEntry -> !pickedEntry.enchantment.canCombine(enchantmentLevelEntry.enchantment));
    }

    @Unique
    private static List<EnchantmentLevelEntry> getPossibleEntries(int level, Stream<Enchantment> possibleEnchantments) {
        List<EnchantmentLevelEntry> list = Lists.newArrayList();
        possibleEnchantments.forEach((enchantmentx) -> {
            for(int j = enchantmentx.getMaxLevel(); j >= enchantmentx.getMinLevel(); --j) {
                if (level >= 1 + 11 * (j - 1) && level <= 21 + 11 * (j - 1)) {
                    list.add(new EnchantmentLevelEntry(enchantmentx, j));
                    break;
                }
            }
        });
        return list;
    }
}
