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

import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;

import java.util.ArrayList;
import java.util.Random;

import static xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSound.*;

/**
 * When the weather's sounds play and from where (WeatherSounds, docs/ambience.md "Weather").
 *
 * The beds are laid as overlapped retriggers, never a loop: each play has its level and pan
 * fixed, and the next starts (length - lead) / pitch after it, where the last one's equal-power
 * fade-out begins, so the two cross at a steady power (docs/weather-sounds.md A.2). A change of
 * kind, level or pan waits for the next retrigger: a crossfade, never a cut. Four bed emitters:
 * the precipitation as two, panned to either side on their own beats with different takes, the
 * rain on the nearest water or leaves, and the wind from its side. The one-shots: a gust as the
 * rain leans over on screen, thunder (a far roll in heavy rain, a storm's strikes, one roll at a
 * time), drips off the trees in the fog and after the rain.
 *
 * At most MAX_PLAYS ring at once, a bed emitter counting as one (its newest play) and the thunder
 * as one (a crack breaking in over a far roll takes the roll's place): the beds the place may lay
 * always have theirs, the gusts, the drips and the heavy rain's rolls share the rest and wait for
 * room, and in a storm they leave the thunder's place free for its strikes.
 *
 * Its clock is only the time it is handed (tick), its dice the Random it is given, and the
 * weather the Sky it is told: the tests drive it frame by frame.
 */
final class WeatherScape {

	/** Where the hero is, for the weather: what reaches him there. */
	enum Where {
		/** under the open sky (the overworld's surface and peaks): everything */
		OPEN,
		/** a dungeon floor the climate reaches (climate depth 1-15): what falls, and the thunder;
		 *  no wind */
		EXPOSED,
		/** a town building or a village house: the rain on the roof, the storm's thunder half as
		 *  loud */
		INDOORS,
		/** the overworld's cave slices: only a storm's thunder, a far roll through the rock */
		CAVE,
		/** deeper than the climate reaches: nothing */
		SILENT
	}

	/** The weather as WeatherSounds reads it once a second: the targets the scape eases toward. */
	static final class Sky {
		Where where = Where.SILENT;
		PrecipType type = PrecipType.NONE;
		/** what falls, 0..1 (0 when the type is NONE) */
		float rate;
		/** m/s */
		float wind;
		/** degrees, the way it blows to: 0 up the screen, 90 right */
		float windDir;
		boolean storm;
		/** the fog, or the clearing after the rain: the trees drip */
		boolean dripping;
		/** on the surface in the desert: the wind lifts the sand */
		boolean desert;
		/** on the peaks: the wind blows harder */
		boolean peaks;
	}

	/** Where the sounds are played: the ambience channel in the game, a list in the tests. */
	interface Out {
		/**
		 * One take of a sound, exactly (no take picked and no nudge after), at a level (before its
		 * gain), a pitch and a pan. `bed` is the emitter that laid it (LEFT, RIGHT, NEAR, WIND), or
		 * ONE_SHOT.
		 */
		void play( int bed, AmbientSound sound, int take, float level, float pitch, float pan );
	}

