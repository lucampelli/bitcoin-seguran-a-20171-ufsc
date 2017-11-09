/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author luca A Blockchain em si
 */
public class BlockChain implements Serializable {

    private HashMap<String, Block> chain = new HashMap<>();
    private Block head;

    /**
     * Blockchain
     */
    public BlockChain() {
    }

    /**
     * Adiciona um novo bloco à blockchain
     * @param block Bloco adicionado
     */
    public void addBlock(Block block) {
        if(chain.containsKey(block.Hash())){
            return;
        }
        head = block;
        chain.put(block.Hash(), block);
    }

    /**
     * Retorna o primeiro elemento da blockchain
     * @return o bloco mais recente
     */
    public Block Head() {
        return head;
    }

    /**
     * Retorna um bloco pela sua hash
     * @param hash a Hash do bloco
     * @return o bloco
     */
    public Block getBlockByHash(String hash) {
        return chain.get(hash);
    }

    /**
     * Retorna todos os blocos de um usuário
     * @param ID
     * @return 
     */
    public ArrayList<Block> getAllBlocksFromUser(String ID) {
        ArrayList<Block> ans = new ArrayList();

        for (Block b : chain.values()) {
            if (b.ID().equals(ID)) {
                ans.add(b);
            }
        }

        return ans;
    }

    /**
     * retorna a blockchain em formato de string com newlines
     * @return descricao
     */
    public String toStringLines() {
        Block b = head;
        String r = "";
        for (int i = 0; i < chain.size(); i++) {
            if (b != null) {
                r += b.toStringLines();
                b = getBlockByHash(b.previousBlock().trim());
            }
        }
        return r;

    }

    /**
     * Retorna todos os blocos direcionados ao usuário
     * @param HashID    o alvo
     * @return todos os blocos direcionados ao usuário
     */
    public ArrayList<Block> getAllBlocksToUser(String HashID) {

        ArrayList<Block> ans = new ArrayList();

        for (Block b : chain.values()) {
            if (b.target().equals(HashID)) {
                ans.add(b);
            }
        }

        return ans;
    }

}
