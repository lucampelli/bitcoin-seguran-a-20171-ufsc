    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author luca
 * A Blockchain em si
 */
public class BlockChain {
    
    private HashMap<String, Block> chain = new HashMap<>();
    private HashMap<String, ArrayList<Block>> IDchain = new HashMap<>();
    private Block head;
    
    public BlockChain(){
        //Block matrixBlock = new Block("AC84B32E9D61A4422D1F7AABEF96C326CD2BDD61BFDBF46C2E193EC645B1CA40DD72662FD25B194A1403EDF76B80D18042A220C4DC97966DE718E37F64FFCF9B",
        //        1, 50);
        
        //addBlock(matrixBlock);
    }
    
    public void addBlock(Block block){
        head = block;
        chain.put(block.Hash(), block);
        if(IDchain.containsKey(block.ID())){
            ArrayList<Block> temp = IDchain.get(block.ID());
            temp.add(block);
        } else {
            ArrayList<Block> temp = new ArrayList();
            temp.add(block);
            IDchain.put(block.ID(), temp);
        }
    }
    
    public Block Head(){
        return head;
    }
    
    public Block getBlockByHash(String hash){
        return chain.get(hash);
    }
    
    public ArrayList<Block> getBlocksByID(String ID){
        return IDchain.get(ID);
    }
    
    public String toStringLines(){
        Block b = head;
        String r = "";
        for(int i = 0; i < chain.size(); i++){
            r += b.toStringLines();
            b = getBlockByHash(b.previousBlock());
        }
        return r;
        
    }
    
    
}
