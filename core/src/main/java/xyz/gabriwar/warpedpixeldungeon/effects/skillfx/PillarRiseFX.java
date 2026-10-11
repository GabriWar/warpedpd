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
import com.watabou.utils.Callback;
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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * A column of light rising out of a cell, its brightest moment first: the ground flares (a white
 * pool, a glow at the foot and a ring spreading 13 to 41 px) the frame it is called, then the
 * painted pillar (a soft column and its 3 px core, 104 px tall) is revealed from the ground up
 * over RISE s, easing out. At the peak (RISE s, as ever: onPeak fires then, the moment a skill
 * waits for) embers of holy fire fall from its top; it holds HOLD s, the soft column fading over
 * SOFT_FADE s and the core lingering CORE_FADE s. Its edge shimmers (the outermost columns of
 * pixels off and on, 10 times a second) and motes spiral up it. In the light layer;
 * 2 pictures + a glow, a pool and a ring, at most 22 particles.
 */
public class PillarRiseFX extends Group {

	public static final float RISE = 0.2f, HOLD = 0.12f, SOFT_FADE = 0.25f, CORE_FADE = 0.45f;
	public static final int HEIGHT = 104;
	public static final float SHIMMER = 0.1f;
	public static final int MOTES_MIN = 6, MOTES_MAX = 10, EMBERS = 12;

	private final FxPic soft, core;
	private final int color;
	private final Callback onPeak;
	private final Ramp motes;
	private float x, foot;
	private float time = 0;
	private boolean peaked = false, laid;
	private int moteCount, motesLeft;

	private PillarRiseFX( int cell, int color, Callback onPeak ){
		this.color = color;
		this.onPeak = onPeak;
		x = SkillVfx.cx( cell );
		foot = SkillVfx.cy( cell ) + 5;
		motes = new Ramp( 0xFFFFFF, Ramp.lerp( color, 0xFFFFFF, 0.5f ), color );
		soft = pic( FxFrames.Light.PILLAR, color );
		core = pic( FxFrames.Light.PILLAR_CORE, Ramp.lerp( color, 0xFFFFFF, 0.75f ) );
		moteCount = motesLeft = FxRandom.IntRange( MOTES_MIN, MOTES_MAX );
	}

	private FxPic pic( int[] frame, int c ){
		FxPic p = new FxPic( SkillVfx.LIGHT_SHEET );
		p.additive = true;
		p.lit( c );
		p.crop( frame, 0, frame[3], frame[2], 0 );
		p.visible = false;
		add( p );
		return p;
	}

	public static void show( int cell, int color, Callback onPeak ){
		if (Dungeon.hero == null || Dungeon.hero.sprite == null || Dungeon.hero.sprite.parent == null
				|| Dungeon.level == null || !Dungeon.level.heroFOV[cell]){
			if (onPeak != null) onPeak.call();
			return;
		}
		final PillarRiseFX fx = new PillarRiseFX( cell, color, onPeak );
		Fx.post( () -> {
			if (GameScene.layer( GameScene.Layer.LIGHTS ) == null){
				//nothing to draw on: the moment it waits for comes all the same
				if (onPeak != null) onPeak.call();
				return;
			}
			fx.ground();
			GameScene.light( fx );
		} );
	}

