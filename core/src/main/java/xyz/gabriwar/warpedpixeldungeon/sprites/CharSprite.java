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
import xyz.gabriwar.warpedpixeldungeon.effects.ButterEffect;
import xyz.gabriwar.warpedpixeldungeon.effects.DarkBlock;
import xyz.gabriwar.warpedpixeldungeon.effects.EmoIcon;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.IceBlock;
import xyz.gabriwar.warpedpixeldungeon.effects.GlowBlock;
import xyz.gabriwar.warpedpixeldungeon.effects.ShieldHalo;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.TorchHalo;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.HalomethaneFlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.CharHealthIndicator;
import com.watabou.glwrap.Matrix;
import com.watabou.glwrap.Vertexbuffer;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.noosa.tweeners.PosTweener;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.nio.Buffer;
import java.util.HashSet;

public class CharSprite extends MovieClip implements Tweener.Listener, MovieClip.Listener {
	
	// Color constants for floating text
	public static final int DEFAULT		= 0xFFFFFF;
	public static final int POSITIVE	= 0x00FF00;
	public static final int NEGATIVE	= 0xFF0000;
	public static final int WARNING		= 0xFF8800;
	public static final int NEUTRAL		= 0xFFFF00;
	
	public static final float DEFAULT_MOVE_INTERVAL = 0.1f;
	private static float moveInterval = DEFAULT_MOVE_INTERVAL;
	private static final float FLASH_INTERVAL	= 0.05f;

	//the amount the sprite is raised from flat when viewed in a raised perspective
	protected float perspectiveRaise    = 6 / 16f; //6 pixels

	//the width and height of the shadow are a percentage of sprite size
	//offset is the number of pixels the shadow is moved down or up (handy for some animations)
	protected boolean renderShadow  = false;
	protected float shadowWidth     = 1.2f;
	protected float shadowHeight    = 0.25f;
	protected float shadowOffset    = 0.25f;

	public enum State {
		BURNING, LEVITATING, INVISIBLE, PARALYSED, FROZEN, ILLUMINATED, CHILLED, DARKENED, MARKED, HEALING, SHIELDED, HEARTS, GLOWING, AURA, ELECTRIC, BUTTER, HALOMETHANEBURNING, HEATSTROKE, HYPOTHERMIA
	}
	
	protected Animation idle;
	protected Animation run;
	protected Animation attack;
	protected Animation operate;
	protected Animation zap;
	protected Animation die;
	
	protected Callback animCallback;
	
	protected PosTweener motion;
	
	protected Emitter burning;
	protected Emitter electric;
	protected Emitter halomethaneBurning;
	protected Emitter heatstroke;
	protected Emitter hypothermia;
	protected Emitter chilled;
	protected Emitter marked;
	protected Emitter levitation;
	protected Emitter healing;
	protected Emitter hearts;
	
	protected IceBlock iceBlock;
	protected DarkBlock darkBlock;
	protected GlowBlock glowBlock;
	protected TorchHalo light;
	protected ShieldHalo shield;
	protected AlphaTweener invisible;
	protected Flare aura;
	protected ButterEffect butter;

	protected EmoIcon emo;
	protected CharHealthIndicator health;

	private Tweener jumpTweener;
	private Callback jumpCallback;

	protected float flashTime = 0;
	
	protected boolean sleeping = false;

	public Char ch;

	//used to prevent the actor associated with this sprite from acting until movement completes
	public volatile boolean isMoving = false;
	
	public CharSprite() {
		super();
		listener = this;
	}
	
	@Override
	public void play(Animation anim) {
		//Shouldn't interrupt the dieing animation
		if (curAnim == null || curAnim != die) {
			super.play(anim);
		}
	}
	
	//intended to be used for placing a character in the game world
	public void link( Char ch ) {
		linkVisuals( ch );
		
		this.ch = ch;
		ch.sprite = this;
		
		place( ch.pos );
		turnTo( ch.pos, Random.Int( Dungeon.level.length() ) );
		renderShadow = true;
		
		if (ch != Dungeon.hero) {
			if (health == null) {
				health = new CharHealthIndicator(ch);
			} else {
				health.target(ch);
			}
		}

		ch.updateSpriteState();
	}

