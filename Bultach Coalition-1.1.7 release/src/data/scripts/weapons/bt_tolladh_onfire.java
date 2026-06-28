
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;
import org.magiclib.util.MagicLensFlare;

import java.awt.*;
import java.util.ArrayList;


public class bt_tolladh_onfire implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {

	public
	void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {


			}


	@Override
	public
	void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (weapon.getChargeLevel() == 1) {
			if (MagicRender.screenCheck(1, weapon.getLocation())) {
				engine.addHitParticle(weapon.getLocation(), weapon.getShip().getVelocity(), 150, 0.4f, 0.15f, new Color(255, 253, 244, 200));
				engine.addHitParticle(weapon.getLocation(), weapon.getShip().getVelocity(), 750, 0.2f, 0.4f, Color.WHITE);
				MagicLensFlare.createSharpFlare(engine, weapon.getShip(), weapon.getLocation(), 5, 400, 0, Color.WHITE, Color.white);

			}

		}

	}
}