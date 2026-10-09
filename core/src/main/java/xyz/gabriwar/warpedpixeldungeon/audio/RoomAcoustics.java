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

package xyz.gabriwar.warpedpixeldungeon.audio;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Room acoustics (Settings, Audio): the game's effects as the space round the hero gives them
 * back (Earshot). A blow in a hall blooms, one in a corridor rings short, a blast in a cavern or
 * under a cliff comes back a moment later, and one behind a door or the rock comes through
 * dull and quieter. Off, every effect plays exactly as it always has.
 *
 * Each effect of its table plays as spatial sound would play it, and beside it up to two more
 * plays: its space's tail (tail_room_, tail_hall_ and tail_cavern_, the reverb alone, from
 * tools/room_acoustics_gen.py) and, in a big rock space or under the open sky facing rock, a
 * late, quieter copy of itself. Through a door or rock it plays its muffled copy (muffled_)
 * instead, quieter, and from the door it is heard through. Every play goes through
 * SpatialSound.playPanned, as a placed sound's does. No more than three of these extras sound at
 * once, footsteps share one, and a busy moment gets none: the device's voices are the effects'.
 *
 * Callable from any thread, as SpatialSound is: Earshot's lock first, then its own, and the
 * device only after both.
 */
public final class RoomAcoustics {

	private RoomAcoustics(){}

	/** Is it on? enable() sets it, at boot and from its switch. */
	public static volatile boolean on = false;

	//under this volume a play is a quiet one: one tail between it and the footsteps, never an echo
	private static final float QUIET = 0.5f;
	//at most this many extras at once
	private static final int MAX_EXTRAS = 3;
	//an echo comes back only for a sound this near the hero (cells)
	private static final int ECHO_CELLS = 8;
	//the muffled copies are used once the last of them has been loaded this long (s): the device
	//may still be decoding it, and a copy played before then is silence
	private static final double READY_AFTER = 10.0;

	//what an effect of the table gets: a tail of each size, a muffled copy (null for none), an echo,
	//and whether it is a footstep (one tail between them all, never an echo)
	private static final class Effect {
		final String dry, room, hall, cavern, muffled;
		final boolean echo, step;
		//how long its room tail, its hall tail, its cavern tail and itself (its echo) keep a voice, s
		//at pitch 1
		final double roomLength, hallLength, cavernLength, dryLength;
		//when it last had a tail and an echo (clock s), under BUDGET
		double lastTail, lastEcho;

		Effect( String dry, boolean tail, boolean muffled, boolean echo, boolean step ){
			String name = dry.substring( "sounds/".length(), dry.length() - ".mp3".length() );
			this.dry = dry;
			this.room = tail ? "sounds/tail_room_" + name + ".mp3" : null;
			this.hall = tail ? "sounds/tail_hall_" + name + ".mp3" : null;
			this.cavern = tail ? "sounds/tail_cavern_" + name + ".mp3" : null;
			this.muffled = muffled ? "sounds/muffled_" + name + ".mp3" : null;
			this.echo = echo;
			this.step = step;
			float[] lengths = tail || echo ? RoomAcousticsLengths.of( name ) : null;
			roomLength = tail ? lengths[0] : 0;
			hallLength = tail ? lengths[1] : 0;
			cavernLength = tail ? lengths[2] : 0;
			dryLength = echo ? lengths[3] : 0;
		}

		//its tail for a space's, and how long that keeps a voice (s at pitch 1)
		String tail( Earshot.Tail t ){
			return t == Earshot.Tail.ROOM ? room : t == Earshot.Tail.HALL ? hall : cavern;
		}

		double tailLength( Earshot.Tail t ){
			return t == Earshot.Tail.ROOM ? roomLength : t == Earshot.Tail.HALL ? hallLength : cavernLength;
		}
	}

