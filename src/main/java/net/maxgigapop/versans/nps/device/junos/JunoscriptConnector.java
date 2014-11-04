/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device.junos;

import net.maxgigapop.versans.nps.device.DeviceException;
import net.maxgigapop.versans.nps.device.NetworkDeviceInstance;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import java.nio.charset.Charset;

/**
 *
 * @author xyang
 */


public class JunoscriptConnector {
    private Logger log = Logger.getLogger(JunoscriptConnector.class);
    // private OSCARSNetLogger netLogger =  OSCARSNetLogger.getTlogger();

    private BufferedReader fromServer = null;
    private OutputStream toServer = null;
    private Session session = null;
    private Channel channel = null;
    
    
    private String privkeyfile  = null;
    private String passphrase   = null;
    private String login        = null;
    private String password      = null;
    private String sshport       = "22";
    private boolean commitsync   = false;
    private boolean isLogging = false;
    private boolean isStub = false;


    public JunoscriptConnector() { }
    
    public void setConfig(Map connectorConfig) throws DeviceException {
        if (connectorConfig == null) {
            throw new DeviceException("no config set!");
        } 
        
        this.privkeyfile    = (String) connectorConfig.get("privkeyfile");
        this.passphrase     = (String) connectorConfig.get("passphrase");
        this.login          = (String) connectorConfig.get("login");
        this.password       = (String) connectorConfig.get("password");
        if (this.login == null || this.login.isEmpty()) {
            throw new DeviceException("login null or empty");
        }
        if ((this.privkeyfile == null || this.privkeyfile.isEmpty())
              &&  (this.password == null || this.password.isEmpty())) {
            throw new DeviceException("both password and passphrase null or empty ");
        }
        if (connectorConfig.get("sshport") != null) {
            sshport = (String)connectorConfig.get("sshport");
        }
        if (connectorConfig.get("commitsync") != null) {
            commitsync = (Boolean)connectorConfig.get("commitsync");
        }
        if (connectorConfig.get("commandlog") != null) {
            isLogging = (Boolean)connectorConfig.get("commandlog");
        }
        if (connectorConfig.get("isstub") != null) {
            isStub = (Boolean)connectorConfig.get("isstub");
        }
    }
    
    /**
     *  @throws DeviceException
     */
    private void connect(String address) throws DeviceException {

        this.log.debug("connect - start");
        
        
        JSch jsch = new JSch();
        try {
            this.session = jsch.getSession(login, address, Integer.valueOf(sshport));
            SSHUserInfo userInfo;
            if (this.password != null && !this.password.isEmpty()) {
                userInfo = new SSHUserInfo(this.password);
                this.session.setPassword(userInfo.getPassword());
            } else {
                userInfo = new SSHUserInfo(this.privkeyfile, this.passphrase);
                if (this.passphrase == null || this.passphrase.isEmpty()) {
                    jsch.addIdentity(userInfo.getKeyFile());            
                } else {
                    jsch.addIdentity(userInfo.getKeyFile(), userInfo.getPassphrase().getBytes());
                }
            }
            this.session.setUserInfo(userInfo);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            this.session.setConfig(config);
            this.session.connect();
            this.channel = this.session.openChannel("exec");            
            this.toServer = channel.getOutputStream();
            ((ChannelExec) this.channel).setCommand("junoscript");
            this.fromServer = new BufferedReader(new InputStreamReader(channel.getInputStream()));            
            this.channel.connect();
        } catch (JSchException ex) {
            throw new DeviceException(ex.getMessage());
        } catch (IOException ex) {
            throw new DeviceException(ex.getMessage());
        }
        this.log.debug("connect - end");
    }
    
    /**
     * Shut down gracefully.
     */
    private void disconnect() throws DeviceException {
        this.log.debug("disconnect - start");
        try {
            if (this.channel != null) {
                this.channel.disconnect();
            }
            if (this.session != null) {
                this.session.disconnect();
            }
            if (this.fromServer != null) {
                this.fromServer.close();
            } 
            if (this.toServer != null) {
                this.toServer.close();
            }
        } catch (IOException ex) {
            throw new DeviceException(ex.getMessage());
        }
        this.log.debug("disconnect - end");
    }

    public String sendApplyCommand(NetworkDeviceInstance ndi) throws DeviceException {
        if (ndi.getDeviceRef() == null) {
            throw new DeviceException("sendApplyCommand to null device");
        }
        synchronized(ndi.getDeviceRef()) {
            // leave interfaceDeltas for now
            String address = ndi.getDeviceRef().getAddress();
            String deviceCommand = ndi.getDelta().getCmdToApply();
            return sendCommand(address, deviceCommand);
        } 
    }
    
    public String sendDeleteCommand(NetworkDeviceInstance ndi) throws DeviceException {
        if (ndi.getDeviceRef() == null) {
            throw new DeviceException("sendApplyCommand to null device");
        }
        synchronized(ndi.getDeviceRef()) {
            // leave interfaceDeltas for now
            String address = ndi.getDeviceRef().getAddress();
            String deviceCommand = ndi.getDelta().getCmdToDelete();
            return sendCommand(address, deviceCommand);
        } 
    }
    
    public String sendVerifyCommand(NetworkDeviceInstance ndi) throws DeviceException {
        if (ndi.getDeviceRef() == null) {
            throw new DeviceException("sendApplyCommand to null device");
        }
        synchronized(ndi.getDeviceRef()) {
            // leave interfaceDeltas for now
            String address = ndi.getDeviceRef().getAddress();
            String deviceCommand = ndi.getDelta().getCmdToVerify();
            return sendCommand(address, deviceCommand);
        } 
    }

