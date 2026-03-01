package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;

import javax.swing.*;

@SystemForm(name = "Dashboard", description = "User dashboard for movie & HLS streaming desktop app")
public class FormDashboard extends Form {

    public FormDashboard() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap,fill,insets 20", "[center]", "[center]"));
        
        JPanel panel = new JPanel(new MigLayout("wrap,fillx,insets 40", "[center]", "[]20[]20[]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:20;" +
                "[light]background:tint($Panel.background,5%);" +
                "[dark]background:tint($Panel.background,2%);");
        
        // Icon
        JLabel icon = new JLabel(new FlatSVGIcon("raven/modal/demo/drawer/icon/dashboard.svg", 100, 100));
        
        JLabel title = new JLabel("HLS Streaming App");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +20;");
        
        JLabel desc = new JLabel("<html><div style='text-align: center; width: 400px;'>" +
                "Chào mừng bạn đến với ứng dụng xem phim trực tuyến chất lượng cao.<br>" +
                "Trải nghiệm xem phim mượt mà với công nghệ HLS, hỗ trợ đa nền tảng và tự động điều chỉnh chất lượng mạng." +
                "</div></html>");
        desc.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;font:+2");
        
        panel.add(icon);
        panel.add(title);
        panel.add(desc);
        
        add(panel);
    }

    @Override
    public void formInit() {
    }

    @Override
    public void formRefresh() {
    }
}
