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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Disarmed;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RivalStatue;
import xyz.gabriwar.warpedpixeldungeon.audio.Notes;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.BlowPresenter;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.WeaponPerk;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flute;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HandAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Katana;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MageBook;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.FT.FT;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Javelin;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.draws;

/**
 * The weapon types' looks (TypeFx, docs/fx/type-looks.md), headless on EnchantFxTest's stage: the
 * wire kinds are pinned; every piece keeps to its quads, full and as an accent; nothing draws the
 * game's Random, in a blow or out of one, from any thread's hand-off; the chord's arpeggio and the
 * Bard's notes are fixed pitches; the felling throw is wood; Disarmed wears its own icon and its
 * blow's card shows it; a Siphon's mana is the wielder's small card; no perk or statue word is left
 * to float; the Bard's held notes go to the guests (NOTES) and their pips show there; every piece
 * goes out and plays on a guest.
 */
public class TypeLooksTest {

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private EnchantFxTest.Ear ear;
	private String savedVersion;

	@Before
	public void setUp(){
		FxRandom.seedForTests( 0x7E5EL );
		rig = new EnchantFxTest.Rig();
		rig.up();
		ear = new EnchantFxTest.Ear();
		Sting.out = ear;
		savedVersion = Game.version;
		Game.version = "test";
		NetVisuals.clear();
		WieldLook.reset();
	}

	@After
	public void tearDown(){
		try {
			TestParty.leave();
		} catch (Exception e){
			throw new AssertionError( e );
		}
		NetVisuals.setSpectatorMobs( null );
		NetVisuals.clear();
		Actor.clear();
		WieldLook.reset();
		Game.version = savedVersion;
		rig.down();
		if (Dungeon.level != null) PathFinder.setMapSize( Dungeon.level.width(), Dungeon.level.height() );
	}

	// ------------------------------------------------------------------ the world

	//a plain sprite on the stage for a character, where it stands
	private CharSprite sprite( Char ch ){
		CharSprite s = new CharSprite(){
			@Override public Emitter emitter(){ return new Emitter(); }
			@Override public Emitter centerEmitter(){ return new Emitter(); }
			@Override public void operate( int cell ){}
		};
		s.ch = ch;
		ch.sprite = s;
		rig.stage.add( s );
		s.place( ch.pos );
		s.visible = true;
		return s;
	}

	//a character that only keeps count of its health, among the actors
	private Char dummy( int cell, Char.Alignment alignment, int hp ){
		Char ch = new Char(){
			@Override public void damage( int amount, Object source ){ HP -= amount; }
		};
		ch.pos = cell;
		ch.HP = ch.HT = hp;
		ch.alignment = alignment;
		sprite( ch );
		Actor.add( ch );
		return ch;
	}

	private Hero hero( int cell ){
		Hero h = new Hero();
		h.pos = cell;
		h.HT = 100;
		h.HP = 60;
		sprite( h );
		Dungeon.hero = h;
		Actor.add( h );
		return h;
	}

	//a weapon's quality of a type, no line, no enchantment
	private static Quality typed( Item item, ItemType type ){
		Quality.roll( item, Rarity.RARE, type, 0 );
		Quality q = item.quality;
		q.type = type;
		q.lines.clear();
		q.rolls.clear();
		if (item instanceof Weapon) ((Weapon) item).enchant( null );
		return q;
	}

	private static List<Char> list( Char... chars ){
		return new ArrayList<>( Arrays.asList( chars ) );
	}

	//every type's entry, as the game calls them, on the stage's rats (a the wielder, d the struck)
	private static void everyEntry( CharSprite a, CharSprite d, CharSprite n1, CharSprite n2 ){
		TypeFx.splinter( a.ch, d.ch, true );
		TypeFx.fell( a.ch, d.ch );
		TypeFx.daze( a.ch, d.ch );
		TypeFx.crack( a.ch, d.ch, list( d.ch, n1.ch, n2.ch ) );
		TypeFx.note( a.ch, false );
		TypeFx.chord( a.ch, d.ch, new Flute(), false, list( a.ch, n1.ch ), null, null );
		TypeFx.chord( a.ch, d.ch, new Flute(), true, null, list( d.ch, n2.ch ), n1.ch );
		TypeFx.resonance( a.ch, d.ch, new Flute(), list( n1.ch, n2.ch ) );
		TypeFx.lash( a.ch, d.ch );
		TypeFx.unarm( a.ch, d.ch );
		TypeFx.siphon( a.ch, d.ch );
		TypeFx.sunder( a.ch, d.ch, true );
		TypeFx.flurry( a.ch, d.ch );
		for (int k = 1; k <= 5; k++) TypeFx.momentum( a.ch, d.ch, k );
		TypeFx.bulwark( a.ch, d.ch );
		for (int k = 3; k <= 6; k++) TypeFx.longshot( a.ch, d.ch, k );
		TypeFx.reach( a.ch, d.ch );
		TypeFx.parry( d.ch, a.ch, 9 );
	}

	// ------------------------------------------------------------------ the wire

	@Test
	public void theKindsAndCodesArePinned(){
		int[] kinds = { PieceFx.SPLINTER, PieceFx.FELL, PieceFx.DAZE, PieceFx.CRACK, PieceFx.NOTE, PieceFx.CHORD, PieceFx.RESONANCE,
				PieceFx.LASH, PieceFx.UNARM, PieceFx.PERK, PieceFx.SIPHON };
		for (int i = 0; i < kinds.length; i++) assertEquals( 17 + i, kinds[i] );
		assertEquals( 16, PieceFx.EXTRA );
		assertEquals( 28, PieceFx.KINDS );
		assertEquals( 10, EnchantNet.NOTES );
		assertEquals( 9, EnchantNet.CARD );
		int[] perks = { TypeFx.SUNDER, TypeFx.FLURRY, TypeFx.MOMENTUM, TypeFx.BULWARK, TypeFx.LONGSHOT, TypeFx.REACH, TypeFx.PARRY };
		for (int i = 0; i < perks.length; i++) assertEquals( i, perks[i] );
		assertEquals( perks.length, TypeFx.PERK_CAP.length );
	}

	// ------------------------------------------------------------------ quads

	//what a run makes over two seconds: within its quads, no light (but `lights` for a lead's), one
	//decal at most; and something
	private void capped( String name, int cap, int lights, Runnable run ){
		Game.timeTotal += 3f;
		int[] m = rig.made( run, 120 );
		assertTrue( name + " made " + m[0] + " quads, cap " + cap, m[0] > 0 && m[0] <= cap );
		assertTrue( name + " lights " + m[1], m[1] <= lights );
		assertTrue( name + " decals " + m[2], m[2] <= 1 );
	}

