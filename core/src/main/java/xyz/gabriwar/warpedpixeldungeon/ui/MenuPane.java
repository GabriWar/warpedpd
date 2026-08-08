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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDAction;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.journal.Document;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.windows.WndChallenges;
import xyz.gabriwar.warpedpixeldungeon.windows.WndGame;
import xyz.gabriwar.warpedpixeldungeon.windows.WndJournal;
import xyz.gabriwar.warpedpixeldungeon.windows.WndKeyBindings;
import xyz.gabriwar.warpedpixeldungeon.windows.WndStory;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage;
import com.watabou.input.GameAction;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.DeviceCompat;

public class MenuPane extends Component {

	private Image bg;

	private Image depthIcon;
	private BitmapText depthText;
	private Button depthButton;

	private Image challengeIcon;
	private BitmapText challengeText;
	private Button challengeButton;

	private JournalButton btnJournal;
	private MenuButton btnMenu;

	private Toolbar.PickedUpItem pickedUp;

	private BitmapText version;
	private NinePatch versionOverflowBG;

	private DangerIndicator danger;

	public static final int WIDTH = 31;

	@Override
	protected void createChildren() {
		super.createChildren();

		bg = new Image(Assets.Interfaces.MENU, 1, 0, 31, 21);
		add(bg);

		versionOverflowBG = new NinePatch(bg.texture, 1, 22, 6, 8, 3, 0, 2, 0);
		add(versionOverflowBG);

		version = new BitmapText( "v" + Game.version , PixelScene.pixelFont);
		version.hardlight( 0xCACFC2 );
		add(version);

		depthIcon = Icons.get(Dungeon.level.feeling);
		add(depthIcon);

		depthText = new BitmapText( placeLine(), PixelScene.pixelFont);
		depthText.hardlight( 0xCACFC2 );
		depthText.measure();
		add( depthText );

		depthButton = new Button(){
			@Override
			protected String hoverText() {
				if (Dungeon.level.feeling != Level.Feeling.NONE){
					return Dungeon.level.feeling.desc();
				} else {
					return null;
				}
			}

			@Override
			protected void onClick() {
				super.onClick();

				if (Dungeon.level.feeling == Level.Feeling.NONE){
					GameScene.show(new WndJournal());
				} else {
					GameScene.show(new WndTitledMessage(Icons.getLarge(Dungeon.level.feeling),
							Messages.titleCase(Dungeon.level.feeling.title()),
							Dungeon.level.feeling.desc()));
				}
			}
		};
		add(depthButton);

		if (Challenges.activeChallenges() > 0){
			challengeIcon = Icons.get(Icons.CHAL_COUNT);
			add(challengeIcon);

			challengeText = new BitmapText( Integer.toString( Challenges.activeChallenges() ), PixelScene.pixelFont);
			challengeText.hardlight( 0xCACFC2 );
			challengeText.measure();
			add( challengeText );

			challengeButton = new Button(){
				@Override
				protected void onClick() {
					GameScene.show(new WndChallenges(Dungeon.challenges, false));
				}

				@Override
				protected String hoverText() {
					return Messages.get(WndChallenges.class, "title");
				}
			};
			add(challengeButton);
		}

		btnJournal = new JournalButton();
		add( btnJournal );

		btnMenu = new MenuButton();
		add( btnMenu );

		danger = new DangerIndicator();
		add( danger );

		add( pickedUp = new Toolbar.PickedUpItem());
	}

