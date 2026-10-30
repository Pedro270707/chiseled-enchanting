package net.pedroricardo.chiseledenchanting;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;

public class ChiseledEnchantingTags {
    public static final TagKey<Enchantment> NOT_OBTAINABLE_FROM_CHISELED_BOOKSHELF = TagKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ChiseledEnchanting.MOD_ID, "not_obtainable_from_chiseled_bookshelf"));
}
