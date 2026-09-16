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

import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.ui.Component;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.WeatherFront;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleepiness;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyContext;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyMiniature;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.SundialIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

import java.util.ArrayList;

/**
 * What the sundial panel opens: a wider window on the same sky, then everything
 * the panel shows written out in full, with the numbers behind it (cloud and
 * humidity, the front on its way, days to the full moon, the season's length,
 * the calendar's effects today). A button leads to the guide to the panel.
 */
public class WndAlmanac extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 180;
	private static final int MARGIN = 4;
	private static final int H_IN = 34;
	private static final int BTN_HEIGHT = 16;

	public WndAlmanac() {
		super();

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		int wIn = width - 2 * MARGIN - 2 * SkyMiniature.FRAME;
		int winW = wIn + 2 * SkyMiniature.FRAME, winH = H_IN + 2 * SkyMiniature.FRAME;
		SkyContext c = SkyMiniature.capture(wIn, H_IN);

		Image titleIcon = new Image(Assets.Interfaces.SUNDIAL_TOGGLE);
		titleIcon.texture.filter(Texture.NEAREST, Texture.NEAREST);
		TextureFilm film = new TextureFilm(titleIcon.texture, 9, 9);
		titleIcon.frame(film.get(c.sunUp ? (c.solarEclipse ? 25 : 0) : SkyMiniature.moonFrame(c)));
		IconTitle title = new IconTitle(titleIcon, Messages.get(this, "title"));
		title.setRect(0, 0, width, 0);
		add(title);

		// ---- the window ----
		float wx = MARGIN, wy = title.bottom() + 3;
		String weather = SundialIndicator.weatherName(ClimateManager.weatherState());
		RenderedTextBlock caption = PixelScene.renderTextBlock(weather, 6);
		caption.hardlight(SundialIndicator.weatherColor(ClimateManager.weatherState()));

		SmartTexture skyTex = tex("almanac-sky", winW, winH);
		Pixmap sky = skyTex.bitmap;
		sky.setColor(0); sky.fill();
		SkyMiniature.paintSky(c, sky, SkyMiniature.FRAME, SkyMiniature.FRAME, 0);
		skyTex.bitmap(sky);
		Image skyImg = new Image(skyTex);
		skyImg.frame(0, 0, winW, winH);
		skyImg.x = wx; skyImg.y = wy;
		add(skyImg);

		int[] sun = SkyMiniature.sunIcon(c);
		if (sun != null) {
			Image sunIcon = new Image(titleIcon.texture);
			sunIcon.frame(film.get(c.solarEclipse ? 25 : 0));
			if (!c.solarEclipse) sunIcon.hardlight(1f, 1f - 0.25f * c.warmth, 1f - 0.45f * c.warmth);
			sunIcon.alpha(SkyMiniature.sunAlpha(c));
			sunIcon.x = wx + SkyMiniature.FRAME + Math.max(0, Math.min(wIn - 9, sun[0]));
			sunIcon.y = wy + SkyMiniature.FRAME + Math.max(0, Math.min(H_IN - 9, sun[1]));
			add(sunIcon);
		}

		SmartTexture overTex = tex("almanac-over", winW, winH);
		Pixmap over = overTex.bitmap;
		over.setColor(0); over.fill();
		SkyMiniature.paintOver(c, over, SkyMiniature.FRAME, SkyMiniature.FRAME, 0);
		SkyMiniature.paintPlates(over, SkyMiniature.FRAME, SkyMiniature.FRAME, wIn, Math.round(caption.width()), Math.round(caption.height()));
		SkyMiniature.paintFrame(over, 0, 0, winW, winH);
		overTex.bitmap(over);
		Image overImg = new Image(overTex);
		overImg.frame(0, 0, winW, winH);
		overImg.x = wx; overImg.y = wy;
		add(overImg);

		Image badge = new Image(titleIcon.texture);
		badge.frame(film.get(SkyMiniature.moonFrame(c)));
		badge.x = wx + SkyMiniature.FRAME + wIn - 11; badge.y = wy + SkyMiniature.FRAME + 2;
		add(badge);

		caption.setPos(wx + SkyMiniature.FRAME + 2, wy + SkyMiniature.FRAME + 2);
		add(caption);

		// ---- the almanac ----
		RenderedTextBlock body = PixelScene.renderTextBlock(text(c), 6);
		body.maxWidth(width - 2 * MARGIN);
		Component content = new Component();
		content.add(body);
		body.setPos(0, 0);
		content.setSize(width - 2 * MARGIN, body.bottom() + 1);

		float top = wy + winH + 3;
		float maxBody = PixelScene.uiCamera.height * 0.8f - top - BTN_HEIGHT - 3 * MARGIN;
		float bodyH = Math.min(content.height(), Math.max(40, maxBody));
		ScrollPane pane = new ScrollPane(content);
		add(pane);

		RedButton guide = new RedButton(Messages.get(this, "guide")) {
			@Override
			protected void onClick() {
				super.onClick();
				hide();
				Game.scene().addToFront(new WndSundialGuide());
			}
		};
		guide.setRect(MARGIN, top + bodyH + MARGIN, width - 2 * MARGIN, BTN_HEIGHT);
		add(guide);

		resize(width, (int)guide.bottom() + MARGIN);
		//a scroll pane places its camera from the window's, which resize() just moved
		pane.setRect(MARGIN, top, width - 2 * MARGIN, bodyH);
	}

	private static SmartTexture tex(String name, int w, int h) {
		SmartTexture t = TextureCache.create("sundial-" + name + "-" + w + "x" + h, w, h);
		t.bitmap.setBlending(Pixmap.Blending.None);
		t.filter(Texture.NEAREST, Texture.NEAREST);
		return t;
	}

	/** everything, in sentences */
	private String text(SkyContext c) {
		StringBuilder sb = new StringBuilder();
		sb.append("_").append(GameCalendar.dateString()).append("_\n");
		if (c.underground) sb.append(Messages.get(this, "underground")).append("\n");
		sb.append("\n");

		// time
		DayNightCycle.Phase phase = DayNightCycle.phase();
		int turns = DayNightCycle.turnsUntilPhaseChange();
		if (turns < 0) {
			sb.append(Messages.get(this, "time_clock", SundialIndicator.phaseName(phase)));
		} else {
			sb.append(Messages.get(this, "time", SundialIndicator.phaseName(phase), SundialIndicator.phaseName(phase.next()), turns,
					DayNightCycle.phaseDuration(DayNightCycle.Phase.DAY), DayNightCycle.phaseDuration(DayNightCycle.Phase.NIGHT)));
		}
		sb.append("\n\n");

		// weather
		sb.append(Messages.get(this, "weather", SundialIndicator.weatherName(ClimateManager.weatherState()),
				Math.round(ClimateManager.cloudCover() * 100), Math.round(ClimateManager.surfaceHumidity() * 100)));
		if (ClimateManager.localPrecipRate() > 0.02f) {
			sb.append(" ").append(Messages.get(this, "precip", Math.round(ClimateManager.localPrecipRate() * 100)));
		}
		sb.append("\n\n");

		// temperature
		float feels = Dungeon.hero != null ? TileTemperature.feelsLikeAt(Dungeon.hero.pos) : ClimateManager.feelsLikeTemp();
		sb.append(Messages.get(this, "temp", Math.round(feels), Math.round(ClimateManager.localTemp()))).append("\n\n");

		// wind
		float wind = ClimateManager.localWindSpeed();
		if (wind < 0.5f) sb.append(Messages.get(this, "wind_calm"));
		else sb.append(Messages.get(this, "wind", Math.round(wind), SundialIndicator.cardinal(ClimateManager.surfaceWindDir())));
		sb.append("\n\n");

		// moon
		float age = GameCalendar.moonProgress();
		String when;
		if (age < 0.5f) {
			int days = Math.round((0.5f - age) * 29f);
			when = days == 0 ? Messages.get(this, "moon_full_tonight") : Messages.get(this, "moon_full", days);
		} else {
			int days = Math.round((1f - age) * 29f);
			when = days == 0 ? Messages.get(this, "moon_new_tonight") : Messages.get(this, "moon_new", days);
		}
		sb.append(Messages.get(this, "moon", GameCalendar.moonPhaseString(), when)).append("\n\n");

		// season
		int day = GameCalendar.dayOfSeason(), days = GameCalendar.daysInCurrentSeason();
		String nextSeason = Messages.get(GameCalendar.class, GameCalendar.season().next().name().toLowerCase());
		sb.append(Messages.get(this, "season", day, days, nextSeason, days - day + 1)).append("\n\n");

		// forecast
		WeatherFront f = ClimateManager.nextFront();
		if (f == null) {
			sb.append(Messages.get(this, "forecast_none"));
		} else {
			String type = Messages.get(this, "front_" + f.type.name().toLowerCase());
			String strength = Messages.get(this, f.intensity < 0.35f ? "front_light" : (f.intensity < 0.7f ? "front_moderate" : "front_heavy"));
			String shift = f.tempShift > 2f ? Messages.get(this, "front_warmer") : (f.tempShift < -2f ? Messages.get(this, "front_colder") : "");
			int in = f.arrivalTurn - Dungeon.cycleTurn;
			if (in > 0) sb.append(Messages.get(this, "forecast_coming", type, in, strength, shift));
			else sb.append(Messages.get(this, "forecast_here", type, Math.max(0, f.arrivalTurn + f.duration - Dungeon.cycleTurn), strength, shift));
		}
		sb.append("\n\n");

		// rest
		Sleepiness tired = Dungeon.hero != null ? Dungeon.hero.buff(Sleepiness.class) : null;
		if (tired == null || tired.level() <= 0) {
			sb.append(Messages.get(this, "rest_fresh"));
		} else {
			sb.append(Messages.get(this, "rest", Math.round(100 * tired.level() / Sleepiness.COMATOSE),
					Math.round(100 * Sleepiness.DROWSY / Sleepiness.COMATOSE)));
		}
		sb.append("\n\n");

		// the calendar's effects
		ArrayList<String[]> mods = GameCalendar.activeModifiers();
		if (mods.isEmpty()) {
			sb.append(Messages.get(this, "today_none"));
		} else {
			StringBuilder list = new StringBuilder();
			for (String[] mod : mods) {
				if (list.length() > 0) list.append(", ");
				list.append(mod[0].toLowerCase()).append(" _").append(mod[1]).append("_");
			}
			sb.append(Messages.get(this, "today", list.toString()));
		}
		return sb.toString();
	}
}