	//weather plays ringing at once at most, a bed emitter counting as one, the thunder as one
	static final int MAX_PLAYS = 6;
	//the bed emitters: what falls, to the left and to the right; the rain on the water or the
	//leaves nearest; the wind
	static final int LEFT = 0, RIGHT = 1, NEAR = 2, WIND = 3, ONE_SHOT = -1;
	//the climate steps (a turn at a time, fronts handing over mid-curve): its targets are eased
	//toward over about this long (s)
	static final float SMOOTH = 1.5f;
	//a change in what falls is taken once it has held this long (s): the type flickers (SNOW and
	//BLIZZARD a turn or two at a time in a wind near 10 m/s), and a bed that followed each flick
	//would lurch between loud and quiet, one side against the other
	static final float TYPE_HOLD = 4f;
	//what falls: nothing under WET; the two emitters this far to either side
	static final float WET = 0.01f, FALL_PAN = 0.5f;
	//on the overworld each side is as loud as the share of its ground within SIDE_CELLS that what
	//falls lands on (the overlay draws the rain over thawed ground only, the snow over frozen); a
	//side with less than SIDE_MIN of it lays nothing
	static final int SIDE_CELLS = 8;
	static final float SIDE_MIN = 0.1f;
	//a side coming in just after the other has: this long after it (s), both at once to the ear
	//but never the same file in line
	static final float JOIN_LAG = 0.3f;
	//the rain's beds by rate: light under LIGHT_TOP, steady under STEADY_TOP, heavy above; a bed
	//is at full by the top of its band (the heavy at a storm's rate, the others' at HEAVY_PRECIP)
	static final float LIGHT_TOP = 0.15f, STEADY_TOP = 0.35f, HEAVY_TOP = 0.6f, OTHER_TOP = 0.4f;
	//a bed's level at the bottom of its band: the first at about half, a heavier rain's 3 dB
	//under its full, the lighter one's full (its gain is 3 dB over the lighter's): never a step
	static final float FIRST_LEVEL = 0.45f, NEXT_LEVEL = 0.71f;
	//the rain on the water or the leaves: from this much rain, the nearest within this many cells
	static final float NEAR_RAIN = 0.05f;
	static final int NEAR_CELLS = 6;
	//a source lost: looked for again this soon (s)
	static final float LOOK_AGAIN = 0.5f;
	//the gale: from GALE_WIND (at GALE_FLOOR) to full at GALE_FULL m/s, harder on the peaks;
	//from the wind's side, never further than WIND_PAN
	static final float GALE_WIND = 10f, GALE_FULL = 18f, GALE_FLOOR = 0.4f, PEAKS_GALE = 1.15f, WIND_PAN = 0.6f;
	//a gust: from GUST_WIND (at GUST_FLOOR) to full at GUST_FULL m/s, as the visuals' gusting
	//rises through GUST_AT, at most one every GUST_EVERY s, its pan strayed this far
	static final float GUST_WIND = 7f, GUST_FULL = 16f, GUST_FLOOR = 0.5f, GUST_AT = 0.5f, GUST_EVERY = 4f, GUST_JITTER = 0.15f;
	//a far roll in heavy rain, every ROLL_MIN to ROLL_MAX s, panned within ROLL_PAN
	static final float ROLL_RAIN = 0.35f, ROLL_MIN = 25f, ROLL_MAX = 70f, ROLL_PAN = 0.5f;
	//a far roll's distance, out of sight (WeatherOverlay's sheet lightning)
	static final float FAR_MIN = 10f, FAR_MAX = 24f;
	//a strike this near (tiles) cracks, as does one in sight; indoors the thunder is half as loud,
	//in the caves a far roll at this much
	static final float NEAR_TILES = 8f, INDOORS_THUNDER = 0.5f, CAVE_THUNDER = 0.35f;
	//when the thunder starts after its flash (s): a crack at once, CRACK_DELAY plus CRACK_TILE a
	//tile, never later than CRACK_LATEST (its file is loud from its 35th ms); a far roll ROLL_DELAY
	//plus ROLL_TILE a tile, never later than ROLL_LATEST (its file swells for 1.6-2 s: started any
	//later, the rumble no longer belongs to the flash). Thunder still due LATE past its time (it
	//waited out the quiet of a fresh start) is dropped: never heard long after its flash
	static final float CRACK_DELAY = 0.03f, CRACK_TILE = 0.012f, CRACK_LATEST = 0.15f;
	static final float ROLL_DELAY = 0.15f, ROLL_TILE = 0.015f, ROLL_LATEST = 0.5f;
	static final float LATE = 0.5f;
	//the drips off the trees, every DRIP_MIN to DRIP_MAX s
	static final float DRIP_MIN = 2f, DRIP_MAX = 6f;

	/** A bed emitter: its newest play, and when it plays again. */
	static final class Emitter {
		/** the newest play's sound (null before the first), take, start, pitch and end */
		AmbientSound sound;
		int take = -1;
		float start, pitch = 1f, end;
		/** the cell it came from (the rain on the water or the leaves), -1 for none */
		int cell = -1;
		/** when it plays next: its beat */
		float next;
		/** a side joining the other: when it plays again after its first play, -1 for none */
		float then = -1f;
		/** laying its bed */
		boolean on;
	}

	//a storm's strike: its thunder to come
	private static final class Strike {
		final float at, level, pan;
		final boolean near;

