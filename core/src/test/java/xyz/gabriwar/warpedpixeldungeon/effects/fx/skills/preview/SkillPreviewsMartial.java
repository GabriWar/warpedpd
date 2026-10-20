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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BladeBind;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BloodDance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Hamstring;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.LockSmith;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.StoneSkin;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SummonCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Warbreaker;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.BannerFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfBlast;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuardSprite;
import com.watabou.utils.PointF;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewsMartialBefore.BANNER;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewsMartialBefore.ROAR_RATS;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewsMartialBefore.TRAPS;

/**
 * The martial-steel package's moments as the game draws them now: the same stages as
 * SkillPreviewsMartialBefore, each skill's visual calls of this code restaged (CellEmitter's bursts
 * through the scene's stand-in emitters; a skill's own helpers called as the skill calls them).
 */
public final class SkillPreviewsMartial {

	private SkillPreviewsMartial(){}

	public static void register( SkillPreviewDump.Registry r ){
		//UndyingWill.execute: the red lights, a fury flare and a ring beating out
		r.entry( "undying-cast", s -> {
			s.h.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 5 );
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), ShoutFX.FURY[2], PixelFlare.M, 0.8f );
			PulseRingFX.around( s.h, ShoutFX.FURY[2], 20, 0.45f );
		} );
		//UndyingWill.incomingDamageReduction: death refused
		r.entry( "undying-saved", s -> {
			s.h.flash();
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), ShoutFX.FURY[2], PixelFlare.L, 0.6f );
			ShoutFX.roar( s.h, ShoutFX.FURY, 40 );
		} ).rat( 4, 4 );
		//UndyingWillWard.roar
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
		//StoneSkin: a plate breaks (and shards at the attacker); a plate grows back
		r.entry( "stone-skin-break", s -> {
			s.h.emitter().burst( SkillBursts.STONE_CHIP, 6 );
			StoneSkin.crack( s.h );
			s.center( s.d[0].ch.pos ).burst( SkillBursts.STONE_CHIP, 5 );
		} ).rat( 4, 4 );
		r.entry( "stone-skin-regrow", s -> {
			s.h.emitter().burst( SkillBursts.GRIT, 3 );
			StoneSkin.regrow( s.h );
		} );
		//Endurance.stomp
		r.entry( "endurance-stomp", s -> {
			SkillPreviewsMartialBefore.land( s, s.hero.pos );
			for (int c : SkillPreviewsMartialBefore.ring( s.hero.pos )){
				s.later( 0.1f, () -> {
					s.get( c ).burst( SkillBursts.GRIT, 2 );
					s.bottom( c ).burst( EarthParticle.FACTORY, 1 );
					WarriorImpactFX.show( c );
				} );
			}
			s.later( 0.1f, () -> { for (CharSprite m : s.d) m.flash(); } );
			s.h.emitter().burst( SkillBursts.METAL_SPARK, 6 );
		} ).rat( 4, 4 ).rat( 2, 3 );
		//Groundwork.onHitProc
		r.entry( "groundwork", s -> {
			s.get( s.d[0].ch.pos ).burst( SkillBursts.STONE_CHIP, 4 );
			for (int k = 1; k < s.d.length; k++){
				final int at = s.d[k].ch.pos;
				s.later( 0.05f * (k - 1), () -> {
					s.get( at ).burst( SkillBursts.GRIT, 3 );
					WarriorImpactFX.show( at );
				} );
			}
		} ).rat( 4, 4 ).rat( 5, 3 ).rat( 5, 5 );
		//KnockBack: the +3 crush; the stance (StanceAura's KNOCKBACK trickle)
		r.entry( "knockback-crush", s -> {
			s.get( s.d[0].ch.pos ).burst( SkillBursts.STONE_CHIP, 4 );
			WarriorImpactFX.show( s.d[0].ch.pos );
		} ).rat( 4, 4 );
		r.entry( "knockback-stance", s -> SkillPreviewsMartialBefore.stance( s, SkillBursts.METAL_SPARK, SkillBursts.METAL_SPARK ) ).seconds( 2.5f );
		//FirmHand's stagger (beside it, a real stun's stars)
		r.entry( "stagger-mark", s -> {
			MartialPreviewStage.mark( s, s.d[0].ch, SkillInteractions.Mark.STAGGER, 2 );
			s.d[0].emitter().burst( SkillBursts.METAL_SPARK, 5 );
			MartialPreviewStage.stunStars( s.d[1] );
		} ).rat( 5, 4 ).rat( 7, 4 ).seconds( 2.5f );
		//Shieldbearer
		r.entry( "shieldbearer-bash", s -> {
			SummonedPet g = MartialPreviewStage.pet( s, GuardSprite.class, at( 5, 4 ) );
			PixelFlare.show( SkillVfx.midX( g.sprite ), SkillVfx.midY( g.sprite ), 0xFFDD88, PixelFlare.M, 0.7f );
			for (CharSprite m : s.d) s.get( m.ch.pos ).burst( SkillBursts.STONE_CHIP, 3 );
			SkillPreviewsMartialBefore.bash( s, at( 5, 4 ) );
			MartialPreviewStage.orbits( g.sprite );
		} ).rat( 6, 3 ).rat( 6, 5 ).seconds( 1.5f );
		//the bound mark (the same stage: the marks are made by the pets themselves)
		r.entry( "summons-marked", SkillPreviewsMartialBefore::summons ).rat( 7, 4 ).seconds( 2f );
		//SummonCrab: the fade and its arrival
		r.entry( "summon-crab-arrival", s -> {
			SkillPreviewsMartialBefore.crab( s );
			SummonCrab.arrival( at( 4, 4 ) );
		} );
		//Toughness
		r.entry( "toughness-held", s -> {
			s.h.emitter().burst( SkillBursts.METAL_SPARK, 6 );
			s.later( 0.5f, () -> s.h.emitter().burst( SkillBursts.HEARTBEAT, 3 ) );
			s.later( 1.0f, () -> s.h.emitter().burst( SkillBursts.HEARTBEAT, 3 ) );
		} ).seconds( 1.5f );
		//Warbreaker
		r.entry( "warbreaker", s -> {
			Wound.hit( s.d[0].ch );
			s.d[0].emitter().burst( SkillBursts.METAL_SPARK, 5 );
			Warbreaker.shatter( s.d[0] );
			s.d[1].emitter().burst( SkillBursts.METAL_SPARK, 3 );
		} ).rat( 4, 4 ).rat( 5, 3 );
		//Challenge
		r.entry( "challenge", s -> {
			for (CharSprite m : s.d){
				m.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 3 );
				ShoutFX.pull( m, SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), ShoutFX.MARTIAL,
						Math.min( 0.3f, 0.05f + 0.03f * Dungeon.level.distance( s.hero.pos, m.ch.pos ) ) );
			}
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), SkillVfx.classColor(), PixelFlare.M, 0.5f );
			s.h.emitter().burst( SkillBursts.FLOURISH, 6 );
		} ).rat( 5, 2 ).rat( 6, 5 );
		//ComboOpener
		r.entry( "combo-opener", s -> {
			PixelFlare.show( SkillVfx.midX( s.d[0] ), SkillVfx.midY( s.d[0] ), 0xD9AE75, PixelFlare.M, 0.5f );
			SkillPreviewsMartialBefore.land( s, s.d[0].ch.pos );
		} ).rat( 4, 4 );
		//Aplomb
		r.entry( "aplomb", s -> {
			s.h.emitter().burst( SkillBursts.METAL_SPARK, 4 );
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), 0xB9DEEE, PixelFlare.S, 0.3f );
			s.d[0].emitter().burst( Speck.factory( Speck.STAR ), 4 );
		} ).rat( 4, 4 );
		//BladeBind.bind
		r.entry( "blade-bind", s -> {
			CharSprite ch = s.d[0];
			PointF a = s.h.center(), b = ch.center();
			SkillMarkFX.tether( s.hero, ch.ch, BladeBind.STEEL, BladeBind.TETHER_LIFE, -2 );
			SkillMarkFX.tether( s.hero, ch.ch, BladeBind.STEEL, BladeBind.TETHER_LIFE, 2 );
			float bite = (float) Math.hypot( (b.x - 3) - (a.x + 4), (b.y + 3) - (a.y + 2) ) / 320f + 0.2f;
			s.later( bite, () -> {
				ch.flash();
				ch.emitter().burst( SkillBursts.METAL_SPARK, 5 );
			} );
			ch.emitter().burst( SkillBursts.METAL_SPARK, 6 );
			ch.flash();
			s.center( ch.ch.pos ).burst( SkillBursts.METAL_SPARK, 8 );
		} ).rat( 4, 4 ).heroClass( HeroClass.DUELIST );
		//IronTip
		r.entry( "iron-tip", s -> s.center( s.d[0].ch.pos ).burst( SkillBursts.METAL_SPARK, 4 ) ).rat( 6, 4 );
		//LockSmith: a disarm; the +3 read
		r.entry( "locksmith-disarm", s -> {
			s.center( s.hero.pos ).burst( SkillBursts.STEEL_GLINT, 6 );
			ClosingRing.on( s.h, 23, 5, 0.3f, LockSmith.STEEL );
			PixelFlare.show( SkillVfx.cx( s.hero.pos ), s.h.y + s.h.height() - 3, LockSmith.STEEL, PixelFlare.S, 0.3f );
		} );
		r.entry( "locksmith-reveal", s -> {
			for (int c : TRAPS){
				s.get( c ).burst( SkillBursts.STEEL_GLINT, 5 );
				LockSmith.found( c );
			}
		} );
		//Lunge
		r.entry( "lunge", s -> {
			final int from = s.hero.pos, land = at( 5, 4 );
			StreakFX.dash( from, land, 0xFFFFFF, 0.5f, 0.35f, true );
			final CharSprite enemy = s.d[0];
			s.h.jump( from, land, () -> {
				s.h.place( land );
				enemy.flash();
				s.bottom( land ).burst( Speck.factory( Speck.DUST ), 4 );
				enemy.emitter().burst( SkillBursts.METAL_SPARK, 4 );
			} );
		} ).hero( 2, 4 ).rat( 6, 4 ).heroClass( HeroClass.DUELIST );
		//TrueEdge
		r.entry( "true-edge", s -> {
			SkillPreviewsMartialBefore.trueEdge( s );
			StreakFX.show( s.hero.pos, at( 6, 4 ), 0xB9DEEE, 0.45f, 0.25f );
		} ).rat( 4, 4 ).rat( 5, 4 ).rat( 6, 4 ).heroClass( HeroClass.DUELIST );
		//Hamstring's +3
		r.entry( "hamstring", s -> {
			s.get( s.d[0].ch.pos ).burst( SkillBursts.GRIT, 3 );
			Hamstring.legs( s.d[0] );
			s.bottom( s.d[0].ch.pos ).burst( Speck.factory( Speck.DUST ), 3 );
			s.d[0].flash();
		} ).rat( 4, 4 );
		//Banner: planted (the pillar as SkillPreviewsKit draws it now), and standing through two turns
		r.entry( "banner-plant", s -> {
			SkillPreviewsKit.pillar( s, BANNER, 0xFFD060 );
			BannerFX.plant( BANNER, true );
			s.center( BANNER ).pour( SkillBursts.GOLD_GLINT, 0.4f );
		} ).view( 1, 0, 9, 8 );
		r.entry( "banner-standing", s -> {
			BannerFX.plant( BANNER, false );
			s.center( BANNER ).pour( SkillBursts.GOLD_GLINT, 0.4f );
			for (float t : new float[]{ 0.2f, 1.2f }){
				s.later( t, () -> {
					PulseRingFX.at( BANNER, BannerFX.LIGHT, 32, 0.5f );
					s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 2 );
				} );
			}
		} ).seconds( 2.5f );
		//Deadfall.drop
		r.entry( "deadfall-trigger", SkillPreviewsMartial::deadfall ).rat( 6, 4 ).rat( 5, 5 ).view( 1, 0, 9, 9 ).seconds( 1.5f );
		//Fleche / Hurl: her own sparkle; Hurl's catch
		r.entry( "fleche-hurl-flourish", s -> {
			s.h.emitter().burst( SkillBursts.FLOURISH, 4 );
			s.later( 0.5f, () -> {
				PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), 0xB9DEEE, PixelFlare.S, 0.25f );
				s.h.emitter().burst( SkillBursts.FLOURISH, 3 );
			} );
		} ).heroClass( HeroClass.DUELIST );
		//DragonKick's wall slam (the stars stay: it is really dazed)
		r.entry( "dragon-kick-wall", s -> {
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), 0xD9AE75, PixelFlare.M, 0.5f );
			Wound.hit( s.d[0].ch );
			s.d[0].emitter().burst( Speck.factory( Speck.STAR ), 4 );
			s.center( s.d[0].ch.pos ).burst( SkillBursts.STONE_CHIP, 5 );
		} ).rat( 4, 4 ).heroClass( HeroClass.DUELIST );
		//Overwatch: the pinned slam
		r.entry( "overwatch-pin", s -> {
			s.d[0].emitter().burst( Speck.factory( Speck.LIGHT ), 3 );
			s.d[0].flash();
			s.get( s.d[0].ch.pos ).burst( SkillBursts.STONE_CHIP, 4 );
			WarriorImpactFX.show( s.d[0].ch.pos );
		} ).rat( 6, 4 ).heroClass( HeroClass.HUNTRESS );
		//Accuracy, Weapon Bond
		r.entry( "accuracy", s -> PixelFlare.show( SkillVfx.midX( s.d[0] ), s.d[0].y + 3, 0xDF9979, PixelFlare.S, 0.5f ) )
				.rat( 6, 4 ).heroClass( HeroClass.HUNTRESS );
		r.entry( "weapon-bond", s -> {
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), 0xB9DEEE, PixelFlare.S, 0.5f );
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
		} ).heroClass( HeroClass.DUELIST );
		//BloodDance.execute
		r.entry( "blood-dance", s -> {
			s.h.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 6 );
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), BloodDance.CRIMSON, PixelFlare.M, 0.6f );
			ArcSpinFX.around( s.h, BloodDance.CRIMSON, 10, 0.5f, 225, BloodDance.RIBBON_SPIN, BloodDance.RIBBON_FOR );
			ArcSpinFX.around( s.h, BloodDance.RIBBON_INNER, 7, 0.35f, 45, -BloodDance.RIBBON_SPIN, BloodDance.RIBBON_FOR );
			FxTimeline t = FxTimeline.start();
			for (int i = 0; i < 8; i++){
				final int k = i;
				t.at( 0.05f * i, () -> BloodDance.step( s.h, k ) );
			}
		} ).heroClass( HeroClass.ROGUE );
	}

	//Deadfall.drop now: dust in a ring two out, then each cell 0.04 s after the last: the
	//counterweight on its rope over the middle, the rest its shock; each landing a blow of the ground
	static void deadfall( SkillPreviewDump.Scene s ){
		int centre = at( 6, 4 );
		SkillPreviewsMartialBefore.dustRing( s, centre );
		StoneOfBlast look = new StoneOfBlast();
		int order = 0;
		for (int c : SkillPreviewsMartialBefore.nine( centre )){
			final int at = c, n = 1 + (order % 2);
			final boolean middle = c == centre;
			s.later( 0.1f + 0.04f * order++, () -> {
				com.watabou.utils.Callback landed = () -> WarriorImpactFX.show( at, middle );
				if (middle){
					SkillFX.rain( at, look, SkillFX.Fall.WEIGHT, 1, landed );
					SkillFX.rain( at, look, SkillFX.Fall.SHOCK, n - 1, null );
				} else {
					SkillFX.rain( at, look, SkillFX.Fall.SHOCK, n, landed );
				}
				s.get( at ).burst( SkillBursts.TREMBLE, 4 );
				s.bottom( at ).burst( Speck.factory( Speck.DUST ), 3 );
			} );
		}
	}
}
