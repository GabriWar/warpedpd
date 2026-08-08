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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.WeatherState;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleepiness;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndSundialGuide;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Sample;

import java.util.Locale;

/**
 * Compact climate/time HUD chip, anchored above the bag button.
 *
 * Layout, top to bottom:
 *   header — time-of-day phase (left) and weather conditions (right), each with a color swatch
 *   bars   — day cycle, season, moon, weather, rest; flanked by vertical temp (L) and wind (R) bars
 *   footer — temperature readout, wind compass, wind speed + heading readout
 *
 * Tapping the panel opens {@link WndSundialGuide}; hovering shows the full calendar date.
 */
public class SundialIndicator extends Button {

	// --- Geometry ---
	private static final int PAD         = 3;
	private static final int BAR_WIDTH   = 60;
	private static final int BAR_HEIGHT  = 3;
	private static final int GAP         = 2;   // between horizontal bars
	private static final int VERT_W      = 3;   // temp / wind side bars
	private static final int VERT_GAP    = 1;
	private static final int SIDE_BAR_H  = BAR_HEIGHT * 4 + GAP * 3; // 18
	private static final int HEADER_H    = 7;
	private static final int SECTION_GAP = 3;   // header ↔ bars ↔ footer
	private static final int COMPASS_SIZE = 9;
	private static final int DATE_H      = 10;  // permanent date row, sun/moon icon + text

	private static final int CONTENT_W = VERT_W + VERT_GAP + BAR_WIDTH + VERT_GAP + VERT_W; // 68
	public  static final int WIDTH     = PAD * 2 + CONTENT_W;                               // 74
	private static final int FOOTER_H    = 9;   // footer row: moon phase, compass, readouts
	private static final int HEIGHT    = PAD * 2 + DATE_H + HEADER_H + SECTION_GAP
			+ SIDE_BAR_H + SECTION_GAP + FOOTER_H;                                          // 56

	// --- Colors ---
	private static final int COLOR_DAWN   = 0xFFFF8844;
	private static final int COLOR_DAY    = 0xFFFFDD44;
	private static final int COLOR_DUSK   = 0xFF995577;
	private static final int COLOR_NIGHT  = 0xFF3A4A80;

	private static final int COLOR_SPRING = 0xFF44BB44;
	private static final int COLOR_SUMMER = 0xFFFFCC22;
	private static final int COLOR_AUTUMN = 0xFFCC6622;
	private static final int COLOR_WINTER = 0xFF88BBDD;

	private static final int COLOR_CLEAR        = 0xFF4488DD;
	private static final int COLOR_FAIR         = 0xFF6699CC;
	private static final int COLOR_PARTLY       = 0xFF8899AA;
	private static final int COLOR_OVERCAST     = 0xFF667788;
	private static final int COLOR_LIGHT_PRECIP = 0xFF5577AA;
	private static final int COLOR_HEAVY_PRECIP = 0xFF3355AA;
	private static final int COLOR_STORM        = 0xFF2233AA;
	private static final int COLOR_FOG          = 0xFF889988;
	private static final int COLOR_CLEARING     = 0xFF77AACC;

	// Continuous gradients — temp maps −20..45°C, wind maps 0..25 m/s
	private static final float[] TEMP_STOPS  = { -20f, 0f, 15f, 25f, 35f, 45f };
	private static final int[]   TEMP_COLORS = {
		0xFF4477FF, 0xFF55CCEE, 0xFF44CC55, 0xFFEEBB33, 0xFFFF7722, 0xFFFF3322 };
	private static final float[] WIND_STOPS  = { 0f, 8f, 16f, 25f };
	private static final int[]   WIND_COLORS = {
		0xFF55AA55, 0xFFCCBB33, 0xFFEE7733, 0xFFEE3333 };

	// Rest: blue while rested, shifting to red once drowsy
	private static final int COLOR_SLEEP_OK    = 0xFF4488FF;
	private static final int COLOR_SLEEP_TIRED = 0xFFCC3366;

