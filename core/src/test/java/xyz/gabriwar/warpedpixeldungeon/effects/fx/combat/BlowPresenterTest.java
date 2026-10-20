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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantState;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.watabou.noosa.Game;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * The blow's presenter (BlowPresenter) and its ledger (Blow), headless on EnchantFxTest's stage
 * with pieces that keep a record of what they were asked: the best rank leads; accents come
 * BEAT s apart, ACCENT_UP px higher each, sideways from the side away from the attacker; the
 * wielder's lane has a beat each; every piece draws, at any pace of blows (the owner's call: a blow
 * arranges its pictures, it never thins them); one flash a sprite, the lead alone stings and the
 * blow's frame alone shakes; every accent keeps its chip; claimed texts reach neither FloatingText
 * nor the guests, released ones do; a blow on another thread presents on the render thread's next
 * turn; nested blows close in order. Against the same calls outside a blow (master's pictures): a
 * blow of three procs draws them all, the echo, the blood, a repeat blow's star, a twist under a
 * proc and the busy blow draw what they draw alone, and a perk's accent draws on its beat.
 */
public class BlowPresenterTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private int savedTier;

	@Before
	public void setUp() throws Exception {
		rig = new EnchantFxTest.Rig();
		rig.up();
		HitCard.reset();
		savedTier = FxBudget.tier();
		NetVisuals.clear();
	}

	@After
	public void tearDown() throws Exception {
		quality( savedTier );
		TestParty.leave();
		NetVisuals.clear();
		HitCard.reset();
		rig.down();
	}

	//the kit's quality tier set by hand
	private static void quality( int tier ) throws Exception {
		Field f = FxBudget.class.getDeclaredField( "tier" );
		f.setAccessible( true );
		f.set( null, tier );
	}

	/** A piece that keeps a record of what it was asked: [time, mayShake] for a lead, [time, x, y]
	 *  for an accent. */
	static final class Fake extends Piece {
		final List<float[]> leads = new ArrayList<>(), accents = new ArrayList<>();
		int stings;

		Fake( int rank, int lane, Char on ){
			super( -1, rank, lane, on );
		}

		Fake chip( int code ){
			chip = code;
			return this;
		}

		@Override
		public void lead( boolean mayShake ){
			leads.add( new float[]{ Game.timeTotal, mayShake ? 1 : 0 } );
		}

		@Override
		public void accent( float x, float y ){
			accents.add( new float[]{ Game.timeTotal, x, y } );
		}

		@Override
		public void sting(){
			stings++;
		}
	}

	//a blow of a on d: the pieces offered, then closed
	private static void blow( Char a, Char d, Piece... pieces ){
		Blow.begin( a, d );
		for (Piece p : pieces) assertTrue( Blow.offer( p ) );
		Blow.end();
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

	@Test
	public void theBestRankLeads(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Fake plain = new Fake( Blow.PLAIN, Blow.TARGET, d.ch ), crit = new Fake( Blow.CRIT, Blow.TARGET, d.ch ),
				twist = new Fake( Blow.TWIST, Blow.TARGET, d.ch ), proc = new Fake( Blow.PROC, Blow.TARGET, d.ch );
		blow( a.ch, d.ch, plain, crit, twist, proc );
		rig.frames( 30, true );
		assertEquals( "the proc leads", 1, proc.leads.size() );
		assertEquals( 0, proc.accents.size() );
		for (Fake f : new Fake[]{ plain, crit, twist }) assertEquals( 0, f.leads.size() );
		assertEquals( "the twist is the first accent", 1, twist.accents.size() );
		assertEquals( 1, crit.accents.size() );

		Game.timeTotal += 1f;
		Fake crit2 = new Fake( Blow.CRIT, Blow.TARGET, d.ch ), plain2 = new Fake( Blow.PLAIN, Blow.TARGET, d.ch );
		blow( a.ch, d.ch, plain2, crit2 );
		assertEquals( "with no proc nor twist, the crit", 1, crit2.leads.size() );
		assertEquals( 0, plain2.leads.size() );
	}

	@Test
	public void accentsComeOnTheirBeatsAndRiseAsAStack(){
		Game.elapsed = 0.005f;
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Fake[] acc = new Fake[3];
		for (int i = 0; i < 3; i++) acc[i] = new Fake( Blow.MINOR, Blow.TARGET, d.ch );
		float t0 = Game.timeTotal;
		blow( a.ch, d.ch, acc );
		rig.frames( 100, true );
		float hx = d.x + d.width() / 2f - BlowPresenter.HIT_OFF, hy = d.y + d.height() / 2f;
		for (int i = 0; i < 3; i++){
			assertEquals( "accent " + i, 1, acc[i].accents.size() );
			float[] r = acc[i].accents.get( 0 );
			assertEquals( "its beat", t0 + BlowPresenter.BEAT * (i + 1), r[0], 0.011f );
			//the attacker stands to the left: the first goes right, then they alternate
			float side = i % 2 == 0 ? 1f : -1f;
			assertEquals( hx + side * BlowPresenter.ACCENT_SIDE, r[1], 0.01f );
			assertEquals( hy - BlowPresenter.ACCENT_UP * (i + 1), r[2], 0.01f );
		}
	}

	@Test
	public void theWieldersLaneTakesABeatEach(){
		Game.elapsed = 0.005f;
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Fake[] w = new Fake[3];
		for (int i = 0; i < 3; i++) w[i] = new Fake( Blow.MINOR, Blow.WIELDER, a.ch );
		float t0 = Game.timeTotal;
		blow( a.ch, d.ch, w );
		rig.frames( 100, true );
		float side = a.flipHorizontal ? -EnchantFx.WEAPON_SIDE : EnchantFx.WEAPON_SIDE;
		for (int i = 0; i < 3; i++){
			assertEquals( 1, w[i].accents.size() );
			float[] b = w[i].accents.get( 0 );
			assertEquals( "beat " + (i + 1), t0 + BlowPresenter.BEAT * (i + 1), b[0], 0.011f );
			assertEquals( a.x + a.width() / 2f + side, b[1], 0.01f );
			if (i > 0) assertEquals( "each 4 px over the last", w[i - 1].accents.get( 0 )[2] - BlowPresenter.WIELDER_UP, b[2], 0.01f );
		}
	}

	@Test
	public void everyPieceDrawsAtAnyPaceAndAnyQuality() throws Exception {
		Game.elapsed = 1 / 60f;
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		//at every quality tier of the kit: a blow, another in the same frame (pellets), a third 0.1 s on
		for (int q : new int[]{ FxBudget.FULL, FxBudget.REDUCED, FxBudget.MINIMAL }){
			quality( q );
			List<Fake> all = new ArrayList<>();
			for (int k = 0; k < 3; k++){
				Fake lead = new Fake( Blow.PROC, Blow.TARGET, d.ch ), proc = new Fake( Blow.PROC, Blow.TARGET, d.ch ),
						acc = new Fake( Blow.MINOR, Blow.TARGET, d.ch ), w = new Fake( Blow.MINOR, Blow.WIELDER, a.ch );
				blow( a.ch, d.ch, lead, proc, acc, w );
				all.add( lead );
				all.add( proc );
				all.add( acc );
				all.add( w );
				if (k == 1) rig.frames( 6, true );
			}
			rig.frames( 30, true );
			for (int i = 0; i < all.size(); i++){
				Fake f = all.get( i );
				assertEquals( "quality " + q + ", piece " + i + " drawn once", 1, f.leads.size() + f.accents.size() );
			}
			assertEquals( "each blow's first proc leads", 3, all.get( 0 ).leads.size() + all.get( 4 ).leads.size()
					+ all.get( 8 ).leads.size() );
			Game.timeTotal += 1f;
		}
	}

	@Test
	public void theLeadStingsAndTheBlowsFrameShakesAtAnyPace(){
		Hero h = hero( at( 5, 6 ) );
		Dungeon.hero = h;
		CharSprite d = rig.rat( at( 6, 6 ) );
		Fake lead = new Fake( Blow.PROC, Blow.TARGET, d.ch ), a1 = new Fake( Blow.CRIT, Blow.TARGET, d.ch ),
				a2 = new Fake( Blow.MINOR, Blow.TARGET, d.ch );
		blow( h, d.ch, lead, a1, a2 );
		Game.timeTotal += 0.1f;
		Fake again = new Fake( Blow.PROC, Blow.TARGET, d.ch );
		blow( h, d.ch, again );
		rig.frames( 30, true );
		assertEquals( "the lead may shake (the hero in it)", 1f, lead.leads.get( 0 )[1], 0f );
		assertEquals( 1, lead.stings );
		assertEquals( 0, a1.stings + a2.stings );
		assertEquals( "the accents were drawn", 2, a1.accents.size() + a2.accents.size() );
		assertEquals( "a repeat blow may shake as ever", 1f, again.leads.get( 0 )[1], 0f );

		Game.timeTotal += 1f;
		CharSprite a = rig.rat( at( 4, 6 ) );
		Fake unseen = new Fake( Blow.PROC, Blow.TARGET, d.ch );
		blow( a.ch, d.ch, unseen );
		assertEquals( "no hero in it: no shake", 0f, unseen.leads.get( 0 )[1], 0f );
	}

	@Test
	public void oneWhiteFlashASpriteABlow(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), n = rig.rat( at( 7, 6 ) );
		Blow.begin( a.ch, d.ch );
		assertTrue( Blow.mayFlash( d ) );
		assertFalse( "a second flash of it", Blow.mayFlash( d ) );
		assertTrue( "another sprite's", Blow.mayFlash( n ) );
		Blow.end();
		assertTrue( "no blow open: as ever", Blow.mayFlash( d ) );
	}

	@Test
	public void everyAccentDrawsAndKeepsItsChip(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		int[] codes = { FloatingText.HEAVE, FloatingText.PARRY, FloatingText.SHRED, FloatingText.FELL, FloatingText.CRACK };
		Fake[] acc = new Fake[codes.length];
		for (int i = 0; i < codes.length; i++) acc[i] = new Fake( Blow.MINOR, Blow.TARGET, d.ch ).chip( codes[i] );
		Blow.begin( a.ch, d.ch );
		for (Fake f : acc) Blow.offer( f );
		Blow.claim( d.ch, "6", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		Blow.end();
		rig.frames( 30, true );
		int drawn = 0;
		for (Fake f : acc) drawn += f.accents.size();
		assertEquals( "every one on its beat", codes.length, drawn );
		HitCard c = EnchantState.get( d ).card;
		assertEquals( "every chip on the card", codes.length, c.chipCount() );
		for (int i = 0; i < codes.length; i++) assertEquals( codes[i], c.chip( i ) );
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

	//whether a posted runnable is a floating text's (Game.runOnRenderThread wraps FloatingText's callback)
	private static boolean text( Runnable r ){
		for (Field f : r.getClass().getDeclaredFields()){
			try {
				f.setAccessible( true );
				Object c = f.get( r );
				if (c != null && c.getClass().getName().startsWith( FloatingText.class.getName() )) return true;
			} catch (ReflectiveOperationException | RuntimeException e){
				return false;
			}
		}
		return false;
	}

	private static int texts( List<Runnable> posted ){
		int n = 0;
		for (Runnable r : posted) if (text( r )) n++;
		return n;
	}

	private static int sentTexts(){
		JSONArray a = NetVisuals.drainEvents();
		int n = 0;
		if (a != null) for (int i = 0; i < a.length(); i++){
			JSONObject e = a.optJSONObject( i );
			if (e != null && "ft".equals( e.optString( "t" ) )) n++;
		}
		return n;
	}

	@Test
	public void claimedTextsReachNeitherTheScreenNorTheGuestsReleasedOnesDo() throws Exception {
		final CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		TestParty.join( new Hero() );
		List<Runnable> posted = keepingPosts( () -> {
			Blow.begin( a.ch, d.ch );
			d.showStatusWithIcon( CharSprite.NEGATIVE, "7", FloatingText.PHYS_DMG );
			d.showStatusWithIcon( CharSprite.NEGATIVE, "3", FloatingText.BURNING );
			Blow.end();
		} );
		assertEquals( "no text of the numbers", 0, texts( posted ) );
		assertEquals( "nothing sent of them", 0, sentTexts() );
		assertEquals( 10, EnchantState.get( d ).card.number() );

		posted = keepingPosts( () -> {
			Blow.begin( a.ch, d.ch );
			d.showStatus( CharSprite.POSITIVE, "invulnerable" );
			Blow.end();
		} );
		assertEquals( "a word is released", 1, texts( posted ) );
		assertEquals( "and sent", 1, sentTexts() );
	}

	@Test
	public void aBlowOnAnotherThreadPresentsOnTheRenderThread() throws Exception {
		final CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		final Throwable[] failed = new Throwable[1];
		List<Runnable> posted = keepingPosts( () -> {
			Thread actor = new Thread( () -> {
				try {
					Blow.begin( a.ch, d.ch );
					Blow.claim( d.ch, "5", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
					Blow.end();
				} catch (Throwable t){
					failed[0] = t;
				}
			} );
			actor.start();
			try {
				actor.join();
			} catch (InterruptedException e){
				throw new AssertionError( e );
			}
		} );
		if (failed[0] != null) throw new AssertionError( failed[0] );
		EnchantState st = EnchantState.get( d );
		assertTrue( "nothing yet", st == null || st.card == null );
		assertEquals( "the presenter posted", 1, posted.size() );
		for (Runnable r : posted) r.run();
		assertNotNull( EnchantState.get( d ).card );
		assertEquals( 5, EnchantState.get( d ).card.number() );
	}

	//the pictures alive on the layers (not the cards', not lights, not decals)
	private void pictures( java.util.IdentityHashMap<com.watabou.noosa.Gizmo, Boolean> into ){
		for (int i = 0; i < rig.layers.length; i++){
			if (i == xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.Layer.STATUSES.ordinal()) continue;
			collect( rig.layers[i], into );
		}
	}

	private static void collect( com.watabou.noosa.Group g, java.util.IdentityHashMap<com.watabou.noosa.Gizmo, Boolean> into ){
		for (com.watabou.noosa.Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive) continue;
			if (m instanceof com.watabou.noosa.Group) collect( (com.watabou.noosa.Group) m, into );
			else if (m instanceof com.watabou.noosa.Visual && !(m instanceof CharSprite)
					&& !(m instanceof xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight)
					&& !(m instanceof xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal)) into.put( m, Boolean.TRUE );
		}
	}

	private static float shook() throws Exception {
		Field f = xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx.class.getDeclaredField( "shookPx" );
		f.setAccessible( true );
		return f.getFloat( null );
	}

	private static void unshake() throws Exception {
		Field f = xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx.class.getDeclaredField( "shookPx" );
		f.setAccessible( true );
		f.setFloat( null, 0f );
	}

	//what a run draws over 40 frames: [quads born, the most born on one frame, the most lights at once]
	private int[] measure( Runnable run ){
		final int lights0 = xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight.live();
		final int[] lights = { 0 };
		//the lights read on each of born's frames
		com.watabou.noosa.Gizmo reader = new com.watabou.noosa.Gizmo(){
			@Override
			public void update(){
				lights[0] = Math.max( lights[0], xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight.live() - lights0 );
			}
		};
		rig.stage.add( reader );
		int[] b = born( run, 40, 0f, 0f, 0f );
		reader.killAndErase();
		return new int[]{ b[0], b[1], lights[0] };
	}

	@Test
	public void theBusyBlowShowsTwoCardsAndNoWordAndDrawsAllItDrewBefore() throws Exception {
		Game.elapsed = 1 / 60f;
		final Hero h = hero( at( 5, 6 ) );
		Dungeon.hero = h;
		xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword sword = new xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword();
		sword.enchant( new xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing() );
		h.belongings.weapon = sword;
		final CharSprite d = rig.rat( at( 6, 6 ) );

		//the same calls with no blow open: the pictures as they were before the ledger
		unshake();
		xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom.seedForTests( 4 );
		int[] before = measure( () -> xyz.gabriwar.warpedpixeldungeon.effects.fx.BusyFightTest.busyCalls( h, d ) );
		float shookBefore = shook();
		Game.timeTotal += 3f;
		HitCard.reset();

		TestParty.join( new Hero() );
		NetVisuals.clear();
		unshake();
		xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom.seedForTests( 4 );
		final List<List<Runnable>> posts = new ArrayList<>();
		int[] blow = measure( () -> posts.add( keepingPosts( () -> xyz.gabriwar.warpedpixeldungeon.effects.fx.BusyFightTest.busyBlow( h, d ) ) ) );
		List<Runnable> posted = posts.get( 0 );
		System.out.println( "the busy blow: " + blow[0] + " quads, " + blow[1] + " on one frame, " + blow[2] + " lights; "
				+ "its calls alone: " + before[0] + " quads, " + before[1] + " on one frame, " + before[2] + " lights" );
		assertTrue( "every picture it drew before: " + blow[0] + " / " + before[0], blow[0] >= before[0] - 2 && blow[0] <= before[0] + 2 );
		assertTrue( "but not all on one frame: " + blow[1] + " / " + before[1], blow[1] < before[1] );
		assertEquals( "its lights as before", before[2], blow[2] );
		assertEquals( "its shake as before", shookBefore, shook(), 0f );
		assertEquals( "no floating word", 0, texts( posted ) );
		int flashes = 0;
		JSONArray sent = NetVisuals.drainEvents();
		for (int i = 0; sent != null && i < sent.length(); i++) if ("sf".equals( sent.optJSONObject( i ).optString( "t" ) )) flashes++;
		assertEquals( "one white flash", 1, flashes );
		HitCard card = EnchantState.get( d ).card;
		assertNotNull( card );
		assertTrue( card.alive );
		assertEquals( 30, card.number() );
		assertTrue( "the X", card.crit() );
		assertEquals( "the flame's chip, the storm's hollow one and the burning's", 3, card.chipCount() );
		HitCard heal = EnchantState.get( h.sprite ).gains[0];
		assertNotNull( "the hero's small card", heal );
		assertTrue( heal.alive );
		assertEquals( 4, heal.number() );
		int cards = 0;
		for (com.watabou.noosa.Gizmo g : rig.layers[xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.Layer.STATUSES.ordinal()].membersView()){
			if (g instanceof HitCard && g.alive) cards++;
		}
		assertEquals( "two cards", 2, cards );
	}

	//the pictures a run makes over `frames` frames after it (the cards' aside)
	private int madeBy( Runnable run, int frames ){
		java.util.IdentityHashMap<com.watabou.noosa.Gizmo, Boolean> before = new java.util.IdentityHashMap<>(), now = new java.util.IdentityHashMap<>();
		pictures( before );
		run.run();
		int made = 0;
		for (int f = 0; f <= frames; f++){
			now.clear();
			pictures( now );
			for (com.watabou.noosa.Gizmo g : now.keySet()) if (!before.containsKey( g )) made++;
			before.clear();
			before.putAll( now );
			rig.frames( 1, true );
		}
		return made;
	}

	private static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook look( Class<?> c ){
		return xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks.of( c );
	}

	//the owner's call: every proc's look in full, the echo as on master, the blood as on master
	@Test
	public void everyProcDrawsItsBeatTheEchoItsOwnLookAndTheBloodAsEver(){
		Game.elapsed = 1 / 60f;
		final CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		final float mid = EnchantFx.strengthOf( EnchantFx.MID );
		final Class<?>[] three = { xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Venomous.class };

		//three procs on one blow, against one alone and today's three
		Game.timeTotal += 2f;
		int alone = madeBy( () -> EnchantFx.proc( look( three[0] ), a.ch, d.ch, mid, 0, 0 ), 40 );
		Game.timeTotal += 2f;
		int together = madeBy( () -> {
			Blow.begin( a.ch, d.ch );
			for (Class<?> c : three) EnchantFx.proc( look( c ), a.ch, d.ch, mid, 0, 0 );
			Blow.end();
		}, 40 );
		Game.timeTotal += 2f;
		int threeToday = madeBy( () -> {
			for (Class<?> c : three) EnchantFx.proc( look( c ), a.ch, d.ch, mid, 0, 0 );
		}, 40 );
		assertTrue( "three procs on a blow draw three beats: " + together + " against one's " + alone, together > alone + 6 );
		assertTrue( "each its full beat, as many as the three alone: " + together + " / " + threeToday,
				together >= threeToday - 2 && together <= threeToday + 2 );

		//the echo: its look on a blow is today's, beat for beat
		Game.timeTotal += 2f;
		xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom.seedForTests( 9 );
		int echoToday = madeBy( () -> EnchantFx.proc( look( three[1] ), a.ch, d.ch, mid, 0, EnchantFx.ECHO ), 40 );
		Game.timeTotal += 2f;
		xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom.seedForTests( 9 );
		int echoBlow = madeBy( () -> {
			Blow.begin( a.ch, d.ch );
			EnchantFx.proc( look( three[1] ), a.ch, d.ch, mid, 0, EnchantFx.ECHO );
			Blow.end();
		}, 40 );
		assertEquals( "the echo as on master", echoToday, echoBlow );

		//the blood of a third quick blow, as outside one
		Game.timeTotal += 2f;
		int bloodToday = madeBy( () -> d.bloodBurstA( a.center(), 30 ), 10 );
		Game.timeTotal += 2f;
		blow( a.ch, d.ch );
		Game.timeTotal += 0.1f;
		blow( a.ch, d.ch );
		Game.timeTotal += 0.1f;
		int bloodBlow = madeBy( () -> {
			Blow.begin( a.ch, d.ch );
			d.bloodBurstA( a.center(), 30 );
			Blow.end();
		}, 10 );
		assertEquals( "the blood as on master", bloodToday, bloodBlow );
	}

	//the pictures a run makes over `frames` frames after it, each counted as it is born: a kit
	//particle the kit reused in the same frame (its life begun again) counts as born again, and one
	//first seen half through its life (the stage's own) is no part of it
	private int bornOf( Runnable run, int frames ){
		return born( run, frames, 0f, 0f, 0f )[0];
	}

	//the same, only those born within `within` px (either way) of (cx, cy)
	private int bornNear( Runnable run, int frames, float cx, float cy, float within ){
		return born( run, frames, cx, cy, within )[0];
	}

	//[pictures born, the most born on one frame]
	private int[] born( Runnable run, int frames, float cx, float cy, float within ){
		java.util.IdentityHashMap<com.watabou.noosa.Gizmo, Float> before = new java.util.IdentityHashMap<>(), now = new java.util.IdentityHashMap<>();
		ages( before );
		run.run();
		int made = 0, most = 0;
		for (int f = 0; f <= frames; f++){
			now.clear();
			ages( now );
			int fresh = 0;
			for (java.util.Map.Entry<com.watabou.noosa.Gizmo, Float> e : now.entrySet()){
				Float was = before.get( e.getKey() );
				float p = e.getValue();
				boolean born = was == null || p < was;
				com.watabou.noosa.Visual v = (com.watabou.noosa.Visual) e.getKey();
				boolean near = within <= 0 || (Math.abs( v.x - cx ) <= within && Math.abs( v.y - cy ) <= within);
				if (born && p < 0.25f && near) fresh++;
			}
			made += fresh;
			most = Math.max( most, fresh );
			before.clear();
			before.putAll( now );
			rig.frames( 1, true );
		}
		return new int[]{ made, most };
	}

	//the pictures alive on the layers, each with how far through its life it is (0 for one not a kit particle)
	private void ages( java.util.IdentityHashMap<com.watabou.noosa.Gizmo, Float> into ){
		java.util.IdentityHashMap<com.watabou.noosa.Gizmo, Boolean> alive = new java.util.IdentityHashMap<>();
		pictures( alive );
		for (com.watabou.noosa.Gizmo g : alive.keySet()){
			into.put( g, g instanceof xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle
					? ((xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle) g).progress() : 0f );
		}
	}

	//a blow of a on d: its number, its blood and its star, as Char.attack raises them
	private static void plainBlow( CharSprite a, CharSprite d, int damage ){
		Blow.begin( a.ch, d.ch );
		d.showStatusWithIcon( CharSprite.NEGATIVE, Integer.toString( damage ), FloatingText.PHYS_DMG );
		d.hitFx( a.x + 8, a.y + 8, damage, HitFx.SLASH, false );
		Blow.end();
	}

	@Test
	public void aQuickBlowsStarIsAsWholeAsAFreshOne(){
		Game.elapsed = 1 / 60f;
		final CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		d.ch.HP = d.ch.HT = 100000;
		//the hit's own place (the rats' idle looks aside)
		final float hx = d.x + d.width() / 2f, hy = d.y + d.height() / 2f;
		Game.timeTotal += 2f;
		xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom.seedForTests( 3 );
		int alone = bornNear( () -> HitFx.hit( d, a.x + 8, a.y + 8, 4, HitFx.SLASH, false ), 8, hx, hy, 8f );
		//a third blow 0.2 s after two others, then two more in one frame
		Game.timeTotal += 2f;
		plainBlow( a, d, 4 );
		rig.frames( 12, true );
		plainBlow( a, d, 4 );
		rig.frames( 12, true );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom.seedForTests( 3 );
		int third = bornNear( () -> plainBlow( a, d, 4 ), 8, hx, hy, 8f );
		rig.frames( 12, true );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom.seedForTests( 3 );
		int both = bornNear( () -> {
			plainBlow( a, d, 4 );
			plainBlow( a, d, 4 );
		}, 8, hx, hy, 8f );
		assertEquals( "a third quick blow's star, whole", alone, third );
		assertEquals( "two in one frame: two whole stars", 2 * alone, both );
		assertEquals( "one card for them all", 20, EnchantState.get( d ).card.number() );
	}

	@Test
	public void aTwistUnderAProcDrawsItsWholeLookOnItsBeat(){
		Game.elapsed = 1 / 60f;
		final CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		d.ch.HP = d.ch.HT = 100000;
		final xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Javelin javelin = new xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Javelin();
		final float mid = EnchantFx.strengthOf( EnchantFx.MID );
		Game.timeTotal += 2f;
		xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom.seedForTests( 6 );
		int proc = bornOf( () -> {
			Blow.begin( a.ch, d.ch );
			EnchantFx.proc( look( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing.class ), a.ch, d.ch, mid, 0, 0 );
			Blow.end();
		}, 40 );
		Game.timeTotal += 2f;
		xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom.seedForTests( 7 );
		int heave = bornOf( () -> xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.heaveHit( a.ch, d.ch, javelin ), 40 );
		Game.timeTotal += 2f;
		xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom.seedForTests( 6 );
		int both = bornOf( () -> {
			Blow.begin( a.ch, d.ch );
			EnchantFx.proc( look( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing.class ), a.ch, d.ch, mid, 0, 0 );
			xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.heaveHit( a.ch, d.ch, javelin );
			Blow.end();
		}, 40 );
		assertTrue( "the heave whole under the proc: " + both + " against " + proc + " + " + heave,
				both >= proc + heave - 2 && both <= proc + heave + 2 );
	}

	@Test
	public void aPerksAccentDrawsOnItsBeatBesideItsChip(){
		Game.elapsed = 1 / 60f;
		final CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		d.ch.HP = d.ch.HT = 100000;
		//a Marksman's or Gunner's shot from afar, a Reach blow, a Flurry's free hit, a Crush sunder, a
		//stack of Momentum and a full one: each draws its own accent besides the plain blow's star
		Runnable[] perks = {
				() -> xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.TypeFx.longshot( a.ch, d.ch, 3 ),
				() -> xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.TypeFx.longshot( a.ch, d.ch, 6 ),
				() -> xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.TypeFx.reach( a.ch, d.ch ),
				() -> xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.TypeFx.flurry( a.ch, d.ch ),
				() -> xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.TypeFx.sunder( a.ch, d.ch, true ),
				() -> xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.TypeFx.momentum( a.ch, d.ch, 1 ),
				() -> xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.TypeFx.momentum( a.ch, d.ch, 4 ) };
		int[] least = { 1, 4, 3, 4, 2, 1, 4 };
		Game.timeTotal += 2f;
		xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom.seedForTests( 2 );
		int plain = bornOf( () -> plainBlow( a, d, 4 ), 12 );
		for (int i = 0; i < perks.length; i++){
			final Runnable perk = perks[i];
			Game.timeTotal += 2f;
			xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom.seedForTests( 2 );
			int with = bornOf( () -> {
				Blow.begin( a.ch, d.ch );
				perk.run();
				d.showStatusWithIcon( CharSprite.NEGATIVE, "4", FloatingText.PHYS_DMG );
				d.hitFx( a.x + 8, a.y + 8, 4, HitFx.SLASH, false );
				Blow.end();
			}, 12 );
			assertTrue( "perk " + i + " draws its accent: " + with + " against the plain " + plain, with >= plain + least[i] );
		}
	}

	@Test
	public void theXShowsOnItsCritsBeat(){
		Game.elapsed = 0.005f;
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Blow.begin( a.ch, d.ch );
		Blow.offer( new Fake( Blow.PROC, Blow.TARGET, d.ch ) );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.crit( a.ch, d.ch );
		Blow.claim( d.ch, "9", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		Blow.end();
		HitCard c = EnchantState.get( d ).card;
		assertTrue( "the X waits for its accent", c.leftIcon() != FloatingText.CRIT );
		rig.frames( Math.round( BlowPresenter.BEAT / Game.elapsed ) + 2, true );
		assertEquals( "and shows on its beat, stamped", FloatingText.CRIT, c.leftIcon() );
		assertTrue( c.stamping() || c.age() > BlowPresenter.BEAT );
	}

	@Test
	public void theLifestealHealShowsWhenTheBeadsLand(){
		Game.elapsed = 1 / 100f;
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HealFx.reset();
		int pools = xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight.live();
		Blow.begin( a.ch, d.ch );
		assertTrue( Blow.claim( a.ch, "4", CharSprite.POSITIVE, FloatingText.HEALING ) );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.lifesteal( a.ch, d.ch );
		Blow.end();
		float land = xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.BEADS_AT
				+ xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.BEADS_HOME;
		rig.frames( Math.round( land * 100 ) - 3, true );
		assertEquals( "not before the last bead", 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HealFx.heals() );
		EnchantState st = EnchantState.get( a );
		assertTrue( "no card yet", st == null || st.gains[0] == null );
		rig.frames( 6, true );
		assertEquals( "as it lands", 1, xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HealFx.heals() );
		assertEquals( 4, EnchantState.get( a ).gains[0].number() );
		assertTrue( "the heal's whole look: its pool of light too", xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight.live() > pools );
	}

	@Test
	public void anExtraYieldRisesInGoldAndReachesTheGuests() throws Exception {
		final xyz.gabriwar.warpedpixeldungeon.items.Item log = new xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword();
		final int cell = at( 4, 4 );
		java.util.IdentityHashMap<com.watabou.noosa.Gizmo, Boolean> before = new java.util.IdentityHashMap<>(), now = new java.util.IdentityHashMap<>();
		pictures( before );
		TestParty.join( new Hero() );
		NetVisuals.clear();
		assertEquals( "nothing of the game's Random", 0, EnchantFxTest.draws( () -> {
			xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.extra( cell, log, 1.3f );
			xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.extraGlint( cell + 1 );
		} ) );
		pictures( now );
		int made = 0;
		for (com.watabou.noosa.Gizmo g : now.keySet()) if (!before.containsKey( g )) made++;
		assertEquals( "a ghost and a glint, and a glint", 3, made );
		JSONArray sent = NetVisuals.drainEvents();
		int extras = 0;
		for (int i = 0; sent != null && i < sent.length(); i++){
			JSONObject e = sent.optJSONObject( i );
			JSONArray ints = e.optJSONArray( "i" );
			if ("eh".equals( e.optString( "t" ) ) && ints != null && ints.optInt( 0 ) == xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.EXTRA){
				extras++;
				assertTrue( ints.optInt( 1 ) == cell || ints.optInt( 1 ) == cell + 1 );
			}
		}
		assertEquals( "both sent", 2, extras );
		//not a blow's: inside one it still draws at once
		Blow.begin( rig.rat( at( 5, 6 ) ).ch, rig.rat( at( 6, 6 ) ).ch );
		pictures( before );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.extraGlint( cell );
		now.clear();
		pictures( now );
		Blow.end();
		assertTrue( "drawn inside a blow too", now.size() > before.size() );
	}

	//a blow a host's rat strikes a rat with: a Blazing proc, a crit, its number, its blood and flash
	private static void hostBlow( CharSprite a, CharSprite d ){
		Blow.begin( a.ch, d.ch );
		EnchantFx.proc( xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks.of(
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing.class ), a.ch, d.ch,
				EnchantFx.strengthOf( EnchantFx.MID ), 0, 0 );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.crit( a.ch, d.ch );
		d.showStatusWithIcon( CharSprite.NEGATIVE, "14", FloatingText.PHYS_DMG );
		d.bloodBurstA( a.center(), 14 );
		d.flash();
		Blow.end();
	}

	@Test
	public void aHostSendsTheBlowsPiecesTaggedThenItsCard() throws Exception {
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		TestParty.join( new Hero() );
		NetVisuals.clear();
		hostBlow( a, d );
		JSONArray sent = NetVisuals.drainEvents();
		int serial = 0, pieces = 0, cards = 0, texts = 0;
		boolean cardLast = false;
		for (int i = 0; i < sent.length(); i++){
			JSONObject e = sent.getJSONObject( i );
			String t = e.optString( "t" );
			if ("ft".equals( t )) texts++;
			if ("sb".equals( t ) || "sf".equals( t )) assertTrue( t + " tagged", e.optInt( "s", 0 ) != 0 );
			if (!"eh".equals( t )) continue;
			assertTrue( "tagged", e.optInt( "s", 0 ) != 0 );
			if (serial == 0) serial = e.getInt( "s" );
			assertEquals( "one blow", serial, e.getInt( "s" ) );
			if (e.getInt( "k" ) == xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantNet.CARD){
				cards++;
				JSONArray c = e.getJSONArray( "i" );
				assertEquals( d.ch.id(), c.getInt( 0 ) );
				assertEquals( serial, c.getInt( 2 ) );
				assertEquals( 14, c.getInt( BlowPresenter.CARD_NUMBER ) );
				assertEquals( "the crit", 1, c.getInt( BlowPresenter.CARD_CRIT ) );
				assertEquals( "the proc leads: the first piece", 0, c.getInt( BlowPresenter.CARD_LEAD ) );
				assertEquals( "every accent named", 1, c.getInt( BlowPresenter.CARD_ACCENTS ) );
				assertEquals( "the crit is the first accent", 1, c.getInt( BlowPresenter.CARD_HEAD ) );
				cardLast = true;
			} else {
				pieces++;
				cardLast = false;
			}
		}
		assertEquals( "the proc and the crit", 2, pieces );
		assertEquals( 1, cards );
		assertTrue( "the card after the pieces", cardLast );
		assertEquals( "the number is not sent as a text", 0, texts );
	}

	//a guest's stage: his own rats standing for the host's, their sprites found by the host's ids
	private CharSprite[] guestOf( CharSprite a, CharSprite d ) throws Exception {
		CharSprite ga = rig.rat( at( 3, 10 ) ), gd = rig.rat( at( 4, 10 ) );
		java.util.Map<Integer, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob> mobs = new java.util.HashMap<>();
		mobs.put( a.ch.id(), (xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob) ga.ch );
		mobs.put( d.ch.id(), (xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob) gd.ch );
		NetVisuals.setSpectatorMobs( mobs );
		TestParty.guest();
		return new CharSprite[]{ ga, gd };
	}

	@Test
	public void aGuestPresentsTheBlowWhenItsCardComesAndFallsBackWithout() throws Exception {
		Game.elapsed = 1 / 60f;
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		TestParty.join( new Hero() );
		NetVisuals.clear();
		hostBlow( a, d );
		JSONArray sent = NetVisuals.drainEvents();
		try {
			CharSprite[] g = guestOf( a, d );
			NetVisuals.replay( sent );
			HitCard card = EnchantState.get( g[1] ).card;
			assertNotNull( "the guest's card", card );
			assertEquals( 14, card.number() );
			assertTrue( card.crit() );
			assertEquals( "the flame's chip", 1, card.chipCount() );

			//the pieces without their card: held, then played as they are
			Game.timeTotal += 2f;
			JSONArray noCard = new JSONArray();
			for (int i = 0; i < sent.length(); i++){
				JSONObject e = sent.getJSONObject( i );
				if ("eh".equals( e.optString( "t" ) ) && e.getInt( "k" ) == xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantNet.CARD) continue;
				noCard.put( e );
			}
			HitCard.reset();
			java.util.IdentityHashMap<com.watabou.noosa.Gizmo, Boolean> before = new java.util.IdentityHashMap<>(), now = new java.util.IdentityHashMap<>();
			pictures( before );
			NetVisuals.replay( noCard );
			pictures( now );
			int held = 0;
			for (com.watabou.noosa.Gizmo q : now.keySet()) if (!before.containsKey( q )) held++;
			assertEquals( "held: nothing drawn yet", 0, held );
			rig.frames( Math.round( xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantNet.HOLD_FOR * 60 ) + 2, true );
			now.clear();
			pictures( now );
			int played = 0;
			for (com.watabou.noosa.Gizmo q : now.keySet()) if (!before.containsKey( q )) played++;
			assertTrue( "played as they are after " + xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantNet.HOLD_FOR + " s: " + played,
					played > 0 );

			//an older host's pictures (no serial) play at once
			JSONArray old = new JSONArray();
			for (int i = 0; i < noCard.length(); i++){
				JSONObject e = new JSONObject( noCard.getJSONObject( i ).toString() );
				e.remove( "s" );
				old.put( e );
			}
			before.clear();
			pictures( before );
			NetVisuals.replay( old );
			now.clear();
			pictures( now );
			int atOnce = 0;
			for (com.watabou.noosa.Gizmo q : now.keySet()) if (!before.containsKey( q )) atOnce++;
			assertTrue( "an old payload plays at once: " + atOnce, atOnce > 0 );
		} finally {
			NetVisuals.setSpectatorMobs( null );
		}
	}

	//the card a drained batch carries for a character, its numbers; null for none
	private static JSONArray cardOf( JSONArray sent, int id ) throws Exception {
		for (int i = 0; i < sent.length(); i++){
			JSONObject e = sent.getJSONObject( i );
			if (!"eh".equals( e.optString( "t" ) ) || e.getInt( "k" ) != xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantNet.CARD) continue;
			JSONArray c = e.getJSONArray( "i" );
			if (c.getInt( 0 ) == id) return c;
		}
		return null;
	}

	@Test
	public void aGuestKeepsTheHostsBeatsPastAnAccentItWasNotSent() throws Exception {
		Game.elapsed = 0.005f;
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		TestParty.join( new Hero() );
		NetVisuals.clear();
		//a proc leads; a host-only accent takes the first beat, the crit the second
		Blow.begin( a.ch, d.ch );
		EnchantFx.proc( xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks.of(
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing.class ), a.ch, d.ch,
				EnchantFx.strengthOf( EnchantFx.MID ), 0, 0 );
		Fake hostOnly = new Fake( Blow.MINOR, Blow.TARGET, d.ch );
		hostOnly.priority = Blow.A_TWIST;
		assertTrue( Blow.offer( hostOnly ) );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.crit( a.ch, d.ch );
		d.showStatusWithIcon( CharSprite.NEGATIVE, "14", FloatingText.PHYS_DMG );
		d.bloodBurstA( a.center(), 14 );
		Blow.end();
		JSONArray sent = NetVisuals.drainEvents();
		JSONArray c = cardOf( sent, d.ch.id() );
		assertNotNull( c );
		assertEquals( "a place for each beat", 2, c.getInt( BlowPresenter.CARD_ACCENTS ) );
		assertEquals( "the first beat has nothing for the guests", -1, c.getInt( BlowPresenter.CARD_HEAD ) );
		assertEquals( "the crit on the second", 1, c.getInt( BlowPresenter.CARD_HEAD + 1 ) );
		try {
			CharSprite[] g = guestOf( a, d );
			float t0 = Game.timeTotal;
			NetVisuals.replay( sent );
			rig.frames( 60, true );
			assertEquals( "the X on the host's second beat", t0 + 2 * BlowPresenter.BEAT, EnchantState.get( g[1] ).critAt, 0.011f );
		} finally {
			NetVisuals.setSpectatorMobs( null );
		}
	}

	@Test
	public void aBlowOutOfTheHostsSightTagsNothingAGuestWouldWaitOn() throws Exception {
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		TestParty.join( new Hero() );
		NetVisuals.clear();
		d.visible = false;
		//the struck one unseen; a proc on the attacker, whom the host sees, comes with it
		Blow.begin( a.ch, d.ch );
		EnchantFx.proc( xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks.of(
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing.class ), d.ch, a.ch,
				EnchantFx.strengthOf( EnchantFx.MID ), 0, 0 );
		d.bloodBurstA( a.center(), 9 );
		d.flash();
		Blow.end();
		JSONArray sent = NetVisuals.drainEvents();
		assertNotNull( sent );
		int looked = 0;
		for (int i = 0; i < sent.length(); i++){
			JSONObject e = sent.getJSONObject( i );
			String t = e.optString( "t" );
			if ("eh".equals( t ) && e.getInt( "k" ) == xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantNet.CARD){
				assertEquals( "only another's small card", 1, e.getJSONArray( "i" ).getInt( BlowPresenter.CARD_SMALL ) );
				continue;
			}
			if (!"eh".equals( t ) && !"sb".equals( t ) && !"sf".equals( t )) continue;
			looked++;
			assertEquals( t + ": no card of the struck one will come, so untagged", 0, e.optInt( "s", 0 ) );
		}
		assertTrue( "the proc and the blood at least: " + looked, looked >= 2 );
	}

	@Test
	public void aNumberClaimedWhileSeenKeepsItsCardWhenTheStruckOneGoesOutOfSight(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), unseen = rig.rat( at( 6, 8 ) );
		Blow.begin( a.ch, d.ch );
		d.showStatusWithIcon( CharSprite.NEGATIVE, "7", FloatingText.PHYS_DMG );
		//a proc after the damage moves it out of the hero's sight before the blow is presented
		d.visible = false;
		Blow.end();
		EnchantState st = EnchantState.get( d );
		assertNotNull( "its card", st == null ? null : st.card );
		assertEquals( "with the number claimed while it was seen", 7, st.card.number() );
		//one never seen claims nothing, and gets no card
		unseen.visible = false;
		Blow.begin( a.ch, unseen.ch );
		unseen.showStatusWithIcon( CharSprite.NEGATIVE, "4", FloatingText.PHYS_DMG );
		Blow.end();
		EnchantState su = EnchantState.get( unseen );
		assertTrue( "no card on one never seen", su == null || su.card == null );
	}

	@Test
	public void numbersClaimedWhileSeenOnTheAttackerAndAnotherStruckOneKeepTheirCards(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), o = rig.rat( at( 7, 6 ) );
		Blow.begin( a.ch, d.ch );
		d.showStatusWithIcon( CharSprite.NEGATIVE, "7", FloatingText.PHYS_DMG );
		//the attacker's own reflected damage, and a second one the blow struck
		a.showStatusWithIcon( CharSprite.NEGATIVE, "2", FloatingText.PHYS_DMG );
		o.showStatusWithIcon( CharSprite.NEGATIVE, "3", FloatingText.PHYS_DMG );
		//both out of sight before the blow is presented
		a.visible = false;
		o.visible = false;
		Blow.end();
		EnchantState sa = EnchantState.get( a );
		assertNotNull( "the attacker's card", sa == null ? null : sa.card );
		assertEquals( "with its reflected number", 2, sa.card.number() );
		assertEquals( "the struck one's card, the attacker's and the other one's small card", 3, HitCard.alive() );
	}

	@Test
	public void nestedBlowsCloseInOrder(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d1 = rig.rat( at( 6, 6 ) ), d2 = rig.rat( at( 6, 7 ) );
		Blow.begin( a.ch, d1.ch );
		Blow.claim( d1.ch, "4", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		int outer = Blow.serial();
		Blow.begin( a.ch, d2.ch );
		assertTrue( Blow.serial() != outer );
		Blow.claim( d2.ch, "9", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		Blow.end();
		assertEquals( "the inner one presented first", 9, EnchantState.get( d2 ).card.number() );
		EnchantState st1 = EnchantState.get( d1 );
		assertTrue( "the outer not yet", st1 == null || st1.card == null );
		assertEquals( outer, Blow.serial() );
		Blow.claim( d1.ch, "2", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		Blow.end();
		assertEquals( 6, EnchantState.get( d1 ).card.number() );
		assertEquals( "nothing open", 0, Blow.serial() );
	}
}
