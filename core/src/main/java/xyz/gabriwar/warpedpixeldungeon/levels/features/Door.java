/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.levels.features;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;

public class Door {

	public static void enter( int pos ) {
		Level.set( pos, Terrain.OPEN_DOOR );
		GameScene.updateMap( pos );

		if (Dungeon.level.heroFOV[pos]) {
			Dungeon.observe();
			Sample.INSTANCE.play( Assets.Sounds.OPEN );
			CellEmitter.get( pos ).burst( EarthParticle.SMALL, 1 );
		}
	}

	public static void leave( int pos ) {
		int chars = 0;

		for (Char ch : Actor.chars()){
			if (ch.pos == pos) chars++;
		}

		//door does not shut if anything else is also on it
		if (Dungeon.level.heaps.get( pos ) == null && chars <= 1) {
			Level.set( pos, Terrain.DOOR );
			GameScene.updateMap( pos );
			if (Dungeon.level.heroFOV[pos]) {
				Dungeon.observe();
				CellEmitter.get( pos ).burst( EarthParticle.SMALL, 1 );
			}
		}
	}
}
