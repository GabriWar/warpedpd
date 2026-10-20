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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BladeBind;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.IronStance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ManaShield;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MeteorCall;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Venom;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SpiritArmor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Transcendence;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillCastFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;

import java.lang.reflect.Constructor;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The runes package's new entries as f0783eacd shows the same moments (its calls only, so this
 * compiles there): dumped once in a detached f0783eacd worktree (FXPREVIEW_SIDE=before
 * FXPREVIEW_REGISTRIES=RfxRunes) into rfx/previews/runes/json-before-new. The entries f0783 already
 * had take their before from the base dump. Nothing showed before for a blow Mana Shield held.
 */
public final class SkillPreviewsRfxRunesBefore {

	private SkillPreviewsRfxRunesBefore(){}

	static final HeroClass MAGE = HeroClass.MAGE, CLERIC = HeroClass.CLERIC;

	public static void register( SkillPreviewDump.Registry r ){
		r.entry( "spirit-armor-cast", s -> {
			cast( new SpiritArmor(), s.hero );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 6 );
			new Flare( 6, 18 ).color( 0x66CCFF, true ).show( s.h, 0.5f );
		} ).heroClass( MAGE );
		r.entry( "mana-shield-raise", s -> {
			cast( new ManaShield(), s.hero );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 6 );
		} ).heroClass( MAGE );
		//f0783eacd's +3 break and reform: the break's flare and ring, its blue light and the reform's
		r.entry( "mana-shield-reform", s -> {
			ManaShieldWard.broke( s.h );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 10 );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 8 );
		} ).heroClass( MAGE ).seconds( 1.5f );
		r.entry( "mana-shield-absorb", s -> { } ).heroClass( MAGE ).rat( 4, 4 ).rat( 2, 5 ).seconds( 1.5f );
		r.entry( "casts-schools", s -> {
			Skill[] k = { new Venom(), new ManaShield(), new IronStance(), new BladeBind(), new Transcendence(), new MeteorCall() };
			HeroClass[] c = { HeroClass.HUNTRESS, MAGE, HeroClass.WARRIOR, HeroClass.DUELIST, CLERIC, MAGE };
			for (int i = 0; i < k.length; i++) side( i == 0 ? s.hero : caster( s, c[i], CASTERS[i] ), k[i] );
		} ).heroClass( HeroClass.HUNTRESS ).hero( CASTERS[0][0], CASTERS[0][1] ).view( 0, 1, 9, 7 );
	}

	static final int[][] CASTERS = { { 1, 3 }, { 4, 3 }, { 7, 3 }, { 1, 6 }, { 4, 6 }, { 7, 6 } };

	static void cast( Skill k, Hero h ){
		k.level = 1;
		SkillCastFX.play( k, h );
	}

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

	//f0783's cast made as its play made it (by its silhouette), each beside the others
	static void side( Hero h, Skill k ){
		try {
			Constructor<SkillCastFX> make = SkillCastFX.class.getDeclaredConstructor( Hero.class, int.class );
			make.setAccessible( true );
			h.sprite.parent.add( make.newInstance( h, SkillCastFX.sprite( k ) ) );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}
}
