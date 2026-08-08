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

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

//Scrollable guide-page window: icon + title, long text, optional drops list.
public class WndGuideNode extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 200;
	private static final int GAP = 2;

	public WndGuideNode( Image icon, String title, String text ){
		this( icon, title, text, null );
	}

	public WndGuideNode( Image icon, String title, String text, ArrayList<Mob.DropInfo> drops ){
		super();

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		IconTitle titlebar = new IconTitle( icon, title );
		titlebar.setRect( 0, 0, width, 0 );
		add( titlebar );

		Component content = new Component();

		RenderedTextBlock body = PixelScene.renderTextBlock( 6 );
		body.text( text, width );
		body.setPos( 0, 0 );
		content.add( body );

		float y = body.bottom() + 2;

		if (drops != null && !drops.isEmpty()){
			RenderedTextBlock header = PixelScene.renderTextBlock(
					"_" + Messages.get(Mob.class, "drops_header") + "_", 6);
			header.setHightlighting(true);
			header.maxWidth(width);
			header.setPos(0, y + GAP);
			content.add(header);
			y = header.bottom() + 2;

			for (Mob.DropInfo drop : drops) {
				y = addDropRow(content, drop, width, y);
			}
		}

		content.setSize( width, y );

		ScrollPane pane = new ScrollPane( content );
		add( pane );

		float maxHeight = PixelScene.uiCamera.height * 0.85f - titlebar.bottom() - 2*GAP;
		float paneHeight = Math.min( maxHeight, y );

		resize( width, (int)(titlebar.bottom() + 2*GAP + paneHeight) );
		pane.setRect( 0, titlebar.bottom() + 2*GAP, width, paneHeight );
	}

	private float addDropRow( Component content, Mob.DropInfo drop, int width, float y ){
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
		content.add(sprite);

		RenderedTextBlock text = PixelScene.renderTextBlock(label, 6);
		text.maxWidth(width - 20);
		text.setPos(20, y + (ItemSpriteSheet.SIZE - text.height()) / 2f);
		content.add(text);

		float rowBottom = Math.max(sprite.y + ItemSpriteSheet.SIZE, text.bottom()) + 1;

		if (drop.slotSeen && drop.item != null) {
			final Mob.DropInfo fDrop = drop;
			PointerArea hotArea = new PointerArea(0, y, width, rowBottom - y) {
				@Override
				protected void onClick(PointerEvent event) {
					boolean catalogSeen = fDrop.isCategory
							|| !Catalog.isTracked(fDrop.item.getClass())
							|| Catalog.isSeen(fDrop.item.getClass());

					Image ic = new ItemSprite(fDrop.item);
					String t;
					String d;
					if (catalogSeen) {
						t = Messages.titleCase(fDrop.item.name());
						d = fDrop.item.info();
					} else {
						ic.lightness(0f);
						t = "???";
						d = Messages.get(WndJournal.CatalogTab.class, "not_seen_item");
					}
					xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon.scene().addToFront(
							new WndTitledMessage(ic, t, d));
				}
			};
			content.add(hotArea);
		}

		return rowBottom;
	}
}
