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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;

import java.util.ArrayList;

/**
 * The enemies a held Ambush blade senses through walls (Quality.senseRange; the host's hero only,
 * as all mind vision is), shown over the fog of war: the first time any are sensed, one ping - a
 * faint amber ring spreading from the hero; each one sensed (MOST at most) a ring pulsing under it
 * at PULSE Hz and, as it is first sensed, a two-frame exclamation over its head; its ring gone once
 * it is no longer sensed (it came into real sight, it left the blade's reach, the blade was put
 * away).
 *
 * Driven render side: step() from the enchantments' module after each sprite is drawn
 * (EnchantArea.afterDraw), acting only for the hero's own sprite and looking at what he senses
 * every SCAN s: the enemies in hero.mindVisionEnemies (filled by Level.updateFieldOfView with every
 * mob his mind vision marks that his eyes do not see) standing within his blade's sense. That list is
 * the actor thread's, read here by index and forgiving a read that races its refill; a foe lost for
 * less than GRACE s keeps its ring, so a refill caught half way neither drops a ring nor pings again.
 * Visual only: nothing of the game's Random, nothing of the game changed, nothing sent (a co-op
 * guest's hero senses nothing: mind vision is the host's).
 */
public final class SenseFx {

	private SenseFx(){}

	/** The most foes marked at once; how often the senses are looked at, s; how long a foe lost
	 *  keeps its ring, s. */
	public static final int MOST = 6;
	public static final float SCAN = 0.1f, GRACE = 0.3f;
	/** The ping: from a ring this wide to this wide over PING_FOR s, its colour and alpha. */
	public static final int PING_FROM = 9, PING_TO = 55, AMBER = 0xFFC060;
	public static final float PING_FOR = 0.5f, PING_ALPHA = 0.35f;
	/** A sensed foe's ring: its pulse (Hz, a telegraph's at most 1) and alpha about which it pulses. */
	public static final float PULSE = 0.75f, RING_ALPHA = 0.4f, RING_SWING = 0.15f;
	/** The exclamation: each of its two frames, s, then its fade. */
	public static final float GLINT_FRAME = 0.1f, GLINT_FADE = 0.15f;

	//the marked: each a foe's sprite, its ring, until when it is kept
	private static final CharSprite[] marked = new CharSprite[MOST];
	private static final Ring[] rings = new Ring[MOST];
	private static final float[] keepUntil = new float[MOST];
	private static float nextScan;
	//the group over the fog that holds the pictures, and the scene (or stand-in layer) it is on
	private static Group over;
	private static Object overOn;

	// ------------------------------------------------------------------ the game's

	/**
	 * After a sprite is drawn (EnchantArea.afterDraw, render thread): for the hero's own sprite,
	 * every SCAN s, the foes his Ambush blade senses marked and the ones it no longer senses let go.
	 */
	public static void step( CharSprite s ){
		if (s == null || s.ch == null || s.ch != Dungeon.hero || NetManager.isNetClient()) return;
		float now = Game.timeTotal;
		if (now < nextScan && now >= nextScan - SCAN * 2f) return;
		nextScan = now + SCAN;
		Hero hero = (Hero) s.ch;
		Level lv = Dungeon.level;
		int range = lv != null && hero.isAlive() ? Quality.senseRange( hero ) : 0;
		if (range > 0) scan( hero, lv, range, now );
		letGo( now );
	}

	//the foes sensed now: their marks kept (a new one marked, the first of a sense pinging)
	private static void scan( Hero hero, Level lv, int range, float now ){
		ArrayList<Mob> sensed = hero.mindVisionEnemies;
		int n = sensed.size();
		for (int i = 0; i < n; i++){
			Mob m;
			try {
				m = sensed.get( i );
			} catch (IndexOutOfBoundsException e){
				//cleared on the actor thread as it was read: the next scan reads it whole
				break;
			}
			if (m == null || m.sprite == null || !m.isAlive() || m.alignment != Char.Alignment.ENEMY) continue;
			if (m.pos < 0 || m.pos >= lv.length() || hero.pos < 0 || lv.distance( hero.pos, m.pos ) > range) continue;
			keep( hero.sprite, m.sprite, now + GRACE );
		}
	}

	// ------------------------------------------------------------------ the marks

	/** How many foes are marked. Render thread. */
	public static int marks(){
		int n = 0;
		for (CharSprite s : marked) if (s != null) n++;
		return n;
	}

	/** Whether a sprite is marked. Render thread. */
	public static boolean marked( CharSprite s ){
		for (CharSprite m : marked) if (m != null && m == s) return true;
		return false;
	}

	//a foe kept marked till `until` (marked now if it was not: its ring and its exclamation, and
	//the ping from `from` if it is the first marked), unless MOST are already
	static void keep( CharSprite from, CharSprite foe, float until ){
		//first: a new scene forgets the last one's marks
		Group g = group();
		int free = -1;
		boolean any = false;
		for (int i = 0; i < MOST; i++){
			if (marked[i] == foe){
				keepUntil[i] = Math.max( keepUntil[i], until );
				return;
			}
			if (marked[i] == null){
				if (free < 0) free = i;
			} else {
				any = true;
			}
		}
		if (free < 0) return;
		marked[free] = foe;
		keepUntil[free] = until;
		if (!any && from != null) ping( g, from );
		rings[free] = g == null ? null : Ring.on( g, foe );
		if (g != null) Glint.on( g, foe );
	}

