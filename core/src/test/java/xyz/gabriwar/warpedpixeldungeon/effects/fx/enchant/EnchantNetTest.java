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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.BlowPresenter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.StateLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HealFx;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.AresLeech;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Surging;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.watabou.noosa.Gizmo;
import com.watabou.utils.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The enchantments' pictures for co-op guests (EnchantNet, NetVisuals' "eh"): the kinds are the
 * numbers pinned here; a host's proc, glyph answer (and quiet moment) and blow ending go out with
 * their numbers as sent, and a guest plays each from his own sprites (the stamp, the arc, the star,
 * the sting) and sends nothing on; short, unknown or newer payloads play nothing; a guest's heal
 * card waits for the beads his own replay of the blow draws.
 */
public class EnchantNetTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private final List<String> heard = new ArrayList<>();

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		heard.clear();
		Sting.out = ( sample, at, volume, pitch ) -> heard.add( sample );
		NetVisuals.clear();
	}

	@After
	public void tearDown() throws Exception {
		TestParty.leave();
		NetVisuals.setSpectatorMobs( null );
		NetVisuals.clear();
		rig.down();
	}

	@Test
	public void theKindsArePinned(){
		assertEquals( 0, EnchantNet.PROC );
		assertEquals( 1, EnchantNet.GLYPH );
		assertEquals( 2, EnchantNet.GAIN );
		assertEquals( 3, EnchantNet.WIELD );
		assertEquals( 4, EnchantNet.PIECE );
		assertEquals( 5, EnchantNet.REVEAL );
		assertEquals( 6, EnchantNet.BLOW_END );
		assertEquals( 7, EnchantNet.SWING );
		assertEquals( 8, EnchantNet.SURGE );
	}

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

	private int flashes(){
		int n = 0;
		for (Gizmo g : rig.layers[GameScene.Layer.LIGHTS.ordinal()].membersView()){
			if (g instanceof EnchantFx.Flash && g.alive) n++;
		}
		return n;
	}

	@Test
	public void aProcAGlyphAndABlowsEndGoOutAndPlayOnTheGuest() throws Exception {
		CharSprite a = rig.rat( EnchantFxTest.at( 5, 6 ) ), d = rig.rat( EnchantFxTest.at( 6, 6 ) );
		EnchantLook blazing = EnchantLooks.of( Blazing.class ), thorns = EnchantLooks.of( Thorns.class );
		TestParty.join( new Hero() );

		//the host: each played here and sent
		EnchantFx.proc( blazing, a.ch, d.ch, 0.45f, 2, EnchantFx.multi( 1 ) );
		EnchantFx.ward( thorns, d.ch, a.ch, 0.7f, 1, 0 );
		EnchantFx.passive( thorns, d.ch, EnchantFx.STEP, 0.2f );
		EnchantFx.blowEnded( a.ch, d.ch );
		rig.frames( 2, false );
		JSONArray sent = NetVisuals.drainEvents();
		List<int[]> ev = events( sent );
		assertEquals( 4, ev.size() );
		int aid = a.ch.id(), did = d.ch.id();
		assertArrayEquals( new int[]{ EnchantNet.PROC, blazing.code, aid, did, 450, 2, EnchantFx.multi( 1 ), EnchantFx.MID }, ev.get( 0 ) );
		assertArrayEquals( new int[]{ EnchantNet.GLYPH, thorns.code, did, aid, 700, 1, 0, -1, EnchantFx.STRONG }, ev.get( 1 ) );
		assertArrayEquals( new int[]{ EnchantNet.GLYPH, thorns.code, did, -1, 200, 0, 0, EnchantFx.STEP, EnchantFx.WEAK }, ev.get( 2 ) );
		assertArrayEquals( new int[]{ EnchantNet.BLOW_END, aid, did }, ev.get( 3 ) );

		//the guest: his own characters standing for the host's, a moment later
		TestParty.guest();
		rig.frames( 90, true );
		heard.clear();
		CharSprite ga = rig.rat( EnchantFxTest.at( 3, 3 ) ), gd = rig.rat( EnchantFxTest.at( 4, 3 ) );
		Map<Integer, Mob> mobs = new HashMap<>();
		mobs.put( aid, (Mob) ga.ch );
		mobs.put( did, (Mob) gd.ch );
		NetVisuals.setSpectatorMobs( mobs );
		int arcs = flashes();
		Random.pushGenerator( 99 );
		float next;
		try {
			NetVisuals.replay( sent );
			rig.frames( 3, false );
			next = Random.Float();
		} finally {
			Random.popGenerator();
		}
		Random.pushGenerator( 99 );
		float fresh = Random.Float();
		Random.popGenerator();
		assertEquals( "a guest's pictures draw nothing of the game's Random", fresh, next, 0f );
		assertSame( "the proc stamped the guest's own sprite", blazing, gd.blowLook );
		assertTrue( "the ward arc and the bubble", flashes() > arcs );
		//one sting for the guest's defender's blow, the strongest (the thorns' answer outranks the
		//middling proc), played on the guest as sounds have no other way over
		assertEquals( "one sting: " + heard, 1, heard.size() );
		assertTrue( "the strongest's: " + heard, heard.contains( thorns.sound ) );
		assertNull( "nothing sent on", NetVisuals.drainEvents() );
	}

	//a sprite's looks (its surge's stacks as it knows them)
	private static StateLooks looks( CharSprite s ) throws Exception {
		Field f = CharSprite.class.getDeclaredField( "looks" );
		f.setAccessible( true );
		return (StateLooks) f.get( s );
	}

	@Test
	public void aSwingAndASurgesStacksGoOutAndPlayOnTheGuest() throws Exception {
		TestParty.join( new Hero() );
		//the host: his hero swings an enchanted sword (his sprite off the stage: only where it
		//stands is read), and a surge on a rat grows to three stacks
		Hero h = new Hero();
		Dungeon.hero = h;
		Sword sword = new Sword();
		sword.enchant( new Blazing() );
		h.belongings.weapon = sword;
		h.pos = EnchantFxTest.at( 5, 6 );
		CharSprite hs = new RatSprite();
		hs.ch = h;
		h.sprite = hs;
		hs.place( h.pos );
		hs.visible = true;
		CharSprite d = rig.rat( EnchantFxTest.at( 6, 6 ) ), r = rig.rat( EnchantFxTest.at( 3, 3 ) );
		EnchantLook blazing = EnchantLooks.of( Blazing.class );
		EnchantFx.swing( h, d.ch, true );
		Buff.affect( r.ch, Surging.SurgeTracker.class, 5f ).stacks = 3;
		r.add( CharSprite.State.SURGING );
		rig.frames( 20, true );
		List<int[]> ev = events( NetVisuals.drainEvents() );
		int hid = h.id(), did = d.ch.id(), rid = r.ch.id();
		assertTrue( "the swing went out: " + ev.size(), ev.stream().anyMatch( e -> java.util.Arrays.equals( e,
				new int[]{ EnchantNet.SWING, blazing.code, hid, did, 1, 1, 0, 0, 1 } ) ) );
		assertTrue( "the surge's stacks went out", ev.stream().anyMatch( e -> java.util.Arrays.equals( e,
				new int[]{ EnchantNet.SURGE, rid, 3 } ) ) );
		Dungeon.hero = null;

		//the guest: his own sprites for the host's characters
		TestParty.guest();
		CharSprite ga = rig.rat( EnchantFxTest.at( 3, 6 ) ), gd = rig.rat( EnchantFxTest.at( 4, 6 ) ), gr = rig.rat( EnchantFxTest.at( 8, 3 ) );
		Map<Integer, CharSprite> his = new HashMap<>();
		his.put( hid, ga );
		his.put( did, gd );
		his.put( rid, gr );
		gr.add( CharSprite.State.SURGING );
		rig.frames( 2, true );
		assertEquals( "a guest's surge knows one stack of itself", 1, looks( gr ).surge() );
		for (int[] e : ev) EnchantNet.play( e[0], java.util.Arrays.copyOfRange( e, 1, e.length ), null, his::get );
		rig.frames( 1, false );
		assertSame( "the swing's tint left on the guest's defender", blazing, EnchantState.get( gd ).swingLook );
		rig.frames( 20, true );
		assertEquals( "the host's three stacks", 3, looks( gr ).surge() );
		assertNull( "nothing sent on", NetVisuals.drainEvents() );
	}

	@Test
	public void shortUnknownAndNewerPayloadsPlayNothing(){
		CharSprite d = rig.rat( EnchantFxTest.at( 6, 6 ) );
		EnchantNet.Sprites one = id -> d;
		EnchantNet.play( EnchantNet.PROC, new int[]{ 0, 1 }, null, one );
		EnchantNet.play( EnchantNet.PROC, new int[]{ 999, 1, 2, 500, 0, 0, 1 }, null, one );
		EnchantNet.play( 99, new int[]{ 0, 1, 2, 500, 0, 0, 1 }, null, one );
		EnchantNet.play( EnchantNet.GLYPH, null, null, one );
		EnchantNet.play( EnchantNet.BLOW_END, new int[]{}, null, one );
		EnchantNet.play( EnchantNet.SWING, new int[]{ 0, 1, 2, 1, 1, 0, 0 }, null, one );
		EnchantNet.play( EnchantNet.SURGE, new int[]{ 1 }, null, one );
		rig.frames( 2, false );
		assertNull( EnchantState.get( d ) == null ? null : EnchantState.get( d ).swingLook );
		assertNull( d.blowLook );
		//a character the guest does not have: nothing either
		EnchantNet.play( EnchantNet.PROC, new int[]{ 0, 1, 2, 500, 0, 0, 1 }, null, id -> null );
		rig.frames( 2, false );
		assertNull( d.blowLook );
		//a whole one, the defender his: played
		EnchantNet.play( EnchantNet.PROC, new int[]{ 0, -1, 2, 500, 0, 0, 1 }, null, one );
		rig.frames( 1, false );
		assertSame( EnchantLooks.byCode( 0 ), d.blowLook );
	}

	@Test
	public void aGuestsBlowDrawsAProcOnItsAttackerAndHearsALeadWithItsOwnSound() throws Exception {
		com.watabou.noosa.Game.elapsed = 1 / 60f;
		CharSprite a = rig.rat( EnchantFxTest.at( 5, 6 ) ), d = rig.rat( EnchantFxTest.at( 6, 6 ) );
		EnchantLook own = null;
		for (EnchantLook l : EnchantLooks.all()){
			if (l.ownSound && l.sound != null && !l.glyph && !l.curse){
				own = l;
				break;
			}
		}
		assertNotNull( "a look whose sound its own code plays", own );
		EnchantLook blazing = EnchantLooks.of( Blazing.class );
		TestParty.join( new Hero() );
		NetVisuals.clear();

		//the host: that look's proc leads; a proc on the attacker (as Thorns plays its) comes with it
		xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.begin( a.ch, d.ch );
		EnchantFx.proc( own, a.ch, d.ch, EnchantFx.strengthOf( EnchantFx.STRONG ), 0, 0 );
		EnchantFx.proc( blazing, d.ch, a.ch, EnchantFx.strengthOf( EnchantFx.MID ), 0, 0 );
		d.showStatusWithIcon( CharSprite.NEGATIVE, "9", xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.PHYS_DMG );
		d.bloodBurstA( a.center(), 9 );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.end();
		rig.frames( 2, false );
		assertSame( "the host drew the proc on the attacker", blazing, a.blowLook );
		assertFalse( "the host's own code plays that sound, not the sting", heard.contains( own.sound ) );
		JSONArray sent = NetVisuals.drainEvents();

		//the guest
		TestParty.guest();
		rig.frames( 90, true );
		heard.clear();
		CharSprite ga = rig.rat( EnchantFxTest.at( 3, 3 ) ), gd = rig.rat( EnchantFxTest.at( 4, 3 ) );
		Map<Integer, Mob> mobs = new HashMap<>();
		mobs.put( a.ch.id(), (Mob) ga.ch );
		mobs.put( d.ch.id(), (Mob) gd.ch );
		NetVisuals.setSpectatorMobs( mobs );
		NetVisuals.replay( sent );
		assertSame( "the proc on the attacker drawn on the guest's attacker", blazing, ga.blowLook );
		assertSame( "the lead on the guest's defender", own, gd.blowLook );
		rig.frames( 3, false );
		assertTrue( "the lead's own sound heard on the guest: " + heard, heard.contains( own.sound ) );
	}

	//a host's blow of a rat on another, healing it by 5, its procs as given (on the attacker, the
	//struck one and a third rat), replayed on a guest: when the guest's heal shows, s after the
	//replay (-1 none within 1.5 s)
	private float guestsHeal( Procs procs ) throws Exception {
		com.watabou.noosa.Game.elapsed = 1 / 60f;
		CharSprite a = rig.rat( EnchantFxTest.at( 5, 6 ) ), d = rig.rat( EnchantFxTest.at( 6, 6 ) ),
				o = rig.rat( EnchantFxTest.at( 3, 6 ) );
		TestParty.join( new Hero() );
		NetVisuals.clear();
		xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.begin( a.ch, d.ch );
		procs.run( a, d, o );
		a.showStatusWithIcon( CharSprite.POSITIVE, "5", xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.HEALING );
		d.showStatusWithIcon( CharSprite.NEGATIVE, "9", xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.PHYS_DMG );
		d.bloodBurstA( a.center(), 9 );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.end();
		rig.frames( 2, false );
		JSONArray sent = NetVisuals.drainEvents();

		TestParty.guest();
		rig.frames( 90, true );
		CharSprite ga = rig.rat( EnchantFxTest.at( 3, 3 ) ), gd = rig.rat( EnchantFxTest.at( 4, 3 ) ),
				go = rig.rat( EnchantFxTest.at( 1, 3 ) );
		Map<Integer, Mob> mobs = new HashMap<>();
		mobs.put( a.ch.id(), (Mob) ga.ch );
		mobs.put( d.ch.id(), (Mob) gd.ch );
		mobs.put( o.ch.id(), (Mob) go.ch );
		NetVisuals.setSpectatorMobs( mobs );
		HealFx.reset();
		float t0 = com.watabou.noosa.Game.timeTotal;
		NetVisuals.replay( sent );
		for (int f = 0; f < 90 && HealFx.heals() == 0; f++) rig.frames( 1, true );
		return HealFx.heals() > 0 ? com.watabou.noosa.Game.timeTotal - t0 : -1f;
	}

	private interface Procs {
		void run( CharSprite a, CharSprite d, CharSprite other );
	}

	@Test
	public void aGuestsHealWaitsForHisOwnAccentsBeads() throws Exception {
		//a strong Vampiric second to a Blazing lead draws its beads on the guest's own beat: the heal
		//waits for them
		float r = guestsHeal( ( a, d, o ) -> {
			EnchantFx.proc( EnchantLooks.of( Blazing.class ), a.ch, d.ch, EnchantFx.strengthOf( EnchantFx.MID ), 0, 0 );
			EnchantFx.proc( EnchantLooks.of( Vampiric.class ), a.ch, d.ch, EnchantFx.strengthOf( EnchantFx.STRONG ), 0, 0,
					ShadowRows.VAMPIRIC );
		} );
		assertTrue( "after the second's beat and its line: " + r, r > BlowPresenter.BEAT + DrainFx.LINE_FOR - 0.02f );
	}

	@Test
	public void aGuestsHealWaitsForADrainOnAnotherFoe() throws Exception {
		//Ares' leech on a foe other than the struck one: its line is drawn on its own, and the guest's
		//heal waits for its beads as the host's does
		float r = guestsHeal( ( a, d, o ) -> ShadowRows.aresLeech( EnchantLooks.of( AresLeech.class ), a.ch,
				java.util.Collections.singletonList( o.ch ), EnchantFx.strengthOf( EnchantFx.MID ), 0 ) );
		assertTrue( "not at once: " + r, r > DrainFx.LINE_FOR - 0.02f );
	}

	@Test
	public void aHeldBlowWhoseTimerWentWithItsSceneIsDroppedAndItsSerialStartsAfresh(){
		com.watabou.noosa.Game.elapsed = 1 / 60f;
		CharSprite d = rig.rat( EnchantFxTest.at( 6, 6 ) );
		EnchantNet.Sprites one = id -> d;
		int[] proc = { 0, -1, 2, 500, 0, 0, 1 };
		EnchantNet.play( EnchantNet.PROC, proc, null, one, 777001 );
		assertNull( "held for its card", d.blowLook );
		//no frame runs its timer (it went with the scene): past STALE s the next picture drops it
		com.watabou.noosa.Game.timeTotal += EnchantNet.STALE + 0.1f;
		EnchantNet.play( EnchantNet.PROC, proc, null, one, 777002 );
		assertEquals( "the stale one dropped, the new one held", 1, EnchantNet.heldBlows() );
		//the dropped serial, handed out again: held afresh, with a timer of its own
		EnchantNet.play( EnchantNet.PROC, proc, null, one, 777001 );
		assertEquals( 2, EnchantNet.heldBlows() );
		rig.frames( Math.round( EnchantNet.HOLD_FOR * 60 ) + 2, true );
		assertEquals( "both played as they are, no card having come", 0, EnchantNet.heldBlows() );
		assertSame( EnchantLooks.byCode( 0 ), d.blowLook );
	}
}
