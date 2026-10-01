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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped;

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceBlock;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SpecialRoom;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The Frozen Cache: a cellar flooded and frozen over, with what was stored in it locked
 * into blocks of ice standing on the sheet (IceBlock). The middle block holds the piece
 * worth coming for - gear of at least rare quality - and the rest hold the small stuff.
 *
 * The season is read once, as the floor is made: a winter cache kept one block more than
 * it would have, and a summer one is found already sweating, two fifths of the way gone.
 * Everything after that is the temperature simulation's: the sheet itself thaws to open
 * water in a warm season, and the blocks hold out on their own cold until something
 * hotter than the weather is brought to them.
 *
 * Only offered on floors whose water can freeze (WarpedRooms.allowed).
 */
public class FrozenCacheRoom extends SpecialRoom {

	@Override
	public int minWidth(){ return 7; }
	@Override
	public int minHeight(){ return 7; }

	/** set by the test floor to paint a given season's cache; null reads the calendar */
	public GameCalendar.Season seasonOverride = null;

	@Override
	public void paint( Level level ){
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.FROZEN_WATER );

		RoomFrame f = new RoomFrame( this, entrance() );
		//dry stone to step in on
		Painter.set( level, f.at( 0, f.doorSide ), Terrain.EMPTY );

		GameCalendar.Season season = seasonOverride != null ? seasonOverride : GameCalendar.season();

		//candidate spots on a two-step lattice, so no two blocks ever touch and one fire
		//cannot do the work of two
		Point centre = f.at( f.depth / 2, f.span / 2 );
		ArrayList<Point> spots = new ArrayList<>();
		for (int row = 1; row < f.depth; row += 2){
			for (int s = (row / 2) % 2 == 0 ? 0 : 1; s < f.span; s += 2){
				Point p = f.at( row, s );
				if (Math.abs( p.x - centre.x ) <= 1 && Math.abs( p.y - centre.y ) <= 1) continue;
				spots.add( p );
			}
		}
		Random.shuffle( spots );

		int small = 2 + (season == GameCalendar.Season.WINTER ? 1 : 0);
		float head = season == GameCalendar.Season.SUMMER ? 0.4f : 0f;

		block( level, centre, centrepiece(), head );
		for (int i = 0; i < small && i < spots.size(); i++){
			block( level, spots.get( i ), Generator.random( Random.oneOf(
					Generator.Category.POTION, Generator.Category.POTION,
					Generator.Category.SCROLL, Generator.Category.RING,
					Generator.Category.WAND ) ), head );
		}

		entrance().set( Door.Type.REGULAR );
	}

	private static Item centrepiece(){
		Item item = Generator.random( Random.Int( 2 ) == 0
				? Generator.Category.WEAPON : Generator.Category.ARMOR );
		item.cursed = false;
		return WarpedRooms.forceQuality( item,
				Random.Float() < 0.3f ? Rarity.LEGENDARY : Rarity.RARE, null );
	}

	private void block( Level level, Point at, Item prize, float head ){
		IceBlock block = new IceBlock();
		block.encase( prize );
		block.preMelt( head );
		block.pos = level.pointToCell( at );
		level.mobs.add( block );
	}

	//the floor's painter must not pour water or sow grass over the sheet
	@Override
	public boolean canPlaceWater( Point p ){
		return false;
	}

	@Override
	public boolean canPlaceGrass( Point p ){
		return false;
	}

	//nor a trap on the one dry step in
	@Override
	public boolean canPlaceTrap( Point p ){
		return false;
	}
}
