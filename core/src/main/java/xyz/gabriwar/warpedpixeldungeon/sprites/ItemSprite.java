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
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantTrail;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.HeapLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.ItemAura;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.ItemLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterDrops;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Matrix;
import com.watabou.glwrap.Vertexbuffer;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;

import java.nio.Buffer;

public class ItemSprite extends MovieClip {

	public static final int SIZE	= 16;
	
	private static final float DROP_INTERVAL = 0.4f;
	
	public Heap heap;
	
	private Glowing glowing;
	//FIXME: a lot of this emitter functionality isn't very well implemented.
	//right now I want to ship 0.3.0, but should refactor in the future.
	protected Emitter emitter;
	private float phase;
	private boolean glowUp;
	
	private float dropInterval;

	//the amount the sprite is raised from flat when viewed in a raised perspective
	protected float perspectiveRaise    = 5 / 16f; //5 pixels

	//the width and height of the shadow are a percentage of sprite size
	//offset is the number of pixels the shadow is moved down or up (handy for some animations)
	protected boolean renderShadow  = false;
	protected float shadowWidth     = 1f;
	protected float shadowHeight    = 0.25f;
	protected float shadowOffset    = 0.5f;

	/**
	 * Where it is drawn, which says how much of an enchanted item's magic it shows (docs/fx/enchant.md,
	 * "The item itself"): ICON (the default: a catalog, a guide chip, an icon) its glow's profile only,
	 * found from the Glowing it is given; the rest from the item it views (ItemLooks): SLOT (a bag's,
	 * a quickslot's, a pedestal's slot) and PANEL (the info window) with a backlight and motes round
	 * it, HEAP (on the floor) with its motes and light in the world, FLIGHT (thrown or shot) with its
	 * trail.
	 */
	public enum Mode { ICON, SLOT, PANEL, HEAP, FLIGHT }

	private Mode mode = Mode.ICON;
	//the item it views (null for a bare image), what it shows of its magic, where its own clock starts
	private Item item;
	private final ItemLooks.Set looks = new ItemLooks.Set();
	//what an icon shows: the look of the very Glowing it was given
	private final ItemLooks.Set iconLooks = new ItemLooks.Set();
	private float lookPhase;
	//whether a look tints it now; the tint worked out (rm..ba, the backlight's colour, the second look's)
	private boolean profiled;
	private final float[] tint = new float[13];
	private float backlight;
	private int backColor;
	//how far a look's item twitches off its place now, whole px
	private final float[] twitch = new float[2];
	//a slot's or the info window's motes (made once it first shows a look), whether its slot shows
	//them now and whether it is served first from their pool
	private ItemAura aura;
	private boolean auraOn = true, auraFirst;
	//a heap's motes, backlight, light and first sight, in the light layer; the last item it showed
	//the hero as itself (in a slot, the info window, a throw), whose landing is then no discovery
	private HeapLook heapLook;
	private Item knownItem;
	//a flight's trail: wanted from where to where, made on the render thread, which flight it is;
	//the item's alpha inside a wall
	private EnchantTrail trail;
	private boolean trailWanted;
	private float flyFx, flyFy, flyTx, flyTy;
	private int flights;
	private float phased = 1f;
	
	public ItemSprite() {
		this( ItemSpriteSheet.SOMETHING, null );
	}
	
	public ItemSprite( Heap heap ){
		super(Assets.Sprites.ITEMS);
		view( heap );
	}
	
	public ItemSprite( Item item ) {
		super(Assets.Sprites.ITEMS);
		view( item );
	}
	
	public ItemSprite( int image ){
		this( image, null );
	}
	
	public ItemSprite( int image, Glowing glowing ) {
		super( Assets.Sprites.ITEMS );
		
		view(image, glowing);
	}
	
	public void link() {
		link(heap);
	}
	
	public void link( Heap heap ) {
		this.heap = heap;
		mode = Mode.HEAP;
		view(heap);
		renderShadow = true;
		visible = heap.seen;
		place(heap.pos);
	}
	
	@Override
	public void revive() {
		super.revive();
		
		speed.set( 0 );
		acc.set( 0 );
		dropInterval = 0;
		
		heap = null;
		mode = Mode.ICON;
		phased = 1f;
		if (emitter != null) {
			emitter.killAndErase();
			emitter = null;
		}
	}

	@Override
	public void copy(Image other) {
		super.copy(other);

		if (other instanceof ItemSprite && ((ItemSprite) other).glowing != null){
			glow(((ItemSprite) other).glowing);
		}

	}

	public void visible(boolean value){
		this.visible = value;
		if (emitter != null && !visible){
			emitter.killAndErase();
			emitter = null;
		}
	}
	
