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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.DreadFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * The rogue's shadow decoy (SkillDecoy: Stealth's double, Ninja Bomb's clone): the hero's own
 * figure, as a Mirror Image wears it, but dark - hardlit in the dread's violet (6B4F94) at 0.6 - and
 * shedding wisps of ink as it stands, so it reads as a shade of shadow and never as a Mirror Image
 * (a scroll's) or as the hero himself. Its colour is its own (a blow's flash gives it back), and a
 * co-op guest finds it by its name as any sprite (NetSprites).
 *
 * The ink is an FxEmitter (never the game's Random) pouring SkillBursts.INK_DRIFT every INK_EVERY
 * s over the figure, made on its first frame on the render thread; it stops when the decoy dies and
 * is let go of (Fx.dispose) with the sprite. One emitter; the wisps through FxBudget (P3).
 */
public class DecoySprite extends MirrorSprite {

	/** The shade's colour and alpha; how often it sheds a wisp, s. */
	public static final int COLOR = ShoutFX.DREAD[3];
	public static final float ALPHA = 0.6f, INK_EVERY = 0.6f;

	private FxEmitter ink;
	private boolean dead;

	public DecoySprite(){
		super();
		shade();
	}

	//dark violet, see-through
	private void shade(){
		hardlight( COLOR );
		alpha( ALPHA );
	}

	@Override
	public void resetColor(){
		super.resetColor();
		shade();
	}

	/** The ink it sheds (null before its first frame or once let go of). */
	public FxEmitter ink(){
		return ink;
	}

	@Override
	public void update(){
		super.update();
		if (ink == null && !dead && parent != null && GameScene.layer( GameScene.Layer.EFFECTS ) != null){
			ink = GameScene.fxEmitter();
			if (ink != null){
				ink.pos( this );
				ink.autoKill = false;
				ink.pour( SkillBursts.INK_DRIFT, INK_EVERY );
			}
		}
		if (ink != null) ink.on = visible && !dead;
	}

	@Override
	public void die(){
		super.die();
		dead = true;
		if (ink != null) ink.on = false;
		//the shade sinks back into the floor it rose from
		if (visible) DreadFX.pool( SkillVfx.midX( this ), y + height() - 2, 0.45f, 0.4f, 0.6f );
	}

	@Override
	public void kill(){
		super.kill();
		let();
	}

	@Override
	public void destroy(){
		super.destroy();
		let();
	}

	private void let(){
		if (ink != null){
			ink.on = false;
			Fx.dispose( ink );
			ink = null;
		}
	}
}
