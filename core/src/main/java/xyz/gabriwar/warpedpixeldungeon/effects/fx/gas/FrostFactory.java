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
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherBlobFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;

/**
 * A freezing cloud (the Freezing blob) as the hero sees it, once a tick for each cell: rime spreading
 * over the cell from its middle in 0.3 s and lying there till the frost lets go (CellOverlay.RIME,
 * faded as the cell thaws), flakes drifting down through it (SnowParticle) with a winking crystal
 * every CRYSTAL_EVERY, a cold mist hugging the floor (about one wisp for every two cells a second)
 * and now and then a glint on the ice over the night's tint (at most one a cell every 1.5 s).
 * About ten pictures a cell and one rime, where today's frost poured some 25.
 */
public class FrostFactory extends Emitter.Factory implements FxBudget.Prioritized, BlobEmitter.Aftermath, BlobEmitter.Held {

	/** The one factory of the freezing cloud. */
	public static final FrostFactory FREEZING = new FrostFactory();

	/** The freezing cloud's pour, s (it was 0.05). */
	public static final float POUR = 0.12f;
	/** Every this many particles a crystal instead of a flake. */
	public static final int CRYSTAL_EVERY = 5;
	/** Rime: its colour, alpha, how long it grows, holds past its last tick and fades. */
	public static final int RIME_COLOR = 0xE8F6FF;
	public static final float RIME_ALPHA = 0.6f, RIME_GROW = 0.3f, RIME_HOLD = 0.6f, RIME_FADE = 0.8f;
	/** The cold mist: its colour, alpha, and how many wisps a cell a second. */
	public static final int MIST_COLOR = 0xC8DCF0;
	public static final float MIST_ALPHA = 0.15f, MIST_RATE = 0.5f;
	/** A glint at most every this many seconds a cell. */
	public static final float GLINT_EVERY = 1.5f;

	private FrostFactory(){}

	@Override
	public int priority(){
		return FxBudget.P1;
	}

	//every cell's rime is held (held()), outside the thinning; a thin cell's flakes thin out
	@Override
	public void held( int cell, int cur ){
		rime( cell );
	}

	@Override
	public void emit( Emitter e, int index, float x, float y ){
		if (!(e instanceof BlobEmitter)) rime( Fx.cellAt( x, y ) );

		if (FxRandom.Float() < POUR * MIST_RATE) mist( e, x, y );
		if (FxRandom.Float() < POUR / GLINT_EVERY) glint( x, y );

		if (index % CRYSTAL_EVERY == 0) WeatherBlobFX.FROST.emit( e, index, x, y );
		else SnowParticle.FACTORY.emit( e, index, x, y );
	}

	/** Rime on a cell, grown from its middle the first time, held while the frost holds. */
	public static void rime( int cell ){
		if (Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) return;
		int w = Dungeon.level.width();
		float cx = (cell % w + 0.5f) * DungeonTilemap.SIZE, cy = (cell / w + 0.5f) * DungeonTilemap.SIZE;
		int v = ((cell % w) * 3 + (cell / w) * 5) % FxFrames.RIME.length;
		//nudged off the cell's middle by the cell itself (a field of rime, not a grid of it)
		int h = (cell * 73) ^ (cell >> 3) * 31;
		float jx = Math.floorMod( h, 7 ) - 3, jy = Math.floorMod( h >> 3, 5 ) - 2;
		CellOverlay o = CellOverlay.keep( CellOverlay.RIME, cell, cx + jx, cy + 3 + jy, FxFrames.RIME[v], false, RIME_COLOR,
				RIME_ALPHA, 0f, RIME_HOLD, RIME_FADE, ((cell / w) & 1) == 1 );
		if (o != null && o.isNew()) o.revealCrop( CellOverlay.FROM_MIDDLE, RIME_GROW );
	}

	//a wisp of cold mist low on the floor, drifting sideways
	private static void mist( Emitter e, float x, float y ){
		if (!FxBudget.keep( FxBudget.P3, x, y )) return;
		float floor = (float)Math.floor( y / DungeonTilemap.SIZE ) * DungeonTilemap.SIZE + DungeonTilemap.SIZE - 3;
		float life = FxRandom.Float( 2f, 3f );
		FxParticle p = ((FxParticle) e.recycle( FxParticle.class )).reset( x, floor );
		p.look( FxFrames.EDGE_WISP[FxRandom.Int( FxFrames.EDGE_WISP.length )] ).paint( MIST_COLOR ).smooth()
				.life( life ).alpha( MIST_ALPHA, life * 0.35f, life * 0.45f ).vel( FxRandom.Float( 1.5f, 3f ) * FxRandom.sign(), 0 )
				.turbulence( 1f, 2f ).wind( 0.15f ).priority( FxBudget.P3 );
		if (p.vx < 0) p.scale.x = -1;
	}

	//a glint on the ice, over the night's tint
	private static void glint( float x, float y ){
		FxEmitter glow = Fx.glowAir();
		if (glow == null || !FxBudget.keep( FxBudget.P3, x, y )) return;
		((FxParticle) glow.recycle( FxParticle.class )).reset( Math.round( x ), Math.round( y ) )
				.look( FxFrames.PLUS_3 ).paint( RIME_COLOR ).light( true ).life( 0.3f ).alpha( 0.8f, 0.05f, 0.2f )
				.source( x, y ).snap().priority( FxBudget.P3 );
	}

	/** A cell thawed: its rime fades now. */
	@Override
	public void dying( int cell ){
		CellOverlay.release( CellOverlay.RIME, cell );
	}
}
