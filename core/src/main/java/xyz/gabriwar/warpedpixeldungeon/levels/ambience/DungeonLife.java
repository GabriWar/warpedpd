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

package xyz.gabriwar.warpedpixeldungeon.levels.ambience;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CampFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.FiendFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.HalomethaneFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.HellFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Inferno;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.MagicFire;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.effects.CritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.SliceCritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherBlobFX;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.CaveMoteParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DriftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DripParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FireflyParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.EyeOfNewt;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.mechanics.ShadowCaster;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.IntPredicate;

/**
 * The small life of the dungeon's floors, around the hero: frogs on the sewers' banks and
 * striders on their water, roaches along the wall bases, moths round the prison's torches and
 * spiders on their threads, newts and blind fish in the caves, swifts on the city's statues,
 * salamanders by the halls' cold lava and crows on their skull pillars, hares and buntings in the
 * ice, flies struggling in the nest's webbing - and the air of each place: gnats over the water at
 * dusk, drips from the roof, glow-worms, spores, embers, drifting silk, dust, fireflies. Who lives
 * where is docs/ambience.md; Fauna keeps the table, Habitat finds the ground on the map.
 *
 * Like the surface's (OverworldCritters.Field) none of it is an Actor and none of it is saved: an
 * invisible Gizmo in the scene's update loop, on the render thread, puts pictures into the scene
 * - on the floor under everyone and under the walls' tops what walks and swims (floorEffect), in
 * the effects over the walls what flies - and sends them away again. Every half second one kind
 * has its turn: a cell of its ground the hero sees, on screen, off his path, with nobody, no heap,
 * no trap and no fire on it, clear of everyone and apart from the others out. Every frame it
 * watches the hero come near them (and four times a second everyone else), hears the fights in
 * sight (noise), and sends away what is far off or has long been out of his sight. Most of the
 * night's life is out at dusk and at night: the clock reaches the dungeon.
 *
 * GameScene puts one in the scene of every floor with an ambience below ground (forLevel); the
 * newest is the one the fights are heard by. The dungeon never slides, so the floor's map is the
 * one the pictures stand on, and they are drawn where they are.
 */
public final class DungeonLife extends Gizmo {

	private static final float SIZE = DungeonTilemap.SIZE;

	//pictures this far (Chebyshev, scene px) from the hero leave: twelve cells
	static final float DESPAWN_PX = 192f;
	//a picture out of the hero's sight this long has gone by the time he looks again
	static final float UNSEEN_S = 3.5f;
	//no two critters turn up nearer each other than this (scene px)
	static final float APART_PX = 28f;
	//cells of a kind's ground tried at its turn: a handful of what the last look found
	static final int PROBES = 6;
	//the widest ring anything turns up in, cells
	static final int REACH = 9;
	//the sight of a lit floor, cells: the nearness everything heeds the hero at is set for it
	static final int FULL_SIGHT = 8;
	//a flock looks up when the hero comes this near; a hare sits up
	private static final float WARY_PX = 88f, ALERT_PX = 104f;
	//the while a flock or a hare stays, seconds: the surface's birds and hares keep none of their
	//own, and a hero fishing on the shore would have the same gulls all day
	static final float STAY_MIN = 45f, STAY_MAX = 100f;
	//birds on statues this near each other are a row, and go together
	private static final float ROW_PX = 56f;
	//the look round again while the hero stands still: grass burns, water freezes, the dark comes on
	static final float RESCAN = 2f;
	//the top of a statue, px above its cell: the city's dwarves stand four pixels into the cell
	//above, the halls' skull pillars six; a bird's feet stand a pixel into the top
	private static final int PERCH_CITY = 3, PERCH_HALLS = 5;
	//a snow bunting: the finch's art paled to winter white
	private static final int BUNTING_TINT = 0xF4F7FF;

	//what burns: nothing turns up in it
	private static final List<Class<? extends Blob>> FIRES = Arrays.asList(
			Fire.class, Inferno.class, HellFire.class, FiendFire.class, HalomethaneFire.class, MagicFire.class, CampFire.class );

	//the render thread's own dice: never the dungeon's seeded generators
	static final java.util.Random RNG = new java.util.Random();

	static float rf( float min, float max ){
		return min + RNG.nextFloat() * (max - min);
	}

	static int ri( int min, int max ){
		return min + RNG.nextInt( max - min + 1 );
	}

	static float cheb( float ax, float ay, float bx, float by ){
		return Math.max( Math.abs( ax - bx ), Math.abs( ay - by ) );
	}

	// ------------------------------------------------------------ the noise

	//the life showing now: the noise goes nowhere without one
	private static volatile DungeonLife current;

	//fights seen since the last frame, by cell: written by whichever thread resolved the attack,
	//drained by the life on the render thread
	private static final ArrayList<Integer> NOISES = new ArrayList<>();

	/** Char.attack: a fight in sight scatters the small life around it. Any thread. */
	public static void noise( int cell ){
		DungeonLife life = current;
		if (life == null || Dungeon.level != life.level) return;
		synchronized (NOISES){
			if (NOISES.size() < 16) NOISES.add( cell );
		}
	}

	/** The life of a floor: one for every floor with an ambience below ground, none elsewhere (the overworld has its own). */
	public static DungeonLife forLevel( Level level ){
		Place p = level == null ? null : level.ambience();
		return p != null && p.underground() ? new DungeonLife( level, p ) : null;
	}

	// ------------------------------------------------------------ the pictures

	/** A picture out, of a kind; a bird belongs to its flock. */
	static final class Critter {
		final Fauna.Kind kind;
		final CritterSprite sprite;
		final Flock flock;
		//seconds out, and out of the hero's sight
		float out, unseen;
		//seconds out before it goes its way unbidden: the dungeon's own creatures keep their while
		//themselves, the surface's birds and hares are given one (a flock's birds all the same)
		float stay = Float.MAX_VALUE;

		Critter( Fauna.Kind kind, CritterSprite sprite, Flock flock ){
			this.kind = kind;
			this.sprite = sprite;
			this.flock = flock;
		}
	}

	/** Birds that came down on the ground together and leave together: buntings, finches, gulls. */
	static final class Flock {
		final Fauna.Kind kind;
		final ArrayList<CritterSprite.Bird> birds = new ArrayList<>();
		boolean scared;

		Flock( Fauna.Kind kind ){
			this.kind = kind;
		}

		boolean gone(){
			for (CritterSprite.Bird b : birds) if (b.exists) return false;
			return true;
		}

		//the nearest bird still down; one still gliding in counts where it will sit
		float nearest( float px, float py ){
			float best = Float.MAX_VALUE;
			for (CritterSprite.Bird b : birds){
				if (!b.exists || !b.grounded()) continue;
				best = Math.min( best, b.landing() ? cheb( b.ax, b.ay, px, py ) : cheb( b.gx(), b.gy(), px, py ) );
			}
			return best;
		}

		void wary( boolean on ){
			for (CritterSprite.Bird b : birds) if (b.exists) b.wary( on );
		}

