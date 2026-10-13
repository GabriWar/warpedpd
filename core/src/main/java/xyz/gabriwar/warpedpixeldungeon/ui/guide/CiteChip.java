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

import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideLayout;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.Cite;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.GuideEffect;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuidePuppet;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Reflection;

//One thing a guide page names, under the paragraph that names it, like a figure in a field guide:
//a plate in the chapter's colour with the thing alive on it, and its name beside. A tap makes a
//creature strike (anything else flashes its plate) and tells what the game knows of it.
//
//Every motion is a whole pixel at most and out of step with its neighbours: items float over
//their shadow, interface icons swell, buff icons pulse, traps arm, plants sway, water flows,
//creatures idle and strike, effects pour.
public class CiteChip extends Component {

	public static final int PLATE = 20;
	//between the plate and its caption
	public static final int GAP = 3;
	private static final float APPEAR = 0.25f;

	private final Cite cite;
	private final Plate plate;
	private final RenderedTextBlock caption;

	//when it starts to appear (Game.timeTotal): a chip scrolled into view late is already there
	private float appearAt = -1;
	private boolean appeared = true;

	public CiteChip( Cite c, int accent ){
		super();
		cite = c;
		plate = new Plate( c, accent );
		add( plate );
		caption = PixelScene.renderTextBlock( c.caption(), 6 );
		caption.setHightlighting( false );
		caption.hardlight( lighter( accent, 0.55f ) );
		add( caption );
		measure();
	}

	/** Its caption wraps to fit `available` pixels for the whole chip. */
	public void maxWidth( int available ){
		caption.maxWidth( Math.max( 1, available - PLATE - GAP ) );
		measure();
	}

	private void measure(){
		width = PLATE + GAP + (int)Math.ceil( caption.width() );
		height = Math.max( PLATE, (int)Math.ceil( caption.height() ) );
		layout();
	}

	/** Fades in after `delay` s, rising 2 px into place. */
	public void appear( float delay ){
		appearAt = Game.timeTotal + delay;
		appeared = false;
		place( 2, 0 );
	}

	@Override
	protected void layout() {
		//while it appears, update() places it
		place( appeared ? 0 : 2, appeared ? 1 : 0 );
	}

	//everything at its place, `rise` pixels low, at alpha a
	private void place( int rise, float a ){
		int px = (int)x, py = (int)y + rise;
		int ch = (int)Math.ceil( caption.height() );
		plate.setPos( px, py + ((int)height - PLATE) / 2 );
		caption.setPos( px + PLATE + GAP, py + ((int)height - ch) / 2 );
		plate.alpha( a );
		caption.alpha( a );
	}

	@Override
	public void update() {
		super.update();
		if (appeared) return;
		float k = (Game.timeTotal - appearAt) / APPEAR;
		if (k < 0){
			place( 2, 0 );
			return;
		}
		float e = GuideLayout.ease( k );
		place( Math.round( 2 * (1 - e) ), e );
		plate.start();
		if (k >= 1) appeared = true;
	}

	/** A tap: the creature strikes, or the plate flashes; then what the game says of it, if anything. */
	public void tap(){
		plate.tap();
		String desc = describe( cite );
		if (desc == null || desc.isEmpty()) return;
		Image icon = CiteArt.icon( cite, PLATE, PLATE, 6, 10 );
		WarpedPixelDungeon.scene().addToFront( new WndTitledMessage( icon, Messages.titleCase( cite.caption() ), desc ) );
	}

	//what the game knows of the thing, or null: a potion's or scroll's text is left unread, as it
	//would name this run's look
	private static String describe( Cite c ){
		try {
			switch (c.kind){
				case MOB:
					//one the guide must never make: its plain text, as Mob.description gives it
					if (GuidePuppet.unmade( c.type ) != null) return Messages.get( c.type, "desc" );
					return ((Mob) Reflection.newInstanceUnhandled( c.type )).description();
				case BUFF:
					return ((Buff) Reflection.newInstanceUnhandled( c.type )).desc();
				case TRAP:
					return ((Trap) Reflection.newInstanceUnhandled( c.type )).desc();
				case PLANT:
					return ((Plant) Reflection.newInstanceUnhandled( c.type )).desc();
				case ITEM:
					Item item = (Item) Reflection.newInstanceUnhandled( c.type );
					return item.icon == -1 ? item.info() : null;
				default:
					return null;
			}
		} catch (Exception e){
			//no game state to describe it from (the title screen)
			return null;
		}
	}

	static int lighter( int c, float f ){
		int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
		r += (int)((255 - r) * f);
		g += (int)((255 - g) * f);
		b += (int)((255 - b) * f);
		return (r << 16) | (g << 8) | b;
	}

	static int darker( int c, float f ){
		int r = (int)(((c >> 16) & 0xFF) * (1 - f)), g = (int)(((c >> 8) & 0xFF) * (1 - f)), b = (int)((c & 0xFF) * (1 - f));
		return (r << 16) | (g << 8) | b;
	}

	/** The chip's plate and the thing alive on it, without its caption (which needs the platform's fonts:
	 *  GuideCitesTest builds plates headlessly). */
	public static class Plate extends Component {

		private static final int INNER = PLATE - 2;
		private static final float TAU = (float)(2 * Math.PI);

		private final Cite.Kind kind;
		private final int frameColor;
		private final ColorBlock[] edges = new ColorBlock[4];
		private final ColorBlock fill;
		private final Image art;
		private final CiteArt.EffectEmitter effect;
		private final GuideEffect effectKind;
		//where the art rests, and its own colour (motions brighten from it)
		private int artX, artY;
		private final float baseR, baseG, baseB;

