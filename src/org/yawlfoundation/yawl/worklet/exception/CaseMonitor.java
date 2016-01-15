/*
 * Copyright (c) 2004-2012 The YAWL Foundation. All rights reserved.
 * The YAWL Foundation is a collaboration of individuals and
 * organisations who are committed to improving workflow technology.
 *
 * This file is part of YAWL. YAWL is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation.
 *
 * YAWL is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with YAWL. If not, see <http://www.gnu.org/licenses/>.
 */

package org.yawlfoundation.yawl.worklet.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.util.JDOMUtil;
import org.yawlfoundation.yawl.worklet.rdr.RuleType;
import org.yawlfoundation.yawl.worklet.support.WorkletConstants;
import org.yawlfoundation.yawl.worklet.support.Persister;
import org.yawlfoundation.yawl.worklet.support.RdrConversionTools;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


/** The CaseMonitor class manages a dataset of descriptors for each case started in the
 *  engine while the ExceptionService is active. The primary purpose is to minimise calls
 *  across the interface.
 *
 *  HandlerRunner instances for each exception raised by this case are maintained
 *  by this class.
 *
 *  @author Michael Adams
 *  @version 0.8, 04/07/2006
 */

public class CaseMonitor {

    private YSpecificationID _specID = null;            // specification id of instance
    private String _caseID = null ;                     // case id of instance
    private Element _caseData = null ;                  // current case data params
    private Element _netLevelData = null ;              // case-level decl. data params
    private Logger _log ;
    private Map<String, ExletRunner> _runners = null ;  // Runners for handling item exlets
    private ExletRunner _hrPreCase, _hrPostCase ;       // pre & post case runners
    private ExletRunner _hrCaseExternal ;               // runner for case-level external
    private List<String> _liveItems = new ArrayList<String>();  // list of executing items
    private boolean _liveCase = false ;                 // is case still executing?
    private boolean _preCaseCancelled = false ;         // has pre-case check killed case

    // persistence members
    private String _hrPreCaseID = null ;
    private String _hrPostCaseID = null ;
    private String _hrCaseExID = null ;
    private String _itemRunnerIDs = null;
    private String _liveItemIDs = null;
    private String _caseDataStr = null;
    private String _netDataStr = null;

    /**
     * The constructor is called when a pre-case constraint check event occurs,
     * marking the start of a new case
     * @param specID
     * @param caseID
     * @param data - a string representation of the case data
     */
    public CaseMonitor(YSpecificationID specID, String caseID, String data) {
        _log = LogManager.getLogger(this.getClass());
        _specID = specID ;
        _caseID = caseID ;
        _caseData = (data != null) ? JDOMUtil.stringToElement(data) : new Element(specID.getUri());
        _netLevelData = _caseData ;
        _runners = new Hashtable<String, ExletRunner>() ;
        _liveCase = true ;
        _caseDataStr = data;
        _netDataStr = data;
   }

    public CaseMonitor() {}                               // required for persistence

    //***************************************************************************//

    // GETTERS & SETTERs //

    public String getCaseID() {
        return _caseID ;
    }


    public YSpecificationID getSpecID() {
        return _specID ;
    }


    public Element getCaseData() {
        return _caseData ;
    }


    public Element getNetLevelData(){
        return _netLevelData ;
    }


    public void setCaseData(Element data) {
        _caseData = data;
        persistThis();
    }


    public void setCaseCompleted() {
        _liveCase = false ;
        persistThis();
    }


    //***************************************************************************//

    // PERSISTENCE METHODS //

    private String get_hrPreCaseID() { return _hrPreCaseID; }

    private String get_hrPostCaseID() { return _hrPostCaseID ; }

    private String get_hrCaseExID() { return _hrCaseExID ; }

    private String get_itemRunnerIDs() { return _itemRunnerIDs; }

    private String get_liveItemIDs() { return _liveItemIDs; }

    private YSpecificationID get_specID() { return _specID; }

