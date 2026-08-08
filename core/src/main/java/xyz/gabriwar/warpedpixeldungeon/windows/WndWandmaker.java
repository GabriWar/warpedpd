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

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Wandmaker;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.ItemButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

public class WndWandmaker extends Window {

	private static final int WIDTH		= 120;
	private static final int BTN_SIZE	= 32;
	private static final int BTN_GAP	= 5;
	private static final int GAP		= 2;

	private final Wandmaker wandmaker;
	private final Hero hero;
	private final int dialogId;       // -1 = local (host); >=0 = remote (client)

	// Local (host's own hero): real wands from quest progress, applied on confirm.
	public WndWandmaker( final Wandmaker wandmaker, final Hero hero ) {
		this( wandmaker, hero, -1, Wandmaker.Quest.type(),
				Wandmaker.Quest.progressFor( hero.id() ) == null ? null : Wandmaker.Quest.progressFor( hero.id() ).wand1,
				Wandmaker.Quest.progressFor( hero.id() ) == null ? null : Wandmaker.Quest.progressFor( hero.id() ).wand2 );
	}

	// Remote (client): display wands from the host payload; confirm ships the choice.
	public WndWandmaker( int dialogId, int type, Item wand1, Item wand2 ) {
		this( null, null, dialogId, type, wand1, wand2 );
	}

	private WndWandmaker( final Wandmaker wandmaker, final Hero hero, final int dialogId,
						  final int type, final Item w1, final Item w2 ) {

		super();

		this.wandmaker = wandmaker;
		this.hero = hero;
		this.dialogId = dialogId;

		String key;
		switch (type) {
			case 2:  key = "ember"; break;
			case 3:  key = "berry"; break;
			default: key = "dust";  break;
		}

		IconTitle titlebar = new IconTitle();
		titlebar.icon(new ItemSprite(w1 != null ? w1.image() : 0, null));
		titlebar.label(Messages.titleCase(Messages.get(Wandmaker.class, "name")));
		titlebar.setRect(0, 0, WIDTH, 0);
		add( titlebar );

		RenderedTextBlock message = PixelScene.renderTextBlock( Messages.get(this, key), 6 );
		message.maxWidth(WIDTH);
		message.setPos(0, titlebar.bottom() + GAP);
		add( message );

		ItemButton btnWand1 = new ItemButton(){
			@Override
			protected void onClick() {
				if (item() != null) GameScene.show(new RewardWindow(item(), true));
				else hide();
			}
		};
		btnWand1.item(w1);
		btnWand1.setRect( (WIDTH - BTN_GAP) / 2 - BTN_SIZE, message.top() + message.height() + BTN_GAP, BTN_SIZE, BTN_SIZE );
		add( btnWand1 );

		ItemButton btnWand2 = new ItemButton(){
			@Override
			protected void onClick() {
				if (item() != null) GameScene.show(new RewardWindow(item(), false));
				else hide();
			}
		};
		btnWand2.item(w2);
		btnWand2.setRect( btnWand1.right() + BTN_GAP, btnWand1.top(), BTN_SIZE, BTN_SIZE );
		add(btnWand2);

		resize(WIDTH, (int) btnWand2.bottom());
	}

	private void selectReward( boolean chooseWand1 ) {
		hide();
		if (dialogId >= 0) {
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.sendDialogChoice( dialogId, chooseWand1 ? "wand1" : "wand2" );
			return;
		}
		Wandmaker.Quest.claimReward( hero, chooseWand1 );
	}

	private class RewardWindow extends WndInfoItem {

		public RewardWindow( Item item, final boolean isWand1 ) {
			super(item);

			RedButton btnConfirm = new RedButton(Messages.get(WndSadGhost.class, "confirm")){
				@Override
				protected void onClick() {
					RewardWindow.this.hide();
					selectReward( isWand1 );
				}
			};
			btnConfirm.setRect(0, height+2, width/2-1, 16);
			add(btnConfirm);

			RedButton btnCancel = new RedButton(Messages.get(WndSadGhost.class, "cancel")){
				@Override
				protected void onClick() {
					hide();
				}
			};
			btnCancel.setRect(btnConfirm.right()+2, height+2, btnConfirm.width(), 16);
			add(btnCancel);

			resize(width, (int)btnCancel.bottom());
		}
	}
}
