package org.yawlfoundation.yawl.controlpanel.preferences;

import java.util.prefs.Preferences;

/**
 * @author Michael Adams
 * @date 5/08/2014
 */
public class UserPreferences {

    private static final Preferences _prefs =
            Preferences.userRoot().node("/org/yawlfoundation/yawl/controlPanel");

    private static final String START_ENGINE_ON_STARTUP = "startEngineOnStartup";
    private static final String CHECK_FOR_UPDATES_ON_STARTUP = "checkForUpdatesOnStartup";
    private static final String STOP_ENGINE_ON_EXIT = "stopEngineOnExit";
    private static final String SHOW_LOGON_PAGE_ON_ENGINE_START = "showLogonPageOnEngineStart";
    private static final String POST_UPDATES_COMPLETED = "postUpdatesCompleted";


    public boolean startEngineOnStartup() {
        return getBoolean(START_ENGINE_ON_STARTUP);
    }

    public void setStartEngineOnStartup(boolean b) {
        setBoolean(START_ENGINE_ON_STARTUP, b);
    }


    public boolean checkForUpdatesOnStartup() {
        return getBoolean(CHECK_FOR_UPDATES_ON_STARTUP);
    }

    public void setCheckForUpdatesOnStartup(boolean b) {
        setBoolean(CHECK_FOR_UPDATES_ON_STARTUP, b);
    }


    public boolean stopEngineOnExit() {
        return getBoolean(STOP_ENGINE_ON_EXIT);
    }

    public void setStopEngineOnExit(boolean b) {
        setBoolean(STOP_ENGINE_ON_EXIT, b);
    }


    public boolean showLogonPageOnEngineStart() {
        return getBoolean(SHOW_LOGON_PAGE_ON_ENGINE_START);
    }

    public void setShowLogonPageOnEngineStart(boolean b) {
        setBoolean(SHOW_LOGON_PAGE_ON_ENGINE_START, b);
    }


    public void setPostUpdatesCompleted(boolean completed) {
        setBoolean(POST_UPDATES_COMPLETED, completed);
    }

    public boolean getPostUpdatesCompleted() {
        return getBoolean(POST_UPDATES_COMPLETED);
    }


    private boolean getBoolean(String key) {
        return _prefs.getBoolean(key, false);
    }

    private void setBoolean(String key, boolean value) {
        _prefs.putBoolean(key, value);
    }

}
