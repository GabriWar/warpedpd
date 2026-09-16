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

package xyz.gabriwar.warpedpixeldungeon.scenes;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.messages.Languages;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.ExitButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.TitleBackground;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.StyledButton;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.ChangeInfo;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.WndChanges;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.WndChangesTabbed;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.v0_1_X_Changes;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.v0_2_X_Changes;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.v0_3_X_Changes;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.v0_4_X_Changes;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.v0_5_X_Changes;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.v0_6_X_Changes;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.v0_7_X_Changes;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.v0_8_X_Changes;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.v0_9_X_Changes;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.v1_X_Changes;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.v2_X_Changes;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.v3_X_Changes;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.vWarped_Changes;
import xyz.gabriwar.warpedpixeldungeon.windows.IconTitle;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.Pixel_Dungeon_Changes;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.v4_X_Changes;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.Scene;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;

import java.util.ArrayList;

public class ChangesScene extends PixelScene {
	
	public static int changesSelected = 0;

	private NinePatch rightPanel;
	private ScrollPane rightScroll;
	private IconTitle changeTitle;
	private RenderedTextBlock changeBody;
	
	@Override
	public void create() {
		super.create();

		Music.INSTANCE.playTracks(
				new String[]{Assets.Music.THEME_1, Assets.Music.THEME_2},
				new float[]{1, 1},
				false);

		int w = Camera.main.width;
		int h = Camera.main.height;

		RectF insets = getCommonInsets();

		TitleBackground BG = new TitleBackground(w, h);
		//background added later

		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;

		IconTitle title = new IconTitle(Icons.CHANGES.get(), Messages.get(this, "title"));
		title.setSize(200, 0);
		title.setPos(
				insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (20 - title.height()) / 2f
		);
		align(title);
		add(title);

		ExitButton btnExit = new ExitButton();
		btnExit.setPos( insets.left + w - btnExit.width(), insets.top );
		add( btnExit );

		NinePatch panel = Chrome.get(Chrome.Type.TOAST);

		int pw = 135 + panel.marginLeft() + panel.marginRight() - 2;
		int ph = h - 36;

		if (h >= PixelScene.MIN_HEIGHT_FULL && w >= 300) {
			panel.size( pw, ph );
			panel.x = insets.left + (w - pw) / 2f - pw/2 - 1;
			panel.y = insets.top + 20;

			rightPanel = Chrome.get(Chrome.Type.TOAST);
			rightPanel.size( pw, ph );
			rightPanel.x = (w - pw) / 2f + pw/2 + 1;
			rightPanel.y = 20;
			add(rightPanel);

			rightScroll = new ScrollPane(new Component());
			add(rightScroll);
			rightScroll.setRect(
					rightPanel.x + rightPanel.marginLeft(),
					rightPanel.y + rightPanel.marginTop()-1,
					rightPanel.innerWidth() + 2,
					rightPanel.innerHeight() + 2);
			rightScroll.scrollTo(0, 0);

			changeTitle = new IconTitle(Icons.get(Icons.CHANGES), Messages.get(this, "right_title"));
			changeTitle.setPos(0, 1);
			changeTitle.setSize(pw, 20);
			rightScroll.content().add(changeTitle);

			String body = Messages.get(this, "right_body");

			changeBody = PixelScene.renderTextBlock(body, 6);
			changeBody.maxWidth(pw - panel.marginHor());
			changeBody.setPos(0, changeTitle.bottom()+2);
			rightScroll.content().add(changeBody);

		} else {
			panel.size( pw, ph );
			panel.x = insets.left + (w - pw) / 2f;
			panel.y = insets.top + 20;
		}
		align( panel );
		add( panel );
		
		final ArrayList<ChangeInfo> changeInfos = new ArrayList<>();

		if (Messages.lang() != Languages.ENGLISH){
			ChangeInfo langWarn = new ChangeInfo("", true, Messages.get(this, "lang_warn"));
			langWarn.hardlight(CharSprite.WARNING);
			changeInfos.add(langWarn);
		}
		
		//everything past the first tab is Shattered's own history, kept as Evan wrote it
		if (changesSelected != 0){
			ChangeInfo base = new ChangeInfo(Messages.get(this, "base_title"), true, Messages.get(this, "base_body"));
			base.hardlight(0xCCCCCC);
			changeInfos.add(base);
		}

		switch (changesSelected){
			case 0: default:
				vWarped_Changes.addAllChanges(changeInfos);
				break;
			case 1:
				v4_X_Changes.addAllChanges(changeInfos);
				break;
			case 2:
				v3_X_Changes.addAllChanges(changeInfos);
				break;
			case 3:
				v2_X_Changes.addAllChanges(changeInfos);
				break;
			case 4:
				v1_X_Changes.addAllChanges(changeInfos);
				break;
			case 5:
				v0_9_X_Changes.addAllChanges(changeInfos);
				break;
			case 6:
				v0_8_X_Changes.addAllChanges(changeInfos);
				break;
			case 7:
				v0_7_X_Changes.addAllChanges(changeInfos);
				break;
			case 8:
				v0_6_X_Changes.addAllChanges(changeInfos);
				v0_5_X_Changes.addAllChanges(changeInfos);
				v0_4_X_Changes.addAllChanges(changeInfos);
				v0_3_X_Changes.addAllChanges(changeInfos);
				v0_2_X_Changes.addAllChanges(changeInfos);
				v0_1_X_Changes.addAllChanges(changeInfos);
				Pixel_Dungeon_Changes.addAllChanges(changeInfos);
				break;
		}

		ScrollPane list = new ScrollPane( new Component() ){

			@Override
			public void onClick(float x, float y) {
				for (ChangeInfo info : changeInfos){
					if (info.onClick( x, y )){
						return;
					}
				}
			}

		};
		add( list );

		Component content = list.content();
		content.clear();

		float posY = 0;
		float nextPosY = 0;
		boolean second = false;
		for (ChangeInfo info : changeInfos){
			if (info.major) {
				posY = nextPosY;
				second = false;
				info.setRect(0, posY, panel.innerWidth(), 0);
				content.add(info);
				posY = nextPosY = info.bottom();
			} else {
				if (!second){
					second = true;
					info.setRect(0, posY, panel.innerWidth()/2f, 0);
					content.add(info);
					nextPosY = info.bottom();
				} else {
					second = false;
					info.setRect(panel.innerWidth()/2f, posY, panel.innerWidth()/2f, 0);
					content.add(info);
					nextPosY = Math.max(info.bottom(), nextPosY);
					posY = nextPosY;
				}
			}
		}

		content.setSize( panel.innerWidth(), (int)Math.ceil(posY) );

		list.setRect(
				panel.x + panel.marginLeft(),
				panel.y + panel.marginTop() - 1,
				panel.innerWidth() + 2,
				panel.innerHeight() + 2);
		list.scrollTo(0, 0);

		//one tab for Warped, then Shattered's history split the way Evan splits it,
		//in two pages so nine tabs fit the panel
		float left = list.left()-4f;

		if (changesSelected <= 4){

			left = setupChangesSelectionButton(0, "WPD", left, list.bottom(), 24);
			left = setupChangesSelectionButton(1, "v4.X", left, list.bottom(), 24);
			left = setupChangesSelectionButton(2, "v3.X", left, list.bottom(), 24);
			left = setupChangesSelectionButton(3, "v2.X", left, list.bottom(), 24);
			left = setupChangesSelectionButton(4, "v1.X", left, list.bottom(), 24);
			left = setupChangesSelectionButton(5, "Old->", left, list.bottom(), 29);

		} else {

			left = setupChangesSelectionButton(4, "<-New", left, list.bottom(), 32);
			left = setupChangesSelectionButton(5, "v0.9", left, list.bottom(), 22);
			left = setupChangesSelectionButton(6, "v0.8", left, list.bottom(), 22);
			left = setupChangesSelectionButton(7, "v0.7", left, list.bottom(), 22);
			left = setupChangesSelectionButton(8, "v0.6-", left, list.bottom(), 27);

		}

		addToBack( BG );

		fadeIn();
	}

