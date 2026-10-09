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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicImmune;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Regeneration;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.ClimateCrystalWard;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfEnergy;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * The demon lord's drop. Worn, it fills to 100% over two in-game days, twice as fast
 * while you stand in bitter cold or fierce heat. At full charge the crystal itself is
 * set down beside you and holds every tile within two of it at a mild temperature for
 * one in-game day, then goes dim. Picking it back up empties it to 0%.
 */
public class ClimateCrystal extends Artifact {

	public static final String AC_PLACE = "PLACE";

	//turns to charge from empty to full: two in-game days
	private static final float TURNS_TO_FULL = 2f * DayNightCycle.FULL_CYCLE;

	{
		image = ItemSpriteSheet.ARTIFACT_CLIMATE_CRYSTAL;

		levelCap = 0;
		charge = 0;
		partialCharge = 0;
		chargeCap = 100;

		unique = true;
		defaultAction = AC_PLACE;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (isEquipped( hero ) && !cursed && hero.buff( MagicImmune.class ) == null && charge >= chargeCap) {
			actions.add( AC_PLACE );
		}
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );

		if (!action.equals( AC_PLACE )) return;

		if (!isEquipped( hero )) {
			GLog.i( Messages.get( Artifact.class, "need_to_equip" ) );
		} else if (cursed || hero.buff( MagicImmune.class ) != null) {
			GLog.w( Messages.get( this, "dormant" ) );
		} else if (charge < chargeCap) {
			GLog.i( Messages.get( this, "no_charge" ) );
		} else {
			GameScene.selectCell( placer );
		}
	}

	private final CellSelector.Listener placer = new CellSelector.Listener() {

		@Override
		public void onSelect( Integer cell ) {
			if (cell == null) return;
			Hero hero = Dungeon.hero;
			if (charge < chargeCap) return;
			if (!Dungeon.level.adjacent( hero.pos, cell ) || !Dungeon.level.passable[cell]
					|| Dungeon.level.pit[cell] || Actor.findChar( cell ) != null) {
				GLog.w( Messages.get( ClimateCrystal.class, "bad_cell" ) );
				return;
			}

			//the crystal itself leaves your hands and goes into the ground
			if (!doUnequip( hero, false )) return;

			ClimateCrystalWard ward = new ClimateCrystalWard();
			ward.pos = cell;
			ward.hold( ClimateCrystal.this, ClimateCrystalWard.DURATION );
			GameScene.add( ward );
			Dungeon.level.occupyCell( ward );

			CellEmitter.get( cell ).burst( Speck.factory( Speck.LIGHT ), 10 );
			SpatialSound.play( Assets.Sounds.CHARGEUP, cell, 1f, 1.2f );
			GLog.i( Messages.get( ClimateCrystal.class, "placed" ) );

			hero.sprite.operate( cell );
			hero.spendAndNext( 1f );
		}

		@Override
		public String prompt() {
			return Messages.get( ClimateCrystal.class, "prompt" );
		}
	};

	@Override
	protected ArtifactBuff passiveBuff() {
		return new crystalRecharge();
	}

	@Override
	public void charge( Hero target, float amount ) {
		if (cursed || target.buff( MagicImmune.class ) != null || charge >= chargeCap) return;
		partialCharge += 5f * amount;
		gain();
	}

	//picking the crystal back up drains everything it had stored
	public void empty() {
		charge = 0;
		partialCharge = 0;
		updateQuickslot();
	}

	private void gain() {
		while (partialCharge >= 1) {
			partialCharge--;
			charge++;
			if (charge >= chargeCap) {
				charge = chargeCap;
				partialCharge = 0;
				GLog.p( Messages.get( ClimateCrystal.class, "full" ) );
			}
		}
		updateQuickslot();
	}

	@Override
	public String desc() {
		String desc = super.desc();
		if (isEquipped( Dungeon.hero )) {
			desc += "\n\n" + Messages.get( this, charge >= chargeCap ? "desc_ready" : "desc_charging" );
		}
		return desc;
	}

	public class crystalRecharge extends ArtifactBuff {

		@Override
		public boolean act() {
			if (charge < chargeCap && !cursed && target.buff( MagicImmune.class ) == null && Regeneration.regenOn()) {
				//full in two in-game days; it drinks in extremes, so bitter cold or fierce heat doubles that
				float gain = chargeCap / TURNS_TO_FULL;
				float feels = TileTemperature.feelsLikeAt( target.pos, target );
				if (feels < 0f || feels > 30f) gain *= 2f;
				partialCharge += gain * (RingOfEnergy.artifactChargeMultiplier( target ) * xyz.gabriwar.warpedpixeldungeon.items.rarity.GearPerk.artifactCharge(ClimateCrystal.this));
				gain();
			}
			spend( TICK );
			return true;
		}
	}
}
