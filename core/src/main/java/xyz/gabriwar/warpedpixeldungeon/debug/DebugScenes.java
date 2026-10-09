/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.QuickSlot;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.WeatherState;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Comfy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.WarmthBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.audio.Earshot;
import xyz.gabriwar.warpedpixeldungeon.audio.RoomAcoustics;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.Torch;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFireblast;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfLightning;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.TownInteriorLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientPlayer;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSounds;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.DungeonLife;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.CaravanAmbush;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.CaveLife;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.CaveSites;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.HuntEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.LayerHazards;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.MountainSites;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.Ores;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldCritters;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.PeakLife;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoadTraffic;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SettlementLights;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEvents;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;

/**
 * Debug scenes: a named situation set up around the hero in one tap, for testing
 * something specific without walking the dungeon for it. Applied from the debug
 * menu (Travel tab, "Scenes...") on the current run, or straight from the command line
 * on a fresh run:
 *
 *   ./gradlew :desktop:debug -Pscene=fliers-paralysed
 *
 * which starts a new game, lands on the surface and applies the scene there. Every
 * scene keeps the hero safe (infinite health) and lifts the fog.
 *
 * The lightning (effects/Lightning, items/wands/WandOfLightning) has lightning-water: the blank
 * room with a pool beside the hero, monsters standing in it and one on dry ground, and a wand of
 * lightning +3 to zap them with, its charge running over the water (into the hero too, a step in).
 *
 * The rooms Warped adds have scenes of their own, all on levels/WarpedRoomsLevel:
 * warped-rooms, warped-rooms-night (the same floor after dark, so the market is open) and
 * warped-rooms-only (the twelve Warped rooms alone, one of each).
 *
 * The surface's critters (levels/overworld/OverworldCritters) have two: overworld-critters
 * (a summer's day by a river) and overworld-critters-night (the same place after dark).
 *
 * The settlements' smoke and lamps (levels/overworld/SettlementAmbience) have
 * overworld-village-evening (a human village at a winter's dusk) and overworld-village-night
 * (the same after midnight). Their daily round (levels/overworld/VillageRoutine) has
 * overworld-village-day (a village by the water at a summer's mid-morning: the farmers out in
 * the fields, the fishers on the bank), overworld-village-dusk (a summer's dusk, the hero on
 * the well among the crowd gathered there), overworld-gnoll-day (a gnoll clan at work) and
 * overworld-metropolis-dawn (the biggest place around as it wakes, the worst case for
 * pathing: switch the lag monitor on); the night one shows the empty streets, the sleepers
 * and the guards' torches at the gates. All of these hold the rain off and put the fog
 * back on once the hero is there: what shows is what players see, through the real fog.
 *
 * The roads' life (levels/overworld/RoadTraffic, CaravanAmbush) has overworld-road (a morning on
 * a road a town's watch walks: a traveller of each kind about the hero, a caravan's cart coming
 * up behind him to pitch its stall just ahead, the watch coming out behind him on its way to the
 * next village, two outlaws on the road ahead) and overworld-ambush (today's caravan nearest
 * the origin marked for an ambush, the hero sixteen cells down the road from it). Both put the
 * fog back on once the hero is there too: walkers appear and leave only out of sight, and the
 * ambush waits for the stall to be seen and springs from cover.
 *
 * The world's timed events (levels/overworld/WorldEvents) have overworld-star (a star forced to
 * come down fifteen cells east of the hero at night: the streak, the blast, the smoking crater,
 * its fragment and its pin on the world map) and overworld-market (a travelling market forced
 * on the nearest human village, the hero set down by its well with gold to spend). Both force
 * the event through the registry the world itself reads, so what shows is what players get.
 * So do overworld-raid (bandits raiding the nearest human village by day, the hero set down
 * just past its fence, the fog back on so the band comes out of the doors he cannot see: the
 * smoke, the shut shops, the folk indoors, the watch fighting in a town of nine houses or more)
 * and overworld-hunt (the clock walked on to the dusk and a wolf pack loosed on a deer or two
 * ten cells from the hero: the chase, the kill and the feeding, or the deer's escape once the
 * pack is broken).
 *
 * The world's slices have their own, all travelling with toSlice. The ore in the rock
 * (levels/overworld/Ores) has layers-ores-caves (a cave slice at -6 near the origin where iron,
 * silver and gold veins glint in sight and a crystal seam or two stands by) and layers-ores-peaks
 * (slice +8 under a summit whose rock holds gold and skyiron, the clock walked on to a summer's
 * day and the hero warmed against the cold). Both hand the hero a pickaxe, put the fog back on
 * and log what shows within eight cells.
 *
 * The slices' small life (levels/overworld/CaveLife, PeakLife) has layers-caves-life (a cave
 * chamber at -3 near the origin with pools, a deep one for the blind fish, glowing fungus and
 * crystal seams: two bat colonies on the walls, a bat flitting, the glows lit, fish and drips
 * coming), layers-caves-deep (the same in a deep cavern at -9, bluer and stranger, with spores
 * in the air and one fungus patch beside the hero burnt to embers to show the deep air's
 * embers), layers-peaks-life (a summer's day on alpine meadow at +3 by the band's edge: the
 * eagle circling, marmots by the boulders) and layers-peaks-clouds (dawn on a summit at +7, or
 * the highest one near the origin, with the sea of clouds below all round). Each puts the fog
 * back on: what players see.
 *
 * The places of the caves (levels/overworld/CaveSites) have one scene each, landing the hero on
 * the stand cell of the place of that kind nearest the world's origin - on the slice it likes
 * best, else the nearest slice it lies on - with the fog back on: layers-cave-mine (-3, a pick in
 * his pack), layers-cave-grotto (-2), layers-cave-crystal (-6), layers-cave-camp (-4, gold and ore
 * to trade), layers-cave-shrine (-4, gold for the pool), layers-cave-rift (-7, a crucible of
 * deepsilver for the forge) and layers-cave-tomb (-9). Each logs where it is and what to look for.
 *
 * The places on the mountains (levels/overworld/MountainSites) have one each, sending the hero to
 * the nearest of its kind to the origin, to the spot a player would come up to it from:
 * layers-peak-hermit (the hut at a winter's dusk, its windows lighting and its chimney smoking),
 * layers-peak-eyrie (the eagles over their nest, out of their reach), layers-peak-cairn (below a
 * summit's cairn), layers-peak-springs (hot springs steaming in the winter snow, the hero left
 * unwarmed to try them), layers-peak-climber (a frozen climber's remains), layers-peak-tower (before
 * a ruined watchtower's gap) and layers-peak-pass (a waystation on a winter's night, unwarmed: its
 * fire and bedroll are the point). All put the fog back on and keep the sky clear.
 *
 * The slices' creatures (levels/overworld/OverworldFauna's bands) have layers-fauna-caves
 * (one of every cave band's kinds, each at its band's strength,
 * asleep around the hero in a dry chamber at -6 with a deep pool nearby for the fish),
 * layers-fauna-peaks (the same for the mountain bands, on open ground at +5) and
 * layers-goat-cliff (the hero on a cliff's lip at +2 by day, the drop at his back and a mountain
 * goat beside him: strike it and it butts him over). These keep the fog off: they are catalogues.
 *
 * The dangers of the slices (levels/overworld/HazardWatch) have one each, all with the hero's
 * debug health turned off on arrival so the harm can be judged: layers-hazard-firedamp (a cell
 * beside a firedamp pocket at -6, a fire wand and torches), layers-hazard-rockfall (west of a
 * thin wall of loose rock at -8, a pickaxe), layers-hazard-thinice (beside the thin edge of a
 * frozen tarn at +2, the clock walked on to a winter's day and the air held at a mild -1C, at
 * which all lake ice is thin) and
 * layers-hazard-thinair (on a ridge at +9 or the nearest high slice with one, the wind held at
 * 16, the hero warmed against the cold).
 *
 * The dungeon's small life and sounds (levels/ambience, docs/ambience.md) have one scene a place,
 * each on a floor of its own kind: ambience-sewers (dusk: frogs, roaches, snails, striders, gnats,
 * the pipes spilling), ambience-sewers-day (birdsong through the grates), ambience-prison,
 * ambience-caves, ambience-city, ambience-city-day (swifts and butterflies), ambience-halls,
 * ambience-frozen, ambience-nest, ambience-vault, ambience-temple, ambience-mines,
 * ambience-meadow, ambience-meadow-night (fireflies and crickets), ambience-shore and
 * ambience-catacomb. Each walks the clock on to the hour its life is out, sets the hero down
 * where the ground of the most of its kinds is in sight (DungeonLife.liveliest), puts the fog
 * back on and logs what lives there and what is heard at this hour. The life turns up over the
 * next seconds, in the cells the hero sees.
 *
 * Room acoustics (audio/RoomAcoustics, docs/ambience.md) has one scene a kind of space:
 * acoustics-room (the prison room nearest the landing, floor 7), acoustics-corridor (the middle of
 * the prison's longest corridor), acoustics-sewers (a sewer room on floor 2: it must ring),
 * acoustics-hall (the biggest space in the demon halls, 22), acoustics-cave (the biggest chamber in
 * the caves, 12), acoustics-cavern (the biggest space round a chamber of the world's caves at -9:
 * the cavern's tail and the echo off a far wall), acoustics-peaks (the nearest spot with an echo off
 * the rock, on alpine meadow at +3 on a summer's day, the rain held off), acoustics-field (the
 * postgame field, 27: it must stay dry), acoustics-church (the town's church) and acoustics-busy (a
 * chamber of the caves at -6, a cavern too, a storm forced and four foes set on the hero: listen for
 * the rain and the cave's own sounds dropping out while the blows ring). Each puts the fog back on,
 * logs the space as a sound finds it (Earshot.Snapshot.describe), waits for the muffled copies to be
 * in use (10 s after they load, 20 s at most), then plays three rounds 1.5 s apart - a heavy blow on
 * the hero, a blast six cells off, a blow behind the nearest door and a mine behind the nearest
 * rock - and logs what the budget let through and held back each round.
 *
 * The weather and its sounds (levels/ambience/WeatherSounds, docs/weather-sounds.md) have one scene
 * a condition, each holding the sky through the climate's debug overrides - every one a weather
 * scene touches is set, so nothing is left over from the scene before - with the fog back on and a
 * log of what to listen for and from which side: weather-rain-light, weather-rain and
 * weather-rain-heavy (0.08, 0.25 and 0.5, the wind held calm; the heavy one rolls far thunder),
 * weather-storm (a storm forced through ClimateManager.debugForceStorm, heavy rain in a 15 m/s wind
 * from the west; the Weather tab's Force storm box or weather-clear ends it) and weather-fog (the
 * rain held off and the wind calm, the clock walked on a turn at a time, the climate stepping with
 * it, until fog or the clearing after rain, when the trees drip, has held for FOG_HOLD turns), all
 * by a river and a wood (OverworldCritters.findCritterGround), the hero set down with open water
 * within three cells and a tree within five and told where each lies; weather-gale (16 m/s from the
 * west), weather-sleet and weather-hail on open plains, weather-snow and weather-blizzard (15 m/s
 * from the east) on the tundra or a snowfield, where the overlay draws snow (it falls only over
 * frozen ground, rain and sleet's streaks only over thawed), and weather-sandstorm (18 m/s from the
 * east, its sound and the overlay's driven sand and haze) in the desert (biomeGround), every ground
 * looked for near the origin off the render thread;
 * weather-indoors-rain (rain over the town, the hero in its inn: only the roof); and weather-clear,
 * which releases every weather override, the temperature's and the clouds' too.
 */
public final class DebugScenes {

	private DebugScenes(){}

	public interface Scene {
		String id();
		String title();
		void apply( Hero hero );
	}

	//the wind the weather scenes hold when the wind is not the point: under the gusts' 7 m/s, so
	//what is heard is the rain (or the snow, the sleet, the hail) alone
	static final float CALM = 2f;

