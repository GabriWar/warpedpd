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

package xyz.gabriwar.warpedpixeldungeon.homestead.farming;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadActor;
import xyz.gabriwar.warpedpixeldungeon.items.Fertilizer;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Aprico;
import xyz.gabriwar.warpedpixeldungeon.items.food.Berry;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blackberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blueberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cloudberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cornwheatshaft;
import xyz.gabriwar.warpedpixeldungeon.items.food.FullMoonberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.Grape;
import xyz.gabriwar.warpedpixeldungeon.items.food.MonsterMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.Moonberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.Nut;
import xyz.gabriwar.warpedpixeldungeon.items.food.Peanut;
import xyz.gabriwar.warpedpixeldungeon.items.food.PlantEgg;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CompostBinSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * A compost bin (FarmPieces' "compost_bin"): seeds, berries, the fields' produce and raw meat -
 * monster meat too - go in (compostable: never a cooked meal, a ration or anything with a gift in
 * it), as many of a stack as the hero says, and every BATCH things become a dose of plant food
 * (Fertilizer) ROT_TURNS turns of the global clock (FarmCrop.clock) after the batch filled. The clock runs on while the hero is away,
 * so a bin left full is ready when he is back. Taking it down (or wrecking it) leaves the plant
 * food it had ready on its cell; what was still rotting is lost.
 */
public class CompostBin extends Mob implements HomesteadActor {

	{
		spriteClass = CompostBinSprite.class;

		HP = HT = 1;
		defenseSkill = 0;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	/** Things that make a dose, and how long a dose takes to rot, in turns. */
	public static final int BATCH = 5;
	public static final float ROT_TURNS = 500f;

	/** Things in it not yet a whole batch. */
	public int loose;
	/** When each full batch began to rot (FarmCrop.clock), oldest first. */
	public final ArrayList<Float> rotting = new ArrayList<>();
	/** Doses of plant food ready to take. */
	public int ready;

	/** Does it go in a compost bin: a seed, a berry of any bush, a crop's plain produce (blandfruit
	 *  not yet brewed with anything, cornwheat, apricots, grapes, plant eggs, plain nuts and
	 *  peanuts) or raw meat (mystery meat, monster meat). Never a meal, a ration, a magic nut or a
	 *  potion. */
	public static boolean compostable( Item item ){
		if (item instanceof Plant.Seed || item instanceof Berry || item instanceof Blackberry || item instanceof Blueberry
				|| item instanceof Cloudberry || item instanceof Moonberry || item instanceof FullMoonberry) return true;
		if (item instanceof Blandfruit) return ((Blandfruit) item).potionAttrib == null;
		return item instanceof Cornwheatshaft || item instanceof Aprico || item instanceof Grape
				|| item instanceof PlantEgg || item instanceof Peanut || item.getClass() == Nut.class
				|| item instanceof MysteryMeat || item instanceof MonsterMeat;
	}

	/** Puts `n` things in: every BATCH of them starts rotting now. */
	public void add( int n ){
		settle();
		loose += Math.max( 0, n );
		while (loose >= BATCH){
			loose -= BATCH;
			rotting.add( FarmCrop.clock() );
		}
	}

	/** Every batch that has rotted its ROT_TURNS turns is a dose ready. */
	public void settle(){
		float now = FarmCrop.clock();
		while (!rotting.isEmpty() && now - rotting.get( 0 ) >= ROT_TURNS){
			rotting.remove( 0 );
			ready++;
		}
	}

	/** Turns until the next batch is ready, -1 with none rotting. */
	public int nextReadyIn(){
		if (rotting.isEmpty()) return -1;
		return Math.max( 0, (int) Math.ceil( ROT_TURNS - (FarmCrop.clock() - rotting.get( 0 )) ) );
	}

	@Override
	protected boolean act(){
		settle();
		spend( TICK );
		return true;
	}

	//a blow at it is a blow at the built piece (Homestead.damage): wrecked, it is taken down
	@Override
	public void damage( int dmg, Object src ){
		Homestead.damage( Dungeon.level, pos, dmg, src );
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
	public void destroy(){
		settle();
		if (ready > 0 && Dungeon.level != null){
			Heap h = Dungeon.level.drop( new Fertilizer().quantity( ready ), pos );
			if (h.sprite != null) h.sprite.drop();
			ready = 0;
		}
		super.destroy();
	}

	@Override
	public String description(){
		return Messages.get( this, "desc", BATCH, (int) ROT_TURNS ) + "\n\n" + status();
	}

	//what is in it, as its window and its description tell it
	private String status(){
		settle();
		StringBuilder s = new StringBuilder( Messages.get( this, "loose", loose, BATCH ) );
		if (!rotting.isEmpty()) s.append( '\n' ).append( Messages.get( this, "rotting", rotting.size(), nextReadyIn() ) );
		if (ready > 0) s.append( '\n' ).append( Messages.get( this, "ready", ready ) );
		return s.toString();
	}

	@Override
	public boolean interact( Char c ){
		if (c != Dungeon.hero){
			if (c instanceof Hero) NetManager.heroLog( (Hero) c, Messages.get( this, "host_only" ) );
			return true;
		}
		final Hero hero = (Hero) c;
		Game.runOnRenderThread( () -> GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ), status(),
				Messages.get( CompostBin.class, "add" ),
				Messages.get( CompostBin.class, "take", ready ),
				Messages.get( CompostBin.class, "leave" ) ){
			@Override
			protected boolean enabled( int index ){
				return index != 1 || ready > 0;
			}

			@Override
			protected void onSelect( int index ){
				if (index == 0) GameScene.selectItem( filler( hero ) );
				else if (index == 1) take( hero );
			}
		} ) );
		return true;
	}

	//the plant food that is ready, into the pack (or at the hero's feet)
	private void take( Hero hero ){
		settle();
		if (ready <= 0) return;
		Fertilizer f = new Fertilizer();
		f.quantity( ready );
		ready = 0;
		if (!f.collect( hero.belongings.backpack )){
			Heap h = Dungeon.level.drop( f, hero.pos );
			if (h.sprite != null) h.sprite.drop();
		}
		GLog.p( Messages.get( this, "taken", f.quantity() ) );
		hero.spendAndNext( 1f );
	}

	private WndBag.ItemSelector filler( final Hero hero ){
		return new WndBag.ItemSelector(){
			@Override
			public String textPrompt(){
				return Messages.get( CompostBin.class, "add_prompt" );
			}

			@Override
			public boolean itemSelectable( Item item ){
				return compostable( item );
			}

			@Override
			public void onSelect( Item item ){
				if (item == null || !compostable( item )) return;
				int all = item.quantity();
				if (all <= 1){
					put( hero, item, 1 );
					return;
				}
				//a stack: how many of it, never the whole of it unasked
				final int[] counts = all > BATCH ? new int[]{ 1, BATCH, all } : new int[]{ 1, all };
				String[] options = new String[counts.length + 1];
				for (int i = 0; i < counts.length; i++){
					options[i] = i == counts.length - 1 ? Messages.get( CompostBin.class, "put_all", all )
							: Messages.get( CompostBin.class, counts[i] == 1 ? "put_one" : "put_some", counts[i] );
				}
				options[counts.length] = Messages.get( CompostBin.class, "leave" );
				GameScene.show( new WndOptions( new ItemSprite( item ), Messages.titleCase( item.name() ),
						Messages.get( CompostBin.class, "how_many", BATCH ), options ){
					@Override
					protected void onSelect( int index ){
						if (index >= 0 && index < counts.length) put( hero, item, counts[index] );
					}
				} );
			}
		};
	}

	/** `n` of a stack in the hero's pack go in (all of it at most). A turn's work. */
	public void put( Hero hero, Item item, int n ){
		if (!compostable( item ) || !hero.belongings.contains( item )) return;
		n = Math.min( n, item.quantity() );
		if (n <= 0) return;
		String name = Messages.titleCase( item.name() );
		if (n >= item.quantity()){
			item.detachAll( hero.belongings.backpack );
		} else {
			item.split( n );
			Item.updateQuickslot();
		}
		add( n );
		if (Game.scene() instanceof GameScene) SpatialSound.play( Assets.Sounds.DIG, pos );
		GLog.i( Messages.get( CompostBin.class, "added", n, name ) );
		hero.spendAndNext( 1f );
	}

	private static final String LOOSE = "loose", ROTTING = "rotting", READY = "ready";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( LOOSE, loose );
		float[] r = new float[rotting.size()];
		for (int i = 0; i < r.length; i++) r[i] = rotting.get( i );
		bundle.put( ROTTING, r );
		bundle.put( READY, ready );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		loose = bundle.getInt( LOOSE );
		rotting.clear();
		if (bundle.contains( ROTTING )) for (float f : bundle.getFloatArray( ROTTING )) rotting.add( f );
		ready = bundle.getInt( READY );
	}
}