	//the brightest moment, the frame it is called: the ground flares
	private void ground(){
		//full for its first moment (a core, 0.05 s), then within the pools' and glows' caps
		FxLight.pool( x, foot, 31, 0xFFFFFF, 0.8f ).core().life( 0, 0.05f, 0.05f );
		FxLight.pool( x, foot, 31, color, 1f ).life( 0, 0.1f, 0.3f );
		FxLight.glow( x, foot - 4, 23, color, 1f ).core().life( 0, 0.04f, 0.04f );
		FxLight.glow( x, foot - 4, 23, color, 1f ).life( 0, 0.1f, 0.25f );
		FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), x, foot, 13, 41, 0.45f, color, 0.8f, true );
	}

	/** How much of the column shows (0..1) `t` s in: revealed easing out over RISE, whole after. */
	public static float reveal( float t ){
		return t >= RISE ? 1f : FxCurves.easeOut( Math.max( 0f, t ) / RISE );
	}

	@Override
	public void update(){
		super.update();
		time += Game.elapsed;
		//the ground under it slid with the window: so does it
		if (laid){
			soft.takeSlide();
			x = soft.midX();
			foot = soft.midY() + soft.height / 2f;
		}

		if (time < RISE){
			lay( reveal( time ), 1f, 1f );
		} else if (!peaked){
			peaked = true;
			lay( 1f, 1f, 1f );
			embers();
			if (onPeak != null) onPeak.call();
		} else {
			float after = time - RISE - HOLD;
			float softA = after <= 0 ? 1f : 1f - after / SOFT_FADE;
			float coreA = after <= 0 ? 1f : 1f - after / CORE_FADE;
			if (coreA <= 0f){
				Fx.dispose( this );
				return;
			}
			lay( 1f, Math.max( 0f, softA ), coreA );
		}
		spiral();
	}

	//the column shown to `shown` of its height, its layers at their alphas
	private void lay( float shown, float softA, float coreA ){
		int h = Math.round( HEIGHT * shown );
		boolean seen = Fx.seen( x, foot - 4 );
		//the edge's shimmer: the outermost columns of the soft layer off and on
		boolean b = ((int) (time / SHIMMER)) % 2 == 1;
		int[] f = FxFrames.Light.PILLAR;
		int inset = b ? 1 : 0;
		soft.crop( f, inset, HEIGHT - h, f[2] - 2 * inset, h );
		soft.place( x, foot - h / 2f );
		laid = true;
		soft.am = softA;
		soft.visible = seen && h > 0 && softA > 0;
		f = FxFrames.Light.PILLAR_CORE;
		core.crop( f, 0, HEIGHT - h, f[2], h );
		core.place( x, foot - h / 2f );
		core.am = coreA;
		core.visible = seen && h > 0 && coreA > 0;
	}

	//motes spiral up the column through its first 0.4 s
	private void spiral(){
		if (motesLeft <= 0) return;
		float due = moteCount * Math.min( 1f, time / 0.4f );
		while (moteCount - motesLeft < due && motesLeft > 0){
			motesLeft--;
			FxParticle p = SkillVfx.glow( x + FxRandom.Float( -2f, 2f ), foot - FxRandom.Float( 0f, 6f ), FxBudget.P2 );
			if (p == null) continue;
			p.look( FxRandom.chance( 0.6f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).ramp( motes, true )
					.vel( 0, -60f ).sway( 4f, FxRandom.Float( 8f, 11f ) ).life( 0.6f ).alpha( 1f, 0.05f, 0.3f )
					.source( x, foot - 4 );
		}
	}

	//at the peak: holy embers fall from the column's top
	private void embers(){
		Ramp r = Element.HOLY.ember;
		float top = foot - HEIGHT;
		for (int i = 0; i < EMBERS; i++){
			float ey = top + FxRandom.Float( 4f, HEIGHT * 0.45f );
			FxParticle p = SkillVfx.glow( x + FxRandom.Float( -5f, 5f ), ey, FxBudget.P2 );
			if (p == null) continue;
			p.look( FxRandom.chance( 0.7f ) ? FxFrames.EMBER_1 : FxFrames.EMBER_2 ).ramp( r, true )
					.vel( FxRandom.Float( -14f, 14f ), FxRandom.Float( -10f, 4f ) ).gravity( 60f ).drag( 0.8f )
					.life( FxRandom.Float( 0.5f, 0.8f ) ).alpha( 1f, 0, 0.3f ).source( x, foot - 4 );
		}
	}
}
