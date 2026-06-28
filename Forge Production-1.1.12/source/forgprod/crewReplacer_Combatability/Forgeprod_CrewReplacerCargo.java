package forgprod.crewReplacer_Combatability;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import data.scripts.CrewReplacer_Log;
import data.scripts.crewReplacer_Job;
import data.scripts.crewReplacer_Main;
import forgprod.abilities.conversion.logic.base.ProductionLogic;
import forgprod.abilities.conversion.logic.production.*;
import forgprod.abilities.conversion.support.ProductionType;

import java.util.ArrayList;

public class Forgeprod_CrewReplacerCargo extends Forgeprod_DefaultCargo {
    /*overview of what i have done:
    * 1) replaced every instance of cargo being checked or used in any way with a crew replacer variant (NOTE: if crew replacer is not installed, it will swap to a 'normal' checking system automatically.)
    *   -also note: this is oganized by 'task'. the following count as diffrent tasks:
    *   a) the mother ship even.
    *   b) each diffrent forge production counts as a diffrent task
    *   c) every item that is used in many diffrent forge productions (crew and heavy_matchenery) counts as a single task
    *
    * 2) changed classes:
    *   -NOTE: i had a list somewhere, i swore i did, but i lost it. and im kinda frustrated, because i had to look through a lot of things. so here is a list of the classes i added crew replacer support in instead.
            (not tested)TooltipIncomeSection
            (started)HullPartsProductionLogic
            (done)(not tested)TooltipAssembly
            (done)(not tested)BreakdownLogic
            (not tested)FleetwideProductionChecks
            (not tested)SideBar
            (done) ProductionType (used by ProdctionLogic. might be important???)
            (done)(not tested)ProductionLogic
    *
    *over view of what i wanted to do but have not:
    * 1) made it so one could change what is used when 'forgeing' a starship. this is the only thing exscluded from my crew replacer update.
    *
    *how to update or add new things, and still maintain in line with crew replacers requirements:
    *  1) when updating the mother ship events requirements, you will need to go into Forgeprod_CrewReplacerCargo.startup() and change the String[] MS_Event_Requirements to contane only the commoditys this taks uses.
    *  2) when adding a new forgeProduction, there are 2 steps:
    *   2.1) (if the given forge production does not have the default 2 cargo spaces) @OverWrite its given 'public ArrayList<String> getRequired()' function to output all its inputs
    *   2.2) in Forgeprod_CrewReplacerCargo.startup() run 'generatJobsFromProductionLogic' with the new forge productions relevant input.
    *  3) when checking or removing a given item:
    *   3.1) make sure to check if you have the item with the following command: 'Forgeprod_DefaultCargo.active.getCommodityAmount(PROUCTION_TYPE,COMMODITY,CARGO)'
    *                                                                         or 'Forgeprod_DefaultCargo.active.getCommodityAmount("NAME OF TASk (gettable in 'Forgeprod_DefaultCargo' as a TYPE_)"),COMMODITY,CARGO)'
    *   3.2) make sure to remove the item with the following command:            'Forgeprod_DefaultCargo.active.removeCommodity(PROUCTION_TYPE,COMMODITY,amountToRemove,CARGO)'
    *                                                                         or 'Forgeprod_DefaultCargo.active.removeCommodity("NAME OF TASk (gettable in 'Forgeprod_DefaultCargo' as a TYPE_)"),COMMODITY,amountToRemove,CARGO)'
    *
    * if you have any questions, feel free to ask.
    * */
    /*
    issues:
        1) (fixed) when i try to complress my new jar file, i get a error that says the following: (o ;pst the error, but it was something along the lines of 'wrong major.minor version 60, for the ForgprodShowRetriveleCost plugin)
    *       -i forgot to set the java version in one or two of the things. so wrong java lol.
    * tasks required to compleat:
    * 1)(done)(tested) mothership event
    *   -replcace requirement
    *   -test (NOTE: there are no consumed resources in this even yet.)
    *
    * 2) forge production
    *   -find all instances of forge production requesting, and using resources.
    *   -find were on earth the used resources are displayed, and modify it to display the new resorses lost.
    *   - -important- make sure this meshes well with the normal display, to make sure nothing is cahnged.
    *   -test*
    * 3) disable most crewReplacer logs. set ones i need to ehter only happen on errors, or only when crewReplacer logging is online.

        -location of scripts:
            (NA)(not required)forge_production displays all losses. (NOTE: the way it displays losses might not require crew replacer. i will need to ask the original mod offer there opinions.)
            (not tested)TooltipIncomeSection
                -this one is started. there are a few issues that will need to be adressed however:
                1) there are a few 'request resources' variables that are used in more then one forging production. this will need to be seperated.
                2) this code looks like it was not built in a expandable way for some reason and this bugs meeeeeeeeeeeeee.
                    -also this means i will need to give them a extra step for adding new content
            (started)(not tested)HullPartsProductionLogic
                -i still need to edit the 'ship hulls' built with this.
            (done)(not tested)TooltipAssembly
            ForgeProductionAbility
            (done)(not tested)BreakdownLogic
                -note: the number of crew available to be lost is complicated. suffice to say, never triggers when there is no crew to remove. or when only ran on automated ships.
                cool, but that does mean that the max amount of loss calculation can be very strange. I should be able to handle it though.
            (not tested)FleetwideProductionChecks
            (not tested)SideBar
            FleetwideModuleManager
                -this handles the small ship creation. it might be wize to look into the possability of adding it to crew replacer, and maybe creating a 'metiforical crew' so i may also use the base hulls in production, as the moder intended.
            (done) ProductionType (used by ProdctionLogic. might be important???)
            (done)(not tested)ProductionLogic

    /
     */
    public Forgeprod_CrewReplacerCargo(){
        Forgeprod_DefaultCargo.active = this;
        startup();
    }
    private final String name = "ForgeProd";
    private void startup(){
        String[] MS_Event_Requirements = {Commodities.CREW,Commodities.HEAVY_MACHINERY,Commodities.GAMMA_CORE};//required resorses for mother ship event
        String[] BF_Requirements = {Commodities.CREW,Commodities.HEAVY_MACHINERY};//required resource for things not bound to the mother ship event, or to any particular forge production

        generatJobsFromProductionLogic(MetalsProductionLogic.getInstance());
        generatJobsFromProductionLogic(TransplutonicsProductionLogic.getInstance());
        generatJobsFromProductionLogic(FuelProductionLogic.getInstance());
        generatJobsFromProductionLogic(SuppliesProductionLogic.getInstance());
        generatJobsFromProductionLogic(MachineryProductionLogic.getInstance());
        generatJobsFromProductionLogic(HullPartsProductionLogic.getInstance());

        createJobs(TYPE_BASE_REQUIRED,BF_Requirements);
        //crewReplacer_Main.getJob(CN(TYPE_BASE_REQUIRED,Commodities.HEAVY_MACHINERY)).addNewCrew("AIretrofit_CombatDrone",1,10);
        //crewReplacer_Main.getJob(CN("TYPE_"+FuelProductionLogic.getInstance().getProductionType().name(),Commodities.VOLATILES)).addNewCrew("AIretrofit_CombatDrone",1,10);
        createJobs(TYPE_MS_Event,MS_Event_Requirements);
    }
    private void generatJobsFromProductionLogic(ProductionLogic logic){
        String TYPE = "TYPE_"+logic.getProductionType().name();
        createJobs(TYPE,logic.getRequired().toArray(new String[]{}));
    }
    private void createJobs(String TYPE,String[] job_Materials){
        for (String a : job_Materials){
            CrewReplacer_Log.loging("job: "+TYPE+", item: "+a,this);
            String temp = this.CN(TYPE,a);
            crewReplacer_Job job = crewReplacer_Main.getJob(temp);
            String crewSetName = name+"_"+a;
            crewReplacer_Main.getCrewSet(crewSetName).addCrewSet(a);//this crew set is created here, and adds the base commodity crew set this set. reason: this lets a player add a commodity to all possible uses in this mod (like adding heavy machinery to all forge productions) at once. or just to one type of forge production by using the job, and not the crew set. or to everything connected to crew replacer with the same commodityID. feel free to ask questions.
            job.addCrewSet(crewSetName);
            job.addNewCrew(a,1,10);//sets the defalt value of the defalt commodity. this is defalt defalt.
        }
    }
    @Override
    public float getCommodityAmount(String TYPE, String commodity,CargoAPI cargo) {
        return crewReplacer_Main.getJob(CN(TYPE,commodity)).getAvailableCrewPower(cargo);
    }

    @Override
    public void removeCommodity(String TYPE, String commodity, float amount,CargoAPI cargo) {
        crewReplacer_Main.getJob(CN(TYPE,commodity)).automaticlyGetAndApplyCrewLost(cargo,(int)amount,(int)amount);
    }
    private String CN(String TYPE, String commodity){
        return name+"_"+TYPE+"_"+commodity;
    }
}
