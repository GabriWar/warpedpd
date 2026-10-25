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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Corruption;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Doom;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FiendBurning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Surging;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantNet;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;

/**
 * What a character wears while its states last - burning, frost, shadow, a torch, a shield of
 * light, vines, bubbles of poison - drawn from the states its sprite holds (CharSprite.add and
 * remove, replicated to co-op guests by name), so a guest sees what the host sees. One per
 * sprite, made on its first state.
 *
 * Threads: a state is added and removed under the sprite's own locks (CharSprite) and only
 * written down here; the looks are made, stepped and drawn on the render thread (update, the
 * draws); a sprite killed on any thread lets go of everything at once (release). Nothing here
 * takes a character's lock while holding its own: what a look reads off the character (the chill
 * left, which dark it is, which fire, whether it breathes) is read first, outside.
 */
public final class StateLooks {

	private static final CharSprite.State[] STATES = CharSprite.State.values();

	//what was asked of each state since the last frame
	private static final byte NONE = 0, ADD = 1, REMOVE = 2;

	private final CharSprite sprite;
	private final Look[] looks = new Look[STATES.length];
	private final byte[] asked = new byte[STATES.length];
	private boolean anyAsked;
	private boolean released;

	//the colour the chill lays on the sprite (0 none), and whether this sprite tinted it last
	private float chill;
	private boolean tinted;
	//read from the character, outside the locks: the chill's strength, which dark, which fire,
	//whether it breathes (its properties read its buffs)
	private float chillRead = CHILL_DEFAULT;
	private int darkRead = DARK;
	private Element fireRead = Element.FIRE;
	private boolean breathesRead;
	//a surge's stacks (Surging.SurgeTracker.stacks), read every SURGE_EVERY s while SURGING is on;
	//a guest's character holds no buffs, so it keeps the last it knew (one at first)
	private volatile int surgeRead = 1;
	//whether a PARALYSED is a real stun (a Paralysis buff), read with the others: a frost, a time
	//freeze, a duel and a fading image only pause the body and show no stars. A guest's character
	//holds no buffs, so his stays true (his ice still hides them: FROZEN comes by name)
	private volatile boolean stunRead = true;
	private float readIn, surgeIn;
	private boolean wasFlashing;

	/** The chill's tint (Chill.cooldown() / Chill.DURATION from 0.15 to 0.35), and its colour. */
	public static final int CHILL_COLOR = 0xB2D6FF;
	static final float CHILL_MIN = 0.15f, CHILL_MAX = 0.35f, CHILL_DEFAULT = 0.25f;
	/** The darks: a darkened body's own, doom's, corruption's. */
	public static final int DARK = 0x6A4A8A, DOOM = 0x8A2A4A, CORRUPTION = 0x9050C0;
	/** How often the buffs behind the looks are read again, s; a surge's stacks more often. */
	static final float READ_EVERY = 0.5f, SURGE_EVERY = 0.25f;

	public StateLooks( CharSprite sprite ){
		this.sprite = sprite;
	}

	// ------------------------------------------------------------------ asked (under the sprite's locks)

	/** The state was added (CharSprite.processStateAddition). Any thread; nothing made yet. */
	public synchronized void add( CharSprite.State state ){
		if (released) return;
		asked[state.ordinal()] = ADD;
		anyAsked = true;
		readIn = 0;
	}

	/** The state was removed (CharSprite.processStateRemoval). Any thread. */
	public synchronized void remove( CharSprite.State state ){
		if (released) return;
		int i = state.ordinal();
		if (looks[i] != null || asked[i] == ADD){
			asked[i] = REMOVE;
			anyAsked = true;
		}
	}

	/** Whether a look of the state is on (not ending). */
	public synchronized boolean has( CharSprite.State state ){
		Look l = looks[state.ordinal()];
		return l != null && !l.stopping;
	}

	/** The look of a state, null for none (tests). */
	synchronized Look look( CharSprite.State state ){
		return looks[state.ordinal()];
	}