	@Override
	protected void layout() {
		super.layout();

		bg.x = x;
		bg.y = y;

		version.scale.set(PixelScene.align(0.5f));
		version.measure();

		float rightMargin = DeviceCompat.isDesktop() ? 1 : 8;
		if (DeviceCompat.isDebug()) rightMargin = 1; //don't care about hiding 'indev'
		float overFlow = version.width()-(bg.width()-4-rightMargin);
		if (overFlow >= 1){
			version.x = x + 2 - overFlow;
			versionOverflowBG.size(overFlow+3, 8);
			versionOverflowBG.x = version.x-3;
			versionOverflowBG.y = y;
		} else {
			version.x = x + 3;
			versionOverflowBG.visible = false;
		}
		version.y = y + 3 - (version.baseLine()*version.scale.y)/2f;
		version.y -= .001f;
		PixelScene.align(version);

		btnMenu.setPos( x + WIDTH - btnMenu.width(), y );

		btnJournal.setPos( btnMenu.left() - btnJournal.width() + 2, y );

		depthIcon.x = btnJournal.left() - 7 + (7 - depthIcon.width())/2f - 0.1f;
		depthIcon.y = y+8;
		PixelScene.align(depthIcon);

		placeDepthText();

		depthButton.setRect(depthText.x, depthIcon.y,
				depthIcon.x + depthIcon.width() - depthText.x, depthIcon.height());

		if (challengeIcon != null){
			challengeIcon.x = btnJournal.left() - 14 + (7 - challengeIcon.width())/2f - 0.1f;
			challengeIcon.y = depthIcon.y;
			PixelScene.align(challengeIcon);

			challengeText.scale.set(PixelScene.align(0.67f));
			challengeText.x = challengeIcon.x + (challengeIcon.width() - challengeText.width())/2f;
			challengeText.y = challengeIcon.y + challengeIcon.height();
			PixelScene.align(challengeText);

			challengeButton.setRect(challengeIcon.x, challengeIcon.y, challengeIcon.width(), challengeIcon.height() + challengeText.height());
		}

		danger.setPos( x + WIDTH - danger.width(), y + bg.height + 1 );
		danger.setSize( camera.width - danger.width(), danger.height());

		//declare the pane's real size: anything anchored below it (the sundial panel)
		//reads bottom(), which is meaningless while width/height stay 0
		width  = WIDTH;
		height = bg.height + 12;
	}

	//lowest edge anything anchored below the pane must clear: the pane bottom, or the
	//enemy-count badge when it's showing (it hangs a few px lower on the right)
	public float anchorBottom() {
		if (danger != null && danger.visible) {
			return Math.max(bottom(), danger.bottom());
		}
		return bottom();
	}

	public void pickup(Item item, int cell) {
		pickedUp.reset( item,
				cell,
				btnJournal.centerX(),
				btnJournal.centerY());
	}

	public void flashForPage( Document doc, String page ){
		btnJournal.flashingDoc = doc;
		btnJournal.flashingPage = page;
	}

	//"Sewers(3)" in the dungeon; just the place on the surface and in buildings
	private static String placeLine(){
		String name = Dungeon.placeName();
		boolean floored = Dungeon.branch == 0 && Dungeon.depth >= 1 && Dungeon.depth <= 26;
		if (Dungeon.depth >= 56 && Dungeon.depth <= 65) floored = true;
		return floored ? name + "(" + Dungeon.depth + ")" : name;
	}

	//the place line sits BESIDE the stairs icon, vertically centred on it,
	//growing leftward by however wide the word is (clear of the challenge
	//icon when that one is up)
	private void placeDepthText(){
		depthText.scale.set(PixelScene.align(0.67f));
		float leftAnchor = challengeIcon != null ? challengeIcon.x : depthIcon.x;
		depthText.x = leftAnchor - depthText.width()*depthText.scale.x - 2;
		depthText.y = depthIcon.y + (depthIcon.height() - depthText.baseLine()*depthText.scale.y)/2f;
		PixelScene.align(depthText);
	}

	private int lastHeroPos = -1;

