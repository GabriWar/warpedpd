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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.HighGrass;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import com.badlogic.gdx.Gdx;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The human villages' fields (WorldStructures.fieldPlots): pure, two to four plots of 4x3 to
 * 6x4 outside the fence, never on or beside a road, a door, the signpost or water, never on
 * another place's grounds; ploughed into a generated window as FURROWED_GRASS with a mask of
 * their own, drawn by the season instead of the warden's grass, trodden flat for no loot and
 * healed when the window moves, and landed on by the crows.
 */
public class FieldPlotsTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final long[] SEEDS = { SEED, 12345L, 987654321L, 0xC0FFEE1234L };
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private Level saved;
	private Hero savedHero;
	private byte[] variance;
	private float shift;
	private boolean shiftHeld;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void save(){
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		variance = DungeonTileSheet.tileVariance;
		shift = WorldModel.seasonShift();
		shiftHeld = WorldModel.seasonShiftHeld();
		Actor.clear();
	}

	@After
	public void restore(){
		Actor.clear();
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
		DungeonTileSheet.tileVariance = variance;
		WorldModel.releaseSeasonShift();
		WorldModel.setSeasonShift( shift );
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
	}

	private static int ring( long seed, int sx, int sy, int wx, int wy ){
		return Math.max( Math.abs( wx - WorldStructures.siteX( seed, sx, sy ) ), Math.abs( wy - WorldStructures.siteY( seed, sx, sy ) ) );
	}

	//the plots: shaped and placed as promised, apart, out of every other place's way; none for gnolls or outlaws
	@Test
	public void everyHumanVillageHasTwoToFourPlotsOutsideItsFence(){
		int villages = 0, plots = 0;
		for (long seed : SEEDS){
			for (int sy = -8; sy <= 8; sy++){
				for (int sx = -8; sx <= 8; sx++){
					if (WorldStructures.siteType( seed, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
					int[] f = WorldStructures.fieldPlots( seed, sx, sy );
					assertEquals( f.length % 4, 0 );
					if (WorldStructures.faction( seed, sx, sy ) != WorldStructures.Faction.HUMAN){
						assertEquals( "a gnoll or outlaw village with fields", 0, f.length );
						continue;
					}
					villages++;
					int n = f.length / 4;
					assertTrue( n <= 4 );
					plots += n;
					int R = WorldStructures.settlementLayout( seed, sx, sy )[0];
					for (int i = 0; i < f.length; i += 4){
						int w = f[i+2] - f[i] + 1, h = f[i+3] - f[i+1] + 1;
						int along = Math.max( w, h ), out = Math.min( w, h );
						assertTrue( "plot " + w + "x" + h, along >= 4 && along <= 6 && out >= 3 && out <= 4 );
						for (int wy = f[i+1]; wy <= f[i+3]; wy++){
							for (int wx = f[i]; wx <= f[i+2]; wx++){
								int r = ring( seed, sx, sy, wx, wy );
								assertTrue( "ring " + r + " of " + R, r >= R + WorldStructures.FIELD_RING0 && r <= R + WorldStructures.FIELD_REACH );
							}
						}
						//a cell apart from the others
						for (int j = i + 4; j < f.length; j += 4){
							assertTrue( "plots touching", f[i] - 1 > f[j+2] || f[j] > f[i+2] + 1 || f[i+1] - 1 > f[j+3] || f[j+1] > f[i+3] + 1 );
						}
					}
					//pure: asked again, the very same
					assertArrayEquals( f, WorldStructures.fieldPlots( seed, sx, sy ) );
				}
			}
			//...and the same after another world's been asked (the caches turn over with the seed)
			int[] again = WorldStructures.fieldPlots( seed, 2, 3 );
			WorldStructures.fieldPlots( seed ^ 1, 2, 3 );
			assertArrayEquals( again, WorldStructures.fieldPlots( seed, 2, 3 ) );
		}
		System.out.println( "[fields] " + plots + " plots over " + villages + " human villages" );
		assertTrue( villages > 100 );
		assertTrue( "plots a village: " + plots / (float) villages, plots >= villages * 2 );
	}

	//every plot cell and the ring round it: no road, no bridge, no door, no house, no signpost,
	//no water - on the land itself, cell by cell, every season of the year: a cold winter's
	//puddles of ice or a warm summer's pools ploughed under with the rest
	@Test
	public void noPlotTouchesARoadADoorOrWater(){
		for (float shift : new float[]{ 0f, -1f, 1f }){
			WorldModel.holdSeasonShift( shift );
			for (long seed : SEEDS) noPlotTouchesARoadADoorOrWater( seed );
		}
	}

	private static void noPlotTouchesARoadADoorOrWater( long seed ){
		{
			for (int sy = -6; sy <= 6; sy++){
				for (int sx = -6; sx <= 6; sx++){
					int[] f = WorldStructures.fieldPlots( seed, sx, sy );
					for (int i = 0; i < f.length; i += 4){
						for (int wy = f[i+1] - 1; wy <= f[i+3] + 1; wy++){
							for (int wx = f[i] - 1; wx <= f[i+2] + 1; wx++){
								boolean inside = wx >= f[i] && wx <= f[i+2] && wy >= f[i+1] && wy <= f[i+3];
								int t = WorldStructures.terrainAt( seed, wx, wy );
								assertNotEquals( "road by a field", Terrain.DIRT_PATH, t );
								assertNotEquals( Terrain.BRIDGE, t );
								assertNotEquals( "a door by a field", Terrain.DOOR, t );
								assertNotEquals( Terrain.WALL, t );
								assertNotEquals( Terrain.SIGN, t );
								assertNotEquals( Terrain.BARRICADE, t );
								if (inside){
									assertEquals( "not ploughed " + wx + "," + wy, Terrain.FURROWED_GRASS, t );
									int wild = WorldModel.wildTerrain( seed, wx, wy, WorldModel.sample( seed, wx, wy, 0f, null ) );
									assertNotEquals( Terrain.WATER, wild );
									assertNotEquals( Terrain.DEEP_WATER, wild );
									assertNotEquals( Terrain.FROZEN_WATER, wild );
								}
							}
						}
					}
				}
			}
		}
	}

	//a generated window ploughs exactly its plots, marks exactly them, and dresses them with no
	//biome edge of their own; a stair's foot or water would leave a cell unploughed and unmarked
	@Test
	public void aWindowPloughsItsVillagesFields(){
		int[] v = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 4, 8 ).get( 0 );
		int ox = v[2] - W / 2, oy = v[3] - H / 2;
		for (float shift : new float[]{ 0f, -1f, 1f }){
			WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, shift );
			int marked = 0;
			for (int y = 1; y < H - 1; y++){
				for (int x = 1; x < W - 1; x++){
					int c = x + y * W, wx = ox + x, wy = oy + y;
					boolean plot = false;
					for (int dy = -1; dy <= 1; dy++){
						for (int dx = -1; dx <= 1; dx++){
							int sx = Math.floorDiv( wx, WorldStructures.SECTOR ) + dx, sy = Math.floorDiv( wy, WorldStructures.SECTOR ) + dy;
							plot |= WorldStructures.inField( WorldStructures.fieldPlots( SEED, sx, sy ), wx, wy );
						}
					}
					if (w.field[c]){
						marked++;
						assertTrue( "a field off its plots", plot );
						assertEquals( Terrain.FURROWED_GRASS, w.terrain[c] );
					} else {
						assertNotEquals( "unmarked furrows", Terrain.FURROWED_GRASS, w.terrain[c] );
					}
				}
			}
			assertTrue( "no field in the window: " + marked, marked >= 2 * 12 );
			int[][] dress = WindowGenerator.dress( SEED, ox, oy, w.terrain, w, GameCalendar.Season.SUMMER );
			for (int c = 0; c < w.field.length; c++){
				if (!w.field[c]) continue;
				assertEquals( "a biome edge over a field", -1, dress[0][c] );
			}
		}
		//the mountains and the caves plough nothing
		WindowGenerator.Window cave = WindowGenerator.generate( SEED, -1, ox, oy, 0f );
		for (boolean b : cave.field) assertFalse( b );
	}

	//the surface window round the first human village, as a mirror's (no populator) made the
	//host's own: Dungeon.level, its tile variance anchored
	private OverworldLevel window(){
		int[] v = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 4, 8 ).get( 0 );
		return window( SEED, v[2], v[3], WorldModel.calendarShift() );
	}

	//...or the one centred on (cx, cy) of another world, at a given season shift
	private OverworldLevel window( long seed, int cx, int cy, float shift ){
		PathFinder.setMapSize( W, H );
		int ox = cx - W / 2, oy = cy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( seed, 0, ox, oy, shift );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, seed, ox, oy, shift, GameCalendar.Season.SUMMER, w.terrain, W, H );
		assertNotNull( ow );
		try {
			Field f = OverworldLevel.class.getDeclaredField( "network" );
			f.setAccessible( true );
			f.set( ow, false );
		} catch (Exception e){
			throw new AssertionError( e );
		}
		Dungeon.level = ow;
		DungeonTileSheet.setupVariance( ow.length(), 7L );
		return ow;
	}

	private static int aField( OverworldLevel ow ){
		for (int c = 0; c < ow.length(); c++) if (ow.fieldAt( c )) return c;
		throw new AssertionError( "no field in the window" );
	}

	//drawn as the season's crop, with none of the warden's grass over it; any other furrowed
	//grass of the surface (a huntress's trail) is the warden's still
	@Test
	public void theFieldsAreDrawnByTheSeason(){
		OverworldLevel ow = window();
		int c = aField( ow );
		assertTrue( DungeonTileSheet.tilledSoil( c ) );
		assertEquals( GameCalendar.Season.SUMMER, ow.fieldSeason() );
		int summer = DungeonTileSheet.fieldTile( GameCalendar.Season.SUMMER, c );
		assertTrue( summer == DungeonTileSheet.FIELD_SUMMER || summer == DungeonTileSheet.FIELD_SUMMER_ALT );
		int spring = DungeonTileSheet.fieldTile( GameCalendar.Season.SPRING, c );
		assertTrue( spring == DungeonTileSheet.FIELD_SPRING || spring == DungeonTileSheet.FIELD_SPRING_ALT );
		int autumn = DungeonTileSheet.fieldTile( GameCalendar.Season.AUTUMN, c );
		assertTrue( autumn == DungeonTileSheet.FIELD_AUTUMN || autumn == DungeonTileSheet.FIELD_AUTUMN_ALT );
		assertEquals( DungeonTileSheet.FIELD_WINTER, DungeonTileSheet.fieldTile( GameCalendar.Season.WINTER, c ) );

		int wild = -1;
		for (int i = 0; i < ow.length() && wild == -1; i++){
			if (ow.map[i] == Terrain.GRASS && !ow.fieldAt( i )) wild = i;
		}
		assertNotEquals( -1, wild );
		ow.map[wild] = Terrain.FURROWED_GRASS;
		assertFalse( "a huntress's furrowed grass drawn as a field", DungeonTileSheet.tilledSoil( wild ) );
	}

	//the tiles themselves are in the overworld sheet, every one of them painted
	@Test
	public void theFieldTilesArePainted() throws Exception {
		BufferedImage sheet = javax.imageio.ImageIO.read( Gdx.files.internal( Assets.Environment.TILES_OVERWORLD ).read() );
		for (int id = DungeonTileSheet.FIELD_SPRING; id <= DungeonTileSheet.FIELD_WINTER; id++){
			int x0 = (id % 16) * 16, y0 = (id / 16) * 16;
			int opaque = 0;
			for (int y = 0; y < 16; y++){
				for (int x = 0; x < 16; x++) if ((sheet.getRGB( x0 + x, y0 + y ) >>> 24) == 255) opaque++;
			}
			assertEquals( "tile " + id + " not painted whole", 256, opaque );
		}
	}

	//trodden: plain grass, nothing dropped, no longer a field; and never kept as the player's edit -
	//the window ploughs it again when it moves
	@Test
	public void aTroddenFieldGivesNothingAndGrowsBack() throws Exception {
		OverworldLevel ow = window();
		int c = aField( ow );
		int heaps = ow.heaps.valueList().size();
		HighGrass.trample( ow, c );
		assertEquals( Terrain.GRASS, ow.map[c] );
		assertEquals( "loot from a field", heaps, ow.heaps.valueList().size() );
		assertFalse( ow.fieldAt( c ) );
		Method capture = OverworldLevel.class.getDeclaredMethod( "captureDiffs" );
		capture.setAccessible( true );
		capture.invoke( ow );
		Field diffs = OverworldLevel.class.getDeclaredField( "diffs" );
		diffs.setAccessible( true );
		assertTrue( "the trodden crop kept as an edit", ((HashMap<?, ?>) diffs.get( ow )).isEmpty() );
	}

	//the crows come down on the fields, and stand in front of the crop of the field below them
	@Test
	public void theCrowsLandOnTheFields() throws Exception {
		OverworldLevel ow = window();
		int c = aField( ow );
		while (ow.fieldAt( c + W )) c += W;   //the plot's bottom row
		c -= W;                               //...and the one above it: a field below
		assertTrue( ow.fieldAt( c ) && ow.fieldAt( c + W ) );
		OverworldCritters.Field critters = new OverworldCritters.Field( ow );
		try {
			Method ground = OverworldCritters.Field.class.getDeclaredMethod( "birdGround", OverworldCritters.Species.class, int.class );
			Method below = OverworldCritters.Field.class.getDeclaredMethod( "clearBelowHere", int.class );
			ground.setAccessible( true );
			below.setAccessible( true );
			assertTrue( (Boolean) ground.invoke( critters, OverworldCritters.Species.CROW, c ) );
			assertFalse( (Boolean) ground.invoke( critters, OverworldCritters.Species.GULL, c ) );
			assertFalse( (Boolean) ground.invoke( critters, OverworldCritters.Species.HARE, c ) );
			assertTrue( (Boolean) below.invoke( critters, c ) );
			//trodden flat, it is grass the crows stand on as anywhere
			ow.map[c + W] = Terrain.GRASS;
			assertTrue( (Boolean) below.invoke( critters, c ) );
		} finally {
			critters.destroy();
		}
	}

	//a village's field draws the crows whatever country it was ploughed in: a foothills farm,
	//whose own country has no birds, and a field in a wood's clearing, which the woods' edge rule
	//(a tree within two cells, none alongside) would turn them away from - by day and dawn,
	//never at dusk, at night or in foul weather
	@Test
	public void theCrowsComeDownOnFoothillsAndWoodlandFields() throws Exception {
		WorldModel.holdSeasonShift( 0f );
		assertNull( "the foothills got birds of their own", OverworldCritters.birdFor( WorldModel.Biome.FOOTHILLS, DayNightCycle.Phase.DAY ) );
		Method below = OverworldCritters.Field.class.getDeclaredMethod( "clearBelowHere", int.class );
		below.setAccessible( true );
		boolean foothills = false, clearing = false;
		int seen = 0;
		for (long seed : SEEDS){
			for (int sy = -8; sy <= 8 && !(foothills && clearing); sy++){
				for (int sx = -8; sx <= 8 && !(foothills && clearing); sx++){
					//only a village with a plot on the country still wanted is worth a window
					int[] f = WorldStructures.fieldPlots( seed, sx, sy );
					boolean wantHills = false, wantWood = false;
					for (int i = 0; i < f.length; i += 4){
						for (int wy = f[i+1]; wy <= f[i+3]; wy++){
							for (int wx = f[i]; wx <= f[i+2]; wx++){
								WorldModel.Biome b = WorldModel.baseBiomeAt( seed, wx, wy );
								wantHills |= !foothills && b == WorldModel.Biome.FOOTHILLS;
								wantWood |= !clearing && b == WorldModel.Biome.FOREST;
							}
						}
					}
					if (!wantHills && !wantWood) continue;
					OverworldLevel ow = window( seed, WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ), 0f );
					OverworldCritters.Field critters = new OverworldCritters.Field( ow );
					try {
						for (int c = 2 * W; c < ow.length() - 2 * W && (wantHills || wantWood); c++){
							int x = c % W;
							if (x < 2 || x > W - 3 || !ow.fieldAt( c )) continue;
							//a cell spawnFlock's own probe would take up: open round, nothing drawn over it
							if (!OverworldCritters.openAround( ow.map, W, c ) || !(Boolean) below.invoke( critters, c )) continue;
							WorldModel.Biome b = ow.biomeAtCell( c );
							if (wantHills && b == WorldModel.Biome.FOOTHILLS){
								seatsCrowsOnly( critters, c );
								foothills = true;
								wantHills = false;
								seen++;
							} else if (wantWood && b == WorldModel.Biome.FOREST && !OverworldCritters.forestEdge( ow.map, W, H, c )){
								seatsCrowsOnly( critters, c );
								clearing = true;
								wantWood = false;
								seen++;
							}
						}
					} finally {
						critters.destroy();
					}
				}
			}
		}
		assertTrue( "no foothills field found to try", foothills );
		assertTrue( "no field in a wood's clearing found to try", clearing );
		assertEquals( 2, seen );
	}

	//crows at the crows' hours on this field cell, nothing at the others, and a flock of them
	//settles there when nobody is near
	private static void seatsCrowsOnly( OverworldCritters.Field critters, int c ) throws Exception {
		Method flockFor = OverworldCritters.Field.class.getDeclaredMethod( "flockFor", int.class,
				DayNightCycle.Phase.class, OverworldCritters.Sky.class, boolean.class );
		Method settle = OverworldCritters.Field.class.getDeclaredMethod( "settle", OverworldCritters.Species.class, int.class, float[].class );
		Field flocks = OverworldCritters.Field.class.getDeclaredField( "flocks" );
		flockFor.setAccessible( true );
		settle.setAccessible( true );
		flocks.setAccessible( true );
		OverworldCritters.Species crow = OverworldCritters.Species.CROW;
		assertEquals( crow, flockFor.invoke( critters, c, DayNightCycle.Phase.DAY, OverworldCritters.Sky.CLEAR, false ) );
		assertEquals( crow, flockFor.invoke( critters, c, DayNightCycle.Phase.DAWN, OverworldCritters.Sky.WET, false ) );
		assertNull( flockFor.invoke( critters, c, DayNightCycle.Phase.DUSK, OverworldCritters.Sky.CLEAR, false ) );
		assertNull( flockFor.invoke( critters, c, DayNightCycle.Phase.NIGHT, OverworldCritters.Sky.CLEAR, false ) );
		assertNull( flockFor.invoke( critters, c, DayNightCycle.Phase.DAY, OverworldCritters.Sky.FOUL, false ) );
		//the critters' own dice, seeded: the flock's spots are drawn at random round the cell
		OverworldCritters.RNG.setSeed( c );
		List<?> list = (List<?>) flocks.get( critters );
		int before = list.size();
		//nobody about but a hero far off in the window's corner
		assertTrue( "no flock settled", (Boolean) settle.invoke( critters, crow, c, new float[]{ 0f, 0f } ) );
		assertEquals( before + 1, list.size() );
		Object flock = list.get( list.size() - 1 );
		Field species = flock.getClass().getDeclaredField( "species" );
		species.setAccessible( true );
		assertEquals( crow, species.get( flock ) );
	}
}
