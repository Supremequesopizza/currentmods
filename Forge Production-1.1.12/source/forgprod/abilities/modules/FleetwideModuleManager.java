package forgprod.abilities.modules;

import java.util.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import forgprod.abilities.conversion.support.ProductionConstants;
import forgprod.abilities.modules.dataholders.ProductionCapacity;
import forgprod.abilities.modules.dataholders.ProductionModule;
import forgprod.settings.SettingsHolder;
import org.apache.log4j.Logger;

/**
 * Registry of all modules and production capacities possessed by the fleet.
 * To be used as an intermediary in production logic and by the control panel for purposes of mass toggling.
 * Singleton-pattern class, persists between sessions, holds info about production configurations.
 * <p>
 *
 * @author Ontheheavens
 * @since 30.11.2022
 */

public class FleetwideModuleManager {

    private static FleetwideModuleManager managerInstance;
    private Set<ProductionCapacity> capacitiesIndex;
    private HashMap<FleetMemberAPI,ArrayList<ProductionModule>> moduleIndex;
    private float accumulatedHullParts;
    private String designatedVariantId;

    private FleetwideModuleManager() {
        this.moduleIndex = new HashMap<>();
        this.capacitiesIndex = new HashSet<>();
        this.accumulatedHullParts = 0f;
        this.ensureDefaultVariant();
    }
    public static void replaceThing(){
        managerInstance = (FleetwideModuleManager) Global.getSector().getPersistentData().get("forgprod_index");
        if (managerInstance != null){
            managerInstance = null;
        }
        if (managerInstance == null) {
            managerInstance = new FleetwideModuleManager();
            Global.getSector().getPersistentData().put("forgprod_index", managerInstance);
        }
    }
    public static FleetwideModuleManager getInstance() {
        managerInstance = (FleetwideModuleManager) Global.getSector().getPersistentData().get("forgprod_index");
        if (managerInstance == null) {
            managerInstance = new FleetwideModuleManager();
            Global.getSector().getPersistentData().put("forgprod_index", managerInstance);
        }
        if (managerInstance.designatedVariantId == null) {
            managerInstance.ensureDefaultVariant();
        }
        return managerInstance;
    }

    private void ensureDefaultVariant() {
        this.designatedVariantId = SettingsHolder.getDefaultVariant();
    }

    //todo: find out how this works, in relation to module index. for real though, wtf does this work???
    public Set<ProductionCapacity> getCapacitiesIndex() {
        //log.info("number of capacities: "+this.capacitiesIndex.size());
        return this.capacitiesIndex;
    }
    public void addCapacityIfPossible(ProductionCapacity capacity){
        for (ProductionCapacity a : capacitiesIndex) if (a.getParentModule().getParentFleetMember().equals(capacity.getParentModule().getParentFleetMember()) && a.getParentModule().getHullmodId().equals(capacity.getParentModule().getHullmodId())) return;
        capacitiesIndex.add(capacity);
    }
    public static final Logger log = Global.getLogger(FleetwideModuleManager.class);
    //todo: please note, only 50% of functions think there is a issue were. go though them one by one to find out if there is.
    public HashMap<FleetMemberAPI,ArrayList<ProductionModule>> getModuleIndex() {
        //int total = 0;
        //for (ArrayList<ProductionModule> a : moduleIndex.values()) for (ProductionModule b : a) total+=1;
        //log.info("number of modules: "+this.moduleIndex.size()+" for total size of: "+total);
        return moduleIndex;
    }
    /// gets if there is a module on the giving ship
    public ArrayList<ProductionModule> getModulesOnShip(FleetMemberAPI member){
        return moduleIndex.get(member);
    }

    public float getAccumulatedHullParts() {
        return accumulatedHullParts;
    }

    public void updateAccumulatedHullParts(float value) {
        this.accumulatedHullParts = accumulatedHullParts + value;
    }

    public ShipVariantAPI getDesignatedVariant() {
        return Global.getSettings().getVariant(designatedVariantId);
    }

    public void setDesignatedVariant(ShipVariantAPI designatedVariant) {
        this.designatedVariantId = designatedVariant.getHullVariantId();
    }

    public void refreshIndexes(CampaignFleetAPI fleet) {
        if (fleet == null) return;
        this.cullCapacitiesIndex(fleet);
        this.cullModuleIndex(fleet);
        this.addModulesToIndex(fleet);
        Global.getSector().getPersistentData().put("forgprod_index", managerInstance);
    }