	//the marks past their time (or their foe's sprite gone) let go, their rings fading
	static void letGo( float now ){
		for (int i = 0; i < MOST; i++){
			CharSprite m = marked[i];
			if (m == null) continue;
			if (now > keepUntil[i] || now < keepUntil[i] - 60f || m.parent == null || m.ch == null || !m.ch.isAlive()){
				marked[i] = null;
				if (rings[i] != null) rings[i].out();
				rings[i] = null;
			}
		}
	}

	private static void ping( Group g, CharSprite from ){
		if (g == null) return;
		FxRing.ground( g, from.x + from.width() / 2f, from.y + from.height() - 2f, PING_FROM, PING_TO, PING_FOR, AMBER,
				PING_ALPHA, true );
	}

	/**
	 * Restaged (the gallery, the previews): a sense from `from` of these foes, held `seconds` - the
	 * ping, a ring and an exclamation on each. Render thread.
	 */
	public static void stage( CharSprite from, CharSprite[] foes, float seconds ){
		float until = Game.timeTotal + seconds;
		for (CharSprite f : foes) if (f != null) keep( from, f, until );
	}

	// ------------------------------------------------------------------ over the fog

	//the group the marks are drawn in: over the fog of the game's scene (one made and laid over
	//the fog for each new scene, the last one's marks forgotten), or off it a test's stand-in light
	//layer; null with neither
	static Group group(){
		Object scene = Game.instance != null ? Game.scene() : null;
		Object on;
		if (scene instanceof GameScene){
			on = scene;
		} else {
			on = GameScene.layer( GameScene.Layer.LIGHTS );
			if (on == null) return null;
		}
		if (over == null || overOn != on || over.parent == null || !over.exists){
			for (int i = 0; i < MOST; i++){
				marked[i] = null;
				rings[i] = null;
			}
			over = new Group();
			overOn = on;
			if (on instanceof GameScene) GameScene.effectOverFog( over );
			else ((Group) on).add( over );
			if (over.parent == null) return null;
		}
		return over;
	}

	/** A sensed foe's ring: RING_13 under its feet, pulsing at PULSE Hz while it is sensed, fading
	 *  in and, let go, out. Added as light. */
	public static final class Ring extends FxPic {

		static final float FADE = 0.2f;
		private CharSprite foe;
		private float age, gone = -1f;
		private boolean counted;

		public Ring(){
			super( Assets.Effects.FX_SHEET );
			additive = true;
			show( FxFrames.RING_13 );
		}

		static Ring on( Group g, CharSprite foe ){
			Ring r = (Ring) g.recycle( Ring.class );
			r.foe = foe;
			r.age = 0f;
			r.gone = -1f;
			r.resetColor();
			r.lit( AMBER );
			r.am = 0f;
			r.revive();
			r.follow();
			if (!r.counted){
				r.counted = true;
				FxBudget.add( FxBudget.PARTICLES, 1 );
			}
			return r;
		}

		//let go: it fades out
		void out(){
			if (gone < 0f) gone = age;
		}

		private void follow(){
			place( foe.x + foe.width() / 2f, foe.y + foe.height() - 2f );
			visible = foe.parent != null;
		}

		@Override
		public void update(){
			super.update();
			if (foe == null){
				kill();
				return;
			}
			age += Game.elapsed;
			float a = RING_ALPHA + RING_SWING * (float)Math.sin( 2 * Math.PI * PULSE * age );
			a *= Math.min( 1f, age / FADE );
			if (gone >= 0f){
				float k = 1f - (age - gone) / FADE;
				if (k <= 0f){
					kill();
					return;
				}
				a *= k;
			}
			am = a;
			follow();
		}

		@Override
		public void kill(){
			super.kill();
			foe = null;
			if (counted){
				counted = false;
				FxBudget.add( FxBudget.PARTICLES, -1 );
			}
		}
	}

	/** A foe first sensed: the exclamation over its head, its two frames GLINT_FRAME s each, then
	 *  fading. Matter, drawn in amber. */
	public static final class Glint extends FxPic {

		private CharSprite foe;
		private float age;
		private int shown;
		private boolean counted;

		public Glint(){
			super( Assets.Effects.FX_SHEET );
		}

		static void on( Group g, CharSprite foe ){
			Glint e = (Glint) g.recycle( Glint.class );
			e.foe = foe;
			e.age = 0f;
			e.shown = 0;
			e.show( FxFrames.EXCLAIM[0] );
			e.resetColor();
			e.lit( AMBER );
			e.am = 1f;
			e.revive();
			e.follow();
			if (!e.counted){
				e.counted = true;
				FxBudget.add( FxBudget.PARTICLES, 1 );
			}
		}

		private void follow(){
			place( foe.x + foe.width() / 2f, foe.y - height / 2f - 1f );
		}

		@Override
		public void update(){
			super.update();
			if (foe == null || foe.parent == null){
				kill();
				return;
			}
			age += Game.elapsed;
			int f = age < GLINT_FRAME ? 0 : 1;
			if (f != shown){
				shown = f;
				show( FxFrames.EXCLAIM[f] );
			}
			float hold = 2f * GLINT_FRAME;
			if (age > hold){
				float k = 1f - (age - hold) / GLINT_FADE;
				if (k <= 0f){
					kill();
					return;
				}
				am = k;
			}
			follow();
		}

		@Override
		public void kill(){
			super.kill();
			foe = null;
			if (counted){
				counted = false;
				FxBudget.add( FxBudget.PARTICLES, -1 );
			}
		}
	}
}
