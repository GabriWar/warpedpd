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

package xyz.gabriwar.warpedpixeldungeon.journal.guidedemos;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.GuideEffect;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

//A guide page's scene, acted out: a little stage in the region's own tiles, the creatures' own
//sprites and the game's own particles, and a timeline of what they do, turn by turn, looping.
//Made only through DemoBook; pure data, so every demo can be checked headlessly (GuideDemosTest).
//ui/guide/DemoStage plays it under the paragraph it acts out.
//
//    book.demo( "boss_goo" ).under( "swells and shouts" )
//            .stage( Demo.Region.SEWERS,
//                    "#######",
//                    "...#...",
//                    "......." )
//            .mob( "goo", Goo.class, 2, 1 ).hero( "you", 3, 2 )
//            .turn()
//            .anim( "goo", "pump" ).status( "goo", "actors.mobs.goo.!!!", CharSprite.WARNING )
//            .caption( "swells" )
//            ...
//
//Steps happen at the timeline's clock: turn() moves it on a turn, beat() a beat (a blow, then its
//number). Steps written before the first turn() are how the stage starts.
public final class Demo {

	/** A turn of the game, in seconds of a demo: slow enough to read a caption by. */
	public static final float TURN = 1f;
	/** What follows something at once: a blow, then its number (a strike's animation is about this long). */
	public static final float BEAT = 0.3f;
	/** How long the end holds before the demo starts over, seconds. */
	public static final float REST = 1f;
	/** Stage sizes, in cells: 7 cells (112 px) fit a phone's page. */
	public static final int MIN_W = 3, MAX_W = 7, MIN_H = 2, MAX_H = 7;

	/** The region a stage is drawn in: its tiles, its water, its rows of the raised grass and of the dressing. */
	public enum Region {
		SEWERS( Assets.Environment.TILES_SEWERS, Assets.Environment.WATER_SEWERS, Assets.Environment.RAISED_TERRAIN, 0, 0, true, 3 ),
		PRISON( Assets.Environment.TILES_PRISON, Assets.Environment.WATER_PRISON, Assets.Environment.RAISED_TERRAIN, 1, 1, true, 8 ),
		CAVES(  Assets.Environment.TILES_CAVES,  Assets.Environment.WATER_CAVES,  Assets.Environment.RAISED_TERRAIN, 2, 2, true, 13 ),
		CITY(   Assets.Environment.TILES_CITY,   Assets.Environment.WATER_CITY,   Assets.Environment.RAISED_TERRAIN, 3, 3, true, 18 ),
		HALLS(  Assets.Environment.TILES_HALLS,  Assets.Environment.WATER_HALLS,  Assets.Environment.RAISED_TERRAIN, 4, 4, true, 23 ),
		NEST(   Assets.Environment.TILES_SPIDERNEST, Assets.Environment.WATER_CAVES, Assets.Environment.RAISED_SPIDERNEST, 0, 1, false, 6 ),
		FROZEN( Assets.Environment.TILES_FROZEN, Assets.Environment.WATER_FROZEN, Assets.Environment.RAISED_FROZEN, 0, 4, false, 21 ),
		TEMPLE( Assets.Environment.TILES_TEMPLE, Assets.Environment.WATER_TEMPLE, Assets.Environment.RAISED_TERRAIN, 2, 2, false, 14 );

		public final String tiles, water, raised;
		//its row of the raised grass sheet (RaisedTerrainTilemap.region) and of the dressing
		//(TerrainFeaturesTilemap's stage)
		public final int raisedRow, dressing;
		//whether its statues, barricades and the like are drawn by the dressing (the five regional sheets)
		public final boolean dressed;
		//a floor of it, for the stitching rules that go by depth
		public final int depth;

		Region( String tiles, String water, String raised, int raisedRow, int dressing, boolean dressed, int depth ){
			this.tiles = tiles;
			this.water = water;
			this.raised = raised;
			this.raisedRow = raisedRow;
			this.dressing = dressing;
			this.dressed = dressed;
			this.depth = depth;
		}
	}