    private String get_caseID() { return _caseID; }

    private String get_caseDataStr() { return _caseDataStr ; }

    private String get_netDataStr() { return _netDataStr ; }

    private boolean get_liveCase() { return _liveCase; }

    private void set_hrPreCaseID(String id) { _hrPreCaseID = id; }

    private void set_hrPostCaseID(String id) { _hrPostCaseID = id ; }

    private void set_hrCaseExID(String id) { _hrCaseExID = id ; }

    private void set_itemRunnerIDs(String ids) { _itemRunnerIDs = ids ; }

    private void set_liveItemIDs(String ids) { _liveItemIDs = ids ;  }

    private void set_specID(YSpecificationID s) { _specID = s; }

    private void set_caseID(String s) { _caseID = s; }

    private void set_caseDataStr(String s) { _caseDataStr = s; }

    private void set_netDataStr(String s) { _netDataStr = s ; }

    private void set_liveCase(boolean b) { _liveCase = b; }


    /**
     * reconstitutes objects persisted as string representations
     */
    public void initNonPersistedItems() {
        _caseData = JDOMUtil.stringToElement(_caseDataStr);
        _netLevelData = JDOMUtil.stringToElement(_netDataStr);
        _liveItems = RdrConversionTools.StringToStringList(_liveItemIDs);
    }


    /**
     * 'reattaches' HandlerRunners belonging to this CaseMonitor after a restore
     * @param runnerMap - a set of all the HandlerRunners restored from persistence
     * @return the list of all runners 'claimed' by this CaseMonitor
     */
    public List<ExletRunner> restoreRunners(Map<String, ExletRunner> runnerMap) {
        List<ExletRunner> restored = new ArrayList<ExletRunner>() ;
        _runners = new Hashtable<String, ExletRunner>();  // workitem level runners

        // restore any case level runners
        _hrPreCase = restoreRunner(_hrPreCaseID, runnerMap) ;
        _hrPostCase = restoreRunner(_hrPostCaseID, runnerMap) ;
        _hrCaseExternal = restoreRunner(_hrCaseExID, runnerMap) ;
        if (_hrPreCase != null) restored.add(_hrPreCase);
        if (_hrPostCase != null) restored.add(_hrPostCase);
        if (_hrCaseExternal != null) restored.add(_hrCaseExternal);

        // restore any item level runners
        // runner ids are a string of ids persisted for this CaseMonitor
        List<String> runnerIDs = RdrConversionTools.StringToStringList(_itemRunnerIDs);
        if (runnerIDs != null) {
            for (String id : runnerIDs) {
                ExletRunner runner = restoreRunner(id, runnerMap) ;
                restored.add(runner);
                _runners.put(runner.getParentWorkItemID(), runner);
            }
        }
        return restored ;
    }


    /**
     * Seek and retrieve the HandlerRunner specified by the id passed
     * @param id - the persisted id of a HandlerRunner
     * @param runnerMap - the set of restored runners
     * @return the runner that 'owns' the id specified, or null if there is no
     *         runner with that id or the id is null
     */
    private ExletRunner restoreRunner(String id, Map<String, ExletRunner>  runnerMap) {
        ExletRunner result = null ;
        if (id != null) {                                // an actual id has been passed
            result = runnerMap.get(id);
            if (result != null) {

                // found a runner with this id, so 'reattach' it to this CaseMonitor
                result.setOwnerCaseMonitor(this);
            }
          }
       return result ;
    }


    /** updates the persisted object after changes (if persisting) */
    private void persistThis() {
        Persister.update(this);
    }

    //***************************************************************************//


    // DATA MAINTENANCE //

