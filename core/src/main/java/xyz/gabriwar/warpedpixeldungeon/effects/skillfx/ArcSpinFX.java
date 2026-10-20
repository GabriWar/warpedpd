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
import com.watabou.noosa.Visual;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.School;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * A blade arc: a painted crescent (FxFrames.SLASH_16, or SLASH_24 for a wide arc) stepping round
 * a point or a sprite one direction (45 degrees) at a time, every STEP_MIN-STEP_MAX s as fast as
 * the arc spins, its last two directions trailing as ghosts at 0.5 and 0.25 in the school's
 * colour (ghostColor: a white blade's are steel), flecks of light thrown off its edge along the
 * cut, a one-frame star where it meets a target, and the last sweep lingering LINGER s. One arc
 * is a slash, two counter-spinning arcs are a flurry, a slow wide one a pirouette's ribbon (whose
 * feet kick up dust). An arc about a sprite keeps its crescent off the sprite's body
 * (aroundRadius), so the hero is never whitened under it. Drawn as light, in the light layer. 3 pictures + at most 6 particles.
 */
public class ArcSpinFX extends Group {

	/** A step's shortest and longest wait, s, and how long the last sweep lingers. */
	public static final float STEP_MIN = 0.035f, STEP_MAX = 0.055f, LINGER = 0.1f;
	/** The ghosts' alphas. */
	public static final float GHOST_1 = 0.5f, GHOST_2 = 0.25f;
	/** Radius from which the wide crescent is drawn. */
	public static final float WIDE = 12f;
	/** How deep the crescent's body lies inside its leading edge, px, and the gap an arc about a
	 *  sprite keeps between that body and the sprite's side. */
	public static final float BODY = 3f, CLEAR = 1f;

	private final Visual follow;
	//the anchor of an arc over a cell: a slide of the window moves it as it moves every visual
	private final Visual anchor;
	private final int color, headColor;
	private final int dir0, sign;
	private final float stepEvery, duration, offset;
	private final int[][] frames;
	private final boolean kick, strike;
	private final FxPic head, ghost1, ghost2;
	private final Ramp sparkRamp;
	private float time;
	private int shownStep = -1, sparksLeft, steps;
	private boolean struck, kicked;

	private ArcSpinFX( Visual follow, float ax, float ay, int color, int headTint, float radius, float startAngle, float spin,
			float duration, boolean kick, boolean strike ){
		this.follow = follow;
		anchor = new Visual( ax, ay, 0, 0 );
		add( anchor );
		this.color = color;
		int ghost = ghostColor( color );
		headColor = headTint >= 0 ? headTint : Ramp.lerp( color, 0xFFFFFF, 0.6f );
		sign = spin < 0 ? -1 : 1;
		dir0 = Math.round( startAngle / 45f );
		stepEvery = stepEvery( spin );
		this.duration = Math.max( stepEvery, duration );
		steps = stepsFor( this.duration, stepEvery );
		//about a sprite: never over its body (a pirouette's or a flurry's inner arc, a hurl)
		if (follow != null) radius = aroundRadius( radius, follow.width() );
		frames = radius >= WIDE ? FxFrames.SLASH_24 : FxFrames.SLASH_16;
		offset = offset( radius, frames[0][2] );
		this.kick = kick;
		this.strike = strike;
		sparksLeft = FxRandom.IntRange( 2, kick ? 2 : 4 );
		sparkRamp = headTint >= 0 ? new Ramp( headColor, ghost ) : new Ramp( 0xFFFFFF, headColor, ghost );
		ghost2 = pic( ghost );
		ghost1 = pic( ghost );
		head = pic( headColor );
	}

	private FxPic pic( int c ){
		FxPic p = new FxPic( SkillVfx.SHEET );
		p.additive = true;
		p.lit( c );
		p.visible = false;
		add( p );
		return p;
	}

	/** The ghosts' colour: the arc's own, or for a plain white blade the steel school's (a white
	 *  arc's trail would otherwise be white on white). */
	public static int ghostColor( int color ){
		return (color & 0xFFFFFF) == 0xFFFFFF ? School.STEEL.color : color;
	}

