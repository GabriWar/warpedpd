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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.water;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;

/**
 * What drops into the water (docs/fx/water-life.md, "Drops on water"): a drop from the roof
 * that gathers as a bead before it lets go and crowns the water where it lands, an item dropped
 * into it, a fish's lunge breaking out of it, something arriving in it at a stroke. Render thread
 * (handed there from any other).
 *
 *  - A roof's drop (DripParticle.FALL): a bead on the roof over its landing point for 0.2 s
 *    (DROP_2 then DROP_3), then the drop falling as it always did (18 px/s, 140 px/s^2, landing on
 *    its point in 0.45-0.6 s), and where it lands a crown and a middling ring - a plink thrown up
 *    2.5 px one time in four - or, on dry ground, a wet mark and two tiny droplets of its colour.
 *  - An item into the water: a middling ring (and its second) and two or three droplets; a fish
 *    within 24 px is startled, one within 40 comes to look (it took it for food).
 *  - A lunge (a piranha's, a squid's bite): the small crown, four droplets flung toward what it
 *    bit, a middling ring.
 *  - An arrival at a stroke (a crab king's, Tengu's, Zot's blink into the water): a middling ring
 *    and three droplets.
 */
public final class WaterDrops {

	private WaterDrops(){}

	/** The bead's while on the roof, the plink's share. */
	public static final float BEAD = 0.2f, PLINK_SHARE = 0.25f;
	/** The drop's fall, as the old drip's: its first speed and its gravity. */
	static final float FALL_V = 18f, FALL_G = 140f;

	/** How high over its landing point a drop lets go that lands in `t` s. */
	public static float fallHeight( float t ){
		return FALL_V * t + FALL_G / 2f * t * t;
	}

	/** A drop from the roof onto (x, y), in `color`, its particles in the emitter (a factory's). */
	public static void fall( Emitter e, float x, float y, int color ){
		if (!FxBudget.keep( FxBudget.P2, x, y )) return;
		float t = FxRandom.Float( 0.45f, 0.6f );
		Bead b = (Bead) e.recycle( Bead.class );
		b.reset( x, y - fallHeight( t ) );
		b.look( BEAD_FRAMES, 0 ).paint( color ).life( BEAD ).alpha( 0.9f, BEAD * 0.5f, 0f ).snap()
				.source( x, y ).priority( FxBudget.P2 );
		b.landY = y;
		b.fallFor = t;
		b.color = color;
		b.pending = true;
	}

	private static final int[][] BEAD_FRAMES = { FxFrames.DROP_2, FxFrames.DROP_3 };

	/** A drop gathering on the roof: when its while is up it lets go (its fall made then). */
	public static class Bead extends FxParticle {
		float landY, fallFor;
		int color;
		boolean pending;

		@Override
		public void kill(){
			super.kill();
			if (!pending) return;
			pending = false;
			if (parent instanceof Emitter){
				FxParticle d = (FxParticle) ((Emitter) parent).recycle( FxParticle.class );
				d.reset( px, py ).look( FxFrames.DROP_3 ).paint( color ).vel( 0, FALL_V ).gravity( FALL_G )
						.ground( landY, FxParticle.DIE, 0 ).fate( LAND ).life( fallFor + 0.2f ).alpha( 0.9f, 0, 0f )
						.source( px, landY ).priority( FxBudget.P2 );
			}
		}
	}

	//where a roof's drop lands: on the water a crown and a middling ring (a plink a time in four),
	//elsewhere a wet mark and two tiny droplets of its own colour
	static final FxParticle.Fate LAND = ( p, landed ) -> {
		if (!landed) return;
		float x = p.px, y = p.py;
		if (WaterFX.onWater( x, y )){
			Element.Liquid l = WaterFX.liquid();
			WaterFX.crown( x, y, WaterFX.S, l.foam );
			WaterFX.quietRing( x, y, WaterFX.M, l.foam, WaterFX.alphaOf( WaterFX.M ), FxBudget.P2 );
			if (FxRandom.chance( PLINK_SHARE )) plink( x, y, l.foam );
			//a fish takes a drop for a fly landing: it comes to look
			WaterFX.disturb( x, y, WaterFX.DISTURB[WaterFX.S], true );
		} else {
			FxDecal.wet( x, y );
			WaterFX.droplets( x, y - 1, 2, 15f, 30f, 120f, p.colorNow() );
		}
	};

