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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ArmoredStatue;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Elastic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Projecting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Dagger;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greataxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Mace;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Spear;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.FT.FT;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingKnife;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.List;

/**
 * The item itself, in the enchantments' gallery page (EnchantArea) and its debug scenes: every look
 * on the floor by day and at night, thrown and shot, carried, and the first sight of a drop
 * (exhibits); fx-enchant-items (-night): a heap of every enchantment, glyph and known curse round
 * the hero; fx-enchant-bag: a backpack of 43 exotic enchanted items, two enchantments and echoes
 * among them, for the slots' cost with the debug profiler (scenes). The exhibits restage looks on
 * the gallery's stage; whatever they lay down or lend they take back as they end, or as the next
 * of them starts.
 */
public final class ItemExhibits {

	private ItemExhibits(){}

	/** Heaps a floor page shows; seconds a page. */
	static final int PAGE = 12;
	static final float PAGE_FOR = 3f;

	/** The page's exhibits (EnchantArea.exhibits). */
	public static void exhibits( FxGallery.Page page ){
		final List<EnchantLook> looks = EnchantLooks.all();
		final int pages = (looks.size() + PAGE - 1) / PAGE;
		page.add( "items on the floor, by day and at night", pages * PAGE_FOR + 4.5f, s -> {
			tidy();
			HeapLook.forgetSights();
			for (int p = 0; p < pages; p++){
				final int from = p * PAGE;
				s.after( 0.2f + p * PAGE_FOR, () -> {
					tidy();
					StringBuilder names = new StringBuilder();
					for (int i = 0; i < PAGE && from + i < looks.size(); i++){
						EnchantLook l = looks.get( from + i );
						lay( itemOf( l ), s.cell( -6 + i, -2 ) );
						names.append( i == 0 ? "" : ", " ).append( l.of.getSimpleName() );
					}
					GLog.i( "  " + names );
				} );
			}
			s.after( 0.2f + pages * PAGE_FOR, () -> {
				tidy();
				GLog.i( "  two enchantments crossfading: Blazing and Chilling, Vampiric and Grim" );
				Sword a = new Sword();
				a.enchant( new Blazing() );
				a.addEnchant( new Chilling() );
				lay( a, s.cell( -1, -2 ) );
				Sword b = new Sword();
				b.enchant( new Vampiric() );
				b.addEnchant( new Grim() );
				lay( b, s.cell( 1, -2 ) );
			} );
			s.after( pages * PAGE_FOR + 4.3f, ItemExhibits::tidy );
		} );

		page.add( "thrown and shot", 9.5f, s -> {
			tidy();
			int y = -2;
			ThrowingKnife knife = new ThrowingKnife();
			knife.enchant( new Blazing() );
			s.after( 0.3f, () -> {
				GLog.i( "  a blazing knife" );
				fly( s.cell( -6, y ), s.cell( 5, y ), knife );
			} );
			Dart dart = new Dart();
			dart.enchant( new Chilling() );
			s.after( 1.5f, () -> {
				GLog.i( "  a chilling dart" );
				fly( s.cell( -6, y ), s.cell( 5, y ), dart );
			} );
			SpiritBow bow = new SpiritBow();
			bow.enchant( new Shocking() );
			s.after( 2.7f, () -> {
				GLog.i( "  a spirit arrow from a shocking bow" );
				fly( s.cell( -6, y ), s.cell( 5, y ), bow.knockArrow() );
			} );
			FT ft = new FT();
			ft.enchant( new Blazing() );
			s.after( 3.9f, () -> {
				GLog.i( "  a flamethrower's round, its launcher's magic" );
				fly( s.cell( -6, y ), s.cell( 5, y ), ft.knockBullet() );
			} );
			ThrowingKnife phase = new ThrowingKnife();
			phase.enchant( new Projecting() );
			s.after( 5.1f, () -> {
				GLog.i( "  a throw through the wall stub" );
				fly( s.cell( -7, 3 ), s.cell( -1, 3 ), phase );
			} );
			HeavyBoomerang rang = new HeavyBoomerang();
			rang.enchant( new Elastic() );
			s.after( 6.3f, () -> {
				GLog.i( "  a boomerang out, then back" );
				fly( s.cell( -1, 1 ), s.cell( 5, 3 ), rang );
			} );
			s.after( 7.6f, () -> {
				MissileSprite back = fly( s.cell( 5, 3 ), s.cell( -1, 1 ), rang );
				//the return fades in, as a boomerang's does (HeavyBoomerang.CircleBack)
				if (back != null){
					back.alpha( 0f );
					back.parent.add( new AlphaTweener( back, 1f, 0.3f ) );
				}
			} );
		} );

		page.add( "the hero carrying them", 12.5f, s -> {
			tidy();
			s.after( 0.2f, () -> {
				GLog.i( "  the hero holds a blazing sword: its embers at his side, its light round him by night" );
				Sword lent = new Sword();
				lent.enchant( new Blazing() );
				lend( lent );
			} );
			s.after( 3.6f, () -> {
				GLog.i( "  a statue's shocking blade, an armoured statue's thorns and brimstone, a mirage stepping" );
				FxStage.Puppet st = s.puppet( Shown.class, s.cell( -3, -2 ) );
				if (st != null) ((Shown) st.ch).arm( weapon( new Sword(), new Shocking() ) );
				FxStage.Puppet arm = s.puppet( ShownArmored.class, s.cell( 3, -2 ) );
				if (arm != null) ((ShownArmored) arm.ch).arm( weapon( new Mace(), null ), armour( new ClothArmor(), new Thorns(), new Brimstone() ) );
				final FxStage.Puppet mirage = s.puppet( ShownArmored.class, s.cell( -2, 3 ) );
				if (mirage != null){
					((ShownArmored) mirage.ch).arm( null, armour( new LeatherArmor(), new Afterimage(), null ) );
					for (int k = 0; k < 4; k++){
						final int to = s.cell( -1 + (k % 2), 3 );
						s.after( 0.6f + k * 0.5f, () -> mirage.move( to ) );
					}
				}
			} );
			s.after( 7f, () -> {
				GLog.i( "  a statue's Kinetic sword charging: its ring beats faster and brighter" );
				final FxStage.Puppet k = s.puppet( Shown.class, s.cell( 0, 3 ) );
				if (k == null) return;
				((Shown) k.ch).arm( weapon( new Greataxe(), new Kinetic() ) );
				for (int i = 1; i <= 4; i++){
					final int step = i;
					s.after( i * 1.1f, () -> {
						Kinetic.ConservedDamage c = Buff.affect( k.ch, Kinetic.ConservedDamage.class );
						c.setBonus( Math.round( k.ch.HT * 0.2f * step ) );
					} );
				}
				s.after( 5.2f, () -> Buff.detach( k.ch, Kinetic.ConservedDamage.class ) );
			} );
			s.after( 12.3f, ItemExhibits::giveBack );
		} );

		page.add( "the first sight of an enchanted drop", 6.5f, s -> {
			tidy();
			HeapLook.forgetSights();
			Sword exotic = new Sword();
			Quality.roll( exotic, Rarity.EXOTIC, null, 0 );
			exotic.enchant( new Blazing() );
			s.after( 0.4f, () -> {
				GLog.i( "  an exotic blazing sword falls: its motif bursts, a pool of its light, a star, its sting" );
				drop( exotic, s.cell( 2, -2 ) );
			} );
			Dagger plain = new Dagger();
			Quality.roll( plain, Rarity.COMMON, null, 0 );
			plain.enchant( new Chilling() );
			s.after( 2.2f, () -> {
				GLog.i( "  a common chilling dagger: the same, silent" );
				drop( plain, s.cell( -2, -2 ) );
			} );
			Spear cursed = new Spear();
			cursed.enchant( new Annoying() );
			cursed.cursed = cursed.cursedKnown = true;
			s.after( 4f, () -> {
				GLog.i( "  a spear whose curse is known: its hex" );
				drop( cursed, s.cell( 0, -3 ) );
			} );
			s.after( 6.3f, ItemExhibits::tidy );
		} );
	}

