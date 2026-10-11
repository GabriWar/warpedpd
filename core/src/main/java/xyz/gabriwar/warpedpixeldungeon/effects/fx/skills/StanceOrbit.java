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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;

/**
 * A stance's aura, one primitive for every class: two motes of the stance's colour in the kit's
 * STANCE orbit (half behind the sprite, half in front, lagging as it moves), each trailed by two
 * fainter copies (0.5, 0.25); breathing BREATH_LO-BREATH_HI at BREATH_HZ; flaring up (FLARE
 * brighter for FLARE_FOR s, three motes thrown off it) the moment the bearer starts a blow (a
 * one-shot animation: attack, zap, operate); and a small cross glinting on the ground at its feet
 * every GLINT_EVERY s. Stances held together share the orbit: two motes each, every stance's in its
 * own colour, their trails shortened to keep within the orbit's quads (trailFor), one flare and
 * one glint for them all, in each stance's colour in turn. 6 orbit quads and a glint; whatever
 * accent a stance pours besides is its own.
 */
public class StanceOrbit extends SkillOrbit {

	public static final int MOTES = 2, TRAIL = 2;
	public static final float BREATH_LO = 0.55f, BREATH_HI = 0.9f, BREATH_HZ = 0.6f;
	public static final float FLARE = 0.4f, FLARE_FOR = 0.15f, GLINT_EVERY = 1.2f;
	public static final int FLARE_MOTES = 3;

	private final int color;
	private boolean wasLooping = true;
	private float glint = GLINT_EVERY;
	private int glints;

	public StanceOrbit( Char owner, int color ){
		super( owner, FxOrbit.Slot.STANCE );
		this.color = color;
	}

	/** How many trailing copies n motes keep within the orbit's quads (TRAIL for one stance). */
	public static int trailFor( int n ){
		return Math.max( 0, Math.min( TRAIL, FxOrbit.MOST / Math.max( 1, n ) - 1 ) );
	}

	@Override
	protected int count(){
		return MOTES;
	}

	@Override
	protected int tint(){
		return color;
	}

	@Override
	protected void look( FxOrbit.Ring ring, int n ){
		ring.motes( n, FxFrames.MOTE_2, color, true ).trail( trailFor( n ) ).breath( BREATH_LO, BREATH_HI, BREATH_HZ ).speed( 2.4f );
	}

	@Override
	protected void tick( float dt ){
		if (!sprite.visible) return;
		//stances held together flare and glint once, by the first of them
		if (!leads()){
			wasLooping = sprite.looping();
			glint = GLINT_EVERY;
			return;
		}
		//a blow begins: the aura flares with it
		boolean looping = sprite.looping();
		if (wasLooping && !looping && ring != null){
			ring.flare( FLARE, FLARE_FOR, 0 );
			//the orbit draws no more than its six quads: the flare's extra motes are thrown off it
			float cx = SkillVfx.midX( sprite ), cy = SkillVfx.midY( sprite ) + slot.h;
			for (int i = 0; i < FLARE_MOTES; i++){
				float a = FxRandom.angle();
				FxParticle p = SkillVfx.glow( cx + slot.rx * (float)Math.cos( a ), cy + slot.ry * (float)Math.sin( a ), FxBudget.P2 );
				if (p != null) p.look( FxFrames.MOTE_1 ).paint( mateTint( i ) ).polar( a, FxRandom.Float( 20f, 30f ) ).drag( 4f )
						.life( 0.25f ).alpha( 1f, 0, 0.15f );
			}
		}
		wasLooping = looping;
		glint -= dt;
		if (glint <= 0){
			glint = GLINT_EVERY;
			float x = SkillVfx.midX( sprite ) + FxRandom.Float( -6f, 6f );
			float y = sprite.y + sprite.height() - 1;
			FxParticle p = SkillVfx.glow( x, y, FxBudget.P3 );
			if (p != null) p.look( FxFrames.PLUS_3 ).paint( mateTint( glints++ ) ).life( 0.3f ).alpha( 0.8f, 0.06f, 0.18f ).snap();
		}
	}
}