	@Override
	public void destroy() {
		super.destroy();
		if (ch != null && ch.sprite == this){
			ch.sprite = null;
		}
	}

	//used for just updating a sprite based on a given character, not linking them or placing in the game
	public void linkVisuals( Char ch ){
		//do nothin by default
	}
	
	public PointF worldToCamera( int cell ) {
		
		final int csize = DungeonTilemap.SIZE;
		
		return new PointF(
			PixelScene.align(Camera.main, ((cell % Dungeon.level.width()) + 0.5f) * csize - width() * 0.5f),
			PixelScene.align(Camera.main, ((cell / Dungeon.level.width()) + 1.0f) * csize - height() - csize * perspectiveRaise)
		);
	}
	
	public void place( int cell ) {
		point( worldToCamera( cell ) );
	}
	
	public void showStatus( int color, String text, Object... args ) {
		showStatusWithIcon(color, text, FloatingText.NO_ICON, args);
	}

	public void showStatusWithIcon( int color, String text, int icon, Object... args ) {
		if (visible) {
			if (args.length > 0) {
				text = Messages.format( text, args );
			}
			float x = destinationCenter().x;
			float y = destinationCenter().y - height()/2f;
			int pos = DungeonTilemap.worldToTile(x, y + height(), Dungeon.level.width());
			if (ch != null) {
				FloatingText.show( x, y, pos, text, color, icon, true );
				xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.recordFloatingText(ch, text, color, icon);
			} else {
				FloatingText.show( x, y, -1, text, color, icon, true );
			}
		}
	}
	
	public void idle() {
		play(idle);
	}
	
	public void move( int from, int to ) {
		turnTo( from , to );

		play( run );

		motion = new PosTweener( this, worldToCamera( to ), moveInterval );
		motion.listener = this;
		parent.add( motion );

		isMoving = true;

		if (visible && Dungeon.level.water[from] && !ch.flying) {
			GameScene.ripple( from );
		}

		xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.recordMove(ch, from, to);
	}
	
	public static void setMoveInterval( float interval){
		moveInterval = interval;
	}
	
	//returns where the center of this sprite will be after it completes any motion in progress
	public PointF destinationCenter(){
		PosTweener motion = this.motion;
		if (motion != null && motion.elapsed >= 0){
			return new PointF(motion.end.x + width()/2f, motion.end.y + height()/2f);
		} else {
			return center();
		}
	}
	
	/**
	 * Slides this sprite AND its in-flight motion tween by a world-space pixel
	 * delta. Used when a sliding-window level rebases: the walk animation
	 * carries straight across the seam instead of being interrupted.
	 */
	public void shiftWorld( float sx, float sy ){
		x += sx;
		y += sy;
		if (motion != null){
			motion.start.offset( sx, sy );
			motion.end.offset( sx, sy );
		}
	}

	public void interruptMotion() {
		if (motion != null) {
			motion.stop(false);
		}
	}
	
	public void attack( int cell ) {
		attack( cell, null );
	}
	
	public synchronized void attack( int cell, Callback callback ) {
		animCallback = callback;
		turnTo( ch.pos, cell );
		play( attack );
		xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.recordSpriteAnim(ch, "attack", cell);
	}
	
	public void operate( int cell ) {
		operate( cell, null );
	}
	
	public synchronized void operate( int cell, Callback callback ) {
		animCallback = callback;
		turnTo( ch.pos, cell );
		play( operate );
		xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.recordSpriteAnim(ch, "operate", cell);
	}
	
	public void zap( int cell ) {
		zap( cell, null );
	}
	
	public synchronized void zap( int cell, Callback callback ) {
		animCallback = callback;
		turnTo( ch.pos, cell );
		play( zap );
		xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.recordSpriteAnim(ch, "zap", cell);
	}
	
	public void turnTo( int from, int to ) {
		int fx = from % Dungeon.level.width();
		int tx = to % Dungeon.level.width();
		if (tx > fx) {
			flipHorizontal = false;
		} else if (tx < fx) {
			flipHorizontal = true;
		}
	}

