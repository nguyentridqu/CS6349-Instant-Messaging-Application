import javax.sound.midi.SysexMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Helper {
    private static String hashFunction = "SHA-256";

    public static void sendEncrypt(ObjectOutputStream objOut, String message, byte[] sessionKey) throws Exception {
        // encrypt the message
        byte[] plaintext = message.getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = KeyedHash.encrypt(plaintext, sessionKey);

        // create hash for integrity verification
        MessageDigest digest = MessageDigest.getInstance(hashFunction);
        byte[] messageKey = Arrays.copyOf(plaintext, plaintext.length + sessionKey.length);
        System.arraycopy(sessionKey, 0, messageKey, plaintext.length, sessionKey.length);
        byte[] hash = digest.digest(messageKey);

        // send message
        objOut.writeObject(ciphertext);
        objOut.writeObject(hash);
        objOut.flush();
    }

    public static String recvDecrypt(ObjectInputStream objIn, byte[] sessionKey) throws Exception {
        // receive and decrypt the message
        byte[] ciphertext = (byte[]) objIn.readObject();
        byte[] plaintext = KeyedHash.decrypt(ciphertext, sessionKey);
        plaintext = Helper.trimNullBytes(plaintext);

        // receive the hash and verify integrity
        byte[] hashReceived = (byte[]) objIn.readObject();
        MessageDigest digest = MessageDigest.getInstance(hashFunction);
        byte[] messageKey = Arrays.copyOf(plaintext, plaintext.length + sessionKey.length);
        System.arraycopy(sessionKey, 0, messageKey, plaintext.length, sessionKey.length);
        byte[] hashCalculated = digest.digest(messageKey);

        if (Arrays.equals(hashCalculated, hashReceived)) {
            //System.out.println("Hash matched\n");
        } else {
            //System.out.println("Hash not matching\n");
            throw new Exception("Integrity hash not matching");
        }

        return new String(plaintext);
    }

    public static byte[] trimNullBytes(byte[] bytes) {
        if (bytes.length == 0) return bytes;
        int i = bytes.length - 1;
        while (bytes[i] == 0) {
            i--;
        }
        byte[] copy = Arrays.copyOfRange(bytes, 0, i + 1);
        return copy;
    }

    public static String bytesToHexString(byte[] bytes) {
        String hexString = "0x";
        for (byte b : bytes) {
            hexString += String.format("%02x", b);
        }
        return hexString;
    }

}
