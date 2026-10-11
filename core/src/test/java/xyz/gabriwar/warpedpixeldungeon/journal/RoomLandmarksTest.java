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

package xyz.gabriwar.warpedpixeldungeon.journal;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceBlock;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowSlideTest;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.EmptyRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.ThermalSpringRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.WayfarersCampRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.BellowsRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.BlackMarketRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.BreakersBenchRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.CounterweightVaultRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.ElementalLockRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.FrozenCacheRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.HothouseRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.IncubatorNestRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.PedigreeHallRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.RivalGalleryRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.SquirrelsHoardRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.TemperingForgeRoom;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The special rooms Warped adds each have a journal entry: its own 16x16 picture on the icons
 * sheet, a name and a description, and something in the room that puts it in the journal the
 * moment the hero sees it - and not before.
 */
public class RoomLandmarksTest {

	private static final Map<Class<? extends Room>, Notes.Landmark> ROOMS = new LinkedHashMap<>();
	static {
		ROOMS.put( RivalGalleryRoom.class,       Notes.Landmark.RIVAL_GALLERY );
		ROOMS.put( TemperingForgeRoom.class,     Notes.Landmark.TEMPERING_FORGE );
		ROOMS.put( BreakersBenchRoom.class,      Notes.Landmark.BREAKERS_BENCH );
		ROOMS.put( PedigreeHallRoom.class,       Notes.Landmark.PEDIGREE_HALL );
		ROOMS.put( FrozenCacheRoom.class,        Notes.Landmark.FROZEN_CACHE );
		ROOMS.put( BellowsRoom.class,            Notes.Landmark.BELLOWS );
		ROOMS.put( ElementalLockRoom.class,      Notes.Landmark.ELEMENTAL_LOCK );
		ROOMS.put( HothouseRoom.class,           Notes.Landmark.HOTHOUSE );
		ROOMS.put( IncubatorNestRoom.class,      Notes.Landmark.INCUBATOR_NEST );
		ROOMS.put( CounterweightVaultRoom.class, Notes.Landmark.COUNTERWEIGHT_VAULT );
		ROOMS.put( BlackMarketRoom.class,        Notes.Landmark.BLACK_MARKET );
		ROOMS.put( SquirrelsHoardRoom.class,     Notes.Landmark.SQUIRRELS_HOARD );
		ROOMS.put( ThermalSpringRoom.class,      Notes.Landmark.THERMAL_SPRING );
		ROOMS.put( WayfarersCampRoom.class,      Notes.Landmark.WAYFARERS_CAMP );
	}

	@BeforeClass
	public static void boot() throws Exception {
		WindowSlideTest.boot();
	}

	@AfterClass
	public static void unboot(){
		WindowSlideTest.unboot();
	}

	private Level level;

