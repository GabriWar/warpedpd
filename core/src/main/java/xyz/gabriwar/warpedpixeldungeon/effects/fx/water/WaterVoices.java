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
import xyz.gabriwar.warpedpixeldungeon.effects.CritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSound;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Place;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * Pictures for the places' water voices (levels/ambience/AmbientSounds; docs/fx/water-life.md,
 * "Ambient voices"): what is heard in sight is seen too. Called right after a voice from a cell has
 * been sent to the ambience channel - the sound untouched, none of the game's random numbers drawn -
 * and only for a cell the hero sees, on screen.
 *
 *  - DRIP: a drop falling 10-14 px in 0.15 s, a small crown and a small ring where it lands in
 *    the water; on dry floor a wet mark.
 *  - LAP: that bank's foam at once (WaterAmbience.lapAt).
 *  - SPLASH (the shore's): where fish live (the shore, the caves, the ice), one of them leaping
 *    from the water there (CritterSprite.Fish: no sound of its own); else a large ring and five
 *    droplets thrown up.
 *  - GURGLE (a drain): two to four bubbles rising and popping into small rings over 0.6 s, in the
 *    water at the drain (under a pipe, its pool).
 *  - TRICKLE from open water: in the caves a stream - foam dashes drifting 8 px/s one way over its
 *    pool; in the city a fountain - three small rings 1.5 s apart.
 * The later beats wait in a small queue (QUEUE) run by the live surface's update.
 */
public final class WaterVoices {

	private WaterVoices(){}

	private static final float SIZE = DungeonTilemap.SIZE;

	/** Beats waiting at once. */
	static final int QUEUE = 24;
	static final int BUBBLE = 0, FOUNTAIN = 1, DASH = 2;
	/** The roof's drop of a DRIP: its fall, px and s. */
	static final float DRIP_FROM = 10f, DRIP_TO = 14f, DRIP_FALL = 0.15f;
	static final float GURGLE_OVER = 0.6f, FOUNTAIN_EVERY = 1.5f, DASH_SPEED = 8f, DASH_LIFE = 2.2f;

	private static final float[] at = new float[QUEUE], qx = new float[QUEUE], qy = new float[QUEUE], qv = new float[QUEUE];
	private static final int[] kinds = new int[QUEUE];
	private static int queued;
	private static float clock;

	/** A new scene: nothing waiting. */
	static void reset(){
		queued = 0;
		clock = 0f;
	}

	/**
	 * A voice of a place was heard from a cell (render thread, the ambience's own update): its
	 * picture there, if the hero sees it on screen.
	 */
	public static void played( AmbientSound sound, int cell, Place place ){
		Level level = Dungeon.level;
		if (sound == null || level == null || cell < 0 || cell >= level.length() || level.heroFOV == null
				|| level.fogHeld()) return;
		int w = level.width();
		float x = (cell % w + 0.5f) * SIZE, y = (cell / w + 0.5f) * SIZE;
		if (!level.heroFOV[cell] || !Fx.onScreen( x, y, 0 )) return;
		switch (sound){
			case DRIP:
				drip( x + FxRandom.Float( -5f, 5f ), y + FxRandom.Float( -3f, 5f ) );
				break;
			case LAP:
				WaterLive.lap( cell );
				break;
			case SPLASH:
				//where fish live, one of them leaping; else something breaking the water
				if (place == Place.SHORE || place == Place.CAVES || place == Place.FROZEN) leap( level, cell );
				else splash( x, y + 2 );
				break;
			case GURGLE:
				gurgle( level, cell );
				break;
			case TRICKLE:
				if (!level.water[cell]) return;
				if (place == Place.CAVES) stream( level, cell );
				else if (place == Place.CITY || place == Place.VAULT) fountain( x, y );
				break;
			default:
		}
	}

	//a drop off the roof onto (x, y)
	private static void drip( float x, float y ){
		FxEmitter air = Fx.air();
		if (air == null || !FxBudget.keep( FxBudget.P2, x, y )) return;
		float h = FxRandom.Float( DRIP_FROM, DRIP_TO );
		((FxParticle) air.recycle( FxParticle.class )).reset( x, y - h ).look( FxFrames.DROP_3 ).paint( 0x9CB8DC )
				.gravity( 2f * h / (DRIP_FALL * DRIP_FALL) ).ground( y, FxParticle.DIE, 0 ).fate( DRIP_LANDS )
				.life( DRIP_FALL + 0.1f ).alpha( 0.9f, 0, 0f ).priority( FxBudget.P2 );
	}

	static final FxParticle.Fate DRIP_LANDS = ( p, landed ) -> {
		if (!landed) return;
		if (WaterFX.onWater( p.px, p.py )){
			WaterFX.crown( p.px, p.py, WaterFX.S, WaterFX.liquid().foam );
			WaterDrops.drip( p.px, p.py );
		} else {
			FxDecal.wet( p.px, p.py );
		}
	};

	//a fish leaping from the water at a cell, across it, never toward the bank
	private static void leap( Level level, int cell ){
		if (!level.water[cell]){
			int w = level.width();
			float x = (cell % w + 0.5f) * SIZE, y = (cell / w + 0.5f) * SIZE;
			splash( x, y + 2 );
			return;
		}
		int w = level.width();
		boolean left = cell % w > 0 && level.water[cell - 1], right = cell % w < w - 1 && level.water[cell + 1];
		int dir = left && right ? (FxRandom.chance( 0.5f ) ? 1 : -1) : right ? 1 : -1;
		GameScene.effect( new CritterSprite.Fish( FxRandom.chance( 0.4f ),
				(cell % w) * SIZE + 8 + FxRandom.Float( -3f, 3f ), (cell / w) * SIZE + 10, dir ) );
	}

	//something broke the shore's open water: a large ring and five droplets
	private static void splash( float x, float y ){
		if (!WaterFX.onWater( x, y )) return;
		Element.Liquid l = WaterFX.liquid();
		WaterFX.ring( x, y, WaterFX.L, l.foam, WaterFX.alphaOf( WaterFX.L ), FxBudget.P2 );
		WaterFX.droplets( x, y - 1, 5, 35f, 65f, 100f, l.foam );
	}

	//air working up through a drain: bubbles in the water at it over 0.6 s
	private static void gurgle( Level level, int cell ){
		int w = level.width();
		int at = level.water[cell] ? cell : (cell + w < level.length() && level.water[cell + w] ? cell + w : -1);
		if (at < 0) return;
		float x = (at % w + 0.5f) * SIZE, y = (at / w) * SIZE + 6;
		int n = FxRandom.IntRange( 2, 4 );
		for (int i = 0; i < n; i++){
			queue( BUBBLE, i * GURGLE_OVER / n, x + FxRandom.Float( -4f, 4f ), y + FxRandom.Float( 0f, 6f ), 0f );
		}
	}

	//a fountain's water falling into its basin: three small rings, 1.5 s apart
	private static void fountain( float x, float y ){
		for (int i = 0; i < 3; i++) queue( FOUNTAIN, i * FOUNTAIN_EVERY, x + FxRandom.Float( -4f, 4f ), y + FxRandom.Float( -2f, 4f ), 0f );
	}

	//a stream through a cave pool: foam dashes drifting one way (each pool's own) at 8 px/s
	private static void stream( Level level, int cell ){
		int w = level.width();
		float x = (cell % w) * SIZE, y = (cell / w) * SIZE;
		float dir = ((cell * 0x9E3779B1) >>> 31) == 0 ? 1f : -1f;
		for (int i = 0; i < 4; i++){
			queue( DASH, i * 0.6f, x + FxRandom.Float( 2f, 14f ), y + FxRandom.Float( 4f, 13f ), dir * DASH_SPEED );
		}
	}

	private static void queue( int kind, float after, float x, float y, float v ){
		if (queued >= QUEUE) return;
		at[queued] = clock + after;
		qx[queued] = x;
		qy[queued] = y;
		qv[queued] = v;
		kinds[queued] = kind;
		queued++;
	}

	/** Every frame (the live surface's update): the beats that are due. */
	static void update( float dt ){
		clock += dt;
		for (int i = 0; i < queued; i++){
			if (at[i] > clock) continue;
			beat( kinds[i], qx[i], qy[i], qv[i] );
			queued--;
			at[i] = at[queued];
			qx[i] = qx[queued];
			qy[i] = qy[queued];
			qv[i] = qv[queued];
			kinds[i] = kinds[queued];
			i--;
		}
	}

	private static void beat( int kind, float x, float y, float v ){
		if (!WaterFX.onWater( x, y )) return;
		Element.Liquid l = WaterFX.liquid();
		switch (kind){
			case BUBBLE:
				WaterAmbience.bubble( x, y, l.foam );
				break;
			case FOUNTAIN:
				WaterFX.quietRing( x, y, WaterFX.S, l.foam, WaterFX.alphaOf( WaterFX.S ), FxBudget.P3 );
				break;
			default:
				if (!FxBudget.keep( FxBudget.P3, x, y )) return;
				Marks.Mark m = Marks.make( Marks.FOAM, false, x, y );
				if (m != null) m.look( FxFrames.WHITECAP[2] ).paint( l.foam ).life( DASH_LIFE ).alpha( 0.5f, 0.4f, 0.6f )
						.vel( v, 0 ).smooth().priority( FxBudget.P3 );
		}
	}

	/** Beats waiting (tests). */
	static int queued(){
		return queued;
	}
}
