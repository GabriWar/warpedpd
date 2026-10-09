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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.modules.ArcaneFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.modules.CharactersFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.modules.FireHeatFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.modules.GasesColdFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.modules.LightningFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.modules.NatureHolyFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.modules.SkillsFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.modules.WaterLifeFx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * The effects' areas (FxModule), in the order the gallery shows them, and the kit's calls into
 * them: the game's scene made, a sprite's step and landing, a sprite drawn, a sprite killed, a
 * cell's terrain changed. Each loops the array, allocating nothing. A sprite's own state for an area lives in
 * its CharSprite.fxSlots, at the area's index here; the kit's (its orbits) at KIT.
 */
public final class FxModules {

	private FxModules(){}

	/** How many areas, and their indices (in fxSlots too). */
	public static final int COUNT = 8;
	public static final int WATER_LIFE = 0, LIGHTNING = 1, FIRE_HEAT = 2, ARCANE = 3, NATURE_HOLY = 4,
			GASES_COLD = 5, CHARACTERS = 6, SKILLS = 7;
	/** The kit's own slot in a sprite's fxSlots, after the areas'. */
	public static final int KIT = COUNT;

	public static final FxModule[] ALL = {
			new WaterLifeFx(),
			new LightningFx(),
			new FireHeatFx(),
			new ArcaneFx(),
			new NatureHolyFx(),
			new GasesColdFx(),
			new CharactersFx(),
			new SkillsFx() };

	/** The area of this key, null for none. */
	public static FxModule byKey( String key ){
		for (FxModule m : ALL) if (m.key().equals( key )) return m;
		return null;
	}

	/** The game's scene stands, every layer made. */
	public static void sceneCreated( GameScene scene ){
		for (FxModule m : ALL) m.sceneCreated( scene );
	}

	/** A sprite stepped (any thread): written down for what reacts to steps (Steps), then told to
	 *  every area, for a sprite with a character. */
	public static void stepped( CharSprite s, int from, int to ){
		Steps.record( from, to, s.visible );
		if (s.ch == null) return;
		for (FxModule m : ALL) m.stepped( s, from, to );
	}

	/** A sprite landed from a jump. */
	public static void jumped( CharSprite s ){
		if (s.ch == null) return;
		for (FxModule m : ALL) m.jumped( s );
	}

	/** Right before a sprite is drawn: what lies behind it (its orbits' far halves). */
	public static void beforeDraw( CharSprite s ){
		FxOrbit.drawBehind( s );
	}

	/** Right after a sprite is drawn: what lies over it, its orbits' near halves first. */
	public static void afterDraw( CharSprite s ){
		FxOrbit.drawInFront( s );
		if (s.ch == null) return;
		for (FxModule m : ALL) m.afterDraw( s );
	}

	/** A sprite was killed (CharSprite.kill, any thread): its orbits let go of, every area told. */
	public static void gone( CharSprite s ){
		FxOrbit.release( s );
		for (FxModule m : ALL) m.gone( s );
	}

	/** A cell's terrain changed (TerrainWatch, render thread). */
	public static void terrainChanged( int cell, int oldTerrain, int newTerrain ){
		for (FxModule m : ALL) m.terrainChanged( cell, oldTerrain, newTerrain );
	}
}
