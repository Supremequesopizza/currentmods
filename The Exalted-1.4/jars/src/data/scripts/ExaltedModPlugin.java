//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import data.scripts.world.systems.exaltedgen;
import exerelin.campaign.SectorManager;

public class ExaltedModPlugin extends BaseModPlugin {
    public static boolean isExerelin = false;

    public ExaltedModPlugin() {
    }

    @Override
    public void onNewGame() {
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (!haveNexerelin || SectorManager.getCorvusMode()){
            new exaltedgen().generate(Global.getSector());
        }
    }

}
