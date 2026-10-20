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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Disarmed;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flute;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The weapon types' looks in the enchantments' gallery (EnchantArea.exhibits; docs/fx/type-looks.md):
 * Woodsman, Quarry, Bard and Lash, then the perk accents, each blow restaged on puppets through the
 * blow's own ledger and presenter (Blow.begin, TypeFx's entries, the numbers and buffs as the game
 * raises them, Blow.end) - no real roll, nothing of the game changed. The throws and the drag are
 * Pushing's own looks played on the puppets; a puppet holds no instrument, so the Bard's held notes
 * are restaged (WieldLook.stage). Captions in the log.
 */
public final class TypeExhibits {

	private TypeExhibits(){}

	public static void exhibits( FxGallery.Page page ){

		page.add( "woodsman, quarry, bard, lash", 13f, s -> {
			final FxStage.Puppet axeman = s.puppet( Gnoll.class, s.cell( 2, 4 ) );
			final FxStage.Puppet log = s.puppet( Rat.class, s.cell( 1, 4 ) );
			final FxStage.Puppet hammer = s.puppet( Gnoll.class, s.cell( -3, -4 ) );
			final FxStage.Puppet stone = s.puppet( Rat.class, s.cell( -2, -4 ) );
			final FxStage.Puppet[] beside = { s.puppet( Rat.class, s.cell( -1, -4 ) ), s.puppet( Rat.class, s.cell( -2, -5 ) ),
					s.puppet( Rat.class, s.cell( -1, -5 ) ) };
			final FxStage.Puppet bard = s.puppet( Gnoll.class, s.cell( 6, 3 ) );
			final FxStage.Puppet tune = s.puppet( Rat.class, s.cell( 7, 3 ) );
			final FxStage.Puppet near = s.puppet( Rat.class, s.cell( 7, 4 ) );
			final FxStage.Puppet friend = s.puppet( Gnoll.class, s.cell( 5, 4 ) );
			final FxStage.Puppet whip = s.puppet( Gnoll.class, s.cell( -6, 2 ) );
			final FxStage.Puppet pulled = s.puppet( Rat.class, s.cell( -6, 4 ) );
			if (axeman == null || log == null || hammer == null || stone == null || bard == null || tune == null || near == null
					|| friend == null || whip == null || pulled == null) return;
			near.ch.alignment = Char.Alignment.ENEMY;
			friend.ch.alignment = Char.Alignment.ALLY;

			s.after( 0.3f, () -> {
				GLog.i( "  an axe splinters: two chips of bark off the struck, SHRED on its card" );
				blow( axeman, log, 6, () -> TypeFx.splinter( axeman.ch, log.ch, true ) );
			} );
			s.after( 1.1f, () -> {
				GLog.i( "  the felling blow: a wide wooden crescent, bark flung, the throw's wood ghosts and ring" );
				blow( axeman, log, 9, () -> {
					TypeFx.splinter( axeman.ch, log.ch, true );
					TypeFx.fell( axeman.ch, log.ch );
				} );
				push( log, s.cell( -1, 4 ), TypeFx.WOOD );
			} );
			s.after( 2.1f, () -> {
				GLog.i( "  felled into a wall: the crack, pebbles and the jolt of the slam" );
				axeman.place( s.cell( 0, 4 ) );
				blow( axeman, log, 9, () -> TypeFx.fell( axeman.ch, log.ch ) );
				push( log, s.cell( -3, 4 ), TypeFx.WOOD );
			} );
			s.after( 3.2f, () -> {
				GLog.i( "  a hammer dazes: two violet specks round the head once, the Daze chip" );
				blow( hammer, stone, 5, () -> {
					Blow.announce( stone.ch, new Daze() );
					TypeFx.daze( hammer.ch, stone.ch );
				} );
			} );
			s.after( 4.3f, () -> {
				GLog.i( "  an alpha's dazing blow cracks the floor: the crack leads, pebbles hop at each crippled neighbour" );
				blow( hammer, stone, 5, () -> {
					Blow.announce( stone.ch, new Daze() );
					List<Char> crippled = chars( beside );
					for (Char c : crippled) Blow.announce( c, new Cripple() );
					TypeFx.crack( hammer.ch, stone.ch, crippled );
				} );
			} );
			//the Bard: three notes held over the hand, then a rally chord; then three more and a dread
			for (int k = 1; k <= 3; k++){
				final int notes = k;
				s.after( 5.2f + 0.4f * (k - 1), () -> {
					if (notes == 1) GLog.i( "  a flute's notes: one rising a hit, a pip held over the hand a note, gold for a rally" );
					blow( bard, tune, 3, () -> TypeFx.note( bard.ch, false ) );
					WieldLook.stage( bard.sprite, notes, false, 3f );
				} );
			}
			s.after( 6.6f, () -> {
				GLog.i( "  the fourth note is the chord: the pips fly home, notes fan up, a ring, a plus over each blessed" );
				chord( bard, tune, false, Arrays.asList( bard.ch, friend.ch ), Collections.<Char>emptyList(), null );
			} );
			s.after( 8.0f, () -> {
				GLog.i( "  a dread chord, violet: an exclamation over each terrified, a note to the charmed one" );
				WieldLook.stage( bard.sprite, 3, true, 0.5f );
			} );
			s.after( 8.6f, () -> chord( bard, tune, true, Collections.<Char>emptyList(), Arrays.asList( (Char) tune.ch ), near.ch ) );
			s.after( 9.6f, () -> {
				GLog.i( "  the resonance: a ring round the struck, a star on each it strikes beside" );
				blow( bard, tune, 3, () -> TypeFx.resonance( bard.ch, tune.ch, new Flute(), chars( new FxStage.Puppet[]{ near } ) ) );
			} );
			s.after( 10.5f, () -> {
				GLog.i( "  a whip drags from two tiles: the crack, the taut line while it is pulled in" );
				blow( whip, pulled, 4, () -> {
					Blow.announce( pulled.ch, new Cripple() );
					TypeFx.lash( whip.ch, pulled.ch );
				} );
				push( pulled, s.cell( -6, 3 ), Pushing.PLAIN );
			} );
			s.after( 11.4f, () -> {
				GLog.i( "  an alpha's disarm: a spark knocked from the grip; its next blow lands halved, Disarmed on the card" );
				blow( whip, pulled, 4, () -> {
					Blow.announce( pulled.ch, new Disarmed() );
					TypeFx.unarm( whip.ch, pulled.ch );
				} );
			} );
			s.after( 12.3f, () -> blow( pulled, whip, 2, () -> Blow.chip( whip.ch, -(BuffIndicator.DISARMED + 1), 0 ) ) );
		} );

		page.add( "perk accents", 9f, s -> {
			final FxStage.Puppet crusher = s.puppet( Gnoll.class, s.cell( -3, -4 ) );
			final FxStage.Puppet sundered = s.puppet( Rat.class, s.cell( -2, -4 ) );
			final FxStage.Puppet dancer = s.puppet( Gnoll.class, s.cell( 0, -5 ) );
			final FxStage.Puppet partner = s.puppet( Rat.class, s.cell( 1, -5 ) );
			final FxStage.Puppet sword = s.puppet( Gnoll.class, s.cell( 3, -5 ) );
			final FxStage.Puppet pressed = s.puppet( Rat.class, s.cell( 4, -5 ) );
			final FxStage.Puppet guard = s.puppet( Gnoll.class, s.cell( 2, 4 ) );
			final FxStage.Puppet brute = s.puppet( Rat.class, s.cell( 1, 4 ) );
			final FxStage.Puppet mage = s.puppet( Gnoll.class, s.cell( -6, 2 ) );
			final FxStage.Puppet drained = s.puppet( Rat.class, s.cell( -5, 2 ) );
			final FxStage.Puppet archer = s.puppet( Gnoll.class, s.cell( 7, 3 ) );
			final FxStage.Puppet mark = s.puppet( Rat.class, s.cell( 7, -2 ) );
			final FxStage.Puppet lancer = s.puppet( Gnoll.class, s.cell( -1, 5 ) );
			final FxStage.Puppet reached = s.puppet( Rat.class, s.cell( 1, 5 ) );
			if (crusher == null || sundered == null || dancer == null || partner == null || sword == null || pressed == null
					|| guard == null || brute == null || mage == null || drained == null || archer == null || mark == null
					|| lancer == null || reached == null) return;

			s.after( 0.3f, () -> {
				GLog.i( "  a Crush sunder: two flakes of steel off the struck, SHRED while a stack is added" );
				blow( crusher, sundered, 7, () -> TypeFx.sunder( crusher.ch, sundered.ch, true ) );
			} );
			s.after( 1.1f, () -> {
				GLog.i( "  a Flurry's free fifth hit: a silver wink and a small burst at the weapon side" );
				blow( dancer, partner, 3, () -> TypeFx.flurry( dancer.ch, partner.ch ) );
			} );
			for (int k = 1; k <= 4; k++){
				final int stacks = k;
				s.after( 1.9f + 0.45f * (k - 1), () -> {
					if (stacks == 1) GLog.i( "  Momentum: a gold mote a stack, then a gold star at full; from three the star leans gold" );
					blow( sword, pressed, 4 + stacks, () -> TypeFx.momentum( sword.ch, pressed.ch, stacks ) );
				} );
			}
			s.after( 4.0f, () -> {
				GLog.i( "  a Bulwark blow spent: two steel motes from the chest into the weapon" );
				blow( guard, brute, 11, () -> TypeFx.bulwark( guard.ch, brute.ch ) );
			} );
			s.after( 4.8f, () -> {
				GLog.i( "  a Guard's riposte: the parry leads on the guard's card, the answer's number on the striker's" );
				blow( brute, guard, 0, () -> {
					TypeFx.parry( brute.ch, guard.ch, 8 );
					brute.sprite.showStatusWithIcon( CharSprite.NEGATIVE, "2", FloatingText.PHYS_DMG );
				} );
			} );
			s.after( 5.8f, () -> {
				GLog.i( "  a Siphon: a blue mote home, the mana's small card when it lands" );
				blow( mage, drained, 4, () -> {
					TypeFx.siphon( mage.ch, drained.ch );
					mage.sprite.showStatusWithIcon( TypeFx.MANA, "+2", FloatingText.MANA );
				} );
			} );
			s.after( 6.6f, () -> {
				GLog.i( "  a shot from afar: a silver mark, bigger at five tiles, LONGSHOT on the card" );
				blow( archer, mark, 8, () -> TypeFx.longshot( archer.ch, mark.ch, 5 ) );
			} );
			s.after( 7.4f, () -> {
				GLog.i( "  a Reach blow from two tiles: a silver tip, REACH on the card" );
				blow( lancer, reached, 5, () -> TypeFx.reach( lancer.ch, reached.ch ) );
			} );
		} );
	}

