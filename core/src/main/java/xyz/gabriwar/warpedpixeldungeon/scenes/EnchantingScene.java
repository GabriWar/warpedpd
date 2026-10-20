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

package xyz.gabriwar.warpedpixeldungeon.scenes;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDAction;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.Family;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PedestalFx;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.journal.Journal;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.EnchantingStationSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.ExitButton;
import xyz.gabriwar.warpedpixeldungeon.ui.IconButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.ItemSlot;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.windows.IconTitle;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndEnergizeItem;
import xyz.gabriwar.warpedpixeldungeon.windows.WndGuideNode;
import xyz.gabriwar.warpedpixeldungeon.windows.WndInfoItem;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.GameAction;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.NoosaScriptNoLighting;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Point;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;
import com.watabou.utils.Random;

import java.io.IOException;
import java.util.ArrayList;

/**
 * The enchanting pedestal's fullscreen scene: the piece lies on the pedestal in a ring of
 * drifting glyphs, and its two sockets lie beside it as cards, each an enchantment (or glyph)
 * named in its own colour with its level, what it does and what can be done to it. A new
 * weave is revealed for energy: three are shown as cards, and the one taken costs nothing more.
 * The pedestal does not remember them: closed unchosen, they are gone.
 */
public class EnchantingScene extends PixelScene {

	//the weapon or armor being worked on. a reference into the hero's
	// belongings, never detached - changes apply to it wherever it lives
	private static Item target = null;

	/** what seeing three weaves costs, to fill a socket or to replace what is in it; taking one is
	 *  free, and three left unchosen are lost */
	public static final int REVEAL_COST = 20;
	public static final int DISENCHANT_ENERGY = 20;

	public static int empowerCost( int level ){
		//10/20/30/40/50 energy for levels 1..5
		return 10 * (level + 1);
	}

	private static final int SOCKETS = 2;
	private static final int GAP = 4;
	private static final int TOP_BAR = 22;
	private static final int SLOT_SIZE = 32;
	private static final int HALO_R = 30;
	//a cursed piece's red, the pedestal's violet, and the faded text of an empty socket
	private static final int CURSE_RED = 0xCC3344, VIOLET = 0x9966FF, FADED = 0x8A7FA0;
	//the vestige's echo ring under the piece, every so often; the three weaves' gems igniting one
	//after another; a rerolled gem's shuffle; a taken weave's sting; an unwoven ring's refilling
	private static final float ECHO_EVERY = 3f, IGNITE_GAP = 0.12f, GEM_CYCLE = 0.3f, STING_VOL = 0.3f, REFILL = 0.5f;

	private Emitter bubbleEmitter;
	private Emitter lowerBubbles;
	private Emitter sparkEmitter;
	private SkinnedBlock water;

	private RenderedTextBlock energyText;
	private Image energyIcon;
	private IconButton energyAdd;
	private NinePatch energyBg;

	private Halo halo;
	private WeaponButton weaponSlot;
	private RenderedTextBlock itemName;
	private RenderedTextBlock itemSub;
	//the piece's vestige, read only, under its name
	private RenderedTextBlock itemVestige;
	private boolean vestigeShown;
	private float echoIn;
	//the pedestal's own effects (over the cards), and the socket cards as last built
	private PedestalFx fx;
	private final Card[] socketCards = new Card[SOCKETS];
	//comets of weaves still flying to the pedestal
	private int comets;
	private final ArrayList<Component> stateElems = new ArrayList<>();

	//the pedestal's centre, and the box the socket cards stack in
	private float px, py;
	private float cardsX, cardsY, cardsW, cardsH;
	private float underItemY, nameW;
	//a wide screen: the pedestal on the left, the cards beside it. A tall one stacks them
	private boolean wide;
	private float contentBottom;

	{
		inGameScene = true;
	}

	/** Opens the pedestal with a weapon or an armour already on it (a debug scene's). Any thread. */
	public static void open( Item item ){
		Game.runOnRenderThread( () -> {
			target = item;
			Game.switchScene( EnchantingScene.class );
		} );
	}

	@Override
	public void create() {
		super.create();

		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();

		water = new SkinnedBlock( w, h, Dungeon.level.waterTex() ){
			@Override
			protected NoosaScript script() {
				return NoosaScriptNoLighting.get();
			}

			@Override
			public void draw() {
				//water has no alpha component, this improves performance
				Blending.disable();
				super.draw();
				Blending.enable();
			}
		};
		water.autoAdjust = true;
		add( water );

		//a violet veil over the water, so the pedestal feels arcane rather than damp
		add( new ColorBlock( w, h, 0x66341a66 ) );

		Image im = new Image( TextureCache.createGradient( 0x662a1144, 0x88221144, 0xAA190b33, 0xCC120722, 0xFF0a0414 ) );
		im.angle = 90;
		im.x = w;
		im.scale.x = h / 5f;
		im.scale.y = w;
		add( im );

		float left = insets.left, top = insets.top;
		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;

		lowerBubbles = new Emitter();
		lowerBubbles.pos( 0, 0, Camera.main.width, Camera.main.height );
		lowerBubbles.autoKill = false;
		add( lowerBubbles );

		//the top bar: energy on the left, the name in the middle, the guide and the way out on the right
		ExitButton btnExit = new ExitButton(){
			@Override
			protected void onClick() {
				Game.switchScene( GameScene.class );
			}
		};
		btnExit.setPos( left + w - btnExit.width(), top );
		add( btnExit );

		IconButton btnGuide = new IconButton( new ItemSprite( ItemSpriteSheet.GUIDE_PAGE ) ){
			@Override
			protected void onClick() {
				WndGuideNode page = WndGuideNode.forPage( GuideGraph.BASICS_ENCHANTING, null, false );
				if (page != null) EnchantingScene.this.addToFront( page );
			}

			@Override
			public GameAction keyAction() {
				return WPDAction.JOURNAL;
			}

			@Override
			protected String hoverText() {
				return Messages.titleCase( GuideGraph.titleForKey( GuideGraph.BASICS_ENCHANTING ) );
			}
		};
		btnGuide.setRect( btnExit.left() - 18, top + (btnExit.height() - 16) / 2f, 16, 16 );
		add( btnGuide );

		IconTitle title = new IconTitle( new ItemSprite( ItemSpriteSheet.STYLUS ),
				Messages.titleCase( Messages.get( EnchantingStation.class, "name" ) ) );
		title.setSize( 200, 0 );
		title.setPos( left + (w - title.reqWidth()) / 2f, top + (TOP_BAR - title.height()) / 2f );
		align( title );
		add( title );

		energyBg = Chrome.get( Chrome.Type.TOAST_TR );
		add( energyBg );
		energyIcon = new ItemSprite( ItemSpriteSheet.ENERGY );
		add( energyIcon );
		energyText = PixelScene.renderTextBlock( 9 );
		energyText.hardlight( 0x44CCFF );
		add( energyText );
		energyAdd = new IconButton( Icons.get( Icons.PLUS ) ){
			@Override
			protected void onClick() {
				WndEnergizeItem.openItemSelector();
			}

			@Override
			public GameAction keyAction() {
				return WPDAction.TAG_ACTION;
			}

			@Override
			protected String hoverText() {
				return Messages.get( AlchemyScene.class, "energize" );
			}
		};
		add( energyAdd );
		//the pedestal on the left with the cards beside it on a wide screen, above them on a tall one;
		//energy sits in the top bar's corner when there is room for it beside the name, else at the bottom
		float contentTop = top + TOP_BAR + 2;
		float contentH = h - TOP_BAR - 4;
		wide = w >= 300;
		if (wide){
			energyBg.x = left + 2;
			energyBg.y = top + 2;
		} else {
			contentH -= 22;
			energyBg.y = top + h - 20;
		}
		contentBottom = contentTop + contentH;
		if (wide){
			float pedW = Math.min( 170, w * 0.4f );
			px = left + pedW / 2f + 6;
			py = contentTop + contentH * 0.42f;
			cardsW = Math.min( 240, w - pedW - 18 );
			cardsX = left + pedW + 12 + (w - pedW - 18 - cardsW) / 2f;
			cardsY = contentTop + 2;
			cardsH = contentH - 4;
			nameW = pedW - 8;
		} else {
			px = left + w / 2f;
			py = contentTop + HALO_R + 6;
			cardsW = Math.min( 240, w - 12 );
			cardsX = left + (w - cardsW) / 2f;
			cardsY = py + HALO_R + 32;
			cardsH = contentBottom - cardsY;
			nameW = w - 8;
		}

		halo = new Halo();
		add( halo );
		halo.center( px, py );

		NinePatch slotBg = Chrome.get( Chrome.Type.TOAST_TR );
		slotBg.size( SLOT_SIZE + 8, SLOT_SIZE + 8 );
		slotBg.x = px - slotBg.width() / 2f;
		slotBg.y = py - slotBg.height() / 2f;
		add( slotBg );

		weaponSlot = new WeaponButton();
		weaponSlot.setRect( px - SLOT_SIZE / 2f, py - SLOT_SIZE / 2f, SLOT_SIZE, SLOT_SIZE );
		add( weaponSlot );

		itemName = PixelScene.renderTextBlock( 9 );
		add( itemName );
		itemSub = PixelScene.renderTextBlock( 6 );
		add( itemSub );
		itemVestige = PixelScene.renderTextBlock( 6 );
		itemVestige.visible = false;
		add( itemVestige );
		underItemY = py + HALO_R + 4;

		bubbleEmitter = new Emitter();
		bubbleEmitter.pos( 0, 0, Camera.main.width, Camera.main.height );
		bubbleEmitter.autoKill = false;
		add( bubbleEmitter );

		sparkEmitter = new Emitter();
		sparkEmitter.pos( px - SLOT_SIZE / 2f, py - SLOT_SIZE / 2f, SLOT_SIZE, SLOT_SIZE );
		sparkEmitter.autoKill = false;
		add( sparkEmitter );

		fx = new PedestalFx();
		add( fx );

		updateState();

		fadeIn();
	}

