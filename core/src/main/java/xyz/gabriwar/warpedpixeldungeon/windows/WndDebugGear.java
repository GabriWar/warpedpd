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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.Enchanting;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.SlayerFamily;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.AresLeech;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.BuzzSaw;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.CromLuck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.JupitersHorror;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.LokisPoison;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.NeptuneShock;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Nomnom;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Osmose;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.OptionSlider;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

//the debug editors for one carried item: its two enchantment (or glyph) slots with their
//levels, or its rarity, type, masterwork and every rolled line with its roll. Each change
//lands on the item at once; picking from a list reopens the window on the new state
public class WndDebugGear extends Window {

	private static final int GAP = 2;
	private static final int BTN_HEIGHT = 16;
	private static final int SLIDER_HEIGHT = 24;

	//weapon enchantments no random roll gives: the relic weapons' own and the chainsaw's
	private static final Class<?>[] WEAPON_SPECIAL = {
			AresLeech.class, BuzzSaw.class, CromLuck.class, JupitersHorror.class,
			LokisPoison.class, NeptuneShock.class, Nomnom.class, Osmose.class
	};

	/** picks a weapon or armor from the pack and opens its enchantment slots */
	public static void enchant(){
		pick( "Choose a weapon or armor to enchant",
				item -> item instanceof Weapon || item instanceof Armor, true );
	}

	/** picks any item with a rarity from the pack and opens its rarity, type, masterwork and lines */
	public static void quality(){
		pick( "Choose an item to set its rarity and stats", Quality::eligible, false );
	}

	private interface ItemTest { boolean test( Item item ); }

	private static void pick( String prompt, ItemTest test, boolean magic ){
		GameScene.selectItem( new WndBag.ItemSelector(){
			@Override
			public String textPrompt(){ return prompt; }

			@Override
			public boolean itemSelectable( Item item ){ return test.test( item ); }

			@Override
			public void onSelect( Item item ){
				if (item == null) return;
				//an item made before the rarity system has none yet: roll it one to edit
				if (!magic) Quality.roll( item );
				GameScene.show( new WndDebugGear( item, magic ) );
			}
		} );
	}

	private final Item item;
	private final boolean magic;
	private final int width;
	private final Component content;
	private float pos = 0;

	private WndDebugGear( Item item, boolean magic ){
		super();
		this.item = item;
		this.magic = magic;

		//as wide as the debug window it opens from
		width = WndDebug.width( chrome.marginHor() );

		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.titleCase( item.name() ), 9 );
		title.hardlight( TITLE_COLOR );
		title.maxWidth( width );
		title.setPos( (width - title.width()) / 2f, 0 );
		add( title );
		float top = title.bottom() + GAP * 2;

		//the pane before its controls: pointer listeners fire newest first (see WndDebugScenes)
		ScrollPane pane = new ScrollPane( new Component() );
		content = pane.content();
		if (magic) buildMagic(); else buildQuality();
		button( "Done", this::hide );
		content.setSize( width, Math.max( 0, pos - GAP ) );
		pane.dragOverButtons();

		int maxH = (int)(PixelScene.uiCamera.height - chrome.marginVer() - 20);
		float paneH = Math.min( content.height(), maxH - top );

