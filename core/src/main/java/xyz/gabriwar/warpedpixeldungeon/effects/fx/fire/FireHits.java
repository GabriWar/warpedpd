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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.fire;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * What fire's wands leave where they strike (the bolts and the cone themselves are the bolts
 * area's): a firebolt's impact - its core, ring, sparks and light (FxRecipes.impact), six embers,
 * a small scorch for SCORCH_HOLD s, or on water a hiss of steam instead of the scorch - and the
 * fireblast's heat: embers and shimmer over whoever it caught or set off. Visual only, from any
 * thread (handed to the render thread), shown where the hero sees the cell; the strike and the
 * heat are sent on to co-op guests (FireNet), who see them where their own heroes see.
 */
public final class FireHits {

	private FireHits(){}

	public static final int BOLT_EMBERS = 6, BOLT_STEAM = 4, BLAST_EMBERS = 6, BLAST_RAYS = 3, CONE_EMBERS = 3;
	public static final float SCORCH_HOLD = 3f, SCORCH_FADE = 1.5f;

	/** A firebolt striking a cell. */
	public static void firebolt( int cell ){
		FireNet.send( FireNet.FIREBOLT, cell, 0, 0, null );
		bolt( cell );
	}

	static void bolt( final int cell ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> bolt( cell ) );
			return;
		}
		if (!seen( cell )) return;
		float x = x( cell ), y = y( cell );
		FxRecipes.impact( x, y, Element.FIRE, 1f );
		FxRecipes.embers( x, y, Element.FIRE, BOLT_EMBERS );
		if (WaterFX.onWater( x, y + 4 )){
			FxRecipes.steam( x, y, BOLT_STEAM );
		} else {
			FxDecal.at( x, y + 4, FxFrames.SCORCH_S, FxRecipes.SCORCH_COLOR, 0.7f, SCORCH_HOLD, SCORCH_FADE, FxRandom.chance( 0.5f ) );
		}
	}

	/** The fireblast's heat over a cell: embers rising and (for a staff's blast) shimmer. */
	public static void heat( int cell, int embers, int rays ){
		FireNet.send( FireNet.HEAT, cell, embers, rays, null );
		heatOver( cell, embers, rays );
	}

	static void heatOver( final int cell, final int embers, final int rays ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> heatOver( cell, embers, rays ) );
			return;
		}
		if (!seen( cell )) return;
		float x = x( cell ), y = y( cell );
		FxRecipes.embers( x, y, Element.FIRE, embers );
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		for (int i = 0; i < rays; i++) HeatRay.rise( glow, x + FxRandom.Float( -6f, 6f ), y + FxRandom.Float( 0f, 5f ) );
	}

	private static boolean seen( int cell ){
		return Dungeon.level != null && cell >= 0 && cell < Dungeon.level.length() && Dungeon.level.heroFOV[cell];
	}

	private static float x( int cell ){
		return (cell % Dungeon.level.width() + 0.5f) * DungeonTilemap.SIZE;
	}

	private static float y( int cell ){
		return (cell / Dungeon.level.width() + 0.5f) * DungeonTilemap.SIZE;
	}
}
