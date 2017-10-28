/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.net.InetAddress;

/**
 *
 * @author luca
 */
public abstract class BCClient {
    
    public abstract void addPeer(String HashID, InetAddress address);
}
