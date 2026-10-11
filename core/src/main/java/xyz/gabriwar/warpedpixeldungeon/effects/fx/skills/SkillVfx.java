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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;

/**
 * The skills area's common ground: particles made straight into the kit's holders (light in the
 * light layer, matter among the effects, marks on the floor), cells to world points without a
 * PointF, what the hero sees, the overworld window's serial (a slide moves every cell index), the
 * one-frame brighten a cast gives its caster, and the per-frame tally of the cells a ring of
 * impacts has drawn in full. Render thread unless said.
 */
public final class SkillVfx {

	private SkillVfx(){}

	/** Light frames (glows, stars, bubbles, pillars). */
	public static final String LIGHT_SHEET = Assets.Effects.FX_LIGHT;
	/** Painted frames (sparks, motes, puffs, slashes, cracks). */
	public static final String SHEET = Assets.Effects.FX_SHEET;

	// ------------------------------------------------------------------ particles

	/** A light particle (added, over the night's tint) at (x, y), or null when the budget says no
	 *  (P0 never) or there is no scene. */
	public static FxParticle glow( float x, float y, int priority ){
		if (!FxBudget.keep( priority, x, y )) return null;
		FxEmitter e = Fx.glowAir();
		if (e == null) return null;
		return ((FxParticle) e.recycle( FxParticle.class )).reset( x, y ).light( true ).priority( priority );
	}

	/** A particle of matter in the air at (x, y), or null. */
	public static FxParticle matter( float x, float y, int priority ){
		if (!FxBudget.keep( priority, x, y )) return null;
		FxEmitter e = Fx.air();
		if (e == null) return null;
		return ((FxParticle) e.recycle( FxParticle.class )).reset( x, y ).priority( priority );
	}

	private static FxEmitter floor;

	/** A particle lying on the floor, under everyone (a ground glint, a crack's dust), or null. */
	public static FxParticle onFloor( float x, float y, int priority ){
		if (!FxBudget.keep( priority, x, y )) return null;
		Group layer = GameScene.layer( GameScene.Layer.FLOOR );
		if (layer == null) return null;
		if (floor == null || !floor.exists || floor.parent != layer){
			floor = GameScene.floorFxEmitter();
			if (floor == null) return null;
			floor.on = false;
			floor.autoKill = false;
		}
		return ((FxParticle) floor.recycle( FxParticle.class )).reset( x, y ).priority( priority );
	}

	/** A colour of a ramp, light lerped (at) or matter stepped (step). */
	public static int tone( Ramp r, float p, boolean light ){
		return light ? r.at( p ) : r.step( p );
	}

	/** A visual lit in a colour (hardlight: keeps a matter frame's shading). */
	public static void lit( com.watabou.noosa.Visual v, int color ){
		v.hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
	}

	// ------------------------------------------------------------------ the world

	/** A cell's middle, x. */
	public static float cx( int cell ){
		Level l = Dungeon.level;
		return l == null ? 0 : (cell % l.width() + 0.5f) * DungeonTilemap.SIZE;
	}

	/** A cell's middle, y. */
	public static float cy( int cell ){
		Level l = Dungeon.level;
		return l == null ? 0 : (cell / l.width() + 0.5f) * DungeonTilemap.SIZE;
	}

	/** Where a sprite stands on a cell (raisedTileCenterToWorld), y. */
	public static float raisedY( int cell ){
		Level l = Dungeon.level;
		return l == null ? 0 : (cell / l.width() + 0.1f) * DungeonTilemap.SIZE;
	}

	/** Whether the cell lies on the level. */
	public static boolean valid( int cell ){
		Level l = Dungeon.level;
		return l != null && cell >= 0 && cell < l.length();
	}

	/** Whether the hero sees the cell now (false off the level). */
	public static boolean seen( int cell ){
		Level l = Dungeon.level;
		return l != null && l.heroFOV != null && cell >= 0 && cell < l.length() && l.heroFOV[cell];
	}

	/** Whether the cell is water (deep or shallow). */
	public static boolean water( int cell ){
		Level l = Dungeon.level;
		return l != null && cell >= 0 && cell < l.length() && l.map[cell] == Terrain.WATER;
	}

	/** Whether grass grows on the cell. */
	public static boolean grass( int cell ){
		Level l = Dungeon.level;
		if (l == null || cell < 0 || cell >= l.length()) return false;
		int t = l.map[cell];
		return t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS;
	}

	/** The colour of the hero's class (his afterimages', his casts' when they have none). */
	public static int classColor(){
		if (Dungeon.hero == null) return 0xB9DEEE;
		switch (Dungeon.hero.heroClass){
			case WARRIOR:  return School.MARTIAL.color;
			case MAGE:     return School.ARCANE.color;
			case ROGUE:    return School.SHADOW.color;
			case HUNTRESS: return School.NATURE.color;
			case CLERIC:   return School.HOLY.color;
			default:       return School.STEEL.color;
		}
	}

	/** The hero's layer (his sprite's group: the mobs), null off the scene. */
	public static Group heroLayer(){
		return Dungeon.hero != null && Dungeon.hero.sprite != null ? Dungeon.hero.sprite.parent : null;
	}

	/** The overworld window's serial: where its (0, 0) lies in the world. A slide changes it, and
	 *  every cell index with it; 0 on any other level. Any thread. */
	public static long serial(){
		Level l = Dungeon.level;
		if (!(l instanceof OverworldLevel)) return 0;
		OverworldLevel o = (OverworldLevel) l;
		return ((long) o.worldX << 32) ^ (o.worldY & 0xFFFFFFFFL);
	}

	/** A sprite's middle, x (no allocation, unlike Visual.center()). */
	public static float midX( CharSprite s ){
		return s.x + s.width() / 2f;
	}

	/** A sprite's middle, y. */
	public static float midY( CharSprite s ){
		return s.y + s.height() / 2f;
	}

	// ------------------------------------------------------------------ busy frames

	private static float tallyAt = -1;
	private static int tally;

	/** One more cell of a wide effect (a ring of impacts, a whirl) this frame: true while it is
	 *  among the first `full` of them, which get every layer; the rest get only their shape. */
	public static boolean richThisFrame( int full ){
		if (Game.timeTotal != tallyAt){
			tallyAt = Game.timeTotal;
			tally = 0;
		}
		return ++tally <= full;
	}

	// ------------------------------------------------------------------ the caster's brighten

	/** The caster lit up for one frame (its additive colour raised, never a screen flash): a
	 *  brighten that steps aside when the sprite already shows something (a flash, a tint). */
	public static void brighten( final CharSprite s, final int color, final float strength ){
		if (s == null || s.parent == null) return;
		if (s.ra != 0 || s.ga != 0 || s.ba != 0) return;
		final float r = ((color >> 16) & 0xFF) / 255f * strength;
		final float g = ((color >> 8) & 0xFF) / 255f * strength;
		final float b = (color & 0xFF) / 255f * strength;
		s.ra = r;
		s.ga = g;
		s.ba = b;
		s.parent.add( new Gizmo(){
			private int frames;
			@Override
			public void update(){
				//the frame after it was drawn: back as it was, unless something else has since lit it
				if (++frames < 2) return;
				if (s.ra == r && s.ga == g && s.ba == b){
					s.ra = s.ga = s.ba = 0;
				}
				killAndErase();
			}
		} );
	}
}