	private float glyphTimer = 0;

	@Override
	public void update() {
		super.update();
		water.offset( 0, -5 * Game.elapsed );

		//ambient glyphs: a slow drizzle of random particles from across the game
		glyphTimer -= Game.elapsed;
		if (glyphTimer <= 0){
			glyphTimer = 0.25f;
			lowerBubbles.burst( Random.element( EnchantingStationSprite.GLYPH_FX ), 1 );
		}

		//the vestige's sign: its doubled ring under the piece, once in a while
		if (vestigeShown && (echoIn -= Game.elapsed) <= 0){
			echoIn = ECHO_EVERY;
			fx.echoRing( px, py + SLOT_SIZE / 2f + 2, 2f );
		}
	}

	@Override
	protected void onBackPressed() {
		Game.switchScene( GameScene.class );
	}

	// ------------------------------------------------------------------ state

	/** the piece on the pedestal as the cards read it, or null with none (or none of ours) */
	private static Piece<?> piece(){
		if (target instanceof Weapon) return new WeaponPiece( (Weapon) target );
		if (target instanceof Armor)  return new ArmorPiece( (Armor) target );
		return null;
	}

	//rebuilds everything that depends on the piece and the energy
	private void updateState(){

		for (Component c : stateElems) c.killAndErase();
		stateElems.clear();

		weaponSlot.item( target );
		updateEnergyText();
		for (int s = 0; s < SOCKETS; s++) socketCards[s] = null;

		Piece<?> p = piece();
		if (p == null) halo.colors( null, null, null );
		else halo.colors( p.colors(), p.families(), p.levels() );
		vestige();

		if (p == null){
			itemName.text( Messages.get( EnchantingScene.class, "text" ) );
			itemName.hardlight( 0xFFFFFF );
			itemSub.text( Messages.get( EnchantingScene.class, "tap_slot" ) );
			itemSub.hardlight( FADED );
			placeUnderItem();
			return;
		}

		itemName.text( Messages.titleCase( target.title() ) );
		itemName.hardlight( target.quality != null ? target.quality.titleColor() : 0xFFFFFF );
		itemSub.text( Messages.get( EnchantingScene.class, p.isWeapon() ? "sub_weapon" : "sub_armor", p.filled(), SOCKETS ) );
		itemSub.hardlight( FADED );
		placeUnderItem();

		float y = itemSub.bottom() + 4;
		float under = itemSub.bottom();
		if (p.hasGood() && !p.cursed()){
			final Piece<?> fp = p;
			RedButton unweave = new RedButton( Messages.get( EnchantingScene.class, "unweave", DISENCHANT_ENERGY ), 6 ){
				@Override
				protected void onClick() {
					confirmUnweave( fp );
				}
			};
			unweave.setSize( unweave.reqWidth() + 6, 14 );
			unweave.setPos( px - unweave.width() / 2f, y );
			align( unweave );
			addState( unweave );
			under = unweave.bottom();
		}
		//stacked under the pedestal, the cards start below whatever stands under it
		if (!wide){
			cardsY = under + 6;
			cardsH = contentBottom - cardsY;
		}

		if (p.cursed()){
			Card c = new Card( CURSE_RED );
			c.title( Messages.get( EnchantingScene.class, "cursed_title" ), CURSE_RED );
			c.text( Messages.get( EnchantingStation.class, "cursed" ) );
			c.setRect( cardsX, cardsY, cardsW, 0 );
			addState( c );
			centerCards( c.height() );
			return;
		}

		Card[] cards = new Card[SOCKETS];
		float total = 0;
		for (int s = 0; s < SOCKETS; s++){
			cards[s] = socketCard( p, s );
			cards[s].setRect( cardsX, 0, cardsW, 0 );
			total += cards[s].height() + (s > 0 ? GAP : 0);
		}
		float cy = cardsY + Math.max( 0, (cardsH - total) / 2f );
		for (Card c : cards){
			c.setPos( cardsX, cy );
			align( c );
			addState( c );
			cy = c.bottom() + GAP;
		}
		for (int s = 0; s < SOCKETS; s++) socketCards[s] = cards[s];
		//the pedestal's effects over the cards
		bringToFront( fx );
	}

