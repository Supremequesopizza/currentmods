package data.scripts.world.systems.haven;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin.AsteroidFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class Osiris {

    public void generate(SectorAPI sector) {

        StarSystemAPI system = sector.createStarSystem("Osiris");

        system.setBackgroundTextureFilename("graphics/backgrounds/background6.jpg");

        PlanetAPI osiris_star = system.initStar("osiris", // unique id for this star
                "star_white", // Star type. Vanilla star types can be found in starsector-core/data/campaign/procgen/star_gen_data.csv and starsector-core/data/config/planets.json
                700f, 		  //gfbdfgb radius (in pixels at default zoom)
                350); // corona radius, from star edge

        system.setLightColor(new Color(245, 187, 186)); // light color in entire system, affects all entities


        PlanetAPI anvil_kyrika = system.addPlanet("anvil_kyrika", osiris_star, "Kyrika", "arid", 270, 200, 3000, 60);
        anvil_kyrika.setCustomDescriptionId("anvil_kyrika");

        // Kumari Aru trojans - L4 leads, L5 follows
        SectorEntityToken osiris_asteroids = system.addTerrain(Terrain.ASTEROID_FIELD,
                new AsteroidFieldParams(
                        300f, // min radius
                        500f, // max radius
                        16, // min asteroid count
                        24, // max asteroid count
                        4f, // min asteroid radius
                        16f, // max asteroid radius
                        "Osiris Asteroids")); // null for default name

        SectorEntityToken osiris_asteroids2 = system.addTerrain(Terrain.ASTEROID_FIELD,
                new AsteroidFieldParams(
                        300f, // min radius
                        500f, // max radius
                        16, // min asteroid count
                        24, // max asteroid count
                        4f, // min asteroid radius
                        16f, // max asteroid radius
                        "Osiris Asteroid field")); // null for default name

        osiris_asteroids.setCircularOrbit(osiris_star, 270 +60, 2800, 80);
        osiris_asteroids2.setCircularOrbit(osiris_star, 270 -60, 2800, 80);


        PlanetAPI chalcedon = system.addPlanet("anvil_catacomb", osiris_star, "Catacomb", "cryovolcanic", 220, 160, 8300, 180);
        chalcedon.getSpec().setGlowTexture(Global.getSettings().getSpriteName("hab_glows", "volturn"));
        chalcedon.getSpec().setGlowColor(new Color(170,255,240,255));
        chalcedon.getSpec().setUseReverseLightForGlow(true);
        chalcedon.applySpecChanges();
        chalcedon.setCustomDescriptionId("anvil_catacomb");

        // counter-orbit sensor array
        SectorEntityToken osiris_loc1 = system.addCustomEntity(null, null, "sensor_array_makeshift", "exalted");
        osiris_loc1.setCircularOrbitPointingDown(osiris_star, 180-120, 6600, 400);

        SectorEntityToken osiris_loc2 = system.addCustomEntity(null, null, "comm_relay_makeshift", "exalted");
        osiris_loc2.setCircularOrbitPointingDown(osiris_star, 180+120, 7300, 400);

        // fun debris
        DebrisFieldParams params = new DebrisFieldParams(
                200f, // field radius - should not go above 1000 for performance reasons
                1f, // density, visual - affects number of debris pieces
                10000000f, // duration in days
                0f); // days the field will keep generating glowing pieces

        params.source = DebrisFieldSource.SALVAGE;
        params.baseSalvageXP = 250; // base XP for scavenging in field
        SectorEntityToken debris = Misc.addDebrisField(system, params, StarSystemGenerator.random);
        SalvageSpecialAssigner.assignSpecialForDebrisField(debris);

        // makes the debris field always visible on map/sensors and not give any xp or notification on being discovered
        debris.setSensorProfile(null);
        debris.setDiscoverable(null);
        debris.setCircularOrbit(osiris_star, 220 + 16, 4300, 180);

        // a jump in the other one: Rama's Bridge :  Jump-point
        JumpPointAPI jumpPoint2 = Global.getFactory().createJumpPoint("osiris_jump", "Osiris Jump Point");
        jumpPoint2.setCircularOrbit( system.getEntityById("Osiris"), 220 + 60, 4300, 180);
        jumpPoint2.setRelatedPlanet(chalcedon);
        system.addEntity(jumpPoint2);


        system.addRingBand(osiris_star, "misc", "rings_dust0", 256f, 0, Color.white, 256f, 5900, 220f, null, null);
        system.addAsteroidBelt(osiris_star, 150, 5900, 128, 200, 240, Terrain.ASTEROID_BELT, "The Hate");



        float radiusAfter = StarSystemGenerator.addOrbitingEntities(system, osiris_star, StarAge.AVERAGE,
                1, 4, // min/max entities to add
                9900, // radius to start adding at
                6, // name offset - next planet will be <system name> <roman numeral of this parameter + 1>
                false); // whether to use custom or system-name based names

        system.autogenerateHyperspaceJumpPoints(true, true);

        //Getting rid of some hyperspace nebula, just in case
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);

        //Misc.setFullySurveyed(kumarikandam_b.getMarket(), null, false);
    }
}