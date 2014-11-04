/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.manager;

import net.maxgigapop.versans.nps.device.Device;
import net.maxgigapop.versans.nps.device.NetworkDeviceInstance;
import net.maxgigapop.versans.nps.device.NetworkDeviceFactory;
import net.maxgigapop.versans.nps.device.Interface;
import net.maxgigapop.versans.nps.config.NPSGlobalConfig;
import net.maxgigapop.versans.nps.config.ConfigException;
import net.maxgigapop.versans.nps.config.NPSConfigYaml;
import net.maxgigapop.versans.nps.api.ServiceTerminationPoint;
import net.maxgigapop.versans.nps.api.ServiceException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.*;

/**
 *
 * @author xyang
 */ 
public class TopologyManager {
    
    private org.apache.log4j.Logger log;

    public TopologyManager() {
        log = org.apache.log4j.Logger.getLogger(this.getClass());
    }
    
    // parse a topology configure file (yaml or xml)
    // create device and interface in DB if not existent
    public void initNetworkTopology() throws ConfigException {
        NPSGlobalConfig config = NPSConfigYaml.getInstance().getNPSGlobalConfig();
        Map devices = config.getDevices();
        Iterator devIt = devices.keySet().iterator();
        while (devIt.hasNext()) {
            String dName = (String) devIt.next();
            Map deviceCfg = (Map) devices.get(dName);
            String urn = (String)deviceCfg.get("urn");
            String model = (String)deviceCfg.get("model");
            String address = (String)deviceCfg.get("address");
            String location = (String)deviceCfg.get("location");
            String description = (String)deviceCfg.get("description");
            Map connector = (Map)deviceCfg.get("connector");
            Map interfaces = (Map)deviceCfg.get("interfaces");
            if (urn == null) {
                throw new ConfigException("device '"+dName+"' has no urn configured");
            }
            Device device = NPSGlobalState.getDeviceStore().getByUrn(urn);
            boolean isNewDevice = false;
            if (device == null) {
                device = new Device();
                device.setUrn(urn);
                isNewDevice = true;
            } 
            if (model != null) {
                device.setModel(model);
            } else {
                throw new ConfigException("device '" + dName + "' has no model configured");
            }
            if (address != null) {
                device.setAddress(address);
            } else {
                throw new ConfigException("device '" + dName + "' has no address configured");
            }
            if (location != null) {
                device.setLocation(location);
            }
            if (description != null) {
                device.setDescription(description);
            }
            if (connector != null) {
                device.setConnectorConfig(connector);
            } else {
                throw new ConfigException("device '"+dName+"' has no connector configured");
            }
            if (isNewDevice) {
                NPSGlobalState.getDeviceStore().add(device);
            } else {
                NPSGlobalState.getDeviceStore().update(device);
            }
            if (interfaces == null) {
                log.warn("device '"+dName+"' has no interfaces configured");
                continue;
            }
            Iterator ifIt = interfaces.keySet().iterator();
            while (ifIt.hasNext()) {
                String iName = (String) ifIt.next();
                Map intfCfg = (Map) interfaces.get(iName);
                String ifUrn = (String)intfCfg.get("urn");
                if (ifUrn == null) {
                    throw new ConfigException("device '"+dName+"' / interface '"
                            +iName+"' has no urn configured");
                }
                Interface intf = NPSGlobalState.getInterfaceStore().getByUrn(ifUrn);
                boolean isNewIf = false;
                if (intf == null) {
                    intf = new Interface();
                    intf.setUrn(ifUrn);
                    isNewIf = true;
                } 
                String ifDescr = (String)intfCfg.get("description");
                if (ifDescr != null)
                    intf.setDescription(ifDescr);
                intf.setDeviceId(device.getId());
                intf.setModel("Generic");
                if (isNewIf) {
                    NPSGlobalState.getInterfaceStore().add(intf);
                } else {     
                    NPSGlobalState.getInterfaceStore().update(intf);
                } 
                device.getInterfaces().add(intf);
            }
        }
    }
    
    // compute full P2P path of STPs -- hack for now: single hop 
    public List<ServiceTerminationPoint> computeP2PPath(ServiceTerminationPoint srcSTP, 
            ServiceTerminationPoint dstSTP) throws ServiceException {
        List<ServiceTerminationPoint> path = new ArrayList<ServiceTerminationPoint>();
        path.add(srcSTP);
        path.add(dstSTP);
        return path;
    }

    
    // create explicit p2p or mp path by simply including all STPs 
    public List<ServiceTerminationPoint> createExplicitPath(List<ServiceTerminationPoint> customerSTPs)
            throws ServiceException {
        List<ServiceTerminationPoint> path = new ArrayList<ServiceTerminationPoint>();
        for (ServiceTerminationPoint stp: customerSTPs) {
            String ifUrn = stp.getId();
            if (NPSGlobalState.getInterfaceStore().getByUrn(ifUrn) == null) {
                throw new ServiceException("createExplicitPath - undefined urn "+ ifUrn);
            }
            path.add(stp);
        }
        return path;
    }


    // generate list of Device with associaed STPs 
    public List<NetworkDeviceInstance> createDeviceInstanceSequence(String contractId,
            List<ServiceTerminationPoint> path) throws ServiceException {
        List<NetworkDeviceInstance> devInsSequence = new ArrayList<NetworkDeviceInstance>();
        NetworkDeviceFactory devInsFactory = NetworkDeviceFactory.getFactory();
        for (ServiceTerminationPoint stp: path) {
            NetworkDeviceInstance devIns = devInsFactory.create(contractId, stp);
            boolean hasDevIns = false;
            for (NetworkDeviceInstance i : devInsSequence) {
                if (i == devIns) {
                    hasDevIns = true;
                    break;
                }
            }
            if (!hasDevIns) {
                devInsSequence.add(devIns);
            }
        }
        return devInsSequence;
    }
}