	@Test
	public void everyPieceKeepsToItsQuads(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		CharSprite[] four = { rig.rat( at( 6, 5 ) ), rig.rat( at( 6, 7 ) ), rig.rat( at( 7, 6 ) ), rig.rat( at( 7, 5 ) ) };
		CharSprite[] two = { four[0], four[1] };
		for (boolean accent : new boolean[]{ false, true }){
			String how = accent ? " as an accent" : "";
			check( "splinter" + how, accent, TypeFx.SPLINTER_CAP, new TypeFx.Splinter( null, a, d, true, false ) );
			check( "felling blow" + how, accent, TypeFx.FELL_CAP, new TypeFx.Fell( null, a, d, true, false ) );
			check( "daze" + how, accent, TypeFx.DAZE_CAP, new TypeFx.Daze( null, a, d, true, false ) );
			check( "crack" + how, accent, accent ? TypeFx.CRACK_ACCENT : TypeFx.CRACK_CAP + TypeFx.CRACK_PER * TypeFx.RICH,
					new TypeFx.Crack( null, a, d, four.clone(), true, false ) );
			check( "note" + how, accent, TypeFx.NOTE_CAP, new TypeFx.Note( a.ch, a, false, true ) );
			check( "rally chord" + how, accent, accent ? TypeFx.CHORD_ACCENT : TypeFx.CHORD_CAP + TypeFx.MARKS,
					new TypeFx.Chord( d.ch, a, d, false, TypeFx.FLUTE, new CharSprite[]{ a, four[0], four[1], four[2] }, null, true, false ) );
			check( "dread chord and a charm" + how, accent, accent ? TypeFx.CHORD_ACCENT : TypeFx.CHORD_CAP + TypeFx.MARKS,
					new TypeFx.Chord( d.ch, a, d, true, TypeFx.HARP, new CharSprite[]{ d, four[0] }, four[3], true, false ) );
			check( "resonance" + how, accent, TypeFx.RESONANCE_CAP + TypeFx.RICH, new TypeFx.Resonance( null, a, d, false, four.clone(), true ) );
			check( "resonance of two" + how, accent, TypeFx.RESONANCE_CAP + 2, new TypeFx.Resonance( null, a, d, true, two, true ) );
			check( "lash" + how, accent, TypeFx.LASH_CAP, new TypeFx.Lash( null, a, d, true ) );
			check( "disarm" + how, accent, TypeFx.UNARM_CAP, new TypeFx.Unarm( null, a, d, true ) );
			check( "siphon" + how, accent, TypeFx.SIPHON_CAP, new TypeFx.Siphon( a.ch, a, d ) );
			for (int code = 0; code < TypeFx.PERK_CAP.length; code++){
				int n = code == TypeFx.MOMENTUM ? 4 : code == TypeFx.LONGSHOT ? 5 : 9;
				//a parry leading is HitFx's PARRY blow: its sparks' end twinkles (FxFactories.SPARK_END, up to
				//six) come besides its quads
				int cap = accent && code == TypeFx.PARRY ? 2 : code == TypeFx.PARRY ? TypeFx.PERK_CAP[code] + 6 : TypeFx.PERK_CAP[code];
				check( "perk " + code + how, accent, cap, new TypeFx.Perk( code, null, a, d, n, true, false ) );
			}
			check( "a stack of momentum" + how, accent, 1, new TypeFx.Perk( TypeFx.MOMENTUM, null, a, d, 1, true, false ) );
			check( "a shot from three tiles" + how, accent, 1, new TypeFx.Perk( TypeFx.LONGSHOT, null, a, d, 3, true, false ) );
			check( "a shot from four tiles" + how, accent, 2, new TypeFx.Perk( TypeFx.LONGSHOT, null, a, d, 4, true, false ) );
		}
	}

	//a piece's full form (alone, outside a blow) or its accent form, within its cap
	private void check( String name, boolean accent, int cap, TypeFx.TypePiece p ){
		//a parry leading is the HitFx PARRY blow, its bloom with it
		int lights = !accent && p instanceof TypeFx.Perk && ((TypeFx.Perk) p).code == TypeFx.PARRY ? 1 : 0;
		capped( name, cap, lights, () -> {
			if (accent) p.accent( p.ds.x, p.ds.y + 4f );
			else p.play( false );
		} );
	}

	@Test
	public void aDazeKeepsAwayWhenItsCrackLeads(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Game.timeTotal += 3f;
		//its own blow's crack led: however late its beat comes (three procs ahead of it), no specks
		TypeFx.Daze daze = new TypeFx.Daze( null, a, d, true, false );
		daze.crack = new TypeFx.Crack( null, a, d, new CharSprite[0], true, false );
		daze.crack.lead( false );
		Game.timeTotal += 0.4f;
		assertEquals( "no specks over a crack that led", 0, rig.made( () -> daze.accent( 0f, 0f ), 30 )[0] );

		//its crack only an accent (a proc led): the specks draw
		TypeFx.Daze besideAccent = new TypeFx.Daze( null, a, d, true, false );
		besideAccent.crack = new TypeFx.Crack( null, a, d, new CharSprite[0], true, false );
		besideAccent.crack.accent( d.x, d.y );
		assertEquals( "two specks beside a crack that did not lead", 2,
				rig.made( () -> besideAccent.accent( 0f, 0f ), 30 )[0] );

		//another blow's crack that led just now on the same sprite keeps nothing away
		new TypeFx.Crack( null, a, d, new CharSprite[0], true, false ).play( false );
		TypeFx.Daze alone = new TypeFx.Daze( null, a, d, true, false );
		assertEquals( "a daze of its own blow draws", 2, rig.made( () -> alone.accent( 0f, 0f ), 30 )[0] );

		//a guest's blow made again: its daze finds the crack beside it
		TypeFx.Crack gc = new TypeFx.Crack( null, a, d, new CharSprite[0], true, false );
		TypeFx.Daze gz = new TypeFx.Daze( null, a, d, true, false );
		TypeFx.link( new xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Piece[]{ gz, null, gc } );
		assertTrue( gz.crack == gc );
	}

	@Test
	public void aCrackOutsideABlowLeadsAlone(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Game.timeTotal += 3f;
		int[] m = rig.made( () -> TypeFx.crack( a.ch, d.ch, null ), 30 );
		assertTrue( "the crack draws", m[0] > 0 );
		assertEquals( "and no daze specks circle over it", 0, motes() );
	}