	//the piece's vestige, once it is identified: a line under its name, three faint motes on the
	//ring, its echo ring now and then
	private void vestige(){
		Bundlable v = target != null && target.isIdentified() ? Quality.vestigeMagic( target ) : null;
		vestigeShown = v != null;
		halo.vestige( vestigeShown );
		if (v == null){
			itemVestige.visible = false;
			return;
		}
		itemVestige.text( Messages.get( EnchantingScene.class, "vestige", Quality.vestigeName( v ),
				Math.round( Quality.value( target, RarityLine.VESTIGE ) * 100 ) ) );
		itemVestige.hardlight( EnchantLook.GHOST );
		itemVestige.alpha( 0.5f );
		itemVestige.visible = true;
	}

	private void centerCards( float height ){
		Component last = stateElems.get( stateElems.size() - 1 );
		last.setPos( cardsX, cardsY + Math.max( 0, (cardsH - height) / 2f ) );
		align( last );
	}

	private void addState( Component c ){
		add( c );
		stateElems.add( c );
	}

	private void placeUnderItem(){
		itemName.maxWidth( (int)nameW );
		itemName.setPos( px - itemName.width() / 2f, underItemY );
		align( itemName );
		float under = itemName.bottom();
		if (itemVestige.visible){
			itemVestige.maxWidth( (int)nameW );
			itemVestige.setPos( px - itemVestige.width() / 2f, under + 1 );
			align( itemVestige );
			under = itemVestige.bottom();
		}
		itemSub.maxWidth( (int)nameW );
		itemSub.setPos( px - itemSub.width() / 2f, under + 1 );
		align( itemSub );
	}

	//one socket as a card: what is woven into it, or how to fill it
	private <E> Card socketCard( final Piece<E> p, final int slot ){
		final E e = p.at( slot );

		if (e == null){
			//the second socket opens once the first holds a weave
			boolean locked = slot > 0 && p.at( 0 ) == null;
			Card c = new Card( locked ? 0x3A2E4A : VIOLET );
			c.gem( locked ? 0x3A2E4A : 0x5A4A7A, true );
			c.title( Messages.get( EnchantingScene.class, locked ? "second_socket" : "empty_socket" ), locked ? FADED : 0xD8CCF0 );
			c.text( Messages.get( EnchantingScene.class, locked ? "second_desc"
					: (p.isWeapon() ? "empty_desc_weapon" : "empty_desc_armor"), REVEAL_COST ) );
			if (!locked){
				RedButton reveal = new RedButton( Messages.get( EnchantingScene.class, "reveal", REVEAL_COST ), 6 ){
					@Override
					protected void onClick() {
						reveal( p, -1 );
					}
				};
				reveal.enable( Dungeon.energy >= REVEAL_COST );
				c.buttons( reveal );
			}
			return c;
		}

		final int color = p.color( e );
		Card c = new Card( color );
		c.gem( color, false );
		c.title( p.name( e ), color );
		c.pips( p.level( e ), color );
		c.text( p.gist( e ) );

		final int level = p.level( e );
		RedButton empower;
		if (level >= p.maxLevel()){
			empower = new RedButton( Messages.get( EnchantingScene.class, "maxed" ), 6 );
			empower.enable( false );
		} else {
			final int cost = empowerCost( level );
			empower = new RedButton( Messages.get( EnchantingScene.class, "empower", cost ), 6 ){
				@Override
				protected void onClick() {
					//recheck on activation: a queued click may belong to an older card
					if (!p.usable() || p.at( slot ) != e || p.level( e ) != level
							|| level >= p.maxLevel() || Dungeon.energy < cost){
						updateState();
						return;
					}
					Dungeon.energy -= cost;
					p.level( e, level + 1 );
					onWeave();
					empowered( slot, level, color );
				}
			};
			empower.enable( Dungeon.energy >= cost );
			empower.textColor( color );
		}

		RedButton reroll = new RedButton( Messages.get( EnchantingScene.class, "reroll", REVEAL_COST ), 6 ){
			@Override
			protected void onClick() {
				reveal( p, slot );
			}
		};
		reroll.enable( Dungeon.energy >= REVEAL_COST );

		IconButton info = new IconButton( Icons.get( Icons.INFO ) ){
			@Override
			protected void onClick() {
				EnchantingScene.this.addToFront( new WndTitledMessage( new ItemSprite( target ), p.name( e ), p.desc( e ) ) );
			}
		};
		c.buttons( empower, reroll, info );
		return c;
	}

	// ------------------------------------------------------------------ weaving

	//a weave into the free socket (slot -1) or in place of the one in a socket: three are
	//revealed for REVEAL_COST and live only in the window that shows them
	private <E> void reveal( Piece<E> p, int slot ){
		if (!p.usable() || Dungeon.energy < REVEAL_COST || (slot >= 0 && p.at( slot ) == null)){
			updateState();
			return;
		}
		Dungeon.energy -= REVEAL_COST;
		E[] opts = p.roll();
		saveNeeded = true;
		bubbleEmitter.start( Speck.factory( Speck.BUBBLE ), 0.01f, 60 );
		sparkEmitter.burst( SparkParticle.FACTORY, 12 );
		Sample.INSTANCE.play( Assets.Sounds.READ, 0.6f );
		updateState();
		addToFront( new WndWeaves<>( p, slot, opts ) );
	}

	//one of the three taken: free, the reveal paid for it. The piece may have changed under the
	//window (a weave elsewhere): three that no longer fit it are refused
	private <E> void take( Piece<E> p, int slot, E[] opts, int index, PointF from ){
		if (!p.usable() || !p.valid( opts )){
			updateState();
			return;
		}
		E chosen = opts[index];
		if (slot < 0){
			if (p.at( 0 ) != null && p.at( 1 ) != null){
				updateState();
				return;
			}
			p.weave( chosen );
		} else {
			E old = p.at( slot );
			if (old == null){
				updateState();
				return;
			}
			p.level( chosen, p.level( old ) );
			p.replace( slot, chosen );
		}
		EnchantLook l = p.look( chosen );
		if (l != null) landing( l, from, p.slotOf( chosen ), slot >= 0 );
		onWeave( l == null );
	}

	//a weave taken: its gem bursts, a comet of it flies to the pedestal; as it lands the ring takes
	//its colour and motion and flares, its sting plays, the piece's slot runs its aura faster and a
	//rerolled socket's gem shuffles through the prism before it settles
	private void landing( final EnchantLook l, PointF from, final int socket, final boolean rerolled ){
		float fx0 = from != null ? from.x : px, fy0 = from != null ? from.y : py - HALO_R - 12;
		fx.burst( fx0, fy0, l, PedestalFx.TAKEN, 30f );
		comets++;
		halo.hold( true );
		fx.comet( fx0, fy0, px, py, l, () -> {
			comets = Math.max( 0, comets - 1 );
			if (comets == 0) halo.hold( false );
			halo.flare();
			if (l.sound != null) Sample.INSTANCE.play( l.sound, STING_VOL, l.pitch );
			if (target != null){
				EnchantFx.markGained( target );
				weaponSlot.item( target );
			}
			if (rerolled && socket >= 0 && socket < SOCKETS && socketCards[socket] != null){
				socketCards[socket].shuffle( GEM_CYCLE );
			}
		} );
	}

