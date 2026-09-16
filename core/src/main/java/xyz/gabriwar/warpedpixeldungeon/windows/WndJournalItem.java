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

import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;

import java.util.ArrayList;

public class WndJournalItem extends WndTitledMessage {

	public WndJournalItem(Image icon, String title, String message ) {
		this(icon, title, message, null);
	}

	public WndJournalItem(Image icon, String title, String message, ArrayList<Mob.DropInfo> drops ) {
		super( icon, title, message);

		if (drops != null && !drops.isEmpty()) {
			addDropsSection(drops);
		}

		PointerArea blocker = new PointerArea( 0, 0, PixelScene.uiCamera.width, PixelScene.uiCamera.height ) {
			@Override
			protected void onClick( PointerEvent event ) {
				onBackPressed();
			}
		};
		blocker.camera = PixelScene.uiCamera;
		add(blocker);
	}

	private void addDropsSection(ArrayList<Mob.DropInfo> drops) {
		int w = this.width;
		float y = this.height;

		RenderedTextBlock header = PixelScene.renderTextBlock(
				"_" + Messages.get(Mob.class, "drops_header") + "_", 6);
		header.setHightlighting(true);
		header.maxWidth(w);
		header.setPos(0, y);
		add(header);
		y = header.bottom() + 2;

		for (Mob.DropInfo drop : drops) {
			y = addDropRow(drop, w, y);
		}

		resize(w, (int)(y + 2));
	}

	/**
	 * Potions, scrolls and rings are told apart by a colour, a rune or a gem that is
	 * rolled anew each run, so a row that showed the real sprite next to the kind of
	 * item would hand the player the answer. Until the kind is known this run, the row
	 * shows the generic holder sprite and only says what sort of item it is.
	 */
	private static boolean concealed(Item item) {
		return item != null
				&& (item instanceof Potion || item instanceof Scroll || item instanceof Ring)
				&& !item.isIdentified();
	}

	private static ItemSprite spriteFor(Mob.DropInfo drop) {
		if (drop.item == null) {
			return new ItemSprite(ItemSpriteSheet.SOMETHING, null);
		}
		if (drop.isCategory || concealed(drop.item)) {
			int holder = drop.item instanceof Potion ? ItemSpriteSheet.POTION_HOLDER
					: drop.item instanceof Scroll ? ItemSpriteSheet.SCROLL_HOLDER
					: drop.item instanceof Ring ? ItemSpriteSheet.RING_HOLDER
					: drop.item instanceof Wand ? ItemSpriteSheet.WAND_HOLDER
					: drop.item instanceof Armor ? ItemSpriteSheet.ARMOR_HOLDER
					: drop.item instanceof MissileWeapon ? ItemSpriteSheet.MISSILE_HOLDER
					: drop.item instanceof Weapon ? ItemSpriteSheet.WEAPON_HOLDER
					: drop.item instanceof Plant.Seed ? ItemSpriteSheet.SEED_HOLDER
					: drop.item instanceof Gold ? drop.item.image
					: ItemSpriteSheet.SOMETHING;
			return new ItemSprite(holder, null);
		}
		return new ItemSprite(drop.item);
	}

	private static String labelFor(Mob.DropInfo drop) {
		//the real name of the kind; only the sprite is withheld, since the sprite is the
		//run's colour, rune or gem and that pairing is what the player has to find out
		if (concealed(drop.item)) {
			return Messages.titleCase(Messages.get(drop.item, "name"));
		}
		return drop.name;
	}

	private float addDropRow(Mob.DropInfo drop, int width, float y) {
		ItemSprite sprite = spriteFor(drop);
		//a name is withheld only while the slot itself is unseen: a key or a shard has
		//nothing to hide, and what is hidden about a potion is hidden by concealed()
		String label;
		if (!drop.slotSeen) {
			sprite.lightness(0f);
			label = "???";
		} else {
			label = labelFor(drop) + " (" + Mob.formatChance(drop.chance) + ")";
		}
		//item frames are cut to the art (a dewdrop is 10 by 10, a shard 8 by 10), so
		//the sprite is centred in the row's 16 by 16 slot rather than pinned to its corner
		sprite.x = 1 + (ItemSpriteSheet.SIZE - sprite.width()) / 2f;
		sprite.y = y + (ItemSpriteSheet.SIZE - sprite.height()) / 2f;
		PixelScene.align(sprite);
		add(sprite);
		RenderedTextBlock text = PixelScene.renderTextBlock(label, 6);
		text.maxWidth(width - 20);
		text.setPos(20, y + (ItemSpriteSheet.SIZE - text.height()) / 2f);
		PixelScene.align(text);
		add(text);
		float rowBottom = Math.max(y + ItemSpriteSheet.SIZE, text.bottom()) + 1;
		if (drop.slotSeen) {
			PointerArea hotArea = new PointerArea(0, y, width, rowBottom - y) {
				@Override
				protected void onClick(PointerEvent event) {
					showDropInfo(drop);
				}
			};
			add(hotArea);
		}
		return rowBottom;
	}

	private void showDropInfo(Mob.DropInfo drop) {
		if (drop.item == null) return;
		Image icon;
		String title;
		String desc;
		if (drop.isCategory) {
			icon = spriteFor(drop);
			title = Messages.titleCase(drop.name);
			desc = Messages.get(WndJournalItem.class, "category_desc");
		} else if (concealed(drop.item)) {
			icon = spriteFor(drop);
			title = Messages.titleCase(labelFor(drop));
			desc = Messages.get(WndJournalItem.class, "concealed_desc");
		} else {
			icon = new ItemSprite(drop.item);
			title = Messages.titleCase(drop.item.name());
			desc = drop.item.info();
		}
		if (WarpedPixelDungeon.scene() instanceof GameScene) {
			GameScene.show(new WndTitledMessage(icon, title, desc));
		} else {
			WarpedPixelDungeon.scene().addToFront(new WndTitledMessage(icon, title, desc));
		}
	}

}