	public void jump( int from, int to, Callback callback ) {
		float distance = Math.max( 1f, Dungeon.level.trueDistance( from, to ));
		jump( from, to, distance * 2, distance * 0.1f, callback );
	}

	public void jump( int from, int to, float height, float duration,  Callback callback ) {
		jumpCallback = callback;

		jumpTweener = new JumpTweener( this, worldToCamera( to ), height, duration );
		jumpTweener.listener = this;
		parent.add( jumpTweener );

		turnTo( from, to );
	}

	public void die() {
		sleeping = false;
		processStateRemoval( State.PARALYSED );
		play( die );

		hideEmo();

		if (health != null){
			health.killAndErase();
		}
		xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.recordSpriteAnim(ch, "die", ch != null ? ch.pos : 0);
	}
	
	public Emitter emitter() {
		Emitter emitter = GameScene.emitter();
		if (emitter != null) emitter.pos( this );
		return emitter;
	}
	
	public Emitter centerEmitter() {
		Emitter emitter = GameScene.emitter();
		if (emitter != null) emitter.pos( center() );
		return emitter;
	}
	
	public Emitter bottomEmitter() {
		Emitter emitter = GameScene.emitter();
		if (emitter != null) emitter.pos( x, y + height, width, 0 );
		return emitter;
	}
	
	public void burst( final int color, int n ) {
		if (visible) {
			Splash.at( center(), color, n );
		}
	}
	
	public void bloodBurstA( PointF from, int damage ) {
		if (visible) {
			PointF c = center();
			int n = (int)Math.min( 9 * Math.sqrt( (double)damage / ch.HT ), 9 );
			Splash.at( c, PointF.angle( from, c ), 3.1415926f / 2, blood(), n );
		}
		xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.recordBloodBurst(ch, damage);
	}

	public int blood() {
		return 0xFFBB0000;
	}
	
	public void flash() {
		ra = ba = ga = 1f;
		flashTime = FLASH_INTERVAL;
		xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.recordFlash(ch);
	}

	private final HashSet<State> stateAdditions = new HashSet<>();

	public void add( State state ) {
		//instant as it just changes an animation property that will get read later
		if (state == State.PARALYSED){
			paused = true;
		} else {
			synchronized (State.class) {
				stateRemovals.remove(state);
				stateAdditions.add(state);
			}
		}
		xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.recordSpriteState(ch, state, true);
	}

	private int auraColor = 0;
	private int auraRays = 0;

	//Aura needs color and ray count data too
	public void aura( int color, int nRays ){
		add(State.AURA);
		auraColor = color;
		auraRays = nRays;
	}