		private float alpha = 1;
		private float time;
		private final float phase = FxRandom.angle();
		//the next swell, arming, sway; how far into one; the frame's flash
		private float nextIn, inFor = -1, flash;
		private float shown = 1;
		private boolean started;

		public Plate( Cite c, int accent ){
			super();
			kind = c.kind;
			effectKind = c.effect;
			frameColor = darker( accent, 0.35f );
			for (int i = 0; i < edges.length; i++){
				edges[i] = new ColorBlock( 1, 1, 0xFFFFFFFF );
				edges[i].hardlight( frameColor );
				add( edges[i] );
			}
			fill = new ColorBlock( INNER, INNER, 0xFF0E0C0A );
			add( fill );
			if (c.still()){
				art = CiteArt.icon( c, INNER, INNER, 3.5f, 7f );
				add( art );
				effect = null;
				baseR = art.rm;
				baseG = art.gm;
				baseB = art.bm;
			} else {
				art = null;
				effect = new CiteArt.EffectEmitter( c.effect );
				add( effect );
				baseR = baseG = baseB = 1;
			}
			nextIn = kind == Cite.Kind.TRAP ? FxRandom.Float( 4f, 6f )
					: kind == Cite.Kind.PLANT ? FxRandom.Float( 3f, 6f ) : FxRandom.Float( 3f, 5f );
			width = height = PLATE;
		}

		@Override
		protected void layout() {
			int px = (int)x, py = (int)y;
			edges[0].x = px;             edges[0].y = py;             edges[0].size( PLATE, 1 );
			edges[1].x = px;             edges[1].y = py + PLATE - 1; edges[1].size( PLATE, 1 );
			edges[2].x = px;             edges[2].y = py + 1;         edges[2].size( 1, PLATE - 2 );
			edges[3].x = px + PLATE - 1; edges[3].y = py + 1;         edges[3].size( 1, PLATE - 2 );
			fill.x = px + 1;
			fill.y = py + 1;
			if (art != null){
				artX = px + 1 + (INNER - (int)Math.ceil( art.width() )) / 2;
				artY = py + 1 + (INNER - (int)Math.ceil( art.height() )) / 2;
				art.x = artX;
				art.y = artY;
				if (art instanceof CiteArt.ItemArt) art.y = artY + bob();
			} else if (CiteArt.rises( effectKind )){
				//flames, lights and bubbles start low and rise through the plate
				effect.pos( px + 4, py + PLATE - 8, INNER - 6, 4 );
			} else {
				effect.pos( px + 5, py + 5, INNER - 8, INNER - 8 );
			}
		}

		/** Fades the plate and what is on it. */
		public void alpha( float a ){
			alpha = a;
			for (ColorBlock e : edges) e.alpha( a );
			fill.alpha( 0.85f * a );
			if (art != null) art.alpha( kind == Cite.Kind.BUFF ? a * pulse() : a );
		}

		/** Starts what pours on it (once it shows). */
		public void start(){
			if (started) return;
			started = true;
			if (effect != null) effect.play();
		}

		/** The creature strikes; anything else flashes the plate. */
		public void tap(){
			if (art instanceof LiveCrop){
				((LiveCrop) art).act();
			} else {
				flash = 0.15f;
			}
		}

		@Override
		public void update() {
			super.update();
			float dt = Game.elapsed;
			time += dt;
			if (alpha >= 1) start();

			if (flash > 0){
				flash -= dt;
				int c = flash > 0 ? lighter( frameColor, 0.45f ) : frameColor;
				for (ColorBlock e : edges) e.hardlight( c );
			}
			if (art == null) return;

			switch (kind){
				case ITEM: case SPRITE:
					int dy = bob();
					art.y = artY + dy;
					if (art instanceof CiteArt.ItemArt) ((CiteArt.ItemArt) art).bob( dy );
					break;
				case ICON: case HERO: case CUSTOM:
					//a slow swell of light every 3-5 s
					bright( timed( 0.6f, 3f, 5f ) ? 1f + 0.3f * (float)Math.sin( Math.PI * inFor / 0.6f ) : 1f );
					break;
				case BUFF:
					art.alpha( alpha * pulse() );
					break;
				case TRAP:
					//arming: a flash every 4-6 s
					bright( timed( 0.15f, 4f, 6f ) ? 1.6f : 1f );
					break;
				case PLANT:
					//a pixel's sway in the draught every 3-6 s
					art.x = artX + (timed( 0.3f, 3f, 6f ) ? 1 : 0);
					break;
				default:
					break;
			}
		}

		//whether a motion of `length` s, coming every min..max s, is under way (inFor: how far in)
		private boolean timed( float length, float min, float max ){
			if (inFor >= 0){
				inFor += Game.elapsed;
				if (inFor < length) return true;
				inFor = -1;
				nextIn = FxRandom.Float( min, max );
				return false;
			}
			if ((nextIn -= Game.elapsed) <= 0){
				inFor = 0;
				return true;
			}
			return false;
		}

		private void bright( float b ){
			//written only when it changes: a custom picture keeps any colour of its own between swells
			if (b == shown) return;
			shown = b;
			art.rm = baseR * b;
			art.gm = baseG * b;
			art.bm = baseB * b;
		}

		//an item's float: -1, 0 or 1 px on a 2.4 s wave
		private int bob(){
			return Math.round( (float)Math.sin( time * TAU / 2.4f + phase ) );
		}

		//a buff's pulse on a 1.8 s wave
		private float pulse(){
			return 0.65f + 0.35f * (0.5f + 0.5f * (float)Math.sin( time * TAU / 1.8f + phase ));
		}
	}
}
