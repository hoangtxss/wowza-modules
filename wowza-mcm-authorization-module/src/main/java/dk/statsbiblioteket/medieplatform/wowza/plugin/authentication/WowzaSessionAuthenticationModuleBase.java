package dk.statsbiblioteket.medieplatform.wowza.plugin.authentication;

import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.client.IClient;
import com.wowza.wms.module.IModuleOnApp;
import com.wowza.wms.module.IModuleOnCall;
import com.wowza.wms.module.IModuleOnConnect;
import com.wowza.wms.module.IModuleOnStream;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.request.RequestFunction;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamActionNotify;
import com.wowza.wms.stream.IMediaStreamActionNotify2;

import dk.statsbiblioteket.medieplatform.wowza.plugin.authentication.model.MCMSessionAndFilenameValidater;
import dk.statsbiblioteket.medieplatform.wowza.plugin.utilities.ConfigReader;

import java.io.File;
import java.io.IOException;


public class WowzaSessionAuthenticationModuleBase extends ModuleBase 
             implements IModuleOnApp, IModuleOnConnect, IModuleOnStream, IModuleOnCall {

    private static final String PLUGIN_NAME = "CHAOS Wowza plugin - Authentication";
    private static final String PLUGIN_VERSION = "${project.version}";

    public static final String PROPERTY_MCM_SERVER_URL_KEY = "GeneralMCMServerURL";
    public static final String PROPERTY_MCM_VALIDATION_METHOD = "ValidationMCMValidationMethod";

    private IMediaStreamActionNotify2 streamAuthenticater;

    public WowzaSessionAuthenticationModuleBase() {
        super();
    }

    public void onAppStart(IApplicationInstance appInstance) {
        String appName = appInstance.getApplication().getName();
        String vhostDir = appInstance.getVHost().getHomePath();
        String storageDir = appInstance.getStreamStorageDir();
        getLogger()
                .info("***Entered onAppStart: " + appName + "\n  Plugin: " + PLUGIN_NAME + " version " + PLUGIN_VERSION
                              + "\n  VHost home path: " + vhostDir + " VHost storage dir: " + storageDir);
        try {
            //Initialise the config reader
            ConfigReader cr;
            cr = new ConfigReader(new File(vhostDir + "/conf/" + appName + "/wowza-modules.properties"),
                                  PROPERTY_MCM_SERVER_URL_KEY, PROPERTY_MCM_VALIDATION_METHOD);

            // Read configuration
            String connectionURLString = cr.get(PROPERTY_MCM_SERVER_URL_KEY);
            String validationMethodAtServer = cr.get(PROPERTY_MCM_VALIDATION_METHOD);

            // Initialise stream authenticater
            streamAuthenticater = new StreamAuthenticater(getLogger(),
                                                          new MCMSessionAndFilenameValidater(getLogger(),
                                                                                             connectionURLString,
                                                                                             validationMethodAtServer));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize module.", e);
        }
    }

    public void onConnect(IClient client, RequestFunction function,
            AMFDataList params) {
        getLogger().info("onConnect (client ID)   : " + client.getClientId());
        client.acceptConnection("CHAOS connection accepted.");
    }

    public void onConnectAccept(IClient client) {
        getLogger().info("onConnectAccept: " + client.getClientId());
    }

    public void onConnectReject(IClient client) {
        getLogger().info("onConnectReject: " + client.getClientId());
    }

    /**
     * Hook into events related to playing the stream. This is done by
     * implementing the IMediaStreamActionNotify2 interface with the
     * StreamAuthenticater class.
     */
    @SuppressWarnings("unchecked")
    public void onStreamCreate(IMediaStream stream) {
        getLogger().info("onStreamCreate by: " + stream.getClientId());
        IMediaStreamActionNotify streamActionNotify  = streamAuthenticater;
        WMSProperties props = stream.getProperties();
        synchronized(props) {
            props.put("streamActionNotifier", streamActionNotify);
        }
        stream.addClientListener(streamActionNotify);
    }

    public void play(IClient client, RequestFunction function, AMFDataList params) {
        getLogger().info("Play called for client id was "+client.getClientId());
        this.invokePrevious(client, function, params);
    }

    /**
     * Unregister event hook for the stream.
     */
    public void onStreamDestroy(IMediaStream stream) {
        getLogger().info("onStreamDestroy by: " + stream.getClientId());
        IMediaStreamActionNotify actionNotify = null;
        WMSProperties props = stream.getProperties();
        synchronized(props) {
            actionNotify = (IMediaStreamActionNotify)stream.getProperties().get("streamActionNotifier");
        }
        if (actionNotify != null) {
            stream.removeClientListener(actionNotify);
            getLogger().info("removeClientListener: "+stream.getSrc());
        }
    }

    @Override
    public void onDisconnect(IClient client) {
        getLogger().info("onDisconnect (client ID)   : " + client.getClientId());
    }

    public void onAppStop(IApplicationInstance appInstance) {
        getLogger().info("onAppStop: " + PLUGIN_NAME + " version " + PLUGIN_VERSION);
    }

    @Override
    public void onCall(String handlerName, IClient client, RequestFunction function, AMFDataList params) {
        getLogger().info("onCall, unimplemented method was called: " + handlerName);
    }
}
