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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.TemperingAnvilSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

/**
 * The anvil of the Tempering Forge. It is furniture with a memory: it holds one piece of
 * metal, reads the temperature of the tile it stands on every turn (TileTemperature, the
 * same number the debug heat overlay shows), and walks the piece through a smith's work:
 *
 *   cold      the tile must hold WORK_TEMP for SOAK_TURNS turns in a row
 *   hot       white-hot and workable - and overcooking: past OVERHEAT_TURNS turns of it a
 *             rarity line burns out of the metal (Quality.scorch)
 *   quenched  either the tile's temperature crashes QUENCH_DROP degrees in one turn (frost,
 *             however it is made), or the hero quenches it by hand in the trough, which
 *             only takes if the trough's water is liquid and no warmer than TROUGH_MAX
 *
 * A clean quench is Quality.temper: the weakest rolled line goes to its maximum, or, with
 * nothing left to raise, the piece takes a masterwork step for free. One per anvil.
 * Nothing here knows about seasons: winter makes the heat hard to reach and the trough
 * cold (or solid), summer the reverse, only because the numbers it reads say so.
 */
public class TemperingAnvil extends NPC {

	{
		spriteClass = TemperingAnvilSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	public static final int EMPTY = 0, COLD = 1, HOT = 2;

	public static final float WORK_TEMP   = 80f;
	public static final float QUENCH_DROP = 50f;
	public static final float TROUGH_MAX  = 25f;
	private static final int SOAK_TURNS     = 3;
	private static final int OVERHEAT_TURNS = 14;

	private Item held = null;
	private int state = EMPTY;
	private int soak = 0;
	private int overheat = 0;
	private int cooling = 0;
	private float lastTemp = Float.NaN;
	private boolean spent = false;

	/** the quench trough's cells, handed over by the room */
	public int[] trough = new int[0];

	/** what the sprite should show: 0 bare, 1 a cold piece, 2 a glowing one, 3 cracked */
	public int visualState(){
		if (spent && held == null) return 3;
		return state;
	}

	private void show(){
		if (sprite instanceof TemperingAnvilSprite) ((TemperingAnvilSprite) sprite).show( visualState() );
	}

	private boolean seen(){
		return Dungeon.level.heroFOV[pos];
	}

	// ---------------------------------------------------------------- the work

	@Override
	protected boolean act(){
		workTurn();
		return super.act();
	}

	/** one turn of the forge's work on whatever lies on the anvil */
	public void workTurn(){
		float temp = TileTemperature.tileTemp( pos );
		if (held != null){
			if (state == COLD){
				soak = temp >= WORK_TEMP ? soak + 1 : Math.max( 0, soak - 1 );
				if (soak >= SOAK_TURNS){
					state = HOT;
					overheat = 0;
					cooling = 0;
					show();
					if (seen()){
						SpatialSound.play( Assets.Sounds.BURNING, pos );
						GLog.p( Messages.get( this, "white_hot", held.name() ) );
					}
				}
			} else if (state == HOT){
				if (!Float.isNaN( lastTemp ) && lastTemp - temp >= QUENCH_DROP){
					quench( null );
				} else if (temp >= WORK_TEMP){
					cooling = 0;
					overheat++;
					if (overheat == OVERHEAT_TURNS - 4 && seen()){
						GLog.w( Messages.get( this, "overheating", held.name() ) );
					} else if (overheat > OVERHEAT_TURNS){
						overcook();
					}
				} else if (++cooling >= 3){
					//it came off the heat and nobody quenched it: annealed soft, nothing gained
					state = COLD;
					soak = 0;
					show();
					if (seen()) GLog.i( Messages.get( this, "cooled", held.name() ) );
				}
			}
		}
		lastTemp = temp;
	}

	public int state(){
		return state;
	}

	public Item held(){
		return held;
	}

	public boolean spent(){
		return spent;
	}

