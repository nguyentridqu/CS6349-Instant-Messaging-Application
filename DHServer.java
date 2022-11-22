import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

public class DHServer {
    KeyPair serverKeys;
    KeyAgreement serverKeyAgreement;

    private DHServer() {

    }

    public void generateServerKeys() throws Exception{
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DiffieHellman");
        keyGen.initialize(4096);
        serverKeys = keyGen.generateKeyPair();
        serverKeyAgreement = KeyAgreement.getInstance("DiffieHellman");
        serverKeyAgreement.init(serverKeys.getPrivate());
    }

    public byte[] getKeyToSend(){
        return serverKeys.getPublic().getEncoded();
    }

    public byte[] computeSharedSecret(byte[] clientPublicKey) throws Exception{
        KeyFactory keyFactory = KeyFactory.getInstance("DiffieHellman");
        X509EncodedKeySpec clientKeyEncoded = new X509EncodedKeySpec(clientPublicKey);
        PublicKey clientKey = keyFactory.generatePublic(clientKeyEncoded);

        serverKeyAgreement.doPhase(clientKey, true);
        return serverKeyAgreement.generateSecret();
    }
}
