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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.Surprise;
import xyz.gabriwar.warpedpixeldungeon.effects.TargetedCell;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.GuideEffect;
import xyz.gabriwar.warpedpixeldungeon.journal.guidedemos.Demo;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuidePuppet;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.TerrainFeaturesTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIcon;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.RenderedText;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Tilemap;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

//A guide page's scene (journal/guidedemos/Demo) on its little stage: the region's own tiles in the
//layers a level draws them in (water, ground, dressing, then the creatures, then the raised grass
//and the walls over them), the creatures' own sprites playing their own animations and sending their
//bolts, throws, rays, lightning and chains (DemoShot), the game's own particles, and over it all the
//marks, arrows, icons and numbers that tell what is going on. Its caption sits under it. It plays
//its timeline and starts over, for as long as it is shown.
//
//Everything it shows is made with it, the timeline's numbers and icons too: playing only moves,
//shows and hides them, so it allocates nothing as it runs. Every position is a whole pixel.
public class DemoStage extends Component {

	public static final int CELL = DungeonTilemap.SIZE;
	//between the stage and its caption
	private static final int GAP = 3;
	//a walk's step and a knockback's cell, seconds; a jump's, per cell
	private static final float STEP = 0.15f, THROW = 0.08f, LEAP = 0.12f;
	//a death's fade: after its animation, over this long
	private static final float DIE_AFTER = 0.4f, DIE_FADE = 0.6f;
	//a number or a word: rises this far over its life (FloatingText's life)
	private static final int RISE = 8;
	private static final float RISE_FOR = 1f;
	//the marks' colours: the game's telegraph red, a safe green, the window's yellow
	private static final int RED = TargetedCell.TELEGRAPH, GREEN = 0x40E060, YELLOW = Window.TITLE_COLOR;

	private final Demo demo;
	private final List<Demo.Step> steps;
	private final int cols, rows;
	private final int frameColor;
	private final int[] start, map;
	private final byte[] variance;

	private final ColorBlock back;
	private final ColorBlock[] edges = new ColorBlock[4];
	private final Water water;
	private final Layer ground, dressing, raised, walls;
	private final Group floor = new Group(), lying = new Group(), standing = new Group();
	private final Group reticles = new Group(), fx = new Group(), arrows = new Group(), heads = new Group(), texts = new Group();

	private final ArrayList<Puppet> puppets = new ArrayList<>();
	private final LinkedHashMap<String, Puppet> puppetById = new LinkedHashMap<>();
	private final LinkedHashMap<String, Image> propById = new LinkedHashMap<>();
	private final CellMark[] marks;
	//what each step shows, made with the stage (by the step's index)
	private final Object[] made;
	private final LinkedHashMap<String, Words> captions = new LinkedHashMap<>();
	private final int captionH;

	private float time;
	private int next;
	private float pulse;
	private boolean paused;
	private Words caption;
	private boolean tilesDirty;
	//where the stage's first cell sits, set by layout
	private int ox, oy;

	public DemoStage( Demo demo, int accent, int width ){
		super();
		this.demo = demo;
		steps = demo.steps();
		cols = demo.width();
		rows = demo.height();
		frameColor = CiteChip.darker( accent, 0.35f );
		start = demo.map();
		map = start.clone();
		variance = variance( demo );
		this.width = width;

		back = new ColorBlock( cols * CELL, rows * CELL, 0xFF000000 );
		add( back );
		for (int i = 0; i < edges.length; i++){
			edges[i] = new ColorBlock( 1, 1, 0xFFFFFFFF );
			edges[i].hardlight( frameColor );
			add( edges[i] );
		}

		Demo.Region region = demo.region();
		water = hasWater() ? new Water( cols * CELL, rows * CELL, region.water ) : null;
		if (water != null) add( water );
		ground = new Layer( region.tiles ){
			@Override
			protected boolean needsRender( int pos ) {
				//the water's own visual is the water under it
				return super.needsRender( pos ) && data[pos] != DungeonTileSheet.WATER;
			}
		};
		add( ground );
		dressing = new Layer( Assets.Environment.TERRAIN_FEATURES );
		add( dressing );
		add( floor );
		add( lying );
		add( standing );
		raised = new Layer( region.raised );
		add( raised );
		walls = new Layer( region.tiles );
		add( walls );
		add( reticles );
		add( fx );
		add( arrows );
		add( heads );
		add( texts );
		paintTiles();

		for (Demo.Actor a : demo.actors()){
			Puppet p = new Puppet( a );
			puppets.add( p );
			puppetById.put( a.id, p );
			if (p.sprite != null) standing.add( p.sprite );
		}
		for (Demo.Prop p : demo.props()){
			Image img = prop( p );
			propById.put( p.id, img );
			lying.add( img );
		}

		marks = new CellMark[cols * rows];
		made = new Object[steps.size()];
		int capH = 0;
		for (int i = 0; i < made.length; i++){
			made[i] = make( steps.get( i ) );
			Demo.Step s = steps.get( i );
			if (s.op == Demo.Op.CAPTION && s.text != null && !captions.containsKey( s.text )){
				Words w = words( demo.words( s.text ), 6 );
				captions.put( s.text, w );
				if (w != null){
					w.setHightlighting( false );
					w.hardlight( CiteChip.lighter( accent, 0.55f ) );
					w.maxWidth( width );
					w.align( RenderedTextBlock.CENTER_ALIGN );
					w.visible = false;
					add( w );
					capH = Math.max( capH, (int)Math.ceil( w.height() ) );
				}
			}
		}
		captionH = capH;
		height = frameH() + (captionH > 0 ? GAP + captionH : 0);

		restart();
	}

	/** The words it draws (captions, numbers): a test, which has no fonts, overrides it to draw none. */
	protected Words words( String text, int size ){
		return new Words( text, size );
	}

	private int frameW(){ return cols * CELL + 2; }
	private int frameH(){ return rows * CELL + 2; }

