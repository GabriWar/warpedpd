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

package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.Nature;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * A leaf, or in spring a petal, tumbling down: it turns over as it falls, swings on its wobble
 * (half its own, half the gust every falling leaf shares) and rides the wind, to come down one to
 * three cells below where it fell from: on the floor it lies a moment and fades, on high grass it
 * fades into the blades, on water it floats a second with a small ring (food, for the fish). By
 * day one turn in ten catches the sun: a pixel's glint. Its colour is the season's, and in an
 * autumn it browns a step (toward C88A40) late in its fall. Hidden where the hero does not see.
 */
public class FallingLeafParticle extends WeatherParticle {

	private static final int[] AUTUMN_COLORS = {
			0xCC6622, 0xDD8833, 0xBB4411, 0xEEAA22, 0x995511
	};

	private static final int[] SPRING_COLORS = {
			0x44BB44, 0x88CC44, 0xFFAABB, 0x66DD66, 0xFF88AA
	};

	private static final int[] SUMMER_COLORS = {
			0x3E7D2E, 0x5A9E3A, 0x78B24A, 0x2F6B25, 0x8FC05A
	};

	/** Where it comes down, cells below where it fell from; how long it lies, floats, fades. */
	public static final int LAND_MIN = 1, LAND_MAX = 3;
	public static final float LIE_MIN = 0.3f, LIE_MAX = 0.6f, FLOAT = 1f, FADE = 0.25f, GRASS_FADE = 0.15f;
	/** The share of its turns that glint by day, and the glint's life. */
	public static final float GLINT_CHANCE = 0.1f, GLINT_LIFE = 0.1f;
	/** Where in its life it browns a step, and how far toward C88A40. */
	public static final float AGES_AT = 0.7f, AGE_STEP = 0.15f;

	/** The leaf colours a season's canopy sheds. */
	public static int[] seasonColors(xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season season) {
		switch (season) {
			case AUTUMN: return AUTUMN_COLORS;
			case SPRING: return SPRING_COLORS;
			default:     return SUMMER_COLORS;
		}
	}

