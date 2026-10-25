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

package xyz.gabriwar.warpedpixeldungeon.homestead.mounts;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Horse;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.homestead.Shelter;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Leashed;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Roping;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.misc.Saddle;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideProgress;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.sea.Seafaring;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

/**
 * Riding a horse of the hero's (actors/mobs/Horse, Riding). A tap on his saddled horse beside him
 * gets him on it (a held tap shows its menu instead: saddle, feed, rope, set free); a tap on
 * himself gets him down, the horse stepping out beside him to wait. Only on the riding slices:
 * the surface and the mountains above it (ridingSlice), out of doors (indoors).
 *
 * On horseback he goes RIDE_SPEED times as fast (Hero.speed), so every cell costs him that much
 * less time and so that much less hunger. A horse never goes over a drop (stepAllowed). At a
 * doorway, a roof or a room of a home he gets down and the horse waits on the cell he left
 * (getsDownBefore, stepped); going to any level that is no riding slice (a cave, the dungeon, a
 * barrow, the town's buildings) he gets down at the threshold and it waits there (leavingLevel);
 * to another riding slice (the steps up a mountain, a waystone) it goes with him.
 *
 * Combat, one rule: ATTACKING DISMOUNTS. A blow he strikes in melee from the saddle has him jump
 * down first, his horse stepping aside (beforeStrike); shooting, throwing and zapping work from
 * the saddle. The horse has its health and is hurt: while he rides it takes a third of every
 * blow that lands on him (shareBlow); if it runs out it falls dead and he lands on foot.
 *
 * Everything runs on the host, for the host's hero: a co-op guest is told it is the host's to do.
 */
public final class Mounts {

	private Mounts(){}

	/** How many times as fast a rider goes; a rider's horse takes one part in HORSE_SHARE of a blow. */
	public static final float RIDE_SPEED = 2f;
	public static final int HORSE_SHARE = 3;
	/** The guide's page on horses. */
	public static final String PAGE = "hs_horses";

	// ----------------------------------------------------------------- where

	/** A level a horse is ridden on: the surface and the mountains over it. */
	public static boolean ridingSlice( Level level ){
		return level instanceof OverworldLevel && ((OverworldLevel) level).altitude() >= 0;
	}

	/** Indoors on a cell: a doorway (a village house's, a hut's, a home's), or under a roof or in a
	 *  room of a home (Shelter.covered). */
	public static boolean indoors( Level level, int cell ){
		if (level == null || cell < 0 || cell >= level.length()) return false;
		int t = level.map[cell];
		if (t == Terrain.DOOR || t == Terrain.OPEN_DOOR || t == Terrain.LOCKED_DOOR || BuiltTerrain.door( t )) return true;
		return level instanceof OverworldLevel && Shelter.covered( level, cell );
	}

	/** May a horse be ridden on a cell: a riding slice, out of doors. */
	public static boolean rideable( Level level, int cell ){
		return ridingSlice( level ) && !indoors( level, cell );
	}

	// ----------------------------------------------------------------- who rides

	/** The hero's ride, null when he is on foot. */
	public static Riding riding( Char ch ){
		Riding r = ch == null ? null : ch.buff( Riding.class );
		return r != null && r.horse() != null ? r : null;
	}

	public static boolean rides( Char ch ){
		return riding( ch ) != null;
	}

	/** Hero.speed: RIDE_SPEED on horseback. */
	public static float speedFactor( Char ch ){
		return rides( ch ) ? RIDE_SPEED : 1f;
	}

	/** A horse was seen, tamed or ridden: the guide's page on them writes itself, once. */
	public static void seen(){
		if (!GuideProgress.pageFound( PAGE )) GuideGraph.reveal( GuideGraph.TAG_HORSE );
	}

	// ----------------------------------------------------------------- getting on

	/** A tap on a horse of his (Horse.interact): saddled, beside him and where he may ride, he gets
	 *  on it; a held tap (Hero.interactHeld), or a horse he cannot get on, shows its menu. */
	public static void tapped( Hero hero, Horse h ){
		if (h.saddled && !hero.interactHeld() && mountBlock( hero, h ) == null){
			mount( hero, h );
		} else {
			menu( hero, h );
		}
	}

