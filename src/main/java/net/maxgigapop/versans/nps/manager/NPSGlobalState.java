/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.manager;

import net.maxgigapop.versans.nps.device.InterfaceDeltaStore;
import net.maxgigapop.versans.nps.device.DeviceDeltaStore;
import net.maxgigapop.versans.nps.device.DeviceStore;
import net.maxgigapop.versans.nps.device.InterfaceStore;
import net.maxgigapop.versans.nps.config.NPSGlobalConfig;
import net.maxgigapop.versans.nps.config.ConfigException;
import net.maxgigapop.versans.nps.config.NPSConfigYaml;
import net.maxgigapop.versans.nps.api.ServiceException;
import net.maxgigapop.versans.nps.rest.model.ModelStore;
import net.maxgigapop.versans.nps.rest.model.DeltaStore;

/**
 *
 * @author xyang
 */
public class NPSGlobalState {
	public static boolean Inited = false;
    private static NPSContractManager contractManager = null;
    private static TopologyManager topologyManager = null;
    private static PolicyManager policyManager = null;
    private static String dbUser = "dragon";
    private static String dbPwd = "flame";
    private static String templateDir = ".";
    private static String providerDefaultBgpGroup = "AWS";
    private static String customerDefaultBgpGroup = "MEMBERS";
    private static int pollInterval = 30000; // 30 seconds by default
    private static int extendedPollInterval = 900000; // 15 minutes by default
    private static DeviceStore deviceStore = null;
    private static InterfaceStore interfaceStore = null;
    private static DeviceDeltaStore deviceDeltaStore = null;
    private static InterfaceDeltaStore interfaceDeltaStore = null;
    private static ModelStore modelStore = null;
    private static DeltaStore deltaStore = null;

    public static void init() throws ConfigException, ServiceException {
    	Inited = true;
        //init from yaml config file
        NPSConfigYaml.loadConfig("/etc/versans/nps/nps.yaml");
        NPSGlobalConfig config = NPSConfigYaml.getInstance().getNPSGlobalConfig();
        if (config.getDbUser() != null && !config.getDbUser().isEmpty()) {
            dbUser = config.getDbUser();
        }
        if (config.getDbPass() != null && !config.getDbPass().isEmpty()) {
            dbPwd = config.getDbPass();
        }
        if (config.getTemplateDir() != null && !config.getTemplateDir().isEmpty()) {
            templateDir = config.getTemplateDir();
        }
        if (config.getProviderDefaultBgpGroup() != null && !config.getProviderDefaultBgpGroup().isEmpty()) {
            providerDefaultBgpGroup = config.getProviderDefaultBgpGroup();
        }
        if (config.getCustomerDefaultBgpGroup() != null && !config.getCustomerDefaultBgpGroup().isEmpty()) {
            customerDefaultBgpGroup = config.getCustomerDefaultBgpGroup();
        }
        if (config.getPollInterval() != 0) {
            pollInterval = config.getPollInterval();
        }
        if (config.getExtendedPollInterval() != 0) {
            extendedPollInterval = config.getExtendedPollInterval();
        }

        //init hibernate
        HibernateUtil.initSessionFactory();

        //init database and tables
        initDatabase();

        //init cached DB data
        deviceStore = new DeviceStore();
        interfaceStore = new InterfaceStore();
        deviceDeltaStore = new DeviceDeltaStore();
        interfaceDeltaStore = new InterfaceDeltaStore();
        modelStore = new ModelStore();
        deltaStore = new DeltaStore();
    }

