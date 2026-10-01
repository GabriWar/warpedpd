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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Rect;

public class ItemSlot extends Button {

	public static final int DEGRADED	= 0xFF4444;
	public static final int UPGRADED	= 0x44FF44;
	public static final int ENCHANTED	= 0x55AAFF;
	public static final int FADED       = 0x999999;
	public static final int WARNING		= 0xFF8800;
	public static final int ENHANCED	= 0x3399FF;
	public static final int MASTERED	= 0xFFFF44;
	public static final int CURSE_INFUSED	= 0x8800FF;
	
	private static final float ENABLED	= 1.0f;
	private static final float DISABLED	= 0.3f;

	private Rect margin = new Rect();

	protected ItemSprite sprite;
	protected Item       item;
	protected BitmapText status;
	protected BitmapText extra;
	protected Image      itemIcon;
	protected BitmapText level;
	
	private static final String TXT_STRENGTH	= ":%d";
	private static final String TXT_TYPICAL_STR	= "%d?";

	private static final String TXT_LEVEL	= "%+d";

	// Special "virtual items"
	public static final Item CHEST = new Item() {
		public int image() { return ItemSpriteSheet.CHEST; }
		public String name() { return Messages.get(Heap.class, "chest"); }
	};
	public static final Item LOCKED_CHEST = new Item() {
		public int image() { return ItemSpriteSheet.LOCKED_CHEST; }
		public String name() { return Messages.get(Heap.class, "locked_chest"); }
	};
	public static final Item CRYSTAL_CHEST = new Item() {
		public int image() { return ItemSpriteSheet.CRYSTAL_CHEST; }
		public String name() { return Messages.get(Heap.class, "crystal_chest"); }
	};
	public static final Item TOMB = new Item() {
		public int image() { return ItemSpriteSheet.TOMB; }
		public String name() { return Messages.get(Heap.class, "tomb"); }
	};
	public static final Item SKELETON = new Item() {
		public int image() { return ItemSpriteSheet.BONES; }
		public String name() { return Messages.get(Heap.class, "skeleton"); }
	};
	public static final Item REMAINS = new Item() {
		public int image() { return ItemSpriteSheet.REMAINS; }
		public String name() { return Messages.get(Heap.class, "remains"); }
	};
	
	public ItemSlot() {
		super();
		sprite.visible(false);
		enable(false);
	}
	
	public ItemSlot( Item item ) {
		this();
		item( item );
	}
		
	@Override
	protected void createChildren() {
		
		super.createChildren();
		
		sprite = new ItemSprite();
		add(sprite);
		
		status = new BitmapText( PixelScene.pixelFont);
		add(status);
		
		extra = new BitmapText( PixelScene.pixelFont);
		add(extra);
		
		level = new BitmapText( PixelScene.pixelFont);
		add(level);

		enchLvl = new BitmapText( PixelScene.pixelFont);
		add(enchLvl);

		qualityFrame = new com.watabou.noosa.ColorBlock[4];
		for (int i = 0; i < 4; i++){
			qualityFrame[i] = new com.watabou.noosa.ColorBlock( 1, 1, 0xFFFFFFFF );
			qualityFrame[i].visible = false;
			add( qualityFrame[i] );
		}
		//three motes circle the frame at different speeds, each with a two-step fading tail
		qualitySparks = new com.watabou.noosa.ColorBlock[SPARKS * 3];
		for (int i = 0; i < qualitySparks.length; i++){
			qualitySparks[i] = new com.watabou.noosa.ColorBlock( 1, 1, 0xFFFFFFFF );
			qualitySparks[i].visible = false;
			add( qualitySparks[i] );
		}
		typeBadge = new Image( Assets.Sprites.RARITY_ICONS );
		typeBadge.scale.set( 0.5f );
		typeBadge.visible = false;
		add( typeBadge );
	}

	//rarity: a 1px frame in the tier colour at rare and above (white-gold when masterworked);
	//type: the gamma/beta/alpha letter at half size, centred on the bottom edge, clear of the
	//charges (top-left), strength (top-right), level (bottom-right) and enchant level (bottom-left)
	private com.watabou.noosa.ColorBlock[] qualityFrame;
	private Image typeBadge;