	/** Why the hero may not get on a horse now (a key of this class's messages), null when he may. */
	public static String mountBlock( Hero hero, Horse h ){
		if (hero != Dungeon.hero) return "host_only";
		if (!h.tame || h.forSale) return "not_yours";
		if (!h.saddled) return "no_saddle";
		if (rides( hero )) return "riding";
		if (!ridingSlice( Dungeon.level )) return "no_riding_here";
		if (indoors( Dungeon.level, h.pos )) return "indoors";
		if (!Dungeon.level.adjacent( hero.pos, h.pos )) return "too_far";
		//nor from a boat or out of the deep (sea/Seafaring)
		if (hero.rooted || hero.flying || Seafaring.atSea( hero )) return "cant_now";
		return null;
	}

	/** The hero gets on a saddled horse of his beside him: it is taken off the level and goes under
	 *  him (Riding), and he is on its cell. Any rope on it comes back to his pack. A turn. */
	public static void mount( Hero hero, Horse h ){
		String no = mountBlock( hero, h );
		if (no != null){
			tell( hero, no );
			return;
		}
		Level level = Dungeon.level;
		unrope( hero, h );
		int from = hero.pos, to = h.pos;
		carry( level, h );
		Buff.affect( hero, Riding.class ).carry( h );
		hero.move( to, false );
		if (hero.sprite != null && hero.sprite.parent != null) hero.sprite.jump( from, to, 4f, 0.2f, null );
		if (Game.scene() instanceof GameScene) Dungeon.observe();
		Taming.sound( Assets.Sounds.TRAMPLE, to );
		GLog.i( Messages.get( Mounts.class, "mounted", h.name() ) );
		seen();
		hero.spendAndNext( 1f );
	}

	//a horse taken off its level, whole: off the scheduler with its clock rebased, as a parked mob is
	static void carry( Level level, Horse h ){
		Actor.remove( h );
		for (Buff b : h.buffs()) Actor.remove( b );
		h.clearTime();
		level.mobs.remove( h );
		if (h.sprite != null) h.sprite.killAndErase();
	}

	//the rope off a horse (led or tied), back in the hero's pack or at his feet
	static void unrope( Hero hero, Horse h ){
		Leashed t = Roping.rope( h );
		if (t == null || t.mode == Leashed.Mode.FREE) return;
		t.mode = Leashed.Mode.FREE;
		t.changed();
		give( hero, new Rope() );
	}

	// ----------------------------------------------------------------- getting down

	/** A tap on himself while he rides (Hero's move in place): he gets down, his horse stepping out
	 *  behind him to wait (a turn: true). False when he is on foot, or when there is no room beside
	 *  him for it (he is told). */
	public static boolean getDown( Hero hero ){
		Riding r = riding( hero );
		if (r == null) return false;
		Level level = Dungeon.level;
		//behind him: as far as can be from the cell he faces
		int front = hero.pos + (hero.sprite != null && hero.sprite.flipHorizontal ? -1 : 1);
		int cell = aside( level, hero.pos, front );
		if (cell == -1){
			tell( hero, "no_room" );
			return false;
		}
		Horse h = r.takeHorse();
		setDown( level, h, cell, hero.pos );
		Taming.sound( Assets.Sounds.TRAMPLE, hero.pos );
		GLog.i( Messages.get( Mounts.class, "dismounted", h.name() ) );
		hero.spendAndNext( 1f );
		return true;
	}

	/** Does the hero, riding, step onto a cell: never over a drop (he is told). */
	public static boolean stepAllowed( Hero hero, int step ){
		if (!rides( hero )) return true;
		Level level = Dungeon.level;
		if (level.pit[step] && !level.solid[step]){
			tell( hero, "no_drop" );
			return false;
		}
		return true;
	}

	/** Does the hero, riding, get down before a step: onto a doorway, under a roof, into a room, or
	 *  aboard his moored boat (no horse goes on a boat: sea/Seafaring). */
	public static boolean getsDownBefore( Hero hero, int step ){
		return rides( hero ) && (indoors( Dungeon.level, step ) || step == Seafaring.boatCell( hero ));
	}

