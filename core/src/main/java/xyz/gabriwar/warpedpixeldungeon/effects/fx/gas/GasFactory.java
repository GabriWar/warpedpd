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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.gas;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;

/**
 * A gas cloud's factory (one per GasStyle, the same object every time): what its blob's emitter
 * makes, once a tick, for each cell the hero sees.
 *
 * - its bodies (GasPuff), drifting down the cloud's volume; at the cloud's edge now and then a
 *   wisp curling outward instead;
 * - every `every`th particle its detail (bubbles, motes, drips, flies, soot...);
 * - welling up (a new cloud, or one grown by half in a tick): 6-10 bodies thrown out of its
 *   thickest cell over 0.4 s, as its hiss starts;
 * - walked through: two bodies pushed along the walker's step (the emitter's kicks, at most 6 a
 *   check);
 * - a luminous gas's glow over the night's tint (GasGlow), a thick toxic or corrosive cell's tint
 *   on the floor, a corrosive cloud's etching (CellOverlay), a paralytic cloud's static ticks and
 *   an ice storm's glints.
 *
 * Its bodies are the cloud's body (P1); everything else is thinned as the kit's budget says. A
 * thin cell emits less (the kit's blobDensity), as before, and none emits while the gases' share of
 * the budget is full (GasPuff.cap); the floor's tint and etching are held on
 * every cell the hero sees (BlobEmitter.Held), outside that thinning. Nothing here draws the game's
 * random numbers.
 */
