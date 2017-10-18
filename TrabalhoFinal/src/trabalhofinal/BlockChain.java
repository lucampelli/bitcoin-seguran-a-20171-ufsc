    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.util.HashMap;

/**
 *
 * @author luca
 */
public class BlockChain {
    
    HashMap<String, Block> chain = new HashMap<>();
    
    public void addBlock(Block block){
        chain.put(block.Hash(), block);
    }
    
    public Block getBlockByHash(String hash){
        return chain.get(hash);
    }
    
    
}