	/** How many looks are on or ending. */
	public synchronized int count(){
		int n = 0;
		for (Look l : looks) if (l != null) n++;
		return n;
	}

	// ------------------------------------------------------------------ the frame (render thread)

	/** Once a frame, from CharSprite.update: the looks asked for made or ended, then stepped. */
	public void update(){
		float dt = Game.elapsed;
		readBuffs( dt );
		synchronized (this){
			if (released) return;
			if (anyAsked){
				anyAsked = false;
				for (int i = 0; i < looks.length; i++){
					byte a = asked[i];
					if (a == NONE) continue;
					asked[i] = NONE;
					Look l = looks[i];
					if (a == ADD){
						if (l != null && !l.stopping){
							l.again();
							continue;
						}
						if (l != null) l.release();
						l = looks[i] = make( STATES[i] );
						if (l != null) l.start();
					} else if (l != null && !l.stopping){
						l.stopping = true;
						l.stop();
					}
				}
			}
			boolean flashing = sprite.isFlashing();
			boolean flashed = flashing && !wasFlashing;
			wasFlashing = flashing;
			for (int i = 0; i < looks.length; i++){
				Look l = looks[i];
				if (l == null) continue;
				if (flashed && !l.stopping) l.flashed();
				l.age += dt;
				if (!l.step( dt )){
					l.release();
					looks[i] = null;
				}
			}
			colour();
		}
	}

	//the buffs the looks go by, read every READ_EVERY s (and as a state is added), outside this
	//one's lock: the chill's strength, the dark's kind, the fire's
	private void readBuffs( float dt ){
		Char ch = sprite.ch;
		if (ch == null) return;
		if ((surgeIn -= dt) <= 0){
			surgeIn = SURGE_EVERY;
			boolean surging;
			synchronized (this){
				surging = asked[CharSprite.State.SURGING.ordinal()] == ADD || looks[CharSprite.State.SURGING.ordinal()] != null;
			}
			if (surging){
				Surging.SurgeTracker t = ch.buff( Surging.SurgeTracker.class );
				if (t != null){
					int n = Math.max( 1, t.stacks );
					if (n != surgeRead){
						surgeRead = n;
						//the host's guests count them too (their characters hold no buffs)
						EnchantNet.sendSurge( ch, n );
					}
				}
			}
		}
		if ((readIn -= dt) > 0) return;
		readIn = READ_EVERY;
		boolean chilled, cold, dark, burning, paralysed;
		synchronized (this){
			paralysed = asked[CharSprite.State.PARALYSED.ordinal()] == ADD || looks[CharSprite.State.PARALYSED.ordinal()] != null;
			chilled = asked[CharSprite.State.CHILLED.ordinal()] == ADD || looks[CharSprite.State.CHILLED.ordinal()] != null;
			cold = asked[CharSprite.State.CHILLED.ordinal()] == ADD || asked[CharSprite.State.HYPOTHERMIA.ordinal()] == ADD;
			dark = asked[CharSprite.State.DARKENED.ordinal()] == ADD || looks[CharSprite.State.DARKENED.ordinal()] != null;
			burning = asked[CharSprite.State.BURNING.ordinal()] == ADD;
		}
		if (chilled){
			Chill c = ch.buff( Chill.class );
			chillRead = c == null ? CHILL_DEFAULT : chillStrength( c.cooldown() );
		}
		if (cold){
			breathesRead = !Char.hasProp( ch, Char.Property.INORGANIC ) && !Char.hasProp( ch, Char.Property.UNDEAD );
		}
		if (dark){
			darkRead = ch.buff( Doom.class ) != null ? DOOM : (ch.buff( Corruption.class ) != null ? CORRUPTION : DARK);
		}
		if (burning){
			fireRead = ch.buff( FiendBurning.class ) != null ? Element.FIEND : Element.FIRE;
		}
		if (paralysed && !NetManager.isNetClient()){
			stunRead = ch.buff( Paralysis.class ) != null;
		}
	}

