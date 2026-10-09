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

package xyz.gabriwar.warpedpixeldungeon.levels.ambience;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.RegularLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.particles.Emitter;

import java.util.List;
import java.util.Locale;

import static xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.DAWN;
import static xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.DAY;
import static xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.DUSK;
import static xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.NIGHT;
import static xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season.AUTUMN;
import static xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season.SPRING;
import static xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season.SUMMER;
import static xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSound.*;
import static xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome.BEACH;
import static xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome.DESERT;
import static xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome.FOREST;
import static xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome.MEADOW;
import static xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome.PLAINS;
import static xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome.SNOWFIELD;
import static xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome.SWAMP;
import static xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome.TUNDRA;

/**
 * The sounds of a place (docs/ambience.md, "The sounds of each place"): its voices, each heard
 * now and then from somewhere near the hero - a drop into the water, a frog on the bank, a rat
 * far off in the dark - or on and on while he is near what makes it (water spilling from a
 * pipe, a torch, the cold lava), on the ambience channel, falling off with distance and panned
 * to its side. The hour, the season and the weather keep some in or let them out. Scheduled by
 * a Soundscape, at most three at once.
 *
 * A Gizmo GameScene adds on every floor that has a place, the cave slices aside (CaveLife has
 * their sounds). Render thread: it rolls AmbientPlayer's dice and only reads the level.
 */
public final class AmbientSounds extends Gizmo {

	//the hour and the weather are looked at again this often (seconds)
	private static final float AIR_EVERY = 1f;
	//a frame longer than this (a hitch, the floor loading) counts as only this long
	private static final float MAX_FRAME = 0.25f;
	//an echo comes back from the other side, not as wide
	private static final float ECHO_PAN = 0.6f;
	//the prison's chains: the game's own CHAINS, a little under a creak's mix, and its length
	static final float CHAINS_GAIN = 0.12f, CHAINS_LENGTH = 0.7f;

	private final Level level;
	private final Place place;
	final Soundscape scape;
	/** where the voices go out: the ambience channel (a list in the tests) */
	Soundscape.Out out = new Channel();
	private final Soundscape.Air air = new Soundscape.Air();
	private final Soundscape.Ground ground = new Soundscape.Ground();
	private float airIn;
	//the overworld window's origin the cells kept were found in
	private int wx, wy;
	//the sounds ticking on the floor shown: the critters ask it whether the place is full (full())
	private static AmbientSounds heard;

	private AmbientSounds( Level level, Place place ){
		this.level = level;
		this.place = place;
		scape = new Soundscape( voices( place ), AmbientPlayer.RNG );
		if (level instanceof RegularLevel){
			ground.secret = secretCells( ((RegularLevel) level).rooms(), level.width(), level.height() );
		}
		if (level instanceof OverworldLevel){
			OverworldLevel ow = (OverworldLevel) level;
			ground.world = ow;
			wx = ow.worldX();
			wy = ow.worldY();
		}
	}

	/** The sounds of the level's place, or null for a floor with none (and the cave slices, CaveLife's). */
	public static AmbientSounds forLevel( Level level ){
		Place p = level == null ? null : level.ambience();
		return p == null || p == Place.CAVE_SLICES ? null : new AmbientSounds( level, p );
	}

	@Override
	public void update(){
		super.update();
		if (Dungeon.level != level) return;
		heard = this;
		float dt = Math.min( Game.elapsed, MAX_FRAME );
		if (Dungeon.hero == null || !Dungeon.hero.isAlive() || !AmbientPlayer.audible()){
			scape.hush( dt );
			return;
		}
		//time stands still (the hourglass, swiftthistle), and so does the place's life
		if (Emitter.freezeEmitters) return;
		//the overworld's window is moving: its map, the sight and the hero's cell are not all in one frame
		if (level.fogHeld()) return;
		follow();
		if ((airIn -= dt) <= 0f){
			airIn = AIR_EVERY;
			readAir();
		}
		ground.set( level.map, level.width(), level.height(), Dungeon.hero.pos, level.heroFOV );
		ground.visited = level.visited;
		ground.mapped = level.mapped;
		//a critter's croak or squeak is one of the three
		scape.others = DungeonCritterSprite.voiceBusy() ? 1 : 0;
		scape.tick( dt, ground, air, out );
	}

