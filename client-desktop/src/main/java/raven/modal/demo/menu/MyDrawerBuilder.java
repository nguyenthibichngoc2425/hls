package raven.modal.demo.menu;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import raven.extras.AvatarIcon;
import raven.modal.demo.ClientDesktopApplication;
import raven.modal.demo.forms.*;
import raven.modal.demo.forms.admin.FormDashboardAdmin;
import raven.modal.demo.forms.admin.FormGenreManagement;
import raven.modal.demo.forms.admin.FormMovieManagement;
import raven.modal.demo.model.ModelUser;
import raven.modal.demo.system.AllForms;
import raven.modal.demo.system.Form;
import raven.modal.demo.system.FormManager;
import raven.modal.drawer.DrawerPanel;
import raven.modal.drawer.item.Item;
import raven.modal.drawer.item.MenuItem;
import raven.modal.drawer.menu.MenuAction;
import raven.modal.drawer.menu.MenuEvent;
import raven.modal.drawer.menu.MenuOption;
import raven.modal.drawer.menu.MenuStyle;
import raven.modal.drawer.renderer.DrawerStraightDotLineStyle;
import raven.modal.drawer.simple.SimpleDrawerBuilder;
import raven.modal.drawer.simple.footer.LightDarkButtonFooter;
import raven.modal.drawer.simple.footer.SimpleFooterData;
import raven.modal.drawer.simple.header.SimpleHeader;
import raven.modal.drawer.simple.header.SimpleHeaderData;
import raven.modal.option.Option;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class MyDrawerBuilder extends SimpleDrawerBuilder {

    // marker classes để nhận diện About/Logout (không phụ thuộc index)
    public static final class ActionAbout {}
    public static final class ActionLogout {}

    private static MyDrawerBuilder instance;

    // giữ reference MenuOption đã truyền vào super()
    private static MenuOption MENU_OPTION_REF;

    private ModelUser user;

    private final int SHADOW_SIZE = 12;

    public static MyDrawerBuilder getInstance() {
        if (instance == null) {
            instance = new MyDrawerBuilder();
        }
        return instance;
    }

    public ModelUser getUser() {
        return user;
    }

    public void setUser(ModelUser user) {
        boolean roleChanged = this.user == null || this.user.getRole() != user.getRole();

        this.user = user;

        // set user to menu validation
        MyMenuValidation.setUser(user);

        // setup drawer header
        updateHeader(user);

        if (roleChanged) {
            rebuildMenuByRole();
        }
    }

    private MyDrawerBuilder() {
        super(createSimpleMenuOption());

        // mode change listener
        LightDarkButtonFooter lightDarkButtonFooter = (LightDarkButtonFooter) getFooter();
        lightDarkButtonFooter.addModeChangeListener(isDarkMode -> {
            // event for light dark mode changed
        });

        // nếu chưa login thì mặc định build menu user
        rebuildMenuByRole();
    }

    private void updateHeader(ModelUser user) {
        SimpleHeader header = (SimpleHeader) getHeader();
        SimpleHeaderData data = header.getSimpleHeaderData();

        AvatarIcon icon = (AvatarIcon) data.getIcon();
        String iconName = user.getRole() == ModelUser.Role.ADMIN ? "avatar_male.svg" : "avatar_female.svg";

        icon.setIcon(new FlatSVGIcon("raven/modal/demo/drawer/image/" + iconName, 100, 100));
        data.setTitle(user.getUserName());
        data.setDescription(user.getMail());
        header.setSimpleHeaderData(data);
    }

    @Override
    public SimpleHeaderData getSimpleHeaderData() {
        AvatarIcon icon = new AvatarIcon(
                new FlatSVGIcon("raven/modal/demo/drawer/image/avatar_male.svg", 100, 100),
                50, 50, 99.5f
        );
        icon.setBorder(2, 2);

        changeAvatarIconBorderColor(icon);

        UIManager.addPropertyChangeListener(evt -> {
            if ("lookAndFeel".equals(evt.getPropertyName())) {
                changeAvatarIconBorderColor(icon);
            }
        });

        return new SimpleHeaderData()
                .setIcon(icon)
                .setTitle("Ra Ven")
                .setDescription("raven@gmail.com");
    }

    private void changeAvatarIconBorderColor(AvatarIcon icon) {
        icon.setBorderColor(new AvatarIcon.BorderColor(UIManager.getColor("Component.accentColor"), 0.7f));
    }

    @Override
    public SimpleFooterData getSimpleFooterData() {
        return new SimpleFooterData()
                .setTitle("Swing Modal Dialog")
                .setDescription("Version " + ClientDesktopApplication.DEMO_VERSION);
    }

    @Override
    public Option createOption() {
        Option option = super.createOption();
        option.setOpacity(0.3f);
        option.getBorderOption().setShadowSize(new Insets(0, 0, 0, SHADOW_SIZE));
        return option;
    }

    // ---------- MENU SPLIT USER/ADMIN ----------

    private void rebuildMenuByRole() {
        if (MENU_OPTION_REF == null) return;

        ModelUser.Role role = (user != null) ? user.getRole() : ModelUser.Role.USER;

        MenuItem[] items = (role == ModelUser.Role.ADMIN)
                ? createAdminMenus()
                : createUserMenus();

        MENU_OPTION_REF.setMenus(items)
                .setBaseIconPath("raven/modal/demo/drawer/icon")
                .setIconScale(0.45f);

        // rebuild drawer UI
        rebuildMenu();
    }

    private MenuItem[] createUserMenus() {
        return new MenuItem[]{
                new Item.Label("PHIM"),
                new Item("Trang chủ", "dashboard.svg", FormDashboard.class),
                new Item("Phim", "forms.svg", FormAllMovies.class),
                new Item("Lịch sử xem", "clock.svg", FormWatchHistory.class),
                new Item("Yêu thích", "heart.svg", FormFavoriteMovies.class),

                new Item.Label("OTHER"),
                new Item("Setting", "setting.svg", FormSetting.class),
                new Item("About", "about.svg", ActionAbout.class),
                new Item("Logout", "logout.svg", ActionLogout.class)
        };
    }

    private MenuItem[] createAdminMenus() {
        return new MenuItem[]{
                new Item.Label("ADMIN"),
                new Item("Trang chủ", "dashboard.svg", FormDashboardAdmin.class),
                new Item("Quản lý phim", "forms.svg", FormMovieManagement.class),
                new Item("Quản lý thể loại", "forms.svg", FormGenreManagement.class),
                new Item("Quản lý người dùng", "forms.svg", raven.modal.demo.forms.admin.FormUserManagement.class),

                // Bạn thêm form admin khác ở đây (nếu có)
                // new Item("User Management", "forms.svg", FormUserManagement.class),
                // new Item("Reports", "pack.svg", FormReports.class),

                new Item.Label("OTHER"),
                new Item("Setting", "setting.svg", FormSetting.class),
                new Item("About", "about.svg", ActionAbout.class),
                new Item("Logout", "logout.svg", ActionLogout.class)
        };
    }

    // ---------- OPTION + EVENT ----------

    public static MenuOption createSimpleMenuOption() {

        MenuOption simpleMenuOption = new MenuOption();
        MENU_OPTION_REF = simpleMenuOption;

        simpleMenuOption.setMenuStyle(new MenuStyle() {

            @Override
            public void styleMenuItem(JButton menu, int[] index, boolean isMainItem) {
                boolean isTopLevel = index.length == 1;
                if (isTopLevel) {
                    menu.putClientProperty(FlatClientProperties.STYLE,
                            "margin:-1,0,-1,0;");
                }
            }

            @Override
            public void styleMenu(JComponent component) {
                component.putClientProperty(FlatClientProperties.STYLE, getDrawerBackgroundStyle());
            }
        });

        simpleMenuOption.getMenuStyle().setDrawerLineStyleRenderer(new DrawerStraightDotLineStyle());
        simpleMenuOption.setMenuValidation(new MyMenuValidation());

        simpleMenuOption.addMenuEvent(new MenuEvent() {
            @Override
            public void selected(MenuAction action, int[] index) {
                System.out.println("Drawer menu selected " + Arrays.toString(index));

                Class<?> itemClass = action.getItem().getItemClass();

                // About / Logout không phụ thuộc index nữa
                if (itemClass == ActionAbout.class) {
                    action.consume();
                    FormManager.showAbout();
                    return;
                }
                if (itemClass == ActionLogout.class) {
                    action.consume();
                    FormManager.logout();
                    return;
                }

                // chỉ xử lý form thật sự
                if (itemClass == null || !Form.class.isAssignableFrom(itemClass)) {
                    action.consume();
                    return;
                }

                @SuppressWarnings("unchecked")
                Class<? extends Form> formClass = (Class<? extends Form>) itemClass;
                FormManager.showForm(AllForms.getForm(formClass));
            }
        });

        // NOTE: menus sẽ được set sau (theo role) ở rebuildMenuByRole()
        simpleMenuOption.setMenus(new MenuItem[]{})
                .setBaseIconPath("raven/modal/demo/drawer/icon")
                .setIconScale(0.45f);

        return simpleMenuOption;
    }

    @Override
    public int getDrawerWidth() {
        return 270 + SHADOW_SIZE;
    }

    @Override
    public int getDrawerCompactWidth() {
        return 80 + SHADOW_SIZE;
    }

    @Override
    public int getOpenDrawerAt() {
        return 1000;
    }

    @Override
    public boolean openDrawerAtScale() {
        return false;
    }

    @Override
    public void build(DrawerPanel drawerPanel) {
        drawerPanel.putClientProperty(FlatClientProperties.STYLE, getDrawerBackgroundStyle());
    }

    private static String getDrawerBackgroundStyle() {
        return "" +
                "[light]background:tint($Panel.background,20%);" +
                "[dark]background:tint($Panel.background,5%);";
    }
}
