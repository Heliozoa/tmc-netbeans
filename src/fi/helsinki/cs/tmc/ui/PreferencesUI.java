package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.Course;
import java.util.List;

public interface PreferencesUI {

    String getProjectDir();

    Course getSelectedCourse();

    String getServerBaseUrl();

    String getUsername();
    
    String getPassword();
    
    boolean getShouldSavePassword();

    public List<Course> getAvailableCourses();

    void setAvailableCourses(List<Course> courses);
    
    void setProjectDir(String projectDir);

    void setSelectedCourse(Course course);

    void setServerBaseUrl(String baseUrl);

    void setUsername(String username);
    
    void setUsernameFieldName(String usernameFieldName);
    
    void setPassword(String password);
    
    void setShouldSavePassword(boolean shouldSavePassword);
}
