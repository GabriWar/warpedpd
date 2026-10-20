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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpringSoak;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.modules.EnchantArea;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.SlayerFamily;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * The rarity lines' looks (LineLooks, ItemAura's hints): a line that lifts a blow puts its gold
 * chip on the card and draws nothing (Slayer its family's emblem, Moonlit only while it adds); a line
 * acting on the hero shows only while it acts - its starting moment of three quads at most and once
 * per 30 s, its one-quad idles no closer than their interval, nothing at MINIMAL; Hushed's ring at
 * most once per 1.5 s and only near a foe that has not noticed him; Fleet's puff every third step of
 * a run on dry floor, at FULL only; Surefoot's landing; Hunter's glint only with the meat; the item
 * hints, one an item in their order, only equipped, quickslotted or in the info window, never beside
 * a look of magic, served last; and none of it drawing the game's Random.
 */
public class LineLooksTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private final EnchantArea area = new EnchantArea();
	private int savedTier;
	private DayNightCycle.Phase savedPhase;
	private Hero hero;
	private CharSprite sprite;

	@Before
	public void setUp() throws Exception {
		rig = new EnchantFxTest.Rig();
		rig.up();
		LineLooks.reset();
		WieldLook.reset();
		HitCard.reset();
		ItemAura.resetPool();
		savedTier = FxBudget.tier();
		savedPhase = DayNightCycle.debugPhaseOverride;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		hero = new Hero();
		hero.pos = at( 3, 2 );
		hero.HP = hero.HT = 20;
		hero.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill( hero.fieldOfView, true );
		Dungeon.hero = hero;
		sprite = new CharSprite(){
			@Override public Emitter emitter(){ return new Emitter(); }
			@Override public Emitter centerEmitter(){ return new Emitter(); }
		};
		sprite.ch = hero;
		hero.sprite = sprite;
		rig.stage.add( sprite );
		sprite.place( hero.pos );
		sprite.visible = true;
	}

	@After
	public void tearDown() throws Exception {
		tier( savedTier );
		DayNightCycle.debugPhaseOverride = savedPhase;
		ClimateManager.debugPrecipOverride = Float.NaN;
		ClimateManager.debugPrecipTypeOverride = null;
		ClimateManager.debugTempOverride = Float.NaN;
		Dungeon.quickslot.reset();
		LineLooks.reset();
		WieldLook.reset();
		HitCard.reset();
		ItemAura.resetPool();
		rig.down();
	}

	private static void tier( int t ) throws Exception {
		Field f = FxBudget.class.getDeclaredField( "tier" );
		f.setAccessible( true );
		f.set( null, t );
	}

	//an item with exactly these lines at a full roll, identified (the gallery's own maker)
	static <T extends Item> T lined( T item, RarityLine... lines ){
		return LineExhibits.lined( item, lines );
	}

	//n frames at 60 fps: the hero's sprite drawn (the area's afterDraw), then the stage
	private void frames( int n ){
		for (int i = 0; i < n; i++){
			Game.timeTotal += 1 / 60f;
			area.afterDraw( sprite );
			rig.frames( 1, false );
		}
	}

	//the quads made by a run and the frames after it
	private int made( Runnable r, int after ){
		return rig.made( r, after )[0];
	}

	// ------------------------------------------------------------------ the previews' moments

	/** The lines' looks as the preview dump plays them (EnchantPreviewDump's registry). */
	static final class Previews {

		private Previews(){}

		/** A blow of the scene's attacker on its defender through the ledger, these lines' chips on
		 *  its card. */
		static void chips( EnchantPreviewDump.Scene s, int number, int... chips ){
			EnchantPreviewDump.inBlow( s, true, () -> {
				for (int c : chips) Blow.chip( s.d.ch, c, 0 );
				EnchantPreviewDump.number( s, number, FloatingText.PHYS_DMG );
				s.d.bloodBurstA( s.a.center(), number );
				s.d.flash();
				s.d.hitFx( s.a.x + 8, s.a.y + 8, number, xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx.SLASH, false );
			} );
		}

		/** A body line's look on the defender: warm grip's and cooling's moment and an idle 0.6 s on,
		 *  windbreak two wisps, the others one look. */
		static void ambient( EnchantPreviewDump.Scene s, int look ){
			s.d.flipHorizontal = false;
			boolean edge = look == LineLooks.WARM_GRIP || look == LineLooks.COOLING;
			LineLooks.stage( s.d, look, edge );
			if (edge || look == LineLooks.WINDBREAK) EnchantFx.later( 0.6f, () -> LineLooks.stage( s.d, look, false ) );
		}

		/** Extra yields round the defender: a log, a stone, an ore, then a hunter's meat's glint. */
		static void extra( EnchantPreviewDump.Scene s ){
			s.a.visible = false;
			int d = s.d.ch.pos;
			PieceFx.runExtra( d - EnchantFxTest.W, new xyz.gabriwar.warpedpixeldungeon.items.materials.Log().image(), 1.3f, true, false );
			EnchantFx.later( 0.2f, () -> PieceFx.runExtra( d - EnchantFxTest.W - 1,
					new xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone().image(), 1.2f, true, false ) );
			EnchantFx.later( 0.4f, () -> PieceFx.runExtra( d - EnchantFxTest.W + 1,
					new xyz.gabriwar.warpedpixeldungeon.items.ore.IronOre().image(), 1.2f, true, false ) );
			EnchantFx.later( 0.6f, () -> PieceFx.runExtra( d + EnchantFxTest.W, -1, 1f, true, false ) );
		}

		/**
		 * The five hints: a row of equipped and quickslotted slots, and under it the same items as
		 * the info window draws them (its quicker pace), each slot's clock set so its hint starts as
		 * the moment does; held at night for moonlit's, the hero and the night let go of as the
		 * moment ends.
		 */
		static void hints( EnchantPreviewDump.Scene s ){
			s.a.visible = false;
			s.d.visible = false;
			final DayNightCycle.Phase phase = DayNightCycle.debugPhaseOverride;
			final Hero saved = Dungeon.hero;
			DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
			Hero h = new Hero();
			Dungeon.hero = h;
			Item[] items = { lined( new Sword(), RarityLine.WARM_GRIP ), lined( new ClothArmor(), RarityLine.COOLING ),
					lined( new ClothArmor(), RarityLine.RAINPROOF ), lined( new Sword(), RarityLine.MOONLIT ),
					lined( new ClothArmor(), RarityLine.FLEET ) };
			h.belongings.weapon = (Weapon) items[0];
			h.belongings.armor = (ClothArmor) items[1];
			for (int i = 2; i < items.length; i++) Dungeon.quickslot.setSlot( i - 2, items[i] );
			float vx = (s.d.ch.pos % EnchantFxTest.W - 2) * 16f, vy = EnchantPreviewDump.VIEW_Y * 16f;
			final float start = Game.timeTotal;
			for (int i = 0; i < items.length; i++){
				slot( s, items[i], ItemSprite.Mode.SLOT, vx + 1f + 16f * i, vy + 10f, start );
				slot( s, items[i], ItemSprite.Mode.PANEL, vx + 1f + 16f * i, vy + 42f, start );
			}
			//let go of the night, the hero and the quickslots after the moment's last frame
			s.rig.stage.add( new com.watabou.noosa.Gizmo(){
				int frames;

				@Override
				public void update(){
					if (++frames < Math.round( 1.25f * 30f )) return;
					DayNightCycle.debugPhaseOverride = phase;
					Dungeon.hero = saved;
					Dungeon.quickslot.reset();
					killAndErase();
				}
			} );
		}

		//an item's sprite at (x, y), its own clock started at `start`, its hint's quad on the stage
		private static void slot( EnchantPreviewDump.Scene s, Item item, ItemSprite.Mode mode, float x, float y, final float start ){
			ItemSprite sp = new ItemSprite(){
				@Override
				public float lookTime(){
					return Game.timeTotal - start;
				}
			}.mode( mode );
			sp.view( item );
			sp.auraGate( true, true );
			sp.x = x;
			sp.y = y;
			s.rig.stage.add( sp );
			sp.update();
			if (sp.aura() != null) s.rig.stage.add( sp.aura().hintQuad() );
		}
	}

	// ------------------------------------------------------------------ the blow's lines

	@Test
	public void everyEdgeLinePutsItsGoldChipAndDrawsNothing(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		a.ch.HP = 10;
		d.ch.HP = 10;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		ClimateManager.debugPrecipOverride = 1f;
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		Sword w = lined( new Sword(), RarityLine.SLAYER, RarityLine.EXECUTIONER, RarityLine.MOONLIT,
				RarityLine.STORMBORN, RarityLine.DEFIANT );
		Quality q = Quality.of( w );
		q.slayer = SlayerFamily.BEASTS;
		final float[] e = new float[1];
		Blow.begin( a.ch, d.ch );
		int quads;
		try {
			quads = made( () -> e[0] = Quality.edge( q, a.ch, d.ch ), 2 );
			Blow.claim( d.ch, "12", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		} finally {
			Blow.end();
		}
		assertEquals( "the lines draw nothing", 0, quads );
		float sum = q.value( RarityLine.SLAYER ) + q.value( RarityLine.EXECUTIONER ) + q.value( RarityLine.MOONLIT )
				+ q.value( RarityLine.STORMBORN ) + q.value( RarityLine.DEFIANT );
		assertEquals( sum, e[0], 1e-5f );
		HitCard c = EnchantState.get( d ).card;
		HashSet<Integer> chips = new HashSet<>();
		for (int i = 0; i < c.chipCount(); i++){
			chips.add( c.chip( i ) );
			assertEquals( "gold chips are drawn plain", 0, c.chipTint( i ) );
		}
		assertEquals( new HashSet<>( Arrays.asList( FloatingText.S_BEAST, FloatingText.EXECUTIONER, FloatingText.MOON,
				FloatingText.STORM, FloatingText.DEFIANT ) ), chips );

		//nothing that did not add, and nothing outside a blow
		a.ch.HP = d.ch.HP = 50;
		ClimateManager.debugPrecipOverride = 0f;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		q.slayer = SlayerFamily.UNDEAD;
		EnchantState.get( d ).card = null;
		Blow.begin( a.ch, d.ch );
		try {
			Quality.edge( q, a.ch, d.ch );
			Blow.claim( d.ch, "12", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		} finally {
			Blow.end();
		}
		assertEquals( "no line added: no chip", 0, EnchantState.get( d ).card.chipCount() );
		assertEquals( 0, made( () -> Quality.edge( q, a.ch, d.ch ), 2 ) );
	}

	@Test
	public void slayersChipIsItsFamilysEmblem(){
		int[] emblems = { FloatingText.S_UNDEAD, FloatingText.S_DEMON, FloatingText.S_CONSTRUCT, FloatingText.S_ELEMENTAL,
				FloatingText.S_BEAST, FloatingText.S_HUMANOID };
		assertEquals( emblems.length, SlayerFamily.values().length );
		for (SlayerFamily f : SlayerFamily.values()) assertEquals( f.name(), emblems[f.ordinal()], LineLooks.chipOf( RarityLine.SLAYER, f ) );
		assertEquals( -1, LineLooks.chipOf( RarityLine.SLAYER, null ) );
		for (RarityLine l : new RarityLine[]{ RarityLine.CRIT, RarityLine.LIFESTEAL, RarityLine.WARM_GRIP, RarityLine.HUNTER,
				RarityLine.FLEET, RarityLine.COOLING }) assertEquals( l.name(), -1, LineLooks.chipOf( l, null ) );
		//a shot whose aim Steady kept from the wind: the gold crosshair
		assertEquals( FloatingText.STEADY, LineLooks.chipOf( RarityLine.STEADY, null ) );

		//a skeleton struck with an undead slayer: the tombstone
		CharSprite a = rig.rat( at( 5, 6 ) );
		Skeleton k = new Skeleton();
		k.pos = at( 6, 6 );
		k.HP = k.HT = 50;
		CharSprite ks = new xyz.gabriwar.warpedpixeldungeon.sprites.SkeletonSprite();
		ks.ch = k;
		k.sprite = ks;
		rig.stage.add( ks );
		ks.place( k.pos );
		ks.visible = true;
		Quality q = Quality.of( lined( new Sword(), RarityLine.SLAYER ) );
		q.slayer = SlayerFamily.UNDEAD;
		Blow.begin( a.ch, k );
		try {
			Quality.edge( q, a.ch, k );
			Blow.claim( k, "9", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		} finally {
			Blow.end();
		}
		HitCard c = EnchantState.get( ks ).card;
		assertEquals( 1, c.chipCount() );
		assertEquals( FloatingText.S_UNDEAD, c.chip( 0 ) );
	}

	@Test
	public void moonlitsChipOnlyWhileItAdds(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Quality q = Quality.of( lined( new Sword(), RarityLine.MOONLIT ) );
		DayNightCycle.Phase[] phases = DayNightCycle.Phase.values();
		for (DayNightCycle.Phase p : phases){
			DayNightCycle.debugPhaseOverride = p;
			EnchantState st = EnchantState.get( d );
			if (st != null) st.card = null;
			Blow.begin( a.ch, d.ch );
			try {
				Quality.edge( q, a.ch, d.ch );
				Blow.claim( d.ch, "7", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
			} finally {
				Blow.end();
			}
			boolean adds = Quality.moonFactor() > 0f;
			assertEquals( p.name(), adds ? 1 : 0, EnchantState.get( d ).card.chipCount() );
			if (adds) assertEquals( FloatingText.MOON, EnchantState.get( d ).card.chip( 0 ) );
		}
	}

	@Test
	public void huntersGlintOnlyWithTheMeat(){
		Sword w = lined( new Sword(), RarityLine.HUNTER );
		hero.belongings.weapon = w;
		int with = 0, without = 0;
		for (int seed = 1; seed <= 40; seed++){
			final int cell = at( 1 + seed % 12, 10 + seed / 12 );
			Dungeon.level.heaps.remove( cell );
			final Rat r = new Rat();
			r.pos = cell;
			Random.pushGenerator( seed );
			int quads;
			try {
				quads = made( () -> Quality.onEnemyKilled( r, hero ), 2 );
			} finally {
				Random.popGenerator();
			}
			Heap h = Dungeon.level.heaps.get( cell );
			boolean meat = h != null && h.peek() instanceof MysteryMeat;
			if (meat){
				with++;
				assertTrue( "the glint with the meat", quads > 0 );
			} else {
				without++;
				assertEquals( "no meat, no glint", 0, quads );
			}
		}
		assertTrue( "both seen: " + with + " / " + without, with > 0 && without > 0 );
	}

	// ------------------------------------------------------------------ the body's lines

	@Test
	public void eachBodyLineActsOnlyUnderItsCondition(){
		hero.belongings.weapon = lined( new Sword(), RarityLine.WARM_GRIP, RarityLine.MOONLIT );
		hero.belongings.armor = lined( new ClothArmor(), RarityLine.COOLING, RarityLine.RAINPROOF, RarityLine.WINDBREAK );
		ClimateManager.debugTempOverride = 0f;

		LineLooks.onWeather( hero, 10f, false, false, 0f );
		assertTrue( LineLooks.acting( hero, LineLooks.WARM_GRIP ) );
		assertFalse( LineLooks.acting( hero, LineLooks.COOLING ) );
		LineLooks.onWeather( hero, 30f, false, false, 0f );
		assertFalse( LineLooks.acting( hero, LineLooks.WARM_GRIP ) );
		assertTrue( LineLooks.acting( hero, LineLooks.COOLING ) );
		LineLooks.onWeather( hero, 21f, false, false, 0f );
		assertFalse( LineLooks.acting( hero, LineLooks.WARM_GRIP ) );
		assertFalse( LineLooks.acting( hero, LineLooks.COOLING ) );

		//moonlit at night only
		assertFalse( LineLooks.acting( hero, LineLooks.MOONLIT ) );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		LineLooks.onWeather( hero, 21f, false, false, 0f );
		assertTrue( LineLooks.acting( hero, LineLooks.MOONLIT ) );

		//rainproof while the rain is on him, its count running and he is not drenched yet
		hero.rainTurns = 0;
		LineLooks.onWeather( hero, 21f, true, false, 0f );
		assertFalse( LineLooks.acting( hero, LineLooks.RAINPROOF ) );
		hero.rainTurns = 2;
		LineLooks.onWeather( hero, 21f, false, false, 0f );
		assertFalse( LineLooks.acting( hero, LineLooks.RAINPROOF ) );
		LineLooks.onWeather( hero, 21f, true, false, 0f );
		assertTrue( LineLooks.acting( hero, LineLooks.RAINPROOF ) );
		Buff.affect( hero, Drenched.class );
		LineLooks.onWeather( hero, 21f, true, false, 0f );
		assertFalse( "drenched: it no longer holds the rain off", LineLooks.acting( hero, LineLooks.RAINPROOF ) );

		//windbreak while the chill would reach him: cold air, a wind over 3, the wind reaching him
		LineLooks.onWeather( hero, 0f, false, true, 5f );
		assertTrue( LineLooks.acting( hero, LineLooks.WINDBREAK ) );
		LineLooks.onWeather( hero, 0f, false, false, 5f );
		assertFalse( LineLooks.acting( hero, LineLooks.WINDBREAK ) );
		LineLooks.onWeather( hero, 0f, false, true, 2f );
		assertFalse( LineLooks.acting( hero, LineLooks.WINDBREAK ) );
		ClimateManager.debugTempOverride = 15f;
		LineLooks.onWeather( hero, 15f, false, true, 5f );
		assertFalse( LineLooks.acting( hero, LineLooks.WINDBREAK ) );

		//a hero with none of the lines keeps no state
		Hero bare = new Hero();
		LineLooks.onWeather( bare, 0f, true, true, 9f );
		assertNull( LineLooks.state( bare, false ) );
	}

	@Test
	public void warmGripsMomentAndIdlesKeepTheirPace(){
		hero.belongings.weapon = lined( new Sword(), RarityLine.WARM_GRIP );
		LineLooks.onWeather( hero, 5f, false, false, 0f );
		List<Float> idles = new ArrayList<>();
		int edges = 0;
		for (int f = 0; f < 60 * 12; f++){
			int n = made( () -> {
				Game.timeTotal += 1 / 60f;
				area.afterDraw( sprite );
			}, 0 );
			assertTrue( "a moment is three quads at most: " + n, n <= 3 );
			if (n > 1) edges++;
			else if (n == 1) idles.add( Game.timeTotal );
		}
		assertEquals( "one starting moment", 1, edges );
		assertTrue( "it idles: " + idles.size(), idles.size() >= 4 );
		for (int i = 1; i < idles.size(); i++){
			assertTrue( "idles " + LineLooks.EVERY[LineLooks.WARM_GRIP] + " s apart",
					idles.get( i ) - idles.get( i - 1 ) >= LineLooks.EVERY[LineLooks.WARM_GRIP] - 1e-3f );
		}

		//it stops and starts again within 30 s: no second moment; after 30 s, one
		LineLooks.onWeather( hero, 20f, false, false, 0f );
		frames( 30 );
		LineLooks.onWeather( hero, 5f, false, false, 0f );
		for (int f = 0; f < 60; f++){
			int n = made( () -> {
				Game.timeTotal += 1 / 60f;
				area.afterDraw( sprite );
			}, 0 );
			assertTrue( "no moment within 30 s", n <= 1 );
		}
		Game.timeTotal += LineLooks.EDGE_GAP;
		LineLooks.onWeather( hero, 20f, false, false, 0f );
		frames( 2 );
		LineLooks.onWeather( hero, 5f, false, false, 0f );
		int most = 0;
		for (int f = 0; f < 5; f++){
			most = Math.max( most, made( () -> {
				Game.timeTotal += 1 / 60f;
				area.afterDraw( sprite );
			}, 0 ) );
		}
		assertEquals( "its moment again past 30 s", 3, most );
	}

	@Test
	public void everyActingLineIdlesOneMoteAtATime(){
		hero.belongings.weapon = lined( new Sword(), RarityLine.WARM_GRIP, RarityLine.MOONLIT );
		hero.belongings.armor = lined( new ClothArmor(), RarityLine.RAINPROOF, RarityLine.WINDBREAK );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		ClimateManager.debugTempOverride = 0f;
		hero.rainTurns = 1;
		LineLooks.onWeather( hero, 5f, true, true, 6f );
		frames( 3 );
		LineLooks.State st = LineLooks.state( hero, false );
		int[] shown = new int[LineLooks.ACTING];
		for (int f = 0; f < 60 * 20; f++){
			Game.timeTotal += 1 / 60f;
			float[] before = st.next.clone();
			int n = made( () -> area.afterDraw( sprite ), 0 );
			assertTrue( "one idle a frame", n <= 1 );
			for (int i = 0; i < LineLooks.ACTING; i++) if (st.next[i] != before[i]) shown[i]++;
		}
		for (int i : new int[]{ LineLooks.WARM_GRIP, LineLooks.MOONLIT, LineLooks.RAINPROOF, LineLooks.WINDBREAK }){
			assertTrue( "line " + i + " idled: " + shown[i], shown[i] >= 2 );
			assertTrue( "line " + i + " no more often than its pace: " + shown[i], shown[i] <= 20f / LineLooks.EVERY[i] + 1 );
		}
		assertEquals( "cooling is not acting", 0, shown[LineLooks.COOLING] );
	}

	@Test
	public void warmGripKnowsTheTorchComesAfterIt(){
		hero.belongings.weapon = lined( new Sword(), RarityLine.WARM_GRIP );
		//lifted to 18 C, then the torch's 6 on top: it acts
		Light torch = new Light();
		torch.flame = true;
		hero.add( torch );
		LineLooks.onWeather( hero, SpringSoak.COMFORT + 6f, false, false, 0f );
		assertTrue( "lifted under the torch", LineLooks.acting( hero, LineLooks.WARM_GRIP ) );
		LineLooks.onWeather( hero, SpringSoak.COMFORT + 7f, false, false, 0f );
		assertFalse( "warm enough before the torch", LineLooks.acting( hero, LineLooks.WARM_GRIP ) );
		//a magical light is no flame: nothing taken off
		torch.flame = false;
		LineLooks.onWeather( hero, SpringSoak.COMFORT + 6f, false, false, 0f );
		assertFalse( LineLooks.acting( hero, LineLooks.WARM_GRIP ) );
		hero.remove( torch );
		LineLooks.onWeather( hero, SpringSoak.COMFORT, false, false, 0f );
		assertTrue( LineLooks.acting( hero, LineLooks.WARM_GRIP ) );
	}

	@Test
	public void aMomentHoldsTheOtherLinesIdlesBack(){
		hero.belongings.weapon = lined( new Sword(), RarityLine.WARM_GRIP );
		hero.belongings.armor = lined( new ClothArmor(), RarityLine.RAINPROOF );
		hero.rainTurns = 1;
		LineLooks.onWeather( hero, 5f, true, false, 0f );
		float edgeAt = -1f;
		for (int f = 0; f < 60 * 4; f++){
			int n = made( () -> {
				Game.timeTotal += 1 / 60f;
				area.afterDraw( sprite );
			}, 0 );
			assertTrue( "three quads a frame at most: " + n, n <= 3 );
			if (n > 1){
				assertTrue( "one moment", edgeAt < 0f );
				edgeAt = Game.timeTotal;
			} else if (n == 1 && edgeAt >= 0f){
				assertTrue( "no idle while the moment's motes live",
						Game.timeTotal - edgeAt >= LineLooks.EDGE_LIFE - 1e-3f );
			}
		}
		assertTrue( "the moment played", edgeAt >= 0f );
	}

	@Test
	public void theLinesAndTheEnchantmentsShareOneCeiling(){
		Sword w = lined( new Sword(), RarityLine.WARM_GRIP, RarityLine.MOONLIT );
		w.enchant( new Blazing() );
		w.addEnchant( new Chilling() );
		ClothArmor a = lined( new ClothArmor(), RarityLine.RAINPROOF, RarityLine.WINDBREAK );
		a.inscribe( new Thorns() );
		a.addGlyph( new Brimstone() );
		hero.belongings.weapon = w;
		hero.belongings.armor = a;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		ClimateManager.debugTempOverride = 0f;
		hero.rainTurns = 1;
		LineLooks.onWeather( hero, 5f, true, true, 6f );
		LineLooks.State st = LineLooks.state( hero, false );
		int lineLooks = 0, most = 0;
		IdentityHashMap<Gizmo, Boolean> alive = new IdentityHashMap<>();
		for (int f = 0; f < 60 * 20; f++){
			float before = st.moteUntil;
			frames( 1 );
			if (st.moteUntil != before) lineLooks++;
			EnchantState es = EnchantState.get( sprite );
			int live = WieldLook.live( es, Game.timeTotal );
			most = Math.max( most, live );
			assertTrue( "counted alive " + live, live <= WieldLook.PER_SPRITE );
			alive.clear();
			rig.alive( alive );
			int quads = 0;
			for (Gizmo g : alive.keySet()) if (g instanceof Visual && ((Visual) g).visible) quads++;
			assertTrue( "drawn alive " + quads, quads <= WieldLook.PER_SPRITE );
		}
		assertTrue( "the enchantments shed: " + most, most >= 4 );
		assertTrue( "the lines still showed: " + lineLooks, lineLooks >= 3 );
		assertTrue( "its moment found room", st.edgeAt[LineLooks.WARM_GRIP] > -1e8f );
	}

	@Test
	public void aLineLookTakesItsPlaceUnderTheCeiling(){
		//WieldLook's motes: five alive for a while
		EnchantState es = EnchantState.of( sprite );
		float t0 = Game.timeTotal;
		for (int i = 0; i < 5; i++){
			es.wieldAt[i] = t0;
			es.wieldLife[i] = 3f;
		}
		assertEquals( 5, WieldLook.live( es, t0 ) );

		//moonlit's idle takes the sixth place, and WieldLook counts it taken
		hero.belongings.weapon = lined( new Sword(), RarityLine.MOONLIT, RarityLine.WARM_GRIP );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		LineLooks.onWeather( hero, 21f, false, false, 0f );
		Game.timeTotal += 1 / 60f;
		assertEquals( 1, made( () -> LineLooks.step( sprite ), 0 ) );
		assertEquals( WieldLook.PER_SPRITE, WieldLook.live( es, Game.timeTotal ) );

		//warm grip starting with no room for its three quads: its moment waits, then plays
		LineLooks.onWeather( hero, 5f, false, false, 0f );
		LineLooks.State st = LineLooks.state( hero, false );
		while (Game.timeTotal < t0 + 3f){
			Game.timeTotal += 1 / 60f;
			assertTrue( "it waits", made( () -> LineLooks.step( sprite ), 0 ) <= 1 );
			assertTrue( WieldLook.live( es, Game.timeTotal ) <= WieldLook.PER_SPRITE );
		}
		int most = 0;
		for (int f = 0; f < 60; f++){
			Game.timeTotal += 1 / 60f;
			most = Math.max( most, made( () -> LineLooks.step( sprite ), 0 ) );
		}
		assertEquals( "its moment once there is room", LineLooks.EDGE_QUADS, most );
		assertTrue( st.edgeAt[LineLooks.WARM_GRIP] > t0 + 3f - 1e-3f );
	}

	@Test
	public void theWindPartsRoundHimAndNeverThroughHim(){
		sprite.width = 12f;
		sprite.height = 15f;
		sprite.place( hero.pos );
		float hw = sprite.width() / 2f, cx = sprite.x + hw;
		IdentityHashMap<Gizmo, Boolean> before = new IdentityHashMap<>(), now = new IdentityHashMap<>();
		rig.alive( before );
		assertTrue( LineLooks.idle( sprite, LineLooks.WINDBREAK ) > 0f );
		rig.alive( now );
		Visual wisp = null;
		for (Gizmo g : now.keySet()) if (!before.containsKey( g )) wisp = (Visual) g;
		assertTrue( "a wisp", wisp != null );
		//where it is drawn: placed by each update, before each draw
		int seen = 0;
		for (rig.frames( 1, false ); wisp.alive && seen < 60; rig.frames( 1, false )){
			//stood on end about its middle: its near side half its height off its middle
			assertEquals( 90f, wisp.angle, 1e-3f );
			float near = Math.abs( wisp.x + wisp.width() / 2f - cx ) - wisp.height() / 2f;
			assertTrue( "its near side " + near + " px from his middle", near >= hw + LineLooks.WIND_OFF - 1e-3f );
			assertTrue( "within a cell of him: " + near, near <= hw + 8f );
			seen++;
		}
		assertTrue( "it lived its frames: " + seen, seen >= 10 );
	}

	@Test
	public void aGoneHerosStateIsTakenFirst(){
		Hero[] party = new Hero[8];
		for (int i = 0; i < party.length; i++){
			party[i] = new Hero();
			Game.timeTotal += 1f;
			LineLooks.State st = LineLooks.state( party[i], true );
			assertTrue( st != null );
		}
		//the third is gone (his game ended); the first is the stalest still here
		LineLooks.state( party[2], false ).hero.clear();
		Hero next = new Hero();
		LineLooks.state( next, true );
		assertTrue( "the first kept", LineLooks.state( party[0], false ) != null );
		assertTrue( LineLooks.state( next, false ) != null );
		for (int i = 0; i < party.length; i++) if (i != 2) assertTrue( "kept " + i, LineLooks.state( party[i], false ) != null );
	}

	@Test
	public void nothingAtMinimal() throws Exception {
		hero.belongings.weapon = lined( new Sword(), RarityLine.WARM_GRIP );
		hero.belongings.armor = lined( new ClothArmor(), RarityLine.HUSHED, RarityLine.FLEET );
		tier( FxBudget.MINIMAL );
		LineLooks.onWeather( hero, 5f, false, false, 0f );
		sleeper( at( 5, 2 ) );
		assertEquals( 0, made( () -> {
			for (int f = 0; f < 600; f++){
				Game.timeTotal += 1 / 60f;
				area.afterDraw( sprite );
				if (f % 9 == 0) area.stepped( sprite, at( 1 + (f / 9) % 6, 2 ), at( 2 + (f / 9) % 6, 2 ) );
			}
			LineLooks.landed( hero, 0.4f );
		}, 2 ) );
		//and its moment is not played late once the tier comes back
		tier( FxBudget.FULL );
		int most = 0;
		for (int f = 0; f < 10; f++){
			most = Math.max( most, made( () -> {
				Game.timeTotal += 1 / 60f;
				area.afterDraw( sprite );
			}, 0 ) );
		}
		assertTrue( "no late moment", most <= 1 );
	}

	//a sleeping rat in the level at a cell
	private Rat sleeper( int cell ){
		Rat r = new Rat();
		r.pos = cell;
		r.state = r.SLEEPING;
		Dungeon.level.mobs.add( r );
		return r;
	}

	@Test
	public void hushedRingsNearAFoeUnawareAtMostOncePerGap(){
		hero.belongings.armor = lined( new ClothArmor(), RarityLine.HUSHED );
		LineLooks.onWeather( hero, 20f, false, false, 0f );
		Rat r = sleeper( at( 6, 2 ) );
		List<Float> rings = new ArrayList<>();
		for (int k = 0; k < 20; k++){
			Game.timeTotal += 0.2f;
			final int from = at( 1 + k % 2, 2 ), to = at( 2 - k % 2, 2 );
			if (made( () -> area.stepped( sprite, from, to ), 0 ) > 0) rings.add( Game.timeTotal );
		}
		assertTrue( "it rang: " + rings.size(), rings.size() >= 2 );
		for (int i = 1; i < rings.size(); i++) assertTrue( rings.get( i ) - rings.get( i - 1 ) >= LineLooks.HUSH_GAP - 1e-3f );

		//a foe hunting him, or too far, or out of his view: nothing
		r.state = r.HUNTING;
		Game.timeTotal += 2f;
		assertEquals( 0, made( () -> area.stepped( sprite, at( 1, 2 ), at( 2, 2 ) ), 0 ) );
		r.state = r.WANDERING;
		Game.timeTotal += 2f;
		assertEquals( "a wandering foe has not noticed him", 1, made( () -> area.stepped( sprite, at( 1, 2 ), at( 2, 2 ) ), 0 ) );
		r.pos = at( 8, 2 );
		Game.timeTotal += 2f;
		assertEquals( 0, made( () -> area.stepped( sprite, at( 1, 2 ), at( 2, 2 ) ), 0 ) );
		r.pos = at( 4, 2 );
		hero.fieldOfView[r.pos] = false;
		Game.timeTotal += 2f;
		assertEquals( 0, made( () -> area.stepped( sprite, at( 1, 2 ), at( 2, 2 ) ), 0 ) );
	}

	@Test
	public void fleetPuffsEveryThirdStepOfARunOnDryFloor() throws Exception {
		hero.belongings.armor = lined( new ClothArmor(), RarityLine.FLEET );
		LineLooks.onWeather( hero, 20f, false, false, 0f );
		int[] puffs = new int[8];
		for (int k = 0; k < 8; k++){
			Game.timeTotal += 0.15f;
			final int from = at( 1 + k, 2 ), to = at( 2 + k, 2 );
			puffs[k] = made( () -> area.stepped( sprite, from, to ), 0 );
		}
		assertTrue( Arrays.toString( puffs ), Arrays.equals( new int[]{ 0, 0, 1, 0, 0, 1, 0, 0 }, puffs ) );

		//a run through water: none
		int wet = 0;
		for (int k = 0; k < 6; k++){
			Game.timeTotal += 0.15f;
			final int from = at( 8 + k % 4 + (k < 4 ? 1 : 0), 5 ), to = from + 1;
			wet += made( () -> area.stepped( sprite, from, to ), 0 );
		}
		assertEquals( "no dust off water", 0, wet );

		//a run broken (a pause, a step from elsewhere) starts again
		Game.timeTotal += 2f;
		int[] again = new int[3];
		for (int k = 0; k < 3; k++){
			Game.timeTotal += k == 2 ? 1f : 0.15f;
			final int from = at( 1 + k, 3 ), to = at( 2 + k, 3 );
			again[k] = made( () -> area.stepped( sprite, from, to ), 0 );
		}
		assertTrue( Arrays.toString( again ), Arrays.equals( new int[]{ 0, 0, 0 }, again ) );

		//below FULL: none
		tier( FxBudget.REDUCED );
		int reduced = 0;
		for (int k = 0; k < 6; k++){
			Game.timeTotal += 0.15f;
			final int from = at( 1 + k, 1 ), to = at( 2 + k, 1 );
			reduced += made( () -> area.stepped( sprite, from, to ), 0 );
		}
		assertEquals( 0, reduced );
	}

	@Test
	public void surefootsLandingIsARingAndAGlint(){
		assertEquals( "no line, nothing", 0, made( () -> LineLooks.landed( hero, 0f ), 2 ) );
		int n = made( () -> LineLooks.landed( hero, 0.3f ), 2 );
		assertTrue( "1-2 quads: " + n, n >= 1 && n <= 2 );
	}

	@Test
	public void theConditionsDrawNoneOfTheGamesRandom(){
		hero.belongings.weapon = lined( new Sword(), RarityLine.WARM_GRIP, RarityLine.MOONLIT );
		hero.belongings.armor = lined( new ClothArmor(), RarityLine.COOLING, RarityLine.RAINPROOF, RarityLine.WINDBREAK,
				RarityLine.HUSHED, RarityLine.FLEET );
		sleeper( at( 6, 2 ) );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		ClimateManager.debugTempOverride = 0f;
		hero.rainTurns = 2;
		assertEquals( 0, EnchantFxTest.draws( () -> {
			for (int f = 0; f < 900; f++){
				if (f % 30 == 0) LineLooks.onWeather( hero, f % 60 == 0 ? 5f : 30f, true, true, 6f );
				Game.timeTotal += 1 / 60f;
				area.afterDraw( sprite );
				if (f % 9 == 0) area.stepped( sprite, at( 1 + (f / 9) % 6, 2 ), at( 2 + (f / 9) % 6, 2 ) );
				rig.frames( 1, false );
			}
			LineLooks.landed( hero, 0.4f );
		} ) );
	}

	// ------------------------------------------------------------------ the item hints

	//an item in a slot (or the info window), its motes run for `seconds`; the most quads it drew in
	//a frame and the frames its hint showed
	private int[] slot( Item item, ItemSprite.Mode mode, boolean first, float seconds ){
		ItemSprite s = new ItemSprite().mode( mode );
		s.view( item );
		s.auraGate( true, first );
		s.x = 20f;
		s.y = 20f;
		rig.stage.add( s );
		int most = 0, hinted = 0;
		for (int f = 0; f < Math.round( seconds * 60f ); f++){
			Game.timeTotal += 1 / 60f;
			s.update();
			ItemAura a = s.aura();
			if (a == null) continue;
			most = Math.max( most, a.quads() );
			if (a.hintQuad().visible) hinted++;
		}
		return new int[]{ most, hinted };
	}

	@Test
	public void oneHintAnItemInItsOrder(){
		assertEquals( LineLooks.WARM_GRIP, ItemAura.hintOf( lined( new Sword(), RarityLine.FLEET, RarityLine.RAINPROOF, RarityLine.WARM_GRIP ) ) );
		assertEquals( LineLooks.COOLING, ItemAura.hintOf( lined( new ClothArmor(), RarityLine.FLEET, RarityLine.RAINPROOF, RarityLine.COOLING ) ) );
		assertEquals( LineLooks.RAINPROOF, ItemAura.hintOf( lined( new ClothArmor(), RarityLine.FLEET, RarityLine.RAINPROOF ) ) );
		Sword moon = lined( new Sword(), RarityLine.MOONLIT, RarityLine.SLAYER );
		assertEquals( "moonlit by night only", -1, ItemAura.hintOf( moon ) );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		assertEquals( LineLooks.MOONLIT, ItemAura.hintOf( moon ) );
		assertEquals( LineLooks.FLEET, ItemAura.hintOf( lined( new ClothArmor(), RarityLine.HUSHED, RarityLine.FLEET ) ) );
		assertEquals( "combat lines get none", -1, ItemAura.hintOf( lined( new Sword(), RarityLine.SLAYER, RarityLine.CRIT ) ) );
		Sword unknown = lined( new Sword(), RarityLine.WARM_GRIP );
		unknown.levelKnown = false;
		unknown.cursedKnown = false;
		assertFalse( unknown.isIdentified() );
		assertEquals( "an unknown item shows no lines", -1, ItemAura.hintOf( unknown ) );
	}

	@Test
	public void aHintShowsOnlyEquippedQuickslottedOrInTheWindow() throws Exception {
		Sword held = lined( new Sword(), RarityLine.WARM_GRIP );
		hero.belongings.weapon = held;
		int[] r = slot( held, ItemSprite.Mode.SLOT, true, ItemAura.HINT_EVERY[LineLooks.WARM_GRIP] * 2 );
		assertEquals( "one quad, no backlight", 1, r[0] );
		assertTrue( "it shows", r[1] > 0 );

		ClothArmor slotted = lined( new ClothArmor(), RarityLine.COOLING );
		Dungeon.quickslot.setSlot( 0, slotted );
		r = slot( slotted, ItemSprite.Mode.SLOT, true, ItemAura.HINT_EVERY[LineLooks.COOLING] * 2 );
		assertEquals( 1, r[0] );
		assertTrue( r[1] > 0 );

		ClothArmor bagged = lined( new ClothArmor(), RarityLine.FLEET );
		r = slot( bagged, ItemSprite.Mode.SLOT, true, ItemAura.HINT_EVERY[LineLooks.FLEET] * 2 );
		assertEquals( "in a bag: none, even pressed", 0, r[1] );
		assertEquals( 0, r[0] );

		r = slot( bagged, ItemSprite.Mode.PANEL, true, ItemAura.HINT_EVERY[LineLooks.FLEET] * 2 );
		assertTrue( "the info window shows it", r[1] > 0 );
		assertEquals( 1, r[0] );

		r = slot( bagged, ItemSprite.Mode.ICON, true, 4f );
		assertEquals( "an icon: none", 0, r[1] );

		tier( FxBudget.MINIMAL );
		r = slot( held, ItemSprite.Mode.SLOT, true, 4f );
		assertEquals( "MINIMAL: none", 0, r[1] );
		assertEquals( 0, r[0] );
	}

	@Test
	public void neverBesideALookOfMagic(){
		Sword w = lined( new Sword(), RarityLine.WARM_GRIP );
		w.enchant( new Blazing() );
		hero.belongings.weapon = w;
		int[] r = slot( w, ItemSprite.Mode.SLOT, true, 4f );
		assertEquals( "an enchanted item shows its motes, no hint", 0, r[1] );
		assertTrue( "within the slot's quads", r[0] <= ItemAura.SLOT_QUADS );
		r = slot( w, ItemSprite.Mode.PANEL, true, 4f );
		assertEquals( 0, r[1] );
	}

	@Test
	public void aHintIsServedLast(){
		//the pool spent by enchanted slots served first
		List<ItemSprite> full = new ArrayList<>();
		for (int i = 0; i < 80; i++){
			Sword w = ItemAuraTest.sword( new Blazing(), new xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling(), null );
			ItemSprite s = new ItemSprite().mode( ItemSprite.Mode.SLOT );
			s.view( w );
			s.auraGate( true, true );
			rig.stage.add( s );
			full.add( s );
		}
		for (int f = 0; f < 3; f++){
			Game.timeTotal += 1 / 60f;
			for (ItemSprite s : full) s.update();
		}
		assertEquals( ItemAura.POOL, ItemAura.held() );
		Sword held = lined( new Sword(), RarityLine.WARM_GRIP );
		hero.belongings.weapon = held;
		ItemSprite s = new ItemSprite().mode( ItemSprite.Mode.SLOT );
		s.view( held );
		s.auraGate( true, true );
		rig.stage.add( s );
		for (int f = 0; f < 120; f++){
			Game.timeTotal += 1 / 60f;
			for (ItemSprite o : full) o.update();
			s.update();
			assertEquals( "it takes from no one", 0, s.aura().claimed() );
			assertFalse( s.aura().hintQuad().visible );
		}
		//room again: it shows
		for (ItemSprite o : full) o.auraGate( false, false );
		for (int f = 0; f < 120; f++){
			Game.timeTotal += 1 / 60f;
			for (ItemSprite o : full) o.update();
			s.update();
		}
		assertEquals( 1, s.aura().claimed() );
	}

	@Test
	public void theHintsDrawNoneOfTheGamesRandom(){
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		Item[] items = { lined( new Sword(), RarityLine.WARM_GRIP ), lined( new ClothArmor(), RarityLine.COOLING ),
				lined( new ClothArmor(), RarityLine.RAINPROOF ), lined( new Sword(), RarityLine.MOONLIT ),
				lined( new ClothArmor(), RarityLine.FLEET ) };
		assertEquals( 0, EnchantFxTest.draws( () -> {
			for (Item i : items) slot( i, ItemSprite.Mode.PANEL, true, 5f );
		} ) );
	}
}
