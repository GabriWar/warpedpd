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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.journal.Document;
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
import xyz.gabriwar.warpedpixeldungeon.ui.StyledButton;
import xyz.gabriwar.warpedpixeldungeon.windows.IconTitle;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndEnergizeItem;
import xyz.gabriwar.warpedpixeldungeon.windows.WndInfoItem;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndStory;
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
import com.watabou.utils.RectF;
import com.watabou.utils.Random;

import java.io.IOException;
import java.util.ArrayList;

/**
 * The enchanting pedestal's fullscreen crafting scene, in the style of the
 * alchemy scene: lay a weapon on the pedestal, then empower, reroll or weave
 * its enchantments using alchemical energy.
 */
public class EnchantingScene extends PixelScene {

	//the weapon or armor being worked on. a reference into the hero's
	// belongings, never detached - changes apply to it wherever it lives
	private static Item target = null;

	public static final int INFUSE_COST = 40;

	public static int empowerCost( int level ){
		//10/20/30/40/50 energy for levels 1..5
		return 10 * (level + 1);
	}

	public static int rerollCost( int level ){
		//rerolling keeps the slot's level, so pricier the more empowered it is
		return 20 + 10 * level;
	}

	private Emitter bubbleEmitter;
	private Emitter lowerBubbles;
	private Emitter sparkEmitter;
	private SkinnedBlock water;

	private Image energyIcon;
	private RenderedTextBlock energyLeft;
	private IconButton energyAdd;

	private WeaponButton weaponSlot;
	private RenderedTextBlock desc;
	private final ArrayList<Component> actionElems = new ArrayList<>();

	private static int centerW;
	private int pw;
	private int left;
	private float actionTop;

	private static final int BTN_SIZE   = 28;
	private static final int BTN_HEIGHT = 16;
	private static final int GAP        = 2;

	{
		inGameScene = true;
	}