	private void overcook(){
		boolean lost = MasterworkCore.quality( held ).scorch( held );
		state = COLD;
		soak = 0;
		show();
		if (seen()){
			CellEmitter.get( pos ).burst( Speck.factory( Speck.SMOKE ), 6 );
			SpatialSound.play( Assets.Sounds.DEGRADE, pos );
			GLog.n( Messages.get( this, lost ? "scorched" : "blackened", held.name() ) );
		}
	}

	//a clean quench. quencher is the hero who did it by hand, or null when the tile's own
	//temperature crashed under the piece (then it is left beside the anvil)
	private void quench( Hero quencher ){
		Item piece = held;
		int gain = MasterworkCore.quality( piece ).temper( piece );
		held = null;
		state = EMPTY;
		spent = true;
		show();

		if (seen()){
			CellEmitter.get( pos ).burst( Speck.factory( Speck.STEAM ), 12 );
			SpatialSound.play( Assets.Sounds.GAS, pos );
			SpatialSound.play( Assets.Sounds.EVOKE, pos );
		}
		GLog.p( Messages.get( this, gain == 2 ? "tempered_step" : "tempered_line", piece.name() ) );

		if (quencher != null && piece.doPickUp( quencher )){
			return;
		}
		Heap left = Dungeon.level.drop( piece, freeBeside() );
		if (left.sprite != null) left.sprite.drop();
	}

	private int freeBeside(){
		for (int ofs : PathFinder.NEIGHBOURS8){
			int c = pos + ofs;
			if (Dungeon.level.insideMap( c ) && Dungeon.level.passable[c] && Actor.findChar( c ) == null){
				return c;
			}
		}
		return pos;
	}

	// ---------------------------------------------------------------- the hero's hands

	@Override
	public boolean interact( Char c ){
		if (NetDialogs.handleNetHero( c, Messages.get( this, "net_host_only" ) )) return true;
		if (c != Dungeon.hero) return true;
		final Hero hero = (Hero) c;

		Game.runOnRenderThread( new Callback(){
			@Override
			public void call(){
				if (held == null){
					if (spent){
						GLog.i( Messages.get( TemperingAnvil.class, "spent" ) );
					} else {
						GameScene.selectItem( placing );
					}
					return;
				}
				final boolean hot = state == HOT;
				String[] options = hot
						? new String[]{ Messages.get( TemperingAnvil.class, "opt_quench" ),
										Messages.get( TemperingAnvil.class, "opt_take" ) }
						: new String[]{ Messages.get( TemperingAnvil.class, "opt_take" ) };
				GameScene.show( new WndOptions( sprite(),
						Messages.titleCase( name() ),
						Messages.get( TemperingAnvil.class, hot ? "menu_hot" : "menu_cold", held.name() ),
						options ){
					@Override
					protected void onSelect( int index ){
						hero.sprite.operate( pos );
						hero.spendAndNext( 1f );
						if (hot && index == 0) quenchInTrough( hero );
						else takeBack( hero );
					}
				} );
			}
		} );
		return true;
	}

	private final WndBag.ItemSelector placing = new WndBag.ItemSelector(){
		@Override
		public String textPrompt(){
			return Messages.get( TemperingAnvil.class, "place_prompt" );
		}
		@Override
		public Class<? extends Bag> preferredBag(){
			return Belongings.Backpack.class;
		}
		@Override
		public boolean itemSelectable( Item item ){
			return workable( item );
		}
		@Override
		public void onSelect( Item item ){
			if (item == null || !place( Dungeon.hero, item )) return;
			Dungeon.hero.sprite.operate( pos );
			Dungeon.hero.spendAndNext( 1f );
			SpatialSound.play( Assets.Sounds.HIT_PARRY, pos, 1f, 0.8f );
			GLog.i( Messages.get( TemperingAnvil.class, "placed", held.name() ) );
		}
	};

	/** takes the piece out of the hero's pack and lays it on the anvil, cold */
	public boolean place( Hero hero, Item item ){
		if (held != null || spent || !workable( item )) return false;
		held = item.detach( hero.belongings.backpack );
		if (held == null) return false;
		state = COLD;
		soak = overheat = cooling = 0;
		show();
		return true;
	}

