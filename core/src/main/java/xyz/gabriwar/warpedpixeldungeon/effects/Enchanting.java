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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.Arc;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.ArcImage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.GainFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.HexOverlay;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.MotifFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.Profile;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.InfusionParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;

//an item being enchanted over its owner's head: its ghost fading in (never scaled up from
//nothing), runes circling it on a 10x4 ellipse (dimmer behind it), sparks spiralling into it as
//the enchantment takes, then it is drawn into its owner - four motes and a glow on him - stepping
//to x2 as it fades. At most DRAWS pictures; let go of with it.
//
//With the look of the magic it gains (v2, docs/fx/enchant.md "Gaining"), the same 1.6 s acts out
//that magic's family: the ghost shows the look's glow and sheds its motif from the start; the six
//runes take the family's frame (embers, ice, sparks, bubbles, leaves, ink, skulls, pebbles, runes,
//stars, glints, beads, or ghosts of the item itself) and tighten as it binds; the sparks spiral in
//in the element's three hottest colours while the family gathers into the ghost (EnchantFx.gather);
//at 1.2 s it binds - a core flash in the element's glint, a ring opening round it, the ghost
//flashing its colour, one light and the sting - and the runes are gone; then the family's finale
//plays at half size on the owner, four motes of it are drawn into him, and the item's slot runs its
//aura faster for a second (EnchantFx.markGained). A curse runs the other way: its runes and VOID motes
//leave the ghost, which darkens to ink and binds in a closing VOID ring with no bright light, the
//CURSED sting, and the hex's finale on the owner. At most PEAK quads at its peak, SETTLE from the
//bind on (the finale's included) and LIGHTS lights. Any thread (show posts itself).
public class Enchanting extends ItemSprite {

	private enum Phase {
		FADE_IN, STATIC, FADE_OUT
	}

	private static final float FADE_IN_TIME		= 0.2f;
	private static final float STATIC_TIME		= 1.0f;
	private static final float FADE_OUT_TIME	= 0.4f;

	private static final float ALPHA	= 0.6f;

	/** Its runes: how many, the ellipse they circle (px), how fast (rad/s), the far half's alpha. */
	public static final int RUNES = 6;
	public static final float RUNE_RX = 5f, RUNE_RY = 2f, RUNE_TURN = 2.4f, BACK = 0.5f;
	/** The sparks spiralling in, and the motes drawn into the owner. */
	public static final int SPARKS = 12, MOTES = 4;
	/** Its pictures at its peak: itself, the runes, the sparks (the motes and glow come after them). */
	public static final int DRAWS = 1 + RUNES + SPARKS;

	/** v2: the sparks (in the element's three hottest colours), the motif's motes the ghost sheds
	 *  at once at most (and in all), how much faster than a slot it sheds them (a window's rate). */
	public static final int SPARKS_V2 = 8, MOTIF_LIVE = 1, MOTIF_MOST = 2;
	public static final float PANEL_RATE = 1.5f;
	/** v2 at its peak: itself, the runes, the sparks, the family's gather, its motif's mote. */
	public static final int PEAK = 1 + RUNES + SPARKS_V2 + EnchantFx.GATHER_CAP + MOTIF_LIVE;
	/** The finale's scale on the owner, the most it makes there (a family's finale at half size),
	 *  and what the bind and the settle make in all: the core and its ring, the finale, the motes. */
	public static final float FINALE_SCALE = 0.5f;
	public static final int FINALE_MOST = Math.round( EnchantFx.FINALE_CAP * FINALE_SCALE );
	public static final int SETTLE = 2 + FINALE_MOST + MOTES;
	/** v2's lights: the bind's, or its finale's own in its place. */
	public static final int LIGHTS = 1;
	/** The gather's length (it lands just before the bind), the ghost's flash at the bind, how long
	 *  before the bind the runes tighten, how far a curse's runes leave (times the ellipse). */
	public static final float GATHER_FOR = 0.95f, BIND_FLASH = 0.05f, TIGHTEN = 0.3f, LEAVE = 1.6f;
	/** The ring opening round the ghost at the bind. */
	public static final int[][] OPENING = { FxFrames.RING_5, FxFrames.RING_9, FxFrames.RING_13, FxFrames.RING_17 };

	private int color;

