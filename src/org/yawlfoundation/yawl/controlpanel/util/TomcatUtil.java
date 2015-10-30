package org.yawlfoundation.yawl.controlpanel.util;

import org.yawlfoundation.yawl.util.StringUtil;
import org.yawlfoundation.yawl.util.XNode;
import org.yawlfoundation.yawl.util.XNodeParser;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Michael Adams
 * @date 4/08/2014
 */
public class TomcatUtil {

    private static int SERVER_PORT = -1;
    private static URL ENGINE_URL;
    private static final String TOMCAT_VERSION = "7.0.55";
    private static final String CATALINA_HOME = deriveCatalinaHome();

    private static final TomcatProcess _process = new TomcatProcess(CATALINA_HOME);

    public static boolean start() throws IOException {
        if (! isEngineRunning()) {                       // yawl isn't running
            if (isPortActive()) {                        // but localhost:port is responsive
                throw new IOException("Tomcat port is already in use by another service.\n" +
                     "Please check/change the port in Preferences and try again.");
            }
            checkSizeOfLog();
            removePidFile();                            // if not already removed
            _process.start();
            return true;
        }
        return false;                                   // already started
    }


    public static boolean stop() { return stop(null); }


    public static boolean stop(PropertyChangeListener listener) {
        try {
            return !isPortActive() || _process.stop(listener);
        }
        catch (IOException ioe) {
            return false;
        }
    }


    public static boolean isPortActive() {
        return isPortActive("localhost", getTomcatServerPort());
    }


    public static boolean isPortActive(int port) {
        return isPortActive("localhost", port);
    }


    public static boolean isTomcatRunning() {
        return _process.isAlive();
    }



    public static String getCatalinaHome() {
        return CATALINA_HOME;
    }


    public static int getTomcatServerPort() {
        if (SERVER_PORT < 0) SERVER_PORT = loadTomcatServerPort();
        return SERVER_PORT;
    }


    public static boolean setTomcatServerPort(int port) {
        XNode root = loadTomcatConfigFile("server.xml");
        if (root != null) {
            XNode service = root.getChild("Service");
            if (service != null) {
                XNode connector = service.getChild("Connector");
                if (connector != null) {
                    connector.addAttribute("port", port);
                    if (writeTomcatConfigFile("server.xml", root.toPrettyString(true))) {
                        updateServiceConfigs(getTomcatServerPort(), port);
                        SERVER_PORT = port;
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public static boolean isEngineRunning() {
        if (ENGINE_URL == null) {
            try {
                ENGINE_URL = new URL("http", "localhost", getTomcatServerPort(),
                        "/yawl/ib");
            }
            catch (MalformedURLException mue) {
                return false;
            }
        }
        return isResponsive(ENGINE_URL);
    }


    public static boolean isResponsive(URL url) {
        try {
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("HEAD");
            httpConnection.setConnectTimeout(1000);
            httpConnection.setReadTimeout(1000);
            return httpConnection.getResponseCode() == 200;
        }
        catch (IOException ioe) {
            return false;
        }
    }


    public static boolean killTomcatProcess() throws IOException {
        _process.kill();
        try {
            Thread.sleep(2000);
        }
        catch (InterruptedException ignore) {}
        return ! isPortActive();
    }


    /*************************************************************************/

    private static boolean isPortActive(String host, int port) {
        try {
            return simplePing(host, port);
        }
        catch (IOException ioe) {
            return false;
        }
    }


    private static boolean simplePing(String host, int port) throws IOException {
        if ((host == null) || (port < 0)) {
            throw new IOException("Error: bad parameters");
        }
        InetAddress address = InetAddress.getByName(host);
        Socket socket = new Socket(address, port);
        socket.close();
        return true;
    }


    private static int loadTomcatServerPort() {
        XNode root = loadTomcatConfigFile("server.xml");
        if (root != null) {
            XNode service = root.getChild("Service");
            if (service != null) {
                XNode connector = service.getChild("Connector");
                if (connector != null) {
                    return StringUtil.strToInt(connector.getAttributeValue("port"), -1);
                }
            }
        }
        return -1;       // default
    }


    private static XNode loadTomcatConfigFile(String filename) {
        File configFile = getTomcatConfigFile(filename);
        return (configFile.exists()) ?
                new XNodeParser().parse(StringUtil.fileToString(configFile)) : null;
    }


    private static boolean writeTomcatConfigFile(String filename, String content) {
        File configFile = getTomcatConfigFile(filename);
        if (configFile.exists()) {
            configFile = StringUtil.stringToFile(configFile, content);
        }
        return configFile != null;
    }


    private static File getTomcatConfigFile(String filename) {
        if (!filename.startsWith("conf")) {
            filename = "conf" + File.separator + filename;
        }
        File configFile = new File(filename);
        if (!configFile.isAbsolute()) {
            configFile = new File(getCatalinaHome(), filename);
        }
        return configFile;
    }


    // rename catalina.out if its too big - tomcat will create a new one on startup
    private static void checkSizeOfLog() {
        File log = new File(FileUtil.buildPath(getCatalinaHome(), "logs", "catalina.out"));
        if (log.exists() && log.length() > (1024 * 1024 * 5)) {              // 5mb
            String suffix = "." + new SimpleDateFormat("yyyyMMdd").format(new Date());
            log.renameTo(new File(log.getAbsolutePath() + suffix));
        }
    }


    public static void removePidFile() {
        File pidTxt = new File(getCatalinaHome(), "catalina_pid.txt");
        if (pidTxt.exists()) pidTxt.delete();
    }


    private static String deriveCatalinaHome() {
        try {
            File thisJar = FileUtil.getJarFile();
            if (thisJar.getAbsolutePath().endsWith(".jar")) {
                return FileUtil.buildPath(thisJar.getParentFile().getParent(),
                        "engine", "apache-tomcat-" + TOMCAT_VERSION);
            }
        }
        catch (URISyntaxException use) {
            //
        }
        return System.getenv("CATALINA_HOME");         // fallback
    }


    private static boolean updateServiceConfigs(int oldPort, int newPort) {
        if (oldPort == newPort) return true;
        String oldChars = ":" + oldPort;
        String newChars = ":" + newPort;
        File appsBase = new File(getCatalinaHome(), "webapps");
        for (File appDir : FileUtil.getDirList(appsBase)) {
            File webxml = new File(appDir, "WEB-INF" + File.separator + "web.xml");
            if (webxml.exists()) {
                StringUtil.replaceInFile(webxml, oldChars, newChars);
            }
        }
        return true;
    }

}
