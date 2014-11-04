/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.config;

import java.util.HashMap;

/**
 *
 * @author xyang
 */
public class BaseConfig {
    protected String id;
    protected String impl;
    protected HashMap<String, Object> params;

    public void setImpl(String impl) {
        this.impl = impl;
    }

    public String getImpl() {
        return impl;
    }

    public void setParams(HashMap<String, Object> params) {
        this.params = params;
    }

    public HashMap<String, Object> getParams() {
        return params;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