		Strike( float at, boolean near, float level, float pan ){
			this.at = at;
			this.near = near;
			this.level = level;
			this.pan = pan;
		}
	}

	/** what WeatherSounds read of the climate */
	final Sky sky = new Sky();
	final Emitter[] beds = { new Emitter(), new Emitter(), new Emitter(), new Emitter() };
	private final Random rng;
	/** the clock: seconds since it started */
	float now;
	private float quietUntil;
	private boolean hushed, fresh;
	//the eased climate: what falls, the wind, and the wind's side (-1 left .. 1 right)
	float rate, wind, side;
	//what falls as heard: the sky's type once it has held TYPE_HOLD; the one waiting, and since when
	PrecipType type = PrecipType.NONE;
	private PrecipType coming = PrecipType.NONE;
	private float comingSince;
	//the gusts' and the drips' ends, while they ring
	private final ArrayList<Float> rings = new ArrayList<>();
	//the thunder: when the rolling rings out, and whether the newest roll is a near one's
	float rollEnd;
	private boolean rollNear;
	private final ArrayList<Strike> strikes = new ArrayList<>();
	//the next far roll of the heavy rain, the next drip (-1: not counting), the gusting's last
	//value (NaN: none yet), a gust waiting for room, and the earliest the next may come
	private float rollDue = -1f, dripDue = -1f, lastGust = Float.NaN, gustNext;
	private boolean gustWaiting;

	WeatherScape( Random rng ){
		this.rng = rng;
		start();
	}

	// ------------------------------------------------------------ the rules

	/** The bed of what falls here, of this type at this rate: null for none. */
	static AmbientSound fall( Where where, PrecipType type, float rate ){
		if (type == null || rate < WET) return null;
		switch (where){
			case INDOORS:
				//on the roof: the rain, the sleet, the hail; snow lands without a sound
				return type == PrecipType.RAIN || type == PrecipType.SLEET || type == PrecipType.HAIL ? RAIN_ROOF : null;
			case OPEN:
			case EXPOSED:
				switch (type){
					case RAIN:     return rate < LIGHT_TOP ? RAIN_LIGHT : rate < STEADY_TOP ? RAIN : RAIN_HEAVY;
					case SLEET:    return SLEET;
					case HAIL:     return HAIL;
					case SNOW:     return SNOW;
					case BLIZZARD: return BLIZZARD;
					default:       return null;
				}
			default:
				return null;
		}
	}

	/** A bed of what falls at this rate: from its band's bottom level to 1 by its band's top. */
	static float intensity( AmbientSound bed, float rate ){
		float lo = WET, hi = OTHER_TOP, floor = FIRST_LEVEL;
		if (bed == RAIN_LIGHT){
			hi = LIGHT_TOP;
		} else if (bed == RAIN){
			lo = LIGHT_TOP;
			hi = STEADY_TOP;
			floor = NEXT_LEVEL;
		} else if (bed == RAIN_HEAVY){
			lo = STEADY_TOP;
			hi = HEAVY_TOP;
			floor = NEXT_LEVEL;
		}
		return floor + (1f - floor) * clamp01( (rate - lo) / (hi - lo) );
	}

	/** The wind's bed here, at this wind with this falling (its bed): null for none. A blizzard
	 *  is its own wind; in the desert the wind is sand. */
	static AmbientSound wind( Where where, AmbientSound falling, float wind, boolean desert ){
		if (where != Where.OPEN || wind < GALE_WIND || falling == BLIZZARD) return null;
		return desert ? SANDSTORM : GALE;
	}

	/** The wind bed's level at this wind. */
	static float galeLevel( float wind, boolean peaks ){
		float l = GALE_FLOOR + (1f - GALE_FLOOR) * clamp01( (wind - GALE_WIND) / (GALE_FULL - GALE_WIND) );
		return peaks ? Math.min( 1f, l * PEAKS_GALE ) : l;
	}

	/** A gust's level at this wind. */
	static float gustLevel( float wind ){
		return GUST_FLOOR + (1f - GUST_FLOOR) * clamp01( (wind - GUST_WIND) / (GUST_FULL - GUST_WIND) );
	}