	@Override
	public void create() {
		super.create();

		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();

		water = new SkinnedBlock(
				w, h,
				Dungeon.level.waterTex() ){

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
		add(water);

		//a violet veil over the water, so the pedestal feels arcane rather than damp
		ColorBlock veil = new ColorBlock( w, h, 0x66341a66 );
		add(veil);

		Image im = new Image(TextureCache.createGradient(0x662a1144, 0x88221144, 0xAA190b33, 0xCC120722, 0xFF0a0414));
		im.angle = 90;
		im.x = w;
		im.scale.x = h/5f;
		im.scale.y = w;
		add(im);

		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;

		ExitButton btnExit = new ExitButton(){
			@Override
			protected void onClick() {
				Game.switchScene(GameScene.class);
			}
		};
		btnExit.setPos( insets.left + w - btnExit.width(), insets.top );
		add( btnExit );

		bubbleEmitter = new Emitter();
		add(bubbleEmitter);

		lowerBubbles = new Emitter();
		add(lowerBubbles);

		IconTitle title = new IconTitle( new ItemSprite(ItemSpriteSheet.STYLUS),
				Messages.titleCase(Messages.get(EnchantingStation.class, "name")) );
		title.setSize(200, 0);
		title.setPos(
				insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (20 - title.height()) / 2f
		);
		align(title);
		add(title);

		pw = Math.min(50 + w/2, 150);
		left = (int)(insets.left) + (w - pw)/2;
		centerW = left + pw/2;

		int pos = (int)(insets.top) + (h - 160)/2;

		desc = PixelScene.renderTextBlock(6);
		desc.maxWidth(pw);
		desc.text( Messages.get(EnchantingScene.class, "text") );
		desc.setPos(left + (pw - desc.width())/2, pos);
		add(desc);

		pos += desc.height() + 6;

		NinePatch inputBG = Chrome.get(Chrome.Type.TOAST_TR);
		inputBG.x = left + (pw - BTN_SIZE - 8)/2f;
		inputBG.y = pos;
		inputBG.size(BTN_SIZE+8, BTN_SIZE+8);
		add(inputBG);

		weaponSlot = new WeaponButton();
		weaponSlot.setRect(inputBG.x + 4, pos + 4, BTN_SIZE, BTN_SIZE);
		add(weaponSlot);

		actionTop = pos + BTN_SIZE + 8 + 4;

		if (Camera.main.height >= 280){
			centerW = (int)(insets.left) + w/2;
		}

		bubbleEmitter.pos(0,
				0,
				2*centerW,
				Camera.main.height);
		bubbleEmitter.autoKill = false;

		lowerBubbles.pos(0,
				0,
				2*centerW,
				Camera.main.height);
		lowerBubbles.autoKill = false;

		String energyText = Messages.get(AlchemyScene.class, "energy") + " " + Dungeon.energy;

		energyLeft = PixelScene.renderTextBlock(energyText, 9);
		energyLeft.setPos(
				centerW - energyLeft.width()/2,
				insets.top + h - 8 - energyLeft.height()
		);
		energyLeft.hardlight(0x44CCFF);
		add(energyLeft);

		energyIcon = new ItemSprite( ItemSpriteSheet.ENERGY );
		energyIcon.x = energyLeft.left() - energyIcon.width();
		energyIcon.y = energyLeft.top() - (energyIcon.height() - energyLeft.height())/2;
		align(energyIcon);
		add(energyIcon);

		energyAdd = new IconButton(Icons.get(Icons.PLUS)){
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
				return Messages.get(AlchemyScene.class, "energize");
			}
		};
		energyAdd.setRect(energyLeft.right(), energyLeft.top() - (16 - energyLeft.height())/2, 16, 16);
		align(energyAdd);
		add(energyAdd);

		sparkEmitter = new Emitter();
		sparkEmitter.pos(energyLeft.left(), energyLeft.top(), energyLeft.width(), energyLeft.height());
		sparkEmitter.autoKill = false;
		add(sparkEmitter);

		StyledButton btnGuide = new StyledButton( Chrome.Type.TOAST_TR, Messages.get(AlchemyScene.class, "guide")){
			@Override
			protected void onClick() {
				super.onClick();
				Document.ADVENTURERS_GUIDE.findPage( Document.GUIDE_ENCHANTING );
				EnchantingScene.this.addToFront( new WndStory(
						Document.ADVENTURERS_GUIDE.pageSprite( Document.GUIDE_ENCHANTING ),
						Document.ADVENTURERS_GUIDE.pageTitle( Document.GUIDE_ENCHANTING ),
						Document.ADVENTURERS_GUIDE.pageBody( Document.GUIDE_ENCHANTING ) ));
				Document.ADVENTURERS_GUIDE.readPage( Document.GUIDE_ENCHANTING );
			}

			@Override
			public GameAction keyAction() {
				return WPDAction.JOURNAL;
			}

			@Override
			protected String hoverText() {
				return Messages.titleCase( Document.ADVENTURERS_GUIDE.pageTitle( Document.GUIDE_ENCHANTING ) );
			}
		};
		btnGuide.icon(new ItemSprite(ItemSpriteSheet.GUIDE_PAGE));
		btnGuide.setSize(btnGuide.reqWidth()+4, 18);
		btnGuide.setPos(centerW - btnGuide.width()/2f, energyAdd.top() - btnGuide.height()-2);
		align(btnGuide);
		add(btnGuide);

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
	}

	@Override
	protected void onBackPressed() {
		Game.switchScene(GameScene.class);
	}

	//rebuilds the action buttons for the current weapon and energy
	private void updateState(){

		for (Component c : actionElems){
			c.killAndErase();
		}
		actionElems.clear();

		weaponSlot.item(target);

		float pos = actionTop;

		if (target == null){
			desc.text( Messages.get(EnchantingScene.class, "text") );
			desc.setPos(left + (pw - desc.width())/2, desc.top());
			updateEnergyText();
			return;
		}

		desc.text( Messages.titleCase(target.name()) );
		desc.setPos(left + (pw - desc.width())/2, desc.top());

		//cursed items refuse the glyphs entirely
		boolean cursed = target.cursed
				|| (target instanceof Weapon && ((Weapon) target).hasCurseEnchant())
				|| (target instanceof Armor && ((Armor) target).hasCurseGlyph());
		if (cursed){
			RenderedTextBlock msg = PixelScene.renderTextBlock(
					Messages.get(EnchantingStation.class, "cursed"), 6 );
			msg.maxWidth( pw );
			msg.setPos( left + (pw - msg.width())/2f, pos );
			add( msg );
			actionElems.add( msg );
			updateEnergyText();
			return;
		}

		if (target instanceof Weapon){
			pos = buildWeaponActions( (Weapon) target, pos );
		} else if (target instanceof Armor){
			pos = buildArmorActions( (Armor) target, pos );
		}

		updateEnergyText();
	}