	// ------------------------------------------------------------------ the stage's helpers

	//the heaps this page laid, and the weapon lent to the hero (and his own, kept in his pack)
	private static final ArrayList<Heap> laid = new ArrayList<>();
	private static KindOfWeapon lent, own;

	//its heaps taken up and the hero's weapon given back
	static void tidy(){
		for (Heap h : laid) if (Dungeon.level != null && Dungeon.level.heaps.get( h.pos ) == h) h.destroy();
		laid.clear();
		giveBack();
	}

	private static void lay( Item item, int cell ){
		if (item == null || cell == -1 || Dungeon.level == null) return;
		Heap h = Dungeon.level.drop( item, cell );
		h.seen = true;
		laid.add( h );
	}

	private static void drop( Item item, int cell ){
		if (cell == -1 || Dungeon.level == null || Dungeon.hero == null) return;
		Heap h = Dungeon.level.drop( item, cell );
		h.seen = true;
		laid.add( h );
		if (h.sprite != null) h.sprite.drop( Dungeon.hero.pos );
	}

	private static MissileSprite fly( int from, int to, Item item ){
		if (from == -1 || to == -1 || Dungeon.hero == null || Dungeon.hero.sprite == null || Dungeon.hero.sprite.parent == null) return null;
		MissileSprite ms = (MissileSprite) Dungeon.hero.sprite.parent.recycle( MissileSprite.class );
		ms.reset( from, to, item, null );
		return ms;
	}

