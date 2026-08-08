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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Ghost;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.FetidRatSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GnollTricksterSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GreatCrabSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.ItemButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

public class WndSadGhost extends Window {

	private static final int WIDTH		= 120;
	private static final int BTN_SIZE	= 32;
	private static final int BTN_GAP	= 5;
	private static final int GAP		= 2;

	Ghost ghost;
	Hero hero;
	private final int dialogId;       // -1 = local (host); >=0 = remote (client, sends choice)
	private final Item weaponItem;
	private final Item armorItem;

	// Local (host): real rewards from the hero's quest progress, applied on confirm.
	public WndSadGhost( final Ghost ghost, final Hero hero, final int type ) {
		this( ghost, hero, type, -1,
				Ghost.Quest.progressFor( hero.id() ) == null ? null : Ghost.Quest.progressFor( hero.id() ).weapon,
				Ghost.Quest.progressFor( hero.id() ) == null ? null : Ghost.Quest.progressFor( hero.id() ).armor );
	}

	// Remote (client): display items from the host payload; confirm ships the choice.
	public WndSadGhost( int dialogId, int type, Item weapon, Item armor ) {
		this( null, null, type, dialogId, weapon, armor );
	}

	private WndSadGhost( final Ghost ghost, final Hero hero, final int type,
						 final int dialogId, final Item weapon, final Item armor ) {

		super();

		this.ghost = ghost;
		this.hero = hero;
		this.dialogId = dialogId;
		this.weaponItem = weapon;
		this.armorItem = armor;

		IconTitle titlebar = new IconTitle();
		RenderedTextBlock message;
		switch (type){
			case 1:default:
				titlebar.icon( new FetidRatSprite() );
				titlebar.label( Messages.get(this, "rat_title") );
				message = PixelScene.renderTextBlock( Messages.get(this, "rat")+"\n\n"+Messages.get(this, "give_item"), 6 );
				break;
			case 2:
				titlebar.icon( new GnollTricksterSprite() );
				titlebar.label( Messages.get(this, "gnoll_title") );
				message = PixelScene.renderTextBlock( Messages.get(this, "gnoll")+"\n\n"+Messages.get(this, "give_item"), 6 );
				break;
			case 3:
				titlebar.icon( new GreatCrabSprite());
				titlebar.label( Messages.get(this, "crab_title") );
				message = PixelScene.renderTextBlock( Messages.get(this, "crab")+"\n\n"+Messages.get(this, "give_item"), 6 );
				break;

		}

		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		message.maxWidth(WIDTH);
		message.setPos(0, titlebar.bottom() + GAP);
		add( message );

		ItemButton btnWeapon = new ItemButton(){
			@Override
			protected void onClick() {
				GameScene.show(new RewardWindow(item(), true));
			}
		};
		btnWeapon.item( weaponItem );
		btnWeapon.setRect( (WIDTH - BTN_GAP) / 2 - BTN_SIZE, message.top() + message.height() + BTN_GAP, BTN_SIZE, BTN_SIZE );
		add( btnWeapon );

		ItemButton btnArmor = new ItemButton(){
			@Override
			protected void onClick() {
				GameScene.show(new RewardWindow(item(), false));
			}
		};
		btnArmor.item( armorItem );
		btnArmor.setRect( btnWeapon.right() + BTN_GAP, btnWeapon.top(), BTN_SIZE, BTN_SIZE );
		add(btnArmor);

		resize(WIDTH, (int) btnArmor.bottom() + BTN_GAP);
	}
	
	private void selectReward( boolean chooseWeapon ) {

		hide();

		if (dialogId >= 0) {
			// Client: ship the choice; the host applies it on the actor thread.
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.sendDialogChoice( dialogId, chooseWeapon ? "weapon" : "armor" );
			return;
		}

		// Host grants the real reward to the acting hero and marks their quest complete.
		// The ghost is NOT killed — in multiplayer it persists so other heroes can still
		// run the quest.
		Ghost.Quest.claimReward( hero, chooseWeapon );
		ghost.yell( Messages.get(this, "farewell") );
	}

	private class RewardWindow extends WndInfoItem {

		public RewardWindow( Item item, final boolean isWeapon ) {
			super(item);

			RedButton btnConfirm = new RedButton(Messages.get(WndSadGhost.class, "confirm")){
				@Override
				protected void onClick() {
					RewardWindow.this.hide();

					WndSadGhost.this.selectReward( isWeapon );
				}
			};
			btnConfirm.setRect(0, height+2, width/2-1, 16);
			add(btnConfirm);

			RedButton btnCancel = new RedButton(Messages.get(WndSadGhost.class, "cancel")){
				@Override
				protected void onClick() {
					RewardWindow.this.hide();
				}
			};
			btnCancel.setRect(btnConfirm.right()+2, height+2, btnConfirm.width(), 16);
			add(btnCancel);

			resize(width, (int)btnCancel.bottom());
		}
	}
}