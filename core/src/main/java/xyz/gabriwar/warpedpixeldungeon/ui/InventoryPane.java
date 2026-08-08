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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDAction;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
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
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndInfoItem;
import xyz.gabriwar.warpedpixeldungeon.windows.WndUseItem;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.GameAction;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PointF;
import com.watabou.utils.Signal;

import java.util.ArrayList;

public class InventoryPane extends Component {

	private NinePatch bg;
	private NinePatch bg2; //2 backgrounds to reduce transparency

	//used to prevent clicks through the BG normally, or to cancel selectors if they're enabled
	private PointerArea blocker;

	private Signal.Listener<KeyEvent> keyBlocker;

	private static InventoryPane instance;

	private ArrayList<InventorySlot> equipped;
	private ArrayList<InventorySlot> bagItems;

	private Image gold;
	private BitmapText goldTxt;
	private Image energy;
	private BitmapText energyTxt;
	private Image bullet;
	private BitmapText bulletTxt;
	private RenderedTextBlock promptTxt;

	private ArrayList<BagButton> bags;

	public static final int WIDTH = 187;
	public static final int HEIGHT = 82;

	// Total slot buttons pre-created. Must be >= Backpack.capacity() at max bags.
	// Currently: 43 base (Bag.capacity) + up to 7 bags = 50.
	// If you increase Bag.capacity(), increase this too.
	public static final int MAX_BAG_ITEMS = 50;

	private static final int ITEMS_PER_ROW = 10;
	private static final int ITEMS_PER_ROW_EXPANDED = 8;

	private static final int SLOT_WIDTH = 17;
	private static final int SLOT_HEIGHT = 24;

	private WndBag.ItemSelector selector;
	private boolean expanded = false;

	public static Bag lastBag;

	private boolean lastEnabled = true;
	private int lastCapacity = -1;

	private static Image crossB;
	private static Image crossM;

	private static boolean targeting = false;
	private static InventorySlot targetingSlot = null;
	public static Char lastTarget = null;

	public InventoryPane(){
		super();
		instance = this;
	}

	@Override
	public synchronized void destroy() {
		KeyEvent.removeKeyListener(keyBlocker);
		super.destroy();
		if (instance == this) instance = null;
	}

	@Override
	protected void createChildren() {

		bg = Chrome.get(Chrome.Type.TOAST_TR_HEAVY);
		add(bg);

		blocker = new PointerArea(0, 0, PixelScene.uiCamera.width, PixelScene.uiCamera.height){
			@Override
			protected void onClick(PointerEvent event) {
				if (selector != null && !bg.overlapsScreenPoint((int)event.current.x, (int)event.current.y)){
					//any windows opened as a consequence of this should be centered on the inventory
					GameScene.centerNextWndOnInvPane();
					selector.onSelect(null);
					selector = null;
					updateInventory();
				}
			}
		};
		blocker.target = bg; //targets bg when there is no selector, otherwise targets itself
		add (blocker);

		keyBlocker = new Signal.Listener<KeyEvent>(){
			@Override
			public boolean onSignal(KeyEvent keyEvent) {
				if (keyEvent.pressed && isSelecting() && InventoryPane.this.visible
						&& KeyBindings.getActionForKey(keyEvent) != WPDAction.BAG_1
						&& KeyBindings.getActionForKey(keyEvent) != WPDAction.BAG_2
						&& KeyBindings.getActionForKey(keyEvent) != WPDAction.BAG_3
						&& KeyBindings.getActionForKey(keyEvent) != WPDAction.BAG_4
						&& KeyBindings.getActionForKey(keyEvent) != WPDAction.BAG_5
						&& KeyBindings.getActionForKey(keyEvent) != WPDAction.BAG_6
						&& KeyBindings.getActionForKey(keyEvent) != WPDAction.BAG_7
						&& KeyBindings.getActionForKey(keyEvent) != WPDAction.BAG_8){
					//any windows opened as a consequence of this should be centered on the inventory
					GameScene.centerNextWndOnInvPane();
					selector.onSelect(null);
					selector = null;
					updateInventory();
					return true;
				}
				return false;
			}
		};

		equipped = new ArrayList<>();
		for (int i = 0; i < 5; i++){
			InventorySlot btn = new InventoryPaneSlot(null);
			equipped.add(btn);
			add(btn);
		}

		gold = Icons.get(Icons.COIN_SML);
		add(gold);
		goldTxt = new BitmapText(PixelScene.pixelFont);
		goldTxt.hardlight(Window.TITLE_COLOR);
		add(goldTxt);

		energy = Icons.get(Icons.ENERGY_SML);
		add(energy);
		energyTxt = new BitmapText(PixelScene.pixelFont);
		energyTxt.hardlight(0x44CCFF);
		add(energyTxt);

		bullet = Icons.get(Icons.BULLET_SML);
		add(bullet);
		bulletTxt = new BitmapText(PixelScene.pixelFont);
		bulletTxt.hardlight(0xFFFFFF);
		add(bulletTxt);

		promptTxt = PixelScene.renderTextBlock(6);
		promptTxt.hardlight(Window.TITLE_COLOR);
		add(promptTxt);

		bagItems = new ArrayList<>();
		for (int i = 0; i < MAX_BAG_ITEMS; i++){
			InventorySlot btn = new InventoryPaneSlot(null);
			bagItems.add(btn);
			add(btn);
		}

		bags = new ArrayList<>();
		for (int i = 0; i < 5; i++){
			BagButton btn = new BagButton(null, i+1);
			bags.add(btn);
			add(btn);
		}

		crossB = Icons.TARGET.get();
		crossB.visible = false;
		add( crossB );

		crossM = new Image();
		crossM.copy( crossB );

		lastEnabled = true;
		updateInventory();

		width = WIDTH;
		height = HEIGHT;
	}

