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

package xyz.gabriwar.warpedpixeldungeon.ui.guide;

import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Chains;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.journal.guidedemos.Demo;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;
import com.watabou.utils.Reflection;

//What a creature sends at a cell on a guide scene's stage (Demo.Shot), drawn as the game draws it: a
//bolt's own trail (MagicMissile's), an item turning as MissileSprite throws it, a ray's core struck in
//and thinned as Beam does, lightning's two arcs thrown anew every frame, a chain's links run out in
//their sag and pulled back (Chains'). A bolt, a thrown item and a chain arrive in the time they are
//given; a ray and lightning strike at once and fade.
//
//Only the stage's own pictures: the game's effects carry their heads, lights, pools and impacts into
//the game scene's layers, which under a page opened in play are the world beneath the window, in the
//level's coordinates. Made with the stage and fired again every time round; FxRandom only.
public abstract class DemoShot extends Group {

	/** A shot of this kind; `reach`: the longest it can be, px (its stage's diagonal). */
	public static DemoShot make( Demo.Shot s, float reach ){
		switch (s.kind){
			case BOLT:      return new Bolt( s.type );
			case THROWN:    return new Thrown( s.item );
			case RAY:       return new Ray( s.ray );
			case LIGHTNING: return new Zap();
			default:        return new Chain( reach );
		}
	}

	/** Sends it from (sx, sy) to (ex, ey): there in `flight` s if it flies. */
	public abstract void fire( float sx, float sy, float ex, float ey, float flight );

	/** Gone at once (its stage starts over). */
	public abstract void stop();

	@Override
	public void destroy() {
		//a trail's particles count themselves alive in the effects budget until they are killed
		stop();
		super.destroy();
	}

	//its image laid from (x0, y0) to (x1, y1), its origin at its left middle
	private static void stretch( Image img, float x0, float y0, float x1, float y1 ){
		float dx = x1 - x0, dy = y1 - y0;
		img.x = x0 - img.origin.x;
		img.y = y0 - img.origin.y;
		img.angle = (float)Math.toDegrees( Math.atan2( dy, dx ) );
		img.scale.x = (float)Math.sqrt( dx * dx + dy * dy ) / img.width;
	}

	//one of the game's effect pictures, drawn as light (as Beam and Lightning draw theirs)
	private static final class Light extends Image {

