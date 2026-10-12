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

/**
 * Parts of the HUD put aside while something stands in for them (the build bar in place of the
 * toolbar and the inventory pane, GameScene.showBuildBar), and put back exactly as they were:
 * each part's own visible and active flags are noted when they are hidden, so a toolbar the intro
 * had hidden stays hidden afterwards. Hiding twice notes nothing new; restoring with nothing
 * hidden changes nothing. A null part (no inventory pane on a phone) is skipped.
 */
public class HudSwap {

	private final Gizmo[] parts;
	private final boolean[] wasVisible, wasActive;
	private boolean hidden;

	public HudSwap( Gizmo... parts ){
		this.parts = parts;
		wasVisible = new boolean[parts.length];
		wasActive = new boolean[parts.length];
	}

	/** Are the parts put aside now. */
	public boolean hidden(){
		return hidden;
	}

	/** Notes each part's flags and hides it. */
	public void hide(){
		if (hidden) return;
		for (int i = 0; i < parts.length; i++){
			if (parts[i] == null) continue;
			wasVisible[i] = parts[i].visible;
			wasActive[i] = parts[i].active;
			parts[i].visible = parts[i].active = false;
		}
		hidden = true;
	}

	/** A part put aside was shown or hidden anew meanwhile (the inventory pane laid out again,
	 *  GameScene.applyInvState): that is what it comes back as, and it is hidden again. Nothing
	 *  while nothing is put aside, or for a part this swap does not hold. */
	public void renote( Gizmo part ){
		if (!hidden || part == null) return;
		for (int i = 0; i < parts.length; i++){
			if (parts[i] != part) continue;
			wasVisible[i] = part.visible;
			wasActive[i] = part.active;
			part.visible = part.active = false;
		}
	}

	/** Puts each part back as it was noted. */
	public void restore(){
		if (!hidden) return;
		for (int i = 0; i < parts.length; i++){
			if (parts[i] == null) continue;
			parts[i].visible = wasVisible[i];
			parts[i].active = wasActive[i];
		}
		hidden = false;
	}
}