	/** The wind's side, for a wind blowing to this bearing: it comes from the other way. -1 is
	 *  the left, 1 the right; a wind up or down the screen is in the middle. */
	static float side( float dir ){
		return (float)-Math.sin( Math.toRadians( dir ) );
	}

	/** The pan of a sound from the wind's side. */
	static float windPan( float side ){
		return Math.max( -WIND_PAN, Math.min( WIND_PAN, side * WIND_PAN ) );
	}

	/** Whether a strike's thunder is a crack: a bolt the hero sees, or one within NEAR_TILES; never
	 *  sheet lightning, far behind the clouds. */
	static boolean cracks( float tiles, boolean sheet, boolean seen ){
		return !sheet && (seen || tiles <= NEAR_TILES);
	}

	/** How long after its flash a strike's thunder starts, `tiles` away: with the flash for a crack,
	 *  within half a second for a far roll, a little later the further off. */
	static float delay( float tiles, boolean crack ){
		return crack ? Math.min( CRACK_LATEST, CRACK_DELAY + tiles * CRACK_TILE )
				: Math.min( ROLL_LATEST, ROLL_DELAY + tiles * ROLL_TILE );
	}

	/** A strike's thunder's level (WeatherOverlay's): duller the further. */
	static float thunderLevel( float tiles ){
		return Math.max( 0.25f, 1f - tiles * 0.035f );
	}

	/** How many bed emitters a place may lay: they always have room. */
	static int bedSlots( Where where ){
		switch (where){
			case OPEN:     return 4;
			case EXPOSED:
			case INDOORS:  return 2;
			default:       return 0;
		}
	}

	/** Rain on open water here: liquid, not under the snow. */
	static boolean water( Soundscape.Ground g, int cell ){
		return Source.liquid( g.map[cell] ) && !g.frozen( cell );
	}

	/** Leaves for the rain to patter on: a tree (the overworld's canopy), a bush, long grass,
	 *  none under the snow. */
	static boolean leaves( Soundscape.Ground g, int cell ){
		int t = g.map[cell];
		return (tree( t ) || t == Terrain.HIGH_GRASS) && !g.frozen( cell );
	}

	/** Leaves to drip from: a tree or a bush, none under the snow. */
	static boolean dripping( Soundscape.Ground g, int cell ){
		return tree( g.map[cell] ) && !g.frozen( cell );
	}

	private static boolean tree( int t ){
		return t == Terrain.TREE_OAK || t == Terrain.TREE_PINE || t == Terrain.SHRUB;
	}

	private static float clamp01( float v ){
		return Math.max( 0f, Math.min( 1f, v ) );
	}

	// ------------------------------------------------------------ the bookkeeping

	/** How many bed emitters hold their place: laying their bed, or their last play still ringing. */
	int bedsHolding(){
		int n = 0;
		for (Emitter e : beds) if (e.on || now < e.end) n++;
		return n;
	}

	/** How many gusts and drips ring now (the thunder keeps its own place). */
	int oneShotsRinging(){
		for (int i = rings.size() - 1; i >= 0; i--) if (rings.get( i ) <= now) rings.remove( i );
		return rings.size();
	}

	/** The thunder's place: taken while it rolls. One roll at a time, and a crack breaking in over
	 *  a far roll takes the roll's place, as a bed's next play takes its last's. */
	int thunderRinging(){
		return now < rollEnd ? 1 : 0;
	}

	/** How many weather plays ring now, a bed emitter counting as one and the thunder as one. */
	int ringing(){
		return bedsHolding() + thunderRinging() + oneShotsRinging();
	}

	//the places the beds keep for them, whether they play or not
	private int bedPlaces(){
		return Math.max( bedSlots( sky.where ), bedsHolding() );
	}

	//room for a gust, a drip or a heavy rain's roll: the beds' places kept for them, and the
	//thunder's while it rolls or, in a storm, for its next strike
	private boolean room(){
		int thunder = now < rollEnd || sky.storm ? 1 : 0;
		return bedPlaces() + thunder + oneShotsRinging() + 1 <= MAX_PLAYS;
	}

