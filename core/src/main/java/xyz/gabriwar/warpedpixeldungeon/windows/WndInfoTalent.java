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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.TalentIcon;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.utils.Callback;

public class WndInfoTalent extends Window {

	private static final float GAP	= 2;

	private static final int WIDTH_MIN = 120;
	private static final int WIDTH_MAX = 220;

	private final Talent talent;
	private int points;
	private final TalentButtonCallback buttonCallback;
	private final java.util.ArrayList<com.watabou.noosa.Gizmo> content = new java.util.ArrayList<>();

	public WndInfoTalent(Talent talent, int points, TalentButtonCallback buttonCallback){
		super();
		this.talent = talent;
		this.points = points;
		this.buttonCallback = buttonCallback;
		build();
	}

	private <T extends com.watabou.noosa.Gizmo> T show( T g ){
		add( g );
		content.add( g );
		return g;
	}

	/** lays the window out for the talent as it is now; a callback that keeps the window
	 *  open has it rebuilt after each upgrade, so the numbers and the button stay fresh */
	private void build(){
		for (com.watabou.noosa.Gizmo g : content){
			erase( g );
			g.destroy();
		}
		content.clear();

		int width = WIDTH_MIN;

		IconTitle titlebar = new IconTitle();

		titlebar.icon( new TalentIcon( talent ) );
		String title = Messages.titleCase(talent.title());
		if (points > 0){
			title += " +" + points;
		}
		titlebar.label( title, Window.TITLE_COLOR );
		titlebar.setRect( 0, 0, width, 0 );
		show( titlebar );

		boolean metaDesc = (buttonCallback != null && buttonCallback.metamorphDesc()) ||
				(Dungeon.hero != null && Dungeon.hero.metamorphedTalents.containsValue(talent));

		RenderedTextBlock txtInfo = PixelScene.renderTextBlock(talent.desc(metaDesc), 6);
		txtInfo.maxWidth(width);
		txtInfo.setPos(titlebar.left(), titlebar.bottom() + 2*GAP);
		show( txtInfo );

		while (PixelScene.landscape()
				&& txtInfo.height() > 120
				&& width < WIDTH_MAX){
			width += 20;
			txtInfo.maxWidth(width);
		}
		titlebar.setRect( 0, 0, width, 0 );
		resize( width, (int)(txtInfo.bottom() + GAP) );

		if (buttonCallback != null && buttonCallback.available()) {
			RedButton button = new RedButton( buttonCallback.prompt() ) {
				@Override
				protected void onClick() {
					super.onClick();
					if (buttonCallback.staysOpen()){
						//keep the flow going: the same window, with fresh numbers
						buttonCallback.call();
						points = Dungeon.hero != null ? Dungeon.hero.pointsInTalent( talent ) : points + 1;
						build();
					} else {
						hide();
						buttonCallback.call();
					}
				}
			};
			button.icon(Icons.get(Icons.TALENT));
			button.setRect(0, txtInfo.bottom() + 2*GAP, width, 18);
			show( button );
			resize( width, (int)button.bottom()+1 );
		}

	}

	public static abstract class TalentButtonCallback implements Callback {

		public abstract String prompt();

		public boolean metamorphDesc(){
			return false;
		}

		/** whether the window stays open after the button and rebuilds itself */
		public boolean staysOpen(){
			return false;
		}

		/** whether the button can be offered right now; checked again on every rebuild */
		public boolean available(){
			return true;
		}

	}

}
