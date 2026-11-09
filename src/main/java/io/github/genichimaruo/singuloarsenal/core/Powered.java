package io.github.genichimaruo.singuloarsenal.core;

/** Gear with an internal FE tank. */
public interface Powered {
    int capacity();

    /** Collapsar-class gear also feeds itself from mass pellets in the wearer's inventory. */
    default boolean accretes() {
        return false;
    }
}
