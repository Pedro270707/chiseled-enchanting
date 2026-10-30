package net.pedroricardo.chiseledenchanting.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.Holder;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.pedroricardo.chiseledenchanting.ChiseledEnchanting;
import net.pedroricardo.chiseledenchanting.ChiseledEnchantingTags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mixin(EnchantmentMenu.class)
public class EnchantmentMenuMixin {
    @Shadow @Final private RandomSource random;

    @Unique
    private final List<EnchantmentInstance> possibleEnchantments = new ArrayList<>();
    @Unique
    private int bookAmount = 0;

    @Inject(method = "lambda$slotsChanged$0", at = @At(value = "HEAD"))
    private void chiseledenchanting$getEnchantments(ItemStack itemStack, Level world, BlockPos tablePos, CallbackInfo ci) {
        this.possibleEnchantments.clear();
        this.bookAmount = 0;
        for (BlockPos blockPos : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (!(world.getBlockEntity(tablePos.offset(blockPos)) instanceof ChiseledBookShelfBlockEntity bookshelf)) {
                continue;
            }
            for (int i = 0; i < bookshelf.getContainerSize(); i++) {
                if (bookshelf.getItem(i).is(Items.ENCHANTED_BOOK)) {
                    ++this.bookAmount;
                    Set<EnchantmentInstance> possibleEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(bookshelf.getItem(i))
                            .entrySet()
                            .stream()
                            .map(entry -> new EnchantmentInstance(entry.getKey(), entry.getIntValue()))
                            .collect(Collectors.toSet());
                    possibleEnchantments.removeIf(entry -> entry.enchantment().is(ChiseledEnchantingTags.NOT_OBTAINABLE_FROM_CHISELED_BOOKSHELF));
                    this.possibleEnchantments.addAll(possibleEnchantments);
                }
            }
        }
    }

    @WrapOperation(
            method = "lambda$slotsChanged$0",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/EnchantingTableBlock;isValidBookShelf(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean chiseledenchanting$chiseledBookshelfProvidesPower(Level world, BlockPos tablePos, BlockPos providerOffset, Operation<Boolean> original) {
        if (!original.call(world, tablePos, providerOffset)) return false;
        if (!(world.getBlockEntity(tablePos.offset(providerOffset)) instanceof ChiseledBookShelfBlockEntity bookshelf)) return true;

        int bookCount = 0;
        for (int i = 0; i < bookshelf.getContainerSize(); ++i) {
            ItemStack itemStack = bookshelf.getItem(i);
            if (!itemStack.isEmpty()) ++bookCount;
        }
        return bookCount >= ChiseledEnchanting.CONFIG.booksNecessaryForPower();
    }

    @ModifyReturnValue(method = "getEnchantmentList", at = @At("RETURN"))
    private List<EnchantmentInstance> chiseledenchanting$addEnchantments(List<EnchantmentInstance> list, @Local(ordinal = 0, argsOnly = true) ItemStack stack, @Local(ordinal = 1, argsOnly = true) int level) {
        List<EnchantmentInstance> possibleEnchantments = this.possibleEnchantments.stream().filter(e -> !list.contains(e) && (e.enchantment().value().canEnchant(stack) || (stack.is(Items.BOOK) && ChiseledEnchanting.CONFIG.allowBookEnchanting())) && EnchantmentHelper.isEnchantmentCompatible(EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet(), e.enchantment())).toList();
        if (possibleEnchantments.isEmpty()) {
            return list;
        }

        for (int i = 0; i < this.bookAmount; i++) {
            if (this.random.nextFloat() >= getProbability(i)) continue;
            Map<Holder<Enchantment>, EnchantmentInstance> maxLevelEnchantments = possibleEnchantments.stream()
                        .collect(Collectors.toMap(
                                EnchantmentInstance::enchantment,
                                Function.identity(),
                                (entry1, entry2) -> entry1.level() > entry2.level() ? entry1 : entry2
                        ));

            List<EnchantmentInstance> entries = EnchantmentHelper.selectEnchantment(this.random, stack, level / (int) Math.pow(2, list.size() - 1), possibleEnchantments.stream().map(EnchantmentInstance::enchantment));
            if (entries.isEmpty()) return list;
            entries = entries.stream().map(e -> {
                for (int j = maxLevelEnchantments.get(e.enchantment()).level(); j >= Math.min(e.enchantment().value().getMinLevel(), maxLevelEnchantments.get(e.enchantment()).level()); --j) {
                    if (level < 1 + 11 * (j - 1) || level > 21 + 11 * (j - 1)) continue;
                    return new EnchantmentInstance(e.enchantment(), j);
                }
                return new EnchantmentInstance(e.enchantment(), e.enchantment().value().getMinLevel());
            }).collect(Collectors.toCollection(ArrayList::new));
            if (stack.is(Items.BOOK)) {
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
}
