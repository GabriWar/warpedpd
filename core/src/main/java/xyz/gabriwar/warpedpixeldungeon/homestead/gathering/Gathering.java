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

package xyz.gabriwar.warpedpixeldungeon.homestead.gathering;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.StoreSection;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Charcoal;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Clay;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.PineLog;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Resin;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Sand;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Axe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.BattleAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greataxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HandAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MasonsHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shovel;
import xyz.gabriwar.warpedpixeldungeon.items.treeseeds.Acorn;
import xyz.gabriwar.warpedpixeldungeon.items.treeseeds.BushSeed;
import xyz.gabriwar.warpedpixeldungeon.items.treeseeds.PineCone;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WoodcuttersAxe;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.HighGrass;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.HashMap;
import java.util.HashSet;

/**
 * The hero working the land of the world's slices for building materials: FELLING a tree with an
 * axe (logs, a pine's resin; a stump left to grow back, GatheringSlice), QUARRYING a boulder with
 * a pick or a mason's hammer (rough stone), DIGGING with a shovel (tilling grass into farmland,
 * clay from a swamp's or a river's bank, sand from a beach or a desert, a stump or a sapling dug
 * out) and REAPING tall grass with a sickle (thatch). The rock walls the pick already breaks give
 * rough stone too (wallStone), and the mason's hammer breaks them, slower (Hero.actMine).
 *
 * A job is a HeroAction.Work: one stroke a turn (Hero.actWork), each its sound and its chips, the
 * work done on the last. The hero's own tap on a tree or a boulder beside him starts one (tapJob;
 * a tap on one farther off still walks, so a forest is not felled by a stray tap); the shovel and
 * the sickle start theirs from the pack (begin), walking up to the cell first. A monster coming
 * into view or a blow stops the work as it stops a walk, and the strokes already struck are kept:
 * the next tap on the same tree takes up where they left off.
 *
 * Only on the world's slices, and never on what is not the hero's to take: a protected cell
 * (OverworldLevel.protectedForBuilding: the town, the stairs, the villages' houses and fields,
 * the places of the world), nor a tree within a village's bounds. Felling and quarrying run on
 * the host for any hero, as the pick's mining does; the shovel's and the sickle's work, started
 * from a pack, is the host's own hero's alone.
 */
public final class Gathering {

	private Gathering(){}

	/** What the hero is put to on a cell. */
	public enum Job { CHOP, QUARRY, DIG, REAP }

	/** What digging a cell gives (dig). */
	public enum Dig { STUMP, SAPLING, CLAY, SAND, TILL }

	/** The turns each axe takes to fell a tree. */
	public static final int HAND_AXE_TURNS = 8, AXE_TURNS = 6, BATTLE_AXE_TURNS = 5,
			GREATAXE_TURNS = 4, WOODCUTTERS_AXE_TURNS = 3;
	/** The turns a boulder takes: the pick, the mason's hammer. */
	public static final int PICK_QUARRY_TURNS = 3, HAMMER_QUARRY_TURNS = 2;
	/** The turns the mason's hammer takes on a rock wall (the pick's blow is one). */
	public static final int HAMMER_WALL_TURNS = 3;
	/** The shovel's turns: a spadeful of ground, a stump dug out by its roots. */
	public static final int DIG_TURNS = 2, STUMP_TURNS = 3;
	/** The sickle's turn through the tall grass. */
	public static final int REAP_TURNS = 1;
	/** The hunger the work costs on top of its turns: a tree felled, a boulder broken (as the
	 *  pick's blow on a rock wall). */
	public static final int CHOP_HUNGER = 4, QUARRY_HUNGER = 3;
	/** How far an axe's strokes carry: every monster within this many cells wakes and comes. */
	public static final int NOISE_RANGE = 8;
	/** A pine's resin, a seed turned up in tilled grass. */
	public static final float RESIN_CHANCE = 0.3f, SEED_CHANCE = 0.1f;
	/** A felled tree's acorn or pine cone, a cleared bush's seed (items/treeseeds). */
	public static final float TREE_SEED_CHANCE = 0.5f, BUSH_SEED_CHANCE = 0.5f;

	// ----------------------------------------------------------------- the tools