	private float buildWeaponActions( final Weapon wep, float pos ){

		//one empower + one reroll button per enchantment on the weapon
		for (final Weapon.Enchantment ench : wep.enchantments()){

			String label;
			boolean enabled;
			final int cost = empowerCost( ench.level() );

			if (ench.level() >= Weapon.Enchantment.MAX_LEVEL){
				label = Messages.get(EnchantingStation.class, "maxed",
						Messages.get(ench, "enchant"), ench.level());
				enabled = false;
			} else {
				label = Messages.get(EnchantingStation.class, "empower",
						Messages.get(ench, "enchant"), ench.level(), ench.level() + 1, cost);
				enabled = Dungeon.energy >= cost;
			}

			RedButton btn = new RedButton( label, 6 ){
				@Override
				protected void onClick() {
					Dungeon.energy -= cost;
					ench.level( ench.level() + 1 );
					onWeave();
				}
			};
			btn.textColor( ench.glowing().color );
			btn.enable( enabled );
			btn.multiline = true;
			btn.setRect( left, pos, pw, BTN_HEIGHT );
			add( btn );
			actionElems.add( btn );
			pos = btn.bottom() + GAP;

			final int rerollCost = rerollCost( ench.level() );
			RedButton reroll = new RedButton( Messages.get(EnchantingStation.class, "reroll",
					Messages.get(ench, "enchant"), rerollCost), 6 ){
				@Override
				protected void onClick() {
					chooseReroll( wep, ench, rerollCost );
				}
			};
			reroll.enable( Dungeon.energy >= rerollCost );
			reroll.multiline = true;
			reroll.setRect( left, pos, pw, BTN_HEIGHT );
			add( reroll );
			actionElems.add( reroll );
			pos = reroll.bottom() + GAP;
		}

		//weave a new enchantment when a slot is free
		if (wep.enchantment == null || wep.enchantment2 == null){
			final boolean first = wep.enchantment == null;
			String label = Messages.get(EnchantingStation.class,
					first ? "infuse_first" : "infuse", INFUSE_COST);

			RedButton btn = new RedButton( label, 6 ){
				@Override
				protected void onClick() {
					chooseInfusion( wep );
				}
			};
			btn.enable( Dungeon.energy >= INFUSE_COST );
			btn.multiline = true;
			btn.setRect( left, pos, pw, BTN_HEIGHT );
			add( btn );
			actionElems.add( btn );
			pos = btn.bottom() + GAP;
		}

		return pos;
	}

	private float buildArmorActions( final Armor arm, float pos ){

		//one empower + one reroll button per glyph on the armor
		for (final Armor.Glyph gl : arm.glyphs()){

			String label;
			boolean enabled;
			final int cost = empowerCost( gl.level() );

			if (gl.level() >= Armor.Glyph.MAX_LEVEL){
				label = Messages.get(EnchantingStation.class, "maxed",
						Messages.get(gl, "glyph"), gl.level());
				enabled = false;
			} else {
				label = Messages.get(EnchantingStation.class, "empower",
						Messages.get(gl, "glyph"), gl.level(), gl.level() + 1, cost);
				enabled = Dungeon.energy >= cost;
			}

			RedButton btn = new RedButton( label, 6 ){
				@Override
				protected void onClick() {
					Dungeon.energy -= cost;
					gl.level( gl.level() + 1 );
					onWeave();
				}
			};
			btn.textColor( gl.glowing().color );
			btn.enable( enabled );
			btn.multiline = true;
			btn.setRect( left, pos, pw, BTN_HEIGHT );
			add( btn );
			actionElems.add( btn );
			pos = btn.bottom() + GAP;

			final int rerollCost = rerollCost( gl.level() );
			RedButton reroll = new RedButton( Messages.get(EnchantingStation.class, "reroll",
					Messages.get(gl, "glyph"), rerollCost), 6 ){
				@Override
				protected void onClick() {
					chooseReroll( arm, gl, rerollCost );
				}
			};
			reroll.enable( Dungeon.energy >= rerollCost );
			reroll.multiline = true;
			reroll.setRect( left, pos, pw, BTN_HEIGHT );
			add( reroll );
			actionElems.add( reroll );
			pos = reroll.bottom() + GAP;
		}

		//weave a new glyph when a slot is free
		if (arm.glyph == null || arm.glyph2 == null){
			final boolean first = arm.glyph == null;
			String label = Messages.get(EnchantingStation.class,
					first ? "infuse_first" : "infuse", INFUSE_COST);

			RedButton btn = new RedButton( label, 6 ){
				@Override
				protected void onClick() {
					chooseInfusion( arm );
				}
			};
			btn.enable( Dungeon.energy >= INFUSE_COST );
			btn.multiline = true;
			btn.setRect( left, pos, pw, BTN_HEIGHT );
			add( btn );
			actionElems.add( btn );
			pos = btn.bottom() + GAP;
		}

		return pos;
	}

