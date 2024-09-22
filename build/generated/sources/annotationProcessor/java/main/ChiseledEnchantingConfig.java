package net.pedroricardo.chiseledenchanting;

import blue.endless.jankson.Jankson;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.util.Observable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ChiseledEnchantingConfig extends ConfigWrapper<net.pedroricardo.chiseledenchanting.ChiseledEnchantingConfigModel> {

    public final Keys keys = new Keys();

    private final Option<java.lang.Boolean> allowBookEnchanting = this.optionForKey(this.keys.allowBookEnchanting);
    private final Option<net.pedroricardo.chiseledenchanting.ChiseledEnchantingConfigModel.ProbabilityType> probabilityType = this.optionForKey(this.keys.probabilityType);
    private final Option<java.lang.Integer> booksNecessaryForPower = this.optionForKey(this.keys.booksNecessaryForPower);
    private final Option<java.lang.Float> subtituteEnchantmentChance = this.optionForKey(this.keys.subtituteEnchantmentChance);

    private ChiseledEnchantingConfig() {
        super(net.pedroricardo.chiseledenchanting.ChiseledEnchantingConfigModel.class);
    }

    private ChiseledEnchantingConfig(Consumer<Jankson.Builder> janksonBuilder) {
        super(net.pedroricardo.chiseledenchanting.ChiseledEnchantingConfigModel.class, janksonBuilder);
    }

    public static ChiseledEnchantingConfig createAndLoad() {
        var wrapper = new ChiseledEnchantingConfig();
        wrapper.load();
        return wrapper;
    }

    public static ChiseledEnchantingConfig createAndLoad(Consumer<Jankson.Builder> janksonBuilder) {
        var wrapper = new ChiseledEnchantingConfig(janksonBuilder);
        wrapper.load();
        return wrapper;
    }

    public boolean allowBookEnchanting() {
        return allowBookEnchanting.value();
    }

    public void allowBookEnchanting(boolean value) {
        allowBookEnchanting.set(value);
    }

    public net.pedroricardo.chiseledenchanting.ChiseledEnchantingConfigModel.ProbabilityType probabilityType() {
        return probabilityType.value();
    }

    public void probabilityType(net.pedroricardo.chiseledenchanting.ChiseledEnchantingConfigModel.ProbabilityType value) {
        probabilityType.set(value);
    }

    public int booksNecessaryForPower() {
        return booksNecessaryForPower.value();
    }

    public void booksNecessaryForPower(int value) {
        booksNecessaryForPower.set(value);
    }

    public float subtituteEnchantmentChance() {
        return subtituteEnchantmentChance.value();
    }

    public void subtituteEnchantmentChance(float value) {
        subtituteEnchantmentChance.set(value);
    }


    public static class Keys {
        public final Option.Key allowBookEnchanting = new Option.Key("allowBookEnchanting");
        public final Option.Key probabilityType = new Option.Key("probabilityType");
        public final Option.Key booksNecessaryForPower = new Option.Key("booksNecessaryForPower");
        public final Option.Key subtituteEnchantmentChance = new Option.Key("subtituteEnchantmentChance");
    }
}

