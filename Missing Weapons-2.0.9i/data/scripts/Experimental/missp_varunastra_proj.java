package data.scripts;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

public class missp_varunastra_proj implements EveryFrameProjectilePlugin {

    // === HE visuals ===
    private static final Color HE_COLOR = new Color(255, 100, 60, 200);

    // === KINETIC visuals ===
    private static final Color CORE_BLUE   = new Color(80, 160, 255, 220);
    private static final Color EDGE_YELLOW = new Color(255, 220, 80, 160);

    private static final float SWITCH_DIST = 450f; // half of 900 arc

    @Override
    public void advance(float amount, CombatEngineAPI engine, DamagingProjectileAPI proj) {

        if (engine.isPaused() || proj == null) return;

        // -------------------------------------------------
        // START POSITION (once)
        // -------------------------------------------------
        Vector2f start = (Vector2f) proj.getCustomData().get("varunastra_start");
        if (start == null) {
            proj.setCustomData("varunastra_start", new Vector2f(proj.getLocation()));
            return;
        }

        float dist = Vector2f.sub(proj.getLocation(), start, null).length();

        // -------------------------------------------------
        // BEFORE MIDPOINT → HE VISUALS
        // -------------------------------------------------
        if (dist < SWITCH_DIST) {
            spawnHEGlow(engine, proj);
            return;
        }

        // -------------------------------------------------
        // MIDPOINT → REPLACE PROJECTILE
        // -------------------------------------------------
        if (!Boolean.TRUE.equals(proj.getCustomData().get("varunastra_replaced"))) {

            proj.setCustomData("varunastra_replaced", true);

            // midpoint flash
            engine.addHitParticle(
                    proj.getLocation(),
                    proj.getVelocity(),
                    90f,
                    1.3f,
                    0.12f,
                    Color.WHITE
            );

            // spawn kinetic projectile
            DamagingProjectileAPI kin =
                    (DamagingProjectileAPI) engine.spawnProjectile(
                            proj.getSource(),
                            proj.getWeapon(),
                            "missp_varunastra_kinetic_shot",
                            proj.getLocation(),
                            proj.getFacing(),
                            proj.getVelocity()
                    );

            // IMPORTANT: reset lifetime
            kin.setElapsed(0f);

            engine.removeEntity(proj);
            return; // CRITICAL
        }

        // -------------------------------------------------
        // AFTER MIDPOINT → KINETIC VISUALS (fade)
        // -------------------------------------------------
        spawnKineticVisuals(engine, proj);
    }

    // =================================================
    // HE VISUAL
    // =================================================
    private void spawnHEGlow(CombatEngineAPI engine, DamagingProjectileAPI proj) {
        engine.addSmoothParticle(
                proj.getLocation(),
                proj.getVelocity(),
                20f,
                0.6f,
                0.04f,
                HE_COLOR
        );
    }

    // =================================================
    // KINETIC VISUAL (blue core + yellow edge fade)
    // =================================================
    private void spawnKineticVisuals(CombatEngineAPI engine, DamagingProjectileAPI proj) {

        float lifeFrac = proj.getElapsed() / proj.getMaxLife();
        float fade = 1f - clamp01((lifeFrac - 0.3f) / 0.7f);

        Vector2f loc = proj.getLocation();
        Vector2f vel = proj.getVelocity();

        // Core
        engine.addSmoothParticle(
                loc,
                vel,
                16f,
                1.4f * fade,
                0.05f,
                new Color(
                        CORE_BLUE.getRed(),
                        CORE_BLUE.getGreen(),
                        CORE_BLUE.getBlue(),
                        (int)(CORE_BLUE.getAlpha() * fade)
                )
        );

        // Halo
        engine.addSmoothParticle(
                loc,
                vel,
                28f,
                0.6f * fade,
                0.08f,
                new Color(
                        EDGE_YELLOW.getRed(),
                        EDGE_YELLOW.getGreen(),
                        EDGE_YELLOW.getBlue(),
                        (int)(EDGE_YELLOW.getAlpha() * fade)
                )
        );
    }

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
