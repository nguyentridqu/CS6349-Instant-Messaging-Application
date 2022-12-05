import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Helper {

    public static void sendEncrypt(ObjectOutputStream objOut, String message, byte[] sessionKey) throws IOException {
        byte[] plaintext = message.getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = KeyedHash.encrypt(plaintext, sessionKey);
        objOut.writeObject(ciphertext);
        objOut.flush();
    }

    public static String recvDecrypt(ObjectInputStream objIn, byte[] sessionKey) throws IOException, ClassNotFoundException {
        byte[] ciphertext = (byte[]) objIn.readObject();
        byte[] plaintext = KeyedHash.decrypt(ciphertext, sessionKey);
        plaintext = Helper.trimNullBytes(plaintext);
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
