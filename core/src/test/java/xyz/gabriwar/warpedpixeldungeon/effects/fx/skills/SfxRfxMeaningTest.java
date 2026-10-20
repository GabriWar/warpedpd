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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ArrowRain;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.HeartseekerArrow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillSequence;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoulMark;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Blackout;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BloodDance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.EmberArrows;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.FrostNova;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Grace;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.HealingPrayer;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Litany;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MassHeal;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Purify;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SoulDrain;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFieldFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.DreadFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LinkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillRay;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The second pass's colours by meaning (docs/fx/skills.md, the palette): the cleric's mends flowing
 * in the heal's green and landing in its red crosses, Litany's spoken runes, the mana gains drawing
 * mana in, Grace's mana bloom, Skill Sequence's dawn mending in sight, Blood Dance and Heartseeker in
 * the blood's and the heart's reds, Ember Arrows and the stance auras in fire's orange, Double Shot's
 * mark in the huntress's green, Frost Nova's ice. Each touched file draws the game's Random where and
 * as often as f0783eacd did (its draw sites, and the seeded next value after each moment played for
 * real); no heal picture is paler than the heal's A8FFC0 or in the old sage, no blood is pink, no
 * fire salmon, no mana lilac; every mana number is BlowPresenter.MANA_COLOR; the dawn's crosses come
 * only where HP rose; nothing touches the GPU while made; a loud holy-heal moment keeps every cap.
 */
public class SfxRfxMeaningTest {

	static final String BASE = "src/main/java/xyz/gabriwar/warpedpixeldungeon/";

	// ------------------------------------------------------------------ the game's Random

	/** RfxMeaningProcs measured at f0783eacd (RFX_BASELINE=1 in a detached f0783eacd checkout):
	 *  each moment's draws and the next value after them (x 1e6). */
	static final Object[][] BASELINE = {
			{ "mass-heal", 39, 147075 },
			{ "litany", 78, 763695 },
			{ "healing-prayer", 36, 821230 },
			{ "purify", 43, 632846 },
			{ "grace", 16, 760961 },
			{ "faith", 30, 967827 },
			{ "meditation", 15, 910356 },
			{ "serene-focus", 29, 280844 },
			{ "spirituality", 44, 638036 },
			{ "inner-peace", 66, 797313 },
			{ "dawn", 18, 660644 },
			{ "blood-dance", 24, 488876 },
			{ "heartseeker", 37, 646833 },
			{ "undying-will", 62, 642585 },
			{ "ember-arrows", 180, 542264 },
			{ "frost-nova", 314, 718205 },
			{ "auras", 84, 184849 } };

	@Test
	public void everyMomentDrawsTheGamesRandomAsF0783Did() throws Exception {
		LinkedHashMap<String, long[]> now = RfxMeaningProcs.measure();
		assertEquals( BASELINE.length, now.size() );
		for (Object[] b : BASELINE){
			long[] n = now.get( (String) b[0] );
			assertNotNull( (String) b[0], n );
			assertEquals( b[0] + " draws", ((Integer) b[1]).longValue(), n[0] );
			assertEquals( b[0] + " next", ((Integer) b[2]).longValue(), n[1] );
		}
	}

