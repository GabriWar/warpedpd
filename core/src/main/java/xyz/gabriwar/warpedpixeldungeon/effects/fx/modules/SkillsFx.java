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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.modules;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ElementalOrbit;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MasterThiefCoins;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpiritArmorMotes;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Blackout;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.EmberArrows;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.FrostNova;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.HealingPrayer;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Spark;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.WildCall;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.ElementalOrbitFX;
import xyz.gabriwar.warpedpixeldungeon.effects.MasterThiefCoinsFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillCastFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFieldFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SpiritArmorFX;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.effects.WhirlHitFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.School;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.StanceOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LinkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.MoteFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillRay;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Gizmo;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.List;

/**
 * Skills (the skills area: docs/fx/skills.md): every class skill's casts, arcs, rings, pillars,
 * streaks, marks, fields, spectacles, impacts and the orbits that circle a skilled hero.
 *
 * Its gallery page shows each of them on the stage; its scene "fx-skills" plays the page's casts,
 * class by class, by day and at night.
 */
public class SkillsFx implements FxModule {

	@Override
	public String key(){
		return "skills";
	}

	@Override
	public String title(){
		return "Skills";
	}

	@Override
	public void sceneCreated( GameScene scene ){
		//the statics a scene's pictures counted themselves in: none of the last scene's are left
		PixelFlare.reset();
		LanceFX.reset();
		SkillSpectacleFX.reset();
		ShoutFX.reset();
		SkillFieldFX.clearCap();
		SkillBolt.reset();
		RuneFX.reset();
		LinkFX.reset();
		SkillRay.reset();
	}