	//a blow of a on d through the ledger: its pieces, its number (none for 0), its blood, its flash and its star
	static void blow( FxStage.Puppet a, FxStage.Puppet d, int number, Runnable pieces ){
		if (a.sprite.parent == null || d.sprite.parent == null) return;
		a.sprite.attack( d.ch.pos );
		Blow.begin( a.ch, d.ch );
		try {
			pieces.run();
			if (number > 0){
				d.sprite.showStatusWithIcon( CharSprite.NEGATIVE, Integer.toString( number ), FloatingText.PHYS_DMG );
				d.sprite.bloodBurstA( a.sprite.center(), number );
				d.sprite.flash();
			}
			d.sprite.hitFx( a.sprite.x + a.sprite.width() / 2f, a.sprite.y + a.sprite.height() / 2f, number, HitFx.SLASH, false );
		} finally {
			Blow.end();
		}
	}

	//a chord played on the fourth note: the pips fly home, and the chord's blow
	private static void chord( FxStage.Puppet bard, FxStage.Puppet tune, boolean dread, List<Char> blessed, List<Char> terrified,
			Char charmed ){
		WieldLook.stage( bard.sprite, 0, dread, 0f );
		blow( bard, tune, 3, () -> {
			TypeFx.note( bard.ch, dread );
			TypeFx.chord( bard.ch, tune.ch, new Flute(), dread, blessed, terrified, charmed );
		} );
	}

	//a puppet thrown to a cell as the game throws it (Pushing's own look: in Woodsman's wood when an axe
	//throws it, plain when a whip pulls it), its cell set as it lands
	private static void push( final FxStage.Puppet p, final int to, int tint ){
		if (to == -1 || p.sprite.parent == null) return;
		Pushing push = new Pushing( p.ch, p.ch.pos, to, () -> p.place( to ), tint );
		push.new Effect();
	}

	//the characters of the puppets there are
	private static List<Char> chars( FxStage.Puppet[] ps ){
		ArrayList<Char> out = new ArrayList<>();
		for (FxStage.Puppet p : ps) if (p != null) out.add( p.ch );
		return out;
	}
}
