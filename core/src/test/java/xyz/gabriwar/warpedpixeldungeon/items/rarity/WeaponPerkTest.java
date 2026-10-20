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

package xyz.gabriwar.warpedpixeldungeon.items.rarity;

import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Disarmed;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RivalStatue;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Crop;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Farming;
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Gathering;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cornwheatshaft;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.AssassinsBlade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.AssassinsKnife;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Axe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.BattleAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Crossbow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Cudgel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Dagger;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ElfBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.FightGloves;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flute;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Glaive;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Gloves;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greataxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greatshield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HandAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HandLight;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Harp;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Katana;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.LargeSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Mace;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MasonsHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MegaCannon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MinersTool;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MiniGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MirrorDoll;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Nunchakus;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.PrayerWheel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Quarterstaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Rapier;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.RoundShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Saber;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ShootGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shovel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Spade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Spear;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.StoneCross;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.TaurcenBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Triangolo;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.TrickSand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Trumpet;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WarHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WarScythe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Wardrum;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Whip;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WindBottle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WoodcuttersAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.ChainFlail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.ChainWhip;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.MeisterHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.UnformedBlade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.Bow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.FT.FT;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.Gun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.LG.LG;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.CromCruachAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.LokisFlail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.NeptunusTrident;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Bolas;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Boomerang;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ForceCube;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ForestDart;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Javelin;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.JupitersWraith;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Kunai;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.SmallChakram;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingKnife;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Tomahawk;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.alchemy.Cross;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.HighGrass;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Cornwheat;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.HERO;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.W;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.dummy;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.only;

/** The type perks: which group every weapon falls in, that every perk's text is whole at every
 *  type, and the groups this batch made or changed doing what they say: Gunner, Ambush's sense,
 *  keen throws that fly back and pass walls, Heave, Arc, Reaper and the rival statue's sweep. */
public class WeaponPerkTest {

	private Level savedLevel;
	private Hero savedHero;
	private String savedVersion;

	@BeforeClass
	public static void boot() throws Exception {
		QualitySourceTest.boot();
		//an item's action cancels the scene's cell selector first (GameScene.cancel): headless
		//there is no scene, so a bare selector stands in for its one, as in HazardWatchTest
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		if (sel.get( null ) == null){
			sel.set( null, bare( CellSelector.class ) );
			standIn = true;
		}
	}

	private static boolean standIn;

	@AfterClass
	public static void unboot() throws Exception {
		if (!standIn) return;
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		sel.set( null, null );
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		//a level's cells reach the co-op layer, which reads the game's version as it starts
		savedVersion = Game.version;
		Game.version = "test";
		QualitySourceTest.arena();
	}

