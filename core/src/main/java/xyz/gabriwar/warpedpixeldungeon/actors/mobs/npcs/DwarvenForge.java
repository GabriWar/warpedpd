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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Ore;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.Ores;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.DwarvenForgeSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;

/**
 * The ancient forge by a burning rift (levels/overworld/CaveSites): an anvil of dark iron beside a
 * furnace the rift's heat keeps glowing. Ore smelts here as at the troll smith's (Blacksmith2
 * .smeltInto), the deep and the sky metals in smaller crucibles (Ores.Kind.forgeBatch) - one
 * economy, two forges, the rift's the better for the rarest ore and paying no gold for filings.
 * Like the troll's it fires one crucible a day. Furniture: it never moves, burns or takes harm.
 */
public class DwarvenForge extends NPC {

	{
		spriteClass = DwarvenForgeSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	/** Is there a stack in the pack the forge can pour at least one core from? */
	public static boolean canSmelt( Hero hero ){
		for (Item it : hero.belongings.backpack){
			if (it instanceof Ore && it.quantity() >= ((Ore) it).kind().forgeBatch()) return true;
		}
		return false;
	}

	@Override
	public boolean interact( Char c ){
		if (NetDialogs.handleNetHero( c, Messages.get( this, "net_host_only" ) )) return true;
		if (c != Dungeon.hero) return true;
		final Hero hero = (Hero) c;
		if (Blacksmith2.firedToday( hero )){
			GLog.w( Messages.get( this, "cold" ) );
			return true;
		}
		//what the pack holds, read here on the actor thread
		if (!canSmelt( hero )){
			GLog.w( Messages.get( this, "nothing" ) );
			return true;
		}
		Game.runOnRenderThread( () -> GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ),
				description() + "\n\n" + Messages.get( DwarvenForge.class, "prompt" ),
				Messages.get( DwarvenForge.class, "smelt" ), Messages.get( DwarvenForge.class, "leave" ) ){
			@Override
			protected void onSelect( int index ){
				if (index == 0) choose( hero );
			}
		} ) );
		return true;
	}

	private void choose( final Hero hero ){
		GameScene.selectItem( new WndBag.ItemSelector(){
			@Override
			public String textPrompt(){
				return Messages.get( DwarvenForge.class, "smelt_prompt" );
			}
			@Override
			public Class<? extends Bag> preferredBag(){
				return Belongings.Backpack.class;
			}
			@Override
			public boolean itemSelectable( Item item ){
				return item instanceof Ore;
			}
			@Override
			public void onSelect( Item item ){
				if (item instanceof Ore && hero.isAlive()) smelt( hero, (Ore) item );
			}
		} );
	}

	private void smelt( Hero hero, Ore ore ){
		int have = ore.quantity();
		String name = ore.name();
		Ores.Kind k = ore.kind();
		if (!Blacksmith2.pourCrucible( hero, ore, true )){
			GLog.w( Messages.get( this, Blacksmith2.firedToday( hero ) ? "cold" : "short", k.forgeBatch(), name, have ) );
			return;
		}
		Sample.INSTANCE.play( Assets.Sounds.EVOKE );
		if (sprite != null) sprite.emitter().burst( Speck.factory( Speck.FORGE ), 8 );
		GLog.p( Messages.get( this, "smelted", k.forgeBatch(), name ) );
		hero.spendAndNext( 2f );
	}

	@Override
	public boolean isImmune( Class effect ){
		return Blob.class.isAssignableFrom( effect ) || Burning.class.isAssignableFrom( effect ) || super.isImmune( effect );
	}

	@Override
	public void damage( int dmg, Object src ){
	}

	@Override
	public boolean add( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff buff ){
		return false;
	}

	@Override
	public boolean reset(){
		return true;
	}
}