	//the quiet before the first sound, and the eased climate to take the sky as it is: on a new
	//level, and when the weather is heard again after a silence
	private void start(){
		quietUntil = now + Soundscape.QUIET_MIN + rng.nextFloat() * (Soundscape.QUIET_MAX - Soundscape.QUIET_MIN);
		fresh = true;
		rollDue = -1f;
		dripDue = -1f;
		lastGust = Float.NaN;
		gustWaiting = false;
		strikes.clear();
		for (Emitter e : beds){
			e.on = false;
			e.then = -1f;
		}
	}

	/**
	 * A frame of silence (the ambience off, the game in the background, the hero dead): the beds
	 * stop, what was to come is dropped, and once it is heard again it starts over after a quiet.
	 * The clock still runs, so what rang before rings out.
	 */
	void hush( float dt ){
		now += dt;
		if (hushed) return;
		hushed = true;
		start();
	}

	/** The overworld's window moved by (dx, dy) cells under the hero (OverworldLevel.rebase): the
	 *  cell the rain was heard on goes with it, or is dropped if left behind. */
	void shift( int dx, int dy, int w, int h ){
		Emitter e = beds[NEAR];
		e.cell = Soundscape.moved( e.cell, dx, dy, w, h );
	}

	/**
	 * A strike, `tiles` away and `dx` to the hero's right (sheet lightning: no bolt, far behind the
	 * clouds), `seen` when the hero sees the bolt itself: its thunder as dull as it is far, a crack
	 * and its roll with the flash when it is in sight or near, a far roll otherwise, starting within
	 * half a second of the flash. A bolt in sight is in the hero's own place (a storm cloud's, on
	 * any floor or in a cave): heard there at its full whatever reaches him of the weather, and
	 * never silent: false (handed back, for the overlay's own crack with its flash) in the quiet of
	 * a fresh start, where its thunder would come long after its flash, and while a crack still
	 * rolls, where it would be lost.
	 */
	boolean strike( float dx, float tiles, boolean sheet, boolean seen ){
		Where w = sky.where;
		if (w == Where.SILENT && !seen) return true;
		if (seen && (now < quietUntil || (rollNear && now < rollEnd))) return false;
		boolean near = cracks( tiles, sheet, seen ) && (seen || w != Where.CAVE);
		float level = thunderLevel( tiles );
		if (!seen && w == Where.INDOORS) level *= INDOORS_THUNDER;
		if (!seen && w == Where.CAVE) level *= CAVE_THUNDER;
		strikes.add( new Strike( now + delay( tiles, near ), near, level, AmbientPlayer.pan( dx ) ) );
		return true;
	}

	/** A frame of `dt` seconds: the climate eased toward the sky's, and what is due played on
	 *  `out`, from `g` as it stands, with the visuals' gusting at `gust` (-1..1). */
	void tick( float dt, Soundscape.Ground g, float gust, Out out ){
		if (hushed){
			hushed = false;
			start();
		}
		now += dt;
		ease( dt );
		boolean rising = !Float.isNaN( lastGust ) && lastGust < GUST_AT && gust >= GUST_AT;
		lastGust = gust;
		if (now < quietUntil) return;

		AmbientSound falling = fall( sky.where, type, rate );
		float level = falling == null ? 0f : intensity( falling, rate );
		float left = level * fallsOn( g, falling, -1 ), right = level * fallsOn( g, falling, 1 );
		pair( LEFT, falling, left, -FALL_PAN, out );
		pair( RIGHT, falling, right, FALL_PAN, out );
		near( g, falling == null ? 0f : level, out );
		//a blizzard falling on neither side is not heard: the wind blows as the gale instead
		AmbientSound blowing = wind( sky.where, left > 0f || right > 0f ? falling : null, wind, sky.desert );
		bed( WIND, blowing, galeLevel( wind, sky.peaks ), windPan( side ), -1, out );

		thunder( out );
		roll( out );
		gusts( rising, gust, out );
		drips( g, out );
	}

	//the climate's targets, eased toward; taken as they are on a fresh start
	private void ease( float dt ){
		float s = side( sky.windDir );
		PrecipType t = sky.type == null ? PrecipType.NONE : sky.type;
		if (fresh){
			fresh = false;
			rate = sky.rate;
			wind = sky.wind;
			side = s;
			type = coming = t;
			return;
		}
		float k = 1f - (float)Math.exp( -dt / SMOOTH );
		rate += (sky.rate - rate) * k;
		wind += (sky.wind - wind) * k;
		side += (s - side) * k;
		settle( t );
	}