	/** Every draw site of the game's Random in the package's files, by kind, as f0783eacd has them:
	 *  bursts, starts, delayed starts, pours, lightnings and their arcs, bolts, rains, Random calls,
	 *  splashes (a plain emitter's quantity is pinned by the moments above). */
	static final Object[][] CENSUS = {
			{ "actors/hero/skills/MassHeal.java", new int[]{ 3, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
			{ "actors/hero/skills/Litany.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
			{ "actors/hero/skills/HealingPrayer.java", new int[]{ 3, 1, 0, 0, 0, 0, 0, 0, 0, 0 } },
			{ "actors/hero/skills/Purify.java", new int[]{ 3, 0, 0, 0, 0, 0, 0, 0, 1, 0 } },
			{ "actors/hero/skills/Grace.java", new int[]{ 1, 0, 0, 0, 0, 0, 2, 0, 0, 0 } },
			{ "actors/hero/skills/Faith.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
			{ "actors/hero/skills/Meditation.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
			{ "actors/hero/skills/SereneFocus.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
			{ "actors/hero/skills/Spirituality.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
			{ "actors/hero/skills/InnerPeace.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 1, 0 } },
			{ "actors/hero/skills/GuardianSpirit.java", new int[]{ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 } },
			{ "actors/hero/skills/BloodDance.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 1, 1 } },
			{ "actors/buffs/HeartseekerArrow.java", new int[]{ 3, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
			{ "actors/hero/skills/UndyingWill.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
			{ "actors/buffs/UndyingWillWard.java", new int[]{ 4, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
			{ "actors/hero/skills/EmberArrows.java", new int[]{ 5, 1, 1, 0, 0, 0, 1, 0, 0, 0 } },
			{ "effects/skillfx/RogueHuntressAuras.java", new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
			{ "actors/hero/skills/DoubleShot.java", new int[]{ 4, 0, 0, 0, 0, 0, 0, 0, 1, 0 } },
			{ "actors/hero/skills/FrostNova.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 1, 0 } },
			{ "actors/buffs/SkillSequence.java", new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } } };

	static final String[] SITES = { "\\.burst\\s*\\(", "(?<!FxTimeline)\\.start\\s*\\(", "\\.startDelayed\\s*\\(", "\\.pour\\s*\\(",
			"new\\s+Lightning\\s*\\(", "new\\s+Lightning\\.Arc\\s*\\(",
			"(MagicMissile\\.boltFromChar|\\(\\s*\\(\\s*MagicMissile\\s*\\)[^;]*?\\.reset\\s*\\(|SkillBolt\\.fly\\s*\\()",
			"SkillFX\\.rain\\s*\\(",
			"(?<![A-Za-z])Random\\.(Int|Float|NormalIntRange|NormalFloat|IntRange|Long|chances|element|shuffle|oneOf|chance)\\b",
			"Splash\\.at\\s*\\(" };

	@Test
	public void everyDrawSiteIsWhereF0783HadIt() throws Exception {
		for (Object[] row : CENSUS){
			String src = source( (String) row[0] );
			int[] want = (int[]) row[1];
			for (int k = 0; k < SITES.length; k++){
				assertEquals( row[0] + " " + SITES[k], want[k], count( SITES[k], src ) );
			}
		}
		//the swapped start keeps its interval and count: the guardian's pillar, holy motes for shafts
		String guardian = source( "actors/hero/skills/GuardianSpirit.java" );
		assertTrue( guardian.contains( "start(xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts.HOLY_MOTE,0.12f,5)" ) );
		assertFalse( guardian.contains( "ShaftParticle" ) );
	}

	/** No PixelFlare, ClosingRing, legacy Flare or FxLight more than f0783eacd's in any of them. */
	@Test
	public void noNewFlareRingOrLightOnTheseSkills() throws Exception {
		Object[][] flares = {
				{ "actors/hero/skills/MassHeal.java", new int[]{ 1, 0, 0, 0 } },
				{ "actors/hero/skills/Faith.java", new int[]{ 0, 0, 1, 0 } },
				{ "actors/hero/skills/Meditation.java", new int[]{ 0, 0, 2, 0 } },
				{ "actors/hero/skills/BloodDance.java", new int[]{ 1, 0, 0, 0 } },
				{ "actors/buffs/HeartseekerArrow.java", new int[]{ 2, 1, 0, 0 } },
				{ "actors/hero/skills/UndyingWill.java", new int[]{ 2, 0, 0, 0 } },
				{ "actors/hero/skills/EmberArrows.java", new int[]{ 2, 0, 0, 1 } },
				{ "actors/hero/skills/DoubleShot.java", new int[]{ 1, 0, 0, 0 } },
				{ "actors/hero/skills/FrostNova.java", new int[]{ 0, 0, 1, 0 } } };
		String[] what = { "PixelFlare\\.show\\s*\\(", "ClosingRing\\.on\\s*\\(", "new\\s+Flare\\s*\\(", "FxLight\\.(glow|pool)\\s*\\(" };
		for (Object[] row : CENSUS){
			int[] want = { 0, 0, 0, 0 };
			for (Object[] f : flares) if (f[0].equals( row[0] )) want = (int[]) f[1];
			String src = source( (String) row[0] );
			for (int k = 0; k < what.length; k++) assertEquals( row[0] + " " + what[k], want[k], count( what[k], src ) );
		}
	}

	// ------------------------------------------------------------------ the colours by meaning

	@Test
	public void noDriftedColourIsLeftInTheseFiles() throws Exception {
		//the old sage and its pale greens (heals read cream), the pink blood, the salmon fire and
		//fury, the lilac mana numbers, a white purge
		String[] gone = { "B3D699", "DDEFD0", "EAFFEA", "F08A9E", "D94969", "DF9979", "8AC0FF", "FF9A4A", "FFC070" };
		for (Object[] row : CENSUS){
			String src = source( (String) row[0] ).toUpperCase();
			for (String g : gone) assertFalse( row[0] + " still uses " + g, src.contains( g ) );
		}
		for (String heal : new String[]{ "MassHeal", "Litany", "HealingPrayer", "Purify" }){
			assertFalse( heal + ": white", source( "actors/hero/skills/" + heal + ".java" ).toUpperCase().contains( "0XFFFFFF" ) );
		}
	}

	@Test
	public void theConstantsAreThePalettes() throws Exception {
		assertEquals( SkillPalette.HEAL_LIGHT, field( MassHeal.class, "HEAL" ) );
		assertEquals( 0x58E08C, SkillPalette.HEAL_LIGHT );
		assertEquals( 0xA8FFC0, SkillPalette.HEAL.stop( 0 ) );
		assertEquals( 0xD01818, BloodDance.CRIMSON );
		assertEquals( 0xFF5050, BloodDance.RIBBON_INNER );
		assertEquals( 0x8A0A14, BloodDance.LIFT.stop( 1 ) );
		assertEquals( 0xCC0000, HeartseekerArrow.CRIMSON );
		assertEquals( 0xD01818, HeartseekerArrow.HEART_LIGHT );
		assertEquals( 0xFF8418, EmberArrows.HEAT );
		assertEquals( 0xFFD24A, EmberArrows.FLARE );
		//the auras' motes: the huntress's leaf green, a fuse's hot gold, fire's orange
		String auras = source( "effects/skillfx/RogueHuntressAuras.java" );
		assertEquals( 2, count( "SkillPalette\\.NATURE_LIGHT", auras ) );
		assertTrue( auras.contains( "SkillPalette.EMBER_HOT, SkillBursts.FUSE" ) );
		assertTrue( auras.contains( "SkillPalette.FIRE.stop( 1 ), SkillBursts.EMBER_MOTE" ) );
		//the mends flow as the heal's ray, Purify's as a short holy one
		assertTrue( source( "actors/hero/skills/MassHeal.java" ).contains( "SkillRay.show( hero.sprite, ch.sprite, SkillRay.HEAL, FLOW )" ) );
		assertTrue( source( "actors/hero/skills/Litany.java" ).contains( "SkillRay.show( hero.sprite, ch.sprite, SkillRay.HEAL, MassHeal.FLOW )" ) );
		assertTrue( source( "actors/hero/skills/Purify.java" ).contains( "SkillRay.show( hero.sprite, ally.sprite, SkillRay.HOLY, CLEANSE )" ) );
		assertEquals( 0.6f, MassHeal.FLOW, 0f );
		assertEquals( 0.3f, Purify.CLEANSE, 0f );
	}

	@Test
	public void everyManaNumberIsTheGamesManaBlue() throws Exception {
		Pattern text = Pattern.compile( "showStatusWithIcon\\(\\s*([^,]+),[^;]*FloatingText\\.MANA" );
		int texts = 0;
		for (Object[] row : CENSUS){
			Matcher m = text.matcher( source( (String) row[0] ) );
			while (m.find()){
				texts++;
				assertTrue( row[0] + ": " + m.group( 1 ), m.group( 1 ).trim().endsWith( "BlowPresenter.MANA_COLOR" ) );
			}
		}
		assertEquals( "Grace, Faith, Meditation, Spirituality", 4, texts );
	}

	@Test
	public void runesOnlyOnTheSpokenPrayerNeverOnAHealOrAManaGain() throws Exception {
		for (Object[] row : CENSUS){
			String src = source( (String) row[0] );
			if (row[0].equals( "actors/hero/skills/Litany.java" )){
				assertEquals( 1, count( "RuneFX\\.", src ) );
				assertTrue( src.contains( "RuneFX.rise( s, RISE_N, SkillPalette.RUNE_HOLY" ) );
			} else {
				assertEquals( row[0] + " runes", 0, count( "RuneFX\\.", src ) );
			}
			//no star thrown off a skill here (the owner's falling stars)
			assertFalse( row[0] + " stars", src.contains( "Speck.STAR" ) );
		}
		//the mana gains draw mana in beside the game's own BLUE_LIGHT
		for (String f : new String[]{ "Faith", "Meditation", "Spirituality", "InnerPeace" }){
			String src = source( "actors/hero/skills/" + f + ".java" );
			assertEquals( f, 1, count( "MoteFX\\.gather\\( hero\\.sprite, 8, SkillPalette\\.MANA, 18f, 0\\.4f \\)", src ) );
			assertTrue( f, src.contains( "Speck.BLUE_LIGHT" ) );
		}
		//Serene Focus gains no mana: nothing drawn in
		assertEquals( 0, count( "MoteFX", source( "actors/hero/skills/SereneFocus.java" ) ) );
		assertEquals( 6, Litany.RISE_N );
		assertEquals( 0.4f, Litany.RISE_LIFE, 0f );
	}

	/** The pictures the heals, the blood, the fire and the mana gains make, played for real, sampled
	 *  over 1.5 s: a green one never paler than the heal's A8FFC0, a red one never pink, an orange one
	 *  never salmon, a blue one never lilac. */
	@Test
	public void eachMomentIsInItsMeaningsColours() throws Exception {
		for (String p : new String[]{ "mass-heal", "litany", "healing-prayer", "dawn", "grace" }){
			for (int c : colours( p )){
				if (green( c )) assertFalse( p + ": a heal paler than A8FFC0: " + hex( c ), r( c ) > 0xC0 && b( c ) > 0xC0 );
				assertFalse( p + ": the old sage", c == 0xB3D699 || c == 0xDDEFD0 || c == 0xEAFFEA );
			}
		}
		for (String p : new String[]{ "blood-dance", "heartseeker", "undying-will" }){
			for (int c : colours( p )) if (red( c )) assertFalse( p + ": pink " + hex( c ), pink( c ) );
		}
		for (int c : colours( "ember-arrows" )) if (orange( c )) assertFalse( "ember: salmon " + hex( c ), salmon( c ) );
		for (String p : new String[]{ "faith", "meditation", "spirituality", "inner-peace", "grace" }){
			for (int c : colours( p )) if (blue( c )) assertTrue( p + ": lilac " + hex( c ), g( c ) >= r( c ) + 0x10 );
		}
	}

	/** A PixelFlare is its star over a glow of its own colour, both added: where they meet the colour
	 *  counts about 1.9 times. The heartbeat's and the waltz's stars must stay red there (FF3355, the
	 *  specks' heart that charm draws, adds up to a pink FF61A1). */
	@Test
	public void theHeartsAndTheWaltzsStarsStayRedWhereTheirLightAdds() throws Exception {
		for (String p : new String[]{ "heartseeker", "blood-dance" }){
			List<Integer> stars = new ArrayList<>();
			stage( t -> {
				Runnable run = RfxMeaningProcs.PROCS.get( p ).stage( t );
				run.run();
				for (int f = 0; f < 90; f++){
					t.rig.frames( 1, true );
					for (PixelFlare s : flares( t )) stars.add( lit( s ) );
				}
			} );
			assertFalse( p + " lit no star", stars.isEmpty() );
			for (int c : stars){
				int added = Math.min( 255, Math.round( r( c ) * 1.9f ) ) << 16 | Math.min( 255, Math.round( g( c ) * 1.9f ) ) << 8
						| Math.min( 255, Math.round( b( c ) * 1.9f ) );
				assertFalse( p + ": a star " + hex( c ) + " pink where its light adds (" + hex( added ) + ")", pink( added ) );
			}
		}
	}

	/** Serene Focus spends no mana and gains none: the zap's steady mind keeps the game's BLUE_LIGHT,
	 *  and no mana is drawn into the hero as if he had gained some. */
	@Test
	public void aZapThatGainsNoManaDrawsNoneIn() throws Exception {
		stage( t -> {
			Runnable run = RfxMeaningProcs.PROCS.get( "serene-focus" ).stage( t );
			int mp = t.hero.MP;
			run.run();
			assertEquals( "Serene Focus gains no mana", mp, t.hero.MP );
			int most = 0;
			for (int f = 0; f < 30; f++){
				t.rig.frames( 1, true );
				most = Math.max( most, all( t ).size() );
			}
			assertEquals( "mana drawn in", 0, most );
		} );
	}

	// ------------------------------------------------------------------ the dawn, Grace, the nova

	/** A mend's flow lands in the heal's crosses, so it flows only to one it really mends (or, Litany's
	 *  every third verse at +3, blesses): never a heal picture on an ally already whole. */
	@Test
	public void aMendFlowsOnlyToOneItMends() throws Exception {
		stage( t -> {
			MassHeal m = DreadShadowProcs.skill( new MassHeal() );
			Litany l = DreadShadowProcs.skill( new Litany() );
			t.hero.MP = t.hero.MT = 500;
			t.hero.HP = t.hero.HT;
			RfxMeaningProcs.ally( t, 4, 4 );
			Rat whole = RfxMeaningProcs.ally( t, 2, 4 );
			whole.HP = whole.HT;
			m.execute( t.hero, Skill.AC_CAST );
			t.hero.buff( MassHeal.Wings.class ).act();
			t.rig.frames( 1, true );
			assertEquals( "Mass Heal's flows", 1, SkillRay.full() );
			t.rig.frames( 120, true );
			assertEquals( 0, SkillRay.full() );
			l.onSkillCast( l );
			t.rig.frames( 1, true );
			assertEquals( "a verse's flows", 1, SkillRay.full() );
			t.rig.frames( 120, true );
			Field verses = Litany.class.getDeclaredField( "verses" );
			verses.setAccessible( true );
			verses.setInt( l, 2 );
			l.onSkillCast( l );
			t.rig.frames( 1, true );
			assertEquals( "a blessing verse reaches both", 2, SkillRay.full() );
		} );
	}

	@Test
	public void theDawnShowsCrossesOnlyWhereHpRose() throws Exception {
		stage( t -> {
			t.hero.HP = t.hero.HT;
			Rat a = RfxMeaningProcs.ally( t, 4, 4 );
			a.HP = a.HT;
			SkillSequence.start( t.hero, SkillSequence.DAWN, 1, t.hero.pos, 5, 2, Collections.emptyList() ).act();
			t.rig.frames( 1, true );
			assertEquals( "nobody mended", 0, crosses( t ) );
			a.HP = 20;
			SkillSequence.start( t.hero, SkillSequence.DAWN, 1, t.hero.pos, 5, 2, Collections.emptyList() ).act();
			t.rig.frames( 1, true );
			assertEquals( "the ally mended", SkillSequence.MENDED_CROSSES, crosses( t ) );
		} );
	}

	@Test
	public void gracesBloomIsARingAndFourMotesOfMana() throws Exception {
		stage( t -> {
			CharSprite hs = t.hero.sprite;
			int rings = FxRing.live(), lights = FxLight.live();
			assertEquals( 0, EnchantFxTest.draws( () -> Grace.manaBloom( hs ) ) );
			assertEquals( rings + 1, FxRing.live() );
			assertEquals( "no light of its own", lights, FxLight.live() );
			assertEquals( Grace.BLOOM_MOTES, particles( t, c -> {} ) );
			//the bloom only for mana: a heal past a full pool lands as the heal's own crosses
			assertTrue( source( "actors/hero/skills/Grace.java" ).contains( "Callback landed = overflow ? null : () -> manaBloom( hs );" ) );
		} );
	}

	@Test
	public void theNovasRimeAndShardsKeepTheirCaps() throws Exception {
		stage( t -> {
			FrostNova f = new FrostNova();
			f.level = 2;
			t.hero.MP = t.hero.MT = 200;
			for (int x = 1; x <= 6; x++) for (int y = 2; y <= 6; y += 2) if (x != 3 || y != 4) t.rat( t.at( x, y ) );
			f.execute( t.hero, Skill.AC_CAST );
			float hx = SkillVfx.midX( t.hero.sprite ), hy = SkillVfx.midY( t.hero.sprite );
			Field fr = frames();
			int most = 0;
			double far = 0;
			for (int i = 0; i < 60; i++){
				t.rig.frames( 1, true );
				most = Math.max( most, FxDecal.live() );
				for (FxParticle p : all( t )){
					int[] k = (int[]) fr.get( p );
					if (k == null || (!Arrays.equals( k, FxFrames.SHARD_ICE[0] ) && !Arrays.equals( k, FxFrames.SHARD_ICE[1] ))) continue;
					double d = Math.hypot( p.x - hx, p.y - hy );
					far = Math.max( far, d );
					assertTrue( "a shard past 20 px: " + d, d <= 22 );
				}
			}
			//out of the flare's white heart (about 10 px), where the ice reads as ice
			assertTrue( "the shards stay inside the flare's heart: " + far, far > 14 );
			assertTrue( "rime " + most, most > 0 && most <= FrostNova.RIMES );
		} );
	}

	// ------------------------------------------------------------------ the GPU, the caps

	@Test
	public void nothingTouchesTheGpuWhileItIsMade() throws Exception {
		stage( t -> {
			CharSprite hs = t.hero.sprite, a = t.rig.rat( t.at( 6, 4 ) );
			GL20 gl = Gdx.gl;
			Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
					(p, m, x) -> { throw new AssertionError( "GL call while building: " + m.getName() ); } );
			try {
				SkillRay.show( hs, a, SkillRay.HEAL, MassHeal.FLOW );
				SkillRay.show( hs, a, SkillRay.HOLY, Purify.CLEANSE );
				Litany.spoken( hs );
				Grace.manaBloom( hs );
				SkillSequence.mended( a.ch );
				FrostNova.shards( hs );
				FrostNova.rime( a.ch.pos );
				for (int k = 0; k < 8; k++) BloodDance.step( hs, k );
				HeartseekerArrow.beat( a, 2 );
				HeartseekerArrow.bursting( a );
				EmberArrows.heatRing( a.ch.pos );
				EmberArrows.kindled( hs, EmberArrows.HEAT, EmberArrows.FLARE );
				t.rig.frames( 40, true );
			} finally {
				Gdx.gl = Gdx.gl20 = gl;
			}
		} );
	}

	/** Mass Heal mending four allies, a verse of Litany, Healing Prayer and Purify at once, on a
	 *  phone's numbers: under SOFT, the lights' cap, SkillRay.FULL (later flows core and halo only),
	 *  RuneFX.CAP and the rings'; nothing left once it is over. */
	@Test
	public void aLoudHolyHealMomentKeepsEveryCap() throws Exception {
		Field scale = FxBudget.class.getDeclaredField( "scale" );
		scale.setAccessible( true );
		float desktop = scale.getFloat( null );
		scale.setFloat( null, 1f );
		try {
			stage( t -> {
				MassHeal m = DreadShadowProcs.skill( new MassHeal() );
				Litany l = DreadShadowProcs.skill( new Litany() );
				HealingPrayer p = DreadShadowProcs.skill( new HealingPrayer() );
				Purify u = DreadShadowProcs.skill( new Purify() );
				t.hero.MP = t.hero.MT = 500;
				t.hero.HP = 40;
				for (int[] c : new int[][]{ { 4, 3 }, { 2, 3 }, { 4, 5 }, { 2, 5 } }) RfxMeaningProcs.ally( t, c[0], c[1] );
				m.execute( t.hero, Skill.AC_CAST );
				t.hero.buff( MassHeal.Wings.class ).act();
				l.onSkillCast( l );
				p.execute( t.hero, Skill.AC_CAST );
				u.execute( t.hero, Skill.AC_CAST );
				int most = 0, lights = 0, rays = 0, runes = 0, rings = 0;
				for (int f = 0; f < 150; f++){
					t.rig.frames( 1, true );
					most = Math.max( most, FxBudget.live( FxBudget.PARTICLES ) );
					lights = Math.max( lights, FxLight.live() );
					rays = Math.max( rays, SkillRay.full() );
					runes = Math.max( runes, RuneFX.live() );
					rings = Math.max( rings, FxRing.live() );
				}
				System.out.println( "rfx meaning holy-heal moment: peak particles " + most + ", lights " + lights + ", rays in full "
						+ rays + ", runes " + runes + ", rings " + rings );
				assertTrue( "particles " + most, most <= FxBudget.soft() );
				assertTrue( "lights " + lights, lights <= FxBudget.lightCap() );
				assertEquals( "rays in full", SkillRay.FULL, rays );
				assertTrue( "runes " + runes, runes >= 1 && runes <= RuneFX.CAP );
				assertTrue( "rings " + rings, rings <= FxBudget.ringCap() );
				t.rig.frames( 600, true );
				assertEquals( 0, SkillRay.full() );
				assertEquals( 0, RuneFX.live() );
				assertEquals( 0, FxBudget.live( FxBudget.PARTICLES ) );
			} );
		} finally {
			scale.setFloat( null, desktop );
		}
	}

	/** Crimson Waltz is red from its ribbons' heads to its flecks and its cast's embers: an arc's head
	 *  took its colour paled toward white (crimson to ECA3A3, pink), its flecks started white, and the
	 *  cast threw a fire's orange embers. Red: no green or blue past the fury's FF5A4A. */
	@Test
	public void theWaltzsRibbonsStayRedFromHeadToFlecks() throws Exception {
		List<Integer> seen = new ArrayList<>();
		stage( t -> {
			BloodDance b = DreadShadowProcs.skill( new BloodDance() );
			t.hero.MP = t.hero.MT = 200;
			b.execute( t.hero, Skill.AC_CAST );
			for (int f = 0; f < 40; f++){
				t.rig.frames( 1, true );
				for (Group g : t.rig.layers) ribbons( g, false, seen );
			}
			for (FxParticle p : all( t )) seen.add( p.colorNow() & 0xFFFFFF );
		} );
		assertFalse( "no ribbon seen", seen.isEmpty() );
		for (int c : seen){
			int red = c >> 16, green = (c >> 8) & 0xFF, blue = c & 0xFF;
			assertTrue( String.format( "%06X is not blood red", c ), green <= 0x5A && blue <= 0x5A && red >= 2 * Math.max( green, blue ) );
		}
	}

	//the lit colour of every shown picture of an arc, and of every particle (its flecks)
	static void ribbons( Group g, boolean inArc, List<Integer> out ){
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive) continue;
			if (m instanceof Group) ribbons( (Group) m, inArc || m instanceof ArcSpinFX, out );
			else if (m instanceof FxParticle) out.add( ((FxParticle) m).colorNow() & 0xFFFFFF );
			else if (inArc && m instanceof FxPic && ((FxPic) m).visible){
				FxPic p = (FxPic) m;
				out.add( Math.round( p.rm * 255 ) << 16 | Math.round( p.gm * 255 ) << 8 | Math.round( p.bm * 255 ) );
			}
		}
	}

	// ------------------------------------------------------------------ the review's fixes (rfx/fixes.md)

	/** Sanctuary's field mends every ally standing in it each turn (the hero too): where HP rose,
	 *  the heal's red cross and green light (SkillField.MEND_*, Avatar of Light's light per-turn
	 *  mend); a whole ally shows nothing. */
	@Test
	public void sanctuarysFieldShowsItsMendOnlyWhereHpRose() throws Exception {
		assertEquals( 1, SkillField.MEND_CROSSES );
		assertEquals( 2, SkillField.MEND_GREENS );
		stage( t -> {
			Rat a = RfxMeaningProcs.ally( t, 4, 4 );
			a.HP = a.HT;
			SkillField f = SkillField.place( t.hero, SkillField.SANCTUARY, 1, 5, Collections.singletonList( a.pos ) );
			f.act();
			t.rig.frames( 1, true );
			assertEquals( "nobody mended", 0, crosses( t ) );
			a.HP = 20;
			f.act();
			assertEquals( "the field's heal, as ever", 22, a.HP );
			t.rig.frames( 1, true );
			assertEquals( "the ally mended", SkillField.MEND_CROSSES, crosses( t ) );
		} );
	}

	/** Blackout at mastery feeds the rogue's shadows: the shade flies to each as ever, and where HP
	 *  rose the heal's crosses show on the shadow as the shade lands (FED_AT), never before it and
	 *  never on a whole one. */
	@Test
	public void blackoutsFedShadowShowsTheHealAsTheShadeLands() throws Exception {
		stage( t -> {
			Rat shadow = t.rat( t.at( 7, 4 ) );
			int[] seen = mendSeen( t, () -> Blackout.fed( shadow, t.at( 4, 4 ), true ), Math.round( Blackout.FED_AT / Game.elapsed ) - 2, 40 );
			assertEquals( "before the shade lands", 0, seen[0] );
			assertEquals( "on the shadow as it lands", 2, seen[1] );
			t.rig.frames( 120, true );
			seen = mendSeen( t, () -> Blackout.fed( shadow, t.at( 4, 4 ), false ), 20, 40 );
			assertEquals( "a whole shadow", 0, seen[0] + seen[1] );
		} );
		//the cast feeds through it, the heal shown only where HP rose
		String src = source( "actors/hero/skills/Blackout.java" );
		assertTrue( src.contains( "int before=shadow.HP;" ) );
		assertTrue( src.contains( "fed(shadow,cell,shadow.HP>before);" ) );
	}

	/** Soul Drain's mastery mend shows as the soul it rides home reaches the Warlock, not at the cast
	 *  (six cells: 96 px at SkillBolt.SPEED, 0.48 s). */
	@Test
	public void soulDrainsMendShowsAsTheSoulComesHome() throws Exception {
		stage( t -> {
			SoulDrain s = DreadShadowProcs.skill( new SoulDrain() );
			Rat dead = t.rat( t.at( 9, 4 ) );
			Buff.affect( dead, SoulMark.class, 5 );
			t.hero.HP = 10;
			t.hero.HT = 100;
			int[] seen = mendSeen( t, () -> {
				s.onEnemyDeath( dead, null );
				t.runDeferred();
			}, Math.round( 96f / SkillBolt.SPEED / Game.elapsed ) - 2, 45 );
			assertTrue( "mended", t.hero.HP > 10 );
			assertEquals( "while the soul flies", 0, seen[0] );
			assertEquals( "as it lands", 2, seen[1] );
		} );
	}

	/** The huntress's nature is the palette's leaf green everywhere: the old sage B3D699 (it read
	 *  cream at alpha) is gone from every skill's and effect's code (the enchantments' own and the
	 *  gallery aside), and the soul well shines in the soul's light, not the pre-pass lilac. */
	@Test
	public void theOldSageIsGoneFromEverySkill() throws Exception {
		List<String> found = new ArrayList<>();
		for (String root : new String[]{ "actors", "effects" }){
			try (java.util.stream.Stream<java.nio.file.Path> files = Files.walk( new File( BASE + root ).toPath() )){
				for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) files::iterator){
					String rel = new File( BASE ).toPath().relativize( p ).toString().replace( '\\', '/' );
					if (!rel.endsWith( ".java" ) || rel.startsWith( "effects/fx/enchant/" ) || rel.startsWith( "effects/fx/modules/" )) continue;
					if (source( rel ).toUpperCase().contains( "B3D699" )) found.add( rel );
				}
			}
		}
		assertEquals( "the old sage", "[]", found.toString() );
		assertEquals( SkillPalette.NATURE_LIGHT, ArrowRain.NATURE );
		assertEquals( SkillPalette.NATURE_LIGHT, SkillFieldFX.ARROWS_LIT );
		assertEquals( SkillPalette.NATURE_LIGHT, SkillSpectacleFX.COLORS[SkillSpectacleFX.RAPTOR] );
		assertEquals( SkillPalette.NATURE_LIGHT, DreadFX.PIN );
		assertEquals( DreadFX.SOUL[2], SkillFieldFX.SOUL_LIT );
	}

	/** Speck.BLUE_LIGHT is the game's mana: only a mana gain shows it. Frost Arrows' hoarfrost on the
	 *  struck and its tips riming over and thawing show frost's glints, the same bursts (one draw of
	 *  the game's Random each, as ever), never the mana sparkle. */
	static final String[] NO_MANA_PICTURE = { "actors/hero/skills/FrostArrows.java" };

	@Test
	public void theManaPictureOnlyOnMana() throws Exception {
		for (String f : NO_MANA_PICTURE) assertFalse( f, source( f ).contains( "Speck.BLUE_LIGHT" ) );
		String frost = source( "actors/hero/skills/FrostArrows.java" );
		for (String call : new String[]{ "hero.sprite.emitter().burst( SkillBursts.FROST_GLINT, 4 )",
				"hero.sprite.emitter().burst( SkillBursts.FROST_GLINT, 6 )", "hero.sprite.emitter().burst( SkillBursts.FROST_GLINT, 2 )",
				"enemy.sprite.bottomEmitter().burst( SkillBursts.FROST_GLINT, 3 )",
				"enemy.sprite.centerEmitter().startDelayed( SkillBursts.FROST_GLINT, 0, 3, 0.12f )" }){
			assertEquals( call, 1, count( Pattern.quote( call ), frost ) );
		}
	}

	/** The swap is neutral: frost's glints in the same plain emitters, at the same counts and
	 *  intervals, draw the game's Random exactly as the mana sparkle did (each particle's place). */
	@Test
	public void frostsGlintsDrawTheGamesRandomAsTheManaSparkleDid() throws Exception {
		stage( t -> {
			CharSprite r = t.rat( t.at( 5, 4 ) ).sprite;
			int[] drawn = new int[2];
			Emitter.Factory[] looks = { Speck.factory( Speck.BLUE_LIGHT ), SkillBursts.FROST_GLINT };
			for (int k = 0; k < 2; k++){
				Emitter.Factory f = looks[k];
				drawn[k] = EnchantFxTest.draws( () -> {
					t.hero.sprite.emitter().burst( f, 4 );
					r.bottomEmitter().burst( f, 3 );
					r.centerEmitter().startDelayed( f, 0, 3, 0.12f );
					t.rig.frames( 30, true );
				} );
				t.rig.frames( 60, true );
			}
			assertTrue( "draws " + drawn[0], drawn[0] > 0 );
			assertEquals( "the glints' draws", drawn[0], drawn[1] );
		} );
	}

	//the most crosses alive in frames [0, split) and in [split, end) after `play` (the rig's frames)
	static int[] mendSeen( DreadShadowProcs t, Runnable play, int split, int end ) throws Exception {
		play.run();
		int[] most = { 0, 0 };
		for (int f = 0; f < end; f++){
			t.rig.frames( 1, true );
			int k = f < split ? 0 : 1;
			most[k] = Math.max( most[k], crosses( t ) );
		}
		return most;
	}

	// ------------------------------------------------------------------ helpers

	interface Stage {
		void run( DreadShadowProcs t ) throws Exception;
	}

	//DreadShadowProcs' stage, with the kit's counts as a new scene has them
	static void stage( Stage s ) throws Exception {
		EnchantFxTest.boot();
		DreadShadowProcs t = new DreadShadowProcs();
		t.up();
		SkillRay.reset();
		RuneFX.reset();
		LinkFX.reset();
		SkillBolt.reset();
		PixelFlare.reset();
		try {
			s.run( t );
		} finally {
			t.down();
		}
	}

	//every colour the kit's particles of a moment showed over 1.5 s
	static List<Integer> colours( String proc ) throws Exception {
		List<Integer> out = new ArrayList<>();
		stage( t -> {
			Runnable run = RfxMeaningProcs.PROCS.get( proc ).stage( t );
			run.run();
			for (int f = 0; f < 90; f++){
				for (FxParticle p : all( t )) out.add( p.colorNow() & 0xFFFFFF );
				t.rig.frames( 1, true );
			}
		} );
		assertFalse( proc + " made none", out.isEmpty() );
		return out;
	}

	static List<FxParticle> all( DreadShadowProcs t ){
		List<FxParticle> out = new ArrayList<>();
		for (Group g : t.rig.layers) collect( g, out );
		collect( t.rig.stage, out );
		return out;
	}

	static void collect( Group g, List<FxParticle> out ){
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive) continue;
			if (m instanceof FxParticle) out.add( (FxParticle) m );
			else if (m instanceof Group) collect( (Group) m, out );
		}
	}

	static List<PixelFlare> flares( DreadShadowProcs t ){
		List<PixelFlare> out = new ArrayList<>();
		for (Group g : t.rig.layers) collectFlares( g, out );
		collectFlares( t.rig.stage, out );
		return out;
	}

	static void collectFlares( Group g, List<PixelFlare> out ){
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive) continue;
			if (m instanceof PixelFlare) out.add( (PixelFlare) m );
			else if (m instanceof Group) collectFlares( (Group) m, out );
		}
	}

	//the colour a light picture is lit (FxPic.lit: hardlight)
	static int lit( PixelFlare s ){
		return Math.round( s.rm * 255 ) << 16 | Math.round( s.gm * 255 ) << 8 | Math.round( s.bm * 255 );
	}

	static int particles( DreadShadowProcs t, Consumer<FxParticle> each ){
		List<FxParticle> l = all( t );
		for (FxParticle p : l) each.accept( p );
		return l.size();
	}

	//the heal's red crosses alive now (MoteFX.crosses: PLUS_5 lit HEAL_CROSS)
	static int crosses( DreadShadowProcs t ) throws Exception {
		Field fr = frames();
		int n = 0;
		for (FxParticle p : all( t )){
			int[] k = (int[]) fr.get( p );
			if (k != null && Arrays.equals( k, FxFrames.PLUS_5 ) && (p.colorNow() & 0xFFFFFF) == SkillPalette.HEAL_CROSS) n++;
		}
		return n;
	}

	//the frame a particle shows now
	static Field frames() throws Exception {
		Field f = FxParticle.class.getDeclaredField( "shown" );
		f.setAccessible( true );
		return f;
	}

	static int field( Class<?> c, String name ) throws Exception {
		Field f = c.getDeclaredField( name );
		f.setAccessible( true );
		return f.getInt( null );
	}

	static int r( int c ){ return (c >> 16) & 0xFF; }
	static int g( int c ){ return (c >> 8) & 0xFF; }
	static int b( int c ){ return c & 0xFF; }
	static String hex( int c ){ return String.format( "%06X", c ); }

	static boolean green( int c ){ return g( c ) > r( c ) + 0x10 && g( c ) > b( c ) + 0x10; }
	static boolean red( int c ){ return r( c ) > g( c ) + 0x40 && r( c ) > b( c ) + 0x40; }
	//a red turned toward magenta and pale (F08A9E, D94969): a red lerped toward white keeps its
	//green and blue equal (a flash), the heart's FF3355 its blue low
	static boolean pink( int c ){ return b( c ) > 0x60 && b( c ) > g( c ) + 8; }
	static boolean orange( int c ){ return r( c ) > b( c ) + 0x60 && g( c ) >= b( c ) + 0x18 && r( c ) > g( c ); }
	//an orange gone grey and pale toward skin (DF9979): its blue high and its green barely above it
	//(a fire's pale flash, FFE592, keeps its green well above its blue)
	static boolean salmon( int c ){ return b( c ) > 0x60 && g( c ) < b( c ) + 0x30; }
	static boolean blue( int c ){ return b( c ) > r( c ) + 0x20 && b( c ) >= g( c ); }

	static int count( String regex, String src ){
		Matcher m = Pattern.compile( regex, Pattern.DOTALL ).matcher( src );
		int n = 0;
		while (m.find()) n++;
		return n;
	}

	//a source file without its comments
	static String source( String path ) throws Exception {
		String s = new String( Files.readAllBytes( new File( BASE + path ).toPath() ), "UTF-8" );
		s = s.replaceAll( "(?s)/\\*.*?\\*/", "" );
		return s.replaceAll( "//[^\\n]*", "" );
	}
}
