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

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Squid;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Piranha;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfInvisibility;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Random;

public class PoolRoom extends SpecialRoom {

	private static final int NPIRANHAS	= 3;
	
	@Override
	public int minWidth() {
		return 6;
	}
	
	@Override
	public int minHeight() {
		return 6;
	}
	
	public void paint(Level level ) {
		
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.WATER );
		
		Door door = entrance();
		door.set( Door.Type.REGULAR );

		int x = -1;
		int y = -1;
		if (door.x == left) {
			
			x = right - 1;
			y = top + height() / 2;
			Painter.fill(level, left+1, top+1, 1, height()-2, Terrain.EMPTY_SP);
			
		} else if (door.x == right) {
			
			x = left + 1;
			y = top + height() / 2;
			Painter.fill(level, right-1, top+1, 1, height()-2, Terrain.EMPTY_SP);
			
		} else if (door.y == top) {
			
			x = left + width() / 2;
			y = bottom - 1;
			Painter.fill(level, left+1, top+1, width()-2, 1, Terrain.EMPTY_SP);
			
		} else if (door.y == bottom) {
			
			x = left + width() / 2;
			y = top + 1;
			Painter.fill(level, left+1, bottom-1, width()-2, 1, Terrain.EMPTY_SP);
			
		}
		
		int pos = x + y * level.width();
		level.drop( prize( level ), pos ).type = Heap.Type.CHEST;
		Painter.set( level, pos, Terrain.PEDESTAL );
		
		level.addItemToSpawn( new PotionOfInvisibility() );
		
		for (int i=0; i < NPIRANHAS; i++) {
			//deep pools sometimes hold a deep dweller instead of a piranha (Unleashed PD port)
			Mob fish = (Dungeon.depth >= 11 && Random.Int( 4 ) == 0) ? new Squid() : Piranha.random();
			do {
				fish.pos = level.pointToCell(random());
			} while (level.map[fish.pos] != Terrain.WATER|| level.findMob( fish.pos ) != null);
			level.mobs.add( fish );
		}
	}
	
	private static Item prize( Level level ) {

		Item prize;

		//33% chance for prize item
		if (Random.Int(3) == 0){
			prize = level.findPrizeItem();
			if (prize != null)
				return prize;
		}

		//1 floor set higher in probability, never cursed
		switch (Random.Int(5)){
			case 0: case 1: default:
				prize = Generator.randomWeapon((Dungeon.depth / 5) + 1);
				if (((Weapon)prize).hasCurseEnchant()){
					((Weapon) prize).enchant(null);
				}
				break;
			case 2:
				prize = Generator.randomMissile((Dungeon.depth / 5) + 1);
				if (((Weapon)prize).hasCurseEnchant()){
					((Weapon) prize).enchant(null);
				}
				break;
			case 3: case 4:
				prize = Generator.randomArmor((Dungeon.depth / 5) + 1);
				if (((Armor)prize).hasCurseGlyph()){
					((Armor) prize).inscribe(null);
				}
				break;
		}
		prize.cursed = false;
		prize.cursedKnown = true;
		
		//33% chance for an extra update.
		if (Random.Int(3) == 0){
			prize.upgrade();
		}

		return prize;
	}
}