public class GasFactory extends Emitter.Factory
		implements FxBudget.Prioritized, BlobEmitter.Bloomable, BlobEmitter.Kickable, BlobEmitter.Held {

	/** A welling up's bodies, and how fast and hard they are thrown and slowed. */
	public static final int BLOOM_MIN = 6, BLOOM_MAX = 10;
	public static final float BLOOM_SPEED_MIN = 40f, BLOOM_SPEED_MAX = 60f, BLOOM_DRAG = 5f;
	/** A step through it: bodies pushed along it, at this speed, slowing this hard. */
	public static final int KICK_BODIES = 2;
	public static final float KICK_SPEED_MIN = 14f, KICK_SPEED_MAX = 20f, KICK_DRAG = 3f;
	/** The share of a cloud's edge cells' bodies that are wisps instead. */
	public static final float EDGE_WISPS = 0.35f;
	/** Floor answers: a thick cell's tint, a corrosive one's etching. */
	public static final float TINT_ALPHA = 0.16f, ETCH_ALPHA = 0.45f;

	public final GasStyle style;

	//the emitter it last made for: where a kick's bodies go
	private Emitter last;
	//a welling up heard before the tick's first emit, made then
	private int bloomCell = -1;

	GasFactory( GasStyle style ){
		this.style = style;
	}

	@Override
	public boolean lightMode(){
		return style.light;
	}

	@Override
	public int priority(){
		return FxBudget.P1;
	}

	//the gases' share of the budget full: no cell emits (and none takes the frame's spawns) until
	//some bodies die; the floor's answers are held all the same
	@Override
	public float density( int cur ){
		return GasPuff.room() ? FxBudget.blobDensity( cur ) : 0f;
	}

	//every cell's floor answers are held (held()), outside the thinning
	@Override
	public void held( int cell, int cur ){
		floor( cell, cur );
	}

	@Override
	public void emit( Emitter e, int index, float x, float y ){
		last = e;
		BlobEmitter.Cell c = e instanceof BlobEmitter ? BlobEmitter.cell() : null;
		int cur = c != null ? c.cur : 100;
		int cell = c != null ? c.cell : Fx.cellAt( x, y );
		float dx = c != null ? c.dx : 0, dy = c != null ? c.dy : 0;
		boolean edge = c != null && c.edge;

		if (bloomCell >= 0){
			int b = bloomCell;
			bloomCell = -1;
			welling( e, b );
		}

		if (c == null) floor( cell, cur );
		answers( x, y, cell );

		if (style.detail != null && index % style.every == 0){
			style.detail.emit( e, index, x, y );
			return;
		}
		if (!GasPuff.room()) return;
		if (style.hug) y = cellTop( y ) + 8 + (y - cellTop( y )) / 2f;
		if (edge && FxRandom.Float() < EDGE_WISPS){
			float len = (float)Math.sqrt( dx * dx + dy * dy );
			float ox, oy;
			if (len > 0.01f){
				ox = dx / len;
				oy = dy / len;
			} else {
				float a = FxRandom.angle();
				ox = (float)Math.cos( a );
				oy = (float)Math.sin( a );
			}
			GasPuff.make( e, x, y ).wisp( style, cur, ox, oy );
			return;
		}
		GasPuff.make( e, x, y ).body( style, cur, dx, dy );
	}

	private static float cellTop( float y ){
		return (float)Math.floor( y / DungeonTilemap.SIZE ) * DungeonTilemap.SIZE;
	}

	//what the floor under the cloud shows: a thick cell's tint, a corrosive cloud's etching
	private void floor( int cell, int cur ){
		if (cell < 0 || Dungeon.level == null || cell >= Dungeon.level.length()) return;
		if (!style.tint && !style.etch) return;
		int w = Dungeon.level.width();
		float cx = (cell % w + 0.5f) * DungeonTilemap.SIZE, cy = (cell / w + 0.5f) * DungeonTilemap.SIZE;
		if (style.tint && cur >= GasStyle.THICK){
			CellOverlay.keep( CellOverlay.TINT, cell, cx, cy + 3, FxFrames.Light.POOL_23, true, style.under,
					TINT_ALPHA, 0.6f, 1f, 1f, false );
		}
		if (style.etch){
			CellOverlay o = CellOverlay.keep( CellOverlay.ETCH, cell, cx + ((cell * 7) % 5) - 2, cy + 3, FxFrames.ETCH, false,
					0x4A4640, ETCH_ALPHA, 1.5f, 2f, 2f, (cell & 1) == 1 );
			if (o != null && o.isNew()) o.revealCrop( CellOverlay.FROM_MIDDLE, 1.5f );
		}
	}

	//its light: a luminous gas's glow, a paralytic one's static ticks, an ice storm's glints
	private void answers( float x, float y, int cell ){
		if (style.glowLevel > 0 && FxRandom.Float() < style.pour / GasGlow.LIFE){
			GasGlow.at( x, y, style.glow, style.glowLevel );
		}
		if (style.glintEvery > 0 && FxRandom.Float() < style.pour / style.glintEvery){
			FxEmitter glow = Fx.glowAir();
			if (glow == null) return;
			if (style.tick){
				if (!FxBudget.keep( FxBudget.P3, x, y )) return;
				((FxParticle) glow.recycle( FxParticle.class )).reset( Math.round( x ), Math.round( y ) )
						.look( FxFrames.MOTE_1 ).paint( 0xFFFFFF ).light( true ).life( 1 / 30f ).alpha( 0.9f, 0, 0 )
						.source( x, y ).snap().priority( FxBudget.P3 );
			} else {
				FxFactories.glint( 0xFFFFFF ).emit( glow, cell, Math.round( x ), Math.round( y ) );
			}
		}
	}

	/** The cloud welling up at its thickest cell: heard during an emitter's tick, made at its
	 *  next emit (this one's). */
	@Override
	public void bloom( int cell ){
		bloomCell = cell;
	}

	//6-10 bodies thrown out of a cell's middle and slowing within 0.4 s
	private void welling( Emitter e, int cell ){
		if (Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) return;
		int w = Dungeon.level.width();
		float cx = (cell % w + 0.5f) * DungeonTilemap.SIZE, cy = (cell / w + 0.5f) * DungeonTilemap.SIZE;
		int n = FxRandom.IntRange( BLOOM_MIN, BLOOM_MAX );
		float a0 = FxRandom.angle();
		for (int i = 0; i < n && GasPuff.room(); i++){
			float a = a0 + (float)(Math.PI * 2) * i / n + FxRandom.Float( -0.3f, 0.3f );
			GasPuff.make( e, cx, cy ).body( style, 300, 0, 0 )
					.pushed( a, FxRandom.Float( BLOOM_SPEED_MIN, BLOOM_SPEED_MAX ), BLOOM_DRAG );
		}
	}

	/** Someone stepped into the cloud on `cell`, (dx, dy) px: two bodies pushed along his step. */
	@Override
	public void kick( int cell, float dx, float dy ){
		Emitter e = last;
		if (e == null || !e.exists || e.parent == null || Dungeon.level == null) return;
		if (cell < 0 || cell >= Dungeon.level.length()) return;
		int w = Dungeon.level.width();
		float cx = (cell % w + 0.5f) * DungeonTilemap.SIZE, cy = (cell / w + 0.5f) * DungeonTilemap.SIZE;
		float a = (float)Math.atan2( dy, dx );
		for (int i = 0; i < KICK_BODIES && GasPuff.room(); i++){
			GasPuff.make( e, cx + FxRandom.Float( -3f, 3f ), cy + FxRandom.Float( -2f, 4f ) ).body( style, 100, 0, 0 )
					.pushed( a + FxRandom.Float( -0.5f, 0.5f ), FxRandom.Float( KICK_SPEED_MIN, KICK_SPEED_MAX ), KICK_DRAG );
		}
	}
}
