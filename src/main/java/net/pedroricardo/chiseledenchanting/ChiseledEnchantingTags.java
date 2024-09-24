package net.pedroricardo.chiseledenchanting;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ChiseledEnchantingTags {
    public static final TagKey<Enchantment> NOT_OBTAINABLE_FROM_CHISELED_BOOKSHELF = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(ChiseledEnchanting.MOD_ID, "not_obtainable_from_chiseled_bookshelf"));
}
