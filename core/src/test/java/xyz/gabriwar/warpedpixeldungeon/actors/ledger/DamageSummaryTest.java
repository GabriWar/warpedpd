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

package xyz.gabriwar.warpedpixeldungeon.actors.ledger;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The Damage tab's numbers (DamageSummary): its summary over a list, and how a step's change and
 * value read. Plain records, no game.
 */
public class DamageSummaryTest {

	private static final float EPS = 0.0001f;

	//a hit that showed `shown`, `shield` of it on a shield, of a kind and a source icon
	private static HitRecord hit( int kind, int shown, int shield, int sourceIcon ){
		HitRecord r = new HitRecord();
		r.kind = kind;
		r.outcome = HitRecord.HIT;
		r.shown = shown;
		r.shieldLost = shield;
		r.hpLost = shown - shield;
		r.sourceIcon = sourceIcon;
		r.steps = new Step[]{ new Step( Step.BASE, Step.ATTACKER, "base", Step.NO_ICON, 0, shown, shown ) };
		return r;
	}

	private static Roll roll( boolean hit ){
		Roll ro = new Roll();
		ro.hit = hit;
		ro.accStat = 10;
		ro.defStat = 5;
		return ro;
	}

	private static HitRecord miss(){
		HitRecord r = new HitRecord();
		r.kind = HitRecord.BLOW;
		r.outcome = HitRecord.MISS;
		r.roll = roll( false );
		return r;
	}

	@Test
	public void anEmptyListHasNothingUnderItsRatios(){
		DamageSummary s = new DamageSummary( Collections.<HitRecord>emptyList() );
		assertEquals( 0, s.count );
		assertEquals( 0, s.landed );
		assertNull( s.top );
		assertFalse( DamageSummary.known( s.average ) );
		assertFalse( DamageSummary.known( s.crits ) );
		assertFalse( DamageSummary.known( s.hits ) );
		assertFalse( DamageSummary.known( s.blocked ) );
		assertEquals( 0, s.sourceIcons.length );
		assertEquals( 0f, s.otherShare, EPS );
	}

	@Test
	public void averageAndTopCountOnlyWhatLanded(){
		HitRecord a = hit( HitRecord.BLOW, 4, 0, 0 );
		HitRecord b = hit( HitRecord.BLOW, 10, 0, 0 );
		HitRecord c = hit( HitRecord.DIRECT, 6, 0, 2 );
		HitRecord immune = new HitRecord();
		immune.outcome = HitRecord.IMMUNE;
		DamageSummary s = new DamageSummary( Arrays.asList( a, miss(), b, immune, c ) );
		assertEquals( 5, s.count );
		assertEquals( 3, s.landed );
		assertEquals( 20, s.shown );
		assertEquals( 20f / 3f, s.average, EPS );
		assertSame( b, s.top );
	}

	@Test
	public void theNewestOfEqualTopsIsKept(){
		HitRecord newer = hit( HitRecord.BLOW, 9, 0, 0 );
		HitRecord older = hit( HitRecord.BLOW, 9, 0, 0 );
		assertSame( newer, new DamageSummary( Arrays.asList( newer, older ) ).top );
	}

	@Test
	public void critsAreCritBlowsOverLandedBlows(){
		HitRecord crit = hit( HitRecord.BLOW, 12, 0, 0 );
		crit.crit = true;
		HitRecord plain = hit( HitRecord.BLOW, 5, 0, 0 );
		HitRecord zap = hit( HitRecord.DIRECT, 7, 0, 2 );
		HitRecord missed = miss();
		DamageSummary s = new DamageSummary( Arrays.asList( crit, plain, zap, missed ) );
		assertEquals( 0.5f, s.crits, EPS );
		assertEquals( 50, DamageSummary.percent( s.crits ) );
	}