	//the hero holds it for the page; his own weapon waits in his pack (or at his feet), so leaving
	//the page early loses him nothing
	private static void lend( KindOfWeapon w ){
		Hero h = Dungeon.hero;
		if (h == null || lent != null) return;
		own = h.belongings.weapon;
		if (own != null && !own.collect( h.belongings.backpack )) Dungeon.level.drop( own, h.pos );
		h.belongings.weapon = w;
		lent = w;
	}

	private static void giveBack(){
		Hero h = Dungeon.hero;
		if (h == null || lent == null){
			lent = own = null;
			return;
		}
		if (h.belongings.weapon == lent){
			h.belongings.weapon = null;
			if (own != null && h.belongings.backpack.items.remove( own )) h.belongings.weapon = own;
		}
		lent = own = null;
	}

	/** A look's item: a +0 sword of a weapon's enchantment or curse, a cloth armour of a glyph or an
	 *  armour curse; a curse known. */
	static Item itemOf( EnchantLook l ){
		Object e = Reflection.newInstance( l.of );
		if (e instanceof Weapon.Enchantment){
			Sword w = new Sword();
			w.enchant( (Weapon.Enchantment) e );
			if (l.curse) w.cursed = w.cursedKnown = true;
			return w;
		}
		if (e instanceof Armor.Glyph){
			ClothArmor a = new ClothArmor();
			a.inscribe( (Armor.Glyph) e );
			if (l.curse) a.cursed = a.cursedKnown = true;
			return a;
		}
		return null;
	}

	private static Weapon weapon( Weapon w, Weapon.Enchantment e ){
		if (e != null) w.enchant( e );
		w.identify();
		return w;
	}

	private static Armor armour( Armor a, Armor.Glyph g, Armor.Glyph g2 ){
		if (g != null) a.inscribe( g );
		if (g2 != null) a.addGlyph( g2 );
		a.identify();
		return a;
	}

	/** A statue puppet that holds the weapon it is handed (a gallery's: never on a level). */
	public static class Shown extends Statue {
		void arm( Weapon w ){
			weapon = w;
		}
	}

	/** An armoured statue puppet that holds the weapon and wears the armour it is handed. */
	public static class ShownArmored extends ArmoredStatue {
		void arm( Weapon w, Armor a ){
			weapon = w;
			armor = a;
		}
	}

	// ------------------------------------------------------------------ the scenes

	/** The scenes (EnchantArea.scenes). */
	public static void scenes( List<DebugScenes.Scene> out ){
		out.add( new Items( false ) );
		out.add( new Items( true ) );
		out.add( new Bag43() );
	}

	//the blank room round the hero; false when he was sent below first (the scene is to be run again)
	private static boolean room( Hero hero, String id ){
		DebugScenes.Scene room = null;
		for (DebugScenes.Scene s : DebugScenes.SCENES) if (s.id().equals( "blank-room" )) room = s;
		if (room == null) return false;
		room.apply( hero );
		if (Dungeon.level instanceof OverworldLevel){
			GLog.w( "Scene: run " + id + " again once below the surface (the blank room needs a dungeon floor)" );
			return false;
		}
		return true;
	}

	private static void hold( boolean night ){
		DayNightCycle.debugPhaseOverride = night ? DayNightCycle.Phase.NIGHT : DayNightCycle.Phase.DAY;
		ClimateManager.onHeroTurn();
		GameScene.updateDayNightTint();
	}

	/**
	 * fx-enchant-items (and fx-enchant-items-night): a heap of every weapon enchantment, glyph and
	 * known curse in rows round the hero in the blank room, each a +0 sword or a cloth armour, by day
	 * or held at night: every look on the floor at once, its motes and light, and the first sight of
	 * each as the room comes into view.
	 */
	static final class Items implements DebugScenes.Scene {

		final boolean night;

		Items( boolean night ){
			this.night = night;
		}

		@Override
		public String id(){
			return night ? "fx-enchant-items-night" : "fx-enchant-items";
		}

		@Override
		public String title(){
			return night ? "Effects: a heap of every enchantment, glyph and curse, at night"
					: "Effects: a heap of every enchantment, glyph and curse";
		}