	@After
	public void tearDown(){
		Actor.clear();
		HighGrass.reaper = null;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Game.version = savedVersion;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	// ---------------------------------------------------------------- helpers

	private static Quality typed( Item item, ItemType type ){
		Quality q = only( item, Rarity.RARE, 1f );
		q.type = type;
		return q;
	}

	private static Object call( Object on, Class<?> owner, String name, Class<?>[] types, Object... args ){
		try {
			Method m = owner.getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( on, args );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	private static Object field( Object on, Class<?> owner, String name ){
		try {
			Field f = owner.getDeclaredField( name );
			f.setAccessible( true );
			return f.get( on );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	private static void setField( Object on, Class<?> owner, String name, Object value ){
		try {
			Field f = owner.getDeclaredField( name );
			f.setAccessible( true );
			f.set( on, value );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	//a thrown stack in the pack, and the one the throw splits off it (as Item.cast detaches it)
	private static MissileWeapon thrownFrom( MissileWeapon stack ){
		Hero hero = Dungeon.hero;
		stack.identify( false );
		assertTrue( stack.collect( hero.belongings.backpack ) );
		return (MissileWeapon) stack.detach( hero.belongings.backpack );
	}

	//an armed heave, landing on `cell` as a throw's onThrow aims it
	private static void heave( Hero hero, int cell ){
		Buff.affect( hero, WeaponPerk.Heave.class, WeaponPerk.Heave.DURATION ).cell = cell;
	}

	//an enemy no throw can touch
	private static Char dodger( int pos ){
		Char ch = new Char(){
			@Override public int defenseSkill( Char enemy ){ return Char.INFINITE_EVASION; }
		};
		ch.pos = pos;
		ch.HP = ch.HT = 1000;
		ch.alignment = Char.Alignment.ENEMY;
		ch.sprite = QualitySourceTest.quietSprite();
		Actor.add( ch );
		return ch;
	}

	private static void onThrow( Item thrown, int cell ){
		call( thrown, Item.class, "onThrow", new Class<?>[]{ int.class }, cell );
	}

	// ---------------------------------------------------------------- groups

	private static Map<Supplier<Item>, WeaponPerk> table(){
		Map<Supplier<Item>, WeaponPerk> t = new LinkedHashMap<>();
		//GAME 1: the weapons that changed group, and those that stayed
		t.put( FT::new, WeaponPerk.GUN );
		t.put( LG::new, WeaponPerk.GUN );
		t.put( MiniGun::new, WeaponPerk.GUN );
		t.put( ShootGun::new, WeaponPerk.GUN );
		t.put( Bow::new, WeaponPerk.SHOT );
		t.put( Crossbow::new, WeaponPerk.SHOT );
		t.put( SpiritBow::new, WeaponPerk.SHOT );
		t.put( ElfBow::new, WeaponPerk.SHOT );
		t.put( TaurcenBow::new, WeaponPerk.SHOT );
		t.put( MegaCannon::new, WeaponPerk.SHOT );
		t.put( Sickle::new, WeaponPerk.SCYTHE );
		t.put( WarScythe::new, WeaponPerk.SCYTHE );
		t.put( Flail::new, WeaponPerk.WILD );
		t.put( Boomerang::new, WeaponPerk.BOOMERANG );
		t.put( HeavyBoomerang::new, WeaponPerk.BOOMERANG );
		t.put( JupitersWraith::new, WeaponPerk.BOOMERANG );
		//the neighbours they left: a light blade, light and heavy throws, the cross that circles back
		t.put( Dagger::new, WeaponPerk.KEEN );
		t.put( ThrowingKnife::new, WeaponPerk.THROW );
		t.put( Javelin::new, WeaponPerk.IMPACT );
		t.put( ForceCube::new, WeaponPerk.IMPACT );
		t.put( Cross::new, WeaponPerk.IMPACT );
		t.put( Dart::new, WeaponPerk.DART );
		//the regroup: every weapon that moved, by its class
		t.put( WoodcuttersAxe::new, WeaponPerk.WOODSMAN );
		t.put( HandAxe::new, WeaponPerk.WOODSMAN );
		t.put( Axe::new, WeaponPerk.WOODSMAN );
		t.put( BattleAxe::new, WeaponPerk.WOODSMAN );
		t.put( Greataxe::new, WeaponPerk.WOODSMAN );
		t.put( CromCruachAxe::new, WeaponPerk.WOODSMAN );
		t.put( Pickaxe::new, WeaponPerk.QUARRY );
		t.put( MasonsHammer::new, WeaponPerk.QUARRY );
		t.put( Shovel::new, WeaponPerk.QUARRY );
		t.put( WarHammer::new, WeaponPerk.QUARRY );
		t.put( MeisterHammer::new, WeaponPerk.QUARRY );
		t.put( Spade::new, WeaponPerk.QUARRY );
		t.put( MinersTool::new, WeaponPerk.QUARRY );
		t.put( Triangolo::new, WeaponPerk.BARD );
		t.put( Flute::new, WeaponPerk.BARD );
		t.put( Wardrum::new, WeaponPerk.BARD );
		t.put( Trumpet::new, WeaponPerk.BARD );
		t.put( Harp::new, WeaponPerk.BARD );
		t.put( Whip::new, WeaponPerk.LASH );
		t.put( ChainWhip::new, WeaponPerk.LASH );
		t.put( ChainFlail::new, WeaponPerk.WILD );
		t.put( LokisFlail::new, WeaponPerk.WILD );
		t.put( AssassinsBlade::new, WeaponPerk.KEEN );
		t.put( UnformedBlade::new, WeaponPerk.KEEN );
		t.put( FightGloves::new, WeaponPerk.SWIFT );
		t.put( Katana::new, WeaponPerk.GUARD );
		t.put( Rapier::new, WeaponPerk.GUARD );
		t.put( Saber::new, WeaponPerk.GUARD );
		t.put( Quarterstaff::new, WeaponPerk.GUARD );
		t.put( ForestDart::new, WeaponPerk.DART );
		t.put( Kunai::new, WeaponPerk.THROW );
		t.put( Bolas::new, WeaponPerk.THROW );
		t.put( SmallChakram::new, WeaponPerk.THROW );
		//and the neighbours left where they were
		t.put( Spear::new, WeaponPerk.REACH );
		t.put( Glaive::new, WeaponPerk.REACH );
		t.put( WindBottle::new, WeaponPerk.REACH );
		t.put( HandLight::new, WeaponPerk.REACH );
		t.put( TrickSand::new, WeaponPerk.REACH );
		t.put( MirrorDoll::new, WeaponPerk.REACH );
		t.put( Gloves::new, WeaponPerk.SWIFT );
		t.put( AssassinsKnife::new, WeaponPerk.SWIFT );
		t.put( Cudgel::new, WeaponPerk.CRUSH );
		t.put( Mace::new, WeaponPerk.CRUSH );
		t.put( LargeSword::new, WeaponPerk.CRUSH );
		t.put( PrayerWheel::new, WeaponPerk.CRUSH );
		t.put( StoneCross::new, WeaponPerk.CRUSH );
		t.put( Longsword::new, WeaponPerk.MOMENTUM );
		t.put( Nunchakus::new, WeaponPerk.MOMENTUM );
		t.put( NeptunusTrident::new, WeaponPerk.MOMENTUM );
		t.put( RoundShield::new, WeaponPerk.GUARD );
		t.put( MagesStaff::new, WeaponPerk.SIPHON );
		t.put( Tomahawk::new, WeaponPerk.IMPACT );
		t.put( ThrowingHammer::new, WeaponPerk.IMPACT );
		return t;
	}

	@Test
	public void everyWeaponIsInItsGroup(){
		for (int pass = 0; pass < 2; pass++){
			//the groups never read the hero: they hold without one too (the catalog has none)
			if (pass == 1) Dungeon.hero = null;
			for (Map.Entry<Supplier<Item>, WeaponPerk> e : table().entrySet()){
				Item item = e.getKey().get();
				assertEquals( item.getClass().getSimpleName() + (pass == 1 ? " (no hero)" : ""),
						e.getValue(), WeaponPerk.of( item ) );
			}
		}
	}

	@Test
	public void theNewGroupsHoldOnlyTheirOwn(){
		Generator.Category[] cats = {
				Generator.Category.WEP_T1, Generator.Category.WEP_T2, Generator.Category.WEP_T3,
				Generator.Category.WEP_T4, Generator.Category.WEP_T5,
				Generator.Category.MIS_T1, Generator.Category.MIS_T2, Generator.Category.MIS_T3,
				Generator.Category.MIS_T4, Generator.Category.MIS_T5 };
		int seen = 0;
		for (Generator.Category cat : cats){
			for (Class<?> c : cat.classes){
				Item item = (Item) Reflection.newInstance( c );
				WeaponPerk p = WeaponPerk.of( item );
				String what = c.getSimpleName() + " is " + p;
				assertEquals( what, item instanceof Gun || item instanceof MiniGun || item instanceof ShootGun,
						p == WeaponPerk.GUN );
				assertEquals( what, item instanceof Sickle || item instanceof WarScythe, p == WeaponPerk.SCYTHE );
				assertEquals( what, item instanceof HeavyBoomerang || item instanceof JupitersWraith,
						p == WeaponPerk.BOOMERANG );
				assertEquals( what, item instanceof WoodcuttersAxe || item instanceof HandAxe || item instanceof Axe
						|| item instanceof BattleAxe || item instanceof Greataxe || item instanceof CromCruachAxe,
						p == WeaponPerk.WOODSMAN );
				assertEquals( what, item instanceof Pickaxe || item instanceof MasonsHammer || item instanceof WarHammer
						|| item instanceof MeisterHammer || item instanceof Shovel, p == WeaponPerk.QUARRY );
				assertEquals( what, item instanceof Triangolo || item instanceof Flute || item instanceof Wardrum
						|| item instanceof Trumpet || item instanceof Harp, p == WeaponPerk.BARD );
				assertEquals( what, item instanceof Whip || item instanceof ChainWhip, p == WeaponPerk.LASH );
				assertEquals( what, item instanceof Flail || item instanceof ChainFlail || item instanceof LokisFlail,
						p == WeaponPerk.WILD );
				seen++;
			}
		}
		assertTrue( seen > 40 );
	}

	// ---------------------------------------------------------------- text

	private static Map<WeaponPerk, Supplier<Item>> sample(){
		Map<WeaponPerk, Supplier<Item>> s = new HashMap<>();
		s.put( WeaponPerk.SHOT, Bow::new );
		s.put( WeaponPerk.GUARD, RoundShield::new );
		s.put( WeaponPerk.REACH, Glaive::new );
		s.put( WeaponPerk.SWIFT, Gloves::new );
		s.put( WeaponPerk.CRUSH, Cudgel::new );
		s.put( WeaponPerk.WILD, Flail::new );
		s.put( WeaponPerk.KEEN, Dagger::new );
		s.put( WeaponPerk.SIPHON, MagesStaff::new );
		s.put( WeaponPerk.MOMENTUM, Longsword::new );
		s.put( WeaponPerk.DART, Dart::new );
		s.put( WeaponPerk.THROW, ThrowingKnife::new );
		s.put( WeaponPerk.IMPACT, Javelin::new );
		s.put( WeaponPerk.GUN, FT::new );
		s.put( WeaponPerk.BOOMERANG, HeavyBoomerang::new );
		s.put( WeaponPerk.SCYTHE, Sickle::new );
		s.put( WeaponPerk.WOODSMAN, HandAxe::new );
		s.put( WeaponPerk.QUARRY, WarHammer::new );
		s.put( WeaponPerk.BARD, Flute::new );
		s.put( WeaponPerk.LASH, Whip::new );
		return s;
	}

	private static final Pattern ARG = Pattern.compile( "%(\\d)\\$" );

	@Test
	public void everyPerkReadsWholeAtEveryType(){
		Map<WeaponPerk, Supplier<Item>> sample = sample();
		for (WeaponPerk p : WeaponPerk.values()){
			Item item = sample.get( p ).get();
			assertEquals( p + " sample", p, WeaponPerk.of( item ) );
			assertFalse( p.title(), p.title().contains( Messages.NO_TEXT_FOUND ) );
			for (ItemType t : ItemType.values()){
				String[] args = { p.title(), p.amount( item, t ), p.extra( item, t ), p.extra2( t ) };
				String key = p.name().toLowerCase();
				//every number the line's text names is filled
				Matcher m = ARG.matcher( Messages.get( WeaponPerk.class, key ) );
				while (m.find()) assertFalse( p + " " + t + " %" + m.group( 1 ), args[Integer.parseInt( m.group( 1 ) ) - 1].isEmpty() );
				//the twist is formatted with the second number alone
				m = ARG.matcher( Messages.get( WeaponPerk.class, key + "_twist" ) );
				while (m.find()){
					assertEquals( p + " twist %" + m.group( 1 ), "1", m.group( 1 ) );
					assertFalse( p + " " + t + " twist", args[2].isEmpty() );
				}
				String text = p.describe( item, t );
				assertFalse( p + " " + t + ": " + text, text.contains( Messages.NO_TEXT_FOUND ) );
				assertFalse( p + " " + t + ": " + text, text.contains( "__" ) || text.contains( "%" + "1$" ) );
			}
		}
		assertEquals( "_Ambush:_ _surprise attacks_ deal _30%_ more damage and ignore _24%_ of the target's armor. "
				+ "While you hold it you _sense enemies_ within _2 tiles_, even through walls.",
				WeaponPerk.KEEN.describe( new Dagger(), ItemType.BETA ) );
		assertTrue( WeaponPerk.KEEN.describe( new Dagger(), ItemType.GAMMA ).contains( "_1 tile_" ) );
		assertEquals( "_Heave:_ _heave_ it instead of throwing: the throw takes _two turns_ and deals _150%_ more damage. "
				+ "A heave lands with a _shockwave_ that hits every enemy beside the target for _40%_ of the blow.",
				WeaponPerk.IMPACT.describe( new Javelin(), ItemType.ALPHA ) );
	}

	// ---------------------------------------------------------------- Gunner

	@Test
	public void aGunnerReloadsFreeAndAnAlphaSparesRounds(){
		FT ft = new FT();
		Quality q = typed( ft, ItemType.GAMMA );
		assertEquals( 0.10f, Quality.freeReload( ft ), 1e-5f );
		assertEquals( 0f, Quality.spareAmmo( ft ), 1e-5f );
		q.type = ItemType.BETA;
		assertEquals( 0.20f, Quality.freeReload( ft ), 1e-5f );
		assertEquals( 0f, Quality.spareAmmo( ft ), 1e-5f );
		q.type = ItemType.ALPHA;
		assertEquals( 0.30f, Quality.freeReload( ft ), 1e-5f );
		assertEquals( 0.20f, Quality.spareAmmo( ft ), 1e-5f );

		//with the lines: summed, and capped at a half
		q.lines.add( RarityLine.RELOAD );
		q.rolls.add( 1f );
		q.lines.add( RarityLine.AMMO );
		q.rolls.add( 1f );
		assertEquals( 0.45f, Quality.freeReload( ft ), 1e-5f );
		assertEquals( 0.35f, Quality.spareAmmo( ft ), 1e-5f );
		q.masterwork = Quality.MASTERWORK_MAX;
		assertEquals( 0.5f, Quality.freeReload( ft ), 1e-5f );
		assertEquals( 0.5f, Quality.spareAmmo( ft ), 1e-5f );
		int free = 0, spared = 0, n = 20000;
		Random.pushGenerator( 0x6A11L );
		try {
			for (int i = 0; i < n; i++){
				if (Quality.rollFreeReload( ft, null )) free++;
				if (Quality.rollSpareAmmo( ft, null )) spared++;
			}
		} finally {
			Random.popGenerator();
		}
		assertEquals( 0.5f, free / (float) n, 0.02f );
		assertEquals( 0.5f, spared / (float) n, 0.02f );

		//an alpha bow is a Marksman: no spared rounds of the perk's
		Bow bow = new Bow();
		typed( bow, ItemType.ALPHA );
		assertEquals( 0f, Quality.spareAmmo( bow ), 1e-5f );
		assertEquals( 0f, Quality.freeReload( bow ), 1e-5f );
	}

	@Test
	public void aGunnerKeepsTheMarksmansReachBonusButNotItsSureShot(){
		Hero hero = Dungeon.hero;
		FT ft = new FT();
		typed( ft, ItemType.ALPHA );
		hero.belongings.weapon = ft;
		//seven tiles out: past the ramp, at the alpha ceiling of +45%
		Char far = dummy( HERO + 7, Char.Alignment.ENEMY, 1000 );
		assertEquals( 145, Quality.weaponHit( ft, hero, far, 100 ) );
		assertFalse( Quality.sureHit( ft, hero, far ) );
		assertEquals( 0f, Quality.drPierce( hero, far ), 1e-5f );
		Bow bow = new Bow();
		typed( bow, ItemType.ALPHA );
		hero.belongings.weapon = bow;
		assertTrue( Quality.sureHit( bow, hero, far ) );
	}

	// ---------------------------------------------------------------- Ambush

	@Test
	public void anAmbushBladeSensesByItsType(){
		Hero hero = Dungeon.hero;
		Dagger dagger = new Dagger();
		hero.belongings.weapon = dagger;
		assertEquals( "the starting kit's untyped dagger", 0, Quality.senseRange( hero ) );
		Quality q = typed( dagger, ItemType.GAMMA );
		assertEquals( 1, Quality.senseRange( hero ) );
		q.type = ItemType.BETA;
		assertEquals( 2, Quality.senseRange( hero ) );
		q.type = ItemType.ALPHA;
		assertEquals( 3, Quality.senseRange( hero ) );
		Longsword sword = new Longsword();
		typed( sword, ItemType.ALPHA );
		hero.belongings.weapon = sword;
		assertEquals( 0, Quality.senseRange( hero ) );
		//carried, not held: nothing
		assertTrue( dagger.collect( hero.belongings.backpack ) );
		assertEquals( 0, Quality.senseRange( hero ) );
		assertEquals( 0, Quality.senseRange( null ) );
	}

	// ---------------------------------------------------------------- Keen

	@Test
	public void aKeenHitEarnsTheReturn(){
		Hero hero = Dungeon.hero;
		ThrowingKnife knife = new ThrowingKnife();
		Quality q = typed( knife, ItemType.BETA );
		Char target = dummy( HERO + 3, Char.Alignment.ENEMY, 1000 );
		q.keenHit = true;
		assertEquals( 150, WeaponPerk.THROW.onHit( q, knife, hero, target, 100 ) );
		assertTrue( q.returnNext );
		//every throw's roll starts clean
		Random.pushGenerator( 0x7E57L );
		try {
			WeaponPerk.THROW.throwRoll( q, 10, 10 );
		} finally {
			Random.popGenerator();
		}
		assertFalse( q.returnNext );
	}

	@Test
	public void aKeenThrowFliesBackAndWearsOneUse(){
		ThrowingKnife stack = new ThrowingKnife();
		typed( stack, ItemType.GAMMA );
		int quantity = stack.quantity();
		MissileWeapon thrown = thrownFrom( stack );
		assertEquals( quantity - 1, stack.quantity() );
		float perUse = stack.durabilityPerUse();
		Char target = dummy( HERO + 3, Char.Alignment.ENEMY, 1000 );
		Quality.of( thrown ).returnNext = true;
		call( thrown, MissileWeapon.class, "rangedHit", new Class<?>[]{ Char.class, int.class }, target, target.pos );
		assertEquals( "back in the stack", quantity, stack.quantity() );
		assertEquals( 0, Dungeon.level.heaps.valueList().size() );
		assertNull( target.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.PinCushion.class ) );
		assertEquals( MissileWeapon.MAX_DURABILITY - perUse, stack.durabilityLeft(), 1e-3f );
		assertFalse( Quality.of( thrown ).returnNext );

		//no keen hit: it sticks where it struck, as before
		MissileWeapon again = thrownFrom( stack );
		call( again, MissileWeapon.class, "rangedHit", new Class<?>[]{ Char.class, int.class }, target, target.pos );
		assertEquals( quantity - 1, stack.quantity() );
		assertNotNull( target.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.PinCushion.class ) );
	}

	@Test
	public void aKeenThrowIntoAFullPackFallsAtTheThrowersFeet(){
		Hero hero = Dungeon.hero;
		ThrowingKnife last = new ThrowingKnife();
		typed( last, ItemType.GAMMA );
		last.quantity( 1 );
		//the last of its stack: nothing in the pack for it to rejoin
		MissileWeapon thrown = thrownFrom( last );
		assertSame( last, thrown );
		int filled = 0;
		while (new Dagger().collect( hero.belongings.backpack )) assertTrue( ++filled < 200 );
		Char target = dummy( HERO + 3, Char.Alignment.ENEMY, 1000 );
		Quality.of( thrown ).returnNext = true;
		call( thrown, MissileWeapon.class, "rangedHit", new Class<?>[]{ Char.class, int.class }, target, target.pos );
		Heap feet = Dungeon.level.heaps.get( hero.pos );
		assertNotNull( "at the thrower's feet", feet );
		assertSame( thrown, feet.peek() );
		assertNull( "not where it struck", Dungeon.level.heaps.get( target.pos ) );
		assertNull( target.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.PinCushion.class ) );
	}

	@Test
	public void anAlphaThrowReachesThreeTilesThroughAWall(){
		Hero hero = Dungeon.hero;
		Level.set( HERO + 1, Terrain.WALL, Dungeon.level );
		Level.set( HERO + 1 + W, Terrain.WALL, Dungeon.level );
		Level.set( HERO + 1 - W, Terrain.WALL, Dungeon.level );
		dummy( HERO + 3, Char.Alignment.ENEMY, 100 );
		dummy( HERO + 4 + W, Char.Alignment.ENEMY, 100 );
		ThrowingKnife knife = new ThrowingKnife();
		Quality q = typed( knife, ItemType.ALPHA );
		assertEquals( HERO + 3, knife.throwPos( hero, HERO + 3 ) );
		assertNotEquals( "four tiles is too far", HERO + 4 + W, knife.throwPos( hero, HERO + 4 + W ) );
		assertNotEquals( "nobody there", HERO + 2, knife.throwPos( hero, HERO + 2 ) );
		q.type = ItemType.BETA;
		assertNotEquals( "only at alpha", HERO + 3, knife.throwPos( hero, HERO + 3 ) );
	}

	// ---------------------------------------------------------------- Heave

	@Test
	public void heaveIsOfferedOnlyOnAKnownTypedHeavyThrow(){
		Hero hero = Dungeon.hero;
		Javelin javelin = new Javelin();
		assertFalse( javelin.actions( hero ).contains( MissileWeapon.AC_HEAVE ) );
		typed( javelin, ItemType.GAMMA );
		assertFalse( "unidentified", javelin.actions( hero ).contains( MissileWeapon.AC_HEAVE ) );
		javelin.identify( false );
		assertTrue( javelin.actions( hero ).contains( MissileWeapon.AC_HEAVE ) );
		assertEquals( "HEAVE", javelin.actionName( MissileWeapon.AC_HEAVE, hero ) );

		ThrowingKnife knife = new ThrowingKnife();
		typed( knife, ItemType.ALPHA );
		knife.identify( false );
		assertFalse( knife.actions( hero ).contains( MissileWeapon.AC_HEAVE ) );
		HeavyBoomerang boomerang = new HeavyBoomerang();
		typed( boomerang, ItemType.ALPHA );
		boomerang.identify( false );
		assertFalse( boomerang.actions( hero ).contains( MissileWeapon.AC_HEAVE ) );
	}

	@Test
	public void aHeaveTakesATurnMoreAndHitsByItsType(){
		Hero hero = Dungeon.hero;
		Javelin javelin = new Javelin();
		Quality q = typed( javelin, ItemType.GAMMA );
		Char target = dummy( HERO + 3, Char.Alignment.ENEMY, 100000 );
		float plain = javelin.castDelay( hero, target.pos );
		ForceCube cube = new ForceCube();
		float cubePlain = cube.castDelay( hero, HERO + 2 );
		Buff.affect( hero, WeaponPerk.Heave.class, WeaponPerk.Heave.DURATION );
		assertEquals( plain + 1f, javelin.castDelay( hero, target.pos ), 1e-5f );
		assertEquals( "the cube on empty ground", cubePlain + 1f, cube.castDelay( hero, HERO + 2 ), 1e-5f );

		int[] expected = { 150, 200, 250 };
		ItemType[] types = { ItemType.GAMMA, ItemType.BETA, ItemType.ALPHA };
		for (int i = 0; i < 3; i++){
			q.type = types[i];
			heave( hero, target.pos );
			assertEquals( types[i].toString(), expected[i], Quality.weaponHit( javelin, hero, target, 100 ) );
			assertNull( "spent on the blow", hero.buff( WeaponPerk.Heave.class ) );
		}
		//a blow at anyone but the one the heave came down on is a plain blow, and leaves it
		Char beside = dummy( HERO + 4, Char.Alignment.ENEMY, 100000 );
		heave( hero, target.pos );
		assertEquals( 100, Quality.weaponHit( javelin, hero, beside, 100 ) );
		assertNotNull( hero.buff( WeaponPerk.Heave.class ) );
		assertEquals( 250, Quality.weaponHit( javelin, hero, target, 100 ) );
		assertNull( hero.buff( WeaponPerk.Heave.class ) );
		//a plain throw is a plain throw: the untouched-target bonus is gone
		assertEquals( 100, Quality.weaponHit( javelin, hero, target, 100 ) );
	}

	@Test
	public void anAlphaHeavesShockwaveSparesAllies(){
		Hero hero = Dungeon.hero;
		Javelin javelin = new Javelin();
		Quality q = typed( javelin, ItemType.ALPHA );
		int t = HERO + 5;
		Char target = dummy( t, Char.Alignment.ENEMY, 100000 );
		Char foe = dummy( t + 1, Char.Alignment.ENEMY, 1000 );
		Char foe2 = dummy( t + W, Char.Alignment.ENEMY, 1000 );
		Char friend = dummy( t - W, Char.Alignment.ALLY, 1000 );
		Char far = dummy( t + 2, Char.Alignment.ENEMY, 1000 );
		heave( hero, t );
		assertEquals( 250, Quality.weaponHit( javelin, hero, target, 100 ) );
		assertEquals( 900, foe.HP );
		assertEquals( 900, foe2.HP );
		assertEquals( 1000, friend.HP );
		assertEquals( 1000, far.HP );

		//below alpha, no shockwave
		q.type = ItemType.BETA;
		heave( hero, t );
		assertEquals( 200, Quality.weaponHit( javelin, hero, target, 100 ) );
		assertEquals( 900, foe.HP );
	}

	@Test
	public void aHeaveIsSpentOnAMissAndOnBareGround(){
		Hero hero = Dungeon.hero;
		Javelin stack = new Javelin();
		typed( stack, ItemType.ALPHA );
		//on bare ground there is nothing to heave, and the heave goes with the throw
		MissileWeapon thrown = thrownFrom( stack );
		//no heap where it lands: a test's level draws none
		thrown.spawnedForEffect = true;
		Buff.affect( hero, WeaponPerk.Heave.class, WeaponPerk.Heave.DURATION );
		onThrow( thrown, HERO + 3 );
		assertNull( "bare ground", hero.buff( WeaponPerk.Heave.class ) );

		Char foe = dodger( HERO + 3 );
		Char beside = dummy( HERO + 4, Char.Alignment.ENEMY, 1000 );
		MissileWeapon again = thrownFrom( stack );
		again.spawnedForEffect = true;
		Buff.affect( hero, WeaponPerk.Heave.class, WeaponPerk.Heave.DURATION );
		onThrow( again, foe.pos );
		assertNull( "a miss", hero.buff( WeaponPerk.Heave.class ) );
		assertEquals( 1000, foe.HP );
		assertEquals( "no shockwave from a miss", 1000, beside.HP );
	}

	@Test
	public void aHeavedCubeHeavesOnlyWhatItLandsOn(){
		//the cube's blast strikes the furthest first: here the enemy behind the target and the
		//ally beside it, both before the one on the cell. The same throw, heaved or not, under
		//the same dice: only the one on the cell takes more
		int[] plain = cubeBlast( false, true );
		int[] heaved = cubeBlast( true, true );
		assertTrue( "the cube hits", plain[0] > 0 && plain[1] > 0 );
		assertTrue( "the one on the cell is heaved", heaved[0] > plain[0] );
		assertEquals( "the enemy behind it is not", plain[1], heaved[1] );
		assertEquals( "nor the ally beside it", plain[2], heaved[2] );
		assertEquals( "spent", 0, heaved[3] );

		//thrown at bare ground, the heave is spent for nothing
		int[] bare = cubeBlast( true, false );
		int[] barePlain = cubeBlast( false, false );
		assertEquals( barePlain[1], bare[1] );
		assertEquals( barePlain[2], bare[2] );
		assertEquals( 0, bare[3] );
	}

	//a beta Force Cube thrown 3 tiles out in a fresh arena, heaved or not, with or without an
	//enemy on its cell: what it took off that enemy, the enemy behind it and the ally beside
	//it, and 1 if the heave was left over
	private static int[] cubeBlast( boolean heaved, boolean onTarget ){
		Hero hero = QualitySourceTest.arena();
		standInBlast( hero );
		int cell = HERO + 3;
		Char target = onTarget ? dummy( cell, Char.Alignment.ENEMY, 100000 ) : null;
		Char behind = dummy( cell + 1, Char.Alignment.ENEMY, 100000 );
		Char friend = dummy( cell + W, Char.Alignment.ALLY, 100000 );
		ForceCube stack = new ForceCube();
		typed( stack, ItemType.BETA );
		//its last throw: it breaks, and leaves no heap for a test's level to draw
		stack.damage( MissileWeapon.MAX_DURABILITY );
		MissileWeapon cube = thrownFrom( stack );
		if (heaved) Buff.affect( hero, WeaponPerk.Heave.class, WeaponPerk.Heave.DURATION );
		Random.pushGenerator( 0xC0BEL );
		try {
			onThrow( cube, cell );
		} finally {
			Random.popGenerator();
		}
		return new int[]{ target == null ? 0 : 100000 - target.HP, 100000 - behind.HP, 100000 - friend.HP,
				hero.buff( WeaponPerk.Heave.class ) == null ? 0 : 1 };
	}

	//the cube's blast wave is drawn on the hero sprite's parent: headless, a group holding a bare
	//wave it can recycle stands in for the scene, so no texture is ever made
	private static void standInBlast( Hero hero ){
		Group scene = new Group();
		scene.add( hero.sprite );
		WandOfBlastWave.BlastWave wave = bare( WandOfBlastWave.BlastWave.class );
		wave.scale = new PointF();
		wave.origin = new PointF();
		scene.add( wave );
	}

	@SuppressWarnings("unchecked")
	private static <T> T bare( Class<T> c ){
		try {
			Field u = Class.forName( "sun.misc.Unsafe" ).getDeclaredField( "theUnsafe" );
			u.setAccessible( true );
			Object unsafe = u.get( null );
			return (T) unsafe.getClass().getMethod( "allocateInstance", Class.class ).invoke( unsafe, c );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	@Test
	public void aGuestsHeaveTakesItsCellFromTheActionAndAsksTheHostNothing(){
		Hero guest = new Hero();
		guest.pos = HERO + W;
		guest.HP = guest.HT = 100;
		guest.sprite = QualitySourceTest.quietSprite();
		final Object[] cast = new Object[2];
		Javelin javelin = new Javelin(){
			@Override public void cast( Hero user, int dst ){ cast[0] = user; cast[1] = dst; }
		};
		typed( javelin, ItemType.GAMMA );
		javelin.quantity( 1 );
		javelin.level( 1 );
		javelin.identify( false );
		//the last of an upgraded set: the host's own hero would be asked first, on the host's screen
		javelin.damage( MissileWeapon.MAX_DURABILITY );
		assertTrue( javelin.collect( guest.belongings.backpack ) );
		GameScene.supplyNetTarget( HERO + 4 );
		try {
			javelin.execute( guest, MissileWeapon.AC_HEAVE );
		} finally {
			GameScene.supplyNetTarget( null );
		}
		assertSame( "thrown by the guest", guest, cast[0] );
		assertEquals( "at the cell the guest sent", HERO + 4, cast[1] );
		assertNotNull( guest.buff( WeaponPerk.Heave.class ) );
		assertNull( Dungeon.hero.buff( WeaponPerk.Heave.class ) );

		//no cell, no heave
		CellSelector.Listener heaver = (CellSelector.Listener) field( null, MissileWeapon.class, "heaver" );
		heaver.onSelect( null );
		assertNull( guest.buff( WeaponPerk.Heave.class ) );
	}

	// ---------------------------------------------------------------- Arc

	@Test
	public void aBetaBoomerangFliesThroughTwoAndWearsOnce(){
		Hero hero = Dungeon.hero;
		HeavyBoomerang stack = new HeavyBoomerang();
		typed( stack, ItemType.BETA );
		Char[] line = new Char[4];
		for (int i = 0; i < 4; i++) line[i] = dummy( HERO + 2 + i, Char.Alignment.ENEMY, 100000 );
		//an ally in the way is flown past, never struck
		Char friend = dummy( HERO + 1, Char.Alignment.ALLY, 1000 );
		int aim = HERO + 5;
		int landing = stack.throwPos( hero, aim );
		assertEquals( "comes down on the third", line[2].pos, landing );

		MissileWeapon thrown = thrownFrom( stack );
		float before = stack.durabilityLeft();
		setField( null, MissileWeapon.class, "arcAim", aim );
		call( thrown, xyz.gabriwar.warpedpixeldungeon.items.Item.class, "onThrow", new Class<?>[]{ int.class }, landing );
		for (int i = 0; i < 3; i++) assertTrue( "struck " + i, line[i].HP < 100000 );
		assertEquals( "the fourth is spared", 100000, line[3].HP );
		assertEquals( 1000, friend.HP );
		assertEquals( "one use for the whole throw", before - stack.durabilityPerUse(), stack.durabilityLeft(), 1e-3f );
		assertNotNull( "it circles back", hero.buff( HeavyBoomerang.CircleBack.class ) );
		assertFalse( hero.extraShotAttack );
	}

	@Test
	public void anArcAimedAtTheFrontOfALineCarriesOnThroughIt(){
		Hero hero = Dungeon.hero;
		HeavyBoomerang stack = new HeavyBoomerang();
		typed( stack, ItemType.BETA );
		Char[] line = new Char[4];
		for (int i = 0; i < 4; i++) line[i] = dummy( HERO + 2 + i, Char.Alignment.ENEMY, 100000 );
		int aim = line[0].pos;
		assertEquals( "through the front two, down on the third", line[2].pos, stack.throwPos( hero, aim ) );
		assertEquals( "on target for the quickslot's auto-aim", aim, stack.targetingPos( hero, aim ) );

		MissileWeapon thrown = thrownFrom( stack );
		setField( null, MissileWeapon.class, "arcAim", aim );
		onThrow( thrown, line[2].pos );
		for (int i = 0; i < 3; i++) assertTrue( "struck " + i, line[i].HP < 100000 );
		assertEquals( "the fourth is spared", 100000, line[3].HP );

		//a lone enemy: it comes down on it, not on the wall behind it
		Char lone = dummy( HERO - 3 * W, Char.Alignment.ENEMY, 100000 );
		HeavyBoomerang other = new HeavyBoomerang();
		typed( other, ItemType.ALPHA );
		assertEquals( lone.pos, other.throwPos( hero, lone.pos ) );
		assertEquals( lone.pos, other.targetingPos( hero, lone.pos ) );
		//aimed at the ground it still stops where it was aimed
		assertEquals( HERO - 2 * W, other.throwPos( hero, HERO - 2 * W ) );
	}

	@Test
	public void anArcThatStrikesOnTheWayAndLandsOnBareGroundIsAHit(){
		Hero hero = Dungeon.hero;
		HeavyBoomerang stack = new HeavyBoomerang();
		typed( stack, ItemType.BETA );
		Char a = dummy( HERO + 2, Char.Alignment.ENEMY, 100000 );
		Char b = dummy( HERO + 3, Char.Alignment.ENEMY, 100000 );
		int aim = HERO + 6;
		assertEquals( "aimed at the ground, it comes down there", aim, stack.throwPos( hero, aim ) );
		MissileWeapon thrown = thrownFrom( stack );
		float before = stack.durabilityLeft();
		setField( null, MissileWeapon.class, "arcAim", aim );
		onThrow( thrown, aim );
		assertTrue( a.HP < 100000 );
		assertTrue( b.HP < 100000 );
		assertEquals( "one use for the whole throw", before - stack.durabilityPerUse(), stack.durabilityLeft(), 1e-3f );
		assertNotNull( "it circles back", hero.buff( HeavyBoomerang.CircleBack.class ) );
		assertEquals( "and lies on no floor", 0, Dungeon.level.heaps.valueList().size() );
		assertFalse( hero.extraShotAttack );
	}

	@Test
	public void theReturnStrikesWhatIsOnItsWayHome(){
		Hero hero = Dungeon.hero;
		HeavyBoomerang boomerang = new HeavyBoomerang();
		Quality q = typed( boomerang, ItemType.BETA );
		Char onTheWay = dummy( HERO + 3, Char.Alignment.ENEMY, 100000 );
		Char friend = dummy( HERO + 2, Char.Alignment.ALLY, 1000 );
		Char behind = dummy( HERO + 6, Char.Alignment.ENEMY, 100000 );
		float durability = boomerang.durabilityLeft();
		call( null, HeavyBoomerang.class, "returnStrikes",
				new Class<?>[]{ MissileWeapon.class, Char.class, int.class, int.class }, boomerang, hero, HERO + 5, HERO );
		assertTrue( onTheWay.HP < 100000 );
		assertEquals( 1000, friend.HP );
		assertEquals( "where it lay is not on its way back", 100000, behind.HP );
		assertEquals( "the way back wears nothing", durability, boomerang.durabilityLeft(), 1e-5f );
		assertFalse( q.returnStrike );
		assertFalse( hero.extraShotAttack );

		//each strike is a share of a hit: a quarter a rank
		q.returnStrike = true;
		assertEquals( 50, WeaponPerk.BOOMERANG.onHit( q, boomerang, hero, onTheWay, 100 ) );
		q.returnStrike = false;
		assertEquals( 100, WeaponPerk.BOOMERANG.onHit( q, boomerang, hero, onTheWay, 100 ) );

		//an untyped one strikes nothing on the way back
		HeavyBoomerang plain = new HeavyBoomerang();
		int hp = onTheWay.HP;
		call( null, HeavyBoomerang.class, "returnStrikes",
				new Class<?>[]{ MissileWeapon.class, Char.class, int.class, int.class }, plain, hero, HERO + 5, HERO );
		assertEquals( hp, onTheWay.HP );
	}

	@Test
	public void anAlphaBoomerangComesBackInTwoTurns(){
		HeavyBoomerang boomerang = new HeavyBoomerang();
		Quality q = typed( boomerang, ItemType.BETA );
		HeavyBoomerang.CircleBack back = new HeavyBoomerang.CircleBack();
		back.setup( boomerang, HERO + 4, HERO, 1, 0 );
		assertEquals( 5, field( back, HeavyBoomerang.CircleBack.class, "left" ) );
		q.type = ItemType.ALPHA;
		back.setup( boomerang, HERO + 4, HERO, 1, 0 );
		assertEquals( 2, field( back, HeavyBoomerang.CircleBack.class, "left" ) );

		JupitersWraith wraith = new JupitersWraith();
		typed( wraith, ItemType.ALPHA );
		JupitersWraith.CircleBack wback = new JupitersWraith.CircleBack();
		wback.setup( wraith, HERO + 4, HERO, 1, 0 );
		assertEquals( 2, field( wback, JupitersWraith.CircleBack.class, "left" ) );
		wback.setup( new JupitersWraith(), HERO + 4, HERO, 1, 0 );
		assertEquals( 5, field( wback, JupitersWraith.CircleBack.class, "left" ) );
	}

	// ---------------------------------------------------------------- Reaper

	@Test
	public void anAlphaScytheSweepsTheEnemiesBesideIt(){
		Hero hero = Dungeon.hero;
		Sickle sickle = new Sickle();
		typed( sickle, ItemType.ALPHA );
		hero.belongings.weapon = sickle;
		Char target = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
		Char up = dummy( HERO - W, Char.Alignment.ENEMY, 1000 );
		Char down = dummy( HERO + W, Char.Alignment.ENEMY, 1000 );
		Char friend = dummy( HERO - 1, Char.Alignment.ALLY, 1000 );
		Char twoAway = dummy( HERO + 2 * W, Char.Alignment.ENEMY, 1000 );
		assertEquals( 100, Quality.weaponHit( sickle, hero, target, 100 ) );
		assertEquals( 955, up.HP );
		assertEquals( 955, down.HP );
		assertEquals( 1000, friend.HP );
		assertEquals( 1000, twoAway.HP );
		assertEquals( 100000, target.HP );
		assertNotNull( "swept enemies bleed", up.buff( Bleeding.class ) );
		assertNull( friend.buff( Bleeding.class ) );

		//below alpha the sweep is smaller and nobody bleeds
		Sickle beta = new Sickle();
		typed( beta, ItemType.BETA );
		assertEquals( 100, Quality.weaponHit( beta, hero, target, 100 ) );
		assertEquals( 925, down.HP );
	}

	@Test
	public void aCarriedScytheGivesASecondSeedAtItsRate(){
		Hero hero = Dungeon.hero;
		assertEquals( 0f, Quality.seedChance( hero ), 1e-5f );
		//nothing drawn without a typed scythe
		long expected = firstLong( 0x5EEDL );
		Random.pushGenerator( 0x5EEDL );
		try {
			assertFalse( Quality.secondSeed( hero, HERO ) );
			//held, so nothing rolls it a type as the pack would
			hero.belongings.weapon = new Sickle();
			assertFalse( Quality.secondSeed( hero, HERO ) );
			assertEquals( expected, Random.Long() );
		} finally {
			Random.popGenerator();
		}

		WarScythe gamma = new WarScythe();
		typed( gamma, ItemType.GAMMA );
		assertTrue( gamma.collect( hero.belongings.backpack ) );
		assertEquals( 0.20f, Quality.seedChance( hero ), 1e-5f );
		//the best one carried counts, held or in the pack
		Sickle alpha = new Sickle();
		typed( alpha, ItemType.ALPHA );
		hero.belongings.weapon = alpha;
		assertEquals( 0.60f, Quality.seedChance( hero ), 1e-5f );

		int doubled = 0, cuts = 10000;
		Random.pushGenerator( 0x5EED2L );
		try {
			for (int i = 0; i < cuts; i++) if (Quality.secondSeed( hero, HERO )) doubled++;
		} finally {
			Random.popGenerator();
		}
		assertEquals( 0.60f, doubled / (float) cuts, 0.03f );
	}

	@Test
	public void aReapersScytheDoublesTheSeedOfACutCrop(){
		Hero hero = Dungeon.hero;
		Sickle alpha = new Sickle();
		typed( alpha, ItemType.ALPHA );
		hero.belongings.weapon = alpha;
		int cell = HERO + 2, doubled = 0, n = 300;
		Random.pushGenerator( 0xC0A5L );
		try {
			for (int i = 0; i < n; i++){
				int got = harvest( hero, cell, Firebloom.Seed.class, Firebloom.Seed.class );
				assertTrue( got == 1 || got == 2 );
				if (got == 2) doubled++;
			}
			//a food crop gives no seed, so nothing to double
			for (int i = 0; i < 30; i++){
				int got = harvest( hero, cell, Cornwheat.Seed.class, Cornwheatshaft.class );
				assertTrue( got >= 2 && got <= 3 );
			}
		} finally {
			Random.popGenerator();
		}
		assertEquals( 0.60f, doubled / (float) n, 0.10f );

		//an untyped sickle cuts one seed, and draws nothing for a second
		hero.belongings.weapon = new Sickle();
		long expected = firstLong( 0xC0A6L );
		Random.pushGenerator( 0xC0A6L );
		try {
			for (int i = 0; i < 20; i++) assertEquals( 1, harvest( hero, cell, Firebloom.Seed.class, Firebloom.Seed.class ) );
			assertEquals( expected, Random.Long() );
		} finally {
			Random.popGenerator();
		}
	}

	//a ripe crop of `sown` on `cell` cut by the hero (Farming.harvest): how many of `yield` it gave
	private static int harvest( Hero hero, int cell, Class<? extends Plant.Seed> sown, Class<? extends Item> yield ){
		Dungeon.level.heaps.clear();
		FarmCrop m = new FarmCrop();
		m.pos = cell;
		m.crop = new Crop( sown, 0, 0 );
		Farming.harvest( hero, m );
		Heap h = Dungeon.level.heaps.get( cell );
		assertNotNull( "the yield is left on the crop's cell", h );
		assertEquals( yield, h.peek().getClass() );
		return h.peek().quantity();
	}

	@Test
	public void aReapersCutDoublesTheSeedTheGrassGivesUp(){
		Hero hero = Dungeon.hero;
		Sickle alpha = new Sickle();
		typed( alpha, ItemType.ALPHA );
		hero.belongings.weapon = alpha;
		int cell = HERO + 2, seeds = 0, doubled = 0;
		for (long s = 1; s < 50000 && seeds < 40; s++){
			Item walked = grassSeed( cell, s, null );
			if (walked == null) continue;
			seeds++;
			//the same cut, by a reaper: the same seed, maybe two of it
			Item reaped = grassSeed( cell, s, hero );
			assertNotNull( reaped );
			if (reaped.quantity() == 2 * walked.quantity()) doubled++;
			else assertEquals( walked.quantity(), reaped.quantity() );
		}
		assertEquals( 40, seeds );
		assertTrue( "a reaper doubles some", doubled > 0 );
		assertTrue( "but not all", doubled < seeds );
	}

	//the seed tall grass on `cell` gives up under the dice `s`, cut by `reaper` or walked
	//through (null); null when it gives none
	private static Item grassSeed( int cell, long s, Hero reaper ){
		Level.set( cell, Terrain.HIGH_GRASS, Dungeon.level );
		Dungeon.level.heaps.clear();
		HighGrass.reaper = reaper;
		Random.pushGenerator( s );
		try {
			HighGrass.trample( Dungeon.level, cell );
		} finally {
			Random.popGenerator();
			HighGrass.reaper = null;
		}
		Heap h = Dungeon.level.heaps.get( cell );
		if (h == null) return null;
		for (Item i : h.items) if (Arrays.asList( Generator.Category.SEED.classes ).contains( i.getClass() )) return i;
		return null;
	}

	@Test
	public void aWarScytheAloneHarvestsAndReaps(){
		Hero hero = Dungeon.hero;
		assertFalse( Farming.hasSickle( hero ) );
		WarScythe scythe = new WarScythe();
		hero.belongings.weapon = scythe;
		assertTrue( "held", Farming.hasSickle( hero ) );
		hero.belongings.weapon = null;
		assertTrue( scythe.collect( hero.belongings.backpack ) );
		assertTrue( "in the pack", Farming.hasSickle( hero ) );
		hero.belongings.backpack.items.remove( scythe );

		int depth = Dungeon.depth, branch = Dungeon.branch;
		DayNightCycle.Phase phase = DayNightCycle.debugPhaseOverride;
		Random.pushGenerator( 4242 );
		try {
			Dungeon.depth = OverworldLevel.DEPTH;
			Dungeon.branch = 0;
			DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
			HomesteadWorld.reset();
			OverworldLevel ow = forestWindow();
			int grass = -1, stand = -1;
			for (int c = ow.width() * 4 + 4; c < ow.length() - ow.width() * 4 && grass < 0; c++){
				if (ow.protectedForBuilding( c ) || !ow.passable[c] || Actor.findChar( c ) != null) continue;
				for (int n : PathFinder.NEIGHBOURS4){
					int s = c + n;
					if (ow.passable[s] && !ow.pit[s] && !ow.protectedForBuilding( s ) && Actor.findChar( s ) == null
							&& (Terrain.flags[ow.map[s]] & Terrain.LIQUID) == 0){
						grass = c;
						stand = s;
						break;
					}
				}
			}
			assertNotEquals( "a cell to grow grass on", -1, grass );
			Level.set( grass, Terrain.HIGH_GRASS, ow );
			hero.pos = stand;
			//no sprite: each stroke lands at once, as in GatheringTest
			hero.sprite = null;
			hero.fieldOfView = new boolean[ow.length()];
			Arrays.fill( hero.fieldOfView, true );
			if (!Actor.all().contains( hero )) Actor.add( hero );

			assertEquals( Messages.get( Gathering.class, "no_sickle" ), Gathering.refusal( hero, grass, Gathering.Job.REAP ) );
			scythe.identify( false );
			assertTrue( scythe.collect( hero.belongings.backpack ) );
			assertNull( Gathering.refusal( hero, grass, Gathering.Job.REAP ) );
			assertTrue( scythe.actions( hero ).contains( Sickle.AC_REAP ) );
			assertEquals( "REAP", scythe.actionName( Sickle.AC_REAP, hero ) );

			final boolean[] done = { false };
			int strokes = 0;
			while (!done[0]){
				assertTrue( Gathering.stroke( hero, grass, Gathering.Job.REAP, d -> done[0] = d ) );
				assertTrue( "a job ends", ++strokes < 20 );
			}
			assertEquals( Terrain.GRASS, ow.map[grass] );
			assertNull( "the reaper is only set while the grass is cut", HighGrass.reaper );
		} finally {
			Random.popGenerator();
			Dungeon.depth = depth;
			Dungeon.branch = branch;
			DayNightCycle.debugPhaseOverride = phase;
			HomesteadWorld.reset();
		}
	}

	//a live surface window of the forest near the origin, as GatheringTest builds one
	private static OverworldLevel forestWindow(){
		long seed = 0x5EED0F7EA7L;
		int w = WindowGenerator.WIDTH, h = WindowGenerator.HEIGHT;
		for (int r = 0; r <= 40; r++){
			for (int j = -r; j <= r; j++){
				for (int i = -r; i <= r; i++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int x = i * 64, y = j * 64;
					if (WorldModel.biomeAt( seed, x, y ) != WorldModel.Biome.FOREST || WorldStructures.townCell( x, y ) != -1
							|| OverworldFauna.nearSettlement( seed, x, y )) continue;
					PathFinder.setMapSize( w, h );
					float s = WorldModel.calendarShift();
					int ox = x - w / 2, oy = y - h / 2;
					WindowGenerator.Window win = WindowGenerator.generate( seed, 0, ox, oy, s );
					OverworldLevel ow = OverworldLevel.forNetwork( 0, seed, ox, oy, s, GameCalendar.season(), win.terrain, w, h );
					assertNotNull( ow );
					setField( ow, OverworldLevel.class, "network", false );
					Dungeon.level = ow;
					return ow;
				}
			}
		}
		throw new AssertionError( "no forest near the origin" );
	}

	// ---------------------------------------------------------------- the four new types

	private static final ItemType[] TYPES = { ItemType.GAMMA, ItemType.BETA, ItemType.ALPHA };

	private static void setNow( float now ){
		setField( null, Actor.class, "now", now );
	}

	//the push a knock-back or a drag queued, if any
	private static Pushing pushing(){
		for (Actor a : Actor.all()) if (a instanceof Pushing) return (Pushing) a;
		return null;
	}

	private static int pushedTo( Pushing p ){
		return (Integer) field( p, Pushing.class, "to" );
	}

	//an enemy that only keeps count of its health, with these properties
	private static Char foe( int pos, int hp, Char.Property... props ){
		Char ch = new Char(){
			{ for (Char.Property p : props) properties.add( p ); }
			@Override public void damage( int amount, Object source ){ HP -= amount; }
		};
		ch.pos = pos;
		ch.HP = ch.HT = hp;
		ch.alignment = Char.Alignment.ENEMY;
		ch.sprite = QualitySourceTest.quietSprite();
		Actor.add( ch );
		return ch;
	}

	@Test
	public void woodsmanSplintersDeeperOnOneEnemy(){
		for (ItemType t : TYPES){
			Hero hero = QualitySourceTest.arena();
			HandAxe axe = new HandAxe();
			Quality q = typed( axe, t );
			hero.belongings.weapon = axe;
			int r = t.rank;
			Char a = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
			//the armor roll comes before the hook: the first hit pierces nothing, each after one stack more
			float[] expected = { 0f, 0.06f * r, 0.12f * r, 0.18f * r, 0.18f * r };
			for (int i = 0; i < expected.length; i++){
				assertEquals( t + " hit " + (i + 1), expected[i], WeaponPerk.WOODSMAN.pierce( q, axe, hero, a ), 1e-5f );
				assertEquals( t + " hit " + (i + 1), Math.min( Quality.PIERCE_CAP, expected[i] ), Quality.drPierce( hero, a ), 1e-5f );
				assertEquals( "no damage of its own", 100, WeaponPerk.WOODSMAN.onHit( q, axe, hero, a, 100 ) );
			}
			//another enemy starts over, and the first one's splinters are let go
			Char b = dummy( HERO - 1, Char.Alignment.ENEMY, 100000 );
			assertEquals( 0f, WeaponPerk.WOODSMAN.pierce( q, axe, hero, b ), 1e-5f );
			WeaponPerk.WOODSMAN.onHit( q, axe, hero, b, 100 );
			assertEquals( 0.06f * r, WeaponPerk.WOODSMAN.pierce( q, axe, hero, b ), 1e-5f );
			assertEquals( 0f, WeaponPerk.WOODSMAN.pierce( q, axe, hero, a ), 1e-5f );
			//and they fade 3 + r turns after the last hit
			setNow( Actor.now() + 3f + r - 0.5f );
			assertEquals( 0.06f * r, WeaponPerk.WOODSMAN.pierce( q, axe, hero, b ), 1e-5f );
			setNow( Actor.now() + 0.5f );
			assertEquals( 0f, WeaponPerk.WOODSMAN.pierce( q, axe, hero, b ), 1e-5f );
			WeaponPerk.WOODSMAN.onHit( q, axe, hero, b, 100 );
			assertEquals( "a faded enemy starts at one", 0.06f * r, WeaponPerk.WOODSMAN.pierce( q, axe, hero, b ), 1e-5f );
		}
	}

	@Test
	public void aFellingBlowLandsEveryThirdHitAndDrawsNothing(){
		Hero hero = Dungeon.hero;
		HandAxe axe = new HandAxe();
		Quality q = typed( axe, ItemType.ALPHA );
		Char a = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
		long expected = firstLong( 0xFE11L );
		Random.pushGenerator( 0xFE11L );
		try {
			for (int i = 1; i <= 6; i++){
				WeaponPerk.WOODSMAN.onHit( q, axe, hero, a, 100 );
				Pushing p = pushing();
				if (i % 3 == 0){
					assertNotNull( "hit " + i + " fells", p );
					assertEquals( "two tiles straight back", HERO + 3, pushedTo( p ) );
					Actor.remove( p );
				} else {
					assertNull( "hit " + i, p );
				}
			}
			//a miss does not start the count over
			WeaponPerk.WOODSMAN.onHit( q, axe, hero, a, 100 );
			WeaponPerk.WOODSMAN.loseStacks( q );
			WeaponPerk.WOODSMAN.onHit( q, axe, hero, a, 100 );
			assertNull( pushing() );
			WeaponPerk.WOODSMAN.onHit( q, axe, hero, a, 100 );
			assertNotNull( pushing() );
			Actor.remove( pushing() );
			//another enemy does
			Char b = dummy( HERO - 1, Char.Alignment.ENEMY, 100000 );
			WeaponPerk.WOODSMAN.onHit( q, axe, hero, a, 100 );
			WeaponPerk.WOODSMAN.onHit( q, axe, hero, a, 100 );
			WeaponPerk.WOODSMAN.onHit( q, axe, hero, b, 100 );
			assertNull( pushing() );
			assertEquals( expected, Random.Long() );
		} finally {
			Random.popGenerator();
		}
		//below alpha there is no felling blow, and no count kept
		HandAxe beta = new HandAxe();
		Quality qb = typed( beta, ItemType.BETA );
		for (int i = 0; i < 6; i++) WeaponPerk.WOODSMAN.onHit( qb, beta, hero, a, 100 );
		assertNull( pushing() );
		assertEquals( 0, qb.hitCount );
	}

	@Test
	public void quarryDazesAtItsShareAndNeverABoss(){
		Hero hero = Dungeon.hero;
		Char a = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
		int n = 4000;
		for (ItemType t : TYPES){
			WarHammer hammer = new WarHammer();
			Quality q = typed( hammer, t );
			int dazed = 0;
			Random.pushGenerator( 0xDA2EL + t.rank );
			try {
				for (int i = 0; i < n; i++){
					assertEquals( 100, WeaponPerk.QUARRY.onHit( q, hammer, hero, a, 100 ) );
					Daze d = a.buff( Daze.class );
					if (d != null){
						dazed++;
						assertEquals( 2f, d.cooldown(), 1e-5f );
						d.detach();
					}
					Buff.detach( a, Cripple.class );
				}
			} finally {
				Random.popGenerator();
			}
			assertEquals( t.toString(), 0.10f * t.rank, dazed / (float) n, 0.02f );
		}
		//a boss is never dazed, and nothing is drawn for it
		WarHammer hammer = new WarHammer();
		Quality q = typed( hammer, ItemType.ALPHA );
		Char boss = foe( HERO - 1, 100000, Char.Property.BOSS );
		Char miniboss = foe( HERO + W, 100000, Char.Property.MINIBOSS );
		long expected = firstLong( 0xB055L );
		Random.pushGenerator( 0xB055L );
		try {
			for (int i = 0; i < 50; i++) WeaponPerk.QUARRY.onHit( q, hammer, hero, boss, 100 );
			assertEquals( expected, Random.Long() );
		} finally {
			Random.popGenerator();
		}
		assertNull( boss.buff( Daze.class ) );
		//a miniboss can be
		boolean dazedOnce = false;
		for (int i = 0; i < 200 && !dazedOnce; i++){
			WeaponPerk.QUARRY.onHit( q, hammer, hero, miniboss, 100 );
			dazedOnce = miniboss.buff( Daze.class ) != null;
		}
		assertTrue( dazedOnce );
	}

	@Test
	public void aCrackCripplesOnlyEnemiesBeside(){
		Hero hero = Dungeon.hero;
		WarHammer hammer = new WarHammer();
		Quality q = typed( hammer, ItemType.ALPHA );
		int t = HERO + 4;
		Char target = dummy( t, Char.Alignment.ENEMY, 100000 );
		Char foe = dummy( t + 1, Char.Alignment.ENEMY, 1000 );
		Char foe2 = dummy( t - W - 1, Char.Alignment.ENEMY, 1000 );
		Char friend = dummy( t + W, Char.Alignment.ALLY, 1000 );
		Char far = dummy( t + 2, Char.Alignment.ENEMY, 1000 );
		Random.pushGenerator( 0xC4AC4L );
		try {
			for (int i = 0; i < 200 && target.buff( Daze.class ) == null; i++){
				WeaponPerk.QUARRY.onHit( q, hammer, hero, target, 100 );
				if (target.buff( Daze.class ) == null) assertNull( "no crack without a daze", target.buff( Cripple.class ) );
			}
		} finally {
			Random.popGenerator();
		}
		assertNotNull( target.buff( Daze.class ) );
		assertEquals( 3f, target.buff( Cripple.class ).cooldown(), 1e-5f );
		assertNotNull( foe.buff( Cripple.class ) );
		assertNotNull( foe2.buff( Cripple.class ) );
		assertNull( "the crack spares allies", friend.buff( Cripple.class ) );
		assertNull( "and the enemies further off", far.buff( Cripple.class ) );
		assertNull( "only the struck one is dazed", foe.buff( Daze.class ) );

		//below alpha, a daze cracks nothing
		Char other = dummy( HERO - 4, Char.Alignment.ENEMY, 100000 );
		Char beside = dummy( HERO - 5, Char.Alignment.ENEMY, 1000 );
		q.type = ItemType.BETA;
		Random.pushGenerator( 0xC4AC5L );
		try {
			for (int i = 0; i < 200 && other.buff( Daze.class ) == null; i++) WeaponPerk.QUARRY.onHit( q, hammer, hero, other, 100 );
		} finally {
			Random.popGenerator();
		}
		assertNotNull( other.buff( Daze.class ) );
		assertNull( other.buff( Cripple.class ) );
		assertNull( beside.buff( Cripple.class ) );
	}

	//the hero among the characters, as in play, so a chord finds it
	private static Hero playing(){
		Hero hero = Dungeon.hero;
		if (!Actor.chars().contains( hero )) Actor.add( hero );
		return hero;
	}

	@Test
	public void aChordComesEveryFourthLandedHitAndAlternates(){
		Hero hero = playing();
		Flute flute = new Flute();
		Quality q = typed( flute, ItemType.GAMMA );
		Char target = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
		Char near = dummy( HERO + 2, Char.Alignment.ENEMY, 100000 );
		Char ally = dummy( HERO + 3 * W, Char.Alignment.ALLY, 1000 );
		Char farAlly = dummy( HERO + 4 * W, Char.Alignment.ALLY, 1000 );
		for (int hit = 1; hit <= 12; hit++){
			assertEquals( 100, WeaponPerk.BARD.onHit( q, flute, hero, target, 100 ) );
			//a miss neither counts nor starts the count over
			WeaponPerk.BARD.loseStacks( q );
			boolean rally = hit == 4 || hit == 12, dread = hit == 8;
			assertEquals( "hit " + hit, rally, hero.buff( Bless.class ) != null );
			assertEquals( "hit " + hit, rally, ally.buff( Bless.class ) != null );
			assertEquals( "hit " + hit, dread, target.buff( Terror.class ) != null );
			assertEquals( "hit " + hit, dread, near.buff( Terror.class ) != null );
			if (rally) assertEquals( "1 + r turns", 2f, hero.buff( Bless.class ).cooldown(), 1e-5f );
			if (dread) assertEquals( 2f, near.buff( Terror.class ).cooldown(), 1e-5f );
			assertNull( "four tiles off is out of earshot", farAlly.buff( Bless.class ) );
			Buff.detach( hero, Bless.class );
			Buff.detach( ally, Bless.class );
			Buff.detach( target, Terror.class );
			Buff.detach( near, Terror.class );
		}
		assertNull( "no charm below alpha", target.buff( Charm.class ) );
	}

	@Test
	public void theDreadSparesBossesAndTheStruckOneKeepsItsTerror(){
		Hero hero = playing();
		Flute flute = new Flute();
		Quality q = typed( flute, ItemType.BETA );
		Char target = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
		Char near = dummy( HERO + 2 * W, Char.Alignment.ENEMY, 100000 );
		Char boss = foe( HERO - 1, 100000, Char.Property.BOSS );
		Char miniboss = foe( HERO - W, 100000, Char.Property.MINIBOSS );
		Char threeOff = dummy( HERO + 3, Char.Alignment.ENEMY, 100000 );
		Char friend = dummy( HERO + W, Char.Alignment.ALLY, 1000 );
		q.notes = 3;
		q.dreadNext = true;
		WeaponPerk.BARD.onHit( q, flute, hero, target, 100 );
		assertFalse( "the next chord is a rally", q.dreadNext );
		Terror struck = target.buff( Terror.class );
		assertNotNull( struck );
		assertEquals( hero.id(), struck.object );
		assertEquals( 3f, struck.cooldown(), 1e-5f );
		assertTrue( struck.ignoreNextHit );
		Terror other = near.buff( Terror.class );
		assertNotNull( other );
		assertFalse( other.ignoreNextHit );
		assertNull( boss.buff( Terror.class ) );
		assertNull( miniboss.buff( Terror.class ) );
		assertNull( "two tiles at most", threeOff.buff( Terror.class ) );
		assertNull( friend.buff( Terror.class ) );
		assertNull( "a dread blesses nobody", hero.buff( Bless.class ) );
		//the blow that played the chord does not break the struck one's terror
		struck.recover();
		assertNotNull( target.buff( Terror.class ) );
	}

	@Test
	public void anAlphaChordCharmsTheWeakestNonBoss(){
		Hero hero = playing();
		Flute flute = new Flute();
		Quality q = typed( flute, ItemType.ALPHA );
		Char target = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
		Char boss = foe( HERO - 1, 1, Char.Property.BOSS );
		Char weak = dummy( HERO + 3 * W, Char.Alignment.ENEMY, 5 );
		Char weakToo = dummy( HERO - 3 * W, Char.Alignment.ENEMY, 5 );
		Char weakest = dummy( HERO + 4, Char.Alignment.ENEMY, 2 );
		Char friend = dummy( HERO + W, Char.Alignment.ALLY, 1 );
		q.notes = 3;
		WeaponPerk.BARD.onHit( q, flute, hero, target, 100 );
		assertNull( "four tiles off", weakest.buff( Charm.class ) );
		assertNull( boss.buff( Charm.class ) );
		assertNull( friend.buff( Charm.class ) );
		assertNull( "a tie goes to the first", weakToo.buff( Charm.class ) );
		Charm c = weak.buff( Charm.class );
		assertNotNull( c );
		assertEquals( hero.id(), c.object );
		assertEquals( 5f, c.cooldown(), 1e-5f );
		assertFalse( c.ignoreNextHit );
		assertNotNull( "the rally still plays", hero.buff( Bless.class ) );
		assertEquals( 4f, hero.buff( Bless.class ).cooldown(), 1e-5f );

		//the next chord is a dread: the weakest left is charmed, and not terrified
		Buff.detach( weak, Charm.class );
		weak.HP = 50;
		target.HP = 3;
		q.notes = 3;
		WeaponPerk.BARD.onHit( q, flute, hero, target, 100 );
		Charm onTarget = target.buff( Charm.class );
		assertNotNull( onTarget );
		assertTrue( "the struck one keeps its charm through the blow", onTarget.ignoreNextHit );
		assertNull( target.buff( Terror.class ) );
		assertNull( "three tiles off is out of the dread", weak.buff( Terror.class ) );
	}

	@Test
	public void aBardDrawsNoRandom() throws Exception {
		Hero hero = playing();
		//a charm's hearts are the first thing here to reach the co-op layer, whose class setup draws
		//once as it loads (WindParticle's): loaded already in any game, so loaded here first
		Class.forName( "xyz.gabriwar.warpedpixeldungeon.net.NetVisuals" );
		Flute flute = new Flute();
		Quality q = typed( flute, ItemType.ALPHA );
		Char target = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
		dummy( HERO + 2, Char.Alignment.ENEMY, 40 );
		dummy( HERO + W, Char.Alignment.ALLY, 1000 );
		long expected = firstLong( 0xBA4DL );
		Random.pushGenerator( 0xBA4DL );
		try {
			for (int i = 0; i < 16; i++) WeaponPerk.BARD.onHit( q, flute, hero, target, 100 );
			assertEquals( expected, Random.Long() );
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void aLashDragsOnlyFromTwoTilesNeverABossNeverIntoAPit(){
		Hero hero = Dungeon.hero;
		int n = 3000;
		for (ItemType t : new ItemType[]{ ItemType.GAMMA, ItemType.BETA }){
			Whip whip = new Whip();
			Quality q = typed( whip, t );
			Char target = dummy( HERO + 2, Char.Alignment.ENEMY, 100000 );
			int dragged = 0;
			Random.pushGenerator( 0x1A5L + t.rank );
			try {
				for (int i = 0; i < n; i++){
					assertEquals( "the crack of the lash", Math.round( 100 * (1f + 0.06f * t.rank) ),
							WeaponPerk.LASH.onHit( q, whip, hero, target, 100 ) );
					Pushing p = pushing();
					if (p != null){
						dragged++;
						assertEquals( "a tile toward the wielder", HERO + 1, pushedTo( p ) );
						assertEquals( 2f, target.buff( Cripple.class ).cooldown(), 1e-5f );
						Actor.remove( p );
						Buff.detach( target, Cripple.class );
					} else {
						assertNull( target.buff( Cripple.class ) );
					}
				}
			} finally {
				Random.popGenerator();
			}
			assertEquals( t.toString(), 0.15f * t.rank, dragged / (float) n, 0.02f );
			Actor.remove( target );
		}

		//every gate fails before the draw: no pull, nothing drawn, and the blow plain beside you
		Whip whip = new Whip();
		Quality q = typed( whip, ItemType.BETA );
		Char adjacent = dummy( HERO + W, Char.Alignment.ENEMY, 100000 );
		Char boss = foe( HERO - 2, 100000, Char.Property.BOSS );
		Char heavy = foe( HERO - 2 * W, 100000, Char.Property.IMMOVABLE );
		Char rooted = dummy( HERO + 2 * W + 2, Char.Alignment.ENEMY, 100000 );
		rooted.rooted = true;
		//over a chasm, even one the floor's flags would let it be pulled onto: only the pit gate stands
		Char overPit = dummy( HERO + 2, Char.Alignment.ENEMY, 100000 );
		Level.set( HERO + 1, Terrain.CHASM, Dungeon.level );
		Dungeon.level.passable[HERO + 1] = true;
		//behind someone
		Char behind = dummy( HERO - 2 * W - 2, Char.Alignment.ENEMY, 100000 );
		dummy( HERO - W - 1, Char.Alignment.ALLY, 1000 );
		long expected = firstLong( 0x6A7EL );
		Random.pushGenerator( 0x6A7EL );
		try {
			assertEquals( 100, WeaponPerk.LASH.onHit( q, whip, hero, adjacent, 100 ) );
			for (Char ch : new Char[]{ boss, heavy, rooted, overPit, behind }){
				for (int i = 0; i < 20; i++) WeaponPerk.LASH.onHit( q, whip, hero, ch, 100 );
			}
			assertEquals( expected, Random.Long() );
		} finally {
			Random.popGenerator();
		}
		assertNull( pushing() );
	}

	@Test
	public void anAlphaLashDisarmsOneHitInFiveButNeverABoss(){
		Hero hero = Dungeon.hero;
		Whip whip = new Whip();
		Quality q = typed( whip, ItemType.ALPHA );
		//beside the wielder: no drag to draw for, only the disarm
		Char target = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
		int disarmed = 0, n = 4000;
		Random.pushGenerator( 0xD15AL );
		try {
			for (int i = 0; i < n; i++){
				assertEquals( 100, WeaponPerk.LASH.onHit( q, whip, hero, target, 100 ) );
				Disarmed d = target.buff( Disarmed.class );
				if (d != null){
					disarmed++;
					assertEquals( Disarmed.DURATION, d.cooldown(), 1e-5f );
					d.detach();
				}
			}
		} finally {
			Random.popGenerator();
		}
		assertEquals( 0.20f, disarmed / (float) n, 0.02f );

		Char boss = foe( HERO - 1, 100000, Char.Property.BOSS );
		long expected = firstLong( 0xD15BL );
		Random.pushGenerator( 0xD15BL );
		try {
			for (int i = 0; i < 50; i++) WeaponPerk.LASH.onHit( q, whip, hero, boss, 100 );
			assertEquals( expected, Random.Long() );
		} finally {
			Random.popGenerator();
		}
		assertNull( boss.buff( Disarmed.class ) );
	}

	// ---------------------------------------------------------------- the review of the others

	@Test
	public void momentumCountsTheFirstHit(){
		Hero hero = Dungeon.hero;
		Longsword sword = new Longsword();
		Quality q = typed( sword, ItemType.GAMMA );
		Char a = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
		int[] expected = { 105, 110, 115, 120, 120 };
		for (int i = 0; i < expected.length; i++){
			assertEquals( "hit " + (i + 1), expected[i], WeaponPerk.MOMENTUM.onHit( q, sword, hero, a, 100 ) );
		}
		//a miss starts it over, and so does another enemy
		WeaponPerk.MOMENTUM.loseStacks( q );
		assertEquals( 105, WeaponPerk.MOMENTUM.onHit( q, sword, hero, a, 100 ) );
		assertEquals( 110, WeaponPerk.MOMENTUM.onHit( q, sword, hero, a, 100 ) );
		Char b = dummy( HERO - 1, Char.Alignment.ENEMY, 100000 );
		assertEquals( 105, WeaponPerk.MOMENTUM.onHit( q, sword, hero, b, 100 ) );
		assertEquals( "the line prints the same numbers", "20%", WeaponPerk.MOMENTUM.amount( sword, ItemType.GAMMA ) );
		assertEquals( "5%", WeaponPerk.MOMENTUM.extra( sword, ItemType.GAMMA ) );
		assertEquals( "60%", WeaponPerk.MOMENTUM.amount( sword, ItemType.ALPHA ) );
	}

	@Test
	public void momentumCarriesItsStacksAfterAKill(){
		Hero hero = Dungeon.hero;
		Longsword sword = new Longsword();
		Quality q = typed( sword, ItemType.ALPHA );
		Char a = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
		Char b = dummy( HERO - 1, Char.Alignment.ENEMY, 100000 );
		assertEquals( 115, WeaponPerk.MOMENTUM.onHit( q, sword, hero, a, 100 ) );
		assertEquals( 130, WeaponPerk.MOMENTUM.onHit( q, sword, hero, a, 100 ) );
		assertEquals( 145, WeaponPerk.MOMENTUM.onHit( q, sword, hero, a, 100 ) );
		WeaponPerk.MOMENTUM.onKill( q, hero );
		assertTrue( "the kill's time is refunded", q.freeNext );
		//the old carry reset the stacks on the next enemy: they ride on to it now
		assertEquals( 160, WeaponPerk.MOMENTUM.onHit( q, sword, hero, b, 100 ) );
		assertEquals( 160, WeaponPerk.MOMENTUM.onHit( q, sword, hero, b, 100 ) );
		assertEquals( "and stay with it", 115, WeaponPerk.MOMENTUM.onHit( q, sword, hero, a, 100 ) );

		//a kill at two stacks carries nothing
		WeaponPerk.MOMENTUM.onHit( q, sword, hero, a, 100 );
		q.freeNext = false;
		WeaponPerk.MOMENTUM.onKill( q, hero );
		assertFalse( q.freeNext );
		assertEquals( 115, WeaponPerk.MOMENTUM.onHit( q, sword, hero, b, 100 ) );

		//and below alpha there is no carry at all
		Longsword beta = new Longsword();
		Quality qb = typed( beta, ItemType.BETA );
		for (int i = 0; i < 4; i++) WeaponPerk.MOMENTUM.onHit( qb, beta, hero, a, 100 );
		WeaponPerk.MOMENTUM.onKill( qb, hero );
		assertFalse( qb.freeNext );
		assertEquals( 110, WeaponPerk.MOMENTUM.onHit( qb, beta, hero, b, 100 ) );
	}

	@Test
	public void reachPaysFromTwoTiles(){
		Hero hero = Dungeon.hero;
		for (ItemType t : TYPES){
			Glaive glaive = new Glaive();
			Quality q = typed( glaive, t );
			int bonus = Math.round( 100 * (1f + 0.10f * t.rank) );
			assertEquals( 100, WeaponPerk.REACH.onHit( q, glaive, hero, dummy( HERO + 1, Char.Alignment.ENEMY, 1000 ), 100 ) );
			assertEquals( bonus, WeaponPerk.REACH.onHit( q, glaive, hero, dummy( HERO + 2, Char.Alignment.ENEMY, 1000 ), 100 ) );
			assertEquals( "flat past two tiles", bonus,
					WeaponPerk.REACH.onHit( q, glaive, hero, dummy( HERO + 3 * W + 3, Char.Alignment.ENEMY, 1000 ), 100 ) );
			assertEquals( (bonus - 100) + "%", WeaponPerk.REACH.amount( glaive, t ) );
		}
		//an instrument keeps its length but no longer the reach group's extra tile
		Flute flute = new Flute();
		typed( flute, ItemType.ALPHA );
		assertEquals( 0, Quality.reachBonus( flute, Dungeon.hero ) );
		Glaive glaive = new Glaive();
		typed( glaive, ItemType.ALPHA );
		assertEquals( 1, Quality.reachBonus( glaive, Dungeon.hero ) );
	}

	@Test
	public void marksmanFullAtFiveTiles(){
		Hero hero = Dungeon.hero;
		for (ItemType t : TYPES){
			Bow bow = new Bow();
			Quality q = typed( bow, t );
			int r = t.rank;
			int[] dist = { 2, 3, 4, 5, 6 };
			float[] share = { 0f, 0.09f * r, 0.12f * r, 0.15f * r, 0.15f * r };
			for (int i = 0; i < dist.length; i++){
				Char target = dummy( HERO + dist[i], Char.Alignment.ENEMY, 1000 );
				assertEquals( t + " at " + dist[i], Math.round( 100 * (1f + share[i]) ),
						WeaponPerk.SHOT.onHit( q, bow, hero, target, 100 ) );
				Actor.remove( target );
			}
			assertEquals( Math.round( 15f * r ) + "%", WeaponPerk.SHOT.amount( bow, t ) );
		}
	}

	@Test
	public void guardBaseRaised(){
		RoundShield shield = new RoundShield();
		Quality q = typed( shield, ItemType.GAMMA );
		assertEquals( 0.035f + 0.004f * 3, WeaponPerk.GUARD.guardFraction( q, shield ), 1e-5f );
		assertEquals( "5%", WeaponPerk.GUARD.amount( shield, ItemType.GAMMA ) );
		q.type = ItemType.BETA;
		assertEquals( 0.07f + 0.004f * 3, WeaponPerk.GUARD.guardFraction( q, shield ), 1e-5f );
		assertEquals( "8%", WeaponPerk.GUARD.amount( shield, ItemType.BETA ) );
		//still capped at 15%
		Greatshield big = new Greatshield();
		Quality qb = typed( big, ItemType.ALPHA );
		big.level( 10 );
		assertEquals( 0.15f, WeaponPerk.GUARD.guardFraction( qb, big ), 1e-5f );
		assertEquals( "15%", WeaponPerk.GUARD.amount( big, ItemType.ALPHA ) );
	}

	@Test
	public void ofNeverReadsTheHero(){
		Generator.Category[] cats = {
				Generator.Category.WEP_T1, Generator.Category.WEP_T2, Generator.Category.WEP_T3,
				Generator.Category.WEP_T4, Generator.Category.WEP_T5,
				Generator.Category.MIS_T1, Generator.Category.MIS_T2, Generator.Category.MIS_T3,
				Generator.Category.MIS_T4, Generator.Category.MIS_T5 };
		Hero hero = Dungeon.hero;
		for (Generator.Category cat : cats){
			for (Class<?> c : cat.classes){
				Item item = (Item) Reflection.newInstance( c );
				Dungeon.hero = hero;
				WeaponPerk with = WeaponPerk.of( item );
				Dungeon.hero = null;
				assertEquals( c.getSimpleName(), with, WeaponPerk.of( item ) );
			}
		}
		Dungeon.hero = hero;
	}

	@Test
	public void theReviewedLinesReadTrue(){
		assertEquals( "_Momentum:_ each hit in a row on the same enemy deals _10%_ more damage, from the first hit, "
				+ "stacking four times for _40%_ in all. A miss starts it over.",
				WeaponPerk.MOMENTUM.describe( new Longsword(), ItemType.BETA ) );
		assertEquals( "_Reach:_ hits from _two or more tiles away_ deal _20%_ more damage.",
				WeaponPerk.REACH.describe( new Glaive(), ItemType.BETA ) );
		assertTrue( WeaponPerk.DART.describe( new Dart(), ItemType.BETA ).contains( "keeps its tip on _30%_ of hits" ) );
		assertEquals( "_Woodsman:_ every hit on the same enemy _splinters its armor_, ignoring _18%_ more of it, three times over. "
				+ "Felling and chopping take _60%_ fewer strokes, and _45%_ of felled trees give an extra log. "
				+ "Every third hit on the same enemy is a _felling blow_ that knocks it back two tiles.",
				WeaponPerk.WOODSMAN.describe( new HandAxe(), ItemType.ALPHA ) );
		assertEquals( "_Bard:_ every hit plays a note, and every _fourth note_ a chord, in turn: a _rally_ that blesses you "
				+ "and your allies within 3 tiles for _2 turns_, then a _dread_ that terrifies the enemies within 2 tiles for as long.",
				WeaponPerk.BARD.describe( new Flute(), ItemType.GAMMA ) );
		assertEquals( "_Lash:_ hits from two or more tiles away deal _12%_ more damage, and _30%_ of them _drag_ the enemy "
				+ "a tile toward you, crippled for 2 turns.",
				WeaponPerk.LASH.describe( new Whip(), ItemType.BETA ) );
		assertTrue( WeaponPerk.QUARRY.describe( new WarHammer(), ItemType.GAMMA ).startsWith( "_Quarry:_ _10%_ of hits _daze_" ) );
	}

	// ---------------------------------------------------------------- the crit

	//a sprite that writes down every word raised over it, and is on screen or not
	private static CharSprite listening( ArrayList<String> said, boolean visible ){
		CharSprite s = new CharSprite(){
			@Override public Emitter emitter(){ return new Emitter(); }
			@Override public Emitter centerEmitter(){ return new Emitter(); }
			@Override public void operate( int cell ){}
			@Override public void showStatus( int color, String text, Object... args ){ said.add( text ); }
			@Override public void showStatusWithIcon( int color, String text, int icon, Object... args ){ said.add( text ); }
		};
		s.visible = visible;
		return s;
	}

	@Test
	public void aCritRaisesNoWord(){
		Hero hero = Dungeon.hero;
		ArrayList<String> said = new ArrayList<>();
		Char target = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
		target.sprite = listening( said, false );
		//a gamma flail: its wild blows open no bleed, whose name would be a word of its own
		Flail flail = new Flail();
		Quality q = typed( flail, ItemType.GAMMA );
		WeaponPerk.crit( q, hero, target );
		assertTrue( q.critBlow );
		//a wild blow and a CRIT line's crit, through the blow as it is dealt
		Random.pushGenerator( 0xC417L );
		try {
			boolean critted = false;
			for (int i = 0; i < 200 && !critted; i++){
				Quality.weaponHit( flail, hero, target, 100 );
				critted = q.critBlow;
			}
			assertTrue( critted );
			Longsword sword = new Longsword();
			Quality qs = only( sword, Rarity.LEGENDARY, 1f, RarityLine.CRIT );
			qs.type = ItemType.GAMMA;
			critted = false;
			for (int i = 0; i < 400 && !critted; i++){
				Quality.weaponHit( sword, hero, target, 100 );
				critted = qs.critBlow;
			}
			assertTrue( critted );
		} finally {
			Random.popGenerator();
		}
		assertTrue( "no word over the struck: " + said, said.isEmpty() );
		assertTrue( "the word's key is gone", Messages.get( WeaponPerk.class, "crit" ).contains( Messages.NO_TEXT_FOUND ) );
	}

	@Test
	public void everyBurstStillDrawsWhatItsSparksDid(){
		//the sparks the perks used to burst drew the game's Random once as they started, whatever their number
		new Emitter().burst( Speck.factory( Speck.STAR ), 4 );
		assertEquals( "an old burst", firstFloatThenLong( 0x5A1L ), drawsOf( 0x5A1L, () -> new Emitter().burst( Speck.factory( Speck.STAR ), 4 ) ) );

		Hero hero = Dungeon.hero;
		hero.sprite.visible = true;
		ArrayList<String> said = new ArrayList<>();
		for (boolean seen : new boolean[]{ true, false }){
			long expected = seen ? firstFloatThenLong( 0x5A2L ) : firstLong( 0x5A2L );
			String how = seen ? " on screen" : " off screen";
			Bow bow = new Bow();
			Quality qShot = typed( bow, ItemType.GAMMA );
			Char far = dummy( HERO + 4, Char.Alignment.ENEMY, 100000 );
			far.sprite = listening( said, seen );
			assertEquals( "a long shot" + how, expected, drawsOf( 0x5A2L, () -> WeaponPerk.SHOT.onHit( qShot, bow, hero, far, 100 ) ) );
			Glaive glaive = new Glaive();
			Quality qReach = typed( glaive, ItemType.GAMMA );
			Char two = dummy( HERO + 2 * W, Char.Alignment.ENEMY, 100000 );
			two.sprite = listening( said, seen );
			assertEquals( "a reach blow" + how, expected, drawsOf( 0x5A2L, () -> WeaponPerk.REACH.onHit( qReach, glaive, hero, two, 100 ) ) );
			Cudgel cudgel = new Cudgel();
			Quality qCrush = typed( cudgel, ItemType.ALPHA );
			Char beside = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
			beside.sprite = listening( said, seen );
			assertEquals( "a sunder" + how, expected, drawsOf( 0x5A2L, () -> WeaponPerk.CRUSH.onHit( qCrush, cudgel, hero, beside, 100 ) ) );
			Longsword sword = new Longsword();
			Quality qMom = typed( sword, ItemType.GAMMA );
			for (int i = 0; i < 4; i++) WeaponPerk.MOMENTUM.onHit( qMom, sword, hero, beside, 100 );
			assertEquals( "a full momentum" + how, expected, drawsOf( 0x5A2L, () -> WeaponPerk.MOMENTUM.onHit( qMom, sword, hero, beside, 100 ) ) );
			Flail flail = new Flail();
			Quality qWild = typed( flail, ItemType.GAMMA );
			assertEquals( "a crit" + how, expected, drawsOf( 0x5A2L, () -> WeaponPerk.crit( qWild, hero, beside ) ) );
			Actor.remove( far );
			Actor.remove( two );
			Actor.remove( beside );
		}
		//the flurry's free hit bursts over the wielder
		Gloves gloves = new Gloves();
		Quality qSwift = typed( gloves, ItemType.ALPHA );
		Char foe = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
		for (int i = 0; i < 4; i++) WeaponPerk.SWIFT.onHit( qSwift, gloves, hero, foe, 100 );
		assertEquals( "a free flurry", firstFloatThenLong( 0x5A3L ), drawsOf( 0x5A3L, () -> WeaponPerk.SWIFT.onHit( qSwift, gloves, hero, foe, 100 ) ) );
		assertTrue( qSwift.freeNext );
		hero.sprite.visible = false;
		assertTrue( said.isEmpty() );
	}

	//the first Long the game's Random gives under `seed` after `run`
	private static long drawsOf( long seed, Runnable run ){
		Random.pushGenerator( seed );
		try {
			run.run();
			return Random.Long();
		} finally {
			Random.popGenerator();
		}
	}

	private static long firstFloatThenLong( long seed ){
		return drawsOf( seed, Random::Float );
	}

	// ---------------------------------------------------------------- Disarmed

	//a character that never misses and always rolls the same blow
	private static Char brute( int pos ){
		Char ch = new Char(){
			@Override public int attackSkill( Char target ){ return Char.INFINITE_ACCURACY; }
			@Override public int damageRoll(){ return 1000; }
		};
		ch.pos = pos;
		ch.HP = ch.HT = 1000;
		ch.alignment = Char.Alignment.ENEMY;
		ch.sprite = QualitySourceTest.quietSprite();
		Actor.add( ch );
		return ch;
	}

	//what one blow of `attacker` takes off a fresh punching bag beside it
	private static int blow( Char attacker ){
		Char bag = dummy( attacker.pos + 1, Char.Alignment.ALLY, 1000000 );
		assertTrue( attacker.attack( bag ) );
		Actor.remove( bag );
		return 1000000 - bag.HP;
	}

	@Test
	public void aDisarmHalvesOnlyTheNextLandedBlow(){
		Char brute = brute( HERO + 2 * W );
		int plain = blow( brute );
		assertTrue( plain > 0 );
		Buff.prolong( brute, Disarmed.class, Disarmed.DURATION );
		assertEquals( Math.round( plain * 0.5f ), blow( brute ), 1 );
		assertNull( "spent on the blow", brute.buff( Disarmed.class ) );
		assertEquals( "the one after is whole", plain, blow( brute ) );
	}

	@Test
	public void disarmedSurvivesAMissAndASave(){
		Char brute = brute( HERO + 2 * W );
		Buff.prolong( brute, Disarmed.class, Disarmed.DURATION );
		Char ghost = dodger( brute.pos + 1 );
		assertFalse( brute.attack( ghost ) );
		Actor.remove( ghost );
		assertNotNull( "a miss leaves it", brute.buff( Disarmed.class ) );

		Bundle bundle = new Bundle();
		brute.storeInBundle( bundle );
		Char loaded = brute( HERO + 4 * W );
		loaded.restoreFromBundle( bundle );
		Disarmed back = loaded.buff( Disarmed.class );
		assertNotNull( "kept by a save", back );
		assertEquals( Disarmed.DURATION, back.cooldown(), 1e-3f );
		assertFalse( Messages.get( back, "desc", "5" ).contains( Messages.NO_TEXT_FOUND ) );
		assertFalse( back.name().contains( Messages.NO_TEXT_FOUND ) );
	}

	// ---------------------------------------------------------------- the rival statue

	@Test
	public void theRivalFightsAsAGunnerOrAReaper(){
		Hero hero = Dungeon.hero;
		hero.HP = hero.HT = 1000;
		RivalStatue rival = new RivalStatue();
		rival.pos = HERO + 1;
		rival.HP = rival.HT = 1000;
		setField( rival, Statue.class, "weapon", new WarScythe() );
		setField( rival, RivalStatue.class, "perk", WeaponPerk.GUN );
		assertTrue( rival.marksman() );
		setField( rival, RivalStatue.class, "perk", WeaponPerk.SHOT );
		assertTrue( rival.marksman() );
		setField( rival, RivalStatue.class, "perk", WeaponPerk.SCYTHE );
		assertFalse( rival.marksman() );

		//the sweep: every hero-aligned character beside it but the one it struck, 45% of the blow
		Char friend = dummy( HERO + 1 + W, Char.Alignment.ALLY, 1000 );
		Char foe = dummy( HERO + 1 - W, Char.Alignment.ENEMY, 1000 );
		rival.attackProc( hero, 100 );
		assertEquals( 955, friend.HP );
		assertEquals( 1000, foe.HP );
		assertEquals( 1000, hero.HP );
		//no shout: the sweep shows as its own look, the swept numbers on their cards
		assertTrue( Messages.get( RivalStatue.class, "sweep" ).contains( Messages.NO_TEXT_FOUND ) );
	}

	//a rival of `perk` at `pos`, armed with a plain longsword so only its perk tells in the blow
	private static RivalStatue rival( WeaponPerk perk, int pos ){
		RivalStatue rival = new RivalStatue();
		rival.pos = pos;
		rival.HP = rival.HT = 1000;
		rival.sprite = QualitySourceTest.quietSprite();
		setField( rival, Statue.class, "weapon", new Longsword() );
		setField( rival, RivalStatue.class, "perk", perk );
		Actor.add( rival );
		return rival;
	}

	@Test
	public void theRivalFightsAsAWoodsmanAQuarryABardAndALash(){
		Hero hero = Dungeon.hero;
		hero.HP = hero.HT = 100000;
		//on an ally of the hero's: blows in a row, and nothing drawn for any of it
		Char friend = dummy( HERO + 3 * W, Char.Alignment.ALLY, 100000 );
		for (WeaponPerk p : new WeaponPerk[]{ WeaponPerk.WOODSMAN, WeaponPerk.QUARRY, WeaponPerk.BARD, WeaponPerk.LASH }){
			RivalStatue r = rival( p, friend.pos + 1 );
			long expected = firstLong( 0x217AL );
			Random.pushGenerator( 0x217AL );
			try {
				for (int i = 0; i < 12; i++) r.attackProc( friend, 100 );
				assertEquals( p.toString(), expected, Random.Long() );
			} finally {
				Random.popGenerator();
			}
			Pushing push;
			while ((push = pushing()) != null) Actor.remove( push );
			Actor.remove( r );
		}

		//Woodsman: every third blow in a row throws the hero two tiles back (a push of the hero
		//pans the camera after it: a bare one stands in)
		Camera savedCam = Camera.main;
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		try {
			RivalStatue woods = rival( WeaponPerk.WOODSMAN, HERO + 1 );
			for (int i = 1; i <= 6; i++){
				assertEquals( 100, woods.attackProc( hero, 100 ) );
				Pushing p = pushing();
				assertEquals( "blow " + i, i % 3 == 0, p != null );
				if (p != null){
					assertEquals( HERO - 2, pushedTo( p ) );
					Actor.remove( p );
				}
			}
			Actor.remove( woods );
		} finally {
			Camera.main = savedCam;
		}

		//Quarry: every third, dazed and crippled
		RivalStatue quarry = rival( WeaponPerk.QUARRY, HERO + 1 );
		for (int i = 1; i <= 3; i++){
			quarry.attackProc( hero, 100 );
			assertEquals( "blow " + i, i == 3, hero.buff( Daze.class ) != null );
		}
		assertEquals( 3f, hero.buff( Cripple.class ).cooldown(), 1e-5f );
		Buff.detach( hero, Daze.class );
		Buff.detach( hero, Cripple.class );
		Actor.remove( quarry );

		//Bard: every fourth, a blessing for itself
		RivalStatue bard = rival( WeaponPerk.BARD, HERO + 1 );
		for (int i = 1; i <= 4; i++){
			bard.attackProc( hero, 100 );
			assertEquals( "blow " + i, i == 4, bard.buff( Bless.class ) != null );
		}
		assertNull( hero.buff( Bless.class ) );
		Actor.remove( bard );

		//Lash: harder from two tiles, and every fourth disarms; a miss starts the count over
		RivalStatue lash = rival( WeaponPerk.LASH, HERO + 2 );
		assertEquals( 118, lash.attackProc( hero, 100 ) );
		assertEquals( 118, lash.attackProc( hero, 100 ) );
		assertEquals( 118, lash.attackProc( hero, 100 ) );
		assertNull( hero.buff( Disarmed.class ) );
		lash.attackProc( hero, 100 );
		assertNotNull( hero.buff( Disarmed.class ) );
		Actor.remove( lash );
		RivalStatue close = rival( WeaponPerk.LASH, HERO + 1 );
		assertEquals( "plain beside", 100, close.attackProc( hero, 100 ) );
	}

	@Test
	public void theRivalArmsItselfAgainstTheNewGroups(){
		Hero hero = Dungeon.hero;
		Random.pushGenerator( 0xA2A1L );
		try {
			for (Supplier<Item> held : Arrays.<Supplier<Item>>asList( HandAxe::new, WarHammer::new, Flute::new, Whip::new )){
				Item w = held.get();
				hero.belongings.weapon = (xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon) w;
				RivalStatue rival = new RivalStatue();
				rival.armAgainst( hero );
				WeaponPerk mine = WeaponPerk.of( w );
				if (mine == WeaponPerk.LASH){
					//the whip is the only one the generator makes: any long weapon may stand in
					assertTrue( rival.perk().toString(), rival.perk() == WeaponPerk.LASH || rival.perk() == WeaponPerk.REACH );
				} else if (mine == WeaponPerk.QUARRY){
					//on the first floors the generator makes no hammer or pick: any crushing weapon may stand in
					assertTrue( rival.perk().toString(), rival.perk() == WeaponPerk.QUARRY || rival.perk() == WeaponPerk.CRUSH );
				} else {
					assertEquals( w.getClass().getSimpleName(), mine, rival.perk() );
				}
			}
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void theRivalMirrorsTheReviewedNumbers(){
		Hero hero = Dungeon.hero;
		hero.HP = hero.HT = 100000;
		RivalStatue mom = rival( WeaponPerk.MOMENTUM, HERO + 1 );
		int[] expected = { 115, 130, 145, 160, 160 };
		for (int i = 0; i < expected.length; i++) assertEquals( "blow " + (i + 1), expected[i], mom.attackProc( hero, 100 ) );
		Actor.remove( mom );
		RivalStatue reach = rival( WeaponPerk.REACH, HERO + 2 );
		assertEquals( 130, reach.attackProc( hero, 100 ) );
		Actor.remove( reach );
		RivalStatue reachClose = rival( WeaponPerk.REACH, HERO + 1 );
		assertEquals( 100, reachClose.attackProc( hero, 100 ) );
		Actor.remove( reachClose );
		RivalStatue shot = rival( WeaponPerk.SHOT, HERO + 5 );
		assertEquals( "full at five tiles", 145, shot.attackProc( hero, 100 ) );
		Actor.remove( shot );
		RivalStatue near = rival( WeaponPerk.SHOT, HERO + 3 );
		assertEquals( 127, near.attackProc( hero, 100 ) );
	}

	private static long firstLong( long seed ){
		Random.pushGenerator( seed );
		try {
			return Random.Long();
		} finally {
			Random.popGenerator();
		}
	}
}
