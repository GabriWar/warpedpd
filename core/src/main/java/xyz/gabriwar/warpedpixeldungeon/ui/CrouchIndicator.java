/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Crouching;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Image;

/**
 * Always-visible tag button that toggles the hero's crouch (stealth) stance.
 * Active (crouching) state: purple tint. Inactive: muted grey.
 */
public class CrouchIndicator extends Tag {

	private static final int COLOR_IDLE    = 0x555566;
	private static final int COLOR_ACTIVE  = 0x6633AA;

	private Image icon;

	public CrouchIndicator() {
		super(COLOR_IDLE);
		setSize(SIZE, SIZE);
		visible = true;
	}

	@Override
	protected void createChildren() {
		super.createChildren();
		icon = Icons.get(Icons.ARROW);
		icon.originToCenter();
		icon.angle = 90f; // default: point down (not crouching)
		add(icon);
	}

	@Override
	protected void layout() {
		super.layout();
		if (!flipped)   icon.x = x + (SIZE - icon.width()) / 2f + 1;
		else            icon.x = x + width - (SIZE + icon.width()) / 2f - 1;
		icon.y = y + (height - icon.height) / 2f;
		PixelScene.align(icon);
	}

	@Override
	protected void onClick() {
		super.onClick();
		if (Dungeon.hero.ready) {
			Dungeon.hero.curAction = new HeroAction.CrouchToggle();
			Dungeon.hero.next();
		}
	}

	@Override
	public void update() {
		if (!Dungeon.hero.isAlive()) {
			visible = false;
		} else {
			visible = true;
			boolean crouching = Dungeon.hero.buff(Crouching.class) != null;
			// Update tag color to reflect state
			if (crouching) {
				setColor(COLOR_ACTIVE);
				icon.angle = -90f; // point up = "stand up"
			} else {
				setColor(COLOR_IDLE);
				icon.angle = 90f;  // point down = "crouch down"
			}
		}
		super.update();
	}
}