	private CharSprite owner;
	//v2: the look it gains (null: today's picture), its level, the item (to mark it gained; none
	//on a co-op guest)
	private final EnchantLook look;
	private final int level;
	private final Item item;

	private Phase phase;
	private float duration;
	private float passed;
	private float age;

	private ArcImage[] runes;
	//v2: the runes as frames of the family, or ghosts of the item (MIRAGE); how far they turned
	private Image[] marks;
	private float turn, flashLeft, motifLeft;
	private EnchantFx.Beat motifs;

	public Enchanting( Item item ) {
		this( item.image(), null, item.glowing() != null ? item.glowing().color : -1, 0, item );
	}

	private Enchanting( int image, EnchantLook look, int color, int level, Item item ){
		super( image, null );
		//originToCenter();

		this.color = color;
		this.look = look;
		this.level = level;
		this.item = item;

		phase = Phase.FADE_IN;
		duration = FADE_IN_TIME;
		passed = 0;
	}

	/** The look it gains, null for today's picture. */
	EnchantLook look(){
		return look;
	}

	//the colour its magic shows in: its glow's, or the arcane's light
	private int magic(){
		return color != -1 ? color & 0xFFFFFF : Element.ARCANE.light;
	}

	@Override
	public void update() {
		super.update();

		if (owner == null || owner.parent == null) {
			finish();
			return;
		}

		float cx = owner.x + owner.width() / 2f;
		x = cx - width() / 2;
		y = owner.y - 8 - height()/2;
		float mx = x + width() / 2f, my = y + height() / 2f;
		age += Game.elapsed;

		if (look != null){
			gains( cx, mx, my );
			return;
		}

		switch (phase) {
			case FADE_IN:
				alpha( passed / duration * ALPHA );
				break;
			case STATIC:
				if (color != -1) {
					tint(color, passed / duration * 0.8f);
				}
				break;
			case FADE_OUT:
				alpha( (1 - passed / duration) * ALPHA );
				scale.set( 2 );
				x = cx - width() / 2;
				y = owner.y - 8 - height() / 2;
				break;
		}
		runes( mx, my );

		if ((passed += Game.elapsed) > duration) {
			switch (phase) {
				case FADE_IN:
					phase = Phase.STATIC;
					duration = STATIC_TIME;
					sparks( mx, my );
					break;
				case STATIC:
					phase = Phase.FADE_OUT;
					duration = FADE_OUT_TIME;
					absorb( mx, my );
					break;
				case FADE_OUT:
					finish();
					return;
			}

			passed = 0;
		}
	}

	//six runes circling it, the far half dimmer
	private void runes( float mx, float my ){
		if (runes == null){
			if (!Arc.onScene()) return;
			runes = new ArcImage[RUNES];
			for (int i = 0; i < RUNES; i++){
				runes[i] = ArcImage.get( GameScene.Layer.LIGHTS, false, true );
				runes[i].show( FxFrames.RUNE_3[i % FxFrames.RUNE_3.length] ).rgb( magic() );
			}
		}
		float fade = phase == Phase.FADE_IN ? passed / duration : (phase == Phase.FADE_OUT ? 1 - passed / duration : 1f);
		for (int i = 0; i < RUNES; i++){
			float a = (float)(2 * Math.PI * i / RUNES) + age * RUNE_TURN;
			float sin = (float)Math.sin( a );
			runes[i].centre( mx + (float)Math.cos( a ) * RUNE_RX, my + 6 + sin * RUNE_RY );
			runes[i].am = (sin < 0 ? BACK : 1f) * fade;
		}
	}

	//the enchantment taking hold: sparks spiralling into it
	private void sparks( float mx, float my ){
		FxEmitter e = GameScene.lightEmitter();
		if (e == null) return;
		e.pos( mx, my );
		e.burst( InfusionParticle.spiralIn( magic() ), SPARKS );
	}

	//drawn into its owner: four motes falling into him, a glow on him
	private void absorb( float mx, float my ){
		if (!Arc.onScene()) return;
		float tx = owner.x + owner.width() / 2f, ty = owner.y + owner.height() / 2f;
		for (int i = 0; i < MOTES; i++){
			FxParticle p = Arc.light( mx + (i - 1.5f) * 3f, my, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.MOTE_2 ).paint( magic() ).vel( (tx - mx) / 0.3f, (ty - my) / 0.3f ).life( 0.3f ).alpha( 1f, 0.05f, 0.1f );
		}
		FxLight l = Arc.glow( tx, ty, 15, magic(), 0.5f );
		if (l != null) l.life( 0.25f, 0, 0.15f );
	}