	//what falls, once a change has held: but what starts to fall out of a dry sky is taken at
	//once, its rate easing in from nothing
	private void settle( PrecipType t ){
		if (t == type){
			coming = t;
			return;
		}
		if (type == PrecipType.NONE){
			type = coming = t;
			return;
		}
		if (t != coming){
			coming = t;
			comingSince = now;
		}
		if (now - comingSince >= TYPE_HOLD) type = t;
	}

	//one of the two emitters of what falls. One coming in while the other lays the same bed takes
	//the beat half a step off the other's, so the pair never lines up: when the other has only
	//just started (a fresh pair) it comes in JOIN_LAG after it, both sides at once and never one ear
	//alone, and plays again half a step after the other's start; else it waits for that half step.
	//From then on each keeps half a step off the other (lock)
	private void pair( int i, AmbientSound want, float level, float pan, Out out ){
		Emitter e = beds[i], other = beds[1 - i];
		if (want != null && level > 0f && !e.on && now >= e.end && other.on && other.sound == want){
			float half = other.start + (want.length - want.lead) / other.pitch / 2f;
			if (now - other.start < JOIN_LAG){
				e.on = true;
				e.next = other.start + JOIN_LAG;
				e.then = half;
			} else if (half > now){
				e.on = true;
				e.next = half;
			}
		}
		if (!bed( i, want, level, pan, -1, out )) return;
		if (e.then >= 0f){
			e.next = e.then;
			e.then = -1f;
		} else {
			lock( e, other );
		}
	}

	//a side's next play aimed half a step past the other's next, as near as its own jitter allows
	//(both pull, so the pair holds against two nudges): on their own jitter and nudges the two
	//would drift about a quarter of a second a step, into line within minutes, and a bed of one
	//take would phase against itself
	private void lock( Emitter e, Emitter other ){
		if (!other.on || other.sound == null || other.next < e.start) return;
		float step = (e.sound.length - e.sound.lead) / e.pitch;
		float aim = other.next + (other.sound.length - other.sound.lead) / other.pitch / 2f;
		e.next = Math.max( e.start + step * (1f - Soundscape.BED_JITTER),
				Math.min( e.start + step * (1f + Soundscape.BED_JITTER), aim ) );
	}

	//how much of the ground to one side of the hero (-1 the left, 1 the right, his own column on
	//both) what falls lands on, as WeatherOverlay draws it on the overworld: the rain on thawed
	//ground, the snow and the blizzard on snowed-under ground, the sleet (it splits) and the hail
	//anywhere. A side with less than SIDE_MIN of it lays nothing; off the overworld it is all ground
	static float fallsOn( Soundscape.Ground g, AmbientSound bed, int dir ){
		if (bed == null || g.world == null) return 1f;
		boolean frozen;
		if (bed == RAIN_LIGHT || bed == RAIN || bed == RAIN_HEAVY) frozen = false;
		else if (bed == SNOW || bed == BLIZZARD) frozen = true;
		else return 1f;
		int hx = g.hero % g.w, hy = g.hero / g.w, all = 0, on = 0;
		for (int dy = -SIDE_CELLS; dy <= SIDE_CELLS; dy++){
			for (int dx = 0; dx <= SIDE_CELLS; dx++){
				int x = hx + dx * dir, y = hy + dy;
				if (x < 0 || y < 0 || x >= g.w || y >= g.h || dx * dx + dy * dy > SIDE_CELLS * SIDE_CELLS) continue;
				all++;
				if (g.frozen( x + y * g.w ) == frozen) on++;
			}
		}
		float share = all == 0 ? 0f : on / (float) all;
		return share < SIDE_MIN ? 0f : share;
	}

