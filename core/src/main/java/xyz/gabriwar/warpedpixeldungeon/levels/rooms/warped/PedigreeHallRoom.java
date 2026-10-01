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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.PedigreeWard;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClassArmor;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SpecialRoom;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

/**
 * The Pedigree Hall: three plinths, and on them the same piece three times over - the
 * very kind of weapon the hero is carrying (or their armor, or failing both a weapon of
 * the floor's tier), identified and laid out to be compared:
 *
 *   common    and alpha   - bare numbers, the perk at its strongest and its twist
 *   rare      and beta    - the middle of both roads
 *   legendary and gamma   - the best numbers, the perk at its weakest
 *
 * It is the rarity and type system as one question: numbers or perk. Take one and the
 * ward (PedigreeWard) breaks the other two.
 */
public class PedigreeHallRoom extends SpecialRoom {

	@Override
	public int minWidth(){ return 7; }
	@Override
	public int minHeight(){ return 7; }

	private static final Rarity[]   RARITIES = { Rarity.COMMON, Rarity.RARE, Rarity.LEGENDARY };
	private static final ItemType[] TYPES    = { ItemType.ALPHA, ItemType.BETA, ItemType.GAMMA };

	@Override
	public void paint( Level level ){
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY_SP );

		RoomFrame f = new RoomFrame( this, entrance() );
		int mid = f.span / 2;
		int back = f.depth - 1;

		//the three plinths in a row one short of the back wall, a rope post between each
		//pair, and the plaque on the wall behind the middle one
		int[] sides = { mid - 2, mid, mid + 2 };
		Class<? extends Item> kind = lineage( Dungeon.hero );

		Integer[] order = { 0, 1, 2 };
		Random.shuffle( order );
		for (int i = 0; i < 3; i++){
			int cell = f.cell( level, back - 1, sides[i] );
			Painter.set( level, cell, Terrain.PEDESTAL );
			level.drop( piece( kind, RARITIES[order[i]], TYPES[order[i]] ), cell );
			Blob.seed( cell, 1, PedigreeWard.class, level );
		}
		for (int s : new int[]{ mid - 1, mid + 1 }){
			int cell = f.cell( level, back - 1, s );
			Painter.set( level, cell, Terrain.CUSTOM_DECO );
			WarpedRoomTiles.place( level, cell, WarpedRoomTiles.ROPE_POST, "rope_post" );
		}
		int plaque = f.cell( level, back, mid );
		Painter.set( level, plaque, Terrain.CUSTOM_DECO );
		WarpedRoomTiles.place( level, plaque, WarpedRoomTiles.PLAQUE, "pedigree_plaque" );

		entrance().set( Door.Type.LOCKED );
		level.addItemToSpawn( new IronKey( Dungeon.depth ) );
	}

	//what the hall is showing: the class of the hero's own weapon when it is an ordinary
	//one, else of their armor, else a weapon fit for the floor
	private static Class<? extends Item> lineage( Hero hero ){
		if (hero != null){
			Item weapon = hero.belongings.weapon();
			if (weapon instanceof MeleeWeapon && !weapon.unique && generated( weapon.getClass(),
					Generator.Category.WEP_T1, Generator.Category.WEP_T2, Generator.Category.WEP_T3,
					Generator.Category.WEP_T4, Generator.Category.WEP_T5 )){
				return weapon.getClass();
			}
			Item armor = hero.belongings.armor();
			if (armor instanceof Armor && !(armor instanceof ClassArmor) && !armor.unique
					&& generated( armor.getClass(), Generator.Category.ARMOR )){
				return armor.getClass();
			}
		}
		return Generator.randomWeapon( WarpedRooms.threat() / 5, true ).getClass();
	}

	private static boolean generated( Class<?> type, Generator.Category... categories ){
		for (Generator.Category cat : categories){
			if (cat.classes == null) continue;
			for (Class<?> c : cat.classes) if (c == type) return true;
		}
		return false;
	}

	private static Item piece( Class<? extends Item> kind, Rarity rarity, ItemType type ){
		Item item = Reflection.newInstance( kind );
		item.level( 0 );
		item.cursed = false;
		WarpedRooms.forceQuality( item, rarity, type );
		item.identify( false );
		return item;
	}
}
