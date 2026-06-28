package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class bt_pather_smoke implements EveryFrameWeaponEffectPlugin {

    private float timer = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null || engine.isPaused()) return;

        if (ship.getSystem() != null && ship.getSystem().isActive()) {
            timer += amount;
            if (timer >= 0.05f) {
                Vector2f loc = new Vector2f(weapon.getLocation());
                Vector2f vel = new Vector2f(
                        (Misc.random.nextFloat() - 0.5f) * 40f,
                        (Misc.random.nextFloat() - 0.5f) * 40f
                );
                engine.addSmokeParticle(
                        loc,
                        vel,
                        15f,
                        0.01f,
                        1f,
                        new Color(126, 92, 102, 190)
                );
                timer = 0f;
            }
        } else {
            timer = 0f;
        }
    }
}