    private static void initDatabase() throws ServiceException {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            throw new ServiceException(ex.getMessage());
        }
        // create 'nps' database
        NPSUtils.executeDirectStatement("CREATE DATABASE IF NOT EXISTS nps");
        // initialize database tables
        //init the devices table
        NPSUtils.executeDirectStatement("CREATE TABLE IF NOT EXISTS nps.devices ( "
                + "id int(11) NOT NULL auto_increment, "
                + "urn VARCHAR(255) NOT NULL, "
                + "makeModel VARCHAR(255) NOT NULL, "
                + "address VARCHAR(255) NOT NULL, "
                + "location VARCHAR(255) NOT NULL, "
                + "description VARCHAR(255) NOT NULL, "
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=latin1");
        //init the interfaces table
        NPSUtils.executeDirectStatement("CREATE TABLE IF NOT EXISTS nps.interfaces ( "
                + "id int(11) NOT NULL auto_increment, "
                + "urn VARCHAR(255) NOT NULL, "
                + "makeModel VARCHAR(255) NOT NULL, "
                + "deviceId int(11) NOT NULL, "
                + "description VARCHAR(255) NOT NULL, "
                + "aliasUrn VARCHAR(255) DEFAULT NULL, "
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=latin1");
        //init the contracts table
        NPSUtils.executeDirectStatement("CREATE TABLE IF NOT EXISTS nps.contracts ( "
                + "id VARCHAR(255) NOT NULL, "
                + "description VARCHAR(255) NOT NULL, "
                + "status VARCHAR(255) NOT NULL, "
                + "error VARCHAR(255) NOT NULL, "
                + "modifiedTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "deleted int(1) NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=latin1");
        //init the deviceDetlas table
        NPSUtils.executeDirectStatement("CREATE TABLE IF NOT EXISTS nps.deviceDeltas ( "
                + "id int(11) NOT NULL auto_increment, "
                + "deviceId int(11) NOT NULL, "
                + "contractId VARCHAR(255) NOT NULL, "
                + "cmdToApply TEXT, "
                + "cmdToDelete TEXT, "
                + "cmdToVerify TEXT, "
                + "xpathVerifyExpr TEXT, "
                + "deleted int(1) NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=latin1");
        //init the interfaceDeltas table
        NPSUtils.executeDirectStatement("CREATE TABLE IF NOT EXISTS nps.interfaceDeltas ( "
                + "id int(11) NOT NULL auto_increment, "
                + "interfaceId int(11) NOT NULL, "
                + "deviceDeltaId int(11) NOT NULL, "
                + "cmdToApply TEXT, "
                + "cmdToDelete TEXT, "
                + "cmdToVerify TEXT, "
                + "xpathVerifyExpr TEXT, "
                + "deleted int(1) NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=latin1");
        //init the models table
        NPSUtils.executeDirectStatement("CREATE TABLE IF NOT EXISTS nps.models ( "
                + "id int(11) NOT NULL auto_increment, "
                + "createdTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "version VARCHAR(255) NOT NULL, "
                + "ttlModel LONGTEXT NOT NULL, "
                + "status VARCHAR(255) NOT NULL, "
                + "PRIMARY KEY (id, version)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=latin1");
        //init the deltas table
        NPSUtils.executeDirectStatement("CREATE TABLE IF NOT EXISTS nps.deltas ( "
                + "id int(11) NOT NULL auto_increment, "
                + "createdTime DATETIME NOT NULL, "
                + "referenceVersion VARCHAR(255) NOT NULL, "
                + "targetVersion VARCHAR(255) NOT NULL, "
                + "modelAddition LONGTEXT NOT NULL, "
                + "modelReduction LONGTEXT NOT NULL, "
                + "status VARCHAR(255) NOT NULL, "
                + "PRIMARY KEY (id, targetVersion)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=latin1");
    }

    public static NPSContractManager getContractManager() {
        return contractManager;
    }

    public static void setContractManager(NPSContractManager contractManager) {
        NPSGlobalState.contractManager = contractManager;
    }

    public static TopologyManager getTopologyManager() {
        return topologyManager;
    }

    public static void setTopologyManager(TopologyManager topologyManager) {
        NPSGlobalState.topologyManager = topologyManager;
    }

    public static PolicyManager getPolicyManager() {
        return policyManager;
    }

    public static void setPolicyManager(PolicyManager policyManager) {
        NPSGlobalState.policyManager = policyManager;
    }

    public static String getDbPwd() {
        return dbPwd;
    }

    public static void setDbPwd(String dbPwd) {
        NPSGlobalState.dbPwd = dbPwd;
    }

    public static String getDbUser() {
        return dbUser;
    }

    public static void setDbUser(String dbUser) {
        NPSGlobalState.dbUser = dbUser;
    }

    public static String getTemplateDir() {
        return templateDir;
    }

    public static void setTemplateDir(String templateDir) {
        NPSGlobalState.templateDir = templateDir;
    }

    public static String getCustomerDefaultBgpGroup() {
        return customerDefaultBgpGroup;
    }

    public static void setCustomerDefaultBgpGroup(String customerDefaultBgpGroup) {
        NPSGlobalState.customerDefaultBgpGroup = customerDefaultBgpGroup;
    }

    public static String getProviderDefaultBgpGroup() {
        return providerDefaultBgpGroup;
    }

    public static void setProviderDefaultBgpGroup(String providerDefaultBgpGroup) {
        NPSGlobalState.providerDefaultBgpGroup = providerDefaultBgpGroup;
    }

    public static int getPollInterval() {
        return pollInterval;
    }

    public static void setPollInterval(int pollInterval) {
        NPSGlobalState.pollInterval = pollInterval;
    }

    public static int getExtendedPollInterval() {
        return extendedPollInterval;
    }

    public static void setExtendedPollInterval(int extendedPollInterval) {
        NPSGlobalState.extendedPollInterval = extendedPollInterval;
    }

    public static DeviceDeltaStore getDeviceDeltaStore() {
        return deviceDeltaStore;
    }

    public static void setDeviceDeltaStore(DeviceDeltaStore deviceDeltaStore) {
        NPSGlobalState.deviceDeltaStore = deviceDeltaStore;
    }

    public static DeviceStore getDeviceStore() {
        return deviceStore;
    }

    public static void setDeviceStore(DeviceStore deviceStore) {
        NPSGlobalState.deviceStore = deviceStore;
    }

    public static InterfaceDeltaStore getInterfaceDeltaStore() {
        return interfaceDeltaStore;
    }

    public static void setInterfaceDeltaStore(InterfaceDeltaStore interfaceDeltaStore) {
        NPSGlobalState.interfaceDeltaStore = interfaceDeltaStore;
    }

    public static InterfaceStore getInterfaceStore() {
        return interfaceStore;
    }

    public static void setInterfaceStore(InterfaceStore interfaceStore) {
        NPSGlobalState.interfaceStore = interfaceStore;
    }

    public static ModelStore getModelStore() {
        return modelStore;
    }

    public static DeltaStore getDeltaStore() {
        return deltaStore;
    }
}
