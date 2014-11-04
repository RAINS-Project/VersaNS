/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.manager;

import java.util.List;
import net.maxgigapop.versans.nps.api.ServicePolicy;
import net.maxgigapop.versans.nps.device.NetworkDeviceInstance;

/**
 *
 * @author xyang
 */
public class PolicyManager {

    public PolicyManager() {}

    // generate and map policies into the list of Devices and associaed STPs
    public List<ServicePolicy> refineDeviceInstancePolicies(String contractId,
            List<ServicePolicy> policyList,
            List<NetworkDeviceInstance> devInsSequence) {
        // TODO: refine policyList into local policies
        for (NetworkDeviceInstance ndi: devInsSequence) {
            ndi.getLocalPolicies().addAll(policyList);
        }
        return policyList;
    }
}