	//the rain on the nearest open water within reach, or else on the nearest leaves: picked again,
	//and its pan and level with it, at every play
	private void near( Soundscape.Ground g, float rain, Out out ){
		Emitter e = beds[NEAR];
		if (sky.where != Where.OPEN || type != PrecipType.RAIN || rate < NEAR_RAIN){
			e.on = false;
			return;
		}
		if (now < e.next) return;
		boolean onWater = true;
		int c = nearest( g, true );
		if (c < 0){
			onWater = false;
			c = nearest( g, false );
		}
		if (c < 0){
			e.on = false;
			e.next = now + LOOK_AGAIN;
			return;
		}
		int dx = c % g.w - g.hero % g.w;
		bed( NEAR, onWater ? RAIN_WATER : RAIN_LEAVES, AmbientPlayer.falloff( g.away( c ) ) * rain,
				AmbientPlayer.pan( dx ), c, out );
	}

	//the nearest cell of water (or of leaves) within reach of the hero, the one it was heard on
	//before kept over another as near; -1 for none
	private int nearest( Soundscape.Ground g, boolean water ){
		int hx = g.hero % g.w, hy = g.hero / g.w;
		int best = -1, kept = beds[NEAR].cell;
		float bestAt = Float.MAX_VALUE;
		for (int dy = -NEAR_CELLS; dy <= NEAR_CELLS; dy++){
			for (int dx = -NEAR_CELLS; dx <= NEAR_CELLS; dx++){
				int x = hx + dx, y = hy + dy;
				if (x < 0 || y < 0 || x >= g.w || y >= g.h || dx * dx + dy * dy > NEAR_CELLS * NEAR_CELLS) continue;
				int c = x + y * g.w;
				if (water ? !water( g, c ) : !leaves( g, c )) continue;
				float d = (float)Math.sqrt( dx * dx + dy * dy );
				if (d < bestAt || (d == bestAt && c == kept)){
					best = c;
					bestAt = d;
				}
			}
		}
		return best;
	}

	//a bed emitter's frame: off while there is nothing to lay, else a play on its beat, at the
	//level and pan of the moment, its next a step on. Whether it played
	private boolean bed( int i, AmbientSound want, float level, float pan, int cell, Out out ){
		Emitter e = beds[i];
		if (want == null || level <= 0f){
			//what it last played rings out
			e.on = false;
			e.then = -1f;
			return false;
		}
		if (!e.on){
			e.on = true;
			//on its beat if its last play still sounds; a fresh bed's beat is long past
			e.next = Math.max( now, e.next );
		}
		if (now < e.next) return false;
		float pitch = AmbientPlayer.nudge( rng, 1f );
		int take = take( i, want );
		out.play( i, want, take, level, pitch, pan );
		e.sound = want;
		e.take = take;
		e.start = now;
		e.pitch = pitch;
		e.end = now + want.length / pitch;
		e.cell = cell;
		e.next = now + (want.length - want.lead) / pitch * (1f + (rng.nextFloat() * 2f - 1f) * Soundscape.BED_JITTER);
		return true;
	}

	//a take for a bed's play: never the one the other side of what falls is playing, when the
	//sound has another
	private int take( int i, AmbientSound s ){
		int n = s.takes.length;
		if (n == 1) return 0;
		int avoid = -1;
		if (i == LEFT || i == RIGHT){
			Emitter other = beds[1 - i];
			if (other.sound == s && now < other.end) avoid = other.take;
		}
		if (avoid < 0) return rng.nextInt( n );
		int t = rng.nextInt( n - 1 );
		return t >= avoid ? t + 1 : t;
	}

	//a storm's strikes as their thunder comes due: one roll at a time, but a crack may break in
	//over a far roll, in its place; one over a roll already rolling is lost in it. The gusts and
	//the drips leave the thunder's place free in a storm, so a strike only finds no room in the
	//moment a storm begins over two of them (and is lost too)
	private void thunder( Out out ){
		for (int k = 0; k < strikes.size(); ){
			Strike s = strikes.get( k );
			if (s.at > now){
				k++;
				continue;
			}
			strikes.remove( k );
			if (now - s.at > LATE) continue;
			boolean rolling = now < rollEnd;
			if (rolling && (!s.near || rollNear)) continue;
			if (!rolling && bedPlaces() + oneShotsRinging() + 1 > MAX_PLAYS) continue;
			oneShot( s.near ? THUNDER_NEAR : THUNDER_FAR, s.level, s.pan, out );
			rollNear = s.near;
		}
	}