	/** The turns an item takes to fell a tree, 0 when it is no axe. */
	public static int axeTurns( Item item ){
		if (item == null) return 0;
		Class<?> c = item.getClass();
		if (c == WoodcuttersAxe.class) return WOODCUTTERS_AXE_TURNS;
		if (c == Greataxe.class) return GREATAXE_TURNS;
		if (c == BattleAxe.class) return BATTLE_AXE_TURNS;
		if (c == Axe.class) return AXE_TURNS;
		if (c == HandAxe.class) return HAND_AXE_TURNS;
		return 0;
	}

	/** The axe the hero fells with: the weapon in his hand when it is an axe, else the quickest
	 *  he carries; null when he has none. */
	public static Item axe( Hero hero ){
		Item held = hero.belongings.weapon();
		if (axeTurns( held ) > 0) return held;
		Item best = null;
		for (Item i : hero.belongings.getAllItems( Item.class )){
			int t = axeTurns( i );
			if (t > 0 && (best == null || t < axeTurns( best ))) best = i;
		}
		return best;
	}

	/** What the hero breaks a boulder with: the mason's hammer, else the pick; null for neither. */
	public static Item quarryTool( Hero hero ){
		Item hammer = hero.belongings.getItem( MasonsHammer.class );
		return hammer != null ? hammer : hero.belongings.getItem( Pickaxe.class );
	}

	/** The turns a boulder takes with a tool (quarryTool). */
	public static int quarryTurns( Item tool ){
		return tool instanceof MasonsHammer ? HAMMER_QUARRY_TURNS : PICK_QUARRY_TURNS;
	}

	public static boolean tree( int t ){
		return t == Terrain.TREE_OAK || t == Terrain.TREE_PINE;
	}

	/** What an axe works: a tree to fell, a bush to clear, a stump to chop out. */
	public static boolean choppable( int t ){
		return tree( t ) || t == Terrain.SHRUB || BuiltTerrain.stump( t );
	}

	/** The turns a bush or a stump takes with an axe: half a tree's, rounded up. */
	public static int lightChop( int axeTurns ){
		return (axeTurns + 1) / 2;
	}

	// ----------------------------------------------------------------- starting work

	/**
	 * What tapping a cell beside the hero puts him to, null for nothing: a tree with an axe to
	 * hand, a boulder with a pick or a mason's hammer. Only beside him: a tap on one farther off
	 * is still a walk (Hero.handle).
	 */
	public static Job tapJob( Hero hero, int cell ){
		Level level = Dungeon.level;
		if (!(level instanceof OverworldLevel) || cell < 0 || cell >= level.length() || !level.adjacent( hero.pos, cell )) return null;
		int t = level.map[cell];
		if (choppable( t ) && axe( hero ) != null) return Job.CHOP;
		if (t == Terrain.BOULDER && quarryTool( hero ) != null) return Job.QUARRY;
		return null;
	}

	/** Whether the hero stands where he works the cell: beside a tree or a boulder, on or beside
	 *  the ground he digs or reaps. */
	public static boolean inReach( Hero hero, int cell, Job job ){
		int d = Dungeon.level.distance( hero.pos, cell );
		return job == Job.DIG || job == Job.REAP ? d <= 1 : d == 1;
	}

	/**
	 * The shovel's dig or the sickle's reap, from the pack, on the cell picked: told at once why
	 * not when there is nothing to do there, else he walks up to it if he must and works it
	 * (HeroAction.Work). A co-op guest is told it is the host's work.
	 */
	public static void begin( Hero hero, Integer cell, Job job ){
		if (cell == null || !(Dungeon.level instanceof OverworldLevel) || cell < 0 || cell >= Dungeon.level.length()) return;
		if (!mayWork( hero )) return;
		String no = refusal( hero, cell, job );
		if (no != null){
			if (!no.isEmpty()) tell( hero, no );
			return;
		}
		hero.curAction = new HeroAction.Work( cell, job );
		hero.next();
	}

	/** Whether this hero may work the land from his pack here: the host's own hero on his own
	 *  slice; a co-op guest is told it is the host's work. */
	public static boolean mayWork( Hero hero ){
		if (Dungeon.level instanceof OverworldLevel && !((OverworldLevel) Dungeon.level).isMirror() && hero == Dungeon.hero) return true;
		tell( hero, Messages.get( Gathering.class, "guest" ) );
		return false;
	}