    private void addModulesToIndex(CampaignFleetAPI fleet) {
        List<String> hullmods = new ArrayList<>(ProductionConstants.ALL_HULLMODS);
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            ArrayList<ProductionModule> add = new ArrayList<>();
            if(moduleIndex.get(member) != null) add.addAll(moduleIndex.get(member));
            boolean canAdd = false;
            //if (moduleIndex.containsKey(member)) continue;//this should fix a lot of issues. but wait...
            for (String b : hullmods){
                boolean canAdd2 = false;
                if (member.getVariant().getHullMods().contains(b)){
                    //System.out.println("addMTI: "+member.getShipName()+": found hullmod if ID:"+b);
                    canAdd2 = true;
                    for (ProductionModule c : add) if (c.getHullmodId().equals(b)){
                        //System.out.println("addMTI: "+member.getShipName()+": already has this hullmod of ID"+b);
                        canAdd2 = false;
                        break;
                    }
                }
                if (canAdd2){
                    //System.out.println("addMTI: "+member.getShipName()+": finally, adding hullmod of ID"+b);
                    canAdd = true;
                    add.add(new ProductionModule(member, b));
                }
            }
            if (add.isEmpty() || !canAdd) continue;
            //log.info("adding a new module of ship for a ship size, total size of: "+member.getShipName()+", "+add.size()+", "+moduleIndex.size());
            moduleIndex.put(member,add);
            /*if (!Collections.disjoint(member.getVariant().getHullMods(), hullmods)) {
                if (this.moduleIndex.get(member) == null) {
                    ProductionModule module = new ProductionModule(member, getInstalledHullmod(member));
                    moduleIndex.put(member, module);
                }
            }*/
        }
    }

    //todo: this needs an upgrade... it just removes items if a giving ship is gone.
    //      I guess its fine, so long as it works.
    private void cullModuleIndex(CampaignFleetAPI fleet) {
        Iterator<FleetMemberAPI> members = moduleIndex.keySet().iterator();
        while (members.hasNext()) {
            FleetMemberAPI member = members.next();
            boolean notPresent = (member.getFleetData() == null)
                    || (member.getFleetData().getFleet() != fleet.getFleetData().getFleet());
            if (notPresent){
                if (moduleIndex.get(member) != null) {
                    for (ProductionModule a : moduleIndex.get(member)) {
                        a.clearCapacities();
                    }
                }
                members.remove();
                //if (moduleIndex.get(member).isEmpty())members.remove();
                continue;
            }
            if (moduleIndex.get(member) == null){
                members.remove();
                continue;
            }
            Iterator<ProductionModule> iterator = moduleIndex.get(member).iterator();
            while (iterator.hasNext()){
                ProductionModule a = iterator.next();
                if (!member.getVariant().getHullMods().contains(a.getHullmodId())){
                    a.clearCapacities();
                    iterator.remove();
                }
            }
            if (moduleIndex.get(member).isEmpty()) members.remove();

            /*boolean noHullmod = !hasInstalledHullmod(member);
            if (notPresent || noHullmod) {
                ProductionModule module = moduleIndex.get(member);
                module.clearCapacities();
                members.remove();
            }*/
        }
    }

    private void cullCapacitiesIndex(CampaignFleetAPI fleet) {
        Iterator<ProductionCapacity> capacities = capacitiesIndex.iterator();
        while (capacities.hasNext()) {
            ProductionCapacity capacity = capacities.next();
            FleetMemberAPI capacityHolder = capacity.getParentModule().getParentFleetMember();
            boolean notPresent = (capacityHolder.getFleetData() == null)
                    || (capacityHolder.getFleetData().getFleet() != fleet.getFleetData().getFleet());
            if (notPresent || capacity.getParentModule() == null || !capacityHolder.getVariant().getHullMods().contains(capacity.getParentModule().getHullmodId())){
                capacities.remove();
            }
        }
    }

    //done.
    public ArrayList<ProductionModule> getSpecificModule(FleetMemberAPI member) {
        return this.moduleIndex.get(member);
    }

    /**
     * @param index 0 for primary, 1 for secondary
     * @return can be null.
     */

    /*public ProductionCapacity getSpecificCapacity(FleetMemberAPI member, int index) {
        if (member == null) { return null; }
        ProductionModule module = getSpecificModule(member);
        if (module == null) { return null; }
        if ((index + 1) >  module.getModuleCapacities().size()) {
            return null;
        }
        return module.getModuleCapacities().get(index);
    }*/

    /*private String getInstalledHullmod(FleetMemberAPI member) {
        List<String> hullmods = new ArrayList<>(ProductionConstants.ALL_HULLMODS);
        for (String checkedHullmodId : member.getVariant().getHullMods()) {
            if (hullmods.contains(checkedHullmodId)) {
                return checkedHullmodId;
            }
        }
        return null;
    }

    private boolean hasInstalledHullmod(FleetMemberAPI member) {
        return member.getVariant().hasHullMod(moduleIndex.get(member).getHullmodId());
    }*/

}
