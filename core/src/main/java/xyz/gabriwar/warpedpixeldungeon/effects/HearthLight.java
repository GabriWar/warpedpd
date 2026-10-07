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
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.CaveLife;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SettlementLights;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

/**
 * A lit window or a burning fire ring after dark: the glass (or the logs) in warm light,
 * laid exactly over the tile it lights, and a soft additive halo spilling round it. Both
 * live above the fog and the night's tint (GameScene.effectOverFog, halo first), so a lamp
 * is as bright at midnight as a lamp should be; they show where the hero sees the place or
 * has seen it, never over unexplored black. The flame flickers gently and now and then a
 * draught catches it - a hearth, not a strobe. SettlementAmbience makes, steers and puts
 * them out.
 */
public class HearthLight extends Image {

	//where each kind's halo sits over its 16x16 frame, centring it on the glass or the logs
	//(effects/settlement_lights.png: the window halo is 16x16, the hearth's 32x16). kinds 7-11
	//are the caves' lights (CaveSites): a lantern's glass, a campfire, a furnace's mouth, a giant
	//mushroom's spots, a crystal cavern's shards - centred on the pixels tools/cave_sites_gen.py lights;
	//kind 12, a violet giant's spots, is kind 10's mask in its own colour
	private static final int[][] HALO_OFFSET = { { 0, 2 }, { 0, 2 }, { 0, -1 }, { 0, -1 }, { 0, 0 }, { 0, -1 }, { -8, -1 },
			{ -2, 2 }, { -8, 0 }, { -8, 1 }, { -1, -2 }, { -2, 1 }, { -1, -2 } };

	public final Image halo;
	//a fire (the hearth's halo, its flicker and its tint), a mushroom's cold spore light, the
	//crystal shards' colder one, and whether it lights open ground (seen as ground is) or a wall's face
	private final boolean hearth, spores, shards, ground;

	//the flame's own phases, its time, and the draughts that catch it
	private final float a, b, c;
	private float t, gutterIn, gutterLeft;
	//how far lit (eases to the target, the way a lamp is lit), how plainly seen
	private float level, target, vis;
	private boolean out;

	public HearthLight( int kind, float x, float y ){
		//kinds 0-7 on the sheet's first row, 8 and on on its third
		super( Assets.Effects.SETTLEMENT_LIGHTS, (frame( kind ) % 8) * 16, frame( kind ) < 8 ? 0 : 32, 16, 16 );
		hearth = kind == SettlementLights.KIND_HEARTH || kind == SettlementLights.KIND_CAMPFIRE
				|| kind == SettlementLights.KIND_FURNACE;
		boolean violet = kind == SettlementLights.KIND_SPORES_VIOLET;
		spores = kind == SettlementLights.KIND_SPORES || violet;
		shards = kind == SettlementLights.KIND_SHARDS;
		ground = kind >= SettlementLights.KIND_HEARTH;
		this.x = x;
		this.y = y;
		//the shards glow in the colour of their cavern's crystals, which changes with the slice
		int alt = Dungeon.level instanceof OverworldLevel ? ((OverworldLevel) Dungeon.level).altitude() : -1;
		hardlight( hearth ? 0xFF7A30 : violet ? 0xE0C0FF : spores ? 0x8FFFE8 : shards ? CaveLife.shardColour( alt ) : 0xFFD47A );
		alpha( 0 );
		halo = new Glow( hearth ? 16 : 0, 16, hearth ? 32 : 16, 16 );
		halo.hardlight( hearth ? 0xFF8A3C : violet ? 0x9C6CFF : spores ? 0x40D8C8 : shards ? CaveLife.shardHaloColour( alt ) : 0xFFB050 );
		halo.alpha( 0 );
		halo.x = x + HALO_OFFSET[kind][0];
		halo.y = y + HALO_OFFSET[kind][1];
		a = Random.Float( 6.283f );
		b = Random.Float( 6.283f );
		c = Random.Float( 6.283f );
		gutterIn = Random.Float( 2f, 9f );
	}

	//the sheet frame a kind draws: its own, but a violet giant's spots are the teal one's mask
	private static int frame( int kind ){
		return kind == SettlementLights.KIND_SPORES_VIOLET ? SettlementLights.KIND_SPORES : kind;
	}