	//an empowered socket: its motes on the ring run a lap, brighter now; its new pip glints
	private void empowered( int slot, int pip, int color ){
		halo.lap( slot );
		Card c = slot >= 0 && slot < SOCKETS ? socketCards[slot] : null;
		PointF at = c != null ? c.pipCenter( pip ) : null;
		if (at != null) fx.glint( at.x, at.y, color );
	}

	//a point of a window's (a card's gem) where it shows on this scene
	private static PointF onScene( Component c, PointF at ){
		if (c == null || at == null || c.camera() == null || Camera.main == null) return null;
		Point s = c.camera().cameraToScreen( at.x, at.y );
		return Camera.main.screenToCamera( s.x, s.y );
	}

	private void confirmUnweave( final Piece<?> p ){
		addToFront( new WndOptions( new ItemSprite( target ), Messages.titleCase( target.name() ),
				Messages.get( EnchantingScene.class, p.isWeapon() ? "unweave_msg_weapon" : "unweave_msg_armor", DISENCHANT_ENERGY ),
				Messages.get( EnchantingScene.class, "unweave", DISENCHANT_ENERGY ),
				Messages.get( EnchantingStation.class, "cancel" ) ){
			@Override
			protected void onSelect( int index ) {
				if (index != 0 || !p.usable() || !p.hasGood()) return;
				//every socket's motes leak out of the ring and fall, greying; it fills again in violet
				halo.leak( fx );
				p.unweave();
				Dungeon.energy += DISENCHANT_ENERGY;
				createEnergy();
				halo.refill( REFILL );
				Item.updateQuickslot();
			}
		} );
	}

	//shared success path: vfx, sound, and a state refresh
	private void onWeave(){
		onWeave( true );
	}

	//the same, the ring's flare now or (a weave's comet still flying) when it lands
	private void onWeave( boolean flareNow ){
		bubbleEmitter.start( Speck.factory( Speck.BUBBLE ), 0.01f, 100 );
		sparkEmitter.burst( SparkParticle.FACTORY, 20 );
		if (flareNow) halo.flare();
		Sample.INSTANCE.play( Assets.Sounds.READ );
		Item.updateQuickslot();
		saveNeeded = true;
		updateState();
	}

	private void updateEnergyText(){
		//energy is never allowed to read below zero, whatever spent it
		if (Dungeon.energy < 0) Dungeon.energy = 0;
		energyText.text( Integer.toString( Dungeon.energy ) );
		//the chip's width follows the number: centred at the bottom of a tall screen
		float chipW = 3 + energyIcon.width() + 1 + energyText.width() + 2 + 16 + 2;
		if (!wide) energyBg.x = px - chipW / 2f;
		energyIcon.x = energyBg.x + 3;
		energyIcon.y = energyBg.y + 2;
		energyText.setPos( energyIcon.x + energyIcon.width() + 1, energyIcon.y + (energyIcon.height() - energyText.height()) / 2f );
		energyAdd.setRect( energyText.right() + 2, energyIcon.y, 16, 16 );
		energyBg.size( energyAdd.right() - energyBg.x + 2, 20 );
		align( energyIcon );
		align( energyText );
		sparkEmitter.pos( px - SLOT_SIZE / 2f, py - SLOT_SIZE / 2f, SLOT_SIZE, SLOT_SIZE );
	}

	//called when items are energized while this scene is up
	public void createEnergy(){
		bubbleEmitter.start( Speck.factory( Speck.BUBBLE ), 0.01f, 100 );
		sparkEmitter.burst( SparkParticle.FACTORY, 20 );
		//at half: the lightning is a whole bolt now, and this is a window's spark
		Sample.INSTANCE.play( Assets.Sounds.LIGHTNING, 0.5f );

		saveNeeded = true;
		updateState();
	}

	private boolean saveNeeded = false;

	private void saveAll(){
		saveNeeded = false;
		try {
			Dungeon.saveAll();
			Badges.saveGlobal();
			Journal.saveGlobal();
		} catch (IOException e) {
			WarpedPixelDungeon.reportException( e );
		}
	}

	@Override
	public void onPause() {
		if (saveNeeded) {
			saveAll();
		}
	}

	@Override
	public void destroy() {
		target = null;
		saveAll();
		super.destroy();
	}

	//a description's first sentence, its highlights closed
	static String gist( String desc ){
		int cut = desc.indexOf( ". " );
		int nl = desc.indexOf( '\n' );
		String s;
		if (nl >= 0 && (cut < 0 || nl < cut)) s = desc.substring( 0, nl ).trim();
		else s = cut < 0 ? desc.trim() : desc.substring( 0, cut + 1 );
		int marks = 0;
		for (int i = 0; i < s.length(); i++) if (s.charAt( i ) == '_') marks++;
		return marks % 2 == 0 ? s : s + "_";
	}

	// ------------------------------------------------------------------ the piece

	/** a weapon's enchantments or an armor's glyphs, as one: the scene never asks which */
	static abstract class Piece<E> {

		abstract boolean isWeapon();
		abstract Item item();
		abstract E at( int slot );
		abstract E[] roll();
		abstract boolean valid( E[] opts );
		abstract void weave( E e );
		abstract void replace( int slot, E e );
		abstract boolean cursed();
		abstract boolean hasGood();
		abstract void unweave();
		abstract int maxLevel();
		abstract int level( E e );
		abstract void level( E e, int value );
		abstract String rawName( E e );
		abstract String desc( E e );
		abstract String rawDesc( E e );
		abstract int color( E e );

		String name( E e ){
			return Messages.titleCase( rawName( e ) );
		}

		String gist( E e ){
			return EnchantingScene.gist( rawDesc( e ) );
		}

		int filled(){
			int n = 0;
			for (int s = 0; s < SOCKETS; s++) if (at( s ) != null) n++;
			return n;
		}

		int[] colors(){
			ArrayList<Integer> out = new ArrayList<>();
			for (int s = 0; s < SOCKETS; s++) if (at( s ) != null) out.add( color( at( s ) ) );
			int[] c = new int[out.size()];
			for (int i = 0; i < c.length; i++) c[i] = out.get( i );
			return c;
		}

		//the filled sockets' families and levels, in the order colors() gives their colours
		Family[] families(){
			Family[] f = new Family[filled()];
			int i = 0;
			for (int s = 0; s < SOCKETS; s++){
				EnchantLook l = at( s ) != null ? look( at( s ) ) : null;
				if (at( s ) != null) f[i++] = l != null ? l.fam : null;
			}
			return f;
		}

		int[] levels(){
			int[] v = new int[filled()];
			int i = 0;
			for (int s = 0; s < SOCKETS; s++) if (at( s ) != null) v[i++] = level( at( s ) );
			return v;
		}

		//a weave's look, and the socket it sits in (-1: none)
		EnchantLook look( E e ){
			return e == null ? null : EnchantLooks.of( e.getClass() );
		}

		int slotOf( E e ){
			for (int s = 0; s < SOCKETS; s++) if (at( s ) == e) return s;
			return -1;
		}

