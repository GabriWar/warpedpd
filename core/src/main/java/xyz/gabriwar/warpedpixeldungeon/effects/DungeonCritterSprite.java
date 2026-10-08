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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.CaveMoteParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SplashParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientPlayer;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSounds;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSound;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

/**
 * The dungeon's small life (levels/ambience/DungeonLife): a frog on the sewers' bank, a roach
 * along a wall's foot, a mouse in a cell's corner, a moth round a torch, a spider on its thread,
 * a newt by the cold water, a snail on the moss, a strider on the open water, a fly stuck in the
 * nest's webbing. Pictures only, like the surface's critters (CritterSprite): no Actor, nothing
 * saved, nothing to fight - and smaller and paler than anything that fights, so none is taken for
 * a rat, a Spinner or a Swarm.
 *
 * effects/dungeon_critters.png, tools/dungeon_critters_gen.py: sixteen-pixel frames, eight to a
 * row, one creature to a row, everything facing right (mirrored to face left). Each creature
 * keeps its own ways on the render thread; the life that put it out reads the floor's map for
 * it where it walks (Ground), and tells it when something comes too near (scare).
 */
public abstract class DungeonCritterSprite extends CritterSprite {

	//the sheet: fifteen rows of eight frames. Sized by hand so the film never touches the texture
	static final TextureFilm FILM = new TextureFilm( 128, 240, 16, 16 );
	public static final int ROW_FROG = 0, ROW_ROACH = 1, ROW_MOUSE = 2, ROW_MOTH = 3, ROW_SPIDER = 4,
			ROW_NEWT = 5, ROW_SALAMANDER = 6, ROW_LIZARD = 7, ROW_SNAIL = 8, ROW_BEETLE = 9,
			ROW_EMBER_BEETLE = 10, ROW_CENTIPEDE = 11, ROW_SILVERFISH = 12, ROW_STRIDER = 13, ROW_FLY = 14;
	//the frames each row holds, from its first column: every one of them is posed by a creature below
	private static final int[] FRAMES_IN_ROW = { 6, 5, 6, 4, 4, 4, 5, 4, 3, 4, 5, 3, 2, 3, 4 };

	private static final float SIZE = DungeonTilemap.SIZE;

	//one critter heard at a time, and no two of their sounds nearer together than this, seconds:
	//the place's own voices (levels/ambience AmbientSounds) are its bed, these only the odd note
	//over it
	private static final float VOICE_GAP = 0.9f;
	//the stretch of the scene's clock (Game.timeTotal) the last critter's sound rings over: a
	//stretch, not only its end, as every new scene starts its clock again at nought - a sound late
	//in one floor's hour must not hush the next floor's critters for as long
	private static float voiceFrom, voiceFree;

	//it has taken wing and asks to be lifted off the floor into the air (DungeonLife moves it)
	private boolean wantsAir, aloft;

	protected DungeonCritterSprite( float ax, float ay ){
		this( Assets.Effects.DUNGEON_CRITTERS, FILM, ax, ay );
	}

	//one drawn from another sheet: the surface's birds, perched on the dungeon's statues
	protected DungeonCritterSprite( Object texture, TextureFilm film, float ax, float ay ){
		super( texture, film, ax, ay );
	}

	/** Every frame of the sheet the creatures pose, as frame indices (row * 8 + col). */
	public static int[] frames(){
		int n = 0;
		for (int k : FRAMES_IN_ROW) n += k;
		int[] out = new int[n];
		int i = 0;
		for (int row = 0; row < FRAMES_IN_ROW.length; row++){
			for (int col = 0; col < FRAMES_IN_ROW[row]; col++) out[i++] = row * 8 + col;
		}
		return out;
	}

	/** Where a creature that walks may go: the life that put it out reads the floor's map for it. */
	public interface Ground {
		/** Its next little way about its haunt from (x, y): waypoints in scene px (x0,y0,x1,y1...), or null to keep still. */
		float[] wander( float x, float y );
		/** Away from (sx, sy) into the dark: waypoints in scene px, or null to dart straight off. */
		float[] flee( float x, float y, float sx, float sy );
	}

	/** Still about and unafraid: something coming near may yet startle it. */
	public abstract boolean calm();

	/** Something at (sx, sy), scene px, came too near or broke into a fight: it goes its own way. */
	public abstract void scare( float sx, float sy );

	/** The hero is `d` scene px off (Chebyshev), every frame: some look up, crouch, draw in. */
	public void heed( float d ){
	}

	/** A picture of its own drawn just under it, in its layer (a spider's thread), or null. */
	public Visual under(){
		return null;
	}

	/** It has taken wing: it wants lifting off the floor into the air over the walls. */
	public boolean wantsAir(){
		return wantsAir && !aloft;
	}

	/** It was lifted into the air (DungeonLife moved it from the floor into the effects). */
	public void lifted(){
		aloft = true;
	}

	protected void takeWing(){
		wantsAir = true;
	}

	/** The cell of the current floor under a scene point, or -1 off the map. */
	protected static int cellAt( float x, float y ){
		if (Dungeon.level == null || x < 0 || y < 0) return -1;
		int cx = (int)(x / SIZE), cy = (int)(y / SIZE);
		if (cx >= Dungeon.level.width() || cy >= Dungeon.level.height()) return -1;
		return cx + cy * Dungeon.level.width();
	}

	/** Is the water (not ice) at this scene point? */
	protected static boolean onWater( float x, float y ){
		int c = cellAt( x, y );
		return c >= 0 && Dungeon.level.water != null && Dungeon.level.water[c];
	}

	/**
	 * One of its sounds from where it is, on the ambience channel: it falls off with the distance
	 * from the hero and pans to its side, a little higher or lower by `pitch`. Never two critters
	 * at once. Whether it sounded.
	 */
	protected boolean voice( AmbientSound sound, float volume, float pitch ){
		return voiceAt( sound, gx(), gy(), volume, pitch );
	}

	/**
	 * A sound of the small life from a scene point (a critter's, the buzz of a cloud of flies),
	 * on the ambience channel, as voice() plays it. None starts while another still rings: the
	 * voice is held for the whole of the longest take at the lowest pitch the player may nudge it
	 * to. Whether it sounded.
	 */
	public static boolean voiceAt( AmbientSound sound, float x, float y, float volume, float pitch ){
		if (Dungeon.level == null || Dungeon.hero == null || !AmbientPlayer.audible()) return false;
		float now = Game.timeTotal;
		//one critter at a time, and none while the place's own voices hold all three
		if (ringing( now ) || AmbientSounds.full()) return false;
		int w = Dungeon.level.width();
		float dx = x / SIZE - (Dungeon.hero.pos % w + 0.5f);
		float dy = y / SIZE - (Dungeon.hero.pos / w + 0.5f);
		float level = AmbientPlayer.falloff( (float)Math.sqrt( dx * dx + dy * dy ) );
		if (level <= 0f) return false;
		voiceFrom = now;
		voiceFree = now + Math.max( VOICE_GAP, sound.length / (pitch * (1f - AmbientPlayer.PITCH_SPREAD)) );
		AmbientPlayer.play( sound, volume * level, pitch, AmbientPlayer.pan( dx ) );
		return true;
	}

