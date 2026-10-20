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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AvatarOfLightHalo;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.PanicHarvest;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SoulDetonation;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SoulDrain;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HolyFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.DreadFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.MoteFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillRay;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The links package (rfx plan.md section 5.3) as the new code shows it: Shadow Link's bind and its
 * held pair, the soul and the fear peeled out of their bodies, the mends shown as heals, Dark
 * Pact's seal, crimson madness, Venom's drops, the rays (Charged Shot, Avatar of Light, the split
 * lance), the strokes' gold dust and sparks, and Cinder Trail's launches. Only the entries whose
 * staging changed are here: the rest of the package's previews (dread-howl, demoralize-terror,
 * dark-pact, shadow-link, charged-draw, charged-release, cinder-gather, the spirit arrows' and holy
 * blades' flights, spark, storm-call, blink-chain) are the older registries' stagings, which call
 * this code. Their before side: the base dump (rfx/previews/_base/json-f0783) or, for the new
 * names, SkillPreviewsRfxLinksBefore.
 */
public final class SkillPreviewsRfxLinks {

	private SkillPreviewsRfxLinks(){}

	static final HeroClass CLERIC = HeroClass.CLERIC;
	static final int GOLD = 0xF1D99C;

	public static void register( SkillPreviewDump.Registry r ){
		// ---------------------------------------------------------------- shadow and the soul
		r.entry( "shadow-link-held", SkillPreviewsRfxLinksBefore::held ).heroClass( HeroClass.ROGUE )
				.rat( 5, 3 ).rat( 7, 5 ).rat( 5, 6 ).seconds( 2.5f );
		//SoulDrain.onEnemyDeath: the soul peeled out of the dead into its flight to the next
		r.entry( "soul-drain", s -> {
			final CharSprite next = s.d[1];
			s.d[0].die();
			SkillBolt.fly( s.h.parent, s.d[0].ch.pos, next.ch.pos, SkillBolt.SOUL,
					() -> ClosingRing.on( next, 23, 9, 0.3f, DreadFX.SOUL[2] ) ).peel( s.d[0], SoulDrain.PEEL );
			next.emitter().burst( SkillBursts.SOUL_WISP, 6 );
		} ).heroClass( HeroClass.MAGE ).rat( 4, 5 ).rat( 7, 3 );
		//the mend's crosses and green light as the soul reaches him (the bolt's callback)
		r.entry( "soul-drain-mend", s -> {
			s.d[0].die();
			SkillBolt.fly( s.h.parent, s.d[0].ch.pos, s.hero.pos, SkillBolt.SOUL, () -> {
				MoteFX.crosses( s.h, 2 );
				MoteFX.greens( s.h, 3 );
			} ).peel( s.d[0], SoulDrain.PEEL );
			s.h.emitter().burst( SkillBursts.SOUL_WISP, 4 );
		} ).heroClass( HeroClass.MAGE ).rat( 7, 4 );
		r.entry( "panic-harvest-reap", s -> {
			s.get( s.d[0].ch.pos ).burst( SkillBursts.SOUL_WISP, 5 );
			SkillBolt.fly( s.h.parent, s.d[0].center(), s.h.center(), SkillBolt.MANA,
					() -> s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 4 ) ).peel( s.d[0], PanicHarvest.PEEL );
		} ).heroClass( HeroClass.ROGUE ).rat( 6, 4 );
		r.entry( "soul-detonation", s -> {
			CharSprite pet = s.d[0];
			int origin = pet.ch.pos;
			SkillBolt.mote( pet, s.h, SkillBolt.SOUL, null ).peel( pet, SoulDetonation.PEEL );
			s.bottom( origin ).burst( SkillBursts.SOUL_WISP, 12 );
			pet.die();
			PixelFlare.show( SkillVfx.cx( origin ), SkillVfx.cy( origin ), DreadFX.SOUL[2], PixelFlare.M, 0.5f );
			s.later( 0.3f, () -> s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 ) );
		} ).heroClass( HeroClass.MAGE ).rat( 6, 4 );
		r.entry( "venom", s -> {
			s.h.emitter().burst( SkillBursts.FLOURISH, 4 );
			DreadFX.coat( s.h, SkillPalette.POISON.stop( 0 ) );
		} ).heroClass( HeroClass.ROGUE );

		// ---------------------------------------------------------------- holy rays and strokes
		r.entry( "avatar-sear", s -> {
			HolyFx.halo( s.h, 2 );
			s.later( 0.2f, () -> {
				CharSprite m = s.d[0];
				SkillRay.show( s.h, m, SkillRay.HOLY, AvatarOfLightHalo.SEAR_RAY );
				FxRecipes.embers( SkillVfx.midX( m ), SkillVfx.midY( m ), Element.HOLY, 4 );
				PixelFlare.show( SkillVfx.midX( m ), SkillVfx.midY( m ), GOLD, PixelFlare.S, 0.25f );
				m.flash();
			} );
		} ).heroClass( CLERIC ).rat( 5, 3 ).seconds( 1.5f );
		//AvatarOfLightHalo.act at +3: an ally inside mended, its heal shown
		r.entry( "avatar-halo-mend", s -> {
			HolyFx.halo( s.h, 2 );
			s.later( 0.2f, () -> {
				MoteFX.greens( s.d[0], AvatarOfLightHalo.MEND_GREENS );
				MoteFX.crosses( s.d[0], AvatarOfLightHalo.MEND_CROSSES );
			} );
		} ).heroClass( CLERIC ).rat( 4, 3 ).seconds( 1.5f );
		r.entry( "holy-charge", s -> {
			int from = s.hero.pos, dest = at( 6, 4 );
			StreakFX.dash( from, dest, GOLD, 0.45f, 0.3f, true, StreakFX.MOTES );
			for (int x = 3; x <= 6; x++) s.center( at( x, 4 ) ).burst( Speck.factory( Speck.LIGHT ), 3 );
			SkillPreviewsHolyBefore.charge( s, from, dest, () -> {
				s.d[0].flash();
				PixelFlare.show( SkillVfx.midX( s.d[0] ), SkillVfx.midY( s.d[0] ), GOLD, PixelFlare.M, 0.6f );
				s.d[0].emitter().burst( SkillBursts.HOLY_MOTE, 8 );
			} );
		} ).heroClass( CLERIC ).hero( 2, 4 ).rat( 7, 4 );
		r.entry( "true-edge", s -> {
			SkillPreviewsMartialBefore.trueEdge( s );
			StreakFX.show( s.hero.pos, at( 6, 4 ), 0xB9DEEE, 0.45f, 0.25f, StreakFX.SPARKS );
		} ).rat( 4, 4 ).rat( 5, 4 ).rat( 6, 4 ).heroClass( HeroClass.DUELIST );

		// ---------------------------------------------------------------- the mage's elements, the marks' mends
		r.entry( "cinder-launch", SkillPreviewsRfxLinksBefore::launch ).heroClass( HeroClass.MAGE ).rat( 7, 3 ).rat( 7, 6 );
		//SkillInteractions.Mark.act's SHARDS mend of an ally: the heal's own picture
		r.entry( "shards-heal", s -> {
			MoteFX.crosses( s.d[0], 2 );
			MoteFX.greens( s.d[0], 2 );
		} ).rat( 5, 4 );
		r.entry( "soul-field-mana", s -> PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), SkillPalette.MANA_LIGHT, PixelFlare.S, .2f ) )
				.heroClass( HeroClass.MAGE );
	}
}
