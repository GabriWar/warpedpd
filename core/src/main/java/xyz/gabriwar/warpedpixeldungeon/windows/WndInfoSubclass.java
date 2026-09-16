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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkillSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.Game;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.HeroIcon;
import xyz.gabriwar.warpedpixeldungeon.ui.TalentButton;
import xyz.gabriwar.warpedpixeldungeon.ui.TalentsPane;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class WndInfoSubclass extends WndTitledMessage {

	public WndInfoSubclass(HeroClass cls, HeroSubClass subCls){
		super( new HeroIcon(subCls), Messages.titleCase(subCls.title()), subCls.desc());

		ArrayList<LinkedHashMap<Talent, Integer>> talentList = new ArrayList<>();
		Talent.initClassTalents(cls, talentList);
		Talent.initSubclassTalents(subCls, talentList);

		TalentsPane.TalentTierPane talentPane = new TalentsPane.TalentTierPane(talentList.get(2), 3, TalentButton.Mode.INFO);
		talentPane.title.text( Messages.titleCase(Messages.get(WndHeroInfo.class, "talents")));
		talentPane.setRect(0, height + 5, width, talentPane.height());
		add(talentPane);
		resize(width, (int) talentPane.bottom());

		RenderedTextBlock heading = PixelScene.renderTextBlock(Messages.get(this, "skills"), 8);
		heading.hardlight(TITLE_COLOR);
		heading.setPos((width - heading.width()) / 2, height + 5);
		add(heading);
        float bottom = heading.bottom() + 4;
        java.util.List<Skill> skills = CurrentSkills.subclassSkills(subCls);
        int columns = Math.max(1, width / (TalentButton.WIDTH + 4));
        for (int row = 0; row * columns < skills.size(); row++) {
            int count = Math.min(columns, skills.size() - row * columns);
            float gap = (width - count * TalentButton.WIDTH) / (float)(count + 1);
            for (int col = 0; col < count; col++) {
                Skill skill = skills.get(row * columns + col);
                SkillPreviewButton button = new SkillPreviewButton(skill);
                button.setRect(gap + col * (TalentButton.WIDTH + gap), bottom,
                        TalentButton.WIDTH, TalentButton.HEIGHT);
                add(button);
                PixelScene.align(button);
            }
            bottom += TalentButton.HEIGHT + 4;
        }

		resize(width, (int)bottom);

	}

    private static class SkillPreviewButton extends xyz.gabriwar.warpedpixeldungeon.ui.Button {
        final Skill skill;
        final com.watabou.noosa.Image background;
        final SkillSprite icon;

        SkillPreviewButton(Skill skill) {
            this.skill = skill;
            background = new com.watabou.noosa.Image(xyz.gabriwar.warpedpixeldungeon.Assets.Interfaces.TALENT_BUTTON);
            background.frame(20 * (Skill.MAX_LEVEL - 1), 0, TalentButton.WIDTH, TalentButton.HEIGHT);
            add(background);
            icon = new SkillSprite(skill.image());
            add(icon);
        }

        @Override protected void layout() {
            super.layout();
            background.x = x; background.y = y;
            icon.x = x + 2; icon.y = y + 2;
            PixelScene.align(icon);
        }

        @Override protected void onClick() {
            Game.scene().addToFront(new WndTitledMessage(new SkillSprite(skill.image()),
                    Messages.titleCase(skill.name()), skill.info()));
        }

        @Override protected String hoverText() { return Messages.titleCase(skill.name()); }
    }

	@Override
	protected float targetHeight() {
		return super.targetHeight()-90;
	}

}