	@Override
	public void destroy(){
		if (heard == this) heard = null;
		super.destroy();
	}

	/**
	 * Do the place's own voices hold all three? A critter's sound then waits
	 * (DungeonCritterSprite.voiceAt): the cap holds both ways, as the place waits for a critter's.
	 */
	public static boolean full(){
		AmbientSounds a = heard;
		return a != null && a.level == Dungeon.level && a.scape.full();
	}

	/**
	 * DebugScenes' ambience scenes: what the level's place sounds like, its voices heard at this
	 * hour and in this weather first, a bed (with the other sound it may play) marked as one.
	 */
	public static String showcase( Level level ){
		AmbientSounds s = forLevel( level );
		if (s == null || Dungeon.hero == null) return "no sounds of its own here";
		s.readAir();
		StringBuilder now = new StringBuilder(), not = new StringBuilder();
		for (Voice v : s.scape.voices){
			StringBuilder to = v.allowed( s.air ) ? now : not;
			to.append( to.length() == 0 ? "" : ", " ).append( name( v ) );
			if (v.alt != null) to.append( '/' ).append( name( v.alt ) );
			if (v.bed) to.append( " (bed)" );
		}
		return "sounds now: " + (now.length() == 0 ? "none" : now)
				+ (not.length() == 0 ? "" : "; not at this hour or in this weather: " + not);
	}

	private static String name( Voice v ){
		if (v.sound != null) return v.sound.name().toLowerCase( Locale.ENGLISH );
		String t = v.takes[0];
		return t.substring( t.lastIndexOf( '/' ) + 1, t.lastIndexOf( '.' ) );
	}

	/** The cells of the secret rooms among these, rim and all; null for none. */
	static boolean[] secretCells( List<Room> rooms, int w, int h ){
		boolean[] out = null;
		for (Room r : rooms){
			if (!(r instanceof SecretRoom)) continue;
			if (out == null) out = new boolean[w * h];
			for (int y = Math.max( 0, r.top ); y <= Math.min( h - 1, r.bottom ); y++){
				for (int x = Math.max( 0, r.left ); x <= Math.min( w - 1, r.right ); x++) out[x + y * w] = true;
			}
		}
		return out;
	}

	//the overworld's window moved under the sources: the cells kept go with it
	private void follow(){
		if (ground.world == null) return;
		int x = ground.world.worldX(), y = ground.world.worldY();
		if (x == wx && y == wy) return;
		scape.shift( x - wx, y - wy, level.width(), level.height() );
		wx = x;
		wy = y;
	}

	private void readAir(){
		air.phase = DayNightCycle.phase();
		air.season = GameCalendar.season();
		air.storm = ClimateManager.isStorming();
		air.rain = ClimateManager.localPrecipType() == PrecipType.NONE ? 0f : ClimateManager.localPrecipRate();
		air.wind = ClimateManager.localWindSpeed();
		air.temp = ClimateManager.localTemp();
		air.depth = Dungeon.depth;
		air.branch = Dungeon.branch;
		air.biome = place == Place.SURFACE ? ground.world.biomeAtCell( Dungeon.hero.pos ) : null;
	}

	//the ambience channel, the take and the pitch as the scheduler picked them: from a cell,
	//falling off and panned; from the air, centred; an echo, from the other side
	private static final class Channel implements Soundscape.Out {
		@Override
		public void play( Voice v, int take, int cell, float level, float pitch, boolean echo ){
			float pan = 0f;
			if (cell >= 0){
				float l = AmbientPlayer.levelAt( cell );
				if (l <= 0f) return;
				level *= l;
				pan = echo ? -AmbientPlayer.panAt( cell ) * ECHO_PAN : AmbientPlayer.panAt( cell );
			}
			AmbientPlayer.playTake( v.takes[take], v.gain, level, pitch, pan );
		}
	}

	// ------------------------------------------------------------ the places

	/** The voices of a place: none for the cave slices, whose sounds are CaveLife's. */
	static Voice[] voices( Place p ){
		switch (p){
			case SEWERS:   return sewers();
			case PRISON:   return prison();
			case CAVES:    return caves();
			case CITY:     return city();
			case HALLS:    return halls();
			case FROZEN:   return frozen();
			case NEST:     return nest();
			case VAULT:    return vault();
			case TEMPLE:   return temple();
			case MINES:    return mines();
			case MEADOW:   return meadow();
			case SHORE:    return shore();
			case CATACOMB: return catacomb();
			case SURFACE:  return surface();
			case PEAKS:    return peaks();
			default:       return new Voice[0];
		}
	}

