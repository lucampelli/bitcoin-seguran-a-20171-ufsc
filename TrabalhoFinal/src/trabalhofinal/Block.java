/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luca Um bloco da blockChain
 */
public class Block {

    private String blockOwnerID;
    private String targetID;
    private Date timeStamp;
    private String previousHash;
    private String hash;
    private String fundBlock;
    private float value;
    private float change;

    /**
     * Normal Block
     * @param owner
     * @param target
     * @param time
     * @param previous
     * @param fundBlock
     * @param value 
     */
    public Block(String owner, String target, Date time, String previous, Block fundBlock, float value) {
        try {
            this.blockOwnerID = owner;
            this.targetID = target;
            this.timeStamp = time;
            this.previousHash = previous;
            this.fundBlock = fundBlock.Hash();
            this.value = value;
            this.change = fundBlock.Value() - value;
            this.hash = BCTimestampServer.bytesToHex(MessageDigest.getInstance("SHA-512").digest((this.blockOwnerID + this.targetID + this.timeStamp.toString() + this.previousHash).getBytes()));
        } catch (NoSuchAlgorithmException ex) {
            System.out.println("Block Error: " + ex.getMessage());
        }
    }
    
    /**
     * MatrixBlock
     * @param owner
     * @param target
     * @param time
     * @param value 
     */
    public Block(String target, int order, float value){
        try {
            this.blockOwnerID = "";
            this.targetID = target;
            this.timeStamp = new Date();
            this.previousHash = "";
            this.fundBlock = "";
            this.value = value;
            this.change = 0;
            this.hash = "" + order;
        } catch (Exception ex) {
            System.out.println("Block Error: " + ex.getMessage());
        }
    }

    /**
     * Normal Block Parse from String
     * @param data 
     */
    public Block(String data) {
        try {
            String[] split = data.trim().split(" ");
            if (split.length != 8) {
                throw new Exception("New Block Wrong Data Length");
            }
            this.blockOwnerID = split[0];
            this.targetID = split[1];
            this.timeStamp = new Date(split[2]);
            this.previousHash = split[3];
            this.fundBlock = split[4];
            this.value = Float.parseFloat(split[5]);
            this.change = Float.parseFloat(split[6]);
            this.hash = split[7];
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public float Value() {
        return this.value;
    }

    public String Hash() {
        return this.hash;
    }
    
    public String ID(){
        return this.blockOwnerID;
    }
    
    public String fundBlock(){
        return this.fundBlock;
    }
    
    public String previousBlock(){
        return this.previousHash;
    }

    public String target(){
        return this.targetID;
    }
    
    @Override
    public String toString() {
        return this.blockOwnerID + " " + this.targetID + " "
                + "17/10/01" + " " + this.previousHash + " "
                + this.fundBlock + " " + this.value + " " + this.change + " " + this.hash;
    }
    
    //Debug purposes
    public String toStringLines() {
        return this.blockOwnerID + System.lineSeparator() + this.targetID + System.lineSeparator()
                + "17/10/01" + System.lineSeparator() + this.previousHash + System.lineSeparator()
                + this.fundBlock + System.lineSeparator() + this.value + System.lineSeparator() + this.change + System.lineSeparator() + this.hash + System.lineSeparator();
    }

}
