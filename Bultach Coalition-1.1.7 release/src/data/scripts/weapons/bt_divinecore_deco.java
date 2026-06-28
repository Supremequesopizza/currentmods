package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class bt_divinecore_deco implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private final List<DamagingProjectileAPI> projectiles = new ArrayList<>();
    private boolean empSlotsInitialized = false;
    private final List<WeaponAPI> targetSystemWeapons = new ArrayList<>();
    private final Random arcRandom = new Random();

    private static final Set<String> TARGET_SLOT_IDS = new HashSet<>(Arrays.asList(
            "WS0016", "WS0024", "WS0010", "WS0025", "WS0026", "WS0029", "WS0030", "WS0031",
            "WS0032", "WS0033", "WS0034", "WS0035", "WS0036", "WS0037", "WS0038", "WS0039",
            "WS0040", "WS0041", "WS0042", "WS0043"
    ));

    private float empIntervalTimer = 0f;
    private static final float BASE_EMP_AVERAGE_INTERVAL = 2.5f;
    private static final float SYSTEM_ACTIVE_EMP_AVERAGE_INTERVAL = 0.6f;

    private static final float EMP_ARC_THICKNESS = 10f;
    private static final Color EMP_ARC_FRINGE_COLOR = new Color(255, 210, 180, 175);
    private static final Color EMP_ARC_CORE_COLOR = new Color(255, 255, 230, 200);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || weapon == null || engine.isPaused()) return;

        SpriteAPI flare1 = Global.getSettings().getSprite("fx", "bt_holy_explosion");
        SpriteAPI flare2 = Global.getSettings().getSprite("fx", "bt_flare1");
        SpriteAPI flare3 = Global.getSettings().getSprite("fx", "bt_flare1");

        Vector2f point = new Vector2f(0f, 0f);
        VectorUtils.rotate(point, weapon.getShip().getFacing());
        Vector2f.add(point, weapon.getLocation(), point);

        MagicRender.singleframe(
                flare1,
                MathUtils.getRandomPointInCircle(point, MathUtils.getRandomNumberInRange(0f, 3f)),
                new Vector2f(90f, 90f),
                0f,
                new Color(255, 229, 200, 115),
                false
        );

        MagicRender.singleframe(
                flare2,
                MathUtils.getRandomPointInCircle(point, MathUtils.getRandomNumberInRange(0f, 3f)),
                new Vector2f(250f, 30f),
                0f,
                new Color(255, 223, 175, 125),
                true
        );

        MagicRender.singleframe(
                flare3,
                MathUtils.getRandomPointInCircle(point, MathUtils.getRandomNumberInRange(0f, 3f)),
                new Vector2f(250f, 30f),
                90f,
                new Color(255, 223, 175, 125),
                true
        );

        for (int i = 0; i < 3; i++) {
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bt_cleave_aura"),
                    MathUtils.getRandomPointInCircle(weapon.getLocation(), MathUtils.getRandomNumberInRange(0f, 3f)),
                    new Vector2f(),
                    new Vector2f(18, 18),
                    new Vector2f(84 + 136 * i, 84 + 136 * i),
                    MathUtils.getRandomNumberInRange(0, 360),
                    MathUtils.getRandomNumberInRange(-15, 15),
                    new Color(255, 248, 238, 15),
                    true,
                    0.08f * i,
                    0.0f,
                    0.4f - i / 8f
            );
        }

        ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        if (!empSlotsInitialized) {
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (TARGET_SLOT_IDS.contains(w.getSlot().getId())) {
                    if (w != weapon) {
                        targetSystemWeapons.add(w);
                    }
                }
            }
            empSlotsInitialized = true;
        }

        if (targetSystemWeapons.isEmpty()) {
            return;
        }

        empIntervalTimer -= amount;
        if (empIntervalTimer <= 0f) {
            float currentInterval = BASE_EMP_AVERAGE_INTERVAL;
            if (ship.getSystem() != null && ship.getSystem().isActive()) {
                currentInterval = SYSTEM_ACTIVE_EMP_AVERAGE_INTERVAL;
            }
            empIntervalTimer = currentInterval * (0.75f + arcRandom.nextFloat() * 0.5f);

            WeaponAPI targetWeapon = targetSystemWeapons.get(arcRandom.nextInt(targetSystemWeapons.size()));
            engine.spawnEmpArcVisual(
                    weapon.getLocation(),
                    ship,
                    targetWeapon.getLocation(),
                    ship,
                    EMP_ARC_THICKNESS,
                    EMP_ARC_FRINGE_COLOR,
                    EMP_ARC_CORE_COLOR
            );
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (projectile != null) {
            projectiles.add(projectile);
        }
    }
}