	private static Voice v( AmbientSound s ){
		return new Voice( s );
	}

	//murky water, drain pipes, moss
	private static Voice[] sewers(){
		return new Voice[]{
				v( DRIP ).every( 4f ).level( 0.9f ).pitch( 0.85f, 1.2f ).from( Source.DRIP ).range( 1f, 10f ),
				v( DRIP_METAL ).every( 20f ).level( 0.8f ).pitch( 0.9f, 1.1f ).from( Source.PIPE ).range( 0f, 10f ),
				//water spilling from the pipes, heard on and on near them: one bed, a trickle from
				//the nearest pipe, or a pour from the one pipe in three that pours
				v( TRICKLE ).bed( 4f, 1.45f ).pitch( 0.95f, 1.08f ).from( Source.PIPE )
						.or( v( POUR ).bed( 6f, 1.75f ).pitch( 0.92f, 1.05f ).from( Source.PIPE_POUR ) ),
				v( GURGLE ).every( 16f ).level( 0.8f ).pitch( 0.85f, 1.1f ).from( Source.DRAIN ).range( 1f, 10f ),
				v( FROG ).every( 10f ).level( 0.9f ).pitch( 0.9f, 1.15f ).hours( DUSK, NIGHT )
						.from( Source.SHORE ).range( 2f, 11f ).chorus(),
				v( CRICKET ).every( 9f ).level( 0.75f ).pitch( 0.95f, 1.08f ).hours( NIGHT )
						.from( Source.GRASS ).range( 1f, 10f ).answer(),
				v( RAT ).every( 32f ).level( 0.9f ).pitch( 0.9f, 1.15f ).from( Source.FLOOR ).range( 6f, 14f ).unseen(),
				v( FLY ).every( 28f ).level( 0.75f ).pitch( 0.9f, 1.12f ).hours( DAY ).from( Source.FLOOR ).range( 1f, 4f ),
				//the streets above, heard through the grates of the floors just under the town
				v( BIRD ).every( 26f ).level( 0.5f ).pitch( 0.95f, 1.08f ).hours( DAWN, DAY ).upTo( 2 )
						.from( Source.FLOOR ).range( 3f, 10f )
		};
	}

	//cells, torches, cages over the chasms
	private static Voice[] prison(){
		return new Voice[]{
				v( DRIP ).every( 8f ).level( 0.8f ).pitch( 0.85f, 1.15f ).from( Source.DRIP ).range( 1f, 10f ),
				v( CRACKLE ).bed( 4f, 1.05f ).level( 0.55f ).pitch( 0.95f, 1.08f ).from( Source.TORCH ),
				v( CREAK ).every( 50f ).level( 0.8f ).pitch( 0.85f, 1.05f ).from( Source.CAGE ).range( 1f, 11f ),
				//a cage stirring on its chain: the game's own chains, low and slow
				new Voice( Assets.Sounds.CHAINS, CHAINS_GAIN, CHAINS_LENGTH ).every( 26f ).level( 0.8f ).pitch( 0.6f, 0.8f )
						.from( Source.HANGING, Source.CAGE ).range( 1f, 11f ),
				v( MOAN ).every( 150f ).level( 0.8f ).pitch( 0.85f, 1f ).from( Source.FLOOR ).range( 8f, 15f ).unseen(),
				v( WIND ).every( 20f ).level( 0.55f ).pitch( 0.9f, 1.1f ).from( Source.CHASM ).range( 0f, 10f ),
				v( CRICKET ).every( 15f ).level( 0.6f ).pitch( 0.95f, 1.05f ).hours( NIGHT )
						.from( Source.WALL_BASE ).range( 2f, 11f ),
				v( RAT ).every( 28f ).level( 0.9f ).pitch( 0.9f, 1.15f ).from( Source.FLOOR ).range( 6f, 14f ).unseen(),
				v( SKITTER ).every( 22f ).level( 0.65f ).pitch( 0.9f, 1.15f ).from( Source.WALL_BASE ).range( 2f, 9f )
		};
	}

