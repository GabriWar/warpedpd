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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The slots' motes (ItemAura): no Random of either kind over a thousand frames; nothing allocated
 * once made; at most five quads a slot (seven in the info window) for every look, two looks and an
 * echo; two hundred slots held within the pool of 160 mote quads (an equipped one served first, the
 * periods stretched past 96); the same item viewed again re-keys nothing and makes nothing; a
 * destroyed slot frees its quads' GPU buffers; the rows that twist their motif draw their twist
 * (Paralysis's ring stopping dead, Vicious's bead splitting, Slashing's streak, Ancient's runes by
 * its kills, Midas's coin and falling gold, Polarized's backlight split red and blue).
 */
public class ItemAuraTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private EnchantFxTest.Rig rig;

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		ItemAura.resetPool();
	}

	@After
	public void tearDown(){
		ItemAura.resetPool();
		rig.down();
	}

	//its quality rolled exotic (the game's Random held apart)
	static void exotic( Item w ){
		w.quality = null;
		Random.pushGenerator( 3 );
		try {
			Quality.roll( w, Rarity.EXOTIC, null, 0 );
		} finally {
			Random.popGenerator();
		}
	}

	//an exotic, identified sword with an enchantment, maybe a second and maybe an echo
	static Sword sword( Weapon.Enchantment a, Weapon.Enchantment b, Weapon.Enchantment echo ){
		Sword w = new Sword();
		exotic( w );
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

	//an item of a look: a sword of a weapon's enchantment or curse, a cloth armour of a glyph
	static Item of( EnchantLook l ){
		Object e = Reflection.newInstance( l.of );
		if (e instanceof Weapon.Enchantment){
			Sword w = new Sword();
			w.enchant( (Weapon.Enchantment) e );
			w.cursedKnown = true;
			return w;
		}
		ClothArmor a = new ClothArmor();
		a.inscribe( (Armor.Glyph) e );
		a.cursedKnown = true;
		return a;
	}

	private ItemSprite slot( Item item, ItemSprite.Mode mode, boolean first ){
		ItemSprite s = new ItemSprite().mode( mode );
		s.view( item );
		s.auraGate( true, first );
		s.x = 20f * (rig.stage.length % 12);
		s.y = 20f * (rig.stage.length / 12);
		rig.stage.add( s );
		return s;
	}

	private List<ItemSprite> twelve(){
		List<ItemSprite> out = new ArrayList<>();
		out.add( slot( sword( new Blazing(), null, null ), ItemSprite.Mode.SLOT, false ) );
		out.add( slot( sword( new Blazing(), new Chilling(), new Shocking() ), ItemSprite.Mode.SLOT, true ) );
		out.add( slot( sword( new Vampiric(), new Grim(), null ), ItemSprite.Mode.PANEL, false ) );
		for (Class<?> c : new Class<?>[]{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Lucky.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Eldritch.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Projecting.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Unstable.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Displacing.class,
				xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns.class,
				xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Viscosity.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic.class }){
			out.add( slot( of( EnchantLooks.of( c ) ), ItemSprite.Mode.SLOT, false ) );
		}
		return out;
	}

	// ------------------------------------------------------------------ the previews' moments

	/** The item's looks as the preview dump plays them (EnchantPreviewDump's registry), today's and
	 *  now: slots and heaps round the defender's cell, the rats hidden. */
	static final class Previews {

		private Previews(){}

		//the view's top-left, world px (the dump's 5x5 cells, two either side of the defender)
		private static float vx( EnchantPreviewDump.Scene s ){
			return (s.d.ch.pos % EnchantFxTest.W - 2) * 16f;
		}

		private static float vy(){
			return EnchantPreviewDump.VIEW_Y * 16f;
		}

		private static void hide( EnchantPreviewDump.Scene s ){
			s.a.visible = false;
			s.d.visible = false;
		}

		//an item in a slot at (x, y): today's plain pulse and sparkle, or now its motes round it
		private static ItemSprite slot( EnchantPreviewDump.Scene s, Item item, float x, float y, boolean now ){
			com.watabou.noosa.Group stage = s.rig.stage;
			if (!now){
				ItemSprite.Glowing g = item.glowing();
				ItemSprite sp = new ItemSprite( item.image(), g == null ? null : new ItemSprite.Glowing( g.color, g.period ) );
				sp.x = x;
				sp.y = y;
				stage.add( sp );
				java.util.ArrayList<Integer> colors = new java.util.ArrayList<>();
				if (item instanceof Weapon){
					for (Weapon.Enchantment e : ((Weapon) item).enchantments()){
						for (int k = 0; k < Math.max( 1, e.level() * (e.level() + 1) ); k++) colors.add( e.glowing().color );
					}
				} else if (item instanceof Armor){
					for (Armor.Glyph e : ((Armor) item).glyphs()) colors.add( e.glowing().color );
				}
				if (!colors.isEmpty()){
					int[] c = new int[colors.size()];
					for (int k = 0; k < c.length; k++) c[k] = colors.get( k );
					com.watabou.noosa.particles.Emitter e = new com.watabou.noosa.particles.Emitter();
					stage.add( e );
					e.pos( sp );
					e.pour( xyz.gabriwar.warpedpixeldungeon.effects.particles.EquipmentSparkle.factory( c ), 4f / c.length );
				}
				return sp;
			}
			ItemSprite sp = new ItemSprite().mode( ItemSprite.Mode.SLOT );
			sp.view( item );
			sp.x = x;
			sp.y = y;
			sp.auraGate( true, true );
			//its motes made, then laid in the order they draw: the halo and backlight, it, the rest
			com.watabou.noosa.Group made = new com.watabou.noosa.Group();
			made.add( sp );
			sp.update();
			made.remove( sp );
			Image[] q = sp.aura().quadsInOrder();
			stage.add( q[0] );
			stage.add( q[1] );
			stage.add( sp );
			for (int k = 2; k < q.length; k++) stage.add( q[k] );
			return sp;
		}

		private static Item of( Class<?> c ){
			return ItemAuraTest.of( EnchantLooks.of( c ) );
		}

		/** A bag strip: twelve enchanted items in slots, four by three. */
		static void bag( EnchantPreviewDump.Scene s, boolean now ){
			hide( s );
			Class<?>[] kinds = { Blazing.class, Chilling.class, Shocking.class,
					xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Venomous.class, Vampiric.class, Grim.class,
					xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Lucky.class,
					xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blooming.class,
					xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Projecting.class,
					xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas.class,
					xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Eldritch.class,
					xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone.class };
			for (int i = 0; i < kinds.length; i++){
				slot( s, of( kinds[i] ), vx( s ) + 4f + (i % 4) * 18f, vy() + 8f + (i / 4) * 20f, now );
			}
		}

		/** Six enchanted heaps round the defender's cell. */
		static void heaps( EnchantPreviewDump.Scene s, boolean now ){
			hide( s );
			//each row its own first sights
			HeapLook.forgetSights();
			Class<?>[] kinds = { Blazing.class, Chilling.class, Shocking.class,
					xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Venomous.class,
					xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Holy.class, Grim.class };
			int cx = s.d.ch.pos % EnchantFxTest.W, cy = s.d.ch.pos / EnchantFxTest.W;
			for (int i = 0; i < kinds.length; i++){
				Item item = of( kinds[i] );
				xyz.gabriwar.warpedpixeldungeon.items.Heap h = new xyz.gabriwar.warpedpixeldungeon.items.Heap();
				h.pos = EnchantFxTest.at( cx - 1 + i % 3, cy - 1 + (i / 3) * 2 );
				h.drop( item );
				h.seen = true;
				ItemSprite sp = new ItemSprite();
				s.rig.stage.add( sp );
				sp.link( h );
				if (!now){
					//today: the heap's item tinted by a pulse of its glow, and nothing else
					ItemSprite.Glowing g = item.glowing();
					sp.view( item.image(), new ItemSprite.Glowing( g.color, g.period ) );
				}
			}
		}

		/** A blazing knife thrown across the view. */
		static void knife( EnchantPreviewDump.Scene s, boolean now ){
			hide( s );
			xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingKnife k = new xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingKnife();
			//set straight: a thrown weapon's enchanting tells a hero this stage has none
			k.enchantment = new Blazing();
			int x = s.d.ch.pos % EnchantFxTest.W, y = s.d.ch.pos / EnchantFxTest.W;
			xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite ms = new xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite();
			s.rig.stage.add( ms );
			ms.reset( EnchantFxTest.at( x - 3, y ), EnchantFxTest.at( x + 3, y ), k, null );
			if (!now){
				//today: the item and its pulse, no trail
				ms.mode( ItemSprite.Mode.ICON );
				ms.glow( new ItemSprite.Glowing( 0xFF4400 ) );
			}
		}

		/** Three swords of two enchantments each, the first starting its blend in view. */
		static void crossfade( EnchantPreviewDump.Scene s, boolean now ){
			hide( s );
			Sword a = sword( new Blazing(), new Chilling(), null );
			Sword b = sword( new Vampiric(), new Grim(), null );
			Sword c = sword( new Shocking(), new xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Holy(), null );
			//the clock set so the first sword's hold ends 0.3 s in
			float at = ItemLooks.HOLD - 0.3f - Profile.mod( Game.timeTotal + ItemLooks.phase( a ), 2 * (ItemLooks.HOLD + ItemLooks.BLEND) );
			Game.timeTotal += Profile.mod( at, 2 * (ItemLooks.HOLD + ItemLooks.BLEND) );
			slot( s, a, vx( s ) + 10f, vy() + 30f, now );
			slot( s, b, vx( s ) + 32f, vy() + 30f, now );
			slot( s, c, vx( s ) + 54f, vy() + 30f, now );
		}

		/** The rows that twist their motif, in slots: Paralysis, Vicious, Slashing, Ancient (nine kills:
		 *  three runes), Midas and Polarized; and a Midas heap below them, its coin hopping. */
		static void twists( EnchantPreviewDump.Scene s, boolean now ){
			hide( s );
			HeapLook.forgetSights();
			Sword ancient = new Sword();
			xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Ancient e = new xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Ancient();
			com.watabou.utils.Bundle b = new com.watabou.utils.Bundle();
			e.storeInBundle( b );
			b.put( "num_kills", 9 );
			e.restoreFromBundle( b );
			ancient.enchant( e );
			Item[] items = {
					of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Paralysis.class ),
					of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vicious.class ),
					of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Slashing.class ),
					ancient,
					of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas.class ),
					of( xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Polarized.class ) };
			for (int i = 0; i < items.length; i++){
				slot( s, items[i], vx( s ) + 4f + (i % 3) * 26f, vy() + 4f + (i / 3) * 20f, now );
			}
			Item gold = of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas.class );
			xyz.gabriwar.warpedpixeldungeon.items.Heap h = new xyz.gabriwar.warpedpixeldungeon.items.Heap();
			h.pos = s.d.ch.pos + EnchantFxTest.W;
			h.drop( gold );
			h.seen = true;
			ItemSprite sp = new ItemSprite();
			s.rig.stage.add( sp );
			sp.link( h );
			if (!now){
				ItemSprite.Glowing g = gold.glowing();
				sp.view( gold.image(), new ItemSprite.Glowing( g.color, g.period ) );
			}
		}

		/** Swords with an echo (an identified vestige) beside one without. */
		static void echo( EnchantPreviewDump.Scene s, boolean now ){
			hide( s );
			slot( s, sword( new Blazing(), null, new Chilling() ), vx( s ) + 10f, vy() + 30f, now );
			slot( s, sword( new Grim(), null, new Blazing() ), vx( s ) + 32f, vy() + 30f, now );
			slot( s, sword( new Blazing(), null, null ), vx( s ) + 54f, vy() + 30f, now );
		}
	}

	@Test
	public void noRandomOfEitherKind(){
		twelve();
		rig.frames( 2, true );
		FxRandom.seedForTests( 41 );
		int draws = EnchantFxTest.draws( () -> rig.frames( 1000, true ) );
		float next = FxRandom.Float();
		FxRandom.seedForTests( 41 );
		assertEquals( "the game's Random", 0, draws );
		assertEquals( "FxRandom", FxRandom.Float(), next, 0f );
	}

	@Test
	public void nothingIsMadeOnceTheyAreMade(){
		List<ItemSprite> slots = twelve();
		for (int i = 0; i < 120; i++){
			Game.timeTotal += 1 / 60f;
			for (ItemSprite s : slots) s.update();
		}
		final ItemSprite[] all = slots.toArray( new ItemSprite[0] );
		long bytes = allocated( () -> {
			for (int i = 0; i < 1000; i++){
				Game.timeTotal += 1 / 60f;
				for (int k = 0; k < all.length; k++) all[k].update();
			}
		} );
		assertTrue( "a thousand frames of twelve slots made " + bytes + " bytes", bytes < 4 * 1024 );
	}

	static long allocated( Runnable r ){
		com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
		long id = Thread.currentThread().getId();
		long before = mx.getThreadAllocatedBytes( id );
		r.run();
		return mx.getThreadAllocatedBytes( id ) - before;
	}

	@Test
	public void atMostFiveQuadsASlotAndSevenAWindow(){
		EnchantLook blazing = EnchantLooks.of( Blazing.class );
		for (ItemSprite.Mode mode : new ItemSprite.Mode[]{ ItemSprite.Mode.SLOT, ItemSprite.Mode.PANEL }){
			int cap = mode == ItemSprite.Mode.SLOT ? ItemAura.SLOT_QUADS : ItemAura.PANEL_QUADS;
			for (EnchantLook l : EnchantLooks.all()){
				ItemAura.resetPool();
				List<ItemSprite> slots = new ArrayList<>();
				Item one = of( l );
				slots.add( slot( one, mode, false ) );
				if (one instanceof Weapon && !l.curse){
					slots.add( slot( sword( (Weapon.Enchantment) Reflection.newInstance( l.of ),
							l == blazing ? new Chilling() : new Blazing(), new Grim() ), mode, false ) );
				}
				int most = 0;
				for (int f = 0; f < 400; f++){
					Game.timeTotal += 1 / 60f;
					for (ItemSprite s : slots){
						s.update();
						assertNotNull( l + " an aura", s.aura() );
						most = Math.max( most, s.aura().quads() );
						assertTrue( l + " " + mode + " drew " + s.aura().quads(), s.aura().quads() <= cap );
					}
				}
				assertTrue( l + " drew its backlight and a mote at least once", most >= 2 );
				for (ItemSprite s : slots) s.killAndErase();
			}
		}
	}

	@Test
	public void twoHundredSlotsStayWithinThePool(){
		List<ItemSprite> slots = new ArrayList<>();
		for (int i = 0; i < 200; i++) slots.add( slot( sword( new Blazing(), new Chilling(), new Shocking() ), ItemSprite.Mode.SLOT, false ) );
		boolean stretched = false;
		for (int f = 0; f < 120; f++){
			Game.timeTotal += 1 / 60f;
			for (ItemSprite s : slots) s.update();
			assertTrue( "held " + ItemAura.held(), ItemAura.held() <= ItemAura.POOL );
			int sum = 0;
			for (ItemSprite s : slots){
				ItemAura a = s.aura();
				sum += a.claimed();
				Image[] q = a.quadsInOrder();
				int motes = a.quads() - 1 - (q[0].visible ? 1 : 0);
				assertTrue( "motes drawn within the claim", motes <= a.claimed() );
			}
			assertEquals( ItemAura.held(), sum );
			if (ItemAura.held() > ItemAura.STRETCH_PAST) stretched = true;
		}
		assertTrue( "past 96 in use the periods stretch", stretched );
		//an equipped slot coming late is served first
		ItemSprite first = slot( sword( new Vampiric(), null, null ), ItemSprite.Mode.SLOT, true );
		for (int f = 0; f < 3; f++){
			Game.timeTotal += 1 / 60f;
			for (ItemSprite s : slots) s.update();
			first.update();
		}
		assertTrue( "served first", first.aura().claimed() > 0 );
		assertTrue( ItemAura.held() <= ItemAura.POOL );
		//a hidden slot gives its quads back
		int had = first.aura().claimed();
		int held = ItemAura.held();
		first.auraGate( false, true );
		first.update();
		assertEquals( 0, first.aura().claimed() );
		assertEquals( held - had, ItemAura.held() );
	}

	@Test
	public void aDestroyedSlotFreesItsQuadsBuffers() throws Exception {
		java.lang.reflect.Field f = com.watabou.glwrap.Vertexbuffer.class.getDeclaredField( "buffers" );
		f.setAccessible( true );
		List<?> buffers = (List<?>) f.get( null );
		rig.stage.camera = com.watabou.noosa.Camera.main;
		int before = buffers.size();
		List<ItemSprite> slots = new ArrayList<>();
		slots.add( slot( sword( new Blazing(), new Chilling(), new Grim() ), ItemSprite.Mode.SLOT, true ) );
		slots.add( slot( sword( new xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Eldritch(), null, null ),
				ItemSprite.Mode.PANEL, true ) );
		for (int i = 0; i < 90; i++){
			Game.timeTotal += 1 / 60f;
			for (ItemSprite s : slots){
				s.update();
				s.aura().drawBack( s );
				s.aura().drawFront( s );
			}
		}
		assertTrue( "the motes drew into buffers of their own", buffers.size() > before );
		for (ItemSprite s : slots){
			ItemAura a = s.aura();
			s.destroy();
			assertTrue( "no aura once destroyed", s.aura() == null );
			assertEquals( "its pool share given back", 0, a.claimed() );
		}
		assertEquals( "every buffer freed", before, buffers.size() );
	}

	//the quads of a slot drawn over 2 s of its own clock, frame by frame: what each test asks of them
	private interface Frame {
		void at( ItemAura a, Image[] q, float t );
	}

	private void run( ItemSprite s, float seconds, Frame f ){
		for (int i = 0; i < Math.round( seconds * 60f ); i++){
			Game.timeTotal += 1 / 60f;
			s.update();
			f.at( s.aura(), s.aura().quadsInOrder(), s.lookTime() );
		}
	}

	private static int motesShown( Image[] q ){
		int n = 0;
		for (int i = 2; i < q.length - 1; i++) if (q[i].visible && q[i].am > 0.004f) n++;
		return n;
	}

	@Test
	public void paralysisRingStopsDead(){
		ItemSprite s = slot( of( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Paralysis.class ) ),
				ItemSprite.Mode.SLOT, true );
		final float[] last = { -1f };
		final int[] held = { 0 }, mostHeld = { 0 }, grew = { 0 };
		run( s, 4f, ( a, q, t ) -> {
			assertTrue( "stillness: one ring at most", motesShown( q ) <= 1 );
			Image ring = q[2];
			if (!ring.visible){
				last[0] = -1f;
				held[0] = 0;
				return;
			}
			if (last[0] >= 0 && ring.scale.x == last[0]) mostHeld[0] = Math.max( mostHeld[0], ++held[0] );
			else held[0] = 0;
			if (last[0] >= 0 && ring.scale.x > last[0]) grew[0]++;
			last[0] = ring.scale.x;
		} );
		assertTrue( "it grew", grew[0] > 3 );
		assertTrue( "then held still for most of 0.2 s: " + mostHeld[0] + " frames", mostHeld[0] >= 9 );
	}

	@Test
	public void viciousBeadSplitsAndItsSerrationGlintsAlone(){
		ItemSprite s = slot( of( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vicious.class ) ),
				ItemSprite.Mode.SLOT, true );
		final boolean[] split = { false }, glint = { false };
		run( s, 6f, ( a, q, t ) -> {
			//the bead (slot 0) and its twin (slot 1) apart, side by side
			if (q[2].visible && q[3].visible && q[3].x > q[2].x + 1f) split[0] = true;
			//the serration (slot 2): a spark turned on the edge
			if (q[4].visible && q[4].angle == 45f) glint[0] = true;
		} );
		assertTrue( "the bead parted in two as it fell", split[0] );
		assertTrue( "its serration glinted", glint[0] );
	}

	@Test
	public void slashingStreaksAcrossItsBlade(){
		ItemSprite s = slot( of( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Slashing.class ) ),
				ItemSprite.Mode.SLOT, true );
		final int[] frames = { 0 };
		run( s, 4f, ( a, q, t ) -> {
			if (q[2].visible && q[2].angle == -45f) frames[0]++;
		} );
		assertTrue( "two or three streaks of a few frames each in 4 s: " + frames[0], frames[0] >= 6 && frames[0] <= 18 );
	}

	@Test
	public void ancientShowsARuneMoreForEachFourKills(){
		for (int kills : new int[]{ 0, 4, 9 }){
			Sword w = new Sword();
			xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Ancient e = new xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Ancient();
			com.watabou.utils.Bundle b = new com.watabou.utils.Bundle();
			e.storeInBundle( b );
			b.put( "num_kills", kills );
			e.restoreFromBundle( b );
			w.enchant( e );
			int want = 1 + kills / 4;
			assertEquals( "runes for " + kills + " kills", want, ItemLooks.of( w ).runes );
			ItemAura.resetPool();
			ItemSprite s = slot( w, ItemSprite.Mode.SLOT, true );
			final int[] most = { 0 };
			run( s, 3f, ( a, q, t ) -> {
				int runes = 0;
				for (int i = 2; i < q.length - 1; i++){
					//a rune orbits in gold (added), the dust falls as matter
					if (q[i].visible && q[i].width == 3f && q[i].height == 3f) runes++;
				}
				most[0] = Math.max( most[0], runes );
			} );
			assertEquals( "runes drawn for " + kills + " kills", want, most[0] );
			s.killAndErase();
		}
	}

	@Test
	public void midasKeepsOneCoinAndDropsGold(){
		ItemSprite s = slot( of( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas.class ) ),
				ItemSprite.Mode.SLOT, true );
		final int[] coins = { 0 }, gold = { 0 };
		run( s, 4f, ( a, q, t ) -> {
			int c = 0;
			for (int i = 2; i < q.length - 1; i++){
				if (!q[i].visible) continue;
				if (q[i].width == 6f) c++;
				else if (q[i].width == 1f) gold[0]++;
			}
			assertTrue( "one coin", c <= 1 );
			coins[0] = Math.max( coins[0], c );
		} );
		assertEquals( 1, coins[0] );
		assertTrue( "gold fell off it", gold[0] > 0 );
	}

	@Test
	public void polarizedSplitsItsBacklight(){
		ItemSprite s = slot( of( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Polarized.class ) ),
				ItemSprite.Mode.SLOT, true );
		run( s, 1f, ( a, q, t ) -> {
			Image halo = q[0], back = q[1];
			assertTrue( back.visible && halo.visible );
			assertEquals( "half the glow each", 5.5f, back.width, 0.001f );
			assertEquals( 5.5f, halo.width, 0.001f );
			assertTrue( "red on the left", back.rm > back.bm && back.x < halo.x );
			assertTrue( "blue on the right", halo.bm > halo.rm );
			assertTrue( a.quads() <= ItemAura.SLOT_QUADS );
		} );
		//another look on the same slot: the whole glow back
		s.view( of( EnchantLooks.of( Blazing.class ) ) );
		run( s, 0.1f, ( a, q, t ) -> assertEquals( 11f, q[1].width, 0.001f ) );
	}

	@Test
	public void theSameItemViewedAgainReKeysNothing(){
		Sword w = sword( new Blazing(), new Chilling(), new Grim() );
		ItemSprite s = slot( w, ItemSprite.Mode.SLOT, true );
		for (int i = 0; i < 30; i++){
			Game.timeTotal += 1 / 60f;
			s.update();
		}
		ItemAura aura = s.aura();
		Image[] quads = aura.quadsInOrder();
		long bytes = allocated( () -> {
			for (int i = 0; i < 1000; i++){
				//a quickslot's refresh, every turn
				s.view( w );
				Game.timeTotal += 1 / 60f;
				s.update();
			}
		} );
		//what a view made before any of this: the two enchantments' averaged Glowing
		//(Weapon.glowing) and the item film's boxed frame ids
		long before = allocated( () -> {
			for (int i = 0; i < 1000; i++){
				w.glowing();
				xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet.film.get( w.image() );
				xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet.film.height( w.image() );
			}
		} );
		assertSame( aura, s.aura() );
		Image[] again = aura.quadsInOrder();
		for (int i = 0; i < quads.length; i++) assertSame( quads[i], again[i] );
		assertTrue( "a thousand refreshes made " + bytes + " bytes, a view's own " + before, bytes - before < 4 * 1024 );
		//another item with the same looks is the same aura too
		Sword twin = sword( new Blazing(), new Chilling(), new Grim() );
		s.view( twin );
		s.update();
		assertSame( aura, s.aura() );
	}
}
