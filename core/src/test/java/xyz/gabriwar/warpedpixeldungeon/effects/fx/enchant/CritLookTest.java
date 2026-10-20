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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.TrueEdge;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
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
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * The red X (PieceFx.crit), headless on EnchantFxTest's stage: three quads at most, no light, no
 * shake; on the card the X takes the left slot (cell 4); stroke B comes XB_LATE s after stroke A and
 * both are gone by X_FOR s; and True Edge's crit raises no word and draws exactly as much of the
 * game's Random as before (its star burst's one draw, kept as a stand-in).
 */
public class CritLookTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		HitCard.reset();
		Sting.out = new EnchantFxTest.Ear();
	}

	@After
	public void tearDown(){
		HitCard.reset();
		Actor.clear();
		rig.down();
	}

	private static float shook() throws Exception {
		Field f = Fx.class.getDeclaredField( "shookPx" );
		f.setAccessible( true );
		return f.getFloat( null );
	}

	@Test
	public void threeQuadsNoLightNoShake() throws Exception {
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		int lights = FxLight.live();
		int[] m = rig.made( () -> PieceFx.runCrit( a, d, true, true, false ), 60 );
		assertTrue( "quads " + m[0], m[0] > 0 && m[0] <= PieceFx.CRIT_CAP );
		assertEquals( "no light", 0, m[1] );
		assertEquals( lights, FxLight.live() );
		assertEquals( "no shake, the hero in it or not", 0f, shook(), 0f );
	}

	//the X's matter strokes alive on the effects' layer, in the order they were made
	private List<EnchantFx.Flash> strokes(){
		List<EnchantFx.Flash> out = new ArrayList<>();
		Group effects = GameScene.layer( GameScene.Layer.EFFECTS );
		for (Gizmo g : effects.membersView()) if (g instanceof EnchantFx.Flash && g.alive) out.add( (EnchantFx.Flash) g );
		return out;
	}

	@Test
	public void strokeBComesAfterAAndBothAreGoneInTime(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Game.elapsed = 0.01f;
		PieceFx.runCrit( a, d, false, false, false );
		List<EnchantFx.Flash> x = strokes();
		assertEquals( "two strokes", 2, x.size() );
		EnchantFx.Flash sa = x.get( 0 ), sb = x.get( 1 );
		float firstA = -1f, firstB = -1f;
		for (int f = 1; f <= 60; f++){
			rig.frames( 1, false );
			float t = f * Game.elapsed;
			if (firstA < 0 && sa.alive && sa.visible) firstA = t;
			if (firstB < 0 && sb.alive && sb.visible) firstB = t;
		}
		assertTrue( "A at once: " + firstA, firstA >= 0f && firstA <= 0.011f );
		assertEquals( "B " + PieceFx.XB_LATE + " s after A", PieceFx.XB_LATE, firstB - firstA, 0.011f );
		assertFalse( "A gone by " + PieceFx.X_FOR, sa.alive );
		assertFalse( "B gone by " + PieceFx.X_FOR, sb.alive );
	}

	@Test
	public void theCardShowsTheXInItsLeftSlot(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Blow.begin( a.ch, d.ch );
		PieceFx.crit( a.ch, d.ch );
		Blow.claim( d.ch, "12", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		Blow.end();
		HitCard c = EnchantState.get( d ).card;
		assertTrue( c.crit() );
		assertEquals( "cell 4", 4, c.leftIcon() );
		assertEquals( FloatingText.CRIT, c.leftIcon() );
	}

	@Test
	public void aBlowsCritShowsBesideTheNumberNotOverTheBody(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		//pellets in one frame: three crits, one card
		for (int i = 0; i < 3; i++){
			Blow.begin( a.ch, d.ch );
			PieceFx.crit( a.ch, d.ch );
			Blow.claim( d.ch, "4", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
			Blow.end();
		}
		Game.elapsed = 1 / 60f;
		boolean stamped = false;
		for (int f = 0; f < 30; f++){
			rig.frames( 1, false );
			assertEquals( "no X over the body", 0, strokes().size() );
			HitCard c = EnchantState.get( d ).card;
			stamped |= c != null && c.stamping();
		}
		HitCard c = EnchantState.get( d ).card;
		assertTrue( c.crit() );
		assertEquals( "the X beside the number", FloatingText.CRIT, c.leftIcon() );
		assertTrue( "it flashed as it landed", stamped );
	}

	@Test
	public void aCritWithNoCardStillDrawsItsX(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		PieceFx.runCrit( a, d, false, false, false );
		rig.frames( 3, false );
		assertEquals( "one X over the body", 2, strokes().size() );
	}

	//True Edge striking a rat two cells from the hero (no slash through), a crit at last
	private int trueEdge( Hero h, TrueEdge edge, CharSprite d, boolean sprite ){
		if (!sprite) d.ch.sprite = null;
		try {
			for (int seed = 1; seed < 2000; seed++){
				Random.pushGenerator( seed );
				int gate = Random.Int( 100 );
				Random.popGenerator();
				if (gate >= 5 * edge.level) continue;
				//once to make every class it touches (a class made for the first time may draw)
				Random.pushGenerator( seed );
				edge.onHitProc( d.ch, 10, false );
				Random.popGenerator();
				Random.pushGenerator( seed );
				int dealt = edge.onHitProc( d.ch, 10, false );
				float next = Random.Float();
				Random.popGenerator();
				assertEquals( "a crit", 15, dealt );
				//how many it drew: the next number after it, against the same seed drawn k times
				for (int k = 0; k < 8; k++){
					Random.pushGenerator( seed );
					for (int i = 0; i < k; i++) Random.Float();
					float f = Random.Float();
					Random.popGenerator();
					if (f == next) return k;
				}
				throw new AssertionError( "the draws of a crit could not be counted" );
			}
			throw new AssertionError( "no crit in two thousand seeds" );
		} finally {
			d.ch.sprite = d;
		}
	}

	@Test
	public void trueEdgeRaisesNoWordAndDrawsTheGamesRandomAsBefore(){
		Hero h = new Hero();
		h.pos = at( 4, 6 );
		h.HP = h.HT = 20;
		Dungeon.hero = h;
		CharSprite d = rig.rat( at( 6, 6 ) );
		TrueEdge edge = new TrueEdge();
		edge.level = 1;
		final List<Runnable> posted = new ArrayList<>();
		Application saved = Gdx.app;
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(), new Class<?>[]{ Application.class },
				(p, m, args) -> {
					if (m.getName().equals( "postRunnable" )){
						posted.add( (Runnable) args[0] );
						return null;
					}
					return saved != null ? m.invoke( saved, args ) : null;
				} );
		int withSprite, without;
		//the co-op manager made first: its making draws the game's Random once, which a first
		//white flash would otherwise count
		NetManager.isHost();
		try {
			withSprite = trueEdge( h, edge, d, true );
			without = trueEdge( h, edge, d, false );
		} finally {
			Gdx.app = saved;
		}
		for (Runnable r : posted){
			for (Field f : r.getClass().getDeclaredFields()){
				try {
					f.setAccessible( true );
					Object c = f.get( r );
					assertFalse( "no word", c != null && c.getClass().getName().startsWith( FloatingText.class.getName() ) );
				} catch (ReflectiveOperationException e){
					throw new AssertionError( e );
				}
			}
		}
		//its gate, and with a sprite the one draw its star burst made as it started (Emitter.start)
		assertEquals( "the gate alone without a sprite", 1, without );
		assertEquals( "the gate and the burst's stand-in", 2, withSprite );
	}

	//keeps the compiler honest about the emitter the stand-in replaced: a burst draws one at once
	@Test
	public void aBurstDrawsTheGamesRandomOnceAsItStarts(){
		assertEquals( 1, EnchantFxTest.draws( () -> new Emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory(
				xyz.gabriwar.warpedpixeldungeon.effects.Speck.STAR ), 6 ) ) );
	}
}