	// ------------------------------------------------------------------ v2: gaining a look

	private void gains( float cx, float mx, float my ){
		switch (phase) {
			case FADE_IN:
				alpha( passed / duration * ALPHA );
				break;
			case STATIC:
				break;
			case FADE_OUT:
				alpha( (1 - passed / duration) * ALPHA );
				scale.set( 2 );
				x = cx - width() / 2;
				y = owner.y - 8 - height() / 2;
				break;
		}
		glow();
		if (phase != Phase.FADE_OUT){
			marks( mx, my );
			motif( mx, my );
		}

		if ((passed += Game.elapsed) > duration) {
			switch (phase) {
				case FADE_IN:
					phase = Phase.STATIC;
					duration = STATIC_TIME;
					gather( mx, my );
					break;
				case STATIC:
					phase = Phase.FADE_OUT;
					duration = FADE_OUT_TIME;
					bind( mx, my );
					break;
				case FADE_OUT:
					finish();
					return;
			}

			passed = 0;
		}
	}

	//the ghost's glow: the look's profile, waking as the hold begins and growing through it; a
	//dark look's darkening with its rim showing; a curse's ink creeping over it; a flash of its
	//colour at the bind
	private void glow(){
		if (flashLeft > 0){
			flashLeft -= Game.elapsed;
			if (look.curse) tint( Element.VOID.ramp.stop( 5 ), 0.9f );
			else tint( look.glow, 1f );
			return;
		}
		float hold = phase == Phase.FADE_IN ? 0f : phase == Phase.STATIC ? passed / duration : 1f;
		if (look.curse){
			tint( Element.VOID.ramp.stop( 4 ), 0.2f + 0.65f * hold );
			return;
		}
		Profile pr = look.profile;
		float v = pr.value( age, 0f );
		if (pr == Profile.INVERT || pr == Profile.CURSE){
			float dark = Math.min( 0.6f, v * (1f + 1.5f * hold) );
			float rim = Profile.RIM_SHARE * (1f + hold);
			rm = gm = bm = 1f - dark;
			ra = ((look.rim >> 16) & 0xFF) / 255f * rim;
			ga = ((look.rim >> 8) & 0xFF) / 255f * rim;
			ba = (look.rim & 0xFF) / 255f * rim;
			return;
		}
		int c = pr == Profile.CYCLE ? Profile.cycleColor( look.cycle, age, 1f ) : look.glow;
		tint( c, Math.min( 0.85f, (0.25f + 0.6f * hold) * v / Math.max( 0.05f, pr.base ) ) );
	}

	//the six runes as the family's frames (or ghosts of the item), circling it; tightening as it
	//binds; a curse's leaving it and gone by the middle of the hold
	private void marks( float mx, float my ){
		if (marks == null){
			if (!Arc.onScene()) return;
			marks = new Image[RUNES];
			boolean lit = look.emissive && !look.curse;
			for (int i = 0; i < RUNES; i++){
				if (look.rune != null){
					ArcImage a = ArcImage.get( lit ? GameScene.Layer.LIGHTS : GameScene.Layer.EFFECTS, false, lit );
					a.show( look.rune ).rgb( runeColor( i ) );
					marks[i] = a;
				} else {
					//a mirage's: ghosts of the item itself, in its rim's colour
					Image g = new Image( this );
					g.scale.set( 0.5f );
					g.hardlight( look.rim );
					owner.parent.add( g );
					marks[i] = g;
				}
			}
		}
		float fade = phase == Phase.FADE_IN ? passed / duration : 1f;
		float r = 1f, spin = RUNE_TURN;
		if (look.curse){
			float t = phase == Phase.FADE_IN ? 0f : Math.min( 1f, passed / (duration * 0.6f) );
			r = 1f + LEAVE * t;
			fade *= 1f - t;
		} else if (phase == Phase.STATIC && passed > duration - TIGHTEN){
			float t = (passed - (duration - TIGHTEN)) / TIGHTEN;
			r = 1f - 0.6f * t;
			spin *= 1f + t;
		}
		turn += Game.elapsed * spin;
		for (int i = 0; i < RUNES; i++){
			float a = (float)(2 * Math.PI * i / RUNES) + turn;
			float sin = (float)Math.sin( a );
			Image m = marks[i];
			float px = mx + (float)Math.cos( a ) * RUNE_RX * r, py = my + 6 + sin * RUNE_RY * r;
			m.x = Math.round( px - m.width * m.scale.x / 2f );
			m.y = Math.round( py - m.height * m.scale.y / 2f );
			m.am = (sin < 0 ? BACK : 1f) * fade * (look.rune == null ? 0.5f : 1f);
		}
	}

