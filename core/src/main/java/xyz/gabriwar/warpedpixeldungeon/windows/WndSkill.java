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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.windows;

import com.watabou.noosa.Gizmo;
import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BranchSkill;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkillSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Toolbar;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

public class WndSkill extends Window {

	private static final float BUTTON_HEIGHT = 16;
	private static final float GAP = 2;

	private static final int WIDTH_MIN = 120;
	private static final int WIDTH_MAX = 220;

	private final Window host;
	private final Skill skill;
	//everything but the chrome, so the window can redraw itself in place
	private final ArrayList<Gizmo> content = new ArrayList<>();

	public WndSkill( final Window host, final Skill skill ){
		this( host, skill, false );
	}

	public WndSkill( final Window host, final Skill skill, final boolean upgradable ){
		super();
		this.host = host;
		this.skill = skill;
		build( upgradable );
	}

	private boolean canUpgradeNow(){
		return skill.level < Skill.MAX_LEVEL && Dungeon.hero.heroSkills.availableSkill >= skill.upgradeCost()
				&& !skill.pathLocked();
	}

	private <T extends Gizmo> T show( T g ){
		add( g );
		content.add( g );
		return g;
	}

	/** lays the window out for the skill as it is now; called again after an
	 *  upgrade or a toggle, so the window stays where it is with fresh numbers */
	private void build( boolean upgradable ){
		for (Gizmo g : content){
			erase( g );
			g.destroy();
		}
		content.clear();

		int width = WIDTH_MIN;

		//title: icon + name + level
		IconTitle titlebar = new IconTitle();
		titlebar.icon( skill.quickslotIcon() );
		titlebar.label( skill instanceof BranchSkill
				? skill.name() + " (" + ((BranchSkill) skill).spent() + " pts)"
				: skill instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CrownSkill ? skill.name()
				: skill.name() + " (lvl " + skill.level + ")" );
		titlebar.setRect( 0, 0, width, 0 );
		show( titlebar );

		RenderedTextBlock info = PixelScene.renderTextBlock( skill.info(), 6 );
		info.maxWidth( width );
		info.setPos( titlebar.left(), titlebar.bottom() + GAP );
		show( info );

		float y = info.bottom() + GAP;

		if (upgradable){
			RedButton btnUp = new RedButton( "Upgrade (" + skill.upgradeCost() + (skill.upgradeCost() == 1 ? " point)" : " points)") ) {
				@Override
				protected void onClick() {
					skill.requestUpgrade();
					//keep the flow going: the same window, with fresh numbers
					build( canUpgradeNow() );
				}
			};
			btnUp.icon( Icons.get(Icons.TALENT) );
			btnUp.setRect( 0, y + GAP, width, BUTTON_HEIGHT );
			show( btnUp );
			y = btnUp.bottom();
		}

		for (final String action : skill.actions( Dungeon.hero )){
			RedButton btn = new RedButton( action ) {
				@Override
				protected void onClick() {
					if (action.equals(Skill.AC_CAST) || action.equals(Skill.AC_SUMMON)){
						//game actions need the board - close this and the hosting window too
						hide();
						if (host != null) host.hide();
						skill.execute( Dungeon.hero, action );
					} else {
						//a toggle: flip it and stay put
						skill.execute( Dungeon.hero, action );
						build( canUpgradeNow() );
					}
				}
			};
			btn.setRect( 0, y + GAP, width, BUTTON_HEIGHT );
			show( btn );
			y = btn.bottom();
		}

		//a learned skill that can be used goes on the hotbar from here too, not only from
		//the slot itself
		if (Dungeon.hero.heroSkills.usableNow( Dungeon.hero ).contains( skill )){
			RedButton btnSlot = new RedButton( Messages.get( this, "quickslot" ) ) {
				@Override
				protected void onClick() {
					chooseSlot();
				}
			};
			btnSlot.setRect( 0, y + GAP, width, BUTTON_HEIGHT );
			show( btnSlot );
			y = btnSlot.bottom();
		}

		resize( width, (int)(y + GAP) );
	}

	/** offers the skill slots of the hotbar page that is showing */
	private void chooseSlot(){
		final int first = Toolbar.visibleStart;
		String[] slots = new String[Toolbar.pageSize];
		for (int i = 0; i < slots.length; i++){
			Skill held = Dungeon.hero.heroSkills.quickslot( first + i );
			slots[i] = Messages.get( this, "slot", i + 1,
					held == null ? Messages.get( this, "slot_empty" ) : Messages.titleCase( held.name() ) );
		}
		GameScene.show( new WndOptions( skill.quickslotIcon(), Messages.titleCase( skill.name() ),
				Messages.get( this, "slot_prompt" ), slots ){
			@Override
			protected void onSelect( int index ) {
				//one slot per skill, as with items
				for (int i = 0; i < xyz.gabriwar.warpedpixeldungeon.QuickSlot.SIZE; i++){
					if (Dungeon.hero.heroSkills.quickslot( i ) == skill) Dungeon.hero.heroSkills.quickslot( i, null );
				}
				Dungeon.hero.heroSkills.quickslot( first + index, skill );
			}
		} );
	}
}
