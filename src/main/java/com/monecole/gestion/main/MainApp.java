package com.monecole.gestion.main;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatLaf;
import com.monecole.gestion.dao.DaoFactory;
import com.monecole.gestion.utils.DatabaseSeeder;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Point d'entrée principal de l'application.
 * Initialise FlatLaf et lance l'interface de connexion.
 */
public class MainApp {

    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    public static void main(String[] args) {
        UIManager.put("Panel.background", new Color(245, 247, 250));
        applyLightTheme();

        SwingUtilities.invokeLater(() -> {
            try {
                DaoFactory.getInstance();
                DatabaseSeeder.seedIfEmpty();
                showLoginWindow();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur fatale lors du démarrage", e);
                JOptionPane.showMessageDialog(null,
                    "Erreur lors du démarrage : " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    private static void showLoginWindow() {
        JFrame loginFrame = new JFrame("Connexion - Application de Gestion Scolaire");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.add(new LoginPanel());
        loginFrame.pack();
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }

    public static void applyLightTheme() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            FlatLaf.updateUI();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de charger FlatLaf Light", e);
        }
    }

    public static void applyDarkTheme() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            FlatLaf.updateUI();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de charger FlatLaf Dark", e);
        }
    }
}
