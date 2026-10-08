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

package xyz.gabriwar.warpedpixeldungeon.levels.ambience;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The ambience's sounds on disk and how they are placed: every take of every AmbientSound is
 * there, short enough for Android's SoundPool, and as long as its `length` says (the beds start
 * their next play by it); the prison's chains are the game's own effect, loaded at boot; a
 * source falls off with distance and pans to its side, both bounded.
 */
public class AmbientPlayerTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	/**
	 * An MP3's length in seconds, counted frame by frame (MPEG 1, 2 or 2.5, layer III). The
	 * encoder's own header frame and padding come in it: a few hundredths over what is heard.
	 */
	static double seconds( byte[] b ){
		int i = 0;
		if (b.length > 10 && b[0] == 'I' && b[1] == 'D' && b[2] == '3'){
			i = 10 + (((b[6] & 0x7F) << 21) | ((b[7] & 0x7F) << 14) | ((b[8] & 0x7F) << 7) | (b[9] & 0x7F));
		}
		final int[] mpeg1 = { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320 };
		final int[] mpeg2 = { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160 };
		final int[] rates = { 44100, 48000, 32000 };
		double secs = 0;
		while (i + 4 <= b.length){
			int h = ((b[i] & 0xFF) << 24) | ((b[i + 1] & 0xFF) << 16) | ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
			int version = (h >>> 19) & 3, layer = (h >>> 17) & 3, bitrate = (h >>> 12) & 15, rate = (h >>> 10) & 3;
			if ((h & 0xFFE00000) != 0xFFE00000 || version == 1 || layer != 1 || bitrate == 0 || bitrate == 15 || rate == 3){
				i++;
				continue;
			}
			boolean one = version == 3;
			int hz = rates[rate] >> (one ? 0 : version == 2 ? 1 : 2);
			int samples = one ? 1152 : 576;
			int size = (samples / 8) * (one ? mpeg1[bitrate] : mpeg2[bitrate]) * 1000 / hz + ((h >>> 9) & 1);
			secs += (double) samples / hz;
			i += size;
		}
		return secs;
	}

	private static double secondsOf( String asset ){
		FileHandle f = Gdx.files.internal( asset );
		assertTrue( "no " + asset + " in the assets", f.exists() );
		return seconds( f.readBytes() );
	}

	@Test
	public void everyTakeIsThereShortAndAsLongAsItsLength(){
		HashSet<String> all = new HashSet<>( Arrays.asList( AmbientSound.assets() ) );
		int takes = 0;
		for (AmbientSound s : AmbientSound.values()){
			assertTrue( s + " is short enough for SoundPool", s.length <= 7f );
			assertTrue( s.gain > 0f && s.gain <= 1f );
			double longest = 0;
			for (String t : s.takes){
				assertTrue( all.contains( t ) );
				double d = secondsOf( t );
				assertTrue( t + " is " + d + "s", d <= 7.0 );
				longest = Math.max( longest, d );
				takes++;
			}
			//its length is the longest take's, give or take the encoder's frame of its own
			assertEquals( s + "'s longest take", s.length, longest, 0.15 );
		}
		assertEquals( "every take is loaded at boot", takes, all.size() );
	}

	@Test
	public void everyTakeOfABedRunsItsWholeLength(){
		//a bed starts its next play a fixed lead before the last one's end: a shorter take would
		//leave a gap after it, every time it came up
		HashSet<AmbientSound> beds = new HashSet<>();
		for (Place p : Place.values()){
			for (Voice v : AmbientSounds.voices( p )){
				if (v.bed) beds.add( v.sound );
				if (v.alt != null) beds.add( v.alt.sound );
			}
		}
		assertTrue( beds.contains( AmbientSound.LAVA ) && beds.contains( AmbientSound.POUR ) );
		for (AmbientSound s : beds){
			for (String t : s.takes) assertEquals( t, s.length, secondsOf( t ), 0.15 );
		}
	}

	@Test
	public void theChainsAreTheGamesOwnAndAsLong(){
		assertTrue( Arrays.asList( Assets.Sounds.all ).contains( Assets.Sounds.CHAINS ) );
		assertEquals( AmbientSounds.CHAINS_LENGTH, secondsOf( Assets.Sounds.CHAINS ), 0.15 );
	}

	@Test
	public void aSourceFallsOffWithDistance(){
		assertEquals( 1f, AmbientPlayer.falloff( 0f ), 0f );
		assertEquals( 1f, AmbientPlayer.falloff( -2f ), 0f );
		assertEquals( "half at HALF_CELLS", 0.5f, AmbientPlayer.falloff( AmbientPlayer.HALF_CELLS ), 1e-6f );
		assertEquals( "gone from HEARD_CELLS", 0f, AmbientPlayer.falloff( AmbientPlayer.HEARD_CELLS ), 0f );
		assertEquals( 0f, AmbientPlayer.falloff( 40f ), 0f );
		float last = 1f;
		for (float d = 0f; d <= 20f; d += 0.1f){
			float f = AmbientPlayer.falloff( d );
			assertTrue( f >= 0f && f <= 1f );
			assertTrue( "never louder further off: " + d, f <= last + 1e-6f );
			last = f;
		}
		//the last stretch fades out rather than cutting off
		assertTrue( AmbientPlayer.falloff( AmbientPlayer.HEARD_CELLS - 0.5f ) < 0.05f );
		assertTrue( AmbientPlayer.falloff( AmbientPlayer.HEARD_CELLS - 0.5f ) > 0f );
	}

	@Test
	public void aSourcePansToItsSideNeverFullyIntoOneEar(){
		assertEquals( 0f, AmbientPlayer.pan( 0f ), 0f );
		float last = -1f;
		for (float dx = -20f; dx <= 20f; dx += 0.5f){
			float p = AmbientPlayer.pan( dx );
			assertTrue( Math.abs( p ) <= AmbientPlayer.MAX_PAN );
			assertTrue( "further right, further right", p >= last );
			assertEquals( "the same either side", -p, AmbientPlayer.pan( -dx ), 1e-6f );
			assertTrue( "to its own side", dx == 0f || Math.signum( p ) == Math.signum( dx ) );
			last = p;
		}
		assertEquals( AmbientPlayer.MAX_PAN, AmbientPlayer.pan( AmbientPlayer.PAN_CELLS ), 1e-6f );
		assertEquals( AmbientPlayer.MAX_PAN, AmbientPlayer.pan( 100f ), 1e-6f );
	}
}