	//a rolled trio sticks to the weapon until one is chosen - cancelling
	// (or quitting) never rerolls it for free
	private static Weapon.Enchantment[] rollOptions( Weapon wep ){
		Weapon.Enchantment[] opts = new Weapon.Enchantment[3];
		Class<? extends Weapon.Enchantment> e1 = wep.enchantment != null ? wep.enchantment.getClass() : null;
		Class<? extends Weapon.Enchantment> e2 = wep.enchantment2 != null ? wep.enchantment2.getClass() : null;
		opts[0] = Weapon.Enchantment.randomCommon( e1, e2 );
		opts[1] = Weapon.Enchantment.randomUncommon( e1, e2 );
		opts[2] = Weapon.Enchantment.random( e1, e2, opts[0].getClass(), opts[1].getClass() );
		return opts;
	}

	//stored options go stale if the weapon's enchants changed since the roll
	private static boolean optsValid( Weapon wep, Weapon.Enchantment[] opts ){
		if (opts == null || opts.length != 3) return false;
		for (Weapon.Enchantment o : opts){
			if (o == null) return false;
			if (wep.enchantment  != null && o.getClass() == wep.enchantment.getClass())  return false;
			if (wep.enchantment2 != null && o.getClass() == wep.enchantment2.getClass()) return false;
		}
		return true;
	}

	private void chooseInfusion( final Weapon wep ){

		if (!optsValid( wep, wep.pendingWeaveOpts )){
			wep.pendingWeaveOpts = rollOptions( wep );
			saveAll();
		}
		final Weapon.Enchantment[] opts = wep.pendingWeaveOpts;

		addToFront( new WndOptions(
				new ItemSprite( wep ),
				Messages.titleCase( wep.name() ),
				Messages.get(EnchantingStation.class, "choice_msg"),
				opts[0].name(),
				opts[1].name(),
				opts[2].name(),
				Messages.get(EnchantingStation.class, "cancel") ){

			@Override
			protected void onSelect( int index ) {
				if (index < 3 && Dungeon.energy >= INFUSE_COST){
					Dungeon.energy -= INFUSE_COST;
					wep.pendingWeaveOpts = null;
					wep.addEnchant( opts[index] );
					onWeave();
				}
			}
		} );
	}

	//swap one enchantment for a fresh one, keeping the slot's level
	private void chooseReroll( final Weapon wep, final Weapon.Enchantment ench, final int cost ){

		int slot = (ench == wep.enchantment) ? 0 : 1;
		if (wep.pendingRerollSlot != slot || !optsValid( wep, wep.pendingRerollOpts )){
			wep.pendingRerollOpts = rollOptions( wep );
			wep.pendingRerollSlot = slot;
			saveAll();
		}
		final Weapon.Enchantment[] opts = wep.pendingRerollOpts;

		addToFront( new WndOptions(
				new ItemSprite( wep ),
				Messages.titleCase( wep.name() ),
				Messages.get(EnchantingStation.class, "reroll_msg", Messages.get(ench, "enchant")),
				opts[0].name(),
				opts[1].name(),
				opts[2].name(),
				Messages.get(EnchantingStation.class, "cancel") ){

			@Override
			protected void onSelect( int index ) {
				if (index < 3 && Dungeon.energy >= cost){
					Dungeon.energy -= cost;
					wep.pendingRerollOpts = null;
					wep.pendingRerollSlot = -1;
					opts[index].level( ench.level() );
					if (wep.enchantment == ench){
						wep.enchantment = opts[index];
					} else {
						wep.enchantment2 = opts[index];
					}
					onWeave();
				}
			}
		} );
	}

	private static Armor.Glyph[] rollOptions( Armor arm ){
		Armor.Glyph[] opts = new Armor.Glyph[3];
		Class<? extends Armor.Glyph> e1 = arm.glyph != null ? arm.glyph.getClass() : null;
		Class<? extends Armor.Glyph> e2 = arm.glyph2 != null ? arm.glyph2.getClass() : null;
		opts[0] = Armor.Glyph.randomCommon( e1, e2 );
		opts[1] = Armor.Glyph.randomUncommon( e1, e2 );
		opts[2] = Armor.Glyph.random( e1, e2, opts[0].getClass(), opts[1].getClass() );
		return opts;
	}

