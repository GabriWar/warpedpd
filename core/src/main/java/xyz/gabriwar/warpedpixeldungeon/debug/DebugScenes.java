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

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.WarmthBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler;
import xyz.gabriwar.warpedpixeldungeon.items.Torch;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFireblast;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
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
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

/**
 * Debug scenes: a named situation set up around the hero in one tap, for testing
 * something specific without walking the dungeon for it. Applied from the debug
 * menu (Mobs tab, "Scenes") on the current run, or straight from the command line
 * on a fresh run:
 *
 *   ./gradlew :desktop:debug -Pscene=fliers-paralysed
 *
 * which starts a new game, lands on the surface and applies the scene there. Every
 * scene keeps the hero safe (infinite health) and lifts the fog.
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
 */
public final class DebugScenes {

	private DebugScenes(){}

	public interface Scene {
		String id();
		String title();
		void apply( Hero hero );
	}

	//all the scenes there are, in menu order
	public static final Scene[] SCENES = {
			new Room(),
			new Fliers( "fliers-paralysed", "All fliers, paralysed", Paralysis.class ),
			new Fliers( "fliers-frozen", "All fliers, frozen", Frost.class ),
			new Fliers( "fliers-free", "All fliers, free", null ),
			new Pit(),
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
	};

	public static Scene byId( String id ){
		for (Scene s : SCENES) if (s.id().equals( id )) return s;
		return null;
	}

	/** Runs a scene on the live game: safe hero, no fog, then the scene's own setup. */
	public static void run( Scene scene ){
		Hero hero = Dungeon.hero;
		if (hero == null || Dungeon.level == null) return;
		hero.migrateDebugGodmode();
		hero.debugInfiniteHealth = true;
		hero.HP = hero.HT;
		Dungeon.debugNoFog = true;
		scene.apply( hero );
		Dungeon.observe();
		GameScene.updateFog();
		GLog.p( "Scene: " + scene.title() );
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
	private static final class Room implements Scene {
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
