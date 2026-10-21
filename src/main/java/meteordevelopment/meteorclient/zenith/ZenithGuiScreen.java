package meteordevelopment.meteorclient.zenith;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.DoubleConsumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** A custom animated dashboard; Meteor remains the module/settings engine. */
public final class ZenithGuiScreen extends TabScreen {
    private static final Color WHITE=new Color(241,232,255),MUTED=new Color(151,137,174),PURPLE=new Color(184,126,255);
    private static final HudRenderer DASHBOARD=HudRenderer.independent();
    private final List<Hit> hits=new ArrayList<>();
    private final Map<String,Double> states=new HashMap<>();
    private Category category;
    private Module selected;
    private String query="";
    private boolean searching,binding,closing;
    private double moduleScroll,settingScroll,open,slide=1,navY;
    private double moduleScrollTarget,settingScrollTarget,navScroll,navScrollTarget;
    private String cachedQuery;
    private Category cachedCategory;
    private List<Module> cachedModules;
    private int cachedCount=-1;
    private double ox,oy,scale=1,mouseX,mouseY;
    private long lastFrame=System.nanoTime();
    private DoubleConsumer drag;
    private record Hit(double x,double y,double w,double h,Runnable action,DoubleConsumer slider) {}

    public ZenithGuiScreen(GuiTheme theme) {
        super(theme,Tabs.get().getFirst());
        for(Category c:Modules.loopCategories())if(category==null||c.name.equals("Zenith Addons"))category=c;
        selectFirst();
    }
    @Override public void initWidgets() {}
    private void selectFirst() {
        List<Module> modules=filtered();selected=modules.isEmpty()?null:modules.getFirst();
    }
    private List<Module> filtered() {
        if(cachedModules!=null&&Objects.equals(cachedQuery,query)&&cachedCategory==category&&cachedCount==Modules.get().getCount())return cachedModules;
        Collection<Module> modules=query.isBlank()?Modules.get().getGroup(category):Modules.get().getAll();
        String match=query.toLowerCase(Locale.ROOT);
        cachedQuery=query;cachedCategory=category;cachedCount=Modules.get().getCount();
        cachedModules=modules.stream().filter(m->match.isBlank()||(m.title+" "+m.description).toLowerCase(Locale.ROOT).contains(match))
            .sorted(Comparator.comparing(m->m.title)).toList();
        return cachedModules;
    }
    private double state(String key,double target,double dt) {
        double value=UiMotion.approach(states.getOrDefault(key,0.0),target,16,dt);states.put(key,value);return value;
    }
    private void box(HudRenderer r,double x,double y,double w,double h,double radius,Color color) {
        ZenithDraw.rounded(r,ox+x*scale,oy+y*scale,w*scale,h*scale,radius*scale,new Color(color.r,color.g,color.b,(int)(color.a*open)));
    }
    private void text(HudRenderer r,String text,double x,double y,double size,Color color) {
        double fs=Hud.get().hasCustomFont()?Math.sqrt(size/13.2*scale):size/18*scale;
        r.text(text,ox+x*scale,oy+y*scale,new Color(color.r,color.g,color.b,(int)(color.a*open)),false,fs);
    }
    private String trim(String text,int max) {return text.length()>max?text.substring(0,Math.max(0,max-3))+"...":text;}
    private boolean over(double x,double y,double w,double h) {return mouseX>=x&&mouseX<x+w&&mouseY>=y&&mouseY<y+h;}
    private void hit(double x,double y,double w,double h,Runnable action) {hits.add(new Hit(x,y,w,h,action,null));}
    private void button(HudRenderer r,String label,double x,double y,double w,Runnable action,double dt) {
        double hover=state("button:"+label,over(x,y,w,29)?1:0,dt);
        box(r,x,y,w,29,7,ZenithDraw.mix(new Color(36,25,51),new Color(73,44,105),hover));
        text(r,label,x+10,y+8,11,WHITE);hit(x,y,w,29,action);
    }
    private void toggle(HudRenderer r,Module module,double x,double y,double dt) {
        double value=state("toggle:"+module.name,module.isActive()?1:0,dt);
        box(r,x,y,29,16,8,ZenithDraw.mix(new Color(59,45,75),new Color(157,91,228),value));
        box(r,x+3+value*13,y+3,10,10,5,new Color(246,236,255));
    }

