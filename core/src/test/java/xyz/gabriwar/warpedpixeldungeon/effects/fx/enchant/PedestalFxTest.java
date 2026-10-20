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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.EnchantingScene;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The enchanting pedestal's own pictures (PedestalFx), headless: a whole visit at once - three
 * weaves revealed (their gems igniting and keeping their magic alive), one taken (its gem's burst
 * and its comet to the pedestal), the piece unwoven (every ring mote leaking), the vestige's echo -
 * never has more than CAP motes alive, never draws the game's Random, never counts in the game's
 * FxBudget; a comet lands where it was sent when it says; the ring's motion is a pure, bounded
 * function.
 */
public class PedestalFxTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private Group scene, window;
	private int most;
	private boolean landed;

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		scene = new PedestalFx();
		window = new PedestalFx();
		rig.stage.add( scene );
		rig.stage.add( window );
		most = 0;
		landed = false;
	}

	@After
	public void tearDown(){
		scene.destroy();
		window.destroy();
		rig.down();
	}

	private PedestalFx scene(){
		return (PedestalFx) scene;
	}

	private PedestalFx window(){
		return (PedestalFx) window;
	}

	private void frames( int n ){
		for (int i = 0; i < n; i++){
			rig.frames( 1, true );
			most = Math.max( most, PedestalFx.live() );
			assertEquals( "the count is the motes alive", alive( scene ) + alive( window ), PedestalFx.live() );
		}
	}

	private static int alive( Group g ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m instanceof PedestalFx.Mote && m.alive) n++;
			else if (m instanceof Group && m.alive) n += alive( (Group) m );
		}
		return n;
	}

	//a whole visit, every moment over the last: three weaves revealed and alive, one taken and
	//flying in, the piece unwoven, the vestige's sign
	private void visit(){
		EnchantLook[] three = { EnchantLooks.of( Blazing.class ), EnchantLooks.of( Chilling.class ), EnchantLooks.of( Vampiric.class ) };
		Object[] cards = { new Object(), new Object(), new Object() };
		for (int i = 0; i < 3; i++){
			window().burst( 40 + 30 * i, 60, three[i], PedestalFx.IGNITE, 18f );
			frames( 4 );
		}
		for (int k = 0; k < 4; k++){
			for (int i = 0; i < 3; i++) window().motif( three[i], 40 + 30 * i, 60, cards[i] );
			frames( 24 );
		}
		scene().burst( 70, 60, three[1], PedestalFx.TAKEN, 30f );
		scene().comet( 70, 60, 120, 120, three[1], () -> landed = true );
		frames( 6 );
		//the unweave, while the comet still flies: 26 ring motes leaking
		for (int i = 0; i < 26; i++){
			double a = i * Math.PI * 2 / 26;
			scene().leak( 120 + (float)Math.cos( a ) * 30, 120 + (float)Math.sin( a ) * 30, 0x9966FF,
					(float)Math.cos( a ), (float)Math.sin( a ) );
		}
		scene().echoRing( 120, 140, 2f );
		scene().glint( 100, 150, 0xFF4400 );
		frames( 60 );
	}

	@Test
	public void aWholeVisitKeepsToTheCap(){
		visit();
		assertTrue( "it drew: " + most, most >= 20 );
		assertTrue( "at most " + PedestalFx.CAP + " at once, drew " + most, most <= PedestalFx.CAP );
		assertTrue( "the comet landed", landed );
		frames( 30 );
		assertEquals( "all gone", 0, PedestalFx.live() );
	}

	@Test
	public void itDrawsNothingOfTheGamesRandomNorCountsInTheGamesBudget(){
		int particles = FxBudget.live( FxBudget.PARTICLES );
		int k = EnchantFxTest.draws( this::visit );
		assertEquals( "the game's Random untouched", 0, k );
		assertEquals( "outside the game's budget", particles, FxBudget.live( FxBudget.PARTICLES ) );
	}

	@Test
	public void aCometLandsWhereItWasSentWhenItSays(){
		final float[] at = new float[1];
		scene().comet( 20, 20, 140, 100, EnchantLooks.of( Thorns.class ), () -> at[0] = Game.timeTotal );
		float start = Game.timeTotal;
		PedestalFx.Mote head = null;
		for (Gizmo g : scene().membersView()) if (g instanceof PedestalFx.Mote && g.alive){ head = (PedestalFx.Mote) g; break; }
		assertTrue( head != null );
		float lastX = head.x;
		for (int i = 0; i < 11; i++){
			rig.frames( 1, true );
			if (head.alive){
				assertTrue( "it flies on toward the pedestal", head.x >= lastX - 0.5f );
				lastX = head.x;
			}
		}
		assertTrue( "near the pedestal as it lands: " + lastX, lastX > 120 );
		rig.frames( 3, true );
		assertTrue( "landed after its flight: " + (at[0] - start), at[0] - start >= PedestalFx.COMET_FOR - 0.01f
				&& at[0] - start <= PedestalFx.COMET_FOR + 0.1f );
	}

	@Test
	public void theRingsMotionIsPureAndBounded(){
		float[] a = new float[4], b = new float[4];
		for (Family f : Family.values()){
			for (int i = 0; i < 26; i++){
				for (float t = 0; t < 12f; t += 0.37f){
					PedestalFx.motion( f, t, i, a );
					PedestalFx.motion( f, t, i, b );
					assertArrayEquals( f + " pure", a, b, 0f );
					assertTrue( f + " moves a few pixels", Math.abs( a[0] ) <= 3f && Math.abs( a[1] ) <= 3f && Math.abs( a[2] ) <= 3f );
					assertTrue( f + " alpha", a[3] >= 0f && a[3] <= 1.25f );
				}
			}
		}
		PedestalFx.motion( null, 3f, 2, a );
		assertArrayEquals( new float[]{ 0, 0, 0, 1 }, a, 0f );
	}

	// ------------------------------------------------------------------ the scene's wiring

	private static Object field( Object o, String name ) throws Exception {
		Field f = o.getClass().getDeclaredField( name );
		f.setAccessible( true );
		return f.get( o );
	}

	private static void set( Object o, String name, Object v ) throws Exception {
		Field f = o.getClass().getDeclaredField( name );
		f.setAccessible( true );
		f.set( o, v );
	}

	private static Object call( Object o, String name, Class<?>[] types, Object... args ) throws Exception {
		Method m = (o instanceof Class ? (Class<?>) o : o.getClass()).getDeclaredMethod( name, types );
		m.setAccessible( true );
		return m.invoke( o instanceof Class ? null : o, args );
	}

	private static Class<?> nested( String name ) throws Exception {
		return Class.forName( EnchantingScene.class.getName() + "$" + name );
	}

	private static Object make( String name, Class<?>[] types, Object... args ) throws Exception {
		Constructor<?> c = nested( name ).getDeclaredConstructor( types );
		c.setAccessible( true );
		return c.newInstance( args );
	}

	//the pedestal scene as far as a weave's landing needs it: its effects, its ring, its middle
	private EnchantingScene pedestal( Object halo ) throws Exception {
		Field t = EnchantingScene.class.getDeclaredField( "target" );
		t.setAccessible( true );
		t.set( null, null );
		EnchantingScene es = new EnchantingScene();
		set( es, "fx", scene() );
		set( es, "halo", halo );
		set( es, "px", 120f );
		set( es, "py", 120f );
		return es;
	}

	private static final Class<?>[] COLORS = { int[].class, Family[].class, int[].class };
	private static final Class<?>[] LANDING = { EnchantLook.class, PointF.class, int.class, boolean.class };

	@Test
	public void theRingWaitsForTheLastCometToLand() throws Exception {
		Object halo = make( "Halo", new Class<?>[0] );
		rig.stage.add( (Gizmo) halo );
		EnchantingScene es = pedestal( halo );
		int[] violet = ((int[]) field( halo, "shown" )).clone();

		//two weaves taken 0.2 s apart; the piece's new weaves told to the ring at once
		call( es, "landing", LANDING, EnchantLooks.of( Blazing.class ), new PointF( 40, 40 ), 0, false );
		frames( 6 );
		call( es, "landing", LANDING, EnchantLooks.of( Chilling.class ), new PointF( 60, 40 ), 1, false );
		int[] two = { 0xFF4400, 0x66CCFF };
		call( halo, "colors", COLORS, two, new Family[]{ Family.EMBER, Family.RIME }, new int[]{ 0, 0 } );
		assertArrayEquals( "held while they fly", violet, (int[]) field( halo, "shown" ) );

		//the first lands: still held for the second
		frames( 8 );
		assertEquals( 1, field( es, "comets" ) );
		assertTrue( (Boolean) field( halo, "held" ) );
		assertArrayEquals( "held for the second", violet, (int[]) field( halo, "shown" ) );

		//the second lands: the ring takes what it was told
		frames( 8 );
		assertEquals( 0, field( es, "comets" ) );
		assertTrue( !(Boolean) field( halo, "held" ) );
		assertArrayEquals( two, (int[]) field( halo, "shown" ) );
		assertArrayEquals( new Family[]{ Family.EMBER, Family.RIME }, (Family[]) field( halo, "fams" ) );

		//told with nothing flying: at once
		call( halo, "colors", COLORS, null, null, null );
		assertArrayEquals( violet, (int[]) field( halo, "shown" ) );
	}

	@Test
	public void anEmpoweredSocketsMotesRunOneLapAheadThenFallBackInPlace() throws Exception {
		Object[] halos = new Object[2];
		for (int h = 0; h < 2; h++){
			halos[h] = make( "Halo", new Class<?>[0] );
			call( halos[h], "center", new Class<?>[]{ float.class, float.class }, 120f, 120f );
			call( halos[h], "colors", COLORS, new int[]{ 0xFF4400, 0x66CCFF }, new Family[]{ Family.EMBER, Family.RIME }, new int[]{ 1, 2 } );
			rig.stage.add( (Gizmo) halos[h] );
		}
		//a level brightens its colour a tenth a level
		int[] shown = (int[]) field( halos[0], "shown" );
		assertTrue( shown[0] != 0xFF4400 && shown[1] != 0x66CCFF );
		frames( 1 );
		call( halos[1], "lap", new Class<?>[]{ int.class }, 1 );
		ColorBlock[] calm = (ColorBlock[]) field( halos[0], "outer" ), lapping = (ColorBlock[]) field( halos[1], "outer" );
		frames( 15 );
		//the outer circle alternates the two sockets' motes: the second's (odd) are ahead, brighter
		assertEquals( "the first socket's in place", calm[0].x, lapping[0].x, 0.01f );
		assertTrue( "the second socket's ahead", Math.abs( calm[1].x - lapping[1].x ) + Math.abs( calm[1].y - lapping[1].y ) > 1f );
		assertTrue( "and brighter", lapping[1].alpha() >= calm[1].alpha() );
		//one whole lap, then back where it was
		frames( 30 );
		for (int i = 0; i < calm.length; i++){
			assertEquals( "mote " + i, calm[i].x, lapping[i].x, 0.01f );
			assertEquals( "mote " + i, calm[i].y, lapping[i].y, 0.01f );
		}
	}

	@Test
	public void aRevealedCardIgnitesThenKeepsAtMostThreeMotesOfItsOwn() throws Exception {
		Object card = make( "Card", new Class<?>[]{ int.class }, 0xFF4400 );
		call( card, "gem", new Class<?>[]{ int.class, boolean.class }, 0xFF4400, false );
		((com.watabou.noosa.ui.Component) card).setRect( 30, 30, 80, 40 );
		rig.stage.add( (Gizmo) card );
		call( card, "live", new Class<?>[]{ PedestalFx.class, EnchantLook.class, float.class },
				window(), EnchantLooks.of( Blazing.class ), 0.3f );
		frames( 6 );
		assertEquals( "dark till it ignites", 0, PedestalFx.live() );
		frames( 6 );
		assertTrue( "its burst as it ignites: " + PedestalFx.live(), PedestalFx.live() >= 4 );
		int mostOwn = 0;
		for (int f = 0; f < 600; f++){
			frames( 1 );
			int own = 0;
			for (Gizmo g : window.membersView()) if (g instanceof PedestalFx.Mote && g.alive && ((PedestalFx.Mote) g).owner == card) own++;
			mostOwn = Math.max( mostOwn, own );
		}
		assertTrue( "it keeps its magic alive", mostOwn >= 1 );
		assertTrue( "at most three of its own: " + mostOwn, mostOwn <= 3 );
	}

	@Test
	public void aWindowsPointIsWhereItShowsOnTheScene() throws Exception {
		Camera main = Camera.main;
		try {
			Camera.main = new Camera( 0, 0, 160, 90, 2 );
			Camera win = new Camera( 40, 30, 100, 80, 2 );
			win.scroll.set( -5, -5 );
			com.watabou.noosa.ui.Component c = new com.watabou.noosa.ui.Component();
			c.camera = win;
			PointF at = (PointF) call( EnchantingScene.class, "onScene",
					new Class<?>[]{ com.watabou.noosa.ui.Component.class, PointF.class }, c, new PointF( 10, 10 ) );
			//(10 + 5) * 2 + 40 = 70 on the screen, 35 on the scene; (10 + 5) * 2 + 30 = 60, 30
			assertEquals( 35f, at.x, 0.01f );
			assertEquals( 30f, at.y, 0.01f );
		} finally {
			Camera.main = main;
		}
	}
}
