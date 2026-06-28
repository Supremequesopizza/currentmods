package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.dem.DEMScript;
import data.scripts.plugins.bt_dem_aura_wrapper;

public class bt_holydem_onfire implements OnFireEffectPlugin {
    
    private static final String HOLY_TORP_PROJECTILE_ID = "bt_holydem_proj";
    
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (!(projectile instanceof MissileAPI)) return;
        
        MissileAPI missile = (MissileAPI) projectile;
        
        ShipAPI ship = null;
        if (weapon != null) ship = weapon.getShip();
        if (ship == null) return;

        DEMScript demScript = new DEMScript(missile, ship, weapon);
        Global.getCombatEngine().addPlugin(demScript);

        if (projectile.getProjectileSpecId() != null &&
            projectile.getProjectileSpecId().equals(HOLY_TORP_PROJECTILE_ID)) {
            
            if (!projectile.isFading()) {
                bt_dem_aura_wrapper auraWrapper = new bt_dem_aura_wrapper(missile);
                Global.getCombatEngine().addPlugin(auraWrapper);
            }
        }
    }
}