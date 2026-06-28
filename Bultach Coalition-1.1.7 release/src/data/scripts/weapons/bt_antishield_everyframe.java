package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class bt_antishield_everyframe implements EveryFrameWeaponEffectPlugin {
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        if (!weapon.getShip().hasListenerOfClass(shieldFukkerDamage.class))
        {
            weapon.getShip().addListener(new shieldFukkerDamage());
        }
    }

    private class shieldFukkerDamage implements DamageDealtModifier
    {
        private shieldFukkerDamage() {
        }

        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {

            if (shieldHit && param instanceof DamagingProjectileAPI && target != null && target instanceof ShipAPI) {
                DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
                if(proj.getWeapon() != null && proj.getWeapon().getId().equals("ork_bolta")) {
                    ShipAPI ship = (ShipAPI) target;
                    damage.setDamage(damage.getDamage() * (1f / ship.getShield().getFluxPerPointOfDamage()));
                }
            }
            return null;
        }
    }
}
