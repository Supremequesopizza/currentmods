package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.scripts.world.systems.Gestalt_Hubris;
import data.scripts.world.systems.ork_Dakka_Den;

@SuppressWarnings("unchecked")
public class ork_Gen implements SectorGeneratorPlugin {

    @Override
    public void generate(SectorAPI sector) {

        new ork_Dakka_Den().generate(sector);
        new Gestalt_Hubris().generate(sector);
        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("orks");
    }
}