	private static final int SPARKS = 5;   //the most any tier gets; rarer items light more of them
	private static final float FRAME_ALPHA = 0.35f;
	private com.watabou.noosa.ColorBlock[] qualitySparks;
	private final float[] sparkPos = new float[SPARKS];
	private final float[] sparkSpeed = new float[SPARKS];   //px per second, negative = counter-clockwise
	private int sparkCount = 0;
	private float frameL, frameT, frameW, frameH;
	private boolean sparksOn = false;

	private void updateQuality(){
		xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality q = item == null ? null : item.quality;
		boolean frame = q != null && (q.rarity.ordinal() >= xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity.RARE.ordinal() || q.fullyMasterworked());
		for (com.watabou.noosa.ColorBlock b : qualityFrame){
			b.visible = frame;
			if (frame){
				b.hardlight( q.titleColor() );
				b.alpha( FRAME_ALPHA );
			}
		}
		sparksOn = frame;
		//rare 2, legendary 3, exotic 4, a full masterwork 5; each mote rolls its own start,
		//speed and direction so no two slots pulse alike
		sparkCount = !frame ? 0 : q.fullyMasterworked() ? 5 : 2 + (q.rarity.ordinal() - xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity.RARE.ordinal());
		for (int i = 0; i < SPARKS; i++){
			boolean on = frame && i < sparkCount;
			for (int k = 0; k < 3; k++){
				qualitySparks[i * 3 + k].visible = on;
				if (on) qualitySparks[i * 3 + k].hardlight( q.titleColor() );
			}
			if (on){
				sparkPos[i] = com.watabou.utils.Random.Float( 80f );
				sparkSpeed[i] = com.watabou.utils.Random.Float( 6f, 16f ) * (com.watabou.utils.Random.Int( 2 ) == 0 ? 1f : -1f);
			}
		}
		typeBadge.visible = q != null;
		if (q != null){
			typeBadge.frame( q.type.icon() * 16, 0, 16, 16 );
		}
		layoutQuality();
	}

	//the motes run clockwise around the frame's perimeter; the head is solid, the two
	//tail steps fade behind it, so the frame reads as a slow current rather than a box
	private void flowSparks(){
		float per = 2 * (frameW + frameH - 2);
		for (int i = 0; i < sparkCount; i++){
			sparkPos[i] = ((sparkPos[i] + sparkSpeed[i] * Game.elapsed) % per + per) % per;
			float dir = Math.signum( sparkSpeed[i] );
			for (int k = 0; k < 3; k++){
				com.watabou.noosa.ColorBlock b = qualitySparks[i * 3 + k];
				//the tail trails behind whichever way the mote is going
				float p = ((sparkPos[i] - k * dir) % per + per) % per;
				placeOnFrame( b, p );
				b.alpha( k == 0 ? 1f : k == 1 ? 0.6f : 0.3f );
			}
		}
	}

	//maps a distance along the perimeter (clockwise from the top-left corner) to a pixel
	private void placeOnFrame( com.watabou.noosa.ColorBlock b, float p ){
		float w = frameW - 1, h = frameH - 1;
		float px, py;
		if (p < w){            px = frameL + p;          py = frameT; }
		else if (p < w + h){   px = frameL + w;          py = frameT + (p - w); }
		else if (p < 2*w + h){ px = frameL + w - (p - w - h); py = frameT + h; }
		else {                 px = frameL;              py = frameT + h - (p - 2*w - h); }
		b.x = (float) Math.floor( px );
		b.y = (float) Math.floor( py );
	}

