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

package xyz.gabriwar.warpedpixeldungeon.levels;

import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

//a level's mobs can change under a walk over them (another thread, or the walk itself)
public class MobSetTest {

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Test
	public void aWalkSurvivesChangesUnderIt(){
		MobSet set = new MobSet();
		ArrayList<Mob> rats = new ArrayList<>();
		for (int i = 0; i < 20; i++){
			Rat r = new Rat();
			rats.add( r );
			set.add( r );
		}
		int seen = 0;
		for (Mob m : set){
			//what a render-thread GameScene.add or a death does mid-walk
			set.add( new Rat() );
			set.remove( rats.get( seen ) );
			seen++;
		}
		assertEquals( "the walk goes over the set as it was", 20, seen );
		assertEquals( 20, set.size() );
		for (Mob r : rats) assertFalse( set.contains( r ) );

		//a walk can take out what it is on
		for (Iterator<Mob> it = set.iterator(); it.hasNext(); ){
			it.next();
			it.remove();
		}
		assertTrue( set.isEmpty() );

		//copies are MobSets and copies of what was there
		Rat r = new Rat();
		set.add( r );
		@SuppressWarnings("unchecked")
		HashSet<Mob> copy = (HashSet<Mob>) set.clone();
		assertTrue( copy instanceof MobSet );
		set.remove( r );
		assertTrue( copy.contains( r ) );
		assertFalse( set.contains( r ) );
	}

	@Test
	public void anotherThreadChangingItNeverBreaksAWalk() throws Exception {
		final MobSet set = new MobSet();
		for (int i = 0; i < 200; i++) set.add( new Rat() );
		final AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread writer = new Thread( () -> {
			try {
				for (int i = 0; i < 20000; i++){
					Rat r = new Rat();
					set.add( r );
					set.remove( r );
				}
			} catch (Throwable t){
				failure.set( t );
			}
		} );
		writer.start();
		try {
			while (writer.isAlive()){
				int n = 0;
				for (Mob m : set) if (m != null) n++;
				assertTrue( n >= 200 );
			}
		} finally {
			writer.join();
		}
		assertNull( failure.get() );
		assertEquals( 200, set.size() );
	}
}