		Light( Effects.Type type ){
			super( Effects.get( type ) );
			origin.set( 0, height / 2 );
			visible = false;
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	// ------------------------------------------------------------------ a bolt

	private static final class Bolt extends DemoShot {

		private final int type;
		private final Trail trail = new Trail();
		private final PointF from = new PointF(), to = new PointF();

		Bolt( int type ){
			this.type = type;
			add( trail );
		}

		@Override
		public void fire( float sx, float sy, float ex, float ey, float flight ) {
			from.set( sx, sy );
			to.set( ex, ey );
			trail.reset( type, from, to, null );
			//there in its time whatever the way: the number rises as it lands
			float dx = ex - trail.x, dy = ey - trail.y;
			float d = (float)Math.sqrt( dx * dx + dy * dy );
			if (d > 0) trail.setSpeed( d / flight );
		}

		@Override
		public void stop() {
			trail.kill();
		}
	}

	//the game's bolt, its trail alone
	private static final class Trail extends MagicMissile {

		//its head, light and impact (BoltFx) and its carrying to the light layer are the level's
		@Override
		protected void setOff() {
		}

		//its first particle's delay by the effects' own random: the game's is the world's
		@Override
		public void pour( Factory factory, float interval ) {
			startDelayed( factory, interval, 0, FxRandom.Float( interval ) );
		}
	}

	// ------------------------------------------------------------------ an item thrown

	private static final class Thrown extends DemoShot {

		private final Image item;
		private final int spin;
		private float sx, sy, ex, ey;
		private float t = -1, len;

		Thrown( Class<? extends Item> cls ){
			Image img;
			try {
				img = CiteArt.item( (Item) Reflection.newInstanceUnhandled( cls ) );
			} catch (Exception e){
				img = null;
			}
			if (img == null) img = new CiteArt.ItemArt( ItemSpriteSheet.SOMETHING );
			item = img;
			item.originToCenter();
			add( item );
			stop();
			spin = MissileSprite.angularSpeed( cls );
		}

		@Override
		public void fire( float sx, float sy, float ex, float ey, float flight ) {
			this.sx = sx - item.width / 2;
			this.sy = sy - item.height / 2;
			this.ex = ex - item.width / 2;
			this.ey = ey - item.height / 2;
			//turned as MissileSprite.setup turns it
			float dx = ex - sx, dy = ey - sy;
			item.angle = 135 - (float)Math.toDegrees( Math.atan2( dx, dy ) );
			item.angularSpeed = spin;
			item.flipHorizontal = dx < 0;
			if (dx < 0){
				item.angularSpeed = -spin;
				item.angle += 90;
			}
			item.frame( item.frame() );
			t = 0;
			len = flight;
			place( 0 );
			item.visible = item.active = true;
		}

		@Override
		public void update() {
			super.update();
			if (t < 0) return;
			t += Game.elapsed;
			if (t >= len) stop();
			else place( t / len );
		}

		private void place( float k ){
			item.x = Math.round( sx + (ex - sx) * k );
			item.y = Math.round( sy + (ey - sy) * k );
		}

		//not updated either while it is put away: an item's sprite with no heap shows itself again on
		//its next update (ItemSprite.update)
		@Override
		public void stop() {
			t = -1;
			item.visible = item.active = false;
			item.angularSpeed = 0;
		}
	}

	// ------------------------------------------------------------------ a ray

	private static final class Ray extends DemoShot {

		private final Light core;
		//Beam.DeathRay's, LightRay's and HealthRay's lives
		private final float life;
		private float t = -1;

		Ray( Effects.Type type ){
			core = new Light( type );
			add( core );
			life = type == Effects.Type.DEATH_RAY ? 0.5f : type == Effects.Type.HEALTH_RAY ? 0.75f : 1f;
		}

		@Override
		public void fire( float sx, float sy, float ex, float ey, float flight ) {
			stretch( core, sx, sy, ex, ey );
			t = 0;
			shape();
			core.visible = true;
		}

		@Override
		public void update() {
			super.update();
			if (t < 0) return;
			t += Game.elapsed;
			if (t >= life) stop();
			else shape();
		}

		//struck in, held, then thinned in whole steps as it fades (Beam.thickness, Beam.coreAlpha)
		private void shape(){
			float left = 1f - t / life;
			core.alpha( Beam.coreAlpha( left ) );
			core.scale.y = Beam.thickness( t, left );
		}

		@Override
		public void stop() {
			t = -1;
			core.visible = false;
		}
	}

	// ------------------------------------------------------------------ lightning

	private static final class Zap extends DemoShot {

		//Lightning's life
		private static final float LIFE = 0.3f;
		//how far its middle is thrown off the way, px
		private static final float WANDER = 4f;

		private final Light a = new Light( Effects.Type.LIGHTNING ), b = new Light( Effects.Type.LIGHTNING );
		private float sx, sy, ex, ey;
		private float t = -1;

		Zap(){
			add( a );
			add( b );
		}

		@Override
		public void fire( float sx, float sy, float ex, float ey, float flight ) {
			this.sx = sx;
			this.sy = sy;
			this.ex = ex;
			this.ey = ey;
			t = 0;
			arc();
			a.visible = b.visible = true;
		}

		@Override
		public void update() {
			super.update();
			if (t < 0) return;
			t += Game.elapsed;
			if (t >= LIFE) stop();
			else arc();
		}

		//two arcs meeting at a middle thrown anew, fading over its life
		private void arc(){
			float mx = (sx + ex) / 2 + FxRandom.Float( -WANDER, WANDER );
			float my = (sy + ey) / 2 + FxRandom.Float( -WANDER, WANDER );
			stretch( a, sx, sy, mx, my );
			stretch( b, mx, my, ex, ey );
			float alpha = 1f - t / LIFE;
			a.alpha( alpha );
			b.alpha( alpha );
		}

		@Override
		public void stop() {
			t = -1;
			a.visible = b.visible = false;
		}
	}

	// ------------------------------------------------------------------ a chain

	private static final class Chain extends DemoShot {

		//a link every 6 px, as Chains lays them
		private static final float LINK = 6f;

		private final Image[] links;
		private int n;
		private float sx, sy, dx, dy;
		private float t = -1, len;

		Chain( float reach ){
			links = new Image[Math.round( reach / LINK ) + 1];
			for (int i = 0; i < links.length; i++){
				links[i] = Effects.get( Effects.Type.CHAIN );
				links[i].origin.set( links[i].width() / 2, links[i].height() );
				if (i % 2 == 1) links[i].brightness( Chains.SHADE );
				links[i].visible = false;
				add( links[i] );
			}
		}

		@Override
		public void fire( float sx, float sy, float ex, float ey, float flight ) {
			this.sx = sx;
			this.sy = sy;
			dx = ex - sx;
			dy = ey - sy;
			n = Math.min( links.length, Math.round( (float)Math.sqrt( dx * dx + dy * dy ) / LINK ) + 1 );
			float rotation = (float)Math.toDegrees( Math.atan2( dy, dx ) ) + 90f;
			for (int i = 0; i < n; i++){
				links[i].angle = rotation;
				links[i].visible = true;
			}
			t = 0;
			len = flight;
			lay( 0, 1, 0 );
		}

		@Override
		public void update() {
			super.update();
			if (t < 0) return;
			t += Game.elapsed;
			if (t < len){
				lay( t / len, 1, t / len );
			} else {
				//bitten: pulled back, fading
				float k = 1f - (t - len) / Chains.RETRACT;
				if (k <= 0) stop();
				else lay( k, k, 1 );
			}
		}

		//its links laid a share p of the way, hanging in the sag of a flight `flown` through, at an alpha
		private void lay( float p, float alpha, float flown ){
			float hang = Chains.sag( flown );
			for (int i = 0; i < n; i++){
				float k = i / (float)n;
				Image c = links[i];
				c.x = sx + dx * k * p - c.width() / 2f;
				c.y = sy + dy * k * p + hang * 4f * k * (1f - k) - c.height() / 2f;
				c.alpha( alpha );
			}
		}

		@Override
		public void stop() {
			t = -1;
			for (Image c : links) c.visible = false;
		}
	}
}