	// ----------------------------------------------------------------- the work

	/** What follows a stroke: Hero.actWork spends its turn, and readies the hero when the job is done. */
	public interface Stroked {
		void after( boolean done );
	}

	//the strokes a hero has struck at a job: kept when he is called away, so the next tap on the
	//same tree carries on. by hero, on a world cell of a slice; never saved
	private static final class Progress {
		final int depth;
		final long key;
		final Job job;
		int strokes;

		Progress( int depth, long key, Job job ){
			this.depth = depth;
			this.key = key;
			this.job = job;
		}
	}
	private static final HashMap<Integer, Progress> progress = new HashMap<>();

	private static Progress progress( Hero hero, OverworldLevel ow, int cell, Job job ){
		long key = OverworldLevel.worldKey( ow.worldX + cell % ow.width(), ow.worldY + cell / ow.width() );
		Progress p = progress.get( hero.id() );
		if (p == null || p.depth != Dungeon.depth || p.key != key || p.job != job){
			p = new Progress( Dungeon.depth, key, job );
			progress.put( hero.id(), p );
		}
		return p;
	}

	/** The strokes the hero has already struck at this job on this cell (0 for none). */
	public static int strokesAt( Hero hero, int cell, Job job ){
		Progress p = progress.get( hero.id() );
		if (p == null || !(Dungeon.level instanceof OverworldLevel)) return 0;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		long key = OverworldLevel.worldKey( ow.worldX + cell % ow.width(), ow.worldY + cell / ow.width() );
		return p.depth == Dungeon.depth && p.key == key && p.job == job ? p.strokes : 0;
	}

	/** The turns a job takes the hero on a cell, with the tools he carries now. */
	public static int turns( Hero hero, int cell, Job job ){
		switch (job){
			case CHOP:   return tree( Dungeon.level.map[cell] ) ? axeTurns( axe( hero ) ) : lightChop( axeTurns( axe( hero ) ) );
			case QUARRY: return quarryTurns( quarryTool( hero ) );
			case DIG:    return dig( (OverworldLevel) Dungeon.level, cell ) == Dig.STUMP ? STUMP_TURNS : DIG_TURNS;
			case REAP: default:
				return REAP_TURNS;
		}
	}

	/**
	 * One stroke at a job, the hero beside the cell (inReach): the swing or the spadeful, its sound
	 * and its chips, the noise an axe makes, and on the last stroke the work done (the tree down,
	 * the boulder broken, the ground dug, the grass cut). `after` runs once the stroke has landed.
	 * False, nothing done, when the job cannot be done there now: the hero is told why (nothing
	 * said when the thing worked on is simply gone).
	 */
	public static boolean stroke( final Hero hero, final int cell, final Job job, final Stroked after ){
		String no = refusal( hero, cell, job );
		if (no != null){
			if (!no.isEmpty()) tell( hero, no );
			return false;
		}
		final OverworldLevel ow = (OverworldLevel) Dungeon.level;
		final int turns = turns( hero, cell, job );
		final Progress p = progress( hero, ow, cell, job );
		Callback blow = new Callback() {
			@Override
			public void call() {
				p.strokes++;
				boolean done = p.strokes >= turns;
				struck( hero, ow, cell, job );
				if (done){
					if (progress.get( hero.id() ) == p) progress.remove( hero.id() );
					finish( hero, ow, cell, job );
				}
				after.after( done );
			}
		};
		if (hero.sprite == null){
			blow.call();
		} else if (job == Job.CHOP || job == Job.QUARRY){
			hero.sprite.attack( cell, blow );
		} else {
			hero.sprite.operate( cell, blow );
		}
		return true;
	}