	/** The hero stepped off a cell (Hero.getCloser, or Seafaring.launch putting him aboard): when he
	 *  got down for the step, his horse waits on it; riding on, it is the cell he came from. */
	public static void stepped( Hero hero, int from, boolean gotDown ){
		Riding r = riding( hero );
		if (r == null) return;
		if (gotDown){
			Horse h = r.takeHorse();
			setDown( Dungeon.level, h, from, -1 );
			//aboard his boat now (Seafaring.moved ran with the step), or at a door
			GLog.i( Messages.get( Mounts.class, Seafaring.aboard( hero ) ? "at_shore" : "at_door", h.name() ) );
		} else {
			r.cameFrom( Dungeon.level, from );
		}
	}

	/** The hero, riding, is about to strike in melee (Hero.actAttack): he jumps down first, his
	 *  horse stepping aside away from the foe. False, and he is told, when there is no room for it:
	 *  no blow then. */
	public static boolean beforeStrike( Hero hero, Char foe ){
		Riding r = riding( hero );
		if (r == null) return true;
		Level level = Dungeon.level;
		int cell = aside( level, hero.pos, foe.pos );
		if (cell == -1){
			tell( hero, "no_room" );
			return false;
		}
		Horse h = r.takeHorse();
		setDown( level, h, cell, hero.pos );
		GLog.i( Messages.get( Mounts.class, "jump_down", h.name() ) );
		return true;
	}

	/** A blow landing on the hero (Hero.damage) while he rides: his horse takes one part in
	 *  HORSE_SHARE of it, rounded; the rest is the hero's. Only blows: no fire, poison or hunger. */
	public static int shareBlow( Hero hero, int dmg, Object src ){
		Riding r = riding( hero );
		if (r == null || dmg <= 0 || !(src instanceof Char) || src == hero) return dmg;
		int part = Math.round( dmg / (float) HORSE_SHARE );
		if (part <= 0) return dmg;
		r.hurt( part, (Char) src );
		return dmg - part;
	}

	/** The hero's horse fell under him (Riding.hurt): it lies dead beside him, and he is on foot. */
	static void horseFalls( Hero hero, Char by ){
		Riding r = hero.buff( Riding.class );
		if (r == null || r.horse() == null) return;
		Horse h = r.takeHorse();
		Level level = Dungeon.level;
		int cell = aside( level, hero.pos, by == null ? -1 : by.pos );
		if (cell == -1){
			h.remains( level, hero.pos );
			GLog.n( Messages.get( Horse.class, "died" ) );
			return;
		}
		h.HP = 1;
		setDown( level, h, cell, -1 );
		h.die( by );
	}

	/** The hero gets down where he stands, his horse on the nearest free cell round him (the open
	 *  ground outside, when he is indoors): found indoors or off the riding slices on horseback
	 *  (Riding.act: a teleport, a push), or at any threshold a horse does not cross that this class
	 *  does not know of. He stays on it while the level has no free cell for it. */
	public static void setDownHere( Hero hero ){
		Riding r = riding( hero );
		Level level = Dungeon.level;
		if (r == null || level == null || hero.pos < 0 || hero.pos >= level.length()) return;
		int cell = nearFree( level, hero.pos );
		if (cell == -1) return;
		Horse h = r.takeHorse();
		setDown( level, h, cell, -1 );
		GLog.i( Messages.get( Mounts.class, "dismounted", h.name() ) );
	}

	/** A horse no longer under the hero stands on the nearest free cell round him, on his level;
	 *  never on his own cell. */
	static void standBeside( Hero hero, Horse h ){
		Level level = Dungeon.level;
		if (level == null || hero.pos < 0 || hero.pos >= level.length()) return;
		int cell = nearFree( level, hero.pos );
		if (cell == -1) return;
		setDown( level, h, cell, -1 );
		GLog.i( Messages.get( Mounts.class, "dismounted", h.name() ) );
	}

