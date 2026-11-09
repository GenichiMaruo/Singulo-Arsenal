package io.github.genichimaruo.singuloarsenal.module;

import io.github.genichimaruo.singuloarsenal.armor.SuitItem;
import io.github.genichimaruo.singuloarsenal.core.Powered;
import io.github.genichimaruo.singuloarsenal.weapon.ArsenalWeapon;
import io.github.genichimaruo.singuloarsenal.weapon.JetLanceItem;
import java.util.function.Predicate;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/** What can be built into gear, and which gear each one physically makes sense in. */
public enum ModuleType {
    BIT_DRONE("bit_drone_module", item -> item instanceof SuitItem suit && suit.getType() == ArmorItem.Type.CHESTPLATE),
    CAPACITOR("capacitor_module", item -> item instanceof Powered),
    PLATING("plating_module", item -> item instanceof SuitItem),
    THRUSTER("thruster_module", item -> item instanceof SuitItem suit && suit.getType() == ArmorItem.Type.BOOTS),
    COOLANT("coolant_module", item -> item instanceof ArsenalWeapon),
    AMPLIFIER("amplifier_module", item -> item instanceof ArsenalWeapon),
    METRIC_DRIVE("metric_drive_module", item -> item instanceof SuitItem),
    MELTDOWN("meltdown_module", item -> item instanceof JetLanceItem);

    public final String id;
    private final Predicate<Item> fits;

    ModuleType(String id, Predicate<Item> fits) {
        this.id = id;
        this.fits = fits;
    }

    public boolean fits(Item gear) {
        return fits.test(gear);
    }
}