		//all of them, away from (sx, sy) on headings a little apart, the nearest first
		void scare( final float sx, final float sy ){
			if (scared) return;
			scared = true;
			float cx = 0f, cy = 0f;
			int n = 0;
			for (CritterSprite.Bird b : birds) if (b.exists){ cx += b.gx(); cy += b.gy(); n++; }
			if (n == 0) return;
			cx /= n;
			cy /= n;
			float away = Math.abs( cx - sx ) + Math.abs( cy - sy ) < 2f ? rf( 0f, 6.2832f ) : (float)Math.atan2( cy - sy, cx - sx );
			ArrayList<CritterSprite.Bird> order = new ArrayList<>( birds );
			Collections.sort( order, (a, b) -> Float.compare( cheb( a.gx(), a.gy(), sx, sy ), cheb( b.gx(), b.gy(), sx, sy ) ) );
			float delay = 0f;
			for (CritterSprite.Bird b : order){
				if (!b.exists) continue;
				b.takeOff( away + rf( -0.45f, 0.45f ), delay );
				delay += rf( 0.03f, 0.10f );
			}
		}
	}

	/** Gnats or flies hanging over one cell a while. */
	static final class Cloud {
		final Fauna.Air air;
		final int cell;
		float left, next;

		Cloud( Fauna.Air air, int cell, float left ){
			this.air = air;
			this.cell = cell;
			this.left = left;
		}
	}

	//a glow-worm on the rock: a pale green point that brightens and dims where it clings
	private static final Emitter.Factory GLOW_WORM = new Emitter.Factory(){
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			((DriftParticle) e.recycle( DriftParticle.class )).look( WeatherSprites.GLOW_3, 0x9CF6D8, rf( 3f, 6f ), 0.75f )
					.at( x, y ).sway( 0.15f, 1.6f, 0f ).pulse( 1.4f ).fades( 0.3f, 0.3f );
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};

