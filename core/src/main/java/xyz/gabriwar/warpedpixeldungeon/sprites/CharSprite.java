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
import xyz.gabriwar.warpedpixeldungeon.effects.ButterEffect;
import xyz.gabriwar.warpedpixeldungeon.effects.DarkBlock;
import xyz.gabriwar.warpedpixeldungeon.effects.EmoIcon;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.IceBlock;
import xyz.gabriwar.warpedpixeldungeon.effects.GlowBlock;
import xyz.gabriwar.warpedpixeldungeon.effects.ShieldHalo;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModules;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.StateLooks;
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
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.noosa.tweeners.PosTweener;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;
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

	public boolean visibleOutOfFFOV = false;

	//the last eight came with the effects kit: the characters area draws them, any area may add them
	//(replicated to co-op guests by name, as every state)
	public enum State {
		BURNING, LEVITATING, INVISIBLE, PARALYSED, FROZEN, ILLUMINATED, CHILLED, DARKENED, MARKED, HEALING, SHIELDED, HEARTS, GLOWING, AURA, ELECTRIC, BUTTER, HALOMETHANEBURNING, HEATSTROKE, HYPOTHERMIA,
		TORCHLIT, ROOTED, BARKED, VERDANT, HERB, POISONED, CORRODED, OOZED
	}
	
	protected Animation idle;
	protected Animation run;
	protected Animation attack;
	protected Animation operate;
	protected Animation zap;
	protected Animation die;
	
	protected Callback animCallback;
	
	protected PosTweener motion;

	//what the states wear (burning, frost, shadow, a torch, vines, bubbles...: effects/fx/combat),
	//made with the first of them
	protected StateLooks looks;

	protected IceBlock iceBlock;
	protected DarkBlock darkBlock;
	protected GlowBlock glowBlock;
	protected ShieldHalo shield;
	protected AlphaTweener invisible;
	protected Flare aura;
	protected ButterEffect butter;

	//an offset of the sprite on the screen only (never its x and y): a recoil from a blow, a
	//squash against a wall (nudge), a shiver (shiver), whole pixels
	private volatile int nudgeX, nudgeY;
	private volatile float nudgeLeft;
	private volatile int shiverX;
	private int shownOffX, shownOffY;

	protected EmoIcon emo;
	protected CharHealthIndicator health;

	private Tweener jumpTweener;
	private Callback jumpCallback;

	protected float flashTime = 0;
	
	protected boolean sleeping = false;

	public Char ch;

	//used to prevent the actor associated with this sprite from acting until movement completes
	public volatile boolean isMoving = false;

	/** Each effects area's own state for this sprite, at its index (FxModules), the kit's last. */
	public final Object[] fxSlots = new Object[FxModules.COUNT + 1];
	
	public CharSprite() {
		super();
		listener = this;
	}
	
	@Override
	public void play(Animation anim) {
		//Shouldn't interrupt the dying animation
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

        // HeroSprite links from its constructor, before GameScene adds it to a layer.
        // Parent-owned buff effects must wait until that layer exists.
        pendingBuffVisuals=parent==null;
        if(!pendingBuffVisuals)ch.updateSpriteState();
	}

	@Override
	public void destroy() {
		super.destroy();
		if (looks != null) looks.release();
		if (ch != null && ch.sprite == this){
			ch.sprite = null;
		}
	}

	//used for just updating a sprite based on a given character, not linking them or placing in the game
	public void linkVisuals( Char ch ){
		//do nothing by default
	}
	
	public PointF worldToCamera( int cell ) {
		
		final int csize = DungeonTilemap.SIZE;
		
		return new PointF(
			PixelScene.align(Camera.main, ((cell % Dungeon.level.width()) + 0.5f) * csize - width() * 0.5f),
			PixelScene.align(Camera.main, ((cell / Dungeon.level.width()) + 1.0f) * csize - height() - csize * perspectiveRaise
					+ groundDrop())
		);
	}

	//a flier knocked out of the air (frozen or paralysed, see Char.loseFlight) is drawn lying on
	//the floor: its frame is cut to the solid art (flying frames carry empty rows and often a
	//faint baked shadow under the body) and the sprite sinks its whole perspective raise, so the
	//lowest solid pixel of the creature rests on the bottom edge of its cell, on the engine's
	//shadow, like a walker's feet
	private float groundDrop(){
		if (ch == null || !ch.groundedFlier()) return 0;
		return DungeonTilemap.SIZE * perspectiveRaise;
	}

	//the frame on show, cut to end under the creature: the empty rows go, and so does a faint
	//band standing apart from the body below it (a shadow baked into a flying frame). faint
	//rows that touch the body (translucent wings, a ghost's tail) are part of it and stay
	private RectF groundedFrame( RectF full ){
		if (texture == null || texture.bitmap == null || full == null) return full;
		int x0 = Math.round( full.left * texture.width ), x1 = Math.round( full.right * texture.width );
		int y0 = Math.round( full.top * texture.height ), y1 = Math.round( full.bottom * texture.height );
		int lastAny = -1, lastSolid = -1;
		boolean detached = false;
		for (int y = y1 - 1; y >= y0 && lastSolid < 0; y--){
			int peak = 0;
			for (int x = x0; x < x1; x++) peak = Math.max( peak, texture.getPixel( x, y ) >>> 24 );
			if (peak >= 128) lastSolid = y;
			else if (peak > 0 && lastAny < 0) lastAny = y;
			else if (peak == 0 && lastAny >= 0) detached = true;
		}
		if (lastSolid < 0) return full;
		int end = (lastAny > lastSolid && !detached ? lastAny : lastSolid) + 1;
		return end == y1 ? full : new RectF( full.left, full.top, full.right, end / (float) texture.height );
	}

	//whether the sprite is drawn on the floor right now; the char's grounding is polled every
	//frame (see update), so the drop never depends on a callback reaching the render thread
	private boolean groundedShown = false;
	private RectF groundedCut;
	private PosTweener groundTween;

	//the char lost or regained flight: the sprite drops to the floor or rises back, briefly animated
	private void followGrounding(){
		boolean grounded = ch.groundedFlier();
		if (grounded && groundedCut != null && frame != groundedCut){
			//the animation put a full frame back (a paused clip can still be re-played): cut it again
			frame( groundedCut = groundedFrame( frame ) );
		}
		if (grounded == groundedShown || isMoving || jumpTweener != null || parent == null
				|| ch.pos < 0 || Dungeon.level == null || ch.pos >= Dungeon.level.length()) return;
		groundedShown = grounded;
		if (grounded){
			frame( groundedCut = groundedFrame( frame ) );
		} else {
			groundedCut = null;
			if (curAnim != null) frame( curAnim.frames[curFrame] );
		}
		if (groundTween != null) groundTween.killAndErase();
		groundTween = new PosTweener( this, worldToCamera( ch.pos ), grounded ? 0.15f : 0.35f );
		parent.add( groundTween );
	}

	public void place( int cell ) {
		point( worldToCamera( cell ) );
	}
	
	//Lock order. The render thread always reaches a sprite through its group: Group.update
	//holds the group while it updates each child, and the child then takes its own lock
	//(MovieClip.updateAnimation, onComplete). A method here that holds the sprite's lock and
	//then touches the group (parent.add, killAndErase -> parent.erase) takes the two the
	//other way round, and with the actor thread in it both threads stop for good - the
	//"render thread stalled" freeze, near certain on a floor with enough sprites to make
	//Group.update long. So whatever moves the sprite takes the group first, then itself.
	private void withGroupThenSelf( Runnable body ){
		for (;;){
			com.watabou.noosa.Group holder = parent;
			synchronized (holder != null ? holder : this){
				//handed to another group while this waited: take that one instead
				if (parent != holder) continue;
				synchronized (this){
					body.run();
				}
				return;
			}
		}
	}

	/** Forced movement replaces any visual travel that still points at the old tile. */
	public void snapToPosition(final int cell){
		withGroupThenSelf( new Runnable(){
			@Override
			public void run(){
				if (motion != null){ motion.killAndErase(); motion = null; }
				if (groundTween != null){ groundTween.killAndErase(); groundTween = null; }
				place(cell);
				finishJump(false);
				isMoving = false;
				CharSprite.this.notifyAll();
			}
		} );
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
	
	public void move( final int from, final int to ) {
		withGroupThenSelf( new Runnable(){
			@Override
			public void run(){
				finishJump(true);
				//A new step may arrive before a previous visual step has finished.
				//Keep exactly one writer of the sprite position.
				if (motion != null){
					motion.killAndErase();
					motion = null;
				}
				if (groundTween != null){
					groundTween.killAndErase();
					groundTween = null;
				}
				turnTo( from , to );

				play( run );

				motion = new PosTweener( CharSprite.this, worldToCamera( to ), moveInterval );
				motion.listener = CharSprite.this;
				parent.add( motion );

				isMoving = true;
			}
		} );

		//neither of these touches the sprite's own state: no lock held for them
		FxModules.stepped( this, from, to );

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

	public void jump( final int from, final int to, final float height, final float duration, final Callback callback ) {
		withGroupThenSelf( new Runnable(){
			@Override
			public void run(){
				finishJump(true);
				//A walk/grounding tween must not keep writing coordinates during the jump.
				if (motion != null){
					motion.killAndErase();
					motion = null;
				}
				if (groundTween != null){
					groundTween.killAndErase();
					groundTween = null;
				}
				isMoving = true;
				jumpCallback = callback;

				jumpTweener = new JumpTweener( CharSprite.this, worldToCamera( to ), height, duration );
				jumpTweener.listener = CharSprite.this;
				parent.add( jumpTweener );

				turnTo( from, to );
			}
		} );
	}

	/** Whether it is in the air, mid-jump. */
	public boolean jumping(){
		return jumpTweener != null;
	}

	private synchronized void finishJump(boolean snapToActor){
		if (jumpTweener == null) return;
		jumpTweener.killAndErase();
		jumpTweener = null;
		Callback completed = jumpCallback;
		jumpCallback = null;
		isMoving = false;
		shadowOffset = 0.25f;
		try {
			if (snapToActor && ch != null) place(ch.pos);
			if (completed != null) completed.call();
		} finally {
			notifyAll();
		}
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

	/**
	 * A blow landing on this sprite from a point (the attacker's middle) for some damage (0: it
	 * was blocked), in a style (HitFx: by the weapon's sound): the hit star, what the weapon
	 * leaves, a recoil on the screen. Any thread; drawn on the render thread, from the effects'
	 * own random numbers.
	 */
	public void hitFx( float fromX, float fromY, int damage, int style, boolean heroInvolved ) {
		HitFx.hit( this, fromX, fromY, damage, style, heroInvolved );
	}

	/** A blow from a point that missed this sprite: a whoosh past it, a puff at its feet. */
	public void dodgeFx( float fromX, float fromY ) {
		HitFx.dodged( this, fromX, fromY );
	}

	public void flash() {
		ra = ba = ga = 1f;
		flashTime = FLASH_INTERVAL;
		xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.recordFlash(ch);
	}

	/** Whether a blow's white flash holds the sprite's colour now. */
	public boolean isFlashing() {
		return flashTime > 0;
	}

	/**
	 * Whether the sprite's colour is free for `block` to tint: not while it flashes, and not while
	 * a look that comes before it holds it (ice, then dark, then glow, then butter, then the
	 * chill's tint, which passes null). A block that is none of this sprite's own (made apart
	 * from its states) gives way to the flash only.
	 */
	public boolean tintFree( com.watabou.noosa.Gizmo block ) {
		if (flashTime > 0) return false;
		if (block != null && block != iceBlock && block != darkBlock && block != glowBlock && block != butter) return true;
		//the first of them alive holds it
		if (iceBlock != null && iceBlock.alive) return block == iceBlock;
		if (darkBlock != null && darkBlock.alive) return block == darkBlock;
		if (glowBlock != null && glowBlock.alive) return block == glowBlock;
		if (butter != null && butter.alive) return block == butter;
		return true;
	}

	/** The frame on show now, in its texture's uv (the sprite's own: read it, never change it). */
	public RectF frameNow() {
		return frame;
	}

	/** An aura's colour (aura()). */
	public int auraColor() {
		return auraColor;
	}

	/** The dark a darkened sprite is tinted (DarkBlock): doom's, corruption's, or a shadow's. */
	public int darkTint() {
		return looks != null ? looks.dark() : StateLooks.DARK;
	}

	/**
	 * The sprite shifted (dx, dy) whole pixels on the screen for `seconds` (a recoil, a squash
	 * against a wall); its x and y never change. Any thread.
	 */
	public void nudge( int dx, int dy, float seconds ) {
		nudgeX = dx;
		nudgeY = dy;
		nudgeLeft = seconds;
	}

	/** The sprite shifted dx whole pixels sideways on the screen until told otherwise (a shiver
	 *  sets it every frame; 0 ends it). */
	public void shiver( int dx ) {
		shiverX = dx;
	}

	/** How many of its states' looks are on or ending (0 before the first). */
	public int looksCount() {
		StateLooks l = looks;
		return l == null ? 0 : l.count();
	}

	/** Its offset on the screen now, px (x then y), into out. */
	public void screenOffset( int[] out ) {
		boolean n = nudgeLeft > 0;
		out[0] = (n ? nudgeX : 0) + shiverX;
		out[1] = n ? nudgeY : 0;
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

	//the sprite's looks, made with its first state
	private StateLooks looks(){
		if (looks == null) looks = new StateLooks( this );
		return looks;
	}

	protected synchronized void processStateAddition( State state ) {
		switch (state) {
			case BURNING:
			case HALOMETHANEBURNING:
				looks().add( state );
				if (visible) {
					SpatialSound.play(Assets.Sounds.BURNING, ch);
				}
				break;
			case LEVITATING:
			case ILLUMINATED:
			case CHILLED:
			case MARKED:
			case HEALING:
			case HEARTS:
			case ELECTRIC:
			case HEATSTROKE:
			case HYPOTHERMIA:
			case TORCHLIT:
			case ROOTED:
			case BARKED:
			case VERDANT:
			case HERB:
			case POISONED:
			case CORRODED:
			case OOZED:
				looks().add( state );
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
				looks().add( state );
				break;
			case DARKENED:
				if (darkBlock != null) darkBlock.killAndErase();
				darkBlock = DarkBlock.darken(this);
				looks().add( state );
				break;
			case SHIELDED:
				if (shield != null) shield.killAndErase();
				//light: over the night's tint
				GameScene.light(shield = new ShieldHalo(this));
				break;
			case GLOWING:
				if (glowBlock != null) glowBlock.killAndErase();
				glowBlock = GlowBlock.lighten(this);
				looks().add( state );
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
				looks().add( state );
				break;
			case BUTTER:
				if (butter != null) butter.dissapear();
				butter = ButterEffect.butter(this);
				looks().add( state );
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
				if (looks != null) looks.remove( state );
				break;
			case DARKENED:
				if (darkBlock != null) {
					darkBlock.lighten();
					darkBlock = null;
				}
				if (looks != null) looks.remove( state );
				break;
			case SHIELDED:
				if (shield != null) {
					shield.putOut();
				}
				break;
			case GLOWING:
				if (glowBlock != null){
					glowBlock.darken();
					glowBlock = null;
				}
				if (looks != null) looks.remove( state );
				break;
			case AURA:
				if (aura != null){
					aura.killAndErase();
					aura = null;
				}
				if (looks != null) looks.remove( state );
				break;
			case BUTTER:
				if (butter != null) {
					butter.dissapear();
					butter = null;
				}
				if (looks != null) looks.remove( state );
				break;
			default:
				if (looks != null) looks.remove( state );
				break;
		}
	}

	//where the aura's flare is put each frame (no allocation)
	private final PointF auraAt = new PointF();

    private boolean pendingBuffVisuals;
	@Override
	public void update() {
        if(pendingBuffVisuals&&parent!=null&&ch!=null){
            pendingBuffVisuals=false;ch.updateSpriteState();
        }
		if (paused && ch != null && curAnim != null && !curAnim.looped && !finished){
			listener.onComplete(curAnim);
			finished = true;
		}

		super.update();

		if (ch != null) followGrounding();

		if (flashTime > 0 && (flashTime -= Game.elapsed) <= 0) {
			resetColor();
		}
		if (nudgeLeft > 0) {
			nudgeLeft -= Game.elapsed;
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

		if (looks != null) {
			looks.update();
		}

		if (iceBlock != null) {
			iceBlock.visible = visible;
		}
		if (darkBlock != null) {
			darkBlock.visible = visible;
		}
		//shield fx updates its own visibility
		if (aura != null) {
			if (aura.parent == null) {
				aura.show(this, 0);
			}
			aura.visible = visible;
			aura.point(auraAt.set(x + width() / 2f, y + height() / 2f));
		}
		if (glowBlock != null){
			glowBlock.visible =visible;
		}

		if (sleeping) {
			showSleep();
		} else {
			hideSleep();
		}
		synchronized (EmoIcon.class) {
			boolean wandering = !sleeping
					&& ch instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob
					&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob) ch).showsUnaware();
			// Awareness and sleep icons take priority over the idle wandering indicator.
			if (wandering && emo == null) {
				emo = new EmoIcon.Wandering(this);
			} else if (!wandering && emo instanceof EmoIcon.Wandering) {
				emo.killAndErase();
				emo = null;
			}
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
		//every look let go of now: a killed sprite is never stepped again
		if (looks != null) looks.release();

		if (health != null){
			health.killAndErase();
		}

		//what the effects keep for it (its orbits, the areas' slots) let go of
		FxModules.gone( this );
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
		//a recoil, a squash, a shiver: the sprite (not its shadow) shifted on the screen only,
		//straight across whatever its scale or angle (the matrix's own translation)
		int ox = (nudgeLeft > 0 ? nudgeX : 0) + shiverX, oy = nudgeLeft > 0 ? nudgeY : 0;
		if (ox != 0 || oy != 0){
			matrix[12] += ox;
			matrix[13] += oy;
		}
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

		//a sprite with no shadow has its matrix remade only when it moves: an offset coming or going
		//on the screen (a recoil, a shiver) remakes it too
		int ox = (nudgeLeft > 0 ? nudgeX : 0) + shiverX, oy = nudgeLeft > 0 ? nudgeY : 0;
		if (!renderShadow && (ox != shownOffX || oy != shownOffY)) updateMatrix();
		shownOffX = ox;
		shownOffY = oy;

		//what its states wear behind it (a glow, a ring at its feet)
		if (looks != null) looks.drawBehind();
		//what circles it: its far side behind it, its near side and the areas' own over it
		FxModules.beforeDraw( this );
		super.draw();
		FxModules.afterDraw( this );
		//what its states wear on it (frost, vines, a rune, a glint)
		if (looks != null) looks.drawOver();

	}

	@Override
	public void onComplete( Tweener tweener ) {
		if (tweener == jumpTweener) {

			FxModules.jumped( this );
			finishJump(false);
			GameScene.sortMobSprites();

		} else if (tweener == motion) {

			synchronized (this) {
				isMoving = false;

				motion.killAndErase();
				motion = null;
				if (ch != null) ch.onMotionComplete();
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
