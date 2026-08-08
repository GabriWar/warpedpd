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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.special;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.MimicTooth;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.TrinketCatalyst;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Random;

public class TreasuryRoom extends SpecialRoom {

	public void paint( Level level ) {
		
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY );
		
		Painter.set( level, center(), Terrain.STATUE );
		
		Heap.Type heapType = Random.Int( 2 ) == 0 ? Heap.Type.CHEST : Heap.Type.HEAP;
		
		int n = Random.IntRange( 2, 3 );
		float mimicChance = 1/5f * MimicTooth.mimicChanceMultiplier();
		for (int i=0; i < n; i++) {
			Item item = level.findPrizeItem(TrinketCatalyst.class);
			if (item == null) item = new Gold().random();

			int pos;
			do {
				pos = level.pointToCell(random());
			} while (level.map[pos] != Terrain.EMPTY || level.heaps.get( pos ) != null || level.findMob(pos) != null);
			if (heapType == Heap.Type.CHEST && Dungeon.depth > 1 && Random.Float() < mimicChance){
				level.mobs.add(Mimic.spawnAt(pos, item));
			} else {
				level.drop( item, pos ).type = heapType;
			}
		}
		
		//living-world twist: the hoard follows the season (autumn harvest wealth,
		//summer heat corrodes it) — the scattered piles scale with the season.
		float goldMult = GameCalendar.seasonGoldMultiplier();
		if (heapType == Heap.Type.HEAP) {
			for (int i=0; i < 6; i++) {
				int pos;
				do {
					pos = level.pointToCell(random());
				} while (level.map[pos] != Terrain.EMPTY);
				level.drop( new Gold( Math.max(1, Math.round(Random.IntRange( 5, 12 ) * goldMult)) ), pos );
			}
		}

		//the vault's lock is solar: found during a solar eclipse, the tumblers
		//have already slid open — no key needed, and the black sun leaves a bonus.
		if (ClimateManager.isSolarEclipse()) {
			int pos;
			do {
				pos = level.pointToCell(random());
			} while (level.map[pos] != Terrain.EMPTY || level.heaps.get( pos ) != null);
			level.drop( new Gold( Random.IntRange( 100, 200 ) ), pos );
			entrance().set( Door.Type.UNLOCKED );
		} else {
			entrance().set( Door.Type.LOCKED );
			level.addItemToSpawn( new IronKey( Dungeon.depth ) );
		}
	}
}