	/** The chill's tint for the turns it has left: from CHILL_MIN, full at Chill.DURATION. */
	public static float chillStrength( float left ){
		return CHILL_MIN + (CHILL_MAX - CHILL_MIN) * Math.max( 0f, Math.min( 1f, left / Chill.DURATION ) );
	}

	/** A surge's stacks as last read (SURGING's motes). */
	public int surge(){
		return surgeRead;
	}

	/** A co-op guest's: the stacks the host read for this character (EnchantNet). Any thread. */
	public void surgeSent( int stacks ){
		surgeRead = Math.max( 1, stacks );
	}

	/** Whether the stun stars show now: PARALYSED's look is on, it is a real stun and no ice
	 *  holds the body (a frozen body is paused, not stunned). */
	public synchronized boolean stunStars(){
		Look l = looks[CharSprite.State.PARALYSED.ordinal()];
		return l != null && !l.stopping && stunShown();
	}

	//a stun's stars may show: a real stun, and no ice over it (the stars' look asks every frame)
	synchronized boolean stunShown(){
		Look ice = looks[CharSprite.State.FROZEN.ordinal()];
		return stunRead && (ice == null || ice.stopping);
	}

	/** The dark a darkened sprite takes now. */
	public int dark(){
		return darkRead;
	}

	//the chill's tint, while nothing else holds the sprite's colour (a flash, ice, dark, glow,
	//butter); let go of once
	private void colour(){
		Look c = looks[CharSprite.State.CHILLED.ordinal()];
		Look h = looks[CharSprite.State.HYPOTHERMIA.ordinal()];
		boolean on = (c != null && !c.stopping) || (h != null && !h.stopping);
		chill = on ? (h != null && !h.stopping ? Math.max( chillRead, CHILL_MAX ) : chillRead) : 0f;
		if (chill > 0 && sprite.tintFree( null )){
			sprite.tint( CHILL_COLOR, chill );
			tinted = true;
		} else if (tinted && chill <= 0){
			tinted = false;
			if (sprite.tintFree( null )) sprite.resetColor();
		}
	}

	/** The chill's tint on the sprite now, 0 for none. */
	public float chill(){
		return chill;
	}

	/** Right before the sprite draws (after its shadow): what lies behind it. Render thread. */
	public synchronized void drawBehind(){
		if (released || !sprite.visible) return;
		for (Look l : looks) if (l != null) l.drawBehind();
	}

	/** Right after the sprite draws: what lies on it. Render thread. */
	public synchronized void drawOver(){
		if (released || !sprite.visible) return;
		for (Look l : looks) if (l != null) l.drawOver();
	}

	/** The sprite was killed or destroyed: every look let go of now. Any thread. */
	public synchronized void release(){
		released = true;
		for (int i = 0; i < looks.length; i++){
			if (looks[i] != null){
				looks[i].release();
				looks[i] = null;
			}
			asked[i] = NONE;
		}
	}

	// ------------------------------------------------------------------ the looks

	private Look make( CharSprite.State s ){
		CharSprite sp = sprite;
		switch (s){
			case BURNING:            return new Looks.Burning( sp, fireRead, true );
			case HALOMETHANEBURNING: return new Looks.Burning( sp, Element.HALOMETHANE, false );
			case HEATSTROKE:         return new Looks.Heat( sp );
			case FROZEN:             return new Looks.Frozen( sp );
			case CHILLED:            return new Looks.Cold( sp, false, breathesRead );
			case HYPOTHERMIA:        return new Looks.Cold( sp, true, breathesRead );
			case DARKENED:           return new Looks.Darkened( sp );
			case MARKED:             return new Looks.Marked( sp );
			case HEALING:            return new Looks.Healing( sp );
			case GLOWING:            return new Looks.Glowing( sp );
			case ILLUMINATED:        return new Looks.Illuminated( sp, this );
			case TORCHLIT:           return new Looks.Torchlit( sp );
			case AURA:               return new Looks.Aura( sp );
			case LEVITATING:         return new Looks.Levitating( sp );
			case ELECTRIC:           return new Looks.Electric( sp );
			case BUTTER:             return new Looks.Butter( sp );
			case HEARTS:             return new Looks.Hearts( sp );
			case POISONED:           return new Looks.Bubbles( sp, Looks.Bubbles.POISON );
			case CORRODED:           return new Looks.Bubbles( sp, Looks.Bubbles.ACID );
			case OOZED:              return new Looks.Bubbles( sp, Looks.Bubbles.OOZE );
			case ROOTED:             return new Looks.Rooted( sp );
			case BARKED:             return new Looks.Barked( sp );
			case VERDANT:            return new Looks.Verdant( sp );
			case HERB:               return new Looks.Herb( sp );
			case PARALYSED:          return sp.ch == null ? null : new Looks.Stunned( sp, this );
			case SEEDED:             return new Looks.Seeded( sp );
			case SURGING:            return new Looks.Surge( sp, this );
			case BOAT_RAFT:          return new BoatLook( sp, false );
			case BOAT_SAIL:          return new BoatLook( sp, true );
			case RIDING:             return new RiddenHorse( sp );
			default:                 return null;
		}
	}

