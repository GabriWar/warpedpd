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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.Bundle;

import java.util.List;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ChargedShot;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/**
 * The Huntress holding her ChargedShot draw. She stands and pulls for ten, seven or
 * five turns, the light gathering on her each turn with the count over her head,
 * and on the last turn the lance is loosed along the line she chose, from wherever
 * she stands then.
 */
public class ChargedShotDraw extends Buff {

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	//seconds of real time each turn of the draw holds the screen
	private static final float DRAW_PAUSE = 0.8f;

	private int aim = -1;
	private int turnsLeft = 0;
	private int level = 1;
	private int depth = -1;
	private int branch = -1;

    /** Charging owns the hero's turns, so walking, attacking and item use cannot interleave. */
    public boolean holdTurn(Hero hero){
        if(!onSameFloor()||turnsLeft<=0){detach();return false;}
        hero.curAction=null;hero.busy();hero.spend(TICK);
        if(hero.sprite!=null&&hero.sprite.parent!=null){
            hero.sprite.parent.add(new Delayer(DRAW_PAUSE){
                @Override protected void onComplete(){hero.next();}
            });
        }else hero.next();
        return true;
    }
    public void aim(int cell){this.aim=cell;fx(true);}
    private AimLine preview;
    //the lane she aims down, in sight: an aim line of nature light (never the crosshair of a
    //telegraphed attack), let go of with Fx.dispose
    @Override public void fx(boolean on){
        if(preview!=null){preview.release();preview=null;}
        if(on && onSameFloor() && target!=null && target.sprite!=null && target.sprite.parent!=null){
            xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica line=new xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica(target.pos,aim,xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica.WONT_STOP);
            java.util.ArrayList<Integer> lane=new java.util.ArrayList<>();
            for(int i=1;i<line.path.size()&&i<=8+2*level;i++){
                int cell=line.path.get(i);if(!Dungeon.level.heroFOV[cell])continue;
                lane.add(cell);
            }
            preview=AimLine.show(lane);
        }
    }
	public void set( int aim, int level, int turns ){
		this.aim = aim;
		this.level = level;
		this.turnsLeft = turns;
		this.depth = Dungeon.depth;
		this.branch = Dungeon.branch;
        fx(true);
		spend( TICK );
	}

	@Override
	public boolean act() {
		if (!(target instanceof Hero) || !onSameFloor()) {
			detach();
			return true;
		}
		Hero hero = (Hero) target;
		fx(true);
        turnsLeft--;
		if (turnsLeft <= 0){
			ChargedShot.fire( hero, aim, level );
			detach();
			return true;
		}
		//the draw, seen: her own light gathering (never the level-up's stars), the count over her head
		if (hero.sprite != null && hero.sprite.visible){
			drawing( hero.sprite, 1 + (level > 2 ? 2 : level) );
			xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.counterOn( ChargedShot.class, hero, turnsLeft, CharSprite.NEUTRAL );
			if (turnsLeft <= 3) SpatialSound.play( Assets.Sounds.CHARGEUP, hero, 0.5f, 1.4f + 0.2f * (3 - turnsLeft) );
		}
		spend( TICK );
        // Hero.holdTurn advances the draw without allowing other actions.

		return true;
	}

	/** A turn of the draw on her: `n` motes of her own light lifting, a small flare. Any thread (one
	 *  burst of the sprite's emitter, as the light's). */
	public static void drawing( CharSprite s, int n ){
		s.emitter().burst( SkillBursts.FLOURISH, n );
		PixelFlare.show( SkillVfx.midX( s ), SkillVfx.midY( s ), AimLine.COLOR, PixelFlare.S, 0.45f );
	}

	/**
	 * The lane she aims down while she draws: a dot of nature light (PULSE_DOT, the huntress's leaf
	 * green) on the ground of every cell of it in sight, breathing within 12% (the line of fire,
	 * shown), and every BEAT s RUNNERS motes of her light flowing down it from her toward the target,
	 * a pale one leading (an aim line, never an enemy's telegraph: no crosshair, no red, on the floor
	 * under the bodies). Made without the GPU (it may be on the actor thread), put on the floor's
	 * layer by Fx.post, let go of with release (Fx.dispose). At most MOST dots (a lane is 14 cells at
	 * most) plus RUNNERS quads; nothing allocated a frame.
	 */
	public static final class AimLine extends Group {