	//cold water, ore, the dwarves' old scaffolds: every drop and stone thrown back by the rock
	private static Voice[] caves(){
		return new Voice[]{
				v( DRIP ).every( 5f ).level( 0.9f ).pitch( 0.85f, 1.15f ).from( Source.DRIP ).range( 1f, 11f ).echo(),
				v( PEBBLES ).every( 26f ).level( 0.75f ).pitch( 0.85f, 1.1f ).from( Source.ROCK_FACE ).range( 3f, 13f ).echo(),
				v( RUMBLE ).every( 75f ).level( 0.85f ).pitch( 0.85f, 1.05f ),
				v( BAT ).every( 32f ).level( 0.8f ).pitch( 0.95f, 1.1f ).from( Source.ROCK_FACE ).range( 3f, 12f ).echo(),
				//an underground stream: water running by the big pools
				v( TRICKLE ).bed( 5f, 1.45f ).level( 0.85f ).pitch( 0.9f, 1.02f ).from( Source.BIG_WATER ),
				v( CRICKET ).every( 18f ).level( 0.55f ).pitch( 0.82f, 0.92f )
						.from( Source.GRASS, Source.WALL_BASE ).range( 2f, 11f ),
				//a minecart's creak on the tracks over the scaffolds
				v( CREAK ).every( 70f ).level( 0.75f ).pitch( 0.6f, 0.72f ).from( Source.SCAFFOLD ).range( 1f, 12f ),
				v( WIND ).every( 24f ).level( 0.5f ).pitch( 0.9f, 1.1f ).from( Source.CHASM ).range( 0f, 10f )
		};
	}

	//the dwarves' city: a smith somewhere, the green flames, a fountain, doves on the statues
	private static Voice[] city(){
		return new Voice[]{
				v( ANVIL ).every( 70f ).level( 0.6f ).pitch( 0.95f, 1.05f ),
				v( CHIME ).every( 90f ).level( 0.55f ).pitch( 0.95f, 1.05f ),
				//the green flames are dense, their crackle a deeper roar than a torch's
				v( CRACKLE ).bed( 4f, 1.05f ).level( 0.55f ).pitch( 0.72f, 0.8f ).from( Source.FLAME ),
				//a draught through the smoke vents, or along the halls
				v( WIND ).every( 36f ).level( 0.45f ).pitch( 0.95f, 1.1f ).from( Source.VENT, Source.AIR ).range( 1f, 10f ),
				v( TRICKLE ).bed( 4f, 1.45f ).level( 0.7f ).pitch( 1.05f, 1.15f ).from( Source.BIG_WATER ),
				v( COO ).every( 30f ).level( 0.65f ).pitch( 0.95f, 1.05f ).hours( DAWN, DAY )
						.from( Source.STATUE, Source.ROCK_FACE ).range( 2f, 12f ),
				v( CRICKET ).every( 13f ).level( 0.6f ).pitch( 0.95f, 1.08f ).hours( NIGHT )
						.from( Source.GRASS ).range( 1f, 10f ).answer(),
				v( DRIP ).every( 12f ).level( 0.7f ).pitch( 0.85f, 1.15f ).from( Source.DRIP ).range( 1f, 10f )
		};
	}

	//the demon halls: cold lava, embermoss, things in the dark that are not there
	private static Voice[] halls(){
		return new Voice[]{
				//every take bubbles from end to end: the next starts as the last fades into its echo
				v( LAVA ).bed( 6f, 1.85f ).level( 0.85f ).pitch( 0.9f, 1.1f ).from( Source.LAVA ),
				v( CRACKLE ).bed( 4f, 1.05f ).level( 0.5f ).pitch( 0.9f, 1.1f ).from( Source.EMBERS ),
				v( WHISPER ).every( 45f ).level( 0.8f ).pitch( 0.9f, 1.05f ).from( Source.FLOOR ).range( 3f, 12f ).unseen(),
				v( MOAN ).every( 60f ).level( 0.8f ).pitch( 0.8f, 0.95f ).from( Source.FLOOR ).range( 6f, 15f ).unseen(),
				v( RUMBLE ).every( 50f ).level( 0.9f ).pitch( 0.85f, 1f ),
				//a hot wind: the draught, deep and slow
				v( WIND ).every( 30f ).level( 0.5f ).pitch( 0.72f, 0.85f )
		};
	}

