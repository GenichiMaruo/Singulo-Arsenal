package io.github.genichimaruo.singuloarsenal.weapon;

import io.github.genichimaruo.singuloarsenal.core.Powered;

/** Marker for hand-held gear. Implementations keep their tooltip text under {@code tooltip.singulo_arsenal.<item id>.*}. */
public interface ArsenalWeapon extends Powered {
    int stage();
}
