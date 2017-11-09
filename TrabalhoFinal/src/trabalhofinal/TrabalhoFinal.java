/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

/**frame.updateText();
 *
 * @author luca
 * Main
 */
public class TrabalhoFinal {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Digite uma das opções abaixo:");
            System.out.println("1 - Server");
            System.out.println("2 - Wallet");
            System.out.println("3 - Miner");
            System.exit(-1);
        }

        switch (Integer.parseInt(args[0])) {
            case 1:
                new Thread(BCTimestampServer.getInstance()).start();
                break;
            case 2:
                new BCWallet();
                break;
            case 3:
                new BCMiner();
                break;
            default:
                System.out.println("Opção não contemplada.");
                System.exit(-2);
        }
    }
}