    // updates the running case data with the workitem input / output params
     public void updateData(String sData) {
        Element eData = JDOMUtil.stringToElement(sData);

        // for each child of the passed Data, add/update the caseData
        for (Element wiParam : eData.getChildren()) {
            Element caseParam = _caseData.getChild(wiParam.getName());

            // if case data contains item, update its value
            if (caseParam != null) caseParam.setText(wiParam.getText());

            // else create a new child, and add it to caseData
            else {
                Element newParam = new Element(wiParam.getName()) ;
                newParam.addContent(wiParam.getText());
                _caseData.addContent(newParam);
            }
        }
        updateCaseDataStr();
    }


    /**
     *  Adds an external exception trigger to the case data so that the correct
     *  RDR can be found for it
     * @param triggerValue - the string value of the external trigger
     */
    public void addTrigger(String triggerValue) {

        // all external triggers must be delimited with "'s
        if (! triggerValue.startsWith("\""))
            triggerValue = "\"" + triggerValue + "\"" ;

        Element eTrigger = new Element("trigger");        // new Element for trigger
        eTrigger.addContent(triggerValue);                // add the text
        _caseData.addContent(eTrigger);                   // add element to case data
        updateCaseDataStr();
    }


    public String getTrigger() { return _caseData.getChildText("trigger"); }

    public void removeTrigger() {
        _caseData.removeChild("trigger");
        updateCaseDataStr();
    }


    /**
     * Adds the contents of the workitem record to the case data for this case - thus
     * providing information about the workitem to the ruleset
     * @param wir - the wir being tested for an exception
     */
    public void addProcessInfo(WorkItemRecord wir) {

        //convert the wir contents to an Element
        Element eWir = JDOMUtil.stringToElement(wir.toXML()).detach();

        Element eInfo = new Element("process_info");     // new Element for info
        eInfo.addContent(eWir);                          // add the wir
        _caseData.addContent(eInfo);                     // add element to case data
        updateCaseDataStr();
        persistThis();
    }


    public void removeProcessInfo() {
        _caseData.removeChild("process_info");
        updateCaseDataStr();
    }


    /** Stringifies the case data for persistence purposes */
    private void updateCaseDataStr() {
        _caseDataStr = JDOMUtil.elementToString(_caseData);
        persistThis();
    }


    //***************************************************************************//

    // HANDLER RUNNER MAINTENANCE //

    /** A CaseMonitor instance may have several HandlerRunner's active at any one
     *  time, each one representing an exception currently being handled by the
     *  ExceptionService. */

    // add a HandlerRunner for a pre-case constraint violation
    public void addPreCaseHandlerRunner(ExletRunner hr) {
        if (_hrPreCase == null) {
            _hrPreCase = hr ;
            _hrPreCaseID = String.valueOf(hr.getID());
            persistThis();
        }
        else
           _log.error("Cannot add a pre-case exception manager when one already exists");
    }


    // add a HandlerRunner for a post-case constraint violation
    public void addPostCaseHandlerRunner(ExletRunner hr) {
        if (_hrPostCase == null) {
            _hrPostCase = hr ;
            _hrPostCaseID = String.valueOf(hr.getID());
            persistThis();
        }
        else
           _log.error("Cannot add a post-case exception manager when one already exists");
    }


    // add a HandlerRunner for a case-level external trigger
    public void addCaseExternalHandlerRunner(ExletRunner hr) {
        if (_hrCaseExternal == null) {
            _hrCaseExternal = hr ;
            _hrCaseExID = String.valueOf(hr.getID());
            persistThis();
        }
        else
           _log.error("Cannot add a case-level external exception manager when one already exists");
    }


    // add a HandlerRunner for a workitem. Note that HandlerRunners for the case level
    // may also be invoked from this method by sending the appropriate tag as the itemid.
    public void addHandlerRunner(ExletRunner hr, String itemID ) {
        if (itemID.equals("pre"))
            addPreCaseHandlerRunner(hr);
        else if (itemID.equals("post"))
            addPostCaseHandlerRunner(hr);
        else if (itemID.equals("external"))
            addCaseExternalHandlerRunner(hr);
        else  {
            if (!_runners.containsKey(itemID)) {
                _runners.put(itemID, hr);
                updateRunnerIDs();
                persistThis();
            }
            else
                _log.error("Exception Manager for itemID already exists: {}", itemID);
        }
    }