	/**
	 * Is a critter's sound ringing now? It is one of the ambience's voices: the place's own count
	 * it among theirs, never more than three sounding at once.
	 */
	public static boolean voiceBusy(){
		return ringing( Game.timeTotal );
	}

	private static boolean ringing( float now ){
		return now >= voiceFrom && now < voiceFree;
	}

	/**
	 * A scene emitter for a burst of the dungeon's small life and its air: never relayed (a
	 * recycled emitter keeps the network tag it last had), or null with no scene.
	 */
	public static Emitter emitter(){
		Emitter e = GameScene.emitter();
		if (e != null) e.netCell = -1;
		return e;
	}

	//a heading (radians) away from (sx, sy), a little off true
	protected float awayFrom( float sx, float sy, float spread ){
		float dx = gx() - sx, dy = gy() - sy;
		float a = Math.abs( dx ) + Math.abs( dy ) < 0.5f ? rf( 0f, 6.2832f ) : (float)Math.atan2( dy, dx );
		return a + rf( -spread, spread );
	}

	/**
	 * A frog on the bank of the sewers' water or out on the moss: it sits, puffs its throat in a
	 * croak now and then (heard, not more than once in six to ten seconds), crouches when the
	 * hero comes near, and when something comes too near it springs into the water beside it -
	 * a splash, a ring, its head a moment above the surface, gone - or, with no water to hand,
	 * hops off twice into the dark.
	 */
	public static class Frog extends DungeonCritterSprite {

		private static final int SIT = 0, CROAK = 1, CROUCH = 2, HOP = 3, LAND = 4, SWIM = 5;
		private static final int SITTING = 0, CROAKING = 1, HOPPING = 2, SWIMMING = 3;
		//the splash it makes, pale on the murky water
		private static final int SPLASH_COLOUR = 0xC8DCD0;

		//the water it dives into, scene px; none on the moss far from the water
		private final boolean water;
		private final float wx, wy;
		//its while out on the bank, then it is off of its own accord
		private final float span;
		private int state = SITTING;
		private float t, croakIn, turnIn;
		private boolean crouched;
		private int puffs;
		//the hop in the air: from where it was to (toX, toY), offsets from its ground point
		private float fromX, fromY, toX, toY, hopTime, apex;
		private int hopsLeft;
		private boolean diving;
		private float awayX, awayY;

		public Frog( float ax, float ay, float wx, float wy, boolean water ){
			super( ax, ay );
			this.water = water;
			this.wx = wx;
			this.wy = wy;
			origin.set( 8, 14 );
			pose( ROW_FROG, SIT );
			//on the bank it faces the water it will leap into
			face( water ? wx < ax : RNG.nextBoolean() );
			croakIn = rf( 1.5f, 6f );
			turnIn = rf( 4f, 9f );
			span = rf( 40f, 80f );
			shown = 0f;
		}

		@Override
		public boolean calm(){
			return (state == SITTING || state == CROAKING) && !leaving();
		}

		@Override
		public void heed( float d ){
			crouched = d <= 64f;
		}

		@Override
		public void scare( float sx, float sy ){
			if (!calm()) return;
			//not yet made out: it was never there
			if (shown <= 0f){
				vanish();
				return;
			}
			state = HOPPING;
			//into the water beside it while it is water: a pool iced over since it sat down
			//sends it off over the bank like one out on the moss
			if (water && onWater( wx, wy - 1f )){
				diving = true;
				hopsLeft = 1;
				face( wx < gx() );
				hopTo( wx - ax, wy - ay, 0.34f, 6f );
			} else {
				diving = false;
				float a = awayFrom( sx, sy, 0.4f );
				awayX = (float)Math.cos( a );
				awayY = (float)Math.sin( a );
				face( awayX < 0f );
				hopsLeft = 2;
				hopTo( ox + awayX * 11f, oy + awayY * 8f, 0.28f, 4f );
			}
		}

		private void hopTo( float x, float y, float time, float height ){
			fromX = ox;
			fromY = oy;
			toX = x;
			toY = y;
			hopTime = time;
			apex = height;
			t = 0f;
		}

		@Override
		protected void act( float dt ){
			t += dt;
			switch (state){
				case SITTING:  sit( dt ); break;
				case CROAKING: croak(); break;
				case HOPPING:  hop(); break;
				default:       swim( dt );
			}
		}

		private void sit( float dt ){
			shown = Math.min( 1f, shown + dt / 0.6f );
			//its while is up: into the water, or off over the moss
			if (life >= span && shown >= 1f){
				float a = rf( 0f, 6.2832f );
				scare( gx() - (float)Math.cos( a ) * SIZE, gy() - (float)Math.sin( a ) * SIZE );
				return;
			}
			pose( ROW_FROG, crouched ? CROUCH : SIT );
			//one out on the moss turns about now and then; one on the bank keeps its eye on the water
			if (!water && (turnIn -= dt) <= 0f){
				turnIn = rf( 4f, 9f );
				face( !flipHorizontal );
			}
			if ((croakIn -= dt) <= 0f && shown >= 1f && !crouched){
				state = CROAKING;
				t = 0f;
				puffs = 2 + RNG.nextInt( 2 );
				croaked();
			}
		}

		//the throat puffed and let go, two or three times
		private void croak(){
			final float cycle = 0.32f;
			int k = (int)(t / cycle);
			if (k >= puffs){
				state = SITTING;
				croakIn = rf( 6f, 10f );
				pose( ROW_FROG, SIT );
				return;
			}
			pose( ROW_FROG, t - k * cycle < 0.2f ? CROAK : SIT );
		}

		/** Its croak, heard from where it sits. */
		protected void croaked(){
			if (visible) voice( AmbientSound.FROG, 0.7f, rf( 0.95f, 1.1f ) );
		}

		private void hop(){
			//a crouch, then the spring
			if (t < 0.07f){
				pose( ROW_FROG, CROUCH );
				return;
			}
			float p = Math.min( 1f, (t - 0.07f) / hopTime );
			ox = fromX + (toX - fromX) * p;
			oy = fromY + (toY - fromY) * p;
			z = (float)Math.sin( p * Math.PI ) * apex;
			pose( ROW_FROG, p < 0.85f ? HOP : LAND );
			//the last hop away on land takes it out of the light
			if (!diving && hopsLeft == 1) shown = 1f - p;
			if (p < 1f) return;
			z = 0f;
			if (diving){
				state = SWIMMING;
				t = 0f;
				pose( ROW_FROG, SWIM );
				plunge( gx(), gy() );
			} else if (--hopsLeft > 0){
				hopTo( ox + awayX * 11f, oy + awayY * 8f, 0.28f, 4f );
			} else {
				vanish();
			}
		}

		//its head above the water a moment, drifting on, then under
		private void swim( float dt ){
			pose( ROW_FROG, SWIM );
			ox += (flipHorizontal ? -3f : 3f) * dt;
			shown = Math.max( 0f, 1f - t / 1.1f );
			if (t >= 1.1f) vanish();
		}

		/** Into the water at (x, y): a splash, a ring, the sound of it. */
		protected void plunge( float x, float y ){
			int cell = cellAt( x, y - 1f );
			if (cell >= 0) GameScene.ripple( cell );
			Splash.at( new PointF( x, y - 2f ), SPLASH_COLOUR, 5 );
			voice( AmbientSound.SPLASH, 0.5f, rf( 1.15f, 1.35f ) );
		}
	}

