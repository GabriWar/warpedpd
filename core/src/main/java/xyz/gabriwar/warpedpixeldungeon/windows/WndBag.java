/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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
import xyz.gabriwar.warpedpixeldungeon.WPDAction;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.AnkhChain;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.bags.KeyRing;
import xyz.gabriwar.warpedpixeldungeon.items.bags.MagicalHolster;
import xyz.gabriwar.warpedpixeldungeon.items.bags.PotionBandolier;
import xyz.gabriwar.warpedpixeldungeon.items.bags.ScrollHolder;
import xyz.gabriwar.warpedpixeldungeon.items.bags.FoodPouch;
import xyz.gabriwar.warpedpixeldungeon.items.bags.VelvetPouch;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.InventorySlot;
import xyz.gabriwar.warpedpixeldungeon.ui.QuickSlotButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.RightClickMenu;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.input.GameAction;
import com.watabou.noosa.ui.Component;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;

public class WndBag extends WndTabbed {
	
	//only one bag window can appear at a time
	public static Window INSTANCE;

	protected static final int COLS_P   = 6;
	protected static final int COLS_L   = 8;
	
	protected static int SLOT_WIDTH_P   = 28;
	protected static int SLOT_WIDTH_L   = 28;

	protected static int SLOT_HEIGHT_P	= 28;
	protected static int SLOT_HEIGHT_L	= 28;

	protected static final int SLOT_MARGIN	= 1;
	
	protected static final int TITLE_HEIGHT	= 14;
	
	private ItemSelector selector;

	private int nCols;
	private int nRows;

	private int slotWidth;
	private int slotHeight;

	protected int count;
	protected int col;
	protected int row;

	private ScrollPane scrollPane;
	private Component scrollContent;
	
	private static Bag lastBag;

	public WndBag( Bag bag ) {
		this(bag, null);
	}

	public WndBag( Bag bag, ItemSelector selector ) {
		
		super();
		
		if( INSTANCE != null ){
			INSTANCE.hide();
		}
		INSTANCE = this;
		
		this.selector = selector;
		
		lastBag = bag;

		slotWidth = PixelScene.landscape() ? SLOT_WIDTH_L : SLOT_WIDTH_P;
		slotHeight = PixelScene.landscape() ? SLOT_HEIGHT_L : SLOT_HEIGHT_P;

		nCols = PixelScene.landscape() ? COLS_L : COLS_P;

		// Grid rows based on USABLE slots only (capacity minus hidden bags).
		// Bags are shown as tab buttons at the bottom, not as grid slots.
		// Without this subtraction, the grid would have extra empty rows
		// that the player can never fill (one phantom row per ~nCols bags).
		//
		// Backpack example: capacity()=50 (43 base + 7 bags), bagCount=7
		//   → usable slots = 43, which matches Bag.capacity() and the HUD.
		int bagCount = 0;
		for (Item item : bag.items) {
			if (item instanceof Bag) bagCount++;
		}
		nRows = (int)Math.ceil((bag.capacity() - bagCount)/(float)nCols);

		int windowWidth = slotWidth * nCols + SLOT_MARGIN * (nCols - 1);

		// shrink slots to fit screen width/height
		if (PixelScene.landscape()){
			while (slotHeight >= 24 && (TITLE_HEIGHT + slotHeight * nRows + SLOT_MARGIN * (nRows-1) + 20 + chrome.marginTop()) > PixelScene.uiCamera.height){
				slotHeight--;
			}
		} else {
			while (slotWidth >= 26 && (windowWidth + chrome.marginHor()) > PixelScene.uiCamera.width){
				slotWidth--;
				windowWidth = slotWidth * nCols + SLOT_MARGIN * (nCols - 1);
			}
		}

		int fullGridHeight = slotHeight * nRows + SLOT_MARGIN * (nRows - 1);
		int maxGridHeight  = (int)(PixelScene.uiCamera.height - chrome.marginTop() - 20 - TITLE_HEIGHT);
		int visibleGridHeight = Math.min(fullGridHeight, maxGridHeight);
		int windowHeight = TITLE_HEIGHT + visibleGridHeight;

		placeTitle( bag, windowWidth );

		scrollContent = new Component();
		scrollPane = new ScrollPane( scrollContent );
		add( scrollPane );

		placeItems( bag );

		resize( windowWidth, windowHeight );

		scrollContent.setSize( windowWidth, fullGridHeight );
		scrollPane.setRect( 0, TITLE_HEIGHT, windowWidth, visibleGridHeight );

		int i = 1;
		for (Bag b : Dungeon.hero.belongings.getBags()) {
			if (b != null) {
				BagTab tab = new BagTab( b, i++ );
				add( tab );
				tab.select( b == bag );
				if  (b == bag){
					selected = tab;
				}
			}
		}

		layoutTabs();
	}