		boolean usable(){
			return Dungeon.hero.belongings.contains( item() ) && !cursed();
		}
	}

	static class WeaponPiece extends Piece<Weapon.Enchantment> {

		private final Weapon wep;

		WeaponPiece( Weapon wep ){
			this.wep = wep;
		}

		@Override boolean isWeapon(){ return true; }
		@Override Item item(){ return wep; }
		@Override Weapon.Enchantment at( int slot ){ return slot == 0 ? wep.enchantment : wep.enchantment2; }

		//one common, one uncommon and one of any kind, never one the weapon has
		@Override
		Weapon.Enchantment[] roll(){
			Weapon.Enchantment[] opts = new Weapon.Enchantment[3];
			Class<? extends Weapon.Enchantment> e1 = wep.enchantment != null ? wep.enchantment.getClass() : null;
			Class<? extends Weapon.Enchantment> e2 = wep.enchantment2 != null ? wep.enchantment2.getClass() : null;
			opts[0] = Weapon.Enchantment.randomCommon( e1, e2 );
			opts[1] = Weapon.Enchantment.randomUncommon( e1, e2 );
			opts[2] = Weapon.Enchantment.random( e1, e2, opts[0].getClass(), opts[1].getClass() );
			return opts;
		}

		@Override
		boolean valid( Weapon.Enchantment[] opts ){
			if (opts == null || opts.length != 3) return false;
			for (Weapon.Enchantment o : opts){
				if (o == null) return false;
				if (wep.enchantment  != null && o.getClass() == wep.enchantment.getClass())  return false;
				if (wep.enchantment2 != null && o.getClass() == wep.enchantment2.getClass()) return false;
			}
			return true;
		}

		@Override void weave( Weapon.Enchantment e ){ wep.addEnchant( e ); }

		@Override
		void replace( int slot, Weapon.Enchantment e ){
			if (slot == 0) wep.enchantment = e;
			else wep.enchantment2 = e;
		}

		@Override boolean cursed(){ return wep.cursed || wep.hasCurseEnchant(); }
		@Override boolean hasGood(){ return wep.hasGoodEnchant(); }

		@Override
		void unweave(){
			wep.enchant( null );
		}

		@Override int maxLevel(){ return Weapon.Enchantment.MAX_LEVEL; }
		@Override int level( Weapon.Enchantment e ){ return e.level(); }
		@Override void level( Weapon.Enchantment e, int value ){ e.level( value ); }
		@Override String rawName( Weapon.Enchantment e ){ return e.name(); }
		@Override String desc( Weapon.Enchantment e ){ return e.desc(); }
		@Override String rawDesc( Weapon.Enchantment e ){ return Messages.get( e, "desc" ); }
		@Override int color( Weapon.Enchantment e ){ return e.glowing() == null ? VIOLET : e.glowing().textColor(); }
	}

	static class ArmorPiece extends Piece<Armor.Glyph> {

		private final Armor arm;

		ArmorPiece( Armor arm ){
			this.arm = arm;
		}

		@Override boolean isWeapon(){ return false; }
		@Override Item item(){ return arm; }
		@Override Armor.Glyph at( int slot ){ return slot == 0 ? arm.glyph : arm.glyph2; }

		@Override
		Armor.Glyph[] roll(){
			Armor.Glyph[] opts = new Armor.Glyph[3];
			Class<? extends Armor.Glyph> e1 = arm.glyph != null ? arm.glyph.getClass() : null;
			Class<? extends Armor.Glyph> e2 = arm.glyph2 != null ? arm.glyph2.getClass() : null;
			opts[0] = Armor.Glyph.randomCommon( e1, e2 );
			opts[1] = Armor.Glyph.randomUncommon( e1, e2 );
			opts[2] = Armor.Glyph.random( e1, e2, opts[0].getClass(), opts[1].getClass() );
			return opts;
		}

		@Override
		boolean valid( Armor.Glyph[] opts ){
			if (opts == null || opts.length != 3) return false;
			for (Armor.Glyph o : opts){
				if (o == null) return false;
				if (arm.glyph  != null && o.getClass() == arm.glyph.getClass())  return false;
				if (arm.glyph2 != null && o.getClass() == arm.glyph2.getClass()) return false;
			}
			return true;
		}

		@Override void weave( Armor.Glyph e ){ arm.addGlyph( e ); }

		@Override
		void replace( int slot, Armor.Glyph e ){
			if (slot == 0) arm.glyph = e;
			else arm.glyph2 = e;
			if (arm.checkSeal() != null) arm.checkSeal().setGlyph( arm.glyph );
		}

		@Override boolean cursed(){ return arm.cursed || arm.hasCurseGlyph(); }
		@Override boolean hasGood(){ return arm.hasGoodGlyph(); }

		@Override
		void unweave(){
			arm.inscribe( null );
		}

		@Override int maxLevel(){ return Armor.Glyph.MAX_LEVEL; }
		@Override int level( Armor.Glyph e ){ return e.level(); }
		@Override void level( Armor.Glyph e, int value ){ e.level( value ); }
		@Override String rawName( Armor.Glyph e ){ return e.name(); }
		@Override String desc( Armor.Glyph e ){ return e.desc(); }
		@Override String rawDesc( Armor.Glyph e ){ return Messages.get( e, "desc" ); }
		@Override int color( Armor.Glyph e ){ return e.glowing() == null ? VIOLET : e.glowing().textColor(); }
	}

	// ------------------------------------------------------------------ the three weaves

	//three revealed weaves as cards side by side (stacked on a narrow screen): one is taken
	//for free; closing without taking one asks first, the reveal was paid
	private class WndWeaves<E> extends Window {

		private static final int MARGIN = 4;

		WndWeaves( final Piece<E> p, final int slot, final E[] opts ){
			super();

			int maxW = Math.min( PixelScene.uiCamera.width - 16, 300 );
			boolean row = maxW >= 240;
			int width = row ? maxW : Math.min( maxW, 160 );

			IconTitle title = new IconTitle( new ItemSprite( p.item() ), Messages.get( EnchantingScene.class, "choose_title" ) );
			title.setRect( 0, 0, width, 0 );
			add( title );

			E old = slot < 0 ? null : p.at( slot );
			RenderedTextBlock msg = PixelScene.renderTextBlock( old == null
					? Messages.get( EnchantingScene.class, "choose_msg" )
					: Messages.get( EnchantingScene.class, "choose_reroll_msg", p.name( old ) ), 6 );
			msg.maxWidth( width );
			msg.setPos( 0, title.bottom() + 3 );
			add( msg );

			float top = msg.bottom() + MARGIN;
			Card[] cards = new Card[opts.length];
			float cw = row ? (width - (opts.length - 1) * MARGIN) / (float) opts.length : width;
			for (int i = 0; i < opts.length; i++){
				final int index = i;
				final E e = opts[i];
				int color = p.color( e );
				Card c = new Card( color );
				c.gem( color, false );
				c.title( p.name( e ), color );
				c.tag( Messages.get( EnchantingScene.class, "tier_" + i ) );
				c.text( p.gist( e ) );
				RedButton takeIt = new RedButton( Messages.get( EnchantingScene.class, "take" ), 6 ){
					@Override
					protected void onClick() {
						//where its gem shows, read before the window goes
						PointF from = onScene( c, c.gemCenter() );
						hide();
						take( p, slot, opts, index, from );
					}
				};
				takeIt.textColor( color );
				IconButton info = new IconButton( Icons.get( Icons.INFO ) ){
					@Override
					protected void onClick() {
						EnchantingScene.this.addToFront( new WndTitledMessage( new ItemSprite( p.item() ), p.name( e ), p.desc( e ) ) );
					}
				};
				c.buttons( takeIt, info );
				c.setRect( 0, 0, cw, 0 );
				cards[i] = c;
			}
			//side by side, the cards share the tallest one's height so their buttons line up
			float tallest = 0;
			for (Card c : cards) tallest = Math.max( tallest, c.height() );
			float y = top;
			for (int i = 0; i < cards.length; i++){
				Card c = cards[i];
				if (row){
					c.stretch( tallest );
					c.setPos( i * (cw + MARGIN), top );
				} else {
					c.setPos( 0, y );
					y = c.bottom() + MARGIN;
				}
				add( c );
			}
			float bottom = row ? top + tallest : y - MARGIN;

			//the gems ignite one after another, each then keeping its magic alive
			PedestalFx gems = new PedestalFx();
			add( gems );
			for (int i = 0; i < cards.length; i++) cards[i].live( gems, p.look( opts[i] ), IGNITE_GAP * i );

			resize( width, (int)Math.ceil( bottom ) );
		}

