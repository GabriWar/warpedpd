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

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Fauna.Air;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Fauna.Kind;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Who lives where, and when (docs/ambience.md, "The critters of each place"): the table is
 * the docs', the night's life keeps the dark hours, the vault keeps quiet, and the caps hold
 * a floor to a handful of creatures.
 */
public class FaunaTest {

	@Test
	public void everyPlaceBelowGroundHasItsLifeAndTheOverworldNone(){
		for (Place p : Place.values()){
			if (p.underground()){
				assertTrue( p + " has no life", Fauna.kinds( p ).length > 0 );
			} else {
				assertEquals( p + " is the overworld's", 0, Fauna.kinds( p ).length );
				assertEquals( 0, Fauna.air( p ).length );
			}
		}
		assertEquals( 0, Fauna.kinds( null ).length );
		assertEquals( 0, Fauna.air( null ).length );
	}

	@Test
	public void theTableIsTheDocs(){
		assertArrayEquals( new Kind[]{ Kind.FROG, Kind.ROACH, Kind.SNAIL, Kind.STRIDER }, Fauna.kinds( Place.SEWERS ) );
		assertArrayEquals( new Air[]{ Air.GNATS, Air.DRIPS }, Fauna.air( Place.SEWERS ) );
		assertArrayEquals( new Kind[]{ Kind.MOTH, Kind.MOUSE, Kind.ROACH, Kind.SPIDER }, Fauna.kinds( Place.PRISON ) );
		assertArrayEquals( new Air[]{ Air.FLIES }, Fauna.air( Place.PRISON ) );
		assertArrayEquals( new Kind[]{ Kind.NEWT, Kind.CENTIPEDE, Kind.BEETLE, Kind.FISH }, Fauna.kinds( Place.CAVES ) );
		assertArrayEquals( new Air[]{ Air.GLOW_WORMS, Air.SPORES, Air.DRIPS }, Fauna.air( Place.CAVES ) );
		assertArrayEquals( new Kind[]{ Kind.MOTH, Kind.LIZARD, Kind.SILVERFISH, Kind.SWIFT, Kind.BUTTERFLY }, Fauna.kinds( Place.CITY ) );
		assertEquals( 0, Fauna.air( Place.CITY ).length );
		assertArrayEquals( new Kind[]{ Kind.SALAMANDER, Kind.EMBER_BEETLE, Kind.MOTH, Kind.CROW }, Fauna.kinds( Place.HALLS ) );
		assertArrayEquals( new Air[]{ Air.EMBERS, Air.FLIES }, Fauna.air( Place.HALLS ) );
		assertArrayEquals( new Kind[]{ Kind.HARE, Kind.MOTH, Kind.BUNTING, Kind.FISH }, Fauna.kinds( Place.FROZEN ) );
		assertEquals( 0, Fauna.air( Place.FROZEN ).length );
		assertArrayEquals( new Kind[]{ Kind.SPIDERLING, Kind.FLY, Kind.SPIDER }, Fauna.kinds( Place.NEST ) );
		assertArrayEquals( new Air[]{ Air.SILK }, Fauna.air( Place.NEST ) );
		assertArrayEquals( new Kind[]{ Kind.MOTH }, Fauna.kinds( Place.TEMPLE ) );
		assertArrayEquals( new Air[]{ Air.DUST }, Fauna.air( Place.TEMPLE ) );
		assertArrayEquals( new Kind[]{ Kind.CENTIPEDE, Kind.BEETLE }, Fauna.kinds( Place.MINES ) );
		assertArrayEquals( new Air[]{ Air.DRIPS }, Fauna.air( Place.MINES ) );
		assertArrayEquals( new Kind[]{ Kind.BUTTERFLY, Kind.FINCH, Kind.HARE }, Fauna.kinds( Place.MEADOW ) );
		assertArrayEquals( new Air[]{ Air.FIREFLIES }, Fauna.air( Place.MEADOW ) );
		assertArrayEquals( new Kind[]{ Kind.GULL, Kind.FISH }, Fauna.kinds( Place.SHORE ) );
		assertEquals( 0, Fauna.air( Place.SHORE ).length );
		assertArrayEquals( new Kind[]{ Kind.ROACH, Kind.MOTH, Kind.SPIDER, Kind.MOUSE }, Fauna.kinds( Place.CATACOMB ) );
		assertArrayEquals( new Air[]{ Air.FLIES, Air.DRIPS }, Fauna.air( Place.CATACOMB ) );
	}

	@Test
	public void theVaultIsTheCityHushed(){
		List<Kind> city = Arrays.asList( Fauna.kinds( Place.CITY ) ), vault = Arrays.asList( Fauna.kinds( Place.VAULT ) );
		assertFalse( "no swifts in the vault", vault.contains( Kind.SWIFT ) );
		assertTrue( city.contains( Kind.SWIFT ) );
		for (Kind k : vault) assertTrue( city.contains( k ) );
		assertEquals( city.size() - 1, vault.size() );
		assertEquals( 0.5f, Fauna.rate( Place.VAULT ), 0f );
		for (Place p : Place.values()) if (p != Place.VAULT) assertEquals( 1f, Fauna.rate( p ), 0f );
	}

