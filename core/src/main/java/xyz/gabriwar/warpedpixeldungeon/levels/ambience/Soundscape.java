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

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * When a place's voices sound and from where (AmbientSounds), at most MAX_VOICES at once, a
 * bed counting as one for as long as it plays on. A voice waits a time drawn about its mean
 * (nextInterval), then plays from a source cell found near the hero by trying random cells
 * around him, or from the air; one due while every voice is taken tries again a moment later.
 * A bed keeps track of its nearest source within its reach, a few random cells a frame, and
 * plays again a little before its last play ends for as long as there is one, keeping that beat
 * through a source lost and found again; while a bed waits for a voice to fall silent, nothing
 * else starts. A source with rock between it and the hero is heard through it, muffled - the
 * dark alone muffles nothing, and what he sees is never muffled.
 *
 * Every sound's take and pitch are picked here, so an echo is the very sound it throws back.
 * Its clock is only the time it is handed (tick) and its dice the Random it is given: the
 * tests drive it frame by frame.
 */
final class Soundscape {

	/** The hour, the season and the weather the voices keep (AmbientSounds reads them once a second). */
	static final class Air {
		DayNightCycle.Phase phase = DayNightCycle.Phase.DAY;
		GameCalendar.Season season = GameCalendar.Season.SUMMER;
		boolean storm;
		/** rain or snow falling, any kind: 0 for none */
		float rain;
		/** m/s */
		float wind;
		/** °C */
		float temp = 15f;
		int depth = 1, branch;
		/** the surface's biome where the hero stands, null off the surface */
		WorldModel.Biome biome;
	}

	/** The map the sources are found on and the hero on it: AmbientSounds points it at the level each frame. */
	static final class Ground {
		int[] map;
		int w, h;
		int hero;
		/** the cells the hero sees (null for none): the far-off voices keep to the others */
		boolean[] seen;
		/** the overworld's level, for its frozen ground: null elsewhere */
		OverworldLevel world;
		/** the cells of the floor's secret rooms, rim and all (null for none), and those the hero
		 *  has explored or mapped: a secret room he has not found keeps its sounds to itself */
		boolean[] secret, visited, mapped;

		Ground set( int[] map, int w, int h, int hero, boolean[] seen ){
			this.map = map;
			this.w = w;
			this.h = h;
			this.hero = hero;
			this.seen = seen;
			return this;
		}

		boolean seen( int cell ){
			boolean[] s = seen;
			return s != null && cell >= 0 && cell < s.length && s[cell];
		}

		boolean frozen( int cell ){
			return world != null && world.frozenAt( cell );
		}

		/** Is this cell in a secret room not found yet (neither explored nor mapped)? Nothing there is heard. */
		boolean hidden( int cell ){
			boolean[] s = secret;
			if (s == null || cell < 0 || cell >= s.length || !s[cell]) return false;
			return !(visited != null && visited[cell]) && !(mapped != null && mapped[cell]);
		}

		/** Inside the map's outer ring, where a source rule may read all eight neighbours. */
		boolean inner( int x, int y ){
			return x >= 1 && y >= 1 && x <= w - 2 && y <= h - 2;
		}

		boolean inner( int cell ){
			return cell >= 0 && cell < w * h && inner( cell % w, cell / w );
		}

		/** How far this cell is from the hero, in cells. */
		float away( int cell ){
			float dx = cell % w - hero % w, dy = cell / w - hero / w;
			return (float)Math.sqrt( dx * dx + dy * dy );
		}
	}

	/** Where the voices are played: the ambience channel in the game, a list in the tests. */
	interface Out {
		/**
		 * One of a voice's takes at a level and a pitch, exactly those (no take picked and no
		 * nudge after), from a cell (falling off with its distance from the hero and panned to its
		 * side) or, at cell -1, from the air, centred. An echo is the sound thrown back by the far
		 * rock: from the other side.
		 */
		void play( Voice v, int take, int cell, float level, float pitch, boolean echo );
	}

