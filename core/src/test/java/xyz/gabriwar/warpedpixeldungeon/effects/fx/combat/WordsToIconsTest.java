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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Combo;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invulnerability;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CreepingDread;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Monk;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantNet;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantState;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.RoundShield;
import xyz.gabriwar.warpedpixeldungeon.messages.Languages;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.particles.Emitter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * Words that floated over characters become icons on the hit cards, headless on EnchantFxTest's
 * stage: a miss is a card with its verb's icon and its reason and floats no word (the hero's parry
 * still spends its tracker); the verb's icon by its text in English and Portuguese; a buff taking
 * hold with no blow open is a late small card, or joins the card a blow left on it; a skill's
 * shout is its own icon on the struck one's card, or a late card over the hero; an invulnerable
 * target's card shows IMMUNE; a small card keeps every chip; a skill chip is framed 10x10 at half
 * size, 11 px apart; a mark gives its slot to a merged number and leads the chips; a late chip goes
 * to co-op guests as a CARD, never as a floating text.
 */
public class WordsToIconsTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private Hero savedHero;

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		HitCard.reset();
		NetVisuals.clear();
		savedHero = Dungeon.hero;
	}

	@After
	public void tearDown() throws Exception {
		TestParty.leave();
		NetVisuals.clear();
		HitCard.reset();
		Messages.setup( Languages.ENGLISH );
		Dungeon.hero = savedHero;
		rig.down();
	}

	//a hero standing at a cell with a plain sprite on the stage
	private Hero hero( int cell ){
		Hero h = new Hero();
		h.pos = cell;
		h.HP = h.HT = 20;
		CharSprite s = new CharSprite(){
			@Override public Emitter emitter(){ return new Emitter(); }
			@Override public Emitter centerEmitter(){ return new Emitter(); }
		};
		s.ch = h;
		h.sprite = s;
		rig.stage.add( s );
		s.place( cell );
		s.visible = true;
		return h;
	}

	//runs with the render thread's queue kept: what was posted
	private static List<Runnable> keepingPosts( Runnable r ){
		final List<Runnable> posted = new ArrayList<>();
		Application saved = Gdx.app;
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(), new Class<?>[]{ Application.class },
				(p, m, args) -> {
					if (m.getName().equals( "postRunnable" )){
						synchronized (posted){
							posted.add( (Runnable) args[0] );
						}
						return null;
					}
					return saved != null ? m.invoke( saved, args ) : null;
				} );
		try {
			r.run();
		} finally {
			Gdx.app = saved;
		}
		return posted;
	}

	//how many posted runnables are a floating text's (Game.runOnRenderThread wraps FloatingText's)
	private static int texts( List<Runnable> posted ){
		int n = 0;
		for (Runnable r : posted){
			for (Field f : r.getClass().getDeclaredFields()){
				try {
					f.setAccessible( true );
					Object c = f.get( r );
					if (c != null && c.getClass().getName().startsWith( FloatingText.class.getName() )) n++;
				} catch (ReflectiveOperationException | RuntimeException e){
					//not a text's
				}
			}
		}
		return n;
	}

	//the live cards on a sprite
	private List<HitCard> cardsOn( CharSprite s ){
		List<HitCard> out = new ArrayList<>();
		for (Gizmo g : rig.layers[GameScene.Layer.STATUSES.ordinal()].membersView()){
			if (g instanceof HitCard && g.alive && ((HitCard) g).target() == s) out.add( (HitCard) g );
		}
		return out;
	}

	private static boolean has( HitCard c, int code ){
		for (int i = 0; i < c.chipCount(); i++) if (c.chip( i ) == code) return true;
		return false;
	}

	// ------------------------------------------------------------------ misses

	@Test
	public void aMissIsACardWithItsVerbsIconAndItsReasonAndNoWord(){
		final CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		((Mob) d.ch).defenseSkill = Char.INFINITE_EVASION;
		//the miss's reason reads the hero (one off the stage)
		Dungeon.hero = new Hero();
		final boolean[] hit = { true };
		List<Runnable> posted = keepingPosts( () -> hit[0] = a.ch.attack( d.ch ) );
		assertTrue( "it missed", !hit[0] );
		assertEquals( "no floating word", 0, texts( posted ) );
		List<HitCard> cards = cardsOn( d );
		assertEquals( "one card", 1, cards.size() );
		HitCard c = cards.get( 0 );
		assertEquals( HitCard.MAIN, c.kind() );
		assertEquals( "no number", -1, c.number() );
		assertEquals( "the dodge in the left slot", FloatingText.MISS_DODGE, c.leftIcon() );
		assertTrue( c.leftShown() );
		//(a sure dodge names no reason: nothing but the dodge)
		assertEquals( 0, c.chipCount() );

		//a miss with a reason, a skill's icon having come first: the reason still leads
		Game.timeTotal += 2f;
		rig.frames( 60, true );
		Blow.begin( a.ch, d.ch );
		Blow.chip( d.ch, HitCard.SKILL_BASE + 6, 0 );
		Blow.miss( d.ch, FloatingText.MISS_BLOCK, FloatingText.MISS_BLS );
		Blow.end();
		c = EnchantState.get( d ).card;
		assertEquals( FloatingText.MISS_BLOCK, c.leftIcon() );
		assertEquals( FloatingText.MISS_BLS, c.chip( 0 ) );
		assertEquals( HitCard.SKILL_BASE + 6, c.chip( 1 ) );
	}

	@Test
	public void theHerosParryIsAParryCardAndSpendsItsTracker(){
		Hero h = hero( at( 6, 6 ) );
		Dungeon.hero = h;
		final CharSprite a = rig.rat( at( 5, 6 ) );
		Buff.affect( h, Combo.ParryTracker.class );
		//no scene to interrupt here
		h.damageInterrupt = false;
		List<Runnable> posted = keepingPosts( () -> a.ch.attack( h ) );
		assertEquals( "no floating word", 0, texts( posted ) );
		assertNull( "the parry is spent (Hero.defenseVerb still ran)", h.buff( Combo.ParryTracker.class ) );
		HitCard c = EnchantState.get( h.sprite ).card;
		assertNotNull( c );
		assertEquals( FloatingText.PARRY, c.leftIcon() );
	}

	private static final Class<?>[] BLOCKERS = {
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.Brute.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalGuardian.class,
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM300.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollGuard.class,
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.Golem.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreatCrab.class,
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.Guard.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.HermitCrab.class,
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShellCrab.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Monk.class,
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Slime.class,
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.GooPlant.class,
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.Beetle.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.VineLasher.class,
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zombie.class,
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RoadPatrol.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith.class,
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Sheep.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Wandmaker.class,
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.VaultTokenDoor.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.VaultMirror.class
	};

	private static final Class<?>[] DODGERS = {
			Char.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat.class,
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer.class, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Velocirooster.class
	};

	private void verbsMap( String lang ){
		for (Class<?> c : BLOCKERS){
			String verb = Messages.get( c, "def_verb" );
			assertEquals( lang + ": " + c.getSimpleName() + " (" + verb + ")", FloatingText.MISS_BLOCK, Blow.missCell( null, verb ) );
		}
		for (Class<?> c : DODGERS){
			String verb = Messages.get( c, "def_verb" );
			assertEquals( lang + ": " + c.getSimpleName() + " (" + verb + ")", FloatingText.MISS_DODGE, Blow.missCell( null, verb ) );
		}
		Hero h = new Hero();
		assertEquals( lang, FloatingText.PARRY, Blow.missCell( h, Messages.get( Monk.class, "parried" ) ) );
		assertEquals( lang, FloatingText.GUARDED, Blow.missCell( h, Messages.get( RoundShield.GuardTracker.class, "guarded" ) ) );
		assertEquals( lang, FloatingText.MISS_DODGE, Blow.missCell( h, Messages.get( Char.class, "def_verb" ) ) );
		Monk monk = new Monk();
		assertEquals( lang + ": a monk's parry", FloatingText.PARRY, Blow.missCell( monk, Messages.get( Monk.class, "parried" ) ) );
		assertEquals( lang + ": a monk's block", FloatingText.MISS_BLOCK, Blow.missCell( monk, Messages.get( Monk.class, "def_verb" ) ) );
	}

	@Test
	public void theVerbsIconByItsTextInEnglishAndPortuguese(){
		Messages.setup( Languages.ENGLISH );
		verbsMap( "en" );
		Messages.setup( Languages.PORTUGUESE );
		verbsMap( "pt" );
	}

	// ------------------------------------------------------------------ buffs, shouts, immunity

	@Test
	public void aBuffWithNoBlowIsALateCardOrJoinsTheBlowsCard(){
		final CharSprite r = rig.rat( at( 6, 6 ) ), a = rig.rat( at( 5, 6 ) );
		List<Runnable> posted = keepingPosts( () -> Buff.affect( r.ch, Weakness.class, 5f ) );
		assertEquals( "no word", 0, texts( posted ) );
		List<HitCard> cards = cardsOn( r );
		assertEquals( 1, cards.size() );
		HitCard small = cards.get( 0 );
		assertEquals( HitCard.SMALL, small.kind() );
		assertTrue( small.isLate() );
		assertEquals( -(BuffIndicator.WEAKNESS + 1), small.chip( 0 ) );

		//a blow's card, then a buff 0.2 s later: it joins that card
		Game.timeTotal += 2f;
		rig.frames( 60, true );
		Blow.begin( a.ch, r.ch );
		Blow.claim( r.ch, "4", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		Blow.end();
		HitCard card = EnchantState.get( r ).card;
		assertNotNull( card );
		//late in its hold, so the late chip's own hold outlasts the card's
		for (int i = 0; i < 600 && card.age() + HitCard.LATE_HOLD <= card.fadeAt(); i++) rig.frames( 1, true );
		float fadeAt = card.fadeAt();
		assertTrue( "not fading yet", card.age() < fadeAt );
		assertTrue( "its hold ends before a late chip's would", fadeAt < card.age() + HitCard.LATE_HOLD );
		posted = keepingPosts( () -> Buff.affect( r.ch, Vulnerable.class, 5f ) );
		assertEquals( "no word", 0, texts( posted ) );
		assertTrue( "the chip joined the blow's card", has( card, -(BuffIndicator.VULNERABLE + 1) ) );
		assertEquals( "and keeps it LATE_HOLD s more", card.age() + HitCard.LATE_HOLD, card.fadeAt(), 1e-4f );
	}

	@Test
	public void aWordStillCountsAsAFloatingText(){
		//the word counter used above sees a word when one floats (so its zeros mean something)
		final CharSprite r = rig.rat( at( 6, 6 ) );
		List<Runnable> posted = keepingPosts( () -> r.showStatus( CharSprite.NEUTRAL, "dodged" ) );
		assertEquals( 1, texts( posted ) );
	}

	@Test
	public void aCountOfNoneIsItsIconAlone(){
		final CharSprite r = rig.rat( at( 6, 6 ) );
		int bleed = -(BuffIndicator.BLEEDING + 1);
		List<Runnable> posted = keepingPosts( () -> Blow.counter( r.ch, bleed, 0, CharSprite.WARNING ) );
		assertEquals( "no word", 0, texts( posted ) );
		List<HitCard> cards = cardsOn( r );
		assertEquals( "a count of 0 still shows that it happened", 1, cards.size() );
		assertTrue( has( cards.get( 0 ), bleed ) );
		assertEquals( "with no number", -1, cards.get( 0 ).number() );
	}

	@Test
	public void aShoutIsTheSkillsIconOnTheStruckOnesCardOrOverTheHero(){
		Hero h = hero( at( 5, 6 ) );
		Dungeon.hero = h;
		final CharSprite d = rig.rat( at( 6, 6 ) );
		final CreepingDread dread = new CreepingDread();
		int code = HitCard.SKILL_BASE + dread.image();
		List<Runnable> posted = keepingPosts( () -> {
			Blow.begin( h, d.ch );
			Blow.claim( d.ch, "3", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
			dread.castTextYell();
			Blow.end();
		} );
		assertEquals( "no shout floats", 0, texts( posted ) );
		assertTrue( "Boo. is the skill's icon on the rat's card", has( EnchantState.get( d ).card, code ) );

		posted = keepingPosts( dread::castTextYell );
		assertEquals( "no shout floats", 0, texts( posted ) );
		List<HitCard> over = cardsOn( h.sprite );
		assertEquals( 1, over.size() );
		assertTrue( "outside a blow: a late card over the hero", over.get( 0 ).isLate() && has( over.get( 0 ), code ) );
	}

	@Test
	public void anInvulnerableTargetsCardShowsImmune(){
		final CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Buff.affect( d.ch, Invulnerability.class, 5f );
		Game.timeTotal += 2f;
		rig.frames( 60, true );
		List<Runnable> posted = keepingPosts( () -> a.ch.attack( d.ch ) );
		assertEquals( "no word", 0, texts( posted ) );
		HitCard c = EnchantState.get( d ).card;
		assertNotNull( c );
		assertEquals( HitCard.MAIN, c.kind() );
		assertEquals( -1, c.number() );
		assertEquals( FloatingText.IMMUNE, c.leftIcon() );
	}

	// ------------------------------------------------------------------ the card's chips

	@Test
	public void aSmallCardKeepsEveryChip(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard c = HitCard.small( s, -1, CharSprite.NEGATIVE, new int[]{ FloatingText.PARRY, -(BuffIndicator.DAZE + 1),
				HitCard.SKILL_BASE + 3 }, new int[]{ 0, 0, 0 }, 3 );
		assertEquals( 3, c.chipCount() );
		assertEquals( 3, c.shownChips() );

		//the attacker dazed and weakened by a blow it struck for nothing: both on its small card
		final CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 7, 6 ) );
		Blow.begin( a.ch, d.ch );
		Blow.chip( a.ch, -(BuffIndicator.DAZE + 1), 0 );
		Blow.chip( a.ch, -(BuffIndicator.WEAKNESS + 1), 0 );
		Blow.chip( a.ch, HitCard.SKILL_BASE + 5, 0 );
		Blow.claim( d.ch, "6", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		Blow.end();
		List<HitCard> on = cardsOn( a );
		assertEquals( 1, on.size() );
		assertEquals( "every chip, not the first only", 3, on.get( 0 ).chipCount() );
	}

	@Test
	public void aSkillChipIsFramedAtHalfSizeAndElevenApart(){
		assertEquals( 0, BlowPresenter.chipClass( HitCard.SKILL_BASE + 3 ) );
		assertEquals( 0, BlowPresenter.chipClass( FloatingText.MISS_DODGE ) );
		assertEquals( 0, BlowPresenter.chipClass( FloatingText.RESISTED ) );
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard c = HitCard.show( s, 1, 5, CharSprite.NEGATIVE, FloatingText.PHYS_DMG, false, 0f,
				new int[]{ HitCard.SKILL_BASE + 3, FloatingText.PARRY, -(BuffIndicator.DAZE + 1) }, new int[3], new float[3], 3 );
		assertEquals( "a skill's frame", 10f, c.chipFrame( 0 ), 0f );
		assertEquals( 0.5f, c.chipScale( 0 ), 0f );
		assertEquals( "a text cell has none", 0f, c.chipFrame( 1 ), 0f );
		assertEquals( "a buff's frame", 9f, c.chipFrame( 2 ), 0f );
		assertEquals( 1f, c.chipScale( 1 ), 0f );
		//the skill's icon sits 1 px in its frame; the next chip starts 11 px after the frame
		assertEquals( 10f, c.chipX( 1 ) - c.chipX( 0 ), 0f );
	}

	@Test
	public void aMarkGivesItsSlotToAMergedNumberAndLeadsTheChips(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard c = HitCard.show( s, 1, -1, CharSprite.NEGATIVE, FloatingText.MISS_DODGE, false, 0f,
				new int[]{ FloatingText.MISS_EVA }, new int[1], new float[1], 1 );
		assertEquals( FloatingText.MISS_DODGE, c.leftIcon() );
		rig.frames( 3, true );
		HitCard again = HitCard.show( s, 1, -1, CharSprite.NEGATIVE, FloatingText.MISS_DODGE, false, 0f, new int[0], new int[0], new float[0], 0 );
		assertEquals( "merged", c, again );
		assertEquals( "xN counts the misses", 2, c.count() );
		HitCard.show( s, 1, 7, CharSprite.NEGATIVE, FloatingText.PHYS_DMG, false, 0f, new int[0], new int[0], new float[0], 0 );
		assertEquals( 7, c.number() );
		assertEquals( FloatingText.PHYS_DMG, c.leftIcon() );
		assertEquals( "the dodge leads the chips now", FloatingText.MISS_DODGE, c.chip( 0 ) );
		assertTrue( has( c, FloatingText.MISS_EVA ) );
	}

	// ------------------------------------------------------------------ co-op

	@Test
	public void aLateChipGoesToTheGuestsAsACardNotAText() throws Exception {
		TestParty.join( new Hero() );
		NetVisuals.clear();
		CharSprite s = rig.rat( at( 6, 6 ) );
		Blow.late( s.ch, FloatingText.RAGE, 0 );
		JSONArray sent = NetVisuals.drainEvents();
		int cards = 0, texts = 0;
		for (int i = 0; sent != null && i < sent.length(); i++){
			JSONObject e = sent.optJSONObject( i );
			if ("eh".equals( e.optString( "t" ) ) && e.optInt( "k" ) == EnchantNet.CARD) cards++;
			if ("ft".equals( e.optString( "t" ) )) texts++;
		}
		assertEquals( "one CARD", 1, cards );
		assertEquals( "no floating text", 0, texts );

		//the guest's side: the CARD joins the guest's live card
		HitCard.reset();
		HitCard mine = HitCard.show( s, 2, 4, CharSprite.NEGATIVE, FloatingText.PHYS_DMG, false, 0f, new int[0], new int[0], new float[0], 0 );
		BlowPresenter.guestCard( BlowPresenter.lateCard( s.ch, -1, CharSprite.NEGATIVE, new int[]{ FloatingText.RAGE }, new int[]{ 0 } ), s, null );
		assertTrue( has( mine, FloatingText.RAGE ) );
	}
}
