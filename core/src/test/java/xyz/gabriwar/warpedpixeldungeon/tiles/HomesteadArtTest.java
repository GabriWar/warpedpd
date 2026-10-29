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

package xyz.gabriwar.warpedpixeldungeon.tiles;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.sprites.HomesteadSprites;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The homesteads' world art (tools/homestead_art_gen.py) sits where HomesteadTiles, RoofTiles and
 * HomesteadSprites say: the built terrain at the same cells of both sheets a slice draws with,
 * every wall piece covering exactly the pixels of the dungeon wall stitch it stands in for, the
 * overlays clear behind, every roof and frame painted.
 */
public class HomesteadArtTest {

	private static BufferedImage image( String asset ) throws IOException {
		File f = new File( "src/main/assets/" + asset );
		assertTrue( asset + " is not in the assets", f.exists() );
		return ImageIO.read( f );
	}

	private static int alpha( BufferedImage img, int cell, int x, int y ){
		return (img.getRGB( (cell % 16) * 16 + x, (cell / 16) * 16 + y ) >>> 24) & 0xFF;
	}

	private static String coverage( BufferedImage img, int cell ){
		StringBuilder sb = new StringBuilder();
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) sb.append( alpha( img, cell, x, y ) == 0 ? '.' : 'X' );
		return sb.toString();
	}

	private static boolean painted( BufferedImage img, int cell ){
		return coverage( img, cell ).indexOf( 'X' ) != -1;
	}

	private static boolean translucent( BufferedImage img, int cell ){
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++){
			int a = alpha( img, cell, x, y );
			if (a > 0 && a < 255) return true;
		}
		return false;
	}

	private static boolean clearSomewhere( BufferedImage img, int cell ){
		return coverage( img, cell ).indexOf( '.' ) != -1;
	}

	@Test
	public void theSurfaceAndTheCavesHoldTheSameBlockAtTheSameCells() throws IOException {
		BufferedImage ow = image( Assets.Environment.TILES_OVERWORLD );
		BufferedImage caves = image( Assets.Environment.TILES_CAVES );
		int rows = HomesteadTiles.LAST_ROW + 1;
		assertTrue( ow.getHeight() >= rows * 16 );
		assertTrue( caves.getHeight() >= rows * 16 );
		for (int y = HomesteadTiles.FIRST_ROW * 16; y < rows * 16; y++){
			for (int x = 0; x < 256; x++){
				//the block's last two free cells hold a flood's mud, the surface's alone
				//(tools/assets/ow_events_nature_art.py MUD_CELLS): no homestead piece
				if (y / 16 == HomesteadTiles.LAST_ROW && x / 16 >= 14) continue;
				assertEquals( "pixel " + x + "," + y, ow.getRGB( x, y ), caves.getRGB( x, y ) );
			}
		}
	}

	@Test
	public void everyWallPieceCoversWhatItsDungeonStitchCovers() throws IOException {
		BufferedImage ow = image( Assets.Environment.TILES_OVERWORLD );
		for (int m = 0; m < HomesteadTiles.MATERIALS; m++){
			for (int s = 0; s < 4; s++){
				String face = coverage( ow, DungeonTileSheet.RAISED_WALL + s );
				assertEquals( face, coverage( ow, HomesteadTiles.face( m, s, false ) ) );
				assertEquals( face, coverage( ow, HomesteadTiles.face( m, s, true ) ) );
				assertEquals( face, coverage( ow, HomesteadTiles.faceOverDoor( m, s ) ) );
				assertEquals( face, coverage( ow, HomesteadTiles.windowFace( m, s ) ) );
				String lip = coverage( ow, DungeonTileSheet.WALL_OVERHANG + s );
				assertEquals( lip, coverage( ow, HomesteadTiles.overhang( m, s ) ) );
				assertEquals( lip, coverage( ow, HomesteadTiles.windowOverhang( m, s ) ) );
				assertEquals( coverage( ow, DungeonTileSheet.DOOR_SIDEWAYS_OVERHANG + s ),
						coverage( ow, HomesteadTiles.sideDoorOverhang( m, false, true, s ) ) );
				assertEquals( coverage( ow, DungeonTileSheet.DOOR_SIDEWAYS_OVERHANG_CLOSED + s ),
						coverage( ow, HomesteadTiles.sideDoorOverhang( m, true, false, s ) ) );
			}
			for (int mask = 0; mask < 16; mask++){
				String top = coverage( ow, DungeonTileSheet.WALLS_INTERNAL + mask );
				assertEquals( top, coverage( ow, HomesteadTiles.internal( m, mask ) ) );
				assertEquals( top, coverage( ow, HomesteadTiles.windowInternal( m, mask ) ) );
			}
			assertFalse( clearSomewhere( ow, HomesteadTiles.flat( m, false ) ) );
			assertFalse( clearSomewhere( ow, HomesteadTiles.flatWindow( m ) ) );
		}
		assertEquals( coverage( ow, DungeonTileSheet.DOOR_OVERHANG ), coverage( ow, HomesteadTiles.BARRED_OVERHANG ) );
		assertEquals( coverage( ow, DungeonTileSheet.DOOR_OVERHANG_OPEN ), coverage( ow, HomesteadTiles.DOOR_OVERHANG_OPEN ) );
		assertEquals( coverage( ow, DungeonTileSheet.DOOR_SIDEWAYS ), coverage( ow, HomesteadTiles.BARRED_SIDEWAYS ) );
		assertFalse( clearSomewhere( ow, HomesteadTiles.DOOR_THRESHOLD ) );
	}

	@Test
	public void theGroundsArePaintedAndTheOverlaysClearBehind() throws IOException {
		BufferedImage ow = image( Assets.Environment.TILES_OVERWORLD );
		for (int i = 0; i < 16; i++){
			assertFalse( clearSomewhere( ow, HomesteadTiles.farmland( i, false ) ) );
			assertFalse( clearSomewhere( ow, HomesteadTiles.farmland( i, true ) ) );
			assertTrue( painted( ow, HomesteadTiles.bridgeEW( i & 3, i >> 2 ) ) );
			assertTrue( painted( ow, HomesteadTiles.bridgeNS( i & 3, i >> 2 ) ) );
		}
		//a bridge's rail on its south or east side casts a see-through shadow on the water
		assertTrue( translucent( ow, HomesteadTiles.bridgeEW( 0, HomesteadTiles.RAIL_LAST ) ) );
		assertTrue( translucent( ow, HomesteadTiles.bridgeNS( 0, HomesteadTiles.RAIL_LAST ) ) );
		assertFalse( clearSomewhere( ow, HomesteadTiles.FLOOR_STONE_ALT_2 ) );

		BufferedImage feats = image( Assets.Environment.TERRAIN_FEATURES );
		for (int mask = 0; mask < 16; mask++){
			for (int cell : new int[]{ HomesteadTiles.palisade( mask ), HomesteadTiles.fence( mask ), HomesteadTiles.pathStone( mask ) }){
				assertTrue( "feature " + cell, painted( feats, cell ) );
			}
			assertTrue( clearSomewhere( feats, HomesteadTiles.palisade( mask ) ) );
			assertTrue( clearSomewhere( feats, HomesteadTiles.fence( mask ) ) );
		}
		for (int cell = HomesteadTiles.GATE_EW; cell <= HomesteadTiles.RUBBLE_STONE_ALT; cell++){
			assertTrue( "feature " + cell, painted( feats, cell ) );
		}
	}

	@Test
	public void everyRoofAndFrameIsPainted() throws IOException {
		BufferedImage roofs = image( RoofTiles.TEXTURE );
		boolean seeThrough = false;
		for (int k = 0; k < RoofTiles.KINDS; k++){
			HashSet<String> looks = new HashSet<>();
			for (int mask = 0; mask < 16; mask++){
				for (boolean north : new boolean[]{ false, true }){
					int cell = RoofTiles.index( k, mask, north );
					assertTrue( painted( roofs, cell ) );
					StringBuilder sb = new StringBuilder();
					for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++){
						int argb = roofs.getRGB( (cell % 16) * 16 + x, (cell / 16) * 16 + y );
						sb.append( Integer.toHexString( argb ) );
						int a = (argb >>> 24) & 0xFF;
						if (k == RoofTiles.GLASS && a > 0 && a < 255) seeThrough = true;
					}
					looks.add( sb.toString() );
				}
			}
			assertEquals( "every mask and slope of roof " + k + " is its own picture", 32, looks.size() );
			for (int e = 0; e < 4; e++) assertTrue( painted( roofs, RoofTiles.ridge( k, e ) ) );
		}
		assertTrue( "the glass roof shows the ground through it", seeThrough );

		BufferedImage sheet = image( HomesteadSprites.TEXTURE );
		int[] frames = { HomesteadSprites.WORKBENCH, HomesteadSprites.MASONS_BENCH, HomesteadSprites.KILN,
				HomesteadSprites.HEARTH, HomesteadSprites.COOKING_POT, HomesteadSprites.TANNING_RACK,
				HomesteadSprites.TANNING_RACK_HIDE, HomesteadSprites.QUERN, HomesteadSprites.FORGE,
				HomesteadSprites.COMPOST_BIN, HomesteadSprites.BED_STRAW, HomesteadSprites.BED_WOOD,
				HomesteadSprites.BED_FUR, HomesteadSprites.CHEST_WOOD, HomesteadSprites.CHEST_IRON,
				HomesteadSprites.ALARM_BELL, HomesteadSprites.HEARTHSTONE, HomesteadSprites.HEARTHSTONE_LIT,
				HomesteadSprites.WAYSTONE, HomesteadSprites.TIE_POST, HomesteadSprites.TRAP_SPIKES,
				HomesteadSprites.WATCHTOWER, HomesteadSprites.WATCHTOWER + 16 };
		for (int f : frames) assertTrue( "frame " + f, painted( sheet, f ) );
		for (int[] anim : new int[][]{ { HomesteadSprites.KILN_LIT, HomesteadSprites.LIT_FRAMES },
				{ HomesteadSprites.HEARTH_LIT, HomesteadSprites.LIT_FRAMES }, { HomesteadSprites.FORGE_LIT, HomesteadSprites.LIT_FRAMES },
				{ HomesteadSprites.COOKING_POT_STEAM, HomesteadSprites.STEAM_FRAMES },
				{ HomesteadSprites.ALARM_BELL_RINGING, HomesteadSprites.RING_FRAMES },
				{ HomesteadSprites.WAYSTONE_ATTUNED, HomesteadSprites.GLOW_FRAMES } }){
			for (int i = 0; i < anim[1]; i++) assertTrue( "frame " + (anim[0] + i), painted( sheet, anim[0] + i ) );
		}
	}
}