	//voices sounding at once at most, a bed counting as one while it plays on
	static final int MAX_VOICES = 3;
	//the floor's first sound comes this long after it loads (or after a silence)
	static final float QUIET_MIN = 1.5f, QUIET_MAX = 3f;
	//a voice due while every voice is taken tries again this soon
	static final float RETRY_MIN = 0.4f, RETRY_MAX = 1.2f;
	//random cells tried for a sound's source (for each of its rules), and for a bed's nearest each frame
	static final int PROBES = 96, BED_PROBES = 24;
	//a source behind the rock is heard through it, muffled
	static final float MUFFLED = 0.6f;
	//an echo: a moment later, quieter, a touch lower
	static final float ECHO_MIN = 0.16f, ECHO_MAX = 0.3f, ECHO_LEVEL = 0.35f, ECHO_PITCH = 0.97f;
	//a chorus: now and then one or two more join in, a moment apart each
	static final float CHORUS_CHANCE = 0.3f, CHORUS_GAP_MIN = 0.25f, CHORUS_GAP_MAX = 0.8f;
	//an answer: now and then another, a little after
	static final float ANSWER_CHANCE = 0.35f, ANSWER_MIN = 0.6f, ANSWER_MAX = 1.6f;
	//how far a bed's plays may stray from their beat, either way
	static final float BED_JITTER = 0.05f;
	//frames the first sound after the quiet is looked for, before the voices are left to their own waits
	private static final int OPENINGS = 3;

	final Voice[] voices;
	private final Random rng;
	/** the clock: seconds since the floor loaded */
	float now;
	private float quietUntil;
	private int opening;
	private boolean hushed;
	//when each voice is due next, and when what it last played has rung out
	private final float[] due, until;
	//the beds: the nearest source in reach (-1 for none), when each plays next, and which play on
	final int[] bedAt;
	private final float[] bedNext;
	final boolean[] on;
	//the voice the one-shots are looked at from, a frame's turn each: none always first in line
	private int turn;
	//sounds to come: echoes, the rest of a chorus, answers
	private final ArrayList<Later> later = new ArrayList<>();

	private static final class Later {
		//the voice it belongs to (its index), and what it plays
		final int voice;
		final Voice v;
		final int take;
		int cell;
		final float at, level, pitch;
		final boolean echo;

		Later( int voice, Voice v, int take, int cell, float at, float level, float pitch, boolean echo ){
			this.voice = voice;
			this.v = v;
			this.take = take;
			this.cell = cell;
			this.at = at;
			this.level = level;
			this.pitch = pitch;
			this.echo = echo;
		}
	}

	Soundscape( Voice[] voices, Random rng ){
		this.voices = voices;
		this.rng = rng;
		int n = voices.length;
		due = new float[n];
		until = new float[n];
		bedAt = new int[n];
		bedNext = new float[n];
		on = new boolean[n];
		Arrays.fill( bedAt, -1 );
		start();
	}

	// ------------------------------------------------------------ the rules

	/**
	 * The wait before a voice's next sound, for a roll of the dice (0..1): exponential about its
	 * mean, as the gaps of a Poisson process are - never a metronome - but never under a third of
	 * the mean, which would bunch them up, nor over four times it.
	 */
	static float nextInterval( float mean, float roll ){
		float e = (float)-Math.log( 1.0 - Math.min( roll, 0.9999f ) );
		return mean * Math.min( 4f, 0.35f + 0.65f * e );
	}

	/** A bed's level at this distance from its source: whole to a cell short of its reach, then
	 *  dying away smoothly to nothing where it stops being heard (heard), quieter with every step
	 *  the hero walks off. It used to go from half to nothing in two cells and seemed to cut off. */
	static float reach( float cells, float radius ){
		float full = radius - 1f, end = heard( radius );
		if (cells <= full) return 1f;
		if (cells >= end) return 0f;
		float t = (end - cells) / (end - full);
		return t * t * (3f - 2f * t);
	}

	/** How far a bed still hears its source: its reach and a fade past it, two cells at least and
	 *  three quarters of a wider reach (a torch's 4: to 7 cells). */
	static float heard( float radius ){
		return radius + Math.max( 2f, radius * 0.75f );
	}

	/**
	 * A random cell `near` to `far` cells from the hero, inside the map's outer ring; -1 when it
	 * falls outside. Spread evenly over the ring's area, or `nearer`: as often at every distance,
	 * which finds what is nearest sooner.
	 */
	static int probe( Random rng, Ground g, float near, float far, boolean nearer ){
		double a = rng.nextDouble() * 2.0 * Math.PI;
		double r = nearer ? near + rng.nextDouble() * (far - near)
				: Math.sqrt( near * near + rng.nextDouble() * (far * far - near * near) );
		int x = g.hero % g.w + (int)Math.round( r * Math.cos( a ) );
		int y = g.hero / g.w + (int)Math.round( r * Math.sin( a ) );
		return g.inner( x, y ) ? x + y * g.w : -1;
	}