	private void layoutQuality(){
		if (qualityFrame == null) return;
		//the frame is drawn inside the slot, along its inner edge
		float l = x + margin.left, t = y + margin.top;
		float w = width - margin.left - margin.right, h = height - margin.top - margin.bottom;
		frameL = l; frameT = t; frameW = w; frameH = h;
		qualityFrame[0].size( w, 1 ); qualityFrame[0].x = l;         qualityFrame[0].y = t;
		qualityFrame[1].size( w, 1 ); qualityFrame[1].x = l;         qualityFrame[1].y = t + h - 1;
		qualityFrame[2].size( 1, h ); qualityFrame[2].x = l;         qualityFrame[2].y = t;
		qualityFrame[3].size( 1, h ); qualityFrame[3].x = l + w - 1; qualityFrame[3].y = t;
		typeBadge.x = x + margin.left + (w - typeBadge.width()) / 2f;
		typeBadge.y = y + height - margin.bottom - typeBadge.height();
		PixelScene.align( typeBadge );
		for (com.watabou.noosa.ColorBlock b : qualityFrame){ PixelScene.align( b ); sendToBack( b ); }
		for (com.watabou.noosa.ColorBlock b : qualitySparks) bringToFront( b );
		bringToFront( typeBadge );
		if (status != null) bringToFront( status );
	}

	//enchantment empowerment levels, shown in blue and cycling between the
	//weapon's two enchantments when both are empowered
	protected BitmapText enchLvl;
	private int[] enchLvls = new int[0];
	private int enchIdx = 0;
	private float enchCycle = 0;
	private com.watabou.noosa.particles.Emitter equipmentSparkles;
	private int[] sparkleColors = new int[0];

	private static void addSparkleColor(java.util.ArrayList<Integer> colors, int color, int level) {
		// +0: one spark/4s; +1: one/2s; +2: 1.5/s; +5: 7.5/s, per effect.
		int weight = Math.max(1, level * (level + 1));
		for (int i=0; i<weight; i++) colors.add(color);
	}

	private void updateSparkles() {
		java.util.ArrayList<Integer> colors = new java.util.ArrayList<>();
		if (item instanceof Weapon) {
			for (Weapon.Enchantment enchantment : ((Weapon)item).enchantments())
				if (item.cursedKnown || !enchantment.curse()) addSparkleColor(colors, enchantment.glowing().color, enchantment.level());
		} else if (item instanceof Armor) {
			for (Armor.Glyph glyph : ((Armor)item).glyphs())
				if (item.cursedKnown || !glyph.curse()) addSparkleColor(colors, glyph.glowing().color, glyph.level());
		}
		int[] next = new int[colors.size()];
		for (int i=0; i<next.length; i++) next[i] = colors.get(i);
		if (java.util.Arrays.equals(next, sparkleColors)) return;
		sparkleColors = next;
		if (equipmentSparkles != null) {
			equipmentSparkles.killAndErase(); equipmentSparkles.destroy(); equipmentSparkles = null;
		}
		if (next.length > 0) {
			equipmentSparkles = new com.watabou.noosa.particles.Emitter();
			// Above the sprite, below quantity and level text; preserve any existing item emitter.
			add(equipmentSparkles);
			bringToFront(status); bringToFront(extra); bringToFront(level); bringToFront(enchLvl);
			equipmentSparkles.pos(sprite);
			equipmentSparkles.pour(xyz.gabriwar.warpedpixeldungeon.effects.particles.EquipmentSparkle.factory(next), 4f / next.length);
		}
	}
	
	@Override
	protected void layout() {
		super.layout();
		
		sprite.x = x + margin.left + (width - sprite.width - (margin.left + margin.right)) / 2f;
		sprite.y = y + margin.top + (height - sprite.height - (margin.top + margin.bottom)) / 2f;
		PixelScene.align(sprite);
		
		if (status != null) {
			status.measure();
			if (status.width > width - (margin.left + margin.right)){
				status.scale.set(PixelScene.align(0.8f));
			} else {
				status.scale.set(1f);
			}
			status.x = x + margin.left;
			status.y = y + margin.top;
			PixelScene.align(status);
		}
		
		if (extra != null) {
			extra.x = x + (width - extra.width()) - margin.right;
			extra.y = y + margin.top;
			PixelScene.align(extra);

			if ((status.width() + extra.width()) > width){
				extra.visible = false;
			} else if (item != null) {
				extra.visible = true;
			}
		}

		if (itemIcon != null){
			//center the icon slightly if there is enough room
			if (width >= 24 || height >= 24) {
				itemIcon.x = x + width - (ItemSpriteSheet.Icons.SIZE + itemIcon.width()) / 2f - margin.right;
				itemIcon.y = y + (ItemSpriteSheet.Icons.SIZE - itemIcon.height) / 2f + margin.top;
			} else {
				itemIcon.x = x + width - itemIcon.width() - margin.right;
				itemIcon.y = y + margin.top;
			}
			PixelScene.align(itemIcon);
		}
		
		if (level != null) {
			level.x = x + (width - level.width()) - margin.right;
			level.y = y + (height - level.baseLine() - 1) - margin.bottom;
			PixelScene.align(level);
		}

		if (enchLvl != null && enchLvl.text() != null) {
			enchLvl.measure();
			if (status != null && status.text() != null && !status.text().isEmpty()){
				//stacks show their quantity in the top-left, so sit at the bottom-left
				enchLvl.x = x + margin.left;
				enchLvl.y = y + (height - enchLvl.baseLine() - 1) - margin.bottom;
			} else {
				enchLvl.x = x + margin.left;
				enchLvl.y = y + margin.top;
			}
			PixelScene.align(enchLvl);
		}

	
		//last, so the quality frame and plate can read where the texts ended up
		layoutQuality();
	}