	public PointF worldToCamera( int cell ) {
		final int csize = DungeonTilemap.SIZE;
		
		return new PointF(
				PixelScene.align(Camera.main, ((cell % Dungeon.level.width()) + 0.5f) * csize - width() * 0.5f),
				PixelScene.align(Camera.main, ((cell / Dungeon.level.width()) + 1.0f) * csize - height() - csize * perspectiveRaise)
		);
	}
	
	public void place( int p ) {
		if (Dungeon.level != null) {
			point(worldToCamera(p));
			shadowOffset = 0.5f;
		}
	}
	
	public void drop() {

		if (heap.isEmpty()) {
			return;
		} else if (heap.size() == 1){
			// normally this would happen for any heap, however this is not applied to heaps greater than 1 in size
			// in order to preserve an amusing visual bug/feature that used to trigger for heaps with size > 1
			// where as long as the player continually taps, the heap sails up into the air.
			place(heap.pos);
		}
			
		dropInterval = DROP_INTERVAL;
		
		speed.set( 0, -100 );
		acc.set(0, -speed.y / DROP_INTERVAL * 2);
		
		if (heap != null && heap.seen && heap.peek() instanceof Gold) {
			CellEmitter.center( heap.pos ).burst( Speck.factory( Speck.COIN ), 5 );
			SpatialSound.play( Assets.Sounds.GOLD, heap.pos, 1, Random.Float( 0.9f, 1.1f ) );
		}
	}
	
	public void drop( int from ) {

		if (heap.pos == from) {
			drop();
		} else {
			
			float px = x;
			float py = y;
			drop();
			
			place(from);
	
			speed.offset((px - x) / DROP_INTERVAL, (py - y) / DROP_INTERVAL);
		}
	}

	public ItemSprite view( Item item ){
		Glowing g = item.glowing();
		show(item.image(), g);
		synchronized (this) {
			this.item = item;
			ItemLooks.into( item, g, looks );
			lookPhase = ItemLooks.phase( item );
		}
		Emitter emitter = item.emitter();
		if (emitter != null && parent != null) {
			emitter.pos( this );
			parent.add( emitter );
			this.emitter = emitter;
		}
		return this;
	}
	
	public ItemSprite view( Heap heap ){
		if (heap.size() <= 0 || heap.items == null){
			return view( 0, null );
		}

		switch (heap.type) {
			case HEAP: case FOR_SALE:
				view( heap.peek() ); break;
			case CHEST:
				view( ItemSpriteSheet.CHEST, null ); break;
			case LOCKED_CHEST:
				view( ItemSpriteSheet.LOCKED_CHEST, null ); break;
			case CRYSTAL_CHEST:
				view( ItemSpriteSheet.CRYSTAL_CHEST, null ); break;
			case TOMB:
				view( ItemSpriteSheet.TOMB, null ); break;
			case SKELETON:
				view( ItemSpriteSheet.BONES, null ); break;
			case REMAINS:
				view( ItemSpriteSheet.REMAINS, null ); break;
			default:
				view( 0, null );
		}

		alpha( heap.hidden ? 0.1f : 1f);

		return this;
	}
	
	public ItemSprite view( int image, Glowing glowing ) {
		show( image, glowing );
		synchronized (this) {
			item = null;
			looks.clear();
		}
		return this;
	}

	private void show( int image, Glowing glowing ) {
		if (this.emitter != null) this.emitter.killAndErase();
		emitter = null;
		frame( image );
		glow( glowing );
	}

	/** Where it is drawn (ICON unless told): see Mode. */
	public synchronized ItemSprite mode( Mode mode ){
		this.mode = mode;
		return this;
	}

	public Mode mode(){
		return mode;
	}

	/** The item it views, null for a bare image. */
	public Item item(){
		return item;
	}

	/** What it shows of its magic as it is drawn now (the icon's look in ICON). Render thread. */
	public ItemLooks.Set looks(){
		return mode == Mode.ICON ? iconLooks : looks;
	}

	/** Its own clock: the time, its item's phase on. */
	public float lookTime(){
		return Game.timeTotal + lookPhase;
	}

	/** Its slot's say over its motes, each frame before it updates (ItemSlot): whether they show
	 *  (the slot visible and active) and whether they are served first (equipped, quickslotted,
	 *  selected). An info window's show and are served first unless told. */
	public void auraGate( boolean on, boolean first ){
		auraOn = on;
		auraFirst = first;
	}

	/** Its motes in a slot or the info window, null until it first shows a look there. */
	public ItemAura aura(){
		return aura;
	}

