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

import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.ExitButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.TitleBackground;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.windows.WndMessage;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;

public class AboutScene extends PixelScene {

	@Override
	public void create() {
		super.create();

		final float colWidth = 120;
		final float fullWidth = colWidth * (landscape() ? 2 : 1);

		int w = Camera.main.width;
		int h = Camera.main.height;

		RectF insets = getCommonInsets();

		TitleBackground BG = new TitleBackground( w, h );
		add( BG );

		//darkens the arches
		add(new ColorBlock(w, h, 0x44000000));

		ScrollPane list = new ScrollPane( new Component() );
		add( list );

		Component content = list.content();
		content.clear();

		//*** Warped Pixel Dungeon Credits ***

		final int WARPD_COLOR = 0x5588CC;
		CreditsBlock warpd = new CreditsBlock(true, WARPD_COLOR,
				"Warped Pixel Dungeon",
				Icons.WPD.get(),
				"Developed by: _GabriWar_\nBased on Shattered, Overgrown, Sprouted\nand other Pixel Dungeon mods",
				"github.com/GabriWar",
				"https://github.com/GabriWar");
		//hold the WPD avatar for 5s to unlock the debug settings
		warpd.holdToActivate(5f, new Runnable() {
			@Override
			public void run() {
				if (!WPDSettings.debugUnlocked()) {
					WPDSettings.debugUnlocked(true);
					WarpedPixelDungeon.scene().add(new WndMessage(Messages.get(AboutScene.class, "debug_unlocked")));
				}
			}
		});
		if (landscape()){
			warpd.setRect((w - fullWidth)/2f - 6, insets.top + 10, 120, 0);
		} else {
			warpd.setRect((w - fullWidth)/2f, insets.top + 6, 120, 0);
		}
		content.add(warpd);

		//*** Shattered Pixel Dungeon Credits ***

		CreditsBlock shpx = new CreditsBlock(true, Window.SHPX_COLOR,
				"Shattered Pixel Dungeon",
				Icons.SHPX.get(),
				"Developed by: _Evan Debenham_\nBase for Warped Pixel Dungeon",
				"ShatteredPixel.com",
				"https://ShatteredPixel.com");
		if (landscape()){
			shpx.setRect(warpd.left(), warpd.bottom() + 8, colWidth, 0);
		} else {
			shpx.setRect(warpd.left(), warpd.bottom() + 8, colWidth, 0);
		}
		content.add(shpx);

		addLine(shpx.top() - 4, content);

		CreditsBlock alex = new CreditsBlock(false, Window.SHPX_COLOR,
				"Splash Art & Design:",
				Icons.ALEKS.get(),
				"Aleksandar Komitov",
				"alekskomitov.com",
				"https://www.alekskomitov.com/");
		alex.setSize(colWidth/2f, 0);
		if (landscape()){
			alex.setPos(shpx.right(), shpx.top() + (shpx.height() - alex.height()*2)/2f);
		} else {
			alex.setPos(w/2f - colWidth/2f, shpx.bottom()+5);
		}
		content.add(alex);

		CreditsBlock celesti = new CreditsBlock(false, Window.SHPX_COLOR,
				"Sound Effects:",
				Icons.CELESTI.get(),
				"Celesti",
				"celesti-whispers.itch.io",
				"https://celesti-whispers.itch.io/");
		celesti.setRect(alex.right(), alex.top(), colWidth/2f, 0);
		content.add(celesti);

		CreditsBlock lumine = new CreditsBlock(false, Window.SHPX_COLOR,
				"Music:",
				Icons.LUMINE.get(),
				"Lumine Haaristo",
				"youtube.com/@Lumine...",
				"https://www.youtube.com/@LumineThomasHaaristo");
		lumine.setRect(alex.right() - colWidth/4f, alex.bottom() + 5, colWidth/2f, 0);
		content.add(lumine);

		//*** The mods Warped is built from ***
		//Warped is a patchwork: most of what is in it was written by these
		//people first, and ported here under the GPL. Each block says what
		//their work became in this game.

		final int OVGR_COLOR = 0x66BB44;

		CreditsBlock modsTitle = new CreditsBlock(true, OVGR_COLOR,
				"Built on their work",
				null,
				"Warped is a patchwork of open source Pixel Dungeon mods. "
						+ "These are the people whose work it carries.",
				null,
				null);
		modsTitle.setRect(shpx.left(), lumine.bottom() + 10, colWidth, 0);
		content.add(modsTitle);

		addLine(modsTitle.top() - 4, content);

		CreditsBlock sprouted = new CreditsBlock(false, OVGR_COLOR,
				"Sprouted PD:",
				Icons.SHPX.get(),
				"_dachhack_\nThe overworld town, farming,\ndew and the deep floors",
				"github.com/dachhack",
				"https://github.com/dachhack/SproutedPixelDungeon");
		sprouted.setRect(modsTitle.left(), modsTitle.bottom() + 6, colWidth, 0);
		content.add(sprouted);

		CreditsBlock overgrown = new CreditsBlock(false, OVGR_COLOR,
				"Overgrown PD:",
				Icons.SHPX.get(),
				"_AnonymousPD_ (_TypedScroll_)\nPlants, seeds and the\ngrowing world",
				"github.com/AnonymousPD",
				"https://github.com/AnonymousPD/OvergrownPD");
		overgrown.setRect(sprouted.left(), sprouted.bottom() + 5, colWidth, 0);
		content.add(overgrown);

		CreditsBlock remixed = new CreditsBlock(false, OVGR_COLOR,
				"Remixed Dungeon:",
				Icons.SHPX.get(),
				"_NYRDS_\nThe town square and its\nbuildings, townsfolk and\nthe ice caves",
				"github.com/NYRDS",
				"https://github.com/NYRDS/remixed-dungeon");
		remixed.setRect(sprouted.left(), overgrown.bottom() + 5, colWidth, 0);
		content.add(remixed);

		CreditsBlock sps = new CreditsBlock(false, OVGR_COLOR,
				"SPS-PD:",
				Icons.SHPX.get(),
				"_hmdzl001_\nItems, enchantments and\nthe unique weapons",
				"github.com/hmdzl001",
				"https://github.com/hmdzl001/SPS-PD");
		sps.setRect(sprouted.left(), remixed.bottom() + 5, colWidth, 0);
		content.add(sps);

		CreditsBlock unleashed = new CreditsBlock(false, OVGR_COLOR,
				"Unleashed PD:",
				Icons.SHPX.get(),
				"_FthrNature_\nMonsters and dungeon\nfeatures",
				"github.com/FthrNature",
				"https://github.com/FthrNature/unleashed-pixel-dungeon");
		unleashed.setRect(sprouted.left(), sps.bottom() + 5, colWidth, 0);
		content.add(unleashed);

		CreditsBlock rearranged = new CreditsBlock(false, OVGR_COLOR,
				"Re-ARranged PD:",
				Icons.SHPX.get(),
				"_Hoto-Mocha_\nGuns, ammunition and\nalchemical weapons",
				"github.com/Hoto-Mocha",
				"https://github.com/Hoto-Mocha/Re-ARranged-Pixel-Dungeon");
		rearranged.setRect(sprouted.left(), unleashed.bottom() + 5, colWidth, 0);
		content.add(rearranged);

		CreditsBlock cursed = new CreditsBlock(false, OVGR_COLOR,
				"Cursed PD:",
				Icons.SHPX.get(),
				"_Smujb_\nTiles and room layouts",
				"github.com/Smujb",
				"https://github.com/Smujb/cursed-pixel-dungeon");
		cursed.setRect(sprouted.left(), rearranged.bottom() + 5, colWidth, 0);
		content.add(cursed);

		//*** Pixel Dungeon Credits ***

		final int WATA_COLOR = 0x55AAFF;
		CreditsBlock wata = new CreditsBlock(true, WATA_COLOR,
				"Pixel Dungeon",
				Icons.WATA.get(),
				"Developed by: _Watabou_\nInspired by Brian Walker's Brogue",
				"watabou.itch.io",
				"https://watabou.itch.io/");
		wata.setRect(shpx.left(), cursed.bottom() + 8, colWidth, 0);
		content.add(wata);

		addLine(wata.top() - 4, content);

		CreditsBlock cube = new CreditsBlock(false, WATA_COLOR,
				"Music:",
				Icons.CUBE_CODE.get(),
				"Cube Code",
				null,
				null);
		cube.setSize(colWidth/2f, 0);
		if (landscape()){
			cube.setPos(wata.right() + colWidth/4f, wata.top() + (wata.height() - cube.height())/2f);
		} else {
			cube.setPos(alex.left() + colWidth/4f, wata.bottom()+5);
		}
		content.add(cube);

		//*** libGDX Credits ***

		final int GDX_COLOR = 0xE44D3C;
		CreditsBlock gdx = new CreditsBlock(true,
				GDX_COLOR,
				"libGDX",
				Icons.LIBGDX.get(),
				"WarpedPD is powered by _libGDX_!",
				"libgdx.com",
				"https://libgdx.com/");
		if (landscape()){
			gdx.setRect(wata.left(), wata.bottom() + 8, colWidth, 0);
		} else {
			gdx.setRect(wata.left(), cube.bottom() + 8, colWidth, 0);
		}
		content.add(gdx);

		addLine(gdx.top() - 4, content);

		CreditsBlock arcnor = new CreditsBlock(false, GDX_COLOR,
				"Pixel Dungeon GDX:",
				Icons.ARCNOR.get(),
				"Edu García",
				"gamedev.place/@arcnor",
				"https://mastodon.gamedev.place/@arcnor");
		arcnor.setSize(colWidth/2f, 0);
		if (landscape()){
			arcnor.setPos(gdx.right(), gdx.top() + (gdx.height() - arcnor.height())/2f);
		} else {
			arcnor.setPos(alex.left(), gdx.bottom()+5);
		}
		content.add(arcnor);

		CreditsBlock purigro = new CreditsBlock(false, GDX_COLOR,
				"Shattered GDX Help:",
				Icons.PURIGRO.get(),
				"Kevin MacMartin",
				"github.com/prurigro",
				"https://github.com/prurigro/");
		purigro.setRect(arcnor.right()+2, arcnor.top(), colWidth/2f, 0);
		content.add(purigro);

		//*** Transifex Credits ***

		CreditsBlock transifex = new CreditsBlock(true,
				Window.TITLE_COLOR,
				null,
				null,
				"Warped PD inherits community translations from Shattered PD, made on _Transifex_! Thank you to all volunteer translators!",
				"transifex.com/shattered-pixel/...",
				"https://explore.transifex.com/shattered-pixel/shattered-pixel-dungeon/");
		transifex.setRect((Camera.main.width - colWidth)/2f, purigro.bottom() + 12, colWidth, 0);
		content.add(transifex);

		addLine(transifex.top() - 4, content);

		addLine(transifex.bottom() + 4, content);

		//*** Freesound Credits ***

		CreditsBlock freesound = new CreditsBlock(true,
				Window.TITLE_COLOR,
				null,
				null,
				"Shattered Pixel Dungeon uses the following sound samples from _freesound.org_:\n\n" +

				"Creative Commons Attribution License:\n" +
				"_SFX ATTACK SWORD 001.wav_ by _JoelAudio_\n" +
				"_Pack: Slingshots and Longbows_ by _saturdaysoundguy_\n" +
				"_Cracking/Crunching, A.wav_ by _InspectorJ_\n" +
				"_Extracting a sword.mp3_ by _Taira Komori_\n" +
				"_Pack: Uni Sound Library_ by _timmy h123_\n\n" +

				"Creative Commons Zero License:\n" +
				"_Pack: Movie Foley: Swords_ by _Black Snow_\n" +
				"_machine gun shot 2.flac_ by _qubodup_\n" +
				"_m240h machine gun burst 4.flac_ by _qubodup_\n" +
				"_Pack: Onomatopoeia_ by _Adam N_\n" +
				"_Pack: Watermelon_ by _lolamadeus_\n" +
				"_metal chain_ by _Mediapaja2009_\n" +
				"_Pack: Sword Clashes Pack_ by _JohnBuhr_\n" +
				"_Pack: Metal Clangs and Pings_ by _wilhellboy_\n" +
				"_Pack: Stabbing Stomachs & Crushing Skulls_ by _TheFilmLook_\n" +
				"_Sheep bleating_ by _zachrau_\n" +
				"_Lemon,Juicy,Squeeze,Fruit.wav_ by _Filipe Chagas_\n" +
				"_Lemon,Squeeze,Squishy,Fruit.wav_ by _Filipe Chagas_",
				"freesound.org",
				"https://www.freesound.org");
		freesound.setRect(transifex.left()-10, transifex.bottom() + 8, colWidth+20, 0);
		content.add(freesound);

		content.setSize( fullWidth, freesound.bottom()+10 + insets.bottom );

		list.setRect( 0, 0, w, h );
		list.scrollTo(0, 0);

		ExitButton btnExit = new ExitButton();
		int ofs = PixelScene.landscape() ? 0 : 4;
		btnExit.setPos( Camera.main.width - btnExit.width() - ofs, ofs );
		add( btnExit );

		//fadeIn();
	}
	
