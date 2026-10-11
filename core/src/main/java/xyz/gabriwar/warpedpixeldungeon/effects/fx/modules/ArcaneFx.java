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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Warlock;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Chains;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.Enchanting;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Identification;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.Transmuting;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.ItemFx;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfAvalanche;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlink;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfCorrosion;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfDisintegration;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFireblast;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFirebolt;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfLivingEarth;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfPoison;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfPrismaticLight;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfTransfusion;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfWarding;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Dagger;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PointF;

import java.util.List;

/**
 * Bolts, beams and the arcane (the bolts-beams-arcane area: docs/fx/bolts-beams-arcane.md): every
 * wand's bolt and cone, the rays and the prismatic laser, flares, specks, enchanting, identifying,
 * transmuting and spell signs, chains, potions drunk and shattering, scrolls read and teleports.
 * Its pictures are effects/fx/arcane/'s.
 *
 * Its gallery page plays each of them on the stage (the prismatic laser over the floor and over
 * the pool); its scenes, fx-bolts and fx-bolts-night, hand the hero every bolt wand at +3 by a
 * pool with dummies in and out of the water, by day or at night, to zap for himself.
 */
public class ArcaneFx implements FxModule {

	@Override
	public String key(){
		return "bolts-beams-arcane";
	}

	@Override
	public String title(){
		return "Bolts, beams and the arcane";
	}

	//every bolt there is, one after another
	private static final int[] BOLTS = { MagicMissile.MAGIC_MISSILE, MagicMissile.FROST, MagicMissile.FIRE,
			MagicMissile.CORROSION, MagicMissile.FOLIAGE, MagicMissile.FORCE, MagicMissile.BEACON, MagicMissile.SHADOW,
			MagicMissile.RAINBOW, MagicMissile.EARTH, MagicMissile.WARD, MagicMissile.SHAMAN_RED, MagicMissile.SHAMAN_BLUE,
			MagicMissile.SHAMAN_PURPLE, MagicMissile.SHAMAN_ELMO, MagicMissile.ELMO, MagicMissile.POISON,
			MagicMissile.LIGHT_MISSILE };

	//a speck of every kind, one to a cell
	private static final int[] SPECKS = { Speck.HEALING, Speck.STAR, Speck.LIGHT, Speck.QUESTION, Speck.UP,
			Speck.SCREAM, Speck.BONE, Speck.WOOL, Speck.ROCK, Speck.NOTE, Speck.CHANGE, Speck.HEART, Speck.BUBBLE,
			Speck.STEAM, Speck.COIN, Speck.DOWN, Speck.DISCOVER, Speck.EVOKE, Speck.MASK, Speck.CROWN, Speck.RATTLE,
			Speck.JET, Speck.TOXIC, Speck.DUST, Speck.FORGE, Speck.CONFUSION, Speck.RED_LIGHT, Speck.YELLOW_LIGHT,
			Speck.BLUE_LIGHT, Speck.CALM };