    @Override public void renderCustom(GuiGraphicsExtractor graphics,int mx,int my,float delta) {
        long now=System.nanoTime();double dt=Math.min(.1,(now-lastFrame)/1e9);lastFrame=now;
        open=UiMotion.approach(open,closing?0:1,closing?22:12,dt);
        if(closing&&open<.025){mc.gui.setScreen(parent);return;}
        slide=UiMotion.approach(slide,0,14,dt);
        double sw=Utils.getWindowWidth(),sh=Utils.getWindowHeight();
        scale=Math.min(1.4,Math.min((sw-38)/900,(sh-38)/530));
        ox=(sw-900*scale)/2;oy=(sh-530*scale)/2+(1-open)*25*scale;
        mouseX=(mx*mc.getWindow().getGuiScale()-ox)/scale;mouseY=(my*mc.getWindow().getGuiScale()-oy)/scale;
        hits.clear();
        Utils.unscaledProjection();
        HudRenderer r=DASHBOARD;r.begin(graphics);
        r.quad(0,0,sw,sh,new Color(6,3,12,(int)(135*open)));
        for(int i=5;i>0;i--)box(r,-i*3,-i*3,900+i*6,530+i*6,19+i*2,new Color(0,0,0,8));
        box(r,0,0,900,530,16,new Color(19,12,29,(int)(247*open)));
        box(r,1,1,898,528,15,new Color(22,15,33,(int)(240*open)));
        box(r,0,0,166,530,16,new Color(15,9,24,245));
        box(r,154,1,12,528,0,new Color(15,9,24,245));
        ZenithHud.star(r,ox+31*scale,oy+35*scale,12*scale,now/1e9,PURPLE);
        text(r,"ZENITH",53,19,18,WHITE);text(r,"CLIENT 2.0",54,41,9,MUTED);
        text(r,"WORKSPACE",19,79,9,MUTED);
        int index=0;double targetNav=104;
        for(Category c:Modules.loopCategories()) {if(c==category)targetNav=104+index*34;index++;}
        navScrollTarget=UiMotion.clamp(navScrollTarget,0,Math.max(0,index*34-272));
        navScroll=UiMotion.approach(navScroll,navScrollTarget,18,dt);
        targetNav-=navScroll;
        navY=UiMotion.approach(navY==0?targetNav:navY,targetNav,18,dt);
        if(navY>=104&&navY+29<=390){box(r,10,navY,146,29,7,new Color(57,31,84));box(r,10,navY+7,3,15,1,PURPLE);}
        index=0;
        for(Category c:Modules.loopCategories()) {
            double y=104+index++*34-navScroll;
            if(y<103.5||y+29>390)continue;
            double hover=state("nav:"+c.name,over(10,y,146,29)?1:0,dt);
            if(c!=category)box(r,10,y,146,29,7,new Color(65,38,91,(int)(60*hover)));
            text(r,c.name,23,y+8,12,c==category?WHITE:MUTED);
            hit(10,y,146,29,()->{category=c;query="";moduleScroll=settingScroll=moduleScrollTarget=settingScrollTarget=0;slide=1;selectFirst();});
        }
        button(r,"Interface style",14,406,138,()->Tabs.get().stream().filter(tab->tab.name.equals("GUI")).findFirst().orElseThrow().openScreen(theme),dt);
        button(r,"HUD editor",14,441,138,()->mc.gui.setScreen(new HudEditorScreen(theme)),dt);
        button(r,"Reset HUD layout",14,476,138,()->Hud.get().resetToDefaultElements(),dt);
        text(r,"26.2 / FABRIC",18,514,8,MUTED);
        box(r,184,17,654,36,9,new Color(31,21,45));
        text(r,searching||!query.isEmpty()?query+(searching&&now/500_000_000L%2==0?"|":""):"Search modules and descriptions...",199,29,12,query.isEmpty()?MUTED:WHITE);
        hit(184,17,654,36,()->searching=true);
        button(r,"X",849,20,30,this::onClose,dt);
        text(r,query.isBlank()?category.name:"Search results",186,72,19,WHITE);
        List<Module> modules=filtered();
        text(r,modules.size()+" modules",186,98,10,MUTED);
        text(r,"Left click toggles  /  Right click configures",186,510,9,MUTED);
        box(r,184,121,318,373,10,new Color(16,10,26));
        moduleScrollTarget=UiMotion.clamp(moduleScrollTarget,0,Math.max(0,modules.size()*55-354));
        moduleScroll=UiMotion.approach(moduleScroll,moduleScrollTarget,18,dt);
        int visible=0;
        for(int i=0;i<modules.size();i++) {
            double y=130+i*55-moduleScroll+slide*8;
            if(y<128||y+49>485)continue;
            Module m=modules.get(i);visible++;
            double hover=state("card:"+m.name,over(192,y,302,49)?1:0,dt);
            box(r,192,y,302,49,8,ZenithDraw.mix(new Color(29,20,41),new Color(47,31,64),hover));
            if(m==selected) {box(r,192,y+8,2,33,1,PURPLE);box(r,194,y,300,49,8,new Color(118,63,175,24));}
            text(r,m.title,204,y+8,13,m.isActive()?WHITE:new Color(207,195,222));
            text(r,trim(m.description,42),204,y+29,9,MUTED);
            toggle(r,m,453,y+9,dt);
            hit(192,y,302,49,()->{selected=m;settingScroll=settingScrollTarget=0;m.toggle();});
        }
        if(visible==0)text(r,"No matching modules",204,151,12,MUTED);
        if(modules.size()>6)box(r,496,132+moduleScroll/Math.max(1,modules.size()*55-354)*300,2,40,1,new Color(119,77,157));
        if(selected!=null) settings(r,dt);
        r.end();Utils.scaledProjection();runAfterRenderTasks();
    }

