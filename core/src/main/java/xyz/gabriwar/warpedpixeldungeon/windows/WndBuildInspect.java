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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildInfo;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildPlacer;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

/**
 * The build bar's look at a cell (INSPECT): the piece on it with its health bar, what it is made
 * of and whether it burns, the roof over it, and the room it belongs to - its size, how much of it
 * is roofed, how well its walls keep the weather out, whether it is sealed, its air against the
 * outside's, and every line its room traits add (Rooms.describe). Where no room is, why not.
 * Scrolls when that runs long; placing goes on under it (BuildPlacer.Owned).
 */
public class WndBuildInspect extends Window implements BuildPlacer.Owned {

	private static final int WIDTH_P = 120, WIDTH_L = 160;
	private static final int GAP = 3;

	public WndBuildInspect( int cell ){
		super();
		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		BuildPiece piece = Homestead.pieceAt( Dungeon.level, cell );
		Image icon = piece != null ? piece.preview() : WndInfoCell.cellImage( cell );
		String name = piece != null ? Messages.titleCase( piece.name() ) : Messages.titleCase( WndInfoCell.cellName( cell ) );
		IconTitle title = new IconTitle( icon, name );
		if (piece != null && piece.destructible()) title.health( Homestead.hpAt( Dungeon.level, cell ) / (float) piece.maxHp );
		title.setRect( 0, 0, width, 0 );
		add( title );

		StringBuilder text = new StringBuilder();
		String built = BuildInfo.cellLines( Dungeon.level, cell );
		if (built != null) text.append( built );
		else text.append( Messages.get( this, "nothing_built" ) );
		String room = Rooms.describe( Dungeon.level, cell );
		if (room != null && !room.isEmpty()){
			text.append( "\n\n" ).append( Messages.get( this, "room" ) ).append( '\n' ).append( room );
		}

		//the pane first, then its text (see WndDebugScenes)
		ScrollPane pane = new ScrollPane( new Component() );
		Component content = pane.content();
		RenderedTextBlock info = PixelScene.renderTextBlock( text.toString(), 6 );
		info.maxWidth( width );
		info.setPos( 0, 0 );
		content.add( info );
		content.setSize( width, info.height() );

		float top = title.bottom() + GAP;
		int maxH = (int) (PixelScene.uiCamera.height * 0.8f - chrome.marginVer());
		float paneH = Math.min( content.height(), maxH - top );
		resize( width, (int) Math.ceil( top + paneH ) );
		add( pane );
		pane.setRect( 0, top, width, paneH );
	}
}