	/**
	 * A creature that walks: it keeps still a while (doing whatever it does there), then goes a
	 * little way about its haunt - the life that put it out finds the way (Ground) - and keeps
	 * still again. Scared, it runs for it along the way it is given, fast, and is gone into the
	 * dark over the last of it; and after a while out, it goes its way into the dark unbidden.
	 */
	public static abstract class Walker extends DungeonCritterSprite {

		private static final int RESTING = 0, WALKING = 1, FLEEING = 2;

		private final Ground ground;
		private final float span;
		private int state = RESTING;
		private float rest;
		private float[] path;
		private int leg;
		private float pace, ran, length, moveT;

		protected Walker( float ax, float ay, Ground ground, float firstRest ){
			super( ax, ay );
			this.ground = ground;
			rest = firstRest;
			span = rf( 40f, 90f );
			origin.set( 8, 14 );
			face( RNG.nextBoolean() );
			shown = 0f;
		}

		//its paces, px a second: pottering about, and running for it
		protected abstract float walkPace();
		protected abstract float fleePace();
		//how long it keeps still between its little ways
		protected abstract float restTime();
		//still, with whatever it does there; on the move, `time` seconds into it
		protected abstract void still( float dt );
		protected abstract void moving( float time );

		//set off on a little way (a skitter, a squeak), and off in flight
		protected void setOff(){
		}

		protected void fled(){
		}

		//a flight that ends in its own way rather than fading into the dark: a dive
		protected boolean fadesAway(){
			return true;
		}

		protected void arrived(){
		}

		@Override
		public boolean calm(){
			return state != FLEEING && !leaving();
		}

		/** Off along these points (scene px, x0,y0,x1,y1...) at a potter; still again at the end. */
		public void walk( float[] pts ){
			if (state != RESTING || pts == null || pts.length < 2) return;
			start( pts, walkPace() );
			state = WALKING;
			setOff();
		}

		/** Away along these points, fast, gone into the dark over the last of them. */
		public void flee( float[] pts ){
			if (state == FLEEING || leaving()) return;
			//not yet made out: it was never there
			if (shown <= 0f){
				vanish();
				return;
			}
			start( pts != null && pts.length >= 2 ? pts : new float[]{ ax, ay }, fleePace() );
			state = FLEEING;
			fled();
		}

		@Override
		public void scare( float sx, float sy ){
			if (!calm()) return;
			float[] pts = ground != null ? ground.flee( ax, ay, sx, sy ) : null;
			if (pts == null || pts.length < 2){
				//hemmed in where the ground is known: into a crack right there; with no ground
				//known, two cells straight off
				pts = straightAway( sx, sy, ground != null ? 0.4f : 2f );
			}
			flee( pts );
		}

		//`cells` straight away from the fright
		private float[] straightAway( float sx, float sy, float cells ){
			float a = awayFrom( sx, sy, 0.2f );
			return new float[]{ ax + (float)Math.cos( a ) * cells * SIZE, ay + (float)Math.sin( a ) * cells * SIZE * 0.75f };
		}

		private void start( float[] pts, float pace ){
			path = pts;
			leg = 0;
			this.pace = pace;
			ran = 0f;
			moveT = 0f;
			length = 0f;
			float px = ax, py = ay;
			for (int i = 0; i + 1 < pts.length; i += 2){
				length += (float)Math.hypot( pts[i] - px, pts[i+1] - py );
				px = pts[i];
				py = pts[i+1];
			}
		}

		@Override
		protected void act( float dt ){
			if (state == RESTING){
				shown = Math.min( 1f, shown + dt / 0.5f );
				//its while out is up: off about its business, the way it would run from a fright
				//a cell behind it
				if (life >= span && shown >= 1f){
					float a = rf( 0f, 6.2832f );
					scare( ax - (float)Math.cos( a ) * SIZE, ay - (float)Math.sin( a ) * SIZE );
					if (!calm()) return;
				}
				still( dt );
				if ((rest -= dt) <= 0f && shown >= 1f){
					float[] pts = ground != null ? ground.wander( ax, ay ) : null;
					if (pts != null && pts.length >= 2) walk( pts );
					else rest = restTime();
				}
				return;
			}
			float step = pace * dt;
			while (step > 0f && leg * 2 + 1 < path.length){
				float dx = path[leg*2] - ax, dy = path[leg*2+1] - ay;
				float d = (float)Math.hypot( dx, dy );
				if (Math.abs( dx ) > 0.5f) face( dx < 0f );
				if (d <= step){
					ax = path[leg*2];
					ay = path[leg*2+1];
					step -= d;
					ran += d;
					leg++;
				} else {
					ax += dx / d * step;
					ay += dy / d * step;
					ran += step;
					step = 0f;
				}
			}
			moveT += dt;
			moving( moveT );
			boolean there = leg * 2 + 1 >= path.length;
			if (state == FLEEING){
				float p = length > 0f ? Math.min( 1f, ran / length ) : 1f;
				if (fadesAway()) shown = Math.min( shown, p > 0.4f ? (1f - p) / 0.6f : 1f );
				if (there){
					arrived();
					vanish();
				}
			} else if (there){
				state = RESTING;
				rest = restTime();
			}
		}
	}

	/**
	 * What scuttles along the foot of a wall: a cockroach, a centipede, a silverfish, a nest's
	 * spiderling on its webbing. Still a moment (a roach's feelers up, a silverfish's flick),
	 * then a burst along its haunt, and a dash for the dark when anything comes.
	 */
	public static class Scuttler extends Walker {

		public enum Look {
			//          row             still twitch  cycle                    fps   walk  flee  rest
			ROACH     ( ROW_ROACH,      0,    4,      new int[]{ 1, 2, 3, 2 }, 20f,  46f,  78f,  1.5f, 5f ),
			CENTIPEDE ( ROW_CENTIPEDE,  0,    1,      new int[]{ 0, 1, 2, 1 }, 10f,  16f,  36f,  2f,   6f ),
			SILVERFISH( ROW_SILVERFISH, 0,    1,      new int[]{ 0, 1 },       22f,  56f,  84f,  1f,   4f ),
			SPIDERLING( ROW_SPIDER,     2,    3,      new int[]{ 2, 3 },       16f,  30f,  56f,  1.5f, 4f );

			final int row, still, twitch;
			final int[] cycle;
			final float fps, walk, flee, restLo, restHi;

			Look( int row, int still, int twitch, int[] cycle, float fps, float walk, float flee, float restLo, float restHi ){
				this.row = row;
				this.still = still;
				this.twitch = twitch;
				this.cycle = cycle;
				this.fps = fps;
				this.walk = walk;
				this.flee = flee;
				this.restLo = restLo;
				this.restHi = restHi;
			}
		}

		public final Look look;
		private float twitchIn, twitch;

		public Scuttler( Look look, float ax, float ay, Ground ground ){
			super( ax, ay, ground, rf( look.restLo, look.restHi ) );
			this.look = look;
			pose( look.row, look.still );
			twitchIn = rf( 0.8f, 2.5f );
		}

		@Override
		protected float walkPace(){
			return look.walk;
		}

		@Override
		protected float fleePace(){
			return look.flee;
		}

		@Override
		protected float restTime(){
			return rf( look.restLo, look.restHi );
		}

