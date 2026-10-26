package meteordevelopment.meteorclient.zenith;

import me.pindour.catppuccin.gui.screens.CatppuccinModulesScreen;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.utils.Utils;

/** Native category windows, with pages so large addon collections stay readable. */
public final class ZenithModulesScreen extends CatppuccinModulesScreen {
    private final GuiTheme zenithTheme;
    private int page,pages=1;
    private WLabel pageLabel;
    public ZenithModulesScreen(GuiTheme theme) { super(theme);this.zenithTheme=theme; }
    @Override public void initWidgets() {
        super.initWidgets();
        var nav=add(zenithTheme.horizontalList()).bottom().centerX().padBottom(18).widget();
        nav.add(zenithTheme.button("Previous")).widget().action=()->{page=Math.max(0,page-1);invalidate();};
        pageLabel=nav.add(zenithTheme.label("Categories 1 / 1")).widget();
        nav.add(zenithTheme.button("Next")).widget().action=()->{page=Math.min(pages-1,page+1);invalidate();};
    }
    @Override protected WCategoryController createController(){return new ZenithCategories();}
    private final class ZenithCategories extends WCategoryController {
        @Override protected void onCalculateSize(){width=Utils.getWindowWidth()-32;height=Utils.getWindowHeight()-180;}
        @Override protected boolean propagateEvents(WWidget widget){return widget.visible&&super.propagateEvents(widget);}
        @Override protected void onCalculateWidgetPositions() {
            double start=16,x=start,y=100,pad=theme.scale(10),available=Utils.getWindowWidth()-16;
            int current=0;
            for(var cell:cells) {
                double w=cell.widget().width;
                if(x>start&&x+w>available){current++;x=start;}
                cell.widget().visible=current==page;
                cell.x=x;cell.y=y;cell.width=w;cell.height=cell.widget().height;
                cell.alignWidget();x+=w+pad;
            }
            pages=current+1;
            if(page>=pages){page=pages-1;invalidate();}
            if(pageLabel!=null)pageLabel.set("Categories "+(page+1)+" / "+pages);
        }
    }
    @Override public void renderBackground(net.minecraft.client.gui.GuiGraphics context,int mouseX,int mouseY,float delta) {
        boolean menu=meteordevelopment.meteorclient.MeteorClient.mc.level==null;
        context.fillGradient(0,0,width,height,menu?0xff130a21:0x38130a28,menu?0xff080610:0x8010081d);
        double time=System.nanoTime()/1e9;
        for(int i=0;i<18;i++) {
            int x=(int)(width*((i*.61803398875)%1)),y=(int)(height*((i*.41421356+time*.004)%1));
            int a=(int)(25+16*Math.sin(time*.7+i));
            context.fill(x,y,x+1,y+2,(a<<24)|0xc79aff);
        }
    }
}
