package net.pedroricardo.chiseledenchanting;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.RangeConstraint;

import java.util.function.Function;

@Config(name = "chiseledenchanting", wrapperName = "ChiseledEnchantingConfig")
public class ChiseledEnchantingConfigModel {
    public boolean allowBookEnchanting = true;
    public ProbabilityType probabilityType = ProbabilityType.EXPONENTIAL_INCREASE;
    @RangeConstraint(min = 0.0, max = 6.0)
    public int booksNecessaryForPower = 0;
    @RangeConstraint(min = 0.0, max = 1.0)
    public float subtituteEnchantmentChance = 0.0f;

    public enum ProbabilityType {
        LINEAR_INCREASE(i -> Math.min(0.05f + (0.95f * (i / 150f)), 1.0f)),
        LINEAR_DECREASE(i -> Math.max(1.0f - (0.95f * (i / 150f)), 0.05f)),
        EXPONENTIAL_INCREASE(i -> 0.05f + 0.95f * (1.0f - (float) Math.pow(Math.E, -0.0005 * i))),
        EXPONENTIAL_DECREASE(i -> Math.max(0.05f + (0.95f * (float) Math.pow(Math.E, -0.0005 * i)), 0.05f)),
        QUADRATIC_INCREASE(i -> {
            float normalized = i / 150f;
            return Math.min(0.05f + 0.95f * normalized * normalized, 1.0f);
        }),
        QUADRATIC_DECREASE(i -> {
            float normalized = i / 150f;
            return Math.max(1.0f - 0.95f * normalized * normalized, 0.05f);
        });

        private Function<Integer, Float> function;

        ProbabilityType(Function<Integer, Float> function) {
            this.function = function;
        }

        public float getProbability(int index) {
            return this.function.apply(index);
        }
    }
}