	public ItemSelector getSelector() {
		return selector;
	}

	public static WndBag lastBag(ItemSelector selector ) {
		
		if (lastBag != null && Dungeon.hero.belongings.backpack.contains( lastBag )) {
			
			return new WndBag( lastBag, selector );
			
		} else {
			
			return new WndBag( Dungeon.hero.belongings.backpack, selector );
			
		}
	}

	public static WndBag getBag( ItemSelector selector ) {
		if (selector.preferredBag() == Belongings.Backpack.class){
			return new WndBag( Dungeon.hero.belongings.backpack, selector );

		} else if (selector.preferredBag() != null){
			Bag bag = Dungeon.hero.belongings.getItem( selector.preferredBag() );
			if (bag != null)    return new WndBag( bag, selector );
			//if a specific preferred bag isn't present, then the relevant items will be in backpack
			else                return new WndBag( Dungeon.hero.belongings.backpack, selector );
		}

		return lastBag( selector );
	}
	
	protected void placeTitle( Bag bag, int width ){

		float titleWidth;
		//left edge of the gold readout, which the bullet readout is placed beside
		float goldLeft, goldRowY;
		if (Dungeon.energy == 0) {
			ItemSprite gold = new ItemSprite(ItemSpriteSheet.GOLD, null);
			gold.x = width - gold.width();
			gold.y = (TITLE_HEIGHT - gold.height()) / 2f;
			PixelScene.align(gold);
			add(gold);

			BitmapText amt = new BitmapText(Integer.toString(Dungeon.gold), PixelScene.pixelFont);
			amt.hardlight(TITLE_COLOR);
			amt.measure();
			amt.x = width - gold.width() - amt.width() - 1;
			amt.y = (TITLE_HEIGHT - amt.baseLine()) / 2f - 1;
			PixelScene.align(amt);
			add(amt);

			titleWidth = amt.x;
			goldLeft = amt.x;
			goldRowY = amt.y;
		} else {

			Image gold = Icons.get(Icons.COIN_SML);
			gold.x = width - gold.width() - 0.5f;
			gold.y = 0;
			PixelScene.align(gold);
			add(gold);

			BitmapText amt = new BitmapText(Integer.toString(Dungeon.gold), PixelScene.pixelFont);
			amt.hardlight(TITLE_COLOR);
			amt.measure();
			amt.x = width - gold.width() - amt.width() - 2f;
			amt.y = 0;
			PixelScene.align(amt);
			add(amt);

			titleWidth = amt.x;
			goldLeft = amt.x;
			goldRowY = amt.y;

			Image energy = Icons.get(Icons.ENERGY_SML);
			energy.x = width - energy.width();
			energy.y = gold.height();
			PixelScene.align(energy);
			add(energy);

			amt = new BitmapText(Integer.toString(Dungeon.energy), PixelScene.pixelFont);
			amt.hardlight(0x44CCFF);
			amt.measure();
			amt.x = width - energy.width() - amt.width() - 1;
			amt.y = energy.y;
			PixelScene.align(amt);
			add(amt);

			titleWidth = Math.min(titleWidth, amt.x);
		}

		if (Dungeon.bullet > 0) {
			Image bullet = Icons.get(Icons.BULLET_SML);
			bullet.x = goldLeft - bullet.width() - 1.5f;
			bullet.y = goldRowY;
			PixelScene.align(bullet);
			add(bullet);

			BitmapText amt = new BitmapText(Integer.toString(Dungeon.bullet), PixelScene.pixelFont);
			amt.hardlight(0xFFFFFF);
			amt.measure();
			amt.x = bullet.x - amt.width() - 1;
			amt.y = bullet.y;
			PixelScene.align(amt);
			add(amt);

			titleWidth = Math.min(titleWidth, amt.x);
		}

		String title = selector != null ? selector.textPrompt() : null;
		RenderedTextBlock txtTitle = PixelScene.renderTextBlock(
				title != null ? Messages.titleCase(title) : Messages.titleCase( bag.name() ), 8 );
		txtTitle.hardlight( TITLE_COLOR );
		txtTitle.maxWidth( (int)titleWidth - 2 );
		txtTitle.setPos(
				1,
				(TITLE_HEIGHT - txtTitle.height()) / 2f - 1
		);
		PixelScene.align(txtTitle);
		add( txtTitle );
	}
	
