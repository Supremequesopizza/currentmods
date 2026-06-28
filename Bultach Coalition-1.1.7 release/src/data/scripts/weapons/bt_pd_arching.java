package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.magiclib.util.MagicLensFlare;
import org.magiclib.util.MagicRender;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

public class bt_pd_arching implements OnHitEffectPlugin {

    private final Color COLOR = new Color(255, 249, 231, 255);
    private final Color ARC_CORE = new Color(255, 249, 216, 200);
    private final Color ARC_FRINGE = new Color(255, 255, 255, 150);
    private static final float ARC_RANGE = 500f;
    private static final float ARC_DAMAGE = 0f;
    private static final float ARC_EMP = 0f;
    private static final float ARC_THICKNESS = 10f;
    private static final String ARC_SOUND = "bt_gestalt_arc_quiet";

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        ShipAPI source = projectile.getSource();
        if (source == null) return;

        Global.getSoundPlayer().playSound("bt_gestalt_arc_quiet", 1.5f, 0.1f, point, target.getVelocity());

        if (MagicRender.screenCheck(1, point)) {
            engine.addSmoothParticle(
                    point,
                    new Vector2f(),
                    800,
                    1,
                    0.05f,
                    Color.WHITE
            );

            for (int i = 0; i < 20; i++) {
                Vector2f loc = MathUtils.getRandomPointInCircle(new Vector2f(), 75);
                engine.addHitParticle(
                        new Vector2f(point.x + loc.x, point.y + loc.y),
                        loc,
                        5 + 5 * (float) Math.random(),
                        1,
                        1 + 2 * (float) Math.random(),
                        COLOR
                );
            }
            engine.addHitParticle(
                    point,
                    new Vector2f(),
                    500,
                    0.5f,
                    2f,
                    COLOR
            );

            for (int i = 0; i < 6; i++) {
                MagicLensFlare.createSharpFlare(engine, projectile.getSource(), MathUtils.getRandomPointInCircle(point, 100), 5 + 5 * (float) Math.random(), 250 + 250 * (float) Math.random(), 0, COLOR, Color.white);
            }
            MagicLensFlare.createSharpFlare(engine, projectile.getSource(), point, 30, 600, 0, COLOR, Color.white);
        }

        spawnArc(engine, source, point, target, target);
        spawnArc(engine, source, projectile.getWeapon().getLocation(), new SimpleEntity(point), source);


        List<CombatEntityAPI> validTargets = new ArrayList<>();
        List<MissileAPI> missiles = CombatUtils.getMissilesWithinRange(point, ARC_RANGE);
        List<ShipAPI> ships = CombatUtils.getShipsWithinRange(point, ARC_RANGE);

        for (MissileAPI missile : missiles) {
            if (missile != projectile && missile.getOwner() != source.getOwner()) {
                validTargets.add(missile);
            }
        }

        for (ShipAPI ship : ships) {
            if (ship.isFighter() && ship.isAlive() && !ship.isPhased() && ship.getOwner() != source.getOwner() && ship != target) {
                validTargets.add(ship);
            }
        }

        for (CombatEntityAPI arcTarget : validTargets) {
            spawnArc(engine, source, point, target, arcTarget);
        }
    }

    private void spawnArc(CombatEngineAPI engine, ShipAPI source, Vector2f from, CombatEntityAPI anchor, CombatEntityAPI target) {
        engine.spawnEmpArcPierceShields(
                source,
                from,
                anchor,
                target,
                DamageType.ENERGY,
                ARC_DAMAGE,
                ARC_EMP,
                10000f,
                ARC_SOUND,
                ARC_THICKNESS,
                ARC_FRINGE,
                ARC_CORE
        );

        if (MagicRender.screenCheck(0.5f, target.getLocation())) {
            MagicLensFlare.createSharpFlare(engine, source, target.getLocation(), 5, 250, 0, ARC_CORE, Color.white);
        }
    }
}