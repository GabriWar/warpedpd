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
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

//the debug scenes, by group: first the groups (DebugScenes.CATEGORIES, each with its count), then a
//group's scenes, one button each, with a way back. A WndOptions grows with its options and has no
//scrolling, so past a screenful of scenes the list ran off the bottom: this one scrolls
public class WndDebugScenes extends Window {

	private static final int GAP = 2;
	private static final int BTN_HEIGHT = 16;

	public WndDebugScenes(){
		this( null );
	}

	/** A group's scenes, or (null) the groups. */
	public WndDebugScenes( final String category ){
		super();

		//as wide as the debug window it opens from
		int width = WndDebug.width( chrome.marginHor() );

		RenderedTextBlock title = PixelScene.renderTextBlock( category == null ? "Debug scenes" : "Scenes: " + category, 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( (width - title.width()) / 2f, 0 );
		add( title );

		float top = title.bottom() + GAP;
		if (category == null){
			RenderedTextBlock note = PixelScene.renderTextBlock(
					"Sets a situation up around the hero: the hero gets infinite health and the fog is lifted (both stay on; the Hero tab turns them off). "
					+ "Also from the command line: ./gradlew :desktop:debug -Pscene=<id>", 6 );
			note.maxWidth( width );
			note.setPos( 0, top );
			add( note );
			top = note.bottom() + GAP * 2;
		} else {
			RedButton back = new RedButton( "< All groups", 6 ){
				@Override
				protected void onClick(){
					WndDebugScenes.this.hide();
					GameScene.show( new WndDebugScenes() );
				}
			};
			back.setRect( 0, top + GAP, width, BTN_HEIGHT );
			add( back );
			top = back.bottom() + GAP * 2;
		}

		//the pane before its buttons: pointer listeners fire newest first, so a pane made
		//after the buttons under it would take their presses (see WndDebug). Dragged over its
		//buttons it scrolls, and a drag never runs the scene under the finger
		ScrollPane pane = new ScrollPane( new Component() );
		Component content = pane.content();
		float pos = 0;
		if (category == null){
			for (final String group : DebugScenes.CATEGORIES){
				int n = DebugScenes.inCategory( group ).size();
				if (n == 0) continue;
				RedButton btn = new RedButton( group + " (" + n + ")", 8 ){
					@Override
					protected void onClick(){
						WndDebugScenes.this.hide();
						GameScene.show( new WndDebugScenes( group ) );
					}
				};
				btn.setRect( 0, pos, width, BTN_HEIGHT + 2 );
				content.add( btn );
				pos = btn.bottom() + GAP;
			}
		} else {
			for (final DebugScenes.Scene scene : DebugScenes.inCategory( category )){
				RedButton btn = new RedButton( scene.title(), 6 ){
					@Override
					protected void onClick(){
						WndDebugScenes.this.hide();
						DebugScenes.run( scene );
					}
				};
				btn.multiline = true;
				btn.setRect( 0, pos, width, BTN_HEIGHT );
				if (btn.reqHeight() > BTN_HEIGHT) btn.setRect( 0, pos, width, btn.reqHeight() + 2 );
				content.add( btn );
				pos = btn.bottom() + GAP;
			}
		}
		content.setSize( width, Math.max( 0, pos - GAP ) );
		pane.dragOverButtons();

		int maxH = (int)(PixelScene.uiCamera.height - chrome.marginVer() - 20);
		float paneH = Math.min( content.height(), maxH - top );

		//resize before the pane goes in, for its camera (see WndDebug)
		resize( width, (int)Math.ceil( top + paneH ) );
		add( pane );
		pane.setRect( 0, top, width, paneH );
	}
}
