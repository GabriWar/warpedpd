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

import xyz.gabriwar.warpedpixeldungeon.homestead.HomeActions;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.HomeThreats;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.Notice;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.BuildIcons;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.ui.Component;

import java.util.List;

/**
 * A home's ledger (the hearthstone's menu, homestead/threats/ThreatActions): the home's NOTICE -
 * how much it draws the eye of bandits - as a gauge against the raid line, then part by part (what
 * it holds, its light by night, less its defences), each with its own bar against its cap; and
 * below, everything that happened there, newest first: the sieges and raids fought and what was
 * settled while the hero was away. All of it scrolls, so it fits a phone held either way.
 */
public class WndHomeLedger extends Window {

	private static final int WIDTH_P = 120, WIDTH_L = 160;
	private static final int GAP = 2, BAR_H = 3;
	//the gauge's full width stands for this much notice
	private static final int GAUGE_MAX = 100;

	private static final int DRAW_COLOR    = 0xFFD9774A;   //what draws the eye: warm
	private static final int DEFENCE_COLOR = 0xFF5FB35F;   //what turns it away: green
	private static final int TRACK_COLOR   = 0xFF2A2420;
	private static final int ABOVE_COLOR   = 0xFFCC3B30;
	private static final int BELOW_COLOR   = 0xFFC8A040;
	private static final int LINE_COLOR    = 0xFFFFFFFF;

	public WndHomeLedger( HomesteadWorld.Home home ){
		super();

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		IconTitle title = new IconTitle( BuildIcons.get( BuildIcons.DEFENCE ), Messages.titleCase( home.title() ) );
		title.setRect( 0, 0, width, 0 );
		add( title );
		float top = title.bottom() + GAP;

		//no buttons in it: the pane only scrolls
		ScrollPane pane = new ScrollPane( new Component() );
		Component content = pane.content();
		float pos = 0;

		Notice.Score score = Notice.of( home );
		int total = score.total();
		pos = text( content, Messages.get( this, score.aboveLine() ? "above" : "below", total, HomeThreats.RAID_LINE ),
				width, pos, score.aboveLine() ? 0xFF8070 : Window.WHITE ) + GAP;
		pos = gauge( content, width, pos, total ) + GAP * 2;

		pos = text( content, Messages.get( this, "draws" ), width, pos, TITLE_COLOR ) + GAP;
		for (Notice.Part p : Notice.Part.values()){
			if (p == Notice.Part.WALLS) pos = text( content, Messages.get( this, "defences" ), width, pos + GAP, TITLE_COLOR ) + GAP;
			pos = part( content, width, pos, p, score.get( p ) ) + GAP;
		}

		pos = text( content, Messages.get( this, "ledger" ), width, pos + GAP * 2, TITLE_COLOR ) + GAP;
		List<HomesteadWorld.Entry> ledger = home.ledger();
		if (ledger.isEmpty()){
			pos = text( content, Messages.get( HomeActions.class, "ledger_empty" ), width, pos, Window.WHITE );
		} else {
			for (int i = ledger.size() - 1; i >= 0; i--){
				HomesteadWorld.Entry e = ledger.get( i );
				pos = text( content, Messages.get( HomeActions.class, "ledger_line", e.day + 1, e.text ), width, pos, Window.WHITE ) + GAP;
			}
		}
		content.setSize( width, pos );

		int maxH = (int)(PixelScene.uiCamera.height - chrome.marginVer() - 20);
		float paneH = Math.min( content.height(), maxH - top );

		//resize before the pane goes in, for its camera (see WndDebugScenes)
		resize( width, (int)Math.ceil( top + paneH ) );
		add( pane );
		pane.setRect( 0, top, width, paneH );
	}

	//a block of text across the window: its bottom
	private static float text( Component into, String s, int width, float y, int color ){
		RenderedTextBlock t = PixelScene.renderTextBlock( s, 6 );
		t.maxWidth( width );
		t.hardlight( color );
		t.setPos( 0, y );
		into.add( t );
		return t.bottom();
	}

	//the notice as a bar across the window, red past the raid line, with the line marked on it
	private static float gauge( Component into, int width, float y, int total ){
		ColorBlock track = new ColorBlock( width, BAR_H + 2, TRACK_COLOR );
		track.x = 0;
		track.y = y;
		into.add( track );
		float fill = width * Math.min( 1f, total / (float) GAUGE_MAX );
		if (fill > 0){
			ColorBlock bar = new ColorBlock( fill, BAR_H + 2, total > HomeThreats.RAID_LINE ? ABOVE_COLOR : BELOW_COLOR );
			bar.x = 0;
			bar.y = y;
			into.add( bar );
		}
		ColorBlock line = new ColorBlock( 1, BAR_H + 6, LINE_COLOR );
		line.x = width * HomeThreats.RAID_LINE / (float) GAUGE_MAX;
		line.y = y - 2;
		into.add( line );
		return y + BAR_H + 4;
	}

	//one part of the notice: its name and its points, and its bar against its cap
	private static float part( Component into, int width, float y, Notice.Part p, int value ){
		RenderedTextBlock name = PixelScene.renderTextBlock( p.title(), 6 );
		name.setPos( 0, y );
		into.add( name );
		RenderedTextBlock points = PixelScene.renderTextBlock( value > 0 ? "+" + value : Integer.toString( value ), 6 );
		points.hardlight( value == 0 ? 0x888888 : p.draws() ? 0xFFB08A : 0x9AE59A );
		points.setPos( width - points.width(), y );
		into.add( points );
		float below = Math.max( name.bottom(), points.bottom() ) + 1;

		ColorBlock track = new ColorBlock( width, BAR_H, TRACK_COLOR );
		track.x = 0;
		track.y = below;
		into.add( track );
		float fill = width * Math.min( 1f, Math.abs( value ) / (float) p.cap() );
		if (fill > 0){
			ColorBlock bar = new ColorBlock( fill, BAR_H, p.draws() ? DRAW_COLOR : DEFENCE_COLOR );
			bar.x = 0;
			bar.y = below;
			into.add( bar );
		}
		return below + BAR_H;
	}
}
