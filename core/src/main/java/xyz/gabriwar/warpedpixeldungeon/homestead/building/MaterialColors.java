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

package xyz.gabriwar.warpedpixeldungeon.homestead.building;

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Charcoal;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Clay;
import xyz.gabriwar.warpedpixeldungeon.items.materials.CutStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.FiredBrick;
import xyz.gabriwar.warpedpixeldungeon.items.materials.GatorTooth;
import xyz.gabriwar.warpedpixeldungeon.items.materials.GlassPane;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.materials.IronFittings;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Leather;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.PineLog;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Resin;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Sand;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.items.materials.VultureFeather;

/**
 * The colour each building material's words take on screen (the floating "-2 planks" over a piece
 * going up, the cost lines of the build menu): the main colour of its own item sprite, lifted
 * enough to read over grass and night. Anything else gets a plain light grey.
 */
public final class MaterialColors {

	public static final int OTHER = 0xDDDDDD;

	private MaterialColors(){}

	public static int of( Class<? extends Item> material ){
		if (material == Log.class)          return 0xC08A52;
		if (material == PineLog.class)      return 0xD07A4A;
		if (material == Plank.class)        return 0xE3B77A;
		if (material == Resin.class)        return 0xFFA640;
		if (material == RoughStone.class)   return 0xB8B2A8;
		if (material == CutStone.class)     return 0xD2D2CC;
		if (material == Clay.class)         return 0xC2704E;
		if (material == FiredBrick.class)   return 0xF0784A;
		if (material == Sand.class)         return 0xF2E2A8;
		if (material == GlassPane.class)    return 0xA8DCFF;
		if (material == Thatch.class)       return 0xF2CC55;
		if (material == Rope.class)         return 0xDCC08A;
		if (material == Hide.class)         return 0xC9A27E;
		if (material == Leather.class)      return 0xB07A4C;
		if (material == Charcoal.class)     return 0x9A9A9A;
		if (material == IronFittings.class) return 0xA9B4C2;
		if (material == VultureFeather.class) return 0xB4ACA2;
		if (material == GatorTooth.class)   return 0xF0EAD6;
		return OTHER;
	}
}
