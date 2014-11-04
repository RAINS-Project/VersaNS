/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device.junos;

import net.maxgigapop.versans.nps.device.DeviceDelta;
import net.maxgigapop.versans.nps.device.Device;
import net.maxgigapop.versans.nps.device.DeviceException;
import net.maxgigapop.versans.nps.api.ServicePolicy;
import net.maxgigapop.versans.nps.api.ServiceTerminationPoint;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import net.maxgigapop.versans.nps.manager.NPSGlobalState;
import net.maxgigapop.versans.nps.manager.NPSUtils;

/**
 *
 * @author xyang
 */
public class JunoscriptGenerator {
    static public DeviceDelta generateDelta(String contractId, 
            List<ServiceTerminationPoint> stpList, List<ServicePolicy> policyList) 
        throws DeviceException {
        DeviceDelta deviceDelta = new DeviceDelta();
        //!! leave interfaceDeltas for now
        //!! all commands aggregate to deviceDelta level
        if (stpList.size() < 2) {
            throw new DeviceException("generateDelta require at least two STPs");
        }
        // assuming P2P for now and template is so formed too.
        ServiceTerminationPoint providerSTP = stpList.get(0);
        ServiceTerminationPoint customerSTP = stpList.get(1);
        // get logical system config
        Device deviceRef = NPSGlobalState.getDeviceStore().getByUrn(NPSUtils.extractDeviceUrn(providerSTP.getId()));
        Map connectorConfig = deviceRef.getConnectorConfig();
        if (connectorConfig == null) {
            throw new DeviceException("Device has no connectorConfig!");
        } 
        String logicalsystem = "";
        Boolean bgp_config_customer = false;
        String bgp_reject_policy = "REJECT";
        if (connectorConfig.get("logicalsystem") != null) {
            logicalsystem = (String)connectorConfig.get("logicalsystem");
        }
        if (connectorConfig.get("bgp_config_customer") != null) {
            bgp_config_customer = (Boolean)connectorConfig.get("bgp_config_customer");
        }
        if (connectorConfig.get("bgp_reject_policy") != null) {
            bgp_reject_policy = (String)connectorConfig.get("bgp_reject_policy");
        }
                
        // comand input
        Map mapInput = new HashMap();
        Map ifce_p = new HashMap();
        Map ifce_c = new HashMap();
        Map bgp_p = new HashMap();
        Map bgp_c = new HashMap();
        Map policies_p = new HashMap();
        Map policies_c = new HashMap();
        Map filters_p = new HashMap();
        Map filters_c = new HashMap();
        Map routing_instance = new HashMap();
        Map policer = new HashMap();
        filters_p.put("input", new HashMap());
        filters_p.put("output", new HashMap());
        filters_c.put("input", new HashMap());
        //filters_c.put("output", new HashMap());
        mapInput.put("logical_system", logicalsystem);
        mapInput.put("ifce_p", ifce_p);
        mapInput.put("ifce_c", ifce_c);
        mapInput.put("bgp_p", bgp_p);
        mapInput.put("bgp_c", bgp_c);
        mapInput.put("policies_p", policies_p);
        mapInput.put("policies_c", policies_c);
        mapInput.put("filters_p", filters_p);
        mapInput.put("filters_c", filters_c);
        mapInput.put("routing_instance", routing_instance);
        mapInput.put("policer", policer);
        
        //configure provider L2 interface
        String provider_port = NPSUtils.getDcnUrnField(providerSTP.getId(), "port");
        String provider_port_vlan = providerSTP.getLayer2Info().getOuterVlanTag().getValue();
        String provider_port_descr = "AWS provider VLAN for contract " + contractId;
        String provider_port_local_address = providerSTP.getLayer3Info().getBgpInfo().getLinkLocalIpAndMask();
        ifce_p.put("name", provider_port);
        ifce_p.put("vlan", provider_port_vlan);
        ifce_p.put("description", provider_port_descr);
        ifce_p.put("mtu", "9000");
        ifce_p.put("address", provider_port_local_address);

        //configure customer sub-interface
        String customer_port = NPSUtils.getDcnUrnField(customerSTP.getId(), "port");
        String customer_port_vlan = customerSTP.getLayer2Info().getOuterVlanTag().getValue();
        ifce_c.put("name", customer_port);
        ifce_c.put("vlan", customer_port_vlan); // vlan=0 for unit 0 
        
        //configure provider bgp
        String provider_group_name = NPSGlobalState.getProviderDefaultBgpGroup();
        if (providerSTP.getLayer3Info().getBgpInfo().getGroupName() != null) {
            provider_group_name = providerSTP.getLayer3Info().getBgpInfo().getGroupName();
        }
        bgp_p.put("group_name", provider_group_name);
        String provider_bgp_local_address = NPSUtils.extractIpAddress(provider_port_local_address); 
        bgp_p.put("local_address", provider_bgp_local_address);
        String provider_port_remote_address = providerSTP.getLayer3Info().getBgpInfo().getLinkRemoteIpAndMask();
        String provider_bgp_peer_address = NPSUtils.extractIpAddress(provider_port_remote_address); 
        bgp_p.put("peer_address", provider_bgp_peer_address);
        String provider_bgp_peer_asn = providerSTP.getLayer3Info().getBgpInfo().getPeerASN();
        bgp_p.put("peer_asn", provider_bgp_peer_asn);
        bgp_p.put("reject_policy_name", bgp_reject_policy);
        
        //configure customer bgp
        String customer_group_name = NPSGlobalState.getCustomerDefaultBgpGroup();
        if (customerSTP.getLayer3Info().getBgpInfo().getGroupName() != null) {
            customer_group_name = customerSTP.getLayer3Info().getBgpInfo().getGroupName();
        }
        bgp_c.put("group_name", customer_group_name);
        //String customer_port_local_address = customerSTP.getLayer3Info().getBgpInfo().getLinkLocalIpAndMask();
        //String customer_bgp_local_address = NPSUtils.extractIpAddress(customer_port_local_address); 
        //bgp_c.put("local_address", customer_bgp_local_address);
        String customer_port_remote_address = customerSTP.getLayer3Info().getBgpInfo().getLinkRemoteIpAndMask();
        String customer_bgp_peer_address = NPSUtils.extractIpAddress(customer_port_remote_address); 
        bgp_c.put("peer_address", customer_bgp_peer_address);
        String customer_bgp_peer_asn = customerSTP.getLayer3Info().getBgpInfo().getPeerASN();
        bgp_c.put("peer_asn", customer_bgp_peer_asn);
        bgp_c.put("reject_policy_name", bgp_reject_policy);
        
        //configure provider policies
        String provider_prefix_list_name = "AWS-prefix-list"; // configurable?
        policies_p.put("prefix_list_name", provider_prefix_list_name);
        List<String> provider_prefix_list_items = providerSTP.getLayer3Info().getBgpInfo().getPeerIpPrefix();
        policies_p.put("prefix_list_items", provider_prefix_list_items);
        String provider_policy_name = "AWS-provider-policy"; // configurable?
        policies_p.put("policy_name", provider_policy_name);
        
        //configure customer policies
        String customer_prefix_list_name = "prefix-list-customer-contract-"+contractId;
        if (customerSTP.getLayer3Info().getBgpInfo().getPeerPrefixListName() != null) {
            // using existing prefix-list
            policies_c.put("prefix_list_existed", true);
            customer_prefix_list_name = customerSTP.getLayer3Info().getBgpInfo().getPeerPrefixListName();
        } else if (customerSTP.getLayer3Info().getBgpInfo().getPeerIpPrefix() != null) {            
            policies_c.put("prefix_list_existed", false);
            List<String> customer_prefix_list_items = customerSTP.getLayer3Info().getBgpInfo().getPeerIpPrefix();
            policies_c.put("prefix_list_items", customer_prefix_list_items);
        } else {
            throw new DeviceException("cannot configure customer IP prefix list - need either an existing prefix-list name or a list or IP prefixes");
        }
        policies_c.put("prefix_list_name", customer_prefix_list_name);
        String customer_policy_name = "policy-customer-contract-"+contractId;
        policies_c.put("policy_name", customer_policy_name);
        
        //configure filters
        String provider_input_filter_name = "filter-provider-input-contract-"+contractId;
        ((Map)filters_p.get("input")).put("name", provider_input_filter_name);
        String provider_output_filter_name = "filter-provider-output-contract-"+contractId;
        ((Map)filters_p.get("output")).put("name", provider_output_filter_name);
        String customer_input_filter_name = "filter-customer-interface-"+customer_port+"."+customer_port_vlan;;
        ((Map)filters_c.get("input")).put("name", customer_input_filter_name);
        String customer_input_filter_term = "match-prefix-list-contract-"+contractId;
        ((Map)filters_c.get("input")).put("term", customer_input_filter_term);

        //configure routing_instance
        String routing_instance_name = "routing-instance-contract-"+contractId;
        routing_instance.put("name", routing_instance_name);

        mapInput.put("has_customer_config", bgp_config_customer.booleanValue()); //TODO: configurable 

        //configure policer
        // TODO: a policyDataParser in PolicyManager to better intepret policy semantics
        mapInput.put("has_bw_policer", false);
        if (policyList != null) {
            for (ServicePolicy policy: policyList) {
                if (policy.getSubject().equalsIgnoreCase("provider")
                        && policy.getAction().equalsIgnoreCase("limit")
                        && policy.getConstraintType().equalsIgnoreCase("bandwidth")) {
                    mapInput.put("has_bw_policer", true);
                    String policer_name = "bw-policer-contract-"+contractId;
                    policer.put("name", policer_name);
                    long bw = NPSUtils.bandwdithToBps(policy.getConstraintValue());
                    String policer_bandwidth_limit = Long.toString(bw);
                    String policer_burst_size_limit = Long.toString(bw/10);
                    // CIR in bit-per-sec
                    policer.put("bandwidth_limit", policer_bandwidth_limit);
                    // CBS in bytes (0.1*8 sec of buffering)
                    policer.put("burst_size_limit", policer_burst_size_limit);
                }
            }
        }
        
        // generate cmdToApply
        String cmdApply = NPSUtils.generateConfig(mapInput, "junos-apply.txt");
        deviceDelta.setCmdToApply(cmdApply);
        // generate cmdToDelete
        String cmdDelete = NPSUtils.generateConfig(mapInput, "junos-delete.txt");
        deviceDelta.setCmdToDelete(cmdDelete);
        // generate cmdToVerify
        String cmdVerify = NPSUtils.generateConfig(mapInput, "junos-verify.txt");
        deviceDelta.setCmdToVerify(cmdVerify);
        // generate xpathExprToVerify
        String xpathVerifyExpr = NPSUtils.generateConfig(mapInput, "junos-verify-xpaths.txt");
        deviceDelta.setXpathVerifyExpr(xpathVerifyExpr);
        
        return deviceDelta;
    }

}
