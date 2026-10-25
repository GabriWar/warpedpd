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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * The sea's moments (sea/Seafaring): a wave breaking over a boat, and a boat breaking up. The water's
 * own crowns, droplets and rings (WaterFX) in its foam, and for a wreck its timbers flung out; the
 * effects' own random numbers, the render thread. The host's screen shows them; a co-op guest has the
 * line in his log (the kit's emitters are never relayed).
 */
public final class SeaFx {

	private SeaFx(){}

	//a hull's timbers, light and dark
	private static final int TIMBER = 0x9C7650, TIMBER_DARK = 0x6A4E34;

	/** A wave over a boat at its sailor: a middling splash at his feet and the spray off it. Any thread. */
	public static void wave( Char ch ){
		if (ch == null || ch.sprite == null || !ch.sprite.visible) return;
		SpatialSound.play( Assets.Sounds.WATER, ch, 1.2f, FxRandom.Float( 0.6f, 0.75f ) );
		final float x = ch.sprite.x + ch.sprite.width() / 2f, y = ch.sprite.y + ch.sprite.height();
		Fx.post( () -> {
			WaterFX.splash( x, y, WaterFX.M, Element.NONE );
			WaterFX.droplets( x, y - 6, FxRandom.IntRange( 4, 6 ), 30f, 55f, 140f, WaterFX.liquid().foam );
		} );
	}

	/** A boat breaking up on a cell: a large splash and its timbers flung over the water. Any thread. */
	public static void wreckage( final int cell ){
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length()) return;
		SpatialSound.play( Assets.Sounds.ROCKS, cell, 0.6f, 0.7f );
		SpatialSound.play( Assets.Sounds.WATER, cell, 1.4f, 0.5f );
		final int w = level.width();
		Fx.post( () -> {
			float x = (cell % w + 0.5f) * DungeonTilemap.SIZE, y = (cell / w + 0.7f) * DungeonTilemap.SIZE;
			WaterFX.splash( x, y, WaterFX.L, Element.NONE );
			FxEmitter e = Fx.air();
			if (e == null) return;
			int n = FxBudget.allow( 7, FxBudget.P1 );
			for (int i = 0; i < n; i++){
				float a = (float)(-Math.PI / 2 + FxRandom.Float( -1.1f, 1.1f ));
				((FxParticle) e.recycle( FxParticle.class )).reset( x, y - 2 )
						.look( FxRandom.chance( 0.5f ) ? FxFrames.BLADE_3 : FxFrames.BLADE_4 )
						.paint( FxRandom.chance( 0.6f ) ? TIMBER : TIMBER_DARK )
						.polar( a, FxRandom.Float( 40f, 70f ) ).gravity( 160f ).spin( 0.2f )
						.ground( y + FxRandom.Float( 0f, 6f ), FxParticle.SETTLE, 0 ).life( 1.1f ).alpha( 1f, 0, 0.4f )
						.priority( FxBudget.P1 );
			}
		} );
	}
}
