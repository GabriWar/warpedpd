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

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FireflyParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SunlightParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * Nature, life and the holy (the nature-life-holy area: docs/fx/nature-life-holy.md once
 * written): grass and leaves, growth, sunlight and foliage, healing, holy light, fireflies, the
 * aurora and the rainbow.
 *
 * Seeded by the effects kit with a gallery page of today's, the "before" its builder works from.
 */
public class NatureHolyFx implements FxModule {

	@Override
	public String key(){
		return "nature-life-holy";
	}

	@Override
	public String title(){
		return "Nature, life and the holy";
	}

	@Override
	public void exhibits( FxGallery.Page page ){
		page.add( "walking through the high grass", 3f, s -> {
			int[] g = s.grass();
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( -2, -3 ) );
			for (int i = 0; i < 3; i++){
				final int to = g[i];
				s.after( 0.5f * i, () -> {
					p.move( to );
					s.burst( to, LeafParticle.LEVEL_SPECIFIC, 4 );
				} );
			}
		} );
		page.add( "regrowth", 3f, s -> {
			for (int c : s.grass()) s.burst( c, LeafParticle.GENERAL, 6 );
			for (int c : s.stone()) s.burst( c, LeafParticle.LEVEL_SPECIFIC, 4 );
		} );
		page.add( "sunlight and foliage", 3.5f, s -> {
			for (int i = 0; i < 3; i++) s.pour( s.grass()[3 + i], ShaftParticle.FACTORY, 0.4f, 3f );
			s.pour( s.stone()[0], SunlightParticle.FACTORY, 0.3f, 3f );
		} );
		page.add( "healing", 2.5f, s -> {
			if (s.dryRat.sprite == null) return;
			s.dryRat.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 4 );
			s.dryRat.sprite.showStatus( CharSprite.POSITIVE, "20" );
		} );
		page.add( "holy light", 2.5f, s -> {
			if (s.skeleton.sprite == null) return;
			new Flare( 6, 32 ).color( 0xFFFFCC, true ).show( s.skeleton.sprite, 1.5f );
			s.skeleton.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
		} );
		page.add( "fireflies over the pool", 4f, s -> {
			for (int i = 0; i < 3; i++) s.pour( s.cell( 2 + 2 * i, 0 ), FireflyParticle.FACTORY, 0.6f, 3.5f );
		} );
		page.add( "leaves landing on the pool", 3f, s -> {
			for (int i = 0; i < 4; i++) s.burst( s.cell( 2 + i, -1 + i % 2 ), LeafParticle.GENERAL, 3 );
		} );
		//the climate's own events, switched on and off again (the scene shows what the climate says)
		page.add( "the aurora", 5f, s -> {
			if (!ClimateManager.isAuroraActive()) ClimateManager.debugToggleAurora();
			s.after( 4.5f, () -> {
				if (ClimateManager.isAuroraActive()) ClimateManager.debugToggleAurora();
			} );
		} );
		page.add( "the rainbow", 5f, s -> {
			if (!ClimateManager.isRainbowActive()) ClimateManager.debugToggleRainbow();
			s.after( 4.5f, () -> {
				if (ClimateManager.isRainbowActive()) ClimateManager.debugToggleRainbow();
			} );
		} );
	}
}
