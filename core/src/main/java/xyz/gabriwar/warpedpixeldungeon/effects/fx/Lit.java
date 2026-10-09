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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

/**
 * Something drawn as light that lives in an ordinary layer (a beam among the effects, a bolt's
 * head in its emitter) but must draw over the night's tint: GameScene.carryLight(lit) puts a
 * stand-in in the light layer that draws it there while it is visible, and the thing itself skips
 * its own draw while it is carried (the stand-in's exists says so):
 *
 *     above = GameScene.carryLight( this );
 *     ...
 *     public void draw(){ if (above == null || !above.exists) drawLit(); }
 */
public interface Lit {

	/** Draws it, as light. */
	void drawLit();

	/** Whether it is still to be drawn. */
	boolean isVisible();
}