	/** Why the job cannot be done on the cell now: null when it can, "" when there is simply
	 *  nothing there any more (nothing to say), else what the hero is told. */
	public static String refusal( Hero hero, int cell, Job job ){
		if (!(Dungeon.level instanceof OverworldLevel) || cell < 0 || cell >= Dungeon.level.length()) return "";
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		if (ow.isMirror()) return "";
		int t = ow.map[cell];
		switch (job){
			case CHOP:
				if (!choppable( t )) return "";
				if (ow.protectedForBuilding( cell )) return Messages.get( Gathering.class, "protected_chop" );
				if (ow.altitude() == 0 && OverworldFauna.nearSettlement( ow.worldSeed(),
						ow.worldX + cell % ow.width(), ow.worldY + cell / ow.width() )){
					return Messages.get( Gathering.class, "villagers" );
				}
				if (axe( hero ) == null) return Messages.get( Gathering.class, "no_axe" );
				return null;
			case QUARRY:
				if (t != Terrain.BOULDER) return "";
				if (ow.protectedForBuilding( cell )) return Messages.get( Gathering.class, "protected_quarry" );
				if (quarryTool( hero ) == null) return Messages.get( Gathering.class, "no_pick" );
				return null;
			case DIG: {
				if (hero.belongings.getItem( Shovel.class ) == null) return Messages.get( Gathering.class, "no_shovel" );
				if (ow.protectedForBuilding( cell )) return Messages.get( Gathering.class, "protected_dig" );
				Dig d = dig( ow, cell );
				if (d == null) return Messages.get( Gathering.class, "nothing_to_dig" );
				if (d != Dig.STUMP && d != Dig.SAPLING && ow.frozenAt( cell )) return Messages.get( Gathering.class, "frozen" );
				return null;
			}
			case REAP: default:
				if (hero.belongings.getItem( Sickle.class ) == null) return Messages.get( Gathering.class, "no_sickle" );
				if (t != Terrain.HIGH_GRASS && t != Terrain.FURROWED_GRASS) return Messages.get( Gathering.class, "nothing_to_reap" );
				if (ow.protectedForBuilding( cell )) return Messages.get( Gathering.class, "protected_reap" );
				return null;
		}
	}

	//a line for the working hero alone (a co-op guest's on his own screen)
	private static void tell( Hero hero, String text ){
		NetManager.heroLog( hero, GLog.WARNING + text );
	}

	/**
	 * What the shovel turns up on a cell of a slice, null for nothing: a stump or a sapling dug out;
	 * clay from the bank of a swamp's or a river's water (natural ground beside it); sand from a
	 * beach or a desert; else plain grass or bare earth tilled into farmland.
	 */
	public static Dig dig( OverworldLevel ow, int cell ){
		int t = ow.map[cell];
		if (BuiltTerrain.stump( t )) return Dig.STUMP;
		if (BuiltTerrain.sapling( t )) return Dig.SAPLING;
		if (t == Terrain.EMPTY_SP && ow.altitude() == 0){
			WorldModel.Biome b = ow.biomeAtCell( cell );
			return b == WorldModel.Biome.BEACH || b == WorldModel.Biome.DESERT ? Dig.SAND : null;
		}
		if (!earth( t )) return null;
		return bank( ow, cell ) ? Dig.CLAY : Dig.TILL;
	}

	/** Natural ground a spade goes into, and a seed is planted in: grass, tall grass, bare earth. */
	public static boolean earth( int t ){
		return t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS || t == Terrain.EMPTY;
	}

	private static boolean grass( int t ){
		return t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS;
	}

	//the muddy bank of a swamp's or a river's water on the surface: water beside the cell, the
	//cell in a swamp or the water a river's
	private static boolean bank( OverworldLevel ow, int cell ){
		if (ow.altitude() != 0) return false;
		boolean swamp = false;
		for (int n : PathFinder.NEIGHBOURS8){
			int m = cell + n;
			if (m < 0 || m >= ow.length()) continue;
			int t = ow.map[m];
			if (t != Terrain.WATER && t != Terrain.DEEP_WATER) continue;
			if (ow.biomeAtCell( m ) == WorldModel.Biome.RIVER) return true;
			swamp = true;
		}
		return swamp && ow.biomeAtCell( cell ) == WorldModel.Biome.SWAMP;
	}

	// ----------------------------------------------------------------- each stroke

	//the stroke landing: its sound and its chips (on every screen: GatheringFx), and an axe's noise
	private static void struck( Hero hero, OverworldLevel ow, int cell, Job job ){
		if (job == Job.CHOP) noise( ow, cell );
		int kind;
		switch (job){
			case CHOP:   kind = GatheringFx.CHOP; break;
			case QUARRY: kind = GatheringFx.QUARRY; break;
			case DIG:    kind = GatheringFx.DIG; break;
			case REAP: default:
				kind = GatheringFx.REAP;
		}
		GatheringFx.show( ow, kind, cell, 0, 0 );
	}

