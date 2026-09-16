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

package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import com.watabou.noosa.Image;

/**
 * Lightweight buff for spectator display. Stores only the visual data
 * needed by BuffIndicator (icon, type, fade, text). No gameplay logic.
 */
public class SpectatorBuff extends Buff {

	private int iconId;
	private float fade;
	private String text;

	public SpectatorBuff(int icon, int typeOrdinal, float fade, String text) {
		this.iconId = icon;
		buffType[] all = buffType.values();
		this.type = (typeOrdinal >= 0 && typeOrdinal < all.length) ? all[typeOrdinal] : buffType.NEUTRAL;
		this.fade = fade;
		this.text = text;
	}

	/** Update visual data in-place without detach/reattach */
	public void updateVisuals(int icon, int typeOrdinal, float fade, String text) {
		this.iconId = icon;
		buffType[] all = buffType.values();
		this.type = (typeOrdinal >= 0 && typeOrdinal < all.length) ? all[typeOrdinal] : buffType.NEUTRAL;
		this.fade = fade;
		this.text = text;
	}

	public int getIconId() { return iconId; }

	@Override
	public int icon() { return iconId; }

	@Override
	public float iconFadePercent() { return fade; }

	@Override
	public String iconTextDisplay() { return text; }

	@Override
	public void tintIcon(Image icon) {}

	@Override
	public boolean act() { return true; }

	@Override
	public void fx(boolean on) {}
}
