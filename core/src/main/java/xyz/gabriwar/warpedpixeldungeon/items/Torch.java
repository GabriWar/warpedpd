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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;

public class Torch extends Item {

	public static final String AC_LIGHT	= "LIGHT";
	//out in the world: set it in the ground beside the hero, where it burns for good
	public static final String AC_PLACE	= "PLACE";
	
	public static final float TIME_TO_LIGHT = 1;
	
	{
		image = ItemSpriteSheet.TORCH;
		
		stackable = true;
		
		defaultAction = AC_LIGHT;
	}
	
	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_LIGHT );
		if (xyz.gabriwar.warpedpixeldungeon.homestead.Homestead.allowedHere( Dungeon.level )) actions.add( AC_PLACE );
		return actions;
	}
	
	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_PLACE )) {
			place( hero );
			return;
		}
		
		if (action.equals( AC_LIGHT )) {
			
			hero.spend( TIME_TO_LIGHT );
			hero.busy();
			
			hero.sprite.operate( hero.pos );
			
			detach( hero.belongings.backpack );
			Catalog.countUse(getClass());
			
			Light.burn(hero, Light.DURATION);
			SpatialSound.play(Assets.Sounds.BURNING, hero);
			
			Emitter emitter = hero.sprite.centerEmitter();
			emitter.start( FlameParticle.FACTORY, 0.2f, 3 );
			
		}
	}
	
	//the cell beside the hero to set the torch in: open ground nobody stands on, nothing lying there
	private void place( final Hero hero ){
		if (!xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Gathering.mayWork( hero )) return;
		GameScene.selectCell( new CellSelector.Listener(){
			@Override
			public void onSelect( Integer cell ){
				if (cell == null) return;
				String no = placeRefusal( hero, cell );
				if (no != null){
					GLog.w( Messages.get( Torch.class, no ) );
					return;
				}
				xyz.gabriwar.warpedpixeldungeon.homestead.torches.StandingTorch torch = new xyz.gabriwar.warpedpixeldungeon.homestead.torches.StandingTorch();
				torch.pos = cell;
				GameScene.add( torch );
				detach( hero.belongings.backpack );
				SpatialSound.play( Assets.Sounds.BURNING, cell );
				hero.spend( TIME_TO_LIGHT );
				hero.busy();
				if (hero.sprite != null) hero.sprite.operate( cell );
			}

			@Override
			public String prompt(){
				return Messages.get( Torch.class, "place_prompt" );
			}
		} );
	}

	/** Why a torch can't be set in a cell (a message key of Torch), null when it can: out in the
	 *  world, beside the hero, on open ground the world's places and villages don't hold, with
	 *  nobody on it and nothing lying there. */
	public static String placeRefusal( Hero hero, int cell ){
		if (!xyz.gabriwar.warpedpixeldungeon.homestead.Homestead.allowedHere( Dungeon.level )) return "place_not_here";
		xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel ow = (xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level;
		if (cell < 0 || cell >= ow.length() || !ow.adjacent( hero.pos, cell )) return "place_too_far";
		if (ow.protectedForBuilding( cell )) return "place_protected";
		if (!ow.passable[cell] || ow.pit[cell] || ow.water[cell]) return "place_ground";
		if (xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( cell ) != null || ow.heaps.get( cell ) != null) return "place_occupied";
		return null;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	@Override
	public boolean isIdentified() {
		return true;
	}
	
	@Override
	public int value() {
		return 8 * quantity;
	}

}