	protected void placeItems( Bag container ) {

		// --- Equipped items (always shown at the top) ---
		Belongings stuff = Dungeon.hero.belongings;
		placeItem( stuff.weapon != null ? stuff.weapon : new Placeholder( ItemSpriteSheet.WEAPON_HOLDER ) );
		placeItem( stuff.armor != null ? stuff.armor : new Placeholder( ItemSpriteSheet.ARMOR_HOLDER ) );
		placeItem( stuff.artifact != null ? stuff.artifact : new Placeholder( ItemSpriteSheet.ARTIFACT_HOLDER ) );
		placeItem( stuff.misc != null ? stuff.misc : new Placeholder( ItemSpriteSheet.SOMETHING ) );
		placeItem( stuff.ring != null ? stuff.ring : new Placeholder( ItemSpriteSheet.RING_HOLDER ) );

		int equipped = 5;

		// When viewing a sub-bag, show the bag itself as the first item
		if (container != Dungeon.hero.belongings.backpack){
			placeItem(container);
			count--; // don't count, it's not actually inside of itself
		} else if (stuff.secondWep != null) {
			// Champion subclass: second weapon goes to front of backpack view
			placeItem(stuff.secondWep);
			equipped++;
		}

		// --- Bag contents ---
		// IMPORTANT: Bags (FoodPouch, VelvetPouch, etc.) are completely excluded
		// from the grid. They appear as tab buttons at the bottom of the window.
		// Do NOT increment 'count' for bags — that would steal empty grey squares
		// from the fill loop below, creating phantom unusable gaps in the grid.
		for (Item item : container.items.toArray(new Item[0])) {
			if (!(item instanceof Bag)) {
				placeItem( item );
			}
		}

		// --- Fill remaining grid with empty slots (grey squares) ---
		// 'count' tracks how many placeItem() calls were made (via count++ in placeItem).
		// Fill until the full grid (nRows * nCols) is covered so there are no gaps.
		while (count < nRows * nCols) {
			placeItem( null );
		}
	}
	
	protected void placeItem( final Item item ) {

		count++;

		int x = col * (slotWidth + SLOT_MARGIN);
		int y = row * (slotHeight + SLOT_MARGIN);

		InventorySlot slot = new InventorySlot( item ){
			@Override
			protected void onClick() {
				if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isSpectator()) return;
				if (lastBag != item && !lastBag.contains(item) && !item.isEquipped(Dungeon.hero)){

					hide();

				} else if (selector != null) {

					if (selector.hideAfterSelecting()){
						hide();
					}
					selector.onSelect( item );

				} else {

					Game.scene().addToFront(new WndUseItem( WndBag.this, item ) );

				}
			}

			@Override
			protected void onRightClick() {
				if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isSpectator()) return;
				if (lastBag != item && !lastBag.contains(item) && !item.isEquipped(Dungeon.hero)){

					hide();

				} else if (selector != null) {

					if (selector.hideAfterSelecting()){
						hide();
					}
					selector.onSelect( item );

				} else {

					RightClickMenu r = new RightClickMenu(item){
						@Override
						public void onSelect(int index) {
							WndBag.this.hide();
						}
					};
					parent.addToFront(r);
					r.camera = camera();
					PointF mousePos = PointerEvent.currentHoverPos();
					mousePos = camera.screenToCamera((int)mousePos.x, (int)mousePos.y);
					r.setPos(mousePos.x-3, mousePos.y-3);

				}
			}