	@Test
	public void aResonanceThatStruckNoOneShowsNothing(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Game.timeTotal += 3f;
		NetVisuals.clear();
		assertEquals( 0, rig.made( () -> TypeFx.resonance( a.ch, d.ch, new Flute(), new ArrayList<>() ), 30 )[0] );
		assertEquals( 0, rig.made( () -> TypeFx.resonance( a.ch, d.ch, new Flute(), null ), 30 )[0] );
		assertNull( "nothing sent", NetVisuals.drainEvents() );
	}

	//the type motes alive on the light layer (pips, daze specks, taut lines)
	private int motes(){
		int n = 0;
		for (Gizmo g : rig.layers[GameScene.Layer.LIGHTS.ordinal()].membersView()){
			if (g instanceof TypeFx.Mote && g.alive) n++;
		}
		return n;
	}

	@Test
	public void aSpentPipLeavesItsPooledMoteAlone(){
		CharSprite s = rig.rat( at( 5, 6 ) );
		EnchantState.of( s );
		WieldLook.stage( s, 2, false, 10f );
		WieldLook.step( s );
		assertEquals( 2, WieldLook.pipsShown( s ) );
		//a chord takes them home; once flown they go back to the pool
		WieldLook.stage( s, 0, false, 0f );
		Game.timeTotal += 0.2f;
		WieldLook.step( s );
		assertEquals( 0, WieldLook.pipsShown( s ) );
		assertEquals( 0, motes() );
		//a daze over the same sprite takes its specks from that pool: the held notes leave them be
		new TypeFx.Daze( null, s, s, true, false ).play( false );
		assertEquals( 2, motes() );
		WieldLook.step( s );
		assertEquals( "the daze's specks still circle", 2, motes() );
		assertEquals( "and are no pips", 0, WieldLook.pipsShown( s ) );

		//pips that went on their own (the sprite unseen a while), their motes then taken by a daze: the
		//notes still held get pips of their own and leave the specks be
		float elapsed = Game.elapsed;
		Game.elapsed = 1f;
		try {
			rig.frames( 1, false );
		} finally {
			Game.elapsed = elapsed;
		}
		assertEquals( "the specks have circled", 0, motes() );
		WieldLook.stage( s, 2, false, 10f );
		WieldLook.step( s );
		assertEquals( 2, WieldLook.pipsShown( s ) );
		s.visible = false;
		Game.timeTotal += 2f;
		rig.frames( 1, false );
		assertEquals( "gone unplaced", 0, motes() );
		s.visible = true;
		new TypeFx.Daze( null, s, s, true, false ).play( false );
		WieldLook.step( s );
		assertEquals( "two specks and two new pips", 4, motes() );
		assertEquals( 2, WieldLook.pipsShown( s ) );
	}

	// ------------------------------------------------------------------ the game's Random

	@Test
	public void nothingDrawsTheGamesRandom() throws Exception {
		for (boolean layers : new boolean[]{ true, false }){
			rig.layers( layers );
			final CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), n1 = rig.rat( at( 6, 5 ) ), n2 = rig.rat( at( 7, 7 ) );
			assertEquals( "outside a blow" + (layers ? "" : ", no layers"), 0, draws( () -> {
				everyEntry( a, d, n1, n2 );
				rig.frames( 90, true );
			} ) );
			assertEquals( "inside a blow and its presenter" + (layers ? "" : ", no layers"), 0, draws( () -> {
				Blow.begin( a.ch, d.ch );
				try {
					everyEntry( a, d, n1, n2 );
					d.showStatusWithIcon( CharSprite.NEGATIVE, "9", FloatingText.PHYS_DMG );
				} finally {
					Blow.end();
				}
				rig.frames( 90, true );
			} ) );
		}

