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

import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

//the debug scenes, one button each. A WndOptions grows with its options and has no
//scrolling, so past a screenful of scenes the list ran off the bottom: this one scrolls
public class WndDebugScenes extends Window {

	private static final int WIDTH_P = 122;
	private static final int WIDTH_L = 200;
	private static final int GAP = 2;
	private static final int BTN_HEIGHT = 16;

	public WndDebugScenes(){
		super();

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		RenderedTextBlock title = PixelScene.renderTextBlock( "Debug scenes", 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( (width - title.width()) / 2f, 0 );
		add( title );

		RenderedTextBlock note = PixelScene.renderTextBlock(
				"Sets a situation up around the hero: the hero gets infinite health and the fog is lifted. "
				+ "Also from the command line: ./gradlew :desktop:debug -Pscene=<id>", 6 );
		note.maxWidth( width );
		note.setPos( 0, title.bottom() + GAP );
		add( note );
		float top = note.bottom() + GAP * 2;

		//the pane before its buttons: pointer listeners fire newest first, so a pane made
		//after the buttons under it would take their presses (see WndDebug)
		ScrollPane pane = new ScrollPane( new Component() );
		Component content = pane.content();
		float pos = 0;
		for (final DebugScenes.Scene scene : DebugScenes.SCENES){
			RedButton btn = new RedButton( scene.title(), 6 ){
				@Override
				protected void onClick(){
					hide();
					DebugScenes.run( scene );
				}
			};
			btn.multiline = true;
			btn.setRect( 0, pos, width, BTN_HEIGHT );
			if (btn.reqHeight() > BTN_HEIGHT) btn.setRect( 0, pos, width, btn.reqHeight() + 2 );
			content.add( btn );
			pos = btn.bottom() + GAP;
		}
		content.setSize( width, pos - GAP );

		int maxH = (int)(PixelScene.uiCamera.height - chrome.marginVer() - 20);
		float paneH = Math.min( content.height(), maxH - top );

		//resize before the pane goes in, for its camera (see WndDebug)
		resize( width, (int)Math.ceil( top + paneH ) );
		add( pane );
		pane.setRect( 0, top, width, paneH );
	}
}
