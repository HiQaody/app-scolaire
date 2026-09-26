package com.monecole.gestion.views;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.monecole.gestion.models.Utilisateur;
import com.monecole.gestion.views.classes.ClassManagementView;
import com.monecole.gestion.views.dashboard.DashboardView;
import com.monecole.gestion.views.enseignants.EnseignantManagementView;
import com.monecole.gestion.views.enseignements.EnseignementManagementView;
import com.monecole.gestion.views.grades.NoteManagementView;
import com.monecole.gestion.views.matieres.MatiereManagementView;
import com.monecole.gestion.views.reports.BulletinView;
import com.monecole.gestion.views.students.StudentManagementView;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fenêtre principale de l'application.
 * Contient un menu latéral (sidebar), une barre de titre personnalisée,
 * et une zone de travail dynamique via CardLayout.
 */
public class MainWindow extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(MainWindow.class.getName());
    private final Utilisateur currentUser;
    private final CardLayout cardLayout;
    private final JPanel workspacePanel;
    private boolean isDarkTheme = false;

    public MainWindow(Utilisateur user) {
        this.currentUser = user;
        this.cardLayout = new CardLayout();
        this.workspacePanel = new JPanel(cardLayout);

        setTitle("Application de Gestion Scolaire");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        add(buildNorthPanel(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildWorkspace(), BorderLayout.CENTER);

        setJMenuBar(buildMenuBar());
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Fichier");
        JMenuItem backupItem = new JMenuItem("Sauvegarder la base");
        backupItem.addActionListener(e -> backupDatabase());
        fileMenu.add(backupItem);
        fileMenu.addSeparator();
        JMenuItem quitItem = new JMenuItem("Quitter");
        quitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(quitItem);
        menuBar.add(fileMenu);

        JMenu toolsMenu = new JMenu("Outils");
        JMenuItem themeToggle = new JMenuItem("Basculer thème");
        themeToggle.addActionListener(e -> toggleTheme());
        toolsMenu.add(themeToggle);
        menuBar.add(toolsMenu);

        return menuBar;
    }

    private JPanel buildNorthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        left.setOpaque(false);
        JLabel titleLabel = new JLabel("Application de Gestion Scolaire");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        left.add(titleLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        right.setOpaque(false);
        right.add(new JLabel(currentUser.login() + " (" + currentUser.role().getLabel() + ")"));
        right.add(Box.createHorizontalStrut(10));
        JToggleButton themeBtn = new JToggleButton("🌙");
        themeBtn.setToolTipText("Basculer thème sombre/clair");
        themeBtn.setFocusPainted(false);
        themeBtn.addActionListener(e -> toggleTheme());
        right.add(themeBtn);
        right.add(Box.createHorizontalStrut(10));
        JButton logoutBtn = new JButton("Déconnexion");
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> logout());
        right.add(logoutBtn);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildSidebar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        String[][] items = {
            {"Dashboard", "dashboard"},
            {"Étudiants", "students"},
            {"Classes", "classes"},
            {"Matières", "matieres"},
            {"Enseignants", "enseignants"},
            {"Enseignements", "enseignements"},
            {"Notes", "grades"},
            {"Bulletins PDF", "reports"},
            {"Statistiques", "stats"}
        };

        for (String[] item : items) {
            JButton btn = new JButton(item[0]);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setFocusPainted(false);
            btn.setMaximumSize(new Dimension(200, 40));
            btn.addActionListener(e -> switchView(item[1]));
            panel.add(Box.createVerticalStrut(5));
            panel.add(btn);
        }

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildWorkspace() {
        workspacePanel.add(new DashboardView(), "dashboard");
        workspacePanel.add(new StudentManagementView(), "students");
        workspacePanel.add(new ClassManagementView(), "classes");
        workspacePanel.add(new MatiereManagementView(), "matieres");
        workspacePanel.add(new EnseignantManagementView(), "enseignants");
        workspacePanel.add(new EnseignementManagementView(), "enseignements");
        workspacePanel.add(new NoteManagementView(currentUser), "grades");
        workspacePanel.add(new BulletinView(), "reports");
        workspacePanel.add(Box.createHorizontalStrut(400), "stats");
        return workspacePanel;
    }

    private void switchView(String name) {
        cardLayout.show(workspacePanel, name);
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        try {
            if (isDarkTheme) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur lors du changement de thème", e);
        }
    }

    private void backupDatabase() {
        try {
            String backupPath = com.monecole.gestion.dao.DatabaseBackupUtil.backupDatabase();
            JOptionPane.showMessageDialog(this, "Sauvegarde créée :\n" + backupPath,
                "Sauvegarde", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Erreur lors de la sauvegarde : " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout() {
        int result = JOptionPane.showConfirmDialog(this,
            "Voulez-vous vraiment vous déconnecter ?",
            "Déconnexion", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                JFrame loginFrame = new JFrame("Connexion - Application de Gestion Scolaire");
                loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                loginFrame.add(new com.monecole.gestion.main.LoginPanel());
                loginFrame.pack();
                loginFrame.setLocationRelativeTo(null);
                loginFrame.setVisible(true);
            });
        }
    }
}