		public static final int COLOR = SkillPalette.NATURE_LIGHT, MOST = 16, RUNNERS = 3;
		public static final float LEVEL = 0.6f, BREATH = 0.12f, BREATH_HZ = 0.8f, BEAT = 0.6f, RUN_PER_CELL = 0.035f, RUN_MOST = 0.4f;
		/** The runners: each this far behind the one before (a share of a cell), their colours
		 *  (leading first) and alphas. */
		public static final float RUN_GAP = 0.35f;
		static final int[] RUN_COLORS = { SkillPalette.NATURE.stop( 0 ), SkillPalette.NATURE.stop( 1 ), SkillPalette.NATURE.stop( 2 ) };
		static final float[] RUN_ALPHAS = { 1f, 0.85f, 0.65f };
		/** The dots lie this far under the cell's middle (on its ground). */
		public static final float GROUND = 3f;

		private final FxPic[] dots;
		private final FxPic[] runners = new FxPic[RUNNERS];
		private volatile boolean gone;

		private AimLine( List<Integer> lane ){
			int n = Math.min( MOST, lane.size() );
			dots = new FxPic[n];
			for (int i = 0; i < n; i++){
				int cell = lane.get( i );
				FxPic d = new FxPic( SkillVfx.LIGHT_SHEET );
				d.additive = true;
				d.show( FxFrames.Light.PULSE_DOT ).lit( COLOR ).place( SkillVfx.cx( cell ), SkillVfx.cy( cell ) + GROUND );
				d.am = LEVEL;
				add( d );
				dots[i] = d;
			}
			for (int j = 0; j < RUNNERS; j++){
				FxPic m = new FxPic( SkillVfx.SHEET );
				m.additive = true;
				m.show( FxFrames.MOTE_2 ).lit( RUN_COLORS[j] );
				m.visible = false;
				add( m );
				runners[j] = m;
			}
		}

		/** An aim line down these cells (none: nothing), on the floor's layer. Any thread. */
		public static AimLine show( List<Integer> lane ){
			if (lane.isEmpty()) return null;
			final AimLine a = new AimLine( lane );
			Fx.post( () -> {
				Group floor = GameScene.layer( GameScene.Layer.FLOOR );
				if (a.gone || floor == null) return;
				floor.add( a );
			} );
			return a;
		}

		/** Let go of (Fx.dispose), whether or not it was put on the scene yet. Any thread. */
		public void release(){
			gone = true;
			Fx.dispose( this );
		}

		public int dots(){
			return dots.length;
		}

		@Override
		public void update(){
			super.update();
			float t = Game.timeTotal;
			for (int i = 0; i < dots.length; i++){
				FxPic d = dots[i];
				d.takeSlide();
				d.am = LEVEL * (1f + BREATH * (float) Math.sin( (t * BREATH_HZ + i * 0.07f) * 2 * Math.PI ));
			}
			//the runners: from the lane's first cell to its last, eased out, the pale one leading and the
			//others a little behind it, then gone till the next beat
			float run = Math.min( RUN_MOST, RUN_PER_CELL * dots.length );
			float p = (t % BEAT) / run;
			boolean on = p < 1f && dots.length > 0;
			float fade = p < 0.8f ? 1f : (1f - p) / 0.2f;
			float lead = FxCurves.easeOut( Math.min( 1f, p ) ) * (dots.length - 1);
			for (int j = 0; j < RUNNERS; j++){
				FxPic m = runners[j];
				float k = lead - j * RUN_GAP;
				m.visible = on && k >= 0;
				if (!m.visible) continue;
				int i = Math.min( dots.length - 2, (int) k );
				float x, y;
				if (i < 0){
					x = dots[0].midX();
					y = dots[0].midY();
				} else {
					float f = k - i;
					x = dots[i].midX() + (dots[i + 1].midX() - dots[i].midX()) * f;
					y = dots[i].midY() + (dots[i + 1].midY() - dots[i].midY()) * f;
				}
				m.place( x, y );
				m.am = RUN_ALPHAS[j] * fade;
			}
		}
	}

	/** still on the floor she drew on; a stairway or a portal spoils the shot */
	private boolean onSameFloor(){
		return aim >= 0 && depth == Dungeon.depth && branch == Dungeon.branch;
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	private static final String TARGET = "target", LEFT = "left", LEVEL = "level", DEPTH = "depth", BRANCH = "branch";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TARGET, aim );
		bundle.put( LEFT, turnsLeft );
		bundle.put( LEVEL, level );
		bundle.put( DEPTH, depth );
		bundle.put( BRANCH, branch );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		aim = bundle.getInt( TARGET );
		turnsLeft = bundle.getInt( LEFT );
		level = bundle.getInt( LEVEL );
		depth = bundle.getInt( DEPTH );
		branch = bundle.getInt( BRANCH );
	}
}
