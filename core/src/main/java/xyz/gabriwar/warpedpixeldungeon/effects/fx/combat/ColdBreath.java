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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.homestead.Shelter;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;

/**
 * A hero's breath in the cold air: out under the open sky (the world's surface and its peaks),
 * while the air he breathes is below FREEZING - the open air over his cell (ClimateManager.airAt),
 * or in a room he built that room's own (Shelter.air: a warm one shows none) - a puff from the front
 * of his head every BREATH_MIN to BREATH_MAX s, the chill's own (Looks.Cold.breath). None while a
 * chill or hypothermia breathes for him already (their look, Looks.Cold), while his sprite is hidden
 * or while time stands still. The air is read every READ_EVERY s. One per hero's sprite, stepped
 * from its update; render thread, visual only.
 */
public final class ColdBreath {

	/** A puff every BREATH_MIN to BREATH_MAX s while the air is below FREEZING (C), read every
	 *  READ_EVERY s. */
	public static final float BREATH_MIN = 1.2f, BREATH_MAX = 1.6f, FREEZING = 0f, READ_EVERY = 0.5f;

	private final CharSprite s;
	private boolean cold;
	//the first puff's delay is drawn when the cold first shows (update): a sprite in the warm draws
	//nothing from the effects' random, so the warm keep the very looks they had
	private float readIn, breathIn = -1;

	public ColdBreath( CharSprite s ){
		this.s = s;
	}

	/** A frame of the sprite's; `chilled`: a chill's or hypothermia's look breathes for it already. */
	public void update( boolean chilled ){
		if (Emitter.freezeEmitters) return;
		float dt = Game.elapsed;
		if ((readIn -= dt) <= 0){
			readIn = READ_EVERY;
			cold = freezing( Dungeon.level, s.ch );
		}
		if (!cold || chilled) return;
		if (breathIn < 0) breathIn = FxRandom.Float( BREATH_MIN );
		if ((breathIn -= dt) <= 0){
			breathIn = FxRandom.Float( BREATH_MIN, BREATH_MAX );
			if (s.visible) Looks.Cold.breath( s, false );
		}
	}

	/** Whether the air one living on a level breathes is below FREEZING, out under its open sky. */
	static boolean freezing( Level level, Char ch ){
		if (!(level instanceof OverworldLevel) || ch == null || !ch.isAlive() || ch.pos < 0 || ch.pos >= level.length()) return false;
		OverworldLevel ow = (OverworldLevel) level;
		return ow.openSky() && Shelter.air( ow, ch.pos, ClimateManager.airAt( ow, ch.pos ) ) < FREEZING;
	}
}
