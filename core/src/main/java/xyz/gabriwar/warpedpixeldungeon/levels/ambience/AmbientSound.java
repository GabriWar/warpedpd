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
 */
public enum AmbientSound {

	//water
	DRIP       ( "drip",       3, 1.9f, 0.32f ),
	DRIP_METAL ( "drip_metal", 2, 2.4f, 0.26f ),
	TRICKLE    ( "trickle",    2, 4.1f, 0.30f ),
	POUR       ( "pour",       1, 4.2f, 0.30f ),
	GURGLE     ( "gurgle",     2, 2.3f, 0.28f ),
	SPLASH     ( "splash",     2, 1.7f, 0.32f ),
	LAP        ( "lap",        3, 1.7f, 0.28f ),
	//small life
	FROG       ( "frog",       2, 1.5f, 0.30f ),
	CRICKET    ( "cricket",    2, 1.8f, 0.20f ),
	BIRD       ( "bird",       2, 1.8f, 0.20f ),
	COO        ( "coo",        1, 2.3f, 0.22f ),
	BAT        ( "bat",        2, 2.1f, 0.20f ),
	RAT        ( "rat",        2, 1.7f, 0.22f ),
	FLY        ( "fly",        1, 2.2f, 0.22f ),
	SKITTER    ( "skitter",    2, 1.5f, 0.45f ),
	//the air and the rock
	PEBBLES    ( "pebbles",    2, 3.7f, 0.28f ),
	WIND       ( "wind",       2, 5.8f, 0.28f ),
	CRACKLE    ( "crackle",    1, 3.2f, 0.60f ),
	RUMBLE     ( "rumble",     1, 6.0f, 0.55f ),
	WHISPER    ( "whisper",    2, 4.6f, 0.16f ),
	MOAN       ( "moan",       2, 5.1f, 0.18f ),
	ANVIL      ( "anvil",      1, 3.9f, 0.20f ),
	CREAK      ( "creak",      2, 2.5f, 0.24f ),
	ICE        ( "ice",        2, 3.9f, 0.28f ),
	LAVA       ( "lava",       2, 4.3f, 0.32f ),
	CHIME      ( "chime",      1, 4.4f, 0.18f );

	/** the asset of every take */
	public final String[] takes;
	/** the longest take, in seconds: how long one play keeps a voice busy */
	public final float length;
	/** the mix: its level among the others, before distance and the ambience volume */
	public final float gain;

	AmbientSound( String name, int takes, float length, float gain ){
		this.takes = new String[takes];
		for (int k = 0; k < takes; k++){
			this.takes[k] = takes == 1 ? "sounds/amb_" + name + ".mp3" : "sounds/amb_" + name + "_" + (k + 1) + ".mp3";
		}
		this.length = length;
		this.gain = gain;
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
