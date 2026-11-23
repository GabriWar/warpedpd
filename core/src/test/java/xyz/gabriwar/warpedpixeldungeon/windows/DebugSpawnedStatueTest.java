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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ArmoredStatue;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.watabou.noosa.Group;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertNotNull;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * A statue spawned from the debug menu's mob picker came bare: a statue is handed its weapon (and
 * an armored one its armor) where it is made (Statue.random, the traps), not by its constructor,
 * so the hero's first blow at an armored one asked its missing armor for its evasion and crashed
 * the game (1 report, from a build with the debug menu on). The picker arms them as they are made.
 */
public class DebugSpawnedStatueTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;

	@Before
	public void up(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		Actor.clear();
		Dungeon.level.mobs = new LinkedHashSet<>();
		PathFinder.setMapSize( Dungeon.level.width(), Dungeon.level.height() );
	}

	@After
	public void down(){
		Actor.clear();
		rig.down();
	}

	private static Hero hero( int cell ){
		Hero h = new Hero();
		h.HP = h.HT = 9999;
		h.STR = 20;
		h.damageInterrupt = false;
		h.pos = cell;
		CharSprite s = new RatSprite();
		s.ch = h;
		h.sprite = s;
		new Group().add( s );
		s.place( cell );
		Dungeon.hero = h;
		Actor.add( h );
		return h;
	}

	//as WndDebugPicker.selectCellForMob puts it down
	private <M extends Mob> M spawned( Class<M> cls, int cell ){
		M m = cls.cast( WndDebugPicker.bare( cls ) );
		m.pos = cell;
		m.state = m.debugSpawnState();
		//enough to last the blows here (a death drops loot, which wants a scene), and the hero in
		//sight (a surprise blow counts toward a badge, which wants the global badges loaded)
		m.HP = m.HT = 100000;
		try {
			Field seen = Mob.class.getDeclaredField( "enemySeen" );
			seen.setAccessible( true );
			seen.setBoolean( m, true );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
		CharSprite s = m.sprite();
		s.ch = m;
		m.sprite = s;
		rig.stage.add( s );
		s.place( cell );
		Actor.add( m );
		Dungeon.level.mobs.add( m );
		return m;
	}

	@Test
	public void aSpawnedArmoredStatueTakesTheHerosBlows(){
		Hero h = hero( at( 5, 6 ) );
		ArmoredStatue s = spawned( ArmoredStatue.class, at( 6, 6 ) );
		//its armor's random glyph off: a glyph's effects want a game scene
		if (s.armor() != null) s.armor().inscribe( null );
		for (int i = 0; i < 5; i++) h.attack( s );
		assertNotNull( s.armor() );
		assertNotNull( s.weapon() );
	}

	@Test
	public void aSpawnedStatueTakesTheHerosBlows(){
		Hero h = hero( at( 5, 6 ) );
		Statue s = spawned( Statue.class, at( 6, 6 ) );
		for (int i = 0; i < 5; i++) h.attack( s );
		assertNotNull( s.weapon() );
	}
}