	/**
	 * Is there rock between these two cells of a map w wide: on the straight line from one to the
	 * other, the cells at either end aside, anything that is solid and blocks the sight - a wall,
	 * a shut door, a tree, a bookshelf? Long grass, deep water and statues let a sound through.
	 */
	static boolean behindRock( int[] map, int w, int from, int to ){
		int x = from % w, y = from / w, tx = to % w, ty = to / w;
		int dx = Math.abs( tx - x ), dy = -Math.abs( ty - y );
		int sx = x < tx ? 1 : -1, sy = y < ty ? 1 : -1, err = dx + dy;
		while (x != tx || y != ty){
			int e2 = 2 * err;
			if (e2 >= dy){
				err += dy;
				x += sx;
			}
			if (e2 <= dx){
				err += dx;
				y += sy;
			}
			if (x == tx && y == ty) break;
			int f = Terrain.flags[map[x + y * w]];
			if ((f & Terrain.SOLID) != 0 && (f & Terrain.LOS_BLOCKING) != 0) return true;
		}
		return false;
	}

	/** The cell `cell` is once a window w x h has moved by (dx, dy) cells under it; -1 if left behind. */
	static int moved( int cell, int dx, int dy, int w, int h ){
		if (cell < 0) return -1;
		int x = cell % w - dx, y = cell / w - dy;
		return x < 0 || y < 0 || x >= w || y >= h ? -1 : x + y * w;
	}

	// ------------------------------------------------------------ the bookkeeping

	/** Does this voice hold one of the voices: a bed playing on, or a sound still ringing? */
	boolean holds( int i ){
		return on[i] || now < until[i];
	}

	/** Sounds from outside the place's voices ringing now (a critter's, DungeonCritterSprite): each takes a voice. */
	int others;

	/** How many voices sound now. */
	int sounding(){
		int n = others;
		for (int i = 0; i < voices.length; i++) if (holds( i )) n++;
		return n;
	}

	/** Do the place's own voices hold all of them? Then a critter's sound waits too. */
	boolean full(){
		return sounding() - others >= MAX_VOICES;
	}

	//the quiet before the first sound, every voice's first wait drawn from its end, and the first
	//sound to choose: on a new floor, and when the ambience is heard again after a silence
	private void start(){
		quietUntil = now + QUIET_MIN + rng.nextFloat() * (QUIET_MAX - QUIET_MIN);
		for (int i = 0; i < voices.length; i++){
			due[i] = quietUntil + nextInterval( voices[i].every, rng.nextFloat() );
			on[i] = false;
		}
		opening = OPENINGS;
		later.clear();
	}

	/**
	 * A frame of silence (the ambience off, the game in the background, the hero dead): the
	 * beds stop, what was to come is dropped, and once it is heard again it starts over after
	 * a quiet, as on a new floor. The clock still runs, so what rang before rings out.
	 */
	void hush( float dt ){
		now += dt;
		if (hushed) return;
		hushed = true;
		later.clear();
		Arrays.fill( on, false );
	}

	/** The overworld's window moved by (dx, dy) cells under the sources (OverworldLevel.rebase):
	 *  the cells kept go with it, those it left behind are dropped. */
	void shift( int dx, int dy, int w, int h ){
		for (int i = 0; i < bedAt.length; i++) bedAt[i] = moved( bedAt[i], dx, dy, w, h );
		for (int i = later.size() - 1; i >= 0; i--){
			Later l = later.get( i );
			if (l.cell < 0) continue;
			l.cell = moved( l.cell, dx, dy, w, h );
			if (l.cell < 0) later.remove( i );
		}
	}