	/** One state's look on one sprite: made on the render thread as the state comes, stepped
	 *  every frame, ended as it goes (stop: its leaving, then step says when it is done) and let
	 *  go of (release: any thread, at once). */
	abstract static class Look {

		final CharSprite s;
		float age;
		volatile boolean stopping;

		Look( CharSprite s ){
			this.s = s;
		}

		/** Its pictures made: the state just came. */
		void start(){}

		/** The state was added again while it was on. */
		void again(){}

		/** A frame: false when it is over (only after its stop). */
		boolean step( float dt ){
			return !stopping;
		}

		/** The state went: what its leaving does. */
		void stop(){}

		/** The sprite flashed (a blow landed) while it was on. */
		void flashed(){}

		void drawBehind(){}

		void drawOver(){}

		/** Everything let go of now. Any thread. */
		void release(){}

		// -------------------------------------------------------------- shared

		//the sprite's middle and its feet
		float cx(){
			return s.x + s.width() / 2f;
		}

		float cy(){
			return s.y + s.height() / 2f;
		}

		float feet(){
			return s.y + s.height();
		}

		/** A kit emitter (light or matter) pouring over a share of the sprite's box (left, top,
		 *  width, height as shares), kept alive until let go of (stopPour). Null off the scene. */
		FxEmitter pour( boolean light, Emitter.Factory f, float interval, float l, float t, float w, float h ){
			FxEmitter e = light ? GameScene.lightEmitter() : GameScene.fxEmitter();
			if (e == null) return null;
			e.pos( s, s.width() * l, s.height() * t, s.width() * w, s.height() * h );
			e.fillTarget = false;
			e.autoKill = false;
			e.pour( f, interval );
			return e;
		}

		/** n made over a share of the sprite's box, one every `interval` s, then the emitter goes. */
		void once( boolean light, Emitter.Factory f, float interval, int n, float l, float t, float w, float h ){
			FxEmitter e = light ? GameScene.lightEmitter() : GameScene.fxEmitter();
			if (e == null) return;
			e.pos( s, s.width() * l, s.height() * t, s.width() * w, s.height() * h );
			e.fillTarget = false;
			e.start( f, interval, n );
		}

		/** A share of the sprite's box for an emitter already pouring (the sprite turned). */
		void place( FxEmitter e, float l, float t, float w, float h ){
			if (e == null) return;
			e.x = s.width() * l;
			e.y = s.height() * t;
			e.width = s.width() * w;
			e.height = s.height() * h;
		}

		/** An emitter's pour stopped: what it made fades out, then it goes. Any thread. */
		static void stopPour( Emitter e ){
			if (e == null) return;
			e.on = false;
			e.autoKill = true;
		}

		/** Shown, and pouring, only while the sprite is seen (a pour kept alive: it never dies of
		 *  being paused). */
		void shown( Emitter e ){
			if (e != null) e.visible = e.on = s.visible;
		}
	}
}
