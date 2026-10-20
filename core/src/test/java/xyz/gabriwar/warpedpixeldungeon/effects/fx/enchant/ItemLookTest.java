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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ElfBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MegaCannon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MiniGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ShootGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.TaurcenBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.Bow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.FT.FT;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.Group;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The item's own look (ItemLooks, ItemSprite's modes): every look's glow bounded as the art spec's
 * 2.3 says (a share of at most 0.45; a lasting one within 0.12 of its rest; no blip or beat
 * faster than 3 a second; the backlight between its trough and peak, a lasting one within 12% of
 * its middle); two looks crossfading on their timing; an icon drawing its look's profile and no
 * motes or backlight; the enchantments' Glowings untouched; the holy override's yellow shimmering;
 * a curse shown only once known; a launcher's shot and a bow's arrow showing their launcher's;
 * heaps: the nearest shedding and each discovery flaring once - not again for a heap made anew
 * (a co-op guest's, cleared and made again with every change, its old look let go) nor for the
 * hero's own item, and nothing from a hidden heap or an item falling into a chasm.
 */
public class ItemLookTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private EnchantFxTest.Rig rig;

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
	}

	@After
	public void tearDown(){
		Dungeon.hero = null;
		rig.down();
	}

	@Test
	public void everyProfileIsBounded(){
		for (EnchantLook l : EnchantLooks.all()){
			for (float period : new float[]{ 1f, 0.5f, 0.25f }){
				int blips = 0;
				boolean high = false;
				for (int i = 0; i < 100 * 120; i++){
					float t = i / 120f;
					float v = ItemLooks.share( l, t, period );
					assertTrue( l + " share " + v, v >= 0f && v <= ItemLooks.MAX_SHARE + 1e-5f );
					float b = ItemLooks.backlight( l.profile, v );
					assertTrue( l + " backlight " + b, b >= ItemLooks.TROUGH - 1e-5f && b <= ItemLooks.PEAK + 1e-5f );
					if (l.profile.lasting){
						assertTrue( l + " lasting " + v, Math.abs( v - l.profile.base ) <= Profile.MOST_SWING + 1e-4f );
						assertTrue( l + " lasting backlight " + b,
								Math.abs( b - ItemLooks.MIDDLE ) <= ItemLooks.MIDDLE * ItemLooks.LASTING_SWING + 1e-4f );
					}
					//a rise of the light past the middle of its range: one blip or beat
					boolean up = v > l.profile.base + 0.1f;
					if (up && !high) blips++;
					high = up;
				}
				assertTrue( l + " blips or beats " + blips + " in 100 s", blips <= 300 );
			}
		}
	}

	@Test
	public void twoLooksCrossfadeOnTheirTiming(){
		assertEquals( 0f, ItemLooks.crossB( 0f ), 1e-5f );
		assertEquals( 0f, ItemLooks.crossB( 1.19f ), 1e-5f );
		assertEquals( 0.5f, ItemLooks.crossB( 1.4f ), 1e-4f );
		assertEquals( 1f, ItemLooks.crossB( 1.6f ), 1e-4f );
		assertEquals( 1f, ItemLooks.crossB( 2.79f ), 1e-5f );
		assertEquals( 0.5f, ItemLooks.crossB( 3.0f ), 1e-4f );
		assertEquals( 0f, ItemLooks.crossB( 3.2f ), 1e-4f );

		//a sword of two: its tint is the first's while it holds, the second's once blended
		Sword sword = new Sword();
		sword.enchant( new Blazing() );
		sword.addEnchant( new Shocking() );
		ItemLooks.Set s = ItemLooks.of( sword );
		assertSame( EnchantLooks.of( Blazing.class ), s.a );
		assertSame( EnchantLooks.of( Shocking.class ), s.b );
		float[] both = new float[13], one = new float[13];
		ItemLooks.Set a = new ItemLooks.Set(), b = new ItemLooks.Set();
		a.a = s.a;
		b.a = s.b;
		b.periodA = s.periodB;
		for (float base : new float[]{ 0.3f, 1.8f }){
			ItemLooks.tint( s, base, both );
			ItemLooks.tint( base < 1f ? a : b, base, one );
			for (int i = 0; i < 6; i++) assertEquals( "at " + base, one[i], both[i], 1e-5f );
		}
		//Shocking keeps its flicker: the second look's share blips while it shows
		boolean blip = false;
		for (int i = 0; i < 30000; i++){
			float t = i / 1000f;
			if (ItemLooks.crossB( t ) == 1f && ItemLooks.share( s.b, t, s.periodB ) > 0.4f) blip = true;
		}
		assertTrue( "Shocking's blips while it shows", blip );
	}

	@Test
	public void anIconDrawsItsProfileAndNoMotesNorBacklight(){
		Group stage = rig.stage;
		ItemSprite icon = new ItemSprite( ItemSpriteSheet.SWORD, new Blazing().glowing() );
		stage.add( icon );
		assertSame( ItemSprite.Mode.ICON, icon.mode() );
		assertSame( EnchantLooks.of( Blazing.class ), icon.looks().a );
		Sword sword = new Sword();
		sword.enchant( new Chilling() );
		ItemSprite viewed = new ItemSprite( sword );
		stage.add( viewed );
		for (int i = 0; i < 120; i++){
			rig.frames( 1, true );
			assertNull( icon.aura() );
			assertNull( icon.heapLook() );
			assertNull( icon.trail() );
			assertNull( viewed.aura() );
		}
		assertTrue( "the look's colour tints it", icon.ra > 0f );
		//the 18x18 rule of the guide's chips: its size is the item's frame's, nothing round it
		assertEquals( ItemSpriteSheet.film.width( ItemSpriteSheet.SWORD ), icon.width(), 0.01f );
		assertEquals( ItemSpriteSheet.film.height( ItemSpriteSheet.SWORD ), icon.height(), 0.01f );
		//nothing of the kit's was made for it
		for (com.watabou.noosa.Gizmo g : rig.layers[xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.Layer.LIGHTS.ordinal()].membersView()){
			assertTrue( "nothing in the light layer: " + g, !(g instanceof HeapLook) && !(g instanceof EnchantTrail) );
		}
		//a plain glow keeps today's pulse
		ItemSprite plain = new ItemSprite( ItemSpriteSheet.SWORD, new ItemSprite.Glowing( 0x00FF00 ) );
		stage.add( plain );
		float most = 0f;
		for (int i = 0; i < 120; i++){
			rig.frames( 1, true );
			most = Math.max( most, plain.ga );
		}
		assertTrue( "today's pulse reaches past the looks' cap: " + most, most > ItemLooks.MAX_SHARE );
	}

	@Test
	public void heapsShedNearestFirstAndFlareOnceAtFirstSight(){
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = EnchantFxTest.at( 1, 1 );
		final java.util.List<String> heard = new java.util.ArrayList<>();
		HeapLook.Ear ear = HeapLook.ear;
		HeapLook.ear = ( sample, cell, volume, pitch ) -> heard.add( sample );
		try {
			HeapLook.reset();
			HeapLook.forgetSights();
			java.util.List<ItemSprite> heaps = new java.util.ArrayList<>();
			for (int i = 0; i < 10; i++){
				Sword w = new Sword();
				if (i < 2) ItemAuraTest.exotic( w );
				w.enchant( new Blazing() );
				xyz.gabriwar.warpedpixeldungeon.items.Heap h = new xyz.gabriwar.warpedpixeldungeon.items.Heap();
				h.pos = EnchantFxTest.at( 2 + i, 1 + i % 2 );
				h.drop( w );
				h.seen = true;
				ItemSprite sp = new ItemSprite();
				rig.stage.add( sp );
				sp.link( h );
				heaps.add( sp );
			}
			rig.frames( 2, true );
			for (ItemSprite sp : heaps){
				assertNotNull( "a heap's look", sp.heapLook() );
				assertTrue( "its first sight played", sp.heapLook().sighted() );
			}
			assertEquals( "the two legendary heaps stung, once each", 2, heard.size() );
			for (int f = 0; f < 240; f++){
				rig.frames( 1, true );
				for (ItemSprite sp : heaps) assertTrue( sp.heapLook().livingMotes() <= HeapLook.PER_HEAP );
			}
			int emitting = 0;
			for (int i = 0; i < heaps.size(); i++){
				int r = heaps.get( i ).heapLook().rank();
				if (r >= 0 && r < HeapLook.MOST_EMIT) emitting++;
				if (i >= HeapLook.MOST_EMIT) assertTrue( "a far heap sheds nothing: " + r, r < 0 || r >= HeapLook.MOST_EMIT );
			}
			assertEquals( HeapLook.MOST_EMIT, emitting );
			assertTrue( "the nearest legendary blazing heap holds its light", heaps.get( 0 ).heapLook().lit() );
			assertTrue( "a common one does not", !heaps.get( 5 ).heapLook().lit() );
			assertEquals( "no first sight again", 2, heard.size() );
		} finally {
			HeapLook.ear = ear;
			HeapLook.reset();
		}
	}

	//an exotic blazing sword
	private static Sword exoticBlazing(){
		Sword w = new Sword();
		ItemAuraTest.exotic( w );
		w.enchant( new Blazing() );
		return w;
	}

	private static xyz.gabriwar.warpedpixeldungeon.items.Heap heap( xyz.gabriwar.warpedpixeldungeon.items.Item item, int cell, boolean hidden ){
		xyz.gabriwar.warpedpixeldungeon.items.Heap h = new xyz.gabriwar.warpedpixeldungeon.items.Heap();
		h.pos = cell;
		h.drop( item );
		h.seen = true;
		h.hidden = hidden;
		return h;
	}

	//a heap's sprite in a group, as the scene's heaps are
	private static ItemSprite sprite( Group heaps, xyz.gabriwar.warpedpixeldungeon.items.Heap h, ItemSprite sp ){
		heaps.add( sp );
		sp.link( h );
		return sp;
	}

	//a co-op guest's heap: a stand-in item signed with what the host sent
	private static ItemSprite guestHeap( Group heaps, int[] sig, int cell ){
		xyz.gabriwar.warpedpixeldungeon.items.Item standIn = new xyz.gabriwar.warpedpixeldungeon.items.Item();
		standIn.image = ItemSpriteSheet.SWORD;
		ItemLooks.sign( standIn, sig );
		return sprite( heaps, heap( standIn, cell, false ), new ItemSprite() );
	}

	private int heapLooksAlive(){
		int n = 0;
		for (com.watabou.noosa.Gizmo g : rig.layers[xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.Layer.LIGHTS.ordinal()].membersView()){
			if (g instanceof HeapLook && g.alive) n++;
		}
		return n;
	}

	@Test
	public void aHeapMadeAgainIsNoNewDiscovery(){
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = EnchantFxTest.at( 1, 1 );
		final java.util.List<String> heard = new java.util.ArrayList<>();
		HeapLook.Ear ear = HeapLook.ear;
		HeapLook.ear = ( sample, cell, volume, pitch ) -> heard.add( sample );
		try {
			HeapLook.reset();
			HeapLook.forgetSights();
			Group heaps = new Group();
			rig.stage.add( heaps );
			int[] sig = ItemLooks.sig( exoticBlazing() );
			ItemSprite first = guestHeap( heaps, sig, EnchantFxTest.at( 4, 2 ) );
			rig.frames( 3, true );
			HeapLook look = first.heapLook();
			assertNotNull( look );
			assertTrue( look.sighted() );
			assertEquals( "the host said exotic: the guest's heap stings too", 1, heard.size() );
			assertEquals( 1, heapLooksAlive() );

			//every heap cleared and made again, as a guest's are with any heap's change
			heaps.clear();
			ItemSprite again = guestHeap( heaps, sig, EnchantFxTest.at( 4, 2 ) );
			rig.frames( 3, true );
			assertTrue( "the old heap's look let go", !look.alive );
			assertNotNull( again.heapLook() );
			assertTrue( again.heapLook() != look );
			assertEquals( "one glow, not two stacked", 1, heapLooksAlive() );
			assertEquals( "no second discovery", 1, heard.size() );
		} finally {
			HeapLook.ear = ear;
			HeapLook.reset();
			HeapLook.forgetSights();
		}
	}

	@Test
	public void hiddenFallingAndOwnItemsGiveNothingAway(){
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = EnchantFxTest.at( 1, 1 );
		final java.util.List<String> heard = new java.util.ArrayList<>();
		HeapLook.Ear ear = HeapLook.ear;
		HeapLook.ear = ( sample, cell, volume, pitch ) -> heard.add( sample );
		try {
			HeapLook.reset();
			HeapLook.forgetSights();
			Group heaps = new Group();
			rig.stage.add( heaps );
			//a hidden heap: a faint hint of loot
			ItemSprite hidden = sprite( heaps, heap( exoticBlazing(), EnchantFxTest.at( 3, 3 ), true ), new ItemSprite() );
			//an exotic item falling into a chasm
			ItemSprite falling = sprite( heaps, heap( exoticBlazing(), EnchantFxTest.at( 5, 3 ), false ),
					new xyz.gabriwar.warpedpixeldungeon.sprites.DiscardedItemSprite() );
			//the hero's own exotic sword, seen in his bag, then dropped
			Sword own = exoticBlazing();
			ItemSprite bag = new ItemSprite().mode( ItemSprite.Mode.SLOT );
			bag.view( own );
			bag.auraGate( true, true );
			rig.stage.add( bag );
			rig.frames( 1, true );
			ItemSprite dropped = sprite( heaps, heap( own, EnchantFxTest.at( 7, 3 ), false ), new ItemSprite() );
			//and a Midas heap, its coin hopping
			Sword gold = new Sword();
			gold.enchant( new xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas() );
			ItemSprite midas = sprite( heaps, heap( gold, EnchantFxTest.at( 2, 4 ), false ), new ItemSprite() );
			for (int f = 0; f < 360; f++){
				rig.frames( 1, true );
				assertNull( "no look for a falling item", falling.heapLook() );
				assertEquals( "a hidden heap sheds nothing", 0, hidden.heapLook().livingMotes() );
				assertTrue( midas.heapLook().livingMotes() <= HeapLook.PER_HEAP );
			}
			assertTrue( "a hidden heap's first sight waits till it is found", !hidden.heapLook().sighted() );
			assertTrue( dropped.heapLook().sighted() );
			assertTrue( "no discovery stung: " + heard, heard.isEmpty() );

			//found: now it is one
			hidden.heap.hidden = false;
			rig.frames( 2, true );
			assertTrue( hidden.heapLook().sighted() );
			assertEquals( 1, heard.size() );
		} finally {
			HeapLook.ear = ear;
			HeapLook.reset();
			HeapLook.forgetSights();
		}
	}

	@Test
	public void theGlowingsAreUntouched(){
		assertEquals( 0xFF4400, new Blazing().glowing().color );
		assertEquals( 0xFFFF00, new Kinetic().glowing().color );
		assertEquals( 0xFFFFFF, new Shocking().glowing().color );
		assertEquals( 0.5f, new Shocking().glowing().period, 0f );
		for (EnchantLook l : EnchantLooks.all()){
			Object e = Reflection.newInstance( l.of );
			ItemSprite.Glowing g = e instanceof Weapon.Enchantment ? ((Weapon.Enchantment) e).glowing()
					: ((xyz.gabriwar.warpedpixeldungeon.items.armor.Armor.Glyph) e).glowing();
			if (g == null) continue;
			int color = g.color;
			float r = g.red, gg = g.green, b = g.blue, period = g.period;
			ItemSprite s = new ItemSprite( ItemSpriteSheet.SWORD, g );
			rig.stage.add( s );
			rig.frames( 30, true );
			assertEquals( l + "", color, g.color );
			assertEquals( r, g.red, 0f );
			assertEquals( gg, g.green, 0f );
			assertEquals( b, g.blue, 0f );
			assertEquals( period, g.period, 0f );
			s.killAndErase();
		}
	}

	@Test
	public void theHolyOverrideShimmersYellow(){
		Hero h = new Hero();
		Dungeon.hero = h;
		Sword sword = new Sword();
		h.belongings.weapon = sword;
		Buff.affect( h, HolyWeapon.HolyWepBuff.class, HolyWeapon.HolyWepBuff.DURATION );
		ItemLooks.Set s = ItemLooks.of( sword );
		assertEquals( ItemLooks.HOLY, s.holy );
		assertNull( s.a );
		float[] out = new float[13];
		float lo = 1f, hi = 0f;
		for (int i = 0; i < 1000; i++){
			ItemLooks.tint( s, i / 1000f, out );
			float v = 1f - out[0];
			lo = Math.min( lo, v );
			hi = Math.max( hi, v );
			assertEquals( ItemLooks.HOLY, (int)out[6] );
		}
		//SHIMMER: 0.30, 0.06 either way at 3 Hz
		assertEquals( 0.24f, lo, 0.01f );
		assertEquals( 0.36f, hi, 0.01f );
		//a held weapon with a glyphed armour: the armour's own look, the sword the yellow
		ClothArmor armour = new ClothArmor();
		armour.inscribe( new Brimstone() );
		assertSame( EnchantLooks.of( Brimstone.class ), ItemLooks.of( armour ).a );
	}

	@Test
	public void aCurseShowsOnlyOnceKnown(){
		Sword sword = new Sword();
		sword.enchant( new Annoying() );
		sword.cursed = true;
		sword.cursedKnown = false;
		assertNull( ItemLooks.of( sword ).a );
		sword.cursedKnown = true;
		EnchantLook l = ItemLooks.of( sword ).a;
		assertSame( EnchantLooks.of( Annoying.class ), l );
		assertSame( Profile.CURSE, l.profile );
		//a good one shows unidentified
		Sword good = new Sword();
		good.enchant( new Blazing() );
		assertTrue( !good.isIdentified() );
		assertSame( EnchantLooks.of( Blazing.class ), ItemLooks.of( good ).a );
	}

	@Test
	public void shotsAndArrowsShowTheirLaunchers(){
		EnchantLook blazing = EnchantLooks.of( Blazing.class ), chilling = EnchantLooks.of( Chilling.class );
		FT ft = new FT();
		ft.enchant( new Blazing() );
		ft.addEnchant( new Chilling() );
		ItemLooks.Set s = ItemLooks.of( ft.knockBullet() );
		assertSame( blazing, s.a );
		assertSame( chilling, s.b );
		MiniGun mini = new MiniGun();
		mini.enchant( new Blazing() );
		assertSame( blazing, ItemLooks.of( mini.knockShot() ).a );
		ShootGun shotgun = new ShootGun();
		shotgun.enchant( new Blazing() );
		assertSame( blazing, ItemLooks.of( shotgun.knockShot() ).a );
		MegaCannon cannon = new MegaCannon();
		cannon.enchant( new Blazing() );
		assertSame( blazing, ItemLooks.of( cannon.knockShot() ).a );
		Bow bow = new Bow();
		bow.enchant( new Chilling() );
		assertSame( chilling, ItemLooks.of( bow.knockArrow() ).a );

		SpiritBow spirit = new SpiritBow();
		spirit.enchant( new Blazing() );
		SpiritBow.SpiritArrow arrow = spirit.knockArrow();
		assertSame( spirit, arrow.launcher() );
		assertSame( blazing, ItemLooks.of( arrow ).a );
		assertSame( spirit.glowing(), arrow.glowing() );
		ElfBow elf = new ElfBow();
		elf.enchant( new Chilling() );
		assertSame( chilling, ItemLooks.of( elf.knockArrow() ).a );
		assertSame( elf.glowing(), elf.knockArrow().glowing() );
		TaurcenBow taurcen = new TaurcenBow();
		taurcen.enchant( new Blazing() );
		assertSame( blazing, ItemLooks.of( taurcen.knockArrow() ).a );

		//and a sprite in flight draws its launcher's look
		ItemSprite shot = new ItemSprite( ft.knockBullet() );
		shot.mode( ItemSprite.Mode.FLIGHT );
		rig.stage.add( shot );
		rig.frames( 3, true );
		assertSame( blazing, shot.looks().a );
		assertTrue( "it is tinted", shot.ra + shot.ga + shot.ba > 0f );
	}
}
