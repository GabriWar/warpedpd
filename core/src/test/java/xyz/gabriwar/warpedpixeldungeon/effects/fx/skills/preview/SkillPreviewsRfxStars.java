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

import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Accuracy;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BladeBind;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LinkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.PointF;

/**
 * The rfx-stars package's moments as this code draws them (rfx plan 5.5, owner ask 4 with 6): the
 * same stages as SkillPreviewsRfxStarsBefore and, for the entries the base dump already has
 * (aplomb, dragon-kick-wall, accuracy, blade-bind), as SkillPreviewsMartial staged them at
 * f0783eacd. Each Speck.STAR burst is the same call with its new factory: a real daze's amber
 * twinkle (DAZE), a boss only shuddering (TREMBLE), steel sparks (METAL_SPARK), a steel glint
 * (STEEL_GLINT), the hero's sparkle (FLOURISH), the Trumpet's blessing in holy gold (HOLY_MOTE)
 * and its held foes' twinkle (DAZE), the Wardrum's terrified shuddering (TREMBLE) where the alarm's
 * brackets were; Accuracy and Second Wind in steel; Blade Bind's
 * link pulling steel from both blades to a knot that snaps on the Bite's frame.
 */
public final class SkillPreviewsRfxStars {

	private SkillPreviewsRfxStars(){}

	public static void register( SkillPreviewDump.Registry r ){
		// ---------------------------------------------------------------- on the base dump's stages
		//Aplomb: the blow bounces off him, the attacker dazed
		r.entry( "aplomb", s -> {
			s.h.emitter().burst( SkillBursts.METAL_SPARK, 4 );
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), 0xB9DEEE, PixelFlare.S, 0.3f );
			s.d[0].emitter().burst( SkillBursts.DAZE, 4 );
		} ).rat( 4, 4 );
		//Dragon Kick slams it into a wall: dazed
		r.entry( "dragon-kick-wall", s -> {
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), 0xD9AE75, PixelFlare.M, 0.5f );
			Wound.hit( s.d[0].ch );
			s.d[0].emitter().burst( SkillBursts.DAZE, 4 );
			s.center( s.d[0].ch.pos ).burst( SkillBursts.STONE_CHIP, 5 );
		} ).rat( 4, 4 ).heroClass( HeroClass.DUELIST );
		//Accuracy's bullseye: the eyes found, in the arrowhead's steel
		r.entry( "accuracy", s -> PixelFlare.show( SkillVfx.midX( s.d[0] ), s.d[0].y + 3, Accuracy.BULLSEYE, PixelFlare.S, 0.5f ) )
				.rat( 6, 4 ).heroClass( HeroClass.HUNTRESS );
		//BladeBind.bind: the two steel tethers, the Bite on the chain's frame, and the link between the
		//Bite's own two points pulling steel off both blades to a knot that snaps on that frame
		r.entry( "blade-bind", s -> {
			final CharSprite ch = s.d[0];
			PointF a = s.h.center(), b = ch.center();
			SkillMarkFX.tether( s.hero, ch.ch, BladeBind.STEEL, BladeBind.TETHER_LIFE, -2 );
			SkillMarkFX.tether( s.hero, ch.ch, BladeBind.STEEL, BladeBind.TETHER_LIFE, 2 );
			PointF biteFrom = new PointF( a.x + 4, a.y + 2 ), biteTo = new PointF( b.x - 3, b.y + 3 );
			s.h.parent.add( new BladeBind.Bite( biteFrom, biteTo, () -> {
				ch.flash();
				ch.emitter().burst( SkillBursts.METAL_SPARK, 5 );
			} ) );
			LinkFX.bind( s.h.parent, biteFrom, biteTo, LinkFX.STEEL, null );
			ch.emitter().burst( SkillBursts.METAL_SPARK, 6 );
			ch.flash();
			s.center( ch.ch.pos ).burst( SkillBursts.METAL_SPARK, 8 );
		} ).rat( 4, 4 ).heroClass( HeroClass.DUELIST );

		// ---------------------------------------------------------------- on this package's stages
		r.entry( "lunge", s -> SkillPreviewsRfxStarsBefore.lunge( s, SkillBursts.DAZE ) )
				.hero( 2, 4 ).rat( 6, 4 ).heroClass( HeroClass.DUELIST ).seconds( 1.5f );
		r.entry( "sixth-sense", s -> SkillPreviewsRfxStarsBefore.sixthSense( s, SkillBursts.DAZE ) )
				.heroClass( HeroClass.HUNTRESS ).rat( 4, 4 ).seconds( 1.5f );
		r.entry( "second-wind", s -> SkillPreviewsRfxStarsBefore.secondWind( s, SkillPalette.STEEL_LIGHT ) )
				.heroClass( HeroClass.DUELIST ).seconds( 1.5f );
		r.entry( "daze-sheet", s -> SkillPreviewsRfxStarsBefore.dazeSheet( s, SkillBursts.DAZE, SkillBursts.STEEL_GLINT ) )
				.view( 0, 0, 9, 8 ).hero( 8, 7 ).rat( 1, 2 ).rat( 4, 2 ).rat( 7, 2 ).rat( 1, 5 ).rat( 4, 5 ).rat( 7, 5 ).seconds( 1.5f );
		r.entry( "last-stand", s -> SkillPreviewsRfxStarsBefore.lastStand( s, SkillBursts.DAZE, SkillBursts.TREMBLE ) )
				.hero( 4, 4 ).rat( 5, 4 ).seconds( 2f );
		r.entry( "steel-sparks", s -> SkillPreviewsRfxStarsBefore.steelSparks( s, SkillBursts.METAL_SPARK ) )
				.rat( 5, 2 ).rat( 6, 4 ).rat( 5, 6 ).rat( 7, 2 ).view( 1, 0, 9, 8 ).seconds( 2f );
		r.entry( "steel-glints", s -> SkillPreviewsRfxStarsBefore.steelGlints( s, SkillBursts.STEEL_GLINT ) )
				.heroClass( HeroClass.DUELIST ).rat( 5, 4 ).rat( 6, 2 ).seconds( 2f );
		r.entry( "flourish-hero", s -> SkillPreviewsRfxStarsBefore.flourishHero( s, SkillBursts.FLOURISH, SkillBursts.FLOURISH,
				SkillBursts.FLOURISH, SkillBursts.FLOURISH ) ).hero( 4, 4 ).heroClass( HeroClass.HUNTRESS ).seconds( 2f );
		r.entry( "weapon-abilities", s -> SkillPreviewsRfxStarsBefore.weaponAbilities( s, SkillBursts.HOLY_MOTE, SkillBursts.FLOURISH,
				SkillBursts.METAL_SPARK, SkillBursts.DAZE, SkillBursts.TREMBLE ) ).hero( 4, 4 ).heroClass( HeroClass.DUELIST ).rat( 2, 3 ).rat( 7, 4 ).rat( 5, 5 ).rat( 5, 3 )
				.view( 0, 1, 9, 7 ).seconds( 2f );
	}
}
