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
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.Game;

/** How to read the sundial panel, a page at a time. Opened from the almanac. */
public class WndSundialGuide extends Window {

	private static final int WIDTH_P  = 120;
	private static final int WIDTH_L  = 180;
	private static final int MARGIN   = 4;
	private static final int BTN_H    = 14;
	private static final int GAP      = 2;

	private static final String[] TITLES = {
		"The Sundial",
		"The Window",
		"The Horizon",
		"The Day Dial",
		"The Instruments",
		"The Calendar"
	};

	private static final String[] PAGES = {
		"The panel at the top right is a window on the sky above you, with the instruments of an almanac under it. Everything on it is read from the world: the real position of the sun, the true shape of the moon, the weather as it falls, the season the land is in.\n\n_Tap_ the panel for the almanac: the full date, the weather in numbers, the front on its way, and the calendar's effects today.\n\n_Hold_ the panel to hide it; the interface settings bring it back. On desktop, resting the pointer on it shows the date.",

		"_The sky_ is lit by the sun itself: how high it stands is the hour, its colour the time of day. Dawn glows on the left, dusk on the right, and night brings the stars.\n\n_The sun_ crosses the window left to right and sets behind the land. In an eclipse it turns to a black disc with a corona.\n\n_The moon_ rises later every night and shows its true shape, a pale ghost when it hangs in the day sky. Its phase is always in the _badge_ at the top right, new to full and back over 29 days.\n\n_The weather_ is drawn as it happens: clouds drifting on the wind, a grey deck when it closes over, rain, snow, hail, fog, lightning; a rainbow after a shower, an aurora on cold clear nights. The word in the top left names it.",

		"The land under the sky is where you stand: forest, plains, hills or mountains, water, desert, tundra, or the roofs of the town, where lamps come on in the windows after dark and chimneys smoke on cold nights.\n\nIt wears the season: green in spring and summer, gold and rust in autumn, bare in winter, and white wherever it freezes.\n\nFrom inside the dungeon the window still shows the surface, so you know what is waiting above.",

		"The strip glued under the window is the day, in four colours: _orange_ dawn, _yellow_ day, _purple_ dusk, _blue_ night. Each part is as long as it really lasts this season: summer days are long, winter days short. The current part is lit, and the white mark is now.\n\nThe text below counts down to the next part of the day, in turns.\n\nNight changes the dungeon: more creatures wake, you see less far, the undead hit harder, and you are harder to spot.",

		"_The thermometer_ reads how warm it feels where you stand, on a scale from -20° to 45°; the blue tick on its side is freezing. Its reading beside it runs from blue for cold through green to red for hot.\n\n_The compass_ points where the wind is blowing, and its needle takes the colour of the wind's speed. The reading gives metres per second and heading. In still air the needle rests and the reading says calm.\n\n_The rest meter_, beside the Zz, fills as tiredness builds. Blue while you are rested, red as collapse nears; the notch marks where drowsiness sets in. Sleep empties it.",

		"The date row names the weekday, the season and the day of the season. Seasons and weekdays carry small bonuses and penalties, and the moon has its say too: a full moon keeps creatures away and makes them hit harder, a new moon breeds more of them. The almanac lists what is in force today.\n\nThe strip at the very bottom is the year: _green_ spring, _gold_ summer, _orange_ autumn, _blue_ winter, with the white mark on today. The ripe season is lit."
	};

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
		RenderedTextBlock body = PixelScene.renderTextBlock(PAGES[page], 6);
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

		RenderedTextBlock counter = PixelScene.renderTextBlock((page + 1) + " / " + PAGES.length, 6);
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
		btnNext.active = page < PAGES.length - 1;
		if (!btnNext.active) btnNext.alpha(0.4f);
		add(btnNext);

		resize(width + 2 * MARGIN, (int)(navY + BTN_H + MARGIN));
	}
}
