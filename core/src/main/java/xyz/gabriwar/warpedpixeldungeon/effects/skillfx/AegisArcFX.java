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
import com.watabou.noosa.particles.Emitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * An aegis of light: one arc of the light sheet's rim (RIM_ARC_23, its eight painted directions)
 * standing round a body on the side a blow comes from. Never a shield item, never the SHIELDED
 * state's bubble: the arc alone, in the cleric's gold, added in the light layer.
 *
 * - flash: Aegis catching a blow: the arc facing the attacker, white for its first FLASH s, then
 *   gold fading out over the rest of its life.
 * - hold: Shield of the Faithful's ward hanging at its bearer's side for as long as the buff
 *   lasts: it faces what it guards (its Ward says which cell, or none: the bearer's front),
 *   sweeping round one direction every SWEEP s when that changes, breathing at BREATH_HZ under
 *   the Ward's resting alpha (which steps down as it cracks), hidden while the ward is broken or
 *   its bearer unseen. Each catch flares it to full for FLASH s with a star and SHARDS flakes
 *   of gold breaking off its face. Let go of with Fx.dispose.
 *
 * 1 quad (+ a flare and SHARDS particles a catch, P2); nothing allocated a frame; made anywhere
 * (an asset key), shown on the render thread. No game Random.
 */
public class AegisArcFX extends FxPic {

	public static final int GOLD = 0xF1D99C, WHITE = 0xFFFFFF;
	/** The flash of a catch at full, s (two frames); a held arc's sweep, s a direction. */
	public static final float FLASH = 0.07f, SWEEP = 0.04f;
	/** A held arc's breath, Hz, and how far under its resting alpha it sinks. */
	public static final float BREATH_HZ = 0.7f, BREATH_DIP = 0.22f;
	public static final int SHARDS = 2;
	/** How far from the body's middle the painted rim lies, px (a flare or a flake there). */
	public static final float RIM = 10f;

	/** What a held arc follows: the cell it faces (-1: its bearer's front) and its resting alpha (0:
	 *  broken, hidden). Read on the render thread. */
	public interface Ward {
		int facing();
		float rest();
	}

	private final CharSprite bearer;
	private final Ward ward;
	private final int color;
	private int octant, shown = -1;
	private float age, life, flash, sweep, seenAlpha;

	private AegisArcFX( CharSprite bearer, Ward ward, int color, int octant, float life ){
		super( SkillVfx.LIGHT_SHEET );
		this.bearer = bearer;
		this.ward = ward;
		this.color = color;
		this.octant = octant;
		this.life = life;
		additive = true;
		origin.set( 11.5f, 11.5f );
		step( octant );
		visible = false;
	}

	/** The direction (HitFx.octant: 0 east, 2 south, 4 west, 6 north) from a body to a cell, or
	 *  its front when there is none. */
	public static int facing( CharSprite s, int cell ){
		if (cell < 0 || !SkillVfx.valid( cell )) return s.flipHorizontal ? 4 : 0;
		float dx = SkillVfx.cx( cell ) - SkillVfx.midX( s ), dy = SkillVfx.cy( cell ) - SkillVfx.midY( s );
		if (dx * dx + dy * dy < 1f) return s.flipHorizontal ? 4 : 0;
		return HitFx.octant( dx, dy );
	}

	/** Aegis's catch: the arc on the side of `from` (a cell), white for FLASH s, then `color`
	 *  fading over `life` s. Any thread. */
	public static void flash( final CharSprite bearer, final int from, final int color, final float life ){
		if (bearer == null) return;
		Fx.post( () -> {
			if (bearer.parent == null || !bearer.visible || GameScene.layer( GameScene.Layer.LIGHTS ) == null) return;
			AegisArcFX a = new AegisArcFX( bearer, null, color, facing( bearer, from ), Math.max( FLASH * 2, life ) );
			a.flash = FLASH;
			GameScene.light( a );
			a.follow();
		} );
	}

	/** A ward's arc held at its bearer's side (shown on the render thread); the caller lets it go
	 *  with Fx.dispose. Any thread. */
	public static AegisArcFX hold( final CharSprite bearer, final Ward ward, final int color ){
		if (bearer == null || ward == null) return null;
		final AegisArcFX a = new AegisArcFX( bearer, ward, color, 0, -1 );
		Fx.post( () -> {
			if (bearer.parent == null || GameScene.layer( GameScene.Layer.LIGHTS ) == null) return;
			a.octant = facing( bearer, ward.facing() );
			a.step( a.octant );
			GameScene.light( a );
			a.follow();
		} );
		return a;
	}

	/** A held arc catches a blow: a flare to full, a star on its face, flakes of gold breaking
	 *  off it. Any thread. */
	public void caught(){
		Fx.post( () -> {
			if (parent == null) return;
			flash = FLASH;
			if (!visible) return;
			float a = octant * (float) Math.PI / 4f;
			float x = SkillVfx.midX( bearer ) + (float) Math.cos( a ) * RIM, y = SkillVfx.midY( bearer ) + (float) Math.sin( a ) * RIM * 0.8f;
			PixelFlare.show( x, y, color, PixelFlare.S, 0.25f );
			for (int i = 0; i < SHARDS; i++){
				FxParticle p = SkillVfx.glow( x, y, FxBudget.P2 );
				if (p == null) continue;
				float out = a + FxRandom.Float( -0.6f, 0.6f );
				p.look( FxFrames.SHARD_GLASS[i % 2] ).paint( i == 0 ? WHITE : color )
						.vel( (float) Math.cos( out ) * FxRandom.Float( 20f, 35f ), (float) Math.sin( out ) * 15f - FxRandom.Float( 15f, 25f ) )
						.gravity( 140f ).spin( 0.08f ).life( 0.45f ).alpha( 1f, 0, 0.2f ).priority( FxBudget.P2 );
			}
		} );
	}

	private void step( int k ){
		if (k == shown) return;
		shown = k;
		show( FxFrames.Light.RIM_ARC_23[k] );
	}

	//on its bearer's middle, its frame the direction it shows
	private void follow(){
		x = Math.round( SkillVfx.midX( bearer ) - 11.5f );
		y = Math.round( SkillVfx.midY( bearer ) - 11.5f );
	}

	@Override
	public void update(){
		super.update();
		boolean frozen = Emitter.freezeEmitters && Game.timeTotal > 1;
		float dt = frozen ? 0 : Game.elapsed;
		age += dt;
		if (bearer.parent == null || (life > 0 && age >= life)){
			if (life > 0) Fx.dispose( this );
			else visible = false;
			return;
		}
		//its bearer walks while time is held still (the hourglass): it stays at his side, held
		follow();
		if (frozen){
			visible = bearer.visible && am > 0.01f && Fx.seen( SkillVfx.midX( bearer ), SkillVfx.midY( bearer ) );
			return;
		}
		if (ward != null){
			//sweep round toward what it guards, one direction at a time, the short way
			int want = facing( bearer, ward.facing() );
			sweep -= dt;
			if (want != octant && sweep <= 0){
				int d = ((want - octant) + 8) % 8;
				octant = (octant + (d <= 4 ? 1 : 7)) % 8;
				sweep = SWEEP;
			}
		}
		step( octant );
		float a;
		boolean white = flash > 0;
		if (flash > 0){
			flash -= dt;
			a = 1f;
		} else if (ward != null){
			float rest = ward.rest();
			//breathing under its resting alpha; a ward coming back fades in rather than popping
			float breath = 1f - BREATH_DIP * (0.5f + 0.5f * (float) Math.sin( age * 2 * Math.PI * BREATH_HZ ));
			float target = rest * breath;
			seenAlpha += Math.max( -dt * 4f, Math.min( dt * 2f, target - seenAlpha ) );
			a = seenAlpha;
		} else {
			a = Math.max( 0f, 1f - (age - FLASH) / Math.max( 0.05f, life - FLASH ) );
		}
		lit( white ? WHITE : color );
		am = a;
		visible = bearer.visible && a > 0.01f && Fx.seen( SkillVfx.midX( bearer ), SkillVfx.midY( bearer ) );
	}
}
