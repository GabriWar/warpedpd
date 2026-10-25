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

package xyz.gabriwar.warpedpixeldungeon.ui;

import com.watabou.noosa.Group;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * A long press on a bag slot to put it in a quickslot closes the bag window in the middle of its
 * scroll pane's update; the pane then walked its destroyed content (members null) and crashed.
 */
public class DestroyedGroupTest {

	@Test
	public void aDestroyedGroupHasNoMembersToWalk(){
		Group outer = new Group();
		outer.add( new Group() );
		outer.destroy();
		assertTrue( outer.membersView().isEmpty() );
	}
}
