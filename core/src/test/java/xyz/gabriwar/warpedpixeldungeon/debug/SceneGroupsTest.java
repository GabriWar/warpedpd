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

package xyz.gabriwar.warpedpixeldungeon.debug;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** The scenes window lists scenes by group: every scene sits in exactly one listed group. */
public class SceneGroupsTest {

	@Test
	public void everySceneIsInOneListedGroup(){
		int total = 0;
		for (String group : DebugScenes.CATEGORIES){
			StringBuilder ids = new StringBuilder();
			for (DebugScenes.Scene s : DebugScenes.inCategory( group )) ids.append( ' ' ).append( s.id() );
			System.out.println( group + " (" + DebugScenes.inCategory( group ).size() + "):" + ids );
			total += DebugScenes.inCategory( group ).size();
		}
		for (DebugScenes.Scene s : DebugScenes.SCENES){
			assertTrue( s.id(), Arrays.asList( DebugScenes.CATEGORIES ).contains( DebugScenes.category( s.id() ) ) );
		}
		assertEquals( DebugScenes.SCENES.length, total );
	}
}