	@Override
	protected void onBackPressed() {
		WarpedPixelDungeon.switchScene(TitleScene.class);
	}

	private void addLine( float y, Group content ){
		ColorBlock line = new ColorBlock(Camera.main.width, 1, 0xFF333333);
		line.y = y;
		content.add(line);
	}

	private static class CreditsBlock extends Component {

		boolean large;
		RenderedTextBlock title;
		Image avatar;
		Flare flare;
		RenderedTextBlock body;

		RenderedTextBlock link;
		ColorBlock linkUnderline;
		PointerArea linkButton;

		//hold-to-activate on the avatar (used to unlock debug settings)
		PointerArea holdArea;
		float holdSecs;
		Runnable holdAction;
		float holdTimer = -1;
		boolean holdFired;

		//many elements can be null, but body is assumed to have content.
		private CreditsBlock(boolean large, int highlight, String title, Image avatar, String body, String linkText, String linkUrl){
			super();

			this.large = large;

			if (title != null) {
				this.title = PixelScene.renderTextBlock(title, large ? 8 : 6);
				if (highlight != -1) this.title.hardlight(highlight);
				add(this.title);
			}

			if (avatar != null){
				this.avatar = avatar;
				add(this.avatar);
			}

			if (large && highlight != -1 && this.avatar != null){
				this.flare = new Flare( 7, 24 ).color( highlight, true ).show(this.avatar, 0);
				this.flare.angularSpeed = 20;
				// Re-add avatar so it renders in front of the flare
				remove(this.avatar);
				add(this.avatar);
			}

			this.body = PixelScene.renderTextBlock(body, 6);
			if (highlight != -1) this.body.setHightlighting(true, highlight);
			if (large) this.body.align(RenderedTextBlock.CENTER_ALIGN);
			add(this.body);

			if (linkText != null && linkUrl != null){

				int color = 0xFFFFFFFF;
				if (highlight != -1) color = 0xFF000000 | highlight;
				this.linkUnderline = new ColorBlock(1, 1, color);
				add(this.linkUnderline);

				this.link = PixelScene.renderTextBlock(linkText, 6);
				if (highlight != -1) this.link.hardlight(highlight);
				add(this.link);

				linkButton = new PointerArea(0, 0, 0, 0){
					@Override
					protected void onClick( PointerEvent event ) {
						WarpedPixelDungeon.platform.openURI( linkUrl );
					}
				};
				add(linkButton);
			}

		}

