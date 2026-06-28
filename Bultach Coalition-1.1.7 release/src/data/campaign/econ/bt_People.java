package data.campaign.econ;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;

public class bt_People {

    public static void create() {
        createBTPeople();
    }

    public static PersonAPI createBTPeople() {
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();

        MarketAPI market1 = Global.getSector().getEconomy().getMarket("dregruk");
        if (market1 != null) {
            PersonAPI admin1 = Global.getFactory().createPerson();
            admin1.setId("bt_uisdean");
            admin1.setFaction("orks");
            admin1.setGender(FullName.Gender.MALE);
            admin1.setPostId(Ranks.POST_FACTION_LEADER);
            admin1.setRankId(Ranks.SPACE_ADMIRAL);
            admin1.setImportance(PersonImportance.VERY_HIGH);
            admin1.getName().setFirst("Uisdean");
            admin1.getName().setLast("Knox");
            admin1.setPortraitSprite(Global.getSettings().getSpriteName("characters", "BTuisdean"));
            admin1.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            ip.addPerson(admin1);
            market1.setAdmin(admin1);
            market1.getCommDirectory().addPerson(admin1, 0);
            market1.addPerson(admin1);

            PersonAPI admiral1 = Global.getSector().getFaction("orks").createRandomPerson(FullName.Gender.ANY);
            admiral1.setId("bt_admiral1");
            admiral1.setPostId(Ranks.POST_OFFICER);
            admiral1.setRankId(Ranks.SPACE_ADMIRAL);
            admiral1.setPersonality(Personalities.AGGRESSIVE);
            admiral1.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
            admiral1.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
            admiral1.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
            admiral1.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
            admiral1.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
            admiral1.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
            admiral1.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
            admiral1.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
            admiral1.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);
            admiral1.addTag("coff_nocapture");
            admiral1.getStats().setLevel(9);
            ip.addPerson(admiral1);

        }


        MarketAPI market2 = Global.getSector().getEconomy().getMarket("orguk");
        if (market2 != null) {
            PersonAPI admin2 = Global.getFactory().createPerson();
            admin2.setId("bt_liam");
            admin2.setFaction("orks");
            admin2.setGender(FullName.Gender.MALE);
            admin2.setPostId(Ranks.POST_FACTION_LEADER);
            admin2.setRankId(Ranks.FACTION_LEADER);
            admin2.setImportance(PersonImportance.VERY_HIGH);
            admin2.getName().setFirst("Liam");
            admin2.getName().setLast("Niall");
            admin2.setPortraitSprite(Global.getSettings().getSpriteName("characters", "BTliam"));
            admin2.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            ip.addPerson(admin2);
            market2.setAdmin(admin2);
            market2.getCommDirectory().addPerson(admin2, 0);
            market2.addPerson(admin2);
        }

        MarketAPI market3 = Global.getSector().getEconomy().getMarket("gathrog");
        if (market3 != null) {
            PersonAPI admin3 = Global.getFactory().createPerson();
            admin3.setId("bt_ailis");
            admin3.setFaction("orks");
            admin3.setGender(FullName.Gender.FEMALE);
            admin3.setPostId(Ranks.POST_FACTION_LEADER);
            admin3.setRankId(Ranks.SPACE_ADMIRAL);
            admin3.setImportance(PersonImportance.VERY_HIGH);
            admin3.getName().setFirst("Ailis");
            admin3.getName().setLast("Mil");
            admin3.setPortraitSprite(Global.getSettings().getSpriteName("characters", "BTailis"));
            admin3.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            ip.addPerson(admin3);
            market3.setAdmin(admin3);
            market3.getCommDirectory().addPerson(admin3, 0);
            market3.addPerson(admin3);
        }

        MarketAPI market4 = Global.getSector().getEconomy().getMarket("bt_siphon_station");
        if (market4 != null) {
            PersonAPI admin4 = Global.getFactory().createPerson();
            admin4.setId("bt_kresknov");
            admin4.setFaction("orks");
            admin4.setGender(FullName.Gender.MALE);
            admin4.setPostId(Ranks.POST_FACTION_LEADER);
            admin4.setRankId(Ranks.SPACE_ADMIRAL);
            admin4.setImportance(PersonImportance.VERY_HIGH);
            admin4.getName().setFirst("Kresknov");
            admin4.getName().setLast("Mor");
            admin4.setPortraitSprite(Global.getSettings().getSpriteName("characters", "BTkresknov"));
            admin4.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            ip.addPerson(admin4);
            market4.setAdmin(admin4);
            market4.getCommDirectory().addPerson(admin4, 0);
            market4.addPerson(admin4);
        }
        return null;
    }
}