	@Test
	public void hitsAreRolledHitsOverEverythingRolled(){
		HitRecord a = hit( HitRecord.BLOW, 3, 0, 0 );
		a.roll = roll( true );
		HitRecord b = hit( HitRecord.BLOW, 3, 0, 0 );
		b.roll = roll( true );
		HitRecord dot = hit( HitRecord.DOT, 1, 0, 6 );
		DamageSummary s = new DamageSummary( Arrays.asList( a, miss(), b, dot ) );
		assertEquals( 3, s.rolled );
		assertEquals( 2f / 3f, s.hits, EPS );
		//taken: the dodges are what is left
		assertEquals( 33, DamageSummary.percent( 1f - s.hits ) );
	}

	@Test
	public void blockedIsArmourAndShieldsOverAllTheyFaced(){
		//10 came in, armour cut 4, a shield took 2 of the 6 left, health 4
		HitRecord r = new HitRecord();
		r.kind = HitRecord.BLOW;
		r.outcome = HitRecord.HIT;
		r.steps = new Step[]{
				new Step( Step.BASE, Step.ATTACKER, "base", Step.NO_ICON, 0, 10, 10 ),
				new Step( Step.ADD, Step.DEFENDER, "Armor", DamageSummary.ARMOR_CELL, 0, 10, 6 ) };
		r.shown = 6;
		r.shieldLost = 2;
		r.hpLost = 4;
		assertTrue( r.sums() );
		assertEquals( 4f, DamageSummary.armorCut( r ), EPS );
		DamageSummary s = new DamageSummary( Collections.singletonList( r ) );
		assertEquals( 4f, s.armorTaken, EPS );
		assertEquals( 2, s.shieldTaken );
		//of the 10 that came in, armour's 4 and the shield's 2 were kept off health
		assertEquals( 6f / 10f, s.blocked, EPS );

		//a miss and an immunity faced nothing: nothing blocked, and a hit with no armour none
		HitRecord bare = hit( HitRecord.BLOW, 5, 0, 0 );
		assertEquals( 0f, new DamageSummary( Arrays.asList( bare, miss() ) ).blocked, EPS );
	}

	@Test
	public void theThreeLargestSourcesAndTheRest(){
		List<HitRecord> l = new ArrayList<>();
		l.add( hit( HitRecord.BLOW, 50, 0, 0 ) );
		l.add( hit( HitRecord.DIRECT, 25, 0, 2 ) );
		l.add( hit( HitRecord.DOT, 10, 0, 6 ) );
		l.add( hit( HitRecord.DOT, 5, 0, 6 ) );
		l.add( hit( HitRecord.DOT, 6, 0, 13 ) );
		l.add( hit( HitRecord.DOT, 4, 0, 10 ) );
		DamageSummary s = new DamageSummary( l );
		assertArrayEquals( new int[]{ 0, 2, 6 }, s.sourceIcons );
		assertEquals( 0.5f, s.sourceShares[0], EPS );
		assertEquals( 0.25f, s.sourceShares[1], EPS );
		assertEquals( 0.15f, s.sourceShares[2], EPS );
		assertEquals( 0.10f, s.otherShare, EPS );

		//one source: all of it, nothing else
		DamageSummary one = new DamageSummary( Collections.singletonList( hit( HitRecord.BLOW, 3, 0, 0 ) ) );
		assertArrayEquals( new int[]{ 0 }, one.sourceIcons );
		assertEquals( 1f, one.sourceShares[0], EPS );
		assertEquals( 0f, one.otherShare, EPS );
	}