	/** How a marked cell looks: about to be struck (a red pool), struck there (the game's red reticle
	 *  over the pool), the one safe place (green), or simply pointed at (a yellow edge). */
	public enum Mark { DANGER, TARGET, SAFE, HINT }

	/** A look a step puts on a creature's sprite and takes off again (the stage takes it off as it
	 *  starts over): DM-300 supercharged is ( s, on ) -> ((DM300Sprite) s).updateChargeState( on ). */
	public interface Look { void apply( CharSprite s, boolean on ); }

	public enum Op {
		MOVE, KNOCK, JUMP, ATTACK, ZAP, OPERATE, SHOOT, ANIM, IDLE, FACE,
		HURT, HEAL, STATUS, SURPRISE, DIE, ICON, BUFF, CLEAR, FREEZE, THAW, LOOK, SHOW, HIDE,
		BURST, POUR, STOP, MARK, UNMARK, ARROW, UNARROW, TERRAIN, CAPTION, CHIP
	}

	/** One thing that happens, at a time of the demo's clock. */
	public static final class Step {
		public final Op op;
		public final float at;
		public final String who;      //the actor (or prop, for SHOW and HIDE) it is about, or null
		public final int[] cells;     //the stage cells (x + y*width) it reaches, in order; empty for none
		public final int n;           //an amount, a terrain, a BuffIndicator icon, a colour
		public final boolean on;      //LOOK: put on or taken off
		public final String text;     //a caption or status key, an animation's field name
		public final GuideEffect effect;
		public final Mark mark;
		public final Icons icon;
		public final Look look;
		public final Class<? extends Buff> buff;
		public final Shot shot;       //SHOOT: what flies

		Step( Op op, float at, String who, int[] cells, int n, boolean on, String text, GuideEffect effect,
		      Mark mark, Icons icon, Look look, Class<? extends Buff> buff ){
			this( op, at, who, cells, n, on, text, effect, mark, icon, look, buff, null );
		}

		Step( Op op, float at, String who, int[] cells, int n, boolean on, String text, GuideEffect effect,
		      Mark mark, Icons icon, Look look, Class<? extends Buff> buff, Shot shot ){
			this.op = op;
			this.at = at;
			this.who = who;
			this.cells = cells;
			this.n = n;
			this.on = on;
			this.text = text;
			this.effect = effect;
			this.mark = mark;
			this.icon = icon;
			this.look = look;
			this.buff = buff;
			this.shot = shot;
		}
	}

	/** What a creature sends at a cell (Op.SHOOT), drawn as the game draws it: a bolt of a MagicMissile
	 *  type, an item thrown (MissileSprite), a ray (Beam), lightning, a chain (Chains). */
	public static final class Shot {
		public enum Kind { BOLT, THROWN, RAY, LIGHTNING, CHAIN }
		public final Kind kind;
		public final int type;                   //BOLT: its MagicMissile type
		public final Class<? extends Item> item; //THROWN
		public final Effects.Type ray;           //RAY: DEATH_RAY, LIGHT_RAY or HEALTH_RAY

		Shot( Kind kind, int type, Class<? extends Item> item, Effects.Type ray ){
			this.kind = kind;
			this.type = type;
			this.item = item;
			this.ray = ray;
		}
	}

	/** A creature on the stage: a monster by its class, or the hero (mob null). */
	public static final class Actor {
		public final String id;
		public final Class<? extends Mob> mob;
		//the hero's class; null for the player's own (the warrior when there is no game)
		public final HeroClass hero;
		public final int cell;

		Actor( String id, Class<? extends Mob> mob, HeroClass hero, int cell ){
			this.id = id;
			this.mob = mob;
			this.hero = hero;
			this.cell = cell;
		}
	}

