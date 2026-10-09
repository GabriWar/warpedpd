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

import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.List;

/**
 * One area of the effects (water and its life, lightning, fire, ...), as the game's scene and the
 * gallery see it: the hooks the kit calls for it on every sprite and every change of terrain, its
 * page of the effects gallery and its debug scenes. Each area's module lives in effects/fx/modules
 * and is the one place its builder wires itself into the scene, so the areas never edit each
 * other's files (FxModules.ALL lists them).
 *
 * The sprite hooks run for every sprite linked to a character (a gallery puppet's too: its
 * character is a detached dummy), on whichever thread moved it - the actor thread, mostly - so
 * they stay quick and touch nothing of the GPU.
 */
public interface FxModule {

	/** Its key: the area's name, the gallery page's and its scene's (fx-gallery-<key>). */
	String key();

	/** Its gallery page's title. */
	String title();

	/** The game's scene was made, every layer standing (GameScene.create). */
	default void sceneCreated( GameScene scene ){}

	/** A sprite stepped from one cell to the next (CharSprite.move). */
	default void stepped( CharSprite s, int from, int to ){}

	/** A sprite landed from a jump (CharSprite.jump), on its character's cell. */
	default void jumped( CharSprite s ){}

	/** A sprite was just drawn (CharSprite.draw): what lies over it, drawn right after it. */
	default void afterDraw( CharSprite s ){}

	/** A sprite was killed (CharSprite.kill, any thread): let go of what the area keeps for it in
	 *  its fxSlots (pictures through Fx.dispose or Fx.post, never their buffers off the render thread). */
	default void gone( CharSprite s ){}

	/** A cell's terrain changed, as the render side saw it (TerrainWatch). */
	default void terrainChanged( int cell, int oldTerrain, int newTerrain ){}

	/** Its gallery page: its exhibits, in the order they play. */
	void exhibits( FxGallery.Page page );

	/** Its own debug scenes, if any. */
	default void scenes( List<DebugScenes.Scene> out ){}
}
