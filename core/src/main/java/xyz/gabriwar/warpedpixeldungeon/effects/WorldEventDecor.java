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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ChimneySmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Halo;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * What the world's timed events put on the land around the hero (levels/overworld/WorldEvents):
 * a fallen star's crater while it is hot - a smoke column above the fog, flames licking in it
 * and a warm glow that shows best at night - and a travelling market's maypole on the well with
 * bunting strung from it to every stall. The level works out on the actor thread what should be
 * shown and posts it here, positions taken there and then; this brings the scene in line on the
 * render thread. Everything lives in the groups a window slide moves (the smoke and the glow
 * over the fog, the flames among the emitters, the pole and the pennants among the effects), so
 * once made nothing is placed again. One per surface level, never saved; a co-op guest's
 * mirror keeps one of its own and slides it by hand (slide).
 */
public final class WorldEventDecor {

	/** A star's hot crater as posted: its heart cell's top-left in scene pixels, and how hot it
	 *  still is (3 just down, 2, 1 nearly cold). */
	public static final class Crater {
		final long id;
		final float x, y;
		final int heat;
		public Crater( long id, float x, float y, int heat ){
			this.id = id;
			this.x = x;
			this.y = y;
			this.heat = heat;
		}
	}

	/** A market as posted: the well cell's top-left, and each trader's - by the world cell he
	 *  stands on (what the bunting is known by, the same whatever the window) and in scene pixels,
	 *  both in the same order. */
	public static final class Fair {
		final long id;
		final float wellX, wellY;
		final long[] stalls;
		final float[] stallXY;
		public Fair( long id, float wellX, float wellY, long[] stalls, float[] stallXY ){
			this.id = id;
			this.wellX = wellX;
			this.wellY = wellY;
			this.stalls = stalls;
			this.stallXY = stallXY;
		}
	}

	/** A crater's smoke: the chimneys' column, but a kind of its own, so the scene never hands the
	 *  craters a chimney's dead column that SettlementAmbience still holds as it fades (it would
	 *  be lit again under that house's key, and the house's smoke would rise over the crater). */
	public static final class CraterSmoke extends ChimneySmokeParticle.Column {}

	/** Do two posts show the same thing (where it is aside: a window slide moves what is shown)? */
	public static boolean same( List<Crater> a, List<Fair> b, List<Crater> c, List<Fair> d ){
		if (a.size() != c.size() || b.size() != d.size()) return false;
		for (int i = 0; i < a.size(); i++){
			if (a.get( i ).id != c.get( i ).id || a.get( i ).heat != c.get( i ).heat) return false;
		}
		for (int i = 0; i < b.size(); i++){
			if (b.get( i ).id != d.get( i ).id || !Arrays.equals( b.get( i ).stalls, d.get( i ).stalls )) return false;
		}
		return true;
	}

	//one crater's smoke, flames and glow
	private static final class CraterFx {
		int heat;
		CraterSmoke smoke;
		Emitter flames;
		Glow glow;
	}

	//one market's pole and pennants, and the stalls they were strung to
	private static final class FairFx {
		long[] stalls;
		final ArrayList<Image> parts = new ArrayList<>();
	}

	//the scene what is shown belongs to, by identity (never the scene itself: no dead scene is kept)
	private int sceneId;
	private final HashMap<Long, CraterFx> craters = new HashMap<>();
	private final HashMap<Long, FairFx> fairs = new HashMap<>();

	//smoke: a puff every this many seconds, by heat
	private static final float[] PUFF = { 0, 0.4f, 0.24f, 0.13f };
	//flames: one every this many seconds, by heat (none once it is nearly cold)
	private static final float[] LICK = { 0, 0, 0.7f, 0.35f };
	//the glow's strength, by heat
	private static final float[] GLOW = { 0, 0.22f, 0.36f, 0.5f };

	/** Render thread: what is on the scene (of identity sceneId) is brought in line with a post. */
	public void show( int sceneId, List<Crater> craterList, List<Fair> fairList ){
		if (!(Game.scene() instanceof GameScene) || System.identityHashCode( Game.scene() ) != sceneId) return;
		if (this.sceneId != sceneId){
			//the old scene took everything it showed with it
			craters.clear();
			fairs.clear();
			this.sceneId = sceneId;
		}

		HashSet<Long> want = new HashSet<>();
		for (Crater c : craterList){
			want.add( c.id );
			CraterFx fx = craters.get( c.id );
			if (fx == null){
				fx = new CraterFx();
				craters.put( c.id, fx );
				//the column rises off the crater's heart, shown as that ground is seen
				fx.smoke = ChimneySmokeParticle.column( CraterSmoke.class, c.x + 4, c.y + 4, 8, 8, PUFF[c.heat], 0, 0, true );
				fx.glow = new Glow( c.x + DungeonTilemap.SIZE / 2f, c.y + DungeonTilemap.SIZE / 2f );
				GameScene.effectOverFog( fx.glow );
				fx.heat = -1;
			}
			if (fx.heat != c.heat){
				if (fx.smoke != null) fx.smoke.pour( ChimneySmokeParticle.FACTORY, PUFF[c.heat] );
				if (LICK[c.heat] > 0){
					if (fx.flames == null){
						fx.flames = GameScene.emitter();
						if (fx.flames != null) fx.flames.pos( c.x + 3, c.y + 5, 10, 8 );
					}
					if (fx.flames != null) fx.flames.pour( FlameParticle.FACTORY, LICK[c.heat] );
				} else if (fx.flames != null){
					//dies once its last flame is out; the scene recycles it, so it is let go at once
					fx.flames.on = false;
					fx.flames = null;
				}
				fx.glow.target( GLOW[c.heat] );
				fx.heat = c.heat;
			}
		}
		for (Iterator<Map.Entry<Long, CraterFx>> it = craters.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<Long, CraterFx> e = it.next();
			if (want.contains( e.getKey() )) continue;
			CraterFx fx = e.getValue();
			if (fx.smoke != null) fx.smoke.on = false;
			if (fx.flames != null) fx.flames.on = false;
			fx.glow.putOut();
			it.remove();
		}

		want.clear();
		for (Fair f : fairList){
			want.add( f.id );
			FairFx fx = fairs.get( f.id );
			if (fx != null && Arrays.equals( fx.stalls, f.stalls )) continue;
			if (fx != null) takeDown( fx );
			fx = new FairFx();
			fx.stalls = f.stalls;
			hang( f, fx.parts );
			fairs.put( f.id, fx );
		}
		for (Iterator<Map.Entry<Long, FairFx>> it = fairs.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<Long, FairFx> e = it.next();
			if (want.contains( e.getKey() )) continue;
			takeDown( e.getValue() );
			it.remove();
		}
	}

