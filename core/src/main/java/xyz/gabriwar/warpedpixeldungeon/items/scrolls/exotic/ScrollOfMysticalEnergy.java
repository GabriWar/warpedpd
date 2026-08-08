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

package xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ArtifactRecharge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRecharging;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

public class ScrollOfMysticalEnergy extends ExoticScroll {
	
	{
		icon = ItemSpriteSheet.Icons.SCROLL_MYSTENRG;
	}
	
	@Override
	public void doRead() {

		detach(curUser.belongings.backpack);
		//append buff
		Buff.affect(curUser, ArtifactRecharge.class).set( 30 ).ignoreHornOfPlenty = false;

		Sample.INSTANCE.play( Assets.Sounds.READ );
		Sample.INSTANCE.play( Assets.Sounds.CHARGEUP );
		
		SpellSprite.show( curUser, SpellSprite.CHARGE, 0, 1, 1 );
		identify();
		ScrollOfRecharging.charge(curUser);
		
		readAnimation();
	}
	
}