	/** Something lying on the stage's floor: an item, item art, a plant or a trap. */
	public static final class Prop {
		public enum Kind { ITEM, SPRITE, PLANT, TRAP }
		public final String id;
		public final Kind kind;
		public final Class<?> type;   //ITEM, PLANT, TRAP
		public final int image;       //SPRITE: an ItemSpriteSheet image
		public final int cell;

		Prop( String id, Kind kind, Class<?> type, int image, int cell ){
			this.id = id;
			this.kind = kind;
			this.type = type;
			this.image = image;
			this.cell = cell;
		}
	}

	//the map's characters and what they stand for; every terrain a stage can draw is here
	private static final LinkedHashMap<Character, Integer> LEGEND = new LinkedHashMap<>();
	static {
		LEGEND.put( '#', Terrain.WALL );
		LEGEND.put( '%', Terrain.WALL_DECO );
		LEGEND.put( '=', Terrain.BOOKSHELF );
		LEGEND.put( 'S', Terrain.SECRET_DOOR );
		LEGEND.put( '+', Terrain.DOOR );
		LEGEND.put( '\'', Terrain.OPEN_DOOR );
		LEGEND.put( 'L', Terrain.LOCKED_DOOR );
		LEGEND.put( '.', Terrain.EMPTY );
		LEGEND.put( ',', Terrain.EMPTY_DECO );
		LEGEND.put( '_', Terrain.EMPTY_SP );
		LEGEND.put( '"', Terrain.GRASS );
		LEGEND.put( '|', Terrain.HIGH_GRASS );
		LEGEND.put( 'f', Terrain.FURROWED_GRASS );
		LEGEND.put( '*', Terrain.EMBERS );
		LEGEND.put( '~', Terrain.WATER );
		LEGEND.put( 'o', Terrain.CHASM );
		LEGEND.put( 'O', Terrain.STATUE );
		LEGEND.put( 'Q', Terrain.STATUE_SP );
		LEGEND.put( 'R', Terrain.REGION_DECO );
		LEGEND.put( 'r', Terrain.REGION_DECO_ALT );
		LEGEND.put( 'B', Terrain.BARRICADE );
		LEGEND.put( 'A', Terrain.ALCHEMY );
		LEGEND.put( 'P', Terrain.PEDESTAL );
		LEGEND.put( '<', Terrain.ENTRANCE );
		LEGEND.put( '>', Terrain.EXIT );
		LEGEND.put( 'W', Terrain.WELL );
		LEGEND.put( 'w', Terrain.EMPTY_WELL );
		LEGEND.put( 'T', Terrain.TRAP );
		LEGEND.put( 't', Terrain.SECRET_TRAP );
		LEGEND.put( 'x', Terrain.INACTIVE_TRAP );
	}

	/** Every terrain a stage can draw. */
	public static List<Integer> terrains(){
		return Collections.unmodifiableList( new ArrayList<>( LEGEND.values() ) );
	}

	/** Whether a stage can draw this terrain. */
	public static boolean drawable( int terrain ){
		return LEGEND.containsValue( terrain );
	}

	/** The page it acts out (guide.<key>). */
	public final String key;
	final String group;
	/** Which of its page's demos it is, from 0. */
	public final int index;

	private String anchor;
	private Region region;
	private int width, height;
	private int[] map;
	private final LinkedHashMap<String, Actor> actors = new LinkedHashMap<>();
	private final LinkedHashMap<String, Prop> props = new LinkedHashMap<>();
	private final ArrayList<Step> steps = new ArrayList<>();
	private float clock;

	Demo( String key, String group, int index ){
		this.key = key;
		this.group = group;
		this.index = index;
	}

	public String anchor(){ return anchor; }
	public Region region(){ return region; }
	public int width(){ return width; }
	public int height(){ return height; }
	/** The stage's terrain as it starts, row by row (Terrain constants): a copy. */
	public int[] map(){ return map == null ? null : map.clone(); }
	public List<Actor> actors(){ return Collections.unmodifiableList( new ArrayList<>( actors.values() ) ); }
	public List<Prop> props(){ return Collections.unmodifiableList( new ArrayList<>( props.values() ) ); }
	public List<Step> steps(){ return Collections.unmodifiableList( steps ); }

