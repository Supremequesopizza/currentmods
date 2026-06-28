package data.scripts.world.systems;

import java.awt.Color;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import static com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.addOrbitingEntities;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class ork_Dakka_Den {
    public void generate(SectorAPI sector) {
        StarSystemAPI system = sector.createStarSystem("Rionnagmor");

        system.setBackgroundTextureFilename("graphics/backgrounds/background_rionnagmor.jpg");
        generateNebula(system);
        system.getLocation().set(-5500, -19000); // Roughly centered and below the core worlds

        PlanetAPI dakka_star = system.initStar("Dakka Den",
                "star_red_giant",
                1000f,
                1500,
                5f,
                0.5f,
                2f);

        system.setLightColor(new Color(255, 171, 171)); // system light color

        PlanetAPI dakkaDen1 = system.addPlanet("dregruk", dakka_star, "Mèinnearach", "barren", 140, 110, 3400, 180);
        dakkaDen1.setCustomDescriptionId("bt_meinnearach");

        JumpPointAPI jumpPoint1 = Global.getFactory().createJumpPoint("dakka_den_jump_1", "Rionnagmor Inner Jump-point");
        jumpPoint1.setCircularOrbit(dakka_star, 200, 3300, 180); // Angle at 200, L5 of Dreguk
        jumpPoint1.setRelatedPlanet(dakkaDen1);
        jumpPoint1.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint1);

        system.addAsteroidBelt(dakka_star, 100, 3800, 300, 160, 220); // Ring system located between inner and outer planets
        system.addRingBand(dakka_star, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 3760, 200f, null, null);
        system.addRingBand(dakka_star, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 3840, 200f, null, null);

        system.addAsteroidBelt(dakka_star, 200, 4500, 500, 160, 220); // Ring system located between inner and outer planets
        system.addRingBand(dakka_star, "misc", "rings_asteroids0", 256f, 1, Color.white, 256f, 4440, 200f, null, null);
        system.addRingBand(dakka_star, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 4560, 200f, null, null);

        PlanetAPI dakkaDen2 = system.addPlanet("orguk", dakka_star, "Dhachaigh", "bt_ruined", 250, 180, 6000, 280);
        dakkaDen2.setCustomDescriptionId("bt_dhachnaigh");
        system.addRingBand(dakkaDen2, "misc", "rings_dust0", 256f, 2, Color.yellow, 256f, 280, 125f);
        Misc.initConditionMarket(dakkaDen2);
        dakkaDen2.getMarket().addCondition(Conditions.ORGANICS_TRACE);
        dakkaDen2.getMarket().addCondition(Conditions.RARE_ORE_ABUNDANT);
        dakkaDen2.getMarket().addCondition(Conditions.IRRADIATED);
        dakkaDen2.getMarket().addCondition(Conditions.TOXIC_ATMOSPHERE);
        dakkaDen2.getMarket().addCondition(Conditions.RUINS_VAST);
        dakkaDen2.getMarket().addCondition(Conditions.VERY_HOT);
        dakkaDen2.getMarket().addCondition(Conditions.TECTONIC_ACTIVITY);
        dakkaDen2.getMarket().addCondition(Conditions.EXTREME_WEATHER);
        for (MarketConditionAPI condition : dakkaDen2.getMarket().getConditions()) {
            condition.setSurveyed(true);
        }

        PlanetAPI dakkaDen2a = system.addPlanet("gathrog", dakkaDen2, "Miotail", "barren2", 40, 105, 700, 29);
        dakkaDen2a.setCustomDescriptionId("bt_miotail");
        dakkaDen2a.getSpec().setGlowTexture(Global.getSettings().getSpriteName("hab_glows", "bt_miotail_glow"));
        dakkaDen2a.getSpec().setGlowColor(new Color(255, 255, 255, 255));
        dakkaDen2a.getSpec().setUseReverseLightForGlow(true);
        dakkaDen2a.applySpecChanges();

        PlanetAPI dakkaDen3 = system.addPlanet("nastruime", dakka_star, "Nastruime", "gas_giant", 250, 350, 8500, 300);

            SectorEntityToken dakkaDen3a = system.addCustomEntity("bt_siphon_station", "Bultach Siphon Station", "bt_siphon_station", "orks");
            dakkaDen3a.setCircularOrbitPointingDown(dakkaDen3, 30, 840,60);
            dakkaDen3a.setCustomDescriptionId("bt_siphon");
            dakkaDen3a.setInteractionImage("illustrations", "orbital");

        //beam from the station
        SectorEntityToken MiningBeam = system.addCustomEntity("bt_siphon_beam", null, "bt_siphon_beam", null); //add the thing orbiting the market
        MiningBeam.setCircularOrbitPointingDown(dakkaDen3a, 210f, 280, 60f); //set as circular orbit

        //gas giant being FUCKED up
        SectorEntityToken PlanetaryEffect = system.addCustomEntity("bt_siphon_planetary_effect", null, "bt_siphon_planetary_effect", null); //add the thing orbiting the market
        PlanetaryEffect.setCircularOrbitPointingDown(dakkaDen3a, 210f, 625f, 60f); //set as circular orbit


        SectorEntityToken orkRelay = system.addCustomEntity(null, "Bultach Comm Relay", "comm_relay_makeshift", "orks"); // Makeshift comm relay at L4 of Orguk
        orkRelay.setCircularOrbit(dakka_star, 190, 6000, 280);

        SectorEntityToken orkBuoy = system.addCustomEntity(null, "Bultach Nav Buoy", "nav_buoy_makeshift", "orks"); // Makeshift nav buoy at L5 of Orguk
        orkBuoy.setCircularOrbit(dakka_star, 310, 6000, 280);

        system.addAsteroidBelt(dakka_star, 200, 10000, 300, 160, 220); // Ring system encircling outer planets
        system.addRingBand(dakka_star, "misc", "rings_asteroids0", 256f, 3, Color.yellow, 256f, 10000, 200f, null, null);
        system.addRingBand(dakka_star, "misc", "rings_dust0", 256f, 1, Color.yellow, 256f, 10000, 200f, null, null);

        system.addAsteroidBelt(dakka_star, 200, 10300, 300, 160, 220); // Ring system encircling outer planets
        system.addRingBand(dakka_star, "misc", "rings_asteroids0", 256f, 3, Color.yellow, 256f, 10300, 200f, null, null);
        system.addRingBand(dakka_star, "misc", "rings_dust0", 256f, 1, Color.yellow, 256f, 10300, 200f, null, null);

        system.autogenerateHyperspaceJumpPoints(true, true);

        addDerelict(system, dakka_star, "ork_battlecruiser_aggressive", ShipRecoverySpecial.ShipCondition.WRECKED, 2200 + ((float) Math.random() * 200f), 0, false);
        addDerelict(system, dakka_star, "ork_vencha_support", ShipRecoverySpecial.ShipCondition.WRECKED, 2200 + ((float) Math.random() * 200f), 120, (Math.random() < 0.2));
        addDerelict(system, dakka_star, "paragon_Elite", ShipRecoverySpecial.ShipCondition.WRECKED, 2200 + ((float) Math.random() * 200f), 240, false);
        addDerelict(system, dakka_star, "ork_crusher_standard", ShipRecoverySpecial.ShipCondition.WRECKED, 2200 + ((float) Math.random() * 200f), 120, (Math.random() < 0.1));

        addDerelict(system, dakka_star, "wolf_Assault", ShipRecoverySpecial.ShipCondition.WRECKED, 2500 + ((float) Math.random() * 200f), 0, (Math.random() < 0.2));
        addDerelict(system, dakka_star, "ork_crusher_standard", ShipRecoverySpecial.ShipCondition.WRECKED, 2500 + ((float) Math.random() * 200f), 120, false);
        addDerelict(system, dakka_star, "scarab_Starting", ShipRecoverySpecial.ShipCondition.WRECKED, 2500 + ((float) Math.random() * 200f), 240, (Math.random() < 0.1));

        addDerelict(system, dakka_star, "ork_faster_standard", ShipRecoverySpecial.ShipCondition.WRECKED, 2750 + ((float) Math.random() * 200f), 0, false);
        addDerelict(system, dakka_star, "aurora_Assault", ShipRecoverySpecial.ShipCondition.WRECKED, 2750 + ((float) Math.random() * 200f), 120, false);
        addDerelict(system, dakka_star, "ork_speeder_standard", ShipRecoverySpecial.ShipCondition.WRECKED, 2750 + ((float) Math.random() * 200f), 240, (Math.random() < 0.1));

        cleanup(system);
    }

    protected void addDerelict(StarSystemAPI system,
                               SectorEntityToken focus,
                               String variantId,
                               ShipRecoverySpecial.ShipCondition condition,
                               float orbitRadius,
                               float angle,
                               boolean recoverable) {
        DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(new ShipRecoverySpecial.PerShipData(variantId, condition), true);
        SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(system, Entities.WRECK, Factions.NEUTRAL, params);
        ship.setDiscoverable(true);

        float orbitDays = orbitRadius * MathUtils.getRandomNumberInRange(0.7f, 1.3f) / 50;
        ship.setCircularOrbit(focus, (float) MathUtils.getRandomNumberInRange(-10, 10) + angle, orbitRadius, orbitDays);

        WeightedRandomPicker<String> factions = new WeightedRandomPicker<>();
        factions.add("ork");
        if (recoverable) {
            SalvageSpecialAssigner.ShipRecoverySpecialCreator creator = new SalvageSpecialAssigner.ShipRecoverySpecialCreator(null, 0, 0, false, null, factions);
            Misc.setSalvageSpecial(ship, creator.createSpecial(ship, null));


        }
    }

    void cleanup(StarSystemAPI system) {
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
    }


    protected void generateNebula(StarSystemAPI system) {


        SectorEntityToken nebulaTiles = Misc.addNebulaFromPNG("data/campaign/terrain/nebula_rionnagmor.png",
                0, 0, // Center of nebula
                system, // Location to add to
                "terrain", "nebula_bt_home", // Texture to use, uses xxx_map for map
                4, 4, Terrain.NEBULA, StarAge.OLD);

        nebulaTiles.getLocation().set(0, 0);

        BaseTiledTerrain nebula = getNebula(system);
        nebula.setTerrainName("Rionnagmor Nebula");

    }

    BaseTiledTerrain getNebula(StarSystemAPI system) {
        for (CampaignTerrainAPI curr : system.getTerrainCopy()) {
            if (curr.getPlugin().getTerrainId().equals(Terrain.NEBULA)) {
                return (BaseTiledTerrain) (curr.getPlugin());
            }
        }
        return null;
    }

}
