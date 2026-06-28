package data.scripts.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class bt_uirBeamEffect extends bt_uir_extra {
    private float beamLastDistance = 0.0F;
    private static final Color MUZZLE_FLASH_COLOR = new Color(249, 220, 255, 237);

    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        super.advance(amount, engine, beam);
        ShipAPI ship = beam.getSource();
        float angle = beam.getWeapon().getCurrAngle();
        float distance = MathUtils.getDistance(beam.getFrom(), beam.getTo());
        if (!(distance - this.beamLastDistance <= 20.0F)) {
            for(int i = (int)this.beamLastDistance; (float)i <= distance; i += 20) {
                for(int j = -1; j <= 1; j += 2) {
                    Vector2f point = MathUtils.getPointOnCircumference(beam.getFrom(), (float)i, angle);
                    Vector2f vel = MathUtils.getPointOnCircumference(ship.getVelocity(), 150.0F, angle + 90.0F * (float)j);
                    engine.spawnExplosion(point, vel, MUZZLE_FLASH_COLOR, 15.0F, 0.3F);
                }
            }

            this.beamLastDistance = distance;
        }
    }
}
