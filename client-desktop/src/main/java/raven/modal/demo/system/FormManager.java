package raven.modal.demo.system;

import raven.modal.Drawer;
import raven.modal.ModalDialog;
import raven.modal.component.SimpleModalBorder;
import raven.modal.demo.api.AuthApi;
import raven.modal.demo.auth.Login;
import raven.modal.demo.component.About;
import raven.modal.demo.forms.FormDashboard;
import raven.modal.demo.forms.admin.FormDashboardAdmin;
import raven.modal.demo.menu.MyDrawerBuilder;
import raven.modal.demo.utils.LogoutStateManager;
import raven.modal.demo.utils.UndoRedo;

import javax.swing.*;

public class FormManager {

    protected static final UndoRedo<Form> FORMS = new UndoRedo<>();

    private static JFrame frame;

    private static MainForm mainForm;
    private static MainFormAdmin mainFormAdmin;

    private static Login login;

    // đang hiển thị MainForm hoặc MainFormAdmin
    private static JPanel activeMain;

    public static void install(JFrame f) {
        frame = f;
        installKeyMap();
        logout();
    }

    private static void installKeyMap() {
        // cài cho cả 2 để lúc nào cũng dùng được
        FormSearch.getInstance().installKeyMap(getMainForm());
        FormSearch.getInstance().installKeyMap(getMainFormAdmin());
    }

    // helper: set form vào đúng main hiện tại
    private static void setFormToActiveMain(Form form) {
        if (activeMain instanceof MainForm) {
            ((MainForm) activeMain).setForm(form);
            ((MainForm) activeMain).refresh();
        } else if (activeMain instanceof MainFormAdmin) {
            ((MainFormAdmin) activeMain).setForm(form);
            ((MainFormAdmin) activeMain).refresh();
        }
    }

    private static void refreshActiveMain() {
        if (activeMain instanceof MainForm) {
            ((MainForm) activeMain).refresh();
        } else if (activeMain instanceof MainFormAdmin) {
            ((MainFormAdmin) activeMain).refresh();
        }
    }

    public static void showForm(Form form) {
        if (form != FORMS.getCurrent()) {
            FORMS.add(form);
            form.formCheck();
            form.formOpen();
            setFormToActiveMain(form);
        }
    }

    public static void undo() {
        if (FORMS.isUndoAble()) {
            Form form = FORMS.undo();
            form.formCheck();
            form.formOpen();
            setFormToActiveMain(form);
            Drawer.setSelectedItemClass(form.getClass());
        }
    }

    public static void redo() {
        if (FORMS.isRedoAble()) {
            Form form = FORMS.redo();
            form.formCheck();
            form.formOpen();
            setFormToActiveMain(form);
            Drawer.setSelectedItemClass(form.getClass());
        }
    }

    public static void refresh() {
        if (FORMS.getCurrent() != null) {
            FORMS.getCurrent().formRefresh();
            refreshActiveMain();
        }
    }

    // login user
    public static void login() {
        LogoutStateManager.reset();
        Drawer.setVisible(true);
        frame.getContentPane().removeAll();

        MainForm mf = getMainForm();
        activeMain = mf;

        frame.getContentPane().add(mf);

        // clear history khi login để tránh dính form cũ
        FORMS.clear();

        // mở dashboard user mặc định
        Drawer.setSelectedItemClass(FormDashboard.class);

        frame.repaint();
        frame.revalidate();
    }

    // login admin
    public static void loginAdmin() {
        LogoutStateManager.reset();
        Drawer.setVisible(true);
        frame.getContentPane().removeAll();

        MainFormAdmin mf = getMainFormAdmin();
        activeMain = mf;

        frame.getContentPane().add(mf);

        // clear history khi login để tránh dính form cũ
        FORMS.clear();

        // mở dashboard admin mặc định
        Drawer.setSelectedItemClass(FormDashboardAdmin.class);

        frame.repaint();
        frame.revalidate();
    }

    public static void logout() {
        var user = MyDrawerBuilder.getInstance().getUser();
        if (user != null) {
            AuthApi.logout(user.getUserId());
        }
        
        Drawer.setVisible(false);
        frame.getContentPane().removeAll();

        Form lg = getLogin();
        lg.formCheck();

        frame.getContentPane().add(lg);
        FORMS.clear();

        activeMain = null;

        frame.repaint();
        frame.revalidate();
    }

    // show register form
    public static void showRegister() {
        Drawer.setVisible(false);
        frame.getContentPane().removeAll();

        Form register = new raven.modal.demo.auth.Register();
        register.formCheck();
        frame.getContentPane().add(register);

        FORMS.clear();
        activeMain = null;

        frame.repaint();
        frame.revalidate();
    }

    public static JFrame getFrame() {
        return frame;
    }

    private static MainForm getMainForm() {
        if (mainForm == null) {
            mainForm = new MainForm();
        }
        return mainForm;
    }

    private static MainFormAdmin getMainFormAdmin() {
        if (mainFormAdmin == null) {
            mainFormAdmin = new MainFormAdmin();
        }
        return mainFormAdmin;
    }

    private static Login getLogin() {
        if (login == null) {
            login = new Login();
        }
        return login;
    }

    public static void showAbout() {
        ModalDialog.showModal(frame,
                new SimpleModalBorder(new About(), "About"),
                ModalDialog.createOption().setAnimationEnabled(false)
        );
    }
}