	//a rune's colour: light ones the look's light and its element's hot stop by turns; matter ones
	//the look's readable colour and a deeper stop; a curse's VOID
	private int runeColor( int i ){
		if (look.curse) return Element.VOID.ramp.stop( 1 + i % 2 );
		int deep = look.el.ramp.stop( Math.min( look.el.ramp.size() - 1, look.emissive ? 1 : 2 ) );
		if (look.emissive) return i % 2 == 0 ? look.light : deep;
		return i % 2 == 0 ? MotifFx.matterColor( look ) : deep;
	}

	//its motif shed off the ghost at a window's rate, one at a time
	private void motif( float mx, float my ){
		if ((motifLeft -= Game.elapsed) > 0) return;
		motifLeft = Math.max( look.motif.periodAt( level ) / PANEL_RATE, look.motif.life + 0.05f );
		if (motifs == null) motifs = EnchantFx.Beat.at( look, mx, my, MOTIF_MOST );
		MotifFx.mote( motifs, MotifFx.HEAP, mx + FxRandom.Float( -3f, 3f ), my + FxRandom.Float( -2f, 2f ), 0, 0 );
	}

	//the hold begins: sparks spiral in (a curse's: VOID motes leave it, no light) and the family
	//gathers into it
	private void gather( float mx, float my ){
		if (look.curse){
			EnchantFx.Beat b = EnchantFx.Beat.at( look, mx, my, SPARKS_V2 );
			for (int i = 0; i < SPARKS_V2; i++){
				FxParticle p = b.mat( mx, my, FxBudget.P1 );
				if (p == null) break;
				float a = (float)(2 * Math.PI * i / SPARKS_V2) + FxRandom.Float( -0.3f, 0.3f );
				p.look( FxFrames.MOTE_2 ).paint( Element.VOID.ramp.stop( 1 + i % 3 ) ).polar( a, FxRandom.Float( 14f, 20f ) )
						.drag( 2f ).life( FxRandom.Float( 0.45f, 0.6f ) ).alpha( 0.9f, 0, 0.3f ).snap();
			}
		} else {
			FxEmitter e = GameScene.lightEmitter();
			if (e != null){
				e.pos( mx, my );
				e.burst( sparkFactory( look ), SPARKS_V2 );
			}
		}
		EnchantFx.gather( look, mx, my, GATHER_FOR );
	}

	//the sparks of a look: spiralling in, a third each in its element's three hottest stops, spread
	//round the ellipse. One each a look, made once
	private static final Emitter.Factory[] SPARK_FACTORIES = new Emitter.Factory[256];

	private static synchronized Emitter.Factory sparkFactory( final EnchantLook l ){
		int k = l.code & 0xFF;
		if (SPARK_FACTORIES[k] != null) return SPARK_FACTORIES[k];
		final int[] stops = new int[3];
		for (int i = 0; i < 3; i++) stops[i] = l.el.ramp.stop( Math.min( i, l.el.ramp.size() - 1 ) );
		Emitter.Factory f = new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ){
				InfusionParticle.spiralIn( stops[index % 3] ).emit( emitter, index * 12 / SPARKS_V2, x, y );
			}