		//press-and-hold the avatar for the given duration to fire the action
		public void holdToActivate(float seconds, Runnable action){
			this.holdSecs = seconds;
			this.holdAction = action;
			holdArea = new PointerArea(0, 0, 0, 0){
				@Override
				protected void onPointerDown( PointerEvent event ){
					holdTimer = 0;
					holdFired = false;
				}
				@Override
				protected void onPointerUp( PointerEvent event ){
					holdTimer = -1;
				}
			};
			add(holdArea);
		}

		@Override
		public void update() {
			super.update();
			if (holdTimer >= 0 && !holdFired){
				holdTimer += Game.elapsed;
				if (holdTimer >= holdSecs){
					holdFired = true;
					holdTimer = -1;
					if (flare == null && avatar != null){
						new Flare(7, 24).color(0xFFFFFF, true).show(avatar, 2f);
					}
					if (holdAction != null) holdAction.run();
				}
			}
		}

		@Override
		protected void layout() {
			super.layout();

			float topY = top();

			if (title != null){
				title.maxWidth((int)width());
				title.setPos( x + (width() - title.width())/2f, topY);
				topY += title.height() + (large ? 2 : 1);
			}

			if (large){

				if (avatar != null){
					avatar.x = x + (width()-avatar.width())/2f;
					avatar.y = topY;
					PixelScene.align(avatar);
					if (flare != null){
						flare.point(avatar.center());
					}
					topY = avatar.y + avatar.height() + 2;
				}

				body.maxWidth((int)width());
				body.setPos( x + (width() - body.width())/2f, topY);
				topY += body.height() + 2;

			} else {

				if (avatar != null){
					avatar.x = x;
					body.maxWidth((int)(width() - avatar.width - 1));

					float fullAvHeight = Math.max(avatar.height(), 16);
					if (fullAvHeight > body.height()){
						avatar.y = topY + (fullAvHeight - avatar.height())/2f;
						PixelScene.align(avatar);
						body.setPos( avatar.x + avatar.width() + 1, topY + (fullAvHeight - body.height())/2f);
						topY += fullAvHeight + 1;
					} else {
						avatar.y = topY + (body.height() - fullAvHeight)/2f;
						PixelScene.align(avatar);
						body.setPos( avatar.x + avatar.width() + 1, topY);
						topY += body.height() + 2;
					}

				} else {
					topY += 1;
					body.maxWidth((int)width());
					body.setPos( x, topY);
					topY += body.height()+2;
				}

			}

			if (holdArea != null && avatar != null){
				holdArea.x = avatar.x - 2;
				holdArea.y = avatar.y - 2;
				holdArea.width = avatar.width() + 4;
				holdArea.height = avatar.height() + 4;
			}

			if (link != null){
				if (large) topY += 1;
				link.maxWidth((int)width());
				link.setPos( x + (width() - link.width())/2f, topY);
				topY += link.height() + 2;

				linkButton.x = link.left()-1;
				linkButton.y = link.top()-1;
				linkButton.width = link.width()+2;
				linkButton.height = link.height()+2;

				linkUnderline.size(link.width(), PixelScene.align(0.49f));
				linkUnderline.x = link.left();
				linkUnderline.y = link.bottom()+1;

			}

			topY -= 2;

			height = Math.max(height, topY - top());
		}
	}
}