	public static final Emitter.Factory AUTUMN = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			FallingLeafParticle p = (FallingLeafParticle) emitter.recycle(FallingLeafParticle.class);
			p.petal = false;
			p.autumn = true;
			p.leafColor(AUTUMN_COLORS[FxRandom.Int(AUTUMN_COLORS.length)]);
			p.reset(x, y);
		}
	};

	public static final Emitter.Factory SPRING = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			FallingLeafParticle p = (FallingLeafParticle) emitter.recycle(FallingLeafParticle.class);
			int c = SPRING_COLORS[FxRandom.Int(SPRING_COLORS.length)];
			//the pinks are blossom, the greens young leaves
			p.petal = (c & 0xFF0000) == 0xFF0000;
			p.autumn = false;
			p.leafColor(c);
			p.reset(x, y);
		}
	};

	private static final int FALL = 0, LIE = 1, FLOAT_ON = 2;

	private float wobblePhase;
	private float windBias;
	private float tumble;
	private int face;
	private boolean petal, autumn, aged;
	private int color;
	private int state;
	private float groundY;

	public FallingLeafParticle() {
		super();
		lifespan = FxRandom.Float(3f, 5f);
	}

	/** Its colour: the frame is shaded grey, so it is lit (hardlight) rather than flattened. */
	public void leafColor(int c) {
		color = c & 0xFFFFFF;
		hardlight(color);
	}

	public void reset(float x, float y) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = FxRandom.Float(3f, 5f);
		face = FxRandom.Int(WeatherSprites.LEAVES.length);
		frame(petal ? WeatherSprites.PETAL : WeatherSprites.LEAVES[face]);
		tumble = FxRandom.Float(0.12f, 0.3f);
		state = FALL;
		aged = false;
		//it comes down one to three cells below where it fell from
		groundY = y + DungeonTilemap.SIZE * FxRandom.IntRange(LAND_MIN, LAND_MAX) + FxRandom.Float(-4f, 4f);

		windBias = ClimateManager.localWindSpeed() * 1.0f;
		speed.set(FxRandom.Float(-6, 6) + windBias, petal ? FxRandom.Float(5, 10) : FxRandom.Float(8, 16));
		acc.set(0, FxRandom.Float(4, 8));
		wobblePhase = FxRandom.Float((float)(Math.PI * 2));
	}

	/**
	 * A leaf torn off a canopy by the wind: carried the way the wind blows
	 * (surfaceWindDir, compass degrees, the direction it blows TOWARDS),
	 * with only a lazy fall of its own.
	 */
	public void resetWind(float x, float y) {
		reset(x, y);
		float wind = ClimateManager.surfaceWindSpeed();
		float windRad = (float) Math.toRadians(ClimateManager.surfaceWindDir());
		windBias = (float) Math.sin(windRad) * wind * 1.5f;
		float windY = -(float) Math.cos(windRad) * wind * 0.6f;
		speed.set(FxRandom.Float(-4, 4) + windBias, FxRandom.Float(3, 9) + windY);
		acc.set(0, FxRandom.Float(1, 3));
	}

	@Override
	public void update() {
		super.update();
		if (!alive) return;
		float dt = Game.elapsed;
		if (state == FALL) {
			wobblePhase += dt * 3f;
			float gust = WeatherSprites.gust();
			//half its own swing, half the gust every falling leaf shares
			speed.x = (0.5f * (float) Math.sin(wobblePhase) + 0.5f * gust) * 10f + windBias * (1f + 0.4f * gust);
			//it turns over as it swings: another face, or the same one mirrored
			tumble -= dt;
			if (tumble <= 0) {
				tumble = FxRandom.Float(0.12f, 0.3f);
				if (!petal && FxRandom.Float() < 0.5f) {
					face = (face + 1 + FxRandom.Int(WeatherSprites.LEAVES.length - 1)) % WeatherSprites.LEAVES.length;
					frame(WeatherSprites.LEAVES[face]);
				}
				scale.x = -scale.x;
				if (Nature.day() && FxRandom.chance(GLINT_CHANCE)) glint();
			}
			if (!aged && autumnal() && left < (1f - AGES_AT) * lifespan) {
				aged = true;
				hardlight(Ramp.lerp(color, Nature.AUTUMN, AGE_STEP));
			}
			am = envelope(0.2f, 0.1f, 1f);
			if (speed.y > 0 && y >= groundY) land();
		} else {
			//lying or floating (drifting): its last FADE s in painted steps
			float k = left / FADE;
			am = k >= 1f ? 1f : k > 0.75f ? 1f : k > 0.5f ? 0.6f : k > 0.25f ? 0.3f : 0f;
		}
		fov();
		if (state != FALL || Math.abs(speed.x) < 12f && Math.abs(speed.y) < 12f) {
			x = Math.round(x);
			y = Math.round(y);
		}
	}

	//autumn leaves, or any leaf under an autumn sky
	private boolean autumnal() {
		return autumn || Nature.autumn();
	}

	//a pixel of sunlight caught on a turn
	private void glint() {
		Emitter g = Nature.glow(null);
		if (g == null || !FxBudget.keep(FxBudget.P3, x, y)) return;
		Nature.make(g, x + width / 2f, y + height / 2f).look(FxFrames.MOTE_1).paint(0xFFFFF0).light(true)
				.life(GLINT_LIFE).alpha(0.9f, 0, 0.05f).snap().priority(FxBudget.P3);
	}

	//down on its ground: lying, fading into high grass, or floating on water
	private void land() {
		float cx = x + width / 2f, cy = y + height / 2f;
		speed.set(0, 0);
		acc.set(0, 0);
		Level level = Dungeon.level;
		int cell = Fx.cellAt(cx, cy);
		if (WaterFX.onWater(cx, cy)) {
			state = FLOAT_ON;
			speed.x = FxRandom.Float(-2f, 2f);
			WaterFX.drop(cx, cy, 0.15f, Element.NONE);
			WaterFX.disturb(cx, cy, 24f, true);
			left = Math.min(left, FLOAT + FADE);
		} else if (level != null && cell >= 0 && cell < level.length() && level.map[cell] == Terrain.HIGH_GRASS) {
			state = LIE;
			left = Math.min(left, GRASS_FADE);
		} else {
			state = LIE;
			left = Math.min(left, FxRandom.Float(LIE_MIN, LIE_MAX) + FADE);
		}
	}
}
