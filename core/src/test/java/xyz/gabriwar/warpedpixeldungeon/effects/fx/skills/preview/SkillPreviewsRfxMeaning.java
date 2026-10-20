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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillSequence;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BloodDance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.EmberArrows;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.FrostNova;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Grace;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Litany;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MassHeal;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Purify;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.MoteFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RogueHuntressAuras;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillRay;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewsMartialBefore.ROAR_RATS;

/**
 * The meaning package's moments (rfx, plan.md 5.4) as the new code shows them: the cleric's mends
 * flowing in the heal's green into its red crosses (Mass Heal, Litany, the dawn), Litany's spoken
 * runes, Healing Prayer and Purify, Grace's mana bloom, the mana gains drawing mana in, the
 * guardian's holy motes, Blood Dance and Heartseeker in the blood's and the heart's reds, Undying
 * Will's fury, Ember Arrows and the stance auras in fire's orange, Double Shot's mark in leaf green,
 * Frost Nova's ice; and, the kit's colours only, Toughness, Pyromancy's leap, Scouring Flame at reach
 * 5 and Shield of the Faithful. Each skill's own visual calls restaged (CellEmitter's bursts through
 * the scene's stand-in emitters). Its before side: the base dump (rfx/previews/_base/json-f0783) and,
 * for the new names, SkillPreviewsRfxMeaningBefore (f0783eacd's calls).
 */
public final class SkillPreviewsRfxMeaning {

	private SkillPreviewsRfxMeaning(){}

	static final HeroClass C = HeroClass.CLERIC;
	static final int GOLD = 0xF1D99C;