	/** How long it plays before it starts over, seconds: its clock, and a turn for its last step at least. */
	public float length(){
		float last = steps.isEmpty() ? 0 : steps.get( steps.size() - 1 ).at;
		return Math.max( clock, last + TURN );
	}

	/** The words of a caption or status key: this demo's own (its group's bundle,
	 *  guidedemo.<page>.<key>), or the game's for a key with a dot in it ("actors.char.def_verb"). */
	public String words( String k ){
		return Messages.get( absolute( k ) ? k : localKey( k ) );
	}

	/** The bundle key of one of this demo's own words. */
	public String localKey( String k ){
		return "guidedemo." + key + "." + k;
	}

	/** Whether a key is the game's own (it has a dot) rather than this demo's. */
	public static boolean absolute( String k ){
		return k.indexOf( '.' ) >= 0;
	}

	// ------------------------------------------------------------------ the stage

	/** The paragraph it acts out, by words of it (matched as the cites are, CiteMatch): it plays under that
	 *  paragraph and its figures. Without one it plays at the end of the page's text. */
	public Demo under( String words ){
		if (words == null || words.trim().isEmpty()) throw wrong( "under() needs words of the paragraph" );
		anchor = words;
		return this;
	}

	/** The stage: its region, then its rows, top first, a character a cell (the legend below; legend() adds). */
	public Demo stage( Region region, String... rows ){
		if (map != null) throw wrong( "one stage a demo" );
		if (region == null) throw wrong( "a stage needs its region" );
		if (rows == null || rows.length < MIN_H || rows.length > MAX_H){
			throw wrong( "a stage is " + MIN_H + " to " + MAX_H + " rows" );
		}
		int w = rows[0].length();
		if (w < MIN_W || w > MAX_W) throw wrong( "a stage is " + MIN_W + " to " + MAX_W + " cells wide" );
		int[] m = new int[w * rows.length];
		for (int y = 0; y < rows.length; y++){
			if (rows[y].length() != w) throw wrong( "row " + y + " is not " + w + " cells wide" );
			for (int x = 0; x < w; x++){
				Integer t = LEGEND.get( rows[y].charAt( x ) );
				if (t == null) throw wrong( "row " + y + ": '" + rows[y].charAt( x ) + "' is no terrain of the legend" );
				m[x + y * w] = t;
			}
		}
		this.region = region;
		width = w;
		height = rows.length;
		map = m;
		return this;
	}

	// ------------------------------------------------------------------ who and what is on it

	/** A monster, boss or NPC, by its class, standing at (x, y) as the demo starts. */
	public Demo mob( String id, Class<? extends Mob> mob, int x, int y ){
		if (mob == null) throw wrong( id + ": no mob class" );
		return actor( new Actor( fresh( id ), mob, null, standing( x, y ) ) );
	}

	/** The hero, in the player's own class (the warrior with no game). */
	public Demo hero( String id, int x, int y ){
		return actor( new Actor( fresh( id ), null, null, standing( x, y ) ) );
	}

	/** The hero, in a class of its own. */
	public Demo hero( String id, HeroClass cls, int x, int y ){
		if (cls == null) throw wrong( id + ": no hero class" );
		return actor( new Actor( fresh( id ), null, cls, standing( x, y ) ) );
	}

	/** An item lying on the floor (a potion, a scroll or a ring shows its kind, never this run's look). */
	public Demo item( String id, Class<? extends Item> item, int x, int y ){
		if (item == null) throw wrong( id + ": no item class" );
		return prop( new Prop( fresh( id ), Prop.Kind.ITEM, item, -1, cell( x, y ) ) );
	}