	protected synchronized void processStateAddition( State state ) {
		switch (state) {
			case BURNING:
				if (burning != null) burning.on = false;
				burning = emitter();
				burning.pour(FlameParticle.FACTORY, 0.06f);
				if (visible) {
					Sample.INSTANCE.play(Assets.Sounds.BURNING);
				}
				break;
			case LEVITATING:
				if (levitation != null) levitation.on = false;
				levitation = emitter();
				levitation.pour(Speck.factory(Speck.JET), 0.02f);
				break;
			case INVISIBLE:
				if (invisible != null) invisible.killAndErase();
				invisible = new AlphaTweener(this, 0.4f, 0.4f);
				if (parent != null) {
					parent.add(invisible);
				} else
					alpha(0.4f);
				break;
			case PARALYSED:
				paused = true;
				break;
			case FROZEN:
				if (iceBlock != null) iceBlock.killAndErase();
				iceBlock = IceBlock.freeze(this);
				break;
			case ILLUMINATED:
				if (light != null) light.putOut();
				GameScene.effect(light = new TorchHalo(this));
				break;
			case CHILLED:
				if (chilled != null) chilled.on = false;
				chilled = emitter();
				chilled.pour(SnowParticle.FACTORY, 0.1f);
				break;
			case DARKENED:
				if (darkBlock != null) darkBlock.killAndErase();
				darkBlock = DarkBlock.darken(this);
				break;
			case MARKED:
				if (marked != null) marked.on = false;
				marked = emitter();
				marked.pour(ShadowParticle.UP, 0.1f);
				break;
			case HEALING:
				if (healing != null) healing.on = false;
				healing = emitter();
				healing.pour(Speck.factory(Speck.HEALING), 0.5f);
				break;
			case SHIELDED:
				if (shield != null) shield.killAndErase();
				GameScene.effect(shield = new ShieldHalo(this));
				break;
			case HEARTS:
				if (hearts != null) hearts.on = false;
				hearts = emitter();
				hearts.pour(Speck.factory(Speck.HEART), 0.5f);
				break;
			case GLOWING:
				if (glowBlock != null) glowBlock.killAndErase();
				glowBlock = GlowBlock.lighten(this);
				break;
			case AURA:
				if (aura != null)   aura.killAndErase();
				float size = Math.max(width(), height());
				size = Math.max(size+4, 16);
				aura = new Flare(auraRays, size);
				aura.angularSpeed = 90;
				aura.color(auraColor, true);
				aura.visible = visible;

				if (parent != null) {
					aura.show(this, 0);
				}
				break;
			case ELECTRIC:
				electric = emitter();
				electric.pour( SparkParticle.STATIC, 0.06f );
				break;
			case BUTTER:
				if (butter != null) butter.dissapear();
				butter = ButterEffect.butter(this);
				break;
			case HALOMETHANEBURNING:
				if (halomethaneBurning != null) halomethaneBurning.on = false;
				halomethaneBurning = emitter();
				halomethaneBurning.pour(HalomethaneFlameParticle.FACTORY, 0.06f);
				if (visible) {
					Sample.INSTANCE.play(Assets.Sounds.BURNING);
				}
				break;
			case HEATSTROKE:
				if (heatstroke != null) heatstroke.on = false;
				heatstroke = emitter();
				heatstroke.pour(Speck.factory(Speck.STEAM), 0.4f);
				break;
			case HYPOTHERMIA:
				if (hypothermia != null) hypothermia.on = false;
				hypothermia = emitter();
				hypothermia.pour(SnowParticle.RISING_FACTORY, 0.1f);
				break;
		}
	}

	private final HashSet<State> stateRemovals = new HashSet<>();

	public void remove( State state ) {
		//instant as it just changes an animation property that will get read later
		if (state == State.PARALYSED){
			paused = false;
		} else {
			synchronized (State.class) {
				stateAdditions.remove(state);
				stateRemovals.add(state);
			}
		}
		xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.recordSpriteState(ch, state, false);
	}

	public void clearAura(){
		remove(State.AURA);
	}

	protected synchronized void processStateRemoval( State state ) {
		switch (state) {
			case BURNING:
				if (burning != null) {
					burning.on = false;
					burning = null;
				}
				break;
			case LEVITATING:
				if (levitation != null) {
					levitation.on = false;
					levitation = null;
				}
				break;
			case INVISIBLE:
				if (invisible != null) {
					invisible.killAndErase();
					invisible = null;
				}
				alpha(1f);
				break;
			case PARALYSED:
				paused = false;
				break;
			case FROZEN:
				if (iceBlock != null) {
					iceBlock.melt();
					iceBlock = null;
				}
				break;
			case ILLUMINATED:
				if (light != null) {
					light.putOut();
					light = null;
				}
				break;
			case CHILLED:
				if (chilled != null) {
					chilled.on = false;
					chilled = null;
				}
				break;
			case DARKENED:
				if (darkBlock != null) {
					darkBlock.lighten();
					darkBlock = null;
				}
				break;
			case MARKED:
				if (marked != null) {
					marked.on = false;
					marked = null;
				}
				break;
			case HEALING:
				if (healing != null) {
					healing.on = false;
					healing = null;
				}
				break;
			case SHIELDED:
				if (shield != null) {
					shield.putOut();
				}
				break;
			case HEARTS:
				if (hearts != null) {
					hearts.on = false;
					hearts = null;
				}
				break;
			case GLOWING:
				if (glowBlock != null){
					glowBlock.darken();
					glowBlock = null;
				}
				break;
			case AURA:
				if (aura != null){
					aura.killAndErase();
					aura = null;
				}
				break;
			case ELECTRIC:
				if (electric != null){
					electric.on = false;
					electric = null;
				}
				break;
			case BUTTER:
				if (butter != null) {
					butter.dissapear();
					butter = null;
				}
				break;
			case HALOMETHANEBURNING:
				if (halomethaneBurning != null) {
					halomethaneBurning.on = false;
					halomethaneBurning = null;
				}
				break;
			case HEATSTROKE:
				if (heatstroke != null) {
					heatstroke.on = false;
					heatstroke = null;
				}
				break;
			case HYPOTHERMIA:
				if (hypothermia != null) {
					hypothermia.on = false;
					hypothermia = null;
				}
				break;
		}
	}
	