	@Test
	public void valuesReadWithAtMostTwoDecimals(){
		assertEquals( "12", DamageSummary.value( 12f, '.' ) );
		assertEquals( "12.25", DamageSummary.value( 12.25f, '.' ) );
		assertEquals( "13.2", DamageSummary.value( 13.2f, '.' ) );
		assertEquals( "13.2", DamageSummary.value( 13.200001f, '.' ) );
		assertEquals( "0.05", DamageSummary.value( 0.05f, '.' ) );
		assertEquals( "-1", DamageSummary.value( -1f, '.' ) );
		assertEquals( "-0.5", DamageSummary.value( -0.5f, '.' ) );
		assertEquals( "0", DamageSummary.value( 0.004f, '.' ) );
		assertEquals( "0", DamageSummary.value( -0f, '.' ) );
		assertEquals( "12,25", DamageSummary.value( 12.25f, ',' ) );
		//a stat that cannot miss or cannot be hit reads as endless, not as a million
		assertEquals( "∞", DamageSummary.value( xyz.gabriwar.warpedpixeldungeon.actors.Char.INFINITE_EVASION, '.' ) );
		assertEquals( "= ∞", DamageSummary.change( new Step( Step.SET, Step.WORLD, "Parry", Step.NO_ICON, 0, 7, xyz.gabriwar.warpedpixeldungeon.actors.Char.INFINITE_EVASION ), '.' ) );
	}

	@Test
	public void multipliersKeepAtLeastOneDecimal(){
		assertEquals( "x1.5", DamageSummary.factor( 1.5f, '.' ) );
		assertEquals( "x1.33", DamageSummary.factor( 4f / 3f, '.' ) );
		assertEquals( "x2.0", DamageSummary.factor( 2f, '.' ) );
		assertEquals( "x0.75", DamageSummary.factor( 0.75f, '.' ) );
		assertEquals( "x0.05", DamageSummary.factor( 0.05f, '.' ) );
		assertEquals( "x0.005", DamageSummary.factor( 0.005f, '.' ) );
		assertEquals( "x1,1", DamageSummary.factor( 1.1f, ',' ) );
	}

	@Test
	public void eachStepReadsByItsKind(){
		assertEquals( "", DamageSummary.change( new Step( Step.BASE, 0, "b", Step.NO_ICON, 0, 8, 8 ), '.' ) );
		assertEquals( "x1.5", DamageSummary.change( new Step( Step.MUL, 0, "m", Step.NO_ICON, 0, 8, 12 ), '.' ) );
		assertEquals( "+4", DamageSummary.change( new Step( Step.ADD, 0, "a", Step.NO_ICON, 0, 12, 16 ), '.' ) );
		assertEquals( "-1", DamageSummary.change( new Step( Step.ADD, 0, "a", Step.NO_ICON, 0, 16, 15 ), '.' ) );
		assertEquals( "+0.5", DamageSummary.change( new Step( Step.ADD, 0, "a", Step.NO_ICON, 0, 2, 2.5f ), '.' ) );
		assertEquals( "= 1", DamageSummary.change( new Step( Step.SET, 0, "s", Step.NO_ICON, 0, 3, 1 ), '.' ) );
		assertEquals( "≈", DamageSummary.change( new Step( Step.ROUND, 0, "r", Step.NO_ICON, 0, 13.2f, 13 ), '.' ) );
		//a multiplier on nothing has no factor: it reads as what it set
		assertEquals( "= 5", DamageSummary.change( new Step( Step.MUL, 0, "m", Step.NO_ICON, 0, 0, 5 ), '.' ) );
	}

	@Test
	public void moodIsGoodOrBadForTheHero(){
		Step more = new Step( Step.ADD, 0, "a", Step.NO_ICON, 0, 4, 6 );
		Step less = new Step( Step.MUL, 0, "m", Step.NO_ICON, 0, 6, 3 );
		Step round = new Step( Step.ROUND, 0, "r", Step.NO_ICON, 0, 6.4f, 6 );
		Step same = new Step( Step.MUL, 0, "m", Step.NO_ICON, 0, 6, 6 );
		//dealt: more is good
		assertEquals( 1, DamageSummary.mood( more, true ) );
		assertEquals( -1, DamageSummary.mood( less, true ) );
		//taken: less is good
		assertEquals( -1, DamageSummary.mood( more, false ) );
		assertEquals( 1, DamageSummary.mood( less, false ) );
		assertEquals( 0, DamageSummary.mood( round, true ) );
		assertEquals( 0, DamageSummary.mood( same, false ) );
	}
}
