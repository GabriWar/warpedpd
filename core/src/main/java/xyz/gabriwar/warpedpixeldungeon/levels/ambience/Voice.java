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

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome;

/**
 * One of a place's voices (AmbientSounds.voices): what it sounds, how often - a mean wait, the
 * waits drawn about it like the gaps between drops of rain, never a metronome - how loud and how
 * high, the hours, seasons and weather it keeps, and where it comes from: a source cell near the
 * hero to its rules (Source), tried in order, or the air. A bed instead sounds on and on while
 * its source is within reach, each play started a little before the last one ends.
 *
 * Set up once a floor, by the place's table, and only read after.
 */
final class Voice {

	/** What the weather does to a voice. */
	enum Weather {
		/** nothing: underground, or a sound the sky does not touch */
		ANY,
		/** hushed by a storm, and by rain or snow falling (birds, crickets) */
		CALM,
		/** heard only while the wind blows (WIND_BLOWS or more), louder and oftener the harder */
		WINDY,
		/** always there, louder and oftener the harder the wind blows (the peaks) */
		GUSTY
	}

	//the phases and the seasons as bits: a voice keeps those whose bit is set
	static final int ALL_HOURS = 0b1111, ALL_SEASONS = 0b1111;
	//rain or snow falling at more than this (ClimateManager.localPrecipRate) counts as falling
	static final float WET = 0.01f;
	//the wind, m/s, that is heard at all on open ground; and the gale it is loudest from
	static final float WIND_BLOWS = 4f, GALE = 17f;

	/** the sound's takes, its mix and its longest take in seconds */
	final String[] takes;
	final float gain, length;
	/** the AmbientSound it plays, or null for one of the game's own effects (takes) */
	final AmbientSound sound;

	/** mean seconds between its sounds (none for a bed) */
	float every;
	float level = 1f;
	float pitchLo = 1f, pitchHi = 1f;
	int hours = ALL_HOURS, seasons = ALL_SEASONS;
	/** the surface's biomes (at the hero) it keeps, as bits; 0 for anywhere */
	int biomes;
	/** biomes where it keeps no hours at all (the swamp's frogs, day and night) */
	int anyHourIn;
	/** rain falling lets it out at any hour (frogs) */
	boolean rainLifts;
	/** the coldest air (°C, ClimateManager.localTemp) it is heard in */
	float minTemp = -Float.MAX_VALUE;
	Weather weather = Weather.ANY;
	/** above zero: heard only on the main dungeon's floors 1 to this */
	int upTo;

	/** where it comes from, tried in order: Source.AIR (last, or alone) is the air, centred */
	Source[] from = { Source.AIR };
	/** how near and how far from the hero (cells) its source cell may be */
	float near = 1f, far = 12f;
	/** only from a cell out of the hero's sight: the far-off things */
	boolean unseen;

	/** a bed: sounding on while its nearest source is within `radius` cells, each play started
	 *  `overlap` seconds before the last one ends */
	boolean bed;
	float radius, overlap;
	/** a bed's other sound, played instead from those of its sources that fit the other's own
	 *  rules (a pipe that pours rather than trickles): one bed, one voice, whichever source it is near */
	Voice alt;

	/** the rock throws it back: an echo a moment later */
	boolean echo;
	/** now and then one or two more join in, at other cells (a frogs' chorus) */
	boolean chorus;
	/** now and then another answers, at another cell (a cricket, a second lap of water) */
	boolean answer;

	Voice( AmbientSound sound ){
		this.sound = sound;
		takes = sound.takes;
		gain = sound.gain;
		length = sound.length;
	}

	/** One of the game's own effects, heard as part of a place: its file, mix and length. */
	Voice( String asset, float gain, float length ){
		sound = null;
		takes = new String[]{ asset };
		this.gain = gain;
		this.length = length;
	}

	private Voice( Voice o ){
		sound = o.sound;
		takes = o.takes;
		gain = o.gain;
		length = o.length;
		every = o.every;
		level = o.level;
		pitchLo = o.pitchLo;
		pitchHi = o.pitchHi;
		hours = o.hours;
		seasons = o.seasons;
		biomes = o.biomes;
		anyHourIn = o.anyHourIn;
		rainLifts = o.rainLifts;
		minTemp = o.minTemp;
		weather = o.weather;
		upTo = o.upTo;
		from = o.from;
		near = o.near;
		far = o.far;
		unseen = o.unseen;
		bed = o.bed;
		radius = o.radius;
		overlap = o.overlap;
		alt = o.alt;
		echo = o.echo;
		chorus = o.chorus;
		answer = o.answer;
	}

	// ------------------------------------------------------------ the tables' words

	Voice every( float seconds ){
		every = seconds;
		return this;
	}

	Voice level( float l ){
		level = l;
		return this;
	}