	private static boolean optsValid( Armor arm, Armor.Glyph[] opts ){
		if (opts == null || opts.length != 3) return false;
		for (Armor.Glyph o : opts){
			if (o == null) return false;
			if (arm.glyph  != null && o.getClass() == arm.glyph.getClass())  return false;
			if (arm.glyph2 != null && o.getClass() == arm.glyph2.getClass()) return false;
		}
		return true;
	}

	private void chooseInfusion( final Armor arm ){

		if (!optsValid( arm, arm.pendingWeaveOpts )){
			arm.pendingWeaveOpts = rollOptions( arm );
			saveAll();
		}
		final Armor.Glyph[] opts = arm.pendingWeaveOpts;

		addToFront( new WndOptions(
				new ItemSprite( arm ),
				Messages.titleCase( arm.name() ),
				Messages.get(EnchantingStation.class, "choice_msg"),
				opts[0].name(),
				opts[1].name(),
				opts[2].name(),
				Messages.get(EnchantingStation.class, "cancel") ){

			@Override
			protected void onSelect( int index ) {
				if (index < 3 && Dungeon.energy >= INFUSE_COST){
					Dungeon.energy -= INFUSE_COST;
					arm.pendingWeaveOpts = null;
					arm.addGlyph( opts[index] );
					onWeave();
				}
			}
		} );
	}

	private void chooseReroll( final Armor arm, final Armor.Glyph gl, final int cost ){

		int slot = (gl == arm.glyph) ? 0 : 1;
		if (arm.pendingRerollSlot != slot || !optsValid( arm, arm.pendingRerollOpts )){
			arm.pendingRerollOpts = rollOptions( arm );
			arm.pendingRerollSlot = slot;
			saveAll();
		}
		final Armor.Glyph[] opts = arm.pendingRerollOpts;

		addToFront( new WndOptions(
				new ItemSprite( arm ),
				Messages.titleCase( arm.name() ),
				Messages.get(EnchantingStation.class, "reroll_msg", Messages.get(gl, "glyph")),
				opts[0].name(),
				opts[1].name(),
				opts[2].name(),
				Messages.get(EnchantingStation.class, "cancel") ){

			@Override
			protected void onSelect( int index ) {
				if (index < 3 && Dungeon.energy >= cost){
					Dungeon.energy -= cost;
					arm.pendingRerollOpts = null;
					arm.pendingRerollSlot = -1;
					opts[index].level( gl.level() );
					if (arm.glyph == gl){
						arm.glyph = opts[index];
					} else {
						arm.glyph2 = opts[index];
					}
					if (arm.checkSeal() != null) arm.checkSeal().setGlyph( arm.glyph );
					onWeave();
				}
			}
		} );
	}

	//shared success path: vfx, sound, and a state refresh
	private void onWeave(){
		bubbleEmitter.start(Speck.factory( Speck.BUBBLE ), 0.01f, 100 );
		sparkEmitter.burst(SparkParticle.FACTORY, 20);
		Sample.INSTANCE.play( Assets.Sounds.READ );
		Item.updateQuickslot();
		saveNeeded = true;
		updateState();
	}

	private void updateEnergyText(){
		energyLeft.text( Messages.get(AlchemyScene.class, "energy") + " " + Dungeon.energy );
		energyLeft.setPos(
				centerW - energyLeft.width()/2,
				energyLeft.top()
		);

		energyIcon.x = energyLeft.left() - energyIcon.width();
		align(energyIcon);

		energyAdd.setPos(energyLeft.right(), energyAdd.top());
		align(energyAdd);
	}

	//called when items are energized while this scene is up
	public void createEnergy(){
		bubbleEmitter.start(Speck.factory( Speck.BUBBLE ), 0.01f, 100 );
		sparkEmitter.burst(SparkParticle.FACTORY, 20);
		Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );

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
			WarpedPixelDungeon.reportException(e);
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

	private final WndBag.ItemSelector weaponSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(EnchantingStation.class, "prompt");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof Weapon || item instanceof Armor;
		}

		@Override
		public void onSelect(Item item) {
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
					EnchantingScene.this.addToFront(WndBag.getBag( weaponSelector ));
				}

				@Override
				protected boolean onLongClick() {
					if (item != null){
						EnchantingScene.this.addToFront(new WndInfoItem(item));
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
						return Messages.get(EnchantingScene.class, "select");
					}
					return super.hoverText();
				}
			};
			slot.enable(true);
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
