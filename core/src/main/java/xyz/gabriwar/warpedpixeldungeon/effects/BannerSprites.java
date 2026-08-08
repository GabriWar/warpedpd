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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.Image;

public class BannerSprites {

	public enum  Type {
		TITLE_PORT,
		TITLE_GLOW_PORT,
		TITLE_LAND,
		TITLE_GLOW_LAND,
		BOSS_SLAIN,
		GAME_OVER,
	}

	//pixel rect of each banner in the sheet, exposed so WobblyBanner can slice it
	public static int[] rect( Type type ) {
		switch (type) {
			case TITLE_PORT:        return new int[]{   0,   0, 139, 100 };
			case TITLE_GLOW_PORT:   return new int[]{ 139,   0, 278, 100 };
			case TITLE_LAND:        return new int[]{   0, 100, 240, 157 };
			case TITLE_GLOW_LAND:   return new int[]{ 240, 100, 480, 157 };
			case BOSS_SLAIN:        return new int[]{   0, 157, 127, 225 };
			case GAME_OVER: default:return new int[]{ 128, 157, 256, 192 };
		}
	}

	public static Image get( Type type ) {
		Image icon = new Image( Assets.Interfaces.BANNERS );
		int[] r = rect(type);
		icon.frame( icon.texture.uvRect( r[0], r[1], r[2], r[3] ) );
		return icon;
	}
}