		@Override
		public void onBackPressed() {
			EnchantingScene.this.addToFront( new WndOptions(
					Icons.get( Icons.WARNING ),
					Messages.get( EnchantingScene.class, "leave_title" ),
					Messages.get( EnchantingScene.class, "leave_msg", REVEAL_COST ),
					Messages.get( EnchantingScene.class, "leave_no" ),
					Messages.get( EnchantingScene.class, "leave_yes" ) ){
				@Override
				protected void onSelect( int index ) {
					if (index == 1) WndWeaves.this.hide();
				}
			} );
		}
	}

	// ------------------------------------------------------------------ the pieces of the scene

	/**
	 * A card: a dark plate with a stripe of its colour down the left, a gem, a title in the
	 * colour, level pips, a line of text and a row of buttons. Its height follows its width.
	 */
	private static class Card extends Component {

		private static final int PAD = 4;
		private static final int BTN_H = 15;

		private final int color;
		private NinePatch bg;
		private ColorBlock stripe;
		private Gem gem;
		private RenderedTextBlock title;
		private RenderedTextBlock tag;
		private ColorBlock[] pips;
		private int pipsOn;
		private RenderedTextBlock text;
		private Component[] buttons;
		private float minHeight = 0;
		//a revealed weave's live magic: the pedestal's effects, its look, its gem's ignition and its
		//motes (at most MOTIF_LIVE of its own at once), a glint sweeping its stripe
		private PedestalFx fx;
		private EnchantLook look;
		private float igniteIn, motifIn, sweep = -1;
		private boolean igniting;
		private ColorBlock sweeper;
		private final PedestalFx.Mote[] motes = new PedestalFx.Mote[MOTIF_LIVE];
		private static final int MOTIF_LIVE = 3;
		private static final float MOTIF_EVERY = 0.8f, SWEEP_FOR = 0.15f;

		Card( int color ){
			super();
			this.color = color;
			bg = Chrome.get( Chrome.Type.TOAST_TR );
			add( bg );
			stripe = new ColorBlock( 2, 1, 0xFF000000 | color );
			stripe.alpha( 0.8f );
			add( stripe );
		}

		void gem( int color, boolean hollow ){
			gem = new Gem( color, hollow );
			add( gem );
		}

		void title( String s, int color ){
			title = PixelScene.renderTextBlock( s, 9 );
			title.hardlight( color );
			add( title );
		}

		void tag( String s ){
			tag = PixelScene.renderTextBlock( s, 6 );
			tag.hardlight( FADED );
			add( tag );
		}

		void pips( int level, int color ){
			pipsOn = level;
			pips = new ColorBlock[Weapon.Enchantment.MAX_LEVEL];
			for (int i = 0; i < pips.length; i++){
				pips[i] = new ColorBlock( 6, 3, i < level ? 0xFF000000 | color : 0xFF3A2E4A );
				add( pips[i] );
			}
		}

		void text( String s ){
			text = PixelScene.renderTextBlock( s, 6 );
			add( text );
		}

		//its gem ignites `delay` s from now with a burst of its look, its stripe glints, and then
		//its magic stays alive: a mote of its motif every MOTIF_EVERY s
		void live( PedestalFx fx, EnchantLook look, float delay ){
			if (fx == null || look == null || gem == null) return;
			this.fx = fx;
			this.look = look;
			igniteIn = delay;
			igniting = true;
			motifIn = MOTIF_EVERY * 0.5f;
			gem.ignite( delay );
			sweeper = new ColorBlock( 2, 3, 0xFFFFFFFF );
			sweeper.visible = false;
			add( sweeper );
		}

		//its gem's middle (null with none)
		PointF gemCenter(){
			return gem == null ? null : new PointF( gem.left() + Gem.SIZE / 2f, gem.top() + Gem.SIZE / 2f );
		}

		//a pip's middle (null with none)
		PointF pipCenter( int i ){
			if (pips == null || i < 0 || i >= pips.length) return null;
			return new PointF( pips[i].x + pips[i].width() / 2f, pips[i].y + pips[i].height() / 2f );
		}

		//its gem shuffles through the prism, then settles on its colour
		void shuffle( float seconds ){
			if (gem != null) gem.shuffle( seconds );
		}

		void buttons( Component... b ){
			buttons = b;
			for (Component c : b) add( c );
		}

		//at least this tall, buttons at the bottom (cards side by side line their buttons up)
		void stretch( float height ){
			minHeight = height;
			layout();
		}

		@Override
		protected void layout() {
			float inner = width - 2 * PAD - 3;
			float x0 = x + PAD + 3;
			float y0 = y + PAD;

			float headX = x0;
			if (gem != null){
				gem.setRect( x0, y0 + 1, Gem.SIZE, Gem.SIZE );
				headX = gem.right() + 3;
			}
			float cursor = y0;
			if (title != null){
				title.maxWidth( (int)(x + width - PAD - headX) );
				title.setPos( headX, y0 + (gem != null ? Math.max( 0, (Gem.SIZE + 2 - title.height()) / 2f ) : 0) );
				PixelScene.align( title );
				cursor = Math.max( cursor, title.bottom() );
			}
			if (gem != null) cursor = Math.max( cursor, gem.bottom() );
			if (tag != null){
				tag.setPos( headX, cursor + 1 );
				PixelScene.align( tag );
				cursor = tag.bottom();
			}
			if (pips != null){
				float pxx = headX;
				for (ColorBlock pip : pips){
					pip.x = pxx;
					pip.y = cursor + 3;
					pxx += 7;
				}
				cursor += 6;
			}
			if (text != null){
				text.maxWidth( (int)inner );
				text.setPos( x0, cursor + 3 );
				PixelScene.align( text );
				cursor = text.bottom();
			}

			float h = cursor - y + PAD;
			if (buttons != null){
				h += 3 + BTN_H;
			}
			h = Math.max( h, minHeight );
			height = h;

			if (buttons != null){
				float by = y + height - PAD - BTN_H;
				//an icon button is a square; the others share what is left
				int wide = 0;
				float fixed = 0;
				for (Component b : buttons){
					if (b instanceof IconButton) fixed += BTN_H;
					else wide++;
				}
				float gaps = (buttons.length - 1) * 2;
				float bw = wide == 0 ? 0 : (inner - fixed - gaps) / wide;
				float bx = x0;
				for (Component b : buttons){
					float thisW = b instanceof IconButton ? BTN_H : bw;
					b.setRect( bx, by, thisW, BTN_H );
					PixelScene.align( b );
					bx += thisW + 2;
				}
			}

			bg.x = x;
			bg.y = y;
			bg.size( width, height );
			stripe.x = x + 2;
			stripe.y = y + 3;
			stripe.size( 2, height - 6 );
		}