		//handed over from another thread (the actor's): nothing of the Random there, nor as the work runs
		rig.layers( true );
		final CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), n1 = rig.rat( at( 6, 5 ) ), n2 = rig.rat( at( 7, 7 ) );
		final List<Runnable> posted = new ArrayList<>();
		com.badlogic.gdx.Application saved = com.badlogic.gdx.Gdx.app;
		com.badlogic.gdx.Gdx.app = (com.badlogic.gdx.Application) java.lang.reflect.Proxy.newProxyInstance(
				com.badlogic.gdx.Application.class.getClassLoader(), new Class<?>[]{ com.badlogic.gdx.Application.class },
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
			assertEquals( "nothing of the Random off the render thread", 0, draws( () -> {
				Thread actor = new Thread( () -> everyEntry( a, d, n1, n2 ) );
				actor.start();
				try {
					actor.join();
				} catch (InterruptedException e){
					throw new AssertionError( e );
				}
			} ) );
			assertFalse( "the work was handed over", posted.isEmpty() );
			assertEquals( "nothing of the Random as it runs", 0, draws( () -> {
				for (Runnable r : posted) r.run();
				rig.frames( 90, true );
			} ) );
		} finally {
			com.badlogic.gdx.Gdx.app = saved;
		}
	}

	// ------------------------------------------------------------------ sounds

	@Test
	public void aChordsArpeggioIsThreeFixedPitches(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Object[][] cases = {
				{ TypeFx.FLUTE, false, Assets.Sounds.FLUTE_NOTE, new float[]{ 1f, 1.25f, 1.5f } },
				{ TypeFx.TRUMPET, true, Assets.Sounds.TRUMPET_NOTE, new float[]{ 1f, 1.2f, 1.5f } },
				{ TypeFx.HARP, false, Assets.Sounds.HARP_NOTE, new float[]{ 1f, 1.25f, 1.5f } },
				{ TypeFx.WARDRUM, false, Assets.Sounds.HIT_CRUSH, new float[]{ 0.7f, 0.875f, 1.05f } },
				{ TypeFx.TRIANGOLO, true, Assets.Sounds.BELL, new float[]{ 1.8f, 2.16f, 2.7f } } };
		for (Object[] c : cases){
			Game.timeTotal += 3f;
			ear.heard.clear();
			new TypeFx.Chord( d.ch, a, d, (Boolean) c[1], (Integer) c[0], new CharSprite[0], null, true, false ).play( false );
			rig.frames( 20, true );
			float[] want = (float[]) c[3];
			assertEquals( "three notes: " + ear.heard.size(), 3, ear.heard.size() );
			for (int i = 0; i < 3; i++){
				assertEquals( c[2], ear.heard.get( i )[0] );
				assertEquals( "note " + i + " of " + c[2], want[i], (Float) ear.heard.get( i )[3], 1e-4f );
			}
		}
	}

	@Test
	public void aBardsNoteStepsUpToItsChord() throws Exception {
		assertArrayEquals( new float[]{ 1.5f, 1f, 1.125f, 1.25f }, Notes.STEPS, 0f );
		Hero h = hero( at( 5, 6 ) );
		Flute flute = new Flute();
		Quality q = typed( flute, ItemType.GAMMA );
		h.belongings.weapon = flute;
		Char foe = dummy( at( 6, 6 ), Char.Alignment.ENEMY, 100000 );
		//each hit's note is read after its hook (attackProc before hitSound): up the scale, the chord on top
		float[] heard = new float[8];
		for (int i = 0; i < heard.length; i++){
			WeaponPerk.BARD.onHit( q, flute, h, foe, 10 );
			heard[i] = Notes.STEPS[q.notes];
		}
		assertArrayEquals( new float[]{ 1f, 1.125f, 1.25f, 1.5f, 1f, 1.125f, 1.25f, 1.5f }, heard, 0f );
	}

	// ------------------------------------------------------------------ the felling throw, Disarmed

	@Test
	public void theFellingThrowIsWood() throws Exception {
		Hero h = hero( at( 5, 6 ) );
		HandAxe axe = new HandAxe();
		Quality q = typed( axe, ItemType.ALPHA );
		h.belongings.weapon = axe;
		Char foe = dummy( at( 6, 6 ), Char.Alignment.ENEMY, 100000 );
		for (int i = 0; i < 3; i++) WeaponPerk.WOODSMAN.onHit( q, axe, h, foe, 10 );
		Pushing push = null;
		for (Actor act : Actor.all()) if (act instanceof Pushing) push = (Pushing) act;
		assertNotNull( "the third hit fells", push );
		Field tint = Pushing.class.getDeclaredField( "tint" );
		tint.setAccessible( true );
		assertEquals( TypeFx.WOOD, tint.getInt( push ) );
	}

	@Test
	public void aWoodsmansBarkFliesOnEveryHitAndItsChipOnlyWithAStack(){
		Hero h = hero( at( 5, 6 ) );
		HandAxe axe = new HandAxe();
		Quality q = typed( axe, ItemType.GAMMA );
		h.belongings.weapon = axe;
		Char foe = dummy( at( 6, 6 ), Char.Alignment.ENEMY, 100000 );
		//15 frames (half a second) a blow: past the merge, inside the card's life
		for (int i = 1; i <= 4; i++){
			Game.timeTotal += 3f;
			final boolean stacked = i <= 3;
			int[] withBark = rig.made( () -> {
				Blow.begin( h, foe );
				try {
					WeaponPerk.WOODSMAN.onHit( q, axe, h, foe, 10 );
					foe.sprite.showStatusWithIcon( CharSprite.NEGATIVE, "10", FloatingText.PHYS_DMG );
				} finally {
					Blow.end();
				}
			}, 15 );
			assertEquals( "the stacks held after hit " + i, Math.min( 3, i ), q.shredStacks );
			assertNotNull( card( foe.sprite ) );
			assertEquals( "SHRED on hit " + i + " only when it added a stack", stacked, holds( card( foe.sprite ), FloatingText.SHRED ) );
			//the same blow with no axe: what the bark adds is its own
			Game.timeTotal += 3f;
			int[] plain = rig.made( () -> {
				Blow.begin( h, foe );
				try {
					foe.sprite.showStatusWithIcon( CharSprite.NEGATIVE, "10", FloatingText.PHYS_DMG );
				} finally {
					Blow.end();
				}
			}, 15 );
			assertTrue( "bark flies on hit " + i, withBark[0] > plain[0] );
		}
	}

	@Test
	public void disarmedWearsItsOwnIcon(){
		assertEquals( 115, BuffIndicator.DISARMED );
		assertEquals( BuffIndicator.DISARMED, new Disarmed().icon() );
		try {
			Disarmed.class.getDeclaredMethod( "tintIcon", Image.class );
			fail( "no tint over its own icon" );
		} catch (NoSuchMethodException expected){
			//drawn as painted
		}
	}

	//the live card on a sprite, null for none
	private static HitCard card( CharSprite s ){
		EnchantState st = EnchantState.get( s );
		return st != null && st.card != null && st.card.alive && st.card.target() == s ? st.card : null;
	}

	private static boolean holds( HitCard c, int code ){
		for (int i = 0; i < c.chipCount(); i++) if (c.chip( i ) == code) return true;
		return false;
	}

	@Test
	public void theDisarmedBlowsCardShowsItsCause(){
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = at( 2, 2 );
		Char brute = new Char(){
			@Override public int attackSkill( Char target ){ return Char.INFINITE_ACCURACY; }
			@Override public int damageRoll(){ return 40; }
		};
		brute.pos = at( 5, 6 );
		brute.HP = brute.HT = 1000;
		brute.alignment = Char.Alignment.ENEMY;
		sprite( brute );
		Actor.add( brute );
		Char bag = dummy( at( 6, 6 ), Char.Alignment.ALLY, 1000000 );
		Buff.prolong( brute, Disarmed.class, Disarmed.DURATION );
		assertTrue( brute.attack( bag ) );
		rig.frames( 3, true );
		HitCard c = card( bag.sprite );
		assertNotNull( "the struck one's card", c );
		assertTrue( "the Disarmed chip beside the halved number", holds( c, -(BuffIndicator.DISARMED + 1) ) );
	}

	// ------------------------------------------------------------------ words to chips

	//no floating text alive on the scene's statuses
	private int floats(){
		int n = 0;
		for (Gizmo g : rig.layers[GameScene.Layer.STATUSES.ordinal()].membersView()){
			if (g instanceof FloatingText && g.alive) n++;
		}
		return n;
	}

	@Test
	public void aSiphonsManaIsTheWieldersSmallCard(){
		Hero h = hero( at( 5, 6 ) );
		h.MT = 100;
		h.MP = 0;
		MageBook book = new MageBook();
		Quality q = typed( book, ItemType.ALPHA );
		h.belongings.weapon = book;
		Char foe = dummy( at( 6, 6 ), Char.Alignment.ENEMY, 100000 );
		Blow.begin( h, foe );
		try {
			WeaponPerk.SIPHON.onHit( q, book, h, foe, 10 );
			foe.sprite.showStatusWithIcon( CharSprite.NEGATIVE, "10", FloatingText.PHYS_DMG );
		} finally {
			Blow.end();
		}
		assertEquals( 3, h.MP );
		//it waits for the mote to land (Blow.flowBack): none yet, then the wielder's small card
		rig.frames( 6, true );
		assertNull( "not before the mote lands", manaCard( h.sprite ) );
		rig.frames( 6, true );
		assertEquals( "no word floats", 0, floats() );
		HitCard mana = manaCard( h.sprite );
		assertNotNull( "the mana's small card on the wielder", mana );
		assertEquals( 3, mana.number() );
	}

	//the wielder's live mana card, null for none
	private static HitCard manaCard( CharSprite s ){
		for (HitCard g : EnchantState.of( s ).gains){
			if (g != null && g.alive && g.target() == s && g.leftIcon() == FloatingText.MANA) return g;
		}
		return null;
	}

	//a sprite that keeps every word it is asked to show
	private CharSprite listening( Char ch, List<String> said ){
		CharSprite s = new CharSprite(){
			@Override public Emitter emitter(){ return new Emitter(); }
			@Override public Emitter centerEmitter(){ return new Emitter(); }
			@Override public void operate( int cell ){}
			@Override public void showStatus( int color, String text, Object... args ){ said.add( text ); }
			@Override public void showStatusWithIcon( int color, String text, int icon, Object... args ){ said.add( text ); }
		};
		s.ch = ch;
		ch.sprite = s;
		rig.stage.add( s );
		s.place( ch.pos );
		s.visible = true;
		return s;
	}

	@Test
	public void noPerkOrStatueWordIsLeftButTheQuickReloads(){
		for (String k : new String[]{ "heave", "riposte" }){
			assertTrue( k, Messages.get( WeaponPerk.class, k ).contains( Messages.NO_TEXT_FOUND ) );
		}
		//a free reload is no blow's: its RELOAD icon on a card over the shooter, its word gone
		assertTrue( Messages.get( WeaponPerk.class, "reload" ).contains( Messages.NO_TEXT_FOUND ) );
		for (String k : new String[]{ "sunder", "wild", "ambush", "siphon", "riposte", "sweep" }){
			assertTrue( k, Messages.get( RivalStatue.class, k ).contains( Messages.NO_TEXT_FOUND ) );
		}
		for (Method m : RivalStatue.class.getDeclaredMethods()) assertFalse( "the statue says nothing", m.getName().equals( "say" ) );

		List<String> said = new ArrayList<>();
		Hero h = hero( at( 5, 6 ) );
		listening( h, said );
		Char foe = dummy( at( 6, 6 ), Char.Alignment.ENEMY, 100000 );
		listening( foe, said );

		//a heave: its chip on the struck one's card
		Javelin javelin = new Javelin();
		Quality qj = typed( javelin, ItemType.GAMMA );
		Buff.affect( h, WeaponPerk.Heave.class ).cell = foe.pos;
		Blow.begin( h, foe );
		try {
			WeaponPerk.IMPACT.onHit( qj, javelin, h, foe, 10 );
			assertTrue( Blow.open() );
		} finally {
			Blow.end();
		}
		//a riposte, turning a blow aside whole, the hero having stood still
		stoodStill();
		Katana katana = new Katana();
		Quality qk = typed( katana, ItemType.ALPHA );
		h.belongings.weapon = katana;
		assertEquals( 0f, WeaponPerk.GUARD.riposte( qk, katana, h, 10f, foe ), 0f );
		//free reloads
		FT ft = new FT();
		typed( ft, ItemType.ALPHA );
		Random.pushGenerator( 0x2E10L );
		try {
			int free = 0;
			for (int i = 0; i < 60; i++) if (Quality.rollFreeReload( ft, h )) free++;
			assertTrue( "some free reloads", free > 0 );
		} finally {
			Random.popGenerator();
		}
		//each free reload says so with its icon on a card over the shooter
		boolean reloadCard = false;
		for (com.watabou.noosa.Gizmo g : rig.layers[xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.Layer.STATUSES.ordinal()].membersView()){
			if (!(g instanceof xyz.gabriwar.warpedpixeldungeon.effects.HitCard) || !g.alive) continue;
			xyz.gabriwar.warpedpixeldungeon.effects.HitCard c = (xyz.gabriwar.warpedpixeldungeon.effects.HitCard) g;
			if (c.target() != h.sprite) continue;
			for (int i = 0; i < c.chipCount(); i++) reloadCard |= c.chip( i ) == FloatingText.RELOAD;
		}
		assertTrue( "each free reload says so", reloadCard );
		rig.frames( 30, true );
		//the riposte's answer is a number on the striker (Quality.reflect's), its PARRY an icon with no
		//text (no blow was open to take it); no word at all
		for (String w : said){
			assertTrue( "no word: " + said, w.isEmpty() || w.matches( "[+-]?\\d+" ) );
		}
	}

	@Test
	public void aRivalsAmbushIsAChipOnTheHerosCard() throws Exception {
		Hero h = hero( at( 5, 6 ) );
		RivalStatue rival = new RivalStatue();
		rival.pos = at( 6, 6 );
		rival.HP = rival.HT = 1000;
		sprite( rival );
		Field weapon = xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue.class.getDeclaredField( "weapon" );
		weapon.setAccessible( true );
		weapon.set( rival, new xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword() );
		Field perk = RivalStatue.class.getDeclaredField( "perk" );
		perk.setAccessible( true );
		perk.set( rival, WeaponPerk.KEEN );
		Actor.add( rival );
		//its opening blow, on a hero who can act: no reason icon says so, the chip does
		Blow.begin( rival, h );
		try {
			rival.attackProc( h, 10 );
		} finally {
			Blow.end();
		}
		rig.frames( 3, true );
		assertNotNull( "the hero's card", card( h.sprite ) );
		assertTrue( "the ambush on the hero's card", holds( card( h.sprite ), FloatingText.HIT_SUPR ) );
	}

	//the hero unmoved for ten turns (an alpha Guard's riposte asks for two)
	private static void stoodStill(){
		try {
			Field f = xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.class.getDeclaredField( "heroMovedAt" );
			f.setAccessible( true );
			f.setFloat( null, Actor.now() - 10f );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	@Test
	public void aHeavesAndAParrysWordsAreChips(){
		Hero h = hero( at( 5, 6 ) );
		Char foe = dummy( at( 6, 6 ), Char.Alignment.ENEMY, 100000 );
		Javelin javelin = new Javelin();
		Quality qj = typed( javelin, ItemType.GAMMA );
		Buff.affect( h, WeaponPerk.Heave.class ).cell = foe.pos;
		Blow.begin( h, foe );
		try {
			WeaponPerk.IMPACT.onHit( qj, javelin, h, foe, 10 );
			foe.sprite.showStatusWithIcon( CharSprite.NEGATIVE, "15", FloatingText.PHYS_DMG );
		} finally {
			Blow.end();
		}
		rig.frames( 3, true );
		assertNotNull( card( foe.sprite ) );
		assertTrue( "HEAVE on the struck one's card", holds( card( foe.sprite ), FloatingText.HEAVE ) );

		//the foe's blow on the hero, turned aside: PARRY on the hero's card
		stoodStill();
		Katana katana = new Katana();
		Quality qk = typed( katana, ItemType.ALPHA );
		h.belongings.weapon = katana;
		Game.timeTotal += 3f;
		Blow.begin( foe, h );
		try {
			WeaponPerk.GUARD.riposte( qk, katana, h, 10f, foe );
		} finally {
			Blow.end();
		}
		rig.frames( 3, true );
		assertNotNull( card( h.sprite ) );
		assertTrue( "PARRY on the hero's card", holds( card( h.sprite ), FloatingText.PARRY ) );
		assertEquals( "no word floats", 0, floats() );
	}

	@Test
	public void aParryOutsideAnyBlowRaisesItsChipAlone(){
		Hero h = hero( at( 5, 6 ) );
		//the hero's sprite keeps every text it is asked to show, with its icon
		final List<int[]> shown = new ArrayList<>();
		CharSprite hs = new CharSprite(){
			@Override public Emitter emitter(){ return new Emitter(); }
			@Override public Emitter centerEmitter(){ return new Emitter(); }
			@Override public void operate( int cell ){}
			@Override public void showStatusWithIcon( int color, String text, int icon, Object... args ){
				shown.add( new int[]{ icon, text.isEmpty() ? 0 : 1 } );
			}
		};
		hs.ch = h;
		h.sprite = hs;
		rig.stage.add( hs );
		hs.place( h.pos );
		hs.visible = true;
		Char foe = dummy( at( 6, 6 ), Char.Alignment.ENEMY, 100000 );
		stoodStill();
		Katana katana = new Katana();
		Quality qk = typed( katana, ItemType.ALPHA );
		h.belongings.weapon = katana;
		//a hit dealt outside any attack (a zap's), turned aside: no card to land on
		assertFalse( Blow.open() );
		assertEquals( 0f, WeaponPerk.GUARD.riposte( qk, katana, h, 10f, foe ), 0f );
		rig.frames( 3, true );
		assertEquals( "one text over the hero", 1, shown.size() );
		assertEquals( "the PARRY icon", FloatingText.PARRY, shown.get( 0 )[0] );
		assertEquals( "and no word with it", 0, shown.get( 0 )[1] );
		assertNull( "no card", card( hs ) );
	}

	@Test
	public void everyShotFromAfarAndEveryReachBlowWearsItsChip(){
		for (int dist = 3; dist <= 6; dist++){
			Game.timeTotal += 3f;
			CharSprite a = rig.rat( at( 2, 2 ) ), d = rig.rat( at( 2 + dist, 2 ) );
			Blow.begin( a.ch, d.ch );
			try {
				TypeFx.longshot( a.ch, d.ch, dist );
			} finally {
				Blow.end();
			}
			rig.frames( 3, true );
			assertNotNull( "a card at " + dist + " tiles", card( d ) );
			assertTrue( "LONGSHOT at " + dist + " tiles", holds( card( d ), FloatingText.LONGSHOT ) );
		}
		Game.timeTotal += 3f;
		CharSprite a = rig.rat( at( 3, 8 ) ), d = rig.rat( at( 5, 8 ) );
		Blow.begin( a.ch, d.ch );
		try {
			TypeFx.reach( a.ch, d.ch );
		} finally {
			Blow.end();
		}
		rig.frames( 3, true );
		assertNotNull( card( d ) );
		assertTrue( "REACH on the card", holds( card( d ), FloatingText.REACH ) );
		assertEquals( FloatingText.REACH, TypeFx.perkIcon( WeaponPerk.REACH ) );
	}

	// ------------------------------------------------------------------ co-op

	//the "eh" events of a drained batch, as (kind, numbers)
	private static List<int[]> events( JSONArray a ) throws Exception {
		List<int[]> out = new ArrayList<>();
		if (a == null) return out;
		for (int i = 0; i < a.length(); i++){
			JSONObject e = a.getJSONObject( i );
			if (!"eh".equals( e.getString( "t" ) )) continue;
			JSONArray n = e.getJSONArray( "i" );
			int[] row = new int[n.length() + 1];
			row[0] = e.getInt( "k" );
			for (int k = 0; k < n.length(); k++) row[k + 1] = n.getInt( k );
			out.add( row );
		}
		return out;
	}

	@Test
	public void heldNotesGoToTheGuestsAndShowThere() throws Exception {
		TestParty.join( new Hero() );
		Hero h = hero( at( 5, 6 ) );
		Flute flute = new Flute();
		Quality q = typed( flute, ItemType.GAMMA );
		h.belongings.weapon = flute;
		q.notes = 2;
		q.dreadNext = true;
		EnchantState.of( h.sprite );
		WieldLook.step( h.sprite );
		assertEquals( "two pips over the host's hand", 2, WieldLook.pipsShown( h.sprite ) );
		WieldLook.step( h.sprite );
		List<int[]> ev = new ArrayList<>();
		for (int[] e : events( NetVisuals.drainEvents() )) if (e[0] == EnchantNet.NOTES) ev.add( e );
		assertEquals( "told once", 1, ev.size() );
		assertArrayEquals( new int[]{ EnchantNet.NOTES, h.id(), 2, 1 }, ev.get( 0 ) );

		//the guest: his own hero's sprite for the host's
		TestParty.guest();
		Hero gh = new Hero();
		gh.pos = at( 3, 3 );
		CharSprite gs = sprite( gh );
		EnchantState.of( gs );
		Map<Integer, CharSprite> his = new HashMap<>();
		his.put( h.id(), gs );
		int[] e = ev.get( 0 );
		EnchantNet.play( e[0], Arrays.copyOfRange( e, 1, e.length ), null, his::get );
		WieldLook.step( gs );
		assertEquals( "the host's two pips", 2, WieldLook.pipsShown( gs ) );
		//a chord takes them home: gone once they have flown
		EnchantNet.play( EnchantNet.NOTES, new int[]{ h.id(), 0, 0 }, null, his::get );
		Game.timeTotal += 0.2f;
		WieldLook.step( gs );
		assertEquals( 0, WieldLook.pipsShown( gs ) );
		assertNull( "nothing sent on", NetVisuals.drainEvents() );
	}

	@Test
	public void everyPieceGoesOutAndPlaysOnTheGuest() throws Exception {
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), n1 = rig.rat( at( 6, 5 ) ), n2 = rig.rat( at( 7, 7 ) );
		Dungeon.hero = new Hero();
		TestParty.join( new Hero() );
		everyEntry( a, d, n1, n2 );
		rig.frames( 2, false );
		JSONArray sent = NetVisuals.drainEvents();
		List<int[]> ev = events( sent );
		int aid = a.ch.id(), did = d.ch.id(), n1id = n1.ch.id(), n2id = n2.ch.id();
		int[][] expected = {
				{ PieceFx.SPLINTER, aid, did },
				{ PieceFx.FELL, aid, did },
				{ PieceFx.DAZE, aid, did },
				//outside a blow the crack leads alone: its daze is not sent
				{ PieceFx.CRACK, aid, did, n1id, n2id },
				{ PieceFx.NOTE, aid, 0 },
				{ PieceFx.CHORD, aid, did, 0, TypeFx.FLUTE, -1, aid, n1id },
				{ PieceFx.CHORD, aid, did, 1, TypeFx.FLUTE, n1id, did, n2id },
				{ PieceFx.RESONANCE, aid, did, 0, n1id, n2id },
				{ PieceFx.LASH, aid, did },
				{ PieceFx.UNARM, aid, did },
				{ PieceFx.SIPHON, aid, did },
				{ PieceFx.PERK, TypeFx.SUNDER, aid, did, 1 },
				{ PieceFx.PERK, TypeFx.FLURRY, aid, did, 0 } };
		for (int i = 0; i < expected.length; i++){
			int[] row = ev.get( i );
			assertEquals( EnchantNet.PIECE, row[0] );
			assertEquals( "piece " + i, Arrays.toString( expected[i] ), Arrays.toString( Arrays.copyOfRange( row, 1, row.length ) ) );
		}
		assertEquals( "the momentum, bulwark, shots, reach and parry after", expected.length + 5 + 1 + 4 + 1 + 1, ev.size() );

		//the guest: his own characters standing for the host's
		TestParty.guest();
		rig.frames( 90, true );
		Game.timeTotal += 3f;
		CharSprite ga = rig.rat( at( 3, 10 ) ), gd = rig.rat( at( 4, 10 ) ), g1 = rig.rat( at( 4, 11 ) ), g2 = rig.rat( at( 5, 11 ) );
		Map<Integer, Mob> mobs = new HashMap<>();
		mobs.put( aid, (Mob) ga.ch );
		mobs.put( did, (Mob) gd.ch );
		mobs.put( n1id, (Mob) g1.ch );
		mobs.put( n2id, (Mob) g2.ch );
		NetVisuals.setSpectatorMobs( mobs );
		ear.heard.clear();
		final JSONArray batch = sent;
		int[] made = new int[1];
		assertEquals( "a guest's pieces draw nothing of the game's Random", 0, draws( () -> {
			made[0] = rig.made( () -> NetVisuals.replay( batch ), 90 )[0];
		} ) );
		assertTrue( "the guest drew them: " + made[0], made[0] > 30 );
		assertFalse( "and heard them", ear.heard.isEmpty() );
		assertNull( "nothing sent on", NetVisuals.drainEvents() );

		//short, out of the perks, or of a character the guest does not have: nothing
		Game.timeTotal += 3f;
		assertEquals( 0, rig.made( () -> {
			EnchantNet.play( EnchantNet.PIECE, new int[]{ PieceFx.FELL, aid }, null, id -> gd );
			EnchantNet.play( EnchantNet.PIECE, new int[]{ PieceFx.PERK, 99, aid, did, 1 }, null, id -> gd );
			EnchantNet.play( EnchantNet.PIECE, new int[]{ PieceFx.CHORD, aid, did, 0 }, null, id -> gd );
			EnchantNet.play( EnchantNet.PIECE, new int[]{ PieceFx.SPLINTER, aid, did }, null, id -> null );
		}, 30 )[0] );
	}

	@Test
	public void aTypesPieceInABlowIsHeldForItsCardOnTheGuest() throws Exception {
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Dungeon.hero = new Hero();
		TestParty.join( new Hero() );
		Blow.begin( a.ch, d.ch );
		try {
			TypeFx.fell( a.ch, d.ch );
			TypeFx.splinter( a.ch, d.ch, true );
			d.showStatusWithIcon( CharSprite.NEGATIVE, "9", FloatingText.PHYS_DMG );
		} finally {
			Blow.end();
		}
		rig.frames( 2, false );
		JSONArray sent = NetVisuals.drainEvents();
		int pieces = 0, cards = 0;
		for (int i = 0; i < sent.length(); i++){
			JSONObject e = sent.getJSONObject( i );
			if (!"eh".equals( e.optString( "t" ) )) continue;
			if (e.getInt( "k" ) == EnchantNet.PIECE){
				assertTrue( "tagged with its blow", e.optInt( "s", 0 ) != 0 );
				pieces++;
			}
			if (e.getInt( "k" ) == EnchantNet.CARD) cards++;
		}
		assertEquals( 2, pieces );
		assertEquals( 1, cards );

		TestParty.guest();
		rig.frames( 90, true );
		Game.timeTotal += 3f;
		CharSprite ga = rig.rat( at( 3, 10 ) ), gd = rig.rat( at( 4, 10 ) );
		Map<Integer, Mob> mobs = new HashMap<>();
		mobs.put( a.ch.id(), (Mob) ga.ch );
		mobs.put( d.ch.id(), (Mob) gd.ch );
		NetVisuals.setSpectatorMobs( mobs );
		NetVisuals.replay( sent );
		rig.frames( 3, true );
		HitCard c = card( gd );
		assertNotNull( "the guest's card", c );
		assertTrue( "FELL from the host's lead", holds( c, FloatingText.FELL ) );
		assertTrue( "SHRED from the host's accent", holds( c, FloatingText.SHRED ) );
		assertEquals( "nothing held", 0, EnchantNet.heldBlows() );
	}

	// ------------------------------------------------------------------ previews (EnchantPreviewDump)

	/** The type looks' moments for the preview dump (EnchantPreviewDump's registry): each a blow of
	 *  the scene's attacker on its defender through the ledger, the throws and the drag as Pushing
	 *  plays them, the Bard's held notes restaged (WieldLook.stage) and stepped each frame as the
	 *  game's afterDraw steps them. */
	static final class Previews {

		private Previews(){}

		//a plain blow of a on d through the ledger, its pieces first
		static void blow( EnchantPreviewDump.Scene s, CharSprite a, CharSprite d, int n, Runnable pieces ){
			Blow.begin( a.ch, d.ch );
			try {
				if (pieces != null) pieces.run();
				if (n > 0){
					d.showStatusWithIcon( CharSprite.NEGATIVE, Integer.toString( n ), FloatingText.PHYS_DMG );
					d.bloodBurstA( a.center(), n );
					d.flash();
				}
				d.hitFx( a.x + 8, a.y + 8, n, xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx.SLASH, false );
			} finally {
				Blow.end();
			}
		}

		static void blow( EnchantPreviewDump.Scene s, int n, Runnable pieces ){
			blow( s, s.a, s.d, n, pieces );
		}

		//a rat thrown (or pulled) from where it stands to a cell, Pushing's own look
		static void push( CharSprite who, int to, int tint ){
			Pushing p = new Pushing( who.ch, who.ch.pos, to, () -> who.ch.pos = to, tint );
			p.new Effect();
		}

		//a wall on a cell of the stage's fresh room
		static void wall( int cell ){
			Dungeon.level.map[cell] = xyz.gabriwar.warpedpixeldungeon.levels.Terrain.WALL;
			Dungeon.level.buildFlagMaps();
		}

		//the Bard's held notes over a's hand, stepped each frame as the game's afterDraw does
		static void stepping( EnchantPreviewDump.Scene s ){
			final CharSprite a = s.a;
			s.rig.stage.add( new Gizmo(){
				@Override
				public void update(){
					WieldLook.step( a );
				}
			} );
		}

		static void splinter( EnchantPreviewDump.Scene s ){
			blow( s, 6, () -> TypeFx.splinter( s.a.ch, s.d.ch, true ) );
		}

		static void fell( EnchantPreviewDump.Scene s, boolean into, boolean after ){
			int open = s.d.ch.pos + 2, stop = s.d.ch.pos + 1;
			if (into) wall( open );
			blow( s, 9, after ? () -> {
				TypeFx.splinter( s.a.ch, s.d.ch, true );
				TypeFx.fell( s.a.ch, s.d.ch );
			} : null );
			push( s.d, into ? stop : open, after ? TypeFx.WOOD : Pushing.PLAIN );
		}

		static void daze( EnchantPreviewDump.Scene s ){
			blow( s, 5, () -> {
				Blow.announce( s.d.ch, new xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze() );
				TypeFx.daze( s.a.ch, s.d.ch );
			} );
		}

		static void crack( EnchantPreviewDump.Scene s ){
			final List<Char> around = list( s.rig.rat( s.d.ch.pos - EnchantFxTest.W ).ch, s.rig.rat( s.d.ch.pos + EnchantFxTest.W ).ch,
					s.rig.rat( s.d.ch.pos + 1 ).ch );
			blow( s, 5, () -> {
				Blow.announce( s.d.ch, new xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze() );
				for (Char c : around) Blow.announce( c, new xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple() );
				TypeFx.crack( s.a.ch, s.d.ch, around );
			} );
		}

		//three notes 0.35 s apart, a pip held each
		static void notes( EnchantPreviewDump.Scene s, boolean after ){
			if (after) stepping( s );
			for (int k = 1; k <= 3; k++){
				final int held = k;
				EnchantFx.later( 0.35f * (k - 1), () -> {
					blow( s, 3, after ? () -> TypeFx.note( s.a.ch, false ) : null );
					if (after) WieldLook.stage( s.a, held, false, 3f );
				} );
			}
		}

		//three notes held, then the fourth plays the chord (a rally, or a dread with a charm)
		static void chord( EnchantPreviewDump.Scene s, boolean dread, boolean charm, boolean after ){
			final CharSprite friend = s.rig.rat( s.a.ch.pos - EnchantFxTest.W ), other = s.rig.rat( s.d.ch.pos + EnchantFxTest.W ),
					weak = s.rig.rat( s.d.ch.pos - EnchantFxTest.W );
			if (!after){
				blow( s, 3, null );
				return;
			}
			stepping( s );
			WieldLook.stage( s.a, 3, dread, 3f );
			EnchantFx.later( 0.2f, () -> {
				WieldLook.stage( s.a, 0, dread, 0f );
				blow( s, 3, () -> {
					TypeFx.note( s.a.ch, dread );
					TypeFx.chord( s.a.ch, s.d.ch, new Flute(), dread, dread ? null : list( s.a.ch, friend.ch ),
							dread ? list( s.d.ch, other.ch ) : null, charm ? weak.ch : null );
				} );
			} );
		}

		static void lashPull( EnchantPreviewDump.Scene s, boolean after ){
			//the whip two tiles off
			int from = s.d.ch.pos - 2;
			s.a.ch.pos = from;
			s.a.place( from );
			blow( s, 4, after ? () -> {
				Blow.announce( s.d.ch, new xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple() );
				TypeFx.lash( s.a.ch, s.d.ch );
			} : null );
			push( s.d, s.d.ch.pos - 1, Pushing.PLAIN );
		}

		static void lashUnarm( EnchantPreviewDump.Scene s, boolean after ){
			blow( s, 4, after ? () -> {
				Blow.announce( s.d.ch, new Disarmed() );
				TypeFx.unarm( s.a.ch, s.d.ch );
			} : null );
			//its next blow lands halved: the cause beside it on the card
			EnchantFx.later( 0.5f, () -> blow( s, s.d, s.a, 2,
					after ? () -> Blow.chip( s.a.ch, -(BuffIndicator.DISARMED + 1), 0 ) : null ) );
		}

		//a sunder, a free flurry, two stacks of momentum, a full one, a Bulwark blow, a shot from five tiles
		static void perks( EnchantPreviewDump.Scene s, boolean after ){
			Runnable[] r = {
					() -> TypeFx.sunder( s.a.ch, s.d.ch, true ),
					() -> TypeFx.flurry( s.a.ch, s.d.ch ),
					() -> TypeFx.momentum( s.a.ch, s.d.ch, 2 ),
					() -> TypeFx.momentum( s.a.ch, s.d.ch, 4 ),
					() -> TypeFx.bulwark( s.a.ch, s.d.ch ),
					() -> TypeFx.longshot( s.a.ch, s.d.ch, 5 ) };
			for (int i = 0; i < r.length; i++){
				final Runnable pieces = r[i];
				EnchantFx.later( 0.2f * i, () -> blow( s, 4, after ? pieces : null ) );
			}
		}

		//a Reach blow, a shot from three tiles, and a riposte turning the defender's blow aside
		static void morePerks( EnchantPreviewDump.Scene s, boolean after ){
			blow( s, 5, after ? () -> TypeFx.reach( s.a.ch, s.d.ch ) : null );
			EnchantFx.later( 0.3f, () -> blow( s, 5, after ? () -> TypeFx.longshot( s.a.ch, s.d.ch, 3 ) : null ) );
			EnchantFx.later( 0.65f, () -> blow( s, s.d, s.a, 0, () -> {
				if (after) TypeFx.parry( s.d.ch, s.a.ch, 8 );
				s.d.showStatusWithIcon( CharSprite.NEGATIVE, "2", FloatingText.PHYS_DMG );
			} ) );
		}

		static void siphon( EnchantPreviewDump.Scene s, boolean after ){
			blow( s, 6, after ? () -> {
				TypeFx.siphon( s.a.ch, s.d.ch );
				s.a.showStatusWithIcon( TypeFx.MANA, "+3", FloatingText.MANA );
			} : null );
		}
	}
}
