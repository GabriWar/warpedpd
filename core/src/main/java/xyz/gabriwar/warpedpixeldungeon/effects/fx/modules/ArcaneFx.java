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

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Warlock;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Chains;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.Enchanting;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Identification;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Dagger;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.tweeners.AlphaTweener;

/**
 * Bolts, beams and the arcane (the bolts-beams-arcane area: docs/fx/bolts-beams-arcane.md once
 * written): every wand's bolt and cone, the rays, flares, specks, enchanting and identifying,
 * chains, potions shattering and teleports.
 *
 * Seeded by the effects kit with a gallery page of today's, the "before" its builder works from.
 */
public class ArcaneFx implements FxModule {

	@Override
	public String key(){
		return "bolts-beams-arcane";
	}

	@Override
	public String title(){
		return "Bolts, beams and the arcane";
	}

	//the bolts, one after another
	private static final int[] BOLTS = { MagicMissile.MAGIC_MISSILE, MagicMissile.FROST, MagicMissile.CORROSION,
			MagicMissile.SHADOW, MagicMissile.RAINBOW, MagicMissile.EARTH, MagicMissile.WARD, MagicMissile.FORCE };

	//a speck of every kind, one to a cell
	private static final int[] SPECKS = { Speck.HEALING, Speck.STAR, Speck.LIGHT, Speck.QUESTION, Speck.UP,
			Speck.SCREAM, Speck.BONE, Speck.WOOL, Speck.ROCK, Speck.NOTE, Speck.CHANGE, Speck.HEART, Speck.BUBBLE,
			Speck.COIN };

	@Override
	public void exhibits( FxGallery.Page page ){
		page.add( "bolts of eight kinds", 4.5f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, 0 ) );
			for (int i = 0; i < BOLTS.length; i++){
				final int type = BOLTS[i];
				s.after( 0.5f * i, () -> MagicMissile.boltFromChar( p.sprite.parent, type, p.sprite, s.dryRat.pos, null ) );
			}
		} );
		page.add( "a cone of magic missiles", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, 0 ) );
			p.zap( s.poolMiddle(), () -> {
				for (int dy = -1; dy <= 1; dy++){
					MagicMissile.boltFromChar( p.sprite.parent, MagicMissile.MAGIC_MISS_CONE, p.sprite, s.cell( 5, dy ), null );
				}
			} );
		} );
		page.add( "the death, light and health rays", 3f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, 0 ) );
			p.sprite.parent.add( new Beam.DeathRay( p.sprite.center(), FxStage.center( s.cell( 6, -1 ) ) ) );
			s.after( 0.8f, () -> p.sprite.parent.add( new Beam.LightRay( p.sprite.center(), FxStage.center( s.cell( 6, 0 ) ) ) ) );
			s.after( 1.6f, () -> p.sprite.parent.add( new Beam.HealthRay( p.sprite.center(), FxStage.center( s.cell( 6, 1 ) ) ) ) );
		} );
		page.add( "flares of 4, 8 and 12 rays", 3f, s -> {
			new Flare( 4, 20 ).color( 0xFFFF80, true ).show( s.dryRat.sprite, 2f );
			new Flare( 8, 24 ).color( 0x80C0FF, true ).show( s.skeleton.sprite, 2f );
			new Flare( 12, 28 ).color( 0xFF80FF, true ).show( s.wetRat.sprite, 2f );
		} );
		page.add( "a speck of every kind", 3f, s -> {
			for (int i = 0; i < SPECKS.length; i++){
				s.burstCenter( s.cell( -6 + i % 7, -4 + i / 7 ), Speck.factory( SPECKS[i] ), 3 );
			}
		} );
		page.add( "enchanting, identifying and a spell's sign", 3f, s -> {
			Enchanting.show( s.skeleton, new Dagger() );
			if (s.dryRat.sprite != null) s.dryRat.sprite.parent.add( new Identification( s.dryRat.sprite.center() ) );
			SpellSprite.show( s.wetRat, SpellSprite.CHARGE );
		} );
		page.add( "chains, plain and ethereal", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, 0 ) );
			if (s.dryRat.sprite == null || s.skeleton.sprite == null) return;
			p.sprite.parent.add( new Chains( p.sprite.center(), s.dryRat.sprite.center(), Effects.Type.CHAIN, null ) );
			p.sprite.parent.add( new Chains( p.sprite.center(), s.skeleton.sprite.center(), Effects.Type.ETHEREAL_CHAIN, null ) );
		} );
		page.add( "a potion shattering on the floor and in the pool", 2.5f, s -> {
			Splash.at( FxStage.center( s.cell( 0, -2 ) ), 0x9050FF, 6 );
			Splash.at( FxStage.center( s.cell( 3, 1 ) ), 0x9050FF, 6 );
			GameScene.ripple( s.cell( 3, 1 ) );
		} );
		page.add( "a teleport", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( 0, -2 ) );
			p.sprite.alpha( 0 );
			p.sprite.parent.add( new AlphaTweener( p.sprite, 1, 0.4f ) );
			p.sprite.emitter().start( Speck.factory( Speck.LIGHT ), 0.2f, 3 );
		} );
	}
}