	@Override
	public void exhibits( FxGallery.Page page ){
		page.add( "every bolt, from a warlock to the dry rat", 6f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, 0 ) );
			for (int i = 0; i < BOLTS.length; i++){
				final int type = BOLTS[i];
				s.after( 0.3f * i, () -> MagicMissile.boltFromChar( p.sprite.parent, type, p.sprite, s.dryRat.pos, null ) );
			}
		} );
		page.add( "a magic missile, a frost bolt and a fire bolt over the pool", 3f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( 1, 0 ) );
			int[] kinds = { MagicMissile.MAGIC_MISSILE, MagicMissile.FROST, MagicMissile.FIRE };
			for (int i = 0; i < kinds.length; i++){
				final int type = kinds[i], row = i - 1;
				s.after( 0.6f * i, () -> MagicMissile.boltFromChar( p.sprite.parent, type, p.sprite, s.cell( 7, row ), null ) );
			}
		} );
		page.add( "three cones: magic, fire and foliage", 3.5f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, 0 ) );
			int[] cones = { MagicMissile.MAGIC_MISS_CONE, MagicMissile.FIRE_CONE, MagicMissile.FOLIAGE_CONE };
			for (int c = 0; c < cones.length; c++){
				final int type = cones[c];
				s.after( 1.1f * c, () -> {
					for (int dy = -2; dy <= 2; dy++){
						MagicMissile.boltFromChar( p.sprite.parent, type, p.sprite, s.cell( 5, dy ), dy == 0 ? () -> {} : null );
					}
				} );
			}
		} );
		page.add( "the death, light, sun, health and supernova rays", 5f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, 0 ) );
			s.after( 0f, () -> p.sprite.parent.add( new Beam.DeathRay( p.sprite.center(), FxStage.center( s.cell( 6, -2 ) ) ) ) );
			s.after( 0.8f, () -> p.sprite.parent.add( new Beam.LightRay( p.sprite.center(), FxStage.center( s.cell( 6, -1 ) ) ) ) );
			s.after( 1.8f, () -> p.sprite.parent.add( new Beam.SunRay( p.sprite.center(), FxStage.center( s.cell( 6, 0 ) ) ) ) );
			s.after( 2.8f, () -> p.sprite.parent.add( new Beam.HealthRay( p.sprite.center(), FxStage.center( s.cell( 6, 1 ) ) ) ) );
			s.after( 3.4f, () -> p.sprite.parent.add( new Beam.SuperNovaRay( p.sprite.center(), FxStage.center( s.cell( 6, 2 ) ), 2f ) ) );
		} );
		page.add( "a sentry's lite rays", 2f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, 0 ) );
			for (int dy = -1; dy <= 1; dy++){
				p.sprite.parent.add( new Beam.DeathRay( p.sprite.center(), FxStage.center( s.cell( 5, dy ) ) ).lite() );
			}
		} );
		page.add( "the prismatic laser over the floor", 2f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, -2 ) );
			p.sprite.parent.add( new Beam.PrismRay( p.sprite.center(), FxStage.center( s.cell( 6, -2 ) ) ) );
		} );
		page.add( "the prismatic laser over the pool", 2f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( 1, 0 ) );
			p.sprite.parent.add( new Beam.PrismRay( p.sprite.center(), FxStage.center( s.cell( 7, 0 ) ) ) );
		} );
		page.add( "flares of 4, 8 and 12 rays, and an aura", 3f, s -> {
			new Flare( 4, 20 ).color( 0xFFFF80, true ).show( s.dryRat.sprite, 2f );
			new Flare( 8, 24 ).color( 0x80C0FF, true ).show( s.skeleton.sprite, 2f );
			new Flare( 12, 28 ).color( 0xFF80FF, true ).show( s.wetRat.sprite, 2f );
			if (s.bat.sprite != null) s.bat.sprite.aura( 0xFFD060, 6 );
			s.after( 2.8f, () -> {
				if (s.bat.sprite != null) s.bat.sprite.clearAura();
			} );
		} );
		page.add( "a speck of every kind", 3.5f, s -> {
			for (int i = 0; i < SPECKS.length; i++){
				s.burstCenter( s.cell( -6 + i % 10, -4 + i / 10 ), Speck.factory( SPECKS[i] ), 3 );
			}
		} );
		page.add( "enchanting, identifying, transmuting and a spell's sign", 3f, s -> {
			Enchanting.show( s.skeleton, new Dagger() );
			if (s.dryRat.sprite != null) s.dryRat.sprite.parent.add( new Identification( s.dryRat.sprite.center() ) );
			Transmuting.show( s.wetRat, new Dagger(), new PotionOfHealing() );
			SpellSprite.show( Dungeon.hero, SpellSprite.CHARGE );
		} );
		page.add( "chains, plain and ethereal", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, 0 ) );
			if (s.dryRat.sprite == null || s.skeleton.sprite == null) return;
			p.sprite.parent.add( new Chains( p.sprite.center(), s.dryRat.sprite.center(), Effects.Type.CHAIN, null ) );
			p.sprite.parent.add( new Chains( p.sprite.center(), s.skeleton.sprite.center(), Effects.Type.ETHEREAL_CHAIN, null ) );
		} );
		page.add( "a potion drunk, and shattering on the floor and in the pool", 2.5f, s -> {
			ItemFx.drink( s.dryRat, 0x9050FF );
			PointF floor = FxStage.center( s.cell( 0, -2 ) ), pool = FxStage.center( s.cell( 3, 1 ) );
			Splash.at( floor, 0x9050FF, 5 );
			ItemFx.shatter( floor.x, floor.y, 0x9050FF );
			Splash.at( pool, 0x9050FF, 5 );
			ItemFx.shatter( pool.x, pool.y, 0x9050FF );
		} );
		page.add( "a teleport out and in, and one into the pool", 3f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( 0, -2 ) );
			s.after( 0.3f, () -> {
				ItemFx.teleport( s.cell( 0, -2 ), s.cell( -3, 1 ), true, true );
				p.place( s.cell( -3, 1 ) );
			} );
			s.after( 1.6f, () -> {
				ItemFx.teleport( s.cell( -3, 1 ), s.cell( 3, -1 ), true, true );
				p.place( s.cell( 3, -1 ) );
			} );
		} );
		page.add( "a scroll read, a curse lifted, an upgrade and fear", 3.5f, s -> {
			ItemFx.read( Dungeon.hero, ItemFx.SCROLL );
			s.after( 0.6f, () -> {
				new Flare( 6, 32 ).show( s.skeleton.sprite, 2f );
				ItemFx.uncurse( s.skeleton );
			} );
			s.after( 1.4f, () -> ItemFx.upgrade( s.dryRat ) );
			s.after( 2.2f, () -> {
				new Flare( 5, 32 ).color( 0xFF0000, true ).show( Dungeon.hero.sprite, 2f );
				ItemFx.shock( Dungeon.hero, 0xFF3030, true );
			} );
		} );
		page.add( "magic mapping, a recharge and a blast of light", 3.5f, s -> {
			ItemFx.mapping( Dungeon.hero );
			ItemFx.secret( s.cell( 2, -3 ) );
			s.after( 1f, () -> {
				xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRecharging.charge( s.dryRat );
				ItemFx.recharge( s.dryRat );
			} );
			s.after( 2.2f, () -> ItemFx.bloom( Dungeon.hero ) );
		} );
		page.add( "notes and hearts flying, a potion of strength", 3f, s -> {
			ItemFx.flyToNear( Dungeon.hero, 8, Speck.NOTE );
			s.after( 1f, () -> ItemFx.flyToNear( Dungeon.hero, 8, Speck.HEART ) );
			s.after( 1.8f, () -> ItemFx.strength( Dungeon.hero ) );
		} );
	}

	@Override
	public void scenes( List<DebugScenes.Scene> out ){
		out.add( new Bolts( false ) );
		out.add( new Bolts( true ) );
	}

	//the bolt wands the hero is handed in the scene, every one at +3
	private static final Class<?>[] WANDS = { WandOfMagicMissile.class, WandOfFrost.class, WandOfFirebolt.class,
			WandOfFireblast.class, WandOfCorrosion.class, WandOfPoison.class, WandOfLivingEarth.class, WandOfWarding.class,
			WandOfBlink.class, WandOfAvalanche.class, WandOfDisintegration.class, WandOfPrismaticLight.class,
			WandOfTransfusion.class };

	/** The hero by a pool with dummies, every bolt wand +3 in his pack, by day or at night. */
	private static final class Bolts implements DebugScenes.Scene {
		final boolean night;

		Bolts( boolean night ){
			this.night = night;
		}

		@Override
		public String id(){
			return night ? "fx-bolts-night" : "fx-bolts";
		}

		@Override
		public String title(){
			return "Effects: every bolt wand +3 by a pool" + (night ? ", at night" : ", by day");
		}

		@Override
		public void apply( Hero hero ){
			//the blank room first (from the surface, the hero is sent below: run it again there)
			for (DebugScenes.Scene sc : DebugScenes.SCENES){
				if (sc.id().equals( "blank-room" )){
					boolean surface = Dungeon.level instanceof OverworldLevel;
					sc.apply( hero );
					if (surface){
						GLog.w( "Scene: sent below to carve the room; run " + id() + " again there" );
						return;
					}
				}
			}
			Level level = Dungeon.level;
			int w = level.width();
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = 2; dx <= 6; dx++){
					int c = hero.pos + dx + dy * w;
					Level.set( c, Terrain.WATER );
					level.visited[c] = level.mapped[c] = true;
				}
			}
			GameScene.updateMap();
			level.cleanWalls();
			GameScene.updateFog();
			dummy( hero.pos + 4, level );
			dummy( hero.pos + 6 - w, level );
			dummy( hero.pos - 3 + 2 * w, level );
			dummy( hero.pos + 3 - 3 * w, level );
			for (Class<?> c : WANDS){
				try {
					Item wand = (Item) c.getDeclaredConstructor().newInstance();
					wand.upgrade( 3 ).identify().collect();
				} catch (Exception e){
					GLog.w( "Scene: no " + c.getSimpleName() );
				}
			}
			DayNightCycle.debugPhaseOverride = night ? DayNightCycle.Phase.NIGHT : DayNightCycle.Phase.DAY;
			ClimateManager.onHeroTurn();
			GameScene.updateDayNightTint();
			GLog.i( "Scene: every bolt wand +3 in the pack; a pool east with a rat in it, dummies dry and wet"
					+ (night ? "; held at night" : "; held by day") + " (the debug menu's Day phase override lets it go)" );
		}

		//a rat that takes whatever it is given and stands there
		private static void dummy( int cell, Level level ){
			if (cell < 0 || cell >= level.length() || xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( cell ) != null) return;
			Mob m = new Rat();
			m.pos = cell;
			m.HP = m.HT = FxStage.HARDY;
			m.state = m.PASSIVE;
			GameScene.add( m );
		}
	}
}