	/** A frame of `dt` seconds: what is due is played on `out`, from `g` as it stands, in `a`. */
	void tick( float dt, Ground g, Air a, Out out ){
		if (hushed){
			hushed = false;
			start();
		}
		now += dt;
		for (int i = later.size() - 1; i >= 0; i--){
			Later l = later.get( i );
			if (l.at > now) continue;
			later.remove( i );
			out.play( l.v, l.take, l.cell, l.level * muffle( g, l.cell ), l.pitch, l.echo );
			//played on the first frame at or after its moment: its voice is held to its very end
			until[l.voice] = Math.max( until[l.voice], now + rings( l.v, l.pitch ) );
		}
		boolean waiting = false;
		for (int i = 0; i < voices.length; i++){
			if (voices[i].bed && bed( i, g, a, out )) waiting = true;
		}
		if (now < quietUntil || waiting) return;
		if (opening > 0) open( a );
		oneShots( g, a, out );
	}

	//a bed's frame: its nearest source kept track of, and played on while there is one in reach.
	//whether it is waiting for a voice to fall silent before it can start
	private boolean bed( int i, Ground g, Air a, Out out ){
		Voice v = voices[i];
		track( i, g );
		int c = bedAt[i];
		if (c < 0 || now < quietUntil || !v.allowed( a )){
			//what it last played rings out (until[i] holds its voice to the end)
			on[i] = false;
			return false;
		}
		if (!on[i]){
			if (!holds( i ) && sounding() >= MAX_VOICES) return true;
			on[i] = true;
			//on its beat if its last play still sounds: a source lost for a moment (the hero at the
			//edge of its reach, a hand-over from one source to the next) never starts a second
			//play over the first. A fresh bed's beat is long past
			bedNext[i] = Math.max( now, bedNext[i] );
		}
		if (now >= bedNext[i]){
			//this source's own sound: a pipe that pours rather than trickles
			Voice p = v.at( g, c );
			float pitch = pitch( p );
			float level = p.level * v.loudness( a ) * reach( g.away( c ), v.radius ) * muffle( g, c );
			out.play( p, take( p ), c, level, pitch, false );
			until[i] = Math.max( until[i], now + rings( p, pitch ) );
			bedNext[i] = now + (p.length - p.overlap) / pitch * (1f + (rng.nextFloat() * 2f - 1f) * BED_JITTER);
		}
		return false;
	}

	//the bed's nearest source it still hears (heard: its reach and the fade past it): the one it
	//had, while it still fits and is heard, or a nearer one some of the frame's random cells came upon
	private void track( int i, Ground g ){
		Voice v = voices[i];
		int c = bedAt[i];
		float best = Float.MAX_VALUE;
		float far = heard( v.radius );
		if (g.inner( c ) && v.fits( g, c ) && !g.hidden( Source.face( g.map, g.w, c ) )) best = g.away( c );
		if (best > far){
			c = -1;
			best = Float.MAX_VALUE;
		}
		for (int k = 0; k < BED_PROBES; k++){
			int p = probe( rng, g, 0f, far, true );
			if (p < 0 || p == c) continue;
			float d = g.away( p );
			if (d < best && d <= far && v.fits( g, p ) && !g.hidden( Source.face( g.map, g.w, p ) )){
				c = p;
				best = d;
			}
		}
		bedAt[i] = c;
	}

	//the floor's first sound as the quiet ends: a voice drawn by how often it sounds, due now. if
	//it finds no source, another is drawn the next frame, a few times
	private void open( Air a ){
		opening--;
		float total = 0f;
		for (Voice v : voices) if (!v.bed && v.allowed( a )) total += 1f / v.every;
		float r = rng.nextFloat() * total;
		for (int i = 0; i < voices.length && total > 0f; i++){
			Voice v = voices[i];
			if (v.bed || !v.allowed( a )) continue;
			r -= 1f / v.every;
			if (r <= 0f){
				due[i] = now;
				return;
			}
		}
	}

	//the frame's one-shots: at most one starts, the first due in line that may sound and finds
	//where to come from
	private void oneShots( Ground g, Air a, Out out ){
		boolean started = false;
		int n = voices.length;
		for (int k = 0; k < n; k++){
			int i = (turn + k) % n;
			Voice v = voices[i];
			if (v.bed || now < due[i]) continue;
			if (!v.allowed( a )){
				due[i] = now + wait( v, a );
				continue;
			}
			if (started || holds( i ) || sounding() >= MAX_VOICES){
				due[i] = now + RETRY_MIN + rng.nextFloat() * (RETRY_MAX - RETRY_MIN);
				continue;
			}
			due[i] = now + wait( v, a );
			int cell = find( v, g, null, 0 );
			if (cell < 0 && !v.airy()) continue;
			sound( i, cell, g, a, out );
			started = true;
			opening = 0;
		}
		turn = n == 0 ? 0 : (turn + 1) % n;
	}

