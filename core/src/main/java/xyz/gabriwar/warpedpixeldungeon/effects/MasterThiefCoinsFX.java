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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MasterThiefCoins;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;

/**
 * Small gold coins spinning around the hero in the kit's COINS orbit, one per coin held: the
 * painted coin turning (face, edge and back, SPIN_FPS a second), now and then one catching the
 * light (a glint every 2-4 s on the near side), and a clink of sparks each time a coin comes or
 * goes. 6 orbit quads and a glint.
 */
public class MasterThiefCoinsFX extends SkillOrbit {

	public static final int GOLD = 0xF0C040;
	public static final float SPIN_FPS = 8f;
	public static final int CLINKS = 3;

	private final MasterThiefCoins state;
	private float spin, glint = FxRandom.Float( 2f, 4f );
	private int frame, last = -1;

	public MasterThiefCoinsFX( Char owner, MasterThiefCoins state ){
		super( owner, FxOrbit.Slot.COINS );
		this.state = state;
	}

	@Override
	protected int count(){
		return state.coins();
	}

	@Override
	protected void look( FxOrbit.Ring ring, int n ){
		ring.motes( n, FxFrames.COIN_6[frame], GOLD, false ).trail( 0 ).speed( 2.4f );
	}

	@Override
	protected void tick( float dt ){
		int n = count();
		if (last >= 0 && n != last && sprite.visible){
			FxRecipes.sparks( SkillVfx.midX( sprite ), SkillVfx.midY( sprite ) + slot.h, Element.METAL, CLINKS );
		}
		last = n;
		if (n <= 0 || ring == null) return;
		//the coins turn: the orbit's one picture stepped to the next painted face
		spin += dt;
		if (spin >= 1f / SPIN_FPS){
			spin -= 1f / SPIN_FPS;
			frame = (frame + 1) % FxFrames.COIN_6.length;
			ring.motes( n, FxFrames.COIN_6[frame], GOLD, false );
		}
		glint -= dt;
		if (glint <= 0 && sprite.visible){
			glint = FxRandom.Float( 2f, 4f );
			//on the near half, where a coin passes in front
			float t = FxRandom.Float( 0.3f, 2.8f );
			FxParticle p = SkillVfx.glow( ellipseX( t ), ellipseY( t ), FxBudget.P3 );
			if (p != null) p.look( FxFrames.PLUS_3 ).paint( 0xFFF6D0 ).life( 0.2f ).alpha( 1f, 0.04f, 0.12f ).snap();
		}
	}
}