	@Override
	public void exhibits( FxGallery.Page page ){
		page.add( "blade arcs: a slash, a pirouette kicking dust, a flurry, a strike on a cell", 2.6f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( -1, 1 ) );
			if (p == null) return;
			ArcSpinFX.slash( p.sprite, 0xFFE8A0, true );
			s.after( 0.5f, () -> {
				ArcSpinFX.around( p.sprite, 0xCCE0FF, 10, 0.5f, 0, 1080, 0.5f, true );
				ArcSpinFX.around( p.sprite, 0xFFFFFF, 7, 0.35f, 180, 1080, 0.5f );
			} );
			s.after( 1.2f, () -> {
				ArcSpinFX.around( p.sprite, 0xFFFFFF, 12, 0.3f, 0, 1000, 0.4f, true );
				ArcSpinFX.around( p.sprite, 0xCCE4FF, 9, 0.22f, 180, -820, 0.4f );
			} );
			s.after( 1.9f, () -> ArcSpinFX.at( s.dryRat.pos, 0xFFFFFF, 10, 0.25f, 200, 900, 0.22f ) );
		} );
		page.add( "pulse rings: about a sprite, and wide over the pool", 2.2f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( -1, 1 ) );
			if (p != null) PulseRingFX.around( p.sprite, 0xFFD060, 20, 0.6f );
			s.after( 0.8f, () -> PulseRingFX.at( s.poolMiddle(), 0xBBFFCC, 36, 0.7f ) );
		} );
		page.add( "a pillar rising, its brightest moment first", 2.5f, s ->
				PillarRiseFX.show( s.cell( 0, -2 ), 0xFFE080, () -> GLog.i( "FX skills: the pillar's peak" ) ) );
		page.add( "streaks: the hero's dash with afterimages and speed lines, and a plain one", 2.2f, s -> {
			StreakFX.dash( Dungeon.hero.pos, s.cell( -4, 2 ), 0xFFFFFF, 0.5f, 0.3f, true );
			s.after( 0.9f, () -> StreakFX.show( s.cell( -3, 1 ), s.cell( 1, 1 ), 0xDDEEFF, 0.3f, 0.3f ) );
		} );
		page.add( "three orbits at once: a stance, spirit armour, coins; a blow flares the stance", 3.2f, s -> {
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( -1, 1 ) );
			if (p == null) return;
			StanceOrbit stance = new StanceOrbit( p.ch, 0xFFB060 );
			SpiritArmorMotes motes = new SpiritArmorMotes();
			motes.motes = 3;
			MasterThiefCoins coins = new MasterThiefCoins();
			coins.countUp( 4 );
			Gizmo armour = new SpiritArmorFX( p.ch, motes ), purse = new MasterThiefCoinsFX( p.ch, coins );
			p.sprite.parent.add( stance );
			p.sprite.parent.add( armour );
			p.sprite.parent.add( purse );
			s.after( 1.0f, () -> p.attack( s.dryRat.pos, null ) );
			s.after( 1.8f, () -> {
				motes.motes = 2;
				SpiritArmorFX.spend( p.ch, s.dryRat );
				coins.countUp( 1 );
			} );
			s.after( 2.4f, () -> p.move( s.cell( 0, 1 ) ) );
			s.after( 3.1f, () -> {
				Fx.dispose( stance );
				Fx.dispose( armour );
				Fx.dispose( purse );
			} );
		} );
		page.add( "a cast of every school: holy, shadow, nature, fire, frost, storm", 4f, s -> {
			Skill[] casts = { new HealingPrayer(), new Blackout(), new WildCall(), new EmberArrows(), new FrostNova(), new Spark() };
			for (int i = 0; i < casts.length; i++){
				final Skill k = casts[i];
				k.level = 1;
				s.after( 0.6f * i, () -> SkillCastFX.play( k, Dungeon.hero ) );
			}
		} );
		page.add( "marks: a chain to its anchor, frost shards, a stagger's stars, a guard's bubble", 3f, s -> {
			int[] kinds = { SkillInteractions.Mark.CHAIN, SkillInteractions.Mark.FROST, SkillInteractions.Mark.STAGGER,
					SkillInteractions.Mark.GUARD };
			int[] at = { s.cell( -3, 1 ), s.cell( -1, 1 ), s.cell( 1, 1 ), s.cell( -1, -1 ) };
			for (int i = 0; i < kinds.length; i++){
				FxStage.Puppet p = s.puppet( Rat.class, at[i] );
				if (p == null) continue;
				SkillInteractions.Mark m = new SkillInteractions.Mark();
				m.kind = kinds[i];
				m.power = 10;
				m.cell = s.cell( -5, -1 );
				m.target = p.ch;
				final SkillMarkFX fx = new SkillMarkFX( m );
				p.sprite.parent.add( fx );
				s.after( 2.9f, () -> Fx.dispose( fx ) );
			}
		} );
		page.add( "skill fields of every kind", 4f, s -> {
			for (int kind = SkillField.CRACK; kind <= SkillField.SMOKE; kind++){
				int x = -6 + (kind % 4) * 3, y = kind < 4 ? -1 : 2;
				final SkillFieldFX fx = new SkillFieldFX( field( kind, s.cell( x, y ), s.cell( x + 1, y ) ) );
				SkillFieldFX.show( fx );
				s.after( 3.9f, () -> Fx.dispose( fx ) );
			}
		} );
		page.add( "spectacles: a smite, a spire, a sabre, a shade, a raptor, a bite, wings, thorns, a lance, a slash", 7.4f, s -> {
			int[] kinds = { SkillSpectacleFX.HAMMER, SkillSpectacleFX.SPIRE, SkillSpectacleFX.SABER, SkillSpectacleFX.SHADOW,
					SkillSpectacleFX.RAPTOR, SkillSpectacleFX.JAW, SkillSpectacleFX.WINGS, SkillSpectacleFX.THORN,
					SkillSpectacleFX.LANCE, SkillSpectacleFX.SLASH };
			for (int i = 0; i < kinds.length; i++){
				final int kind = kinds[i];
				s.after( 0.7f * i, () -> {
					if (kind == SkillSpectacleFX.WINGS) SkillSpectacleFX.show( kind, Dungeon.hero.pos );
					else SkillSpectacleFX.fly( kind, s.cell( -3, 1 ), s.dryRat.pos, 0, 0.45f );
				} );
			}
		} );
		kit( page );
		rfxKit( page );
		page.add( "Earthshatter: a ring of impacts on the floor and over the pool, then a hammer", 2.6f, s -> {
			StaggerFX.ring( s.cell( 2, 0 ), 2, 0.12f, ( c, r ) -> WarriorImpactFX.show( c ) );
			s.after( 1.2f, () -> WarriorImpactFX.show( s.cell( -1, -1 ), true ) );
		} );
		page.add( "a whirl's hits", 2f, s -> {
			WhirlHitFX.show( s.dryRat.pos );
			WhirlHitFX.show( s.skeleton.pos );
			s.after( 0.4f, () -> WhirlHitFX.show( s.wetRat.pos ) );
		} );
		page.add( "an elemental orbit, hot, then a fire orb and an ice orb launched", 3.4f, s -> {
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( -2, 1 ) );
			if (p == null) return;
			ElementalOrbit orbit = new ElementalOrbit();
			//the orbs' elements are the game's random draw: drawn here from a generator of the
			//gallery's own, so the game's sequence never moves
			Random.pushGenerator( 7 );
			try {
				orbit.set( 3 );
			} finally {
				Random.popGenerator();
			}
			for (int i = 0; i < 30; i++) orbit.warm();
			final ElementalOrbitFX fx = new ElementalOrbitFX( p.ch, orbit );
			ElementalOrbitFX.show( fx );
			s.after( 1.2f, () -> ElementalOrbitFX.launch( p.ch, s.dryRat.pos, 0, 3, () -> GLog.i( "FX skills: fire orb arrives" ) ) );
			s.after( 2.0f, () -> ElementalOrbitFX.launch( p.ch, s.wetRat.pos, 1, 2, () -> GLog.i( "FX skills: ice orb arrives" ) ) );
			s.after( 3.3f, () -> Fx.dispose( fx ) );
		} );
		page.add( "pillar of light, a landing on stone, on grass and in the pool", 2.8f, s -> {
			SkillFX.pillar( s.cell( 2, 3 ), 0xA0E0FF );
			SkillFX.land( s.stone()[0] );
			s.after( 0.5f, () -> SkillFX.landing( s.grass()[3] ) );
			s.after( 0.9f, () -> SkillFX.landing( s.poolMiddle() ) );
			s.after( 1.4f, () -> FxRecipes.impact( SkillVfx.cx( s.dryRat.pos ), SkillVfx.cy( s.dryRat.pos ), Element.HOLY, 1f ) );
		} );
		page.add( "flares and a closing ring", 2f, s -> {
			SkillInteractions.flare( s.dryRat.pos, 0xFFF1A1 );
			SkillInteractions.flare( s.skeleton.pos, 0xA4E9FF );
			if (Dungeon.hero.sprite != null){
				PixelFlare.show( SkillVfx.midX( Dungeon.hero.sprite ), SkillVfx.midY( Dungeon.hero.sprite ), 0x88CC66, PixelFlare.M, 0.6f );
				ClosingRing.on( Dungeon.hero.sprite, 31, 9, 0.45f, 0x88CC66 );
			}
		} );
		classCasts( page );
	}

	//the skills' own kit (docs/fx/skills.md): the lance, the shout and dread, the flights, the falls
	//and throws that are not items, the bursts
	private static void kit( FxGallery.Page page ){
		page.add( "Impaling Thrust's lance of light through two, then the +3's spectral lances rippling down a lane", 2.6f, s -> {
			FxStage.Puppet a = s.puppet( Rat.class, s.cell( 1, 0 ) ), b = s.puppet( Rat.class, s.cell( 3, 0 ) );
			LanceFX.thrust( Dungeon.hero.pos, s.cell( 3, 0 ), 3, LanceFX.STEEL, Dungeon.hero.sprite );
			if (a != null) s.after( LanceFX.STEP * 0, () -> LanceFX.pierce( a.sprite, 0, LanceFX.STEEL ) );
			if (b != null) s.after( LanceFX.STEP * 2, () -> LanceFX.pierce( b.sprite, 0, LanceFX.STEEL ) );
			s.after( 1.2f, () -> {
				for (int i = 0; i < 5; i++) LanceFX.spectral( Dungeon.hero.pos, s.cell( 1 + i, 1 ), 0.04f * i );
			} );
		} );
		page.add( "War Cry's roar, its flinches; dread creeping in and spilling; a howl; a taunt's pull", 4.2f, s -> {
			FxStage.Puppet[] p = { s.puppet( Rat.class, s.cell( 1, -1 ) ), s.puppet( Rat.class, s.cell( 2, 1 ) ),
					s.puppet( Rat.class, s.cell( 3, 0 ) ) };
			if (Dungeon.hero.sprite == null) return;
			float hx = SkillVfx.midX( Dungeon.hero.sprite ), hy = SkillVfx.midY( Dungeon.hero.sprite );
			ShoutFX.roar( Dungeon.hero.sprite, ShoutFX.MARTIAL, 72 );
			for (int i = 0; i < p.length; i++) if (p[i] != null) ShoutFX.flinch( p[i].sprite, hx, hy, ShoutFX.MARTIAL, 0.05f + 0.03f * (i + 1) );
			s.after( 1.2f, () -> {
				if (p[1] == null || p[2] == null) return;
				ShoutFX.dread( p[1].sprite, false, 0 );
				ShoutFX.creep( p[1].ch.pos, p[2].ch.pos, 0.1f );
				ShoutFX.dread( p[2].sprite, true, 0.18f );
			} );
			s.after( 2.3f, () -> ShoutFX.howl( Dungeon.hero.sprite, ShoutFX.WOLF, 3, 0.08f ) );
			s.after( 3.2f, () -> {
				for (FxStage.Puppet q : p) if (q != null) ShoutFX.pull( q.sprite, hx, hy, ShoutFX.FURY, 0 );
			} );
		} );
		page.add( "the skills' flights: soul, mana, hex, ember, holy, heal, spark, arcane, spirit, the ward's runes", 5.4f, s -> {
			for (int l = 0; l < SkillBolt.LOOKS; l++){
				final int look = l;
				s.after( 0.5f * l, () -> {
					if (look == SkillBolt.HEAL){
						SkillBolt.mote( Dungeon.hero.pos, s.dryRat.pos, look, null );
						return;
					}
					//a bolt's one draw of the game's Random from a generator of the gallery's own
					Random.pushGenerator( 7 + look );
					try {
						SkillBolt.fly( Dungeon.hero.sprite.parent, Dungeon.hero.pos, s.dryRat.pos, look, null );
					} finally {
						Random.popGenerator();
					}
				} );
			}
		} );
		page.add( "falls that are no items: a meteor, spirit arrows, a holy blade, a counterweight; throws: a coin, "
				+ "a feather dart, a pellet of ink, a snare", 5.4f, s -> {
			int[] falls = { SkillFX.Fall.METEOR, SkillFX.Fall.SPIRIT_ARROW, SkillFX.Fall.HOLY_BLADE, SkillFX.Fall.WEIGHT };
			for (int i = 0; i < falls.length; i++){
				final int fall = falls[i];
				s.after( 0.7f * i, () -> {
					Random.pushGenerator( 11 + fall );
					try {
						SkillFX.rain( s.cell( -2 + 2 * fall, -1 ), new xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart(),
								fall, fall == SkillFX.Fall.SPIRIT_ARROW ? 3 : 1, null );
					} finally {
						Random.popGenerator();
					}
				} );
			}
			int[] throws_ = { SkillFX.Streak.COIN, SkillFX.Streak.FEATHER_DART, SkillFX.Streak.PELLET, SkillFX.Streak.SNARE };
			for (int i = 0; i < throws_.length; i++){
				final int look = throws_[i];
				s.after( 3f + 0.6f * i, () -> SkillFX.streak( Dungeon.hero.pos, s.dryRat.pos, look, null ) );
			}
		} );
		page.add( "the skills' bursts, every one", 3f, s -> {
			for (int i = 0; i < SkillBursts.ALL.length; i++){
				final int k = i;
				s.after( 0.1f * i, () -> s.burstCenter( s.cell( -5 + k % 6 * 2, -2 + k / 6 * 2 ),
						(com.watabou.noosa.particles.Emitter.Factory) SkillBursts.ALL[k][1], 4 ) );
			}
		} );
	}

	//the second pass's kit (docs/fx/skills.md): a ward volley, every RuneFX verb, the links pulled from
	//both ends, the rays, the motes and a daze
	private static void rfxKit( FxGallery.Page page ){
		page.add( "Spirit Volley's ward bolts, four at once; a soul bolt peeling off its source", 2.4f, s -> {
			FxStage.Puppet[] p = { s.puppet( Rat.class, s.cell( 3, -2 ) ), s.puppet( Rat.class, s.cell( 4, 0 ) ),
					s.puppet( Rat.class, s.cell( 3, 2 ) ), s.puppet( Rat.class, s.cell( 1, 2 ) ) };
			Random.pushGenerator( 31 );
			try {
				for (FxStage.Puppet q : p){
					if (q != null) SkillBolt.fly( Dungeon.hero.sprite.parent, Dungeon.hero.sprite.center(), q.sprite.destinationCenter(), SkillBolt.RUNE, null );
				}
			} finally {
				Random.popGenerator();
			}
			s.after( 1.2f, () -> {
				if (p[1] != null) SkillBolt.mote( p[1].sprite, Dungeon.hero.sprite, SkillBolt.SOUL, null ).peel( p[1].sprite, 6 );
			} );
		} );
		page.add( "runes: a ring, spun up and released; a ward locking on, whole and on one side; shut; scattered; rising; a stamp", 3f, s -> {
			FxStage.Puppet a = s.puppet( Rat.class, s.cell( 2, -1 ) ), b = s.puppet( Rat.class, s.cell( 2, 1 ) );
			if (Dungeon.hero.sprite == null) return;
			RuneFX.Runes ring = RuneFX.ring( Dungeon.hero.sprite, 6, SkillPalette.RUNE_ARCANE, 6, 2.5f, RuneFX.RING_TURN, 1.4f ).spin( 8f, 0.3f );
			s.after( 0.9f, () -> ring.release( 40f, 0.25f ) );
			if (a != null) RuneFX.lock( a.sprite, 6, SkillPalette.RUNE_MANA, RuneFX.LOCK_RX, RuneFX.LOCK_RY, RuneFX.LOCK_GAP, RuneFX.LOCK_HOLD );
			if (b != null) RuneFX.lock( b.sprite, 3, SkillPalette.RUNE_MANA, RuneFX.LOCK_RX, RuneFX.LOCK_RY, RuneFX.LOCK_GAP, RuneFX.LOCK_HOLD, 0f, 1.2f );
			s.after( 1.2f, () -> {
				if (a != null) RuneFX.shut( a.sprite, 5, SkillPalette.RUNE_VOID, RuneFX.SHUT_RX, RuneFX.SHUT_RY, RuneFX.SHUT_FOR );
				if (b != null) RuneFX.scatter( b.sprite, 6, SkillPalette.RUNE_MANA, RuneFX.SCATTER_DIST, RuneFX.SCATTER_LIFE );
			} );
			s.after( 1.8f, () -> {
				RuneFX.rise( Dungeon.hero.sprite, 6, SkillPalette.RUNE_HOLY, RuneFX.RISE_RX, RuneFX.RISE_RY, RuneFX.RISE_SPEED, RuneFX.RISE_LIFE );
				if (a != null) RuneFX.stamp( a.sprite, FxFrames.RUNE_5[2], 0xF1D99C, 0.4f );
			} );
		} );
		page.add( "links: a shadow bond pulled from both ends to its bite, a steel one between neighbours, a held one, a drain", 3.4f, s -> {
			FxStage.Puppet a = s.puppet( Rat.class, s.cell( 1, -1 ) ), b = s.puppet( Rat.class, s.cell( 4, 0 ) ), c = s.puppet( Rat.class, s.cell( 2, 2 ) );
			if (a == null || b == null || c == null || Dungeon.hero.sprite == null) return;
			LinkFX.bind( Dungeon.hero.sprite.parent, a.sprite.center(), b.sprite.center(), LinkFX.SHADOW, null );
			s.after( 0.6f, () -> LinkFX.bind( Dungeon.hero.sprite.parent, Dungeon.hero.sprite.center(), a.sprite.center(), LinkFX.STEEL, null ) );
			s.after( 1.0f, () -> {
				final Gizmo held = LinkFX.held( b.sprite, c.sprite, LinkFX.SHADOW );
				s.after( 1.6f, () -> Fx.dispose( held ) );
			} );
			s.after( 2.6f, () -> LinkFX.drain( c.sprite, Dungeon.hero.sprite, LinkFX.SOUL_HEAL, 0.3f ) );
		} );
		page.add( "rays: holy and a heal flowing; a stream riding the cleric's light ray", 2.0f, s -> {
			FxStage.Puppet a = s.puppet( Rat.class, s.cell( 4, -1 ) ), b = s.puppet( Rat.class, s.cell( 4, 1 ) );
			if (a == null || b == null || Dungeon.hero.sprite == null) return;
			SkillRay.show( Dungeon.hero.sprite, a.sprite, SkillRay.HOLY, 0.5f );
			s.after( 0.6f, () -> SkillRay.show( Dungeon.hero.sprite, b.sprite, SkillRay.HEAL, 0.6f ) );
			s.after( 1.3f, () -> {
				a.sprite.parent.add( new xyz.gabriwar.warpedpixeldungeon.effects.Beam.LightRay( a.sprite.center(), b.sprite.center() ) );
				SkillRay.stream( a.sprite, b.sprite, SkillRay.STREAM_HOLY, 0.5f );
			} );
		} );
		page.add( "motes: the heal's crosses and greens, mana gathered, sparks and shards flicked, a glint; a daze over a head", 2.4f, s -> {
			if (Dungeon.hero.sprite == null) return;
			MoteFX.crosses( Dungeon.hero.sprite, 3 );
			MoteFX.greens( Dungeon.hero.sprite, 4 );
			float x = SkillVfx.cx( s.cell( 2, 0 ) ), y = SkillVfx.cy( s.cell( 2, 0 ) );
			MoteFX.gather( x, y, 8, SkillPalette.MANA, 18, 0.4f );
			s.after( 0.6f, () -> {
				MoteFX.flick( x, y, 6, FxFrames.SPARK_2, SkillPalette.STEEL, 30f, 50f, 4f, 0f, 0.2f, 10f, FxBudget.P2 );
				MoteFX.flick( x + 16, y, 3, FxFrames.SHARD_GLASS, 0x66CCFF, 20f, 34f, 3f, 0f, 0.35f, 10f, FxBudget.P2 );
				MoteFX.glint( x - 8, y - 6, FxFrames.STAR4_5, 0xE8F0FF, 0.3f, 40f, 0f );
			} );
			s.after( 1.2f, () -> {
				if (s.dryRat == null || s.dryRat.sprite == null) return;
				//a burst's draws of the game's Random from a generator of the gallery's own
				Random.pushGenerator( 41 );
				try {
					s.dryRat.sprite.emitter().burst( SkillBursts.DAZE, 4 );
				} finally {
					Random.popGenerator();
				}
			} );
		} );
	}

	//a few casts per class, as the classes' skills show them
	private static void classCasts( FxGallery.Page page ){
		page.add( "warrior: a stance, a slam's ring of impacts, a landing", 3f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( -2, 1 ) );
			if (p == null) return;
			StanceOrbit stance = new StanceOrbit( p.ch, 0xCCDDEE );
			p.sprite.parent.add( stance );
			FxRecipes.castBloom( SkillVfx.midX( p.sprite ), SkillVfx.midY( p.sprite ), School.MARTIAL.element );
			s.after( 0.4f, () -> StaggerFX.ring( s.cell( -2, 1 ), 1, 0.08f, ( c, r ) -> WarriorImpactFX.show( c ) ) );
			s.after( 1.4f, () -> p.jump( s.cell( 0, 1 ) ) );
			s.after( 1.9f, () -> SkillFX.land( s.cell( 0, 1 ) ) );
			s.after( 2.9f, () -> Fx.dispose( stance ) );
		} );
		page.add( "rogue: a shadow cast, a blade's crescent, smoke blades", 2.6f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( -2, 1 ) );
			if (p == null) return;
			FxRecipes.castBloom( SkillVfx.midX( p.sprite ), SkillVfx.midY( p.sprite ), School.SHADOW.element );
			s.after( 0.4f, () -> ArcSpinFX.slash( p.sprite, 0xB89CE4, false ) );
			s.after( 0.9f, () -> SkillSpectacleFX.fly( SkillSpectacleFX.SHURIKEN, s.cell( -2, 1 ), s.dryRat.pos, 0, 0.4f ) );
			s.after( 1.5f, () -> SkillSpectacleFX.show( SkillSpectacleFX.SHADOW, s.skeleton.pos ) );
		} );
		page.add( "huntress: a raptor, briar thorns, a jaw snapping", 3f, s -> {
			SkillSpectacleFX.show( SkillSpectacleFX.RAPTOR, s.dryRat.pos );
			s.after( 0.6f, () -> {
				int[] around = { s.cell( -1, -1 ), s.cell( 0, -2 ), s.cell( 1, -1 ) };
				for (int c : around) SkillSpectacleFX.fly( SkillSpectacleFX.THORN, s.cell( 0, -1 ), c, 0, 0.45f );
			} );
			s.after( 1.6f, () -> SkillSpectacleFX.show( SkillSpectacleFX.JAW, s.skeleton.pos ) );
		} );
		page.add( "duelist: a fleche, a riposte, a pirouette", 3f, s -> {
			StreakFX.dash( Dungeon.hero.pos, s.cell( 3, 2 ), 0xFFFFFF, 0.5f, 0.3f, true );
			s.after( 0.3f, () -> ArcSpinFX.at( s.cell( 3, 2 ), 0xFFFFFF, 10, 0.25f, 200, 900, 0.22f ) );
			s.after( 1.0f, () -> {
				if (Dungeon.hero.sprite != null) ArcSpinFX.slash( Dungeon.hero.sprite, 0xFFFFFF, true );
			} );
			s.after( 1.8f, () -> {
				if (Dungeon.hero.sprite == null) return;
				ArcSpinFX.around( Dungeon.hero.sprite, 0xCCE0FF, 10, 0.5f, 0, 1080, 0.5f, true );
				ArcSpinFX.around( Dungeon.hero.sprite, 0xFFFFFF, 7, 0.35f, 180, 1080, 0.5f );
			} );
		} );
		page.add( "cleric: a prayer's pulses, a pillar, a smite", 3f, s -> {
			if (Dungeon.hero.sprite == null) return;
			FxRecipes.castBloom( SkillVfx.midX( Dungeon.hero.sprite ), SkillVfx.midY( Dungeon.hero.sprite ), School.HOLY.element );
			PulseRingFX.around( Dungeon.hero.sprite, 0xBBFFCC, 14, 0.55f );
			s.after( 0.08f, () -> PulseRingFX.around( Dungeon.hero.sprite, 0xFFF1A1, 10, 0.45f ) );
			s.after( 0.9f, () -> PillarRiseFX.show( s.dryRat.pos, 0xFFEE88, null ) );
			s.after( 1.9f, () -> PulseRingFX.at( s.skeleton.pos, 0xFFE9A0, 16, 0.45f ) );
		} );
	}

	//a field of a kind on two cells, of this floor, as a save would hold it
	private static SkillField field( int kind, int a, int b ){
		Bundle bundle = new Bundle();
		bundle.put( "kind", kind );
		bundle.put( "rank", 2 );
		bundle.put( "remaining", 10 );
		bundle.put( "age", 0 );
		bundle.put( "cells", new int[]{ a, b } );
		bundle.put( "depth", Dungeon.depth );
		bundle.put( "branch", Dungeon.branch );
		bundle.put( "origin", a );
		SkillField f = new SkillField();
		f.restoreFromBundle( bundle );
		return f;
	}

	@Override
	public void scenes( List<DebugScenes.Scene> out ){
		out.add( new Casts() );
	}

	/** "fx-skills": the skills' page, class casts and all, by day and at night, over and over. */
	private static final class Casts implements DebugScenes.Scene {
		@Override
		public String id(){
			return "fx-skills";
		}

		@Override
		public String title(){
			return "Effects: skills, a few casts of every class";
		}

		@Override
		public void apply( Hero hero ){
			//the gallery's own page runner stages it (FxStage is the gallery's to build)
			for (DebugScenes.Scene sc : FxGallery.SCENES){
				if (sc.id().equals( "fx-gallery-skills" )){
					sc.apply( hero );
					return;
				}
			}
		}
	}
}
