package io.github.genichimaruo.singuloarsenal.gametest;

import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Development-only test functions; excluded from distributable JARs. */
public final class ArsenalTestRegistration {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, "singulo_arsenal");
    public static void register(IEventBus bus) {
        register("arsenalgametests.gearonlyacceptsenergy", ArsenalGameTests.class, "gearOnlyAcceptsEnergy");
        register("arsenalgametests.singulosmeschargesarsenalandpreservesmodules", ArsenalGameTests.class, "singuloSmesChargesArsenalAndPreservesModules");
        register("arsenalgametests.statesurvivessaveandload", ArsenalGameTests.class, "stateSurvivesSaveAndLoad");
        register("arsenalgametests.accretionconsumespelletsonlywhentheyfit", ArsenalGameTests.class, "accretionConsumesPelletsOnlyWhenTheyFit");
        register("arsenalgametests.meissnerexpelsprojectiles", ArsenalGameTests.class, "meissnerExpelsProjectiles");
        register("arsenalgametests.persistentcurrentbalanceswithoutloss", ArsenalGameTests.class, "persistentCurrentBalancesWithoutLoss");
        register("arsenalgametests.chandrasekharlimitcapsahitandcharges", ArsenalGameTests.class, "chandrasekharLimitCapsAHitAndCharges");
        register("arsenalgametests.collapseatthelimitlocksthesuit", ArsenalGameTests.class, "collapseAtTheLimitLocksTheSuit");
        register("arsenalgametests.ventrequiresminimumpressure", ArsenalGameTests.class, "ventRequiresMinimumPressure");
        register("arsenalgametests.pastaphasedrivesattributes", ArsenalGameTests.class, "pastaPhaseDrivesAttributes");
        register("arsenalgametests.eventhorizoncatcheslethalhitonce", ArsenalGameTests.class, "eventHorizonCatchesLethalHitOnce");
        register("arsenalgametests.collapsarsuitpaysfordamageinenergy", ArsenalGameTests.class, "collapsarSuitPaysForDamageInEnergy");
        register("arsenalgametests.metricdrivefliesonenergyalone", ArsenalGameTests.class, "metricDriveFliesOnEnergyAlone");
        register("arsenalgametests.jetburstrequiresbootsandburnsbehind", ArsenalGameTests.class, "jetBurstRequiresBootsAndBurnsBehind");
        register("arsenalgametests.nohairclearsharmfuleffects", ArsenalGameTests.class, "noHairClearsHarmfulEffects");
        register("arsenalgametests.quenchonfifthhit", ArsenalGameTests.class, "quenchOnFifthHit");
        register("arsenalgametests.casimirignoresarmorwhenarmed", ArsenalGameTests.class, "casimirIgnoresArmorWhenArmed");
        register("arsenalgametests.tidalstressstacksandshears", ArsenalGameTests.class, "tidalStressStacksAndShears");
        register("arsenalgametests.cannonfiresforwardonlyandstopsatwalls", ArsenalGameTests.class, "cannonFiresForwardOnlyAndStopsAtWalls");
        register("arsenalgametests.cannonhitstargetscontainingtherayorigin", ArsenalGameTests.class, "cannonHitsTargetsContainingTheRayOrigin");
        register("arsenalgametests.cannonneverhurtsthewielder", ArsenalGameTests.class, "cannonNeverHurtsTheWielder");
        register("arsenalgametests.cannonfiresamagazinethenreloads", ArsenalGameTests.class, "cannonFiresAMagazineThenReloads");
        register("arsenalgametests.switchingawaycannotskipanemptymagazinesreload", ArsenalGameTests.class, "switchingAwayCannotSkipAnEmptyMagazinesReload");
        register("arsenalgametests.expiredshotsstillreloadanemptymagazine", ArsenalGameTests.class, "expiredShotsStillReloadAnEmptyMagazine");
        register("arsenalgametests.onlythefirstinstantofashotdrillsterrain", ArsenalGameTests.class, "onlyTheFirstInstantOfAShotDrillsTerrain");
        register("arsenalgametests.reaverfocusiscarriedonthestack", ArsenalGameTests.class, "reaverFocusIsCarriedOnTheStack");
        register("arsenalgametests.drillinggivesoutatitsdepth", ArsenalGameTests.class, "drillingGivesOutAtItsDepth");
        register("arsenalgametests.meltdowngoesofftheinstanttheshotlands", ArsenalGameTests.class, "meltdownGoesOffTheInstantTheShotLands");
        register("arsenalgametests.platingmergesintothearmorvalue", ArsenalGameTests.class, "platingMergesIntoTheArmorValue");
        register("arsenalgametests.integrationinstallsandpaysformodules", ArsenalGameTests.class, "integrationInstallsAndPaysForModules");
        register("arsenalgametests.placinggearunpacksitsmodulesexactlyonce", ArsenalGameTests.class, "placingGearUnpacksItsModulesExactlyOnce");
        register("arsenalgametests.legacyintegratorsavegainsfivesocketswithoutlosingmodules", ArsenalGameTests.class, "legacyIntegratorSaveGainsFiveSocketsWithoutLosingModules");
        register("arsenalgametests.integrationrefuseswrongorexcessmodules", ArsenalGameTests.class, "integrationRefusesWrongOrExcessModules");
        register("arsenalgametests.bitspayenergyandhithostiles", ArsenalGameTests.class, "bitsPayEnergyAndHitHostiles");
        register("arsenalgametests.fluxpinconsumesthelastpartialcharge", ArsenalGameTests.class, "fluxPinConsumesTheLastPartialCharge");
        register("arsenalgametests.balancingdifferentcapacitiesconservesenergy", ArsenalGameTests.class, "balancingDifferentCapacitiesConservesEnergy");
        register("arsenalgametests.integratorchargingmarksthechunkforsaving", ArsenalGameTests.class, "integratorChargingMarksTheChunkForSaving");
        register("arsenalgametests.holdingarmedbladedoesnotmodifydronedamage", ArsenalGameTests.class, "holdingArmedBladeDoesNotModifyDroneDamage");
        register("arsenalgametests.cannonpulseusesitsamplifier", ArsenalGameTests.class, "cannonPulseUsesItsAmplifier");
        register("arsenalgametests.breakertakesthreebythreeunlesssneaking", ArsenalGameTests.class, "breakerTakesThreeByThreeUnlessSneaking");
        register("arsenalgametests.breakerleavescontainersalone", ArsenalGameTests.class, "breakerLeavesContainersAlone");
        register("arsenalgametests.canceledminingdoesnotbreakneighborsorspendenergy", ArsenalGameTests.class, "canceledMiningDoesNotBreakNeighborsOrSpendEnergy");
        register("arsenalgametests.protectedneighborsdonotcostminingenergy", ArsenalGameTests.class, "protectedNeighborsDoNotCostMiningEnergy");
        register("arsenalgametests.sawfellslivingtreesbutnotlogwalls", ArsenalGameTests.class, "sawFellsLivingTreesButNotLogWalls");
        register("arsenalgametests.drillfollowsveinsintothepack", ArsenalGameTests.class, "drillFollowsVeinsIntoThePack");
        register("arsenalgametests.toolswithoutpowerdigathandspeed", ArsenalGameTests.class, "toolsWithoutPowerDigAtHandSpeed");
        register("arsenalgametests.squidpickfindsoresnearestfirst", ArsenalGameTests.class, "squidPickFindsOresNearestFirst");
        register("arsenalgametests.sensitivitymodulestunethepickintheotherhand", ArsenalGameTests.class, "sensitivityModulesTuneThePickInTheOtherHand");
        register("arsenalgametests.pickkeepsthescannerssensitivity", ArsenalGameTests.class, "pickKeepsTheScannersSensitivity");
        register("arsenalgametests.craftingpickpreservessensitivityforclickandshiftclick", ArsenalGameTests.class, "craftingPickPreservesSensitivityForClickAndShiftClick");
        register("arsenalgametests.canisterstorespelletsandfeedsgearfirst", ArsenalGameTests.class, "canisterStoresPelletsAndFeedsGearFirst");
        register("arsenalgametests.smeltercooksoresbutnotstone", ArsenalGameTests.class, "smelterCooksOresButNotStone");
        register("arsenalgametests.smeltingkeepsoutputsthatexceedonestack", ArsenalGameTests.class, "smeltingKeepsOutputsThatExceedOneStack");
        register("arsenalgametests.inductionfeedstheheldtoolfromthechest", ArsenalGameTests.class, "inductionFeedsTheHeldToolFromTheChest");
        register("arsenalgametests.collectorpullslooseitemsbutnotthrownones", ArsenalGameTests.class, "collectorPullsLooseItemsButNotThrownOnes");
        register("arsenalgametests.everyrecipeloadsagainstrealsingulo", ArsenalGameTests.class, "everyRecipeLoadsAgainstRealSingulo");
        register("cannonmigrationtests.legacycannonstackcodec", CannonMigrationTests.class, "legacyCannonStackCodec");
        register("cannonmigrationtests.legacycannonplayerinventory", CannonMigrationTests.class, "legacyCannonPlayerInventory");
        register("cannonmigrationtests.legacycannonchest", CannonMigrationTests.class, "legacyCannonChest");
        register("cannonmigrationtests.legacycannonframe", CannonMigrationTests.class, "legacyCannonFrame");
        register("cannonmigrationtests.legacycannondroppedentity", CannonMigrationTests.class, "legacyCannonDroppedEntity");
        register("cannonmigrationtests.legacycannondatapackcodecs", CannonMigrationTests.class, "legacyCannonDatapackCodecs");
        register("nihoniumgametests.nihoniumrepairswithrealsinguloandhasnopower", NihoniumGameTests.class, "nihoniumRepairsWithRealSinguloAndHasNoPower");
        register("nihoniumgametests.nihoniumtoolskeeptheirtieranddurability", NihoniumGameTests.class, "nihoniumToolsKeepTheirTierAndDurability");
        register("nihoniumgametests.nihoniumarmorretainsvanillatrimsandstats", NihoniumGameTests.class, "nihoniumArmorRetainsVanillaTrimsAndStats");
        FUNCTIONS.register(bus);
    }
    private static void register(String name, Class<?> owner, String method) {
        FUNCTIONS.register(name, () -> helper -> {
            try { owner.getMethod(method, GameTestHelper.class).invoke(null, helper); }
            catch (java.lang.reflect.InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException runtime) throw runtime;
                if (e.getCause() instanceof Error error) throw error;
                throw new RuntimeException(e.getCause());
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        });
    }
}