	/** A flight starting from (fx, fy) to (tx, ty), world px (MissileSprite.reset, any thread):
	 *  FLIGHT mode, and its trail made as it next updates. */
	public synchronized void flight( float fx, float fy, float tx, float ty ){
		mode = Mode.FLIGHT;
		flyFx = fx;
		flyFy = fy;
		flyTx = tx;
		flyTy = ty;
		flights++;
		trailWanted = true;
	}

	/** The flight landed (MissileSprite.onComplete): its trail ends with it. */
	public synchronized void endFlight(){
		trailWanted = false;
		if (trail != null) trail.end();
		trail = null;
		phased = 1f;
	}

	/** How many flights it has flown (a trail knows its own by it). */
	public int flights(){
		return flights;
	}

	/** Its alpha's share while it passes through a wall (EnchantTrail), 1 otherwise. */
	public void phased( float share ){
		phased = share;
	}

	public float phased(){
		return phased;
	}

	/** Its flight's trail, null for none (tests). */
	public EnchantTrail trail(){
		return trail;
	}

	/** Whether it is still falling from a drop (it lands, then sounds). */
	public boolean falling(){
		return dropInterval > 0;
	}

	/** A heap's look in the world, null for none (tests). */
	public HeapLook heapLook(){
		return heapLook;
	}

	/** The frame it shows now, not a copy: read it, never change it. */
	public RectF frameRect(){
		return frame;
	}

	/** The backlight's level (ItemLooks.TROUGH..PEAK) and colour as its looks set them this frame. */
	public float backlight(){
		return backlight;
	}

	public int backColor(){
		return backColor;
	}

	public void frame( int image ){
		frame( ItemSpriteSheet.film.get( image ));

		float height = ItemSpriteSheet.film.height( image );
		//adds extra raise to very short items, so they are visible
		if (height < 8f){
			perspectiveRaise =  (5 + 8 - height) / 16f;
		}
	}
	
	public synchronized void glow( Glowing glowing ){
		this.glowing = glowing;
		iconLooks.clear();
		EnchantLook l = EnchantLooks.forGlowing( glowing );
		if (l != null){
			iconLooks.a = l;
			iconLooks.level = 0;
			iconLooks.periodA = glowing.period;
		}
		if (glowing == null) resetColor();
	}

	@Override
	public void kill() {
		super.kill();
		if (emitter != null) {
			emitter.on = false;
			emitter.autoKill = true;
		}
		emitter = null;
	}

	private float[] shadowMatrix = new float[16];

	@Override
	protected void updateMatrix() {
		super.updateMatrix();
		Matrix.copy(matrix, shadowMatrix);
		Matrix.translate(shadowMatrix,
				(width() * (1f - shadowWidth)) / 2f,
				(height() * (1f - shadowHeight)) + shadowOffset);
		Matrix.scale(shadowMatrix, shadowWidth, shadowHeight);
	}

	@Override
	public void destroy() {
		super.destroy();
		//its motes' quads lie outside the scene graph: their GPU buffers go with it
		if (aura != null) aura.destroy();
		aura = null;
	}

	//whether it and every group it lies in are shown (a closed bag's slots are not)
	private boolean shownUp(){
		for (com.watabou.noosa.Gizmo g = this; g != null; g = g.parent){
			if (!g.visible || !g.exists) return false;
		}
		return true;
	}

	private boolean auraShown(){
		return aura != null && (mode == Mode.SLOT || mode == Mode.PANEL);
	}

	@Override
	public void draw() {
		if (texture == null || (!dirty && buffer == null))
			return;

		boolean motes = auraShown();
		if (motes) aura.drawBack( this );

		//a look's twitch (Horror's shiver, Elastic's bob): drawn off its place this frame; faint
		//inside a wall
		float tx = twitch[0], ty = twitch[1], a0 = am;
		x += tx;
		y += ty;
		am *= phased;

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

		x -= tx;
		y -= ty;
		am = a0;

		if (motes) aura.drawFront( this );
	}

