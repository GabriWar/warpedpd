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

package xyz.gabriwar.warpedpixeldungeon.audio;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The sounds room acoustics plays (tools/room_acoustics_gen.py) are all there, are made from
 * effects the game loads, and are made from those effects as they are now: a merge that changes
 * one fails here until the generator is run again (docs/upstream-merge.md). Its voice budget knows
 * how long each of them plays (RoomAcousticsLengths, which the generator writes too).
 */
public class RoomAcousticsAssetsTest {

	private static final File ASSETS = new File( "src/main/assets" );
	private static final File MANIFEST = new File( "../tools/room_acoustics_sources.txt" );

	@Test
	public void everyFileItPlaysIsThere(){
		String[] assets = RoomAcoustics.assets();
		assertEquals( 121, assets.length );
		assertEquals( "the switch's preview's tail loads first", "sounds/tail_hall_hit_strong.mp3", assets[0] );
		assertEquals( "no file twice", 121, new HashSet<>( Arrays.asList( assets ) ).size() );
		int caverns = 0;
		for (String path : assets){
			assertTrue( "missing " + path, new File( ASSETS, path ).isFile() );
			if (path.startsWith( "sounds/tail_cavern_" )) caverns++;
		}
		assertEquals( 32, caverns );
		assertTrue( "the muffled copies load last: the last of them in tells they all are",
				assets[assets.length - 1].startsWith( "sounds/muffled_" ) );
	}

	@Test
	public void everySoundOfItsTableIsOneTheGameLoads(){
		List<String> all = Arrays.asList( Assets.Sounds.all );
		assertEquals( 33, RoomAcoustics.sounds().size() );
		for (String id : RoomAcoustics.sounds()){
			assertTrue( id + " is not in Assets.Sounds.all", all.contains( id ) );
		}
	}

	@Test
	public void itKnowsHowLongEveryTailAndEveryEffectWithTailsPlays() throws IOException {
		int n = 0;
		for (String path : RoomAcoustics.assets()){
			if (!path.startsWith( "sounds/tail_room_" )) continue;
			String name = path.substring( "sounds/tail_room_".length(), path.length() - ".mp3".length() );
			String regenerate = ": run python3 tools/room_acoustics_gen.py";
			float[] lengths = RoomAcousticsLengths.of( name );
			assertNotNull( name + " has no lengths" + regenerate, lengths );
			assertEquals( path + regenerate, playLength( new File( ASSETS, path ) ), lengths[0], 1e-4 );
			assertEquals( "tail_hall_" + name + regenerate,
					playLength( new File( ASSETS, "sounds/tail_hall_" + name + ".mp3" ) ), lengths[1], 1e-4 );
			assertEquals( "tail_cavern_" + name + regenerate,
					playLength( new File( ASSETS, "sounds/tail_cavern_" + name + ".mp3" ) ), lengths[2], 1e-4 );
			assertEquals( name + regenerate, playLength( new File( ASSETS, "sounds/" + name + ".mp3" ) ), lengths[3], 1e-4 );
			n++;
		}
		assertEquals( 32, n );
	}

	//MPEG layer III: kbit/s by bitrate index for MPEG 1, then for MPEG 2 and 2.5; Hz by rate index for each
	private static final int[][] KBPS = { { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320 },
			{ 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160 } };
	private static final int[][] HZ = { { 44100, 48000, 32000 }, { 22050, 24000, 16000 }, { 11025, 12000, 8000 } };

	/** How long an MP3 plays, s: every frame of it, its Info frame too, as libGDX's desktop decoder
	 *  plays them (the longest of the platforms). */
	static double playLength( File file ) throws IOException {
		byte[] b = Files.readAllBytes( file.toPath() );
		int i = 0;
		if (b.length > 10 && b[0] == 'I' && b[1] == 'D' && b[2] == '3'){
			i = 10 + ((b[6] & 0x7f) << 21 | (b[7] & 0x7f) << 14 | (b[8] & 0x7f) << 7 | (b[9] & 0x7f));
		}
		long samples = 0;
		int rate = 0;
		while (i + 4 <= b.length && (b[i] & 0xFF) == 0xFF && (b[i + 1] & 0xE0) == 0xE0){
			int version = b[i + 1] >> 3 & 3, layer = b[i + 1] >> 1 & 3;
			int bitrate = (b[i + 2] & 0xFF) >> 4, rateIndex = b[i + 2] >> 2 & 3, padding = b[i + 2] >> 1 & 1;
			if (layer != 1 || bitrate == 0 || bitrate == 15 || rateIndex == 3 || version == 1) break;
			boolean mpeg1 = version == 3;
			rate = HZ[mpeg1 ? 0 : version == 2 ? 1 : 2][rateIndex];
			samples += mpeg1 ? 1152 : 576;
			i += (mpeg1 ? 144 : 72) * KBPS[mpeg1 ? 0 : 1][bitrate] * 1000 / rate + padding;
		}
		assertTrue( "no MPEG audio in " + file, samples > 0 );
		return samples / (double) rate;
	}

	@Test
	public void everyDrySoundIsAsTheFilesWereMadeFrom() throws IOException, NoSuchAlgorithmException {
		TreeMap<String, String> manifest = new TreeMap<>();
		for (String line : Files.readAllLines( MANIFEST.toPath(), StandardCharsets.UTF_8 )){
			if (line.isEmpty() || line.startsWith( "#" )) continue;
			String[] parts = line.split( "\\s+" );
			manifest.put( parts[1], parts[0] );
		}
		assertEquals( "the manifest names the table's sounds", new TreeSet<>( RoomAcoustics.sounds() ), manifest.keySet() );
		for (String id : RoomAcoustics.sounds()){
			byte[] digest = MessageDigest.getInstance( "SHA-1" ).digest( Files.readAllBytes( new File( ASSETS, id ).toPath() ) );
			StringBuilder hex = new StringBuilder();
			for (byte b : digest) hex.append( String.format( "%02x", b ) );
			assertEquals( id + " changed since its room acoustics were made: run python3 tools/room_acoustics_gen.py",
					manifest.get( id ), hex.toString() );
		}
	}
}