		@Override
		protected void still( float dt ){
			if (twitch > 0f){
				twitch -= dt;
				pose( look.row, look.twitch );
				return;
			}
			pose( look.row, look.still );
			if ((twitchIn -= dt) <= 0f){
				twitch = rf( 0.15f, 0.35f );
				twitchIn = rf( 0.8f, 2.5f );
			}
		}

		@Override
		protected void moving( float time ){
			pose( look.row, look.cycle[(int)(time * look.fps) % look.cycle.length] );
		}

		@Override
		protected void setOff(){
			if (look != Look.CENTIPEDE && visible && RNG.nextFloat() < 0.15f) voice( AmbientSound.SKITTER, 0.35f, rf( 1.2f, 1.5f ) );
		}

		@Override
		protected void fled(){
			if (visible && RNG.nextFloat() < 0.4f) voice( AmbientSound.SKITTER, 0.45f, rf( 1.05f, 1.3f ) );
		}
	}

	/**
	 * A mouse in the corner of a cell: it sits, sniffs the air, grooms, runs along the walls to
	 * another corner; it sits up with its nose working when the hero is about, and squeaking, it
	 * is off along the wall and gone.
	 */
	public static class Mouse extends Walker {

		private static final int SIT = 0, SNIFF = 1, GROOM = 2;
		private static final int[] RUN = { 3, 4, 5, 4 };

		private int idle = SIT;
		private float idleFor;
		private boolean wary;

		public Mouse( float ax, float ay, Ground ground ){
			super( ax, ay, ground, rf( 2f, 5f ) );
			pose( ROW_MOUSE, SIT );
			idleFor = rf( 1f, 2.5f );
		}

		@Override
		protected float walkPace(){
			return 50f;
		}

		@Override
		protected float fleePace(){
			return 88f;
		}

		@Override
		protected float restTime(){
			return rf( 2f, 6f );
		}

		@Override
		public void heed( float d ){
			wary = d <= 72f;
		}

		@Override
		protected void still( float dt ){
			//the hero about: up on its haunches, nose working
			if (wary){
				pose( ROW_MOUSE, SNIFF );
				return;
			}
			if ((idleFor -= dt) <= 0f){
				float r = RNG.nextFloat();
				idle = r < 0.45f ? SIT : r < 0.75f ? SNIFF : GROOM;
				idleFor = idle == GROOM ? rf( 1f, 2.5f ) : idle == SNIFF ? rf( 0.4f, 1f ) : rf( 1f, 3f );
			}
			pose( ROW_MOUSE, idle );
		}

		@Override
		protected void moving( float time ){
			pose( ROW_MOUSE, RUN[(int)(time * 18f) & 3] );
		}

		@Override
		protected void setOff(){
			if (visible && RNG.nextFloat() < 0.12f) voice( AmbientSound.RAT, 0.3f, rf( 1.35f, 1.55f ) );
		}

		@Override
		protected void fled(){
			if (visible && RNG.nextFloat() < 0.6f) voice( AmbientSound.RAT, 0.4f, rf( 1.3f, 1.5f ) );
		}
	}

	/**
	 * What basks: a newt at the caves' cold water, a lizard under the city's warm smoke vents, a
	 * fire salamander by the halls' cold lava. Long still, its head up now and then (and up when
	 * the hero is near), a quick dart a little way; scared, it slips into the water or the lava
	 * beside it - a ring, or a burst of sparks - or runs for a crack. A salamander's spots glow
	 * and fade.
	 */
	public static class Basker extends Walker {

		public enum Look {
			//          row             walk  flee   fps
			NEWT      ( ROW_NEWT,       26f,  60f,  14f ),
			LIZARD    ( ROW_LIZARD,     60f,  96f,  22f ),
			SALAMANDER( ROW_SALAMANDER, 34f,  70f,  16f );

			final int row;
			final float walk, flee, fps;

			Look( int row, float walk, float flee, float fps ){
				this.row = row;
				this.walk = walk;
				this.flee = flee;
				this.fps = fps;
			}
		}

		private static final int REST = 0, HEAD_UP = 3, GLOW = 4;
		private static final int[] WALK = { 1, 2 };

		public final Look look;
		//where it slips in when scared, scene px: the water (or the lava) beside its bank
		private final boolean dives;
		private final float wx, wy;
		private boolean alert, diving;
		private float lookIn, looking, glowIn, glowing;

		public Basker( Look look, float ax, float ay, Ground ground, float wx, float wy, boolean dives ){
			super( ax, ay, ground, rf( 4f, 12f ) );
			this.look = look;
			this.wx = wx;
			this.wy = wy;
			this.dives = dives;
			pose( look.row, REST );
			lookIn = rf( 2f, 6f );
			glowIn = rf( 1f, 4f );
		}

		@Override
		protected float walkPace(){
			return look.walk;
		}

		@Override
		protected float fleePace(){
			return look.flee;
		}

		@Override
		protected float restTime(){
			return rf( 4f, 12f );
		}

		@Override
		public void heed( float d ){
			alert = d <= 72f;
		}

		@Override
		public void scare( float sx, float sy ){
			if (!calm()) return;
			//into the water (the lava) while it is still that: a pool iced over since sends it
			//off for a crack
			if (dives && onWater( wx, wy - 1f )){
				diving = true;
				flee( new float[]{ wx, wy } );
			} else {
				super.scare( sx, sy );
			}
		}

		@Override
		protected boolean fadesAway(){
			return !diving;
		}

		@Override
		protected void arrived(){
			if (diving) slipIn( ax, ay );
		}

		/** Under the surface at (x, y): a ring on the water, or the lava taking it in a burst of sparks. */
		protected void slipIn( float x, float y ){
			if (look == Look.SALAMANDER){
				Splash.at( new PointF( x, y - 2f ), 0xFF7A30, 3 );
				Emitter e = emitter();
				if (e != null){
					e.pos( x - 3f, y - 5f, 6f, 4f );
					e.burst( CaveMoteParticle.EMBERS, 6 );
				}
				voice( AmbientSound.LAVA, 0.45f, rf( 1.2f, 1.45f ) );
			} else {
				int cell = cellAt( x, y - 1f );
				if (cell >= 0) GameScene.ripple( cell );
				Splash.at( new PointF( x, y - 2f ), 0xBFD4D8, 3 );
				voice( AmbientSound.SPLASH, 0.35f, rf( 1.35f, 1.55f ) );
			}
		}

		@Override
		protected void still( float dt ){
			//a salamander's spots glow up and die down
			if (look == Look.SALAMANDER){
				if (glowing > 0f){
					glowing -= dt;
					pose( look.row, GLOW );
					return;
				}
				if ((glowIn -= dt) <= 0f){
					glowing = rf( 0.4f, 0.8f );
					glowIn = rf( 2f, 5f );
				}
			}
			if (alert){
				pose( look.row, HEAD_UP );
				return;
			}
			if (looking > 0f){
				looking -= dt;
				pose( look.row, HEAD_UP );
				return;
			}
			pose( look.row, REST );
			if ((lookIn -= dt) <= 0f){
				looking = rf( 0.8f, 2f );
				lookIn = rf( 3f, 8f );
			}
		}

		@Override
		protected void moving( float time ){
			pose( look.row, WALK[(int)(time * look.fps) & 1] );
		}
	}

