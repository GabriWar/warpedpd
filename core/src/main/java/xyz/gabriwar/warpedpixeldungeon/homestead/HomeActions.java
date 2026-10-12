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


package xyz.gabriwar.warpedpixeldungeon.homestead;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.ui.BuildIcons;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTextInput;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage;
import com.watabou.utils.Callback;

import java.util.ArrayList;
import java.util.List;

/**
 * What a home's menu offers (the hearthstone's: the travel stage builds the menu): each ACTION an
 * id, its label's string key with the class it is under, and the Callback that does it. The menu
 * shows them in order and calls the one chosen on the render thread; an action that changes the
 * world posts its work to the actor thread itself.
 *
 * Built in, for the host's own hero: "ledger", the home's ledger of what happened while he was
 * away (AwaySettlers), newest first, and "rename", a name for the home. The rest come from
 * PROVIDERS, looked up by class name (a missing one is skipped), each a public class with a public
 * no-argument constructor implementing Provider; one made in code (a test, a debug scene) is added
 * with register. A co-op guest's hero is offered what the providers offer him: a guest's choice
 * reaches his own screen through NetDialogs, which is the menu's to route.
 *
 * <pre>
 * public class TravelActions implements HomeActions.Provider {
 *     public void offer( HomesteadWorld.Home home, Hero hero, List<HomeActions.Action> out ){
 *         out.add( new HomeActions.Action( "travel", TravelActions.class, "travel", () -> ...open the waystones... ) );
 *     }
 * }
 * </pre>
 */
public final class HomeActions {

	private HomeActions(){}

	/** One thing a home's menu offers. */
	public static final class Action {
		public final String id;
		/** The class its label's key is under (Messages.get( owner, labelKey )). */
		public final Class<?> owner;
		public final String labelKey;
		public final Callback callback;

		public Action( String id, Class<?> owner, String labelKey, Callback callback ){
			this.id = id;
			this.owner = owner;
			this.labelKey = labelKey;
			this.callback = callback;
		}

		public String label(){
			return Messages.get( owner, labelKey );
		}
	}

	/** A part of the homesteads with something to offer at a home. */
	public interface Provider {
		void offer( HomesteadWorld.Home home, Hero hero, List<Action> out );
	}

	private static final String PKG = "xyz.gabriwar.warpedpixeldungeon.homestead.";

	/** The providers, by class name, in the order their actions are listed (after the built-in ones). */
	public static final String[] PROVIDERS = {
			PKG + "travel.TravelActions",    //travel: the waystones
			PKG + "threats.ThreatActions"    //threats: the home's notice and defences
	};

	private static final Providers<Provider> providers = new Providers<>( Provider.class, PROVIDERS );

	/** Adds a provider made in code (a test, a debug scene). */
	public static void register( Provider p ){
		providers.add( p );
	}

	public static void unregister( Provider p ){
		providers.remove( p );
	}

	/** The most ledger lines the ledger's window shows, newest first. */
	public static final int LEDGER_SHOWN = 10;
	/** The longest name a home takes. */
	public static final int NAME_MAX = 24;

	/** Everything a home's menu offers this hero, in order. */
	public static List<Action> forHome( final HomesteadWorld.Home home, Hero hero ){
		ArrayList<Action> out = new ArrayList<>();
		if (hero == Dungeon.hero){
			out.add( new Action( "ledger", HomeActions.class, "ledger", () -> GameScene.show(
					new WndTitledMessage( BuildIcons.get( BuildIcons.TRAVEL ), home.title(), ledgerText( home ) ) ) ) );
			out.add( new Action( "rename", HomeActions.class, "rename", () -> GameScene.show(
					new WndTextInput( Messages.get( HomeActions.class, "rename_title" ), null,
							home.name == null ? "" : home.name, NAME_MAX, false,
							Messages.get( HomeActions.class, "rename_ok" ), Messages.get( HomeActions.class, "rename_cancel" ) ){
						@Override
						public void onSelect( boolean positive, String text ){
							if (positive && text != null && !text.trim().isEmpty()) home.name = text.trim();
						}
					} ) ) );
		}
		for (Provider p : providers.all()) p.offer( home, hero, out );
		return out;
	}

	/** A home's ledger as its window shows it: the last LEDGER_SHOWN lines, newest first, each with
	 *  its day; a line saying so when nothing has happened yet. */
	public static String ledgerText( HomesteadWorld.Home home ){
		List<HomesteadWorld.Entry> ledger = home.ledger();
		if (ledger.isEmpty()) return Messages.get( HomeActions.class, "ledger_empty" );
		StringBuilder s = new StringBuilder();
		for (int i = ledger.size() - 1; i >= Math.max( 0, ledger.size() - LEDGER_SHOWN ); i--){
			HomesteadWorld.Entry e = ledger.get( i );
			if (s.length() > 0) s.append( '\n' );
			s.append( Messages.get( HomeActions.class, "ledger_line", e.day + 1, e.text ) );
		}
		return s.toString();
	}
}