	public void alpha( float value ){
		if (!active) value *= 0.3f;
		if (sprite != null)     sprite.alpha(value);
		if (extra != null)      extra.alpha(value);
		if (status != null)     status.alpha(value);
		if (itemIcon != null)   itemIcon.alpha(value);
		if (level != null)      level.alpha(value);
		if (enchLvl != null)    enchLvl.alpha(value);
	}

	private void refreshEnchText(){
		if (enchLvl == null) return;
		if (enchLvls.length == 0){
			enchLvl.text(null);
		} else {
			enchLvl.text( Messages.format( TXT_LEVEL, enchLvls[enchIdx % enchLvls.length] ) );
			enchLvl.measure();
			enchLvl.hardlight( ENCHANTED );
		}
		layout();
	}

	@Override
	public void update() {
		if (equipmentSparkles != null) {
			equipmentSparkles.visible = equipmentSparkles.on = visible && active && sprite.visible;
			equipmentSparkles.pos(sprite);
		}
		super.update();
		if (sparksOn && qualitySparks != null && frameW > 2 && frameH > 2) flowSparks();
		//with two empowered enchantments the blue number cycles between them
		if (enchLvls.length > 1){
			enchCycle += Game.elapsed;
			if (enchCycle >= 1f){
				enchCycle = 0;
				enchIdx = (enchIdx + 1) % enchLvls.length;
				refreshEnchText();
			}
		}
	}

	public void clear(){
		item(null);
		enable(true);
		sprite.visible(true);
		sprite.view(ItemSpriteSheet.SOMETHING, null);
		layout();
	}
	
	public Item item() { return item; }

	public void item( Item item ) {
		if (this.item == item) {
			if (item != null) {
				sprite.view( item );
			}
			updateText();
			return;
		}

		this.item = item;

		if (item == null) {

			enable(false);
			sprite.visible(false);

			updateText();
			
		} else {
			
			enable(true);
			sprite.visible(true);

			sprite.view( item );
			updateText();
		}
		updateQuality();
	}