	Voice pitch( float lo, float hi ){
		pitchLo = lo;
		pitchHi = hi;
		return this;
	}

	Voice hours( Phase... phases ){
		hours = 0;
		for (Phase p : phases) hours |= 1 << p.ordinal();
		return this;
	}

	Voice seasons( Season... ss ){
		seasons = 0;
		for (Season s : ss) seasons |= 1 << s.ordinal();
		return this;
	}

	Voice biomes( Biome... bs ){
		biomes = bits( bs );
		return this;
	}

	Voice anyHourIn( Biome... bs ){
		anyHourIn = bits( bs );
		return this;
	}

	Voice rainLifts(){
		rainLifts = true;
		return this;
	}

	Voice minTemp( float celsius ){
		minTemp = celsius;
		return this;
	}

	Voice weather( Weather w ){
		weather = w;
		return this;
	}

	Voice upTo( int depth ){
		upTo = depth;
		return this;
	}

	Voice from( Source... sources ){
		from = sources;
		return this;
	}

	Voice range( float nearest, float farthest ){
		near = nearest;
		far = farthest;
		return this;
	}

	Voice unseen(){
		unseen = true;
		return this;
	}

	Voice bed( float reach, float lead ){
		bed = true;
		radius = reach;
		overlap = lead;
		return this;
	}

	/** A bed's other sound, with its own lead and pitch, from those of its sources that fit its own rules. */
	Voice or( Voice other ){
		alt = other;
		return this;
	}

	Voice echo(){
		echo = true;
		return this;
	}

	Voice chorus(){
		chorus = true;
		return this;
	}

	Voice answer(){
		answer = true;
		return this;
	}

	/** This voice at `slower` times its wait and `quieter` times its level (the vault's hush). */
	Voice hushed( float slower, float quieter ){
		Voice v = new Voice( this );
		v.every = every * slower;
		v.level = level * quieter;
		if (alt != null) v.alt = alt.hushed( slower, quieter );
		return v;
	}

	private static int bits( Biome... bs ){
		int b = 0;
		for (Biome x : bs) b |= 1 << x.ordinal();
		return b;
	}

	// ------------------------------------------------------------ the rules

	/** Does it come from the air when no source cell is found (the air last among its sources)? */
	boolean airy(){
		return from[from.length - 1] == Source.AIR;
	}

	/** Is this cell one of its sources? */
	boolean fits( Soundscape.Ground g, int cell ){
		for (Source s : from){
			if (s != Source.AIR && s.test( g, cell )) return true;
		}
		return false;
	}

	/** What a bed plays from this source of its: its other sound where that one's rules fit, else its own. */
	Voice at( Soundscape.Ground g, int cell ){
		return alt != null && alt.fits( g, cell ) ? alt : this;
	}

	/** May it sound now: its floor, its biome, its season and hour, warm enough, and its weather. */
	boolean allowed( Soundscape.Air a ){
		if (upTo > 0 && (a.branch != 0 || a.depth < 1 || a.depth > upTo)) return false;
		if (biomes != 0 && (a.biome == null || (biomes & (1 << a.biome.ordinal())) == 0)) return false;
		if ((seasons & (1 << a.season.ordinal())) == 0) return false;
		boolean hour = (hours & (1 << a.phase.ordinal())) != 0
				|| (rainLifts && a.rain > WET)
				|| (a.biome != null && (anyHourIn & (1 << a.biome.ordinal())) != 0);
		if (!hour || a.temp < minTemp) return false;
		switch (weather){
			case CALM:  return !a.storm && a.rain <= WET;
			case WINDY: return a.wind >= WIND_BLOWS;
			default:    return true;
		}
	}

	/** How much louder the weather makes it: the harder the wind, the louder the wind. */
	float loudness( Soundscape.Air a ){
		switch (weather){
			case WINDY: return windLevel( a.wind );
			case GUSTY: return Math.max( 0.5f, windLevel( a.wind ) );
			default:    return 1f;
		}
	}

	/** How much oftener the weather makes it. */
	float rate( Soundscape.Air a ){
		return weather == Weather.WINDY || weather == Weather.GUSTY ? windRate( a.wind ) : 1f;
	}

	/** The wind's level at this speed (m/s): a breeze at WIND_BLOWS, full from a GALE up. */
	static float windLevel( float speed ){
		return 0.35f + 0.65f * gust( speed );
	}

	/** How much oftener the wind is heard at this speed: as often as its mean in a breeze,
	 *  three times as often in a gale. */
	static float windRate( float speed ){
		return 1f + 2f * gust( speed );
	}

	//0 at a breeze, 1 from a gale
	private static float gust( float speed ){
		return Math.max( 0f, Math.min( 1f, (speed - WIND_BLOWS) / (GALE - WIND_BLOWS) ) );
	}
}