	private static final LinkedHashMap<Object, Effect> TABLE = new LinkedHashMap<>();
	private static void add( boolean tail, boolean muffled, boolean echo, boolean step, String... sounds ){
		for (String s : sounds) TABLE.put( s, new Effect( s, tail, muffled, echo, step ) );
	}
	static {
		add( true, true, true, false, Assets.Sounds.HIT, Assets.Sounds.HIT_SLASH, Assets.Sounds.HIT_STAB,
				Assets.Sounds.HIT_CRUSH, Assets.Sounds.HIT_STRONG, Assets.Sounds.HIT_PARRY, Assets.Sounds.HIT_ARROW,
				Assets.Sounds.HIT_MAGIC, Assets.Sounds.ATK_CROSSBOW, Assets.Sounds.ATK_SPIRITBOW, Assets.Sounds.OPEN,
				Assets.Sounds.UNLOCK, Assets.Sounds.SHATTER, Assets.Sounds.LIGHTNING, Assets.Sounds.TRAP,
				Assets.Sounds.ROCKS_LIGHT, Assets.Sounds.BLAST, Assets.Sounds.MINE, Assets.Sounds.WALL_BREAK_1,
				Assets.Sounds.WALL_BREAK_2, Assets.Sounds.WALL_BREAK_3 );
		add( true, true, false, false, Assets.Sounds.MISS, Assets.Sounds.ZAP, Assets.Sounds.ROCKS );
		add( true, false, true, false, Assets.Sounds.CHAINS );
		add( true, false, false, true, Assets.Sounds.STEP, Assets.Sounds.STURDY, Assets.Sounds.GRASS,
				Assets.Sounds.TRAMPLE, Assets.Sounds.WATER );
		add( true, false, false, false, Assets.Sounds.BONES, Assets.Sounds.PUFF );
		//the alarm plays unseen, through the walls, and rings on by itself
		add( false, true, false, false, Assets.Sounds.ALERT );
	}

	//the switch's taste of it: a heavy blow in a big hall
	private static final String PREVIEW_TAIL = "sounds/tail_hall_hit_strong.mp3";

	//every file, the preview's tail first (it is heard half a second after the switch), the
	//muffled copies last (the last of them loaded tells they all are)
	private static final String[] ASSETS;
	static {
		ArrayList<String> files = new ArrayList<>();
		files.add( PREVIEW_TAIL );
		for (Effect e : TABLE.values()) if (e.room != null) files.add( e.room );
		for (Effect e : TABLE.values()) if (e.hall != null && !e.hall.equals( PREVIEW_TAIL )) files.add( e.hall );
		for (Effect e : TABLE.values()) if (e.cavern != null) files.add( e.cavern );
		for (Effect e : TABLE.values()) if (e.muffled != null) files.add( e.muffled );
		ASSETS = files.toArray( new String[0] );
	}
	private static final String LAST = ASSETS[ASSETS.length - 1];

	/** Every file it plays besides the effects themselves. */
	public static String[] assets(){
		return ASSETS.clone();
	}

	/** The effects it plays more of: every other sound plays as spatial sound alone plays it. */
	static Set<String> sounds(){
		Set<String> s = new LinkedHashSet<>();
		for (Effect e : TABLE.values()) s.add( e.dry );
		return Collections.unmodifiableSet( s );
	}

	/** Seconds, for the budget and the copies' readiness: a test may set its own. */
	static Earshot.Clock clock = () -> System.nanoTime() / 1e9;

	/** Has a file been loaded? The device's own answer: a test may set its own. */
	interface Loaded {
		boolean loaded( String asset );
	}

	static Loaded loaded = asset -> Sample.INSTANCE.isLoaded( asset );

	/** Turns it on or off (WPDSettings, at boot and from the switch): on, its files are queued to
	 *  load, the preview's tail first; off, they are let go, and the effects sound as they always have. */
	public static void enable( boolean value ){
		on = value;
		reset();
		if (value){
			Sample.INSTANCE.load( ASSETS );
		} else {
			for (String a : ASSETS) Sample.INSTANCE.unload( a );
		}
	}

	/** The switch's taste of it, half a second on: a heavy blow, centred, with a big hall's tail. */
	public static void preview(){
		SpatialSound.playPanned( Assets.Sounds.HIT_STRONG, 0.5f, 1f, 1f, 0f );
		SpatialSound.playPanned( PREVIEW_TAIL, 0.5f, gain( -8f ), 1f, 0f );
	}

	//a tail's volume for one L dB under its dry sound: the files are stored 6 dB under theirs
	private static float gain( float db ){
		return (float) Math.pow( 10, (db + 6f) / 20f );
	}

