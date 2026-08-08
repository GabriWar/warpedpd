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

import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
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

	private float addDropRow(Mob.DropInfo drop, int width, float y) {
		ItemSprite sprite;
		if (drop.item != null) {
			sprite = new ItemSprite(drop.item);
		} else {
			sprite = new ItemSprite(ItemSpriteSheet.SOMETHING, null);
		}

		boolean nameRevealed = drop.slotSeen
				&& (drop.isCategory
					|| drop.item == null
					|| !Catalog.isTracked(drop.item.getClass())
					|| Catalog.isSeen(drop.item.getClass()));

		String label;
		if (!drop.slotSeen) {
			sprite.lightness(0f);
			label = "???";
		} else if (nameRevealed) {
			label = drop.name + " (" + Mob.formatChance(drop.chance) + ")";
		} else {
			label = "??? (" + Mob.formatChance(drop.chance) + ")";
		}

		sprite.x = 1;
		sprite.y = y;
		add(sprite);

		RenderedTextBlock text = PixelScene.renderTextBlock(label, 6);
		text.maxWidth(width - 20);
		text.setPos(20, y + (ItemSpriteSheet.SIZE - text.height()) / 2f);
		add(text);

		float rowBottom = Math.max(sprite.y + ItemSpriteSheet.SIZE, text.bottom()) + 1;

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

		boolean catalogSeen = drop.isCategory
				|| !Catalog.isTracked(drop.item.getClass())
				|| Catalog.isSeen(drop.item.getClass());

		Image icon;
		String title;
		String desc;

		if (catalogSeen) {
			icon = new ItemSprite(drop.item);
			title = Messages.titleCase(drop.item.name());
			desc = drop.item.info();
		} else {
			icon = new ItemSprite(drop.item);
			icon.lightness(0f);
			title = "???";
			desc = Messages.get(WndJournal.CatalogTab.class, "not_seen_item");
		}

		if (WarpedPixelDungeon.scene() instanceof GameScene) {
			GameScene.show(new WndTitledMessage(icon, title, desc));
		} else {
			WarpedPixelDungeon.scene().addToFront(new WndTitledMessage(icon, title, desc));
		}
	}

}
