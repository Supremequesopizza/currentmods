package Utilities;

import com.fs.starfarer.api.Global;

public class mhmods_eneableSmod {
    static Boolean enable = null;

    public static boolean getEnable(){
        if (enable == null){
            enable = Global.getSettings().getModManager().isModEnabled("better_deserving_smods") || Global.getSettings().getBoolean("mhmods_SmodBonusesWithoutBDSM");
        }
        return enable;
    }
}