	// ------------------------------------------------------------------ budget

	private static final Object BUDGET = new Object();
	//the extras playing or due, from and to (clock s); a slot is free once its extra is over
	private static final double[] extraFrom = new double[8], extraTo = new double[8];
	//when the latest effects of the table that are not footsteps were played
	private static final double[] busy = new double[8];
	private static int busyAt;
	//when the footsteps' and the quiet plays' tail last sounded
	private static double quietTail;
	private static int tails, echoes, muffled, skipped;

	//when the last muffled copy was first seen loaded (clock s), NaN while it is not
	private static volatile double readySince = Double.NaN;

	/** Forgets the budget, the counts and the copies' readiness (enable, and the tests). */
	static void reset(){
		synchronized (BUDGET){
			Arrays.fill( extraFrom, Double.NEGATIVE_INFINITY );
			Arrays.fill( extraTo, Double.NEGATIVE_INFINITY );
			Arrays.fill( busy, Double.NEGATIVE_INFINITY );
			busyAt = 0;
			quietTail = Double.NEGATIVE_INFINITY;
			for (Effect e : TABLE.values()) e.lastTail = e.lastEcho = Double.NEGATIVE_INFINITY;
			tails = echoes = muffled = skipped = 0;
		}
		readySince = Double.NaN;
	}

	static {
		reset();
	}

	//how many effects (footsteps aside) started after `since`, under BUDGET
	private static int busySince( double since ){
		int n = 0;
		for (double t : busy) if (t > since) n++;
		return n;
	}

	//how many extras sound at some time between from and to, under BUDGET
	private static int extrasBetween( double from, double to ){
		int n = 0;
		for (int i = 0; i < extraTo.length; i++) if (extraTo[i] > from && extraFrom[i] < to) n++;
		return n;
	}

	//how soon the footsteps' and the quiet plays' one tail comes again in a space (s): a footstep's
	//tail is under twice that long in each (0.73, 1.75 and 3.06 s at most), so no more than two of
	//theirs sound at once, and a blow among them still finds a voice
	private static double quietGap( Earshot.Tail t ){
		return t == Earshot.Tail.ROOM ? 0.4 : t == Earshot.Tail.HALL ? 1.0 : 1.6;
	}

	//books a voice for an extra from `from` to `to` if fewer than MAX_EXTRAS sound then, under BUDGET
	private static boolean book( double from, double to, double now ){
		if (extrasBetween( from, to ) >= MAX_EXTRAS) return false;
		for (int i = 0; i < extraTo.length; i++){
			if (extraTo[i] <= now){
				extraFrom[i] = from;
				extraTo[i] = to;
				return true;
			}
		}
		return false;
	}

	/** Are the muffled copies in use yet: 10 s after the last of them loaded? Asking starts that wait
	 *  once they are all in, as every effect of its table does. */
	public static boolean muffledReady(){
		return muffledReady( clock.now() );
	}

	private static boolean muffledReady( double now ){
		double since = readySince;
		if (Double.isNaN( since )){
			if (!loaded.loaded( LAST )) return false;
			readySince = since = now;
		}
		return now - since >= READY_AFTER;
	}

	/** How many tails and echoes it has played since it was last switched on. */
	public static int scheduled(){
		synchronized (BUDGET){
			return tails + echoes;
		}
	}

	/** How many tails and echoes the budget has held back since it was last switched on. */
	public static int skipped(){
		synchronized (BUDGET){
			return skipped;
		}
	}

	/** One line for a log: on or off, and what it has played and held back. */
	public static String describe(){
		boolean ready = muffledReady( clock.now() );
		synchronized (BUDGET){
			return String.format( Locale.ENGLISH, "room acoustics %s, muffled copies %s: %d tails, %d echoes, %d muffled, %d held back",
					on ? "on" : "off", ready ? "ready" : "not ready yet", tails, echoes, muffled, skipped );
		}
	}

	// -------------------------------------------------------------------- play

