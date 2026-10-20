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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.ItemGhost;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Projecting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingKnife;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import com.watabou.noosa.Gizmo;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Flights (EnchantTrail): an enchanted throw sheds at most eight motes; four full trails at once,
 * the later missiles the streak alone; a throw through a wall draws faint inside it and ripples
 * where it enters and leaves, melding in once; a throw past a wall's corner (its straight line of
 * pixels clipping the wall, its path of cells not) none of that; a boomerang's dots and ghosts
 * within their caps out and back; none of it draws the game's Random.
 */
public class EnchantTrailTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private final List<String> heard = new ArrayList<>();
	private HeapLook.Ear ear;

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		//a thrown weapon's enchanting tells the hero's set tracker
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = EnchantFxTest.at( 8, 12 );
		EnchantTrail.reset();
		heard.clear();
		ear = EnchantTrail.ear;
		EnchantTrail.ear = ( sample, cell, volume, pitch ) -> heard.add( sample );
	}

	@After
	public void tearDown(){
		EnchantTrail.ear = ear;
		EnchantTrail.reset();
		Dungeon.hero = null;
		rig.down();
	}

	private static Weapon enchanted( Weapon w, Weapon.Enchantment e ){
		if (e != null) w.enchant( e );
		return w;
	}

	private MissileSprite fly( int from, int to, Item item ){
		MissileSprite ms = new MissileSprite();
		rig.stage.add( ms );
		ms.reset( from, to, item, null );
		return ms;
	}

	//the ripples in the light layer: upright rings
	private int ripples(){
		int n = 0;
		for (Gizmo g : rig.layers[GameScene.Layer.LIGHTS.ordinal()].membersView()){
			if (g instanceof EnchantFx.Flash && g.alive && ((EnchantFx.Flash) g).angle == 90f) n++;
		}
		return n;
	}

	@Test
	public void eightShedsAFlightAndFourTrailsAtOnce(){
		List<MissileSprite> ms = new ArrayList<>();
		for (int row = 1; row <= 6; row++){
			ms.add( fly( EnchantFxTest.at( 1, row + 8 ), EnchantFxTest.at( 14, row + 8 ),
					enchanted( new ThrowingKnife(), new Blazing() ) ) );
		}
		rig.frames( 2, true );
		List<EnchantTrail> trails = new ArrayList<>();
		int full = 0;
		for (MissileSprite m : ms){
			EnchantTrail t = m.trail();
			assertNotNull( "each flight has its trail", t );
			trails.add( t );
			if (t.isFull()) full++;
		}
		assertEquals( "four full trails, the rest the streak alone", EnchantTrail.MOST_TRAILS, full );
		assertEquals( EnchantTrail.MOST_TRAILS, EnchantTrail.fullTrails() );
		for (int f = 0; f < 60; f++){
			rig.frames( 1, true );
			for (EnchantTrail t : trails) assertTrue( "sheds " + t.sheds(), t.sheds() <= EnchantTrail.MOST_SHEDS );
		}
		int shed = 0;
		for (EnchantTrail t : trails) shed = Math.max( shed, t.sheds() );
		assertEquals( "a long throw sheds its eight", EnchantTrail.MOST_SHEDS, shed );
		assertEquals( "every flight landed, every trail let go", 0, EnchantTrail.fullTrails() );
	}

	@Test
	public void aThrowThroughAWallFadesAndRipples(){
		//a wall across the room's seventh column
		for (int y = 0; y < EnchantFxTest.W; y++) Dungeon.level.solid[EnchantFxTest.at( 7, y )] = true;
		MissileSprite m = fly( EnchantFxTest.at( 3, 12 ), EnchantFxTest.at( 11, 12 ), enchanted( new ThrowingKnife(), new Projecting() ) );
		boolean faint = false;
		int most = 0;
		for (int f = 0; f < 60; f++){
			rig.frames( 1, true );
			if (m.trail() != null && m.trail().phasing()){
				faint = true;
				assertEquals( EnchantTrail.PHASED_ALPHA, m.phased(), 0f );
			}
			most = Math.max( most, ripples() );
		}
		assertTrue( "it drew faint inside the wall", faint );
		assertEquals( "back to itself once out", 1f, m.phased(), 0f );
		assertTrue( "a ripple where it entered and one where it left: " + most, most >= 1 && most <= 2 );
		assertEquals( "it melded in once", 1, heard.size() );
		assertEquals( Assets.Sounds.MELD, heard.get( 0 ) );

		//a plain throw that never meets a wall: none of it
		heard.clear();
		MissileSprite plain = fly( EnchantFxTest.at( 1, 14 ), EnchantFxTest.at( 5, 14 ), new ThrowingKnife() );
		for (int f = 0; f < 40; f++){
			rig.frames( 1, true );
			assertEquals( 1f, plain.phased(), 0f );
		}
		assertTrue( heard.isEmpty() );
	}

	@Test
	public void aThrowPastAWallsCornerDoesNotPhase(){
		//knight's moves: from (5,10) to (7,11) the path steps (6,11) and the pixels clip (6,10); back
		//from (7,14) to (5,13) the path steps (6,13) and the pixels clip (6,14)
		final int[] corners = { EnchantFxTest.at( 6, 10 ), EnchantFxTest.at( 6, 14 ) };
		for (int c : corners) Dungeon.level.solid[c] = true;
		MissileSprite[] ms = {
				fly( EnchantFxTest.at( 5, 10 ), EnchantFxTest.at( 7, 11 ), enchanted( new ThrowingKnife(), new Blazing() ) ),
				fly( EnchantFxTest.at( 7, 14 ), EnchantFxTest.at( 5, 13 ), new ThrowingKnife() ) };
		boolean[] overCorner = new boolean[ms.length];
		for (int f = 0; f < 40; f++){
			rig.frames( 1, true );
			for (int i = 0; i < ms.length; i++){
				MissileSprite m = ms[i];
				assertEquals( "never faint", 1f, m.phased(), 0f );
				if (m.trail() != null) assertTrue( !m.trail().phasing() );
				float cx = m.x + m.width() / 2f, cy = m.y + m.height() / 2f;
				if (xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx.cellAt( cx, cy + EnchantTrail.RAISE ) == corners[i]) overCorner[i] = true;
			}
			assertEquals( "no ripple", 0, ripples() );
		}
		for (int i = 0; i < ms.length; i++) assertTrue( "throw " + i + "'s pixels did cross its corner", overCorner[i] );
		assertTrue( "no meld: " + heard, heard.isEmpty() );
	}

	@Test
	public void aBoomerangsDotsAndGhostsKeepToTheirCaps(){
		for (boolean magic : new boolean[]{ false, true }){
			ItemGhost.reset();
			HeavyBoomerang rang = new HeavyBoomerang();
			if (magic) rang.enchant( new Blazing() );
			for (int leg = 0; leg < 2; leg++){
				MissileSprite m = leg == 0 ? fly( EnchantFxTest.at( 1, 13 ), EnchantFxTest.at( 14, 13 ), rang )
						: fly( EnchantFxTest.at( 14, 13 ), EnchantFxTest.at( 1, 13 ), rang );
				if (leg == 1) m.alpha( 0f );
				rig.frames( 1, true );
				EnchantTrail t = m.trail();
				assertNotNull( t );
				int most = 0;
				for (int f = 0; f < 60; f++){
					if (leg == 1) m.alpha( Math.min( 1f, m.alpha() + 0.1f ) );
					rig.frames( 1, true );
					assertTrue( "dots " + t.dots(), t.dots() <= EnchantTrail.MOST_DOTS );
					assertTrue( "ghosts " + t.ghosts(), t.ghosts() <= EnchantTrail.GHOSTS );
					most = Math.max( most, t.dots() );
					assertTrue( ItemGhost.live() <= ItemGhost.MOST );
				}
				assertTrue( "a dotted path", most > 0 );
				assertTrue( "ghosts along it", t.ghosts() > 0 );
			}
		}
	}

	@Test
	public void noneOfItDrawsTheGamesRandom(){
		for (int y = 0; y < EnchantFxTest.W; y++) Dungeon.level.solid[EnchantFxTest.at( 7, y )] = true;
		final Weapon knife = enchanted( new ThrowingKnife(), new Blazing() ), rang = enchanted( new HeavyBoomerang(), new Projecting() );
		int draws = EnchantFxTest.draws( () -> {
			fly( EnchantFxTest.at( 3, 12 ), EnchantFxTest.at( 11, 12 ), knife );
			fly( EnchantFxTest.at( 1, 13 ), EnchantFxTest.at( 14, 13 ), rang );
			rig.frames( 60, true );
		} );
		assertEquals( 0, draws );
	}
}