	/** Starts from the beginning. */
	public void restart(){
		time = 0;
		next = 0;
		pulse = 0;
		System.arraycopy( start, 0, map, 0, map.length );
		if (tilesDirty) paintTiles();

		for (Puppet p : puppets) p.reset();
		order();
		for (Image img : propById.values()) showProp( img, true );
		for (int c = 0; c < marks.length; c++) if (marks[c] != null) marks[c].set( null );
		for (int i = 0; i < made.length; i++) resetMade( made[i] );
		showCaption( null );
		layout();

		//what comes before the first turn is how the stage starts
		while (next < steps.size() && steps.get( next ).at <= 0) run( next++ );
	}

	/** How far into its timeline it is, seconds. */
	public float played(){
		return time;
	}

	/** A tap: it holds still, or plays on. */
	public void tap(){
		paused = !paused;
		int c = paused ? CiteChip.lighter( frameColor, 0.45f ) : frameColor;
		for (ColorBlock e : edges) e.hardlight( c );
	}

	@Override
	public void update() {
		if (paused) return;
		super.update();
		float dt = Game.elapsed;
		time += dt;
		pulse += dt;
		while (next < steps.size() && steps.get( next ).at <= time) run( next++ );
		//indexed: a frame allocates nothing
		for (int i = 0; i < puppets.size(); i++) puppets.get( i ).update( dt );
		for (Object m : made) advance( m, dt );
		for (CellMark m : marks) if (m != null) m.update();
		if (water != null) water.flow( dt );
		if (time >= demo.length() + Demo.REST) restart();
	}

	@Override
	protected void layout() {
		int fx0 = (int)x + ((int)width - frameW()) / 2, fy0 = (int)y;
		ox = fx0 + 1;
		oy = fy0 + 1;
		int w = frameW(), h = frameH();
		edges[0].x = fx0;         edges[0].y = fy0;         edges[0].size( w, 1 );
		edges[1].x = fx0;         edges[1].y = fy0 + h - 1; edges[1].size( w, 1 );
		edges[2].x = fx0;         edges[2].y = fy0 + 1;     edges[2].size( 1, h - 2 );
		edges[3].x = fx0 + w - 1; edges[3].y = fy0 + 1;     edges[3].size( 1, h - 2 );
		back.x = ox;
		back.y = oy;
		if (water != null){
			water.x = ox;
			water.y = oy;
		}
		ground.x = dressing.x = raised.x = walls.x = ox;
		ground.y = dressing.y = raised.y = walls.y = oy;
		for (Puppet p : puppets) p.place();
		for (Demo.Prop p : demo.props()) lay( p, propById.get( p.id ) );
		for (int c = 0; c < marks.length; c++) if (marks[c] != null) marks[c].place( c );
		for (Object m : made) placeMade( m );
		for (Words c : captions.values()){
			if (c == null) continue;
			c.setPos( (int)x + ((int)width - (int)Math.ceil( c.width() )) / 2, fy0 + h + GAP );
		}
	}

	// ------------------------------------------------------------------ the timeline

	private void run( int i ){
		Demo.Step s = steps.get( i );
		Puppet p = s.who == null ? null : puppetById.get( s.who );
		Object m = made[i];
		switch (s.op){
			case MOVE:     if (p != null) p.walk( s.cells ); break;
			case KNOCK:    if (p != null) p.slide( s.cells[0], THROW, 0, false ); break;
			case JUMP:     if (p != null) p.slide( s.cells[0], LEAP, 4, false ); break;
			case ATTACK:   if (p != null) p.act( GuidePuppet.Slot.ATTACK, s.cells[0] ); break;
			case ZAP:      if (p != null) p.act( GuidePuppet.Slot.ZAP, s.cells[0] ); break;
			case OPERATE:  if (p != null) p.act( GuidePuppet.Slot.OPERATE, s.cells[0] ); break;
			case SHOOT:    if (p != null) shoot( p, s.cells[0], (DemoShot) m ); break;
			case ANIM:     if (p != null && p.sprite != null) GuidePuppet.play( p.sprite, GuidePuppet.named( p.sprite, s.text ) ); break;
			case IDLE:     if (p != null && p.sprite != null) GuidePuppet.play( p.sprite, GuidePuppet.Slot.IDLE ); break;
			case FACE:     if (p != null) p.face( s.cells[0] ); break;
			case HURT:
				if (p != null && p.sprite != null && p.shown) p.sprite.flash();
				if (p != null) ((Rising) m).start( p );
				break;
			case HEAL: case STATUS: case CHIP:
				if (p != null) ((Rising) m).start( p );
				break;
			case SURPRISE:
				if (p != null && p.sprite != null && p.shown){
					((Surprise) m).reset( p.sprite );
				}
				break;
			case DIE:      if (p != null) p.die(); break;
			case ICON: case BUFF:
				if (p != null) p.head( (Image) m );
				break;
			case CLEAR:    if (p != null) p.head( null ); break;
			case FREEZE:   if (p != null && p.sprite != null) p.sprite.paused = true; break;
			case THAW:     if (p != null && p.sprite != null) p.sprite.paused = false; break;
			case LOOK:
				if (p != null && p.sprite != null){
					s.look.apply( p.sprite, s.on );
					p.lookOn( s.look, s.on );
				}
				break;
			case SHOW: case HIDE:
				if (p != null){
					p.show( s.op == Demo.Op.SHOW );
				} else if (propById.get( s.who ) != null){
					showProp( propById.get( s.who ), s.op == Demo.Op.SHOW );
				}
				break;
			case BURST:
				for (Spout e : (Spout[]) m) e.burst();
				break;
			case POUR:
				for (Spout e : (Spout[]) m) e.pour();
				break;
			case STOP:
				for (Object o : made){
					if (!(o instanceof Spout[])) continue;
					for (Spout e : (Spout[]) o) if (e.pouring && reaches( s.cells, e.cell )) e.stop();
				}
				break;
			case MARK:
				for (int c : s.cells) marks[c].set( s.mark );
				break;
			case UNMARK:
				for (int c = 0; c < marks.length; c++) if (marks[c] != null && reaches( s.cells, c )) marks[c].set( null );
				break;
			case ARROW:    ((Arrow) m).visible = true; break;
			case UNARROW:
				for (Object o : made) if (o instanceof Arrow) ((Arrow) o).visible = false;
				break;
			case TERRAIN:
				for (int c : s.cells) map[c] = s.n;
				paintTiles();
				break;
			case CAPTION:  showCaption( s.text == null ? null : captions.get( s.text ) ); break;
		}
	}