	/**
	 * Plays an effect from a cell as the hero standing on `hero` hears it there (SpatialSound's
	 * hook): `lvl` and `pan` are what spatial sound gives it, `volume` what the game asked for.
	 * False, having played nothing, for an effect not in its table or a space it cannot tell: the
	 * caller then plays it as it always has.
	 */
	public static boolean play( Object id, float delay, int cell, float volume, float lvl, float pitch, float pan,
	                            Level level, int hero ){
		Effect e = TABLE.get( id );
		if (e == null) return false;
		double now = clock.now();
		//asked at every effect, so the copies' wait starts once they are all in, not at the first
		//sound heard through a wall
		boolean ready = muffledReady( now );
		Earshot.Snapshot s = Earshot.of( level, hero );
		if (s == null) return false;
		Earshot.Relation heard = s.relation( cell );
		boolean quiet = e.step || volume < QUIET;
		int w = level.width();

		//the sound, or what comes of it through a door or the rock, from the door it comes through
		String dry = e.dry;
		float dryVolume = volume * lvl, dryPan = pan;
		if (heard != Earshot.Relation.CLEAR){
			boolean door = heard == Earshot.Relation.DOOR;
			dryVolume *= door ? (s.openAir ? 0.89f : 0.8f) : (s.openAir ? 0.77f : 0.6f);
			//a copy that never loaded (its file failed, or the switch went off meanwhile) would be silence
			if (e.muffled != null && ready && loaded.loaded( e.muffled )) dry = e.muffled;
			int through = door ? s.door( cell ) : -1;
			if (through >= 0) dryPan = SpatialSound.pan( through % w - hero % w );
		}

		//its tail: the hero's space's, never through the rock, wetter than the sound further off
		String tail = null;
		float tailVolume = 0f;
		double tailLength = 0;
		if (heard != Earshot.Relation.WALL && s.tail != Earshot.Tail.NONE){
			tail = e.tail( s.tail );
			tailVolume = Math.min( 1.12f * dryVolume, volume * gain( s.level ) * (0.55f + 0.45f * lvl)
					* (heard == Earshot.Relation.DOOR ? 0.7f : 1f) );
			tailLength = e.tailLength( s.tail ) / pitch;
		}

		//an echo: a loud sound near the hero, heard clear, where something far gives it back
		boolean wantEcho = e.echo && !quiet && heard == Earshot.Relation.CLEAR && s.echoDelay > 0f
				&& Math.max( Math.abs( cell % w - hero % w ), Math.abs( cell / w - hero / w ) ) <= ECHO_CELLS;

		boolean withTail = false, withEcho = false;
		float echoDelay = 0f;
		synchronized (BUDGET){
			int busyHalf = busySince( now - 0.5 ), busyQuarter = busySince( now - 0.25 );
			if (!e.step){
				busy[busyAt] = now;
				busyAt = (busyAt + 1) % busy.length;
			}
			if (tail != null){
				boolean room = s.tail == Earshot.Tail.ROOM;
				double from = now + delay;
				withTail = busyQuarter + (e.step ? 0 : 1) < 4
						&& now - e.lastTail >= (room ? 0.15 : 0.4)
						&& (!quiet || now - quietTail >= quietGap( s.tail ))
						&& book( from, from + tailLength, now );
				if (withTail){
					e.lastTail = now;
					if (quiet) quietTail = now;
					tails++;
				} else {
					skipped++;
				}
			}
			if (wantEcho){
				echoDelay = (float) (s.echoDelay * (1 + ThreadLocalRandom.current().nextDouble( -0.06, 0.06 )));
				double from = now + delay + echoDelay;
				withEcho = busyHalf < 3 && now - e.lastEcho >= 0.35 && book( from, from + e.dryLength / pitch, now );
				if (withEcho){
					e.lastEcho = now;
					echoes++;
				} else {
					skipped++;
				}
			}
			if (!dry.equals( e.dry )) muffled++;
		}

		SpatialSound.playPanned( dry, delay, dryVolume, pitch, dryPan );
		if (withTail) SpatialSound.playPanned( tail, delay, tailVolume, pitch, -0.3f * dryPan );
		if (withEcho) SpatialSound.playPanned( e.dry, delay + echoDelay, volume * lvl * s.echoLevel, pitch,
				SpatialSound.on ? s.echoPan : 0f );
		return true;
	}
}