	/**
	 * A beetle by the caves' scaffolds or in the mines, an ember beetle on the halls' embermoss:
	 * it trundles about a little, rests; scared, it opens its wing cases and is up and away,
	 * gone in a second. An ember beetle's back glows and dims.
	 */
	public static class Beetle extends Walker {

		private static final int[] WALK = { 0, 1 };
		private static final int WINGS = 2, REST = 3, GLOW = 4;

		public final boolean ember;
		private final int row;
		private boolean flying;
		private float heading, speed, flyT, glowIn, glowing;

		public Beetle( boolean ember, float ax, float ay, Ground ground ){
			super( ax, ay, ground, rf( 2f, 6f ) );
			this.ember = ember;
			row = ember ? ROW_EMBER_BEETLE : ROW_BEETLE;
			pose( row, REST );
			glowIn = rf( 1f, 3f );
		}

		@Override
		protected float walkPace(){
			return ember ? 9f : 11f;
		}

		@Override
		protected float fleePace(){
			return 30f;
		}

		@Override
		protected float restTime(){
			return rf( 2f, 7f );
		}

		@Override
		public boolean calm(){
			return !flying && super.calm();
		}

		@Override
		public void scare( float sx, float sy ){
			if (!calm()) return;
			if (shown <= 0f){
				vanish();
				return;
			}
			flying = true;
			heading = awayFrom( sx, sy, 0.5f );
			face( Math.cos( heading ) < 0 );
			speed = 20f;
			flyT = 0f;
			takeWing();
		}

		@Override
		protected void act( float dt ){
			if (flying) fly( dt );
			else super.act( dt );
		}

		private void fly( float dt ){
			flyT += dt;
			//the wing cases open on the ground, then it is up
			if (flyT < 0.18f){
				pose( row, WINGS );
				return;
			}
			speed = Math.min( 56f, speed + 120f * dt );
			ox += (float)Math.cos( heading ) * speed * dt;
			oy += (float)Math.sin( heading ) * speed * dt;
			z += 14f * dt;
			//the hind wings a whirr under the open cases
			pose( row, ((int)(flyT * 30f) & 1) == 0 ? WINGS : REST );
			shown = flyT < 0.6f ? 1f : Math.max( 0f, 1f - (flyT - 0.6f) / 0.6f );
			if (flyT >= 1.2f) vanish();
		}

		@Override
		protected void still( float dt ){
			if (ember){
				if (glowing > 0f){
					glowing -= dt;
					pose( row, GLOW );
					if (glowing <= 0f && visible && RNG.nextFloat() < 0.3f) spark();
					return;
				}
				if ((glowIn -= dt) <= 0f){
					glowing = rf( 0.3f, 0.6f );
					glowIn = rf( 1.5f, 4f );
				}
			}
			pose( row, REST );
		}

		//an ember let go off the glowing back
		private void spark(){
			Emitter e = emitter();
			if (e == null) return;
			e.pos( gx() - 1f, gy() - 4f, 2f, 2f );
			e.burst( CaveMoteParticle.EMBERS, 1 );
		}

		@Override
		protected void moving( float time ){
			pose( row, WALK[(int)(time * 8f) & 1] );
		}
	}

	/**
	 * A snail on the sewers' moss: it creeps, stretching and drawing in, about its patch; when
	 * the hero comes up beside it (or anything passes, or a fight breaks out) it draws into its
	 * shell, and comes out again once all has been quiet a while. It never runs: after a long
	 * while it is simply lost in the moss and the dark.
	 */
	public static class Snail extends DungeonCritterSprite {

		private static final int STRETCHED = 0, DRAWN = 1, SHELL = 2;
		private static final int OUT = 0, HIDING = 1, HIDDEN = 2, EMERGING = 3;
		//how far it strays from where it was found, px: it keeps to its patch of moss
		private static final float ROAM_X = 5f, ROAM_Y = 2f;
		//quiet this long, it comes out again
		private static final float QUIET = 2.5f;

		private final float span;
		private int state = OUT;
		private float t, quiet, dir, drift;

		public Snail( float ax, float ay ){
			super( ax, ay );
			origin.set( 8, 14 );
			dir = RNG.nextBoolean() ? 1f : -1f;
			face( dir < 0f );
			drift = rf( -0.3f, 0.3f );
			span = rf( 60f, 120f );
			pose( ROW_SNAIL, STRETCHED );
			shown = 0f;
		}

		@Override
		public boolean calm(){
			return !leaving();
		}

		/** In its shell (or drawing into it). */
		public boolean hidden(){
			return state == HIDING || state == HIDDEN;
		}

		@Override
		public void scare( float sx, float sy ){
			if (state == OUT || state == EMERGING){
				state = HIDING;
				t = 0f;
			}
			quiet = 0f;
		}

		@Override
		public void heed( float d ){
			if (d <= 24f) scare( 0f, 0f );
			else if (d <= 40f) quiet = 0f;
		}

		@Override
		protected void act( float dt ){
			t += dt;
			shown = Math.min( 1f, shown + dt / 0.8f );
			if (life >= span) leave( 2f );
			switch (state){
				case OUT:
					//stretch and glide a little, draw up the tail, again
					boolean stretched = t % 1.4f < 0.8f;
					pose( ROW_SNAIL, stretched ? STRETCHED : DRAWN );
					if (stretched){
						ox += dir * 3.2f * dt;
						oy += drift * dt;
					}
					if (Math.abs( ox ) > ROAM_X){
						ox = Math.signum( ox ) * ROAM_X;
						dir = -dir;
						face( dir < 0f );
					}
					if (Math.abs( oy ) > ROAM_Y){
						oy = Math.signum( oy ) * ROAM_Y;
						drift = -drift;
					}
					break;
				case HIDING:
					pose( ROW_SNAIL, t < 0.12f ? DRAWN : SHELL );
					if (t >= 0.12f) state = HIDDEN;
					break;
				case HIDDEN:
					pose( ROW_SNAIL, SHELL );
					if ((quiet += dt) >= QUIET){
						state = EMERGING;
						t = 0f;
					}
					break;
				default:
					pose( ROW_SNAIL, DRAWN );
					if (t >= 0.5f){
						state = OUT;
						t = 0f;
					}
			}
		}
	}

	/**
	 * A moth: round a light - a prison torch (cream), a city flame, the halls' cold lava (an
	 * orange cinder moth), a frozen seam's glimmer (frost blue) - on an erratic way in and out,
	 * now and then down on the stone by it for a rest (if there is stone by it: the lava has
	 * none); or, where nothing burns, flat on a wall, fluttering a little round its spot now and
	 * then. Startled, it flutters off into the dark.
	 */
	public static class Moth extends DungeonCritterSprite {

		private static final int[] FLUTTER = { 0, 1, 2, 1 };
		private static final int REST = 3;
		private static final int ORBIT = 0, PERCHED = 1, FLITTING = 2, OFF = 3;

		//circling its light (ax, ay), or with none its spot on the wall
		private final boolean light;
		//a light on stone it can come down on beside it (a torch, a pedestal)
		private final boolean lands;
		//the cell whose sight decides whether it shows: this far below its light, or its spot
		private final float seenDY;
		private int state;
		private float t, theta, omega, r, rTarget, jitterIn, fps, landIn, perchFor, flitFor, span;
		private float restX, restY, heading, speed;