	@Override
	public void update() {
		if (paused && ch != null && curAnim != null && !curAnim.looped && !finished){
			listener.onComplete(curAnim);
			finished = true;
		}
		
		super.update();
		
		if (flashTime > 0 && (flashTime -= Game.elapsed) <= 0) {
			resetColor();
		}

		synchronized (State.class) {
			for (State s : stateAdditions) {
				processStateAddition(s);
			}
			stateAdditions.clear();
			for (State s : stateRemovals) {
				processStateRemoval(s);
			}
			stateRemovals.clear();
		}

		if (burning != null) {
			burning.visible = visible;
		}
		if (halomethaneBurning != null) {
			halomethaneBurning.visible = visible;
		}
		if (levitation != null) {
			levitation.visible = visible;
		}
		if (iceBlock != null) {
			iceBlock.visible = visible;
		}
		if (light != null) {
			light.visible = visible;
		}
		if (chilled != null) {
			chilled.visible = visible;
		}
		if (darkBlock != null) {
			darkBlock.visible = visible;
		}
		if (marked != null) {
			marked.visible = visible;
		}
		if (healing != null) {
			healing.visible = visible;
		}
		if (hearts != null) {
			hearts.visible = visible;
		}
		//shield fx updates its own visibility
		if (aura != null) {
			if (aura.parent == null) {
				aura.show(this, 0);
			}
			aura.visible = visible;
			aura.point(center());
		}
		if (glowBlock != null){
			glowBlock.visible =visible;
		}
		if (heatstroke != null){
			heatstroke.visible = visible;
		}
		if (hypothermia != null){
			hypothermia.visible = visible;
		}
		if (electric != null){
			electric.visible = visible;
		}

		if (sleeping) {
			showSleep();
		} else {
			hideSleep();
		}
		synchronized (EmoIcon.class) {
			if (emo != null && emo.alive) {
				emo.visible = visible;
			}
		}
	}
	
	@Override
	public void resetColor() {
		super.resetColor();
		if (invisible != null){
			alpha(0.4f);
		}
	}
	
	public void showSleep() {
		synchronized (EmoIcon.class) {
			if (!(emo instanceof EmoIcon.Sleep)) {
				if (emo != null) {
					emo.killAndErase();
				}
				emo = new EmoIcon.Sleep(this);
				emo.visible = visible;
			}
		}
		idle();
	}
	
	public void hideSleep() {
		synchronized (EmoIcon.class) {
			if (emo instanceof EmoIcon.Sleep) {
				emo.killAndErase();
				emo = null;
			}
		}
	}
	
	public void showAlert() {
		synchronized (EmoIcon.class) {
			if (!(emo instanceof EmoIcon.Alert)) {
				if (emo != null) {
					emo.killAndErase();
				}
				emo = new EmoIcon.Alert(this);
				emo.visible = visible;
			}
		}
	}
	
	public void hideAlert() {
		synchronized (EmoIcon.class) {
			if (emo instanceof EmoIcon.Alert) {
				emo.killAndErase();
				emo = null;
			}
		}
	}

	public void showInvestigate() {
		synchronized (EmoIcon.class) {
			if (!(emo instanceof EmoIcon.Investigate)) {
				if (emo != null) {
					emo.killAndErase();
				}
				emo = new EmoIcon.Investigate(this);
				emo.visible = visible;
			}
		}
	}

