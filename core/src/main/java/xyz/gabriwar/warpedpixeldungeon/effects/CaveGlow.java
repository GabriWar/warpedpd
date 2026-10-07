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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

/**
 * The caves' own light (levels/overworld/CaveLife): a fungus patch glowing softly, a crystal
 * of a seam glinting, a vein of rich ore catching the eye. The HearthLight pattern - a soft
 * additive halo above the fog and the caves' darkness, eased on and off - with the glowing
 * thing itself laid where it is drawn:
 * - the fungus's caps, lit in the glow's colour, lie on the ground UNDER everyone
 *   (GameScene.floorEffect): a hero standing on the patch is never painted over;
 * - the crystal's brightest facets (read off the crystal the caves draw, effects/layer_critters
 *   .png), over the fog: a crystal is solid, nobody stands in it.
 * Crystals and ore strike a little star of light now and then. All of it shows where the hero
 * sees the cell or has seen it, never over unexplored black.
 */
public class CaveGlow extends Image {

	public static final int MUSHROOM = 0, CRYSTAL = 1, GEM = 2;

	//the brightest pixels of the crystal as the caves draw it, from its cell's top-left (the
	//tip stands up into the cell above): where its sparks are struck
	private static final int[][] FACETS = { { 8, -4 }, { 7, -2 }, { 6, 1 }, { 5, 3 }, { 4, 6 }, { 11, 7 }, { 2, 4 }, { 7, 8 } };

	public final int kind;
	public final Image halo;
	private Image spark;
	//the face is the facet mask laid from the cell above (a blue crystal), or the cap mask on the
	//cell, or nothing (a crystal of another colour, a vein: the halo and the sparks only)
	private final boolean face, tall;
	private final int sparkColour;
	private final float a;
	private float t, level, target, vis, sparkIn, sparkT = -1f;
	private int sparkX, sparkY;
	private boolean out;

	/**
	 * A glow for the cell whose top-left is (cx, cy) in scene pixels. `facets`: the crystal is
	 * the blue one the facet mask was read off (another colour gets no facets and white sparks).
	 */
	public CaveGlow( int kind, int colour, float cx, float cy, boolean facets ){
		super( Assets.Effects.LAYER_CRITTERS );
		this.kind = kind;
		tall = kind == CRYSTAL && facets;
		face = kind == MUSHROOM || tall;
		if (tall){
			frame( 64, 64, 16, 32 );
			x = cx;
			y = cy - 16;
		} else {
			frame( 64, 48, 16, 16 );
			x = cx;
			y = cy;
		}
		visible = face;
		hardlight( colour );
		alpha( 0 );
		halo = kind == MUSHROOM ? new Additive( 0, 64, 32, 32 ) : new Additive( 32, 64, 24, 32 );
		halo.hardlight( colour );
		halo.alpha( 0 );
		halo.x = cx - 4;
		halo.y = cy + (kind == MUSHROOM ? -6 : kind == CRYSTAL ? -14 : -8);
		sparkColour = kind == CRYSTAL && !facets ? 0xFFFFFF : mix( colour, 0xFFFFFF, 0.6f );
		a = Random.Float( 6.283f );
		sparkIn = Random.Float( 1f, 5f );
	}

	/** Puts it on the scene: the halo above the fog, the caps under everyone, the facets over the fog. */
	public void show(){
		GameScene.effectOverFog( halo );
		if (kind == MUSHROOM) GameScene.floorEffect( this );
		else GameScene.effectOverFog( this );
	}

	/** How brightly to glow, 0..1. */
	public void target( float level ){
		target = level;
		out = false;
	}

	/** Fades out and takes itself off the scene (target() before then lights it again). */
	public void putOut(){
		target = 0;
		out = true;
	}

	/** Out at once: what it lit is gone (a crystal mined, a patch burnt). */
	public void extinguish(){
		level = target = 0;
		takeOff();
	}

