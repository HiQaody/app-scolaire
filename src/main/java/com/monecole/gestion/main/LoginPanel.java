package com.monecole.gestion.main;

import com.monecole.gestion.models.Utilisateur;
import com.monecole.gestion.services.AuthService;
import com.monecole.gestion.views.MainWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Panneau de connexion (Login).
 */
public class LoginPanel extends JPanel {

    private final AuthService authService = new AuthService();
    private final JTextField loginField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel errorLabel = new JLabel(" ");

    public LoginPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(buildTitlePanel(), BorderLayout.NORTH);
        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildTitlePanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        JLabel title = new JLabel("Connexion à l'application");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(title);
        return panel;
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0;
        panel.add(new JLabel("Login :"), gc);
        gc.gridx = 1;
        panel.add(loginField, gc);

        gc.gridx = 0; gc.gridy = 1;
        panel.add(new JLabel("Mot de passe :"), gc);
        gc.gridx = 1;
        panel.add(passwordField, gc);

        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.CENTER;
        errorLabel.setForeground(Color.RED);
        panel.add(errorLabel, gc);

        return panel;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        JButton loginBtn = new JButton("Se connecter");
        loginBtn.addActionListener(this::onLogin);
        panel.add(loginBtn);
        return panel;
    }

    private void onLogin(ActionEvent e) {
        String login = loginField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (login.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        Utilisateur user = authService.authenticate(login, password);
        if (user != null) {
            SwingUtilities.getWindowAncestor(this).dispose();
            openMainWindow(user);
        } else {
            showError("Login ou mot de passe incorrect.");
            passwordField.setText("");
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
    }

    private void openMainWindow(Utilisateur user) {
        JFrame mainFrame = new MainWindow(user);
        mainFrame.setVisible(true);
    }
}
