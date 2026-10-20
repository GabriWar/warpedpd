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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ManaShieldWard;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Aegis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BladeBind;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Condemn;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Hex;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.IronStance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ManaShield;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MeteorCall;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sanctuary;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sorcerer;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SpellBlade;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SpiritArmor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Transcendence;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Venom;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.WeaponBond;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillCastFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SpiritArmorFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.AegisArcFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraFX;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;

import java.lang.reflect.Constructor;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The runes package (rfx plan 5.2) as the new code shows it: Spirit Volley's ward bolt and the rune
 * leaving the ring, the mana ward cast, Mana Shield raised, holding blows and breaking, Spell Blade's
 * spell on the blow, Wizard's ricochet, Arcane Echo, Sorcerer's overload winding up and thrown out,
 * Hex's words, Condemn's seal, Weapon Bond, Sanctuary, Aegis, and the casts of six schools side by
 * side. Its before side: SkillPreviewsRfxRunesBefore (f0783eacd's calls) for the new entries, the base
 * dump (rfx/previews/_base/json-f0783) for the rest. Spirit Armor's ring (an FxOrbit, drawn by its
 * sprite) is not dumped.
 */
public final class SkillPreviewsRfxRunes {

	private SkillPreviewsRfxRunes(){}

	static final HeroClass MAGE = HeroClass.MAGE, CLERIC = HeroClass.CLERIC;
	static final int GOLD = 0xF1D99C;

