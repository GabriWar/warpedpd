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

import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

//a profiler or lag report: hundreds of lines, so unlike a WndTitledMessage it scrolls
public class WndDebugReport extends Window {

	private static final int WIDTH_P = 122;
	private static final int WIDTH_L = 240;
	private static final int GAP = 2;

	public WndDebugReport( String title, String report ){
		super();

		int width = (int)Math.min( PixelScene.landscape() ? WIDTH_L : WIDTH_P,
				PixelScene.uiCamera.width - chrome.marginHor() - 2 );

		IconTitle titlebar = new IconTitle( Icons.get( Icons.WARNING ), title );
		titlebar.setRect( 0, 0, width, 0 );
		add( titlebar );
		float top = titlebar.bottom() + GAP * 2;

		//the pane before its text, as everywhere else (pointer listeners fire newest first)
		ScrollPane pane = new ScrollPane( new Component() );
		RenderedTextBlock text = PixelScene.renderTextBlock( 6 );
		//class and method names carry underscores, which would toggle highlighting
		text.setHightlighting( false );
		text.text( report, width - 3 );
		text.setPos( 0, 0 );
		pane.content().add( text );
		pane.content().setSize( width, text.height() );

		int maxH = (int)(PixelScene.uiCamera.height - chrome.marginVer() - 20);
		float paneH = Math.min( text.height(), maxH - top );

		//resize before the pane goes in, for its camera
		resize( width, (int)Math.ceil( top + paneH ) );
		add( pane );
		pane.setRect( 0, top, width, paneH );
	}
}
