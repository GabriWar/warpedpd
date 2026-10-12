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

package xyz.gabriwar.warpedpixeldungeon.homestead.animals;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.HuntPack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Enclosure;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Grazing;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * The hero's rope on the world's peaceful beasts (Leadable: deer, goats, bunnies). Nothing is
 * tamed: an animal is ROPED and PULLED, TIED to a post, or PENNED in.
 *
 * LEASH (a rope's action, thrown over an animal up to LASSO cells off that the hero sees with
 * nothing in the way - past a deer's or a goat's shyness, so a wary beast is roped as it eyes him):
 * the rope goes from the pack onto it and it follows him - every turn it is more than a step away
 * it walks after him (twice as fast when it lags further), and when it cannot find the way and is
 * more than DRAG cells off it is pulled to the cell he just left. Past SNAP cells the rope slips off (back to his pack) and it is free. It
 * never flees while roped. He leads MAX_LEASHED at most. Going between the world's slices or by a
 * waystone, a roped animal beside him comes along; any other's rope slips.
 *
 * TIE (a rope's action, or the tap menu, on a fence, a palisade or a tie post beside the hero):
 * the animal he leads nearest to it is tied there and keeps within a step of it; the post gone,
 * it is loose (the rope dropped where it stands). A tap on a tied animal unties it (to lead it, or
 * to let it go) or butchers it.
 *
 * PENNED: an animal standing in an area a fence, a palisade, built walls and shut gates close all
 * round (Enclosure) stays there for good: never culled, never parked out of mind, never fleeing,
 * wandering only inside. A tap on it butchers it. BUTCHERED: it dies at once, leaving its meat
 * and a hide (no experience: it was no foe).
 *
 * Everything runs on the host, for the host's hero: a co-op guest is told it is the host's to do
 * (every action and the tap menu refuse him).
 */
public final class Roping {

	private Roping(){}

	public static final String AC_LEASH = "LEASH", AC_TIE = "TIE";

	/** The most animals the hero leads at once, how far a roped one strays before its rope slips,
	 *  how far behind one that cannot follow is pulled from, and how often (turns) a beast looks
	 *  whether it is penned. */
	public static final int MAX_LEASHED = 2, SNAP = 6, DRAG = 3, PEN_LOOK = 10;

	/** How far the hero throws a rope over an animal, in cells: one more than a deer's shyness, so
	 *  he can rope one that has not yet bolted from him. */
	public static final int LASSO = 6;

	// ----------------------------------------------------------------- who

	public static Leashed rope( Char ch ){
		return ch.buff( Leashed.class );
	}

	/** Is the animal the homestead's now: roped, tied or penned (Leadable.homesteadKept). */
	public static boolean held( Mob m ){
		Leashed t = rope( m );
		return t != null && t.holds();
	}

	/** May the hero put a rope on it: a peaceful beast of the world, alive and nobody's, no boss
	 *  and no hunt's quarry. */
	public static boolean leadable( Mob m ){
		return m instanceof Leadable && m.isAlive() && m.alignment == Char.Alignment.NEUTRAL
				&& !Char.hasProp( m, Char.Property.BOSS ) && m.buff( HuntPack.class ) == null;
	}

	/** The animals on a level the hero leads now. */
	public static ArrayList<Mob> leashed( Level level ){
		ArrayList<Mob> out = new ArrayList<>();
		for (Mob m : level.mobs){
			Leashed t = rope( m );
			if (t != null && t.mode == Leashed.Mode.LEASHED) out.add( m );
		}
		return out;
	}

	/** How many animals are tied to the post on a cell (for the post's tap). */
	public static int tiedTo( Level level, int post ){
		if (!(level instanceof OverworldLevel)) return 0;
		OverworldLevel ow = (OverworldLevel) level;
		int px = ow.worldX + post % ow.width(), py = ow.worldY + post / ow.width(), n = 0;
		for (Mob m : level.mobs){
			Leashed t = rope( m );
			if (t != null && t.mode == Leashed.Mode.TIED && t.postX == px && t.postY == py) n++;
		}
		return n;
	}

	/** Can an animal be tied to this cell: a fence, a palisade, a tie post. */
	public static boolean tiePoint( Level level, int cell ){
		if (cell < 0 || cell >= level.length()) return false;
		int t = level.map[cell];
		if (t == Terrain.FENCE || t == Terrain.PALISADE) return true;
		for (Mob m : level.mobs) if (m.pos == cell && m instanceof TiePost) return true;
		return false;
	}

	// ----------------------------------------------------------------- the rope's actions

	/** The rope's actions on the world's slices. */
	public static void actions( Hero hero, ArrayList<String> out ){
		if (Dungeon.level instanceof OverworldLevel){
			out.add( AC_LEASH );
			out.add( AC_TIE );
		}
	}

	/** A rope's action chosen: the cell to put it on. The host's hero only. */
	public static void execute( final Hero hero, String action ){
		if (!AC_LEASH.equals( action ) && !AC_TIE.equals( action )) return;
		if (hero != Dungeon.hero){
			tell( hero, "host_only" );
			return;
		}
		if (AC_LEASH.equals( action )){
			GameScene.selectCell( new CellSelector.Listener(){
				@Override
				public void onSelect( Integer cell ){
					if (cell == null) return;
					String no = lassoBlock( hero, cell );
					if (no != null){
						tell( hero, no, LASSO );
						return;
					}
					Mob m = (Mob) Actor.findChar( cell );
					no = refusal( hero, m );
					if (no != null) refuse( hero, m, no );
					else throwRope( hero, m );
				}

				@Override
				public String prompt(){
					return Messages.get( Roping.class, "leash_prompt" );
				}
			} );
		} else if (AC_TIE.equals( action )){
			GameScene.selectCell( new CellSelector.Listener(){
				@Override
				public void onSelect( Integer cell ){
					if (cell == null) return;
					if (!tiePoint( Dungeon.level, cell )){
						tell( hero, "not_post" );
					} else if (Dungeon.level.distance( hero.pos, cell ) > 1){
						tell( hero, "too_far_post" );
					} else {
						Mob led = nearestLed( Dungeon.level, cell );
						if (led == null) tell( hero, "nothing_led" );
						else tie( hero, led, cell );
					}
				}

				@Override
				public String prompt(){
					return Messages.get( Roping.class, "tie_prompt" );
				}
			} );
		}
	}

	/**
	 * Can the hero throw his rope over whatever stands on a cell: a beast for a rope (leadable),
	 * within LASSO cells, in his sight, nothing in the rope's way (Ballistica.PROJECTILE). The key of
	 * the message telling why not ("not_animal", "too_far"), null when he can.
	 */
	public static String lassoBlock( Hero hero, int cell ){
		Level level = Dungeon.level;
		if (cell < 0 || cell >= level.length()) return "not_animal";
		Char ch = Actor.findChar( cell );
		if (!(ch instanceof Mob) || !leadable( (Mob) ch )) return "not_animal";
		if (level.distance( hero.pos, cell ) > LASSO || hero.fieldOfView == null || !hero.fieldOfView[cell]
				|| new Ballistica( hero.pos, cell, Ballistica.PROJECTILE ).collisionPos != cell) return "too_far";
		return null;
	}

	/** The hero throws his rope over the animal on a cell (lassoBlock, then leash), at once: what
	 *  a rope's LEAD does once its rope has flown. False, and the hero told why, when it does not
	 *  take. */
	public static boolean lasso( Hero hero, int cell ){
		String no = lassoBlock( hero, cell );
		if (no != null){
			tell( hero, no, LASSO );
			return false;
		}
		return leash( hero, (Mob) Actor.findChar( cell ) );
	}

	//the rope flies from the hero to the animal and takes hold there (leash); no flight to show
	//without a scene
	private static void throwRope( final Hero hero, final Mob m ){
		if (hero.sprite == null || hero.sprite.parent == null || m.sprite == null || !(Game.scene() instanceof GameScene)){
			leash( hero, m );
			return;
		}
		hero.sprite.zap( m.pos );
		hero.busy();
		((MissileSprite) hero.sprite.parent.recycle( MissileSprite.class )).reset( hero.sprite, m.sprite, new Rope(), () -> {
			if (!leash( hero, m )) hero.next();
		} );
	}

	//the animal the hero leads nearest a cell, or null
	private static Mob nearestLed( Level level, int cell ){
		Mob best = null;
		for (Mob m : leashed( level )){
			if (best == null || level.distance( m.pos, cell ) < level.distance( best.pos, cell )) best = m;
		}
		return best;
	}

	//told to the hero who asked (a guest's goes to his own log)
	private static void tell( Hero hero, String key, Object... args ){
		String text = Messages.get( Roping.class, key, args );
		if (hero == Dungeon.hero) GLog.w( text );
		else NetManager.heroLog( hero, text );
	}

	//why the hero may not rope an animal now (a message key), null when he may: a guest, off the
	//world's slices, no beast for a rope, one already led, MAX_LEASHED led already, no rope (one
	//tied to a post is led on its own)
	private static String refusal( Hero hero, Mob m ){
		if (hero != Dungeon.hero) return "host_only";
		if (!(Dungeon.level instanceof OverworldLevel)) return "not_here";
		if (!leadable( m )) return "not_animal";
		Leashed t = rope( m );
		if (t != null && t.mode == Leashed.Mode.LEASHED) return "already";
		if (leashed( Dungeon.level ).size() >= MAX_LEASHED) return "too_many";
		if ((t == null || t.mode != Leashed.Mode.TIED) && Material.count( hero, Rope.class ) < 1) return "no_rope";
		return null;
	}

	//the hero told a refusal, with what its message names
	private static void refuse( Hero hero, Mob m, String key ){
		if ("already".equals( key )) tell( hero, key, m.name() );
		else if ("too_many".equals( key )) tell( hero, key, MAX_LEASHED );
		else tell( hero, key );
	}

	/**
	 * The hero ropes an animal: a rope from his pack goes on it (one already tied to a post is
	 * untied and led on its own rope). How near it must be is the caller's (lasso, the tap menu).
	 * False, and the hero told why, when it may not be (refusal). A turn's work.
	 */
	public static boolean leash( Hero hero, Mob m ){
		String no = refusal( hero, m );
		if (no != null){
			refuse( hero, m, no );
			return false;
		}
		Leashed t = rope( m );
		boolean tied = t != null && t.mode == Leashed.Mode.TIED;
		if (!tied) Material.spend( hero, Rope.class, 1 );
		t = Buff.affect( m, Leashed.class );
		t.mode = Leashed.Mode.LEASHED;
		t.leaderAt = worldKey( (OverworldLevel) Dungeon.level, hero.pos );
		t.leaderWas = Long.MIN_VALUE;
		t.changed();
		m.state = m.WANDERING;
		if (m.sprite != null) m.sprite.hideAlert();
		m.remove( Grazing.class );
		sound( Assets.Sounds.ROPE, m.pos );
		GLog.i( Messages.get( Roping.class, tied ? "untied_led" : "leashed", m.name() ) );
		xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.reveal( xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.TAG_ROPE );
		hero.spendAndNext( 1f );
		return true;
	}

	/** The hero ties an animal he leads to a post beside him (Roping.tiePoint): its rope moves
	 *  from his hand to the post. A turn's work. */
	public static void tie( Hero hero, Mob m, int post ){
		if (hero != Dungeon.hero){
			tell( hero, "host_only" );
			return;
		}
		Leashed t = rope( m );
		if (t == null || t.mode != Leashed.Mode.LEASHED || !(Dungeon.level instanceof OverworldLevel)) return;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		t.mode = Leashed.Mode.TIED;
		t.postX = ow.worldX + post % ow.width();
		t.postY = ow.worldY + post / ow.width();
		t.changed();
		sound( Assets.Sounds.ROPE, post );
		GLog.i( Messages.get( Roping.class, "tied", m.name() ) );
		hero.spendAndNext( 1f );
	}

	/** The hero takes his rope off an animal he leads or has tied: it is free (penned, when it
	 *  stands in a pen). A turn's work. */
	public static void letGo( Hero hero, Mob m ){
		if (hero != Dungeon.hero){
			tell( hero, "host_only" );
			return;
		}
		Leashed t = rope( m );
		if (t == null || t.mode == Leashed.Mode.FREE) return;
		ropeBack( hero, m.pos );
		free( m, t );
		GLog.i( Messages.get( Roping.class, "let_go", m.name() ) );
		hero.spendAndNext( 1f );
	}

	/** The rope slips off an animal (it strayed too far, the hero went on without it): back in
	 *  his pack, and the animal is free. */
	public static void snap( Mob m ){
		Leashed t = rope( m );
		if (t == null || t.mode == Leashed.Mode.FREE) return;
		ropeBack( Dungeon.hero, m.pos );
		free( m, t );
		if (Dungeon.level != null && Dungeon.level.mobs.contains( m ) && Dungeon.level.heroFOV != null
				&& m.pos >= 0 && m.pos < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[m.pos]){
			GLog.w( Messages.get( Roping.class, "snapped", m.name() ) );
		}
	}

	//the rope off it; it is free, or penned where it stands
	private static void free( Mob m, Leashed t ){
		t.mode = Leashed.Mode.FREE;
		t.changed();
		if (Dungeon.level instanceof OverworldLevel) lookAtPen( (OverworldLevel) Dungeon.level, m, true );
		if (!t.holds()) t.penLooked = Float.NEGATIVE_INFINITY;
	}

	//a rope back into the hero's pack, or on the ground at a cell when he has no room (or is gone)
	private static void ropeBack( Hero hero, int cell ){
		Rope rope = new Rope();
		if (hero != null && hero.isAlive() && rope.collect( hero.belongings.backpack )) return;
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length()) return;
		Heap h = level.drop( rope, cell );
		if (h.sprite != null) h.sprite.drop();
	}

	/** The meat an animal gives the butcher: two for a deer or a goat, one for anything smaller. */
	public static int meatOf( Mob m ){
		return m instanceof Deer || m instanceof Goat ? 2 : 1;
	}

	/**
	 * The hero butchers a tied or penned animal: it dies on the spot, leaving its meat (meatOf)
	 * and a hide, and any rope on it, at its feet; no experience. A turn's work.
	 */
	public static void butcher( Hero hero, Mob m ){
		if (hero != Dungeon.hero){
			tell( hero, "host_only" );
			return;
		}
		Leashed t = rope( m );
		if (t == null || !(t.mode == Leashed.Mode.TIED || t.penned) || !m.isAlive()) return;
		Level level = Dungeon.level;
		int cell = m.pos;
		if (t.mode != Leashed.Mode.FREE) ropeBack( hero, cell );
		t.mode = Leashed.Mode.FREE;
		t.changed();
		Heap h = level.drop( new MysteryMeat().quantity( meatOf( m ) ), cell );
		if (h.sprite != null) h.sprite.drop();
		h = level.drop( new Hide(), cell );
		if (h.sprite != null) h.sprite.drop();
		sound( Assets.Sounds.HIT_SLASH, cell );
		GLog.i( Messages.get( Roping.class, "butchered", m.name() ) );
		//no blow and no death of a foe: nothing to learn from it
		m.destroy();
		if (m.sprite != null) m.sprite.die();
		hero.spendAndNext( 1f );
	}

	// ----------------------------------------------------------------- the tap

	/** A tap on an animal the homestead holds (Animals.interact): its menu. False for a free one
	 *  (its own ways). */
	public static boolean interact( final Mob m, Char c ){
		Leashed t = rope( m );
		if (t == null || !t.holds()) return false;
		if (c != Dungeon.hero){
			if (c instanceof Hero) tell( (Hero) c, "host_only" );
			return true;
		}
		final Hero hero = (Hero) c;
		final Leashed.Mode mode = t.mode;
		final ArrayList<String> options = new ArrayList<>();
		final ArrayList<Runnable> does = new ArrayList<>();
		String body;
		if (mode == Leashed.Mode.LEASHED){
			body = Messages.get( Roping.class, "menu_led", m.name() );
			options.add( Messages.get( Roping.class, "do_let_go" ) );
			does.add( () -> letGo( hero, m ) );
			final int post = postBeside( Dungeon.level, hero.pos, m.pos );
			if (post != -1){
				options.add( Messages.get( Roping.class, "do_tie" ) );
				does.add( () -> tie( hero, m, post ) );
			}
		} else if (mode == Leashed.Mode.TIED){
			body = Messages.get( Roping.class, "menu_tied", m.name() );
			if (leashed( Dungeon.level ).size() < MAX_LEASHED){
				options.add( Messages.get( Roping.class, "do_untie_lead" ) );
				does.add( () -> leash( hero, m ) );
			}
			options.add( Messages.get( Roping.class, "do_untie" ) );
			does.add( () -> letGo( hero, m ) );
			options.add( Messages.get( Roping.class, "do_butcher" ) );
			does.add( () -> confirmButcher( hero, m ) );
		} else {
			body = Messages.get( Roping.class, "menu_penned", m.name() );
			options.add( Messages.get( Roping.class, "do_butcher" ) );
			does.add( () -> confirmButcher( hero, m ) );
		}
		options.add( Messages.get( Roping.class, "do_leave" ) );
		final String text = body;
		Game.runOnRenderThread( () -> GameScene.show( new WndOptions( m.sprite(), Messages.titleCase( m.name() ), text,
				options.toArray( new String[0] ) ){
			@Override
			protected void onSelect( int index ){
				if (index >= 0 && index < does.size()) does.get( index ).run();
			}
		} ) );
		return true;
	}

	//a tie point beside the hero, the nearest to the animal; -1 for none
	private static int postBeside( Level level, int heroPos, int animalPos ){
		int best = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = heroPos + n;
			if (!tiePoint( level, c )) continue;
			if (best == -1 || level.distance( c, animalPos ) < level.distance( best, animalPos )) best = c;
		}
		return best;
	}

	private static void confirmButcher( final Hero hero, final Mob m ){
		GameScene.show( new WndOptions( m.sprite(), Messages.titleCase( m.name() ),
				Messages.get( Roping.class, "butcher_confirm", m.name(), meatOf( m ) ),
				Messages.get( Roping.class, "do_butcher_yes" ), Messages.get( Roping.class, "do_leave" ) ){
			@Override
			protected void onSelect( int index ){
				if (index == 0) butcher( hero, m );
			}
		} );
	}

	// ----------------------------------------------------------------- the animal's turn

	/**
	 * A Leadable animal's turn (Animals.act): it looks whether it is penned now and then, then a
	 * roped one follows the hero, a tied one keeps by its post, a penned one wanders inside its pen.
	 * The time it spent, or 0 for a free one (its own ways).
	 */
	public static float act( Mob m ){
		if (!(Dungeon.level instanceof OverworldLevel) || !(m instanceof Leadable)) return 0f;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		lookAtPen( ow, m, false );
		Leashed t = rope( m );
		if (t == null || !t.holds()) return 0f;
		if (m.sprite != null) m.sprite.hideAlert();
		switch (t.mode){
			case LEASHED: return follow( ow, m, t );
			case TIED:    return keepByPost( ow, m, t );
			default:      return keepInPen( ow, m );
		}
	}

	/**
	 * Whether an animal stands in a closed pen (Enclosure), looked at once every PEN_LOOK turns, or
	 * at once with `now` (the window about to move, a rope just taken off). An animal already
	 * penned keeps its pen while the hero holds its gate.
	 */
	public static void lookAtPen( OverworldLevel ow, Mob m, boolean now ){
		if (!(m instanceof Leadable) || !m.isAlive()) return;
		Leashed t = rope( m );
		float clock = Actor.now();
		if (!now){
			//a wild beast with no rope state yet looks one turn in PEN_LOOK, its own turn of them
			if (t == null && Math.floorMod( (int) clock + m.id(), PEN_LOOK ) != 0) return;
			if (t != null && clock >= t.penLooked && clock - t.penLooked < PEN_LOOK) return;
		}
		//nothing built anywhere: no pen
		boolean penned = ow.homestead().builtCount() > 0 && leadable( m )
				&& Enclosure.closed( ow, m.pos, t != null && t.penned );
		if (t == null){
			if (!penned) return;
			t = Buff.affect( m, Leashed.class );
		}
		t.penLooked = clock;
		if (penned != t.penned){
			t.penned = penned;
			if (penned){
				m.state = m.WANDERING;
				m.remove( Grazing.class );
			}
		}
	}

	//roped: after the hero, pulled when it cannot follow, loose past SNAP
	private static float follow( OverworldLevel ow, Mob m, Leashed t ){
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive() || hero.pos < 0){
			snap( m );
			return Actor.TICK;
		}
		long at = worldKey( ow, hero.pos );
		if (at != t.leaderAt){
			t.leaderWas = t.leaderAt;
			t.leaderAt = at;
		}
		int d = ow.distance( m.pos, hero.pos );
		if (d > SNAP){
			snap( m );
			return Actor.TICK;
		}
		if (d <= 1) return Actor.TICK;
		int step = stepToward( m, hero.pos );
		if (step != -1){
			moveTo( m, step );
			//lagging well behind it trots to catch up
			return d > 2 ? Actor.TICK / 2f : Actor.TICK;
		}
		if (d > DRAG){
			int to = dragCell( ow, hero, t, m.pos );
			if (to != -1) pull( m, to );
		}
		return Actor.TICK;
	}

	//where a roped animal that cannot follow is pulled to: the cell the hero just left, else the
	//free cell beside him nearest it
	private static int dragCell( OverworldLevel ow, Hero hero, Leashed t, int from ){
		if (t.leaderWas != Long.MIN_VALUE){
			int was = ow.localCell( (int) (t.leaderWas & 0xFFFFFFFFL), (int) (t.leaderWas >> 32) );
			if (was != -1 && ow.adjacent( was, hero.pos ) && standable( ow, was )) return was;
		}
		return freeBeside( ow, hero.pos, from );
	}

	//a free cell an animal can stand on beside a cell (the nearest to `near` when given), -1 for none
	private static int freeBeside( Level level, int cell, int near ){
		int best = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = cell + n;
			if (!standable( level, c )) continue;
			if (best == -1 || (near != -1 && level.distance( c, near ) < level.distance( best, near ))) best = c;
		}
		return best;
	}

	//firm ground an animal may be set on: no wall, water, pit or harm, and nobody there
	private static boolean standable( Level level, int c ){
		return c >= 0 && c < level.length() && level.insideMap( c ) && level.passable[c] && !level.avoid[c]
				&& !level.pit[c] && Actor.findChar( c ) == null;
	}

	//tied: back within a step of its post, grazing round it; the post gone, it is loose. A post the
	//window does not hold (past its rim, or on it: OverworldLevel.localCell) is not known to be
	//gone: the beast stands still until the window holds it again
	private static float keepByPost( OverworldLevel ow, Mob m, Leashed t ){
		int post = ow.localCell( t.postX, t.postY );
		if (post == -1) return Actor.TICK;
		if (!tiePoint( ow, post )){
			boolean seen = ow.heroFOV != null && ow.heroFOV[m.pos];
			ropeBack( null, m.pos );
			free( m, t );
			if (seen) GLog.w( Messages.get( Roping.class, "post_gone", m.name() ) );
			return 0f;
		}
		int d = ow.distance( m.pos, post );
		if (d > 1){
			int step = stepToward( m, post );
			if (step != -1) moveTo( m, step );
			else if (d > DRAG){
				int to = freeBeside( ow, post, m.pos );
				if (to != -1) pull( m, to );
			}
			return Actor.TICK;
		}
		amble( ow, m, post );
		return Actor.TICK;
	}

	//penned: a step now and then, never into a gateway or a doorway
	private static float keepInPen( OverworldLevel ow, Mob m ){
		amble( ow, m, -1 );
		return Actor.TICK;
	}

	//a step one time in three to a free cell beside it: within a step of `post` when given, never a
	//gate or a door (a penned beast keeps to its pen while the hero holds the gate), no water
	private static void amble( OverworldLevel ow, Mob m, int post ){
		if (Random.Int( 3 ) != 0) return;
		int c = m.pos + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
		if (!standable( ow, c ) || ow.water[c]) return;
		int t = ow.map[c];
		if (BuiltTerrain.gate( t ) || BuiltTerrain.door( t )) return;
		if (post != -1 && ow.distance( c, post ) > 1) return;
		moveTo( m, c );
	}

	// ----------------------------------------------------------------- moving one

	//every char stands in the way, wherever it is (a pull or a step never lands on one)
	private static boolean[] allSeen;

	/** An animal's step toward a cell (a char's or a post's: it need not be free), round whoever
	 *  stands in the way; -1 when no way leads there. */
	public static int stepToward( Mob m, int to ){
		Level level = Dungeon.level;
		if (allSeen == null || allSeen.length != level.length()){
			allSeen = new boolean[level.length()];
			Arrays.fill( allSeen, true );
		}
		int step = Dungeon.findStep( m, to, level.passable, allSeen, true );
		return step == to && Actor.findChar( to ) != null ? -1 : step;
	}

	/** An animal steps onto a cell beside it, its sprite walking there when the hero sees it. One
	 *  with no sprite (no scene shows it) is only set on the cell. */
	public static void moveTo( Mob m, int cell ){
		Level level = Dungeon.level;
		if (m.sprite == null){
			m.pos = cell;
			level.occupyCell( m );
			return;
		}
		if (m.sprite.isVisible() && m.sprite.parent != null && (level.heroFOV[m.pos] || level.heroFOV[cell])){
			m.sprite.move( m.pos, cell );
		} else {
			m.sprite.turnTo( m.pos, cell );
			m.sprite.place( cell );
		}
		m.move( cell );
	}

	//a roped or tied animal hauled to a cell it could not walk to: no walk, the rope tightens
	private static void pull( Mob m, int cell ){
		Level level = Dungeon.level;
		int from = m.pos;
		m.pos = cell;
		if (m.sprite != null){
			if (level.heroFOV[from] || level.heroFOV[cell]) m.sprite.move( from, cell );
			else m.sprite.place( cell );
		}
		level.occupyCell( m );
		sound( Assets.Sounds.ROPE, cell );
	}

	//a sound from a cell, when the game is on screen
	private static void sound( String id, int cell ){
		if (Game.scene() instanceof GameScene) SpatialSound.play( id, cell );
	}

	private static long worldKey( OverworldLevel ow, int cell ){
		return OverworldLevel.worldKey( ow.worldX + cell % ow.width(), ow.worldY + cell / ow.width() );
	}

	// ----------------------------------------------------------------- travel

	/**
	 * Mob.holdAllies, as the hero leaves a level: a roped animal beside him comes along when he is
	 * going to another of the world's slices (a slice's way, a waystone); any other roped one's
	 * rope slips (snap). True when it comes along.
	 */
	public static boolean goesAlong( Mob m, int heroPos ){
		Leashed t = rope( m );
		if (t == null || t.mode != Leashed.Mode.LEASHED) return false;
		if (Dungeon.level instanceof OverworldLevel && Dungeon.level.distance( m.pos, heroPos ) <= 1 && toASlice()){
			m.remove( Grazing.class );
			return true;
		}
		snap( m );
		return false;
	}

	//is the hero on his way to one of the world's slices (by a slice's way, or landing on one)
	private static boolean toASlice(){
		if (InterlevelScene.mode == null) return false;
		switch (InterlevelScene.mode){
			case DESCEND: case ASCEND:
				return InterlevelScene.curTransition != null && InterlevelScene.curTransition.destBranch == 0
						&& WorldLayers.isLayerDepth( InterlevelScene.curTransition.destDepth );
			case RETURN:
				return InterlevelScene.returnBranch == 0 && WorldLayers.isLayerDepth( InterlevelScene.returnDepth );
			default:
				return false;
		}
	}
}