	@Test
	public void mostOfTheNightsLifeIsOutAtDuskAndNight(){
		EnumSet<Kind> night = EnumSet.of( Kind.FROG, Kind.ROACH, Kind.MOUSE, Kind.MOTH, Kind.SNAIL, Kind.NEWT,
				Kind.CENTIPEDE, Kind.SILVERFISH );
		for (Kind k : night){
			assertEquals( k + " at night", 1f, Fauna.hour( k, Phase.NIGHT ), 0f );
			assertEquals( k + " at dusk", 1f, Fauna.hour( k, Phase.DUSK ), 0f );
			assertTrue( k + " by day", Fauna.hour( k, Phase.DAY ) > 0f && Fauna.hour( k, Phase.DAY ) < 0.5f );
			assertTrue( Fauna.hour( k, Phase.DAWN ) < 0.5f );
		}
		//the day's birds and butterflies are home at night
		for (Kind k : new Kind[]{ Kind.SWIFT, Kind.BUNTING, Kind.BUTTERFLY, Kind.FINCH, Kind.GULL }){
			assertEquals( k + " at night", 0f, Fauna.hour( k, Phase.NIGHT ), 0f );
			assertEquals( k + " by day", 1f, Fauna.hour( k, Phase.DAY ), 0f );
		}
		assertEquals( 0f, Fauna.hour( Kind.BUTTERFLY, Phase.DUSK ), 0f );
		//the deep things keep no hours
		for (Kind k : new Kind[]{ Kind.SPIDER, Kind.SPIDERLING, Kind.FLY, Kind.SALAMANDER, Kind.EMBER_BEETLE, Kind.BEETLE, Kind.FISH }){
			for (Phase ph : Phase.values()) assertEquals( k + " at " + ph, 1f, Fauna.hour( k, ph ), 0f );
		}
		//gnats dance at dusk and night only, fireflies light at night
		for (Phase ph : Phase.values()){
			assertEquals( ph == Phase.DUSK || ph == Phase.NIGHT ? 1f : 0f, Fauna.hour( Air.GNATS, ph ), 0f );
		}
		assertEquals( 1f, Fauna.hour( Air.FIREFLIES, Phase.NIGHT ), 0f );
		assertEquals( 0f, Fauna.hour( Air.FIREFLIES, Phase.DAY ), 0f );
		assertTrue( Fauna.hour( Air.GLOW_WORMS, Phase.NIGHT ) > Fauna.hour( Air.GLOW_WORMS, Phase.DAY ) );
		for (Air a : new Air[]{ Air.DRIPS, Air.SPORES, Air.EMBERS, Air.SILK, Air.DUST }){
			for (Phase ph : Phase.values()) assertEquals( 1f, Fauna.hour( a, ph ), 0f );
		}
	}

	@Test
	public void capsKeepAFloorToAHandful(){
		assertEquals( 10, Fauna.GLOBAL_CAP );
		for (Kind k : Kind.values()){
			assertTrue( k + " cap", k.cap >= 1 && k.cap <= 4 );
			assertTrue( k + " chance", k.chance > 0f && k.chance <= 0.5f );
			assertTrue( k + " ring", k.near >= 1 && k.near <= k.far && k.far <= DungeonLife.REACH );
			assertTrue( k + " reaches", k.scare >= k.pass && k.noise >= k.scare );
		}
		//the temple keeps only a few moths
		assertEquals( 2, Fauna.cap( Kind.MOTH, Place.TEMPLE ) );
		assertEquals( Kind.MOTH.cap, Fauna.cap( Kind.MOTH, Place.PRISON ) );
		//the birds that come down in flocks are counted by the flock; those on statues by the bird
		EnumSet<Kind> flocks = EnumSet.noneOf( Kind.class );
		for (Kind k : Kind.values()) if (k.flocks()) flocks.add( k );
		assertEquals( EnumSet.of( Kind.BUNTING, Kind.FINCH, Kind.GULL ), flocks );
		//nothing sends a leaping fish off; a stuck fly only struggles
		assertEquals( 0f, Kind.FISH.scare, 0f );
		assertTrue( Kind.FLY.scare > 0f );
		for (Air a : Air.values()){
			assertTrue( a + " every", a.every > 0f && a.every <= a.most );
			assertTrue( a + " ring", a.near <= a.far && a.far <= DungeonLife.REACH );
		}
		assertTrue( Air.GNATS.cloud && Air.FLIES.cloud );
		assertFalse( Air.DRIPS.cloud );
	}

	@Test
	public void theMothsTakeTheColourOfTheirLight(){
		assertEquals( Fauna.MOTH_CINDER, Fauna.mothTint( Place.HALLS ) );
		assertEquals( Fauna.MOTH_FROST, Fauna.mothTint( Place.FROZEN ) );
		for (Place p : new Place[]{ Place.PRISON, Place.CITY, Place.VAULT, Place.TEMPLE, Place.CATACOMB }){
			assertEquals( Fauna.MOTH_CREAM, Fauna.mothTint( p ) );
		}
	}
}
