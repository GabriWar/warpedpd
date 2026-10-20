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

package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantNet;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantState;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.ItemLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.modules.EnchantArea;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingKnife;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.StatueSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.utils.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * What co-op guests are told of an enchanted item's looks (ItemLooks.sig): a heap's optional "g"
 * and an item's optional "sig" go out and come back to the same looks on the guest's stand-ins; the
 * heaps' hash moves when a heap is enchanted (or its curse found) in place; a missile's "sig" trails
 * the guest's stand-in; what a statue carries goes out as WIELD and fills the guest's sprite; a
 * net hero's inventory goes out again when his weapon is enchanted; and a payload of an older host,
 * with none of these fields (or a sig without its rarity), still parses and shows plain items.
 */
public class EnchantSigNetTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private EnchantFxTest.Rig rig;

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		NetVisuals.clear();
	}

	@After
	public void tearDown() throws Exception {
		TestParty.leave();
		NetVisuals.setSpectatorMobs( null );
		NetVisuals.clear();
		Dungeon.hero = null;
		rig.down();
	}

	private static Object call( Class<?> c, String name, Class<?>[] types, Object... args ) throws Exception {
		Method m = c.getDeclaredMethod( name, types );
		m.setAccessible( true );
		return m.invoke( null, args );
	}

	private static Sword exotic( Weapon.Enchantment a, Weapon.Enchantment b, Weapon.Enchantment echo ){
		Sword w = new Sword();
		w.quality = null;
		Random.pushGenerator( 5 );
		try {
			Quality.roll( w, Rarity.EXOTIC, null, 0 );
		} finally {
			Random.popGenerator();
		}
		w.enchant( a );
		if (b != null) w.addEnchant( b );
		Quality q = Quality.of( w );
		int at = q.lines.indexOf( RarityLine.VESTIGE );
		if (at >= 0){
			q.lines.remove( at );
			q.rolls.remove( at );
		}
		q.vestige = echo;
		if (echo != null){
			q.lines.add( 0, RarityLine.VESTIGE );
			q.rolls.add( 0, 1f );
		}
		w.identify();
		return w;
	}

	private static void sameLooks( Item host, Item guest ){
		ItemLooks.Set h = ItemLooks.of( host ), g = ItemLooks.of( guest );
		assertSame( h.a, g.a );
		assertSame( h.b, g.b );
		assertSame( h.echo, g.echo );
		assertEquals( h.level, g.level );
	}

	@Test
	public void aHeapsLooksAndAnItemsGoOutAndComeBack() throws Exception {
		Level level = Dungeon.level;
		Sword sword = exotic( new Blazing(), new Chilling(), new Grim() );
		Heap heap = new Heap();
		heap.pos = EnchantFxTest.at( 5, 5 );
		heap.drop( sword );
		level.heaps.put( heap.pos, heap );

		JSONArray heaps = (JSONArray) call( StateSerializer.class, "serializeHeaps", new Class<?>[]{ Level.class }, level );
		JSONObject h = heaps.getJSONObject( 0 );
		JSONArray g = h.getJSONArray( "g" );
		int[] sig = ItemLooks.sig( sword );
		assertNotNull( sig );
		assertEquals( sig.length, g.length() );
		for (int i = 0; i < sig.length; i++) assertEquals( sig[i], g.getInt( i ) );

		//the guest's level: its heap's stand-in shows the same
		level.heaps.clear();
		call( SpectatorReceiver.class, "applyHeaps", new Class<?>[]{ Level.class, JSONArray.class }, level, heaps );
		Item standIn = level.heaps.get( heap.pos ).peek();
		sameLooks( sword, standIn );
		assertEquals( "its rarity, for the guest's legendary heaps", Rarity.EXOTIC, ItemLooks.signedRarity( standIn ) );

		//an item's "sig"
		JSONObject item = (JSONObject) call( StateSerializer.class, "serializeItem", new Class<?>[]{ Item.class }, sword );
		assertTrue( item.has( "sig" ) );
		Item back = (Item) call( SpectatorReceiver.class, "deserializeItem", new Class<?>[]{ JSONObject.class, boolean.class }, item, true );
		sameLooks( sword, back );
		//a plain one sends none
		JSONObject plain = (JSONObject) call( StateSerializer.class, "serializeItem", new Class<?>[]{ Item.class }, new Sword() );
		assertTrue( !plain.has( "sig" ) );
	}

	@Test
	public void theHeapsHashMovesWhenAHeapIsEnchantedInPlace() throws Exception {
		Level level = Dungeon.level;
		Sword sword = new Sword();
		Heap heap = new Heap();
		heap.pos = EnchantFxTest.at( 4, 4 );
		heap.drop( sword );
		level.heaps.put( heap.pos, heap );
		int before = (int) call( StateSerializer.class, "heapHash", new Class<?>[]{ Level.class }, level );
		sword.enchant( new Blazing() );
		int enchanted = (int) call( StateSerializer.class, "heapHash", new Class<?>[]{ Level.class }, level );
		assertNotEquals( before, enchanted );
		//a hidden curse found in place
		sword.enchant( new Annoying() );
		sword.cursedKnown = false;
		int hidden = (int) call( StateSerializer.class, "heapHash", new Class<?>[]{ Level.class }, level );
		sword.cursedKnown = true;
		int known = (int) call( StateSerializer.class, "heapHash", new Class<?>[]{ Level.class }, level );
		assertNotEquals( hidden, known );
	}

	@Test
	public void anOlderHostsPayloadStillParses() throws Exception {
		Level level = Dungeon.level;
		level.heaps.clear();
		JSONArray heaps = new JSONArray( "[{\"pos\":" + EnchantFxTest.at( 3, 3 ) + ",\"type\":0,\"sprite\":1}]" );
		call( SpectatorReceiver.class, "applyHeaps", new Class<?>[]{ Level.class, JSONArray.class }, level, heaps );
		Heap h = level.heaps.get( EnchantFxTest.at( 3, 3 ) );
		assertNotNull( h );
		assertNull( ItemLooks.of( h.peek() ).a );
		JSONObject item = new JSONObject( "{\"img\":1,\"name\":\"a sword\",\"qty\":1,\"lvl\":0,\"cursed\":false,\"cursedK\":false,\"id\":true}" );
		Item back = (Item) call( SpectatorReceiver.class, "deserializeItem", new Class<?>[]{ JSONObject.class, boolean.class }, item, false );
		assertNotNull( back );
		assertNull( ItemLooks.signed( back ) );
		assertNull( ItemLooks.of( back ).a );
		//an older host's sig, four long: its looks, and no rarity
		Item four = new Item();
		ItemLooks.sign( four, new int[]{ EnchantLooks.of( Blazing.class ).code, -1, -1, 0 } );
		assertSame( EnchantLooks.of( Blazing.class ), ItemLooks.of( four ).a );
		assertNull( ItemLooks.signedRarity( four ) );

		//a missile with no "sig": a plain stand-in, no trail of magic
		guestHero();
		TestParty.guest();
		JSONArray events = new JSONArray( "[{\"t\":\"ms\",\"fx\":40,\"fy\":40,\"tx\":120,\"ty\":40,\"img\":1,\"as\":0,\"a\":0}]" );
		NetVisuals.replay( events );
		MissileSprite ms = missile();
		assertNotNull( "the stand-in flies", ms );
		assertNull( ms.looks().a );
		//a WIELD too short plays nothing
		CharSprite s = new StatueSprite();
		EnchantNet.play( EnchantNet.WIELD, new int[]{ 1, 2, 3 }, null, id -> s );
		assertNull( EnchantState.get( s ) );
	}

	//the guest's own hero with a sprite on the stage (his missiles fly from its group)
	private void guestHero(){
		Hero h = new Hero();
		h.pos = EnchantFxTest.at( 2, 2 );
		Dungeon.hero = h;
		CharSprite sp = new CharSprite(){};
		sp.ch = h;
		h.sprite = sp;
		rig.stage.add( sp );
		sp.place( h.pos );
	}

	private MissileSprite missile(){
		for (Gizmo g : rig.stage.membersView()) if (g instanceof MissileSprite && g.alive) return (MissileSprite) g;
		return null;
	}

	@Test
	public void aMissilesSigTrailsTheGuestsStandIn() throws Exception {
		guestHero();
		TestParty.join( new Hero() );
		ThrowingKnife knife = new ThrowingKnife();
		knife.enchant( new Blazing() );
		MissileSprite host = new MissileSprite();
		rig.stage.add( host );
		host.reset( EnchantFxTest.at( 2, 6 ), EnchantFxTest.at( 12, 6 ), knife, null );
		JSONArray sent = NetVisuals.drainEvents();
		JSONObject ms = null;
		for (int i = 0; i < sent.length(); i++) if ("ms".equals( sent.getJSONObject( i ).getString( "t" ) )) ms = sent.getJSONObject( i );
		assertNotNull( ms );
		JSONArray sig = ms.getJSONArray( "sig" );
		assertEquals( EnchantLooks.of( Blazing.class ).code, sig.getInt( 0 ) );
		host.killAndErase();

		TestParty.guest();
		NetVisuals.replay( sent );
		MissileSprite standIn = missile();
		assertNotNull( standIn );
		assertSame( EnchantLooks.of( Blazing.class ), standIn.looks().a );
		rig.frames( 2, true );
		assertNotNull( "its trail", standIn.trail() );
		assertTrue( standIn.trail().isFull() );
	}

	@Test
	public void aNetHerosWeaponEnchantedSendsHisInventoryAgain() throws Exception {
		Hero h = new Hero();
		h.pos = EnchantFxTest.at( 3, 3 );
		Sword sword = new Sword();
		h.belongings.weapon = sword;
		String plain = (String) call( StateSerializer.class, "netHeroSig", new Class<?>[]{ Hero.class }, h );
		sword.enchant( new Blazing() );
		String enchanted = (String) call( StateSerializer.class, "netHeroSig", new Class<?>[]{ Hero.class }, h );
		assertNotEquals( plain, enchanted );
	}

	@Test
	public void whatAStatueCarriesGoesOutAsWield() throws Exception {
		TestParty.join( new Hero() );
		Statue st = new Statue();
		Field f = Statue.class.getDeclaredField( "weapon" );
		f.setAccessible( true );
		Sword sword = exotic( new Blazing(), null, new Grim() );
		f.set( st, sword );
		st.pos = EnchantFxTest.at( 6, 6 );
		CharSprite s = new StatueSprite();
		s.ch = st;
		st.sprite = s;
		rig.stage.add( s );
		s.place( st.pos );
		EnchantArea area = new EnchantArea();
		area.afterDraw( s );
		JSONArray sent = NetVisuals.drainEvents();
		int[] wield = null;
		for (int i = 0; i < sent.length(); i++){
			JSONObject e = sent.getJSONObject( i );
			if (!"eh".equals( e.getString( "t" ) ) || e.getInt( "k" ) != EnchantNet.WIELD) continue;
			JSONArray n = e.getJSONArray( "i" );
			wield = new int[n.length()];
			for (int k = 0; k < wield.length; k++) wield[k] = n.getInt( k );
		}
		assertNotNull( "WIELD went out", wield );
		int blazing = EnchantLooks.of( Blazing.class ).code, grim = EnchantLooks.of( Grim.class ).code;
		assertArrayEquals( new int[]{ st.id(), blazing, -1, sword.enchantment.level(), -1, -1, grim, 0 }, wield );
		//nothing new: nothing sent again before the resend
		Game.timeTotal += EnchantState.POLL_EVERY + 0.01f;
		area.afterDraw( s );
		assertNull( NetVisuals.drainEvents() );

		//the guest's sprite for it, filled by what came
		TestParty.guest();
		CharSprite g = new StatueSprite();
		Map<Integer, CharSprite> his = new HashMap<>();
		his.put( st.id(), g );
		EnchantNet.play( EnchantNet.WIELD, java.util.Arrays.copyOfRange( wield, 0, wield.length ), null, his::get );
		EnchantState gs = EnchantState.get( g );
		assertNotNull( gs );
		assertEquals( 1, gs.weaponCount );
		assertEquals( blazing, gs.weaponCodes[0] );
		assertEquals( grim, gs.echoCode );
		//his poll leaves what the host said
		gs.poll( new Statue() );
		assertEquals( blazing, gs.weaponCodes[0] );
	}
}
