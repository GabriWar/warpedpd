/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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

import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite.Glowing;

public class GoldenSkeletonKey extends Key {

	{
		image = ItemSpriteSheet.GOLDEN_KEY;
	}

	public GoldenSkeletonKey() {
		this(0);
	}

	public GoldenSkeletonKey(int depth) {
		super();
		this.depth = depth;
	}

	//a master key: opens any lock, from any floor, after confirmation.
	//the cell the player has confirmed spending a key on this turn
	public static int confirmedCell = -1;

	public static boolean anyInJournal(){
		for (Notes.KeyRecord rec : Notes.getRecords(Notes.KeyRecord.class)){
			if (rec.type() == GoldenSkeletonKey.class && rec.quantity() > 0){
				return true;
			}
		}
		return false;
	}

	public static boolean removeAny(){
		for (Notes.KeyRecord rec : Notes.getRecords(Notes.KeyRecord.class)){
			if (rec.type() == GoldenSkeletonKey.class && rec.quantity() > 0){
				return Notes.remove(new GoldenSkeletonKey(rec.depth()));
			}
		}
		return false;
	}

	private static final Glowing WHITE = new Glowing(0xFFFFCC);

	@Override
	public Glowing glowing() {
		return WHITE;
	}
}
