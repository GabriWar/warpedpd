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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.RectF;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * The martial-steel previews' stage props, the same in the old code and the new (both registries
 * use them, so the Before side compiles in a checkout of master too): summoned allies with their
 * sprites, a skill mark on a dummy, a stand-in of the real stun's stars (a sprite draws those itself,
 * so the dump never sees them), and a capture of the kit's orbits (FxOrbit draws from a sprite's own
 * draw, which the headless stage never makes): each frame it lays the motes of a sprite's MARK orbit
 * as pictures in the light layer where the orbit would draw them, read from the orbit's own settings.
 */
public final class MartialPreviewStage {

	private MartialPreviewStage(){}

	/** A summoned ally of a kind at a cell, its sprite on the stage and linked, its sprite's state
	 *  brought up as linking does (updateSpriteState). */
	public static SummonedPet pet( SkillPreviewDump.Scene s, SummonedPet.PET_TYPES type, int cell ){
		return link( s, new SummonedPet( type ), cell );
	}

	/** A summoned ally drawn as a sprite class (a shieldbearer: GuardSprite). */
	public static SummonedPet pet( SkillPreviewDump.Scene s, Class<? extends CharSprite> sprite, int cell ){
		return link( s, new SummonedPet( sprite ), cell );
	}

	private static SummonedPet link( SkillPreviewDump.Scene s, SummonedPet pet, int cell ){
		pet.pos = cell;
		pet.HP = pet.HT = 20;
		CharSprite sp = pet.sprite();
		sp.ch = pet;
		pet.sprite = sp;
		s.h.parent.add( sp );
		sp.place( cell );
		sp.visible = true;
		pet.updateSpriteState();
		return pet;
	}

	/** A skill mark of a kind on a character, its picture on the stage (Mark.fx). */
	public static SkillMarkFX mark( SkillPreviewDump.Scene s, Char on, int kind, int rank ){
		SkillInteractions.Mark m = new SkillInteractions.Mark();
		m.kind = kind;
		m.rank = rank;
		m.target = on;
		SkillMarkFX fx = new SkillMarkFX( m );
		s.h.parent.add( fx );
		return fx;
	}

	// ------------------------------------------------------------------ the real stun, stood in

	/** The real stun's colour (Looks.Stunned.COLOR). */
	public static final int STUN = Ramp.lerp( Element.METAL.ramp.stop( 1 ), 0xCCAA44, 0.6f );

	/** Looks.Stunned's two amber stars circling the MARK orbit's ellipse at 0.8 turns a second (a
	 *  sprite draws them itself; this lays them in the light layer for the dump). */
	public static void stunStars( final CharSprite on ){
		final FxPic[] stars = new FxPic[2];
		Group lights = GameScene.layer( GameScene.Layer.LIGHTS );
		for (int i = 0; i < 2; i++){
			stars[i] = new FxPic( SkillVfx.SHEET );
			stars[i].additive = true;
			stars[i].show( FxFrames.STAR4_5 );
			stars[i].lit( STUN );
			lights.add( stars[i] );
		}
		lights.add( new Gizmo(){
			float t;
			@Override
			public void update(){
				t += Game.elapsed;
				FxOrbit.Slot m = FxOrbit.Slot.MARK;
				for (int i = 0; i < 2; i++){
					float a = (float) (2 * Math.PI * 0.8f * t + Math.PI * i);
					stars[i].place( SkillVfx.midX( on ) + m.rx * (float) Math.cos( a ), SkillVfx.midY( on ) + m.h + m.ry * (float) Math.sin( a ) );
					stars[i].am = Math.sin( a ) < 0 ? 0.55f : 1f;
				}
			}
		} );
	}

	// ------------------------------------------------------------------ the orbits, captured

	/** Lays the MARK orbit of each of these sprites (if they have one) as pictures every frame. */
	public static void orbits( final CharSprite... on ){
		GameScene.layer( GameScene.Layer.LIGHTS ).add( new OrbitShot( on ) );
	}

	private static final class OrbitShot extends Group {
		private final CharSprite[] on;
		private final ArrayList<FxPic> pics = new ArrayList<>();
		private final float[] phase;
		private float t;

		OrbitShot( CharSprite[] on ){
			this.on = on;
			phase = new float[on.length];
			for (int i = 0; i < on.length; i++) phase[i] = Float.NaN;
		}

		@Override
		public void update(){
			super.update();
			float dt = Game.elapsed;
			t += dt;
			int used = 0;
			try {
				for (int k = 0; k < on.length; k++){
					FxOrbit.Ring r = FxOrbit.get( on[k], FxOrbit.Slot.MARK );
					if (r == null) continue;
					int count = (Integer) get( r, "count" );
					int trail = (Integer) get( r, "trail" );
					float lo = (Float) get( r, "lo" ), hi = (Float) get( r, "hi" ), hz = (Float) get( r, "hz" );
					float speed = (Float) get( r, "speed" );
					if (Float.isNaN( phase[k] )) phase[k] = (Float) get( r, "phase" );
					phase[k] += speed * dt;
					Image img = (Image) get( r, "image" );
					if (img == null || count <= 0) continue;
					RectF f = img.frame();
					int[] frame = { Math.round( f.left * img.texture.width ), Math.round( f.top * img.texture.height ),
							Math.round( (f.right - f.left) * img.texture.width ), Math.round( (f.bottom - f.top) * img.texture.height ) };
					float breath = hz > 0 ? lo + (hi - lo) * (0.5f + 0.5f * (float) Math.sin( 2 * Math.PI * hz * t )) : hi;
					int shown = Math.min( count, FxOrbit.MOST / (1 + trail) );
					FxOrbit.Slot slot = FxOrbit.Slot.MARK;
					float cx = SkillVfx.midX( on[k] ), cy = SkillVfx.midY( on[k] );
					for (int i = 0; i < shown; i++){
						for (int j = trail; j >= 0; j--){
							float a = phase[k] + (float) (2 * Math.PI * i / shown) - j * 0.35f;
							FxPic p = pic( used++ );
							p.show( frame );
							p.rm = img.rm; p.gm = img.gm; p.bm = img.bm;
							p.x = Math.round( cx + slot.rx * (float) Math.cos( a ) - frame[2] / 2f );
							p.y = Math.round( cy + slot.h + slot.ry * (float) Math.sin( a ) - frame[3] / 2f );
							p.am = breath * (j == 0 ? 1f : j == 1 ? 0.5f : 0.25f);
							p.visible = on[k].visible;
						}
					}
				}
			} catch (ReflectiveOperationException e){
				throw new AssertionError( e );
			}
			for (int i = used; i < pics.size(); i++) pics.get( i ).visible = false;
		}

		private FxPic pic( int i ){
			while (pics.size() <= i){
				FxPic p = new FxPic( Assets.Effects.FX_SHEET );
				p.additive = true;
				pics.add( p );
				add( p );
			}
			return pics.get( i );
		}

		private static Object get( Object o, String name ) throws ReflectiveOperationException {
			Field f = o.getClass().getDeclaredField( name );
			f.setAccessible( true );
			return f.get( o );
		}
	}
}
