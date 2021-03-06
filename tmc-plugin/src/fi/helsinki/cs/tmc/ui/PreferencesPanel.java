package fi.helsinki.cs.tmc.ui;

import com.google.common.base.Optional;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.configuration.TmcSettings;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Organization;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;
import fi.helsinki.cs.tmc.tailoring.SelectedTailoring;
import fi.helsinki.cs.tmc.tasks.LoginTask;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.DelayedRunner;
import fi.helsinki.cs.tmc.utilities.LoginManager;
import fi.helsinki.cs.tmc.utilities.ThrowingFunction;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The settings panel.
 *
 * This is missing the "OK" and "Cancel" buttons because it is placed in
 * a dialog that provides these.
 */
/*package*/ class PreferencesPanel extends JPanel implements PreferencesUI {

    private ConvenientDialogDisplayer dialogs = ConvenientDialogDisplayer.getDefault();

    private DelayedRunner refreshRunner = new DelayedRunner();
    
    private final TmcSettings settings = TmcSettingsHolder.get();

    /*package*/ PreferencesPanel() {
        initComponents();
        setUpErrorMsgLocaleSelection();
        updateFields();
    }
    
    private void updateFields() {
        final Optional<String> email = this.settings.getEmail();
        final Optional<String> username = this.settings.getUsername();
        final JLabel login = this.loginLabel;
        final JButton logout = this.logoutButton;
        if (email.isPresent() && !email.get().isEmpty()) {
            login.setText("Logged in as " + email.get());
            logout.setEnabled(true);
        } else if (username.isPresent()) {
            login.setText("Logged in as " + username.get());
            logout.setEnabled(true);
        } else {
            login.setText("Not logged in!");
            logout.setEnabled(false);
        }
        
        Optional<Organization> org = this.settings.getOrganization();
        final JLabel selectedOrg = this.selectedOrganizationLabel;
        if (org.isPresent()) {
            selectedOrg.setText(org.get().getName());
        } else {
            selectedOrg.setText("No organization selected");
        }
        
        Optional<Course> course = this.settings.getCurrentCourse();
        final JLabel selectedCourse = this.selectedCourseLabel;
        if (course.isPresent()) {
            selectedCourse.setText(course.get().getTitle());
        } else {
            selectedCourse.setText("No course selected");
        }
    }
    
    @Override
    public List<Course> getAvailableCourses() throws Exception {
        List<Course> courses = TmcCore.get().listCourses(ProgressObserver.NULL_OBSERVER).call();
        return courses;
    }

    @Override
    public String getProjectDir() {
        return projectFolderTextField.getText().trim();
    }

    @Override
    public void setProjectDir(String projectDir) {
        projectFolderTextField.setText(projectDir);
    }

    public void setSelectedCourse(Course course) {
        this.settings.setCourse(Optional.of(course));
        this.selectedCourseLabel.setText(course.getTitle());
    }
    
    @Override
    public String getSelectedCourseName() {
        final Optional<Course> currentCourse = this.settings.getCurrentCourse();
        if (currentCourse.isPresent()) {
            return currentCourse.get().getName();
        }
        return null;
    }

    @Override
    public boolean getCheckForUpdatesInTheBackground() {
        return checkForUpdatesInBackgroundCheckbox.isSelected();
    }

    @Override
    public void setCheckForUpdatesInTheBackground(boolean shouldCheck) {
        checkForUpdatesInBackgroundCheckbox.setSelected(shouldCheck);
    }

    @Override
    public boolean getCheckForUnopenedExercisesAtStartup() {
        return checkForUnopenedExercisesCheckbox.isSelected();
    }

    @Override
    public void setCheckForUnopenedExercisesAtStartup(boolean shouldCheck) {
        checkForUnopenedExercisesCheckbox.setSelected(shouldCheck);
    }
    
    @Override
    public boolean getFixUnoptimalSettingsEnabled() {
        return fixUnoptimalSettings.isSelected();
    }
    
    @Override
    public void setFixUnoptimalSettingsEnabled(boolean value) {
        fixUnoptimalSettings.setSelected(value);
    }

    @Override
    public Locale getErrorMsgLocale() {
        Object item = errorMsgLocaleComboBox.getSelectedItem();
        if (item != null) {
            return ((LocaleWrapper)item).getLocale();
        } else {
            return new Locale("en_US");
        }
    }

    @Override
    public void setErrorMsgLocale(Locale locale) {
        errorMsgLocaleComboBox.setSelectedItem(new LocaleWrapper(locale));
    }

    @Override
    public boolean getSendDiagnosticsEnabled() {
        return sendDiagnostics.isSelected();
    }

    @Override
    public void setSendDiagnosticsEnabled(boolean value) {
        sendDiagnostics.setSelected(value);
    }
    
    public void setOrganization(Organization organization) {
        Optional<Organization> oldOrganization = this.settings.getOrganization();
        Organization newOrganization = organization;

        if (!oldOrganization.isPresent() || (oldOrganization.isPresent() && !oldOrganization.get().getSlug().equals(newOrganization.getSlug()))) {
            this.settings.setOrganization(Optional.of(newOrganization));
            this.settings.setCourse(Optional.<Course>absent());
            this.selectedOrganizationLabel.setText(newOrganization.getName());
            this.selectedCourseLabel.setText("No course selected");
        }
    }
    
    private static class LocaleWrapper {
        private Locale locale;
        public LocaleWrapper(Locale locale) {
            this.locale = locale;
        }

        public Locale getLocale() {
            return locale;
        }

        @Override
        public String toString() {
            return locale.getDisplayLanguage();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            return obj instanceof LocaleWrapper && this.locale.equals(((LocaleWrapper) obj).locale);
        }

        @Override
        public int hashCode() {
            return locale.hashCode();
        }
    }

    private void updateSettingsForRefresh() {
        TmcCoreSettingsImpl tmcSettings = (TmcCoreSettingsImpl)this.settings;
        tmcSettings.setProjectRootDir(getProjectDir());
        tmcSettings.save(); // TODO: is this wanted
    }

    private void setUpErrorMsgLocaleSelection() {

        restartMessage.setText("");

        for (Locale locale : SelectedTailoring.get().getAvailableErrorMsgLocales()) {
            errorMsgLocaleComboBox.addItem(new LocaleWrapper(locale));
        }

        errorMsgLocaleComboBox.setSelectedItem(SelectedTailoring.get().getDefaultErrorMsgLocale());
        errorMsgLocaleComboBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent event) {

                if (event.getStateChange() == ItemEvent.SELECTED) {

                    // Language changed, notify user about restarting
                    if (!((TmcCoreSettingsImpl)TmcSettingsHolder.get()).getErrorMsgLocale().equals(getErrorMsgLocale())) {
                        restartMessage.setText("Changing language requires restart");
                    } else {
                        restartMessage.setText("");
                    }
                }
            }
        });
    }
    
    private void wrapWithExceptionHandling(ThrowingFunction function) {
        try {
            function.apply();
        } catch (IOException ex) {
            dialogs.displayError("Couldn't connect to the server! Please check your internet connection.");
        } catch (Exception ex) {
            dialogs.displayError(ex.getMessage());
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        projectFolderLabel = new javax.swing.JLabel();
        projectFolderTextField = new javax.swing.JTextField();
        folderChooserBtn = new javax.swing.JButton();
        changeCourseButton = new javax.swing.JButton();
        coursesLabel = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        checkForUpdatesInBackgroundCheckbox = new javax.swing.JCheckBox();
        checkForUnopenedExercisesCheckbox = new javax.swing.JCheckBox();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        errorMsgLocaleLabel = new javax.swing.JLabel();
        errorMsgLocaleComboBox = new javax.swing.JComboBox();
        restartMessage = new javax.swing.JLabel();
        fixUnoptimalSettings = new javax.swing.JCheckBox();
        sendDiagnostics = new javax.swing.JCheckBox();
        organizationLabel = new javax.swing.JLabel();
        changeOrganizationButton = new javax.swing.JButton();
        selectedCourseLabel = new javax.swing.JLabel();
        loginLabel = new javax.swing.JLabel();
        logoutButton = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        selectedOrganizationLabel = new javax.swing.JLabel();

        projectFolderLabel.setLabelFor(projectFolderTextField);
        projectFolderLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.projectFolderLabel.text")); // NOI18N

        projectFolderTextField.setEditable(false);
        projectFolderTextField.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.projectFolderTextField.text")); // NOI18N
        projectFolderTextField.setEnabled(false);
        projectFolderTextField.setPreferredSize(new java.awt.Dimension(250, 27));

        folderChooserBtn.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.folderChooserBtn.text")); // NOI18N
        folderChooserBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                folderChooserBtnActionPerformed(evt);
            }
        });

        changeCourseButton.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.changeCourseButton.text")); // NOI18N
        changeCourseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeCourseButtonActionPerformed(evt);
            }
        });

        coursesLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.coursesLabel.text")); // NOI18N

        checkForUpdatesInBackgroundCheckbox.setSelected(true);
        checkForUpdatesInBackgroundCheckbox.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.checkForUpdatesInBackgroundCheckbox.text")); // NOI18N

        checkForUnopenedExercisesCheckbox.setSelected(true);
        checkForUnopenedExercisesCheckbox.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.checkForUnopenedExercisesCheckbox.text")); // NOI18N
        checkForUnopenedExercisesCheckbox.setToolTipText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.checkForUnopenedExercisesCheckbox.toolTipText")); // NOI18N

        errorMsgLocaleLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.errorMsgLocaleLabel.text")); // NOI18N
        errorMsgLocaleLabel.setToolTipText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.errorMsgLocaleLabel.toolTipText")); // NOI18N

        errorMsgLocaleComboBox.setToolTipText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.errorMsgLocaleComboBox.toolTipText")); // NOI18N

        restartMessage.setFont(new java.awt.Font("Ubuntu", 1, 14)); // NOI18N
        restartMessage.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.restartMessage.text")); // NOI18N

        fixUnoptimalSettings.setSelected(true);
        fixUnoptimalSettings.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.fixUnoptimalSettings.text")); // NOI18N
        fixUnoptimalSettings.setToolTipText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.fixUnoptimalSettings.toolTipText")); // NOI18N
        fixUnoptimalSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fixUnoptimalSettingsActionPerformed(evt);
            }
        });

        sendDiagnostics.setSelected(true);
        sendDiagnostics.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.sendDiagnostics.text")); // NOI18N
        sendDiagnostics.setToolTipText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.sendDiagnostics.toolTipText")); // NOI18N
        sendDiagnostics.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendDiagnosticsActionPerformed(evt);
            }
        });

        organizationLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.organizationLabel.text")); // NOI18N

        changeOrganizationButton.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.changeOrganizationButton.text")); // NOI18N
        changeOrganizationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeOrganizationButtonActionPerformed(evt);
            }
        });

        selectedCourseLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.selectedCourseLabel.text")); // NOI18N

        loginLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.loginLabel.text")); // NOI18N

        logoutButton.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.logoutButton.text")); // NOI18N
        logoutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutButtonActionPerformed(evt);
            }
        });

        selectedOrganizationLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.selectedOrganizationLabel.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(restartMessage)
                .addGap(22, 22, 22))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fixUnoptimalSettings)
                            .addComponent(sendDiagnostics))
                        .addGap(0, 216, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator1)
                            .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(projectFolderLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(projectFolderTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 381, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(folderChooserBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jSeparator3)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(errorMsgLocaleLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(errorMsgLocaleComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(coursesLabel)
                                    .addComponent(organizationLabel))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(selectedOrganizationLabel)
                                    .addComponent(selectedCourseLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 353, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(changeOrganizationButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(changeCourseButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(160, 160, 160))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(loginLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(logoutButton))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(checkForUnopenedExercisesCheckbox)
                                    .addComponent(checkForUpdatesInBackgroundCheckbox))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jSeparator4))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(loginLabel)
                    .addComponent(logoutButton))
                .addGap(3, 3, 3)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(organizationLabel)
                    .addComponent(changeOrganizationButton)
                    .addComponent(selectedOrganizationLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(coursesLabel)
                    .addComponent(changeCourseButton)
                    .addComponent(selectedCourseLabel))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 6, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(folderChooserBtn)
                    .addComponent(projectFolderTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(projectFolderLabel))
                .addGap(18, 18, 18)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(checkForUpdatesInBackgroundCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkForUnopenedExercisesCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fixUnoptimalSettings)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sendDiagnostics)
                .addGap(18, 18, 18)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(errorMsgLocaleLabel)
                    .addComponent(errorMsgLocaleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(restartMessage)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * The method is used to select the default save folder for downloaded exercises.
     * It is called when the user presses the "Browse" button on the preferences window.
     * @param evt
     */
    private void folderChooserBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_folderChooserBtnActionPerformed
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int choice = folderChooser.showOpenDialog(this);
        if (choice == JFileChooser.CANCEL_OPTION) {
            return;
        }
        File projectDefaultFolder = folderChooser.getSelectedFile();
        
        final String projectDefaultFolderPath = projectDefaultFolder.getAbsolutePath();

        final String errorMsg = "OneDrive doesn't work with the Test My Code plugin for Netbeans.\nPlease use another folder for your projects.";

        if (projectDefaultFolderPath.toLowerCase().contains("onedrive")) {
            dialogs.displayError(errorMsg);
            return;
        }

        projectFolderTextField.setText(projectDefaultFolderPath);
    }//GEN-LAST:event_folderChooserBtnActionPerformed

    private void changeCourseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeCourseButtonActionPerformed
        wrapWithExceptionHandling(CourseListWindow::display);

        updateSettingsForRefresh();
    }//GEN-LAST:event_changeCourseButtonActionPerformed

    private void fixUnoptimalSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fixUnoptimalSettingsActionPerformed
       
    }//GEN-LAST:event_fixUnoptimalSettingsActionPerformed

    private void sendDiagnosticsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendDiagnosticsActionPerformed
        TmcCoreSettingsImpl tmcSettings = (TmcCoreSettingsImpl) this.settings;
        tmcSettings.setSendDiagnostics(getSendDiagnosticsEnabled());
    }//GEN-LAST:event_sendDiagnosticsActionPerformed

    private void changeOrganizationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeOrganizationButtonActionPerformed
        this.selectedCourseLabel.setText("No course selected");
        
        wrapWithExceptionHandling(OrganizationListWindow::display);
        
        final Optional<Course> currentCourse = this.settings.getCurrentCourse();
        if (currentCourse.isPresent()) {
            this.selectedCourseLabel.setText(currentCourse.get().getTitle());
        }
        
        updateSettingsForRefresh();
    }//GEN-LAST:event_changeOrganizationButtonActionPerformed

    private void logoutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoutButtonActionPerformed
        LoginManager manager = new LoginManager();
        manager.logout();
        updateFields();
        
        JDialog window = (JDialog) SwingUtilities.getWindowAncestor(this);
        window.setVisible(false);
        window.dispose();
        
        BgTask.start("Logged out. Asking to log in.", new LoginTask());
    }//GEN-LAST:event_logoutButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton changeCourseButton;
    private javax.swing.JButton changeOrganizationButton;
    private javax.swing.JCheckBox checkForUnopenedExercisesCheckbox;
    private javax.swing.JCheckBox checkForUpdatesInBackgroundCheckbox;
    private javax.swing.JLabel coursesLabel;
    private javax.swing.JComboBox errorMsgLocaleComboBox;
    private javax.swing.JLabel errorMsgLocaleLabel;
    private javax.swing.JCheckBox fixUnoptimalSettings;
    private javax.swing.JButton folderChooserBtn;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JLabel loginLabel;
    private javax.swing.JButton logoutButton;
    private javax.swing.JLabel organizationLabel;
    private javax.swing.JLabel projectFolderLabel;
    private javax.swing.JTextField projectFolderTextField;
    private javax.swing.JLabel restartMessage;
    private javax.swing.JLabel selectedCourseLabel;
    private javax.swing.JLabel selectedOrganizationLabel;
    private javax.swing.JCheckBox sendDiagnostics;
    // End of variables declaration//GEN-END:variables

}
