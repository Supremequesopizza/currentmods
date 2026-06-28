package data.scripts.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPluginWithReset;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class bt_uir_extra implements BeamEffectPluginWithReset {
    private static final Color PARTICLE_COLOR = new Color(246, 213, 255, 213);
    private static final Color PARTICLE_COLOR2 = new Color(255, 215, 240, 224);

    public void reset() {
    }

    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        WeaponAPI weapon = beam.getWeapon();
        ShipAPI ship = beam.getSource();
        float chargeLevel = beam.getBrightness() * beam.getBrightness();
        float particle_count_this_frame = 120.0F * chargeLevel * amount;
        float minBeamWidth = beam.getWidth() * 0.25F;
        float maxBeamWidth = beam.getWidth();

        for(Vector2f spawnPoint = MathUtils.getRandomPointInCircle(beam.getFrom(), maxBeamWidth * 0.5F); particle_count_this_frame > 0.0F; --particle_count_this_frame) {
            if (particle_count_this_frame >= 1.0F || (double)particle_count_this_frame < Math.random()) {
                float size = MathUtils.getRandomNumberInRange(minBeamWidth, maxBeamWidth);
                float speed = MathUtils.getRandomNumberInRange(100.0F, 200.0F);
                float angle = weapon.getCurrAngle() + MathUtils.getRandomNumberInRange(-14.0F, 14.0F);
                Vector2f velocity = MathUtils.getPointOnCircumference(ship.getVelocity(), speed, angle);
                engine.addHitParticle(spawnPoint, velocity, size, chargeLevel, 0.2F, MathUtils.getRandomNumberInRange(0.25F, 0.5F), Misc.interpolateColor(PARTICLE_COLOR, PARTICLE_COLOR2, MathUtils.getRandom().nextFloat()));
            }
        }

    }
}