	/** Item-sheet art with no class lying on the floor (an ItemSpriteSheet image: a tomb, bones). */
	public Demo sprite( String id, int image, int x, int y ){
		return prop( new Prop( fresh( id ), Prop.Kind.SPRITE, null, image, cell( x, y ) ) );
	}

	/** A plant grown on the floor. */
	public Demo plant( String id, Class<? extends Plant> plant, int x, int y ){
		if (plant == null) throw wrong( id + ": no plant class" );
		return prop( new Prop( fresh( id ), Prop.Kind.PLANT, plant, -1, cell( x, y ) ) );
	}

	/** A trap on the floor, as it shows once found. */
	public Demo trap( String id, Class<? extends Trap> trap, int x, int y ){
		if (trap == null) throw wrong( id + ": no trap class" );
		return prop( new Prop( fresh( id ), Prop.Kind.TRAP, trap, -1, cell( x, y ) ) );
	}

	// ------------------------------------------------------------------ the clock

	/** A turn passes. */
	public Demo turn(){
		return turns( 1 );
	}

	/** Some turns pass. */
	public Demo turns( int n ){
		if (n < 1) throw wrong( "turns( " + n + " )" );
		clock += n * TURN;
		return this;
	}

	/** A beat passes: what follows comes right after what came before. */
	public Demo beat(){
		clock += BEAT;
		return this;
	}

	// ------------------------------------------------------------------ what creatures do

	/** Walks cell by cell through (x, y) pairs, its walk playing. */
	public Demo move( String id, int... xy ){
		return step( Op.MOVE, actor( id ), path( xy ) );
	}

	/** Thrown straight to (x, y), sliding, no walk (a knockback). */
	public Demo knock( String id, int x, int y ){
		return step( Op.KNOCK, actor( id ), new int[]{ standing( x, y ) } );
	}

	/** Jumps to (x, y) in an arc. */
	public Demo jump( String id, int x, int y ){
		return step( Op.JUMP, actor( id ), new int[]{ standing( x, y ) } );
	}

	/** Strikes toward (x, y): its attack, played once. */
	public Demo attack( String id, int x, int y ){
		return step( Op.ATTACK, actor( id ), new int[]{ cell( x, y ) } );
	}

	/** Zaps toward (x, y). */
	public Demo zap( String id, int x, int y ){
		return step( Op.ZAP, actor( id ), new int[]{ cell( x, y ) } );
	}

	/** Works at (x, y) (its operate animation). */
	public Demo operate( String id, int x, int y ){
		return step( Op.OPERATE, actor( id ), new int[]{ cell( x, y ) } );
	}

	// ------------------------------------------------------------------ what creatures send flying

	//A shot goes from the middle of who sends it to the middle of whoever stands on (x, y), else to
	//the cell's raised middle, as the game aims. Its animation is a step of its own (zap(), attack(),
	//anim()): a bolt, a thrown item or a chain lands a beat later, with the number a beat() puts there;
	//a ray and lightning strike at once.

	/** Its bolt flies to (x, y) as the game's of this MagicMissile type flies (a wand's MAGIC_MISSILE,
	 *  a red shaman's SHAMAN_RED...). */
	public Demo bolt( String id, int type, int x, int y ){
		if (type < 0 || type >= MagicMissile.MAGIC_MISS_CONE) throw wrong( id + ": " + type + " is no bolt of MagicMissile's" );
		return shoot( id, new Shot( Shot.Kind.BOLT, type, null, null ), x, y );
	}

	/** Throws an item to (x, y), turning as the game throws it (MissileSprite: a dart flies point first). */
	public Demo toss( String id, Class<? extends Item> item, int x, int y ){
		if (item == null) throw wrong( id + ": toss() needs an item class" );
		return shoot( id, new Shot( Shot.Kind.THROWN, 0, item, null ), x, y );
	}

