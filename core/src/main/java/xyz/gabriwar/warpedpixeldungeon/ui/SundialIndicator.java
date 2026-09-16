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

package xyz.gabriwar.warpedpixeldungeon.ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.WeatherState;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleepiness;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyContext;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyMiniature;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndAlmanac;

/**
 * The climate and calendar HUD panel, top right under the menu pane.
 *
 * Top to bottom: a window on the sky above (the real sun, the moon in its shape,
 * the weather as it falls, the land you stand on), with the weather named in one
 * corner and the moon's phase badged in the other; the day as a dial under the
 * window; a thermometer and a wind compass with their readings; the countdown to
 * the next phase and the rest meter; the date; and the year as a strip.
 *
 * Tapping opens {@link WndAlmanac}; holding hides the panel; hovering shows the date.
 * The art is painted into three small textures and repainted only when what it
 * shows has changed, a few times a second at most while weather is falling.
 */
public class SundialIndicator extends Button {

	// --- geometry, mirrored by debug.SundialPreview ---
	private static final int PAD   = 3;
	private static final int W_IN  = 68, H_IN = 22;
	private static final int WIN_W = W_IN + 2 * SkyMiniature.FRAME;
	private static final int WIN_H = H_IN + 2 * SkyMiniature.FRAME;
	public  static final int WIDTH = PAD * 2 + WIN_W;
	private static final int Y_WIN = PAD;
	private static final int Y_DAY = Y_WIN + WIN_H;
	private static final int Y_ROW1 = Y_DAY + 3 + 2;
	private static final int Y_ROW2 = Y_ROW1 + 9 + 1;
	private static final int Y_ROW3 = Y_ROW2 + 7 + 1;
	private static final int Y_SEASON = Y_ROW3 + 7 + 1;
	private static final int HEIGHT = Y_SEASON + 3 + PAD;
	private static final int REST_W = 14;
	//the dials texture: rows 0-2 the day, 3-5 the year, 6-8 rest, 6-14 the instruments
	private static final int DIALS_H = 15, THERM_X = 20, COMPASS_X = 30;

	// --- colours ---
	public static final int[] PHASE_COLS  = { 0xff9a4a, 0xffd84a, 0xb070b8, 0x3a4a80 };
	public static final int[] PHASE_TEXT  = { 0xffb070, 0xffe888, 0xd8a0d8, 0x9cb0f0 };
	public static final int[] SEASON_COLS = { 0x4cb84c, 0xf0c832, 0xd06a24, 0x86b8de };
	public static final int[] SEASON_TEXT = { 0x8ee08a, 0xffe070, 0xf0a060, 0xb8dcf8 };

	// continuous gradients: temperature over -20..45, wind over 0..25 m/s
	public static final float[] TEMP_STOPS = { -20f, 0f, 15f, 25f, 35f, 45f };
	public static final int[]   TEMP_COLS  = { 0x4477ff, 0x55ccee, 0x44cc55, 0xeebb33, 0xff7722, 0xff3322 };
	public static final float[] WIND_STOPS = { 0f, 8f, 16f, 25f };
	public static final int[]   WIND_COLS  = { 0x66bb66, 0xd8c43a, 0xee7733, 0xee3333 };

	private static final int COLOR_SLEEP_OK = 0x4488ff, COLOR_SLEEP_TIRED = 0xdd3355;
	private static final int COLOR_TEXT = 0xCACFC2, COLOR_CALM = 0x9a9aa4;

