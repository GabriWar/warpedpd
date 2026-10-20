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
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * The shout, the taunt and fear, drawn in the skills' own words (never an alarm trap's red
 * brackets, a Scroll of Rage or Terror, a curse landing or a wraith rising):
 *
 * - roar: the caster lit, a glow and a flare at his mouth, three rings rolling out on the floor
 *   from his feet (white turning the school's colour, the last of matter), a pool of light, eight
 *   force lines bursting out at chest height along the ground's ellipse; on water a splash, on
 *   grass blades flicked (on a dry floor the rings are the ground's answer: a puff of dust would
 *   cloud him). Never washed white (a body flashing white is a body hit).
 * - flinch: an enemy struck by it jolts back as a double image thrown away from the caster, a small
 *   flare at its head (with the shout's TREMBLE ticks, the caller's burst).
 * - pull: a taunted enemy leans toward the caster, motes pulled off it toward him.
 * - dread: fear creeping in: a violet ring closing on its feet, ink crawling inward over the floor
 *   and sinking, and (unless small) a shudder left and right and a pool of ink spreading under it.
 * - creep: dread spreading from one body to the next as ink gliding over the floor.
 * - howl: rings rolling out one after another, shedding wisps on their rims.
 *
 * Any thread: each is put on the scene (and its delay counted) on the render thread. Draws only
 * FxRandom. Caps: the first FLINCH_RICH flinches of a shout get their double image (the rest the
 * flare: the afterimages' own cap is 12), roar 3 rings + 8 lines + a flare + about 6 dust, dread
 * 1 ring + 4 wisps + 2 afterimages + 1 decal (counted with FxDecal's cap), particles through
 * FxBudget (P2 wisps and motes, P3 dressing).
 */
public final class ShoutFX {

	private ShoutFX(){}

	/** The schools' ramps, lightest first. */
	public static final int[] MARTIAL = { 0xFFFFFF, 0xF3DDB5, 0xD9AE75, 0x8C6A40 };
	public static final int[] FURY = { 0xFFE0D0, 0xFF5A4A, 0xCC1111, 0x6A0A0A };
	public static final int[] DREAD = { 0xFFFFFF, 0xE3D6F7, 0xB89CE4, 0x6B4F94 };
	public static final int[] WOLF = { 0xFFFFFF, 0xD7DBE3, 0x9EA4B0, 0x5B6170 };
	/** Ink: its body and its pool. */
	public static final int INK = 0x2A1F3A, INK_POOL = 0x1E1628;

	/** Flinches a shout draws in full; a shout's force lines; the roar's lines' speed and life. */
	public static final int FLINCH_RICH = 5, LINES = 8;
	public static final float LINE_SPEED = 150f, LINE_LIFE = 0.22f, RING_LIFE = 0.5f;
	/** The howl's rings, widest last, and their alphas. */
	public static final int[] HOWL_WIDTHS = { 31, 41, 55, 73, 73 };
	public static final float[] HOWL_ALPHAS = { 0.8f, 0.65f, 0.5f, 0.4f, 0.3f };

	// ------------------------------------------------------------------ the roar

	/** A shout from the caster rolling out to about reachPx, in a school's ramp. Any thread. */
	public static void roar( final CharSprite caster, final int[] ramp, final float reachPx ){
		if (caster == null) return;
		Fx.post( () -> {
			if (caster.parent == null || GameScene.layer( GameScene.Layer.LIGHTS ) == null) return;
			Roar r = new Roar( caster, ramp, reachPx );
			GameScene.light( r );
			r.start();
		} );
	}

	//the roar's moving parts: the first ring's colour, the force lines
	private static final class Roar extends Group {
		private final CharSprite caster;
		private final int[] ramp;
		private final float reach;
		private final FxPic[] lines = new FxPic[LINES];
		private final float[] lx = new float[LINES], ly = new float[LINES], vx = new float[LINES], vy = new float[LINES];
		private final Ramp tone;
		private FxRing first;
		private float time, firstP;

		Roar( CharSprite caster, int[] ramp, float reach ){
			this.caster = caster;
			this.ramp = ramp;
			this.reach = reach;
			tone = new Ramp( 0xFFFFFF, ramp[2] );
			for (int i = 0; i < LINES; i++){
				FxPic p = new FxPic( SkillVfx.LIGHT_SHEET );
				p.additive = true;
				p.show( FxFrames.Light.CORE_BAND_1 );
				p.origin.set( 0, 0.5f );
				p.scale.set( 6f / 16f, 1 );
				p.visible = false;
				lines[i] = p;
				add( p );
			}
		}

		void start(){
			float mx = SkillVfx.midX( caster ), my = SkillVfx.midY( caster );
			float mouthY = caster.y + 5, feet = caster.y + caster.height() - 3;
			boolean seen = caster.visible && Fx.seen( mx, my );
			SkillVfx.brighten( caster, ramp[2], 0.35f );
			if (!seen){
				Fx.dispose( this );
				return;
			}
			//the shout's light at his mouth: a small white core, his body lit in the school's colour
			FxLight.glow( mx, mouthY, 11, 0xFFFFFF, 0.8f ).core().life( 0, 0.04f, 0.02f );
			FxLight.glow( mx, mouthY, 23, ramp[2], 0.7f ).life( 0, 0.04f, 0.2f );
			PixelFlare.show( mx, mouthY, ramp[2], PixelFlare.M, 0.3f );
			Group floor = GameScene.layer( GameScene.Layer.FLOOR );
			int w1 = PulseRingFX.ringWidth( reach );
			int i1 = FxFrames.ringIndex( w1 );
			first = FxRing.ground( floor, mx, feet, 9, w1, RING_LIFE, 0xFFFFFF, 0.9f, true );
			FxRing r2 = FxRing.ground( floor, mx, feet, 9, FxFrames.RING_WIDTHS[Math.max( 1, i1 - 1 )], RING_LIFE, ramp[2], 0.7f, true );
			if (r2 != null) r2.delay( 0.07f );
			FxRing r3 = FxRing.ground( floor, mx, feet, 9, FxFrames.RING_WIDTHS[Math.max( 1, i1 - 2 )], RING_LIFE, ramp[3], 0.5f, false );
			if (r3 != null) r3.delay( 0.14f );
			FxLight.pool( mx, feet, 47, ramp[2], 0.45f ).life( 0, 0.08f, 0.3f );
			for (int i = 0; i < LINES; i++){
				float a = (float) Math.toRadians( i * 45 + FxRandom.Float( -8f, 8f ) );
				float dx = (float) Math.cos( a ), dy = (float) Math.sin( a ) * 0.5f;
				float l = (float) Math.sqrt( dx * dx + dy * dy );
				vx[i] = dx / l * LINE_SPEED;
				vy[i] = dy / l * LINE_SPEED;
				lx[i] = mx + (float) Math.cos( a ) * 7f;
				ly[i] = my + (float) Math.sin( a ) * 3.5f;
				lines[i].angle = (float) Math.toDegrees( Math.atan2( dy, dx ) );
			}
			//the ground's answer at his feet: a splash, grass flicked; never a cloud of dust over him
			if (caster.ch != null && (WaterFX.onWater( mx, feet ) || SkillVfx.grass( caster.ch.pos ))) SkillFX.ground( caster.ch.pos );
		}

		@Override
		public void update(){
			super.update();
			time += Game.elapsed;
			if (first != null && first.alive && first.progress() >= firstP){
				firstP = first.progress();
				SkillVfx.lit( first, tone.at( Math.min( 1f, firstP * 2.2f ) ) );
			} else {
				first = null;
			}
			float p = time / LINE_LIFE;
			for (int i = 0; i < LINES; i++){
				FxPic l = lines[i];
				if (p >= 1f){
					l.visible = false;
					continue;
				}
				lx[i] += vx[i] * Game.elapsed;
				ly[i] += vy[i] * Game.elapsed;
				l.x = lx[i] - l.origin.x;
				l.y = ly[i] - l.origin.y;
				l.lit( Ramp.lerp( ramp[1], ramp[2], p ) );
				l.am = 0.8f * (1f - FxCurves.easeOut( p ));
				l.visible = true;
			}
			if (time >= RING_LIFE + 0.2f) Fx.dispose( this );
		}
	}

	// ------------------------------------------------------------------ what it does to them

	//the flinches of the latest shout (those asked for within half a second of each other)
	private static int flinches;
	private static float flinchAt = -1;

	/** A new scene (a test, the preview dump): no shout's flinches counted. */
	public static synchronized void reset(){
		flinches = 0;
		flinchAt = -1;
	}

	private static synchronized boolean richFlinch(){
		float now = Game.timeTotal;
		if (now - flinchAt > 0.5f || now < flinchAt) flinches = 0;
		flinchAt = now;
		return ++flinches <= FLINCH_RICH;
	}

	/**
	 * An enemy struck by a shout from (fromX, fromY): `delay` s later its body jolts back as a
	 * double image thrown away from the shout, and a flare shows at its head. The first
	 * FLINCH_RICH of a shout in full, the rest the flare. Any thread.
	 */
	public static void flinch( final CharSprite victim, final float fromX, final float fromY, final int[] ramp, float delay ){
		if (victim == null) return;
		final boolean rich = richFlinch();
		later( delay, () -> {
			if (victim.parent == null || !victim.visible) return;
			float mx = SkillVfx.midX( victim ), my = SkillVfx.midY( victim );
			float dx = mx - fromX, dy = my - fromY, l = (float) Math.sqrt( dx * dx + dy * dy );
			if (l < 0.01f){
				dx = 1;
				dy = 0;
			} else {
				dx /= l;
				dy /= l;
			}
			if (rich){
				Afterimage.of( victim, ramp[1], 0.7f, 0.16f, 3 * dx, 1.5f * dy );
				Afterimage.of( victim, ramp[2], 0.45f, 0.12f, 6 * dx, 3 * dy );
			}
			PixelFlare.show( mx, victim.y + 3, ramp[1], PixelFlare.S, 0.18f );
		} );
	}

	/** A taunted enemy: `delay` s later it leans 2 px toward (toX, toY) and three motes are pulled
	 *  off it toward there. Any thread. */
	public static void pull( final CharSprite victim, final float toX, final float toY, final int[] ramp, float delay ){
		if (victim == null) return;
		later( delay, () -> {
			if (victim.parent == null || !victim.visible) return;
			float mx = SkillVfx.midX( victim ), my = SkillVfx.midY( victim );
			float dx = toX - mx, dy = toY - my, l = Math.max( 0.01f, (float) Math.sqrt( dx * dx + dy * dy ) );
			dx /= l;
			dy /= l;
			Afterimage.of( victim, ramp[2], 0.4f, 0.15f, 2 * dx, 2 * dy );
			for (int i = 0; i < 3; i++){
				float x = mx + FxRandom.Float( -4f, 4f ), y = my + FxRandom.Float( -4f, 4f );
				FxParticle p = SkillVfx.glow( x, y, FxBudget.P2 );
				if (p != null) p.look( FxFrames.MOTE_2 ).paint( ramp[2] ).vel( dx * 60f, dy * 60f ).life( 0.2f )
						.alpha( 1f, 0, 0.1f ).source( x, y );
			}
		} );
	}

	/**
	 * Fear creeping into a body, `delay` s from now: a violet ring closing on its feet, ink crawling
	 * in over the floor from the ring's rim and sinking (2 wisps when small, else 4), and unless
	 * small a shudder left then right and a pool of ink spreading under it. Any thread.
	 */
	public static void dread( final CharSprite victim, final boolean small, float delay ){
		if (victim == null) return;
		later( delay, () -> {
			if (victim.parent == null || !victim.visible) return;
			ClosingRing.on( victim, small ? 23 : 31, 7, small ? 0.28f : 0.36f, DREAD[2] );
			float mx = SkillVfx.midX( victim ), feet = victim.y + victim.height() - 3;
			int n = small ? 2 : 4;
			float a0 = FxRandom.angle();
			for (int i = 0; i < n; i++){
				float a = a0 + i * (float) (2 * Math.PI / n) + FxRandom.Float( -0.3f, 0.3f );
				float c = (float) Math.cos( a ), s = (float) Math.sin( a );
				float x = mx + c * 5.5f, y = feet + s * 2.5f;
				FxParticle p = SkillVfx.onFloor( x, y, FxBudget.P2 );
				if (p == null) continue;
				float v = FxRandom.Float( 18f, 24f );
				p.look( FxFrames.INK_WISP, 5 ).paint( Element.SHADOW.ramp.step( 0.3f ) )
						.vel( -c * v, -s * v * 0.5f + 3f ).life( 0.45f ).alpha( 0.7f, 0.05f, 0.25f ).smooth();
			}
			if (small) return;
			Afterimage.of( victim, DREAD[3], 0.45f, 0.1f, -2, 0 );
			later( 0.06f, () -> {
				if (victim.parent != null && victim.visible) Afterimage.of( victim, DREAD[3], 0.45f, 0.1f, 2, 0 );
			} );
			FxDecal pool = FxDecal.at( mx, feet + 1, FxFrames.SPLAT[2], INK_POOL, 0.4f, 0.35f, 0.4f, FxRandom.chance( 0.5f ) );
			if (pool != null) pool.revealCrop( FxDecal.FROM_MIDDLE, 0.12f );
		} );
	}

	/** Dread spreading from one cell to the next: three wisps of ink gliding over the floor from
	 *  `fromCell` to `toCell` over 0.2 s, 0.04 s apart, `delay` s from now. Any thread. */
	public static void creep( final int fromCell, final int toCell, float delay ){
		if (!SkillVfx.valid( fromCell ) || !SkillVfx.valid( toCell )) return;
		final float ax = SkillVfx.cx( fromCell ), ay = SkillVfx.cy( fromCell ) + 4;
		final float bx = SkillVfx.cx( toCell ), by = SkillVfx.cy( toCell ) + 4;
		for (int i = 0; i < 3; i++){
			later( delay + 0.04f * i, () -> {
				if (!Fx.seen( ax, ay ) && !Fx.seen( bx, by )) return;
				float x = ax + FxRandom.Float( -2f, 2f ), y = ay + FxRandom.Float( -1f, 1f );
				FxParticle p = SkillVfx.onFloor( x, y, FxBudget.P2 );
				if (p != null) p.look( FxFrames.INK_WISP, 5 ).paint( Element.SHADOW.ramp.step( 0.3f ) )
						.vel( (bx - x) / 0.2f, (by - y) / 0.2f ).turbulence( 1f, 6f ).life( 0.25f ).alpha( 0.6f, 0.03f, 0.08f )
						.smooth();
			} );
		}
	}

	/**
	 * A howl rolling out from the caster: `rings` rings on the floor one after another `perRing`
	 * s apart, spreading to 31, 41, 55, 73, 73 px at 0.8, 0.65, 0.5, 0.4, 0.3 of the ramp's colour,
	 * each shedding four wisps on its rim (ink for DREAD, the ramp's mist for the rest). Any thread.
	 */
	public static void howl( final CharSprite caster, final int[] ramp, final int rings, final float perRing ){
		if (caster == null) return;
		for (int k = 0; k < Math.min( rings, HOWL_WIDTHS.length ); k++){
			final int ring = k;
			later( perRing * k, () -> {
				if (caster.parent == null || !caster.visible) return;
				float mx = SkillVfx.midX( caster ), feet = caster.y + caster.height() - 3;
				if (!Fx.seen( mx, feet )) return;
				int w = HOWL_WIDTHS[ring];
				FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), mx, feet, 9, w, RING_LIFE, ramp[2], HOWL_ALPHAS[ring], true );
				boolean ink = ramp == DREAD;
				for (int i = 0; i < 4; i++){
					float a = FxRandom.angle();
					float x = mx + (float) Math.cos( a ) * w * 0.45f, y = feet + (float) Math.sin( a ) * w * 0.22f;
					FxParticle p = ink ? SkillVfx.matter( x, y, FxBudget.P3 ) : SkillVfx.glow( x, y, FxBudget.P3 );
					if (p == null) continue;
					if (ink){
						p.look( FxFrames.INK_WISP, 5 ).paint( Element.SHADOW.ramp.step( 0.3f ) ).vel( 0, 4f ).turbulence( 1.5f, 3f )
								.life( 0.6f ).alpha( 0.6f, 0.1f, 0.3f ).smooth();
					} else {
						p.look( FxFrames.EDGE_WISP[FxRandom.Int( 2 )] ).paint( ramp[3] ).vel( (float) Math.cos( a ) * 6f, -2f )
								.life( 0.6f ).alpha( 0.5f, 0.1f, 0.3f );
					}
				}
			} );
		}
	}

	//runs it on the render thread `delay` s from now (at once for none): on the hero's timeline,
	//which drops what is left on a floor change
	static void later( final float delay, final Runnable r ){
		Fx.post( () -> {
			if (delay <= 0){
				r.run();
			} else if (Dungeon.hero != null && Dungeon.hero.sprite != null && Dungeon.hero.sprite.parent != null){
				FxTimeline.start().at( delay, r );
			}
		} );
	}
}