	/** How long one 45-degree step waits at this spin (deg/s). */
	public static float stepEvery( float spin ){
		float s = Math.abs( spin );
		if (s < 1f) return STEP_MAX;
		return Math.max( STEP_MIN, Math.min( STEP_MAX, 45f / s ) );
	}

	/** How many steps the head takes in `duration` s. */
	public static int stepsFor( float duration, float stepEvery ){
		return Math.max( 1, (int) Math.ceil( duration / stepEvery - 1e-4f ) );
	}

	/** How far the crescent's frame sits out from the middle so its leading edge lies on `radius`
	 *  (the painted edge is 0.255 of the frame out from its middle). */
	public static float offset( float radius, int frameSize ){
		return Math.max( 0f, Math.min( 8f, radius - 0.255f * frameSize ) );
	}

	/** The radius of an arc about a sprite this wide: at least far enough out that the crescent's
	 *  body (BODY inside its edge) clears the sprite's side by CLEAR. */
	public static float aroundRadius( float radius, float spriteWidth ){
		return Math.max( radius, spriteWidth / 2f + BODY + CLEAR );
	}

	/** The direction (0..7, east first, clockwise) a step shows. */
	public static int direction( int start, int sign, int step ){
		return Math.floorMod( start + sign * step, 8 );
	}

	/** An arc that rides along with a sprite. */
	public static ArcSpinFX around( Visual sprite, int color, float radius, float sweep, float startAngle, float spin, float duration ){
		return around( sprite, color, radius, sweep, startAngle, spin, duration, false );
	}

	/** The same; `kick`: the feet kick up dust as it spins (a pirouette, a flurry). */
	public static ArcSpinFX around( Visual sprite, int color, float radius, float sweep, float startAngle, float spin,
			float duration, boolean kick ){
		if (sprite == null) return null;
		ArcSpinFX fx = new ArcSpinFX( sprite, 0, 0, color, -1, radius, startAngle, spin, duration, kick, false );
		fx.launch();
		return fx;
	}

	/** An arc riding a sprite whose head is `head` (a colour's own pale stop, SkillPalette's) instead
	 *  of the colour paled toward white, its flecks running head to colour with no white: a colour
	 *  whose white-paled head would change its meaning (blood's red pales to pink). */
	public static ArcSpinFX around( Visual sprite, int color, int head, float radius, float sweep, float startAngle,
			float spin, float duration ){
		if (sprite == null) return null;
		ArcSpinFX fx = new ArcSpinFX( sprite, 0, 0, color, head & 0xFFFFFF, radius, startAngle, spin, duration, false, false );
		fx.launch();
		return fx;
	}

	/** An arc over a cell, its first step striking what stands there. */
	public static ArcSpinFX at( int cell, int color, float radius, float sweep, float startAngle, float spin, float duration ){
		if (Dungeon.hero == null || Dungeon.hero.sprite == null || !SkillVfx.valid( cell )) return null;
		ArcSpinFX fx = new ArcSpinFX( null, SkillVfx.cx( cell ), SkillVfx.raisedY( cell ) + 6, color, -1, radius, startAngle,
				spin, duration, false, true );
		fx.launch();
		return fx;
	}

	/** A quick slash: a crescent snapping 200 degrees around a sprite in a fifth of a second,
	 *  striking at its middle. */
	public static void slash( Visual sprite, int color, boolean clockwise ){
		if (sprite == null) return;
		ArcSpinFX fx = new ArcSpinFX( sprite, 0, 0, color, -1, 11, clockwise ? -60 : 240, clockwise ? 900 : -900, 0.22f, false, true );
		fx.launch();
	}

	private void launch(){
		Fx.post( () -> {
			if (follow != null && (!follow.exists || follow.parent == null)) return;
			GameScene.light( this );
		} );
	}

	//the middle it turns about, now
	private float midX(){
		if (follow instanceof CharSprite) return SkillVfx.midX( (CharSprite) follow );
		if (follow != null) return follow.x + follow.width() / 2f;
		return anchor.x;
	}

	private float midY(){
		if (follow instanceof CharSprite) return SkillVfx.midY( (CharSprite) follow );
		if (follow != null) return follow.y + follow.height() / 2f;
		return anchor.y;
	}

