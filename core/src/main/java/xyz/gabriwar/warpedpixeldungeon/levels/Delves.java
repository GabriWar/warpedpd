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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ChampionEnemy;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MobSpawner;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.SteelHoneypot;
import xyz.gabriwar.warpedpixeldungeon.items.UpgradeBlobRed;
import xyz.gabriwar.warpedpixeldungeon.items.UpgradeBlobViolet;
import xyz.gabriwar.warpedpixeldungeon.items.UpgradeBlobYellow;
import xyz.gabriwar.warpedpixeldungeon.items.food.GoldenNut;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfExperience;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMight;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMuscle;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfMight;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfDivineInspiration;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfMastery;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfProtain;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicalInfusion;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMultiUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfAscension;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Musclemoss;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The world's other dungeons: sealed barrows on the overworld (WorldStructures.Site.DUNGEON)
 * that open only to a hero carrying the Amulet of Yendor.
 *
 * <h3>What one is</h3>
 *
 * A barrow's sector fixes everything about it, from the seed alone: its LEVEL (a base that grows
 * with the distance from the town, times a log-random factor from x0.25 to x3: two barrows side
 * by side can be worlds apart, and none is capped), how many FLOORS it runs ({@link #MIN_FLOORS}
 * to {@link #MAX_FLOORS}), the REGION its first floor is cut like (the level's own region up to
 * 25, any region past it) and its name. Its floors take the dungeon's own regular depth numbers
 * ({@link #DEPTHS}: the boss depths are skipped) from its region down, so the rooms, the tiles
 * and the mob rotation are the dungeon's and a long barrow runs on into the regions below; one
 * that would run past the halls starts higher up instead. They live on a branch of their own,
 * from {@link #BRANCH_BASE} up: one per barrow entered, handed out in order and kept with the run. The deepest floor has no way further down; a GUARDIAN holds it, a
 * champion of the region three times as hardy, and its death pays out the barrow's hoard and
 * marks it cleared for good: a flag flies over its stairs (tiles/DelveFlag) and its map dot
 * blinks. A cleared barrow can still be walked into; its floors are kept as they were left.
 *
 * <h3>Scaling</h3>
 *
 * A floor's EFFECTIVE depth is the barrow's level plus how deep the floor is in it. Up to 26
 * the region's own mobs are near the strength they have there, nudged up by what the floor is
 * short of its effective depth. Past 26 every enemy is first brought to the strength of the
 * dungeon's bottom (the Ascension challenge's per-mob factors, which measure exactly that gap)
 * and then grows with no cap: HP and damage by {@link #power}, accuracy and evasion more slowly
 * by {@link #skill}. Experience grows with the power and a barrow's enemies never stop paying
 * it, nor their loot, however far the hero has out-levelled them. Gold, the gear tiers and the
 * rarity rolls read the effective depth ({@link #lootDepth}).
 *
 * <h3>No permanent upgrades</h3>
 *
 * The main dungeon's guaranteed hero power (strength, upgrades, the stylus and the like) is
 * already gated to branch 0. Everything else that grows the hero for good is rerolled out of
 * the item generator and swapped for gold where it is dropped on a barrow's floor
 * ({@link #banned}).
 */
public final class Delves {

	private Delves(){}

	/** The first branch number a barrow gets; each one entered takes the next. */
	public static final int BRANCH_BASE = 100;
	/** The fewest and the most floors a barrow runs. */
	public static final int MIN_FLOORS = 4, MAX_FLOORS = 15;

	/** The depth numbers a barrow's floors take, in order: the dungeon's regular floors, never a
	 *  boss depth (5, 10, 15, 20) nor past 24, where the rooms' tables run out. */
	public static final int[] DEPTHS = { 1, 2, 3, 4, 6, 7, 8, 9, 11, 12, 13, 14, 16, 17, 18, 19, 21, 22, 23, 24 };

	// ------------------------------------------------------------- what a barrow is (pure)

	private static long hash( long seed, long x, long y ){
		long h = seed ^ 0xDE17E5L;
		h ^= x * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft( h, 31 );
		h ^= y * 0xC2B2AE3D27D4EB4FL;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 33;
		return h;
	}

	//the level's spread round its distance's base: a factor log-uniform from a quarter of the
	//base to three times it, so the easy ones and the deadly ones are both common
	private static final double LEVEL_LOW = 0.25, LEVEL_HIGH = 3.0;

	/** The level of the barrow in this sector: a base growing with the distance from the town,
	 *  times a log-random factor; at least 1, never capped. */
	public static int level( long seed, int sx, int sy ){
		int wx = WorldStructures.siteX( seed, sx, sy ), wy = WorldStructures.siteY( seed, sx, sy );
		double dist = Math.sqrt( (double)wx * wx + (double)wy * wy );
		double base = 3 + dist / 120;
		double u = (hash( seed ^ 0x1E7E1L, sx, sy ) >>> 11) / (double)(1L << 53);
		double factor = LEVEL_LOW * Math.pow( LEVEL_HIGH / LEVEL_LOW, u );
		return Math.max( 1, (int)Math.round( base * factor ) );
	}

	/** How many floors the barrow in this sector runs. */
	public static int floors( long seed, int sx, int sy ){
		return MIN_FLOORS + (int)Math.floorMod( hash( seed ^ 0xF100E5L, sx, sy ), MAX_FLOORS - MIN_FLOORS + 1 );
	}

	/** Where in DEPTHS a barrow of this region and length starts: its region's first floor, or
	 *  higher up when the run would go past the halls. */
	public static int startIndex( int region, int floors ){
		return Math.max( 0, Math.min( region * 4, DEPTHS.length - floors ) );
	}

	/** The region (0 sewers .. 4 halls) a barrow's floors are cut like. */
	public static int region( long seed, int sx, int sy ){
		int level = level( seed, sx, sy );
		if (level <= 25) return Math.min( 4, (level - 1) / 5 );
		return (int)Math.floorMod( hash( seed ^ 0x2E610L, sx, sy ) >>> 16, 5 );
	}

	private static final String[] NAME_A = { "Ash", "Black", "Bone", "Cold", "Dread", "Dusk", "Grey", "Grim",
			"Hollow", "Iron", "Mire", "Night", "Raven", "Rot", "Salt", "Shade", "Silent", "Thorn", "Wither", "Wolf" };
	private static final String[] NAME_B = { "barrow", "crypt", "deep", "delve", "hold", "pit", "vault",
			"warren", "maw", "undercroft" };

	/** The name of the barrow in this sector. */
	public static String name( long seed, int sx, int sy ){
		long h = hash( seed ^ 0x9A3E5L, sx, sy );
		String a = NAME_A[(int)Math.floorMod( h, NAME_A.length )];
		String b = NAME_B[(int)Math.floorMod( h >> 12, NAME_B.length )];
		return a + " " + Messages.titleCase( b );
	}

	// ------------------------------------------------------------- the barrows entered

	/** A barrow the hero has opened: its sector, the branch its floors are saved under, and
	 *  whether its guardian has fallen. */
	public static class Entry {
		public int sx, sy, branch, level, region, floors = MIN_FLOORS;
		public boolean cleared;

		int start(){
			return startIndex( region, floors );
		}

		/** The depth number of a floor (0 the first). */
		public int depthOf( int floor ){
			return DEPTHS[start() + floor];
		}

		/** The floor (0 the first) a depth number is, or -1 when it is none of this barrow's. */
		public int floorOf( int depth ){
			for (int f = 0; f < floors; f++) if (depthOf( f ) == depth) return f;
			return -1;
		}

		public int firstDepth(){
			return depthOf( 0 );
		}

		public int lastDepth(){
			return depthOf( floors - 1 );
		}
	}

	private static final ArrayList<Entry> entries = new ArrayList<>();

	public static void reset(){
		entries.clear();
		mainDoor = false;
	}

	//the stairs the hero last went down into the main dungeon by, when they were a ruin's
	//(OverworldLevel); none means the town's gate. Climbing out of floor 1 comes back up there
	private static boolean mainDoor = false;
	private static int mainDoorX, mainDoorY;

	/** The hero went down into the main dungeon by these world stairs (a ruin's). */
	public static void enteredMainDungeonAt( int wx, int wy ){
		mainDoor = true;
		mainDoorX = wx;
		mainDoorY = wy;
	}

	/** The hero went down into the main dungeon by the town's gate. */
	public static void enteredMainDungeonByTheGate(){
		mainDoor = false;
	}

	/** SewerLevel: the floor-1 climb lands back on the stairs the hero came down by. */
	public static void arriveAtMainDungeonDoor(){
		if (mainDoor){
			OverworldLevel.arriveAt( mainDoorX, mainDoorY );
		} else {
			OverworldLevel.arriveInTown( WorldStructures.TOWN_DUNGEON_GATE + 32 );
		}
	}

	/** The seed the world's sites are laid by (OverworldLevel.worldSeedOf the run's): a barrow's
	 *  stairs, name and level are the world's, never the run seed's own. */
	public static long worldSeed(){
		return OverworldLevel.worldSeedOf( Dungeon.seed );
	}

	public static boolean isDelve( int branch ){
		return branch >= BRANCH_BASE;
	}

	public static boolean inDelve(){
		return isDelve( Dungeon.branch );
	}

	/** The barrow of a branch, or null. */
	public static Entry byBranch( int branch ){
		for (Entry e : entries) if (e.branch == branch) return e;
		return null;
	}

	/** The barrow the hero is in, or null. */
	public static Entry current(){
		return inDelve() ? byBranch( Dungeon.branch ) : null;
	}

	/** The barrow of a sector, if the hero has opened it. */
	public static Entry bySector( int sx, int sy ){
		for (Entry e : entries) if (e.sx == sx && e.sy == sy) return e;
		return null;
	}

	public static boolean cleared( int sx, int sy ){
		Entry e = bySector( sx, sy );
		return e != null && e.cleared;
	}

	/** The barrow of a sector, given a branch the first time it is opened. */
	public static Entry open( long seed, int sx, int sy ){
		Entry e = bySector( sx, sy );
		if (e != null) return e;
		e = new Entry();
		e.sx = sx;
		e.sy = sy;
		e.level = level( seed, sx, sy );
		e.region = region( seed, sx, sy );
		e.floors = floors( seed, sx, sy );
		int branch = BRANCH_BASE;
		for (Entry o : entries) branch = Math.max( branch, o.branch + 1 );
		e.branch = branch;
		entries.add( e );
		return e;
	}

	/** Every barrow opened so far (the map greys out the cleared ones). */
	public static ArrayList<Entry> entries(){
		return entries;
	}

	private static final String SX = "delve_sx", SY = "delve_sy", BRANCHES = "delve_branch",
			LEVELS = "delve_level", REGIONS = "delve_region", CLEARED = "delve_cleared", FLOOR_COUNTS = "delve_floors",
			MAIN_DOOR = "main_door", MAIN_DOOR_X = "main_door_x", MAIN_DOOR_Y = "main_door_y";

	public static void storeInBundle( Bundle bundle ){
		int n = entries.size();
		int[] sx = new int[n], sy = new int[n], br = new int[n], lv = new int[n], rg = new int[n], fl = new int[n];
		boolean[] cl = new boolean[n];
		for (int i = 0; i < n; i++){
			Entry e = entries.get( i );
			sx[i] = e.sx; sy[i] = e.sy; br[i] = e.branch; lv[i] = e.level; rg[i] = e.region; cl[i] = e.cleared;
			fl[i] = e.floors;
		}
		bundle.put( FLOOR_COUNTS, fl );
		bundle.put( SX, sx );
		bundle.put( SY, sy );
		bundle.put( BRANCHES, br );
		bundle.put( LEVELS, lv );
		bundle.put( REGIONS, rg );
		bundle.put( CLEARED, cl );
		bundle.put( MAIN_DOOR, mainDoor );
		bundle.put( MAIN_DOOR_X, mainDoorX );
		bundle.put( MAIN_DOOR_Y, mainDoorY );
	}

	public static void restoreFromBundle( Bundle bundle ){
		entries.clear();
		mainDoor = bundle.getBoolean( MAIN_DOOR );
		mainDoorX = bundle.getInt( MAIN_DOOR_X );
		mainDoorY = bundle.getInt( MAIN_DOOR_Y );
		if (!bundle.contains( BRANCHES )) return;
		int[] sx = bundle.getIntArray( SX ), sy = bundle.getIntArray( SY ), br = bundle.getIntArray( BRANCHES ),
				lv = bundle.getIntArray( LEVELS ), rg = bundle.getIntArray( REGIONS );
		boolean[] cl = bundle.getBooleanArray( CLEARED );
		//barrows opened before they ran more than four floors keep their four
		int[] fl = bundle.contains( FLOOR_COUNTS ) ? bundle.getIntArray( FLOOR_COUNTS ) : null;
		for (int i = 0; i < br.length; i++){
			Entry e = new Entry();
			e.sx = sx[i]; e.sy = sy[i]; e.branch = br[i]; e.level = lv[i]; e.region = rg[i]; e.cleared = cl[i];
			e.floors = fl != null && i < fl.length ? fl[i] : MIN_FLOORS;
			entries.add( e );
		}
	}

	// ------------------------------------------------------------- floors

	/** The level class a barrow's floor is built from: its depth's region's. */
	public static Level newLevel( int branch, int depth ){
		Entry e = byBranch( branch );
		if (e == null || e.floorOf( depth ) < 0) return new DeadEndLevel();
		switch ((depth - 1) / 5){
			case 0: default: return new SewerLevel();
			case 1: return new PrisonLevel();
			case 2: return new CavesLevel();
			case 3: return new CityLevel();
			case 4: return new HallsLevel();
		}
	}

	/** A floor's seed: the run's seed mixed with the barrow's own branch and the floor. */
	public static long levelSeed( long seed, int branch, int depth ){
		return hash( seed ^ 0x5EEDL, branch, depth );
	}

	/** How many floors into its barrow the hero stands (0 for the first), or -1 outside one. */
	public static int floor(){
		Entry e = current();
		return e == null ? -1 : e.floorOf( Dungeon.depth );
	}

	/** The depth number of the floor below this one in the hero's barrow, or -1 at its bottom. */
	public static int below( int depth ){
		Entry e = current();
		if (e == null) return -1;
		int f = e.floorOf( depth );
		return f < 0 || f + 1 >= e.floors ? -1 : e.depthOf( f + 1 );
	}

	public static boolean lastFloor(){
		Entry e = current();
		return e != null && Dungeon.depth >= e.lastDepth();
	}

	/** The depth a barrow's floor counts as: its level plus how deep the floor is. */
	public static int effectiveDepth(){
		Entry e = current();
		return e == null ? Dungeon.depth : e.level + Math.max( 0, e.floorOf( Dungeon.depth ) );
	}

	/** The depth gold, gear tiers and rarity are rolled by: the effective depth in a barrow. */
	public static int lootDepth(){
		return inDelve() ? effectiveDepth() : Dungeon.depth;
	}

	// ------------------------------------------------------------- scaling

	//the bottom of the dungeon: past it the factor is the Ascension's per-mob gap and grows on
	private static final int BOTTOM = 26;

	private static boolean scaled( Char ch ){
		return inDelve() && ch instanceof Mob && ch.alignment == Char.Alignment.ENEMY;
	}

	/** The HP / damage / armour factor of an enemy in a barrow, 1 elsewhere. */
	public static float power( Char ch ){
		if (!scaled( ch )) return 1f;
		return powerAt( effectiveDepth(), Dungeon.depth, AscensionChallenge.baseModifier( ch ) );
	}

	/** The accuracy / evasion factor of an enemy in a barrow, 1 elsewhere. */
	public static float skill( Char ch ){
		if (!scaled( ch )) return 1f;
		return skillAt( effectiveDepth(), Dungeon.depth, AscensionChallenge.baseModifier( ch ) );
	}

	/** The power factor at an effective depth, for a mob native to depth d whose Ascension
	 *  factor (its gap to the dungeon's bottom) is gap. Pure, for the tests. */
	public static float powerAt( int effective, int d, float gap ){
		float near = 1f + 0.04f * Math.max( 0, effective - d );
		if (effective <= BOTTOM) return near;
		int x = effective - BOTTOM;
		return Math.max( near, gap * (1f + 0.10f * x + 0.002f * x * x) );
	}

	public static float skillAt( int effective, int d, float gap ){
		float near = 1f + 0.02f * Math.max( 0, effective - d );
		if (effective <= BOTTOM) return near;
		int x = effective - BOTTOM;
		return Math.max( near, gap * (1f + 0.03f * x) );
	}

	// ------------------------------------------------------------- no permanent upgrades

	private static final Set<Class<? extends Item>> BANNED = new HashSet<>( Arrays.asList(
			PotionOfStrength.class, PotionOfMight.class, PotionOfMuscle.class, PotionOfExperience.class,
			ElixirOfMight.class, PotionOfMastery.class, PotionOfDivineInspiration.class, PotionOfProtain.class,
			ScrollOfUpgrade.class, ScrollOfMagicalInfusion.class, ScrollOfMultiUpgrade.class, ScrollOfAscension.class,
			Musclemoss.Seed.class, GoldenNut.class, SteelHoneypot.class,
			UpgradeBlobRed.class, UpgradeBlobViolet.class, UpgradeBlobYellow.class ) );

	/** Is this item something that grows the hero for good, and so kept out of the barrows? */
	public static boolean banned( Item item ){
		return item != null && inDelve() && BANNED.contains( item.getClass() );
	}

	/** What a banned item dropped on a barrow's floor becomes: its worth in gold. */
	public static Item substitute( Item item ){
		return new Gold( Math.max( 20, item.value() * item.quantity() ) );
	}

	// ------------------------------------------------------------- in and out

	/** Fires a transition, unless it is a barrow's way out: that one returns the hero to the
	 *  barrow's door on the world. Every place a transition fires goes through here. */
	public static boolean fire( Hero hero, LevelTransition transition ){
		Entry e = current();
		if (e != null && Dungeon.depth == e.firstDepth()
				&& (transition.type == LevelTransition.Type.REGULAR_ENTRANCE
					|| transition.type == LevelTransition.Type.SURFACE)){
			leave( e );
			return true;
		}
		return Dungeon.level.activateTransition( hero, transition );
	}

	/** The world cell of a barrow's stairway, where leaving it lands the hero. */
	public static int[] stairs( Entry e ){
		return new int[]{ WorldStructures.siteX( worldSeed(), e.sx, e.sy ), WorldStructures.siteY( worldSeed(), e.sx, e.sy ) };
	}

	private static void leave( Entry e ){
		Level.beforeTransition();
		//back out onto the barrow's own stairway
		int[] stairs = stairs( e );
		OverworldLevel.arriveAt( stairs[0], stairs[1] );
		OverworldLevel.travelToSurface();
	}

	/** Called on every floor of a barrow once it is built. Its stairs lead to the floors above and
	 *  below it in the barrow (the depth numbers skip the boss depths, so depth +- 1 would not);
	 *  the deepest loses its way down and gains its guardian. */
	public static void afterCreate( Level level ){
		Entry e = current();
		if (e == null) return;
		int f = e.floorOf( Dungeon.depth );
		boolean last = f == e.floors - 1;
		int guardAt = -1;
		for (LevelTransition t : level.transitions.toArray( new LevelTransition[0] )){
			boolean down = t.type == LevelTransition.Type.REGULAR_EXIT || t.type == LevelTransition.Type.BRANCH_EXIT;
			if (down && last){
				guardAt = t.cell();
				level.map[t.cell()] = Terrain.EMPTY_SP;
				level.transitions.remove( t );
			} else if (down){
				t.destDepth = e.depthOf( f + 1 );
				t.destBranch = e.branch;
			} else if (f > 0){
				//the way up (the top floor's is the barrow's door: Delves.fire)
				t.destDepth = e.depthOf( f - 1 );
				t.destBranch = e.branch;
			}
		}
		if (!last) return;
		//a cleared barrow's floor built again (a beacon set in it) has no guardian left to hold it
		if (guardAt == -1 || e.cleared) return;

		ArrayList<Class<? extends Mob>> rotation = MobSpawner.getMobRotation( Dungeon.depth );
		Mob guardian = Reflection.newInstance( rotation.get( rotation.size() - 1 ) );
		if (guardian == null) return;
		guardian.HT = guardian.HP = guardian.HT * 3;
		guardian.pos = guardAt;
		guardian.state = guardian.WANDERING;
		Buff.affect( guardian, Guardian.class );
		Buff.affect( guardian, CHAMPIONS[Random.Int( CHAMPIONS.length )] );
		level.mobs.add( guardian );
	}

	@SuppressWarnings("unchecked")
	private static final Class<? extends ChampionEnemy>[] CHAMPIONS = new Class[]{
			ChampionEnemy.Blazing.class, ChampionEnemy.Projecting.class, ChampionEnemy.AntiMagic.class,
			ChampionEnemy.Giant.class, ChampionEnemy.Blessed.class, ChampionEnemy.Growing.class };

	/** Marks a barrow's guardian. */
	public static class Guardian extends Buff {
		{
			revivePersists = true;
		}
	}

	/** Mob.die: a guardian's death pays out the hoard and seals its barrow as cleared. */
	public static void onDeath( Mob mob ){
		Entry e = current();
		if (e == null || mob.buff( Guardian.class ) == null || e.cleared) return;
		e.cleared = true;
		int depth = effectiveDepth();

		Heap heap = Dungeon.level.drop( new Gold( Random.IntRange( 100 * depth, 200 * depth ) ), mob.pos );
		for (Item item : hoard( e.level, depth )){
			Dungeon.level.drop( item, mob.pos );
		}
		heap.type = Heap.Type.CHEST;
		if (heap.sprite != null) heap.sprite.view( heap ).place( heap.pos );
		GLog.p( Messages.get( Delves.class, "cleared", name( worldSeed(), e.sx, e.sy ) ) );
	}

	private static final Generator.Category[] HOARD = { Generator.Category.WEAPON, Generator.Category.ARMOR,
			Generator.Category.WAND, Generator.Category.RING, Generator.Category.ARTIFACT };

	/** A guardian's hoard: more pieces and more upgrades the higher the barrow. */
	static ArrayList<Item> hoard( int level, int depth ){
		ArrayList<Item> items = new ArrayList<>();
		int pieces = 2 + Math.min( 6, level / 10 );
		int bonus = depth / 10;
		for (int i = 0; i < pieces; i++){
			Item item = Generator.random( HOARD[Random.Int( HOARD.length )] );
			if (item == null) continue;
			item.cursed = false;
			if (item.isUpgradable() && bonus > 0) item.upgrade( bonus );
			items.add( item );
		}
		int cores = depth / 20;
		if (cores > 0) items.add( new MasterworkCore().quantity( cores ) );
		return items;
	}
}