    private void settings(HudRenderer r,double dt) {
        box(r,516,71,365,423,10,new Color(28,18,42));
        text(r,selected.title,532,85,17,WHITE);toggle(r,selected,836,87,dt);
        hit(830,80,44,30,selected::toggle);
        text(r,trim(selected.description,54),532,111,10,MUTED);
        List<Object> rows=new ArrayList<>();
        List<SettingGroup> groups=new ArrayList<>();selected.settings.forEach(groups::add);
        groups.sort(Comparator.comparingInt(g->g.name.equals("Zenith Addons")?0:1));
        for(SettingGroup group:groups) {
            List<Setting<?>> shown=new ArrayList<>();for(Setting<?> setting:group)if(setting.isVisible())shown.add(setting);
            if(!shown.isEmpty()){rows.add(group.name);rows.addAll(shown);}
        }
        double full=0;for(Object row:rows)full+=row instanceof String?28:45;
        settingScrollTarget=UiMotion.clamp(settingScrollTarget,0,Math.max(0,full-291));
        settingScroll=UiMotion.approach(settingScroll,settingScrollTarget,18,dt);
        double y=139-settingScroll;
        for(Object row:rows) {
            double h=row instanceof String?28:45;
            if(y>=138&&y+h<=443) {
                if(row instanceof String title)text(r,title.toUpperCase(Locale.ROOT),532,y+8,9,PURPLE);
                else setting(r,(Setting<?>)row,y,dt);
            }
            y+=h;
        }
        if(full>291)box(r,874,143+settingScroll/Math.max(1,full-291)*247,2,36,1,new Color(119,77,157));
        button(r,binding?"Press a key...":"Bind: "+selected.keybind,532,454,145,()->binding=true,dt);
        button(r,"Advanced editor",687,454,178,()->mc.gui.setScreen(theme.moduleScreen(selected)),dt);
    }
    private void setting(HudRenderer r,Setting<?> setting,double y,double dt) {
        Object value=setting.get();
        text(r,trim(setting.title,31),532,y+3,11,WHITE);
        if(value instanceof Boolean enabled) {
            double t=state(selected.name+":"+setting.name,enabled?1:0,dt);
            box(r,833,y+3,28,15,7,ZenithDraw.mix(new Color(62,46,78),new Color(158,93,225),t));
            box(r,836+t*12,y+6,9,9,4,WHITE);hit(529,y,335,32,()->set(setting,!(Boolean)setting.get()));
        } else if(value instanceof Number number&&(setting instanceof DoubleSetting||setting instanceof IntSetting)) {
            double min=setting instanceof DoubleSetting s?s.sliderMin:((IntSetting)setting).sliderMin;
            double max=setting instanceof DoubleSetting s?s.sliderMax:((IntSetting)setting).sliderMax;
            double t=UiMotion.clamp((number.doubleValue()-min)/Math.max(.00001,max-min),0,1);
            text(r,String.format(Locale.ROOT,value instanceof Integer?"%.0f":"%.2f",number.doubleValue()),810,y+3,10,PURPLE);
            box(r,532,y+27,329,3,1,new Color(57,39,76));box(r,532,y+27,329*t,3,1,PURPLE);
            box(r,529+329*t,y+24,8,8,4,WHITE);
            DoubleConsumer slider=x->{double v=min+UiMotion.clamp((x-532)/329,0,1)*(max-min);
                if(setting instanceof IntSetting s)set(s,(int)Math.round(UiMotion.clamp(v,s.min,s.max)));
                else {DoubleSetting s=(DoubleSetting)setting;set(s,UiMotion.clamp(v,s.min,s.max));}};
            hits.add(new Hit(527,y+19,340,18,()->{},slider));
        } else if(value instanceof Enum<?> mode) {
            box(r,702,y,160,23,5,new Color(44,29,60));text(r,trim(mode.toString(),23),710,y+6,10,PURPLE);
            hit(700,y,163,27,()->{Object[] modes=mode.getDeclaringClass().getEnumConstants();set(setting,modes[(mode.ordinal()+1)%modes.length]);});
        } else if(value instanceof SettingColor color) {
            box(r,831,y,30,20,5,color);
            hit(529,y,335,31,()->mc.gui.setScreen(theme.moduleScreen(selected)));
        } else {
            text(r,trim(String.valueOf(value),42),532,y+22,9,MUTED);
            hit(529,y,335,34,()->mc.gui.setScreen(theme.moduleScreen(selected)));
        }
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static void set(Setting setting,Object value) {setting.set(value);}

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubled) {
        if(closing)return false;
        mouseX=(event.x()*mc.getWindow().getGuiScale()-ox)/scale;mouseY=(event.y()*mc.getWindow().getGuiScale()-oy)/scale;
        searching=false;
        if(event.button()==1&&over(192,130,302,355)) {
            List<Module> list=filtered();int i=(int)((mouseY-130+moduleScroll)/55);
            if(i>=0&&i<list.size()){selected=list.get(i);settingScroll=settingScrollTarget=0;return true;}
        }
        if(event.button()!=0)return false;
        for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(over(h.x,h.y,h.w,h.h)){
            if(h.slider!=null){drag=h.slider;drag.accept(mouseX);}else h.action.run();return true;
        }}
        return false;
    }
    @Override public void mouseMoved(double x,double y) {if(drag!=null)drag.accept((x*mc.getWindow().getGuiScale()-ox)/scale);}
    @Override public boolean mouseReleased(MouseButtonEvent event){drag=null;return true;}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        double px=(x*mc.getWindow().getGuiScale()-ox)/scale;
        if(px<166){navScrollTarget=Math.max(0,navScrollTarget-vertical*34);return true;}
        if(px>516)settingScrollTarget=Math.max(0,settingScrollTarget-vertical*32);else moduleScrollTarget=Math.max(0,moduleScrollTarget-vertical*42);
        return true;
    }
    @Override public boolean charTyped(CharacterEvent event) {
        if(searching&&event.isAllowedChatCharacter()&&query.length()<100){query+=event.codepointAsString();moduleScroll=moduleScrollTarget=0;selectFirst();return true;}return false;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(binding){if(event.key()==GLFW.GLFW_KEY_ESCAPE||event.key()==GLFW.GLFW_KEY_BACKSPACE)selected.keybind.set(Keybind.none());else selected.keybind.set(Keybind.fromKey(event.key()));binding=false;return true;}
        if(event.key()==GLFW.GLFW_KEY_ESCAPE){if(searching){searching=false;}else onClose();return true;}
        if(searching&&event.key()==GLFW.GLFW_KEY_BACKSPACE&&!query.isEmpty()){query=query.substring(0,query.offsetByCodePoints(query.length(),-1));moduleScroll=moduleScrollTarget=0;selectFirst();return true;}
        if(searching&&event.key()==GLFW.GLFW_KEY_A&&(event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0){query="";selectFirst();return true;}
        if(event.key()==GLFW.GLFW_KEY_F&&(event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0){searching=true;return true;}
        return false;
    }
    @Override public void onClose(){closing=true;}
    @Override public boolean isPauseScreen(){return false;}
    public String selectedModuleName(){return selected==null?"":selected.name;}
}
