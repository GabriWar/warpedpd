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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * What waits for its beat on a blow (docs/fx/enchant.md, "Arranged for the hit card"), in the
 * enchantments' gallery (EnchantArea.exhibits): the heal of a lifesteal and of a drain enchantment
 * shown when its last bead lands, and the stun stars dropping in on their beat. Restaged on
 * puppets through the blow's own ledger and presenter - no real roll, nothing of the game changed
 * (the stun is a Paralysis on a puppet, taken off again). Captions in the log.
 */
public final class CalmExhibits {

	private CalmExhibits(){}

	public static void exhibits( FxGallery.Page page ){

		page.add( "drains landing: lifesteal and the drain enchantments", 7f, s -> {
			final FxStage.Puppet striker = s.puppet( Gnoll.class, s.cell( -1, 0 ) );
			final FxStage.Puppet foe = s.puppet( Rat.class, s.cell( 0, 0 ) );
			if (striker == null || foe == null) return;
			final EnchantLook vampiric = EnchantLooks.of( Vampiric.class ), blazing = EnchantLooks.of( Blazing.class );
			s.after( 0.3f, () -> {
				GLog.i( "  a lifesteal: its three beads, then the heal's card and ring as the last lands" );
				drain( striker, foe, null, null, 0, true );
			} );
			s.after( 1.8f, () -> {
				GLog.i( "  Vampiric leading: its beads home, the heal as the last one lands" );
				drain( striker, foe, null, vampiric, EnchantFx.MID, false );
			} );
			s.after( 3.4f, () -> {
				GLog.i( "  Vampiric strong: seven beads and three more after them; the heal waits for the last" );
				drain( striker, foe, null, vampiric, EnchantFx.STRONG, false );
			} );
			s.after( 5.2f, () -> {
				GLog.i( "  Vampiric strong, second after a Blazing lead: its beads leave on its beat, the heal after the last" );
				drain( striker, foe, blazing, vampiric, EnchantFx.STRONG, false );
			} );
		} );

		page.add( "the stun stars drop in on their beat", 6f, s -> {
			final FxStage.Puppet striker = s.puppet( Gnoll.class, s.cell( -1, -1 ) );
			final FxStage.Puppet foe = s.puppet( Rat.class, s.cell( 0, -1 ) );
			final FxStage.Puppet still = s.puppet( Rat.class, s.cell( 0, 1 ) );
			if (striker == null || foe == null || still == null) return;
			s.after( 0.3f, () -> {
				GLog.i( "  a blow that stuns: the hit first, then the stars drop into their orbit on the stun's beat" );
				stun( striker, foe );
			} );
			s.after( 2.6f, () -> Buff.detach( foe.ch, Paralysis.class ) );
			s.after( 3.1f, () -> {
				GLog.i( "  a stun outside a blow: the stars at once, as ever" );
				Buff.affect( still.ch, Paralysis.class, Paralysis.DURATION );
			} );
			s.after( 5.4f, () -> Buff.detach( still.ch, Paralysis.class ) );
		} );
	}

	//a blow of a on d that heals a: a lead proc (or none), a drain proc of a look at a tier (or none),
	//a lifesteal, the heal, the number and the star
	private static void drain( FxStage.Puppet a, FxStage.Puppet d, EnchantLook lead, EnchantLook drain, int tier, boolean lifesteal ){
		if (a.sprite.parent == null || d.sprite.parent == null) return;
		a.sprite.attack( d.ch.pos );
		Blow.begin( a.ch, d.ch );
		try {
			if (lead != null) EnchantFx.proc( lead, a.ch, d.ch, EnchantFx.strengthOf( EnchantFx.MID ), 0, 0 );
			if (drain != null) EnchantFx.proc( drain, a.ch, d.ch, EnchantFx.strengthOf( tier ), 0, 0, ShadowRows.VAMPIRIC );
			a.sprite.showStatusWithIcon( CharSprite.POSITIVE, "5", FloatingText.HEALING );
			if (lifesteal) PieceFx.lifesteal( a.ch, d.ch );
			hit( a, d, 9 );
		} finally {
			Blow.end();
		}
	}

	//a blow of a on d that stuns it: the number, the star, the Paralysis on the puppet
	private static void stun( FxStage.Puppet a, FxStage.Puppet d ){
		if (a.sprite.parent == null || d.sprite.parent == null) return;
		a.sprite.attack( d.ch.pos );
		Blow.begin( a.ch, d.ch );
		try {
			Buff.affect( d.ch, Paralysis.class, Paralysis.DURATION );
			hit( a, d, 7 );
		} finally {
			Blow.end();
		}
	}

	private static void hit( FxStage.Puppet a, FxStage.Puppet d, int n ){
		d.sprite.showStatusWithIcon( CharSprite.NEGATIVE, Integer.toString( n ), FloatingText.PHYS_DMG );
		d.sprite.flash();
		d.sprite.hitFx( a.sprite.x + a.sprite.width() / 2f, a.sprite.y + a.sprite.height() / 2f, n, HitFx.SLASH, false );
	}
}
