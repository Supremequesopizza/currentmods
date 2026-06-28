package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class bt_oldbeam_flare_effect implements EveryFrameWeaponEffectPlugin {
    private static final String TARGET_WEAPON_ID = "ork_old_laser";

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null) return;

        if (!weapon.getShip().hasListenerOfClass(BeamRangeDebuffListener.class)) {
            weapon.getShip().addListener(new BeamRangeDebuffListener());
        }

        if (weapon.getChargeLevel() > 0f && !weapon.getBeams().isEmpty()) {
            BeamAPI beam = weapon.getBeams().get(0);

            Vector2f jitterFrom = MathUtils.getRandomPointInCircle(beam.getFrom(), 2f);
            MagicRender.singleframe(
                    Global.getSettings().getSprite("fx", "bt_flare1"),
                    jitterFrom,
                    new Vector2f(120f, 20f),
                    0f,
                    new Color(255, 92, 92, 150),
                    true
            );
            MagicRender.singleframe(
                    Global.getSettings().getSprite("fx", "bt_flare1"),
                    jitterFrom,
                    new Vector2f(90f, 10f),
                    0f,
                    new Color(255, 200, 200, 250),
                    true
            );

            if (beam.getDamageTarget() != null) {
                Vector2f jitterTo = MathUtils.getRandomPointInCircle(beam.getTo(), 3f);

                MagicRender.singleframe(
                        Global.getSettings().getSprite("fx", "bt_flare1"),
                        jitterTo,
                        new Vector2f(200f, 45f),
                        0f,
                        new Color(255, 118, 118, 195),
                        true
                );
                MagicRender.singleframe(
                        Global.getSettings().getSprite("fx", "bt_flare1"),
                        jitterTo,
                        new Vector2f(170f, 25f),
                        0f,
                        new Color(255, 240, 240, 250),
                        true
                );
            }
        }
    }

    private static class BeamRangeDebuffListener implements WeaponRangeModifier {
        @Override
        public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            return 0f;
        }

        @Override
        public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            return 1f;
        }

        @Override
        public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            if (weapon.getSlot() != null
                    && weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.ENERGY
                    && TARGET_WEAPON_ID.equals(weapon.getSpec().getWeaponId())) {
                return -200f;
            }
            return 0f;
        }
    }
}
