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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.PathFinder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A co-op client sees the host's sleepers asleep: the host ships each mob's sleep with its
 * position (in the full state, and in a delta whenever it falls asleep or wakes), and the
 * client's stand-in mob takes it up, so its sprite shows the 'z' - a village abed at night
 * looks abed on every screen. A host that sends no flag (an older build) sends no sleepers.
 */
public class MobSleepSyncTest {

	private static final long SEED = 0x5EED0F7EA7L;

	private Level level;
	private Hero hero;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void save(){
		level = Dungeon.level;
		hero = Dungeon.hero;
		Actor.clear();
	}

	@After
	public void restore(){
		Actor.clear();
		StateSerializer.resetDeltaTracking();
		Dungeon.level = level;
		Dungeon.hero = hero;
	}

	private static JSONObject entry( JSONArray mobs, Mob m ) throws Exception {
		for (int i = 0; mobs != null && i < mobs.length(); i++){
			if (mobs.getJSONObject( i ).getInt( "id" ) == m.id()) return mobs.getJSONObject( i );
		}
		return null;
	}

	@Test
	public void theHostShipsWhoSleeps() throws Exception {
		int w = WindowGenerator.WIDTH, h = WindowGenerator.HEIGHT;
		PathFinder.setMapSize( w, h );
		float shift = WorldModel.calendarShift();
		WindowGenerator.Window win = WindowGenerator.generate( SEED, 0, 0, 0, shift );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, 0, 0, shift, GameCalendar.season(), win.terrain, w, h );
		assertNotNull( ow );
		Dungeon.level = ow;
		Hero me = new Hero();
		me.pos = w / 2 + (h / 2) * w;
		Dungeon.hero = me;
		Villager abed = new Villager(), awake = new Villager();
		abed.pos = me.pos + 2;
		abed.state = abed.SLEEPING;
		awake.pos = me.pos - 2;
		awake.state = awake.PASSIVE;
		ow.mobs.add( abed );
		ow.mobs.add( awake );

		StateSerializer.resetDeltaTracking();
		JSONObject full = StateSerializer.serializeFullState();
		assertNotNull( full );
		assertTrue( entry( full.getJSONArray( "mobs" ), abed ).getBoolean( "sl" ) );
		assertFalse( entry( full.getJSONArray( "mobs" ), awake ).getBoolean( "sl" ) );

		//nothing changed: neither is sent again
		JSONObject quiet = StateSerializer.serializeDelta();
		assertTrue( quiet == null || entry( quiet.optJSONArray( "mobs" ), abed ) == null );

		//one wakes and the other goes to bed, standing where they were: both are sent
		abed.state = abed.PASSIVE;
		awake.state = awake.SLEEPING;
		JSONObject delta = StateSerializer.serializeDelta();
		assertNotNull( delta );
		assertFalse( entry( delta.getJSONArray( "mobs" ), abed ).getBoolean( "sl" ) );
		assertTrue( entry( delta.getJSONArray( "mobs" ), awake ).getBoolean( "sl" ) );
	}

	@Test
	public void theClientsStandInSleepsWithIt() throws Exception {
		SpectatorReceiver.SpectatorMob m = new SpectatorReceiver.SpectatorMob();
		SpectatorReceiver.applySleep( m, new JSONObject( "{\"id\":1,\"sl\":true}" ) );
		assertEquals( m.SLEEPING, m.state );
		SpectatorReceiver.applySleep( m, new JSONObject( "{\"id\":1,\"sl\":false}" ) );
		assertEquals( m.PASSIVE, m.state );
		//an older host's mob, with no flag: awake, as it always was
		m.state = m.SLEEPING;
		SpectatorReceiver.applySleep( m, new JSONObject( "{\"id\":1}" ) );
		assertEquals( m.PASSIVE, m.state );
	}
}
