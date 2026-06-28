package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import static data.scripts.utils.bultach_utils.lerp;

public class bt_patherbeam_effect implements BeamEffectPlugin {

    private final IntervalUtil interval = new IntervalUtil(0.25f, 0.25f);
    private static final float ARC_DAMAGE_AMOUNT = 100f;
    private static final float ARC_DISTANCE = 0.75f;
    private float baseBeamDamage = 100f;

    private static final List<DamageType> DAMAGE_TYPES = Arrays.asList(
            DamageType.KINETIC,
            DamageType.FRAGMENTATION,
            DamageType.HIGH_EXPLOSIVE,
            DamageType.ENERGY
    );

    private static final List<Color> ARC_CORE_COLORS = Arrays.asList(
            new Color(255, 233, 195, 200),
            new Color(218, 255, 246, 200),
            new Color(255, 150, 150, 200),
            new Color(209, 171, 255, 200)
    );

    private static final List<Color> ARC_FRINGE_COLORS = Arrays.asList(
            new Color(255, 233, 195, 120),
            new Color(218, 255, 246, 120),
            new Color(255, 150, 150, 120),
            new Color(209, 171, 255, 120)
    );

    private static final List<Float> SOUND_PITCHES = Arrays.asList(
            1.4f,
            1.0f,
            0.8f,
            1.2f
    );

    private static final Color PIERCE_ARC_COLOR = new Color(255, 61, 84, 225);

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;

        int randomIndex = Misc.random.nextInt(DAMAGE_TYPES.size());
        DamageType selectedDamageType = DAMAGE_TYPES.get(randomIndex);
        Color selectedCoreColor = ARC_CORE_COLORS.get(randomIndex);
        Color selectedFringeColor = ARC_FRINGE_COLORS.get(randomIndex);
        float selectedPitch = SOUND_PITCHES.get(randomIndex);

        Vector2f beamStart = beam.getFrom();
        Vector2f beamEnd = beam.getTo();
        float currentHitRange = beam.getWeapon().getRange();

        if (beam.didDamageThisFrame() && beam.getDamageTarget() != null && beam.getDamageTarget().getCollisionClass() != CollisionClass.NONE) {
            CombatEntityAPI directTarget = beam.getDamageTarget();
            currentHitRange = Math.min(currentHitRange, MathUtils.getDistance(beamStart, directTarget.getLocation()) - directTarget.getCollisionRadius() * 0.5f);
            currentHitRange = Math.max(0f, currentHitRange);
            beamEnd = MathUtils.getPointOnCircumference(beamStart, currentHitRange, beam.getWeapon().getCurrAngle());
        }

        Vector2f arcVisualStart = lerp(beamStart, beamEnd, Misc.random.nextFloat() * ARC_DISTANCE + (1f - ARC_DISTANCE));

        engine.spawnEmpArcVisual(
                arcVisualStart,
                null,
                beamEnd,
                beam.getSource(),
                20f,
                selectedFringeColor,
                selectedCoreColor
        );

        if (beam.didDamageThisFrame() && beam.getDamageTarget() != null) {
            CombatEntityAPI arcActualTarget = beam.getDamageTarget();
            engine.applyDamage(
                    arcActualTarget,
                    beamEnd,
                    ARC_DAMAGE_AMOUNT,
                    selectedDamageType,
                    0f,
                    false,
                    true,
                    beam.getSource()
            );
        }

        engine.spawnEmpArcPierceShields(
                beam.getSource(),
                beamStart,
                beam.getSource(),
                beam.getSource(),
                DamageType.ENERGY,
                baseBeamDamage * 0.0f,
                baseBeamDamage,
                100000f,
                null,
                10f,
                PIERCE_ARC_COLOR,
                PIERCE_ARC_COLOR
        );

        Global.getSoundPlayer().playSound("tachyon_lance_emp_impact", selectedPitch, 0.4f, lerp(arcVisualStart, beamEnd, 0.5f), Misc.ZERO);

        MagicLensFlare.createSharpFlare(
                engine,
                beam.getSource(),
                beamEnd,
                7f,
                300f,
                0f,
                selectedCoreColor,
                selectedFringeColor
        );
    }
}