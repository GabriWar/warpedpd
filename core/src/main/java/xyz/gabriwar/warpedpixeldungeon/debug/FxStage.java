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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * The effects gallery's stage (FxGallery), carved in the blank room around the hero (sent to the
 * sewers' first floor from the surface): a 5x3 pool two cells east of him with a one-cell inlet
 * south of its middle (a curved shore), a strip of three high grass and three grass three rows
 * north, a 3x2 patch of bare stone to the west for blobs and scorches, a two-cell stub of wall to
 * the south-west, and four hardy dummies standing still (9999 health): a rat on dry floor, a rat
 * in the pool, a skeleton, a bat over the pool.
 *
 * An exhibit (FxGallery.Exhibit) plays on it on the render thread: it may call any of the game's
 * effects, prefers visual-only calls, and uses puppets (puppet()) where something must move,
 * jump, strike or zap: a sprite linked to a dummy character that is on no level and never acts,
 * whose cell the stage sets before each scripted move. What an exhibit leaves running (a pour, a
 * timer, the weather held, a blob) the stage undoes at the next page (reset()).
 */
public final class FxStage {

	/** The dummies' health: they take whatever an exhibit throws at them. */
	public static final int HARDY = 9999;

	/** The hero's cell when it was built: every spot is set off from it. */
	public final int origin;

	public Mob dryRat, wetRat, skeleton, bat;

	private final ArrayList<Puppet> puppets = new ArrayList<>();
	private final ArrayList<Emitter> pours = new ArrayList<>();
	private final ArrayList<Timer> timers = new ArrayList<>();
	private boolean skyHeld;

	private FxStage( int origin ){
		this.origin = origin;
	}

	/** Carves the stage round the hero (the blank room already carved: DebugScenes.Room) and puts
	 *  its dummies on it. */
	static FxStage build( Hero hero ){
		FxStage s = new FxStage( hero.pos );
		s.carve();
		s.dummies();
		return s;
	}

	// ------------------------------------------------------------------ where things are