	/**
	 * The hero leaves a level (Mob.holdAllies): riding on to another riding slice (the steps up or
	 * down a mountain, a waystone), his horse goes under him. Anywhere else (a cave, the dungeon,
	 * a barrow, the town's buildings, a fall) he gets down at the threshold, and his horse waits on
	 * the level he leaves: on the cell he rode in from when it is free, else beside him.
	 */
	public static void leavingLevel( Level level, int heroPos ){
		Hero hero = Dungeon.hero;
		Riding r = riding( hero );
		if (r == null || level == null || toRidingSlice()) return;
		int came = r.cameFrom( level );
		Horse h = r.takeHorse();
		int cell = came != -1 && level.adjacent( came, heroPos ) && standable( level, came ) ? came : aside( level, heroPos, -1 );
		if (cell == -1) cell = nearFree( level, heroPos );
		h.pos = cell == -1 ? heroPos : cell;
		h.settle();
		//the level is about to be saved and left: no scene, no scheduler, only its mobs
		level.mobs.add( h );
		GLog.i( Messages.get( Mounts.class, "waits", h.name() ) );
	}

	//is the level being travelled to a riding slice
	static boolean toRidingSlice(){
		if (InterlevelScene.mode == null) return false;
		switch (InterlevelScene.mode){
			case DESCEND: case ASCEND:
				LevelTransition t = InterlevelScene.curTransition;
				return t != null && t.destBranch == 0 && ridingDepth( t.destDepth );
			case RETURN:
				return InterlevelScene.returnBranch == 0 && ridingDepth( InterlevelScene.returnDepth );
			default:
				return false;
		}
	}

	//a depth of branch 0 that is the surface or a slice of the mountains
	static boolean ridingDepth( int depth ){
		return WorldLayers.isLayerDepth( depth ) && WorldLayers.altitudeOf( depth ) >= 0;
	}

	// ----------------------------------------------------------------- where a horse stands

	/** Can a horse be set down on a cell: open ground out of doors, nobody on it. */
	static boolean standable( Level level, int c ){
		return c >= 0 && c < level.length() && level.insideMap( c ) && level.passable[c] && !level.avoid[c]
				&& !level.pit[c] && !indoors( level, c ) && Actor.findChar( c ) == null;
	}

