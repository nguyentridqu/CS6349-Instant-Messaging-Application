import javax.crypto.KeyAgreement;
import javax.crypto.ShortBufferException;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

public class DHClient {
    private KeyPair clientKeys;
    private KeyAgreement clientKeyAgreement;

    public DHClient(byte[] serverPublicKey) throws Exception{
        KeyFactory keyFactory = KeyFactory.getInstance("DiffieHellman");
        X509EncodedKeySpec serverKeyEncoded = new X509EncodedKeySpec(serverPublicKey);
        PublicKey serverKey = keyFactory.generatePublic(serverKeyEncoded);
        DHParameterSpec DHParams = ((DHPublicKey)serverKey).getParams();

        KeyPairGenerator clientKeyGen = KeyPairGenerator.getInstance("DiffieHellman");
        clientKeyGen.initialize(DHParams);
        clientKeys = clientKeyGen.generateKeyPair();

        clientKeyAgreement = KeyAgreement.getInstance("DiffieHellman");
        clientKeyAgreement.init(clientKeys.getPrivate());
        clientKeyAgreement.doPhase(serverKey, true);
    }

    public byte[] getKeyToSend(){
        return clientKeys.getPublic().getEncoded();
    }

    public byte[] computeSharedSecret() throws ShortBufferException {
        return clientKeyAgreement.generateSecret();
    }
}