	//whether a step's cells take in this cell (none named: all)
	private static boolean reaches( int[] cells, int cell ){
		if (cells.length == 0) return true;
		for (int c : cells) if (c == cell) return true;
		return false;
	}

	private void showCaption( Words w ){
		if (caption != null) caption.visible = false;
		caption = w;
		if (caption != null) caption.visible = true;
	}

	//what a step will show, made now: its number, icon, particles, marks, arrow
	private Object make( Demo.Step s ){
		switch (s.op){
			case HURT:
				return new Rising( "" + s.n, CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
			case HEAL:
				return new Rising( "" + s.n, CharSprite.POSITIVE, FloatingText.HEALING );
			case STATUS:
				return new Rising( demo.words( s.text ), s.n, FloatingText.NO_ICON );
			case CHIP:
				return new Rising( s.n );
			case SURPRISE: {
				Surprise sur = new Surprise();
				sur.kill();
				texts.add( sur );
				return sur;
			}
			case ICON: {
				Image icon = Icons.get( s.icon );
				icon.visible = false;
				heads.add( icon );
				return icon;
			}
			case BUFF: {
				Image icon = buffIcon( s.buff, s.n );
				icon.visible = false;
				heads.add( icon );
				return icon;
			}
			case BURST: case POUR: {
				Spout[] spouts = new Spout[s.cells.length];
				for (int i = 0; i < spouts.length; i++){
					spouts[i] = new Spout( s.effect, s.cells[i] );
					fx.add( spouts[i] );
				}
				return spouts;
			}
			case MARK:
				for (int c : s.cells) if (marks[c] == null) marks[c] = new CellMark();
				return null;
			case ARROW: {
				Arrow a = new Arrow( s.cells[0], s.cells[1], s.mark );
				arrows.add( a );
				return a;
			}
			case SHOOT: {
				DemoShot shot = DemoShot.make( s.shot, (float)Math.sqrt( cols * cols + rows * rows ) * CELL );
				fx.add( shot );
				return shot;
			}
			default:
				return null;
		}
	}

	private void resetMade( Object m ){
		if (m instanceof Rising) ((Rising) m).stop();
		else if (m instanceof Surprise) ((Surprise) m).kill();
		else if (m instanceof Image) ((Image) m).visible = false;
		else if (m instanceof Spout[]) for (Spout e : (Spout[]) m) e.stop();
		else if (m instanceof Arrow) ((Arrow) m).visible = false;
		else if (m instanceof DemoShot) ((DemoShot) m).stop();
	}

	private void placeMade( Object m ){
		if (m instanceof Spout[]) for (Spout e : (Spout[]) m) e.place();
		else if (m instanceof Arrow) ((Arrow) m).place();
	}

	private void advance( Object m, float dt ){
		if (m instanceof Rising) ((Rising) m).update( dt );
		else if (m instanceof Arrow && ((Arrow) m).visible) ((Arrow) m).flow();
	}

	// ------------------------------------------------------------------ the tiles

	//a stage's own variance (a level's is its own and the stage has none): a cell's look, from its
	//page and its place, so a stage looks the same every time it is opened
	private static byte[] variance( Demo demo ){
		byte[] v = new byte[demo.width() * demo.height()];
		long seed = demo.key.hashCode() * 31L + demo.index;
		for (int i = 0; i < v.length; i++){
			seed = seed * 6364136223846793005L + 1442695040888963407L;
			v[i] = (byte)((seed >>> 33) % 100);
		}
		return v;
	}

	private boolean hasWater(){
		for (int t : start) if (t == Terrain.WATER) return true;
		for (Demo.Step s : steps) if (s.op == Demo.Op.TERRAIN && s.n == Terrain.WATER) return true;
		return false;
	}

	//the terrain at (x, y), -1 off the stage (as a level's edge reads)
	private int at( int x, int y ){
		return x >= 0 && y >= 0 && x < cols && y < rows ? map[x + y * cols] : -1;
	}

	private int alt( int visual, int x, int y ){
		return DungeonTileSheet.visualWithAlts( visual, variance[x + y * cols] );
	}

	private void paintTiles(){
		int[] g = new int[map.length], d = new int[map.length], r = new int[map.length], w = new int[map.length];
		for (int y = 0; y < rows; y++){
			for (int x = 0; x < cols; x++){
				int i = x + y * cols;
				g[i] = groundVisual( x, y );
				d[i] = dressingVisual( x, y );
				r[i] = raisedVisual( x, y );
				w[i] = wallVisual( x, y );
			}
		}
		ground.map( g, cols );
		dressing.map( d, cols );
		raised.map( r, cols );
		walls.map( w, cols );
		tilesDirty = !Arrays.equals( map, start );
	}

	//the ground, as DungeonTerrainTilemap draws a level's (raised)
	private int groundVisual( int x, int y ){
		int t = at( x, y );
		int direct = DungeonTileSheet.directVisuals.get( t, -1 );
		if (direct != -1) return alt( direct, x, y );
		int below = at( x, y + 1 );
		switch (t){
			case Terrain.WATER:
				return DungeonTileSheet.WATER + (bank( at( x, y - 1 ) ) ? 1 : 0) + (bank( at( x + 1, y ) ) ? 2 : 0)
						+ (bank( below ) ? 4 : 0) + (bank( at( x - 1, y ) ) ? 8 : 0);
			case Terrain.CHASM:
				return chasm( at( x, y - 1 ) );
			case Terrain.STATUE:          return DungeonTileSheet.RAISED_STATUE;
			case Terrain.STATUE_SP:       return DungeonTileSheet.RAISED_STATUE_SP;
			case Terrain.REGION_DECO:     return DungeonTileSheet.RAISED_REGION_DECO;
			case Terrain.REGION_DECO_ALT: return DungeonTileSheet.RAISED_REGION_DECO_ALT;
			case Terrain.ALCHEMY:         return DungeonTileSheet.RAISED_ALCHEMY_POT;
			case Terrain.BARRICADE:       return DungeonTileSheet.RAISED_BARRICADE;
			case Terrain.HIGH_GRASS:      return alt( DungeonTileSheet.RAISED_HIGH_GRASS, x, y );
			case Terrain.FURROWED_GRASS:  return alt( DungeonTileSheet.RAISED_FURROWED_GRASS, x, y );
		}
		if (DungeonTileSheet.doorTile( t )) return DungeonTileSheet.getRaisedDoorTile( t, below );
		if (DungeonTileSheet.wallStitcheable( t )){
			//a wall's face shows over the open ground below it (DungeonTileSheet.getRaisedWallTile)
			int face;
			if (DungeonTileSheet.wallStitcheable( below ))     return -1;
			else if (DungeonTileSheet.doorTile( below ))       face = DungeonTileSheet.RAISED_WALL_DOOR;
			else if (t == Terrain.WALL_DECO)                   face = DungeonTileSheet.RAISED_WALL_DECO;
			else if (t == Terrain.BOOKSHELF)                   face = DungeonTileSheet.RAISED_WALL_BOOKSHELF;
			else                                               face = DungeonTileSheet.RAISED_WALL;
			face = alt( face, x, y );
			if (!DungeonTileSheet.wallStitcheable( at( x + 1, y ) )) face += 1;
			if (!DungeonTileSheet.wallStitcheable( at( x - 1, y ) )) face += 2;
			return face;
		}
		return -1;
	}

	//whether water lying against this terrain gets a bank (DungeonTileSheet.waterStitcheable, by the
	//stage's region where a level goes by its own)
	private boolean bank( int t ){
		if (t == -1) return false;
		Demo.Region r = demo.region();
		if ((t == Terrain.EMPTY_SP || t == Terrain.STATUE_SP) && (r == Demo.Region.FROZEN || r == Demo.Region.NEST)) return true;
		if (t == Terrain.REGION_DECO_ALT) return r.depth > 20;
		return DungeonTileSheet.waterStitcheable.contains( t );
	}

	//a chasm's edge, from what lies above it (DungeonTileSheet.stitchChasmTile, by the stage's region)
	private int chasm( int above ){
		if (above == Terrain.REGION_DECO_ALT){
			int d = demo.region().depth;
			if (d <= 5)  return DungeonTileSheet.CHASM_FLOOR_SP;
			if (d <= 10) return DungeonTileSheet.CHASM;
			if (d <= 20) return DungeonTileSheet.CHASM_FLOOR_SP;
			return DungeonTileSheet.CHASM_FLOOR;
		}
		return DungeonTileSheet.chasmStitcheable.get( above, DungeonTileSheet.CHASM );
	}

	//the region's dressing over the ground, as TerrainFeaturesTilemap draws it
	private int dressingVisual( int x, int y ){
		Demo.Region r = demo.region();
		int t = at( x, y ), s = 16 * r.dressing;
		switch (t){
			case Terrain.HIGH_GRASS:
				return 192 + s + (alt( DungeonTileSheet.RAISED_HIGH_GRASS, x, y ) == DungeonTileSheet.RAISED_HIGH_GRASS_ALT ? 1 : 0);
			case Terrain.FURROWED_GRASS:
				return 194 + s + (alt( DungeonTileSheet.RAISED_FURROWED_GRASS, x, y ) == DungeonTileSheet.RAISED_FURROWED_ALT ? 1 : 0);
			case Terrain.GRASS:
				return 196 + s + (alt( DungeonTileSheet.GRASS, x, y ) == DungeonTileSheet.GRASS_ALT ? 1 : 0);
			case Terrain.EMBERS:
				return 272 + (alt( DungeonTileSheet.EMBERS, x, y ) == DungeonTileSheet.EMBERS_ALT ? 1 : 0);
		}
		//only the five regional sheets leave their objects to the dressing
		if (!r.dressed) return -1;
		switch (t){
			case Terrain.BARRICADE:       return 198 + s;
			case Terrain.ALCHEMY:         return 199 + s;
			case Terrain.STATUE: case Terrain.STATUE_SP:
				return 200 + s;
			case Terrain.REGION_DECO:     return 201 + s;
			case Terrain.REGION_DECO_ALT: return 202 + s;
			default:                      return -1;
		}
	}

	//the tall grass's tuft over whoever stands in it, as RaisedTerrainTilemap draws it
	private int raisedVisual( int x, int y ){
		int t = at( x, y ), row = 4 * demo.region().raisedRow;
		if (t == Terrain.HIGH_GRASS){
			return row + (alt( DungeonTileSheet.RAISED_HIGH_GRASS, x, y ) == DungeonTileSheet.RAISED_HIGH_GRASS_ALT ? 2 : 0);
		} else if (t == Terrain.FURROWED_GRASS){
			return row + 1 + (alt( DungeonTileSheet.RAISED_FURROWED_GRASS, x, y ) == DungeonTileSheet.RAISED_FURROWED_ALT ? 2 : 0);
		}
		return -1;
	}

	//the walls' tops and the lips they cast on the cell above, over the creatures, as DungeonWallsTilemap draws them
	private int wallVisual( int x, int y ){
		int t = at( x, y );
		boolean hasBelow = y + 1 < rows;
		int below = at( x, y + 1 ), right = at( x + 1, y ), left = at( x - 1, y );
		int rightBelow = at( x + 1, y + 1 ), leftBelow = at( x - 1, y + 1 );
		if (DungeonTileSheet.wallStitcheable( t )){
			if (hasBelow && !DungeonTileSheet.wallStitcheable( below )){
				if (below == Terrain.DOOR)             return DungeonTileSheet.DOOR_SIDEWAYS;
				else if (below == Terrain.LOCKED_DOOR) return DungeonTileSheet.DOOR_SIDEWAYS_LOCKED;
				else if (below == Terrain.OPEN_DOOR)   return -1;
			} else {
				int top = DungeonTileSheet.WALLS_INTERNAL
						+ (t == Terrain.BOOKSHELF || below == Terrain.BOOKSHELF ? 32 : 0);
				if (!DungeonTileSheet.wallStitcheable( right ))      top += 1;
				if (!DungeonTileSheet.wallStitcheable( rightBelow )) top += 2;
				if (!DungeonTileSheet.wallStitcheable( leftBelow ))  top += 4;
				if (!DungeonTileSheet.wallStitcheable( left ))       top += 8;
				return top;
			}
		}
		if (!hasBelow) return -1;
		if (DungeonTileSheet.wallStitcheable( below )){
			int lip;
			if (t == Terrain.OPEN_DOOR)            lip = DungeonTileSheet.DOOR_SIDEWAYS_OVERHANG;
			else if (t == Terrain.DOOR)            lip = DungeonTileSheet.DOOR_SIDEWAYS_OVERHANG_CLOSED;
			else if (t == Terrain.LOCKED_DOOR)     lip = DungeonTileSheet.DOOR_SIDEWAYS_OVERHANG_LOCKED;
			else if (below == Terrain.BOOKSHELF)   lip = DungeonTileSheet.WALL_OVERHANG_WOODEN;
			else                                   lip = DungeonTileSheet.WALL_OVERHANG;
			if (!DungeonTileSheet.wallStitcheable( rightBelow )) lip += 1;
			if (!DungeonTileSheet.wallStitcheable( leftBelow ))  lip += 2;
			return lip;
		}
		switch (below){
			case Terrain.DOOR: case Terrain.LOCKED_DOOR:
				return DungeonTileSheet.DOOR_OVERHANG;
			case Terrain.OPEN_DOOR:       return DungeonTileSheet.DOOR_OVERHANG_OPEN;
			case Terrain.STATUE:          return DungeonTileSheet.STATUE_OVERHANG;
			case Terrain.STATUE_SP:       return DungeonTileSheet.STATUE_SP_OVERHANG;
			case Terrain.REGION_DECO:     return DungeonTileSheet.REGION_DECO_OVERHANG;
			case Terrain.REGION_DECO_ALT: return DungeonTileSheet.REGION_DECO_ALT_OVERHANG;
			case Terrain.ALCHEMY:         return DungeonTileSheet.ALCHEMY_POT_OVERHANG;
			case Terrain.BARRICADE:       return DungeonTileSheet.BARRICADE_OVERHANG;
			case Terrain.HIGH_GRASS:      return alt( DungeonTileSheet.HIGH_GRASS_OVERHANG, x, y + 1 );
			case Terrain.FURROWED_GRASS:  return alt( DungeonTileSheet.FURROWED_OVERHANG, x, y + 1 );
			default:                      return -1;
		}
	}

	//one layer of the stage's tiles, in 16 px cells of one sheet
	private static class Layer extends Tilemap {
		Layer( String asset ){
			super( asset, new TextureFilm( asset, CELL, CELL ) );
		}
	}

	//the region's water under the ground, flowing down a whole pixel every 0.2 s
	private static class Water extends SkinnedBlock {

		private static final float FLOW = 0.2f;
		private float flowing;

		Water( float w, float h, String asset ){
			super( w, h, asset );
			autoAdjust = true;
		}

		void flow( float dt ){
			flowing += dt;
			while (flowing >= FLOW){
				flowing -= FLOW;
				offset( 0, -1 );
			}
		}
	}

	// ------------------------------------------------------------------ what lies on the floor

	//what lies on the floor: an item as its cite draws it (a potion, a scroll or a ring shows its kind,
	//never this run's look) with its shadow under it, a plant or a trap as the floor shows them.
	//What cannot be made is the unknown item
	private static Image prop( Demo.Prop p ){
		Image img;
		try {
			switch (p.kind){
				case ITEM:   img = CiteArt.item( (Item) Reflection.newInstanceUnhandled( p.type ) ); break;
				case SPRITE: img = new CiteArt.ItemArt( p.image ); break;
				case PLANT:  img = TerrainFeaturesTilemap.getPlantVisual( (Plant) Reflection.newInstanceUnhandled( p.type ) ); break;
				default:     img = TerrainFeaturesTilemap.getTrapVisual( (Trap) Reflection.newInstanceUnhandled( p.type ) ); break;
			}
		} catch (Exception e){
			img = null;
		}
		if (img == null) img = new CiteArt.ItemArt( ItemSpriteSheet.SOMETHING );
		if (img instanceof CiteArt.ItemArt) ((CiteArt.ItemArt) img).bob( 0 );
		return img;
	}

	//a prop shown or hidden: a hidden one is not updated either, as an item's sprite with no heap shows
	//itself again on its next update (ItemSprite.update)
	private static void showProp( Image img, boolean on ){
		img.visible = on;
		img.active = on;
	}

	//an item stands raised off its cell's floor as a heap does (ItemSprite.perspectiveRaise: 5 px, more
	//for a short one); a plant or a trap fills its cell
	private void lay( Demo.Prop p, Image img ){
		int cx = ox + (p.cell % cols) * CELL, cy = oy + (p.cell / cols) * CELL;
		int w = (int)img.width(), h = (int)img.height();
		if (img instanceof ItemSprite){
			img.x = cx + (int)Math.floor( (CELL - w) / 2f );
			img.y = cy + CELL - h - (h < 8 ? 13 - h : 5);
		} else {
			img.x = cx + (CELL - w) / 2;
			img.y = cy + (CELL - h) / 2;
		}
	}

	private static Image buffIcon( Class<? extends Buff> cls, int icon ){
		try {
			Buff b = Reflection.newInstanceUnhandled( cls );
			BuffIcon img = new BuffIcon( icon >= 0 ? icon : b.icon(), false );
			b.tintIcon( img );
			return img;
		} catch (Exception e){
			return new BuffIcon( Math.max( icon, 0 ), false );
		}
	}

	// ------------------------------------------------------------------ the creatures

	private final class Puppet {

		final Demo.Actor actor;
		final CharSprite sprite;
		int cell;
		boolean shown;
		//where it stands, px from the stage's corner; a motion under way to (tx, ty)
		int rx, ry;
		float fx, fy, tx, ty, t, len, arc;
		boolean moving, walking;
		int[] path;
		int pathAt;
		//dying: seconds since it died (-1 alive)
		float dead = -1;
		Image head;
		float headT;
		//the looks put on it, to take off when the stage starts over
		final ArrayList<Demo.Look> looks = new ArrayList<>();

		Puppet( Demo.Actor a ){
			actor = a;
			CharSprite s;
			if (a.mob != null){
				s = GuidePuppet.make( a.mob );
			} else {
				//dressed as a hero starts out (cloth armour), or as the player is now
				HeroClass cls = a.hero;
				int tier = 1;
				if (cls == null){
					cls = Dungeon.hero != null ? Dungeon.hero.heroClass : HeroClass.WARRIOR;
					if (Dungeon.hero != null) tier = Dungeon.hero.tier();
				}
				s = GuidePuppet.hero( cls, tier );
			}
			if (s != null) GuidePuppet.shadow( s );
			sprite = s;
		}

		void reset(){
			cell = actor.cell;
			moving = walking = false;
			path = null;
			dead = -1;
			head( null );
			shown = true;
			if (sprite == null) return;
			for (Demo.Look l : looks) l.apply( sprite, false );
			looks.clear();
			sprite.paused = false;
			sprite.visible = true;
			sprite.alpha( 1 );
			sprite.resetColor();
			sprite.flipHorizontal = false;
			GuidePuppet.play( sprite, GuidePuppet.Slot.IDLE );
			stand();
		}

		//its place on a cell, px from the stage's corner (as a level puts a creature on a cell)
		private int standX( int c ){
			return (c % cols) * CELL + (int)Math.floor( (CELL - sprite.width()) / 2f );
		}

		private int standY( int c ){
			return (c / cols + 1) * CELL - (int)sprite.height() - GuidePuppet.raise( sprite );
		}

		private void stand(){
			rx = standX( cell );
			ry = standY( cell );
		}

		void place(){
			if (sprite == null) return;
			sprite.x = ox + rx;
			sprite.y = oy + ry;
			if (head != null){
				head.x = (int)(sprite.x + sprite.width() - head.width() / 2f);
				head.y = (int)sprite.y - (int)head.height() + 2 - ((int)(headT / 0.6f) % 2);
			}
		}

		void face( int to ){
			if (sprite == null) return;
			int dx = to % cols - cell % cols;
			if (dx != 0) sprite.flipHorizontal = dx < 0;
		}

		void walk( int[] cells ){
			path = cells;
			pathAt = 0;
			nextStep();
		}

		private void nextStep(){
			if (pathAt >= path.length){
				path = null;
				return;
			}
			slide( path[pathAt++], STEP, 0, true );
		}

		void slide( int to, float perCell, float height, boolean walk ){
			if (sprite == null){
				cell = to;
				return;
			}
			int dist = Math.max( Math.abs( to % cols - cell % cols ), Math.abs( to / cols - cell / cols ) );
			//a walk or a jump turns it the way it goes; thrown, it keeps facing where it faced
			if (walk || height > 0) face( to );
			fx = rx;
			fy = ry;
			tx = standX( to );
			ty = standY( to );
			cell = to;
			t = 0;
			len = Math.max( 1, dist ) * perCell;
			arc = height * Math.max( 1, dist );
			moving = true;
			if (walk && !walking){
				GuidePuppet.play( sprite, GuidePuppet.Slot.RUN );
			}
			walking = walk;
			//the one nearer the bottom stands in front
			order();
		}

		void act( GuidePuppet.Slot slot, int at ){
			if (sprite == null) return;
			face( at );
			GuidePuppet.play( sprite, slot );
		}

		void die(){
			if (sprite == null) return;
			GuidePuppet.play( sprite, GuidePuppet.Slot.DIE );
			head( null );
			dead = 0;
		}

		void show( boolean on ){
			shown = on;
			if (sprite == null) return;
			if (on && dead >= 0){
				//shown again after it died: another of its kind, whole
				dead = -1;
				sprite.alpha( 1 );
				GuidePuppet.play( sprite, GuidePuppet.Slot.IDLE );
			}
			sprite.visible = on;
			if (head != null) head.visible = on;
		}

		void head( Image img ){
			if (head != null) head.visible = false;
			head = img;
			headT = 0;
			if (head != null){
				//over a creature that could not be drawn it would float at the stage's corner
				head.visible = shown && sprite != null;
				place();
			}
		}

		void lookOn( Demo.Look l, boolean on ){
			if (on && !looks.contains( l )) looks.add( l );
			else if (!on) looks.remove( l );
		}

		void update( float dt ){
			if (sprite == null) return;
			if (moving){
				t += dt;
				float k = Math.min( 1f, t / len );
				rx = Math.round( fx + (tx - fx) * k );
				ry = Math.round( fy + (ty - fy) * k - arc * 4 * k * (1 - k) );
				if (k >= 1){
					moving = false;
					order();
					if (path != null){
						nextStep();
					}
					if (!moving && walking){
						walking = false;
						GuidePuppet.play( sprite, GuidePuppet.Slot.IDLE );
					}
				}
			}
			if (dead >= 0){
				dead += dt;
				float k = (dead - DIE_AFTER) / DIE_FADE;
				if (k >= 1){
					sprite.visible = false;
				} else if (k > 0){
					sprite.alpha( 1 - k );
				}
			}
			if (head != null) headT += dt;
			place();
		}
	}

	//a shot from the middle of who sends it to the middle of whoever stands on the cell (where it
	//comes to rest), or to the cell's raised middle with no one there
	//(DungeonTilemap.raisedTileCenterToWorld), as the game aims; one that flies lands on the beat
	private void shoot( Puppet from, int cell, DemoShot shot ){
		if (from.sprite == null || !from.shown) return;
		float sx = from.sprite.x + from.sprite.width() / 2f, sy = from.sprite.y + from.sprite.height() / 2f;
		float ex = ox + (cell % cols + 0.5f) * CELL, ey = oy + (cell / cols + 0.1f) * CELL;
		for (int i = 0; i < puppets.size(); i++){
			Puppet q = puppets.get( i );
			if (q != from && q.cell == cell && q.shown && q.dead < 0 && q.sprite != null){
				ex = ox + q.standX( cell ) + q.sprite.width() / 2f;
				ey = oy + q.standY( cell ) + q.sprite.height() / 2f;
				break;
			}
		}
		shot.fire( sx, sy, ex, ey, Demo.BEAT );
	}

	//the creatures nearer the bottom drawn over those above them, as a level sorts its sprites
	private void order(){
		for (int row = 0; row < rows; row++){
			for (int i = 0; i < puppets.size(); i++){
				Puppet p = puppets.get( i );
				if (p.sprite != null && p.cell / cols == row) standing.bringToFront( p.sprite );
			}
		}
	}

	// ------------------------------------------------------------------ numbers and words

	/** Words on the stage: a caption, or a number or word rising over a creature. */
	public static class Words extends RenderedTextBlock {

		public Words( String text, int size ){
			super( text, size * zoom() );
			zoom( 1f / zoom() );
		}

		//as PixelScene.renderTextBlock: drawn at the screen's own pixels, then scaled to the ui's
		private static int zoom(){
			return Math.round( PixelScene.defaultZoom * DeviceCompat.getRealPixelScaleX() );
		}

		//moved whole pixels, its words with it, without laying it out again
		void shift( int dx, int dy ){
			x += dx;
			y += dy;
			for (int i = 0; i < words.size(); i++){
				RenderedText w = words.get( i );
				w.x += dx;
				w.y += dy;
			}
		}
	}

	//a number or a word rising over a creature and fading, with its icon on its left (FloatingText's
	//colours, icons and life, as Char.damage shows them)
	private final class Rising {

		final Words text;
		final Image icon;
		float t = -1;
		//where it started (its top), and its height
		int top, h;

		Rising( String s, int color, int iconIdx ){
			text = words( s, 9 );
			if (text != null){
				text.setHightlighting( false );
				text.hardlight( color );
				text.visible = false;
				texts.add( text );
			}
			if (iconIdx != FloatingText.NO_ICON){
				icon = new Image( Assets.Effects.TEXT_ICONS );
				icon.frame( FloatingText.iconFilm.get( iconIdx ) );
				icon.visible = false;
				texts.add( icon );
			} else {
				icon = null;
			}
		}

		//a hit card's icon alone (a chip code: a text cell, a buff's icon, a skill's)
		Rising( int code ){
			text = null;
			icon = new Image();
			xyz.gabriwar.warpedpixeldungeon.effects.HitCard.picture( icon, code );
			icon.visible = false;
			texts.add( icon );
		}

		void start( Puppet p ){
			if (p.sprite == null || !p.shown) return;
			t = 0;
			int tw = text != null ? (int)Math.ceil( text.width() ) : 0;
			int iw = icon != null ? (int)icon.width() + 1 : 0;
			h = Math.max( text != null ? (int)Math.ceil( text.height() ) : 0, icon != null ? (int)icon.height() : 0 );
			int x0 = (int)(p.sprite.x + p.sprite.width() / 2f) - (tw + iw) / 2;
			top = (int)p.sprite.y - h + 2;
			if (icon != null){
				icon.x = x0;
				icon.visible = true;
			}
			if (text != null){
				text.setPos( x0 + iw, top );
				text.visible = true;
			}
			show( top, 1 );
		}

		void update( float dt ){
			if (t < 0) return;
			t += dt;
			if (t >= RISE_FOR){
				stop();
				return;
			}
			float left = 1 - t / RISE_FOR;
			show( top - (int)(RISE * t / RISE_FOR), left > 0.5f ? 1 : left * 2 );
		}

		//at this top, this alpha: moved only by whole pixels, never laid out again
		private void show( int y, float a ){
			if (text != null){
				int dy = y - (int)text.top();
				if (dy != 0) text.shift( 0, dy );
				text.alpha( a );
			}
			if (icon != null){
				icon.y = y + (h - (int)icon.height()) / 2;
				icon.alpha( a );
			}
		}

		void stop(){
			t = -1;
			if (text != null) text.visible = false;
			if (icon != null) icon.visible = false;
		}
	}

	// ------------------------------------------------------------------ the game's particles

	//a cell's emitter of a guide effect: its particles placed by the effects' own random (the game's is
	//the world's), and killed with it so the effects budget's count stays true
	private final class Spout extends Emitter {

		final GuideEffect effect;
		final int cell;
		boolean pouring;

		Spout( GuideEffect effect, int cell ){
			this.effect = effect;
			this.cell = cell;
			autoKill = false;
		}

		void place(){
			int cx = ox + (cell % cols) * CELL, cy = oy + (cell / cols) * CELL;
			//what rises starts low; falling rocks land low (they drop from 13 px over where they land)
			if (CiteArt.rises( effect ) || effect == GuideEffect.ROCKS) pos( cx + 3, cy + CELL - 5, CELL - 6, 3 );
			else pos( cx + 3, cy + 3, CELL - 6, CELL - 6 );
		}

		void burst(){
			if (effect == GuideEffect.ROCKS){
				//as rocks fall on a cell of a level: 10 of them over 0.7 s (DelayedRockFall)
				startDelayed( CiteArt.factory( effect ), 0.07f, 10, 0 );
			} else {
				int n = CiteArt.burstSize( effect );
				startDelayed( CiteArt.factory( effect ), 0.05f, n > 0 ? n : 6, 0 );
			}
			pouring = false;
		}

		void pour(){
			float i = CiteArt.interval( effect );
			startDelayed( CiteArt.factory( effect ), i, 0, FxRandom.Float( i ) );
			pouring = true;
		}

		void stop(){
			on = false;
			pouring = false;
		}

		@Override
		protected void emit( int index ) {
			factory.emit( this, index, x + FxRandom.Float( width ), y + FxRandom.Float( height ) );
		}

		@Override
		public void destroy() {
			kill();
			super.destroy();
		}
	}

	// ------------------------------------------------------------------ marks and arrows

	//how a cell is marked: a pool of colour on its floor, the game's reticle over it, an edge round it
	private final class CellMark {

		final ColorBlock pool = new ColorBlock( CELL, CELL, 0xFFFFFFFF );
		final Image reticle = Icons.get( Icons.TARGET );
		final ColorBlock[] edge = new ColorBlock[4];
		Demo.Mark mark;

		CellMark(){
			floor.add( pool );
			for (int i = 0; i < edge.length; i++){
				edge[i] = new ColorBlock( 1, 1, 0xFFFFFFFF );
				floor.add( edge[i] );
			}
			reticle.hardlight( 0xFF0000 );
			reticles.add( reticle );
		}

		void set( Demo.Mark m ){
			mark = m;
			pool.visible = m == Demo.Mark.DANGER || m == Demo.Mark.TARGET || m == Demo.Mark.SAFE;
			reticle.visible = m == Demo.Mark.TARGET;
			boolean edged = m == Demo.Mark.SAFE || m == Demo.Mark.HINT;
			int c = m == Demo.Mark.SAFE ? GREEN : m == Demo.Mark.HINT ? YELLOW : RED;
			pool.hardlight( c );
			for (ColorBlock e : edge){
				e.visible = edged;
				e.hardlight( c );
			}
			update();
		}

		void place( int cell ){
			int cx = ox + (cell % cols) * CELL, cy = oy + (cell / cols) * CELL;
			pool.x = cx;
			pool.y = cy;
			reticle.x = cx + (CELL - (int)reticle.width()) / 2;
			reticle.y = cy + (CELL - (int)reticle.height()) / 2;
			edge[0].x = cx;            edge[0].y = cy;            edge[0].size( CELL, 1 );
			edge[1].x = cx;            edge[1].y = cy + CELL - 1; edge[1].size( CELL, 1 );
			edge[2].x = cx;            edge[2].y = cy + 1;        edge[2].size( 1, CELL - 2 );
			edge[3].x = cx + CELL - 1; edge[3].y = cy + 1;        edge[3].size( 1, CELL - 2 );
		}

		//a slow beat, once a second, as the game's telegraph breathes
		void update(){
			if (mark == null) return;
			float beat = TargetedCell.poolAlpha( pulse ) / TargetedCell.POOL_HIGH;
			pool.alpha( (mark == Demo.Mark.SAFE ? 0.35f : 0.45f) * beat );
			reticle.alpha( 0.7f + 0.3f * beat );
			for (ColorBlock e : edge) e.alpha( 0.6f + 0.4f * beat );
		}
	}

	//an arrow from a cell's middle to another's, of dots that run toward its head
	private final class Arrow extends Group {

		private static final int SPACING = 4;
		final int from, to;
		final ColorBlock[] dots;
		final ColorBlock[] point = new ColorBlock[3];
		final int sx, sy, length;

		Arrow( int from, int to, Demo.Mark mark ){
			this.from = from;
			this.to = to;
			sx = Integer.signum( to % cols - from % cols );
			sy = Integer.signum( to / cols - from / cols );
			length = Math.max( Math.abs( to % cols - from % cols ), Math.abs( to / cols - from / cols ) ) * CELL;
			int c = mark == Demo.Mark.SAFE ? GREEN : mark == Demo.Mark.HINT ? YELLOW : RED;
			int n = Math.max( 0, (length - 10) / SPACING );
			dots = new ColorBlock[n];
			for (int i = 0; i < n; i++){
				dots[i] = new ColorBlock( 2, 2, 0xFF000000 | c );
				add( dots[i] );
			}
			for (int i = 0; i < point.length; i++){
				point[i] = new ColorBlock( 2, 2, 0xFF000000 | c );
				add( point[i] );
			}
			visible = false;
		}

		void place(){
			int ax = ox + (from % cols) * CELL + CELL / 2 - 1, ay = oy + (from / cols) * CELL + CELL / 2 - 1;
			for (int i = 0; i < dots.length; i++){
				int d = 4 + i * SPACING;
				dots[i].x = ax + sx * d;
				dots[i].y = ay + sy * d;
			}
			//the head: its tip, and two barbs back from it either side
			int tip = length - 3, back = length - 6;
			point[0].x = ax + sx * tip;                  point[0].y = ay + sy * tip;
			point[1].x = ax + sx * back - sy * 3;        point[1].y = ay + sy * back + sx * 3;
			point[2].x = ax + sx * back + sy * 3;        point[2].y = ay + sy * back - sx * 3;
		}

		//a brighter dot running from tail to head, a dot every 0.12 s
		void flow(){
			int lit = (int)(pulse / 0.12f);
			for (int i = 0; i < dots.length; i++){
				dots[i].alpha( (lit - i) % 4 == 0 ? 1f : 0.5f );
			}
		}
	}
}