		//resize before the pane goes in, for its camera (see WndDebug)
		resize( width, (int)Math.ceil( top + paneH ) );
		add( pane );
		pane.setRect( 0, top, width, paneH );
	}

	// ---------------------------------------------------------------- enchantments

	private void buildMagic(){
		boolean weapon = item instanceof Weapon;
		for (int s = 0; s < 2; s++){
			final int slot = s;
			Object ench = slot( slot );
			RedButton btn = button( "Slot " + (slot + 1) + ": " + (ench == null ? "none" : Messages.titleCase( magicName( ench ) )),
					() -> chooseMagic( slot ) );
			//the second slot only ever sits behind the first
			if (slot == 1 && slot( 0 ) == null) btn.enable( false );
			if (ench != null){
				int max = weapon ? Weapon.Enchantment.MAX_LEVEL : Armor.Glyph.MAX_LEVEL;
				slider( "0", Integer.toString( max ), 0, max, magicLevel( ench ),
						v -> "Level " + v, v -> {
							if (ench instanceof Weapon.Enchantment) ((Weapon.Enchantment) ench).level( v );
							else ((Armor.Glyph) ench).level( v );
							Item.updateQuickslot();
						} );
			}
		}
		button( weapon ? "Remove both enchantments" : "Remove both glyphs", () -> {
			if (weapon) ((Weapon) item).enchant( null );
			else ((Armor) item).inscribe( null );
			reopen();
		} );
	}

	private Object slot( int slot ){
		if (item instanceof Weapon){
			Weapon w = (Weapon) item;
			return slot == 0 ? w.enchantment : w.enchantment2;
		}
		Armor a = (Armor) item;
		return slot == 0 ? a.glyph : a.glyph2;
	}

	private static String magicName( Object ench ){
		return ench instanceof Weapon.Enchantment ? ((Weapon.Enchantment) ench).name() : ((Armor.Glyph) ench).name();
	}

	private static int magicLevel( Object ench ){
		return ench instanceof Weapon.Enchantment ? ((Weapon.Enchantment) ench).level() : ((Armor.Glyph) ench).level();
	}

	//every enchantment or glyph there is, by tier, then nothing
	private void chooseMagic( int slot ){
		boolean weapon = item instanceof Weapon;
		Class<?>[][] tables = weapon
				? new Class<?>[][]{ Weapon.Enchantment.common, Weapon.Enchantment.uncommon,
						Weapon.Enchantment.rare, WEAPON_SPECIAL, Weapon.Enchantment.curses }
				: new Class<?>[][]{ Armor.Glyph.common, Armor.Glyph.uncommon,
						Armor.Glyph.rare, Armor.Glyph.curses };
		String[] tiers = weapon
				? new String[]{ "common", "uncommon", "rare", "special", "curse" }
				: new String[]{ "common", "uncommon", "rare", "curse" };
		ArrayList<String> labels = new ArrayList<>();
		ArrayList<Runnable> actions = new ArrayList<>();
		for (int t = 0; t < tables.length; t++){
			for (Class<?> cls : tables[t]){
				labels.add( Messages.titleCase( magicName( Reflection.newInstance( cls ) ) ) + " (" + tiers[t] + ")" );
				actions.add( () -> setMagic( slot, Reflection.newInstance( cls ) ) );
			}
		}
		labels.add( "- none -" );
		actions.add( () -> setMagic( slot, null ) );
		GameScene.show( WndDebugPicker.forChoices( (weapon ? "ENCHANT" : "INSCRIBE") + " SLOT " + (slot + 1),
				labels.toArray( new String[0] ), actions.toArray( new Runnable[0] ) ) );
	}

	private void setMagic( int slot, Object ench ){
		Object first = slot( 0 );
		if (slot == 1 && ench != null && first != null && first.getClass() == ench.getClass()){
			GLog.w( "Slot 1 already holds that." );
			return;
		}
		if (item instanceof Weapon){
			Weapon w = (Weapon) item;
			if (slot == 0){
				//emptying the first slot moves the second up, level and all
				w.enchant( ench != null ? (Weapon.Enchantment) ench : w.enchantment2 );
			} else {
				w.enchantment2 = (Weapon.Enchantment) ench;
				Quality.empower( w );
			}
		} else {
			Armor a = (Armor) item;
			if (slot == 0){
				a.inscribe( ench != null ? (Armor.Glyph) ench : a.glyph2 );
			} else {
				a.glyph2 = (Armor.Glyph) ench;
				Quality.empower( a );
			}
		}
		if (ench != null && Dungeon.hero != null) Enchanting.show( Dungeon.hero, item );
		Item.updateQuickslot();
		reopen();
	}

	// ---------------------------------------------------------------- rarity, type and lines

	private void buildQuality(){
		Quality q = item.quality;
		slider( "common", "exotic", 0, Rarity.values().length - 1, q.rarity.ordinal(),
				v -> "Rarity: " + Rarity.values()[v].title(), v -> {
					q.rarity = Rarity.values()[v];
					changed();
				} );
		slider( "gamma", "alpha", 0, ItemType.values().length - 1, q.type.ordinal(),
				v -> "Type: " + ItemType.values()[v].title(), v -> {
					q.type = ItemType.values()[v];
					changed();
				} );
		slider( "0", Integer.toString( Quality.MASTERWORK_MAX ), 0, Quality.MASTERWORK_MAX, q.masterwork,
				v -> "Masterwork: " + v + "/" + Quality.MASTERWORK_MAX, v -> {
					q.masterwork = v;
					changed();
				} );
		button( "Roll new lines for this rarity", () -> {
			q.rerollRarity( item, q.rarity );
			reopen();
		} );
		if (item instanceof Weapon || item instanceof Armor){
			Bundlable v = Quality.vestigeMagic( item );
			button( "Vestige: " + (v == null ? "none" : Messages.titleCase( Quality.vestigeName( v ) )), this::chooseVestige );
		}

		for (int i = 0; i < q.lines.size(); i++){
			final int at = i;
			RedButton line = button( lineText( at ), () -> chooseLine( at ) );
			slider( "0%", "100%", 0, 100, Math.round( q.rolls.get( at ) * 100f ), v -> "Roll: " + v + "%", v -> {
				q.rolls.set( at, v / 100f );
				changed();
				line.text( lineText( at ) );
			} );
		}
		if (!unusedLines( -1 ).isEmpty()) button( "+ Add a line", () -> chooseLine( -1 ) );
	}

	//the line as the item shows it, with masterwork worked in
	private String lineText( int at ){
		Quality q = item.quality;
		RarityLine l = q.lines.get( at );
		return l.describe( item, q.roll( l ) );
	}

	//the lines the item's family can roll that it does not carry yet (keeping the one at `keep`)
	private ArrayList<RarityLine> unusedLines( int keep ){
		Quality q = item.quality;
		ArrayList<RarityLine> out = RarityLine.pool( Quality.family( item ), item );
		for (int i = 0; i < q.lines.size(); i++){
			if (i != keep) out.remove( q.lines.get( i ) );
		}
		return out;
	}

	//at = -1 adds a new line, at its maximum roll; otherwise swaps or removes the line at `at`
	private void chooseLine( int at ){
		Quality q = item.quality;
		ArrayList<String> labels = new ArrayList<>();
		ArrayList<Runnable> actions = new ArrayList<>();
		for (RarityLine l : unusedLines( at )){
			if (l == RarityLine.SLAYER){
				//its family is picked before it goes in
				labels.add( "+" + Math.round( l.max() * 100f ) + "% damage against a family you pick (at max)" );
				actions.add( () -> chooseSlayer( at ) );
				continue;
			}
			labels.add( l.describe( item, 1f ) + " (at max)" );
			actions.add( () -> {
				if (at < 0){
					q.lines.add( l );
					q.rolls.add( 1f );
				} else {
					q.lines.set( at, l );
				}
				changed();
				reopen();
			} );
		}
		if (at >= 0){
			labels.add( "- remove line -" );
			actions.add( () -> {
				q.lines.remove( at );
				q.rolls.remove( at );
				changed();
				reopen();
			} );
		}
		GameScene.show( WndDebugPicker.forChoices( at < 0 ? "ADD A LINE" : "LINE " + (at + 1),
				labels.toArray( new String[0] ), actions.toArray( new Runnable[0] ) ) );
	}

	//the family a SLAYER line is sharpened against: the line goes in with it (added at its maximum
	//roll, or swapped in at `at`), or takes the new family where it already is
	private void chooseSlayer( int at ){
		Quality q = item.quality;
		ArrayList<String> labels = new ArrayList<>();
		ArrayList<Runnable> actions = new ArrayList<>();
		for (SlayerFamily f : SlayerFamily.values()){
			labels.add( Messages.titleCase( f.title() ) );
			actions.add( () -> {
				q.slayer = f;
				if (at < 0){
					q.lines.add( RarityLine.SLAYER );
					q.rolls.add( 1f );
				} else {
					q.lines.set( at, RarityLine.SLAYER );
				}
				changed();
				reopen();
			} );
		}
		GameScene.show( WndDebugPicker.forChoices( "SLAYER FAMILY", labels.toArray( new String[0] ), actions.toArray( new Runnable[0] ) ) );
	}

	//any enchantment (a weapon's) or glyph (an armor's) a vestige can be, or none: the line at
	//index 0 (at its maximum roll) and the magic are set or cleared together
	private void chooseVestige(){
		Quality q = item.quality;
		ArrayList<String> labels = new ArrayList<>();
		ArrayList<Runnable> actions = new ArrayList<>();
		for (Class<?> cls : item instanceof Armor ? Quality.glyphVestigePool() : Quality.vestigePool()){
			labels.add( Messages.titleCase( Quality.vestigeName( (Bundlable) Reflection.newInstance( cls ) ) ) );
			actions.add( () -> setVestige( q, (Bundlable) Reflection.newInstance( cls ) ) );
		}
		labels.add( "- none -" );
		actions.add( () -> setVestige( q, null ) );
		GameScene.show( WndDebugPicker.forChoices( "VESTIGE", labels.toArray( new String[0] ), actions.toArray( new Runnable[0] ) ) );
	}

	private void setVestige( Quality q, Bundlable v ){
		int at = q.lines.indexOf( RarityLine.VESTIGE );
		if (at >= 0){
			q.lines.remove( at );
			q.rolls.remove( at );
		}
		q.vestige = v;
		if (v != null){
			q.lines.add( 0, RarityLine.VESTIGE );
			q.rolls.add( 0, 1f );
		}
		changed();
		reopen();
	}

	private void changed(){
		item.quality.applyDerived( item );
		Item.updateQuickslot();
	}

	// ---------------------------------------------------------------- layout

	private void reopen(){
		hide();
		GameScene.show( new WndDebugGear( item, magic ) );
	}

	private RedButton button( String label, Runnable action ){
		RedButton btn = new RedButton( label, 7 ){
			@Override
			protected void onClick(){
				action.run();
			}
		};
		btn.setRect( 0, pos, width, BTN_HEIGHT );
		content.add( btn );
		pos = btn.bottom() + GAP;
		return btn;
	}

	//a slider whose title shows its value
	private void slider( String minTxt, String maxTxt, int min, int max, int value, IntFunction<String> title, IntConsumer onChange ){
		OptionSlider s = new OptionSlider( title.apply( value ), minTxt, maxTxt, min, max ){
			@Override
			protected void onChange(){
				setTitle( title.apply( getSelectedValue() ) );
				onChange.accept( getSelectedValue() );
			}
		};
		s.setSelectedValue( value );
		s.setRect( 0, pos, width, SLIDER_HEIGHT );
		content.add( s );
		pos = s.bottom() + GAP;
	}
}
