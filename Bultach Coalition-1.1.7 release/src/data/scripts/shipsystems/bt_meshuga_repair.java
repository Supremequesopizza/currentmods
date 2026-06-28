package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import java.awt.Color;

public class bt_meshuga_repair extends BaseShipSystemScript {

    private boolean repaired = false;
    private float timer = 0f;

    public static final float ARMOR_HULL_REPAIR_PERCENT = 0.10f;
    public static final float REPAIR_DURATION = 12f;
    public static final float EMP_RESIST_INITIAL = 0.85f;
    public static final float EMP_DURATION = 8f;

    public static final Color JITTER_COLOR = new Color(255,165,90,155);
    public static final float JITTER_INTENSITY = 0.5f;
    public static final int JITTER_COPIES = 5;
    public static final float JITTER_RANGE = 9.0f;


    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        timer += amount;

        if (!repaired && effectLevel > 0) {
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.isDisabled()) {
                    w.repair();
                }
            }
            for (ShipEngineAPI e : ship.getEngineController().getShipEngines()) {
                if (e.isDisabled()) {
                    e.setHitpoints(e.getMaxHitpoints());
                }
            }

            ship.setJitterUnder(this, JITTER_COLOR, JITTER_INTENSITY, JITTER_COPIES, 0f, JITTER_RANGE);

            repaired = true;
        }

        if (timer < REPAIR_DURATION) {
            float maxHull = ship.getMaxHitpoints();
            float hullToRepairThisFrame = (maxHull * ARMOR_HULL_REPAIR_PERCENT / REPAIR_DURATION) * amount * effectLevel;
            ship.setHitpoints(Math.min(maxHull, ship.getHitpoints() + hullToRepairThisFrame));

            float maxTotalArmor = ship.getHullSpec().getArmorRating() * 10f;
            float armorToRepairThisFrame = (maxTotalArmor * ARMOR_HULL_REPAIR_PERCENT / REPAIR_DURATION) * amount * effectLevel;
            distributeArmorRepair(ship, armorToRepairThisFrame);
        }

        if (timer < EMP_DURATION) {
            float currentResistance = EMP_RESIST_INITIAL * (1f - (timer / EMP_DURATION));
            stats.getEmpDamageTakenMult().modifyMult(id, 1f - currentResistance);
        } else {
            stats.getEmpDamageTakenMult().unmodify(id);
        }
    }

    private void distributeArmorRepair(ShipAPI ship, float totalAmount) {
        ArmorGridAPI grid = ship.getArmorGrid();
        int cellsX = grid.getGrid().length;
        int cellsY = grid.getGrid()[0].length;
        if (cellsX * cellsY <= 0) return;

        float totalMaxArmor = ship.getHullSpec().getArmorRating() * 10f;
        float averageMaxArmorPerCell = totalMaxArmor / (cellsX * cellsY);
        float amountPerCell = totalAmount / (cellsX * cellsY);

        for (int i = 0; i < cellsX; i++) {
            for (int j = 0; j < cellsY; j++) {
                float currentArmor = grid.getArmorValue(i, j);
                if (currentArmor < averageMaxArmorPerCell) {
                    grid.setArmorValue(i, j, Math.min(averageMaxArmorPerCell, currentArmor + amountPerCell));
                }
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getEmpDamageTakenMult().unmodify(id);
        repaired = false;
        timer = 0f;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (state == State.IN || state == State.ACTIVE) {
            if (index == 0 && timer < REPAIR_DURATION) {
                return new StatusData("Repairing systems...", false);
            }
            if (index == 1 && timer < EMP_DURATION) {
                int percent = (int) ((EMP_RESIST_INITIAL * (1f - (timer / EMP_DURATION))) * 100);
                if (percent > 0) {
                    return new StatusData("EMP resistance: " + percent + "%", false);
                }
            }
        }
        return null;
    }
}