	@Override
	public synchronized void update() {
		super.update();

		visible = (heap == null || heap.seen);

		if (emitter != null){
			emitter.visible = visible;
		}

		if (dropInterval > 0){
			shadowOffset -= speed.y * Game.elapsed * 0.8f;

			if ((dropInterval -= Game.elapsed) <= 0){

				speed.set(0);
				acc.set(0);
				shadowOffset = 0.25f;
				place(heap.pos);

				if (visible) {

					//into the water: a drop with its droplets, and the fish come to look
					if (Dungeon.level.water[heap.pos]) {
						PointF c = DungeonTilemap.tileCenterToWorld(heap.pos);
						WaterDrops.item(c.x, c.y + 2);
					}

					if (Dungeon.level.water[heap.pos]) {
						SpatialSound.play( Assets.Sounds.WATER, heap.pos, 0.8f, Random.Float( 1f, 1.45f ) );
					} else if (Dungeon.level.map[heap.pos] == Terrain.EMPTY_SP) {
						SpatialSound.play( Assets.Sounds.STURDY, heap.pos, 0.8f, Random.Float( 1.16f, 1.25f ) );
					} else if (Dungeon.level.map[heap.pos] == Terrain.GRASS
							|| Dungeon.level.map[heap.pos] == Terrain.EMBERS
							|| Dungeon.level.map[heap.pos] == Terrain.FURROWED_GRASS){
						SpatialSound.play( Assets.Sounds.GRASS, heap.pos, 0.8f, Random.Float( 1.16f, 1.25f ) );
					} else if (Dungeon.level.map[heap.pos] == Terrain.HIGH_GRASS) {
						SpatialSound.play( Assets.Sounds.STEP, heap.pos, 0.8f, Random.Float( 1.16f, 1.25f ) );
					} else {
						SpatialSound.play( Assets.Sounds.STEP, heap.pos, 0.8f, Random.Float( 1.16f, 1.25f ));
					}
				}
			}
		}

		ItemLooks.Set shown = looks();
		if (visible && shown.any()) {
			//an enchantment's, a glyph's or a curse's look: its profile, two of them crossfading
			float t = lookTime();
			backlight = ItemLooks.tint( shown, t, tint );
			backColor = (int)tint[6];
			rm = tint[0];
			gm = tint[1];
			bm = tint[2];
			ra = tint[3];
			ga = tint[4];
			ba = tint[5];
			profiled = true;
			ItemLooks.twitch( shown.b != null && ItemLooks.crossB( t ) > 0.5f ? shown.b : shown.a, t, twitch );
		} else if (profiled) {
			profiled = false;
			twitch[0] = twitch[1] = 0;
			if (glowing == null) resetColor();
		}

		//an enchanted item seen as itself is no discovery where it lands
		if (mode != Mode.ICON && mode != Mode.HEAP && item != knownItem && looks.a != null){
			knownItem = item;
			HeapLook.known( item );
		}

		//a slot's or the info window's motes
		if (mode == Mode.SLOT || mode == Mode.PANEL){
			//a plain item's rarity line hint shows there too (ItemAura.hintOf)
			if (aura == null && (looks.any() || (item != null && ItemAura.hintOf( item ) >= 0
					&& ItemAura.hinted( item, mode == Mode.PANEL )))) aura = new ItemAura( mode == Mode.PANEL );
			if (aura != null) aura.update( this, auraOn && shownUp(), auraFirst || mode == Mode.PANEL );
		} else if (aura != null){
			aura.update( this, false, false );
		}
		//a flight's trail (it steers itself from here on)
		if (mode == Mode.FLIGHT && trailWanted){
			trailWanted = false;
			trail = EnchantTrail.attach( this, flyFx, flyFy, flyTx, flyTy );
		}
		//a heap's look in the world (it steers itself from here on)
		if (mode == Mode.HEAP && looks.a != null && (heapLook == null || !heapLook.owns( this ))){
			heapLook = HeapLook.attach( this );
		}

		if (visible && glowing != null && !profiled) {
			if (glowUp && (phase += Game.elapsed) > glowing.period) {
				
				glowUp = false;
				phase = glowing.period;
				
			} else if (!glowUp && (phase -= Game.elapsed) < 0) {
				
				glowUp = true;
				phase = 0;
				
			}
			
			float value = phase / glowing.period * 0.6f;
			
			rm = gm = bm = 1 - value;
			ra = glowing.red * value;
			ga = glowing.green * value;
			ba = glowing.blue * value;
		}
	}

	public static int pick( int index, int x, int y ) {
		SmartTexture tx = TextureCache.get( Assets.Sprites.ITEMS );
		int rows = tx.width / SIZE;
		int row = index / rows;
		int col = index % rows;
		return tx.getPixel( col * SIZE + x, row * SIZE + y );
	}
	
	public static class Glowing {
		
		public int color;
		public float red;
		public float green;
		public float blue;
		public float period;
		
		public Glowing( int color ) {
			this( color, 1f );
		}
		
		public Glowing( int color, float period ) {

			this.color = color;

			red = (color >> 16) / 255f;
			green = ((color >> 8) & 0xFF) / 255f;
			blue = (color & 0xFF) / 255f;
			
			this.period = period;
		}

		/** the glow as a colour words can be read in on a dark window: a curse's black
		 *  glow becomes a dark red */
		public int textColor(){
			int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
			return Math.max( r, Math.max( g, b ) ) < 0x50 ? 0xB0405A : color & 0xFFFFFF;
		}
	}
}