		private float pulse = 0;

		@Override
		public void update() {
			super.update();
			if (pips != null && pipsOn > 0){
				//the highest lit pip breathes, so the level reads at a glance
				pulse += Game.elapsed;
				pips[pipsOn - 1].alpha( 0.65f + 0.35f * (float)Math.abs( Math.sin( pulse * 2.4f ) ) );
			}
			if (fx == null) return;
			float dt = Game.elapsed;
			if (igniting){
				if ((igniteIn -= dt) > 0) return;
				igniting = false;
				PointF g = gemCenter();
				fx.burst( g.x, g.y, look, PedestalFx.IGNITE, 18f );
				sweep = 0;
			}
			if (sweep >= 0){
				sweep += dt;
				float k = Math.min( 1f, sweep / SWEEP_FOR );
				sweeper.visible = sweep < SWEEP_FOR;
				sweeper.x = stripe.x;
				sweeper.y = stripe.y + (stripe.height() - 3) * k;
				sweeper.alpha( 0.9f * (1f - k) );
				if (!sweeper.visible) sweep = -1;
			}
			if ((motifIn -= dt) <= 0){
				motifIn = MOTIF_EVERY;
				for (int i = 0; i < motes.length; i++){
					PedestalFx.Mote m = motes[i];
					if (m != null && m.alive && m.owner == this) continue;
					PointF g = gemCenter();
					motes[i] = fx.motif( look, g.x, g.y, this );
					break;
				}
			}
		}
	}

	/** a cut gem of a colour: dark rim, the colour, a glint; a hollow one is an empty socket */
	private static class Gem extends Component {

		static final int SIZE = 9;

		private final ColorBlock rim, face, glint;
		private final boolean hollow;
		private final int color;
		private float t = Random.Float( 6f );
		//dark until it ignites, bright a moment as it does; shuffling through the prism's colours
		private float igniteIn, lit, shuffleLeft, shuffleFor;
		private boolean igniting;
		private static final float LIT_FOR = 0.15f;

		Gem( int color, boolean hollow ){
			super();
			this.hollow = hollow;
			this.color = color;
			rim = new ColorBlock( SIZE, SIZE, 0xFF140A22 );
			add( rim );
			//white, lit in its colour: so it can shuffle through others
			face = new ColorBlock( SIZE - 2, SIZE - 2, 0xFFFFFFFF );
			face.hardlight( color );
			add( face );
			glint = new ColorBlock( 2, 2, 0xFFFFFFFF );
			glint.alpha( hollow ? 0f : 0.7f );
			add( glint );
			if (hollow) face.alpha( 0.35f );
		}

		@Override
		protected void layout() {
			rim.x = x;
			rim.y = y;
			face.x = x + 1;
			face.y = y + 1;
			glint.x = x + 2;
			glint.y = y + 2;
		}

		void ignite( float delay ){
			igniteIn = delay;
			igniting = true;
			face.alpha( 0.25f );
			glint.alpha( 0f );
		}

		void shuffle( float seconds ){
			shuffleFor = shuffleLeft = seconds;
		}

		@Override
		public void update() {
			super.update();
			if (hollow) return;
			float dt = Game.elapsed;
			if (igniting){
				if ((igniteIn -= dt) > 0) return;
				igniting = false;
				lit = LIT_FOR;
			}
			if (shuffleLeft > 0){
				shuffleLeft -= dt;
				int n = Element.PRISM.ramp.size();
				int i = Math.min( n - 1, (int)((1f - Math.max( 0f, shuffleLeft ) / shuffleFor) * n) );
				face.hardlight( shuffleLeft > 0 ? Element.PRISM.ramp.stop( i ) : color );
			}
			if (lit > 0){
				lit -= dt;
				face.alpha( 1f );
				glint.alpha( 1f );
				if (lit <= 0) glint.alpha( 0.7f );
				return;
			}
			t += dt;
			face.alpha( 0.8f + 0.2f * (float)Math.sin( t * 2f ) );
		}
	}

	/**
	 * the pedestal's ring: two circles of motes in the piece's colours, turning opposite ways, each
	 * moving as its weave's family does (PedestalFx.motion: embers rise, frost drifts down, static
	 * jumps...) and a tenth brighter for each level of it; an empowered socket's motes run a lap; held
	 * as it was while a weave's comet flies, taking the new weave as it lands; three faint ghost motes
	 * on the outer circle for a vestige
	 */
	private static class Halo extends Component {

		private static final int OUTER = 16, INNER = 10, ECHOES = 3;
		private static final float LAP_FOR = 1f, ECHO_ALPHA = 0.3f;

		private final ColorBlock[] outer = new ColorBlock[OUTER];
		private final ColorBlock[] inner = new ColorBlock[INNER];
		private final ColorBlock[] echoes = new ColorBlock[ECHOES];
		private float cx, cy, angle, flare, time, refill, refillFor;
		//each filled socket's colour (brightened by its level) and family; a lap it runs
		private int[] shown = { VIOLET };
		private Family[] fams = new Family[0];
		private final float[] lap = new float[SOCKETS];
		private final float[] move = new float[4];
		//while a weave's comet flies: what the ring is told waits for it to land
		private boolean held, pending;
		private int[] heldColors, heldLevels;
		private Family[] heldFams;

		Halo(){
			super();
			for (int i = 0; i < OUTER; i++){
				outer[i] = new ColorBlock( 2, 2, 0xFFFFFFFF );
				add( outer[i] );
			}
			for (int i = 0; i < INNER; i++){
				inner[i] = new ColorBlock( 1, 1, 0xFFFFFFFF );
				add( inner[i] );
			}
			for (int i = 0; i < ECHOES; i++){
				echoes[i] = new ColorBlock( 1, 1, 0xFFFFFFFF );
				echoes[i].hardlight( EnchantLook.GHOST );
				echoes[i].visible = false;
				add( echoes[i] );
			}
			colors( null, null, null );
		}

		void center( float x, float y ){
			cx = x;
			cy = y;
		}

