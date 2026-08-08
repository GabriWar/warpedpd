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

import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.Game;

public class WndSundialGuide extends Window {

	private static final int WIDTH_P  = 120;
	private static final int WIDTH_L  = 180;
	private static final int MARGIN   = 4;
	private static final int BTN_H    = 14;
	private static final int GAP      = 2;

	private static final String[] TITLES = {
		"Overview",
		"Day Cycle",
		"Season",
		"Moon",
		"Weather",
		"Rest",
		"Temperature",
		"Wind & Compass",
		"Today"
	};

	private static final String[] PAGES = {
		"Carrying this sundial shows a panel just above the bag button, reading the world at a glance:\n\n_Header_ — time of day and current weather, by name.\n_Five bars_ — day cycle, season, moon, weather, rest.\n_Side bars_ — temperature (left) and wind (right).\n_Footer_ — exact readouts and a wind compass.\n\n_Tap the panel_ at any time to reopen this guide. _Hold it_ to hide the panel — it can be re-enabled in the interface settings.",

		"_Top bar_ — the full day in four colored phases:\n\n_Orange_ — dawn\n_Yellow_ — day\n_Purple_ — dusk\n_Blue_ — night\n\nThe current phase is lit while the others stay dim, and a _white pin_ slides left to right marking the exact time. The header names the current phase.",

		"_Second bar_ — the year, split into four seasons:\n\n_Green_ — spring\n_Yellow_ — summer\n_Orange_ — autumn\n_Blue_ — winter\n\nThe current season is lit, the rest are dimmed, and a _white pin_ tracks how far the year has progressed.",

		"_Third bar_ — 8 segments, one per lunar phase:\n\nDark _new moon_ on the left, brightening through crescent, quarter and gibbous to the _full moon_ at the center, then waning back down on the right.\n\nThe _white pin_ sits over tonight's phase.",

		"_Fourth bar_ — its color reflects the sky, and the header names the conditions:\n\n_Bright blue_ — clear\n_Gray-blue_ — partly cloudy\n_Deep blue_ — rain or storm\n_Gray-green_ — fog\n\nCloud cover blends into the bar color — the more clouds, the duller the hue.",

		"_Fifth bar_ — your hero's need for sleep.\n\nIt fills left to right as tiredness builds: _blue_ while rested, turning _red_ as exhaustion nears. The small gray notch marks where drowsiness starts to bite.\n\nSleep to empty it.",

		"_Left side bar_ — fills bottom-up with temperature, from \u221220\u00b0C to 45\u00b0C, shifting smoothly from _blue_ (freezing) through _green_ (mild) to _red_ (hot).\n\nThe small gray notch marks _0\u00b0C_ — below it, water freezes.\n\nThe exact reading sits at the bottom-left of the panel.",

		"_Right side bar_ — fills bottom-up with wind speed (0\u201325 m/s):\n\n_Green_ — calm  _Yellow_ — breezy  _Red_ — gale\n\nThe exact speed and heading sit at the bottom-right.\n\n_Compass_ (bottom center): ticks at N, E, S, W. The needle points where the wind blows, colored by speed with a _bright white_ tip — in still air it disappears."
	};

	//the static pages above, plus a last page built live from the calendar
	private static int pageCount() {
		return PAGES.length + 1;
	}

	private static String pageText(int page) {
		return page < PAGES.length ? PAGES[page] : todayText();
	}

	//the date, the moon, and every calendar modifier that is not neutral today
	private static String todayText() {
		StringBuilder sb = new StringBuilder();
		sb.append("_").append(GameCalendar.dateString()).append("_\n");
		sb.append("Moon: ").append(GameCalendar.moonPhaseString()).append("\n\n");
		java.util.ArrayList<String[]> mods = GameCalendar.activeModifiers();
		if (mods.isEmpty()) {
			sb.append("No calendar modifiers are in effect today.");
		} else {
			sb.append("Active modifiers:\n");
			for (String[] mod : mods) {
				sb.append("\n").append(mod[0]).append("  _").append(mod[1]).append("_");
			}
		}
		return sb.toString();
	}

	private final int page;

	public WndSundialGuide() {
		this(0);
	}

	public WndSundialGuide(int startPage) {
		super(0, 0, Chrome.get(Chrome.Type.SCROLL));
		this.page = startPage;

		int width = PixelScene.landscape() ? WIDTH_L - MARGIN * 2 : WIDTH_P - MARGIN * 2;

		// Title
		RenderedTextBlock title = PixelScene.renderTextBlock(TITLES[page], 8);
		title.hardlight(Window.TITLE_COLOR);
		title.maxWidth(width);
		title.invert();
		title.setPos(MARGIN, MARGIN);
		add(title);

		// Body
		RenderedTextBlock body = PixelScene.renderTextBlock(pageText(page), 6);
		body.maxWidth(width);
		body.invert();
		body.setPos(MARGIN, title.bottom() + GAP);
		add(body);

		// Nav buttons + counter
		float navY = body.bottom() + GAP * 2;
		int btnW = 28;

		RedButton btnPrev = new RedButton("< Prev", 7) {
			@Override
			protected void onClick() {
				hide();
				Game.scene().addToFront(new WndSundialGuide(page - 1));
			}
		};
		btnPrev.setSize(btnW, BTN_H);
		btnPrev.setPos(MARGIN, navY);
		btnPrev.active = page > 0;
		if (!btnPrev.active) btnPrev.alpha(0.4f);
		add(btnPrev);

		RenderedTextBlock counter = PixelScene.renderTextBlock((page + 1) + " / " + pageCount(), 6);
		counter.invert();
		counter.setPos(MARGIN + btnW + GAP, navY + (BTN_H - counter.height()) / 2f);
		add(counter);

		RedButton btnNext = new RedButton("Next >", 7) {
			@Override
			protected void onClick() {
				hide();
				Game.scene().addToFront(new WndSundialGuide(page + 1));
			}
		};
		btnNext.setSize(btnW, BTN_H);
		btnNext.setPos(width + MARGIN - btnW, navY);
		btnNext.active = page < pageCount() - 1;
		if (!btnNext.active) btnNext.alpha(0.4f);
		add(btnNext);

		resize(width + 2 * MARGIN, (int)(navY + BTN_H + MARGIN));
	}
}