	private float setupChangesSelectionButton(int idx, String text, float left, float top, float width){
		StyledButton button = new StyledButton(Chrome.Type.GREY_BUTTON_TR, text, 8){
			@Override
			protected void onClick() {
				super.onClick();
				if (changesSelected != idx) {
					changesSelected = idx;
					WarpedPixelDungeon.seamlessResetScene();
				}
			}
		};
		if (changesSelected != idx) button.textColor( 0xBBBBBB );
		button.setRect(left, top, width, changesSelected == idx ? 19 : 15);
		addToBack(button);
		return button.right()-2;
	}

	private void updateChangesText(Image icon, String title, String... messages){
		if (changeTitle != null){
			changeTitle.icon(icon);
			changeTitle.label(title);
			changeTitle.setPos(changeTitle.left(), changeTitle.top());

			String message = "";
			for (int i = 0; i < messages.length; i++){
				message += messages[i];
				if (i != messages.length-1){
					message += "\n\n";
				}
			}
			changeBody.text(message);
			rightScroll.content().setSize(rightScroll.width(), changeBody.bottom()+2);
			rightScroll.setSize(rightScroll.width(), rightScroll.height());
			rightScroll.scrollTo(0, 0);

		} else {
			if (messages.length == 1) {
				addToFront(new WndChanges(icon, title, messages[0]));
			} else {
				addToFront(new WndChangesTabbed(icon, title, messages));
			}
		}
	}

	public static void showChangeInfo(Image icon, String title, String... messages){
		Scene s = WarpedPixelDungeon.scene();
		if (s instanceof ChangesScene){
			((ChangesScene) s).updateChangesText(icon, title, messages);
			return;
		}
		if (messages.length == 1) {
			s.addToFront(new WndChanges(icon, title, messages[0]));
		} else {
			s.addToFront(new WndChangesTabbed(icon, title, messages));
		}
	}
	
	@Override
	protected void onBackPressed() {
		WarpedPixelDungeon.switchNoFade(TitleScene.class);
	}

}
