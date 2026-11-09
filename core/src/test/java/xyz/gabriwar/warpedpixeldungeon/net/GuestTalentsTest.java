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

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Bundle;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A co-op guest's hero is built from the host's full state with the host's talents on it: the
 * guest who opened the hero window on arrival, before any delta brought them, crashed on a tree
 * with no tiers. With nothing to read, the hero still gets his class's tree.
 */
public class GuestTalentsTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void theHostsTalentsComeWithTheFullState() throws Exception {
		Hero host = new Hero();
		host.heroClass = HeroClass.HUNTRESS;
		Talent.initClassTalents( host );
		host.talents.get( 0 ).put( Talent.NATURES_AID, 2 );
		Bundle tb = new Bundle();
		Talent.storeTalentsInBundle( tb, host );

		Hero guest = new Hero();
		guest.heroClass = HeroClass.HUNTRESS;
		assertTrue( guest.talents.isEmpty() );
		SpectatorReceiver.applyTalents( guest, new JSONObject().put( "talents", tb.toString() ) );
		assertEquals( Talent.MAX_TALENT_TIERS, guest.talents.size() );
		assertEquals( 2, guest.pointsInTalent( Talent.NATURES_AID ) );
	}

	@Test
	public void noTalentsSentStillGivesTheClassTree(){
		Hero guest = new Hero();
		guest.heroClass = HeroClass.WARRIOR;
		SpectatorReceiver.applyTalents( guest, new JSONObject() );
		assertEquals( Talent.MAX_TALENT_TIERS, guest.talents.size() );
		assertTrue( guest.talents.get( 0 ).containsKey( Talent.HEARTY_MEAL ) );

		Hero garbled = new Hero();
		garbled.heroClass = HeroClass.WARRIOR;
		SpectatorReceiver.applyTalents( garbled, new JSONObject().put( "talents", "{not a bundle" ) );
		assertTrue( garbled.talents.get( 1 ).size() > 0 );
	}
}