    /**
     * Sends the XML command to the server.
     * @throws DeviceException
     */
    public String sendCommand(String address, String deviceCommand) throws DeviceException {
        log.debug("sendCommand start");
        // String event = "pssSendCommand";
        // netLogger.init(ModuleName.PSS, command.getTransactionId());
        // log.debug(netLogger - start(event));
        
        // prepare XML handlers
        SAXBuilder sb = new SAXBuilder();
        XMLOutputter outputter = new XMLOutputter();
        Format format = outputter.getFormat();
        format.setLineSeparator("\n");
        format.setEncoding("US-ASCII");
        outputter.setFormat(format);
        XMLOutputter prettyOut = new XMLOutputter(Format.getPrettyFormat());        
        
        if (commitsync) {
            deviceCommand = deviceCommand.replaceAll("<commit-configuration />", "<commit-configuration> <synchronize/> </commit-configuration>");
        }
        if (deviceCommand == null) {
            throw new DeviceException("null device command");
        } else if (address == null) {
            throw new DeviceException("null device address");
        }
        //log.debug("sendCommand deviceCommand "+deviceCommand);
        log.debug("sendCommand address "+address);
        
        String responseString = "";
        try {
            Document commandDoc = sb.build(new StringReader(deviceCommand));
            
            // log if necessary
            if (isLogging) {
                String logOutput = outputter.outputString(commandDoc);
                this.log.info("\nCOMMAND\n\n" + logOutput);
            }
            
            if (isStub) {
                log.debug("working in stub mode - command not sent to device");
                return "";
            }
            
            // connect to router over SSH
            this.log.debug("connecting to "+address);
            this.connect(address);
            this.log.debug("sending command...");
            // send command
            outputter.output(commandDoc, this.toServer);
            
            // grab response
            Document responseDoc = null;
            if (this.fromServer == null) {
                throw new DeviceException("Cannot get output stream from device");
            }
            /*
            String line;
            StringBuffer result = new StringBuffer();
            while ((line = this.fromServer.readLine()) != null) {
                result.append(line);
            }
            */
            responseDoc = sb.build(this.fromServer);
            if (responseDoc == null) {
                throw new DeviceException("Device "+address+" did not return a response");
            }
            
            // convert response to string
            ByteArrayOutputStream buff  = new ByteArrayOutputStream();
            prettyOut.output(responseDoc, buff);
            responseString = buff.toString();
            
            boolean errorInResponse = this.checkForErrors(responseDoc);
            if (errorInResponse) {
                this.log.error("\nRESPONSE ERROR:\n"+responseString);
                throw new DeviceException("Device "+address+" returned a configuration error: "+responseString);
            } else if (isLogging) {
                this.log.info("\nRESPONSE:\n\n"+responseString);
            }
            

            this.log.info("response received");
            this.log.info("sendCommand.end for "+address);

        } catch (JDOMException e) {
            log.error(e);
            throw new DeviceException(e);
        } catch (IOException e) {
            log.error(e);
            throw new DeviceException(e);
        } finally {
            // close SSH connection
            this.disconnect();           
        }

        return responseString;
    }

    @SuppressWarnings("unchecked")
    private boolean checkForErrors(Document responseDoc) {
        boolean hasErrors = false;
        Namespace xnmNs = Namespace.getNamespace("xnm", "http://xml.juniper.net/xnm/1.1/xnm");
        Element responseRoot = responseDoc.getRootElement();
        List<Element> rpcReplies = responseRoot.getChildren("rpc-reply", xnmNs);
        for (Element rpcReply : rpcReplies) {
            Element configRes = rpcReply.getChild("load-configuration-results", xnmNs);
            if (configRes != null) {
                List<Element> configErrors = configRes.getChildren("error", xnmNs);
                if (configErrors != null && !configErrors.isEmpty()) {
                    hasErrors = true;
                }
            }
            
            Element commitRes = rpcReply.getChild("commit-results", xnmNs);
            if (commitRes != null) {
                List<Element> routingEngines = commitRes.getChildren("routing-engine", xnmNs);
                if (routingEngines != null) {
                    for (Element re: routingEngines) {
                        List<Element> commitErrors = re.getChildren("error", xnmNs);
                        if (commitErrors != null && !commitErrors.isEmpty()) {
                            hasErrors = true;
                        }
                    }
                } else {
                    List<Element> commitErrors = commitRes.getChildren("error", xnmNs);
                    if (commitErrors != null && !commitErrors.isEmpty()) {
                        hasErrors = true;
                    }
                }
            }
        }
        return hasErrors;
        
    }
    
    public static class SSHUserInfo implements UserInfo {
     private final String password;
     private boolean secretDelivered = false;
     private final String keyFile;
     private final String passphrase;

     public SSHUserInfo(final String password) {

         super();
         this.password = password;
         this.keyFile = null;
         this.passphrase = null;
     }

     public SSHUserInfo(final String keyFileName, final String passphrase) {
         super();
         this.password = null;
         this.keyFile = keyFileName;
         this.passphrase = new String(passphrase.getBytes(), Charset.forName("UTF-8"));
     }

     @Override
     public String getPassphrase() {
         secretDelivered = true;
         return passphrase;
     }

     @Override
     public String getPassword() {
         secretDelivered = true;
         return password;
     }

     @Override
     public boolean promptPassword(final String message) {
         return !secretDelivered;
     }

     @Override
     public boolean promptPassphrase(final String message) {
         return !secretDelivered;
     }

     @Override
     public boolean promptYesNo(final String message) {
         return true;
     }
     
     @Override
     public void showMessage(final String message) {
         // noop
     }
     
     public String getKeyFile() {
         return keyFile;
     }
    }
}