	/** Moves it, its halo and its spark by (dx, dy) scene pixels: a network mirror's window was
	 *  re-labelled under it without a slide of the scene. */
	public void slide( float dx, float dy ){
		x += dx;
		y += dy;
		halo.x += dx;
		halo.y += dy;
		if (spark != null){
			spark.x += dx;
			spark.y += dy;
		}
	}

	//the cell's top-left, wherever a slide has carried it
	private float cellX(){ return x; }
	private float cellY(){ return tall ? y + 16 : y; }

	@Override
	public void update(){
		super.update();
		float dt = Game.elapsed;
		t += dt;
		level += (target - level) * Math.min( 1f, dt * 0.8f );
		if (out && level < 0.01f){
			takeOff();
			return;
		}
		float f = kind == MUSHROOM
				? 0.78f + 0.22f * (float)Math.sin( 6.283f * t / 3.4f + a )
				: 0.6f + 0.4f * (0.5f + 0.5f * (float)Math.sin( 6.283f * t / 6.5f + a ));
		vis += (visibility() - vis) * Math.min( 1f, dt * 4f );
		float on = level * vis * f;
		alpha( Math.min( 1f, on * (kind == MUSHROOM ? 0.85f : 0.55f) ) );
		halo.alpha( Math.min( 1f, on * (kind == MUSHROOM ? 0.45f : 0.28f) ) );
		if (kind != MUSHROOM) glint( dt );
	}

	//now and then a little star of light on the crystal or the ore, while it is plainly seen
	private void glint( float dt ){
		if (sparkT >= 0f){
			sparkT += dt;
			float p = sparkT / 0.5f;
			if (p >= 1f){
				sparkT = -1f;
				spark.visible = false;
				return;
			}
			int[] frames = { 0, 1, 2, 1 };
			spark.frame( frames[Math.min( 3, (int)(p * 4) )] * 16, 48, 16, 16 );
			spark.x = cellX() + sparkX - 8;
			spark.y = cellY() + sparkY - 8;
			spark.alpha( level * vis * (1f - 0.3f * p) );
			return;
		}
		if (vis < 0.5f || (sparkIn -= dt) > 0f) return;
		sparkIn = Random.Float( 3.5f, 8f );
		if (kind == CRYSTAL){
			int[] p = FACETS[Random.Int( FACETS.length )];
			sparkX = p[0];
			sparkY = p[1];
		} else {
			sparkX = Random.IntRange( 4, 11 );
			sparkY = Random.IntRange( 4, 11 );
		}
		if (spark == null){
			spark = new Additive( 0, 48, 16, 16 );
			spark.hardlight( sparkColour );
			GameScene.effectOverFog( spark );
		}
		spark.visible = true;
		sparkT = 0f;
		spark.alpha( 0 );
	}

	private float visibility(){
		int w = Dungeon.level != null ? Dungeon.level.width() : 0;
		float cx = cellX() + 8, cy = cellY() + 8;
		if (w == 0 || cx < 0 || cy < 0) return 0f;
		return HearthLight.groundSeen( (int)(cx / DungeonTilemap.SIZE) + (int)(cy / DungeonTilemap.SIZE) * w );
	}

	//off the scene for good, its vertex buffers freed with it (the world is one long scene)
	private void takeOff(){
		halo.killAndErase();
		halo.destroy();
		if (spark != null){
			spark.killAndErase();
			spark.destroy();
		}
		killAndErase();
		destroy();
	}

	private static int mix( int a, int b, float p ){
		int r = (int)(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * p);
		int g = (int)(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * p);
		int bl = (int)((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * p);
		return (r << 16) | (g << 8) | bl;
	}

	//light added to what lies under it
	private static class Additive extends Image {
		Additive( int left, int top, int width, int height ){
			super( Assets.Effects.LAYER_CRITTERS, left, top, width, height );
		}

		@Override
		public void draw(){
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}
}