	private static final int COLOR_TRACK        = 0xFF101216;
	private static final int COLOR_PIN          = 0xFFFFFFFF;
	private static final int COLOR_TICK         = 0xFF666666;
	private static final int COLOR_TEXT         = 0xFFCCCCCC;
	private static final int COLOR_COMPASS_N    = 0xFFAABBCC;
	private static final int COLOR_COMPASS_TICK = 0xFF444444;
	private static final int COLOR_COMPASS_HUB  = 0xFF555555;

	// Dim factor for the non-current day-phase / season segments
	private static final float SEG_DIM = 0.4f;

	private static final String[] CARDINALS = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };

	// --- Children ---
	private NinePatch panel;

	//strip frames: 0 sun, 1..24 interpolated moon phases, 25 solar ecl, 26 lunar ecl
	private static final int FRAME_SUN = 0, FRAME_SOLAR_ECL = 25, FRAME_LUNAR_ECL = 26;

	private static int moonFrame() {
		return 1 + Math.round(GameCalendar.moonProgress() * 24f) % 24;
	}

	private Image dateIcon;                    //sun by day, the moon in its phase by night
	private RenderedTextBlock dateLabel;
	private TextureFilm dateFilm;
	private int dateFrame = -1;

	private ColorBlock phaseSwatch, weatherSwatch;
	private RenderedTextBlock phaseLabel, weatherLabel, tempLabel, windLabel;

	private ColorBlock dayTrack, seasonTrack, weatherTrack, sleepTrack;
	private ColorBlock[] daySegs, seasonSegs;
	private ColorBlock dayPin, dayPinCap, seasonPin, seasonPinCap;
	private ColorBlock weatherFill, sleepFill, sleepTick;

	private ColorBlock tempTrack, tempFill, tempTick;
	private ColorBlock windTrack, windFill;

	private ColorBlock compassBg, compassN, compassE, compassS, compassW, compassHub;
	private ColorBlock arrowA, arrowB, arrowC;

	// --- State ---
	private final float[] dayDims    = new float[4];
	private final float[] seasonDims = new float[4];
	private float arrowDim = 1f;

	private String sPhase = "", sWeather = "", sTemp = "", sWind = "", sDate = "";
	private int lastTempCol = 0, lastWindCol = 0;

	private float targetAlpha  = 0f;
	private float currentAlpha = 0f;

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
		super.createChildren(); // hotArea — tap opens the guide

		panel = Chrome.get(Chrome.Type.TOAST_TR);
		add(panel);

		dateIcon = new Image(Assets.Interfaces.SUNDIAL_TOGGLE);
		//flush frames + linear sampling bleed the neighbouring frame's rim in
		dateIcon.texture.filter(Texture.NEAREST, Texture.NEAREST);
		dateFilm = new TextureFilm(dateIcon.texture, 9, 9);
		dateIcon.frame(dateFilm.get(0));
		add(dateIcon);
		dateLabel = text();

		// header
		phaseSwatch   = block(COLOR_DAY);
		weatherSwatch = block(COLOR_CLEAR);
		phaseLabel    = text();
		weatherLabel  = text();

		// side bars
		tempTrack = block(COLOR_TRACK);
		tempFill  = block(0xFFFFFFFF);
		tempTick  = block(COLOR_TICK);
		windTrack = block(COLOR_TRACK);
		windFill  = block(0xFFFFFFFF);

		// day cycle
		dayTrack = block(COLOR_TRACK);
		daySegs = new ColorBlock[] {
			block(COLOR_DAWN), block(COLOR_DAY), block(COLOR_DUSK), block(COLOR_NIGHT) };

		// season
		seasonTrack = block(COLOR_TRACK);
		seasonSegs = new ColorBlock[] {
			block(COLOR_SPRING), block(COLOR_SUMMER), block(COLOR_AUTUMN), block(COLOR_WINTER) };

		// moon

		// weather + rest
		weatherTrack = block(COLOR_TRACK);
		weatherFill  = block(COLOR_CLEAR);
		sleepTrack   = block(COLOR_TRACK);
		sleepFill    = block(COLOR_SLEEP_OK);
		sleepTick    = block(COLOR_TICK);

		// position pins, drawn above the bars they mark
		dayPin       = block(COLOR_PIN); dayPinCap    = block(COLOR_PIN);
		seasonPin    = block(COLOR_PIN); seasonPinCap = block(COLOR_PIN);

		// footer
		tempLabel = text();
		windLabel = text();
		compassBg  = block(COLOR_TRACK);
		compassN   = block(COLOR_COMPASS_N);
		compassE   = block(COLOR_COMPASS_TICK);
		compassS   = block(COLOR_COMPASS_TICK);
		compassW   = block(COLOR_COMPASS_TICK);
		compassHub = block(COLOR_COMPASS_HUB);
		arrowA = block(COLOR_PIN);
		arrowB = block(COLOR_PIN);
		arrowC = block(COLOR_PIN);
	}

	private ColorBlock block(int color) {
		ColorBlock b = new ColorBlock(1, 1, color);
		add(b);
		return b;
	}

	private RenderedTextBlock text() {
		RenderedTextBlock tb = PixelScene.renderTextBlock(6);
		tb.hardlight(COLOR_TEXT & 0xFFFFFF);
		add(tb);
		return tb;
	}

	@Override
	protected void layout() {
		float px    = x + PAD;                       // content left
		float bx    = px + VERT_W + VERT_GAP;        // horizontal bars left
		float windX = bx + BAR_WIDTH + VERT_GAP;
		float right = windX + VERT_W;                // content right

		panel.x = x;
		panel.y = y;
		panel.size(WIDTH, HEIGHT);

		DayNightCycle.Phase phase = DayNightCycle.phase();
		boolean daytime = phase == DayNightCycle.Phase.DAWN || phase == DayNightCycle.Phase.DAY;

		// === Date row — the living sun/moon and the calendar date, always visible ===
		float dy2 = y + PAD;
		int frame;
		if (daytime) {
			frame = ClimateManager.isSolarEclipse() ? FRAME_SOLAR_ECL : FRAME_SUN;
		} else if (ClimateManager.isLunarEclipse()) {
			frame = FRAME_LUNAR_ECL;
		} else {
			frame = moonFrame();
		}
		if (frame != dateFrame) {
			dateFrame = frame;
			dateIcon.frame(dateFilm.get(frame));
		}
		switch (phase) {
			case DAWN: dateIcon.hardlight(1f, 0.8f, 0.6f);  break;
			case DUSK: dateIcon.hardlight(1f, 0.85f, 0.95f); break;
			default:   dateIcon.resetColor();
		}
		dateIcon.x = px; dateIcon.y = dy2;
		PixelScene.align(dateIcon);

		String dTxt = pretty(GameCalendar.weekday()).substring(0, 3) + ", "
				+ pretty(GameCalendar.season()) + " " + GameCalendar.dayOfSeason();
		if (!dTxt.equals(sDate)) {
			sDate = dTxt;
			dateLabel.text(dTxt);
		}
		dateLabel.setPos(px + 11, dy2 + (9 - dateLabel.height()) / 2f);
		PixelScene.align(dateLabel);

		// === Header — phase name (left), weather conditions (right) ===
		float hy = y + PAD + DATE_H;
		String pName = pretty(phase);
		if (!pName.equals(sPhase)) {
			sPhase = pName;
			phaseLabel.text(pName);
		}
		int pCol = phaseColor(phase);
		phaseSwatch.hardlight(r(lighten(pCol)), g(lighten(pCol)), b(lighten(pCol)));
		phaseSwatch.x = px; phaseSwatch.y = hy + 2; phaseSwatch.size(2, 2);
		phaseLabel.setPos(px + 4, hy + (HEADER_H - phaseLabel.height()) / 2f);
		PixelScene.align(phaseLabel);

		WeatherState ws = ClimateManager.weatherState();
		String wName = weatherName(ws);
		if (!wName.equals(sWeather)) {
			sWeather = wName;
			weatherLabel.text(wName);
		}
		// weather color dulled by cloud cover — same blend as the weather bar below
		int wsColor = weatherColor(ws);
		float cc = ClimateManager.cloudCover();
		float wR = r(wsColor) * (1f - cc) + 0.25f * cc;
		float wG = g(wsColor) * (1f - cc) + 0.25f * cc;
		float wB = b(wsColor) * (1f - cc) + 0.25f * cc;
		weatherLabel.setPos(right - weatherLabel.width(), hy + (HEADER_H - weatherLabel.height()) / 2f);
		PixelScene.align(weatherLabel);
		weatherSwatch.hardlight(
				wR + (1f - wR) * 0.25f, wG + (1f - wG) * 0.25f, wB + (1f - wB) * 0.25f);
		weatherSwatch.x = weatherLabel.left() - 4; weatherSwatch.y = hy + 2; weatherSwatch.size(2, 2);

		// === Bars block ===
		float by = y + PAD + DATE_H + HEADER_H + SECTION_GAP;

		// Temp vertical bar (LEFT) — fills bottom-up, gray notch at 0°C
		float temp = (Dungeon.hero != null)
				? TileTemperature.feelsLikeAt(Dungeon.hero.pos)
				: ClimateManager.feelsLikeTemp();
		float tempFrac  = clamp01((temp + 20f) / 65f);
		float tempFillH = Math.max(1, SIDE_BAR_H * tempFrac);
		int tCol = gradient(TEMP_STOPS, TEMP_COLORS, temp);
		tempTrack.x = px; tempTrack.y = by; tempTrack.size(VERT_W, SIDE_BAR_H);
		tempFill.hardlight(r(tCol), g(tCol), b(tCol));
		tempFill.x = px; tempFill.y = by + SIDE_BAR_H - tempFillH;
		tempFill.size(VERT_W, tempFillH);
		float freezeFrac = 20f / 65f; // (0°C + 20) / 65
		tempTick.x = px;
		tempTick.y = by + Math.round((SIDE_BAR_H - 1) * (1f - freezeFrac));
		tempTick.size(VERT_W, 1);

		// Wind vertical bar (RIGHT) — fills bottom-up with speed
		float wind     = ClimateManager.localWindSpeed();
		float windFrac  = clamp01(wind / 25f);
		float windFillH = Math.max(1, SIDE_BAR_H * windFrac);
		int wCol = gradient(WIND_STOPS, WIND_COLORS, wind);
		float aR = r(wCol), aG = g(wCol), aB = b(wCol);
		windTrack.x = windX; windTrack.y = by; windTrack.size(VERT_W, SIDE_BAR_H);
		windFill.hardlight(aR, aG, aB);
		windFill.x = windX; windFill.y = by + SIDE_BAR_H - windFillH;
		windFill.size(VERT_W, windFillH);

		// Day cycle bar — current phase lit, others dimmed, pin on exact time
		dayTrack.x = bx; dayTrack.y = by; dayTrack.size(BAR_WIDTH, BAR_HEIGHT);
		float segX = bx;
		for (int i = 0; i < 4; i++) {
			DayNightCycle.Phase p = DayNightCycle.Phase.values()[i];
			float w = BAR_WIDTH * DayNightCycle.phaseDuration(p) / (float) DayNightCycle.FULL_CYCLE;
			daySegs[i].x = segX; daySegs[i].y = by; daySegs[i].size(w, BAR_HEIGHT);
			dayDims[i] = (p == phase) ? 1f : SEG_DIM;
			segX += w;
		}
		pin(dayPin, dayPinCap, bx + (BAR_WIDTH - 1) * dayProgress(), by);

		// Season bar — 1px separators, current season lit, pin on day of year
		float sy = by + BAR_HEIGHT + GAP;
		seasonTrack.x = bx; seasonTrack.y = sy; seasonTrack.size(BAR_WIDTH, BAR_HEIGHT);
		float sw = BAR_WIDTH / 4f;
		int curSeason = GameCalendar.season().ordinal();
		for (int i = 0; i < 4; i++) {
			float w = (i < 3) ? sw - 1 : BAR_WIDTH - sw * 3;
			seasonSegs[i].x = bx + sw * i; seasonSegs[i].y = sy; seasonSegs[i].size(w, BAR_HEIGHT);
			seasonDims[i] = (i == curSeason) ? 1f : SEG_DIM;
		}
		pin(seasonPin, seasonPinCap, bx + (BAR_WIDTH - 1) * yearProgress(), sy);

		// Moon bar — 8 phase segments, pin centered on tonight's phase
		float wy = sy + BAR_HEIGHT + GAP;
		weatherTrack.x = bx; weatherTrack.y = wy; weatherTrack.size(BAR_WIDTH, BAR_HEIGHT);
		weatherFill.hardlight(wR, wG, wB);
		weatherFill.x = bx; weatherFill.y = wy; weatherFill.size(BAR_WIDTH, BAR_HEIGHT);

		// Rest bar — fills with tiredness, gray notch where drowsiness starts
		float sleepY = wy + BAR_HEIGHT + GAP;
		sleepTrack.x = bx; sleepTrack.y = sleepY; sleepTrack.size(BAR_WIDTH, BAR_HEIGHT);
		float sleepFrac = 0f;
		if (Dungeon.hero != null) {
			Sleepiness tired = Dungeon.hero.buff(Sleepiness.class);
			if (tired != null) {
				sleepFrac = clamp01(tired.level() / Sleepiness.COMATOSE);
			}
		}
		float drowsyFrac = Sleepiness.DROWSY / Sleepiness.COMATOSE;
		float warn = sleepFrac <= drowsyFrac ? 0f
				: Math.min(1f, (sleepFrac - drowsyFrac) / (1f - drowsyFrac));
		int sCol = lerpColor(COLOR_SLEEP_OK, COLOR_SLEEP_TIRED, warn);
		sleepFill.hardlight(r(sCol), g(sCol), b(sCol));
		sleepFill.x = bx; sleepFill.y = sleepY;
		sleepFill.size(Math.max(1, BAR_WIDTH * sleepFrac), BAR_HEIGHT);
		sleepTick.x = bx + Math.round((BAR_WIDTH - 1) * drowsyFrac);
		sleepTick.y = sleepY; sleepTick.size(1, BAR_HEIGHT);

		// === Footer — temp readout, compass, wind readout ===
		float fy = by + SIDE_BAR_H + SECTION_GAP;

		String tTxt = Math.round(temp) + "°";
		if (!tTxt.equals(sTemp)) {
			sTemp = tTxt;
			tempLabel.text(tTxt);
		}
		if (tCol != lastTempCol) {
			lastTempCol = tCol;
			tempLabel.hardlight(tCol & 0xFFFFFF);
		}
		tempLabel.setPos(px, fy + (FOOTER_H - tempLabel.height()) / 2f);
		PixelScene.align(tempLabel);

		float windDirVal = ClimateManager.surfaceWindDir();
		boolean calm = wind < 0.5f;
		String wTxt = calm ? "Calm" : Math.round(wind) + " " + cardinal(windDirVal);
		if (!wTxt.equals(sWind)) {
			sWind = wTxt;
			windLabel.text(wTxt);
		}
		if (wCol != lastWindCol) {
			lastWindCol = wCol;
			windLabel.hardlight(wCol & 0xFFFFFF);
		}
		windLabel.setPos(right - windLabel.width(), fy + (FOOTER_H - windLabel.height()) / 2f);
		PixelScene.align(windLabel);

		// Compass — needle points where the wind blows, white tip; hidden when calm
		float cx = bx + (BAR_WIDTH - COMPASS_SIZE) / 2f;
		float half = (COMPASS_SIZE - 1) / 2f;
		compassBg.x = cx; compassBg.y = fy; compassBg.size(COMPASS_SIZE, COMPASS_SIZE);
		float cyy = fy;
		compassN.x = cx + half;              compassN.y = cyy;                     compassN.size(1, 1);
		compassS.x = cx + half;              compassS.y = cyy + COMPASS_SIZE - 1;  compassS.size(1, 1);
		compassE.x = cx + COMPASS_SIZE - 1;  compassE.y = cyy + half;              compassE.size(1, 1);
		compassW.x = cx;                     compassW.y = cyy + half;              compassW.size(1, 1);
		compassHub.x = cx + half; compassHub.y = cyy + half; compassHub.size(1, 1);
		float windRad = (float) Math.toRadians(windDirVal);
		float sinW = (float) Math.sin(windRad);
		float cosW = (float) Math.cos(windRad);
		arrowA.x = cx + half + Math.round(1.5f * sinW); arrowA.y = cyy + half - Math.round(1.5f * cosW); arrowA.size(1, 1); arrowA.hardlight(aR, aG, aB);
		arrowB.x = cx + half + Math.round(2.5f * sinW); arrowB.y = cyy + half - Math.round(2.5f * cosW); arrowB.size(1, 1); arrowB.hardlight(aR, aG, aB);
		arrowC.x = cx + half + Math.round(3.5f * sinW); arrowC.y = cyy + half - Math.round(3.5f * cosW); arrowC.size(1, 1); arrowC.hardlight(1f, 1f, 1f);
		arrowDim = calm ? 0f : 1f;

		width  = WIDTH;
		height = HEIGHT;
		super.layout(); // size hotArea to the panel
	}

	// White position pin: 3px cap above the bar, stem through it with 1px overhang below
	private static void pin(ColorBlock stem, ColorBlock cap, float pinX, float barY) {
		pinX = Math.round(pinX);
		stem.x = pinX;     stem.y = barY - 1; stem.size(1, BAR_HEIGHT + 2);
		cap.x  = pinX - 1; cap.y  = barY - 2; cap.size(3, 1);
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

		//no fade: the panel pops with the rest of the HUD. the tween used to
		//strand it half-transparent whenever game time stood still
		targetAlpha = WPDSettings.sundial() ? 1f : 0f;
		currentAlpha = targetAlpha;

		visible = currentAlpha > 0.01f;
		if (visible) {
			layout();
			applyAlpha(currentAlpha);
		}
	}

	@Override
	protected void onClick() {
		Sample.INSTANCE.play(Assets.Sounds.CLICK);
		GameScene.show(new WndSundialGuide());
	}

	@Override
	protected boolean onLongClick() {
		WPDSettings.sundial(false);
		GLog.i(Messages.get(this, "hidden"));
		return true;
	}

	@Override
	protected String hoverText() {
		return pretty(GameCalendar.weekday()) + ", "
				+ pretty(GameCalendar.season()) + " " + GameCalendar.dayOfSeason() + ", "
				+ GameCalendar.year();
	}

	private void applyAlpha(float a) {
		panel.alpha(a * 0.9f);
		dateIcon.alpha(a); dateLabel.alpha(a);

		phaseSwatch.alpha(a); weatherSwatch.alpha(a);
		phaseLabel.alpha(a); weatherLabel.alpha(a);
		tempLabel.alpha(a); windLabel.alpha(a);

		tempTrack.alpha(a); tempFill.alpha(a); tempTick.alpha(a);
		windTrack.alpha(a); windFill.alpha(a);

		dayTrack.alpha(a);
		for (int i = 0; i < 4; i++) daySegs[i].alpha(a * dayDims[i]);
		seasonTrack.alpha(a);
		for (int i = 0; i < 4; i++) seasonSegs[i].alpha(a * seasonDims[i]);
		weatherTrack.alpha(a); weatherFill.alpha(a);
		sleepTrack.alpha(a); sleepFill.alpha(a); sleepTick.alpha(a);

		dayPin.alpha(a); dayPinCap.alpha(a);
		seasonPin.alpha(a); seasonPinCap.alpha(a);

		compassBg.alpha(a); compassN.alpha(a); compassE.alpha(a);
		compassS.alpha(a); compassW.alpha(a); compassHub.alpha(a);
		arrowA.alpha(a * arrowDim); arrowB.alpha(a * arrowDim); arrowC.alpha(a * arrowDim);
	}

	public static float totalWidth() {
		return WIDTH;
	}

	public static float totalHeight() {
		return HEIGHT;
	}

	// --- helpers ---
	private static float r(int c) { return ((c >> 16) & 0xFF) / 255f; }
	private static float g(int c) { return ((c >>  8) & 0xFF) / 255f; }
	private static float b(int c) { return  (c        & 0xFF) / 255f; }

	private static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	private static int lerpColor(int c1, int c2, float t) {
		t = clamp01(t);
		int rr = (int) (((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
		int gg = (int) (((c1 >>  8) & 0xFF) * (1 - t) + ((c2 >>  8) & 0xFF) * t);
		int bb = (int) ((c1         & 0xFF) * (1 - t) + (c2         & 0xFF) * t);
		return 0xFF000000 | (rr << 16) | (gg << 8) | bb;
	}

	private static int gradient(float[] stops, int[] colors, float v) {
		if (v <= stops[0]) return colors[0];
		for (int i = 1; i < stops.length; i++) {
			if (v <= stops[i]) {
				return lerpColor(colors[i - 1], colors[i],
						(v - stops[i - 1]) / (stops[i] - stops[i - 1]));
			}
		}
		return colors[colors.length - 1];
	}

	private static int lighten(int c) {
		return lerpColor(c, 0xFFFFFFFF, 0.25f);
	}

	private static String pretty(Enum<?> e) {
		String n = e.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
		return Character.toUpperCase(n.charAt(0)) + n.substring(1);
	}

	private static String cardinal(float deg) {
		int idx = Math.round((((deg % 360f) + 360f) % 360f) / 45f) % 8;
		return CARDINALS[idx];
	}

	private static int phaseColor(DayNightCycle.Phase phase) {
		switch (phase) {
			case DAWN:  return COLOR_DAWN;
			case DAY:   return COLOR_DAY;
			case DUSK:  return COLOR_DUSK;
			case NIGHT: default: return COLOR_NIGHT;
		}
	}

	private static int weatherColor(WeatherState ws) {
		if (ws == null) return COLOR_CLEAR;
		switch (ws) {
			case CLEAR:         return COLOR_CLEAR;
			case FAIR:          return COLOR_FAIR;
			case PARTLY_CLOUDY: return COLOR_PARTLY;
			case OVERCAST:      return COLOR_OVERCAST;
			case LIGHT_PRECIP:  return COLOR_LIGHT_PRECIP;
			case HEAVY_PRECIP:  return COLOR_HEAVY_PRECIP;
			case STORM:         return COLOR_STORM;
			case FOG:           return COLOR_FOG;
			case CLEARING:      return COLOR_CLEARING;
			default:            return COLOR_CLEAR;
		}
	}

	private static String weatherName(WeatherState ws) {
		if (ws == null) return "Clear";
		switch (ws) {
			case CLEAR:         return "Clear";
			case FAIR:          return "Fair";
			case PARTLY_CLOUDY: return "Clouds";
			case OVERCAST:      return "Overcast";
			case LIGHT_PRECIP:
			case HEAVY_PRECIP:
				switch (ClimateManager.localPrecipType()) {
					case SNOW:     return "Snow";
					case HAIL:     return "Hail";
					case SLEET:    return "Sleet";
					case BLIZZARD: return "Blizzard";
					default:       return ws == WeatherState.HEAVY_PRECIP ? "Downpour" : "Rain";
				}
			case STORM:         return "Storm";
			case FOG:           return "Fog";
			case CLEARING:      return "Clearing";
			default:            return "Clear";
		}
	}

	private float dayProgress() {
		int pos = Dungeon.cycleTurn % DayNightCycle.FULL_CYCLE;
		return pos / (float) DayNightCycle.FULL_CYCLE;
	}

	private float yearProgress() {
		GameCalendar.Season season = GameCalendar.season();
		float seasonStart;
		switch (season) {
			case SPRING: seasonStart = 0f;    break;
			case SUMMER: seasonStart = 0.25f; break;
			case AUTUMN: seasonStart = 0.50f; break;
			case WINTER: seasonStart = 0.75f; break;
			default:     seasonStart = 0f;    break;
		}
		float withinSeason = (GameCalendar.dayOfSeason() - 1f) / GameCalendar.daysInCurrentSeason();
		return seasonStart + 0.25f * withinSeason;
	}
}