	@Override
	protected void layout() {
		width = WIDTH;
		int itemsPerRow = expanded ? ITEMS_PER_ROW_EXPANDED : ITEMS_PER_ROW;
		// Hardcoded to match Bag.capacity() (43). Do NOT use lastBag.capacity() here
		// because Backpack.capacity() returns 43 + numBags, and bags are hidden in the
		// grid (shown as tab buttons). Using capacity() would show extra unusable slots.
		// If you change Bag.capacity(), change this too.
		int shownItems = 43;
		int rows = (int)Math.ceil((float)shownItems / itemsPerRow);
		height = 32 + rows * (SLOT_HEIGHT + 1);

		bg.x = x;
		bg.y = y;
		bg.size(width, height);

		float left = x+4;
		for (InventorySlot i : equipped){
			i.setRect(left, y+4, SLOT_WIDTH, SLOT_HEIGHT);
			left = i.right()+1;
		}

		promptTxt.maxWidth((int) (width - (left - x) - bg.marginRight()));
		if (promptTxt.height() > 10){
			promptTxt.setPos(left, y + 2 + (12 - promptTxt.height()) / 2);
		} else {
			promptTxt.setPos(left, y + 4 + (10 - promptTxt.height()) / 2);
		}

		goldTxt.x = left;
		goldTxt.y = y+5.5f;
		PixelScene.align(goldTxt);

		gold.x = goldTxt.x + goldTxt.width() + 1;
		gold.y = goldTxt.y;

		energyTxt.x = gold.x + gold.width() + 2;
		energyTxt.y = y+5.5f;
		PixelScene.align(energyTxt);

		energy.x = energyTxt.x + energyTxt.width() + 1;
		energy.y = energyTxt.y;

		bulletTxt.x = energy.x + energy.width() + 2;
		bulletTxt.y = y+5.5f;
		PixelScene.align(bulletTxt);

		bullet.x = bulletTxt.x + bulletTxt.width() + 1;
		bullet.y = bulletTxt.y;

		for (BagButton b : bags){
			b.setRect(left, y + 14, SLOT_WIDTH, 14);
			left = b.right()+1;
		}

		left = x+4;
		float top = y+4+SLOT_HEIGHT+1;
		int col = 0;
		for (int i = 0; i < bagItems.size(); i++){
			InventorySlot b = bagItems.get(i);
			if (i < shownItems){
				b.visible = true;
				b.setRect(left, top, SLOT_WIDTH, SLOT_HEIGHT);
				left = b.right()+1;
				col++;
				if (col >= itemsPerRow){
					col = 0;
					left = x+4;
					top += SLOT_HEIGHT+1;
				}
			} else {
				b.visible = b.active = false;
			}
		}

		super.layout();
	}
	
