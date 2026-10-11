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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.watabou.noosa.particles.Emitter;

/**
 * The trails a bolt pours that are the arcane area's own (the rest are the trails it always
 * poured, upgraded where they live): a fire bolt's tongues, a cone's tongues growing toward its
 * tip, a foliage bolt's leaves turning round its path. Each a factory poured by a MagicMissile
 * (its velocity and progress read from it); kit particles, FxRandom only.
 */
public final class BoltTrails {

	private BoltTrails(){}

	/** A flame's colours as it cools, its five hottest stops held as a tongue's. */
	public static final Ramp FLAME = Element.FIRE.ramp.keyed( 0, .18f, .45f, .7f, 1 );
	/** Where a tongue is born, a share of the bolt's velocity behind it, and the lift it is given. */
	public static final float BEHIND = 0.15f, LIFT = 10f;
	/** A foliage bolt's leaves: how far they swing off its path, px, and how fast, rad/s. */
	public static final float HELIX = 3f, HELIX_RATE = 12f, CONE_HELIX_RATE = 8f;

	/** A fire bolt's trail: tongues born behind the head, lifting as they shrink, 0.25-0.4 s. */
	public static final Emitter.Factory FIRE = new Emitter.Factory(){
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P1, x, y )) return;
			float vx = 0, vy = 0;
			if (e instanceof MagicMissile){
				vx = ((MagicMissile) e).velocityX();
				vy = ((MagicMissile) e).velocityY();
			}
			float life = FxRandom.Float( 0.25f, 0.4f );
			FxParticle p = Arc.make( e, x, y );
			if (FxRandom.chance( 0.6f )) p.look( FxFrames.TONGUE_S, 12 + FxRandom.Float( -2f, 2f ) );
			else p.look( FxFrames.TONGUE_M, 12 + FxRandom.Float( -2f, 2f ) ).then( 0.5f, FxFrames.TONGUE_S );
			p.ramp( FLAME, true ).life( life ).alpha( 1f, 0, life * 0.4f )
					.vel( -BEHIND * vx + FxRandom.Float( -4f, 4f ), -BEHIND * vy - LIFT )
					.gravity( FxRandom.Float( -34f, -22f ) ).priority( FxBudget.P1 );
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};

	/** A fire cone's trail: small tongues near the caster, middling ones, large ones at the tip. */
	public static final Emitter.Factory FIRE_CONE = new Emitter.Factory(){
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P1, x, y )) return;
			float prog = e instanceof MagicMissile ? ((MagicMissile) e).progress() : 0.5f;
			FxParticle p = Arc.make( e, x, y );
			float life;
			if (prog < 0.33f){
				p.look( FxFrames.TONGUE_S, 12 + FxRandom.Float( -2f, 2f ) );
				life = 0.35f;
			} else if (prog < 0.66f){
				p.look( FxFrames.TONGUE_M, 12 + FxRandom.Float( -2f, 2f ) ).then( 0.6f, FxFrames.TONGUE_S );
				life = 0.45f;
			} else {
				p.look( FxFrames.TONGUE_L, 12 + FxRandom.Float( -2f, 2f ) ).then( 0.55f, FxFrames.TONGUE_M ).then( 0.8f, FxFrames.TONGUE_S );
				life = 0.55f;
			}
			life *= FxRandom.Float( 0.85f, 1.15f );
			p.ramp( FLAME, true ).life( life ).alpha( 1f, 0, life * 0.3f )
					.vel( FxRandom.Float( -4f, 4f ), FxRandom.Float( -12f, -6f ) ).gravity( FxRandom.Float( -30f, -20f ) )
					.sway( FxRandom.Float( 3f, 7f ), FxRandom.Float( 5f, 9f ) ).priority( FxBudget.P1 );
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};

	/** A foliage bolt's trail: leaves of the level's own greens shed on a helix round its path. */
	public static final Emitter.Factory LEAF = leaf( HELIX_RATE, 0f );

	/** A foliage cone's: leaves spiralling round each ray more slowly, a seed among every four. */
	public static final Emitter.Factory LEAF_CONE = leaf( CONE_HELIX_RATE, 0.25f );

	private static Emitter.Factory leaf( final float rate, final float seeds ){
		return new Emitter.Factory(){
			@Override
			public void emit( Emitter e, int index, float x, float y ){
				if (!FxBudget.keep( FxBudget.P2, x, y )) return;
				float vx = 0, vy = 0, t = 0;
				if (e instanceof MagicMissile){
					MagicMissile m = (MagicMissile) e;
					vx = m.velocityX();
					vy = m.velocityY();
					t = m.flown();
				}
				float len = (float)Math.sqrt( vx * vx + vy * vy );
				float nx = len > 0 ? -vy / len : 0, ny = len > 0 ? vx / len : 1;
				float off = HELIX * (float)Math.sin( t * rate + index * 0.9f );
				FxParticle p = Arc.make( e, x + nx * off, y + ny * off );
				if (seeds > 0 && FxRandom.chance( seeds )){
					p.look( FxFrames.SEED ).paint( 0xC8E070 );
				} else {
					p.look( FxRandom.chance( 0.5f ) ? FxFrames.LEAF_D : FxFrames.LEAF_E, 6 )
							.paint( FxRandom.chance( 0.5f ) ? leafA() : leafB() );
				}
				float life = FxRandom.Float( 0.5f, 0.8f );
				p.life( life ).alpha( 1f, 0, 0.25f ).vel( -0.1f * vx + FxRandom.Float( -6f, 6f ), -0.1f * vy - 6f )
						.gravity( 28f ).drag( 2.5f ).sway( FxRandom.Float( 4f, 8f ), FxRandom.Float( 3f, 5f ) )
						.priority( FxBudget.P2 );
			}
		};
	}

	//the level's own greens, as LeafParticle takes them; the surface's when there is no level
	static int leafA(){
		return Dungeon.level != null ? Dungeon.level.color1 : 0x004400;
	}

	static int leafB(){
		return Dungeon.level != null ? Dungeon.level.color2 : 0x88CC44;
	}
}
