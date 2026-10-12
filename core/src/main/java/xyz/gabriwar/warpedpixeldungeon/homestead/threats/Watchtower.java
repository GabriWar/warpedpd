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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadActor;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.WatchtowerSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;

/**
 * A homestead's watchtower (DefencePieces): a plank tower the hero climbs from the cell beside it
 * (tap it). Up there (Lookout) he sees VIEW_BONUS cells further, and the wolves and bandits coming
 * for a home within SPOT_RANGE cells of the tower are shown to him, with a warning of where they
 * come from (ThreatSlice). Standing on a home's ground it takes Notice.WATCHTOWER_OFF from the
 * home's notice. Its health is the homestead's (Homestead.damage): nothing hurts the actor itself.
 */
public class Watchtower extends NPC implements HomesteadActor {

	/** How much further the hero sees from its top. */
	public static final int VIEW_BONUS = 4;
	/** How far round it a pack or a band coming for a home is spotted from its top. */
	public static final int SPOT_RANGE = 30;

	{
		spriteClass = WatchtowerSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;
	}

	@Override
	public void damage( int dmg, Object src ){
		//the tower's health is the homestead's (Homestead.damage), not the actor's
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
	public boolean interact( Char c ){
		if (NetDialogs.handleNetHero( c, Messages.get( this, "net_host_only" ) )) return true;
		if (c != Dungeon.hero || !(Dungeon.level instanceof OverworldLevel)) return true;
		final Hero hero = (Hero) c;
		Lookout up = hero.buff( Lookout.class );
		if (up != null && up.tower() == this){
			GLog.i( Messages.get( this, "already_up" ) );
			return true;
		}
		Game.runOnRenderThread( () -> GameScene.show( new WndOptions( sprite(),
				Messages.titleCase( name() ),
				Messages.get( Watchtower.class, "ask", VIEW_BONUS, SPOT_RANGE ),
				Messages.get( Watchtower.class, "climb" ),
				Messages.get( Watchtower.class, "stay" ) ){
			@Override
			protected void onSelect( int index ){
				if (index == 0) climb( hero );
			}
		} ) );
		return true;
	}

	//up the ladder: a turn's climb
	private void climb( Hero hero ){
		if (!(Dungeon.level instanceof OverworldLevel) || !Dungeon.level.adjacent( hero.pos, pos ) || !isAlive()) return;
		Lookout.climb( hero, (OverworldLevel) Dungeon.level );
		hero.sprite.operate( pos );
		SpatialSound.play( Assets.Sounds.ROPE, pos, 0.6f, 1.2f );
		GLog.i( Messages.get( this, "climbed" ) );
		hero.spendAndNext( 1f );
	}
}