	//a far roll now and then in the heavy rain, out of sight (a storm's strikes bring their own)
	private void roll( Out out ){
		boolean rolls = !sky.storm && (sky.where == Where.OPEN || sky.where == Where.EXPOSED)
				&& type == PrecipType.RAIN && rate >= ROLL_RAIN;
		if (!rolls){
			rollDue = -1f;
			return;
		}
		if (rollDue < 0f) rollDue = now + ROLL_MIN + rng.nextFloat() * (ROLL_MAX - ROLL_MIN);
		if (now < rollDue) return;
		if (now < rollEnd){
			rollDue = rollEnd;
			return;
		}
		if (!room()){
			rollDue = now + retry();
			return;
		}
		float tiles = FAR_MIN + rng.nextFloat() * (FAR_MAX - FAR_MIN);
		oneShot( THUNDER_FAR, thunderLevel( tiles ), (rng.nextFloat() * 2f - 1f) * ROLL_PAN, out );
		rollNear = false;
		rollDue = now + ROLL_MIN + rng.nextFloat() * (ROLL_MAX - ROLL_MIN);
	}

	//a gust as the visuals' gusting rises through GUST_AT (the rain leans over with it), from the
	//wind's side; one that finds no room waits while the gust lasts
	private void gusts( boolean rising, float gust, Out out ){
		if (sky.where != Where.OPEN || wind < GUST_WIND || gust < GUST_AT){
			gustWaiting = false;
			return;
		}
		if (rising && now >= gustNext) gustWaiting = true;
		if (!gustWaiting || !room()) return;
		gustWaiting = false;
		gustNext = now + GUST_EVERY;
		float pan = windPan( side ) + (rng.nextFloat() * 2f - 1f) * GUST_JITTER;
		oneShot( GUST, gustLevel( wind ), Math.max( -AmbientPlayer.MAX_PAN, Math.min( AmbientPlayer.MAX_PAN, pan ) ), out );
	}

	//drops off the trees near the hero in the fog and after the rain, on the surface
	private void drips( Soundscape.Ground g, Out out ){
		if (sky.where != Where.OPEN || sky.peaks || !sky.dripping){
			dripDue = -1f;
			return;
		}
		if (dripDue < 0f) dripDue = now + dripWait();
		if (now < dripDue) return;
		if (!room()){
			dripDue = now + retry();
			return;
		}
		dripDue = now + dripWait();
		int c = anyTree( g );
		if (c < 0) return;
		oneShot( DRIP_LEAF, AmbientPlayer.falloff( g.away( c ) ), AmbientPlayer.pan( c % g.w - g.hero % g.w ), out );
	}

	//a tree within reach of the hero, any of them; -1 for none
	private int anyTree( Soundscape.Ground g ){
		int hx = g.hero % g.w, hy = g.hero / g.w, found = 0, pick = -1;
		for (int dy = -NEAR_CELLS; dy <= NEAR_CELLS; dy++){
			for (int dx = -NEAR_CELLS; dx <= NEAR_CELLS; dx++){
				int x = hx + dx, y = hy + dy;
				if (x < 0 || y < 0 || x >= g.w || y >= g.h || dx * dx + dy * dy > NEAR_CELLS * NEAR_CELLS) continue;
				int c = x + y * g.w;
				//each one found takes the place of the pick by a fair draw: any is as likely
				if (dripping( g, c ) && rng.nextInt( ++found ) == 0) pick = c;
			}
		}
		return pick;
	}

	//a one-shot: a take and a pitch of its own, held as ringing to its end (the thunder in its
	//own place)
	private void oneShot( AmbientSound s, float level, float pan, Out out ){
		float pitch = AmbientPlayer.nudge( rng, 1f );
		out.play( ONE_SHOT, s, rng.nextInt( s.takes.length ), level, pitch, pan );
		float end = now + s.length / pitch;
		if (s == THUNDER_FAR || s == THUNDER_NEAR) rollEnd = Math.max( rollEnd, end );
		else rings.add( end );
	}

	private float retry(){
		return Soundscape.RETRY_MIN + rng.nextFloat() * (Soundscape.RETRY_MAX - Soundscape.RETRY_MIN);
	}

	private float dripWait(){
		return DRIP_MIN + rng.nextFloat() * (DRIP_MAX - DRIP_MIN);
	}
}
