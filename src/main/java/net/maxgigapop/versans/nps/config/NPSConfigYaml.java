/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.ho.yaml.Yaml;

/**
 *
 * @author xyang
 */
public class NPSConfigYaml {
    private static NPSConfigYaml instance;
    private NPSGlobalConfig npsGlobalConfig;

    private NPSConfigYaml() {}
    
    public static NPSConfigYaml getInstance() {
        if (instance == null) {
            instance = new NPSConfigYaml();
        }
        return instance;
    }

    @SuppressWarnings("static-access")
    public static void loadConfig(String filename) throws ConfigException {
        System.out.println("NPSConfigYaml: "+filename);
        NPSConfigYaml holder = NPSConfigYaml.getInstance();

        NPSGlobalConfig configuration = null;
        InputStream propFile = NPSConfigYaml.class.getClassLoader().getSystemResourceAsStream(filename);
        try {
            configuration = (NPSGlobalConfig) Yaml.loadType(propFile, NPSGlobalConfig.class);
        } catch (NullPointerException ex) {
            try {
                propFile = new FileInputStream(new File(filename));
                configuration = (NPSGlobalConfig) Yaml.loadType(propFile, NPSGlobalConfig.class);
            } catch (FileNotFoundException e) {
                System.out.println("NPSConfigYaml: configuration file: "+ filename + " not found");
                e.printStackTrace();
                throw new ConfigException(e.getMessage());
            }
        } catch (FileNotFoundException e) {
            System.out.println("NPSConfigYaml: configuration file: "+ filename + " not found");
            e.printStackTrace();
            throw new ConfigException(e.getMessage());
        }
        holder.setNPSGlobalConfig(configuration);
        try {
            propFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NPSGlobalConfig getNPSGlobalConfig() {
        return npsGlobalConfig;
    }

    public void setNPSGlobalConfig(NPSGlobalConfig npsGlobalConfig) {
        this.npsGlobalConfig = npsGlobalConfig;
    }
    
    
}