	/** An axe ringing on a trunk: every monster within NOISE_RANGE wakes and comes to see. */
	public static void noise( Level level, int cell ){
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			if (m.alignment == Char.Alignment.ENEMY && m.state != m.HUNTING
					&& level.distance( m.pos, cell ) <= NOISE_RANGE){
				m.beckon( cell );
			}
		}
	}

	// ----------------------------------------------------------------- the work done

	private static void finish( Hero hero, OverworldLevel ow, int cell, Job job ){
		switch (job){
			case CHOP: {
				int t = ow.map[cell];
				if (tree( t ))              fell( hero, ow, cell );
				else if (t == Terrain.SHRUB) clearBush( hero, ow, cell );
				else                        uproot( ow, cell );
				break;
			}
			case QUARRY: quarry( hero, ow, cell ); break;
			case DIG:    dug( ow, cell ); break;
			case REAP:   reap( ow, cell ); break;
		}
	}

	/**
	 * The tree comes down, away from the hero: its stump left on the cell to grow back
	 * (GatheringSlice), whoever stood where it fell hurt (TreeFall), the logs - oak 3-4, pine 2-3
	 * and resin one time in three or so - in a heap at the stump. Hungry work.
	 */
	static void fell( Hero hero, OverworldLevel ow, int cell ){
		int tree = ow.map[cell];
		Level.set( cell, BuiltTerrain.stumpOf( tree ), ow );
		GatheringSlice slice = ow.homestead().section( GatheringSlice.class );
		if (slice != null) slice.felled( ow, cell, WorldClock.day() );
		changed( ow, cell );
		TreeFall.fall( ow, cell, cell + (cell - hero.pos) );
		if (tree == Terrain.TREE_OAK){
			give( ow, cell, new Log().quantity( Random.IntRange( 3, 4 ) ) );
			if (Random.Float() < TREE_SEED_CHANCE) give( ow, cell, new Acorn() );
		} else {
			give( ow, cell, new PineLog().quantity( Random.IntRange( 2, 3 ) ) );
			if (Random.Float() < RESIN_CHANCE) give( ow, cell, new Resin() );
			if (Random.Float() < TREE_SEED_CHANCE) give( ow, cell, new PineCone() );
		}
		hunger( hero, CHOP_HUNGER );
	}

	/** A bush cut down to the ground: grass where it stood, and its seed one time in two. */
	static void clearBush( Hero hero, OverworldLevel ow, int cell ){
		Level.set( cell, Terrain.GRASS, ow );
		changed( ow, cell );
		GatheringFx.show( ow, GatheringFx.CHOP, cell, 0, 0 );
		if (Random.Float() < BUSH_SEED_CHANCE) give( ow, cell, new BushSeed() );
	}

	/** A stump (or a young tree or bush) taken out by its roots: grass in its place, and growing
	 *  back no more; a stump's roots are one last log. The axe's work and the shovel's alike. */
	static void uproot( OverworldLevel ow, int cell ){
		int t = ow.map[cell];
		//grass, whatever the season: frozen ground shows it as snow (WindowGenerator.visualGround),
		//where snow itself, kept as the player's edit, would lie on it all summer
		Level.set( cell, Terrain.GRASS, ow );
		GatheringSlice slice = ow.homestead().section( GatheringSlice.class );
		if (slice != null) slice.forget( ow, cell );
		changed( ow, cell );
		if (BuiltTerrain.stump( t )) give( ow, cell, t == Terrain.STUMP_OAK ? new Log() : new PineLog() );
	}

	/** The boulder breaks into 2-3 rough stone, the ground broken where it stood; in a cave the
	 *  blow may bring the roof down, as the pick's on the rock does (HazardWatch). */
	static void quarry( Hero hero, OverworldLevel ow, int cell ){
		Level.set( cell, Terrain.EMPTY_DECO, ow );
		changed( ow, cell );
		ow.hazards().mined( hero, cell, Terrain.BOULDER );
		GatheringFx.show( ow, GatheringFx.BOULDER_BREAKS, cell, 0, 0 );
		give( ow, cell, new RoughStone().quantity( Random.IntRange( 2, 3 ) ) );
		hunger( hero, QUARRY_HUNGER );
	}

	/** The shovel's work done (dig): a stump's log, the clay, the sand or the furrow. */
	static void dug( OverworldLevel ow, int cell ){
		Dig what = dig( ow, cell );
		if (what == null) return;
		int t = ow.map[cell];
		switch (what){
			case STUMP:
			case SAPLING:
				//the roots of a stump are a last log; a sapling is too young to give one
				uproot( ow, cell );
				break;
			case CLAY:
				give( ow, cell, new Clay() );
				break;
			case SAND:
				give( ow, cell, new Sand() );
				break;
			case TILL:
				Level.set( cell, Terrain.FARMLAND, ow );
				changed( ow, cell );
				//the guide's page on the fields (the shovel is the host's own hero's)
				xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.reveal( xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.TAG_FARM );
				//grass turned over can bring up a seed, as the shovel's digging always could
				if (grass( t ) && Random.Float() < SEED_CHANCE){
					give( ow, cell, Generator.randomUsingDefaults( Generator.Category.SEED ) );
				}
				break;
		}
	}

	/** A tree burnt down to embers leaves 1-2 charcoal in its ashes (OverworldLevel.destroy). */
	public static void charred( OverworldLevel ow, int cell ){
		give( ow, cell, new Charcoal().quantity( Random.IntRange( 1, 2 ) ) );
	}

	/** The tall grass is cut: 1-2 thatch, and whatever the grass gives up when it is trampled. */
	static void reap( OverworldLevel ow, int cell ){
		HighGrass.trample( ow, cell );
		give( ow, cell, new Thatch().quantity( Random.IntRange( 1, 2 ) ) );
	}

	/** The rough stone a rock wall gives up as the pick or the hammer breaks it on a slice: 1-2,
	 *  on the broken ground. Not from the walls of a village or a place of the world, which are
	 *  their masonry and not the land's rock. Called by Hero.actMine once the wall is down. */
	public static void wallStone( OverworldLevel ow, int cell ){
		if (ow.isMirror() || ow.protectedForBuilding( cell )) return;
		give( ow, cell, new RoughStone().quantity( Random.IntRange( 1, 2 ) ) );
	}

	//a heap of what the work gave on the cell (the nearest free one when a shop's ware lies there)
	private static void give( OverworldLevel ow, int cell, Item item ){
		if (item == null) return;
		Heap h = ow.drop( item, RaidEvent.dropCell( ow, cell ) );
		if (h.sprite != null) h.sprite.drop();
	}

	private static void hunger( Hero hero, int amount ){
		Hunger h = hero.buff( Hunger.class );
		if (h != null) h.affectHunger( -amount );
	}

	/**
	 * Cells changed by the work or by growing back: redrawn (the tree's crown and trunk gone, the
	 * furrow laid) and the homesteads' parts told, as a change through the Homestead facade tells
	 * them (Homestead.refresh). Many at once are redrawn as one: a single pass of the world's
	 * dressing (OverworldLevel.redressCells) and a single fresh look, not one each.
	 */
	public static void changed( OverworldLevel ow, int... cells ){
		if (cells.length == 1){
			Homestead.refresh( ow, cells[0] );
		} else if (cells.length > 1){
			for (int c : cells) ow.cleanWalls( c );
			if (live( ow )){
				ow.redressCells( cells );
				HashSet<Integer> drawn = new HashSet<>();
				int w = ow.width();
				for (int c : cells){
					for (int y = Math.max( 0, c / w - 1 ); y <= Math.min( ow.height() - 1, c / w + 1 ); y++){
						for (int x = Math.max( 0, c % w - 1 ); x <= Math.min( w - 1, c % w + 1 ); x++){
							if (drawn.add( x + y * w )) GameScene.updateMap( x + y * w );
						}
					}
				}
				if (Dungeon.hero != null) Dungeon.observe();
			}
		}
		for (int c : cells){
			for (StoreSection s : ow.homestead().sections()) s.cellChanged( ow, c );
		}
	}

	static boolean live( Level level ){
		return level == Dungeon.level && Game.scene() instanceof GameScene;
	}
}