	public void updateText(){
		updateSparkles();

		if (itemIcon != null){
			remove(itemIcon);
			itemIcon = null;
		}

		if (item == null){
			status.visible = extra.visible = level.visible = false;
			if (enchLvl != null) enchLvl.text(null);
			return;
		} else {
			status.visible = extra.visible = level.visible = true;
		}

		//collect visible enchantment/glyph empowerment levels for the blue indicator
		enchLvls = new int[0];
		if (item instanceof Weapon){
			Weapon w = (Weapon) item;
			int n = 0;
			int[] tmp = new int[2];
			for (Weapon.Enchantment e : w.enchantments()){
				if (e.level() > 0 && (item.cursedKnown || !e.curse())){
					tmp[n++] = e.level();
				}
			}
			enchLvls = new int[n];
			System.arraycopy(tmp, 0, enchLvls, 0, n);
		} else if (item instanceof Armor){
			Armor a = (Armor) item;
			int n = 0;
			int[] tmp = new int[2];
			for (Armor.Glyph g : a.glyphs()){
				if (g.level() > 0 && (item.cursedKnown || !g.curse())){
					tmp[n++] = g.level();
				}
			}
			enchLvls = new int[n];
			System.arraycopy(tmp, 0, enchLvls, 0, n);
		}
		enchIdx = 0;
		enchCycle = 0;
		refreshEnchText();

		status.text( item.status() );

		//thrown weapons on their last use show quantity in orange, unless they are single-use
		if (item instanceof MissileWeapon
				&& ((MissileWeapon) item).durabilityLeft() <= 50f
				&& ((MissileWeapon) item).durabilityLeft() <= ((MissileWeapon) item).durabilityPerUse()){
			status.hardlight(WARNING);
		} else {
			status.resetColor();
		}

		if (item.icon != -1 && (item.isIdentified() || (item instanceof Ring && ((Ring) item).isKnown()))){
			extra.text( null );

			itemIcon = new Image(Assets.Sprites.ITEM_ICONS);
			itemIcon.frame(ItemSpriteSheet.Icons.film.get(item.icon));
			add(itemIcon);

		} else if (item instanceof Weapon || item instanceof Armor) {

			if (item.levelKnown){
				int str = item instanceof Weapon ? ((Weapon)item).STRReq() : ((Armor)item).STRReq();
				extra.text( Messages.format( TXT_STRENGTH, str ) );
				if (Dungeon.hero != null && str > Dungeon.hero.STR()) {
					extra.hardlight( DEGRADED );
				} else if (item instanceof Weapon && ((Weapon) item).masteryPotionBonus){
					extra.hardlight( MASTERED );
				} else if (item instanceof Armor && ((Armor) item).masteryPotionBonus) {
					extra.hardlight( MASTERED );
				} else {
					extra.resetColor();
				}
			} else {
				int str = item instanceof Weapon ? ((Weapon)item).STRReq(0) : ((Armor)item).STRReq(0);
				extra.text( Messages.format( TXT_TYPICAL_STR, str ) );
				extra.hardlight( WARNING );
			}
			extra.measure();

		} else {

			extra.text( null );

		}

		int trueLvl = item.visiblyUpgraded();
		int buffedLvl = item.buffedVisiblyUpgraded();

		if (trueLvl != 0 || buffedLvl != 0) {
			level.text( Messages.format( TXT_LEVEL, buffedLvl ) );
			level.measure();
			if (trueLvl == buffedLvl || buffedLvl <= 0) {
				if (buffedLvl > 0){
					if ((item instanceof Weapon && ((Weapon) item).curseInfusionBonus)
						|| (item instanceof Armor && ((Armor) item).curseInfusionBonus)
							|| (item instanceof Wand && ((Wand) item).curseInfusionBonus)){
						level.hardlight(CURSE_INFUSED);
					} else {
						level.hardlight(UPGRADED);
					}
				} else {
					level.hardlight( DEGRADED );
				}
			} else {
				level.hardlight(buffedLvl > trueLvl ? ENHANCED : WARNING);
			}
		} else {
			level.text( null );
		}

		layout();
	}
	
	public void enable( boolean value ) {
		
		active = value;
		//reset properties if was pressed
		if (!active && pressedButton == this){
			hotArea.reset();
			pressedButton = null;
			clickReady = false;
			onPointerUp();
		}
		
		float alpha = value ? ENABLED : DISABLED;
		sprite.alpha( alpha );
		status.alpha( alpha );
		extra.alpha( alpha );
		level.alpha( alpha );
		if (itemIcon != null) itemIcon.alpha( alpha );
	}

	public void showExtraInfo( boolean show ){

		if (show){
			add(extra);
		} else {
			remove(extra);
		}

	}

	public void textVisible( boolean visible ){
		if (visible){
			add(status);
			add(extra);
			add(level);
		} else {
			remove(status);
			remove(extra);
			remove(level);
		}
	}

	public void setMargins( int left, int top, int right, int bottom){
		margin.set(left, top, right, bottom);
		layout();
	}

	@Override
	protected String hoverText() {
		if (item != null && item.name() != null) {
			return Messages.titleCase(item.name());
		} else {
			return super.hoverText();
		}
	}
}
