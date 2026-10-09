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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A wall breaking (Assets.Sounds.WALL_BREAK_1..3, tools/effect_sound_gen.py): the strike, the crack
 * running through, the pieces landing, the grit settling. Three takes and never the same one twice
 * running, so a run of mined walls never sounds like one sound over and over; each from where its
 * wall stood, through SpatialSound. The take is drawn from its own random: the dungeon's seeded
 * Random is never spent on a sound.
 *
 * The files are as loud as ROCKS at full volume: the pick plays them at PICK, as loud as MINE was.
 */
public final class WallBreak {

	private WallBreak(){}

	static final String[] TAKES = { Assets.Sounds.WALL_BREAK_1, Assets.Sounds.WALL_BREAK_2, Assets.Sounds.WALL_BREAK_3 };

	/** A wall mined with the pick: as loud as MINE, which played there before. */
	public static final float PICK = 0.8f;

	private static int last = -1;

	/** The next take: any but the one played last. */
	static synchronized String take(){
		int k = ThreadLocalRandom.current().nextInt( last < 0 ? TAKES.length : TAKES.length - 1 );
		if (last >= 0 && k >= last) k++;
		last = k;
		return TAKES[k];
	}

	/** A wall breaking at a cell. */
	public static void play( int cell, float volume, float pitch ){
		SpatialSound.play( take(), cell, volume, pitch );
	}

	/** A wall breaking at a cell, `delay` seconds from now. */
	public static void playDelayed( float delay, int cell, float volume, float pitch ){
		SpatialSound.playDelayed( take(), delay, cell, volume, pitch );
	}

	/**
	 * Walls broken together (a blast, a boss smashing through): one take from the one nearest the
	 * hero and, for three or more, a second from the one furthest from it, a moment later and
	 * quieter. Never one a wall: the same sound at the same instant from every wall summed into one
	 * far too loud, a voice each (a bomb by eight walls played sixteen sounds at once).
	 */
	public static void playAll( List<Integer> cells, float volume, float pitch ){
		if (cells.isEmpty()) return;
		int near = -1;
		for (int c : cells) near = SpatialSound.nearer( near, c );
		play( near, volume, pitch );
		Level l = Dungeon.level;
		if (cells.size() < 3 || l == null) return;
		int far = near;
		for (int c : cells) if (l.trueDistance( near, c ) > l.trueDistance( near, far )) far = c;
		playDelayed( 0.07f, far, volume * 0.75f, pitch );
	}
}
