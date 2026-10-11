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
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
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
		SkillFieldFX.clearCap();
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
		page.add( "spectacles: hammer, shuriken, thorn, wings, jaw, slash, shadow", 5f, s -> {
			int[] kinds = { SkillSpectacleFX.HAMMER, SkillSpectacleFX.SHURIKEN, SkillSpectacleFX.THORN,
					SkillSpectacleFX.WINGS, SkillSpectacleFX.JAW, SkillSpectacleFX.SLASH, SkillSpectacleFX.SHADOW };
			for (int i = 0; i < kinds.length; i++){
				final int kind = kinds[i];
				s.after( 0.7f * i, () -> {
					if (kind == SkillSpectacleFX.SHURIKEN) SkillSpectacleFX.fly( kind, s.cell( -3, 1 ), s.dryRat.pos, 0, 0.4f );
					else SkillSpectacleFX.show( kind, s.dryRat.pos );
				} );
			}
		} );
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
