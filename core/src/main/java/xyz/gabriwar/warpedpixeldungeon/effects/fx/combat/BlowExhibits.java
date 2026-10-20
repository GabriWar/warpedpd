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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * One blow, one card (docs/fx/hit-card.md) in the enchantments' gallery (EnchantArea.exhibits): the
 * busy blow, rapid hits merging into one card, and the red X, each restaged on puppets through the
 * blow's own ledger and presenter (Blow.begin, the pictures' entries, the numbers raised as the
 * game raises them, Blow.end) - no real roll, nothing of the game changed. Captions in the log.
 */
public final class BlowExhibits {

	private BlowExhibits(){}

	public static void exhibits( FxGallery.Page page ){

		page.add( "one busy blow", 5f, s -> {
			final FxStage.Puppet striker = s.puppet( Gnoll.class, s.cell( -1, 0 ) );
			final FxStage.Puppet foe = s.puppet( Rat.class, s.cell( 0, 0 ) );
			if (striker == null || foe == null) return;
			s.after( 0.3f, () -> {
				GLog.i( "  a Blazing proc leads, its flame chip on the card; the vestige's hollow storm chip and the red X come a beat later" );
				GLog.i( "  the heal waits for the lifesteal's beads; Burning is its icon on the card, not a word" );
				busy( striker, foe );
			} );
			s.after( 2.6f, () -> busy( striker, foe ) );
		} );

		page.add( "rapid hits: minigun, shotgun, flurry", 7f, s -> {
			final FxStage.Puppet gunner = s.puppet( Gnoll.class, s.cell( -1, -2 ) );
			final FxStage.Puppet foe = s.puppet( Rat.class, s.cell( 0, -2 ) );
			final FxStage.Puppet shooter = s.puppet( Gnoll.class, s.cell( -1, 0 ) );
			final FxStage.Puppet prey = s.puppet( Rat.class, s.cell( 0, 0 ) );
			final FxStage.Puppet dancer = s.puppet( Gnoll.class, s.cell( -1, 2 ) );
			final FxStage.Puppet mark = s.puppet( Rat.class, s.cell( 0, 2 ) );
			if (gunner == null || foe == null || shooter == null || prey == null || dancer == null || mark == null) return;
			s.after( 0.3f, () -> GLog.i( "  four rounds in 0.4 s: one card counting up, every round's star whole" ) );
			for (int k = 0; k < 4; k++){
				final int round = k;
				s.after( 0.3f + 0.1f * k, () -> blow( gunner, foe, round == 1, round == 0 ? new Vulnerable() : null, 11, HitFx.HIT, null ) );
			}
			s.after( 2.3f, () -> {
				GLog.i( "  five pellets in one frame: one card x5, a whole star for each pellet" );
				for (int k = 0; k < 5; k++) blow( shooter, prey, false, null, 6, HitFx.ARROW, null );
			} );
			s.after( 4.3f, () -> GLog.i( "  a flurry, a blow every 0.15 s: the card hops at most three times a second" ) );
			for (int k = 0; k < 5; k++){
				s.after( 4.3f + 0.15f * k, () -> blow( dancer, mark, false, null, 4, HitFx.SLASH, null ) );
			}
		} );

		page.add( "the red X: lead, accent, repeat, pellets", 7f, s -> {
			final FxStage.Puppet striker = s.puppet( Gnoll.class, s.cell( -1, -1 ) );
			final FxStage.Puppet foe = s.puppet( Rat.class, s.cell( 0, -1 ) );
			final FxStage.Puppet shooter = s.puppet( Gnoll.class, s.cell( -1, 1 ) );
			final FxStage.Puppet prey = s.puppet( Rat.class, s.cell( 0, 1 ) );
			if (striker == null || foe == null || shooter == null || prey == null) return;
			s.after( 0.3f, () -> {
				GLog.i( "  a crit leading a plain blow: the bigger star, then the small red X; the X stamps the card's left slot" );
				blow( striker, foe, true, null, 12, HitFx.SLASH, null );
			} );
			s.after( 1.9f, () -> {
				GLog.i( "  a crit under a proc's lead: the X is the first accent, with a tick of its sound" );
				blow( striker, foe, true, null, 12, HitFx.SLASH, EnchantLooks.of( Blazing.class ) );
			} );
			s.after( 3.5f, () -> {
				GLog.i( "  two crits 0.1 s apart: two whole stars and two Xs, one card, the X stamped once" );
				blow( striker, foe, true, null, 12, HitFx.SLASH, null );
			} );
			s.after( 3.6f, () -> blow( striker, foe, true, null, 12, HitFx.SLASH, null ) );
			s.after( 5.1f, () -> {
				GLog.i( "  three crit pellets in one frame: one X" );
				for (int k = 0; k < 3; k++) blow( shooter, prey, true, null, 6, HitFx.ARROW, null );
			} );
		} );
	}

	//a blow of a on d through the ledger: a proc of a look (or none), a crit, a buff announced, its
	//number, its blood, its flash and its star
	private static void blow( FxStage.Puppet a, FxStage.Puppet d, boolean crit, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff buff,
			int number, int style, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook proc ){
		if (a.sprite.parent == null || d.sprite.parent == null) return;
		a.sprite.attack( d.ch.pos );
		Blow.begin( a.ch, d.ch );
		try {
			if (proc != null) EnchantFx.proc( proc, a.ch, d.ch, EnchantFx.strengthOf( EnchantFx.MID ), 0, 0 );
			if (crit) PieceFx.crit( a.ch, d.ch );
			if (buff != null) Blow.announce( d.ch, buff );
			number( d.sprite, number, FloatingText.PHYS_DMG );
			d.sprite.bloodBurstA( a.sprite.center(), number );
			d.sprite.flash();
			d.sprite.hitFx( a.sprite.x + a.sprite.width() / 2f, a.sprite.y + a.sprite.height() / 2f, number, style, false );
		} finally {
			Blow.end();
		}
	}

	//the worked busy blow: a Blazing proc, a Shocking vestige's echo, a crit, a lifesteal, the burn's
	//and the blow's numbers, Burning, the blood, two flashes, the star
	private static void busy( FxStage.Puppet a, FxStage.Puppet d ){
		if (a.sprite.parent == null || d.sprite.parent == null) return;
		a.sprite.attack( d.ch.pos );
		Blow.begin( a.ch, d.ch );
		try {
			float mid = EnchantFx.strengthOf( EnchantFx.MID );
			EnchantFx.proc( EnchantLooks.of( Blazing.class ), a.ch, d.ch, mid, 0, 0 );
			EnchantFx.proc( EnchantLooks.of( Shocking.class ), a.ch, d.ch, mid, 0, EnchantFx.ECHO );
			PieceFx.crit( a.ch, d.ch );
			a.sprite.showStatusWithIcon( CharSprite.POSITIVE, "4", FloatingText.HEALING );
			PieceFx.lifesteal( a.ch, d.ch );
			number( d.sprite, 3, FloatingText.MAGIC_DMG );
			Blow.announce( d.ch, new Burning() );
			number( d.sprite, 27, FloatingText.PHYS_DMG );
			d.sprite.bloodBurstA( a.sprite.center(), 27 );
			d.sprite.flash();
			d.sprite.flash();
			d.sprite.hitFx( a.sprite.x + a.sprite.width() / 2f, a.sprite.y + a.sprite.height() / 2f, 27, HitFx.SLASH, false );
		} finally {
			Blow.end();
		}
	}

	private static void number( CharSprite s, int n, int icon ){
		s.showStatusWithIcon( CharSprite.NEGATIVE, Integer.toString( n ), icon );
	}
}
