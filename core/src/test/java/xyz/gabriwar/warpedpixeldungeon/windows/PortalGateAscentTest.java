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
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.Portals;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate;
import xyz.gabriwar.warpedpixeldungeon.items.Amulet;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.watabou.utils.Signal;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Carrying the Amulet of Yendor up the dungeon holds the hero to its stairs: Lloyd's Beacon, the
 * Scroll of Passage, fadeleaf and the rest refuse to take him to another floor
 * (Dungeon.interfloorTeleportAllowed). The shops' portal gates did not ask, and since the
 * shopkeepers no longer flee the climb (taking their gates with them) a lit gate carried him
 * between floors all the way up. A gate now refuses the trip as the others do, with the same
 * warning, and takes no hunger for it.
 */
public class PortalGateAscentTest {

	private static final int SHOP6 = Portals.key( 6, 0 ), SHOP11 = Portals.key( 11, 0 );

	private long seed;
	private int depth, branch;
	private Level level;
	private boolean amuletObtained;
	private InterlevelScene.Mode mode;
	private int returnDepth, returnBranch, returnPos;
	private Application app;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		seed = Dungeon.seed; depth = Dungeon.depth; branch = Dungeon.branch; level = Dungeon.level;
		amuletObtained = Statistics.amuletObtained;
		mode = InterlevelScene.mode;
		returnDepth = InterlevelScene.returnDepth;
		returnBranch = InterlevelScene.returnBranch;
		returnPos = InterlevelScene.returnPos;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.seed = 0xA5CE27L;
		Dungeon.init();
		Portals.reset();
		InterlevelScene.mode = InterlevelScene.Mode.NONE;

		//a gate's trip is made on the render thread (Game.runOnRenderThread): headless, at once
		app = Gdx.app;
		final Application inner = app;
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(),
				new Class<?>[]{ Application.class }, (proxy, m, args) -> {
					if (m.getName().equals( "postRunnable" )){
						((Runnable) args[0]).run();
						return null;
					}
					return m.invoke( inner, args );
				} );
	}

	@After
	public void tearDown(){
		Gdx.app = app;
		NetManager.setPendingRelocationAnchor( -1 );
		Portals.reset();
		Statistics.amuletObtained = amuletObtained;
		Dungeon.seed = seed; Dungeon.depth = depth; Dungeon.branch = branch; Dungeon.level = level;
		InterlevelScene.mode = mode;
		InterlevelScene.returnDepth = returnDepth;
		InterlevelScene.returnBranch = returnBranch;
		InterlevelScene.returnPos = returnPos;
		Actor.clear();
	}

	//the hero beside the lit gate of depth 6's shop, depth 11's gate lit and known
	private static Hero atTheGate(){
		Dungeon.depth = 6; Dungeon.branch = 0;
		Actor.clear();
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( 32, 32 );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		Dungeon.level = l;

		int gate = 10 + 10 * l.width();
		Portals.register( SHOP6, gate );
		Portals.discover( SHOP6 );
		Portals.register( SHOP11, 12 + 12 * l.width() );
		Portals.setState( SHOP11, PortalGate.State.ACTIVE );
		Portals.discover( SHOP11 );
		assertEquals( "the first shop's gate is lit", PortalGate.State.ACTIVE, Portals.getState( SHOP6 ) );

		Hero hero = Dungeon.hero;
		hero.pos = gate + 1;
		return hero;
	}

	private static ArrayList<String> logged( Runnable run ){
		final ArrayList<String> lines = new ArrayList<>();
		Signal.Listener<String> l = lines::add;
		GLog.update.add( l );
		try {
			run.run();
		} finally {
			GLog.update.remove( l );
		}
		return lines;
	}

	@Test
	public void aGateRefusesTheAmuletsClimb(){
		final Hero hero = atTheGate();
		//taken on 26 and carried up, cursed
		Statistics.amuletObtained = true;
		hero.belongings.backpack.items.add( new Amulet() );
		Buff.affect( hero, AscensionChallenge.class );
		assertFalse( "no trip between floors now, as the beacon and the scrolls know",
				Dungeon.interfloorTeleportAllowed() );

		float hunger = hero.buff( Hunger.class ).hunger();
		ArrayList<String> log = logged( () -> WndPortal.hostTravel( hero, SHOP11 ) );

		assertEquals( "the gate takes him nowhere", InterlevelScene.Mode.NONE, InterlevelScene.mode );
		assertEquals( 6, Dungeon.depth );
		assertEquals( "and takes no hunger for it", hunger, hero.buff( Hunger.class ).hunger(), 0.001f );
		assertTrue( "with the warning the other trips give",
				log.contains( GLog.WARNING + Messages.get( ScrollOfTeleportation.class, "no_tele" ) ) );
	}

	@Test
	public void aGateCarriesTheHeroWithoutTheAmulet(){
		Hero hero = atTheGate();
		assertTrue( Dungeon.interfloorTeleportAllowed() );

		float hunger = hero.buff( Hunger.class ).hunger();
		WndPortal.hostTravel( hero, SHOP11 );

		assertEquals( InterlevelScene.Mode.RETURN, InterlevelScene.mode );
		assertEquals( 11, InterlevelScene.returnDepth );
		assertEquals( 0, InterlevelScene.returnBranch );
		assertEquals( "a trip costs 150 hunger", hunger + 150, hero.buff( Hunger.class ).hunger(), 0.001f );
	}
}
