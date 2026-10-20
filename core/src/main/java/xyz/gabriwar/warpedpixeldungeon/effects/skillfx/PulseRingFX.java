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

package xyz.gabriwar.warpedpixeldungeon.effects.skillfx;

import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * A prayer's pulse, a purge, a halo's beat: two rings of light spreading on the ground about a
 * point or a sprite (the painted 2:1 rings, the second 0.08 s behind and a size smaller), a pale
 * of the school's colour at first (never pure white: a pulse is no white sonar ping before its
 * colour arrives; FIRST_PALE of the way to white) turning its colour as they go; a pale core
 * (CORE_PALE of the way to white) for their first 0.08 s, a
 * pool of the colour under them, three to six motes lifting off the rim, and on the water inside
 * the reach a small ring where the pulse passes (at most WATER_RINGS). On the floor, under
 * everyone; its light in the light layer. 2 rings + at most 6 motes.
 */
public class PulseRingFX extends Gizmo {

	public static final float STAGGER = 0.08f, CORE_FOR = 0.08f;
	/** How far toward white the first ring starts and the core shows: never pure white. */
	public static final float FIRST_PALE = 0.3f, CORE_PALE = 0.5f;
	public static final int FROM_W = 9, WATER_RINGS = 8, MOTES_MIN = 3, MOTES_MAX = 6;

	private final Visual follow;
	private float x, y;
	private final int color, toW, motes;
	private final float duration, radius;
	private final Ramp tone;
	private FxRing first, second;
	private float time, lastX, lastY, firstP, secondP;
	private int motesLeft;
	//the water cells the pulse will pass: their middles and how far from its own
	private final float[] wx = new float[WATER_RINGS], wy = new float[WATER_RINGS], wd = new float[WATER_RINGS];
	private int waters;
	private boolean started;

	private PulseRingFX( Visual follow, float x, float y, int color, float radius, float duration ){
		this.follow = follow;
		this.x = x;
		this.y = y;
		this.color = color;
		this.radius = radius;
		this.duration = Math.max( 0.2f, duration );
		toW = ringWidth( radius );
		motes = moteCount( radius );
		motesLeft = motes;
		tone = new Ramp( Ramp.lerp( color, 0xFFFFFF, FIRST_PALE ), color );
	}

	/** The widest painted ring a pulse of this radius spreads to (the ground's 2:1 ellipse as wide
	 *  as the disc it replaces). */
	public static int ringWidth( float radius ){
		return FxFrames.RING_WIDTHS[FxFrames.ringIndex( Math.round( radius * 2f ) + 1 )];
	}

	/** Motes off the rim: one for every six px of reach, three to six. */
	public static int moteCount( float radius ){
		return Math.max( MOTES_MIN, Math.min( MOTES_MAX, Math.round( radius / 6f ) ) );
	}

	public static void around( Visual sprite, int color, float radius, float duration ){
		if (sprite == null) return;
		final PulseRingFX fx = new PulseRingFX( sprite, 0, 0, color, radius, duration );
		Fx.post( () -> {
			if (!sprite.exists || sprite.parent == null) return;
			fx.start();
		} );
	}

	public static void at( int cell, int color, float radius, float duration ){
		if (Dungeon.hero == null || Dungeon.hero.sprite == null || !SkillVfx.valid( cell )) return;
		final PulseRingFX fx = new PulseRingFX( null, SkillVfx.cx( cell ), SkillVfx.cy( cell ) + 2, color, radius, duration );
		Fx.post( fx::start );
	}

	private void start(){
		Group floor = GameScene.layer( GameScene.Layer.FLOOR );
		if (floor == null) return;
		where();
		lastX = x;
		lastY = y;
		started = true;
		floor.add( this );
		if (!Fx.seen( x, y )) return;
		first = FxRing.ground( floor, x, y, FROM_W, toW, duration, tone.stop( 0 ), 0.85f, true );
		int secondW = FxFrames.RING_WIDTHS[Math.max( 0, FxFrames.ringIndex( toW ) - 1 )];
		second = FxRing.ground( floor, x, y, FROM_W, secondW, duration, color, 0.6f, true );
		if (second != null) second.delay( STAGGER );
		FxLight.glow( x, y - 4, 11, Ramp.lerp( color, 0xFFFFFF, CORE_PALE ), 1f ).core().life( 0, CORE_FOR * 0.5f, CORE_FOR * 0.5f );
		FxLight.pool( x, y, 31, color, 0.57f ).life( 0.04f, duration * 0.3f, duration * 0.7f );
		findWater();
	}

