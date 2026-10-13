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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Flames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.gas.CellOverlay;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FiredampParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.HalomethaneFlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PurpleParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.RainbowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.WebParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.Cite;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.GuideEffect;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GooSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuidePuppet;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.TerrainFeaturesTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIcon;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;
import com.watabou.utils.Reflection;

import java.util.HashMap;

//What a guide cite looks like: the creature's own sprite, the item as it lies on the floor, the
//tile, the trap, the game's particles. Everything is made in a try: what cannot be made (no game
//to build it from, a class gone) is the unknown item's picture, never a crash in the guide.
//Nothing here draws the game's Random: the guide's own motion runs on FxRandom.
public final class CiteArt {

	private CiteArt(){}

	/** The cite's picture, alive where it lives (creatures act on their own timer), cropped to w x h, never
	 *  scaled. Never null for a still cite: what cannot be made is ItemSpriteSheet.SOMETHING. An EFFECT has
	 *  no picture (null): it is poured by an EffectEmitter on its plate instead. */
	public static Image icon( Cite c, int w, int h, float actMin, float actMax ){
		if (!c.still()) return null;
		Image img;
		try {
			img = make( c, w, h, actMin, actMax );
		} catch (Exception e){
			img = null;
		}
		if (img == null) img = new ItemArt( ItemSpriteSheet.SOMETHING );
		if (!(img instanceof LiveCrop) && !(img instanceof SkinnedBlock)) fit( img, w, h );
		return img;
	}

	@SuppressWarnings("unchecked")
	private static Image make( Cite c, int w, int h, float actMin, float actMax ) throws Exception {
		switch (c.kind){
			case MOB:
				CharSprite s = GuidePuppet.make( (Class<? extends Mob>) c.type );
				return s == null ? null : new LiveCrop( s, w, h, actMin, actMax );
			case HERO:
				return HeroSprite.avatar( c.hero, 6 );
			case ITEM:
				return item( (Item) Reflection.newInstanceUnhandled( c.type ) );
			case SPRITE:
				return new ItemArt( c.image );
			case ICON:
				return Icons.get( c.icon );
			case BUFF:
				Buff b = (Buff) Reflection.newInstanceUnhandled( c.type );
				int icon = c.image >= 0 ? c.image : b.icon();
				return icon == BuffIndicator.NONE ? null : new BuffArt( b, icon );
			case TERRAIN:
				return tile( c.asset, c.image );
			case WATER:
				return new Water( c.asset );
			case TRAP:
				return TerrainFeaturesTilemap.getTrapVisual( (Trap) Reflection.newInstanceUnhandled( c.type ) );
			case PLANT:
				return TerrainFeaturesTilemap.getPlantVisual( (Plant) Reflection.newInstanceUnhandled( c.type ) );
			case CUSTOM:
				return c.picture.make();
			default:
				return null;
		}
	}

	//an item whose look changes from run to run (potions, scrolls, rings) never shows this run's:
	//opened mid-run, that would tell which colour is which. Its kind's holder with its sign instead
	static Image item( Item item ){
		if (item.icon == -1) return new ItemArt( item );
		int holder = item instanceof Potion ? ItemSpriteSheet.POTION_HOLDER
				: item instanceof Scroll ? ItemSpriteSheet.SCROLL_HOLDER
				: item instanceof Ring ? ItemSpriteSheet.RING_HOLDER
				: ItemSpriteSheet.SOMETHING;
		return new Unrevealed( holder, item.icon );
	}

	//a larger picture is cut to the box like a creature's frame: centred across, a third from the top
	private static void fit( Image img, int w, int h ){
		RectF f = img.frame();
		if (f == null || img.texture == null || (img.width <= w && img.height <= h)) return;
		float tw = img.texture.width, th = img.texture.height;
		if (img.width > w){
			int cut = (int)((img.width - w) / 2);
			f.left += cut / tw;
			f.right = f.left + w / tw;
		}
		if (img.height > h){
			int cut = (int)((img.height - h) / 3);
			f.top += cut / th;
			f.bottom = f.top + h / th;
		}
		img.frame( f );
	}

