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
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.Rankings;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.BannerSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.WobblyBanner;
import xyz.gabriwar.warpedpixeldungeon.effects.Fireball;
import xyz.gabriwar.warpedpixeldungeon.journal.Document;
import xyz.gabriwar.warpedpixeldungeon.journal.Journal;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.TitleBackground;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.StyledButton;
import xyz.gabriwar.warpedpixeldungeon.windows.WndError;
import xyz.gabriwar.warpedpixeldungeon.windows.WndHardNotification;
import com.watabou.glwrap.Blending;
import com.watabou.input.ControllerHandler;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.FileUtils;
import com.watabou.utils.RectF;

import java.util.Collections;

public class WelcomeScene extends PixelScene {

	private static final int LATEST_UPDATE = WarpedPixelDungeon.v4_0_0;

	//used so that the game does not keep showing the window forever if cleaning fails
	private static boolean triedCleaningTemp = false;

	@Override
	public void create() {
		super.create();

		final int previousVersion = WPDSettings.version();

		if (!triedCleaningTemp && FileUtils.cleanTempFiles()){
			add(new WndHardNotification(Icons.get(Icons.WARNING),
					Messages.get(WndError.class, "title"),
					Messages.get(this, "save_warning"),
					Messages.get(this, "continue"),
					5){
				@Override
				public void hide() {
					super.hide();
					triedCleaningTemp = true;
					WarpedPixelDungeon.resetScene();
				}
			});
			return;
		}

		if (WarpedPixelDungeon.versionCode == previousVersion && !WPDSettings.intro()) {
			WarpedPixelDungeon.switchNoFade(TitleScene.class);
			return;
		}

		Music.INSTANCE.playTracks(
				new String[]{Assets.Music.THEME_1, Assets.Music.THEME_2},
				new float[]{1, 1},
				false);

		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();

		TitleBackground BG = new TitleBackground(w, h);
		add( BG );

		//darkens the arches
		add(new ColorBlock(w, h, 0x44000000));

		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;

		//same living wordmark as the title scene
		final WobblyBanner title = new WobblyBanner(
				landscape() ? BannerSprites.Type.TITLE_LAND : BannerSprites.Type.TITLE_PORT,
				1.6f, 60f, 2.6f, false );
		add( title );

		float topRegion = Math.max(title.height - 6, h*0.45f);

		title.x = insets.left + (w - title.width()) / 2f;
		title.y = insets.top + 2 + (topRegion - title.height()) / 2f;

		title.x = align(title.x);
		title.y = align(title.y);

		if (landscape()){
			placeTorch(title.x + 30, title.y + 35);
			placeTorch(title.x + title.width - 30, title.y + 35);
		} else {
			placeTorch(title.x + 16, title.y + 70);
			placeTorch(title.x + title.width - 16, title.y + 70);
		}

		WobblyBanner signs = new WobblyBanner(
				landscape() ? BannerSprites.Type.TITLE_GLOW_LAND : BannerSprites.Type.TITLE_GLOW_PORT,
				1.6f, 60f, 2.6f, true ){
			private float time = 0;
			@Override
			public void update() {
				super.update();
				am = Math.max(0f, (float)Math.sin( time += Game.elapsed ));
				if (time >= 1.5f*Math.PI) time = 0;
			}
		};
		signs.follow(title);
		signs.x = title.x + (title.width() - signs.width())/2f;
		signs.y = title.y;
		add( signs );
		
		StyledButton okay = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "continue")){
			@Override
			protected void onClick() {
				super.onClick();
				if (previousVersion == 0 || WPDSettings.intro()){

					if (previousVersion > 0){
						updateVersion(previousVersion);
					}

					WPDSettings.version(WarpedPixelDungeon.versionCode);
					GamesInProgress.selectedClass = null;
					GamesInProgress.curSlot = GamesInProgress.firstEmpty();
					if (GamesInProgress.curSlot == -1 || Rankings.INSTANCE.totalNumber > 0){
						WPDSettings.intro(false);
						WarpedPixelDungeon.switchScene(TitleScene.class);
					} else {
						WarpedPixelDungeon.switchScene(HeroSelectScene.class);
					}
				} else {
					updateVersion(previousVersion);
					WarpedPixelDungeon.switchScene(TitleScene.class);
				}
			}
		};

		float buttonY = insets.top + Math.min(topRegion + (PixelScene.landscape() ? 60 : 120), h - 24);

		float buttonAreaWidth = landscape() ? PixelScene.MIN_WIDTH_L-6 : PixelScene.MIN_WIDTH_P-2;
		float btnAreaLeft = insets.left + (w - buttonAreaWidth) / 2f;
		if (previousVersion != 0 && !WPDSettings.intro()){
			StyledButton changes = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(TitleScene.class, "changes")){
				@Override
				protected void onClick() {
					super.onClick();
					updateVersion(previousVersion);
					ChangesScene.changesSelected = ChangesScene.NEWEST;
					WarpedPixelDungeon.switchScene(ChangesScene.class);
				}
			};
			okay.setRect(btnAreaLeft, buttonY, (buttonAreaWidth/2)-1, 20);
			add(okay);

			changes.setRect(okay.right()+1, buttonY, okay.width(), 20);
			changes.icon(Icons.get(Icons.CHANGES));
			add(changes);
		} else {
			okay.text(Messages.get(TitleScene.class, "enter"));
			okay.setRect(btnAreaLeft, buttonY, buttonAreaWidth, 20);
			okay.icon(Icons.get(Icons.ENTER));
			add(okay);
		}

		RenderedTextBlock text = PixelScene.renderTextBlock(6);
		String message;
		if (previousVersion == 0 || WPDSettings.intro()) {
			message = Document.INTROS.pageBody(0);
		} else if (previousVersion <= WarpedPixelDungeon.versionCode) {
			if (previousVersion < LATEST_UPDATE){
				message = Messages.get(this, "update_intro");
				message += "\n\n" + Messages.get(this, "update_msg");
			} else {
				//what is new since the version the player last ran, as rows with their icons,
				//from the same list as the Changes screen's version tabs (WarpedUpdates)
				float titleBottom = title.y + title.height();
				addPatchRows( previousVersion, insets.left, w, titleBottom + 2, okay.top() - 4 );
				return;
			}
		} else {
			message = Messages.get(this, "what_msg");
		}

		text.text(message, Math.min(w-20, 300));
		float titleBottom = title.y + title.height();
		float textSpace = okay.top() - titleBottom - 4;
		text.setPos(insets.left + (w - text.width()) / 2f, (titleBottom + 2) + (textSpace - text.height())/2);
		add(text);

		if (WPDSettings.intro() && ControllerHandler.isControllerConnected()){
			addToFront(new WndHardNotification(Icons.CONTROLLER.get(),
					Messages.get(WelcomeScene.class, "controller_title"),
					Messages.get(WelcomeScene.class, "controller_body"),
					Messages.get(WelcomeScene.class, "controller_okay"),
					0){
				@Override
				public void onBackPressed() {
					//do nothing, must press the okay button
				}
			});
		}
	}

	private void placeTorch( float x, float y ) {
		Fireball fb = new Fireball();
		fb.x = x - fb.width()/2f;
		fb.y = y - fb.height();

		align(fb);
		add( fb );
	}

	//the latest release's changelog as plain bullet text (hashes stripped), or null
	//if the news feed hasn't loaded yet
	/**
	 * The patched screen's list: a heading, then one row per update newer than the version the
	 * player last ran (or the newest one), each its icon beside its title in the title colour and a
	 * one-line summary under it, as many as fit between the banner and the buttons, and a last line
	 * pointing to the Changes screen for the rest. The block is centred in that space.
	 */
	private void addPatchRows( int previousVersion, float left, float width, float top, float bottom ){
		java.util.ArrayList<xyz.gabriwar.warpedpixeldungeon.ui.changelist.WarpedUpdates.Entry> news =
				xyz.gabriwar.warpedpixeldungeon.ui.changelist.WarpedUpdates.since( previousVersion );
		if (news.isEmpty() && xyz.gabriwar.warpedpixeldungeon.ui.changelist.WarpedUpdates.ALL.length > 0){
			news.add( xyz.gabriwar.warpedpixeldungeon.ui.changelist.WarpedUpdates.ALL[0] );
		}
		float colW = Math.min( width - 20, 220 );
		float colX = left + (width - colW) / 2f;
		final float ICON = 16, GAP = 5, ROW_GAP = 6;

		RenderedTextBlock heading = PixelScene.renderTextBlock( Messages.get( this, "patch_intro" ), 9 );
		heading.hardlight( 0xFFFFFF );
		java.util.ArrayList<com.watabou.noosa.ui.Component> rows = new java.util.ArrayList<>();
		java.util.ArrayList<Float> heights = new java.util.ArrayList<>();
		float used = heading.height() + 8;
		int shown = 0;
		for (xyz.gabriwar.warpedpixeldungeon.ui.changelist.WarpedUpdates.Entry e : news){
			RenderedTextBlock t = PixelScene.renderTextBlock( e.title, 7 );
			t.hardlight( xyz.gabriwar.warpedpixeldungeon.ui.Window.TITLE_COLOR );
			t.maxWidth( (int)(colW - ICON - GAP) );
			RenderedTextBlock d = PixelScene.renderTextBlock( e.summary, 6 );
			d.hardlight( 0xCACFC2 );
			d.maxWidth( (int)(colW - ICON - GAP) );
			float h = Math.max( ICON, t.height() + 2 + d.height() );
			//room left for this row and, if more follow, the line pointing to the Changes screen
			float more = news.size() - shown - 1 > 0 ? 12 : 0;
			if (used + h + more > bottom - top && shown > 0) break;
			Image icon = e.icon.get();
			float s = Math.min( 1f, ICON / Math.max( icon.width(), icon.height() ) );
			icon.scale.set( s );
			final RenderedTextBlock ft = t, fd = d;
			final Image fi = icon;
			com.watabou.noosa.ui.Component row = new com.watabou.noosa.ui.Component(){
				@Override
				protected void layout(){
					fi.x = x + (ICON - fi.width() * fi.scale.x) / 2f;
					fi.y = y + (Math.min( height, ICON + 4 ) - fi.height() * fi.scale.y) / 2f;
					PixelScene.align( fi );
					ft.setPos( x + ICON + GAP, y );
					fd.setPos( x + ICON + GAP, ft.bottom() + 2 );
				}
			};
			row.add( icon );
			row.add( t );
			row.add( d );
			rows.add( row );
			heights.add( h );
			used += h + ROW_GAP;
			shown++;
		}
		RenderedTextBlock rest = null;
		if (news.size() > shown){
			rest = PixelScene.renderTextBlock( Messages.get( this, "patch_more", news.size() - shown ), 6 );
			rest.hardlight( 0xA0A79A );
			used += rest.height();
		}

		float y = top + Math.max( 0, (bottom - top - used) / 2f );
		heading.setPos( left + (width - heading.width()) / 2f, y );
		PixelScene.align( heading );
		add( heading );
		y = heading.bottom() + 8;
		for (int i = 0; i < rows.size(); i++){
			com.watabou.noosa.ui.Component row = rows.get( i );
			row.setRect( colX, y, colW, heights.get( i ) );
			add( row );
			y += heights.get( i ) + ROW_GAP;
		}
		if (rest != null){
			rest.setPos( left + (width - rest.width()) / 2f, y );
			PixelScene.align( rest );
			add( rest );
		}
	}



	private void updateVersion(int previousVersion){

		//update rankings, to update any data which may be outdated
		if (previousVersion < LATEST_UPDATE){

			Badges.loadGlobal();
			Journal.loadGlobal();

			try {
				Rankings.INSTANCE.load();
				for (Rankings.Record rec : Rankings.INSTANCE.records.toArray(new Rankings.Record[0])){
					try {
						Rankings.INSTANCE.loadGameData(rec);
						Rankings.INSTANCE.saveGameData(rec);
					} catch (Exception e) {
						//if we encounter a fatal per-record error, then clear that record's data
						rec.gameData = null;
						Game.reportException( new RuntimeException("Rankings Updating Failed!",e));
					}
				}
				if (Rankings.INSTANCE.latestDaily != null){
					try {
						Rankings.INSTANCE.loadGameData(Rankings.INSTANCE.latestDaily);
						Rankings.INSTANCE.saveGameData(Rankings.INSTANCE.latestDaily);
					} catch (Exception e) {
						//if we encounter a fatal per-record error, then clear that record's data
						Rankings.INSTANCE.latestDaily.gameData = null;
						Game.reportException( new RuntimeException("Rankings Updating Failed!",e));
					}
				}
				Collections.sort(Rankings.INSTANCE.records, Rankings.scoreComparator);
				Rankings.INSTANCE.save();
			} catch (Exception e) {
				//if we encounter a fatal error, then just clear the rankings
				FileUtils.deleteFile( Rankings.RANKINGS_FILE );
				Game.reportException( new RuntimeException("Rankings Updating Failed!",e));
			}
			Dungeon.daily = Dungeon.dailyReplay = false;

			Badges.saveGlobal(true);
			Journal.saveGlobal(true);

		}

		WPDSettings.version(WarpedPixelDungeon.versionCode);
	}
	
}
