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

package xyz.gabriwar.warpedpixeldungeon.effects.skillfx;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * The Champion's standard (Banner, BannerStandard): a spectral standard of light planted on a
 * cell, never a spear lying on the floor. A pale pole of light (BANNER_POLE, lit POLE) standing
 * with its foot FOOT px over the cell's bottom edge, its swallowtail cloth of gold (BANNER_CLOTH,
 * lit CLOTH: a cream cloth added over a dark floor reads as a white flag)
 * flying off it and fluttering A, B, C, B at FLUTTER_FPS, all of it at ALPHA breathing BREATH
 * either way at BREATH_HZ; a lasting pool of light at its foot (POOL px, POOL_LEVEL). Freshly
 * planted it rises out of its cell (the pole revealed from the foot up over RISE s, then the cloth
 * unfurling from the pole over UNFURL s); restored from a save it simply stands.
 *
 * In the light layer (it reads at night as the light it is). Made on any thread (no GPU until its
 * first frame, the pool asked for then: FxLight.room, asking again every RETRY s when there is
 * none); let go of with Fx.dispose, which puts its pool out. 2 quads + 1 lasting light.
 */
public final class BannerFX extends Group {

	/** Its colours: the pole (MARTIAL's light), the cloth (gold), its light (MARTIAL's). */
	public static final int POLE = 0xF3DDB5, CLOTH = 0xE0A050, LIGHT = 0xD9AE75;
	/** The foot over the cell's bottom edge, px; the cloth's top under the pole's top, px. */
	public static final int FOOT = 2, CLOTH_DROP = 3;
	public static final float ALPHA = 0.85f, BREATH = 0.1f, BREATH_HZ = 0.8f, FLUTTER_FPS = 6f;
	/** The cloth's frames in turn. */
	public static final int[] FLUTTER = { 0, 1, 2, 1 };
	public static final int POOL = 31;
	public static final float POOL_LEVEL = 0.35f, RISE = 0.15f, UNFURL = 0.1f, RETRY = 0.5f;

	private final int cell;
	private final FxPic pole, cloth;
	private final boolean rising;
	private float time;
	private int shownCloth = -1, shownPole = -1, shownWidth = -1;
	private FxLight pool;
	private int poolSetups;
	private float retryAt;

	private BannerFX( int cell, boolean rising ){
		this.cell = cell;
		this.rising = rising;
		pole = new FxPic( SkillVfx.SHEET );
		pole.additive = true;
		pole.show( FxFrames.BANNER_POLE );
		pole.lit( POLE );
		pole.visible = false;
		cloth = new FxPic( SkillVfx.SHEET );
		cloth.additive = true;
		cloth.show( FxFrames.BANNER_CLOTH[0] );
		cloth.lit( CLOTH );
		cloth.visible = false;
		add( pole );
		add( cloth );
	}

	/** A standard planted on a cell (`rising`: freshly, so it rises out of it). Any thread: it is put
	 *  in the light layer on the render thread. The holder lets it go with Fx.dispose. */
	public static BannerFX plant( int cell, boolean rising ){
		final BannerFX b = new BannerFX( cell, rising );
		Fx.post( () -> {
			if (GameScene.layer( GameScene.Layer.LIGHTS ) == null) return;
			GameScene.light( b );
		} );
		return b;
	}

	/** The cloth's frame (an index of BANNER_CLOTH) `time` s after it was planted. */
	public static int clothFrame( float time ){
		return FLUTTER[((int) (Math.max( 0f, time ) * FLUTTER_FPS )) % FLUTTER.length];
	}

	/** How bright it stands `time` s in (before the cell's sight). */
	public static float alphaAt( float time ){
		return ALPHA + BREATH * (float) Math.sin( 2 * Math.PI * BREATH_HZ * time );
	}

	/** How many rows of the pole show `time` s after a fresh planting (from the foot up). */
	public static int poleRows( float time ){
		int h = FxFrames.BANNER_POLE[3];
		return Math.max( 1, Math.min( h, Math.round( h * FxCurves.easeOut( Math.min( 1f, time / RISE ) ) ) ) );
	}

	public int cell(){
		return cell;
	}

	@Override
	public void update(){
		super.update();
		time += Game.elapsed;
		int w = DungeonTilemap.SIZE;
		float cx = SkillVfx.cx( cell );
		float footY = SkillVfx.cy( cell ) + w / 2f - FOOT;
		float seen = Fx.groundSeen( cell );
		float a = alphaAt( time ) * seen;
		int[] pf = FxFrames.BANNER_POLE;
		int rows = rising ? poleRows( time ) : pf[3];
		if (rows != shownPole){
			shownPole = rows;
			pole.crop( pf, 0, pf[3] - rows, pf[2], rows );
		}
		//the pole's middle column on the cell's middle, its foot on the foot line (whole px)
		pole.x = Math.round( cx ) - 1;
		pole.y = Math.round( footY ) - rows;
		pole.am = a;
		pole.visible = seen > 0;
		//the cloth: once the pole stands, unfurled from it and fluttering
		float unfurl = rising ? (time - RISE) / UNFURL : 1f;
		int f = clothFrame( time );
		int[] cf = FxFrames.BANNER_CLOTH[f];
		int width = Math.max( 0, Math.min( cf[2], Math.round( cf[2] * FxCurves.easeOut( Math.max( 0f, Math.min( 1f, unfurl ) ) ) ) ) );
		if (f != shownCloth || width != shownWidth){
			shownCloth = f;
			shownWidth = width;
			cloth.crop( cf, 0, 0, Math.max( 1, width ), cf[3] );
		}
		cloth.x = pole.x + 1;
		cloth.y = Math.round( footY ) - pf[3] + CLOTH_DROP;
		cloth.am = a;
		cloth.visible = seen > 0 && width > 0 && rows == pf[3];
		light( cx, footY );
	}

	//its lasting pool of light, asked for once there is room
	private void light( float cx, float footY ){
		if (pool != null && pool.alive && pool.setups() == poolSetups) return;
		pool = null;
		if (Game.timeTotal < retryAt) return;
		if (!FxLight.room( 1, 1 )){
			retryAt = Game.timeTotal + RETRY;
			return;
		}
		pool = FxLight.pool( cx, footY - 1, POOL, LIGHT, POOL_LEVEL ).persistent().flicker( FxLight.BREATH );
		poolSetups = pool.setups();
	}

	/** Whether its pool of light is lit now. */
	public boolean lit(){
		return pool != null && pool.alive && pool.setups() == poolSetups;
	}

	private void putOut(){
		if (lit()) pool.putOut( 0.3f );
		pool = null;
	}

	@Override
	public void kill(){
		putOut();
		super.kill();
	}

	@Override
	public void destroy(){
		putOut();
		super.destroy();
	}
}
