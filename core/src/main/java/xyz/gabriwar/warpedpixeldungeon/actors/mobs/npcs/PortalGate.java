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

/*
 * Warped Pixel Dungeon
 *
 * PortalGate: shopkeeper-guarded fast-travel node. Three states:
 *   LOCKED  – costs gold to activate (shopkeeper alive, not yet paid)
 *   ACTIVE  – usable as travel destination (paid)
 *   DEAD    – shopkeeper killed; permanently unusable for this run
 * The town's gate (Portals.TOWN, OverworldLevel.placeTownGate) has no keeper: always ACTIVE.
 */

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Portals;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.PortalGateSprite;
import xyz.gabriwar.warpedpixeldungeon.windows.WndPortal;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

import org.json.JSONArray;
import org.json.JSONObject;

public class PortalGate extends NPC {

	public enum State { LOCKED, ACTIVE, DEAD }

	private static final String STATE = "state";

	public State state = State.LOCKED;

	{
		spriteClass = PortalGateSprite.class;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.MINIBOSS); // shields from most CC

		HP = HT = 1000;
		alignment = Alignment.NEUTRAL;
	}

	@Override
	public int defenseSkill(Char enemy) {
		return 0;
	}

	@Override
	public void damage(int dmg, Object src) {
		// invulnerable — only shopkeeper death flips it to DEAD
	}

	//no spell takes hold of a gate. A lullaby put it to sleep, and the amok of a scroll of rage
	//woke it hunting (Mob.add): it walked off round the floor until another lullaby, and terror
	//sent it running the same way
	@Override
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	protected boolean act() {
		returnToShop();
		return super.act();
	}

	//a shop's gate stands on the cell its shop raised it on (Portals.Record.cell), where the
	//gate's own checks and every trip through it look for it. One a spell set walking, in a save
	//from before add() turned every spell away, blinks back there once the cell is clear. The
	//town's gate is left to the surface, which records wherever it stands (placeTownGate)
	void returnToShop() {
		int key = Portals.keyHere();
		Portals.Record home = key == Portals.TOWN ? null : Portals.get(key);
		if (home == null || home.cell < 0 || home.cell == pos || home.cell >= Dungeon.level.length()
				|| !Dungeon.level.passable[home.cell] || Actor.findChar(home.cell) != null) {
			return;
		}
		if (sprite != null) {
			ScrollOfTeleportation.appear(this, home.cell);
		} else {
			pos = home.cell;
		}
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	public String description() {
		switch (state) {
			case ACTIVE: return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(this, "desc_active");
			case DEAD:   return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(this, "desc_dead");
			case LOCKED:
			default:     return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(this, "desc_locked");
		}
	}

	@Override
	public boolean interact(Char c) {
		if (!(c instanceof Hero)) return true;
		Hero h = (Hero) c;
		Portals.discover(Portals.keyHere());

		// Remote (netHero) → route the portal UI to the owning client via NetDialogs.
		// The host can't open a GameScene window on someone else's screen, so we
		// ship the rendered state + the player picks a choice that comes back via
		// DIALOG_CHOICE and is applied in NetDialogs.resolve.
		if (h.isRemote) {
			try {
				JSONObject payload = new JSONObject();
				payload.put("state", state.name());
				payload.put("depth", Dungeon.depth);
				payload.put("cost", Portals.costForDepth(Dungeon.depth));
				payload.put("gold", Dungeon.gold);
				if (state == State.ACTIVE) {
					JSONArray dests = new JSONArray();
					for (int d : Portals.travelable(Portals.keyHere())) dests.put(d);
					payload.put("dests", dests);
				}
				NetDialogs.request(h, NetDialogs.KIND_PORTAL, payload, true);
			} catch (Exception ignored) {}
			return true;
		}

		// Host hero — open the window locally.
		if (c != Dungeon.hero) return true;
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show(new WndPortal(PortalGate.this));
			}
		});
		return true;
	}

	public void setState(State s) {
		if (state == s) return;
		// The town's gate has no keeper to lose: a surface trader who flees puts out every
		// gate on the level, and this one stays lit.
		if (Portals.keyHere() == Portals.TOWN && s != State.ACTIVE) return;
		State prev = state;
		state = s;
		Portals.setState(Portals.keyHere(), s);
		if (sprite instanceof PortalGateSprite) {
			((PortalGateSprite) sprite).onStateChange(prev, s);
		}
	}

	// Sync this NPC's state with the global registry. Called when added to level
	// (re-spawn on revisit picks up DEAD/ACTIVE flags set on prior visits).
	public void syncFromRegistry() {
		Portals.Record r = Portals.register(Portals.keyHere(), pos);
		if (state != r.state) {
			State prev = state;
			state = r.state;
			if (sprite instanceof PortalGateSprite) {
				((PortalGateSprite) sprite).onStateChange(prev, state);
			}
		}
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(STATE, state);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		state = bundle.getEnum(STATE, State.class);
	}
}
