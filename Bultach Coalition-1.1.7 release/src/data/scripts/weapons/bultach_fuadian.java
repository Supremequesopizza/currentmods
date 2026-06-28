package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.bultach_utils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import static data.scripts.utils.bultach_utils.lerp;

public class bultach_fuadian implements BeamEffectPlugin
{
    IntervalUtil interval = new IntervalUtil(0.1f, 0.2f); // min/max duration between arcs, in seconds
    final float ARC_DAMAGE_AMOUNT = 200f; // damage type will match the beam's damage type

    // how far (in degrees) around the shield the arcs will hit
    // note that arcs that wouldn't hit shields get discarded
    // so if this is a large number, it'll  have reduced effectiveness against narrow shields
    final float ARC_SPREAD_ANGLE = 35f;

    // how far along the beam the arcs start happening- 0 will be the whole length, 1 is from only the endpoint
    final float ARC_DISTANCE = 0.75f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam)
    {
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;
        CombatEntityAPI target = beam.getDamageTarget();
        if (target instanceof ShipAPI && beam.getBrightness() >= 1f)
        {
            ShipAPI ship = (ShipAPI) target;
            boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
            if (!hitShield) return;

            // get random point along beam length, somewhere towards the second half of the beam
            Vector2f startloc = lerp(beam.getFrom(), beam.getTo(), Misc.random.nextFloat() * ARC_DISTANCE + (1f - ARC_DISTANCE));

            // get random point on shield within some angle of the impact location
            float angle_from_target = VectorUtils.getAngle(target.getLocation(), beam.getTo());
            Vector2f endloc = MathUtils.getPointOnCircumference(
                    ship.getShield().getLocation(),
                    ship.getShield().getRadius(),
                    angle_from_target + bultach_utils.random_between(-ARC_SPREAD_ANGLE, ARC_SPREAD_ANGLE)
            );
            if (!ship.getShield().isWithinArc(endloc))
                return;
            // emp arcs share their colors with the beam
            engine.spawnEmpArcVisual(
                    startloc,
                    null,
                    endloc,
                    ship,
                    20f,
                    beam.getFringeColor(),
                    beam.getCoreColor()
            );
            engine.applyDamage(
                    ship,
                    endloc,
                    ARC_DAMAGE_AMOUNT,
                    beam.getDamage().getType(),
                    0f,
                    false,
                    true,
                    beam.getSource()
            );
            Global.getSoundPlayer().playSound("tachyon_lance_emp_impact", 1f, 1f, bultach_utils.lerp(startloc, endloc, 0.5f), Misc.ZERO);
        }
    }
}
