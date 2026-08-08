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

package xyz.gabriwar.warpedpixeldungeon.items.artifacts;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

//ported from Remixed PD: lighting it grants mind vision while it burns down,
//and it slowly recovers when carried unlit.
public class CandleOfMindVision extends Artifact {

	public static final String AC_LIGHT = "LIGHT";
	public static final String AC_SNUFF = "SNUFF";

	{
		image = ItemSpriteSheet.ARTIFACT_FROST;

		levelCap = 0;
		charge = 50;
		chargeCap = 50;

		defaultAction = AC_LIGHT;
		unique = true;
		bones = false;
	}

	private boolean lit = false;

	private static final String LIT = "lit";

	@Override
	public void storeInBundle( com.watabou.utils.Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LIT, lit );
	}

	@Override
	public void restoreFromBundle( com.watabou.utils.Bundle bundle ) {
		super.restoreFromBundle( bundle );
		lit = bundle.getBoolean( LIT );
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (isEquipped( hero ) && charge > 0 && !cursed) {
			actions.add( lit ? AC_SNUFF : AC_LIGHT );
		}
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );

		if (action.equals( AC_LIGHT )) {
			if (!isEquipped( hero )) {
				GLog.i( Messages.get( Artifact.class, "need_to_equip" ) );
			} else if (charge <= 0) {
				GLog.i( Messages.get( this, "spent" ) );
			} else {
				lit = true;
				Buff.affect( hero, MindVision.class, charge );
				GLog.i( Messages.get( this, "lit" ) );
				updateQuickslot();
			}

		} else if (action.equals( AC_SNUFF )) {
			lit = false;
			Buff.detach( hero, MindVision.class );
			GLog.i( Messages.get( this, "snuffed" ) );
			updateQuickslot();
		}
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new CandleBuff();
	}

	@Override
	public String desc() {
		String desc = Messages.get( this, "desc" );
		if (isEquipped( Dungeon.hero )) {
			desc += "\n\n" + Messages.get( this, lit ? "desc_lit" : "desc_unlit" );
		}
		return desc;
	}

	public class CandleBuff extends ArtifactBuff {

		@Override
		public boolean act() {
			if (lit) {
				//burning down
				charge--;
				if (charge <= 0) {
					charge = 0;
					lit = false;
					Buff.detach( target, MindVision.class );
					GLog.w( Messages.get( CandleOfMindVision.class, "burnt_out" ) );
				}
			} else if (charge < chargeCap) {
				//recovers slowly while carried unlit
				partialCharge += 0.1f;
				if (partialCharge >= 1f) {
					partialCharge--;
					charge++;
				}
			}
			updateQuickslot();
			spend( TICK );
			return true;
		}
	}
}
