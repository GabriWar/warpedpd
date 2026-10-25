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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;

/**
 * A lighthouse's lamp at night (tiles/SeaArt lays one on every lighthouse of the window): a warm glow
 * in its glass and two beams sweeping round from it, slowly and steadily, over the fog - a sailor sees
 * the light from as far as the world is drawn. It comes up as the dusk darkens and goes as the dawn
 * comes (the night's tint, GameScene.nightTintAlpha), never flickers, and keeps to its world cell
 * through every slide of the window. The beams turn on the scene's clock (Game.timeTotal) from a phase
 * of their own cell: the art lays the lamps afresh whenever it is laid again (a slide, a boat moored),
 * and a new lamp takes up the turn where the last one was.
 */
public class LighthouseLamp extends Group {

	/** A full turn of the beams, s. */
	public static final float PERIOD = 9f;
	//the night's tint at which the lamp burns its brightest, and the glow's and the beams' peaks
	static final float FULL_DARK = 0.45f, GLOW_ALPHA = 0.7f, BEAM_ALPHA = 0.3f;
	/** The light's colour: lamp oil's warm white. */
	public static final int COLOR = 0xFFE6A8;

	private final int wx, wy;
	private final Image glow, beamA, beamB;
	//where the beams stand at the scene's start: every window agrees
	private final float phase;

	public LighthouseLamp( int wx, int wy ){
		this.wx = wx;
		this.wy = wy;
		glow = new Additive( Assets.Effects.FX_LIGHT );
		int[] g = FxFrames.Light.GLOW_23;
		glow.frame( g[0], g[1], g[2], g[3] );
		glow.hardlight( COLOR );
		add( glow );
		beamA = beam();
		beamB = beam();
		add( beamA );
		add( beamB );
		phase = Math.floorMod( wx * 37 + wy * 101, 360 );
		//placed once, in pixels: a world-anchored visual, slid with the rest by a rebase's render block
		//(GameScene.shiftWorldVisuals), which also lays the lamps again. worked out each frame from the
		//window's origin it jumped a slide ahead while the actors moved the origin before that block
		place();
		//lit as it stands now from its very first frame: never a frame of the images' own full white
		shine();
	}

	private static Image beam(){
		Image b = new Additive( "effects/sea_beam.png" );
		b.origin.set( 0, b.height / 2f );
		b.hardlight( COLOR );
		return b;
	}

	//where the glass is on the screen: the lamp room's cell, its upper middle
	private void place(){
		if (!(Dungeon.level instanceof OverworldLevel)) return;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		float cx = (wx - ow.worldX + 0.5f) * DungeonTilemap.SIZE;
		float cy = (wy - ow.worldY) * DungeonTilemap.SIZE + 6f;
		glow.x = cx - glow.width / 2f;
		glow.y = cy - glow.height / 2f;
		beamA.x = beamB.x = cx;
		beamA.y = beamB.y = cy - beamA.height / 2f;
	}

	@Override
	public void update(){
		super.update();
		shine();
	}

	//the glow and the beams as the night and the clock have them now
	private void shine(){
		float dark = Math.min( 1f, GameScene.nightTintAlpha() / FULL_DARK );
		visible = dark > 0.02f && Dungeon.level instanceof OverworldLevel;
		if (!visible) return;
		float angle = (phase + 360f * (Game.timeTotal % PERIOD) / PERIOD) % 360f;
		glow.alpha( GLOW_ALPHA * dark );
		beamA.alpha( BEAM_ALPHA * dark );
		beamB.alpha( BEAM_ALPHA * dark );
		beamA.angle = angle;
		beamB.angle = angle + 180f;
	}

	//drawn adding its light
	private static final class Additive extends Image {
		Additive( Object tx ){
			super( tx );
		}

		@Override
		public void draw(){
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}
}