	/** Render thread: everything shown moved by (dx, dy) scene pixels. A co-op guest's mirror
	 *  (levels/overworld/EventDecorMirror) re-labels its window without sliding the scene, so
	 *  what stands on the ground is moved with it by hand; the host's slide moves the groups. */
	public void slide( float dx, float dy ){
		for (CraterFx fx : craters.values()){
			if (fx.smoke != null) fx.smoke.slide( dx, dy );
			if (fx.flames != null){
				fx.flames.x += dx;
				fx.flames.y += dy;
			}
			fx.glow.x += dx;
			fx.glow.y += dy;
		}
		for (FairFx fx : fairs.values()){
			for (Image i : fx.parts){
				i.x += dx;
				i.y += dy;
			}
		}
	}

	//off the scene at once, buffers freed: an erased image keeps them till the next scene
	//change, and the surface is one long scene
	private static void takeDown( FairFx fx ){
		for (Image i : fx.parts){
			if (i.parent == null) continue;
			i.killAndErase();
			i.destroy();
		}
		fx.parts.clear();
	}

	/**
	 * The maypole on the well and a rope of pennants from near its top to just above each
	 * trader's head, sagging in the middle (tools/market_bunting_gen.py draws the parts and
	 * previews this very layout). The pennant nearest a trader is left off, so none hides his
	 * face. Pennants first and the pole last, so it stands in front of the ropes it holds.
	 */
	private static void hang( Fair f, ArrayList<Image> parts ){
		float ax = f.wellX + 8, ay = f.wellY - 16;
		for (int k = 0; k < f.stalls.length; k++){
			float bx = f.stallXY[2 * k] + 8, by = f.stallXY[2 * k + 1] - 9;
			float length = (float) Math.hypot( bx - ax, by - ay );
			int n = Math.max( 3, Math.round( length / 7 ) );
			float sag = 4 + length / 12;
			for (int i = 1; i < n - 1; i++){
				float t = i / (float) n;
				float px = ax + (bx - ax) * t;
				float py = ay + (by - ay) * t + sag * 4 * t * (1 - t);
				Image pennant = new Image( Assets.Effects.MARKET_BUNTING, 8 + 6 * ((i + k) % 4), 0, 6, 7 );
				pennant.x = Math.round( px - 3 );
				pennant.y = Math.round( py - 1 );
				GameScene.effect( pennant );
				parts.add( pennant );
			}
		}
		//its foot on the ground line a character's feet stand on (CharSprite's perspective raise)
		Image pole = new Image( Assets.Effects.MARKET_BUNTING, 0, 0, 8, 32 );
		pole.x = f.wellX + 4;
		pole.y = f.wellY - 22;
		GameScene.effect( pole );
		parts.add( pole );
	}

	//the crater's warm light, added to what lies under it: strongest after dark, breathing
	//slowly, shown as plainly as the hero sees the ground under its middle (read off its own
	//position, so a window slide never leaves it stale); fades out before it goes
	private static final class Glow extends Halo {

		private float level, target, t;
		private boolean out;

		Glow( float cx, float cy ){
			super( 34, 0xFF7A30, 1f );
			point( cx, cy );
			am = 0;
		}

		void target( float v ){
			target = v;
			out = false;
		}

		void putOut(){
			target = 0;
			out = true;
		}

		@Override
		public void update(){
			super.update();
			float dt = Game.elapsed;
			t += dt;
			level += (target - level) * Math.min( 1f, dt * 0.8f );
			if (out && level < 0.01f){
				killAndErase();
				destroy();
				return;
			}
			float breath = 0.85f + 0.1f * (float) Math.sin( 1.3f * t ) + 0.05f * (float) Math.sin( 3.7f * t );
			float night = Math.min( 1f, 0.3f + GameScene.nightTintAlpha() * 1.4f );
			am = level * breath * night * seen();
		}

		private float seen(){
			int w = Dungeon.level != null ? Dungeon.level.width() : 0;
			float cx = x + width() / 2f, cy = y + height() / 2f;
			if (w == 0 || cx < 0 || cy < 0) return 0f;
			return HearthLight.groundSeen( (int) (cx / DungeonTilemap.SIZE) + (int) (cy / DungeonTilemap.SIZE) * w );
		}

		@Override
		public void draw(){
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}
}
