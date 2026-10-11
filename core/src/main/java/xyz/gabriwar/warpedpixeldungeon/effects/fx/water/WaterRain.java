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

import xyz.gabriwar.warpedpixeldungeon.effects.Ripple;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;

/**
 * The weather landing on the water (docs/fx/water-life.md, "Drops on water"): the rain's and the
 * sleet's drops ring the water in its surface slot - under the night's tint and the shore's lip,
 * no longer a ring of the weather's own drawn over everything - and a hailstone rings it harder
 * and throws a drop back up. The rain's rings are the water's small ones, a ring in three for a
 * drop (the weather particles decide), never more at once than twice the water cells in view
 * and 24 at most (WaterGeom.rainRings); past 0.4 the pocks lying over the whole water carry the
 * rest (WaterLive). Render thread (the weather's particles land there).
 */
public final class WaterRain {

	private WaterRain(){}

	/** A rain ring's alpha, and the sleet's share of it; lifted by a quarter at night (under the
	 *  night's tint), never past 1. */
	public static final float RING_ALPHA = 0.8f, SLEET_SHARE = 0.6f, NIGHT_LIFT = 0.25f;

	//when the rain's last rings were made (the scene's clock): a ring lives as long as a small one
	private static final float NEVER = -1000f;
	private static final float[] made = new float[24];
	private static int next;

	static {
		reset();
	}

	/** The rain's rings spreading now, as the clock reads them. */
	static int spreading(){
		float now = Game.timeTotal, life = WaterFX.lifeOf( WaterFX.S );
		int n = 0;
		for (float t : made) if (now - t < life && now >= t) n++;
		return n;
	}

	/** A raindrop (or sleet: `sleet`) landing on the water at (x, y): a small ring, while the rain's
	 *  rings in view leave room for it. False when it made none. */
	public static boolean drop( float x, float y, boolean sleet ){
		if (!WaterFX.onWater( x, y )) return false;
		if (spreading() >= WaterGeom.rainRings( WaterLive.waterInView() )) return false;
		Element.Liquid l = WaterFX.liquid();
		//the fish do not take fright at the rain
		float a = Math.min( 1f, RING_ALPHA * (sleet ? SLEET_SHARE : 1f) * WaterGeom.nightLift( GameScene.nightTintAlpha(), NIGHT_LIFT ) );
		Ripple r = WaterFX.quietRing( x, y, WaterFX.S, l.foam, a, FxBudget.P2 );
		if (r == null) return false;
		made[next] = Game.timeTotal;
		next = (next + 1) % made.length;
		return true;
	}

	/** A hailstone into the water: a middling ring, and a drop thrown back up 2 px for 0.15 s. */
	public static void hail( float x, float y ){
		if (!WaterFX.onWater( x, y )) return;
		Element.Liquid l = WaterFX.liquid();
		WaterFX.ring( x, y, WaterFX.M, l.foam, WaterFX.alphaOf( WaterFX.M ), FxBudget.P2 );
		FxEmitter air = Fx.air();
		if (air == null || !FxBudget.keep( FxBudget.P3, x, y )) return;
		((FxParticle) air.recycle( FxParticle.class )).reset( x, y - 1 ).look( FxFrames.DROP_2 ).paint( l.foam )
				.vel( 0, -53f ).gravity( 710f ).life( 0.15f ).alpha( 0.9f, 0, 0.05f ).snap().priority( FxBudget.P3 );
	}

	/** A new scene: the last one's rings forgotten (its clock starts again). */
	public static void reset(){
		for (int i = 0; i < made.length; i++) made[i] = NEVER;
		next = 0;
	}
}