	//where its middle is now: on the sprite's feet's ellipse, or its cell
	private void where(){
		if (follow instanceof CharSprite){
			CharSprite s = (CharSprite) follow;
			x = SkillVfx.midX( s );
			y = s.y + s.height() - 3;
		} else if (follow != null){
			x = follow.x + follow.width() / 2f;
			y = follow.y + follow.height() - 3;
		}
	}

	private void findWater(){
		Level l = Dungeon.level;
		if (l == null) return;
		int reach = (int) Math.ceil( radius / 16f );
		int w = l.width();
		int c0 = Fx.cellAt( x, y );
		if (c0 < 0) return;
		for (int dy = -reach; dy <= reach && waters < WATER_RINGS; dy++){
			for (int dx = -reach; dx <= reach && waters < WATER_RINGS; dx++){
				int cx = c0 % w + dx, cy = c0 / w + dy;
				if (cx < 0 || cy < 0 || cx >= w || cy >= l.height()) continue;
				float px = (cx + 0.5f) * 16, py = (cy + 0.5f) * 16;
				float ex = (px - x) / radius, ey = (py - y) / (radius * 0.5f);
				if (ex * ex + ey * ey > 1.2f) continue;
				if (!WaterFX.onWater( px, py ) || !Fx.seen( px, py )) continue;
				wx[waters] = px;
				wy[waters] = py;
				wd[waters] = (float) Math.sqrt( ex * ex + ey * ey );
				waters++;
			}
		}
	}

	@Override
	public void update(){
		super.update();
		if (!started) return;
		time += Game.elapsed;
		//a ring the cap took and someone else's since: let go of
		first = mine( first, firstP );
		second = mine( second, secondP );
		if (first != null) firstP = first.progress();
		if (second != null) secondP = second.progress();
		if (follow == null && first != null){
			//over a cell: its ring takes in any slide of the window
			x = first.centerX();
			y = first.centerY();
		}
		if (follow != null){
			if (!follow.exists){
				Fx.dispose( this );
				return;
			}
			where();
			float dx = x - lastX, dy = y - lastY;
			if (dx != 0 || dy != 0){
				nudge( first, dx, dy );
				nudge( second, dx, dy );
				lastX = x;
				lastY = y;
			}
		}
		float p = Math.min( 1f, time / duration );
		//pale at first, the school's colour by its middle
		if (first != null) SkillVfx.lit( first, tone.at( Math.min( 1f, p * 2.2f ) ) );
		//the rim's motes lift off as it passes them, between a sixth and a half of its life
		float reachNow = toW / 2f * (1f - (1f - p) * (1f - p));
		if (motesLeft > 0 && p > 0.15f){
			float due = motes * Math.min( 1f, (p - 0.15f) / 0.35f );
			while (motes - motesLeft < due && motesLeft > 0){
				motesLeft--;
				float a = FxRandom.angle();
				float mx = x + (float) Math.cos( a ) * reachNow, my = y + (float) Math.sin( a ) * reachNow * 0.5f;
				FxParticle m = SkillVfx.glow( mx, my, FxBudget.P2 );
				if (m != null){
					m.look( FxRandom.chance( 0.5f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).paint( tone.at( 0.5f + FxRandom.Float( 0.5f ) ) )
							.vel( FxRandom.Float( -3f, 3f ), FxRandom.Float( -22f, -14f ) ).drag( 1.5f ).life( 0.5f )
							.alpha( 0.9f, 0.05f, 0.3f ).source( mx, my );
				}
			}
		}
		for (int i = 0; i < waters; i++){
			if (wd[i] >= 0 && reachNow * 2f / toW >= wd[i]){
				WaterFX.ring( wx[i], wy[i], WaterFX.S );
				wd[i] = -1;
			}
		}
		if (time >= duration + STAGGER) Fx.dispose( this );
	}

	//the ring, while it is still this pulse's: alive, and never further back than it was
	private static FxRing mine( FxRing r, float lastP ){
		if (r == null || !r.alive || r.progress() < lastP || lastP >= 1f) return null;
		return r;
	}

	private static void nudge( FxRing r, float dx, float dy ){
		if (r == null) return;
		r.x += dx;
		r.y += dy;
	}
}