	//a source cell for a sound of v: for each of its rules in turn, random cells between its near
	//and far (out of sight, if it keeps to the dark) until one fits; never one of `not`. -1 for none
	private int find( Voice v, Ground g, int[] not, int nots ){
		for (Source s : v.from){
			if (s == Source.AIR) break;
			for (int k = 0; k < PROBES; k++){
				int c = probe( rng, g, v.near, v.far, false );
				if (c < 0 || (v.unseen && g.seen( c )) || !s.test( g, c ) || g.hidden( Source.face( g.map, g.w, c ) )) continue;
				boolean taken = false;
				for (int j = 0; j < nots; j++) if (not[j] == c) taken = true;
				if (!taken) return c;
			}
		}
		return -1;
	}

	//a voice's sound, with its echo, the rest of its chorus or an answer to come
	private void sound( int i, int cell, Ground g, Air a, Out out ){
		Voice v = voices[i];
		int take = take( v );
		float pitch = pitch( v );
		float level = v.level * v.loudness( a );
		out.play( v, take, cell, level * muffle( g, cell ), pitch, false );
		float end = now + rings( v, pitch );
		if (v.echo && cell >= 0){
			//the very sound thrown back: its take, a touch lower
			float at = now + ECHO_MIN + rng.nextFloat() * (ECHO_MAX - ECHO_MIN);
			later.add( new Later( i, v, take, cell, at, level * ECHO_LEVEL, pitch * ECHO_PITCH, true ) );
			end = Math.max( end, at + rings( v, pitch * ECHO_PITCH ) );
		}
		if (v.chorus && cell >= 0 && rng.nextFloat() < CHORUS_CHANCE){
			//one or two more at other cells, staggered: a deeper voice, then a higher one
			int[] cells = { cell, -1, -1 };
			int n = 1;
			float at = now;
			for (int more = 1 + rng.nextInt( 2 ); more > 0; more--){
				int c = find( v, g, cells, n );
				if (c < 0) break;
				at += CHORUS_GAP_MIN + rng.nextFloat() * (CHORUS_GAP_MAX - CHORUS_GAP_MIN);
				float p = pitch * (n == 1 ? 0.8f + 0.1f * rng.nextFloat() : 1.1f + 0.12f * rng.nextFloat());
				later.add( new Later( i, v, take( v ), c, at, level * (0.7f + 0.3f * rng.nextFloat()), p, false ) );
				end = Math.max( end, at + rings( v, p ) );
				cells[n++] = c;
			}
		} else if (v.answer && cell >= 0 && rng.nextFloat() < ANSWER_CHANCE){
			int c = find( v, g, new int[]{ cell }, 1 );
			if (c >= 0){
				float at = now + ANSWER_MIN + rng.nextFloat() * (ANSWER_MAX - ANSWER_MIN);
				float p = pitch( v );
				later.add( new Later( i, v, take( v ), c, at, level * (0.6f + 0.4f * rng.nextFloat()), p, false ) );
				end = Math.max( end, at + rings( v, p ) );
			}
		}
		until[i] = end;
	}

	private int take( Voice v ){
		return rng.nextInt( v.takes.length );
	}

	//a pitch in the voice's range, nudged a little off as the player nudges every sound of its own
	private float pitch( Voice v ){
		return AmbientPlayer.nudge( rng, v.pitchLo + rng.nextFloat() * (v.pitchHi - v.pitchLo) );
	}

	private float wait( Voice v, Air a ){
		return nextInterval( v.every / v.rate( a ), rng.nextFloat() );
	}

	//how long a play at this pitch rings: its longest take
	private static float rings( Voice v, float pitch ){
		return v.length / pitch;
	}

	/**
	 * Heard through the rock between, or clear. A source in a wall is heard from its open face
	 * (Source.face): clear before it, along it or in sight, muffled from behind the wall it is in.
	 * Whatever the hero sees he hears clearly, whatever a straight line to it clips on the way.
	 */
	static float muffle( Ground g, int cell ){
		if (cell < 0) return 1f;
		int at = Source.face( g.map, g.w, cell );
		return g.seen( at ) || !behindRock( g.map, g.w, g.hero, at ) ? 1f : MUFFLED;
	}
}