	//metal only, off the body, and with something left for the forge to give it
	private static boolean workable( Item item ){
		Quality.Family family = Quality.family( item );
		if (family != Quality.Family.WEAPON && family != Quality.Family.ARMOR
				&& family != Quality.Family.MISSILE) return false;
		if (item.isEquipped( Dungeon.hero ) || item.unique) return false;
		Quality q = Quality.of( item );
		return q == null || q.temperGain() > 0;
	}

	public void takeBack( Hero hero ){
		if (held == null) return;
		Item piece = held;
		boolean wasHot = state == HOT;
		held = null;
		state = EMPTY;
		show();
		if (wasHot){
			//nobody said to use your hands
			Buff.affect( hero, Burning.class ).reignite( hero );
			GLog.w( Messages.get( this, "burnt_hands", piece.name() ) );
		}
		if (!piece.doPickUp( hero )){
			Dungeon.level.drop( piece, hero.pos ).sprite.drop();
		}
	}

	public void quenchInTrough( Hero hero ){
		if (held == null || state != HOT) return;

		//the coolest liquid cell of the trough is the one a smith would reach for
		float coolest = Float.NaN;
		boolean frozen = false;
		for (int cell : trough){
			if (Dungeon.level.map[cell] == Terrain.FROZEN_WATER) frozen = true;
			if (Dungeon.level.map[cell] != Terrain.WATER) continue;
			float t = TileTemperature.tileTemp( cell );
			if (Float.isNaN( coolest ) || t < coolest) coolest = t;
		}

		if (Float.isNaN( coolest )){
			GLog.w( Messages.get( this, frozen ? "trough_frozen" : "trough_dry" ) );
			return;
		}
		if (coolest > TROUGH_MAX){
			//warm water draws the heat out too slowly to set anything
			state = COLD;
			soak = 0;
			show();
			if (seen()) CellEmitter.get( pos ).burst( Speck.factory( Speck.STEAM ), 4 );
			GLog.w( Messages.get( this, "trough_warm", held.name() ) );
			return;
		}
		quench( hero );
	}

	// ---------------------------------------------------------------- furniture

	@Override
	public void damage( int dmg, Object src ){
	}

	@Override
	public boolean add( Buff buff ){
		return false;
	}

	@Override
	public boolean reset(){
		return true;
	}

	@Override
	public String description(){
		String desc = Messages.get( this, "desc" );
		if (held != null){
			desc += "\n\n" + Messages.get( this, state == HOT ? "desc_hot" : "desc_cold", held.name() );
		} else if (spent){
			desc += "\n\n" + Messages.get( this, "desc_spent" );
		}
		return desc;
	}

	private static final String HELD      = "held";
	private static final String STATE     = "state";
	private static final String SOAK      = "soak";
	private static final String OVERHEAT  = "overheat";
	private static final String COOLING   = "cooling";
	private static final String LAST_TEMP = "last_temp";
	private static final String SPENT     = "spent";
	private static final String TROUGH    = "trough";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		if (held != null) bundle.put( HELD, held );
		bundle.put( STATE, state );
		bundle.put( SOAK, soak );
		bundle.put( OVERHEAT, overheat );
		bundle.put( COOLING, cooling );
		if (!Float.isNaN( lastTemp )) bundle.put( LAST_TEMP, lastTemp );
		bundle.put( SPENT, spent );
		bundle.put( TROUGH, trough );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		held = bundle.contains( HELD ) ? (Item) bundle.get( HELD ) : null;
		state = bundle.getInt( STATE );
		soak = bundle.getInt( SOAK );
		overheat = bundle.getInt( OVERHEAT );
		cooling = bundle.getInt( COOLING );
		lastTemp = bundle.contains( LAST_TEMP ) ? bundle.getFloat( LAST_TEMP ) : Float.NaN;
		spent = bundle.getBoolean( SPENT );
		trough = bundle.getIntArray( TROUGH );
	}
}