		public Moth( float ax, float ay, float seenDY, int tint, boolean light, boolean lands ){
			super( ax, ay );
			this.light = light;
			this.lands = lands;
			this.seenDY = seenDY;
			origin.set( 7, 8 );
			hardlight( tint );
			fps = rf( 16f, 22f );
			span = rf( 25f, 60f );
			shown = 0f;
			if (light){
				state = ORBIT;
				theta = rf( 0f, 6.2832f );
				omega = (RNG.nextBoolean() ? 1f : -1f) * rf( 2.5f, 4.5f );
				r = rTarget = rf( 6f, 12f );
				landIn = rf( 6f, 15f );
				pose( ROW_MOTH, FLUTTER[0] );
			} else {
				state = PERCHED;
				perchFor = rf( 3f, 9f );
				pose( ROW_MOTH, REST );
			}
		}

		@Override
		public boolean calm(){
			return state != OFF && !leaving();
		}

		@Override
		public void scare( float sx, float sy ){
			if (!calm()) return;
			if (shown <= 0f){
				vanish();
				return;
			}
			off( awayFrom( sx, sy, 0.6f ) );
		}

		private void off( float heading ){
			state = OFF;
			t = 0f;
			this.heading = heading;
			speed = rf( 32f, 46f );
			jitterIn = 0f;
		}

		@Override
		protected float seenX(){
			return state == OFF ? super.seenX() : ax;
		}

		@Override
		protected float seenY(){
			return state == OFF ? super.seenY() : ay + seenDY;
		}

		@Override
		protected void act( float dt ){
			t += dt;
			switch (state){
				case ORBIT:    orbit( dt ); break;
				case PERCHED:  perched( dt ); break;
				case FLITTING: flit( dt ); break;
				default:       away( dt );
			}
			//its night is long, but not that long
			if (state != OFF && life >= span) off( rf( 0f, 6.2832f ) );
		}

		private void flutter(){
			pose( ROW_MOTH, FLUTTER[(int)(t * fps) & 3] );
		}

		//the circle round (cx, cy), offsets from the ground point: in and out, faster and
		//slower, every fraction of a second a new mind
		private void circle( float cx, float cy, float dt, float rMin, float rMax ){
			if ((jitterIn -= dt) <= 0f){
				jitterIn = rf( 0.1f, 0.28f );
				rTarget = rf( rMin, rMax );
				omega += rf( -2f, 2f );
				if (RNG.nextFloat() < 0.08f) omega = -omega;
				float mag = Math.max( 2.2f, Math.min( 6f, Math.abs( omega ) ) );
				omega = omega < 0f ? -mag : mag;
			}
			r += (rTarget - r) * Math.min( 1f, dt * 6f );
			theta += omega * dt;
			float nx = cx + r * (float)Math.cos( theta ), ny = cy + r * (float)Math.sin( theta ) * 0.7f;
			if (Math.abs( nx - ox ) > 0.2f) face( nx < ox );
			ox = nx;
			oy = ny;
			flutter();
		}

		//into a circle from wherever it is, without a jump
		private void join( float cx, float cy ){
			float dx = ox - cx, dy = (oy - cy) / 0.7f;
			r = rTarget = Math.max( 2f, (float)Math.hypot( dx, dy ) );
			theta = (float)Math.atan2( dy, dx );
		}

		private void orbit( float dt ){
			shown = Math.min( 1f, shown + dt / 0.5f );
			circle( 0f, -2f, dt, 3f, 14f );
			//now and then down on the stone by the light, for a rest
			if (lands && (landIn -= dt) <= 0f){
				settle( rf( -9f, 9f ), rf( -7f, 3f ) );
			}
		}

		private void settle( float x, float y ){
			state = PERCHED;
			restX = x;
			restY = y;
			perchFor = rf( 2f, 6f );
		}

		private void perched( float dt ){
			shown = Math.min( 1f, shown + dt / 0.5f );
			float dx = restX - ox, dy = restY - oy, d = (float)Math.hypot( dx, dy );
			if (d > 0.5f){
				//coming in to land
				float v = Math.min( d, 26f * dt );
				ox += dx / d * v;
				oy += dy / d * v;
				if (Math.abs( dx ) > 0.3f) face( dx < 0f );
				flutter();
				return;
			}
			ox = restX;
			oy = restY;
			pose( ROW_MOTH, REST );
			if ((perchFor -= dt) <= 0f){
				if (light){
					state = ORBIT;
					landIn = rf( 6f, 15f );
					join( 0f, -2f );
				} else {
					state = FLITTING;
					flitFor = rf( 1.5f, 4f );
					join( restX, restY - 4f );
				}
			}
		}

		//a little loop round its spot on the wall, then down again a little along
		private void flit( float dt ){
			circle( restX, restY - 4f, dt, 5f, 14f );
			if ((flitFor -= dt) <= 0f){
				settle( Math.max( -10f, Math.min( 10f, restX + rf( -8f, 8f ) ) ),
						Math.max( -4f, Math.min( 4f, restY + rf( -3f, 3f ) ) ) );
			}
		}

		private void away( float dt ){
			if ((jitterIn -= dt) <= 0f){
				jitterIn = rf( 0.08f, 0.2f );
				heading += rf( -0.8f, 0.8f );
			}
			ox += (float)Math.cos( heading ) * speed * dt;
			oy += (float)Math.sin( heading ) * speed * dt;
			z += 8f * dt;
			if (Math.abs( Math.cos( heading ) ) > 0.2f) face( Math.cos( heading ) < 0 );
			pose( ROW_MOTH, FLUTTER[(int)(t * 24f) & 3] );
			shown = Math.min( shown, t < 0.6f ? 1f : Math.max( 0f, 1f - (t - 0.6f) / 0.9f ) );
			if (t >= 1.5f) vanish();
		}
	}

	/**
	 * A small spider let down on its thread from the top of a wall's face, over the floor at its
	 * foot: it lowers itself, hangs there turning its legs and bobbing on the silk, and when
	 * anything comes near it climbs back up its thread and is gone into the cracks at the top.
	 * The thread is a picture of its own, one pixel wide, drawn just under it.
	 */
	public static class Spider extends DungeonCritterSprite {

		private static final int TUCKED = 0, SPREAD = 1;
		private static final int LOWERING = 0, HANGING = 1, CLIMBING = 2;

		private final ColorBlock silk;
		//its sight is the floor under the face it hangs on, this far below its anchor
		private final float seenDY, drop;
		private int state = LOWERING;
		private float bob, flexIn, flex, span;

		public Spider( float ax, float ay, float seenDY, float drop ){
			super( ax, ay );
			this.seenDY = seenDY;
			this.drop = drop;
			origin.set( 8, 2 );
			pose( ROW_SPIDER, TUCKED );
			silk = new ColorBlock( 1, 1, 0xFFE2DEEA );
			silk.alpha( 0f );
			silk.visible = false;
			bob = rf( 0f, 6.2832f );
			flexIn = rf( 1f, 3f );
			span = rf( 30f, 70f );
			shown = 0f;
		}

		@Override
		public Visual under(){
			return silk;
		}

		@Override
		public boolean calm(){
			return state != CLIMBING && !leaving();
		}

		@Override
		public void scare( float sx, float sy ){
			if (!calm()) return;
			state = CLIMBING;
		}

