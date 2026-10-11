/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.gas.Shade;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;

import java.util.ArrayList;

public class GooSprite extends MobSprite {
	
	private Animation pump;
	private Animation pumpAttack;

	private Emitter spray;

	private int pumpUpEmitterDist = 0;
	private int lastPumpUpPos = -1;
	private ArrayList<Emitter> pumpUpEmitters = new ArrayList<>();

	public GooSprite() {
		super();
		
		texture( Assets.Sprites.GOO );
		
		TextureFilm frames = new TextureFilm( texture, 20, 14 );
		
		idle = new Animation( 10, true );
		idle.frames( frames, 2, 1, 0, 0, 1 );
		
		run = new Animation( 15, true );
		run.frames( frames, 3, 2, 1, 2 );
		
		pump = new Animation( 20, true );
		pump.frames( frames, 4, 3, 2, 1, 0 );

		pumpAttack = new Animation ( 20, false );
		pumpAttack.frames( frames, 4, 3, 2, 1, 0, 7);

		attack = new Animation( 10, false );
		attack.frames( frames, 8, 9, 10 );
		
		die = new Animation( 10, false );
		die.frames( frames, 5, 6, 7 );
		
		play(idle);

		spray = centerEmitter();
		if (spray != null) {
			spray.autoKill = false;
			spray.pour(GooParticle.FACTORY, 0.08f);
			spray.on = false;
		}
	}

	@Override
	public void link(Char ch) {
		super.link(ch);
		if (ch.HP*2 <= ch.HT)
			spray(true);
	}

	public void pumpUp( int warnDist ) {
		pumpUpEmitterDist = warnDist;
		if (warnDist > 0){
			play(pump);
			SpatialSound.play( Assets.Sounds.CHARGEUP, ch, 1f, warnDist == 1 ? 0.8f : 1f );
		}
		updateEmitters();
	}

	public void updateEmitters( ){
		clearEmitters();
		if (pumpUpEmitterDist > 0 && ch != null) {
			lastPumpUpPos = ch.pos;
			if (ch.fieldOfView == null || ch.fieldOfView.length != Dungeon.level.length()) {
				ch.fieldOfView = new boolean[Dungeon.level.length()];
				Dungeon.level.updateFieldOfView(ch, ch.fieldOfView);
			}
			for (int i = 0; i < Dungeon.level.length(); i++) {
				if (ch.fieldOfView != null && ch.fieldOfView[i]
						&& Dungeon.level.distance(i, ch.pos) <= pumpUpEmitterDist
						&& new Ballistica(ch.pos, i, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos == i
						&& new Ballistica(i, ch.pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos == ch.pos) {
					Emitter e = CellEmitter.get(i);
					e.pour(GooParticle.WARN, 0.08f);
					pumpUpEmitters.add(e);
				}
			}
		}
	}

	public void clearEmitters(){
		for (Emitter e : pumpUpEmitters){
			e.on = false;
		}
		pumpUpEmitters.clear();
	}

	public void triggerEmitters(){
		for (Emitter e : pumpUpEmitters){
			e.burst(ElmoParticle.FACTORY, 10);
		}
		SpatialSound.play( Assets.Sounds.BURNING, ch );
		pumpUpEmitterDist = 0;
		pumpUpEmitters.clear();
	}

	public void pumpAttack() { play(pumpAttack); }

	@Override
	public void play(Animation anim) {
		if (anim != pump && anim != pumpAttack){
			pumpUpEmitterDist = 0;
			clearEmitters();
		}
		super.play(anim);
	}

	@Override
	public int blood() {
		return 0xFF000000;
	}

	public void spray(boolean on){
		spray.on = on;
	}

	@Override
	public void update() {
		super.update();
		if (spray != null) {
			spray.pos(center());
			spray.visible = visible;
		}
		if (pumpUpEmitterDist > 0 && ch != null && ch.pos != lastPumpUpPos){
			updateEmitters();
		}
	}

	/**
	 * A drop of goo: a 2x2 or 3x2 blob of black ooze with a pale glint on its crown, flung up 32-48
	 * px/s and falling at 160 px/s^2 to land a few pixels below - a dark splat on the floor a time in
	 * four (at 0.3, gone in a second and a half), a dark ring on the water. Its randomness is the
	 * effects' own. FACTORY's (an ooze cloud, Goo's spray, an arcane bomb) are P2; WARN's (Goo
	 * pumping up, its GooWarn cells) are a telegraph the player reads, P0: never thinned.
	 */
	public static class GooParticle extends FxParticle {

		/** The goo, and the glint on its crown. */
		public static final int GOO = 0x1A1A22, GLINT = 0x8A8AA0;
		private static final int[][] DROPS = { FxFrames.PEBBLE_2, FxFrames.PEBBLE_3 };

		public static final Emitter.Factory FACTORY = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				if (!FxBudget.keep( FxBudget.P2, x, y )) return;
				((GooParticle)emitter.recycle( GooParticle.class )).drop( x, y );
			}
		};

		public static final Emitter.Factory WARN = new WarnFactory();

		private static class WarnFactory extends Factory implements FxBudget.Prioritized {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				FxBudget.keep( FxBudget.P0 );
				((GooParticle)emitter.recycle( GooParticle.class )).drop( x, y ).priority( FxBudget.P0 );
			}

			@Override
			public int priority() {
				return FxBudget.P0;
			}
		}

		//a drop's landing: a ring on water, a splat on the floor a time in four
		private static final Fate LANDING = ( p, landed ) -> {
			if (!landed) return;
			if (WaterFX.onWater( p.px, p.py )){
				WaterFX.ring( p.px, p.py, WaterFX.M, GOO, 0.6f, FxBudget.P2 );
			} else if (FxRandom.chance( 0.25f ) && FxDecal.live() < FxBudget.decalCap() - 8){
				FxDecal.at( p.px, p.py, FxFrames.SPLAT[FxRandom.Int( 2 )], GOO, 0.3f, 1f, 0.5f, FxRandom.chance( 0.5f ) );
			}
		};

		public GooParticle drop( float x, float y ) {
			reset( x, y );
			look( FxRandom.element( DROPS ) ).life( 1f ).alpha( 1f, 0, 0.1f )
					.polar( -FxRandom.Float( 0.15f, 0.85f ) * (float)Math.PI, FxRandom.Float( 32f, 48f ) ).gravity( 160f )
					.ground( y + FxRandom.Float( 2f, 6f ), DIE, 0 ).fate( LANDING ).priority( FxBudget.P2 );
			Shade.map( this, GLINT, Shade.CROWN, GOO, Shade.BODY );
			return this;
		}
	}

	@Override
	public void onComplete( Animation anim ) {
		super.onComplete(anim);

		if (anim == pumpAttack) {

			triggerEmitters();

			idle();
			ch.onAttackComplete();
		} else if (anim == die) {
			spray.killAndErase();
		}
	}
}