    /** Stringifies the list of item runner ids (required for persistence) */
    private void updateRunnerIDs() {
        List<String> ids = new ArrayList<String>();
        for (ExletRunner runner : _runners.values()) {
            ids.add(String.valueOf(runner.getID()));
        }
        _itemRunnerIDs = RdrConversionTools.StringListToString(ids);
    }

    //***************************************************************************//

    public ExletRunner getPreCaseHandlerRunner() {
        return _hrPreCase;
    }


    public ExletRunner getPostCaseHandlerRunner() {
        return _hrPostCase;
    }

    public ExletRunner getCaseExternalHandlerRunner() {
        return _hrCaseExternal;
    }

    /** retrieves an item-level runner */
    public ExletRunner getHandlerRunnerForItem(String itemID) {
        return _runners.get(itemID) ;
    }


    /** returns the runner for the specified type (if any) */
    public ExletRunner getRunnerForType(RuleType xType, String itemID) {
        ExletRunner result ;
        switch (xType) {
            case CasePreconstraint :
                result = getPreCaseHandlerRunner(); break ;
            case CasePostconstraint:
                result = getPostCaseHandlerRunner(); break ;
            case CaseExternalTrigger :
                result = getCaseExternalHandlerRunner(); break ;
            default :
                result = getHandlerRunnerForItem(itemID);
        }
        return result ;
    }


    // returns the complete list of all active HandlerRunners for this case
    public List<ExletRunner> getHandlerRunners() {
        List<ExletRunner> list = new ArrayList<ExletRunner>();
        list.addAll(_runners.values());
        if (_hrPreCase != null) list.add(_hrPreCase) ;
        if (_hrPostCase != null) list.add(_hrPreCase) ;
        if (_hrCaseExternal != null) list.add(_hrCaseExternal) ;
        return list;
    }

    //***************************************************************************//

    public void removePreCaseHandlerRunner() {
        _hrPreCase = null ;
        _hrPreCaseID = null ;
        persistThis();
     }


    public void removePostCaseHandlerRunner() {
        _hrPostCase = null ;
        _hrPostCaseID = null ;
        persistThis();
    }


    public void removeCaseExternalHandlerRunner() {
        _hrCaseExternal = null ;
        _hrCaseExID = null ;
        persistThis();
    }


    // remove the HandlerRunner for the specified workitem
    public void removeHandlerRunnerForItem(String itemID) {
        if (itemID.equals("pre"))
            removePreCaseHandlerRunner();
        else if (itemID.equals("post"))
            removePostCaseHandlerRunner();
        else if (itemID.equals("external"))
            removeCaseExternalHandlerRunner();
        else {
            if (_runners.containsKey(itemID)) {
                _runners.remove(itemID);
                _itemRunnerIDs = RdrConversionTools.MapKeySetToString(_runners);
                persistThis();
            }
            else
                _log.error("Exception Manager for itemID does not exist: {}", itemID);
        }
    }


    // remove specified HandlerRunner
    public void removeHandlerRunner(ExletRunner runner) {
        if (runner == _hrPreCase)
            removePreCaseHandlerRunner();
        else if (runner == _hrPostCase)
            removePostCaseHandlerRunner();
        else if (runner == _hrCaseExternal)
            removeCaseExternalHandlerRunner();
        else {
            if (_runners.containsKey(runner.getParentWorkItemID())) {
                _runners.remove(runner.getParentWorkItemID());
                _itemRunnerIDs = RdrConversionTools.MapKeySetToString(_runners);
                persistThis();
            }
        }
    }


    // remove all HandlerRunners for this case (called when parent case is cancelled)
    public void removeAllRunners() {
        removePreCaseHandlerRunner();
        removePostCaseHandlerRunner();
        removeCaseExternalHandlerRunner();
        _itemRunnerIDs = null;
        _runners.clear();
        persistThis();
    }


