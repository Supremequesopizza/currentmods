package com.fs.starfarer.api.impl.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.world.TTBlackSite;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class bt_DerelictSpawner {
    private static Logger log = Global.getLogger(bt_DerelictSpawner.class);

    public bt_DerelictSpawner() {
    }

    public static void spawnDerelicts() {
        List<SectorEntityToken> spawnLocations = findSpawnLocations();
        List<String> variantsToSpawn = getVariantsToSpawn();

        if (spawnLocations.isEmpty()) {
            log.warn("No suitable spawn locations found for derelict spawning.");
            return;
        }

        Collections.shuffle(spawnLocations, MathUtils.getRandom());

        int spawnedCount = 0;
        int maxSpawns = Math.min(variantsToSpawn.size(), spawnLocations.size());

        for (int i = 0; i < maxSpawns; i++) {
            String variantIdToSpawn = variantsToSpawn.get(i);
            SectorEntityToken spawnLocation = spawnLocations.get(i);

            ShipVariantAPI shipVariant = Global.getSettings().getVariant(variantIdToSpawn);
            if (shipVariant == null) {
                log.error("Could not find variant: " + variantIdToSpawn + ". Skipping this derelict.");
                continue;
            }

            String hullIdForTypeField = shipVariant.getHullSpec().getHullId();
            String hullForLogging = shipVariant.getHullVariantId();
            FactionAPI faction = Global.getSector().getFaction("neutral");
            String name = faction.pickRandomShipName();

            if (spawnLocation instanceof PlanetAPI) {
                PlanetAPI planet = (PlanetAPI) spawnLocation;
                float orbitRadius = planet.getRadius() + MathUtils.getRandomNumberInRange(50f, 300f);
                TTBlackSite.addDerelict(
                        planet.getStarSystem(),
                        planet,
                        variantIdToSpawn,
                        name,
                        hullIdForTypeField,
                        ShipRecoverySpecial.ShipCondition.AVERAGE,
                        orbitRadius,
                        true
                );
                log.info("Generated a " + hullForLogging + " (" + variantIdToSpawn + ") in orbit of " + planet.getName() + " in the " + planet.getStarSystem().getName() + " system.");
            } else {

                StarSystemAPI system = getStarSystemForEntity(spawnLocation);
                if (system != null) {
                    float orbitRadius = MathUtils.getRandomNumberInRange(100f, 500f);
                    TTBlackSite.addDerelict(
                            system,
                            spawnLocation,
                            variantIdToSpawn,
                            name,
                            hullIdForTypeField,
                            ShipRecoverySpecial.ShipCondition.AVERAGE,
                            orbitRadius,
                            true
                    );
                    log.info("Generated a " + hullForLogging + " (" + variantIdToSpawn + ") near " + spawnLocation.getName() + " in the " + system.getName() + " system.");
                } else {
                    log.warn("Could not find star system for entity: " + spawnLocation.getName() + ", skipping derelict spawn.");
                }
            }
            spawnedCount++;
        }

        log.info("Successfully spawned " + spawnedCount + " derelicts out of " + variantsToSpawn.size() + " planned.");
    }

    private static StarSystemAPI getStarSystemForEntity(SectorEntityToken entity) {
        LocationAPI location = entity.getContainingLocation();
        if (location instanceof StarSystemAPI) {
            return (StarSystemAPI) location;
        }
        return null;
    }

    private static List<SectorEntityToken> findSpawnLocations() {
        List<SectorEntityToken> locations = new ArrayList<SectorEntityToken>();

        // Primary
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (!system.isProcgen()) continue;

            for (PlanetAPI planet : system.getPlanets()) {
                Float prob = MathUtils.getRandomNumberInRange(0f, 100f);
                if (prob > 90 && hasRuinsCondition(planet)) {
                    locations.add(planet);
                }
            }
        }

        // Secondary fallback (it's over)
        if (locations.size() < 5) {
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                if (!system.isProcgen()) continue;


                if (system.hasTag(Tags.THEME_REMNANT) ||
                        system.hasTag(Tags.THEME_REMNANT_SECONDARY)) {

                    for (SectorEntityToken entity : system.getEntitiesWithTag(Tags.JUMP_POINT)) {
                        locations.add(entity);
                    }
                }
            }
        }

        // Tertiary fallback (it's so over)
        if (locations.size() < 3) {
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                if (!system.isProcgen()) continue;

                for (SectorEntityToken entity : system.getEntitiesWithTag(Tags.JUMP_POINT)) {
                    if (locations.size() >= 10) break;
                    locations.add(entity);
                }
                if (locations.size() >= 10) break;
            }
        }

        // Final fallback (new levels of over previously thought impossible)
        if (locations.isEmpty()) {
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                if (!system.isProcgen()) continue;

                for (PlanetAPI planet : system.getPlanets()) {
                    if (locations.size() >= 5) break;
                    locations.add(planet);
                }
                if (locations.size() >= 5) break;
            }
        }

        log.info("Found " + locations.size() + " potential derelict spawn locations");
        return locations;
    }

    private static boolean hasRuinsCondition(PlanetAPI planet) {
        return planet.hasCondition(Conditions.RUINS_VAST) ||
                planet.hasCondition(Conditions.RUINS_EXTENSIVE) ||
                planet.hasCondition(Conditions.RUINS_WIDESPREAD) ||
                planet.hasCondition(Conditions.RUINS_SCATTERED);
    }

    private static List<String> getVariantsToSpawn() {
        List<String> variantsToSpawn = new ArrayList<String>();
        variantsToSpawn.add("ork_anathema_standard");
        variantsToSpawn.add("ork_triumph_standard");
        variantsToSpawn.add("ork_criterion_standard");
        variantsToSpawn.add("ork_vocifer_standard");
        variantsToSpawn.add("ork_criterion_standard");
        variantsToSpawn.add("ork_despot_standard");
        variantsToSpawn.add("ork_supermule_standard");
        variantsToSpawn.add("ork_cyclops_standard");
        variantsToSpawn.add("ork_apsis_standard");

        Collections.shuffle(variantsToSpawn, MathUtils.getRandom());
        return variantsToSpawn;
    }
}