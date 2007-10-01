/*
 * This file is made available under the terms of the LGPL licence.
 * This licence can be retreived from http://www.gnu.org/copyleft/lesser.html.
 * The source remains the property of the YAWL Foundation.  The YAWL Foundation is a collaboration of
 * individuals and organisations who are commited to improving workflow technology.
 *
 */


package au.edu.qut.yawl.engine;

import au.edu.qut.yawl.elements.YSpecification;
import au.edu.qut.yawl.elements.state.YIdentifier;
import au.edu.qut.yawl.unmarshal.YMarshal;
import au.edu.qut.yawl.exceptions.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.jdom.JDOMException;

/**
 /**
 * 
 * @author Lachlan Aldred
 * Date: 26/03/2004
 * Time: 15:54:07
 * 
 */
public class TestMarlonsEagerNessExperiment {
    private static YIdentifier _idForTopNet;
    private static YWorkItemRepository _worItemRepository;

    public static void main(String[] args) throws YSchemaBuildingException, YSyntaxException, JDOMException, IOException, YStateException, YPersistenceException, YDataStateException {
        YEngine _engine = YEngine.getInstance();
        EngineClearer.clear(_engine);
        URL fileURL = TestMarlonsEagerNessExperiment.class.getResource("MarlonsEagerExperiment.xml");
        File yawlXMLFile = new File(fileURL.getFile());
        YSpecification specification = null;
        specification = (YSpecification) YMarshal.
                        unmarshalSpecifications(yawlXMLFile.getAbsolutePath()).get(0);
        YEngine engine2 = YEngine.getInstance();
        engine2.loadSpecification(specification);
        _idForTopNet = engine2.startCase(null, null, specification.getID(), null, null);

        _worItemRepository = YWorkItemRepository.getInstance();
    }
}
