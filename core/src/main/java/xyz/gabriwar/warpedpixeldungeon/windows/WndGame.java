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
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.HeroSelectScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.RankingsScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.TitleScene;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.Game;
import com.watabou.utils.DeviceCompat;

import java.io.IOException;

public class WndGame extends Window {

	private static final int WIDTH		= 120;
	private static final int BTN_HEIGHT	= 20;
	private static final int GAP		= 2;
	
	private int pos;
	
	public WndGame() {
		
		super();

		//settings
		RedButton curBtn;

		//gaze upward: the living sky, wherever you are
		addButton( curBtn = new RedButton( Messages.get(this, "sky") ) {
			@Override
			protected void onClick() {
				hide();
				Game.switchScene( xyz.gabriwar.warpedpixeldungeon.scenes.SkyScene.class );
			}
		} );

		addButton( curBtn = new RedButton( Messages.get(this, "settings") ) {
			@Override
			protected void onClick() {
				hide();
				GameScene.show(new WndSettings());
			}
		});
		curBtn.icon(Icons.get(Icons.PREFS));

		// Challenges window
		if (Dungeon.challenges > 0) {
			addButton( curBtn = new RedButton( Messages.get(this, "challenges") ) {
				@Override
				protected void onClick() {
					hide();
					GameScene.show( new WndChallenges( Dungeon.challenges, false ) );
				}
			} );
			curBtn.icon(Icons.get(Icons.CHALLENGE_COLOR));
		}

		// Restart
		if (Dungeon.hero == null || !Dungeon.hero.isAlive()) {

			addButton( curBtn = new RedButton( Messages.get(this, "start") ) {
				@Override
				protected void onClick() {
					GamesInProgress.selectedClass = Dungeon.hero.heroClass;
					GamesInProgress.curSlot = GamesInProgress.firstEmpty();
					WarpedPixelDungeon.switchScene(HeroSelectScene.class);
				}
			} );
			curBtn.icon(Icons.get(Icons.ENTER));
			curBtn.textColor(Window.TITLE_COLOR);
			
			addButton( curBtn = new RedButton( Messages.get(this, "rankings") ) {
				@Override
				protected void onClick() {
					InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
					Game.switchScene( RankingsScene.class );
				}
			} );
			curBtn.icon(Icons.get(Icons.RANKINGS));
		}

		// Debug menu (debug builds, or unlocked via the credits easter egg)
		if (DeviceCompat.isDebug() || WPDSettings.debugUnlocked()) {
			addButton(curBtn = new RedButton("Debug") {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(new WndDebug());
				}
			});
			curBtn.icon(Icons.get(Icons.WARNING));
			curBtn.textColor(0xFF4444);
		}

		// Network controls
		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()) {
			int count = xyz.gabriwar.warpedpixeldungeon.net.NetManager.getClientCount();
			String label = "Stop Hosting" + (count > 0 ? " (" + count + " watching)" : "");
			addButton(curBtn = new RedButton(label) {
				@Override
				protected void onClick() {
					// Persist netHero state before tearing down — Dungeon.saveAll writes
					// the level bundle which carries the netHeroes piggybacked.
					try {
						Dungeon.saveAll();
					} catch (IOException e) {
						WarpedPixelDungeon.reportException(e);
					}
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.stop();
					hide();
				}
			});
			curBtn.icon(Icons.get(Icons.CLOSE));
			curBtn.textColor(0xFF8844);
		} else if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isNetClient()) {
			addButton(curBtn = new RedButton("Disconnect") {
				@Override
				protected void onClick() {
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.stop();
					Game.switchScene(TitleScene.class);
				}
			});
			curBtn.icon(Icons.get(Icons.CLOSE));
			curBtn.textColor(0xFF4444);
		}

		// Main menu
		addButton(curBtn = new RedButton(Messages.get(this, "menu")) {
			@Override
			protected void onClick() {
				if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isNetClient()) {
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.stop();
				} else {
					try {
						Dungeon.saveAll();
					} catch (IOException e) {
						WarpedPixelDungeon.reportException(e);
					}
				}
				Game.switchScene(TitleScene.class);
			}
		});
		curBtn.icon(Icons.get(Icons.DISPLAY));
		if (WPDSettings.intro()) curBtn.enable(false);

		resize( WIDTH, pos );
	}
	
	private void addButton( RedButton btn ) {
		add( btn );
		btn.setRect( 0, pos > 0 ? pos += GAP : 0, WIDTH, BTN_HEIGHT );
		pos += BTN_HEIGHT;
	}

	private void addButtons( RedButton btn1, RedButton btn2 ) {
		add( btn1 );
		btn1.setRect( 0, pos > 0 ? pos += GAP : 0, (WIDTH - GAP) / 2, BTN_HEIGHT );
		add( btn2 );
		btn2.setRect( btn1.right() + GAP, btn1.top(), WIDTH - btn1.right() - GAP, BTN_HEIGHT );
		pos += BTN_HEIGHT;
	}
}