	/** How brightly to burn, 0..1 (SettlementLights.glowLevel). */
	public void target( float level ){
		target = level;
		out = false;
	}

	/** Fades out and takes itself and its halo off the scene (target() before then lights
	 *  it again). */
	public void putOut(){
		target = 0;
		out = true;
	}

	/** Moves it and its halo by (dx, dy) scene pixels: a network mirror's window was
	 *  re-labelled under it without a slide of the scene (SettlementAmbience.Mirror). */
	public void slide( float dx, float dy ){
		x += dx;
		y += dy;
		halo.x += dx;
		halo.y += dy;
	}

	@Override
	public void update(){
		super.update();
		float dt = Game.elapsed;
		t += dt;
		//about a second and a bit to come on, like a lamp being lit
		level += (target - level) * Math.min( 1f, dt * 0.8f );
		if (out && level < 0.01f){
			//gone for good, its vertex buffers freed with it: an erased image keeps them till
			//the next scene change, and the surface is one long scene
			halo.killAndErase();
			halo.destroy();
			killAndErase();
			destroy();
			return;
		}
		//a fire leaps, a spore glow or a crystal's barely breathes
		float amp = hearth ? 2.2f : spores || shards ? 0.5f : 1f;
		float f = 0.92f + amp * (0.035f * (float)Math.sin( 1.7f * t + a )
				+ 0.025f * (float)Math.sin( 4.3f * t + b ) + 0.015f * (float)Math.sin( 9.1f * t + c ));
		//a draught catches the flame now and then
		if (gutterLeft > 0){
			gutterLeft -= dt;
			f -= amp * 0.10f * (float)Math.sin( Math.PI * (1f - Math.max( 0f, gutterLeft ) / 0.45f) );
		} else if ((gutterIn -= dt) <= 0){
			gutterLeft = 0.45f;
			gutterIn = Random.Float( 3f, 9f );
		}
		f = Math.max( 0f, Math.min( 1f, f ) );
		vis += (visibility() - vis) * Math.min( 1f, dt * 4f );
		float on = level * f * vis;
		alpha( Math.min( 1f, on * (hearth ? 0.9f : spores || shards ? 0.7f : 0.85f) ) );
		halo.alpha( Math.min( 1f, on * (hearth ? 0.55f : spores || shards ? 0.45f : 0.5f) ) );
	}

	//read off the cell under the light's own position, so a window slide never leaves it stale
	private float visibility(){
		int w = Dungeon.level != null ? Dungeon.level.width() : 0;
		if (w == 0 || x < 0 || y < 0) return 0f;
		int cell = (int)((x + 8) / DungeonTilemap.SIZE) + (int)((y + 8) / DungeonTilemap.SIZE) * w;
		return ground ? groundSeen( cell ) : faceSeen( cell );
	}

	/** How plainly the hero sees a camera-facing wall (a window, a house front): 1 in view,
	 *  0.8 explored, 0 never seen. The face counts only from outside, so the cell in front of
	 *  it must be seen too - from within a house the wall shows its black back. */
	public static float faceSeen( int cell ){
		Level level = Dungeon.level;
		if (level == null) return 0f;
		boolean[] fov = level.heroFOV, visited = level.visited;
		int front = cell + level.width();
		if (fov == null || visited == null || cell < 0 || front >= fov.length || front >= visited.length) return 0f;
		if (fov[cell] && fov[front]) return 1f;
		return visited[cell] && visited[front] ? 0.8f : 0f;
	}

	/** The same for open ground: 1 in view, 0.8 explored, 0 never seen. */
	public static float groundSeen( int cell ){
		Level level = Dungeon.level;
		if (level == null) return 0f;
		boolean[] fov = level.heroFOV, visited = level.visited;
		if (fov == null || visited == null || cell < 0 || cell >= fov.length || cell >= visited.length) return 0f;
		return fov[cell] ? 1f : visited[cell] ? 0.8f : 0f;
	}

	//the halo: light added to what lies under it
	private static class Glow extends Image {
		Glow( int left, int top, int width, int height ){
			super( Assets.Effects.SETTLEMENT_LIGHTS, left, top, width, height );
		}

		@Override
		public void draw(){
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}
}