	/** A ray from it to (x, y): Effects.Type.DEATH_RAY (an evil eye's), LIGHT_RAY or HEALTH_RAY (a necromancer's). */
	public Demo ray( String id, Effects.Type ray, int x, int y ){
		if (ray != Effects.Type.DEATH_RAY && ray != Effects.Type.LIGHT_RAY && ray != Effects.Type.HEALTH_RAY){
			throw wrong( id + ": " + ray + " is no ray" );
		}
		return shoot( id, new Shot( Shot.Kind.RAY, 0, null, ray ), x, y );
	}

	/** Lightning from it to (x, y). */
	public Demo lightning( String id, int x, int y ){
		return shoot( id, new Shot( Shot.Kind.LIGHTNING, 0, null, null ), x, y );
	}

	/** Its chain thrown to (x, y), then pulled back. */
	public Demo chain( String id, int x, int y ){
		return shoot( id, new Shot( Shot.Kind.CHAIN, 0, null, null ), x, y );
	}

	private Demo shoot( String id, Shot shot, int x, int y ){
		return add( new Step( Op.SHOOT, clock, actor( id ), new int[]{ cell( x, y ) }, 0, false, null, null, null, null, null, null, shot ) );
	}

	/** Plays an animation of its sprite's own, by its field's name (GooSprite's "pump"): a looped one runs
	 *  until the next, the rest play once and it idles again. */
	public Demo anim( String id, String field ){
		if (field == null || field.trim().isEmpty()) throw wrong( id + ": anim() needs a field's name" );
		return add( new Step( Op.ANIM, clock, actor( id ), new int[0], 0, false, field, null, null, null, null, null ) );
	}

	/** Idles again (a looped animation stops). */
	public Demo idle( String id ){
		return step( Op.IDLE, actor( id ), new int[0] );
	}

	/** Turns to face (x, y). */
	public Demo face( String id, int x, int y ){
		return step( Op.FACE, actor( id ), new int[]{ cell( x, y ) } );
	}

	/** Takes damage: it flashes, and the number rises over it in the game's red. */
	public Demo hurt( String id, int damage ){
		if (damage < 1) throw wrong( id + ": hurt( " + damage + " )" );
		return add( new Step( Op.HURT, clock, actor( id ), new int[0], damage, false, null, null, null, null, null, null ) );
	}

	/** Heals: the number rises over it in green. */
	public Demo heal( String id, int amount ){
		if (amount < 1) throw wrong( id + ": heal( " + amount + " )" );
		return add( new Step( Op.HEAL, clock, actor( id ), new int[0], amount, false, null, null, null, null, null, null ) );
	}

	/** A word rises over it in a colour (CharSprite.WARNING...): this demo's own key, or the game's
	 *  ("actors.char.def_verb" is "dodged"). */
	public Demo status( String id, String key, int color ){
		if (key == null || key.trim().isEmpty()) throw wrong( id + ": status() needs a key" );
		return add( new Step( Op.STATUS, clock, actor( id ), new int[0], color, false, key, null, null, null, null, null ) );
	}

	/** A hit card's icon rises over it, as the game shows one where a word used to float: a miss's
	 *  dodge (FloatingText.MISS_DODGE), a parry (PARRY), immunity (IMMUNE), a buff taking hold
	 *  (-(BuffIndicator icon + 1)), a skill (HitCard.SKILL_BASE + its image). */
	public Demo chip( String id, int code ){
		return add( new Step( Op.CHIP, clock, actor( id ), new int[0], code, false, null, null, null, null, null, null ) );
	}

	/** Struck by surprise: the game's own exclamation over it. */
	public Demo surprise( String id ){
		return step( Op.SURPRISE, actor( id ), new int[0] );
	}

	/** Dies: its death plays, then it fades away. */
	public Demo die( String id ){
		return step( Op.DIE, actor( id ), new int[0] );
	}