	//one film of 16x16 tiles per tileset
	private static final HashMap<String, TextureFilm> tileFilms = new HashMap<>();

	private static synchronized Image tile( String asset, int tile ){
		Image img = new Image( asset );
		TextureFilm film = tileFilms.get( asset );
		if (film == null){
			film = new TextureFilm( img.texture, 16, 16 );
			tileFilms.put( asset, film );
		}
		RectF uv = film.get( tile );
		if (uv == null) return null;
		img.frame( uv );
		return img;
	}

	/** An item's sprite as it lies on the floor: lifted dy whole pixels, its shadow stays where it lay. */
	public static class ItemArt extends ItemSprite {

		public ItemArt( int image ){
			super( image );
		}

		public ItemArt( Item item ){
			super( item );
		}

		/** Shows its floor shadow, lying dy pixels below the sprite's rest (dy -1 is a pixel up). */
		public void bob( int dy ){
			renderShadow = true;
			shadowOffset = 0.5f - dy;
		}
	}

	//the holder of an item's kind with the sign of which one it is at its bottom right, as the
	//catalog draws an unknown potion
	private static class Unrevealed extends ItemArt {

		private final Image sign;

		Unrevealed( int holder, int icon ){
			super( holder );
			sign = new Image( Assets.Sprites.ITEM_ICONS );
			sign.frame( ItemSpriteSheet.Icons.film.get( icon ) );
		}

		@Override
		public void draw() {
			super.draw();
			sign.camera = camera();
			sign.x = x + width - sign.width;
			sign.y = y + height - sign.height;
			sign.rm = rm; sign.gm = gm; sign.bm = bm; sign.am = am;
			sign.ra = ra; sign.ga = ga; sign.ba = ba; sign.aa = aa;
			sign.draw();
		}

		@Override
		public void destroy() {
			super.destroy();
			sign.destroy();
		}
	}

	//a buff's large icon in the buff's own tint, which a colour reset (a card's reveal) puts back
	private static class BuffArt extends BuffIcon {

		private final Buff buff;

		BuffArt( Buff buff, int icon ){
			super( icon, true );
			this.buff = buff;
			tint();
		}

		@Override
		public void resetColor() {
			super.resetColor();
			//Visual's constructor resets the colour before the buff is known
			if (buff != null) tint();
		}

		private void tint(){
			try {
				buff.tintIcon( this );
			} catch (Exception e){
				//a tint that reads its target: untinted
			}
		}
	}

	//a patch of water flowing down a whole pixel every 0.2 s
	private static class Water extends SkinnedBlock {

		private static final float STEP = 0.2f;
		private float flow = FxRandom.Float( STEP );

		Water( String asset ){
			super( 16, 16, asset );
			autoAdjust = true;
			offsetTo( FxRandom.Int( 16 ), FxRandom.Int( 16 ) );
		}

		@Override
		public void update() {
			super.update();
			flow += Game.elapsed;
			while (flow >= STEP){
				flow -= STEP;
				offset( 0, -1 );
			}
		}
	}

	// ------------------------------------------------------------------ effects

	/** A guide effect's particles over a box (pos it like any emitter, then play()): pouring, or for a
	 *  bursting preset a burst now and every 2-3 s. Its particles die with it. */
	public static class EffectEmitter extends Emitter {

		private final GuideEffect effect;
		private float burstIn = -1;

		public EffectEmitter( GuideEffect effect ){
			this.effect = effect;
			autoKill = false;
		}

		public void play(){
			Emitter.Factory f = factory( effect );
			int n = burstSize( effect );
			if (n > 0){
				startDelayed( f, 0, n, 0 );
				burstIn = FxRandom.Float( 2f, 3f );
			} else {
				startDelayed( f, interval( effect ), 0, FxRandom.Float( interval( effect ) ) );
			}
		}

		@Override
		public void update() {
			super.update();
			if (burstIn >= 0 && !on && (burstIn -= Game.elapsed) <= 0) play();
		}

		//where each particle starts, by the effects' own random: the game's is the world's
		@Override
		protected void emit( int index ) {
			factory.emit( this, index, x + FxRandom.Float( width ), y + FxRandom.Float( height ) );
		}

