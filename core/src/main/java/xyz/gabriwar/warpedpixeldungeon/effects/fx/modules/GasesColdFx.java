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

import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.effects.IceBlock;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.CorrosionParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PitfallParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.WebParticle;

/**
 * Gases, cold and earth (the gases-cold-earth area: docs/fx/gases-cold-earth.md once written):
 * every gas cloud, frost and ice, shadow and curses, poison and corrosion, earth, pits and webs.
 *
 * Seeded by the effects kit with a gallery page of today's, the "before" its builder works from:
 * each gas shown by the look its cloud pours (its speck over the stone patch), not the gas itself,
 * so the stage's dummies and the hero breathe nothing.
 */
public class GasesColdFx implements FxModule {

	@Override
	public String key(){
		return "gases-cold-earth";
	}

	@Override
	public String title(){
		return "Gases, cold and earth";
	}

	//a gas's look over the stone patch for three seconds
	private static void gas( FxGallery.Page page, String name, int speck ){
		page.add( name, 3.5f, s -> {
			for (int c : s.stone()) s.pour( c, Speck.factory( speck ), 0.3f, 3f );
		} );
	}

	@Override
	public void exhibits( FxGallery.Page page ){
		gas( page, "toxic gas", Speck.TOXIC );
		gas( page, "confusion gas", Speck.CONFUSION );
		gas( page, "paralytic gas", Speck.PARALYSIS );
		gas( page, "corrosive gas", Speck.CORROSION );
		gas( page, "stench", Speck.STENCH );
		gas( page, "smoke", Speck.SMOKE );
		page.add( "frost on the floor and on the pool", 3f, s -> {
			for (int c : s.stone()) s.burst( c, SnowParticle.FACTORY, 5 );
			for (int c : s.pool()) s.burst( c, SnowParticle.FACTORY, 2 );
		} );
		page.add( "a rat frozen and thawing", 3f, s -> {
			if (s.dryRat.sprite == null) return;
			IceBlock ice = IceBlock.freeze( s.dryRat.sprite );
			s.after( 2f, ice::melt );
		} );
		page.add( "a curse's shadow", 2.5f, s -> s.burstCenter( s.skeleton.pos, ShadowParticle.CURSE, 12 ) );
		page.add( "poison and corrosion splashes", 2.5f, s -> {
			s.burstCenter( s.stone()[1], PoisonParticle.SPLASH, 12 );
			s.burstCenter( s.stone()[2], CorrosionParticle.SPLASH, 12 );
		} );
		page.add( "earth, a pit's edge and webs", 3f, s -> {
			s.burst( s.stone()[3], EarthParticle.FACTORY, 10 );
			s.burst( s.stone()[4], PitfallParticle.FACTORY8, 8 );
			s.burst( s.stone()[5], WebParticle.FACTORY, 6 );
		} );
	}
}
