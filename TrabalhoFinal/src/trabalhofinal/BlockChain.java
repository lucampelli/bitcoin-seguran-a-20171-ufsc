    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author luca
 * A Blockchain em si
 */
public class BlockChain {
    
    HashMap<String, Block> chain = new HashMap<>();
    HashMap<String, ArrayList<Block>> IDchain = new HashMap<>();
    Block head;
    
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
    
    public Block getBlockByHash(String hash){
        return chain.get(hash);
    }
    
    public ArrayList<Block> getBlocksByID(String ID){
        return IDchain.get(ID);
    }
    
    
}
