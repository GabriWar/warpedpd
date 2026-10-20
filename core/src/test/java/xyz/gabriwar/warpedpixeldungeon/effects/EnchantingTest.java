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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyWeapon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.ArcImage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.Family;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.GainFx;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Enchanting v2, headless (the enchantments' stand-in stage): shown from another thread it posts
 * itself; the two-argument form takes the item's own look (its first slot's, a known curse's) and
 * keeps today's picture for an item of no magic or under HolyWeapon's yellow; it keeps to its caps
 * (PEAK at once, SETTLE from the bind on, one light besides a finale's own) for every family, a
 * curse and a glyph; a curse runs the other way (its runes leave, the ghost darkens, CURSED); and
 * none of it draws the game's Random.
 */
public class EnchantingTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private final List<String> heard = new ArrayList<>();

	@Before
	public void setUp() throws Exception {
		rig = new EnchantFxTest.Rig();
		rig.up();
		heard.clear();
		ear();
	}

	@After
	public void tearDown(){
		Dungeon.hero = null;
		rig.down();
	}

	//every sting played goes to `heard` (Sting.out is the enchantments' package's own: set by hand)
	private void ear() throws Exception {
		Class<?> sting = Class.forName( "xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.Sting" );
		Class<?> out = Class.forName( "xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.Sting$Out" );
		Object ear = Proxy.newProxyInstance( out.getClassLoader(), new Class<?>[]{ out }, (p, m, a) -> {
			if (m.getName().equals( "play" )) heard.add( (String) a[0] );
			return null;
		} );
		Field f = sting.getDeclaredField( "out" );
		f.setAccessible( true );
		f.set( null, ear );
	}

	private CharSprite owner(){
		return rig.rat( EnchantFxTest.at( 5, 6 ) );
	}

	private static Sword sword( Object... magic ){
		Sword s = new Sword();
		for (Object m : magic) s.addEnchant( (xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon.Enchantment) m );
		return s;
	}

	private List<Enchanting> shown(){
		List<Enchanting> out = new ArrayList<>();
		for (Gizmo g : rig.stage.membersView()) if (g instanceof Enchanting && g.alive) out.add( (Enchanting) g );
		return out;
	}

	private static Object field( Object o, String name ) throws Exception {
		Field f = Enchanting.class.getDeclaredField( name );
		f.setAccessible( true );
		return f.get( o );
	}

	private static boolean bound( Enchanting e ) throws Exception {
		return "FADE_OUT".equals( String.valueOf( field( e, "phase" ) ) );
	}

	/** A ceremony run to its end: the most quads alive at once, the quads made from the bind on,
	 *  the most lights at once. */
	private int[] run( CharSprite s, Item item, EnchantLook gained ) throws Exception {
		IdentityHashMap<Gizmo, Boolean> prev = new IdentityHashMap<>(), now = new IdentityHashMap<>();
		rig.alive( prev );
		int lights0 = FxLight.live();
		Enchanting.show( s.ch, item, gained );
		Enchanting e = shown().get( 0 );
		int most = 0, after = 0, lights = 0;
		boolean wasBound = false;
		for (int f = 0; f < 75; f++){
			rig.frames( 1, true );
			now.clear();
			rig.alive( now );
			most = Math.max( most, now.size() );
			boolean b = wasBound || (e.alive && bound( e ));
			if (b) for (Gizmo g : now.keySet()) if (!prev.containsKey( g ) && g != e) after++;
			wasBound = b;
			lights = Math.max( lights, FxLight.live() - lights0 );
			IdentityHashMap<Gizmo, Boolean> t = prev;
			prev = now;
			now = t;
		}
		assertTrue( "the ceremony ends", shown().isEmpty() );
		return new int[]{ most, after, lights };
	}

	//the most lights a family's finale throws at Enchanting's scale on its own
	private int finaleLights( EnchantLook l ){
		int lights0 = FxLight.live(), most = 0;
		EnchantFx.finale( l, 80, 90, Enchanting.FINALE_SCALE );
		for (int f = 0; f < 30; f++){
			rig.frames( 1, true );
			most = Math.max( most, FxLight.live() - lights0 );
		}
		return most;
	}

	// ------------------------------------------------------------------ threads

	@Test
	public void shownFromAnotherThreadItPostsItself() throws Exception {
		final CharSprite s = owner();
		final Sword sword = sword( new Blazing() );
		final List<Runnable> posted = new ArrayList<>();
		Application saved = Gdx.app;
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(), new Class<?>[]{ Application.class },
				(p, m, args) -> {
					if (m.getName().equals( "postRunnable" )){
						synchronized (posted){
							posted.add( (Runnable) args[0] );
						}
						return null;
					}
					return saved != null ? m.invoke( saved, args ) : null;
				} );
		try {
			Thread actor = new Thread( () -> Enchanting.show( s.ch, sword ) );
			actor.start();
			actor.join();
			assertTrue( "nothing added off the render thread", shown().isEmpty() );
			assertEquals( "the ceremony, posted", 1, posted.size() );
			for (Runnable r : posted) r.run();
		} finally {
			Gdx.app = saved;
		}
		assertEquals( 1, shown().size() );
		assertSame( EnchantLooks.of( Blazing.class ), shown().get( 0 ).look() );
	}

	// ------------------------------------------------------------------ which look

	@Test
	public void theTwoArgumentFormTakesTheItemsOwnLook(){
		assertSame( "its one enchantment", EnchantLooks.of( Blazing.class ), GainFx.gainLook( sword( new Blazing() ) ) );
		assertSame( "two: the first slot's", EnchantLooks.of( Blazing.class ), GainFx.gainLook( sword( new Blazing(), new Shocking() ) ) );
		Sword cursed = sword( new Annoying(), new Chilling() );
		cursed.cursedKnown = false;
		assertSame( "a curse not known yet: the good one", EnchantLooks.of( Chilling.class ), GainFx.gainLook( cursed ) );
		cursed.cursedKnown = true;
		assertSame( "a known curse, its newest: the curse's", EnchantLooks.of( Annoying.class ), GainFx.gainLook( cursed ) );
		ClothArmor cloth = new ClothArmor();
		cloth.inscribe( new Thorns() );
		assertSame( "a glyph", EnchantLooks.of( Thorns.class ), GainFx.gainLook( cloth ) );
		assertNull( "no magic: today's picture", GainFx.gainLook( new Sword() ) );

		//HolyWeapon's yellow laid over an enchanted weapon keeps today's (yellow) picture
		Hero h = new Hero();
		Dungeon.hero = h;
		Sword holy = sword( new Blazing() );
		h.belongings.weapon = holy;
		Buff.affect( h, HolyWeapon.HolyWepBuff.class, 50f );
		assertEquals( 0xFFFF00, holy.glowing().color );
		assertNull( "the holy weapon's yellow: today's", GainFx.gainLook( holy ) );
		assertSame( "the look it is given still wins", EnchantLooks.of( Blazing.class ),
				gainedOf( holy, EnchantLooks.of( Blazing.class ) ) );
	}

	private EnchantLook gainedOf( Item item, EnchantLook l ){
		CharSprite s = owner();
		Enchanting.show( s.ch, item, l );
		List<Enchanting> e = shown();
		return e.get( e.size() - 1 ).look();
	}

	@Test
	public void anItemOfNoMagicKeepsTodaysPicture() throws Exception {
		CharSprite s = owner();
		int[] r = run( s, new Sword(), null );
		assertTrue( "today's at most " + Enchanting.DRAWS + " at once, drew " + r[0], r[0] <= Enchanting.DRAWS );
		assertTrue( "today's one glow", r[2] <= 1 );
		//its runes are today's
		Enchanting.show( s.ch, new Sword() );
		Enchanting e = shown().get( 0 );
		assertNull( e.look() );
		rig.frames( 3, true );
		ArcImage[] runes = (ArcImage[]) field( e, "runes" );
		assertNotNull( runes );
		assertSame( FxFrames.RUNE_3[0], runes[0].shown() );
	}

	// ------------------------------------------------------------------ caps

	@Test
	public void everyFamilyKeepsToItsCaps() throws Exception {
		List<EnchantLook> looks = new ArrayList<>();
		for (Family f : Family.values()) looks.add( EnchantFxTest.face( f ) );
		looks.add( EnchantLooks.of( Annoying.class ) );
		looks.add( EnchantLooks.of( Thorns.class ) );
		for (EnchantLook l : looks){
			rig.layers( true );
			CharSprite s = owner();
			Item item = l.glyph ? new ClothArmor() : new Sword();
			int[] r = run( s, item, l );
			assertTrue( l + ": it shows, drew " + r[0], r[0] >= Enchanting.PEAK / 2 );
			assertTrue( l + ": at most " + Enchanting.PEAK + " at once, drew " + r[0], r[0] <= Enchanting.PEAK );
			assertTrue( l + ": from the bind on at most " + Enchanting.SETTLE + ", made " + r[1], r[1] <= Enchanting.SETTLE );
			//one light: the bind's, or the finale's own in its place (a drain's two heartbeat pools,
			//which the core's finale overlaps for a frame, are that finale's whatever plays it)
			int own = finaleLights( l );
			assertTrue( l + ": the finale's " + own + " light(s) stand for the bind's, threw " + r[2],
					r[2] <= Math.max( Enchanting.LIGHTS, own ) );
		}
	}

	@Test
	public void blazingChillingAndACurseThrowOneLight() throws Exception {
		Sword cursed = sword( new Annoying() );
		cursed.cursedKnown = true;
		for (Item item : new Item[]{ sword( new Blazing() ), sword( new Chilling() ), cursed }){
			rig.layers( true );
			int[] r = run( owner(), item, null );
			assertTrue( item + " threw " + r[2], r[2] <= Enchanting.LIGHTS );
			assertTrue( item + " drew " + r[0], r[0] <= Enchanting.PEAK );
		}
	}

	// ------------------------------------------------------------------ a curse

	@Test
	public void aCurseRunsTheOtherWay() throws Exception {
		CharSprite s = owner();
		Sword cursed = sword( new Annoying() );
		cursed.cursedKnown = true;
		Enchanting.show( s.ch, cursed );
		Enchanting e = shown().get( 0 );
		assertTrue( e.look().curse );
		rig.frames( 8, true );
		float early = spread( e );
		rig.frames( 10, true );
		float later = spread( e );
		assertTrue( "its runes leave the ghost: " + early + " -> " + later, later > early );
		float lightEarly = e.rm;
		rig.frames( 18, true );
		assertTrue( "it darkens toward ink: " + lightEarly + " -> " + e.rm, e.rm < lightEarly && e.rm < 0.4f );
		rig.frames( 10, true );
		assertTrue( "the CURSED sting: " + heard, heard.contains( Assets.Sounds.CURSED ) );
	}

	@Test
	public void aGoodOneStingsItsOwnSoundAtTheBind() throws Exception {
		CharSprite s = owner();
		Enchanting.show( s.ch, sword( new Blazing() ) );
		rig.frames( 30, true );
		assertTrue( "nothing before the bind: " + heard, heard.isEmpty() );
		rig.frames( 15, true );
		assertEquals( "its own sting: " + heard, 1, heard.size() );
		assertEquals( EnchantLooks.of( Blazing.class ).sound, heard.get( 0 ) );
	}

	//how far the runes stand from the ghost's middle (their mean), drawn ones only
	private static float spread( Enchanting e ) throws Exception {
		Image[] marks = (Image[]) field( e, "marks" );
		if (marks == null) return 0f;
		float mx = e.x + e.width() / 2f, my = e.y + e.height() / 2f + 6f, sum = 0f;
		for (Image m : marks){
			float dx = m.x + m.width / 2f - mx, dy = m.y + m.height / 2f - my;
			sum += (float)Math.sqrt( dx * dx + dy * dy );
		}
		return sum / marks.length;
	}

	// ------------------------------------------------------------------ the game's Random

	@Test
	public void noneOfItDrawsTheGamesRandom(){
		Sword cursed = sword( new Annoying() );
		cursed.cursedKnown = true;
		ClothArmor cloth = new ClothArmor();
		cloth.inscribe( new Thorns() );
		for (final Item item : new Item[]{ new Sword(), sword( new Blazing() ), sword( new Chilling(), new Shocking() ), cursed, cloth }){
			rig.layers( true );
			final CharSprite s = owner();
			int k = EnchantFxTest.draws( () -> {
				Enchanting.show( s.ch, item );
				rig.frames( 75, true );
			} );
			assertEquals( item + " drew the game's Random", 0, k );
		}
		//and every family, by its look
		for (Family f : Family.values()){
			rig.layers( true );
			final CharSprite s = owner();
			final EnchantLook l = EnchantFxTest.face( f );
			int k = EnchantFxTest.draws( () -> {
				Enchanting.show( s.ch, new Sword(), l );
				rig.frames( 75, true );
			} );
			assertEquals( f + " drew the game's Random", 0, k );
		}
		Game.elapsed = 1 / 30f;
	}
}
