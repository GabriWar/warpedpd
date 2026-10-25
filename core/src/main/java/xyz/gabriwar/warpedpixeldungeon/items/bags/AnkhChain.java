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

package xyz.gabriwar.warpedpixeldungeon.items.bags;

import xyz.gabriwar.warpedpixeldungeon.items.AdamantRing;
import xyz.gabriwar.warpedpixeldungeon.items.AncientCoin;
import xyz.gabriwar.warpedpixeldungeon.items.Ankh;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfDead;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfLife;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfTranscendence;
import xyz.gabriwar.warpedpixeldungeon.items.Bone;
import xyz.gabriwar.warpedpixeldungeon.items.CavesKey;
import xyz.gabriwar.warpedpixeldungeon.items.CityKey;
import xyz.gabriwar.warpedpixeldungeon.items.ConchShell;
import xyz.gabriwar.warpedpixeldungeon.items.HallsKey;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.PrisonKey;
import xyz.gabriwar.warpedpixeldungeon.items.ReturnBeacon;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarah;
import xyz.gabriwar.warpedpixeldungeon.items.SewersKey;
import xyz.gabriwar.warpedpixeldungeon.items.TenguKey;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.LloydsBeacon;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.RingOfDisintegration;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.Key;
import xyz.gabriwar.warpedpixeldungeon.items.quest.EscapeCrystal;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class AnkhChain extends Bag {

	{
		image = ItemSpriteSheet.CHAIN;
	}

	@Override
	public boolean canHold( Item item ) {
		if (item instanceof Ankh || item instanceof Ring || item instanceof RingOfDisintegration
				|| item instanceof AdamantRing
				//keys of every kind: the ancient keys that open portals, the dungeon's door
				//keys and the skeleton key artifact
				|| item instanceof SewersKey || item instanceof PrisonKey
				|| item instanceof CavesKey || item instanceof CityKey
				|| item instanceof HallsKey || item instanceof TenguKey
				|| item instanceof Key || item instanceof SkeletonKey
				//and everything else that takes you somewhere: the books, the coin, the bone and the
				//conch that open portals, the forged SanChikarah, the beacons and the vault's crystal
				|| item instanceof BookOfDead || item instanceof BookOfLife
				|| item instanceof BookOfTranscendence || item instanceof AncientCoin
				|| item instanceof Bone || item instanceof ConchShell || item instanceof SanChikarah
				|| item instanceof ReturnBeacon || item instanceof LloydsBeacon
				|| item instanceof EscapeCrystal){
			return super.canHold(item);
		} else {
			return false;
		}
	}

	public int capacity(){
		return 48;
	}

	@Override
	public int value() {
		return 50;
	}
}