		//the piece's weaves (their colours, families and levels), or the pedestal's violet with none
		void colors( int[] colors, Family[] families, int[] levels ){
			if (held){
				pending = true;
				heldColors = colors;
				heldFams = families;
				heldLevels = levels;
				return;
			}
			if (colors == null || colors.length == 0){
				shown = new int[]{ VIOLET };
				fams = new Family[0];
			} else {
				shown = new int[colors.length];
				for (int i = 0; i < colors.length; i++){
					int level = levels != null && i < levels.length ? Math.max( 0, Math.min( 5, levels[i] ) ) : 0;
					shown[i] = Ramp.lerp( colors[i] & 0xFFFFFF, 0xFFFFFF, 0.1f * level );
				}
				fams = families != null ? families : new Family[0];
			}
			for (int i = 0; i < OUTER; i++) outer[i].hardlight( shown[i % shown.length] );
			for (int i = 0; i < INNER; i++) inner[i].hardlight( shown[(i + 1) % shown.length] );
		}

		//held as it is (a comet flying), or let go, taking what it was told meanwhile
		void hold( boolean on ){
			held = on;
			if (!on && pending){
				pending = false;
				colors( heldColors, heldFams, heldLevels );
			}
		}

		//a weave lands: the ring swells and brightens, then settles
		void flare(){
			flare = 1f;
		}

		//an empowered socket: its motes run a lap ahead of the rest, brighter as they go
		void lap( int socket ){
			if (socket >= 0 && socket < SOCKETS) lap[socket] = LAP_FOR;
		}

		//the ring emptied: it fills again over `seconds`
		void refill( float seconds ){
			refill = refillFor = seconds;
		}

		void vestige( boolean on ){
			for (ColorBlock e : echoes) e.visible = on;
		}

		//every mote leaks out of the ring, falling and greying (an unweave)
		void leak( PedestalFx fx ){
			for (int i = 0; i < OUTER; i++) leak( fx, outer[i], shown[i % shown.length], 1f );
			for (int i = 0; i < INNER; i++) leak( fx, inner[i], shown[(i + 1) % shown.length], 0.5f );
		}

		private void leak( PedestalFx fx, ColorBlock m, int color, float half ){
			float x = m.x + half, y = m.y + half, dx = x - cx, dy = y - cy;
			float len = Math.max( 0.01f, (float)Math.sqrt( dx * dx + dy * dy ) );
			fx.leak( x, y, color, dx / len, dy / len );
		}

		private Family fam( int i ){
			return i < fams.length ? fams[i] : null;
		}

		//how far ahead a socket's lap has its motes, and how much brighter
		private float ahead( int socket ){
			if (socket >= SOCKETS || lap[socket] <= 0) return 0f;
			float p = 1f - lap[socket] / LAP_FOR;
			return (float)(2 * Math.PI) * p * p * (3f - 2f * p);
		}

		private float bright( int socket ){
			if (socket >= SOCKETS || lap[socket] <= 0) return 0f;
			return 0.3f * (float)Math.sin( Math.PI * (1f - lap[socket] / LAP_FOR) );
		}

		@Override
		public void update() {
			super.update();
			float dt = Game.elapsed;
			time += dt;
			angle += dt * 0.5f;
			if (flare > 0) flare = Math.max( 0, flare - dt * 1.5f );
			for (int s = 0; s < SOCKETS; s++) if (lap[s] > 0) lap[s] = Math.max( 0, lap[s] - dt );
			if (refill > 0) refill = Math.max( 0, refill - dt );
			float fill = refillFor > 0 ? 1f - refill / refillFor : 1f;
			int n = shown.length;
			float r = HALO_R + 4 * flare;
			for (int i = 0; i < OUTER; i++){
				int s = i % n;
				double a = angle + i * Math.PI * 2 / OUTER + ahead( s );
				PedestalFx.motion( fam( s ), time, i, move );
				float rr = r + move[2];
				outer[i].x = cx + (float)Math.cos( a ) * rr - 1 + move[0];
				outer[i].y = cy + (float)Math.sin( a ) * rr - 1 + move[1];
				outer[i].alpha( fill * Math.min( 1f, (0.35f + 0.45f * (float)Math.abs( Math.sin( angle * 2 + i ) ) + flare + bright( s )) * move[3] ) );
			}
			float ri = r - 7;
			for (int i = 0; i < INNER; i++){
				int s = (i + 1) % n;
				double a = -angle * 1.6f + i * Math.PI * 2 / INNER - ahead( s );
				PedestalFx.motion( fam( s ), time, i + OUTER, move );
				float rr = ri + move[2] * 0.5f;
				inner[i].x = cx + (float)Math.cos( a ) * rr + move[0] * 0.5f;
				inner[i].y = cy + (float)Math.sin( a ) * rr + move[1] * 0.5f;
				inner[i].alpha( fill * Math.min( 1f, (0.3f + 0.3f * (float)Math.abs( Math.cos( angle * 3 + i ) ) + flare + bright( s )) * move[3] ) );
			}
			//a vestige's: faint, at half the outer circle's speed, just outside it
			for (int i = 0; i < ECHOES; i++){
				double a = angle * 0.5f + i * Math.PI * 2 / ECHOES + 0.45f;
				echoes[i].x = cx + (float)Math.cos( a ) * (r + 2);
				echoes[i].y = cy + (float)Math.sin( a ) * (r + 2);
				echoes[i].alpha( ECHO_ALPHA * fill );
			}
		}
	}

	private final WndBag.ItemSelector weaponSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get( EnchantingStation.class, "prompt" );
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable( Item item ) {
			return (item instanceof Weapon || item instanceof Armor) && item.isIdentified();
		}

		@Override
		public void onSelect( Item item ) {
			if (item instanceof Weapon || item instanceof Armor) {
				target = item;
				updateState();
			}
		}
	};

	private class WeaponButton extends Component {

		protected NinePatch bg;
		protected ItemSlot slot;

		private Item item = null;

		@Override
		protected void createChildren() {
			super.createChildren();

			bg = Chrome.get( Chrome.Type.RED_BUTTON );
			add( bg );

			slot = new ItemSlot() {
				@Override
				protected void onPointerDown() {
					bg.brightness( 1.2f );
					Sample.INSTANCE.play( Assets.Sounds.CLICK );
				}
				@Override
				protected void onPointerUp() {
					bg.resetColor();
				}
				@Override
				protected void onClick() {
					super.onClick();
					EnchantingScene.this.addToFront( WndBag.getBag( weaponSelector ) );
				}

				@Override
				protected boolean onLongClick() {
					if (item != null){
						EnchantingScene.this.addToFront( new WndInfoItem( item ) );
						return true;
					}
					return false;
				}

				@Override
				public GameAction keyAction() {
					return WPDAction.INVENTORY;
				}

				@Override
				protected String hoverText() {
					if (item == null){
						return Messages.get( EnchantingScene.class, "select" );
					}
					return super.hoverText();
				}
			};
			slot.enable( true );
			add( slot );
		}

		@Override
		protected void layout() {
			super.layout();

			bg.x = x;
			bg.y = y;
			bg.size( width, height );

			slot.setRect( x + 2, y + 2, width - 4, height - 4 );
		}

		public void item( Item item ) {
			this.item = item;
			slot.item( item );
		}
	}
}
