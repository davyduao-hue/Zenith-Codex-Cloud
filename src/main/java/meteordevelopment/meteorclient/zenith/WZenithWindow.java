package meteordevelopment.meteorclient.zenith;

import me.pindour.catppuccin.gui.themes.catppuccin.widgets.container.WCatppuccinWindow;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.render.color.Color;

/** Dark plum headers with a glowing violet rim; native drag, search and collapse remain intact. */
public final class WZenithWindow extends WCatppuccinWindow {
    public WZenithWindow(WWidget icon,String title){super(icon,title);}
    @Override protected void renderHeaderBackground(WWidget header,double corners) {
        Color accent=theme().accentColor();
        float bottom=(float)(radius()*(1-corners));
        roundedRect().bounds(header).radii(radius(),radius(),bottom,bottom)
            .color(new Color(29,18,45,245)).outline(new Color(accent.r,accent.g,accent.b,80),1f)
            .shadow(12,0,new Color(accent.r,accent.g,accent.b,28)).render();
        ZenithGuiTheme zenith=(ZenithGuiTheme)theme;
        double time=zenith.reducedMotion.get()||!zenith.headerMotion.get()?0:System.nanoTime()/1e9*zenith.motionSpeed.get();
        double inset=theme.scale(14),track=header.width-2*inset;
        roundedRect().pos(header.x+inset,header.y+header.height-theme.scale(2)).size(track,theme.scale(1))
            .radius(1).color(new Color(accent.r,accent.g,accent.b,65)).render();
        double glow=track*.24;
        roundedRect().pos(header.x+inset+(track-glow)*(.5+.5*Math.sin(time*.65)),header.y+header.height-theme.scale(2))
            .size(glow,theme.scale(1.5)).radius(1)
            .color(new Color(218,178,255,145)).shadow(6,0,new Color(accent.r,accent.g,accent.b,80)).render();
    }
}