	public void hideInvestigate() {
		synchronized (EmoIcon.class) {
			if (emo instanceof EmoIcon.Investigate) {
				emo.killAndErase();
				emo = null;
			}
		}
	}
	
	public void showLost() {
		synchronized (EmoIcon.class) {
			if (!(emo instanceof EmoIcon.Lost)) {
				if (emo != null) {
					emo.killAndErase();
				}
				emo = new EmoIcon.Lost(this);
				emo.visible = visible;
			}
		}
	}
	
	public void hideLost() {
		synchronized (EmoIcon.class) {
			if (emo instanceof EmoIcon.Lost) {
				emo.killAndErase();
				emo = null;
			}
		}
	}

	public void hideEmo(){
		synchronized (EmoIcon.class) {
			if (emo != null) {
				emo.killAndErase();
				emo = null;
			}
		}
	}
	
	@Override
	public void kill() {
		super.kill();
		
		hideEmo();
		
		for( State s : State.values()){
			processStateRemoval(s);
		}
		
		if (health != null){
			health.killAndErase();
		}
	}

	private float[] shadowMatrix = new float[16];

	@Override
	protected void updateMatrix() {
		super.updateMatrix();
		Matrix.copy(matrix, shadowMatrix);
		Matrix.translate(shadowMatrix,
				(width * (1f - shadowWidth)) / 2f,
				(height * (1f - shadowHeight)) + shadowOffset);
		Matrix.scale(shadowMatrix, shadowWidth, shadowHeight);
	}

	@Override
	public void draw() {
		if (texture == null || (!dirty && buffer == null))
			return;

		if (renderShadow) {
			if (dirty) {
				((Buffer)verticesBuffer).position(0);
				verticesBuffer.put(vertices);
				if (buffer == null)
					buffer = new Vertexbuffer(verticesBuffer);
				else
					buffer.updateVertices(verticesBuffer);
				dirty = false;
			}

			NoosaScript script = script();

			texture.bind();

			script.camera(camera());

			updateMatrix();

			script.uModel.valueM4(shadowMatrix);
			script.lighting(
					0, 0, 0, am * .6f,
					0, 0, 0, aa * .6f);

			script.drawQuad(buffer);
		}

		super.draw();

	}

	@Override
	public void onComplete( Tweener tweener ) {
		if (tweener == jumpTweener) {

			if (visible && Dungeon.level.water[ch.pos] && !ch.flying) {
				GameScene.ripple( ch.pos );
			}
			if (jumpCallback != null) {
				jumpCallback.call();
			}
			GameScene.sortMobSprites();

		} else if (tweener == motion) {

			synchronized (this) {
				isMoving = false;

				motion.killAndErase();
				motion = null;
				ch.onMotionComplete();
				// Net MP: if this sprite is mid-chain (driven by NetVisuals queue
				// for a remote actor), kick the next step / idle. Needed because
				// Hero.onMotionComplete() doesn't know about NetVisuals — without
				// this hook, multi-step chains for the client's own hero stall
				// after step 1 and the sprite stays in 'run'.
				xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.onSpriteMotionComplete(this);

				GameScene.sortMobSprites();
				notifyAll();
			}

		}
	}

	@Override
	public synchronized void onComplete( Animation anim ) {
		
		if (animCallback != null) {
			Callback executing = animCallback;
			animCallback = null;
			executing.call();
		} else {
			
			if (anim == attack) {
				
				idle();
				ch.onAttackComplete();
				
			} else if (anim == operate) {
				
				idle();
				ch.onOperateComplete();
				
			}
			
		}
	}

	private static class JumpTweener extends Tweener {

		public CharSprite visual;

		public PointF start;
		public PointF end;

		public float height;

		public JumpTweener( CharSprite visual, PointF pos, float height, float time ) {
			super( visual, time );

			this.visual = visual;
			start = visual.point();
			end = pos;

			this.height = height;
		}

		@Override
		protected void updateValues( float progress ) {
			float hVal = -height * 4 * progress * (1 - progress);
			visual.point( PointF.inter( start, end, progress ).offset( 0, hVal ) );
			visual.shadowOffset = 0.25f - hVal*0.8f;
		}
	}
}