		@Override
		public void apply( Hero hero ){
			if (!room( hero, id() )) return;
			//every heap laid flares as it is first seen, though the scene ran here before
			Fx.post( HeapLook::forgetSights );
			Level level = Dungeon.level;
			int w = level.width(), i = 0;
			List<EnchantLook> looks = EnchantLooks.all();
			for (int dy = -4; dy <= 4 && i < looks.size(); dy++){
				for (int dx = -7; dx <= 7 && i < looks.size(); dx++){
					if (dx == 0 && dy == 0) continue;
					int cell = hero.pos + dx + dy * w;
					if (cell < 0 || cell >= level.length() || level.solid[cell]) continue;
					Item item = itemOf( looks.get( i++ ) );
					if (item == null) continue;
					item.identify();
					Heap h = level.drop( item, cell );
					h.seen = true;
				}
			}
			hold( night );
			GLog.i( "Scene: " + i + " heaps round the hero, a +0 sword of every weapon enchantment and curse and a cloth"
					+ " armour of every glyph and armour curse, the curses known. " + (night ? "Held at night." : "Held by day.") );
		}
	}

	/**
	 * fx-enchant-bag: the hero's backpack holds 43 exotic enchanted items - every weapon enchantment
	 * on a weapon of its own, every third with a second enchantment, every fourth with an echo, and
	 * six armours, three with two glyphs - for what a full bag of slots costs (the debug profiler).
	 * What he carried loose is laid at his feet first.
	 */
	static final class Bag43 implements DebugScenes.Scene {

		static final int ITEMS = 43;

		@Override
		public String id(){
			return "fx-enchant-bag";
		}

		@Override
		public String title(){
			return "Effects: a backpack of 43 exotic enchanted items";
		}

		@Override
		public void apply( Hero hero ){
			ArrayList<Item> loose = new ArrayList<>();
			for (Item it : hero.belongings.backpack.items) if (!(it instanceof Bag)) loose.add( it );
			for (Item it : loose){
				hero.belongings.backpack.items.remove( it );
				Dungeon.level.drop( it, hero.pos );
			}
			List<EnchantLook> looks = EnchantLooks.all();
			Class<?>[] kinds = { Sword.class, Spear.class, Mace.class, Dagger.class, Greataxe.class };
			int made = 0;
			for (int i = EnchantLooks.ENCHANTMENTS; i < EnchantLooks.GLYPHS && made < ITEMS - 6; i++){
				Weapon w = (Weapon) Reflection.newInstance( kinds[i % kinds.length] );
				Weapon.Enchantment e = (Weapon.Enchantment) Reflection.newInstance( looks.get( i ).of );
				if (w == null || e == null) continue;
				exotic( w );
				w.enchant( e );
				if (i % 3 == 2){
					Weapon.Enchantment e2 = (Weapon.Enchantment) Reflection.newInstance( looks.get( (i + 7) % EnchantLooks.GLYPHS ).of );
					if (e2 != null) w.addEnchant( e2 );
				}
				if (i % 4 == 3) echo( w, (Weapon.Enchantment) Reflection.newInstance( looks.get( (i + 11) % EnchantLooks.GLYPHS ).of ) );
				w.identify();
				if (w.collect( hero.belongings.backpack )) made++;
			}
			for (int k = 0; k < 6 && made < ITEMS; k++){
				Armor a = new ClothArmor();
				exotic( a );
				a.inscribe( (Armor.Glyph) Reflection.newInstance( looks.get( EnchantLooks.GLYPHS + k * 3 ).of ) );
				if (k % 2 == 0) a.addGlyph( (Armor.Glyph) Reflection.newInstance( looks.get( EnchantLooks.GLYPHS + k * 3 + 1 ).of ) );
				a.identify();
				if (a.collect( hero.belongings.backpack )) made++;
			}
			GLog.i( "Scene: " + made + " exotic enchanted items in the backpack (two enchantments on every third weapon, an"
					+ " echo on every fourth, six armours); what was loose lies at the hero's feet." );
		}

		private static void exotic( Item item ){
			item.quality = null;
			Quality.roll( item, Rarity.EXOTIC, null, 0 );
		}

		//its vestige set to this enchantment (as the debug gear editor sets one)
		private static void echo( Weapon w, Weapon.Enchantment v ){
			Quality q = Quality.of( w );
			if (q == null || v == null) return;
			int at = q.lines.indexOf( RarityLine.VESTIGE );
			if (at >= 0){
				q.lines.remove( at );
				q.rolls.remove( at );
			}
			q.vestige = v;
			q.lines.add( 0, RarityLine.VESTIGE );
			q.rolls.add( 0, 1f );
		}
	}
}