	//a strand of loose silk adrift in the nest's still air
	private static final Emitter.Factory SILK = new Emitter.Factory(){
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			((DriftParticle) e.recycle( DriftParticle.class )).look( WeatherSprites.WISP_S, 0xE6E2EE, rf( 4f, 7f ), 0.32f )
					.at( x, y ).motion( 0f, 2.5f, 3f, 1.5f, 0f, 0f ).sway( 3f, 1.2f, 0.2f ).fades( 0.3f, 0.35f );
		}
	};

	// ------------------------------------------------------------ the life

	final Level level;
	final Place place;
	private final Fauna.Kind[] kinds;
	private final Fauna.Air[] air;
	final ArrayList<Critter> live = new ArrayList<>();
	final ArrayList<Flock> flocks = new ArrayList<>();
	final ArrayList<Cloud> clouds = new ArrayList<>();
	//the ground the last look round found for each kind and each air, and how much of it
	private final int[][] spots, airSpots;
	private final int[] spotsN, airSpotsN;
	private final float[] airIn;
	private float thinkIn = 1f, charsIn, rescanIn;
	private int turn, scanCell = -1;
	//the hero's sight, cells, as his field of view has it: the halls' and the mines' are short,
	//and so is everyone's on a dark night, and there the life turns up close
	int sight = FULL_SIGHT;
	//the hero's last cell and the way he last stepped: nothing turns up on his path
	private int lastCell = -1, headX, headY;

	private DungeonLife( Level level, Place place ){
		this.level = level;
		this.place = place;
		kinds = Fauna.kinds( place );
		air = Fauna.air( place );
		int area = (2 * REACH + 1) * (2 * REACH + 1);
		spots = new int[kinds.length][area];
		spotsN = new int[kinds.length];
		airSpots = new int[air.length][area];
		airSpotsN = new int[air.length];
		airIn = new float[air.length];
		for (int i = 0; i < airIn.length; i++) airIn[i] = rf( 0.5f, 2f );
		synchronized (NOISES){
			NOISES.clear();
		}
		current = this;
	}

	@Override
	public void destroy(){
		super.destroy();
		if (current == this) current = null;
		//nothing it put out may linger with nobody left to send it away. A picture whose layer
		//was torn down first (the scene going) has no parent and was destroyed with it: never
		//twice, or its vertex buffer would be deleted again under another picture's
		for (Critter c : live) if (c.sprite.exists && c.sprite.parent != null) c.sprite.vanish();
		live.clear();
		flocks.clear();
		clouds.clear();
	}

	@Override
	public void update(){
		if (Dungeon.level != level || Dungeon.hero == null || Dungeon.hero.sprite == null || level.heroFOV == null) return;
		long tm = LagMonitor.begin();
		prune();
		//time stands still (the hourglass, swiftthistle): so does the small life
		if (!Emitter.freezeEmitters){
			float dt = Game.elapsed;
			CharSprite hs = Dungeon.hero.sprite;
			float hx = hs.x + hs.width() / 2f, hy = hs.y + hs.height() - 2f;
			int hcell = Dungeon.hero.pos;
			if (hcell >= 0 && hcell < level.length()){
				heading( hcell );
				rescanIn -= dt;
				if (hcell != scanCell || rescanIn <= 0f){
					scanCell = hcell;
					rescanIn = RESCAN;
					scan( hcell );
				}
				hear();
				react( hx, hy, true );
				//everyone else four times a second
				charsIn -= dt;
				if (charsIn <= 0f){
					charsIn = 0.25f;
					if (!live.isEmpty()){
						for (CharSprite s : others()) react( s.x + s.width() / 2f, s.y + s.height() - 2f, false );
					}
				}
				tend( dt, hx, hy );
				if ((thinkIn -= dt) <= 0f){
					thinkIn = rf( 0.4f, 0.6f );
					think( hcell );
				}
				air( dt );
			}
		}
		LagMonitor.end( "dungeon life", tm );
	}

	private void prune(){
		for (int i = live.size() - 1; i >= 0; i--) if (!live.get( i ).sprite.exists) live.remove( i );
		for (int i = flocks.size() - 1; i >= 0; i--) if (flocks.get( i ).gone()) flocks.remove( i );
	}

	//the way the hero last stepped
	private void heading( int hcell ){
		if (hcell == lastCell) return;
		int w = level.width();
		if (lastCell >= 0){
			headX = Integer.signum( hcell % w - lastCell % w );
			headY = Integer.signum( hcell / w - lastCell / w );
		}
		lastCell = hcell;
	}

	//a look round the hero at each of his steps (and every two seconds): the ground of every kind
	//and every air within its ring, kept for their turns - nothing scans the floor each frame
	private void scan( int hcell ){
		final int[] map = level.map;
		final int w = level.width(), h = level.height();
		sight = Math.min( REACH, heroSight( Dungeon.hero ) );
		int hx = hcell % w, hy = hcell / w;
		for (int i = 0; i < kinds.length; i++){
			final Fauna.Kind k = kinds[i];
			int far = Math.min( k.far, sight ), near = Math.min( k.near, far );
			spotsN[i] = ring( hx, hy, near, far, spots[i], c -> Habitat.groundFor( k, place, map, w, h, c ) );
		}
		for (int i = 0; i < air.length; i++){
			final Fauna.Air a = air[i];
			int far = Math.min( a.far, sight ), near = Math.min( a.near, far );
			airSpotsN[i] = ring( hx, hy, near, far, airSpots[i], c -> airAt( a, c ) );
		}
	}

	/**
	 * How far the hero sees, cells, as his field of view reckons it (Level.updateFieldOfView): his
	 * view distance, wider for the Farsight talent and narrower for the Eye of Newt, less what the
	 * dark, the weather and his tiredness take - never under two. His view distance alone is the
	 * sight of a lit floor: on a dark night the life would turn up where he cannot see it, at
	 * reaches he can never see past.
	 */
	static int heroSight( Hero hero ){
		float d = hero.viewDistance * (1f + 0.25f * hero.pointsInTalent( Talent.FARSIGHT )) * EyeOfNewt.visionRangeMultiplier();
		return Math.round( Math.max( 2f, d + DayNightCycle.viewDistanceModifier() ) );
	}

	//the cells from near to far (Chebyshev) of (hx, hy) that pass, into `out`: how many
	private int ring( int hx, int hy, int near, int far, int[] out, IntPredicate ok ){
		int w = level.width(), h = level.height(), n = 0;
		for (int y = Math.max( 1, hy - far ); y <= Math.min( h - 2, hy + far ); y++){
			for (int x = Math.max( 1, hx - far ); x <= Math.min( w - 2, hx + far ); x++){
				if (Math.max( Math.abs( x - hx ), Math.abs( y - hy ) ) < near) continue;
				int c = x + y * w;
				if (ok.test( c )) out[n++] = c;
			}
		}
		return n;
	}

	//the air's ground, and the flies' remains: bones and remains lie as heaps, not terrain
	private boolean airAt( Fauna.Air a, int cell ){
		if (Habitat.airFor( a, place, level.map, level.width(), level.height(), cell )) return true;
		if (a != Fauna.Air.FLIES) return false;
		Heap heap = level.heaps.get( cell );
		return heap != null && (heap.type == Heap.Type.SKELETON || heap.type == Heap.Type.REMAINS);
	}

	// ------------------------------------------------------------ turns

	private void think( int hcell ){
		Phase phase = DayNightCycle.phase();
		//the hour turned against some of them: they go, a few at a time; and a flock or a hare
		//whose while is up goes too, so the room's life changes while the hero stands in it
		for (Critter c : live){
			if (!calm( c )) continue;
			if (c.out >= c.stay || (Fauna.hour( c.kind, phase ) <= 0f && RNG.nextFloat() < 0.15f)) dismiss( c );
		}
		if (kinds.length == 0) return;
		int i = turn++ % kinds.length;
		Fauna.Kind k = kinds[i];
		float chance = k.chance * Fauna.hour( k, phase ) * Fauna.rate( place );
		if (spotsN[i] == 0 || RNG.nextFloat() >= chance) return;
		if (census( k ) >= Fauna.cap( k, place ) || total() >= Fauna.GLOBAL_CAP) return;
		float[] people = people();
		for (int t = 0; t < PROBES; t++){
			int cell = spots[i][RNG.nextInt( spotsN[i] )];
			if (free( cell, hcell, k, people ) && putOut( k, i, cell, people )) return;
		}
	}

	/** How many of a kind are out and unafraid (of birds, how many flocks still down). */
	int census( Fauna.Kind k ){
		int n = 0;
		if (k.flocks()){
			for (Flock f : flocks) if (f.kind == k && !f.scared && !f.gone()) n++;
			return n;
		}
		for (Critter c : live) if (c.kind == k && calm( c )) n++;
		return n;
	}

	/** Every picture out, going or not: the global cap counts them all, a flock by its birds. */
	int total(){
		int n = 0;
		for (Critter c : live) if (c.sprite.exists) n++;
		return n;
	}

	//out and unafraid: something coming near can still startle it
	static boolean calm( Critter c ){
		CritterSprite s = c.sprite;
		if (!s.exists || s.leaving()) return false;
		if (c.flock != null) return !c.flock.scared && ((CritterSprite.Bird) s).grounded();
		if (s instanceof DungeonCritterSprite) return ((DungeonCritterSprite) s).calm();
		if (s instanceof CritterSprite.Hare) return ((CritterSprite.Hare) s).sitting();
		return true;
	}

	//sent home by the hour: off the way each goes when something comes, from somewhere to one side
	private void dismiss( Critter c ){
		CritterSprite s = c.sprite;
		float a = rf( 0f, 6.2832f );
		float sx = s.gx() + (float)Math.cos( a ) * 48f, sy = s.gy() + (float)Math.sin( a ) * 48f;
		if (c.flock != null){
			c.flock.scare( sx, sy );
		} else if (s instanceof CritterSprite.Hare){
			bolt( (CritterSprite.Hare) s, sx, sy );
		} else if (s instanceof DungeonCritterSprite){
			DungeonCritterSprite d = (DungeonCritterSprite) s;
			frighten( d, sx, sy );
			//what does not run (a snail in its shell, a fly in the web) fades where it is
			if (d.calm()) d.leave( 1.2f );
		} else {
			s.leave( 1f );
		}
	}

	// ------------------------------------------------------------ where

	//may a picture turn up here: in sight and on screen, off the hero's path, nobody on it, no
	//heap, no trap, no fire, apart from the others out, and clear of everyone by its reach
	private boolean free( int cell, int hcell, Fauna.Kind k, float[] people ){
		if (!level.heroFOV[cell] || !onScreen( cell ) || onPath( cell, hcell ) || standing( cell )) return false;
		if (level.heaps.get( cell ) != null || level.map[cell] == Terrain.TRAP || burning( cell )) return false;
		int w = level.width();
		float px = (cell % w + 0.5f) * SIZE, py = (cell / w + 0.5f) * SIZE;
		return apart( px, py ) && clearOfChars( px, py, people, reach( k.scare ) + 8f );
	}

	//no picture out within APART_PX of (px, py), where it stands
	private boolean apart( float px, float py ){
		for (Critter c : live){
			if (c.sprite.exists && cheb( c.sprite.ax, c.sprite.ay, px, py ) < APART_PX) return false;
		}
		return true;
	}

	private boolean burning( int cell ){
		for (Class<? extends Blob> f : FIRES){
			Blob b = level.blobs.get( f );
			if (b != null && b.volume > 0 && b.cur != null && b.cur[cell] > 0) return true;
		}
		return false;
	}

	/**
	 * A kind's reach (scene px) in the hero's sight: where he sees only two or three cells (the
	 * halls, the mines) everything lets him nearer, or he would never see it.
	 */
	float reach( float px ){
		return Math.min( px, (sight - 0.75f) * SIZE );
	}

	/**
	 * How near the hero `d` px off seems to a creature of this reach (its scare, px), for the
	 * nearness it heeds him at (a frog crouching, a crow looking up). In a lit floor's sight, as
	 * near as he is. In a shorter one the stretch between its reach and the edge of his sight
	 * stands for the stretch between its reach and the edge of a lit floor's, and within its reach
	 * he is nearer still: else every crow on a pillar he can see would watch him the whole while,
	 * and every frog in a dark night's sight crouch, never croaking.
	 */
	float seems( float d, float px ){
		if (sight >= FULL_SIGHT || px <= 0f) return d;
		float r = reach( px );
		if (d <= r) return d * px / r;
		return px + (d - r) * (FULL_SIGHT * SIZE - px) / (sight * SIZE - r);
	}

	/** Is (gx, gy) more than `px` (Chebyshev, scene px) from every point of `pts` (x0,y0,x1,y1...)? */
	static boolean clearOfChars( float gx, float gy, float[] pts, float px ){
		for (int i = 0; i + 1 < pts.length; i += 2){
			if (cheb( gx, gy, pts[i], pts[i+1] ) <= px) return false;
		}
		return true;
	}

	boolean onScreen( int cell ){
		Camera cam = Camera.main;
		if (cam == null) return false;
		float x = (cell % level.width()) * SIZE, y = (cell / level.width()) * SIZE;
		return x >= cam.scroll.x + 8 && x + SIZE <= cam.scroll.x + cam.width - 8
				&& y >= cam.scroll.y + 8 && y + SIZE <= cam.scroll.y + cam.height - 8;
	}

	//on the line he is walking (to where he is headed, and five cells straight on)
	private boolean onPath( int cell, int hcell ){
		int w = level.width();
		float x = cell % w + 0.5f, y = cell / w + 0.5f;
		float hx = hcell % w + 0.5f, hy = hcell / w + 0.5f;
		HeroAction a = Dungeon.hero.curAction;
		if (a != null && a.dst >= 0 && a.dst < level.length()
				&& Habitat.nearSegment( x, y, hx, hy, a.dst % w + 0.5f, a.dst / w + 0.5f, 1.5f )) return true;
		return (headX != 0 || headY != 0) && Habitat.nearSegment( x, y, hx, hy, hx + 5 * headX, hy + 5 * headY, 1.5f );
	}

	//everyone but the hero the critters mind, by sprite - each read once, since the actor thread
	//may drop one at any moment. a network client registers nobody with the scheduler: the host's
	//mobs live only in its level's mobs
	private ArrayList<CharSprite> others(){
		ArrayList<CharSprite> out = new ArrayList<>();
		if (NetManager.isNetClient()){
			for (Mob m : level.mobs){
				CharSprite s = m.sprite;
				if (s != null && !betrays( m, s )) out.add( s );
			}
		} else {
			for (Char ch : Actor.chars()){
				CharSprite s = ch.sprite;
				if (ch != Dungeon.hero && s != null && !betrays( ch, s )) out.add( s );
			}
		}
		return out;
	}

	//one a critter running from would give away: out of the hero's sight (its sprite hidden), or
	//keeping still in its disguise - a mimic as a chest, a statue as stone
	private static boolean betrays( Char ch, CharSprite s ){
		return !s.visible || (ch instanceof Mob && ((Mob) ch).state == ((Mob) ch).PASSIVE);
	}

	private boolean standing( int cell ){
		return NetManager.isNetClient() ? level.findMob( cell ) != null : Actor.findChar( cell ) != null;
	}

	//where everyone stands, the hero first, in scene px (x0,y0,x1,y1...)
	private float[] people(){
		ArrayList<CharSprite> others = others();
		float[] pts = new float[others.size() * 2 + 2];
		CharSprite hs = Dungeon.hero.sprite;
		pts[0] = hs.x + hs.width() / 2f;
		pts[1] = hs.y + hs.height() - 2f;
		int n = 2;
		for (CharSprite s : others){
			pts[n++] = s.x + s.width() / 2f;
			pts[n++] = s.y + s.height() - 2f;
		}
		return pts;
	}

	// ------------------------------------------------------------ out

	//what walks and swims lies on the floor, under everyone and under the walls' tops; its
	//thread, if it hangs on one, under it. In front of what is out already: the group fills empty
	//slots first, so without this a new one could sort behind an old
	private Critter onFloor( Fauna.Kind k, CritterSprite s ){
		if (s instanceof DungeonCritterSprite){
			Visual u = ((DungeonCritterSprite) s).under();
			if (u != null){
				GameScene.floorEffect( u );
				front( u );
			}
		}
		GameScene.floorEffect( s );
		front( s );
		Critter c = new Critter( k, s, null );
		live.add( c );
		return c;
	}

	//what flies, over the walls
	private Critter inAir( Fauna.Kind k, CritterSprite s, Flock f ){
		GameScene.effect( s );
		front( s );
		Critter c = new Critter( k, s, f );
		live.add( c );
		return c;
	}

	private static void front( Visual v ){
		if (v.parent != null) v.parent.bringToFront( v );
	}

	//a creature of the floor took wing: up from under the walls into the air over them. Done here,
	//before the floor's own update, so it is never moved twice in a frame
	private void lift( DungeonCritterSprite s ){
		if (s.parent != null) s.parent.erase( s );
		GameScene.effect( s );
		front( s );
		s.lifted();
	}

	//a picture of this kind on this cell; false if the ground there has no room for it after all
	private boolean putOut( Fauna.Kind k, int i, int cell, float[] people ){
		int[] map = level.map;
		int w = level.width();
		float x0 = (cell % w) * SIZE, y0 = (cell / w) * SIZE;
		switch (k){
			case FROG: {
				int water = Habitat.waterBeside( map, w, cell );
				if (water < 0){
					onFloor( k, new DungeonCritterSprite.Frog( x0 + ri( 4, 12 ), y0 + ri( 8, 12 ), 0f, 0f, false ) );
					return true;
				}
				//on the bank's water side; it dives into the middle of the water beside it
				int dx = water % w - cell % w, dy = water / w - cell / w;
				float ax = x0 + 8 + dx * 4 + (dy != 0 ? ri( -3, 3 ) : 0);
				float ay = y0 + (dy > 0 ? 13 : dy < 0 ? 7 : ri( 9, 12 ));
				onFloor( k, new DungeonCritterSprite.Frog( ax, ay,
						(water % w) * SIZE + 8 + ri( -2, 2 ), (water / w) * SIZE + 10, true ) );
				return true;
			}
			case ROACH: case CENTIPEDE: case SILVERFISH: case SPIDERLING: {
				DungeonCritterSprite.Scuttler.Look look = k == Fauna.Kind.ROACH ? DungeonCritterSprite.Scuttler.Look.ROACH
						: k == Fauna.Kind.CENTIPEDE ? DungeonCritterSprite.Scuttler.Look.CENTIPEDE
						: k == Fauna.Kind.SILVERFISH ? DungeonCritterSprite.Scuttler.Look.SILVERFISH
						: DungeonCritterSprite.Scuttler.Look.SPIDERLING;
				float ay = k == Fauna.Kind.SPIDERLING ? y0 + ri( 8, 12 ) : Habitat.laneY( map, w, cell );
				onFloor( k, new DungeonCritterSprite.Scuttler( look, x0 + ri( 4, 12 ), ay, new Haunt( k ) ) );
				return true;
			}
			case MOUSE: {
				//tucked into the corner: against the side wall, at the foot of the face (a corner at
				//a room's bottom has the wall's lip drawn over it, and is no ground to stand on)
				float ax = x0 + (Habitat.rock( map[cell - 1] ) ? 4 : 12);
				onFloor( k, new DungeonCritterSprite.Mouse( ax, Habitat.laneY( map, w, cell ), new Haunt( k ) ) );
				return true;
			}
			case NEWT: case SALAMANDER: {
				int water = Habitat.waterBeside( map, w, cell );
				if (water < 0) return false;
				int dx = water % w - cell % w, dy = water / w - cell / w;
				float ax = x0 + 8 + dx * 3 + (dy != 0 ? ri( -3, 3 ) : 0);
				float ay = y0 + (dy > 0 ? 13 : dy < 0 ? 6 : ri( 9, 12 ));
				onFloor( k, new DungeonCritterSprite.Basker( k == Fauna.Kind.NEWT ? DungeonCritterSprite.Basker.Look.NEWT
						: DungeonCritterSprite.Basker.Look.SALAMANDER, ax, ay, new Haunt( k ),
						(water % w) * SIZE + 8 + ri( -2, 2 ), (water / w) * SIZE + 9, true ) );
				return true;
			}
			case LIZARD:
				//in the warmth under the vent
				onFloor( k, new DungeonCritterSprite.Basker( DungeonCritterSprite.Basker.Look.LIZARD,
						x0 + ri( 5, 11 ), Habitat.laneY( map, w, cell ), new Haunt( k ), 0f, 0f, false ) );
				return true;
			case SNAIL:
				onFloor( k, new DungeonCritterSprite.Snail( x0 + ri( 5, 11 ), y0 + ri( 8, 12 ) ) );
				return true;
			case BEETLE: case EMBER_BEETLE: {
				float ay = k == Fauna.Kind.BEETLE ? Habitat.laneY( map, w, cell ) : y0 + ri( 8, 12 );
				onFloor( k, new DungeonCritterSprite.Beetle( k == Fauna.Kind.EMBER_BEETLE, x0 + ri( 4, 12 ), ay, new Haunt( k ) ) );
				return true;
			}
			case STRIDER:
				onFloor( k, new DungeonCritterSprite.Strider( x0 + ri( 5, 11 ), y0 + ri( 5, 11 ) ) );
				return true;
			case FLY:
				onFloor( k, new DungeonCritterSprite.Fly( x0 + ri( 4, 12 ), y0 + ri( 7, 12 ) ) );
				return true;
			case SPIDER: {
				//its thread fast at the top of the face above the cell, let down over its foot
				float ax = x0 + ri( 4, 12 ), ay = y0 - SIZE + 1;
				onFloor( k, new DungeonCritterSprite.Spider( ax, ay, y0 + 4 - ay, ri( 9, 19 ) ) );
				return true;
			}
			case MOTH:
				return moths( cell );
			case SWIFT: case CROW:
				return perches( k, i, cell, people );
			case BUNTING: case FINCH: case GULL:
				return settle( k, cell, people );
			case HARE:
				return hare( cell );
			case FISH:
				return fish( cell );
			case BUTTERFLY:
				return butterflies( cell );
			default:
				return false;
		}
	}

	//moths round a light, one to three; where nothing burns, one flat on the face above the cell
	private boolean moths( int cell ){
		int w = level.width();
		float x0 = (cell % w) * SIZE, y0 = (cell / w) * SIZE;
		int tint = Fauna.mothTint( place );
		int room = Math.min( Fauna.cap( Fauna.Kind.MOTH, place ) - census( Fauna.Kind.MOTH ), Fauna.GLOBAL_CAP - total() );
		if (room <= 0) return false;
		if (!Habitat.lit( place )){
			float ax = x0 + ri( 4, 12 ), ay = y0 - ri( 4, 10 );
			inAir( Fauna.Kind.MOTH, new DungeonCritterSprite.Moth( ax, ay, y0 + 8 - ay, tint, false, false ), null );
			return true;
		}
		//the light, and the cell whose sight decides whether its moths show
		float lx = x0 + 8, ly, seen;
		if (place == Place.CITY || place == Place.VAULT){
			//the green flame burns at the pedestal's top
			ly = y0 - 3;
			seen = 11f;
		} else if (place == Place.HALLS){
			//over the glow of the cold lava, the embers
			ly = y0 + 3;
			seen = 5f;
		} else {
			//a torch, an ice seam, on the face. The fog shows a wall's face only with the open
			//cell before it in sight (a wall seen from the room behind it stays black), so that
			//cell decides: never moths over the back of a wall
			if (!level.heroFOV[cell + w]) return false;
			ly = y0 + 7;
			seen = 9f;
		}
		int n = Math.min( room, ri( 1, 3 ) );
		for (int k = 0; k < n; k++){
			//over the lava there is no stone to come down on
			inAir( Fauna.Kind.MOTH, new DungeonCritterSprite.Moth( lx, ly, seen, tint, true, place != Place.HALLS ), null );
		}
		return true;
	}

	//one bird to a statue's top, on this one and on up to two more near it: the swifts on the
	//city's dwarves, the carrion crows on the halls' skull pillars. They glide in together
	private boolean perches( Fauna.Kind k, int i, int cell, float[] people ){
		int w = level.width();
		int room = Math.min( 3, Math.min( Fauna.cap( k, place ) - census( k ), Fauna.GLOBAL_CAP - total() ) );
		ArrayList<Integer> tops = new ArrayList<>();
		tops.add( cell );
		for (int j = 0; j < spotsN[i] && tops.size() < room; j++){
			int c = spots[i][j];
			if (c == cell || Math.max( Math.abs( c % w - cell % w ), Math.abs( c / w - cell / w ) ) > 3) continue;
			float px = (c % w + 0.5f) * SIZE, py = (c / w + 0.5f) * SIZE;
			if (!level.heroFOV[c] || !apart( px, py ) || !clearOfChars( px, py, people, reach( k.scare ) + 8f )) continue;
			tops.add( c );
		}
		int top = place == Place.HALLS ? PERCH_HALLS : PERCH_CITY;
		float heading = rf( 0f, 6.2832f ), delay = 0f;
		for (int c : tops){
			float ax = (c % w) * SIZE + 8 + ri( -1, 1 ), ay = (c / w) * SIZE - top;
			//its sight is the statue's own cell
			DungeonCritterSprite.Percher b = new DungeonCritterSprite.Percher( k == Fauna.Kind.CROW, ax, ay, (c / w) * SIZE + 8 - ay );
			b.land( heading + rf( -0.3f, 0.3f ), delay );
			delay += rf( 0.08f, 0.2f );
			inAir( k, b, null );
		}
		return true;
	}

	//something frightened one: off it goes - and a bird off a statue sets the rest of its row
	//going after it, one after another, the nearest first
	private void frighten( DungeonCritterSprite s, float sx, float sy ){
		s.scare( sx, sy );
		if (!(s instanceof DungeonCritterSprite.Percher)) return;
		final float fx = s.gx(), fy = s.gy();
		ArrayList<DungeonCritterSprite.Percher> row = new ArrayList<>();
		for (Critter c : live){
			if (c.sprite != s && c.sprite instanceof DungeonCritterSprite.Percher && c.sprite.exists
					&& ((DungeonCritterSprite.Percher) c.sprite).calm() && cheb( c.sprite.ax, c.sprite.ay, s.ax, s.ay ) <= ROW_PX){
				row.add( (DungeonCritterSprite.Percher) c.sprite );
			}
		}
		Collections.sort( row, (a, b) -> Float.compare( cheb( a.ax, a.ay, fx, fy ), cheb( b.ax, b.ay, fx, fy ) ) );
		float away = (float)Math.atan2( fy - sy, fx - sx ), delay = 0f;
		for (DungeonCritterSprite.Percher p : row){
			delay += rf( 0.05f, 0.15f );
			p.flyOff( away + rf( -0.45f, 0.45f ), delay );
		}
	}

	//a flock of three to five on the cell and around it, gliding in together: the frozen branch's
	//snow buntings, the meadow's finches, the shore's gulls. False when the ground has no room
	private boolean settle( Fauna.Kind k, int cell, float[] people ){
		int w = level.width(), h = level.height();
		int n = Math.min( Fauna.GLOBAL_CAP - total(), ri( 3, 5 ) );
		if (n < 3) return false;
		ArrayList<float[]> at = new ArrayList<>();
		for (int t = 0; t < n * 4 && at.size() < n; t++){
			int c = cell + ri( -1, 1 ) + ri( -1, 1 ) * w;
			if (!Habitat.groundFor( k, place, level.map, w, h, c ) || standing( c ) || !level.heroFOV[c]) continue;
			float px = (c % w) * SIZE + ri( 3, 13 ), py = (c / w) * SIZE + ri( 8, 13 );
			if (!clearOfChars( px, py, people, reach( k.scare ) + 8f )) continue;
			boolean crowded = false;
			for (float[] o : at){
				if (Math.abs( o[0] - px ) < 6f && Math.abs( o[1] - py ) < 4f) crowded = true;
			}
			if (!crowded) at.add( new float[]{ px, py } );
		}
		if (at.size() < 3) return false;
		//the farther up the screen first, so the nearer stand in front
		Collections.sort( at, (a, b) -> Float.compare( a[1], b[1] ) );
		Flock f = new Flock( k );
		float heading = rf( 0f, 6.2832f ), delay = 0f, stay = rf( STAY_MIN, STAY_MAX );
		for (float[] o : at){
			CritterSprite.Bird b = new CritterSprite.Bird( k == Fauna.Kind.GULL ? CritterSprite.Bird.Kind.GULL : CritterSprite.Bird.Kind.FINCH,
					o[0], o[1], 0f );
			if (k == Fauna.Kind.BUNTING) b.tint( BUNTING_TINT, 0.55f );
			b.land( heading + rf( -0.3f, 0.3f ), delay );
			delay += rf( 0.06f, 0.14f );
			inAir( k, b, f ).stay = stay;
			f.birds.add( b );
		}
		flocks.add( f );
		return true;
	}

	//a hare: out of the cover beside it in two hops, or made out where it sat all along
	private boolean hare( int cell ){
		int w = level.width();
		float ax = (cell % w) * SIZE + 8, ay = (cell / w) * SIZE + 11;
		//the winter coat in the ice; the meadow's hare is brown
		CritterSprite.Hare h = new CritterSprite.Hare( place == Place.FROZEN, ax, ay );
		int cover = Habitat.coverBeside( level.map, w, cell );
		if (cover >= 0){
			h.emerge( (cover % w) * SIZE + 8 - ax, (cover / w) * SIZE + 11 - ay );
		} else {
			h.appear();
		}
		onFloor( Fauna.Kind.HARE, h ).stay = rf( STAY_MIN, STAY_MAX );
		return true;
	}

	//the hare's dash, worked out on the floor's map, as offsets from its ground point
	private void bolt( CritterSprite.Hare h, float sx, float sy ){
		int w = level.width();
		int cx = Math.max( 1, Math.min( w - 2, (int)(h.gx() / SIZE) ) );
		int cy = Math.max( 1, Math.min( level.height() - 2, (int)(h.gy() / SIZE) ) );
		Habitat.Dash r = Habitat.dash( level.map, w, level.height(), cx + cy * w, sx / SIZE, sy / SIZE, RNG );
		//its feet are three px below the cell's middle
		float[] pts = new float[r.pts.length];
		for (int k = 0; k + 1 < pts.length; k += 2){
			pts[k] = r.pts[k] * SIZE - h.ax;
			pts[k+1] = r.pts[k+1] * SIZE + 3f - h.ay;
		}
		h.bolt( pts, r.cover );
	}

	//a fish in open water: the caves' blind ones glide pale under the surface or now and then leap;
	//the ice fish leap silver, and the shore's (now and then a green river fish). Only the blind
	//fish glide: the swimmer under the surface is drawn as one
	private boolean fish( int cell ){
		int[] map = level.map;
		int w = level.width();
		float x = (cell % w) * SIZE + 8, y = (cell / w) * SIZE;
		boolean glides = place == Place.CAVES && RNG.nextFloat() < 0.7f;
		if (glides){
			onFloor( Fauna.Kind.FISH, new SliceCritterSprite.Swimmer( x, y + 9, rf( 0f, 6.2832f ) ) );
			return true;
		}
		//across the water, never toward the bank
		boolean left = Habitat.liquid( map[cell - 1] ), right = Habitat.liquid( map[cell + 1] );
		int dir = left && right ? (RNG.nextBoolean() ? 1 : -1) : right ? 1 : -1;
		float fx = x + rf( -3f, 3f ), fy = y + 10;
		CritterSprite f = place == Place.CAVES ? CritterSprite.Fish.blind( fx, fy, dir )
				: new CritterSprite.Fish( place == Place.SHORE && RNG.nextFloat() < 0.4f, fx, fy, dir );
		inAir( Fauna.Kind.FISH, f, null );
		return true;
	}

	//one or two over the flowers
	private boolean butterflies( int cell ){
		int w = level.width();
		int room = Math.min( Fauna.cap( Fauna.Kind.BUTTERFLY, place ) - census( Fauna.Kind.BUTTERFLY ), Fauna.GLOBAL_CAP - total() );
		int n = Math.min( room, ri( 1, 2 ) );
		for (int k = 0; k < n; k++){
			inAir( Fauna.Kind.BUTTERFLY, new CritterSprite.Butterfly( (cell % w) * SIZE + ri( 2, 14 ), (cell / w) * SIZE + ri( 6, 14 ) ), null );
		}
		return n > 0;
	}

	/** The ground a walker keeps to: Habitat's ways over this floor's map, on its lane in scene px. */
	private final class Haunt implements DungeonCritterSprite.Ground {

		private final Fauna.Kind kind;

		Haunt( Fauna.Kind kind ){
			this.kind = kind;
		}

		@Override
		public float[] wander( float x, float y ){
			int cell = cellOf( x, y );
			if (cell < 0) return null;
			return lane( Habitat.wander( kind, place, level.map, level.width(), level.height(), cell, RNG ) );
		}

		@Override
		public float[] flee( float x, float y, float sx, float sy ){
			int cell = cellOf( x, y );
			if (cell < 0) return null;
			return lane( Habitat.flee( kind, place, level.map, level.width(), level.height(), cell, sx / SIZE, sy / SIZE ) );
		}
	}

	//the cell under a scene point while this floor is the one shown, or -1
	private int cellOf( float x, float y ){
		if (Dungeon.level != level || x < 0 || y < 0) return -1;
		int cx = (int)(x / SIZE), cy = (int)(y / SIZE);
		if (cx >= level.width() || cy >= level.height()) return -1;
		return cx + cy * level.width();
	}

	//cells to waypoints: about the middle of each across, its lane down (the foot of a wall, or a
	//little below the middle)
	private float[] lane( int[] cells ){
		if (cells.length == 0) return null;
		int w = level.width();
		float[] pts = new float[cells.length * 2];
		for (int k = 0; k < cells.length; k++){
			int c = cells[k];
			pts[k*2] = (c % w) * SIZE + 8 + rf( -3f, 3f );
			pts[k*2+1] = Habitat.laneY( level.map, w, c ) + rf( -1f, 1f );
		}
		return pts;
	}

	// ------------------------------------------------------------ near

	//someone at (px, py): the hero (whom some also heed) or anyone else
	private void react( float px, float py, boolean hero ){
		for (Flock f : flocks){
			if (f.scared) continue;
			float d = f.nearest( px, py );
			if (d <= reach( hero ? f.kind.scare : f.kind.pass )) f.scare( px, py );
			else if (hero) f.wary( seems( d, f.kind.scare ) <= WARY_PX );
		}
		for (Critter c : live){
			CritterSprite s = c.sprite;
			if (c.flock != null || !s.exists) continue;
			float d = cheb( s.gx(), s.gy(), px, py );
			float r = reach( hero ? c.kind.scare : c.kind.pass );
			if (s instanceof DungeonCritterSprite){
				DungeonCritterSprite dc = (DungeonCritterSprite) s;
				if (hero) dc.heed( seems( d, c.kind.scare ) );
				//a bird still gliding in is measured where it will sit
				if (dc instanceof DungeonCritterSprite.Percher && ((DungeonCritterSprite.Percher) dc).landing()){
					d = cheb( s.ax, s.ay, px, py );
				}
				if (r > 0f && d <= r && dc.calm()) frighten( dc, px, py );
			} else if (s instanceof CritterSprite.Hare){
				CritterSprite.Hare h = (CritterSprite.Hare) s;
				if (!h.sitting()) continue;
				//a hare still hopping out of cover is measured where it will sit
				float hd = h.emerging() ? cheb( h.ax, h.ay, px, py ) : d;
				if (hd <= r) bolt( h, px, py );
				else if (hero) h.alert( seems( hd, c.kind.scare ) <= ALERT_PX );
			} else if (s instanceof CritterSprite.Butterfly && r > 0f && d <= r){
				((CritterSprite.Butterfly) s).startle( px, py );
			}
		}
	}

	//fights seen since the last frame
	private void hear(){
		int[] heard;
		synchronized (NOISES){
			if (NOISES.isEmpty()) return;
			heard = new int[NOISES.size()];
			for (int k = 0; k < heard.length; k++) heard[k] = NOISES.get( k );
			NOISES.clear();
		}
		int w = level.width();
		for (int cell : heard){
			float px = (cell % w + 0.5f) * SIZE, py = (cell / w + 0.5f) * SIZE;
			for (Flock f : flocks){
				if (!f.scared && f.nearest( px, py ) <= f.kind.noise) f.scare( px, py );
			}
			for (Critter c : live){
				CritterSprite s = c.sprite;
				if (c.flock != null || !s.exists || c.kind.noise <= 0f || cheb( s.gx(), s.gy(), px, py ) > c.kind.noise) continue;
				if (s instanceof DungeonCritterSprite){
					if (((DungeonCritterSprite) s).calm()) frighten( (DungeonCritterSprite) s, px, py );
				} else if (s instanceof CritterSprite.Hare && ((CritterSprite.Hare) s).sitting()){
					bolt( (CritterSprite.Hare) s, px, py );
				} else if (s instanceof CritterSprite.Butterfly){
					((CritterSprite.Butterfly) s).startle( px, py );
				}
			}
		}
	}

	//every frame: up into the air what took wing; away what is far off or long out of sight
	private void tend( float dt, float hx, float hy ){
		for (Critter c : live){
			CritterSprite s = c.sprite;
			if (!s.exists) continue;
			c.out += dt;
			if (s instanceof DungeonCritterSprite && ((DungeonCritterSprite) s).wantsAir()) lift( (DungeonCritterSprite) s );
			if (s.leaving()) continue;
			c.unseen = s.visible ? 0f : c.unseen + dt;
			if (c.unseen > UNSEEN_S || cheb( s.gx(), s.gy(), hx, hy ) > DESPAWN_PX) s.leave( 0.4f );
		}
	}

	// ------------------------------------------------------------ the air

	private void air( float dt ){
		Phase phase = DayNightCycle.phase();
		for (int i = 0; i < air.length; i++){
			if ((airIn[i] -= dt) > 0f) continue;
			Fauna.Air a = air[i];
			airIn[i] = rf( a.every, a.most );
			//out of its best hour it comes the less often, out of its hours not at all
			float h = Fauna.hour( a, phase ) * Fauna.rate( place );
			if (h <= 0f || airSpotsN[i] == 0 || RNG.nextFloat() >= h) continue;
			if (a.cloud) gather( a, i );
			else breathe( a, i );
		}
		clouds( dt, phase );
	}

	//a cell of the air's ground in sight and on screen, or -1
	private int airCell( int i ){
		for (int k = 0; k < 4; k++){
			int c = airSpots[i][RNG.nextInt( airSpotsN[i] )];
			if (level.heroFOV[c] && onScreen( c )) return c;
		}
		return -1;
	}

	//one of the air's particles, from one of its cells
	private void breathe( Fauna.Air a, int i ){
		int c = airCell( i );
		if (c < 0) return;
		Emitter e = DungeonCritterSprite.emitter();
		if (e == null) return;
		int w = level.width();
		float x0 = (c % w) * SIZE, y0 = (c / w) * SIZE;
		switch (a){
			case DRIPS:
				//straight down from the roof, landing on the point: a ring on the water, a splash by the rock
				e.pos( x0 + ri( 3, 13 ), y0 + ri( 4, 13 ) );
				e.burst( DripParticle.FALL, 1 );
				break;
			case GLOW_WORMS:
				//on the lower face of the rock, over the moss at its foot
				e.pos( x0 + ri( 2, 14 ), y0 + ri( 7, 14 ) );
				e.burst( GLOW_WORM, 1 );
				break;
			case SPORES:
				e.pos( x0 + 2, y0 + 2, 12, 10 );
				e.burst( CaveMoteParticle.SPORES, 1 );
				break;
			case EMBERS:
				e.pos( x0 + 2, y0 + 6, 12, 8 );
				e.burst( CaveMoteParticle.EMBERS, 1 );
				break;
			case SILK:
				e.pos( x0, y0 - 4, 16, 12 );
				e.burst( SILK, 1 );
				break;
			case DUST:
				e.pos( x0, y0 - 6, 16, 16 );
				e.burst( CaveMoteParticle.DUST, 1 );
				break;
			default:
				//FIREFLIES: lit over the grass, wandering off on their own
				e.pos( x0 + 2, y0, 12, 12 );
				e.burst( FireflyParticle.FACTORY, 1 );
		}
	}

	//a new cloud of gnats or flies over one of its cells, while there are fewer than CLOUDS
	private void gather( Fauna.Air a, int i ){
		int n = 0;
		for (Cloud c : clouds) if (c.air == a) n++;
		if (n >= Fauna.CLOUDS) return;
		int cell = airCell( i );
		if (cell < 0) return;
		for (Cloud c : clouds) if (c.cell == cell) return;
		clouds.add( new Cloud( a, cell, rf( 10f, 20f ) ) );
		//a buzz as the flies gather on the stain, the remains: a critter's voice, one at a time
		if (a == Fauna.Air.FLIES && RNG.nextFloat() < 0.3f){
			int w = level.width();
			DungeonCritterSprite.voiceAt( AmbientSound.FLY, (cell % w + 0.5f) * SIZE, (cell / w + 0.5f) * SIZE, 0.3f, 1f );
		}
	}

	private void clouds( float dt, Phase phase ){
		int w = level.width();
		int hcell = Dungeon.hero.pos;
		for (int i = clouds.size() - 1; i >= 0; i--){
			Cloud c = clouds.get( i );
			c.left -= dt;
			//gone with its time, its hour, or the hero's going
			if (c.left <= 0f || Fauna.hour( c.air, phase ) <= 0f
					|| Math.max( Math.abs( c.cell % w - hcell % w ), Math.abs( c.cell / w - hcell / w ) ) > REACH + 2){
				clouds.remove( i );
				continue;
			}
			if ((c.next -= dt) > 0f || !level.heroFOV[c.cell]) continue;
			c.next = c.air == Fauna.Air.GNATS ? 0.3f : 0.4f;
			Emitter e = DungeonCritterSprite.emitter();
			if (e == null) continue;
			float x0 = (c.cell % w) * SIZE, y0 = (c.cell / w) * SIZE;
			int t = level.map[c.cell];
			if (c.air == Fauna.Air.GNATS){
				//a little dancing knot over the water
				e.pos( x0 + 4, y0 - 2, 8, 6 );
				e.burst( WeatherBlobFX.FLIES, 2 );
			} else if (t == Terrain.STATUE || t == Terrain.STATUE_SP){
				//about the skulls of a halls' pillar: their column in its own cell, above its foot,
				//and its top six pixels up into the cell above where that stands in the open
				float top = Habitat.perch( level.map, w, c.cell ) ? 6f : 0f;
				e.pos( x0 + 3, y0 - top, 10, 9 + top );
				e.burst( WeatherBlobFX.FLIES, 1 );
			} else {
				e.pos( x0 + 3, y0 + 3, 10, 8 );
				e.burst( WeatherBlobFX.FLIES, 1 );
			}
		}
	}

	// ------------------------------------------------------------ the debug scenes

	//the ring around the hero the life turns up in, as the debug scenes judge a spot by
	private static final int SHOW_NEAR = 2, SHOW_FAR = 6;

	/**
	 * DebugScenes' ambience scenes: of the open cells the hero can walk to from `from`, the one
	 * with the ground of the most of its place's kinds in sight, two to six cells off (the ring
	 * the life turns up in), ties going to the one with the most of it. -1 for a level with no
	 * small life of its own.
	 */
	public static int liveliest( Level level, int from ){
		Place p = level == null ? null : level.ambience();
		if (p == null || !p.underground()) return -1;
		Fauna.Kind[] kinds = Fauna.kinds( p );
		int w = level.width(), h = level.height(), len = level.length();
		//which kinds have ground at each cell, a bit each
		int[] ground = new int[len];
		for (int c = 0; c < len; c++){
			for (int k = 0; k < kinds.length; k++){
				if (Habitat.groundFor( kinds[k], p, level.map, w, h, c )) ground[c] |= 1 << k;
			}
		}
		PathFinder.buildDistanceMap( from, level.passable );
		boolean[] seen = new boolean[len];
		int best = -1;
		long bestScore = -1;
		for (int c = 0; c < len; c++){
			if (PathFinder.distance[c] == Integer.MAX_VALUE || !level.passable[c] || level.pit[c] || level.avoid[c]
					|| level.traps.get( c ) != null || (c != from && Actor.findChar( c ) != null)) continue;
			int cx = c % w, cy = c / w;
			ShadowCaster.castShadow( cx, cy, w, seen, level.losBlocking, SHOW_FAR );
			int have = 0, cells = 0;
			for (int y = Math.max( 0, cy - SHOW_FAR ); y <= Math.min( h - 1, cy + SHOW_FAR ); y++){
				for (int x = Math.max( 0, cx - SHOW_FAR ); x <= Math.min( w - 1, cx + SHOW_FAR ); x++){
					int s = x + y * w;
					if (ground[s] == 0 || !seen[s] || Math.max( Math.abs( x - cx ), Math.abs( y - cy ) ) < SHOW_NEAR) continue;
					have |= ground[s];
					cells++;
				}
			}
			long score = Integer.bitCount( have ) * 100000L + cells;
			if (score > bestScore){
				bestScore = score;
				best = c;
			}
		}
		return best;
	}

	/** DebugScenes: the level's small life and its air, each with how much of it is out at this hour. */
	public static String showcase( Level level ){
		Place p = level == null ? null : level.ambience();
		if (p == null || !p.underground()) return "no small life of its own here";
		Phase ph = DayNightCycle.phase();
		StringBuilder life = new StringBuilder(), air = new StringBuilder();
		for (Fauna.Kind k : Fauna.kinds( p )){
			life.append( life.length() == 0 ? "" : ", " ).append( k.name().toLowerCase( Locale.ENGLISH ) )
					.append( ' ' ).append( Math.round( 100 * Fauna.hour( k, ph ) ) ).append( '%' );
		}
		for (Fauna.Air a : Fauna.air( p )){
			air.append( air.length() == 0 ? "" : ", " ).append( a.name().toLowerCase( Locale.ENGLISH ) )
					.append( ' ' ).append( Math.round( 100 * Fauna.hour( a, ph ) ) ).append( '%' );
		}
		return p.name().toLowerCase( Locale.ENGLISH ) + " at " + ph.name().toLowerCase( Locale.ENGLISH )
				+ ", out at this hour - " + life + (air.length() == 0 ? "" : "; the air: " + air);
	}
}
