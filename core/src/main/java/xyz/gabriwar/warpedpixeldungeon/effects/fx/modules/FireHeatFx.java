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

import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Warlock;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.StarStreak;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.AshParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.HalomethaneFlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SacrificialParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.HolyFlameParticle;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

/**
 * Fire and heat (the fire-heat area: docs/fx/fire-heat.md once written): flames of every kind,
 * blasts, embers, smoke, scorches, heat and what fire does to the ground and the water.
 *
 * Seeded by the effects kit with a gallery page of today's fire, the "before" its builder works
 * from.
 */
public class FireHeatFx implements FxModule {

	@Override
	public String key(){
		return "fire-heat";
	}

	@Override
	public String title(){
		return "Fire and heat";
	}

	@Override
	public void exhibits( FxGallery.Page page ){
		page.add( "fire on the grass", 5f, s -> {
			s.seed( Fire.class, 6, s.grass() );
			s.after( 4.5f, s::clearBlobs );
		} );
		page.add( "fire at the pool's edge", 4.5f, s -> {
			s.seed( Fire.class, 6, s.cell( 1, -1 ), s.cell( 1, 0 ), s.cell( 1, 1 ) );
			s.after( 4f, s::clearBlobs );
		} );
		page.add( "a bomb's blast", 3f, s -> {
			int c = s.stone()[0];
			s.burstCenter( c, BlastParticle.FACTORY, 30 );
			for (int n : s.stone()) s.burst( n, SmokeParticle.FACTORY, 4 );
		} );
		page.add( "a fire bolt", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, 0 ) );
			p.zap( s.wetRat.pos, () -> MagicMissile.boltFromChar( p.sprite.parent, MagicMissile.FIRE, p.sprite, s.wetRat.pos, null ) );
		} );
		page.add( "a fireblast's cone", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, 0 ) );
			p.zap( s.poolMiddle(), () -> {
				for (int dy = -1; dy <= 1; dy++){
					MagicMissile.boltFromChar( p.sprite.parent, MagicMissile.FIRE_CONE, p.sprite, s.cell( 5, dy ), null );
				}
			} );
		} );
		page.add( "eight fires side by side", 4f, s -> {
			Emitter.Factory[] fires = { FlameParticle.FACTORY, HalomethaneFlameParticle.FACTORY, SacrificialParticle.FACTORY,
					ElmoParticle.FACTORY, HolyFlameParticle.FACTORY, ShadowParticle.UP, AshParticle.EMBER_GLOW,
					Speck.factory( Speck.INFERNO ) };
			for (int i = 0; i < fires.length; i++) s.pour( s.cell( -4 + i, 4 ), fires[i], 0.05f, 3.5f );
		} );
		page.add( "smoke and steam", 3.5f, s -> {
			s.pour( s.stone()[1], SmokeParticle.FACTORY, 0.1f, 3f );
			s.pour( s.cell( 2, 1 ), Speck.factory( Speck.STEAM ), 0.2f, 3f );
		} );
		page.add( "a falling star", 3f, s -> {
			PointF to = FxStage.center( s.stone()[0] );
			StarStreak.fall( new PointF( to.x - 60, to.y - 140 ), to, 0.7f, true );
		} );
	}
}
