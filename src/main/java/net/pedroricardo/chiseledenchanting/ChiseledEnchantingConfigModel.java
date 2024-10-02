package net.pedroricardo.chiseledenchanting;

import com.mojang.datafixers.util.Function3;
import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.RangeConstraint;

@Config(name = "chiseledenchanting", wrapperName = "ChiseledEnchantingConfig")
public class ChiseledEnchantingConfigModel {
    public boolean allowBookEnchanting = true;
    public ProbabilityType probabilityType = ProbabilityType.EXPONENTIAL;
    @RangeConstraint(min = 0.0, max = 1.0)
    public float firstBookProbability = 1.0f / 4.0f;
    @RangeConstraint(min = 0.0, max = 1.0)
    public float tenthBookProbability = 1.0f / 100.0f;
    public boolean chiseledBookshelvesProvidePower = false;
    @RangeConstraint(min = 0.0, max = 6.0)
    public int booksNecessaryForPower = 0;
    @RangeConstraint(min = 0.0, max = 1.0)
    public float substituteEnchantmentChance = 0.0f;

    public enum ProbabilityType {
        LINEAR((zero, ten, i) -> zero + (ten - zero) * (i / 10.0f)),
        EXPONENTIAL((zero, ten, i) -> zero * (float) Math.pow(ten / zero, i / 10.0f)),
        QUADRATIC((zero, ten, i) -> zero + (ten - zero) * (float) Math.pow(i / 10.0f, 2));

        private Function3<Float, Float, Integer, Float> function;

        ProbabilityType(Function3<Float, Float, Integer, Float> function) {
            this.function = function;
        }

        public float getProbability(float zero, float ten, int index) {
            return Math.min(this.function.apply(zero, ten, index), 1.0f);
        }
    }
}
