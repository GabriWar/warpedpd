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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Enchanting;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicalInfusion;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Javelin;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import com.watabou.noosa.Gizmo;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The moments an item gains or shows its magic outside Enchanting's ceremony, headless: an
 * enchanted item's first pickup plays once per item (collect's answer and the pack as without it);
 * identifying reveals a curse (the hex, CURSED) and a vestige (its finale, then its echo) once each
 * (identify's answer unchanged); none draws the game's Random; Scroll of Magical Infusion shows
 * Enchanting only when it gave the item its first enchantment or glyph; the host sends GAIN and
 * REVEAL and a guest plays them.
 */
public class GainMomentsTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private final List<String> heard = new ArrayList<>();
	private Hero hero;
	//the badges held as all earned, so collecting or infusing shows and saves none
	private Object savedGlobal, savedLocal;
	private String savedSeed;

	private static Field badges( String name ) throws Exception {
		Field f = Badges.class.getDeclaredField( name );
		f.setAccessible( true );
		return f;
	}

	@Before
	public void setUp() throws Exception {
		savedGlobal = badges( "global" ).get( null );
		savedLocal = badges( "local" ).get( null );
		savedSeed = Dungeon.customSeedText;
		badges( "global" ).set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
		badges( "local" ).set( null, new HashSet<Badges.Badge>() );
		Dungeon.customSeedText = "test";
		rig = new EnchantFxTest.Rig();
		rig.up();
		heard.clear();
		Sting.out = ( sample, at, volume, pitch ) -> heard.add( sample );
		GainFx.forgetPickups();
		NetVisuals.clear();
		hero = new Hero();
		hero.HP = hero.HT = 20;
		hero.pos = EnchantFxTest.at( 5, 6 );
		Dungeon.hero = hero;
		CharSprite s = new HeroSprite( hero );
		rig.stage.add( s );
		s.place( hero.pos );
		s.visible = true;
	}

	@After
	public void tearDown() throws Exception {
		TestParty.leave();
		NetVisuals.setSpectatorMobs( null );
		NetVisuals.clear();
		Dungeon.hero = null;
		rig.down();
		badges( "global" ).set( null, savedGlobal );
		badges( "local" ).set( null, savedLocal );
		Dungeon.customSeedText = savedSeed;
	}

	//what a run makes: quads that came alive and the most lights over `frames` frames
	private int[] made( Runnable r, int frames ){
		IdentityHashMap<Gizmo, Boolean> prev = new IdentityHashMap<>(), now = new IdentityHashMap<>();
		rig.alive( prev );
		int lights0 = FxLight.live(), quads = 0, lights = 0;
		r.run();
		for (int f = 0; f <= frames; f++){
			now.clear();
			rig.alive( now );
			for (Gizmo g : now.keySet()) if (!prev.containsKey( g )) quads++;
			lights = Math.max( lights, FxLight.live() - lights0 );
			IdentityHashMap<Gizmo, Boolean> t = prev;
			prev = now;
			now = t;
			rig.frames( 1, true );
		}
		return new int[]{ quads, lights };
	}

	private static Sword enchanted( Weapon.Enchantment e ){
		Sword s = new Sword();
		Quality.roll( s, Rarity.COMMON, ItemType.GAMMA, 0 );
		s.enchant( e );
		s.identify();
		return s;
	}

	// ------------------------------------------------------------------ the first pickup

	@Test
	public void theFirstPickupPlaysOncePerItemAndChangesNothing(){
		final Sword sword = enchanted( new Blazing() );
		final boolean[] got = new boolean[1];
		int[] first = made( () -> got[0] = sword.collect( hero.belongings.backpack ), 20 );
		assertTrue( "collected", got[0] );
		assertTrue( "in the pack", hero.belongings.backpack.items.contains( sword ) );
		assertTrue( "a bloom and its motes: " + first[0], first[0] >= 4 );
		assertEquals( "the bloom's one light", 1, first[1] );
		assertTrue( "its slot runs faster", EnchantFx.gainedAgo( sword ) < 1f );

		//dropped and picked up again: nothing more
		sword.detach( hero.belongings.backpack );
		int[] again = made( () -> got[0] = sword.collect( hero.belongings.backpack ), 20 );
		assertTrue( got[0] );
		assertEquals( "once per item", 0, again[0] );
		assertEquals( 0, again[1] );

		//a plain sword: nothing, and the same pack
		Sword plain = enchanted( null );
		int[] none = made( () -> got[0] = plain.collect( hero.belongings.backpack ), 20 );
		assertTrue( got[0] );
		assertEquals( 0, none[0] );
		assertEquals( 2, hero.belongings.backpack.items.size() );

		//the game's Random: collecting an enchanted sword draws what a plain one draws
		final Sword a = enchanted( new Chilling() ), b = enchanted( null );
		int withMagic = EnchantFxTest.draws( () -> {
			a.collect( hero.belongings.backpack );
			rig.frames( 20, true );
		} );
		int without = EnchantFxTest.draws( () -> {
			b.collect( hero.belongings.backpack );
			rig.frames( 20, true );
		} );
		assertEquals( "no draw of the game's Random", without, withMagic );
	}

	@Test
	public void aPackFilledWithNoSpriteShowsNothing(){
		Hero loading = new Hero();
		Sword sword = enchanted( new Blazing() );
		int[] r = made( () -> sword.collect( loading.belongings.backpack ), 20 );
		assertTrue( loading.belongings.backpack.items.contains( sword ) );
		assertEquals( 0, r[0] );
		assertEquals( "nothing remembered either", Float.MAX_VALUE, EnchantFx.gainedAgo( sword ), 0f );
	}

	@Test
	public void aKnownCurseBloomsInVoidAndAnUnknownOneNot(){
		Sword hidden = enchanted( new Annoying() );
		hidden.cursedKnown = false;
		hidden.levelKnown = false;
		assertEquals( "an unknown curse: nothing", 0, made( () -> hidden.collect( hero.belongings.backpack ), 20 )[0] );
		Sword known = enchanted( new Annoying() );
		assertTrue( known.cursedKnown );
		assertTrue( "a known curse blooms", made( () -> known.collect( hero.belongings.backpack ), 20 )[0] > 0 );
	}

	@Test
	public void gearPutBackIsNoPickup() throws Exception {
		//a host, so whatever played would go out to guests too
		TestParty.join( new Hero() );
		final boolean[] got = new boolean[1];
		//a worn sword and armour, as a loaded game has them: never through collect
		Sword worn = enchanted( new Blazing() );
		hero.belongings.weapon = worn;
		ClothArmor armour = new ClothArmor();
		armour.inscribe( new xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns() );
		armour.identify();
		hero.belongings.armor = armour;
		hero.belongings.backpack.items.clear();

		//unequipped, each is collected while still in its slot: nothing made, nothing sent
		int[] r = made( () -> got[0] = worn.doUnequip( hero, true, false ), 20 );
		assertTrue( got[0] );
		assertTrue( "back in the pack", hero.belongings.backpack.items.contains( worn ) );
		assertEquals( "the sword: nothing", 0, r[0] );
		r = made( () -> got[0] = armour.doUnequip( hero, true, false ), 20 );
		assertTrue( got[0] );
		assertEquals( "the armour: nothing", 0, r[0] );
		assertEquals( Float.MAX_VALUE, EnchantFx.gainedAgo( worn ), 0f );
		assertTrue( "nothing sent", events( NetVisuals.drainEvents() ).isEmpty() );

		//a second weapon leaves its slot before it is collected: held before, so nothing either
		Sword second = enchanted( new Chilling() );
		hero.belongings.secondWep = second;
		Sword found = enchanted( new Blazing() );
		assertTrue( "a new one still blooms", made( () -> found.collect( hero.belongings.backpack ), 20 )[0] > 0 );
		assertEquals( "its PICKUP sent", 1, events( NetVisuals.drainEvents() ).size() );
		r = made( () -> got[0] = second.doUnequip( hero, true, false ), 20 );
		assertTrue( got[0] );
		assertTrue( hero.belongings.backpack.items.contains( second ) );
		assertEquals( "the second weapon put back: nothing", 0, r[0] );
		assertEquals( 0, r[1] );
		assertTrue( "nothing sent", events( NetVisuals.drainEvents() ).isEmpty() );

		//a weapon enchanted where it lay, put back later: nothing
		Sword plain = enchanted( null );
		plain.collect( hero.belongings.backpack );
		plain.enchant( new Chilling() );
		Enchanting.show( hero, plain );
		rig.frames( 75, true );
		plain.detach( hero.belongings.backpack );
		assertEquals( "enchanted, then put back: nothing", 0, made( () -> plain.collect( hero.belongings.backpack ), 20 )[0] );
	}

	@Test
	public void aThrownStacksPiecesAreNoNewPickup(){
		Javelin stack = new Javelin();
		stack.quantity( 3 );
		stack.enchant( new Blazing() );
		stack.identify();
		assertTrue( "the stack blooms once", made( () -> stack.collect( hero.belongings.backpack ), 20 )[0] > 0 );
		//one thrown: a new object split off the stack
		Item piece = stack.detach( hero.belongings.backpack );
		assertTrue( piece != stack );
		//the rest thrown too, and the split piece picked up first
		stack.detachAll( hero.belongings.backpack );
		assertTrue( hero.belongings.backpack.items.isEmpty() );
		final boolean[] got = new boolean[1];
		int[] r = made( () -> got[0] = piece.collect( hero.belongings.backpack ), 20 );
		assertTrue( got[0] );
		assertTrue( hero.belongings.backpack.items.contains( piece ) );
		assertEquals( "a piece of a stack he held: nothing", 0, r[0] );
		//and the stack itself, merged back into it
		assertEquals( 0, made( () -> stack.collect( hero.belongings.backpack ), 20 )[0] );
		assertEquals( 3, piece.quantity() );
	}

	// ------------------------------------------------------------------ identifying

	@Test
	public void identifyingRevealsACurseOnce(){
		final Sword sword = enchanted( new Annoying() );
		sword.cursed = true;
		sword.cursedKnown = false;
		sword.levelKnown = false;
		hero.belongings.backpack.items.add( sword );
		final Item[] got = new Item[1];
		int[] r = made( () -> got[0] = sword.identify(), 30 );
		assertSame( "identify's answer", sword, got[0] );
		assertTrue( sword.isIdentified() );
		assertTrue( "the hex at the hero: " + r[0], r[0] >= 6 );
		assertEquals( "CURSED once: " + heard, 1, heard.size() );
		assertEquals( Assets.Sounds.CURSED, heard.get( 0 ) );
		heard.clear();
		int[] again = made( () -> sword.identify(), 30 );
		assertEquals( "known now: nothing", 0, again[0] );
		assertTrue( heard.isEmpty() );
	}

	@Test
	public void identifyingRevealsAVestigeOnceThenItsEcho(){
		final Sword sword = new Sword();
		Quality.roll( sword, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
		sword.enchant( null );
		sword.quality.vestige = new Blazing();
		assertSame( "a vestige", sword.quality.vestige, Quality.vestigeOf( sword ) );
		sword.cursedKnown = true;
		sword.levelKnown = false;
		hero.belongings.backpack.items.add( sword );
		int[] r = made( () -> sword.identify(), 10 );
		assertTrue( "its enchantment's finale: " + r[0], r[0] > 0 );
		assertTrue( "the echo waits: " + heard, heard.isEmpty() );
		int[] echo = made( () -> { }, 20 );
		assertTrue( "the echo's rings and motes: " + echo[0], echo[0] >= 3 && echo[0] <= GainFx.ECHO_CAP );
		assertEquals( "its sting, muffled: " + heard, 1, heard.size() );
		assertEquals( EnchantLooks.of( Blazing.class ).sound, heard.get( 0 ) );
		heard.clear();
		assertEquals( "known now: nothing", 0, made( () -> sword.identify(), 30 )[0] );
		assertTrue( heard.isEmpty() );
	}

	@Test
	public void identifyingRevealsAnArmoursVestigeOnceThenItsEcho(){
		final ClothArmor cloth = new ClothArmor();
		Quality.roll( cloth, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
		cloth.inscribe( null );
		cloth.quality.vestige = new Thorns();
		assertSame( "a vestige", cloth.quality.vestige, Quality.glyphVestigeOf( cloth ) );
		cloth.cursedKnown = true;
		cloth.levelKnown = false;
		hero.belongings.backpack.items.add( cloth );
		int[] r = made( () -> cloth.identify(), 10 );
		assertTrue( "its glyph's finale: " + r[0], r[0] > 0 );
		assertTrue( "the echo waits: " + heard, heard.isEmpty() );
		int[] echo = made( () -> { }, 20 );
		assertTrue( "the echo's rings and motes: " + echo[0], echo[0] >= 3 && echo[0] <= GainFx.ECHO_CAP );
		assertEquals( "its sting, muffled: " + heard, 1, heard.size() );
		assertEquals( EnchantLooks.of( Thorns.class ).sound, heard.get( 0 ) );
		heard.clear();
		assertEquals( "known now: nothing", 0, made( () -> cloth.identify(), 30 )[0] );
		assertTrue( heard.isEmpty() );
	}

	@Test
	public void anItemOutsideThePackRevealsNothing(){
		Sword sword = enchanted( new Annoying() );
		sword.cursed = true;
		sword.cursedKnown = false;
		assertEquals( 0, made( () -> sword.identify(), 30 )[0] );
		assertTrue( heard.isEmpty() );
	}

	@Test
	public void revealsDrawNothingOfTheGamesRandom(){
		final Sword cursed = enchanted( new Annoying() );
		cursed.cursed = true;
		cursed.cursedKnown = false;
		hero.belongings.backpack.items.add( cursed );
		final Sword vestige = new Sword();
		Quality.roll( vestige, Rarity.EXOTIC, ItemType.GAMMA, 0 );
		vestige.cursedKnown = true;
		vestige.levelKnown = false;
		hero.belongings.backpack.items.add( vestige );
		assertEquals( 0, EnchantFxTest.draws( () -> {
			cursed.identify();
			vestige.identify();
			rig.frames( 60, true );
		} ) );
	}

	// ------------------------------------------------------------------ Magical Infusion

	//the scroll's choice of an item, as its window gives it
	private static final class Infusion extends ScrollOfMagicalInfusion {
		void on( Hero h, Item item ){
			curUser = h;
			onItemSelected( item );
		}
	}

	private List<Enchanting> shown(){
		List<Enchanting> out = new ArrayList<>();
		for (Gizmo g : rig.stage.membersView()) if (g instanceof Enchanting && g.alive) out.add( (Enchanting) g );
		return out;
	}

	@Test
	public void magicalInfusionShowsEnchantingOnlyForAFirstEnchantment(){
		Sword plain = new Sword();
		plain.identify();
		new Infusion().on( hero, plain );
		assertTrue( plain.enchantment != null );
		assertEquals( "the first enchantment shown taking", 1, shown().size() );
		rig.frames( 75, true );
		assertTrue( shown().isEmpty() );

		Sword had = enchanted( new Chilling() );
		new Infusion().on( hero, had );
		assertTrue( "an enchanted one: none", shown().isEmpty() );

		ClothArmor cloth = new ClothArmor();
		cloth.identify();
		new Infusion().on( hero, cloth );
		assertTrue( ((Armor) cloth).glyph != null );
		assertEquals( "an armour's first glyph", 1, shown().size() );
	}

	// ------------------------------------------------------------------ co-op

	//the "eh" events of a drained batch, as (kind, numbers)
	private static List<int[]> events( JSONArray a ) throws Exception {
		List<int[]> out = new ArrayList<>();
		if (a == null) return out;
		for (int i = 0; i < a.length(); i++){
			JSONObject e = a.getJSONObject( i );
			if (!"eh".equals( e.getString( "t" ) )) continue;
			JSONArray n = e.getJSONArray( "i" );
			int[] row = new int[n.length() + 1];
			row[0] = e.getInt( "k" );
			for (int k = 0; k < n.length(); k++) row[k + 1] = n.getInt( k );
			out.add( row );
		}
		return out;
	}

	@Test
	public void aGuestsHeroRevealsTheCurseOfItsOwnSword() throws Exception {
		Hero his = new Hero();
		his.HP = his.HT = 20;
		his.pos = EnchantFxTest.at( 7, 6 );
		TestParty.join( his );
		CharSprite s = new HeroSprite( his );
		rig.stage.add( s );
		s.place( his.pos );
		s.visible = true;
		Sword cursed = enchanted( new Annoying() );
		cursed.cursed = true;
		cursed.cursedKnown = false;
		cursed.levelKnown = false;
		his.belongings.backpack.items.add( cursed );
		final Item[] got = new Item[1];
		int[] r = made( () -> got[0] = cursed.identify(), 30 );
		assertSame( cursed, got[0] );
		assertTrue( "the hex at his hero: " + r[0], r[0] >= 6 );
		List<int[]> ev = events( NetVisuals.drainEvents() );
		assertEquals( 1, ev.size() );
		assertArrayEquals( new int[]{ EnchantNet.REVEAL, GainFx.CURSE, EnchantLooks.of( Annoying.class ).code, his.id() }, ev.get( 0 ) );
	}

	@Test
	public void gainsAndRevealsGoOutAndPlayOnTheGuest() throws Exception {
		TestParty.join( new Hero() );
		EnchantLook blazing = EnchantLooks.of( Blazing.class ), annoying = EnchantLooks.of( Annoying.class );
		Sword sword = enchanted( new Blazing() );
		Enchanting.show( hero, sword );
		Sword cursed = enchanted( new Annoying() );
		cursed.cursed = true;
		cursed.cursedKnown = false;
		hero.belongings.backpack.items.add( cursed );
		cursed.identify();
		rig.frames( 75, true );
		List<int[]> ev = events( NetVisuals.drainEvents() );
		int hid = hero.id();
		assertArrayEquals( new int[]{ EnchantNet.GAIN, blazing.code, hid, sword.image(), sword.glowing().color }, ev.get( 0 ) );
		assertArrayEquals( new int[]{ EnchantNet.REVEAL, GainFx.CURSE, annoying.code, hid }, ev.get( 1 ) );

		//the guest: his own sprite for the host's hero
		TestParty.guest();
		heard.clear();
		CharSprite mine = rig.rat( EnchantFxTest.at( 3, 3 ) );
		Map<Integer, CharSprite> his = new HashMap<>();
		his.put( hid, mine );
		for (int[] e : ev) EnchantNet.play( e[0], java.util.Arrays.copyOfRange( e, 1, e.length ), null, his::get );
		assertEquals( "the ceremony over his sprite", 1, shown().size() );
		rig.frames( 75, true );
		assertTrue( "the curse's sting and the ceremony's: " + heard,
				heard.contains( Assets.Sounds.CURSED ) && heard.contains( blazing.sound ) );
		assertNull( "nothing sent on", NetVisuals.drainEvents() );

		//a newer look or a short payload plays nothing
		heard.clear();
		EnchantNet.play( EnchantNet.GAIN, new int[]{ 9999, hid, 0, -1 }, null, his::get );
		EnchantNet.play( EnchantNet.REVEAL, new int[]{ GainFx.CURSE }, null, his::get );
		rig.frames( 2, true );
		assertTrue( shown().isEmpty() );
		assertTrue( heard.isEmpty() || !heard.contains( Assets.Sounds.CURSED ) );
	}
}