    //***************************************************************************//

    // LIVE ITEM METHODS //

    // A live item is added when a pre-task constraint event is received by the
    // ExceptionService, and removed when a post-task event is received for the item.
    // They are basically flags to prevent the CaseMonitor instance being destroyed
    // before all events have been received and processed (they may come out of order
    // - i.e. a post-task constraint event may be processed after a post-case constraint
    //  event).

    public void addLiveItem(String taskID) {
        _liveItems.add(taskID);
        _liveItemIDs = RdrConversionTools.StringListToString(_liveItems);
        persistThis();
    }

    public  void removeLiveItem(String taskID) {
        if (_liveItems.contains(taskID)) {
            _liveItems.remove(taskID);
            _liveItemIDs = RdrConversionTools.StringListToString(_liveItems);
            persistThis();
        }
    }


    //***************************************************************************//

    // BOOLEAN METHODS //

    public boolean hasHandlerRunnerForItem(String itemID) {
        return (_runners.containsKey(itemID));
    }


    public boolean isItemRunner(ExletRunner runner) {
        return _runners.containsValue(runner);
    }


    public boolean isCaseRunner(ExletRunner runner) {
        return (runner == _hrPreCase) || (runner == _hrPostCase) ||
               (runner == _hrCaseExternal) ;
    }


    public boolean hasLiveHandlerRunners() {
        return ! ((_hrPreCase == null) && (_hrPostCase == null) &&
                  (_hrCaseExternal == null) && _runners.isEmpty());
    }


    public boolean hasLiveItems() {
        return ! _liveItems.isEmpty();
    }

    // returns true if the case has completed, all workitem checks have completed
    // and there are no exception handlers running for this case
    public boolean isDone() {
        return ! (_liveCase || hasLiveItems() || hasLiveHandlerRunners()) ;
    }


    public boolean isCaseCompleted() {
        return ! _liveCase ;
    }


    public void setPreCaseCancellationFlag() {
        _preCaseCancelled = true ;
    }

    public boolean isPreCaseCancelled() {
        return _preCaseCancelled ;
    }

    /********************************************************************************/

    public String dump() {
        StringBuilder s = new StringBuilder("##### CASEMONITOR RECORD #####");
        s.append(WorkletConstants.newline);

        String specID = (_specID == null)? "null" : _specID.toString();
        String caseID = (_caseID == null)? "null" : _caseID;
        String caseData = (_caseData == null)? "null" : JDOMUtil.elementToString(_caseData);
        String netData = (_netLevelData == null)? "null" : JDOMUtil.elementToString(_netLevelData);              // case-level decl. data params
        String preHR = (_hrPreCase == null)? "null" :  _hrPreCase.toString();
        String postHR = (_hrPostCase == null)? "null" :  _hrPostCase.toString();
        String extHR = (_hrCaseExternal == null)? "null" :  _hrCaseExternal.toString();
        String liveCase = String.valueOf(_liveCase);

        WorkletConstants.appendLine(s, "SPECIFICATION ID", specID);
        WorkletConstants.appendLine(s, "CASE ID", caseID);
        WorkletConstants.appendLine(s, "CASE DATA", caseData);
        WorkletConstants.appendLine(s, "NET LEVEL DATA", netData);
        WorkletConstants.appendLine(s, "PRE-CASE RUNNER", preHR);
        WorkletConstants.appendLine(s, "POST-CASE RUNNER", postHR);
        WorkletConstants.appendLine(s, "EXTERNAL RUNNER", extHR);
        WorkletConstants.appendLine(s, "ITEM RUNNERS", _itemRunnerIDs);
        WorkletConstants.appendLine(s, "LIVE ITEMS", _liveItemIDs);
        WorkletConstants.appendLine(s, "LIVE CASE?", liveCase);

        return s.toString();

    }

} // end CaseMonitor class
