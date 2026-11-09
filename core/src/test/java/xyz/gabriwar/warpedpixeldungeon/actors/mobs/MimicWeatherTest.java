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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.StormCloud;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A mimic still hiding as a chest sits through the weather: the climate's cold, heat and rain
 * gave it negative buffs and a storm's bolt struck it, and either woke it (a crystal mimic fled
 * the moment the hero arrived). Once it shows itself the weather reaches it like any mob.
 */
public class MimicWeatherTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void theWeatherPassesAHiddenMimicBy(){
		for (Mimic mimic : new Mimic[]{ new Mimic(), new CrystalMimic(), new GoldenMimic() }){
			String name = mimic.getClass().getSimpleName();
			assertFalse( name + " feels no climate", mimic.feelsWeather() );
			assertTrue( name + ": a storm's bolt", mimic.isImmune( StormCloud.class ) );
			assertTrue( name + ": the cold", mimic.isImmune( Hypothermia.class ) );
			assertFalse( name + ": a gas still reaches it", mimic.isImmune( ToxicGas.class ) );

			mimic.alignment = Char.Alignment.ENEMY;
			assertTrue( name + " revealed feels the climate", mimic.feelsWeather() );
			assertFalse( name + " revealed is struck", mimic.isImmune( StormCloud.class ) );
		}
	}
}
