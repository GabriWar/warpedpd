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

package xyz.gabriwar.warpedpixeldungeon.net.ui;

/**
 * Shared palette + helpers for the multiplayer UI, so every net screen and
 * overlay speaks the same visual language:
 *
 *   GREEN  — host identity, good ping, "your turn"
 *   YELLOW — player identity (matches the game's TITLE_COLOR family)
 *   BLUE   — spectator identity
 *   MUTED  — captions and hints (same grey the base game uses for fine print)
 */
public final class NetUi {

	public static final int GREEN  = 0x44FF44;
	public static final int YELLOW = 0xFFEE88;
	public static final int BLUE   = 0x44CCFF;
	public static final int ORANGE = 0xFFAA00;
	public static final int RED    = 0xFF6644;
	public static final int MUTED  = 0x888888;

	/** Latency quality color: green &lt;100ms, orange &lt;300ms, red beyond. */
	public static int pingColor(long ping) {
		if (ping < 0)    return MUTED;
		if (ping < 100)  return GREEN;
		if (ping < 300)  return ORANGE;
		return RED;
	}

	private NetUi() {}
}
