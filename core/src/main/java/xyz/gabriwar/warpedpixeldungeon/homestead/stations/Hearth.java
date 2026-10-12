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

package xyz.gabriwar.warpedpixeldungeon.homestead.stations;

import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.sprites.HomesteadSprites;
import xyz.gabriwar.warpedpixeldungeon.sprites.StationSprite;

/**
 * The hearth: a stone fireplace under a hood, of brick or of rough stone. It is the home's fire:
 * lit, it warms the room it stands in by ROOM_HEAT (StationHeat) - a closed room round it stays
 * warm through a winter night - and its cell and the cells round it the way a camp fire does; no
 * wild animal is set down within HomeAnchors.RADIUS of it (StationAnchors); it bakes bread and
 * grills meat (StationRecipes); and anything dropped into its fire burns as in a camp fire's
 * (Heap.burn: raw meat grills, scrolls burn, a bomb goes off).
 */
public class Hearth extends HeatStation {

	{
		spriteClass = StationSprite.Hearth.class;
	}

	/** What a lit hearth gives its room, C. */
	public static final float ROOM_HEAT = 16f;

	@Override
	public int frame(){
		return HomesteadSprites.HEARTH;
	}

	//as warm on its cell and round it as a camp fire (actors/blobs/CampFire)
	@Override
	protected float cellHeat(){
		return 8f;
	}

	@Override
	protected float radiate(){
		return 5f;
	}

	@Override
	protected float litRoomHeat(){
		return ROOM_HEAT;
	}

	@Override
	public boolean anchorsHome(){
		return lit();
	}

	@Override
	protected void liveTurn( OverworldLevel level ){
		super.liveTurn( level );
		if (!lit() || !level.mobs.contains( this )) return;
		Heap heap = level.heaps.get( pos );
		if (heap != null) heap.burn();
	}
}