	public void alpha( float value ){
		bg.alpha( value );
		
		for (InventorySlot slot : equipped){
			slot.alpha( value );
		}
		for (InventorySlot slot : bagItems){
			slot.alpha( value );
		}
		
		gold.alpha(value);
		goldTxt.alpha(value);
		energy.alpha(value);
		energyTxt.alpha(value);
		bullet.alpha(value);
		bulletTxt.alpha(value);

		for (BagButton bag : bags){
			bag.alpha( value );
		}
	}

	public static void refresh(){
		if (instance != null) instance.updateInventory();
	}

	public void updateInventory(){
		if (selector == null){
			blocker.target = bg;
			KeyEvent.removeKeyListener(keyBlocker);
		} else {
			blocker.target = blocker;
			KeyEvent.addKeyListener(keyBlocker);
		}

		Belongings stuff = Dungeon.hero.belongings;

		if (lastBag == null || !stuff.getBags().contains(lastBag)){
			lastBag = stuff.backpack;
		}

		equipped.get(0).item(stuff.weapon == null ? new WndBag.Placeholder( ItemSpriteSheet.WEAPON_HOLDER ) : stuff.weapon);
		equipped.get(1).item(stuff.armor == null ? new WndBag.Placeholder( ItemSpriteSheet.ARMOR_HOLDER ) : stuff.armor);
		equipped.get(2).item(stuff.artifact == null ? new WndBag.Placeholder( ItemSpriteSheet.ARTIFACT_HOLDER ) : stuff.artifact);
		equipped.get(3).item(stuff.misc == null ? new WndBag.Placeholder( ItemSpriteSheet.SOMETHING ) : stuff.misc);
		equipped.get(4).item(stuff.ring == null ? new WndBag.Placeholder( ItemSpriteSheet.RING_HOLDER ) : stuff.ring);

		ArrayList<Item> items = (ArrayList<Item>) lastBag.items.clone();

		if (lastBag == stuff.backpack && stuff.secondWep != null){
			items.add(0, stuff.secondWep);
		}

		// Assign items to slot buttons, skipping bags (they're shown as tab buttons).
		// j = index into the items list, i = slot button index.
		// When a bag is found: j advances past it, i rewinds (i--) so the next
		// non-bag item takes the same slot position. This means bags never occupy
		// a visible slot — they're invisible in the grid.
		int j = 0;
		for (int i = 0; i < MAX_BAG_ITEMS; i++){
			// When viewing a sub-bag (not backpack), slot 0 shows the bag itself
			if (i == 0 && lastBag != stuff.backpack){
				bagItems.get(i).item(lastBag);
				continue;
			}
			if (items.size() > j){
				if (items.get(j) instanceof Bag){
					j++;  // skip the bag in the items list
					i--;  // rewind slot index so next item takes this position
					continue;
				}
				bagItems.get(i).item(items.get(j));
				j++;
			} else {
				// Empty slot — shows as grey square with no item
				bagItems.get(i).item(null);
			}
		}

		if (selector == null) {
			promptTxt.visible = false;

			goldTxt.text(Integer.toString(Dungeon.gold));
			goldTxt.measure();
			goldTxt.visible = gold.visible = true;

			energyTxt.text(Integer.toString(Dungeon.energy));
			energyTxt.measure();
			energyTxt.visible = energy.visible = Dungeon.energy > 0;

			bulletTxt.text(Integer.toString(Dungeon.bullet));
			bulletTxt.measure();
			bulletTxt.visible = bullet.visible = Dungeon.bullet > 0;
		} else {
			promptTxt.text(selector.textPrompt());
			promptTxt.visible = true;

			goldTxt.visible = gold.visible = false;
			energyTxt.visible = energy.visible = false;
			bulletTxt.visible = bullet.visible = false;
		}

		ArrayList<Bag> inventBags = stuff.getBags();
		for (int i = 0; i < bags.size(); i++){
			if (inventBags.size() > i){
				bags.get(i).bag(inventBags.get(i));
			} else {
				bags.get(i).bag(null);
			}
		}

		boolean lostInvent = Dungeon.hero.belongings.lostInventory();
		for (InventorySlot b : equipped){
			b.enable(lastEnabled
					&& !(b.item() instanceof WndBag.Placeholder)
					&& (selector == null || selector.itemSelectable(b.item()))
					&& (!lostInvent || b.item().keptThroughLostInventory()));
		}
		for (InventorySlot b : bagItems){
			b.enable(lastEnabled
					&& b.item() != null
					&& (selector == null || selector.itemSelectable(b.item()))
					&& (!lostInvent || b.item().keptThroughLostInventory()));
		}
		for (BagButton b : bags){
			b.enable(lastEnabled);
		}

		goldTxt.alpha( lastEnabled ? 1f : 0.3f );
		gold.alpha( lastEnabled ? 1f : 0.3f );
		energyTxt.alpha( lastEnabled ? 1f : 0.3f );
		energy.alpha( lastEnabled ? 1f : 0.3f );

		layout();
		GameScene.repositionInv();
	}

