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

package xyz.gabriwar.warpedpixeldungeon.homestead.gathering;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Alligator;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bison;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Camel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import com.watabou.utils.Random;

/**
 * The hides the world's beasts leave (a homestead's leather starts here): a deer and a bison always,
 * a goat, a yeti, a camel and an alligator one time in two, a wolf one time in three, beside whatever
 * else they drop. Deer, goats, bison, camels and alligators lay theirs with their carcass (Deer.die,
 * Goat.die and the rest); the wolves and the yeti through their extra loot (Mob.dropExtraLoot), not
 * held back by the hero's level: a hide is no prize to outgrow.
 * Each declares it as its first extra loot (Mob.declareExtraLoot), so the bestiary lists it, and
 * one dropped marks it seen there (drop).
 */
public final class Hides {

	private Hides(){}

	public static final float DEER = 1f, GOAT = 0.5f, YETI = 0.5f, WOLF = 1 / 3f;
	public static final float BISON = 1f, CAMEL = 0.5f, GATOR = 0.5f;

	/** The chance a beast of this kind leaves a hide, 0 for none. */
	public static float chance( Class<? extends Mob> kind ){
		if (kind == Deer.class) return DEER;
		if (kind == Goat.class) return GOAT;
		if (kind == Yeti.class) return YETI;
		if (kind == BrownWolf.class || kind == GrayWolf.class) return WOLF;
		if (kind == Bison.class) return BISON;
		if (kind == Camel.class) return CAMEL;
		if (kind == Alligator.class) return GATOR;
		return 0f;
	}

	/** The bestiary's slot for the hide: each beast's first extra loot (Mob.getDrops). */
	public static final int SLOT = 3;

	/** Whether this beast, dying, leaves a hide. */
	public static boolean dropped( Mob beast ){
		float c = chance( beast.getClass() );
		return c > 0f && Random.Float() < c;
	}

	/** A dying beast's hide, when it leaves one (dropped): on the cell given, and seen in the
	 *  bestiary from then on. */
	public static void drop( Mob beast, int cell ){
		if (!dropped( beast )) return;
		Heap h = Dungeon.level.drop( new Hide(), cell );
		if (h.sprite != null) h.sprite.drop( beast.pos );
		Bestiary.setLootSlotSeen( beast.getClass(), SLOT );
	}
}