	@Override
	public synchronized void update(){
		super.update();
		//on the surface the place follows the hero across biomes and villages
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
				&& Dungeon.hero != null && Dungeon.hero.pos != lastHeroPos){
			lastHeroPos = Dungeon.hero.pos;
			depthText.text( placeLine() );
			depthText.measure();
			placeDepthText();
		}
	}

	public void updateKeys(){
		btnJournal.updateKeyDisplay();
	}

	private static class JournalButton extends Button {

		private Image bg;
		private Image journalIcon;
		private KeyDisplay keyIcon;

		private Document flashingDoc = null;
		private String flashingPage = null;

		public JournalButton() {
			super();

			width = bg.width + 4;
			height = bg.height + 10;
		}

		@Override
		public GameAction keyAction() {
			return WPDAction.JOURNAL;
		}

		@Override
		protected void createChildren() {
			super.createChildren();

			bg = new Image( Assets.Interfaces.MENU_BTN, 2, 2, 13, 11 );
			add( bg );

			journalIcon = new Image( Assets.Interfaces.MENU_BTN, 31, 0, 11, 6);
			add( journalIcon );

			keyIcon = new KeyDisplay();
			add(keyIcon);
			updateKeyDisplay();
		}

		@Override
		protected void layout() {
			super.layout();

			bg.x = x + 2;
			bg.y = y + 8;

			journalIcon.x = bg.x + (bg.width() - journalIcon.width())/2f;
			journalIcon.y = bg.y + (bg.height() - journalIcon.height())/2f;
			PixelScene.align(journalIcon);

			keyIcon.x = bg.x + 1;
			keyIcon.y = bg.y + 1;
			keyIcon.width = bg.width - 2;
			keyIcon.height = bg.height - 2;
			PixelScene.align(keyIcon);
		}

		private float time;

		@Override
		public void update() {
			super.update();

			if (flashingPage != null){
				journalIcon.am = (float)Math.abs(Math.cos( StatusPane.FLASH_RATE * (time += Game.elapsed) ));
				keyIcon.am = journalIcon.am;
				bg.brightness(0.5f + journalIcon.am);
				if (time >= Math.PI/StatusPane.FLASH_RATE) {
					time = 0;
				}
			}
		}

		public void updateKeyDisplay() {
			keyIcon.updateKeys();
			keyIcon.visible = keyIcon.keyCount() > 0;
			journalIcon.visible = !keyIcon.visible;
			if (keyIcon.keyCount() > 0) {
				bg.brightness(.8f - (Math.min(6, keyIcon.keyCount()) / 20f));
			} else {
				bg.resetColor();
			}
		}

		@Override
		protected void onPointerDown() {
			bg.brightness( 1.5f );
			Sample.INSTANCE.play( Assets.Sounds.CLICK );
		}

		@Override
		protected void onPointerUp() {
			if (keyIcon.keyCount() > 0) {
				bg.brightness(.8f - (Math.min(6, keyIcon.keyCount()) / 20f));
			} else {
				bg.resetColor();
			}
		}

		@Override
		protected void onClick() {
			time = 0;
			keyIcon.am = journalIcon.am = 1;
			if (flashingPage != null){
				if (flashingDoc == Document.ALCHEMY_GUIDE){
					WndJournal.last_index = 2;
					GameScene.show( new WndJournal() );
				} else if (flashingDoc.pageNames().contains(flashingPage)){
					if (flashingDoc == Document.ADVENTURERS_GUIDE){
						WndJournal.last_index = 1;
					} else if (flashingDoc.isLoreDoc()){
						WndJournal.last_index = 3;
						WndJournal.CatalogTab.currentItemIdx = 3;
					}
					GameScene.show( new WndStory( flashingDoc.pageSprite(flashingPage),
							flashingDoc.pageTitle(flashingPage),
							flashingDoc.pageBody(flashingPage) ){
						@Override
						public void hide() {
							super.hide();
							if (WPDSettings.intro()){
								GameScene.endIntro();
							}
						}
					});
					flashingDoc.readPage(flashingPage);
				} else {
					GameScene.show( new WndJournal() );
				}
				flashingPage = null;
			} else {
				GameScene.show( new WndJournal() );
			}
		}

		@Override
		protected String hoverText() {
			return Messages.titleCase(Messages.get(WndKeyBindings.class, "journal"));
		}
	}

	private static class MenuButton extends Button {

		private Image image;

		public MenuButton() {
			super();

			width = image.width + 4;
			height = image.height + 10;
		}

		@Override
		protected void createChildren() {
			super.createChildren();

			image = new Image( Assets.Interfaces.MENU_BTN, 17, 2, 12, 11 );
			add( image );
		}

		@Override
		protected void layout() {
			super.layout();

			image.x = x + 2;
			image.y = y + 8;
		}

		@Override
		protected void onPointerDown() {
			image.brightness( 1.5f );
			Sample.INSTANCE.play( Assets.Sounds.CLICK );
		}

		@Override
		protected void onPointerUp() {
			image.resetColor();
		}

		@Override
		protected void onClick() {
			GameScene.show( new WndGame() );
		}

		@Override
		public GameAction keyAction() {
			return GameAction.BACK;
		}

		@Override
		protected String hoverText() {
			return Messages.titleCase(Messages.get(WndKeyBindings.class, "menu"));
		}
	}
}
