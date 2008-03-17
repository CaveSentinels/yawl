/*
 * This file is made available under the terms of the LGPL licence.
 * This licence can be retrieved from http://www.gnu.org/copyleft/lesser.html.
 * The source remains the property of the YAWL Foundation.  The YAWL Foundation is a
 * collaboration of individuals and organisations who are committed to improving
 * workflow technology.
 */

package org.yawlfoundation.yawl.resourcing.jsf;

import com.sun.rave.web.ui.appbase.AbstractApplicationBean;
import com.sun.rave.web.ui.component.Link;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.ResourceManager;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import java.util.*;

/**
 * Application scope data bean for the worklist and admin pages.
 *
 *  @author Michael Adams
 *  BPM Group, QUT Australia
 *  v0.1, 21/10/2007
 *
 *  Boilerplate code generated by Sun Java Studio Creator 2.1
 *
 *  Last Date: 05/01/2008
 */

public class ApplicationBean extends AbstractApplicationBean {

    // REQUIRED AND/OR IMPLEMENTED ABSTRACT PAGE BEAN METHODS //

    private int __placeholder;

    private void _init() throws Exception { }

    /** Constructor */
    public ApplicationBean() { }

    public void init() {
        super.init();

        // Initialize automatically managed components - do not modify
        try {
            _init();
        } catch (Exception e) {
            log("ApplicationBean Initialization Failure", e);
            throw e instanceof FacesException ? (FacesException) e: new FacesException(e);
        }

        // Add init code here that must complete *after* managed components are initialized
        _rm.registerJSFApplicationReference(this) ;
    }

    public void destroy() { }

    public String getLocaleCharacterEncoding() {
        return super.getLocaleCharacterEncoding();
    }


    /*******************************************************************************/

    // GLOBAL COMPONENTS //

    public enum PageRef { adminQueues, caseMgt, customServices, dynForm, Login,
                          participantData, selectUser, userWorkQueues, viewProfile }

    public enum TabRef { offered, allocated, started, suspended, unoffered, worklisted }

    public enum DynFormType { netlevel, tasklevel }

    // favIcon appears in the browser's address bar for all pages
    private Link favIcon = new Link() ;

    public Link getFavIcon() { return favIcon; }

    public void setFavIcon(Link link) { favIcon = link; }


    /*******************************************************************************/

    // MEMBERS AND METHODS USED BY ALL SESSIONS //

    private static final int PAGE_AUTO_REFRESH_RATE = 30 ;

    // reference to resource manager
    private ResourceManager _rm = ResourceManager.getInstance();

    public ResourceManager getResourceManager() { return _rm; }


    public int getDefaultJSFRefreshRate() {
        return PAGE_AUTO_REFRESH_RATE ;
    }


    // mapping of participant id to each session
    private Map<String, SessionBean> sessionReference = new HashMap<String, SessionBean>();

    public void addSessionReference(String participantID, SessionBean sBean) {
        sessionReference.put(participantID, sBean) ;
    }

    public SessionBean getSessionReference(String participantID) {
        return sessionReference.get(participantID) ;
    }

    public void removeSessionReference(String participantID) {
        sessionReference.remove(participantID) ;
    }

    public void refreshUserWorkQueues(String participantID) {
        SessionBean sessionBean = sessionReference.get(participantID) ;
        if (sessionBean != null) sessionBean.refreshUserWorkQueues();
    }


    // set of participants currently logged on
    private Set<String> liveUsers = new HashSet<String>();

    public Set<String> getLiveUsers() {
        return liveUsers;
    }

    public void setLiveUsers(Set<String> userSet) {
        liveUsers = userSet;
    }

    public void addLiveUser(String userid) {
        liveUsers.add(userid);
    }

    public void removeLiveUser(String userid) {
        if (isLoggedOn(userid)) liveUsers.remove(userid);
    }

    public boolean isLoggedOn(String userid) {
        return liveUsers.contains(userid);
    }


    /** @return true if the id passed is not a currently used userid */
    public boolean isUniqueUserID(String id) {
        return (getResourceManager().getParticipantFromUserID(id) == null) ;
    }


    public boolean isEmptyWorkItem(WorkItemRecord wir) {
        try {
            Map<String, FormParameter> params = getWorkItemParams(wir);
            return ((params == null) || (params.size() == 0)) ;
        }
        catch (Exception e) { return false; }
    }


    private Map<String, Map<String, FormParameter>> _workItemParams = new
            HashMap<String, Map<String, FormParameter>>();


    public Map<String, FormParameter> getWorkItemParams(WorkItemRecord wir) {
        Map<String, FormParameter> result = _workItemParams.get(wir.getID());
        if (result == null) {
            try {
                result = getResourceManager().getWorkItemParamsInfo(wir);
                if (result != null)
                    _workItemParams.put(wir.getID(), result);
            }
            catch (Exception e) { return null; }
        }
        return result;
    }

    public void removeWorkItemParams(WorkItemRecord wir) {
        _workItemParams.remove(wir.getID());
    }

    /**
     * formats a long time value into a string of the form 'ddd:hh:mm:ss'
     * @param age the time value (in milliseconds)
     * @return the formatted time string
     */
    public String formatAge(long age) {
        long secsPerHour = 60 * 60 ;
        long secsPerDay = 24 * secsPerHour ;
        age = age / 1000 ;                             // ignore the milliseconds

        long days = age / secsPerDay ;
        age %= secsPerDay ;
        long hours = age / secsPerHour ;
        age %= secsPerHour ;
        long mins = age / 60 ;
        age %= 60 ;                                    // seconds leftover
        return String.format("%d:%02d:%02d:%02d", days, hours, mins, age) ;
    }

    /** @deprecated */
    public Map<String, FormParameter> yParamListToFormParamMap(List params) {
        Map<String, FormParameter> result = new HashMap<String, FormParameter>();
        for (Object obj : params) {
            YParameter param = (YParameter) obj ;
            result.put(param.getName(), new FormParameter(param)) ;
        }
        if (result.isEmpty()) result = null ;
        return result ;
    }


    public String rPadSp(String str, int padlen) {
        int len = padlen - str.length();
        if (len < 1) return str ;

        StringBuilder result = new StringBuilder(str) ;
        for (int i = 0; i < len; i++) {
            result.append("&nbsp;");
        }
        return result.toString();
    }

    
    public String rPad (String str, int padlen) {
        int len = padlen - str.length();
        if (len < 1) return str ;

        StringBuilder padded = new StringBuilder(str);
        char[] spaces  = new char[len] ;
        for (int i = 0; i < len; i++) spaces[i] = ' ';
        padded.append(spaces) ;
        return padded.toString();
    }

    public void refresh() {
        FacesContext context = FacesContext.getCurrentInstance();
        Application application = context.getApplication();
        ViewHandler viewHandler = application.getViewHandler();
        UIViewRoot viewRoot = viewHandler.createView(context, context
             .getViewRoot().getViewId());
        context.setViewRoot(viewRoot);
      //  context.renderResponse(); //Optional
  }

}
