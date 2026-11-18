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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DoubleStab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.TargetHealthIndicator;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tengu's first phase ends when a hit takes him to half health: the floor turns to its pause map
 * and the second phase's arena is built when the hero walks to the top of the hallway. A Shadow Link
 * (Double Stab) on a lone Tengu binds him to himself, and his own blow's echo lands inside that
 * blow's damage call; the echo and the blow each ended the phase, so progress() ran twice and built
 * the arena at once, round a hero still in Tengu's cell, in its walls.
 */
public class TenguPhaseChangeTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private TargetHealthIndicator savedIndicator;

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		savedIndicator = TargetHealthIndicator.instance;
		Actor.clear();
	}

	@After
	public void tearDown(){
		Actor.clear();
		TargetHealthIndicator.instance = savedIndicator;
		rig.down();
		if (Dungeon.level != null) PathFinder.setMapSize( Dungeon.level.width(), Dungeon.level.height() );
	}

	private static Field field( String name ){
		try {
			Field f = PrisonBossLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			return f;
		} catch (NoSuchFieldException e){
			throw new AssertionError( e );
		}
	}

	//the prison's boss floor as Level.create builds it, without its loot, with each phase change
	//it is asked for kept (the state it was asked in)
	private static class Prison extends PrisonBossLevel {
		final ArrayList<State> progressed = new ArrayList<>();
		@Override
		public void progress(){
			progressed.add( state() );
			super.progress();
		}
	}

	private static Prison prison(){
		Prison l = new Prison();
		l.transitions = new ArrayList<>();
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.build();
		l.createMobs();
		l.buildFlagMaps();
		Dungeon.level = l;
		return l;
	}

	@Test(timeout = 20000)
	public void aShadowLinkedHitEndsTheFirstPhaseOnce() throws IllegalAccessException {
		Dungeon.depth = 10;
		Prison l = prison();
		//the health bar over a struck foe, which the phase change lets go of
		new TargetHealthIndicator();

		Hero hero = new Hero();
		hero.pos = 10 + 25 * l.width(); //in Tengu's cell, two rows in from its door
		Dungeon.hero = hero;
		Actor.add( hero );

		//phase 1 as progress() starts it (the floor sealed, Tengu in the middle of his cell), with a
		//sprite that only keeps still: the puff and the music are the scene's
		Tengu tengu = (Tengu) field( "tengu" ).get( l );
		CharSprite s = new CharSprite(){
			@Override public Emitter emitter(){ return new Emitter(); }
			@Override public void showStatusWithIcon( int color, String text, int icon, Object... args ){ }
		};
		s.ch = tengu;
		tengu.sprite = s;
		l.seal();
		tengu.state = tengu.HUNTING;
		tengu.pos = 10 + 27 * l.width();
		l.mobs.add( tengu );
		Actor.add( tengu );
		field( "state" ).set( l, PrisonBossLevel.State.FIGHT_START );
		assertFalse( l.solid[hero.pos] );

		//alone in his cell, Tengu is the only foe a Shadow Link can find: it binds him to himself
		//(DoubleStab.automaticPartner), and 70% of each hit echoes back onto him
		DoubleStab.link( tengu, tengu, 3 );
		tengu.HP = tengu.HT / 2 + 10;

		try {
			tengu.damage( 20, hero );
		} finally {
			//read even if the hit threw: off a scene the arena cannot be built (no cell selector to
			//stop the hero's keys, no emitter for Tengu's puff)
			assertEquals( "one hit ends phase 1 once; a second progress() builds the arena round the hero",
					Collections.singletonList( PrisonBossLevel.State.FIGHT_START ), l.progressed );
		}
		assertEquals( PrisonBossLevel.State.FIGHT_PAUSE, l.state() );
		assertFalse( "the hero stands on the floor of Tengu's cell", l.solid[hero.pos] );
		assertFalse( "Tengu waits off the floor for the second phase", l.mobs.contains( tengu ) );
		assertEquals( tengu.HT / 2, tengu.HP );
	}
}
