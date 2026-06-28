package data.scripts;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class missp_varunastra_onfire implements OnFireEffectPlugin {

    public static final String KEY_START = "varunastra_start";
    public static final String KEY_HALF  = "varunastra_half";
    public static final String KEY_SWAP  = "varunastra_swap";

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        if (proj == null) return;

        // Cancel vanilla damage ONLY
        proj.setDamageAmount(0f);

        proj.setCustomData(KEY_START, new Vector2f(proj.getLocation()));
        proj.setCustomData(KEY_HALF, weapon.getRange() * 0.5f);
        proj.setCustomData(KEY_SWAP, false);
    }
}
