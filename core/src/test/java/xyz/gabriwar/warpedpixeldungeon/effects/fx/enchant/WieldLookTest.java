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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ArmoredStatue;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Sentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.WeaponLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.modules.EnchantArea;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.SentinelSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.StatueSprite;
import com.watabou.noosa.Game;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * What a wielder carries (WieldLook): its caps (two motes alive a look, six a sprite); the core's
 * 0.25 s poll showing an enchantment changed on the weapon in hand, an armour's glyphs, the echo and
 * Kinetic's charge with no hook of their own; none of it drawing the game's Random; only the hero
 * and the three nearest armed mobs shown; the hero's emissive weapon's night light, out while he is
 * invisible.
 */
public class WieldLookTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private final EnchantArea area = new EnchantArea();
	private final List<CharSprite> drawn = new ArrayList<>();

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		WieldLook.reset();
		WeaponLight.out( 0f );
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = EnchantFxTest.at( 2, 2 );
		drawn.clear();
	}

	@After
	public void tearDown(){
		WeaponLight.out( 0f );
		WieldLook.reset();
		Dungeon.hero = null;
		rig.down();
	}

	private static void set( Object o, Class<?> c, String field, Object v ) throws Exception {
		Field f = c.getDeclaredField( field );
		f.setAccessible( true );
		f.set( o, v );
	}

	//a statue's sprite at a cell, holding a weapon (and, armoured, wearing an armour)
	private CharSprite statue( int cell, Weapon w, Armor a ) throws Exception {
		Statue s = a != null ? new ArmoredStatue() : new Statue();
		set( s, Statue.class, "weapon", w );
		if (a != null) set( s, ArmoredStatue.class, "armor", a );
		s.pos = cell;
		s.HP = s.HT = 100;
		CharSprite sp = new StatueSprite();
		sp.ch = s;
		s.sprite = sp;
		rig.stage.add( sp );
		sp.place( cell );
		sp.visible = true;
		drawn.add( sp );
		return sp;
	}

	private CharSprite hero( Weapon w ){
		Hero h = Dungeon.hero;
		h.belongings.weapon = w;
		//a plain sprite for him (a mob's would read him as a mob)
		CharSprite sp = new CharSprite(){};
		sp.ch = h;
		h.sprite = sp;
		rig.stage.add( sp );
		sp.place( h.pos );
		sp.visible = true;
		drawn.add( sp );
		return sp;
	}

	private static Sword sword( Weapon.Enchantment... es ){
		Sword w = new Sword();
		for (Weapon.Enchantment e : es) w.addEnchant( e );
		return w;
	}

	//n frames: each drawn sprite's afterDraw, then the stage and the layers
	private void frames( int n ){
		for (int i = 0; i < n; i++){
			Game.timeTotal += Game.elapsed;
			for (int k = 0; k < drawn.size(); k++) area.afterDraw( drawn.get( k ) );
			rig.frames( 1, false );
		}
	}

	@Test
	public void itKeepsToItsCaps() throws Exception {
		Sword w = ItemAuraTest.sword( new Blazing(), new Chilling(), new Grim() );
		ClothArmor a = new ClothArmor();
		a.inscribe( new Thorns() );
		a.addGlyph( new Brimstone() );
		CharSprite s = statue( EnchantFxTest.at( 4, 3 ), w, a );
		int most = 0;
		for (int f = 0; f < 900; f++){
			frames( 1 );
			EnchantState st = EnchantState.get( s );
			float now = Game.timeTotal;
			int live = WieldLook.live( st, now );
			most = Math.max( most, live );
			assertTrue( "alive " + live, live <= WieldLook.PER_SPRITE );
			for (int ch = 0; ch < 5; ch++){
				int n = 0;
				for (int k = 0; k < 2; k++) if (now - st.wieldAt[ch * 2 + k] < st.wieldLife[ch * 2 + k]) n++;
				assertTrue( n <= WieldLook.PER_LOOK );
			}
		}
		assertTrue( "it shed: " + most, most >= 3 );
		EnchantState st = EnchantState.get( s );
		assertEquals( 2, st.weaponCount );
		assertEquals( 2, st.armorCount );
		assertEquals( EnchantLooks.of( Grim.class ).code, st.echoCode );
	}

	@Test
	public void thePollShowsAChangeWithNoHook() throws Exception {
		Sword w = sword( new Blazing() );
		CharSprite s = statue( EnchantFxTest.at( 4, 3 ), w, null );
		frames( 3 );
		EnchantState st = EnchantState.get( s );
		assertEquals( EnchantLooks.of( Blazing.class ).code, st.weaponCodes[0] );
		w.enchant( new Chilling() );
		frames( (int)Math.ceil( EnchantState.POLL_EVERY / Game.elapsed ) + 1 );
		assertEquals( EnchantLooks.of( Chilling.class ).code, st.weaponCodes[0] );
		assertEquals( 1, st.weaponCount );

		//Kinetic's charge, read in the same poll
		Sword k = sword( new Kinetic() );
		CharSprite ks = statue( EnchantFxTest.at( 5, 3 ), k, null );
		frames( 10 );
		assertEquals( 0, EnchantState.get( ks ).kineticBin );
		Buff.affect( ks.ch, Kinetic.ConservedDamage.class ).setBonus( Math.round( ks.ch.HT * 0.35f ) );
		frames( 10 );
		assertEquals( 2, EnchantState.get( ks ).kineticBin );
	}

	@Test
	public void aSentinelCarriesItsWeaponAsAStatueDoes() throws Exception {
		Sword w = sword( new Blazing() );
		Sentinel m = new Sentinel();
		set( m, Sentinel.class, "weapon", w );
		m.pos = EnchantFxTest.at( 4, 3 );
		CharSprite s = new SentinelSprite();
		s.ch = m;
		m.sprite = s;
		rig.stage.add( s );
		s.place( m.pos );
		s.visible = true;
		drawn.add( s );
		assertSame( w, EnchantFx.attacking( m ) );
		frames( 3 );
		EnchantState st = EnchantState.get( s );
		assertEquals( 1, st.weaponCount );
		assertEquals( EnchantLooks.of( Blazing.class ).code, st.weaponCodes[0] );
		frames( (int)Math.ceil( WieldLook.WEAPON_EVERY / Game.elapsed ) + 2 );
		assertTrue( "its weapon sheds", shed( s ) );
	}

	@Test
	public void itDrawsNoneOfTheGamesRandom() throws Exception {
		Sword w = sword( new Blazing(), new Chilling() );
		ClothArmor a = new ClothArmor();
		a.inscribe( new Thorns() );
		statue( EnchantFxTest.at( 4, 3 ), w, a );
		hero( sword( new Blazing() ) );
		frames( 2 );
		assertEquals( 0, EnchantFxTest.draws( () -> frames( 600 ) ) );
	}

	@Test
	public void onlyTheHeroAndTheThreeNearestMobs() throws Exception {
		CharSprite h = hero( sword( new Blazing() ) );
		List<CharSprite> statues = new ArrayList<>();
		for (int i = 0; i < 5; i++) statues.add( statue( EnchantFxTest.at( 3 + 2 * i, 2 ), sword( new Blazing() ), null ) );
		frames( 300 );
		assertTrue( "the hero shed", shed( h ) );
		int shown = 0;
		for (CharSprite s : statues) if (shed( s )) shown++;
		assertEquals( WieldLook.NEAREST, shown );
		for (int i = 0; i < WieldLook.NEAREST; i++) assertTrue( "the nearest " + i, shed( statues.get( i ) ) );
	}

	private static boolean shed( CharSprite s ){
		EnchantState st = EnchantState.get( s );
		if (st == null) return false;
		for (float t : st.wieldAt) if (t > 0f) return true;
		return false;
	}

	@Test
	public void theHerosWeaponLightsTheNight() throws Exception {
		CharSprite h = hero( sword( new Blazing() ) );
		frames( 3 );
		assertTrue( "a blazing sword's light", WeaponLight.held() );
		Dungeon.hero.invisible = 1;
		frames( 2 );
		assertFalse( "out while he is invisible", WeaponLight.held() );
		Dungeon.hero.invisible = 0;
		frames( 2 );
		assertTrue( WeaponLight.held() );
		//a frost sword casts none (frost is not light)
		Dungeon.hero.belongings.weapon = sword( new Chilling() );
		frames( (int)Math.ceil( EnchantState.POLL_EVERY / Game.elapsed ) + 2 );
		assertFalse( WeaponLight.held() );
		assertTrue( h.visible );
	}
}
