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
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * A dash, a thrust, a lunge drawn as one stroke of light: the painted tapered streak
 * (FxFrames.STREAK_TAPER, its thick head at the far end) shooting out over its first third and
 * thinning away, a four-point glint at its tip, dust kicked up where it left. A thrust (show)
 * is the stroke alone; a dash (dash: the hero himself crossing it) also leaves two or three
 * afterimages of him along the way (white, then his class's colour) and, for the fastest moves,
 * three thin speed lines beside it. In the light layer. 1 streak + 3 lines + 3 afterimages, at
 * most 6 particles.
 *
 * Opt-in extras (the overloads taking `extras`; every older call looks as it did): MOTES, a dust of
 * its own light left along it as it grows (one MOTE_2 of its colour every MOTE_PX px, drifting up
 * 4 to 8 px/s for MOTE_LIFE s, P3, at most MOTES_MOST); SPARKS, metal scraping off it (one SPARK_2
 * of Element.METAL every SPARK_PX px, thrown either side at 20 to 40 px/s with a drag of 4 for
 * SPARK_LIFE s, P2, at most SPARKS_MOST). Laid where the stroke has reached, FxRandom only.
 */
public class StreakFX extends Group {

	public static final float TIP_FOR = 0.12f, AFTERIMAGE_FOR = 0.15f, LINES_FOR = 0.12f;
	public static final int DUST_MAX = 4;
	/** The extras: MOTES, SPARKS (or both), and their numbers. */
	public static final int MOTES = 1, SPARKS = 2;
	public static final int MOTE_PX = 8, MOTES_MOST = 12, SPARK_PX = 8, SPARKS_MOST = 10;
	public static final float MOTE_LIFE = 0.45f, SPARK_LIFE = 0.2f;

	private static final double A = 180 / Math.PI;

	private final FxPic streak;
	private final FxPic[] lines;
	private final float length, duration;
	private float time;
	//its extras: which, where it starts and its way, its colour, how many of each laid
	private int extras;
	private final float sx, sy, ux, uy;
	private final int color;
	private int motes, sparks;

	private StreakFX( float sx, float sy, float ex, float ey, int color, float duration, boolean speedLines ){
		float dx = ex - sx, dy = ey - sy;
		length = (float) Math.sqrt( dx * dx + dy * dy );
		this.sx = sx;
		this.sy = sy;
		ux = dx / Math.max( 1f, length );
		uy = dy / Math.max( 1f, length );
		this.color = color;
		float angle = (float) (Math.atan2( dy, dx ) * A);
		this.duration = Math.max( 0.1f, duration );
		streak = line( SkillVfx.SHEET, FxFrames.STREAK_TAPER, Ramp.lerp( color, 0xFFFFFF, 0.3f ), sx, sy, angle );
		if (speedLines){
			lines = new FxPic[3];
			float nx = -dy / Math.max( 1f, length ), ny = dx / Math.max( 1f, length );
			for (int i = 0; i < 3; i++){
				float off = (i - 1) * 3f + (i == 1 ? 0 : FxRandom.Float( -0.5f, 0.5f ));
				//the middle line runs ahead of the streak's tail, the outer ones behind it
				float back = i == 1 ? 0.25f : 0.1f;
				lines[i] = line( SkillVfx.LIGHT_SHEET, FxFrames.Light.CORE_BAND_1, color,
						sx + nx * off + dx * back, sy + ny * off + dy * back, angle );
				lines[i].scale.set( length * 0.55f / 16f, 1 );
				lines[i].am = 0.6f;
			}
		} else {
			lines = null;
		}
	}

	private FxPic line( String sheet, int[] frame, int color, float x, float y, float angle ){
		FxPic p = new FxPic( sheet );
		p.additive = true;
		p.show( frame );
		p.lit( color );
		p.origin.set( 0, frame[3] / 2f );
		p.x = x - p.origin.x;
		p.y = y - p.origin.y;
		p.angle = angle;
		p.scale.set( 0, 1 );
		add( p );
		return p;
	}

	/** A thrust between two cells: the stroke alone (whoever makes it stays where he stands). */
	public static void show( int from, int to, int color, float thickness, float duration ){
		show( from, to, color, thickness, duration, 0 );
	}

	/** The same carrying `extras` (MOTES, SPARKS). */
	public static void show( int from, int to, int color, float thickness, float duration, int extras ){
		if (!SkillVfx.valid( from ) || !SkillVfx.valid( to )) return;
		launch( SkillVfx.cx( from ), SkillVfx.raisedY( from ) + 6, SkillVfx.cx( to ), SkillVfx.raisedY( to ) + 6,
				color, duration, false, false, extras );
	}

	/** The hero dashing between two cells: the stroke and his afterimages along it (when he is at
	 *  either end: this hero's own dash), with speed lines beside it for the fastest (a fleche, a
	 *  lunge). */
	public static void dash( int from, int to, int color, float thickness, float duration, boolean speedLines ){
		dash( from, to, color, thickness, duration, speedLines, 0 );
	}

	/** The same carrying `extras` (MOTES, SPARKS). */
	public static void dash( int from, int to, int color, float thickness, float duration, boolean speedLines, int extras ){
		if (!SkillVfx.valid( from ) || !SkillVfx.valid( to )) return;
		boolean hero = Dungeon.hero != null && (from == Dungeon.hero.pos || to == Dungeon.hero.pos);
		launch( SkillVfx.cx( from ), SkillVfx.raisedY( from ) + 6, SkillVfx.cx( to ), SkillVfx.raisedY( to ) + 6,
				color, duration, speedLines, hero, extras );
	}

	public static void show( PointF from, PointF to, int color, float thickness, float duration ){
		launch( from.x, from.y, to.x, to.y, color, duration, false, false, 0 );
	}

	private static void launch( float sx, float sy, float ex, float ey, int color, float duration, boolean speedLines, boolean hero,
	                            int extras ){
		if (Dungeon.hero == null || Dungeon.hero.sprite == null) return;
		float dx = ex - sx, dy = ey - sy;
		if (dx * dx + dy * dy < 1f) return;
		final StreakFX fx = new StreakFX( sx, sy, ex, ey, color, duration, speedLines );
		fx.extras = extras;
		Fx.post( () -> {
			if (GameScene.layer( GameScene.Layer.LIGHTS ) == null) return;
			GameScene.light( fx );
			fx.firstFrame( sx, sy, ex, ey, color, hero );
		} );
	}

	//what is struck and left the moment it starts: the tip's glint, the dust, the afterimages
	private void firstFrame( float sx, float sy, float ex, float ey, int color, boolean hero ){
		boolean seenEnd = Fx.seen( ex, ey ), seenStart = Fx.seen( sx, sy );
		if (seenEnd){
			FxParticle tip = SkillVfx.glow( ex, ey, FxBudget.P1 );
			if (tip != null) tip.look( FxFrames.STAR4_7 ).paint( 0xFFFFFF ).life( TIP_FOR ).alpha( 1f, 0, TIP_FOR * 0.7f ).snap();
		}
		if (seenStart){
			int n = FxRandom.IntRange( 2, DUST_MAX );
			for (int i = 0; i < n; i++){
				float side = FxRandom.sign();
				FxParticle p = SkillVfx.matter( sx + side * FxRandom.Float( 1f, 4f ), sy + 6, FxBudget.P3 );
				if (p == null) continue;
				p.look( FxFrames.PUFF_S[FxRandom.Int( 2 )] ).paint( Element.EARTH.smoke.step( 0.4f ) )
						.vel( -(ex - sx) / length * FxRandom.Float( 8f, 16f ) + side * 4f, FxRandom.Float( -6f, -2f ) )
						.drag( 3f ).life( 0.4f ).alpha( 0.5f, 0, 0.25f );
			}
		}
		CharSprite s = Dungeon.hero.sprite;
		if (hero && s != null && s.visible){
			int n = length > 40 ? 3 : 2;
			float mx = SkillVfx.midX( s ), my = SkillVfx.midY( s );
			for (int i = 0; i < n; i++){
				float k = (i + 1f) / (n + 1f);
				float px = sx + (ex - sx) * k, py = sy + (ey - sy) * k;
				int c = Ramp.lerp( 0xFFFFFF, SkillVfx.classColor(), (float) i / Math.max( 1, n - 1 ) );
				Afterimage.of( s, c, 0.55f - 0.1f * i, AFTERIMAGE_FOR + 0.04f * i, px - mx, py - my );
			}
		}
	}

	@Override
	public void update(){
		super.update();
		time += Game.elapsed;
		if (time >= duration){
			Fx.dispose( this );
			return;
		}
		float p = time / duration;
		float grow = Math.min( 1f, p * 3f );
		float fade = p < 0.4f ? 1f : (1f - p) / 0.6f;
		streak.scale.set( length / 16f * grow, 1 );
		streak.am = fade;
		if (extras != 0) shed( length * grow );
		if (lines != null){
			float lp = Math.min( 1f, time / LINES_FOR );
			for (FxPic l : lines){
				l.visible = lp < 1f;
				l.am = 0.6f * (1f - lp);
			}
		}
	}

	//its extras as far as it has grown: a mote of its light every MOTE_PX px, a spark every SPARK_PX
	private void shed( float grown ){
		while ((extras & MOTES) != 0 && motes < MOTES_MOST && (motes + 0.5f) * MOTE_PX <= grown){
			float d = (motes++ + 0.5f) * MOTE_PX;
			float x = sx + ux * d, y = sy + uy * d;
			if (!Fx.seen( x, y )) continue;
			FxParticle p = SkillVfx.glow( x + FxRandom.Float( -1f, 1f ), y + FxRandom.Float( -1f, 1f ), FxBudget.P3 );
			if (p != null) p.look( FxFrames.MOTE_2 ).paint( color ).vel( FxRandom.Float( -1f, 1f ), -FxRandom.Float( 4f, 8f ) )
					.life( MOTE_LIFE * FxRandom.Float( 0.8f, 1f ) ).alpha( 0.9f, 0, MOTE_LIFE * 0.5f ).snap();
		}
		while ((extras & SPARKS) != 0 && sparks < SPARKS_MOST && (sparks + 0.5f) * SPARK_PX <= grown){
			float d = (sparks++ + 0.5f) * SPARK_PX;
			float x = sx + ux * d, y = sy + uy * d;
			if (!Fx.seen( x, y )) continue;
			FxParticle p = SkillVfx.glow( x, y, FxBudget.P2 );
			if (p == null) continue;
			float v = FxRandom.sign() * FxRandom.Float( 20f, 40f );
			p.look( FxFrames.SPARK_2 ).orient().ramp( Element.METAL.ramp, true ).vel( -uy * v + ux * 6f, ux * v + uy * 6f )
					.drag( 4f ).life( SPARK_LIFE * FxRandom.Float( 0.8f, 1f ) ).alpha( 1f, 0, SPARK_LIFE * 0.5f );
		}
	}
}
