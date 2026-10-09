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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.modules;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DripParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

/**
 * Water and its life (the water-life area: docs/fx/water-life.md once written): how the water
 * answers what walks in it, jumps in it, drops into it and rains on it, and what lives in it.
 *
 * Seeded by the effects kit with today's behaviour - the ring a wader leaves stepping out of a
 * water cell and the one a jumper makes landing in water, moved here unchanged out of CharSprite -
 * and a gallery page of today's water effects, the "before" its builder works from.
 */
public class WaterLifeFx implements FxModule {

	@Override
	public String key(){
		return "water-life";
	}

	@Override
	public String title(){
		return "Water and its life";
	}

	//a wader leaves a ring on the cell he steps out of, if it is water (not a flier)
	@Override
	public void stepped( CharSprite s, int from, int to ){
		if (s.visible && Dungeon.level.water[from] && s.ch != null && !s.ch.flying) {
			GameScene.ripple( from );
		}
	}

	//and a jumper one where he lands in water
	@Override
	public void jumped( CharSprite s ){
		if (s.visible && Dungeon.level.water[s.ch.pos] && !s.ch.flying) {
			GameScene.ripple( s.ch.pos );
		}
	}

	@Override
	public void exhibits( FxGallery.Page page ){
		page.add( "a wader walks into the pool and through it", 3f, s -> {
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( 1, 1 ) );
			p.move( s.cell( 2, 1 ) );
			s.after( 0.6f, () -> p.move( s.cell( 3, 1 ) ) );
			s.after( 1.2f, () -> p.move( s.cell( 4, 1 ) ) );
			s.after( 1.8f, () -> p.move( s.cell( 5, 1 ) ) );
		} );
		page.add( "a wader walks out of the pool", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( 6, 0 ) );
			s.after( 0.3f, () -> p.move( s.cell( 7, 0 ) ) );
		} );
		page.add( "a wader stands in the pool", 2.5f, s -> s.puppet( Rat.class, s.cell( 3, -1 ) ) );
		page.add( "a jump into the pool", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( 1, -2 ) );
			s.after( 0.3f, () -> p.jump( s.cell( 3, 0 ) ) );
		} );
		page.add( "something dropped into the pool", 2f, s -> GameScene.ripple( s.cell( 5, 0 ) ) );
		page.add( "a frog's plunge", 2f, s -> Splash.at( FxStage.center( s.cell( 3, -1 ) ), 0xC8DCD0, 6 ) );
		page.add( "drops from the roof", 3f, s -> {
			for (int i = 0; i < 3; i++){
				final int c = s.cell( 2 + 2 * i, i - 1 );
				s.after( 0.6f * i, () -> {
					Emitter e = GameScene.emitter();
					if (e == null || c == -1) return;
					PointF p = FxStage.center( c );
					e.pos( p.x, p.y );
					e.burst( DripParticle.FALL, 1 );
				} );
			}
		} );
		page.add( "light rain on the pool", 5f, s -> s.sky( PrecipType.RAIN, 0.2f, 2f, false ) );
		page.add( "heavy rain on the pool", 5f, s -> s.sky( PrecipType.RAIN, 0.6f, 2f, false ) );
	}
}
