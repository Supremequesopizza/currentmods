package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.bultach_utils;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

import static data.scripts.utils.bultach_utils.lerp;

public class bt_old_beam_effect implements BeamEffectPlugin {
	IntervalUtil interval = new IntervalUtil(0.2f, 0.2f);
	final float ARC_DAMAGE_AMOUNT = 85f;
	final float ARC_SPREAD_ANGLE = 25f;
	final float ARC_DISTANCE = 0.5f;
	private Color COLOR = new Color(255, 26, 26, 225);

	private float Damage = 50f;
	private static Logger log = Global.getLogger(bt_old_beam_effect.class);

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		interval.advance(amount);
		if (!interval.intervalElapsed()) return;

		CombatEntityAPI target = beam.getDamageTarget();
		if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
			ShipAPI ship = (ShipAPI) target;
			boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
			if (!hitShield) return;

			Vector2f startloc = lerp(
					beam.getFrom(),
					beam.getTo(),
					Misc.random.nextFloat() * ARC_DISTANCE + (1f - ARC_DISTANCE)
			);

			float angle_from_target = VectorUtils.getAngle(target.getLocation(), beam.getTo());
			Vector2f endloc = MathUtils.getPointOnCircumference(
					ship.getShield().getLocation(),
					ship.getShield().getRadius(),
					angle_from_target + bultach_utils.random_between(-ARC_SPREAD_ANGLE, ARC_SPREAD_ANGLE)
			);

			if (!ship.getShield().isWithinArc(endloc)) return;

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
					DamageType.KINETIC,
					0f,
					false,
					true,
					beam.getSource()
			);

			engine.spawnEmpArcPierceShields(
					beam.getSource(),
					beam.getFrom(),
					beam.getSource(),
					beam.getSource(),
					DamageType.ENERGY,
					Damage * 0.015f,
					Damage * 0.015f,
					100000f,
					null,
					10f,
					COLOR,
					COLOR
			);

			// shield interference system
			{
				ship = (ShipAPI) target;
				hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
				if (!hitShield) return;

				ShipAPI shipTarget = (ShipAPI) target;

				if (!shipTarget.hasListenerOfClass(bt_shieldinterference.class)) {
					shipTarget.addListener(new bt_shieldinterference(shipTarget));
				}

				List<bt_shieldinterference> listeners = shipTarget.getListeners(bt_shieldinterference.class);
				if (listeners.isEmpty()) return;

				bt_shieldinterference listener = listeners.get(0);
				if (listener == null) return;
				if (listener.stacks.size() < 50) {
					listener.stacks.add(new bt_shieldinterference.InterferenceStack(6f));
				}
				return;
			}
		}
	}
}