	/** An interface icon over its head (Icons.SLEEP, ALERT, LOST...), in place of any before. */
	public Demo icon( String id, Icons icon ){
		if (icon == null) throw wrong( id + ": no icon" );
		return add( new Step( Op.ICON, clock, actor( id ), new int[0], 0, false, null, null, null, icon, null, null ) );
	}

	/** A buff's own small icon over its head, in its tint, in place of any before. */
	public Demo buff( String id, Class<? extends Buff> buff ){
		return buff( id, buff, -1 );
	}

	/** A buff over its head drawn with this BuffIndicator icon (for a buff whose own icon depends on its state). */
	public Demo buff( String id, Class<? extends Buff> buff, int icon ){
		if (buff == null) throw wrong( id + ": no buff class" );
		return add( new Step( Op.BUFF, clock, actor( id ), new int[0], icon, false, null, null, null, null, null, buff ) );
	}

	/** Takes the icon over its head away. */
	public Demo clear( String id ){
		return step( Op.CLEAR, actor( id ), new int[0] );
	}

	/** Stops dead where it stands (paralysed, stunned, frozen): its animation holds. */
	public Demo freeze( String id ){
		return step( Op.FREEZE, actor( id ), new int[0] );
	}

	/** Moves again. */
	public Demo thaw( String id ){
		return step( Op.THAW, actor( id ), new int[0] );
	}

	/** Puts a look on its sprite, or takes it off. */
	public Demo look( String id, Look look, boolean on ){
		if (look == null) throw wrong( id + ": no look" );
		return add( new Step( Op.LOOK, clock, actor( id ), new int[0], 0, on, null, null, null, null, look, null ) );
	}

	/** Shows a creature or a prop (an item dropped, a plant come up). */
	public Demo show( String id ){
		return step( Op.SHOW, thing( id ), new int[0] );
	}

	/** Hides a creature or a prop (an item picked up). Before the first turn: it starts hidden. */
	public Demo hide( String id ){
		return step( Op.HIDE, thing( id ), new int[0] );
	}

	// ------------------------------------------------------------------ what happens to the cells

	/** The game's particles burst once on each of these cells. */
	public Demo burst( GuideEffect effect, int... xy ){
		if (effect == null) throw wrong( "burst() needs an effect" );
		return add( new Step( Op.BURST, clock, null, cells( xy, false ), 0, false, null, effect, null, null, null, null ) );
	}

	/** The game's particles pour on each of these cells until stop(). */
	public Demo pour( GuideEffect effect, int... xy ){
		if (effect == null) throw wrong( "pour() needs an effect" );
		return add( new Step( Op.POUR, clock, null, cells( xy, false ), 0, false, null, effect, null, null, null, null ) );
	}

	/** What pours on these cells stops; on every cell, with none named. */
	public Demo stop( int... xy ){
		return add( new Step( Op.STOP, clock, null, cells( xy, true ), 0, false, null, null, null, null, null, null ) );
	}

	/** Marks these cells. */
	public Demo mark( Mark mark, int... xy ){
		if (mark == null) throw wrong( "mark() needs a mark" );
		return add( new Step( Op.MARK, clock, null, cells( xy, false ), 0, false, null, null, mark, null, null, null ) );
	}

	/** Takes the marks off these cells; off every cell, with none named. */
	public Demo unmark( int... xy ){
		return add( new Step( Op.UNMARK, clock, null, cells( xy, true ), 0, false, null, null, null, null, null, null ) );
	}

	/** An arrow from (x0, y0) to (x1, y1), along a row, a column or a diagonal, in a mark's colour. */
	public Demo arrow( Mark mark, int x0, int y0, int x1, int y1 ){
		if (mark == null) throw wrong( "arrow() needs a mark" );
		int dx = x1 - x0, dy = y1 - y0;
		if ((dx == 0 && dy == 0) || (dx != 0 && dy != 0 && Math.abs( dx ) != Math.abs( dy ))){
			throw wrong( "an arrow runs along a row, a column or a diagonal" );
		}
		return add( new Step( Op.ARROW, clock, null, new int[]{ cell( x0, y0 ), cell( x1, y1 ) }, 0, false, null, null, mark, null, null, null ) );
	}

