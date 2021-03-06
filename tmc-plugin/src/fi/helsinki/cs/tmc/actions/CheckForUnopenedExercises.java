package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.OpenClosedExercisesDialog;
import fi.helsinki.cs.tmc.ui.TmcNotificationDisplayer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;

import org.openide.awt.NotificationDisplayer;
import org.openide.util.ImageUtilities;

public class CheckForUnopenedExercises implements ActionListener {
    public static boolean shouldRunOnStartup() {
        return ((TmcCoreSettingsImpl)TmcSettingsHolder.get()).isCheckingForUnopenedAtStartup();
    }
    
    private static final TmcNotificationDisplayer.SingletonToken NOTIFIER_TOKEN = TmcNotificationDisplayer.createSingletonToken();
    
    private ProjectMediator projects;
    private CourseDb courseDb;
    private TmcNotificationDisplayer notifier;

    public CheckForUnopenedExercises() {
        this.projects = ProjectMediator.getInstance();
        this.courseDb = CourseDb.getInstance();
        this.notifier = TmcNotificationDisplayer.getDefault();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }
    
    public void run() {
        projects.callWhenProjectsCompletelyOpened(new Runnable() {
            @Override
            public void run() {
                List<Exercise> unopenedExercises = unopenedExercises();
                if (countUncompleted(unopenedExercises) > 0) {
                    showNotification(unopenedExercises);
                }
            }
        });
    }
    
    private List<Exercise> unopenedExercises() {
        List<Exercise> unopenedExercises = new ArrayList<>();
        for (Exercise ex : courseDb.getCurrentCourseExercises()) {
            TmcProjectInfo project = projects.tryGetProjectForExercise(ex);
            if (project != null && !projects.isProjectOpen(project)) {
                unopenedExercises.add(ex);
            }
        }
        return unopenedExercises;
    }

    private void showNotification(List<Exercise> unopenedExercises) {
        int count = countUncompleted(unopenedExercises);
        String msg;
        String prompt;
        if (count == 1) {
            msg = "There is one exercise that is downloaded but not opened.";
            prompt = "Click here to open it.";
        } else {
            msg = "There are " + count + " exercises that are downloaded but not opened.";
            prompt = "Click here to open them.";
        }
        notifier.notify(NOTIFIER_TOKEN, msg, getNotificationIcon(), prompt, openAction(), NotificationDisplayer.Priority.LOW);
    }
    
    private int countUncompleted(List<Exercise> unopenedExercises) {
        int count = 0;
        for (Exercise ex : unopenedExercises) {
            if (!ex.isCompleted() && !ex.hasDeadlinePassed()) {
                count++;
            }
        }
        return count;
    }
    
    private ActionListener openAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenClosedExercisesDialog.display(unopenedExercises());
            }
        };
    }
    
    private Icon getNotificationIcon() {
        return ImageUtilities.loadImageIcon("fi/helsinki/cs/tmc/smile.gif", false);
    }
}
