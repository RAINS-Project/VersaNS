/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

/**
 *
 * @author xyang
 */
public class DeviceException extends Exception {
    private static final long serialVersionUID = 1L;

    public DeviceException(Throwable ex) {
        super(ex);
    }

    public DeviceException(String msg) {
        super(msg);
    }
}