	/** Takes every arrow away. */
	public Demo unarrow(){
		return add( new Step( Op.UNARROW, clock, null, new int[0], 0, false, null, null, null, null, null, null ) );
	}

	/** These cells become another terrain (a door opens, a hidden one is found, grass is trampled). */
	public Demo terrain( int terrain, int... xy ){
		if (!drawable( terrain )) throw wrong( "terrain " + terrain + " is no terrain a stage can draw" );
		return add( new Step( Op.TERRAIN, clock, null, cells( xy, false ), terrain, false, null, null, null, null, null, null ) );
	}

	/** The line under the stage: one of this demo's own words (guidedemo.<page>.<key> in its group's bundle). */
	public Demo caption( String key ){
		if (key == null || key.trim().isEmpty() || absolute( key )){
			throw wrong( "a caption is a key of the demo's own, with no dot" );
		}
		return add( new Step( Op.CAPTION, clock, null, new int[0], 0, false, key, null, null, null, null, null ) );
	}

	/** No line under the stage. */
	public Demo uncaption(){
		return add( new Step( Op.CAPTION, clock, null, new int[0], 0, false, null, null, null, null, null, null ) );
	}

	// ------------------------------------------------------------------ checks

	private Demo actor( Actor a ){
		actors.put( a.id, a );
		return this;
	}

	private Demo prop( Prop p ){
		props.put( p.id, p );
		return this;
	}

	private Demo step( Op op, String who, int[] cells ){
		return add( new Step( op, clock, who, cells, 0, false, null, null, null, null, null, null ) );
	}

	private Demo add( Step s ){
		steps.add( s );
		return this;
	}

	private String fresh( String id ){
		if (map == null) throw wrong( "stage() comes before who stands on it" );
		if (id == null || id.trim().isEmpty()) throw wrong( "a blank id" );
		if (actors.containsKey( id ) || props.containsKey( id )) throw wrong( "two things called " + id );
		return id;
	}

	private String actor( String id ){
		if (!actors.containsKey( id )) throw wrong( "no creature called " + id );
		return id;
	}

	private String thing( String id ){
		if (!actors.containsKey( id ) && !props.containsKey( id )) throw wrong( "nothing called " + id );
		return id;
	}

	private int cell( int x, int y ){
		if (map == null) throw wrong( "stage() comes before its cells" );
		if (x < 0 || y < 0 || x >= width || y >= height) throw wrong( "(" + x + ", " + y + ") is off the stage" );
		return x + y * width;
	}

	//where a creature can stand: its head rises a cell over its feet, so never the top row
	private int standing( int x, int y ){
		int c = cell( x, y );
		if (y == 0) throw wrong( "(" + x + ", " + y + "): a creature on the top row would stand out of the stage" );
		return c;
	}

	private int[] path( int[] xy ){
		if (xy == null || xy.length == 0 || xy.length % 2 != 0) throw wrong( "move() takes (x, y) pairs" );
		int[] out = new int[xy.length / 2];
		for (int i = 0; i < out.length; i++) out[i] = standing( xy[2*i], xy[2*i + 1] );
		return out;
	}

	private int[] cells( int[] xy, boolean noneIsAll ){
		if (xy == null || xy.length == 0){
			if (noneIsAll) return new int[0];
			throw wrong( "no cells" );
		}
		if (xy.length % 2 != 0) throw wrong( "cells are (x, y) pairs" );
		int[] out = new int[xy.length / 2];
		for (int i = 0; i < out.length; i++) out[i] = cell( xy[2*i], xy[2*i + 1] );
		return out;
	}

	private IllegalArgumentException wrong( String why ){
		return new IllegalArgumentException( key + " (demo " + index + "): " + why );
	}
}
