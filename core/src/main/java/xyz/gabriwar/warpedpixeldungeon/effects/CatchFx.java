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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FireflyParticle;
import xyz.gabriwar.warpedpixeldungeon.items.catching.Critter;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;

/**
 * The bug net's pictures (items/catching/BugNet), on the render thread:
 *
 * - swing: the net itself scoops through the cell it was swung at, its hoop sweeping across the
 *   cell's middle from the hero's side (SWING_TIME), fading as it finishes;
 * - netted: a critter caught, swept into the net - a few motes of its own colour drawn in to where it
 *   was, and gone;
 * - released: a jar opened at the hero's feet: a butterfly flutters off in its colour, a firefly
 *   lifts glowing, a moth or a fly rises away, and what crawls or hops is off in a puff of dust.
 *
 * Its matter draws on the effects' own dice (FxRandom), only as it shows.
 */
public final class CatchFx {

	private CatchFx(){}

	/** The net's sweep, s; how far it turns either side of its middle, degrees. */
	public static final float SWING_TIME = 0.24f, SWING_ARC = 55f;

	/** The net's sweep through a cell. */
	public static void swing( Hero hero, int cell ){
		if (hero.sprite == null || !(Game.scene() instanceof GameScene) || Dungeon.level == null) return;
		GameScene.effect( new Swing( hero, cell ) );
	}

	//the net turning about its handle, the hoop crossing the cell's middle
	private static final class Swing extends ItemSprite {

		private final float from, to;
		private float t;

		Swing( Hero hero, int cell ){
			super( ItemSpriteSheet.BUG_NET );
			int w = Dungeon.level.width();
			float cx = (cell % w + 0.5f) * DungeonTilemap.SIZE, cy = (cell / w + 0.5f) * DungeonTilemap.SIZE;
			//from the hero's side of the cell: the handle's end there, the hoop reaching over the middle
			float hx = (hero.pos % w + 0.5f) * DungeonTilemap.SIZE, hy = (hero.pos / w + 0.5f) * DungeonTilemap.SIZE;
			double toward = Math.atan2( hy - cy, hx - cx );
			float px = cx + (float)Math.cos( toward ) * 9f, py = cy + (float)Math.sin( toward ) * 9f;
			//the art leans its hoop up and to the right of its handle (-45 degrees): turned to the cell
			float base = (float)Math.toDegrees( Math.atan2( cy - py, cx - px ) ) + 45f;
			from = base - SWING_ARC;
			to = base + SWING_ARC;
			origin.set( 1.5f, 14.5f );
			x = px - origin.x;
			y = py - origin.y;
			angle = from;
			alpha( 0.95f );
		}

		@Override
		public void update(){
			super.update();
			t += Game.elapsed;
			float p = Math.min( 1f, t / SWING_TIME );
			//fast through the middle
			float e = p * p * (3f - 2f * p);
			angle = from + (to - from) * e;
			alpha( p < 0.7f ? 0.95f : 0.95f * (1f - p) / 0.3f );
			if (p >= 1f){
				killAndErase();
				destroy();
			}
		}
	}

	/** A critter swept into the net at (x, y), scene px: motes of its colour drawn in, and gone. */
	public static void netted( float x, float y, Critter critter ){
		FxEmitter e = Fx.air();
		if (e == null) return;
		int color = colour( critter );
		for (int i = 0; i < 6; i++){
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			float a = FxRandom.angle(), r = FxRandom.Float( 4f, 7f );
			((FxParticle) e.recycle( FxParticle.class )).reset( x + (float)Math.cos( a ) * r, y + (float)Math.sin( a ) * r )
					.look( FxFrames.MOTE_1 ).paint( color ).light( critter.glow != 0 )
					.vel( -(float)Math.cos( a ) * r / 0.2f, -(float)Math.sin( a ) * r / 0.2f )
					.life( 0.2f ).alpha( 0.95f, 0f, 0.08f ).priority( FxBudget.P2 );
		}
	}

	/** A jar let open at the hero's feet: the critter is off. */
	public static void released( Hero hero, Critter critter ){
		if (hero.sprite == null || !(Game.scene() instanceof GameScene)) return;
		float x = hero.sprite.x + hero.sprite.width() / 2f, y = hero.sprite.y + hero.sprite.height() - 2f;
		int colour = critter.butterflyColour();
		if (colour >= 0){
			//a butterfly of its own colour flutters off about its new patch
			GameScene.effect( new CritterSprite.Butterfly( x + FxRandom.Float( -3f, 3f ), y - 2f, colour ) );
			return;
		}
		if (critter == Critter.FIREFLY){
			Emitter em = GameScene.emitter();
			if (em != null){
				em.pos( x - 3f, y - 10f, 6f, 6f );
				em.burst( FireflyParticle.FACTORY, 1 );
			}
			return;
		}
		FxEmitter e = Fx.air();
		if (e == null) return;
		boolean flies = critter.jar == Critter.Jar.WINGS;
		int color = colour( critter );
		for (int i = 0; i < 5; i++){
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			FxParticle p = ((FxParticle) e.recycle( FxParticle.class )).reset( x + FxRandom.Float( -4f, 4f ), y - (flies ? 6f : 1f) );
			if (flies){
				//it rises off, a flutter of its colour
				p.look( FxFrames.MOTE_2 ).paint( color ).light( critter.glow != 0 ).vel( FxRandom.Float( -10f, 10f ), -FxRandom.Float( 18f, 30f ) )
						.life( FxRandom.Float( 0.5f, 0.8f ) ).alpha( 0.9f, 0f, 0.25f );
			} else {
				//dust where it scuttled or hopped off
				p.look( FxFrames.PEBBLE_2 ).paint( 0x9C8A70 ).vel( FxRandom.Float( -16f, 16f ), -FxRandom.Float( 6f, 14f ) ).gravity( 60f )
						.life( FxRandom.Float( 0.3f, 0.5f ) ).alpha( 0.7f, 0f, 0.2f );
			}
			p.priority( FxBudget.P2 );
		}
	}

	/** The colour a critter shows in the net's pictures. */
	public static int colour( Critter critter ){
		int b = critter.butterflyColour();
		if (b >= 0) return CritterSprite.Butterfly.COLOURS[b];
		if (critter.glow != 0) return critter.glow;
		switch (critter){
			case MOTH:        return 0xF2E8C8;
			case CINDER_MOTH: return 0xF09848;
			case FROST_MOTH:  return 0xA8D2FA;
			case FLY:         return 0x6A7078;
			case BEETLE:      return 0x5A4632;
			case ROACH:       return 0x7A4E2A;
			case CENTIPEDE:   return 0x9A4A2A;
			case SILVERFISH:  return 0xB8C0C8;
			case SPIDER: case SPIDERLING: return 0x4A4450;
			case SNAIL:       return 0xB08A58;
			case STRIDER:     return 0x58524A;
			case FROG:        return 0x6AA040;
			case NEWT:        return 0xC87838;
			case LIZARD:      return 0x7AA050;
			default:          return 0xD8D0C0;
		}
	}
}
