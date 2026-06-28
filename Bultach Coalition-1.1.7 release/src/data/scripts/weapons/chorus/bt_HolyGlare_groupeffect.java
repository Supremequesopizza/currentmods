package data.scripts.weapons.chorus;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

/**
 * A "grouping" script allowing the Gigaton Lance to have several weapon effect scripts applied to it at once, without a
 * massive file to configure
 * @author Nicke535
 * Borrowed and fucked up by Pogre/Noof for a different beam
 */
public class bt_HolyGlare_groupeffect implements EveryFrameWeaponEffectPlugin {

    //Keeps an instance of our beam behaviour script so that we can call it
    private bt_HolyGlareEffect beamBehaviourScript = null;

    //Keeps an instance of our muzzle flash script so that we can call it
    private bt_HolyGlare_muzzleflash muzzleflashScript = null;

    //Keeps an instance of the onhit flare to be called
    private bt_HolyGlare_onhitflare hitflashScript = null;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run if we are paused, or our if weapon is null
        if (engine.isPaused() || weapon == null) {
            return;
        }

        //Run our beam behaviour script!
        if (beamBehaviourScript == null) {
            beamBehaviourScript = new bt_HolyGlareEffect();
        }
        beamBehaviourScript.advance(amount, engine, weapon);

        //And finally, our muzzle flash!
        if (muzzleflashScript == null) {
            muzzleflashScript = new bt_HolyGlare_muzzleflash();
        }
        muzzleflashScript.advance(amount, engine, weapon);
    }
}
