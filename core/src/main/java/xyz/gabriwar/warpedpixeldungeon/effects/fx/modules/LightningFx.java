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
import xyz.gabriwar.warpedpixeldungeon.actors.StormStrikes;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM100;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.ThunderBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import com.watabou.utils.PointF;

import java.util.ArrayList;

/**
 * Lightning (the lightning area: docs/fx/lightning.md once written): every bolt, arc and spark of
 * electricity, the storm's and the game's, and how far they flash.
 *
 * Seeded by the effects kit with a gallery page of today's lightning, the "before" its builder
 * works from.
 */
public class LightningFx implements FxModule {

	@Override
	public String key(){
		return "lightning";
	}

	@Override
	public String title(){
		return "Lightning";
	}

	@Override
	public void exhibits( FxGallery.Page page ){
		page.add( "a DM-100's zap, three times", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( DM100.class, s.cell( -1, 1 ) );
			for (int i = 0; i < 3; i++){
				s.after( 0.2f + 0.7f * i, () -> p.zap( s.dryRat.pos, () -> {
					if (s.dryRat.sprite != null){
						p.sprite.parent.add( new Lightning( p.sprite.center(), s.dryRat.sprite.center(), null ) );
					}
				} ) );
			}
		} );
		page.add( "a wand of lightning into the pool and on", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( 0, 0 ) );
			p.zap( s.wetRat.pos, () -> {
				if (s.wetRat.sprite == null || s.bat.sprite == null) return;
				ArrayList<Lightning.Arc> arcs = new ArrayList<>();
				arcs.add( new Lightning.Arc( p.sprite.center(), s.wetRat.sprite.center() ) );
				arcs.add( new Lightning.Arc( s.wetRat.sprite.center(), s.bat.sprite.center() ) );
				p.sprite.parent.add( new Lightning( arcs, null ) );
			} );
		} );
		page.add( "its charge running over the water", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( 1, -1 ) );
			p.zap( s.poolMiddle(), () -> {
				ArrayList<Lightning.Arc> arcs = new ArrayList<>();
				PointF at = FxStage.center( s.poolMiddle() );
				arcs.add( new Lightning.Arc( p.sprite.center(), at ) );
				int[] run = { s.cell( 5, 0 ), s.cell( 6, 1 ), s.cell( 3, 1 ), s.cell( 2, 0 ) };
				PointF from = at;
				for (int i = 0; i < run.length; i++){
					PointF to = FxStage.center( run[i] );
					arcs.add( new Lightning.Arc( from, to ).crawl( i + 1 ) );
					from = to;
				}
				p.sprite.parent.add( new Lightning( arcs, null ) );
			} );
		} );
		page.add( "a storm's bolt", 3f, s -> StormStrikes.show( Dungeon.level, s.cell( 5, 1 ), false ) );
		page.add( "a thunderbolt on the skeleton", 2.5f, s -> ThunderBolt.thunderEffect( s.skeleton.sprite ) );
		page.add( "sparks, struck and static", 2.5f, s -> {
			s.burstCenter( s.cell( -2, -1 ), SparkParticle.FACTORY, 8 );
			s.burstCenter( s.cell( 0, -1 ), SparkParticle.STATIC, 8 );
		} );
		page.add( "an electric pond", 3.5f, s -> {
			for (int c : s.pool()) s.pour( c, SparkParticle.STATIC, 0.15f, 3f );
		} );
	}
}
