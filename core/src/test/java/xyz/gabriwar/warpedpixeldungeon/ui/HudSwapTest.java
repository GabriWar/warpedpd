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

import com.watabou.noosa.Gizmo;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The toolbar put aside for the build bar and put back (HudSwap, GameScene.showBuildBar): each
 * part comes back exactly as it was, hidden twice or restored twice changes nothing, a missing
 * part (no inventory pane on a phone) is skipped, and a part laid out again while it is aside
 * (the inventory pane when the hero's items change) stays hidden and comes back as laid out.
 */
public class HudSwapTest {

	@Test
	public void partsComeBackExactlyAsTheyWere(){
		Gizmo toolbar = new Gizmo(), inventory = new Gizmo();
		//the intro had hidden the inventory pane but left it active
		inventory.visible = false;
		HudSwap swap = new HudSwap( toolbar, inventory, null );
		assertFalse( swap.hidden() );
		swap.hide();
		assertTrue( swap.hidden() );
		assertFalse( toolbar.visible );
		assertFalse( toolbar.active );
		assertFalse( inventory.active );

		//hidden again: nothing new is noted (the parts are hidden now, not as they were)
		swap.hide();
		swap.restore();
		assertFalse( swap.hidden() );
		assertTrue( toolbar.visible );
		assertTrue( toolbar.active );
		assertFalse( "it stays as the intro left it", inventory.visible );
		assertTrue( inventory.active );

		//restored with nothing hidden: nothing changes
		toolbar.visible = false;
		swap.restore();
		assertFalse( toolbar.visible );
	}

	@Test
	public void aPartLaidOutAgainWhileAsideStaysHiddenAndComesBackAsLaidOut(){
		Gizmo toolbar = new Gizmo(), inventory = new Gizmo();
		inventory.visible = inventory.active = false;
		HudSwap swap = new HudSwap( toolbar, inventory );
		swap.hide();
		//a piece built: the items changed and the pane was laid out (and shown) again
		inventory.visible = inventory.active = true;
		swap.renote( inventory );
		assertFalse( "hidden while the bar is up", inventory.visible );
		assertFalse( inventory.active );
		assertFalse( toolbar.visible );
		swap.restore();
		assertTrue( "back as it was last laid out", inventory.visible );
		assertTrue( inventory.active );

		//nothing aside: renote changes nothing
		swap.renote( inventory );
		assertTrue( inventory.visible );
	}
}
