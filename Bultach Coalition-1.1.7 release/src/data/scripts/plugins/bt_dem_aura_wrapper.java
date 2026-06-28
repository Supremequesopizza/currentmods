package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;
import data.scripts.weapons.bt_holydem_aura;

import java.util.List;

public class bt_dem_aura_wrapper extends BaseEveryFrameCombatPlugin {
    private MissileAPI missile;
    private bt_holydem_aura auraPlugin;
    private boolean auraPluginAdded = false;
    private boolean demActive = false;
    private CombatEntityAPI demDrone = null;

    public bt_dem_aura_wrapper(MissileAPI missile) {
        this.missile = missile;
        this.auraPlugin = new bt_holydem_aura(missile);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;

        if (missile == null || !engine.isEntityInPlay(missile)) {
            cleanUp();
            return;
        }

        boolean isNowDemActive = isDEMScriptActive();

        if (isNowDemActive && !demActive) {
            demActive = true;
            demDrone = findDEMDrone(engine);
        }

        if (demActive && demDrone != null && !engine.isEntityInPlay(demDrone)) {
            demDrone = findDEMDrone(engine);
        }

        if (!auraPluginAdded) {
            engine.addPlugin(auraPlugin);
            auraPluginAdded = true;
        }

        updateAuraPluginTarget();

        if (shouldCleanUp()) {
            cleanUp();
        }
    }

    private boolean isDEMScriptActive() {
        if (missile.getMissileAI() != null &&
                missile.getMissileAI().getClass().getSimpleName().equals("DEMScript")) {
            return true;
        }

        if (missile.getMaxFlightTime() > 1000f && missile.getMaxFlightTime() < 20000f) {
            return true;
        }

        return findDEMDrone(Global.getCombatEngine()) != null;
    }

    private CombatEntityAPI findDEMDrone(CombatEngineAPI engine) {
        for (ShipAPI ship : engine.getShips()) {
            if (ship.isDrone() &&
                    ship.getHullSpec() != null &&
                    ship.getHullSpec().getHullId().equals("dem_drone") &&
                    Vector2f.sub(ship.getLocation(), missile.getLocation(), new Vector2f()).length() < 100f) {
                return ship;
            }
        }
        return null;
    }

    private void updateAuraPluginTarget() {
        if (demActive && demDrone != null) {
            if (auraPluginAdded) {
                Global.getCombatEngine().removePlugin(auraPlugin);
                auraPluginAdded = false;
            }

            if (!auraPluginAdded) {
                auraPlugin = new bt_holydem_aura_compatible(demDrone);
                Global.getCombatEngine().addPlugin(auraPlugin);
                auraPluginAdded = true;
            }
        }
    }

    private boolean shouldCleanUp() {
        if (missile == null) return true;

        CombatEngineAPI engine = Global.getCombatEngine();
        if (!engine.isEntityInPlay(missile)) return true;

        if (demActive && (demDrone == null || !engine.isEntityInPlay(demDrone))) {
            return true;
        }

        return false;
    }

    private void cleanUp() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (auraPluginAdded && auraPlugin != null) {
            engine.removePlugin(auraPlugin);
            auraPluginAdded = false;
        }
        if (engine != null) {
            engine.removePlugin(this);
        }
    }
}

class bt_holydem_aura_compatible extends bt_holydem_aura {
    private CombatEntityAPI entity;

    public bt_holydem_aura_compatible(CombatEntityAPI entity) {
        super(null);
        this.entity = entity;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (entity == null || !Global.getCombatEngine().isEntityInPlay(entity)) {
            Global.getCombatEngine().removePlugin(this);
            return;
        }

        super.advance(amount, events);
    }
}