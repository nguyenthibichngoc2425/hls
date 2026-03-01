package raven.modal.demo;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.util.FontUtils;
import raven.modal.Drawer;
import raven.modal.demo.api.AuthApi;
import raven.modal.demo.menu.MyDrawerBuilder;
import raven.modal.demo.system.FormManager;
import raven.modal.demo.utils.DemoPreferences;
import raven.modal.demo.utils.GenreManager;
import raven.modal.demo.utils.LogoutStateManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
public class ClientDesktopApplication extends JFrame {

    public static final String DEMO_VERSION = "2.6.0-SNAPSHOT";

    public ClientDesktopApplication() {
        init();
    }

    private void init() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getRootPane().putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true);
        Drawer.installDrawer(this, MyDrawerBuilder.getInstance());
        FormManager.install(this);
        setSize(new Dimension(1366, 768));
        setLocationRelativeTo(null);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!LogoutStateManager.isLoggedOut()) {
                    var user = MyDrawerBuilder.getInstance().getUser();
                    if (user != null) {
                        AuthApi.logout(user.getUserId());
                    }
                }
            }
        });
        
        new Thread(() -> GenreManager.getInstance().loadGenres()).start();
    }

    public static void main(String[] args) {
        DemoPreferences.init();
        FlatRobotoFont.install();
        FlatLaf.registerCustomDefaultsSource("raven.modal.demo.themes");
        UIManager.put("defaultFont", FontUtils.getCompositeFont(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        DemoPreferences.setupLaf();
        EventQueue.invokeLater(() -> new ClientDesktopApplication().setVisible(true));
    }
}