	@Override
	public void update(){
		super.update();
		if (follow != null && !follow.exists){
			Fx.dispose( this );
			return;
		}
		time += Game.elapsed;
		if (time >= duration + LINGER){
			Fx.dispose( this );
			return;
		}
		int step = Math.min( steps - 1, (int) (time / stepEvery) );
		float cx = midX(), cy = midY();
		boolean seen = follow != null ? follow.visible : Fx.seen( cx, cy );
		float a = time <= duration ? 1f : Math.max( 0f, 1f - (time - duration) / LINGER );
		lay( head, step, 0, cx, cy, a, seen );
		lay( ghost1, step, 1, cx, cy, a * GHOST_1 * (time <= duration ? 1f : a), seen );
		lay( ghost2, step, 2, cx, cy, a * GHOST_2 * (time <= duration ? 1f : a), seen );
		if (step != shownStep){
			shownStep = step;
			if (seen) onStep( step, cx, cy );
		}
	}

	//a picture at a step `back` steps behind the head
	private void lay( FxPic p, int step, int back, float cx, float cy, float alpha, boolean seen ){
		int s = step - back;
		if (s < 0 || !seen || alpha <= 0.01f){
			p.visible = false;
			return;
		}
		int d = direction( dir0, sign, s );
		p.show( frames[d] );
		float rad = (float) Math.toRadians( d * 45 );
		p.place( cx + (float) Math.cos( rad ) * offset, cy + (float) Math.sin( rad ) * offset );
		p.am = alpha;
		p.visible = true;
	}

	//each new step: a fleck or two thrown off its edge, the strike, the dust
	private void onStep( int step, float cx, float cy ){
		int d = direction( dir0, sign, step );
		float rad = (float) Math.toRadians( d * 45 );
		float ux = (float) Math.cos( rad ), uy = (float) Math.sin( rad );
		float edge = offset + 0.255f * frames[0][2];
		float ex = cx + ux * edge, ey = cy + uy * edge;
		int left = steps - step;
		if (sparksLeft > 0 && FxRandom.Float() * left < sparksLeft){
			sparksLeft--;
			//along the cut: the tangent in the spin's sense
			float tx = -uy * sign, ty = ux * sign;
			float v = FxRandom.Float( 60f, 90f );
			FxParticle p = SkillVfx.glow( ex, ey, FxBudget.P2 );
			if (p != null){
				p.look( FxFrames.SPARK_2 ).ramp( sparkRamp, true ).vel( tx * v, ty * v ).drag( 4f ).orient()
						.life( 0.2f ).alpha( 1f, 0, 0.12f );
			}
		}
		boolean strikeNow = strike && !struck && (follow == null ? step == 0 : step == steps / 2);
		if (strikeNow){
			struck = true;
			float sx = follow == null ? cx : ex, sy = follow == null ? cy : ey;
			FxParticle p = SkillVfx.glow( sx, sy, FxBudget.P0 );
			if (p != null){
				p.look( FxFrames.IMPACT_STAR[FxRandom.Int( 2 )] ).paint( headColor ).life( 0.04f ).alpha( 1f, 0, 0.01f ).snap();
			}
		}
		if (kick && !kicked){
			kicked = true;
			float fy = follow != null ? follow.y + follow.height() - 2 : cy + 6;
			for (int i = 0; i < 4; i++){
				float dir = i < 2 ? -1 : 1;
				FxParticle p = SkillVfx.matter( cx + dir * FxRandom.Float( 2f, 5f ), fy, FxBudget.P3 );
				if (p != null){
					p.look( FxFrames.PUFF_S[FxRandom.Int( 2 )] ).paint( Element.EARTH.smoke.step( 0.3f ) )
							.vel( dir * FxRandom.Float( 12f, 22f ), FxRandom.Float( -6f, -2f ) ).drag( 3f )
							.life( 0.4f ).alpha( 0.55f, 0, 0.25f );
				}
			}
		}
	}

	/** The colour it was given (its ghosts' unless white: ghostColor). */
	public int color(){
		return color;
	}
}