	//the frozen branch: wind, ice, meltwater off the seams
	private static Voice[] frozen(){
		return new Voice[]{
				v( WIND ).every( 18f ).level( 0.6f ).pitch( 0.95f, 1.1f ),
				v( ICE ).every( 28f ).level( 0.75f ).pitch( 0.9f, 1.1f ).from( Source.ICE ).range( 1f, 12f ),
				//meltwater: small cold drops off the ice seams, or from the roof
				v( DRIP ).every( 7f ).level( 0.8f ).pitch( 1.05f, 1.25f ).from( Source.MELT, Source.DRIP ).range( 1f, 10f ),
				//a snow bunting
				v( BIRD ).every( 70f ).level( 0.45f ).pitch( 1.05f, 1.15f ).hours( DAWN, DAY ).from( Source.FLOOR ).range( 3f, 11f )
		};
	}

	//the spider nest: legs in the webbing, flies caught in it
	private static Voice[] nest(){
		return new Voice[]{
				v( SKITTER ).every( 9f ).level( 0.75f ).pitch( 0.9f, 1.2f ).from( Source.WEB, Source.WALL_BASE ).range( 1f, 10f ),
				//a fly struggling in the silk: higher and frantic
				v( FLY ).every( 22f ).level( 0.6f ).pitch( 1.05f, 1.25f ).from( Source.WEB ).range( 1f, 10f ),
				v( DRIP ).every( 12f ).level( 0.7f ).pitch( 0.85f, 1.15f ).from( Source.DRIP ).range( 1f, 10f )
		};
	}

	//the dwarves' vault: the city's own, half as often and quieter (a floor to sneak through)
	private static Voice[] vault(){
		Voice[] v = city();
		for (int i = 0; i < v.length; i++) v[i] = v[i].hushed( 2f, 0.6f );
		return v;
	}

	//the temple: still air, old stone
	private static Voice[] temple(){
		return new Voice[]{
				v( CHIME ).every( 45f ).level( 0.5f ).pitch( 0.95f, 1.05f ),
				v( DRIP ).every( 10f ).level( 0.7f ).pitch( 0.9f, 1.1f ).from( Source.DRIP ).range( 1f, 10f ).echo(),
				v( WIND ).every( 30f ).level( 0.4f ).pitch( 0.9f, 1f ).from( Source.CHASM, Source.AIR ).range( 0f, 10f )
		};
	}

	//the kupua mines: dark tunnels, mostly heard
	private static Voice[] mines(){
		return new Voice[]{
				v( DRIP ).every( 6f ).level( 0.85f ).pitch( 0.85f, 1.15f ).from( Source.DRIP ).range( 1f, 10f ).echo(),
				v( PEBBLES ).every( 22f ).level( 0.75f ).pitch( 0.85f, 1.1f ).from( Source.ROCK_FACE ).range( 2f, 12f ).echo(),
				v( RUMBLE ).every( 55f ).level( 0.85f ).pitch( 0.85f, 1.05f ),
				v( BAT ).every( 30f ).level( 0.8f ).pitch( 0.95f, 1.1f ).from( Source.ROCK_FACE ).range( 2f, 12f ).echo()
		};
	}

	//the postgame's open field
	private static Voice[] meadow(){
		return new Voice[]{
				v( BIRD ).every( 9f ).level( 0.75f ).pitch( 0.92f, 1.1f ).hours( DAWN, DAY )
						.from( Source.TREE, Source.GRASS ).range( 2f, 12f ),
				v( CRICKET ).every( 6f ).level( 0.7f ).pitch( 0.95f, 1.08f ).hours( NIGHT )
						.from( Source.GRASS ).range( 1f, 11f ).answer(),
				v( FLY ).every( 25f ).level( 0.6f ).pitch( 0.9f, 1.12f ).hours( DAY ).from( Source.GRASS, Source.FLOOR ).range( 1f, 4f ),
				v( WIND ).every( 25f ).level( 0.45f ).pitch( 0.95f, 1.1f )
		};
	}