	//all the scenes there are, in menu order
	public static final Scene[] SCENES = concat( new Scene[]{
			new Room(),
			new Fliers( "fliers-paralysed", "All fliers, paralysed", Paralysis.class ),
			new Fliers( "fliers-frozen", "All fliers, frozen", Frost.class ),
			new Fliers( "fliers-free", "All fliers, free", null ),
			new Pit(),
			new LightningWater() },
			//the effects gallery (debug/FxGallery) and every effects area's own scenes
			FxGallery.SCENES, new Scene[]{
			new Edges(),
			new Critters( false ),
			new Critters( true ),
			new VillageScene( "overworld-village-evening", "Surface: a village at dusk, chimneys and windows",
					GameCalendar.Season.WINTER, DayNightCycle.Phase.DUSK, 0.45f, WorldStructures.Faction.HUMAN, 4, false, true, 12 ),
			new VillageScene( "overworld-village-night", "Surface: a village after midnight, embers and night owls",
					GameCalendar.Season.WINTER, DayNightCycle.Phase.NIGHT, 0.75f, WorldStructures.Faction.HUMAN, 4, false, true, 12 ),
			new VillageScene( "overworld-village-day", "Surface: a village at work",
					GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.25f, WorldStructures.Faction.HUMAN, 6, true, false, 12 ),
			new VillageScene( "overworld-village-dusk", "Surface: the village gathers at the well",
					GameCalendar.Season.SUMMER, DayNightCycle.Phase.DUSK, 0.30f, WorldStructures.Faction.HUMAN, 6, false, false, 12 ),
			new VillageScene( "overworld-gnoll-day", "Surface: a gnoll clan at work",
					null, DayNightCycle.Phase.DAY, 0.25f, WorldStructures.Faction.GNOLL, 4, false, false, 12 ),
			new VillageScene( "overworld-metropolis-dawn", "Surface: a metropolis wakes up (lag check)",
					null, DayNightCycle.Phase.DAWN, 0.30f, WorldStructures.Faction.HUMAN, 45, false, false, 24 ),
			new Road(),
			new Ambush(),
			new StarFall(),
			new MarketDay(),
			new Raid(),
			new Hunt(),
			new OreScene( "layers-ores-caves", "Caves: ore veins and gem pockets", -6,
					Ores.Kind.IRON, Ores.Kind.SILVER, Ores.Kind.GOLD ),
			new OreScene( "layers-ores-peaks", "Peaks: skyiron and gold veins", 8,
					Ores.Kind.GOLD, Ores.Kind.SKYIRON ),
			new CaveScene( "layers-caves-life", "Caves: bats, drips, glowing fungus and crystal", -3, false ),
			new CaveScene( "layers-caves-deep", "Caves: a deep cavern at -9", -9, true ),
			new PeakScene( "layers-peaks-life", "Peaks: eagle and marmots", 3,
					GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.3f, false ),
			new PeakScene( "layers-peaks-clouds", "Peaks: above the clouds", 7,
					null, DayNightCycle.Phase.DAWN, 0.35f, true ),
			new CaveSiteScene( "layers-cave-mine", "Caves: an old mine", CaveSites.Type.MINE, -3 ),
			new CaveSiteScene( "layers-cave-grotto", "Caves: a mushroom grotto", CaveSites.Type.GROTTO, -2 ),
			new CaveSiteScene( "layers-cave-crystal", "Caves: a crystal cavern", CaveSites.Type.CRYSTAL, -6 ),
			new CaveSiteScene( "layers-cave-camp", "Caves: a miners' camp and its trader", CaveSites.Type.CAMP, -4 ),
			new CaveSiteScene( "layers-cave-shrine", "Caves: a lake shrine", CaveSites.Type.SHRINE, -4 ),
			new CaveSiteScene( "layers-cave-rift", "Caves: a burning rift and its forge", CaveSites.Type.RIFT, -7 ),
			new CaveSiteScene( "layers-cave-tomb", "Caves: a sealed tomb", CaveSites.Type.TOMB, -9 ),
			new PeakSiteScene( "layers-peak-hermit", "Heights: a hermit's hut at dusk", MountainSites.Kind.HERMIT,
					GameCalendar.Season.WINTER, DayNightCycle.Phase.DUSK, 0.45f, true ),
			new PeakSiteScene( "layers-peak-eyrie", "Heights: eagles over their eyrie", MountainSites.Kind.EYRIE,
					GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.3f, true ),
			new PeakSiteScene( "layers-peak-cairn", "Heights: a summit cairn", MountainSites.Kind.CAIRN,
					GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.3f, true ),
			new PeakSiteScene( "layers-peak-springs", "Heights: hot springs in the snow", MountainSites.Kind.SPRINGS,
					GameCalendar.Season.WINTER, DayNightCycle.Phase.DAY, 0.3f, false ),
			new PeakSiteScene( "layers-peak-climber", "Heights: a frozen climber", MountainSites.Kind.CLIMBER,
					GameCalendar.Season.WINTER, DayNightCycle.Phase.DAY, 0.3f, true ),
			new PeakSiteScene( "layers-peak-tower", "Heights: a ruined watchtower", MountainSites.Kind.TOWER,
					GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.3f, true ),
			new PeakSiteScene( "layers-peak-pass", "Heights: a waystation at night", MountainSites.Kind.PASS,
					GameCalendar.Season.WINTER, DayNightCycle.Phase.NIGHT, 0.3f, false ),
			new LayerFauna( "layers-fauna-caves", "Caves: a creature of every band", -6 ),
			new LayerFauna( "layers-fauna-peaks", "Peaks: a creature of every band", 5 ),
			new GoatCliff(),
			new HazardScene( "layers-hazard-firedamp", "Caves -6: a firedamp pocket, a fire wand and torches", HazardScene.FIREDAMP ),
			new HazardScene( "layers-hazard-rockfall", "Caves -8: loose rock and a pickaxe", HazardScene.ROCKFALL ),
			new HazardScene( "layers-hazard-thinice", "Mountains +2: a tarn of thin ice on a mild day", HazardScene.THIN_ICE ),
			new HazardScene( "layers-hazard-thinair", "Mountains +9: a wind-scoured ridge", HazardScene.THIN_AIR ),
			new WarpedRoomsScene( "warped-rooms", "Warped rooms: all, live, with supplies", false, false ),
			new WarpedRoomsScene( "warped-rooms-night", "Warped rooms at nightfall (market open)", true, false ),
			new WarpedRoomsScene( "warped-rooms-only", "Warped rooms only: one of each, no standard rooms", false, true ),
			new AmbienceScene( "ambience-sewers", "Sewers at dusk: frogs, roaches, snails, striders, gnats", 2, 0, DayNightCycle.Phase.DUSK ),
			new AmbienceScene( "ambience-sewers-day", "Sewers by day: birdsong through the grates, striders", 2, 0, DayNightCycle.Phase.DAY ),
			new AmbienceScene( "ambience-prison", "Prison at dusk: moths at the torches, mice, spiders on silk", 7, 0, DayNightCycle.Phase.DUSK ),
			new AmbienceScene( "ambience-caves", "Caves at dusk: newts, centipedes, beetles, glow-worms", 12, 0, DayNightCycle.Phase.DUSK ),
			new AmbienceScene( "ambience-city", "City at dusk: moths at the flames, silverfish, lizards", 17, 0, DayNightCycle.Phase.DUSK ),
			new AmbienceScene( "ambience-city-day", "City by day: swifts on the statues, butterflies", 17, 0, DayNightCycle.Phase.DAY ),
			new AmbienceScene( "ambience-halls", "Halls at dusk: salamanders, ember beetles, crows, embers", 22, 0, DayNightCycle.Phase.DUSK ),
			new AmbienceScene( "ambience-frozen", "Frozen branch at dusk: snow hares, frost moths, ice fish", 22, 2, DayNightCycle.Phase.DUSK ),
			new AmbienceScene( "ambience-nest", "Spider nest: spiderlings, flies stuck in the webbing", 7, 5, DayNightCycle.Phase.DUSK ),
			new AmbienceScene( "ambience-vault", "Dwarven vault: the city's life, kept quiet", 17, 1, DayNightCycle.Phase.DUSK ),
			new AmbienceScene( "ambience-temple", "Temple: a few moths, dust, far chimes", 14, 3, DayNightCycle.Phase.DUSK ),
			new AmbienceScene( "ambience-mines", "Kupua mines: centipedes, beetles, drips", 57, 0, DayNightCycle.Phase.DUSK ),
			new AmbienceScene( "ambience-meadow", "Postgame field by day: butterflies, finches, hares", 27, 0, DayNightCycle.Phase.DAY ),
			new AmbienceScene( "ambience-meadow-night", "Postgame field at night: fireflies, crickets", 27, 0, DayNightCycle.Phase.NIGHT ),
			new AmbienceScene( "ambience-shore", "Postgame shore: gulls, leaping fish, lapping water", 29, 0, DayNightCycle.Phase.DAY ),
			new AmbienceScene( "ambience-catacomb", "Postgame catacombs: roaches, moths, spiders, mice", 31, 0, DayNightCycle.Phase.DUSK ),
			new AcousticsScene( "acoustics-room", "Room acoustics: a prison room", 7, 0, 0, AcousticsScene.Spot.ROOM, false ),
			new AcousticsScene( "acoustics-corridor", "Room acoustics: the prison's longest corridor", 7, 0, 0, AcousticsScene.Spot.CORRIDOR, false ),
			new AcousticsScene( "acoustics-sewers", "Room acoustics: a sewer room, which must ring", 2, 0, 0, AcousticsScene.Spot.ROOM, false ),
			new AcousticsScene( "acoustics-hall", "Room acoustics: the biggest space in the demon halls", 22, 0, 0, AcousticsScene.Spot.LARGEST, false ),
			new AcousticsScene( "acoustics-cave", "Room acoustics: the biggest chamber in the caves", 12, 0, 0, AcousticsScene.Spot.LARGEST, false ),
			new AcousticsScene( "acoustics-cavern", "Room acoustics: a cavern of the world's caves at -9", 0, 0, -9, AcousticsScene.Spot.LARGEST, false ),
			new AcousticsScene( "acoustics-peaks", "Room acoustics: facing a rock face on the peaks at +3", 0, 0, 3, AcousticsScene.Spot.ECHO, false ),
			new AcousticsScene( "acoustics-field", "Room acoustics: the postgame field, which must stay dry", 27, 0, 0, AcousticsScene.Spot.HERE, false ),
			new AcousticsScene( "acoustics-church", "Room acoustics: the town's church", 1, TownInteriorLevel.BRANCH, 0, AcousticsScene.Spot.HERE, false ),
			new AcousticsScene( "acoustics-busy", "Room acoustics: a fight in the caves at -6 in a storm", 0, 0, -6, AcousticsScene.Spot.HERE, true ),
			new WeatherScene( "weather-rain-light", "Weather: light rain by a river",
					new Sky( PrecipType.RAIN, 0.08f, CALM, Float.NaN, false ), WeatherScene.Ground.RIVER, false,
					"light rain held at 0.08, the wind calm: a soft, dark patter left and right (the light rain bed),"
					+ " and the rain on the water from the water's side; more than six cells from the water, the rain"
					+ " in the leaves from the trees' side instead" ),
			new WeatherScene( "weather-rain", "Weather: steady rain by a river",
					new Sky( PrecipType.RAIN, 0.25f, CALM, Float.NaN, false ), WeatherScene.Ground.RIVER, false,
					"steady rain held at 0.25, the wind calm: the rain bed left and right, and the rain on the water"
					+ " (or in the leaves, away from it) from its side, staying put as you walk" ),
			new WeatherScene( "weather-rain-heavy", "Weather: heavy rain by a river, far thunder",
					new Sky( PrecipType.RAIN, 0.5f, CALM, Float.NaN, false ), WeatherScene.Ground.RIVER, false,
					"heavy rain held at 0.5, the wind calm: the heavy rain bed, denser and lower, the water or the"
					+ " leaves from their side, and a far roll of thunder every 25 to 70 seconds" ),
			new WeatherScene( "weather-storm", "Weather: a thunderstorm by a river",
					new Sky( PrecipType.RAIN, 0.6f, 15f, 90f, true ), WeatherScene.Ground.RIVER, false,
					"a storm forced (no need to wait for one), heavy rain at 0.6 in a 15 m/s wind: the heavy rain bed,"
					+ " the gale and its gusts from the wind's side, and thunder with each flash - a crack with the flash for a bolt in sight or"
					+ " within 8 cells, a far roll within half a second for one beyond out of sight or for sheet lightning, quieter the"
					+ " farther it struck. The bolts come down on your"
					+ " turns (about every third turn, within 10 cells: stand still and only sheet lightning flashes); one in sight is"
					+ " drawn, burns its cell to embers and sets the grass round it alight (never by you or anyone), and hurts and stuns"
					+ " whatever it hits, you too. Rest and the bolts come fast: each in sight drawn, the flash and thunder at most every 0.7 s" ),
			new WeatherScene( "weather-gale", "Weather: a gale over open plains",
					new Sky( null, 0f, 16f, 90f, false ), WeatherScene.Ground.PLAINS, false,
					"dry, the wind held at 16 m/s: the gale bed from the wind's side, and a gust from the same side"
					+ " as the rain would lean on screen, one in 4 seconds at most" ),
			new WeatherScene( "weather-sandstorm", "Weather: a sandstorm in the desert",
					new Sky( null, 0f, 18f, 270f, false ), WeatherScene.Ground.DESERT, false,
					"dry desert, the wind held at 18 m/s: the sandstorm in place of the gale, from the wind's side,"
					+ " and gusts with it; on screen, sand driven low toward the west, puffs of dust rolling after it"
					+ " and a tan haze, all surging in the gusts" ),
			new WeatherScene( "weather-snow", "Weather: snowfall on the tundra",
					new Sky( PrecipType.SNOW, 0.3f, CALM, Float.NaN, false ), WeatherScene.Ground.FROZEN, false,
					"snow held at 0.3, the wind calm: the snow bed left and right, a hush, very quiet by design" ),
			new WeatherScene( "weather-sleet", "Weather: sleet on open ground",
					new Sky( PrecipType.SLEET, 0.3f, CALM, Float.NaN, false ), WeatherScene.Ground.PLAINS, false,
					"sleet held at 0.3, the wind calm: the sleet bed left and right, wet and slushy" ),
			new WeatherScene( "weather-hail", "Weather: hail on open ground",
					new Sky( PrecipType.HAIL, 0.35f, CALM, Float.NaN, false ), WeatherScene.Ground.PLAINS, false,
					"hail held at 0.35 (it never falls naturally), the wind calm: the hail bed left and right, dull"
					+ " knocks and ticks" ),
			new WeatherScene( "weather-blizzard", "Weather: a blizzard on the tundra",
					new Sky( PrecipType.BLIZZARD, 0.35f, 15f, 270f, false ), WeatherScene.Ground.FROZEN, false,
					"a blizzard held at 0.35 in a 15 m/s wind: the blizzard bed and no gale (the blizzard is the wind),"
					+ " gusts from the wind's side; on screen snow driven toward the west over the snowfall, a white vignette"
					+ " closing in from the edges and pulsing with the gusts, and a brief whiteout as a strong gust comes"
					+ " (about every 17 s)" ),
			new WeatherScene( "weather-fog", "Weather: fog or the clearing after rain, by the trees",
					new Sky( null, 0f, CALM, Float.NaN, false ), WeatherScene.Ground.RIVER, true,
					"no rain, the wind calm: drips off the trees within six cells, every 2 to 6 seconds, each from its"
					+ " tree's side" ),
			new WeatherScene( "weather-indoors-rain", "Weather: rain on the roof of the inn",
					new Sky( PrecipType.RAIN, 0.3f, Float.NaN, Float.NaN, false ), WeatherScene.Ground.INDOORS, false,
					"rain held at 0.3 over the town, the hero in the inn: only the rain on the roof, low and muffled;"
					+ " no rain bed, no wind, no drips (the debug rain is drawn indoors too: the override reaches"
					+ " every level, where a real rain stops at the door)" ),
			new WeatherScene( "weather-clear", "Weather: release every weather override",
					null, WeatherScene.Ground.HERE, false, null ),
	} );

	//lists of scenes one after another
	private static Scene[] concat( Scene[]... parts ){
		ArrayList<Scene> all = new ArrayList<>();
		for (Scene[] part : parts) all.addAll( Arrays.asList( part ) );
		return all.toArray( new Scene[0] );
	}

	public static Scene byId( String id ){
		for (Scene s : SCENES) if (s.id().equals( id )) return s;
		return null;
	}

	/** Runs a scene on the live game: safe hero, no fog, then the scene's own setup. */
	public static void run( Scene scene ){
		Hero hero = Dungeon.hero;
		if (hero == null || Dungeon.level == null) return;
		boolean wasSafe = hero.debugInfiniteHealth && Dungeon.debugNoFog;
		hero.debugInfiniteHealth = true;
		hero.HP = hero.HT;
		Dungeon.debugNoFog = true;
		//a gallery page still playing stops first
		FxGallery.stopRunning();
		scene.apply( hero );
		Dungeon.observe();
		GameScene.updateFog();
		GLog.p( "Scene: " + scene.title() );
		//both stay on after the scene (the debug window's Hero tab turns them off); a scene that
		//needs the real thing turns them off itself
		if (!wasSafe && hero.debugInfiniteHealth && Dungeon.debugNoFog){
			GLog.i( "Debug: infinite health and no fog of war are on." );
		}
	}

	// ----------------------------------------------------- command line start

	//the scene the command line asked for: a new run is started for it (TitleScene)
	//and it is applied once the game scene stands (GameScene.create)
	private static Scene pending;
	private static boolean starting;

	public static void request( String id ){
		pending = byId( id );
		if (pending == null) System.out.println( "[scene] unknown scene '" + id + "', known: " + ids() );
	}

	public static String ids(){
		StringBuilder sb = new StringBuilder();
		for (Scene s : SCENES) sb.append( sb.length() == 0 ? "" : ", " ).append( s.id() );
		return sb.toString();
	}

	/** TitleScene: a requested scene starts a fresh run at once instead of showing the menu. */
	public static boolean startRequested(){
		if (pending == null || starting) return false;
		starting = true;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		GamesInProgress.curSlot = 1;
		Dungeon.hero = null;
		Dungeon.daily = Dungeon.dailyReplay = false;
		Dungeon.initSeed();
		InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		Game.switchScene( InterlevelScene.class );
		return true;
	}

	/** GameScene, once it stands: the requested scene is applied and forgotten. */
	public static void applyRequested(){
		if (pending == null || !starting) return;
		Scene s = pending;
		pending = null;
		run( s );
	}

	// -------------------------------------------------------------- helpers

