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

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.Surprise;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * Characters and combat (the characters-combat area: docs/fx/characters-combat.md once written):
 * what a character wears while a state lasts (burning, frozen, shielded and the rest), and every
 * blow, its blood and its marks.
 *
 * Seeded by the effects kit with a gallery page of today's, the "before" its builder works from:
 * each state put on the dry rat for three seconds, then a fight.
 */
public class CharactersFx implements FxModule {

	@Override
	public String key(){
		return "characters-combat";
	}

	@Override
	public String title(){
		return "Characters and combat";
	}

	//the states shown, each on the dry rat for three seconds
	private static final CharSprite.State[] STATES = {
			CharSprite.State.BURNING, CharSprite.State.FROZEN, CharSprite.State.CHILLED, CharSprite.State.DARKENED,
			CharSprite.State.MARKED, CharSprite.State.HEALING, CharSprite.State.SHIELDED, CharSprite.State.GLOWING,
			CharSprite.State.ILLUMINATED, CharSprite.State.LEVITATING, CharSprite.State.ELECTRIC,
			CharSprite.State.HEARTS, CharSprite.State.INVISIBLE, CharSprite.State.HALOMETHANEBURNING };

	@Override
	public void exhibits( FxGallery.Page page ){
		for (final CharSprite.State state : STATES){
			page.add( state.name().toLowerCase( java.util.Locale.ENGLISH ), 3.2f, s -> {
				CharSprite sprite = s.dryRat.sprite;
				if (sprite == null) return;
				sprite.add( state );
				s.after( 3f, () -> sprite.remove( state ) );
			} );
		}
		page.add( "an aura", 3.2f, s -> {
			CharSprite sprite = s.dryRat.sprite;
			if (sprite == null) return;
			sprite.aura( 0xFFD060, 6 );
			s.after( 3f, sprite::clearAura );
		} );
		page.add( "a blow, its blood and its wound", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( -1, 2 ) );
			p.attack( s.dryRat.pos, () -> {
				if (s.dryRat.sprite == null) return;
				s.dryRat.sprite.bloodBurstA( p.sprite.center(), 8 );
				s.dryRat.sprite.flash();
				Wound.hit( s.dryRat );
			} );
		} );
		page.add( "a surprise attack", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( 0, 3 ) );
			p.attack( s.skeleton.pos, () -> {
				Surprise.hit( s.skeleton );
				if (s.skeleton.sprite != null) s.skeleton.sprite.flash();
			} );
		} );
		page.add( "blocked and missed", 2.5f, s -> {
			if (s.dryRat.sprite != null) s.dryRat.sprite.showStatus( CharSprite.NEUTRAL, "blocked" );
			if (s.skeleton.sprite != null) s.skeleton.sprite.showStatus( CharSprite.NEUTRAL, "missed" );
		} );
		page.add( "blood on the floor and in the pool", 2.5f, s -> {
			Splash.at( FxStage.center( s.cell( 0, 2 ) ), 0xFFBB0000, 8 );
			Splash.at( FxStage.center( s.cell( 3, 0 ) ), 0xFFBB0000, 8 );
			GameScene.ripple( s.cell( 3, 0 ) );
		} );
	}
}