			@Override
			protected boolean onLongClick() {
				if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isSpectator()) return false;
				if (selector == null && item.defaultAction() != null) {
					hide();
					QuickSlotButton.set( item );
					return true;
				} else if (selector != null) {
					Game.scene().addToFront(new WndInfoItem(item));
					return true;
				} else {
					return false;
				}
			}
		};
		slot.setRect( x, y, slotWidth, slotHeight );
		scrollContent.add(slot);

		if (item == null || (selector != null && !selector.itemSelectable(item))){
			slot.enable(false);
		}
		
		if (++col >= nCols) {
			col = 0;
			row++;
		}

	}

	@Override
	public boolean onSignal(KeyEvent event) {
		if (event.pressed && KeyBindings.getActionForKey( event ) == WPDAction.INVENTORY) {
			onBackPressed();
			return true;
		} else {
			return super.onSignal(event);
		}
	}
	
	@Override
	public void onBackPressed() {
		if (selector != null) {
			selector.onSelect( null );
		}
		super.onBackPressed();
	}
	
	@Override
	protected void onClick( Tab tab ) {
		hide();
		Window w = new WndBag(((BagTab) tab).bag, selector);
		if (Game.scene() instanceof GameScene){
			GameScene.show(w);
		} else {
			Game.scene().addToFront(w);
		}
	}
	
	@Override
	public void hide() {
		super.hide();
		if (INSTANCE == this){
			INSTANCE = null;
		}
	}
	
	@Override
	protected int tabHeight() {
		return 20;
	}
	
	private Image icon( Bag bag ) {
		if (bag instanceof VelvetPouch) {
			return Icons.get( Icons.SEED_POUCH );
		} else if (bag instanceof ScrollHolder) {
			return Icons.get( Icons.SCROLL_HOLDER );
		} else if (bag instanceof MagicalHolster) {
			return Icons.get( Icons.WAND_HOLSTER );
		} else if (bag instanceof PotionBandolier) {
			return Icons.get( Icons.POTION_BANDOLIER );
		} else if (bag instanceof FoodPouch) {
			return Icons.get( Icons.FOOD_POUCH );
		} else if (bag instanceof AnkhChain) {
			return Icons.get( Icons.ANKH_CHAIN );
		} else if (bag instanceof KeyRing) {
			return Icons.get( Icons.KEYRING );
		} else {
			return Icons.get( Icons.BACKPACK );
		}
	}
	
	private class BagTab extends IconTab {

		private Bag bag;
		private int index;
		
		public BagTab( Bag bag, int index ) {
			super( icon(bag) );
			
			this.bag = bag;
			this.index = index;
		}

		@Override
		public GameAction keyAction() {
			switch (index){
				case 1: default:
					return WPDAction.BAG_1;
				case 2:
					return WPDAction.BAG_2;
				case 3:
					return WPDAction.BAG_3;
				case 4:
					return WPDAction.BAG_4;
				case 5:
					return WPDAction.BAG_5;
				case 6:
					return WPDAction.BAG_6;
				case 7:
					return WPDAction.BAG_7;
				case 8:
					return WPDAction.BAG_8;
			}
		}

		@Override
		protected String hoverText() {
			return Messages.titleCase(bag.name());
		}
	}
	
	public static class Placeholder extends Item {

		public Placeholder(int image ) {
			this.image = image;
		}

		@Override
		public String name() {
			return null;
		}

		@Override
		public boolean isIdentified() {
			return true;
		}
		
		@Override
		public boolean isEquipped( Hero hero ) {
			return true;
		}
	}

	public abstract static class ItemSelector {
		public abstract String textPrompt();
		public Class<?extends Bag> preferredBag(){
			return null; //defaults to last bag opened
		}
		public boolean hideAfterSelecting(){
			return true; //defaults to hiding the window when an item is picked
		}
		public abstract boolean itemSelectable( Item item );
		public abstract void onSelect( Item item );
	}
}
