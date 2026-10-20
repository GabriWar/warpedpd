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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.DivineWrathGround;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SacredWeaponSwords;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ShieldOfTheFaithfulWard;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DivineWrath;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sanctuary;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Transcendence;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillCastFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HolyFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.AegisArcFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.HolyFlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.particles.Emitter;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The holy-light package's moments as the game draws them now: the same stages as
 * SkillPreviewsHolyBefore, each cleric skill's visual calls of this code restaged (CellEmitter's
 * bursts through the scene's stand-in emitters; a buff's held pictures through their own show).
 */
public final class SkillPreviewsHoly {

	private SkillPreviewsHoly(){}

	static final int GOLD = 0xF1D99C, HEAL = 0xB3D699;

	public static void register( SkillPreviewDump.Registry r ){
		r.entry( "divine-wrath-activate", s -> {
			s.h.emitter().burst( SkillBursts.HOLY_MOTE, 8 );
			PulseRingFX.around( s.h, DivineWrath.WRATH, 12, 0.4f );
			s.h.parent.add( new StanceAuraFX( s.hero, DivineWrath.WRATH, SkillBursts.HOLY_MOTE, 1.1f ) );
		} ).heroClass( SkillPreviewsHolyBefore.C ).seconds( 2f );
		r.entry( "divine-wrath-ground", SkillPreviewsHoly::wrathGround ).heroClass( SkillPreviewsHolyBefore.C ).rat( 5, 4 ).seconds( 2.5f );
		r.entry( "avatar-cast", s -> {
			s.h.emitter().start( SkillBursts.HOLY_MOTE, 0.3f, 12 );
			s.h.emitter().burst( SkillBursts.HOLY_MOTE, 8 );
			PillarRiseFX.show( s.hero.pos, GOLD, null );
			PulseRingFX.around( s.h, GOLD, 18, 0.6f );
			FxTimeline.start()
					.at( 0.25f, () -> { s.h.emitter().burst( SkillBursts.HOLY_MOTE, 5 ); PulseRingFX.around( s.h, 0xFFF4D0, 12, 0.45f ); } )
					.at( 0.6f, () -> s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 6 ) );
			HolyFx.halo( s.h, 2 );
		} ).heroClass( SkillPreviewsHolyBefore.C ).seconds( 2f );
		r.entry( "avatar-sear", s -> {
			HolyFx.halo( s.h, 2 );
			s.later( 0.2f, () -> {
				CharSprite m = s.d[0];
				StreakFX.show( s.hero.pos, m.ch.pos, GOLD, 0.3f, 0.15f );
				FxRecipes.embers( SkillVfx.midX( m ), SkillVfx.midY( m ), Element.HOLY, 4 );
				PixelFlare.show( SkillVfx.midX( m ), SkillVfx.midY( m ), GOLD, PixelFlare.S, 0.25f );
				m.flash();
			} );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 5, 3 ).seconds( 1.5f );
		r.entry( "aegis", s -> {
			AegisArcFX.flash( s.h, s.d[0].ch.pos, AegisArcFX.GOLD, 0.3f );
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), GOLD, PixelFlare.M, 0.5f );
			PulseRingFX.around( s.h, GOLD, 12, 0.35f );
			s.later( 0.9f, () -> PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), GOLD, PixelFlare.M, 0.4f ) );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 4, 4 ).seconds( 1.5f );
		r.entry( "holy-smite", s -> {
			int cell = s.d[0].ch.pos;
			SkillSpectacleFX.show( SkillSpectacleFX.HAMMER, cell );
			s.bottom( cell ).burst( Speck.factory( Speck.DUST ), 8 );
			s.d[0].flash();
			PixelFlare.show( SkillVfx.midX( s.d[0] ), SkillVfx.midY( s.d[0] ), GOLD, PixelFlare.L, 0.35f );
			PulseRingFX.at( cell, 0xFFE9A0, 16, 0.45f );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 5, 4 );
		r.entry( "holy-charge", s -> {
			int from = s.hero.pos, dest = at( 6, 4 );
			StreakFX.dash( from, dest, GOLD, 0.45f, 0.3f, true );
			for (int x = 3; x <= 6; x++) s.center( at( x, 4 ) ).burst( Speck.factory( Speck.LIGHT ), 3 );
			SkillPreviewsHolyBefore.charge( s, from, dest, () -> {
				s.d[0].flash();
				PixelFlare.show( SkillVfx.midX( s.d[0] ), SkillVfx.midY( s.d[0] ), GOLD, PixelFlare.M, 0.6f );
				s.d[0].emitter().burst( SkillBursts.HOLY_MOTE, 8 );
			} );
		} ).heroClass( SkillPreviewsHolyBefore.C ).hero( 2, 4 ).rat( 7, 4 );
		r.entry( "litany-cast", s -> {
			s.h.emitter().burst( SkillBursts.VERSE, 2 );
			s.h.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
		} ).heroClass( SkillPreviewsHolyBefore.C );
		r.entry( "litany-mend", s -> {
			s.h.emitter().burst( SkillBursts.VERSE, 2 );
			s.h.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
			for (CharSprite a : s.d){
				a.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
				SkillBolt.mote( s.h, a, SkillBolt.HEAL, null );
			}
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 5, 3 ).rat( 4, 6 );
		r.entry( "last-rites", s -> {
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 12 );
			s.h.emitter().burst( SkillBursts.HOLY_MOTE, 6 );
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), GOLD, PixelFlare.L, 1.2f );
			PillarRiseFX.show( s.hero.pos, GOLD, null );
		} ).heroClass( SkillPreviewsHolyBefore.C ).view( 0, 0, 9, 8 );
		r.entry( "grace", s -> {
			SkillBolt.fly( s.h.parent, s.d[0], s.hero.pos, SkillBolt.HOLY, null );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 7, 4 );
		r.entry( "pillar-of-light", s -> {
			int cell = s.d[0].ch.pos;
			PillarRiseFX.show( cell, 0xFFEE88, () -> { SkillInteractions.flare( cell, 0xFFF1A1 ); s.d[0].flash(); } );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 6, 4 ).view( 1, 0, 9, 8 );
		r.entry( "cathedral", s -> SkillPreviewsHolyBefore.cathedral( s, at( 6, 4 ) ) ).heroClass( SkillPreviewsHolyBefore.C ).rat( 6, 4 )
				.view( 2, 0, 9, 9 ).seconds( 2f );
		r.entry( "righteous-strikes", s -> {
			PixelFlare.show( SkillVfx.midX( s.d[0] ), SkillVfx.midY( s.d[0] ), GOLD, PixelFlare.M, 0.6f );
			s.d[0].emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 6 );
			FxTimeline chain = FxTimeline.start();
			for (int i = 0; i + 1 < s.d.length; i++){
				final CharSprite a = s.d[i], b = s.d[i + 1];
				chain.at( 0.1f * (i + 1), () -> leap( a, b ) );
			}
			final CharSprite last = s.d[s.d.length - 1];
			chain.at( 0.1f * (s.d.length + 1), () -> SkillBolt.mote( last, s.h, SkillBolt.HOLY,
					() -> FxRecipes.castBloom( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), Element.HOLY ) ) );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 4, 4 ).rat( 5, 2 ).rat( 7, 3 ).view( 1, 0, 9, 8 );
		r.entry( "retribution", s -> {
			s.d[0].emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 6 );
			s.d[0].flash();
			StreakFX.show( s.d[0].ch.pos, s.d[1].ch.pos, GOLD, 0.35f, 0.2f );
			PixelFlare.show( SkillVfx.midX( s.d[1] ), SkillVfx.midY( s.d[1] ), GOLD, PixelFlare.S, 0.25f );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 4, 4 ).rat( 5, 2 );
		r.entry( "purify", s -> {
			for (CharSprite a : s.d){
				a.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
				SkillBolt.mote( s.h, a, SkillBolt.HOLY, null );
			}
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			s.h.emitter().burst( SkillBursts.HOLY_MOTE, 5 );
			PulseRingFX.around( s.h, 0xFFFFFF, 14, 0.45f );
			FxTimeline.start().at( 0.1f, () -> PulseRingFX.around( s.h, GOLD, 16, 0.4f ) );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 6, 3 );
		r.entry( "mass-heal", s -> {
			SkillSpectacleFX.fly( SkillSpectacleFX.WINGS, s.hero.pos, s.hero.pos, 0, .65f, HEAL );
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), HEAL, PixelFlare.M, 1f );
			s.h.emitter().burst( SkillBursts.HOLY_MOTE, 6 );
			PulseRingFX.around( s.h, HEAL, 40, 0.7f );
			FxTimeline.start()
					.at( 0.1f, () -> PulseRingFX.around( s.h, GOLD, 24, 0.55f ) )
					.at( 0.45f, () -> s.h.emitter().burst( Speck.factory( Speck.HEALING ), 5 ) );
			s.later( 1f, () -> {
				s.h.emitter().burst( Speck.factory( Speck.HEALING ), 2 );
				for (CharSprite a : s.d){
					a.emitter().burst( Speck.factory( Speck.HEALING ), 2 );
					SkillBolt.mote( s.h, a, SkillBolt.HEAL, null );
				}
			} );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 5, 3 ).rat( 4, 6 ).seconds( 2f );
		r.entry( "scouring-flame", s -> SkillPreviewsHolyBefore.scour( s, end -> SkillBolt.fly( s.h.parent, s.h, end, SkillBolt.HOLY, null ) ) )
				.heroClass( SkillPreviewsHolyBefore.C ).rat( 5, 4 ).rat( 6, 3 ).seconds( 1.5f );
		r.entry( "sanctuary", s -> {
			SkillCastFX.play( new Sanctuary(), s.hero );
			PulseRingFX.around( s.h, GOLD, 24, 0.5f );
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			s.h.parent.add( new StanceAuraFX( s.hero, 0xFFF1A1, SkillBursts.HOLY_MOTE, 1.6f ) );
			s.later( 0.9f, () -> {
				int[] cells = SkillPreviewsKitBefore.around( 3, 4 );
				SkillPreviewsKitBefore.field( SkillField.SANCTUARY, cells );
				for (int c : cells) SkillInteractions.flare( c, 0xFFF1A1 );
				PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), GOLD, PixelFlare.L, 0.8f );
			} );
		} ).heroClass( SkillPreviewsHolyBefore.C ).seconds( 2.5f );
		r.entry( "reckoning", s -> {
			FxTimeline flashes = FxTimeline.start();
			for (int i = 0; i < s.d.length; i++){
				final CharSprite m = s.d[i];
				final boolean pillar = i != 1;
				flashes.at( 0.12f * i, () -> {
					if (pillar) PillarRiseFX.show( m.ch.pos, GOLD, null );
					else s.get( m.ch.pos ).burst( Speck.factory( Speck.LIGHT ), 4 );
					SkillInteractions.flare( m.ch.pos, 0xFFF1A1 );
					m.flash();
				} );
			}
			s.h.emitter().start( SkillBursts.HOLY_MOTE, 0.3f, 10 );
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), GOLD, PixelFlare.L, 0.8f );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 4, 3 ).rat( 5, 5 ).rat( 6, 3 ).view( 0, 0, 9, 8 ).seconds( 1.5f );
		r.entry( "fervor-kindle", s -> s.h.emitter().burst( SkillBursts.HOLY_MOTE, 10 ) ).heroClass( SkillPreviewsHolyBefore.C );
		r.entry( "fervor-eruption", s -> {
			s.d[0].emitter().burst( HolyFlameParticle.FACTORY, 12 );
			PixelFlare.show( SkillVfx.midX( s.d[0] ), SkillVfx.midY( s.d[0] ), GOLD, PixelFlare.M, 0.6f );
			for (int i = 1; i < s.d.length; i++) s.d[i].emitter().burst( HolyFlameParticle.FACTORY, 6 );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 4, 4 ).rat( 5, 3 ).rat( 5, 5 );
		r.entry( "shield-faithful-catch", SkillPreviewsHoly::shield ).heroClass( SkillPreviewsHolyBefore.C ).rat( 4, 4 ).rat( 7, 2 ).seconds( 2.5f );
		r.entry( "transcendence", s -> {
			int from = s.hero.pos, land = at( 5, 4 );
			s.center( from ).burst( Speck.factory( Speck.LIGHT ), 6 );
			s.hero.pos = land;
			s.h.place( land );
			StreakFX.dash( from, land, GOLD, 0.45f, 0.25f, true );
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 8 );
			for (CharSprite b : s.d){
				b.flash();
				LanceFX.stab( SkillVfx.midX( b ), b.y - Transcendence.LANCE_OVER, -90, Transcendence.HOLY, Transcendence.LANCE, Transcendence.LANCE_LIFE );
				s.center( b.ch.pos ).burst( Speck.factory( Speck.LIGHT ), 5 );
			}
			for (int end : new int[]{ at( 7, 4 ), at( 7, 6 ), at( 7, 2 ) }) StreakFX.show( land, end, GOLD, 0.35f, 0.2f );
		} ).heroClass( SkillPreviewsHolyBefore.C ).hero( 2, 4 ).rat( 3, 4 ).rat( 4, 4 );
		r.entry( "sacred-plant", s -> {
			int cell = at( 6, 4 );
			final SacredWeaponSwords.Blades[] b = new SacredWeaponSwords.Blades[1];
			SkillFX.rain( cell, new Sword(), SkillFX.Fall.HOLY_BLADE, 1, () -> {
				s.center( cell ).burst( Speck.factory( Speck.LIGHT ), 6 );
				b[0].landed( cell );
			} );
			b[0] = SacredWeaponSwords.Blades.show( new int[]{ cell }, new int[]{ cell } );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 5, 4 ).view( 1, 0, 9, 8 );
		r.entry( "sacred-cut", s -> {
			SacredWeaponSwords.Blades.show( new int[]{ at( 6, 4 ) }, new int[0] );
			s.later( 0.3f, () -> {
				SkillSpectacleFX.fly( SkillSpectacleFX.SABER, at( 6, 4 ), s.d[0].ch.pos, 0, .25f, SacredWeaponSwords.Blades.GOLD );
				s.d[0].flash();
			} );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 5, 4 );
		r.entry( "sacred-throw", s -> {
			SacredWeaponSwords.Blades b = SacredWeaponSwords.Blades.show( new int[]{ at( 6, 4 ) }, new int[0] );
			s.later( 0.3f, () -> {
				SkillFX.streak( at( 6, 4 ), s.d[0].ch.pos, SkillFX.Streak.HOLY_BLADE, new Sword(), () -> s.d[0].flash() );
				s.center( at( 6, 4 ) ).burst( Speck.factory( Speck.LIGHT ), 4 );
				b.fadeExcept( new int[0] );
			} );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 8, 2 ).view( 2, 0, 9, 7 );
		r.entry( "healing-prayer", s -> {
			SkillSpectacleFX.show( SkillSpectacleFX.WINGS, s.hero.pos );
			s.h.emitter().start( Speck.factory( Speck.HEALING ), 0.4f, 4 );
			s.h.emitter().burst( SkillBursts.HOLY_MOTE, 5 );
			PulseRingFX.around( s.h, HEAL, 14, 0.55f );
			FxTimeline.start()
					.at( 0.08f, () -> { PulseRingFX.around( s.h, GOLD, 10, 0.45f ); s.h.emitter().burst( Speck.factory( Speck.HEALING ), 4 ); } )
					.at( 0.42f, () -> s.h.emitter().burst( SkillBursts.HOLY_MOTE, 3 ) );
		} ).heroClass( SkillPreviewsHolyBefore.C ).seconds( 1.5f );
		r.entry( "condemn", s -> {
			s.get( s.d[0].ch.pos ).burst( Speck.factory( Speck.LIGHT ), 3 );
			PixelFlare.show( SkillVfx.midX( s.d[0] ), SkillVfx.midY( s.d[0] ), GOLD, PixelFlare.M, 0.35f );
			s.d[0].emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 4 );
		} ).heroClass( SkillPreviewsHolyBefore.C ).rat( 4, 4 );
		r.entry( "second-wind", s -> {
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), HEAL, PixelFlare.M, 0.6f );
			PulseRingFX.around( s.h, HEAL, 16, 0.4f );
			s.h.emitter().burst( SkillBursts.BREATH, 5 );
		} ).heroClass( SkillPreviewsHolyBefore.C );
		r.entry( "inner-peace", s -> s.h.emitter().burst( SkillBursts.BREATH, 5 ) ).heroClass( SkillPreviewsHolyBefore.C );
		r.entry( "regeneration", s -> s.h.emitter().burst( SkillBursts.BREATH, 3 ) ).heroClass( SkillPreviewsHolyBefore.C );
	}

	// ------------------------------------------------------------------ the moments now

	//Divine Wrath at +2 on a blow: the struck rat's glints and star, holy motes off all nine cells,
	//the ground laid (a glint on each new cell, holy motes poured, a halo of light opening on each), a turn
	//later it burns the rat (its halo flaring), and at 2 s the ground goes (the halos fade)
	static void wrathGround( SkillPreviewDump.Scene s ){
		CharSprite rat = s.d[0];
		int[] cells = SkillPreviewsKitBefore.around( 5, 4 );
		rat.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 5 );
		rat.flash();
		PixelFlare.show( SkillVfx.midX( rat ), SkillVfx.midY( rat ), GOLD, PixelFlare.S, 0.3f );
		for (int c : cells) s.floor( c ).burst( SkillBursts.HOLY_MOTE, 2 );
		for (int c : cells) s.get( c ).burst( Speck.factory( Speck.YELLOW_LIGHT ), 3 );
		final Emitter[] pours = new Emitter[cells.length];
		for (int i = 0; i < cells.length; i++){
			pours[i] = s.get( cells[i] );
			pours[i].pour( SkillBursts.HOLY_MOTE, 0.3f );
		}
		final DivineWrathGround.Hallow g = DivineWrathGround.Hallow.show( cells, cells );
		s.later( 1f, () -> {
			rat.flash();
			s.center( rat.ch.pos ).burst( Speck.factory( Speck.YELLOW_LIGHT ), 3 );
			g.flare( rat.ch.pos );
			PixelFlare.show( SkillVfx.cx( rat.ch.pos ), SkillVfx.cy( rat.ch.pos ), GOLD, PixelFlare.S, 0.25f );
		} );
		s.later( 2f, () -> {
			for (Emitter e : pours) e.on = false;
			g.fadeExcept( new int[0] );
		} );
	}

	//a leap of Righteous Strikes' light: a stroke of gold, a star, the glints
	static void leap( CharSprite a, CharSprite b ){
		StreakFX.show( a.ch.pos, b.ch.pos, GOLD, 0.35f, 0.2f );
		PixelFlare.show( SkillVfx.midX( b ), SkillVfx.midY( b ), GOLD, PixelFlare.S, 0.25f );
		b.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 4 );
	}

	//Shield of the Faithful at +3 holding three blows: the arc at his side facing the rat, the
	//second catch a shot from afar (the arc sweeps to it, a bolt thrown back), the third breaking it
	static void shield( SkillPreviewDump.Scene s ){
		final int[] state = { s.d[0].ch.pos, 0, 0 }; //facing, cracks, broken
		final CharSprite owner = s.h;
		final AegisArcFX arc = AegisArcFX.hold( owner, new AegisArcFX.Ward(){
			@Override
			public int facing(){
				return state[0];
			}
			@Override
			public float rest(){
				return state[2] > 0 ? 0f : ShieldOfTheFaithfulWard.rest( state[1] );
			}
		}, AegisArcFX.GOLD );
		for (int k = 0; k < 3; k++){
			final int n = k;
			s.later( 0.4f + 0.6f * k, () -> {
				if (n == 1) state[0] = s.d[1].ch.pos;
				state[1]++;
				arc.caught();
				owner.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
				if (n == 1) SkillBolt.fly( owner.parent, owner, s.d[1].ch.pos, SkillBolt.HOLY, null );
				if (n == 2){
					state[1] = 0;
					state[2] = 1;
					s.center( s.hero.pos ).burst( Speck.factory( Speck.LIGHT ), 8 );
				}
			} );
		}
	}
}