	//open cells around the hero, nearest first, none adjacent to him (room to breathe)
	static ArrayList<Integer> cellsAround( Hero hero, int count ){
		Level level = Dungeon.level;
		ArrayList<Integer> out = new ArrayList<>();
		int w = level.width(), hx = hero.pos % w, hy = hero.pos / w;
		for (int r = 2; r < 12 && out.size() < count; r++){
			for (int dy = -r; dy <= r && out.size() < count; dy++){
				for (int dx = -r; dx <= r && out.size() < count; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = hx + dx, y = hy + dy;
					if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) continue;
					int cell = x + y * w;
					if (!level.passable[cell] || level.pit[cell] || Actor.findChar( cell ) != null) continue;
					out.add( cell );
				}
			}
		}
		return out;
	}

	//every kind of monster in the bestiary that flies
	static ArrayList<Class<? extends Mob>> fliers(){
		ArrayList<Class<? extends Mob>> out = new ArrayList<>();
		HashSet<Class<?>> seen = new HashSet<>();
		for (Bestiary b : Bestiary.values()){
			for (Class<?> cls : b.entities()){
				if (!Mob.class.isAssignableFrom( cls ) || !seen.add( cls )) continue;
				Mob sample = (Mob) Reflection.newInstance( cls );
				if (sample != null && sample.flying){
					@SuppressWarnings("unchecked") Class<? extends Mob> mc = (Class<? extends Mob>) cls;
					out.add( mc );
				}
			}
		}
		return out;
	}

	//moves the world's clock FORWARD (never back) to `into` of the given phase - of the given season
	//first when one is named - and clears the debug menu's hour override, which would pin phase() and
	//freeze every schedule. at least five turns past the boundary, so it is unmistakable. false on a
	//real-clock run, whose clock is the player's own
	static boolean walkClock( GameCalendar.Season season, DayNightCycle.Phase phase, float into ){
		if (Dungeon.isChallenged( Challenges.REAL_CLOCK )) return false;
		DayNightCycle.debugPhaseOverride = null;
		int moved = toSeason( season );
		//then on to the hour: the phase lengths are the season's, so measured after the jump
		int start = 0;
		for (DayNightCycle.Phase p = DayNightCycle.Phase.DAWN; p != phase; p = p.next()) start += DayNightCycle.phaseDuration( p );
		int want = start + Math.max( 5, Math.round( into * DayNightCycle.phaseDuration( phase ) ) );
		int step = Math.floorMod( want - Math.floorMod( Dungeon.cycleTurn, DayNightCycle.FULL_CYCLE ), DayNightCycle.FULL_CYCLE );
		Dungeon.cycleTurn += step;
		moved += step;
		//today's hour had gone by on the season's last day, so the step crossed into the next
		//season: on by whole days to the season's return. a whole day keeps the hour, and the
		//same season keeps the same phase lengths
		moved += toSeason( season );
		if (moved > 0){
			Statistics.duration += moved;
			ClimateManager.onHeroTurn();
		}
		return true;
	}

	//whole days forward until the calendar shows the season (a year and a bit at most); the turns walked
	private static int toSeason( GameCalendar.Season season ){
		int moved = 0;
		for (int d = 0; season != null && d < 400 && GameCalendar.season() != season; d++){
			Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
			moved += DayNightCycle.FULL_CYCLE;
		}
		return moved;
	}

	/** Sends the hero to a world slice, landing on (wx, wy): arriveAt plus travelToAltitude. */
	public static void toSlice( int altitude, int wx, int wy ){
		OverworldLevel.arriveAt( wx, wy );
		OverworldLevel.travelToAltitude( altitude );
	}

	//a search of the world can run for many seconds (a hermit's hut, a thawed meadow): it runs off
	//the render thread so the game keeps drawing - on it, a long one froze the screen and tripped
	//the freeze watchdog - and its answer (null: nothing near) is handed back to the render thread
	static <T> void search( String what, java.util.function.Supplier<T> find, java.util.function.Consumer<T> then ){
		GLog.i( "Scene: looking for " + what + "..." );
		Thread t = new Thread( () -> {
			T found = find.get();
			Game.runOnRenderThread( () -> then.accept( found ) );
		}, "scene-search" );
		t.setDaemon( true );
		t.start();
	}

	static Mob spawn( Class<? extends Mob> cls, int cell ){
		Mob mob = Reflection.newInstance( cls );
		if (mob == null) return null;
		mob.pos = cell;
		mob.state = mob.WANDERING;
		GameScene.add( mob );
		return mob;
	}

	// --------------------------------------------------------------- scenes

	//a bare walled room around the hero: 15 by 11 of plain floor and nothing else in it.
	//The world's slices are painted over their terrain (town art, dress layers), so from
	//there the hero is first sent to the first sewer floor and the room carved on arrival
	static final class Room implements Scene {
		static final int HALF_W = 7, HALF_H = 5;
		@Override public String id(){ return "blank-room"; }
		@Override public String title(){ return "Blank room around the hero"; }
		@Override public void apply( Hero hero ){
			ensure( this, hero );
		}

		/** Carves the room for a scene; false when the hero had to be sent to the sewers
		 *  first, in which case the scene is applied again on arrival. */
		static boolean ensure( Scene scene, Hero hero ){
			Level level = Dungeon.level;
			if (level instanceof OverworldLevel){
				pending = scene;
				starting = true;
				InterlevelScene.mode = InterlevelScene.Mode.RETURN;
				InterlevelScene.returnDepth = 1;
				InterlevelScene.returnBranch = 0;
				InterlevelScene.returnPos = -1;
				Game.switchScene( InterlevelScene.class );
				return false;
			}
			int w = level.width(), hx = hero.pos % w, hy = hero.pos / w;
			for (int dy = -HALF_H - 1; dy <= HALF_H + 1; dy++){
				for (int dx = -HALF_W - 1; dx <= HALF_W + 1; dx++){
					int x = hx + dx, y = hy + dy;
					if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) continue;
					int cell = x + y * w;
					boolean wall = Math.abs( dx ) > HALF_W || Math.abs( dy ) > HALF_H;
					Level.set( cell, wall ? Terrain.WALL : Terrain.EMPTY );
					level.plants.remove( cell );
					level.traps.remove( cell );
					if (level.heaps.get( cell ) != null) level.heaps.get( cell ).destroy();
					if (!wall && level.blobs != null){
						for (xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob b : level.blobs.values()) b.clear( cell );
					}
					for (Mob m : level.mobs.toArray( new Mob[0] )){
						if (m.pos == cell){
							if (m.sprite != null) m.sprite.killAndErase();
							m.destroy();
						}
					}
					level.visited[cell] = true;
				}
			}
			GameScene.updateMap();
			level.cleanWalls();
			return true;
		}
	}
	//every flying monster around the hero in the blank room, held by the given buff for two turns (or free)
	private static final class Fliers implements Scene {
		final String id, title;
		final Class<? extends FlavourBuff> hold;
		Fliers( String id, String title, Class<? extends FlavourBuff> hold ){
			this.id = id; this.title = title; this.hold = hold;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			if (!Room.ensure( this, hero )) return;
			ArrayList<Class<? extends Mob>> kinds = fliers();
			ArrayList<Integer> cells = cellsAround( hero, kinds.size() );
			for (int i = 0; i < kinds.size() && i < cells.size(); i++){
				Mob mob = spawn( kinds.get( i ), cells.get( i ) );
				if (mob != null && hold != null) Buff.prolong( mob, hold, 2f );
			}
			if (cells.size() < kinds.size()){
				GLog.w( "Scene: only room for " + cells.size() + " of " + kinds.size() + " fliers here" );
			}
		}
	}

	//the nearest place on the surface where rocky foothills run into a snowfield:
	//boulders, standing rocks, the snow line and frozen ponds side by side, for
	//the ground transitions and the rocks' footing
	private static final class Edges implements Scene {
		//set while the hero is on his way there, so the arrival does not send him off again
		boolean travelling;
		@Override public String id(){ return "overworld-edges"; }
		@Override public String title(){ return "Surface: rocks and a snow line"; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				return;
			}
			int[] spot = xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator.findRockyEdge(
					OverworldLevel.worldSeedOf( Dungeon.seed ) );
			if (spot == null){
				GLog.w( "Scene: no rocky snow edge near the origin in this world" );
				return;
			}
			travelling = true;
			pending = this;
			starting = true;
			OverworldLevel.arriveAt( spot[0], spot[1] );
			OverworldLevel.travelToSurface();
		}
	}

	//the surface's small life all at once (OverworldCritters): the nearest place to the
	//origin where a meadow, a river and a wood meet, the clock walked on to a summer's day
	//(or night), and a full set put down around the hero on arrival - flocks to scatter,
	//hares to bolt, butterflies, fish leaping; after dark an owl in an oak and fireflies
	private static final class Critters implements Scene {
		final boolean night;
		//set while the hero is on his way there, so the arrival sets the scene instead of sending him off again
		boolean travelling;
		Critters( boolean night ){
			this.night = night;
		}
		@Override public String id(){ return night ? "overworld-critters-night" : "overworld-critters"; }
		@Override public String title(){
			return night ? "Surface at night: an owl, fireflies, fish" : "Surface: birds, fish, hares, butterflies";
		}
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				GLog.i( "Scene: " + OverworldCritters.showcase( night ) );
				if (OverworldCritters.sky() != OverworldCritters.Sky.CLEAR){
					GLog.w( "Scene: the weather keeps new critters in; clear it in the debug menu to see more come" );
				}
				return;
			}
			if (!walkClock( GameCalendar.Season.SUMMER, night ? DayNightCycle.Phase.NIGHT : DayNightCycle.Phase.DAY, 0.3f )) GLog.w( "Scene: a real-clock run keeps its own season and hour" );
			int[] spot = OverworldCritters.findCritterGround(
					OverworldLevel.worldSeedOf( Dungeon.seed ), WorldModel.calendarShift() );
			if (spot == null){
				GLog.w( "Scene: no meadow by a river and a wood near the origin in this world" );
				return;
			}
			travelling = true;
			pending = this;
			starting = true;
			OverworldLevel.arriveAt( spot[0], spot[1] );
			OverworldLevel.travelToSurface();
		}
	}

	//a settlement near the origin, its clock walked on (moved, not overridden, so the day plays
	//on from there: the folk keep their round, VillageRoutine) and the rain held off. the hero
	//stands on its axis south of the well, where the house fronts face him (front), or on the
	//well itself. arrival turns the fog back on, so what the developer sees is what players see
	private static final class VillageScene implements Scene {
		final String id, title;
		final GameCalendar.Season season;   //null: the season it is
		final DayNightCycle.Phase phase;
		final float into;
		final WorldStructures.Faction faction;
		final int minHouses, sectors;
		final boolean wantShore, front;
		//set while the hero is on his way there, so the arrival does not send him off again
		boolean travelling;
		int[] found;           //{sx, sy, cx, cy, houses}
		boolean shore;         //the one found has water for its fishers
		VillageScene( String id, String title, GameCalendar.Season season, DayNightCycle.Phase phase, float into,
				WorldStructures.Faction faction, int minHouses, boolean wantShore, boolean front, int sectors ){
			this.id = id;
			this.title = title;
			this.season = season;
			this.phase = phase;
			this.into = into;
			this.faction = faction;
			this.minHouses = minHouses;
			this.wantShore = wantShore;
			this.front = front;
			this.sectors = sectors;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			if (travelling){
				travelling = false;
				Dungeon.debugNoFog = false;
				GLog.i( "Scene: " + WorldStructures.villageName( seed, found[0], found[1] ) + ", " + found[4] + " houses, "
						+ WorldStructures.populatedHouses( found[4] ) + " lived in - "
						+ GameCalendar.season().name().toLowerCase( Locale.ENGLISH ) + ", "
						+ DayNightCycle.phase().name().toLowerCase( Locale.ENGLISH ) + " (fog on: what players see)" );
				report();
				return;
			}
			String kind = faction.name().toLowerCase( Locale.ENGLISH );
			ArrayList<int[]> near = WorldStructures.settlementsNear( seed, 0, 0, faction, minHouses, sectors );
			if (near.isEmpty()){
				GLog.w( "Scene: no " + kind + " settlement of " + minHouses + " houses or more within "
						+ sectors + " sectors of the origin in this world" );
				return;
			}
			found = near.get( 0 );
			shore = false;
			if (wantShore){
				for (int i = 0; i < near.size() && i < 12; i++){
					if (VillageRoutine.shoreNear( seed, near.get( i )[0], near.get( i )[1] )){
						found = near.get( i );
						shore = true;
						break;
					}
				}
			}
			if (!walkClock( season, phase, into )) GLog.w( "Scene: a real-clock run keeps its own hour - the clock was not moved" );
			ClimateManager.debugPrecipOverride = 0f;
			ClimateManager.debugForceStorm = false;
			int[] s = front ? SettlementLights.standCell( seed, found[0], found[1] ) : new int[]{ found[2], found[3] };
			travelling = true;
			pending = this;
			starting = true;
			OverworldLevel.arriveAt( s[0], s[1] );
			OverworldLevel.travelToSurface();
		}

		//who of the place is about, by trade, and what the scene did to the clock and the sky
		private void report(){
			long key = WorldStructures.sectorOf( found[0], found[1] );
			int[] trades = new int[VillageRoutine.Role.values().length];
			int unknown = 0, asleep = 0, folk = 0;
			for (Mob m : Dungeon.level.mobs){
				if (!(m instanceof Settler) || ((Settler) m).settlementKey() != key) continue;
				Settler s = (Settler) m;
				folk++;
				if (s.role < 0 || s.role >= trades.length) unknown++;
				else trades[s.role]++;
				if (s.state == s.SLEEPING) asleep++;
			}
			StringBuilder sb = new StringBuilder();
			for (VillageRoutine.Role r : VillageRoutine.Role.values()){
				if (trades[r.ordinal()] == 0) continue;
				sb.append( sb.length() == 0 ? "" : ", " ).append( trades[r.ordinal()] ).append( ' ' )
						.append( r.name().toLowerCase( Locale.ENGLISH ) );
			}
			if (unknown > 0) sb.append( sb.length() == 0 ? "" : ", " ).append( unknown ).append( " unassigned" );
			GLog.i( "Folk: " + folk + " in the window (" + (sb.length() == 0 ? "none" : sb) + "), " + asleep + " asleep" );
			GLog.i( "Clock walked to " + phase.name().toLowerCase( Locale.ENGLISH ) + " " + Math.round( into * 100 )
					+ "%: the day plays on from here" );
			GLog.i( "Precipitation held at zero: release it in the debug menu's weather controls" );
			GLog.i( "Folk more than " + VillageRoutine.NEAR + " cells off step straight to their spots; the fog hides it" );
			if (wantShore && !shore) GLog.w( "No water near this one: its fishers work the fields" );
			if (GameCalendar.season() == GameCalendar.Season.WINTER){
				GLog.i( "Winter: the fields lie fallow, so the farmers sweep their doorsteps" );
			}
		}
	}

	//a morning on a road near the origin that a big town's watch walks (RoadTraffic): the clock
	//walked on (moved, not overridden), the hero set down out along the watch's beat, and on
	//arrival a traveller of each kind about him, a caravan's cart coming up behind him to pitch
	//its stall just ahead, the watch walking out behind him and two outlaws waiting on the road
	//ahead
	private static final class Road implements Scene {
		//set while the hero is on his way there, so the arrival sets the scene instead of sending him off again
		boolean travelling;
		int[] road;   //{sx, sy, nx, ny, tx, ty}: the road, and the town whose watch walks it
		@Override public String id(){ return "overworld-road"; }
		@Override public String title(){ return "Surface: a road with travellers and a patrol"; }
		@Override public void apply( Hero hero ){
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			if (travelling){
				travelling = false;
				//the fog back on: walkers come and go only out of a player's sight
				Dungeon.debugNoFog = false;
				if (!(Dungeon.level instanceof OverworldLevel)) return;
				int[] n = ((OverworldLevel) Dungeon.level).debugStageRoad( road[0], road[1], road[2], road[3], road[4], road[5] );
				//the watch's town is one end of the road; it walks to the other
				boolean first = road[0] == road[4] && road[1] == road[5];
				if (n[1] > 0) GLog.i( "Scene: a caravan's cart comes up the road behind you and pitches its stall about 8 cells ahead" );
				else GLog.w( "Scene: no room on the road for the caravan's cart" );
				GLog.i( "Scene: " + n[0] + " travellers on the road; the watch of " + WorldStructures.villageName( seed, road[4], road[5] )
						+ " walks out behind you on its way to " + WorldStructures.villageName( seed, first ? road[2] : road[0], first ? road[3] : road[1] )
						+ ", and two outlaws wait on the road ahead. Talk to the travellers for news."
						+ " (fog on: what players see)" );
				return;
			}
			if (!walkClock( null, DayNightCycle.Phase.DAY, 0.05f )) GLog.w( "Scene: a real-clock run keeps its own hour - the clock was not moved" );
			road = watchedRoad( seed );
			if (road == null){
				GLog.w( "Scene: no road with a town watch near the origin in this world" );
				return;
			}
			float[] beat = RoadTraffic.beatRoute( seed, road[4], road[5] );
			float[] spot = RoadTraffic.point( beat, RoadTraffic.length( beat ) * 0.6f );
			travelling = true;
			pending = this;
			starting = true;
			OverworldLevel.arriveAt( Math.round( spot[0] ), Math.round( spot[1] ) );
			OverworldLevel.travelToSurface();
		}
	}

	//the road nearest the origin that a town watch walks: {sx, sy, nx, ny, tx, ty} - the road, and
	//its watch's town
	static int[] watchedRoad( long seed ){
		int[] best = null;
		long bestD = Long.MAX_VALUE;
		for (int[] r : RoadTraffic.roads( seed, -6, -6, 6, 6 )){
			if (!RoadTraffic.travelled( seed, r[0], r[1], r[2], r[3] )) continue;
			for (int end = 0; end < 2; end++){
				int tx = r[end * 2], ty = r[end * 2 + 1], ox = r[2 - end * 2], oy = r[3 - end * 2];
				//the town's beat must run along THIS road: its own road neighbour is the other end
				if (!RoadTraffic.patrolTown( seed, tx, ty )
						|| WorldStructures.roadNeighbour( seed, tx, ty ) != WorldStructures.sectorOf( ox, oy )) continue;
				long wx = WorldStructures.siteX( seed, tx, ty ), wy = WorldStructures.siteY( seed, tx, ty );
				if (wx * wx + wy * wy < bestD){
					bestD = wx * wx + wy * wy;
					best = new int[]{ r[0], r[1], r[2], r[3], tx, ty };
				}
			}
		}
		return best;
	}

	//today's caravan nearest the origin, marked for an ambush on arrival (CaravanAmbush), the
	//clock walked on a quarter into the day (every cart in from its morning leg, its stall up)
	//and the hero set down sixteen cells back down the road from it: two steps on and it springs
	private static final class Ambush implements Scene {
		//set while the hero is on his way there, so the arrival sets the scene instead of sending him off again
		boolean travelling;
		@Override public String id(){ return "overworld-ambush"; }
		@Override public String title(){ return "Surface: a caravan under attack"; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				//the fog back on: the ambush waits for the stall to be in sight, and its outlaws
				//break from cover the hero cannot see into
				Dungeon.debugNoFog = false;
				if (!(Dungeon.level instanceof OverworldLevel)) return;
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer c = ((OverworldLevel) Dungeon.level).debugAmbushNearest();
				if (c == null) GLog.w( "Scene: no caravan stall stands near the hero" );
				else GLog.i( "Scene: the stall ahead is marked for an ambush - walk up to within "
						+ CaravanAmbush.SIGHT + " cells of it, in sight. (fog on: what players see)" );
				return;
			}
			//a quarter into the day: every cart has made its morning leg and its stall is up
			if (!walkClock( null, DayNightCycle.Phase.DAY, 0.25f )) GLog.w( "Scene: a real-clock run keeps its own hour - the clock was not moved" );
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			int weekday = GameCalendar.weekday().ordinal(), dos = GameCalendar.dayOfSeason();
			int[] spot = null, road = null;
			long bestD = Long.MAX_VALUE;
			for (int[] r : RoadTraffic.roads( seed, -6, -6, 6, 6 )){
				int[] s = RoadTraffic.caravanSpot( seed, r[0], r[1], r[2], r[3], weekday, dos );
				if (s == null || !RoadTraffic.roadNear( seed, s[0], s[1] )) continue;
				long d = (long) s[0] * s[0] + (long) s[1] * s[1];
				if (d < bestD){
					bestD = d;
					spot = s;
					road = r;
				}
			}
			if (spot == null){
				GLog.w( "Scene: no caravan on the roads near the origin today" );
				return;
			}
			//back down the road toward the village it starts from
			float vx = WorldStructures.siteX( seed, road[0], road[1] ) - spot[0];
			float vy = WorldStructures.siteY( seed, road[0], road[1] ) - spot[1];
			float len = Math.max( 1f, (float) Math.sqrt( vx * vx + vy * vy ) );
			travelling = true;
			pending = this;
			starting = true;
			OverworldLevel.arriveAt( Math.round( spot[0] + vx / len * 16 ), Math.round( spot[1] + vy / len * 16 ) );
			OverworldLevel.travelToSurface();
		}
	}

	//a star brought down fifteen cells east of the hero, at night (WorldEvents): the nearest ground
	//one could fall on by the real rules, the hero set down west of it and the clock walked on to
	//the night; on arrival the star is forced through the world's own registry and the hero's
	//step pass run at once, so it screams down in front of him onto its crater
	private static final class StarFall implements Scene {
		//set while the hero is on his way there, so the arrival brings the star down instead of sending him off again
		boolean travelling;
		int spotX, spotY;
		@Override public String id(){ return "overworld-star"; }
		@Override public String title(){ return "Surface: a star falls nearby"; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel)) return;
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				ow.forceEvent( WorldEvents.forcedStar( spotX, spotY, ow.eventTurn() ) );
				ow.pollEvents();
				GLog.i( "Scene: a star came down about 15 cells east - its crater smokes for a day, the fragment lies in it,"
						+ " and the world map pins it." );
				return;
			}
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			int nx = 0, ny = 0;
			if (Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0){
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				nx = ow.worldX() + hero.pos % ow.width();
				ny = ow.worldY() + hero.pos / ow.width();
			}
			int[] spot = WorldEvents.findStarSpot( seed, nx, ny );
			if (spot == null){
				GLog.w( "Scene: no open ground for a star near here" );
				return;
			}
			if (!walkClock( null, DayNightCycle.Phase.NIGHT, 0.1f )) GLog.w( "Scene: a real-clock run keeps its own hour - the clock was not moved" );
			spotX = spot[0];
			spotY = spot[1];
			travelling = true;
			pending = this;
			starting = true;
			OverworldLevel.arriveAt( spotX - 15, spotY );
			OverworldLevel.travelToSurface();
		}
	}

	//a travelling market on the nearest human village (WorldEvents): the hero set down by its well
	//by day, with gold to spend; on arrival the market is forced through the world's own registry
	//and the hero's step pass run at once, so its stalls go up round the well in front of him
	private static final class MarketDay implements Scene {
		//set while the hero is on his way there, so the arrival opens the market instead of sending him off again
		boolean travelling;
		int vsx, vsy;
		@Override public String id(){ return "overworld-market"; }
		@Override public String title(){ return "Surface: the travelling market"; }
		@Override public void apply( Hero hero ){
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel)) return;
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				Dungeon.gold = Math.max( Dungeon.gold, 20000 );
				ow.forceEvent( WorldEvents.forcedMarket( seed, vsx, vsy, ow.eventTurn() ) );
				ow.pollEvents();
				GLog.i( "Scene: the market stands round the well of " + WorldStructures.villageName( seed, vsx, vsy )
						+ " for three days; 20000 gold to spend." );
				return;
			}
			int nx = 0, ny = 0;
			if (Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0){
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				nx = ow.worldX() + hero.pos % ow.width();
				ny = ow.worldY() + hero.pos / ow.width();
			}
			ArrayList<int[]> near = WorldStructures.settlementsNear( seed, nx, ny, WorldStructures.Faction.HUMAN, 1, 8 );
			if (near.isEmpty()){
				GLog.w( "Scene: no human village within eight sectors" );
				return;
			}
			//the shops shut at night: on to the morning if it is dark
			if (DayNightCycle.isNight() && !walkClock( null, DayNightCycle.Phase.DAY, 0.1f )){
				GLog.w( "Scene: a real-clock run keeps its own hour - the clock was not moved" );
			}
			vsx = near.get( 0 )[0];
			vsy = near.get( 0 )[1];
			travelling = true;
			pending = this;
			starting = true;
			OverworldLevel.arriveAt( near.get( 0 )[2], near.get( 0 )[3] + 3 );
			OverworldLevel.travelToSurface();
		}
	}

	//bandits raiding the nearest human village (RaidEvent): the clock walked on to the morning and
	//the hero set down just past its fence, on the side he came from; on arrival the raid is forced
	//through the world's own registry and the hero's step pass run at once, so the smoke goes up and
	//the band comes out of the houses ahead of him - out of the doors he cannot see, the fog back on
	private static final class Raid implements Scene {
		//set while the hero is on his way there, so the arrival brings the raid instead of sending him off again
		boolean travelling;
		int vsx, vsy;
		@Override public String id(){ return "overworld-raid"; }
		@Override public String title(){ return "Surface: bandits raid a village"; }
		@Override public void apply( Hero hero ){
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel)) return;
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				Dungeon.debugNoFog = false;
				Dungeon.observe();
				ow.forceEvent( WorldEvents.forcedRaid( seed, vsx, vsy, ow.eventTurn() ) );
				ow.pollEvents();
				GLog.i( "Scene: bandits are raiding " + WorldStructures.villageName( seed, vsx, vsy )
						+ " - walk in and drive them all out (fog on: what players see)." );
				return;
			}
			int wx = 0, wy = 0;
			if (Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0){
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				wx = ow.worldX() + hero.pos % ow.width();
				wy = ow.worldY() + hero.pos / ow.width();
			}
			ArrayList<int[]> near = WorldStructures.settlementsNear( seed, wx, wy, WorldStructures.Faction.HUMAN, 1, 12 );
			if (near.isEmpty()){
				GLog.w( "Scene: no human village within twelve sectors" );
				return;
			}
			if (!walkClock( null, DayNightCycle.Phase.DAY, 0.2f )) GLog.w( "Scene: a real-clock run keeps its own hour - the clock was not moved" );
			vsx = near.get( 0 )[0];
			vsy = near.get( 0 )[1];
			int[] edge = RaidEvent.edgeToward( seed, vsx, vsy, wx, wy );
			travelling = true;
			pending = this;
			starting = true;
			OverworldLevel.arriveAt( edge[0], edge[1] );
			OverworldLevel.travelToSurface();
		}
	}

	//a wolf pack on a deer's trail ten cells from the hero at dusk (HuntEvent): the clock walked on
	//to the dusk, a ground found ten cells off and the hunt forced through the world's own registry,
	//the hero's step pass run at once so it starts in front of him
	private static final class Hunt implements Scene {
		//set while the hero is on his way to the surface, so the arrival starts the hunt instead of sending him off again
		boolean travelling;
		@Override public String id(){ return "overworld-hunt"; }
		@Override public String title(){ return "Surface: wolves hunt deer at dusk"; }
		@Override public void apply( Hero hero ){
			boolean surface = Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0;
			if (!surface && !travelling){
				travelling = true;
				pending = this;
				starting = true;
				OverworldLevel.travelToSurface();
				return;
			}
			travelling = false;
			if (!surface){
				GLog.w( "Scene: could not reach the surface" );
				return;
			}
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			if (!walkClock( null, DayNightCycle.Phase.DUSK, 0.3f )){
				GLog.w( "Scene: a real-clock run keeps its own hour - a hunt starts only at dusk or after dark" );
			}
			int[] ground = HuntEvent.debugGround( ow, hero.pos, 10 );
			if (ground == null){
				GLog.w( "Scene: no open ground ten cells out for a hunt here" );
				return;
			}
			ow.forceEvent( WorldEvents.forcedHunt( ground[0], ground[1], ow.eventTurn(), WorldClock.nightStart( WorldClock.night() + 1 ) ) );
			ow.pollEvents();
			GLog.i( "Scene: a pack is on a deer's trail just ahead - watch, or step in." );
		}
	}

	//the ore of the world's rock (levels/overworld/Ores): the nearest place to the origin on a slice
	//where veins of every wanted metal glint within sight of the hero (and, in the caves, a crystal
	//seam stands by), a pickaxe in his pack; on the peaks the clock walked on to a summer's day and
	//the hero warmed so the cold does not freeze him solid. arrival puts the fog back on
	private static final class OreScene implements Scene {
		final String id, title;
		final int altitude;
		final Ores.Kind[] want;
		//set while the hero is on his way there, so the arrival sets the scene instead of sending him off again
		boolean travelling;
		OreScene( String id, String title, int altitude, Ores.Kind... want ){
			this.id = id; this.title = title; this.altitude = altitude; this.want = want;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != altitude){
					GLog.w( "Scene: could not reach slice " + altitude );
					return;
				}
				Dungeon.debugNoFog = false;
				if (hero.belongings.getItem( Pickaxe.class ) == null) new Pickaxe().collect( hero.belongings.backpack );
				if (altitude > 0) Buff.prolong( hero, WarmthBuff.class, 2000f );
				GLog.i( "Scene: " + report( hero ) + " (fog on: what players see)" );
				return;
			}
			if (altitude > 0 && !walkClock( GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.4f )){
				GLog.w( "Scene: a real-clock run keeps its own season and hour" );
			}
			int[] spot = oreShowcase( OverworldLevel.worldSeedOf( Dungeon.seed ), altitude, want );
			if (spot == null){
				GLog.w( "Scene: slice " + altitude + " holds no ore" );
				return;
			}
			travelling = true;
			pending = this;
			starting = true;
			toSlice( altitude, spot[0], spot[1] );
		}
	}

	/**
	 * Debug and tooling: the world cell nearest the origin, on rings 64 cells apart out to 6400,
	 * where a hero standing on open ground of the slice has a rock face of every wanted metal within
	 * eight cells (and, in the caves, two crystals at least). Approximate on purpose - it reads the
	 * natural rock, not the window (no ways, no flooding) - and bounded: about two seconds at worst.
	 * Where no place shows them all, the one that shows the rarest of them (then the most) - so a
	 * scene always has somewhere to go; null only on a slice with no ore at all.
	 */
	public static int[] oreShowcase( long seed, int altitude, Ores.Kind... want ){
		if (Ores.density( altitude ) == 0) return null;
		final int R = 8, S = 2 * R + 1;
		byte[] veins = new byte[S * S];
		boolean[] rock = new boolean[S * (S + 1)];
		int[] best = null;
		int bestScore = 0;
		for (int r = 0; r <= 6400; r += 64){
			for (int i = -r; i <= r; i += 64){
				for (int side = 0; side < 4; side++){
					if (r == 0 && side > 0) break;
					int cx = side < 2 ? i : (side == 2 ? -r : r);
					int cy = side < 2 ? (side == 0 ? -r : r) : i;
					if ((side >= 2) && (i == -r || i == r)) continue;   //the corners are on the rows already
					//the hero's own cell must be open ground of the slice
					if (Ores.naturalRock( seed, altitude, cx, cy )) continue;
					if (altitude < 0 ? WorldModel.caveWet( seed, cx, cy, altitude )
							: WorldLayers.band( WorldModel.elevation( seed, cx, cy ) ) < altitude) continue;
					for (int y = 0; y <= S; y++){
						for (int x = 0; x < S; x++){
							rock[x + y * S] = Ores.naturalRock( seed, altitude, cx - R + x, cy - R + y );
						}
					}
					java.util.Arrays.fill( veins, (byte) 0 );
					Ores.veins( seed, altitude, cx - R, cy - R, S, S, null, veins );
					boolean[] shows = new boolean[Ores.Kind.values().length];
					int crystals = 0;
					WorldModel.CaveSample cs = new WorldModel.CaveSample();
					for (int y = 0; y < S; y++){
						for (int x = 0; x < S; x++){
							int c = x + y * S;
							if (!rock[c]) continue;
							if (!rock[c + S] && veins[c] != 0) shows[veins[c] - 1] = true;
							if (altitude < 0 && x > 0 && x < S - 1 && y > 0
									&& (!rock[c - 1] || !rock[c + 1] || !rock[c - S] || !rock[c + S])
									&& WorldModel.caveTerrain( seed, cx - R + x, cy - R + y, altitude,
										WorldModel.caveSample( seed, cx - R + x, cy - R + y, altitude, cs ) ) == Terrain.MINE_CRYSTAL){
								crystals++;
							}
						}
					}
					boolean all = altitude > 0 || crystals >= 2;
					//a rarer metal outweighs every commoner one together
					int score = 0;
					for (Ores.Kind k : want){
						all &= shows[k.ordinal()];
						if (shows[k.ordinal()]) score += 1 << k.ordinal();
					}
					if (all) return new int[]{ cx, cy };
					if (score > bestScore){
						bestScore = score;
						best = new int[]{ cx, cy };
					}
				}
			}
		}
		return best;
	}

	//the dangers of the slices (levels/overworld/HazardWatch), each where the finders of
	//LayerHazards put it nearest the world origin. arrival puts the fog back and turns the hero's
	//debug health off, so what the hazard does to him shows
	private static final class HazardScene implements Scene {
		static final int FIREDAMP = 0, ROCKFALL = 1, THIN_ICE = 2, THIN_AIR = 3;
		//a mild winter's day: the tarns still frozen, all their ice thin (LayerHazards.WARM_C)
		static final float MILD = -1f;
		final String id, title;
		final int kind;
		int altitude;
		//set while the hero is on his way there, so the arrival sets the scene instead of sending him off again
		boolean travelling;
		HazardScene( String id, String title, int kind ){
			this.id = id; this.title = title; this.kind = kind;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != altitude){
					GLog.w( "Scene: could not reach slice " + altitude );
					return;
				}
				Dungeon.debugNoFog = false;
				hero.debugInfiniteHealth = false;
				arrive( hero, (OverworldLevel) Dungeon.level );
				return;
			}
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			int[] at = null;
			switch (kind){
				case FIREDAMP:
					altitude = -6;
					at = LayerHazards.findFiredamp( seed, altitude, 80 );
					break;
				case ROCKFALL:
					altitude = -8;
					at = LayerHazards.findLooseWall( seed, altitude, 400 );
					break;
				case THIN_ICE:
					altitude = 2;
					if (!walkClock( GameCalendar.Season.WINTER, DayNightCycle.Phase.DAY, 0.5f )){
						GLog.w( "Scene: a real-clock run keeps its own season and hour" );
					}
					ClimateManager.debugTempOverride = MILD;
					at = LayerHazards.findThinIce( seed, altitude, WorldModel.calendarShift(), MILD, 30, 40 );
					break;
				case THIN_AIR:
					if (!walkClock( GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.4f )){
						GLog.w( "Scene: a real-clock run keeps its own season and hour" );
					}
					ClimateManager.debugWindOverride = 16f;
					for (int alt : new int[]{ 9, 10, 8, 7 }){
						at = LayerHazards.findRidge( seed, alt, WorldModel.calendarShift(), 30, 12 );
						altitude = alt;
						if (at != null) break;
					}
					break;
			}
			if (at == null){
				GLog.w( "Scene: nothing of the kind near the origin on slice " + altitude );
				return;
			}
			travelling = true;
			pending = this;
			starting = true;
			toSlice( altitude, at[0], at[1] );
		}

		private void arrive( Hero hero, OverworldLevel ow ){
			switch (kind){
				case FIREDAMP: {
					WandOfFireblast wand = new WandOfFireblast();
					wand.identify();
					wand.curCharges = wand.maxCharges;
					wand.collect( hero.belongings.backpack );
					new Torch().quantity( 3 ).collect( hero.belongings.backpack );
					GLog.i( "Scene: the foul air starts a step from you (debug health off). Walk in to smell it, light a"
							+ " torch inside to be warned and blown up, or blast it with the wand from outside." );
					break;
				}
				case ROCKFALL:
					if (hero.belongings.getItem( Pickaxe.class ) == null) new Pickaxe().collect( hero.belongings.backpack );
					GLog.i( "Scene: mine the thin wall of loose rock east of you (debug health off). When the ceiling"
							+ " groans, step off the marked cells." );
					break;
				case THIN_ICE: {
					int to = nearest( ow, hero.pos, 20, c -> standsBy( ow, c, n -> LayerHazards.thinIce( ow.map, ow.width(), n, MILD ) ) );
					place( hero, to );
					GLog.i( "Scene: a mild day, the air held at -1C (debug override; the Weather tab releases it): the whole"
							+ " tarn's ice is thin; below -2C only its shore would be, and at -10C or colder none (debug health"
							+ " off). Step on, stand, and watch it crack." );
					break;
				}
				case THIN_AIR: {
					Buff.prolong( hero, WarmthBuff.class, 2000f );
					int to = nearest( ow, hero.pos, 20, c -> LayerHazards.ridgeCell( ow.map, ow.width(), c ) );
					place( hero, to );
					GLog.i( "Scene: slice +" + altitude + ", the wind held at 16 (debug override; the Weather tab releases it),"
							+ " the hero warmed against the cold, debug health off. On the ridge a gust is warned of, then pushes"
							+ " along it or staggers you; Breathless builds while you stay this high." );
					break;
				}
			}
		}

		//a passable cell with a 4-neighbour the test likes
		private static boolean standsBy( OverworldLevel ow, int c, java.util.function.IntPredicate n ){
			int w = ow.width();
			if (!ow.passable[c] || ow.pit[c] || ow.map[c] == Terrain.FROZEN_WATER) return false;
			return n.test( c - w ) || n.test( c + 1 ) || n.test( c + w ) || n.test( c - 1 );
		}

		//the cell nearest `from` within r that the test likes, `from` itself first; -1 when none
		private static int nearest( OverworldLevel ow, int from, int r, java.util.function.IntPredicate test ){
			int w = ow.width(), fx = from % w, fy = from / w;
			for (int d = 0; d <= r; d++){
				for (int dy = -d; dy <= d; dy++){
					for (int dx = -d; dx <= d; dx++){
						if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != d) continue;
						int x = fx + dx, y = fy + dy;
						if (x < 2 || y < 2 || x >= w - 2 || y >= ow.height() - 2) continue;
						int c = x + y * w;
						if (Actor.findChar( c ) == null || c == from){
							if (test.test( c )) return c;
						}
					}
				}
			}
			return -1;
		}

		private static void place( Hero hero, int cell ){
			if (cell == -1 || cell == hero.pos){
				if (cell == -1) GLog.w( "Scene: nothing of the kind within 20 cells of the landing" );
				return;
			}
			hero.pos = cell;
			if (hero.sprite != null) hero.sprite.place( cell );
		}
	}

	//what the hero can see of the ore from where he stands: the glinting faces within eight cells by metal, the crystals
	private static String report( Hero hero ){
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		int w = ow.width(), hx = hero.pos % w, hy = hero.pos / w;
		int[] faces = new int[Ores.Kind.values().length];
		int crystals = 0;
		for (int y = Math.max( 1, hy - 8 ); y <= Math.min( ow.height() - 2, hy + 8 ); y++){
			for (int x = Math.max( 1, hx - 8 ); x <= Math.min( w - 2, hx + 8 ); x++){
				int c = x + y * w;
				if (ow.map[c] == Terrain.MINE_CRYSTAL) crystals++;
				Ores.Kind k = ow.shownOre( c );
				if (k != null) faces[k.ordinal()]++;
			}
		}
		StringBuilder sb = new StringBuilder();
		for (Ores.Kind k : Ores.Kind.values()){
			if (faces[k.ordinal()] == 0) continue;
			sb.append( sb.length() == 0 ? "" : ", " ).append( faces[k.ordinal()] ).append( ' ' ).append( k.name().toLowerCase( Locale.ENGLISH ) );
		}
		return (sb.length() == 0 ? "no" : sb.toString()) + " vein faces showing"
				+ (ow.altitude() < 0 ? ", " + crystals + " crystals" : "") + " within 8 cells";
	}

	//the caves' small life (levels/overworld/CaveLife): the chamber nearest the origin on the
	//slice with pools (a deep one), fungus and crystal; on arrival the fog goes back on and a full
	//set is put down around the hero. the deep scene burns one fungus patch beside him to embers
	//- its only edit - so the deep air's embers show
	private static final class CaveScene implements Scene {
		final String id, title;
		final int altitude;
		final boolean burnPatch;
		//set while the hero is on his way there, so the arrival sets the scene instead of sending him off again
		boolean travelling;
		CaveScene( String id, String title, int altitude, boolean burnPatch ){
			this.id = id; this.title = title; this.altitude = altitude; this.burnPatch = burnPatch;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != altitude){
					GLog.w( "Scene: could not reach slice " + altitude );
					return;
				}
				Dungeon.debugNoFog = false;
				if (burnPatch) burnNearestPatch( hero );
				GLog.i( "Scene: " + OverworldCritters.showcase( false ) + " (fog on: what players see)" );
				return;
			}
			int[] spot = CaveLife.findChamber( OverworldLevel.worldSeedOf( Dungeon.seed ), altitude );
			if (spot == null){
				GLog.w( "Scene: no cave chamber with pools, fungus and crystal near the origin at " + altitude );
				return;
			}
			travelling = true;
			pending = this;
			starting = true;
			toSlice( altitude, spot[0], spot[1] );
		}
	}

	//the fungus patch nearest the hero within five cells, burnt to embers
	private static void burnNearestPatch( Hero hero ){
		Level l = Dungeon.level;
		int w = l.width(), hx = hero.pos % w, hy = hero.pos / w, best = -1, bestD = Integer.MAX_VALUE;
		for (int y = Math.max( 1, hy - 5 ); y <= Math.min( l.height() - 2, hy + 5 ); y++){
			for (int x = Math.max( 1, hx - 5 ); x <= Math.min( w - 2, hx + 5 ); x++){
				int c = x + y * w, d = (x - hx) * (x - hx) + (y - hy) * (y - hy);
				if (l.map[c] == Terrain.MUSHROOM_PATCH && d < bestD){
					bestD = d;
					best = c;
				}
			}
		}
		if (best < 0){
			GLog.w( "Scene: no fungus patch within five cells to burn for the embers" );
			return;
		}
		Level.set( best, Terrain.EMBERS );
		GameScene.updateMap( best );
		GLog.i( "Scene: one fungus patch beside you burnt to embers, to show the deep air's embers" );
	}

	//the peaks' small life (levels/overworld/PeakLife): the clock walked on and the rain held off,
	//then alpine meadow by the band's edge (the eagle, the marmots) or a summit with the clouds
	//below all round. arrival puts the fog back on and a full set down around the hero
	private static final class PeakScene implements Scene {
		final String id, title;
		final int altitude;
		final GameCalendar.Season season;   //null: the season it is
		final DayNightCycle.Phase phase;
		final float into;
		final boolean clouds;
		//set while the hero is on his way there, so the arrival sets the scene instead of sending him off again
		boolean travelling;
		//the slice actually travelled to: a summit search may settle on a lower one
		int target;
		PeakScene( String id, String title, int altitude, GameCalendar.Season season, DayNightCycle.Phase phase,
				float into, boolean clouds ){
			this.id = id; this.title = title; this.altitude = altitude; this.season = season;
			this.phase = phase; this.into = into; this.clouds = clouds;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != target){
					GLog.w( "Scene: could not reach slice " + target );
					return;
				}
				Dungeon.debugNoFog = false;
				GLog.i( "Scene: " + OverworldCritters.showcase( false ) + " (fog on: what players see)" );
				if (OverworldCritters.sky() != OverworldCritters.Sky.CLEAR){
					GLog.w( "Scene: the weather keeps new critters in; clear it in the debug menu to see more come" );
				}
				return;
			}
			if (!walkClock( season, phase, into )) GLog.w( "Scene: a real-clock run keeps its own season and hour" );
			ClimateManager.debugPrecipOverride = 0f;
			ClimateManager.debugForceStorm = false;
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.calendarShift();
			search( clouds ? "a summit above the clouds" : "thawed alpine meadow by the edge of slice +" + altitude,
					() -> clouds ? PeakLife.findPeak( seed, altitude, shift ) : PeakLife.findMeadowEdge( seed, altitude, shift ),
					spot -> {
				if (spot == null){
					GLog.w( clouds ? "Scene: no summit of +" + PeakLife.CLOUD_ALTITUDE + " or higher near the origin in this world"
							: "Scene: no thawed alpine meadow by the edge of slice " + altitude + " near the origin" );
					return;
				}
				target = clouds ? spot[2] : altitude;
				if (target != altitude) GLog.w( "Scene: no +" + altitude + " summit near the origin: the highest is +" + target );
				travelling = true;
				pending = this;
				starting = true;
				toSlice( target, spot[0], spot[1] );
			} );
		}
	}

	//a place of the caves (levels/overworld/CaveSites): the one of its kind nearest the origin, on
	//its favourite slice or the nearest other it lies on, the hero set down where it is seen from,
	//with what the place asks for in his pack; arrival puts the fog back on
	private static final class CaveSiteScene implements Scene {
		final String id, title;
		final CaveSites.Type type;
		final int altitude;
		//set while the hero is on his way there, so the arrival sets the scene instead of sending him off again
		boolean travelling;
		CaveSites.Site site;
		CaveSiteScene( String id, String title, CaveSites.Type type, int altitude ){
			this.id = id; this.title = title; this.type = type; this.altitude = altitude;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != site.altitude){
					GLog.w( "Scene: could not reach slice " + site.altitude );
					return;
				}
				Dungeon.debugNoFog = false;
				GLog.i( "Scene: " + report( hero ) + " (fog on: what players see)" );
				return;
			}
			site = caveSite( OverworldLevel.worldSeedOf( Dungeon.seed ), type, altitude );
			if (site == null){
				GLog.w( "Scene: no " + type.key() + " near the origin on any slice it lies on" );
				return;
			}
			equip( hero );
			travelling = true;
			pending = this;
			starting = true;
			toSlice( site.altitude, site.standX, site.standY );
		}
		//what the place asks for: a pick to follow the mine, ore to sell and to smelt, gold for the pool
		private void equip( Hero hero ){
			switch (type){
				case MINE:
					if (hero.belongings.getItem( Pickaxe.class ) == null) new Pickaxe().collect( hero.belongings.backpack );
					break;
				case CAMP:
					Dungeon.gold = Math.max( Dungeon.gold, 3000 );
					Ores.lumps( Ores.commonKind( site.altitude ), 5 ).collect( hero.belongings.backpack );
					Reflection.newInstance( Ores.rollGem( site.altitude, 50 ).item ).collect( hero.belongings.backpack );
					break;
				case SHRINE:
					Dungeon.gold = Math.max( Dungeon.gold, 3000 );
					break;
				case RIFT:
					Ores.lumps( Ores.Kind.DEEPSILVER, Ores.Kind.DEEPSILVER.forgeBatch() ).collect( hero.belongings.backpack );
					break;
				default:
					break;
			}
		}
		private String report( Hero hero ){
			String at = type.key() + " at (" + site.cx + "," + site.cy + "), caves " + site.altitude;
			switch (type){
				case MINE: return at + (site.hasShaft() ? ", its shaft at (" + site.shaftX + "," + site.shaftY + ")" : ", no shaft");
				case TOMB: return at + ", its key hidden in the bones at (" + site.keyX + "," + site.keyY + ")";
				case CAMP: return at + ", 3000 gold and ore to trade";
				case RIFT: return at + ", " + Ores.Kind.DEEPSILVER.forgeBatch() + " deepsilver for the forge";
				default:   return at;
			}
		}
	}

	/**
	 * Debug and tooling: the place of a kind nearest the world's origin, on the slice given when it
	 * lies on that one, else on the nearest other it lies on (twelve sector rings out at most on each);
	 * a mine with its shaft down when one is that near, so the scene shows where it leads.
	 */
	public static CaveSites.Site caveSite( long seed, CaveSites.Type type, int altitude ){
		ArrayList<Integer> slices = new ArrayList<>();
		for (int a = type.shallowest; a >= type.deepest; a--) slices.add( a );
		java.util.Collections.sort( slices, ( a, b ) -> Math.abs( a - altitude ) - Math.abs( b - altitude ) );
		for (boolean shaft : type == CaveSites.Type.MINE ? new boolean[]{ true, false } : new boolean[]{ false }){
			for (int a : slices){
				CaveSites.Site s = CaveSites.nearest( seed, type, a, 0, 0, 12, shaft );
				if (s != null) return s;
			}
		}
		return null;
	}

	//a place on the mountains (levels/overworld/MountainSites): the nearest of a kind to the origin,
	//the clock walked on to the hour it is best seen at, the sky cleared, the hero set down where a
	//player would walk up to it (MountainSites.standCell: plain ground, never rock an arrival would
	//carve) and warmed against the peaks' cold unless the cold is the point. arrival puts the fog
	//back on and names the place
	private static final class PeakSiteScene implements Scene {
		final String id, title;
		final MountainSites.Kind kind;
		final GameCalendar.Season season;
		final DayNightCycle.Phase phase;
		final float into;
		final boolean warm;
		//set while the hero is on his way there, so the arrival sets the scene instead of sending him off again
		boolean travelling;
		MountainSites.Site site;
		PeakSiteScene( String id, String title, MountainSites.Kind kind, GameCalendar.Season season,
				DayNightCycle.Phase phase, float into, boolean warm ){
			this.id = id; this.title = title; this.kind = kind;
			this.season = season; this.phase = phase; this.into = into; this.warm = warm;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != site.altitude){
					GLog.w( "Scene: could not reach slice " + site.altitude );
					return;
				}
				Dungeon.debugNoFog = false;
				if (warm) Buff.prolong( hero, WarmthBuff.class, 2000f );
				GLog.i( "Scene: " + MountainSites.mapName( ((OverworldLevel) Dungeon.level).worldSeed(), kind, site.wx, site.wy )
						+ " at " + site.wx + "," + site.wy + " on height +" + site.altitude
						+ (warm ? "" : " (the hero is not warmed: the cold is the point)") + " (fog on: what players see)" );
				return;
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			search( "the nearest " + kind.lower(), () -> MountainSites.nearest( seed, kind, 0, 0, 48 ), found -> {
				site = found;
				if (site == null){
					GLog.w( "Scene: no " + kind.lower() + " found within 48 sectors of the origin (the search gives up after "
							+ MountainSites.NEAREST_SECONDS + "s)" );
					return;
				}
				if (!walkClock( season, phase, into )){
					GLog.w( "Scene: a real-clock run keeps its own season and hour" );
				}
				ClimateManager.debugPrecipOverride = 0f;
				ClimateManager.debugForceStorm = false;
				int[] at = MountainSites.standCell( seed, site );
				travelling = true;
				pending = this;
				starting = true;
				toSlice( site.altitude, at[0], at[1] );
			} );
		}
	}

	//the slices' creatures (levels/overworld/OverworldFauna's bands): one of every kind of the caves'
	//(or the mountains') bands, each at the strength of the furthest band that has it, set down
	//asleep around the hero - the fish in the nearest deep pool - and named band by band in the log.
	//a catalogue: the fog stays off
	private static final class LayerFauna implements Scene {
		final String id, title;
		final int altitude;
		//set while the hero is on his way there, so the arrival sets the scene instead of sending him off again
		boolean travelling;
		LayerFauna( String id, String title, int altitude ){
			this.id = id; this.title = title; this.altitude = altitude;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			boolean caves = altitude < 0;
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != altitude){
					GLog.w( "Scene: could not reach slice " + altitude );
					return;
				}
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				if (!caves) Buff.prolong( hero, WarmthBuff.class, 2000f );
				ArrayList<Kind> kinds = faunaKinds( caves );
				ArrayList<Integer> cells = cellsAround( hero, kinds.size() );
				int next = 0;
				for (Kind k : kinds){
					int cell;
					if (OverworldFauna.home( k.cls ) == OverworldFauna.Habitat.POOL){
						cell = nearestHabitat( ow, hero.pos, 30, OverworldFauna.Habitat.POOL );
					} else {
						cell = next < cells.size() ? cells.get( next++ ) : -1;
					}
					Mob m = Reflection.newInstance( k.cls );
					if (cell == -1 || m == null){
						GLog.w( "Scene: no room for the " + (m == null ? k.cls.getSimpleName() : m.name()) + " near enough" );
						continue;
					}
					OverworldFauna.prepare( m, k.altitude );
					m.pos = cell;
					m.state = m.SLEEPING;
					GameScene.add( m );
				}
				for (String line : bandLines( caves )) GLog.i( "Scene: " + line );
				return;
			}
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			int[] spot = caves ? faunaChamber( seed, altitude ) : faunaPeak( seed, altitude );
			if (spot == null){
				GLog.w( "Scene: no " + (caves ? "dry chamber" : "open ground") + " of slice " + altitude + " near the origin in this world" );
				return;
			}
			travelling = true;
			pending = this;
			starting = true;
			toSlice( altitude, spot[0], spot[1] );
		}
	}

	//a mountain goat at a cliff's lip on the high pastures (+2) by day: the hero on the lip, the drop at
	//his back and the goat beside him. strike it and it butts him over the edge (down a slice); wait
	//and it slips away along the edge. the fog stays off: the scene is the goat
	private static final class GoatCliff implements Scene {
		static final int ALTITUDE = 2;
		boolean travelling;
		@Override public String id(){ return "layers-goat-cliff"; }
		@Override public String title(){ return "Peaks: a goat at a cliff edge"; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != ALTITUDE){
					GLog.w( "Scene: could not reach slice " + ALTITUDE );
					return;
				}
				Buff.prolong( hero, WarmthBuff.class, 2000f );
				int[] lip = cliffLip( Dungeon.level, hero.pos, 40 );
				if (lip == null){
					GLog.w( "Scene: no cliff edge within 40 cells" );
					return;
				}
				ScrollOfTeleportation.appear( hero, lip[0] );
				spawn( Goat.class, lip[1] );
				GLog.i( "Scene: the drop is at your back. Strike the goat and it answers with a butt that throws you "
						+ "over the edge (down a slice); wait instead and it slips away along the edge." );
				return;
			}
			if (!walkClock( null, DayNightCycle.Phase.DAY, 0.3f )){
				GLog.w( "Scene: a real-clock run keeps its own hour - goats are out only by day" );
			}
			int[] spot = faunaPeak( OverworldLevel.worldSeedOf( Dungeon.seed ), ALTITUDE );
			if (spot == null){
				GLog.w( "Scene: no open ground of slice " + ALTITUDE + " near the origin in this world" );
				return;
			}
			travelling = true;
			pending = this;
			starting = true;
			toSlice( ALTITUDE, spot[0], spot[1] );
		}
	}

	/** A kind of the slices' fauna and the altitude of the furthest band that has it (its strongest self). */
	static final class Kind {
		final Class<? extends Mob> cls;
		final int altitude;
		Kind( Class<? extends Mob> cls, int altitude ){ this.cls = cls; this.altitude = altitude; }
	}

	//every kind of the caves' bands (or the mountains'), in band order, each once, at its furthest band
	static ArrayList<Kind> faunaKinds( boolean caves ){
		ArrayList<Kind> out = new ArrayList<>();
		for (int i = 1; WorldLayers.exists( caves ? -i : i ); i++){
			int a = caves ? -i : i;
			for (Class<? extends Mob> cls : OverworldFauna.kindsAt( a )){
				Kind had = null;
				for (Kind k : out) if (k.cls == cls) had = k;
				if (had == null) out.add( new Kind( cls, a ) );
				else out.set( out.indexOf( had ), new Kind( cls, a ) );
			}
		}
		return out;
	}

	//one log line per band: its slices and its kinds by name
	private static ArrayList<String> bandLines( boolean caves ){
		ArrayList<String> out = new ArrayList<>();
		int from = 0;
		ArrayList<Class<? extends Mob>> kinds = null;
		for (int i = 1; ; i++){
			int a = caves ? -i : i;
			ArrayList<Class<? extends Mob>> here = WorldLayers.exists( a ) ? OverworldFauna.kindsAt( a ) : null;
			if (kinds != null && !kinds.equals( here )){
				StringBuilder sb = new StringBuilder( (caves ? "-" : "+") + from + ".." + (caves ? "-" : "+") + (i - 1) + ":" );
				for (Class<? extends Mob> cls : kinds){
					Mob m = Reflection.newInstance( cls );
					sb.append( ' ' ).append( m == null ? cls.getSimpleName() : m.name() ).append( ',' );
				}
				sb.setLength( sb.length() - 1 );
				out.add( sb.toString() );
				kinds = null;
			}
			if (here == null) return out;
			if (kinds == null){
				kinds = here;
				from = i;
			}
		}
	}

	//the window cell nearest `from` (rings out to `radius`) whose ground is of the habitat, with nobody on it; -1 if none
	static int nearestHabitat( OverworldLevel ow, int from, int radius, OverworldFauna.Habitat want ){
		int w = ow.width(), fx = from % w, fy = from / w;
		for (int r = 0; r <= radius; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = fx + dx, y = fy + dy;
					if (x <= 0 || y <= 0 || x >= w - 1 || y >= ow.height() - 1) continue;
					int c = x + y * w;
					if ((OverworldFauna.habitat( ow, c ) & want.bit) != 0 && Actor.findChar( c ) == null) return c;
				}
			}
		}
		return -1;
	}

	/** A cliff's lip near `from` (rings out to `radius`): { the lip cell, with a drop straight behind it,
	 *  the open cell in front of it }, both free; null if none. */
	static int[] cliffLip( Level level, int from, int radius ){
		int w = level.width(), fx = from % w, fy = from / w;
		for (int r = 0; r <= radius; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = fx + dx, y = fy + dy;
					if (x <= 2 || y <= 2 || x >= w - 3 || y >= level.height() - 3) continue;
					int c = x + y * w;
					if (!level.passable[c] || level.pit[c] || (Actor.findChar( c ) != null && c != from)
							|| level.map[c] == Terrain.ENTRANCE || level.map[c] == Terrain.EXIT) continue;
					for (int d : new int[]{ 1, -1, w, -w }){
						if (level.pit[c + d] && level.passable[c - d] && !level.pit[c - d] && Actor.findChar( c - d ) == null
								&& level.passable[c - 2 * d] && !level.pit[c - 2 * d]){
							return new int[]{ c, c - d };
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Debug and tooling: a dry cave chamber of the slice near the origin - the cell and the 9x9 around
	 * it open and dry - on rings 16 cells apart out to 480; one with a deep pool within twelve cells
	 * (wet on this slice and the one below: the water runs deep there) when there is one, else the
	 * first dry one. null when there is none.
	 */
	static int[] faunaChamber( long seed, int altitude ){
		int[] dry = null;
		int dryRing = -1;
		for (int r = 0; r <= 480; r += 16){
			if (dry != null && r - dryRing > 8 * 16) break;
			for (int dy = -r; dy <= r; dy += 16){
				for (int dx = -r; dx <= r; dx += 16){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					if (!dryChamber( seed, altitude, dx, dy )) continue;
					if (dry == null){
						dry = new int[]{ dx, dy };
						dryRing = r;
					}
					for (int py = -12; py <= 12; py += 3){
						for (int px = -12; px <= 12; px += 3){
							if (WorldModel.caveWet( seed, dx + px, dy + py, altitude )
									&& WorldModel.caveWet( seed, dx + px, dy + py, altitude - 1 )) return new int[]{ dx, dy };
						}
					}
				}
			}
		}
		return dry;
	}

	private static boolean dryChamber( long seed, int altitude, int cx, int cy ){
		for (int y = -4; y <= 4; y++){
			for (int x = -4; x <= 4; x++){
				if (!WorldModel.caveOpen( seed, cx + x, cy + y, altitude ) || WorldModel.caveWet( seed, cx + x, cy + y, altitude )) return false;
			}
		}
		return true;
	}

	/** Debug and tooling: open ground of the mountain slice near the origin - the cell and the 5x5
	 *  around it all of the slice's own band - on rings 32 cells apart out to 3200 (the high bands
	 *  are rare); null if none. */
	static int[] faunaPeak( long seed, int altitude ){
		for (int r = 0; r <= 3200; r += 32){
			for (int dy = -r; dy <= r; dy += 32){
				for (int dx = -r; dx <= r; dx += 32){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					if (WorldLayers.band( WorldModel.elevation( seed, dx, dy ) ) != altitude) continue;
					boolean all = true;
					for (int y = -2; y <= 2 && all; y++){
						for (int x = -2; x <= 2 && all; x++){
							all = WorldLayers.band( WorldModel.elevation( seed, dx + x, dy + y ) ) == altitude;
						}
					}
					if (all) return new int[]{ dx, dy };
				}
			}
		}
		return null;
	}

	//the floor of Warped's own rooms (levels/WarpedRoomsLevel): every one of them painted
	//once and left running, the season-reading ones once per season, and a row of supplies
	//by the entrance. Each run of the scene builds the floor afresh, so a room that has been
	//used up is whole again. The night variant also walks the world's clock on to nightfall
	//- moved, not overridden, so it keeps running - because the black market's dealer is
	//only on the floor after dark
	private static final class WarpedRoomsScene implements Scene {
		static final int DEPTH = 86;
		final String id, title;
		final boolean night;
		//the twelve Warped rooms and nothing else: no standard rooms, no season copies
		final boolean warpedOnly;
		//set while the hero is on his way to the floor, so the arrival finishes the scene
		//instead of sending him off again
		boolean travelling;
		WarpedRoomsScene( String id, String title, boolean night, boolean warpedOnly ){
			this.id = id; this.title = title; this.night = night; this.warpedOnly = warpedOnly;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			if (!travelling){
				travelling = true;
				pending = this;
				starting = true;
				//read by the floor as it is built, a few moments from now
				xyz.gabriwar.warpedpixeldungeon.levels.WarpedRoomsLevel.warpedOnly = warpedOnly;
				//never the saved floor: a fresh one every time
				Dungeon.generatedLevels.remove( Integer.valueOf( DEPTH ) );
				if (Dungeon.depth == DEPTH && Dungeon.branch == 0){
					InterlevelScene.mode = InterlevelScene.Mode.RESET;
				} else {
					InterlevelScene.mode = InterlevelScene.Mode.RETURN;
					InterlevelScene.returnDepth = DEPTH;
					InterlevelScene.returnBranch = 0;
					InterlevelScene.returnPos = -1;
				}
				Game.switchScene( InterlevelScene.class );
				return;
			}
			travelling = false;

			//enough coin for the market's whole counter, the wheel and a few contracts, and a
			//clean slate with the market: no heat, no ban carried in from an earlier test
			Dungeon.gold = Math.max( Dungeon.gold, 50000 );
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms.settle();
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms.banned = false;

			if (night && !Dungeon.isChallenged( xyz.gabriwar.warpedpixeldungeon.Challenges.REAL_CLOCK )){
				xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase p
						= xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phase();
				if (p != xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.NIGHT){
					int turns = xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.turnsUntilPhaseChange();
					for (p = p.next(); p != xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.NIGHT; p = p.next()){
						turns += xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phaseDuration( p );
					}
					//a few turns past the boundary, so it is unmistakably night
					turns += 5;
					Dungeon.cycleTurn += turns;
					xyz.gabriwar.warpedpixeldungeon.Statistics.duration += turns;
					xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.onHeroTurn();
				}
			}

			GLog.i( "Warped rooms: the signs name each room; supplies are in the two rows by the entrance." );
			GLog.i( "The floor's climate is a sewer floor's: the debug menu's weather, temperature and hour overrides all reach it." );
			if (night) GLog.i( "Clock moved to nightfall: the black market's dealer sets up on his next turn." );
		}
	}

	//a floor of one place below ground, with its small life and its sounds: the clock walked on to
	//the hour the place's life is out, the hero set down where the ground of the most of its kinds
	//is in sight, the fog back on (what players see), and a log of what lives there and is heard
	private static final class AmbienceScene implements Scene {
		final String id, title;
		final int depth, branch;
		final DayNightCycle.Phase phase;
		//set while the hero is on his way to the floor, so the arrival finishes the scene
		//instead of sending him off again
		boolean travelling;
		AmbienceScene( String id, String title, int depth, int branch, DayNightCycle.Phase phase ){
			this.id = id; this.title = title; this.depth = depth; this.branch = branch; this.phase = phase;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			if (!travelling && (Dungeon.depth != depth || Dungeon.branch != branch)){
				travelling = true;
				pending = this;
				starting = true;
				InterlevelScene.mode = InterlevelScene.Mode.RETURN;
				InterlevelScene.returnDepth = depth;
				InterlevelScene.returnBranch = branch;
				InterlevelScene.returnPos = -1;
				Game.switchScene( InterlevelScene.class );
				return;
			}
			travelling = false;
			if (!walkClock( null, phase, 0.3f )) GLog.w( "Scene: a real-clock run keeps its own hour" );
			int cell = DungeonLife.liveliest( Dungeon.level, hero.pos );
			if (cell >= 0 && cell != hero.pos) ScrollOfTeleportation.appear( hero, cell );
			Dungeon.debugNoFog = false;
			GLog.i( "Scene: " + DungeonLife.showcase( Dungeon.level ) );
			GLog.i( "Scene: " + AmbientSounds.showcase( Dungeon.level ) );
			if (!AmbientPlayer.audible()) GLog.w( "Scene: the ambience is muted or at zero (Settings > Audio)" );
			if (ClimateManager.debugForceStorm){
				GLog.w( "Scene: a storm is still forced (weather-storm), and the calm voices keep quiet in it: the Weather"
						+ " tab's Force storm box or the weather-clear scene ends it" );
			}
		}
	}

	//room acoustics (audio/RoomAcoustics, docs/ambience.md): a place with one kind of space, the hero
	//set down in it and the fog back on, the space logged as a sound finds it (Earshot), then, once
	//the muffled copies are in use, three rounds 1.5 s apart of a heavy blow on the hero, a blast six
	//cells off, a blow behind the nearest door and a mine going off behind the nearest rock, with
	//what the budget let through each round
	static final class AcousticsScene implements Scene {
		//where on the floor: where the hero lands, the nearest room, the middle of the longest
		//corridor, the biggest space, or the nearest spot with an echo
		enum Spot { HERE, ROOM, CORRIDOR, LARGEST, ECHO }
		static final int ROUNDS = 3;
		static final float ROUND = 1.5f;
		//how long the rounds wait for the muffled copies at most (s)
		static final float COPIES_WAIT = 20f;

		final String id, title;
		final int depth, branch;   //a floor of the dungeon; 0 for a slice of the world
		final int altitude;        //the slice, for depth 0
		final Spot spot;
		final boolean fight;       //a storm forced and a fight started round the hero
		//set while the hero is on his way there, so the arrival sets the scene instead of sending him off again
		boolean travelling;

		AcousticsScene( String id, String title, int depth, int branch, int altitude, Spot spot, boolean fight ){
			this.id = id; this.title = title; this.depth = depth; this.branch = branch;
			this.altitude = altitude; this.spot = spot; this.fight = fight;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			if (travelling || (depth > 0 && Dungeon.depth == depth && Dungeon.branch == branch)){
				travelling = false;
				arrive( hero );
				return;
			}
			if (depth > 0){
				travelling = true;
				pending = this;
				starting = true;
				InterlevelScene.mode = InterlevelScene.Mode.RETURN;
				InterlevelScene.returnDepth = depth;
				InterlevelScene.returnBranch = branch;
				InterlevelScene.returnPos = -1;
				Game.switchScene( InterlevelScene.class );
				return;
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			if (altitude > 0){
				//the alpine meadow at the edge of the slice, the rock of the next one up rising over it,
				//on a dry summer's day: the meadow must be thawed to be found
				if (!walkClock( GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.3f )) GLog.w( "Scene: a real-clock run keeps its own season and hour" );
				ClimateManager.debugPrecipOverride = 0f;
				ClimateManager.debugForceStorm = false;
				final float shift = WorldModel.calendarShift();
				search( "thawed alpine meadow by the edge of slice +" + altitude,
						() -> PeakLife.findMeadowEdge( seed, altitude, shift ), at -> {
					if (at == null){
						GLog.w( "Scene: no thawed alpine meadow by the edge of slice +" + altitude + " near the origin" );
						return;
					}
					travelling = true;
					pending = this;
					starting = true;
					toSlice( altitude, at[0], at[1] );
				} );
				return;
			}
			int[] at = CaveLife.findChamber( seed, altitude );
			if (at == null){
				GLog.w( "Scene: no cave chamber near the origin at " + altitude );
				return;
			}
			travelling = true;
			pending = this;
			starting = true;
			toSlice( altitude, at[0], at[1] );
		}

		private void arrive( Hero hero ){
			Level level = Dungeon.level;
			boolean there = depth > 0 ? Dungeon.depth == depth && Dungeon.branch == branch
					: level instanceof OverworldLevel && ((OverworldLevel) level).altitude() == altitude;
			if (!there){
				GLog.w( "Scene: could not get there" );
				return;
			}
			int to = pick( level, hero.pos );
			if (to == -1) GLog.w( "Scene: no such spot on this floor: the test runs where you stand" );
			else if (to != hero.pos) ScrollOfTeleportation.appear( hero, to );
			Dungeon.debugNoFog = false;
			if (fight){
				new Sky( PrecipType.RAIN, 0.6f, 15f, 90f, true ).hold();
				brawl( hero );
				GLog.i( "Scene: a storm forced (the Weather tab's Force storm box or weather-clear ends it) and a"
						+ " fight round you: rest or strike, and listen for the rain and the cave's own sounds"
						+ " dropping out while the blows ring" );
			}
			Earshot.Snapshot s = Earshot.of( level, hero.pos );
			if (s == null){
				GLog.w( "Scene: room acoustics cannot tell the space here" );
				return;
			}
			GLog.i( "Scene: " + s.describe() );
			GLog.i( "Scene: " + RoomAcoustics.describe() );
			if (!RoomAcoustics.on) GLog.w( "Scene: room acoustics is off (Settings > Audio): only the dry sounds play" );
			int blast = near( level, hero.pos, 6, 6, c -> s.relation( c ) == Earshot.Relation.CLEAR );
			int door = near( level, hero.pos, 1, Earshot.RADIUS, c -> s.relation( c ) == Earshot.Relation.DOOR );
			int rock = near( level, hero.pos, 1, Earshot.RADIUS, c -> s.relation( c ) == Earshot.Relation.WALL );
			GLog.i( "Scene: " + ROUNDS + " rounds, " + ROUND + " s apart: a heavy blow on you, a blast "
					+ (blast < 0 ? "(no open cell 6 off)" : "6 cells " + where( level, hero.pos, blast ))
					+ ", a blow " + (door < 0 ? "(no door in reach)" : "behind the door " + where( level, hero.pos, door ))
					+ ", a mine " + (rock < 0 ? "(no rock in reach)" : "behind the rock " + where( level, hero.pos, rock )) );
			waitForCopies( 0f, () -> rounds( hero, blast, door, rock ) );
		}

		//the rounds, 1.5 s apart: a heavy blow on the hero, a blast six cells off, a blow behind the
		//door and a mine behind the rock, and what the budget let through and held back each round
		private static void rounds( Hero hero, int blast, int door, int rock ){
			for (int r = 0; r < ROUNDS; r++){
				final int round = r + 1;
				final int[] before = new int[2];
				float at = 0.5f + ROUND * r;
				after( at, () -> {
					before[0] = RoomAcoustics.scheduled();
					before[1] = RoomAcoustics.skipped();
					SpatialSound.play( Assets.Sounds.HIT_STRONG, hero.pos );
				} );
				if (blast >= 0) after( at + 0.3f, () -> SpatialSound.play( Assets.Sounds.BLAST, blast ) );
				if (door >= 0) after( at + 0.6f, () -> SpatialSound.play( Assets.Sounds.HIT, door ) );
				after( at + 0.9f, () -> {
					if (rock >= 0) SpatialSound.play( Assets.Sounds.MINE, rock );
					GLog.i( "Scene: round " + round + " of " + ROUNDS + ": " + (RoomAcoustics.scheduled() - before[0])
							+ " tails and echoes played, " + (RoomAcoustics.skipped() - before[1]) + " held back by the budget" );
				} );
			}
		}

		//the muffled copies come into use 10 s after the last of them loads (RoomAcoustics): the
		//rounds wait for that, COPIES_WAIT s at most, or the blows behind the door and the rock play dry
		private static void waitForCopies( float waited, Runnable then ){
			if (!RoomAcoustics.on || RoomAcoustics.muffledReady()){
				then.run();
			} else if (waited >= COPIES_WAIT){
				GLog.w( "Scene: the muffled copies are still not ready: the blows behind the door and the rock play dry" );
				then.run();
			} else {
				if (waited == 0f) GLog.i( "Scene: waiting for the muffled copies, in use 10 s after they load: stand still" );
				after( 0.5f, () -> waitForCopies( waited + 0.5f, then ) );
			}
		}

		//the cell to stand on, -1 when the floor has none of the kind
		int pick( Level level, int from ){
			switch (spot){
				case ROOM:
					return near( level, from, 0, Math.max( level.width(), level.height() ), c -> {
						Earshot.Snapshot s = Earshot.of( level, c );
						return s != null && s.space == Earshot.Space.ROOM;
					} );
				case CORRIDOR:
					return corridor( level );
				case LARGEST:
					return largest( level, from, depth > 0 ? Math.max( level.width(), level.height() ) : 12 );
				case ECHO:
					return near( level, from, 0, 15, c -> {
						Earshot.Snapshot s = Earshot.of( level, c );
						return s != null && s.echoDelay > 0f;
					} );
				default:
					return from;
			}
		}

		//a cell to stand on or to sound from: open ground with nobody on it
		private static boolean free( Level level, int c, int from ){
			return level.passable[c] && !level.pit[c] && (c == from || Actor.findChar( c ) == null);
		}

		//the free cell nearest `from`, `min` to `max` cells off, the test likes; -1 when none
		static int near( Level level, int from, int min, int max, java.util.function.IntPredicate test ){
			int w = level.width(), fx = from % w, fy = from / w;
			for (int d = min; d <= max; d++){
				for (int dy = -d; dy <= d; dy++){
					for (int dx = -d; dx <= d; dx++){
						if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != d) continue;
						int x = fx + dx, y = fy + dy;
						if (x < 1 || y < 1 || x >= w - 1 || y >= level.height() - 1) continue;
						int c = x + y * w;
						if (free( level, c, from ) && test.test( c )) return c;
					}
				}
			}
			return -1;
		}

		//the middle of the longest straight run of open cells walled in on both sides; -1 for none
		static int corridor( Level level ){
			int w = level.width(), h = level.height(), best = -1, bestLength = 0;
			for (int pass = 0; pass < 2; pass++){
				boolean across = pass == 0;
				int lines = across ? h : w, along = across ? w : h;
				for (int a = 1; a < lines - 1; a++){
					int run = 0;
					for (int b = 1; b < along; b++){
						int c = across ? b + a * w : a + b * w;
						int side = across ? w : 1;
						boolean walled = b < along - 1 && free( level, c, -1 ) && level.solid[c - side] && level.solid[c + side];
						if (walled){
							run++;
						} else {
							if (run > bestLength){
								bestLength = run;
								int mid = b - 1 - run / 2;
								best = across ? mid + a * w : a + mid * w;
							}
							run = 0;
						}
					}
				}
			}
			return best;
		}

		//the free cell within `reach` of `from`, every other one, in the biggest space, the one with
		//most open ground round it among those; -1 for none
		static int largest( Level level, int from, int reach ){
			int w = level.width(), fx = from % w, fy = from / w, best = -1, bestScore = -1;
			for (int y = Math.max( 2, fy - reach ); y <= Math.min( level.height() - 3, fy + reach ); y += 2){
				for (int x = Math.max( 2, fx - reach ); x <= Math.min( w - 3, fx + reach ); x += 2){
					int c = x + y * w;
					if (!free( level, c, from )) continue;
					Earshot.Snapshot s = Earshot.of( level, c );
					if (s == null) continue;
					int open = 0;
					for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) if (level.passable[c + dx + dy * w]) open++;
					int score = s.area * 32 + open;
					if (score > bestScore){
						bestScore = score;
						best = c;
					}
				}
			}
			return best;
		}

		//four foes round the hero, hunting him
		private static void brawl( Hero hero ){
			Class<?>[] kinds = { xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll.class,
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll.class,
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.Crab.class,
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat.class };
			ArrayList<Integer> cells = cellsAround( hero, kinds.length );
			for (int i = 0; i < kinds.length && i < cells.size(); i++){
				@SuppressWarnings("unchecked") Mob mob = spawn( (Class<? extends Mob>) kinds[i], cells.get( i ) );
				if (mob != null) mob.aggro( hero );
			}
		}

		//runs `then` on the render thread `seconds` from now, while this game scene lasts
		private static void after( float seconds, Runnable then ){
			Game.scene().add( new Delayer( seconds ){
				@Override
				protected void onComplete(){
					then.run();
				}
			} );
		}
	}

	/** The weather a weather scene holds: every climate override a weather scene sets, all set at
	 *  once, so none is left over from the scene before. NaN (null for the type) releases one. */
	static final class Sky {
		final PrecipType type;
		final float rate, wind, dir;
		final boolean storm;
		Sky( PrecipType type, float rate, float wind, float dir, boolean storm ){
			this.type = type; this.rate = rate; this.wind = wind; this.dir = dir; this.storm = storm;
		}
		void hold(){
			ClimateManager.debugPrecipTypeOverride = type;
			ClimateManager.debugPrecipOverride = rate;
			ClimateManager.debugWindOverride = wind;
			ClimateManager.debugWindDirOverride = dir;
			ClimateManager.debugForceStorm = storm;
		}
	}

	/** weather-clear: every weather override released, the temperature's and the clouds' too. */
	static void clearWeather(){
		new Sky( null, Float.NaN, Float.NaN, Float.NaN, false ).hold();
		ClimateManager.debugTempOverride = Float.NaN;
		ClimateManager.debugCloudOverride = Float.NaN;
	}

	//the weather and its sounds (levels/ambience/WeatherSounds, docs/weather-sounds.md): the sky
	//held through the climate's debug overrides, the hero set down where the weather has something
	//to fall on or blow over, the fog back on (what players see) and a log of what to listen for
	static final class WeatherScene implements Scene {
		//where a scene takes the hero: a meadow by a river and a wood (OverworldCritters), open
		//plains, the desert, frozen ground (the tundra or a snowfield: the overlay draws snow only
		//over frozen ground, rain and sleet's streaks only over thawed), the town's inn, or nowhere
		enum Ground { RIVER, PLAINS, DESERT, FROZEN, INDOORS, HERE }
		//the inn among the town's buildings (Dungeon.newLevel, branch 6)
		static final int INN = 6;
		//the clearing walk gives up after four days: a day has two spells of rain on average
		static final int CLEARING_TURNS = 4 * DayNightCycle.FULL_CYCLE;

		final String id, title;
		final Sky sky;            //null: weather-clear, every override released
		final Ground ground;
		final boolean clearing;   //the clock walked on to fog, or to the clearing after rain
		final String listen;
		//set while the hero is on his way there, so the arrival sets the scene instead of sending him off again
		boolean travelling;
		WeatherScene( String id, String title, Sky sky, Ground ground, boolean clearing, String listen ){
			this.id = id; this.title = title; this.sky = sky; this.ground = ground;
			this.clearing = clearing; this.listen = listen;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				arrive( hero );
				return;
			}
			if (sky == null){
				clearWeather();
				Dungeon.debugNoFog = false;
				GLog.i( "Scene: every weather override released (the temperature's and the clouds' too): the sky is the"
						+ " climate's own - " + ClimateManager.weatherState().name().toLowerCase( Locale.ENGLISH ) + ", "
						+ ClimateManager.localPrecipType().name().toLowerCase( Locale.ENGLISH )
						+ String.format( Locale.ENGLISH, " at %.2f, wind %.1f m/s", ClimateManager.localPrecipRate(), ClimateManager.localWindSpeed() ) );
				return;
			}
			sky.hold();
			if (clearing){
				if (walkToClearing( CLEARING_TURNS )){
					GLog.i( "Scene: the clock walked on to " + ClimateManager.weatherState().name().toLowerCase( Locale.ENGLISH )
							+ ": it holds while you stand, and ends as the clouds thin" );
				} else {
					GLog.w( Dungeon.isChallenged( Challenges.REAL_CLOCK ) ? "Scene: a real-clock run keeps its own sky - no drips without fog or a clearing"
							: "Scene: no fog and no clearing after rain within four days - no drips without one" );
				}
			}
			if (ground == Ground.INDOORS){
				if (Dungeon.depth == INN && Dungeon.branch == TownInteriorLevel.BRANCH){
					arrive( hero );
					return;
				}
				travelling = true;
				pending = this;
				starting = true;
				InterlevelScene.mode = InterlevelScene.Mode.RETURN;
				InterlevelScene.returnDepth = INN;
				InterlevelScene.returnBranch = TownInteriorLevel.BRANCH;
				InterlevelScene.returnPos = -1;
				Game.switchScene( InterlevelScene.class );
				return;
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.calendarShift();
			String what = ground == Ground.RIVER ? "a meadow by a river and a wood"
					: ground == Ground.PLAINS ? "open plains" : ground == Ground.FROZEN ? "open tundra or snowfield" : "open desert";
			search( what + " near the origin", () -> find( ground, seed, shift ), spot -> {
				if (spot == null){
					GLog.w( "Scene: no " + what + " near the origin in this world" );
					return;
				}
				travelling = true;
				pending = this;
				starting = true;
				OverworldLevel.arriveAt( spot[0], spot[1] );
				OverworldLevel.travelToSurface();
			} );
		}

		/** Debug and tooling: the world cell an overworld ground lies at near the origin, null when
		 *  none. Pure: it runs off the render thread (search). */
		static int[] find( Ground ground, long seed, float shift ){
			switch (ground){
				case RIVER:  return OverworldCritters.findCritterGround( seed, shift );
				case PLAINS: return biomeGround( seed, shift, WorldModel.Biome.PLAINS );
				case FROZEN: return biomeGround( seed, shift, WorldModel.Biome.TUNDRA, WorldModel.Biome.SNOWFIELD );
				default:     return biomeGround( seed, shift, WorldModel.Biome.DESERT );
			}
		}

		private void arrive( Hero hero ){
			Dungeon.debugNoFog = false;
			if (ground == Ground.INDOORS ? !Comfy.indoors()
					: !(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
				GLog.w( ground == Ground.INDOORS ? "Scene: could not reach the inn" : "Scene: could not reach the surface" );
				return;
			}
			if (ground == Ground.RIVER) settleByTheWater( hero, (OverworldLevel) Dungeon.level );
			GLog.i( "Scene: " + listen + " (fog on: what players see)" );
			if (!Float.isNaN( sky.wind ) && !Float.isNaN( sky.dir ) && ground != Ground.INDOORS){
				GLog.i( "Scene: " + windSide( sky.dir ) );
			}
			GLog.i( "Scene: the Weather tab of the debug menu releases the overrides" + (sky.storm ? " (the storm too, by its Force storm box)" : "")
					+ ", or the weather-clear scene all at once" );
			if (!AmbientPlayer.audible()) GLog.w( "Scene: the ambience is muted or at zero (Settings > Audio)" );
		}
	}

	//the hero set down where the rain falls on open water and in the trees beside it - the dry cell
	//nearest the landing with water within three cells and a tree within five - and the nearest of
	//each named with its side, to check the panning by
	private static void settleByTheWater( Hero hero, OverworldLevel ow ){
		int to = byWaterAndTrees( ow, hero.pos, 12 );
		if (to == -1) GLog.w( "Scene: no dry ground with water and trees close by within 12 cells of the landing" );
		else if (to != hero.pos) ScrollOfTeleportation.appear( hero, to );
		int water = HazardScene.nearest( ow, hero.pos, 12, c -> water( ow.map[c] ) );
		int tree = HazardScene.nearest( ow, hero.pos, 12, c -> tree( ow.map[c] ) );
		GLog.i( "Scene: " + (water == -1 ? "no open water within 12 cells" : "open water " + where( ow, hero.pos, water ))
				+ ", " + (tree == -1 ? "no tree within 12 cells" : "the nearest tree " + where( ow, hero.pos, tree )) );
	}

	/** Debug and tooling: the dry cell nearest `from` (rings out to `radius`) with open water within
	 *  three cells and a tree within five, nobody on it; -1 when none. */
	static int byWaterAndTrees( OverworldLevel ow, int from, int radius ){
		return HazardScene.nearest( ow, from, radius, c -> ow.passable[c] && !ow.pit[c] && !water( ow.map[c] )
				&& within( ow, c, 3, n -> water( ow.map[n] ) ) && within( ow, c, 5, n -> tree( ow.map[n] ) ) );
	}

	static boolean water( int t ){
		return (Terrain.flags[t] & Terrain.LIQUID) != 0;
	}

	static boolean tree( int t ){
		return t == Terrain.TREE_OAK || t == Terrain.TREE_PINE;
	}

	//any cell within r of c (straight-line, as the weather's sounds reach: WeatherScape.NEAR_CELLS is
	//6, so a tree within five is one its drips and its leaves are heard from) the test likes
	private static boolean within( Level level, int c, int r, java.util.function.IntPredicate test ){
		int w = level.width(), cx = c % w, cy = c / w;
		for (int y = Math.max( 0, cy - r ); y <= Math.min( level.height() - 1, cy + r ); y++){
			for (int x = Math.max( 0, cx - r ); x <= Math.min( w - 1, cx + r ); x++){
				if ((x - cx) * (x - cx) + (y - cy) * (y - cy) > r * r) continue;
				if (test.test( x + y * w )) return true;
			}
		}
		return false;
	}

	//"3 cells to the north-east" of `from`: north is up the screen
	private static String where( Level level, int from, int cell ){
		int w = level.width(), dx = cell % w - from % w, dy = cell / w - from / w;
		int d = Math.max( Math.abs( dx ), Math.abs( dy ) );
		if (d == 0) return "under you";
		return d + (d == 1 ? " cell" : " cells") + " to the " + compass( (float) Math.toDegrees( Math.atan2( dx, -dy ) ) );
	}

	/** The compass point nearest a bearing in degrees (0 = north, up the screen; 90 = east). */
	static String compass( float degrees ){
		String[] points = { "north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west" };
		return points[Math.floorMod( Math.round( degrees / 45f ), 8 )];
	}

	/** Where a wind blowing toward `dir` (degrees, ClimateManager.surfaceWindDir) is heard: it comes
	 *  from dir + 180, so its side is the pan sign -sin(dir). */
	static String windSide( float dir ){
		float pan = (float) -Math.sin( Math.toRadians( dir ) );
		String side = Math.abs( pan ) < 0.2f ? "in both ears alike" : pan < 0 ? "on your left" : "on your right";
		return "the wind blows toward the " + compass( dir ) + ": it comes from the " + compass( dir + 180f ) + ", " + side;
	}

	//the turns in a row the fog or the clearing must have held before the walk stops on it: in a front
	//the wind wobbles up to 2 m/s a turn about the fog's 3 m/s, so a fog of a few turns is a flicker
	//the arrival's own turn (DayNightCycle.onHeroTurn) or the first steps end - and a flicker of fog
	//ends a clearing too (it no longer follows rain). measured: stopping on the first turn, 20 walks in
	//480 found it gone within three more; after 8 turns held, 7 in 1200; after 20, none in 1200. longer
	//holds land later in the clearing, nearer its end
	static final int FOG_HOLD = 20;

	/** Debug and tooling: walks the clock on a turn at a time, the climate stepping with it as it does
	 *  in play, until the sky has been fogged in or clearing after rain (when the trees drip) for
	 *  FOG_HOLD turns in a row, at most `turns` turns; forward only, the game's duration with it. False
	 *  when neither came, or on a real-clock run. */
	static boolean walkToClearing( int turns ){
		if (Dungeon.isChallenged( Challenges.REAL_CLOCK )) return false;
		int held = foggedOrClearing() ? 1 : 0;
		for (int t = 0; t < turns && held < FOG_HOLD; t++){
			Dungeon.cycleTurn++;
			Statistics.duration++;
			ClimateManager.onHeroTurn();
			held = foggedOrClearing() ? held + 1 : 0;
		}
		return held >= FOG_HOLD;
	}

	private static boolean foggedOrClearing(){
		WeatherState s = ClimateManager.weatherState();
		return s == WeatherState.FOG || s == WeatherState.CLEARING;
	}

	/**
	 * Debug and tooling: open ground of a biome (or of any of a few) near the world's origin, for the
	 * weather scenes: a cell on dry ground, out of the town and every village, whose 5x5 samples four
	 * cells apart are all of the biomes given at the seasonal shift given and at a climate band either
	 * side (the live window adds the weather's band to the calendar, OverworldLevel.refreshSeasonShift).
	 * On rings 64 cells apart out to 6400, the nearest of the first ring that has one; null when none.
	 * Pure: it runs off the render thread (search).
	 */
	static int[] biomeGround( long seed, float shift, WorldModel.Biome... biomes ){
		EnumSet<WorldModel.Biome> biome = EnumSet.copyOf( Arrays.asList( biomes ) );
		WorldModel.Sample s = new WorldModel.Sample();
		float[] shifts = { shift, shift - WorldModel.CLIMATE_SHIFT, shift + WorldModel.CLIMATE_SHIFT };
		final int step = 64, rings = 100;
		for (int r = 0; r <= rings; r++){
			int[] best = null;
			long bestD = Long.MAX_VALUE;
			for (int j = -r; j <= r; j++){
				for (int i = -r; i <= r; i++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int cx = i * step, cy = j * step;
					if (!biomeGroundAt( seed, cx, cy, shifts, biome, s )) continue;
					long d = (long) cx * cx + (long) cy * cy;
					if (d < bestD){
						bestD = d;
						best = new int[]{ cx, cy };
					}
				}
			}
			if (best != null) return best;
		}
		return null;
	}

	private static boolean biomeGroundAt( long seed, int cx, int cy, float[] shifts, EnumSet<WorldModel.Biome> biome, WorldModel.Sample s ){
		if (!biome.contains( WorldModel.sample( seed, cx, cy, shifts[0], s ).biome )) return false;
		int t = WorldModel.wildTerrain( seed, cx, cy, s );
		if ((Terrain.flags[t] & Terrain.PASSABLE) == 0 || water( t )) return false;
		if (WorldStructures.townCell( cx, cy ) != -1 || OverworldFauna.nearSettlement( seed, cx, cy )) return false;
		for (float shift : shifts){
			for (int y = -8; y <= 8; y += 4){
				for (int x = -8; x <= 8; x += 4){
					if (!biome.contains( WorldModel.sample( seed, cx + x, cy + y, shift, s ).biome )) return false;
				}
			}
		}
		return true;
	}

	//in the blank room, a pool of water from the cell east of the hero to the room's east side, a
	//rat standing in it three cells off, two more and a bat flying over it three steps from that
	//one (out of its arcs' reach, in the run's), and a gnoll on dry ground to the west, all stock
	//still and hardy enough for a few zaps; a wand of lightning +3 in hand, charged and quickslotted.
	//For the wand's run over the water (WandOfLightning.runOverWater) and every lightning's look,
	//flash and crack (effects/Lightning)
	private static final class LightningWater implements Scene {
		//the pool's reach east of the hero, and each way north and south
		static final int POOL_EAST = Room.HALF_W - 1, POOL_HALF = 3;
		//the stand-ins' health: a few zaps of a +3 wand at the whole blow
		static final int HARDY = 300;
		@Override public String id(){ return "lightning-water"; }
		@Override public String title(){ return "Lightning: a wand of lightning by a pool of monsters"; }
		@Override public void apply( Hero hero ){
			if (!Room.ensure( this, hero )) return;
			for (int dy = -POOL_HALF; dy <= POOL_HALF; dy++){
				for (int dx = 1; dx <= POOL_EAST; dx++){
					int cell = off( hero, dx, dy );
					if (cell != -1) Level.set( cell, Terrain.WATER );
				}
			}
			GameScene.updateMap();
			//the one to zap, then those only the water reaches from him, the bat it passes under,
			//and one to zap on dry ground
			hardy( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat.class, off( hero, 3, 0 ) );
			hardy( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat.class, off( hero, 6, -2 ) );
			hardy( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat.class, off( hero, 5, 3 ) );
			hardy( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat.class, off( hero, 6, 1 ) );
			hardy( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll.class, off( hero, -3, -2 ) );

			//the one this scene gave before, or another as strong
			WandOfLightning wand = null;
			for (WandOfLightning w : hero.belongings.getAllItems( WandOfLightning.class )){
				if (w.level() >= 3) wand = w;
			}
			if (wand == null){
				wand = new WandOfLightning();
				wand.upgrade( 3 );
				wand.identify();
				wand.collect( hero.belongings.backpack );
			}
			wand.curCharges = wand.maxCharges;
			if (!Dungeon.quickslot.contains( wand )){
				for (int s = 0; s < QuickSlot.SIZE; s++){
					if (Dungeon.quickslot.getItem( s ) == null){
						Dungeon.quickslot.setSlot( s, wand );
						break;
					}
				}
			}
			Item.updateQuickslot();
			GLog.i( "Scene: zap the rat in the pool: its arcs reach two cells through the water, then the charge runs"
					+ " three steps over it into the two rats further off (not the bat flying over it), every lightning"
					+ " in sight flashing and cracking as a storm's. Step into the pool and zap him again: the water"
					+ " carries it back into you, for half. Zap the gnoll on dry ground: no run. Run the scene again to"
					+ " recharge the wand." );
		}

		//the cell this far from the hero, -1 off the level's inner cells (the room is cut short by
		//the level's edge as well)
		private static int off( Hero hero, int dx, int dy ){
			int w = Dungeon.level.width(), x = hero.pos % w + dx, y = hero.pos / w + dy;
			if (x <= 0 || y <= 0 || x >= w - 1 || y >= Dungeon.level.height() - 1) return -1;
			return x + y * w;
		}

		//a monster that stands where it is put and takes a few zaps
		private static void hardy( Class<? extends Mob> kind, int cell ){
			if (cell == -1 || !Dungeon.level.passable[cell] || Actor.findChar( cell ) != null) return;
			Mob mob = spawn( kind, cell );
			if (mob == null) return;
			mob.HP = mob.HT = HARDY;
			mob.state = mob.PASSIVE;
		}
	}

	//in the blank room, a chasm three rows ahead of the hero with a bat, a wraith and a rat frozen on its lip: for
	//what falls, what shatters and what survives (Chasm.mobFall)
	private static final class Pit implements Scene {
		@Override public String id(){ return "pit-frozen"; }
		@Override public String title(){ return "Pit with frozen monsters on the edge"; }
		@Override public void apply( Hero hero ){
			if (!Room.ensure( this, hero )) return;
			Level level = Dungeon.level;
			int w = level.width();
			int row = hero.pos - 3 * w;
			if (row < w) return;
			for (int dx = -3; dx <= 3; dx++){
				int cell = row + dx;
				if (cell % w <= 0 || cell % w >= w - 1) continue;
				Level.set( cell, Terrain.CHASM );
				GameScene.updateMap( cell );
			}
			Class<?>[] kinds = {
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat.class,
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.Wraith.class,
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat.class };
			int[] at = { row + w - 2, row + w, row + w + 2 };
			for (int i = 0; i < kinds.length; i++){
				int cell = at[i];
				if (!level.passable[cell] || Actor.findChar( cell ) != null) continue;
				@SuppressWarnings("unchecked") Mob mob = spawn( (Class<? extends Mob>) kinds[i], cell );
				if (mob != null) Buff.prolong( mob, Frost.class, 300f );
			}
			for (int i : PathFinder.NEIGHBOURS9) level.mapped[hero.pos + i] = true;
		}
	}
}
