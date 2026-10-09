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

import java.util.ArrayList;

/**
 * The ambience channel's sounds: sounds/amb_*.mp3, synthesised by tools/ambience_sound_gen.py
 * (a falling drop is a closing bubble, a frog a pulse train through two formants, a cricket a
 * gated tone...). A sound comes in a few takes where one would grow stale; the player picks one
 * at random and plays it a little off pitch. The files are levelled by loudness, so {@link #gain}
 * is the mix: how loud each sits among the others before distance and the ambience volume.
 *
 * Every file is mono, 44.1 kHz and under seven seconds: Android's SoundPool decodes a whole
 * sound into memory and refuses much past a megabyte of it.
 *
 * The weather's sounds (WeatherSounds, docs/weather-sounds.md) are the last group. Its beds carry
 * their own {@link #lead}: they are laid by WeatherSounds, which has no Voice to hold it.
 */
public enum AmbientSound {

	//water
	DRIP       ( "drip",       3, 1.9f, 0.32f ),
	DRIP_METAL ( "drip_metal", 2, 1.3f, 0.16f ),
	TRICKLE    ( "trickle",    2, 4.1f, 0.05f ),
	POUR       ( "pour",       1, 4.2f, 0.30f ),
	GURGLE     ( "gurgle",     2, 2.3f, 0.28f ),
	SPLASH     ( "splash",     2, 1.7f, 0.32f ),
	LAP        ( "lap",        3, 1.7f, 0.28f ),
	//small life
	FROG       ( "frog",       2, 1.5f, 0.34f ),
	CRICKET    ( "cricket",    2, 1.8f, 0.20f ),
	BIRD       ( "bird",       2, 1.8f, 0.20f ),
	COO        ( "coo",        1, 2.3f, 0.35f ),
	BAT        ( "bat",        2, 2.1f, 0.23f ),
	RAT        ( "rat",        2, 1.7f, 0.23f ),
	FLY        ( "fly",        1, 2.2f, 0.28f ),
	SKITTER    ( "skitter",    2, 1.5f, 1.00f ),
	//the air and the rock
	PEBBLES    ( "pebbles",    2, 3.7f, 0.28f ),
	WIND       ( "wind",       2, 5.8f, 0.28f ),
	CRACKLE    ( "crackle",    1, 3.2f, 0.088f ),
	RUMBLE     ( "rumble",     1, 6.0f, 0.55f ),
	WHISPER    ( "whisper",    2, 4.6f, 0.16f ),
	MOAN       ( "moan",       2, 5.1f, 0.18f ),
	ANVIL      ( "anvil",      1, 3.9f, 0.20f ),
	CREAK      ( "creak",      2, 2.7f, 0.14f ),
	ICE        ( "ice",        2, 3.9f, 0.28f ),
	LAVA       ( "lava",       2, 4.3f, 0.32f ),
	CHIME      ( "chime",      1, 4.4f, 0.18f ),
	//the weather: beds (a lead) and one-shots, every bed's takes as long as each other
	RAIN_LIGHT   ( "rain_light",   3, 5.62f, 0.08f, 1.42f ),
	RAIN         ( "rain",         2, 5.62f, 0.10f, 1.42f ),
	RAIN_HEAVY   ( "rain_heavy",   2, 5.67f, 0.14f, 1.47f ),
	RAIN_WATER   ( "rain_water",   2, 5.67f, 0.06f, 1.47f ),
	RAIN_LEAVES  ( "rain_leaves",  2, 5.59f, 0.06f, 1.39f ),
	RAIN_ROOF    ( "rain_roof",    2, 5.77f, 0.09f, 1.57f ),
	THUNDER_FAR  ( "thunder_far",  2, 6.48f, 0.17f ),
	THUNDER_NEAR ( "thunder_near", 2, 6.48f, 0.5f ),
	GALE         ( "gale",         2, 5.88f, 0.10f, 1.88f ),
	GUST         ( "gust",         2, 5.88f, 0.27f ),
	HAIL         ( "hail",         1, 5.67f, 0.08f, 1.47f ),
	SLEET        ( "sleet",        1, 5.62f, 0.09f, 1.42f ),
	SNOW         ( "snow",         1, 5.77f, 0.03f, 1.37f ),
	BLIZZARD     ( "blizzard",     2, 5.77f, 0.15f, 1.77f ),
	SANDSTORM    ( "sandstorm",    1, 5.67f, 0.11f, 1.67f ),
	DRIP_LEAF    ( "drip_leaf",    2, 0.68f, 0.3f );

	/** the asset of every take */
	public final String[] takes;
	/** the longest take, in seconds: how long one play keeps a voice busy */
	public final float length;
	/** the mix: its level among the others, before distance and the ambience volume */
	public final float gain;
	/** a weather bed's lead, in seconds: its next play starts this long before the last one's end
	 *  (length - lead after the last one's start, over its pitch), just where the last one's
	 *  equal-power fade-out begins, so the two cross at a steady power. lead = length - (dry -
	 *  fade) of its recipe. 0 for every other sound: the place beds keep theirs on their Voice */
	public final float lead;

	AmbientSound( String name, int takes, float length, float gain ){
		this( name, takes, length, gain, 0f );
	}

	AmbientSound( String name, int takes, float length, float gain, float lead ){
		this.takes = new String[takes];
		for (int k = 0; k < takes; k++){
			this.takes[k] = takes == 1 ? "sounds/amb_" + name + ".mp3" : "sounds/amb_" + name + "_" + (k + 1) + ".mp3";
		}
		this.length = length;
		this.gain = gain;
		this.lead = lead;
	}

	/** Every ambience file, for the loader (WarpedPixelDungeon.create). */
	public static String[] assets(){
		ArrayList<String> all = new ArrayList<>();
		for (AmbientSound s : values()){
			for (String t : s.takes) all.add( t );
		}
		return all.toArray( new String[0] );
	}
}