	/** The free cell beside `from` a horse can be set down on, the furthest from `away` (any of
	 *  them when -1); -1 for none. */
	static int aside( Level level, int from, int away ){
		int best = -1, bestD = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = from + n;
			if (!standable( level, c )) continue;
			int d = away == -1 ? 0 : level.distance( c, away );
			if (best == -1 || d > bestD){
				best = c;
				bestD = d;
			}
		}
		return best;
	}

	//the nearest cell a horse can be set down on round a cell, ring by ring out to the level's
	//edge (from indoors, the nearest open ground outside); -1 for none on the whole level
	static int nearFree( Level level, int cell ){
		int w = level.width(), cx = cell % w, cy = cell / w;
		for (int r = 1, reach = Math.max( w, level.height() ); r <= reach; r++){
			//the ring's top and bottom rows, and its sides between them
			for (int i = -r; i <= r; i++){
				int c = freeAt( level, cx + i, cy - r );
				if (c == -1) c = freeAt( level, cx + i, cy + r );
				if (c == -1 && i > -r && i < r) c = freeAt( level, cx - r, cy + i );
				if (c == -1 && i > -r && i < r) c = freeAt( level, cx + r, cy + i );
				if (c != -1) return c;
			}
		}
		return -1;
	}

	//the cell at a level's x, y when a horse can be set down on it, else -1
	private static int freeAt( Level level, int x, int y ){
		if (x < 0 || y < 0 || x >= level.width() || y >= level.height()) return -1;
		int c = x + y * level.width();
		return standable( level, c ) ? c : -1;
	}

	/** A horse put back on the level played, on a cell: on the scene, its sprite walking out from
	 *  `from` when given and seen; calm. */
	static void setDown( Level level, Horse h, int cell, int from ){
		h.pos = cell;
		h.settle();
		if (level != Dungeon.level){
			level.mobs.add( h );
			return;
		}
		GameScene.add( h );
		//off a scene (a test) GameScene.add only lists it on the level
		if (!Actor.all().contains( h )) Actor.add( h );
		if (from != -1 && from != cell && h.sprite != null && h.sprite.parent != null && level.heroFOV != null
				&& (level.heroFOV[from] || level.heroFOV[cell])){
			h.sprite.place( from );
			h.sprite.move( from, cell );
		}
	}

	// ----------------------------------------------------------------- the saddle, food, the rope

	/** The hero saddles a horse of his beside him with the saddle from his pack. A turn. */
	public static void saddle( Hero hero, Horse h ){
		if (hero != Dungeon.hero){
			tell( hero, "host_only" );
			return;
		}
		Saddle s = hero.belongings.getItem( Saddle.class );
		if (s == null || h.saddled || !h.tame || h.forSale) return;
		s.detach( hero.belongings.backpack );
		h.saddled = true;
		h.setLook();
		Taming.sound( Assets.Sounds.ROPE, h.pos );
		GLog.i( Messages.get( Mounts.class, "saddled", h.name() ) );
		hero.spendAndNext( 1f );
	}

	/** The saddle's own action on a cell (items/misc/Saddle): the horse of his standing there,
	 *  beside him, saddled; else he is told why not. */
	public static void saddleAt( Hero hero, int cell ){
		if (hero != Dungeon.hero){
			tell( hero, "host_only" );
			return;
		}
		Char ch = cell >= 0 && cell < Dungeon.level.length() ? Actor.findChar( cell ) : null;
		if (!(ch instanceof Horse)){
			tell( hero, "not_a_horse" );
			return;
		}
		Horse h = (Horse) ch;
		if (!h.tame || h.forSale) tell( hero, "not_yours" );
		else if (h.saddled) tell( hero, "has_saddle" );
		else if (!Dungeon.level.adjacent( hero.pos, h.pos )) tell( hero, "too_far" );
		else saddle( hero, h );
	}

	/** The hero takes the saddle off a horse of his (into his pack, or at his feet). A turn. */
	public static void unsaddle( Hero hero, Horse h ){
		if (hero != Dungeon.hero || !h.saddled) return;
		h.saddled = false;
		h.setLook();
		give( hero, new Saddle() );
		Taming.sound( Assets.Sounds.ROPE, h.pos );
		GLog.i( Messages.get( Mounts.class, "unsaddled", h.name() ) );
		hero.spendAndNext( 1f );
	}

	/** The hero feeds a horse of his from his pack: it gets back a quarter of its health. A turn. */
	public static void feed( Hero hero, Horse h ){
		if (hero != Dungeon.hero) return;
		Item food = Taming.feedIn( hero );
		if (food == null) return;
		Item piece = food.detach( hero.belongings.backpack );
		if (piece == null) return;
		h.HP = Math.min( h.HT, h.HP + Math.max( 1, h.HT / 4 ) );
		Taming.munch( h );
		Taming.hearts( h, 1 );
		Taming.sound( Assets.Sounds.EAT, h.pos );
		GLog.i( Messages.get( Mounts.class, "fed", h.name(), piece.name() ) );
		hero.spendAndNext( 1f );
	}

	/** The hero lets a horse of his go back to the wild: it no longer trusts him; its saddle and any
	 *  rope come back to him. A turn. */
	public static void setFree( Hero hero, Horse h ){
		if (hero != Dungeon.hero || !h.tame) return;
		unrope( hero, h );
		if (h.saddled){
			h.saddled = false;
			give( hero, new Saddle() );
		}
		h.tame = false;
		h.trust = 0;
		h.setLook();
		GLog.i( Messages.get( Mounts.class, "freed", h.name() ) );
		hero.spendAndNext( 1f );
	}

	//an item for the hero: in his pack, or at his feet when it is full
	static void give( Hero hero, Item item ){
		if (hero != null && hero.isAlive() && item.collect( hero.belongings.backpack )) return;
		Level level = Dungeon.level;
		if (level == null || hero == null || hero.pos < 0) return;
		Heap heap = level.drop( item, hero.pos );
		if (heap.sprite != null) heap.sprite.drop();
	}

	//a tie point beside the hero (a fence, a palisade, a tie post), the nearest to the horse; -1 for none
	private static int postBeside( Level level, int heroPos, int horsePos ){
		int best = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = heroPos + n;
			if (!Roping.tiePoint( level, c )) continue;
			if (best == -1 || level.distance( c, horsePos ) < level.distance( best, horsePos )) best = c;
		}
		return best;
	}

	// ----------------------------------------------------------------- the menu

	/** A horse of his menu: ride it, its saddle, food, the rope, set it free. */
	public static void menu( final Hero hero, final Horse h ){
		if (hero != Dungeon.hero){
			tell( hero, "host_only" );
			return;
		}
		final ArrayList<String> options = new ArrayList<>();
		final ArrayList<Runnable> does = new ArrayList<>();
		String body = Messages.get( Mounts.class, "menu", h.HP, h.HT );
		if (h.saddled){
			String no = mountBlock( hero, h );
			if (no == null){
				options.add( Messages.get( Mounts.class, "do_ride" ) );
				does.add( () -> mount( hero, h ) );
			} else {
				body += "\n\n" + Messages.get( Mounts.class, no );
			}
			options.add( Messages.get( Mounts.class, "do_unsaddle" ) );
			does.add( () -> unsaddle( hero, h ) );
		} else if (hero.belongings.getItem( Saddle.class ) != null){
			options.add( Messages.get( Mounts.class, "do_saddle" ) );
			does.add( () -> saddle( hero, h ) );
		} else {
			body += "\n\n" + Messages.get( Mounts.class, "menu_no_saddle" );
		}
		Item food = Taming.feedIn( hero );
		if (food != null){
			options.add( Messages.get( Mounts.class, "do_feed", food.name() ) );
			does.add( () -> feed( hero, h ) );
		}
		Leashed t = Roping.rope( h );
		Leashed.Mode mode = t == null ? Leashed.Mode.FREE : t.mode;
		if (mode == Leashed.Mode.LEASHED){
			options.add( Messages.get( Mounts.class, "do_let_go" ) );
			does.add( () -> Roping.letGo( hero, h ) );
			final int post = postBeside( Dungeon.level, hero.pos, h.pos );
			if (post != -1){
				options.add( Messages.get( Mounts.class, "do_tie" ) );
				does.add( () -> Roping.tie( hero, h, post ) );
			}
		} else if (mode == Leashed.Mode.TIED){
			if (Roping.leashed( Dungeon.level ).size() < Roping.MAX_LEASHED){
				options.add( Messages.get( Mounts.class, "do_untie_lead" ) );
				does.add( () -> Roping.leash( hero, h ) );
			}
			options.add( Messages.get( Mounts.class, "do_untie" ) );
			does.add( () -> Roping.letGo( hero, h ) );
		} else if (Material.count( hero, Rope.class ) > 0 && Roping.leashed( Dungeon.level ).size() < Roping.MAX_LEASHED){
			options.add( Messages.get( Mounts.class, "do_lead" ) );
			does.add( () -> Roping.leash( hero, h ) );
		}
		options.add( Messages.get( Mounts.class, "do_free" ) );
		does.add( () -> confirmFree( hero, h ) );
		options.add( Messages.get( Mounts.class, "do_leave" ) );
		final String text = body;
		Game.runOnRenderThread( () -> GameScene.show( new WndOptions( h.sprite(), Messages.titleCase( h.name() ), text,
				options.toArray( new String[0] ) ){
			@Override
			protected void onSelect( int index ){
				if (index >= 0 && index < does.size()) does.get( index ).run();
			}
		} ) );
	}

	private static void confirmFree( final Hero hero, final Horse h ){
		GameScene.show( new WndOptions( h.sprite(), Messages.titleCase( h.name() ),
				Messages.get( Mounts.class, "free_confirm" ),
				Messages.get( Mounts.class, "do_free_yes" ), Messages.get( Mounts.class, "do_leave" ) ){
			@Override
			protected void onSelect( int index ){
				if (index == 0) setFree( hero, h );
			}
		} );
	}

	//told to the hero who asked (a guest's goes to his own log)
	static void tell( Hero hero, String key, Object... args ){
		String text = Messages.get( Mounts.class, key, args );
		if (hero == Dungeon.hero) GLog.w( text );
		else NetManager.heroLog( hero, text );
	}
}
