package raven.modal.demo.auth;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.component.DropShadowBorder;
import raven.modal.demo.api.AuthApi;
import raven.modal.demo.component.LabelButton;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.AuthResponse;
import raven.modal.demo.system.Form;
import raven.modal.demo.system.FormManager;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

public class Register extends Form {

    private JLabel lbError;

    public Register() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("al center center"));
        createRegister();
    }

    private void createRegister() {
        JPanel panelRegister = new JPanel(new BorderLayout()) {
            @Override
            public void updateUI() {
                super.updateUI();
                applyShadowBorder(this);
            }
        };
        panelRegister.setOpaque(false);
        applyShadowBorder(panelRegister);

        JPanel registerContent = new JPanel(new MigLayout("fillx,wrap,insets 35 35 25 35", "[fill,300]"));

        JLabel lbTitle = new JLabel("Tạo tài khoản mới!");
        JLabel lbDescription = new JLabel("Điền thông tin để đăng ký");
        lbTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +12;");

        registerContent.add(lbTitle);
        registerContent.add(lbDescription);

        // Fields
        JTextField txtFullName = new JTextField();
        JTextField txtEmail = new JTextField();
        JPasswordField txtPassword = new JPasswordField();
        JPasswordField txtConfirmPassword = new JPasswordField();

        JButton cmdRegister = new JButton("Đăng ký") {
            @Override
            public boolean isDefaultButton() {
                return true;
            }
        };

        // placeholder
        txtFullName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập họ và tên");
        txtEmail.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập email của bạn");
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập mật khẩu");
        txtConfirmPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập lại mật khẩu");

        // style panel
        panelRegister.putClientProperty(FlatClientProperties.STYLE,
                "[dark]background:tint($Panel.background,1%);");
        registerContent.putClientProperty(FlatClientProperties.STYLE, "background:null;");

        // style input
        String inputStyle = "margin:4,10,4,10;arc:12;";
        txtFullName.putClientProperty(FlatClientProperties.STYLE, inputStyle);
        txtEmail.putClientProperty(FlatClientProperties.STYLE, inputStyle);

        txtPassword.putClientProperty(FlatClientProperties.STYLE,
                inputStyle + "showRevealButton:true;");
        txtConfirmPassword.putClientProperty(FlatClientProperties.STYLE,
                inputStyle + "showRevealButton:true;");

        cmdRegister.putClientProperty(FlatClientProperties.STYLE,
                "margin:4,10,4,10;arc:12;");

        // error label
        lbError = new JLabel(" ");
        lbError.putClientProperty(FlatClientProperties.STYLE,
                "foreground:#f87171;"); // đỏ nhạt

        // Layout
        registerContent.add(new JLabel("Họ Và Tên"), "gapy 25");
        registerContent.add(txtFullName);

        registerContent.add(new JLabel("Email"), "gapy 10");
        registerContent.add(txtEmail);

        registerContent.add(new JLabel("Password"), "gapy 10");
        registerContent.add(txtPassword);

        registerContent.add(new JLabel("Confirm Password"), "gapy 10");
        registerContent.add(txtConfirmPassword);

        registerContent.add(cmdRegister, "gapy 20");
        registerContent.add(lbError); // dòng hiển thị lỗi

        // Switch to login
        registerContent.add(showLogin(), "gapy 10");

        panelRegister.add(registerContent);
        add(panelRegister);

        // event register
        cmdRegister.addActionListener(e -> {
            String fullName = txtFullName.getText().trim();
            String email = txtEmail.getText().trim();
            String password = String.valueOf(txtPassword.getPassword());
            String confirm = String.valueOf(txtConfirmPassword.getPassword());

            // validate đơn giản
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                lbError.setText("Vui lòng nhập đủ thông tin!");
                JOptionPane.showMessageDialog(
                        SwingUtilities.windowForComponent(this),
                        "Vui lòng nhập đủ thông tin!",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            if (!password.equals(confirm)) {
                lbError.setText("Mật khẩu nhập lại không khớp!");
                JOptionPane.showMessageDialog(
                        SwingUtilities.windowForComponent(this),
                        "Mật khẩu nhập lại không khớp!",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            lbError.setText("Đang đăng ký...");
            cmdRegister.setEnabled(false);

            // call API trên background
            new SwingWorker<ApiResponse<AuthResponse>, Void>() {
                @Override
                protected ApiResponse<AuthResponse> doInBackground() {
                    return AuthApi.register(email, password, fullName);
                }

                @Override
                protected void done() {
                    cmdRegister.setEnabled(true);
                    try {
                        ApiResponse<AuthResponse> res = get();

                        if (!res.isSuccess()) {
                            String msg = res.getMessage() != null ? res.getMessage() : "Đăng ký thất bại";
                            lbError.setText(msg);
                            JOptionPane.showMessageDialog(
                                    SwingUtilities.windowForComponent(Register.this),
                                    msg,
                                    "Đăng ký thất bại",
                                    JOptionPane.ERROR_MESSAGE
                            );
                            return;
                        }

                        lbError.setText(" ");
                        JOptionPane.showMessageDialog(
                                SwingUtilities.windowForComponent(Register.this),
                                "Đăng ký thành công, vui lòng đăng nhập!",
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE
                        );

                        // quay về màn login (theo template của bạn đang dùng logout)
                        FormManager.logout();

                    } catch (InterruptedException | ExecutionException ex) {
                        ex.printStackTrace();
                        String msg = "Lỗi: " + ex.getCause().getMessage();
                        lbError.setText(msg);
                        JOptionPane.showMessageDialog(
                                SwingUtilities.windowForComponent(Register.this),
                                msg,
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }.execute();
        });
    }

    private JPanel showLogin() {
        JPanel panel = new JPanel(new MigLayout("wrap,al center", "[center]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "background:null;");

        JLabel lbQuestion = new JLabel("Bạn đã có tài khoản?");
        LabelButton lbLogin = new LabelButton("Đăng nhập");
        panel.add(lbQuestion, "split 2");
        panel.add(lbLogin);

        // theo template của bạn, logout sẽ quay về màn login
        lbLogin.addOnClick(e -> FormManager.logout());
        return panel;
    }

    private void applyShadowBorder(JPanel panel) {
        if (panel != null) {
            panel.setBorder(new DropShadowBorder(new Insets(5, 8, 12, 8), 1, 25));
        }
    }
}