		@Override
		public void destroy() {
			//a kit particle counts itself alive in FxBudget until it is killed
			kill();
			super.destroy();
		}
	}

	private static final Emitter.Factory FIRE = tongues( FlameParticle.class, Element.FIRE );
	private static final Emitter.Factory GREEN_FIRE = tongues( ElmoParticle.class, Element.ELMO );
	private static final Emitter.Factory SOUL_FIRE = tongues( HalomethaneFlameParticle.class, Element.HALOMETHANE );

	//the burning factories' own tongue, made in the emitter that pours it: their factories would take
	//the world's frame budget and hide the flame wherever the hero does not see that spot of the level
	private static Emitter.Factory tongues( final Class<? extends FxParticle> cls, final Element el ){
		return new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				//no sky over a page: the wind last blowing in the game (Fx.windX) would bend it
				Flames.tongue( Flames.make( emitter, cls, x, y ), el, Flames.MIX ).wind( 0 );
			}

			@Override
			public boolean lightMode() {
				return true;
			}
		};
	}

	//Goo's drops as its charge pours them, without their landing: on a level's floor they leave a
	//splat or a ring on the water, and a window has no floor of the level to leave them on
	private static final Emitter.Factory GOO = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((GooSprite.GooParticle) emitter.recycle( GooSprite.GooParticle.class )).drop( x, y ).fate( null );
		}
	};

	//firedamp's haze as the cave shows it, but in no draught and seen wherever it is
	private static final Emitter.Factory FIREDAMP = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			FiredampParticle.haze( Flames.make( emitter, FiredampParticle.class, x, y ) );
		}
	};

	//a bomb's blast, its fire as Explosion.bomb draws it, from the middle of where it bursts: the
	//core's flash, 16-20 tongues thrown up and out, 8-12 embers falling. Explosion's own also lights
	//and rings the level's floor, scorches it, throws its bits and shakes the camera
	private static final Emitter.Factory BLAST = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			x = emitter.x + emitter.width / 2f;
			y = emitter.y + emitter.height / 2f;
			Flames.make( emitter, BlastParticle.class, x, y ).look( FxFrames.CORE_FLASH ).paint( 0xFFF6DC ).light( true )
					.life( 0.12f ).alpha( 1f, 0, 0.07f ).snap().priority( FxBudget.P0 );
			for (int i = FxRandom.IntRange( 16, 20 ); i > 0; i--){
				//through the upper 240 degrees
				float a = FxRandom.Float( (float)Math.toRadians( -210 ), (float)Math.toRadians( 30 ) );
				Flames.tongue( Flames.make( emitter, BlastParticle.class, x, y ), Element.FIRE,
						FxRandom.chance( 0.4f ) ? Flames.L : Flames.M )
						.polar( a, FxRandom.Float( 30f, 70f ) ).drag( 4f ).gravity( -30f ).life( FxRandom.Float( 0.35f, 0.6f ) )
						.wind( 0 );
			}
			for (int i = FxRandom.IntRange( 8, 12 ); i > 0; i--){
				Flames.make( emitter, BlastParticle.class, x, y ).look( FxRandom.chance( 0.7f ) ? FxFrames.EMBER_1 : FxFrames.EMBER_2 )
						.ramp( Element.FIRE.ember, true ).life( FxRandom.Float( 0.2f, 1.2f ) ).alpha( 1f, 0, 0.3f )
						.polar( FxRandom.Float( (float)Math.toRadians( -150 ), (float)Math.toRadians( -30 ) ), FxRandom.Float( 30f, 60f ) )
						.gravity( 90f ).drag( 1f );
			}
		}

		@Override
		public boolean lightMode() {
			return true;
		}
	};

	//a web of silk on the middle of its cell as WebParticle strings it there, coming in, held, fading
	private static final Emitter.Factory WEB = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			float life = WebParticle.IN + 2f + WebParticle.FADE;
			Flames.make( emitter, FxParticle.class, emitter.x + emitter.width / 2f, emitter.y + emitter.height / 2f )
					.look( FxFrames.WEB[FxRandom.Int( FxFrames.WEB.length )] ).paint( CellOverlay.SILK ).snap()
					.life( life ).alpha( WebParticle.ALPHA, WebParticle.IN, WebParticle.FADE );
		}
	};

	static Emitter.Factory factory( GuideEffect e ){
		switch (e){
			case FIRE:          return FIRE;
			case GREEN_FIRE:    return GREEN_FIRE;
			case SOUL_FIRE:     return SOUL_FIRE;
			case TOXIC_GAS:     return Speck.factory( Speck.TOXIC );
			case PARALYTIC_GAS: return Speck.factory( Speck.PARALYSIS );
			case CONFUSION_GAS: return Speck.factory( Speck.CONFUSION );
			case CORROSIVE_GAS: return Speck.factory( Speck.CORROSION );
			case STENCH:        return Speck.factory( Speck.STENCH );
			case SMOKE:         return Speck.factory( Speck.SMOKE );
			case STORM_CLOUD:   return Speck.factory( Speck.STORM );
			case BLIZZARD:      return Speck.factory( Speck.BLIZZARD );
			case INFERNO:       return Speck.factory( Speck.INFERNO );
			case STEAM:         return Speck.factory( Speck.STEAM );
			case MIASMA:        return Speck.factory( Speck.MIASMA );
			case POP_GAS:       return Speck.factory( Speck.POPGAS );
			case SLOWING_GAS:   return Speck.factory( Speck.SLOW );
			case DEPRESSANT:    return Speck.factory( Speck.DEPRESSANT );
			case DIRT_CLOUD:    return Speck.factory( Speck.DIRTCLOUD );
			case CORRUPTION:    return Speck.factory( Speck.CLOUDOFCORR );
			case LIGHT:         return Speck.factory( Speck.LIGHT );
			case BUBBLES:       return Speck.factory( Speck.BUBBLE );
			case NOTES:         return Speck.factory( Speck.NOTE );
			case HEARTS:        return Speck.factory( Speck.HEART );
			case GOO:           return GOO;
			case FIREDAMP:      return FIREDAMP;
			case HEALING:       return Speck.factory( Speck.HEALING );
			case COINS:         return Speck.factory( Speck.COIN );
			case ROCKS:         return Speck.factory( Speck.ROCK );
			case BLOOD:         return BloodParticle.BURST;
			case ARCANE:        return PurpleParticle.BURST;
			case RAINBOW:       return RainbowParticle.BURST;
			case BONES:         return Speck.factory( Speck.BONE );
			case BLAST:         return BLAST;
			case WEB:           return WEB;
			default:            throw new IllegalArgumentException( "no factory for " + e );
		}
	}

	//how many a burst throws, 0 for a preset that pours
	static int burstSize( GuideEffect e ){
		switch (e){
			case HEALING: return 3;
			case COINS:   return 5;
			case ROCKS:   return 4;
			case BLOOD:   return 6;
			case ARCANE:  case RAINBOW: return 8;
			case BONES:   return 6;
			case BLAST:   case WEB: return 1;
			default:      return 0;
		}
	}

	//whether its particles start low and rise (flames, lights, bubbles, notes, hearts, steam, healing)
	static boolean rises( GuideEffect e ){
		switch (e){
			case FIRE: case GREEN_FIRE: case SOUL_FIRE: case LIGHT: case BUBBLES: case NOTES:
			case HEARTS: case STEAM: case HEALING:
				return true;
			default:
				return false;
		}
	}

	//seconds between particles of a pouring preset: about as many alive as fill a plate
	static float interval( GuideEffect e ){
		switch (e){
			case FIRE: case GREEN_FIRE: case SOUL_FIRE: return 0.06f;
			case DEPRESSANT: case DIRT_CLOUD:           return 0.4f;
			case NOTES: case HEARTS:                    return 0.5f;
			case LIGHT: case BUBBLES:                   return 0.3f;
			case GOO:                                   return 0.15f;
			case FIREDAMP:                              return 0.4f;
			default:                                    return 0.25f;
		}
	}
}