	public static void register( SkillPreviewDump.Registry r ){
		//SpiritArmorMotes.release: a bolt and a rune leaving the ring a target
		r.entry( "spirit-volley", s -> {
			for (CharSprite d : s.d){
				SkillBolt.fly( s.h.parent, s.h.center(), d.destinationCenter(), SkillBolt.RUNE, null );
				SpiritArmorFX.spend( s.hero, d.ch );
				d.flash();
			}
		} ).rat( 6, 3 ).rat( 6, 5 ).heroClass( MAGE );
		//SpiritArmor.execute (activate)
		r.entry( "spirit-armor-cast", s -> {
			cast( new SpiritArmor(), s.hero );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 6 );
			new Flare( 6, 18 ).color( 0x66CCFF, true ).show( s.h, 0.5f );
		} ).heroClass( MAGE );
		//ManaShield.execute
		r.entry( "mana-shield-raise", s -> {
			cast( new ManaShield(), s.hero );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 6 );
			ManaShield.raised( s.h );
		} ).heroClass( MAGE );
		//ManaShieldWard.absorbDamage: a blow from the east held, then one from the south-west
		r.entry( "mana-shield-absorb", s -> {
			ManaShieldWard.absorbed( s.h, s.d[0].ch.pos );
			s.later( 0.6f, () -> ManaShieldWard.absorbed( s.h, s.d[1].ch.pos ) );
		} ).heroClass( MAGE ).rat( 4, 4 ).rat( 2, 5 ).seconds( 1.5f );
		//ManaShieldWard.chargeWeapon
		r.entry( "mana-shield-break", s -> {
			ManaShieldWard.broke( s.h );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 10 );
		} ).heroClass( MAGE );
		//ManaShieldWard.absorbDamage at +3: the ward breaks into the weapon and is raised again, its
		//runes locking in once the broken ones have scattered
		r.entry( "mana-shield-reform", s -> {
			ManaShieldWard.broke( s.h );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 10 );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 8 );
			ManaShieldWard.reformed( s.h );
		} ).heroClass( MAGE ).seconds( 1.5f );
		//SpellBlade.onHitProc at +3, drinking: her blue light, the spell on the blow
		r.entry( "spell-blade", s -> {
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 5 );
			SpellBlade.inscribed( s.h, s.d[0] );
		} ).heroClass( MAGE ).rat( 4, 4 );
		//Wizard.ricochet (the kit's arcane bolt)
		r.entry( "wizard-ricochet", s -> {
			SkillBolt.fly( s.h.parent, s.d[0].ch.pos, s.d[1].ch.pos, SkillBolt.ARCANE, null );
			s.d[1].centerEmitter().burst( SkillBursts.ARCANE_SPARK, 6 );
		} ).rat( 5, 4 ).rat( 8, 5 ).heroClass( MAGE );
		r.entry( "arcane-echo", s -> SkillPreviewsHunt.echo( s, false ) ).rat( 4, 4 ).heroClass( MAGE );
		r.entry( "arcane-echo-chain", s -> SkillPreviewsHunt.echo( s, true ) ).rat( 4, 4 ).rat( 5, 5 ).heroClass( MAGE );
		//Sorcerer.onZap's overload, Overload.act arming (0.6 s) and discharging (1 s)
		r.entry( "sorcerer-overload", s -> {
			Sorcerer.overloaded( s.d[0] );
			final RuneFX.Runes held = Sorcerer.charging( s.d[0] );
			s.later( 0.6f, () -> s.d[0].centerEmitter().burst( SkillBursts.ARCANE_SPARK, 5 ) );
			s.later( 1.0f, () -> {
				Sorcerer.discharged( held, s.d[0].ch.pos );
				for (CharSprite d : s.d){
					if (d != s.d[0]) s.h.parent.add( new Lightning( s.d[0].ch.pos, d.ch.pos, null ).tint( Lightning.ARCANE ) );
					d.centerEmitter().burst( SkillBursts.ARCANE_SPARK, 6 );
					d.flash();
				}
			} );
		} ).rat( 6, 4 ).rat( 7, 3 ).rat( 7, 5 ).heroClass( MAGE ).seconds( 1.6f );
		//Hex.Curser: no character on the cell in the dump, so MagicMissile.boltFromChar's second way
		r.entry( "hex", s -> {
			final CharSprite ch = s.d[0];
			SkillBolt.fly( s.h.parent, s.h, ch.ch.pos, SkillBolt.HEX, () -> {
				ch.emitter().burst( SkillBursts.DREAD, 6 );
				ch.flash();
				ClosingRing.on( ch, 23, 5, 0.25f, ShoutFX.DREAD[3] );
				Hex.spoken( ch );
			} );
		} ).heroClass( MAGE ).rat( 7, 4 );
		//Condemn.onHitProc
		r.entry( "condemn", s -> {
			s.get( s.d[0].ch.pos ).burst( Speck.factory( Speck.LIGHT ), 3 );
			PixelFlare.show( SkillVfx.midX( s.d[0] ), SkillVfx.midY( s.d[0] ), GOLD, PixelFlare.M, 0.35f );
			Condemn.branded( s.d[0] );
			s.d[0].emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 4 );
		} ).heroClass( CLERIC ).rat( 4, 4 );
		//WeaponBond.onKill
		r.entry( "weapon-bond", s -> {
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), 0xB9DEEE, PixelFlare.S, 0.5f );
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
			WeaponBond.bonded( s.h );
		} ).heroClass( HeroClass.DUELIST );
		//Sanctuary.execute (activate), then the circle consecrated 0.9 s on (as SkillPreviewsHoly)
		r.entry( "sanctuary", s -> {
			cast( new Sanctuary(), s.hero );
			PulseRingFX.around( s.h, GOLD, 24, 0.5f );
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			Sanctuary.raised( s.h );
			s.h.parent.add( new StanceAuraFX( s.hero, 0xFFF1A1, SkillBursts.HOLY_MOTE, 1.6f ) );
			s.later( 0.9f, () -> {
				int[] cells = SkillPreviewsKitBefore.around( 3, 4 );
				SkillPreviewsKitBefore.field( SkillField.SANCTUARY, cells );
				for (int c : cells) SkillInteractions.flare( c, 0xFFF1A1 );
				PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), GOLD, PixelFlare.L, 0.8f );
			} );
		} ).heroClass( CLERIC ).seconds( 2.5f );
		//Aegis.onDefendProc
		r.entry( "aegis", s -> {
			AegisArcFX.flash( s.h, s.d[0].ch.pos, AegisArcFX.GOLD, 0.3f );
			Aegis.warded( s.h, s.d[0].ch.pos );
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), GOLD, PixelFlare.M, 0.5f );
			PulseRingFX.around( s.h, GOLD, 12, 0.35f );
			s.later( 0.9f, () -> PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), GOLD, PixelFlare.M, 0.4f ) );
		} ).heroClass( CLERIC ).rat( 4, 4 ).seconds( 1.5f );
		//six casts at once, each its own caster: a skill with no look of its own (the huntress's Venom:
		//plain, in her leaf green, no runes), a mana ward
		//(Mana Shield), a martial stance (Iron Stance), steel (Blade Bind), holy (Transcendence), fire
		//(Meteor Call); top row left to right, then the bottom row
		r.entry( "casts-schools", s -> {
			Skill[] k = { new Venom(), new ManaShield(), new IronStance(), new BladeBind(), new Transcendence(), new MeteorCall() };
			HeroClass[] c = { HeroClass.HUNTRESS, MAGE, HeroClass.WARRIOR, HeroClass.DUELIST, CLERIC, MAGE };
			for (int i = 0; i < k.length; i++) side( s, i == 0 ? s.hero : caster( s, c[i], CASTERS[i] ), k[i] );
		} ).heroClass( HeroClass.HUNTRESS ).hero( CASTERS[0][0], CASTERS[0][1] ).view( 0, 1, 9, 7 );
	}

	/** Where casts-schools' six casters stand. */
	static final int[][] CASTERS = { { 1, 3 }, { 4, 3 }, { 7, 3 }, { 1, 6 }, { 4, 6 }, { 7, 6 } };

	//a skill's cast, as Skill.castTextYell plays it (at its first level)
	static void cast( Skill k, Hero h ){
		k.level = 1;
		SkillCastFX.play( k, h );
	}

	//a hero of a class standing at a cell beside the scene's
	static Hero caster( SkillPreviewDump.Scene s, HeroClass c, int[] at ){
		Hero h = new Hero();
		h.HP = h.HT = 20;
		h.heroClass = c;
		h.pos = at( at[0], at[1] );
		HeroSprite hs = new HeroSprite( h );
		h.sprite = hs;
		s.h.parent.add( hs );
		hs.place( h.pos );
		hs.visible = true;
		return h;
	}

	//a cast on its own beside the others (SkillCastFX.play keeps one at a time: made as play makes it)
	static void side( SkillPreviewDump.Scene s, Hero h, Skill k ){
		try {
			k.level = 1;
			Constructor<SkillCastFX> make = SkillCastFX.class.getDeclaredConstructor( Hero.class, Skill.class );
			make.setAccessible( true );
			h.sprite.parent.add( make.newInstance( h, k ) );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}
}