	@Before
	public void setUp(){
		Actor.clear();
		Dungeon.depth = 3;
		Dungeon.branch = 0;
		Dungeon.hero = new Hero();
		ClimateManager.debugTempOverride = 15f;
		Notes.reset();
		level = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public boolean waterCanFreeze(){ return true; }
		};
		level.setSize( 40, 40 );
		level.mobs = new HashSet<>();
		level.heaps = new SparseArray<>();
		level.blobs = new HashMap<>();
		level.plants = new SparseArray<>();
		level.traps = new SparseArray<>();
		level.customTiles = new ArrayList<>();
		level.customTerrain = new ArrayList<>();
		level.customWalls = new ArrayList<>();
		level.transitions = new ArrayList<>();
		Dungeon.level = level;
		Dungeon.hero.pos = 1 + level.width();
	}

	@Test
	public void everyRoomEntryHasItsOwnPictureNameAndDescription(){
		Image stairs = Icons.STAIRS.get();
		HashSet<String> rects = new HashSet<>();
		for (Notes.Landmark lm : ROOMS.values()){
			Notes.LandmarkRecord rec = new Notes.LandmarkRecord( lm, 3 );

			String title = rec.title(), desc = rec.desc();
			assertFalse( lm + " has no name", title.isEmpty() || title.contains( Messages.NO_TEXT_FOUND ) );
			assertFalse( lm + " has no description", desc.isEmpty() || desc.contains( Messages.NO_TEXT_FOUND ) );

			Image icon = rec.icon();
			assertTrue( lm + " is not drawn from the icons sheet", icon.texture == stairs.texture );
			RectF f = icon.frame();
			int w = icon.texture.width, h = icon.texture.height;
			int x = Math.round( f.left * w ), y = Math.round( f.top * h );
			assertEquals( lm + " is not 16 wide", 16, Math.round( f.width() * w ) );
			assertEquals( lm + " is not 16 high", 16, Math.round( f.height() * h ) );
			assertFalse( lm + " falls back to the stairs", f.left == stairs.frame().left && f.top == stairs.frame().top );
			assertTrue( lm + " shares its picture", rects.add( x + "," + y ) );

			//and there is a picture there: a solid middle on a clear corner
			Pixmap sheet = icon.texture.bitmap;
			int opaque = 0;
			for (int i = 0; i < 16; i++){
				for (int j = 0; j < 16; j++){
					if ((sheet.getPixel( x + i, y + j ) & 0xFF) == 0xFF) opaque++;
				}
			}
			assertTrue( lm + " has an empty cell (" + opaque + " solid pixels)", opaque > 100 );
		}
	}

	@Test
	public void everyRoomGoesInTheJournalOnlyOnceSeen() throws Exception {
		for (Map.Entry<Class<? extends Room>, Notes.Landmark> e : ROOMS.entrySet()){
			setUp();
			Room room = paint( e.getKey() );
			String what = e.getKey().getSimpleName();

			boolean[] fov = new boolean[level.length()];
			assertFalse( what + " is in the journal unseen", seen( fov ).contains( e.getValue() ) );

			for (int x = room.left; x <= room.right; x++){
				for (int y = room.top; y <= room.bottom; y++){
					fov[x + y * level.width()] = true;
				}
			}
			HashSet<Notes.Landmark> found = seen( fov );
			assertTrue( what + " is not in the journal once seen: " + found, found.contains( e.getValue() ) );
			assertEquals( what + " gives other entries too: " + found, 1, found.size() );
		}
	}

	@Test
	public void theEntriesSurviveASave() throws Exception {
		for (Notes.Landmark lm : ROOMS.values()) Notes.add( lm, 3 );
		Bundle bundle = new Bundle();
		Notes.storeInBundle( bundle );
		Notes.reset();
		Notes.restoreFromBundle( Bundle.read( new java.io.ByteArrayInputStream( bundleBytes( bundle ) ) ) );
		for (Notes.Landmark lm : ROOMS.values()){
			assertTrue( lm + " lost in the save", Notes.contains( lm, 3 ) );
		}
	}

	@Test
	public void theFrozenCacheLeavesTheJournalWithItsLastBlock() throws Exception {
		paint( FrozenCacheRoom.class );
		Notes.add( Notes.Landmark.FROZEN_CACHE );
		ArrayList<IceBlock> blocks = new ArrayList<>();
		for (Mob m : level.mobs) if (m instanceof IceBlock) blocks.add( (IceBlock) m );
		assertTrue( blocks.size() >= 2 );
		for (int i = 0; i < blocks.size(); i++){
			assertTrue( "gone with blocks still standing", Notes.contains( Notes.Landmark.FROZEN_CACHE ) );
			blocks.get( i ).destroy();
		}
		assertFalse( "still there with every block gone", Notes.contains( Notes.Landmark.FROZEN_CACHE ) );
	}

	// ---------------------------------------------------------------- helpers

	private Room paint( Class<? extends Room> type ) throws Exception {
		Room room = type.getConstructor().newInstance();
		if (room instanceof FrozenCacheRoom) ((FrozenCacheRoom) room).seasonOverride = GameCalendar.Season.WINTER;
		if (room instanceof SquirrelsHoardRoom) ((SquirrelsHoardRoom) room).seasonOverride = GameCalendar.Season.SUMMER;
		room.set( 12, 12, 12 + room.minWidth() - 1, 12 + room.minHeight() - 1 );
		Painter.fill( level, 0, 0, 40, 40, Terrain.EMPTY );
		Room.Door door = new Room.Door( room.left, room.top + 2 );
		room.connected.put( new EmptyRoom(), door );
		//a floor is painted before it is anybody's current level (Dungeon.newLevel)
		Dungeon.level = null;
		room.paint( level );
		Dungeon.level = level;
		Painter.set( level, door.x, door.y, Terrain.DOOR );
		level.buildFlagMaps();
		return room;
	}

	//what Hero.checkVisibleMobs() would note with this field of view: a mob that is in sight,
	//and a living blob with any marked cell in sight
	private HashSet<Notes.Landmark> seen( boolean[] fov ){
		HashSet<Notes.Landmark> found = new HashSet<>();
		for (Mob m : level.mobs){
			if (fov[m.pos] && m.landmark() != null) found.add( m.landmark() );
		}
		for (Blob b : level.blobs.values()){
			if (b.volume <= 0 || b.landmark() == null) continue;
			for (int i = b.area.top; i < b.area.bottom; i++){
				for (int j = b.area.left; j < b.area.right; j++){
					int cell = j + i * level.width();
					if (fov[cell] && b.cur[cell] > 0) found.add( b.landmark() );
				}
			}
		}
		return found;
	}

	private static byte[] bundleBytes( Bundle bundle ){
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		assertTrue( Bundle.write( bundle, out ) );
		return out.toByteArray();
	}
}