	/** A small drop into the water at (x, y) (a roof's, a tree's): a small ring that stirs the fish
	 *  only as food does, a plink a time in four. Render thread. */
	public static void drip( float x, float y ){
		if (!WaterFX.onWater( x, y )) return;
		Element.Liquid l = WaterFX.liquid();
		WaterFX.quietRing( x, y, WaterFX.S, l.foam, WaterFX.alphaOf( WaterFX.S ), FxBudget.P2 );
		if (FxRandom.chance( PLINK_SHARE )) plink( x, y, l.foam );
		WaterFX.disturb( x, y, WaterFX.DISTURB[WaterFX.S], true );
	}

	/** A drop thrown back up off the water by one landing: 1x3, 2-3 px up, 0.12 s. */
	static void plink( float x, float y, int color ){
		FxEmitter air = Fx.air();
		if (air == null || !FxBudget.keep( FxBudget.P3, x, y )) return;
		((FxParticle) air.recycle( FxParticle.class )).reset( x, y - 1 ).look( FxFrames.PLINK ).paint( color )
				.vel( 0, -83f ).gravity( 1390f ).life( 0.12f ).alpha( 0.85f, 0, 0.04f ).snap().priority( FxBudget.P3 );
	}

	/** An item into the water at (x, y): a middling ring (and its second), two or three droplets;
	 *  a fish within ITEM_STARTLES px is startled, one within 40 comes to look (it took it for food).
	 *  Any thread. */
	public static void item( final float x, final float y ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> item( x, y ) );
			return;
		}
		if (!WaterFX.onWater( x, y )) return;
		Element.Liquid l = WaterFX.liquid();
		WaterFX.quietRing( x, y, WaterFX.M, l.foam, WaterFX.alphaOf( WaterFX.M ), FxBudget.P1 );
		if (!l.ringAdditive) WaterFX.droplets( x, y - 1, FxRandom.IntRange( 2, 3 ), 25f, 50f, 90f, l.foam );
		WaterFX.disturb( x, y, ITEM_STARTLES, false );
		WaterFX.disturb( x, y, WaterFX.DISTURB[WaterFX.M], true );
	}

	/** An item landing this near a fish startles it; farther, within 40 px, it draws it. */
	public static final float ITEM_STARTLES = 24f;

	/** A lunge out of the water at (x, y) toward (tx, ty): the small crown, four droplets flung
	 *  toward it, a middling ring. Any thread. */
	public static void lunge( final float x, final float y, final float tx, final float ty ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> lunge( x, y, tx, ty ) );
			return;
		}
		if (!WaterFX.onWater( x, y )) return;
		Element.Liquid l = WaterFX.liquid();
		WaterFX.ring( x, y, WaterFX.M, l.foam, WaterFX.alphaOf( WaterFX.M ), FxBudget.P1 );
		if (l.ringAdditive){
			FxRecipes.sparks( x, y - 2, Element.LAVA, 3 );
			return;
		}
		WaterFX.crown( x, y, WaterFX.S, l.foam );
		//up, and leaning toward what it bit
		float lean = Math.abs( tx - x ) < 1f ? 0f : Math.signum( tx - x ) * 35f;
		WaterFX.droplets( x, y - 1, 4, 35f, 60f, 70f, l.foam, -90f + lean );
	}

	/** A creature's lunge out of the water from its cell at `cell` toward the cell it bit (-1 for
	 *  none known): as lunge(), from the middle of its cell, a little below. Any thread. */
	public static void lunge( int cell, int bitten ){
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length()) return;
		int w = level.width();
		float x = (cell % w + 0.5f) * DungeonTilemap.SIZE, y = (cell / w + 0.5f) * DungeonTilemap.SIZE + 3;
		float tx = bitten >= 0 && bitten < level.length() ? (bitten % w + 0.5f) * DungeonTilemap.SIZE : x;
		lunge( x, y, tx, y );
	}

	/** Something arriving in the water at a stroke on a cell (a blink): as arrive(), at its middle,
	 *  a little below. Any thread. */
	public static void arrive( int cell ){
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length()) return;
		int w = level.width();
		arrive( (cell % w + 0.5f) * DungeonTilemap.SIZE, (cell / w + 0.5f) * DungeonTilemap.SIZE + 3 );
	}

	/** Something arriving in the water at a stroke at (x, y): a middling ring and three droplets.
	 *  Any thread. */
	public static void arrive( final float x, final float y ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> arrive( x, y ) );
			return;
		}
		if (!WaterFX.onWater( x, y )) return;
		Element.Liquid l = WaterFX.liquid();
		WaterFX.ring( x, y, WaterFX.M, l.foam, WaterFX.alphaOf( WaterFX.M ), FxBudget.P1 );
		if (!l.ringAdditive) WaterFX.droplets( x, y - 1, 3, 30f, 55f, 100f, l.foam );
	}
}
