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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

//elixir of bloom: grass springs up where the hero walks, and trampled grass drops double seeds
public class BloomBuff extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	public static final float DURATION = 100f;

	public void bloom( int cell ) {
		int t = Dungeon.level.map[cell];
		if (t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.EMBERS) {
			Level.set( cell, Terrain.GRASS );
			GameScene.updateMap( cell );
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.HERB_HEALING;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight( 0.4f, 1f, 0.3f );
	}

	@Override
	public float iconFadePercent() {
		return Math.max( 0, (DURATION - visualcooldown()) / DURATION );
	}
}