			@Override
			public boolean lightMode(){
				return true;
			}
		};
		SPARK_FACTORIES[k] = f;
		return f;
	}

	//it binds: the runes are gone; a core flash in the element's glint and a ring opening round
	//it (a curse: a VOID ring closing on it and ink), one light, the ghost flashing, the sting;
	//then the family's finale at half on the owner, four motes of it drawn into him, the item's
	//slot told. A finale that throws its own light (a drain's heartbeat) stands for the bind's
	private void bind( float mx, float my ){
		killMarks();
		float tx = owner.x + owner.width() / 2f, ty = owner.y + owner.height() / 2f;
		int lights0 = FxLight.live();
		EnchantFx.finale( look, tx, ty, FINALE_SCALE );
		boolean lit = FxLight.live() > lights0;
		EnchantFx.Beat b = EnchantFx.Beat.at( look, mx, my, 2 );
		if (look.curse){
			EnchantFx.Flash ring = b.flash( false, true, HexOverlay.CLOSING, 0f, mx, my, Element.VOID.light, 0.8f, 0.25f );
			if (ring != null) ring.envelope( 0.02f, 0.18f );
			FxParticle ink = b.mat( mx, my, FxBudget.P0 );
			if (ink != null) ink.look( FxFrames.INK_CORE ).paint( Element.VOID.ramp.stop( 4 ) ).life( 0.4f ).alpha( 0.85f, 0, 0.25f ).snap();
			FxLight l = lit ? null : Arc.glow( mx, my, 15, Element.VOID.light, EnchantFx.HEX_LIGHT );
			if (l != null) l.life( 0.02f, 0.05f, 0.3f );
		} else {
			FxParticle core = b.lit( mx, my, FxBudget.P0 );
			if (core != null) core.look( FxFrames.CORE_FLASH ).paint( look.el.glint ).life( 0.12f ).alpha( 1f, 0, 0.06f ).snap();
			b.flash( false, true, OPENING, 0f, mx, my, look.light, 0.8f, 0.3f );
			FxLight l = lit ? null : Arc.glow( mx, my, 15, look.light, 0.5f );
			if (l != null) l.life( 0.02f, 0.06f, 0.35f );
		}
		flashLeft = BIND_FLASH;
		GainFx.sting( look, owner.ch );
		if (!look.curse && Arc.onScene()){
			int c = look.emissive ? look.light : MotifFx.matterColor( look );
			for (int i = 0; i < MOTES; i++){
				FxParticle p = Arc.light( mx + (i - 1.5f) * 3f, my, FxBudget.P1 );
				if (p == null) break;
				p.look( FxFrames.MOTE_2 ).paint( c ).vel( (tx - mx) / 0.3f, (ty - my) / 0.3f ).life( 0.3f ).alpha( 1f, 0.05f, 0.1f );
			}
		}
		if (item != null){
			EnchantFx.markGained( item );
			GainFx.held( item );
		}
	}

	private void killMarks(){
		if (marks == null) return;
		for (Image m : marks){
			if (m instanceof ArcImage) m.kill();
			else Fx.dispose( m );
		}
		marks = null;
	}

	//gone, its runes and its own buffer let go of
	private void finish(){
		if (runes != null) for (ArcImage r : runes) r.kill();
		runes = null;
		killMarks();
		Fx.dispose( this );
	}

	/** An item enchanted over its owner's head: the look of its own magic (GainFx.gainLook), or
	 *  today's picture with none. Any thread. */
	public static void show( Char ch, Item item ) {
		show( ch, item, null );
	}

	/** The same with the look it just gained (null: as the two-argument form). Any thread. */
	public static void show( Char ch, Item item, EnchantLook gained ) {
		if (ch == null || item == null) return;
		EnchantLook l = gained != null ? gained : GainFx.gainLook( item );
		ItemSprite.Glowing g = item.glowing();
		int color = g != null ? g.color : -1;
		int image = item.image();
		GainFx.sendGain( ch, l, image, color );
		play( ch.sprite, image, l, color, GainFx.levelOf( item ), item );
	}

	/** The ceremony over a sprite: an item's image, the look it gains (null: today's picture, in
	 *  `color`, -1 for the arcane's), its level, the item (null on a co-op guest). Any thread. */
	public static void play( final CharSprite owner, final int image, final EnchantLook l, final int color, final int level,
			final Item item ){
		if (owner == null) return;
		if (!Fx.onRenderThread()){
			Fx.post( () -> play( owner, image, l, color, level, item ) );
			return;
		}
		if (!owner.visible || owner.parent == null) {
			return;
		}

		Enchanting sprite = new Enchanting( image, l, color, level, item );
		sprite.owner = owner;
		owner.parent.add( sprite );
	}
}
