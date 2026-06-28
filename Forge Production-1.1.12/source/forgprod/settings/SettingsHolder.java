package forgprod.settings;

import java.util.ArrayList;

/**
 * Convenience class for holding setting values. Values are overwritten by DataSupplier at game load.
 * @author Ontheheavens
 * @since 02.12.2022
 */

public class SettingsHolder {

        // Here: Installation Settings

        public static float CREW_REQ_PER_CAPACITY = 7.5f;
        public static float SUPPLY_REQ_PER_CAPACITY = 0.5f;
        public static float MAXIMUM_CR_DECREASE = 0.2f;

        // Here: Miscellaneous Settings

        public static boolean ENABLE_FEATURE_UNLOCK = false;
        public static String INTERACTION_CALL_HOTKEY = "LCONTROL";

        public static boolean ENABLE_SLOW_MOVE_PENALTY = true;
        public static boolean ENABLE_DETECT_AT_RANGE_PENALTY = true;
        public static boolean ENABLE_BURN_ABILITIES_INCOMPATIBILITY = true;
        public static boolean ENABLE_GO_DARK_INCOMPATIBILITY = true;

        public static float SENSOR_PROFILE_INCREASE = 100;
        public static int SHIP_LIST_SIZE = 5;
        public static int NOTIFICATION_INTERVAL = 3;

        // Here: Capacity Settings

        public static int BASE_CAPACITY_FRIGATE = 1;
        public static int BASE_CAPACITY_DESTROYER = 2;
        public static int BASE_CAPACITY_CRUISER = 4;
        public static int BASE_CAPACITY_CAPITAL = 8;

        // A key variable that serves as a divisor for commodity values and multiplier for ship capacity.
        // Determines size of each production cycle unit, the bigger granularity the lower the size.
        // Sufficiently big granularity is needed to deal with cargo leftovers.
        public static int GRANULARITY = 20;

        public static float MINIMUM_MACHINERY_PERCENT = 0.5f;
        public static float MINIMUM_CR_PERCENT = 0.3f;

        public static int HULL_ASSEMBLING_CAPACITY_PER_RIG = 10;

        // Base Metals production values.
        public static float BASE_ORE_INPUT = 16;
        public static float BASE_METAL_OUTPUT = 10;
        public static float BASE_MACHINERY_USE_METALS = 6;

        // Base Transplutonics production values.
        public static float BASE_TRANSPLUTONIC_ORE_INPUT = 8;
        public static float BASE_TRANSPLUTONICS_OUTPUT = 4;
        public static float BASE_MACHINERY_USE_TRANSPLUTONICS = 8;

        // Base Fuel production values.
        public static float BASE_VOLATILES_INPUT = 1;
        public static float BASE_FUEL_OUTPUT = 8;
        public static float BASE_MACHINERY_USE_FUEL = 12;

        // Base Supplies production values.
        public static float BASE_METALS_INPUT_SUPPLIES = 8;
        public static float BASE_TRANSPLUTONICS_INPUT_SUPPLIES = 2;
        public static float BASE_SUPPLIES_OUTPUT = 6;
        public static float BASE_MACHINERY_USE_SUPPLIES = 10;

        // Base Machinery production values.
        public static float BASE_METAL_INPUT_MACHINERY = 10;
        public static float BASE_TRANSPLUTONICS_INPUT_MACHINERY = 1;
        public static float BASE_MACHINERY_OUTPUT = 4;
        public static float BASE_MACHINERY_USE_MACHINERY = 14;

        // Base Hull Parts production values.
        public static boolean REQUIRE_SALVAGE_RIGS = false;
        public static boolean ENABLE_HULL_PRODUCTION_LIMITATIONS = true;
        public static float BASE_METAL_INPUT_HULL_PARTS = 10;
        public static float BASE_TRANSPLUTONICS_INPUT_HULL_PARTS = 1;
        public static float BASE_SUPPLIES_INPUT_HULL_PARTS = 2;
        public static float BASE_MACHINERY_INPUT_HULL_PARTS = 2;
        public static float BASE_HULL_PARTS_OUTPUT = 1;
        public static float BASE_MACHINERY_USE_HULL_PARTS = 18;

        public static float HULL_COST_MULTIPLIER = 1f;

        // Here: Machinery Breakdown  Settings
        public static float BASE_BREAKDOWN_CHANCE = 0.3f;
        public static float BREAKDOWN_SEVERITY = 0.05f;
        public static float DAILY_CR_DECREASE = 0.02f;

        // Here: Special Item bonus outputs.
        public static int CATALYTIC_CORE_OUTPUT_BONUS = 2;
        public static int SYNCHROTRON_CORE_OUTPUT_BONUS = 4;
        public static int CORRUPTED_NANOFORGE_OUTPUT_BONUS = 1;
        public static int PRISTINE_NANOFORGE_OUTPUT_BONUS = 2;
        public static float CORRUPTED_NANOFORGE_BREAKDOWN_DECREASE = 0.2f;
        public static float PRISTINE_NANOFORGE_BREAKDOWN_DECREASE = 0.5f;

        // Hull production allowances
        public static ArrayList<String> hull_prod_allowedTags = new ArrayList<>();
        public static ArrayList<String> hull_prod_allowedManufacturers = new ArrayList<>();
        public static ArrayList<String> hull_prod_bannedHullmods = new ArrayList<>();
        public static ArrayList<String> hull_prod_forcedVariants = new ArrayList<>();
        public static boolean hull_prod_allowForcedVariants = true;
        public static boolean hull_prod_allowedFrigate = true;
        public static boolean hull_prod_allowedDestroyer = false;
        public static boolean hull_prod_allowedCruiser = false;
        public static boolean hull_prod_allowedCapital = false;

        // Hullmod allowances.
        public static boolean hullmod_allowedFrigate = true;
        public static boolean hullmod_allowedDestroyer = false;
        public static boolean hullmod_allowedCruiser = false;
        public static boolean hullmod_allowedCapital = false;
        public static boolean hullmod_allowMulti = false;
        public static String getDefaultVariant() {
                if (SettingsHolder.hull_prod_forcedVariants.isEmpty()) return "picket_Assault";
                else return SettingsHolder.hull_prod_forcedVariants.get(0);
        }
        public static ArrayList<String> getAllowedHullmodNames(){
                ArrayList<String> out = new ArrayList<>();
                if (hullmod_allowedFrigate) out.add("frigate");
                if (hullmod_allowedDestroyer) out.add("destroyer");
                if (hullmod_allowedCruiser) out.add("cruiser");
                if (hullmod_allowedCapital) out.add("capital");
                return out;
        }
}