	public static void register( SkillPreviewDump.Registry r ){
		// ---------------------------------------------------------------- the cleric's heals
		r.entry( "mass-heal", s -> {
			SkillSpectacleFX.fly( SkillSpectacleFX.WINGS, s.hero.pos, s.hero.pos, 0, .65f, SkillPalette.HEAL_LIGHT );
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), SkillPalette.HEAL.stop( 0 ), PixelFlare.M, 1f );
			s.h.emitter().burst( SkillBursts.HOLY_MOTE, 6 );
			PulseRingFX.around( s.h, SkillPalette.HEAL_LIGHT, 40, 0.7f );
			FxTimeline.start()
					.at( 0.1f, () -> PulseRingFX.around( s.h, GOLD, 24, 0.55f ) )
					.at( 0.45f, () -> { s.h.emitter().burst( Speck.factory( Speck.HEALING ), 5 ); MoteFX.greens( s.h, 4 ); } );
			//a turn later the wings mend him and the allies about him
			s.later( 1f, () -> {
				s.h.emitter().burst( Speck.factory( Speck.HEALING ), 2 );
				for (CharSprite a : s.d){
					a.emitter().burst( Speck.factory( Speck.HEALING ), 2 );
					SkillRay.show( s.h, a, SkillRay.HEAL, MassHeal.FLOW );
				}
			} );
		} ).heroClass( C ).rat( 5, 3 ).rat( 4, 6 ).seconds( 2f );
		r.entry( "litany-cast", s -> {
			s.h.emitter().burst( SkillBursts.VERSE, 2 );
			Litany.spoken( s.h );
			s.h.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
		} ).heroClass( C );
		r.entry( "litany-mend", s -> {
			s.h.emitter().burst( SkillBursts.VERSE, 2 );
			Litany.spoken( s.h );
			s.h.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
			for (CharSprite a : s.d){
				a.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
				SkillRay.show( s.h, a, SkillRay.HEAL, MassHeal.FLOW );
			}
		} ).heroClass( C ).rat( 5, 3 ).rat( 4, 6 );
		r.entry( "healing-prayer", s -> {
			SkillSpectacleFX.show( SkillSpectacleFX.WINGS, s.hero.pos );
			s.h.emitter().start( Speck.factory( Speck.HEALING ), 0.4f, 4 );
			s.h.emitter().burst( SkillBursts.HOLY_MOTE, 5 );
			PulseRingFX.around( s.h, SkillPalette.HEAL_LIGHT, 14, 0.55f );
			FxTimeline.start()
					.at( 0.08f, () -> { PulseRingFX.around( s.h, GOLD, 10, 0.45f ); s.h.emitter().burst( Speck.factory( Speck.HEALING ), 4 ); MoteFX.greens( s.h, 4 ); } )
					.at( 0.42f, () -> s.h.emitter().burst( SkillBursts.HOLY_MOTE, 3 ) );
		} ).heroClass( C ).seconds( 1.5f );
		r.entry( "purify", s -> {
			for (CharSprite a : s.d){
				a.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
				SkillRay.show( s.h, a, SkillRay.HOLY, Purify.CLEANSE );
			}
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			s.h.emitter().burst( SkillBursts.HOLY_MOTE, 5 );
			PulseRingFX.around( s.h, SkillPalette.HOLY.stop( 0 ), 14, 0.45f );
			FxTimeline.start().at( 0.1f, () -> PulseRingFX.around( s.h, GOLD, 16, 0.4f ) );
		} ).heroClass( C ).rat( 6, 3 );
		r.entry( "grace", s -> {
			final CharSprite h = s.h;
			SkillBolt.fly( s.h.parent, s.d[0], s.hero.pos, SkillBolt.HOLY, () -> Grace.manaBloom( h ) );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 );
		} ).heroClass( C ).rat( 7, 4 );
		//Skill Sequence's dawn: the wings, and the heal's crosses and green light on each one it mended
		r.entry( "dawn-mend", s -> {
			SkillSpectacleFX.show( SkillSpectacleFX.WINGS, s.hero.pos );
			SkillSequence.mended( s.hero );
			for (CharSprite a : s.d) SkillSequence.mended( a.ch );
		} ).heroClass( C ).rat( 4, 4 ).rat( 2, 3 ).seconds( 1.5f );

		// ---------------------------------------------------------------- mana
		r.entry( "inner-peace", s -> {
			s.h.emitter().burst( SkillBursts.BREATH, 5 );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 );
			MoteFX.gather( s.h, 8, SkillPalette.MANA, 18f, 0.4f );
		} ).heroClass( C );
		//Faith answered, a Meditation pulse, Spirituality's jolt, Serene Focus calming a rat, 0.65 s apart
		r.entry( "mana-gains", s -> {
			new Flare( 6, 32 ).color( 0xFFEE88, true ).show( s.h, 1f );
			PulseRingFX.around( s.h, SkillPalette.MANA_LIGHT, 14, 0.5f );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 8 );
			MoteFX.gather( s.h, 8, SkillPalette.MANA, 18f, 0.4f );
			s.later( 0.65f, () -> {
				new Flare( 5, 18 ).color( 0x66CCFF, true ).show( s.h, 1f );
				s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 7 );
				MoteFX.gather( s.h, 8, SkillPalette.MANA, 18f, 0.4f );
			} );
			s.later( 0.75f, () -> new Flare( 8, 30 ).color( 0x99DDFF, true ).show( s.h, 0.8f ) );
			s.later( 1.3f, () -> {
				s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 6 );
				MoteFX.gather( s.h, 8, SkillPalette.MANA, 18f, 0.4f );
			} );
			s.later( 1.95f, () -> {
				s.d[0].emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 5 );
				s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 );
				MoteFX.gather( s.h, 8, SkillPalette.MANA, 18f, 0.4f );
			} );
		} ).heroClass( HeroClass.MAGE ).rat( 6, 4 ).seconds( 2.5f );
		//the seraph guardian fading in under its pillar, holy motes rising round it
		r.entry( "guardian-spirit", s -> SkillPreviewsRfxMeaningBefore.guardian( s, SkillBursts.HOLY_MOTE ) ).heroClass( C ).view( 1, 0, 9, 8 ).seconds( 2f );

		// ---------------------------------------------------------------- blood, the heart, fury
		r.entry( "blood-dance", s -> {
			s.h.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 6 );
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), BloodDance.CRIMSON, PixelFlare.M, 0.6f );
			ArcSpinFX.around( s.h, BloodDance.CRIMSON, BloodDance.RIBBON_INNER, 10, 0.5f, 225, BloodDance.RIBBON_SPIN, BloodDance.RIBBON_FOR );
			ArcSpinFX.around( s.h, BloodDance.RIBBON_INNER, BloodDance.RIBBON_INNER, 7, 0.35f, 45, -BloodDance.RIBBON_SPIN, BloodDance.RIBBON_FOR );
			FxTimeline t = FxTimeline.start();
			for (int i = 0; i < 8; i++){
				final int k = i;
				t.at( 0.05f * i, () -> BloodDance.step( s.h, k ) );
			}
		} ).heroClass( HeroClass.ROGUE );
		r.entry( "heartseeker-beats", SkillPreviewsHunt::heartseeker ).rat( 6, 4 ).heroClass( HeroClass.HUNTRESS ).seconds( 2.5f );
		r.entry( "toughness-held", s -> {
			s.h.emitter().burst( SkillBursts.METAL_SPARK, 6 );
			s.later( 0.5f, () -> s.h.emitter().burst( SkillBursts.HEARTBEAT, 3 ) );
			s.later( 1.0f, () -> s.h.emitter().burst( SkillBursts.HEARTBEAT, 3 ) );
		} ).seconds( 1.5f );
		r.entry( "undying-cast", s -> {
			s.h.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 5 );
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), ShoutFX.FURY[2], PixelFlare.M, 0.8f );
			PulseRingFX.around( s.h, ShoutFX.FURY[2], 20, 0.45f );
		} );
		r.entry( "undying-saved", s -> {
			s.h.flash();
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), ShoutFX.FURY[2], PixelFlare.L, 0.6f );
			ShoutFX.roar( s.h, ShoutFX.FURY, 40 );
		} ).rat( 4, 4 );
		SkillPreviewsMartialBefore.rats( r.entry( "undying-roar", s -> {
			for (CharSprite m : s.d){
				m.emitter().burst( SkillBursts.TREMBLE, 2 );
				ShoutFX.pull( m, SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), ShoutFX.FURY,
						Math.min( 0.3f, 0.05f + 0.03f * Dungeon.level.distance( s.hero.pos, m.ch.pos ) ) );
			}
			s.h.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 10 );
			s.h.emitter().burst( SkillBursts.ROAR, 4 );
			ShoutFX.roar( s.h, ShoutFX.FURY, 64 );
		} ), ROAR_RATS );

		// ---------------------------------------------------------------- fire, the huntress, frost
		r.entry( "ember-activate", s -> {
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 3 );
			s.h.emitter().burst( SkillBursts.EMBER_MOTE, 6 );
			EmberArrows.kindled( s.h, EmberArrows.HEAT, EmberArrows.FLARE );
		} ).heroClass( HeroClass.HUNTRESS );
		r.entry( "ember-leap", s -> {
			SkillPreviewsHunt.flames( s.d[0] );
			SkillBolt.fly( s.d[0].parent, s.d[0].center(), s.d[1].center(), SkillBolt.EMBER, () -> SkillPreviewsHunt.flames( s.d[1] ) );
		} ).rat( 5, 4 ).rat( 7, 3 ).heroClass( HeroClass.HUNTRESS );
		r.entry( "ember-kill", s -> {
			int pos = at( 6, 4 );
			s.center( pos ).burst( FlameParticle.FACTORY, 10 );
			EmberArrows.heatRing( pos );
			int ring = 0;
			for (int c : SkillPreviewsHunt.around( pos )) s.get( c ).startDelayed( SkillBursts.EMBER_MOTE, 0, 5, 0.05f + 0.03f * ring++ );
		} ).heroClass( HeroClass.HUNTRESS );
		r.entry( "pyre-leap", s -> {
			s.d[0].emitter().burst( FlameParticle.FACTORY, 6 );
			PixelFlare.show( SkillVfx.midX( s.d[0] ), SkillVfx.midY( s.d[0] ), 0xFFC070, PixelFlare.S, 0.35f );
			SkillBolt.fly( s.h.parent, s.d[0].ch.pos, s.d[1].ch.pos, SkillBolt.EMBER, () -> {
				s.d[1].emitter().burst( FlameParticle.FACTORY, 6 );
				s.d[1].flash();
			} );
		} ).rat( 5, 4 ).rat( 6, 5 ).heroClass( HeroClass.MAGE );
		//every stance aura of the rogue and the huntress, built for real on six figures: its two motes
		//in the stance's colour and its accent
		r.entry( "stance-accents", s -> SkillPreviewsRfxMeaningBefore.auras( s, RogueHuntressAuras.class ) )
				.heroClass( HeroClass.HUNTRESS ).hero( 5, 7 ).view( 0, 0, 11, 9 ).seconds( 2.5f );
		r.entry( "double-shot", s -> {
			s.h.emitter().burst( SkillBursts.FLOURISH, 6 );
			PixelFlare.show( SkillVfx.midX( s.d[0] ), SkillVfx.midY( s.d[0] ), SkillPalette.NATURE_LIGHT, PixelFlare.S, 0.6f );
			s.d[0].emitter().burst( SkillBursts.FLOURISH, 3 );
		} ).heroClass( HeroClass.HUNTRESS ).rat( 6, 4 );
		r.entry( "frost-nova", s -> ice( s, SkillPreviewsRfxMeaningBefore.nova( s ) ) ).heroClass( HeroClass.MAGE )
				.rat( 5, 4 ).rat( 4, 2 ).rat( 2, 6 ).view( 0, 0, 8, 8 ).seconds( 2f );

		// ---------------------------------------------------------------- the kit's colours only
		r.entry( "scouring-flame", s -> SkillPreviewsHolyBefore.scour( s, end -> SkillBolt.fly( s.h.parent, s.h, end, SkillBolt.HOLY, null ) ) )
				.heroClass( C ).rat( 5, 4 ).rat( 6, 3 ).seconds( 1.5f );
		r.entry( "shield-faithful-catch", SkillPreviewsHoly::shield ).heroClass( C ).rat( 4, 4 ).rat( 7, 2 ).seconds( 2.5f );
	}

	/** Frost Nova's new ice, for the shared nova: the shards out of him, rime under each caught. */
	static void ice( SkillPreviewDump.Scene s, int[] caught ){
		FrostNova.shards( s.h );
		for (int i = 0; i < caught.length && i < FrostNova.RIMES; i++){
			final int c = caught[i];
			s.later( 0.09f * Dungeon.level.distance( s.hero.pos, c ), () -> FrostNova.rime( c ) );
		}
	}
}