	private static final String[] CARDINALS = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };

	// --- children ---
	private NinePatch panel;
	private SmartTexture skyTex, overTex, dialsTex;
	private Image skyImg, overImg, sunIcon, moonBadge;
	private Image dayStrip, seasonStrip, restBar, therm, compass;
	private TextureFilm film;
	private RenderedTextBlock caption, tempLabel, windLabel, countLabel, zzLabel, weekdayLabel, seasonLabel;

	// --- state ---
	private SkyContext ctx;
	private float lastRefresh = -1f;
	private long artKey = Long.MIN_VALUE, dialsKey = Long.MIN_VALUE;
	private int sunFrame = -1, badgeFrame = -1;
	private String sCaption = "", sTemp = "", sWind = "", sCount = "", sWeekday = "", sSeason = "";
	private int captionW = 0;

	//set by GameScene at creation: right edge to align to, the menu pane to hang under
	//(read live every frame - a frozen y drifts if the pane lays out after us and the
	//panel ends up covering the pane's buttons), and the boss health bar's extent so
	//the panel can duck under it during a boss fight
	private static float anchorRight = -1;
	private static MenuPane anchorPane = null;
	private static float bossRight   = 0;
	private static float bossClearY  = 0;

	public static void anchor(float right, MenuPane pane, float bossRightEdge, float bossBottom) {
		anchorRight = right;
		anchorPane  = pane;
		bossRight   = bossRightEdge;
		bossClearY  = bossBottom;
	}

	@Override
	protected void createChildren() {
		super.createChildren(); // hotArea: tap opens the almanac

		panel = Chrome.get(Chrome.Type.TOAST_TR);
		panel.alpha(0.9f);
		add(panel);

		skyTex   = tex("sky", WIN_W, WIN_H);
		overTex  = tex("over", WIN_W, WIN_H);
		dialsTex = tex("dials", WIN_W, DIALS_H);

		skyImg = new Image(skyTex);
		skyImg.frame(0, 0, WIN_W, WIN_H);
		add(skyImg);

		sunIcon = new Image(Assets.Interfaces.SUNDIAL_TOGGLE);
		//flush frames + linear sampling bleed the neighbouring frame's rim in
		sunIcon.texture.filter(Texture.NEAREST, Texture.NEAREST);
		film = new TextureFilm(sunIcon.texture, 9, 9);
		sunIcon.frame(film.get(0));
		add(sunIcon);

		overImg = new Image(overTex);
		overImg.frame(0, 0, WIN_W, WIN_H);
		add(overImg);

		moonBadge = new Image(sunIcon.texture);
		moonBadge.frame(film.get(1));
		add(moonBadge);

		caption = text();

		dayStrip    = slice(0, 0, WIN_W, 3);
		seasonStrip = slice(0, 3, WIN_W, 3);
		restBar     = slice(0, 6, REST_W, 3);
		therm       = slice(THERM_X, 6, 5, 9);
		compass     = slice(COMPASS_X, 6, 9, 9);

		tempLabel    = text();
		windLabel    = text();
		countLabel   = text();
		zzLabel      = text();
		weekdayLabel = text();
		seasonLabel  = text();
		zzLabel.text("Zz");
	}

	private static SmartTexture tex(String name, int w, int h) {
		SmartTexture t = TextureCache.create("sundial-" + name + "-" + w + "x" + h, w, h);
		t.bitmap.setBlending(Pixmap.Blending.None);
		t.filter(Texture.NEAREST, Texture.NEAREST);
		return t;
	}

	private Image slice(int l, int t, int w, int h) {
		Image i = new Image(dialsTex);
		i.frame(l, t, w, h);
		add(i);
		return i;
	}

	private RenderedTextBlock text() {
		RenderedTextBlock tb = PixelScene.renderTextBlock(6);
		tb.hardlight(COLOR_TEXT);
		add(tb);
		return tb;
	}

	// ------------------------------------------------------------ refresh

	/** reads the world, and repaints whatever it changed */
	private void refresh() {
		SkyContext c = SkyMiniature.capture(W_IN, H_IN);
		ctx = c;

		//the window animates only while something in it moves
		boolean animated = (c.precip != PrecipType.NONE && c.precipRate > 0.02f)
				|| c.storm
				|| c.ambient != ClimateManager.WeatherOverlayAmbient.NONE
				|| (c.cloudCover >= 0.12f && c.wind > 2f)
				|| (c.nightness > 0.55f && c.cloudCover < 0.8f)
				|| c.water() || c.town;
		int anim = animated ? (int)(Game.timeTotal * 8) : 0;

		captionUpdate(c);

		long key = 17;
		key = key * 31 + Math.round(c.f * 300);
		key = key * 31 + Math.round(c.cloudCover * 16);
		key = key * 31 + c.precip.ordinal();
		key = key * 31 + Math.round(c.precipRate * 6);
		key = key * 31 + (c.fog ? 1 : 0) + (c.storm ? 2 : 0) + (c.aurora ? 4 : 0) + (c.rainbow ? 8 : 0)
				+ (c.solarEclipse ? 16 : 0) + (c.lunarEclipse ? 32 : 0) + (c.town ? 64 : 0) + (c.underground ? 128 : 0) + (c.snow ? 256 : 0);
		key = key * 31 + c.season.ordinal();
		key = key * 31 + c.biome.ordinal();
		key = key * 31 + Math.round(c.moonAge * 48);
		key = key * 31 + Math.round(c.wind);
		key = key * 31 + (c.windDir > 0 ? 1 : 0);
		key = key * 31 + Math.round(c.humidity * 8);
		key = key * 31 + Math.round(c.temp / 5f);
		key = key * 31 + c.ambient.ordinal();
		key = key * 31 + captionW;
		key = key * 31 + anim;
		if (key != artKey) {
			artKey = key;
			paintWindow(c, anim);
		}

		//the sun: the icon, tinted as the sun is, or the black disc of an eclipse
		int[] sun = SkyMiniature.sunIcon(c);
		sunIcon.visible = sun != null;
		if (sun != null) {
			int frame = c.solarEclipse ? 25 : 0;
			if (frame != sunFrame) { sunFrame = frame; sunIcon.frame(film.get(frame)); }
			if (c.solarEclipse) sunIcon.resetColor();
			else sunIcon.hardlight(1f, 1f - 0.25f * c.warmth, 1f - 0.45f * c.warmth);
			sunIcon.alpha(SkyMiniature.sunAlpha(c));
		}
		int mf = SkyMiniature.moonFrame(c);
		if (mf != badgeFrame) { badgeFrame = mf; moonBadge.frame(film.get(mf)); }

		readouts(c);
	}

	private void paintWindow(SkyContext c, int anim) {
		Pixmap sky = skyTex.bitmap;
		sky.setColor(0); sky.fill();
		SkyMiniature.paintSky(c, sky, SkyMiniature.FRAME, SkyMiniature.FRAME, anim);
		skyTex.bitmap(sky);

		Pixmap over = overTex.bitmap;
		over.setColor(0); over.fill();
		SkyMiniature.paintOver(c, over, SkyMiniature.FRAME, SkyMiniature.FRAME, anim);
		SkyMiniature.paintPlates(over, SkyMiniature.FRAME, SkyMiniature.FRAME, W_IN, captionW, Math.round(caption.height()));
		SkyMiniature.paintFrame(over, 0, 0, WIN_W, WIN_H);
		overTex.bitmap(over);
	}

	private void captionUpdate(SkyContext c) {
		String name = weatherName(ClimateManager.weatherState());
		if (!name.equals(sCaption)) {
			sCaption = name;
			caption.text(name);
			int col = weatherColor(ClimateManager.weatherState());
			caption.hardlight(col);
			captionW = Math.round(caption.width());
		}
	}

	/** the numbers under the window, and the dials they sit beside */
	private void readouts(SkyContext c) {
		DayNightCycle.Phase phase = DayNightCycle.phase();

		// temperature where the hero stands
		float temp = Dungeon.hero != null ? TileTemperature.feelsLikeAt(Dungeon.hero.pos) : ClimateManager.feelsLikeTemp();
		int tCol = gradient(TEMP_STOPS, TEMP_COLS, temp);
		String tTxt = Math.round(temp) + "°";
		if (!tTxt.equals(sTemp)) { sTemp = tTxt; tempLabel.text(tTxt); }
		tempLabel.hardlight(tCol);

		// wind
		float wind = ClimateManager.localWindSpeed();
		float windDir = ClimateManager.surfaceWindDir();
		boolean calm = wind < 0.5f;
		int wCol = gradient(WIND_STOPS, WIND_COLS, wind);
		String wTxt = calm ? Messages.get(this, "calm") : Math.round(wind) + " " + cardinal(windDir);
		if (!wTxt.equals(sWind)) { sWind = wTxt; windLabel.text(wTxt); }
		windLabel.hardlight(calm ? COLOR_CALM : wCol);

		// the countdown to the next phase; the real-clock challenge has no turns to count
		int turns = DayNightCycle.turnsUntilPhaseChange();
		String next = phaseName(phase.next());
		String cTxt = turns < 0 ? phaseName(phase) : Messages.get(this, "until", next, turns);
		if (!cTxt.equals(sCount)) {
			sCount = cTxt;
			countLabel.text(cTxt);
			//if the row is full, the short form
			if (turns >= 0 && countLabel.width() > WIN_W - REST_W - zzLabel.width() - 6) {
				countLabel.text(Messages.get(this, "until_short", next, turns));
			}
		}
		countLabel.hardlight(PHASE_TEXT[phase.ordinal()]);

		// rest
		float sleepFrac = 0f;
		if (Dungeon.hero != null) {
			Sleepiness tired = Dungeon.hero.buff(Sleepiness.class);
			if (tired != null) sleepFrac = SkyPaint.clamp01(tired.level() / Sleepiness.COMATOSE);
		}
		float drowsyFrac = Sleepiness.DROWSY / Sleepiness.COMATOSE;
		float warn = sleepFrac <= drowsyFrac ? 0f : Math.min(1f, (sleepFrac - drowsyFrac) / (1f - drowsyFrac));
		int sCol = SkyPaint.mix(COLOR_SLEEP_OK, COLOR_SLEEP_TIRED, warn);
		zzLabel.hardlight(sCol);

		// the date: the weekday drops its "day" when the row is full
		String wd = GameCalendar.weekdayString();
		String sd = Messages.get(GameCalendar.class, GameCalendar.season().name().toLowerCase()) + " " + GameCalendar.dayOfSeason();
		if (!wd.equals(sWeekday) || !sd.equals(sSeason)) {
			sWeekday = wd; sSeason = sd;
			seasonLabel.text(sd);
			weekdayLabel.text(wd);
			if (weekdayLabel.width() + seasonLabel.width() + 4 > WIN_W && wd.toLowerCase().endsWith("day") && wd.length() > 5) {
				weekdayLabel.text(wd.substring(0, wd.length() - 3));
			}
		}
		seasonLabel.hardlight(SEASON_TEXT[GameCalendar.season().ordinal()]);

		// the dials, repainted when a pixel of them would move
		float dayPos = dayProgress(), yearPos = yearProgress();
		int dirStep = Math.round((((windDir % 360f) + 360f) % 360f) / 22.5f) % 16;
		long dk = 17;
		dk = dk * 31 + Math.round(dayPos * WIN_W);
		dk = dk * 31 + phase.ordinal();
		dk = dk * 31 + GameCalendar.season().ordinal();
		dk = dk * 31 + Math.round(yearPos * WIN_W);
		dk = dk * 31 + Math.round(sleepFrac * REST_W * 2);
		dk = dk * 31 + Math.round(warn * 8);
		dk = dk * 31 + Math.round(temp);
		dk = dk * 31 + dirStep;
		dk = dk * 31 + Math.round(wind);
		dk = dk * 31 + (calm ? 1 : 0);
		if (dk != dialsKey) {
			dialsKey = dk;
			Pixmap pm = dialsTex.bitmap;
			pm.setColor(0); pm.fill();
			float total = DayNightCycle.FULL_CYCLE;
			float[] fr = new float[4];
			for (int i = 0; i < 4; i++) fr[i] = DayNightCycle.phaseDuration(DayNightCycle.Phase.values()[i]) / total;
			SkyMiniature.paintStrip(pm, 0, 0, WIN_W, 3, fr, PHASE_COLS, phase.ordinal(), dayPos);
			SkyMiniature.paintStrip(pm, 0, 3, WIN_W, 3, new float[]{ 0.25f, 0.25f, 0.25f, 0.25f }, SEASON_COLS, GameCalendar.season().ordinal(), yearPos);
			SkyMiniature.paintBar(pm, 0, 6, REST_W, 3, sleepFrac, drowsyFrac, sCol);
			SkyMiniature.paintThermometer(pm, THERM_X, 6, temp, TEMP_STOPS[0], TEMP_STOPS[TEMP_STOPS.length - 1], tCol);
			SkyMiniature.paintCompass(pm, COMPASS_X, 6, windDir, calm, wCol);
			dialsTex.bitmap(pm);
		}
	}

	// ------------------------------------------------------------- layout

	@Override
	protected void layout() {
		panel.x = x;
		panel.y = y;
		panel.size(WIDTH, HEIGHT);

		float wx = x + PAD, wy = y + Y_WIN;
		skyImg.x = overImg.x = wx;
		skyImg.y = overImg.y = wy;
		float ix = wx + SkyMiniature.FRAME, iy = wy + SkyMiniature.FRAME;

		if (ctx != null && sunIcon.visible) {
			int[] p = SkyMiniature.sunIcon(ctx);
			if (p != null) {
				sunIcon.x = ix + Math.max(0, Math.min(W_IN - 9, p[0]));
				sunIcon.y = iy + Math.max(0, Math.min(H_IN - 9, p[1]));
			}
		}
		moonBadge.x = ix + W_IN - 11;
		moonBadge.y = iy + 2;

		caption.setPos(ix + 2, iy + 2);
		PixelScene.align(caption);

		dayStrip.x = wx;    dayStrip.y = y + Y_DAY;
		seasonStrip.x = wx; seasonStrip.y = y + Y_SEASON;

		// row 1: the thermometer and its reading, the compass and its reading
		therm.x = wx + 1; therm.y = y + Y_ROW1;
		tempLabel.setPos(wx + 8, y + Y_ROW1 + (9 - tempLabel.height()) / 2f);
		PixelScene.align(tempLabel);
		windLabel.setPos(wx + WIN_W - windLabel.width(), y + Y_ROW1 + (9 - windLabel.height()) / 2f);
		PixelScene.align(windLabel);
		compass.x = Math.round(windLabel.left() - 11); compass.y = y + Y_ROW1;

		// row 2: the countdown and the rest meter
		countLabel.setPos(wx + 1, y + Y_ROW2 + (7 - countLabel.height()) / 2f);
		PixelScene.align(countLabel);
		restBar.x = wx + WIN_W - REST_W; restBar.y = y + Y_ROW2 + 2;
		zzLabel.setPos(restBar.x - zzLabel.width() - 2, y + Y_ROW2 + (7 - zzLabel.height()) / 2f);
		PixelScene.align(zzLabel);

		// row 3: the date
		weekdayLabel.setPos(wx + 1, y + Y_ROW3 + (7 - weekdayLabel.height()) / 2f);
		PixelScene.align(weekdayLabel);
		seasonLabel.setPos(wx + WIN_W - seasonLabel.width(), y + Y_ROW3 + (7 - seasonLabel.height()) / 2f);
		PixelScene.align(seasonLabel);

		width  = WIDTH;
		height = HEIGHT;
		super.layout(); // size hotArea to the panel
	}

	@Override
	public void update() {
		super.update();

		//top-right under the menu pane, away from the toolbar/tag/log traffic at the bottom
		if (anchorRight >= 0 && anchorPane != null) {
			x = Math.round(anchorRight - WIDTH);
			float ty = anchorPane.anchorBottom() + 2;
			//duck under the boss health bar when one is up and would overlap
			if (BossHealthBar.isAssigned() && x < bossRight) {
				ty = Math.max(ty, bossClearY);
			}
			y = Math.round(ty);
		}

		//no fade: the panel pops with the rest of the HUD
		visible = WPDSettings.sundial();
		if (!visible) return;

		if (ctx == null || Game.timeTotal - lastRefresh >= 0.25f) {
			lastRefresh = Game.timeTotal;
			refresh();
		}
		layout();
	}

	@Override
	protected void onClick() {
		Sample.INSTANCE.play(Assets.Sounds.CLICK);
		GameScene.show(new WndAlmanac());
	}

	@Override
	protected boolean onLongClick() {
		WPDSettings.sundial(false);
		GLog.i(Messages.get(this, "hidden"));
		return true;
	}

	@Override
	protected String hoverText() {
		return GameCalendar.dateString();
	}

	public static float totalWidth() {
		return WIDTH;
	}

	public static float totalHeight() {
		return HEIGHT;
	}

	// ------------------------------------------------------------ helpers

	public static int gradient(float[] stops, int[] colors, float v) {
		if (v <= stops[0]) return colors[0];
		for (int i = 1; i < stops.length; i++) {
			if (v <= stops[i]) {
				return SkyPaint.mix(colors[i - 1], colors[i], (v - stops[i - 1]) / (stops[i] - stops[i - 1]));
			}
		}
		return colors[colors.length - 1];
	}

	public static String cardinal(float deg) {
		int idx = Math.round((((deg % 360f) + 360f) % 360f) / 45f) % 8;
		return CARDINALS[idx];
	}

	public static String phaseName(DayNightCycle.Phase phase) {
		return Messages.get(SundialIndicator.class, "phase_" + phase.name().toLowerCase());
	}

	/** the text colour that names a state of the sky */
	public static int weatherColor(WeatherState ws) {
		if (ws == null) return 0x9cd0ff;
		switch (ws) {
			case CLEAR:         return 0x9cd0ff;
			case FAIR:          return 0xb8d8f8;
			case PARTLY_CLOUDY: return 0xc8ccd4;
			case OVERCAST:      return 0xa0a6b0;
			case LIGHT_PRECIP:
			case HEAVY_PRECIP:
				switch (ClimateManager.localPrecipType()) {
					case SNOW: case BLIZZARD: case SLEET: return 0xe8f4ff;
					case HAIL: return 0xdfe4ea;
					default:   return 0x8cb8f0;
				}
			case STORM:         return 0xb0a0f0;
			case FOG:           return 0xc4ccc0;
			case CLEARING:      return 0xa8e0ff;
			default:            return 0x9cd0ff;
		}
	}

	public static String weatherName(WeatherState ws) {
		String key;
		if (ws == null) key = "w_clear";
		else switch (ws) {
			case CLEAR:         key = "w_clear"; break;
			case FAIR:          key = "w_fair"; break;
			case PARTLY_CLOUDY: key = "w_clouds"; break;
			case OVERCAST:      key = "w_overcast"; break;
			case LIGHT_PRECIP:
			case HEAVY_PRECIP:
				switch (ClimateManager.localPrecipType()) {
					case SNOW:     key = "w_snow"; break;
					case HAIL:     key = "w_hail"; break;
					case SLEET:    key = "w_sleet"; break;
					case BLIZZARD: key = "w_blizzard"; break;
					default:       key = ws == WeatherState.HEAVY_PRECIP ? "w_downpour" : "w_rain";
				}
				break;
			case STORM:         key = "w_storm"; break;
			case FOG:           key = "w_fog"; break;
			case CLEARING:      key = "w_clearing"; break;
			default:            key = "w_clear";
		}
		return Messages.get(SundialIndicator.class, key);
	}

	public static float dayProgress() {
		int pos = Dungeon.cycleTurn % DayNightCycle.FULL_CYCLE;
		return pos / (float) DayNightCycle.FULL_CYCLE;
	}

	public static float yearProgress() {
		float seasonStart = GameCalendar.season().ordinal() * 0.25f;
		float withinSeason = (GameCalendar.dayOfSeason() - 1f) / GameCalendar.daysInCurrentSeason();
		return seasonStart + 0.25f * withinSeason;
	}
}
