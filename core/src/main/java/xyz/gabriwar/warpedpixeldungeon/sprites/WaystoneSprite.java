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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Travel;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.TravelShare;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Waystone;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

/**
 * A waystone (homestead/travel/Waystone): its slate with a dormant rune, and once attuned the rune
 * alight in cyan, its glow pulsing (WAYSTONE_ATTUNED, two frames at 2 fps) with a soft light. On a
 * co-op guest's screen the stone is a mirror that knows only where it stands: the host's waystones,
 * shipped to the guest (TravelShare), say whether the one on its world cell is attuned.
 */
public class WaystoneSprite extends GlowStoneSprite {

	public WaystoneSprite(){
		super( HomesteadSprites.WAYSTONE, HomesteadSprites.WAYSTONE_ATTUNED, HomesteadSprites.GLOW_FRAMES, 2 );
	}

	@Override
	protected boolean lit(){
		if (ch instanceof Waystone) return ((Waystone) ch).attuned();
		if (ch == null || !(Dungeon.level instanceof OverworldLevel)) return false;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		if (ch.pos < 0 || ch.pos >= ow.length()) return false;
		return TravelShare.attunedAt( ow.worldX + ch.pos % ow.width(), ow.worldY + ch.pos / ow.width() );
	}

	@Override
	protected int glowColor(){
		return Travel.RUNE;
	}

	@Override
	protected int glowSize(){
		return 15;
	}

	@Override
	protected float glowLevel(){
		return 0.45f;
	}
}