	public void setExpanded(boolean expanded){
		this.expanded = expanded;
		updateInventory();
	}

	public void setSelector(WndBag.ItemSelector selector){
		this.selector = selector;
		if (selector.preferredBag() == Belongings.Backpack.class){
			lastBag = Dungeon.hero.belongings.backpack;
		} else if (selector.preferredBag() != null) {
			Bag preferred = Dungeon.hero.belongings.getItem(selector.preferredBag());
			if (preferred != null)  lastBag = preferred;
			//if a specific preferred bag isn't present, then the relevant items will be in backpack
			else                    lastBag = Dungeon.hero.belongings.backpack;
		}
		updateInventory();
	}

	public WndBag.ItemSelector getSelector() {
		return selector;
	}

	public boolean isSelecting(){
		return selector != null;
	}

	public static void clearTargetingSlot(){
		targetingSlot = null;
	}

	public static void useTargeting(){
		if (instance != null &&
				instance.visible &&
				lastTarget != null &&
				targetingSlot != null &&
				Actor.chars().contains( lastTarget ) &&
				lastTarget.isAlive() &&
				lastTarget.alignment != Char.Alignment.ALLY &&
				Dungeon.level.heroFOV[lastTarget.pos]) {

			targeting = true;
			CharSprite sprite = lastTarget.sprite;

			if (sprite.parent != null) {
				sprite.parent.addToFront(crossM);
				crossM.point(sprite.center(crossM));
			}

			crossB.point(targetingSlot.sprite.center(crossB));
			crossB.visible = true;

		} else {

			lastTarget = null;
			targeting = false;

		}
	}

	public static void cancelTargeting(){
		if (targeting){
			crossB.visible = false;
			crossM.remove();
			targeting = false;
		}
	}

	@Override
	public synchronized void update() {
		super.update();

		if (lastBag != null && lastBag.capacity() != lastCapacity) {
			lastCapacity = lastBag.capacity();
			updateInventory();
		}

		if (lastEnabled != (Dungeon.hero.ready || !Dungeon.hero.isAlive())) {
			lastEnabled = (Dungeon.hero.ready || !Dungeon.hero.isAlive());

			boolean lostInvent = Dungeon.hero.belongings.lostInventory();
			for (InventorySlot b : equipped){
				b.enable(lastEnabled
						&& !(b.item() instanceof WndBag.Placeholder)
						&& (selector == null || selector.itemSelectable(b.item()))
						&& (!lostInvent || b.item().keptThroughLostInventory()));
			}
			for (InventorySlot b : bagItems){
				b.enable(lastEnabled
						&& b.item() != null
						&& (selector == null || selector.itemSelectable(b.item()))
						&& (!lostInvent || b.item().keptThroughLostInventory()));
			}
			for (BagButton b : bags){
				b.enable(lastEnabled);
			}

			goldTxt.alpha( lastEnabled ? 1f : 0.3f );
			gold.alpha( lastEnabled ? 1f : 0.3f );
			energyTxt.alpha( lastEnabled ? 1f : 0.3f );
			energy.alpha( lastEnabled ? 1f : 0.3f );
		}

	}

