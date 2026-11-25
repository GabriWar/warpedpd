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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Bundle;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * A game loads its hero, buffs and all, before the floor (Dungeon.loadGame), and the potion of
 * eye's buff revealed the floor's secrets as it was attached, so a save with it running could not
 * be loaded: the floor was still null (1 report, 24.7.4, the save crashed on every load).
 */
public class EyeingOnLoadTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void aHeroUnderItLoadsBeforeTheFloor(){
		Level saved = Dungeon.level;
		try {
			Dungeon.level = null;
			Hero hero = new Hero();
			Buff.prolong( hero, Eyeing.class, Eyeing.DURATION );
			assertNotNull( hero.buff( Eyeing.class ) );

			Bundle b = new Bundle();
			b.put( "eyeing", hero.buff( Eyeing.class ) );
			Hero loaded = new Hero();
			((Eyeing) b.get( "eyeing" )).attachTo( loaded );
			assertNotNull( "still on the loaded hero", loaded.buff( Eyeing.class ) );
		} finally {
			Dungeon.level = saved;
		}
	}
}
