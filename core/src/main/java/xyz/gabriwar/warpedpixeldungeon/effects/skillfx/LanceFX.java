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
import com.watabou.noosa.Group;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * A lance of light: a painted head (FxFrames.LANCE_HEAD_9) turned to its way, a 1 px shaft and a
 * 3 px halo of the light sheet behind it, a four-point glint riding its tip, and a glow and a pool
 * of light following the tip so it lights the lane at night. All of it at chest height, along the
 * lane, never rising out of the floor (a spike trap's read) nor lying on it (an item's).
 *
 * - thrust: Impaling Thrust's. Drawn from the hand (6 px out of the hero's middle toward the
 *   target), it shoots down the lane one cell every STEP s (its tip at the lane's cell k at
 *   STEP x (k - 1), with the lane's own timeline), pokes OVERSHOOT px out past the last body, holds
 *   HOLD s while a ring of spent force spreads on the floor at its end, then is pulled back over
 *   WITHDRAW s (easing in), fading to 0.3, two ghosts of its head trailing the pull.
 * - pierce: what each body it runs through shows, out of its back: a flare, a one-frame star,
 *   sparks of the lance's own colours and drops of the body's blood (honest: Bleeding is applied),
 *   in full for the first RICH bodies of a thrust.
 * - spectral: the +3's ghost thrust over one cell of the lane, in the spectral ramp: it stabs 10 px
 *   through the cell in SPECTRAL_STAB s, holds, and dissolves into motes. It keeps to its own cell:
 *   its shaft never reaches further back than SPECTRAL_BACK px behind the cell's middle, so the
 *   first cell's never runs through the one who cast it and a full lane ripples as separate stabs,
 *   never one beam.
 * - stab: a short lance of the caller's ramp stabbing to a point (Ambush's shadow stake,
 *   Transcendence's lance through a body).
 *
 * In the light layer, added. At most CAP lances at once (counted, not looked for): past it a
 * thrust shows only the glint at its end, the bodies their flares. 6 quads a lance (shaft, halo,
 * head, glint, 2 ghosts), a glow and a pool; a pierce 1 flare, 1 star, 3 sparks and 3 drops (P2).
 * Any thread: built where it is called, put on the scene on the render thread. Draws only FxRandom.
 */
public class LanceFX extends Group {

	/** Lances at once; bodies a thrust pierces in full. */
	public static final int CAP = 16, RICH = 4;
	/** A thrust's tip crosses a cell in STEP s, pokes OVERSHOOT px past the last, holds HOLD s
	 *  and is pulled back over WITHDRAW s; the draw's core at the hand lasts DRAW s. */
	public static final float STEP = 0.035f, HOLD = 0.07f, WITHDRAW = 0.12f, DRAW = 0.05f, OVERSHOOT = 5f;
	/** A spectral lance stabs over SPECTRAL_STAB s, holds SPECTRAL_HOLD and dissolves over
	 *  SPECTRAL_FADE; its shaft is SPECTRAL_SHAFT px. */
	public static final float SPECTRAL_STAB = 0.07f, SPECTRAL_HOLD = 0.05f, SPECTRAL_FADE = 0.18f;
	public static final int SPECTRAL_SHAFT = 13;
	/** How far behind its cell's middle a spectral lance may reach at most, px: a lane's lances stand
	 *  4 px apart (the last cell's tip is 8 px past its middle), a dashed ripple, never one beam. */
	public static final float SPECTRAL_BACK = 4f;
	/** A stab's tip comes STAB_FROM px in over STAB_IN s. */
	public static final float STAB_FROM = 6f, STAB_IN = 0.06f;
	/** A thrust's height over the cell's raised middle (SkillVfx.raisedY), px: his hand, a body's middle. */
	public static final float HAND = 4f;

	/** The steel lance (Impaling Thrust): white, ice, steel, its shadow. */
	public static final int[] STEEL = { 0xFFFFFF, 0xEAF6FF, 0xB9DEEE, 0x7FA9C9 };
	/** The spectral lance (the +3's): white, ghost blue, its haze, its shadow. */
	public static final int[] SPECTRAL = { 0xFFFFFF, 0xD6E4FA, 0xA9B8E8, 0x6E78B8 };

	private static final int THRUST = 0, GHOST = 1, STAB = 2;
	private static final double DEG = 180 / Math.PI;

	private static int live;
	private boolean counted = true;

	private final int mode;
	private final int[] ramp;
	private final FxPic shaft, halo, head, glint, ghost1, ghost2;
	//its way, and the tip's road: where it starts, the cells it crosses (a thrust), where it ends
	private final float cos, sin, deg;
	private final float baseX, baseY, fromX, fromY, toX, toY;
	private final int reach;
	private final float delay, extend, total, life;
	private final int depth = Dungeon.depth, branch = Dungeon.branch;
	private final CharSprite hero;
	private float time = -1;
	private float tipX, tipY, prev1X, prev1Y, prev2X, prev2Y;
	private boolean begun, peaked, dissolved;

	private LanceFX( int mode, int[] ramp, float baseX, float baseY, float fromX, float fromY, float toX, float toY,
			int reach, float delay, float life, CharSprite hero ){
		this.mode = mode;
		this.ramp = ramp;
		this.baseX = baseX;
		this.baseY = baseY;
		this.fromX = fromX;
		this.fromY = fromY;
		this.toX = toX;
		this.toY = toY;
		this.reach = Math.max( 1, reach );
		this.delay = Math.max( 0f, delay );
		this.hero = hero;
		float dx = toX - fromX, dy = toY - fromY;
		float len = (float) Math.sqrt( dx * dx + dy * dy );
		if (mode == THRUST){
			dx = toX - baseX;
			dy = toY - baseY;
			len = (float) Math.sqrt( dx * dx + dy * dy );
		}
		cos = len > 0.01f ? dx / len : 1f;
		sin = len > 0.01f ? dy / len : 0f;
		deg = (float) (Math.atan2( sin, cos ) * DEG);
		extend = mode == THRUST ? STEP * (this.reach - 1) : 0;
		switch (mode){
			case THRUST: total = extend + HOLD + WITHDRAW; break;
			case GHOST:  total = SPECTRAL_STAB + SPECTRAL_HOLD + SPECTRAL_FADE; break;
			default:     total = Math.max( STAB_IN + 0.05f, life ); break;
		}
		this.life = total;
		halo = line( SkillVfx.LIGHT_SHEET, FxFrames.Light.CORE_BAND_3, ramp[3] );
		shaft = line( SkillVfx.LIGHT_SHEET, FxFrames.Light.CORE_BAND_1, ramp[2] );
		ghost2 = headPic( ramp[1] );
		ghost1 = headPic( ramp[1] );
		head = headPic( ramp[1] );
		glint = new FxPic( SkillVfx.SHEET );
		glint.additive = true;
		glint.show( FxFrames.STAR4_7 );
		glint.visible = false;
		add( glint );
		tipX = prev1X = prev2X = fromX;
		tipY = prev1Y = prev2Y = fromY;
	}

	private FxPic line( String sheet, int[] frame, int color ){
		FxPic p = new FxPic( sheet );
		p.additive = true;
		p.show( frame );
		p.lit( color );
		p.origin.set( 0, frame[3] / 2f );
		p.visible = false;
		add( p );
		return p;
	}

	private FxPic headPic( int color ){
		FxPic p = new FxPic( SkillVfx.SHEET );
		p.additive = true;
		p.show( FxFrames.LANCE_HEAD_9 );
		p.lit( color );
		//turned about its socket, where the shaft meets it
		p.origin.set( 0, FxFrames.LANCE_HEAD_9[3] / 2f );
		p.visible = false;
		add( p );
		return p;
	}

	// ------------------------------------------------------------------ the count

	/** Lances on show. */
	public static synchronized int live(){
		return live;
	}

	/** A new scene (a test, the preview dump): none counted. */
	public static synchronized void reset(){
		live = 0;
		pierced = 0;
		pierceAt = -1;
	}

	private static synchronized boolean enter(){
		if (live >= CAP) return false;
		live++;
		return true;
	}

	private synchronized void leave(){
		if (!counted) return;
		counted = false;
		synchronized (LanceFX.class){
			live = Math.max( 0, live - 1 );
		}
	}

	// ------------------------------------------------------------------ making one

	/**
	 * Impaling Thrust's lance from the hero's hand down the lane from `fromCell` to `toCell`
	 * (`reach` cells), in a ramp (lightest first: STEEL), its tip reaching the lane's cell k at
	 * STEP x (k - 1) s. Any thread.
	 */
	public static void thrust( int fromCell, int toCell, int reach, int[] ramp, CharSprite hero ){
		if (!SkillVfx.valid( fromCell ) || !SkillVfx.valid( toCell ) || fromCell == toCell || Dungeon.hero == null) return;
		//at the height of his hand and of the bodies' middles (a rat's as a man's)
		float sx = SkillVfx.cx( fromCell ), sy = SkillVfx.raisedY( fromCell ) + HAND;
		float ex = SkillVfx.cx( toCell ), ey = SkillVfx.raisedY( toCell ) + HAND;
		float dx = ex - sx, dy = ey - sy, len = (float) Math.sqrt( dx * dx + dy * dy );
		//the hand: 6 px out of his middle toward the target
		float hx = sx + dx / len * 6f, hy = sy + dy / len * 6f;
		if (!enter()){
			//past the cap: the glint where it ends
			Fx.post( () -> glintAt( ex, ey, 0xFFFFFF ) );
			return;
		}
		//its tip's road: the lane's first cell (as the lane's first step shows it) to its last,
		//poked past
		float fx = sx + dx / Math.max( 1, reach ), fy = sy + dy / Math.max( 1, reach );
		LanceFX lance = new LanceFX( THRUST, ramp, hx, hy, fx, fy, ex + dx / len * OVERSHOOT, ey + dy / len * OVERSHOOT,
				reach, 0, 0, hero );
		lance.laneX = sx;
		lance.laneY = sy;
		lance.laneDX = dx;
		lance.laneDY = dy;
		lance.ringY = SkillVfx.cy( toCell ) + 4;
		launch( lance );
	}

	/** The +3's spectral lance over a cell, thrust from the cast's cell (from == cell: from up
	 *  and to the left), `delay` s from now. Any thread. */
	public static void spectral( int fromCell, int cell, float delay ){
		spectral( fromCell, cell, delay, SPECTRAL );
	}

	/** The same in a ramp of the caller's. */
	public static void spectral( int fromCell, int cell, float delay, int[] ramp ){
		if (!SkillVfx.valid( cell ) || !SkillVfx.valid( fromCell ) || Dungeon.hero == null) return;
		float cx = SkillVfx.cx( cell ), cy = SkillVfx.cy( cell ) - 2;
		float dx, dy;
		if (fromCell == cell){
			dx = (float) Math.cos( Math.toRadians( 30 ) );
			dy = (float) Math.sin( Math.toRadians( 30 ) );
		} else {
			dx = cx - SkillVfx.cx( fromCell );
			dy = cy - (SkillVfx.cy( fromCell ) - 2);
			float l = (float) Math.sqrt( dx * dx + dy * dy );
			dx /= l;
			dy /= l;
		}
		if (!enter()){
			Fx.post( () -> PixelFlare.show( cx, cy, ramp[1], PixelFlare.S, 0.2f ) );
			return;
		}
		//its tip comes from 2 px behind the cell's middle to 8 px through it
		LanceFX fx = new LanceFX( GHOST, ramp, 0, 0, cx - dx * 2f, cy - dy * 2f, cx + dx * 8f, cy + dy * 8f, 1, delay, 0, null );
		fx.ringY = cy + 6;
		fx.midX = cx;
		fx.midY = cy;
		launch( fx );
	}

	/** A short lance of a ramp stabbing to (x, y) along angleDeg (0 east, 90 down), its shaft
	 *  `length` px, shown `life` s: Ambush's shadow stake, Transcendence's lance. Any thread. */
	public static void stab( float x, float y, float angleDeg, int[] ramp, int length, float life ){
		if (Dungeon.hero == null || !enter()) return;
		float dx = (float) Math.cos( Math.toRadians( angleDeg ) ), dy = (float) Math.sin( Math.toRadians( angleDeg ) );
		LanceFX fx = new LanceFX( STAB, ramp, 0, 0, x - dx * STAB_FROM, y - dy * STAB_FROM, x, y, 1, 0, life, null );
		fx.shaftLen = Math.max( 2, length );
		launch( fx );
	}

	//the thrust's lane (its start and its run to the last cell); the ring's floor; a spectral
	//lance's cell
	private float laneX, laneY, laneDX, laneDY, ringY, midX, midY;
	private int shaftLen = SPECTRAL_SHAFT;

	private static void launch( final LanceFX fx ){
		Fx.post( () -> {
			if (GameScene.layer( GameScene.Layer.LIGHTS ) == null){
				fx.leave();
				return;
			}
			GameScene.light( fx );
		} );
	}

	// ------------------------------------------------------------------ the frame

	@Override
	public void update(){
		super.update();
		if (depth != Dungeon.depth || branch != Dungeon.branch){
			Fx.dispose( this );
			return;
		}
		if (time < 0){
			time = 0;
		} else {
			time += Game.elapsed;
		}
		float t = time - delay;
		if (t < 0){
			hide();
			return;
		}
		if (t >= total){
			Fx.dispose( this );
			return;
		}
		if (mode == THRUST) thrustFrame( t );
		else if (mode == GHOST) ghostFrame( t );
		else stabFrame( t );
	}

	private void thrustFrame( float t ){
		if (!begun){
			begun = true;
			drawn();
		}
		float a = 1f;
		boolean held = false;
		prev2X = prev1X;
		prev2Y = prev1Y;
		prev1X = tipX;
		prev1Y = tipY;
		if (t < extend){
			//down the lane a cell every STEP s: cell k at STEP x (k - 1)
			float k = 1f + t / STEP;
			tipX = laneX + laneDX * k / reach;
			tipY = laneY + laneDY * k / reach;
		} else if (t < extend + HOLD){
			//it pokes out past the last body, easing, and holds
			float h = Math.min( 1f, (t - extend) / STEP );
			float ex = laneX + laneDX, ey = laneY + laneDY;
			tipX = ex + (toX - ex) * FxCurves.easeOut( h );
			tipY = ey + (toY - ey) * FxCurves.easeOut( h );
			held = true;
			if (!peaked){
				peaked = true;
				//the spent force spreading on the floor at its end
				if (Fx.seen( toX, toY )){
					FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), laneX + laneDX, ringY, 9, 23, 0.3f, ramp[2], 0.6f, true );
				}
			}
		} else {
			//pulled back to the hand
			float w = Math.min( 1f, (t - extend - HOLD) / WITHDRAW );
			float e = FxCurves.easeIn( w );
			tipX = toX + (baseX - toX) * e;
			tipY = toY + (baseY - toY) * e;
			a = 1f - 0.7f * w;
		}
		boolean seen = Fx.seen( tipX, tipY ) || Fx.seen( baseX, baseY );
		lay( baseX, baseY, tipX, tipY, a, seen );
		head.lit( held ? Ramp.lerp( ramp[1], 0xFFFFFF, 0.6f ) : ramp[1] );
		glint.visible = seen && t < extend + HOLD;
		glint.place( tipX, tipY );
		glint.am = held ? 1f : 0.85f;
		boolean pulled = t >= extend + HOLD;
		ghost( ghost1, prev1X, prev1Y, 0.45f * a, seen && pulled );
		ghost( ghost2, prev2X, prev2Y, 0.2f * a, seen && pulled );
	}

	//frame 0 of a thrust: the draw. A white core at the hand, the hero lit a moment, his feet
	//kicking dust back, and the glow and the pool that follow the tip
	private void drawn(){
		glint.place( tipX, tipY );
		FxLight.glow( tipX, tipY, 11, ramp[1], 0.8f ).follow( glint, 0, 0 ).life( 0, total - WITHDRAW, WITHDRAW );
		FxLight.pool( tipX, tipY + 8, 23, ramp[2], 0.3f ).follow( glint, 0, 8 ).life( 0, total - WITHDRAW, WITHDRAW );
		if (Fx.seen( baseX, baseY )){
			//a small core at his hand: a bigger one washes him white (a body flashing white is a body hit)
			FxLight.glow( baseX, baseY, 7, 0xFFFFFF, 0.9f ).core().life( 0, DRAW * 0.5f, DRAW * 0.5f );
			if (hero != null) SkillVfx.brighten( hero, ramp[2], 0.2f );
			//his feet: dust kicked back, or on water a small ring
			if (WaterFX.onWater( laneX, laneY + 8 )){
				WaterFX.ring( laneX, laneY + 8, WaterFX.S );
				return;
			}
			int n = FxRandom.IntRange( 2, 4 );
			for (int i = 0; i < n; i++){
				float side = FxRandom.sign();
				FxParticle p = SkillVfx.matter( laneX + side * FxRandom.Float( 1f, 4f ), laneY + 6, FxBudget.P3 );
				if (p == null) continue;
				p.look( FxFrames.PUFF_S[FxRandom.Int( 2 )] ).paint( Element.EARTH.smoke.step( 0.4f ) )
						.vel( -cos * FxRandom.Float( 8f, 16f ) + side * 4f, FxRandom.Float( -6f, -2f ) )
						.drag( 3f ).life( 0.4f ).alpha( 0.5f, 0, 0.25f );
			}
		}
	}

	private void ghostFrame( float t ){
		float a;
		if (t < SPECTRAL_STAB){
			float e = FxCurves.easeOut( t / SPECTRAL_STAB );
			tipX = fromX + (toX - fromX) * e;
			tipY = fromY + (toY - fromY) * e;
			a = Math.min( 1f, 0.4f + t / SPECTRAL_STAB );
		} else {
			tipX = toX;
			tipY = toY;
			if (!peaked){
				peaked = true;
				if (Fx.seen( midX, midY )){
					PixelFlare.show( midX, midY, ramp[1], PixelFlare.S, 0.2f );
					FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), midX, ringY, 5, 13, 0.3f, ramp[2], 0.5f, true );
				}
			}
			float f = t - SPECTRAL_STAB - SPECTRAL_HOLD;
			if (f <= 0){
				a = 1f;
			} else {
				if (!dissolved){
					dissolved = true;
					dissolve();
				}
				a = Math.max( 0f, 1f - f / SPECTRAL_FADE );
			}
		}
		boolean seen = Fx.seen( midX, midY );
		//its shaft from no further back than the cell's back edge
		float back = Math.min( LANCE_HEAD_LEN + shaftLen, (tipX - midX) * cos + (tipY - midY) * sin + SPECTRAL_BACK );
		float sx = tipX - cos * back, sy = tipY - sin * back;
		//while the tip is still near that edge, the head comes out of it (cut behind it)
		headCut = Math.max( 0, Math.min( FxFrames.LANCE_HEAD_9[2] - 1, (int) Math.ceil( LANCE_HEAD_LEN - back ) ) );
		lay( sx, sy, tipX, tipY, a, seen );
		glint.visible = seen && t >= SPECTRAL_STAB - 0.02f && t < SPECTRAL_STAB + SPECTRAL_HOLD;
		glint.place( tipX, tipY );
		glint.am = 0.8f;
		ghost1.visible = ghost2.visible = false;
	}

	//a spectral lance's end: motes lifting off its shaft (as much of it as shows)
	private void dissolve(){
		float shown = Math.max( 1f, Math.min( shaftLen, 8f + SPECTRAL_BACK - LANCE_HEAD_LEN ) );
		for (int i = 0; i < 3; i++){
			float k = FxRandom.Float( 0.2f, 1f ) * shown;
			float x = tipX - cos * (LANCE_HEAD_LEN + k), y = tipY - sin * (LANCE_HEAD_LEN + k);
			FxParticle p = SkillVfx.glow( x, y, FxBudget.P2 );
			if (p != null) p.look( FxFrames.MOTE_1 ).paint( Ramp.lerp( ramp[1], ramp[2], FxRandom.Float() ) )
					.vel( FxRandom.Float( -2f, 2f ), -10f ).life( 0.4f ).alpha( 0.9f, 0, 0.3f ).source( x, y );
		}
	}

	private void stabFrame( float t ){
		float e = FxCurves.easeOut( Math.min( 1f, t / STAB_IN ) );
		tipX = fromX + (toX - fromX) * e;
		tipY = fromY + (toY - fromY) * e;
		float fade = total * 0.4f;
		float a = t < total - fade ? 1f : Math.max( 0f, (total - t) / fade );
		boolean seen = Fx.seen( toX, toY );
		if (!peaked && t >= STAB_IN){
			peaked = true;
			if (seen) FxLight.glow( toX, toY, 11, ramp[1], 0.7f ).life( 0, 0.05f, 0.2f );
		}
		float sx = tipX - cos * (LANCE_HEAD_LEN + shaftLen), sy = tipY - sin * (LANCE_HEAD_LEN + shaftLen);
		lay( sx, sy, tipX, tipY, a, seen );
		glint.visible = seen && t >= STAB_IN - 0.02f && t < STAB_IN + 0.06f;
		glint.place( tipX, tipY );
		glint.am = 0.9f;
		ghost1.visible = ghost2.visible = false;
	}

	/** The head's length along its way, tip to socket, px. */
	private static final float LANCE_HEAD_LEN = FxFrames.LANCE_HEAD_9[2] - 1;

	//the head's first px left out (a spectral lance's coming out of its cell's back edge)
	private int headCut;

	//the shaft and its halo from (bx, by) to the head's socket, the head at the tip
	private void lay( float bx, float by, float tx, float ty, float a, boolean seen ){
		float sockX = tx - cos * LANCE_HEAD_LEN, sockY = ty - sin * LANCE_HEAD_LEN;
		float dx = sockX - bx, dy = sockY - by;
		float len = (dx * cos + dy * sin);
		boolean shaftShown = seen && len >= 1f;
		place( shaft, bx, by, len, 0.85f * a, shaftShown );
		place( halo, bx, by, len, 0.35f * a, shaftShown );
		int[] f = FxFrames.LANCE_HEAD_9;
		head.crop( f, headCut, 0, f[2] - headCut, f[3] );
		head.x = sockX + cos * headCut - head.origin.x;
		head.y = sockY + sin * headCut - head.origin.y;
		head.angle = deg;
		head.am = a;
		head.visible = seen;
	}

	private void place( FxPic p, float x, float y, float len, float alpha, boolean shown ){
		p.x = x - p.origin.x;
		p.y = y - p.origin.y;
		p.angle = deg;
		p.scale.set( Math.max( 0f, len ) / 16f, 1 );
		p.am = alpha;
		p.visible = shown;
	}

	private void ghost( FxPic g, float tx, float ty, float alpha, boolean shown ){
		g.x = tx - cos * LANCE_HEAD_LEN - g.origin.x;
		g.y = ty - sin * LANCE_HEAD_LEN - g.origin.y;
		g.angle = deg;
		g.am = alpha;
		g.visible = shown && alpha > 0.01f;
	}

	private void hide(){
		shaft.visible = halo.visible = head.visible = glint.visible = ghost1.visible = ghost2.visible = false;
	}

	@Override
	public void kill(){
		leave();
		super.kill();
	}

	@Override
	public void destroy(){
		leave();
		super.destroy();
	}

	// ------------------------------------------------------------------ pierce

	//the bodies pierced lately, for RICH: a thrust's bodies come within a few frames of each other
	private static float pierceAt = -1;
	private static int pierced;

	private static synchronized boolean richPierce(){
		float now = Game.timeTotal;
		if (now - pierceAt > 0.3f || now < pierceAt) pierced = 0;
		pierceAt = now;
		return ++pierced <= RICH;
	}

	/**
	 * A body the lance runs through, at the moment its tip enters it (the lane's step): a flare, a
	 * one-frame star where the tip is, and out of its back (along angleDeg, the thrust's way) three
	 * sparks in the lance's colours and three drops of its blood landing on its feet's line; the
	 * first RICH bodies of a thrust in full, the rest the flare alone. Any thread.
	 */
	public static void pierce( final CharSprite body, final float angleDeg, final int[] ramp ){
		if (body == null) return;
		Fx.post( () -> {
			if (body.parent == null || !body.visible) return;
			float x = SkillVfx.midX( body ), y = SkillVfx.midY( body );
			PixelFlare.show( x, y, ramp[1], PixelFlare.S, 0.2f );
			if (!richPierce()) return;
			FxParticle star = SkillVfx.glow( x, y, FxBudget.P0 );
			if (star != null) star.look( FxFrames.IMPACT_STAR[FxRandom.Int( 2 )] ).paint( 0xFFFFFF ).life( 0.04f )
					.alpha( 1f, 0, 0.01f ).snap();
			float a0 = (float) Math.toRadians( angleDeg );
			float bx = x + (float) Math.cos( a0 ) * 3f, by = y + (float) Math.sin( a0 ) * 3f;
			for (int i = 0; i < 3; i++){
				FxParticle p = SkillVfx.glow( bx, by, FxBudget.P2 );
				if (p == null) continue;
				float a = a0 + (float) Math.toRadians( FxRandom.Float( -25f, 25f ) );
				p.look( FxFrames.SPARK_2 ).orient().paint( Ramp.lerp( 0xFFFFFF, ramp[2], FxRandom.Float() ) )
						.polar( a, FxRandom.Float( 70f, 110f ) ).drag( 2f ).life( 0.2f ).alpha( 1f, 0, 0.1f );
			}
			float feet = body.y + body.height() - 2;
			int blood = body.blood();
			for (int i = 0; i < 3; i++){
				FxParticle p = SkillVfx.matter( bx, by, FxBudget.P2 );
				if (p == null) continue;
				float v = FxRandom.Float( 25f, 45f );
				p.look( FxFrames.DROP_2 ).orient().paint( blood & 0xFFFFFF )
						.vel( (float) Math.cos( a0 ) * v, (float) Math.sin( a0 ) * v - 20f ).gravity( 200f )
						.ground( Math.max( feet, by + 1 ), FxParticle.RING, 0 ).landDry( FxParticle.SPLAT ).strength( 0.15f )
						.mark( blood & 0xFFFFFF ).life( 0.5f ).alpha( 1f, 0, 0.15f );
			}
		} );
	}

	//past the cap: a glint where the thrust ends
	private static void glintAt( float x, float y, int color ){
		FxParticle tip = SkillVfx.glow( x, y, FxBudget.P1 );
		if (tip != null) tip.look( FxFrames.STAR4_7 ).paint( color ).life( StreakFX.TIP_FOR ).alpha( 1f, 0, StreakFX.TIP_FOR * 0.7f ).snap();
	}
}