	private Image bagIcon(Bag bag ) {
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

	private class InventoryPaneSlot extends InventorySlot {

		private InventoryPaneSlot( Item item ){
			super(item);
		}

		@Override
		protected void onClick() {
			if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isSpectator()) return;
			if (lastBag != item && !lastBag.contains(item) && !item.isEquipped(Dungeon.hero)){
				updateInventory();
				return;
			}

			if (targeting){
				if (targetingSlot == this){
					int cell = QuickSlotButton.autoAim(lastTarget, item());

					if (cell != -1){
						GameScene.handleCell(cell);
					} else {
						//couldn't auto-aim, just target the position and hope for the best.
						GameScene.handleCell( lastTarget.pos );
					}
					return;
				} else {
					cancelTargeting();
				}
			}

			//any windows opened as a consequence of this button should be centered on the inventory
			GameScene.centerNextWndOnInvPane();
			if (selector != null) {
				WndBag.ItemSelector activating = selector;
				selector = null;
				activating.onSelect( item );
				updateInventory();
			} else {
				targetingSlot = this;
				GameScene.show(new WndUseItem( null, item ));
			}
		}

		@Override
		protected boolean onLongClick() {
			if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isSpectator()) return false;
			if (selector == null && item.defaultAction() != null) {
				QuickSlotButton.set( item );
				return true;
			} else if (selector != null) {
				GameScene.centerNextWndOnInvPane();
				GameScene.show(new WndInfoItem(item));
				return true;
			} else {
				return false;
			}
		}

		@Override
		protected void onMiddleClick() {
			if (lastBag != item && !lastBag.contains(item) && !item.isEquipped(Dungeon.hero)){
				updateInventory();
				return;
			}

			if (!Dungeon.hero.isAlive() || !Dungeon.hero.ready){
				return;
			}

			if (targeting){
				if (targetingSlot == this){
					onClick();
				}
				return;
			}

			if (selector == null && item.defaultAction() != null){
				xyz.gabriwar.warpedpixeldungeon.items.Item.executeNetAware(item, Dungeon.hero);
				if (item != null && item.usesTargeting) {
					targetingSlot = this;
					InventoryPane.useTargeting();
				}
			} else {
				onClick();
			}
		}

		@Override
		protected void onRightClick() {
			if (lastBag != item && !lastBag.contains(item) && !item.isEquipped(Dungeon.hero)){
				updateInventory();
				return;
			}

			if (!Dungeon.hero.isAlive() || !Dungeon.hero.ready){
				return;
			}

			if (targeting){
				//do nothing
				return;
			}

			if (selector == null){
				targetingSlot = this;
				RightClickMenu r = new RightClickMenu(item);
				parent.addToFront(r);
				r.camera = camera();
				PointF mousePos = PointerEvent.currentHoverPos();
				mousePos = camera.screenToCamera((int)mousePos.x, (int)mousePos.y);
				r.setPos(mousePos.x-3, mousePos.y-3);
			} else {
				//do nothing
			}
		}
	}

	private class BagButton extends IconButton {

		private static final int ACTIVE		= 0x9953564D;
		private static final int INACTIVE	= 0x9942443D;

		private ColorBlock bgTop;
		private ColorBlock bgBottom;

		private Bag bag;
		private int index;

		public BagButton( Bag bag, int index ){
			super( bagIcon(bag) );
			this.bag = bag;
			this.index = index;
			visible = active = bag != null;
		}

		public void bag( Bag bag ){
			this.bag = bag;
			icon(bagIcon(bag));
			visible = active = bag != null;

			if (lastBag == bag){
				bgTop.texture(TextureCache.createSolid(ACTIVE));
				bgBottom.texture(TextureCache.createSolid(ACTIVE));
			} else {
				bgTop.texture(TextureCache.createSolid(INACTIVE));
				bgBottom.texture(TextureCache.createSolid(INACTIVE));
			}
		}

		@Override
		protected void createChildren() {
			super.createChildren();

			bgTop = new ColorBlock(1, 1, ACTIVE);
			add(bgTop);

			bgBottom = new ColorBlock(1, 1, ACTIVE);
			add(bgBottom);
		}

		@Override
		protected void layout() {
			super.layout();

			bgTop.size(width-2, 1);
			bgTop.y = y;
			bgTop.x = x+1;

			bgBottom.size(width, height-1);
			bgBottom.y = y+1;
			bgBottom.x = x;
		}
		
		public void alpha( float value ){
			bgTop.alpha(value);
			bgBottom.alpha(value);
			icon.alpha(value);
		}

		@Override
		protected void onClick() {
			super.onClick();
			GameScene.cancel();
			lastBag = bag;
			refresh();
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
		public GameAction secondaryTooltipAction() {
			return WPDAction.INVENTORY_SELECTOR;
		}

		@Override
		protected String hoverText() {
			if (bag != null) {
				return Messages.titleCase(bag.name());
			} else {
				return null;
			}
		}
	}

}