	/** The cell (dx, dy) from the hero's, -1 off the level's inner cells. */
	public int cell( int dx, int dy ){
		Level level = Dungeon.level;
		int w = level.width(), x = origin % w + dx, y = origin / w + dy;
		if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) return -1;
		return x + y * w;
	}

	/** A cell's middle, world px. */
	public static PointF center( int cell ){
		return DungeonTilemap.tileCenterToWorld( cell );
	}

	/** The pool's cells, west to east and north to south. */
	public int[] pool(){
		int[] out = new int[15];
		int i = 0;
		for (int dy = -1; dy <= 1; dy++) for (int dx = 2; dx <= 6; dx++) out[i++] = cell( dx, dy );
		return out;
	}

	/** The pool's middle cell. */
	public int poolMiddle(){
		return cell( 4, 0 );
	}

	/** The inlet south of the pool's middle. */
	public int inlet(){
		return cell( 4, 2 );
	}

	/** The grass strip, high grass first. */
	public int[] grass(){
		int[] out = new int[6];
		for (int i = 0; i < 6; i++) out[i] = cell( -1 + i, -3 );
		return out;
	}

	/** The patch of bare stone, its middle first. */
	public int[] stone(){
		return new int[]{ cell( -5, -1 ), cell( -6, -1 ), cell( -4, -1 ), cell( -6, 0 ), cell( -5, 0 ), cell( -4, 0 ) };
	}

	/** The wall stub. */
	public int[] wall(){
		return new int[]{ cell( -4, 3 ), cell( -4, 4 ) };
	}

	// ------------------------------------------------------------------ the stage itself

	private void carve(){
		for (int c : pool()) set( c, Terrain.WATER );
		set( inlet(), Terrain.WATER );
		int[] g = grass();
		for (int i = 0; i < g.length; i++) set( g[i], i < 3 ? Terrain.HIGH_GRASS : Terrain.GRASS );
		for (int c : stone()) set( c, Terrain.EMPTY_SP );
		for (int c : wall()) set( c, Terrain.WALL );
		GameScene.updateMap();
		Dungeon.level.cleanWalls();
		GameScene.updateFog();
	}

	private static void set( int cell, int terrain ){
		if (cell == -1) return;
		Level level = Dungeon.level;
		if (Actor.findChar( cell ) instanceof Hero) return;
		Level.set( cell, terrain );
		level.visited[cell] = true;
		level.mapped[cell] = true;
	}

	private void dummies(){
		dryRat = hardy( dryRat, Rat.class, cell( -2, 2 ) );
		wetRat = hardy( wetRat, Rat.class, cell( 4, 0 ) );
		skeleton = hardy( skeleton, Skeleton.class, cell( 1, 3 ) );
		bat = hardy( bat, Bat.class, cell( 5, -1 ) );
	}

	//a dummy standing where it was put, as hale as at first: brought back if it died
	private static Mob hardy( Mob was, Class<? extends Mob> kind, int cell ){
		if (cell == -1) return was;
		Mob mob = was;
		if (mob == null || !mob.isAlive() || !Dungeon.level.mobs.contains( mob )){
			Char occupant = Actor.findChar( cell );
			if (occupant != null) return was;
			mob = DebugScenes.spawn( kind, cell );
			if (mob == null) return was;
		} else if (mob.pos != cell && Actor.findChar( cell ) == null){
			mob.pos = cell;
			if (mob.sprite != null) mob.sprite.place( cell );
		}
		mob.HP = mob.HT = HARDY;
		mob.state = mob.PASSIVE;
		if (mob.sprite != null){
			for (CharSprite.State st : CharSprite.State.values()) mob.sprite.remove( st );
		}
		return mob;
	}

	/**
	 * Back to how it was built, at each page's start: the terrain carved again, the room's blobs
	 * cleared, the dummies brought back hale to their spots, every puppet gone, every pour stopped,
	 * every timer dropped and the weather let go.
	 */
	public void reset(){
		for (Puppet p : puppets) p.remove();
		puppets.clear();
		for (Emitter e : pours) e.on = false;
		pours.clear();
		timers.clear();
		clearBlobs();
		if (skyHeld){
			DebugScenes.clearWeather();
			skyHeld = false;
		}
		carve();
		dummies();
	}

	/** Every blob in the room cleared. */
	public void clearBlobs(){
		Level level = Dungeon.level;
		if (level.blobs == null) return;
		for (int dy = -DebugScenes.Room.HALF_H; dy <= DebugScenes.Room.HALF_H; dy++){
			for (int dx = -DebugScenes.Room.HALF_W; dx <= DebugScenes.Room.HALF_W; dx++){
				int c = cell( dx, dy );
				if (c == -1) continue;
				for (Blob b : level.blobs.values()) b.clear( c );
			}
		}
	}

	/** A blob seeded on cells (the real thing: it stays as it is while the hero does not act). */
	public void seed( Class<? extends Blob> kind, int amount, int... cells ){
		for (int c : cells){
			if (c != -1) GameScene.add( Blob.seed( c, amount, kind ) );
		}
	}

	/** The weather held (a type of precipitation at a rate, a wind from the east, a storm) till the
	 *  next page, as the weather scenes hold it (DebugScenes.Sky). */
	public void sky( PrecipType type, float rate, float wind, boolean storm ){
		new DebugScenes.Sky( type, rate, wind, 90f, storm ).hold();
		skyHeld = true;
	}

	/** A factory poured over a cell for `seconds`, then stopped (and stopped at the next page). */
	public Emitter pour( int cell, Emitter.Factory factory, float interval, float seconds ){
		Emitter e = CellEmitter.get( cell );
		e.pour( factory, interval );
		pours.add( e );
		after( seconds, () -> e.on = false );
		return e;
	}

	/** A factory burst over a cell. */
	public void burst( int cell, Emitter.Factory factory, int n ){
		if (cell != -1) CellEmitter.get( cell ).burst( factory, n );
	}

	/** A factory burst from a cell's middle. */
	public void burstCenter( int cell, Emitter.Factory factory, int n ){
		if (cell != -1) CellEmitter.center( cell ).burst( factory, n );
	}

	/** Runs `r` `seconds` from now on the render thread, unless the page ends first. */
	public void after( float seconds, Runnable r ){
		timers.add( new Timer( seconds, r ) );
	}

	/** Once a frame, from the gallery's runner: the timers that are due. */
	void update( float dt ){
		for (int i = 0; i < timers.size(); i++){
			Timer t = timers.get( i );
			if ((t.left -= dt) <= 0){
				timers.remove( i-- );
				t.run.run();
			}
		}
	}

	private static final class Timer {
		float left;
		final Runnable run;
		Timer( float left, Runnable run ){
			this.left = left;
			this.run = run;
		}
	}

	// ------------------------------------------------------------------ puppets

	/** A puppet of a kind at a cell: a sprite linked to a dummy character on no level. */
	public Puppet puppet( Class<? extends Mob> kind, int cell ){
		Mob dummy = Reflection.newInstance( kind );
		if (dummy == null || cell == -1) return null;
		dummy.pos = cell;
		dummy.state = dummy.PASSIVE;
		dummy.HP = dummy.HT = HARDY;
		GameScene.addSprite( dummy );
		Puppet p = new Puppet( dummy );
		puppets.add( p );
		return p;
	}

	/** A sprite the gallery moves by hand. */
	public final class Puppet {
		public final Mob ch;
		public final CharSprite sprite;

		Puppet( Mob ch ){
			this.ch = ch;
			this.sprite = ch.sprite;
		}

		/** A step to a cell next to it, its cell set first. */
		public void move( int to ){
			int from = ch.pos;
			ch.pos = to;
			sprite.move( from, to );
			after( CharSprite.DEFAULT_MOVE_INTERVAL + 0.05f, sprite::idle );
		}

		/** A jump to a cell. */
		public void jump( int to ){
			int from = ch.pos;
			ch.pos = to;
			sprite.jump( from, to, null );
		}

		/** Put on a cell at once. */
		public void place( int cell ){
			ch.pos = cell;
			sprite.place( cell );
		}

		/** Its strike at a cell; `then` as it lands (its anim's end). */
		public void attack( int cell, Runnable then ){
			sprite.attack( cell, done( then ) );
		}

		/** Its zap at a cell; `then` as it goes off (or after half a second, for a kind with no zap
		 *  of its own, which would never end it). */
		public void zap( int cell, Runnable then ){
			sprite.zap( cell, done( then ) );
		}

		private Callback done( final Runnable then ){
			final boolean[] ran = { false };
			final Runnable once = () -> {
				if (ran[0]) return;
				ran[0] = true;
				sprite.idle();
				if (then != null) then.run();
			};
			after( 0.5f, once );
			return once::run;
		}

		/** Off the stage, its pictures let go. */
		void remove(){
			Fx.dispose( sprite );
		}
	}
}
