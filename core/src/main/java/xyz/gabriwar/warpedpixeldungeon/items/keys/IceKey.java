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

package xyz.gabriwar.warpedpixeldungeon.items.keys;

import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;

//ported from Remixed PD's Ice Caves: the caged kobold's price for its candle.
//The Ice Guardian Core drops it two floors below the cage.
public class IceKey extends Key {

	{
		image = ItemSpriteSheet.ICE_KEY;
		unique = true;
	}

	public IceKey() {
		this( 0 );
	}

	public IceKey( int depth ) {
		super();
		this.depth = depth;
	}

	/** The cage accepts a key from any floor, including legacy inventory keys. */
	public static boolean consume(Hero hero) {
		for (Notes.KeyRecord record : Notes.getRecords(Notes.KeyRecord.class)) {
			if (record.type() == IceKey.class && record.quantity() > 0) {
				return Notes.remove(new IceKey(record.depth()));
			}
		}
		IceKey key = hero.belongings.getItem(IceKey.class);
		if (key == null) return false;
		key.detach(hero.belongings.backpack);
		return true;
	}
}