	//the postgame's fishing shore
	private static Voice[] shore(){
		return new Voice[]{
				//a fish breaking the water
				v( SPLASH ).every( 14f ).level( 0.75f ).pitch( 0.9f, 1.15f ).from( Source.BIG_WATER ).range( 2f, 12f ),
				//the water lapping at the edge, often twice
				v( LAP ).every( 5f ).level( 0.55f ).pitch( 0.9f, 1.1f ).from( Source.LAPPING ).range( 1f, 9f ).answer(),
				v( WIND ).every( 18f ).level( 0.5f ).pitch( 0.95f, 1.1f ),
				v( BIRD ).every( 45f ).level( 0.5f ).pitch( 0.95f, 1.1f ).hours( DAWN, DAY )
		};
	}

	//the postgame's catacombs
	private static Voice[] catacomb(){
		return new Voice[]{
				v( DRIP ).every( 7f ).level( 0.85f ).pitch( 0.85f, 1.15f ).from( Source.DRIP ).range( 1f, 10f ),
				v( MOAN ).every( 50f ).level( 0.75f ).pitch( 0.85f, 1f ).from( Source.FLOOR ).range( 8f, 15f ).unseen(),
				v( WHISPER ).every( 40f ).level( 0.75f ).pitch( 0.9f, 1.05f ).from( Source.FLOOR ).range( 3f, 12f ).unseen(),
				v( SKITTER ).every( 18f ).level( 0.6f ).pitch( 0.9f, 1.15f ).from( Source.WALL_BASE ).range( 2f, 9f ),
				v( RAT ).every( 24f ).level( 0.9f ).pitch( 0.9f, 1.15f ).from( Source.FLOOR ).range( 6f, 14f ).unseen()
		};
	}

	//the overworld's surface: by the biome where the hero stands, the hour, the season, the weather
	private static Voice[] surface(){
		return new Voice[]{
				v( BIRD ).every( 8f ).level( 0.75f ).pitch( 0.92f, 1.1f ).hours( DAWN, DAY ).biomes( FOREST, MEADOW, PLAINS )
						.weather( Voice.Weather.CALM ).from( Source.TREE, Source.GRASS ).range( 2f, 13f ),
				v( COO ).every( 28f ).level( 0.6f ).pitch( 0.95f, 1.05f ).hours( DAWN, DAY ).biomes( FOREST, MEADOW, PLAINS )
						.weather( Voice.Weather.CALM ).from( Source.TREE, Source.FLOOR ).range( 3f, 12f ),
				//crickets want a warm evening
				v( CRICKET ).every( 5f ).level( 0.6f ).pitch( 0.95f, 1.08f ).hours( DUSK, NIGHT ).seasons( SPRING, SUMMER, AUTUMN )
						.minTemp( 10f ).weather( Voice.Weather.CALM ).from( Source.GRASS ).range( 1f, 11f ).answer(),
				//frogs by the water at night, at any hour in the rain, day and night in the swamps
				v( FROG ).every( 9f ).level( 0.85f ).pitch( 0.9f, 1.15f ).hours( NIGHT ).rainLifts().anyHourIn( SWAMP )
						.seasons( SPRING, SUMMER, AUTUMN ).minTemp( 5f ).from( Source.SHORE ).range( 2f, 12f ).chorus(),
				//rivers and lakes lapping at their banks (ice has none)
				v( LAP ).every( 6f ).level( 0.55f ).pitch( 0.9f, 1.1f ).from( Source.LAPPING ).range( 1f, 9f ).answer(),
				//the wind over open ground when it blows, until the weather's gusts take over
				v( WIND ).every( 14f ).level( 0.55f ).pitch( 0.95f, 1.1f ).biomes( PLAINS, TUNDRA, SNOWFIELD, DESERT, BEACH )
						.weather( Voice.Weather.WINDY )
		};
	}

	//the mountains: always the wind, the weather's gusts and gale once it really blows
	private static Voice[] peaks(){
		return new Voice[]{
				v( WIND ).every( 10f ).level( 0.6f ).pitch( 0.95f, 1.1f ).weather( Voice.Weather.GUSTY ),
				v( PEBBLES ).every( 30f ).level( 0.65f ).pitch( 0.85f, 1.1f ).from( Source.ROCK_FACE ).range( 3f, 13f ).echo(),
				//a bird of prey's far cry
				v( BIRD ).every( 60f ).level( 0.45f ).pitch( 0.75f, 0.9f ).hours( DAWN, DAY ).weather( Voice.Weather.CALM ),
				v( ICE ).every( 35f ).level( 0.6f ).pitch( 0.9f, 1.1f ).from( Source.FROST ).range( 1f, 11f )
		};
	}
}
