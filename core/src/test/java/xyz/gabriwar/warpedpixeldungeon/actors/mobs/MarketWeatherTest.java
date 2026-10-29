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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.StormCloud;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The Undercroft's guards stand where the weather reaches: while the peace holds its bolts, cold
 * and heat pass them by, since being struck by them counted as the hero's steel and shut every
 * market for the run. Once the market has turned they are ordinary enemies again.
 */
public class MarketWeatherTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void theWeatherPassesAPeacefulGuardBy(){
		Hero saved = Dungeon.hero;
		try {
			Dungeon.hero = new Hero();
			MarketBodyguard guard = new MarketBodyguard();
			assertTrue( "a storm's bolt", guard.isImmune( StormCloud.class ) );
			assertTrue( "the cold", guard.isImmune( Hypothermia.class ) );
			assertFalse( "a gas still reaches him", guard.isImmune( ToxicGas.class ) );

			guard.turnHostile();
			assertFalse( "a turned guard is an enemy like any", guard.isImmune( StormCloud.class ) );
			assertFalse( guard.isImmune( Hypothermia.class ) );
		} finally {
			Dungeon.hero = saved;
		}
	}
}
