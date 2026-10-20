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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ArrowRain;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ChargedShotDraw;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FletchingFeathers;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.HeartseekerArrow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ManaShieldWard;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MeteorFall;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.PoacherBait;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ArcaneEcho;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ArrowStorm;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Blink;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ChargedShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CinderTrail;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.EmberArrows;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Fletching;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MeteorCall;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sorcerer;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Spark;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.StormCall;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.WildCall;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SpiritArmorFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.Arrays;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The hunt-elements package's moments as the game draws them now (huntress, mage, fire, frost,
 * storm, arcane): the same stages as SkillPreviewsHuntBefore, each skill's visual calls of this code
 * (its own static pictures where it has them: HeartseekerArrow.beat, Fletching.burst, WildCall.bite,
 * MeteorFall.impact ...) restaged; CellEmitter's bursts through the scene's stand-in emitters.
 */
public final class SkillPreviewsHunt {

	private SkillPreviewsHunt(){}

	static final HeroClass HUNTRESS = HeroClass.HUNTRESS, MAGE = HeroClass.MAGE, ROGUE = HeroClass.ROGUE;

	public static void register( SkillPreviewDump.Registry r ){
		//huntress
		r.entry( "heartseeker-beats", SkillPreviewsHunt::heartseeker ).rat( 6, 4 ).heroClass( HUNTRESS ).seconds( 2.5f );
		r.entry( "heartseeker-seek", s -> {
			HeartseekerArrow.bursting( s.d[0] );
			SkillFX.streak( s.d[0].ch.pos, s.d[1].ch.pos, SkillFX.Streak.SPIRIT_ARROW, new Dart(), () -> {
				s.d[1].flash();
				HeartseekerArrow.lodged( s.d[1], 3 );
			} );
		} ).rat( 5, 4 ).rat( 7, 3 ).heroClass( HUNTRESS );
		r.entry( "ember-activate", s -> {
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 3 );
			s.h.emitter().burst( SkillBursts.EMBER_MOTE, 6 );
			EmberArrows.kindled( s.h, EmberArrows.HEAT, 0xFFC070 );
		} ).heroClass( HUNTRESS );
		r.entry( "ember-leap", s -> {
			flames( s.d[0] );
			SkillBolt.fly( s.d[0].parent, s.d[0].center(), s.d[1].center(), SkillBolt.EMBER, () -> flames( s.d[1] ) );
		} ).rat( 5, 4 ).rat( 7, 3 ).heroClass( HUNTRESS );
		r.entry( "ember-kill", s -> {
			int pos = at( 6, 4 );
			s.center( pos ).burst( FlameParticle.FACTORY, 10 );
			EmberArrows.heatRing( pos );
			int ring = 0;
			for (int c : around( pos )) s.get( c ).startDelayed( SkillBursts.EMBER_MOTE, 0, 5, 0.05f + 0.03f * ring++ );
		} ).heroClass( HUNTRESS );
		r.entry( "frost-activate", s -> {
			s.h.emitter().burst( SkillBursts.FROST_GLINT, 4 );
			s.h.emitter().burst( SkillBursts.FROST_GLINT, 6 );
			EmberArrows.kindled( s.h, 0x5FB8D8, 0xA4E9FF );
		} ).heroClass( HUNTRESS );
		r.entry( "fletching-burst", s -> {
			Fletching.burst( s.d[0] );
			for (int i = 1; i < s.d.length; i++){
				final CharSprite v = s.d[i];
				SkillFX.streak( s.d[0].ch.pos, v.ch.pos, SkillFX.Streak.FEATHER_DART, new Dart(), () -> {
					v.flash();
					Fletching.splintered( v );
				} );
			}
		} ).rat( 5, 4 ).rat( 7, 3 ).rat( 7, 5 ).rat( 4, 6 ).heroClass( HUNTRESS );
		r.entry( "fletching-pluck", s -> s.h.emitter().burst( SkillBursts.FEATHER, 2 ) ).heroClass( HUNTRESS );
		r.entry( "arrow-storm-cast", s -> {
			final int target = at( 7, 6 );
			final PointF top = DungeonTilemap.tileCenterToWorld( target );
			final int n = 5;
			FxTimeline t = FxTimeline.start();
			for (int i = 0; i < n; i++){
				final int k = i;
				t.at( 0.07f * i, () -> {
					PointF hand = s.h.center();
					ArrowStorm.loose( s.h.parent, hand.x, hand.y, top.x + (k - (n - 1) / 2f) * 6f, top.y - DungeonTilemap.SIZE * 6, k );
				} );
			}
			int order = 0;
			for (int c : area( target )){
				final int at = c, k = order++;
				t.at( 0.45f + 0.05f * k, () -> s.bottom( at ).burst( Speck.factory( Speck.DUST ), 3 ) );
			}
		} ).hero( 3, 6 ).view( 0, 0, 11, 9 ).heroClass( HUNTRESS ).seconds( 1.5f );
		r.entry( "arrow-rain-volley", s -> {
			int center = at( 6, 6 );
			Dart look = new Dart();
			int i = 0;
			for (int c : volley( center )){
				SkillFX.rain( c, look, SkillFX.Fall.SPIRIT_ARROW, 1 + (i++ % 2), c == center ? () -> ArrowRain.stormLands( center ) : null );
			}
		} ).hero( 2, 6 ).view( 1, 0, 9, 9 ).rat( 6, 6 ).rat( 7, 5 ).heroClass( HUNTRESS ).seconds( 1.5f );
		r.entry( "charged-draw", s -> {
			ChargedShotDraw.AimLine.show( lane( 3, 4, 10 ) );
			for (int k = 0; k < 3; k++) s.later( 0.8f * k, () -> ChargedShotDraw.drawing( s.h, 3 ) );
		} ).hero( 2, 4 ).view( 1, 1, 10, 7 ).rat( 7, 4 ).heroClass( HUNTRESS ).seconds( 2.5f );
		r.entry( "charged-release", s -> {
			int far = at( 10, 4 );
			ChargedShot.release( s.h, far );
			SkillFX.streak( s.h, far, SkillFX.Streak.SPIRIT_ARROW, null );
			FxTimeline t = FxTimeline.start();
			int i = 0;
			for (int c : lane( 3, 4, 10 )){
				final int at = c;
				t.at( 0.02f * i++, () -> s.center( at ).burst( Speck.factory( Speck.DUST ), 2 ) );
			}
			for (CharSprite d : s.d) d.flash();
		} ).hero( 2, 4 ).view( 1, 1, 10, 7 ).rat( 5, 4 ).rat( 8, 4 ).heroClass( HUNTRESS );
		r.entry( "bear-trap-throw", s -> {
			final int at = at( 6, 4 );
			SkillFX.streak( s.h, at, SkillFX.Streak.SNARE, () -> {
				s.get( at ).burst( Speck.factory( Speck.DUST ), 4 );
				s.center( at ).burst( SkillBursts.METAL_SPARK, 3 );
				SkillSpectacleFX.show( SkillSpectacleFX.JAW, at );
			} );
		} ).heroClass( HUNTRESS );
		r.entry( "bear-trap-spring", s -> {
			final int pos = at( 6, 4 );
			SkillSpectacleFX.show( SkillSpectacleFX.JAW, pos );
			s.center( pos ).burst( SkillBursts.METAL_SPARK, 4 );
			s.center( pos ).burst( SkillBursts.METAL_SPARK, 8 );
			FxTimeline.start().at( 0.14f, () -> {
				SkillSpectacleFX.show( SkillSpectacleFX.JAW, pos );
				s.center( pos ).burst( SkillBursts.METAL_SPARK, 4 );
			} );
			Wound.hit( s.d[0].ch );
			s.d[0].flash();
		} ).rat( 6, 4 ).heroClass( HUNTRESS );
		r.entry( "briar", s -> {
			int center = at( 6, 4 );
			for (CharSprite d : s.d) SkillSpectacleFX.fly( SkillSpectacleFX.THORN, center, d.ch.pos, 0, 0.45f );
		} ).rat( 6, 4 ).rat( 7, 3 ).rat( 5, 5 ).heroClass( HUNTRESS );
		r.entry( "wild-call-cast", s -> {
			s.h.emitter().burst( SkillBursts.FLOURISH, 4 );
			s.get( s.d[0].ch.pos ).burst( LeafParticle.GENERAL, 10 );
			WildCall.marked( s.d[0] );
		} ).rat( 6, 4 ).heroClass( HUNTRESS );
		r.entry( "wild-call-bite", s -> {
			Wound.hit( s.d[0].ch );
			s.d[0].emitter().burst( LeafParticle.GENERAL, 4 );
			WildCall.bite( s.d[0].ch.pos, s.hero.pos );
		} ).rat( 6, 4 ).heroClass( HUNTRESS );
		r.entry( "thorns-lash", s -> {
			s.d[0].emitter().burst( LeafParticle.GENERAL, 5 );
			SkillSpectacleFX.show( SkillSpectacleFX.THORN, s.d[0].ch.pos );
			s.bottom( s.d[0].ch.pos ).burst( EarthParticle.FACTORY, 4 );
		} ).rat( 6, 4 ).heroClass( HUNTRESS );
		r.entry( "poacher-bait", s -> {
			int cell = at( 6, 4 );
			s.get( cell ).burst( SkillBursts.SCENT, 6 );
			PixelFlare.show( SkillVfx.cx( cell ), SkillVfx.cy( cell ), 0xB5D67A, PixelFlare.S, 0.4f );
			s.later( 0.8f, () -> s.get( cell ).burst( SkillBursts.SCENT, 2 ) );
			s.later( 1.6f, () -> s.get( cell ).burst( SkillBursts.SCENT, 2 ) );
		} ).heroClass( HUNTRESS ).seconds( 2.5f );
		r.entry( "poacher-snare", s -> {
			int cell = s.d[0].ch.pos;
			s.d[0].flash();
			s.get( cell ).burst( LeafParticle.GENERAL, 10 );
			s.get( cell ).burst( SkillBursts.GRIT, 4 );
			PoacherBait.noose( s.d[0] );
		} ).rat( 6, 4 ).heroClass( HUNTRESS );
		r.entry( "sixth-sense", s -> {
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), 0x88CC66, PixelFlare.M, 0.6f );
			ClosingRing.on( s.h, 31, 9, 0.45f, 0x88CC66 );
			s.h.emitter().burst( SkillBursts.FLOURISH, 6 );
		} ).heroClass( HUNTRESS );

		//mage, fire, frost, storm and arcane
		r.entry( "meteor-mark", s -> {
			int cell = at( 6, 4 );
			MeteorCall.marked( s.h, cell );
			s.center( cell ).burst( SkillBursts.EMBER_MOTE, 6 );
			StaggerFX.ring( cell, 1, 0.15f, ( c, rr ) -> s.get( c ).burst( SkillBursts.EMBER_MOTE, 2 ) );
			s.center( cell ).burst( SkillBursts.EMBER_MOTE, 4 );
			MeteorFall.HeatMark.at( cell );
		} ).heroClass( MAGE ).seconds( 2f );
		r.entry( "meteor-fall", s -> {
			final int cell = at( 6, 6 );
			SkillFX.rain( cell, new Bomb(), SkillFX.Fall.METEOR, 1, () -> {
				MeteorFall.impact( cell );
				s.center( cell ).burst( SkillBursts.STONE_CHIP, 30 );
				for (int c : area( cell )){
					//flame only where something burns: the rats caught
					s.get( c ).burst( bodyAt( s, c ) != null ? FlameParticle.FACTORY : SkillBursts.EMBER_MOTE, 4 );
				}
			} );
		} ).hero( 2, 6 ).view( 1, 0, 9, 9 ).rat( 6, 6 ).rat( 7, 7 ).heroClass( MAGE ).seconds( 1.5f );
		r.entry( "arcane-echo", s -> echo( s, false ) ).rat( 4, 4 ).heroClass( MAGE );
		r.entry( "arcane-echo-chain", s -> echo( s, true ) ).rat( 4, 4 ).rat( 5, 5 ).heroClass( MAGE );
		r.entry( "blink-chain", SkillPreviewsHunt::blink ).hero( 2, 4 ).rat( 3, 4 ).rat( 5, 5 ).heroClass( ROGUE ).seconds( 1.5f );
		r.entry( "spark", SkillPreviewsHunt::spark ).rat( 5, 4 ).rat( 7, 4 ).heroClass( MAGE ).seconds( 1.5f );
		r.entry( "sorcerer-overload", s -> {
			Sorcerer.overloaded( s.d[0] );
			s.later( 0.6f, () -> s.d[0].centerEmitter().burst( SkillBursts.ARCANE_SPARK, 5 ) );
			s.later( 1.0f, () -> {
				for (CharSprite d : s.d){
					if (d != s.d[0]) s.h.parent.add( new Lightning( s.d[0].ch.pos, d.ch.pos, null ).tint( Lightning.ARCANE ) );
					d.centerEmitter().burst( SkillBursts.ARCANE_SPARK, 6 );
					d.flash();
				}
			} );
		} ).rat( 6, 4 ).rat( 7, 3 ).rat( 7, 5 ).heroClass( MAGE ).seconds( 1.6f );
		r.entry( "wizard-ricochet", s -> {
			SkillBolt.fly( s.h.parent, s.d[0].ch.pos, s.d[1].ch.pos, SkillBolt.ARCANE, null );
			s.d[1].centerEmitter().burst( SkillBursts.ARCANE_SPARK, 6 );
		} ).rat( 5, 4 ).rat( 8, 5 ).heroClass( MAGE );
		r.entry( "pyre-leap", s -> {
			s.d[0].emitter().burst( FlameParticle.FACTORY, 6 );
			PixelFlare.show( SkillVfx.midX( s.d[0] ), SkillVfx.midY( s.d[0] ), 0xFFC070, PixelFlare.S, 0.35f );
			SkillBolt.fly( s.h.parent, s.d[0].ch.pos, s.d[1].ch.pos, SkillBolt.EMBER, () -> {
				s.d[1].emitter().burst( FlameParticle.FACTORY, 6 );
				s.d[1].flash();
			} );
		} ).rat( 5, 4 ).rat( 6, 5 ).heroClass( MAGE );
		r.entry( "cinder-gather", s -> {
			CinderTrail.gather( s.h, 6 );
			StaggerFX.after( 0.15f, () -> PixelFlare.show( SkillVfx.midX( s.h ) + CinderTrail.SIDE, SkillVfx.midY( s.h ), CinderTrail.FROST, PixelFlare.S, 0.4f ) );
		} ).heroClass( MAGE );
		r.entry( "spirit-volley", s -> {
			for (CharSprite d : s.d){
				SkillBolt.fly( s.h.parent, s.h.center(), d.destinationCenter(), SkillBolt.SPIRIT, null );
				SpiritArmorFX.spend( s.hero, d.ch );
				d.flash();
			}
		} ).rat( 6, 3 ).rat( 6, 5 ).heroClass( MAGE );
		r.entry( "spell-blade", s -> s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 5 ) ).heroClass( MAGE );
		r.entry( "storm-call", SkillPreviewsHunt::stormCall ).hero( 2, 4 ).rat( 6, 4 ).rat( 7, 2 ).heroClass( MAGE ).seconds( 1.5f );
		r.entry( "mana-shield-break", s -> {
			ManaShieldWard.broke( s.h );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 10 );
		} ).heroClass( MAGE );
	}

	// ------------------------------------------------------------------ the moments now

	//the lodged arrow: the throb as it lodges, two beats 0.6 s apart, the burst
	static void heartseeker( SkillPreviewDump.Scene s ){
		final CharSprite e = s.d[0];
		HeartseekerArrow.lodged( e, 3 );
		s.later( 0.6f, () -> HeartseekerArrow.beat( e, 1 ) );
		s.later( 1.2f, () -> HeartseekerArrow.beat( e, 2 ) );
		s.later( 1.8f, () -> HeartseekerArrow.bursting( e ) );
	}

	//EmberArrows.flames: the target catching (real Burning)
	static void flames( CharSprite c ){
		c.centerEmitter().burst( FlameParticle.FACTORY, 4 );
		c.emitter().start( FlameParticle.FACTORY, 0.05f, 6 );
	}

	//ArcaneEcho.onHitProc's pictures: the arc back along the blow, sparks, the echo; at +3 the chain
	static void echo( SkillPreviewDump.Scene s, boolean chain ){
		s.h.parent.add( new Lightning( s.hero.pos, s.d[0].ch.pos, null ).tint( Lightning.ARCANE ) );
		s.d[0].emitter().burst( SkillBursts.ARCANE_SPARK, 5 );
		s.d[0].flash();
		ArcaneEcho.echo( s.d[0].ch.pos, s.hero.pos );
		if (!chain) return;
		s.h.parent.add( new Lightning( s.d[0].ch.pos, s.d[1].ch.pos, null ).tint( Lightning.ARCANE ) );
		s.d[1].emitter().burst( SkillBursts.ARCANE_SPARK, 5 );
		s.d[1].flash();
		ArcaneEcho.echo( s.d[1].ch.pos, s.d[0].ch.pos );
	}

	//Blink.playJump: one arc at a time body to body, the next when it fades
	static void blink( SkillPreviewDump.Scene s ){
		int[][] jumps = { { at( 2, 4 ), at( 4, 4 ), 0 }, { at( 4, 4 ), at( 6, 6 ), 1 } };
		jump( s, jumps, 0 );
	}

	private static void jump( SkillPreviewDump.Scene s, int[][] jumps, int i ){
		if (i >= jumps.length){
			s.h.emitter().burst( SkillBursts.STORM_SPARK, 10 );
			return;
		}
		int[] j = jumps[i];
		CharSprite target = s.d[j[2]];
		s.h.place( j[0] );
		PointF start = s.h.center();
		s.h.place( j[1] );
		PointF end = s.h.center();
		PointF middle = target.center();
		ArrayList<Lightning.Arc> arcs = new ArrayList<>( Arrays.asList( new Lightning.Arc( start, middle ), new Lightning.Arc( middle, end ) ) );
		target.emitter().burst( SkillBursts.STORM_SPARK, 6 );
		target.flash();
		Blink.jumped( s.h, j[0], j[1] );
		s.h.parent.add( new Lightning( arcs, () -> jump( s, jumps, i + 1 ) ).tint( Lightning.STORM ) );
	}

	//Spark: the static in her hands, the spark, the sparks where it breaks, then the arcs body to body
	static void spark( SkillPreviewDump.Scene s ){
		final int cell = s.d[1].ch.pos;
		s.h.centerEmitter().burst( SkillBursts.STORM_SPARK, 4 );
		Spark.bolt( s.h, cell, () -> {
			s.center( cell ).burst( SkillBursts.STORM_SPARK, 10 );
			arc( s, s.hero.pos, new int[]{ s.d[0].ch.pos, cell }, 0 );
		} );
	}

	private static void arc( SkillPreviewDump.Scene s, int from, int[] struck, int i ){
		if (i >= struck.length) return;
		int to = struck[i];
		s.center( to ).burst( SkillBursts.STORM_SPARK, 6 );
		CharSprite b = bodyAt( s, to );
		if (b != null) b.flash();
		s.h.parent.add( new Lightning( from, to, () -> arc( s, to, struck, i + 1 ) ).tint( Lightning.STORM ) );
	}

	//StormCall.playArc: out of the sky onto the first, then body to body
	static void stormCall( SkillPreviewDump.Scene s ){
		stormArc( s, new int[][]{ { -1, s.d[0].ch.pos }, { s.d[0].ch.pos, s.d[1].ch.pos } }, 0 );
	}

	private static void stormArc( SkillPreviewDump.Scene s, int[][] arcs, int i ){
		if (i >= arcs.length) return;
		int[] a = arcs[i];
		s.center( a[1] ).burst( SkillBursts.STORM_SPARK, 8 );
		StormCall.struck( a[1] );
		CharSprite b = bodyAt( s, a[1] );
		if (b != null) b.flash();
		Callback then = () -> stormArc( s, arcs, i + 1 );
		if (a[0] < 0){
			PointF foot = DungeonTilemap.raisedTileCenterToWorld( a[1] );
			s.h.parent.add( new Lightning( new PointF( foot.x, foot.y - DungeonTilemap.SIZE * 7 ), a[1], then ).major().aloft().tint( Lightning.STORM ) );
		} else {
			s.h.parent.add( new Lightning( a[0], a[1], then ).major().tint( Lightning.STORM ) );
		}
	}

	// ------------------------------------------------------------------ the stage

	static CharSprite bodyAt( SkillPreviewDump.Scene s, int cell ){
		for (CharSprite d : s.d) if (d.ch.pos == cell) return d;
		return null;
	}

	//the eight cells round one
	static int[] around( int cell ){
		int w = SkillPreviewDump.W, x = cell % w, y = cell / w, n = 0;
		int[] out = new int[8];
		for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) if (dx != 0 || dy != 0) out[n++] = at( x + dx, y + dy );
		return out;
	}

	//a cell and the eight round it (SkillInteractions.area( cell, 1 ))
	static int[] area( int cell ){
		int w = SkillPreviewDump.W, x = cell % w, y = cell / w, n = 0;
		int[] out = new int[9];
		for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) out[n++] = at( x + dx, y + dy );
		return out;
	}

	//ArrowRain at +2: the nine cells and the four cross tips two out
	static int[] volley( int center ){
		int w = SkillPreviewDump.W, x = center % w, y = center / w;
		int[] a = area( center );
		int[] out = Arrays.copyOf( a, 13 );
		out[9] = at( x - 2, y );
		out[10] = at( x + 2, y );
		out[11] = at( x, y - 2 );
		out[12] = at( x, y + 2 );
		return out;
	}

	//a straight lane on row y from x0 to x1
	static ArrayList<Integer> lane( int x0, int y, int x1 ){
		ArrayList<Integer> out = new ArrayList<>();
		for (int x = x0; x <= x1; x++) out.add( at( x, y ) );
		return out;
	}
}
