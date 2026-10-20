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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The clouds' shadows over the surface, by their rules (no GL): a partly cloudy sky shades the land
 * and neither a clear nor a grey one does, never by a jump; none by night, rising with the sun and
 * gone in the dusk; rain, fog and an eclipse wipe them out; they drift at the sea of clouds' pace;
 * the deck lies on the land, so a rebase of the window (whole decks) moves no shadow, and a pixel of
 * drift east carries every shadow a pixel east.
 */
public class CloudShadowsTest {

	private static final int T = CloudDeck.SIZE;

	//the deck's texel under a scene pixel, the deck lying at this phase
	private static int texel( int scenePx, int phase ){
		return Math.floorMod( scenePx + phase, T );
	}

	@Test
	public void aPartlyCloudySkyShadesTheLandAndNeitherAClearNorAGreyOne(){
		assertEquals( 0f, CloudShadows.coverShare( 0f ), 0f );
		assertEquals( 0f, CloudShadows.coverShare( CloudShadows.COVER_FROM ), 0f );
		float fair = CloudShadows.coverShare( 0.2f );
		assertTrue( "a fair sky's: " + fair, fair > 0.1f && fair < 0.9f );
		for (float c = CloudShadows.COVER_FULL; c <= CloudShadows.COVER_GREY; c += 0.02f){
			assertEquals( "cover " + c, 1f, CloudShadows.coverShare( c ), 0.001f );
		}
		float greying = CloudShadows.coverShare( 0.72f );
		assertTrue( "a greying sky's: " + greying, greying > 0.1f && greying < 0.9f );
		assertEquals( 0f, CloudShadows.coverShare( CloudShadows.COVER_GONE ), 0f );
		assertEquals( 0f, CloudShadows.coverShare( 1f ), 0f );
		//never by a jump: a hundredth more cloud, a little more or less shade
		for (float c = 0f; c < 1f; c += 0.01f){
			assertTrue( "at " + c, Math.abs( CloudShadows.coverShare( c + 0.01f ) - CloudShadows.coverShare( c ) ) < 0.08f );
		}
	}

	@Test
	public void noShadowByNightRisingWithTheSunGoneInTheDusk(){
		for (float p = 0f; p <= 1f; p += 0.1f){
			assertEquals( 1f, CloudShadows.daylight( Phase.DAY, p ), 0f );
			assertEquals( 0f, CloudShadows.daylight( Phase.NIGHT, p ), 0f );
		}
		assertEquals( 0f, CloudShadows.daylight( Phase.DAWN, 0.5f ), 0f );
		assertEquals( 1f, CloudShadows.daylight( Phase.DAWN, 1f ), 0f );
		assertEquals( 1f, CloudShadows.daylight( Phase.DUSK, 0f ), 0f );
		assertEquals( 0f, CloudShadows.daylight( Phase.DUSK, 0.5f ), 0f );
		//rising through the dawn and falling through the dusk, never back
		float last = 0f;
		for (float p = 0f; p <= 1.001f; p += 0.05f){
			float d = CloudShadows.daylight( Phase.DAWN, p );
			assertTrue( d >= last );
			last = d;
		}
		last = 1f;
		for (float p = 0f; p <= 1.001f; p += 0.05f){
			float d = CloudShadows.daylight( Phase.DUSK, p );
			assertTrue( d <= last );
			last = d;
		}
	}

	@Test
	public void rainFogAndAnEclipseWipeThemOut(){
		assertEquals( 1f, CloudShadows.strength( 0.45f, 0f, false, false, Phase.DAY, 0.5f ), 0.001f );
		float drizzle = CloudShadows.strength( 0.45f, CloudShadows.RAIN_GONE / 2f, false, false, Phase.DAY, 0.5f );
		assertTrue( "in a drizzle: " + drizzle, drizzle > 0f && drizzle < 1f );
		assertEquals( 0f, CloudShadows.strength( 0.45f, CloudShadows.RAIN_GONE, false, false, Phase.DAY, 0.5f ), 0f );
		assertEquals( 0f, CloudShadows.strength( 0.45f, 0f, true, false, Phase.DAY, 0.5f ), 0f );
		assertEquals( 0f, CloudShadows.strength( 0.45f, 0f, false, true, Phase.DAY, 0.5f ), 0f );
		assertEquals( 0f, CloudShadows.strength( 0.45f, 0f, false, false, Phase.NIGHT, 0.5f ), 0f );
		//at its strongest, about a sixth darker: subtle
		assertTrue( CloudShadows.SHADE <= 0.2f );
	}

	@Test
	public void theyDriftAtTheSeaOfCloudsPace(){
		assertEquals( 1.5f, CloudShadows.pace( 0f ), 0f );
		assertEquals( 7.5f, CloudShadows.pace( 10f ), 0.001f );
		assertEquals( 12f, CloudShadows.pace( 25f ), 0f );
	}

	@Test
	public void aRebaseMovesNoShadow() throws Exception {
		//the window moves a shift at a time (OverworldLevel.SHIFT_Q cells): whole decks, so even read
		//against the moved window a frame early (the actor moves it before the scene slides) a shadow
		//lies where it lay
		Field q = OverworldLevel.class.getDeclaredField( "SHIFT_Q" );
		q.setAccessible( true );
		int shift = q.getInt( null );
		assertEquals( "a shift is whole decks", 0, (shift * DungeonTilemap.SIZE) % T );
		for (float drift : new float[]{ 0f, 3.7f, -250.2f, 511.9f }){
			for (int origin : new int[]{ -100, 0, 17, 5000 }){
				int before = CloudShadows.phase( origin, drift );
				for (int s : new int[]{ 0, 100, 511, 1000, -37 }){
					//scene pixel s shows the world pixel the scene's s - 512 shows after the slide
					assertEquals( texel( s, before ), texel( s - shift * DungeonTilemap.SIZE, CloudShadows.phase( origin + shift, drift ) ) );
					//and, read a frame early, the same
					assertEquals( texel( s, before ), texel( s, CloudShadows.phase( origin + shift, drift ) ) );
				}
			}
		}
	}

	@Test
	public void aPixelOfDriftEastCarriesEveryShadowAPixelEast(){
		for (int s = -40; s < 600; s += 13){
			assertEquals( texel( s, CloudShadows.phase( 10, 0f ) ), texel( s + 1, CloudShadows.phase( 10, 1f ) ) );
			//and whole texels only: under a pixel it stays put
			assertEquals( texel( s, CloudShadows.phase( 10, 0f ) ), texel( s, CloudShadows.phase( 10, 0.9f ) ) );
		}
	}
}