		@Override
		protected float seenY(){
			return ay + seenDY;
		}

		@Override
		protected void act( float dt ){
			switch (state){
				case LOWERING:
					shown = Math.min( 1f, shown + dt / 0.6f );
					pose( ROW_SPIDER, TUCKED );
					oy = Math.min( drop, oy + 9f * dt );
					if (oy >= drop) state = HANGING;
					break;
				case HANGING:
					bob += dt * 1.7f;
					oy = drop + (float)Math.sin( bob ) * 1.2f;
					//its legs drawn in a moment now and then, then spread again
					if (flex > 0f){
						flex -= dt;
						pose( ROW_SPIDER, TUCKED );
					} else {
						pose( ROW_SPIDER, SPREAD );
						if ((flexIn -= dt) <= 0f){
							flex = rf( 0.2f, 0.5f );
							flexIn = rf( 1.5f, 4f );
						}
					}
					if (life >= span) state = CLIMBING;
					break;
				default:
					pose( ROW_SPIDER, TUCKED );
					oy = Math.max( 0f, oy - 30f * dt );
					//up in the cracks at the top of the face
					if (oy <= 2f) shown = Math.max( 0f, shown - dt / 0.25f );
					if (oy <= 0f && shown <= 0f) vanish();
			}
		}

		@Override
		public void update(){
			super.update();
			if (!exists) return;
			silk.x = ax;
			silk.y = ay;
			silk.size( 1, Math.max( 0f, oy ) );
			silk.alpha( alpha() * 0.55f );
			silk.visible = visible && oy > 0.5f;
		}

		@Override
		public void vanish(){
			silk.killAndErase();
			silk.destroy();
			super.vanish();
		}
	}

	/**
	 * A water strider out on the sewers' open water, seen from above: a push of its long legs, a
	 * glide that dies away, a little dimple in the surface where it pushed; it turns back from
	 * the bank, and darts off across the water when anything comes. It never sits on ice: if its
	 * water freezes (or drains) under it, it is gone.
	 */
	public static class Strider extends DungeonCritterSprite {

		private static final int REST = 0, GLIDE = 1, SHIFT = 2;

		private float heading, speed, strokeIn, strokeT, span, fleeT;
		private boolean shifted, fleeing;

		public Strider( float ax, float ay ){
			super( ax, ay );
			origin.set( 8, 8 );
			heading = rf( 0f, 6.2832f );
			strokeIn = rf( 0.3f, 1f );
			span = rf( 20f, 45f );
			pose( ROW_STRIDER, REST );
			angle = (float)Math.toDegrees( heading );
			shown = 0f;
		}

		@Override
		public boolean calm(){
			return !fleeing && !leaving();
		}

		@Override
		public void scare( float sx, float sy ){
			if (!calm()) return;
			if (shown <= 0f){
				vanish();
				return;
			}
			fleeing = true;
			fleeT = 0f;
			heading = awayFrom( sx, sy, 0.4f );
			strokeIn = 0f;
		}

		@Override
		protected void act( float dt ){
			if (!fleeing) shown = Math.min( 1f, shown + dt / 0.6f );
			if (!onWater( gx(), gy() )){
				//its water froze or drained under it
				leave( 0.4f );
			} else if (!onWater( gx() + (float)Math.cos( heading ) * 8f, gy() + (float)Math.sin( heading ) * 8f )){
				//back from the bank before it gets there
				heading += (float)Math.PI + rf( -0.6f, 0.6f );
				speed *= 0.3f;
			}
			//a push of the legs, then a glide that dies away
			if ((strokeIn -= dt) <= 0f){
				speed = fleeing ? rf( 60f, 75f ) : rf( 26f, 38f );
				strokeT = 0.12f;
				shifted = !shifted;
				if (!fleeing) heading += rf( -0.9f, 0.9f );
				strokeIn = fleeing ? 0.16f : rf( 0.6f, 1.6f );
				dimple();
			}
			ox += (float)Math.cos( heading ) * speed * dt;
			oy += (float)Math.sin( heading ) * speed * dt;
			speed *= (float)Math.exp( -3.2f * dt );
			strokeT -= dt;
			pose( ROW_STRIDER, strokeT > 0f ? GLIDE : shifted ? SHIFT : REST );
			//seen from above: it turns to where it is going
			angle = (float)Math.toDegrees( heading );
			if (fleeing){
				fleeT += dt;
				shown = Math.max( 0f, 1f - fleeT / 0.7f );
				if (fleeT >= 0.7f) vanish();
			} else if (life >= span){
				leave( 1f );
			}
		}

		//the little ring its push leaves on the water
		private void dimple(){
			if (visible && parent != null) SplashParticle.splash( parent, gx(), gy(), 0xD6E6EE, 0.35f );
		}
	}

	/**
	 * A fly caught in the nest's webbing: it keeps still, then struggles - wings a blur, tugging at
	 * the threads - buzzing now and then, the harder when anything comes near; once in a while one
	 * tears itself free and is off into the dark.
	 */
	public static class Fly extends DungeonCritterSprite {

		private static final int SIT = 0, BLUR = 1;
		private static final int[] FLY = { 2, 3 };

		private boolean free;
		private float t, struggle, stillFor, buzzIn, freeAfter, heading, speed, jitterIn;

		public Fly( float ax, float ay ){
			super( ax, ay );
			origin.set( 8, 12 );
			face( RNG.nextBoolean() );
			stillFor = rf( 0.5f, 3f );
			buzzIn = rf( 2f, 6f );
			freeAfter = rf( 12f, 40f );
			pose( ROW_FLY, SIT );
			shown = 0f;
		}

		@Override
		public boolean calm(){
			return !free && !leaving();
		}

		@Override
		public void scare( float sx, float sy ){
			if (free) return;
			//caught fast: all it can do is struggle the harder
			if (struggle <= 0f) buzz();
			struggle = Math.max( struggle, rf( 0.6f, 1.2f ) );
		}

		private void buzz(){
			if (buzzIn <= 0f && visible && voice( AmbientSound.FLY, 0.4f, rf( 1.1f, 1.35f ) )) buzzIn = rf( 8f, 15f );
		}

		@Override
		protected void act( float dt ){
			t += dt;
			buzzIn -= dt;
			if (free){
				fly( dt );
				return;
			}
			shown = Math.min( 1f, shown + dt / 0.6f );
			if (struggle > 0f){
				struggle -= dt;
				//the wings a blur, the body tugging at the threads
				pose( ROW_FLY, ((int)(t * 26f) & 1) == 0 ? BLUR : SIT );
				ox = ((int)(t * 12f) & 1) == 0 ? -1f : 0f;
				if (struggle <= 0f){
					ox = 0f;
					stillFor = rf( 1.5f, 5f );
				}
				return;
			}
			pose( ROW_FLY, SIT );
			if ((stillFor -= dt) <= 0f){
				//now and then one tears itself free
				if (life >= freeAfter && RNG.nextFloat() < 0.35f){
					free = true;
					t = 0f;
					heading = rf( 0f, 6.2832f );
					speed = 30f;
					takeWing();
					return;
				}
				struggle = rf( 0.4f, 1.2f );
				buzz();
			}
		}

