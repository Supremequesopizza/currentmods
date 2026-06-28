package data.scripts.world.systems;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.scripts.world.systems.haven.Mausoleum;
import data.scripts.world.systems.haven.Osiris;

public class exaltedgen implements SectorGeneratorPlugin {
    public exaltedgen() {
    }

    public static void initFactionRelationships(SectorAPI sector) {
        FactionAPI hegemony = sector.getFaction("hegemony");
        FactionAPI tritachyon = sector.getFaction("tritachyon");
        FactionAPI pirates = sector.getFaction("pirates");
        FactionAPI church = sector.getFaction("luddic_church");
        FactionAPI path = sector.getFaction("luddic_path");
        FactionAPI indep = sector.getFaction("independent");
        FactionAPI diktat = sector.getFaction("sindrian_diktat");
        FactionAPI persean = sector.getFaction("persean");
        FactionAPI exalted = sector.getFaction("exalted");
        FactionAPI remnant = sector.getFaction("remnant");
        
        exalted.setRelationship(path.getId(), RepLevel.VENGEFUL);
        exalted.setRelationship(hegemony.getId(), RepLevel.VENGEFUL);
        exalted.setRelationship(church.getId(), RepLevel.VENGEFUL);
        exalted.setRelationship(pirates.getId(), RepLevel.VENGEFUL);
        exalted.setRelationship(tritachyon.getId(), RepLevel.VENGEFUL);
        exalted.setRelationship(indep.getId(), RepLevel.FAVORABLE);
        exalted.setRelationship(persean.getId(), RepLevel.WELCOMING);
        exalted.setRelationship(diktat.getId(), RepLevel.SUSPICIOUS);
        exalted.setRelationship(remnant.getId(), RepLevel.COOPERATIVE);


        exalted.setRelationship("keruvim", RepLevel.WELCOMING);
        exalted.setRelationship("blackjack", RepLevel.SUSPICIOUS);
        exalted.setRelationship("mayasura", RepLevel.FRIENDLY);
        exalted.setRelationship("draco", RepLevel.HOSTILE);
        exalted.setRelationship("fang", RepLevel.HOSTILE);
        exalted.setRelationship("metelson", RepLevel.FAVORABLE);
        exalted.setRelationship("new_galactic_order", RepLevel.VENGEFUL);
        exalted.setRelationship("junk_pirates", RepLevel.HOSTILE);
        exalted.setRelationship("junk_pirates_hounds", RepLevel.HOSTILE);
        exalted.setRelationship("junk_pirates_junkboys", RepLevel.HOSTILE);
        exalted.setRelationship("junk_pirates_technicians", RepLevel.HOSTILE);
        exalted.setRelationship("blade_breakers", RepLevel.VENGEFUL);
        exalted.setRelationship("cabal", RepLevel.HOSTILE);
        exalted.setRelationship("mess", RepLevel.VENGEFUL);
    }

    public void generate(SectorAPI sector) {
        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("exalted");
        initFactionRelationships(sector);
        (new Mausoleum()).generate(sector);
        (new Osiris()).generate(sector);
    }



}