		private void fly( float dt ){
			if ((jitterIn -= dt) <= 0f){
				jitterIn = rf( 0.08f, 0.2f );
				heading += rf( -1.2f, 1.2f );
			}
			speed = Math.min( 46f, speed + 40f * dt );
			ox += (float)Math.cos( heading ) * speed * dt;
			oy += (float)Math.sin( heading ) * speed * dt;
			z += 10f * dt;
			if (Math.abs( Math.cos( heading ) ) > 0.2f) face( Math.cos( heading ) < 0 );
			pose( ROW_FLY, FLY[(int)(t * 30f) & 1] );
			shown = t < 0.8f ? 1f : Math.max( 0f, 1f - (t - 0.8f) / 0.8f );
			if (t >= 1.6f) vanish();
		}
	}

	/**
	 * A bird on a statue's top: a swift on one of the city's dwarves, a carrion crow on a skull
	 * pillar of the halls - the surface's birds (effects/critters.png) on the dungeon's stone. It
	 * glides in and settles on the narrow top, turns about on it, preens, looks up when the hero
	 * comes close, and flies off - flapping up and away, growing as it climbs toward the eye,
	 * fading - when he comes closer. It never hops: up there is nowhere to hop to.
	 */
	public static class Percher extends DungeonCritterSprite {

		//the surface's sheet, tools/critters_gen.py, cut as CritterSprite cuts it
		private static final TextureFilm BIRDS = new TextureFilm( 128, 128, 16, 16 );
		private static final int PERCH = 0, PREEN = 1, WARY = 2, SPRING = 3, GLIDE = 7;
		private static final int[] FLAP = { 4, 5, 6, 5 };
		private static final int LANDING = 0, PERCHED = 1, FLYING = 2;

		public final boolean crow;
		private final int row;
		//its sight is the statue's own cell, this far below its feet
		private final float seenDY;
		//its while on the statue, then it is off of its own accord
		private final float span;
		private int state = PERCHED;
		private float t, delay, dur, fromX, fromY, fromZ, idle, preen;
		private float heading, speed, vmax, climb, alphaCap = 1f, scale0 = 1f;
		private boolean wary, airborne;

		public Percher( boolean crow, float ax, float ay, float seenDY ){
			super( Assets.Effects.CRITTERS, BIRDS, ax, ay );
			this.crow = crow;
			this.seenDY = seenDY;
			row = crow ? ROW_CROW : ROW_FINCH;
			origin.set( 8, 14 );
			pose( row, PERCH );
			idle = rf( 0.5f, 2f );
			span = rf( 45f, 100f );
		}

		/** Comes down onto its statue out of the air on `heading`'s far side, after `delay`. */
		public void land( float heading, float delay ){
			state = LANDING;
			t = 0f;
			this.delay = delay;
			fromX = -(float)Math.cos( heading ) * 56f;
			fromY = -(float)Math.sin( heading ) * 56f;
			fromZ = 36f;
			dur = rf( 1.0f, 1.3f );
			face( Math.cos( heading ) < 0 );
			ox = fromX;
			oy = fromY;
			z = fromZ;
			shown = 0f;
		}

		/** Still gliding in: where it will sit is its ground point. */
		public boolean landing(){
			return state == LANDING;
		}

		@Override
		public boolean calm(){
			return state != FLYING && !leaving();
		}

		@Override
		public void heed( float d ){
			wary = d <= 88f;
		}

		@Override
		public void scare( float sx, float sy ){
			flyOff( awayFrom( sx, sy, 0.45f ), 0f );
		}

		/** Up and away along `heading` after `delay`: the birds on a row of statues go one after another. */
		public void flyOff( float heading, float delay ){
			if (state == FLYING) return;
			airborne = state == LANDING;
			//not yet come into view: it simply never arrives
			if (airborne && shown <= 0f){
				vanish();
				return;
			}
			alphaCap = shown;
			scale0 = scale.x;
			state = FLYING;
			t = airborne ? 0f : -delay;
			this.heading = heading;
			speed = 40f;
			vmax = crow ? rf( 75f, 90f ) : rf( 95f, 115f );
			climb = rf( 26f, 34f );
			face( Math.cos( heading ) < 0 );
		}

		@Override
		protected float seenX(){
			return state == FLYING ? super.seenX() : ax;
		}

		@Override
		protected float seenY(){
			return state == FLYING ? super.seenY() : ay + seenDY;
		}

		//in the air it is seen wherever it flies; the fog dims it over what he cannot see
		@Override
		protected boolean hidesOutOfSight(){
			return state != FLYING;
		}

		@Override
		protected void act( float dt ){
			t += dt;
			if (state == LANDING) glideIn();
			else if (state == PERCHED) perched( dt );
			else flying( dt );
		}

		private void flap( float time, float fps ){
			pose( row, FLAP[(int)(time * fps) & 3] );
		}

		private void glideIn(){
			if (t < delay){
				shown = 0f;
				return;
			}
			float p = Math.min( 1f, (t - delay) / dur );
			float e = 1f - (1f - p) * (1f - p);
			ox = fromX * (1f - e);
			oy = fromY * (1f - e);
			z = fromZ * (1f - e);
			scale.set( 1f + 0.3f * (1f - e) );
			shown = Math.min( 1f, (t - delay) / 0.3f );
			if (p < 0.7f) flap( t - delay, 12f );
			else if (p < 0.92f) pose( row, GLIDE );
			else pose( row, SPRING );
			if (p >= 1f){
				state = PERCHED;
				ox = oy = z = 0f;
				scale.set( 1f );
				shown = 1f;
				pose( row, PERCH );
			}
		}

		private void perched( float dt ){
			shown = Math.min( 1f, shown + dt / 0.6f );
			if (life >= span){
				flyOff( rf( 0f, 6.2832f ), 0f );
				return;
			}
			if (wary){
				preen = 0f;
				pose( row, WARY );
				return;
			}
			if (preen > 0f){
				//the head down into its feathers, and up, and down
				preen -= dt;
				pose( row, ((int)(preen / 0.2f) & 1) == 0 ? PREEN : PERCH );
				return;
			}
			pose( row, PERCH );
			if ((idle -= dt) > 0f) return;
			//the swift is restless, the crow takes its time
			idle = crow ? rf( 2f, 5f ) : rf( 1f, 3f );
			float roll = RNG.nextFloat();
			if (roll < 0.45f) face( !flipHorizontal );
			else if (roll < 0.75f) preen = rf( 0.4f, 0.9f );
		}

		private void flying( float dt ){
			//the row's stagger: a beat of alarm first, then the spring off the stone
			if (t < 0f){
				pose( row, WARY );
				return;
			}
			if (!airborne && t < 0.08f){
				pose( row, SPRING );
				return;
			}
			speed = Math.min( vmax, speed + 90f * dt );
			ox += (float)Math.cos( heading ) * speed * dt;
			oy += (float)Math.sin( heading ) * speed * dt;
			z += climb * dt;
			scale.set( scale0 + Math.min( 0.35f, t * 0.25f ) );
			if (t > 1.1f && ((int)(t * 2.5f) & 1) == 1) pose( row, GLIDE );
			else flap( t, t < 0.5f ? 16f : 11f );
			shown = alphaCap * (t < 1f ? 1f : Math.max( 0f, 1f - (t - 1f) / 0.8f ));
			if (t >= 1.8f) vanish();
		}
	}
}
